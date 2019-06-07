package com.example.ducks.screen;

import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.*;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.text.*;
import java.util.Date;
import java.util.Locale;

import static com.example.ducks.screen.MainActivity.room;
import static com.example.ducks.screen.Search.URL;
import static com.example.ducks.screen.Search.getUnsafeOkHttpClient;


public class Timer extends AppCompatActivity {

    private RelativeLayout relativeLayout;
    private Button button;
    private TimePicker timePicker;
    private TextView textView;
    static boolean isStarted = false;
    private long time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        relativeLayout = findViewById(R.id.ll);
        timePicker = findViewById(R.id.timePicker);
        textView = findViewById(R.id.textView);
        button = findViewById(R.id.send);
        button.setVisibility(View.INVISIBLE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendTime().start();
                Toast.makeText(Timer.this, "Время отправлено", Toast.LENGTH_LONG);
            }
        });

        Toast.makeText(Timer.this, "Выберите время запуска", Toast.LENGTH_LONG).show();

        ChangeText changeText = new ChangeText(Integer.MAX_VALUE, 1000);
        changeText.start();
        //обновление времени каждую секунду
        timePicker.setIs24HourView(true);

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-", Locale.getDefault());
                    Date date = new Date();
                    Date runIn = new SimpleDateFormat("dd.MM.yyyy-HH:mm", Locale.getDefault()).parse(formatter.format(date) + i + ":" + i1);
                    long dif = runIn.getTime() - (System.currentTimeMillis() + (int) Sync.deltaT);
                    if (dif <= 0) {
                        button.setVisibility(View.INVISIBLE);
                    } else if (room != -1) {
                        time = runIn.getTime();
                        button.setVisibility(View.VISIBLE);
                        //появление кнопки
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    class ChangeText extends CountDownTimer {

        public ChangeText(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            SimpleDateFormat output = new SimpleDateFormat("HH:mm:ss");
            textView.setText("Время: " + output.format(new Date((int) Sync.deltaT + System.currentTimeMillis())));
        }

        @Override
        public void onFinish() {

        }
    }

    class SendTime extends Thread {
        @Override
        public void run() {
            super.run();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .client(getUnsafeOkHttpClient().build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Call<Void> call = retrofit.create(Service.class).putStartVideo(room, time);
            try {
                call.execute();
                //отправка времени
            } catch (IOException e) {
                e.printStackTrace();
            }
            finish();
        }
    }


}