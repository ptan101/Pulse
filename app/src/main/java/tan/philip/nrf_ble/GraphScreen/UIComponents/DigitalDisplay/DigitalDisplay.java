package tan.philip.nrf_ble.GraphScreen.UIComponents.DigitalDisplay;

import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Html.FROM_HTML_MODE_LEGACY;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;

import tan.philip.nrf_ble.R;

public class DigitalDisplay extends LinearLayout {
    //public float value;
    public DigitalDisplaySettings settings;
    public TextView label;
    public ImageView icon;
    public ConstraintLayout layout;
    //Associate image here

    public DigitalDisplay (Context context, DigitalDisplaySettings settings) {
        super(context);
        this.settings = settings;

        initializeViews(context);
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.digital_display_layout, this);

        this.layout = (ConstraintLayout) this.findViewById(R.id.digital_display_container);
        label = (TextView) this.findViewById(R.id.digital_display_label);


        String displayName = settings.displayName;
        //Pad the label to maxTextLen
        for(int i = displayName.length(); i < settings.maxTextLen; i ++) {
            displayName += " ";
        }

        label.setText(displayName);
        label.setTextColor(Color.argb(255, 125, 125, 125));
        //label.setId(View.generateViewId());

        icon = (ImageView) this.findViewById(R.id.digital_display_icon);
        switch (settings.iconName) {
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
        //icon.setId(View.generateViewId());

        /*
        float factor = context.getResources().getDisplayMetrics().density;
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams((int) (30 * factor), (int) (30 * factor));
        icon.setLayoutParams(layoutParams);

         */
    }

    public void changeValue(float newValue) {
        String labelText = settings.prefix + settings.decimalFormat.format(newValue) + settings.suffix;
        label.setText(Html.fromHtml(labelText, FROM_HTML_MODE_LEGACY ));

        //ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        //label.setLayoutParams(layoutParams);

        label.invalidate();
        label.requestLayout();
        //layout.requestLayout();


        //this.setLayoutParams(layoutParams);
        //this.invalidate();
        //this.requestLayout();

    }

    public void changeValue(String newValue) {
        label.setText(newValue);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }
}
