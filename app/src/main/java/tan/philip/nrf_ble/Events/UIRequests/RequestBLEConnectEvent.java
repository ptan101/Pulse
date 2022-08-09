package tan.philip.nrf_ble.Events.UIRequests;

import java.util.ArrayList;

public class RequestBLEConnectEvent {
    ArrayList<String> addresses = new ArrayList<>();

    public RequestBLEConnectEvent(ArrayList<String> addresses) {
        this.addresses = addresses;
    }

    public ArrayList<String> getAddresses() {
        return addresses;
    }
}
