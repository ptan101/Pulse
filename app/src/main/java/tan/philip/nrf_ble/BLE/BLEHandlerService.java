package tan.philip.nrf_ble.BLE;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.BLETattooDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice;
import tan.philip.nrf_ble.BLE.Gatt.GattManager;
import tan.philip.nrf_ble.Events.NUSPacketRecievedEvent;
import tan.philip.nrf_ble.Events.PlotDataEvent;
import tan.philip.nrf_ble.Events.ScanListUpdatedEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEClearScanListEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEConnectEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEDisconnectEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEStartScanEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEStopScanEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestEndBLEForegroundEvent;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.SickbayPush.SickbayPushService;

import static tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice.DEBUG_MODE_ADDRESS;
import static tan.philip.nrf_ble.Constants.NUS_UUID;
import static tan.philip.nrf_ble.NotificationHandler.CHANNEL_ID;
import static tan.philip.nrf_ble.NotificationHandler.FOREGROUND_SERVICE_NOTIFICATION_ID;
import static tan.philip.nrf_ble.NotificationHandler.makeNotification;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

//This service is a higher level package to handle scanning, connection events, receiving and sending data, etc
public class BLEHandlerService extends Service {

    public static final String TAG = "BLEHandlerService";
    private Map<String, BluetoothDevice> mScanResults = new HashMap();
    private static boolean isRunning = false;

    //Messenger to communicate with client threads
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    private final IBinder binder = new LocalBinder();

    //Tattoo Connection Management
    private GattManager mGattManager;
    private TattooConnectionManager mConnectionManager;

    //Scanning
    private int numDevicesFound = 0;
    private boolean mScanning = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Handler scanHandler;

    //Connecting
    private final ArrayList<String> bluetoothAddresses = new ArrayList<>();
    private final ArrayList<BluetoothDevice> broadcastingBLEDevices = new ArrayList<>();

    //Communication to SickbayPush
    SickbayPushService mService;
    boolean mIsBound = false;
    boolean pushToSickbay = true;

    //Saving to file
    private boolean mRecording = false;

    NotificationCompat.Builder notificationBuilder;


    /////////////////////////Lifecycle Methods//////////////////////////////////////////////
    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //activityToServiceMessenger = new Messenger(new ServiceHandler());
        //return activityToServiceMessenger.getBinder();
        return binder;
    }

    //@Override
    public void onStartCommand() {

    }

    @Override
    public void onCreate() {
        mGattManager = new GattManager(this);
        mConnectionManager = new TattooConnectionManager(this, mGattManager);

        //Register on EventBus
        EventBus.getDefault().register(this);

        //Scanning
        setupBLEScanner();

        //Connecting
        //disconnect();

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
        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
        mConnectionManager.unregister();

        //Unbind from the SickbayPushService
        unbindService(sickbayPushConnection);
        mIsBound = false;

        //Connecting
        closeAllConnections();

        isRunning = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void endForeground(RequestEndBLEForegroundEvent event) {
        stopForeground(true);
    }

    public class LocalBinder extends Binder {
        public BLEHandlerService getService() {
            // Return this instance of BLEHandlerService so clients can call public methods
            return BLEHandlerService.this;
        }
    }

    ////////////////////////////Connection to SickbayPush Service/////////////////////////////////

    private final ServiceConnection sickbayPushConnection = new ServiceConnection() {
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

    //////////////////////////Scanning//////////////////////////////////////////////////
    public boolean hasBLEPermissions() {
        return (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());
    }

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

    @Subscribe(threadMode =  ThreadMode.MAIN)
    public void startScan(RequestBLEStartScanEvent event) {
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
        scanHandler = new Handler();
        //mHandler.postDelayed(this::stopScan, SCAN_PERIOD);
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(numDevicesFound != mScanResults.size()) {
                    foundDevice();
                    numDevicesFound = mScanResults.size();
                }

                EventBus.getDefault().post(new ScanListUpdatedEvent(mScanResults));

                scanHandler.postDelayed(this, 200);
            }
        }, 200);
    }

    @Subscribe(threadMode =  ThreadMode.MAIN)
    private void stopScan(RequestBLEStopScanEvent event) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void clearScan(RequestBLEClearScanListEvent event) {
        broadcastingBLEDevices.clear();
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
        broadcastingBLEDevices.clear();
        for(String deviceAddress : mScanResults.keySet()) {
            bluetoothAddresses.add(deviceAddress);
            broadcastingBLEDevices.add(mScanResults.get(deviceAddress));
        }

        Bundle b = new Bundle();
        b.putSerializable("btAddresses", bluetoothAddresses);
        b.putSerializable("btDevices", broadcastingBLEDevices);
        //sendMessageToUI(MSG_BT_DEVICES, b);
    }

    private class BtleScanCallback extends ScanCallback {

        List<UUID> serviceUUIDsList        = new ArrayList<>();
        List<UUID> characteristicUUIDsList = new ArrayList<>();
        List<UUID> descriptorUUIDsList     = new ArrayList<>();


        private final Map<String, BluetoothDevice> mScanResults;

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
    public ArrayList<BLEDevice> getBLEDevices() {
        return mConnectionManager.getBLEDevices();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void connectDevices(RequestBLEConnectEvent event) {
        ArrayList<String> addresses = event.getAddresses();
        for (String address : addresses)
            connectDevice(address);
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
    private void connectDevice(final String address) {
        notificationBuilder.setContentTitle("Attempting BLE connection...");
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build());
        makeNotification(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build());

        //deviceAddress = address;
        //deviceNameToConnect = getBluetoothIdentifier(deviceAddress);

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return;
        }

        try {
            BLETattooDevice newTattooDevice;
            if(!address.equals(DEBUG_MODE_ADDRESS)) {
                final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                newTattooDevice = new BLETattooDevice(this, device);

                if (device == null) {
                    Log.w(TAG, "Device not found.  Unable to connect.");
                    return;
                }
            } else {
                newTattooDevice = new DebugBLEDevice(this, null);
            }

            mConnectionManager.checkAndConnectToTattoo(newTattooDevice);
            notificationBuilder.setContentTitle("Paired with " + newTattooDevice.getBluetoothIdentifier());
            makeNotification(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build());

        } catch (FileNotFoundException e) {
            disconnect(address);
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(String address) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mConnectionManager.disconnectTattoo(address);
        if(mConnectionManager.getBLEDevices().size() == 0)
            stopForeground(true);
    }

    public void closeAllConnections() {
        for(BLEDevice d : mConnectionManager.getBLEDevices())
            disconnect(d.getAddress());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void disconnect(RequestBLEDisconnectEvent event) {
        closeAllConnections();
    }

    public static boolean isRunning() {
        return isRunning;
    }

    /////////////////////Transcieving/////////////////////////////////////
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void processNUSPacket(NUSPacketRecievedEvent event) {
        byte[] messageBytes = event.getPacketData();
        BLETattooDevice tattoo = event.getTattoo();

        if(mRecording)
            tattoo.saveToFile(messageBytes);

        HashMap<Integer, ArrayList<Integer>> packaged_data = tattoo.convertPacketToHashMap(messageBytes);

        //Send data to SickbayPushService to be sent over web sockets
        //TO DO: Send the filtered data to Sickbay
        if (pushToSickbay) {
            //Convert to HashMap. Keys are the Sickbay IDs.
            HashMap<Integer, ArrayList<Integer>> sickbayPush = tattoo.convertPacketForSickbayPush(packaged_data);

            mService.addToQueue(sickbayPush);
        }

        //Send the data to the UI for display
        EventBus.getDefault().post(new PlotDataEvent(tattoo.getAddress(), tattoo.convertPacketForDisplay(packaged_data)));
    }
}

