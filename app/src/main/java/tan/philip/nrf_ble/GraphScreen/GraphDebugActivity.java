package tan.philip.nrf_ble.GraphScreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.animation.ValueAnimator;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import java.text.DecimalFormat;
import java.util.Random;

import tan.philip.nrf_ble.BluetoothLeService;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity;
import tan.philip.nrf_ble.Vibration;
import tan.philip.nrf_ble.databinding.ActivityGraphBinding;

public class GraphDebugActivity extends AppCompatActivity {

    private final double MIN_VALUE = -1.5;
    private final double MAX_VALUE = 1.5;
    private final double MARGIN = 1;
    //private static final int INDEX_TO_START_FILTER_DISPLAY = 5000;
    private static final int INPUT_DELAY = 5000;


    public static final String TAG = "GraphDebugActivity";
    public static final int MAX_POINTS_PLOT = 1000;
    public static final int MAX_POINTS_ARRAY = 20000;
    //public static final float SAMPLE_PERIOD = 0.008f; //At 125ksps sample rate
    public static final float SAMPLE_PERIOD = 0.025f; //At 40 ksps sample rate
    public static final int GRAPHING_PERIOD = 35; //In ms

    public static final int SENSOR_1_IDENTIFIER = 1;
    public static final int SENSOR_2_IDENTIFIER = 2;

    private LineGraphSeries<DataPoint> sensor1Input;
    private LineGraphSeries<DataPoint> sensor1Output;
    private LineGraphSeries<DataPoint> sensor2Input;
    //private LineGraphSeries<DataPoint> sensor2Output;
    //private BarGraphSeries<DataPoint> peaks;

    private GraphView graph;
    private boolean graph1Scrollable = false;

    private float sensor1x[] = new float[MAX_POINTS_ARRAY];
    private float sensor1y[] = new float[MAX_POINTS_ARRAY];
    private float sensor2x[] = new float[MAX_POINTS_ARRAY];
    private float sensor2y[] = new float[MAX_POINTS_ARRAY];
    Filter sensor1Filter = new Filter(sensor1x, sensor1y);
    Filter sensor2Filter = new Filter(sensor2x, sensor2y);
    private int numPoints = 0;


    private HeartDataAnalysis hda;
    private ValueAnimator heartRateAnimator;
    private int heartRate = -1;
    DecimalFormat decimalFormat;
    private int indexLastPeakSensor1 = -1;
    private int indexLastPeakSensor2 = -1;

    private ActivityGraphBinding mBinding;
    private String deviceIdentifier;

    private final Handler mHandler = new Handler();
    private Runnable graphTimer;

    private long startTime;
    private long endTime;
    private long startGraphTime;
    private float elapsedTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        deviceIdentifier = "Debug Mode";

        mBinding  = DataBindingUtil.setContentView(this, R.layout.activity_graph);

        mBinding.textView5.setText("Reading from device " + deviceIdentifier);
        graph = mBinding.graph1;

        sensor1Input = new LineGraphSeries<>();
        sensor1Output = new LineGraphSeries<>();
        sensor2Input = new LineGraphSeries<>();
        //sensor2Output = new LineGraphSeries<>();
        //peaks = new BarGraphSeries<>();

        setupGraph();


        mBinding.btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetViewport();
            }
        });

        hda = new HeartDataAnalysis();
        heartRateAnimator = ValueAnimator.ofInt(255, 0);
        heartRateAnimator.setRepeatCount(1);
        heartRateAnimator.setRepeatMode(ValueAnimator.REVERSE);
        heartRateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mBinding.txtHeartRate.setTextColor((mBinding.txtHeartRate.getTextColors().withAlpha((int)heartRateAnimator.getAnimatedValue())));
            }
        });

        decimalFormat = new DecimalFormat("#.####");

        drawHrRanges();
    }

    @Override
    protected  void onResume() {
        super.onResume();

        resetViewport();
        startTime = System.nanoTime();

        graphTimer = new Runnable() {
            @Override
            public void run() {
                long  timeStart = System.nanoTime();

                //if(numPoints == INPUT_DELAY)
                //   startGraphTime = System.nanoTime();

                for(int i = 0; i < GRAPHING_PERIOD/SAMPLE_PERIOD; i ++)
                    runData();
                drawHrRanges();
                displayData();

                //Reached end of data, scroll automatically
                if(graph1Scrollable && graph.getViewport().getMinX(false) < graph.getViewport().getMinX(true)) {
                    double viewportWidth = graph.getViewport().getMaxX(false) - graph.getViewport().getMinX(false);
                    graph.getViewport().setMinX(graph.getViewport().getMinX(true));
                    graph.getViewport().setMaxX(graph.getViewport().getMinX(true) + viewportWidth);
                }

                float timeElapsed = (float) (System.nanoTime() - timeStart) / 1_000_000f;

                mHandler.postDelayed(this, (int) (GRAPHING_PERIOD - timeElapsed));
            }
        };

        mHandler.postDelayed(graphTimer, GRAPHING_PERIOD);
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(graphTimer);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setupGraph() {
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        //graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show  sensor1x values in seconds
                    String label = super.formatLabel(value, isValueX);
                    if(label.contains(".") || value < 0) {
                        return null;
                    }
                    return label;
                } else {
                    // show normal sensor1y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });


        graph.getGridLabelRenderer().setGridStyle( GridLabelRenderer.GridStyle.NONE );

        graph.getViewport().setYAxisBoundsManual(true);
        //graph.getViewport().setMinY(MIN_VALUE - MARGIN/2);
        graph.getViewport().setMinY(-0.5);
        graph.getViewport().setMaxY(4*MAX_VALUE - MIN_VALUE + 3/2*MARGIN);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(3);

        graph.addSeries(sensor1Input);
        graph.addSeries(sensor2Input);
        graph.addSeries(sensor1Output);
        //graph.addSeries(sensor2Output);
        //graph.addSeries(peaks);

        graph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!graph1Scrollable) {
                    graph1Scrollable = true;
                    graph.getViewport().setScrollable(true);
                    graph.getViewport().setScalable(true);
                    graph.getViewport().setScalableY(false);

                    mBinding.btnReset.setVisibility(View.VISIBLE);
                    ValueAnimator resetButtonAnimator = ValueAnimator.ofInt(0,255);
                    resetButtonAnimator.setDuration(1000);
                    resetButtonAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            mBinding.btnReset.getBackground().setAlpha((int)resetButtonAnimator.getAnimatedValue());
                            mBinding.btnReset.setTextColor((mBinding.btnReset.getTextColors().withAlpha((int)resetButtonAnimator.getAnimatedValue())));
                        }
                    });
                    resetButtonAnimator.start();
                }
            }
        });

        Paint redPaint = new Paint();
        redPaint.setARGB(255, 255, 0, 0);
        redPaint.setStrokeWidth(5);
        sensor1Output.setCustomPaint(redPaint);

        Paint sensor1Paint = new Paint();
        sensor1Paint.setARGB(255, 166, 166, 166);
        sensor1Paint.setStrokeWidth(5);
        sensor1Input.setCustomPaint(sensor1Paint);

        /*
        Paint bluePaint = new Paint();
        bluePaint.setARGB(255, 0, 0, 255);
        bluePaint.setStrokeWidth(5);
        sensor2Output.setCustomPaint(bluePaint);
        */

        Paint sensor2Paint = new Paint();
        sensor2Paint.setARGB(255, 107, 107, 107);
        sensor2Paint.setStrokeWidth(5);
        sensor2Input.setCustomPaint(sensor2Paint);

        //peaks.setDataWidth(0.01);
        //peaks.setSize(5);
    }

    private void resetViewport() {
        graph1Scrollable = false;
        graph.getViewport().setScrollable(false);
        graph.getViewport().setScalable(false);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(3);
        graph.getViewport().scrollToEnd();
        mBinding.btnReset.setVisibility(View.GONE);
    }

    private void displayData() {
        if(numPoints > INPUT_DELAY) {
            float maxX = (float) (numPoints - INPUT_DELAY - 3) * SAMPLE_PERIOD / 1000;
            //sensor1Input.appendData(new DataPoint(elapsedTime, sensor1x[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY]), !graph1Scrollable, MAX_POINTS_PLOT);
            sensor1Input.appendData(new DataPoint((float) (numPoints - INPUT_DELAY) * SAMPLE_PERIOD / 1000, sensor1x[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY] +2 ), !graph1Scrollable, MAX_POINTS_PLOT);
            sensor2Input.appendData(new DataPoint((float) (numPoints - INPUT_DELAY) * SAMPLE_PERIOD / 1000, sensor2x[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY] - 0.5), !graph1Scrollable, MAX_POINTS_PLOT);

            //sensor1Output.appendData(new DataPoint(elapsedTime, sensor1y[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY]), !graph1Scrollable, MAX_POINTS_PLOT);
            //float out = sensor1y[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY];


            //sensor1Output.appendData(new DataPoint(maxX, 2 + out), true, MAX_POINTS_PLOT);
            sensor1Output.appendData(new DataPoint((float) (numPoints - INPUT_DELAY) * SAMPLE_PERIOD / 1000, sensor1y[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY]), !graph1Scrollable, MAX_POINTS_PLOT);
            //sensor2Output.appendData(new DataPoint((float) (numPoints - INPUT_DELAY) * SAMPLE_PERIOD / 1000, sensor2y[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY]), !graph1Scrollable, MAX_POINTS_PLOT);

            if((int)heartRateAnimator.getAnimatedValue() <= 10 && heartRate != -1)
                mBinding.txtHeartRate.setText(String.valueOf(heartRate) + " bpm");

            if(hda.getNumPWVPoints() > 5) {
                float pwvAvg = hda.calculateWeightedAveragePWV();
                mBinding.txtPWV.setText(decimalFormat.format(pwvAvg) + " m/s");
            }
        }
    }

    private void runData() {
        gatherInputData(sensor1x, 0);
        gatherInputData(sensor2x, -0.005f);
        filterData(sensor1Filter, sensor1x, sensor1y, SENSOR_1_IDENTIFIER);
        filterData(sensor2Filter, sensor2x, sensor2y, SENSOR_2_IDENTIFIER);
        /*
        if (numPoints == INDEX_TO_START_FILTER_DISPLAY) {
            Toast toast = Toast.makeText(getApplicationContext(), "Filter started.", Toast.LENGTH_SHORT);
            toast.show();
            graph.addSeries(sensor1Output);
            //graph.addSeries(sensor2Output);
        }
        */

        numPoints++;
    }

    private void gatherInputData(float inputArray[], float delayInS) {
        //endTime = System.nanoTime();
        //elapsedTime = ((float)(endTime - startTime) / 1_000_000_000);

        float newInput;

        newInput = sampleECG(((float)numPoints * SAMPLE_PERIOD / 1000), delayInS);


        inputArray[numPoints % MAX_POINTS_ARRAY] = newInput;
    }

    private void filterData(Filter filter, float inputArray[], float outputArray[], int sensorIdentifier) {
        float newInput = inputArray[numPoints % MAX_POINTS_ARRAY];

        if(numPoints > Filter.N_POLES) {
            filter.findNextYDebug();
            filter.amplifyPeaks();
            filter.amplify(4.5f);
        } else {
            filter.setXv(newInput, numPoints);
            filter.setYv(0, numPoints);
        }

        //Determining if there was a peak detected, and where.
        int peakIndex = filter.findPeak();

        //if(numPoints > INPUT_DELAY)

        handlePeaks(peakIndex, sensorIdentifier);
    }

    private void handlePeaks(int peakIndex, int sensorIdentifier) {
        //float elapsedTime = ((float)(endTime - startTime) / 1_000_000_000);
        int indexLastPeak = indexLastPeakSensor2;

        if(sensorIdentifier == SENSOR_1_IDENTIFIER)
            indexLastPeak = indexLastPeakSensor1;

        if(peakIndex != Filter.NO_PEAK_DETECTED) {
            //long timeOfCurrentPulse = System.nanoTime();

            if(indexLastPeak != -1) {
                heartRate = hda.calculateHeartRate((float) (peakIndex - indexLastPeak) / 1000f * SAMPLE_PERIOD);

                if(!graph1Scrollable)
                    Vibration.vibrate(this, 100);

                if(!mBinding.txtHeartRate.getText().equals(String.valueOf(heartRate) + " bpm"))
                    heartRateAnimator.start();

                if(sensorIdentifier == SENSOR_2_IDENTIFIER) {
                    float currentPWV = hda.calculatePWV((float) (peakIndex - indexLastPeakSensor1) / 10f * SAMPLE_PERIOD);
                }

            }
            //peaks.appendData(new DataPoint((float) (indexCurrentPulse) * SAMPLE_PERIOD / 1000,1.5), false, 2);
            if(sensorIdentifier == SENSOR_1_IDENTIFIER) {
                indexLastPeakSensor1 = peakIndex;
                //peaks.appendData(new DataPoint((float) (peakIndex) * SAMPLE_PERIOD / 1000, sensor1y[peakIndex % MAX_POINTS_ARRAY]), false, 2);
            } else {
                indexLastPeakSensor2 = peakIndex;
                //peaks.appendData(new DataPoint((float) (peakIndex) * SAMPLE_PERIOD / 1000, sensor2y[peakIndex % MAX_POINTS_ARRAY]), false, 2);
            }

        }
    }

    private static final int MIN_HR = 40;
    private static final int MAX_HR = 200;

    /**
     * Draw the heart rate range bar on the bottom
     */
    private void drawHrRanges() {
        float positionPerHR = (float) mBinding.imgGreyBar.getLayoutParams().width/ (float) (MAX_HR - MIN_HR);

        float lowPosition = 0;
        float highPosition = 0;

        if(hda.getMinHR() != HeartDataAnalysis.NO_DATA) {
            lowPosition = (hda.getMinHR() - MIN_HR) * positionPerHR;
            mBinding.txtLowHr.setText(String.valueOf(hda.getMinHR()));
        }
        if(hda.getMaxHR() != HeartDataAnalysis.NO_DATA) {
            highPosition = (hda.getMaxHR() - MIN_HR) * positionPerHR;
            mBinding.txtHighHr.setText(String.valueOf(hda.getMaxHR()));
        }
        if(hda.getMaxHR() == HeartDataAnalysis.NO_DATA && hda.getMinHR() == HeartDataAnalysis.NO_DATA) {
            mBinding.imgBlueBar.setVisibility(View.GONE);
            mBinding.imgLeftBlueCircle.setVisibility(View.GONE);
            mBinding.imgRightBlueCircle.setVisibility(View.GONE);
            mBinding.txtLowHr.setVisibility(View.GONE);
            mBinding.txtHighHr.setVisibility(View.GONE);
            mBinding.txtSingleHr.setVisibility(View.GONE);
            mBinding.txtWarning.setVisibility(View.GONE);
            return;
        }
        mBinding.imgBlueBar.setVisibility(View.VISIBLE);
        mBinding.imgLeftBlueCircle.setVisibility(View.VISIBLE);
        mBinding.imgRightBlueCircle.setVisibility(View.VISIBLE);
        if(hda.getMaxHR() != hda.getMinHR()) {
            mBinding.txtLowHr.setVisibility(View.VISIBLE);
            mBinding.txtHighHr.setVisibility(View.VISIBLE);
            mBinding.txtSingleHr.setVisibility(View.GONE);
        } else {
            mBinding.txtLowHr.setVisibility(View.GONE);
            mBinding.txtHighHr.setVisibility(View.GONE);
            mBinding.txtSingleHr.setVisibility(View.VISIBLE);
            mBinding.txtSingleHr.setText(String.valueOf(hda.getMinHR()));
        }
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.imgBlueBar.getLayoutParams();

        params.setMargins(Math.round(lowPosition), params.topMargin, params.rightMargin, params.bottomMargin);
        mBinding.imgBlueBar.setLayoutParams(params);
        mBinding.imgBlueBar.getLayoutParams().width = Math.max(Math.round(highPosition-lowPosition), 1);

        mBinding.txtWarning.setVisibility(View.VISIBLE);
        if(hda.getRangeHR() <= 5) {
            mBinding.txtWarning.setText("Regular heart rhythm");
        } else if (hda.getRangeHR() <= 13) {
            mBinding.txtWarning.setText("Slightly irregular heart rhythm");
        } else {
            mBinding.txtWarning.setText("Extremely irregular heart rhythm");
        }
    }


    float mLastRandom = 2;
    Random mRand = new Random();
    private float getRandom() {
        if(mLastRandom > MAX_VALUE - 0.25) {
            return mLastRandom -= Math.abs(mRand.nextDouble()*0.5 - 0.25);
        } else if (mLastRandom < MIN_VALUE + 0.25) {
            return mLastRandom += Math.abs(mRand.nextDouble()*0.5 - 0.25);
        }
        return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
    }

    private static final double PERCENTAGE_NOISE = 0.2;
    private static final float HR = 76 ;
    private float sampleECG(float x, float delay) {
        float frequency = (float) (1/HR * Math.PI);

        //float out = 4 + (float)(MAX_VALUE*Math.pow(Math.sin(5*(x + delay)), 16) - MAX_VALUE*Math.pow(Math.sin(5*(x+delay)+2.9), 16));
        float out = (float)(MAX_VALUE*Math.pow(Math.sin(5*(x + delay)), 16) + 0.4*(mRand.nextDouble()-0.5)*Math.sin(160*x + 0.3)- MAX_VALUE*Math.pow(Math.sin(5*(x + delay) + 2.9), 16));
        out += 4+mRand.nextDouble()*MAX_VALUE*PERCENTAGE_NOISE - MAX_VALUE*PERCENTAGE_NOISE/2;


        return out;
    }
}