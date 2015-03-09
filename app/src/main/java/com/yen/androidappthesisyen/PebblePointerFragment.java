package com.yen.androidappthesisyen;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.UUID;



/* SOURCE: partially based on https://github.com/foldedtoad/PebblePointer/tree/master/android-app*/



/* WERKT :D
* Moet just neig met pols draaien! */


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PebblePointerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class PebblePointerFragment extends Fragment {

//    private OnFragmentInteractionListener mListener;


    private static final String TAG = "PebblePointer";

    // Het heeft niet dat meerdere keys dezelfde int waarden heeft he! Toch niet als ze tot verschillende collecties/doeleinden behoren!
    // The tuple key corresponding to a vector received from the watch
    private static final int PP_KEY_CMD = 128;
    private static final int PP_KEY_X   = 1;
    private static final int PP_KEY_Y   = 2;
    private static final int PP_KEY_Z   = 3;

    @SuppressWarnings("unused")
    private static final int PP_CMD_INVALID = 0;
    private static final int PP_CMD_VECTOR  = 1;

    public static final int VECTOR_INDEX_X  = 0;
    public static final int VECTOR_INDEX_Y  = 1;
    public static final int VECTOR_INDEX_Z  = 2;

    private static int vector[] = new int[3];

    private PebbleKit.PebbleDataReceiver dataReceiver;

    // This UUID identifies the PebblePointer app.
    private static final UUID PEBBLEPOINTER_UUID = UUID.fromString("273761eb-97dc-4f08-b353-3384a2170902");

    private static final int SAMPLE_SIZE = 30;

    private XYPlot dynamicPlot = null;

    SimpleXYSeries xSeries = null;
    SimpleXYSeries ySeries = null;
    SimpleXYSeries zSeries = null;



    private final Handler handler = new Handler();


    public PebblePointerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO nodig? of gwn standaard bij alle fragments zetten?
        setRetainInstance(true);



        vector[VECTOR_INDEX_X] = 0;
        vector[VECTOR_INDEX_Y] = 0;
        vector[VECTOR_INDEX_Z] = 0;

        PebbleKit.startAppOnPebble(getActivity(), PEBBLEPOINTER_UUID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_pebble_pointer, container, false);


        dynamicPlot = (XYPlot) returnedView.findViewById(R.id.dynamicPlot);


        dynamicPlot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
        dynamicPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);

        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0.0"));
        dynamicPlot.getGraphWidget().setRangeValueFormat(new DecimalFormat("0"));

        dynamicPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);

        dynamicPlot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        dynamicPlot.setTicksPerDomainLabel(1);
        dynamicPlot.setTicksPerRangeLabel(1);

        dynamicPlot.getGraphWidget().getDomainLabelPaint().setTextSize(30);
        dynamicPlot.getGraphWidget().getRangeLabelPaint().setTextSize(30);

        dynamicPlot.getGraphWidget().setDomainLabelWidth(40);
        dynamicPlot.getGraphWidget().setRangeLabelWidth(80);

        dynamicPlot.setDomainLabel("time");
        dynamicPlot.getDomainLabelWidget().pack();

        dynamicPlot.setRangeLabel("G-force");
        dynamicPlot.getRangeLabelWidget().pack();

        dynamicPlot.setRangeBoundaries(-1024, 1024, BoundaryMode.FIXED);
        dynamicPlot.setDomainBoundaries(0, SAMPLE_SIZE, BoundaryMode.FIXED);


        xSeries = new SimpleXYSeries("X-axis");
        xSeries.useImplicitXVals();

        ySeries = new SimpleXYSeries("Y-axis");
        ySeries.useImplicitXVals();

        zSeries = new SimpleXYSeries("Z-axis");
        zSeries.useImplicitXVals();

        // Blue line for X axis.
        LineAndPointFormatter fmtX = new LineAndPointFormatter(Color.BLUE, null, null, null);
        dynamicPlot.addSeries(xSeries, fmtX);

        // Green line for Y axis.
        LineAndPointFormatter fmtY = new LineAndPointFormatter(Color.GREEN, null, null, null);;
        dynamicPlot.addSeries(ySeries, fmtY);

        // Red line for Z axis.
        LineAndPointFormatter fmtZ = new LineAndPointFormatter(Color.RED, null, null, null);
        dynamicPlot.addSeries(zSeries, fmtZ);




        return returnedView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // TODO als blijkt dat je finaal toch met DEZE code implementatie ga werken,
        // pas dan getToggleStatesAndEnableServices(); enzo toe.
        // +
        // check op (dataReceiver == null) enzo



        dataReceiver = new PebbleKit.PebbleDataReceiver(PEBBLEPOINTER_UUID) {

            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary dict) {

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        PebbleKit.sendAckToPebble(context, transactionId);

                        final Long cmdValue = dict.getInteger(PP_KEY_CMD);
                        if (cmdValue == null) {
                            return;
                        }

                        if (cmdValue.intValue() == PP_CMD_VECTOR) {

                            // Capture the received vector.
                            final Long xValue = dict.getInteger(PP_KEY_X);
                            if (xValue != null) {
                                vector[VECTOR_INDEX_X] = xValue.intValue();
                            }

                            final Long yValue = dict.getInteger(PP_KEY_Y);
                            if (yValue != null) {
                                vector[VECTOR_INDEX_Y] = yValue.intValue();
                            }

                            final Long zValue = dict.getInteger(PP_KEY_Z);
                            if (zValue != null) {
                                vector[VECTOR_INDEX_Z] = zValue.intValue();
                            }

                            // Update the user interface.
                            updateUI(getView());
                        }
                    }
                });
            }
        };

        PebbleKit.registerReceivedDataHandler(getActivity(), dataReceiver);


    }


    // TODO private?
    public void updateUI(View returnedView) {

        final String x = String.format(Locale.getDefault(), "X: %d", vector[VECTOR_INDEX_X]);
        final String y = String.format(Locale.getDefault(), "Y: %d", vector[VECTOR_INDEX_Y]);
        final String z = String.format(Locale.getDefault(), "Z: %d", vector[VECTOR_INDEX_Z]);

        // Update the numerical fields

        TextView x_axis_tv = (TextView) returnedView.findViewById(R.id.x_axis_Text);
        x_axis_tv.setText(x);

        TextView y_axis_tv = (TextView) returnedView.findViewById(R.id.y_axis_Text);
        y_axis_tv.setText(y);

        TextView z_axis_tv = (TextView) returnedView.findViewById(R.id.z_axis_Text);
        z_axis_tv.setText(z);

        // Update the Plot

        // Remove oldest vector data.
        if (xSeries.size() > SAMPLE_SIZE) {
            xSeries.removeFirst();
            ySeries.removeFirst();
            zSeries.removeFirst();
        }

        // Add the latest vector data.
        xSeries.addLast(null, vector[VECTOR_INDEX_X]);
        ySeries.addLast(null, vector[VECTOR_INDEX_Y]);
        zSeries.addLast(null, vector[VECTOR_INDEX_Z]);

        // Redraw the Plots.
        dynamicPlot.redraw();
    }



    @Override
    public void onPause() {
        super.onPause();


        if (dataReceiver != null) {
            getActivity().unregisterReceiver(dataReceiver);
            dataReceiver = null;
        }
        PebbleKit.closeAppOnPebble(getActivity(), PEBBLEPOINTER_UUID);

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
