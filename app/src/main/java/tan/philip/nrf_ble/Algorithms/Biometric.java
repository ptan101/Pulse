package tan.philip.nrf_ble.Algorithms;

import java.io.Serializable;
import java.util.ArrayList;

import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay;

public class Biometric implements Serializable {
    public ArrayList<Integer> signalIndices = new ArrayList<>(); //Which signal (from ArrayList of SignalSettings or GraphSettings) to use
    public int sampleRate;

    public DigitalDisplay digitalDisplay;

    public Biometric (int[] signalIndices, int sampleRate) {
        for (int i = 0; i < signalIndices.length; i ++)
            this.signalIndices.add(signalIndices[i]);


        this.sampleRate = sampleRate;
    }

    public void setDigitalDisplay(DigitalDisplay display) {
        this.digitalDisplay = display;
    }

    /**
     * I feel like this should be an interface but all children of this class can extend this to run algorithm and update DD.
     */
    public void computeAndDisplay(ArrayList<float[]> newData) {}

}
