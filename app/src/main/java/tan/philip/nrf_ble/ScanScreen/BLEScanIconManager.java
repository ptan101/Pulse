package tan.philip.nrf_ble.ScanScreen;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

import tan.philip.nrf_ble.Events.Connecting.BLEIconNumSelectedChangedEvent;
import tan.philip.nrf_ble.Events.Connecting.BLEIconSelectedEvent;
import tan.philip.nrf_ble.Events.PlotDataEvent;
import tan.philip.nrf_ble.R;

public class BLEScanIconManager {
    Random rand;
    ArrayList<BLEScanIcon> icons;
    ArrayList<String> selectedAddresses;
    ConstraintLayout mLayout;

    public BLEScanIconManager(ConstraintLayout layout) {
        rand = new Random();
        icons = new ArrayList<>();
        selectedAddresses = new ArrayList<>();
        mLayout = layout;

        register();
    }

    public void register() {
        //Register on EventBus
        EventBus.getDefault().register(this);
    }

    public void deregister() {
        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
    }

    public void generateNewIcon(Context ctx, String name, String address, int rssi, int imageResource) {
        BLEScanIcon newIcon = new BLEScanIcon(ctx, name, address, rssi, imageResource);
        newIcon.setVisibility(View.GONE);
        mLayout.addView(newIcon);

        //This is super goofy but basically the view needs to be drawn before we can get the dimensions.
        //After this runnable, the view is fully drawn.
        newIcon.post(new Runnable() {
            @Override
            public void run() {
                newIcon.getHeight(); //height is ready
                newIcon.getWidth();
                setIconLocation(newIcon);
            }
        });
    }

    public void removeIcon(String address) {
        for(BLEScanIcon icon : icons) {
            if(icon.getAddress().equals(address)) {
                icon.fadeOut();
                icons.remove(icon);
                mLayout.removeView(icon);
                selectedAddresses.remove(icon.getAddress());
                return;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateSelectedIcons(BLEIconSelectedEvent event) {
        if(event.isSelected())
            selectedAddresses.add(event.getAddress());
        else
            selectedAddresses.remove(event.getAddress());

        EventBus.getDefault().post(new BLEIconNumSelectedChangedEvent(selectedAddresses.size()));
    }

    public ArrayList<String> getSelectedAddresses() {
        return selectedAddresses;
    }

    private void setIconLocation(BLEScanIcon newIcon) {
        int xpos;
        int ypos;
        int width = newIcon.getWidth();
        int height = newIcon.getHeight();

        //Temporary solution. Find out why incorrect width and height are still being produced.
        if (width == 0)
            width = 100;
        if (height == 0)
            height = 100;


        int numTries = 1; //To Do: if too many failed attempts, do something

        //Pretty inefficient with many icons but it's fine with just a few.
        do {
            xpos = 0;//rand.nextInt(mLayout.getResources().getDisplayMetrics().widthPixels - width);
            ypos = 0;//rand.nextInt(mLayout.getResources().getDisplayMetrics().heightPixels - 2 * height) + height;

            numTries ++;
        } while (checkAllOverlaps(xpos, ypos, newIcon));

        newIcon.setX(xpos);
        newIcon.setY(ypos);

        icons.add(newIcon);
        newIcon.setVisibility(View.VISIBLE);
        newIcon.fadeIn();
    }

    private boolean checkAllOverlaps(int xpos, int ypos, BLEScanIcon icon1) {
        boolean hasOverlap;
        for (BLEScanIcon icon2 : icons) {
            hasOverlap = checkOverlap(xpos, ypos, icon1, icon2);

            if(hasOverlap)
                return true;
        }

        return checkOverlap(xpos, ypos, icon1, mLayout.getViewById(R.id.btn_Scan));
    }

    private boolean checkOverlap(int xpos, int ypos, BLEScanIcon icon1, View icon2) {
        int[] firstPosition = {xpos, ypos};
        int[] secondPosition = {(int)icon2.getX(), (int)icon2.getY()};

        // Rect constructor parameters: left, top, right, bottom
        Rect rectFirstView = new Rect(firstPosition[0], firstPosition[1],
                firstPosition[0] + icon1.getWidth(), firstPosition[1] + icon1.getHeight());
        Rect rectSecondView = new Rect(secondPosition[0], secondPosition[1],
                secondPosition[0] + icon2.getWidth(), secondPosition[1] + icon2.getHeight());


        return rectFirstView.intersect(rectSecondView);
    }
}
