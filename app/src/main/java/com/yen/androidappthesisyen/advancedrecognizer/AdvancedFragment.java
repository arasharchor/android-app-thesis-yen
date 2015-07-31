package com.yen.androidappthesisyen.advancedrecognizer;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
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
import android.widget.Toast;
import android.widget.ToggleButton;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.pushnotificationlistener.MQTTService;
import com.yen.androidappthesisyen.simplerecognizer.PebbleGestureModel;
import com.yen.androidappthesisyen.simplerecognizer.TiltGestureRecognizer;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AdvancedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
// 31-07 verwijderd: implements DialogInterface.OnClickListener
public class AdvancedFragment extends Fragment {


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
    private Boolean isTrainingAccelStreamEnabled = false;

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

    // mag wrsl weg
//    final Runnable showAlert = new Runnable() {
//        public void run() {
//            show_alert_box();
//        }
//    };

    // ff public
    public com.yen.androidappthesisyen.advancedrecognizer.App.STATES state = com.yen.androidappthesisyen.advancedrecognizer.App.STATES.STATE_LEARN;


    // private MENUITEMS menuitems;



    private void showChooseGestureDialog() {

        // Get a list of all the distinct gestures by checking which gestures all the Action Devices reported.
        // So if an Action Device adds/removes/renames a gesture, the list in the Android app will adapt.
        Map<String, String> savedMap = getMapSupportedGestures();
        Set setSupportedGestures = new TreeSet();
        if (savedMap != null) {

            List<String> listGestures = new ArrayList<>();

            //iterating over values only. Values = concatenated string of gestures using ";".
            for (String concatenatedString : savedMap.values()) {
                String[] arrayGestures = concatenatedString.split(";");
                List<String> newList = Arrays.asList(arrayGestures);
                for (int i = 0; i < newList.size(); i++) {
                    newList.set(i, WordUtils.capitalize(newList.get(i)));
                }
                listGestures.addAll(newList);
            }

            // Remove duplicates by converting to Set datatype.
            // More specific: using TreeSet so the gestures are sorted alphabetically.
            setSupportedGestures = new TreeSet<>(listGestures);
        }

        final String[] arraySupportedGestures = (String[]) setSupportedGestures.toArray(new String[setSupportedGestures.size()]);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.description_choose_gesture))
                .setItems(arraySupportedGestures, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        String chosenGesture = arraySupportedGestures[which];
                        // Saved with NO uppercase: all internal processing works without uppercases.
                        recordingGestureIDString = WordUtils.uncapitalize(chosenGesture);
                        Log.w(LOG_TAG, "recording for " + recordingGestureIDString);
                        TextView lblRecordedGesture = (TextView) getView().findViewById(R.id.lbl_recorded_gesture);
                        lblRecordedGesture.setText("Gesture being trained: " + chosenGesture);
//                        Toast.makeText(getActivity(), chosenGesture, Toast.LENGTH_LONG).show();

                        if(!isTrainingAccelStreamEnabled){
                            enableAccelStreamForTraining();
                        }

                    }
                });


        AlertDialog dialog = builder.create();
        dialog.show();


    }


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


        Bundle bundle = this.getArguments();
        String state = bundle.getString("state");
        if (state.equalsIgnoreCase("recognize")) {

            if (useGestureSpotting) {
                findBoundariesGesture(values);
            } else {
                // TODO eigenlijk mag IF weg want doen niets bij ELSE?
            }


        } else if (state.equalsIgnoreCase("learn")) {


            if (RECORD_GESTURE) {

                float[] traceItem = {dataX.floatValue(),
                        dataY.floatValue(),
                        dataZ.floatValue()};
                if (recordingGestureTrace != null) {
                    recordingGestureTrace.add(traceItem);
                }

            } else {
                // TODO eigenlijk mag IF weg want doen niets bij ELSE?
            }

        }


        // OUDER
//        if (useGestureSpotting) {
//            findBoundariesGesture(values);
//        } else {
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
//            }
//
//        }


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

    private synchronized void stopRecordingGesture() {


        RECORD_GESTURE = false;
        // Object[] gestureTrace = recordingGestureTrace.toArray();
        /*int numItems = recordingGestureTrace.size();
        // malloc LOL
		float [][] traces = new float [numItems][3];
		// "copy" gesture info to traces!!!*/
        // traces = recordingGestureTrace.toArray(traces);



        doRemainingTasksAfterRecording();



    }

    private void doRemainingTasksAfterRecording() {

        // clear the existing trace
        switch (state) {

            case STATE_LEARN:
                // recordingGestureTrace = null;
                // note that the arraylist is being copied
                Gesture ng = new Gesture(recordingGestureIDString, new ArrayList<float[]>(recordingGestureTrace));

                // add gesture to library but prepare with recognizer settings first
                myGestureLibrary.addGesture(ng.gestureID, this.myGestureRecognizer.prepare_gesture_for_library(ng), false);
                if (DEBUG)
                    Log.w("stopRecordingGesture", "Recorded Gesture ID " + recordingGestureIDString + " Gesture Trace Length:" + recordingGestureTrace.size());
                break;

            case STATE_RECOGNIZE:

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
                        final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_gestures);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                outputWindow.append(detected_gid + "\n");
                                ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);
                                Toast.makeText(getActivity(), detected_gid, Toast.LENGTH_LONG).show();
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

        final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_gestures);


        // Check for which action devices the accel stream is currently running.
        String concatenatedListEnabledActionDevices = getEnabledAccelStreamDevices();

        if (concatenatedListEnabledActionDevices != null && !concatenatedListEnabledActionDevices.equalsIgnoreCase("") && !concatenatedListEnabledActionDevices.equalsIgnoreCase(";")) {

            // Get set of action devices where accel stream is running.
            String[] arrayEnabledActionDevices = concatenatedListEnabledActionDevices.split(";");
            Set<String> setEnabledActionDevices = new HashSet<String>(Arrays.asList(arrayEnabledActionDevices));


            // Get list of action devices which support the recognized gesture.
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


            // Do cross check of action devices which accel stream is running and that support the gesture.


            List<String> listActionDevicesToSendTo = new ArrayList<>();
            Log.w(LOG_TAG, "=================================== listActionDevicesToSendTo.size() " + listActionDevicesToSendTo.size());

            for (String accelStreamRunningActionDevice : setEnabledActionDevices) {
                if (supportedSystems.contains(accelStreamRunningActionDevice)) {
                    listActionDevicesToSendTo.add(accelStreamRunningActionDevice);
                }
            }


            if (listActionDevicesToSendTo.size() >= 1) {


                // Sending feedback to the user:
                // If a gesture detected + supported by at least one of the currently used action devices: do 1x short vibration.
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

                    for (String actionDeviceToSendTo : listActionDevicesToSendTo) {

                        String IPAddress = "192.168.1.1";
                        if (actionDeviceToSendTo.equalsIgnoreCase("yen-asus")) {
                            // TODO momenteel nog hardgecodeerde mapping yen-asus naar enum 1
                            IPAddress = gestureHandlersettings.getString("ip_address_1", "192.168.1.1"); // OF HIER dus checken of er al waarde is: INDIEN NIET: TOON DIALOOG VENSTER.
                        } else if (actionDeviceToSendTo.equalsIgnoreCase("yen-medion")) {
                            // TODO momenteel nog hardgecodeerde mapping yen-medion naar enum 2
                            IPAddress = gestureHandlersettings.getString("ip_address_2", "192.168.1.1"); // OF HIER dus checken of er al waarde is: INDIEN NIET: TOON DIALOOG VENSTER.
                        } else {
                            Log.w("pushnotificationlistener", "SystemID unknown so no mapped IP address for Gesture Handler found");
                        }


                        Log.w(LOG_TAG, "saved IP is " + IPAddress);

                        // TODO met of zonder slash?
                        final String stringURL = "http://" + IPAddress + ":8080/RESTWithJAXB/rest/handlegesture/invoer";
                        // TODO KAN DIE NIET ALTIJD WIJZIGEN DUS VIA DIALOOGVENSTER AAN USER VRAGEN?


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

                    }


                } else {
                    // Arriving here if no internet connection.
                    // TODO iets doen: bv. melden aan user of zelfs direct terug IP insert dialog tonen!
                    // MAAR stel dat er internet is, maar een broker is niet meer verbonden, wordt dat hier niet ontdekt dus moet dat ook ontdekken!
                    doTwoShortPebbleVibrations();

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            outputWindow.append("No internet connection\n");
                            ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);
                            Toast.makeText(getActivity(), "No internet connection", Toast.LENGTH_LONG).show();
                        }
                    });


                    Log.w(LOG_TAG, "No internet connection");
                }


                // ---- STOP EIGEN TOEVOEGING


            } else {
                // Do nothing.
                doTwoShortPebbleVibrations();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        outputWindow.append("No cross match found\n");
                        ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);
                    }
                });

                Log.w(LOG_TAG, "No cross match found");
            }


        } else {
            // The accel stream isn't running. This means we don't need to do any processing.
            doTwoShortPebbleVibrations();

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    outputWindow.append("Data stream not running\n");
                    ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);
                    Toast.makeText(getActivity(), "Data stream not running", Toast.LENGTH_LONG).show();
                }
            });


            Log.w(LOG_TAG, "Accel stream not running");
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
            httpcon.setConnectTimeout(10000);
            httpcon.setReadTimeout(10000);


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


            httpResult = httpcon.getResponseCode();
            if (httpResult == HttpURLConnection.HTTP_OK) { // Numeric status code, 200: OK

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
                // TODO DIALOG OF TOAST OFZO TONEN ALS HET FOUTLIEP
                // TODO OOK TELKENS MELDEN ALS GOED GING?


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


        // see if we have a gesture library
        // TODO eventueel deze if binnen de try/catch zetten indien nodig.
        if (GestureLibrary.GLibrarySingleInstance == null) {

            Log.w(LOG_TAG, "--------------------- NO GESTURE LIBRARY YET AdvancedFragment ---------------------");
            // TODO iets doen? popup? leeg venster met tekst? etc.


            // TODO je hebt hier TRY CATCH rond gezet want gaf error over SQL en close(): to fix.
            try {
                myGestureLibrary = new GestureLibrary("GESTURES", getActivity());
            } catch (Exception ex) {

            }

            Log.w(LOG_TAG, "--------------------- nu gemaakt ---------------------");

        } else {

            myGestureLibrary = GestureLibrary.GLibrarySingleInstance;
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
        // mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        // TODO mag weg maar test of NIETS BREAKT.
//        mSensorManager.registerListener(sensorListener, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST/*SensorManager.SENSOR_DELAY_GAME*/);


        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_advanced, container, false);


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
                    enableAccelStream("triggered-by-user");

                } else {
                    Log.w(LOG_TAG, "CALLING disableAccelStream");
                    disableAccelStream("triggered-by-user");
                }

            }
        });


        // TODO die checkbox mag eigenlijk weg he, want user zal nooit toch manueel gesture spotting aan/uit zetten?
        // Of houden en gewoon in comments zetten.
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


        // Button CHOOSE gesture.
        final Button btnChooseGesture = (Button) returnedView.findViewById(R.id.btn_choose_gesture);
        btnChooseGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showChooseGestureDialog();

            }
        });


        // Button TRAIN gesture.
        final Button btnRecordGesture = (Button) returnedView.findViewById(R.id.btn_record_gesture);
        btnRecordGesture.setOnTouchListener(new View.OnTouchListener() {
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

        // TODO vereist of?
        btnRecordGesture.setOnClickListener(new View.OnClickListener()

                                      {
                                          public void onClick(View v) {
                                              // clicked
                                              //Log.w("onClick", "Clicked");
                        /*if (btnRecordGesture.isFocused())
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

        Bundle bundle = this.getArguments();
        String state = bundle.getString("state");
        if (state.equalsIgnoreCase("recognize")) {
            TextView lblRecordedGesture = (TextView) returnedView.findViewById(R.id.lbl_recorded_gesture);
            lblRecordedGesture.setVisibility(View.INVISIBLE);
            btnChooseGesture.setVisibility(View.INVISIBLE);
            btnRecordGesture.setVisibility(View.INVISIBLE);
        } else if (state.equalsIgnoreCase("learn")) {
            toggleAccelStream.setVisibility(View.INVISIBLE);
            checkboxGestureSpotting.setVisibility(View.INVISIBLE);
        }


        // We also place it here, since in case there is already text in the ScrollView when arriving there, we immediately scroll.
        ((ScrollView) returnedView.findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);


        setCorrectStateAndView();


        return returnedView;
    }

    private void setCorrectStateAndView() {

        Bundle bundle = this.getArguments();
        String state = bundle.getString("state");
        if (state.equalsIgnoreCase("learn")) {
            this.state = App.STATES.STATE_LEARN;
        } else if (state.equalsIgnoreCase("recognize")) {
            this.state = App.STATES.STATE_RECOGNIZE;
        } else if (state.equalsIgnoreCase("library")) {
            this.state = App.STATES.STATE_LIBRARY;
        } else if (state.equalsIgnoreCase("default")) {
            // TODO iets doen?
        }

        stateChanged(); // FYI: dit doet niets concreets als het state "library" is.

        // 16-07 OUD
//        if(state.equalsIgnoreCase("library")){
//            // start library activity
//            Intent i = new Intent(getActivity(), DBManagerUIActivity.class);
//            startActivityForResult(i, 0);
//        }

    }


    // ----------- KOPIE OOK TE VINDEN IN MQTTSERVICE.JAVA DUS VOER DAAR OOK WIJZIGINGEN DOOR.
    private void enableAccelStream(String systemID) {


        String previousList = getEnabledAccelStreamDevices();
        Log.w(LOG_TAG, "previousList " + previousList);

        addNewAccelStreamState(systemID, "enable"); // List has now been updated.

        if (previousList.equalsIgnoreCase("") || previousList.equalsIgnoreCase(";")) {
            // The previous list was empty. This means we deliberately need to send a signal to start the accel stream.

            PebbleDictionary dict = new PebbleDictionary();
            dict.addInt32(1, 0); // key = 1 = TRUE = start stream, value = 0
            PebbleKit.sendDataToPebble(getActivity(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);

        } else {
            // The previous list was NOT empty. This means we don't need to send the signal to start the stream, since it's already running.

        }

    }

    // ----------- KOPIE OOK TE VINDEN IN MQTTSERVICE.JAVA DUS VOER DAAR OOK WIJZIGINGEN DOOR.
    private void disableAccelStream(String systemID) {


        String previousList = getEnabledAccelStreamDevices();
        Log.w(LOG_TAG, "previousList " + previousList);

        addNewAccelStreamState(systemID, "disable"); // List has now been updated.

        String newList = getEnabledAccelStreamDevices();
        Log.w(LOG_TAG, "newList " + newList);

        if (newList.equalsIgnoreCase("") || newList.equalsIgnoreCase(";")) {
            // The NEW list is empty. This means the just removed device was the only device where it was running. Stop the accel stream.

            PebbleDictionary dict = new PebbleDictionary();
            dict.addInt32(0, 0); // key = 0 = FALSE = stop stream, value = 0
            PebbleKit.sendDataToPebble(getActivity(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);

        } else {
            // The NEW list is NOT empty. This means we keep the accel stream alive. So we do nothing.
        }
    }


    private void enableAccelStreamForTraining() {

        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(1, 0); // key = 1 = TRUE = start stream, value = 0
        PebbleKit.sendDataToPebble(getActivity(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);

        isTrainingAccelStreamEnabled = true;

    }


    private void disableAccelStreamForTraining() {

        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(0, 0); // key = 0 = FALSE = stop stream, value = 0
        PebbleKit.sendDataToPebble(getActivity(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);

        isTrainingAccelStreamEnabled = false;

        Log.w(LOG_TAG, "stream disabled");
    }


    // STAAT OOK IN MQTTSERVICE.JAVA DUS DAAR OOK AANPASSEN
    private String getEnabledAccelStreamDevices() {

        SharedPreferences enumSetting = getActivity().getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        String enabledList = enumSetting.getString("enabledaccelstreamdevices", "");
        return enabledList;
    }

    // STAAT OOK IN MQTTSERVICE.JAVA DUS DAAR OOK AANPASSEN
    // KEY = "accelstreamenabled" - VALUE = comma separated list of systemIDs where stream is currently enabled.
    private void addNewAccelStreamState(String systemID, String stateRequest) {


        String concatenatedListEnabledActionDevices = getEnabledAccelStreamDevices();

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

                // TODO zien of niet met ";" direct moet.
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
                // TODO ========= zeker zijn dat als set nu LEEG zou zijn, dat er geen problemen zijn, bv. dat er een ";" instaat en dat dat problemen zou geven.

                Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);

            } else {

                // TODO zien of niet met ";" direct moet.
                newConcatenatedString = systemID;

                Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);
            }


        } else {
            Log.w(LOG_TAG, "Wrong accel stream state request: not 'enable' or 'disable'");
        }


        SharedPreferences pSharedPref = getActivity().getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("enabledaccelstreamdevices").commit();
            editor.putString("enabledaccelstreamdevices", newConcatenatedString);
            editor.commit();
        }


    }


    // TODO mag weg: werd gebruikt bij oude dialog waarbij user gesture title kon ingeven.
//    @Override
//    public void onClick(DialogInterface dialog, int which) {
//
//        /*
//         * android.content.DialogInterface.OnClickListener callback
//		 *
//		 */
//        if (dialog == this.learning_dialog) {
//            if (this.learning_dialog_view != null) {
//                //this.recordingGestureIDString =
//                EditText et = (EditText) learning_dialog_view.findViewById(R.id.EditText01);
//                this.recordingGestureIDString = et.getText().toString();
//
//            }
//        }
//
//    }


    // TODO 31-07 mag merkelijk nu weg want elke case is nu LEEG?
    public void stateChanged() {


        switch (this.state) {

            case STATE_LEARN:

                // OUD:
//                Context ctx = getActivity();
//                LayoutInflater li = LayoutInflater.from(getActivity());
//                View dialog = li.inflate(R.layout.learngesturedialog, null);
//                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                builder.setTitle("Enter Gesture ID");
//                builder.setView(dialog);
//                builder.setPositiveButton("OK", this);
//                builder.setNegativeButton("Cancel", this);
//                // show the actual dialog
//                AlertDialog ad = builder.create();
//                this.learning_dialog = ad;
//                this.learning_dialog_view = dialog;
//                ad.show();

                break;

            case STATE_RECOGNIZE:
                break;

            case STATE_LIBRARY:
                // njet: en logisch want we komen hier nooit als Gesture Library action button wordt geklikt: er wordt dan direct naar ander Fragment gesprongen,
                // ipv DIT fragment met z'n learn en recognize states.
                break;

            default:

                break;
        }
    }


    // TODO DEZE GANSE METHOD + LOGICA ERIN MAG WRSL ZOMAAR WEG WANT IS VAN EEN OUD SYSTEEM VIA GESTURELIBRARY DAT IN GANS APARTE ACTIVITY ZAT.
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // change state and map to enum value
        this.state = com.yen.androidappthesisyen.advancedrecognizer.App.STATES.values()[resultCode];
        //update activity's state
        this.stateChanged();
    }


    @Override
    public void onResume() {
        super.onResume();


        // TODO stond zeer lange tijd in onCreate maar hier is wrsl beter?
        // voor starten app voor ACCEL DATA STREAM.
        PebbleKit.startAppOnPebble(getActivity(), uuid);


        // voor (o.a.) PEBBLE ACCEL STREAM
        getToggleStatesAndEnableServices();


        // 31-07 uit want enablen nu pas wann een eerste gesture is geselecteerd.
        // ONLY when we are in the TRAIN tab do we automatically enable the data stream.
//        Bundle bundle = this.getArguments();
//        String state = bundle.getString("state");
//        if (state.equalsIgnoreCase("learn")) {
//
//            Log.w(LOG_TAG, "======================= net voor enableAccelStreamForTraining");
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//
//                    try {
//                        Thread.sleep(1000); // TODO default 5000. Als problemen geeft, verhoog.
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    enableAccelStreamForTraining();
//
//                }
//            }).start();
//
//
//        }

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
                    if (state == App.STATES.STATE_RECOGNIZE) {

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


    }


    @Override
    public void onPause() {
        super.onPause();


        // ONLY when we leave in the TRAIN tab do we automatically disable the data stream (since we also enabled it automatically).
        Bundle bundle = this.getArguments();
        String state = bundle.getString("state");
        if (state.equalsIgnoreCase("learn")) {
            disableAccelStreamForTraining();
        }

        // TODO mag dit wel pauzeren of niet?
        // voor PEBBLE ACCEL STREAM
        disableAllServices();

        PebbleKit.closeAppOnPebble(getActivity(), uuid);
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
