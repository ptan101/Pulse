package tan.philip.nrf_ble.FileWriting;

import static tan.philip.nrf_ble.FileWriting.PulseFile.BASE_DIR_PATH;

import android.os.Handler;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MarkerFile extends PulseFile {
    private static final String TAG = "MarkerFile";
    private final ConcurrentLinkedQueue<String[]> markerQueue;

    public MarkerFile(String fileName, String deviceName) {
        super(fileName, deviceName);
        markerQueue = new ConcurrentLinkedQueue();
    }

    //Writes to a CSV file. Used for event markers.
    public synchronized void writeData(String[] data) {
        isWriting = true;

        Log.d(TAG, "Writing to CSV");
        String filePath = BASE_DIR_PATH +
                File.separator + fileName +
                File.separator + deviceName +
                File.separator + fileName + "_" + deviceName + ".csv";

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
        } finally {
            if(markerQueue.size() > 0)
                writeData(markerQueue.poll());
            else
                isWriting = false;
        }
    }

    public void queueWrite(String[] data) {
        markerQueue.add(data);

        if(!isWriting)
            writeData(markerQueue.poll());
    }
}
