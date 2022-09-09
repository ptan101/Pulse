package tan.philip.nrf_ble.GraphScreen;

import static tan.philip.nrf_ble.Constants.NUS_TX_UUID;
import static tan.philip.nrf_ble.Constants.NUS_UUID;
import static tan.philip.nrf_ble.Constants.TMS_TX_UUID;
import static tan.philip.nrf_ble.Constants.TMS_UUID;
import static tan.philip.nrf_ble.Constants.convertDpToPixel;

import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.databinding.DataBindingUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.BLEHandlerService;
import tan.philip.nrf_ble.BLE.PacketParsing.TattooMessage;
import tan.philip.nrf_ble.Events.GATTConnectionChangedEvent;
import tan.philip.nrf_ble.Events.GATTServicesDiscoveredEvent;
import tan.philip.nrf_ble.Events.PlotDataEvent;
import tan.philip.nrf_ble.Events.TMSMessageReceivedEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEDisconnectEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestSendTMSEvent;
import tan.philip.nrf_ble.FileWriting.FileManager;
import tan.philip.nrf_ble.GraphScreen.UIComponents.OptionsMenu;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.databinding.ActivityGraphBinding;

public class GraphActivity extends AppCompatActivity {
    private GraphRenderer renderer;
    private BLEHandlerService bleHandlerService;
    private boolean mIsBound = false;
    private HashMap<String, TextView> deviceStates;
    private OptionsMenu menu;
    private ActivityGraphBinding mBinding;
    private FileManager fileManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_graph);

        //Bind BLEHandlerService
        Intent intent = new Intent(GraphActivity.this, BLEHandlerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //Register activity on EventBus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().post(new RequestBLEDisconnectEvent());

        renderer.deregister();
        fileManager.deregister();

        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
        unbindService(mConnection);

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Terminating connection")
            .setMessage("Are you sure you want to terminate the BLE connection?")
            .setNegativeButton("No", null)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EventBus.getDefault().post(new RequestBLEDisconnectEvent());
                    finish();
                }

            })
            .show();
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            BLEHandlerService.LocalBinder binder = (BLEHandlerService.LocalBinder) service;
            bleHandlerService = binder.getService();
            mIsBound = true;

            setupGraphs();
            setupDeviceStateTexts();
        }

        public void onServiceDisconnected(ComponentName className) { mIsBound = false; }
    };

    private void setupGraphs() {
        ArrayList<BLEDevice> bleDevices = bleHandlerService.getBLEDevices();

        //If no available devices, close activity.
        if(bleDevices.size() == 0)
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("No devices available to connect.")
                    .setMessage("None of the BLE devices could connect. Please try again (ensure that .init files are set up correctly).")
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();

        renderer = new GraphRenderer(this, bleDevices, mBinding.graphRecordStopwatch, mBinding.graphDigitaldisplayLayout);

        LinearLayout layout = mBinding.graphSpaceLayout;
        renderer.addGraphContainersToView(layout, this);
        fileManager = new FileManager(bleDevices);

        menu = new OptionsMenu(this, mBinding.graphOptionsButton, fileManager, bleDevices);
        invalidateOptionsMenu();
    }

    private void setupDeviceStateTexts() {
        deviceStates = new HashMap<>();
        ArrayList<BLEDevice> bleDevices = bleHandlerService.getBLEDevices();
        ConstraintLayout layout = mBinding.graphHeader;
        View lastView = mBinding.graphHeaderText;

        for (BLEDevice device : bleDevices) {
            TextView text = new TextView(this);
            text.setText(device.getBluetoothIdentifier() + " connecting...");
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
            text.setId(View.generateViewId());

            deviceStates.put(device.getAddress(), text);
            layout.addView(text);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(layout);

            constraintSet.connect(text.getId(), ConstraintSet.TOP, lastView.getId(), ConstraintSet.BOTTOM, (int) convertDpToPixel(10, this));
            constraintSet.centerHorizontally(text.getId(), layout.getId());
            constraintSet.applyTo(layout);

            lastView = text;
        }

        //Make the recording timer below the connection texts
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(layout);

        constraintSet.connect(mBinding.graphRecordStopwatch.getId(), ConstraintSet.TOP, lastView.getId(), ConstraintSet.BOTTOM, (int) convertDpToPixel(10, this));
        constraintSet.applyTo(layout);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public synchronized void displayData(PlotDataEvent event) {
        HashMap<Integer, ArrayList<Float>> newData = event.getFilteredData();
        renderer.queueDataPoints(event.getDeviceAddress(), newData);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public synchronized void onConnectionChanged(GATTConnectionChangedEvent event) {
        TextView text = deviceStates.get(event.getAddress());
        String state = text.getText().toString();
        switch(event.getNewState()) {
            case BluetoothProfile.STATE_CONNECTED:
                state = state.substring(0, 1+state.indexOf(")")) + " connecting...";
                text.setText(state);
                text.setTextColor(Color.GRAY);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                state = state.substring(0, 1+state.indexOf(")")) + " disconnected.";
                text.setTextColor(Color.RED);
                text.setText(state);
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void servicesDiscovered(GATTServicesDiscoveredEvent event) {
        TextView text = deviceStates.get(event.getDevice().getAddress());
        String state = text.getText().toString();
        state = state.substring(0, 1+state.indexOf(")")) + " connected.";
        text.setText(state);
        text.setTextColor(Color.GRAY);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public synchronized void onTMSReceived(TMSMessageReceivedEvent event) {
        TattooMessage message = event.getMessage();

        if(message == null)
            return;

        //If the message should appear as an alert dialog, display.
        if(message.isAlertDialog())
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("New Message from Tattoo")
                    .setMessage(message.getMainMessage())
                    .setPositiveButton("Close", null)
                    .show();

        //Change the header text if required
        String brief = message.getBrief();
        TextView text = deviceStates.get(event.getAddress());
        String state = text.getText().toString();
        state = state.substring(0, 1+state.indexOf(")")) + " connected.";
        if (brief != null)
            state = state.substring(0, 1+state.indexOf(")")) + brief;
        text.setText(state);
        text.setTextColor(Color.GRAY);

        //If an automatic response is required, send the response.
        if(message.getAutoTXMessage() >= 0) {
            ArrayList<BLEDevice> bleDevices = bleHandlerService.getBLEDevices();
            for(BLEDevice d: bleDevices) {
                if(d.getAddress().equals(event.getAddress())) {
                    EventBus.getDefault().post(new RequestSendTMSEvent(d.getBluetoothDevice(),
                            new byte[] {(byte)message.getAutoTXMessage()}));

                }
            }
        }
    }
}