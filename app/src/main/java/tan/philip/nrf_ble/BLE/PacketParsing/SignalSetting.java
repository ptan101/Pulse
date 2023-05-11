package tan.philip.nrf_ble.BLE.PacketParsing;

import java.io.Serializable;

import tan.philip.nrf_ble.GraphScreen.UIComponents.Filter;

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
    public int graphHeight = 150;

    //Filter settings
    public Filter filter = null;
    //Digital display settings
    public boolean digitalDisplay = false;
    public String decimalFormat = null;
    public String conversion = "x";
    public String prefix = "";
    public String suffix = "";

    //Sickbay ID
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