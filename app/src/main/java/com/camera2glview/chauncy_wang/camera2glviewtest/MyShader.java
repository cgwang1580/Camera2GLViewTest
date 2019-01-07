package com.camera2glview.chauncy_wang.camera2glviewtest;

/**
 * create by cgwang1580 on 2019/1/4
 * this class will do some shader relative work
 */

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUseProgram;

public class MyShader {

    public static final float[] vertexDate = {
            1f, 1f, 1f, 1f,
            -1f, 1f, 0f, 1f,
            -1f, -1f, 0f, 0f,
            1f, 1f, 1f, 1f,
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f
    };

    public static final String VERTEX_SHADER = "" +
            //顶点坐标
            "attribute vec4 aPosition;\n" +
            //纹理矩阵
            "uniform mat4 uTextureMatrix;\n" +
            //自己定义的纹理坐标
            "attribute vec4 aTextureCoordinate;\n" +
            //传给片段着色器的纹理坐标
            "varying vec2 vTextureCoord;\n" +
            "void main()\n" +
            "{\n" +
            //根据自己定义的纹理坐标和纹理矩阵求取传给片段着色器的纹理坐标
            "  vTextureCoord = (uTextureMatrix * aTextureCoordinate).xy;\n" +
            "  gl_Position = aPosition;\n" +
            "}\n";

    public static final String FRAGMENT_SHADER = "" +
            //使用外部纹理必须支持此扩展
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            //外部纹理采样器
            "uniform samplerExternalOES uTextureSampler;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() \n" +
            "{\n" +
            //获取此纹理（预览图像）对应坐标的颜色值
            "  vec4 vCameraColor = texture2D(uTextureSampler, vTextureCoord);\n" +
            //将此灰度值作为输出颜色的RGB值，这样就会变成黑白滤镜
            "  gl_FragColor = vec4(vCameraColor.r, vCameraColor.g, vCameraColor.b, 1.0);\n" +
            "}\n";

    ////求此颜色的灰度值
    //            "  float fGrayColor = (0.3*vCameraColor.r + 0.59*vCameraColor.g + 0.11*vCameraColor.b);\n" +


    public MyShader (){

    }

    /**
     * create outside texture
     * @return
     */
    public static int createOESTextureObject() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return tex[0];
    }

    public static FloatBuffer createBuffer (float[] vertexDate){
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexDate.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        buffer.put(vertexDate, 0, vertexDate.length).position(0);
        return buffer;
    }

    /**
     *
     * @param type
     * @param shaderSource
     * @return
     */
    public static int loadShader (int type, String shaderSource){

        // create shader
        int shader = glCreateShader(type);
        if (0 == shader){
            throw new RuntimeException("Create Shader Failed!" + glGetError());
        }
        // load shader
        glShaderSource(shader, shaderSource);
        // compile shader
        glCompileShader(shader);
        return shader;
    }

    /**
     *
     * @param verShader
     * @param fragShader
     * @return
     */
    public static int linkProgram (int verShader, int fragShader) {
        // create program
        int program = glCreateProgram();
        if (0 == program){
            throw new RuntimeException("Create program failed!" + glGetError());
        }
        // attach vertex and fragment
        glAttachShader(program, verShader);
        glAttachShader(program, fragShader);
        // link vertex and fragment shader
        glLinkProgram(program);
        // tele opengl es to use this program
        glUseProgram(program);
        return program;
    }
}
