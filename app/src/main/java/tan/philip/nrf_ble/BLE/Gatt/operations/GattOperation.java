package tan.philip.nrf_ble.BLE.Gatt.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import tan.philip.nrf_ble.BLE.Gatt.GattOperationBundle;

public abstract class GattOperation {

    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 10000;
    private final BluetoothDevice mDevice;
    private GattOperationBundle mBundle;

    public GattOperation(BluetoothDevice device) {
        mDevice = device;
    }

    public abstract void execute(BluetoothGatt bluetoothGatt);

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getTimoutInMillis() {
        return DEFAULT_TIMEOUT_IN_MILLIS;
    }

    public abstract boolean hasAvailableCompletionCallback();

    public GattOperationBundle getBundle() {
        return mBundle;
    }

    public void setBundle(GattOperationBundle bundle) {
        mBundle = bundle;
    }
}