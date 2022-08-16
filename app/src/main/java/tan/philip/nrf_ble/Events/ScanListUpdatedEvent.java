package tan.philip.nrf_ble.Events;

import android.bluetooth.BluetoothDevice;

import java.util.Map;

public class ScanListUpdatedEvent {
    Map<String, BluetoothDevice> mScanResults;
    Map<String, Integer> mRSSIs;

    public ScanListUpdatedEvent(Map<String, BluetoothDevice> scanResults, Map<String, Integer> rssis) {
        this.mScanResults = scanResults;
        this.mRSSIs = rssis;
    }

    public Map<String, BluetoothDevice> getScanResults() {
        return mScanResults;
    }

    public Map<String, Integer> getRSSIs() {
        return mRSSIs;
    }
}
