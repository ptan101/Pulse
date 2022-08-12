package tan.philip.nrf_ble.FileWriting;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;

public class TattooFile extends PulseFile {
    private static final String TAG = "TattooFile";
    private static final byte TAT_FILE_VERSION_NUMBER = 1;
    private final ConcurrentLinkedQueue<byte[]> packetQueue;

    public TattooFile(String fileName, String deviceName, HashMap<Integer, SignalSetting> signalSettings, ArrayList<Byte> signalOrder) {
        super(fileName, deviceName);
        packetQueue = new ConcurrentLinkedQueue<>();


        writeTATHeader(signalSettings, signalOrder);
    }

    public synchronized void queueWrite(byte[] data) {
        packetQueue.add(data);

        if(!isWriting)
            writeData(packetQueue.poll());
    }

    protected synchronized void writeData(byte[] data) {
        isWriting = true;

        String filePath = BASE_DIR_PATH +
                File.separator + fileName +
                File.separator + deviceName +
                File.separator + fileName + "_" + deviceName + ".tat";

        FileOutputStream fileOutputStream = null;

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Log.e(TAG, "Error creating .tat file.");
                    //Toast.makeText(getApplicationContext(), "Error creating file", Toast.LENGTH_SHORT).show();
                }
            }
            fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(data);

        } catch (FileNotFoundException ex) {
            Log.e(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(packetQueue.size() > 0)
                writeData(packetQueue.poll());
            else
                isWriting = false;
        }
    }

    //Converts Signal Settings into a header so when parsing later, know how to rearrange the data.
    private void writeTATHeader(HashMap<Integer, SignalSetting> signalSettings, ArrayList<Byte> signalOrder) {
        //First thing to write is a 32 bit number (4 bytes) that indicate how many bytes are written into the header.
        //So, we need to keep track of how many bytes are being written
        int bytes_in_header = 0;

        //The header will include one byte that represents the version number.
        bytes_in_header ++;

        //Just calculate how many bytes per signal setting
        for (int i : signalSettings.keySet()) {
            byte name_length = (byte) signalSettings.get(i).name.length();
            bytes_in_header += (10 + name_length);
        }

        //Write how many bytes the header will take
        byte[] out = new byte[bytes_in_header + 4];
        out[0] = (byte) (bytes_in_header >> 24);
        out[1] = (byte) (bytes_in_header >> 16);
        out[2] = (byte) (bytes_in_header >> 8);
        out[3] = (byte) (bytes_in_header & 0xFF);

        //Write the version number of the .tat file
        out[4] = TAT_FILE_VERSION_NUMBER;

        int cur_index = 5;

        //Actually append data for writing.
        for (int i : signalSettings.keySet()) {
            SignalSetting setting = signalSettings.get(i);

            //Index, name length, name, bytes per point, fs, bit resolution, signed/unsigned, big/little
            //1,    1,            name_length, 1,        4,  1,             1                 1
            byte name_length = (byte) setting.name.length();

            out[0 + cur_index] = setting.index;
            out[1 + cur_index] = name_length;
            for (int j = 0; j < name_length; j ++) {
                out[j + 2 + cur_index] = (byte) setting.name.charAt(j);
            }
            out[2 + name_length + cur_index] = setting.bytesPerPoint;
            out[3 + name_length + cur_index] = (byte) (setting.fs >> 24);
            out[4 + name_length + cur_index] = (byte) (setting.fs >> 16);
            out[5 + name_length + cur_index] = (byte) (setting.fs >> 8);
            out[6 + name_length + cur_index] = (byte) (setting.fs & 0xFF);
            out[7 + name_length + cur_index] = setting.bitResolution;
            out[8 + name_length + cur_index] = boolToByte(setting.signed);
            out[9 + name_length + cur_index] = boolToByte(setting.littleEndian);

            cur_index += (name_length + 10);
        }
        queueWrite(out);

        //Now, write the sequence of data
        //Once again, first write how much data is in each packet, then the sequence
        out = new byte[signalOrder.size() + 2];

        //Write the number of signals to be parsed. Probably can fit in a single byte, but using short to be safe
        out[0] = (byte) (signalOrder.size() >> 8);
        out[1] = (byte) (signalOrder.size() & 0xFF);

        //Write the signal order
        for(int i = 0; i < signalOrder.size(); i ++) {
            out[i + 2] = signalOrder.get(i);
        }

        queueWrite(out);
    }
}
