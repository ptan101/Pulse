package tan.philip.nrf_ble.Events.UIRequests;

public class RequestChangeAutoScaleAll {
    private final boolean autoscale;

    public RequestChangeAutoScaleAll(boolean autoscale) {
        this.autoscale = autoscale;
    }

    public boolean getAutoscale() {
        return autoscale;
    }

}
