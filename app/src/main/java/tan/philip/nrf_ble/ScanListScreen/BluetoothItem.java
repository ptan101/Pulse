package tan.philip.nrf_ble.ScanListScreen;

public class BluetoothItem {
    private int mImageResource;
    private String mText1;

    public BluetoothItem(int imageResource, String text1) {
        mImageResource = imageResource;
        mText1 = text1;
    }

    public void changeText1(String text) {
        mText1 = text;
    }

    public int getImageResource() {
        return mImageResource;
    }

    public String getText1() {
        return mText1;
    }
}