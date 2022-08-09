package tan.philip.nrf_ble.Events;

import java.util.ArrayList;
import java.util.HashMap;

public class PlotDataEvent {
    String deviceAddress;
    HashMap<Integer, ArrayList<Float>> filteredData;

    public PlotDataEvent(String deviceAddress, HashMap<Integer, ArrayList<Float>> filteredData) {
        this.filteredData = filteredData;
        this.deviceAddress = deviceAddress;
    }

    public HashMap<Integer, ArrayList<Float>> getFilteredData() {
        return filteredData;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }
}
