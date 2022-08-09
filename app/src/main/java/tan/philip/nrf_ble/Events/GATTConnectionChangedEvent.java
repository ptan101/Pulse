package tan.philip.nrf_ble.Events;

public class GATTConnectionChangedEvent {
    private String mAddress;
    private int mNewState;

    public GATTConnectionChangedEvent(String address, int newState) {
        mAddress = address;
        mNewState = newState;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getNewState() {
        return mNewState;
    }
}
