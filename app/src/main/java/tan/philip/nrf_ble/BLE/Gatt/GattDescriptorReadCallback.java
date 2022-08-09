package tan.philip.nrf_ble.BLE.Gatt;

public interface GattDescriptorReadCallback {
    void call(byte[] value);
}