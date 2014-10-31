package jmri.enginedriver3;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Present a UI for requesting a loco address, by direct entry, by roster enty, or recently used
 */
public class SelectLocoDialogFragment extends android.support.v4.app.DialogFragment implements View.OnClickListener{
    private static MainApplication mainApp; // hold pointer to mainApp
    private View fragmentView; //the view object for this fragment
    private ArrayList<HashMap<String, String>> rosterEntryListLocalCopy = new ArrayList<HashMap<String, String>>();  //local copy of shared var
    private SimpleAdapter rosterEntryListAdapter = null;

    private String _fragmentName = null;  //should be set by calling throttle fragment, to be used as
                                        // as key to throttle list


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.select_loco_dialog_fragment, null);
        Button buttonCancel = (Button) fragmentView.findViewById(R.id.ButtonCancel);
        buttonCancel.setOnClickListener(this);
        return fragmentView;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Select Loco for Throttle");
        return dialog;
    }
    @Override
    public void onStart() {
        super.onStart();
        mainApp =(MainApplication)getActivity().getApplication();  //set pointer to app

        RefreshLocalCopyOfRosterEntryList();
        rosterEntryListAdapter =new SimpleAdapter(getActivity(), rosterEntryListLocalCopy,
                R.layout.roster_list_item,
                new String[] {"roster_name", "roster_address"},
                new int[] {R.id.roster_name_label, R.id.roster_address_label}) {};

        ListView tlv = (ListView) fragmentView.findViewById(R.id.listview);
        tlv.setAdapter(rosterEntryListAdapter);
        tlv.setOnItemClickListener(new viewlist_item());

        //hide the empty message if any items added
        if (rosterEntryListLocalCopy.size() > 0) {
            View ev = fragmentView.findViewById(android.R.id.empty);
            ev.setVisibility(View.GONE);
        }
    }

    private void RefreshLocalCopyOfRosterEntryList() {
        rosterEntryListLocalCopy.clear();  //MUST use same object, since simple adapter still looks at this one
        for(Map.Entry<String, RosterEntry> entry: mainApp.getRosterEntryList().entrySet()) {
            HashMap<String, String> hm = new HashMap<String, String>();  //make a temp hashmap for a single entry
            hm.put("roster_name", entry.getValue().getId());
            hm.put("roster_address", entry.getValue().getDccAddress());
            rosterEntryListLocalCopy.add(hm);  //add this new entry to local list for adapter
        }
      //sort the list
      Comparator<HashMap<String, String>> comparator = new Comparator<HashMap<String, String>>() {
        @Override
        public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
          return arg0.get("roster_name").compareToIgnoreCase(arg1.get("roster_name"));
        }
      };
      Collections.sort(rosterEntryListLocalCopy, comparator);
    }

    @Override  //process clicks on the OK and Cancel buttons
    public void onClick(View view) {
        if (view.getId() == R.id.ButtonCancel) {
            dismiss();  //just close the dialog
        }
    }
    //available click listeners for a viewlist item
    public class viewlist_item implements AdapterView.OnItemClickListener	  {

        public void onItemClick(AdapterView<?> parent, View v, int position, long id)	    {
            //use clicked position in list to retrieve details from local roster_list
            HashMap<String, String> hm 	= rosterEntryListLocalCopy.get(position);
//            String rosterNameString 	= hm.get("roster_name");
//            String rosterAddressString 	= hm.get("roster_address");
//
            //add some more items
            hm.put("loco_direction", Integer.toString(Consts.FORWARD));
            hm.put("fragment_name", getFragmentName());

            //convert to json for message transport
            Gson gson = new Gson();
            String hmJson = gson.toJson(hm, new TypeToken<HashMap<String, String>>() {}.getType());

            Log.d(Consts.APP_NAME, "Fragment " + getFragmentName() + " requesting "
                    + hm.get("roster_name") + " (" + hm.get("roster_address") + ")");
//            Toast.makeText(getActivity(), "Item clicked: " + tv.getText().toString() + "(" + id + ")", Toast.LENGTH_SHORT).show();
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.LOCO_REQUESTED, hmJson);
            dismiss();  //close the dialog box
        }
    }

    //accept a throttle id from calling fragment, to be passed in throttle request
    public void setFragmentName(String in_fragmentName) {
        _fragmentName = in_fragmentName;
    }
    private String getFragmentName() { return _fragmentName;}

}
