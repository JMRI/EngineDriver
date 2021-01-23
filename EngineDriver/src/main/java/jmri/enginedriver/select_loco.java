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
import android.os.Looper;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmri.enginedriver.Consist.ConLoco;
import jmri.enginedriver.util.SwipeDetector;
import jmri.jmrit.roster.RosterEntry;

//import java.util.Arrays;
//import java.util.Collection;

public class select_loco extends Activity {
    static public final int RESULT_LOCO_EDIT = RESULT_FIRST_USER;

    private static final String WHICH_METHOD_FIRST = "0"; // first time the app has been used
    private static final String WHICH_METHOD_ADDRESS = "1";
    private static final String WHICH_METHOD_ROSTER = "2";
    private static final String WHICH_METHOD_RECENT = "3";
    private static final String WHICH_METHOD_CONSIST = "4";

    ArrayList<HashMap<String, String>> recent_engine_list;
    ArrayList<HashMap<String, String>> roster_list;
    private RosterSimpleAdapter roster_list_adapter;
    private RecentSimpleAdapter recent_list_adapter;

    private static final int WHICH_SOURCE_UNKNOWN = 0;
    private static final int WHICH_SOURCE_ADDRESS = 1;
    private static final int WHICH_SOURCE_ROSTER = 2;

    private static final int DIRECTION_FORWARD = 0;
    private static final int DIRECTION_BACKWARD = 1;

    private static final int LIGHT_OFF = 0;
    private static final int LIGHT_FOLLOW = 1;
    private static final int LIGHT_UNKNOWN = 2;

    // recent consists
    ArrayList<HashMap<String, String>> recent_consists_list;
    private RecentConsistsSimpleAdapter recent_consists_list_adapter;

    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    ListView consists_list_view;
    SwipeDetector recentConsistsSwipeDetector;
    SwipeDetector recentsSwipeDetector;

    private int engine_address;
    private int address_size;
    private String locoName = "";
    private int locoSource = 0;

    private String sWhichThrottle = "0";  // "0" or "1" or "2" + roster name
    int whichThrottle = 0;
    private int result;
    protected boolean selectLocoRendered = false;         // this will be true once set_labels() runs following rendering of the loco select textViews

    protected threaded_application mainapp; // hold pointer to mainapp

    private SharedPreferences prefs;
    private String default_address_length;
    private Menu SMenu;

    protected int layoutViewId = R.layout.select_loco;

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

    String overrideThrottleName;

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
                                    Log.d("Engine_Driver", "select_loco: xml roster entry " + rostername + " found, but no icon specified.");
                                }
                            } else {
                                Log.w("Engine_Driver", "select_loco: WiThrottle roster entry " + rostername + " not found in xml roster.");
                            }
                        } else {
                            Log.w("Engine_Driver", "select_loco: xml roster not available");
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
                    String s0 = arg0.get("roster_name").replaceAll("_"," ").toLowerCase();
                    String s1 = arg1.get("roster_name").replaceAll("_"," ").toLowerCase();
                    return s0.compareTo(s1);
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

    private String getLocoIconUrlFromRoster(String engineAddress, String engineName) {
        if (prefRosterRecentLocoNames) {
            if ((mainapp.roster_entries != null) && (mainapp.roster_entries.size() > 0) && (mainapp.roster != null)) {
                for (String rostername : mainapp.roster_entries.keySet()) {  // loop thru roster entries,
                    if (engineName.equals("")) {
                        if (mainapp.roster_entries.get(rostername).equals(engineAddress)) {
                            RosterEntry rosterentry = mainapp.roster.get(rostername);
                            if (rosterentry == null) return "";
                            String iconPath = rosterentry.getIconPath();  //if found, return the icon url
                            if (iconPath == null) return "";
                            return iconPath;
                        }
                    } else { // if there is a name as well, confirm they match (for entries with the same address)
                        if (rostername.equals(engineName)) {
                            RosterEntry rosterentry = mainapp.roster.get(rostername);
                            if (rosterentry == null) return "";
                            String iconPath = rosterentry.getIconPath();  //if found, return the icon url
                            if (iconPath == null) return "";
                            return iconPath;
                        }

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
        LinearLayout llEditConsist = findViewById(R.id.LL_edit_consist);

        TextView tvSelectLocoHeading = findViewById(R.id.select_loco_heading);
        tvSelectLocoHeading.setText(this.getResources().getString(R.string.select_loco_heading).replace("%1$s", Integer.toString(mainapp.throttleCharToInt(sWhichThrottle.charAt(0)) + 1)));

        bR.setVisibility(View.VISIBLE);
        llThrottle.setVisibility(View.VISIBLE);
        llEditConsist.setVisibility(View.GONE);

        if ((mainapp.consists != null) && (mainapp.consists[whichThrottle].isActive())) {
            if (mainapp.consists[whichThrottle].size()>1) {
                llEditConsist.setVisibility(View.VISIBLE);
            }
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
            tvThrottleNameHeader.setVisibility(View.GONE);
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
//                    Log.d("Engine_Driver", "select_loco: select_loco_handler - ROSTER_UPDATE");
                    //refresh labels when any roster response is received
                    roster_list_adapter.notifyDataSetChanged();
                    set_labels();
                    break;
                case message_type.RESPONSE:
                    String response_str = msg.obj.toString();
//                    Log.d("Engine_Driver", "select_loco: select_loco_handler - RESPONSE - message: " + response_str);
                    if (response_str.length() >= 1) {
                        char com1 = response_str.charAt(0);
                        if (com1 == 'R') {                                  //refresh labels when any roster response is received
                            roster_list_adapter.notifyDataSetChanged();
                            set_labels();
                            break;
                        } else if (com1 == 'M' && response_str.length() >= 3) { // refresh Release buttons if loco is added or removed from a consist
                            char com2 = response_str.charAt(2);
                            if (com2 == '+' || com2 == '-')
                                set_labels();
                                break;
                        } else { // ignore everything else
//                            Log.d("Engine_Driver", "select_loco: select_loco_handler - RESPONSE - ignoring message: " + response_str);
                            break;
                        }
                    }
                    if (!selectLocoRendered)         // call set_labels if the select loco textViews had not rendered the last time it was called
                        set_labels();
                    break;
                case message_type.WIT_CON_RETRY:
//                    Log.d("Engine_Driver", "select_loco: select_loco_handler - WIT_CON_RETRY");
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
//                    Log.d("Engine_Driver", "select_loco: select_loco_handler - WIT_CON_RECONNECT");
                    roster_list_adapter.notifyDataSetChanged();
                    set_labels();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
//                    Log.d("Engine_Driver", "select_loco: select_loco_handler - DISCONNECT");
                    end_this_activity();
                    break;
            }
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
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

    void acquire_engine(boolean bUpdateList, int numberInConsist, int totalInConsist) { // if numberInConsist is greater than -1 it is not from the recent consists list
        String roster_name = "";
        String sAddr = importExportPreferences.locoAddressToString(engine_address, address_size, true);
        Loco l = new Loco(sAddr);
        if (locoSource!=WHICH_SOURCE_ADDRESS) {
            roster_name = sWhichThrottle.substring(1);
            l.setDesc(roster_name);       //use rosterName if present
            roster_name = mainapp.findLocoNameInRoster(roster_name);  // confirm that the loco is actually in the roster
            l.setRosterName(roster_name); //use rosterName if present
            l.setIsFromRoster(true);
        } else {
            l.setDesc(locoName);
            l.setRosterName(null); //make sure rosterName is null
            l.setFunctionLabelDefaults(mainapp, whichThrottle);
        }
        if(mainapp.consists==null || mainapp.consists[whichThrottle]==null) {
            if(mainapp.consists==null)
                Log.d("Engine_Driver", "acquireEngine consists is null");
            else if(mainapp.consists[whichThrottle]==null)
                Log.d("Engine_Driver", "acquireEngine consists["+whichThrottle+"] is null");
            end_this_activity();
            return;
        }

        Consist consist = mainapp.consists[whichThrottle];

        // if we already have it do nothing
        if (consist.size()>=1) {
            for (int i = 0; i<=consist.size();i++) {
                if (consist.getLoco(sAddr)!=null) {
                    overrideThrottleName = "";
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastLocoAlreadySelected), Toast.LENGTH_LONG).show();
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, sAddr, whichThrottle);  // send the acquire message anyway
                    return;
                }
            }
        }

            if (!roster_name.equals("")) {// add roster selection info if present
            sAddr += "<;>" + roster_name;
        }

        // user preference set to not consist, or consisting not supported in this JMRI, so drop before adding
        // ignore the preference if a recent consist was selected
        if (prefs.getBoolean("drop_on_acquire_preference", false) && numberInConsist<0) {
            ConLoco cl = consist.getLoco(sAddr);
            if (cl == null) { // if the newly selected loco is different/not in the consist, release everything
                release_loco(whichThrottle);
            } else { // already have it so don't do anything
                result = RESULT_OK;
                end_this_activity();
                return;
            }
        }
        Log.d("Engine_Driver", "select_loco: acquire_engine: sAddr:'" + sAddr +"'");

        if ( (!consist.isActive()) && (numberInConsist<1) ) {               // if this is the only loco in consist then just tell WiT and exit
            consist.add(l);
            consist.setWhichSource(importExportPreferences.locoAddressToString(engine_address, address_size, true), locoSource);
            consist.setLeadAddr(l.getAddress());
            consist.setTrailAddr(l.getAddress());
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, sAddr, whichThrottle);
            if(numberInConsist<0) { // don't save the recents if a recent consist was selected
                saveRecentLocosList(bUpdateList);
            }
            result = RESULT_OK;
            end_this_activity();

        } else {                                // else consist exists so bring up editor
            ConLoco cl = consist.getLoco(sAddr);
            newEngine = (cl == null);
            if (newEngine || !cl.isConfirmed()) {        // if engine is not already in the consist, or if it is but never got acquired
                consist.add(l);
                consist.setWhichSource(importExportPreferences.locoAddressToString(engine_address, address_size, true), locoSource);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type. REQ_LOCO_ADDR, sAddr, whichThrottle);

                saveUpdateList = bUpdateList;
                Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);
                consistEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));

                consist.setTrailAddr(l.getAddress());  // set the newly added loco as the trailing loco

                if (numberInConsist<0) { // don't show the Consist edit screen.  Only used for Recent Consists
                    startActivityForResult(consistEdit, throttle.ACTIVITY_CONSIST);
                    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                }
            }
        }
    }

    //handle return from ConsistEdit
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == throttle.ACTIVITY_CONSIST) {                          // edit consist
            if (newEngine) {
                saveRecentLocosList(saveUpdateList);
                updateRecentConsists(saveUpdateList);
            }
            result = RESULT_LOCO_EDIT;                 //tell Throttle to update loco directions
        }
        overrideThrottleName="";
        end_this_activity();
    }

    //write the recent locos to a file
    void saveRecentLocosList(boolean bUpdateList) {

        //if not updating list or no SD Card present then nothing else to do
        if (!bUpdateList || !android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;


        if (!removingLocoOrForceReload) {

            // check if it already in the list and remove it
            for (int i = 0; i < importExportPreferences.recent_loco_address_list.size(); i++) {
                if (engine_address == importExportPreferences.recent_loco_address_list.get(i)
                        && address_size == importExportPreferences.recent_loco_address_size_list.get(i)
                        && locoName.equals(importExportPreferences.recent_loco_name_list.get(i))) {
                    //noinspection SuspiciousListRemoveInLoop
                    importExportPreferences.recent_loco_address_list.remove(i);
                    //noinspection SuspiciousListRemoveInLoop
                    importExportPreferences.recent_loco_address_size_list.remove(i);
                    //noinspection SuspiciousListRemoveInLoop
                    importExportPreferences.recent_loco_name_list.remove(i);
                    //noinspection SuspiciousListRemoveInLoop
                    importExportPreferences.recent_loco_source_list.remove(i);
                }
            }

            // now append it to the beginning of the list
            importExportPreferences.recent_loco_address_list.add(0, engine_address);
            importExportPreferences.recent_loco_address_size_list.add(0, address_size);
            importExportPreferences.recent_loco_name_list.add(0, locoName);
            importExportPreferences.recent_loco_source_list.add(0, locoSource);


        }

        importExportPreferences.writeRecentLocosListToFile(prefs);
    }


    private void loadRecentLocosList(boolean reload) {

        importExportPreferences.recent_loco_address_list = new ArrayList<>();
        importExportPreferences.recent_loco_address_size_list = new ArrayList<>();
        importExportPreferences.recent_loco_name_list = new ArrayList<>();
        importExportPreferences.recent_loco_source_list = new ArrayList<>();
        if (reload) {
            recent_engine_list = new ArrayList<>();
        }


        rbRecent = findViewById(R.id.select_loco_method_recent_button);

        //if no SD Card present then there is no recent consists list
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent locos list requires SD Card
            TextView v = findViewById(R.id.recent_engines_heading);
            v.setText(getString(R.string.sl_recent_engine_notice));
            rbRecent.setVisibility(View.GONE); // if the list is empty, hide the radio button
        } else {

            importExportPreferences.getRecentLocosListFromFile();

            for (int i = 0; i < importExportPreferences.recent_loco_address_list.size(); i++) {
                HashMap<String, String> hm = new HashMap<>();
                String engineAddressString = importExportPreferences.locoAddressToString(
                        importExportPreferences.recent_loco_address_list.get(i),
                        importExportPreferences.recent_loco_address_size_list.get(i), false);
                String engineIconUrl = getLocoIconUrlFromRoster(engineAddressString,importExportPreferences.recent_loco_name_list.get(i));

                hm.put("engine_icon", engineIconUrl);
                hm.put("engine", importExportPreferences.recent_loco_name_list.get(i)); // the larger loco name text
                hm.put("engine_name", importExportPreferences.locoAddressToHtml(
                        importExportPreferences.recent_loco_address_list.get(i),
                        importExportPreferences.recent_loco_address_size_list.get(i),
                        importExportPreferences.recent_loco_source_list.get(i)));   // the small loco address field at the top of the row
                recent_engine_list.add(hm);
            }

            if (importExportPreferences.recent_loco_address_list.size()==0) {  // if the list is empty, hide the radio button
                rbRecent.setVisibility(View.GONE);
            } else {
                rbRecent.setVisibility(View.VISIBLE);
            }

        }
        recent_list_adapter.notifyDataSetChanged();
    }


    private void loadRecentConsistsList(boolean reload) {
        recent_consists_list_adapter.notifyDataSetChanged();
        RadioButton myRadioButton = findViewById(R.id.select_consists_method_recent_button);

        if (reload) {
            recent_consists_list = new ArrayList<>();
        }

        //if no SD Card present then there is no recent consists list
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent locos list requires SD Card
            TextView v = findViewById(R.id.recent_consists_heading);
            v.setText(getString(R.string.sl_recent_engine_notice));
            myRadioButton.setVisibility(View.GONE); // if the list is empty, hide the radio button
        } else {
            importExportPreferences.getRecentConsistsListFromFile();
            for (int i = 0; i < importExportPreferences.consistEngineAddressList.size(); i++) {
                HashMap<String, String> hm = new HashMap<>();
                hm.put("consist_name", mainapp.getRosterNameFromAddress(importExportPreferences.consistNameHtmlList.get(i),
                                false));
                hm.put("consist", mainapp.locoAndConsistNamesCleanupHtml(importExportPreferences.consistNameList.get(i)));
                recent_consists_list.add(hm);
            }
            if (importExportPreferences.consistEngineAddressList.size()==0) {
                myRadioButton.setVisibility(View.GONE); // if the list is empty, hide the radio button
            } else {
                myRadioButton.setVisibility(View.VISIBLE);
            }

        }
    }


    void updateRecentConsists(boolean bUpdateList) {
        ArrayList<Integer> tempConsistEngineAddressList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistAddressSizeList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistDirectionList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistSourceList_inner = new ArrayList<>();
        ArrayList<String> tempConsistRosterNameList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistLightList_inner = new ArrayList<>();

        //if not updating list or no SD Card present then nothing else to do
        if (!bUpdateList || !android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;

        Consist consist = mainapp.consists[whichThrottle];
        Collection<ConLoco> conLocos = consist.getLocos();
        StringBuilder oneConsistHtml = new StringBuilder();

        int whichEntryIsBeingUpdated = -1;
        boolean isBuilding = true;

        if (!removingConsistOrForceRewite) {

            int k = -1;
            for (ConLoco l : conLocos) {
                k++;
                tempConsistEngineAddressList_inner.add(l.getIntAddress());
                tempConsistAddressSizeList_inner.add(l.getIntAddressLength());
                String addr = importExportPreferences.locoAddressToString(l.getIntAddress(), l.getIntAddressLength(), true);
                tempConsistDirectionList_inner.add((consist.isBackward(addr) ? DIRECTION_BACKWARD : DIRECTION_FORWARD));
                String rosterName = "";
                if (l.getRosterName() != null) {
                    rosterName = l.getRosterName();
                }
                tempConsistSourceList_inner.add(l.getWhichSource());
                tempConsistRosterNameList_inner.add(rosterName);
                tempConsistLightList_inner.add(k==0 ? LIGHT_FOLLOW : consist.isLight(addr));   // always set the first loco as 'follow'

                int lastItem = tempConsistEngineAddressList_inner.size()-1;
                oneConsistHtml.append(importExportPreferences.addOneConsistAddressHtml(
                        tempConsistEngineAddressList_inner.get(lastItem),
                        tempConsistAddressSizeList_inner.get(lastItem),
                        tempConsistDirectionList_inner.get(lastItem),
                        tempConsistSourceList_inner.get(lastItem),
                        tempConsistLightList_inner.get(lastItem)));
            }

            // check if we already have it
            for (int i = 0; i < importExportPreferences.consistEngineAddressList.size(); i++) {
                if (importExportPreferences.consistEngineAddressList.get(i).size() == tempConsistEngineAddressList_inner.size()) {  // if the lists are different sizes don't bother
                    boolean isSame = true;
                    for (int j = 0; j < importExportPreferences.consistEngineAddressList.get(i).size() && isSame; j++) {
                        if ((!importExportPreferences.consistEngineAddressList.get(i).get(j).equals(tempConsistEngineAddressList_inner.get(j)))
                        ) {
                            isSame = false;
                        }
                    }
                    if (isSame) {
                        whichEntryIsBeingUpdated = i + 1; //remember this, so we can remove this line in the list.  Add 1 because we are going to force a new line at the top
                    }
                }
            }

            // check to see if we are still building the consist
            if ( (importExportPreferences.consistEngineAddressList.size()>0)
                    && (importExportPreferences.consistEngineAddressList.get(0).size() == (tempConsistEngineAddressList_inner.size()-1) ) ) {
                // check of the last added one is the same other then the last extra loco
                for (int j = 0; j < tempConsistEngineAddressList_inner.size()-1; j++) {
                    if ((!importExportPreferences.consistEngineAddressList.get(0).get(j).equals(tempConsistEngineAddressList_inner.get(j)))) {
                        isBuilding = false;
                    }
                }
                if (isBuilding) {  // remove the first entry
                    importExportPreferences.consistEngineAddressList.remove(0);
                    importExportPreferences.consistAddressSizeList.remove(0);
                    importExportPreferences.consistDirectionList.remove(0);
                    importExportPreferences.consistSourceList.remove(0);
                    importExportPreferences.consistRosterNameList.remove(0);
                    importExportPreferences.consistLightList.remove(0);
                    importExportPreferences.consistNameList.remove(0);
                    importExportPreferences.consistNameHtmlList.remove(0);
                    whichEntryIsBeingUpdated = -1;
                }
            }

            // now add it
            importExportPreferences.consistEngineAddressList.add(0, tempConsistEngineAddressList_inner);
            importExportPreferences.consistAddressSizeList.add(0, tempConsistAddressSizeList_inner);
            importExportPreferences.consistDirectionList.add(0, tempConsistDirectionList_inner);
            importExportPreferences.consistSourceList.add(0, tempConsistSourceList_inner);
            importExportPreferences.consistRosterNameList.add(0, tempConsistRosterNameList_inner);
            importExportPreferences.consistLightList.add(0, tempConsistLightList_inner);
            String consistName = consist.toString();
            if (whichEntryIsBeingUpdated>0) { //this may already have a custom name
                consistName = importExportPreferences.consistNameList.get(whichEntryIsBeingUpdated - 1);
            }
            importExportPreferences.consistNameList.add(0, consistName);
            importExportPreferences.consistNameHtmlList.add(0, oneConsistHtml.toString());

        }
        importExportPreferences.writeRecentConsistsListToFile(prefs, whichEntryIsBeingUpdated);
    }


    // listener for the Acquire button when entering a DCC Address
    public class button_listener implements View.OnClickListener {
        public void onClick(View v) {
            EditText entry = findViewById(R.id.loco_address);
            try {
                engine_address = Integer.parseInt(entry.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(getApplicationContext(), "ERROR - Please enter a valid DCC address.\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            Spinner spinner = findViewById(R.id.address_length);
            address_size = spinner.getSelectedItemPosition();
            locoName = importExportPreferences.locoAddressToString(engine_address, address_size, false);
            sWhichThrottle += locoName;
            locoSource = WHICH_SOURCE_ADDRESS;

            acquire_engine(true, -1, -1);
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close

        }
    }

    public class release_button_listener implements View.OnClickListener {
        int _throttle;

        release_button_listener(int throttle) {
            _throttle = throttle;
        }

        public void onClick(View v) {
            release_loco(_throttle);
            overrideThrottleName="";
            end_this_activity();
        }
    }

    public class edit_consist_button_listener implements View.OnClickListener {
        int _throttle;
        Activity _selectLocoActivity;

        edit_consist_button_listener(int throttle, Activity selectLocoActivity) {
            _throttle = throttle;
            _selectLocoActivity = selectLocoActivity;
        }

        public void onClick(View v) {
            Intent consistEdit = new Intent().setClass(_selectLocoActivity, ConsistEdit.class);
            consistEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));
            consistEdit.putExtra("saveConsistsFile", 'Y');

            startActivityForResult(consistEdit, throttle.ACTIVITY_CONSIST);
            connection_activity.overridePendingTransition(_selectLocoActivity, R.anim.fade_in, R.anim.fade_out);

        }
    }

     public class edit_consist_lights_button_listener implements View.OnClickListener {
        int _throttle;
        Activity _selectLocoActivity;

        edit_consist_lights_button_listener(int throttle, Activity selectLocoActivity) {
            _throttle = throttle;
            _selectLocoActivity = selectLocoActivity;
        }

        public void onClick(View v) {
            Intent consistLightsEdit = new Intent().setClass(_selectLocoActivity, ConsistLightsEdit.class);
            consistLightsEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));

            startActivityForResult(consistLightsEdit, throttle.ACTIVITY_CONSIST_LIGHTS);
            connection_activity.overridePendingTransition(_selectLocoActivity, R.anim.fade_in, R.anim.fade_out);

        }
    }

    // onClick for the Recent Locos list items
    public class engine_item implements AdapterView.OnItemClickListener {
        // When an item is clicked, acquire that engine.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            if(recentsSwipeDetector.swipeDetected()) {
                if(recentsSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearRecentListItem(v, position, id);
                }
            } else {  //no swipe
                engine_address = importExportPreferences.recent_loco_address_list.get(position);
                address_size = importExportPreferences.recent_loco_address_size_list.get(position);
                locoSource = importExportPreferences.recent_loco_source_list.get(position);
                locoName = importExportPreferences.recent_loco_name_list.get(position);
                if (locoSource==WHICH_SOURCE_UNKNOWN ) {
                    locoName = mainapp.getRosterNameFromAddress(importExportPreferences.locoAddressToString(engine_address, address_size, false),true);
                }

                sWhichThrottle += locoName;
                acquire_engine(true, -1, -1);
            }
        }
    }

    // onClick for the Recent Consists list items
    public class consist_item implements AdapterView.OnItemClickListener {
        // When an item is clicked, acquire that consist.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            String sAddr;
            int dir;
            int light;

            String tempsWhichThrottle = sWhichThrottle;

            if(recentConsistsSwipeDetector.swipeDetected()) {
                if(recentConsistsSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearRecentConsistsListItem(v, position, id);
                }
            } else {  //no swipe
                overrideThrottleName = importExportPreferences.consistNameList.get(position);

                for (int i = 0; i < importExportPreferences.consistEngineAddressList.get(position).size(); i++) {

                    engine_address = importExportPreferences.consistEngineAddressList.get(position).get(i);
                    address_size = importExportPreferences.consistAddressSizeList.get(position).get(i);
                    sAddr = importExportPreferences.locoAddressToString(engine_address, address_size, true);
                    locoSource = importExportPreferences.consistSourceList.get(position).get(i);
                    locoName = mainapp.getRosterNameFromAddress(importExportPreferences.locoAddressToString(engine_address, address_size, false), false);
                    if ( (locoSource!=WHICH_SOURCE_ADDRESS) && (!importExportPreferences.consistRosterNameList.get(position).get(i).equals("")) ) {
                            locoName = importExportPreferences.consistRosterNameList.get(position).get(i);
                    }
                    sWhichThrottle = tempsWhichThrottle
                            + locoName;

                    acquire_engine(true, i, importExportPreferences.consistEngineAddressList.get(position).size());

                    Consist consist = mainapp.consists[whichThrottle];

                    dir = importExportPreferences.consistDirectionList.get(position).get(i);
                    if (dir==DIRECTION_BACKWARD) {
                        consist.setBackward(sAddr, true);
                    }

                    light = importExportPreferences.consistLightList.get(position).get(i);
                    if (light!=LIGHT_UNKNOWN) {
                        consist.setLight(sAddr,light);
                    }

                }
                updateRecentConsists(saveUpdateList);

                result = RESULT_LOCO_EDIT;
                end_this_activity();

            }
        }
    }

    //Clears recent connection list of locos when button is touched or clicked
    public class clear_Loco_List_button implements AdapterView.OnClickListener {
        public void onClick(View v) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            clearList();
                            onCreate(null);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder ab = new AlertDialog.Builder(select_loco.this);
            ab.setTitle(getApplicationContext().getResources().getString(R.string.dialogConfirmClearTitle))
                    .setMessage(getApplicationContext().getResources().getString(R.string.dialogRecentLocoConfirmClearQuestion))
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.cancel, dialogClickListener);
            ab.show();

        }
    }

    public class clear_consists_list_button implements AdapterView.OnClickListener {
        public void onClick(View v) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            clearConsistsList();
                            onCreate(null);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder ab = new AlertDialog.Builder(select_loco.this);
            ab.setTitle(getApplicationContext().getResources().getString(R.string.dialogConfirmClearTitle))
                    .setMessage(getApplicationContext().getResources().getString(R.string.dialogRecentConsistsConfirmClearQuestions))
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.cancel, dialogClickListener);
            ab.show();

        }
    }

    // onClick Listener for the Roster list items
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
                    engine_address = Integer.parseInt(ras[0]);   // convert address to int
                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), "ERROR - could not parse address\n" + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return; //get out, don't try to acquire
                }
                if ("loco".equals(rosterEntryType)) {
                    locoName = rosterNameString;
                    sWhichThrottle += rosterNameString;     //append rostername if type is loco (not consist) 
                }
                locoSource = WHICH_SOURCE_ROSTER;

                boolean bRosterRecent = prefs.getBoolean("roster_recent_locos_preference",
                        getResources().getBoolean(R.bool.prefRosterRecentLocosDefaultValue));

                overrideThrottleName = rosterNameString;
                acquire_engine(bRosterRecent,  -1, -1);
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
            overrideThrottleName="";
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
        recent_list_adapter = new RecentSimpleAdapter(this, recent_engine_list, R.layout.engine_list_item,
                new String[]{"engine"},
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
        loadRecentLocosList(false);

        // Set up a list adapter to allow adding the list of recent consists to the UI.
        recent_consists_list = new ArrayList<>();
        recent_consists_list_adapter = new RecentConsistsSimpleAdapter(this, recent_consists_list,
                R.layout.consists_list_item, new String[]{"consist"},
                new int[]{R.id.consist_item_label});
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

        // consist edit button
        button = findViewById(R.id.Sl_edit_consist);
        button.setOnClickListener(new edit_consist_button_listener(whichThrottle, this));

        // consist lights edit button
        button = findViewById(R.id.Sl_edit_consist_lights);
        button.setOnClickListener(new edit_consist_lights_button_listener(whichThrottle, this));


        rbAddress = findViewById(R.id.select_loco_method_address_button);
        rbRoster = findViewById(R.id.select_loco_method_roster_button);
        rbRecentConsists = findViewById(R.id.select_consists_method_recent_button);

        prefSelectLocoMethod = prefs.getString("prefSelectLocoMethod", WHICH_METHOD_FIRST);
        // if the recent lists are empty make sure the radio button will be pointing to something valid
        if ( ((recent_consists_list.size()==0) && (prefSelectLocoMethod.equals(WHICH_METHOD_CONSIST)))
           | ((importExportPreferences.recent_loco_address_list.size()==0) && (prefSelectLocoMethod.equals(WHICH_METHOD_RECENT))) ) {
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
        overrideThrottleName = "";

        Handler handler = new Handler();
        handler.postDelayed(showMethodTask, 500);  // show or hide the soft keyboard after a short delay
    }

    private Runnable showMethodTask = new Runnable() {
        public void run() {
            showMethod(prefSelectLocoMethod);
        }
    };

    @SuppressLint("ApplySharedPref")
    private void showMethod(String whichMethod) {
        EditText la = findViewById(R.id.loco_address);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

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

                la.requestFocus();
                imm.showSoftInput(la, InputMethodManager.SHOW_IMPLICIT);

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
                if (!mainapp.shownToastRoster) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRosterHelp), Toast.LENGTH_LONG).show();
                    mainapp.shownToastRoster = true;
                }

                imm.hideSoftInputFromWindow(la.getWindowToken(), 0);
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
                rlRecentConsistsHeader.setVisibility(View.GONE);
                llRecentConsists.setVisibility(View.GONE);

                rbAddress.setChecked(false);
                rbRoster.setChecked(false);
                rbRecent.setChecked(true);
                rbRecentConsists.setChecked(false);
                if (!mainapp.shownToastRecentLocos) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRecentsHelp), Toast.LENGTH_LONG).show();
                    mainapp.shownToastRecentLocos = true;
                }

                imm.hideSoftInputFromWindow(la.getWindowToken(), 0);
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
                if (!mainapp.shownToastRecentConsists) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRecentConsistsHelp), Toast.LENGTH_LONG).show();
                    mainapp.shownToastRecentConsists = true;
                }

                imm.hideSoftInputFromWindow(la.getWindowToken(), 0);
                break;
            }
        }
        prefs.edit().putString("prefSelectLocoMethod", whichMethod).commit();
    }

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
            importExportPreferences.consistEngineAddressList.clear();
            importExportPreferences.consistAddressSizeList.clear();
            importExportPreferences.consistDirectionList.clear();
            importExportPreferences.consistSourceList.clear();
            importExportPreferences.consistRosterNameList.clear();
            importExportPreferences.consistLightList.clear();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        // checking address length here covers (future) case where prefs changed while paused
        default_address_length = prefs.getString("default_address_length", this
                .getResources().getString(R.string.prefDefaultAddressLengthDefaultValue));
        updateAddressEntry();   // enable/disable buttons
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

   @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "select_loco.onDestroy() called");
        super.onDestroy();

        if (mainapp.select_loco_msg_handler !=null) {
           mainapp.select_loco_msg_handler.removeCallbacksAndMessages(null);
           mainapp.select_loco_msg_handler = null;
        } else {
           Log.d("Engine_Driver", "onDestroy: mainapp.select_loco_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // end current activity
    void end_this_activity() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("whichThrottle", sWhichThrottle.charAt(0));  //pass whichThrottle as an extra
        resultIntent.putExtra("overrideThrottleName", overrideThrottleName);
        setResult(result, resultIntent);
        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void updateAddressEntry() {
        Button ba = findViewById(R.id.acquire);
        EditText la = findViewById(R.id.loco_address);
        String txt = la.getText().toString().trim();
        int txtLen = txt.length();
        int addr = -1;
        if (txtLen>0) {
            try {
                addr = Integer.parseInt(txt);
            } catch (NumberFormatException e) {
                la.setText(""); //clear the bad entry
            }
        }

        // don't allow acquire button if nothing entered
        if (addr > -1) {
            ba.setEnabled(true);

            // set address length
            Spinner al = findViewById(R.id.address_length);
            if (default_address_length.equals("Long") ||
                    ( default_address_length.equals("Auto") && (addr > 127)) ) {
                al.setSelection(1);
            } else {
                al.setSelection(0);
            }

        } else {
            ba.setEnabled(false);
        }

        return;
    }

    // long click handler for the Roster List items.  Shows the details of the enter in a dialog.
    protected boolean onLongListItemClick(View v, int position, long id) {
        if (mainapp.roster == null) {
            Log.w("Engine_Driver", "No roster details found.");
            return true;
        }
        HashMap<String, String> hm = roster_list.get(position);
        String rosterNameString = hm.get("roster_name");
        if (mainapp.roster == null) {
            Log.w("Engine_Driver", "select_loco: Roster is null.");
            return true;
        }
        RosterEntry re = mainapp.roster.get(rosterNameString);
        if (re == null) {
            Log.w("Engine_Driver", "select_loco: Roster entry " + rosterNameString + " not available.");
            return true;
        }
        String iconURL = hm.get("roster_icon");

        showRosterDetailsDialog(re, rosterNameString, iconURL);

        return true;
    }

    protected void showRosterDetailsDialog(RosterEntry re, String rosterNameString, String iconURL) {
        Log.d("Engine_Driver", "select_loco: Showing details for roster entry " + rosterNameString);
        final Dialog dialog = new Dialog(select_loco.this, mainapp.getSelectedTheme());
        dialog.setTitle(getApplicationContext().getResources().getString(R.string.rosterDetailsDialogTitle) + rosterNameString);
        dialog.setContentView(R.layout.roster_entry);
        String res = re.toString();
        TextView tv = dialog.findViewById(R.id.rosterEntryText);
        tv.setText(res);

        ImageView imageView = dialog.findViewById(R.id.rosterEntryImage);
        if ((iconURL != null) && (iconURL.length() > 0)) {
            mainapp.imageDownloader.download(iconURL, imageView);
        } else {
            imageView.setVisibility(View.GONE);
        }

        Button buttonClose = dialog.findViewById(R.id.rosterEntryButtonClose);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(true);
        dialog.show();

    }

    // long click for the recent loco list items.
    protected boolean onLongRecentListItemClick(View v, int position, long id) {
        if (importExportPreferences.recent_loco_source_list.get(position)==WHICH_SOURCE_ROSTER) {
            String rosterEntryName = importExportPreferences.recent_loco_name_list.get(position);
            RosterEntry re = mainapp.roster.get(rosterEntryName);
            if (re == null) {
                Log.w("Engine_Driver", "Roster entry " + rosterEntryName + " not available.");
                return true;
            }
            showRosterDetailsDialog(re, rosterEntryName, "");
        } else {
            showEditRecentsNameDialog(position);
        }
        return true;
    }

    //  Clears the entry from the list
    protected boolean clearRecentListItem(View v, final int position, long id) {

        importExportPreferences.recent_loco_address_list.remove(position);
        importExportPreferences.recent_loco_address_size_list.remove(position);
        importExportPreferences.recent_loco_name_list.remove(position);
        importExportPreferences.recent_loco_source_list.remove(position);

        removingLocoOrForceReload = true;

        Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        anim.setDuration(500);
        View itemView = engine_list_view.getChildAt(position - engine_list_view.getFirstVisiblePosition());
        itemView.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    recent_engine_list.remove(position);
                    saveRecentLocosList(true);
                    engine_list_view.invalidateViews();
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRecentCleared), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}

                public void run() {}
        });

        return true;
    }

    // long click for the recent consists list items.  Clears the entry from the list
    protected boolean onLongRecentConsistsListItemClick(View v, int position, long id) {
        showEditRecentConsistsNameDialog(position);
        return true;
    }

    // Clears the entry from the list
    protected boolean clearRecentConsistsListItem(View v, final int position, long id) {
        View itemView = consists_list_view.getChildAt(position - consists_list_view.getFirstVisiblePosition());

        importExportPreferences.consistEngineAddressList.remove(position);
        importExportPreferences.consistAddressSizeList.remove(position);
        importExportPreferences.consistDirectionList.remove(position);
        importExportPreferences.consistSourceList.remove(position);
        importExportPreferences.consistRosterNameList.remove(position);
        importExportPreferences.consistLightList.remove(position);
        importExportPreferences.consistNameList.remove(position);

        removingConsistOrForceRewite = true;

        Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        anim.setDuration(500);
        itemView.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                recent_consists_list.remove(position);
                updateRecentConsists(true);
                consists_list_view.invalidateViews();
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRecentConsistCleared), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}

            public void run() {}
        });

        return true;
    }

public class RosterSimpleAdapter extends SimpleAdapter {
    private Context cont;

    RosterSimpleAdapter(Context context,
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

    RecentSimpleAdapter(Context context,
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
            name.setText(Html.fromHtml(str));
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

        RecentConsistsSimpleAdapter(Context context,
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
                secondLine.setText(Html.fromHtml(str));
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

        final EditText edt = dialogView.findViewById(R.id.editRecentName);
        edt.setText(importExportPreferences.consistNameList.get(pos));

        dialogBuilder.setTitle(getApplicationContext().getResources().getString(R.string.RecentConsistsNameEditTitle));
        dialogBuilder.setMessage(getApplicationContext().getResources().getString(R.string.RecentConsistsNameEditText));
        dialogBuilder.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String rslt = edt.getText().toString();
                if (rslt.length()>0) {
                    importExportPreferences.consistNameList.set(pos, rslt);
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

        final EditText edt = dialogView.findViewById(R.id.editRecentName);
        edt.setText(importExportPreferences.recent_loco_name_list.get(pos));

        dialogBuilder.setTitle(getApplicationContext().getResources().getString(R.string.RecentsNameEditTitle));
        dialogBuilder.setMessage(getApplicationContext().getResources().getString(R.string.RecentsNameEditText));
        dialogBuilder.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String rslt = edt.getText().toString();
                if (rslt.length()>0) {
                    importExportPreferences.recent_loco_name_list.set(pos, rslt);
                    removingLocoOrForceReload = true;
                    saveRecentLocosList(true);
                    loadRecentLocosList(true);
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


}
