package tan.philip.nrf_ble.FileWriting;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public abstract class PulseFile {
    private static final String TAG = "PulseFile";
    protected static final String BASE_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Pulse_Data";
    protected final String sessionDirPath;
    protected final String fileName;
    protected final String deviceName;
    protected boolean isWriting = false;

    /**
     * Generic file for Pulse App. All other files extend it.
     * @param fileName Name of the file
     */
    public PulseFile(String fileName, String deviceName) {
        this.fileName = fileName;
        this.deviceName = deviceName;
        this.sessionDirPath = BASE_DIR_PATH + File.separator + fileName + File.separator + fileName + ".tat";
    }

    //Wanted to make write and queue abstract methods but had issues with generic primitive parameters

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
        File folder = new File(BASE_DIR_PATH + File.separator + folderName);
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

    protected static byte boolToByte(boolean in) {
        if (in)
            return 1;
        return 0;
    }
}
