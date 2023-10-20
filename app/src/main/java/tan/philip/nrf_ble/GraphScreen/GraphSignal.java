package tan.philip.nrf_ble.GraphScreen;

import android.content.Context;
import android.graphics.Color;

import com.jjoe64.graphview.series.Series;

import java.util.ArrayList;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.GraphSeries;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.ImageSeries;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.NumericalSeries;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.WaveformSeries;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplay;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplaySettings;
import tan.philip.nrf_ble.GraphScreen.UIComponents.GraphContainer;

public class  GraphSignal {

    private final SignalSetting settings;
    //private GraphContainer graphContainer;

    public ArrayList<GraphSeries> series = new ArrayList<>();
    private ArrayList<GraphContainer> graphContainers = new ArrayList<>();
    //private WaveformSeries waveformSeries;

    protected final float samplePeriod;

    public GraphSignal(SignalSetting settings) {
        this.settings = settings;
        samplePeriod = 1000 /  settings.fs;
    }

    public GraphContainer setupWaveformGraph(Context ctx, int monitor_length) {
        //interactive_series = new LineGraphSeries<>();
        WaveformSeries newSeries = new WaveformSeries(settings, monitor_length);
        series.add(newSeries);
        Series[] seriesToRender = {newSeries.getMonitor_series(), newSeries.getMonitor_mask()};
        GraphContainer graphContainer = new GraphContainer(ctx, this, seriesToRender);
        graphContainer.setViewportMinX(0);
        graphContainer.setViewportMaxX(monitor_length);
        return graphContainer;
    }

    public GraphContainer setupImageGraph(Context ctx, int nLines) {
        ImageSeries newSeries = new ImageSeries(settings);
        series.add(newSeries);
        Series[] seriesToRender = {newSeries.getSeries()};
        GraphContainer graphContainer = new GraphContainer(ctx, this, seriesToRender);
        graphContainer.setViewportMinX(0);
        graphContainer.setViewportMaxX(nLines);
        graphContainer.setViewportMaxY(30);
        return graphContainer;
    }

    public void queueDataPoints(ArrayList<Float> newDataPoints, long curTime) {
        for (GraphSeries s : series)
            s.queueDataPoints(newDataPoints, curTime);
    }

    public void setLastUpdateTime(long curTime) {
        for (GraphSeries s : series)
            s.setLastUpdateTime(curTime);
    }

    public void setAutoscale(boolean autoscale) {
        for(GraphContainer g : graphContainers)
            g.setAutoscale(autoscale);
    }

    public void setColor(int[] color) {
        settings.color = color;

        for(GraphSeries s : series)
            s.setColor(color);
    }

    public int getColorARGB() {
        return Color.argb(settings.color[3], settings.color[0], settings.color[1], settings.color[2]);
    }

    /////////////////////////////////////////////Getters///////////////////////////////////////////
    public float getSample_period() {
        return samplePeriod;
    }


    public String getName() {
        return settings.name;
    }

    public int getBitResolution() {
        return settings.bitResolution;
    }

    public boolean isGraphable() {
        return settings.graphable;
    }

    public boolean useDigitalDisplay() {
        return settings.ddSettings != null;
    }

    public ArrayList<GraphSeries> getSeries() {return series;}

    public int[] getColor() {
        return settings.color;
    }

    public int getLayoutHeight() {
        return settings.graphHeight;
    }

    public boolean imageable() {
        return settings.image;
    }

    public DigitalDisplaySettings getDigitalDisplaySettings() { return settings.ddSettings; }

    public int getNumImageLines() { return settings.nImageLines; }

    ///////////////////////////////////////Setters/////////////////////////////////////////////////
    public void addDigitalDisplay(DigitalDisplay display) {
        NumericalSeries nSeries = new NumericalSeries(settings, display);
        series.add(nSeries);
    }
}