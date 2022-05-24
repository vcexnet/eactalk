package com.eacpay.tools.animation;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

public class DecelerateOvershootInterpolator implements Interpolator {
    private final DecelerateInterpolator accelerate;
    private final OvershootInterpolator overshoot;

    public DecelerateOvershootInterpolator(float factor, float tension) {
        accelerate = new DecelerateInterpolator(factor);
        overshoot = new OvershootInterpolator(tension);
    }

    @Override
    public float getInterpolation(float input) {
        return overshoot.getInterpolation(accelerate.getInterpolation(input));
    }

}