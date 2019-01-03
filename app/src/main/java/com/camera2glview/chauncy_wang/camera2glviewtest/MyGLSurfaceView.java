package com.camera2glview.chauncy_wang.camera2glviewtest;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer myGLRenderer;

    public MyGLSurfaceView (Context context){
        super(context);

        setEGLContextClientVersion(3);
        myGLRenderer = new MyGLRenderer();
        setRenderer(myGLRenderer);

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
