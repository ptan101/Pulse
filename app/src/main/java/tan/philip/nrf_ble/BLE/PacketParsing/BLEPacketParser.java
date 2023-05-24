package tan.philip.nrf_ble.BLE.PacketParsing;

import static tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing.MainSettingsParser.importPacketStructure;
import static tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing.MainSettingsParser.importSignalSettings;
import static tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing.SickbaySettingsParser.parseSickbayOptions;
import static tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing.TattooMessagesParser.importMessages;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.Algorithms.BiometricsSet;
import tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing.BiometricSettingsParser;
import tan.philip.nrf_ble.SickbayPush.SickbayImportSettings;

public class BLEPacketParser {
    private static final String TAG = "BLEPacketParser";

    //int numSignals;
    float notificationFrequency;
    int packageSizeBytes;                              //Use this to check incoming packages and if the init file is valid
    HashMap<Integer, SignalSetting> signalSettings;           //Just holds all the information about each signal
    ArrayList<Byte> signalOrder;
    ArrayList<TattooMessage> rxMessages;
    ArrayList<TattooMessage> txMessages;

    //Biometrics
    BiometricSettingsParser bmParser;
    BiometricsSet biometrics = new BiometricsSet();

    //Sickbay settings
    public SickbayImportSettings sickbaySettings;

    private boolean initFileGood = true;

    //Use this to instantiate a new BLEPackageParser object
    public BLEPacketParser(Context context, String deviceName) throws FileNotFoundException {
        signalSettings = new HashMap<>();
        signalOrder = new ArrayList<>();
        rxMessages = new ArrayList<>();
        bmParser = new BiometricSettingsParser(signalSettings);
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

        //Read the packet information
        parsePacketInformation(lines);

        //Read the signal settings
        initFileGood = importSignalSettings(lines, signalSettings);

        //Check the packet structure
        initFileGood = importPacketStructure(lines, signalOrder);

        //Load in Tattoo Messaging Service messages
        importMessages(lines, rxMessages, "RX Messages");
        importMessages(lines, txMessages, "TX Messages");

        //Load in Biometric settings
        bmParser.importBiometrics(lines, biometrics);

        //Load in Sickbay settings
        sickbaySettings = parseSickbayOptions(lines);

        Log.d(TAG, "Init file successfully loaded in.");
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

    private void parsePacketInformation(ArrayList<String> initFileLines) {
        try {
            ArrayList<String> lines = getInitFileSection(initFileLines, "Packet Information");
            String cur_line = lines.get(0);
            String[] packet_data = cur_line.split(", ");
            notificationFrequency = Float.parseFloat(packet_data[0]);
            packageSizeBytes = Integer.parseInt(packet_data[1]);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to read .init file Packet Information.");
            initFileGood = false;
        }
    }

    /**
     * Truncates an .init file to just a desired section.
     * @param lines Entire .init file
     * @param sectionName Desired section to truncate to
     * @return The truncated section
     */
    public static ArrayList<String> getInitFileSection(ArrayList<String> lines,String sectionName) {
        //Find the starting index of the section by using the section name.
        int startIndex = lines.indexOf(sectionName) + 1;
        int endIndex = lines.subList(startIndex, lines.size()).indexOf("end");

        ArrayList<String> section = new ArrayList<String>(lines.subList(startIndex, endIndex + startIndex));

        return section;
    }
    
    public static ArrayList<String> gatherSubHeadings(ArrayList<String> lines, int curLine) {
        ArrayList<String> out = new ArrayList<String>();
        for(int i = curLine + 1; i < lines.size(); i ++) {
            if(!lines.get(i).startsWith(".."))
                break;

            out.add(lines.get(i).substring(2));
        }
        
        return out;
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

    /////////////GETTERS AND SETTERS////////////////////////

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

}


