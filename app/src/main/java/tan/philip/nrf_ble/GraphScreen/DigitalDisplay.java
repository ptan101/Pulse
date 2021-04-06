package tan.philip.nrf_ble.GraphScreen;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;

import tan.philip.nrf_ble.R;

public class DigitalDisplay {
    public float value;
    public String name;
    public TextView label;
    public ImageView icon;
    public ConstraintLayout layout;
    //Associate image here

    public DigitalDisplay (Context context, String name, String image_icon) {
        this.name = name;

        label = new TextView(context);
        label.setText(this.name);
        label.setTextColor(Color.argb(255, 125, 125, 125));
        label.setId(View.generateViewId());

        icon = new ImageView(context);
        switch (image_icon) {
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
        icon.setLayoutParams(new ViewGroup.LayoutParams((int) (30 * factor), (int) (30 * factor)));

    }

    public static void addToDigitalDisplay(DigitalDisplay newDisplay, ConstraintLayout layout_left, ConstraintLayout layout_center, ConstraintLayout layout_right, ArrayList<DigitalDisplay> digitalDisplays) {
        //If the data is in a new row, initially put it in the center column
        //Otherwise, put in the right column and put the last element in the left column
        digitalDisplays.add(newDisplay);

        if(digitalDisplays.size() % 2 == 1) {
            addToDigitalDisplay(layout_center, newDisplay.icon, newDisplay.label);
        } else {
            TextView label = digitalDisplays.get(digitalDisplays.size() - 2).label;
            ImageView icon = digitalDisplays.get(digitalDisplays.size() - 2).icon;

            layout_center.removeView(label);
            layout_center.removeView(icon);

            if (digitalDisplays.size() <= 2) {
                addToDigitalDisplay(layout_left, icon, label);
                addToDigitalDisplay(layout_right, newDisplay.icon, newDisplay.label);
            } else {
                addToDigitalDisplay(layout_left, icon, label, digitalDisplays.get(digitalDisplays.size() - 4).icon.getId());
                addToDigitalDisplay(layout_right, newDisplay.icon, newDisplay.label, digitalDisplays.get(digitalDisplays.size() - 3).icon.getId());
            }
        }
    }

    //Add to the first row
    private static void addToDigitalDisplay(ConstraintLayout layout, ImageView icon, TextView label) {
        ConstraintSet set = new ConstraintSet();

        layout.addView(label);
        layout.addView(icon);

        set.clone(layout);
        int[] viewIds = {icon.getId(), label.getId()};

        set.connect(icon.getId(), ConstraintSet.TOP, layout.getId(), ConstraintSet.TOP);
        set.connect(icon.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START);
        set.connect(icon.getId(), ConstraintSet.END, label.getId(), ConstraintSet.START,10);

        set.connect(label.getId(), ConstraintSet.TOP, icon.getId(), ConstraintSet.TOP);
        set.connect(label.getId(), ConstraintSet.BOTTOM, icon.getId(), ConstraintSet.BOTTOM);
        set.connect(label.getId(), ConstraintSet.START, icon.getId(), ConstraintSet.END);
        set.connect(label.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END);

        set.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, viewIds, null, ConstraintSet.CHAIN_PACKED);

        set.applyTo(layout);
    }

    //Add to subsequent rows
    private static void addToDigitalDisplay(ConstraintLayout layout, ImageView icon, TextView label, int id_Constrain_To_Bottom) {
        ConstraintSet set = new ConstraintSet();

        layout.addView(label);
        layout.addView(icon);

        set.clone(layout);
        int[] viewIds = {icon.getId(), label.getId()};

        set.connect(icon.getId(), ConstraintSet.TOP, id_Constrain_To_Bottom, ConstraintSet.BOTTOM, 50);
        set.connect(icon.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START);
        set.connect(icon.getId(), ConstraintSet.END, label.getId(), ConstraintSet.START,10);

        set.connect(label.getId(), ConstraintSet.TOP, icon.getId(), ConstraintSet.TOP);
        set.connect(label.getId(), ConstraintSet.BOTTOM, icon.getId(), ConstraintSet.BOTTOM);
        set.connect(label.getId(), ConstraintSet.START, icon.getId(), ConstraintSet.END);
        set.connect(label.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END);

        set.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, viewIds, null, ConstraintSet.CHAIN_PACKED);

        set.applyTo(layout);
    }

}
