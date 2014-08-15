package jmri.enginedriver3;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Present a list of tabs which could be removed.  If the user clicks on one, send a message back
 *   to activity to remove it and refresh the list.
 */
public class RemoveTabDialogFragment extends android.support.v4.app.DialogFragment {
    private static MainApplication mainApp; // hold pointer to mainApp
    private View fragmentView; //the view object for this fragment
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.remove_tab_dialog_fragment, null);
        return fragmentView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Select Tab to REMOVE");
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        mainApp =(MainApplication)getActivity().getApplication();  //set pointer to app
        List<String> tabList = new ArrayList<String>();
        for (int i = 0; i < mainApp.getDynaFrags().size(); i++) {
            if (!mainApp.getDynaFrags().get(i).getType().equals(Consts.CONNECT)) {  //cannot delete Connect tab
                tabList.add(mainApp.getDynaFrags().get(i).getName());
            }
        }
        ListView listview = (ListView) fragmentView.findViewById(R.id.listview);
        int liid = android.R.layout.simple_list_item_1;  //use default layout and options above
        listview.setAdapter(new ArrayAdapter<String>(getActivity(), liid, tabList));
        listview.setOnItemClickListener(new viewlist_item());
        //hide the empty message if any items added
        if (tabList.size() > 0) {
            View ev = fragmentView.findViewById(android.R.id.empty);
            ev.setVisibility(View.GONE);
        }
    }
    //available click listeners for a viewlist item
    public class viewlist_item implements android.widget.AdapterView.OnItemClickListener	  {

        public void onItemClick(android.widget.AdapterView<?> parent, View v, int position, long id)	    {
            TextView tv = (TextView) v;
//            Log.i(Consts.APP_NAME, "Item clicked: " + tv.getText().toString() + "(" + id + ")");
//            Toast.makeText(getActivity(), "Item clicked: " + tv.getText().toString() + "(" + id + ")", Toast.LENGTH_SHORT).show();
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler,
                    MessageType.REMOVE_TAB_REQUESTED, tv.getText().toString());
            dismiss();  //close the dialog box
        };
    }


}
