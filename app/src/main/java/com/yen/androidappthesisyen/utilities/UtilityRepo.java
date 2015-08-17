package com.yen.androidappthesisyen.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Yen on 15/08/2015.
 */
public class UtilityRepo {


    private static final String LOG_TAG = UtilityRepo.class.getName();


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

            // TODO check dat geen problemen geeft als de lijst nu leeg is, maar wrsl geen probs.
            Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);

        } else {

            // Do nothing!

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
