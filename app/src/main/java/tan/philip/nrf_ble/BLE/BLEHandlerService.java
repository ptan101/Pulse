package tan.philip.nrf_ble.BLE;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static tan.philip.nrf_ble.Constants.NUS_UUID;

//This service is a higher level package to handle scanning, connection events, receiving and sending data, etc
public class BLEHandlerService extends Service {

    public static final String TAG = "BLEHandlerService";
    private Map<String, BluetoothDevice> mScanResults = new HashMap();
    private static boolean isRunning = false;

    //Messenger to communicate with client threads
    //final Messenger mMessenger = new Messenger(new ServiceHandler()); // Target we publish for clients to send messages to IncomingHandler.
    Messenger mMessenger;
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.

    //Client -> Service
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_START_SCAN = 3;
    public static final int MSG_STOP_SCAN = 4;
    public static final int MSG_CLEAR_SCAN = 5;
    public static final int MSG_REQUEST_SCAN_RESULTS = 6;
    public static final int MSG_CONNECT = 7;
    public static final int MSG_DISCONNECT = 17;
    public static final int MSG_CHECK_BT_ENABLED = 16;
    public static final int MSG_START_RECORD = 18;
    public static final int MSG_STOP_RECORD = 19;
    //Service -> Client
    public static final int MSG_BT_DEVICES = 8;
    public static final int MSG_SEND_PACKAGE_INFORMATION = 9;
    public static final int MSG_GATT_CONNECTED = 10;
    public static final int MSG_GATT_DISCONNECTED = 11;
    public static final int MSG_GATT_FAILED = 12;
    public static final int MSG_GATT_SERVICES_DISCOVERED = 13;
    public static final int MSG_GATT_ACTION_DATA_AVAILABLE = 14;
    public static final int MSG_CHECK_PERMISSIONS = 15;

    //final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.

    //Scanning
    private int numDevicesFound = 0;
    private boolean mScanning = false;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Handler mHandler;

    //Connecting
    private ArrayList<String> bluetoothAddresses = new ArrayList<>();
    private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private String deviceNameToConnect = "";

    private BluetoothLeService mBluetoothLeService;
    private boolean mConnecting;
    private int mConnectingIndex;
    private boolean mConnected;

    //Transceiving
    private BLEPacketParser bleparser;
    private String fileName;
    private boolean mRecording = false;


    /////////////////////////Lifecycle Methods//////////////////////////////////////////////
    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mMessenger = new Messenger(new ServiceHandler());
        return mMessenger.getBinder();
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
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        disconnectGattServer();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        isRunning = true;

        //Transcieving

    }

    @Override
    public void onDestroy() {
        //Connecting
        mConnecting = false;
        mConnectingIndex = -1;
        unregisterReceiver(mGattUpdateReceiver);
        disconnectGattServer();
        unbindService(mServiceConnection);

        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
        mBluetoothLeService = null;

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
                    String address = msg.obj.toString();
                    connectDevice(address);
                    break;
                case MSG_DISCONNECT:
                    disconnectGattServer();
                    break;
                case MSG_CHECK_BT_ENABLED:
                    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                        //Request permission to enable BT
                        sendMessageToUI(MSG_CHECK_PERMISSIONS);
                    }
                    break;
                case MSG_START_RECORD:
                    startRecord();
                    break;
                case MSG_STOP_RECORD:
                    stopRecord();
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

    //////////////////////////Scanning//////////////////////////////////////////////////
    private void setupBLEScanner() {
        //Get Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void startScan() {
        //BLE code
        //Return all broadcasting Bluetooth devices
        List<ScanFilter> filters = new ArrayList<>();


        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(NUS_UUID))
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
        mHandler = new Handler();
        //mHandler.postDelayed(this::stopScan, SCAN_PERIOD);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(numDevicesFound != mScanResults.size()) {
                    foundDevice();
                    numDevicesFound = mScanResults.size();
                }
                mHandler.postDelayed(this, 200);
            }
        }, 200);
    }

    private void stopScan() {
        //valueAnimator.cancel();
        mScanning = false;

        if(mHandler != null)
            mHandler.removeCallbacksAndMessages(null);

        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);

            if (mScanResults.isEmpty()) {
                Toast toast = Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
        }

        mScanCallback = null;
        mHandler = null;
    }

    private void clearScan() {
        bluetoothDevices.clear();
        bluetoothAddresses.clear();
        mScanResults.clear();
        numDevicesFound = 0;
    }

    private void viewScanList() {
        mHandler = new Handler();
        mHandler.removeCallbacksAndMessages(null);
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
    };


    /////////////////////////Connecting////////////////////////////////////////////////

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                Toast toast = Toast.makeText(BLEHandlerService.this, "Unable to initialize Bluetooth", Toast.LENGTH_SHORT);
                toast.show();
                stopSelf(); // Kill the service since BT was unable to be initialized.
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                sendMessageToUI(MSG_GATT_CONNECTED);

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mConnecting = false;
                sendMessageToUI(MSG_GATT_DISCONNECTED);

            } else if (BluetoothLeService.ACTION_GATT_FAILED.equals(action)) {
                mConnected = false;
                mConnecting = false;

                sendMessageToUI(MSG_GATT_FAILED);

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                Toast.makeText(BLEHandlerService.this, "Connection successful!", Toast.LENGTH_SHORT).show();
                initializeBLEParser();  //To do: initialize based on sensor name or a version characteristic?

                Bundle b = new Bundle();
                b.putSerializable("sigSettings", bleparser.getSignalSettings());
                b.putSerializable("bioSettings", bleparser.getBiometricsSettings());
                b.putInt("notif f", bleparser.notificationFrequency);
                sendDataToUI(b, MSG_SEND_PACKAGE_INFORMATION);

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte data[] = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                processPackage(data);
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    //Returns the name, or address if name is null
    private String getBluetoothIdentifier (String deviceAddress) {
        if(mScanResults.get(deviceAddress).getName() != null)
            return mScanResults.get(deviceAddress).getName() + " (" + deviceAddress + ")";
        return "Unknown (" + deviceAddress + ")";
    }

    private void connectDevice(String deviceAddress) {
        deviceNameToConnect = getBluetoothIdentifier(deviceAddress);
        Toast toast = Toast.makeText(getApplicationContext(), "Connecting to " + deviceNameToConnect, Toast.LENGTH_SHORT);
        toast.show();
        mBluetoothLeService.connect(deviceAddress);
    }

    public void disconnectGattServer() {
        if (mBluetoothLeService != null) {
            if(mConnected){
                String toast = "Device disconnected";
                Toast.makeText(BLEHandlerService.this, toast, Toast.LENGTH_SHORT).show();
            }

            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        }
        mConnected = false;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_FAILED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    /////////////////////////////////////////Transcieving//////////////////////////////////////////
    private void initializeBLEParser() {
        bleparser = new BLEPacketParser(this);

    }
    private void processPackage(byte[] data) {
        //If save enabled, save raw data to phone memory
        if(mRecording) {
            for (int i = 0; i < data.length; i ++) {
                FileWriter.writeBIN(data, fileName);
            }
        }

        //Convert byte array into arrays of signals and send over messenger
        ArrayList<ArrayList<Integer>> packaged_data = bleparser.parsePackage(data);

        //For each signal, filter in the right way
        ArrayList<float[]> filtered_data = new ArrayList<>();
        for (int i = 0; i < packaged_data.size(); i ++)
            filtered_data.add(bleparser.filterSignals(packaged_data.get(i), i));

        Bundle b = new Bundle();
        b.putSerializable("btData", filtered_data);
        sendDataToUI(b, MSG_GATT_ACTION_DATA_AVAILABLE);
    }

    private void startRecord() {
        fileName = Calendar.getInstance().getTime().toString() + ".bin";

        FileWriter.createFolder();
        FileWriter.writeBINHeader(bleparser.signalSettings, bleparser.signalOrder, fileName);
        mRecording = true;
    }

    private void stopRecord() {
        mRecording = false;
    }
}

