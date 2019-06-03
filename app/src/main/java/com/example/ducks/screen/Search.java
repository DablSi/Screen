package com.example.ducks.screen;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.*;
import java.net.Socket;
import java.util.Currency;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class Search extends AppCompatActivity {
    RelativeLayout relativeLayout;
    boolean isClicked = false, isTrue = true;
    public static String URL = "http://192.168.1.6:8080";
    private String android_id;
    private int color1, color2;
    public static Integer room;
    private FragmentTransaction transaction;
    private PowerManager.WakeLock wakeLock;
    private Response<ResponseBody> responseBody;
    private Fragment newFragment;
    private long timeStart = 0;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        PowerManager powerManager = (PowerManager) Search.this.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "screen:logtag");
        //wakeLock.acquire();

        EditText editText = findViewById(R.id.editText);
        relativeLayout = findViewById(R.id.ll);
        TextView textView = findViewById(R.id.textView);
        android_id = android.provider.Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        floatingActionButton.bringToFront();
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    Search.room = Integer.parseInt(editText.getText().toString());
                    relativeLayout.removeView(editText);
                    relativeLayout.removeView(floatingActionButton);
                    relativeLayout.removeView(textView);
                    hideSystemUI();
                    SendThread sendThread = new SendThread();
                    sendThread.start();
                }
            }
        });
    }

    class SendThread extends Thread {

        @Override
        public void run() {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            Service service = retrofit.create(Service.class);
            Call<Void> call = service.putDevice(android_id, room, null);
            try {
                call.execute();
                Log.d("SEND_AND_RETURN", "Ready.");
                GetThread getThread = new GetThread();
                getThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Call<int[]> call2 = service.getColor(android_id);
            try {
                Response<int[]> colorResponse = call2.execute();
                color1 = colorResponse.body()[0];
                color2 = colorResponse.body()[1];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        relativeLayout.setBackgroundColor(color1);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (wakeLock.isHeld())
                wakeLock.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class GetThread extends Thread {
        long time = 0;

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);
            DownloadThread downloadThread = new DownloadThread();
            downloadThread.start();
            while (time < System.currentTimeMillis()) {
                Call<Long> call = service.getTime(android_id);
                try {
                    Response<Long> userResponse = call.execute();
                    time = userResponse.body();
                    Thread.sleep(150);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d("SEND_AND_RETURN", "" + (time - (System.currentTimeMillis() + (int) Sync.deltaT)));
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            relativeLayout.setBackgroundColor(color2);
                        }
                    });
                    Service.Coords coords = null;
                    try {
                        while (coords == null || coords.y2 == -1) {
                            Call<Service.Coords> call = service.getCoords(android_id);
                            Response<Service.Coords> response = call.execute();
                            coords = response.body();
                            Thread.sleep(150);
                        }
                        Video.ax = coords.x1;
                        Video.bx = coords.x2;
                        Video.ay = coords.y1;
                        Video.by = coords.y2;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                relativeLayout.setBackgroundColor(Color.WHITE);
                                Toast.makeText(Search.this, "Жду время", Toast.LENGTH_LONG).show();
                            }
                        });
                        Call<Long> call = null;
                        Call<ResponseBody> videoCall = null;
                        while (timeStart == 0) {
                            call = service.getStartVideo(android_id);
                            Response<Long> response = call.execute();
                            timeStart = response.body();
                            Thread.sleep(150);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Search.this, "Время получено", Toast.LENGTH_LONG).show();
                            }
                        });
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Video.mMediaPlayer.start();
                            }
                        }, timeStart - (System.currentTimeMillis() + (int) Sync.deltaT));
                        while (Video.path == null) {
                            Thread.sleep(150);
                        }
                        startActivity(new Intent(Search.this, Video.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, time - (System.currentTimeMillis() + (int) Sync.deltaT) - 90);
        }
    }

    private class DownloadThread extends Thread {

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);

            Call<ResponseBody> call2 = service.getFile(room);
            call2.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    responseBody = response;
                    VideoThread videoThread = new VideoThread();
                    videoThread.start();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("VIDEO", t.getMessage());
                }
            });
        }
    }

    private class VideoThread extends Thread {
        @Override
        public void run() {
            try {
                byte[] video = responseBody.body().bytes();
                File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                File file = new File(storageDirectory, "Screen.mp4");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(video);
                Video.path = file.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
