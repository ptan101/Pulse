package tan.philip.nrf_ble.Algorithms;

import java.util.ArrayList;

import static java.lang.Math.round;

public class ZeroCrossingAlgorithm extends Biometric {
    /**
     * This algorithm calculates rate through the zero crossing method. It can be used for HR or RR.
     */
    private static final int MAX_BPM = 210;
    private static final int MIN_BPM = 40;
    private static final int MA_SIZE = 4;                              //How many samples to average

    //private static final int BUFFER_SIZE = 200;                      //This should not be hardcoded...
    //float[] signal_buffer = new float[BUFFER_SIZE];                  //Holds previous data points. Maybe I don't need a buffer actually
    //private int curIndex = 0;                                        //Index for next element in signal_buffer
    private float last_point= 1;
    private float bpm;
    private float delta;
    private int num_beats = 0;
    private final float[] beat_buffer = new float[MA_SIZE];


    public ZeroCrossingAlgorithm(int[] signalIndices, int sampleRate) {
        super(signalIndices, sampleRate);
    }

    /**
     * Calculates heart rate with the Maxim algorithm
     * May be helpful reading ->
     * https://github.com/sparkfun/SparkFun_MAX3010x_Sensor_Library/blob/master/src/heartRate.cpp
     * @param newData All the new data to calculate heart rate from
     */
    @Override
    public void computeAndDisplay(ArrayList<float[]> newData) {
        for (float x: newData.get(0)) {
            if(checkForBeat(x)) {
                bpm = 60000 / delta;
                delta = 0;

                if (bpm > MIN_BPM && bpm < MAX_BPM) {
                    beat_buffer[num_beats % MA_SIZE] = bpm;

                    if (num_beats >= MA_SIZE) {
                        //Display the average BPM
                        float avg_bpm = 0;
                        for (Float f : beat_buffer)
                            avg_bpm += f;
                        avg_bpm /= MA_SIZE;
                        //this.digitalDisplay.changeValue(round(avg_bpm));
                        this.digitalDisplay.changeValue(avg_bpm);
                    }


                    num_beats++;
                }
            }

            delta += 1000f/sampleRate;
        }
    }

    /**
     * Takes data one by one to calculate heart rate based on zero crossing.
     * @param newData Single float of new data. Assumes data is filtered (high pass, i.e., centered around 0)
     * @return True if a heartbeat was detected at the current data point
     */
    private boolean checkForBeat(float newData) {
        boolean out = false;

        //Detect positive zero crossing (rising edge)
        if (last_point < 0 & newData >= 0) {
            out = true;
        }

        //Save the current point for future analysis
        last_point = newData;

        return out;
    }

}
