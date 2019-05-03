package com.example.ducks.screen;


import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.*;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.*;
import android.view.*;
import android.widget.Button;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera extends AppCompatActivity {
    private CameraManager cameraManager;
    private int cameraFacing;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private String cameraId;
    private final int CAMERA_REQUEST_CODE = 1;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback stateCallback;
    private TextureView textureView;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private ImageReader mImageReader;
    private Bitmap bitmap, bitmap2;
    private Size previewSize;
    private File galleryFolder, galleryFolder2;
    private static String URL = "http://192.168.1.8:8080/";
    private String android_id;
    private long t;
    private int xs = 640, ys = 360;

    private Size chooseOptimalSize(Size[] outputSizes, int width, int height) {
        double preferredRatio = height / (double) width;
        Size currentOptimalSize = outputSizes[0];
        double currentOptimalRatio = currentOptimalSize.getWidth() / (double) currentOptimalSize.getHeight();
        for (Size currentSize : outputSizes) {
            double currentRatio = currentSize.getWidth() / (double) currentSize.getHeight();
            if (Math.abs(preferredRatio - currentRatio) <
                    Math.abs(preferredRatio - currentOptimalRatio)) {
                currentOptimalSize = currentSize;
                currentOptimalRatio = currentRatio;
            }
        }
        return currentOptimalSize;
    }

    private void setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), size.y, size.x);
                    this.cameraId = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);

            fixDarkPreview();

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                Camera.this.cameraCaptureSession = cameraCaptureSession;
                                Camera.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, "Screen.jpg");
        galleryFolder2 = new File(storageDirectory, "Screen2.jpg");
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
    }

    private void lock() {
        try {
            cameraCaptureSession.capture(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        textureView = findViewById(R.id.texture_view);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);

        android_id = android.provider.Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
                Camera.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                cameraDevice.close();
                Camera.this.cameraDevice = null;
            }

            @Override
            public void onError(CameraDevice cameraDevice, int error) {
                cameraDevice.close();
                Camera.this.cameraDevice = null;
            }
        };

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                setUpCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };

        ActivityCompat.requestPermissions(Camera.this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (textureView.isAvailable()) {
            setUpCamera();
            //transformImage(textureView.getWidth(), textureView.getHeight());
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    public void fab(View view) {
        PhotoThread newThread = new PhotoThread();
        newThread.start();
    }

    private void fixDarkPreview() throws CameraAccessException {
        Range<Integer>[] autoExposureFPSRanges = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        if (autoExposureFPSRanges != null) {
            for (Range<Integer> autoExposureRange : autoExposureFPSRanges) {
                if (autoExposureRange.equals(Range.create(15, 30))) {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            Range.create(15, 30));
                }
            }
        }
    }

    class PhotoThread extends Thread {

        @Override
        public void run() {
            SendThread sendThread = new SendThread();
            t = System.currentTimeMillis() + (int) Sync.deltaT + 1000;
            sendThread.start();
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bitmap = textureView.getBitmap();
                }
            }, t - (System.currentTimeMillis() + (int)Sync.deltaT));
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bitmap2 = textureView.getBitmap();
                    new CordThread().start();
                }
            }, t - (System.currentTimeMillis() + (int)Sync.deltaT) + 45);
            /*try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            FileOutputStream outputPhoto = null, outputPhoto2 = null;
            try {
                createImageGallery();
                outputPhoto = new FileOutputStream(galleryFolder);
                outputPhoto2 = new FileOutputStream(galleryFolder2);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                outputPhoto.write(byteArray);
                stream = new ByteArrayOutputStream();
                bitmap2.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byteArray = stream.toByteArray();
                outputPhoto2.write(byteArray);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }
    }

    class SendThread extends Thread {

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);
            Call<Void> call = service.put(android_id, 0, t);
            try {
                call.execute();
                Log.d("SEND_AND_RETURN", "" + (t - (System.currentTimeMillis() + (int) Sync.deltaT)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class CordThread extends Thread {

        @Override
        public void run() {
            int orientation = Camera.this.getResources().getConfiguration().orientation;
            Matrix matrix = new Matrix();
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                matrix.postRotate(-90);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap = Bitmap.createScaledBitmap(bitmap, xs, ys, false);
            bitmap2 = bitmap2.copy(Bitmap.Config.ARGB_8888, true);
            matrix = new Matrix();
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                matrix.postRotate(-90);
            }
            bitmap2 = Bitmap.createBitmap(bitmap2, 0, 0, bitmap2.getWidth(), bitmap2.getHeight(), matrix, true);
            bitmap2 = Bitmap.createScaledBitmap(bitmap2, xs, ys, false);


            LinkedList<Point> linkedList = new LinkedList<>();
            LinkedList<Point> linkedList2 = new LinkedList<>();
            for (int i = 0; i < bitmap.getHeight(); i++) {
                for (int j = 0; j < bitmap.getWidth(); j++) {
                    if (bitmap.getPixel(j, i) != bitmap2.getPixel(j, i)) {
                        int is = bitmap2.getPixel(j, i);
                        float[] hsv = new float[3];
                        Color.RGBToHSV(Color.red(is), Color.green(is), Color.blue(is), hsv);

                        int need2 = 0xff303f9f;
                        float[] hsv3 = new float[3];
                        Color.RGBToHSV(Color.red(need2), Color.green(need2), Color.blue(need2), hsv3);
                        if (Math.abs(hsv[0] - hsv3[0]) <= 16 && Math.abs(hsv[1] - hsv3[1]) <= 0.35 && Math.abs(hsv[2] - hsv3[2]) <= 0.35) {
                            bitmap2.setPixel(j, i, Color.GREEN);
                            linkedList.add(new Point(i, j));
                        }

                        int need = 0xff00ff00;
                        float[] hsv2 = new float[3];
                        Color.RGBToHSV(Color.red(need), Color.green(need), Color.blue(need), hsv2);
                        if (Math.abs(hsv[0] - hsv2[0]) <= 25 && Math.abs(hsv[1] - hsv2[1]) <= 0.60 && Math.abs(hsv[2] - hsv2[2]) <= 0.60) {
                                linkedList2.add(new Point(i, j));
                                bitmap2.setPixel(j, i, Color.RED);
                        }
                    }
                }
            }

            TreeMap<Integer, LinkedList<Integer>> treeMap = new TreeMap<>();
            if (linkedList.size() > 0) {
                for (Point i : linkedList) {
                    if (treeMap.containsKey(i.x)) {
                        treeMap.get(i.x).add(i.y);
                    } else {
                        treeMap.put(i.x, new LinkedList<Integer>());
                        treeMap.get(i.x).add(i.y);
                    }
                }
                int j = 0, a = 0;
                for (int i : treeMap.keySet()) {
                    a += treeMap.get(i).size();
                    j++;
                }
                Iterator it = treeMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, LinkedList<Integer>> item = (Map.Entry<Integer, LinkedList<Integer>>) it.next();
                    if (item.getValue().size() != a / j)
                        it.remove();
                }
//                Log.e("PHOTO", Collections.min(treeMap.keySet()) + ";" + treeMap.get(Collections.min(treeMap.keySet())).get(0)
//                        + " " + Collections.max(treeMap.keySet()) + ";" + treeMap.get(Collections.max(treeMap.keySet())).get(0));
            }
        }
    }
}
