package tan.philip.nrf_ble.GraphScreen;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.databinding.DataBindingUtil;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import tan.philip.nrf_ble.BLE.BLEHandlerService;
import tan.philip.nrf_ble.BLE.FileWriter;
import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity;
import tan.philip.nrf_ble.BLE.SignalSetting;
import tan.philip.nrf_ble.databinding.ActivityPwvgraphBinding;

import static tan.philip.nrf_ble.BLE.FileWriter.writeCSV;
import static tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity.EXTRA_BT_IDENTIFIER;

public class GraphActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    //To Do:
    //1 Menu remove old deprecated stuff
    //3 Menu disconnect/reconnect
    public static final String TAG = "PWVGraphActivity";
    public static final int MAX_POINTS_PLOT = 10000;

    private final float MIN_Y = 0;
    private final float MAX_Y = 12;
    private final float MARGINS = 1;
    private static final int MAX_MONITOR_DISPLAY_LENGTH = 10; //seconds

    //Graphing Initializers
    private GraphView graph;
    private boolean graph1Scrollable = false;
    private ArrayList<TextView> legend = new ArrayList<TextView>();
    private ArrayList<DigitalDisplay> digitalDisplays = new ArrayList<>();

    private ArrayList<GraphSignal> signals = new ArrayList<>();
    private PointsGraphSeries<DataPoint> monitor_mask = new PointsGraphSeries<>();
    private DataPoint[] mask = new DataPoint[1];

    //Graphing, filtering, etc.
    private int notification_frequency;
    private int notification_period;
    private float amplification = 5;
    private boolean monitorView = true;
    private float t = 0;

    private Biometrics biometrics;

    private ActivityPwvgraphBinding mBinding;
    private String deviceIdentifier;
    private ColorStateList defaultTextColor;

    private final Handler mHandler = new Handler();
    private Runnable graphTimer;

    //Saving
    private boolean storeData = false;
    private long startRecordTime;
    DecimalFormat decimalFormat;
    private String fileName;    //This is for event marking.
    private float startRecordTimeEventMarker;  //Time based on notifications for event marking.

    //Stuff for interacting with the service
    Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new GraphActivity.IncomingHandler());
    boolean mConnected = true;

    ////////////////////Methods for communicating with BLEHandlerService///////////////////////////

    //Handles messages from the BLEHandlerService
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BLEHandlerService.MSG_GATT_FAILED:
                case BLEHandlerService.MSG_GATT_DISCONNECTED:
                    runOnUiThread(() -> Toast.makeText(GraphActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show());
                    mBinding.textView5.setText(deviceIdentifier + " disconnected");
                    mBinding.textView5.setTextColor(Color.rgb(255,0,0));
                    mConnected = false;
                    invalidateOptionsMenu();
                    writeCSV(new String[] {Float.toString((t - startRecordTimeEventMarker) / 1000), "Device disconnected"}, fileName);
                    stopRecord();
                    break;
                case BLEHandlerService.MSG_GATT_ACTION_DATA_AVAILABLE:
                    displayData( (ArrayList<float[]>)msg.getData().getSerializable("btData"));
                    mBinding.recordTimer.setText(decimalFormat.format((System.currentTimeMillis() - startRecordTime) / 1000f));

                    break;
                case BLEHandlerService.MSG_GATT_CONNECTED:
                    mConnected = true;
                    mBinding.textView5.setText("Reading from " + deviceIdentifier);
                    mBinding.textView5.setTextColor(defaultTextColor);
                    invalidateOptionsMenu();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, BLEHandlerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
        }
    };

    void doBindService() {
        mIsBound = bindService(new Intent(GraphActivity.this, BLEHandlerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, BLEHandlerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void sendMessageToService(int msgID) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, msgID);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    private void sendStringToService(int msgID, String s) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, msgID, s);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    ////////////////////////////////////////Life cycle methods///////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pwvgraph);
        invalidateOptionsMenu();

        //Setup from prior activity
        Bundle extras = getIntent().getExtras();
        deviceIdentifier = extras.getString(EXTRA_BT_IDENTIFIER);

        //UI Setup
        setupButtons();


        //Graphing setup
        graph = mBinding.graph1;
        monitor_mask = new PointsGraphSeries<>();
        decimalFormat = new DecimalFormat("#.####");

        setupGraph((ArrayList<SignalSetting>)extras.getSerializable(ScanResultsActivity.EXTRA_SIGNAL_SETTINGS_IDENTIFIER));
        biometrics = (Biometrics)extras.getSerializable(ScanResultsActivity.EXTRA_BIOMETRIC_SETTINGS_IDENTIFIER);
        setupBiometricsDigitalDisplay();
        notification_frequency = extras.getInt(ScanResultsActivity.EXTRA_NOTIF_F_IDENTIFIER);
        notification_period = 1000 / notification_frequency;

        //Bind activity to service
        doBindService();

    }

    @Override
    protected  void onResume() {
        super.onResume();

        resetViewport();


        graphTimer = new Runnable() {
            @Override
            public void run() {
                //Reached end of data, scroll automatically
                if(!monitorView && graph1Scrollable && graph.getViewport().getMinX(false) < graph.getViewport().getMinX(true)) {
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

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Unbind the service
        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
        }
    }

    @Override
    public void onBackPressed() {
        if(mConnected) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Terminating connection")
                    .setMessage("Are you sure you want to terminate the BLE connection?")
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendMessageToService(BLEHandlerService.MSG_DISCONNECT);
                            finish();
                        }

                    })
                    .show();
        } else {
            finish();
        }
    }

    ////////////////////////////////////GRAPH STUFF////////////////////////////////////////////////

    private void setupGraph(ArrayList<SignalSetting> signalSettings) {
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);

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
        graph.getViewport().setMinY(MIN_Y - MARGINS);
        graph.getViewport().setMaxY(MAX_Y + MARGINS);

        //graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);
        graph.getViewport().setXAxisBoundsManual(true);

        //Click on graph to enable scroll in interactive view
        graph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!graph1Scrollable && !monitorView) {
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

        int color = Color.WHITE;
        Drawable background = mBinding.backgroundCL.getBackground();
        if (background instanceof ColorDrawable)
            color = ((ColorDrawable) background).getColor();

        monitor_mask.setColor(color);
        monitor_mask.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                paint.setStrokeWidth(50);
                canvas.drawLine(x, y, x, y-2000, paint);
            }
        });

        //Set up signals
        int num_graphable = 0;
        for (int i = 0; i < signalSettings.size(); i ++) {
            SignalSetting cur_setting = signalSettings.get(i);
            signals.add(new GraphSignal(cur_setting));

            //This is getting hairy. Should lump setting loading together
            if (cur_setting.graphable)
                num_graphable++;
            if(cur_setting.decimalFormat != null)
                signals.get(i).decimalFormat = new DecimalFormat(cur_setting.decimalFormat);
            if(cur_setting.conversion != null)
                signals.get(i).conversion = cur_setting.conversion;
            if(cur_setting.prefix != null)
                signals.get(i).prefix = cur_setting.prefix;
            if(cur_setting.suffix != null)
                signals.get(i).suffix = cur_setting.suffix;

            signals.get(i).setupDisplay(this);
        }

        //For graphable signals, set the y offset
        for (int i = 0; i < signalSettings.size(); i ++) {
            if (signalSettings.get(i).graphable) {
                float offset = (num_graphable - i) * (MAX_Y - MIN_Y) / (num_graphable + 1);
                signals.get(i).addGraphSettings(offset, MAX_MONITOR_DISPLAY_LENGTH);
            }
        }

        //resetViewport();
        resetAllSeries();
    }

    private void resetAllSeries() {
        graph.removeAllSeries();

        for (GraphSignal signal: signals) {
            if(signal.graphable()) {
                signal.resetSeries();
                monitorView = true;
                graph.addSeries(signal.monitor_series);
                graph.addSeries((monitor_mask));
            }
        }
    }

    private void setSeriesPaint(int a, int r, int g, int b, int strokeWidth, LineGraphSeries<DataPoint> series) {
        Paint sensorPaint = new Paint();
        sensorPaint.setARGB(a, r, g, b);
        sensorPaint.setStrokeWidth(strokeWidth);
        series.setCustomPaint(sensorPaint);
    }

    private void resetViewport() {
        if(!monitorView) {
            graph1Scrollable = false;
            graph.getViewport().setScrollable(false);
            graph.getViewport().setScalable(false);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(6);
            graph.getViewport().scrollToEnd();
        }

        mBinding.btnReset.setVisibility(View.GONE);
    }

    private void displayData(ArrayList<float[]> new_data) {
        for (int i = 0; i < new_data.size(); i ++) {
            //Each signal_packet is one signal that was parsed from the whole BLE packet
            float[] signal_packet = new_data.get(i);

            //Determine the time between each data point as the time between packages / number of data points per package
            float deltaT = notification_period / signal_packet.length;  //In ms

            for (int j = 0; j < signal_packet.length; j++) {
                //Plot every data point in the packet. We alter both series since we want them to both be updated when we switch between
                if(signals.get(i).graphable()) {
                    //Log.d("", Float.toString(signal_packet[j]));
                    float x = (t + (j * deltaT)) / 1000;
                    float y = signal_packet[j]  / (float) Math.pow(2, signals.get(i).bitResolution) * amplification  + signals.get(i).offset;// * proximal_gain * amplification + offsets[j];

                    //Add the data to the interactive series
                    signals.get(i).interactive_series.appendData(new DataPoint(x, y), !graph1Scrollable, MAX_POINTS_PLOT);

                    //Add the data to the monitor series;
                    signals.get(i).addDataToMonitorBuffer(y);
                }

                if(signals.get(i).useDigitalDisplay) {
                    //Do something here
                    signals.get(i).setDigitalDisplayText(signal_packet[j]);
                }
            }
        }

        //Draw mask over old monitor points
        mask[0] = new DataPoint((float)((int) t % (MAX_MONITOR_DISPLAY_LENGTH * 1000)) / 1000f, -0.5);
        monitor_mask.resetData(mask);

        t += notification_period;
    }

    private void toggleView() {
        if(monitorView) {
            monitorView = false;

            resetViewport();

            //Remove series from graph
            graph.removeAllSeries();

            //Add the correct series
            for(int i = 0; i < signals.size(); i++) {
                if (signals.get(i).monitor_series != null){
                    graph.addSeries(signals.get(i).interactive_series);
                    signals.get(i).startMonitorView();
                }
            }
        } else {
            monitorView = true;

            //Set viewport to 10 seconds
            //graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(10);

            //Remove series from graph
            graph.removeAllSeries();

            //Add the correct series
            for(int i = 0; i < signals.size(); i++) {
                if(signals.get(i).monitor_series != null)
                    graph.addSeries(signals.get(i).monitor_series);
            }
            graph.addSeries(monitor_mask);

            mBinding.btnReset.setVisibility(View.GONE);
        }
    }

    ////////////////////////////////////////////////GRAPHICAL UI//////////////////////////////////////////////////////////////////////////////////

    private void setupBiometricsDigitalDisplay() {
        ArrayList<Integer> hr_list = biometrics.getHr_signals();
        for (Integer i : hr_list) {
            DigitalDisplay.addToDigitalDisplay(new DigitalDisplay(this, "Heart rate", "heartrate"),
                    mBinding.digitalDisplayLeft, mBinding.digitalDisplayCenter, mBinding.digitalDisplayRight, digitalDisplays);
        }

        ArrayList<int[]> spo2_list = biometrics.getSpo2_signals();
        for (int[] i : spo2_list) {
            DigitalDisplay.addToDigitalDisplay(new DigitalDisplay(this, "SpO2", "spo2"),
                    mBinding.digitalDisplayLeft, mBinding.digitalDisplayCenter, mBinding.digitalDisplayRight, digitalDisplays);
        }

        ArrayList<int[]> pwv_list = biometrics.getPwv_signals();
        for (int[] i : pwv_list) {
            DigitalDisplay.addToDigitalDisplay(new DigitalDisplay(this, "PWV", "pwv"),
                    mBinding.digitalDisplayLeft, mBinding.digitalDisplayCenter, mBinding.digitalDisplayRight, digitalDisplays);
        }
    }


    //////////////////////////////////////////SEND DATA TO NRF/////////////////////////////////////
    /*
    private void checkSD() {
        byte[] output = {CHECK_SD};
        mBluetoothLeService.writeCharacteristic(output);
    }
    */

    //////////////////////////////////////////SAVING TO MEMORY/////////////////////////////////////
    private void startRecord() {
        if(FileWriter.isStoragePermissionGranted(this)) {
            final EditText input = new EditText(this);
            input.setText(Calendar.getInstance().getTime().toString());

            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Please give the file a name.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Start Record", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startRecordTime = System.currentTimeMillis();
                            startRecordTimeEventMarker = t;
                            mBinding.recordTimer.setVisibility(View.VISIBLE);
                            storeData = true;
                            fileName = input.getText().toString();
                            fileName = fileName.replace(":", "");
                            sendStringToService(BLEHandlerService.MSG_START_RECORD, fileName);
                        }

                    })
                    .setView(input)
                    .show();

        } else {
            Toast.makeText(this, "Storage Permission is not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecord() {
        sendMessageToService(BLEHandlerService.MSG_STOP_RECORD);
        mBinding.recordTimer.setVisibility(View.INVISIBLE);
        storeData = false;
    }

    private void toggleRecord() {
        if(!storeData) {
            startRecord();
        } else {
            stopRecord();
        }
    }

    //////////////////////////////////////////User Interface//////////////////////////////////////////
    //To do: fix this
    private static final int MENU_MARK_EVENT = 0;
    private static final int MENU_DISCONNECT_BLE = 1;
    private static final int MENU_RECONNECT_BLE = 2;

    public void showOptions(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);

        popup.inflate(R.menu.popup_menu_graph);

        //Record Text
        MenuItem recordMenuItem = popup.getMenu().findItem(R.id.record);
        if(storeData) {
            recordMenuItem.setTitle("Stop recording");
            popup.getMenu().add(0, MENU_MARK_EVENT, Menu.NONE, "Mark Event");
        } else
            recordMenuItem.setTitle("Record");

        if(mConnected)
            popup.getMenu().add(0, MENU_DISCONNECT_BLE, Menu.NONE, "Disconnect");
        else
            popup.getMenu().add(0, MENU_RECONNECT_BLE, Menu.NONE, "Reconnect");

        //View Text
        MenuItem switchViewMenuItem = popup.getMenu().findItem(R.id.switchView);
        if(monitorView)
            switchViewMenuItem.setTitle("Interactive View");
        else
            switchViewMenuItem.setTitle("Patient Monitor View");

        popup.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.record:
                toggleRecord();
                return true;
            case R.id.switchView:
                toggleView();
                return true;
            case MENU_MARK_EVENT:
                final EditText input = new EditText(this);

                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Label this event?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Mark Event", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d("", "Event marked! t = " + Float.toString(t - startRecordTimeEventMarker));
                                writeCSV(new String[] {Float.toString((t - startRecordTimeEventMarker) / 1000), input.getText().toString()}, fileName);
                            }
                        })
                        .setView(input)
                        .show();
                return true;
            case MENU_DISCONNECT_BLE:
                sendMessageToService(BLEHandlerService.MSG_DISCONNECT);
                return true;
            case MENU_RECONNECT_BLE:
                sendStringToService(BLEHandlerService.MSG_CONNECT, deviceIdentifier.substring(deviceIdentifier.indexOf("(")+1, deviceIdentifier.indexOf(")")));
                return true;
            default:
                return false;
        }
    }



    //////////////////Setup//////////////////////////////////////////////////////////////
    private void setupButtons() {
        mBinding  = DataBindingUtil.setContentView(this, R.layout.activity_pwvgraph);
        mBinding.recordTimer.setVisibility(View.INVISIBLE);
        mBinding.textView5.setText("Reading from device " + deviceIdentifier);
        defaultTextColor = mBinding.textView5.getTextColors();
        mBinding.btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetViewport();
            }
        });
        mBinding.amplification.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //amplification = i / 10; //(float) (Math.pow(2, (float) i / 9) / 30);
                amplification = (float)Math.pow(10f, (float)i / 30f) / 10f;

                //resetAllSeries();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    class GraphSignal {
        int fs;
        int sample_period;
        float offset;
        int num_points = 0;
        String name;
        int bitResolution;

        //For graphable
        boolean graphable = false;
        LineGraphSeries<DataPoint> interactive_series;
        LineGraphSeries<DataPoint> monitor_series;
        DataPoint[] monitor_buffer;
        int[] color;

        //For Digital Dispaly
        boolean useDigitalDisplay = false;          //Probably could remove this and just check if DD is null
        DigitalDisplay digitalDisplay = null;
        DecimalFormat decimalFormat = new DecimalFormat("###.#");   //Default format in case user doesn't define one
        String conversion = "x";                                            //Default conversion (output exactly what is given)
        String prefix = "";
        String suffix = "";

        public GraphSignal(SignalSetting settings) {
            this.fs = settings.fs;
            this.name = settings.name;

            this.graphable = settings.graphable;
            this.useDigitalDisplay = settings.digitalDisplay;
            this.bitResolution = settings.bitResolution;

            this.color = settings.color;
            sample_period = 1000 /  fs;
        }

        public void addGraphSettings(float offset, int monitor_length) {
            this.offset = offset;

            interactive_series = new LineGraphSeries<>();
            monitor_series = new LineGraphSeries<>();
            monitor_buffer = new DataPoint[fs * monitor_length];

            resetSeries();
        }

        public void resetSeries() {
            if(graphable) {
                interactive_series = new LineGraphSeries<>();
                monitor_series = new LineGraphSeries<>();


                for (int i = 0; i < monitor_buffer.length; i++) {
                    monitor_buffer[i] = new DataPoint(i * (float) sample_period / 1000f, offset);
                }
                monitor_series.resetData(monitor_buffer);
                setColor(color);
            }
        }

        public void addDataToMonitorBuffer(float data) {
            int cur_index = num_points % monitor_buffer.length;
            monitor_buffer[cur_index] = new DataPoint(((float) (cur_index * sample_period)) / 1000f, data);
            num_points ++;

            monitor_series.resetData(monitor_buffer);
        }

        public void setColor(int[] color) {
            this.color = color;
            setSeriesPaint(color[3], color[0], color[1], color[2], 5, interactive_series);
            setSeriesPaint(color[3], color[0], color[1], color[2], 5, monitor_series);
        }

        public int getColorARGB() {
            return Color.argb(color[3], color[0], color[1], color[2]);
        }

        public boolean graphable() {
            return graphable;
        }

        public void startMonitorView() {
            monitor_series.resetData(monitor_buffer);
        }

        //Depending on if it is a graphable or digitial display, add the right UI
        public void setupDisplay(Context context) {
            //If graphable, set up legends
            if(this.graphable) {
                //Add a new TextView to the legend with the signal name
                ConstraintSet set = new ConstraintSet();
                TextView signal_name = new TextView(context);
                signal_name.setText(this.name);
                signal_name.setTextColor(this.getColorARGB());
                signal_name.setId(View.generateViewId());
                mBinding.graphLegendCL.addView(signal_name);

                set.clone(mBinding.graphLegendCL);
                if(legend.size() == 0) {
                    set.connect(signal_name.getId(), ConstraintSet.TOP, mBinding.graphLegendCL.getId(), ConstraintSet.TOP, 20);
                } else {
                    set.connect(signal_name.getId(), ConstraintSet.TOP, legend.get(legend.size() - 1).getId(), ConstraintSet.BOTTOM, 0);
                }
                set.connect(signal_name.getId(), ConstraintSet.END, mBinding.graphLegendCL.getId(), ConstraintSet.END, 60);
                set.applyTo(mBinding.graphLegendCL);
                legend.add(signal_name);
            }

            //If digital display, set up digital display
            //Try making constraint layout for each
            if(this.useDigitalDisplay) {
                //Add a new TextView to the legend with the signal name
                digitalDisplay = new DigitalDisplay(context, this.name, "temperature");
                DigitalDisplay.addToDigitalDisplay(digitalDisplay, mBinding.digitalDisplayLeft, mBinding.digitalDisplayCenter, mBinding.digitalDisplayRight, digitalDisplays);
            }
        }

        public void setDigitalDisplayText(Float data) {
            //Convert the signal packet into the desired format

            //First, replace 'x' in the string with actual data
            String func = conversion.replace("x", Float.toString(data));

            //Now, evaluate the function
            Double evaluation = DigitalDisplay.eval(func);

            //Format text and display
            this.digitalDisplay.label.setText(Html.fromHtml(prefix + decimalFormat.format(evaluation) + suffix));
        }
    }
}