package tan.philip.nrf_ble.GraphScreen.GraphSeries;

import java.math.BigDecimal;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplay;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplayManager;

public class NumericalSeries extends GraphSeries{
    private DigitalDisplay digitalDisplay;

    public NumericalSeries(SignalSetting settings, DigitalDisplay digitalDisplay) {
        super(settings);
        this.digitalDisplay = digitalDisplay;
    }

    @Override
    public void updateSeriesFromQueue(long time, int numPointsToRender) {
        float[] newData = new float[numPointsToRender];
        for (int i = 0; i < numPointsToRender; i ++)
            newData[i] = renderQueue.poll();

        if(settings.ddSettings != null)
            this.setDigitalDisplayText(newData[newData.length - 1]);

        lastUpdateTime = time;
    }

    @Override
    public void setColor(int[] color) {
        //No need to do anything here. I don't set the color of the digital display.
    }

    public void setDigitalDisplayText(Float data) {
        //Convert the signal packet into the desired format

        //First, replace 'x' in the string with actual data
        String func = settings.ddSettings.conversion.replace("x", new BigDecimal(data).toPlainString());

        //Now, evaluate the function
        double evaluation = DigitalDisplayManager.eval(func);

        //Format text and display
        this.digitalDisplay.changeValue((float) evaluation);
    }

    public DigitalDisplay getDigitalDisplay() {
        return digitalDisplay;
    }

    public void setDigitalDisplay(DigitalDisplay digitalDisplay) {
        this.digitalDisplay = digitalDisplay;
    }
}
