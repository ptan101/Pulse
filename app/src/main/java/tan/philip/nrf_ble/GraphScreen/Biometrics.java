package tan.philip.nrf_ble.GraphScreen;

import java.io.Serializable;
import java.util.ArrayList;

public class Biometrics implements Serializable {
    private ArrayList<Integer> hr_signals = new ArrayList<>();      //Holds indices of which signals to calculate HR from
    private ArrayList<int[]> spo2_signals = new ArrayList<>();      //Holds set of indices of which signals to calculate SpO2 from [IR_AC, IR_DC, RED_AC, RED_DC]
    private ArrayList<int[]> pwv_signals = new ArrayList<>();       //Holds set of indices of which signals to calculate PWV from [proximal, distal]

    public Biometrics () {

    }

    public void addHRSignals (int index_of_signal) {
        hr_signals.add(index_of_signal);
    }

    public void addSpO2Signals (int ir_ac, int ir_dc, int red_ac, int red_dc) {
        spo2_signals.add(new int[] {ir_ac, ir_dc, red_ac, red_dc});
    }

    public void addPWVSignals (int proximal_index, int distal_index) {
        pwv_signals.add(new int[] {proximal_index, distal_index});
    }

    public ArrayList<Integer> getHr_signals() {
        return hr_signals;
    }

    public ArrayList<int[]> getSpo2_signals() {
        return spo2_signals;
    }

    public ArrayList<int[]> getPwv_signals() {
        return pwv_signals;
    }
}
