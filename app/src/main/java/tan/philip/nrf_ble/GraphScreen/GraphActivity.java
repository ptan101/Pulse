package tan.philip.nrf_ble.GraphScreen;

import static tan.philip.nrf_ble.Constants.convertDpToPixel;
import static tan.philip.nrf_ble.Constants.convertPixelsToDp;

import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.BLETattooDevice;
import tan.philip.nrf_ble.BLE.BLEHandlerService;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.Events.GATTConnectionChangedEvent;
import tan.philip.nrf_ble.Events.PlotDataEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestBLEDisconnectEvent;
import tan.philip.nrf_ble.GraphScreen.UIComponents.GraphContainer;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanScreen.ClientActivity;

public class GraphActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private GraphRenderer renderer;
    private BLEHandlerService bleHandlerService;
    private boolean mIsBound = false;
    private HashMap<String, TextView> deviceStates;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        invalidateOptionsMenu();

        //Bind BLEHandlerService
        Intent intent = new Intent(GraphActivity.this, BLEHandlerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //Register activity on EventBus
        EventBus.getDefault().register(this);

        //UI Setup
        //setupButtons();


        //biometrics = (BiometricsSet)extras.getSerializable(EXTRA_BIOMETRIC_SETTINGS_IDENTIFIER);
        //setupBiometricsDigitalDisplay();
        //notification_frequency = extras.getFloat(EXTRA_NOTIF_F_IDENTIFIER);
        //notification_period = 1000 / notification_frequency;
        //rxMessages = (ArrayList<TattooMessage>)extras.getSerializable(EXTRA_RX_MESSAGES_IDENTIFIER);
        //txMessages = (ArrayList<TattooMessage>)extras.getSerializable(EXTRA_TX_MESSAGES_IDENTIFIER);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        renderer.deregister();

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

        renderer = new GraphRenderer(this, bleDevices);

        LinearLayout layout = findViewById(R.id.graph_space_layout);
        renderer.addGraphContainersToView(layout, this);
    }

    private void setupDeviceStateTexts() {
        deviceStates = new HashMap<>();
        ArrayList<BLEDevice> bleDevices = bleHandlerService.getBLEDevices();
        ConstraintLayout layout = findViewById(R.id.graphHeader);
        View lastView = findViewById(R.id.graphHeaderText);

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
                state = state.substring(0, 1+state.indexOf(")")) + " connected.";
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

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void setDeviceConnectionStateText(String connectionText, int deviceIndex) {

    }
}