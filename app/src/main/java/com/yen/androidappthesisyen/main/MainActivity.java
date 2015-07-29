package com.yen.androidappthesisyen.main;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.advancedrecognizer.AdvancedFragment;
import com.yen.androidappthesisyen.gesturelibrary.GestureListFragment;
import com.yen.androidappthesisyen.pushnotificationlistener.MQTTService;


public class MainActivity extends Activity implements ActionBar.TabListener {

    private static final String LOG_TAG = MainActivity.class.getName();

    // FOR REFRESH ICON
    private final Handler handler = new Handler();


    private static final int ID_action_pebble_companion_app_install_run = 1;


    /* FOR MQTT SERVICE */
    private StatusUpdateReceiver statusUpdateIntentReceiver;
    private MQTTMessageReceiver messageIntentReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);


        // !! "R.id.framelayout_container_main_activity" werd gedefinieerd
        // in "R.layout.activity_main" !
        if (savedInstanceState == null) {

            // DONT PLACE THIS FRAGMENT IN A GLOBAL VARIABLE. Since fragments get made and remade upon orientation changes and stuff!
            // So that variable could become NULL at some point!
            Fragment aPlaceholderFragment = new MainFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.framelayout_container_main_activity, aPlaceholderFragment)
                    .commit();
        }

        final ActionBar theActionBar = getActionBar();

        // FALSE because ROOT ACTIVITY
        theActionBar.setDisplayHomeAsUpEnabled(false);


        // TODO hoort dit niet in onCreateOptionsMenu ? UPDATE is goed volgens tutorial @ https://developer.android.com/training/implementing-navigation/lateral.html#tabs
        // TODO inflate the tab layout by using XML files instead of coding it here.
        addNavigationTabs(theActionBar);





        /* REGARDING THE MQTT SERVICE */
        // TODO juiste plek om het hier te zetten?
        /*SharedPreferences settings = getSharedPreferences("com.yen.androidappthesisyen.user_detector", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ip_address_broker_1", "192.168.1.1");
        editor.putString("ip_address_broker_2", "192.168.1.2");
        editor.putString("topic_accelstream",  "accelstream/state"); // TODO dit hoeft op zich niet in Preference want is altijd hetzelfde? OF WEL DOEN OMDAT ZO GENERIEK IS?
        editor.putString("topics_gesturepusher",  "gesturepusher/#"); // TODO dit hoeft op zich niet in Preference want is altijd hetzelfde? OF WEL DOEN OMDAT ZO GENERIEK IS?
        editor.commit();
        Log.w(LOG_TAG, "--------- arriveerden in MainActivity net na editor.commit()");*/


        statusUpdateIntentReceiver = new StatusUpdateReceiver();
        IntentFilter intentSFilter = new IntentFilter(MQTTService.MQTT_STATUS_INTENT);
        registerReceiver(statusUpdateIntentReceiver, intentSFilter);

        messageIntentReceiver = new MQTTMessageReceiver();
        IntentFilter intentCFilter = new IntentFilter(MQTTService.MQTT_MSG_RECEIVED_INTENT);
        registerReceiver(messageIntentReceiver, intentCFilter);


        // SERVICE NU PAS GESTART NADAT IP ADRES WERD INGEGEVEN.
//        Intent svc = new Intent(this, MQTTService.class);
//        startService(svc);
//        Log.w(LOG_TAG, "--------- arriveerden in MainActivity net na startService(svc)");


        // TODO dit wrsl WEL goede plek want moet werken globaal over alle fragments?
//        To detect Pebble being (dis)connected.
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(BTReceiver, filter1);
        registerReceiver(BTReceiver, filter2);
        registerReceiver(BTReceiver, filter3);


        // Certain apps need to keep the screen turned on, such as games or movie apps. The best way to do this is to use the FLAG_KEEP_SCREEN_ON in your activity (and only in an activity, never in a service or other app component).
        // From https://developer.android.com/training/scheduling/wakelock.html
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }


    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Device found
                // TODO?
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                // Device is now connected
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.pebble_connected_toast, Toast.LENGTH_LONG).show();
                    }
                });
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Done searching
                // TODO?
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                // Device is about to disconnect
                // TODO?
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // Device has disconnected
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.pebble_not_connected_toast, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    };


    // Following 3 methods are due to "implements ActionBar.TabListener"
    // TODO tabs are probably meant to CHANGE THE LAYOUT (change activity or fragment) while the BUTTONS on the left of the OVERFLOW BUTTON are probably for ACTIONS on the current layout
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO dit aangewezen manier voor vinden juiste tab?
        // Don't check == 0 because that tab 0 is SELECTED by DEFAULT when starting the app.
        // So tab 0 corresponds with the MainActivity!
        if (tab.getPosition() == 0) {
            switchToMainFragment();
        } else if (tab.getPosition() == 1) {
            switchToAdvancedRecognizerFragment("learn");
        } else if (tab.getPosition() == 2) {
            switchToAdvancedRecognizerFragment("recognize");
        }

    }


    private void switchToMainFragment() {

        // Create new fragment and transaction
        FragmentTransaction transaction = getFragmentManager().beginTransaction();


        Fragment fragment = new MainFragment();
        // Nu GEEN BUNDLE NODIG MET STATE want het STATE systeem geldt enkel bij de learn/recognize/library state omdat die dezelfde "ouderfragment" hebben die telkens wordt aangepast.
        // De MainActivity en MainFragment hebben hier niets mee te zien.

        // Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack if needed
        transaction.replace(R.id.framelayout_container_main_activity, fragment);
        /*Note: When you remove or replace a fragment and add the transaction to the back stack, the fragment that is removed is stopped (not destroyed). If the user navigates back to restore the fragment, it restarts. If you do not add the transaction to the back stack, then the fragment is destroyed when removed or replaced. To allow the user to navigate backward
        through the fragment transactions, you must call addToBackStack() before you commit the FragmentTransaction.*/
//        http://sapandiwakar.in/replacing-fragments/
        transaction.addToBackStack(null);

// Commit the transaction
        transaction.commit();

    }

    // 16-07 mag weg
    private void toSecondTabActivity() {
        // TODO eventueel nieuwe tab nog gebruiken voor iets
//        Intent intent = new Intent(this, PebbleAccelStreamActivity.class);
//        startActivity(intent);

        switchToAdvancedRecognizerFragment("learn");

    }

    // TODO 16-07 VOLGENDE MAG WEG MAAR LEES NOG IS COMMENTS EERST.
    /*TODO perhaps: You can also declare the click event handler programmatically rather than in an XML layout. This might be necessary if you instantiate the Button at runtime or you need to declare the click behavior in a Fragment subclass.
     https://developer.android.com/guide/topics/ui/controls/button.html
      So put the following code in the FRAGMENT since it's the FRAGMENT that builds the GUI; not the ACTIVITY in our case. */
    // UPDATE: maar dan moet je de states van de tabs continu tussen de fragments doorgeven. Ipv nu: nu heeft de MainActivity de tab logica en zit daar dus de state.
    // TODO ----------- er stond public void toGestureRecognizerActivity(View view) { maar die parameter precies nooit gebruikt?
    public void toGestureRecognizerActivity() {
//        Intent intent = new Intent(this, AdvancedActivity.class);
//        startActivity(intent);
        // TODO bovenstaande is oud: we BLIJVEN deze MainActivity gebruiken maar switchen gewoon van Fragment:

        switchToAdvancedRecognizerFragment("recognize");
    }

    private void switchToAdvancedRecognizerFragment(String requestedState) {
        // Create new fragment and transaction
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // OM DATA DOOR TE GEVEN ONDER FRAGMENTS ONDERLING.
        // In dit geval dus Bundle OK want SharedPreferences is voor ECHTE PREFERENTIES DIE LANGDURIGER BLIJVEN!
        Fragment fragment = new AdvancedFragment();
        Bundle bundle = new Bundle();
        if (requestedState.equalsIgnoreCase("learn")) {
            bundle.putString("state", "learn");
        } else if (requestedState.equalsIgnoreCase("recognize")) {
            bundle.putString("state", "recognize");
        } else if (requestedState.equalsIgnoreCase("library")) { // TODO dit mag weg want komt toch nooit voor hier?
            bundle.putString("state", "library");
        } else {
            // JA wel zetten want nu bij getString GEEN default waarde systeem (vereiste API groter dan 21)
            bundle.putString("state", "default");
        }

        fragment.setArguments(bundle);

        // Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack if needed
        transaction.replace(R.id.framelayout_container_main_activity, fragment);
        /*Note: When you remove or replace a fragment and add the transaction to the back stack, the fragment that is removed is stopped (not destroyed). If the user navigates back to restore the fragment, it restarts. If you do not add the transaction to the back stack, then the fragment is destroyed when removed or replaced. To allow the user to navigate backward
        through the fragment transactions, you must call addToBackStack() before you commit the FragmentTransaction.*/
//        http://sapandiwakar.in/replacing-fragments/
        transaction.addToBackStack(null);

// Commit the transaction
        transaction.commit();


    }

    // TODO enkel nog nodig als we uiteindelijk CursorListActivity gebruiken.
//    public void toCursorListActivity() {
//        Intent intent = new Intent(this, CursorListActivity.class);
//        startActivity(intent);
//    }


    // TODO ALS JE HIER IETS AANPAST, OOK IN ZELFDE METHODE IN ADVANCEDACTIVITY.JAVA
    private void addNavigationTabs(ActionBar theActionBar) {

        // Only recently gotten deprecated: since Android 5.0
        theActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // TODO perhaps work with LIST instead of TABS if you want to.

        for (int i = 1; i <= 3; i++) {
            // Only recently gotten deprecated: since Android 5.0
            if (i == 1) {
                theActionBar.addTab(theActionBar.newTab().setText(R.string.label_tab_2).setTabListener(this));
            } else if (i == 2) {
                theActionBar.addTab(theActionBar.newTab().setText(R.string.label_tab_3).setTabListener(this));
            } else if (i == 3) {
                theActionBar.addTab(theActionBar.newTab().setText(R.string.label_tab_4).setTabListener(this));
            } else {
                theActionBar.addTab(theActionBar.newTab().setText("Tab " + i).setTabListener(this));
            }

        }

    }


    /* FOR MQTT SERVICE */
    public class StatusUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle notificationData = intent.getExtras();
            String newStatus = notificationData.getString(MQTTService.MQTT_STATUS_MSG);

            // ...
        }
    }

    public class MQTTMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle notificationData = intent.getExtras();
            String newTopic = notificationData.getString(MQTTService.MQTT_MSG_RECEIVED_TOPIC);
            String newData = notificationData.getString(MQTTService.MQTT_MSG_RECEIVED_MSG);

            // ...
        }
    }

    @Override
    protected void onDestroy() {

        // ...

        // MQTT SERVICE IS GEKILLED IN THREE DOLLAR GESTURE ACTIVITY (niet fragment!)
        // UPDATE: toch niet want bij orientatie change wordt service dan gekilled! (en daarnaast start ze precies niet meer automatisch op!)
        // UPDATE: toch NIET gezet want bij orientatie verandering in mainactivity wordt service gekilled dan!
//        Intent svc = new Intent(getApplicationContext(), MQTTService.class);
//        stopService(svc);


        unregisterReceiver(statusUpdateIntentReceiver);
        unregisterReceiver(messageIntentReceiver);
        unregisterReceiver(BTReceiver);


        super.onDestroy();

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotificationManager.cancel(MQTTService.MQTT_NOTIFICATION_UPDATE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // INFO OVER ALLE MOGELIJKE OPTIES: https://stackoverflow.com/questions/10303898/oncreateoptionsmenu-calling-super


        super.onCreateOptionsMenu(menu);


        // You have inflated the menu IN THE ACTIVITY. It's also possible to do it into the FRAGMENT. (right now, MainFragment doesn't have this method onCreateOptionsMenu)
        // We could place all the following code into the MainFragment if we want.
        // TODO perhaps?
        // It's also possible to have a portion in an ACTIVITY and a portion into a FRAGMENTS. Those portions will then get combined at runtime!

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);

        // FOR REFRESH ICON
        final MenuItem refreshMenuItem = (MenuItem) menu.findItem(R.id.action_button_refresh);
        refreshMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            // on selecting show progress spinner for 1s
            public boolean onMenuItemClick(MenuItem item) {
                // Probably best to NOT do something here, but do it in the right section in onOptionsItemSelected.
                handler.postDelayed(new Runnable() {
                    public void run() {
                        refreshMenuItem.setActionView(null);
                    }
                }, 1000);
                return false;
            }
        });


        // Here we work programmatically instead of via XML. Since we have to check AT RUNTIME some conditions.
        addInstallOrRunPebbleCompanionAppOption(menu);


        return true;
//        return super.onCreateOptionsMenu(menu);
    }

    private void addInstallOrRunPebbleCompanionAppOption(Menu menu) {

        MenuItem firstButton = menu.findItem(R.id.action_button_1);

        final Intent intent = getPackageManager().getLaunchIntentForPackage("com.getpebble.android");


        if (intent != null) {
            firstButton.setTitle("Official Pebble™ Companion App");
            firstButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    return true;
                }
            });


        } else {
            // TODO translate
            firstButton.setTitle("Install official Pebble™ Companion App");
            firstButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    newIntent.setData(Uri.parse("market://details?id=com.getpebble.android"));
                    startActivity(newIntent);

                    return true;
                }
            });


        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        /*int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.actionbar_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);*/


        // TODO of dit gebruiken. MJA, LIJKT OP ZELFDE SYSTEEM ALS HIER DUS MSS BETER NIET.
        /*Tip: Android 3.0 adds the ability for you to define the on-click behavior for a menu item in XML, using the android:onClick attribute. The value for the attribute must be the name of a method defined by the activity using the menu.
        The method must be public and accept a single MenuItem parameter—when the system calls this method, it passes the menu item selected. For more information and an example, see the Menu Resource document.*/

        switch (item.getItemId()) {
            // TODO eventueel zoekfunctie implementeren
            /*case R.id.action_search:
                // TODO zoekfunctie
                return true;*/

            case android.R.id.home:
                // TODO handle clicking the app icon/logo
                return false; // TODO why here 'false'?

            case R.id.action_button_refresh:
                // switch to a progress animation
                // THIS CODE TRIGGERS THE ANIMATION. AND THE CODE ABOVE (with handler.postDelayed) STOPS the animation after 1 sec.
                item.setActionView(R.layout.indeterminate_progress_action);

                // !!
                MainFragment currentFragment = (MainFragment) getFragmentManager().findFragmentById(R.id.framelayout_container_main_activity);


                // TODO here should arrive code to for example do a Bluetooth sweep of the environment, and show and enable the toggles for the discovered devices.
                currentFragment.setLabelStates(currentFragment.getView(), true);
                currentFragment.setEnableDisableStates(currentFragment.getView(), true);
                // When clicking REFRESH we for now simulate the behavior that several (2) devices get detected.
                // So their toggles get enabled but we don't set it on ON automatically. (That would be insane)
                currentFragment.setToggleStates(currentFragment.getView(), false);

                return true;


            case R.id.action_button_1:
                Log.w(LOG_TAG, "clicked action button 1");

                // TODO doe iets

                return true;

            case R.id.action_button_2:

                switchToGestureLibraryFragment();

                return true;

            case R.id.action_set_1_opt_1:
                item.setChecked(true);
                // TODO doe iets
                return true;

            case R.id.action_set_1_opt_3:
                item.setChecked(true);
                // TODO doe iets
                return true;

            case R.id.action_set_1_opt_2:
                item.setChecked(true);
                // TODO doe iets
                return true;

            case R.id.action_set_2_opt_1:
                item.setChecked(true);
                // TODO doe iets
//                getActionBar().setBackgroundDrawable(null);
                return true;

            case R.id.action_set_2_opt_2:
                item.setChecked(true);
                // TODO doe iets
//                getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ad_action_bar_gradient_bak));
                return true;


            case R.id.action_settings:
                // TODO doe iets
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void switchToGestureLibraryFragment() {


        // This is a work-around to make it so no tab is selected.
        try {
            ActionBar actionbar = (ActionBar) getActionBar();
            actionbar.selectTab(null);
        } catch (Exception e) {
            // Do nothing.
        }

        // Create new fragment and transaction
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        Fragment fragment = new GestureListFragment();
        // Nu GEEN BUNDLE NODIG MET STATE want het STATE systeem geldt enkel bij de learn/recognize/library state omdat die dezelfde "ouderfragment" hebben die telkens wordt aangepast.
        // De MainActivity en MainFragment EN GestureListFragment hebben hier niets mee te zien.

        // Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack if needed
        transaction.replace(R.id.framelayout_container_main_activity, fragment);
        /*Note: When you remove or replace a fragment and add the transaction to the back stack, the fragment that is removed is stopped (not destroyed). If the user navigates back to restore the fragment, it restarts. If you do not add the transaction to the back stack, then the fragment is destroyed when removed or replaced. To allow the user to navigate backward
        through the fragment transactions, you must call addToBackStack() before you commit the FragmentTransaction.*/
//        http://sapandiwakar.in/replacing-fragments/
        transaction.addToBackStack(null);

// Commit the transaction
        transaction.commit();

    }


    // OLD CODE: fragment is now standalone instead of line.
    /*public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // If you do it in onCreate it seems to crash! (Even though the view has been inflated using setContentView(...) beforehand.
            ToggleButton testToggle = (ToggleButton) rootView.findViewById(R.id.toggle_pebble_acceldata_stream);
            testToggle.setEnabled(false);



            return rootView;
        }
    }*/


    @Override
    protected void onResume() {
        super.onResume();


    }


    /*When the system calls onPause() for your activity, it technically means your activity is still partially visible, but most often is an indication that the user is leaving the activity and it will soon enter the Stopped state. You should usually use the onPause() callback to:

Stop animations or other ongoing actions that could consume CPU.
Commit unsaved changes, but only if users expect such changes to be permanently saved when they leave (such as a draft email).
Release system resources, such as broadcast receivers, handles to sensors (like GPS), or any resources that may affect battery life while your activity is paused and the user does not need them.

    Generally, you should not use onPause() to store user changes (such as personal information entered into a form) to permanent storage. The only time you should persist user changes to permanent storage within onPause() is when you're certain users expect the changes to be auto-saved (such as when drafting an email). However, you should avoid performing CPU-intensive work during onPause(), such as writing to a database, because it can slow the visible transition to the next activity (you should instead perform heavy-load shutdown operations during onStop()).

You should keep the amount of operations done in the onPause() method relatively simple in order to allow for a speedy transition to the user's next destination if your activity is actually being stopped.

Note: When your activity is paused, the Activity instance is kept resident in memory and is recalled when the activity resumes. You don’t need to re-initialize components that were created during any of the callback methods leading up to the Resumed state.

    */
    @Override
    protected void onPause() {
        super.onPause();


    }


    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO doe iets?
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO doe iets?
    }







    /*
    ON RESTART niet echt nodig. Gebruik beter ON START voor code dat je in ON RESTART zou willen zetten.
    Zie https://developer.android.com/training/basics/activity-lifecycle/stopping.html
     */


}
