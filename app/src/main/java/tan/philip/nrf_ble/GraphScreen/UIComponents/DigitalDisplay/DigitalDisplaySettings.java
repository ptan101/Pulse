package tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay;

import java.text.DecimalFormat;

public class DigitalDisplaySettings {
    public String conversion = "x";
    public String prefix = "";
    public String suffix = "";
    public String displayName;
    public String iconName = "";
    public static final int maxTextLen = 12;

    public DecimalFormat decimalFormat = new DecimalFormat("###.#");

    public DigitalDisplaySettings(String displayName) {
        this.displayName = displayName;
    }
}
