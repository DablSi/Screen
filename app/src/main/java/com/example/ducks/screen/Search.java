package com.example.ducks.screen;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Search extends AppCompatActivity {

    RelativeLayout relativeLayout;
    boolean isClicked = true, isTrue = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ColorThread colorThread = new ColorThread();
        colorThread.start();
    }

    class ColorThread extends Thread {

        @Override
        public void run() {
            /*DataInputStream input = null;
            try {
                input = new DataInputStream(Sync.socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (input.readUTF().equals("Camera")){
                    relativeLayout.setBackgroundColor(0xff303f9f);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }
    }
}
