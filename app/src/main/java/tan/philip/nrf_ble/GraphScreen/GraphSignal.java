package tan.philip.nrf_ble.GraphScreen;

import static tan.philip.nrf_ble.Constants.convertPixelsToDp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintSet;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplayManager;
import tan.philip.nrf_ble.GraphScreen.UIComponents.GraphContainer;

public class  GraphSignal {

    private final SignalSetting settings;
    private GraphContainer graphContainer;
    //LineGraphSeries<DataPoint> interactive_series;
    private LineGraphSeries<DataPoint> monitor_series;
    private PointsGraphSeries<DataPoint> monitor_mask;
    private final DataPoint[] mask = new DataPoint[1];
    private DataPoint[] monitor_buffer;
    private int numPoints;

    //For Digital Display
    private DigitalDisplay digitalDisplay;
    private DecimalFormat decimalFormat = new DecimalFormat("###.#");   //Default format in case user doesn't define one
    private final float samplePeriod;

    //For rendering
    ConcurrentLinkedQueue<Float> renderQueue;
    int numPointsLastEnqueued = 0;
    long lastUpdateTime = 0;

    public GraphSignal(SignalSetting settings) {
        this.settings = settings;

        if(settings.decimalFormat != null)
            this.decimalFormat = new DecimalFormat(settings.decimalFormat);

        samplePeriod = 1000 /  settings.fs;

        this.renderQueue = new ConcurrentLinkedQueue<>();
    }

    public GraphContainer setupGraph(Context ctx, int monitor_length) {
        //interactive_series = new LineGraphSeries<>();
        monitor_series = new LineGraphSeries<>();
        monitor_buffer = new DataPoint[settings.fs * monitor_length];
        monitor_mask = new PointsGraphSeries<>();
        setMaskSettings(android.R.color.background_light);

        resetSeries();

        this.graphContainer = new GraphContainer(ctx, this);
        graphContainer.setViewportMinX(0);
        graphContainer.setViewportMaxX(monitor_length);

        return graphContainer;
    }

    public void resetSeries() {
        if(settings.graphable) {
            //interactive_series = new LineGraphSeries<>();
            monitor_series = new LineGraphSeries<>();


            for (int i = 0; i < monitor_buffer.length; i++) {
                monitor_buffer[i] = new DataPoint(i * samplePeriod / 1000f, 0);
            }
            monitor_series.resetData(monitor_buffer);
            setColor(settings.color);
        }
    }

    public void setAutoscale(boolean autoscale) {
        graphContainer.setAutoscale(autoscale);
    }

    /**
     * Plots the data on the patient monitor
     * @param data Y value of new data to plot
     * @return The x value where it was plotted
     */
    public float addDataToMonitorBuffer(float[] data) {
        int cur_index = numPoints % monitor_buffer.length;
        float display_t = ((float) (cur_index * samplePeriod)) / 1000f;

        for(int i = 0; i < data.length; i++) {
            cur_index = numPoints % monitor_buffer.length;
            display_t = ((float) (cur_index * samplePeriod)) / 1000f;

            monitor_buffer[cur_index] = new DataPoint(display_t, data[i]);
            numPoints++;
        }

        monitor_series.resetData(monitor_buffer);

        //Draw mask over old monitor points
        mask[0] = new DataPoint(display_t, 0);
        monitor_mask.resetData(mask);


        return (float) (cur_index * samplePeriod) / 1000f;
    }

    public synchronized void queueDataPoints(ArrayList<Float> newDataPoints, long curTime) {
        this.numPointsLastEnqueued = newDataPoints.size();

        renderQueue.addAll(newDataPoints);
    }

    public synchronized void updateSeriesFromQueue(long time, int numPointsToRender) {
        float[] newData = new float[numPointsToRender];
        for (int i = 0; i < numPointsToRender; i ++)
            newData[i] = renderQueue.poll();

        if(settings.graphable) {
            addDataToMonitorBuffer(newData);
            //monitor_series.appendData(new DataPoint(time, renderQueue.poll()), true, 1000);
            lastUpdateTime = time;
        }

        //Display on the Digital Display, if necessary
        if(settings.digitalDisplay)
            this.setDigitalDisplayText(newData[newData.length - 1]);
    }

    public synchronized void setLastUpdateTime(long curTime) {
        this.lastUpdateTime = curTime;
    }

    public void setColor(int[] color) {
        settings.color = color;
        //setSeriesPaint(color[3], color[0], color[1], color[2], 5, interactive_series);
        setSeriesPaint(color[3], color[0], color[1], color[2], 5, monitor_series);
    }

    public int getColorARGB() {
        return Color.argb(settings.color[3], settings.color[0], settings.color[1], settings.color[2]);
    }

    public void startMonitorView() {
        monitor_series.resetData(monitor_buffer);
    }

    public void setDigitalDisplayText(Float data) {
        //Convert the signal packet into the desired format

        //First, replace 'x' in the string with actual data
        String func = settings.conversion.replace("x", new BigDecimal(data).toPlainString());

        //Now, evaluate the function
        Double evaluation = DigitalDisplayManager.eval(func);

        //Format text and display
        this.digitalDisplay.label.setText(Html.fromHtml(settings.prefix + decimalFormat.format(evaluation) + settings.suffix));
    }

    private void setSeriesPaint(int a, int r, int g, int b, int strokeWidth, LineGraphSeries<DataPoint> series) {
        Paint sensorPaint = new Paint();
        sensorPaint.setARGB(a, r, g, b);
        sensorPaint.setStrokeWidth(strokeWidth);
        series.setCustomPaint(sensorPaint);
    }

    private void setMaskSettings(int color) {
        color = Color.WHITE;
        monitor_mask.setColor(color);
        monitor_mask.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                paint.setStrokeWidth(20);
                canvas.drawLine(x, y+1000, x, y-1000, paint);
            }
        });
    }

    /////////////////////////////////////////////Getters///////////////////////////////////////////

    public int getFs() {
        return settings.fs;
    }

    public float getSample_period() {
        return samplePeriod;
    }

    public int getNum_points() {
        return numPoints;
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
        return settings.digitalDisplay;
    }

    public GraphContainer getGraphContainer() {
        return graphContainer;
    }

    public LineGraphSeries<DataPoint> getMonitor_series() { return monitor_series; }

    public PointsGraphSeries<DataPoint> getMonitor_mask() {return monitor_mask; }

    public DataPoint[] getMonitor_buffer() {
        return monitor_buffer;
    }

    public int[] getColor() {
        return settings.color;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public int samplesInQueue() { return renderQueue.size(); }

    public int getNumPointsLastEnqueued() {
        return numPointsLastEnqueued;
    }

    public DigitalDisplay getDigitalDisplay() {
        return digitalDisplay;
    }

    public int getLayoutHeight() {
        return settings.graphHeight;
    }
    ///////////////////////////////////////Setters/////////////////////////////////////////////////

    public void setDigitalDisplay(DigitalDisplay digitalDisplay) {
        this.digitalDisplay = digitalDisplay;
    }
}