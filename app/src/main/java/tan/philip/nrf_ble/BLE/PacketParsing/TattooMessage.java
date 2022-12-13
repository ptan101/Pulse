package tan.philip.nrf_ble.BLE.PacketParsing;

import java.io.Serializable;

public class TattooMessage implements Serializable {
    String message;
    //Set a short descriptor for the device's current state. E.g., "calibrating."
    String brief = null;
    //Set the ID of the RX message that will be swapped after this message is sent.
    //For example, shut off should be swapped with turn on.
    int alternate = 0;
    boolean alertDialog;
    boolean isAlternate = false;
    //Set the ID of the TX message that should be automatically sent upon RX.
    //Can be used for message acknowledgments or to ensure a state (e.g., tattoo shutoff).
    //Set to 0 for no automatic TX
    int idTXMessage = 0;
    //Each menu item needs a unique id for the touch listener
    int menuId;

    public TattooMessage(String message, boolean alertDialog) {
        this.message = message;
        this.alertDialog = alertDialog;
    }

    public String getMainMessage() {
        return message;
    }

    public String getBrief() {
        return brief;
    }

    public boolean isAlertDialog() {
        return alertDialog;
    }

    public boolean isAlternate() {
        return isAlternate;
    }

    public int getAlternate() {
        return alternate;
    }

    public void setIsAlternate(boolean isAlternate) {
        this.isAlternate = isAlternate;
    }

    public int getAutoTXMessage() {
        return idTXMessage;
    }

    public int getMenuId() {
        return menuId;
    }

    public void setMenuID(int menuId) {
        this.menuId = menuId;
    }
}
