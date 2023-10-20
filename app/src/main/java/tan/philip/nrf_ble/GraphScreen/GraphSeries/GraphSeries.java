package tan.philip.nrf_ble.GraphScreen.GraphSeries;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;

public abstract class GraphSeries {
    protected SignalSetting settings;
    protected long lastUpdateTime = 0;
    //For rendering
    ConcurrentLinkedQueue<Float> renderQueue;
    int numPointsLastEnqueued = 0;

    public GraphSeries(SignalSetting settings) {
        this.settings = settings;
        this.renderQueue = new ConcurrentLinkedQueue<>();
    }

    public synchronized void queueDataPoints(ArrayList<Float> newDataPoints, long curTime) {
        this.numPointsLastEnqueued = newDataPoints.size();

        renderQueue.addAll(newDataPoints);
    }
    public int getFs() {
        return settings.fs;
    }

    public synchronized void setLastUpdateTime(long curTime) {
        this.lastUpdateTime = curTime;
    }

    public abstract void updateSeriesFromQueue(long time, int numPointsToRender);

    public abstract void setColor(int[] color);

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    public int samplesInQueue() { return renderQueue.size(); }

    public int getNumPointsLastEnqueued() {
        return numPointsLastEnqueued;
    }
}
