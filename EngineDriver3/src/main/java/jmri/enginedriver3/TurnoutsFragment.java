package jmri.enginedriver3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TurnoutsFragment extends DynaFragment {
    private int started = 0;
    private ArrayList<HashMap<String, String>> turnoutsListLocalCopy = new ArrayList<HashMap<String, String>>();  //local copy of shared var
    private SimpleAdapter turnoutsListAdapter = null;
    private MainActivity mainActivity = null;

    private int turnoutsListSavedPosition = 0;

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

        if (savedInstanceState!=null) { //restore saved scroll position, if saved
            turnoutsListSavedPosition = savedInstanceState.getInt("turnoutsListSavedPosition", 0);
        }

		//choose the proper layout xml for this fragment's type
		int rx = R.layout.turnouts_fragment;
		//inflate the proper layout xml and remember it in fragment
		fragmentView = inflater.inflate(rx, container, false);
		return fragmentView;
	}
    @Override
    public void onStart() {
        super.onStart();
        started++;
        Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.onStart() " + started);

        //Set up a list adapter to present list of turnouts to the UI.
        //uses local copy of the list in mainapp, which is managed by the websocket thread
        RefreshLocalCopyOfTurnoutList();
        turnoutsListAdapter =new SimpleAdapter(getActivity(), turnoutsListLocalCopy,
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
        tlv.setSelectionFromTop(turnoutsListSavedPosition, 0);  //position to saved position

        //set backref to this handler to indicate fragment is ready to receive messages from activity
        mainApp.dynaFrags.get(getFragNum()).setHandler(new Fragment_Handler());

        SetFooterMessage();
    }

    private void RefreshLocalCopyOfTurnoutList() {
        turnoutsListLocalCopy.clear();  //MUST use same object, since adapter still looks at this one
        for(Map.Entry<String, Turnout> entry: mainApp.getTurnoutList().entrySet()) {
            HashMap<String, String> hm = new HashMap<String, String>();  //make a temp hashmap for a single entry
            hm.put("name", entry.getValue().getSystemName());
            hm.put("userName", entry.getValue().getUserName());
            hm.put("state", getStateDesc(entry.getValue().getState()));
            turnoutsListLocalCopy.add(hm);  //add this new entry to local list for adapter
        }
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
        //save scroll position for later resume
        ListView lv=(ListView) fragmentView.findViewById(R.id.turnouts_list);
        int turnoutsListSavedPosition = lv.getFirstVisiblePosition();
        outState.putInt("turnoutsListSavedPosition", turnoutsListSavedPosition);
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
                case MessageType.TURNOUT_LIST_CHANGED:
                    Log.d(Consts.DEBUG_TAG, "in TurnoutsFragment.handleMessage() TURNOUTS_LIST_CHANGED");
                    RefreshLocalCopyOfTurnoutList();
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
            int newState = ((mainApp.getTurnoutState(systemName)==4) ? 2 : 4); //get toggled value from current state
            mainApp.sendMsg(mainActivity.mainActivityHandler, MessageType.TURNOUT_CHANGE_REQUESTED, systemName, newState);
        }
    }

    private void SetFooterMessage() {
        View tv = fragmentView.findViewById(R.id.to_footer);
        if (tv != null) {
                ((TextView)tv).setText(turnoutsListLocalCopy.size() + " items");
        }
    }
    private String getStateDesc(int in_state) {
        if (in_state==Consts.STATE_CLOSED) return Consts.STATE_CLOSED_DESC;
        if (in_state==Consts.STATE_THROWN) return Consts.STATE_THROWN_DESC;
        return Consts.STATE_UNKNOWN_DESC;
    }

}
