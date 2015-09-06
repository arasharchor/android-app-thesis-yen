package com.yen.androidappthesisyen.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.getpebble.android.kit.PebbleKit;
import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.pushnotificationlistener.MQTTService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

import static com.yen.androidappthesisyen.utilities.UtilityRepo.addSystemIDToListSystemIDsToConnectTo;
import static com.yen.androidappthesisyen.utilities.UtilityRepo.getListSavedSystemIDs;
import static com.yen.androidappthesisyen.utilities.UtilityRepo.getListSystemIDsToConnectTo;
import static com.yen.androidappthesisyen.utilities.UtilityRepo.removeSystemIDFromListSystemIDsToConnectTo;



public class MainFragment extends Fragment implements View.OnClickListener {



    /*Use Static Final For Constants
    * See https://developer.android.com/training/articles/perf-tips.html */
    private static final String LOG_TAG = MainFragment.class.getName();

    public static final UUID WATCHAPP_UUID = UUID.fromString("7c5167e8-9df4-479f-9353-714481681af1");


    // Pebble communication test
    private PebbleKit.PebbleDataReceiver myPebbleDataReceiver;
    private static final int KEY_BUTTON_EVENT = 2,
            BUTTON_EVENT_UP = 3,
            BUTTON_EVENT_DOWN = 4,
            BUTTON_EVENT_SELECT = 5,
            KEY_VIBRATION = 6;


    // Pebble accel data logging
    private static final int DATA_LOG_ACCEL_DATA_TAG = 42;
    private PebbleKit.PebbleDataLogReceiver myPebbleDataLOGReceiver;
    private StringBuilder resultBuilder = new StringBuilder();
    private final Handler handler = new Handler();


    private static boolean areDefaultsInserted = false;

    private static Bundle bundleLabelStates = new Bundle();
    private static Bundle bundleEnableDisableStates = new Bundle();
    private static Bundle bundleToggleStates = new Bundle();

    private BluetoothAdapter BTadapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_ENABLE_BT = 50;
    private static final int REQUEST_BT_DISCOVERABLE = 51;
    private Set<BluetoothDevice> setPairedBTDevices;


    private ArrayAdapter<String> arrayListMainAdapter;
    private List<String> arrayListNamesPairedBTDevices;


    // Intent request code for BT dialog
    private static final int REQUEST_CONNECT_DEVICE = 7;

    private View theView = null;


    public MainFragment() {
        // Required empty public constructor
    }


    // onCreate wasn't displayed by default.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Useful and good practice! See http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
        // Retain this fragment across configuration changes.
        setRetainInstance(true);


        // get a list of paired devices by calling getBondedDevices()
        // List empty if BT is off.
        setPairedBTDevices = BTadapter.getBondedDevices();

        // TODO of gaan we toch globaal landscape afdwingen? (als ja, doe dit via manifest)
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_main, container, false);


        // SAVE THE DEFAULTS THE VERY FIRST TIME THE FRAGMENT GETS MADE. AND ONLY THEN.
        // We could use the second parameter in bundle.getString(...) to define a DEFAULT VALUE, but that means we have to add that 2nd parameter each time we call bundle.getString(...)
        // + it requires API LEVEL >= 12!
        // So we here make sure everything has a value in the bundles.
        if (!areDefaultsInserted) {
            insertDefaultLabelStates(returnedView);
            insertDefaultEnableDisableStates(returnedView);
            insertDefaultToggleStates(returnedView);
            areDefaultsInserted = true;
        }


        // SYSTEEM DAT WE NU TOEPASSEN
        setLabelStatesFromBundle(returnedView);
        setEnableDisableStatesFromBundle(returnedView);
        setToggleStatesFromBundle(returnedView);



        registerButtonAndToggleListeners(returnedView);

        initListViewMain(returnedView);


        // We also place it here, since in case there is already text in the Output Window when arriving there, we immediately scroll.
        // ((ScrollView) returnedView.findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);

        setBTRelatedStates(returnedView);

        Button serviceButton = (Button) returnedView.findViewById(R.id.buttonService);
        serviceButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                TextView textViewActionDevice = (TextView) theView.findViewById(R.id.textView_action_device);
                textViewActionDevice.setText(getResources().getString(R.string.generic_not_connected));
                bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_not_connected));

                setToggleStates(getView(), false);
                setEnableDisableStates(getView(), false);

                Intent svcOld = new Intent(getActivity().getApplicationContext(), MQTTService.class);
                getActivity().stopService(svcOld);

                // Clear the list
                List<String> theList = getListSystemIDsToConnectTo(getActivity());
                for (String systemIDToRemove : theList) {
                    removeSystemIDFromListSystemIDsToConnectTo(getActivity(), systemIDToRemove);
                }

            }
        });

        theView = returnedView;

        return returnedView;
    }


    public void pebbleGotConnected() {
        TextView textViewPebble = (TextView) theView.findViewById(R.id.textView_pebble);
        // Without "getResources()." it also seems to work, but recommended to use it
        textViewPebble.setText(getResources().getString(R.string.pebble_connected));
        textViewPebble.invalidate();
        bundleLabelStates.putString("R.id.textView_pebble", getResources().getString(R.string.pebble_connected));
    }

    public void pebbleGotDisconnected() {
        TextView textViewPebble = (TextView) theView.findViewById(R.id.textView_pebble);
        textViewPebble.setText(getResources().getString(R.string.pebble_not_connected));
        textViewPebble.invalidate();
        bundleLabelStates.putString("R.id.textView_pebble", getResources().getString(R.string.pebble_not_connected));
    }


    private void showIPDialogNoSavedSystemIDs(final int currentEnumInSystemIDList) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Location of Face Detector and Gesture Handler");
        // OUD builder.setMessage("Insert the current IPv4 address for Action Device " + systemID + ". Use previous location (" + savedBrokerIP + ") by leaving the field blank.");
        builder.setMessage("This is a first-time setup since no previously used systemIDs and IP addresses are found. Insert the required information in the pattern 'systemID//IPaddress'. Example: first-laptop//192.168.1.2 \nMake sure to insert the same systemID in the particular Action Device Runtime application and push this information to the hub to confirm the association.");

        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(input);


        builder.setPositiveButton("Insert next IP",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {


                        String value = String.valueOf(input.getText());

                        String[] systemIDandIPaddress = value.split("//");

                        Log.w(LOG_TAG, "================================ first " + systemIDandIPaddress[0] + " last " + systemIDandIPaddress[1]);


                        saveIPIfInserted(systemIDandIPaddress[0], systemIDandIPaddress[1]);



                        addSystemIDToListSystemIDsToConnectTo(getActivity(), systemIDandIPaddress[0]);



                        showIPDialogNoSavedSystemIDs(currentEnumInSystemIDList + 1);



                    }
                });


        builder.setNeutralButton("Done: start service",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        String value = String.valueOf(input.getText());

                        String[] systemIDandIPaddress = value.split("//");

                        Log.w(LOG_TAG, "================================ first " + systemIDandIPaddress[0] + " last " + systemIDandIPaddress[1]);

                        saveIPIfInserted(systemIDandIPaddress[0], systemIDandIPaddress[1]);


                        addSystemIDToListSystemIDsToConnectTo(getActivity(), systemIDandIPaddress[0]);


                        // ======= START

//                        OUD: setLabelStates(getView(), true);
                        // NIEUW: vervangen door meer granulair:
                        TextView textViewActionDevice = (TextView) theView.findViewById(R.id.textView_action_device);
                        textViewActionDevice.setText(getResources().getString(R.string.generic_connected));
                        bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_connected));

                        setEnableDisableStates(getView(), true);
                        setToggleStates(getView(), false);
                        // ======= END

                        int enumForService = currentEnumInSystemIDList + 1;
                        startOrRestartService(enumForService);


                    }
                });

        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        // Do nothing.
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void askNextAction(final int currentEnumInSystemIDList) {


        // SharedPreferences settings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.user_detector", Context.MODE_PRIVATE);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Location of Face Detector and Gesture Handler");
        // OUD builder.setMessage("Insert the current IPv4 address for Action Device " + systemID + ". Use previous location (" + savedBrokerIP + ") by leaving the field blank.");
        builder.setMessage("If you want to add a new Action Device, insert the required information in the pattern 'systemID//IPaddress'. \nExample: first-laptop//192.168.1.2 \nMake sure to insert the same systemID in the particular Action Device Runtime application and to push it to the central hub to confirm the association. \nIf you didn't want to register a new Action Device, leave the field blank and choose 'Done: start service'.");

        final EditText input = new EditText(getActivity());
        // OUD InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(input);


        builder.setPositiveButton("Insert next IP",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {


                        String value = String.valueOf(input.getText());

                        String[] systemIDandIPaddress = value.split("//");

                        Log.w(LOG_TAG, "================================ first " + systemIDandIPaddress[0] + " last " + systemIDandIPaddress[1]);



                        // For our use case, the IP of Face Detector and IP of Gesture Handler are always one and the same.
                        saveIPIfInserted(systemIDandIPaddress[0], systemIDandIPaddress[1]);



                        addSystemIDToListSystemIDsToConnectTo(getActivity(), systemIDandIPaddress[0]);



                        askNextAction(currentEnumInSystemIDList + 1);



                    }
                });


        builder.setNeutralButton("Done: start service",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {


                        String value = String.valueOf(input.getText());


                        if(value.length() >= 1){

                            String[] systemIDandIPaddress = value.split("//");

                            Log.w(LOG_TAG, "================================ first " + systemIDandIPaddress[0] + " last " + systemIDandIPaddress[1]);



                            // For our use case, the IP of Face Detector and IP of Gesture Handler are always one and the same.
                            saveIPIfInserted(systemIDandIPaddress[0], systemIDandIPaddress[1]);


                            addSystemIDToListSystemIDsToConnectTo(getActivity(), systemIDandIPaddress[0]);


                            // ======= START

//                        OUD: setLabelStates(getView(), true);
                            // NIEUW: vervangen door meer granulair:
                            TextView textViewActionDevice = (TextView) theView.findViewById(R.id.textView_action_device);
                            textViewActionDevice.setText(getResources().getString(R.string.generic_connected));
                            bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_connected));

                            setEnableDisableStates(getView(), true);
                            setToggleStates(getView(), false);
                            // ======= END


                            int enumForService = currentEnumInSystemIDList + 1;
                            startOrRestartService(enumForService);


                        } else {

                            // The user inserted nothing.


                            // ======= START

//                        OUD: setLabelStates(getView(), true);
                            // NIEUW: vervangen door meer granulair:
                            TextView textViewActionDevice = (TextView) theView.findViewById(R.id.textView_action_device);
                            textViewActionDevice.setText(getResources().getString(R.string.generic_connected));
                            bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_connected));

                            setEnableDisableStates(getView(), true);
                            setToggleStates(getView(), false);
                            // ======= END


                            // One less than the value in the value above. Since the field was empty, we have 1 less item.
                            int enumForService = currentEnumInSystemIDList;
                            startOrRestartService(enumForService);

                        }




                    }
                });

        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        // Do nothing.
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

    }



    private void showIPDialog(final int currentEnumInSystemIDList, final String systemID) {

        SharedPreferences settings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.user_detector", Context.MODE_PRIVATE);

        String preferenceKey = "ip_address_broker_" + systemID;
        String savedBrokerIP = settings.getString(preferenceKey, "None");
        Log.w(LOG_TAG, "systemID is " + systemID + " and IP is " + savedBrokerIP);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Location of Face Detector and Gesture Handler");
        builder.setMessage("Insert the current IPv4 address for Action Device " + systemID + ". Use previous location (" + savedBrokerIP + ") by leaving the field blank.");


        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER);


        builder.setView(input);


        builder.setPositiveButton("Insert next IP",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        String value = String.valueOf(input.getText());


                        // For our use case, the IP of Face Detector and IP of Gesture Handler are always one and the same.
                        saveIPIfInserted(systemID, value);

                        addSystemIDToListSystemIDsToConnectTo(getActivity(), systemID);


                        List<String> listSavedSystemIDs = getListSavedSystemIDs(getActivity());

                        if (listSavedSystemIDs.size() > currentEnumInSystemIDList + 1){
                            showIPDialog(currentEnumInSystemIDList + 1, listSavedSystemIDs.get(currentEnumInSystemIDList + 1));
                        } else {
                            askNextAction(currentEnumInSystemIDList + 1);
                        }

                    }
                });


        builder.setNeutralButton("Done: start service",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        String value = String.valueOf(input.getText());


                        saveIPIfInserted(systemID, value);

                        addSystemIDToListSystemIDsToConnectTo(getActivity(), systemID);


                        // ======= START

//                        OUD: setLabelStates(getView(), true);
                        // NIEUW: vervangen door meer granulair:
                        TextView textViewActionDevice = (TextView) theView.findViewById(R.id.textView_action_device);
                        textViewActionDevice.setText(getResources().getString(R.string.generic_connected));
                        bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_connected));

                        setEnableDisableStates(getView(), true);
                        setToggleStates(getView(), false);
                        // ======= END


                        int enumForService = currentEnumInSystemIDList + 1;
                        startOrRestartService(enumForService);


                    }
                });

        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        // Do nothing.
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();


    }



    private void startOrRestartService(int enumerator) {

        Log.w(LOG_TAG, "enum in beginning of service: " + enumerator);

        // We stop and restart the service so that the new IP addresses immediately get applied.
        Intent svcOld = new Intent(getActivity().getApplicationContext(), MQTTService.class);
        getActivity().stopService(svcOld);

        // getApplicationContext() instead of getActivity() because the service shouldn't be linked to a particular Activity.
        Intent svcNew = new Intent(getActivity().getApplicationContext(), MQTTService.class);


        Log.w(LOG_TAG, "enum right before putExtra " + enumerator);
        svcNew.putExtra("enumerator", enumerator);

        SharedPreferences settingsUserDetector = getActivity().getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        SharedPreferences.Editor editorUserDetector = settingsUserDetector.edit();
        editorUserDetector.putInt("enumerator", enumerator);
        editorUserDetector.commit();

        getActivity().startService(svcNew);

    }

    private void saveIPIfInserted(String systemID, String value) {


        // Note it only works with IPv4 addresses; not IPv6.
        Matcher matcher = Patterns.IP_ADDRESS.matcher(value);

        if (matcher.matches()) {

            // Only when the value is not empty, the value gets saved.

            SharedPreferences settingsUserDetector = getActivity().getSharedPreferences("com.yen.androidappthesisyen.user_detector", Context.MODE_PRIVATE);
            SharedPreferences.Editor editorUserDetector = settingsUserDetector.edit();
            // OUD
//            editorUserDetector.putString("ip_address_broker_" + (enumerator-1), value); // Because here we still counted from 1 onwards. Not 0 onwards.
            // NIEUW
            String preferenceKey = "ip_address_broker_" + systemID;
            editorUserDetector.putString(preferenceKey, value);
            editorUserDetector.commit();


            SharedPreferences settingsGestureHandler = getActivity().getSharedPreferences("com.yen.androidappthesisyen.gesture_handler", Context.MODE_PRIVATE);
            SharedPreferences.Editor editorGestureHandler = settingsGestureHandler.edit();
//            editorGestureHandler.putString("ip_address_" + (enumerator-1), value); // Because here we still counted from 1 onwards. Not 0 onwards.
            String preferenceKey2 = "ip_address_" + systemID;
            editorGestureHandler.putString(preferenceKey2, value);
            editorGestureHandler.commit();
        }

    }


    private void setBTRelatedStates(View returnedView) {

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);

        if (BTadapter == null) {
            // set toggle DISABLED
            // set toggle OFF

            // ENABLED/DISABLED
            toggleBT.setEnabled(false);
            bundleEnableDisableStates.putBoolean("R.id.toggle_BT", false);
            // ON/OFF
            toggleBT.setChecked(false);
            bundleToggleStates.putBoolean("R.id.toggle_BT", false);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), R.string.BT_adapter_not_found, Toast.LENGTH_LONG).show();
                }
            });


        } else {

            if (BTadapter.isEnabled()) {
                // set toggle ENABLED
                // set toggle ON
                toggleBT.setEnabled(true);
                bundleEnableDisableStates.putBoolean("R.id.toggle_BT", true);

                toggleBT.setChecked(true);
                bundleToggleStates.putBoolean("R.id.toggle_BT", true);

            } else {
                // set toggle ENABLED
                // set toggle OFF
                toggleBT.setEnabled(true);
                bundleEnableDisableStates.putBoolean("R.id.toggle_BT", true);

                toggleBT.setChecked(false);
                bundleToggleStates.putBoolean("R.id.toggle_BT", false);

            }
        }

    }

    private void insertDefaultLabelStates(View returnedView) {

        // we use the IDs as KEYs in the Bundle: you know they are always unique and traceable to the view object.

        TextView textViewPebble = (TextView) returnedView.findViewById(R.id.textView_pebble);
        TextView textViewGeneric = (TextView) returnedView.findViewById(R.id.textView_action_device);

        // retrieves the DEFAULT values from XML. So when we change the XML file, the following code adapts.
        bundleLabelStates.putString("R.id.textView_pebble", (String) textViewPebble.getText());
        bundleLabelStates.putString("R.id.textView_generic", (String) textViewGeneric.getText());

    }

    private void insertDefaultEnableDisableStates(View returnedView) {

        // ENABLED/DISABLED

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        bundleEnableDisableStates.putBoolean("R.id.toggle_BT", toggleBT.isEnabled());

        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        bundleEnableDisableStates.putBoolean("R.id.toggle_generic", toggleGeneric.isEnabled());

    }

    private void insertDefaultToggleStates(View returnedView) {

        // ON/OFF

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        bundleToggleStates.putBoolean("R.id.toggle_BT", toggleBT.isChecked());
        // hier niets voor buttonBTDiscoverable want heeft die functie niet he.

        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        bundleToggleStates.putBoolean("R.id.toggle_generic", toggleGeneric.isChecked());

    }


    private void setLabelStatesFromBundle(View returnedView) {

        TextView textViewPebble = (TextView) returnedView.findViewById(R.id.textView_pebble);
        TextView textViewGeneric = (TextView) returnedView.findViewById(R.id.textView_action_device);

        // we use the IDs as KEYs in the Bundle: you know they are always unique and traceable to the view object.
        textViewPebble.setText(bundleLabelStates.getString("R.id.textView_pebble"));
        textViewGeneric.setText(bundleLabelStates.getString("R.id.textView_generic"));

    }

    private void setEnableDisableStatesFromBundle(View returnedView) {

        // ENABLED/DISABLED

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        toggleBT.setEnabled(bundleEnableDisableStates.getBoolean("R.id.toggle_BT"));

        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setEnabled(bundleEnableDisableStates.getBoolean("R.id.toggle_generic"));

    }


    private void setToggleStatesFromBundle(View returnedView) {

        // ON/OFF

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        toggleBT.setChecked(bundleToggleStates.getBoolean("R.id.toggle_BT"));
        // NVT op buttonBTDiscoverable

        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setChecked(bundleToggleStates.getBoolean("R.id.toggle_generic"));

    }


    private void registerButtonAndToggleListeners(View returnedView) {

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        toggleBT.setOnClickListener(this);

        Button buttonConnectPebble = (Button) returnedView.findViewById(R.id.button_connect_pebble);
        buttonConnectPebble.setOnClickListener(this);
        Button buttonConnectGeneric = (Button) returnedView.findViewById(R.id.button_connect_generic);
        buttonConnectGeneric.setOnClickListener(this);

        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setOnClickListener(this);

    }

    private void initListViewMain(View returnedView) {

        /* If you know in advance what the size of the ArrayList is going to be, it is more efficient to specify the initial capacity. If you don't do this, the internal array will have to be repeatedly reallocated as the list grows. */
        arrayListNamesPairedBTDevices = new ArrayList<>(setPairedBTDevices.size());
        // FOR EACH is recommended usage, performance wise, compared to for (int i = ...
        for (BluetoothDevice pairedBTDevice : setPairedBTDevices) {
            arrayListNamesPairedBTDevices.add(pairedBTDevice.getName());
        }


        /*String[] myStringArray = {
                "Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6", "Item 7",
                "Item 8", "Item 9", "Item 10"
        };*/
        /*, "Item 11", "Item 12", "Item 13", "Item 14",
                "Item 15"*/
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
//                R.layout.listview_main_item, R.id.list_main_title, myStringArray);


        arrayListMainAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.listview_main_item, R.id.list_main_title, arrayListNamesPairedBTDevices);
//        DEFAULT LAYOUT: android.R.layout.simple_list_item_1

        ListView listView = (ListView) returnedView.findViewById(R.id.listView_main);
        listView.setAdapter(arrayListMainAdapter);

        /*To customize the appearance of each item you can override the toString() method for the objects in your array.
        Or, to create a view for each item that's something other than a TextView (for example, if you want an ImageView for each array item),
        extend the ArrayAdapter class and override getView() to return the type of view you want for each item.*/
        // UPDATE: je hebt het anders gedaan: door een aangepaste layout file R.layout.listview_main_item mee te geven aan constructor ArrayAdapter.
        /* UPDATE: wij pasten - momenteel - optie 1 toe:
        * To customize the appearance we can:

Create a customized layout file ourselves, representing each item. For example one with an ImageView and multiple TextViews.
Extend the ArrayAdapter class and override the getView() method to modify the views for each items and return.*/

    }


    public void setLabelStates(View returnedView, boolean isFound) {
        TextView textViewPebble = (TextView) returnedView.findViewById(R.id.textView_pebble);
        TextView textViewActionDevice = (TextView) returnedView.findViewById(R.id.textView_action_device);
        if (isFound) {
            textViewPebble.setText(getResources().getString(R.string.pebble_connected));
            bundleLabelStates.putString("R.id.textView_pebble", getResources().getString(R.string.pebble_connected));
            textViewActionDevice.setText(getResources().getString(R.string.generic_connected));
            bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_connected));
        } else {
            textViewPebble.setText(getResources().getString(R.string.pebble_not_connected));
            bundleLabelStates.putString("R.id.textView_pebble", getResources().getString(R.string.pebble_not_connected));
            textViewActionDevice.setText(getResources().getString(R.string.generic_not_connected));
            bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_not_connected));
        }
    }

    public void setEnableDisableStates(View returnedView, boolean state) {

        // FEITELIJK GAAN WE NOOIT GLOBAAL ZO BLUETOOTH TOGGLEN.
//        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
//        toggleBT.setEnabled(state);
//        bundleEnableDisableStates.putBoolean("R.id.toggle_BT", state);

        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setEnabled(state);
        bundleEnableDisableStates.putBoolean("R.id.toggle_generic", state);
    }


    public void setToggleStates(View returnedView, boolean state) {

        // FEITELIJK GAAN WE NOOIT GLOBAAL ZO BLUETOOTH TOGGLEN.
//        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
//        toggleBT.setChecked(state);
//        bundleToggleStates.putBoolean("R.id.toggle_BT", state);

        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setChecked(state);
        bundleToggleStates.putBoolean("R.id.toggle_generic", state);
    }


    @Override
    public void onClick(View v) {
        // View v is hier HET OBJECT DAT WERD AANGEKLIKT. Niet de View dat je veel gebruikt!

        switch (v.getId()) {

            case R.id.toggle_BT:

                ToggleButton toggleBT = (ToggleButton) v.findViewById(R.id.toggle_BT);
                // alternative: if(!BTadapter.isEnabled()){}
                // UPDATE: passen dat al toe IN startBT()
                if (toggleBT.isChecked()) {
                    startBT();

                } else {
                    stopBT();
                }

                break;


            case R.id.button_connect_pebble:

//                openPebbleCompanionAppOrGoAppStore();

                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);

                break;

            case R.id.button_connect_generic:

                List<String> listSavedSystemIDs = getListSavedSystemIDs(getActivity());

                if (listSavedSystemIDs == null || listSavedSystemIDs.size() == 0) {

                    // 05/09 OUD showIPDialog(0, "[No saved systemIDs found]");
                    showIPDialogNoSavedSystemIDs(0);
                } else {
                    Log.w(LOG_TAG, "the first saved systemID is " + listSavedSystemIDs.get(0));
                    showIPDialog(0, listSavedSystemIDs.get(0));
                }


                break;

            default:

                break;
        }

    }



    // TODO en nu moet gezorgd worden dat lijst links in GUI wordt geupdate. maar gebeurt al sowieso na een pairing en na restart vd app.
    // Maar kan dit direct nu al gebeuren?
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
//                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
//                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                }

                break;

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_CANCELED) {

                    ToggleButton toggleBT = (ToggleButton) getView().findViewById(R.id.toggle_BT);
                    // ENABLED/DISABLED
//                    toggleBT.setEnabled(false);
//                    bundleEnableDisableStates.putBoolean("R.id.toggle_BT", false);
                    // ON/OFF
                    toggleBT.setChecked(false);
                    bundleToggleStates.putBoolean("R.id.toggle_BT", false);
                }

                break;

        }
    }

    private void startBT() {

        if (!BTadapter.isEnabled()) {


            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);


            // TODO SYSTEEM LIJKT NIET TE WERKEN?
            // ! Lijst werd al ingevuld in onCreate ALS BT AANSTOND.
            // maar indien niet, wordt het NU gedaan.
            setPairedBTDevices = BTadapter.getBondedDevices();
            // arrayListNamesPairedBTDevices herbouwen: clearen + alles toevoegen
            // en clearen ADAPTER + alles toevoegen!
            arrayListNamesPairedBTDevices.clear();
            arrayListMainAdapter.clear();
            for (BluetoothDevice pairedBTDevice : setPairedBTDevices) {
                arrayListNamesPairedBTDevices.add(pairedBTDevice.getName());
                arrayListMainAdapter.add(pairedBTDevice.getName());
            }
            arrayListMainAdapter.notifyDataSetChanged();
//            ListView listViewMain = (ListView) getView().findViewById(R.id.listView_main);


        } else {

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), R.string.BT_already_enabled, Toast.LENGTH_LONG).show();
                }
            });

        }

    }


    private void stopBT() {


        if (BTadapter.isEnabled()) {

            BTadapter.disable();
            // hiervoor bestaat er precies geen equivalent voor startActivityForResult


        } else {

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), R.string.BT_already_disabled, Toast.LENGTH_LONG).show();
                }
            });

        }

    }


    @Override
    public void onResume() {
        super.onResume();


        // USED METHOD: we first create the view via onCreateView.
        // In onCreateView we should check the saved state of all the toggled buttons.
        // THEN, here in onResume, we check these states to re enable the enabled services.

        getToggleStatesAndEnableServices();


    }

    private void getToggleStatesAndEnableServices() {

        View theView = getView();

        ToggleButton toggleGeneric = (ToggleButton) theView.findViewById(R.id.toggle_generic);
        if (toggleGeneric.isEnabled() && toggleGeneric.isChecked()) {

        }


    }


    @Override
    public void onPause() {
        super.onPause();


        disableAllServices();
    }

    private void disableAllServices() {

        // Stop methods here for any services.

    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        // just to make sure.
        if (BTadapter.isDiscovering()) {
            BTadapter.cancelDiscovery();
        }

    }

}
