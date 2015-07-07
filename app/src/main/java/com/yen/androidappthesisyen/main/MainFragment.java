package com.yen.androidappthesisyen.main;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.yen.androidappthesisyen.AccelData;
import com.yen.androidappthesisyen.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */

// TODO you have disabled some methods. Reenable them if you need them.

public class MainFragment extends Fragment implements View.OnClickListener {

    /*However, this is a bad idea on Android. Virtual method calls are expensive, much more so than instance field lookups. It's reasonable to follow common object-oriented programming practices and have getters and setters in the public interface, but within a class you should always access fields directly.

Without a JIT, direct field access is about 3x faster than invoking a trivial getter. With the JIT (where direct field access is as cheap as accessing a local), direct field access is about 7x faster than invoking a trivial getter.
 https://developer.android.com/training/articles/perf-tips.html*/


//    private OnFragmentInteractionListener mListener;

    /*Use Static Final For Constants
    * See https://developer.android.com/training/articles/perf-tips.html */
    private static final String LOG_TAG = MainFragment.class.getName();

    public static final UUID WATCHAPP_UUID = UUID.fromString("7c5167e8-9df4-479f-9353-714481681af1");


    // TODO use more useful name
    // For Pebble communication test
    private PebbleKit.PebbleDataReceiver myPebbleDataReceiver;
    private static final int KEY_BUTTON_EVENT = 2,
            BUTTON_EVENT_UP = 3,
            BUTTON_EVENT_DOWN = 4,
            BUTTON_EVENT_SELECT = 5,
            KEY_VIBRATION = 6;


    // For Pebble accel data logging
    private static final int DATA_LOG_ACCEL_DATA_TAG = 42;
    private PebbleKit.PebbleDataLogReceiver myPebbleDataLOGReceiver;
    private StringBuilder resultBuilder = new StringBuilder();
    private final Handler handler = new Handler();


    private static boolean areDefaultsInserted = false;
    // TODO testen of het STATIC moet/mag zijn!
    // TODO of moet die = new in onCreate? TO TEST: want mss als fragment GANS opnieuw wordt gemaakt (wanneer?) zijn onze Bundles weer leeg?!
    private static Bundle bundleLabelStates = new Bundle();
    private static Bundle bundleEnableDisableStates = new Bundle();
    private static Bundle bundleToggleStates = new Bundle();
    // je kon BOOLEAN ARRAY toepassen, maar nu heb je Bundle, voor het geval je TOCH met savedInstanceState gaat werken voor terugkrijgen van states.


    private BluetoothAdapter BTadapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_ENABLE_BT = 50;
    private static final int REQUEST_BT_DISCOVERABLE = 51;
    private Set<BluetoothDevice> setPairedBTDevices;


    private ArrayAdapter<String> arrayListMainAdapter;
    private List<String> arrayListNamesPairedBTDevices;


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

        // TODO als je toch gaat werken met savedInstanceState.
//        if(savedInstanceState == null){
//            bundleLabelStates = new Bundle();
//            bundleEnableDisableStates = new Bundle();
//            bundleToggleStates = new Bundle();
//        } else {
//            // TODO haal de 2 bundles uit savedInstanceState en assign aan de vars.
//        }



        /*public void setRetainInstance (boolean retain)
        * onCreate(Bundle) will not be called since the fragment is not being re-created.
        * https://developer.android.com/reference/android/app/Fragment.html#setRetainInstance(boolean) */
        // DUS HIER KOMEN WE ENKEL DE ALLEREERSTE KEER DAT FRAGMENT WORDT GEMAAKT.
        // DUS HIER INITIALISEREN VAN DE BUNDLES?
        // EN DIRECT DUS DE BEGIN STATES INVULLEN HIER ZEKER?!
//        bundleLabelStates = new Bundle();
//        bundleEnableDisableStates = new Bundle();
//        bundleToggleStates = new Bundle();


//        BTadapter = BluetoothAdapter.getDefaultAdapter();
//        Log.w("BLUETOOTH", "name BT adapter: " + BTadapter.getName());

        // direct ook die lijst van paired BT devices opvullen.
        // get a list of paired devices by calling getBondedDevices()
        // maar lijst LEEG als BT uit staat!
        setPairedBTDevices = BTadapter.getBondedDevices();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_main, container, false);


        // SYSTEEM DAT WE NU NIET TOEPASSEN
//        if(savedInstanceState == null){
//
//            // If you do it in onCreate in MainActivity it seems to crash! (Even though the view has been inflated using setContentView(...) beforehand.
//            // UPDATE: this is logical! Since the FRAGMENT inflates the View objects; not the ACTIVITY, in your case.
//            setLabelStates(returnedView, false);
//        setEnableDisableStatesFromBundle
//            setEnableDisableStates(returnedView, false);
//
//        } else {
//
//            // TODO gebruik die bundle om de state eruit te halen van je buttons enzo.
//
//        }

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


        // hier moet geen if(==null) rond omdat bij creëren view dit ALTIJD moet gebeuren, terwijl hierboven niet steeds hetzelfde gedrag mag zijn!
        // BEST SOLUTION TO COMPLETELY DECOUPLING THE GUI COMPONENTS AND THEIR LISTENERS FROM THE ACTIVITY THE FRAGMENT IS RESIDING IN.
        // SEE https://stackoverflow.com/questions/6091194/how-to-handle-button-clicks-using-the-xml-onclick-within-fragments
        // TODO via onClick in XML
        registerButtonAndToggleListeners(returnedView);
        // TODO the above doesn't really belong here in onCreateView but for example in onViewCreated or similar.

        initListViewMain(returnedView);


        // We also place it here, since in case there is already text in the Output Window when arriving there, we immediately scroll.
        ((ScrollView) returnedView.findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


        // TODO maar doen we nu geen overbodige enable/disable en on/off aanpassingen bij de toggle voor BT?
        // of toch behouden wat we hebben: anders wordt het te complex.
        setBTRelatedStates(returnedView);


        return returnedView;
    }

    private void setBTRelatedStates(View returnedView) {

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        Button buttonBTDiscoverable = (Button) returnedView.findViewById(R.id.button_BT_discoverable);

        if (BTadapter == null) {
            // set toggle DISABLED
            // set toggle OFF

            // ENABLED/DISABLED
            toggleBT.setEnabled(false);
            bundleEnableDisableStates.putBoolean("R.id.toggle_BT", false);
            buttonBTDiscoverable.setEnabled(false);
            bundleEnableDisableStates.putBoolean("R.id.button_BT_discoverable", false);
            // ON/OFF
            toggleBT.setChecked(false);
            bundleToggleStates.putBoolean("R.id.toggle_BT", false);
            // NVT voor buttonBTDiscoverable

            Toast.makeText(getActivity(), R.string.BT_adapter_not_found, Toast.LENGTH_LONG).show();

        } else {

            if (BTadapter.isEnabled()) {
                // set toggle ENABLED
                // set toggle ON
                toggleBT.setEnabled(true);
                bundleEnableDisableStates.putBoolean("R.id.toggle_BT", true);
                buttonBTDiscoverable.setEnabled(true);
                bundleEnableDisableStates.putBoolean("R.id.button_BT_discoverable", true);

                toggleBT.setChecked(true);
                bundleToggleStates.putBoolean("R.id.toggle_BT", true);
                // NVT voor buttonBTDiscoverable

                Toast.makeText(getActivity(), R.string.BT_is_enabled, Toast.LENGTH_LONG).show();

            } else {
                // set toggle ENABLED
                // set toggle OFF
                toggleBT.setEnabled(true);
                bundleEnableDisableStates.putBoolean("R.id.toggle_BT", true);
                buttonBTDiscoverable.setEnabled(false);
                bundleEnableDisableStates.putBoolean("R.id.button_BT_discoverable", false);

                toggleBT.setChecked(false);
                bundleToggleStates.putBoolean("R.id.toggle_BT", false);
                // NVT voor buttonBTDiscoverable

                Toast.makeText(getActivity(), R.string.BT_is_disabled, Toast.LENGTH_LONG).show();
            }
        }

    }

    private void insertDefaultLabelStates(View returnedView) {

        // we use the IDs as KEYs in the Bundle: you know they are always unique and traceable to the view object.

        TextView textViewPebble = (TextView) returnedView.findViewById(R.id.textView_pebble);
        TextView textViewGeneric = (TextView) returnedView.findViewById(R.id.textView_generic);

        // retrieves the DEFAULT values from XML. So when we change the XML file, the following code adapts.
        bundleLabelStates.putString("R.id.textView_pebble", (String) textViewPebble.getText());
        bundleLabelStates.putString("R.id.textView_generic", (String) textViewGeneric.getText());

    }

    private void insertDefaultEnableDisableStates(View returnedView) {

        // ENABLED/DISABLED

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        bundleEnableDisableStates.putBoolean("R.id.toggle_BT", toggleBT.isEnabled());
        Button buttonBTDiscoverable = (Button) returnedView.findViewById(R.id.button_BT_discoverable);
        bundleEnableDisableStates.putBoolean("R.id.button_BT_discoverable", buttonBTDiscoverable.isEnabled());

        ToggleButton toggleCommunicationTest = (ToggleButton) returnedView.findViewById(R.id.toggle_communication_test);
        bundleEnableDisableStates.putBoolean("R.id.toggle_communication_test", toggleCommunicationTest.isEnabled());
        ToggleButton togglePebbleStream = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_stream);
        bundleEnableDisableStates.putBoolean("R.id.toggle_pebble_acceldata_stream", togglePebbleStream.isEnabled());
        ToggleButton togglePebbleDataLogging = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        bundleEnableDisableStates.putBoolean("R.id.toggle_pebble_acceldata_datalogging", togglePebbleDataLogging.isEnabled());
        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        bundleEnableDisableStates.putBoolean("R.id.toggle_generic", toggleGeneric.isEnabled());

    }

    private void insertDefaultToggleStates(View returnedView) {

        // ON/OFF

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        bundleToggleStates.putBoolean("R.id.toggle_BT", toggleBT.isChecked());
        // hier niets voor buttonBTDiscoverable want heeft die functie niet he.

        ToggleButton toggleCommunicationTest = (ToggleButton) returnedView.findViewById(R.id.toggle_communication_test);
        bundleToggleStates.putBoolean("R.id.toggle_communication_test", toggleCommunicationTest.isChecked());
        ToggleButton togglePebbleStream = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_stream);
        bundleToggleStates.putBoolean("R.id.toggle_pebble_acceldata_stream", togglePebbleStream.isChecked());
        ToggleButton togglePebbleDataLogging = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        bundleToggleStates.putBoolean("R.id.toggle_pebble_acceldata_datalogging", togglePebbleDataLogging.isChecked());
        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        bundleToggleStates.putBoolean("R.id.toggle_generic", toggleGeneric.isChecked());

    }


    private void setLabelStatesFromBundle(View returnedView) {

        TextView textViewPebble = (TextView) returnedView.findViewById(R.id.textView_pebble);
        TextView textViewGeneric = (TextView) returnedView.findViewById(R.id.textView_generic);

        // we use the IDs as KEYs in the Bundle: you know they are always unique and traceable to the view object.
        textViewPebble.setText(bundleLabelStates.getString("R.id.textView_pebble"));
        textViewGeneric.setText(bundleLabelStates.getString("R.id.textView_generic"));

    }

    private void setEnableDisableStatesFromBundle(View returnedView) {

        // ENABLED/DISABLED

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        toggleBT.setEnabled(bundleEnableDisableStates.getBoolean("R.id.toggle_BT"));
        Button buttonBTDiscoverable = (Button) returnedView.findViewById(R.id.button_BT_discoverable);
        buttonBTDiscoverable.setEnabled(bundleEnableDisableStates.getBoolean("R.id.button_BT_discoverable"));

        ToggleButton toggleCommunicationTest = (ToggleButton) returnedView.findViewById(R.id.toggle_communication_test);
        toggleCommunicationTest.setEnabled(bundleEnableDisableStates.getBoolean("R.id.toggle_communication_test"));
        ToggleButton togglePebbleStream = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_stream);
        togglePebbleStream.setEnabled(bundleEnableDisableStates.getBoolean("R.id.toggle_pebble_acceldata_stream"));
        ToggleButton togglePebbleDataLogging = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        togglePebbleDataLogging.setEnabled(bundleEnableDisableStates.getBoolean("R.id.toggle_pebble_acceldata_datalogging"));
        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setEnabled(bundleEnableDisableStates.getBoolean("R.id.toggle_generic"));

    }


    private void setToggleStatesFromBundle(View returnedView) {

        // ON/OFF

        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        toggleBT.setChecked(bundleToggleStates.getBoolean("R.id.toggle_BT"));
        // NVT op buttonBTDiscoverable

        ToggleButton toggleCommunicationTest = (ToggleButton) returnedView.findViewById(R.id.toggle_communication_test);
        toggleCommunicationTest.setChecked(bundleToggleStates.getBoolean("R.id.toggle_communication_test"));
        ToggleButton togglePebbleStream = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_stream);
        togglePebbleStream.setChecked(bundleToggleStates.getBoolean("R.id.toggle_pebble_acceldata_stream"));
        ToggleButton togglePebbleDataLogging = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        togglePebbleDataLogging.setChecked(bundleToggleStates.getBoolean("R.id.toggle_pebble_acceldata_datalogging"));
        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setChecked(bundleToggleStates.getBoolean("R.id.toggle_generic"));

    }


    // TODO eventueel usen als je toch met ander systeem wilt werken.
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        outState.putBundle();
//
//        Boolean[] labelStates = { find, false };
//
//        // DESCRIPTION: communication test (enabled/disabled), communication test (on/off), data stream ..., data stream ..., data logging ..., data logging ..., generic stream ..., generic stream ...
//        Boolean[] toggleStates = { find, false };
////        outState.putBooleanArray();
//
//        super.onSaveInstanceState(outState);
//    }

    // TODO in XML met onClick werken.
    private void registerButtonAndToggleListeners(View returnedView) {

        // Die LOG zie je dus bij elke orientation change. Maar das normaal want VIEW wordt HERMAAKT steeds.
//        Log.w("MAIN FRAGMENT", "registered BUTTON TOGGLE LISTENERS");

        // TODO zien voor optie om gewoon alle Buttons en ToggleButtons (+ nog?) te vinden in huidige interface?


        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
        toggleBT.setOnClickListener(this);

        Button buttonBTDiscoverable = (Button) returnedView.findViewById(R.id.button_BT_discoverable);
        buttonBTDiscoverable.setOnClickListener(this);

        Button buttonConnectPebble = (Button) returnedView.findViewById(R.id.button_connect_pebble);
        buttonConnectPebble.setOnClickListener(this);

        ToggleButton toggleCommunicationTest = (ToggleButton) returnedView.findViewById(R.id.toggle_communication_test);
        toggleCommunicationTest.setOnClickListener(this);
        ToggleButton togglePebbleStream = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_stream);
        togglePebbleStream.setOnClickListener(this);
        ToggleButton togglePebbleDataLogging = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        togglePebbleDataLogging.setOnClickListener(this);
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
        TextView textViewGeneric = (TextView) returnedView.findViewById(R.id.textView_generic);
        if (isFound) {
            textViewPebble.setText(getResources().getString(R.string.pebble_found));
            bundleLabelStates.putString("R.id.textView_pebble", getResources().getString(R.string.pebble_found));
            textViewGeneric.setText(getResources().getString(R.string.generic_found));
            bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_found));
        } else {
            textViewPebble.setText(getResources().getString(R.string.pebble_not_found));
            bundleLabelStates.putString("R.id.textView_pebble", getResources().getString(R.string.pebble_not_found));
            textViewGeneric.setText(getResources().getString(R.string.generic_not_found));
            bundleLabelStates.putString("R.id.textView_generic", getResources().getString(R.string.generic_not_found));
        }
    }

    public void setEnableDisableStates(View returnedView, boolean state) {

        // FEITELIJK GAAN WE NOOIT GLOBAAL ZO BLUETOOTH TOGGLEN.
//        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
//        toggleBT.setEnabled(state);
//        bundleEnableDisableStates.putBoolean("R.id.toggle_BT", state);
        ToggleButton toggleCommunicationTest = (ToggleButton) returnedView.findViewById(R.id.toggle_communication_test);
        toggleCommunicationTest.setEnabled(state);
        bundleEnableDisableStates.putBoolean("R.id.toggle_communication_test", state);
        ToggleButton togglePebbleStream = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_stream);
        togglePebbleStream.setEnabled(state);
        bundleEnableDisableStates.putBoolean("R.id.toggle_pebble_acceldata_stream", state);
        ToggleButton togglePebbleDataLogging = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        togglePebbleDataLogging.setEnabled(state);
        bundleEnableDisableStates.putBoolean("R.id.toggle_pebble_acceldata_datalogging", state);
        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setEnabled(state);
        bundleEnableDisableStates.putBoolean("R.id.toggle_generic", state);
    }


    public void setToggleStates(View returnedView, boolean state) {

        // FEITELIJK GAAN WE NOOIT GLOBAAL ZO BLUETOOTH TOGGLEN.
//        ToggleButton toggleBT = (ToggleButton) returnedView.findViewById(R.id.toggle_BT);
//        toggleBT.setChecked(state);
//        bundleToggleStates.putBoolean("R.id.toggle_BT", state);
        ToggleButton toggleCommunicationTest = (ToggleButton) returnedView.findViewById(R.id.toggle_communication_test);
        toggleCommunicationTest.setChecked(state);
        bundleToggleStates.putBoolean("R.id.toggle_communication_test", state);
        ToggleButton togglePebbleStream = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_stream);
        togglePebbleStream.setChecked(state);
        bundleToggleStates.putBoolean("R.id.toggle_pebble_acceldata_stream", state);
        ToggleButton togglePebbleDataLogging = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        togglePebbleDataLogging.setChecked(state);
        bundleToggleStates.putBoolean("R.id.toggle_pebble_acceldata_datalogging", state);
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


            case R.id.button_BT_discoverable:

                setBTDiscoverable();

                break;

            case R.id.button_connect_pebble:

                openPebbleCompanionAppOrGoAppStore();

                break;

            case R.id.toggle_communication_test:

                ToggleButton togglePebbleCommunicationTest = (ToggleButton) v.findViewById(R.id.toggle_communication_test);

                if (togglePebbleCommunicationTest.isChecked()) {
                    // START TEST
                    startPebbleCommunicationTest();

                } else {
                    // STOP TEST
                    stopPebbleCommunicationTest();
                }

                break; // NOT "return true/false" since return type is now VOID.

            case R.id.toggle_pebble_acceldata_datalogging:

                ToggleButton togglePebbleDataLogging = (ToggleButton) v.findViewById(R.id.toggle_pebble_acceldata_datalogging);

                if (togglePebbleDataLogging.isChecked()) {
                    // START DATA LOGGING
                    startPebbleDataLogging();

                } else {
                    // STOP DATA LOGGING
                    stopPebbleDataLogging();
                }

                break;

            default:

                break;
        }

    }

    private void startBT() {

        if (!BTadapter.isEnabled()) {

            // DO NOT USE BTadapter.enable(); HERE: that wouldn't show a dialog to the user. Bad practice!

            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
            // TODO eventueel: checken dat er geen error was bij enablen, via https://developer.android.com/training/basics/intents/result.html#ReceiveResult
            // en dan daarop de Toast aanpassen.

            // TODO kunnen dit stuk BUITEN de IF lus zetten, zodat sowieso de button wordt geenabled. Maar is wrsl overbodig en kan mss ongewenst gedrag geven...
            Button buttonBTDiscoverable = (Button) getView().findViewById(R.id.button_BT_discoverable);
            buttonBTDiscoverable.setEnabled(true);
            bundleEnableDisableStates.putBoolean("R.id.button_BT_discoverable", true);


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


            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            outputWindow.append("--- " + getResources().getString(R.string.start_BT) + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);

            // THE REASON parameter "R.string.pebble_companion_app_found" WORKS INSTEAD OF NEEDING
            // "getResources().getString() is because makeText also has a parameter list that
            // only needs the string ID.
            Toast.makeText(getActivity(), R.string.start_BT, Toast.LENGTH_LONG).show();

        } else {

            Toast.makeText(getActivity(), R.string.BT_already_enabled, Toast.LENGTH_LONG).show();

        }

    }

    // TODO
    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO nodig?
//        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_ENABLE_BT){

            if(resultCode == RESULT_OK){

            }
        }

    }*/

    private void setBTDiscoverable() {

        // TODO eventueel de button DISABLEN gedurende 2 min.
        // maar op zich hoeft het niet: als je erop klikt en 2 min. nog niet voorbij, zorgt de IF lus dat er geen problemen komen.

        if (BTadapter == null) {
            Log.w("BLUETOOTH", "BT adapter is null");
        }

        if (!BTadapter.isDiscovering()) {


            // TODO zien of je volgende werkende kan krijgen.
            // Om zo die 2de ListView op te vullen met discoverable devices.
            // Maar die 2de ListView zit dus in ANDER fragment.
            // Communiceren via andere fragments: TO RESEARCH.
//            ArrayAdapter<String> neededAdapter = (ArrayAdapter<String>) ((MasterDetailItemListFragment) getFragmentManager().findFragmentById(R.id.masterdetailitem_list)).getListAdapter();
//            neededAdapter.clear();
//            neededAdapter.add("blaaa");


            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), REQUEST_BT_DISCOVERABLE);

            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            outputWindow.append("--- " + getResources().getString(R.string.start_BT_discoverable) + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);

            Toast.makeText(getActivity(), R.string.start_BT_discoverable, Toast.LENGTH_LONG).show();

        } else {


            // TODO mss dus best met TOGGLE button werken want met normal button ziet er raar uit.
            // TODO systeem werkt niet? we geraken hier nooit?
            BTadapter.cancelDiscovery();

            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            outputWindow.append("--- " + getResources().getString(R.string.BT_already_discoverable) + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);

            Toast.makeText(getActivity(), R.string.BT_already_discoverable, Toast.LENGTH_LONG).show();
        }

    }

    private void stopBT() {


        if (BTadapter.isEnabled()) {

            BTadapter.disable();
            // hiervoor bestaat er precies geen equivalent voor startActivityForResult

            Button buttonBTDiscoverable = (Button) getView().findViewById(R.id.button_BT_discoverable);
            buttonBTDiscoverable.setEnabled(false);
            bundleEnableDisableStates.putBoolean("R.id.button_BT_discoverable", false);

            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            outputWindow.append("--- " + getResources().getString(R.string.stop_BT) + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);

            Toast.makeText(getActivity(), R.string.stop_BT, Toast.LENGTH_LONG).show();
        } else {

            Toast.makeText(getActivity(), R.string.BT_already_disabled, Toast.LENGTH_LONG).show();
        }

    }


    private void openPebbleCompanionAppOrGoAppStore() {

        // Changed "context" to "getActivity()"
        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage("com.getpebble.android");
        if (intent != null) {
            Toast t = Toast.makeText(getActivity(), R.string.pebble_companion_app_found, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();

            Toast t2 = Toast.makeText(getActivity(), R.string.connect_your_pebble, Toast.LENGTH_LONG);
            t2.setGravity(Gravity.CENTER, 0, 0);
            t2.show();

            /*You can use flags to modify the default behaviour of how an activity will be associated with a task when using startActivity() to start the activity:

FLAG_ACTIVITY_NEW_TASK – This starts the activity in a new task. If it’s already running in a task, then that task is brought to the foreground and the activity’s onNewIntent() method receives the intent (this is the same as using singleTask in the manifest)
FLAG_ACTIVITY_SINGLE_TOP – In this case, if the activity is currently at the top of the stack, then its onNewIntent() method receives the intent. A new activity is not created (this is the same as using singleTop in the manifest).
FLAG_ACTIVITY_CLEAR_TOP – Here, if the activity is already running in the current task, then this activity is brought to the top of the stack (all others above it are destroyed) and its onNewIntent() method will receive the intent. There is no launchMode equivalent for this flag.
Make a note: You can use the Intent Flags to override the launch mode defined in the manifest file!*/
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
        } else {

            Toast t = Toast.makeText(getActivity(), R.string.pebble_companion_app_not_found, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();

            Toast t2 = Toast.makeText(getActivity(), R.string.download_and_retry, Toast.LENGTH_LONG);
            t2.setGravity(Gravity.CENTER, 0, 0);
            t2.show();

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=com.getpebble.android"));
            getActivity().startActivity(intent);
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

        // TODO of de states retrieven via de Bundles?

        View theView = getView();

        // TODO doe dit anders! door te kijken naar de waarden die je hebt gesaved in je Preferences.
        // UPDATE: maar Preferences is wrsl voor ECHTE preferenties van in keuzemenu? Dus via Bundles is beste optie hier?

        // NIET NODIG OMDAT WE NOOIT BT GAAN DISABLEN BIJ ONPAUSE.
        /*ToggleButton toggleBT = (ToggleButton) theView.findViewById(R.id.toggle_BT);
        if (toggleBT.isEnabled() && toggleBT.isChecked()) {
            startBT();
        }*/

        ToggleButton toggleCommunicationTest = (ToggleButton) theView.findViewById(R.id.toggle_communication_test);
        if (toggleCommunicationTest.isEnabled() && toggleCommunicationTest.isChecked()) {
            startPebbleCommunicationTest();
        } else {
            // Do NOTHING since the service should have been disabled already when the fragment arrived in onPause().
        }
        ToggleButton togglePebbleStream = (ToggleButton) theView.findViewById(R.id.toggle_pebble_acceldata_stream);
        if (togglePebbleStream.isEnabled() && togglePebbleStream.isChecked()) {
            // TODO
        }
        ToggleButton togglePebbleDataLogging = (ToggleButton) theView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        if (togglePebbleDataLogging.isEnabled() && togglePebbleDataLogging.isChecked()) {
            startPebbleDataLogging();
        }
        ToggleButton toggleGeneric = (ToggleButton) theView.findViewById(R.id.toggle_generic);
        if (toggleGeneric.isEnabled() && toggleGeneric.isChecked()) {
            // TODO
        }


    }

    // TODO gebruik de volgende methode bv. bij het geval dat je terugkeert NAAR dit fragment van elders,
    // en de communication test listener dient NOG ACTIEF TE ZIJN.
    private void startPebbleCommunicationTest() {


        // FOR DISPLAYING WHICH PEBBLE BUTTON (UP/SELECT/DOWN) WAS PRESSED.
        if (myPebbleDataReceiver == null) {


            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            // Without "getResources()." it also seems to work, but better to USE IT!
            outputWindow.append("--- " + getResources().getString(R.string.start_communication_test) + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            // public static abstract class PebbleKit.PebbleDataReceiver
            // extends android.content.BroadcastReceiver
            myPebbleDataReceiver = new PebbleKit.PebbleDataReceiver(WATCHAPP_UUID) {

                // in tutorial: public void receiveData(Context context, int transactionId, PebbleDictionary data)
                @Override
                public void receiveData(Context context, int transactionId, PebbleDictionary pebbleTuples) {

                    // ACK het bericht
                    PebbleKit.sendAckToPebble(context, transactionId);

                    // check of de key bestaat
                    // getUnsignedInteger BESTAAT NIET MEER?
                    if (pebbleTuples.getUnsignedIntegerAsLong(KEY_BUTTON_EVENT) != null) {
                        int button = pebbleTuples.getUnsignedIntegerAsLong(KEY_BUTTON_EVENT).intValue();

                        switch (button) {
                            case BUTTON_EVENT_UP:
                                outputWindow.append(getResources().getString(R.string.pebble_button_up_pressed));
                                ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);
                                break;
                            case BUTTON_EVENT_DOWN:
                                outputWindow.append(getResources().getString(R.string.pebble_button_down_pressed));
                                ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);
                                break;
                            case BUTTON_EVENT_SELECT:
                                outputWindow.append(getResources().getString(R.string.pebble_button_select_pressed));
                                ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);
                                break;
                        }

                    }


                    //Make the watch vibrate
                    PebbleDictionary dict = new PebbleDictionary();
                    dict.addInt32(KEY_VIBRATION, 0);
                    PebbleKit.sendDataToPebble(context, WATCHAPP_UUID, dict);


                }
            };

            // first parameter was 'this' (for type CONTEXT)
            PebbleKit.registerReceivedDataHandler(getActivity(), myPebbleDataReceiver);


        }

        // of IN if lus?
        PebbleKit.startAppOnPebble(getActivity(), WATCHAPP_UUID);

    }


    private void stopPebbleCommunicationTest() {


        /* http://developer.getpebble.com/docs/android/com/getpebble/android/kit/PebbleKit/
        A convenience function to assist in programmatically registering a broadcast receiver for the 'CONNECTED' intent. To avoid leaking memory, activities registering BroadcastReceivers must unregister them in the Activity's Activity.onPause() method.
         */

        // Checking for null is recommended.
        if (myPebbleDataReceiver != null) {


            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            outputWindow.append("--- " + getResources().getString(R.string.stop_communication_test) + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            // TODO zien of deze try/catch werkt voor fixen: Caused by: java.lang.IllegalArgumentException: Receiver not registered: com.yen.myfirstapp.MainActivity$1@40fc03c0
            try {

//                unregisterReceiver(myPebbleDataReceiver);
                // Changed to following since we are in a FRAGMENT; not an ACTIVITY.
                getActivity().unregisterReceiver(myPebbleDataReceiver);


                // TODO we gebruiken nu dit omdat anders de IF(... == NULL) soms te WEINIG wordt binnengegaan!
                myPebbleDataReceiver = null;


            } catch (IllegalArgumentException ex) {


                // TODO niets doen gewoon?


                // TODO we gebruiken nu dit omdat anders de IF(... == NULL) soms te WEINIG wordt binnengegaan!
                myPebbleDataReceiver = null;
            }

        }


        // IN if?
        PebbleKit.closeAppOnPebble(getActivity(), WATCHAPP_UUID);

    }


    private void startPebbleDataLogging() {

        // voor PEBBLE DATA LOGGING
        if (myPebbleDataLOGReceiver == null) {


            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            // Without "getResources()." it also seems to work, but better to USE IT!
            outputWindow.append("--- " + getResources().getString(R.string.start_pebble_data_logging) + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            // MEER INFO https://developer.android.com/reference/android/os/Handler.html
            // + https://developer.android.com/training/multiple-threads/communicate-ui.html


        /*ZORGT VOOR DELAYS (skipped frames) IN EMULATOR DUS DIT NIET GEBRUIKEN.
        TUTORIAL GEBRUIKT OOK BOVENSTAANDE FINAL IMPLEMENTATIE.
        if(handler == null){
            handler = new Handler();
        }
        */


            myPebbleDataLOGReceiver = new PebbleKit.PebbleDataLogReceiver(WATCHAPP_UUID) {

                @Override
                public void receiveData(Context context, UUID logUuid, Long timestamp, Long tag, byte[] data) {
                    // Important note: If your Java IDE places a call to super() by default, this will cause an UnsupportedOperationException to be thrown.
                    // Remove this line to avoid the Exception.
                    // super.receiveData(context, logUuid, timestamp, tag, data);


                    if (tag.intValue() == DATA_LOG_ACCEL_DATA_TAG) {

                        // TODO klopt dit systeem nog?
                        // misaligned data, just drop it
                        if (data.length % 15 != 0 || data.length < 15) {
                            Log.w("DATA LOGGING", "Misaligned data while data logging");
                            return;
                        }


                        List<AccelData> accelDataList = AccelData.fromDataArray(data);


//                        resultBuilder.append("size van list accelDataList:" + accelDataList.size() + "\n");
                        // size gaf 1 MAAR ZAL DIT NIET ALTIJD ZO ZIJN
                        // OMDAT WE INGESTELD HADDEN MAAR 1 SAMPLE PER KEER TE STUREN?
                        // maar best houden voor als we samples verhogen.


                        /* Use Enhanced For Loop Syntax
                        * two() is fastest for devices without a JIT, and indistinguishable from one() for devices with a JIT. It uses the enhanced for loop syntax introduced in version 1.5 of the Java programming language.

So, you should use the enhanced for loop by default, but consider a hand-written counted loop for performance-critical ArrayList iteration. */
                        for (final AccelData accelItem : accelDataList) {
//                            try {

//                                resultBuilder.append(accel.toJson().toString(2));
//                            resultBuilder.append(accelItem.getOneLineString() + "\n");


                            /* Gebruik van POST: uitleg zie code @ https://developer.android.com/guide/components/processes-and-threads.html */
                            handler.post(new Runnable() {
                                @Override
                                public void run() {

                                    // TODO test of nuttig/nadelig.
                                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

//                                    outputWindow.setText(resultBuilder.toString());
//                                    outputWindow.append(resultBuilder.toString());
                                    outputWindow.append(accelItem.getOneLineString() + "\n");
                                    ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);
                                }
                            });
//
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
                        }


                    }
                }

                @Override
                public void onFinishSession(Context context, UUID logUuid, Long timestamp, Long tag) {
                    super.onFinishSession(context, logUuid, timestamp, tag);

                    // Session is finished, use the data!

                    // logView.setText("Sending data log FINISHED. " + resultBuilder.toString());
//                    outputWindow.setText("Sending data log FINISHED.");

                    outputWindow.append("--- " + getResources().getString(R.string.sending_data_log_finished) + " ---" + "\n");
                    ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);
                }
            };

            PebbleKit.registerDataLogReceiver(getActivity(), myPebbleDataLOGReceiver);


            // TODO TEST: PebbleKit.requestDataLogsForApp(this, WATCHAPP_UUID);
            // A convenience function to emit an intent to pebble.apk to request the data logs for a particular app.

        }

        // IN if lus?
        PebbleKit.startAppOnPebble(getActivity(), WATCHAPP_UUID);


    }


    private void stopPebbleDataLogging() {

        // Finally, as with any Receivers registered with PebbleKit,
        // remember to unregister your receiver when the user leaves the app:
        if (myPebbleDataLOGReceiver != null) {


            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            // Without "getResources()." it also seems to work, but better to USE IT!
            outputWindow.append("--- " + getResources().getString(R.string.stop_pebble_data_logging) + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            try {

                getActivity().unregisterReceiver(myPebbleDataLOGReceiver);

                myPebbleDataLOGReceiver = null;

            } catch (IllegalArgumentException ex) {
                // TODO niets doen gewoon?


                myPebbleDataLOGReceiver = null;
            }
        }


        // IN if?
        PebbleKit.closeAppOnPebble(getActivity(), WATCHAPP_UUID);

    }


    @Override
    public void onPause() {
        super.onPause();


        disableAllServices();
    }

    private void disableAllServices() {

        // NIET de BT service disablen he. Gebeurt expliciet als user het wilt en niet zomaar bij onPause ofzo.

        stopPebbleCommunicationTest();
        stopPebbleDataLogging();

        // hier dus best NIET maar wel in de individuele methodes?
//        PebbleKit.closeAppOnPebble(getActivity(), WATCHAPP_UUID);

        // TODO voeg stop-methods van andere services toe.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        // just to make sure.
        if (BTadapter.isDiscovering()) {
            BTadapter.cancelDiscovery();
        }

    }


    // TODO: Rename method, update argument and hook method into UI event
    /*public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }*/




   /* @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }*/




    /*@Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }*/


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    /*public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }*/

}
