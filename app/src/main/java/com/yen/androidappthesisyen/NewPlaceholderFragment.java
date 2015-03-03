package com.yen.androidappthesisyen;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NewPlaceholderFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */

// TODO you have disabled some methods. Reenable them if you need them.

public class NewPlaceholderFragment extends Fragment {

//    private OnFragmentInteractionListener mListener;

    public NewPlaceholderFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.fragment_new_placeholder, container, false);



        // If you do it in onCreate it seems to crash! (Even though the view has been inflated using setContentView(...) beforehand.
        ToggleButton testToggle = (ToggleButton) returnedView.findViewById(R.id.toggleButton_pebble_acceldata_stream);
//        testToggle.setEnabled(false);



        return returnedView;
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
