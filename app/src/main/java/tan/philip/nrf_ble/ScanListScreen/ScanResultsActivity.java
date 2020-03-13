package tan.philip.nrf_ble.ScanListScreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import tan.philip.nrf_ble.BluetoothLeService;
import tan.philip.nrf_ble.Constants;
import tan.philip.nrf_ble.GraphScreen.GraphActivity;
import tan.philip.nrf_ble.GraphScreen.GraphDebugActivity;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanScreen.ClientActivity;

import static tan.philip.nrf_ble.Constants.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID;
import static tan.philip.nrf_ble.Constants.CHARACTERISTIC_UUID;
import static tan.philip.nrf_ble.Constants.SERVICE_UUID;

public class ScanResultsActivity extends AppCompatActivity {

    public static final String TAG = "ScanResultsActivity";
    public static final String EXTRA_BT_IDENTIFIER = "bt identifier";

    private RecyclerView mRecyclerView;
    private BluetoothItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //Bluetooth stuff
    private Map<String, BluetoothDevice> mScanResults;
    private ArrayList<String> bluetoothAddresses;
    private ArrayList<BluetoothDevice> bluetoothDevices;

    //For the recycler viewer
    private ArrayList<BluetoothItem> bluetoothList;

    private boolean mConnecting;
    private int mConnectingIndex;
    private boolean mConnected;

    private BluetoothLeService mBluetoothLeService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                Toast toast = Toast.makeText(ScanResultsActivity.this, "Unable to initialize Bluetooth", Toast.LENGTH_SHORT);
                toast.show();
                finish();
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
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mConnecting = false;
                resetConnectingText();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_FAILED.equals(action)) {
                mConnected = false;
                mConnecting = false;
                runOnUiThread(() -> Toast.makeText(ScanResultsActivity.this, "Connection failed", Toast.LENGTH_SHORT).show());
                resetConnectingText();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                runOnUiThread(() -> Toast.makeText(ScanResultsActivity.this, "Connection successful!", Toast.LENGTH_SHORT).show());
                startGraphActivity();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    @Override
    @SuppressWarnings("unchecked")  //For HashMap cast
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_results);
        this.getSupportActionBar().hide();

        Intent intent = getIntent();
        mScanResults = (HashMap<String, BluetoothDevice>)intent.getSerializableExtra(ClientActivity.EXTRA_BT_SCAN_RESULTS);
        bluetoothList = new ArrayList<>();
        bluetoothAddresses = new ArrayList<>();
        bluetoothDevices = new ArrayList<>();

        for(String deviceAddress : mScanResults.keySet()) {
            bluetoothList.add(new BluetoothItem(R.drawable.ic_bluetooth_black_24dp, getBluetoothIdentifier(deviceAddress)));

            bluetoothAddresses.add(deviceAddress);
            bluetoothDevices.add(mScanResults.get(deviceAddress));
        }

        bluetoothList.add(new BluetoothItem(R.drawable.ic_bug_report_black_24dp, "Debug mode"));
        bluetoothAddresses.add("00:00:00:00:00:00");
        bluetoothDevices.add(null);

        buildRecyclerView();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }
    @Override
    protected void onResume() {
        super.onResume();
        mConnecting = false;
        resetConnectingText();
        mConnectingIndex = -1;
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        disconnectGattServer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConnecting = false;
        resetConnectingText();
        mConnectingIndex = -1;
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "Destroyed ScanResultsActivity");
        disconnectGattServer();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    public void buildRecyclerView() {
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new BluetoothItemAdapter(bluetoothList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new BluetoothItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if(!mConnecting) {
                    mConnecting = true;
                    mConnectingIndex = position;
                    changeItem(position, "Connecting...");
                    connectDevice(bluetoothDevices.get(position));
                }
            }
        });

        mRecyclerView.addItemDecoration(new LineDivider(this));
    }

    private void changeItem(int position, String text) {
        bluetoothList.get(position).changeText1(text);
        mAdapter.notifyItemChanged(position);
    }

    //Returns the name, or address if name is null
    private String getBluetoothIdentifier (String deviceAddress) {
        if(mScanResults.get(deviceAddress) == null)
            return "Debug Mode";

        if(mScanResults.get(deviceAddress).getName() != null)
            return mScanResults.get(deviceAddress).getName();
        return deviceAddress;
    }

    //Bluetooth methods
    private void connectDevice(BluetoothDevice device) {
        if(device == null) {
            startGraphDebugActivity();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Connecting to " + getBluetoothIdentifier(device.getAddress()), Toast.LENGTH_SHORT);
            toast.show();
            mBluetoothLeService.connect(device.getAddress());
        }
    }

    public void disconnectGattServer() {
        if (mBluetoothLeService != null) {
            if(mConnected){
                String toast = "Device disconnected";
                runOnUiThread(() -> Toast.makeText(ScanResultsActivity.this, toast, Toast.LENGTH_SHORT).show());
            }

            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        }
        mConnected = false;
    }

    private void startGraphActivity() {
        Intent intent = new Intent(this, GraphActivity.class);
        intent.putExtra(EXTRA_BT_IDENTIFIER, getBluetoothIdentifier(bluetoothAddresses.get(mConnectingIndex)));

        startActivity(intent);
    }

    private void startGraphDebugActivity() {
        Intent intent = new Intent(this, GraphDebugActivity.class);

        startActivity(intent);
    }

    private void resetConnectingText() {
        if(mConnectingIndex >= 0)
            changeItem(mConnectingIndex, getBluetoothIdentifier(bluetoothAddresses.get(mConnectingIndex)));
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
}