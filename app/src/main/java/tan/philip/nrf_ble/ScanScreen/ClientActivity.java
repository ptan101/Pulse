package tan.philip.nrf_ble.ScanScreen;

import android.Manifest;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;

import tan.philip.nrf_ble.BLE.BLEHandlerService;
import tan.philip.nrf_ble.NotificationHandler;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity;
import tan.philip.nrf_ble.databinding.ActivityClientBinding;

import static tan.philip.nrf_ble.NotificationHandler.createNotificationChannel;
import static tan.philip.nrf_ble.NotificationHandler.makeNotification;

public class ClientActivity extends AppCompatActivity {
    //private ImageButton btnScan;

    private static final String TAG = "ClientActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;
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
                case BLEHandlerService.MSG_BT_DEVICES:
                    //The service is sending the list of Bluetooth devices
                    ArrayList<String> bluetoothAddresses = (ArrayList<String>)msg.getData().getSerializable("btAddresses");
                    int newNumDevices = bluetoothAddresses.size();
                    if(newNumDevices > numDevicesFound) {
                        foundDevice(newNumDevices);
                    }
                    break;
                case BLEHandlerService.MSG_CHECK_PERMISSIONS:
                    requestBluetoothEnable();
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
                sendMessageToService(BLEHandlerService.MSG_CLEAR_SCAN);
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
            finish();
            return;
        }

        //Check if device supports BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }

        stopScan();

        //Clear scan results
        numDevicesFound = 0;
        sendMessageToService(BLEHandlerService.MSG_CLEAR_SCAN);


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
    public void viewScanList(View view) {
        Intent intent = new Intent(this, ScanResultsActivity.class);
        //intent.putExtra(EXTRA_BT_SCAN_RESULTS, (HashMap)mScanResults);
        startActivity(intent);
    }

    private void startScan() {
        mBinding.layout1.getBackground().setAlpha(0);

        sendMessageToService(BLEHandlerService.MSG_CLEAR_SCAN);
        numDevicesFound = 0;
        sendMessageToService(BLEHandlerService.MSG_START_SCAN);

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
        sendMessageToService(BLEHandlerService.MSG_STOP_SCAN);
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

    private boolean hasPermissions() {

//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            requestBluetoothEnable();
//            Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Adapter not enabled", Toast.LENGTH_LONG);
//            toast.show();
//            return false;
//
//        } else
        sendMessageToService(BLEHandlerService.MSG_CHECK_BT_ENABLED);

        if (!hasLocationPermissions()) {
            requestLocationPermission();
            Toast toast = Toast.makeText(getApplicationContext(), "Please enable location permissions", Toast.LENGTH_LONG);
            toast.show();
            return false;
        }

        return true;
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "Requested user enables Bluetooth. Try starting the scan again.");
    }
    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
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

        /*
        final float[] from = new float[3],
                to =   new float[3];

        //Color.colorToHSV(Color.parseColor("#FF2c2a3a"), from);   // from blue
        Color.colorToHSV(Color.parseColor("#00FFFFFF"), from);      //Transparent
        Color.colorToHSV(Color.parseColor("#FFFFFFFF"), to);     // to opaque

        valueAnimator = ValueAnimator.ofFloat(0, 1);                  // animate from 0 to 1
        valueAnimator.setDuration(4000);

        final float[] hsv  = new float[3];                  // transition color
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                // Transition along each axis of HSV (hue, saturation, value)
                hsv[0] = from[0] + (to[0] - from[0])*animation.getAnimatedFraction();
                hsv[1] = from[1] + (to[1] - from[1])*animation.getAnimatedFraction();
                hsv[2] = from[2] + (to[2] - from[2])*animation.getAnimatedFraction();

                mBinding.layout1.setBackgroundColor(Color.HSVToColor(hsv));
            }
        });

         */
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