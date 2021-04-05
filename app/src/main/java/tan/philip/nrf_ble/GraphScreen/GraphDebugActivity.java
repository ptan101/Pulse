//package tan.philip.nrf_ble.GraphScreen;
//
//import android.Manifest;
//import android.animation.ValueAnimator;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.pm.PackageManager;
//import android.graphics.Paint;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Environment;
//import android.os.Handler;
//import android.util.Log;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.PopupMenu;
//import android.widget.SeekBar;
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
//import com.jjoe64.graphview.series.LineGraphSeries;
//import com.opencsv.CSVWriter;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.DecimalFormat;
//import java.util.Calendar;
//import java.util.Random;
//
//import tan.philip.nrf_ble.BLE.BluetoothLeService;
//import tan.philip.nrf_ble.R;
//import tan.philip.nrf_ble.databinding.ActivityPwvgraphBinding;
//
//import static tan.philip.nrf_ble.GraphScreen.HeartDataAnalysis.MAX_POSSIBLE_HR;
//import static tan.philip.nrf_ble.GraphScreen.HeartDataAnalysis.MIN_POSSIBLE_HR;
//
//public class GraphDebugActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
//
//    private final double MIN_VALUE = -1.5;
//    private final double MAX_VALUE = 1.5;
//    private final double MARGIN = 1;
//    private static final int INPUT_DELAY = 100;
//
//    public static final String TAG = "PWVGraphActivity";
//    public static final int MAX_POINTS_PLOT = 10000;
//    public static final int MAX_POINTS_ARRAY = 20000;
//    public static final int SAMPLE_RATE = 500;  //Hz
//    public static final int TARGET_SAMPLE_RATE = 10000;
//    public static final float SAMPLE_PERIOD = 1000 / TARGET_SAMPLE_RATE; //ms
//    public static final int GRAPHING_PERIOD = 20; //In ms
//
//    public static final int SENSOR_1_IDENTIFIER = 1;
//    public static final int SENSOR_2_IDENTIFIER = 2;
//
//    private LineGraphSeries<DataPoint> sensor1Input;
//    private LineGraphSeries<DataPoint> sensor2Output;
//    private LineGraphSeries<DataPoint> sensor2Input;
//    private LineGraphSeries<DataPoint> sensor3Input;
//
//    //private LineGraphSeries<DataPoint> sensor2Output;
//    //private BarGraphSeries<DataPoint> peaks;
//
//    private GraphView graph;
//    private boolean graph1Scrollable = false;
//
//    private float sensor1x[] = new float[MAX_POINTS_ARRAY];
//    private float sensor1y[] = new float[MAX_POINTS_ARRAY];
//    private float sensor2x[] = new float[MAX_POINTS_ARRAY];
//    private float sensor2y[] = new float[MAX_POINTS_ARRAY];
//    private float sensor3x[] = new float[MAX_POINTS_ARRAY];
//
//
//    //Graphing, filtering, etc.
//    Filter sensor1Filter = new Filter(sensor1x, sensor1y, SAMPLE_RATE, TARGET_SAMPLE_RATE, Filter.SignalType.PPG);
//    Filter sensor2Filter = new Filter(sensor2x, sensor2y, SAMPLE_RATE, TARGET_SAMPLE_RATE, Filter.SignalType.PPG);
//    private int numPoints = 0;
//    private int numLFPoints = 0;
//    private float proximalGain = 1;
//    private float distalGain = 1;
//    private float amplification;
//
//
//    private HeartDataAnalysis hda;
//    private ValueAnimator heartRateAnimator;
//    private int heartRate = HeartDataAnalysis.NO_DATA;
//    DecimalFormat decimalFormat;
//    private int indexLastPeakSensor1 = HeartDataAnalysis.NO_DATA;
//    private int indexLastPeakSensor2 = HeartDataAnalysis.NO_DATA;
//
//    private ActivityPwvgraphBinding mBinding;
//    private String deviceIdentifier;
//
//    private final Handler mHandler = new Handler();
//    private Runnable graphTimer;
//
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
//        setContentView(R.layout.activity_pwvgraph);
//        invalidateOptionsMenu();
//
//
//        Intent intent = getIntent();
//        deviceIdentifier = "Simulation";
//        mBinding  = DataBindingUtil.setContentView(this, R.layout.activity_pwvgraph);
//
//        mBinding.recordTimer.setVisibility(View.INVISIBLE);
//
//        mBinding.textView5.setText("Reading from device " + deviceIdentifier);
//        graph = mBinding.graph1;
//
//        sensor1Input = new LineGraphSeries<>();
//        sensor2Input = new LineGraphSeries<>();
//        sensor3Input = new LineGraphSeries<>();
//        sensor2Output = new LineGraphSeries<>();
//        //sensor2Output = new LineGraphSeries<>();
//        //peaks = new BarGraphSeries<>();
//
//        setupGraph();
//
//
//        mBinding.btnReset.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                resetViewport();
//            }
//        });
//
//        mBinding.amplification.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//                amplification = (float) (Math.pow(2, (float) i / 9) / 30);
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
//        graphTimer = new Runnable() {
//            @Override
//            public void run() {
//                //Reached end of data, scroll automatically
//                if(graph1Scrollable && graph.getViewport().getMinX(false) < graph.getViewport().getMinX(true)) {
//                    double viewportWidth = graph.getViewport().getMaxX(false) - graph.getViewport().getMinX(false);
//                    graph.getViewport().setMinX(graph.getViewport().getMinX(true));
//                    graph.getViewport().setMaxX(graph.getViewport().getMinX(true) + viewportWidth);
//                }
//
//                mHandler.postDelayed(this, 200);
//            }
//        };
//        mHandler.postDelayed(graphTimer, 200);
//    }
//
//    @Override
//    public void onPause() {
//        mHandler.removeCallbacks(graphTimer);
//
//        super.onPause();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
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
//        //graph.getViewport().setMinY(MIN_VALUE - MARGIN/2);
//        graph.getViewport().setMinY(-0.5);
//        graph.getViewport().setMaxY(6*MAX_VALUE - MIN_VALUE + 3/2*MARGIN);
//
//        graph.getViewport().setXAxisBoundsManual(true);
//        graph.getViewport().setMinX(0);
//        graph.getViewport().setMaxX(3);
//
//        graph.addSeries(sensor1Input);
//        graph.addSeries(sensor2Input);
//        graph.addSeries(sensor3Input);
//        graph.addSeries(sensor2Output);
//        //graph.addSeries(sensor2Output);
//        //graph.addSeries(peaks);
//
//        graph.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(!graph1Scrollable) {
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
//        setSeriesPaint(255, 166, 166, 166, 5, sensor1Input);
//        setSeriesPaint(255, 107, 107, 107, 5, sensor2Input);
//        setSeriesPaint(255, 0, 0, 0, 5, sensor3Input);
//        setSeriesPaint(255, 255, 0, 0, 5, sensor2Output);
//
//        //peaks.setDataWidth(0.01);
//        //peaks.setSize(5);
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
//        graph1Scrollable = false;
//        graph.getViewport().setScrollable(false);
//        graph.getViewport().setScalable(false);
//        graph.getViewport().setMinX(0);
//        graph.getViewport().setMaxX(3);
//        graph.getViewport().scrollToEnd();
//        mBinding.btnReset.setVisibility(View.GONE);
//    }
//
//    private static final float SENSOR_1_OFFSET = 8;
//    private static final float SENSOR_2_OFFSET = 5;
//    private static final float SENSOR_3_OFFSET = 2;
//    private void displayData() {
//        if(numPoints > INPUT_DELAY) {
//            float xPoint = (float) (numPoints - INPUT_DELAY/100) * SAMPLE_PERIOD / 1000;
//            float yPoint1 = sensor1x[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY] + SENSOR_1_OFFSET ;
//            float yPoint2 = sensor2x[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY] + SENSOR_2_OFFSET;
//            float yPoint3 = sensor3x[(numLFPoints - INPUT_DELAY / 50) % MAX_POINTS_ARRAY] + SENSOR_3_OFFSET;
//
//
//            float yPoint1Filtered = sensor1y[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY] * distalGain * amplification + SENSOR_1_OFFSET ;
//            float yPoint2Filtered = sensor2y[(numPoints - INPUT_DELAY) % MAX_POINTS_ARRAY] * proximalGain * amplification + SENSOR_2_OFFSET ;
//
//            //Log.d(TAG, Float.toString(yPoint1));
//
//            //sensor1Input.appendData(new DataPoint(xPoint, yPoint1), !graph1Scrollable, MAX_POINTS_PLOT);
//            //sensor2Input.appendData(new DataPoint(xPoint, yPoint2), !graph1Scrollable, MAX_POINTS_PLOT);
//            sensor3Input.appendData(new DataPoint(xPoint, yPoint3), !graph1Scrollable, MAX_POINTS_PLOT);
//
//            sensor1Input.appendData(new DataPoint(xPoint, yPoint1Filtered), !graph1Scrollable, MAX_POINTS_PLOT);
//            sensor2Input.appendData(new DataPoint(xPoint, yPoint2Filtered), !graph1Scrollable, MAX_POINTS_PLOT);
//            sensor2Output.appendData(new DataPoint(xPoint, 0), !graph1Scrollable, MAX_POINTS_PLOT);
//
//            if((int)heartRateAnimator.getAnimatedValue() <= 10 && heartRate != -1)
//                mBinding.txtHeartRate.setText(String.valueOf(heartRate) + " bpm");
//
//            if(hda.getNumPWVPoints() > 5) {
//                float pwvAvg = hda.calculateWeightedAveragePWV();
//                mBinding.txtPWV.setText(decimalFormat.format(pwvAvg) + " m/s");
//            }
//        }
//    }
//
//
//    ////////////////////////////////////////DATA RECEPTION//////////////////////////////////////////
//
//    private static final float PRESCALER = 3f / 1024f;     //10 bit resolution
//
//    private float lfDataBuff;
//    private boolean availableLFData = false;
//    private float sensor1FloatData;
//    private float sensor2FloatData;
//
//    private void runData(byte[] input) {
//        //Simulate LF waveform
//        lfDataBuff = 2;
//        availableLFData = true;
//        sensor3x[numLFPoints % MAX_POINTS_ARRAY] = lfDataBuff;
//        numLFPoints ++;
//
//
//        //Simulate pulsatile waveform
//        sensor1FloatData = sampleECG(numPoints, 0);
//        sensor2FloatData = sampleECG(numPoints, 0.01f);
//
//        //Saving Data in memory
//        if (storeData) {
//            //Store data
//            if (availableLFData) {
//                String data[] = {Float.toString(sensor1FloatData), Float.toString(sensor2FloatData), Float.toString(lfDataBuff)};
//                writeCSV(data, data.length);
//                availableLFData = false;
//            } else {
//                String data[] = {Float.toString(sensor1FloatData), Float.toString(sensor2FloatData)};
//                writeCSV(data, data.length);
//            }
//            mBinding.recordTimer.setText(decimalFormat.format((System.currentTimeMillis() - startRecordTime) / 1000f));
//        }
//
//        gatherInputData(sensor1x, sensor1FloatData, proximalGain);
//        gatherInputData(sensor2x, sensor2FloatData, distalGain);
//        filterData(sensor1Filter, sensor1x, sensor1y, proximalGain, SENSOR_1_IDENTIFIER);
//        filterData(sensor2Filter, sensor2x, sensor2y, distalGain, SENSOR_2_IDENTIFIER);
//
//        numPoints++;
//    }
//
//
//    private void gatherInputData(float inputArray[], float dataIn, float gain) {
//        inputArray[numPoints % MAX_POINTS_ARRAY] = dataIn;
//    }
//
//    private void filterData(Filter filter, float inputArray[], float outputArray[], float gain, int sensorIdentifier) {
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
//
//    /**
//     * Will calculate HR and PWV from the index of the last peak detected
//     * @param peakIndex Index of the last peak detected
//     * @param sensorIdentifier Which sensor the peak was detected from
//     */
//    /*
//    private void handlePeaks(int peakIndex, int sensorIdentifier) {
//        int indexLastPeak = indexLastPeakSensor1;
//
//        if(sensorIdentifier == SENSOR_2_IDENTIFIER)
//            indexLastPeak = indexLastPeakSensor2;
//
//        if(peakIndex != Filter.NO_PEAK_DETECTED) {
//            if(indexLastPeak != -1) {
//                int hrBuffer = hda.calculateHeartRate((float) (peakIndex - indexLastPeak) / 1000f * SAMPLE_PERIOD);
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
//                    float currentPWV = hda.calculatePWV((float) (peakIndex - indexLastPeakSensor1) / 10f * SAMPLE_PERIOD);
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
//    */
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
//        //If there is a min value available, display
//        if(hda.getMinHR() != HeartDataAnalysis.NO_DATA) {
//            lowPosition = (hda.getMinHR() - MIN_POSSIBLE_HR) * positionPerHR;
//            mBinding.txtLowHr.setText(String.valueOf(hda.getMinHR()));
//        }
//        //If there is a max value available, display
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
//    private static int[] convertByteArray(byte[] in) {
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
//    //////////////////////////////////////////SAVING TO MEMORY/////////////////////////////////////
//    private void toggleRecord(MenuItem menuItem) {
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
//            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
//        popup.inflate(R.menu.popup_menu_pwv);
//
//        MenuItem recordMenuItem = popup.getMenu().findItem(R.id.record);
//        if(storeData)
//            recordMenuItem.setTitle("Stop recording");
//        else
//            recordMenuItem.setTitle("Record");
//
//
//        popup.show();
//    }
//
//
//    @Override
//    public boolean onMenuItemClick(MenuItem menuItem) {
//        switch(menuItem.getItemId()) {
//            case R.id.invertProximal:
//                proximalGain *= -1;
//                return true;
//            case R.id.invertDistal:
//                distalGain *= -1;
//                return true;
//            case R.id.record:
//                toggleRecord(menuItem);
//                return true;
//            default:
//                return false;
//        }
//    }
//
//    float mLastRandom = 2;
//    Random mRand = new Random();
//    private float getRandom() {
//        if(mLastRandom > MAX_VALUE - 0.25) {
//            return mLastRandom -= Math.abs(mRand.nextDouble()*0.5 - 0.25);
//        } else if (mLastRandom < MIN_VALUE + 0.25) {
//            return mLastRandom += Math.abs(mRand.nextDouble()*0.5 - 0.25);
//        }
//        return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
//    }
//
//    private static final double PERCENTAGE_NOISE = 0.2;
//    private static final float HR = 76 ;
//    private float sampleECG(float x, float delay) {
//        float frequency = (float) (1/HR * Math.PI);
//
//        //float out = 4 + (float)(MAX_VALUE*Math.pow(Math.sin(5*(x + delay)), 16) - MAX_VALUE*Math.pow(Math.sin(5*(x+delay)+2.9), 16));
//        float out = (float)(MAX_VALUE*Math.pow(Math.sin(5*(x + delay)), 16) + 0.4*(mRand.nextDouble()-0.5)*Math.sin(160*x + 0.3)- MAX_VALUE*Math.pow(Math.sin(5*(x + delay) + 2.9), 16));
//        out += 4+mRand.nextDouble()*MAX_VALUE*PERCENTAGE_NOISE - MAX_VALUE*PERCENTAGE_NOISE/2;
//
//
//        return out;
//    }
//}