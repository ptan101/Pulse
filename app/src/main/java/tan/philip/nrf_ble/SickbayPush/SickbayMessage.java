package tan.philip.nrf_ble.SickbayPush;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;

public class SickbayMessage {
    private static final String TAG = "SickbayMessage";

    public static JSONObject convertPacketToJSONString(long timestamp,
                                                           HashMap<Integer, ArrayList<Float>> data,
                                                           BLEDevice device,
                                                           String bedName) {

        JSONObject obj_parent = new JSONObject();
        JSONObject obj = new JSONObject();
        try {
            String dataString = "{";
            ArrayList<String> signalStrings = new ArrayList<>();
            boolean signalsNeedPushing = false;
            //Iterate through all signals in the Hashmap
            for (int i : data.keySet()) {
                //Make a local copy of the signals to push. This way, we only remove items that we
                //pushed. In case new items are enqueued now.
                ArrayList<Float> localCopyOfSignal = new ArrayList<>(data.get(i));
                String curSignalString = "\"" + i + "\": " + "[";

                //Question for Dr. Rusin: If there is no data to push from that particular signal,
                //is it ok to send an empty array? E.g., "s1": [],"s2": [1, 2, 3, 4]. Or should it be omitted? -> CGR ANSWER It should be omitted
                //Add all samples for that particular signal
                curSignalString += localCopyOfSignal.stream().map(Object::toString).collect(Collectors.joining(", "));
                curSignalString += "]";
                signalStrings.add(curSignalString);

                //Check if any signals actually need pushing
                signalsNeedPushing = signalsNeedPushing || (localCopyOfSignal.size() > 0);
            }
            dataString += String.join(", ", signalStrings);
            dataString += "}";

            if (!signalsNeedPushing)
                return null;

            float dt = 1000 / device.getNotificationFrequency();
            //Log.d(TAG, "dt: " + dt + ", num packets: " + numPacketsInQueue);
            obj.put("channel", bedName);
            obj.put("ns", device.getSickbayNS());
            obj.put("t0", ((double) timestamp));
            obj.put("dt", dt/1000);
            obj.put("VIZ", new Integer(0));
            obj.put("Z", new Integer(0));
            obj.put("InstanceID", new Integer(0));

            JSONObject jObject = new JSONObject(dataString);
            obj.put("signals", jObject);

            obj_parent.put("data", obj);

        } catch (JSONException e) {
            Log.e(TAG, "Unable to put data into JSONObject (" + e.getMessage() + ")");
        }

        return obj_parent;
    }
}
