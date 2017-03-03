package com.malone.androidcamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.view.Surface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by luchunyang on 2017/2/28.
 */

public class CameraUtil {
    public static boolean saveBitmap(byte[] data, String path,int rotation) {

        Bitmap src = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (src == null)
            return false;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap bitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, false);

        boolean ok = false;
        try {
            ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if (ok)
            return true;
        else
            return false;
    }

    public static boolean saveBitmap(byte[] data, String path) {

        Bitmap src = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (src == null)
            return false;

//        Matrix matrix = new Matrix();
//        matrix.postRotate(rotation);
//        Bitmap bitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, false);

        boolean ok = false;
        try {
            ok = src.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if (ok)
            return true;
        else
            return false;
    }

    public static int setDisplayOrientation(Activity activity, int cameraId, Camera camera){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId,cameraInfo);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result = 0;
        if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        else if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }

        camera.setDisplayOrientation(result);
        return result;
    }
}
