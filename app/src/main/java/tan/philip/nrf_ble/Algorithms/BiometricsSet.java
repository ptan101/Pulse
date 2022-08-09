package tan.philip.nrf_ble.Algorithms;

import java.io.Serializable;
import java.util.ArrayList;

public class BiometricsSet implements Serializable {
    private final ArrayList<Biometric> biometrics = new ArrayList<>();

    /*
    //Things to handle heart rate. Move this to ZeroCrossingAlgorithm.java
    private ArrayList<ZeroCrossingAlgorithm> hr_signals = new ArrayList<>();      //Holds indices of which signals to calculate HR from
    private ArrayList<ZeroCrossingAlgorithm> hr_buffers = new ArrayList<>();  //Holds buffers

    private ArrayList<int[]> spo2_signals = new ArrayList<>();      //Holds set of indices of which signals to calculate SpO2 from [IR_AC, IR_DC, RED_AC, RED_DC]
    private ArrayList<int[]> pwv_signals = new ArrayList<>();       //Holds set of indices of which signals to calculate PWV from [proximal, distal]
    */
    public BiometricsSet() {

    }

    public void addHRSignals (int index_of_signal) {
        //hr_buffers.add(new ZeroCrossingAlgorithm(index_of_signal, 20));
        biometrics.add(new ZeroCrossingAlgorithm(new int[] {index_of_signal}, 20));
        //biometrics.add(new Biometric(new int[] {0}, 20));
    }

    public void addSpO2Signals (int ir_ac, int ir_dc, int red_ac, int red_dc) {
        //spo2_signals.add(new int[] {ir_ac, ir_dc, red_ac, red_dc});
        biometrics.add(new SpO2Algorithm(new int[] {ir_ac, ir_dc, red_ac, red_dc}, 20));
    }

    public void addPWVSignals (int proximal_index, int distal_index) {
        //pwv_signals.add(new int[] {proximal_index, distal_index});
        biometrics.add(new PWVAlgorithm(new int[] {proximal_index, distal_index}, 20));
    }

    public ArrayList<ZeroCrossingAlgorithm> getHr_signals() {
        //return hr_signals;
        ArrayList<ZeroCrossingAlgorithm> out = new ArrayList<>();
        for(Biometric b: biometrics) {
            if (b instanceof ZeroCrossingAlgorithm) {
                out.add((ZeroCrossingAlgorithm) b);
            }
        }

        return out;
    }

    //Would be better to make this repeatable
    public ArrayList<SpO2Algorithm> getSpo2_signals() {
        ArrayList<SpO2Algorithm> out = new ArrayList<>();
        for(Biometric b: biometrics) {
            if (b instanceof SpO2Algorithm) {
                out.add((SpO2Algorithm) b);
            }
        }

        return out;
    }

    public ArrayList<PWVAlgorithm> getPwv_signals() {
        ArrayList<PWVAlgorithm> out = new ArrayList<>();
        for(Biometric b: biometrics) {
            if (b instanceof PWVAlgorithm) {
                out.add((PWVAlgorithm) b);
            }
        }

        return out;
    }

    /**
     * Will run through all biometric algorithms and update their respective DigitalDisplays
     */
    public void computeAndDisplay(ArrayList<float[]> newData) {
        for(Biometric b: biometrics) {
            ArrayList<Integer> indices = b.signalIndices;       //Need to know which signals to use for the algorithm
            ArrayList<float []> dataToProcess = new ArrayList<float []>();
            for(Integer i : indices)
                dataToProcess.add(newData.get(i));

            b.computeAndDisplay(dataToProcess);
        }
    }
}
