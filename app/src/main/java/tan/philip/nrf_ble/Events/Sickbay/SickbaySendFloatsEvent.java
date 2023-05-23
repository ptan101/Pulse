package tan.philip.nrf_ble.Events.Sickbay;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;

public class SickbaySendFloatsEvent {
    private final HashMap<Integer, ArrayList<Float>> data;
    private final BLEDevice device;

    public SickbaySendFloatsEvent(HashMap<Integer, ArrayList<Float>> data, BLEDevice device) {
        this.data = data;
        this.device = device;
    }

    public HashMap<Integer, ArrayList<Float>> getData() {
        return data;
    }

    public BLEDevice getBLEDevice() {
        return device;
    }
}
