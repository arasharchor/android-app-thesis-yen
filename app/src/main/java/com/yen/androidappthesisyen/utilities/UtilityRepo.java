package com.yen.androidappthesisyen.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Yen on 15/08/2015.
 */
public class UtilityRepo {


    private static final String LOG_TAG = UtilityRepo.class.getName();


//    public static void enableAccelStream(Context theContext, String systemID) {
//
//        String previousList = getEnabledAccelStreamDevices(theContext);
//        Log.w(LOG_TAG, "previousList " + previousList);
//
//        addNewAccelStreamState(theContext, systemID, "enable"); // List has now been updated.
//
//        if (systemID.equalsIgnoreCase("triggered-by-user") || previousList.equalsIgnoreCase("") || previousList.equalsIgnoreCase(";")) {
//            // The previous list was empty. This means we deliberately need to send a signal to start the accel stream.
//
//            PebbleDictionary dict = new PebbleDictionary();
//            dict.addInt32(1, 0); // key = 1 = TRUE = start stream, value = 0
//            PebbleKit.sendDataToPebble(theContext, UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);
//
//        } else {
//            // The previous list was NOT empty. This means we don't need to send the signal to start the stream, since it's already running.
//        }
//    }

    public static Map<String, String> getMapSupportedGestures(Context theContext) {

        Map<String, String> outputMap = new HashMap<String, String>();

        SharedPreferences pSharedPref = theContext.getSharedPreferences("com.yen.androidappthesisyen.system_id_to_supported_gestures", Context.MODE_PRIVATE);

        try {
            if (pSharedPref != null) {
                String jsonString = pSharedPref.getString("my_map", (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    String value = (String) jsonObject.get(key); // a value = comma separated list of supported gestures for the specific systemID
                    outputMap.put(key, value);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputMap;
    }

    public static String getEnabledAccelStreamDevices(Context theContext) {
        SharedPreferences enumSetting = theContext.getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        String enabledList = enumSetting.getString("enabledaccelstreamdevices", "");
        return enabledList;
    }

    // KEY = "accelstreamenabled" - VALUE = comma separated list of systemIDs where stream is currently enabled.
    public static void addNewAccelStreamState(Context theContext, String systemID, String stateRequest) {


        String concatenatedListEnabledActionDevices = getEnabledAccelStreamDevices(theContext);

        String newConcatenatedString = "";


        if (stateRequest.equalsIgnoreCase("enable")) {


            if (concatenatedListEnabledActionDevices != null && !concatenatedListEnabledActionDevices.equalsIgnoreCase("") && !concatenatedListEnabledActionDevices.equalsIgnoreCase(";")) {

                String[] arrayEnabledActionDevices = concatenatedListEnabledActionDevices.split(";");
                Set<String> setEnabledActionDevices = new HashSet<String>(Arrays.asList(arrayEnabledActionDevices));
                // adding new action device systemID
                setEnabledActionDevices.add(systemID);
                // recreate concatenated string from new set
                newConcatenatedString = TextUtils.join(";", setEnabledActionDevices);

                Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);

            } else {

                newConcatenatedString = systemID;

                Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);
            }


        } else if (stateRequest.equalsIgnoreCase("disable")) {


            if (concatenatedListEnabledActionDevices != null && !concatenatedListEnabledActionDevices.equalsIgnoreCase("") && !concatenatedListEnabledActionDevices.equalsIgnoreCase(";")) {

                String[] arrayEnabledActionDevices = concatenatedListEnabledActionDevices.split(";");
                Set<String> setEnabledActionDevices = new HashSet<String>(Arrays.asList(arrayEnabledActionDevices));
                // removing action device systemID
                setEnabledActionDevices.remove(systemID);
                // recreate concatenated string from new set
                newConcatenatedString = TextUtils.join(";", setEnabledActionDevices);

                Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);

            } else {

                // Do nothing!

            }


        } else {
            Log.w(LOG_TAG, "Wrong accel stream state request: not 'enable' or 'disable'");
        }


        SharedPreferences pSharedPref = theContext.getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("enabledaccelstreamdevices").commit();
            editor.putString("enabledaccelstreamdevices", newConcatenatedString);
            editor.commit();
        }


    }


    public static List<String> getListSavedSystemIDs(Context context) {

        List<String> listSystemIDs = new ArrayList<>();

        // OUD getActivity().getSharedPreferences
        SharedPreferences pSharedPref = context.getSharedPreferences("com.yen.androidappthesisyen.system_id_to_supported_gestures", Context.MODE_PRIVATE);

        try {
            if (pSharedPref != null) {
                String jsonString = pSharedPref.getString("my_map", (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
//                    String value = (String) jsonObject.get(key); // a value = comma separated list of supported gestures for the specific systemID
//                    outputMap.put(key, value);
                    listSystemIDs.add(key);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return listSystemIDs;
    }


    public static List<String> getListSystemIDsToConnectTo(Context context) {

        List<String> theList = new ArrayList<>();

        SharedPreferences sharedPref = context.getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        String resultString = sharedPref.getString("connectedactiondevices", "");
        if (resultString != null && !resultString.equalsIgnoreCase("") && !resultString.equalsIgnoreCase(";")) {
            String[] arrayConnectedActionDevices = resultString.split(";");
            theList = new ArrayList<>(Arrays.asList(arrayConnectedActionDevices));
        }

        return theList;
    }


    public static void addSystemIDToListSystemIDsToConnectTo(Context context, String systemID) {

        List<String> theList = getListSystemIDsToConnectTo(context);

        String newConcatenatedString = "";


        if (theList != null) {

            Set<String> setStructure = new HashSet<>(theList);

            // adding new action device systemID
            setStructure.add(systemID);
            // recreate concatenated string from new set
            newConcatenatedString = TextUtils.join(";", setStructure);

            Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);

        } else {

            theList.add(systemID);
            newConcatenatedString = TextUtils.join(";", theList);

            Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);
        }


        SharedPreferences pSharedPref = context.getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("connectedactiondevices").commit();
            editor.putString("connectedactiondevices", newConcatenatedString);
            editor.commit();
        }


    }


    public static void removeSystemIDFromListSystemIDsToConnectTo(Context context, String systemID) {

        List<String> theList = getListSystemIDsToConnectTo(context);

        String newConcatenatedString = "";

        if (theList != null) {

            Set<String> setStructure = new HashSet<>(theList);

            // removing action device systemID
            setStructure.remove(systemID);
            // recreate concatenated string from new set
            newConcatenatedString = TextUtils.join(";", setStructure);

            Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);

        } else {
            // Do nothing.
        }


        SharedPreferences pSharedPref = context.getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("connectedactiondevices").commit();
            editor.putString("connectedactiondevices", newConcatenatedString);
            editor.commit();
        }

    }


}
