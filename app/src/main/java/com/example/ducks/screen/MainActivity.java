package com.example.ducks.screen;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.text.*;
import java.util.Calendar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(MainActivity.this, Sync.class));
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
                        Toast.makeText(getApplicationContext(), "Время меньше серверного!", Toast.LENGTH_LONG).show();
                    }
                    else {
                        MyTimer myTimer = new MyTimer();
                        timer = new Timer();
                        Toast.makeText(getApplicationContext(), "Видео запустится через " + dif + " миллисекунд" , Toast.LENGTH_LONG).show();
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






/*package com.example.ducks.screen;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(MainActivity.this, Sync.class));
        et1 = findViewById(R.id.edit1);
        et2 = findViewById(R.id.edit2);
        et3 = findViewById(R.id.edit3);
        et4 = findViewById(R.id.edit4);
        relativeLayout = (RelativeLayout) findViewById(R.id.ll);
        timePicker = (TimePicker) findViewById(R.id.timePicker);
        textView = (TextView) findViewById(R.id.textView);
        MyCountDownTimer myCountDownTimer = new MyCountDownTimer(Integer.MAX_VALUE, 1000);
        myCountDownTimer.start();
        Calendar now = Calendar.getInstance();
        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(now.get(Calendar.HOUR_OF_DAY));
        timePicker.setCurrentMinute(now.get(Calendar.MINUTE));
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                if (timer != null) {
                    timer.cancel();
                    i = 1;
                }
                SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-");
                Date date = new Date();
                Date runTo = null;
                try {
                    runTo = new SimpleDateFormat("dd.MM.yyyy-HH:mm").parse(formatter.format(date) + hourOfDay + ":" + minute);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long currentTime = System.currentTimeMillis() + Sync.deltaT;  // вычислили время на сервере
                long destTime = runTo.getTime(); // во сколько, по времени сервера запустить
                Long pause = destTime - currentTime; // вычислил задержку до запуска видео
                if (pause > 0) {
                    MyTimer myTimer = new MyTimer();
                    timer = new Timer();
                    timer.schedule(myTimer, pause);
                    Toast.makeText(MainActivity.this, "Start video about " + pause / 1000 + " seс", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(MainActivity.this, "Время начала находится в прошлом по времени сервера", Toast.LENGTH_SHORT).show();

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

    class MyCountDownTimer extends CountDownTimer {

        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            textView.setText("Time on server: " + new Date(Sync.t2)+"\nDELTA:"+Sync.deltaT);
        }

@Override
public void onFinish() {

        }
        }

class MyTimer extends TimerTask {
    @Override
    public void run() {
        if(isPressed) {
            VideoAssetActivity.mMediaPlayer.start();
        }
    }
}*/