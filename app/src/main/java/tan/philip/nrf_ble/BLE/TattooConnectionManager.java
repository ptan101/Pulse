package tan.philip.nrf_ble.BLE;

import static tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice.DEBUG_MODE_ADDRESS;
import static tan.philip.nrf_ble.Constants.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID;
import static tan.philip.nrf_ble.Constants.NUS_TX_UUID;
import static tan.philip.nrf_ble.Constants.NUS_UUID;
import static tan.philip.nrf_ble.Constants.TMS_RX_UUID;
import static tan.philip.nrf_ble.Constants.TMS_TX_UUID;
import static tan.philip.nrf_ble.Constants.TMS_UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.UUID;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.BLETattooDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice;
import tan.philip.nrf_ble.BLE.Gatt.CharacteristicChangeListener;
import tan.philip.nrf_ble.BLE.Gatt.GattManager;
import tan.philip.nrf_ble.BLE.Gatt.operations.GattCharacteristicWriteOperation;
import tan.philip.nrf_ble.BLE.Gatt.operations.GattDisconnectOperation;
import tan.philip.nrf_ble.BLE.Gatt.operations.GattSetNotificationOperation;
import tan.philip.nrf_ble.Events.GATTConnectionChangedEvent;
import tan.philip.nrf_ble.Events.GATTServicesDiscoveredEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestSendTMSEvent;

public class TattooConnectionManager {
    //Credit to Nordic Puck Manager
    private static final String TAG = "TattooConnectionManager";

    private final Context mCtx;
    private final ArrayList<BLEDevice> mConnectedDevices; //Should use HashMap
    private int uniqueId = 0;

    GattManager mGattManager;

    public TattooConnectionManager(Context ctx, GattManager gattManager) {
        mConnectedDevices = new ArrayList<>();
        mGattManager = gattManager;
        mCtx = ctx;

        //Register on EventBus
        EventBus.getDefault().register(this);

        mGattManager.addCharacteristicChangeListener( TMS_TX_UUID,
                new CharacteristicChangeListener() {
                    @Override
                    public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic) {
                        BLEDevice device = null;
                        for (BLEDevice t : mConnectedDevices) {
                            if (t.getAddress().equals(deviceAddress)) {
                                device = t;
                                break;
                            }
                        }

                        if (device == null) {
                            return;
                        }

                        byte[] messageBytes = characteristic.getValue();

                        ((BLETattooDevice) device).processCUSPacket(messageBytes);
                    }
                }
        );

        mGattManager.addCharacteristicChangeListener( NUS_TX_UUID,
                new CharacteristicChangeListener() {
                    @Override
                    public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic) {
                        BLEDevice device = null;
                        for (BLEDevice t : mConnectedDevices) {
                            if (t.getAddress().equals(deviceAddress)) {
                                device = t;
                                break;
                            }
                        }

                        if (device == null) {
                            return;
                        }

                        byte[] messageBytes = characteristic.getValue();

                        ((BLETattooDevice) device).processNUSPacket(messageBytes);
                    }
                }
        );
    }

    public synchronized void checkAndConnectToTattoo(final BLETattooDevice tattoo) {
        if (mConnectedDevices.contains(tattoo)) { //|| !tattoo.getServiceUUIDs().contains(NUS_UUID)) {
            return;
        }

        Log.i(TAG, "Found Tattoo. Initiating gatt subscribe to " + tattoo);

        //Check if the same tattoo name already exists. If so, assign a different instance id.
        int instanceId = 0;
        for(BLEDevice d : mConnectedDevices)
            if(d.getDisplayName().equals(tattoo.getDisplayName()))
                instanceId ++;

        tattoo.setInstanceId(instanceId);
        tattoo.setUniqueId(uniqueId++);
        mConnectedDevices.add(tattoo);

        if(tattoo.getAddress().equals(DEBUG_MODE_ADDRESS))
            return;

        subscribeToNotification(tattoo.getBluetoothDevice(), NUS_UUID, NUS_TX_UUID);
    }

    public synchronized void disconnectTattoo(String address) {
        for (BLEDevice d : mConnectedDevices) {
            if (d.getAddress().equals(address)) {
                d.unregister();
                mConnectedDevices.remove(d);
                if(d.getAddress().equals(DEBUG_MODE_ADDRESS)) {
                    DebugBLEDevice dDebug = (DebugBLEDevice)d;
                    dDebug.endDebugMode();
                    return;
                }

                mGattManager.deregisterDevice(address);
                mGattManager.queue(new GattDisconnectOperation(d.getBluetoothDevice()));
                break;
            }
        }
    }

    public void unregister() {
        //Unregister from EventBus
        EventBus.getDefault().unregister(this);

        for(BLEDevice d : mConnectedDevices)
            d.unregister();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void connectionStateChanged(GATTConnectionChangedEvent event) {
        for (BLEDevice device : mConnectedDevices) {
            if (device.getAddress().equals(event.getAddress())) {
                if (event.getNewState() == BluetoothProfile.STATE_DISCONNECTED || event.getNewState() == 133) {
                    device.setConnected(false);
                    //L.i("Disconnected from " + bundle.mAddress + ". Removing from subscribed list.");
                    //mConnectedDevices.remove(device);
                } else if (event.getNewState() == BluetoothProfile.STATE_CONNECTED) {
                    device.setConnected(true);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void servicesDiscovered(GATTServicesDiscoveredEvent event) {
        for (BLEDevice device : mConnectedDevices) {
            if (device.getBluetoothDevice().equals(event.getDevice())) {
                device.setServiceUUIDs(event.getGATTServiceUUIDs());

                subscribeToNotification(device.getBluetoothDevice(), NUS_UUID, NUS_TX_UUID);

                //If the device supports TMS, subscribe to this service.
                if (event.getGATTServiceUUIDs().contains(TMS_UUID))
                    subscribeToNotification(device.getBluetoothDevice(), TMS_UUID, TMS_TX_UUID);
            }
        }
    }

    private void subscribeToNotification(BluetoothDevice device, UUID serviceUUID, UUID characteristicUUID) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                mGattManager.queue(new GattSetNotificationOperation(
                        device,
                        serviceUUID,
                        characteristicUUID,
                        CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));

                // Android BLE stack might have issues connecting to
                // multiple Gatt services right after another.
                // See: http://stackoverflow.com/questions/21237093/android-4-3-how-to-connect-to-multiple-bluetooth-low-energy-devices
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void sendByte(RequestSendTMSEvent event) {
            mGattManager.queue(new GattCharacteristicWriteOperation(
                    event.getBleDevice(),
                    TMS_UUID,
                    TMS_RX_UUID,
                    event.getMessageId()));
    }

    public ArrayList<BLEDevice> getBLEDevices() {
        return mConnectedDevices;
    }


}
