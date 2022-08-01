package tan.philip.nrf_ble.BLE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import tan.philip.nrf_ble.GraphScreen.Filter;

//Definitely rename this once I have a better idea of what it does lol
//Should I combine this with GraphSignal?
public class SignalSetting implements Serializable {
    public byte index;          //This is used when putting data into the output ArrayList.
    public String name;
    public byte bytesPerPoint;
    public int fs;
    public byte bitResolution;
    public boolean signed;
    public boolean littleEndian = true;

    //Optional settings
    //Graph settings
    public boolean graphable = false;
    public int[] color = null;
    //Filter settings
    public Filter filter = null;
    //Digital display settings
    public boolean digitalDisplay = false;
    public String decimalFormat = null;
    public String conversion = null;
    public String prefix = null;
    public String suffix = null;
    //Sickbay settings
    public int sickbayID = -1;

    public SignalSetting(byte index, String name, byte bytesPerPoint, int fs, byte resolution, boolean signed) {
        this.index = index;
        this.name = name;
        this.bytesPerPoint = bytesPerPoint;
        this.fs = fs;
        this.bitResolution = resolution;
        this.signed = signed;
    }
}