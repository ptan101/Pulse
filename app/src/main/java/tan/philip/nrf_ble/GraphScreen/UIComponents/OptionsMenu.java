package tan.philip.nrf_ble.GraphScreen.UIComponents;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.PacketParsing.TattooMessage;
import tan.philip.nrf_ble.Events.UIRequests.RequestChangeAutoScaleAll;
import tan.philip.nrf_ble.Events.UIRequests.RequestChangeRecordEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestSendTMSEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestTMSSendEvent;
import tan.philip.nrf_ble.FileWriting.FileManager;
import tan.philip.nrf_ble.FileWriting.PulseFile;
import tan.philip.nrf_ble.R;

public class OptionsMenu implements PopupMenu.OnMenuItemClickListener{

    private static final int MENU_MARK_EVENT = 0;
    private boolean isRecording = false;
    private final Context ctx;
    private String fileName;
    private final FileManager fileManager;
    private final ArrayList<BLEDevice> devices;

    public OptionsMenu(Context ctx, ImageButton b, FileManager fileManager, ArrayList<BLEDevice> devices) {
        this.ctx = ctx;
        this.fileManager = fileManager;
        this.devices = devices;

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

        //Add submenu items for each BLE Device.
        int itemId = MENU_MARK_EVENT;
        for (BLEDevice d : devices) {
            itemId ++;
            d.setMenuID(itemId);
            popup.getMenu().addSubMenu(Menu.NONE, itemId, Menu.NONE, d.getDisplayName());
            SubMenu subMenu = popup.getMenu().findItem(itemId).getSubMenu();

            for(TattooMessage t : d.getTmsTxMessages()) {
                if(t != null && !t.isAlternate()) {
                    itemId++;
                    t.setMenuID(itemId);
                    subMenu.add(Menu.NONE, itemId, Menu.NONE, t.getMainMessage());
                }
            }
        }

        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.record:
                toggleRecord();
                return true;
            case R.id.enable_all_autoscale:
                EventBus.getDefault().post(new RequestChangeAutoScaleAll(true));
                return true;
            case R.id.disable_all_autoscale:
                EventBus.getDefault().post(new RequestChangeAutoScaleAll(false));
                return true;
            case R.id.help:
                showHelp();
                return true;
            case MENU_MARK_EVENT:
                markEvent();
                return true;
            default:
                return checkTXMessageClicked(menuItem.getItemId());
        }
    }

    private boolean checkTXMessageClicked(int menuItemId) {
        for(BLEDevice d : devices) {
            if(d.getMenuID() > menuItemId)
                break;

            ArrayList<TattooMessage> messages = d.getTmsTxMessages();
            for(int i = 0; i < messages.size(); i ++) {
                TattooMessage t = messages.get(i);
                if (t != null && t.getMenuId() == menuItemId) {
                    sendTMSMessage(t, i, d);
                    return true;
                }
            }
        }

        return false;
    }

    private void sendTMSMessage(TattooMessage msg, int msgId, BLEDevice device) {
        if(device.connected()) {
            EventBus.getDefault().post(new RequestSendTMSEvent(device.getBluetoothDevice(),
                    new byte[] {(byte) msgId}));

            Toast.makeText(ctx, "Message sent.", Toast.LENGTH_SHORT).show();

            //TO DO: May need to display the message. Eg.m if the brief or alert dialog needs display.
            //displayTMSMessage(msg);

            int alternateID = msg.getAlternate();
            if(alternateID > 0) {
                //Hide this message from the menu
                msg.setIsAlternate(true);

                //Set the alternate as visible
                device.getTmsTxMessages().get(alternateID).setIsAlternate(false);
            }
        } else {
            Toast.makeText(ctx, "Cannot send message (tattoo disconnected)", Toast.LENGTH_SHORT).show();
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
                        String label = input.getText().toString();
                        Toast.makeText(ctx, "Event " + label + " marked.", Toast.LENGTH_SHORT).show();
                        fileManager.markEvent(label);
                    }
                })
                .setView(input)
                .show();
    }
}
