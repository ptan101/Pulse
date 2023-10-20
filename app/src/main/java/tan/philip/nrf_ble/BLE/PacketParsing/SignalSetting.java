package tan.philip.nrf_ble.BLE.PacketParsing;

import java.io.Serializable;

import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplaySettings;
import tan.philip.nrf_ble.Algorithms.Filter.Filter;

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
    public int[] color = {0, 0, 0, 255};
    public int graphHeight = 150;

    //Filter settings
    public Filter filter = null;
    //Digital display settings
    public DigitalDisplaySettings ddSettings;

    //Imaging settings
    public boolean image = false;
    public int c1_index = 1; //Signal index of Calibration 1
    public int c2_index = 2; //Signal index of Calibration 2
    public int t1_index = 3; //Signal index of Time 1
    public int nImageLines = 320;

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