package tan.philip.nrf_ble.BLE.BLEDevices;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.FileNotFoundException;

import tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.Events.GATTConnectionChangedEvent;
import tan.philip.nrf_ble.Events.NUSPacketRecievedEvent;
import tan.philip.nrf_ble.FileWriting.MarkerFile;
import tan.philip.nrf_ble.FileWriting.TattooFile;

public class DebugBLEDevice extends BLETattooDevice {
    public static final String DEBUG_MODE_ADDRESS = "00:00:00:00:00:00d";
    public static final String DEBUG_MODE_BT_ID = "Debug Mode";

    private float debugModeTime;
    private final Handler debugNotificationHandler;
    private final Handler debugConnectionHandler;
    private long lastRunTime = 0;



    public DebugBLEDevice(Context context, BluetoothDevice bluetoothDevice) throws FileNotFoundException {
        super(context, bluetoothDevice);

        debugNotificationHandler = new Handler();
        debugConnectionHandler = new Handler();
        debugNotifier.run();
        debugConnectionHandler.postDelayed(debugOnConnectionChanged, 5000);
    }

    public void endDebugMode() {
        debugNotificationHandler.removeCallbacks(debugNotifier);
    }

    @Override
    public String getBluetoothIdentifier () {
        return "Debug Mode (" + DEBUG_MODE_ADDRESS + ")";
    }

    @Override
    public String getAddress() {
        return DEBUG_MODE_ADDRESS;
    }

    Runnable debugNotifier = new Runnable() {
        @Override
        public void run() {
            try {
                byte[] data = new byte[mBLEParser.getPackageSizeBytes()];
                int[] numSamples = new int[mBLEParser.getSignalSettings().size()];
                int bytesWritten = 0;
                //Process fake data
                for (int i: mBLEParser.getSignalOrder()) {
                    int new_data;
                    SignalSetting curSignal = mBLEParser.getSignalSettings().get(i);
                    float t = debugModeTime + (float)numSamples[i] / curSignal.fs; //Time in seconds
                    switch(curSignal.name) {
                        case "Sine":
                            new_data = (int) ((1 << (curSignal.bitResolution - 1) - 1) * Math.sin(Math.PI * 2 * t));
                            break;
                        case "Square":
                            new_data = (int) ((1 << (curSignal.bitResolution - 1) - 1) * Math.signum(Math.sin(Math.PI * 2 * t)));
                            break;
                        case "Sawtooth":
                            new_data = (int) ((1 << (curSignal.bitResolution - 1) - 1) * t) % (1 << (curSignal.bitResolution - 1) - 1);
                            break;
                        default:
                            new_data = 0;
                            break;
                    }
                    for(int j = 0; j < curSignal.bytesPerPoint; j++) {
                        data[bytesWritten] = (byte) ((new_data >> (j*8)) & 0xFF);
                        bytesWritten ++;
                    }
                    numSamples[i] ++;
                }
                debugModeTime += 1/mBLEParser.getNotificationFrequency();

                long curTime = System.currentTimeMillis();
//                if(lastRunTime != 0)
//                    Log.d("Debug", "Run (dt = " + (curTime - lastRunTime) + " ms)");
                lastRunTime = curTime;


                processNUSPacket(data);
            } finally {
                debugNotificationHandler.postDelayed(debugNotifier, (long) (1000 / mBLEParser.getNotificationFrequency()));
            }
        }
    };


    Runnable debugOnConnectionChanged = new Runnable() {
        @Override
        public void run() {
            EventBus.getDefault().post(new GATTConnectionChangedEvent(DEBUG_MODE_ADDRESS, BluetoothProfile.STATE_CONNECTED));
            debugConnectionHandler.removeCallbacks(this);
        }
    };
}
