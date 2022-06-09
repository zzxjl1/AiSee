package com.idealbroker.aisee;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class CameraActivity extends AppCompatActivity {
    private static String TAG = "camera";
    private static Handler mBackgroundHandler;
    private static CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private static CameraCaptureSession mCaptureSession;
    private static CaptureRequest mPreviewRequest;
    private CameraManager manager;
    private Size[] ResArray;
    private static CameraCharacteristics characteristics;
    private HandlerThread mBackgroundThread;

    private TextureView Preview;
    private ImageReader mImageReader;
    private Size mPreviewSize;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        Preview = findViewById(R.id.PREVIEW);
        Preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        });
        openCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void createCameraPreviewSession() {

        try {
            SurfaceTexture surfaceTexture = Preview.getSurfaceTexture();
            mPreviewSize = getOptimalSize(ResArray);
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface_preview = new Surface(surfaceTexture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);


            mImageReader = ImageReader.newInstance(Preview.getWidth(), Preview.getHeight(), ImageFormat.YUV_420_888, 2);
            mImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
            Surface surface_capture = mImageReader.getSurface();
            mPreviewRequestBuilder.addTarget(surface_preview);
            mPreviewRequestBuilder.addTarget(surface_capture);
            mCameraDevice.createCaptureSession(Arrays.asList(surface_preview, surface_capture),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);
                                Log.e(TAG, "正在开启camera2相机预览");
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "开启camera2预览失败: onConfigureFailed ");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "开启camera2预览失败: CameraAccessException ");
            e.printStackTrace();
        }
    }


    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;

        }

    };

    Long last = 0L;
    static int interval = 1000;
    private boolean ImageIsProcessing = false;
    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //Log.i("imageReader", "onImageAvailable");
            Image image = reader.acquireLatestImage();
            if (image == null) return;
            if (System.currentTimeMillis() - last < interval || ImageIsProcessing) {
                image.close();
                return;
            }

            Log.i("imageReader", "ImageIsProcessing");
            ImageIsProcessing = true;
            last = System.currentTimeMillis();

            Bitmap img = ToolUtils.yuvCameraImageToBitmap(image);
            Mat mat = new Mat();
            org.opencv.android.Utils.bitmapToMat(img, mat);

            double ratio = 4;
            int height = Double.valueOf(mat.size().height/ratio).intValue();
            int width = Double.valueOf(mat.size().width/ratio).intValue();
            org.opencv.core.Size size = new org.opencv.core.Size(width, height);
            Mat resizedImage = new Mat(size, CvType.CV_8UC4);
            Imgproc.resize(mat, resizedImage, size);
            //Core.flip(resizedImage, resizedImage, 1);

            detectDocument.Quadrilateral t = detectDocument.findDocument(resizedImage);
            if (t!=null){
                Log.i("imageReader", t.contour.toString());
                for (int i=0;i<t.points.length;i++){
                    Imgproc.circle(resizedImage, t.points[i], 5, new Scalar(0, 255, 0, 150), 4);
                    ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                    contours.add(t.contour);
                    Imgproc.drawContours(resizedImage,contours,0 ,new Scalar(0,0,255));
                    try {
                        FileOutputStream out = new FileOutputStream(getFilesDir().getPath()+"/1.png");
                        //ToolUtils.Mat2Bitmap(resizedImage).compress(Bitmap.CompressFormat.PNG, 100, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

            image.close();
            mat.release();
            resizedImage.release();
            ImageIsProcessing = false;
            Log.i("imageReader", "Image done Processing");



        }
    };


    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    private void openCamera() {
        try {
            manager.openCamera(getcameraID(), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException | SecurityException e) {
            e.printStackTrace();
        }
        startBackgroundThread();
    }


    private String getcameraID() {
        try {
            //获取可用摄像头列表
            for (String cameraId : manager.getCameraIdList()) {
                //获取相机的相关参数
                characteristics = manager.getCameraCharacteristics(cameraId);
                // 不使用前置摄像头。
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                Log.e(TAG, "可用摄像头:" + cameraId);

                ResArray = map.getOutputSizes(ImageFormat.JPEG);
                Log.e(TAG, "可选分辨率:" + Arrays.toString(ResArray));


                return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //不支持Camera2API
        }
        return ":0";
    }

    private Size getOptimalSize(Size[] sizeMap) {
        int width = Preview.getWidth();
        int height = Preview.getHeight();
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

}
