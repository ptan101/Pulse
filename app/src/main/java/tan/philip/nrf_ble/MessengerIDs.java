package tan.philip.nrf_ble;

//Should be an enum but bit more complicated using an enum as the message ID
public class MessengerIDs {
    //Client -> Service
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_START_SCAN = 3;
    public static final int MSG_STOP_SCAN = 4;
    public static final int MSG_CLEAR_SCAN = 5;
    public static final int MSG_REQUEST_SCAN_RESULTS = 6;
    public static final int MSG_CONNECT = 7;
    public static final int MSG_DISCONNECT = 17;
    public static final int MSG_CHECK_BT_ENABLED = 16;
    public static final int MSG_START_RECORD = 18;
    public static final int MSG_STOP_RECORD = 19;
    public static final int MSG_STOP_FOREGROUND = 21;
    public static final int MSG_START_DEBUG_MODE = 22;
    //Service -> Client
    public static final int MSG_BT_DEVICES = 8;
    public static final int MSG_SEND_PACKAGE_INFORMATION = 9;
    public static final int MSG_GATT_CONNECTED = 10;
    public static final int MSG_GATT_DISCONNECTED = 11;
    public static final int MSG_GATT_FAILED = 12;
    public static final int MSG_GATT_SERVICES_DISCOVERED = 13;
    public static final int MSG_GATT_ACTION_DATA_AVAILABLE = 14;
    public static final int MSG_CHECK_PERMISSIONS = 15;
    public static final int MSG_UNRECOGNIZED_NUS_DEVICE = 20;
}
