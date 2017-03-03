package com.malone.androidcamera;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

//android.hardware.Camera在5.0后被废弃，由android.hardware.Camera2代替

/**
 * Android的Camera是独享的，如果多处调用，就会抛出异常,
 * 所以，我们需要将Camera的生命周期与Activity的生命周期绑定：
 * 1.onResume方法中初始化相机
 * 2.onPause方法中释放相机
 */
public class CameraActivity extends AppCompatActivity implements Callback{

    public static final String TAG = "Camera";
    public static final String PATH = "/sdcard/DCIM/Camera/abc.jpg";
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private LinearLayout ll;
    private int rotation;//屏幕方向 The starting position is 0 默认四竖屏(0)。横屏时为1
    private int needRotation;//预览需要旋转的度数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ll = (LinearLayout) findViewById(R.id.ll);
        surfaceView = new SurfaceView(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);
        ll.addView(surfaceView);

        rotation = getWindowManager().getDefaultDisplay().getRotation();
        if(rotation == Surface.ROTATION_0)
            Log.i(TAG, "onCreate: 当前屏幕方向为竖屏");
        else if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
            Log.i(TAG, "onCreate: 当前屏幕方向为横屏");

    }

    @Override
    protected void onResume() {
        super.onResume();
//        try {
//            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//            Camera.Parameters parameters = camera.getParameters();
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//            camera.setParameters(parameters);
////            camera.cancelAutoFocus();
//        }catch (Exception e){
//            e.printStackTrace();
//            camera = null;
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        releaseCamera();
    }

    public void cameraParam(View view) {
        Log.i(TAG, "cameraParam: 摄像头数量:"+Camera.getNumberOfCameras());

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            StringBuilder sb = new StringBuilder();
            Camera.getCameraInfo(i,cameraInfo);
            sb.append("第"+i+"个摄像头:");
            if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                sb.append("后置摄像头");
            else if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                sb.append("前置摄像头");

            sb.append(" orientation="+cameraInfo.orientation);
            Log.i(TAG, "cameraParam: "+sb.toString());
        }
    }


    /**
     * meizu pro6 plus 5.7英寸2560x1440
     * width=2560,height=1440
     * width=2048,height=1536
     * width=1920,height=1080
     * width=1440,height=1080
     * width=1280,height=720
     * width=800,height=600
     * width=720,height=480
     * width=640,height=480
     * width=320,height=240
     * width=176,height=144
     */
    public void openBack(View view) {
        try {
            releaseCamera();
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

            Camera.Parameters parameters = camera.getParameters();
            StringBuilder sb = new StringBuilder();
            sb.append("后置摄像头PreviewSize:\n");

            //是预览的大小，也就是拍照前看到的图片大小
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            for (int i = 0; i < sizes.size(); i++) {
                int width = sizes.get(i).width;
                int height = sizes.get(i).height;
                sb.append("width="+width+" height="+height).append("\n");
            }

            parameters.setPreviewSize(320,240);
            //拍照尺寸 是指最终拍摄到的图片的大小，也就是图片的质量
            List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
            sb.append("pictureSize").append("\n");
            for (int i = 0; i < pictureSizes.size(); i++) {
                Camera.Size size = pictureSizes.get(i);
                sb.append("width="+size.width+" height="+size.height).append("\n");
            }
            parameters.setPictureSize(2000,1504);

            /**
             * FLASH_MODE_AUTO 自动模式，当光线较暗时自动打开闪光灯
             * FLASH_MODE_OFF 关闭闪光灯
             * FLASH_MODE_ON 拍照时闪光灯
             * FLASH_MODE_RED_EYE 闪光灯参数，防红眼模式
             */
            sb.append("闪光灯模式:").append("\n");//off auto on torch
            List<String> flashModes = parameters.getSupportedFlashModes();
            for (int i = 0; i < flashModes.size(); i++) {
                sb.append(flashModes.get(i)).append("\n");
            }

            List<String> colorEffects = parameters.getSupportedColorEffects();
            sb.append("拍摄场景:").append(colorEffects.size()).append("\n");

            for (int i = 0; i < colorEffects.size(); i++) {
                sb.append(colorEffects.get(i)).append("\n");
            }


            sb.append("照片格式").append("\n");//查询支持的预览帧格式
            List<Integer> pictureFmts = parameters.getSupportedPictureFormats();
            for (int i = 0; i < pictureFmts.size(); i++) {
                if(pictureFmts.get(i) == ImageFormat.JPEG){
                    sb.append("JPEG").append("\n");
                }
            }

            /**
             * SCENE_MODE_AUTO 自动选择场景
             * SCENE_MODE_NIGHT 夜间场景
             * SCENE_MODE_ACTION 动作场景，就是抓拍跑得飞快的运动员、汽车等场景用的；
             * ...
             */
            List<String> sceneModes = parameters.getSupportedSceneModes();
            sb.append("场景模式").append("\n");
            for (int i = 0; i < sceneModes.size(); i++) {
                sb.append(sceneModes.get(i)).append("\n");
            }

            Log.i(TAG, "openBack: "+sb.toString());
//            parameters.getZoom();

            /**
             * 对焦模式
             * FOCUS_MODE_AUTO 自动对焦模式，摄影小白专用模式
             * FOCUS_MODE_INFINITY 远景模式，拍风景大场面的模式；
             * FOCUS_MODE_MACRO 微焦模式，拍摄小花小草小蚂蚁专用模式
             * FOCUS_MODE_FIXED 固定焦距模式，拍摄老司机模式
             * FOCUS_MODE_EDOF 景深模式，文艺女青年最喜欢的模式
             * FOCUS_MODE_CONTINUOUS_VIDEO 持续对焦
             */
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//用于拍照的连续自动对焦模式

//            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);一直亮
//            parameters.setColorEffect(colorEffects.get(2));//虽然预览看不出效果，但是拍出来的照片就是黑白的
            camera.setParameters(parameters);
            camera.setPreviewDisplay(surfaceHolder);

            //Camera.setDisplayOrientaion(int)的参数是以角度为单位的，而且只能是0，90，180，270其中之一
//            camera.setDisplayOrientation( needRotation = 90);

            needRotation = CameraUtil.setDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK,camera);
            System.out.println("needRotation="+needRotation);
            //从而当下一幅预览图像可用时调用一次onPreviewFrame
//            camera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
//                @Override
//                public void onPreviewFrame(byte[] data, Camera camera) {
//                    System.out.println("--------------");
//                }
//            });
            camera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openFont(View view) {
        try {
            releaseCamera();
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            camera.setPreviewDisplay(surfaceHolder);
            camera.setDisplayOrientation(needRotation = 270);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void takePhoto(View view) {
        //拍照前可以自动对焦一次,也可以不对焦直接拍照
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(success){
                    Log.i(TAG, "onAutoFocus: 对焦成功 开始拍照");
                    camera.takePicture(null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            boolean ok = CameraUtil.saveBitmap(data,PATH,needRotation);
                            if (ok) {
                                Toast.makeText(CameraActivity.this, "拍照成功,保存到本地"+PATH, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(CameraActivity.this, "拍照成功失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Log.i(TAG, "onAutoFocus: 对焦失败");
                }
            }
        });


        //直接拍照，不对焦
//        camera.takePicture(new Camera.ShutterCallback() {
//            @Override
//            public void onShutter() {
//                //Camera.ShutterCallback：定义了onShutter方法，当捕获图像时立刻调用它
//                //在拍摄瞬间瞬间被回调，通常用于播放“咔嚓”这样的音效
//            }
//        }, null, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                CameraUtil.saveBitmap(data,PATH,90);
//            }
//        });
    }


    //main线程
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//        if(camera!=null)
//            try {
//                camera.setPreviewDisplay(surfaceHolder);
//                camera.setDisplayOrientation(90);
//                camera.startPreview();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

//        Log.i(TAG, "surfaceCreated: threadname="+Thread.currentThread().getName());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        Log.i(TAG, "surfaceChanged: threadname="+Thread.currentThread().getName());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        if(camera != null)
//            camera.stopPreview();
    }

    void releaseCamera(){
        if(camera != null){
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void openLight(View view) {
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
            camera.startPreview();
            camera.autoFocus(new Camera.AutoFocusCallback() {
                public void onAutoFocus(boolean success, Camera camera) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            camera = null;
        }
    }
}
