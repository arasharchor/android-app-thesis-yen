package com.yen.androidappthesisyen.ThreeDollarGestureRecognizer;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.yen.androidappthesisyen.R;

import java.util.ArrayList;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ThreeDollarGestureFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ThreeDollarGestureFragment extends Fragment implements DialogInterface.OnClickListener {

//    private OnFragmentInteractionListener mListener;


    // voor ACCEL DATA STREAM
    // TODO zien of alle lidattributen gebruikt worden. Anders verwijderen.
    // Want code is deels copy/paste van PebbleAccelStreamFragment.
    private static final int NUM_SAMPLES = 15;

    private int sampleCount = 0;
    private long lastAverageTime = 0;
    private int[] latest_data;
    private int sampleCounter = 0;
    private int totalData = 0;

    private TextView
            xView,
            yView,
            zView,
            rateView;
    private Button startButton;

    private PebbleKit.PebbleDataReceiver receiver;

    // UUID van originele accel_stream:
//    private UUID uuid = UUID.fromString("2893b0c4-2bca-4c83-a33a-0ef6ba6c8b17");
    private UUID uuid = UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4");

    private Handler handler = new Handler();


    // is voor THREE DOLLAR GESTURE detection
    //Declare Sensor Manager class object

    private SensorManager mSensorManager;

    // Gesture Library and Recognizer
    private GestureLibrary myGestureLibrary = null;
    private GestureRecognizer myGestureRecognizer = null;

    private ArrayList<float[]> recordingGestureTrace = null;
    private String recordingGestureIDString = "default";

    //Next get the handle to the Sensor service
    private Gesture currentGesture = null;

    private boolean RECORD_GESTURE = false;

    // ff public
    public boolean DEBUG = true;
    private boolean VERBOSE = false;


    // dialog view for entering learning gesture

    private AlertDialog learning_dialog = null;
    private View learning_dialog_view = null;


    final Handler alertHandler = new Handler();
    public String detected_gid = "Unknown";
    final Runnable showAlert = new Runnable() {
        public void run() {
            show_alert_box();
        }
    };

    // ff public
    public com.yen.androidappthesisyen.ThreeDollarGestureRecognizer.App.STATES state = com.yen.androidappthesisyen.ThreeDollarGestureRecognizer.App.STATES.STATE_LEARN;


    // private MENUITEMS menuitems;

    public void show_alert_box() {
        /*
         * Shows alert box when gesture recognition thread returns
		 *
		 * also change the statusText back
		 *
		 * also turn acceleration sensor back on
		 */

        // status text
        TextView statusText = (TextView) getView().findViewById(R.id.statusText);
        statusText.setText("Gesture recognition mode");
        Log.w("show_alert_box", "ALLLLLEEEEERRRRRTTTTT");
        // display a dialog

        // stond 'this'
        new AlertDialog.Builder(getActivity())
                .setMessage("Recognized Gesture: " + this.detected_gid)
                .setPositiveButton("OK", null)
                .show();
        // turn on the acc sensor
        mSensorManager.registerListener(sensorListener,
                SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_GAME);

    }


    // TODO lol easy fix maarja...
    @SuppressWarnings("deprecation")
    private final SensorListener sensorListener = new SensorListener() {

        public void onSensorChanged(int sensor, float[] values) {

            // TODO TEST in comments gezet want gebruiken ZELF GEMAAKTE methode die we aanroepen

            /*//Retrieve the values from the float array values which contains sensor data
            Float dataX = values[SensorManager.DATA_X];

            Float dataY = values[SensorManager.DATA_Y];

            Float dataZ = values[SensorManager.DATA_Z];

            //	Context c = getApplicationContext();

            //Now we got the values and we can use it as we want
            if (VERBOSE) {
                Log.w("X - Value, " + dataX, "");

                Log.w("Y - Value, " + dataY, "");

                Log.w("Z - Value, " + dataZ, "");
            }
            TextView tv1 = (TextView) getView().findViewById(R.id.accX);
            TextView tv2 = (TextView) getView().findViewById(R.id.accY);
            TextView tv3 = (TextView) getView().findViewById(R.id.accZ);

            tv1.setText(dataX.toString());
            tv2.setText(dataY.toString());
            tv3.setText(dataZ.toString());

            if (RECORD_GESTURE) {

                float[] traceItem = {dataX.floatValue(),
                        dataY.floatValue(),
                        dataZ.floatValue()};
                if (recordingGestureTrace != null) {
                    recordingGestureTrace.add(traceItem);
                }


            }*/

        }

        public void onAccuracyChanged(int sensor, int accuracy) {
            // TODO Auto-generated method stub

        }
    };


    // TODO TEST zelf gemaakt
    public void sendAccelDataToFragment(float[] values) {

        //Retrieve the values from the float array values which contains sensor data
        // TODO wrom met hoofdletter?
        Float dataX = values[0];

        Float dataY = values[1];

        Float dataZ = values[2];

        //	Context c = getApplicationContext();

        //Now we got the values and we can use it as we want
        if (VERBOSE) {
            Log.w("X - Value, " + dataX, "");

            Log.w("Y - Value, " + dataY, "");

            Log.w("Z - Value, " + dataZ, "");
        }
        TextView tv1 = (TextView) getView().findViewById(R.id.accX);
        TextView tv2 = (TextView) getView().findViewById(R.id.accY);
        TextView tv3 = (TextView) getView().findViewById(R.id.accZ);

        tv1.setText(dataX.toString());
        tv2.setText(dataY.toString());
        tv3.setText(dataZ.toString());

        if (RECORD_GESTURE) {

            float[] traceItem = {dataX.floatValue(),
                    dataY.floatValue(),
                    dataZ.floatValue()};
            if (recordingGestureTrace != null) {
                recordingGestureTrace.add(traceItem);
            }


        }

    }


    private synchronized void startRecordingGesture() {
        /*
		 *  initiate recording of gesture
		 *  usually called after onTouchListener, ACTION_DOWN
		 */
        // recordingGesture = new Gesture();


        if (DEBUG) {
            Log.w("MainActivity", "Starting Gesture Recording");
        }

        recordingGestureTrace = new ArrayList<float[]>(250);

        RECORD_GESTURE = true;
    }

    private synchronized void stopRecordingGesture() {/*
		 *  stop recording of gesture
		 *  usually called after onTouchListener, ACTION_UP / CANCEL
		 */

        TextView statusText = (TextView) getView().findViewById(R.id.statusText);
        if (DEBUG) {
            Log.w("stopRecordingGesture", "Stopping Gesture Recording");
        }

        RECORD_GESTURE = false;
        // Object[] gestureTrace = recordingGestureTrace.toArray();
		/*int numItems = recordingGestureTrace.size();
		// malloc LOL
		float [][] traces = new float [numItems][3];
		// "copy" gesture info to traces!!!*/
        // traces = recordingGestureTrace.toArray(traces);


        // clear the existing trace
        switch (state) {
            case STATE_LEARN:
                // recordingGestureTrace = null;
                // note that the arraylist is being copied
                statusText.setText("Saving gesture to DB...");
                Gesture ng = new Gesture(recordingGestureIDString, new ArrayList<float[]>(recordingGestureTrace));
                // add gesture to library but prepare with recognizer settings first
                myGestureLibrary.addGesture(ng.gestureID, this.myGestureRecognizer.prepare_gesture_for_library(ng), false);
                if (DEBUG)
                    Log.w("stopRecordingGesture", "Recorded Gesture ID " + recordingGestureIDString + " Gesture Trace Length:" + recordingGestureTrace.size());
                statusText.setText("Press button to train gesture.");
                break;
            case STATE_RECOGNIZE:
                statusText.setText("Recognizing gesture...");
                // stop accelerometer
                mSensorManager.unregisterListener(sensorListener);

                // BUGFIX VIA https://code.google.com/p/three-dollar-gesture-recognizer/issues/detail?id=1
                final Gesture candidate = new Gesture(null, new ArrayList<float[]>(recordingGestureTrace));

                // save a reference to activity for this context
                Thread t = new Thread() {
                    public void run() {
                        if (DEBUG)
                            Log.w("stopRecordingGesture-recogThread", "Attempting Gesture Recognition Trace-Length: " + recordingGestureTrace.size());
                        String gid = myGestureRecognizer.recognize_gesture(candidate);
                        if (DEBUG) Log.w("stopRecordingGesture-recogThread", "===== \n" +
                                "Recognized Gesture: " + gid +
                                "\n===");
                        // set gid as currently detected gid
                        detected_gid = gid;

                        // show the alert
                        alertHandler.post(showAlert);

                    }
                };
                t.start();
                if (DEBUG) Log.w("stopRecordingGesture", "STATE_RECOGNIZE --> thread dispatched");
                break;
            case STATE_LIBRARY:
                break;
            default:
                break;
        }
        recordingGestureTrace.clear();

    }


    public ThreeDollarGestureFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(true);


        // voor starten app voor ACCEL DATA STREAM.
        PebbleKit.startAppOnPebble(getActivity(), uuid);


        // voor THREE DOLLAR gestures
        // acc sensor update rate
        /*
		TextView statusText = (TextView) findViewById(R.id.statusText);
		statusText.setText("Press button to train gesture");*/

        // create database test code
        /*Log.w("main", "will create database");
        GestureLibraryDBAdapter db = new GestureLibraryDBAdapter(getApplicationContext());
        Log.w("main", "created database");
        Log.w("main", "test Gesture"+ db.testGesture());*/

        // create gesture library

        myGestureLibrary = new GestureLibrary("GESTURES", getActivity());
        myGestureRecognizer = new gesturerec3d(myGestureLibrary, 50);


        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        mSensorManager.registerListener(sensorListener,
                SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_FASTEST
    	    	/*SensorManager.SENSOR_DELAY_GAME*/);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_three_dollar_gesture, container, false);


        // voor ACCEL DATA STREAM.
        xView = (TextView) returnedView.findViewById(R.id.x_view);
        yView = (TextView) returnedView.findViewById(R.id.y_view);
        zView = (TextView) returnedView.findViewById(R.id.z_view);
        rateView = (TextView) returnedView.findViewById(R.id.rate_view);
        startButton = (Button) returnedView.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PebbleDictionary dict = new PebbleDictionary();
                dict.addInt32(0, 0);
                // TODO stond getApplicationContext() maar is dit beter?
                PebbleKit.sendDataToPebble(getActivity(), uuid, dict);
            }

        });


        // voor THREE DOLLAR gestures
        final Button mainButton = (Button) returnedView.findViewById(R.id.Button01);


        mainButton.setOnTouchListener(new View.OnTouchListener()


                                      {
                                          public boolean onTouch(View v, MotionEvent event) {

                                              if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                                  // button pressed, start recording the trace!
                                                  Log.w("OnTouch", "Down!");
                                                  startRecordingGesture();
                                              } else if (event.getAction() == MotionEvent.ACTION_UP)

                                              {
                                                  Log.w("OnTouch", "Up!");
                                                  stopRecordingGesture();
                                              } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                                                  Log.w("OnTouch", "Cancel!");
                                                  stopRecordingGesture();
                                              }

                                              if (VERBOSE)
                                                  Log.w("Ontouch", "Touched:" + event.getX() + " " + event.getY() + " " + event.getPressure() + " " + event.getAction());


                                              return false;
                                          }
                                      }


        );


        mainButton.setOnClickListener(new View.OnClickListener()

                                      {
                                          public void onClick(View v) {
                                              // clicked
                                              //Log.w("onClick", "Clicked");
    					/*if (mainButton.isFocused())
    					{
    						Log.w("onClick", "InFocus");
    					}
    					else
    					{
    						Log.w("onClic", "NotInFocus");
    					}*/

                                          }


                                      }


        );


        return returnedView;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        /*
		 * android.content.DialogInterface.OnClickListener callback
		 *
		 */
        if (dialog == this.learning_dialog) {
            if (this.learning_dialog_view != null) {
                //this.recordingGestureIDString =
                EditText et = (EditText) learning_dialog_view.findViewById(R.id.EditText01);
                this.recordingGestureIDString = et.getText().toString();
                if (DEBUG)
                    Log.w("onClick", "recordingGestureIDString set to: " + this.recordingGestureIDString);
            }
        }

    }

    public void stateChanged() {
        TextView statusText = (TextView) getView().findViewById(R.id.statusText);

        if (DEBUG) Log.w("stateChanged", "current State is: " + this.state.toString());
        switch (this.state) {
            case STATE_LEARN:
                // show dialog with which the user can enter the gesture id

                if (DEBUG) Log.w("stateChanged", "STATE_LEARN");
                statusText.setText("Press button to train gesture");
                Context ctx = getActivity();
                LayoutInflater li = LayoutInflater.from(getActivity());
                // final so that i can use it in the inner class uargh!

                View dialog = li.inflate(R.layout.learngesturedialog, null);
                if (DEBUG) Log.w("stateChanged", "inflated");
                // Dialog dialog = new Dialog(this);
                // dialog.setContentView(R.layout.learngesturedialog);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                // Prompt Listener
                // PromptListener pl = new PromptListener(dialog);


                builder.setTitle("Enter Gesture ID");
                builder.setView(dialog);
                builder.setPositiveButton("OK", this);
                builder.setNegativeButton("Cancel", this);

                // show the actual dialog
                AlertDialog ad = builder.create();
                this.learning_dialog = ad;
                this.learning_dialog_view = dialog;
                ad.show();

                // this.recordingGestureIDString = pl.getPromptReply();
                // if (DEBUG) Log.w("statechanged","recording gesture id now changed to:" + this.recordingGestureIDString);


                // this.recordingGestureIDString = Alerts.showPrompt("Please Enter Gesture ID", this);


                break;
            case STATE_RECOGNIZE:
                statusText.setText("Gesture recognition mode");
                break;
            case STATE_LIBRARY:
                break;
            default:
                break;
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) {
            Log.w("onActivityResult", "Code: " + requestCode + " resultCode " + resultCode/*+ " data " + data.getDataString()*/);
        }
        // change state and map to enum value
        this.state = com.yen.androidappthesisyen.ThreeDollarGestureRecognizer.App.STATES.values()[resultCode];
        //update activitie's state
        this.stateChanged();
    }

    @Override
    public void onResume() {
        super.onResume();


        // voor (o.a.) PEBBLE ACCEL STREAM
        getToggleStatesAndEnableServices();


        // voor THREE DOLLAR gestures
        mSensorManager.registerListener(sensorListener,
                SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_GAME);
    }


    private void getToggleStatesAndEnableServices() {

        // TODO zien hoe je dus kunt communiceren MET ANDERE FRAGMENTS...
//        ToggleButton togglePebbleStream = (ToggleButton) getView().findViewById(R.id.toggle_pebble_acceldata_stream);
//        if (togglePebbleStream.isEnabled() && togglePebbleStream.isChecked()) {
        startPebbleDataStream();
//        }

    }

    private void startPebbleDataStream() {


        if (receiver == null) {


//            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
//            // Without "getResources()." it also seems to work, but better to USE IT!
//            outputWindow.append("--- " + getResources().getString(R.string.start_communication_test) + " ---" + "\n");
//            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            receiver = new PebbleKit.PebbleDataReceiver(uuid) {

                @Override
                public void receiveData(Context context, int transactionId, PebbleDictionary data) {

                    // stond getApplicationContext()
                    PebbleKit.sendAckToPebble(getActivity(), transactionId);

                    //Count total data
                    totalData += 3 * NUM_SAMPLES * 4; // TODO betekenis? 3 wegens XYZ, en 4 wegens 4-byte int?

                    //Get data
                    latest_data = new int[3 * NUM_SAMPLES];
//                    Log.w(LOG_TAG, "NEW DATA PACKET");
                    for (int i = 0; i < NUM_SAMPLES; i++) {
                        for (int j = 0; j < 3; j++) {
                            try {
                                latest_data[(3 * i) + j] = data.getInteger((3 * i) + j).intValue();
                            } catch (Exception e) {
                                latest_data[(3 * i) + j] = -1;
                            }
                        }
//                        Log.w(LOG_TAG, "Sample " + i + " data: X: " + latest_data[(3 * i)] + ", Y: " + latest_data[(3 * i) + 1] + ", Z: " + latest_data[(3 * i) + 2]);
                    }


                    // ------ TEST - voor THREE DOLLAR gesture detection
// TODO we werken nu met INTs maar de gesture recognizer werkt eigenlijk met FLOATs
//                    is het nuttig om met FLOATs te werken? to test...
                    float[] floatArray = {latest_data[0], latest_data[1], latest_data[2]};
                    sendAccelDataToFragment(floatArray);

                    // ------ TEST - voor THREE DOLLAR gesture detection



                    /*// ------ TEST - voor MOEILIJKERE gesture detection

                    // TODO we werken nu met INTs maar de gesture recognizer werkt eigenlijk met FLOATs
                    // is het nuttig om met FLOATs te werken? to test...
                    float[] floatArray = {latest_data[0], latest_data[1], latest_data[2]};

                    try {
                        Log.w("CHECK", "---------- net voor aanroep giveNewAccelDataToService");
                        // TODO ZIEN WANNEER recognitionService wordt aangemaakt/gecalled!
                        recognitionService.giveNewAccelDataToService(floatArray);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    // ------ TEST - voor MOEILIJKERE gesture detection*/


                    /*// ------ TEST - voor SIMPELE gesture detection
                    // TODO plek van deze code is veranderen!
                    // TODO alsook de para's
                    PebbleAccelPacket pebbleAccelPacket = new PebbleAccelPacket(latest_data[0], latest_data[1], latest_data[2]);
                    test.update(pebbleAccelPacket);

                    // ------ TEST - voor SIMPELE gesture detection*/


                    //Show
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            xView.setText("X: " + latest_data[0]);
                            yView.setText("Y: " + latest_data[1]);
                            zView.setText("Z: " + latest_data[2]);
                        }

                    });

                    //Show on graph
                    for (int i = 0; i < NUM_SAMPLES; i++) {

                        sampleCounter++;
                    }

                    if (System.currentTimeMillis() - lastAverageTime > 1000) {
                        lastAverageTime = System.currentTimeMillis();

                        rateView.setText("" + sampleCount + " samples per second."
                                + "\n"
                                + data.size() + " * 4-btye int * " + sampleCount + " samples = " + (4 * data.size() * sampleCount) + " Bps."
                                + "\n"
                                + "Total data received: " + getTotalDataString());
                        sampleCount = 0;
                    } else {
                        sampleCount++;
                    }
                }

            };

            PebbleKit.registerReceivedDataHandler(getActivity(), receiver);


        }

        // TODO of beter IN if lus?
        PebbleKit.startAppOnPebble(getActivity(), uuid);

    }


    @Override
    public void onPause() {
        super.onPause();


        // voor PEBBLE ACCEL STREAM
        disableAllServices();
    }


    private void disableAllServices() {
        stopPebbleDataStream();
        // TODO voeg stop-methods van andere services toe.
    }

    private void stopPebbleDataStream() {

        if (receiver != null) {


//            final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_output_window);
//            outputWindow.append("--- " + getResources().getString(R.string.stop_communication_test) + " ---" + "\n");
//            ((ScrollView) getView().findViewById(R.id.scrollView_output_window)).fullScroll(View.FOCUS_DOWN);


            // TODO zien of deze try/catch werkt voor fixen: Caused by: java.lang.IllegalArgumentException: Receiver not registered: com.yen.myfirstapp.MainActivity$1@40fc03c0
            try {

//                unregisterReceiver(receiver);
                // Changed to following since we are in a FRAGMENT; not an ACTIVITY.
                getActivity().unregisterReceiver(receiver);


                // TODO we gebruiken nu dit omdat anders de IF(... == NULL) soms te WEINIG wordt binnengegaan!
                receiver = null;


            } catch (IllegalArgumentException ex) {


                // TODO niets doen gewoon?


                // TODO we gebruiken nu dit omdat anders de IF(... == NULL) soms te WEINIG wordt binnengegaan!
                receiver = null;
            }


        }

        // TODO of IN de if lus? Maar mag erbuiten.
        PebbleKit.closeAppOnPebble(getActivity(), uuid);

    }


    private String getTotalDataString() {
        if (totalData < 1000) {
            return "" + totalData + " Bytes.";
        } else if (totalData > 1000 && totalData < 1000000) {
            return "" + totalData / 1000 + " KBytes.";
        } else {
            return "" + totalData / 1000000 + " MBytes.";
        }
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
//        super.onDestroy();

        if (DEBUG) Log.w("onDestroy", "killing this here app!");
        mSensorManager.unregisterListener(sensorListener);
        // this.myGestureLibrary.onApplicationStop();
        super.onDestroy();
    }

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }

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
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        public void onFragmentInteraction(Uri uri);
//    }

}
