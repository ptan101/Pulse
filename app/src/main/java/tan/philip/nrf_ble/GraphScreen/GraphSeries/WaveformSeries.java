package tan.philip.nrf_ble.GraphScreen.GraphSeries;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;

public class WaveformSeries extends GraphSeries{

    private LineGraphSeries<DataPoint> monitor_series;
    private PointsGraphSeries<DataPoint> monitor_mask;
    private final DataPoint[] mask = new DataPoint[1];
    private DataPoint[] monitor_buffer;
    private int numPoints;
    private final float samplePeriod;
    public WaveformSeries(SignalSetting settings, int monitor_length) {
        super(settings);
        samplePeriod = 1000 /  settings.fs;
        monitor_series = new LineGraphSeries<>();
        monitor_buffer = new DataPoint[settings.fs * monitor_length];
        monitor_mask = new PointsGraphSeries<>();
        setMaskSettings(android.R.color.background_light);
        resetSeries();
    }

    public synchronized void updateSeriesFromQueue(long time, int numPointsToRender) {
        float[] newData = new float[numPointsToRender];
        for (int i = 0; i < numPointsToRender; i ++)
            newData[i] = renderQueue.poll();

        addDataToMonitorBuffer(newData, time);

        //Display on the Digital Display, if necessary
        /*
        if(settings.ddSettings != null)
            this.setDigitalDisplayText(newData[newData.length - 1]);

         */
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

    /**
     * Plots the data on the patient monitor
     * @param data Y value of new data to plot
     * @return The x value where it was plotted
     */
    public float addDataToMonitorBuffer(float[] data, long time) {
        lastUpdateTime = time;

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

    @Override
    public void setColor(int[] color) {
        setSeriesPaint(color[3], color[0], color[1], color[2], 5, monitor_series);
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

    public LineGraphSeries<DataPoint> getMonitor_series() { return monitor_series; }

    public PointsGraphSeries<DataPoint> getMonitor_mask() {return monitor_mask; }
}
