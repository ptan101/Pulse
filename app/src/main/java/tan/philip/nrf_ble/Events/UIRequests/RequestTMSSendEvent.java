package tan.philip.nrf_ble.Events.UIRequests;

public class RequestTMSSendEvent {
    private int mMsgID;

    public RequestTMSSendEvent(int msgID) {
        mMsgID = msgID;
    }

    public int getMsgId() {
        return mMsgID;
    }
}
