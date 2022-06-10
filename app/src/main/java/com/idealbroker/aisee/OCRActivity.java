package com.idealbroker.aisee;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.idealbroker.aisee.opencv.Camera2View;

import org.opencv.android.BaseLoaderCallback;

import com.idealbroker.aisee.opencv.CameraBridgeViewBase;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.Features2d;

import java.util.List;


public class OCRActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    Camera2View cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    FloatingActionButton assistButton;
    FloatingActionButton flashButton;
    FloatingActionButton captureButton;
    boolean is_assist_on = true;
    boolean is_flash_on = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);
        cameraBridgeViewBase = findViewById(R.id.CameraView);
        assistButton = findViewById(R.id.assistButton);
        flashButton = findViewById(R.id.flashButton);
        captureButton = findViewById(R.id.captureButton);


        assistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                is_assist_on = !is_assist_on;
                updateAssistState();
            }
        });

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                is_flash_on = !is_flash_on;
                if(is_flash_on){
                    MyApplication.tts.speek("开启闪光灯", true, true);
                    cameraBridgeViewBase.toggleFlashMode(true);
                    flashButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.greenyellow)));
                }else{
                    MyApplication.tts.speek("关闭闪光灯", true, true);
                    cameraBridgeViewBase.toggleFlashMode(false);
                    flashButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.gray)));
                }
            }
        });

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "TAKE PHOTO", Snackbar.LENGTH_SHORT).show();
            }
        });

        updateAssistState();
        MyApplication.tts.speek("AI读书", false, false);
        MyApplication.tts.speek("请将镜头对准要识别的页面，并拍照", false, false);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.enableFpsMeter();
        cameraBridgeViewBase.setCvCameraViewListener(this);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                if (status == BaseLoaderCallback.SUCCESS) {
                    cameraBridgeViewBase.enableView();
                    //cameraBridgeViewBase.enableFpsMeter();
                } else {
                    super.onManagerConnected(status);
                }
            }
        };
    }

    private void updateAssistState() {
        if (is_assist_on) {
            MyApplication.tts.speek("已启动视觉辅助模式", true, true);
            assistButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.gold)));
        } else {
            MyApplication.tts.speek("已关闭视觉辅助模式", true, true);
            assistButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.gray)));
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "There is problem", Toast.LENGTH_SHORT).show();
        } else {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication.tts.speek("退出AI读书", true, true);
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Camera2View.Camera2Frame inputFrame) {

        Mat mat = inputFrame.rgba();
        if (is_assist_on) {
            MatOfKeyPoint t = detectDocument.findKeyPoints(mat);
            Mat canvas = new Mat(new Size(mat.width(), mat.height()), CvType.CV_8UC4, new Scalar(0));
            Features2d.drawKeypoints(canvas, t, canvas, new Scalar(0, 255, 0), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
            return canvas;
        } else {
            return mat;
        }


    }
}