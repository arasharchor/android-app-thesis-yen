package com.yen.androidappthesisyen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;


public class PebbleGestureRecognitionActivity extends Activity {

    private static final String LOG_TAG = PebbleGestureRecognitionActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pebble_gesture_recognition);


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container_pebble_gesture_recognition, new PebbleGestureRecognitionFragment())
                    .commit();
        }


        getActionBar().setDisplayHomeAsUpEnabled(true);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_pebble_gesture_recognition, menu);
//        return true;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


//        int id = item.getItemId();
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);


        switch (item.getItemId()) {
            case R.id.edit_gestures:

                Intent editGesturesIntent = new Intent().setClass(this, PebbleGestureOverviewActivity.class);
                // stond activeTrainingSet (2e param)
                editGesturesIntent.putExtra("trainingSetName", ((PebbleGestureRecognitionFragment) getFragmentManager().findFragmentById(R.id.container_pebble_gesture_recognition)).activeTrainingSet);
                startActivity(editGesturesIntent);
                return true;

            default:
                return false;
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
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
//            View rootView = inflater.inflate(R.layout.fragment_pebble_gesture_recognition, container, false);
//            return rootView;
//        }
//    }
}
