package com.yen.androidappthesisyen;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;


public class MainActivity extends Activity implements ActionBar.TabListener {

    private static final String LOG_TAG = MainActivity.class.getName();

    // FOR REFRESH ICON
    private final Handler handler = new Handler();


    private static final int ID_action_pebble_companion_app_install_run = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main); // WAT IN WEZEN DE FRAGMENT CONTAINER IS (type FrameLayout) !


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


        // Only recently gotten deprecated: since Android 5.0
        theActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // TODO perhaps work with LIST instead of TABS if you want to.


        // TODO hoort dit niet in onCreateOptionsMenu ? UPDATE is goed volgens tutorial @ https://developer.android.com/training/implementing-navigation/lateral.html#tabs
        // TODO inflate the tab layout by using XML files instead of coding it here.
        // set up tabs nav
        for (int i = 1; i <= 3; i++) {
            // Only recently gotten deprecated: since Android 5.0
            if (i == 2) {
                theActionBar.addTab(theActionBar.newTab().setText(R.string.label_tab_2).setTabListener(this));
            } else if (i == 3) {
                theActionBar.addTab(theActionBar.newTab().setText(R.string.label_tab_3).setTabListener(this));
            } else {
                theActionBar.addTab(theActionBar.newTab().setText("Tab " + i).setTabListener(this));
            }

        }


        // Certain apps need to keep the screen turned on, such as games or movie apps. The best way to do this is to use the FLAG_KEEP_SCREEN_ON in your activity (and only in an activity, never in a service or other app component).
        // From https://developer.android.com/training/scheduling/wakelock.html
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
            firstButton.setTitle("Pebble Companion App");
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
            firstButton.setTitle("Install Pebble Companion App");
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
                // TODO doe iets
                /*useLogo = !useLogo; VANBOVEN STOND ER: private boolean useLogo = false;
                item.setChecked(useLogo);
                getActionBar().setDisplayUseLogoEnabled(useLogo);*/
                return true;

            case R.id.action_button_2:

                toPebblePointerActivity();

                /*showHomeUp = !showHomeUp;
                item.setChecked(showHomeUp);
                getActionBar().setDisplayHomeAsUpEnabled(showHomeUp);*/
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

    private void toPebblePointerActivity() {

        Intent intent = new Intent(this, PebblePointerActivity.class);
        startActivity(intent);

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


    /*TODO perhaps: You can also declare the click event handler programmatically rather than in an XML layout. This might be necessary if you instantiate the Button at runtime or you need to declare the click behavior in a Fragment subclass.
     https://developer.android.com/guide/topics/ui/controls/button.html
      So put the following code in the FRAGMENT since it's the FRAGMENT that builds the GUI; not the ACTIVITY in our case. */
    public void toMasterDetailActivity(View view) {
        Intent intent = new Intent(this, MasterDetailItemListActivity.class);
        // nu geen extra info met de Intent verstuurd.
        startActivity(intent);
    }


    public void toCursorListActivity() {
        Intent intent = new Intent(this, CursorListActivity.class);
        startActivity(intent);
    }


    // Following 3 methods are due to "implements ActionBar.TabListener"
    // TODO tabs are probably meant to CHANGE THE LAYOUT (change activity or fragment) while the BUTTONS on the left of the OVERFLOW BUTTON are probably for ACTIONS on the current layout
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO dit aangewezen manier voor vinden juiste tab?
        // Don't check == 0 because that tab 0 is SELECTED by DEFAULT when starting the app.
        // So tab 0 corresponds with the MainActivity!
        if (tab.getPosition() == 1) {
            toCursorListActivity();
        } else if (tab.getPosition() == 2) {
            toPebbleAccelStreamActivity();
        }

    }

    private void toPebbleAccelStreamActivity() {
        Intent intent = new Intent(this, PebbleAccelStreamActivity.class);
        startActivity(intent);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO doe iets
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO doe iets
    }







    /*
    ON RESTART niet echt nodig. Gebruik beter ON START voor code dat je in ON RESTART zou willen zetten.
    Zie https://developer.android.com/training/basics/activity-lifecycle/stopping.html
     */


}
