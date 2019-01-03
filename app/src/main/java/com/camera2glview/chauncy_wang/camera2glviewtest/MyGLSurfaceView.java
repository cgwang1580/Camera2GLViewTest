package com.camera2glview.chauncy_wang.camera2glviewtest;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView {

    MyGLRenderer myGLRenderer;

    public MyGLSurfaceView (Context context){
        super(context);

        myGLRenderer = new MyGLRenderer();

    }
}
