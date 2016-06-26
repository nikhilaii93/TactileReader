package org.opencv.samples.colorblobdetect;

/**
 * Created by Nikhil on 4/22/2016.
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
    static List<String> states;
    static List<MatOfPoint2f> statesContours;
    static List<List<Point>> statesPoints;

    public Utility(Context currCont) {
        this.cont = currCont;
        Corners = new Point[4];
        states = new ArrayList<String>();
        statesContours = new ArrayList<MatOfPoint2f>();
        statesPoints = new ArrayList<List<Point>>();
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

    public static void parseFile(String filename){
        try {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/Tactile Reader";
            File file = new File(path, filename);

            Log.wtf("MTP", "parsing: " + path + "/"+filename);

            BufferedReader br = new BufferedReader(new FileReader(file));

            // Skip first line
            String line = br.readLine();
            // Fill corners
            int t = 0;
            while(t < 4 && (line = br.readLine()) != null) {
                Log.i("Line", line+'\n');
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
                if (line.equals("=")) {
                    line = br.readLine();
                    states.add(line.trim());
                    if (!firstTime) {
                        statesPoints.add(contour);
                        // Mat m = Converters.vector_Point_to_Mat(contour);
                        // statesContours.add(new MatOfPoint2f(m));
                        contour = new ArrayList<Point>();
                    }
                    firstTime = false;
                } else {
                    Point gP = getPoint(line);
                    gP.x -= xOffset;
                    gP.y -= yOffset;
                    contour.add(gP);
                }
            }
            statesPoints.add(contour);


        } catch (IOException e) {
            e.printStackTrace();
            Log.wtf("MTP", "error in parsing");
        }
    }
    public static void parseFile2() {
        AssetManager assetManager = cont.getAssets();
        try {
            InputStream in = assetManager.open("state_coordinates.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            // Skip first line
            String line = br.readLine();
            // Fill corners
            int t = 0;
            while(t < 4 && (line = br.readLine()) != null) {
                Log.i("Line", line+'\n');
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
                if (line.equals("=")) {
                    line = br.readLine();
                    states.add(line.trim());
                    if (!firstTime) {
                        statesPoints.add(contour);
                        // Mat m = Converters.vector_Point_to_Mat(contour);
                        // statesContours.add(new MatOfPoint2f(m));
                        contour = new ArrayList<Point>();
                    }
                    firstTime = false;
                } else {
                    Point gP = getPoint(line);
                    gP.x -= xOffset;
                    gP.y -= yOffset;
                    contour.add(gP);
                }
            }
            statesPoints.add(contour);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
