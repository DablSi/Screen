package com.example.ducks.screen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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

public class Video extends Activity implements TextureView.SurfaceTextureListener {
    // Log тэг
    private static final String TAG = Video.class.getName();
    private float mVideoWidth;
    private float mVideoHeight;
    private float mDisplayWidth;
    private float mDisplayHeight;
    static int ax, ay, bx, by;
    // MediaPlayer для проигрывания видео
    static MediaPlayer mMediaPlayer;
    private TextureView mTextureView;
    static String path;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.texture_video_crop);
        calculateVideoSize();
        ax = ax * ((int) mVideoWidth / 100);
        ay = ay * ((int) mVideoHeight / 100);
        bx = bx * ((int) mVideoWidth / 100);
        by = by * ((int) mVideoHeight / 100);
        //перевод координат из процентов
        initView();
    }

    //для полноэкранного режима
    public void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }


    //для неполноэкранного режима
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    //подготовка TextureView
    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        hideSystemUI();

        mTextureView = findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(this);
        FrameLayout rootView = findViewById(R.id.rootView);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        mDisplayWidth = size.x;
        mDisplayHeight = size.y;

        updateTextureViewSize();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            // Освобождаем ресурсы
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    //получение размеров видеофайла
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


    //обрезка видео путем изменения размеров TextureView
    private void updateTextureViewSize() {
        float scaleX = mDisplayWidth / mVideoWidth, scaleY = mDisplayHeight / mVideoHeight;
        float scale = mDisplayHeight / Math.abs(by - ay);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(scale / scaleX, scale / scaleY);
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
            } //получение разрешений

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
            //загрузка видео

            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setLooping(true);

            mMediaPlayer.prepareAsync();
            //обязательно вызывается, если используешь конструктор для создания медиаплеера

        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length != 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Доступ к файлам запрещен!\nРазрешите приложению читать файлы.",
                        Toast.LENGTH_SHORT).show();
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
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
