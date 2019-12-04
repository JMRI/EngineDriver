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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmri.enginedriver.Consist.ConLoco;
import jmri.jmrit.roster.RosterEntry;

import jmri.enginedriver.util.SwipeDetector;

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
    private static final String WHICH_METHOD_CONSIST = "4";

    ArrayList<HashMap<String, String>> recent_engine_list;
    ArrayList<HashMap<String, String>> roster_list;
    private RosterSimpleAdapter roster_list_adapter;
    private RecentSimpleAdapter recent_list_adapter;

    private ArrayList<Integer> recent_loco_address_list;
    private ArrayList<Integer> recent_loco_address_size_list; // Look at address_type.java
    private ArrayList<String> recent_loco_name_list;

    // recent consists
    ArrayList<HashMap<String, String>> recent_consists_list;
    private RecentConsistsSimpleAdapter recent_consists_list_adapter;

    private ArrayList<ArrayList<Integer>> consistEngineAddressList = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> consistAddressSizeList = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> consistDirectionList = new ArrayList<>();
    private ArrayList<String> consistNameList = new ArrayList<>();

    ListView consists_list_view;
    SwipeDetector recentConsistsSwipeDetector;
    SwipeDetector recentsSwipeDetector;
    //

    boolean rosterHelp = false;
    boolean recentsHelp = false;
    boolean recentsConsistsHelp = false;

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
    private int clearConsistsListCount = 0;

    private String prefRosterFilter = "";
    EditText filter_roster_text;

    RelativeLayout rlAddress;
    RelativeLayout rlAddressHelp;
    RelativeLayout rlRosterHeader;
    RelativeLayout rlRosterEmpty;
    LinearLayout llRoster;
    RelativeLayout rlRecentHeader;
    LinearLayout llRecent;
    RelativeLayout rlRecentConsistsHeader;
    LinearLayout llRecentConsists;
    RadioButton rbAddress;
    RadioButton rbRoster;
    RadioButton rbRecent;
    RadioButton rbRecentConsists;
    String prefSelectLocoMethod = WHICH_METHOD_FIRST;

    boolean prefRosterRecentLocoNames = true;
    boolean removingLocoOrForceReload = false; //flag used to indicate that the selected loco is being removed and not to save it.
    boolean removingConsistOrForceRewite = false; //flag used to indicate that the selected consist is being removed and not to save it.
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
//        String sAddr = (address_size == address_type.LONG ? "L" : "S") + engine_address;
        String sAddr = locoAddressToString(engine_address, address_size, true);
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
//            updateRecentConsists(bUpdateList);
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
                updateRecentConsists(saveUpdateList);
            }
            result = RESULT_LOCO_EDIT;                 //tell Throttle to update loco directions
        }
        end_this_activity();
    }

    //write the recent locos to a file
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
                if (!removingLocoOrForceReload) {
                    mrl--;
                    // Add this engine to the head of recent engines list.
                    String locoName = locoAddressToString(engine_address, address_size,false);
                    for (int i = 0; i < recent_loco_address_list.size() && mrl > 0; i++) {
                        if (engine_address == recent_loco_address_list.get(i) && address_size == recent_loco_address_size_list.get(i)) {
                            locoName = recent_loco_name_list.get(i); // grab the current name
                            recent_loco_address_list.remove(i);      // before removing it from its current location in the list
                            recent_loco_address_size_list.remove(i);
                            recent_loco_name_list.remove(i);
                        }
                    }
                    list_output.format("%d:%d~%s\n", engine_address, address_size, locoName);
                }
                removingLocoOrForceReload = false;
                for (int i = 0; i < recent_loco_address_list.size() && mrl > 0; i++) {
//                    if (engine_address != recent_loco_address_list.get(i) || address_size != recent_loco_address_size_list.get(i)) {
                        list_output.format("%d:%d~%s\n", recent_loco_address_list.get(i), recent_loco_address_size_list.get(i), recent_loco_name_list.get(i));
//                        mrl--;
//                    }
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

    // read the recent locos from a file
    // and load the on screen list
    private void loadRecentsList(boolean reload) {
        // simliar, but different, code exists in importExportPreferences.java. if you modify one, make sure you modify the other
        recent_loco_address_list = new ArrayList<>();
        recent_loco_address_size_list = new ArrayList<>();
        recent_loco_name_list = new ArrayList<>();
        if (reload) {
            recent_engine_list = new ArrayList<>();
        }

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
                            String ln = "";
                            try {
                                ea = Integer.decode(line.substring(0, splitPos));
                                as = Integer.decode(line.substring(splitPos + 1,splitPos + 2));
                                if (line.length()>splitPos+2) { // has the name extras
                                    if (line.substring(splitPos + 2,splitPos + 3).equals("~")) { // get the name
                                        ln = line.substring(splitPos + 3);
                                    }
                                }
                            } catch (Exception e) {
                                ea = -1;
                                as = -1;
                                ln = "";
                            }

                            if ((ea >= 0) && (as >= 0)) {
                                recent_loco_address_list.add(ea);
                                recent_loco_address_size_list.add(as);
                                HashMap<String, String> hm = new HashMap<>();
//                                String addressLengthString = ((as == 0) ? "S" : "L");  //show L or S based on length from file
                                String engineAddressString = locoAddressToString(ea, as, false);
                                if ((ln.length()==0  || ln.equals(engineAddressString))) { // if nothing is stored, or what is stored is the same as the address, look for it in the roster
                                    ln = getLocoNameFromRoster(engineAddressString);
                                }
                                recent_loco_name_list.add(ln);

                                String engineIconUrl = getLocoIconUrlFromRoster(engineAddressString);
//                                engineAddressString = getLocoNameFromRoster(engineAddressString);
                                hm.put("engine_icon", engineIconUrl);
                                hm.put("engine", ln);
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
        rbRecent = findViewById(R.id.select_loco_method_recent_button);
        if (recent_loco_address_list.size()==0) {  // if the list is empty, hide the radio button
            rbRecent.setVisibility(View.GONE);
        } else {
            rbRecent.setVisibility(View.VISIBLE);
        }
    }

    // read the recent consists from a file
    // and load the on screen list
    private void loadRecentConsistsList(boolean reload) {
        consistEngineAddressList = new ArrayList<>();
        consistAddressSizeList = new ArrayList<>();
        consistDirectionList = new ArrayList<>();
        consistNameList = new ArrayList<>();
        if (reload) {
            recent_consists_list = new ArrayList<>();
        }

        ArrayList<Integer> tempConsistEngineAddressList_inner;
        ArrayList<Integer> tempConsistAddressSizeList_inner;
        ArrayList<Integer> tempConsistDirectionList_inner;

        //if no SD Card present then there is no recent consists list
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent locos list requires SD Card
            TextView v = findViewById(R.id.recent_consists_heading);
            v.setText(getString(R.string.sl_recent_engine_notice));
        } else {
            try {
                // Populate the list with the recent consists saved in a file
                File sdcard_path = Environment.getExternalStorageDirectory();
                File consist_list_file = new File(sdcard_path + "/engine_driver/recent_consist_list.txt");
                if (consist_list_file.exists()) {
                    BufferedReader list_reader = new BufferedReader(
                            new FileReader(consist_list_file));
                    while (list_reader.ready()) {
                        StringBuilder oneConsist = new StringBuilder();
                        String line = list_reader.readLine();
                        tempConsistEngineAddressList_inner = new ArrayList<>();
                        tempConsistAddressSizeList_inner = new ArrayList<>();
                        tempConsistDirectionList_inner = new ArrayList<>();

                        String consistName = "";
                        int splitName = line.indexOf('~');
                        if (splitName!=-1) { // see if there is a name saved as well
                            consistName = line.substring(splitName+1);
                            line = line.substring(0,splitName);
                        }

                        int splitLoco = line.indexOf(',');
                        if (splitLoco!=-1) {
                            oneConsist.append(addOneConsistAddress(line, 0, splitLoco, tempConsistEngineAddressList_inner, tempConsistAddressSizeList_inner, tempConsistDirectionList_inner));

                            boolean foundOne = true;
                            while (foundOne) {
                                Integer prevSplitLoco = splitLoco + 1;
                                splitLoco = line.indexOf(',', prevSplitLoco);
                                if (splitLoco != -1) {
                                    oneConsist.append(addOneConsistAddress(line, prevSplitLoco, splitLoco, tempConsistEngineAddressList_inner, tempConsistAddressSizeList_inner, tempConsistDirectionList_inner));
                                } else {
                                    oneConsist.append(addOneConsistAddress(line, prevSplitLoco, line.length(), tempConsistEngineAddressList_inner, tempConsistAddressSizeList_inner, tempConsistDirectionList_inner));
                                    foundOne = false;
                                }
                            }
                            consistEngineAddressList.add(tempConsistEngineAddressList_inner);
                            consistAddressSizeList.add(tempConsistAddressSizeList_inner);
                            consistDirectionList.add(tempConsistDirectionList_inner);
                            if (consistName.length()==0) { consistName = oneConsist.toString(); }
                            consistNameList.add(consistName);

                            HashMap<String, String> hm = new HashMap<>();
                            hm.put("consist_name", getLocoNameFromRoster(oneConsist.toString()));
                            hm.put("consist", consistName);
                            recent_consists_list.add(hm);
                        }
                    }
                    list_reader.close();
                    recent_consists_list_adapter.notifyDataSetChanged();
                }

            } catch (IOException except) {
                Log.e("Engine_Driver", "select_loco - Error reading recent loco file. "
                        + except.getMessage());
            }

            RadioButton myRadioButton = findViewById(R.id.select_consists_method_recent_button);
            if (consistEngineAddressList.size()==0) {
                myRadioButton.setVisibility(View.GONE); // if the list is empty, hide the radio button
            } else {
                myRadioButton.setVisibility(View.VISIBLE);
            }
        }
    }
    String addOneConsistAddress(String line, Integer start, Integer end, ArrayList<Integer> tempConsistEngineAddressList_inner, ArrayList<Integer> tempConsistAddressSizeList_inner, ArrayList<Integer> tempConsistDirectionList_inner) {
        String rslt = "";
        String splitLine = line.substring(start, end);
        int splitPos = splitLine.indexOf(':');
        if (splitPos!=-1) {
            Integer addr = Integer.decode(splitLine.substring(0, splitPos));
            Integer size = Integer.decode(splitLine.substring(splitPos + 1, splitPos + 2));
            Integer dir = Integer.decode(splitLine.substring(splitPos + 2, splitPos + 3));
            tempConsistEngineAddressList_inner.add(addr);
            tempConsistAddressSizeList_inner.add(size);
            tempConsistDirectionList_inner.add(dir);
            rslt = "<span>" + addr.toString()+"<small><small>("+ (size==0 ? "S":"L") +")</small>"+ (dir==0 ? "▲":"▼") + "</small> &nbsp;</span>";
//            rslt = addr.toString() + " " + (dir==0 ? "F>":"<R") + " ";
        }
        return rslt;
    }

    //write the recent consists to a file
    void updateRecentConsists(boolean bUpdateList) {
        ArrayList<Integer> tempConsistEngineAddressList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistAddressSizeList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistDirectionList_inner = new ArrayList<>();
        boolean haveConsist = false;

        //if not updating list or no SD Card present then nothing else to do
        if (!bUpdateList || !android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;

        Consist consist = mainapp.consists[whichThrottle];
        Collection<ConLoco> conLocos = consist.getLocos();

        if (!removingConsistOrForceRewite) {

            for (ConLoco l : conLocos) {
                tempConsistEngineAddressList_inner.add(l.getIntAddress());
                tempConsistAddressSizeList_inner.add(l.getIntAddressLength());
//                String addr = (l.getIntAddressLength() == 0 ? "S" : "L") + l.getIntAddress().toString();
                String addr = locoAddressToString(l.getIntAddress(), l.getIntAddressLength(), true);
                tempConsistDirectionList_inner.add((consist.isBackward(addr) ? 1 : 0));
            }

            // check if we already have it
            for (int i = 0; i < consistEngineAddressList.size(); i++) {
                if (consistEngineAddressList.get(i).size() == tempConsistEngineAddressList_inner.size()) {  // if the lists are different sizes don't bother
                    boolean isSame = true;
                    for (int j = 0; j < consistEngineAddressList.get(i).size() && isSame; j++) {
                        if ((!consistEngineAddressList.get(i).get(j).equals(tempConsistEngineAddressList_inner.get(j)))
                                || (!consistDirectionList.get(i).get(j).equals(tempConsistDirectionList_inner.get(j)))) {
                            isSame = false;
                        }
                    }
                    if (isSame) {
                        haveConsist = true;
                    }
                }
            }

            // check to see if we are still building the consist
            if ( (consistEngineAddressList.size()>0)
                    && (consistEngineAddressList.get(0).size() == (tempConsistEngineAddressList_inner.size()-1) ) ) {
                // check of the last added one is the same other then the last extra loco
                boolean isBuilding = true;
                for (int j = 0; j < consistEngineAddressList.get(0).size(); j++) {
                    if ((!consistEngineAddressList.get(0).get(j).equals(tempConsistEngineAddressList_inner.get(j)))
                            || (!consistDirectionList.get(0).get(j).equals(tempConsistDirectionList_inner.get(j)))) {
                        isBuilding = false;
                    }
                }
                if (isBuilding) {  // remove the first entry
                    consistEngineAddressList.remove(0);
                    consistAddressSizeList.remove(0);
                    consistDirectionList.remove(0);
                    consistNameList.remove(0);
                    haveConsist = false;
                }
            }

        }

        if (!haveConsist) {  // we don't have the consist already, or are removing or forcing a rewrite

            if (!removingConsistOrForceRewite) {
                consistEngineAddressList.add(0, tempConsistEngineAddressList_inner);
                consistAddressSizeList.add(0, tempConsistAddressSizeList_inner);
                consistDirectionList.add(0, tempConsistDirectionList_inner);
                String consistName = consist.toString();
                consistNameList.add(0, consistName);
            }

            // Save the consist list to the recent_consist_list.txt file
            File sdcard_path = Environment.getExternalStorageDirectory();
            File connections_list_file = new File(sdcard_path,
                    "engine_driver/recent_consist_list.txt");
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
                    mrl--;

                    for (int i = 0; i < consistEngineAddressList.size() && mrl > 0; i++) {
                        list_output.format("%d:%d%d", consistEngineAddressList.get(i).get(0), consistAddressSizeList.get(i).get(0), consistDirectionList.get(i).get(0));
                        for (int j = 1; j < consistAddressSizeList.get(i).size(); j++) {
                            list_output.format(",%d:%d%d", consistEngineAddressList.get(i).get(j), consistAddressSizeList.get(i).get(j), consistDirectionList.get(i).get(j));
                        }
                        list_output.format("~%s",consistNameList.get(i));
                        list_output.format("\n");
                        mrl--;
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
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            if(recentsSwipeDetector.swipeDetected()) {
                if(recentsSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearRecentListItem(v, position, id);
//                } else {
                }
            } else {  //no swipe
                engine_address = recent_loco_address_list.get(position);
                address_size = recent_loco_address_size_list.get(position);
                acquire_engine(true);
            }
        }
    }

    public class consist_item implements AdapterView.OnItemClickListener {
        // When an item is clicked, acquire that consist.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            Integer addr;
            String sAddr;
            Integer size;
            Integer dir;
            Consist consist = mainapp.consists[whichThrottle];

            if(recentConsistsSwipeDetector.swipeDetected()) {
                if(recentConsistsSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearRecentConsistsListItem(v, position, id);
//                } else {
                }
            } else {  //no swipe

                for (int i = 0; i < consistEngineAddressList.get(position).size(); i++) {
                    addr = consistEngineAddressList.get(position).get(i);
                    size = consistAddressSizeList.get(position).get(i);
                    dir = consistDirectionList.get(position).get(i);

//                    sAddr = (size==0 ? "S":"L") + addr.toString();  // convert 0/1 to S/L
                    sAddr = locoAddressToString(addr, size, true);  // convert 0/1 to S/L + addr
                    consist.add(sAddr);
                    Log.d("Engine_Driver", "select_loco Acquiring Consist. loco: " + locoAddressToString(addr, size, false) + (dir==0 ? "↑":"↓"));

    //                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, sAddr, whichThrottle);
                    mainapp.sendMsgDelay(mainapp.comm_msg_handler, i*100, message_type.REQ_LOCO_ADDR, sAddr, whichThrottle,0);

                    if (dir==1) {
                        consist = mainapp.consists[whichThrottle];
                        consist.setBackward(sAddr, true);
                    }
                }

                result = RESULT_LOCO_EDIT;
                end_this_activity();

            }
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

    public class clear_consists_list_button implements AdapterView.OnClickListener {
        public void onClick(View v) {
            clearConsistsListCount++;
            if (clearConsistsListCount <= 1) {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastSelectLocoConfirmClear), Toast.LENGTH_LONG).show();
            } else { // only clear the list if the button is clicked a second time
                clearConsistsList();
                clearConsistsListCount = 0;
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
            String[] ras = threaded_application.splitByString(rosterAddressString, "(");
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

        // Set up a list adapter to allow adding the list of recent engines to the UI.
        recent_engine_list = new ArrayList<>();
        recent_list_adapter = new RecentSimpleAdapter(this, recent_engine_list,
                R.layout.engine_list_item, new String[]{"engine"},
                new int[]{R.id.engine_item_label, R.id.engine_icon_image});
        engine_list_view = findViewById(R.id.engine_list);
        engine_list_view.setAdapter(recent_list_adapter);
        engine_list_view.setOnTouchListener(recentsSwipeDetector = new SwipeDetector());
        engine_list_view.setOnItemClickListener(new engine_item());
        engine_list_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongRecentListItemClick(v, pos, id);
            }
        });
        loadRecentsList(false);

        // Set up a list adapter to allow adding the list of recent consists to the UI.
        recent_consists_list = new ArrayList<>();
        recent_consists_list_adapter = new RecentConsistsSimpleAdapter(this, recent_consists_list,
                R.layout.consists_list_item, new String[]{"consist"},
                new int[]{R.id.consist_item_label});
//                new int[]{R.id.engine_item_label, R.id.engine_icon_image});
        consists_list_view = findViewById(R.id.consists_list);
        consists_list_view.setAdapter(recent_consists_list_adapter);
        consists_list_view.setOnTouchListener(recentConsistsSwipeDetector = new SwipeDetector());
        consists_list_view.setOnItemClickListener(new consist_item());
        consists_list_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongRecentConsistsListItemClick(v, pos, id);
            }
        });
        loadRecentConsistsList(false);



        // Set the button callbacks.
        Button button = findViewById(R.id.acquire);
        button_listener click_listener = new button_listener();
        button.setOnClickListener(click_listener);

        //Jeffrey added 7/3/2013
        button = findViewById(R.id.clear_Loco_List_button);
        button.setOnClickListener(new clear_Loco_List_button());

        button = findViewById(R.id.clear_consists_list_button);
        button.setOnClickListener(new clear_consists_list_button());


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
//        rbRecent = findViewById(R.id.select_loco_method_recent_button);
        rbRecentConsists = findViewById(R.id.select_consists_method_recent_button);

        prefSelectLocoMethod = prefs.getString("prefSelectLocoMethod", WHICH_METHOD_FIRST);
        // if the recent lists are empty make sure the radio button will be pointing to something valid
        if ( ((recent_consists_list.size()==0) && (prefSelectLocoMethod.equals(WHICH_METHOD_CONSIST)))
           | ((recent_loco_address_list.size()==0) && (prefSelectLocoMethod.equals(WHICH_METHOD_RECENT))) ) {
            prefSelectLocoMethod = WHICH_METHOD_ADDRESS;
        }

        rlAddress = findViewById(R.id.enter_loco_group);
        rlAddressHelp = findViewById(R.id.enter_loco_group_help);
        rlRosterHeader = findViewById(R.id.roster_list_header_group);
        rlRosterEmpty = findViewById(R.id.roster_list_empty_group);
        llRoster = findViewById(R.id.roster_list_group);
        rlRecentHeader = findViewById(R.id.engine_list_header_group);
        llRecent = findViewById(R.id.engine_list_wrapper);
        rlRecentConsistsHeader = findViewById(R.id.consists_list_header_group);
        llRecentConsists = findViewById(R.id.consists_list_wrapper);
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
                    case R.id.select_consists_method_recent_button:
                        showMethod(WHICH_METHOD_CONSIST);
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
                rlRecentConsistsHeader.setVisibility(View.GONE);
                llRecentConsists.setVisibility(View.GONE);

                rbAddress.setChecked(true);
                rbRoster.setChecked(false);
                rbRecent.setChecked(false);
                rbRecentConsists.setChecked(false);
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
                rlRecentConsistsHeader.setVisibility(View.GONE);
                llRecentConsists.setVisibility(View.GONE);

                rbAddress.setChecked(false);
                rbRoster.setChecked(true);
                rbRecent.setChecked(false);
                rbRecentConsists.setChecked(false);
                if (!rosterHelp) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRosterHelp), Toast.LENGTH_SHORT).show();
                    rosterHelp = true;
                }                break;
            }
            case WHICH_METHOD_RECENT: {
                rlAddress.setVisibility(View.GONE);
                rlAddressHelp.setVisibility(View.GONE);
                rlRosterHeader.setVisibility(View.GONE);
                llRoster.setVisibility(View.GONE);
                rlRosterEmpty.setVisibility(View.GONE);
                rlRecentHeader.setVisibility(View.VISIBLE);
                llRecent.setVisibility(View.VISIBLE);
                rlRecentConsistsHeader.setVisibility(View.GONE);
                llRecentConsists.setVisibility(View.GONE);

                rbAddress.setChecked(false);
                rbRoster.setChecked(false);
                rbRecent.setChecked(true);
                rbRecentConsists.setChecked(false);
                if (!recentsHelp) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRecentsHelp), Toast.LENGTH_SHORT).show();
                    recentsHelp = true;
                }
                break;
            }
            case WHICH_METHOD_CONSIST: {
                rlAddress.setVisibility(View.GONE);
                rlAddressHelp.setVisibility(View.GONE);
                rlRosterHeader.setVisibility(View.GONE);
                llRoster.setVisibility(View.GONE);
                rlRosterEmpty.setVisibility(View.GONE);
                rlRecentHeader.setVisibility(View.GONE);
                llRecent.setVisibility(View.GONE);
                rlRecentConsistsHeader.setVisibility(View.VISIBLE);
                llRecentConsists.setVisibility(View.VISIBLE);

                rbAddress.setChecked(false);
                rbRoster.setChecked(false);
                rbRecent.setChecked(false);
                rbRecentConsists.setChecked(true);
                if (!recentsConsistsHelp) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRecentConsistsHelp), Toast.LENGTH_SHORT).show();
                    recentsConsistsHelp = true;
                }
                break;
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

    public void clearConsistsList() {
        File sdcard_path = Environment.getExternalStorageDirectory();
        File consists_list_file = new File(sdcard_path + "/engine_driver/recent_consist_list.txt");

        if (consists_list_file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            consists_list_file.delete();
            recent_consists_list.clear();
            consistEngineAddressList.clear();
            consistAddressSizeList.clear();
            consistDirectionList.clear();
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

    // long click for the recent loco list items.
    protected boolean onLongRecentListItemClick(View v, int position, long id) {
//        clearRecentListItem(v, position, id);
        showEditRecentsNameDialog(position);
        return true;
}

    //  Clears the entry from the list
    protected boolean clearRecentListItem(View v, int position, long id) {
        recent_engine_list.remove(position);

        recent_loco_address_list.remove(position);
        recent_loco_address_size_list.remove(position);
        recent_loco_name_list.remove(position);

        removingLocoOrForceReload = true;
        updateRecentEngines(true);
        engine_list_view.invalidateViews();
        return true;
    }

    // long click for the recent consists list items.  Clears the entry from the list
    protected boolean onLongRecentConsistsListItemClick(View v, int position, long id) {
//        clearRecentConsistsListItem(v, position, id);
        showEditRecentConsistsNameDialog(position);
        return true;
    }

    // Clears the entry from the list
    protected boolean clearRecentConsistsListItem(View v, int position, long id) {
        recent_consists_list.remove(position);

        consistEngineAddressList.remove(position);
        consistAddressSizeList.remove(position);
        consistDirectionList.remove(position);
        consistNameList.remove(position);

        removingConsistOrForceRewite = true;

        updateRecentConsists(true);
        consists_list_view.invalidateViews();
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

    public class RecentConsistsSimpleAdapter extends SimpleAdapter {
        private Context cont;

        public RecentConsistsSimpleAdapter(Context context,
                                   List<? extends Map<String, ?>> data, int resource,
                                   String[] from, int[] to) {
            super(context, data, resource, from, to);
            cont = context;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            if (position > recent_consists_list.size())
                return convertView;

            HashMap<String, String> hm = recent_consists_list.get(position);
            if (hm == null)
                return convertView;

            LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.consists_list_item, null, false);

            String str = hm.get("consist_name");
            if (str != null) {
                TextView name = view.findViewById(R.id.consist_name_label);
                name.setText(Html.fromHtml(str));
            }

            str = hm.get("consist");
            if (str != null) {
                TextView secondLine = view.findViewById(R.id.consist_item_label);
                secondLine.setText(str);
            }


            return view;
        }

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public void showEditRecentConsistsNameDialog(final int pos) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.edit_recent_name, null);
        dialogBuilder.setView(dialogView);

        final EditText edt = (EditText) dialogView.findViewById(R.id.editRecentName);
        edt.setText(consistNameList.get(pos));

        dialogBuilder.setTitle(getApplicationContext().getResources().getString(R.string.RecentConsistsNameEditTitle));
        dialogBuilder.setMessage(getApplicationContext().getResources().getString(R.string.RecentConsistsNameEditText));
        dialogBuilder.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String rslt = edt.getText().toString();
                if (rslt.length()>0) {
                    consistNameList.set(pos, rslt);
                    removingConsistOrForceRewite = true;
                    updateRecentConsists(true);
                    loadRecentConsistsList(true);
                    consists_list_view.invalidateViews();
                }
            }
        });
        dialogBuilder.setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    public void showEditRecentsNameDialog(final int pos) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.edit_recent_name, null);
        dialogBuilder.setView(dialogView);

        final EditText edt = (EditText) dialogView.findViewById(R.id.editRecentName);
        edt.setText(recent_loco_name_list.get(pos));

        dialogBuilder.setTitle(getApplicationContext().getResources().getString(R.string.RecentsNameEditTitle));
        dialogBuilder.setMessage(getApplicationContext().getResources().getString(R.string.RecentsNameEditText));
        dialogBuilder.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String rslt = edt.getText().toString();
                if (rslt.length()>0) {
                    recent_loco_name_list.set(pos, rslt);
                    removingLocoOrForceReload = true;
                    updateRecentEngines(true);
                    loadRecentsList(true);
                    engine_list_view.invalidateViews();
                }
            }
        });
        dialogBuilder.setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    String locoAddressToString(Integer addr, int size, boolean sizeAsPrefix) {
        String engineAddressString = "";
        try {
            String addressLengthString = ((size == 0) ? "S" : "L");  //show L or S based on length from file
            if (!sizeAsPrefix) {
                engineAddressString = String.format("%s(%s)", addr.toString(), addressLengthString);  //e.g.  1009(L)
            } else {
                engineAddressString = addressLengthString + addr.toString();  //e.g.  L1009
            }
        } catch (Exception e) {
            Log.e("Engine_Driver", "locoAddressToString. ");
        }
        return engineAddressString;
    }

}
