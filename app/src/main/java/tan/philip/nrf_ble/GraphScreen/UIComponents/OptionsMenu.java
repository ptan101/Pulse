package tan.philip.nrf_ble.GraphScreen.UIComponents;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;

import tan.philip.nrf_ble.Events.UIRequests.RequestChangeRecordEvent;
import tan.philip.nrf_ble.FileWriting.FileManager;
import tan.philip.nrf_ble.FileWriting.PulseFile;
import tan.philip.nrf_ble.R;

public class OptionsMenu implements PopupMenu.OnMenuItemClickListener{

    private static final int MENU_MARK_EVENT = 0;
    private boolean isRecording = false;
    private final Context ctx;
    private String fileName;
    private final FileManager fileManager;

    public OptionsMenu(Context ctx, ImageButton b, FileManager fileManager) {
        this.ctx = ctx;
        this.fileManager = fileManager;

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(ctx, view);
            }
        });
    }

    public void showPopup(Context ctx, View v) {
        PopupMenu popup = new PopupMenu(ctx, v);
        popup.setOnMenuItemClickListener(this);
        popup.getMenuInflater().inflate(R.menu.popup_menu_graph, popup.getMenu());

        //Record Text
        MenuItem recordMenuItem = popup.getMenu().findItem(R.id.record);
        if(isRecording) {
            recordMenuItem.setTitle("Stop recording");
            popup.getMenu().add(0, MENU_MARK_EVENT, Menu.NONE, "Mark Event");
        } else
            recordMenuItem.setTitle("Record");

        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.record:
                toggleRecord();
                return true;
            case R.id.help:
                showHelp();
                return true;
            case MENU_MARK_EVENT:
                markEvent();
                return true;
            default:
                //if (menuItem.getItemId() >= MENU_FIRST_TX_MESSAGE) {
                //    sendTMSMessage(menuItem.getItemId() - MENU_FIRST_TX_MESSAGE);
                //    return true;
                //}
                return false;
        }
    }

    private void toggleRecord() {
        if(!isRecording) {
            startRecord();
        } else {
            stopRecord();
        }
    }

    private void startRecord() {
        if(PulseFile.isStoragePermissionGranted(ctx)) {
            final EditText input = new EditText(ctx);
            String curTime = Calendar.getInstance().getTime().toString();
            input.setText(curTime);

            new AlertDialog.Builder(ctx)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Please give the file a name.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Start Record", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isRecording = true;
                            fileName = input.getText().toString();
                            fileName = fileName.replace(":", "");
                            fileName = fileName.replace("/", "");
                            fileName = fileName.replace("\\", "");
                            EventBus.getDefault().post(new RequestChangeRecordEvent(true, fileName));
                        }

                    })
                    .setView(input)
                    .show();
        } else {
            Toast.makeText(ctx, "Storage Permission is not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecord() {
        this.isRecording = false;
        EventBus.getDefault().post(new RequestChangeRecordEvent(false, null));
    }

    private void showHelp() {
        new AlertDialog.Builder(ctx)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Help")
                .setMessage(Html.fromHtml("<b>" + "Record:" + "</b>" + " Starts saving waveform data into internal storage (/Pulse_Data)." + "<br>" +
                        "<b>" + "Mark Event:" + "</b>" + " User can place a named marker when recording. This will be written in internal storage (/Pulse_Data)." + "<br>" +
                        "<br>" + "Please feel free to email Philip Tan (Lu Research Group) at philip.tan@utexas.edu if any issues are observed.", Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("Close", null)
                .show();
    }

    private void markEvent() {
        final EditText input = new EditText(ctx);

        new AlertDialog.Builder(ctx)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Label this event?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Mark Event", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("", "Event marked!");
                        String label = input.getText().toString();
                        fileManager.markEvent(label);
                    }
                })
                .setView(input)
                .show();
    }
}
