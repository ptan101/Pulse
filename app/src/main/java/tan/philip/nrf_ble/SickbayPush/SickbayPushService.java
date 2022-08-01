package tan.philip.nrf_ble.SickbayPush;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;

public class SickbayPushService extends Service {
    private static final String TAG = "SickbayPushService";
    //private static final String WEB_SOCKET_URL = "http://192.168.1.97";
    private static final int QUEUE_MAX_SIZE = 10000;
    private static final int PUSH_INTERVAL_MS = 250;        //every _ ms the queue will be pushed

    private String bedName = "";


    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    //BlockingQueue should be thread safe.
    //private ConcurrentLinkedQueue<QueueObject> signals = new ConcurrentLinkedQueue<>();
    private Map<Integer, ArrayList<Integer>> signals = new ConcurrentHashMap<>();
    private Handler mHandler;

    //If we need to deal with IPC, we will need to use a messenger
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
        mHandler = new Handler();
        connectSocket();
        startPushingPackets();
    }

    @Override
    public void onDestroy() {
        disconnectSocket();
        stopPushingPackets();
    }

    ////////////////////////////////////Queue Functions////////////////////////////////////////////

    public synchronized void addToQueue(HashMap<Integer, ArrayList<Integer>> dataIn) {
        //Add data to queue
        for (int i : dataIn.keySet()) {
            signals.get(i).addAll(dataIn.get(i));
        }
    }

    private void pushQueue(long timestamp) {
        //Consolidate queue to a single frame and push the frame

        //To ensure that we remove only the number of elements that we push.
        int elementsToPush = signals.size();
        synchronized (this) {
            // Make a local copy of the queue

            //
        }

        //Reformat data in queue into string.
        JSONObject message = convertToJSONString("TATTOOWAVE", timestamp, (float) PUSH_INTERVAL_MS);
        //Log.d(TAG, message);

        //Attempt to send the data
        attemptSend(message);

        //Remove elements that we pushed
        synchronized (this) {

        }


    }

    private JSONObject convertToJSONString(String namespace, long timestamp, float dt) {
        /*
        This is called a data frame
        {
        "CH": "BED012", //Bed name (need to put in manually in the mobile app)
        "NS": "GEVITAL", //Name space (Every signal needs a unique ID, given by Craig, ETATTOOVITAL for num, ETATTOOWAVE for waveforms)
        "T": 1657310006119.1106, //Time\

        tamp (int64_t UTC 100ns intervals) of the first element entering the queue. Javascript handles time in 1000ms. Use the Javascript time instead of android java time. 64 bit int divided by 10000. Units ms
        "DT": 2, // Dt of the data frame, num samples * period (seconds, float). E.g., 0.250. Better to do num_samples * time interval per sample.
        "VIZ": 0, // Always 0
        "Z": 0, //Always 0
        "InstanceID": 0, //Probably going to be 0. But may change with multiple tattoos.
        "DATA": [
            {
                "s1": [
                    162, 100
                ],
                "s99": [
                    72
                ],
                "s100": [
                    76
                ],
                "s101": [
                    162
                ],
                "s112": [
                    -1.3
                ]
            }
        ]

    }
         */

        JSONObject obj = new JSONObject();
        try {
            obj.put("CH", "BED012");
            obj.put("NS", namespace);
            obj.put("T", ((double) timestamp) / 10000);
            obj.put("DT", dt);
            obj.put("VIZ", new Integer(0));
            obj.put("Z", new Integer(0));
            obj.put("InstanceID", new Integer(0));
            obj.put("Data", "[{\"s1\": [162],\"s99\": [72]}]");
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Unable to put data into JSONObject (" + e.getMessage() + ")");
        }

        return obj;
    }

    private String convertSignalToString() {
        String out = "";
        return out;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////Functions for sockets/////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private Socket mSocket;
    {
        try {
            /*
            IO.Options options = IO.Options.builder()
                // IO factory options
                .setForceNew(false)
                .setMultiplex(true)

                // low-level engine options
                .setTransports(new String[] { Polling.NAME, WebSocket.NAME })
                .setUpgrade(true)
                .setRememberUpgrade(false)
                .setPath("/socket.io/")
                .setQuery(null)
                .setExtraHeaders(null)

                // Manager options
                .setReconnection(true)
                .setReconnectionAttempts(Integer.MAX_VALUE)
                .setReconnectionDelay(1_000)
                .setReconnectionDelayMax(5_000)
                .setRandomizationFactor(0.5)
                .setTimeout(20_000)

                // Socket options
                .setAuth(null)
                .build();

             */

            mSocket = IO.socket("http://192.168.1.97:3000");
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

        if (mSocket.connected()) {
            mSocket.emit("NewData", message);
        }
    }

    //For listening. Currently we do not expect to receive packets, so it doesn't do anything.
    private Emitter.Listener onNewMessage = new Emitter.Listener() {
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
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mSocket.connect();
            Log.e(TAG, "Socket connection had an error (" + args[0] +")");
        }
    };

    //Handler for connection event
    private Emitter.Listener onConnection = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Socket connected!");
        }
    };


    //////////////////////////////////Repeated Task (Pushing)//////////////////////////////////////
    Runnable packetPusher = new Runnable() {
        @Override
        public void run() {
            try {
                //Log.d(TAG, "Socket connected: " + mSocket.connected());
                // Should be 64 bit UTC. One thing to note is that it operates off the phone clock.
                // Therefore, if the phone clock is wrong, the timestamp will also be wrong.
                long timestamp = System.currentTimeMillis();

                // Push queue to Sickbay
                pushQueue(timestamp);

                //Log.d(TAG, "Queue pushed!");
            } finally {
                mHandler.postDelayed(packetPusher, PUSH_INTERVAL_MS);
            }
        }
    };

    void startPushingPackets() {
        packetPusher.run();
    }

    void stopPushingPackets() {
        mHandler.removeCallbacks(packetPusher);
    }
}

//Build infrastructure, not up to JSON formatting
//Create a string called CRF frame, make it empty.
//Dr. Rusin will do the conversion.

//private connectionToWebServer() { }
//Connection to the web socket
//https://github.com/socketio/socket.io-client-java

//Every 250 ms, send the queue
//Reformat the data in the queue to be a single JSON string
//Push the data over the web socket connection

//1 Timestamp (64 bit int UTC)
//2 List of signals and signal IDs that they represent
//  array of short ints where each element represents a single signal
//  array of bytes/samples included in that data segment
//
//Recognize if there is an error and reestablish connection

// TO DO: option to set URL
// TO DO: option to disable SickbayPush