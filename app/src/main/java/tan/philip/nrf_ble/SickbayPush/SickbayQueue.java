package tan.philip.nrf_ble.SickbayPush;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SickbayQueue {
    private static final String TAG = "SickbayQueue";

    private String bedName;
    private String namespace;
    private int instanceID;
    private final Map<Integer, ArrayList<Integer>> dataQueue;
    private final float notificationFrequency;
    private int numPacketsInQueue = 0;

    public SickbayQueue(String bedName, String namespace, int instanceID, float notificationFrequency) {
        dataQueue = new ConcurrentHashMap<>();
        this.bedName = bedName;
        this.namespace = namespace;
        this.instanceID = instanceID;
        this.notificationFrequency = notificationFrequency;
    }

    public synchronized void addToQueue(HashMap<Integer, ArrayList<Integer>> dataIn) {
        //Add data to queue
        for (int i : dataIn.keySet()) {
            if (dataQueue.get(i) == null)
                dataQueue.put(i, dataIn.get(i));
            else
                dataQueue.get(i).addAll(dataIn.get(i));
        }

        numPacketsInQueue ++;
    }

    /**
     * Converts the queue into a single data frame JSON String.
     * @return The JSON string. If no data is available to send, returns null.
     */
    public synchronized JSONObject convertQueueToJSONString(long timestamp) {

        JSONObject obj_parent = new JSONObject();
        JSONObject obj = new JSONObject();
        try {
            String dataString = "{";
            ArrayList<String> signalStrings = new ArrayList<>();
            boolean signalsNeedPushing = false;
            //Iterate through all signals in the Hashmap
            for (int i : dataQueue.keySet()) {
                //Make a local copy of the signals to push. This way, we only remove items that we
                //pushed. In case new items are enqueued now.
                ArrayList<Integer> localCopyOfSignal = new ArrayList<>(dataQueue.get(i));
                String curSignalString = "\"" + i + "\": " + "[";

                //Question for Dr. Rusin: If there is no data to push from that particular signal,
                //is it ok to send an empty array? E.g., "s1": [],"s2": [1, 2, 3, 4]. Or should it be omitted? -> CGR ANSWER It should be omitted
                //Add all samples for that particular signal
                curSignalString += localCopyOfSignal.stream().map(Object::toString).collect(Collectors.joining(", "));
                curSignalString += "]";
                signalStrings.add(curSignalString);

                //Check if any signals actually need pushing
                signalsNeedPushing = signalsNeedPushing || (localCopyOfSignal.size() > 0);

                //Remove the samples that we pushed
                dataQueue.get(i).subList(0, localCopyOfSignal.size()).clear();
            }
            dataString += String.join(", ", signalStrings);
            dataString += "}";

            if (!signalsNeedPushing)
                return null;

            float dt = 1000 * numPacketsInQueue / notificationFrequency;
            //Log.d(TAG, "dt: " + dt + ", num packets: " + numPacketsInQueue);
            numPacketsInQueue = 0;  //I think this is thread safe, if not change this

            obj.put("channel", bedName);
            obj.put("ns", namespace);
            obj.put("t0", ((double) timestamp));
            obj.put("dt", dt/1000);
            obj.put("VIZ", new Integer(0));
            obj.put("Z", new Integer(0));
            obj.put("InstanceID", instanceID);

            JSONObject jObject = new JSONObject(dataString);
            obj.put("signals", jObject);

            obj_parent.put("data", obj);

        } catch (JSONException e) {
            Log.e(TAG, "Unable to put data into JSONObject (" + e.getMessage() + ")");
        }

        return obj_parent;
    }

    public Map<Integer, ArrayList<Integer>> getQueue() {
        return dataQueue;
    }

    public String getNamespace() {
        return namespace;
    }

    public int getInstanceID() {
        return instanceID;
    }
}
