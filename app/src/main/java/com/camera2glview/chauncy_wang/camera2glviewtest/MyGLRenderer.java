package com.camera2glview.chauncy_wang.camera2glviewtest;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "MyGLRenderer";
    private Context mContext;
    private GLCameraV2Base mCameraV2;
    private MyGLSurfaceView mCameraV2GLSurfaceView;
    private boolean mIsPreviewStart;
    private int mOESTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private float[] mTransformMatrix;

    // shader relative parameter
    private int mVertexShader;
    private int mFragShader;
    private int mShaderProgram;
    private FloatBuffer mDataBuffer;
    private int aPositionLocation;
    private int aTextureCoordLocation;
    private int uTextureMatrixLocation;
    private int uTextureSamplerLocation;
    private int[] mFBOIds = new int[1];


    public void init (MyGLSurfaceView myGLSurfaceView, GLCameraV2Base glCameraV2Base, boolean isPreviewStart, Context context){
        mContext = context;
        mCameraV2 = glCameraV2Base;
        mIsPreviewStart = isPreviewStart;
        mCameraV2GLSurfaceView = myGLSurfaceView;
        mTransformMatrix = new float[16];
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        mOESTextureId = MyShader.createOESTextureObject();
        mDataBuffer = MyShader.createBuffer(MyShader.vertexDate);
        mVertexShader = MyShader.loadShader(GL_VERTEX_SHADER, MyShader.VERTEX_SHADER);
        mFragShader = MyShader.loadShader(GL_VERTEX_SHADER, MyShader.FRAGMENT_SHADER);
        mShaderProgram = MyShader.linkProgram(mVertexShader, mFragShader);

        glGenFramebuffers(1, mFBOIds, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, mFBOIds[0]);
        Log.i(TAG, "onSurfaceCreated: mFBOId: " + mFBOIds[0]);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        Log.d(TAG, "View port width = " + width + " height = " + height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        Log.d(TAG, "onDrawFrame");
        if (null != mSurfaceTexture){
            // update texture image
            mSurfaceTexture.updateTexImage();
            // obtain matrix of outside texture, to judge the position of texture
            mSurfaceTexture.getTransformMatrix(mTransformMatrix);
        }

        if (!mIsPreviewStart){
            mIsPreviewStart = initSurfaceTexture();
            mIsPreviewStart = true;
            return;
        }
        drawImage();
    }

    public boolean initSurfaceTexture() {

        if (null == mCameraV2 || null == mCameraV2GLSurfaceView) {
            Log.e(TAG, "initSurfaceTexture failed");
            return false;
        }
        //根据OES纹理ID实例化SurfaceTexture
        mSurfaceTexture = new SurfaceTexture(mOESTextureId);

        if (null == mSurfaceTexture){
            Log.e(TAG, "create surface texture failed");
        }

        //当SurfaceTexture接收到一帧数据时，请求OpenGL ES进行渲染
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                Log.d(TAG, "onFrameAvailable");
                mCameraV2GLSurfaceView.requestRender();
            }
        });
        mCameraV2.setPreviewTexture(mSurfaceTexture);
        mCameraV2.startPreview();
        return true;
    }

    public void drawImage (){

        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        aPositionLocation = glGetAttribLocation(mShaderProgram, "aPosition");
        aTextureCoordLocation = glGetAttribLocation(mShaderProgram, "aTextureCoordinate");
        uTextureMatrixLocation = glGetUniformLocation(mShaderProgram, "uTextureMatrix");
        uTextureSamplerLocation = glGetUniformLocation(mShaderProgram, "uTextureSampler");

        //激活纹理单元0
        glActiveTexture(GL_TEXTURE_EXTERNAL_OES);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTextureId);
        glUniform1i(uTextureSamplerLocation, 0);

        //将纹理矩阵传给片段着色器
        glUniformMatrix4fv(uTextureMatrixLocation, 1, false, mTransformMatrix, 0);

        //将顶点和纹理坐标传给顶点着色器
        if (mDataBuffer != null) {
            //顶点坐标从位置0开始读取
            mDataBuffer.position(0);
            //使能顶点属性
            glEnableVertexAttribArray(aPositionLocation);
            //顶点坐标每次读取两个顶点值，之后间隔16（每行4个值 * 4个字节）的字节继续读取两个顶点值
            glVertexAttribPointer(aPositionLocation, 2, GL_FLOAT, false, 16, mDataBuffer);

            //纹理坐标从位置2开始读取
            mDataBuffer.position(2);
            glEnableVertexAttribArray(aTextureCoordLocation);
        // 纹理坐标每次读取两个顶点值，之后间隔16（每行4个值 * 4个字节）的字节继续读取两个顶点值
            glVertexAttribPointer(aTextureCoordLocation, 2, GL_FLOAT, false, 16, mDataBuffer);
        }

        //glDrawElements(GL_TRIANGLE_FAN, 6,GL_UNSIGNED_INT, 0);
        //glDrawArrays(GL_TRIANGLE_FAN, 0 , 6);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        //glDrawArrays(GL_TRIANGLES, 3, 3);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

}


