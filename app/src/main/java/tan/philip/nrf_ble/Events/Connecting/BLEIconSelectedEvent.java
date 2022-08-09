package tan.philip.nrf_ble.Events.Connecting;

public class BLEIconSelectedEvent {
    String address;
    boolean isSelected;

    public BLEIconSelectedEvent(String address, boolean isSelected) {
        this.address = address;
        this.isSelected = isSelected;
    }

    public String getAddress() {
        return address;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
