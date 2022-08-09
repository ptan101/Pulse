package tan.philip.nrf_ble;

import android.content.Context;
import android.util.DisplayMetrics;

import java.util.UUID;

public class Constants {
    public static final int REQUEST_ENABLE_BT = 1;

    // Stops scanning after 5 seconds.
    public static final long SCAN_PERIOD = 10000;

    //Nordic UART Service UUID
    public static final String NUS_UUID_STRING = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final UUID NUS_UUID = UUID.fromString(NUS_UUID_STRING);

    //NUS RX Characteristic UUID
    public static final String NUS_RX_UUID_STRING = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final UUID NUS_RX_UUID = UUID.fromString(NUS_RX_UUID_STRING);

    //NUS TX Characteristic UUID
    public static final String NUS_TX_UUID_STRING = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final UUID NUS_TX_UUID = UUID.fromString(NUS_TX_UUID_STRING);

    public static String CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_STRING = "00002902-0000-1000-8000-00805f9b34fb";
    public static UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_STRING);


    //Tattoo Messaging Service UUID
    public static final String TMS_UUID_STRING = "F3641400-00B0-4240-BA50-05CA45BF8AB1";
    public static final UUID TMS_UUID = UUID.fromString(TMS_UUID_STRING);

    //TMS RX Characteristic UUID
    public static final String TMS_TX_UUID_STRING = "F3641401-00B0-4240-BA50-05CA45BF8AB1";
    public static final UUID TMS_TX_UUID = UUID.fromString(TMS_TX_UUID_STRING);

    //TMS TX Characteristic UUID
    public static final String TMS_RX_UUID_STRING = "F3641402-00B0-4240-BA50-05CA45BF8AB1";
    public static final UUID TMS_RX_UUID = UUID.fromString(TMS_RX_UUID_STRING);


    public static String CHARACTERISTIC_TIME_STRING = "7D2EDEAD-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID CHARACTERISTIC_TIME_UUID = UUID.fromString(CHARACTERISTIC_TIME_STRING);
    public static String CLIENT_CONFIGURATION_DESCRIPTOR_STRING = "00002902-0000-1000-8000-00805f9b34fb";
    public static UUID CLIENT_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString(CLIENT_CONFIGURATION_DESCRIPTOR_STRING);

    public static final String CLIENT_CONFIGURATION_DESCRIPTOR_SHORT_ID = "2902";

    //Convert bytes to hex string
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}