package tan.philip.nrf_ble.ScanScreen;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.greenrobot.eventbus.EventBus;

import tan.philip.nrf_ble.Events.Connecting.BLEIconSelectedEvent;
import tan.philip.nrf_ble.Events.PlotDataEvent;
import tan.philip.nrf_ble.R;

public class BLEScanIcon extends LinearLayout {
    private static final int FADE_IN_TIME_MS = 750;

    private int mImageResource;
    private String mName;
    private String mAddress;
    private int mRSSI;

    private ImageButton mButton;
    private ImageView mHighlight;
    private TextView mNameView;
    private TextView mAddressView;
    private TextView mRSSIView;

    private boolean mSelected = false;

    private final ValueAnimator mAlphaAnimator = ValueAnimator.ofFloat(0, 1);;

    public BLEScanIcon(Context ctx, String name, String address, int rssi, int imageResource) {
        super(ctx);
        this.mName = name;
        this.mAddress = address;
        this.mRSSI = rssi;
        this.mImageResource = imageResource;

        initializeViews(ctx);
    }

    public BLEScanIcon(Context ctx, AttributeSet attrs, String name, String address, int rssi, int imageResource) {
        super(ctx, attrs);
        new BLEScanIcon(ctx, name, address, rssi, imageResource);
    }

    public BLEScanIcon(Context ctx, AttributeSet attrs, int defStyleAttr, String name, String address, int rssi, int imageResource) {
        super(ctx, attrs, defStyleAttr);
        new BLEScanIcon(ctx, name, address, rssi, imageResource);
    }

    /**
     * Inflates the views in the layout.
     *
     * @param context
     *           the current context for the view.
     */
    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.ble_scan_icon, this);

        mButton = (ImageButton)this.findViewById(R.id.ble_scan_icon_button);
        mHighlight = (ImageView)this.findViewById(R.id.ble_scan_icon_highlight);
        mNameView = (TextView)this.findViewById(R.id.ble_scan_icon_name);
        mAddressView = (TextView)this.findViewById(R.id.ble_scan_icon_address);
        mRSSIView = (TextView)this.findViewById(R.id.ble_scan_icon_rssi);
        mHighlight.setVisibility(View.GONE);

        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSelected = !mSelected;

                EventBus.getDefault().post(new BLEIconSelectedEvent(mAddress, mSelected));
                if(mSelected)
                    mHighlight.setVisibility(View.VISIBLE);
                else
                    mHighlight.setVisibility(View.GONE);
            }
        });

        mNameView.setText(mName);
        mAddressView.setText(mAddress);
        mRSSIView.setText(mRSSI + " dBm");

        mAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mButton.setAlpha((float)mAlphaAnimator.getAnimatedValue());
                mNameView.setAlpha((float)mAlphaAnimator.getAnimatedValue());
                mAddressView.setAlpha((float)mAlphaAnimator.getAnimatedValue());
                mRSSIView.setAlpha((float)mAlphaAnimator.getAnimatedValue());
                mHighlight.setAlpha((float)mAlphaAnimator.getAnimatedValue());
            }
        });

        mAlphaAnimator.setDuration(FADE_IN_TIME_MS);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setRSSI(int rssi) {
        mRSSI = rssi;
        mRSSIView.setText(mRSSI + " dBm");
    }

    public String getAddress() {
        return mAddress;
    }

    public void fadeIn() {
        mAlphaAnimator.start();
    }

    public void fadeOut() {
        mAlphaAnimator.reverse();
    }


}
