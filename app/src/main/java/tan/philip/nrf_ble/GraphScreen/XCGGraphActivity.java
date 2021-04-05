//package tan.philip.nrf_ble.GraphScreen;
//
//import android.Manifest;
//import android.animation.ValueAnimator;
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.ServiceConnection;
//import android.content.pm.PackageManager;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.drawable.ColorDrawable;
//import android.graphics.drawable.Drawable;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.IBinder;
//import android.util.Log;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.PopupMenu;
//import android.widget.SeekBar;
//import android.widget.Toast;
//import android.widget.ToggleButton;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.core.app.ActivityCompat;
//import androidx.databinding.DataBindingUtil;
//
//import com.jjoe64.graphview.DefaultLabelFormatter;
//import com.jjoe64.graphview.GraphView;
//import com.jjoe64.graphview.GridLabelRenderer;
//import com.jjoe64.graphview.series.DataPoint;
//import com.jjoe64.graphview.series.DataPointInterface;
//import com.jjoe64.graphview.series.LineGraphSeries;
//import com.jjoe64.graphview.series.PointsGraphSeries;
//import com.opencsv.CSVWriter;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.DecimalFormat;
//import java.util.Calendar;
//
//import tan.philip.nrf_ble.BluetoothLeService;
//import tan.philip.nrf_ble.R;
//import tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity;
//import tan.philip.nrf_ble.databinding.ActivityXcggraphBinding;
//
//import static tan.philip.nrf_ble.GraphScreen.HeartDataAnalysis.MAX_POSSIBLE_HR;
//import static tan.philip.nrf_ble.GraphScreen.HeartDataAnalysis.MIN_POSSIBLE_HR;
//
//public class XCGGraphActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
//
//    public static final String TAG = "XCGGraphActivity";
//    public static final int MAX_POINTS_PLOT = 10000;
//    public static final int MAX_POINTS_ARRAY = 20000;
//    public static final int GRAPHING_RATE = 100; //In Hz
//    private float GRAPHING_PERIOD = 1000f / (float) GRAPHING_RATE; //ms
//    public static final int MAX_ECG_DISPLAY_LENGTH = 10; //seconds
//
//
//
//
//    private final double MIN_VALUE = -1.5;
//    private final double MAX_VALUE = 1.5;
//    private final double MARGIN = 1;
//    private static final int INPUT_DELAY = 10;
//
//    public static final int SENSOR_1_IDENTIFIER = 1;
//    public static final int SENSOR_2_IDENTIFIER = 2;
//
//    //Sample rate
//    private int ecg_sample_rate = 100;  //Hz
//    private int scg_sample_rate = 100;  //Hz
//    private float ecg_sample_period = 1000f / (float) ecg_sample_rate; //ms
//    private float scg_sample_period = 1000f / (float) scg_sample_rate; //ms
//    private int NUM_POINTS_IN_ECG_VIEW = (int) ((float) MAX_ECG_DISPLAY_LENGTH * GRAPHING_RATE);
//
//    //Timer
//    private long start;
//    private long finish;
//
//    //Graphing Initializers
//    private GraphView graph;
//    private boolean graph1Scrollable = false;
//
//    //Series for Interactive Mode
//    private LineGraphSeries<DataPoint> series1_Interactive;
//    private LineGraphSeries<DataPoint> series2_Interactive;
//
//    //Series for ECG Mode
//    private PointsGraphSeries<DataPoint> ECG_Mask;
//    private LineGraphSeries<DataPoint> series1_ECG;
//    private LineGraphSeries<DataPoint> series2_ECG;
//
//    //ECG View
//    private DataPoint[] series1_Buffer = new DataPoint[NUM_POINTS_IN_ECG_VIEW];
//    private DataPoint[] series2_Buffer = new DataPoint[NUM_POINTS_IN_ECG_VIEW];
//    private DataPoint[] mask = new DataPoint[1];
//
//
//    //Interactive, for filtering
//    private float sensor1x[] = new float[MAX_POINTS_ARRAY];
//    private float sensor1y[] = new float[MAX_POINTS_ARRAY];
//    private float sensor2x[] = new float[MAX_POINTS_ARRAY];
//    private float sensor2y[] = new float[MAX_POINTS_ARRAY];
//
//
//    //Graphing, filtering, etc.
//    Filter sensor1Filter = new Filter(sensor1x, sensor1y, ecg_sample_rate, ecg_sample_rate, Filter.SignalType.SCG);
//    Filter sensor2Filter = new Filter(sensor2x, sensor2y, ecg_sample_rate, ecg_sample_rate, Filter.SignalType.ECG);
//    private int numPointsECG = 0;
//    private int numPointsSCG = 0;
//    private int numGraphedPoints = 0;
//    private float SCG_gain = 0.6f;
//    private float ECG_gain = 0.8f;
//    private float amplification = 5;
//    private boolean ECGView = true;
//
//
//    private HeartDataAnalysis hda;
//    private ValueAnimator heartRateAnimator;
//    private int heartRate = HeartDataAnalysis.NO_DATA;
//    DecimalFormat decimalFormat;
//
//    private ActivityXcggraphBinding mBinding;
//    private String deviceIdentifier;
//
//    private final Handler mHandler = new Handler();
//    private Runnable graphTimer;
//
//    private boolean sdGood = false;
//
//    private BluetoothLeService mBluetoothLeService;
//
//    //Saving
//    private ToggleButton recordButton;
//    private String fileName;
//    private boolean storeData = false;
//    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Pulse_Data";
//    private long startRecordTime;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_xcggraph);
//        invalidateOptionsMenu();
//
//        //Setup from prior activity
//        Intent intent = getIntent();
//        deviceIdentifier = (String) intent.getSerializableExtra(ScanResultsActivity.EXTRA_BT_IDENTIFIER);
//
//        //Bluetooth Setup
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//
//        //UI Setup
//        mBinding  = DataBindingUtil.setContentView(this, R.layout.activity_xcggraph);
//        mBinding.recordTimer.setVisibility(View.INVISIBLE);
//        mBinding.textView5.setText("Reading from device " + deviceIdentifier);
//        mBinding.btnReset.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                resetViewport();
//            }
//        });
//        mBinding.amplification.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//                //amplification = i / 10; //(float) (Math.pow(2, (float) i / 9) / 30);
//                amplification = i * i / 500;
//
//                //resetAllSeries();
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//
//        //Graphing setup
//        graph = mBinding.graph1;
//        ECG_Mask = new PointsGraphSeries<>();
//        series1_ECG = new LineGraphSeries<>();
//        series2_ECG = new LineGraphSeries<>();
//        series1_Interactive = new LineGraphSeries<>();
//        series2_Interactive = new LineGraphSeries<>();
//        setupGraph();
//        start = System.currentTimeMillis();
//
//        //Data setup
//        hda = new HeartDataAnalysis();
//        heartRateAnimator = ValueAnimator.ofInt(255, 0);
//        heartRateAnimator.setRepeatCount(1);
//        heartRateAnimator.setRepeatMode(ValueAnimator.REVERSE);
//        heartRateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                mBinding.txtHeartRate.setTextColor((mBinding.txtHeartRate.getTextColors().withAlpha((int)heartRateAnimator.getAnimatedValue())));
//            }
//        });
//
//        decimalFormat = new DecimalFormat("#.####");
//
//        drawHrRanges();
//    }
//
//    @Override
//    protected  void onResume() {
//        super.onResume();
//
//        resetViewport();
//
//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//
//        graphTimer = new Runnable() {
//            @Override
//            public void run() {
//                displayData();
//
//                //Reached end of data, scroll automatically
//                if(!ECGView && graph1Scrollable && graph.getViewport().getMinX(false) < graph.getViewport().getMinX(true)) {
//                    double viewportWidth = graph.getViewport().getMaxX(false) - graph.getViewport().getMinX(false);
//                    graph.getViewport().setMinX(graph.getViewport().getMinX(true));
//                    graph.getViewport().setMaxX(graph.getViewport().getMinX(true) + viewportWidth);
//                }
//
//                mHandler.postDelayed(this, (long) GRAPHING_PERIOD);
//            }
//        };
//        mHandler.postDelayed(graphTimer, 200);
//    }
//
//    @Override
//    public void onPause() {
//        mHandler.removeCallbacks(graphTimer);
//        unregisterReceiver(mGattUpdateReceiver);
//
//        super.onPause();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//
//        mBluetoothLeService.disconnect();
//        mBluetoothLeService.close();
//
//        unbindService(mServiceConnection);
//        mBluetoothLeService = null;
//    }
//
//    ////////////////////////////////////GRAPH STUFF////////////////////////////////////////////////
//
//    private void setupGraph() {
//        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
//        //graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
//
//        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
//            @Override
//            public String formatLabel(double value, boolean isValueX) {
//                if (isValueX) {
//                    // show  sensor1x values in seconds
//                    String label = super.formatLabel(value, isValueX);
//                    if(label.contains(".") || value < 0) {
//                        return null;
//                    }
//                    return label;
//                } else {
//                    // show normal sensor1y values
//                    return super.formatLabel(value, isValueX);
//                }
//            }
//        });
//
//
//        graph.getGridLabelRenderer().setGridStyle( GridLabelRenderer.GridStyle.NONE );
//
//        graph.getViewport().setYAxisBoundsManual(true);
//        graph.getViewport().setMinY(-0.5);
//        graph.getViewport().setMaxY(6*MAX_VALUE - MIN_VALUE + 3/2*MARGIN);
//
//        //graph.getViewport().setXAxisBoundsManual(true);
//        graph.getViewport().setMinX(0);
//        graph.getViewport().setMaxX(10);
//        graph.getViewport().setXAxisBoundsManual(true);
//
//        graph.addSeries(series1_ECG);
//        graph.addSeries(series2_ECG);
//        graph.addSeries(ECG_Mask);
//
//        //Click on graph to enable scroll in interactive view
//        graph.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(!graph1Scrollable && !ECGView) {
//                    graph1Scrollable = true;
//                    graph.getViewport().setScrollable(true);
//                    graph.getViewport().setScalable(true);
//                    graph.getViewport().setScalableY(false);
//
//                    mBinding.btnReset.setVisibility(View.VISIBLE);
//                    ValueAnimator resetButtonAnimator = ValueAnimator.ofInt(0,255);
//                    resetButtonAnimator.setDuration(1000);
//                    resetButtonAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                        @Override
//                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                            mBinding.btnReset.getBackground().setAlpha((int)resetButtonAnimator.getAnimatedValue());
//                            mBinding.btnReset.setTextColor((mBinding.btnReset.getTextColors().withAlpha((int)resetButtonAnimator.getAnimatedValue())));
//                        }
//                    });
//                    resetButtonAnimator.start();
//                }
//            }
//        });
//
//        setSeriesPaint(255, 166, 166, 166, 5, series1_ECG);
//        //setSeriesPaint(255, 255, 255, 255, 6, ECG_Mask);
//
//        int color = Color.WHITE;
//        Drawable background = mBinding.backgroundCL.getBackground();
//        if (background instanceof ColorDrawable)
//            color = ((ColorDrawable) background).getColor();
//        //int color = Color.rgb(0, 250,0);
//
//        ECG_Mask.setColor(color);
//        ECG_Mask.setCustomShape(new PointsGraphSeries.CustomShape() {
//            @Override
//            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
//                paint.setStrokeWidth(15);
//                canvas.drawLine(x, y, x, y-2000, paint);
//            }
//        });
//
//        setSeriesPaint(255, 255, 0, 0, 5, series2_ECG);
//
//        resetAllSeries();
//
//    }
//
//    private void resetAllSeries() {
//        series1_Interactive = new LineGraphSeries<>();
//        series2_Interactive = new LineGraphSeries<>();
//        setSeriesPaint(255, 166, 166, 166, 5, series1_Interactive);
//        setSeriesPaint(255, 255, 0, 0, 5, series2_Interactive);
//
//        for (int i = 0; i < series1_Buffer.length; i ++) {
//            series1_Buffer[i] = new DataPoint((float) i * GRAPHING_PERIOD/ 1000f, SENSOR_1_OFFSET);
//            series2_Buffer[i] = new DataPoint((float) i * GRAPHING_PERIOD / 1000f, SENSOR_2_OFFSET);
//        }
//    }
//
//    private void setSeriesPaint(int a, int r, int g, int b, int strokeWidth, LineGraphSeries<DataPoint> series) {
//        Paint sensorPaint = new Paint();
//        sensorPaint.setARGB(a, r, g, b);
//        sensorPaint.setStrokeWidth(strokeWidth);
//        series.setCustomPaint(sensorPaint);
//    }
//
//    private void resetViewport() {
//        if(!ECGView) {
//            graph1Scrollable = false;
//            graph.getViewport().setScrollable(false);
//            graph.getViewport().setScalable(false);
//            graph.getViewport().setMinX(0);
//            graph.getViewport().setMaxX(6);
//            graph.getViewport().scrollToEnd();
//        }
//        mBinding.btnReset.setVisibility(View.GONE);
//    }
//
//    private static final float SENSOR_1_OFFSET = 6;
//    private static final float SENSOR_2_OFFSET = 2;
//    private void displayData() {
//        if(Math.min(numPointsECG, numPointsSCG) >= INPUT_DELAY) {
//            //Determine next plotting point
//            //float xPoint = (float) (numPoints - INPUT_DELAY/100) * ecg_sample_period / 1000;
//
//            float xPoint = (float) (numGraphedPoints - INPUT_DELAY) * GRAPHING_PERIOD / 1000;
//            //Log.d(TAG, Integer.toString(numGraphedPoints) + " " + Integer.toString(numPointsECG) + " " + Integer.toString(numPointsSCG));
//            finish = System.currentTimeMillis();
//            Long elapsedTime = start - finish;
//            //Log.d("", Long.toString(elapsedTime));
//            start = finish;
//
//
//            //Take the last point in each sensor data array
//            float yPoint1Filtered = sensor1y[(numPointsSCG) % MAX_POINTS_ARRAY] * SCG_gain * amplification + SENSOR_1_OFFSET ;
//            float yPoint2Filtered = sensor2y[(numPointsECG) % MAX_POINTS_ARRAY] * ECG_gain * amplification + SENSOR_2_OFFSET ;
//
//            Log.d("", Integer.toString(numGraphedPoints) + " " + Integer.toString((numPointsSCG) % MAX_POINTS_ARRAY) + " " + Integer.toString((numPointsSCG) % MAX_POINTS_ARRAY));
//            Log.d("", Float.toString(sensor1y[(numPointsECG) % MAX_POINTS_ARRAY]) + " " + Float.toString(sensor2y[(numPointsSCG) % MAX_POINTS_ARRAY]));
//            //Plot the data
//
//            //Interactive series
//            series1_Interactive.appendData(new DataPoint(xPoint, yPoint1Filtered), !graph1Scrollable, MAX_POINTS_PLOT);;
//            series2_Interactive.appendData(new DataPoint(xPoint, yPoint2Filtered), !graph1Scrollable, MAX_POINTS_PLOT);
//
//            //ECG style series, buffer the data so they are in order.
//            series1_Buffer[numGraphedPoints % NUM_POINTS_IN_ECG_VIEW] = new DataPoint((float)(numGraphedPoints % NUM_POINTS_IN_ECG_VIEW) * GRAPHING_PERIOD / 1000f, yPoint1Filtered);
//            series2_Buffer[numGraphedPoints % NUM_POINTS_IN_ECG_VIEW] = new DataPoint((float)(numGraphedPoints % NUM_POINTS_IN_ECG_VIEW) * GRAPHING_PERIOD / 1000f, yPoint2Filtered);
//
//            //Actually plot the ECG data
//            series1_ECG.resetData(series1_Buffer);
//            series2_ECG.resetData(series2_Buffer);
//
//            //Draw Mask over old data points
//            float maskX = xPoint;
//            //Inefficient but I'm too tired to think of a better way
//            while(maskX > 10) {
//                while(maskX > 100) {
//                    while(maskX > 1000) {
//                        maskX -= 1000;
//                    }
//                    maskX -= 100;
//                }
//                maskX -= 10;
//            }
//            //mBinding.txtHeartRate.setText(Float.toString(maskX));
//
//            //Again, follows ECG data
//            mask[0] = new DataPoint((float)(numGraphedPoints % NUM_POINTS_IN_ECG_VIEW) * (float) GRAPHING_PERIOD / 1000f, -0.5);
//            ECG_Mask.resetData(mask);
//
//
//            //Display HR and PWV
//            if((int)heartRateAnimator.getAnimatedValue() <= 10 && heartRate != -1)
//                mBinding.txtHeartRate.setText(String.valueOf(heartRate) + " bpm");
//
//            numGraphedPoints ++;
//        }
//    }
//
//    private void toggleView() {
//        if(ECGView) {
//            ECGView = false;
//
//            resetViewport();
//
//            //Remove series from graph
//            graph.removeAllSeries();
//
//            //Add the correct series
//            graph.addSeries(series1_Interactive);
//            graph.addSeries(series2_Interactive);
//        } else {
//            ECGView = true;
//
//            //Set viewport to 10 seconds
//            //graph.getViewport().setXAxisBoundsManual(true);
//            graph.getViewport().setMinX(0);
//            graph.getViewport().setMaxX(10);
//
//            //Remove series from graph
//            graph.removeAllSeries();
//
//            //Add the correct series
//            graph.addSeries(series1_ECG);
//            graph.addSeries(series2_ECG);
//            graph.addSeries(ECG_Mask);
//
//            mBinding.btnReset.setVisibility(View.GONE);
//        }
//    }
//
//    ////////////////////////////////////////DATA RECEPTION//////////////////////////////////////////
//
//    private static final int INDEX_SENSOR_1_DATA = 1;      //Change to 1 for original code from XX
//    private static final int INDEX_SENSOR_2_DATA = 0;      //Change to 5 for original code from XX
//    private static final float PRESCALER = 3f / 1024f;     //10 bit resolution to convert to volts (nRF has 1/3 prescaler)
//
//    private static final int DATA_FLAG = 0x0000;
//    private static final int SD_FLAG = 0xF000;
//    private static final int FS_FLAG = 0xE000;
//    private static final int SD_GOOD = 0xFF;
//    private static final int SD_BAD = 0xFE;
//    private static final int SCG_FLAG = 0x0;
//    private static final int ECG_FLAG = 0x0400;
//
//
//    private void runData(byte[] input) {
//        //Little endian conversion
//        int[] shorts = convertByteArrayToShorts(input);
//
//        //Log all received bytes
//        for(int i = 0; i < input.length; i ++) {
//            //Log.d("", Byte.toString(input[i]));
//        }
//
//        for(int i = 0; i < shorts.length; i ++) {
//            int currentShort = shorts[i];
//            //Log.d("", Integer.toString(currentShort));
//
//            int flag = currentShort & 0xF000;
//            //Log.d("", Integer.toString(flag));
//
//            switch(flag) {
//                case DATA_FLAG:
//                    //float sensor1FloatData = (float) shorts[INDEX_SENSOR_1_DATA + i] * PRESCALER;
//                    //float sensor2FloatData = (float) shorts[INDEX_SENSOR_2_DATA + i] * PRESCALER;
//
//                    handleADCdata(shorts[i]);
//                    break;
//                case SD_FLAG:
//                    int SD_BITS = currentShort & 0x00FF;
//
//                    if (SD_BITS == SD_GOOD) {
//                        sdGood = true;
//                    } else if (SD_BITS == SD_BAD) {
//                        sdGood = false;
//                        mBinding.sdDetectedText.setText("SD card not detected");
//                        mBinding.sdDetectedText.setTextColor(Color.rgb(255,0,0));
//                    }
//                    break;
//                case FS_FLAG:
//                    int FS_BITS = currentShort & 0x00FF;
//                    int ID_BIT = currentShort & 0x0400;
//
//                    updateSampleRates(ID_BIT, FS_BITS);
//
//                    break;
//                default:
//                    break;
//            }
//
//            //Check flags
//
//
//            /*
//            if (currentShort == SD_GOOD) {
//                sdGood = true;
//            } else if (currentShort == SD_BAD) {
//                sdGood = false;
//                mBinding.sdDetectedText.setText("SD card not detected");
//                mBinding.sdDetectedText.setTextColor(Color.rgb(255,0,0));
//            } else {
//                //Not right
//                float sensor1FloatData = (float) shorts[INDEX_SENSOR_1_DATA + i] * PRESCALER;
//                float sensor2FloatData = (float) shorts[INDEX_SENSOR_2_DATA + i] * PRESCALER;
//
//                handleADCdata(sensor1FloatData, sensor2FloatData);
//            }
//            */
//
//
//        }
//    }
//
//    private void handleADCdata(int sensorData){
//        int ID_BIT = sensorData & 0x0400;
//        if(ID_BIT == ECG_FLAG) {
//            sensorData &= 0xFBFF;
//        }
//        float floatData = (float) sensorData * PRESCALER;
//
//        //Saving Data in memory
//        if (storeData && ID_BIT == ECG_FLAG) {
//            //Store data
//            String data[] = {Float.toString(floatData)};
//            writeCSV(data, data.length);
//
//            mBinding.recordTimer.setText(decimalFormat.format((System.currentTimeMillis() - startRecordTime) / 1000f));
//        }
//
//        //Put data into right array based on sensor ID
//        if(ID_BIT == SCG_FLAG) {
//            numPointsSCG ++;
//            gatherInputData(sensor1x, floatData, SCG_gain, numPointsSCG);
//            filterData(sensor1Filter, sensor1x, sensor1y, SCG_gain, SENSOR_1_IDENTIFIER, numPointsSCG);
//        } else if (ID_BIT == ECG_FLAG) {
//            numPointsECG ++;
//            gatherInputData(sensor2x, floatData, ECG_gain, numPointsECG);
//            filterData(sensor2Filter, sensor2x, sensor2y, ECG_gain, SENSOR_2_IDENTIFIER, numPointsECG);
//        }
//
//        //displayData();
//    }
//
//    private void gatherInputData(float inputArray[], float dataIn, float gain, int numPoints) {
//        inputArray[numPoints % MAX_POINTS_ARRAY] = dataIn;
//    }
//
//    private void filterData(Filter filter, float inputArray[], float outputArray[], float gain, int sensorIdentifier, int numPoints) {
//        float newInput = inputArray[numPoints % MAX_POINTS_ARRAY];
//
//        if(numPoints > Filter.N_POLES) {
//            filter.findNextY();
//        } else {
//            filter.setXv(newInput, numPoints);
//            filter.setYv(0, numPoints);
//        }
//
//        //Determining if there was a peak detected, and where.
//        //int peakIndex = filter.findPeak();
//
//        //if(numPoints > INPUT_DELAY)
//        //    handlePeaks(peakIndex, sensorIdentifier);
//    }
//
//    private void updateSampleRates(int sensorID, int newFs){
//        if(sensorID == ECG_FLAG) {
//            //I send the sample period instead of sample rate
//            if(ecg_sample_period != newFs) {
//                ecg_sample_period = newFs;  //ms
//                ecg_sample_rate = (int) (1000/ecg_sample_period); //Hz
//                mBinding.txtEcg.setText("ECG ("+ Integer.toString(ecg_sample_rate) +" Hz)");
//                resetAllSeries();
//            }
//        } else {
//            if(scg_sample_period != newFs) {
//                scg_sample_period = newFs;  //Hz
//                scg_sample_rate = (int) (1000f / (float) scg_sample_period); //ms
//                mBinding.txtScg.setText("SCG ("+ Integer.toString(scg_sample_rate) +" Hz)");
//                resetAllSeries();
//            }
//        }
//    }
//
//    /**
//     * Will calculate HR and PWV from the index of the last peak detected
//     * @param peakIndex Index of the last peak detected
//     * @param sensorIdentifier Which sensor the peak was detected from
//     */
//
//    /*
//    private void handlePeaks(int peakIndex, int sensorIdentifier) {
//        int indexLastPeak = indexLastPeakSensor1;
//
//        if(sensorIdentifier == SENSOR_2_IDENTIFIER)
//            indexLastPeak = indexLastPeakSensor2;
//
//        if(peakIndex != Filter.NO_PEAK_DETECTED) {
//            if(indexLastPeak != -1) {
//                int hrBuffer = hda.calculateHeartRate((float) (peakIndex - indexLastPeak) / 1000f * ecg_sample_period);
//
//                if (!graph1Scrollable)
//                    Vibration.vibrate(this, 100);
//
//                if(hrBuffer != HeartDataAnalysis.NO_DATA) {
//                    heartRate = hrBuffer;
//
//                    if (!mBinding.txtHeartRate.getText().equals(String.valueOf(heartRate) + " bpm"))
//                        heartRateAnimator.start();
//                }
//
//                if(sensorIdentifier == SENSOR_2_IDENTIFIER) {
//                    float currentPWV = hda.calculatePWV((float) (peakIndex - indexLastPeakSensor1) / 10f * ecg_sample_period);
//                }
//
//            }
//            if(sensorIdentifier == SENSOR_1_IDENTIFIER) {
//                indexLastPeakSensor1 = peakIndex;
//            } else {
//                indexLastPeakSensor2 = peakIndex;
//            }
//        }
//    }
//
//     */
//
//    ///////////////////////////////////////////////////////BT///////////////////////////////////////
//    // Code to manage Service lifecycle.
//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder service) {
//            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
//            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
//            }
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            mBluetoothLeService = null;
//        }
//    };
//
//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                //mConnected = true;
//                invalidateOptionsMenu();
//            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)|| BluetoothLeService.ACTION_GATT_FAILED.equals(action)) {
//                //mConnected = false;
//                runOnUiThread(() -> Toast.makeText(XCGGraphActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show());
//                mBinding.textView5.setText(deviceIdentifier + " disconnected");
//                mBinding.textView5.setTextColor(Color.rgb(255,0,0));
//                invalidateOptionsMenu();
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                runData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
//
//                //if(numPoints % GRAPHING_FREQUENCY == 0) {
//                    //displayData();
//                drawHrRanges();
//                //}
//            }
//        }
//    };
//
//
//    ////////////////////////////////////////////////GRAPHICAL UI//////////////////////////////////////////////////////////////////////////////////
//
//    /**
//     * Draw the heart rate range bar on the bottom
//     */
//    private void drawHrRanges() {
//        float positionPerHR = (float) mBinding.imgGreyBar.getLayoutParams().width/ (float) (MAX_POSSIBLE_HR - MIN_POSSIBLE_HR);
//
//        float lowPosition = 0;
//        float highPosition = 0;
//
//        //If both min and max do not have values and there are enough data points, make everything disappear.
//        if((hda.getMaxHR() == HeartDataAnalysis.NO_DATA && hda.getMinHR() == HeartDataAnalysis.NO_DATA)) {
//            mBinding.imgBlueBar.setVisibility(View.GONE);
//            mBinding.imgLeftBlueCircle.setVisibility(View.GONE);
//            mBinding.imgRightBlueCircle.setVisibility(View.GONE);
//            mBinding.txtLowHr.setVisibility(View.GONE);
//            mBinding.txtHighHr.setVisibility(View.GONE);
//            mBinding.txtSingleHr.setVisibility(View.GONE);
//            mBinding.txtWarning.setVisibility(View.GONE);
//            return;
//        }
//
//        //If there is a min value available, series1_Buffer
//        if(hda.getMinHR() != HeartDataAnalysis.NO_DATA) {
//            lowPosition = (hda.getMinHR() - MIN_POSSIBLE_HR) * positionPerHR;
//            mBinding.txtLowHr.setText(String.valueOf(hda.getMinHR()));
//        }
//        //If there is a max value available, series1_Buffer
//        if(hda.getMaxHR() != HeartDataAnalysis.NO_DATA) {
//            highPosition = (hda.getMaxHR() - MIN_POSSIBLE_HR) * positionPerHR;
//            mBinding.txtHighHr.setText(String.valueOf(hda.getMaxHR()));
//        }
//
//        //Assuming that there is data (not returned out), make bar visible
//        mBinding.imgBlueBar.setVisibility(View.VISIBLE);
//        mBinding.imgLeftBlueCircle.setVisibility(View.VISIBLE);
//        mBinding.imgRightBlueCircle.setVisibility(View.VISIBLE);
//
//        //If  the min value is different from the max value, make a bar
//        if(hda.getMaxHR() != hda.getMinHR()) {
//            mBinding.txtLowHr.setVisibility(View.VISIBLE);
//            mBinding.txtHighHr.setVisibility(View.VISIBLE);
//            mBinding.txtSingleHr.setVisibility(View.GONE);
//        } else {
//            //Don't make a bar, just put a single value
//            mBinding.txtLowHr.setVisibility(View.GONE);
//            mBinding.txtHighHr.setVisibility(View.GONE);
//            mBinding.txtSingleHr.setVisibility(View.VISIBLE);
//            mBinding.txtSingleHr.setText(String.valueOf(hda.getMinHR()));
//        }
//
//        //Adjust the bar length depending on the range of values
//        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mBinding.imgBlueBar.getLayoutParams();
//        params.setMargins(Math.round(lowPosition), params.topMargin, params.rightMargin, params.bottomMargin);
//        mBinding.imgBlueBar.setLayoutParams(params);
//        mBinding.imgBlueBar.getLayoutParams().width = Math.max(Math.round(highPosition-lowPosition), 1);
//
//        //Set the UI text at the bottom
//        mBinding.txtWarning.setVisibility(View.VISIBLE);
//        if(hda.getRangeHR() <= 5) {
//            mBinding.txtWarning.setText("Regular heart rhythm");
//        } else if (hda.getRangeHR() <= 13) {
//            mBinding.txtWarning.setText("Slightly irregular heart rhythm");
//        } else {
//            mBinding.txtWarning.setText("Extremely irregular heart rhythm");
//        }
//    }
//
//
//    private static IntentFilter makeGattUpdateIntentFilter() {
//        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_FAILED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
//        return intentFilter;
//    }
//
//    /**
//     * Convert an unsigned byte array to an array of ints (2 bytes -> short -> int, Little Endian)
//     * @return Array of ints
//     */
//    private static int[] convertByteArrayToShorts(byte[] in) {
//        int[] out = new int[in.length/2];
//
//        for(int i = 0; i < in.length; i += 2) {
//            int byte1 = (int) in[i] & 0xFF;
//            int byte2 = (int) in[i+1] & 0xFF;
//
//
//            out[i/2] = 0x0000FFFF & (byte1 << 8)| (byte2);
//
//            /*
//            Log.d("", "Byte 1: " + Integer.toBinaryString(in[i]));
//            Log.d("", "Byte 2: " + Integer.toBinaryString(in[i]));
//            Log.d("", "Short : " + Integer.toBinaryString(out[i/2]));
//            */
//        }
//
//        return out;
//    }
//
//    //////////////////////////////////////////SEND DATA TO NRF/////////////////////////////////////
//    /*
//    private void checkSD() {
//        byte[] output = {CHECK_SD};
//        mBluetoothLeService.writeCharacteristic(output);
//    }
//    */
//
//    //////////////////////////////////////////SAVING TO MEMORY/////////////////////////////////////
//    private void toggleRecord() {
//        if(!storeData) {
//            startRecordTime = System.currentTimeMillis();
//            mBinding.recordTimer.setVisibility(View.VISIBLE);
//            storeData = true;
//
//            //Set File Name to current time
//            fileName = Calendar.getInstance().getTime().toString() + ".csv";
//
//            //Check Permission
//            while(!isStoragePermissionGranted());
//
//            //Create Folder
//            createFolder();
//
//        } else {
//            mBinding.recordTimer.setVisibility(View.INVISIBLE);
//            storeData = false;
//        }
//    }
//
//    public  boolean isStoragePermissionGranted() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    == PackageManager.PERMISSION_GRANTED) {
//                Log.v(TAG,"Permission is granted");
//                return true;
//            } else {
//
//                Log.v(TAG,"Permission is revoked");
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//                return false;
//            }
//        }
//        else { //permission is automatically granted on sdk<23 upon installation
//            Log.v(TAG,"Permission is granted");
//            return true;
//        }
//    }
//
//    private void createFolder() {
//        File folder = new File(path);
//        boolean success = true;
//        if (!folder.exists()) {
//            success = folder.mkdirs();
//        }
//        if (success) {
//            Log.d(TAG, "Successfully created new folder / already exists");
//        } else {
//            Log.d(TAG, "Failed to create new folder");
//        }
//    }
//
//    private void writeCSV(String data[], int lenData) {
//        Log.d(TAG, "Writing to CSV");
//        String filePath = path + File.separator + fileName;
//        File f = new File(filePath);
//        CSVWriter writer;
//        FileWriter mFileWriter;
//
//        try {
//            // File exist
//            if(f.exists()&&!f.isDirectory())
//            {
//                mFileWriter = new FileWriter(filePath, true);
//                writer = new CSVWriter(mFileWriter);
//            }
//            else
//            {
//                writer = new CSVWriter(new FileWriter(filePath));
//            }
//
//            writer.writeNext(data);
//
//            writer.close();
//            Log.d(TAG, "SUCCESSFUL WRITE");
//        } catch (IOException e) {
//            Log.d(TAG, e.toString());
//        }
//    }
//
//
//    //////////////////////////////////////////User Interface//////////////////////////////////////////
//    public void showOptions(View v) {
//        PopupMenu popup = new PopupMenu(this, v);
//        popup.setOnMenuItemClickListener(this);
//        popup.inflate(R.menu.popup_menu_xcg);
//
//        //Record Text
//        MenuItem recordMenuItem = popup.getMenu().findItem(R.id.record);
//        if(storeData)
//            recordMenuItem.setTitle("Stop recording");
//        else
//            recordMenuItem.setTitle("Record");
//
//        //View Text
//        MenuItem switchViewMenuItem = popup.getMenu().findItem(R.id.switchView);
//        if(ECGView)
//            switchViewMenuItem.setTitle("Interactive View");
//        else
//            switchViewMenuItem.setTitle("Medical ECG View");
//
//        popup.show();
//    }
//
//
//    @Override
//    public boolean onMenuItemClick(MenuItem menuItem) {
//        switch(menuItem.getItemId()) {
//            case R.id.invertProximal:
//                SCG_gain *= -1;
//                return true;
//            case R.id.invertDistal:
//                ECG_gain *= -1;
//                return true;
//            case R.id.record:
//                toggleRecord();
//                return true;
//            case R.id.switchView:
//                toggleView();
//                return true;
//            default:
//                return false;
//        }
//    }
//
//
//}