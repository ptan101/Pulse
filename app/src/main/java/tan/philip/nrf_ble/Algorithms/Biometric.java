package tan.philip.nrf_ble.Algorithms;

import android.app.AlertDialog;
import android.content.Context;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.GraphSeries;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplay;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplaySettings;
import tan.philip.nrf_ble.GraphScreen.UIComponents.ValueAlert;

public abstract class Biometric implements Serializable {
    private static final int BIOMETRIC_DISPLAY_RATE = 1000;
    public HashMap<Integer, SignalSetting> signalsInAlgorithm;
    public ArrayList<Float> inputParameters;

    public DigitalDisplaySettings ddSettings;
    public DigitalDisplay digitalDisplay;

    public SignalSetting setting;
    public ArrayList<GraphSeries> graphSeries = new ArrayList<>();

    public boolean logData = false;

    protected boolean algorithmReady = false;

    public ArrayList<GraphSeries> getGraphSeries() {
        return graphSeries;
    }

    public ArrayList<ValueAlert> alerts = new ArrayList<>();

    public Biometric (HashMap<Integer, SignalSetting> signalsInAlgorithm, byte index, String name) {
        this.signalsInAlgorithm = new HashMap(signalsInAlgorithm);
        inputParameters = new ArrayList();
        setting = new SignalSetting(index, name, (byte)0, BIOMETRIC_DISPLAY_RATE, (byte) 0, true);
    }

    public void initializeDigitalDisplaySettings() {
        ddSettings = new DigitalDisplaySettings("");
    }

    public void setDigitalDisplay(DigitalDisplay display) {
        this.digitalDisplay = display;
    }

    /**
     * I feel like this should be an interface but all children of this class can extend this to run algorithm and update DD.
     */
    public void computeAndDisplay(HashMap<Integer, ArrayList<Integer>> allNewData) {}

    /**
     * When importing the biometric algorithm from the .init file, use this to import additional
     * values. The parameters should be added in order.
     * @param param The parameter value to add.
     */
    public void addParameter(float param) {
        inputParameters.add(param);
    }

    /**
     * Once all parameters are loaded in, use this command to start running the algorithm.
     * Otherwise, the algorithm will not run.
     */
    public void startAlgorithm() {
        algorithmReady = true;
    }

    /**
     * Returns if the algorithm output should be displayed using a Digital Display.
     * @return True if uses Digital Display
     */
    public boolean hasDigitalDisplay() { return ddSettings != null; }

    public void addAlert(ValueAlert alert) {
        alerts.add(alert);
    }

}