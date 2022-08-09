package tan.philip.nrf_ble.BLE.Gatt;

import java.util.ArrayList;

import tan.philip.nrf_ble.BLE.Gatt.operations.GattOperation;

public class GattOperationBundle {
    final ArrayList<GattOperation> operations;

    public GattOperationBundle() {
        operations = new ArrayList<>();
    }

    public void addOperation(GattOperation operation) {
        operations.add(operation);
        operation.setBundle(this);
    }

    public ArrayList<GattOperation> getOperations() {
        return operations;
    }
}