package jmri.enginedriver3;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

public class ConnectFragment extends DynaFragment {
    private int started = 0;
//    public Fragment_Handler fragmentHandler = null;
    private ArrayList<HashMap<String, String> > recentConnectionsList;
    private SimpleAdapter recentConnectionsListAdapter;
    private SimpleAdapter discoveredServerListAdapter;

    public ConnectFragment() {
    }

    /**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static ConnectFragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.DEBUG_TAG, "in ConnectFragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		ConnectFragment f = new ConnectFragment();

		// Store variables for retrieval 
		Bundle args = new Bundle();
		args.putInt("fragNum", fragNum);
		args.putString("fragType", fragType);
		args.putString("fragName", fragName);
		f.setArguments(args);

		return f;
	}

	/**---------------------------------------------------------------------------------------------**
	 * inflate and populate the proper xml layout for this fragment type
	 *    runs before activity starts, note does not call super		 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		Log.d(Consts.DEBUG_TAG, "in ConnectFragment.onCreateView() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.connect_fragment;
		//inflate the proper layout xml and remember it in fragment
		view = inflater.inflate(rx, container, false);

		return view;
	}
    @Override
    public void onStart() {
        super.onStart();
        started++;
        Log.d(Consts.DEBUG_TAG, "in ConnectFragment.onStart() " + started);

        //Set up a list adapter to present list of discovered WiThrottle servers to the UI.
        //this list is in mainapp, and managed by the jmdns thread
        discoveredServerListAdapter =new SimpleAdapter(getActivity(), mainApp.discovered_servers_list,
                R.layout.connections_list_item,
                new String[] {"ip_address", "host_name", "port"},
                new int[] {R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
        ListView dlv = (ListView) getActivity().findViewById(R.id.discovered_server_list);
        dlv.setAdapter(discoveredServerListAdapter);
//        dlv.setOnItemClickListener(new connect_item(server_list_type.DISCOVERED_SERVER));

        //populate the recent list from sharedprefs, defaulting if needed.
        // this list is managed by this fragment, and stored in sharedprefs
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("ConnectFragment", Context.MODE_PRIVATE);
        String recentConnectionsListJson = sharedPreferences.getString("recent_connections_list_json",
                "[{\"host_name\":\"jmri.mstevetodd.com\",\"port\":\"44444\",\"ip_address\":\"jmri.mstevetodd.com\"}]");
//        Log.d(Consts.DEBUG_TAG, recentConnectionsListJson);
        Gson gson = new Gson();
        recentConnectionsList = gson.fromJson(recentConnectionsListJson, new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType());

        //Set up a list adapter to present list of recent connections to the UI.
        recentConnectionsListAdapter =new SimpleAdapter(getActivity(), recentConnectionsList,
                R.layout.connections_list_item,
                new String[] {"ip_address", "host_name", "port"},
                new int[] {R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
        ListView conn_list=(ListView) getActivity().findViewById(R.id.server_list);
        conn_list.setAdapter(recentConnectionsListAdapter);
//        conn_list.setOnItemClickListener(new connect_item(server_list_type.RECENT_CONNECTION));

        // suppress popup keyboard until EditText is touched
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //only set this when fragment is ready to receive messages
        mainApp.dynaFrags.get(getFragNum()).setHandler(new Fragment_Handler());

    }

    @Override
    public void onStop() {
        //clear this to avoid late messages
        mainApp.dynaFrags.get(getFragNum()).setHandler(null);
        //save the recent connections list to sharedprefs as json string
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("ConnectFragment", Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        String recentConnectionsListJson = gson.toJson(recentConnectionsList);
//        Log.d(Consts.DEBUG_TAG, recentConnectionsListJson);
        sharedPreferencesEditor.putString("recent_connections_list_json", recentConnectionsListJson);
        sharedPreferencesEditor.commit();

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
//        Log.d(Consts.DEBUG_TAG, "in ConnectFragment.onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onResume() {
//        Log.d(Consts.DEBUG_TAG, "in ConnectFragment.onResume()");
        super.onResume();
    }
    private class Fragment_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.DEBUG_TAG, "in ConnectFragment.handleMessage()");
            switch (msg.what) {
                case MessageType.DISCOVERED_SERVER_LIST_CHANGED:
                    Log.d(Consts.DEBUG_TAG, "in ConnectFragment.handleMessage() DISCOVERED_SERVER_LIST_CHANGED");
                    //ignore changes made while Connect is not running
//                    if (discoveredServerListAdapter!=null) {
                    discoveredServerListAdapter.notifyDataSetChanged();
//                    } else {
//                        Log.d(Consts.DEBUG_TAG, "discoveredServerListAdapter is null");
//                    }
                    break;
                case MessageType.CONNECTED:  //TODO: redraw the screen to reflect connectedness
                    Log.d(Consts.DEBUG_TAG, "in ConnectFragment.handleMessage() CONNECTED");
//                    if (discoveredServerListAdapter!=null) {
                    discoveredServerListAdapter.notifyDataSetChanged();
//                    } else {
//                        Log.d(Consts.DEBUG_TAG, "discoveredServerListAdapter is null");
//                    }
                    break;
                default:
                    Log.d(Consts.DEBUG_TAG, "in ConnectFragment.handleMessage() not handled");
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

}
