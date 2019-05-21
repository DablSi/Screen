package com.example.ducks.screen;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Currency;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class Search extends AppCompatActivity {
    RelativeLayout relativeLayout;
    boolean isClicked = false, isTrue = true;
    public static String URL = "https://server-screen.herokuapp.com/";
    private String android_id;
    private int color = 0x0ff000000;
    public static Integer room;
    private FragmentTransaction transaction;
    private Fragment newFragment;

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
                    relativeLayout.setBackgroundColor(color);
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
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
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

            Call<Integer> call2 = service.getColor(android_id);
            try {
                Response<Integer> colorResponse = call2.execute();
                color = colorResponse.body();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class GetThread extends Thread {
        Long time = null;

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);
            while (time == null) {
                Call<Long> call = service.getTime(android_id);
                try {
                    Response<Long> userResponse = call.execute();
                    time = userResponse.body();
                    if (time != null)
                        Log.d("SEND_AND_RETURN", "" + (time - (System.currentTimeMillis() + (int) Sync.deltaT)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            relativeLayout.setBackgroundColor(color);
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Service.Coords coords = null;
                    try {
                        while (coords == null || coords.y2 == -1) {
                            Call<Service.Coords> call = service.getCoords(android_id);
                            Response<Service.Coords> response = call.execute();
                            coords = response.body();
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
                        Long time = null;
                        Call<Long> call = null;
                        long dif = 0;
                        while (dif <= 0) {
                            call = service.getStartVideo(android_id);
                            Response<Long> response = call.execute();
                            time = response.body();
                            if (time != null)
                                dif = time - (System.currentTimeMillis() + (int) Sync.deltaT);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Search.this, "Время получено", Toast.LENGTH_LONG).show();
                            }
                        });
                        Timer timer = new Timer();
                        Toast.makeText(getApplicationContext(), getString(R.string.time_more) + " " + dif + " " + getString(R.string.mls), Toast.LENGTH_SHORT).show();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Video.mMediaPlayer.start();
                            }
                        }, dif);
                        startActivity(new Intent(Search.this, Video.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, time - (System.currentTimeMillis() + (int) Sync.deltaT) - 110);
        }
    }
}
