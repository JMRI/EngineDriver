package jmri.enginedriver3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class TurnoutsFragment extends DynaFragment {
    private int started = 0;
    private ArrayList<HashMap<String, String> > turnoutsList;  //local copy of shared var
    private SimpleAdapter turnoutsListAdapter;
    private MainActivity mainActivity = null;

    public TurnoutsFragment() {
    }

    /**---------------------------------------------------------------------------------------------**
	 * create a new fragment of this type, and populate basic settings in bundle 
	 * Note: this is a static method, called from the activity's getItem() when new ones are needed */	
	static TurnoutsFragment newInstance(int fragNum, String fragType, String fragName) {
		Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.newInstance() for " + fragName + " (" + fragNum + ")" + " type " + fragType);
		TurnoutsFragment f = new TurnoutsFragment();

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
//		Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.onCreateView() for " + getFragName() + " (" + getFragNum() + ")" + " type " + getFragType());
		//choose the proper layout xml for this fragment's type
		int rx = R.layout.turnouts_fragment;
		//inflate the proper layout xml and remember it in fragment
		view = inflater.inflate(rx, container, false);

		return view;
	}
    @Override
    public void onStart() {
        super.onStart();
        started++;
        Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.onStart() " + started);

        //Set up a list adapter to present list of turnouts to the UI.
        //this list is in mainapp, and managed by the websocket thread
        turnoutsList = new ArrayList<HashMap<String, String> >(mainApp.getTurnoutsList());  //make a local copy
        turnoutsListAdapter =new SimpleAdapter(getActivity(), turnoutsList,
                R.layout.turnouts_item,
                new String[] {"userName", "name", "state"},
                new int[] {R.id.to_user_name, R.id.to_system_name, R.id.to_current_state_desc}) {

            //set up listener for each state button
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                if (row != null) {
                    Button b = (Button) row.findViewById(R.id.to_current_state_desc);
                    b.setOnClickListener(new turnout_state_button_listener());
                }
                return row;
            }
        };

        ListView tlv = (ListView) getActivity().findViewById(R.id.turnouts_list);
        tlv.setAdapter(turnoutsListAdapter);
//        tlv.setOnItemClickListener(new clicked_item());

        //only set this when fragment is ready to receive messages
        mainApp.dynaFrags.get(getFragNum()).setHandler(new Fragment_Handler());

        SetFooterMessage();
    }

    @Override
    public void onStop() {
        //clear this to avoid late messages
        mainApp.dynaFrags.get(getFragNum()).setHandler(null);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.onAttach()");
        this.mainActivity = (MainActivity) activity;  //save ref to the new activity
    }
    @Override
    public void onDetach() {
//        Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.onDetach()");
        this.mainActivity = null; //clear ref
        super.onDetach();
    }
    private class Fragment_Handler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.handleMessage()");
            switch (msg.what) {
                case MessageType.TURNOUTS_LIST_CHANGED:
                    Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.handleMessage() TURNOUTS_LIST_CHANGED");
                    turnoutsList.clear();  //refresh the local copy, keeping same memory location for adapter
                    turnoutsList.addAll(mainApp.getTurnoutsList());
                    turnoutsListAdapter.notifyDataSetChanged();
                    SetFooterMessage();
                    break;
                default:
                    Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.handleMessage() not handled: " + msg.what);
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }
    //handle click for each turnout's state toggle button, send request to activity
    public class turnout_state_button_listener implements View.OnClickListener  {

        public void onClick(View v) {
            ViewGroup vg = (ViewGroup)v.getParent();  //start with the list item the button belongs to
            ViewGroup rl = (ViewGroup) vg.getChildAt(0);  //get relativelayout that holds systemname and username
            TextView snv = (TextView) rl.getChildAt(1); // get systemname text from 2nd box
            String systemName = (String) snv.getText();
            mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.TURNOUT_CHANGE_REQUESTED, systemName, 2);	// 2=toggle
        }
    }

//    public class clicked_item implements AdapterView.OnItemClickListener {
//        //When an item is clicked, send request to connect to the selected IP address and port.
//        public void onItemClick(AdapterView<?> parent, View v, int position, long id)    {
//
//            ViewGroup vg = (ViewGroup)v; //convert to viewgroup for clicked row
//            TextView hip = (TextView) vg.getChildAt(0); // get host ip from 1st box
//            String requestedHostIP = (String) hip.getText();
////                    TextView hnv = (TextView) vg.getChildAt(1); // get host name from 2nd box
////                    connected_hostname = (String) hnv.getText();
//            TextView hpv = (TextView) vg.getChildAt(2); // get port from 3rd box
//            int requestedPort = Integer.valueOf((String) hpv.getText());
////                mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.CONNECT_REQUESTED,
////                        requestedHostIP, requestedPort);
//
//        };
//    }
    private void SetFooterMessage() {
        View tv = view.findViewById(R.id.to_footer);
        if (tv != null) {
                ((TextView)tv).setText(turnoutsList.size() + " items");
        }
    }


}
