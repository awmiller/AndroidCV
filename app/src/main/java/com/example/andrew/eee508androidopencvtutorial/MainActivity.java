package com.example.andrew.eee508androidopencvtutorial;

import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2,
        View.OnTouchListener {

    //<editor-fold desc="Draw Tool Members">

    public class TouchSample {
        public long time = 0;
        public float X = 0;
        public float Y = 0;
        public Point aPoint;
        public org.opencv.core.Point cvPoint;

        public TouchSample() {
        }

        public TouchSample(float x, float y, long t) {
            X = x;
            Y = y;
            time = t;
            aPoint = new Point(Math.round(X), Math.round(Y));
            cvPoint = new org.opencv.core.Point(X, Y);
        }

        public TouchSample(MotionEvent e) {
            X = e.getX();
            Y = e.getY();
            time = e.getEventTime();
            aPoint = new Point(Math.round(X), Math.round(Y));
            cvPoint = new org.opencv.core.Point(X, Y);
        }

        public TouchSample(MotionEvent e, int pkey, int hkey) {
            X = e.getHistoricalX(pkey, hkey);
            Y = e.getHistoricalY(pkey, hkey);
            time = e.getHistoricalEventTime(hkey);
            aPoint = new Point(Math.round(X), Math.round(Y));
            cvPoint = new org.opencv.core.Point(X, Y);
        }

        public TouchSample(MotionEvent e, int pkey) {
            X = e.getX(pkey);
            Y = e.getY(pkey);
            time = e.getEventTime();
            aPoint = new Point(Math.round(X), Math.round(Y));
            cvPoint = new org.opencv.core.Point(X, Y);
        }

        public TouchSample Delta(TouchSample s2) {
            return new TouchSample(X - s2.X, Y - s2.Y, time - s2.time);
        }

        public TouchSample Translate(float dx, float dy){
            return Translate(dx,dy,0);
        }

        public TouchSample Translate(float dx, float dy, long dt){
            return new TouchSample(X+dx,Y+dy,time+dt);
        }
    }

    public List<TouchSample> InflateSamples(MotionEvent e) {
        final int hsize = e.getHistorySize();
        final int pc = e.getPointerCount();
        List<TouchSample> l = new LinkedList<>();
        for (int i = 0; i < hsize; i++) {
            for (int j = 0; j < pc; j++) {
                l.add(new TouchSample(e, j, i));
            }
        }
        for (int j = 0; j < pc; j++)
            l.add(new TouchSample(e, j));
        return l;
    }

    private TextView touch_coords;

    private TextView touch_color;

    private static String coord_format = "X:[%3f] Y:[%3f]      (%3.1f,%3.1f)";

    private static String color_format = "Color: %X:%X:%X";

    private List<Point> touchCoordList;

    private Stack<TouchSample> touchStack = new Stack<>();

    private double[] ScreenCalY = new double[]{0.240196, 0.7696078};

    double yLow = 0;

    double yHigh = 0;

    private void ApplyScreenCal() {
        yHigh = (double) mOpenCvCameraView.getHeight() * ScreenCalY[1];
        yLow = (double) mOpenCvCameraView.getHeight() * ScreenCalY[0];
    }

    private Scalar mBlobColorRgba;

    private Scalar mBlobColorHsv;

    //</editor-fold>

    //<editor-fold desc="misc members">

    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat mRgba;

    //</editor-fold>

    //<editor-fold desc="loader interface">
    private LoaderCallbackInterface mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                }
            }
        }
    };
    //</editor-fold>

    //<editor-fold desc="Life Cycle">
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_tutorial_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        touch_color = findViewById(R.id.touch_color);
        touch_coords = findViewById(R.id.touch_coord);
        touchStack.add(new TouchSample(0, 0, 0));
        //ApplyScreenCal();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        //ApplyScreenCal();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    //</editor-fold>

    //<editor-fold desc="CameraViewListener2 Interface">
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        return mRgba;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    //</editor-fold>

    //<editor-fold desc="Touch Listener">
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        UpdateTouchMetrics(event);
        return false;
    }

    private void UpdateTouchMetrics(MotionEvent event) {
        // get new point
        TouchSample s1 = new TouchSample(event);
        // normalize to picture frame
        ApplyScreenCal();
        TouchSample pxPoint = ScaleTouchPoint(s1);
        // manage stack
        TouchSample s2 = touchStack.pop();
        touchStack.push(pxPoint);

        if (pxPoint != null && s2!=null) {

            int c = ApplyPatchColor(pxPoint);

            UpdateText(s1,s1.Delta(s2),c);
        }
    }

    private TouchSample ScaleTouchPoint(TouchSample s1) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        double xScale = (double) cols / (double) mOpenCvCameraView.getWidth();
        double yScale = (double) rows / (yHigh - yLow);
        double Y = (double) s1.Y - yLow;
        double X = (double) s1.X * xScale;
        Y = Y * yScale;
        if (X > cols || X < 0 || Y > rows || Y < 0) return null;
        else return new TouchSample((float) X, (float) Y, s1.time);
    }

    private int ApplyPatchColor(TouchSample s1) {
        // los cols que fuer tocs
        Rect touch = new Rect(s1.Translate(4,4).cvPoint,new Size(8,8));
        Mat tRgba = mRgba.submat(touch);

        // traducir
        Mat tHsv = new Mat();
        Imgproc.cvtColor(tRgba,tHsv,Imgproc.COLOR_RGB2HSV_FULL);

        // uberstzn
        mBlobColorHsv = Core.sumElems(tHsv);
        int zahlePnkte =  touch.width * touch.height;
        for(int i =0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= zahlePnkte;

        // weider uberstzn
        mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

        int c = Color.rgb((int)mBlobColorRgba.val[0],
                (int)mBlobColorRgba.val[1],
                (int)mBlobColorRgba.val[2]);

        return c;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsv) {
        Mat ptMatRpga = new Mat();
        Mat ptHsv = new Mat(1,1, CvType.CV_8UC3,hsv);
        Imgproc.cvtColor(ptHsv,ptMatRpga,Imgproc.COLOR_HSV2RGB_FULL,4);

        return new Scalar(ptMatRpga.get(0,0));
    }

    private void UpdateText(TouchSample s1, TouchSample d, int c) {
        // update text
        touch_coords.setText(
                String.format(Locale.ENGLISH, coord_format, s1.X, s1.Y, d.X, d.Y));
        touch_color.setText(
                String.format(Locale.ENGLISH,color_format,
                        (int)mBlobColorRgba.val[0],
                        (int)mBlobColorRgba.val[1],
                        (int)mBlobColorRgba.val[2]));

        touch_color.setTextColor(c);
        touch_coords.setTextColor(c);
    }

    //</editor-fold>

}
