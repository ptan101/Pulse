package tan.philip.nrf_ble.GraphScreen;

import java.util.ArrayList;

public class HeartDataAnalysis {

    public static final int NO_DATA = -1;
    public static final float SENSOR_DISTANCE = 5.0f;

    public static final int MIN_POSSIBLE_HR = 40;
    public static final int MAX_POSSIBLE_HR = 200;
    private static final int HR_DATA_TO_REJECT = 6;         //Reject this number of initial data points

    private int maxHR = NO_DATA;
    private int minHR = NO_DATA;
    private int numHRPeaks = 0;

    private ArrayList<Float> pwvPoints;

    public HeartDataAnalysis() {
        pwvPoints = new ArrayList<>();
    }

    public int calculateHeartRate(float timeBetweenPulseInSeconds) {
        int hr = Math.round(60f / timeBetweenPulseInSeconds);
        if(hr < MIN_POSSIBLE_HR || hr > MAX_POSSIBLE_HR)
            return NO_DATA;

        numHRPeaks ++;

        if(numHRPeaks <= HR_DATA_TO_REJECT)
            return NO_DATA;

        if(maxHR == NO_DATA || hr > maxHR)
            maxHR = hr;
        if(minHR == NO_DATA || hr < minHR)
            minHR = hr;
        return hr;
    }

    public float calculatePWV(float timeBetweenPulses) {
        float newPWV = SENSOR_DISTANCE / timeBetweenPulses;

        if(newPWV != newPWV || !Float.isFinite(newPWV))
            return NO_DATA;

        if(pwvPoints.isEmpty())
            pwvPoints.add(newPWV);
        else {
            for(int i = 0; i < pwvPoints.size(); i ++) {
                if(pwvPoints.get(i) > newPWV) {
                    pwvPoints.add(i, newPWV);
                    break;
                }
                if(i == pwvPoints.size() - 1) {
                    pwvPoints.add(newPWV);
                    break;
                }
            }
        }

        return newPWV;
    }

    public float calculateWeightedAveragePWV() {
        float sumPWV = 0;

        //int trim = pwvPoints.size() /3;
        int trim = 0;
        int numPoints = 0;

        for(int i = trim; i < pwvPoints.size() - trim; i++) {
            sumPWV += pwvPoints.get(i);
            numPoints++;
        }

        return sumPWV / numPoints;
    }

    public int getNumPWVPoints() {
        return pwvPoints.size();
    }


    public int getMinHR() {
        return minHR;
    }

    public int getMaxHR() {
        return maxHR;
    }

    public int getRangeHR() {
        if(maxHR == NO_DATA || minHR == NO_DATA)
            return NO_DATA;

        return maxHR - minHR;
    }

    public int getNumHRPeaks() {
        return numHRPeaks;
    }
}