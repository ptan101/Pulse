package tan.philip.nrf_ble.FileWriting;

import android.os.Handler;
import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static tan.philip.nrf_ble.FileWriting.PulseFile.BASE_DIR_PATH;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.Events.UIRequests.RequestChangeRecordEvent;
import tan.philip.nrf_ble.Events.Device.DeviceConnectionChangedEvent;

public class FileManager {
    private static final String TAG = "FileManager";
    private final ArrayList<BLEDevice> devices;
    private boolean recording = false;
    private long startRecordTime;
    private String currentSession;

    private Handler syncHandler;
    private static final int SYNC_RATE_MIN = 1; //Once every 10 minutes
    private static final int SYNC_RATE_MS = 60000 * SYNC_RATE_MIN;

    public FileManager(ArrayList<BLEDevice> devices) {
        this.devices = devices;
        setupSynchronizer();

        //Register activity on EventBus
        EventBus.getDefault().register(this);

    }

    public void deregister() {
        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
    }

    public void markEvent(String label) {
        for(BLEDevice d : devices)
            d.markEvent(label);
    }

    private void setupSynchronizer() {
        syncHandler = new Handler();
        syncHandler.postDelayed(syncLoop, SYNC_RATE_MS);
    }

    private final Runnable syncLoop = new Runnable() {
        @Override
        public void run() {
            long curTime = System.currentTimeMillis();
            markEvent("Sync time: " + (curTime - startRecordTime) + " ms");

            syncHandler.postDelayed(this, SYNC_RATE_MS);
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeRecordState(RequestChangeRecordEvent event) {
        if(event.startRecord()) {
            writeToggleTime(event.getFilename(), true);
            startRecord(event.getFilename());
        }
        else {
            writeToggleTime(currentSession, false);
            stopRecord();
        }
    }

    private static String nowLocal24() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getDefault()); // device’s local TZ (handles DST, etc.)
        return sdf.format(new java.util.Date());
    }

    private void startRecord(String filename) {
        currentSession = filename;
        startRecordTime = System.currentTimeMillis();
        recording = true;

        for(BLEDevice d : devices)
            d.startRecord(filename);
    }

    private void stopRecord() {
        recording = false;
        for(BLEDevice d : devices)
            d.stopRecord();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnectionChanged(DeviceConnectionChangedEvent event) {
        // Only log during an active recording session
        if (!recording || currentSession == null) return;

        if (!event.isConnected()) {
            writeDisconnectTime(currentSession, event.getDisplayName(), event.getAddress());
        }
    }

    private void writeDisconnectTime(String sessionName, String displayName, String address) {
        File sessionDir = new File(BASE_DIR_PATH, sessionName);
        if (!sessionDir.exists() && !sessionDir.mkdirs()) {
            Log.e(TAG, "Unable to create session dir: " + sessionDir);
            return;
        }

        File outFile = new File(sessionDir, "record_times.txt");

        String ts = nowLocal24(); // <-- 24h, local

        String line = "DISCONNECTED: " + ts + " | " + displayName + " (" + address + ")";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile, true))) {
            bw.write(line);
            bw.newLine();
            Log.d(TAG, "Wrote disconnect line: " + line);
        } catch (IOException e) {
            Log.e(TAG, "Error writing disconnect time", e);
        }
    }

    private void writeToggleTime(String sessionName, boolean isStart) {
        File sessionDir = new File(BASE_DIR_PATH, sessionName);
        if (!sessionDir.exists() && !sessionDir.mkdirs()) {
            Log.e(TAG, "Unable to create session dir: " + sessionDir);
            return;
        }

        File outFile = new File(sessionDir, "record_times.txt");

        String ts = nowLocal24(); // <-- 24h, local
        String line = (isStart ? "START: " : "STOP:  ") + ts;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile, true))) {
            bw.write(line);
            bw.newLine();
            Log.d(TAG, "Wrote toggle line: " + line);
        } catch (IOException e) {
            Log.e(TAG, "Error writing toggle time", e);
        }
    }

}
