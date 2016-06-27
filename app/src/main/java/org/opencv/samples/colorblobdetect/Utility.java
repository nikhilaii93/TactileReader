package org.opencv.samples.colorblobdetect;

/**
 * Created by Nikhil on 4/22/2016.
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;
import android.view.Display;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.utils.Converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;

public class Utility {
    static Context cont;

    /*
        Corner[0]           Corner[1]
            *------------------*
            |                  |
            |                  |
            |                  |
            |                  |
            *------------------*
        Corner[3]           Corner[2]
     */
    static Point[] Corners;
    static List<String> titles;
    static List<String> descriptions;
    static List<MatOfPoint2f> regionContours;
    static List<List<Point>> regionPoints;
    static String audioFormat = ".wav";

    public Utility(Context currCont) {
        this.cont = currCont;
        Corners = new Point[4];
        titles = new ArrayList<String>();
        regionContours = new ArrayList<MatOfPoint2f>();
        regionPoints = new ArrayList<List<Point>>();
    }

    private static Point getPoint(String line) {
        line = line.trim();
        StringTokenizer st = new StringTokenizer(line);
        float x = Float.parseFloat(st.nextToken());
        float y = Float.parseFloat(st.nextToken());
        Point P = new Point(x, y);

        return P;
    }

    /*
        Coordinate axis of file:
            -------------- X
            |
            |
            |
            |
            | Y
     */

    public static void parseFile(String filename) {
        try {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/Tactile Reader";
            File file = new File(path + File.separator + filename, filename + ".txt");

            Log.wtf("MTP", "parsing: " + path + "/" + filename + "/" + filename + ".txt");

            BufferedReader br = new BufferedReader(new FileReader(file));

            // Skip first line
            String line = br.readLine();
            // Fill corners
            int t = 0;
            while (t < 4 && (line = br.readLine()) != null) {
                Log.i("Line", line + '\n');
                Corners[t] = getPoint(line);
                t++;
            }
            double xOffset = Corners[0].x;
            double yOffset = Corners[0].y;

            for (int i = 0; i < 4; i++) {
                Corners[i].x -= xOffset;
                Corners[i].y -= yOffset;
            }

            List<Point> contour = new ArrayList<Point>();
            boolean firstTime = true;
            // Skip the first empty line
            while ((line = br.readLine()) != null) {
                line = br.readLine();
                titles.add(line.trim());
                line = br.readLine();
                if (line.startsWith("$AUDIO$")) {
                    descriptions.add(line.trim());
                } else {
                    String desc = "";
                    while ((line = br.readLine()) != "=") {
                        desc += line;
                    }
                    descriptions.add(desc);
                }
                while ((line = br.readLine()) != "=") {
                    Point gP = getPoint(line);
                    gP.x -= xOffset;
                    gP.y -= yOffset;
                    contour.add(gP);
                }
                regionPoints.add(contour);
            }
        } catch (
                IOException e
                )
        {
            e.printStackTrace();
            Log.wtf("MTP", "error in parsing");
        }

    }

    public static boolean isPulse() {

        return false;
    }

    public static void playAudio(String filePath, String fileName) {
        Context appContext = cont;
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(filePath + File.separator + fileName + audioFormat);
            mp.prepare();
        } catch (IOException e) {
            Log.i("PLAY_AUDIO", "Audio File cannot be played");
            e.printStackTrace();
        }
        mp.start();
    }

    Comparator<Point> compY = new Comparator<Point>() {
        @Override
        public int compare(Point lhs, Point rhs) {
            return (int) (lhs.y - rhs.y);
        }
    };

    Comparator<Point> compX = new Comparator<Point>() {
        @Override
        public int compare(Point lhs, Point rhs) {
            return (int) (lhs.x - rhs.x);
        }
    };

    public Point[] getCentroid(List<MatOfPoint> Contour, Comparator comp) {
        Point[] centroids = new Point[Contour.size()];
        for (int i = 0; i < Contour.size(); i++) {
            Moments p = Imgproc.moments(Contour.get(i), false);
            int cX = (int) (p.get_m10() / p.get_m00());
            int cY = (int) (p.get_m01() / p.get_m00());
            //Imgproc.circle(mRgba, new Point(cX, cY), 10, CONTOUR_COLOR);
            centroids[i] = new Point(cX, cY);
        }
        Arrays.sort(centroids, comp);

        Log.i("ROT", "SORT1: " + centroids[0].x + " " + centroids[0].y);
        Log.i("ROT", "SORT2: " + centroids[1].x + " " + centroids[1].y);
        Log.i("ROT", "SORT3: " + centroids[2].x + " " + centroids[2].y);

        if (getOrientation() == 1 || getOrientation() == 4) {
            centroids[0].x -= centroids[2].x;
            centroids[0].y -= centroids[2].y;
            centroids[1].x -= centroids[2].x;
            centroids[1].y -= centroids[2].y;
            centroids[2].x -= centroids[2].x;
            centroids[2].y -= centroids[2].y;
        } else {
            centroids[1].x -= centroids[0].x;
            centroids[1].y -= centroids[0].y;
            centroids[2].x -= centroids[0].x;
            centroids[2].y -= centroids[0].y;
            centroids[0].x -= centroids[0].x;
            centroids[0].y -= centroids[0].y;
        }

        Log.i("ROT", "MOV1: " + centroids[0].x + " " + centroids[0].y);
        Log.i("ROT", "MOV2: " + centroids[1].x + " " + centroids[1].y);
        Log.i("ROT", "MOV3: " + centroids[2].x + " " + centroids[2].y);

        return centroids;
    }

    public Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public boolean polygonTest(Point test, List<Point> points) {
        Log.i("POLYGON_TEST", "polygonTestRunning");
        int i;
        int j;
        boolean result = false;

        for (int a = 0; a < points.size(); a++) {
            Log.i("POLYGON_TEST", "Points " + a + ": " + points.get(a).x + "  " + points.get(a).y);
        }
        for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {

            if ((points.get(i).y > test.y) != (points.get(j).y > test.y) &&
                    (test.x < (points.get(j).x - points.get(i).x) * (test.y - points.get(i).y) / (points.get(j).y - points.get(i).y) + points.get(i).x)) {
                result = !result;
            }
        }

        return result;
    }

    /*
    The orientation of the app is landscape.
    If the orientation of the phone is portrait : +----------+
                                                  |1        2|
                                                  |          |
                                                  |          |
                                                  |          |
                                                  |          |
                                                  |4        3|
                                                  +----------+

     Then relative to it the orientation of the tactile can be 4, assuming the tags are the
     uppermost left & right corner of the diagram, the 4 orientations w.r.t. tags are, clockwise:
     1. left tag @ 1 & right tag @ 2
     2. left tag @ 2 & right tag @ 3
     3. left tag @ 3 & right tag @ 4
     4. left tag @ 4 & right tag @ 1
     */
    // Retuns orientation number between 1 to 4
    public int getOrientation() {
        return 0;
    }
}
