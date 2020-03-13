package tan.philip.nrf_ble.GraphScreen;

import android.util.Log;
import android.widget.Toast;

import static tan.philip.nrf_ble.GraphScreen.GraphActivity.MAX_POINTS_ARRAY;

public class Filter {
    /*
    https://www-users.cs.york.ac.uk/~fisher/mkfilter/
    Holy shit this website is literally a lifesaver
     */

    public static final float THRESHOLD_FOR_PEAK = 0.7f;

    public static final int N_ZEROS = 2;
    public static final int N_POLES = 2;
    //Max for ADC is 200 ksps
    public static final double GAIN_DEBUG =  1.554127469e+03;    //At 40 kHz
    //public static final double GAIN =  4.740642571e+00;    //At 100 Hz
    public static final double GAIN =  2.031823796e+01;    //At 500 Hz

    private float[] xv = new float[N_ZEROS+1];
    private float[] yv = new float[N_POLES+1];

    private float[] x;
    private float[] y;

    private int currentIndex = N_POLES;
    private int numPoints = N_POLES;

    private boolean aboveThreshold = false;
    private float peak;
    private float normalizationFactor = 1;
    private int peakIndex;


    public Filter(float[] inputs, float[] outputs) {
        x = inputs;
        y = outputs;
    }


    public void findNextY() {
        //Advance in time
        xv[0] = xv[1]; xv[1] = xv[2];

        //Set the next input
        xv[2] = (float) (x[currentIndex] / GAIN);

        //Advance in time
        yv[0] = yv[1]; yv[1] = yv[2];

        //Calculate current output
        /*
        //40 kHz
        yv[2] =   (float) ((xv[2] - xv[0])
                + ( -0.9988225962 * yv[0]) + (  1.9988224975 * yv[1]));
        //100 Hz
        yv[2] =   (float) ((xv[2] - xv[0])
                + ( -0.6128007881 * yv[0]) + (  1.5998427471 * yv[1]));
        */
        //500 Hz
        yv[2] = (float) ((xv[2] - xv[0])
                + ( -0.9099299882 * yv[0]) + (  1.9093263649 * yv[1]));


        numPoints ++;
        currentIndex = numPoints% MAX_POINTS_ARRAY;
        y[currentIndex] = yv[2];
    }

    public void findNextYDebug() {
        //Advance in time
        xv[0] = xv[1]; xv[1] = xv[2];

        //Set the next input
        xv[2] = (float) (x[currentIndex] / GAIN_DEBUG);

        //Advance in time
        yv[0] = yv[1]; yv[1] = yv[2];

        //40 kHz
        yv[2] =   (float) ((xv[2] - xv[0])
                + ( -0.9988225962 * yv[0]) + (  1.9988224975 * yv[1]));

        numPoints ++;
        currentIndex = numPoints% MAX_POINTS_ARRAY;
        y[currentIndex] = yv[2];
    }




    public void setXv(float xvValue, int index) {
        xv[index] = (float) (xvValue / GAIN);
    }

    public void setYv(float yvValue, int index) {
        yv[index] = yvValue;
    }


    private static final float PREFACTOR = 2f;
    /**
     * Amplifies the peaks in the data by raising y[t] to the 8th power.
     */
    public void amplifyPeaks() {
        y[currentIndex] = (float) Math.pow(PREFACTOR * y[currentIndex], 8);
    }

    /**
     * Amplifies the data by a constant
     * @param gain How much to amplify y[t] by
     */
    public void amplify(float gain) {
        y[currentIndex] *= gain;
    }


    public static final int NO_PEAK_DETECTED = -1;
    private int firstIndexAboveThreshold = 0;
    private int lastIndexAboveThreshold = 0;

    /**
     * Returns the index of the last detected peak in set of data
     * @return Index of last detected peak
     */
    public int findPeak() {
        if(aboveThreshold) {
            if (y[currentIndex] < THRESHOLD_FOR_PEAK) {
                aboveThreshold = false;

                normalizePeak(firstIndexAboveThreshold, currentIndex, peak);
                lastIndexAboveThreshold = numPoints;

                peak = NO_PEAK_DETECTED;
                return peakIndex;
            } else {
                if(y[currentIndex] > peak) {
                    peakIndex = numPoints;
                    peak = y[currentIndex];
                }
                return NO_PEAK_DETECTED;
            }
        } else {
            if(y[currentIndex] >= THRESHOLD_FOR_PEAK) {
                aboveThreshold = true;
                firstIndexAboveThreshold = numPoints;
            }
            return NO_PEAK_DETECTED;
        }
    }

    public int getFirstIndexAboveThreshold() {
        return firstIndexAboveThreshold;
    }

    public int getLastIndexAboveThreshold() {
        return lastIndexAboveThreshold;
    }

    public float getPeakAmplitude() {
        return peak;
    }

    public int getLastPeakIndex() {
        return peakIndex;
    }

    private void normalizePeak(int firstIndex, int finalIndex, float peakAmplitude) {
        int i = firstIndex;

        while (i != finalIndex) {
            y[i % MAX_POINTS_ARRAY] /= peakAmplitude;
            i = (i+1) % MAX_POINTS_ARRAY;
        }
    }

}