package tan.philip.nrf_ble.SickbayPush;

import static android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY;
import static tan.philip.nrf_ble.SickbayPush.SickbayMessage.convertPacketToJSONString;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.Events.Sickbay.SickbayQueueEvent;
import tan.philip.nrf_ble.Events.Sickbay.SickbayReinitializeEvent;
import tan.philip.nrf_ble.Events.Sickbay.SickbaySendFloatsEvent;

public class SickbayPushService extends Service {
    private static final String TAG = "SickbayPushService";
    private static final String WIFI_TAG = "SICKBAY_WIFI_LOCK";

    //Address the Sickbay IP dialog starts from, and what gets written when no IP has been
    //set yet. Includes the scheme: the clinical Sickbay servers terminate TLS, while the
    //local test bench (sickbay-test-script) is plain http.
    public static final String DEFAULT_SICKBAY_ADDRESS = "https://10.88.155.246";
    //Addresses earlier versions stamped into sickbayIP.txt on their own. A phone still
    //holding one of these was never configured by a user, so it follows the default above.
    private static final String[] LEGACY_DEFAULT_ADDRESSES = {"10.145.69.74", "192.168.50.147"};
    private static final String DEFAULT_WEB_SOCKET_PORT = "3001";
    private static final String DEFAULT_WEB_SOCKET_URL = buildWebSocketURL(DEFAULT_SICKBAY_ADDRESS);
    private String webSocketURL = DEFAULT_WEB_SOCKET_URL;

    private final String DEFAULT_BED_NAME = "BED001";
    private String bedName = DEFAULT_BED_NAME;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    //Might be better to be a hashmap. However, there are 2 keys (NS and instanceID), which is messy.
    //Key is the instanceID.
    //private final HashMap<Integer, SickbayQueue> dataQueues = new HashMap<>();
    private Handler mHandler;
    private boolean queuesInitialized = false;

    private long lastPushTime = 0;

    //Wifi Manager. Used for WiFi Lock, may be good to have in separate service dedicated for
    //handling WiFi.
    WifiManager mWifiManager;// = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    WifiManager.WifiLock mWifiLock;// = mWifiManager.createWifiLock(WIFI_MODE_FULL_LOW_LATENCY, WIFI_TAG);
    public class LocalBinder extends Binder {
        public SickbayPushService getService() {
            // Return this instance of SickbayPushService so clients can call public methods
            return SickbayPushService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        initializeSickbaySettings();

        //Register on EventBus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        disconnectSocket();
        releaseWifiLock();

        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
    }

    private void initializeSickbaySettings() {
        if (mSocket != null) disconnectSocket();

        readSickbaySettings(); //TO DO: Check if the IP address and Bed names are valid

        Log.d(TAG, "Initializing Sockets.");
        initializeSocket();

        mHandler = new Handler();
        connectSocket();

        //No need to have WiFi lock on to start probably
        mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        mWifiLock = mWifiManager.createWifiLock(WIFI_MODE_FULL_LOW_LATENCY, WIFI_TAG);
        releaseWifiLock();
    }

    //Reads the sickbay settings from local memory
    private static final String BASE_DIR_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator + "Pulse_Data";
    private void readSickbaySettings() {
        //Read the sickbay IP first
        String filePath = BASE_DIR_PATH + File.separator + "sickbayIP.txt";

        try {
            FileReader fileReader = new FileReader(filePath);

            String sickbayIP = "";
            int i;
            while ((i = fileReader.read()) != -1) {
                sickbayIP += (char)i;
            }
            if (isUnconfiguredAddress(sickbayIP))
                sickbayIP = DEFAULT_SICKBAY_ADDRESS;

            webSocketURL = buildWebSocketURL(sickbayIP);
            Log.d(TAG, "Sickbay IP set to:" + sickbayIP.trim() + " (URL " + webSocketURL + ")");

            fileReader.close();
        }
        catch (Exception e) {
            File file = new File(filePath);
            try {
                ensureBaseDirExists();
                file.createNewFile();
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(DEFAULT_SICKBAY_ADDRESS);
                fileWriter.close();

                Log.e(TAG, "Made sickbay settings file.", e);

            } catch(Exception f) {
                Log.e(TAG, "Could not make sickbay settings files.", e);
            }
        }

        //Read the sickbay bed ID
        filePath = BASE_DIR_PATH + File.separator + "sickbayBedID.txt";

        try {
            FileReader fileReader = new FileReader(filePath);

            String bedID = "";
            int i;
            while ((i = fileReader.read()) != -1) {
                bedID += (char)i;
            }
            bedID = bedID.trim();
            Log.d(TAG, "Sickbay Bed ID set to:" + bedID);

            bedName = bedID;

            fileReader.close();
        }
        catch (Exception e) {
            File file = new File(filePath);
            try {
                ensureBaseDirExists();
                file.createNewFile();
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(DEFAULT_BED_NAME);
                fileWriter.close();
                Log.e(TAG, "Made sickbay settings file.", e);

            } catch(Exception f) {
                Log.e(TAG, "Could not make sickbay settings files.", e);
            }
        }
    }

    /**
     * True if nothing has been set yet, or if the stored address is one this app wrote by
     * itself as a default. Either way the current default should be used instead.
     */
    public static boolean isUnconfiguredAddress(String address) {
        String trimmed = address.trim();

        if (trimmed.isEmpty())
            return true;

        for (String legacy : LEGACY_DEFAULT_ADDRESSES)
            if (trimmed.equals(legacy))
                return true;

        return false;
    }

    /**
     * Builds the web socket URL from whatever the user typed into the Sickbay IP dialog.
     * Accepts "host", "host:port", "http://host", "https://host:port". The scheme defaults
     * to http (the local test bench); the port defaults to 3001.
     */
    private static String buildWebSocketURL(String rawIP) {
        String scheme = "http://";
        String hostAndPort = rawIP.trim();

        if (hostAndPort.startsWith("http://") || hostAndPort.startsWith("https://")) {
            int schemeEnd = hostAndPort.indexOf("://") + 3;
            scheme = hostAndPort.substring(0, schemeEnd);
            hostAndPort = hostAndPort.substring(schemeEnd);
        }

        //Drop any trailing path, e.g. "10.0.0.5:3001/socket.io"
        int slash = hostAndPort.indexOf('/');
        if (slash >= 0)
            hostAndPort = hostAndPort.substring(0, slash);

        //Only add the default port if the user did not type one
        if (!hostAndPort.contains(":"))
            hostAndPort = hostAndPort + ":" + DEFAULT_WEB_SOCKET_PORT;

        return scheme + hostAndPort;
    }

    //The settings files live in a shared folder that may not exist yet on a fresh install.
    private static void ensureBaseDirExists() {
        File baseDir = new File(BASE_DIR_PATH);
        if (!baseDir.exists())
            baseDir.mkdirs();
    }

    private void toastToMain(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    //Connection errors retry immediately, so only surface one to the user every 10 seconds.
    private static final long ERROR_TOAST_INTERVAL_MS = 10000;
    private long lastErrorToastTime = 0;
    private void toastConnectError(String message) {
        long curTime = System.currentTimeMillis();
        if (curTime - lastErrorToastTime < ERROR_TOAST_INTERVAL_MS)
            return;
        lastErrorToastTime = curTime;
        toastToMain("Sickbay (" + webSocketURL + ") not connected: " + message);
    }

    // ////////////////////////////////Queue Functions////////////////////////////////////////////
    @Subscribe
    public void reinitalizeSickbaySettings(SickbayReinitializeEvent event) {
        initializeSickbaySettings();
    }

    @Subscribe
    public void sendSickbayFrameEvent(SickbaySendFloatsEvent event) {
        Long curTime = System.currentTimeMillis();

        JSONObject message = convertPacketToJSONString(curTime, event.getData(), event.getBLEDevice(), bedName);

        //Attempt to send the data
        attemptSend(message);
    }

    /*
    @Subscribe
    public void addToQueueEvent(SickbayQueueEvent event) {
        if(queuesInitialized && dataQueues != null)
            dataQueues.get(event.getInstanceId()).addToQueue(event.getData());
    }

    public void initializeQueues(ArrayList<BLEDevice> devices) {
        for (BLEDevice d : devices) {
            //To do: Unique namespace
            //WARNING. UNIQUE ID IS HARD CODED
            //dataQueues.put(d.getUniqueId(), new SickbayQueue(bedName, "TATTOOWAVE", d.getUniqueId(), d.getNotificationFrequency()));
            dataQueues.put(0, new SickbayQueue(bedName, "TATTOOWAVE", d.getUniqueId(), d.getNotificationFrequency()));
        }
        queuesInitialized = true;
    }

    private synchronized void pushQueue(long timestamp) {
        //Consolidate queue to a single frame and push the frame

        //For every instance ID and namespace, push the respective queue.
        for (int instanceID : dataQueues.keySet()) {
            SickbayQueue q = dataQueues.get(instanceID);
            //Reformat data in queue into string.
            JSONObject message = q.convertQueueToJSONString(timestamp);

            //Attempt to send the data
            attemptSend(message);
        }
    }

     */

    ///////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////Functions for sockets/////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private Socket mSocket;

    void initializeSocket() {
        try {
            IO.Options options = new IO.Options();
            if (webSocketURL.startsWith("https://")) {
                SocketSSL.set(options);
            }
            mSocket = IO.socket(webSocketURL, options);
            Log.d(TAG, "Socket object created. URL=" + webSocketURL);
        } catch (URISyntaxException e) {
            Log.e("Error URI", String.valueOf(e));
            throw new RuntimeException(e);
        }
    }

    //Will need a recovery
    void connectSocket() {
        //Listen for events using onNewMessage (Emitter.Listener)
        mSocket.on("new message", onNewMessage);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT, onConnection);

        mSocket.connect();
        Log.d(TAG,"Attempted to connect socket.");
    }

    void disconnectSocket() {
        mSocket.disconnect();
        mSocket.off("new message", onNewMessage);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT, onConnection);
        releaseWifiLock();
    }

    private void attemptSend(JSONObject message) {
        //If the message is empty, don't send a packet.
        if (message == null) {
            return;
        }

        Long curTime = System.currentTimeMillis();
//        if(lastPushTime != 0)
//            Log.d(TAG, "Push (dt = " + (curTime - lastPushTime) + " ms)");
        lastPushTime = curTime;


        if (mSocket.connected()) {
            mSocket.emit("NewWebsocketData_serverside_timestamp", message);
        }
    }

    //For listening. Currently we do not expect to receive packets, so it doesn't do anything.
    private final Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Runnable listenSocket = new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String testMessage;
                    try {
                        // Replace with whatever we expect to recieve
                        testMessage = data.getString("test");
                    } catch (JSONException e) {
                        return;
                    }
                    // Do something with our testMessage
                }
            };
        }
    };

    //Handler for server connection error
    private final Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mSocket.connect();
            String msg = String.valueOf(args[0]);
            if (args[0] instanceof Throwable) {
                Throwable cause = ((Throwable) args[0]).getCause();
                if (cause != null) msg += " | cause: " + cause;
            }
            Log.e(TAG, "Socket connection had an error (" + msg + ")");
            toastConnectError(msg);
        }
    };

    //Handler for connection event
    private final Emitter.Listener onConnection = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Socket connected!");
            lastErrorToastTime = 0;
            toastToMain("Sickbay connected (" + webSocketURL + ")");

            acquireWifiLock();
        }
    };

    private void releaseWifiLock() {
        if(mWifiLock != null && mWifiLock.isHeld())
            mWifiLock.release();
    }

    private void acquireWifiLock() {
        if(mWifiLock != null && !mWifiLock.isHeld())
            mWifiLock.acquire();
    }

}