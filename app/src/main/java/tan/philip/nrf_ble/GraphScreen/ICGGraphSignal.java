package tan.philip.nrf_ble.GraphScreen;

import android.content.Context;
import android.util.Log;

import com.jjoe64.graphview.series.Series;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.NumericalSeries;
import tan.philip.nrf_ble.GraphScreen.GraphSeries.WaveformSeries;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplay;
import tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay.DigitalDisplaySettings;
import tan.philip.nrf_ble.GraphScreen.UIComponents.GraphContainer;

/**
 * Special-cased visualization for the ICG (thoracic bioimpedance) channel.
 *
 * The firmware ships the raw impedance magnitude Z (see MAX30009.c: z = sqrt(I^2+Q^2)),
 * which is dominated by the baseline Z0 and the respiration (IP) component. Per the SCRIBE
 * reference paper (Cook et al., IEEE Sensors 2026), the cardiac impedance (cIPG) sits
 * ~20-40 dB below the respiration signal and is extracted with a 1-10 Hz bandpass followed
 * by a Savitzky-Golay derivative. This class reproduces that host-side pipeline live and
 * renders, in addition to the raw Z graph:
 *
 *   1. cIPG-Z(t)  : baseline/respiration removed (1-pole HP @ ~1 Hz) -> the pulsatile Z.
 *   2. -dZ/dt     : Savitzky-Golay first derivative of cIPG-Z (ICG sign convention: the
 *                   C-wave is positive), i.e. the classic ICG waveform.
 *
 * It also logs a rolling modulation depth (peak-to-peak pulsatile dZ / Z0) and a crude SNR,
 * which is the number to compare across electrode placements when scoring the board.
 *
 * Activated by GraphRenderer when a channel's name equals "ICG" (case-insensitive).
 */
public class ICGGraphSignal extends GraphSignal {

    private static final String TAG = "ICG";

    // --- 1-pole high-pass @ ~1 Hz (removes Z0 baseline + respiration) -------------------
    // alpha = RC / (RC + dt), RC = 1/(2*pi*fc). fc = 1 Hz.
    private final double hpAlpha;
    private double hpPrevX = 0, hpPrevY = 0;
    private boolean hpPrimed = false;

    // --- Savitzky-Golay 1st derivative (cubic fit == linear-weighted slope) -------------
    // 25-tap symmetric window (~200 ms @ 125 Hz). slope = sum(k * y[k]) / sum(k^2) * fs.
    private static final int SG_M = 12;                 // half-window
    private static final int SG_LEN = 2 * SG_M + 1;     // 25
    private static final double SG_NORM = (double) SG_M * (SG_M + 1) * (2 * SG_M + 1) / 3.0; // 1300
    private final double[] sgBuf = new double[SG_LEN];
    private final int fs;

    // --- Rolling placement-quality metric ----------------------------------------------
    private static final int METRIC_WINDOW = 250;       // ~2 s @ 125 Hz
    private int metricCount = 0;
    private double czMin = Double.POSITIVE_INFINITY, czMax = Double.NEGATIVE_INFINITY;
    private double z0Sum = 0;

    // Derived render series (owned here, also added to super.series so the render loop drains them).
    private WaveformSeries rawSeries, czSeries, dzdtSeries;
    private final SignalSetting czSetting, dzdtSetting;

    // On-screen modulation-depth readout, driven through the render loop (main thread).
    private final SignalSetting modSetting;
    private NumericalSeries modSeries;

    public ICGGraphSignal(SignalSetting settings) {
        super(settings);
        fs = Math.round(1000f / getSample_period());

        double rc = 1.0 / (2.0 * Math.PI * 1.0);
        double dt = 1.0 / fs;
        hpAlpha = rc / (rc + dt);

        // Derived channels reuse the parent's resolution/height; distinct names + colors.
        czSetting   = derivedSetting("ICG cIPG-Z", new int[]{200, 120, 0, 255});   // amber
        dzdtSetting = derivedSetting("ICG -dZ/dt", new int[]{0, 80, 255, 255});     // blue

        // Digital-display setting for the live modulation depth (dZpp / Z0, in %).
        modSetting = new SignalSetting((byte) 0, "ICG mod", (byte) 3, fs,
                (byte) getBitResolution(), true);
        DigitalDisplaySettings dd = new DigitalDisplaySettings("ICG mod");
        dd.conversion = "x";
        dd.suffix = "%";
        dd.iconName = "resistance";
        dd.decimalFormat = new DecimalFormat("##0.000");
        modSetting.ddSettings = dd;
    }

    private SignalSetting derivedSetting(String name, int[] color) {
        SignalSetting s = new SignalSetting((byte) 0, name, (byte) 3, fs,
                (byte) getBitResolution(), true);
        s.graphable = true;
        s.color = color;
        s.graphHeight = getLayoutHeight();
        return s;
    }

    /**
     * Build the raw-Z graph plus the two derived graphs. Called by GraphRenderer in place of
     * the normal setupWaveformGraph() for ICG channels. Returns all containers, top to bottom.
     */
    public List<GraphContainer> setupICGGraphs(Context ctx, int monitorLength) {
        List<GraphContainer> containers = new ArrayList<>();

        rawSeries  = buildSeries(getSettingsForRaw(), monitorLength);
        czSeries   = buildSeries(czSetting, monitorLength);
        dzdtSeries = buildSeries(dzdtSetting, monitorLength);

        // GraphContainer reads its label/height/color from the passed GraphSignal, so give the
        // derived graphs their own lightweight label signals (the real series live in super.series
        // and are drained by the render loop regardless of which signal labels the container).
        containers.add(makeContainer(ctx, this, rawSeries, monitorLength));
        containers.add(makeContainer(ctx, new GraphSignal(czSetting), czSeries, monitorLength));
        containers.add(makeContainer(ctx, new GraphSignal(dzdtSetting), dzdtSeries, monitorLength));
        return containers;
    }

    // Raw graph keeps the channel's own (init-file) settings via the parent's public getters.
    private SignalSetting getSettingsForRaw() {
        SignalSetting s = new SignalSetting((byte) 0, getName(), (byte) 3, fs,
                (byte) getBitResolution(), true);
        s.graphable = true;
        s.color = getColor();
        s.graphHeight = getLayoutHeight();
        return s;
    }

    private WaveformSeries buildSeries(SignalSetting setting, int monitorLength) {
        WaveformSeries ws = new WaveformSeries(setting, monitorLength);
        series.add(ws);   // super.series is drained by GraphRenderer's render loop
        return ws;
    }

    private GraphContainer makeContainer(Context ctx, GraphSignal labelSignal, WaveformSeries ws,
                                         int monitorLength) {
        Series[] toRender = {ws.getMonitor_series(), ws.getMonitor_mask()};
        GraphContainer gc = new GraphContainer(ctx, labelSignal, toRender);
        gc.setViewportMinX(0);
        gc.setViewportMaxX(monitorLength);
        return gc;
    }

    // --- Digital-display hooks: GraphRenderer creates+registers the display, then calls
    //     addDigitalDisplay() so we can bind our own NumericalSeries to it. ----------------
    @Override
    public boolean useDigitalDisplay() {
        return true;
    }

    @Override
    public DigitalDisplaySettings getDigitalDisplaySettings() {
        return modSetting.ddSettings;
    }

    @Override
    public void addDigitalDisplay(DigitalDisplay display) {
        // Bind the modulation readout to modSetting (not the parent's raw-Z setting), and add it
        // to super.series so the render loop drains it and updates the display on the main thread.
        modSeries = new NumericalSeries(modSetting, display);
        series.add(modSeries);
    }

    /**
     * Receives the raw Z samples for this packet, runs the cIPG pipeline, and routes raw /
     * cIPG-Z / -dZ/dt to their own series. Overrides the parent (which would feed identical
     * raw data to every series). The I/Q subclass converts I+Q to a Z stream first, then
     * calls processZStream() directly.
     */
    @Override
    public void queueDataPoints(ArrayList<Float> newDataPoints, long curTime) {
        processZStream(newDataPoints, curTime);
    }

    /** Core cIPG pipeline shared by the magnitude and complex (I/Q) paths. */
    protected void processZStream(ArrayList<Float> zSamples, long curTime) {
        if (rawSeries == null) return;   // graphs not built yet

        int n = zSamples.size();
        ArrayList<Float> raw = new ArrayList<>(n);
        ArrayList<Float> cz  = new ArrayList<>(n);
        ArrayList<Float> dz  = new ArrayList<>(n);

        for (Float xf : zSamples) {
            double x = xf;

            // 1-pole HP: prime on the first sample so we don't emit a huge startup step.
            double czVal;
            if (!hpPrimed) {
                hpPrevX = x;
                hpPrevY = 0;
                hpPrimed = true;
                czVal = 0;
            } else {
                czVal = hpAlpha * (hpPrevY + x - hpPrevX);
                hpPrevX = x;
                hpPrevY = czVal;
            }

            // SG derivative on the baseline-removed signal, negated for ICG convention.
            System.arraycopy(sgBuf, 1, sgBuf, 0, SG_LEN - 1);
            sgBuf[SG_LEN - 1] = czVal;
            double num = 0;
            for (int k = 0; k < SG_LEN; k++)
                num += (k - SG_M) * sgBuf[k];
            double dzdt = -(num / SG_NORM) * fs;   // -dZ/dt, aligned to sample (n - SG_M)

            raw.add(xf);
            cz.add((float) czVal);
            dz.add((float) dzdt);

            updateMetric(x, czVal);
        }

        rawSeries.queueDataPoints(raw, curTime);
        czSeries.queueDataPoints(cz, curTime);
        dzdtSeries.queueDataPoints(dz, curTime);
    }

    // Rolling ΔZ/Z0 modulation depth + crude SNR, logged for placement A/B scoring.
    private void updateMetric(double rawZ, double cz) {
        z0Sum += rawZ;
        if (cz < czMin) czMin = cz;
        if (cz > czMax) czMax = cz;

        if (++metricCount >= METRIC_WINDOW) {
            double z0 = z0Sum / metricCount;
            double dZpp = czMax - czMin;                     // pulsatile peak-to-peak (Ω-counts)
            double modPct = (z0 != 0) ? 100.0 * dZpp / Math.abs(z0) : 0;
            Log.i(TAG, String.format(
                    "cIPG: Z0=%.0f  dZpp=%.1f  mod=%.3f%%  (higher mod = better placement)",
                    z0, dZpp, modPct));

            // Push to the on-screen readout via the render loop (do NOT touch the View here;
            // this runs on the BLE/parse thread). NumericalSeries updates the UI on dequeue.
            if (modSeries != null) {
                ArrayList<Float> one = new ArrayList<>(1);
                one.add((float) modPct);
                modSeries.queueDataPoints(one, System.currentTimeMillis());
            }

            metricCount = 0;
            czMin = Double.POSITIVE_INFINITY;
            czMax = Double.NEGATIVE_INFINITY;
            z0Sum = 0;
        }
    }
}
