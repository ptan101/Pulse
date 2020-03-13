package tan.philip.nrf_ble;

import java.util.UUID;

public class Constants {
    public static final int REQUEST_ENABLE_BT = 1;

    // Stops scanning after 5 seconds.
    public static final long SCAN_PERIOD = 10000;

    public static String SERVICE_STRING = "ACDCDCD0-0451-9D97-CC4B-F5A1B93E25BA";
    public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

    public static String CHARACTERISTIC_STRING = "ACDCDCD2-0451-9D97-CC4B-F5A1B93E25BA";
    public static UUID CHARACTERISTIC_UUID = UUID.fromString(CHARACTERISTIC_STRING);

    public static String CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_STRING = "00002902-0000-1000-8000-00805f9b34fb";
    public static UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_STRING);



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
}