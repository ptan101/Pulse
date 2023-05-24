package tan.philip.nrf_ble.GraphScreen.UIComponents;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

public class ValueAlert {
    private static String TAG = "ValueALert";

    public float threshold = 15;
    public boolean aboveAlert = true;
    public String message = "Default alert message";
    public String title = "Default alert title";

    private Context context;

    private AlertDialog alertDialog;
    private boolean thresholdReset = true; //If the value went below threshold

    //TO DO:
    //Moving average
    //

    public ValueAlert() {

    }

    public void initialize(Context context, AlertDialog alertDialog) {
        this.context = context;
        this.alertDialog = alertDialog;
    }

    public boolean initialized() {
        return context != null;
    }

    public void checkValue(float value) {
        if(!this.initialized()) {
            Log.e(TAG, "ValueAlert not initialized, will not check value.");
            return;
        }

        if(aboveAlert && value > threshold) {
            throwAlert();
        } else if (!aboveAlert && value < threshold) {
            throwAlert();
        } else {
            thresholdReset = true;
        }
    }

    private void throwAlert() {
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);

        if(!alertDialog.isShowing() && thresholdReset) {
            alertDialog.show();
            thresholdReset = false;
        }
    }
 }
