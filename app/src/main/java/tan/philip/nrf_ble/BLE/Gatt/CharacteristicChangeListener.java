package tan.philip.nrf_ble.BLE.Gatt;

import android.bluetooth.BluetoothGattCharacteristic;

public interface CharacteristicChangeListener {
    void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic);
}