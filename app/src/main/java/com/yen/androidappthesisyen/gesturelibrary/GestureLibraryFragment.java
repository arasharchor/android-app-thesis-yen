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
import com.yen.androidappthesisyen.advancedrecognizer.App;
import com.yen.androidappthesisyen.advancedrecognizer.GestureLibrary;

/**
 * A fragment representing a list of Items.
 */
public class GestureLibraryFragment extends ListFragment {


    private static final String LOG_TAG = GestureLibraryFragment.class.getName();


    public com.yen.androidappthesisyen.advancedrecognizer.App.STATES stateChange = App.STATES.STATE_LIBRARY;

    // TODO uitzetten of zelfs wissen?
    private boolean DEBUG = true;

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

        // TODO nodig in deze klasse?
        setRetainInstance(true);



        // points to the XML file specifying the content
        // UIT WNT PRECIES NIET NODIG IN EEN FRAGMENT (WEL IN EEN ACTIVITY) getActivity().setContentView(R.layout.dbui);


        stateChange = App.STATES.STATE_LIBRARY;



        // see if we have a gesture library
        // TODO eventueel deze if binnen de try/catch zetten indien nodig.
        if (GestureLibrary.GLibrarySingleInstance == null) {

            Log.w(LOG_TAG, "--------------------- NO GESTURE LIBRARY YET GestureLibraryFragment ---------------------");
            // TODO iets doen? popup? leeg venster met tekst? etc.


            // TODO je hebt hier TRY CATCH rond gezet want gaf error over SQL en close(): to fix.
            try {
                this.glibrary_instance = new GestureLibrary("GESTURES", getActivity());
            } catch (Exception ex) {

            }

            Log.w(LOG_TAG, "--------------------- nu gemaakt ---------------------");

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
        // TODO moet dat niet onClickListener zijn?
        mainButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP)

                {
                    // Later add a confirmation dialog here!
//                    Log.w("OnTouch", "Deleting Gestures in Library");

                    glibrary_instance.removeAllGesturesFromLibrary();
                }
                // "return false" staat ook bij een ontouchlistener bij AdvancedFragment dus NIET WEGDOEN!
                return false;
            }
        });



        return returnedView;
    }



    public void initListView() {
        /* Inits the list view displaying gesture ids and counts */
        // see if we have a gesture library instance
        String[] gestureIDStrings;
        if (this.glibrary_instance != null) {
            /* initialize the list view */
            gestureIDStrings = this.glibrary_instance.getAllGestureTitles();
        } else {
            // WE SHOULD NEVER ARRIVE HERE.
            gestureIDStrings = new String[]{getResources().getString(R.string.error_gesture_library)};
        }

        setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, gestureIDStrings));
    }


    @Override
    public void onStart() {
        super.onStart();

        // TODO mag wrsl weg wnt niet echt relevant?
        stateChange = App.STATES.STATE_LIBRARY;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

//        if (null != mListener) {
//            // Notify the active callbacks interface (the activity, if the
//            // fragment is attached to one) that an item has been selected.
//            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
//        }


    }






}
