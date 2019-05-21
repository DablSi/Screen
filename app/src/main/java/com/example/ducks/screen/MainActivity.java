package com.example.ducks.screen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.*;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.ducks.screen.Search.URL;


public class MainActivity extends AppCompatActivity {

    public RelativeLayout relativeLayout;
    TimePicker timePicker;
    TextView textView;
    private TextView textView1;
    static boolean isStarted = false;
    private boolean isPressed = false;
    int i = 1;
    public static int room = -1;
    private EditText et1, et2, et3, et4;
    private Spinner spinner;
    private Timer timer;
    private ArrayAdapter<CharSequence> adapter;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 0;
    public static byte[] video;
    protected static String android_id;
    private long time;

    private void showFileChooser() {
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
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
                    video = new byte[(int) new File(selectedImagePath).length()];
                    try {
                        new FileInputStream(new File(selectedImagePath)).read(video);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        RoomAsync roomAsync = new RoomAsync();
        roomAsync.start();
    }

    class RoomAsync extends Thread {

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);
            Call<Integer> integerCall = service.getRoom();
            try {
                Response<Integer> integerResponse = integerCall.execute();
                room = integerResponse.body();
                Call<Void> call = service.putDevice(android_id, MainActivity.room, null);
                call.execute();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView1.setText(getString(R.string.roomNum) + room);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getPath(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
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
                } else if (choose[selectedItemPosition].equals("Фото")) {
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
        android_id = android.provider.Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        textView1 = findViewById(R.id.textView1);
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
        //textView1.setText("Размер экрана:" + size.x + "*" + size.y);

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
                    long dif = runIn.getTime() - (System.currentTimeMillis() + (int) Sync.deltaT);
                    if (dif <= 0) {
                        Toast.makeText(getApplicationContext(), getString(R.string.time_less), Toast.LENGTH_SHORT).show();
                    } else if(room != -1){
                        time = runIn.getTime();
                        new SendTime().start();
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
        if (spinner != null && adapter != null) {
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
            textView.setText("Время:" + output.format(new Date((int) Sync.deltaT + System.currentTimeMillis())) /*+ " delta:" + Sync.deltaT*/);
        }

        @Override
        public void onFinish() {

        }
    }
    class NewThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (!Sync.isStarted)
                startService(new Intent(MainActivity.this, Sync.class));
            //startService(new Intent(MainActivity.this, Autorun.class));
            return null;
        }
    }

    class SendTime extends Thread{
        @Override
        public void run() {
            super.run();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Call<Void> call = retrofit.create(Service.class).putStartVideo(room,  time);
            try {
                call.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}