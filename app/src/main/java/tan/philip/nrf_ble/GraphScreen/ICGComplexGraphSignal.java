package tan.philip.nrf_ble.GraphScreen;

import java.util.ArrayList;

import tan.philip.nrf_ble.BLE.PacketParsing.SignalSetting;

/**
 * I/Q variant of the ICG visualization for the quad_v1_iq experiment build, which ships the
 * raw in-phase (I) and quadrature (Q) bioimpedance components instead of the on-device
 * magnitude. It performs complex phase demodulation on the host: it tracks the slowly varying
 * baseline phasor (I0, Q0) and projects each instantaneous sample onto it, yielding a
 * phase-noise-rejecting "aligned" impedance
 *
 *     R_aligned = I*cos(theta0) + Q*sin(theta0) = (I*I0 + Q*Q0) / |(I0,Q0)|
 *
 * The cardiac blood-volume change is largely along one consistent phasor direction, so
 * projecting onto the baseline rejects motion/phase noise orthogonal to it - cleaner than the
 * plain magnitude sqrt(I^2+Q^2). The aligned Z is fed into the shared cIPG pipeline (baseline
 * removal + Savitzky-Golay derivative), so the app still shows cIPG-Z(t), -dZ/dt and the
 * modulation-depth readout, i.e. live heartbeats during the experiment.
 *
 * Registered by GraphRenderer for a channel named "ICGI". The matching "ICGQ" samples are
 * handed in per packet via setPendingQ() before queueDataPoints() (which carries I) runs.
 */
public class ICGComplexGraphSignal extends ICGGraphSignal {

    // Baseline phasor low-pass @ ~0.2 Hz: alpha = dt / (RC + dt), RC = 1/(2*pi*fc).
    private final double lpAlpha;
    private double i0 = 0, q0 = 0;
    private boolean lpPrimed = false;

    private ArrayList<Float> pendingQ;

    public ICGComplexGraphSignal(SignalSetting settings) {
        super(settings);
        int fs = Math.round(1000f / getSample_period());
        double rc = 1.0 / (2.0 * Math.PI * 0.2);
        double dt = 1.0 / fs;
        lpAlpha = dt / (rc + dt);
    }

    /** Supplies this packet's Q samples; call before queueDataPoints() (which carries I). */
    public void setPendingQ(ArrayList<Float> qSamples) {
        this.pendingQ = qSamples;
    }

    @Override
    public void queueDataPoints(ArrayList<Float> iSamples, long curTime) {
        ArrayList<Float> q = pendingQ;
        pendingQ = null;
        if (q == null) return;   // need the matching Q batch to demodulate

        int n = Math.min(iSamples.size(), q.size());
        ArrayList<Float> z = new ArrayList<>(n);

        for (int k = 0; k < n; k++) {
            double i = iSamples.get(k);
            double qq = q.get(k);

            // Track the slowly varying baseline phasor.
            if (!lpPrimed) {
                i0 = i; q0 = qq; lpPrimed = true;
            } else {
                i0 += lpAlpha * (i - i0);
                q0 += lpAlpha * (qq - q0);
            }

            // Project onto the baseline direction (phase-noise-rejecting aligned impedance).
            double mag0 = Math.hypot(i0, q0);
            double rAligned = (mag0 > 1e-9)
                    ? (i * i0 + qq * q0) / mag0     // = i*cos(theta0) + q*sin(theta0)
                    : Math.hypot(i, qq);            // fallback: plain magnitude
            z.add((float) rAligned);
        }

        processZStream(z, curTime);
    }
}
