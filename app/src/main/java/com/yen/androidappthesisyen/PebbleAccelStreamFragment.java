package com.yen.androidappthesisyen;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.util.UUID;


/* SOURCE: partially based on https://github.com/C-D-Lewis/accelstream-android */


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PebbleAccelStreamFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class PebbleAccelStreamFragment extends Fragment {

//    private OnFragmentInteractionListener mListener;


    //Constants
    public static final String TAG = PebbleAccelStreamFragment.class.getName();
    private static final int NUM_SAMPLES = 15;
    private static final int GRAPH_HISTORY = 200;

    //State
    private int sampleCount = 0;
    private long lastAverageTime = 0;
    private int[] latest_data;
    private GraphViewSeries seriesX, seriesY, seriesZ;
    private int sampleCounter = 0;
    private int totalData = 0;

    //Layout members
    private TextView
            xView,
            yView,
            zView,
            rateView;
    private Button startButton;
    private GraphView gView;

    //Other members
    private PebbleKit.PebbleDataReceiver receiver;


    // UUID van originele accel_stream:
//    private UUID uuid = UUID.fromString("2893b0c4-2bca-4c83-a33a-0ef6ba6c8b17");
    private UUID uuid = UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4");

    private Handler handler = new Handler();


    public PebbleAccelStreamFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // TODO nodig? of gwn standaard bij alle fragments zetten?
        setRetainInstance(true);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_pebble_accel_stream, container, false);


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

        //Graph
        seriesX = new GraphViewSeries("X", new GraphViewSeries.GraphViewSeriesStyle(Color.argb(255, 255, 0, 0), 2), new GraphView.GraphViewData[]{
                new GraphView.GraphViewData(1, 0)
        });
        seriesY = new GraphViewSeries("Y", new GraphViewSeries.GraphViewSeriesStyle(Color.argb(255, 0, 255, 0), 2), new GraphView.GraphViewData[]{
                new GraphView.GraphViewData(1, 0)
        });
        seriesZ = new GraphViewSeries("Z", new GraphViewSeries.GraphViewSeriesStyle(Color.argb(255, 0, 0, 255), 2), new GraphView.GraphViewData[]{
                new GraphView.GraphViewData(1, 0)
        });

        gView = new LineGraphView(getActivity(), getResources().getString(R.string.label_pebble_accelerometer_history));
        gView.setShowLegend(true);
        gView.setViewPort(0, GRAPH_HISTORY);
        gView.setScrollable(true);
        gView.addSeries(seriesX);
        gView.addSeries(seriesY);
        gView.addSeries(seriesZ);

        LinearLayout layout = (LinearLayout) returnedView.findViewById(R.id.graph_layout);
        layout.addView(gView);


        return returnedView;
    }


    @Override
    public void onResume() {
        super.onResume();


        getToggleStatesAndEnableServices();


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
//                    Log.w(TAG, "NEW DATA PACKET");
                    for (int i = 0; i < NUM_SAMPLES; i++) {
                        for (int j = 0; j < 3; j++) {
                            try {
                                latest_data[(3 * i) + j] = data.getInteger((3 * i) + j).intValue();
                            } catch (Exception e) {
                                latest_data[(3 * i) + j] = -1;
                            }
                        }
//                        Log.w(TAG, "Sample " + i + " data: X: " + latest_data[(3 * i)] + ", Y: " + latest_data[(3 * i) + 1] + ", Z: " + latest_data[(3 * i) + 2]);
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
                        /* TODO fixable of gewoon negeren?
                        * if (values.length > 0 && value.getX() < values[values.length-1].getX()) {
        throw new IllegalArgumentException("new x-value must be greater then the last value. x-values has to be ordered in ASC.");
    } */
                        try {
                            seriesX.appendData(new GraphView.GraphViewData(sampleCounter, latest_data[(3 * i)]), true, GRAPH_HISTORY);
                            seriesY.appendData(new GraphView.GraphViewData(sampleCounter, latest_data[(3 * i) + 1]), true, GRAPH_HISTORY);
                            seriesZ.appendData(new GraphView.GraphViewData(sampleCounter, latest_data[(3 * i) + 2]), true, GRAPH_HISTORY);
                        } catch (IllegalArgumentException ex) {
                            // DO... NJET
                        }
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

    @Override
    public void onPause() {
        super.onPause();

        disableAllServices();

    }

    private void disableAllServices() {

        stopPebbleDataStream();
        // TODO voeg stop-methods van andere services toe.

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
