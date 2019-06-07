package com.example.ducks.screen;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;


public class Sync extends Service {

    public static final String SYNC = "Sync";
    public static boolean isStarted = false;
    public static float deltaT;
    int D;
    public static String date;
    static long t1, t2, t3;
    public Socket socket;


    private SyncThread syncThread;

    public Sync() {
        isStarted = true;
    }

    @Override
    public void onCreate() {
        SharedPreferences sp = getSharedPreferences("Sync", MODE_PRIVATE);
        deltaT = sp.getFloat("deltaT", 0);
        syncThread = new SyncThread();
        syncThread.execute();
        //получение прошлой дельты из sharedPreferences
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                SharedPreferences sp = getSharedPreferences("Sync", MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                edit.putFloat("deltaT", deltaT);
                edit.commit();
            }
        }, 5000, 5000);
        //сохранение дельты в sharedPreferences для быстрой повторной синхронизации
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class SyncThread extends AsyncTask<Void, Void, Void> {

        public SyncThread() {

        }

        @Override
        protected Void doInBackground(Void... voids) {
            socket = null;
            isStarted = true;
            try {
                socket = new Socket("178.79.155.166", 5001);
            } catch (IOException e) {
                Log.e("Everything is bad: ", "Not connected");
            }
            if (socket == null) {
                startService(new Intent(Sync.this, Sync.class));
                isStarted = false;
                stopService(new Intent(Sync.this, Sync.class));
                return null;
            }
            Log.d("Everything is fine: ", "Connected");
            //успешное подключение к серверу
            DataInputStream input = null;
            DataOutputStream outputStream = null;
            try {
                input = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                while (true) {
                    t1 = System.currentTimeMillis() + (int) deltaT;
                    outputStream.writeLong(t1);
                    outputStream.flush();
                    t1 = input.readLong();
                    t2 = input.readLong();
                    t3 = System.currentTimeMillis() + (int) deltaT;
                    int newD = (int) (t2 - (t1 + t3) / 2);
                    D = newD;
                    deltaT += (float) D / 10;
                    //получение дельты времени
                    if (t1 % 1000 == 0) Log.d(SYNC, "delta is " + (int) deltaT);
                    Thread.sleep(250);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // закрытие соединенеия
                try {
                    input.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}