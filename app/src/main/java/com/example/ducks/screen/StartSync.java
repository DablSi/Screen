package com.example.ducks.screen;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class StartSync extends Service {

    private OnBoot onBoot;

    private class OnBoot extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            startService(new Intent(arg0, Sync.class));
        }

    }

    public StartSync() {

    }

    public void onCreate() {
        super.onCreate();
        onBoot = new OnBoot();
        startService(new Intent(this, Sync.class));
        this.registerReceiver(this.onBoot,
                new IntentFilter(Intent.ACTION_BOOT_COMPLETED));
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_REDELIVER_INTENT;
    }

    public void onDestroy() {
        super.onDestroy();

    }

    public IBinder onBind(Intent intent) {

        return null;
    }
}