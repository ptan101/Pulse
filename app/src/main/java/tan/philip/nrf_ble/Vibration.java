package tan.philip.nrf_ble;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import static android.content.Context.VIBRATOR_SERVICE;

public class Vibration {
    public static void vibrate(Activity activity, int duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) activity.getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(duration, -1));
        } else {
            //deprecated in API 26
            ((Vibrator) activity.getSystemService(VIBRATOR_SERVICE)).vibrate(duration);
        }
    }

}