package com.example.ducks.screen;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
    LinearLayout linearLayout;
    boolean isClicked = false, isTrue = true;
    public static String URL = "http://192.168.1.8:8080/";
    private String android_id;
    private int color = 0x0ff000000;

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
        hideSystemUI();
        android_id = android.provider.Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        SendThread sendThread = new SendThread();
        sendThread.start();
    }

    class SendThread extends Thread {

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);
            Call<Void> call = service.putDevice(android_id, 0, null /*System.currentTimeMillis() + (int)Sync.deltaT + 10000*/);
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
                    linearLayout = findViewById(R.id.ll);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            linearLayout.setBackgroundColor(color);
                        }
                    });
                }
            }, time - (System.currentTimeMillis() + (int) Sync.deltaT) - 110);
        }
    }
}
