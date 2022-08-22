package tan.philip.nrf_ble.BLE.BLEDevices;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import org.greenrobot.eventbus.EventBus;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.Events.PlotDataEvent;
import tan.philip.nrf_ble.Events.Sickbay.SickbayQueueEvent;
import tan.philip.nrf_ble.FileWriting.MarkerFile;
import tan.philip.nrf_ble.FileWriting.TattooFile;
import tan.philip.nrf_ble.GraphScreen.GraphSignal;

public class BLETattooDevice extends BLEDevice {
    private final ArrayList<GraphSignal> graphSignals;

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
        }

        HashMap<Integer, ArrayList<Integer>> packaged_data = convertPacketToHashMap(messageBytes);

        //Send data to SickbayPushService to be sent over web sockets
        //TO DO: Send the filtered data to Sickbay
        if (pushToSickbay) {
            //Convert to HashMap. Keys are the Sickbay IDs.
            HashMap<Integer, ArrayList<Integer>> sickbayPush = convertPacketForSickbayPush(packaged_data);

            EventBus.getDefault().post(new SickbayQueueEvent(sickbayPush, this.uniqueID));
        }

        //Send the data to the UI for display
        EventBus.getDefault().post(new PlotDataEvent(getAddress(), convertPacketForDisplay(packaged_data)));

    }
}
