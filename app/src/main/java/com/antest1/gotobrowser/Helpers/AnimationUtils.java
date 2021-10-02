package com.antest1.gotobrowser.Helpers;

import android.view.ViewGroup;

import androidx.transition.AutoTransition;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

public class AnimationUtils {
    public static final int DURATION = 200;

    public static void beginAuto(ViewGroup viewGroup) {
        AutoTransition transition = new AutoTransition();
        transition.setDuration(DURATION);
        transition.setOrdering(TransitionSet.ORDERING_TOGETHER);
        TransitionManager.beginDelayedTransition(viewGroup, transition);
    }

    public static void beginFade(ViewGroup viewGroup) {
        Fade transition = new Fade();
        transition.setDuration(DURATION);
        TransitionManager.beginDelayedTransition(viewGroup, transition);
    }

    public static void beginChangeBounds(ViewGroup viewGroup) {
        ChangeBounds transition = new ChangeBounds();
        transition.setDuration(DURATION);
        TransitionManager.beginDelayedTransition(viewGroup, transition);
    }
}
