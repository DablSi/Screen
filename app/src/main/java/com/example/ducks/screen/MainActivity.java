package com.example.ducks.screen;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.text.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public RelativeLayout relativeLayout;
    TimePicker timePicker;
    TextView textView;
    boolean isPressed = false;
    int i = 1;
    EditText et1, et2, et3, et4;
    Timer timer;
    private static final int FILE_SELECT_CODE = 0;

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d("FILE", "File path: " + uri.getPath());
                    VideoAssetActivity.uri = uri;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(MainActivity.this, Sync.class));
        showFileChooser();
        et1 = findViewById(R.id.edit1);
        et2 = findViewById(R.id.edit2);
        et3 = findViewById(R.id.edit3);
        et4 = findViewById(R.id.edit4);
        relativeLayout = findViewById(R.id.ll);
        timePicker = findViewById(R.id.timePicker);
        textView = findViewById(R.id.textView);

        ChangeText changeText = new ChangeText(Integer.MAX_VALUE, 500);
        changeText.start();
        timePicker.setIs24HourView(true);

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-");
                    Date date = new Date();
                    Date runIn = new SimpleDateFormat("dd.MM.yyyy-HH:mm").parse(formatter.format(date) + i + ":" + i1);
                    long dif = runIn.getTime() - (System.currentTimeMillis() + Sync.deltaT);
                    if(dif <= 0){
                        Toast.makeText(getApplicationContext(), "Время меньше серверного!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        MyTimer myTimer = new MyTimer();
                        timer = new Timer();
                        Toast.makeText(getApplicationContext(), "Видео запустится через " + dif + " миллисекунд" , Toast.LENGTH_SHORT).show();
                        timer.schedule(myTimer, dif);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        Button button = findViewById(R.id.ready);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VideoAssetActivity.ax = Integer.parseInt(et1.getText().toString());
                VideoAssetActivity.ay = Integer.parseInt(et2.getText().toString());
                VideoAssetActivity.bx = Integer.parseInt(et3.getText().toString());
                VideoAssetActivity.by = Integer.parseInt(et4.getText().toString());
                isPressed = true;
                Intent intent1 = new Intent(MainActivity.this, VideoAssetActivity.class);
                startActivity(intent1);
            }
        });
    }

    class ChangeText extends CountDownTimer {

        public ChangeText(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            textView.setText("Server time: " + new Date(Sync.deltaT + System.currentTimeMillis()).toString());
        }

        @Override
        public void onFinish() {

        }
    }

    class MyTimer extends TimerTask {
        @Override
        public void run() {
            if (isPressed) {
                VideoAssetActivity.mMediaPlayer.start();
            }
        }
    }

}