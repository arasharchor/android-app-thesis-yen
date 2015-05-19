package com.yen.androidappthesisyen.ThreeDollarGestureRecognizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;

import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.mqtt.MQTTService;

import java.util.regex.Matcher;

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


        // MOET HIER NIET WANT STAAT AL STANDAARD INGEVULD MET EERDERE WAARDE. EN MOEST ER NOG GEEN WAARDE ZIJN WORDT ER EEN DEFAULT GEGEVEN.
        /*SharedPreferences settings = getSharedPreferences("com.yen.androidappthesisyen.gesture_handler", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ip_address", "192.168.1.1");
        editor.commit();*/


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
        menu.add(3,
                App.MENUITEMS.ITEM_INSERT_IP.ordinal(),
                3,
                "Insert IP Address");
        /*menu.add(3,
                App.MENUITEMS.ITEM_IP_USER_DETECTOR.ordinal(),
                3,
                "IP User Detector");
        menu.add(4,
                App.MENUITEMS.ITEM_IP_GESTURE_HANDLER.ordinal(),
                4,
                "IP Gesture Handler");*/

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

        } else if (value == App.MENUITEMS.ITEM_INSERT_IP.ordinal()) {

            showIPDialog();

        }
        /*else if (value == App.MENUITEMS.ITEM_IP_USER_DETECTOR.ordinal()) {

            showIPUserDetectorDialog();

        } else if (value == App.MENUITEMS.ITEM_IP_GESTURE_HANDLER.ordinal()) {

            showIPGestureHandlerDialog();

        }*/


        return super.onContextItemSelected(item);

    }

    private void showIPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location of User Detector and Gesture Handler");
        builder.setMessage("Insert the current IPv4 address");

        // TODO dit toepassen? WEL ALS WE BV. 2x EDITTEXT WENSEN ALS USER VERSCHILLENDE IPs ZOU KUNNEN INGEVEN.
//        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View view = inflater.inflate(R.layout.alert, null);
//        final EditText ipfield = (EditText) view.findViewById(R.id.ipfield);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER);

//        builder.setView(view);

        builder.setView(input);


        builder.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        // TODO IP User Detector en IP Gesture Handler zijn op dit ogenblik STEEDS GELIJK.
                        // Wordt verondersteld dat dit in toekomst ook zo is of niet?

                        String value = String.valueOf(input.getText());

                        // !! Patterns.IP_ADDRESS only applies to IPv4, so it's misleading.
                        // http://blog.danlew.net/2014/05/22/why-i-dont-use-patterns/
                        // TODO bedenken hoe IPv6 ook kan ondersteund worden? WRSL VIA CHECKBOX dat user kan aanvinken en afhankelijk daarvan wordt andere matcher gekozen.
                        // dus google op "android verify ipv6" ofzo
                        Matcher matcher = Patterns.IP_ADDRESS.matcher(value);

                        if(matcher.matches()){
                            // Only when the value is not empty, the value gets saved.

                            // TODO SERVICE MOET NU GEHERSTART WORDEN - OF BEDENK ANDERE WERKWIJZE ZODAT SERVICE NOG NIET IS GESTART VOORALEER JUISTE IP IN SYSTEEM ZIT.
                            SharedPreferences settingsUserDetector = getSharedPreferences("com.yen.androidappthesisyen.user_detector", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editorUserDetector = settingsUserDetector.edit();
                            editorUserDetector.putString("ip_address_broker", value);
                            // editor.putString("topic",  "accelstream/state"); // TODO dit hoeft op zich niet in Preference want is altijd hetzelfde? OF WEL DOEN OMDAT ZO GENERIEK IS?
                            editorUserDetector.commit();
                        }

                        // TODO doen we dit in alle gevallen? of moet er soms NIET gestopt worden na klikken op "save" bij bepaalde values?
                        // WE STOPPEN EERST DE SERVICE EN HERSTARTEN DAN. ZO KAN NIEUW IP DIRECT TOEGEPAST WORDEN :D
                        Intent svcOld = new Intent(getApplicationContext(), MQTTService.class);
                        stopService(svcOld);

                        // Dit BUITEN de IF lus: zelfs als er geen tekst werd ingevuld, wordt service gestart: die gebruikt dan het eerder opgeslagen IP.
                        // STARTING THE SERVICE NOW THAT THE IP ADDRESS OF THE BROKER IS KNOWN
                        // TODO getApplicationContext()is hier wrsl WEL OK want service best niet gelinkt aan een bepaalde activity?
                        Intent svcNew = new Intent(getApplicationContext(), MQTTService.class);
                        startService(svcNew);


                        if(matcher.matches()){
                            SharedPreferences settingsGestureHandler = getSharedPreferences("com.yen.androidappthesisyen.gesture_handler", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editorGestureHandler = settingsGestureHandler.edit();
                            editorGestureHandler.putString("ip_address", value);
                            editorGestureHandler.commit();
                        }


                    }
                });

        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {



        Log.w("debugging", "three dollar gesture ACTIVITY DESTROYED");

        super.onDestroy();
    }



    /*private void showIPUserDetectorDialog() {

    }

    private void showIPGestureHandlerDialog() {

    }*/


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
