package com.yen.androidappthesisyen.utilities;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Yen on 15/08/2015.
 */
public class UtilityRepo {


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


}
