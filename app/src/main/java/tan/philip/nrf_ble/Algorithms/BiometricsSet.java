package tan.philip.nrf_ble.Algorithms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class BiometricsSet implements Serializable {
    //Should I just hold everything in an arraylist instead of making a new object?
    private final ArrayList<Biometric> biometrics = new ArrayList<>();

    public BiometricsSet() {

    }

    public void add(Biometric biometric) {
        biometrics.add(biometric);
    }

    /**
     * Will run through all biometric algorithms and update their respective DigitalDisplays
     */
    public void computeAndDisplay(HashMap<Integer, ArrayList<Integer>> filteredData) {
        for(Biometric b: biometrics) {
            b.computeAndDisplay(filteredData);
        }
    }

    public Biometric get(int i) {
        return biometrics.get(i);
    }

    public int size() {
        return biometrics.size();
    }

    public boolean isEmpty() {
        return biometrics.isEmpty();
    }

    public ArrayList<Biometric> getBiometrics() { return biometrics; }
}
