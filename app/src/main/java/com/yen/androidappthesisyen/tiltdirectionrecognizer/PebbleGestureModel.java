package com.yen.androidappthesisyen.tiltdirectionrecognizer;

/**
 * SOURCE: Partially based on https://gist.github.com/C-D-Lewis/ba1349bb0ebdee76b0cf#file-pebblegesturemodel-java
 */


public abstract class PebbleGestureModel {


    //Constants
    public static final int
            G = 1000,
            MODE_FLICK = 0,
            MODE_TILT = 1;

    //State - { LEFT, RIGHT, UP, DOWN }
    private boolean[] triggers = {false, false, false, false};
    private int mode = 0;
    private boolean actionYetToEnd = false;

    //Object data state
    private int threshold;
    private long
            duration,
            lastActionTime;

    /**
     * Object that manages Pebble wrist movement gestures.
     * The user should extend their wrist with the fist pointing directly out from the chest as if to punch, with the watch's face pointing upwards
     *
     * @param threshold            Value from 0 to 4000 above which an action on an axis will be triggered
     * @param durationMilliseconds Minimum time between gestures in milliseconds
     * @param modeConstant         Mode constant from this class for FLICK or TILT operation
     */
    public PebbleGestureModel(int threshold, long durationMilliseconds, int modeConstant) {
        this.threshold = threshold;
        this.duration = durationMilliseconds;
        mode = modeConstant;
    }

    /**
     * Update the internal model and check to trigger events
     *
     * @param packet Packet of most recent data to use
     */
    // boolean array of 2 booleans
    // first one is TRUE if a gesture was recognized, so a direction (left/right/up/down) was recognized.
    // second one is TRUE when the recognized gesture has ended.
    // only when BOTH are TRUE do with start "from zero" = checking for a new gesture, either via the simple recognizer (here) or the advanced recognizer.
    public Boolean[] update(int[] intArray) {

        //Get time
        long now = System.currentTimeMillis();

        //Check min duration elapsed
        if (now - lastActionTime > duration) {
            switch (mode) {
                case MODE_FLICK:
                    //Check left
                    if (!triggers[0] && intArray[1] < (-1 * threshold)) {
                        triggers[0] = true;
                        onWristLeft();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[0] = false;
                    }

                    //Check right
                    if (!triggers[1] && intArray[1] > threshold) {
                        triggers[1] = true;
                        onWristRight();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[1] = false;
                    }

                    //Check up
                    if (!triggers[2] && intArray[2] < ((-1 * G) - threshold)) {
                        triggers[2] = true;
                        onWristUp();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[2] = false;
                    }

                    //Check down
                    if (!triggers[3] && intArray[2] > ((-1 * G) + threshold)) {
                        triggers[3] = true;
                        onWristDown();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[3] = false;
                    }

                    break;


                case MODE_TILT:
                    //Check left
                    if (!triggers[0] && intArray[1] > threshold) {
                        triggers[0] = true;
                        onWristLeft();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[0] = false;
                    }

                    //Check right
                    if (!triggers[1] && intArray[1] < (-1 * threshold)) {
                        triggers[1] = true;
                        onWristRight();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[1] = false;
                    }

                    //Check up
                    if (!triggers[2] && intArray[0] < (-1 * threshold)) {
                        triggers[2] = true;
                        onWristUp();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[2] = false;
                    }

                    //Check down
                    if (!triggers[3] && intArray[0] > threshold) {
                        triggers[3] = true;
                        onWristDown();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[3] = false;
                    }

                    break;
            }

            if (actionYetToEnd) {
                actionYetToEnd = false;
                onActionEnd();

                Boolean[] booleanArray = {true, true};
                return booleanArray;
            }
        }

        // We only arrive here if the above IF statements were never true.
        // This means no wrist of tilt gesture was detected.
        Boolean[] booleanArray = {false, false};
        return booleanArray;

    }

    /**
     * Set the trigger mode
     *
     * @param newModeConstant Constant from this class
     */
    public void setMode(int newModeConstant) {
        mode = newModeConstant;
    }

    /**
     * When a leftward movement is detected
     */
    public abstract void onWristLeft();

    /**
     * When a rightward movement is detected
     */
    public abstract void onWristRight();

    /**
     * When a upward movement is detected
     */
    public abstract void onWristUp();

    /**
     * When a downward movement is detected
     */
    public abstract void onWristDown();

    /**
     * When an action ends - fired after duration
     */
    public abstract void onActionEnd();


}
