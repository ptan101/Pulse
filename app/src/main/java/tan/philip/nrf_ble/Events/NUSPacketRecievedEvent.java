package tan.philip.nrf_ble.Events;

import tan.philip.nrf_ble.BLE.BLEDevices.BLETattooDevice;

public class NUSPacketRecievedEvent {
    private final BLETattooDevice mTattoo;
    private final byte[] mData;

    public NUSPacketRecievedEvent(BLETattooDevice tattoo, byte[] data) {
        mTattoo = tattoo;
        mData = data;
    }

    public BLETattooDevice getTattoo() {
        return mTattoo;
    }

    public byte[] getPacketData() {
        return mData;
    }

}
