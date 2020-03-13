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
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import tan.philip.nrf_ble.BluetoothLeService;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity;
import tan.philip.nrf_ble.Vibration;
import tan.philip.nrf_ble.databinding.ActivityGraphBinding;

import static tan.philip.nrf_ble.GraphScreen.HeartDataAnalysis.MAX_POSSIBLE_HR;
import static tan.philip.nrf_ble.GraphScreen.HeartDataAnalysis.MIN_POSSIBLE_HR;

public class GraphActivity extends AppCompatActivity {

    private final double MIN_VALUE = -1.5;
    private final double MAX_VALUE = 1.5;
    private final double MARGIN = 1;
    //private static final int INDEX_TO_START_FILTER_DISPLAY = 5000;
    private static final int INPUT_DELAY = 500;


    public static final String TAG = "GraphActivity";
    public static final int MAX_POINTS_PLOT = 1000;
    public static final int MAX_POINTS_ARRAY = 20000;
    public static final float SAMPLE_PERIOD = 2;
    //public static final float SAMPLE_PERIOD = 0.025f; //At 40 ksps sample rate
    public static final int GRAPHING_PERIOD = 20; //In ms

    public static final int SENSOR_1_IDENTIFIER = 1;
    public static final int SENSOR_2_IDENTIFIER = 2;

    private LineGraphSeries<DataPoint> sensor1Input;
    private LineGraphSeries<DataPoint> sensor2Output;
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
    private int heartRate = HeartDataAnalysis.NO_DATA;
    DecimalFormat decimalFormat;
    private int indexLastPeakSensor1 = HeartDataAnalysis.NO_DATA;
    private int indexLastPeakSensor2 = HeartDataAnalysis.NO_DATA;

    private ActivityGraphBinding mBinding;
    private String deviceIdentifier;

    private final Handler mHandler = new Handler();
    private Runnable graphTimer;

    private boolean mConnected;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothLeService mBluetoothLeService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private static final int GRAPHING_FREQUENCY = Math.round(GRAPHING_PERIOD / SAMPLE_PERIOD);
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)|| BluetoothLeService.ACTION_GATT_FAILED.equals(action)) {
                mConnected = false;
                runOnUiThread(() -> Toast.makeText(GraphActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show());
                mBinding.textView5.setText(deviceIdentifier + " disconnected");
                mBinding.textView5.setTextColor(Color.rgb(255,0,0));
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                runData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));


                if(numPoints % GRAPHING_FREQUENCY == 0) {
                    displayData();
                    drawHrRanges();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        this.getSupportActionBar().hide();



        Intent intent = getIntent();
        deviceIdentifier = (String) intent.getSerializableExtra(ScanResultsActivity.EXTRA_BT_IDENTIFIER);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mBinding  = DataBindingUtil.setContentView(this, R.layout.activity_graph);


        mBinding.textView5.setText("Reading from device " + deviceIdentifier);
        graph = mBinding.graph1;

        sensor1Input = new LineGraphSeries<>();
        sensor2Output = new LineGraphSeries<>();
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

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        graphTimer = new Runnable() {
            @Override
            public void run() {
                //Reached end of data, scroll automatically
                if(graph1Scrollable && graph.getViewport().getMinX(false) < graph.getViewport().getMinX(true)) {
                    double viewportWidth = graph.getViewport().getMaxX(false) - graph.getViewport().getMinX(false);
                    graph.getViewport().setMinX(graph.getViewport().getMinX(true));
                    graph.getViewport().setMaxX(graph.getViewport().getMinX(true) + viewportWidth);
                }

                mHandler.postDelayed(this, 200);
            }
        };
        mHandler.postDelayed(graphTimer, 200);
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(graphTimer);
        unregisterReceiver(mGattUpdateReceiver);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();

        unbindService(mServiceConnection);
        mBluetoothLeService = null;
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
        graph.addSeries(sensor2Output);
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
        sensor2Output.setCustomPaint(redPaint);

        Paint sensor1Paint = new Paint();
        sensor1Paint.setARGB(255, 166, 166, 166);
        sensor1Paint.setStrokeWidth(5);
        sensor1Input.setCustomPaint(sensor1Paint);

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

    private static final float SENSOR_1_OFFSET = 5;
    private static final float SENSOR_2_OFFSET = 2;
    private void displayData() {
        if(numPoints > INPUT_DELAY) {
            float xPoint = (float) (numPoints - INPUT_DELAY) * SAMPLE_PERIOD / 1000;
            float yPoint1 = sensor1x[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY] + SENSOR_1_OFFSET ;
            float yPoint2 = sensor2x[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY] + SENSOR_2_OFFSET;
            float yPoint2Filtered = sensor2y[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY];

            //Log.d(TAG, Float.toString(yPoint1));

            sensor1Input.appendData(new DataPoint(xPoint, yPoint1), !graph1Scrollable, MAX_POINTS_PLOT);
            sensor2Input.appendData(new DataPoint(xPoint, yPoint2), !graph1Scrollable, MAX_POINTS_PLOT);
            sensor2Output.appendData(new DataPoint(xPoint, yPoint2Filtered), !graph1Scrollable, MAX_POINTS_PLOT);

            if((int)heartRateAnimator.getAnimatedValue() <= 10 && heartRate != -1)
                mBinding.txtHeartRate.setText(String.valueOf(heartRate) + " bpm");

            if(hda.getNumPWVPoints() > 5) {
                float pwvAvg = hda.calculateWeightedAveragePWV();
                mBinding.txtPWV.setText(decimalFormat.format(pwvAvg) + " m/s");
            }
        }
    }

    private static final int INDEX_SENSOR_1_DATA = 0;      //Change to 1 for original code from XX
    private static final int INDEX_SENSOR_2_DATA = 1;      //Change to 5 for original code from XX
    private static final float PRESCALER = 3f / 1024f;     //10 bit resolution
    private void runData(byte[] input) {
        //Little endian conversion
        //short[] shorts = new short[input.length/2];
        //ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        int[] shorts = convertByteArray(input);


        for(int i = 0; i < (shorts.length); i +=2) {
            float sensor1FloatData = (float) shorts[INDEX_SENSOR_1_DATA + i] * PRESCALER;
            float sensor2FloatData = (float) shorts[INDEX_SENSOR_2_DATA + i] * PRESCALER;

            //Log.d("", Float.toString(sensor2FloatData));

            gatherInputData(sensor1x, sensor1FloatData);
            gatherInputData(sensor2x, sensor2FloatData);
            filterData(sensor1Filter, sensor1x, sensor1y, SENSOR_1_IDENTIFIER);
            filterData(sensor2Filter, sensor2x, sensor2y, SENSOR_2_IDENTIFIER);

            numPoints++;
        }
        //Log.d(TAG, "///////////////////////");
    }

    private void gatherInputData(float inputArray[], float dataIn) {
        inputArray[numPoints % MAX_POINTS_ARRAY] = dataIn;
    }

    private void filterData(Filter filter, float inputArray[], float outputArray[], int sensorIdentifier) {
        float newInput = inputArray[numPoints % MAX_POINTS_ARRAY];

        if(numPoints > Filter.N_POLES) {
            filter.findNextY();
            filter.amplifyPeaks();
            filter.amplify(2000f);

        } else {
            filter.setXv(newInput, numPoints);
            filter.setYv(0, numPoints);
        }

        //Determining if there was a peak detected, and where.
        int peakIndex = filter.findPeak();

        if(numPoints > INPUT_DELAY)
            handlePeaks(peakIndex, sensorIdentifier);
    }

    /**
     * Will calculate HR and PWV from the index of the last peak detected
     * @param peakIndex Index of the last peak detected
     * @param sensorIdentifier Which sensor the peak was detected from
     */
    private void handlePeaks(int peakIndex, int sensorIdentifier) {
        int indexLastPeak = indexLastPeakSensor1;

        if(sensorIdentifier == SENSOR_2_IDENTIFIER)
            indexLastPeak = indexLastPeakSensor2;

        if(peakIndex != Filter.NO_PEAK_DETECTED) {
            if(indexLastPeak != -1) {
                int hrBuffer = hda.calculateHeartRate((float) (peakIndex - indexLastPeak) / 1000f * SAMPLE_PERIOD);

                if (!graph1Scrollable)
                    Vibration.vibrate(this, 100);

                if(hrBuffer != HeartDataAnalysis.NO_DATA) {
                    heartRate = hrBuffer;

                    if (!mBinding.txtHeartRate.getText().equals(String.valueOf(heartRate) + " bpm"))
                        heartRateAnimator.start();
                }

                if(sensorIdentifier == SENSOR_2_IDENTIFIER) {
                    float currentPWV = hda.calculatePWV((float) (peakIndex - indexLastPeakSensor1) / 10f * SAMPLE_PERIOD);
                }

            }
            if(sensorIdentifier == SENSOR_1_IDENTIFIER) {
                indexLastPeakSensor1 = peakIndex;
            } else {
                indexLastPeakSensor2 = peakIndex;
            }
        }
    }



    /**
     * Draw the heart rate range bar on the bottom
     */
    private void drawHrRanges() {
        float positionPerHR = (float) mBinding.imgGreyBar.getLayoutParams().width/ (float) (MAX_POSSIBLE_HR - MIN_POSSIBLE_HR);

        float lowPosition = 0;
        float highPosition = 0;

        //If both min and max do not have values and there are enough data points, make everything disappear.
        if((hda.getMaxHR() == HeartDataAnalysis.NO_DATA && hda.getMinHR() == HeartDataAnalysis.NO_DATA)) {
            mBinding.imgBlueBar.setVisibility(View.GONE);
            mBinding.imgLeftBlueCircle.setVisibility(View.GONE);
            mBinding.imgRightBlueCircle.setVisibility(View.GONE);
            mBinding.txtLowHr.setVisibility(View.GONE);
            mBinding.txtHighHr.setVisibility(View.GONE);
            mBinding.txtSingleHr.setVisibility(View.GONE);
            mBinding.txtWarning.setVisibility(View.GONE);
            return;
        }

        //If there is a min value available, display
        if(hda.getMinHR() != HeartDataAnalysis.NO_DATA) {
            lowPosition = (hda.getMinHR() - MIN_POSSIBLE_HR) * positionPerHR;
            mBinding.txtLowHr.setText(String.valueOf(hda.getMinHR()));
        }
        //If there is a max value available, display
        if(hda.getMaxHR() != HeartDataAnalysis.NO_DATA) {
            highPosition = (hda.getMaxHR() - MIN_POSSIBLE_HR) * positionPerHR;
            mBinding.txtHighHr.setText(String.valueOf(hda.getMaxHR()));
        }

        //Assuming that there is data (not returned out), make bar visible
        mBinding.imgBlueBar.setVisibility(View.VISIBLE);
        mBinding.imgLeftBlueCircle.setVisibility(View.VISIBLE);
        mBinding.imgRightBlueCircle.setVisibility(View.VISIBLE);

        //If  the min value is different from the max value, make a bar
        if(hda.getMaxHR() != hda.getMinHR()) {
            mBinding.txtLowHr.setVisibility(View.VISIBLE);
            mBinding.txtHighHr.setVisibility(View.VISIBLE);
            mBinding.txtSingleHr.setVisibility(View.GONE);
        } else {
            //Don't make a bar, just put a single value
            mBinding.txtLowHr.setVisibility(View.GONE);
            mBinding.txtHighHr.setVisibility(View.GONE);
            mBinding.txtSingleHr.setVisibility(View.VISIBLE);
            mBinding.txtSingleHr.setText(String.valueOf(hda.getMinHR()));
        }

        //Adjust the bar length depending on the range of values
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.imgBlueBar.getLayoutParams();
        params.setMargins(Math.round(lowPosition), params.topMargin, params.rightMargin, params.bottomMargin);
        mBinding.imgBlueBar.setLayoutParams(params);
        mBinding.imgBlueBar.getLayoutParams().width = Math.max(Math.round(highPosition-lowPosition), 1);

        //Set the UI text at the bottom
        mBinding.txtWarning.setVisibility(View.VISIBLE);
        if(hda.getRangeHR() <= 5) {
            mBinding.txtWarning.setText("Regular heart rhythm");
        } else if (hda.getRangeHR() <= 13) {
            mBinding.txtWarning.setText("Slightly irregular heart rhythm");
        } else {
            mBinding.txtWarning.setText("Extremely irregular heart rhythm");
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_FAILED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * Convert an unsigned byte array to an array of ints (2 bytes -> short -> int, Little Endian)
     * @return Array of ints
     */
    private static int[] convertByteArray(byte[] in) {
        int[] out = new int[in.length/2];

        for(int i = 0; i < in.length; i += 2) {
            int byte1 = (int) in[i] & 0xFF;
            int byte2 = (int) in[i+1] & 0xFF;


            out[i/2] = 0x0000FFFF & (byte1 << 8)| (byte2);

            /*
            Log.d("", "Byte 1: " + Integer.toBinaryString(in[i]));
            Log.d("", "Byte 2: " + Integer.toBinaryString(in[i]));
            Log.d("", "Short : " + Integer.toBinaryString(out[i/2]));
            */
        }

        return out;
    }
}