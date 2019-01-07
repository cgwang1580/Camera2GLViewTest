package com.camera2glview.chauncy_wang.camera2glviewtest;

/**
 * created by cgwang1580 on 2019/1/3
 * this is a camera2 base class
 */

import android.Manifest;
import android.app.Activity;
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
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GLCameraV2Base {

    private static final String TAG = "CameraV2";

    private int saveIndex = 0;

    private Context mContext;
    private Activity mActivity;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;

    private CameraManager mCameraManager;
    private String mCameraId;
    private Size mPreviewSize;
    private SurfaceTexture mSurfaceTexture;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraPreviewSession;

    public GLCameraV2Base (Context context, Activity activity){
        mContext = context;
        mActivity = activity;
        startCameraThread();
    }

    public void startCameraThread () {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    /**
     * setup camera
     * @param width width and height of device relative
     * @param height
     * @return true if ok, false if wrong
     */
    public boolean setupCamera (int width, int height) {
        Log.d(TAG, "setupCamera");
        applyDevicePermission();

        mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraList = mCameraManager.getCameraIdList();
            for (String cameraId : cameraList){
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                //use rear camera only
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                // get StreamConfigurationMap object which manage all formats and sizes of camera
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                // choose size by testView
                Size[] sizeMap = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
                mPreviewSize = getOptimalSize (sizeMap, width, height);
                Log.d(TAG, "setupCamera mPreview size width = " + mPreviewSize.getWidth()
                        + " height = " + mPreviewSize.getHeight());
                mCameraId = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * this function to open camera but not start preview
     * @return true if openCamera succeeded
     */
    public boolean openCamera(){
        try {
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)){
                return false;
            }
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    mCameraDevice = camera;
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    mCameraDevice = null;
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * set surface texture from outside
     * @param surfaceTexture
     */
    public void setPreviewTexture (SurfaceTexture surfaceTexture){
        mSurfaceTexture = surfaceTexture;
    }

    /**
     * start camera preview
     * @return
     */
    public boolean startPreview () {

        //----------------create ImageReader object to test image data begin----------------//
        ImageReader imageReader = ImageReader.newInstance(mPreviewSize.getWidth(),
                mPreviewSize.getHeight(), ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //Image image = reader.acquireLatestImage();
                /*Log.d(TAG, "onImageAvailable");
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);*/

                mCameraHandler.post(new ImageSaver(reader.acquireLatestImage(), saveIndex));
                saveIndex++;

                //image.close();
            }
        }, mCameraHandler);
        Surface imageReaderSurface = imageReader.getSurface();
        //-----------------create ImageReader object to test image data end-----------------//


        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(mSurfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);

            mCaptureRequestBuilder.addTarget(imageReaderSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, imageReaderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mCaptureRequest = mCaptureRequestBuilder.build();
                    mCameraPreviewSession = session;
                    try {
                        mCameraPreviewSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // apply devices permission dynamically like camera and storage
    private void applyDevicePermission (){
        // apply camera authority dynamically
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)){
            ActivityCompat.requestPermissions(mActivity, new String[] { Manifest.permission.CAMERA}, 1);
        }
        // apply read and write storage authority
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)){
            ActivityCompat.requestPermissions(mActivity, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    public static class ImageSaver implements Runnable {
        private Image mImage;
        private int mSaveIndex;
        public ImageSaver (Image image, int index){
            mImage = image;
            mSaveIndex = index;
        }

        @Override
        public void run () {
            Log.d("ImageSaver", "run");
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File mImageFile = new File(Environment.getExternalStorageDirectory()
                    + "/DCIM/MyCapture/myPicture_" + mSaveIndex +".jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mImageFile);
                fos.write(data, 0 ,data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImageFile = null;
                if (fos != null) {
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            mImage.close();
        }
    }

    // choose best preview size
    private Size getOptimalSize (Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        if (width > height) {
            for (Size size1 : sizeMap) {
                if (size1.getWidth() > width && size1.getHeight() > height) {
                    sizeList.add(size1);
                }
            }
        }else {
            for (Size size1 : sizeMap) {
                if (size1.getWidth() > height && size1.getHeight() > width) {
                    sizeList.add(size1);
                }
            }
        }

        if (sizeList.size() > 0){
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
        }

        return sizeMap[0];
    }
}
