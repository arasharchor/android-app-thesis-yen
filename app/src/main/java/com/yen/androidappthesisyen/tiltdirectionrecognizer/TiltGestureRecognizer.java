package com.yen.androidappthesisyen.tiltdirectionrecognizer;

import android.util.Log;

/**
 * Created by Yen on 4/07/2015.
 */


public class TiltGestureRecognizer extends PebbleGestureModel {


    private static final String LOG_TAG = "SIMPLE TILT RECOGNIZER";

    /**
     * Object that manages Pebble wrist movement gestures.
     * The user should extend their wrist with the fist pointing directly out from the chest as if to punch, with the watch's face pointing upwards
     *
     * @param threshold            Value from 0 to 4000 above which an action on an axis will be triggered
     * @param durationMilliseconds Minimum time between gestures in milliseconds
     * @param modeConstant         Mode constant from this class for FLICK or TILT operation
     */
    public TiltGestureRecognizer(int threshold, long durationMilliseconds, int modeConstant) {
        super(threshold, durationMilliseconds, modeConstant);
    }

    @Override
    public void onWristLeft() {
        // TODO
        Log.w(LOG_TAG, "wrist LEFT <--");
    }

    @Override
    public void onWristRight() {
        // TODO
        Log.w(LOG_TAG, "wrist RIGHT -->");
    }

    @Override
    public void onWristUp() {
        // TODO
        Log.w(LOG_TAG, "wrist UP ^^");
    }

    @Override
    public void onWristDown() {
        // TODO
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
