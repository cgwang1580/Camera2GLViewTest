package com.camera2glview.chauncy_wang.camera2glviewtest;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;

public class CameraV2GLSurfaceActivity extends Activity {

    private MyGLSurfaceView mMyGLSurfaceView;
    private SurfaceTexture mSurfaceTexture;

    @Override
    protected void onCreate (Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
    }

    public void init () {
        mMyGLSurfaceView = new MyGLSurfaceView(this);
    }


}
