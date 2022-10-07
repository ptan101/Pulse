package tan.philip.nrf_ble.GraphScreen.UIComponents;

import java.io.Serializable;

public class Filter implements Serializable {

    int len;
    double[] b;
    double[] a;
    double gain;

    double[] x;
    double[] y;

    //Assumes equal number of holes and poles
    public Filter (double[] b, double[] a, double gain) {
        this.b = b;
        this.a = a;
        this.gain = gain;

        //Equal to num zeros or poles - 1
        this.len = b.length - 1;

        x = new double[b.length];
        y = new double[a.length];
    }

    public float findNextY(float newX) {
        //Advance in time
        //Make this a circular FIFO for efficiency
        for(int i = 0; i < len; i ++) {
            x[i] = x[i+1];
            y[i] = y[i+1];
        }

        //Set the current input
        x[len] = newX * gain;

        if(len == 0)
            return (float) x[len];

        //Start calculating the new output.
        y[len] = b[0] * x[len];

        //Calculate the new output based on previous inputs and outputs
        for(int i = 1; i < len + 1; i ++) {
            y[len] += (b[i] * x[len - i] - a[i] * y[len - i]);
        }

        //Adjust by initial coefficient
        y[len] = y[len] / a[0];

        //Return the current filtered output
        return (float) y[len];
    }
}