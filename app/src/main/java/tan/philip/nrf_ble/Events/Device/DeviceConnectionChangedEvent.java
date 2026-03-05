package tan.philip.nrf_ble.Events.Device;

public class DeviceConnectionChangedEvent {
    private final String address;
    private final String displayName;
    private final boolean connected;

    public DeviceConnectionChangedEvent(String address, String displayName, boolean connected) {
        this.address = address;
        this.displayName = displayName;
        this.connected = connected;
    }
    public String getAddress() { return address; }
    public String getDisplayName() { return displayName; }
    public boolean isConnected() { return connected; }
}
