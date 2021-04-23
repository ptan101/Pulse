package tan.philip.nrf_ble.BLE;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

import tan.philip.nrf_ble.BLE.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.Biometrics;
import tan.philip.nrf_ble.GraphScreen.Filter;

public class BLEPacketParser {
    private static final String TAG = "BLEPacketParser";

    //int numSignals;
    int notificationFrequency;
    int packageSizeBytes;                              //Use this to check incoming packages and if the init file is valid
    ArrayList<SignalSetting> signalSettings;           //Just holds all the information about each signal
    ArrayList<Byte> signalOrder;
    Biometrics biometrics = new Biometrics();

    //Use this to instantiate a new BLEPackageParser object
    public BLEPacketParser(Context context, String deviceName) throws Exception  {
        signalSettings = new ArrayList<>();
        signalOrder = new ArrayList<>();

        //First, use the deviceName to lookup the right init file
        String initFileName = lookupInitFile(deviceName, context);

        if (initFileName == null)
            throw new Exception();

        //Load in the init file
        parseInitFile(initFileName, context);
    }

    //To do: make this arraylist of arrays
    public ArrayList<ArrayList<Integer>> parsePackage(byte data[]) {
        if (data.length != packageSizeBytes) {
            Log.e("Data parser", "The package is not the same size as expected");
            return null;
        }

        ArrayList<ArrayList<Integer>> parsedData = new ArrayList();
        for (int i = 0; i < signalSettings.size(); i ++) {
            parsedData.add(new ArrayList<>());
        }

        int i = 0;
        for (int index: signalOrder) {
            SignalSetting signalSetting = signalSettings.get(index);

            //Determine size of data in bytes
            int size = signalSetting.bytesPerPoint;

            int cur_data = data[i];
            if(!signalSetting.signed)
                cur_data &= 0xFF;

            //Load a certain number of bytes from data
            for (int j = 1; j < size; j ++) {
                cur_data = ((cur_data) << 8) | (data[i + j] & 0xFF);       //Shift old bytes by 8 bits to make space for new byte
            }

            //Store the parsed data into the correct ArrayList
            parsedData.get(index).add(cur_data);

            //Move to the next data in the package
            i += size;
        }

        return parsedData;
    }

    //Use this to filter a signal based on the filter in the init file
    //index is the index in the ArrayList that the data was parsed
    //Also converts the ArrayList to a normal array of floats
    public float[] filterSignals(ArrayList<Integer> raw_data, int index) {
        float [] filtered_data = new float[raw_data.size()];

        for(int i = 0; i < raw_data.size(); i ++) {
            if(signalSettings.get(index).filter != null)
                filtered_data[i] = signalSettings.get(index).filter.findNextY((raw_data.get(i)));
                //filtered_data[i] = raw_data.get(i);
            else
                filtered_data[i] = raw_data.get(i);
        }

        return filtered_data;
    }


    private void parseInitFile(String initFileName, Context context) {
        ArrayList<String> lines = loadInitFile(initFileName, context);
        int i = 0;
        String cur_line;

        //First section (information about the packet as a whole)
        cur_line = lines.get(i);
        String[] packet_data = cur_line.split(", ");
        notificationFrequency = Integer.parseInt(packet_data[0]);
        packageSizeBytes = Integer.parseInt(packet_data[1]);
        i+=2;

        //Second section (information about each signal)
        while(true) {
            cur_line = lines.get(i);
            //If main heading, add a new signal setting
            i++;
            if (cur_line.equals("end"))
                break;
            if(cur_line.charAt(0) != '.') {
                String[] signal_info = cur_line.split(", ");
                SignalSetting cur_setting = new SignalSetting(Byte.parseByte(signal_info[0]), signal_info[1],
                        Byte.parseByte(signal_info[2]), Integer.parseInt(signal_info[3]),
                        Byte.parseByte(signal_info[4]), signal_info[5].equals("signed"));
                signalSettings.add(cur_setting);
            } else if (cur_line.charAt(0) == '.' && !cur_line.substring(0, 2).equals("..")) {
                parseSignalMainOptions(signalSettings.get(signalSettings.size() - 1), cur_line, gatherSubHeadings(lines, i));
            }


        }
        //Third section (information about calculated biometrics)
        while(true) {
            cur_line = lines.get(i);
            i++;
            if (cur_line.equals("end"))
                break;
            parseBiometricsOptions(biometrics, cur_line);
        }
        //Fourth section (information about order of data in packet)
        while(true) {
            cur_line = lines.get(i);
            i++;
            if (cur_line.equals("end"))
                break;
            signalOrder.add(Byte.parseByte(cur_line));
        }
    }

    public ArrayList<SignalSetting> getSignalSettings() {
        return signalSettings;
    }

    public Biometrics getBiometricsSettings() {
        return biometrics;
    }

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

    private void parseSignalMainOptions(SignalSetting signalSetting, String mLine, ArrayList<String> subheadings) {
        //Cut out the initial period
        mLine = mLine.substring(1);

        switch (mLine) {
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
            default:
                break;
        }

    }

    private void parseBiometricsOptions(Biometrics biometrics, String mLine) {
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
    
    private ArrayList<String> gatherSubHeadings(ArrayList<String> lines, int curLine) {
        ArrayList<String> out = new ArrayList<String>();
        for(int i = curLine; i < lines.size(); i ++) {
            if(!lines.get(i).substring(0, 2).equals(".."))
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
                    int color[] = new int[4];
                    String[] color_s = options[1].split(" ");
                    for (int i = 0; i < color_s.length; i++) {
                        color[i] = Integer.parseInt(color_s[i]);
                    }
                    signalSetting.color = color;
                    break;
                default:
                    break;
            }
        }
    }

    private void importFilterSubSettings(SignalSetting signalSetting, ArrayList<String> subsettings) {
        float[] b = new float[0];
        float[] a = new float[0];
        float gain = 1;

        for(String s: subsettings) {
            s = s.substring(2);
            String[] options = s.split(": ");

            switch (options[0]) {
                case "b":
                    String[] b_s = options[1].split(", ");
                    float[] b_f = new float[b_s.length];

                    for (int i = 0; i < b_s.length; i++) {
                        b_f[i] = Float.parseFloat(b_s[i]);
                    }
                    b = b_f;
                    break;
                case "a":
                    String[] a_s = options[1].split(", ");
                    float[] a_f = new float[a_s.length];

                    for (int i = 0; i < a_s.length; i++) {
                        a_f[i] = Float.parseFloat(a_s[i]);
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
    private String lookupInitFile(String deviceName, Context context) {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("inits/init_file_lookup.txt")));

            String mLine;

            while (!((mLine = reader.readLine()) == null))
                if (mLine.startsWith(deviceName.split(" \\(")[0]))
                    return mLine.split(", ")[1];

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
        return null;
    }

}


