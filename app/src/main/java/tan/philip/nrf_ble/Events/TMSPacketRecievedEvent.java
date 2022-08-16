package tan.philip.nrf_ble.Events;

import tan.philip.nrf_ble.BLE.PacketParsing.TattooMessage;

public class TMSPacketRecievedEvent {
    private final TattooMessage mMsg;

    public TMSPacketRecievedEvent(TattooMessage msg) {
        mMsg = msg;
    }

    public TattooMessage getTattooMessage() {
        return mMsg;
    }
}
