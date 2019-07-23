/*Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jmri.enginedriver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmri.enginedriver.Consist.ConLoco;
import jmri.jmrit.roster.RosterEntry;

//import java.util.Arrays;
//import java.util.Collection;

public class select_loco extends Activity {
    static public final int RESULT_LOCO_EDIT = RESULT_FIRST_USER;

//    private static final int GONE = 8;
//    private static final int VISIBLE = 0;

    private static final String WHICH_METHOD_FIRST = "0"; // furst time the app has been used
    private static final String WHICH_METHOD_ADDRESS = "1";
    private static final String WHICH_METHOD_ROSTER = "2";
    private static final String WHICH_METHOD_RECENT = "3";

    ArrayList<HashMap<String, String>> recent_engine_list;
    ArrayList<HashMap<String, String>> roster_list;
    private RosterSimpleAdapter roster_list_adapter;
    private RecentSimpleAdapter recent_list_adapter;

    private ArrayList<Integer> engine_address_list;
    private ArrayList<Integer> address_size_list; // Look at address_type.java

    private int engine_address;
    private int address_size;
    private String sWhichThrottle = "0";  // "0" or "1" or "2" + roster name
    int whichThrottle = 0;
    private int result;
    protected boolean selectLocoRendered = false;         // this will be true once set_labels() runs following rendering of the loco select textViews

    protected threaded_application mainapp; // hold pointer to mainapp

    private SharedPreferences prefs;
    private String default_address_length;
    private Menu SMenu;
    private boolean navigatingAway = false;     // flag for onPause: set to true when another activity is selected, false if going into background 

    protected int layoutViewId = R.layout.select_loco;

    private int clearListCount = 0;

    private String prefRosterFilter = "";
    EditText filter_roster_text;

    RelativeLayout rlAddress;
    RelativeLayout rlAddressHelp;
    RelativeLayout rlRosterHeader;
    RelativeLayout rlRosterEmpty;
    LinearLayout llRoster;
    RelativeLayout rlRecentHeader;
    LinearLayout llRecent;
    RadioButton rbAddress;
    RadioButton rbRoster;
    RadioButton rbRecent;
    String prefSelectLocoMethod = WHICH_METHOD_FIRST;

    boolean prefRosterRecentLocoNames = true;
    boolean removingEngine = false; //flag used to indicate that the selected loco is being removed and not to save it.
    ListView engine_list_view;

    // populate the on-screen roster view from global hashmap
    public void refresh_roster_list() {
        // clear and rebuild
        roster_list.clear();
        if (((mainapp.roster_entries != null)  // add roster and consist entries if any defined
                && (mainapp.roster_entries.size() > 0))
                || ((mainapp.consist_entries != null)
                && (mainapp.consist_entries.size() > 0))) {

            //put roster entries into screen list
            if (mainapp.roster_entries != null) {
                ArrayList<String> rns = new ArrayList<>(mainapp.roster_entries.keySet());  //copy from synchronized map to avoid holding it while iterating
                for (String rostername : rns) {
                    if ((prefRosterFilter.length() == 0) || (rostername.toUpperCase().contains(prefRosterFilter.toUpperCase()))) {
                        // put key and values into temp hashmap
                        HashMap<String, String> hm = new HashMap<>();
                        hm.put("roster_name", rostername);
                        hm.put("roster_address", mainapp.roster_entries.get(rostername));
                        hm.put("roster_entry_type", "loco");
                        //add icon if url set
                        if (mainapp.roster != null) {
                            if (mainapp.roster.get(rostername) != null) {
                                if ((mainapp.roster != null) && (mainapp.roster.get(rostername) != null) && (mainapp.roster.get(rostername).getIconPath() != null)) {
                                    hm.put("roster_icon", mainapp.roster.get(rostername).getIconPath() + "?maxHeight=52");  //include sizing instructions
                                } else {
                                    Log.d("Engine_Driver", "xml roster entry " + rostername + " found, but no icon specified.");
                                }
                            } else {
                                Log.w("Engine_Driver", "WiThrottle roster entry " + rostername + " not found in xml roster.");
                            }
                        } else {
                            Log.w("Engine_Driver", "xml roster not available");
                        }
                        // add temp hashmap to list which view is hooked to
                        roster_list.add(hm);
                    }
                }
            }

            //add consist entries to screen list
            if (mainapp.consist_entries != null) {
                ArrayList<String> ces = new ArrayList<>(mainapp.consist_entries.keySet());  //copy from synchronized map to avoid holding it while iterating
                for (String consist_addr : ces) {
                    // put key and values into temp hashmap
                    HashMap<String, String> hm = new HashMap<>();
                    hm.put("roster_name", mainapp.consist_entries.get(consist_addr));
                    hm.put("roster_address", consist_addr);
                    hm.put("roster_entry_type", "consist");

                    // add temp hashmap to list which view is hooked to
                    roster_list.add(hm);
                }
            }

            Comparator<HashMap<String, String>> comparator = new Comparator<HashMap<String, String>>() {
                @Override
                public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
                    return arg0.get("roster_name").compareToIgnoreCase(arg1.get("roster_name"));
                }
            };
            Collections.sort(roster_list, comparator);

            roster_list_adapter.notifyDataSetChanged();
            View v = findViewById(R.id.roster_list_heading);
            v.setVisibility(View.VISIBLE);
            v = findViewById(R.id.filter_roster_text);
            v.setVisibility(View.VISIBLE);
            v = findViewById(R.id.roster_list);
            v.setVisibility(View.VISIBLE);
            v = findViewById(R.id.roster_list_empty);
            v.setVisibility(View.GONE);

        } else { // hide roster section if nothing to show
            View v = findViewById(R.id.roster_list_heading);
            v.setVisibility(View.GONE);
            v = findViewById(R.id.filter_roster_text);
            v.setVisibility(View.GONE);
            v = findViewById(R.id.roster_list);
            v.setVisibility(View.GONE);
            v = findViewById(R.id.roster_list_empty);
            v.setVisibility(View.VISIBLE);
        } // if roster_entries not null
    }

    private String getLocoNameFromRoster(String engineAddressString) {
        if (prefRosterRecentLocoNames) {
            if ((mainapp.roster_entries != null) && (mainapp.roster_entries.size() > 0)) {
                for (String rostername : mainapp.roster_entries.keySet()) {  // loop thru roster entries,
                    if (mainapp.roster_entries.get(rostername).equals(engineAddressString)) { //looking for value = input parm
//                        return engineAddressString + " - " + rostername;  //if found, return the roster name (key)
                        return rostername;  //if found, return the roster name (key)
                    }
                }
            }
            if (mainapp.getConsistNameFromAddress(engineAddressString) != null) { //check for a JMRI consist for this address
                return mainapp.getConsistNameFromAddress(engineAddressString);
            }
        }
        return engineAddressString;
    }

    private String getLocoIconUrlFromRoster(String engineAddressString) {
        if (prefRosterRecentLocoNames) {
            if ((mainapp.roster_entries != null) && (mainapp.roster_entries.size() > 0) && (mainapp.roster != null)) {
                for (String rostername : mainapp.roster_entries.keySet()) {  // loop thru roster entries,
                    if (mainapp.roster_entries.get(rostername).equals(engineAddressString)) { //looking for value = input parm
                        RosterEntry rosterentry = mainapp.roster.get(rostername);
                        if (rosterentry == null) return "";
                        String iconPath = rosterentry.getIconPath();  //if found, return the icon url
                        if (iconPath == null) return "";
                        return iconPath;
                    }
                }
            }
        }
        return "";
    }

    // lookup and set values of various text labels
    protected void set_labels() {

        refresh_roster_list();
        if (prefSelectLocoMethod.equals(WHICH_METHOD_FIRST)) {
            if ((mainapp.roster != null) && (mainapp.roster.size() > 0)) {
                prefSelectLocoMethod = WHICH_METHOD_ROSTER;
                showMethod(WHICH_METHOD_ROSTER);
            } else {
                prefSelectLocoMethod = WHICH_METHOD_ADDRESS;
                showMethod(WHICH_METHOD_ADDRESS);
            }
        }

        boolean prefShowAddressInsteadOfName = prefs.getBoolean("prefShowAddressInsteadOfName", getResources().getBoolean(R.bool.prefShowAddressInsteadOfNameDefaultValue));

        TextView tvThrottleNameHeader = findViewById(R.id.throttle_name_header);
        // show throttle name
        String s = "Throttle Name: "
                + prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
        tvThrottleNameHeader.setText(s);

        // format and show currently selected locos, and hide or show Release buttons
        final int conNomTextSize = 16;
        final double minTextScale = 0.8;

        Button bR = findViewById(R.id.Sl_release);
        LinearLayout llThrottle = findViewById(R.id.LL_loco);

        TextView tvSelectLocoHeading = findViewById(R.id.select_loco_heading);
        tvSelectLocoHeading.setText(this.getResources().getString(R.string.select_loco_heading).replace("%1$s", Integer.toString(mainapp.throttleCharToInt(sWhichThrottle.charAt(0)) + 1)));

        //hide the release button row if nothing currently aquired
//        TextView tvThrottleNameHeader = findViewById(R.id.throttle_name_header);
        if (mainapp.consists[whichThrottle].isActive()) {
            llThrottle.setVisibility(View.GONE);
            tvThrottleNameHeader.setVisibility(View.VISIBLE);
        } else {
            llThrottle.setVisibility(View.VISIBLE);
            tvThrottleNameHeader.setVisibility(View.GONE);
        }

        bR.setVisibility(View.VISIBLE);
        llThrottle.setVisibility(View.VISIBLE);

        if (mainapp.consists[whichThrottle].isActive()) {
            String vLabel = mainapp.consists[whichThrottle].toString();
            if (prefShowAddressInsteadOfName) { // show the DCC Address instead of the loco name if the preference is set
                vLabel = mainapp.consists[whichThrottle].formatConsistAddr();
            }
            bR.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);

            // scale text if required to fit the button
            double textScale = 1.0;
            int bWidth = bR.getWidth();
            double textWidth = bR.getPaint().measureText(vLabel);

            if (bWidth == 0) { // screen has probably not rendered yet
                final DisplayMetrics dm = getResources().getDisplayMetrics();
                // Get the screen's density scale
                final float denScale = dm.density;
                int screenWidth = dm.widthPixels; // get the width of usable area
                bWidth = (screenWidth - (int) (denScale * 6)) / 2;
            }
            if (textWidth > 0 && textWidth > bWidth) {
                textScale = bWidth / textWidth;
                if (textScale < minTextScale)
                    textScale = minTextScale;
            }
            int textSize = (int) (conNomTextSize * textScale);
            bR.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

            selectLocoRendered = true;
            bR.setText(this.getResources().getString(R.string.releaseThrottleLocos).replace("%1$s", vLabel));
            bR.setEnabled(true);
        } else {
            bR.setEnabled(false);
            bR.setVisibility(View.GONE);
            llThrottle.setVisibility(View.GONE);
        }

        if (SMenu != null) {
            mainapp.displayEStop(SMenu);
        }

    }

    // Handle messages from the communication thread back to this thread
    // (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class select_loco_handler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.ROSTER_UPDATE:
                    //refresh labels when any roster response is received
                    roster_list_adapter.notifyDataSetChanged();
                    set_labels();
                    break;
                case message_type.RESPONSE:
                    String response_str = msg.obj.toString();
                    if (response_str.length() >= 1) {
                        char com1 = response_str.charAt(0);
                        if (com1 == 'R') {                                  //refresh labels when any roster response is received
                            roster_list_adapter.notifyDataSetChanged();
                            set_labels();
                        } else if (com1 == 'M' && response_str.length() >= 3) { // refresh Release buttons if loco is added or removed from a consist
                            char com2 = response_str.charAt(2);
                            if (com2 == '+' || com2 == '-')
                                set_labels();
                        }
                    }
                    if (!selectLocoRendered)         // call set_labels if the select loco textViews had not rendered the last time it was called
                        set_labels();
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    roster_list_adapter.notifyDataSetChanged();
                    set_labels();
                    break;
                case message_type.DISCONNECT:
                    end_this_activity();
                    break;
            }
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        navigatingAway = true;
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    // request release of specified throttle
    void release_loco(int whichThrottle) {
        mainapp.consists[whichThrottle].release();
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", whichThrottle); // pass 0, 1 or 2 in message
    }

    boolean saveUpdateList;         // save value across ConsistEdit activity 
    boolean newEngine;              // save value across ConsistEdit activity

    void acquire_engine(boolean bUpdateList) {
        String roster_name = sWhichThrottle.substring(1);
        String sAddr = (address_size == address_type.LONG ? "L" : "S") + engine_address;
        String addr = sAddr;
        Loco l = new Loco(addr);
        if (!roster_name.equals("")) {
            l.setDesc(roster_name);       //use rosterName if present
            l.setRosterName(roster_name); //use rosterName if present
            l.setIsFromRoster(true);
        } else {
            l.setDesc(mainapp.getRosterNameFromAddress(l.toString()));  //lookup rostername from address if not set
            l.setRosterName(null); //make sure rosterName is null
            l.setFunctionLabelDefaults(mainapp, whichThrottle); //make sure rosterName is null
        }
        Consist consist = mainapp.consists[whichThrottle];

        if (sWhichThrottle.length() > 1) // add roster selection info if present
            addr += "<;>" + sWhichThrottle.substring(1);


        //user preference set to not consist, or consisting not supported in this JMRI, so drop before adding
        if (prefs.getBoolean("drop_on_acquire_preference", false)) {
            ConLoco cl = consist.getLoco(sAddr);
            if (cl == null) { // if the newly selected loco is different/not in the consist, release everything
                release_loco(whichThrottle);
            } else { // already have it so don't do anything
                result = RESULT_OK;
                end_this_activity();
                return;
            }
        }

        if (!consist.isActive()) {               // if this is the only loco in consist then just tell WiT and exit
            consist.add(l);
            consist.setLeadAddr(l.getAddress());
            consist.setTrailAddr(l.getAddress());
//            consist.setConfirmed(l.getAddress()); //this happens after response from WiTS
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, addr, whichThrottle);
            updateRecentEngines(bUpdateList);
            result = RESULT_OK;
            end_this_activity();

        } else {                                // else consist exists so bring up editor
            ConLoco cl = consist.getLoco(addr);
            newEngine = (cl == null);
            if (newEngine || !cl.isConfirmed()) {        // if engine is not already in the consist, or if it is but never got acquired
                consist.add(l);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, addr, whichThrottle);

                saveUpdateList = bUpdateList;
                Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);
                consistEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));

                consist.setTrailAddr(l.getAddress());  // set the newly added loco as the trailing loco

                navigatingAway = true;
                startActivityForResult(consistEdit, throttle.ACTIVITY_CONSIST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            }
        }
    }

    //handle return from ConsistEdit
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == throttle.ACTIVITY_CONSIST) {                          // edit consist
            if (newEngine) {
                updateRecentEngines(saveUpdateList);
            }
            result = RESULT_LOCO_EDIT;                 //tell Throttle to update loco directions
        }
        end_this_activity();
    }

    void updateRecentEngines(boolean bUpdateList) {
        //if not updating list or no SD Card present then nothing else to do
        if (!bUpdateList || !android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;
        // Save the engine list to the recent_engine_list.txt file
        File sdcard_path = Environment.getExternalStorageDirectory();
        File connections_list_file = new File(sdcard_path,
                "engine_driver/recent_engine_list.txt");
        PrintWriter list_output;
        String smrl = prefs.getString("maximum_recent_locos_preference", ""); //retrieve pref for max recent locos to show  
        try {
            int mrl = 10; //default to 10 if pref is blank or invalid
            try {
                mrl = Integer.parseInt(smrl);
            } catch (NumberFormatException ignored) {
            }
            list_output = new PrintWriter(connections_list_file);
            if (mrl > 0) {
                // Add this engine to the head of recent engines list.
                mrl--;
                if (!removingEngine) {
                    list_output.format("%d:%d\n", engine_address, address_size);
                }
                removingEngine = false;
                for (int i = 0; i < engine_address_list.size() && mrl > 0; i++) {
                    if (engine_address != engine_address_list.get(i) || address_size != address_size_list.get(i)) {
                        list_output.format("%d:%d\n", engine_address_list.get(i), address_size_list.get(i));
                        mrl--;
                    }
                }
            }
            list_output.flush();
            list_output.close();
        } catch (IOException except) {
            Log.e("Engine_Driver",
                    "select_loco - Error creating a PrintWriter, IOException: "
                            + except.getMessage());
        }
    }

    public class button_listener implements View.OnClickListener {
        public void onClick(View v) {
            EditText entry = findViewById(R.id.loco_address);
            try {
                engine_address = Integer.valueOf(entry.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(getApplicationContext(), "ERROR - Please enter a valid DCC address.\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            Spinner spinner = findViewById(R.id.address_length);
            address_size = spinner.getSelectedItemPosition();
            acquire_engine(true);
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close

        }
    }

    public class release_button_listener implements View.OnClickListener {
        int _throttle;

        public release_button_listener(int throttle) {
            _throttle = throttle;
        }

        public void onClick(View v) {
            release_loco(_throttle);
            end_this_activity();
        }
    }

    public class engine_item implements AdapterView.OnItemClickListener {
        // When an item is clicked, acquire that engine.
        public void onItemClick(AdapterView<?> parent, View v, int position,
                                long id) {
            engine_address = engine_address_list.get(position);
            address_size = address_size_list.get(position);
            acquire_engine(true);
        }
    }

    //Jeffrey M added 7/3/2013
    //Clears recent connection list of locos when button is touched or clicked
    public class clear_Loco_List_button implements AdapterView.OnClickListener {
        public void onClick(View v) {
            clearListCount++;
            if (clearListCount <= 1) {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastSelectLocoConfirmClear), Toast.LENGTH_LONG).show();
            } else { // only clear the list if the button is clicked a second time
                clearList();
                clearListCount = 0;
            }
            onCreate(null);
        }
    }

    public class roster_item_ClickListener implements
            AdapterView.OnItemClickListener {
        // When a roster item is clicked, send request to acquire that engine.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            //use clicked position in list to retrieve roster item object from roster_list
            HashMap<String, String> hm = roster_list.get(position);
            String rosterNameString = hm.get("roster_name");
            String rosterAddressString = hm.get("roster_address");
            String rosterEntryType = hm.get("roster_entry_type");
            // parse address and length from string, e.g. 2591(L)
            String ras[] = threaded_application.splitByString(rosterAddressString, "(");
            if (ras[0].length() > 0) {  //only process if address found
                address_size = (ras[1].charAt(0) == 'L')
                        ? address_type.LONG
                        : address_type.SHORT;   // convert S/L to 0/1
                try {
                    engine_address = Integer.valueOf(ras[0]);   // convert address to int
                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), "ERROR - could not parse address\n" + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return; //get out, don't try to acquire
                }
                if ("loco".equals(rosterEntryType)) {
                    sWhichThrottle += rosterNameString;     //append rostername if type is loco (not consist) 
                }
                boolean bRosterRecent = prefs.getBoolean("roster_recent_locos_preference",
                        getResources().getBoolean(R.bool.prefRosterRecentLocosDefaultValue));
                acquire_engine(bRosterRecent);
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    private void filterRoster() {
        prefRosterFilter = filter_roster_text.getText().toString().trim();
        prefs.edit().putString("prefRosterFilter", prefRosterFilter).commit();
        refresh_roster_list();
        //        onCreate(null);
    }

    // Handle pressing of the back button to simply return to caller
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            end_this_activity();
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        result = RESULT_CANCELED;
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        mainapp.applyTheme(this);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_select_loco)); // needed in case the langauge was changed from the default

//        setContentView(R.layout.select_loco);
        setContentView(layoutViewId);

        // put pointer to this activity's handler in main app's shared variable
        mainapp.select_loco_msg_handler = new select_loco_handler();

        prefRosterFilter = prefs.getString("prefRosterFilter", this.getResources().getString(R.string.prefRosterFilterDefaultValue));
        prefRosterRecentLocoNames = prefs.getBoolean("prefRosterRecentLocoNames",
                getResources().getBoolean(R.bool.prefRosterRecentLocoNamesDefaultValue));


        // Set the options for the address length.
        Spinner address_spinner = findViewById(R.id.address_length);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this,
                R.array.address_size, android.R.layout.simple_spinner_item);
        spinner_adapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        address_spinner.setAdapter(spinner_adapter);

        // Set up a list adapter to contain the current roster list.
        roster_list = new ArrayList<>();
        roster_list_adapter = new RosterSimpleAdapter(this, roster_list,
                R.layout.roster_list_item, new String[]{"roster_name",
                "roster_address", "roster_icon"}, new int[]{R.id.roster_name_label,
                R.id.roster_address_label, R.id.roster_icon_image});

        ListView roster_list_view = findViewById(R.id.roster_list);
        roster_list_view.setAdapter(roster_list_adapter);
        roster_list_view.setOnItemClickListener(new roster_item_ClickListener());
        roster_list_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });


        //      refresh_roster_list();

        // Set up a list adapter to allow adding the list of recent engines to
        // the UI.
        recent_engine_list = new ArrayList<>();
        recent_list_adapter = new RecentSimpleAdapter(this, recent_engine_list,
                R.layout.engine_list_item, new String[]{"engine"},
                new int[]{R.id.engine_item_label, R.id.engine_icon_image});
        engine_list_view = findViewById(R.id.engine_list);
        engine_list_view.setAdapter(recent_list_adapter);
        engine_list_view.setOnItemClickListener(new engine_item());
        engine_list_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongRecentListItemClick(v, pos, id);
            }
        });


        // simliar, but different, code exists in importExportPreferences.java. if you modify one, make sure you modify the other
        engine_address_list = new ArrayList<>();
        address_size_list = new ArrayList<>();
        //if no SD Card present then there is no recent locos list
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent locos list requires SD Card
            TextView v = findViewById(R.id.recent_engines_heading);
            v.setText(getString(R.string.sl_recent_engine_notice));
        } else {
            try {
                // Populate the ListView with the recent engines saved in a file. This
                // will be stored in /sdcard/engine_driver/recent_engine_list.txt
                // entries not matching the assumptions will be ignored
                File sdcard_path = Environment.getExternalStorageDirectory();
                File engine_list_file = new File(sdcard_path + "/engine_driver/recent_engine_list.txt");
                if (engine_list_file.exists()) {
                    BufferedReader list_reader = new BufferedReader(
                            new FileReader(engine_list_file));
                    while (list_reader.ready()) {
                        String line = list_reader.readLine();
                        Integer splitPos = line.indexOf(':');
                        if (splitPos > 0) {
                            Integer ea, as;
                            try {
                                ea = Integer.decode(line.substring(0, splitPos));
                                as = Integer.decode(line.substring(splitPos + 1, line.length()));
                            } catch (Exception e) {
                                ea = -1;
                                as = -1;
                            }

                            if ((ea >= 0) && (as >= 0)) {
                                engine_address_list.add(ea);
                                address_size_list.add(as);
                                HashMap<String, String> hm = new HashMap<>();
                                String addressLengthString = ((as == 0) ? "S" : "L");  //show L or S based on length from file
                                String engineAddressString = String.format("%s(%s)", ea.toString(), addressLengthString);
                                String engineIconUrl = getLocoIconUrlFromRoster(engineAddressString);
//                                engineAddressString = getLocoNameFromRoster(engineAddressString);
                                hm.put("engine_icon", engineIconUrl);
                                hm.put("engine", getLocoNameFromRoster(engineAddressString));
                                hm.put("engine_name", engineAddressString);
                                recent_engine_list.add(hm);
                            } //if ea>=0&&as>=0
                        } //if splitPos>0
                    }
                    list_reader.close();
                    recent_list_adapter.notifyDataSetChanged();
                }

            } catch (IOException except) {
                Log.e("Engine_Driver", "select_loco - Error reading recent loco file. "
                        + except.getMessage());
            }
        }

        // Set the button callbacks.
        Button button = findViewById(R.id.acquire);
        button_listener click_listener = new button_listener();
        button.setOnClickListener(click_listener);

        //Jeffrey added 7/3/2013
        button = findViewById(R.id.clear_Loco_List_button);
        button.setOnClickListener(new clear_Loco_List_button());

        filter_roster_text = findViewById(R.id.filter_roster_text);
        filter_roster_text.setText(prefRosterFilter);
        filter_roster_text.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                filterRoster();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        filter_roster_text.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                    filterRoster();
                    return true;
                } else
                    return false;
            }
        });

        default_address_length = prefs.getString("default_address_length", this
                .getResources().getString(
                        R.string.prefDefaultAddressLengthDefaultValue));
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sWhichThrottle = extras.getString("sWhichThrottle");
            whichThrottle = mainapp.throttleCharToInt(sWhichThrottle.charAt(0));
        }

        button = findViewById(R.id.Sl_release);
        button.setOnClickListener(new release_button_listener(whichThrottle));

        EditText la = findViewById(R.id.loco_address);
        la.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                updateAddressEntry();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        la.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    return true;
                } else
                    return false;
            }
        });

        rbAddress = findViewById(R.id.select_loco_method_address_button);
        rbRoster = findViewById(R.id.select_loco_method_roster_button);
        rbRecent = findViewById(R.id.select_loco_method_recent_button);

        prefSelectLocoMethod = prefs.getString("prefSelectLocoMethod", WHICH_METHOD_FIRST);

        rlAddress = findViewById(R.id.enter_loco_group);
        rlAddressHelp = findViewById(R.id.enter_loco_group_help);
        rlRosterHeader = findViewById(R.id.roster_list_header_group);
        rlRosterEmpty = findViewById(R.id.roster_list_empty_group);
        llRoster = findViewById(R.id.roster_list_group);
        rlRecentHeader = findViewById(R.id.engine_list_header_group);
        llRecent = findViewById(R.id.engine_list_wrapper);
        showMethod(prefSelectLocoMethod);

        RadioGroup rgLocoSelect = findViewById(R.id.select_loco_method_address_button_radio_group);
        rgLocoSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.select_loco_method_roster_button:
                        showMethod(WHICH_METHOD_ROSTER);
                        break;
                    case R.id.select_loco_method_recent_button:
                        showMethod(WHICH_METHOD_RECENT);
                        break;
                    case R.id.select_loco_method_address_button:
                        showMethod(WHICH_METHOD_ADDRESS);
                        break;
                }
            }
        });

        set_labels();
    }

    @SuppressLint("ApplySharedPref")
    private void showMethod(String whichMethod) {
        switch (whichMethod) {
            default:
            case WHICH_METHOD_ADDRESS: {
                rlAddress.setVisibility(View.VISIBLE);
                rlAddressHelp.setVisibility(View.VISIBLE);
                rlRosterHeader.setVisibility(View.GONE);
                llRoster.setVisibility(View.GONE);
                rlRosterEmpty.setVisibility(View.GONE);
                rlRecentHeader.setVisibility(View.GONE);
                llRecent.setVisibility(View.GONE);

                rbAddress.setChecked(true);
                rbRoster.setChecked(false);
                rbRecent.setChecked(false);
                break;
            }
            case WHICH_METHOD_ROSTER: {
                rlAddress.setVisibility(View.GONE);
                rlAddressHelp.setVisibility(View.GONE);
                rlRosterHeader.setVisibility(View.VISIBLE);
                llRoster.setVisibility(View.VISIBLE);
                rlRosterEmpty.setVisibility(View.VISIBLE);
                rlRecentHeader.setVisibility(View.GONE);
                llRecent.setVisibility(View.GONE);

                rbAddress.setChecked(false);
                rbRoster.setChecked(true);
                rbRecent.setChecked(false);
                break;
            }
            case WHICH_METHOD_RECENT: {
                rlAddress.setVisibility(View.GONE);
                rlAddressHelp.setVisibility(View.GONE);
                rlRosterHeader.setVisibility(View.GONE);
                llRoster.setVisibility(View.GONE);
                rlRosterEmpty.setVisibility(View.GONE);
                rlRecentHeader.setVisibility(View.VISIBLE);
                llRecent.setVisibility(View.VISIBLE);

                rbAddress.setChecked(false);
                rbRoster.setChecked(false);
                rbRecent.setChecked(true);
            }
        }
        prefs.edit().putString("prefSelectLocoMethod", whichMethod).commit();
    }


    //Jeffrey added 7/3/2013
    //Clears recent connection list of locos
    public void clearList() {
        File sdcard_path = Environment.getExternalStorageDirectory();
        File engine_list_file = new File(sdcard_path + "/engine_driver/recent_engine_list.txt");

        if (engine_list_file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            engine_list_file.delete();
            recent_engine_list.clear();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        navigatingAway = false;

        // checking address length here covers (future) case where prefs changed while paused
        default_address_length = prefs.getString("default_address_length", this
                .getResources().getString(R.string.prefDefaultAddressLengthDefaultValue));
        updateAddressEntry();   // enable/disable buttons
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.isFinishing() && !navigatingAway) {        //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "select_loco.onDestroy() called");
        mainapp.select_loco_msg_handler = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.select_loco_menu, menu);
        SMenu = menu;
        mainapp.displayEStop(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // end current activity
    void end_this_activity() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("whichThrottle", sWhichThrottle.charAt(0));  //pass whichThrottle as an extra
        setResult(result, resultIntent);
        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private int updateAddressEntry() {
        Button ba = findViewById(R.id.acquire);
        EditText la = findViewById(R.id.loco_address);
        int txtLen = la.getText().toString().trim().length();

        // don't allow acquire button if nothing entered
        if (txtLen > 0) {
            ba.setEnabled(true);
        } else {
            ba.setEnabled(false);
        }

        // set address length
        Spinner al = findViewById(R.id.address_length);
        if (default_address_length.equals("Long")
                || (default_address_length.equals("Auto") && txtLen > 2)) {
            al.setSelection(1);
        } else {
            al.setSelection(0);
        }
        return txtLen;
    }

    protected boolean onLongListItemClick(View v, int position, long id) {
        if (mainapp.roster == null) {
            Log.w("Engine_Driver", "No roster details found.");
            return true;
        }
        HashMap<String, String> hm = roster_list.get(position);
        String rosternamestring = hm.get("roster_name");
        RosterEntry re = mainapp.roster.get(rosternamestring);
        if (re == null) {
            Log.w("Engine_Driver", "Roster entry " + rosternamestring + " not available.");
            return true;
        }
        Log.d("Engine_Driver", "Showing details for roster entry " + rosternamestring);
        Dialog dialog = new Dialog(select_loco.this);
        dialog.setTitle("Roster details for " + rosternamestring);
        dialog.setContentView(R.layout.roster_entry);
        String res = re.toString();
        TextView tv = dialog.findViewById(R.id.rosterEntryText);
        tv.setText(res);
        dialog.setCancelable(true);
        dialog.show();
        return true;
    }

    // long click for the recent loco list items.  Clears the entry from the list
    protected boolean onLongRecentListItemClick(View v, int position, long id) {
        recent_engine_list.remove(position);
        removingEngine = true;
        updateRecentEngines(true);
        engine_list_view.invalidateViews();
        return true;
}

public class RosterSimpleAdapter extends SimpleAdapter {
    private Context cont;

    public RosterSimpleAdapter(Context context,
                               List<? extends Map<String, ?>> data, int resource,
                               String[] from, int[] to) {
        super(context, data, resource, from, to);
        cont = context;
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        if (position > roster_list.size())
            return convertView;

        HashMap<String, String> hm = roster_list.get(position);
        if (hm == null)
            return convertView;

        LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.roster_list_item, null, false);

        String str = hm.get("roster_name");
        if (str != null) {
            TextView name = view.findViewById(R.id.roster_name_label);
            name.setText(str);
        }

        str = hm.get("roster_address");
        if (str != null) {
            TextView secondLine = view.findViewById(R.id.roster_address_label);
            secondLine.setText(hm.get("roster_address"));
        }

        String iconURL = hm.get("roster_icon");
        if ((iconURL != null) && (iconURL.length() > 0)) {
            ImageView imageView = view.findViewById(R.id.roster_icon_image);
            mainapp.imageDownloader.download(iconURL, imageView);
        } else {
            View v = view.findViewById(R.id.roster_icon_image);
            v.setVisibility(View.GONE);
        }

        return view;
    }
}


public class RecentSimpleAdapter extends SimpleAdapter {
    private Context cont;

    public RecentSimpleAdapter(Context context,
                               List<? extends Map<String, ?>> data, int resource,
                               String[] from, int[] to) {
        super(context, data, resource, from, to);
        cont = context;
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        if (position > recent_engine_list.size())
            return convertView;

        HashMap<String, String> hm = recent_engine_list.get(position);
        if (hm == null)
            return convertView;

        LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.engine_list_item, null, false);

        String str = hm.get("engine_name");
        if (str != null) {
            TextView name = view.findViewById(R.id.engine_name_label);
            name.setText(str);
        }

        str = hm.get("engine");
        if (str != null) {
            TextView secondLine = view.findViewById(R.id.engine_item_label);
            secondLine.setText(str);
        }

        String iconURL = hm.get("engine_icon");
        if ((iconURL != null) && (iconURL.length() > 0)) {
            ImageView imageView = view.findViewById(R.id.engine_icon_image);
            mainapp.imageDownloader.download(iconURL, imageView);
        } else {
            View v = view.findViewById(R.id.engine_icon_image);
            v.setVisibility(View.GONE);
        }

        return view;
    }

}

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}
