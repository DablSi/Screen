package com.example.ducks.screen;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
    TextView textView, textView1;
    static boolean isStarted = false;
    private boolean isPressed = false;
    int i = 1;
    private EditText et1, et2, et3, et4;
    private Timer timer;
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
                    VideoAssetActivity.path = selectedImagePath;
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
        Spinner spinner = (Spinner) item.getActionView();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
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
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BootCompletedIntentReceiver bootCompletedIntentReceiver = new BootCompletedIntentReceiver();
        this.registerReceiver(bootCompletedIntentReceiver,
                new IntentFilter(Intent.ACTION_BOOT_COMPLETED));
        startService(new Intent(MainActivity.this, Sync.class));
        setContentView(R.layout.activity_main);
        et1 = findViewById(R.id.edit1);
        et2 = findViewById(R.id.edit2);
        et3 = findViewById(R.id.edit3);
        et4 = findViewById(R.id.edit4);
        relativeLayout = findViewById(R.id.ll);
        timePicker = findViewById(R.id.timePicker);
        textView = findViewById(R.id.textView);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        textView1 = findViewById(R.id.textView1);
        textView1.setText("Размер экрана:" + size.x + "*" + size.y);

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
                    long dif = runIn.getTime() - (System.currentTimeMillis() + Sync.deltaT);
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
                    VideoAssetActivity.ax = Integer.parseInt(et1.getText().toString());
                    VideoAssetActivity.ay = Integer.parseInt(et2.getText().toString());
                    VideoAssetActivity.bx = Integer.parseInt(et3.getText().toString());
                    VideoAssetActivity.by = Integer.parseInt(et4.getText().toString());
                }
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
            //!!!
            SimpleDateFormat output = new SimpleDateFormat("HH:mm:ss");
            //!!!
            textView.setText("Server Time:" + output.format(new Date(Sync.deltaT + System.currentTimeMillis())) + " delta:" + Sync.deltaT);
        }

        @Override
        public void onFinish() {

        }
    }

    class MyTimer extends TimerTask {
        @Override
        public void run() {
            if (isPressed) {
                if (!isStarted)
                    VideoAssetActivity.mMediaPlayer.start();
                isStarted = true;
            }
        }

    }

}