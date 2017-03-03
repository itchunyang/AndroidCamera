package com.malone.androidcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * surfaceview的优势在于可以自己控制帧数，比较适合对帧数要求较高的程序
 */
public class SurfaceViewActivity extends AppCompatActivity {

    public static final String TAG = "SurfaceView";
    private SurfaceView mySurfaceView;
    private LoopThread loopThread;
    private boolean isWork;

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "surfaceCreated: ");
            loopThread.start();
        }

        //当Surface的状态（大小和格式）发生变化的时候会调用该函数，在surfaceCreated调用后该函数至少会被调用一次
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(TAG, "surfaceChanged: ");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "surfaceDestroyed: ");
            holder.removeCallback(callback);
        }
    };

    /**
     * SurfaceHolder，顾名思义，它里面保存了一个对Surface对象的引用
     */
    private SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_view);
        mySurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = mySurfaceView.getHolder();
        loopThread = new LoopThread(this, surfaceHolder);

        //设置Surface生命周期回调
        surfaceHolder.addCallback(callback);

//        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //设置SurfaceView大小里面的分辨率(width多少个像素，height多少个像素)
        surfaceHolder.setFixedSize(450, 600);

//        ViewGroup.LayoutParams layoutParams = mySurfaceView.getLayoutParams();
//        layoutParams.width = 600;
//        layoutParams.height = 600;
//        mySurfaceView.setLayoutParams(layoutParams);
        isWork = true;
    }

    class LoopThread extends Thread {

        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Context context;
        private SurfaceHolder holder;
        private float raidus;

        public LoopThread(Context context, SurfaceHolder holder) {
            this.context = context;
            this.holder = holder;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.YELLOW);
        }

        @Override
        public void run() {
            Canvas canvas = null;

            while (isWork) {

                canvas = holder.lockCanvas();

                if(canvas == null)
                    return;

                //这个很重要，清屏操作，清楚掉上次绘制的残留图像
                canvas.drawColor(Color.BLACK);

                if (raidus >= 200)
                    raidus = 10;
                canvas.drawCircle(200, 200, raidus+=2, paint);

                //通过它来控制帧数执行一次绘制后休息50ms
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                holder.unlockCanvasAndPost(canvas);// 结束锁定画图，并提交改变。
            }
        }
    }


}
