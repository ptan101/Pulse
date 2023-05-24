package tan.philip.nrf_ble.BLE.PacketParsing.SubheaderParsing;

import static tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser.gatherSubHeadings;
import static tan.philip.nrf_ble.BLE.PacketParsing.BLEPacketParser.getInitFileSection;

import android.util.Log;

import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplaySettings;
import tan.philip.nrf_ble.Algorithms.Filter.Filter;
import tan.philip.nrf_ble.GraphScreen.UIComponents.ValueAlert;

public class MainSettingsParser {
    private static final String TAG = "MainSettingsParser";

    /**
     * Imports the signal settings from an .init file.
     * @param initFileLines All lines in the .init file
     * @param signalSettings HashMap of signalSettings where keys represent the listed indices.
     * @return initFileGood Whether the section had issues or not
     */
    public static boolean importSignalSettings(ArrayList<String> initFileLines, HashMap<Integer, SignalSetting> signalSettings) {
        try {
            ArrayList<String> lines = getInitFileSection(initFileLines, "Signals Information");

            String cur_line;

            //Keep reading lines until "end" is reached.
            for(int i = 0; i < lines.size(); i ++) {
                //Read the current line
                cur_line = lines.get(i);

                if(cur_line.charAt(0) != '.') {
                    //If the current line is a main heading, construct a new signalSetting object.
                    String[] signal_info = cur_line.split(", ");
                    SignalSetting cur_setting = new SignalSetting(Byte.parseByte(signal_info[0]), signal_info[1],
                            Byte.parseByte(signal_info[2]), Integer.parseInt(signal_info[3]),
                            Byte.parseByte(signal_info[4]), signal_info[5].equals("signed"));
                    signalSettings.put((int) cur_setting.index, cur_setting);

                } else if (cur_line.charAt(0) == '.' && !cur_line.startsWith("..")) {
                    //If the current line is a subheading, add the sub settings to the current signalSetting.
                    parseSignalMainOptions(signalSettings.get(signalSettings.size() - 1),
                            cur_line,
                            gatherSubHeadings(lines, i));
                }
            }

            return true;
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to read .init file Signal Information.");
            return false;
        }
    }

    /**
     * Function to determine the order of signals in the packet.
     * @param initFileLines All lines in the .init file
     * @param signalOrder Array where the order should be put into
     * @return
     */
    public static boolean importPacketStructure(ArrayList<String> initFileLines, ArrayList<Byte> signalOrder) {
        try {
            ArrayList<String> lines = getInitFileSection(initFileLines, "Packet Structure");

            for (String cur_line : lines)
                signalOrder.add(Byte.parseByte(cur_line));

            return true;
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to read .init file Packet Structure.");
            return false;
        }
    }

    /**
     * Function to read subheadings
     * @param signalSetting Current signalSetting that is being modified.
     * @param pLine Current line containing the specific setting to be modified.
     * @param subheadings Subsettings associated with current setting.
     */
    private static void parseSignalMainOptions(SignalSetting signalSetting, String pLine, ArrayList<String> subheadings) {
        //Cut out the initial period
        pLine = pLine.substring(1);

        //Look at first word
        String[] mainOption = pLine.split(": ");

        switch (mainOption[0]) {
            case "graphable":
                signalSetting.graphable = true;
                importGraphableSubSettings(signalSetting, subheadings);
                break;
            case "digdisplay":
                signalSetting.ddSettings = new DigitalDisplaySettings(signalSetting.name);
                importDigitalDisplaySubSettings(signalSetting.ddSettings, subheadings);
                break;
            case "valueAlert":
                //To Do
                break;
            case "filter":
                importFilterSubSettings(signalSetting, subheadings);
                break;
            case "big":
                signalSetting.littleEndian = false;
                break;
            case "sickbayID":
                signalSetting.sickbayID = Integer.parseInt(mainOption[1]);
                break;
            default:
                break;
        }

    }

    /**
     *
     * @param signalSetting
     * @param subsettings
     */
    private static void importGraphableSubSettings(SignalSetting signalSetting, ArrayList<String> subsettings) {
        for(String s: subsettings) {
            String[] options = s.split(": ");

            switch (options[0]) {
                case "color":
                    int[] color = new int[4];
                    String[] color_s = options[1].split(" ");

                    if(color_s.length != 4)
                        Log.e(TAG, "Color " + options[1] + " needs to be RGBA.");

                    for (int i = 0; i < 4; i++) {
                        color[i] = Integer.parseInt(color_s[i]);
                    }
                    signalSetting.color = color;
                    break;
                case "graphHeight":
                    signalSetting.graphHeight = Integer.parseInt(options[1]);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     *
     * @param signalSetting
     * @param subsettings
     */
    private static void importFilterSubSettings(SignalSetting signalSetting, ArrayList<String> subsettings) {
        double[] b = new double[0];
        double[] a = new double[0];
        float gain = 1;

        for(String s: subsettings) {
            String[] options = s.split(": ");

            switch (options[0]) {
                case "b":
                    String[] b_s = options[1].split(", ");
                    double[] b_f = new double[b_s.length];

                    for (int i = 0; i < b_s.length; i++) {
                        b_f[i] = Double.parseDouble(b_s[i]);
                    }
                    b = b_f;
                    break;
                case "a":
                    String[] a_s = options[1].split(", ");
                    double[] a_f = new double[a_s.length];

                    for (int i = 0; i < a_s.length; i++) {
                        a_f[i] = Double.parseDouble(a_s[i]);
                    }
                    a = a_f;
                    break;
                case "gain":
                    gain = Float.parseFloat(options[1]);
                    break;
                default:
                    break;
            }
        }

        signalSetting.filter = new Filter(b, a, gain);
    }

    /**
     *
     * @param ddSettings
     * @param subsettings
     */
    public static void importDigitalDisplaySubSettings(DigitalDisplaySettings ddSettings, ArrayList<String> subsettings) {
        for(String s: subsettings) {
            String[] options = s.split(": ");

            switch (options[0]) {
                case "DecimalFormat":
                    ddSettings.decimalFormat = new DecimalFormat(options[1]);
                    break;
                case "conversion":
                    ddSettings.conversion = options[1];
                    break;
                case "prefix":
                    Properties p = new Properties();
                    try {
                        p.load(new StringReader("key="+options[1]));
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to convert escape sequence" + options[1]);
                    }
                    ddSettings.prefix = p.getProperty("key");
                    ddSettings.prefix.replaceAll(" ", "&nbsp;");
                    ddSettings.prefix.replaceAll("_", "&nbsp;");
                    break;
                case "suffix":
                    p = new Properties();
                    try {
                        p.load(new StringReader("key="+options[1]));
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to convert escape sequence" + options[1]);
                    }
                    ddSettings.suffix = p.getProperty("key").replaceAll(" ", "&nbsp;");
                    ddSettings.suffix = p.getProperty("key").replaceAll("_", "&nbsp;");

                    break;
                case "displayName":
                    ddSettings.displayName = options[1];
                    break;
                default:
                    break;
            }
        }
    }

    public static void importValueAlertSettings(ValueAlert valueAlert, ArrayList<String> subsettings) {
        for(String s: subsettings) {
            String[] options = s.split(": ");

            switch (options[0]) {
                case "belowAlert":
                    valueAlert.aboveAlert = false;
                    break;
                case "theshold":
                    valueAlert.threshold = Float.parseFloat(options[1]);
                    break;
                case "title":
                    valueAlert.title = options[1];
                case "message":
                    valueAlert.message = options[1];
                default:
                    break;
            }
        }
    }
}
