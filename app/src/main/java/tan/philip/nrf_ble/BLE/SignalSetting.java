package tan.philip.nrf_ble.BLE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import tan.philip.nrf_ble.GraphScreen.Filter;

//Definitely rename this once I have a better idea of what it does lol
public class SignalSetting implements Serializable {
    public byte index;          //This is used when putting data into the output ArrayList.
    public String name;
    public byte bytesPerPoint;
    public int fs;
    public byte bitResolution;
    public boolean signed;

    //Optional settings
    public boolean graphable = false;
    public int[] color = null;
    public boolean digitalDisplay = false;
    public Filter filter = null;

    public SignalSetting(byte index, String name, byte bytesPerPoint, int fs, byte resolution, boolean signed) {
        this.index = index;
        this.name = name;
        this.bytesPerPoint = bytesPerPoint;
        this.fs = fs;
        this.bitResolution = resolution;
        this.signed = signed;
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