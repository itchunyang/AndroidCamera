package com.malone.androidcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {

    // 预览
    public static final int STATE_PREVIEW = 0;
    //等待上锁(拍照片前将预览锁上保证图像不在变化)
    public static final int STATE_WAITING_LOCK = 1;
    //等待预拍照(对焦, 曝光等操作)
    public static final int STATE_WAITING_PRECAPTURE = 2;
    //等待非预拍照(闪光灯等操作)
    public static final int STATE_WAITING_NON_PRECAPTURE = 3;
    //已经获取照片
    public static final int STATE_PICTURE_TAKEN = 4;

    /**
     * 当前的相机状态, 这里初始化为预览, 因为刚载入这个fragment时应显示预览
     */
    private int mState = STATE_PREVIEW;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    ///为了使照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static final String TAG = "Camera2";
    public static final String PATH = "/sdcard/DCIM/Camera/abc.jpg";
    private CameraManager cameraManager;

    /**
     * 信号量控制器, 防止相机没有关闭时退出本应用(若没有关闭就退出, 会造成其他应用无法调用相机)
     * 当某处获得这个许可时, 其他需要许可才能执行的代码需要等待许可被释放才能获取
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * 正在使用的相机
     */
    private CameraDevice mCameraDevice;

    /**
     * 正在使用的相机id
     */
    private String cameraId;

    /**
     * 预览使用的自定义TextureView控件
     */
    private SurfaceView surfaceView;

    /**
     * 预览用的获取会话
     */
    private CameraCaptureSession captureSession;

    /**
     * 预览数据的尺寸
     */
    private Size prevSize;

    /**
     * 相机状态改变的回调函数
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.i(TAG, "onOpened: ");
            mCameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.i(TAG, "onDisconnected: ");
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.i(TAG, "onError: ");
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };


    /**
     * 捕获会话回调函数
     */
    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    /**
     * 预览请求构建器, 用来构建"预览请求"(下面定义的)通过pipeline发送到Camera device
     */
    private CaptureRequest.Builder previewRequestBuilder;

    /**
     * 预览请求, 由上面的构建器构建出来
     */
    private CaptureRequest previewRequest;

    /**
     * 静止页面捕获(拍照)处理器
     */
    private ImageReader imageReader;
    private OnImageAvailableListener onImageAvailableListener = new OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {

        }
    };

    private int PREVIEW_WIDTH = 600;
    private int PREVIEW_HEIGHT = 600;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        imageReader = ImageReader.newInstance(2000, 1504, ImageFormat.JPEG, 1);
        HandlerThread thread = new HandlerThread("thread");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void cameraParams(View view) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String cameraId : cameraManager.getCameraIdList()) {
                //[0,1] 0是后置  1是前置
                sb.append("摄像头id=").append(cameraId);
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (facing == CameraCharacteristics.LENS_FACING_FRONT)
                    sb.append(" 前置摄像头").append("\n");
                else if (facing == CameraCharacteristics.LENS_FACING_BACK)
                    sb.append("后置摄像头").append("\n");

                sb.append("闪光灯:" + characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)).append("\n");

                sb.append("图片输出尺寸").append("\n");
                StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = configurationMap.getOutputSizes(ImageFormat.JPEG);//获取图片输出的尺寸
                for (int i = 0; i < sizes.length; i++) {
                    sb.append("width=" + sizes[i].getWidth() + " height=" + sizes[i].getHeight()).append("\n");
                }

                sb.append("\n预览尺寸").append("\n");
                sizes = configurationMap.getOutputSizes(SurfaceTexture.class);
                for (int i = 0; i < sizes.length; i++) {
                    sb.append("width=" + sizes[i].getWidth() + " height=" + sizes[i].getHeight()).append("\n");
                }

                sb.append("-----------------------------------\n");
            }

            Log.i(TAG, "cameraParams: " + sb.toString());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void openCamera(View view) {
        if (mCameraDevice != null)
            mCameraDevice.close();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            /**
             * 第一个是我们之前获取的CameraId
             * 第二个参数是当CameraDevice被打开时的回调StateCallback
             * 第三个参数是一个Handler，决定了回调函数触发的线程，若为null，则选择当前线程
             */
            if (view.getId() == R.id.openBack)
                cameraManager.openCamera("0", stateCallback, handler);
            else
                cameraManager.openCamera("1", stateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void capture(View view) {
        if (mCameraDevice == null)
            return;

        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(imageReader.getSurface());
            // 自动对焦
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //打开闪光灯
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            CaptureRequest request = builder.build();
            captureSession.capture(request, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    List<CaptureResult> list = result.getPartialResults();
                    super.onCaptureCompleted(session, request, result);
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        Surface surface = surfaceView.getHolder().getSurface();
        try {
            // 预览请求构建
            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            // 创建预览的捕获会话
            mCameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;

                    try {
                        // 自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // 自动闪光
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                        // 构建上述的请求,CameraRequest代表了一次捕获请求，用于描述捕获图片的各种参数设置，比如对焦模式、曝光模式
                        previewRequest = previewRequestBuilder.build();

                        // 重复进行上面构建的请求, 以便显示预览
                        captureSession.setRepeatingRequest(previewRequest, captureCallback, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, handler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraDevice != null)
            mCameraDevice.close();
    }
}
