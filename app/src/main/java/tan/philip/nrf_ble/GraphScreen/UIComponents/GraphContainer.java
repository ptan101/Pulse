package tan.philip.nrf_ble.GraphScreen.UIComponents;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import tan.philip.nrf_ble.GraphScreen.GraphSignal;
import tan.philip.nrf_ble.R;

public class GraphContainer extends LinearLayout {
    GraphView graphView;
    private float minX = 0;
    private float maxX = 10;


    public GraphContainer(Context context, GraphSignal signal) {
        super(context);

        initializeViews(context, signal);
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

    private void initializeViews(Context context, GraphSignal signal) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.graph_layout, this);

        //Attach the GraphView
        this.graphView = (GraphView) this.findViewById(R.id.graph_view);
        setupGraphView();

        //Set the display name
        TextView signalName = (TextView)this.findViewById(R.id.signal_name_display);
        signalName.setText(signal.getName());
        signalName.setTextColor(signal.getColorARGB());

        //Attach the data series to the graph
        graphView.addSeries(signal.getMonitor_series());
        graphView.addSeries(signal.getMonitor_mask());
    }

    private void setupGraphView() {
        //Set the GraphView y axis limites
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(-0.5);
        graphView.getViewport().setMaxY(0.5);

        graphView.getViewport().setXAxisBoundsManual(true);

        //Turn off GraphView vertical label
        graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);

        //Turn off GraphView horizontal label
        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        //No grid lines
        graphView.getGridLabelRenderer().setGridStyle( GridLabelRenderer.GridStyle.NONE );
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
