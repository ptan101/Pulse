package tan.philip.nrf_ble.Events.Sickbay;

import java.util.ArrayList;
import java.util.HashMap;

public class SickbayQueueEvent {
    private final HashMap<Integer, ArrayList<Integer>> data;
    private final int instanceId;

    public SickbayQueueEvent(HashMap<Integer, ArrayList<Integer>> data, int instanceId) {
        this.data = data;
        this.instanceId = instanceId;
    }

    public HashMap<Integer, ArrayList<Integer>> getData() {
        return data;
    }

    public int getInstanceId() {
        return instanceId;
    }
}
