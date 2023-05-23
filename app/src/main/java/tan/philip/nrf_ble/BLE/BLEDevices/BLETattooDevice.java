package tan.philip.nrf_ble.BLE.BLEDevices;

import static androidx.constraintlayout.widget.Constraints.TAG;
import static tan.philip.nrf_ble.Constants.bytesToHex;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.Events.PlotDataEvent;
import tan.philip.nrf_ble.Events.Sickbay.SickbaySendFloatsEvent;
import tan.philip.nrf_ble.Events.TMSMessageReceivedEvent;
import tan.philip.nrf_ble.GraphScreen.GraphSignal;

public class BLETattooDevice extends BLEDevice {
    private final ArrayList<GraphSignal> graphSignals;
    private long timestamps[] = new long[1000];
    private int n = 0;

    public BLETattooDevice(Context context, BluetoothDevice bluetoothDevice) throws FileNotFoundException {
        super(context, bluetoothDevice);

        //Add graphable signals for all BLETattooDevices
        graphSignals = new ArrayList<>();

        for (Integer i : mBLEParser.getSignalSettings().keySet()) {
            SignalSetting signal = mBLEParser.getSignalSettings().get(i);
            if(signal.graphable)
                graphSignals.add(new GraphSignal(signal));
        }
    }

    public void processNUSPacket(byte[] messageBytes) {
        if(mRecording) {
            saveToFile(messageBytes);
            recordTime += (1.0f / mBLEParser.getNotificationFrequency());

            //Save timestamps of each packet
            timestamps[n] = System.currentTimeMillis();
            n++;
            if (n == 1000) {
                n = 0;
                dumpTimestamps();
            }
        }

        HashMap<Integer, ArrayList<Integer>> packaged_data = convertPacketToHashMap(messageBytes);


        HashMap<Integer, ArrayList<Float>> filtered_data = convertPacketForDisplay(packaged_data);
        //Send data to SickbayPushService to be sent over web sockets
        //TO DO: Send the filtered data to Sickbay
        if (pushToSickbay) {
            //Convert to HashMap. Keys are the Sickbay IDs.
            //HashMap<Integer, ArrayList<Integer>> sickbayPush = convertIntegerPacketForSickbayPush(packaged_data);
            HashMap<Integer, ArrayList<Float>> sickbayPush = convertPacketForSickbayPush(filtered_data);

            EventBus.getDefault().post(new SickbaySendFloatsEvent(sickbayPush, this));
        }

        //Send the data to the UI for display
        EventBus.getDefault().post(new PlotDataEvent(getAddress(), filtered_data));
    }

    public void processCUSPacket(byte[] messageBytes) {
        //Save the message as an event marker
        if(mRecording) {
            markEvent("TMS message received: " + bytesToHex(messageBytes));
            recordTime += (1.0f / mBLEParser.getNotificationFrequency());
        }

        //Send the message to the UI for display
        EventBus.getDefault().post(new TMSMessageReceivedEvent(this.getAddress(), this.getTMSMessage(messageBytes[0])));
    }

    private static final String BASE_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Pulse_Data";
    private void dumpTimestamps() {
        String filePath = BASE_DIR_PATH + File.separator + "timestamps.bin";

        FileOutputStream fileOutputStream = null;

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Log.e(TAG, "Error creating .bin file.");
                    //Toast.makeText(getApplicationContext(), "Error creating file", Toast.LENGTH_SHORT).show();
                }
            }
            fileOutputStream = new FileOutputStream(file, false);

            ByteBuffer bb = ByteBuffer.allocate(timestamps.length * Long.BYTES);
            bb.asLongBuffer().put(timestamps);
            fileOutputStream.write(bb.array());

        } catch (FileNotFoundException ex) {
            Log.e(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
