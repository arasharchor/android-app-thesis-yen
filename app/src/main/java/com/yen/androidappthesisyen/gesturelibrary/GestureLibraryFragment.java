package com.yen.androidappthesisyen.gesturelibrary;

import android.app.ListFragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.advancedrecognizer.UsedConstants;

/**
 * A fragment representing a list of Items.
 */
public class GestureLibraryFragment extends ListFragment {


    private static final String LOG_TAG = GestureLibraryFragment.class.getName();


    public UsedConstants.STATES stateChange = UsedConstants.STATES.STATE_LIBRARY;

    private boolean DEBUG = false;

    private GestureLibrary glibrary_instance = null;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GestureLibraryFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        stateChange = UsedConstants.STATES.STATE_LIBRARY;



        // see if we have a gesture library
        // TODO eventueel deze if binnen de try/catch zetten indien nodig.
        if (GestureLibrary.GLibrarySingleInstance == null) {

            Log.w(LOG_TAG, "--------------------- NO GESTURE LIBRARY YET ---------------------");


            // TODO je hebt hier TRY CATCH rond gezet want gaf error over SQL en close(): to fix.
            try {
                this.glibrary_instance = new GestureLibrary("GESTURES", getActivity());
            } catch (Exception ex) {

            }

            Log.w(LOG_TAG, "--------------------- Created gesture library instance ---------------------");

        } else {

            this.glibrary_instance = GestureLibrary.GLibrarySingleInstance;
        }


        // initialize the list view
        this.initListView();


        // weet niet of in deze klasse ook nodig is but better safe than sorry.
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        DEFAULT return super.onCreateView(inflater, container, savedInstanceState);

// Inflate the layout for this fragment
        View returnedView = inflater.inflate(R.layout.dbui, container, false);

        // button to delete all gestures
        final Button mainButton = (Button) returnedView.findViewById(R.id.deleteGesturesButton);

        mainButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP)

                {

                    glibrary_instance.removeAllGesturesFromLibrary();
                }

                return false;
            }
        });



        return returnedView;
    }



    public void initListView() {
        /* Inits the list view displaying gesture ids and counts */
        // see if we have a gesture library instance
        String[] arrayGestureTitles;
        if (this.glibrary_instance != null) {
            /* initialize the list view */
            arrayGestureTitles = this.glibrary_instance.getAllGestureTitles();
        } else {
            // WE SHOULD NEVER ARRIVE HERE.
            arrayGestureTitles = new String[]{getResources().getString(R.string.error_gesture_library)};
        }

        setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, arrayGestureTitles));
    }


    @Override
    public void onStart() {
        super.onStart();

        // TODO mag wrsl weg wnt niet echt relevant?
        stateChange = UsedConstants.STATES.STATE_LIBRARY;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);


    }






}
