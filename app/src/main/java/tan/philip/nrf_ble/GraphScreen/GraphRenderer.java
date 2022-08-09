package tan.philip.nrf_ble.GraphScreen;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.BLETattooDevice;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.UIComponents.GraphContainer;

public class GraphRenderer {
    private int mDisplayRateMS = 1000 / 30;
    private Handler mRenderHandler;
    private boolean firstData = true;

    private HashMap<String, HashMap<Integer, GraphSignal>> signals;
    private boolean isRunning = false;

    public GraphRenderer(Context ctx, ArrayList<BLEDevice> bleDevices) {
        this.signals = new HashMap<>();

        setupGraphSignals(bleDevices);
        setupRenderer();
    }

    public void deregister() {
        mRenderHandler.removeCallbacks(renderLoop);
    }

    public synchronized void queueDataPoints(String address, HashMap<Integer, ArrayList<Float>> newDataPoints) {
        HashMap<Integer, GraphSignal> signalsInDevice = signals.get(address);
        long curTime = System.currentTimeMillis();
        for(Integer i : signalsInDevice.keySet()) {
            signalsInDevice.get(i).queueDataPoints(newDataPoints.get(i), curTime);
        }

        //Update the last update time of the signal. Otherwise, initial rendering will be glitchy
        if(firstData) {
            for(Integer i : signalsInDevice.keySet()) {
                signalsInDevice.get(i).setLastUpdateTime(curTime);
            }
            firstData = false;
        }
    }

    public void addGraphContainersToView(ViewGroup v, Context ctx) {
        for (String s : signals.keySet()) {
            HashMap<Integer, GraphSignal> signalsInDevice = signals.get(s);

            for(Integer i : signalsInDevice.keySet()) {
                GraphContainer newView = signalsInDevice.get(i).setupGraph(ctx, 0 , 10);
                v.addView(newView);
            }
        }
    }

    private void setupGraphSignals(ArrayList<BLEDevice> bleDevices) {
        for(BLEDevice bleDevice : bleDevices) {
            if(bleDevice instanceof BLETattooDevice) {
                HashMap<Integer, GraphSignal> signalsInDevice = new HashMap<>();

                for(Integer i : bleDevice.getSignalSettings().keySet()) {
                    SignalSetting setting = bleDevice.getSignalSettings().get(i);
                    if (setting.graphable) {
                        signalsInDevice.put((int) setting.index, new GraphSignal(setting));
                    }
                }

                signals.put(bleDevice.getAddress(), signalsInDevice);
            }
        }
    }

    private void setupRenderer() {
        mRenderHandler = new Handler();
        mRenderHandler.postDelayed(renderLoop, mDisplayRateMS);
    }

    private Runnable renderLoop = new Runnable() {
        @Override
        public void run() {
            updateDisplay();

            mRenderHandler.postDelayed(this, mDisplayRateMS);
        }
    };

    private void updateDisplay() {
        for (String s : signals.keySet()) {
            HashMap<Integer, GraphSignal> signalsInDevice = signals.get(s);

            for(Integer i : signalsInDevice.keySet()) {
                dequeueDataPoint(signalsInDevice.get(i));
            }
        }
    }

    private void dequeueDataPoint(GraphSignal signal) {
        //Dequeue and display as many datapoints that should be plotted in the display interval.
        long curTime = System.currentTimeMillis();

        //Points to render [n] = Time between renders [s] x samples per time [n/s]
        int numPointsToRender = (int)((curTime - signal.getLastUpdateTime()) * signal.fs / 1000);
        /*
        Log.d("dt", (curTime - signal.getLastUpdateTime()) + "");
        Log.d("npr", numPointsToRender + "\n");
         */

        //In case that there are too many points in the queue, we should speed up the rendering
        if (signal.samplesInQueue() > signal.getNumPointsLastEnqueued())
            numPointsToRender *= 2;

        //Only plot as many points that are available in the buffer
        numPointsToRender = Math.min(signal.samplesInQueue(), numPointsToRender);

        //Don't try to render negative number of points
        if (numPointsToRender <= 0)
            return;

        signal.updateSeriesFromQueue(curTime, numPointsToRender);
    }
}