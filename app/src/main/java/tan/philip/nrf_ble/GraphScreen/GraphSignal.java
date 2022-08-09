package tan.philip.nrf_ble.GraphScreen;

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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay;
import tan.philip.nrf_ble.GraphScreen.UIComponents.GraphContainer;

public class  GraphSignal {
    int fs;
    int sample_period;
    float offset;
    int num_points = 0;
    String name;
    int bitResolution;

    //For graphable
    boolean graphable = false;
    GraphContainer graphContainer;
    //LineGraphSeries<DataPoint> interactive_series;
    LineGraphSeries<DataPoint> monitor_series;
    private PointsGraphSeries<DataPoint> monitor_mask;
    private final DataPoint[] mask = new DataPoint[1];
    DataPoint[] monitor_buffer;
    int[] color;

    //For Digital Display
    boolean useDigitalDisplay = false;          //Probably could remove this and just check if DD is null
    DigitalDisplay digitalDisplay = null;
    DecimalFormat decimalFormat = new DecimalFormat("###.#");   //Default format in case user doesn't define one
    String conversion = "x";                                            //Default conversion (output exactly what is given)
    String prefix = "";
    String suffix = "";

    //For rendering
    ConcurrentLinkedQueue<Float> renderQueue;
    int numPointsLastEnqueued = 0;
    long lastUpdateTime = 0;

    public GraphSignal(SignalSetting settings) {
        this.fs = settings.fs;
        this.name = settings.name;

        this.graphable = settings.graphable;
        this.useDigitalDisplay = settings.digitalDisplay;
        this.bitResolution = settings.bitResolution;

        this.color = settings.color;
        sample_period = 1000 /  fs;

        this.renderQueue = new ConcurrentLinkedQueue<>();
    }

    public GraphContainer setupGraph(Context ctx, float offset, int monitor_length) {
        this.offset = offset;

        //interactive_series = new LineGraphSeries<>();
        monitor_series = new LineGraphSeries<>();
        monitor_buffer = new DataPoint[fs * monitor_length];
        monitor_mask = new PointsGraphSeries<>();
        setMaskSettings(android.R.color.background_light);

        resetSeries();

        this.graphContainer = new GraphContainer(ctx, this);
        graphContainer.setViewportMinX(0);
        graphContainer.setViewportMaxX(monitor_length);

        return graphContainer;
    }

    public void resetSeries() {
        if(graphable) {
            //interactive_series = new LineGraphSeries<>();
            monitor_series = new LineGraphSeries<>();


            for (int i = 0; i < monitor_buffer.length; i++) {
                monitor_buffer[i] = new DataPoint(i * (float) sample_period / 1000f, offset);
            }
            monitor_series.resetData(monitor_buffer);
            setColor(color);
        }
    }

    /**
     * Plots the data on the patient monitor
     * @param data Y value of new data to plot
     * @return The x value where it was plotted
     */
    public float addDataToMonitorBuffer(float[] data) {
        int cur_index = num_points % monitor_buffer.length;
        float display_t = ((float) (cur_index * sample_period)) / 1000f;

        for(int i = 0; i < data.length; i++) {
            cur_index = num_points % monitor_buffer.length;
            display_t = ((float) (cur_index * sample_period)) / 1000f;

            monitor_buffer[cur_index] = new DataPoint(display_t, data[i]);
            num_points++;
        }

        monitor_series.resetData(monitor_buffer);

        //Draw mask over old monitor points
        mask[0] = new DataPoint(display_t, 0);
        monitor_mask.resetData(mask);


        return (float) (cur_index * sample_period) / 1000f;
    }

    public synchronized void queueDataPoints(ArrayList<Float> newDataPoints, long curTime) {
        this.numPointsLastEnqueued = newDataPoints.size();

        renderQueue.addAll(newDataPoints);
    }

    public synchronized void updateSeriesFromQueue(long time, int numPointsToRender) {
        float[] newData = new float[numPointsToRender];
        for (int i = 0; i < numPointsToRender; i ++)
            newData[i] = renderQueue.poll();

        addDataToMonitorBuffer(newData);
        //monitor_series.appendData(new DataPoint(time, renderQueue.poll()), true, 1000);
        lastUpdateTime = time;
    }

    public synchronized void setLastUpdateTime(long curTime) {
        this.lastUpdateTime = curTime;
    }

    public void setColor(int[] color) {
        this.color = color;
        //setSeriesPaint(color[3], color[0], color[1], color[2], 5, interactive_series);
        setSeriesPaint(color[3], color[0], color[1], color[2], 5, monitor_series);
    }

    public int getColorARGB() {
        return Color.argb(color[3], color[0], color[1], color[2]);
    }

    public boolean graphable() {
        return graphable;
    }

    public void startMonitorView() {
        monitor_series.resetData(monitor_buffer);
    }

    //Depending on if it is a graphable or digitial display, add the right UI
    public void setupDisplay(Context context) {
        //If graphable, set up legends
        if(this.graphable) {
            //Add a new TextView to the legend with the signal name
            ConstraintSet set = new ConstraintSet();
            TextView signal_name = new TextView(context);
            signal_name.setText(this.name);
            signal_name.setTextColor(this.getColorARGB());
            signal_name.setId(View.generateViewId());

            /*
            mBinding.graphLegendCL.addView(signal_name);

            set.clone(mBinding.graphLegendCL);
            if(legend.size() == 0) {
                set.connect(signal_name.getId(), ConstraintSet.TOP, mBinding.graphLegendCL.getId(), ConstraintSet.TOP, 20);
            } else {
                set.connect(signal_name.getId(), ConstraintSet.TOP, legend.get(legend.size() - 1).getId(), ConstraintSet.BOTTOM, 0);
            }
            set.connect(signal_name.getId(), ConstraintSet.END, mBinding.graphLegendCL.getId(), ConstraintSet.END, 60);
            set.applyTo(mBinding.graphLegendCL);
            legend.add(signal_name);

             */
        }

        //If digital display, set up digital display
        //Try making constraint layout for each
        if(this.useDigitalDisplay) {
            //Add a new TextView to the legend with the signal name
            //digitalDisplay = new DigitalDisplay(context, this.name, "temperature");
            //DigitalDisplay.addToDigitalDisplay(digitalDisplay, mBinding.digitalDisplayLeft, mBinding.digitalDisplayCenter, mBinding.digitalDisplayRight, digitalDisplays);
        }
    }

    public void setDigitalDisplayText(Float data) {
        //Convert the signal packet into the desired format

        //First, replace 'x' in the string with actual data
        String func = conversion.replace("x", Float.toString(data));

        //Now, evaluate the function
        Double evaluation = DigitalDisplay.eval(func);

        //Format text and display
        this.digitalDisplay.label.setText(Html.fromHtml(prefix + decimalFormat.format(evaluation) + suffix));
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
        return fs;
    }

    public int getSample_period() {
        return sample_period;
    }

    public float getOffset() {
        return offset;
    }

    public int getNum_points() {
        return num_points;
    }

    public String getName() {
        return name;
    }

    public int getBitResolution() {
        return bitResolution;
    }

    public boolean isGraphable() {
        return graphable;
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
        return color;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public int samplesInQueue() { return renderQueue.size(); }

    public int getNumPointsLastEnqueued() {
        return numPointsLastEnqueued;
    }
}