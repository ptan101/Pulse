package tan.philip.nrf_ble.Events.UIRequests;

import android.bluetooth.BluetoothDevice;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;

public class RequestSendTMSEvent {
    private final byte[] messageId;
    private final BluetoothDevice device;

    public RequestSendTMSEvent(BluetoothDevice device, byte[] messageId) {
        this.messageId = messageId;
        this.device = device;
    }

    public byte[] getMessageId() {
        return messageId;
    }

    public BluetoothDevice getBleDevice() {
        return device;
    }
}
