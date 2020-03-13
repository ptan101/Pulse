package tan.philip.nrf_ble.ScanScreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity;
import tan.philip.nrf_ble.databinding.ActivityClientBinding;

import static tan.philip.nrf_ble.Constants.SCAN_PERIOD;
import static tan.philip.nrf_ble.Constants.SERVICE_UUID;


public class ClientActivity extends AppCompatActivity {

    //private ImageButton btnScan;

    private static final String TAG = "ClientActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;
    public static final String EXTRA_BT_SCAN_RESULTS = "scan results";

    private ActivityClientBinding mBinding;

    private boolean mScanning = false;
    private Handler mHandler;
    private Handler mLogHandler;
    private Map<String, BluetoothDevice> mScanResults;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;

    //Color scan background
    private final int GREY = 0xFF2C2C2C;//2c2a3a;
    private ValueAnimator valueAnimator;
    private ArrayList<Pulse> pulses;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_client);
        this.getSupportActionBar().hide();

        mBinding.btnListDevices.setVisibility(View.GONE);

        //Set up background color transition
        setupBackgroundTransition();

        setupBLE();

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
                viewScanList(view);
            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mHandler != null)
            mHandler.removeCallbacksAndMessages(null);

        stopScan();
    }
    protected void onResume() {
        super.onResume();

        mBinding.btnListDevices.setVisibility(View.GONE);
        //mBinding.layout1.setBackgroundColor(0xFF2c2a3a);
        mBinding.layout1.setBackgroundColor(GREY);
        mHandler = new Handler();

        if (!hasPermissions()) {
            finish();
            return;
        }

        //Check if device supports BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupBLE() {
        //Get Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //Enable BT
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void startScan() {
        if (!hasPermissions() || mScanning) {
            return;
        }

        mBinding.btnListDevices.setVisibility(View.GONE);

        //Display message
        Toast toast = Toast.makeText(getApplicationContext(), "Scanning for BLE devices...", Toast.LENGTH_SHORT);
        toast.show();

        //Change background color
        valueAnimator.start();
        for(Pulse currentPulse: pulses) {
            currentPulse.restart();
        }


        //BLE code
        //Return all broadcasting Bluetooth devices
        //EF:C6:E7:96:D4:D6
        //21:A8:58:EF:95:27
        List<ScanFilter> filters = new ArrayList<>();


        ScanFilter scanFilter = new ScanFilter.Builder()
                //.setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .setDeviceAddress("EF:C6:E7:96:D4:D6")
                .build();
        filters.add(scanFilter);




        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
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
                if(mScanResults.isEmpty())
                    mHandler.postDelayed(this, 200);
                else
                    stopScan();
            }
        }, 200);
    }

    private void stopScan() {
        //valueAnimator.cancel();
        if(mHandler != null)
            mHandler.removeCallbacksAndMessages(null);

        for(Pulse pulse: pulses) {
            pulse.end();
        }


        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            //Display message
            //Toast toast = Toast.makeText(getApplicationContext(), "Scan complete", Toast.LENGTH_LONG);
            //toast.show();

            scanComplete();
        }

        mScanCallback = null;
        mScanning = false;
        mHandler = null;

    }

    private void scanComplete() {
        if (mScanResults.isEmpty()) {
            Toast toast = Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT);
            toast.show();
            return;
        } else {
            mBinding.btnListDevices.setVisibility(View.VISIBLE);

            ValueAnimator newButton = ValueAnimator.ofInt(0,255);
            newButton.setDuration(1000);
            newButton.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    mBinding.btnListDevices.getBackground().setAlpha((int)newButton.getAnimatedValue());
                    mBinding.btnListDevices.setTextColor((mBinding.btnListDevices.getTextColors().withAlpha((int)newButton.getAnimatedValue())));
                }
            });
            newButton.start();
        }

        for (String deviceAddress : mScanResults.keySet()) {
            Log.d(TAG, "Found device: " + deviceAddress);
        }
    }

    private boolean hasPermissions() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
            Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Adapter not enabled", Toast.LENGTH_LONG);
            toast.show();
            return false;

        } else if (!hasLocationPermissions()) {
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
                    Log.d(TAG, deviceAddress + " " + uuidString);

                    if (!serviceList.contains(serviceUUID))
                        serviceList.add(serviceUUID);
                }
            }

            return serviceList;
        }
    };

    private void setupBackgroundTransition() {
        final float[] from = new float[3],
                to =   new float[3];

        //Color.colorToHSV(Color.parseColor("#FF2c2a3a"), from);   // from blue
        Color.colorToHSV(Color.parseColor("#FF2C2C2C"), from);
        Color.colorToHSV(Color.parseColor("#FFFFFFFF"), to);     // to white

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
    }

    public void viewScanList(View view) {
        mHandler = new Handler();
        mHandler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(this, ScanResultsActivity.class);
        intent.putExtra(EXTRA_BT_SCAN_RESULTS, (HashMap)mScanResults);
        startActivity(intent);
    }
}