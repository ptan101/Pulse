package tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing;

import static tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser.gatherSubHeadings;
import static tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser.getInitFileSection;
import static tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing.MainSettingsParser.importDigitalDisplaySubSettings;
import static tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing.MainSettingsParser.importImageSubSettings;
import static tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing.MainSettingsParser.importValueAlertSettings;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import tan.philip.nrf_ble.Algorithms.Biometric;
import tan.philip.nrf_ble.Algorithms.BiometricsSet;
import tan.philip.nrf_ble.Algorithms.PanTompkinsAlgorithm;
import tan.philip.nrf_ble.Algorithms.TimeOfFlightAlgorithm;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.BLE.PacketParsing.TattooMessage;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplaySettings;
import tan.philip.nrf_ble.GraphScreen.UIComponents.ValueAlert;

public class BiometricSettingsParser {
    private static final String TAG = "BiometricSettingsParser";
    private HashMap<Integer, SignalSetting> signalSettings;

    public BiometricSettingsParser(HashMap<Integer, SignalSetting> signalSettings) {
        this.signalSettings = signalSettings;
    }

    public void importBiometrics(ArrayList<String> initFileLines, BiometricsSet biometrics) {
        try {
            ArrayList<String> lines = getInitFileSection(initFileLines, "Biometric Settings");

            String cur_line;

            for(int i = 0; i < lines.size(); i ++) {
                cur_line = lines.get(i);
                if (cur_line.charAt(0) != '.') {
                    //Check what algorithm to import
                    selectBiometric(cur_line, biometrics);
                } else if (cur_line.charAt(0) == '.') {
                    //Input parameters to last Biometric
                    Biometric cur_algo = biometrics.get(biometrics.size() - 1);
                    parseBiometricMainOptions(cur_algo, cur_line, gatherSubHeadings(lines, i));
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Biometrics not imported.");
        }

        //Check and start all algorithms
        for (int i = 0; i < biometrics.size(); i++)
            biometrics.get(i).startAlgorithm();

        Log.d(TAG, "Biometrics imported.");
    }

    private void selectBiometric(String algorithmAndSignals, BiometricsSet biometrics) {
        String[] cur_line = algorithmAndSignals.split(": ");
        String algorithm = cur_line[0];
        String[] signal_ids = cur_line[1].split(", ");

        //Signals used in algorithm
        HashMap<Integer, SignalSetting> signals = new HashMap<>();

        for (String id : signal_ids) {
            //Convert signal id into integers
            int i = Integer.parseInt(id);
            signals.put(i, signalSettings.get(i));
        }
        byte index = (byte) (biometrics.size() + 1);

        switch (algorithm) {
            case "pan-tompkins":
                biometrics.add(new PanTompkinsAlgorithm(signals, index));
                break;
            case "tofs":
                biometrics.add(new TimeOfFlightAlgorithm(signals, index));
                break;
            default:
                break;
        }
    }

    private void parseBiometricMainOptions(Biometric biometric, String pLine, ArrayList<String> subheadings) {
        //Cut out the initial period
        pLine = pLine.substring(1);

        //Look at first word
        String[] mainOption = pLine.split(": ");

        switch (mainOption[0]) {
            case "digdisplay":
                biometric.initializeDigitalDisplaySettings();
                importDigitalDisplaySubSettings(biometric.ddSettings, subheadings);
                break;
            case "additionalParam":
                parseAdditionalParameters(biometric, subheadings);
                break;
            case "valueAlert":
                ValueAlert alert = new ValueAlert();
                biometric.addAlert(alert);
                importValueAlertSettings(alert, subheadings);
                break;
            case "log":
                if(mainOption[1].equals("True") || mainOption[1].equals("true"))
                    biometric.logData = true;
                break;
            case "image":
                biometric.setting.image = true;
                importImageSubSettings(biometric.setting, subheadings);
                break;
            default:
                break;
        }
    }

    private void parseAdditionalParameters(Biometric biometric, ArrayList<String> subsettings) {
        for(String s: subsettings) {
            biometric.addParameter(Float.parseFloat(s));
        }
    }
}
