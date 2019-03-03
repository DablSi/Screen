package com.example.ducks.screen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
            Intent pushIntent = new Intent(context, Sync.class);
            context.startService(pushIntent);
    }
}