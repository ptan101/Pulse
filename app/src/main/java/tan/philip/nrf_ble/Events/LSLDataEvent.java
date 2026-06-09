package tan.philip.nrf_ble.Events;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;

public class LSLDataEvent {
    private final HashMap<Integer, ArrayList<Float>> filteredData;
    private final BLEDevice device;

    public LSLDataEvent(HashMap<Integer, ArrayList<Float>> filteredData, BLEDevice device) {
        this.filteredData = filteredData;
        this.device = device;
    }

    public HashMap<Integer, ArrayList<Float>> getFilteredData() {
        return filteredData;
    }

    public BLEDevice getDevice() {
        return device;
    }
}
