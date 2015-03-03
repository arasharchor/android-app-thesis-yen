package com.yen.androidappthesisyen;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends Activity implements ActionBar.TabListener {

    private static final String LOG_TAG = "MainActivity";

    // FOR REFRESH ICON
    private final Handler handler = new Handler();

    // Probably NOT recommended to make the variable FINAL and immediately initialize a fragment here.
    // Because in onCreate you see there is a check on the savedInstanceState so it's possible a new MainFragment has to be made, but not always!
    private MainFragment usedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main); // WAT IN WEZEN DE FRAGMENT CONTAINER IS (type FrameLayout) !


        // !! "R.id.framelayout_container_main_activity" werd gedefinieerd
        // in "R.layout.activity_main" !
        if (savedInstanceState == null) {
//            Fragment aPlaceholderFragment = new PlaceholderFragment();
            usedFragment = new MainFragment();
//            Fragment aPlaceholderFragment = new MainFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.framelayout_container_main_activity, usedFragment)
                    .commit();

        }

        final ActionBar theActionBar = getActionBar();

        // is FALSE bij ROOT ACTIVITY he.
        theActionBar.setDisplayHomeAsUpEnabled(false);

        // TODO inflate the tab layout by using XML files instead of coding it here.
        // set up tabs nav
        for (int i = 1; i < 4; i++) {
            // Only recently gotten deprecated: since Android 5.0
            if(i == 2){
                theActionBar.addTab(theActionBar.newTab().setText(R.string.label_tab_2).setTabListener(this));
            } else {
                theActionBar.addTab(theActionBar.newTab().setText("Tab " + i).setTabListener(this));
            }


        }

        // Only recently gotten deprecated: since Android 5.0
        theActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // TODO perhaps work with LIST instead of TABS if you want to.



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // INFO OVER ALLE MOGELIJKE OPTIES: https://stackoverflow.com/questions/10303898/oncreateoptionsmenu-calling-super

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


//        return true;
        return super.onCreateOptionsMenu(menu);
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

                // TODO here should arrive code to for example do a Bluetooth sweep of the environment, and show and enable the toggles for the discovered devices.
                usedFragment.setLabelStates(usedFragment.getView(), true);
                usedFragment.setToggleStates(usedFragment.getView(), true);


                return true;


            case R.id.action_button_1:
                // TODO doe iets
                /*useLogo = !useLogo; VANBOVEN STOND ER: private boolean useLogo = false;
                item.setChecked(useLogo);
                getActionBar().setDisplayUseLogoEnabled(useLogo);*/
                return true;

            case R.id.action_button_2:
                // TODO doe iets
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
        if(tab.getPosition() == 1){
            toCursorListActivity();
        }

    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO doe iets
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO doe iets
    }


}
