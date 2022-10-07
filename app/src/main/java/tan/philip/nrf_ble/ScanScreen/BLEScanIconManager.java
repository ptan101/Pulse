package tan.philip.nrf_ble.ScanScreen;

import static tan.philip.nrf_ble.Constants.convertDpToPixel;
import static tan.philip.nrf_ble.Constants.convertPixelsToDp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import tan.philip.nrf_ble.Events.Connecting.BLEIconNumSelectedChangedEvent;
import tan.philip.nrf_ble.Events.Connecting.BLEIconSelectedEvent;
import tan.philip.nrf_ble.Events.PlotDataEvent;
import tan.philip.nrf_ble.R;

public class BLEScanIconManager {
    private static final float DEGREES_BETWEEN_DEVICES = 45;
    private static final float RADIUS_FROM_CENTER = 125; //DPI
    private final Map<String, BLEScanIcon> icons; //Should make this a hashmap honestly
    private final ArrayList<String> selectedAddresses;
    private final ConstraintLayout mLayout;
    private final Context context;

    public BLEScanIconManager(ConstraintLayout layout, Context ctx) {
        icons = new HashMap<>();
        selectedAddresses = new ArrayList<>();
        mLayout = layout;
        this.context = ctx;

        register();
    }

    public void deselectAllIcons() {
        for (String address : icons.keySet()) {
            icons.get(address).setSelected(false);
        }
        selectedAddresses.clear();
    }

    public void clearAllIcons() {
        for (String address : icons.keySet()) {
            BLEScanIcon icon = icons.get(address);
            icon.fadeOut();
            icons.remove(icon);
            mLayout.removeView(icon);
            selectedAddresses.remove(icon.getAddress());
        }
        EventBus.getDefault().post(new BLEIconNumSelectedChangedEvent(selectedAddresses.size()));
    }

    public void register() {
        //Register on EventBus
        EventBus.getDefault().register(this);
    }

    public void deregister() {
        //Unregister from EventBus
        EventBus.getDefault().unregister(this);
    }

    public void generateNewIcon(Context ctx, String name, String address, int rssi, int imageResource, boolean isInitialized) {
        BLEScanIcon newIcon = new BLEScanIcon(ctx, name, address, rssi, imageResource, isInitialized);
        newIcon.setVisibility(View.INVISIBLE);
        mLayout.addView(newIcon);

        //setIconLocation(newIcon);

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

    public void updateRSSI(String address, int rssi) {
        BLEScanIcon icon = icons.get(address);
        icon.setRSSI(rssi);
    }

    public void removeIcon(String address) {
        BLEScanIcon icon = icons.get(address);
        icon.fadeOut();
        icons.remove(address);
        mLayout.removeView(icon);
        selectedAddresses.remove(icon.getAddress());
        EventBus.getDefault().post(new BLEIconNumSelectedChangedEvent(selectedAddresses.size()));
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

    public void setInitialized(Map<String, Boolean> isInitialized) {
        for(String address : isInitialized.keySet())
            setInitialized(address, isInitialized.get(address));
    }

    public void setInitialized(String address, boolean isInitialized) {
        icons.get(address).setIsInitialized(isInitialized);
    }

    private void setIconLocation(BLEScanIcon newIcon) {
        double degrees = Math.toRadians(180 - DEGREES_BETWEEN_DEVICES * icons.size());
        int xcenter = mLayout.getResources().getDisplayMetrics().widthPixels / 2;
        int ycenter = mLayout.getResources().getDisplayMetrics().heightPixels / 2;
        float radius = RADIUS_FROM_CENTER;
        if(icons.size() % 2 == 1)
            radius *= 1.5;

        int xpos = (int) (Math.cos(degrees) * convertDpToPixel(radius, context) + xcenter - newIcon.getWidth()/2);
        int ypos = (int) (-Math.sin(degrees) * convertDpToPixel(radius, context) + ycenter - newIcon.getHeight()/2);

        Log.d("", "width " + newIcon.getWidth() + " height " + newIcon.getHeight());

        newIcon.setX(xpos);
        newIcon.setY(ypos);

        icons.put(newIcon.getAddress(), newIcon);
        newIcon.setVisibility(View.VISIBLE);
        newIcon.fadeIn();
    }
}
