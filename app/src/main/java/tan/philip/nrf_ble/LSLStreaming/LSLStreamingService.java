package tan.philip.nrf_ble.LSLStreaming;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.ucsd.sccn.LSL;
import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.Events.LSLDataEvent;
import tan.philip.nrf_ble.GraphScreen.GraphActivity;
import tan.philip.nrf_ble.R;

import static tan.philip.nrf_ble.NotificationHandler.CHANNEL_ID;

public class LSLStreamingService extends Service {

    private static final String TAG = "LSLStreamingService";
    private static final int FOREGROUND_NOTIFICATION_ID = 3;

    // Key: "deviceAddress_signalIndex" → outlet/info pair
    private final HashMap<String, LSL.StreamOutlet> outlets = new HashMap<>();
    private final HashMap<String, LSL.StreamInfo> infos = new HashMap<>();

    private WifiManager.MulticastLock multicastLock;
    private ConnectivityManager connectivityManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        startForegroundWithNotification();
        bindProcessToWifi();
        acquireMulticastLock();
        EventBus.getDefault().register(this);
        Log.d(TAG, "LSLStreamingService started.");
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        closeAllOutlets();
        releaseMulticastLock();
        unbindProcessFromWifi();
        Log.d(TAG, "LSLStreamingService stopped.");
    }

    @Subscribe
    public void onLSLData(LSLDataEvent event) {
        BLEDevice device = event.getDevice();
        HashMap<Integer, ArrayList<Float>> data = event.getFilteredData();
        HashMap<Integer, SignalSetting> signalSettings = device.getSignalSettings();
        float notifHz = device.getNotificationFrequency();

        for (int signalIndex : data.keySet()) {
            ArrayList<Float> samples = data.get(signalIndex);
            if (samples == null || samples.isEmpty()) continue;

            SignalSetting setting = signalSettings.get(signalIndex);
            if (setting == null) continue;

            int N = samples.size();
            LSL.StreamOutlet outlet = getOrCreateOutlet(device, signalIndex, setting, N, notifHz);
            if (outlet == null) continue;

            // Reconstruct per-sample timestamps by spreading backwards from now.
            // All N samples in one BLE packet span exactly 1/notifHz seconds, so the
            // inter-sample interval is 1/(notifHz * N).  Without this, every sample in
            // a 20-sample packet would get the same push timestamp.
            double now = LSL.local_clock();
            double interSampleSec = 1.0 / (notifHz * N);
            float[] buf = new float[1];
            for (int k = 0; k < N; k++) {
                double ts = now - (N - 1 - k) * interSampleSec;
                buf[0] = samples.get(k);
                try {
                    outlet.push_sample(buf, ts);
                } catch (Exception e) {
                    Log.w(TAG, "push_sample failed for signal " + signalIndex + ": " + e.getMessage());
                }
            }
        }
    }

    private LSL.StreamOutlet getOrCreateOutlet(BLEDevice device, int signalIndex,
                                                SignalSetting setting, int batchSize, float notifHz) {
        String key = device.getAddress() + "_" + signalIndex;
        if (outlets.containsKey(key)) return outlets.get(key);

        try {
            // Actual rate = notification rate × samples per packet.
            // This is more accurate than the integer rate in the init file
            // (e.g. 6.25 × 20 = 125.0, 6.25 × 5 = 31.25, 6.25 × 1 = 6.25).
            double actualRate = notifHz * batchSize;
            String streamName = device.getDisplayName() + "_" + setting.name;
            LSL.StreamInfo info = new LSL.StreamInfo(
                    streamName,
                    setting.name,
                    1,
                    actualRate,
                    LSL.ChannelFormat.float32,
                    device.getAddress() + "_" + signalIndex
            );
            LSL.StreamOutlet outlet = new LSL.StreamOutlet(info);
            infos.put(key, info);
            outlets.put(key, outlet);
            Log.d(TAG, "Created LSL outlet: " + streamName + " @ " + actualRate + " Hz");
            return outlet;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create LSL outlet for " + key + ": " + e.getMessage());
            return null;
        }
    }

    private void closeAllOutlets() {
        for (LSL.StreamOutlet o : outlets.values()) o.close();
        for (LSL.StreamInfo i : infos.values()) i.destroy();
        outlets.clear();
        infos.clear();
    }

    // Forces all sockets created in this process (including liblsl.so via JNA) to use the
    // WiFi interface. Without this, Boost.Asio inside liblsl picks the USB-ADB virtual
    // interface when the phone is connected via USB cable, making outlets unreachable over WiFi.
    private void bindProcessToWifi() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;
        for (Network network : connectivityManager.getAllNetworks()) {
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                boolean bound = connectivityManager.bindProcessToNetwork(network);
                Log.d(TAG, "Bound process to WiFi network: " + bound);
                return;
            }
        }
        Log.w(TAG, "No WiFi network found — LSL outlets may not be discoverable");
    }

    private void unbindProcessFromWifi() {
        if (connectivityManager != null) {
            connectivityManager.bindProcessToNetwork(null);
        }
    }

    private void acquireMulticastLock() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            multicastLock = wifiManager.createMulticastLock("LSL_MULTICAST");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        }
    }

    private void releaseMulticastLock() {
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
    }

    private void startForegroundWithNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, GraphActivity.class),
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LSL Streaming")
                .setContentText("Streaming sensor data over LSL")
                .setSmallIcon(R.drawable.heartrate)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        }
    }
}
