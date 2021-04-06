package tan.philip.nrf_ble.BLE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

//Definitely rename this once I have a better idea of what it does lol
public class SignalSetting implements Serializable {
    public int index;          //This is used when putting data into the output ArrayList.
    public String name;
    public int bytesPerPoint;
    public int fs;

    //Optional settings
    public boolean graphable = false;
    public int[] color = null;
    public boolean digitalDisplay = false;

    public SignalSetting(int index, String name, int bytesPerPoint, int fs) {
        this.index = index;
        this.name = name;
        this.bytesPerPoint = bytesPerPoint;
        this.fs = fs;
    }

//    private void writeObject(ObjectOutputStream oos)
//            throws IOException {
//        oos.defaultWriteObject();
//        oos.writeObject(name);
//    }
//
//    private void readObject(ObjectInputStream ois)
//            throws ClassNotFoundException, IOException {
//        ois.defaultReadObject();
//        this.name = (String) ois.readObject();
//    }
}