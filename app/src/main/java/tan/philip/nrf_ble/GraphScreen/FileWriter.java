package tan.philip.nrf_ble.GraphScreen;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class FileWriter {
    private static final String TAG = "FileWriter";
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Pulse_Data";


    public  boolean isStoragePermissionGranted(Context context) {
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

    private void createFolder() {
        File folder = new File(path);
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

    public void writeBIN(short data, String fileName) {
        String filePath = path + File.separator + fileName;

        try {
            new File(path).mkdir();
            File file = new File(filePath);
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    //Toast.makeText(getApplicationContext(), "Error creating file", Toast.LENGTH_SHORT).show();
                }
            }
            Log.d("", Short.toString(data));
            fileOutputStream.write(ByteBuffer.allocate(2).putShort(data).array());

        } catch (FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
    }

    private void writeCSV(String data[], int lenData, String fileName) {
        Log.d(TAG, "Writing to CSV");
        String filePath = path + File.separator + fileName;
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

}
