package com.malone.androidcamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void surfaceView(View view) {
        startActivity(new Intent(this,SurfaceViewActivity.class));
    }

    public void camera(View view) {
        startActivity(new Intent(this,CameraActivity.class));
    }

    public void camera2(View view) {
        startActivity(new Intent(this,Camera2Activity.class));
    }
}
