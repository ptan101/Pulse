package tan.philip.nrf_ble.GraphScreen;

import static tan.philip.nrf_ble.GraphScreen.PWVGraphActivity.MAX_POINTS_ARRAY;

public class Filter {
    /*
    https://www-users.cs.york.ac.uk/~fisher/mkfilter/
    Holy shit this website is literally a lifesaver
     */

    public static final int N_ZEROS = 2;
    public static final int N_POLES = 2;
    public static final double GAIN =  2.031823796e+01;    //At 500 Hz
    //public static final double GAIN =  6.800073801e+10;    //At 500 Hz, Cheby

    //Small array for next y calculation
    private float[] xv = new float[N_ZEROS+1];
    private float[] yv = new float[N_POLES+1];

    //Full array of input / outputs
    private float[] x;
    private float[] y;

    //Array of filter coefficients
    private double gain;
    private double[] fx = new double[N_ZEROS +1];
    private double[] fy = new double[N_POLES +1];

    private int fs;
    private int tfs;
    private int currentIndex = N_POLES;
    private int numPoints = N_POLES;

    private boolean aboveThreshold = false;
    private float peak;
    private float normalizationFactor = 1;
    private int peakIndex;

    private SignalType signalType;

    public enum SignalType {
        PPG,
        ECG,
        SCG
    }


    public Filter(float[] inputs, float[] outputs, int sampleRate, int targetSampleRate, SignalType signal) {
        x = inputs;
        y = outputs;
        fs = sampleRate;
        tfs = targetSampleRate;
        signalType = signal;

        switch(signalType) {
            case PPG:
                //fs = 500 Hz, fc1 = 1.5Hz, fc2 = 8Hz
                gain = 2.410311073e+01;
                fx[0] = -1; fx[1] = 0; fx[2] = 1;
                fy[0] = -0.9214816746; fy[1] = 1.9196603801;
                break;
            case ECG:
                //fs = 100 Hz, fc1 = 1.5Hz, fc2 = 40Hz
                gain = 1.353880782e+00;
                fx[0] = -1; fx[1] = 0; fx[2] = 1;
                fy[0] = 0.4515173131; fy[1] = 0.4094486552;
                break;
            case SCG:
                //fs = 100 Hz, fc1 = 12Hz, fc2 = 40Hz
                gain = 1.826471689e+00;
                fx[0] = -1; fx[1] = 0; fx[2] = 1;
                fy[0] = 0.0945278312; fy[1] = -0.0891950551;
                break;
            default:
                break;
        }
    }


    public void findNextY() {
        //Advance in time
        xv[0] = xv[1]; xv[1] = xv[2];

        //Set the next input
        xv[2] = (float) (x[currentIndex] / gain);

        //Advance in time
        yv[0] = yv[1]; yv[1] = yv[2];

        //Calculate current output
        yv[2] = (float) (xv[0] * fx[0] + xv[1] * fx[1] + xv[2] * fx[2]
            + yv[0] * fy[0] + yv[1] * fy[1]);


        numPoints ++;
        currentIndex = numPoints% MAX_POINTS_ARRAY;
        y[currentIndex] = yv[2];
        //y[currentIndex] = x[currentIndex];
    }

    public void setXv(float xvValue, int index) {
        xv[index] = (float) (xvValue / GAIN);
    }

    public void setYv(float yvValue, int index) {
        yv[index] = yvValue;
    }

    public void lerp() {

    }

}