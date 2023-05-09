package tan.philip.nrf_ble.ScanScreen;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import tan.philip.nrf_ble.BLE.BLEHandlerService;
import tan.philip.nrf_ble.Events.Connecting.BLEIconNumSelectedChangedEvent;
import tan.philip.nrf_ble.Events.ScanListUpdatedEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEClearScanListEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEConnectEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEStartScanEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEStopScanEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestEndBLEForegroundEvent;
import tan.philip.nrf_ble.FileWriting.PulseFile;
import tan.philip.nrf_ble.GraphScreen.GraphActivity;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.databinding.ActivityScanningBinding;

import static tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice.DEBUG_MODE_ADDRESS;
import static tan.philip.nrf_ble.NotificationHandler.createNotificationChannel;
import static tan.philip.nrf_ble.NotificationHandler.makeNotification;

import com.opencsv.CSVWriter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ScanningActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    //private ImageButton btnScan;

    private static final String TAG = "ClientActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_CODE = 2;
    public static final String EXTRA_BT_SCAN_RESULTS = "scan results";

    BLEHandlerService bleHandlerService;
    boolean mIsBound = false;

    private ActivityScanningBinding mBinding;

    private boolean connectButtonVisible = false;
    private boolean mScanning = false;
    private int numDevicesFound = 0;
    private final Map<String, BluetoothDevice> scanResults = new HashMap<>();
    BLEScanIconManager mIconManager;

    //Color scan background
    private final int GREY = 0xFF2C2C2C;
    private ArrayList<Pulse> pulses;

    ////////////////////Methods for communicating with BLEHandlerService///////////////////////////


    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            EventBus.getDefault().post(new RequestBLEClearScanListEvent());
            numDevicesFound = 0;

            BLEHandlerService.LocalBinder binder = (BLEHandlerService.LocalBinder) service;
            bleHandlerService = binder.getService();
            mIsBound = true;
            if(!bleHandlerService.hasBLEPermissions())
                getPermissions();
        }

        public void onServiceDisconnected(ComponentName className) { mIsBound = false; }
    };

    /////////////////////////////Life cycle functions///////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_scanning);

        //Register to EventBus
        EventBus.getDefault().register(this);

        createNotificationChannel(this);

        //Request permissions
        requestBluetoothEnable();
        PulseFile.isStoragePermissionGranted(ScanningActivity.this);

        //Start the BLEHandlerService
        Intent intent = new Intent(ScanningActivity.this, BLEHandlerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //this.getSupportActionBar().hide();

        //Set up background color transition
        setupPulses();
        setupButtons();
        mIconManager = new BLEScanIconManager((ConstraintLayout)findViewById(R.id.layout1), this);

        //setupBLE();


        //CheckIfServiceIsRunning();
    }

    @Override
    protected void onStop() {
        super.onStop();

        //if(mHandler != null)
        //    mHandler.removeCallbacksAndMessages(null);

        try {
            //doUnbindService();
        }
        catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
        }

        stopScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //doBindService();

        mBinding.btnStartBleConnection.setVisibility(View.GONE);
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
        EventBus.getDefault().post(new RequestBLEClearScanListEvent());
        EventBus.getDefault().post(new RequestEndBLEForegroundEvent());
        mIconManager.clearAllIcons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
        mIconManager.deregister();

        unbindService(mConnection);
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
                startDebugMode();
                return true;
            case R.id.enterSickbayIP:
                enterSickbayIP();
                return true;
            case R.id.enterSickbayBedID:
                enterSickbayBedID();
                return true;
            case R.id.clearScan:
                clearScan();
                return true;
            default:
                return false;
        }
    }

    //TO DO
    private void startDebugMode() {
        //Create a DebugMode object (extends BLETattooDevice?) that acts as BLE device
        connectToDevices(new ArrayList<String> (Arrays.asList(DEBUG_MODE_ADDRESS)));
    }

    private void clearScan() {
        scanResults.clear();
        mIconManager.clearAllIcons();
        EventBus.getDefault().post(new RequestBLEClearScanListEvent());
    }

    private void connectToDevices(ArrayList<String> addresses) {
        EventBus.getDefault().post(new RequestBLEConnectEvent(addresses));
        startGraphActivity();
    }

    private String sickbayIP = "";
    private String sickbayBedID = "";
    private void enterSickbayIP() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(input);

        builder.setTitle("Set Sickbay IP");
        builder.setMessage("Enter the IP address of the Sickbay server to connect to.");
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sickbayIP = input.getText().toString();

                Log.d(TAG, "Sickbay IP set to " + sickbayIP);

                //Write to file
                writeSickbaySettings("sickbayIP", sickbayIP);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();


    }

    private void enterSickbayBedID() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(input);

        builder.setTitle("Set Sickbay Bed ID");
        builder.setMessage("Enter the Sickbay Bed ID.");
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sickbayBedID = input.getText().toString();

                Log.d(TAG, "Sickbay Bed ID set to " + sickbayBedID);

                //Write to file
                writeSickbaySettings("sickbayBedID", sickbayBedID);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }

    //Very bad, doesn't check if number or if folder exists
    private static final String BASE_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Pulse_Data";
    private void writeSickbaySettings(String fileName, String data) {
        Log.d(TAG, "Writing Sickbay setting " + fileName + " " + data);
        String filePath = BASE_DIR_PATH + File.separator + fileName + ".txt";

        java.io.FileWriter mFileWriter;

        try {
            mFileWriter = new java.io.FileWriter(filePath, false);
            mFileWriter.write(data);
            mFileWriter.close();
            Log.d(TAG, "SUCCESSFUL WRITE");
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        } finally {

        }
    }

    private void startGraphActivity() {
        Log.d(TAG, "Starting Graph Activity");
        Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateScanList(ScanListUpdatedEvent event) {
        Map<String, BluetoothDevice> scanList = event.getScanResults();
        Map<String, Integer> rssis = event.getRSSIs();
        Map<String, Boolean> isInitialized = event.getIsInitialized();

        for(String address : scanResults.keySet()) {
            //If no longer available, remove.
            if(!scanList.containsKey(address)) {
                scanResults.remove(address);
                mIconManager.removeIcon(address);
            }
        }
        for(String address : scanList.keySet()) {
            //If not already in the local scan results, add it
            if (!scanResults.containsKey(address)) {
                scanResults.put(address, scanList.get(address));
                mIconManager.generateNewIcon(this,
                        scanList.get(address).getName(),
                        address,
                        rssis.get(address),
                        R.drawable.ic_bluetooth_black_24dp,
                        isInitialized.get(address));
            }

            //Update the RSSIs
            mIconManager.updateRSSI(address, rssis.get(address));
        }
    }

    ValueAnimator connectAlpha = ValueAnimator.ofInt(255, 0);


    @Subscribe
    public void updateNumDevicesSelected(BLEIconNumSelectedChangedEvent event) {
        int numSelected = event.getNumDevicesSelected();

        connectAlpha.cancel();

        if (numSelected == 0) {
            //Fade button out
            connectAlpha.setDuration(1000);
            connectAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    mBinding.btnStartBleConnection.getBackground().setAlpha((int) connectAlpha.getAnimatedValue());
                    mBinding.btnStartBleConnection.setTextColor((mBinding.btnStartBleConnection.getTextColors().withAlpha((int) connectAlpha.getAnimatedValue())));
                }
            });
            connectAlpha.start();

            //When finished fading, make it untouchable
            connectAlpha.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mBinding.btnStartBleConnection.setVisibility(View.GONE);
                }
            });

            connectButtonVisible = false;
        } else {
            if(!connectButtonVisible) {
                mBinding.btnStartBleConnection.setVisibility(View.VISIBLE);
                connectButtonVisible = true;
                //Fade button in
                ValueAnimator connectAlpha = ValueAnimator.ofInt(0, 255);
                connectAlpha.setDuration(1000);
                connectAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mBinding.btnStartBleConnection.getBackground().setAlpha((int) connectAlpha.getAnimatedValue());
                        mBinding.btnStartBleConnection.setTextColor((mBinding.btnStartBleConnection.getTextColors().withAlpha((int) connectAlpha.getAnimatedValue())));
                    }
                });
                connectAlpha.start();
            }

            if (numSelected == 1)
                mBinding.btnStartBleConnection.setText("Connect (" + numSelected + " device)");
            else
                mBinding.btnStartBleConnection.setText("Connect (" + numSelected + " devices)");
        }
    }

    private void startScan() {
        mBinding.layout1.getBackground().setAlpha(0);

        EventBus.getDefault().post(new RequestBLEStartScanEvent());

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
        EventBus.getDefault().post(new RequestBLEStopScanEvent());
        //valueAnimator.cancel();
        mScanning = false;
        for(Pulse pulse: pulses) {
            pulse.end();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Log.d("Permission", "onRequestPermissionsResult: "+ grantResults[0] + grantResults[1]);
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
                //Toast.makeText(this, "????????", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private boolean hasPermissions() {
        if(bleHandlerService != null && !bleHandlerService.hasBLEPermissions()) {
            requestBluetoothEnable();
            return false;
        }

        int write_external_storage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int location_access = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return (write_external_storage == PackageManager.PERMISSION_GRANTED) && (location_access == PackageManager.PERMISSION_GRANTED);
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "Requested user enabled Bluetooth. Try starting the scan again.");
    }

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
        mBinding.btnStartBleConnection.setVisibility(View.GONE);

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

        mBinding.btnStartBleConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
                connectToDevices(mIconManager.getSelectedAddresses());
            }
        });
    }
}