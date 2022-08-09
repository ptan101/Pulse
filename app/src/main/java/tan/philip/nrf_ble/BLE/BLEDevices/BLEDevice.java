package tan.philip.nrf_ble.BLE.BLEDevices;

import static tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice.DEBUG_MODE_BT_ID;
import static tan.philip.nrf_ble.Constants.NUS_UUID;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import tan.philip.nrf_ble.BLE.FileWriter;
import tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.BLE.PacketParsing.TattooMessage;

public class BLEDevice {
    protected Context mCtx;

    protected BluetoothDevice mBluetoothDevice;

    protected boolean mConnected;
    protected String displayName;

    protected BLEPacketParser mBLEParser;

    protected ArrayList<UUID> mServiceUUIDs;

    //Saving
    protected String fileName;
    protected boolean mRecording = false;

    public BLEDevice(Context context, BluetoothDevice bluetoothDevice) throws FileNotFoundException {
        this.mCtx = context;
        this.mBluetoothDevice = bluetoothDevice;
        mServiceUUIDs = new ArrayList<>();
        //Temporary solution. Use GattCallback to populate this. TO DO
        mServiceUUIDs.add(NUS_UUID);

        if(bluetoothDevice != null)
            mBLEParser = new BLEPacketParser(mCtx, bluetoothDevice.getName());
        else
            mBLEParser = new BLEPacketParser(mCtx, DEBUG_MODE_BT_ID);
    }

    //Returns the name, or address if name is null
    public String getBluetoothIdentifier () {
        if(mBluetoothDevice.getName() != null)
            return mBluetoothDevice.getName() + " (" + mBluetoothDevice.getAddress() + ")";
        return "Unknown (" + mBluetoothDevice.getAddress() + ")";
    }

    public String getAddress() {
        return mBluetoothDevice.getAddress();
    }

    public TattooMessage getTMSMessage(byte i) {
        return mBLEParser.getRxMessages().get(i);
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public HashMap<Integer, SignalSetting> getSignalSettings() {
        return mBLEParser.getSignalSettings();
    }

    /////////////////////////////////////////Transcieving//////////////////////////////////////////
    public void saveToFile(byte[] data) {
        FileWriter.writeBIN(data, fileName);
    }

    public HashMap<Integer, ArrayList<Integer>> convertPacketToHashMap(byte[] data) {
        //Sometimes a packet arrives before app can disconnect when BLE device not recognized.
        if (mBLEParser == null)
            return null;

        return mBLEParser.parsePacket(data);
    }

    public HashMap<Integer, ArrayList<Integer>> convertPacketForSickbayPush(HashMap<Integer, ArrayList<Integer>> packaged_data) {
        //Sometimes a packet arrives before app can disconnect when BLE device not recognized.
        if (mBLEParser == null)
            return null;

        return mBLEParser.convertToSickbayHashMap(packaged_data);
    }

    public HashMap<Integer, ArrayList<Float>> convertPacketForDisplay(HashMap<Integer, ArrayList<Integer>> packaged_data) {
        //Sometimes a packet arrives before app can disconnect when BLE device not recognized.
        if (mBLEParser == null)
            return null;

        ///Formatting for plotting
        //For each signal, filter in the right way
        HashMap<Integer, ArrayList<Float>> filtered_data = new HashMap<>();
        for (Integer i : packaged_data.keySet())
            filtered_data.put(i, mBLEParser.filterSignals(packaged_data.get(i), i));

        return filtered_data;
    }

    private void startRecord() {
        FileWriter.createFolder(fileName);
        FileWriter.createFolder(fileName);
        //FileWriter.writeBINHeader(mBLEParser.getSignalSettings(), mBLEParser.getSignalOrder(), fileName);
        mRecording = true;
    }

    private void stopRecord() {
        mRecording = false;
    }
}

