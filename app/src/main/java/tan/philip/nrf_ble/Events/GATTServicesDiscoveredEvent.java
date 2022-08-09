package tan.philip.nrf_ble.Events;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;

public class GATTServicesDiscoveredEvent {
    ArrayList<BluetoothGattService> mServices;
    BluetoothDevice mDevice;

    public GATTServicesDiscoveredEvent(ArrayList<BluetoothGattService> services, BluetoothDevice device) {
        mServices = services;
        mDevice = device;
    }

    public ArrayList<BluetoothGattService> getGATTServices() {
        return mServices;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
