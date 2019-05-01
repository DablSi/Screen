package com.example.ducks.screen;

import android.graphics.Color;
import android.os.AsyncTask;
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
import java.util.concurrent.ExecutionException;

public class Search extends AppCompatActivity {
    LinearLayout linearLayout;
    boolean isClicked = false, isTrue = true;
    private static String URL = "http://192.168.1.8:8080/";
    private String android_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
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
            Call<Void> call = service.put(android_id, 0, null /*System.currentTimeMillis() + (int)Sync.deltaT + 10000*/);
            try {
                call.execute();
                Log.d("SEND_AND_RETURN", "Ready.");
                GetThread getThread = new GetThread();
                getThread.start();
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
                Call<Long> call = service.get(android_id);
                try {
                    Response<Long> userResponse = call.execute();
                    time = userResponse.body();
                    if (time != null)
                        Log.d("SEND_AND_RETURN", "" + (time - (System.currentTimeMillis() + (int) Sync.deltaT)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(time - (System.currentTimeMillis() + (int) Sync.deltaT) - 120);
                linearLayout = findViewById(R.id.ll);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        linearLayout.setBackgroundColor(0xff303f9f);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
