package tan.philip.nrf_ble.GraphScreen.UIComponents;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import tan.philip.nrf_ble.Events.Rendering.ThrowValueAlertEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestSendTMSEvent;

public class ValueAlert {
    private static String TAG = "ValueALert";

    public float threshold = 15;
    public boolean aboveAlert = true;
    public String message = "Default alert message";
    public String title = "Default alert title";
    private int samplesToAverage = 1;
    private boolean thresholdReset = true; //If the value went below threshold

    private float[] averagedValues = new float[1];
    private int numSamplesAveraged = 0;

    //TO DO:
    //Moving average
    //

    public ValueAlert() {

    }

    public void checkValue(float value) {
        averagedValues[numSamplesAveraged % samplesToAverage] = value;
        numSamplesAveraged ++;

        if(numSamplesAveraged < samplesToAverage)
            return;

        float averagedValue = 0;

        for(float x : averagedValues) {
            averagedValue += x;
        }

        averagedValue /= samplesToAverage;

        if(aboveAlert && averagedValue > threshold) {
            throwAlert();
        } else if (!aboveAlert && averagedValue < threshold) {
            throwAlert();
        } else {
            thresholdReset = true;
        }
    }

    private void throwAlert() {
        if(thresholdReset) {
            EventBus.getDefault().post(new ThrowValueAlertEvent(title, message));
            thresholdReset = false;
        }

    }

    public void setSamplesToAverage(int num) {
        samplesToAverage = num;
        averagedValues = new float[num];
    }
 }
