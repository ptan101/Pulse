package tan.philip.nrf_ble.ScanScreen;

import android.Manifest;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;

import tan.philip.nrf_ble.Algorithms.BiometricsSet;
import tan.philip.nrf_ble.BLE.BLEHandlerService;
import tan.philip.nrf_ble.BLE.FileWriter;
import tan.philip.nrf_ble.BLE.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.GraphActivity;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity;
import tan.philip.nrf_ble.databinding.ActivityClientBinding;

import static tan.philip.nrf_ble.GraphScreen.GraphActivity.EXTRA_BIOMETRIC_SETTINGS_IDENTIFIER;
import static tan.philip.nrf_ble.GraphScreen.GraphActivity.EXTRA_BT_IDENTIFIER;
import static tan.philip.nrf_ble.GraphScreen.GraphActivity.EXTRA_NOTIF_F_IDENTIFIER;
import static tan.philip.nrf_ble.MessengerIDs.*;
import static tan.philip.nrf_ble.NotificationHandler.createNotificationChannel;
import static tan.philip.nrf_ble.NotificationHandler.makeNotification;

public class ClientActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    //private ImageButton btnScan;

    private static final String TAG = "ClientActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_CODE = 2;
    public static final String EXTRA_BT_SCAN_RESULTS = "scan results";

    Messenger mService = null;
    boolean mIsBound = false;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ActivityClientBinding mBinding;

    private boolean mScanning = false;
    private int numDevicesFound = 0;

    //Color scan background
    private final int GREY = 0xFF2C2C2C;
    private ArrayList<Pulse> pulses;

    ////////////////////Methods for communicating with BLEHandlerService///////////////////////////

    //Handles messages from the BLEHandlerService
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BT_DEVICES:
                    //The service is sending the list of Bluetooth devices
                    ArrayList<String> bluetoothAddresses = (ArrayList<String>)msg.getData().getSerializable("btAddresses");
                    int newNumDevices = bluetoothAddresses.size();
                    if(newNumDevices > numDevicesFound) {
                        foundDevice(newNumDevices);
                    }
                    break;
                case MSG_CHECK_PERMISSIONS:
                    requestBluetoothEnable();
                    FileWriter.isStoragePermissionGranted(ClientActivity.this);
                    break;
                case MSG_SEND_PACKAGE_INFORMATION:
                    startGraphActivity((ArrayList<SignalSetting>) msg.getData().getSerializable("sigSettings"),
                            (BiometricsSet) msg.getData().getSerializable("bioSettings"),
                            (float) msg.getData().getFloat("notif f"));
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
                Message msg = Message.obtain(null, MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
                sendMessageToService(MSG_CLEAR_SCAN);
                numDevicesFound = 0;
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
        mIsBound = bindService(new Intent(ClientActivity.this, BLEHandlerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MSG_UNREGISTER_CLIENT);
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


    /////////////////////////////Life cycle functions///////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_client);

        createNotificationChannel(this);

        //Start the BLEHandlerService
        Intent intent = new Intent(ClientActivity.this, BLEHandlerService.class);
        startService(intent);
        //this.getSupportActionBar().hide();

        //Set up background color transition
        setupPulses();
        setupButtons();



        //setupBLE();


        //CheckIfServiceIsRunning();
    }

    @Override
    protected void onStop() {
        super.onStop();


        //if(mHandler != null)
        //    mHandler.removeCallbacksAndMessages(null);

        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
        }

        stopScan();
    }

    @Override
    protected void onStart() {
        super.onStart();
        doBindService();

        mBinding.btnListDevices.setVisibility(View.GONE);
        mBinding.layout1.getBackground().setAlpha(0);
        //mBinding.layout1.setBackgroundColor(0xFF2c2a3a);
        //mBinding.layout1.setBackgroundColor(GREY);
        //mHandler = new Handler();

        if (!hasPermissions()) {
            getPermissions();
        }


        //Check if device supports BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }

        stopScan();

        //Clear scan results
        numDevicesFound = 0;
        sendMessageToService(MSG_CLEAR_SCAN);
        sendMessageToService(MSG_STOP_FOREGROUND);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
        }
    }

    /////////////////////////////////////UI///////////////////////////////////////////////////////
    public void showOptions(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);

        popup.inflate(R.menu.popup_menu_client);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.enterDebugMode:
                sendMessageToService(MSG_START_DEBUG_MODE);
                return true;
            default:
                return false;
        }
    }

    private void startGraphActivity(ArrayList<SignalSetting> signalSettings, BiometricsSet bioSettings, float notif_f) {
        Log.d(TAG, "Starting Graph Activity");
        Intent intent = new Intent(this, GraphActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable(GraphActivity.EXTRA_SIGNAL_SETTINGS_IDENTIFIER, signalSettings);
        extras.putSerializable(EXTRA_BIOMETRIC_SETTINGS_IDENTIFIER, bioSettings);
        extras.putString(EXTRA_BT_IDENTIFIER, "Debug Mode"); //Probably not necessary, graph activity can ask for it from the service
        extras.putFloat(EXTRA_NOTIF_F_IDENTIFIER, notif_f);
        intent.putExtras(extras);

        startActivity(intent);
    }

    public void viewScanList(View view) {
        Intent intent = new Intent(this, ScanResultsActivity.class);
        //intent.putExtra(EXTRA_BT_SCAN_RESULTS, (HashMap)mScanResults);
        startActivity(intent);
    }

    private void startScan() {
        mBinding.layout1.getBackground().setAlpha(0);

        sendMessageToService(MSG_CLEAR_SCAN);
        numDevicesFound = 0;
        sendMessageToService(MSG_START_SCAN);

        if (!hasPermissions() || mScanning) {
            return;
        }

        //mBinding.btnListDevices.setVisibility(View.GONE);

        //Display message
        Toast toast = Toast.makeText(getApplicationContext(), "Scanning for BLE devices...", Toast.LENGTH_SHORT);
        toast.show();

        for(Pulse currentPulse: pulses) {
            currentPulse.restart();
        }



        mScanning = true;
    }

    private void stopScan() {
        sendMessageToService(MSG_STOP_SCAN);
        //valueAnimator.cancel();
        mScanning = false;
        for(Pulse pulse: pulses) {
            pulse.end();
        }
    }

    private void foundDevice(int newNumDevices) {
        if(numDevicesFound == 0) {
            //Animate button
            mBinding.btnListDevices.setVisibility(View.VISIBLE);

            //Change background color
            startBackgroundTransition();


            ValueAnimator newButton = ValueAnimator.ofInt(0, 255);
            newButton.setDuration(1000);
            newButton.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    mBinding.btnListDevices.getBackground().setAlpha((int) newButton.getAnimatedValue());
                    mBinding.btnListDevices.setTextColor((mBinding.btnListDevices.getTextColors().withAlpha((int) newButton.getAnimatedValue())));
                }
            });
            newButton.start();
        }

        numDevicesFound = newNumDevices;
        mBinding.btnListDevices.setText("List Devices (" + numDevicesFound + ")");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("Permission", "onRequestPermissionsResult: "+ grantResults[0] + grantResults[1]);
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(grantResults.length==2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    System.exit(0);
                }
                break;
            default:
                Toast.makeText(this, "????????", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasPermissions() {

//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            requestBluetoothEnable();
//            Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Adapter not enabled", Toast.LENGTH_LONG);
//            toast.show();
//            return false;
//
//        } else
        sendMessageToService(MSG_CHECK_BT_ENABLED);

//        if (!hasLocationPermissions()) {
//            requestLocationPermission();
//            Toast toast = Toast.makeText(getApplicationContext(), "Please enable location permissions", Toast.LENGTH_LONG);
//            toast.show();
//            return false;
//        }
//        return true;

        int write_external_storage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int location_access = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return (write_external_storage == PackageManager.PERMISSION_GRANTED) && (location_access == PackageManager.PERMISSION_GRANTED);
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "Requested user enabled Bluetooth. Try starting the scan again.");
    }
//    private boolean hasLocationPermissions() {
//        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
//    }
//    private void requestLocationPermission() {
//        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
//    }

    private void getPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_PERMISSION_CODE);
    }

    private void startBackgroundTransition() {
        ValueAnimator backgroundTransition = ValueAnimator.ofInt(0, 255);
        backgroundTransition.setDuration(4000);
        backgroundTransition.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mBinding.layout1.getBackground().setAlpha((int) backgroundTransition.getAnimatedValue());
            }
        });
        backgroundTransition.start();

    }

    private void setupPulses() {
        pulses = new ArrayList<>();
        Pulse.setDPScale(getApplicationContext().getResources().getDisplayMetrics().density);

        for(int i = 0; i < 4; i ++) {
            Pulse newPulse;
            switch (i) {
                case 0: newPulse = new Pulse(mBinding.pulse1, i, (Vibrator) getSystemService(Context.VIBRATOR_SERVICE)); break;
                case 1: newPulse = new Pulse(mBinding.pulse2, i, (Vibrator) getSystemService(Context.VIBRATOR_SERVICE)); break;
                case 2: newPulse = new Pulse(mBinding.pulse3, i, (Vibrator) getSystemService(Context.VIBRATOR_SERVICE)); break;
                default: newPulse = new Pulse(mBinding.pulse4, i,(Vibrator) getSystemService(Context.VIBRATOR_SERVICE));
            }
            pulses.add(newPulse);
        }
    }

    private void setupButtons() {
        mBinding.btnListDevices.setVisibility(View.GONE);

        mBinding.btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mScanning) {
                    startScan();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Scan stopped", Toast.LENGTH_SHORT);
                    toast.show();

                    //mHandler.removeCallbacksAndMessages(null);
                    stopScan();

                    //Reset background color
                    //mBinding.layout1.setBackgroundColor(GREY);
                }
            }
        });

        mBinding.btnListDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
                viewScanList(view);
            }
        });
    }
}