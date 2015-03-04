package com.yen.androidappthesisyen;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.List;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */

// TODO you have disabled some methods. Reenable them if you need them.

public class MainFragment extends Fragment implements View.OnClickListener {

//    private OnFragmentInteractionListener mListener;

    private static final String LOG_TAG = "MainFragment";

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
    private final int DATA_LOG_ACCEL_DATA_TAG = 42;
    private PebbleKit.PebbleDataLogReceiver myPebbleDataLOGReceiver;
    private StringBuilder resultBuilder = new StringBuilder();
    private final Handler handler = new Handler();


    public MainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_main, container, false);


        setLabelStates(returnedView, false);
        // If you do it in onCreate in MainActivity it seems to crash! (Even though the view has been inflated using setContentView(...) beforehand.
        // UPDATE: this is logical! Since the FRAGMENT inflates the View objects; not the ACTIVITY, in your case.
        setToggleStates(returnedView, false);


        // BEST SOLUTION TO COMPLETELY DECOUPLING THE GUI COMPONENTS AND THEIR LISTENERS FROM THE ACTIVITY THE FRAGMENT IS RESIDING IN.
        // SEE https://stackoverflow.com/questions/6091194/how-to-handle-button-clicks-using-the-xml-onclick-within-fragments
        registerButtonAndToggleListeners(returnedView);
        // TODO the above doesn't really belong here in onCreateView but for example in onViewCreated or similar.

        initListViewMain(returnedView);


        // We also place it here, since in case there is already text in the Output Window when arriving there, we immediately scroll.
        ((ScrollView) returnedView.findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


        return returnedView;
    }

    private void registerButtonAndToggleListeners(View returnedView) {

        // TODO zien voor optie om gewoon alle Buttons en ToggleButtons (+ nog?) te vinden in huidige interface?

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

        String[] myStringArray = {
                "Item 1", "Item 2", "Item 3", "Item 4", "Item 5"
        };
        /*, "Item 6", "Item 7",
                "Item 8", "Item 9", "Item 10", "Item 11", "Item 12", "Item 13", "Item 14",
                "Item 15"*/

        // changed 'this' to 'getActivity()'
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, myStringArray);


        ListView listView = (ListView) returnedView.findViewById(R.id.listView_main);
        listView.setAdapter(adapter);

        /*To customize the appearance of each item you can override the toString() method for the objects in your array.
        Or, to create a view for each item that's something other than a TextView (for example, if you want an ImageView for each array item),
        extend the ArrayAdapter class and override getView() to return the type of view you want for each item.*/

    }


    public void setToggleStates(View returnedView, boolean state) {

        ToggleButton toggleCommunicationTest = (ToggleButton) returnedView.findViewById(R.id.toggle_communication_test);
        toggleCommunicationTest.setEnabled(state);
        ToggleButton togglePebbleStream = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_stream);
        togglePebbleStream.setEnabled(state);
        ToggleButton togglePebbleDataLogging = (ToggleButton) returnedView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        togglePebbleDataLogging.setEnabled(state);
        ToggleButton toggleGeneric = (ToggleButton) returnedView.findViewById(R.id.toggle_generic);
        toggleGeneric.setEnabled(state);
    }

    public void setLabelStates(View returnedView, boolean isFound) {
        TextView textViewPebble = (TextView) returnedView.findViewById(R.id.textView_pebble);
        TextView textViewGeneric = (TextView) returnedView.findViewById(R.id.textView_generic);
        if (isFound) {
            textViewPebble.setText(R.string.pebble_found);
            textViewGeneric.setText(R.string.generic_found);
        } else {
            textViewPebble.setText(R.string.pebble_not_found);
            textViewGeneric.setText(R.string.generic_not_found);
        }


    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

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

            // TODO check whether really needed
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

        View theView = getView();

        // TODO doe dit anders! door te kijken naar de waarden die je hebt gesaved in je Preferences.
        ToggleButton toggleCommunicationTest = (ToggleButton) theView.findViewById(R.id.toggle_communication_test);
        if (toggleCommunicationTest.isChecked()) {
            startPebbleCommunicationTest();
        } else {
            // Do NOTHING since the service should have been disabled already when the fragment arrived in onPause().
        }
        ToggleButton togglePebbleStream = (ToggleButton) theView.findViewById(R.id.toggle_pebble_acceldata_stream);
        if (togglePebbleStream.isChecked()) {
            // TODO
        }
        ToggleButton togglePebbleDataLogging = (ToggleButton) theView.findViewById(R.id.toggle_pebble_acceldata_datalogging);
        if (togglePebbleDataLogging.isChecked()) {
            startPebbleDataLogging();
        }
        ToggleButton toggleGeneric = (ToggleButton) theView.findViewById(R.id.toggle_generic);
        if (toggleGeneric.isChecked()) {
            // TODO
        }


    }

    // TODO gebruik de volgende methode bv. bij het geval dat je terugkeert NAAR dit fragment van elders,
    // en de communication test listener dient NOG ACTIEF TE ZIJN.
    private void startPebbleCommunicationTest() {


        Log.w(LOG_TAG, "STARTED Pebble communication test BEFORE IF");

        // FOR DISPLAYING WHICH PEBBLE BUTTON (UP/SELECT/DOWN) WAS PRESSED.
        if (myPebbleDataReceiver == null) {


            // TODO use StringBuilder of something so the TEXT REMAINS
            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            // Without "getResources()." it also seems to work, but better to USE IT!
            String string = getResources().getString(R.string.start_communication_test);
            outputWindow.append("--- " + string + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            Log.w(LOG_TAG, "STARTED Pebble communication test");


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

    }


    private void stopPebbleCommunicationTest() {




        /* http://developer.getpebble.com/docs/android/com/getpebble/android/kit/PebbleKit/
        A convenience function to assist in programatically registering a broadcast receiver for the 'CONNECTED' intent. To avoid leaking memory, activities registering BroadcastReceivers must unregister them in the Activity's Activity.onPause() method.
         */

        // Checking for null is recommended.
        if (myPebbleDataReceiver != null) {

            // TODO use StringBuilder of something so the TEXT REMAINS
            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            String string = getResources().getString(R.string.stop_communication_test);
            outputWindow.append("--- " + string + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            Log.w(LOG_TAG, "STOPPED Pebble communication test");


            // TODO zien of deze try/catch werkt voor fixen: Caused by: java.lang.IllegalArgumentException: Receiver not registered: com.yen.myfirstapp.MainActivity$1@40fc03c0
            try {

                Log.w(LOG_TAG, "in TRY");
//                unregisterReceiver(myPebbleDataReceiver);
                // Changed to following since we are in a FRAGMENT; not an ACTIVITY.
                getActivity().unregisterReceiver(myPebbleDataReceiver);


                // TODO we gebruiken nu dit omdat anders de IF(... == NULL) soms te WEINIG wordt binnengegaan!
                myPebbleDataReceiver = null;


            } catch (IllegalArgumentException ex) {

                Log.w(LOG_TAG, "in CATCH");

                // TODO niets doen gewoon?


                // TODO we gebruiken nu dit omdat anders de IF(... == NULL) soms te WEINIG wordt binnengegaan!
                myPebbleDataReceiver = null;
            }

        }

    }


    private void startPebbleDataLogging() {

        // voor PEBBLE DATA LOGGING
        if (myPebbleDataLOGReceiver == null) {


            // TODO use StringBuilder of something so the TEXT REMAINS
            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            // Without "getResources()." it also seems to work, but better to USE IT!
            String string = getResources().getString(R.string.start_pebble_data_logging);
            outputWindow.append("--- " + string + " ---" + "\n");
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
                            // System.out.println("Misaligned data while data logging");
                            Log.w("DATA LOGGING", "Misaligned data while data logging");
                            return;
                        }


                        List<AccelData> accelDataList = AccelData.fromDataArray(data);


//                        resultBuilder.append("size van list accelDataList:" + accelDataList.size() + "\n");
                        // size gaf 1 MAAR ZAL DIT NIET ALTIJD ZO ZIJN
                        // OMDAT WE INGESTELD HADDEN MAAR 1 SAMPLE PER KEER TE STUREN?
                        // maar best houden voor als we samples verhogen.


                        for (final AccelData accelItem : accelDataList) {
//                            try {

//                                resultBuilder.append(accel.toJson().toString(2));
//                            resultBuilder.append(accelItem.getOneLineString() + "\n");


                            handler.post(new Runnable() {
                                @Override
                                public void run() {
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


    }


    private void stopPebbleDataLogging() {

        // Finally, as with any Receivers registered with PebbleKit,
        // remember to unregister your receiver when the user leaves the app:
        if (myPebbleDataLOGReceiver != null) {


            // TODO use StringBuilder of something so the TEXT REMAINS
            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
            // Without "getResources()." it also seems to work, but better to USE IT!
            String string = getResources().getString(R.string.stop_pebble_data_logging);
            outputWindow.append("--- " + string + " ---" + "\n");
            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            try {

                getActivity().unregisterReceiver(myPebbleDataLOGReceiver);

                myPebbleDataLOGReceiver = null;

            } catch (IllegalArgumentException ex) {
                // TODO niets doen gewoon?


                myPebbleDataLOGReceiver = null;
            }
        }

    }


    @Override
    public void onPause() {
        super.onPause();


        disableAllServices();
    }

    private void disableAllServices() {


        stopPebbleCommunicationTest();
        stopPebbleDataLogging();

        // TODO voeg stop-methods van andere services toe.
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
