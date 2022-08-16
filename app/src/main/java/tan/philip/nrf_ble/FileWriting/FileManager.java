package tan.philip.nrf_ble.FileWriting;

import android.os.Handler;
import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.Events.UIRequests.RequestChangeRecordEvent;

public class FileManager {
    private final ArrayList<BLEDevice> devices;
    private boolean recording = false;
    private long startRecordTime;

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
        if(event.startRecord())
            startRecord(event.getFilename());
        else
            stopRecord();
    }

    private void startRecord(String filename) {
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
}
