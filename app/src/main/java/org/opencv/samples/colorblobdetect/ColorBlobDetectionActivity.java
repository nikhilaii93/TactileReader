package org.opencv.samples.colorblobdetect;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private boolean mIsColorSelected = false;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private TextToSpeech tts;

//    String envpath = Environment.getDataDirectory().getPath() + File.separator + "Tactile Reader";
     String envpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/Tactile Reader";



    // private Scalar               mBlackColorHsv;

    private ColorBlobDetector mDetector;

    // private ColorBlobDetector    mBlackDetector;
    private Utility mUtility;

    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;

    private List<Integer> blackCentroidsX;
    private List<Integer> blackCentroidsY;
    private int fingerCentroidX;
    private int fingerCentroidY;

    private int previousState = -1;
    String filename;

    private CameraBridgeViewBase mOpenCvCameraView;

    SharedPreferences sp;

    public static boolean calibrated = true;
    public static int calibrationTagsNotVisible = 0;
    public static int calibrationCount = 0;
    public static int noCalibrationCount = 0;
    public static int pulsedPolygon = -1;
    public static int pulseState = -1;
    public static int pulseDuration = 0;
    public static int pulseDurationLimit = 5;
    public static int calibrationFrameRate = 100;
    public static int continousFrameBehaivior = 0;

    public static int allowedFingerMovement = 50;
    public static int fingerStaticCount = 0;
    public static int fingerStaticLimit = 3;
    public static Point previousFingerPosition = null;
    public static int fingerApprovalCount = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);
        //android.graphics.Point sSize = getScreenDimensions();
        //getWindow().setLayout(sSize.x, sSize.y);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        filename = sp.getString("context_name", null);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mUtility = new Utility(getApplicationContext());
        mUtility.parseFile(filename);

        final String speakStr = "Scanning " + filename;
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.speak(speakStr, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
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
        CONTOUR_COLOR = new Scalar(0, 255, 0, 255);

        displayColor();
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public void displayColor() {
        JSONArray savedColor = new JSONArray();
        try {
            savedColor = new JSONArray(sp.getString("touched_color_hsv", "[]"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (savedColor == null || savedColor.length() == 0) {
            for (int i = 0; i < mBlobColorHsv.val.length; i++) {
                mBlobColorHsv.val[i] = 0;

            }
        } else {
            for (int i = 0; i < mBlobColorHsv.val.length; i++) {
                try {
                    mBlobColorHsv.val[i] = savedColor.getDouble(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        mBlobColorRgba = mUtility.convertScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Saved rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
        mIsColorSelected = true;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i("TOUCH", "Touch image coordinates + Offset: (" + x + xOffset + ", " + y + yOffset + ")");
        Log.i("TOUCH", "Offset: (" + xOffset + ", " + yOffset + ")");
        Log.i("TOUCH", "Touch image coordinates: (" + x + ", " + y + ")");
        Log.i("TOUCH", "Cols + Rows: (" + cols + ", " + rows + ")");
        Log.i("TOUCH", "Width + Height: (" + mOpenCvCameraView.getWidth() + ", " + mOpenCvCameraView.getHeight() + ")");

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            //changeCalibrationState(contours, getApplicationContext());
            if (contours.size() == 2 && calibrated && (pulseState == 0)) {
                pulseDuration++;
                Log.i("PULSE", "PulseDuration: " + pulseDuration);
                if (pulseDuration > pulseDurationLimit) {
                    pulseState = 1;
                    pulseDuration = 0;
                }
            }

            Log.i("PULSE", "PulseState: " + pulseState);
            Log.i("PULSE", "ContourSize: " + contours.size());

            // Logic to call state name
            if (contours.size() == 3 && calibrated && !tts.isSpeaking() && !mUtility.mp.isPlaying()) {
                pulseDuration = 0;
                Point[] centroids;
                if (mUtility.getOrientation() % 2 == 0) {
                    centroids = mUtility.getCentroid(contours, mUtility.compX);
                } else {
                    centroids = mUtility.getCentroid(contours, mUtility.compY);
                }
                fingerCentroidX = (int) centroids[1].x;
                fingerCentroidY = (int) centroids[1].y;

                blackCentroidsX = new ArrayList<Integer>();
                blackCentroidsY = new ArrayList<Integer>();
                blackCentroidsX.add((int) centroids[0].x);
                blackCentroidsX.add((int) centroids[2].x);
                blackCentroidsY.add((int) centroids[0].y);
                blackCentroidsY.add((int) centroids[2].y);

                Log.i("CENTROIDS", "Finger: " + fingerCentroidX + " " + fingerCentroidY);
                Log.i("CENTROIDS", "Blob1: " + blackCentroidsX.get(0) + " " + blackCentroidsY.get(0));
                Log.i("CENTROIDS", "Blob2: " + blackCentroidsX.get(1) + " " + blackCentroidsY.get(1));
                android.graphics.Point pt = getScreenDimensions();
                Log.i("CENTROIDS", "Size: " + pt.x + " " + pt.y);

                Point nP = normalizePoint(new Point(fingerCentroidX, fingerCentroidY));
                Log.i(TAG, "Normalized Finger: " + nP.x + " " + nP.y + " " + mUtility.regionPoints.size());
                if (Utility.isFingerStatic(nP) || true) {
                    for (int i = 0; i < mUtility.regionPoints.size(); i++) {
                        Log.i(TAG, "For polygonTest: " + mUtility.titles.get(i) + " " + i);
                        if (/*Imgproc.pointPolygonTest(mUtility.statesContours.get(i), nP, false) > 0*/
                                mUtility.polygonTest(nP, mUtility.regionPoints.get(i))) {
                            Log.i(TAG, "polygontestpassed");
                            Log.i(TAG, "PulsedPolygon: " + pulsedPolygon + " " + Utility.titles.get(i) + " " + i);
                            if (pulseState == -1) {
                                pulseState = 0;
                                pulsedPolygon = i;
                            } else if (pulseState == 1 && pulsedPolygon == i) {
                                pulseState = 2;
                            } else if (pulseState == 1 && pulsedPolygon != i) {
                                pulsedPolygon = i;
                                pulseState = 0;
                            } else if (pulseState == 0) {
                                pulsedPolygon = i;
                            }

                            if ((previousState != i) && !mUtility.isPulse()) {
                                previousState = i;
                                String speakStr = mUtility.titles.get(i);
                                Log.i("PULSE", "toSpeak: " + speakStr);
                                speakOut(speakStr, getApplicationContext());
                            }
                            if (mUtility.isPulse() && previousState == i) {
                                pulseState = 0;
                                final String toDescribe = mUtility.descriptions.get(pulsedPolygon);
                                if (toDescribe.startsWith("$AUDIO$")) {
                                    envpath = Environment.getDataDirectory() + File.separator + "Tactile Reader";

                                    mUtility.playAudio(envpath + File.separator + filename, toDescribe);

                                    Log.wtf("MTP", "parsing: " + envpath + "/" + toDescribe);
                                } else {
                                    Log.i(TAG, "toDescribe: " + toDescribe);
                                    speakOut(toDescribe, getApplicationContext());
                                }
                            }
                            break;
                        }
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
        double scalingFactor;

        // Log.i(TAG, "Corner1: " + blackCentroidsX.get(l) + " " + blackCentroidsY.get(l));
        // Log.i(TAG, "Corner2: " + blackCentroidsX.get(h) + " " + blackCentroidsY.get(h));

        // Find screen dist
        double ySQR = Math.pow((double) blackCentroidsY.get(1).intValue() - (double) blackCentroidsY.get(0).intValue(), 2);
        double xSQR = Math.pow((double) blackCentroidsX.get(1).intValue() - (double) blackCentroidsX.get(0).intValue(), 2);
        double screenDist = Math.pow(xSQR + ySQR, 0.5);

        scalingFactor = ((double) Math.abs(mUtility.Corners[1].x - mUtility.Corners[0].x)) / screenDist;

        double xDash = P.x * scalingFactor;
        double yDash = P.y * scalingFactor;

        double theta = Math.atan(((double) blackCentroidsY.get(1).intValue() - (double) blackCentroidsY.get(0).intValue()) / ((double) blackCentroidsX.get(1).intValue() - (double) blackCentroidsX.get(0).intValue()));

        if (mUtility.getOrientation() > 2) {
            theta = theta - Math.toRadians(180);
        }

        double x = xDash * Math.cos(theta) - yDash * Math.sin(theta);
        double y = xDash * Math.sin(theta) + yDash * Math.cos(theta);
        Log.i("ROTATION", "Theta: " + Math.toDegrees(theta));
        Log.i("ROTATION", "xDash, yDash: " + xDash + ", " + yDash);
        Log.i("ROTATION", "x, y: " + x + ", " + y);

        Point normalizedP = new Point(x, y);
        return normalizedP;
    }

    // Returns size of screen in pixels
    public android.graphics.Point getScreenDimensions() {
        Display display = getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);

        return size;
    }

    public boolean isInView(List<MatOfPoint> contours) {
        Point[] tempC;
        if (mUtility.getOrientation() % 2 == 0) {
            tempC = mUtility.getCentroid(contours, mUtility.compY);
        } else {
            tempC = mUtility.getCentroid(contours, mUtility.compX);
        }
        Point lowestPoint = new Point();
        double toAdd = 0;
        if (mUtility.getOrientation() == 1) {
            lowestPoint = tempC[1];
            toAdd = lowestPoint.x;
        } else if (mUtility.getOrientation() == 2) {
            lowestPoint = tempC[1];
            toAdd = lowestPoint.y;
        } else if (mUtility.getOrientation() == 3 || (mUtility.getOrientation() == 4)) {
            if (contours.size() == 2) {
                lowestPoint = tempC[0];
            } else if (contours.size() == 3) {
                lowestPoint = tempC[1];
            } else {
                Log.i(TAG, "This Orientation is not Possible");
            }
            if (mUtility.getOrientation() == 3) {
                toAdd = lowestPoint.x;
            } else {
                toAdd = lowestPoint.y;
            }
        } else {
            Log.i(TAG, "This Orientation is not Possible");
        }
        double ySQR = Math.pow((double) tempC[1].y - (double) tempC[0].y, 2);
        double xSQR = Math.pow((double) tempC[1].x - (double) tempC[0].x, 2);
        double screenDistX = Math.pow(xSQR + ySQR, 0.5);

        double scalingFactor = ((double) Math.abs(mUtility.Corners[1].x - mUtility.Corners[0].x)) / screenDistX;

        double cornerDist_1_2 = Math.abs(mUtility.Corners[1].y - mUtility.Corners[2].y);
        double cornerDist_0_3 = Math.abs(mUtility.Corners[0].y - mUtility.Corners[3].y);
        double cornerDistY = (cornerDist_1_2 < cornerDist_0_3) ? cornerDist_0_3 : cornerDist_1_2;

        // Actual Y would be lower than this as the image would be slightly tilted but to be on the safe side
        double extreme = 0;
        if (mUtility.getOrientation() > 2) {
            extreme = toAdd - (cornerDistY / scalingFactor);
        } else {
            extreme = toAdd + (cornerDistY / scalingFactor);
        }

        if (mUtility.getOrientation() % 2 == 1) {
            int cols = mRgba.cols();
            int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
            extreme += xOffset;

            Log.i("CALIBRATION", "extreme+Offset" + "\t" + "lX" + "\t" + "lY");
            Log.i("CALIBRATION", extreme + " " + lowestPoint.x + " " + lowestPoint.y);

            if ((extreme > mOpenCvCameraView.getWidth()) || (extreme < 0)) {
                return false;
            } else {
                return true;
            }
        } else {
            int rows = mRgba.rows();
            int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
            extreme += yOffset;

            if ((extreme > mOpenCvCameraView.getHeight()) || (extreme < 0)) {
                return false;
            } else {
                return true;
            }
        }
    }

    public void changeCalibrationState(List<MatOfPoint> contours, Context applicationContext) {
        boolean prevCalibrationState = calibrated;
        if (contours.size() == 0 || contours.size() == 1) {
            calibrationTagsNotVisible++;
            calibrationCount = 0;
            noCalibrationCount = 0;
            continousFrameBehaivior++;
            if (continousFrameBehaivior > 10) {
                calibrated = false;
                continousFrameBehaivior = 0;
            }
            Log.i("CalCheck", "NotCalibrated2");
            if ((prevCalibrationState != calibrated) || (calibrationTagsNotVisible > calibrationFrameRate * 2)) {
                String toSpeak = "Image not placed";
                speakOut(toSpeak, applicationContext);
                calibrationTagsNotVisible = 0;
            }
        } else if (contours.size() == 2 || contours.size() == 3) {
            continousFrameBehaivior = 0;
            calibrationTagsNotVisible = 0;
            calibrated = isInView(contours);
            Log.i("CALIBRATION", "IsCalibrated: " + calibrated);
            if (calibrated) {
                calibrationCount++;
                calibrationCount = calibrationCount % calibrationFrameRate;
                noCalibrationCount = 0;
                if (prevCalibrationState != calibrated) {
                    calibrationCount = 0;
                    final String toSpeak = "Image now in view";
                    Log.i("CalCheck", "Calibrated");
                    // Add in queue even if tts isSpeaking(), speakOut doesn't add if isSpeaking()
                    speakOut(toSpeak, applicationContext);
                }
            } else {
                noCalibrationCount++;
                calibrationCount = 0;
                if ((prevCalibrationState != calibrated) || (noCalibrationCount > calibrationFrameRate)) {
                    noCalibrationCount = 0;
                    final String toSpeak = "Only part of image visible";
                    Log.i("CalCheck", "NotCalibrated");
                    speakOut(toSpeak, applicationContext);
                }
            }
        } else {
            String toSpeak = "Conditions not suitable, many tags visible";
            // speakOut(toSpeak, applicationContext);
        }
    }

    public void speakOut(String toSpeak, Context applicationContext) {
        if (!mUtility.mp.isPlaying() && !tts.isSpeaking()) {
            final String speakStr = toSpeak;
            tts = new TextToSpeech(applicationContext, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        tts.setLanguage(Locale.ENGLISH);
                        tts.speak(speakStr, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
            });
        }
    }
}
