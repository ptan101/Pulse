package tan.philip.nrf_ble.BLE.Gatt.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

public class GattDisconnectOperation extends GattOperation {

    public GattDisconnectOperation(BluetoothDevice device) {
        super(device);
    }

    @Override
    public void execute(BluetoothGatt gatt) {
        gatt.disconnect();
    }

    @Override
    public boolean hasAvailableCompletionCallback() {
        return true;
    }
}