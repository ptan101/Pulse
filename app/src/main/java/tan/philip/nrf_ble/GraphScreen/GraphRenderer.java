package tan.philip.nrf_ble.GraphScreen;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.Algorithms.Biometric;
import tan.philip.nrf_ble.BLE.BLEDevices.BLEDevice;
import tan.philip.nrf_ble.BLE.BLEDevices.BLETattooDevice;
import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.Events.UIRequests.RequestChangeAutoScaleAllEvent;
import tan.philip.nrf_ble.Events.UIRequests.RequestChangeRecordEvent;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.GraphSeries;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplay;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplayManager;
import tan.philip.nrf_ble.GraphScreen.UIComponents.GraphContainer;

public class GraphRenderer {
    private final int mDisplayRateMS = 1000 / 30;
    private Handler mRenderHandler;
    private boolean firstData = true;

    private final HashMap<String, HashMap<Integer, GraphSignal>> signals;
    //For the I/Q ICG experiment: maps device address -> {ICGI index, ICGQ index}.
    private final HashMap<String, int[]> icgIQIndex = new HashMap<>();

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

        //I/Q build: hand this packet's Q batch to the complex ICGI processor before dispatch,
        //so it can demodulate I against the matching Q (both are in newDataPoints).
        int[] iq = icgIQIndex.get(address);
        if (iq != null) {
            GraphSignal isig = signalsInDevice.get(iq[0]);
            if (isig instanceof ICGComplexGraphSignal) {
                ArrayList<Float> qData = newDataPoints.get(iq[1]);
                if (qData != null)
                    ((ICGComplexGraphSignal) isig).setPendingQ(qData);
            }
        }

        for(Integer i : signalsInDevice.keySet()) {
            //Skip null elements (negative indices). I am currently using them for Biometrics but they queue points
            //separately.
            ArrayList<Float> points = newDataPoints.get(i);
            if(points != null)
                signalsInDevice.get(i).queueDataPoints(points, curTime);
        }

        //Update the last update time of the signal. Otherwise, initial rendering will be glitchy
        //I.e., the deltaT will be negative, leading to negative points to render
        if(firstData) {
            for(Integer i : signalsInDevice.keySet()) {
                signalsInDevice.get(i).setLastUpdateTime(curTime);
            }
            firstData = false;
        }
    }

    public void addGraphContainersToView(ViewGroup v, Context ctx) {
        //Add graph containers for signals
        for (String s : signals.keySet()) {
            HashMap<Integer, GraphSignal> signalsInDevice = signals.get(s);

            for(Integer i : signalsInDevice.keySet()) {
                GraphSignal signal = signalsInDevice.get(i);
                if (signal.isGraphable()) {
                    if (signal instanceof ICGGraphSignal) {
                        //ICG: raw Z + derived cIPG-Z(t) + -dZ/dt, stacked top to bottom.
                        for (GraphContainer c : ((ICGGraphSignal) signal).setupICGGraphs(ctx, 10))
                            v.addView(c);
                    } else {
                        GraphContainer newView = signal.setupWaveformGraph(ctx, 10);
                        v.addView(newView);
                    }
                }

                if (signal.imageable()) {
                    GraphContainer newView = signal.setupImageGraph(ctx, signal.getNumImageLines());
                    v.addView((newView));
                }

                if (signal.useDigitalDisplay()) {
                    DigitalDisplay display = new DigitalDisplay(ctx, signal.getDigitalDisplaySettings());
                    displayManager.addToDigitalDisplay(display);
                    signal.addDigitalDisplay(display);
                }
            }
        }
        if(displayManager.getDigitalDisplays().size() == 0)
            displayManager.disableDigitalDisplay();
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
                for (Integer i : bleDevice.getSignalSettings().keySet()) {
                    SignalSetting setting = bleDevice.getSignalSettings().get(i);
                    //Special-case the ICG channels: derived cIPG-Z(t)/-dZ/dt graphs + mod%.
                    //  "ICG"  = on-device magnitude; "ICGI" = host I/Q complex demod build.
                    GraphSignal newSignal;
                    if ("ICG".equalsIgnoreCase(setting.name))
                        newSignal = new ICGGraphSignal(setting);
                    else if ("ICGI".equalsIgnoreCase(setting.name))
                        newSignal = new ICGComplexGraphSignal(setting);
                    else
                        newSignal = new GraphSignal(setting);

                    if (setting.graphable || setting.image || newSignal.useDigitalDisplay())
                        signalsInDevice.put((int) setting.index, newSignal);
                }

                //Add digital displays for Biometrics
                ArrayList<Biometric> biometrics = bleDevice.getBiometrics().getBiometrics();
                HashMap<Integer, GraphSignal> biometricGraphsInDevice = new HashMap<>();
                for (Biometric biometric : biometrics) {
                    GraphSignal newSignal = new GraphSignal(biometric.setting);
                    newSignal.series = biometric.graphSeries;

                    if (biometric.setting.graphable || biometric.setting.image || biometric.setting.ddSettings != null)
                        signalsInDevice.put(-biometric.setting.index, newSignal);

                }
                signals.put(bleDevice.getAddress(), signalsInDevice);

                //Pair ICGI (complex demod) with ICGQ so we can hand Q in per packet.
                int iIdx = -1, qIdx = -1;
                for (Integer k : bleDevice.getSignalSettings().keySet()) {
                    SignalSetting s = bleDevice.getSignalSettings().get(k);
                    if ("ICGI".equalsIgnoreCase(s.name)) iIdx = s.index;
                    else if ("ICGQ".equalsIgnoreCase(s.name)) qIdx = s.index;
                }
                if (iIdx >= 0 && qIdx >= 0)
                    icgIQIndex.put(bleDevice.getAddress(), new int[]{iIdx, qIdx});
            }
        }
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
                for (GraphSeries ser : signalsInDevice.get(i).getSeries())
                    dequeueDataPoint(ser);
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
    public void toggleAllAutoscale(RequestChangeAutoScaleAllEvent event) {
        for(String i: signals.keySet()) {
            for(Integer j : signals.get(i).keySet())
                signals.get(i).get(j).setAutoscale(event.getAutoscale());
        }
    }

    private void dequeueDataPoint(GraphSeries series) {
        //Dequeue and display as many datapoints that should be plotted in the display interval.
        long curTime = System.currentTimeMillis();

        //Points to render [n] = Time between renders [s] x samples per time [n/s]
        int numPointsToRender = (int)((curTime - series.getLastUpdateTime()) * series.getFs() / 1000);
        /*
        Log.d("dt", (curTime - series.getLastUpdateTime()) + "");
        Log.d("npr", numPointsToRender + "\n");
         */

        //In case that there are too many points in the queue, we should speed up the rendering
        if (series.samplesInQueue() > series.getNumPointsLastEnqueued())
            numPointsToRender *= 2;

        //Only plot as many points that are available in the buffer
        numPointsToRender = Math.min(series.samplesInQueue(), numPointsToRender);

        //Don't try to render negative number of points
        if (numPointsToRender <= 0)
            return;

        series.updateSeriesFromQueue(curTime, numPointsToRender);
    }

    public boolean isEmpty() {
        return signals.isEmpty();
    }
}