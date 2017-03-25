package com.example.vinothramss.Sher_Lock;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.gesture.Gesture;
import android.gesture.GestureStroke;
import android.util.Log;

/**
 * Created by Vinoth Ram S S on 23-03-2017.
 */

public class Doodle {
    final int REP_SIZE = 96;
    ArrayList<double[]> numericalRep = new ArrayList<double[]>();
    final double THRESHOLD = 0.001;
    final int TOLERANCE = 5;
    double[] means = new double[REP_SIZE];
    double[] variances = new double[REP_SIZE];
    public Doodle(ArrayList<Gesture> gestureList) {
        for (Gesture gesture : gestureList) {
            numericalRep.add(gestureToArray(gesture));
        }
        for (int i = 0; i < REP_SIZE; i++) {
            double sum = 0.0;
            for (double[] gestureRep : numericalRep) {
                sum += gestureRep[i];
            }
            means[i] = sum / numericalRep.size();
        }
        for (int i = 0; i < REP_SIZE; i++) {
            double temp = 0;
            for (double[] gestureRep : numericalRep) {
                temp += (means[i] - gestureRep[i]) * (means[i] - gestureRep[i]);
            }
            variances[i] = temp / numericalRep.size();
        }
    }

    public boolean authenticate(Gesture testGesture) {
        double[] gestureRep = gestureToArray(testGesture);
        double[] confidences = new double[REP_SIZE];
        double confidence = 1.0;
        int count = 0;

        for (int i = 0; i < REP_SIZE; i++) {
            if (variances[i] != 0) {
                confidences[i] = gauss(gestureRep[i], means[i], variances[i]);
                confidence *= confidences[i];
                count++;
            } else if (gestureRep[i] != 0) {
                return false;
            }
        }
        confidence *= count;
        return (confidence >= THRESHOLD);
    }

    private double[] gestureToArray(Gesture gesture) {
        double[] gestureValues = new double[REP_SIZE];
        ArrayList<GestureStroke> strokes = gesture.getStrokes();
        for (int i = 0; i < REP_SIZE / 8; i++) {
            if (i < strokes.size()) {
                GestureStroke currentStroke = strokes.get(i);
                gestureValues[8 * i + 0] = currentStroke.length;
                gestureValues[8 * i + 1] = currentStroke.points[0];
                gestureValues[8 * i + 2] = currentStroke.points[1];
                gestureValues[8 * i + 3] = currentStroke.points[currentStroke.points.length - 2];
                gestureValues[8 * i + 4] = currentStroke.points[currentStroke.points.length - 1];
                gestureValues[8 * i + 5] = currentStroke.boundingBox.width();
                gestureValues[8 * i + 6] = currentStroke.boundingBox.height();
                long duration = 0;
                try {
                    Field f = currentStroke.getClass().getDeclaredField("timestamps");
                    f.setAccessible(true);
                    long[] timestamps = (long[]) f.get(currentStroke);
                    duration = timestamps[timestamps.length - 1]
                            - timestamps[0];
                } catch (Exception e) {
                    Log.e("ERROR", "Reflection didn't work");
                }
                gestureValues[8 * i + 7] = duration;
            }
            else {
                for (int j = 0; j < 8; j++) {
                    gestureValues[8 * i + j] = 0.0;
                }
            }
        }
        return gestureValues;
    }

    private double gauss(double x, double mean, double variance) {
        return Math.exp(-(x - mean) * (x - mean) / (TOLERANCE * 2 * variance));
    }
}
