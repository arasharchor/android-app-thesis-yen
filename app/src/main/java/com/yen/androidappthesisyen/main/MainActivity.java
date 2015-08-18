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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.advancedrecognizer.AdvancedFragment;
import com.yen.androidappthesisyen.gesturelibrary.GestureLibraryFragment;
import com.yen.androidappthesisyen.pushnotificationlistener.MQTTService;

import java.util.List;

import static com.yen.androidappthesisyen.utilities.UtilityRepo.getListSystemIDsToConnectTo;
import static com.yen.androidappthesisyen.utilities.UtilityRepo.removeSystemIDFromListSystemIDsToConnectTo;


public class MainActivity extends Activity implements ActionBar.TabListener {

    private static final String LOG_TAG = MainActivity.class.getName();

    // FOR REFRESH ICON
    private final Handler handler = new Handler();


    private static final int ID_action_pebble_companion_app_install_run = 1;


    /* FOR MQTT SERVICE */
    private StatusUpdateReceiver statusUpdateIntentReceiver;
    private MQTTMessageReceiver messageIntentReceiver;


    // TODO als je via deze var de view objecten niet kunt accessen, probeer via een tag bij .add() bij transaction stuff.
    // UPDATE nu via TAG, voor zekerheid
//    private MainFragment theMainFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);


        // !! "R.id.framelayout_container_main_activity" werd gedefinieerd
        // in "R.layout.activity_main" !
        if (savedInstanceState == null) {

            // DONT PLACE THIS FRAGMENT IN A GLOBAL VARIABLE. Since fragments get made and remade upon orientation changes and stuff!
            // So that variable could become NULL at some point!
            MainFragment theMainFragment = new MainFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.framelayout_container_main_activity, theMainFragment, "main")
                    .commit();
        }

        final ActionBar theActionBar = getActionBar();

        // FALSE because ROOT ACTIVITY
        theActionBar.setDisplayHomeAsUpEnabled(false);

        // Only when the screen is large enough do we show the app title in the ActionBar.
        // On smaller screens the tabs would otherwise be shown as a drop-down list.
        if (!isLargeScreen()) {
            theActionBar.setDisplayShowTitleEnabled(false);
        }


        // TODO inflate the tab layout by using XML files instead of coding it here.
        addNavigationTabs(theActionBar);


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


        // We keep the screen on continuously while running the app so the recognized gestures scrollview can be seen all the time.
        // TODO OF BETER UIT WNT NIET ECHT NODIG?
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }


    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Device found
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                // Device got connected
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // TODO labels don't update dynamically
                        // Change Pebble™ connection label
//                        MainFragment myFragment = (MainFragment)getFragmentManager().findFragmentByTag("main");
//                        if (myFragment != null) {
//                            myFragment.pebbleGotConnected();
//                        }

                        Toast.makeText(context, R.string.pebble_connected_toast, Toast.LENGTH_LONG).show();
                    }
                });
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Done searching
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                // Device is about to disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // Device has disconnected

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // TODO labels don't update dynamically
                        // Change Pebble™ connection label
//                        MainFragment myFragment = (MainFragment)getFragmentManager().findFragmentByTag("main");
//                        if (myFragment != null) {
//                            myFragment.pebbleGotDisconnected();
//                        }

                        Toast.makeText(context, R.string.pebble_not_connected_toast, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    };

    private boolean isLargeScreen() {
        return (this.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    // Following 3 methods are due to "implements ActionBar.TabListener"
    // TODO tabs are probably meant to CHANGE THE LAYOUT (change activity or fragment) while the BUTTONS on the left of the OVERFLOW BUTTON are probably for ACTIONS on the current layout
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO dit aangewezen manier voor vinden juiste tab?
        // Don't check == 0 because that tab 0 is SELECTED by DEFAULT when starting the app.
        // So tab 0 corresponds with the MainActivity!
        if (tab.getPosition() == 0) {
            switchToMainFragment(tab);
        } else if (tab.getPosition() == 1) {
            switchToAdvancedRecognizerFragment(tab, "learn");
        } else if (tab.getPosition() == 2) {
            switchToAdvancedRecognizerFragment(tab, "recognize");
        }

    }


    private void switchToMainFragment(ActionBar.Tab tab) {


//        ActionBar actionbar = (ActionBar) getActionBar();
//        actionbar.selectTab(tab);


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


    private void switchToAdvancedRecognizerFragment(ActionBar.Tab tab, String requestedState) {


//        ActionBar actionbar = (ActionBar) getActionBar();
//        actionbar.selectTab(tab);


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

        // Clear the list
        List<String> theList = getListSystemIDsToConnectTo(this);
        for (String systemIDToRemove : theList) {
            removeSystemIDFromListSystemIDsToConnectTo(this, systemIDToRemove);
        }


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

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);

        // FOR REFRESH ICON
//        final MenuItem refreshMenuItem = (MenuItem) menu.findItem(R.id.action_button_refresh);
//        refreshMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            // on selecting show progress spinner for 1s
//            public boolean onMenuItemClick(MenuItem item) {
//                // Probably best to NOT do something here, but do it in the right section in onOptionsItemSelected.
//                handler.postDelayed(new Runnable() {
//                    public void run() {
//                        refreshMenuItem.setActionView(null);
//                    }
//                }, 1000);
//                return false;
//            }
//        });


        // Here we work programmatically instead of via XML. Since we have to check AT RUNTIME some conditions.
        addInstallOrRunPebbleCompanionAppOption(menu);


        return true;
//        return super.onCreateOptionsMenu(menu);
    }

    private void addInstallOrRunPebbleCompanionAppOption(Menu menu) {

        MenuItem firstButton = menu.findItem(R.id.action_button_1);

        final Intent intent = getPackageManager().getLaunchIntentForPackage("com.getpebble.android");


        if (intent != null) {


            firstButton.setTitle(R.string.official_app);
            firstButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast t = Toast.makeText(getApplicationContext(), R.string.pebble_companion_app_found, Toast.LENGTH_LONG);
                            t.setGravity(Gravity.CENTER, 0, 0);
                            t.show();

                            Toast t2 = Toast.makeText(getApplicationContext(), R.string.connect_your_pebble, Toast.LENGTH_LONG);
                            t2.setGravity(Gravity.CENTER, 0, 0);
                            t2.show();
                        }
                    });


                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    return true;
                }
            });


        } else {

            firstButton.setTitle("Install official Pebble™ Companion App");
            firstButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast t = Toast.makeText(getApplicationContext(), R.string.pebble_companion_app_not_found, Toast.LENGTH_LONG);
                            t.setGravity(Gravity.CENTER, 0, 0);
                            t.show();

                            Toast t2 = Toast.makeText(getApplicationContext(), R.string.download_and_retry, Toast.LENGTH_LONG);
                            t2.setGravity(Gravity.CENTER, 0, 0);
                            t2.show();
                        }
                    });

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

            case android.R.id.home:
                // TODO handle clicking the app icon/logo
                return false; // TODO wrom 'false'?

//            case R.id.action_button_refresh:
//                // switch to a progress animation
//                // THIS CODE TRIGGERS THE ANIMATION. AND THE CODE ABOVE (with handler.postDelayed) STOPS the animation after 1 sec.
//                item.setActionView(R.layout.indeterminate_progress_action);
//
//                return true;


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

        Fragment fragment = new GestureLibraryFragment();
        // Nu GEEN BUNDLE NODIG MET STATE want het STATE systeem geldt enkel bij de learn/recognize/library state omdat die dezelfde "ouderfragment" hebben die telkens wordt aangepast.
        // De MainActivity en MainFragment EN GestureLibraryFragment hebben hier niets mee te zien.

        // Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack if needed
        transaction.replace(R.id.framelayout_container_main_activity, fragment, "gesturelibrary");
        /*Note: When you remove or replace a fragment and add the transaction to the back stack, the fragment that is removed is stopped (not destroyed). If the user navigates back to restore the fragment, it restarts. If you do not add the transaction to the back stack, then the fragment is destroyed when removed or replaced. To allow the user to navigate backward
        through the fragment transactions, you must call addToBackStack() before you commit the FragmentTransaction.*/
//        http://sapandiwakar.in/replacing-fragments/
        transaction.addToBackStack(null);

// Commit the transaction
        transaction.commit();

    }


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
