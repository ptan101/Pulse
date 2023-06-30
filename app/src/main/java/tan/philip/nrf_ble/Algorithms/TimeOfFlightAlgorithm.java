package tan.philip.nrf_ble.Algorithms;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.Algorithms.Filter.Filter;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;

public class TimeOfFlightAlgorithm extends Biometric {
    private static final String TAG = "ToF Algorithm";

    public TimeOfFlightAlgorithm(HashMap<Integer, SignalSetting> signalsInAlgorithm) {
        super(signalsInAlgorithm);
    }

    @Override
    public void startAlgorithm() {
        //Check if the number of imported parameters for the bandpass filter is correct
        if(signalsInAlgorithm.size() != 3) {
            Log.e(TAG, "Wrong number of signals for ToF algorithm.");
            return;
        }

        super.startAlgorithm();
    }

    @Override
    public void computeAndDisplay(HashMap<Integer, ArrayList<Integer>> allNewData) {
        if(!this.algorithmReady)
            return;

        //ArrayList<Integer> rawData = allNewData.get(signalID);

        //for(float newSample : rawData) {
            //processSample(newSample);
        //}
    }
}
