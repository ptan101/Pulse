package tan.philip.nrf_ble.Algorithms;

import android.media.Image;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.Algorithms.Filter.Filter;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.ImageSeries;

public class TimeOfFlightAlgorithm extends Biometric {
    private static final String TAG = "ToF Algorithm";
    private int tnsIndex = 0;
    private int c1Index = 1;
    private int c2Index = 2;
    private int t1Index = 3;

    private static final int CALIBRATION2_PERIODS = 10;
    private static final float CLOCK_PERIOD = 1/8e6f;

    public TimeOfFlightAlgorithm(HashMap<Integer, SignalSetting> signalsInAlgorithm, byte index) {
        super(signalsInAlgorithm, index,"M-mode");
    }

    @Override
    public void startAlgorithm() {
        //Check if the number of imported parameters for the bandpass filter is correct
        if(signalsInAlgorithm.size() != 4) {
            Log.e(TAG, "Wrong number of signals for ToF algorithm.");
            return;
        }

        super.startAlgorithm();
    }

    @Override
    public void computeAndDisplay(HashMap<Integer, ArrayList<Integer>> allNewData) {
        if(!this.algorithmReady)
            return;

        ArrayList<Integer> cal1s = allNewData.get(c1Index);
        ArrayList<Integer> cal2s = allNewData.get(c2Index);
        ArrayList<Integer> t1s = allNewData.get(t1Index);
        ArrayList<Integer> tns = allNewData.get(tnsIndex);

        ArrayList<Float> depths = new ArrayList<>();

        if(graphSeries.size() < 1) {
            Log.e(TAG, "ToF algorithm does not have any GraphSeries to plot to.");
            algorithmReady = false;
            return;
        }
        if(!(graphSeries.get(0) instanceof  ImageSeries)) {
            Log.e(TAG, "ToF algorithm does not have the right GraphSeries to plot to.");
            algorithmReady = false;
            return;
        }

        ImageSeries imageSeries = (ImageSeries) graphSeries.get(0);
        for(int i = 0; i < tns.size(); i +=2) {
            float tn = tns.get(i + 1);
            float clockCount = tns.get(i);
            float cal1 = cal1s.get(i/imageSeries.getNToFsPerLine()/2);
            float cal2 = cal2s.get(i/imageSeries.getNToFsPerLine()/2);
            float t1 = t1s.get(i/imageSeries.getNToFsPerLine()/2);

            depths.add(processSample(cal1, cal2, t1, tn, clockCount));
        }

        imageSeries.queueDataPoints(depths, 0);
    }

    private float processSample(float cal1, float cal2, float t1, float tn, float clockCount) {
        float calCount = (cal2 - cal1) / (CALIBRATION2_PERIODS - 1);
        float normLSB = CLOCK_PERIOD / calCount;
        float tof = normLSB * (t1 - tn) + clockCount * CLOCK_PERIOD;
        float depth = tof * 1540 / 2 * 1000;
        return  depth;
    }
}
