package com.yen.androidappthesisyen.advancedrecognizer;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.simplerecognizer.PebbleGestureModel;
import com.yen.androidappthesisyen.simplerecognizer.TiltGestureRecognizer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AdvancedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class AdvancedFragment extends Fragment implements DialogInterface.OnClickListener {


    //    private static final String LOG_TAG = AdvancedFragment.class.getName();
    // was relatief lang dus nu:
    private static final String LOG_TAG = "ADVANCED RECOGNIZER";

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
    private ToggleButton toggleAccelStream;
    private CheckBox checkboxGestureSpotting;

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
    // voor TILT GESTURE RECOGNITION
    private TiltGestureRecognizer theTiltGestureRecognizer = null;

    private ArrayList<float[]> recordingGestureTrace = null;
    private String recordingGestureIDString = "default";

    //Next get the handle to the Sensor service
    private Gesture currentGesture = null;

    private boolean RECORD_GESTURE = false;

    // ff public
    public boolean DEBUG = false; // stond default op TRUE
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
    public com.yen.androidappthesisyen.advancedrecognizer.App.STATES state = com.yen.androidappthesisyen.advancedrecognizer.App.STATES.STATE_LEARN;


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

        // TODO dit dus disablen want niet nodig maar TEST OF HET NIET BREAKT.
        // turn on the acc sensor
//        mSensorManager.registerListener(sensorListener, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);

    }


    // TODO niet meer nodig wrsl.
    // TODO lol easy fix maarja...
//    @SuppressWarnings("deprecation")
//    private final SensorListener sensorListener = new SensorListener() {
//
//        public void onSensorChanged(int sensor, float[] values) {
//
//            // TODO TEST in comments gezet want gebruiken ZELF GEMAAKTE methode die we aanroepen
//
//            /*//Retrieve the values from the float array values which contains sensor data
//            Float dataX = values[SensorManager.DATA_X];
//
//            Float dataY = values[SensorManager.DATA_Y];
//
//            Float dataZ = values[SensorManager.DATA_Z];
//
//            //	Context c = getApplicationContext();
//
//            //Now we got the values and we can use it as we want
//            if (VERBOSE) {
//                Log.w("X - Value, " + dataX, "");
//
//                Log.w("Y - Value, " + dataY, "");
//
//                Log.w("Z - Value, " + dataZ, "");
//            }
//            TextView tv1 = (TextView) getView().findViewById(R.id.accX);
//            TextView tv2 = (TextView) getView().findViewById(R.id.accY);
//            TextView tv3 = (TextView) getView().findViewById(R.id.accZ);
//
//            tv1.setText(dataX.toString());
//            tv2.setText(dataY.toString());
//            tv3.setText(dataZ.toString());
//
//            if (RECORD_GESTURE) {
//
//                float[] traceItem = {dataX.floatValue(),
//                        dataY.floatValue(),
//                        dataZ.floatValue()};
//                if (recordingGestureTrace != null) {
//                    recordingGestureTrace.add(traceItem);
//                }
//
//
//            }*/
//
//        }
//
//        public void onAccuracyChanged(int sensor, int accuracy) {
//
//        }
//
//    };


    // TODO TEST zelf gemaakt
    public void sendAccelDataToFragment(float[] values) {

        //Retrieve the values from the float array values which contains sensor data
        // TODO wrom met hoofdletter? is een wrapper class voor de primitieve klasse float.
        // MAAR verderop gebruik je precies toch gewone float dus mag het hier ook gewone float zijn? TO TEST.
        Float dataX = values[0];

        Float dataY = values[1];

        Float dataZ = values[2];

        //	Context c = getApplicationContext();



        // TODO mag weg:
//        TextView tv1 = (TextView) getView().findViewById(R.id.accX);
//        TextView tv2 = (TextView) getView().findViewById(R.id.accY);
//        TextView tv3 = (TextView) getView().findViewById(R.id.accZ);
//
//        tv1.setText(dataX.toString());
//        tv2.setText(dataY.toString());
//        tv3.setText(dataZ.toString());


        // ------
        // hier bepalen welke data behoort tot een gesture en welke niet.
        // door vinden start en stop via checken van thresholds ofzo.
        // en dan moet ergens startRecordingGesture() aangeroepen woren wanneer zo'n start werd gevonden, en stop... bij vinden van een stop.


        // Boolean useGestureSpotting = false; // ==================================================================

        SharedPreferences settings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.gesture_spotting", Context.MODE_PRIVATE);
        Boolean useGestureSpotting = settings.getBoolean("use", true);


        if (useGestureSpotting) {
            findBoundariesGesture(values);
        } else {


            // ======================================================================================================================================================
            // TODO hierboven wordt alle gekregen accel data gewoon getoond.
            // en HIERONDER wordt in IF lus de gekregen accel data bepaald die in een TRACE moet om een GESTURE te vormen.
            // dus wrsl aanpassen van CODE DAT BOOLEAN RECORD_GESTURE aanpast wat dat moet nu gebeuren automatisch (in RECOGNIZE phase tenminste).
            if (RECORD_GESTURE) {

                float[] traceItem = {dataX.floatValue(),
                        dataY.floatValue(),
                        dataZ.floatValue()};
                if (recordingGestureTrace != null) {
                    recordingGestureTrace.add(traceItem);
                }

            }

        }


    }


    // TODO verzet deze blok code terug naar boven wann het is gefinetuned.
    // for AUTOMATIC detection boundaries gestures (= GESTURE SPOTTING):
    // TODO hernoem naar iets duidelijker:
    public enum RecordMode {
        MOTION_DETECTION, PUSH_TO_GESTURE
    }

    RecordMode recordMode = RecordMode.MOTION_DETECTION;
    boolean isRecording = false;
    ArrayList<float[]> gestureValues = new ArrayList<float[]>();
    float THRESHOLD = 1050; // default 2
    int stepsSinceNoMovement; // TODO default op 0 zetten of niet?
    final int MIN_GESTURE_SIZE = 5; // default 8
    final int MIN_STEPS_SINCE_NO_MOVEMENT = 8; // default 10


    private void findBoundariesGesture(float[] values) {


//        float[] values = { sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2] };

        Log.w(LOG_TAG, "X " + values[0] + " Y " + values[1] + " Z " + values[2]);


        switch (recordMode) {
            case MOTION_DETECTION: // WE KOMEN ALTIJD HIER. IS DIT GOED? IS recordmode overbodig want de originele app past dat precies ook niet meer toe?

                // Log.w(LOG_TAG, "============================ arrived in CASE MOTION_DETECTION");

                if (isRecording) { // TODO initializen maar WAAR? (UPDATE: op FALSE in begin) of niet nodig deze if check?

                    gestureValues.add(values);
                    if (calcVectorNorm(values) < THRESHOLD) {

                        Log.w(LOG_TAG, "========================= gesture NIET meer gedetecteerd ============= stepsSinceNoMovement++");
                        stepsSinceNoMovement++;
                    } else {

                        Log.w(LOG_TAG, "===================== NOG STEEDS IN GESTURE ======= stepsSinceNoMovement = 0");
                        stepsSinceNoMovement = 0;
                    }

                } else if (calcVectorNorm(values) >= THRESHOLD) { // TODO een andere threshold hiervoor nemen dan diegene dat hierboven wordt gebruikt?

                    Log.w(LOG_TAG, "========================================================== STARTEN met recorden gesture");

                    isRecording = true;
                    stepsSinceNoMovement = 0;
                    gestureValues = new ArrayList<float[]>();
                    gestureValues.add(values);
                }


                if (stepsSinceNoMovement == MIN_STEPS_SINCE_NO_MOVEMENT) { // TODO =========================================================================================== iets anders dan 10 nemen? ofja 10 wrsl goed genoeg: als te hoog is moet de user te lang stilstaan met Pebble.

                    Log.w(LOG_TAG, "============================ detectie MOGELIJKE gesture ");

                    int length = gestureValues.size() - MIN_STEPS_SINCE_NO_MOVEMENT;
                    if (length > MIN_GESTURE_SIZE) { // TODO ====================================================================== MIN_GESTURE_SIZE wrsl verhogen?
//                         listener.onGestureRecorded(gestureValues.subList(0, gestureValues.size() - MIN_STEPS_SINCE_NO_MOVEMENT));
                        // FYI ArrayList<float[]> gestureValues;

                        Log.w(LOG_TAG, "============= het was IDD een gesture =============== GESTOPT MET RECORDEN GESTURE EN DE GESTURE SIZE IS GROTER DAN MINIMUM. size = " + length);
                        // TODO de gesture is nu gedaan en de geldige waardes zitten in index 0 tem index gestureValues.size() - MIN_STEPS_SINCE_NO_MOVEMENT


                        recordingGestureTrace = new ArrayList<float[]>(250);
                        // opvullen met de juiste accel data.
                        // maken kopie van subList van subList geeft VIEW terug en niet iets dat je kan casten naar ArrayList.
                        recordingGestureTrace = new ArrayList<>(gestureValues.subList(0, gestureValues.size() - MIN_STEPS_SINCE_NO_MOVEMENT));


                        doRemainingTasksAfterRecording();

                    }

                    Log.w(LOG_TAG, "============================ starten TERUG VAN NUL.");
                    gestureValues = new ArrayList<float[]>(); // TODO stond eerst = null; maar dit wrsl veiliger?
                    stepsSinceNoMovement = 0;
                    isRecording = false;
                }
                break;


            case PUSH_TO_GESTURE: // TODO is dit nu nodig of niet? wrsl niet?

                // Log.w(LOG_TAG, "============================ arrived in CASE PUSH_TO_GESTURE");

                if (isRecording) {
                    gestureValues.add(values);
                }
                break;
        }

    }

    private float calcVectorNorm(float[] values) {
        // TODO wijzig de deprecated code. UPDATE DONE.
        float norm = (float) Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]) - 9.9f;
        return norm;
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


        Boolean nieuwSysteem = true;


        /*if (nieuwSysteem){*/
        doRemainingTasksAfterRecording();
        /*} else {
            TextView statusText = (TextView) getView().findViewById(R.id.statusText);

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

                    // TODO mag weg?
                    // stop accelerometer
                    mSensorManager.unregisterListener(sensorListener);

                    // BUGFIX VIA https://code.google.com/p/three-dollar-gesture-recognizer/issues/detail?id=1
                    final Gesture candidate = new Gesture(null, new ArrayList<float[]>(recordingGestureTrace));


                    // save a reference to activity for this context
                    Thread t = new Thread() {
                        public void run() {

                            if (DEBUG)
                                Log.w("stopRecGest-recogThread", "Attempting Gesture Recognition Trace-Length: " + recordingGestureTrace.size());
                            String gid = myGestureRecognizer.recognize_gesture(candidate);
                            if (DEBUG)
                                Log.w("stopRecGest-recogThread", "===== \n" + "Recognized Gesture: " + gid + "\n===");
                            // set gid as currently detected gid
                            detected_gid = gid;


                            // show the alert
                            alertHandler.post(showAlert); // showAlert is een RUNNABLE met daarin een RUN methode.


                            // ---- START EIGEN TOEVOEGING
                            // vóór showAlert gedaan want netwerktaken vragen toch wat tijd.
                            // UPDATE: toch showAlert eerst want als geen internet, wordt alertbox pas getoond NA OVERSCHREIDEN TIME-OUT.


                            ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                            if (networkInfo != null && networkInfo.isConnected()) {

                                // fetch data


                                SharedPreferences settings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.gesture_handler", Context.MODE_PRIVATE);
                                String IPAddress = settings.getString("ip_address", "192.168.1.1"); // OF HIER dus checken of er al waarde is: INDIEN NIET: TOON DIALOOG VENSTER.
                                Log.w(LOG_TAG, "saved IP is " + IPAddress);

                                // TODO met of zonder slash?
                                String stringURL = "http://" + IPAddress + ":8080/RESTWithJAXB/rest/handlegesture/invoer";
                                // TODO KAN DIE NIET ALTIJD WIJZIGEN DUS VIA DIALOOGVENSTER AAN USER VRAGEN?


                                // TODO TIJDELIJK GEEN ASYNCTASK GEBRUIKT OMDAT GAF: Can't create handler inside thread that has not called Looper.prepare(
//                            new AsyncPOSTGestureToServer().execute(stringURL, gid);


                                // EEEEEEEEEEERST NOG IS DIT MAAR MET ENGELSE GESTURETERMEN TESTEN; DAARNA HET ANDERE.
                                // UPDATE: EIGEN CODE WERKT NU :D
                                int httpResult = POSTGestureToServer(stringURL, gid);
                                // DIT DUS NIET MEER NODIG:
//                            nieuweTestcode(stringURL, gid);


                            } else {
                                // Arriving here if no internet connection.

                            }


                            // ---- STOP EIGEN TOEVOEGING




                        }
                    };
                    t.start();

                    // TEST LOCATIE ASYNCTASK
                    // UPDATE: werkt wrsl niet OMDAT DETECTED_GID hier nog de VORIGE waarde bevat
                    // omdat de THREAD nog aan het runnen is terwijl men HIER komt!
//                Log.w(LOG_TAG, "DETECTED GID: " + detected_gid);
//                new AsyncPOSTGestureToServer().execute(stringURL, detected_gid);


                    if (DEBUG) Log.w("stopRecordingGesture", "STATE_RECOGNIZE --> thread dispatched");
                    break;



                case STATE_LIBRARY:
                    break;
                default:
                    break;
            }
            recordingGestureTrace.clear();


        }*/


    }

    private void doRemainingTasksAfterRecording() {


        TextView statusText = (TextView) getView().findViewById(R.id.statusText);

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

                // TODO mag weg?
                // stop accelerometer
//                mSensorManager.unregisterListener(sensorListener);

                // BUGFIX VIA https://code.google.com/p/three-dollar-gesture-recognizer/issues/detail?id=1
                final Gesture candidate = new Gesture(null, new ArrayList<float[]>(recordingGestureTrace));


                // save a reference to activity for this context
                Thread t = new Thread() {
                    public void run() {

                        if (DEBUG)
                            Log.w("stopRecGest-recogThread", "Attempting Gesture Recognition Trace-Length: " + recordingGestureTrace.size());
                        String gid = myGestureRecognizer.recognize_gesture(candidate);
                        if (DEBUG)
                            Log.w("stopRecGest-recogThread", "===== \n" + "Recognized Gesture: " + gid + "\n===");
                        // set gid as currently detected gid
                        detected_gid = gid;


                        // TODO eventueel gebruiken: if (!detected_gid.equalsIgnoreCase("unknown") && !detected_gid.equalsIgnoreCase("unknown gesture")) {
                        // Updating Gestures ScrollView
                        final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_gestures);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                outputWindow.append(detected_gid + "\n");
                                ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);

                            }
                        });




                        // TODO "  && !detected_gid.equalsIgnoreCase("not recognized!") " ook in IF steken?
                        // of dat net niet om te zien wann er NOG GEEN GESTURES in DB zitten?
                        // maar dat wrsl op betere manier tonen dan via dialog na uitvoeren gebaar?
                        // show the alert
                        /*if (!detected_gid.equalsIgnoreCase("unknown") && !detected_gid.equalsIgnoreCase("unknown gesture")) {
                            alertHandler.post(showAlert); // showAlert is een RUNNABLE met daarin een RUN methode.
                        }*/


                        if (detected_gid.equalsIgnoreCase("unknown") || detected_gid.equalsIgnoreCase("unknown gesture")) { // TODO nodig of niet? && gid.equalsIgnoreCase("unknown gesture")

                            // Niet nodig. En hebben gemerkt dat de nu nog te krijgen "unknown gesture" berichten mogen genegeerd worden.
                            // Het is precies niet meer het geval dat we een gesture uitvoeren en dat die niet wordt herkend.
                            // doTwoShortPebbleVibrations();

                        } else {


                            sendGestureIfMatchFound(detected_gid);


                        }


                    }
                };
                t.start();

                // TEST LOCATIE ASYNCTASK
                // UPDATE: werkt wrsl niet OMDAT DETECTED_GID hier nog de VORIGE waarde bevat
                // omdat de THREAD nog aan het runnen is terwijl men HIER komt!
//                Log.w(LOG_TAG, "DETECTED GID: " + detected_gid);
//                new AsyncPOSTGestureToServer().execute(stringURL, detected_gid);


                if (DEBUG) Log.w("stopRecordingGesture", "STATE_RECOGNIZE --> thread dispatched");
                break;


            case STATE_LIBRARY:
                break;
            default:
                break;
        }
        recordingGestureTrace.clear();

    }

    public void sendGestureIfMatchFound(final String recognizedGesture) {

        Log.w(LOG_TAG, "START of sendGestureIfMatchFound - GESTURE " + recognizedGesture);

        // Check for which system the accel data stream is currently running. This should always be only 1 system at a time because the user's face can only get detected by 1 system at the time.
        SharedPreferences systemIDsettings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.system_id_accel_stream", Context.MODE_PRIVATE);
        String enabledSystem = systemIDsettings.getString("enabledSystem", "none");
        Log.w(LOG_TAG, "enabledSystem " + enabledSystem);

        // Get the list of systems which support the recognized gesture.
        Map<String, String> savedMap = getMapSupportedGestures();
        ArrayList<String> supportedSystems = new ArrayList<String>();

        for (Map.Entry<String, String> entry : savedMap.entrySet()) {
            String key = entry.getKey(); // key = systemID
            String value = entry.getValue(); // value = supported gestures
            String[] arrayGestures = value.split(";");

            for (String gesture : arrayGestures) {
                if (gesture.equalsIgnoreCase(recognizedGesture)) {
                    supportedSystems.add(key);
                }
            }
        }


        Boolean isMatchFound = false;
        if (supportedSystems.contains(enabledSystem)) {
            isMatchFound = true;
        }
        Log.w(LOG_TAG, "isMatchFound " + isMatchFound);


        if (isMatchFound) {


            // Sending feedback to the user:
            // If a gesture detected + supported by the currently used system: do 1x short vibration.
            // This detected gesture can still be a false positive, though.
            doShortPebbleVibration();


            // ---- START EIGEN TOEVOEGING
            // vóór showAlert gedaan want netwerktaken vragen toch wat tijd.
            // UPDATE: toch showAlert eerst want als geen internet, wordt alertbox pas getoond NA OVERSCHREIDEN TIME-OUT.


            ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {

                // fetch data

                // TODO je moet op basis van de systemID de overeenkomstige enumerator weten, zodat je hier de enumerator kunt concateren.
                // momenteel hardcodeer je nog de link tussen de systemID (tot nu toe enkel yen-asus en yen-medion) naar de enumerators (tot nu toe enkel 1 en 2)
                // OFWEL direct een mapping tussen systemID en IP adres gesture handler opslaan, maar WRSL BEST MET ENUMERATOR WERKEN OMDAT JE OVERAL IN JE SHAREDPREF ZO WERKT!
                SharedPreferences gestureHandlersettings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.gesture_handler", Context.MODE_PRIVATE);
                String IPAddress = "192.168.1.1";
                if (enabledSystem.equalsIgnoreCase("yen-asus")) {
                    // TODO momenteel nog hardgecodeerde mapping yen-asus naar enum 1
                    IPAddress = gestureHandlersettings.getString("ip_address_1", "192.168.1.1"); // OF HIER dus checken of er al waarde is: INDIEN NIET: TOON DIALOOG VENSTER.
                } else if (enabledSystem.equalsIgnoreCase("yen-medion")) {
                    // TODO momenteel nog hardgecodeerde mapping yen-medion naar enum 2
                    IPAddress = gestureHandlersettings.getString("ip_address_2", "192.168.1.1"); // OF HIER dus checken of er al waarde is: INDIEN NIET: TOON DIALOOG VENSTER.
                } else {
                    Log.w("pushnotificationlistener", "SystemID unknown so no mapped IP address for Gesture Handler found");
                }


                Log.w(LOG_TAG, "saved IP is " + IPAddress);

                // TODO met of zonder slash?
                final String stringURL = "http://" + IPAddress + ":8080/RESTWithJAXB/rest/handlegesture/invoer";
                // TODO KAN DIE NIET ALTIJD WIJZIGEN DUS VIA DIALOOGVENSTER AAN USER VRAGEN?


                // TODO TIJDELIJK GEEN ASYNCTASK GEBRUIKT OMDAT GAF: Can't create handler inside thread that has not called Looper.prepare(
//                            new AsyncPOSTGestureToServer().execute(stringURL, gid);


                // Network actions not allowed on main thread (= UI thread) so starting a new thread.
                Thread t = new Thread() {
                    public void run() {

                        // EEEEEEEEEEERST NOG IS DIT MAAR MET ENGELSE GESTURETERMEN TESTEN; DAARNA HET ANDERE.
                        // UPDATE: EIGEN CODE WERKT NU :D
                        int httpResult = POSTGestureToServer(stringURL, recognizedGesture);

                    }
                };
                t.start();



                // DIT DUS NIET MEER NODIG:
//                            nieuweTestcode(stringURL, gid);



            } else {
                // Arriving here if no internet connection.

            }


            // ---- STOP EIGEN TOEVOEGING


        } else {
            // Do nothing.
        }


        Log.w(LOG_TAG, "END of sendGestureIfMatchFound");

    }



    private void doShortPebbleVibration() {
        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(3, 0);
        PebbleKit.sendDataToPebble(getActivity(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);
    }



    private void doTwoShortPebbleVibrations() {
        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(4, 0);
        PebbleKit.sendDataToPebble(getActivity(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);
    }




    // TODO mag weg want nooit gebruikt of?
    private void nieuweTestcode(String stringURL, String stringGesture) {


        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
        HttpResponse response;
        JSONObject json = new JSONObject();

        try {
            HttpPost post = new HttpPost(stringURL);
            json.put("gesture", stringGesture);
//            json.put("emailid", email);
            Log.w("JSON NAAR STRING", json.toString());
            StringEntity se = new StringEntity(json.toString());
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);
            response = client.execute(post);

        /*Checking response */
            if (response != null) {
                InputStream in = response.getEntity().getContent(); //Get the data in the entity
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.v("Error", "Cannot Establish Connection");
        }

    }

    // TODO mag weg want nooit gebruikt of?
    private class AsyncPOSTGestureToServer extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {


            int httpResult = POSTGestureToServer(params[0], params[1]);


            return httpResult;
        }


        @Override
        protected void onPostExecute(Integer httpResult) {
//            super.onPostExecute(httpResult);

            // TODO DIALOG OF TOAST OFZO TONEN ALS HET FOUTLIEP
            // TODO OOK TELKENS MELDEN ALS GOED GING?

        }
    }


    private int POSTGestureToServer(String stringURL, String stringGesture) {




        HttpURLConnection httpcon = null;
        int httpResult = -1; // initial value

        try {

//Connect
            httpcon = (HttpURLConnection) ((new URL(stringURL).openConnection()));
            // TODO nodig? Best enablen zeker.
            httpcon.setDoInput(true); // Sets the flag indicating whether this URLConnection allows input. It cannot be set after the connection is established.
            httpcon.setDoOutput(true);
            httpcon.setRequestProperty("Content-Type", "application/json");
            // TODO nodig? UPDATE: JA voor mogelijke problemen te voorkomen.
            httpcon.setRequestProperty("charset", "utf-8");
            // TODO dit enablen ALS JSON ONTVANGEN! ipv gewoon een code?
//            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("POST");
            httpcon.setUseCaches(false);
            // TODO EVENTUEEL MEE SPELEN:
            httpcon.setConnectTimeout(30000); // stond op 10000
            httpcon.setReadTimeout(30000); // stond op 10000


            // TODO is testen: (dan moet JSON object hier gemaakt worden ipv verderop)
            // REDEN: http://www.evanjbrunner.info/posts/json-requests-with-httpurlconnection-in-android/
//            String message = new JSONObject().toString();
//            conn.setFixedLengthStreamingMode(message.getBytes().length)

            httpcon.connect();

            Log.w(LOG_TAG, "arrived after .connect()");

            // JSON
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("gesture", stringGesture);
            // TODO NOG ANDERE DATA STUREN?
//            jsonObject.put("description", "Real");
//            jsonObject.put("enable", "true");


//Write
            OutputStream os = httpcon.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(jsonObject.toString());
            // TODO TEST OF NODIG:
//            OS IPV WRITER.write(jsonObject.toString().getBytes("UTF-8"));
            writer.close(); // Closes this writer. The contents of the buffer are flushed, the target writer is closed, and the buffer is released. Only the first invocation of close has any effect.
            os.close();


            // -------------- EVEN UITGEZET OMDAT PAS REPONSE KRIJGT ALS JE HET C GUI WINDOW SLUIT.

            httpResult = httpcon.getResponseCode();
            if (httpResult == HttpURLConnection.HTTP_OK) { // Numeric status code, 200: OK


                //Read
                BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));


                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    // TODO test: andere tut had: sb.append(line + "\n");
                }

                br.close();
                String resultString = sb.toString();
                System.out.println(" ------- RESULT STRING: " + resultString);

            } else {
                // TODO eventueel:
//            } else if (statusCode != HttpURLConnection.HTTP_OK) {
//                // handle any other errors, like 404, 500,..
//            }
                // TODO bijvoorbeeld: throw new RuntimeException

                System.out.println("RESPONSE WAS NOT CODE HTTP_OK: " + httpcon.getResponseMessage());
                // TODO toast?
            }


            // TODO  test of nodig:
//            httpcon.disconnect();


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {

            if (httpcon != null) {
                httpcon.disconnect();
            }

        }

        return httpResult;

    }


    public AdvancedFragment() {
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


        // TODO je hebt hier TRY CATCH rond gezet want gaf error over SQL en close(): to fix.
        try {
            myGestureLibrary = new GestureLibrary("GESTURES", getActivity());
        } catch (Exception ex) {

        }


        myGestureRecognizer = new gesturerec3d(myGestureLibrary, 50);

        /**
         * Object that manages Pebble wrist movement gestures.
         * The user should extend their wrist with the fist pointing directly out from the chest as if to punch, with the watch's face pointing upwards
         *
         * @param threshold            Value from 0 to 4000 above which an action on an axis will be triggered
         * @param durationMilliseconds Minimum time between gestures in milliseconds
         * @param modeConstant         Mode constant from this class for FLICK or TILT operation
         */
        // TODO finetuning
        theTiltGestureRecognizer = new TiltGestureRecognizer(this, 700, 1000, PebbleGestureModel.MODE_TILT);



        // TODO mag weg maar test of NIETS BREAKT.
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        // TODO mag weg maar test of NIETS BREAKT.
//        mSensorManager.registerListener(sensorListener, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST/*SensorManager.SENSOR_DELAY_GAME*/);

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
        toggleAccelStream = (ToggleButton) returnedView.findViewById(R.id.toggle_accel_stream);
        toggleAccelStream.setChecked(false);
        toggleAccelStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (toggleAccelStream.isChecked()) {
                    Log.w(LOG_TAG, "CALLING enableAccelStream");
                    enableAccelStream();

                } else {
                    Log.w(LOG_TAG, "CALLING disableAccelStream");
                    disableAccelStream();
                }

            }
        });


        checkboxGestureSpotting = (CheckBox) returnedView.findViewById(R.id.checkBoxGestureSpotting);
        SharedPreferences gestureSpottingSettings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.gesture_spotting", Context.MODE_PRIVATE);
        Boolean useGestureSpotting = gestureSpottingSettings.getBoolean("use", true);
        checkboxGestureSpotting.setChecked(useGestureSpotting);
        checkboxGestureSpotting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences.Editor editor = getActivity().getSharedPreferences("com.yen.androidappthesisyen.gesture_spotting", Context.MODE_PRIVATE).edit();
                editor.putBoolean("use", checkboxGestureSpotting.isChecked());
                editor.commit();
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
    						Log.w("onClick", "NotInFocus");
    					}*/

                                          }


                                      }


        );


        // We also place it here, since in case there is already text in the ScrollView when arriving there, we immediately scroll.
        ((ScrollView) returnedView.findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);





        return returnedView;
    }


    // ----------- KOPIE OOK TE VINDEN IN MQTTSERVICE.JAVA DUS VOER DAAR OOK WIJZIGINGEN DOOR.
    private void enableAccelStream() {

        String systemID = "triggered-by-user";
        SharedPreferences accelStreamSettings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.system_id_accel_stream", Context.MODE_PRIVATE);
        SharedPreferences.Editor accelStreamEditor = accelStreamSettings.edit();
        accelStreamEditor.putString("enabledSystem", systemID);
        accelStreamEditor.commit();

        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(1, 0); // key = 1 = TRUE = start stream, value = 0
        PebbleKit.sendDataToPebble(getActivity(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);
    }

    private void disableAccelStream() {

        String systemID = "triggered-by-user";
        SharedPreferences accelStreamSettings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.system_id_accel_stream", Context.MODE_PRIVATE);
        SharedPreferences.Editor accelStreamEditor = accelStreamSettings.edit();
        accelStreamEditor.putString("enabledSystem", systemID);
        accelStreamEditor.commit();

        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(0, 0); // key = 0 = FALSE = stop stream, value = 0
        PebbleKit.sendDataToPebble(getActivity(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);
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
        this.state = com.yen.androidappthesisyen.advancedrecognizer.App.STATES.values()[resultCode];
        //update activity's state
        this.stateChanged();
    }


    @Override
    public void onResume() {
        super.onResume();


        // voor (o.a.) PEBBLE ACCEL STREAM
        getToggleStatesAndEnableServices();

        // TODO mag dus weg maar check dat NIETS BREAKT.
        // voor THREE DOLLAR gestures
//        mSensorManager.registerListener(sensorListener, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
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



                    // We only allow tilt gesture detection in the RECOGNIZE PHASE. We don't need it in the LEARN PHASE since the system doesn't need to learn the up/down/left/right tilt gestures.
                    // Since there is no clear difference between different persons exercising these 4 gestures.
                    Boolean[] results = {false, false};
                    if(state == App.STATES.STATE_RECOGNIZE){

                        // TILT GESTURE DETECTION
                        // TODO is testen met float[] ipv int[]. want andere recognizer past float toe.
                        // UPDATE: maar hierboven is de data vanuit een int[] gehaald dusja.
                        int[] intArray = {latest_data[0], latest_data[1], latest_data[2]};
                        results = theTiltGestureRecognizer.update(intArray);

                    } else {
                        // We are in the LEARN STATE or LIBRARY STATE

                        results[0] = false;
                        results[1] = false;
                    }



                    // If NO tilt gesture was detected, we pass the accel data to the more advanced recognizer.
                    if (results[0] == false && results[1] == false) {

                        // ------ TEST - voor THREE DOLLAR gesture detection
// TODO we werken nu met INTs maar de gesture recognizer werkt eigenlijk met FLOATs
//                    is het nuttig om met FLOATs te werken? to test...
                        float[] floatArray = {latest_data[0], latest_data[1], latest_data[2]};
                        sendAccelDataToFragment(floatArray);

                        // ------ TEST - voor THREE DOLLAR gesture detection

                    }





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

        // TODO mag dit wel pauzeren of niet?
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


        if (DEBUG) Log.w("onDestroy", "AdvancedFragment destroyed.");

        // TODO mag dus weg?
//        mSensorManager.unregisterListener(sensorListener);
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


    // key = system ID - value = comma separated list of supported gestures for the specific systemID
    // STAAT OOK IN MQTTService dus wijzingen BIJ ALLEBEI DOORVOEREN
    private void addSupportedGesture(String systemID, String gestureToBeAdded) {

        Map<String, String> savedMap = getMapSupportedGestures();
        if (savedMap == null) {
            Log.w("pushnotificationlistener", "SAVEDMAP IS NULL");
        }


        String concatenatedGestures = savedMap.get(systemID);
//        if(concatenatedGestures == null){
//            Log.w("pushnotificationlistener", "concatenatedGestures IS NULL");
//        }

        String newConcatenatedString = "";

        if (concatenatedGestures != null) {

            String[] arrayGestures = concatenatedGestures.split(";");
            Set<String> setGestures = new HashSet<String>(Arrays.asList(arrayGestures));
            // adding new gesture
            setGestures.add(gestureToBeAdded);
            // recreate concatenated string from new set
            newConcatenatedString = TextUtils.join(";", setGestures);

            Log.w("pushnotificationlistener", "newConcatenatedString " + newConcatenatedString);

        } else {

            // TODO zien of niet met ";" direct moet.
            newConcatenatedString = gestureToBeAdded;

            Log.w("pushnotificationlistener", "newConcatenatedString " + newConcatenatedString);
        }


        savedMap.put(systemID, newConcatenatedString);


        SharedPreferences pSharedPref = getActivity().getSharedPreferences("com.yen.androidappthesisyen.system_id_to_supported_gestures", Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            JSONObject jsonObject = new JSONObject(savedMap);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("my_map").commit();
            editor.putString("my_map", jsonString);
            editor.commit();
        }
    }

    // STAAT OOK IN MQTTService dus wijzingen BIJ ALLEBEI DOORVOEREN
    private Map<String, String> getMapSupportedGestures() {

        Map<String, String> outputMap = new HashMap<String, String>();

        SharedPreferences pSharedPref = getActivity().getSharedPreferences("com.yen.androidappthesisyen.system_id_to_supported_gestures", Context.MODE_PRIVATE);

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

}
