package tan.philip.nrf_ble.Events;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GATTServicesDiscoveredEvent {
    private final ArrayList<UUID> mServiceUUIDs;
    private final BluetoothDevice mDevice;

    public GATTServicesDiscoveredEvent(ArrayList<BluetoothGattService> services, BluetoothDevice device) {
        mServiceUUIDs = new ArrayList<>();
        mDevice = device;

        for(BluetoothGattService s: services)
            mServiceUUIDs.add(s.getUuid());
    }

    public ArrayList<UUID> getGATTServiceUUIDs() {
        return mServiceUUIDs;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
