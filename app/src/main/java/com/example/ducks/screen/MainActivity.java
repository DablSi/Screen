package com.example.ducks.screen;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.text.*;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public RelativeLayout relativeLayout;
    TimePicker timePicker;
    TextView textView;
    static boolean isStarted = false;
    private boolean isPressed = false;
    int i = 1;
    private EditText et1, et2, et3, et4;
    private Spinner spinner;
    private Timer timer;
    private ArrayAdapter<CharSequence> adapter;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 0;

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                Uri selectedImageUri = data.getData();
                // MEDIA GALLERY
                String selectedImagePath = getPath(selectedImageUri);
                if (selectedImagePath != null) {
                    Log.e("FILE", selectedImagePath);
                    Video.path = selectedImagePath;
                }
            }
        }
    }

    private String getPath(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            //!!! какая та страннность MediaStore.Video.Media.DATA на новом телефоне возвращает null
            String[] projection = {MediaStore.Video.Media.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                cursor.moveToFirst();
                String index = cursor.getString(column_index);
                cursor.close();
                //!!!
                String DownloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                String file = DownloadDirectory + "/" + index;
                return file;
            } else
                return null;
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        MenuItem item = menu.findItem(R.id.spinner);
        spinner = (Spinner) item.getActionView();

        adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_list_item_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,
                                       View itemSelected, int selectedItemPosition, long selectedId) {

                String[] choose = getResources().getStringArray(R.array.spinner_list_item_array);
                if (choose[selectedItemPosition].equals("Файл")) {
                    showFileChooser();
                }
                else if(choose[selectedItemPosition].equals("Фото")){
                    startActivity(new Intent(MainActivity.this, Camera.class));
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NewThread newThread = new NewThread();
        newThread.execute();
        et1 = findViewById(R.id.edit1);
        et2 = findViewById(R.id.edit2);
        et3 = findViewById(R.id.edit3);
        et4 = findViewById(R.id.edit4);
        relativeLayout = findViewById(R.id.ll);
        timePicker = findViewById(R.id.timePicker);
        textView = findViewById(R.id.textView);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        /*textView1 = findViewById(R.id.textView1);
        textView1.setText("Размер экрана:" + size.x + "*" + size.y);*/

        ChangeText changeText = new ChangeText(Integer.MAX_VALUE, 1000);
        changeText.start();
        timePicker.setIs24HourView(true);

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy-", Locale.getDefault());
                    Date date = new Date();
                    Date runIn = new SimpleDateFormat("dd.MM.yyyy-HH:mm", Locale.getDefault()).parse(formatter.format(date) + i + ":" + i1);
                    long dif = runIn.getTime() - (System.currentTimeMillis() + (int)Sync.deltaT);
                    if (dif <= 0) {
                        Toast.makeText(getApplicationContext(), getString(R.string.time_less), Toast.LENGTH_SHORT).show();
                    } else {
                        MyTimer myTimer = new MyTimer();
                        timer = new Timer();
                        Toast.makeText(getApplicationContext(), getString(R.string.time_more) + " " + dif + " " + getString(R.string.mls), Toast.LENGTH_SHORT).show();
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
                if (!(et1.getText().toString().equals("") ||
                        et2.getText().toString().equals("") ||
                        et3.getText().toString().equals("") ||
                        et4.getText().toString().equals(""))) {
                    Video.ax = Integer.parseInt(et1.getText().toString());
                    Video.ay = Integer.parseInt(et2.getText().toString());
                    Video.bx = Integer.parseInt(et3.getText().toString());
                    Video.by = Integer.parseInt(et4.getText().toString());
                }
                isPressed = true;
                Intent intent1 = new Intent(MainActivity.this, Video.class);
                startActivity(intent1);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(spinner != null && adapter != null){
            spinner.setAdapter(adapter);
        }
    }

    class ChangeText extends CountDownTimer {

        public ChangeText(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            //!!!
            SimpleDateFormat output = new SimpleDateFormat("HH:mm:ss");
            //!!!
            textView.setText("Server Time:" + output.format(new Date((int) Sync.deltaT + System.currentTimeMillis())) /*+ " delta:" + Sync.deltaT*/);
        }

        @Override
        public void onFinish() {

        }
    }

    class MyTimer extends TimerTask {
        @Override
        public void run() {
            if (isPressed) {
                //if (!isStarted)
                    Video.mMediaPlayer.start();
                isStarted = true;
            }
        }

    }

    class NewThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if(!Sync.isStarted)
                startService(new Intent(MainActivity.this, Sync.class));
            //startService(new Intent(MainActivity.this, Autorun.class));
            return null;
        }
    }


}