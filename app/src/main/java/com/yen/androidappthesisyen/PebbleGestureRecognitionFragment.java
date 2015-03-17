package com.yen.androidappthesisyen;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

import de.dfki.ccaal.gestures.IGestureRecognitionListener;
import de.dfki.ccaal.gestures.IGestureRecognitionService;
import de.dfki.ccaal.gestures.classifier.Distribution;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PebbleGestureRecognitionFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class PebbleGestureRecognitionFragment extends Fragment {

//    private OnFragmentInteractionListener mListener;


    private static final String LOG_TAG = PebbleGestureRecognitionFragment.class.getName();

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


    // DIT IS VOOR DIE MEER SIMPELE GESTURE DETECTION DAT DIRECT OP PEBBLE KAN.
    private PebbleGestureModelImplementation test;


    // voor COMPLEXERE gesture detection
    IGestureRecognitionService recognitionService;
    // TODO ff public gezet om eraan te geraken vanin activity dat dit fragment heeft.
    public String activeTrainingSet;


    // DIT IS VOOR DIE MEER SIMPELE GESTURE DETECTION DAT DIRECT OP PEBBLE KAN.
    private class PebbleGestureModelImplementation extends PebbleGestureModel {


        /**
         * Object that manages Pebble wrist movement gestures.
         * The user should extend their wrist with the fist pointing directly out from the chest as if to punch, with the watch's face pointing upwards
         *
         * @param threshold            Value from 0 to 4000 above which an action on an axis will be triggered
         * @param durationMilliseconds Minimum time between gestures in milliseconds
         * @param modeConstant         Mode constant from this class for FLICK or TILT operation
         */
        public PebbleGestureModelImplementation(int threshold, long durationMilliseconds, int modeConstant) {
            super(threshold, durationMilliseconds, modeConstant);
        }

        @Override
        public void onWristLeft() {
            // TODO
//            Log.w(LOG_TAG, "wrist LEFT <--");
        }

        @Override
        public void onWristRight() {
            // TODO
//            Log.w(LOG_TAG, "wrist RIGHT -->");
        }

        @Override
        public void onWristUp() {
            // TODO
//            Log.w(LOG_TAG, "wrist UP ^^");
        }

        @Override
        public void onWristDown() {
            // TODO
//            Log.w(LOG_TAG, "wrist DOWN __");
        }


        /**
         * When an action ends - fired after duration
         */
        @Override
        public void onActionEnd() {
            // TODO
//            Log.w(LOG_TAG, "onActionEnd()");
        }
    }


    public PebbleGestureRecognitionFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);


        // TODO werkt niet ook al geraken we hier!
        // de andere gelijkaardige regels in andere klassen, werken wel!
        // (en btw, de code hier hoort hier niet te staan - wel in onResume en onPause - maar is als test.
        Log.w(LOG_TAG, "arrived here!");

        // voor starten app voor ACCEL DATA STREAM.
        PebbleKit.startAppOnPebble(getActivity(), uuid);

        // voor SIMPELERE gesture detection
        // TODO modes
        // TODO experimenteren met de params
        test = new PebbleGestureModelImplementation(700, 500, PebbleGestureModel.MODE_FLICK);


        // voor COMPLEXERE gesture detection


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_pebble_gesture_recognition, container, false);


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


        // voor COMPLEXERE gesture detection
        final TextView activeTrainingSetText = (TextView) returnedView.findViewById(R.id.activeTrainingSet);
        final EditText trainingSetText = (EditText) returnedView.findViewById(R.id.trainingSetName);
        final EditText editText = (EditText) returnedView.findViewById(R.id.gestureName);
        activeTrainingSet = editText.getText().toString();
        final Button startTrainButton = (Button) returnedView.findViewById(R.id.trainButton);
        final Button deleteTrainingSetButton = (Button) returnedView.findViewById(R.id.deleteTrainingSetButton);
        final Button changeTrainingSetButton = (Button) returnedView.findViewById(R.id.startNewSetButton);
        final SeekBar seekBar = (SeekBar) returnedView.findViewById(R.id.seekBar1);
        // TODO is mee experimenteren
//        seekBar.setVisibility(View.INVISIBLE);
        seekBar.setMax(20);
        // staat dus standaard op maximum
        seekBar.setProgress(20);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                try {
                    recognitionService.setThreshold(progress / 10.0f);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });

        startTrainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recognitionService != null) {
                    try {
                        if (!recognitionService.isLearning()) {
                            startTrainButton.setText("Stop Training");
                            editText.setEnabled(false);
                            deleteTrainingSetButton.setEnabled(false);
                            changeTrainingSetButton.setEnabled(false);
                            trainingSetText.setEnabled(false);
                            recognitionService.startLearnMode(activeTrainingSet, editText.getText().toString());
                        } else {
                            startTrainButton.setText("Start Training");
                            editText.setEnabled(true);
                            deleteTrainingSetButton.setEnabled(true);
                            changeTrainingSetButton.setEnabled(true);
                            trainingSetText.setEnabled(true);
                            recognitionService.stopLearnMode();
                        }
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
        changeTrainingSetButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                activeTrainingSet = trainingSetText.getText().toString();
                activeTrainingSetText.setText(activeTrainingSet);

                if (recognitionService != null) {
                    try {
                        recognitionService.startClassificationMode(activeTrainingSet);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        deleteTrainingSetButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // stond GestureTrainer.this
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("You really want to delete the training set?").setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (recognitionService != null) {
                            try {
                                recognitionService.deleteTrainingSet(activeTrainingSet);
                            } catch (RemoteException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
            }
        });


        return returnedView;
    }


//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
////        super.onCreateOptionsMenu(menu, inflater);
//
//        inflater.inflate(R.menu.options_menu, menu);
////        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.edit_gestures:
//
//                Intent editGesturesIntent = new Intent().setClass(getActivity(), PebbleGestureOverviewActivity.class);
//                editGesturesIntent.putExtra("trainingSetName", activeTrainingSet);
//                startActivity(editGesturesIntent);
//                return true;
//
//            default:
//                return false;
//        }
//    }

    @Override
    public void onResume() {

        // voor (o.a.) PEBBLE ACCEL STREAM
        getToggleStatesAndEnableServices();

        // voor COMPLEXE gesture detection
        Intent bindIntent = new Intent("de.dfki.ccaal.gestures.GESTURE_RECOGNIZER");
        getActivity().bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        // TODO mag dit wel: dat super NIET als eerste regel wordt aangeroepen?
        super.onResume();


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


                    // ------ TEST - voor MOEILIJKERE gesture detection

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
                    // ------ TEST - voor MOEILIJKERE gesture detection


                    // ------ TEST - voor SIMPELE gesture detection
                    // TODO plek van deze code is veranderen!
                    // TODO alsook de para's
                    PebbleAccelPacket pebbleAccelPacket = new PebbleAccelPacket(latest_data[0], latest_data[1], latest_data[2]);
                    test.update(pebbleAccelPacket);

                    // ------ TEST - voor SIMPELE gesture detection


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

        // voor COMPLEXE gesture detection
        try {
            recognitionService.unregisterListener(IGestureRecognitionListener.Stub.asInterface(gestureListenerStub));
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        recognitionService = null;
        getActivity().unbindService(serviceConnection);
        // TODO mag dit wel: dat super NIET als eerste regel wordt aangeroepen?
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


    // voor COMPLEXERE gesture detection
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            recognitionService = IGestureRecognitionService.Stub.asInterface(service);

            Log.w("TEST", "--------------- recognitionService aangemaakt");

            try {
                recognitionService.startClassificationMode(activeTrainingSet);
                recognitionService.registerListener(IGestureRecognitionListener.Stub.asInterface(gestureListenerStub));
            } catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            recognitionService = null;
        }
    };

    IBinder gestureListenerStub = new IGestureRecognitionListener.Stub() {

        // 1e param FINAL gemaakt: TODO test of nog werkt
        @Override
        public void onGestureLearned(final String gestureName) throws RemoteException {
            // 2de antwoord @ https://stackoverflow.com/questions/3875184/cant-create-handler-inside-thread-that-has-not-called-looper-prepare
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), String.format("Gesture %s learned", gestureName), Toast.LENGTH_SHORT).show();
                    System.err.println("Gesture %s learned");
                }
            });

        }

        @Override
        public void onTrainingSetDeleted(final String trainingSet) throws RemoteException {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), String.format("Training set %s deleted", trainingSet), Toast.LENGTH_SHORT).show();
                    System.err.println(String.format("Training set %s deleted", trainingSet));
                }
            });
        }

        @Override
        public void onGestureRecognized(final Distribution distribution) throws RemoteException {
            // HIER STOND HET AL STANDAARD :D
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), String.format("%s: %f", distribution.getBestMatch(), distribution.getBestDistance()), Toast.LENGTH_LONG).show();
                    System.err.println(String.format("%s: %f", distribution.getBestMatch(), distribution.getBestDistance()));
                }
            });
        }
    };


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
