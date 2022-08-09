package tan.philip.nrf_ble.BLE;

import static tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice.DEBUG_MODE_ADDRESS;
import static tan.philip.nrf_ble.Constants.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID;
import static tan.philip.nrf_ble.Constants.NUS_TX_UUID;
import static tan.philip.nrf_ble.Constants.NUS_UUID;
import static tan.philip.nrf_ble.Constants.TMS_TX_UUID;

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
import java.util.List;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.BLETattooDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.DebugBLEDevice;
import tan.philip.nrf_ble.BLE.Gatt.CharacteristicChangeListener;
import tan.philip.nrf_ble.BLE.Gatt.GattManager;
import tan.philip.nrf_ble.BLE.Gatt.operations.GattDisconnectOperation;
import tan.philip.nrf_ble.BLE.Gatt.operations.GattSetNotificationOperation;
import tan.philip.nrf_ble.Events.GATTServicesDiscoveredEvent;
import tan.philip.nrf_ble.Events.NUSPacketRecievedEvent;
import tan.philip.nrf_ble.Events.TMSPacketRecievedEvent;

public class TattooConnectionManager {
    private static final String TAG = "TattooConnectionManager";

    private final Context mCtx;
    private ArrayList<BLEDevice> mConnectedDevices;

    GattManager mGattManager;

    public TattooConnectionManager(Context ctx, GattManager gattManager) {
        mConnectedDevices = new ArrayList<>();
        mGattManager = gattManager;
        mCtx = ctx;

        //Register on EventBus
        EventBus.getDefault().register(this);

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

                        //Data is sensor data (from NUS)
                        //Unfortunately switch case does not work with objects
                        if(characteristic.getUuid().equals(NUS_TX_UUID)) {
                            EventBus.getDefault().post(new NUSPacketRecievedEvent((BLETattooDevice) device, messageBytes));
                        } else if (characteristic.getUuid().equals(TMS_TX_UUID)) {
                            EventBus.getDefault().post(new TMSPacketRecievedEvent(device.getTMSMessage(messageBytes[0])));
                        }
                    }
                }
        );
    }

    public synchronized void checkAndConnectToTattoo(final BLETattooDevice tattoo) {
        if (mConnectedDevices.contains(tattoo)) { //|| !tattoo.getServiceUUIDs().contains(NUS_UUID)) {
            return;
        }

        Log.i(TAG, "Found Tattoo. Initiating gatt subscribe to " + tattoo);
        mConnectedDevices.add(tattoo);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                if(tattoo.getAddress().equals(DEBUG_MODE_ADDRESS))
                    return;

                BluetoothDevice device = tattoo.getBluetoothDevice();
                mGattManager.queue(new GattSetNotificationOperation(
                        device,
                        NUS_UUID,
                        NUS_TX_UUID,
                        CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));

                /*
                //TO DO: If the tattoo supports Tattoo Messaging Service, subscribe to notifications
                if (tattoo.getServiceUUIDs().contains(TMS_UUID)) {
                    mGattManager.queue(new GattSetNotificationOperation(
                            device,
                            TMS_UUID,
                            TMS_TX_UUID,
                            CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));
                }

                 */

                // Android BLE stack might have issues connecting to
                // multiple Gatt services right after another.
                // See: http://stackoverflow.com/questions/21237093/android-4-3-how-to-connect-to-multiple-bluetooth-low-energy-devices
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 100);
    }

    public synchronized void disconnectTattoo(String address) {
        for (BLEDevice d : mConnectedDevices) {
            if (d.getAddress().equals(address)) {
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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void connectionStateChanged(GattManager.ConnectionStateChangedBundle bundle) {
        for (BLEDevice device : mConnectedDevices) {
            if (device.getAddress().equals(bundle.mAddress)) {
                if (bundle.mNewState == BluetoothProfile.STATE_DISCONNECTED || bundle.mNewState == 133) {
                    //L.i("Disconnected from " + bundle.mAddress + ". Removing from subscribed list.");
                    //mConnectedDevices.remove(device);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void servicesDiscovered(GATTServicesDiscoveredEvent event) {
        for (BLEDevice device : mConnectedDevices) {
            if (device.getBluetoothDevice().equals(event.getDevice())) {
               mGattManager.queue(new GattSetNotificationOperation(
                    event.getDevice(),
                    NUS_UUID,
                    NUS_TX_UUID,
                    CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));
            }
        }
    }

    public ArrayList<BLEDevice> getBLEDevices() {
        return mConnectedDevices;
    }


}
