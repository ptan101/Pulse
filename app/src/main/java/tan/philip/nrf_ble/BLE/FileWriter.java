package tan.philip.nrf_ble.BLE;

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

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;

public class FileWriter {
    private static final String TAG = "FileWriter";
    private static final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Pulse_Data";


    public static boolean isStoragePermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    public static void createFolder(String folderName) {
        File folder = new File(path + File.separator + folderName);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            Log.d(TAG, "Successfully created new folder / already exists");
        } else {
            Log.d(TAG, "Failed to create new folder");
        }
    }

    //Converts Signal Settings into a header so when parsing later, know how to rearrange the data.
    public static void writeBINHeader(ArrayList<SignalSetting> signalSettings, ArrayList<Byte> signalOrder, String fileName) {
        //First thing to write is a 32 bit number (4 bytes) that indicate how many bytes are written into the header.
        //So, we need to keep track of how many bytes are being written
        int bytes_in_header = 0;

        //Just calculate how many bytes per signal setting
        for (SignalSetting setting: signalSettings) {
            byte name_length = (byte) setting.name.length();
            bytes_in_header += (10 + name_length);
        }

        byte[] out = new byte[bytes_in_header + 4];
        out[0] = (byte) (bytes_in_header >> 24);
        out[1] = (byte) (bytes_in_header >> 16);
        out[2] = (byte) (bytes_in_header >> 8);
        out[3] = (byte) (bytes_in_header & 0xFF);

        int cur_index = 4;

        //Actually append data for writing.
        for (SignalSetting setting: signalSettings) {
            //Index, name length, name, bytes per point, fs, bit resolution, signed/unsigned, big/little
            //1,    1,            name_length, 1,        4,  1,             1                 1
            byte name_length = (byte) setting.name.length();

            out[0 + cur_index] = setting.index;
            out[1 + cur_index] = name_length;
            for (int i = 0; i < name_length; i ++) {
                out[i + 2 + cur_index] = (byte) setting.name.charAt(i);
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
        writeBIN(out, fileName);

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

        writeBIN(out, fileName);
    }

    public static void writeBIN(byte[] data, String fileName) {
        String filePath = path + File.separator + fileName + File.separator + fileName + ".bin";
        FileOutputStream fileOutputStream = null;

        try {
            new File(path).mkdir();
            File file = new File(filePath);
            fileOutputStream = new FileOutputStream(file, true);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    //Toast.makeText(getApplicationContext(), "Error creating file", Toast.LENGTH_SHORT).show();
                }
            }
            //Log.d("", Byte.toString(data));
            //fileOutputStream.write(ByteBuffer.allocate(2).putShort(data).array());
            fileOutputStream.write(data);

        } catch (FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Writes to a CSV file. Used for event markers.
    public static void writeCSV(String[] data, String fileName) {
        Log.d(TAG, "Writing to CSV");
        String filePath = path + File.separator + fileName + File.separator + fileName + ".csv";
        File f = new File(filePath);
        CSVWriter writer;
        java.io.FileWriter mFileWriter;

        try {
            // File exist
            if(f.exists()&&!f.isDirectory())
            {
                mFileWriter = new java.io.FileWriter(filePath, true);
                writer = new CSVWriter(mFileWriter);
            }
            else
            {
                writer = new CSVWriter(new java.io.FileWriter(filePath));
            }

            writer.writeNext(data);

            writer.close();
            Log.d(TAG, "SUCCESSFUL WRITE");
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        }
    }

    private static byte boolToByte(boolean in) {
        if (in)
            return 1;
        return 0;
    }

}
