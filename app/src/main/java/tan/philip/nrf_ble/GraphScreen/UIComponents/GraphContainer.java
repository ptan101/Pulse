package tan.philip.nrf_ble.GraphScreen.UIComponents;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import static tan.philip.nrf_ble.Constants.convertDpToPixel;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;

import tan.philip.nrf_ble.GraphScreen.GraphSignal;
import tan.philip.nrf_ble.R;

public class GraphContainer extends LinearLayout {
    GraphView graphView;
    GraphSignal graphSignal;
    private float minX = 0;
    private float maxX = 10;
    private Context context;
    private float range = 1;
    private int seekbarProgress = 50;
    private boolean autoscale = false;

    public GraphContainer(Context context, GraphSignal signal) {
        super(context);
        this.context = context;
        this.graphSignal = signal;

        initializeViews(context);
    }

    public GraphContainer(Context context, @Nullable AttributeSet attrs, GraphSignal signal) {
        super(context, attrs);
        new GraphContainer(context, signal);
    }

    public GraphContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr, GraphSignal signal) {
        super(context, attrs, defStyleAttr);
        new GraphContainer(context, attrs, signal);
    }

    public GraphContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, GraphSignal signal) {
        super(context, attrs, defStyleAttr, defStyleRes);
        new GraphContainer(context, attrs, defStyleAttr, signal);
    }

    public void setViewportMinX(float minX) {
        this.minX = minX;
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(minX);
    }

    public void setViewportMaxX(float maxX) {
        this.maxX = maxX;
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMaxX(maxX);
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.graph_layout, this);

        //Attach the GraphView
        this.graphView = (GraphView) this.findViewById(R.id.graph_view);
        setupGraphView();

        //Set the display name
        TextView signalName = (TextView)this.findViewById(R.id.signal_name_display);
        signalName.setText(graphSignal.getName());
        signalName.setTextColor(graphSignal.getColorARGB());

        //Attach the data series to the graph
        graphView.addSeries(graphSignal.getMonitor_series());
        graphView.addSeries(graphSignal.getMonitor_mask());

        graphView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupWindow();
            }
        });
    }

    private void setupGraphView() {
        //Set the GraphView y axis limites
        graphView.getViewport().setYAxisBoundsManual(!autoscale);
        graphView.getViewport().setMinY(-range/2);
        graphView.getViewport().setMaxY(range/2);

        graphView.getViewport().setXAxisBoundsManual(true);

        //Turn off GraphView vertical label
        graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);

        //Turn off GraphView horizontal label
        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        //No grid lines
        graphView.getGridLabelRenderer().setGridStyle( GridLabelRenderer.GridStyle.NONE );

        //Set the layout height
        //NOTE: This seems to be very slow. TO DO: look into speeding up
        graphView.setLayoutParams(new ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                (int) convertDpToPixel(graphSignal.getLayoutHeight(), context)));
    }

    /**
     * Popup window that displays the amplification bar.
     */
    private void showPopupWindow() {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.graphview_popup_window, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = graphView.getHeight();

        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        SeekBar seekBar = popupView.findViewById(R.id.amplifcation_seek_bar);
        seekBar.setEnabled(!autoscale);
        seekBar.setProgress(seekbarProgress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekbarProgress = progress;
                range = 1 / (float)Math.pow(10f, (float) progress / 50f - 1);
                graphView.getViewport().setMinY(-range/2);
                graphView.getViewport().setMaxY(range/2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Autoscale switch
        Switch autoscaleSwitch = popupView.findViewById(R.id.switch_autoscale);
        autoscaleSwitch.setChecked(autoscale);
        autoscaleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAutoscale(isChecked);
                seekBar.setEnabled(!autoscale);
            }
        });

        popupWindow.showAsDropDown(this, 0, -height, Gravity.TOP | Gravity.LEFT);
    }

    public void setAutoscale(boolean autoscale) {
        this.autoscale = autoscale;
        graphView.getViewport().setYAxisBoundsManual(!autoscale);
        if(!autoscale){
            range = 1 / (float)Math.pow(10f, (float) seekbarProgress / 50f - 1);
            graphView.getViewport().setMinY(-range/2);
            graphView.getViewport().setMaxY(range/2);
        }


    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public int getBackgroundColor() {
        ColorDrawable viewColor = (ColorDrawable) this.findViewById(R.id.graph_constraint_layout).getBackground();
        return viewColor.getColor();
    }
}