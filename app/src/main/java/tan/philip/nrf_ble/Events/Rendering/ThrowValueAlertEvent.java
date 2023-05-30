package tan.philip.nrf_ble.Events.Rendering;

public class ThrowValueAlertEvent {
    private final String mMsgTitle;
    private final String mMsg;

    public ThrowValueAlertEvent(String msgTitle, String msg) {
        mMsgTitle = msgTitle;
        mMsg = msg;
    }

    public String getMsgTitle() {
        return mMsgTitle;
    }

    public String getMsg() {
        return mMsg;
    }
}
