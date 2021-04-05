package tan.philip.nrf_ble.BLE;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import tan.philip.nrf_ble.BLE.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.Filter;

public class BLEPackageParser {
    //int numSignals;
    int notificationFrequency;
    int packageSizeBytes;                              //Use this to check incoming packages and if the init file is valid
    ArrayList<SignalSetting> signalSettings;           //Just holds all the information about each signal
    ArrayList<Integer> signalOrder;
    ArrayList<Filter> filters;

    //Use this to instantiate a new BLEPackageParser object
    public BLEPackageParser (Context context) {
        signalSettings = new ArrayList<>();
        signalOrder = new ArrayList<>();
        filters = new ArrayList<>();

        //Eventually, load in data from some init file to tell how to parse the BT package.
        loadInitFile("", context);
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

    private void loadInitFile(String initFileName, Context context) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("inits/ppg_v2.init")));
                    //new InputStreamReader(context.getAssets().open("ppg_v2.init")));

            // do reading, usually loop until end of file reading
            String mLine;
            //First, get information about the whole packet
            while (!(mLine = reader.readLine()).contains("end")) {
                String[] line_info = mLine.split(", ");
                Log.d("1", mLine);
                notificationFrequency = Integer.parseInt(line_info[0]);
                packageSizeBytes = Integer.parseInt(line_info[1]);
            }
            //Then, get information about each signal
            while (!(mLine = reader.readLine()).contains("end")) {
                String[] line_info = mLine.split(", ");
                Log.d("2", mLine);

                int index = Integer.parseInt(line_info[0]);                 //Index might not be necessary but oh well
                String name = line_info[1];
                int bytesPerPoint = Integer.parseInt(line_info[2]);
                int fs = Integer.parseInt(line_info[3]);

                int color[] = new int[4];
                String[] color_s = line_info[4].split(" ");
                for(int i = 0; i < color_s.length; i ++) {
                    color[i] = Integer.parseInt(color_s[i]);
                }


                signalSettings.add(new SignalSetting(index, name, bytesPerPoint, fs, color));
            }
            //Finally, get information about the structure of the packet
            while ((mLine = reader.readLine()) != null) {
                Log.d("3", mLine);
                signalOrder.add(Integer.parseInt(mLine));
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

        //numSignals = 2;         //Load this in

        //signalOrder.add(new SignalSetting(0, 2, 8));
        //signalOrder.add(new SignalSetting(1, 2, 8));

        //Load these in eventually
        filters.add(new Filter(Filter.SignalType.PPG));
        filters.add(new Filter(Filter.SignalType.PPG));
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
}


