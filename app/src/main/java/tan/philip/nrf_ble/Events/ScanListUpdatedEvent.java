package tan.philip.nrf_ble.Events;

import android.bluetooth.BluetoothDevice;

import java.util.Map;

public class ScanListUpdatedEvent {
    Map<String, BluetoothDevice> mScanResults;
    Map<String, Integer> mRSSIs;
    Map<String, Boolean> isInitialized;

    public ScanListUpdatedEvent(Map<String, BluetoothDevice> scanResults, Map<String, Integer> rssis, Map<String, Boolean> isInitialized) {
        this.mScanResults = scanResults;
        this.mRSSIs = rssis;
        this.isInitialized = isInitialized;
    }

    public Map<String, BluetoothDevice> getScanResults() {
        return mScanResults;
    }

    public Map<String, Integer> getRSSIs() {
        return mRSSIs;
    }

    public Map<String, Boolean> getIsInitialized() {
        return isInitialized;
    }
}
