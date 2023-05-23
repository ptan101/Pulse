package tan.philip.nrf_ble.BLE.Gatt;

import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import tan.philip.nrf_ble.BLE.Gatt.operations.GattCharacteristicReadOperation;
import tan.philip.nrf_ble.BLE.Gatt.operations.GattDescriptorReadOperation;
import tan.philip.nrf_ble.BLE.Gatt.operations.GattOperation;
import tan.philip.nrf_ble.Events.GATTConnectionChangedEvent;
import tan.philip.nrf_ble.Events.GATTServicesDiscoveredEvent;

//Credit to Nordic Puck Manager
public class GattManager {
    private static final String TAG = "GATTManager";
    private final ConcurrentLinkedQueue<GattOperation> mQueue;
    private final ConcurrentHashMap<String, BluetoothGatt> mGatts;
    private final List<String> mAddressesToDeregister;
    private GattOperation mCurrentOperation;
    private final HashMap<UUID, ArrayList<CharacteristicChangeListener>> mCharacteristicChangeListeners;
    private AsyncTask<Void, Void, Void> mCurrentOperationTimeout;

    private final Context mCtx;

    public GattManager(Context ctx) {
        mQueue = new ConcurrentLinkedQueue<>();
        mGatts = new ConcurrentHashMap<>();
        mAddressesToDeregister = Collections.synchronizedList(new ArrayList<String>());
        mCurrentOperation = null;
        mCharacteristicChangeListeners = new HashMap<>();

        this.mCtx = ctx;
    }

    public synchronized void cancelCurrentOperationBundle() {
        Log.d(TAG, "Cancelling current operation. Queue size before: " + mQueue.size());
        if(mCurrentOperation != null && mCurrentOperation.getBundle() != null) {
            for(GattOperation op : mCurrentOperation.getBundle().getOperations()) {
                mQueue.remove(op);
            }
        }
        Log.d(TAG, "Queue size after: " + mQueue.size());
        mCurrentOperation = null;
        drive();
    }

    public synchronized void queue(GattOperation gattOperation) {
        mQueue.add(gattOperation);
        Log.d(TAG, "Queueing Gatt operation, size will now become: " + mQueue.size());
        drive();
    }

    private synchronized void drive() {
        if(mCurrentOperation != null) {
            Log.e(TAG, "tried to drive, but currentOperation was not null, " + mCurrentOperation);
            return;
        }
        if( mQueue.size() == 0) {
            Log.d(TAG, "Queue empty, drive loop stopped.");
            mCurrentOperation = null;
            return;
        }

        final GattOperation operation = mQueue.poll();
        Log.d(TAG, "Driving Gatt queue, size will now become: " + mQueue.size());
        setCurrentOperation(operation);


        if(mCurrentOperationTimeout != null) {
            mCurrentOperationTimeout.cancel(true);
        }
        mCurrentOperationTimeout = new AsyncTask<Void, Void, Void>() {
            @Override
            protected synchronized Void doInBackground(Void... voids) {
                try {
                    Log.d(TAG, "Starting to do a background timeout");
                    wait(operation.getTimoutInMillis());
                } catch (InterruptedException e) {
                    Log.d(TAG, "was interrupted out of the timeout");
                }
                if(isCancelled()) {
                    Log.d(TAG, "The timeout was cancelled, so we do nothing.");
                    return null;
                }
                Log.d(TAG, "Timeout ran to completion, time to cancel the entire operation bundle. Abort, abort!");
                cancelCurrentOperationBundle();
                return null;
            }

            @Override
            protected synchronized void onCancelled() {
                super.onCancelled();
                notify();
            }
        }.execute();

        final BluetoothDevice device = operation.getDevice();
        if(mGatts.containsKey(device.getAddress())) {
            execute(mGatts.get(device.getAddress()), operation);
        } else {
            device.connectGatt(mCtx, true, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);

                    EventBus.getDefault().post(new GATTConnectionChangedEvent(device.getAddress(), newState));

                    if (status == 133) {
                        Log.e(TAG, "Got the status 133 bug, closing gatt");
                        gatt.close();
                        mGatts.remove(device.getAddress());
                        return;
                    }

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Gatt connected to device " + device.getAddress());

                        if(!mGatts.containsKey(device.getAddress()))
                            mGatts.put(device.getAddress(), gatt);

                        gatt.discoverServices();
                        gatt.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from gatt server " + device.getAddress() + ", newState: " + newState);
                        setCurrentOperation(null);
                        if(mAddressesToDeregister.contains(device.getAddress())) {
                            mGatts.remove(device.getAddress());
                            gatt.close();
                        }
                        drive();
                    }
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorRead(gatt, descriptor, status);
                    ((GattDescriptorReadOperation) mCurrentOperation).onRead(descriptor);
                    setCurrentOperation(null);
                    drive();
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                    setCurrentOperation(null);
                    drive();
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    ((GattCharacteristicReadOperation) mCurrentOperation).onRead(characteristic);
                    setCurrentOperation(null);
                    drive();
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    EventBus.getDefault().post(new GATTServicesDiscoveredEvent(new ArrayList<>(gatt.getServices()), device));

                    Log.d(TAG, "services discovered, status: " + status);
                    execute(gatt, operation);
                }


                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    Log.d(TAG, "Characteristic " + characteristic.getUuid() + "written to on device " + device.getAddress());
                    setCurrentOperation(null);
                    drive();
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    //Log.d(TAG, "Characteristic " + characteristic.getUuid() + "was changed, device: " + device.getAddress());
                    if (mCharacteristicChangeListeners.containsKey(characteristic.getUuid())) {
                        for (CharacteristicChangeListener listener : mCharacteristicChangeListeners.get(characteristic.getUuid())) {
                            listener.onCharacteristicChanged(device.getAddress(), characteristic);
                        }
                    }
                }
            });
        }
    }

    private void execute(BluetoothGatt gatt, GattOperation operation) {
        if(operation != mCurrentOperation) {
            return;
        }
        operation.execute(gatt);
        if(!operation.hasAvailableCompletionCallback()) {
            setCurrentOperation(null);
            drive();
        }
    }

    public synchronized void setCurrentOperation(GattOperation currentOperation) {
        mCurrentOperation = currentOperation;
    }

    public BluetoothGatt getGatt(BluetoothDevice device) {
        return mGatts.get(device);
    }

    public void addCharacteristicChangeListener(UUID characteristicUuid, CharacteristicChangeListener characteristicChangeListener) {
        if(!mCharacteristicChangeListeners.containsKey(characteristicUuid)) {
            mCharacteristicChangeListeners.put(characteristicUuid, new ArrayList<CharacteristicChangeListener>());
        }
        mCharacteristicChangeListeners.get(characteristicUuid).add(characteristicChangeListener);
    }

    public void queue(GattOperationBundle bundle) {
        for(GattOperation operation : bundle.getOperations()) {
            queue(operation);
        }
    }

    public void deregisterDevice(String address) {
        mAddressesToDeregister.add(address);
    }

    public class ConnectionStateChangedBundle {
        public final int mNewState;
        public final String mAddress;

        public ConnectionStateChangedBundle(String address, int newState) {
            mAddress = address;
            mNewState = newState;
        }
    }
}