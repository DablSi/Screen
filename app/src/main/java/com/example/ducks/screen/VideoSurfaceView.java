package com.example.ducks.screen;

import android.content.Context;
import android.graphics.*;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

//данный класс не используется и является запасным прототипом
//на случай, если данной синхронизации видео будет недостаточно
public class VideoSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private DrawThread drawThread;
    int i = 1, x = 0, y = 0, width = 640, height = 360;

    public VideoSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawThread = new DrawThread(getContext(), getHolder());
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawThread.requestStop();
        boolean retry = true;
        while (retry) {
            try {
                drawThread.join();
                retry = false;
            } catch (InterruptedException e) {
                //
            }
        }
    }

    class DrawThread extends Thread {

        private SurfaceHolder surfaceHolder;

        private volatile boolean running = true;//флаг для остановки потока

        public DrawThread(Context context, SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        public void requestStop() {
            running = false;
        }

        @Override
        public void run() {
            while(running) {
                Canvas canvas = surfaceHolder.lockCanvas();
                Paint paint = new Paint();
                try {
                    Bitmap bm = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("out" + i, "drawable", "com.example.ducks.screen")));
                    width = bm.getWidth();
                    height = bm.getHeight();
                    Matrix matrix = new Matrix();
                    //Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    //display.getSize(size);
                    int width = size.x;
                    int height = size.y;
                    //matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);
                    RectF drawableRect = new RectF(0, 0, width, height);
                    RectF viewRect = new RectF(0, 0, width, height);
                    matrix.setRectToRect(viewRect, drawableRect, Matrix.ScaleToFit.FILL);
                    canvas.drawBitmap(bm, matrix, paint);
                    surfaceHolder.unlockCanvasAndPost(canvas);
                    i++;
                    if (i > 858)
                        i = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

