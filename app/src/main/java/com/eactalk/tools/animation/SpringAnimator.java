package com.eactalk.tools.animation;

import android.app.Activity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.eactalk.R;

public class SpringAnimator {
    private static final String TAG = SpringAnimator.class.getName();

    public static void showExpandCameraGuide(final View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        ScaleAnimation trans = new ScaleAnimation(0.0f, 1f, 0.0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans.setDuration(800);
        trans.setInterpolator(new DecelerateOvershootInterpolator(1.5f, 2.5f));
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            view.startAnimation(trans);
        }

    }

    /**
     * Shows the springy animation on views
     */
    public static void springView(final View view) {
        if (view == null) return;
        ScaleAnimation trans = new ScaleAnimation(0.8f, 1f, 0.8f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans.setDuration(1000);
        trans.setInterpolator(new DecelerateOvershootInterpolator(0.5f, 1f));
        view.setVisibility(View.VISIBLE);
        view.startAnimation(trans);

    }
    /**
     * Shows the springy animation on views
     */
    public static void shortSpringView(final View view) {
        if (view == null) return;
        ScaleAnimation trans = new ScaleAnimation(0.9f, 1f, 0.9f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans.setDuration(200);
        trans.setInterpolator(new DecelerateOvershootInterpolator(1.3f, 1.4f));
        view.setVisibility(View.VISIBLE);
        view.startAnimation(trans);

    }

    /**
     * Shows the springy bubble animation on views
     */
    public static void showBubbleAnimation(final View view) {
        if (view == null) return;
        ScaleAnimation trans = new ScaleAnimation(0.75f, 1f, 0.75f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans.setDuration(300);
        trans.setInterpolator(new DecelerateOvershootInterpolator(1.0f, 1.85f));
        view.setVisibility(View.VISIBLE);
        view.startAnimation(trans);
    }

    public static void failShakeAnimation(Activity context, View view) {
        if (view == null) return;
        Animation shake = AnimationUtils.loadAnimation(context, R.anim.shake);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(shake);
    }

    public static void donationFailShakeAnimation(Activity context, TextView view) {
        if (view == null) return;
        Animation shake = AnimationUtils.loadAnimation(context, R.anim.shake);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(shake);
    }

}