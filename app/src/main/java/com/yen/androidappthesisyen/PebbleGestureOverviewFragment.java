package com.yen.androidappthesisyen;

import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.dfki.ccaal.gestures.IGestureRecognitionService;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class PebbleGestureOverviewFragment extends ListFragment {

//    private OnFragmentInteractionListener mListener;

    String trainingSet;
    private IGestureRecognitionService recognitionService;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            recognitionService = IGestureRecognitionService.Stub.asInterface(service);
            try {

                List<String> items = recognitionService.getGestureList(trainingSet);
                // stond GestureOverview.this (1e param)
                setListAdapter(new ArrayAdapter<String>(getActivity(), R.layout.gesture_item, items));
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ListView lv = getListView();
            lv.setTextFilterEnabled(true);
            registerForContextMenu(lv);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // When clicked, show a toast with the TextView text
                    // stond getApplicationContext() (1e param)
                    Toast.makeText(getActivity(), ((TextView) view).getText(), Toast.LENGTH_LONG).show();
                    System.err.println(((TextView) view).getText());
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            recognitionService = null;
        }
    };


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PebbleGestureOverviewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // TODO: Change Adapter to display your content
//        setListAdapter(new ArrayAdapter<DummyContent.DummyItem>(getActivity(),
//                android.R.layout.simple_list_item_1, android.R.id.text1, DummyContent.ITEMS));
    }

    @Override
    public void onResume() {
        // startte met "getIntent()"
        trainingSet = getActivity().getIntent().getExtras().get("trainingSetName").toString();
        Intent bindIntent = new Intent("de.dfki.ccaal.gestures.GESTURE_RECOGNIZER");
        getActivity().bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        super.onResume();
    }

    @Override
    public void onPause() {
        recognitionService = null;
        getActivity().unbindService(serviceConnection);
        super.onPause();
    }

    // TODO niet beter BUTTON zetten ipv via CONTEXT MENU?
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(getListAdapter().getItem(info.position).toString());
        String[] menuItems = {"Delete"};

        for (int i = 0; i < menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (item.getItemId() == 0) {
            try {
                recognitionService.deleteGesture(trainingSet, getListAdapter().getItem(info.position).toString());
                List<String> items = recognitionService.getGestureList(trainingSet);
                // stond GestureOverview.this (1e param)
                setListAdapter(new ArrayAdapter<String>(getActivity(), R.layout.gesture_item, items));
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return true;
    }


    //    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }


//    @Override
//    public void onListItemClick(ListView l, View v, int position, long id) {
//        super.onListItemClick(l, v, position, id);
//
//        if (null != mListener) {
//            // Notify the active callbacks interface (the activity, if the
//            // fragment is attached to one) that an item has been selected.
//            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
//        }
//    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        public void onFragmentInteraction(String id);
//    }

}
