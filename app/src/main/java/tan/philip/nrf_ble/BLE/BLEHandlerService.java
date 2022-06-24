package tan.philip.nrf_ble.BLE;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.SickbayPush.SickbayPushService;

import static tan.philip.nrf_ble.Constants.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID;
import static tan.philip.nrf_ble.Constants.NUS_RX_UUID;
import static tan.philip.nrf_ble.Constants.NUS_TX_UUID;
import static tan.philip.nrf_ble.Constants.NUS_UUID;
import static tan.philip.nrf_ble.MessengerIDs.*;
import static tan.philip.nrf_ble.NotificationHandler.CHANNEL_ID;
import static tan.philip.nrf_ble.NotificationHandler.FOREGROUND_SERVICE_NOTIFICATION_ID;
import static tan.philip.nrf_ble.NotificationHandler.makeNotification;

//This service is a higher level package to handle scanning, connection events, receiving and sending data, etc
public class BLEHandlerService extends Service {

    public static final String TAG = "BLEHandlerService";
    private Map<String, BluetoothDevice> mScanResults = new HashMap();
    private static boolean isRunning = false;
    private BluetoothGatt mBluetoothGatt;

    //Messenger to communicate with client threads
    //final Messenger mMessenger = new Messenger(new ServiceHandler()); // Target we publish for clients to send messages to IncomingHandler.
    Messenger activityToServiceMessenger;
    final Messenger serviceToServiceMessenger = new Messenger(new BLEHandlerService.ServiceHandler());;
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.

    //Scanning
    private int numDevicesFound = 0;
    private boolean mScanning = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Handler scanHandler;

    //Connecting
    private ArrayList<String> bluetoothAddresses = new ArrayList<>();
    private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private String deviceNameToConnect = "";
    private String deviceAddress = "";

    private boolean mConnecting;
    private int mConnectingIndex;
    private boolean mConnected;

    //Transceiving
    private BLEPacketParser bleparser;
    private String fileName;
    private boolean mRecording = false;

    //Debug mode
    public static final String DEBUG_MODE_BT_ID = "Debug Mode";
    private Handler debugNotificationHandler;
    private boolean inDebugMode = false;
    private float debugNotificationFrequency;
    private int debugModeTime = 0;

    //Communication to SickbayPush
    SickbayPushService mService;
    boolean mIsBound = false;
    boolean pushToSickbay = true;


    NotificationCompat.Builder notificationBuilder;




    /////////////////////////Lifecycle Methods//////////////////////////////////////////////
    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        activityToServiceMessenger = new Messenger(new ServiceHandler());
        return activityToServiceMessenger.getBinder();
    }

    //@Override
    public void onStartCommand() {

    }

    @Override
    public void onCreate() {
        //Scanning
        setupBLEScanner();

        //Connecting
        mConnecting = false;
        mConnectingIndex = -1;
        disconnect();

        //Start and bind to the SickbayPushService
        Intent intent = new Intent(this, SickbayPushService.class);
        bindService(intent, sickbayPushConnection, Context.BIND_AUTO_CREATE);

        isRunning = true;

        //Transcieving

        //Foreground Service - keep it running. Just set up here, need to call startForeground to actually make it run in foreground
        Intent notificationIntent = new Intent(this, BLEHandlerService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Attempting BLE connection...")
                .setContentText("")
                .setSmallIcon(R.drawable.heartrate)
                .setContentIntent(pendingIntent);
    }

    @Override
    public void onDestroy() {
        //Unbind from the SickbayPushService
        unbindService(sickbayPushConnection);
        mIsBound = false;

        //Connecting
        mConnecting = false;
        mConnectingIndex = -1;
        disconnect();

        if(inDebugMode) {
            endDebugMode();
            inDebugMode = false;
        }
        isRunning = false;
    }

    //////////////////////////////////Messenger///////////////////////////////////////////////////
    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_START_SCAN:
                    startScan();
                    break;
                case MSG_STOP_SCAN:
                    stopScan();
                    break;
                case MSG_CLEAR_SCAN:
                    clearScan();
                    break;
                case MSG_REQUEST_SCAN_RESULTS:
                    Bundle b = new Bundle();
                    b.putSerializable("btAddresses", bluetoothAddresses);
                    b.putSerializable("btDevices", bluetoothDevices);
                    sendDataToUI(b, MSG_BT_DEVICES);  //Should make this a reply maybe
                    break;
                case MSG_CONNECT:
//                    String address = msg.getData().getString("deviceAddress");
                    connectDevice(msg.obj.toString());

                    notificationBuilder.setContentTitle("Attempting BLE connection...");
                    startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build());
                    makeNotification(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build());

                    break;
                case MSG_DISCONNECT:
                    disconnect();
                    stopForeground(true);

                    if(inDebugMode)
                        endDebugMode();

                    break;
                case MSG_CHECK_BT_ENABLED:
                    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                        //Request permission to enable BT
                        sendMessageToUI(MSG_CHECK_PERMISSIONS);
                    }
                    break;
                case MSG_START_RECORD:
                    fileName = msg.obj.toString();
                    startRecord();
                    break;
                case MSG_STOP_RECORD:
                    stopRecord();
                    break;
                case MSG_STOP_FOREGROUND:
                    stopForeground(true);
                    break;
                case MSG_START_DEBUG_MODE:
                    startDebugMode();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendDataToUI(Bundle b, int message_id) {
        for (int i = mClients.size()-1; i >= 0; i--) {
            try {
                Message msg = Message.obtain(null, message_id);
                msg.setData(b);
                mClients.get(i).send(msg);
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    private void sendMessageToUI(int msg_id) {
        for (int i = mClients.size()-1; i >= 0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, msg_id));
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    ////////////////////////////Connection to SickbayPush Service/////////////////////////////////

    private ServiceConnection sickbayPushConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            SickbayPushService.LocalBinder binder = (SickbayPushService.LocalBinder) service;
            mService = binder.getService();
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mIsBound = false;
        }
    };

    //////////////////////////////Gatt Callback///////////////////////////////////////////////////

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i(TAG, "Failed to connect.");
                mConnected = false;
                mConnecting = false;

                sendMessageToUI(MSG_GATT_FAILED);

                Toast.makeText(BLEHandlerService.this, "Connection failed." , Toast.LENGTH_SHORT).show();

                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                mConnected = false;
                //broadcastUpdate(intentAction);
                Log.i(TAG, "Failed to connect.");
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnected = false;
                Log.i(TAG, "Disconnected from GATT server.");
                mConnected = false;
                mConnecting = false;

                sendMessageToUI(MSG_GATT_DISCONNECTED);
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }

            BluetoothGattService service = gatt.getService(NUS_UUID);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(NUS_TX_UUID);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

            boolean mInitialized = gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

            if(mInitialized)
                Log.d(TAG, "Services initialized!");
            else
                Log.d(TAG, "Services not initialized");

            try {
                initializeBLEParser();  //To do: initialize based on sensor name or a version characteristic? Right now just sensor name
                Toast.makeText(BLEHandlerService.this, "Connection successful!", Toast.LENGTH_SHORT).show();

            } catch (FileNotFoundException e) {
                disconnect();
                sendMessageToUI(MSG_UNRECOGNIZED_NUS_DEVICE);
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            byte[] messageBytes = characteristic.getValue();

            //String messageString = Constants.bytesToHex(messageBytes);
            //Log.d(TAG, "Received message: " + messageString);

            processPacket(messageBytes);
            //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.e(TAG, characteristic.getStringValue(0));
        }
    };

    //////////////////////////Scanning//////////////////////////////////////////////////
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    private boolean setupBLEScanner() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    private void startScan() {
        //BLE code
        //Return all broadcasting Bluetooth devices
        List<ScanFilter> filters = new ArrayList<>();

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(NUS_UUID))
                .build();

        filters.add(scanFilter);

        //For some reason, the dongle is not recognized... This is just temp.
        scanFilter = new ScanFilter.Builder()
                .setDeviceName("PPG Dongle")
                .build();
        filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        //Store scan results in HashMap
        mScanResults = new HashMap<>();
        mScanCallback = new BtleScanCallback(mScanResults);

        //Now grab hold of the BluetoothLeScanner to start the scan, and set our scanning boolean to true.
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mScanning = true;

        //The scan results saved in the map includes:
        //BluetoothDevice: Name and address
        //RSSI: Received signal strength indication
        //Timestamp
        //ScanRecord
        //  - Advertisement Flags: Discoverable mode and cababilities of the device
        //  - Manufacturer Specific Data: Info useful when filtering
        //  - GATT Service UUIDs


        //Limit the scan duration to a specified time
        scanHandler = new Handler();
        //mHandler.postDelayed(this::stopScan, SCAN_PERIOD);
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(numDevicesFound != mScanResults.size()) {
                    foundDevice();
                    numDevicesFound = mScanResults.size();
                }
                scanHandler.postDelayed(this, 200);
            }
        }, 200);
    }

    private void stopScan() {
        //valueAnimator.cancel();
        mScanning = false;

        if(scanHandler != null)
            scanHandler.removeCallbacksAndMessages(null);

        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);

            if (mScanResults.isEmpty()) {
                Toast toast = Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
        }

        mScanCallback = null;
        scanHandler = null;
    }

    private void clearScan() {
        bluetoothDevices.clear();
        bluetoothAddresses.clear();
        mScanResults.clear();
        numDevicesFound = 0;
    }

    private void viewScanList() {
        scanHandler = new Handler();
        scanHandler.removeCallbacksAndMessages(null);
    }

    private void foundDevice() {
        for (String deviceAddress : mScanResults.keySet()) {
            Log.d(TAG, "Found device: " + deviceAddress);
        }

        //Seperate map to two seperate ArrayLists (may not be necessary if I can send HashMap but oh well)
        bluetoothAddresses.clear();
        bluetoothDevices.clear();
        for(String deviceAddress : mScanResults.keySet()) {
            bluetoothAddresses.add(deviceAddress);
            bluetoothDevices.add(mScanResults.get(deviceAddress));
        }

        Bundle b = new Bundle();
        b.putSerializable("btAddresses", bluetoothAddresses);
        b.putSerializable("btDevices", bluetoothDevices);
        sendDataToUI(b, MSG_BT_DEVICES);
    }


    private class BtleScanCallback extends ScanCallback {

        List<UUID> serviceUUIDsList        = new ArrayList<>();
        List<UUID> characteristicUUIDsList = new ArrayList<>();
        List<UUID> descriptorUUIDsList     = new ArrayList<>();


        private Map<String, BluetoothDevice> mScanResults;

        BtleScanCallback(Map<String, BluetoothDevice> scanResults) {
            mScanResults = scanResults;
        }
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }
        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();

            serviceUUIDsList = getServiceUUIDsList(result, deviceAddress);

            mScanResults.put(deviceAddress, device);
        }

        private List<UUID> getServiceUUIDsList(ScanResult scanResult, String deviceAddress)
        {
            List<ParcelUuid> parcelUuids = scanResult.getScanRecord().getServiceUuids();

            List<UUID> serviceList = new ArrayList<>();

            if(parcelUuids != null) {
                for (int i = 0; i < parcelUuids.size(); i++) {
                    UUID serviceUUID = parcelUuids.get(i).getUuid();
                    String uuidString = serviceUUID.toString();
                    //Log.d(TAG, deviceAddress + " " + uuidString);

                    if (!serviceList.contains(serviceUUID))
                        serviceList.add(serviceUUID);
                }
            }

            return serviceList;
        }
    }


    /////////////////////////Connecting////////////////////////////////////////////////

    //Returns the name, or address if name is null
    private String getBluetoothIdentifier (String deviceAddress) {
        if(mScanResults.get(deviceAddress).getName() != null)
            return mScanResults.get(deviceAddress).getName() + " (" + deviceAddress + ")";
        return "Unknown (" + deviceAddress + ")";
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connectDevice(final String address) {
        deviceAddress = address;
        deviceNameToConnect = getBluetoothIdentifier(deviceAddress);

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        //Autoconnect = True
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");

        mConnecting = true;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */

    public void disconnect() {
        if(mConnected){
            //String toast = "Device disconnected";
            //Toast.makeText(BLEHandlerService.this, toast, Toast.LENGTH_SHORT).show();
        }

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();

        close();

        sendMessageToUI(MSG_GATT_DISCONNECTED);

        mConnected = false;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    /////////////////////////////////////////Transcieving//////////////////////////////////////////
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public void writeCharacteristic(byte[] txBytes) {
        BluetoothGattService service = mBluetoothGatt.getService(NUS_UUID);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(NUS_RX_UUID);

        if(characteristic != null){
            characteristic.setValue(txBytes);
        }

        mBluetoothGatt.writeCharacteristic(characteristic);

        this.setCharacteristicNotification(characteristic, true);
    }

    private void initializeBLEParser() throws FileNotFoundException {
        bleparser = new BLEPacketParser(this, deviceNameToConnect);

        Bundle b = new Bundle();
        b.putSerializable("sigSettings", bleparser.getSignalSettings());
        b.putSerializable("bioSettings", bleparser.getBiometricsSettings());
        b.putFloat("notif f", bleparser.notificationFrequency);
        sendDataToUI(b, MSG_SEND_PACKAGE_INFORMATION);

        notificationBuilder.setContentTitle("Paired with " + deviceNameToConnect);
        makeNotification(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build());

        sendMessageToUI(MSG_GATT_CONNECTED);
    }

    private void processPacket(byte[] data) {
        //If save enabled, save raw data to phone memory
        if(mRecording) {
            FileWriter.writeBIN(data, fileName);
        }

        //Sometimes a packet arrives before app can disconnect when BLE device not recognized.
        if (bleparser == null)
            return;

        //Convert byte array into arrays of signals
        ArrayList<ArrayList<Integer>> packaged_data = bleparser.parsePacket(data);

        //Send data to SickbayPushService to be sent over web sockets
        if (pushToSickbay)
            mService.addToQueue(packaged_data);

        ///Formatting for plotting
        //For each signal, filter in the right way
        ArrayList<float[]> filtered_data = new ArrayList<>();
        for (int i = 0; i < packaged_data.size(); i ++)
            filtered_data.add(bleparser.filterSignals(packaged_data.get(i), i));

        //Send the data to the UI for display
        Bundle b = new Bundle();
        b.putSerializable("btData", filtered_data);
        sendDataToUI(b, MSG_GATT_ACTION_DATA_AVAILABLE);
    }

    private void startRecord() {
        FileWriter.createFolder(fileName);
        FileWriter.createFolder(fileName);
        FileWriter.writeBINHeader(bleparser.signalSettings, bleparser.signalOrder, fileName);
        mRecording = true;
    }

    private void stopRecord() {
        mRecording = false;
    }

    /////////////////////Debug mode code//////////////////////////////////

    private void startDebugMode() {
        inDebugMode = true;
        deviceNameToConnect = DEBUG_MODE_BT_ID;

        try {
            initializeBLEParser();
        } catch (FileNotFoundException e) {
            //debug.init file was not set up.
        }

        debugNotificationFrequency = bleparser.notificationFrequency;

        debugNotificationHandler = new Handler();
        debugNotifier.run();
    }

    private void endDebugMode() {
        inDebugMode = false;
        debugNotificationHandler.removeCallbacks(debugNotifier);
    }

    Runnable debugNotifier = new Runnable() {
        @Override
        public void run() {
            try {
                byte data[] = new byte[bleparser.packageSizeBytes];
                int numSamples[] = new int[bleparser.getSignalSettings().size()];
                int bytesWritten = 0;
                //Process fake data
                for (int i: bleparser.getSignalOrder()) {
                    int new_data;
                    SignalSetting curSignal = bleparser.getSignalSettings().get(i);
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
                debugModeTime += bleparser.notificationFrequency;

                //In the future, if we care about optimization of debug mode, this can be made into
                //a lookup table as long as one period is fit in the table
                processPacket(data);
            } finally {
                debugNotificationHandler.postDelayed(debugNotifier, (long) (debugNotificationFrequency * 1000));
            }
        }
    };
}

