package tan.philip.nrf_ble.BLE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

//Definitely rename this once I have a better idea of what it does lol
public class SignalSetting implements Serializable {
    public int index;          //This is used when putting data into the output ArrayList.
    public String name;
    public int bytesPerPoint;
    public int fs;
    public int[] color;

    public SignalSetting(int index, String name, int bytesPerPoint, int fs, int[] color) {
        this.index = index;
        this.name = name;
        this.bytesPerPoint = bytesPerPoint;
        this.fs = fs;
        this.color = color;
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