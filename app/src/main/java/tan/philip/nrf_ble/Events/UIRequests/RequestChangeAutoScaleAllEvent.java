package tan.philip.nrf_ble.Events.UIRequests;

public class RequestChangeAutoScaleAllEvent {
    private final boolean autoscale;

    public RequestChangeAutoScaleAllEvent(boolean autoscale) {
        this.autoscale = autoscale;
    }

    public boolean getAutoscale() {
        return autoscale;
    }

}
