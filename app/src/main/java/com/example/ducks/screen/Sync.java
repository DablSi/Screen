package com.example.ducks.screen;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;
import static android.media.MediaPlayer.SEEK_CLOSEST;

public class Sync extends Service {
    public int D = 0;
    public int deltaT = 0;
    private int timer = 0;
    int i = 1, x = 0, y = 0, width = 640, height = 360;
    private SyncThread syncThread;

    public Sync() {
    }

    @Override
    public void onCreate() {
        // сообщение о создании службы
        Toast.makeText(this, "Service created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // сообщение о запуске службы
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();

        // создаем объект нашего AsyncTask (необходимо для работы с сетью)
        syncThread = new SyncThread();
        syncThread.execute();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        //сообщение об остановке службы
        Toast.makeText(this, "Service stoped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class SyncThread extends AsyncTask<Void, Void, Void> {
        private long t1, t2, t3;

        public SyncThread() {

        }

        @Override
        protected Void doInBackground(Void... voids) {
            Socket socket = null;
            try {
                socket = new Socket("178.79.155.166", 5001);
            } catch (IOException e) {
                e.printStackTrace();
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
                t1 = System.currentTimeMillis();
                outputStream.writeLong(t1);
                outputStream.flush();
                t1 = input.readLong();
                t2 = input.readLong();
                t3 = System.currentTimeMillis();
                D = (int) (t2 - (t1 + (t2 + deltaT)) / 2);
                deltaT = Math.abs(D) > 10 ? deltaT + D / 10 : deltaT + D;
                Log.e(TAG, Long.toString(deltaT));
                // здесь ввожу время когда запустить то, что нужно (видео)
                SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-");
                Date date = new Date();
                Date runTo = new SimpleDateFormat("dd.MM.yyyy-HH:mm").parse(formatter.format(date) + "19:13");
                long t = runTo.getTime() - deltaT;
                Log.e(TAG, Long.toString(t - System.currentTimeMillis()));
                MyTimer myTimer = new MyTimer();
                Timer timer = new Timer();
                timer.schedule(myTimer, 0, 10);

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

    class MyTimer extends TimerTask {

        @Override
        public void run() {
            try {
                // здесь делаем то что нужно запустить одновременно
                Bitmap bm = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("out" + i, "drawable", getPackageName())));
                width = bm.getWidth();
                height = bm.getHeight();
                bm = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("out" + i, "drawable", getPackageName())), x, y, width / 2, height);
                BitmapDrawable bd = new BitmapDrawable(getResources(), bm);
                VideoActivity.ll.setBackground(bd);
                i++;
                if (i > 858)
                    i = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
