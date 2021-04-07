package tan.philip.nrf_ble.BLE;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import tan.philip.nrf_ble.BLE.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.Biometrics;
import tan.philip.nrf_ble.GraphScreen.Filter;

public class BLEPackageParser {
    //int numSignals;
    int notificationFrequency;
    int packageSizeBytes;                              //Use this to check incoming packages and if the init file is valid
    ArrayList<SignalSetting> signalSettings;           //Just holds all the information about each signal
    ArrayList<Integer> signalOrder;
    Biometrics biometrics = new Biometrics();

    //Use this to instantiate a new BLEPackageParser object
    public BLEPackageParser (Context context) {
        signalSettings = new ArrayList<>();
        signalOrder = new ArrayList<>();

        //Eventually, load in data from some init file to tell how to parse the BT package.
        parseInitFile("", context);
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

            int cur_data = 0;
            //Load a certain number of bytes from data
            for (int j = 0; j < size; j ++) {
                cur_data = (cur_data << 8) | data[i + j];       //Shift old bytes by 8 bits to make space for new byte
            }

            //Store the parsed data into the correct ArrayList
            parsedData.get(index).add(cur_data);

            //Move to the next data in the package
            i += size;
        }

        return parsedData;
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
                SignalSetting cur_setting = new SignalSetting(Integer.parseInt(signal_info[0]), signal_info[1], Integer.parseInt(signal_info[2]), Integer.parseInt(signal_info[3]), Integer.parseInt(signal_info[4]));
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
            signalOrder.add(Integer.parseInt(cur_line));
        }
    }

    //Use this to filter a signal based on the filter in the init file
    //index is the index in the ArrayList that the data was parsed
    //Also converts the ArrayList to a normal array of floats
    public float[] filterSignals(ArrayList<Integer> raw_data, int index) {
        float [] filtered_data = new float[raw_data.size()];

        for(int i = 0; i < raw_data.size(); i ++) {
            //filtered_data[i] = filters.get(i).findNextY((raw_data.get(i)));
            filtered_data[i] = raw_data.get(i);
        }

        return filtered_data;
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
                    new InputStreamReader(context.getAssets().open("inits/ppg_v2.init")));
            //new InputStreamReader(context.getAssets().open("ppg_v2.init")));

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

        for(String s: subsettings) {
            s = s.substring(2);
            String[] options = s.split(": ");

            switch (options[0]) {
                case "b":
                    String[] b_s = options[1].split(", ");
                    float[] b_f = new float[b_s.length];

                    for (int i = 0; i < b_s.length - 1; i++) {
                        b_f[i] = Float.parseFloat(b_s[i + 1]);
                    }
                    b = b_f;
                    break;
                case "a":
                    String[] a_s = options[1].split(", ");
                    float[] a_f = new float[a_s.length];

                    for (int i = 0; i < a_s.length - 1; i++) {
                        a_f[i] = Float.parseFloat(a_s[i + 1]);
                    }
                    a = a_f;
                    break;
                default:
                    break;
            }
        }

        signalSetting.filter = new Filter(b, a);
    }

}


