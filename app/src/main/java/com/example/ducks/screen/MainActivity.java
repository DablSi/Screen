package com.example.ducks.screen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;

import static com.example.ducks.screen.Search.URL;
import static com.example.ducks.screen.Search.getUnsafeOkHttpClient;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_TAKE_GALLERY_VIDEO = 0, REQUEST_START_CAMERA_ACTIVITY = 1;
    public static byte[] video;
    public static int room = -1;
    private TextView textView1;
    private static boolean isUploaded = false;
    protected static String android_id;
    private Button button;

    //открывает проводник для выбора файла
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
    }

    //открывает таймер, если фото было удачным
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                Uri selectedImageUri = data.getData();
                String selectedImagePath = getPath(selectedImageUri);
                if (selectedImagePath != null) {
                    Log.e("FILE", selectedImagePath);
                    Video.path = selectedImagePath;
                    RoomAsync roomAsync = new RoomAsync();
                    roomAsync.start();
                }
            }
        }
        if (requestCode == REQUEST_START_CAMERA_ACTIVITY && resultCode == REQUEST_START_CAMERA_ACTIVITY) {
            startActivity(new Intent(MainActivity.this, Timer.class));
        }
    }

    class RoomAsync extends Thread {

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .client(getUnsafeOkHttpClient().build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);
            Call<Integer> integerCall = service.getRoom();
            try {
                Response<Integer> integerResponse = integerCall.execute();
                room = integerResponse.body(); //получение номера комнаты
                Call<Void> call = service.putDevice(android_id, room, null);
                call.execute(); //создание комнаты
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Подождите, видео загружается", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            File file = new File(Video.path);
            RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
            MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("video", file.getName(), requestBody);
            //загрузка видео на сервер
            service.uploadVideo(fileToUpload, room).enqueue(new Callback<Void>() {

                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Видео загрузилось", Toast.LENGTH_LONG).show();
                            textView1.setText(getString(R.string.roomNum) + room);
                            button.setText("Сфотографировать видеостену");
                            isUploaded = true;
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivityForResult(new Intent(MainActivity.this, Camera.class), REQUEST_START_CAMERA_ACTIVITY);
                                }
                            });
                        }
                    });
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {

                }
            });
        }
    }

    //получение пути из URI
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new ServiceThread().execute();
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
        button = findViewById(R.id.start);
        textView1 = findViewById(R.id.textView1);
        android_id = android.provider.Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if (room != -1 && isUploaded)
            textView1.setText(getString(R.string.roomNum) + room);
        if (!isUploaded)
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFileChooser();
                }
            });
        else{
            button.setText("Сфотографировать видеостену");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(MainActivity.this, Camera.class), REQUEST_START_CAMERA_ACTIVITY);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (room != -1 && isUploaded)
            textView1.setText(getString(R.string.roomNum) + room);
    }

    class ServiceThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (!Sync.isStarted)
                startService(new Intent(MainActivity.this, Sync.class));
            startService(new Intent(MainActivity.this, Autorun.class));
            //запуск сервисов
            return null;
        }
    }
}
