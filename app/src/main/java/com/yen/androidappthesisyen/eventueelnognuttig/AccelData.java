package com.yen.androidappthesisyen.eventueelnognuttig;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * SOURCE: https://github.com/kramimus/pebble-accel-analyzer
 * GNU GENERAL PUBLIC LICENSE: https://github.com/kramimus/pebble-accel-analyzer/blob/master/LICENSE
 */


public class AccelData {

    private static final String TAG = AccelData.class.getSimpleName();

    final private int x;
    final private int y;
    final private int z;

    private long timestamp = 0;
    final private boolean didVibrate;

    public AccelData(byte[] data) {
        // Convert byte array sent from Pebble back to AccelData on Android
        x = (data[0] & 0xff) | (data[1] << 8);
        y = (data[2] & 0xff) | (data[3] << 8);
        z = (data[4] & 0xff) | (data[5] << 8);
        // data waarbij vibratie werkte, moeten we negeren want die mogen we niet interpreteren!
        didVibrate = data[6] != 0;

        for (int i = 0; i < 8; i++) {
            timestamp |= ((long) (data[i + 7] & 0xff)) << (i * 8);
        }
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("x", x);
            json.put("y", y);
            json.put("z", z);
            json.put("ts", timestamp);
            json.put("v", didVibrate);
            return json;
        } catch (JSONException e) {
            Log.w(TAG, "Problem constructing accel data, skipping " + e);
        }
        return null;
    }


    // Added by Yen.
    public String getOneLineString() {
        return new String("X " + x + " Y " + y + " Z " + z);
    }

    public static List<AccelData> fromDataArray(byte[] data) {
        List<AccelData> accels = new ArrayList<AccelData>();

        // TODO ze veronderstellen hier dat elke DATA UNIT 15 BYTES groot zal zijn?
        // moeten dit aanpassen op wat wij concreet gebruiken of?
        for (int i = 0; i < data.length; i += 15) {
            accels.add(new AccelData(Arrays.copyOfRange(data, i, i + 15)));
        }
        return accels;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void applyTimezone(TimeZone tz) {
        timestamp -= tz.getOffset(timestamp);
    }
}



