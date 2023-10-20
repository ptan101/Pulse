package tan.philip.nrf_ble.GraphScreen.GraphSeries;

import android.graphics.Color;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;

public class ImageSeries extends GraphSeries{
    private PointsGraphSeries<DataPoint> timeOfFlights = new PointsGraphSeries<>();

    private int nLines;
    private int nToFsPerLine = 5;
    private DataPoint[] tofsBuffer;
    private int numPoints = 0;
    private float maxDepth = 20; //mm

    public ImageSeries(SignalSetting settings) {
        super(settings);

        this.nLines = settings.nImageLines;
        tofsBuffer = new DataPoint[nLines * nToFsPerLine];
        //Initialize all DataPoints so there are no null elements
        for(int i = 0; i < tofsBuffer.length; i ++) {
            tofsBuffer[i] = new DataPoint(i / nToFsPerLine, maxDepth);
        }

        //Set the series marker size and style
        timeOfFlights.setSize(3);
        timeOfFlights.setColor(Color.BLACK);
    }

    @Override
    public void updateSeriesFromQueue(long time, int numPointsToRender) {
        //Load in a certain number of datapoints from the buffer to render.
        float[] newData = new float[numPointsToRender];
        for (int i = 0; i < numPointsToRender; i ++)
            newData[i] = renderQueue.poll();

        lastUpdateTime = time;

        //Plot this data
        for(int i = 0; i < newData.length; i++) {
            float display_line = (numPoints / nToFsPerLine) % nLines;
            int cur_index = numPoints % tofsBuffer.length;

            tofsBuffer[cur_index] = new DataPoint(display_line, maxDepth - newData[i]);
            //tofsBuffer[cur_index] = new DataPoint(display_line, 0);
            numPoints++;
        }

        timeOfFlights.resetData(tofsBuffer);
    }

    @Override
    public void setColor(int[] color) {

    }

    public PointsGraphSeries<DataPoint> getSeries() {
        return timeOfFlights;
    }

    public int getNToFsPerLine() {
        return nToFsPerLine;
    }
}
