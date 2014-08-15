package jmri.enginedriver3;

import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

public class ConnectFragment extends DynaFragment {
    private int started = 0;
    private ArrayList<HashMap<String, String> > recentConnectionsList;  //maintained locally, stored in prefs
    private SimpleAdapter recentConnectionsListAdapter;
    private ArrayList<HashMap<String, String> > discoveredServersList;  //local copy of shared var
    private SimpleAdapter discoveredServerListAdapter;
    private MainActivity mainActivity = null;

    public ConnectFragment() {
    }

    /**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static ConnectFragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.APP_NAME, "in ConnectFragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
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
//		Log.d(Consts.APP_NAME, "in ConnectFragment.onCreateView() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.connect_fragment;
		//inflate the proper layout xml and remember it in fragment
		fragmentView = inflater.inflate(rx, container, false);

		return fragmentView;
	}
    @Override
    public void onStart() {
        super.onStart();
        started++;
        Log.d(Consts.APP_NAME, "in ConnectFragment.onStart() " + started);

        //Set up a list adapter to present list of discovered WiThrottle servers to the UI.
        //this list is in mainapp, and managed by the jmdns thread
        discoveredServersList = new ArrayList<HashMap<String, String> >(mainApp.getDiscoveredServersList());  //make a local copy
        discoveredServerListAdapter =new SimpleAdapter(getActivity(), discoveredServersList,
                R.layout.connections_list_item,
                new String[] {"ip_address", "host_name", "port"},
                new int[] {R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
        ListView dlv = (ListView) getActivity().findViewById(R.id.discovered_server_list);
        dlv.setAdapter(discoveredServerListAdapter);
        dlv.setOnItemClickListener(new connect_item());
//        discoveredServerListAdapter.notifyDataSetChanged();

        //populate the recent list from sharedprefs, defaulting if needed.
        // this list is managed by this fragment, and stored in sharedprefs
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("ConnectFragment", Context.MODE_PRIVATE);
        String recentConnectionsListJson = sharedPreferences.getString("recent_connections_list_json",
                "[{\"host_name\":\"jmri.mstevetodd.com\",\"port\":\"1080\",\"ip_address\":\"jmri.mstevetodd.com\"}]");
//        Log.d(Consts.APP_NAME, recentConnectionsListJson);
        Gson gson = new Gson();
        recentConnectionsList = gson.fromJson(recentConnectionsListJson, new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType());

        //Set up a list adapter to present list of recent connections to the UI.
        recentConnectionsListAdapter =new SimpleAdapter(getActivity(), recentConnectionsList,
                R.layout.connections_list_item,
                new String[] {"ip_address", "host_name", "port"},
                new int[] {R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
        ListView conn_list=(ListView) getActivity().findViewById(R.id.server_list);
        conn_list.setAdapter(recentConnectionsListAdapter);
        conn_list.setOnItemClickListener(new connect_item());

        // suppress popup keyboard until EditText is touched
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        SetConnectedMessage();

        //only set this when fragment is ready to receive messages
        mainApp.setDynaFragHandler(getFragNum(), new Fragment_Handler());

    }

    @Override
    public void onStop() {
        Log.d(Consts.APP_NAME, "in ConnectFragment.onStop()");
        //clear this to avoid late messages
        if (mainApp.getDynaFrags().get(getFragNum())!=null) mainApp.getDynaFrags().get(getFragNum()).setHandler(null);
        //save the recent connections list to sharedprefs as json string
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("ConnectFragment", Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        String recentConnectionsListJson = gson.toJson(recentConnectionsList);
//        Log.d(Consts.APP_NAME, recentConnectionsListJson);
        sharedPreferencesEditor.putString("recent_connections_list_json", recentConnectionsListJson);
        sharedPreferencesEditor.commit();

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        Log.d(Consts.APP_NAME, "in ConnectFragment.onAttach()");
        this.mainActivity = (MainActivity) activity;  //save ref to the new activity
    }

    @Override
    public void onDetach() {
//        Log.d(Consts.APP_NAME, "in ConnectFragment.onDetach()");
        this.mainActivity = null; //clear ref
        super.onDetach();
    }

//    @Override
//    public void onDestroy() {
//        Log.d(Consts.APP_NAME, "in ConnectFragment.onDestroy()");
//        super.onDestroy();
//    }

//    @Override
//    public void onResume() {
//        Log.d(Consts.APP_NAME, "in ConnectFragment.onResume()");
//        super.onResume();
//    }
    private class Fragment_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.APP_NAME, "in ConnectFragment.handleMessage()");
            switch (msg.what) {
                case MessageType.DISCOVERED_SERVER_LIST_CHANGED:
                    Log.d(Consts.APP_NAME, "in ConnectFragment.handleMessage() DISCOVERED_SERVER_LIST_CHANGED");
                    discoveredServersList.clear();  //refresh the local copy, keeping same memory location for adapter
                    discoveredServersList.addAll(mainApp.getDiscoveredServersList());
                    discoveredServerListAdapter.notifyDataSetChanged();
                    break;
                case MessageType.CONNECTED:
                case MessageType.DISCONNECTED:
                    Log.d(Consts.APP_NAME, "in ConnectFragment.handleMessage() DIS/CONNECTED");
                    SetConnectedMessage();
                    if (mainApp.isConnected()) {
                        UpdateRecentServerList();
                    }
                    break;
                default:
                    Log.d(Consts.APP_NAME, "in ConnectFragment.handleMessage() not handled");
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

    private void SetConnectedMessage() {
        View tv = fragmentView.findViewById(R.id.cf_footer);
        if (tv != null) {
            if (!mainApp.isConnected()) {
                ((TextView)tv).setText("Not Connected");
            } else {
                String s = "Connected to " + mainApp.getServer() + ":" + mainApp.getWebPort() +
                        "\nver " + mainApp.getJmriVersion() + ", " + "heartbeat " + mainApp.getJmriHeartbeat() ;

                ((TextView)tv).setText(s);
            }
        }
    }

    private void UpdateRecentServerList() {
        HashMap<String, String> hm=new HashMap<String, String>();
        hm.put("ip_address", mainApp.getServer());
        hm.put("host_name", mainApp.getServer());
        hm.put("port", Integer.toString(mainApp.getWebPort()));
        if(!recentConnectionsList.contains(hm)) {	// suppress dups
            recentConnectionsList.add(hm);
            recentConnectionsListAdapter.notifyDataSetChanged();
        }
    }

    public class connect_item implements AdapterView.OnItemClickListener {
        //When an item is clicked, send request to connect to the selected IP address and port.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id)    {

            ViewGroup vg = (ViewGroup)v; //convert to viewgroup for clicked row
            TextView hip = (TextView) vg.getChildAt(0); // get host ip from 1st box
            String requestedHostIP = (String) hip.getText();
//                    TextView hnv = (TextView) vg.getChildAt(1); // get host name from 2nd box
//                    connected_hostname = (String) hnv.getText();
            TextView hpv = (TextView) vg.getChildAt(2); // get port from 3rd box
            int requestedPort = Integer.valueOf((String) hpv.getText());
            //don't connect to same server:port again
            if (mainApp.getServer()!=null && mainApp.getServer().equals(requestedHostIP) && mainApp.getWebPort()== requestedPort) {
                String s = "Already connected to " + requestedHostIP + ":" + requestedPort;
                mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.MESSAGE_SHORT, s);
            } else {
                View tv = fragmentView.findViewById(R.id.cf_footer);
                if (tv != null) {
                    ((TextView) tv).setText("New Connection requested.......");
                }
                mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.CONNECT_REQUESTED,
                        requestedHostIP, requestedPort);
            }

        };
    }


}
