package tan.philip.nrf_ble.SickbayPush;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Queue;

public class SickbayPushService extends Service {
    private Queue signals;

    public SickbayPushService() {
    }

    //onCreate
    //Open a web socket connection to Sickbay web service at specific URL (persistent)
    //Recognize if there is an error and reestablish connection


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void addMessageToQueue() {

    }

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

    private class QueueObject {

    }
}
//Build infrastructure, not up to JSON formatting
//Create a string called CRF frame, make it empty.
//Dr. Rusin will do the conversion.


