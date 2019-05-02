package com.example.ducks.screen;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static android.content.ContentValues.TAG;

public class Sync extends Service {
    public static boolean isStarted = false;
    int D = 0;
    static float deltaT = 0;
    public static String date;
    static long t1, t2, t3;
    public Socket socket;

    private SyncThread syncThread;

    public Sync() {
        isStarted = true;
    }

    @Override
    public void onCreate() {
        // сообщение о создании службы
//        Toast.makeText(this, "Service created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // сообщение о запуске службы
//        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();

        // создаем объект нашего AsyncTask (необходимо для работы с сетью)
        syncThread = new SyncThread();
        syncThread.execute();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        //сообщение об остановке службы
        //Toast.makeText(this, "Service stoped", Toast.LENGTH_SHORT).show();
        startService(new Intent(this, Sync.class));
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
            if(socket == null){
                startService(new Intent(Sync.this, Sync.class));
                isStarted = false;
                stopService(new Intent(Sync.this, Sync.class));
                return null;
            }
            Log.e("Everything is fine: ", "Connected");
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
                    D = (int) (t2 - (t1 + t3) / 2);
                    deltaT += (float)D / 10;
                    Log.d(TAG, Long.toString((int) deltaT));
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // close the connection
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