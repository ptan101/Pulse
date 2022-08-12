package tan.philip.nrf_ble.SickbayPush;

public class SickbayQueueItem {
    public int data;
    public int instanceId;

    public SickbayQueueItem(int data, int instanceId) {
        this.data = data;
        this.instanceId = instanceId;
    }
}
