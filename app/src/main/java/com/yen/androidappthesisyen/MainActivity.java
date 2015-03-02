package com.yen.androidappthesisyen;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


public class MainActivity extends Activity implements ActionBar.TabListener {

    private static final String LOG_TAG = "MainActivity";

    // FOR REFRESH ICON
    private final Handler handler = new Handler();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main); // WAT IN WEZEN DE FRAGMENT CONTAINER IS (type FrameLayout) !


        // !! "R.id.framelayout_container_main_activity" werd gedefinieerd
        // in "R.layout.activity_main" !
        if (savedInstanceState == null) {
            Fragment aPlaceholderFragment = new PlaceholderFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.framelayout_container_main_activity, aPlaceholderFragment)
                    .commit();

        }

        final ActionBar theActionBar = getActionBar();

        // is FALSE bij ROOT ACTIVITY he.
        theActionBar.setDisplayHomeAsUpEnabled(false);

        // set up tabs nav
        for (int i = 1; i < 4; i++) {
            // Only recently gotten deprecated: since Android 5.0
            theActionBar.addTab(theActionBar.newTab().setText("Tab " + i).setTabListener(this));
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
                // is by default DISABLED and we leave it that way since it's not needed
//                item.setActionView(R.layout.progress_action);
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
                item.setActionView(R.layout.indeterminate_progress_action);
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




    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
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



    public void toMasterDetailActivity(View view) {
        Intent intent = new Intent(this, MasterDetailItemListActivity.class);
        // nu geen extra info met de Intent verstuurd.
        startActivity(intent);
    }



    // Following 3 methods are due to "implements ActionBar.TabListener"
    // TODO tabs are probably meant to CHANGE THE LAYOUT (change activity or fragment) while the BUTTONS on the left of the OVERFLOW BUTTON are probably for ACTIONS on the current layout
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO doe iets
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
