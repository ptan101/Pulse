package tan.philip.nrf_ble.GraphScreen.UIComponents;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;

import tan.philip.nrf_ble.R;

public class DigitalDisplay extends LinearLayout {
    //public float value;
    public String name;
    public TextView label;
    public ImageView icon;
    public ConstraintLayout layout;
    //Associate image here

    public DigitalDisplay (Context context, String name, String imageIcon) {
        super(context);
        this.name = name;

        initializeViews(context, imageIcon);
    }

    private void initializeViews(Context context, String imageIcon) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.digital_display_layout, this);

        label = (TextView) this.findViewById(R.id.digital_display_label);
        label.setText(this.name);
        label.setTextColor(Color.argb(255, 125, 125, 125));
        label.setId(View.generateViewId());

        icon = (ImageView) this.findViewById(R.id.digital_display_icon);
        switch (imageIcon) {
            case "heartrate":
                icon.setBackgroundResource(R.drawable.heartrate);
                break;
            case "pwv":
                icon.setBackgroundResource(R.drawable.pwv);
                break;
            case "spo2":
                icon.setBackgroundResource(R.drawable.spo2);
                break;
            case "temperature":
                icon.setBackgroundResource(R.drawable.temp);
                break;
            default:
                icon.setBackgroundResource(R.drawable.heartrate);
                break;
        }
        icon.setId(View.generateViewId());

        float factor = context.getResources().getDisplayMetrics().density;
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams((int) (30 * factor), (int) (30 * factor));
        icon.setLayoutParams(layoutParams);
    }

    public void changeValue(float newValue) {
        label.setText(Float.toString(newValue));
    }

    public void changeValue(String newValue) {
        label.setText(newValue);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }
}
