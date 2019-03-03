package com.example.ducks.screen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static android.content.pm.PackageManager.*;

public class VideoAssetActivity extends Activity implements TextureView.SurfaceTextureListener {
    // Log tag
    private static final String TAG = VideoAssetActivity.class.getName();
    // Asset video file name
    private float mVideoWidth;
    private float mVideoHeight;
    private float mDisplayWidth;
    private float mDisplayHeight;
    static int ax, ay, bx, by;
    // MediaPlayer instance to control playback of video file.
    static MediaPlayer mMediaPlayer;
    private TextureView mTextureView;
    static String path;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.texture_video_crop);
        calculateVideoSize();
        initView();
    }

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

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        hideSystemUI();
        mTextureView = findViewById(R.id.textureView);
        // SurfaceTexture is available only after the TextureView
        // is attached to a window and onAttachedToWindow() has been invoked.
        // We need to use SurfaceTextureListener to be notified when the SurfaceTexture
        // becomes available.
        mTextureView.setSurfaceTextureListener(this);
        FrameLayout rootView = findViewById(R.id.rootView);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        mDisplayWidth = size.x;
        mDisplayHeight = size.y;

        updateTextureViewSize();

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (MainActivity.isStarted) {
                        if (mMediaPlayer.isPlaying()) {
                            showSystemUI();
                            mMediaPlayer.pause();
                        } else {
                            hideSystemUI();
                            mMediaPlayer.start();
                        }
                    }
                }
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            // Make sure we stop video and release resources when activity is destroyed.
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void calculateVideoSize() {
        try {
            FileDescriptor fd = new FileInputStream(path).getFD();
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(fd);
            String height = metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String width = metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            mVideoHeight = Float.parseFloat(height);
            mVideoWidth = Float.parseFloat(width);

        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        } catch (NumberFormatException e) {
            Log.d(TAG, e.getMessage());
        }
    }


    private void updateTextureViewSize() {
        float scaleX = mDisplayWidth / mVideoWidth, scaleY = mDisplayHeight / mVideoHeight;
        float scale = mDisplayHeight / Math.abs(by - ay);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(scale / scaleX, scale / scaleY);
        //Без " / scaleX" результаты те же, что и с прошлым кодом
        matrix.postTranslate(-scale * ax, -scale * ay);
        mTextureView.setLayoutParams(new FrameLayout.LayoutParams((int) mDisplayWidth, (int) mDisplayHeight));
        mTextureView.setTransform(matrix);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        Surface surface = new Surface(surfaceTexture);

        try {
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            }

            mMediaPlayer = new MediaPlayer();

            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(path);
                mMediaPlayer.setDataSource(fileInputStream.getFD());
                fileInputStream.close();
                mMediaPlayer.prepare();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setLooping(true);

            // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
            // creating MediaPlayer
            mMediaPlayer.prepareAsync();

        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "permission_storage_success",
                        Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this,
                        "permission_storage_failure",
                        Toast.LENGTH_SHORT).show();
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            return;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}
