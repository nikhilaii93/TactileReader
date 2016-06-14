package org.opencv.samples.colorblobdetect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;


public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;

    // private Scalar               mBlackColorHsv;

    private ColorBlobDetector    mDetector;

    // private ColorBlobDetector    mBlackDetector;
    private Utility              mUtility;

    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private List<Integer> blackCentroidsX;
    private List<Integer> blackCentroidsY;
    private int fingerCentroidX;
    private int fingerCentroidY;

    private int previousState = -1;
    private TextToSpeech tts;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mUtility = new Utility(getApplicationContext());
        mUtility.parseFile();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        // mBlackDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        // mBlackColorHsv = new Scalar(0,0,0,255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        // mBlackDetector.setHsvColor(mBlackColorHsv);

        /*
        Log.i("BLACK", mBlackDetector.getmLowerBound().val[0]+" "
                +mBlackDetector.getmLowerBound().val[1]+" "
                +mBlackDetector.getmLowerBound().val[2]+" "
                +mBlackDetector.getmLowerBound().val[3]);
        Log.i("BLACK", mBlackDetector.getmUpperBound().val[0]+" "
                +mBlackDetector.getmUpperBound().val[1]+" "
                +mBlackDetector.getmUpperBound().val[2]+" "
                +mBlackDetector.getmUpperBound().val[3]);
        */

        // Imgproc.resize(mBlackDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            // mBlackDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            // List<MatOfPoint> blackContours = mBlackDetector.getContours();
            // Log.e(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
            // Imgproc.drawContours(mRgba, blackContours, -1, CONTOUR_COLOR);

            /*
            blackCentroidsX = new ArrayList<Integer>();
            blackCentroidsY = new ArrayList<Integer>();
            for (int i = 0; i < blackContours.size(); i++) {
                Point bcP = getCentroid(blackContours.get(i));
                Log.i("BLACK", "Identified");
                blackCentroidsX.add((int)bcP.x);
                blackCentroidsY.add((int)bcP.y);
            }
            */



            // Logic to call state name
            if (contours.size() == 3) {
                Point[] centroids = getCentroid(contours);
                fingerCentroidX = (int)centroids[1].x;
                fingerCentroidY = (int)centroids[1].y;

                blackCentroidsX = new ArrayList<Integer>();
                blackCentroidsY = new ArrayList<Integer>();
                blackCentroidsX.add((int)centroids[0].x);
                blackCentroidsX.add((int)centroids[2].x);
                blackCentroidsY.add((int)centroids[0].y);
                blackCentroidsY.add((int)centroids[2].y);

                Point nP = normalizePoint(new Point(fingerCentroidX, fingerCentroidY));
                Log.i(TAG, "Normalized Finger: " + nP.x + " " + nP.y + " " + mUtility.statesPoints.size());
                for (int i = 0; i < mUtility.statesPoints.size(); i++) {
                    if (/*Imgproc.pointPolygonTest(mUtility.statesContours.get(i), nP, false) > 0*/
                            polygonTest(nP, mUtility.statesPoints.get(i))) {
                        Log.i(TAG,"polygontestpassed" );
                        if (previousState != i) {
                            previousState = i;
                            final String toSpeak = mUtility.states.get(i);
                            Log.i(TAG,"toSpeak: " + toSpeak);
                            tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                                @Override
                                public void onInit(int status) {
                                    if(status != TextToSpeech.ERROR) {
                                        tts.setLanguage(Locale.ENGLISH);
                                        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                }
                            });

                        }
                        break;
                    }
                }
            }

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            //Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            //mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    Comparator<Point> comp = new Comparator<Point>() {
        @Override
        public int compare(Point lhs, Point rhs) {
            return (int)(lhs.y-rhs.y);
        }
    };

    private Point[] getCentroid(List<MatOfPoint> Contour) {
        Point[] centroids = new Point[Contour.size()];
        for (int i = 0; i < Contour.size(); i++) {
            Moments p = Imgproc.moments(Contour.get(i), false);
            int cX = (int) (p.get_m10() / p.get_m00());
            int cY = (int) (p.get_m01() / p.get_m00());
            //Imgproc.circle(mRgba, new Point(cX, cY), 10, CONTOUR_COLOR);
            centroids[i] = new Point(cX, cY);
        }
        Arrays.sort(centroids, comp);

        // Log.i(TAG, "SORT1: " + centroids[0].x + " " + centroids[0].y);
        // Log.i(TAG, "SORT2: " + centroids[1].x + " " + centroids[1].y);
        // Log.i(TAG, "SORT3: " + centroids[2].x + " " + centroids[2].y);

        centroids[0].x -= centroids[2].x;
        centroids[0].y -= centroids[2].y;
        centroids[1].x -= centroids[2].x;
        centroids[1].y -= centroids[2].y;
        centroids[2].x -= centroids[2].x;
        centroids[2].y -= centroids[2].y;

        return centroids;
    }

    /*
        Screen coordinate axis:
         Y -------------
                       |
                       |
                       |
                       |
                       | X
     */
    private Point normalizePoint(Point P) {

        int l = blackCentroidsY.get(0) < blackCentroidsY.get(1) ? 0 : 1;
        int h = blackCentroidsY.get(0) < blackCentroidsY.get(1) ? 1 : 0;

        // Log.i(TAG, "Corner1: " + blackCentroidsX.get(l) + " " + blackCentroidsY.get(l));
        // Log.i(TAG, "Corner2: " + blackCentroidsX.get(h) + " " + blackCentroidsY.get(h));

        // Find screen dist
        double ySQR = Math.pow((double)blackCentroidsY.get(h).intValue()-(double)blackCentroidsY.get(l).intValue(), 2);
        double xSQR = Math.pow((double)blackCentroidsX.get(h).intValue()-(double)blackCentroidsX.get(l).intValue(), 2);
        double screenDist = Math.pow(xSQR+ySQR, 0.5);

        double scalingFactor = Math.abs(mUtility.Corners[1].x-mUtility.Corners[0].x)/screenDist;

        /*
        double theta = Math.atan(((double)blackCentroidsY.get(h).intValue()-(double)blackCentroidsY.get(l).intValue())/((double)blackCentroidsX.get(h).intValue()-(double)blackCentroidsX.get(l).intValue()));

        double xDash = P.x*Math.sin(theta) - P.y*Math.cos(theta);
        double yDash = P.x*Math.cos(theta) + P.y*Math.sin(theta);

        double x = mUtility.Corners[1].x - scalingFactor*(yDash - (double)blackCentroidsY.get(l).intValue());
        double y = mUtility.Corners[0].y + scalingFactor*(xDash - (double)blackCentroidsX.get(l).intValue());
        */
        double xDash = P.x*scalingFactor;
        double yDash = P.y*scalingFactor;

        double theta = Math.atan(((double)blackCentroidsY.get(l).intValue()-(double)blackCentroidsY.get(h).intValue())/((double)blackCentroidsX.get(l).intValue()-(double)blackCentroidsX.get(h).intValue()));
        // Log.i(TAG, "Theta: " + theta);
        double x = xDash*Math.cos(theta) - yDash*Math.sin(theta);
        double y = xDash*Math.sin(theta) + yDash*Math.cos(theta);
        // Log.i(TAG, "xDash, yDash: " + xDash +", "+yDash);
        // Log.i(TAG, "x, y: " + x +", "+y);

        Point normalizedP = new Point(x, y);
        return normalizedP;
    }

    public boolean polygonTest(Point test, List<Point> points) {
        Log.i(TAG, "polygonTestRunning");
        int i;
        int j;
        boolean result = false;

        for(int a = 0; a < points.size(); a++) {
            Log.i(TAG, "Points " + a + ": " + points.get(a).x + "  " + points.get(a).y);
        }
        for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {

            if ((points.get(i).y > test.y) != (points.get(j).y > test.y) &&
                    (test.x < (points.get(j).x - points.get(i).x) * (test.y - points.get(i).y) / (points.get(j).y-points.get(i).y) + points.get(i).x)) {
                result = !result;
            }
        }
        return result;
    }
}
