package tan.philip.nrf_ble.ScanScreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.animation.ValueAnimator;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import tan.philip.nrf_ble.R;
import tan.philip.nrf_ble.ScanListScreen.ScanResultsActivity;
import tan.philip.nrf_ble.databinding.ActivityClientBinding;

import static tan.philip.nrf_ble.Constants.SCAN_PERIOD;

public class TestUI extends AppCompatActivity {

    private ActivityClientBinding mBinding;

    private boolean mScanning = false;

    private final int GREY = 0xFF2C2C2C;
    private ValueAnimator valueAnimator;

    private ArrayList<Pulse> pulses;

    public static final String EXTRA_BT_SCAN_RESULTS = "scan results";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_client);
        mBinding.btnListDevices.setVisibility(View.GONE);

        //Set up background color transition
        mBinding.layout1.setBackgroundColor(GREY);
        setupBackgroundTransition();

        pulses = new ArrayList<>();
        Pulse.setDPScale(getApplicationContext().getResources().getDisplayMetrics().density);


        for(int i = 0; i < 4; i ++) {
            Pulse newPulse;

            switch (i) {
                case 0: newPulse = new Pulse(mBinding.pulse1, i, (Vibrator) getSystemService(Context.VIBRATOR_SERVICE)); break;
                case 1: newPulse = new Pulse(mBinding.pulse2, i, (Vibrator) getSystemService(Context.VIBRATOR_SERVICE)); break;
                case 2: newPulse = new Pulse(mBinding.pulse3, i, (Vibrator) getSystemService(Context.VIBRATOR_SERVICE)); break;
                default: newPulse = new Pulse(mBinding.pulse4, i, (Vibrator) getSystemService(Context.VIBRATOR_SERVICE));
            }
            pulses.add(newPulse);
        }


        mBinding.btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mScanning) {
                    startScan();
                } else {

                    Toast toast = Toast.makeText(getApplicationContext(), "Scan stopped", Toast.LENGTH_SHORT);
                    toast.show();

                    stopScan();

                    //Reset background color
                    //mBinding.layout1.setBackgroundColor(GREY);

                }
            }
        });

        mBinding.btnListDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewScanList(view);
            }
        });
    }

    private void startScan() {
        mBinding.btnListDevices.setVisibility(View.GONE);
        mScanning = true;

        //Display message
        Toast toast = Toast.makeText(getApplicationContext(), "Scanning for BLE devices...", Toast.LENGTH_SHORT);
        toast.show();

        //Change background color
        valueAnimator.start();
        for(Pulse currentPulse: pulses) {
            currentPulse.restart();
        }

    }

    private void stopScan() {
        valueAnimator.cancel();
        for(Pulse pulse: pulses) {
            pulse.end();
        }

        mScanning = false;
    }

    private void setupBackgroundTransition() {
        final float[] from = new float[3],
                to =   new float[3];

        //Color.colorToHSV(Color.parseColor("#FF2c2a3a"), from);   // from blue
        Color.colorToHSV(Color.parseColor("#FF2C2C2C"), from);
        Color.colorToHSV(Color.parseColor("#FFFFFFFF"), to);     // to white

        valueAnimator = ValueAnimator.ofFloat(0, 1);                  // animate from 0 to 1
        valueAnimator.setDuration(SCAN_PERIOD-1500);

        final float[] hsv  = new float[3];                  // transition color
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                // Transition along each axis of HSV (hue, saturation, value)
                hsv[0] = from[0] + (to[0] - from[0])*animation.getAnimatedFraction();
                hsv[1] = from[1] + (to[1] - from[1])*animation.getAnimatedFraction();
                hsv[2] = from[2] + (to[2] - from[2])*animation.getAnimatedFraction();

                mBinding.layout1.setBackgroundColor(Color.HSVToColor(hsv));
            }
        });
    }

    public void viewScanList(View view) {
        Intent intent = new Intent(this, ScanResultsActivity.class);
        intent.putExtra(EXTRA_BT_SCAN_RESULTS, new HashMap<String, BluetoothDevice>());
        startActivity(intent);
    }
}