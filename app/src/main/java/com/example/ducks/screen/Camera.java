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
import android.graphics.drawable.GradientDrawable;
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
import android.widget.Toast;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.example.ducks.screen.MainActivity.android_id;
import static com.example.ducks.screen.MainActivity.video;
import static com.example.ducks.screen.Search.URL;

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
    private long t;
    private OrientationListener orientationListener;
    private int rotate;
    private int xs = 640, ys = 360;
    private FloatingActionButton floatingActionButton;

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            // Set the content to appear under the system bars so that the
                            // content doesn't resize when the system bars hide and show.
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            // Hide the nav bar and status bar
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

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
                    previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), size.x, size.y);
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
        hideSystemUI();
        orientationListener = new OrientationListener(Camera.this);
        floatingActionButton = findViewById(R.id.floatingActionButton);
        if (MainActivity.room < 0) {
            Toast.makeText(Camera.this, "Прежде выбирете файл!", Toast.LENGTH_LONG).show();
            finish();
        }
        textureView = findViewById(R.id.texture_view);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
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
    protected void onStart() {
        orientationListener.enable();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (textureView.isAvailable()) {
            setUpCamera();
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
        orientationListener.disable();
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
            }, t - (System.currentTimeMillis() + (int) Sync.deltaT));
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bitmap2 = textureView.getBitmap();
                    new CordThread().start();
                }
            }, t - (System.currentTimeMillis() + (int) Sync.deltaT) + 45);
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

            Call<Void> call = service.putDevice(android_id, MainActivity.room, t);
            try {
                call.execute();
                Log.d("SEND_AND_RETURN", "" + (t - (System.currentTimeMillis() + (int) Sync.deltaT)));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (video != null) {
                String send = new String(video);
                Call<Void> videoCall = service.putVideo(send, MainActivity.room);
                try {
                    videoCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    class CordThread extends Thread {

        @Override
        public void run() {
            int orientation = Camera.this.getResources().getConfiguration().orientation;
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//            bitmap = Bitmap.createScaledBitmap(bitmap, xs, ys, false);
            bitmap2 = bitmap2.copy(Bitmap.Config.ARGB_8888, true);
            bitmap2 = Bitmap.createBitmap(bitmap2, 0, 0, bitmap2.getWidth(), bitmap2.getHeight(), matrix, true);
//            bitmap2 = Bitmap.createScaledBitmap(bitmap2, xs, ys, false);

            Bitmap bitmap3 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap3 = bitmap3.copy(Bitmap.Config.ARGB_8888, true);


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
                            bitmap3.setPixel(j, i, Color.GREEN);
                            linkedList2.add(new Point(j, i));
                        }

                        int need = 0xff00ff00;
                        float[] hsv2 = new float[3];
                        Color.RGBToHSV(Color.red(need), Color.green(need), Color.blue(need), hsv2);
                        if (Math.abs(hsv[0] - hsv2[0]) <= 25 && Math.abs(hsv[1] - hsv2[1]) <= 0.60 && Math.abs(hsv[2] - hsv2[2]) <= 0.60) {
                            linkedList.add(new Point(j, i));
                            bitmap3.setPixel(j, i, Color.RED);
                        }
                    }
                }
            }

            Comparator<Point> xComparator = new Comparator<Point>() {
                @Override
                public int compare(Point o1, Point o2) {
                    return o1.x - o2.x;
                }
            };
            Comparator<Point> yComparator = new Comparator<Point>() {
                @Override
                public int compare(Point o1, Point o2) {
                    return o1.y - o2.y;
                }
            };

            int left1 = 0, left2 = 0, right1 = 0, right2 = 0, up1 = 0, up2 = 0, down1 = 0, down2 = 0;
            Point size = new Point(bitmap.getWidth(), bitmap.getHeight());

            if (linkedList.size() > 0) {
                Collections.sort(linkedList, xComparator);
                left1 = linkedList.getFirst().x;
                right1 = linkedList.getLast().x;
                Collections.sort(linkedList, yComparator);
                up1 = linkedList.getFirst().y;
                down1 = linkedList.getLast().y;

                Bitmap bitmap4 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap4 = bitmap4.copy(Bitmap.Config.ARGB_8888, true);

                bitmap4.setPixel(left1, up1, Color.RED);
                bitmap4.setPixel(left1, down1, Color.RED);
                bitmap4.setPixel(right1, up1, Color.RED);
                bitmap4.setPixel(right1, down1, Color.RED);

                for (int i = left1; i < right1; i++) {
                    bitmap4.setPixel(i, up1, Color.RED);
                    bitmap4.setPixel(i, down1, Color.RED);
                }

                for (int i = up1; i < down1; i++) {
                    bitmap4.setPixel(left1, i, Color.RED);
                    bitmap4.setPixel(right1, i, Color.RED);
                }

                Log.e("Coords", left1 + ";" + up1 + " " + right1 + ";" + down1);
            }
            if (linkedList2.size() > 0) {
                Collections.sort(linkedList2, xComparator);
                left2 = linkedList2.getFirst().x;
                right2 = linkedList2.getLast().x;
                Collections.sort(linkedList2, yComparator);
                up2 = linkedList2.getFirst().y;
                down2 = linkedList2.getLast().y;

                Bitmap bitmap4 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap4 = bitmap4.copy(Bitmap.Config.ARGB_8888, true);

                bitmap4.setPixel(left2, up2, Color.GREEN);
                bitmap4.setPixel(left2, down2, Color.GREEN);
                bitmap4.setPixel(right2, up2, Color.GREEN);
                bitmap4.setPixel(right2, down2, Color.GREEN);

                for (int i = left2; i < right2; i++) {
                    bitmap4.setPixel(i, up2, Color.GREEN);
                    bitmap4.setPixel(i, down2, Color.GREEN);
                }

                for (int i = up2; i < down2; i++) {
                    bitmap4.setPixel(left2, i, Color.GREEN);
                    bitmap4.setPixel(right2, i, Color.GREEN);
                }

                Log.e("Coords2", left2 + ";" + up2 + " " + right2 + ";" + down2);
            }

            /*if (up1 <= up2) {
                up2 -= up1;
                down1 -= up1;
                down2 -= up1;
                size.y -= up1;
                up1 = 0;
            } else {
                up1 -= up2;
                down2 -= up2;
                down1 -= up2;
                size.y -= up2;
                up2 = 0;
            }
            if (left1 <= left2) {
                left2 -= left1;
                right1 -= left1;
                right2 -= left1;
                size.x -= left1;
                left1 = 0;
            } else {
                left1 -= left2;
                right2 -= left2;
                right1 -= left2;
                size.x -= left2;
                left2 = 0;
            }*/

            left1 /= (size.x / 100);
            up1 /= (size.y / 100);
            right1 /= (size.x / 100);
            down1 /= (size.y / 100);
            left2 /= (size.x / 100);
            up2 /= (size.y / 100);
            right2 /= (size.x / 100);
            down2 /= (size.y / 100);
            Log.e("Coords", left1 + ";" + up1 + " " + right1 + ";" + down1);
            Log.e("Coords2", left2 + ";" + up2 + " " + right2 + ";" + down2);
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);

            Call<Void> call = service.putCoords(MainActivity.room, left1, up1, right1, down1, 0xff00ff00);
            Call<Void> call2 = service.putCoords(MainActivity.room, left2, up2, right2, down2, 0xff303f9f);
            try {
                call.execute();
                call2.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private class OrientationListener extends OrientationEventListener {
        final int ROTATION_O = 1;
        final int ROTATION_90 = 2;
        final int ROTATION_180 = 3;
        final int ROTATION_270 = 4;

        private int rotation = 0;

        public OrientationListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (((orientation < 35 && orientation > 0) || orientation > 325) && rotation != ROTATION_O) { // PORTRAIT
                rotation = ROTATION_O;
                rotate = 0;
            } else if (orientation > 55 && orientation < 125 && rotation != ROTATION_270) { // REVERSE LANDSCAPE
                rotation = ROTATION_270;
                rotate = 90;
            } else if (orientation > 235 && orientation < 305  && rotation != ROTATION_90) { //LANDSCAPE
                rotation = ROTATION_90;
                rotate = -90;
            }
            floatingActionButton.setRotation(-rotate);
        }
    }
}
