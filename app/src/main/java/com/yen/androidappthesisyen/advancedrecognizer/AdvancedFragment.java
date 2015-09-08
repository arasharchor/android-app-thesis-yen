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
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.gesturelibrary.GestureLibrary;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static com.yen.androidappthesisyen.utilities.UtilityRepo.addNewAccelStreamState;
import static com.yen.androidappthesisyen.utilities.UtilityRepo.getEnabledAccelStreamDevices;
import static com.yen.androidappthesisyen.utilities.UtilityRepo.getMapSupportedGestures;


public class AdvancedFragment extends Fragment {


    private static final String LOG_TAG = "Advanced Fragment";


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
    private Boolean isVibrationFeedbackEnabled = false;
    private ToggleButton toggleVibrationFeedback;
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

    private boolean DEBUG = false;
    private boolean VERBOSE = false;


    // dialog view for entering learning gesture
    private AlertDialog learning_dialog = null;
    private View learning_dialog_view = null;


    final Handler alertHandler = new Handler();
    public String detected_gid = "Unknown";


    private UsedConstants.STATES state = UsedConstants.STATES.STATE_LEARN;


    public enum RecordMode {
        MOTION_DETECTION, PUSH_TO_GESTURE
    }

    RecordMode recordMode = RecordMode.MOTION_DETECTION;
    boolean isAdvancedRecording = false;
    ArrayList<float[]> gestureValues = new ArrayList<float[]>();

    // Values related to gesture spotting.
    float MINIMUM_ACCELERATION_THRESHOLD_FOR_STARTING = 1200;
    float MINIMUM_ACCELERATION_THRESHOLD_WHILE_RECORDING = 1050;
    int stepsSinceNoMovement; // TODO default op 0 zetten of niet?
    final int MINIMUM_GESTURE_LENGTH = 5; // default 8
    // If too high, the user has to stand still for a too long time with the Pebble:
    final int MINIMUM_STEPS_SINCE_NO_MOVEMENT = 8; // default 10


    private void showChooseGestureDialog() {

        // Get a list of all the distinct gestures by checking which gestures all the Action Devices reported.
        // So if an Action Device adds/removes/renames a gesture, the list in the Android app will adapt.
        Map<String, String> savedMap = getMapSupportedGestures(getActivity());
        Set setSupportedGestures = new TreeSet();
        if (savedMap != null) {

            List<String> listGestures = new ArrayList<>();

            // Iterating over values only. Values = concatenated string of gestures using ";".
            for (String concatenatedString : savedMap.values()) {
                String[] arrayGestures = concatenatedString.split(";");
                List<String> newList = Arrays.asList(arrayGestures);
                for (int i = 0; i < newList.size(); i++) {
                    // Remove up/down/left/right since those never need to be trained.
                    if (newList.get(i).equalsIgnoreCase("up") || newList.get(i).equalsIgnoreCase("down") || newList.get(i).equalsIgnoreCase("left") || newList.get(i).equalsIgnoreCase("right")) {
                        newList.set(i, "");
                    } else {
                        newList.set(i, WordUtils.capitalize(newList.get(i)));
                    }

                }
                listGestures.addAll(newList);
            }

            // Remove duplicates by converting to Set datatype.
            // More specific: using TreeSet so the gestures are sorted alphabetically.
            setSupportedGestures = new TreeSet<>(listGestures);


        }

        final String[] arraySupportedGestures = (String[]) setSupportedGestures.toArray(new String[setSupportedGestures.size()]);

        Boolean caseEmpty = false;
        if (arraySupportedGestures.length == 1 && arraySupportedGestures[0].equalsIgnoreCase("")) {
            arraySupportedGestures[0] = "No options available: none of your Action Devices support any trainable gesture at the moment.";
            caseEmpty = true;
        }

        Log.w(LOG_TAG, "arraySupportedGestures.length " + arraySupportedGestures.length);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final Boolean finalCaseEmpty = caseEmpty;

        builder.setTitle(getResources().getString(R.string.description_choose_gesture))
                .setItems(arraySupportedGestures, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        if (!finalCaseEmpty && which != 0) {

                            String chosenGesture = arraySupportedGestures[which];
                            // Saved with NO uppercase: all internal processing works without uppercases.
                            recordingGestureIDString = WordUtils.uncapitalize(chosenGesture);
                            Log.w(LOG_TAG, "recording for " + recordingGestureIDString);
                            TextView lblRecordedGesture = (TextView) getView().findViewById(R.id.lbl_recorded_gesture);
                            lblRecordedGesture.setText("Gesture being trained: " + chosenGesture);


                            if (!isTrainingAccelStreamEnabled) {
                                enableAccelStreamForTraining();
                            }

                        } else {
                            // Do nothing.
                        }


                    }
                });


        AlertDialog dialog = builder.create();
        dialog.show();


    }


    public void sendAccelDataToFragment(float[] values) {


        SharedPreferences settings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.gesture_spotting", Context.MODE_PRIVATE);
        Boolean useGestureSpotting = settings.getBoolean("use", true);


        Bundle bundle = this.getArguments();
        String state = bundle.getString("state");
        if (state.equalsIgnoreCase("recognize")) {


            // Gesture spotting
            findBoundariesGesture(values);


        } else if (state.equalsIgnoreCase("learn")) {


            if (RECORD_GESTURE) {

                float[] traceItem = {values[0],
                        values[1],
                        values[2]};
                if (recordingGestureTrace != null) {
                    recordingGestureTrace.add(traceItem);
                }

            } else {

            }

        }


    }


    public Boolean isAdvancedRecording() {
        return isAdvancedRecording;
    }


    private void findBoundariesGesture(float[] values) {


        Log.w(LOG_TAG, "X " + values[0] + " Y " + values[1] + " Z " + values[2]);


        /*
        switch (recordMode) {

            case MOTION_DETECTION:

        */
        if (isAdvancedRecording) {

            gestureValues.add(values);

            if (calcVectorNorm(values) < MINIMUM_ACCELERATION_THRESHOLD_WHILE_RECORDING) {

                Log.w(LOG_TAG, "========================= Gesture NOT recorded anymore ============= stepsSinceNoMovement++");
                stepsSinceNoMovement++;

            } else {

                Log.w(LOG_TAG, "===================== Still IN gesture ======= stepsSinceNoMovement = 0");
                stepsSinceNoMovement = 0;

            }

        } else if (calcVectorNorm(values) >= MINIMUM_ACCELERATION_THRESHOLD_FOR_STARTING) {

            Log.w(LOG_TAG, "========================================================== STARTING recording gesture");

            isAdvancedRecording = true;
            stepsSinceNoMovement = 0;
            gestureValues = new ArrayList<float[]>();
            gestureValues.add(values);
        }


        if (stepsSinceNoMovement == MINIMUM_STEPS_SINCE_NO_MOVEMENT) {

            Log.w(LOG_TAG, "============================ Detection POSSIBLE gesture");

            int length = gestureValues.size() - MINIMUM_STEPS_SINCE_NO_MOVEMENT;
            if (length > MINIMUM_GESTURE_LENGTH) {
//                         listener.onGestureRecorded(gestureValues.subList(0, gestureValues.size() - MINIMUM_STEPS_SINCE_NO_MOVEMENT));
                // FYI ArrayList<float[]> gestureValues;

                Log.w(LOG_TAG, "============= It was INDEED a gesture =============== STOPPED with RECORDING gesture and the gesture length was GREATER THAN MINIMUM. length = " + length);

                // The gesture is done and the valid values are within index 0 to gestureValues.size() - MINIMUM_STEPS_SINCE_NO_MOVEMENT

                recordingGestureTrace = new ArrayList<float[]>(250);
                // opvullen met de juiste accel data.
                // maken kopie van subList van subList geeft VIEW terug en niet iets dat je kan casten naar ArrayList.
                recordingGestureTrace = new ArrayList<>(gestureValues.subList(0, gestureValues.size() - MINIMUM_STEPS_SINCE_NO_MOVEMENT));


                // If we were in TRAIN mode: save the recorded data and link it to the right gesture.
                // If we were in RECOGNIZE mode: recognize the gesture, update the UI using a separate thread, and send the gesture
                // to the connected Action Device(s) that support(s) it if any.
                doRemainingTasksAfterRecording();

            }

            Log.w(LOG_TAG, "============================ Starting back FROM ZERO");
            gestureValues = new ArrayList<float[]>();
            stepsSinceNoMovement = 0;
            isAdvancedRecording = false;
        }


        /*
                break;


            case PUSH_TO_GESTURE:

                if (isAdvancedRecording) {
                    gestureValues.add(values);
                }
                break;
        }
        */

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
        doRemainingTasksAfterRecording();


    }


    private void doRemainingTasksAfterRecording() {

        // clear the existing trace
        switch (state) {

            case STATE_LEARN:
                // recordingGestureTrace = null;
                // note that the arraylist is being copied
                Gesture ng = new Gesture(recordingGestureIDString, new ArrayList<>(recordingGestureTrace));

                // add gesture to library but prepare with recognizer settings first
                myGestureLibrary.addGesture(ng.gestureID, this.myGestureRecognizer.prepare_gesture_for_library(ng), false);
                if (DEBUG)
                    Log.w("stopRecordingGesture", "Recorded Gesture ID " + recordingGestureIDString + " Gesture Trace Length:" + recordingGestureTrace.size());
                break;

            case STATE_RECOGNIZE:

                // BUGFIX VIA https://code.google.com/p/three-dollar-gesture-recognizer/issues/detail?id=1
                final Gesture candidate = new Gesture(null, new ArrayList<>(recordingGestureTrace));


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


                        if (!detected_gid.equalsIgnoreCase("not recognized!")) {

                            if (!detected_gid.equalsIgnoreCase("unknown") && !detected_gid.equalsIgnoreCase("unknown gesture")) {


                                final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_gestures);
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // OUD
//                                    outputWindow.append(detected_gid + "\n");
                                        // NIEUW
                                        outputWindow.append(Html.fromHtml("<b>" + detected_gid + "</b>" + "<br />"));

                                        ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);
                                        Toast.makeText(getActivity(), detected_gid, Toast.LENGTH_LONG).show();
                                    }
                                });

                            } else {

                                // Do nothing.

                            }


                            if (detected_gid.equalsIgnoreCase("unknown") || detected_gid.equalsIgnoreCase("unknown gesture")) {

                                // doTwoShortPebbleVibrations();
                            } else {

                                sendGestureIfMatchFound(detected_gid);

                            }


                        } else {

                            // Do nothing.

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
        String concatenatedListEnabledActionDevices = getEnabledAccelStreamDevices(getActivity());

        if (concatenatedListEnabledActionDevices != null && !concatenatedListEnabledActionDevices.equalsIgnoreCase("") && !concatenatedListEnabledActionDevices.equalsIgnoreCase(";")) {

            // Get set of action devices where accel stream is running.
            String[] arrayEnabledActionDevices = concatenatedListEnabledActionDevices.split(";");
            Set<String> setEnabledActionDevices = new HashSet<String>(Arrays.asList(arrayEnabledActionDevices));


            // Get list of action devices which support the recognized gesture.
            Map<String, String> savedMap = getMapSupportedGestures(getActivity());
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
                if (isVibrationFeedbackEnabled) {
                    doShortPebbleVibration();
                }


                // ---- START EIGEN TOEVOEGING
                // vóór showAlert gedaan want netwerktaken vragen toch wat tijd.
                // UPDATE: toch showAlert eerst want als geen netwerk, wordt alertbox pas getoond NA OVERSCHREIDEN TIME-OUT.


                ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {

                    // fetch data

                    SharedPreferences gestureHandlersettings = getActivity().getSharedPreferences("com.yen.androidappthesisyen.gesture_handler", Context.MODE_PRIVATE);

                    for (String actionDeviceToSendTo : listActionDevicesToSendTo) {

                        String IPAddress = "192.168.1.1";
                        String preferenceKey = "ip_address_" + actionDeviceToSendTo;


                        IPAddress = gestureHandlersettings.getString(preferenceKey, "192.168.1.1");


                        Log.w(LOG_TAG, "saved IP is " + IPAddress);

                        final String stringURL = "http://" + IPAddress + ":8080/RESTWithJAXB/rest/handlegesture/invoer";


                        // Network actions not allowed on main thread (= UI thread) so starting a new thread.
                        Thread t = new Thread() {
                            public void run() {

                                // EEEEEEEEEEERST NOG IS DIT MAAR MET ENGELSE GESTURETERMEN TESTEN; DAARNA HET ANDERE.
                                // UPDATE: EIGEN CODE WERKT NU :D
                                int httpResult = POSTGestureToServer(stringURL, recognizedGesture);

                            }
                        };
                        t.start();


                    }


                } else {

                    // Arriving here if no network connection.
                    if (isVibrationFeedbackEnabled) {
                        doTwoShortPebbleVibrations();
                    }


                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            outputWindow.append("No network connection\n");
                            ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);
                            Toast.makeText(getActivity(), "No network connection", Toast.LENGTH_LONG).show();
                        }
                    });


                    Log.w(LOG_TAG, "No network connection");
                }


                // ---- STOP EIGEN TOEVOEGING


            } else {

                if (isVibrationFeedbackEnabled) {
                    doTwoShortPebbleVibrations();
                }


                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        outputWindow.append(getResources().getString(R.string.no_connected_AD_supports));
                        ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);
                    }
                });

                Log.w(LOG_TAG, "No currently connected Action Device supports this gesture");
            }


        } else {

            // We should never arrive here anyway...

            // The accel stream isn't running. This means we don't need to do any processing.
            if (isVibrationFeedbackEnabled) {
                doTwoShortPebbleVibrations();
            }


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

            httpcon = (HttpURLConnection) ((new URL(stringURL).openConnection()));
            httpcon.setDoInput(true); // Sets the flag indicating whether this URLConnection allows input. It cannot be set after the connection is established.
            httpcon.setDoOutput(true);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("charset", "utf-8");
            httpcon.setRequestMethod("POST");
            httpcon.setUseCaches(false);
            httpcon.setConnectTimeout(5000);
            httpcon.setReadTimeout(5000);

            httpcon.connect();


            JSONObject jsonObject = new JSONObject();
            jsonObject.put("gesture", stringGesture);


            OutputStream os = httpcon.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(jsonObject.toString());
            writer.close(); // Closes this writer. The contents of the buffer are flushed, the target writer is closed, and the buffer is released. Only the first invocation of close has any effect.
            os.close();


            httpResult = httpcon.getResponseCode();
            if (httpResult == HttpURLConnection.HTTP_OK) { // Numeric status code, 200: OK

                BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));


                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();
                String resultString = sb.toString();
                System.out.println(" ------- RESULT STRING: " + resultString);

            } else {
                // Handle any other errors (404, 500, ...)

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final TextView outputWindow = (TextView) getView().findViewById(R.id.textView_gestures);
                        outputWindow.append("Sending gesture to action device(s) unsuccessful\n");
                        ((ScrollView) getView().findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);
                        Toast.makeText(getActivity(), "Sending gesture to action device(s) unsuccessful", Toast.LENGTH_LONG).show();
                    }
                });

                System.out.println("RESPONSE WAS NOT CODE HTTP_OK BUT: " + httpcon.getResponseMessage());
            }


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


        // create gesture library
        // see if we have a gesture library
        if (GestureLibrary.GLibrarySingleInstance == null) {

            Log.w(LOG_TAG, "--------------------- NO GESTURE LIBRARY YET ---------------------");


            try {
                myGestureLibrary = new GestureLibrary("GESTURES", getActivity());
            } catch (Exception ex) {

            }

            Log.w(LOG_TAG, "--------------------- Made gesture library instance ---------------------");

        } else {

            myGestureLibrary = GestureLibrary.GLibrarySingleInstance;
        }


        myGestureRecognizer = new GestureRec3D(myGestureLibrary, 50);

        // threshold param was lange tijd 700
        // update nu 900 maar mag nog wat lager indien nuttig! nu is 850 - nu terug 900 MAAR IS 875 testen
        theTiltGestureRecognizer = new TiltGestureRecognizer(this, 1000, 750, PebbleGestureModel.MODE_TILT);


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


        toggleVibrationFeedback = (ToggleButton) returnedView.findViewById(R.id.toggle_vibration_feedback);
        toggleVibrationFeedback.setChecked(false);
        toggleVibrationFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (toggleVibrationFeedback.isChecked()) {
                    isVibrationFeedbackEnabled = true;
                } else {
                    isVibrationFeedbackEnabled = false;
                }

            }
        });


        // May be removed. Only for debugging purposes.
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
                                                        // Disabling the button for a short moment to make sure processing fully completes before the user records again.
                                                        btnRecordGesture.setEnabled(false);
                                                        stopRecordingGesture();
                                                        btnRecordGesture.setEnabled(true);

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

        final Button btnManualMMLab = (Button) returnedView.findViewById(R.id.button_MMLab_manually);
        btnManualMMLab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendGestureIfMatchFound("circle");
            }
        });

        TextView tutorial = (TextView) returnedView.findViewById(R.id.tutorial);

        Bundle bundle = this.getArguments();
        String state = bundle.getString("state");
        if (state.equalsIgnoreCase("recognize")) {
            TextView lblRecordedGesture = (TextView) returnedView.findViewById(R.id.lbl_recorded_gesture);
            lblRecordedGesture.setVisibility(View.INVISIBLE);
            btnChooseGesture.setVisibility(View.INVISIBLE);
            btnRecordGesture.setVisibility(View.INVISIBLE);
            tutorial.setText(getResources().getString(R.string.tutorial_recognize));
        } else if (state.equalsIgnoreCase("learn")) {
            toggleAccelStream.setVisibility(View.INVISIBLE);
            TextView lblVibrationFeedback = (TextView) returnedView.findViewById(R.id.label_FeedbackVibration);
            lblVibrationFeedback.setVisibility(View.INVISIBLE);
            toggleVibrationFeedback.setVisibility(View.INVISIBLE);
            tutorial.setText(getResources().getString(R.string.tutorial_train));
            btnManualMMLab.setVisibility(View.INVISIBLE);
            TextView lblMMLabManually = (TextView) returnedView.findViewById(R.id.textView_MMLab_manually);
            lblMMLabManually.setVisibility(View.INVISIBLE);
        }
        // Don't need to show it anymore but still available if necessary.
        checkboxGestureSpotting.setVisibility(View.INVISIBLE);

        // We also place it here, since in case there is already text in the ScrollView when arriving there, we immediately scroll.
        ((ScrollView) returnedView.findViewById(R.id.scrollView_gestures)).fullScroll(View.FOCUS_DOWN);


        setCorrectStateAndView();


        return returnedView;
    }

    private void setCorrectStateAndView() {

        Bundle bundle = this.getArguments();
        String state = bundle.getString("state");
        if (state.equalsIgnoreCase("learn")) {
            this.state = UsedConstants.STATES.STATE_LEARN;
        } else if (state.equalsIgnoreCase("recognize")) {
            this.state = UsedConstants.STATES.STATE_RECOGNIZE;
        } else if (state.equalsIgnoreCase("library")) {
            this.state = UsedConstants.STATES.STATE_LIBRARY;
        } else if (state.equalsIgnoreCase("default")) {

        }

    }


    // ----------- KOPIE OOK TE VINDEN IN MQTTSERVICE.JAVA DUS VOER DAAR OOK WIJZIGINGEN DOOR.
    private void enableAccelStream(String systemID) {

        String previousList = getEnabledAccelStreamDevices(getActivity());
        Log.w(LOG_TAG, "previousList " + previousList);

        addNewAccelStreamState(getActivity(), systemID, "enable"); // List has now been updated.

        if (systemID.equalsIgnoreCase("triggered-by-user") || previousList.equalsIgnoreCase("") || previousList.equalsIgnoreCase(";")) {
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


        String previousList = getEnabledAccelStreamDevices(getActivity());
        Log.w(LOG_TAG, "previousList " + previousList);

        addNewAccelStreamState(getActivity(), systemID, "disable"); // List has now been updated.

        String newList = getEnabledAccelStreamDevices(getActivity());
        Log.w(LOG_TAG, "newList " + newList);

        if (systemID.equalsIgnoreCase("triggered-by-user") || newList.equalsIgnoreCase("") || newList.equalsIgnoreCase(";")) {
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


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // change state and map to enum value
        this.state = UsedConstants.STATES.values()[resultCode];
        //update activity's state

    }


    @Override
    public void onResume() {
        super.onResume();

        // We use delay of 500 milliseonds before starting the Pebble app, because when switching between the Train and Recognize tab would
        // otherwise sometimes have the effect that the Pebble app doesn't get started.
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PebbleKit.startAppOnPebble(getActivity(), uuid);
            }
        }, 500);


        getToggleStatesAndEnableServices();

    }


    private void getToggleStatesAndEnableServices() {

        startPebbleDataStream();

    }


    private void startPebbleDataStream() {


        if (receiver == null) {


            receiver = new PebbleKit.PebbleDataReceiver(uuid) {

                @Override
                public void receiveData(Context context, int transactionId, PebbleDictionary data) {

                    // stond getApplicationContext()
                    PebbleKit.sendAckToPebble(getActivity(), transactionId);

                    // Count total data
                    totalData += 3 * NUM_SAMPLES * 4;

                    // Get data
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


                    if (state == UsedConstants.STATES.STATE_RECOGNIZE) {

                        // TILT GESTURE DETECTION
                        int[] intArray = {latest_data[0], latest_data[1], latest_data[2]};
                        results = theTiltGestureRecognizer.update(intArray);

                    } else {
                        // We are in the LEARN STATE or LIBRARY STATE

                        results[0] = false;
                        results[1] = false;
                    }


                    // If NO tilt gesture was detected, we pass the accel data to the more advanced recognizer.
                    if (results[0] == false && results[1] == false) {

                        float[] floatArray = {latest_data[0], latest_data[1], latest_data[2]};
                        sendAccelDataToFragment(floatArray);

                    }


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


        disableAllServices();

        PebbleKit.closeAppOnPebble(getActivity(), uuid);
    }


    private void disableAllServices() {
        stopPebbleDataStream();
    }

    private void stopPebbleDataStream() {

        if (receiver != null) {

            try {

                getActivity().unregisterReceiver(receiver);

                receiver = null;


            } catch (IllegalArgumentException ex) {

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

        super.onDestroy();
    }


    // 06/09 MAG WEG WANT NIET GEBRUIKT: wel in MQTT service maar daar is code al aangepast naar recentere versie!
    // key = system ID - value = comma separated list of supported gestures for the specific systemID
    // STAAT OOK IN MQTTService dus wijzingen BIJ ALLEBEI DOORVOEREN
//    private void addSupportedGesture(String systemID, String gestureToBeAdded) {
//
//        Map<String, String> savedMap = getMapSupportedGestures();
//
//        String concatenatedGestures = savedMap.get(systemID);
//
//        String newConcatenatedString = "";
//
//        if (concatenatedGestures != null) {
//
//            String[] arrayGestures = concatenatedGestures.split(";");
//            Set<String> setGestures = new HashSet<String>(Arrays.asList(arrayGestures));
//            // adding new gesture
//            setGestures.add(gestureToBeAdded);
//            // recreate concatenated string from new set
//            newConcatenatedString = TextUtils.join(";", setGestures);
//
//            Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);
//
//        } else {
//
//            newConcatenatedString = gestureToBeAdded;
//
//            Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);
//        }
//
//
//        savedMap.put(systemID, newConcatenatedString);
//
//
//        SharedPreferences pSharedPref = getActivity().getSharedPreferences("com.yen.androidappthesisyen.system_id_to_supported_gestures", Context.MODE_PRIVATE);
//        if (pSharedPref != null) {
//            JSONObject jsonObject = new JSONObject(savedMap);
//            String jsonString = jsonObject.toString();
//            SharedPreferences.Editor editor = pSharedPref.edit();
//            editor.remove("my_map").commit();
//            editor.putString("my_map", jsonString);
//            editor.commit();
//        }
//    }

    // STAAT OOK IN MQTTService dus wijzingen BIJ ALLEBEI DOORVOEREN
//    private Map<String, String> getMapSupportedGestures() {
//
//        Map<String, String> outputMap = new HashMap<String, String>();
//
//        SharedPreferences pSharedPref = getActivity().getSharedPreferences("com.yen.androidappthesisyen.system_id_to_supported_gestures", Context.MODE_PRIVATE);
//
//        try {
//            if (pSharedPref != null) {
//                String jsonString = pSharedPref.getString("my_map", (new JSONObject()).toString());
//                JSONObject jsonObject = new JSONObject(jsonString);
//                Iterator<String> keysItr = jsonObject.keys();
//                while (keysItr.hasNext()) {
//                    String key = keysItr.next();
//                    String value = (String) jsonObject.get(key); // a value = comma separated list of supported gestures for the specific systemID
//                    outputMap.put(key, value);
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return outputMap;
//    }


}
