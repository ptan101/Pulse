package tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing;

import static tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser.getInitFileSection;

import android.util.Log;

import java.util.ArrayList;

import tan.philip.nrf_ble.SickbayPush.SickbayImportSettings;

public class SickbaySettingsParser {

    private static final String TAG = "SickbayOptionsParser";

    public static SickbayImportSettings parseSickbayOptions(ArrayList<String> initFileLines) {
        SickbayImportSettings sickbaySettings= new SickbayImportSettings();

        try {
            ArrayList<String> lines = getInitFileSection(initFileLines, "Sickbay Settings");

            String cur_line;

            //Keep reading lines until "end" is reached.
            for(int i = 0; i < lines.size(); i ++) {
                cur_line = lines.get(i);

                if (cur_line.charAt(0) == '.') {
                    //Look at first word
                    String[] settings = cur_line.substring(1).split(": ");

                    switch (settings[0]) {
                        case "sickbayNS":
                            sickbaySettings.setNamespace(settings[1]);
                            break;
                        default:
                            break;
                    }
                }
            }
        }  catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Sickbay settings not imported.");
        }

        return sickbaySettings;
    }
}
