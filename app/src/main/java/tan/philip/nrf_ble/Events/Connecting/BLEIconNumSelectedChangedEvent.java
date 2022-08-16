package tan.philip.nrf_ble.Events.Connecting;

public class BLEIconNumSelectedChangedEvent {
    private final int numDevicesSelected;

    public BLEIconNumSelectedChangedEvent(int numDevicesSelected) {
        this.numDevicesSelected = numDevicesSelected;
    }

    public int getNumDevicesSelected() {
        return numDevicesSelected;
    }
}
