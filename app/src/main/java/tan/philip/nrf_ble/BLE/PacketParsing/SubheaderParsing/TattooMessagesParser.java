package tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing;

import static tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser.getInitFileSection;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.BLE.PacketParsing.TattooMessage;

public class TattooMessagesParser {
    private static final String TAG = "TattooMessagesParser";

    public static void importMessages(ArrayList<String> initFileLines, ArrayList<TattooMessage> messages, String sectionName) {
        try {
            ArrayList<String> lines = getInitFileSection(initFileLines, sectionName);

            String cur_line;

            for(int i = 0; i < lines.size(); i ++) {
                cur_line = lines.get(i);

                if (cur_line.charAt(0) != '.') {
                    TattooMessage cur_mesg = new TattooMessage(cur_line, false);
                    messages.add(cur_mesg);
                } else if (cur_line.charAt(0) == '.') {
                    parseMessageData(messages.get(messages.size() - 1), cur_line);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, sectionName + " not imported.");
        }
    }


    private static void parseMessageData(TattooMessage message, String pLine) {
        //Cut out the initial period
        String option = pLine.substring(1, pLine.indexOf(" "));
        String setting = pLine.substring(pLine.indexOf(" ") + 1);

        switch (option) {
            case "brief":
                message.setBrief(setting);
                break;
            case "alternate":
                message.setAlternate(Integer.parseInt(setting));
                break;
            case "isAlternate":
                message.setIsAlternate(Boolean.parseBoolean(setting));
                break;
            case "alertDialog":
                message.setAlertDialog(Boolean.parseBoolean(setting));
                break;
            case "sendTX":
                message.setIdTXMessage(Integer.parseInt(setting));
                break;
            default:
                break;
        }
    }
}
