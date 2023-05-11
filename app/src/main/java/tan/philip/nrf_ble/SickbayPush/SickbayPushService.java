package tan.philip.nrf_ble.SickbayPush;

import static tan.philip.nrf_ble.SickbayPush.SickbayMessage.convertPacketToJSONString;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.Events.Sickbay.SickbayQueueEvent;
import tan.philip.nrf_ble.Events.Sickbay.SickbaySendFloatsEvent;

public class SickbayPushService extends Service {
    private static final String TAG = "SickbayPushService";

    private static final String DEFAULT_WEB_SOCKET_URL = "http://192.168.50.147:3001";
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
        readSickbaySettings(); //TO DO: Check if the IP address and Bed names are valid

        initializeSocket();

        mHandler = new Handler();
        connectSocket();

        //Register on EventBus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        disconnectSocket();

        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
    }

    //Reads the sickbay settings from local memory
    private static final String BASE_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Pulse_Data";
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
            Log.d(TAG, "Sickbay IP set to:" + sickbayIP);

            webSocketURL = "https://" + sickbayIP + ":3001";

            fileReader.close();
        }
        catch (Exception e) {
            Log.e(TAG, "exception", e);
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
            Log.d(TAG, "Sickbay Bed ID set to:" + bedID);

            bedName = bedID;

            fileReader.close();
        }
        catch (Exception e) {
            Log.e(TAG, "exception", e);
        }
    }

    // ////////////////////////////////Queue Functions////////////////////////////////////////////

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
            SocketSSL.set(options);
            mSocket = IO.socket(webSocketURL, options);
            Log.d(TAG, "Socket object created.");
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
            mSocket.emit("NewDataVICU", message);
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
            Log.e(TAG, "Socket connection had an error (" + args[0] +")");
        }
    };

    //Handler for connection event
    private final Emitter.Listener onConnection = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Socket connected!");
        }
    };

}

