package tan.philip.nrf_ble.Events.UIRequests;

public class RequestChangeRecordEvent {
    private boolean mRecord;

    private String mFilename;

    public RequestChangeRecordEvent(boolean record, String filename) {
        mRecord = record;
        mFilename = filename;
    }

    public boolean startRecord() {
        return mRecord;
    }

    public String getFilename() {
        return mFilename;
    }
}
