package tan.philip.nrf_ble.ScanListScreen;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import tan.philip.nrf_ble.BLE.BLEHandlerService;
import tan.philip.nrf_ble.GraphScreen.GraphActivity;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.BLE.SignalSetting;

public class ScanResultsActivity extends AppCompatActivity {

    public static final String TAG = "ScanResultsActivity";
    public static final String EXTRA_BT_IDENTIFIER = "bt identifier";
    public static final String EXTRA_SIGNAL_SETTINGS_IDENTIFIER = "signal settings";

    private RecyclerView mRecyclerView;
    private BluetoothItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //Stuff for interacting with the service
    Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    //Bluetooth stuff
    private ArrayList<String> bluetoothAddresses;
    private ArrayList<BluetoothDevice> bluetoothDevices;

    //For the recycler viewer
    private ArrayList<BluetoothItem> bluetoothList;

    private boolean mConnecting;
    private int mConnectingIndex;
    private boolean mConnected;

    ////////////////////Methods for communicating with BLEHandlerService///////////////////////////

    //Handles messages from the BLEHandlerService
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BLEHandlerService.MSG_BT_DEVICES:
                    //The service is sending the list of Bluetooth devices
                    bluetoothAddresses = (ArrayList<String>)msg.getData().getSerializable("btAddresses");
                    bluetoothDevices = (ArrayList<BluetoothDevice>)msg.getData().getSerializable("btDevices");
                    buildRecyclerView();
                    break;
                case BLEHandlerService.MSG_GATT_CONNECTED:
                    mConnected = true;
                    invalidateOptionsMenu();
                    break;
                case BLEHandlerService.MSG_GATT_DISCONNECTED:
                    mConnected = false;
                    mConnecting = false;
                    invalidateOptionsMenu();
                    break;
                case BLEHandlerService.MSG_GATT_FAILED:
                    mConnected = false;
                    mConnecting = false;
                    resetConnectingText();
                    break;
                case BLEHandlerService.MSG_SEND_PACKAGE_INFORMATION:
                    resetConnectingText();
                    mConnected = true;
                    mConnecting = false;
                    startPWVGraphActivity((ArrayList<SignalSetting>) msg.getData().getSerializable("sigSettings"));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, BLEHandlerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
                getBluetoothDevices();
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
        }
    };

    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (BLEHandlerService.isRunning()) {
            doBindService();
        }
    }

    void doBindService() {
        mIsBound = bindService(new Intent(ScanResultsActivity.this, BLEHandlerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, BLEHandlerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void sendMessageToService(int msgID) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, msgID);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    private void connectDevice(String deviceAddress) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, BLEHandlerService.MSG_CONNECT, deviceAddress);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    ////////////////////////Life cycle Methods/////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_results);

        //Bind this activity to the BLEHandlerService
        doBindService();
        //CheckIfServiceIsRunning();

        //Initialize our ArrayLists
        bluetoothList = new ArrayList<>();
        bluetoothAddresses = new ArrayList<>();
        bluetoothDevices = new ArrayList<>();

        //Get the bluetooth devices and addresses from BLEHandlerService

        //buildRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        doBindService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
        }
        Log.d(TAG, "Destroyed ScanResultsActivity");
    }

    public void buildRecyclerView() {
        //Populate the lists with BT information
        bluetoothList = new ArrayList<>();

        for(int i = 0; i < bluetoothAddresses.size(); i++) {
            bluetoothList.add(new BluetoothItem(R.drawable.ic_bluetooth_black_24dp, getBluetoothIdentifier(i)));
        }

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

                    connectDevice(bluetoothAddresses.get(position));
                }
            }
        });

        mRecyclerView.addItemDecoration(new LineDivider(this));
    }

    private void changeItem(int position, String text) {
        bluetoothList.get(position).changeText1(text);
        mAdapter.notifyItemChanged(position);
    }



    //Bluetooth methods
    private void startPWVGraphActivity(ArrayList<SignalSetting> signalSettings) {
        Log.d(TAG, "Starting Graph Activity");
        Intent intent = new Intent(this, GraphActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable(EXTRA_SIGNAL_SETTINGS_IDENTIFIER, signalSettings);
        extras.putString(EXTRA_BT_IDENTIFIER, getBluetoothIdentifier(mConnectingIndex)); //Probably not necessary, graph activity can ask for it from the service
        intent.putExtras(extras);

        startActivity(intent);
    }

    private void resetConnectingText() {
        if(mConnectingIndex >= 0)
            changeItem(mConnectingIndex, getBluetoothIdentifier(mConnectingIndex));
    }

    private void getBluetoothDevices() {
        sendMessageToService(BLEHandlerService.MSG_REQUEST_SCAN_RESULTS);
    }

    //Returns the name, or address if name is null
    //Not good practice having this and having another method in BLEHandlerService but whatever
    private String getBluetoothIdentifier (int i) {
        if(bluetoothDevices.get(i).getName() != null)
            return bluetoothDevices.get(i).getName() + " (" + bluetoothAddresses.get(i) + ")";
        return "Unknown (" + bluetoothAddresses.get(i) + ")";
    }
}