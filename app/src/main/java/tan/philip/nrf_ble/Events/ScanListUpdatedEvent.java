package tan.philip.nrf_ble.Events;

import android.bluetooth.BluetoothDevice;

import java.util.Map;

public class ScanListUpdatedEvent {
    Map<String, BluetoothDevice> mScanResults;

    public ScanListUpdatedEvent(Map<String, BluetoothDevice> scanResults) {
        this.mScanResults = scanResults;
    }

    public Map<String, BluetoothDevice> getScanResults() {
        return mScanResults;
    }
}
