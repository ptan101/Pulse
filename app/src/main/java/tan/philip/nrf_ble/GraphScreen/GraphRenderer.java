package tan.philip.nrf_ble.GraphScreen;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.BLETattooDevice;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.Events.UIRequests.RequestChangeAutoScaleAll;
import tan.philip.nrf_ble.Events.UIRequests.RequestChangeRecordEvent;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplayManager;
import tan.philip.nrf_ble.GraphScreen.UIComponents.GraphContainer;

public class GraphRenderer {
    private final int mDisplayRateMS = 1000 / 30;
    private Handler mRenderHandler;
    private boolean firstData = true;

    private final HashMap<String, HashMap<Integer, GraphSignal>> signals;

    private final TextView recordTimer;
    private boolean recording = false;
    private long startRecordTime;
    private final DigitalDisplayManager displayManager;

    public GraphRenderer(Context ctx, ArrayList<BLEDevice> bleDevices, TextView recordTimer, ConstraintLayout layout) {
        this.signals = new HashMap<>();
        this.recordTimer = recordTimer;
        this.displayManager = new DigitalDisplayManager(ctx, layout);

        setupGraphSignals(bleDevices, ctx);
        setupRenderer();

        //Register activity on EventBus
        EventBus.getDefault().register(this);
    }

    public void deregister() {
        mRenderHandler.removeCallbacks(renderLoop);
        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
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
                GraphSignal signal = signalsInDevice.get(i);
                if (signal.isGraphable()) {
                    GraphContainer newView = signal.setupGraph(ctx, 10);
                    v.addView(newView);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void recordStateChanged(RequestChangeRecordEvent event) {
        if(event.startRecord())
            startRecord();
        else
            stopRecord();
    }

    public void startRecord() {
        startRecordTime = System.currentTimeMillis();
        recordTimer.setVisibility(View.VISIBLE);
        recordTimer.setText("Record length: " + 0.00 + "s");
        recording = true;
    }

    public void stopRecord() {
        recordTimer.setVisibility(View.INVISIBLE);
        recording = false;
    }

    private void setupGraphSignals(ArrayList<BLEDevice> bleDevices, Context ctx) {
        for(BLEDevice bleDevice : bleDevices) {
            if(bleDevice instanceof BLETattooDevice) {
                HashMap<Integer, GraphSignal> signalsInDevice = new HashMap<>();

                //Create  GraphSignals for all plottable signals
                for(Integer i : bleDevice.getSignalSettings().keySet()) {
                    SignalSetting setting = bleDevice.getSignalSettings().get(i);
                    GraphSignal newSignal = new GraphSignal(setting);

                    if (setting.graphable)
                        signalsInDevice.put((int) setting.index, newSignal);
                    if (newSignal.useDigitalDisplay()) {
                        signalsInDevice.put((int) setting.index, newSignal);
                        DigitalDisplay display = new DigitalDisplay(ctx, newSignal.getName(), "temperature");
                        displayManager.addToDigitalDisplay(display);
                        newSignal.setDigitalDisplay(display);
                    }

                }

                signals.put(bleDevice.getAddress(), signalsInDevice);
            }
        }

        if(displayManager.getDigitalDisplays().size() == 0)
            displayManager.disableDigitalDisplay();
        //displayManager.finishDigitalDisplays();
    }

    private void setupRenderer() {
        mRenderHandler = new Handler();
        mRenderHandler.postDelayed(renderLoop, mDisplayRateMS);
    }

    private final Runnable renderLoop = new Runnable() {
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

        if(recording) {
            int totalSecs = (int) (System.currentTimeMillis() - startRecordTime) / 1000;

            int hours = totalSecs / 3600;
            int minutes = (totalSecs % 3600) / 60;
            int seconds = totalSecs % 60;

            recordTimer.setText("Record length: " + String.format("%02d:%02d:%02d", hours, minutes, seconds));
        }

    }

    @Subscribe(threadMode =  ThreadMode.MAIN)
    public void toggleAllAutoscale(RequestChangeAutoScaleAll event) {
        for(String i: signals.keySet()) {
            for(Integer j : signals.get(i).keySet())
                signals.get(i).get(j).setAutoscale(event.getAutoscale());
        }
    }

    private void dequeueDataPoint(GraphSignal signal) {
        //Dequeue and display as many datapoints that should be plotted in the display interval.
        long curTime = System.currentTimeMillis();

        //Points to render [n] = Time between renders [s] x samples per time [n/s]
        int numPointsToRender = (int)((curTime - signal.getLastUpdateTime()) * signal.getFs() / 1000);
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

    public boolean isEmpty() {
        return signals.isEmpty();
    }
}