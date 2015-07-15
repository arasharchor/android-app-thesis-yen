package com.yen.androidappthesisyen.simplerecognizer;

import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.advancedrecognizer.AdvancedFragment;

/**
 * Created by Yen on 4/07/2015.
 */


public class TiltGestureRecognizer extends PebbleGestureModel {


    private static final String LOG_TAG = "SIMPLE TILT RECOGNIZER";

    private AdvancedFragment theAdvancedFragment = null;

    /**
     * Object that manages Pebble wrist movement gestures.
     * The user should extend their wrist with the fist pointing directly out from the chest as if to punch, with the watch's face pointing upwards
     *
     * @param threshold            Value from 0 to 4000 above which an action on an axis will be triggered
     * @param durationMilliseconds Minimum time between gestures in milliseconds
     * @param modeConstant         Mode constant from this class for FLICK or TILT operation
     */
    public TiltGestureRecognizer(AdvancedFragment theAdvancedFragment, int threshold, long durationMilliseconds, int modeConstant) {
        super(threshold, durationMilliseconds, modeConstant);

        this.theAdvancedFragment = theAdvancedFragment;
        if(this.theAdvancedFragment == null){
            Log.w(LOG_TAG, "IS NOG NULL");
        }

    }

    @Override
    public void onWristLeft() {

        // Although the following line of code gets duplicated int the following methods, we can't initialize it once in the constructor, since at that time, the View hasn't yet been fully initialized.
        TextView gestureWindow = (TextView) theAdvancedFragment.getView().findViewById(R.id.textView_gestures);
        gestureWindow.append("wrist LEFT <--" + "\n");
        ((ScrollView) theAdvancedFragment.getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);

        theAdvancedFragment.sendGestureIfMatchFound("left");

        Log.w(LOG_TAG, "wrist LEFT <--");
    }

    @Override
    public void onWristRight() {

        TextView gestureWindow = (TextView) theAdvancedFragment.getView().findViewById(R.id.textView_gestures);
        gestureWindow.append("wrist RIGHT -->" + "\n");
        ((ScrollView) theAdvancedFragment.getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);

        theAdvancedFragment.sendGestureIfMatchFound("right");

        Log.w(LOG_TAG, "wrist RIGHT -->");
    }

    @Override
    public void onWristUp() {

        TextView gestureWindow = (TextView) theAdvancedFragment.getView().findViewById(R.id.textView_gestures);
        gestureWindow.append("wrist UP ^^" + "\n");
        ((ScrollView) theAdvancedFragment.getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);

        theAdvancedFragment.sendGestureIfMatchFound("up");

        Log.w(LOG_TAG, "wrist UP ^^");
    }

    @Override
    public void onWristDown() {

        TextView gestureWindow = (TextView) theAdvancedFragment.getView().findViewById(R.id.textView_gestures);
        gestureWindow.append("wrist DOWN __" + "\n");
        ((ScrollView) theAdvancedFragment.getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);

        theAdvancedFragment.sendGestureIfMatchFound("down");

        Log.w(LOG_TAG, "wrist DOWN __");
    }


    /**
     * When an action ends - fired after duration
     */
    @Override
    public void onActionEnd() {
        // TODO
        Log.w(LOG_TAG, "onActionEnd()");
    }

}
