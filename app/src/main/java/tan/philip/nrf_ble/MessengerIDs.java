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
    public static final int MSG_DISCONNECT = 8;
    public static final int MSG_CHECK_BT_ENABLED = 9;
    public static final int MSG_START_RECORD = 10;
    public static final int MSG_STOP_RECORD = 11;
    public static final int MSG_STOP_FOREGROUND = 12;
    public static final int MSG_START_DEBUG_MODE = 13;
    public static final int MSG_TMS_MSG_TRANSMIT = 14;
    //Service -> Client
    public static final int MSG_BT_DEVICES = -1;
    public static final int MSG_SEND_PACKAGE_INFORMATION = -2;
    public static final int MSG_GATT_CONNECTED = -3;
    public static final int MSG_GATT_DISCONNECTED = -4;
    public static final int MSG_GATT_FAILED = -5;
    public static final int MSG_GATT_SERVICES_DISCOVERED = -6;
    public static final int MSG_GATT_ACTION_DATA_AVAILABLE = -7;
    public static final int MSG_CHECK_PERMISSIONS = -8;
    public static final int MSG_UNRECOGNIZED_NUS_DEVICE = -9;
    public static final int MSG_TMS_MSG_RECIEVED = -10;
}
