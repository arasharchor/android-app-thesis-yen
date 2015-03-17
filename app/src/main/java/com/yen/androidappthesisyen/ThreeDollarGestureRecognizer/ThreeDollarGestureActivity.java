package com.yen.androidappthesisyen.ThreeDollarGestureRecognizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.yen.androidappthesisyen.R;

public class ThreeDollarGestureActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_three_dollar_gesture);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container_three_dollar_gesture_activity, new ThreeDollarGestureFragment())
                    .commit();
        }


        getActionBar().setDisplayHomeAsUpEnabled(true);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_three_dollar_gesture, menu);
//        return true;


        menu.add(0, // group
                App.MENUITEMS.ITEM_LEARN.ordinal(), // item id
                0, // order id
                "Train");
        menu.add(1,
                App.MENUITEMS.ITEM_RECOGNIZE.ordinal(),
                1,
                "Recognize");
        menu.add(2,
                App.MENUITEMS.ITEM_LIBRARY.ordinal(),
                2,
                "Gesture Library");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);


        /*
         * React to selection of options item
    	 */
        // stond "DEBUG"
        if (((ThreeDollarGestureFragment) getFragmentManager().findFragmentById(R.id.container_three_dollar_gesture_activity)).DEBUG)
            Log.w("onOptionsItemSelected", "Selected: " + item.getItemId());

        App.MENUITEMS m;

        m = App.MENUITEMS.ITEM_LEARN;

        int value = item.getItemId();

        if (value == App.MENUITEMS.ITEM_LEARN.ordinal()) {
            // do something here
            ((ThreeDollarGestureFragment) getFragmentManager().findFragmentById(R.id.container_three_dollar_gesture_activity)).state = App.STATES.STATE_LEARN;
            ((ThreeDollarGestureFragment) getFragmentManager().findFragmentById(R.id.container_three_dollar_gesture_activity)).stateChanged();

        } else if (value == App.MENUITEMS.ITEM_RECOGNIZE.ordinal()) {
            // activate recognition here
            ((ThreeDollarGestureFragment) getFragmentManager().findFragmentById(R.id.container_three_dollar_gesture_activity)).state = App.STATES.STATE_RECOGNIZE;
            ((ThreeDollarGestureFragment) getFragmentManager().findFragmentById(R.id.container_three_dollar_gesture_activity)).stateChanged();
        } else if (value == App.MENUITEMS.ITEM_LIBRARY.ordinal()) {
            // library stats here
            ((ThreeDollarGestureFragment) getFragmentManager().findFragmentById(R.id.container_three_dollar_gesture_activity)).state = App.STATES.STATE_LIBRARY;
            ((ThreeDollarGestureFragment) getFragmentManager().findFragmentById(R.id.container_three_dollar_gesture_activity)).stateChanged();

            // start library activity
            Intent i = new Intent(this.getApplicationContext(), DBManagerUIActivity.class);
            startActivityForResult(i, 0);
        }


        return super.onContextItemSelected(item);

    }

    /**
     * A placeholder fragment containing a simple view.
     */
//    public static class PlaceholderFragment extends Fragment {
//
//        public PlaceholderFragment() {
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                                 Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_three_dollar_gesture, container, false);
//            return rootView;
//        }
//    }
}
