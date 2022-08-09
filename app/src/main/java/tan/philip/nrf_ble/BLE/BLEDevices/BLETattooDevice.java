package tan.philip.nrf_ble.BLE.BLEDevices;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.GraphSignal;

public class BLETattooDevice extends BLEDevice {
    private ArrayList<GraphSignal> graphSignals;

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
}

