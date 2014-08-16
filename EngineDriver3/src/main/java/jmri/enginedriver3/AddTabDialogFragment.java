package jmri.enginedriver3;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Present a list of tab definitions which could be added.  If the user clicks on one, send a message back
 *   to activity to add it and refresh the list.
 */
public class AddTabDialogFragment extends android.support.v4.app.DialogFragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    private static MainApplication mainApp; // hold pointer to mainApp
    private View fragmentView; //the view object for this fragment

    //possible choices for new fragment, list is build in getFragmentChoices
    private ArrayList<HashMap<String, String>> fragmentChoices;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.add_tab_dialog_fragment, null);
        setCancelable(false);  //make dialog stay active until one of the buttons is pressed
        Button buttonOk = (Button) fragmentView.findViewById(R.id.ButtonOk);
        buttonOk.setOnClickListener(this);
        Button buttonCancel = (Button) fragmentView.findViewById(R.id.ButtonCancel);
        buttonCancel.setOnClickListener(this);
        return fragmentView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Add a new Tab");
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        mainApp =(MainApplication)getActivity().getApplication();  //set pointer to app

        //populate the options for fragment types
        fragmentChoices = getFragmentChoices();

        //build the adapter for the spinner
        Spinner spinnerFragmentChoices = (Spinner) fragmentView.findViewById(R.id.spinner_fragment_choices);
        SimpleAdapter fragmentChoicesListAdapter =new SimpleAdapter(getActivity(), fragmentChoices,
                android.R.layout.simple_list_item_1,
                new String[] {"ft_description"},
                new int[] {android.R.id.text1});
        //attach the adapter and the listeners to the spinner
        spinnerFragmentChoices.setAdapter(fragmentChoicesListAdapter);
        spinnerFragmentChoices.setOnItemSelectedListener(this);
    }

    //set up a smart list of choices with various settings
    private ArrayList<HashMap<String, String>> getFragmentChoices() {
        ArrayList<HashMap<String, String>> ftc = new ArrayList<HashMap<String, String>>();
        ftc.add(NewFragmentChoice("<Select Tab Type to Add>", "", "", "", ""));  //empty entry to force user to make a selection
        ftc.add(NewFragmentChoice("Vertical Throttle", "Throttle", Consts.CONSIST, "2", "vertical"));
        ftc.add(NewFragmentChoice("Horizontal Throttle", "Throttle", Consts.CONSIST, "2", "horizontal"));
        ftc.add(NewFragmentChoice("Turnout", "Turnout", Consts.TURNOUT, "2", null));

        for (int i = 0; i < mainApp.getPanelList().size(); i++) {  //add an item for each open panel
            Panel p = mainApp.getPanelList().get(i);
            String tabName = p.getUserName();
            if (tabName.length()>11) {  //truncate to avoid tab width growth
                tabName = tabName.substring(0,11);
            }
            ftc.add(NewFragmentChoice("Panel: " + p.getUserName(), tabName ,
                    Consts.WEB, "2", "/panel/" + p.getName()));
        }
        ftc.add(NewFragmentChoice("Home Page", "Web", Consts.WEB, "2", "/"));
        if (!mainApp.dynaFragExists("Panels")) {
            ftc.add(NewFragmentChoice("Panels", "Panels", Consts.WEB, "2", "/panel/"));
        }
        if (!mainApp.dynaFragExists("Trains")) {
            ftc.add(NewFragmentChoice("Trains", "Trains", Consts.WEB, "2", "/operations/trains"));
        }
        if (!mainApp.dynaFragExists("About")) {
            ftc.add(NewFragmentChoice("About Page", "About", Consts.WEB, "2", "file:///android_asset/about_page.html"));
        }
        return ftc;
    }

    private HashMap<String, String> NewFragmentChoice(String d, String n, String t, String width, String data) {
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("ft_description", d);
        hm.put("ft_name",        n);
        hm.put("ft_type",        t);
        hm.put("ft_width",       width);
        hm.put("ft_data",        data);
        return hm;
    }

    //append a number to the end until it is not used
    private String getUniqueFragmentName(String in_name) {
        String newName = in_name;
        int suffix = 2;  //start at 2
        while (mainApp.dynaFragExists(newName)) {
            newName = in_name + suffix;
            suffix++;
        }
        return newName;
    }

    @Override  //when user selects, populate and hide/show other fields based on selection
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String description = fragmentChoices.get(i).get("ft_description");
        Log.d(Consts.APP_NAME, "in AddTabDialogFragment.onItemSelected("+description+")");
        TextView tabNameLabel = (TextView) fragmentView.findViewById(R.id.label_tab_name);
        EditText tabName = (EditText) fragmentView.findViewById(R.id.tab_name);
        TextView fragmentWidthLabel = (TextView) fragmentView.findViewById(R.id.label_fragment_width);
        Spinner fragmentWidthSpinner = (Spinner) fragmentView.findViewById(R.id.spinner_fragment_width);
        Button buttonOk = (Button) fragmentView.findViewById(R.id.ButtonOk);

        if (description.equals("<Select Tab Type to Add>")) {
            tabNameLabel.setVisibility(View.INVISIBLE);
            tabName.setVisibility(View.INVISIBLE);
            fragmentWidthLabel.setVisibility(View.INVISIBLE);
            fragmentWidthSpinner.setVisibility(View.INVISIBLE);
            buttonOk.setEnabled(false);
        } else {
            tabNameLabel.setVisibility(View.VISIBLE);
            tabName.setVisibility(View.VISIBLE);
            fragmentWidthLabel.setVisibility(View.VISIBLE);
            fragmentWidthSpinner.setVisibility(View.VISIBLE);
            buttonOk.setEnabled(true);

            String name = fragmentChoices.get(i).get("ft_name");
            name = getUniqueFragmentName(name);  //make sure the default name is not already in use by another tab
            int width = Integer.parseInt(fragmentChoices.get(i).get("ft_width")) - 1;  //set default width

            tabName.setText(getUniqueFragmentName(name));
            fragmentWidthSpinner.setSelection(width);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Log.d(Consts.APP_NAME, "in AddTabDialogFragment.onNothingSelected()");
    }

    @Override  //process clicks on the OK and Cancel buttons
    public void onClick(View view) {
        if (view.getId()==R.id.ButtonCancel) {
            dismiss();  //just close the dialog
        } else if (view.getId()==R.id.ButtonOk) {
//            TextView tabNameLabel = (TextView) fragmentView.findViewById(R.id.label_tab_name);
            EditText tabName = (EditText) fragmentView.findViewById(R.id.tab_name);

//            TextView fragmentWidthLabel = (TextView) fragmentView.findViewById(R.id.label_fragment_width);
            Spinner fWidthSpinner = (Spinner) fragmentView.findViewById(R.id.spinner_fragment_width);
            Spinner fChoicesSpinner = (Spinner) fragmentView.findViewById(R.id.spinner_fragment_choices);
            int pos = fChoicesSpinner.getSelectedItemPosition();
            String type = fragmentChoices.get(pos).get("ft_type");
            String data = fragmentChoices.get(pos).get("ft_data");
            String name = getUniqueFragmentName(tabName.getText().toString());  //insure name is unique //TODO: give user a chance to change
            int width = Integer.parseInt(fWidthSpinner.getSelectedItem().toString());
            Log.d(Consts.APP_NAME, "ready to add Tab of " + type + " called " + name + " width " + width);
            //put the modified values into a hashmap for shipment in the message
            HashMap<String, String> hm = NewFragmentChoice("N/A", name, type, width + "", data);
            Gson gson = new Gson();
            String hmJson = gson.toJson(hm, new TypeToken<HashMap<String, String>>() {}.getType());
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.ADD_TAB_REQUESTED, hmJson);
            dismiss();  //close the dialog box

        }
    }
}
