package tan.philip.nrf_ble.Algorithms.Filter;

import android.util.Log;

import java.util.HashMap;

public class FilterCalculator {
    //This is not ready
    /*
    private static final String TAG = "FilterCalculator";

    //Product of all elements in array
    private float prod(float[] a) {
        float prod = 1;

        for(float x : a) {
            prod *= x;
        }

        return prod;
    }

    //Add scalar to all elements in array
    private float[] add(float[]a, float s) {
        float[] out = new float[a.length];

        for(int i = 0; i < a.length; i ++)
            out[i] = a[i] + s;

        return out;
    }

    //Subtract scalar from all elements in array
    private float[] sub(float[]a, float s) {
        float[] out = new float[a.length];

        for(int i = 0; i < a.length; i ++)
            out[i] = a[i] - s;

        return out;
    }

    //Product between array and scalar
    private float[] mul(float[]a, float s) {
        float[] out = new float[a.length];

        for(int i = 0; i < a.length; i ++)
            out[i] = a[i] * s;

        return out;
    }

    //Divide each array element by scalar
    private float[] div(float[]a, float s) {
        float[] out = new float[a.length];

        for(int i = 0; i < a.length; i ++)
            out[i] = a[i] / s;

        return out;
    }

    //Divide each array element by scalar
    private float[] div(float[]a, float[]b) {
        float[] out = new float[a.length];

        for(int i = 0; i < a.length; i ++)
            out[i] = a[i] / b[i];

        return out;
    }

    private float relativeDegree(float[] z, float[] p) throws Exception {
        int degree = z.length - p.length;
        if (degree < 0) {
            Log.e(TAG, "Improper transfer function. Must have at least as many poles as zeros.");
            throw new Exception("Improper transfer function. Must have at least as many poles as zeros.");
        }
        return degree;
    }

    private HashMap<String, float[]> lp2lp_zpk(float[] z, float[] p, float[] k, float wo) throws Exception {
        float degree = relativeDegree(z, p);

        //Scale all points radially from origin to shift cutoff frequency
        float[] z_lp = mul(z, wo);
        float[] p_lp = mul(p, wo);


        //Each shifted pole decreases gain by wo, each shifted zero increases it.
        //Cancel out the net change to keep overall gain the same
        float[] k_lp = mul(k, (float) Math.pow(wo, degree));

        HashMap<String, float[]> out = new HashMap();
        out.put("z", z_lp);
        out.put("p", p_lp);
        out.put("k", k_lp);
        return out;
    }

    private HashMap<String, float[]> lp2hp_zpk(float[] z, float[] p, float[] k, float wo) throws Exception {
        float degree = relativeDegree(z, p);

        //Invert positions radially about unit circle to convert LPF to HPF
        //Scale all points radially from origin to shift cutoff frequency
        float[] z_hp = new float[z.length + (int) degree];
        float[] p_hp = new float[p.length];
        for (int i = 0; i < z.length; i++)
            z_hp[i] = wo / z[i];
        for (int i = 0; i < z.length; i++)
            p_hp[i] = wo / p[i];

        //If lowpass had zeros at infinity, inverting moves them to origin.
        for (int i = z.length; i < z_hp.length; i ++)
            z_hp[i] = 0; //Not really necessary

        //Cancel out gain change caused by inversion
        float[] k_hp = new float[k.length];

        Complex num = new Complex(prod(-z));
        for (int i = 0; i < z.length; i++)
            //k_lp[i] = k[i] * (float) Math.pow(wo, degree);
            k_hp[i] = k[i] * real(prod(-z) / prod(-p));

        HashMap<String, float[]> out = new HashMap();
        out.put("z", z_hp);
        out.put("p", p_hp);
        out.put("k", k_hp);
        return out;
    }

    def lp2bp_zpk(z, p, k, wo=1.0, bw=1.0):
    z = atleast_1d(z)
    p = atleast_1d(p)
    wo = float(wo)
    bw = float(bw)

    degree = _relative_degree(z, p)

    # Scale poles and zeros to desired bandwidth
            z_lp = z * bw/2
    p_lp = p * bw/2

            # Square root needs to produce complex result, not NaN
    z_lp = z_lp.astype(complex)
    p_lp = p_lp.astype(complex)

            # Duplicate poles and zeros and shift from baseband to +wo and -wo
            z_bp = concatenate((z_lp + sqrt(z_lp**2 - wo**2),
    z_lp - sqrt(z_lp**2 - wo**2)))
    p_bp = concatenate((p_lp + sqrt(p_lp**2 - wo**2),
    p_lp - sqrt(p_lp**2 - wo**2)))

            # Move degree zeros to origin, leaving degree zeros at infinity for BPF
            z_bp = append(z_bp, zeros(degree))

    # Cancel out gain change from frequency scaling
            k_bp = k * bw**degree

    return z_bp, p_bp, k_bp

    def lp2bs_zpk(z, p, k, wo=1.0, bw=1.0):
    z = atleast_1d(z)
    p = atleast_1d(p)
    wo = float(wo)
    bw = float(bw)

    degree = _relative_degree(z, p)

    # Invert to a highpass filter with desired bandwidth
    z_hp = (bw/2) / z
            p_hp = (bw/2) / p

    # Square root needs to produce complex result, not NaN
    z_hp = z_hp.astype(complex)
    p_hp = p_hp.astype(complex)

            # Duplicate poles and zeros and shift from baseband to +wo and -wo
            z_bs = concatenate((z_hp + sqrt(z_hp**2 - wo**2),
    z_hp - sqrt(z_hp**2 - wo**2)))
    p_bs = concatenate((p_hp + sqrt(p_hp**2 - wo**2),
    p_hp - sqrt(p_hp**2 - wo**2)))

            # Move any zeros that were at infinity to the center of the stopband
            z_bs = append(z_bs, full(degree, +1j*wo))
    z_bs = append(z_bs, full(degree, -1j*wo))

            # Cancel out gain change caused by inversion
            k_bs = k * real(prod(-z) / prod(-p))

    return z_bs, p_bs,

    def bilinear_zpk(z, p, k, fs):
    z = atleast_1d(z)
    p = atleast_1d(p)

    degree = _relative_degree(z, p)

    fs2 = 2.0*fs

    # Bilinear transform the poles and zeros
    z_z = (fs2 + z) / (fs2 - z)
    p_z = (fs2 + p) / (fs2 - p)

            # Any zeros that were at infinity get moved to the Nyquist frequency
    z_z = append(z_z, -ones(degree))

            # Compensate for gain change
    k_z = k * real(prod(fs2 - z) / prod(fs2 - p))

            return z_z, p_z, k_z

    def zpk2tf(z, p, k):
    z = atleast_1d(z)
    k = atleast_1d(k)
    if len(z.shape) > 1:
    temp = poly(z[0])
    b = np.empty((z.shape[0], z.shape[1] + 1), temp.dtype.char)
            if len(k) == 1:
    k = [k[0]] * z.shape[0]
            for i in range(z.shape[0]):
    b[i] = k[i] * poly(z[i])
    else:
    b = k * poly(z)
    a = atleast_1d(poly(p))

            # Use real output if possible. Copied from numpy.poly, since
    # we can't depend on a specific version of numpy.
            if issubclass(b.dtype.type, numpy.complexfloating):
            # if complex roots are all complex conjugates, the roots are real.
            roots = numpy.asarray(z, complex)
    pos_roots = numpy.compress(roots.imag > 0, roots)
    neg_roots = numpy.conjugate(numpy.compress(roots.imag < 0, roots))
            if len(pos_roots) == len(neg_roots):
            if numpy.all(numpy.sort_complex(neg_roots) ==
            numpy.sort_complex(pos_roots)):
    b = b.real.copy()

            if issubclass(a.dtype.type, numpy.complexfloating):
            # if complex roots are all complex conjugates, the roots are real.
            roots = numpy.asarray(p, complex)
    pos_roots = numpy.compress(roots.imag > 0, roots)
    neg_roots = numpy.conjugate(numpy.compress(roots.imag < 0, roots))
            if len(pos_roots) == len(neg_roots):
            if numpy.all(numpy.sort_complex(neg_roots) ==
            numpy.sort_complex(pos_roots)):
    a = a.real.copy()

            return b, a

    def buttap(N):
            if abs(int(N)) != N:
    raise ValueError("Filter order must be a nonnegative integer")
    z = numpy.array([])
    m = numpy.arange(-N+1, N, 2)
            # Middle value is 0 to ensure an exactly real pole
    p = -numpy.exp(1j * pi * m / (2 * N))
    k = 1
            return z, p, k

    def butter(N, fc, btype, fs):
    Wn = [float(i) / (float(fs) / 2) for i in fc]
    Wn = np.array(Wn)

            if numpy.any(Wn <= 0):
    raise ValueError("filter critical frequencies must be greater than 0")

    if Wn.size > 1 and not Wn[0] < Wn[1]:
    raise ValueError("Wn[0] must be less than Wn[1]")

    try:
    btype = band_dict[btype]
    except KeyError as e:
    raise ValueError("'%s' is an invalid bandtype for filter." % btype) from e

    typefunc = buttap


            z, p, k = buttap(N)

    # Pre-warp frequencies for digital filter design

    if numpy.any(Wn <= 0) or numpy.any(Wn >= 1):
    raise ValueError("Digital filter critical frequencies must "
                     f"be 0 < Wn < fs/2 (fs={fs} -> fs/2={fs/2})")

    fs = 2.0
    warped = 2 * fs * tan(pi * Wn / fs)


    # transform to lowpass, bandpass, highpass, or bandstop
    if btype in ('lowpass', 'highpass'):
            if numpy.size(Wn) != 1:
    raise ValueError('Must specify a single critical frequency Wn '
                             'for lowpass or highpass filter')

        if btype == 'lowpass':
    z, p, k = lp2lp_zpk(z, p, k, wo=warped)
    elif btype == 'highpass':
    z, p, k = lp2hp_zpk(z, p, k, wo=warped)
    elif btype in ('bandpass', 'bandstop'):
            try:
    bw = warped[1] - warped[0]
    wo = sqrt(warped[0] * warped[1])
    except IndexError as e:
    raise ValueError('Wn must specify start and stop frequencies for '
                             'bandpass or bandstop filter') from e

        if btype == 'bandpass':
    z, p, k = lp2bp_zpk(z, p, k, wo=wo, bw=bw)
    elif btype == 'bandstop':
    z, p, k = lp2bs_zpk(z, p, k, wo=wo, bw=bw)
    else:
    raise NotImplementedError("'%s' not implemented in iirfilter." % btype)

    z, p, k = bilinear_zpk(z, p, k, fs=fs)


    return zpk2tf(z, p, k)

     */
}
