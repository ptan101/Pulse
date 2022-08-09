package tan.philip.nrf_ble.ScanScreen;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;

public class Pulse {
    private static final int TIME_BETWEEN_PULSES = 1000;
    private static final int PULSE_LIFETIME = 4000;

    private static float scale;

    private final AnimatorSet animatorSet;
    private final ValueAnimator sizeAnimator;
    private final ValueAnimator alphaAnimator;
    private final ImageView imageView;
    private final int position;
    private final Vibrator vibrator;


    public Pulse(ImageView image, int position, Vibrator v) {

        vibrator = v;
        imageView = image;
        imageView.setVisibility(View.GONE);
        this.position = position;

        animatorSet = new AnimatorSet();

        sizeAnimator = ValueAnimator.ofInt(108, 700);
        alphaAnimator = ValueAnimator.ofFloat(1, 0);

        sizeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);

        sizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                changeImageViewDimensions((int)sizeAnimator.getAnimatedValue(), (int)sizeAnimator.getAnimatedValue());
                imageView.setAlpha((float)alphaAnimator.getAnimatedValue());
            }
        });

        sizeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                ValueAnimator vAnim = (ValueAnimator) animation;

                if(imageView.getVisibility() == View.VISIBLE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(100, 1));
                    } else {
                        //deprecated in API 26
                        v.vibrate(100);
                    }
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                ValueAnimator vAnim = (ValueAnimator) animation;

                if(imageView.getVisibility() == View.VISIBLE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(100, 1));
                    } else {
                        //deprecated in API 26
                        v.vibrate(100);
                    }
                }
            }
        });

        sizeAnimator.setDuration(PULSE_LIFETIME);
        alphaAnimator.setDuration(PULSE_LIFETIME);

        animatorSet.play(alphaAnimator).with(sizeAnimator);

    }

    public void restart() {
        animatorSet.end();

        imageView.setVisibility(View.VISIBLE);
        sizeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        animatorSet.setStartDelay(TIME_BETWEEN_PULSES * position);

        animatorSet.start();
    }

    public void pause() {
        animatorSet.pause();
    }

    public void resume() {
        animatorSet.resume();
    }

    public void end() {
        long playTime = animatorSet.getCurrentPlayTime();
        int timesRepeated = (int) (playTime / PULSE_LIFETIME);

        sizeAnimator.setRepeatCount(timesRepeated);
        alphaAnimator.setRepeatCount(timesRepeated);

        if(sizeAnimator.getAnimatedFraction() == 0) {
            sizeAnimator.end();
            alphaAnimator.end();
            imageView.setVisibility(View.GONE);
        }
    }

    public static void setDPScale(float DPscale) {
        scale = DPscale;
    }

    private void changeImageViewDimensions(float width, float height) {
        imageView.getLayoutParams().height = Math.round(height * scale);//(int) (width * scale + 0.5f);
        imageView.getLayoutParams().width = Math.round(width * scale);//(int) (height* scale + 0.5f);
        imageView.requestLayout();
    }
}