package com.camera2glview.chauncy_wang.camera2glviewtest;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;

public class CameraV2GLSurfaceActivity extends Activity {

    private MyGLSurfaceView mMyGLSurfaceView;
    private GLCameraV2Base mCameraBase;

    @Override
    protected void onCreate (Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        init();
        setContentView(mMyGLSurfaceView);
    }

    public void init () {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mCameraBase = new GLCameraV2Base(this, this);
        mCameraBase.setupCamera(dm.widthPixels, dm.heightPixels);
        if (!mCameraBase.openCamera()){
            return;
        }
        mMyGLSurfaceView = new MyGLSurfaceView(this);
        mMyGLSurfaceView.init(mCameraBase, false, CameraV2GLSurfaceActivity.this);
    }

    @Override
    protected void onResume (){
        super.onResume();
    }

    @Override
    protected void onDestroy (){
        super.onDestroy();
    }

}
