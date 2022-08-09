package tan.philip.nrf_ble.BLE.Gatt;

public interface GattCharacteristicReadCallback {
    void call(byte[] characteristic);
}