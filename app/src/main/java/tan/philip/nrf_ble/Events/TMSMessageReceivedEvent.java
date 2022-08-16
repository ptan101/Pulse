package tan.philip.nrf_ble.Events;

import tan.philip.nrf_ble.BLE.PacketParsing.TattooMessage;

public class TMSMessageReceivedEvent {
    private final String address;
    private final TattooMessage message;

    public TMSMessageReceivedEvent(String address, TattooMessage message) {
        this.message = message;
        this.address = address;
    }

    public TattooMessage getMessage() {
        return message;
    }

    public String getAddress() {
        return address;
    }
}
