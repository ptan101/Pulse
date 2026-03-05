package tan.philip.nrf_ble.BLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

import androidx.core.app.NotificationManagerCompat;

public class AlertStopReceiver extends BroadcastReceiver {
    public static final int NOTIF_ID_ALERT = 4242; // keep in sync with service

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Vibrator vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (vib != null) vib.cancel();
        NotificationManagerCompat.from(ctx).cancel(NOTIF_ID_ALERT);
    }
}
