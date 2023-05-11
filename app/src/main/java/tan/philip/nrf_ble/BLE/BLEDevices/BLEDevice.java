package tan.philip.nrf_ble.BLE.BLEDevices;

import static tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice.DEBUG_MODE_BT_ID;
import static tan.philip.nrf_ble.Constants.NUS_UUID;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.BLE.PacketParsing.TattooMessage;
import tan.philip.nrf_ble.FileWriting.MarkerFile;
import tan.philip.nrf_ble.FileWriting.TattooFile;

public class BLEDevice {
    protected Context mCtx;

    protected BluetoothDevice mBluetoothDevice;
    protected int rssi;

    protected int instanceId = 0; //If there are multiple of the same device, this is used for preventing file saving conflicts.
    protected int uniqueID; //A new unique ID is generated for every device, regardless if they are unique devices.
    protected String displayName;

    protected BLEPacketParser mBLEParser;

    protected ArrayList<UUID> mServiceUUIDs;

    protected boolean pushToSickbay = true;


    protected float recordTime;
    protected long disconnectTime;

    //Saving
    protected boolean mRecording = false;
    protected TattooFile mFile;
    // Although the event markers will be the same for each device, the time relative to the actual data points
    // will not. Can be used for synchronization. Therefore, need separate marker files per device.
    protected MarkerFile markerFile;
    protected int menuID;
    protected boolean mConnected;

    public BLEDevice(Context context, BluetoothDevice bluetoothDevice) throws FileNotFoundException {
        this.mCtx = context;
        this.mBluetoothDevice = bluetoothDevice;

        mServiceUUIDs = new ArrayList<>();
        //Temporary solution. Use GattCallback to populate this. TO DO
        mServiceUUIDs.add(NUS_UUID);

        if(bluetoothDevice != null)
            displayName = bluetoothDevice.getName();
        else
            displayName = DEBUG_MODE_BT_ID;
        if (displayName == null)
            displayName = "Unknown";

        mBLEParser = new BLEPacketParser(mCtx, displayName);

        //Register on EventBus
        //EventBus.getDefault().register(this);
    }

    public void unregister() {
        //Unregister from EventBus
        //EventBus.getDefault().unregister(this);
    }

    public int getInstanceId() {
        return instanceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setInstanceId(int id) {
        this.instanceId = id;
    }

    //Returns the name, or address if name is null
    public String getBluetoothIdentifier () {
        return displayName + " (" + mBluetoothDevice.getAddress() + ")";
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
    public HashMap<Integer, ArrayList<Integer>> convertPacketToHashMap(byte[] data) {
        //Sometimes a packet arrives before app can disconnect when BLE device not recognized.
        if (mBLEParser == null)
            return null;

        return mBLEParser.parsePacket(data);
    }

    public HashMap<Integer, ArrayList<Float>> convertPacketForSickbayPush(HashMap<Integer, ArrayList<Float>> packaged_data) {
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

    public void saveToFile(byte[] data) {
        if(mRecording)
            mFile.queueWrite(data);
    }

    public void markEvent(String label) {
        if(mRecording && markerFile != null)
            markerFile.queueWrite(new String [] {Float.toString(recordTime), label});
    }

    //To Do: If multiple devices with the same name connect, have different file names. Right now,
    // there will be file conflicts.
    public void startRecord(String fileName) {
        recordTime = 0;
        String fileDisplayName = displayName;
        //if(instanceId > 0)
        fileDisplayName += "_" + mBluetoothDevice.getAddress().replace(":", ".");

        TattooFile.createFolder(fileName);
        TattooFile.createFolder(fileName + File.separator + fileDisplayName);
        mFile = new TattooFile(fileName, fileDisplayName, mBLEParser.getSignalSettings(), mBLEParser.getSignalOrder());
        markerFile = new MarkerFile(fileName, fileDisplayName);
        mRecording = true;
    }

    public void setServiceUUIDs(ArrayList<UUID> serviceUUIDs) {
        this.mServiceUUIDs = serviceUUIDs;
    }

    public ArrayList<UUID> getServiceUUIDs() {
        return mServiceUUIDs;
    }

    public boolean connected() { return mConnected;}

    public void setConnected(boolean connected) {
        Long curTime = System.currentTimeMillis();
        mConnected = connected;

        //If recording, note the disconnect time
        if(mRecording && mConnected && disconnectTime != 0) {
            Long timeDisconnected = curTime - disconnectTime;
            markerFile.queueWrite(new String[] {Float.toString(recordTime),
                    "Device disconnected for " + timeDisconnected + " ms"});
        } else if (mRecording && !mConnected){
            disconnectTime = curTime;
        }
    }

    public void stopRecord() {
        mRecording = false;
    }

    public int getMenuID() {
        return menuID;
    }

    public void setMenuID(int menuID) {
        this.menuID = menuID;
    }

    public ArrayList<TattooMessage> getTmsTxMessages() {
        return mBLEParser.getTxMessages();
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public void setUniqueId(int uniqueID) {
        this.uniqueID = uniqueID;
    }

    public int getUniqueId() {
        return uniqueID;
    }

    public float getNotificationFrequency() {return mBLEParser.getNotificationFrequency(); }

    public String getSickbayNS() { return mBLEParser.sickbayNS; }
}

