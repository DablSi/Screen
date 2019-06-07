package com.example.ducks.screen;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.net.ssl.*;
import java.io.*;
import java.security.cert.CertificateException;
import java.util.Timer;
import java.util.TimerTask;

public class Search extends AppCompatActivity {
    private RelativeLayout relativeLayout;
    boolean isClicked = false, isTrue = true;
    public static String URL = "https://cloud.itx.ru:444/Server-0.0.1-SNAPSHOT/";
    private String android_id;
    private int color1, color2;
    public static Integer room;
    private Response<ResponseBody> responseBody;
    private long timeStart = 0;
    private PowerManager.WakeLock wakeLock;

    //для полноэкранного режима
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        PowerManager powerManager = (PowerManager) Search.this.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "screen:logtag");
        wakeLock.acquire();
        //отключение блокировки экрана

        EditText editText = findViewById(R.id.editText);
        relativeLayout = findViewById(R.id.ll);
        TextView textView = findViewById(R.id.textView);
        android_id = android.provider.Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        floatingActionButton.bringToFront();
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    Search.room = Integer.parseInt(editText.getText().toString());
                    relativeLayout.removeView(editText);
                    relativeLayout.removeView(floatingActionButton);
                    relativeLayout.removeView(textView);
                    hideSystemUI();
                    SendThread sendThread = new SendThread();
                    sendThread.start();
                }
            }
        });
    }

    class SendThread extends Thread {

        @Override
        public void run() {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .client(getUnsafeOkHttpClient().build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            Service service = retrofit.create(Service.class);
            Call<Void> call = service.putDevice(android_id, room, null);
            try {
                call.execute();
                //добавление телефона в комнату
                Log.d("SEND_AND_RETURN", "Ready.");
                GetThread getThread = new GetThread();
                getThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Call<int[]> call2 = service.getColor(android_id);
            try {
                Response<int[]> colorResponse = call2.execute();
                color1 = colorResponse.body()[0];
                color2 = colorResponse.body()[1];
                //получение цветов
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        relativeLayout.setBackgroundColor(color1);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    class GetThread extends Thread {
        long time = 0;

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .client(getUnsafeOkHttpClient().build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);
            DownloadThread downloadThread = new DownloadThread();
            downloadThread.start();
            while (time <= (System.currentTimeMillis() + 100)) {
                Call<Long> call = service.getTime(android_id);
                try {
                    Response<Long> userResponse = call.execute();
                    time = userResponse.body();
                    //получение времени запуска видео
                    if (time <= (System.currentTimeMillis() + 100))
                        Thread.sleep(150);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d("SEND_AND_RETURN", "" + (time - (System.currentTimeMillis() + (int) Sync.deltaT)));
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            relativeLayout.setBackgroundColor(color2);
                        }
                    });
                    Service.Coords coords = null;
                    try {
                        while (coords == null || coords.y2 == -1) {
                            Call<Service.Coords> call = service.getCoords(android_id);
                            Response<Service.Coords> response = call.execute();
                            coords = response.body();
                            Thread.sleep(150);
                        }
                        Video.ax = coords.x1;
                        Video.bx = coords.x2;
                        Video.ay = coords.y1;
                        Video.by = coords.y2;
                        //получение координат
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                relativeLayout.setBackgroundColor(Color.WHITE);
                                Toast.makeText(Search.this, "Жду время", Toast.LENGTH_LONG).show();
                            }
                        });
                        Call<Long> call = null;
                        Call<ResponseBody> videoCall = null;
                        while (timeStart < System.currentTimeMillis()) {
                            call = service.getStartVideo(android_id);
                            Response<Long> response = call.execute();
                            timeStart = response.body();
                            //получение времени начала видео
                            Thread.sleep(150);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Search.this, "Время получено", Toast.LENGTH_LONG).show();
                            }
                        });
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Video.mMediaPlayer.start();
                                //начало видео
                            }
                        }, timeStart - (System.currentTimeMillis() + (int) Sync.deltaT));
                        while (Video.path == null) {
                            Thread.sleep(150);
                        }
                        startActivity(new Intent(Search.this, Video.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, time - (System.currentTimeMillis() + (int) Sync.deltaT) - 80);
        }
    }

    private class DownloadThread extends Thread {

        @Override
        public void run() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .client(getUnsafeOkHttpClient().build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Service service = retrofit.create(Service.class);

            Call<ResponseBody> call2 = service.getFile(room);
            call2.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    responseBody = response;
                    VideoThread videoThread = new VideoThread();
                    videoThread.start();
                    //видео получено
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("VIDEO", t.getMessage());
                }
            });
        }
    }

    private class VideoThread extends Thread {
        @Override
        public void run() {
            try {
                byte[] video = responseBody.body().bytes();
                File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                File file = new File(storageDirectory, "Screen.mp4");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(video);
                Video.path = file.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // здесь получала SSLHandshakeException для URL из частного облака https://cloud.itx.ru
    // решение в создании TrustManager который не проверяет цепочку сертификатов HTTPS
    // см https://mobikul.com/android-retrofit-handling-sslhandshakeexception/
    public static OkHttpClient.Builder getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            return builder;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
