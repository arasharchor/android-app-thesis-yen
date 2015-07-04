package com.yen.androidappthesisyen.tiltdirectionrecognizer;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.yen.androidappthesisyen.R;
















// IS OUD EN MAG WEG
















public class PebbleAccelStreamActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_pebble_accel_stream);


        // TODO werkt niet?!
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container_activity_pebble_accel_stream, new PebbleAccelStreamFragment())
                    .commit();
        }



        /* TODO zorgen dat de TABS in deze activity ook te zien zijn!
        * Maar neem aan dat dit niet zomaar copy/paste mag zijn van code in de MainActivity.
         * Je zult het moeten erven ofzo wrsl? */


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pebble_accel_stream, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // TODO zien of dit nodig is.

        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
