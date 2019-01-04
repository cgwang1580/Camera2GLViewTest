package com.camera2glview.chauncy_wang.camera2glviewtest;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView {

    private MyGLRenderer myGLRenderer;

    public MyGLSurfaceView (Context context){
        super(context);
    }

    public void init (GLCameraV2Base glCameraV2Base, boolean isPreviewStart, Context context){
        setEGLContextClientVersion(3);
        myGLRenderer = new MyGLRenderer();
        myGLRenderer.init (this, glCameraV2Base, isPreviewStart, context);
        setRenderer(myGLRenderer);
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
