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

import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SickbayPushService extends Service {
    private static final String TAG = "SickbayPushService";
    private static final String WEB_SOCKET_URL = "http://chat.socket.io";

    private static final int PUSH_INTERVAL_MS = 250;        //every _ ms the queue will be pushed

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    private Queue<QueueObject> signals = new ArrayDeque<>();
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

    public void addToQueue(ArrayList<ArrayList<Integer>> dataIn) {
        //Convert data into QueueObjects
        for(int i = 0; i < dataIn.size(); i ++) {
            ArrayList<Integer> curChannel = dataIn.get(i);

            for(int j = 0; j < curChannel.size(); j ++) {
                //TO DO: Scale the 4 byte integer data to a 2 byte short

                signals.add(new QueueObject(i, curChannel.get(j)));
            }
        }
    }

    private void pushQueue(long timestamp) {
        //TO DO: Reformat data in queue into string. Currently, just sending timestamp.
        String message = "Hello World! (" + Long.toString(timestamp) + ")";

        //Attempt to send the data
        attemptSend(message);
    }

    private void emptyQueue() {
        signals.clear();
    }

    private class QueueObject {
        //Object that holds information for each element in the queue
        private int channel;    //Which channel the data belongs to
        private int data;       //One sample of data. TO DO: Change to short

        public QueueObject(int channel, int data) {
            this.channel = channel;
            this.data = data;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////Functions for sockets/////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(WEB_SOCKET_URL);
        } catch (URISyntaxException e) {}
    }

    void connectSocket() {
        //Listen for events using onNewMessage (Emitter.Listener)
        mSocket.on("new message", onNewMessage);
        mSocket.connect();
    }

    void disconnectSocket() {
        mSocket.disconnect();
        mSocket.off("new message", onNewMessage);
    }

    private void attemptSend(String message) {
        //If the message is empty, don't send a packet.
        if (TextUtils.isEmpty(message)) {
            return;
        }
        mSocket.emit("new message", message);
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


    //////////////////////////////////Repeated Task (Pushing)//////////////////////////////////////
    Runnable packetPusher = new Runnable() {
        @Override
        public void run() {
            try {
                // Should be 64 bit UTC. One thing to note is that it operates off the phone clock.
                // Therefore, if the phone clock is wrong, the timestamp will also be wrong.
                long timestamp = System.currentTimeMillis();

                // Push queue to Sickbay
                pushQueue(timestamp);

                // Empty queue
                emptyQueue();

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