package com.yen.androidappthesisyen.simplerecognizer;


public abstract class PebbleGestureModel {


    public static final int G = 1000, MODE_FLICK = 0, MODE_TILT = 1;

    //State - { LEFT, RIGHT, UP, DOWN }
    private boolean[] triggers = {false, false, false, false};
    private int mode = 0;
    private boolean actionYetToEnd = false;


    private int accelThreshold;
    private long duration, lastActionTime;

    // We reduce the accelThreshold for the right, up and down tilt gesture, since it's harder to recognize otherwise.
    // This implies the Pebble™ wearer is right handed and hence wears the device on the left wrist.
    private final double REDUCTION_FACTOR = 0.75;

    /**
     * Object that manages Pebble wrist movement gestures.
     * The user should extend their wrist with the fist pointing directly out from the chest as if to punch, with the watch's face pointing upwards
     *
     * @param accelThreshold            Value from 0 to 4000 above which an action on an axis will be triggered
     * @param durationMilliseconds Minimum time between gestures in milliseconds
     * @param modeConstant         Mode constant from this class for FLICK or TILT operation
     */
    public PebbleGestureModel(int accelThreshold, long durationMilliseconds, int modeConstant) {
        this.accelThreshold = accelThreshold;
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
    public Boolean[] update(int[] accelData) {

        // Get time
        long now = System.currentTimeMillis();

        // Check min duration elapsed
        if (now - lastActionTime > duration) {

            /*
            switch (mode) {

                case MODE_TILT:
                */

                    // Check left
                    if (!triggers[0] && accelData[1] > accelThreshold) {
                        triggers[0] = true;
                        onWristLeft();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[0] = false;
                    }


                    // Check right
                    if (!triggers[1] && accelData[1] < (-1 * accelThreshold * REDUCTION_FACTOR)) {
                        triggers[1] = true;
                        onWristRight();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[1] = false;
                    }


                    // Check up
                    if (!triggers[2] && accelData[0] < (-1 * accelThreshold * REDUCTION_FACTOR)) {
                        triggers[2] = true;
                        onWristUp();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[2] = false;
                    }


                    // Check down
                    if (!triggers[3] && accelData[0] > accelThreshold * REDUCTION_FACTOR) {
                        triggers[3] = true;
                        onWristDown();
                        lastActionTime = now;
                        actionYetToEnd = true;
                        Boolean[] booleanArray = {true, false};
                        return booleanArray;
                    } else {
                        triggers[3] = false;
                    }

            /*
                    break;
            }
            */

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
