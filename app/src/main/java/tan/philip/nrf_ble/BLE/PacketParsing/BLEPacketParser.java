package tan.philip.nrf_ble.BLE.PacketParsing;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import tan.philip.nrf_ble.Algorithms.BiometricsSet;
import tan.philip.nrf_ble.GraphScreen.UIComponents.Filter;

public class BLEPacketParser {
    private static final String TAG = "BLEPacketParser";

    //int numSignals;
    float notificationFrequency;
    int packageSizeBytes;                              //Use this to check incoming packages and if the init file is valid
    HashMap<Integer, SignalSetting> signalSettings;           //Just holds all the information about each signal
    ArrayList<Byte> signalOrder;
    ArrayList<TattooMessage> rxMessages;
    ArrayList<TattooMessage> txMessages;
    BiometricsSet biometrics = new BiometricsSet();

    //Sickbay settings
    public String sickbayNS = null;

    //Use this to instantiate a new BLEPackageParser object
    public BLEPacketParser(Context context, String deviceName) throws FileNotFoundException {
        signalSettings = new HashMap<>();
        signalOrder = new ArrayList<>();
        rxMessages = new ArrayList<>();
        rxMessages.add(null);   //Message ID 0 should be an empty message (i.e., no messsage)
        txMessages = new ArrayList<>();
        txMessages.add(null);   //Message ID 0 should be an empty message (i.e., no messsage)

        //First, use the deviceName to lookup the right init file
        String initFileName = lookupInitFile(deviceName, context);

        if (initFileName == null) {
            Log.e(TAG, "initFileName is null.");
            throw new FileNotFoundException();
        }

        //Load in the init file
        parseInitFile(initFileName, context);
    }

    public HashMap<Integer, ArrayList<Integer>> parsePacket(byte[] data) {
        HashMap<Integer, ArrayList<Integer>> parsedData = new HashMap();
        for (Integer i : signalSettings.keySet()) {
            parsedData.put(i, new ArrayList<>());
        }

        int i = 0;
        for (int index: signalOrder) {
            SignalSetting signalSetting = signalSettings.get(index);

            //Determine size of data in bytes
            int size = signalSetting.bytesPerPoint;

            if(size > 4)
                size = 4; //Clamp data length to a 32 bit integer.

            int cur_data = data[i];

            //If the data is big endian and signed, we want to extend the MSB to maintain sign.
            //Also, if the data is only one byte and signed, regardless of endian, we want to keep the sign.
            //Otherwise, we want to remove those bits.
            if((signalSetting.littleEndian || !signalSetting.signed) && !(size == 1 && signalSetting.signed))
                cur_data &= 0xFF;

            //Load a certain number of bytes from data
            for (int j = 1; j < size; j ++) {
                if(!signalSetting.littleEndian)
                    cur_data = ((cur_data) << 8) | (data[i + j] & 0xFF);          //Shift old bytes by 8 bits to make space for new byte. This is for Big Endian
                else {
                    int cur_byte = data[i + j];
                    if(j != size - 1 || !signalSetting.signed)
                        cur_byte &= 0xFF;   //If it's not signed or the MSB, no need to sign extend

                    cur_data = (cur_data | (cur_byte) << (8 * j));        //Shift new bytes by however many bytes there are already in cur_data.. This is for little Endian
                }
            }

            //Store the parsed data into the correct ArrayList
            parsedData.get(index).add(cur_data);

            //Move to the next data in the package
            i += size;
        }

        return parsedData;
    }

    public HashMap<Integer, ArrayList<Float>> convertToSickbayHashMap(HashMap<Integer, ArrayList<Float>> signals) {
        HashMap<Integer, ArrayList<Float>> sickbayQueue = new HashMap<>();

        for (Integer i : signalSettings.keySet()) {
            int sickbayID = signalSettings.get(i).sickbayID;

            if(sickbayID >= 0) {
                sickbayQueue.put(sickbayID, signals.get(i));
            }
        }

        return sickbayQueue;
    }

    //Use this to filter a signal based on the filter in the init file
    //index is the index in the ArrayList that the data was parsed
    //Also converts the ArrayList to a normal array of floats
    public ArrayList<Float> filterSignals(ArrayList<Integer> raw_data, int index) {
        ArrayList<Float> filtered_data = new ArrayList<>(raw_data.size());
        float prescaler = (float) Math.pow(2, signalSettings.get(index).bitResolution);

        for(int i = 0; i < raw_data.size(); i ++) {
            if(signalSettings.get(index).filter != null) {
                float filteredSample = signalSettings.get(index).filter.findNextY((raw_data.get(i)));
                filteredSample /= prescaler;
                filtered_data.add(filteredSample);
            } else {
                filtered_data.add((float) raw_data.get(i));
            }
        }

        return filtered_data;
    }


    private void parseInitFile(String initFileName, Context context) {
        ArrayList<String> lines = loadInitFile(initFileName, context);
        int i = 0;
        String cur_line;

        while (true) {
            cur_line = lines.get(i);
            switch(cur_line) {
                case "Packet Information":
                    i++;
                    cur_line = lines.get(i);
                    String[] packet_data = cur_line.split(", ");
                    notificationFrequency = Float.parseFloat(packet_data[0]);
                    packageSizeBytes = Integer.parseInt(packet_data[1]);
                    i++;
                    break;
                case "Signals Information":
                    i++;
                    while(true) {
                        cur_line = lines.get(i);
                        //If main heading, add a new signal setting
                        if (cur_line.equals("end"))
                            break;
                        i++;
                        if(cur_line.charAt(0) != '.') {
                            String[] signal_info = cur_line.split(", ");
                            SignalSetting cur_setting = new SignalSetting(Byte.parseByte(signal_info[0]), signal_info[1],
                                    Byte.parseByte(signal_info[2]), Integer.parseInt(signal_info[3]),
                                    Byte.parseByte(signal_info[4]), signal_info[5].equals("signed"));
                            signalSettings.put((int) cur_setting.index, cur_setting);
                        } else if (cur_line.charAt(0) == '.' && !cur_line.startsWith("..")) {
                            parseSignalMainOptions(signalSettings.get(signalSettings.size() - 1),
                                    cur_line, gatherSubHeadings(lines, i));
                        }
                    }
                    break;
                case "Biometric Settings":
                    i++;
                    while(true) {
                        cur_line = lines.get(i);
                        if (cur_line.equals("end"))
                            break;
                        parseBiometricsOptions(biometrics, cur_line);
                        i++;
                    }
                    break;
                case "Packet Structure":
                    i++;
                    while(true) {
                        cur_line = lines.get(i);
                        if (cur_line.equals("end"))
                            break;
                        signalOrder.add(Byte.parseByte(cur_line));
                        i++;
                    }
                    break;
                case "RX Messages":
                    i++;
                    while(true) {
                        cur_line = lines.get(i);
                        if (cur_line.equals("end"))
                            break;
                        if(cur_line.charAt(0) != '.') {
                            TattooMessage cur_mesg = new TattooMessage(cur_line, true);
                            rxMessages.add(cur_mesg);
                        } else if (cur_line.charAt(0) == '.') {
                            parseMessageData(rxMessages.get(rxMessages.size() - 1), cur_line);
                        }
                        i++;
                    }
                    break;
                case "TX Messages":
                    i++;
                    while(true) {
                        cur_line = lines.get(i);
                        if (cur_line.equals("end"))
                            break;
                        if(cur_line.charAt(0) != '.') {
                            TattooMessage cur_mesg = new TattooMessage(cur_line, false);
                            txMessages.add(cur_mesg);
                        } else if (cur_line.charAt(0) == '.') {
                            parseMessageData(txMessages.get(txMessages.size() - 1), cur_line);
                        }
                        i++;
                    }
                    break;
                case "Sickbay Settings":
                    i++;
                    while(true) {
                        cur_line = lines.get(i);
                        if (cur_line.equals("end"))
                            break;
                        if (cur_line.charAt(0) == '.') {
                            parseSickbayOptions(cur_line);
                        }
                        i++;
                    }
                default:
                    Log.e(TAG, "Unknown header: " + cur_line);
                    break;
            }
            i++;
            if (i >= lines.size())
                break;
        }
    }

    public HashMap<Integer, SignalSetting> getSignalSettings() {
        return signalSettings;
    }

    public BiometricsSet getBiometricsSettings() {
        return biometrics;
    }

    public ArrayList<Byte> getSignalOrder() {
        return signalOrder;
    }

    public ArrayList<TattooMessage> getRxMessages() {
        return rxMessages;
    }

    public ArrayList<TattooMessage> getTxMessages() {
        return txMessages;
    }

    public float getNotificationFrequency() { return notificationFrequency; }

    public int getPackageSizeBytes() { return packageSizeBytes; }

    public BiometricsSet getBiometrics() { return biometrics; }

    ////////////////////////Helper Methods to Parse Init file/////////////////////////////////////



    private ArrayList<String> loadInitFile(String initFileName, Context context) {
        BufferedReader reader = null;
        ArrayList<String> lines = new ArrayList<String>();
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("inits/" + initFileName)));

            String mLine;

            //First, get information about the whole packet
            while (!((mLine = reader.readLine()) == null)) {
                lines.add(mLine);
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
        return lines;
    }

    private void parseSignalMainOptions(SignalSetting signalSetting, String pLine, ArrayList<String> subheadings) {
        //Cut out the initial period
        pLine = pLine.substring(1);

        //Look at first word
        String[] mainOption = pLine.split(" ");

        switch (mainOption[0]) {
            case "graphable":
                signalSetting.graphable = true;
                importGraphableSubSettings(signalSetting, subheadings);
                break;
            case "digdisplay":
                signalSetting.digitalDisplay = true;
                importDigitalDisplaySubSettings(signalSetting, subheadings);
                break;
            case "filter":
                importFilterSubSettings(signalSetting, subheadings);
                break;
            case "big":
                signalSetting.littleEndian = false;
                break;
            case "sickbayID":
                signalSetting.sickbayID = Integer.parseInt(mainOption[1]);
                break;
            default:
                break;
        }

    }

    private void parseMessageData(TattooMessage message, String pLine) {
        //Cut out the initial period
        String option = pLine.substring(1, pLine.indexOf(" "));
        String setting = pLine.substring(pLine.indexOf(" ") + 1);

        switch (option) {
            case "brief":
                message.brief = setting;
                break;
            case "alternate":
                message.alternate = Integer.parseInt(setting);
                break;
            case "isAlternate":
                message.isAlternate = Boolean.parseBoolean(setting);
                break;
            case "alertDialog":
                message.alertDialog = Boolean.parseBoolean(setting);
                break;
            case "sendTX":
                message.idTXMessage = Integer.parseInt(setting);
                break;
            default:
                break;
        }
    }

    private void parseBiometricsOptions(BiometricsSet biometrics, String mLine) {
        String[] settings = mLine.split(", ");

        switch (settings[0]) {
            case "heartrate":
                biometrics.addHRSignals(Integer.parseInt(settings[1]));
                break;
            case "spo2":
                biometrics.addSpO2Signals(Integer.parseInt(settings[1]), Integer.parseInt(settings[2]), Integer.parseInt(settings[3]), Integer.parseInt(settings[4]));
                break;
            case "pwv":
                biometrics.addPWVSignals(Integer.parseInt(settings[1]), Integer.parseInt(settings[2]));
                break;
            default:
                break;
        }

    }

    private void parseSickbayOptions(String pLine) {
        //Cut out the initial period
        pLine = pLine.substring(1);

        //Look at first word
        String[] sickbaySettings = pLine.split(" ");

        switch (sickbaySettings[0]) {
            case "sickbayNS":
                sickbayNS = sickbaySettings[1];
                break;
            default:
                break;
        }

    }
    
    private ArrayList<String> gatherSubHeadings(ArrayList<String> lines, int curLine) {
        ArrayList<String> out = new ArrayList<String>();
        for(int i = curLine; i < lines.size(); i ++) {
            if(!lines.get(i).startsWith(".."))
                break;

            out.add(lines.get(i));
        }
        
        return out;
    }

    private void importGraphableSubSettings(SignalSetting signalSetting, ArrayList<String> subsettings) {
        for(String s: subsettings) {
            s = s.substring(2);
            String[] options = s.split(": ");

            switch (options[0]) {
                case "color":
                    int[] color = new int[4];
                    String[] color_s = options[1].split(" ");

                    if(color_s.length != 4)
                        Log.e(TAG, "Color " + options[1] + " needs to be RGBA.");

                    for (int i = 0; i < 4; i++) {
                        color[i] = Integer.parseInt(color_s[i]);
                    }
                    signalSetting.color = color;
                    break;
                case "graphHeight":
                    signalSetting.graphHeight = Integer.parseInt(options[1]);
                    break;
                default:
                    break;
            }
        }
    }

    private void importFilterSubSettings(SignalSetting signalSetting, ArrayList<String> subsettings) {
        double[] b = new double[0];
        double[] a = new double[0];
        float gain = 1;

        for(String s: subsettings) {
            s = s.substring(2);
            String[] options = s.split(": ");

            switch (options[0]) {
                case "b":
                    String[] b_s = options[1].split(", ");
                    double[] b_f = new double[b_s.length];

                    for (int i = 0; i < b_s.length; i++) {
                        b_f[i] = Double.parseDouble(b_s[i]);
                    }
                    b = b_f;
                    break;
                case "a":
                    String[] a_s = options[1].split(", ");
                    double[] a_f = new double[a_s.length];

                    for (int i = 0; i < a_s.length; i++) {
                        a_f[i] = Double.parseDouble(a_s[i]);
                    }
                    a = a_f;
                    break;
                case "gain":
                    gain = Float.parseFloat(options[1]);
                    break;
                default:
                    break;
            }
        }

        signalSetting.filter = new Filter(b, a, gain);
    }



    private void importDigitalDisplaySubSettings(SignalSetting signalSetting, ArrayList<String> subsettings) {
        for(String s: subsettings) {
            s = s.substring(2);
            String[] options = s.split(": ");

            switch (options[0]) {
                case "DecimalFormat":
                    signalSetting.decimalFormat = options[1];
                    break;
                case "conversion":
                    signalSetting.conversion = options[1];
                    break;
                case "prefix":
                    Properties p = new Properties();
                    try {
                        p.load(new StringReader("key="+options[1]));
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to convert escape sequence" + options[1]);
                    }
                    signalSetting.prefix = p.getProperty("key");
                    break;
                case "suffix":
                    p = new Properties();
                    try {
                        p.load(new StringReader("key="+options[1]));
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to convert escape sequence" + options[1]);
                    }
                    signalSetting.suffix = p.getProperty("key");
                    break;
                default:
                    break;
            }
        }
    }

    //////////////////Helper Method for loading in init file lookup table
    public static String lookupInitFile(String deviceName, Context context) {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("inits/init_file_lookup.txt")));

            String mLine;

            while (!((mLine = reader.readLine()) == null))
                if (mLine.startsWith(deviceName.split(" \\(")[0]))
                    return mLine.split(", ")[1];

        } catch (IOException e) {
            Log.e(TAG, "Unable to open init file lookup table.");
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Unable tp close buffered reader");
                    //log the exception
                }
            }
        }
        return null;
    }

}


