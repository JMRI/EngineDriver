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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.Consist.ConLoco;
import jmri.enginedriver.type.Loco;
import jmri.enginedriver.type.light_follow_type;
import jmri.enginedriver.type.sort_type;
import jmri.enginedriver.type.source_type;
import jmri.enginedriver.util.SwipeDetector;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.address_type;

import jmri.jmrit.roster.RosterEntry;
import jmri.enginedriver.import_export.ImportExportPreferences;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.type.direction_type;
import jmri.enginedriver.type.sub_activity_type;

public class select_loco extends AppCompatActivity {
    static public final int RESULT_LOCO_EDIT = RESULT_FIRST_USER;


    interface which_method {
        String FIRST = "0"; // first time the app has been used
        String ADDRESS = "1";
        String ROSTER = "2";
        String RECENT_LOCOS = "3";
        String RECENT_CONSISTS = "4";
        String IDNGO = "5";
    }
    String prefSelectLocoMethod = which_method.FIRST;

    private static final String RECENT_LOCO_DIR = "recent_engine_list";

    ArrayList<HashMap<String, String>> recentEngineList;
    ArrayList<HashMap<String, String>> rosterList;
    ArrayList<String> rosterOwnersList;
    String rosterOwnersFilter = "Owner";
    int rosterOwnersFilterIndex = 0;
    private RosterSimpleAdapter rosterListAdapter;
    private RecentSimpleAdapter recentListAdapter;

    public static final int ACTIVITY_DEVICE_SOUNDS_SETTINGS = 5;
    public static final int ACTIVITY_SELECT_ROSTER_ENTRY_IMAGE = 6;

    // recent consists
    ArrayList<HashMap<String, String>> recent_consists_list;
    private RecentConsistsSimpleAdapter recent_consists_list_adapter;

    public final ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    ListView consists_list_view;
    SwipeDetector recentConsistsSwipeDetector;
    SwipeDetector recentsSwipeDetector;

    private int    locoAddress;
    private int    locoAddressSize;
    private String locoName = "";
    private int    locoSource = source_type.UNKNOWN;
    private ImageView detailsRosterImageView;
    String detailsRosterNameString = "";
    boolean newRosterImageSelected = false;
    boolean hasLocalRosterImage = false;
    boolean LocalRosterImageRemoved = false;
    Button buttonRemoveRosterImage;
    Button buttonClose;

    private String sWhichThrottle = "0";  // "0" or "1" or "2" + roster name
    int whichThrottle = 0;
    private int result;
    protected boolean selectLocoRendered = false;         // this will be true once setLabels() runs following rendering of the loco select textViews

    protected threaded_application mainapp; // hold pointer to mainapp

    private SharedPreferences prefs;
    private String defaultAddressLength;
    private Menu SMenu;

    protected final int layoutViewId = R.layout.select_loco;

    private String prefRosterFilter = "";
    EditText filterRosterText;

    RelativeLayout rlAddress;
    RelativeLayout rlAddressHelp;
    TextView rlRosterHeading;
    RelativeLayout rlRosterHeaderGroup;
    RelativeLayout rlRosterEmpty;
    LinearLayout llRoster;
    LinearLayout rlRecentHeader;
    LinearLayout llRecent;
    LinearLayout rlRecentConsistsHeader;
    LinearLayout llRecentConsists;
    RelativeLayout rlIDnGo;
    RadioButton rbAddress;
    RadioButton rbRoster;
    RadioButton rbRecent;
    RadioButton rbRecentConsists;
    RadioButton rbIDnGo;
    Button rosterDownloadButton;

    boolean prefRosterRecentLocoNames = true;
    boolean removingLocoOrForceReload = false; //flag used to indicate that the selected loco is being removed and not to save it.
    boolean removingConsistOrForceRewrite = false; //flag used to indicate that the selected consist is being removed and not to save it.
    ListView recentListView;
    ListView rosterListView;

    String overrideThrottleName;

    private int maxAddr = 9999;

    // populate the on-screen roster view from global hashmap
    @SuppressLint("DefaultLocale")
    public void refreshRosterList() {
        // clear and rebuild
        rosterList.clear();  // local list. For UI
        mainapp.rosterFullList.clear(); // full global list
        if (((mainapp.roster_entries != null)  // add roster and consist entries if any defined
                && (!mainapp.roster_entries.isEmpty()))
                || ((mainapp.consist_entries != null)
                && (!mainapp.consist_entries.isEmpty()))) {

            //only show this warning once, it will be skipped for each entry below
            if (mainapp.rosterJmriWeb == null) {
                Log.w("Engine_Driver", "select_loco: refreshRosterList(): xml roster not available");
            }

            int position = -1;

            //put roster entries into screen list
            if (mainapp.roster_entries != null) {
                ArrayList<String> rns = new ArrayList<>(mainapp.roster_entries.keySet());  //copy from synchronized map to avoid holding it while iterating
                for (String rostername : rns) {
                    // put key and values into temp hashmap
                    HashMap<String, String> hm = new HashMap<>();
                    hm.put("roster_name", rostername);
                    hm.put("roster_address", mainapp.roster_entries.get(rostername));
                    hm.put("roster_entry_type", "loco");
                    String owner = "";
                    if ((mainapp.rosterJmriWeb!=null) && (mainapp.rosterJmriWeb.get(rostername)!=null)) {
                        owner = Objects.requireNonNull(mainapp.rosterJmriWeb.get(rostername)).getOwner();
                        boolean foundOwner = false;
                        for (int j=0;j< rosterOwnersList.size(); j++) {
                            if (rosterOwnersList.get(j).equals(owner)) {
                                foundOwner = true;
                                break;
                            }
                        }
                        if (!foundOwner) {
                            rosterOwnersList.add(owner);
                        }
                    }
                    hm.put("roster_owner", owner);
                    position++;
                    hm.put("roster_position", String.format("%04d",position));


                    boolean includeInList = false;
                    if ((prefRosterFilter.isEmpty()) && (rosterOwnersFilterIndex==0) ) {
                        includeInList = true;
                    } else if ((!prefRosterFilter.isEmpty()) && (rosterOwnersFilterIndex==0)) {
                         if (rostername.toUpperCase().contains(prefRosterFilter.toUpperCase())) {
                            includeInList = true;
                         }
                    } else if ((prefRosterFilter.isEmpty()) && (rosterOwnersFilterIndex > 0)) {
                        if (owner.equals(rosterOwnersList.get(rosterOwnersFilterIndex))) {
                            includeInList = true;
                        }
                    } else { // if ((prefRosterFilter.length() > 0) && (rosterOwnersFilterIndex > 0)) {
                        if ( (rostername.toUpperCase().contains(prefRosterFilter.toUpperCase()))
                            && owner.equals(rosterOwnersList.get(rosterOwnersFilterIndex)) ) {
                            includeInList = true;
                        }
                    }

//                    if ((prefRosterFilter.length() == 0) || (rostername.toUpperCase().contains(prefRosterFilter.toUpperCase()))) {
                    if (includeInList) {
                        //add icon if url set
                        if (mainapp.rosterJmriWeb != null) {
                            if (mainapp.rosterJmriWeb.get(rostername) != null) {
                                String iconPath = Objects.requireNonNull(mainapp.rosterJmriWeb.get(rostername)).getIconPath();
                                if (iconPath != null) {
                                    hm.put("roster_icon", iconPath + "?maxHeight=52");  //include sizing instructions
                                } else {
                                    Log.d("Engine_Driver", "select_loco: refreshRosterList(): xml roster entry " + rostername + " found, but no icon specified.");
                                }
                            } else {
                                Log.w("Engine_Driver", "select_loco: refreshRosterList(): WiThrottle roster entry " + rostername + " not found in xml roster.");
                            }
                        }
                        // add temp hashmap to list which view is hooked to
                        rosterList.add(hm);
                    }
                    mainapp.rosterFullList.add(hm); // add to the global list regardless
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
                    hm.put("roster_owner", "");
                    position++;
                    hm.put("roster_position", String.format("%04d",position));

                    // add temp hashmap to list which view is hooked to
                    rosterList.add(hm);
//                    mainapp.rosterFullList.add(hm); // don't add consists to the global list
                }
            }

            Comparator<HashMap<String, String>> comparator = new Comparator<HashMap<String, String>>() {
                @Override
                public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
                    int rslt;
                    String s0;
                    String s1;
                    switch (mainapp.rosterOrder) {
                        case sort_type.ID:
                            s0 = threaded_application.formatNumberInName(arg0.get("roster_address"));
                            s1 = threaded_application.formatNumberInName(arg1.get("roster_address"));
                            break;
                        case sort_type.POSITION:
                            s0 = threaded_application.formatNumberInName(arg0.get("roster_position"));
                            s1 = threaded_application.formatNumberInName(arg1.get("roster_position"));
                            break;
                        case sort_type.NAME:
                        default:
                            s0 = arg0.get("roster_name");
                            s1 = arg1.get("roster_name");
                            s0 = (s0 != null) ? s0.replaceAll("_", " ").toLowerCase() : "";
                            s1 = (s1 != null) ? s1.replaceAll("_", " ").toLowerCase() : "";
                            break;
                    }
                    rslt = s0.compareTo(s1);
                    return rslt;
                }
            };
            Collections.sort(rosterList, comparator);

            rosterListAdapter.notifyDataSetChanged();
            View v = findViewById(R.id.roster_list_heading);
            if (prefSelectLocoMethod.equals(which_method.ROSTER)) v.setVisibility(View.VISIBLE);  // only show it if 'roster' is the currently selected method
            v = findViewById(R.id.filter_roster_text);
            v.setVisibility(View.VISIBLE);
            v = findViewById(R.id.roster_list);
            v.setVisibility(View.VISIBLE);
            v = findViewById(R.id.roster_list_empty);
            v.setVisibility(GONE);

            int visible = GONE;
            boolean prefRosterOwnersFilterShowOption = prefs.getBoolean("prefRosterOwnersFilterShowOption", getResources().getBoolean(R.bool.prefRosterOwnersFilterShowOptionDefaultValue));
            if ( (rosterOwnersList.size()>1) && (prefRosterOwnersFilterShowOption) ) {
                visible = VISIBLE;
            }

            v = findViewById(R.id.roster_list_filter_owner_label);
            v.setVisibility(visible);
            v = findViewById(R.id.roster_filter_owner);
            v.setVisibility(visible);

        } else { // hide roster section if nothing to show
            View v = findViewById(R.id.roster_list_heading);
            v.setVisibility(GONE);
            v = findViewById(R.id.filter_roster_text);
            v.setVisibility(GONE);
            v = findViewById(R.id.roster_list);
            v.setVisibility(GONE);
            v = findViewById(R.id.roster_list_empty);
            v.setVisibility(View.VISIBLE);
        } // if roster_entries not null

        rosterDownloadButton.setEnabled((!mainapp.rosterFullList.isEmpty()) && (mainapp.roster_entries.size()==mainapp.rosterFullList.size()));
    }

    private String getLocoIconUrlFromRoster(String engineAddress, String engineName) {
        if (prefRosterRecentLocoNames) {
            if ((mainapp.roster_entries != null) && (!mainapp.roster_entries.isEmpty()) && (mainapp.rosterJmriWeb != null)) {
                for (String rostername : mainapp.roster_entries.keySet()) {  // loop thru roster entries,
                    if (engineName.isEmpty()) {
                        if (mainapp.roster_entries.get(rostername).equals(engineAddress)) {
                            RosterEntry rosterentry = mainapp.rosterJmriWeb.get(rostername);
                            if (rosterentry == null) return "";
                            String iconPath = rosterentry.getIconPath();  //if found, return the icon url
                            if (iconPath == null) return "";
                            return iconPath;
                        }
                    } else { // if there is a name as well, confirm they match (for entries with the same address)
                        if (rostername.equals(engineName)) {
                            RosterEntry rosterentry = mainapp.rosterJmriWeb.get(rostername);
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
    protected void setLabels() {

        refreshRosterList();
        if (prefSelectLocoMethod.equals(which_method.FIRST)) {
            if ((mainapp.rosterJmriWeb != null) && (!mainapp.rosterJmriWeb.isEmpty())) {
                prefSelectLocoMethod = which_method.ROSTER;
                showMethod(which_method.ROSTER);
            } else {
                prefSelectLocoMethod = which_method.ADDRESS;
                showMethod(which_method.ADDRESS);
            }
        }

        boolean prefShowAddressInsteadOfName = prefs.getBoolean("prefShowAddressInsteadOfName",
                getResources().getBoolean(R.bool.prefShowAddressInsteadOfNameDefaultValue));

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
        llEditConsist.setVisibility(GONE);

        if ((mainapp.consists != null) && (mainapp.consists[whichThrottle].isActive())) {
            if (mainapp.consists[whichThrottle].size() > 1) {
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
            bR.setVisibility(GONE);
            llThrottle.setVisibility(GONE);
        }

        if (SMenu != null) {
            mainapp.displayEStop(SMenu);
        }

    }

    // Handle messages from the communication thread back to this thread
    // (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class SelectLocoHandler extends Handler {

        public SelectLocoHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE:
                    String response_str = msg.obj.toString();
                    Log.d("Engine_Driver", "select_loco: SelectLocoHandler(): RESPONSE - message <--:" + response_str);
                    if (response_str.length() >= 3) {
                        String comA = response_str.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(comA)) {
                            mainapp.setPowerStateButton(SMenu);
                        }
                    }
                    if (!response_str.isEmpty()) {
                        char com1 = response_str.charAt(0);
                        if (com1 == 'R') {                                  //refresh labels when any roster response is received
                            rosterListAdapter.notifyDataSetChanged();
                            setLabels();
                            break;
                        } else if (com1 == 'M' && response_str.length() >= 3) { // refresh Release buttons if loco is added or removed from a consist
                            char com2 = response_str.charAt(2);
                            if (com2 == '+' || com2 == '-')
                                setLabels();
                            break;
                        } else { // ignore everything else
                            Log.d("Engine_Driver", "select_loco: SelectLocoHandler(): RESPONSE - ignoring message: " + response_str);
                            break;
                        }
                    }
                    if (!selectLocoRendered)         // call setLabels() if the select loco textViews had not rendered the last time it was called
                        setLabels();
                    break;
                case message_type.WIT_CON_RETRY:
                    Log.d("Engine_Driver", "select_loco: SelectLocoHandler(): WIT_CON_RETRY");
                    witRetry(msg.obj.toString());
                    break;
                case message_type.ROSTER_UPDATE:
                    Log.d("Engine_Driver", "select_loco: SelectLocoHandler(): ROSTER_UPDATE");
                case message_type.WIT_CON_RECONNECT:
                    Log.d("Engine_Driver", "select_loco: SelectLocoHandler(): WIT_CON_RECONNECT");
                    rosterListAdapter.notifyDataSetChanged();
                    setLabels();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                    Log.d("Engine_Driver", "select_loco: SelectLocoHandler(): DISCONNECT");
                    endThisActivity();
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
    void releaseLoco(int whichThrottle) {
        mainapp.storeThrottleLocosForReleaseDCCEX(whichThrottle);
        mainapp.consists[whichThrottle].release();
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", whichThrottle); // pass 0, 1 or 2 in message
    }

    boolean saveUpdateList;         // save value across ConsistEdit activity 
    boolean newEngine;              // save value across ConsistEdit activity

    void acquireLoco(boolean bUpdateList, int numberInConsist) { // if numberInConsist is greater than -1 it is not from the recent consists list
        Log.d("Engine_Driver", "select_loco: acquireLoco()");
        String roster_name = "";
        String sAddr = importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, true);
        Loco l = new Loco(sAddr);
        if (locoSource != source_type.ADDRESS) {
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
        if (mainapp.consists == null || mainapp.consists[whichThrottle] == null) {
            if (mainapp.consists == null)
                Log.d("Engine_Driver", "select_loco: acquireLoco(): consists is null");
            else if (mainapp.consists[whichThrottle] == null)
                Log.d("Engine_Driver", "select_loco: acquireLoco(): consists[" + whichThrottle + "] is null");
            endThisActivity();
            return;
        }

        Consist consist = mainapp.consists[whichThrottle];

        // if we already have it show message and request it anyway
        if (!consist.isEmpty()) {
            for (int i = 0; i <= consist.size(); i++) {
                if (consist.getLoco(sAddr) != null) {
                    overrideThrottleName = "";
                    threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastLocoAlreadySelected, sAddr), Toast.LENGTH_SHORT);
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, sAddr, whichThrottle);  // send the acquire message anyway
                    return;
                }
            }
        }

        if (!roster_name.isEmpty()) {// add roster selection info if present
            sAddr += "<;>" + roster_name;
        }

        // user preference set to not consist, or consisting not supported in this JMRI, so drop before adding
        // ignore the preference if a recent consist was selected
        if (prefs.getBoolean("drop_on_acquire_preference", false) && numberInConsist < 0) {
            ConLoco cl = consist.getLoco(sAddr);
            if (cl == null) { // if the newly selected loco is different/not in the consist, release everything
                releaseLoco(whichThrottle);
            } else { // already have it so don't do anything
                result = RESULT_OK;
                endThisActivity();
                return;
            }
        }
        Log.d("Engine_Driver", "select_loco: acquireLoco(): sAddr:'" + sAddr +"'");

        if ((!consist.isActive()) && (numberInConsist < 1)) {               // if this is the only loco in consist then just tell WiT and exit
            consist.add(l);
            consist.setWhichSource(importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, true), locoSource);
            consist.setLeadAddr(l.getAddress());
            consist.setTrailAddr(l.getAddress());
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, sAddr, whichThrottle);
            result = RESULT_OK;
            endThisActivity();

        } else {                                // else consist exists so bring up editor
            ConLoco conLoco = consist.getLoco(sAddr);
            newEngine = (conLoco == null);
            if (newEngine || !conLoco.isConfirmed()) {        // if engine is not already in the consist, or if it is but never got acquired
                consist.add(l);
                consist.setWhichSource(importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, true), locoSource);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, sAddr, whichThrottle);

                saveUpdateList = bUpdateList;
                Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);
                consistEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));

                consist.setTrailAddr(l.getAddress());  // set the newly added loco as the trailing loco

                if (numberInConsist < 0) { // don't show the Consist edit screen.  Only used for Recent Consists
                    startActivityForResult(consistEdit, sub_activity_type.CONSIST);
                    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                }
            }
        }

        // see if we can get the functions for the recent locos list
        if ( (prefSelectLocoMethod.equals(which_method.RECENT_LOCOS)) && (!mainapp.isDCCEX) ) {
            int position = mainapp.findLocoInRecents(locoAddress ,locoAddressSize ,locoName);
            if (position>0) {
                if (mainapp.prefAlwaysUseFunctionsFromServer) { // unless overridden by the preference
                    String lead = mainapp.consists[whichThrottle].getLeadAddr();
                    if (lead.equals(sAddr)) {                        // only process if for lead engine in consist
                        processRosterFunctionString("RF29}|{1234(L)"
                                + importExportPreferences.recentLocoFunctionsList.get(position), whichThrottle);  //prepend some stuff to match old-style
                        mainapp.consists[whichThrottle].getLoco(lead).setIsServerSuppliedFunctionlabels(true);
                    }
                }
            }
        }
    }

    void processRosterFunctionString(String responseStr, int whichThrottle) {
        Log.d("Engine_Driver", "select_loco: processRosterFunctionString(): processing function labels for " + mainapp.throttleIntToString(whichThrottle));
        LinkedHashMap<Integer, String> functionLabelsMap = threaded_application.parseFunctionLabels(responseStr);
        mainapp.function_labels[whichThrottle] = functionLabelsMap; //set the appropriate global variable from the temp
    }

    //handle return from ConsistEdit
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case sub_activity_type.CONSIST: // edit consist
                if (newEngine) {
//                    saveRecentLocosList(saveUpdateList);
                    updateRecentConsists(saveUpdateList);
                }
                result = RESULT_LOCO_EDIT;                 //tell Throttle to update loco directions

                overrideThrottleName = "";
                endThisActivity();
                break;

            case ACTIVITY_SELECT_ROSTER_ENTRY_IMAGE: // edit consist
                if (resultCode == RESULT_OK && data != null) {
                    // Get the Image from data
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.MediaColumns.DATA};
                    if (selectedImage == null) break;

                    // Get the cursor
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor == null) break;
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String imgpath = cursor.getString(columnIndex);
                    cursor.close();

                    File image_file = null;
                    try {
                        image_file = new File(imgpath);
                    } catch (Exception e) {    // isBackward returns null if address is not in consist - should not happen since address was selected from consist list
                        Log.d("Engine_Driver", "select_loco: onActivityResult(): Load image failed : " + imgpath);
                    }
                    if ( (image_file != null) && (image_file.exists()) ) {
                        try {
                            int inWidth;
                            int inHeight;

                            InputStream in = new FileInputStream(image_file.getPath());

                            // decode image size (decode metadata only, not the whole image)
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeStream(in, null, options);
                            in.close();

                            // save width and height
                            inWidth = options.outWidth;
                            inHeight = options.outHeight;

                            // decode full image pre-resized
                            in = new FileInputStream(image_file.getPath());
                            options = new BitmapFactory.Options();
                            // calc rough re-size (this is no exact resize)
                            options.inSampleSize = Math.max(inWidth/150, inHeight/150);
                            // decode full image
                            Bitmap roughBitmap = BitmapFactory.decodeStream(in, null, options);

                            // calc exact destination size
                            Matrix m = new Matrix();
                            RectF inRect = new RectF(0, 0, roughBitmap.getWidth(), roughBitmap.getHeight());
                            RectF outRect = new RectF(0, 0, 150, 150);
                            m.setRectToRect(inRect, outRect, Matrix.ScaleToFit.CENTER);
                            float[] values = new float[9];
                            m.getValues(values);

                            // resize bitmap
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(roughBitmap, (int) (roughBitmap.getWidth() * values[0]),
                                                                            (int) (roughBitmap.getHeight() * values[4]), true);

                            int degree = getRotateDegreeFromExif(image_file.getPath());
                            Matrix matrix = new Matrix();
                            matrix.postRotate(degree);/*from   w  w w.  j  a v  a2 s  .co  m*/
                            if (degree!=0) {
                                Bitmap rotatedImage = Bitmap.createBitmap(resizedBitmap, 0, 0,
                                        resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true);
                                detailsRosterImageView.setImageBitmap(rotatedImage);
                            } else {
                                detailsRosterImageView.setImageBitmap(resizedBitmap);
                            }

//                            detailsRosterImageView.setImageBitmap(BitmapFactory.decodeFile(image_file.getPath()));
                            detailsRosterImageView.setVisibility(View.VISIBLE);
                            detailsRosterImageView.invalidate();
                            newRosterImageSelected = true;
                            hasLocalRosterImage = true;
                            LocalRosterImageRemoved = false;
                            buttonRemoveRosterImage.setVisibility(VISIBLE);
                            buttonClose.setText(getString(R.string.rosterEntryImageSaveButtonText));
                        } catch (Exception e) {
                            Log.d("Engine_Driver", "select_loco: onActivityResult(): load image - image file found but could not loaded");
                        }
                    }
                }
                break;
        }
    }

    /** @noinspection SameParameterValue*/ //write the recent locos to a file
    void saveRecentLocosList(boolean bUpdateList) {

        //if not updating list or no SD Card present then nothing else to do
        if (!bUpdateList || !android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;


        if (!removingLocoOrForceReload) {

            // check if it is already in the list and remove it
            String keepFunctions = "";
            for (int i = 0; i < importExportPreferences.recentLocoAddressList.size(); i++) {
                Log.d("Engine_Driver", "select_loco: saveRecentLocosList(): vLocoName='"+locoName+"', address="+locoAddress+", size="+locoAddressSize);
                Log.d("Engine_Driver", "select_loco: saveRecentLocosList(): sLocoName='"+importExportPreferences.recentLocoNameList.get(i)
                        + "', address=" + importExportPreferences.recentLocoAddressList.get(i)
                        + ", size="+importExportPreferences.recentLocoAddressSizeList.get(i));
                if (locoAddress == importExportPreferences.recentLocoAddressList.get(i)
                        && locoAddressSize == importExportPreferences.recentLocoAddressSizeList.get(i)
                        && locoName.equals(importExportPreferences.recentLocoNameList.get(i))) {

                    keepFunctions = importExportPreferences.recentLocoFunctionsList.get(i);
                    if ( (i==0) && (!keepFunctions.isEmpty()) ) { return; } // if it already at the start of the list, don't do anything

                    importExportPreferences.recentLocoAddressList.remove(i);
                    importExportPreferences.recentLocoAddressSizeList.remove(i);
                    importExportPreferences.recentLocoNameList.remove(i);
                    importExportPreferences.recentLocoSourceList.remove(i);
                    importExportPreferences.recentLocoFunctionsList.remove(i);
                    Log.d("Engine_Driver", "select_loco: saveRecentLocosList(): Loco '"+ locoName + "' removed from Recents");
                    break;
                }
            }

            // now append it to the beginning of the list
            importExportPreferences.recentLocoAddressList.add(0, locoAddress);
            importExportPreferences.recentLocoAddressSizeList.add(0, locoAddressSize);
            importExportPreferences.recentLocoNameList.add(0, locoName);
            importExportPreferences.recentLocoSourceList.add(0, locoSource);
            importExportPreferences.recentLocoFunctionsList.add(0, keepFunctions);
            Log.d("Engine_Driver", "select_loco: saveRecentLocosList(): Loco '"+ locoName + "' added to Recents");
        }

        importExportPreferences.writeRecentLocosListToFile(prefs);
    }

    private void loadRecentLocosList(boolean reload) {
        importExportPreferences.recentLocoAddressList = new ArrayList<>();
        importExportPreferences.recentLocoAddressSizeList = new ArrayList<>();
        importExportPreferences.recentLocoNameList = new ArrayList<>();
        importExportPreferences.recentLocoSourceList = new ArrayList<>();
        importExportPreferences.recentLocoFunctionsList = new ArrayList<>();
        if (reload) {
            recentEngineList = new ArrayList<>();
        }

        rbRecent = findViewById(R.id.select_loco_method_recent_button);

        //if no SD Card present then there is no recent locos list
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent locos list requires SD Card
            TextView v = findViewById(R.id.recent_engines_heading);
            v.setText(getString(R.string.sl_recent_engine_notice));
            rbRecent.setVisibility(GONE); // if the list is empty, hide the radio button
        } else {

            importExportPreferences.loadRecentLocosListFromFile();

            for (int i = 0; i < importExportPreferences.recentLocoAddressList.size(); i++) {
                HashMap<String, String> hm = new HashMap<>();
                String engineAddressString = importExportPreferences.locoAddressToString(
                        importExportPreferences.recentLocoAddressList.get(i),
                        importExportPreferences.recentLocoAddressSizeList.get(i), false);
                String engineIconUrl = getLocoIconUrlFromRoster(engineAddressString, importExportPreferences.recentLocoNameList.get(i));

                hm.put("engine_icon", engineIconUrl);
                hm.put("engine", importExportPreferences.recentLocoNameList.get(i)); // the larger loco name text
                hm.put("engine_name", importExportPreferences.locoAddressToHtml(
                        importExportPreferences.recentLocoAddressList.get(i),
                        importExportPreferences.recentLocoAddressSizeList.get(i),
                        importExportPreferences.recentLocoSourceList.get(i)));   // the small loco address field at the top of the row
                hm.put("locoAddress", Integer.toString(importExportPreferences.recentLocoAddressList.get(i)));
                hm.put("last_used", Integer.toString(i));
                hm.put("functions", importExportPreferences.recentLocoFunctionsList.get(i));
                recentEngineList.add(hm);
            }

            if (importExportPreferences.recentLocoAddressList.isEmpty()) {  // if the list is empty, hide the radio button
                rbRecent.setVisibility(GONE);
            } else {

                Comparator<HashMap<String, String>> comparator = new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
                        int rslt;
                        String s0;
                        String s1;
                        switch (mainapp.recentLocosOrder) {
                            case sort_type.NAME: {
                                s0 = threaded_application.formatNumberInName(arg0.get("engine").replaceAll("_", " ").toLowerCase());
                                s1 = threaded_application.formatNumberInName(arg1.get("engine").replaceAll("_", " ").toLowerCase());
                                break;
                            }
                            case sort_type.ID: {
                                s0 = threaded_application.formatNumberInName(arg0.get("locoAddress"));
                                s1 = threaded_application.formatNumberInName(arg1.get("locoAddress"));
                                break;
                            }
                            case sort_type.LAST_USED:
                            default: {
                                s0 = threaded_application.formatNumberInName(arg0.get("last_used"));
                                s1 = threaded_application.formatNumberInName(arg1.get("last_used"));
                            }
                        }
                        rslt = s0.compareTo(s1);
                        return rslt;
                    }
                };
                Collections.sort(recentEngineList, comparator);

                rbRecent.setVisibility(View.VISIBLE);
            }
        }
        recentListAdapter.notifyDataSetChanged();
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
            myRadioButton.setVisibility(GONE); // if the list is empty, hide the radio button
        } else {
            importExportPreferences.loadRecentConsistsListFromFile();
            for (int i = 0; i < importExportPreferences.consistEngineAddressList.size(); i++) {
                HashMap<String, String> hm = new HashMap<>();
                hm.put("consist_name", mainapp.getRosterNameFromAddress(importExportPreferences.consistNameHtmlList.get(i),
                        false));
                hm.put("consist", mainapp.locoAndConsistNamesCleanupHtml(importExportPreferences.consistNameList.get(i)));
                hm.put("last_used", Integer.toString(i));
                recent_consists_list.add(hm);
            }
            if (importExportPreferences.consistEngineAddressList.isEmpty()) {
                myRadioButton.setVisibility(GONE); // if the list is empty, hide the radio button
            } else {
                Comparator<HashMap<String, String>> comparator = new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
                        int rslt;
                        String s0;
                        String s1;
                        switch (mainapp.recentConsistsOrder) {
                            case sort_type.NAME: {
                                s0 = threaded_application.formatNumberInName(arg0.get("consist").replaceAll("_", " ").toLowerCase());
                                s1 = threaded_application.formatNumberInName(arg1.get("consist").replaceAll("_", " ").toLowerCase());
                                break;
                            }
                            case sort_type.ID: {
                                s0 = threaded_application.formatNumberInName(arg0.get("consist_name"));
                                s1 = threaded_application.formatNumberInName(arg1.get("consist_name"));
                                break;
                            }
                            case sort_type.LAST_USED:
                            default: {
                                s0 = threaded_application.formatNumberInName(arg0.get("last_used"));
                                s1 = threaded_application.formatNumberInName(arg1.get("last_used"));
                            }
                        }
                        rslt = s0.compareTo(s1);
                        return rslt;
                    }
                };
                Collections.sort(recent_consists_list, comparator);

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

        if (!removingConsistOrForceRewrite) {

            int k = -1;
            for (ConLoco l : conLocos) {
                k++;
                tempConsistEngineAddressList_inner.add(l.getIntAddress());
                tempConsistAddressSizeList_inner.add(l.getIntAddressLength());
                String addr = importExportPreferences.locoAddressToString(l.getIntAddress(), l.getIntAddressLength(), true);
                tempConsistDirectionList_inner.add((consist.isBackward(addr) ? direction_type.BACKWARD : direction_type.FORWARD));
                String rosterName = "";
                if (l.getRosterName() != null) {
                    rosterName = l.getRosterName();
                }
                tempConsistSourceList_inner.add(l.getWhichSource());
                tempConsistRosterNameList_inner.add(rosterName);
                tempConsistLightList_inner.add(k == 0 ? light_follow_type.FOLLOW : consist.isLight(addr));   // always set the first loco as 'follow'

                int lastItem = tempConsistEngineAddressList_inner.size() - 1;
                oneConsistHtml.append(importExportPreferences.addOneConsistAddressHtml(
                        tempConsistEngineAddressList_inner.get(lastItem),
                        tempConsistAddressSizeList_inner.get(lastItem)
//                        ,
//                        tempConsistDirectionList_inner.get(lastItem),
//                        tempConsistSourceList_inner.get(lastItem),
//                        tempConsistLightList_inner.get(lastItem)
                ));
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
            if ((!importExportPreferences.consistEngineAddressList.isEmpty())
                    && (importExportPreferences.consistEngineAddressList.get(0).size() == (tempConsistEngineAddressList_inner.size() - 1))) {
                // check of the last added one is the same other then the last extra loco
                for (int j = 0; j < tempConsistEngineAddressList_inner.size() - 1; j++) {
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
            if (whichEntryIsBeingUpdated > 0) { //this may already have a custom name
                consistName = importExportPreferences.consistNameList.get(whichEntryIsBeingUpdated - 1);
            }
            importExportPreferences.consistNameList.add(0, consistName);
            importExportPreferences.consistNameHtmlList.add(0, oneConsistHtml.toString());

        }
        importExportPreferences.writeRecentConsistsListToFile(prefs, whichEntryIsBeingUpdated);
    }


    // listener for the Acquire button when entering a DCC Address
    public class AcquireButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            EditText entry = findViewById(R.id.loco_address);
            try {
                locoAddress = Integer.parseInt(entry.getText().toString());
            } catch (NumberFormatException e) {
                threaded_application.safeToast("ERROR - Please enter a valid DCC address.\n" + e.getMessage(), Toast.LENGTH_SHORT);
                return;
            }
            Spinner spinner = findViewById(R.id.address_length);
            locoAddressSize = spinner.getSelectedItemPosition();
            locoName = importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, false);
            sWhichThrottle += locoName;
            locoSource = source_type.ADDRESS;

            acquireLoco(true, -1);
            hideSoftKeyboard(v);
            mainapp.buttonVibration();
        }
    }

    // listener for the ID 'n' Go button, when clicked, send the id request and return to Throttle
    public class IdngoButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            Consist consist = mainapp.consists[whichThrottle];
            consist.setWaitingOnID(true);
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, "*", whichThrottle);
            result = RESULT_OK;
            hideSoftKeyboard(v);
            endThisActivity();
            mainapp.buttonVibration();
        }
    }

    public class ReleaseButtonListener implements View.OnClickListener {
        final int _throttle;

        ReleaseButtonListener(int throttle) {
            _throttle = throttle;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();

            releaseLoco(_throttle);
            overrideThrottleName = "";
            hideSoftKeyboard(v);
            endThisActivity();
            mainapp.buttonVibration();
        }
    }

    public class EditConsistButtonListener implements View.OnClickListener {
        final int _throttle;
        final Activity _selectLocoActivity;

        EditConsistButtonListener(int throttle, Activity selectLocoActivity) {
            _throttle = throttle;
            _selectLocoActivity = selectLocoActivity;
        }

        public void onClick(View v) {
            Intent consistEdit = new Intent().setClass(_selectLocoActivity, ConsistEdit.class);
            consistEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));
            consistEdit.putExtra("saveConsistsFile", 'Y');

            hideSoftKeyboard(v);
            startActivityForResult(consistEdit, sub_activity_type.CONSIST);
            connection_activity.overridePendingTransition(_selectLocoActivity, R.anim.fade_in, R.anim.fade_out);
            mainapp.buttonVibration();
        }
    }

    public class EditConsistLightsButtonListener implements View.OnClickListener {
        final int _throttle;
        final Activity _selectLocoActivity;

        EditConsistLightsButtonListener(int throttle, Activity selectLocoActivity) {
            _throttle = throttle;
            _selectLocoActivity = selectLocoActivity;
        }

        public void onClick(View v) {
            Intent consistLightsEdit = new Intent().setClass(_selectLocoActivity, ConsistLightsEdit.class);
            consistLightsEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));

            hideSoftKeyboard(v);
            startActivityForResult(consistLightsEdit, sub_activity_type.CONSIST_LIGHTS);
            connection_activity.overridePendingTransition(_selectLocoActivity, R.anim.fade_in, R.anim.fade_out);
            mainapp.buttonVibration();
        }
    }

    @SuppressLint("ApplySharedPref")
    public class DownloadRosterButtonListener implements View.OnClickListener {
        DownloadRosterButtonListener() {}

        public void onClick(View v) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
//                            downloadRosterToRecents();
                            importExportPreferences.loadRecentLocosListFromFile();
                            importExportPreferences.downloadRosterToRecents(context,prefs,mainapp);
                            recreate();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                    mainapp.buttonVibration();
                }
            };

            AlertDialog.Builder ab = new AlertDialog.Builder(select_loco.this);
            ab.setTitle(getApplicationContext().getResources().getString(R.string.dialogConfirmClearTitle))
                    .setMessage(getApplicationContext().getResources().getString(R.string.dialogDownloadRosterConfirmQuestion))
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.cancel, dialogClickListener);
            ab.show();

        }
    }

    public class SortRosterButtonListener implements View.OnClickListener {
        SortRosterButtonListener() {}

        public void onClick(View v) {
            switch (mainapp.rosterOrder) {
                case sort_type.NAME:
                    mainapp.rosterOrder = sort_type.ID;
                    break;
                case sort_type.ID:
                    mainapp.rosterOrder = sort_type.POSITION;
                    break;
                case sort_type.POSITION:
                default:
                    mainapp.rosterOrder = sort_type.NAME;
            }
            refreshRosterList();
            mainapp.toastSortType(mainapp.rosterOrder);
            mainapp.buttonVibration();
        }
    }

    public class SortRecentLocosButtonListener implements View.OnClickListener {
        SortRecentLocosButtonListener() {}

        public void onClick(View v) {
            switch (mainapp.recentLocosOrder) {
                case sort_type.NAME:
                    mainapp.recentLocosOrder=sort_type.ID;
                    break;
                case sort_type.ID:
                    mainapp.recentLocosOrder=sort_type.LAST_USED;
                    break;
                case sort_type.LAST_USED:
                default:
                    mainapp.recentLocosOrder=sort_type.NAME;
            }
            loadRecentLocosList(true);
            mainapp.toastSortType(mainapp.recentLocosOrder);
            mainapp.buttonVibration();
        }
    }

    public class SortRecentConsistsButtonListener implements View.OnClickListener {
        SortRecentConsistsButtonListener() {}

        public void onClick(View v) {
            switch (mainapp.recentConsistsOrder) {
                case sort_type.NAME:
                    mainapp.recentConsistsOrder=sort_type.ID;
                    break;
                case sort_type.ID:
                    mainapp.recentConsistsOrder=sort_type.LAST_USED;
                    break;
                case sort_type.LAST_USED:
                default:
                    mainapp.recentConsistsOrder=sort_type.NAME;
            }
            loadRecentConsistsList(true);
            mainapp.toastSortType(mainapp.recentConsistsOrder);
            mainapp.buttonVibration();
        }
    }

    // listener for the ID 'n' Go button, when clicked, send the id request and return to Throttle
    public class DeviceSoundsButtonListener implements View.OnClickListener {
        final Activity _selectLocoActivity;

        DeviceSoundsButtonListener(Activity selectLocoActivity) {
            _selectLocoActivity = selectLocoActivity;
        }

        public void onClick(View v) {
            Intent deviceSounds = new Intent().setClass(_selectLocoActivity, device_sounds_settings.class);
            startActivityForResult(deviceSounds, ACTIVITY_DEVICE_SOUNDS_SETTINGS);
            connection_activity.overridePendingTransition(_selectLocoActivity, R.anim.fade_in, R.anim.fade_out);
            result = RESULT_OK;
            hideSoftKeyboard(v);
            endThisActivity();
            mainapp.buttonVibration();
        }
    }

    // onClick for the Recent Locos list items
    public class RecentLocosItemListener implements AdapterView.OnItemClickListener {
        // When an item is clicked, acquire that engine.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            if (recentsSwipeDetector.swipeDetected()) {
                if (recentsSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearRecentListItem(position);
                }
            } else {  //no swipe
                if (mainapp.consists==null) return; // attempt to catch NPEs

                locoAddress = importExportPreferences.recentLocoAddressList.get(position);
                locoAddressSize = importExportPreferences.recentLocoAddressSizeList.get(position);
                locoSource = importExportPreferences.recentLocoSourceList.get(position);
                locoName = importExportPreferences.recentLocoNameList.get(position);
                // simple map of functions. Only used by 'Recent Locos' temporarily to retrieve the fucntions.
                String functions = importExportPreferences.recentLocoFunctionsList.get(position);
                if (locoSource == source_type.UNKNOWN) {
                    locoName = mainapp.getRosterNameFromAddress(importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, false), true);
                }

                sWhichThrottle += locoName;
                acquireLoco(true, -1);
                if (!functions.isEmpty()) {
                    if ( (locoSource == source_type.ROSTER)
                    || (mainapp.prefAlwaysUseFunctionsFromServer) ) { // unless overridden by the preference
                        String addrStr = ((locoAddressSize == 0) ? "S" : "L") + locoAddress;
                        String lead = mainapp.consists[whichThrottle].getLeadAddr();
                        if (lead.equals(addrStr)) {                        //*** temp - only process if for lead engine in consist
                            LinkedHashMap<Integer, String> functionLabelsMap = threaded_application.parseFunctionLabels("RF29}|{1234(L)]\\[" + functions);  //prepend some stuff to match old-style
                            mainapp.function_labels[whichThrottle] = functionLabelsMap;
                            mainapp.consists[whichThrottle].getLoco(lead).setIsServerSuppliedFunctionlabels(true);
                        }
                    }
                }
                mainapp.buttonVibration();
            }
        }
    }

    // onClick for the Recent Consists list items
    public class ConsistItemListener implements AdapterView.OnItemClickListener {
        // When an item is clicked, acquire that consist.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            String sAddr;
            int dir;
            int light;

            String tempsWhichThrottle = sWhichThrottle;

            if (recentConsistsSwipeDetector.swipeDetected()) {
                if (recentConsistsSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearRecentConsistsListItem(position);
                }
            } else {  //no swipe
                if (mainapp.consists==null) return; // attempt to catch NPEs

                overrideThrottleName = importExportPreferences.consistNameList.get(position);

                for (int i = 0; i < importExportPreferences.consistEngineAddressList.get(position).size(); i++) {

                    locoAddress = importExportPreferences.consistEngineAddressList.get(position).get(i);
                    locoAddressSize = importExportPreferences.consistAddressSizeList.get(position).get(i);
                    sAddr = importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, true);
                    locoSource = importExportPreferences.consistSourceList.get(position).get(i);
                    locoName = mainapp.getRosterNameFromAddress(importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, false), false);
                    if ((locoSource != source_type.ADDRESS) && (!importExportPreferences.consistRosterNameList.get(position).get(i).isEmpty())) {
                        locoName = importExportPreferences.consistRosterNameList.get(position).get(i);
                    }
                    sWhichThrottle = tempsWhichThrottle
                            + locoName;

                    acquireLoco(true, i);

                    Consist consist = mainapp.consists[whichThrottle];

                    dir = importExportPreferences.consistDirectionList.get(position).get(i);
                    if (dir == direction_type.BACKWARD) {
                        consist.setBackward(sAddr, true);
                    }

                    light = importExportPreferences.consistLightList.get(position).get(i);
                    if (light != light_follow_type.UNKNOWN) {
                        consist.setLight(sAddr, light);
                    }

                }
                updateRecentConsists(saveUpdateList);

                result = RESULT_LOCO_EDIT;
                mainapp.buttonVibration();
                hideSoftKeyboard(v);
                endThisActivity();
            }
        }
    }

    //Clears recent connection list of locos when button is touched or clicked
    public class ClearLocoListButtonListner implements AdapterView.OnClickListener {
        public void onClick(View v) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            clearList();
                            recreate();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                    mainapp.buttonVibration();
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

    public class ClearConsistsListButtonListener implements AdapterView.OnClickListener {
        public void onClick(View v) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            clearConsistsList();
                            recreate();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                    mainapp.buttonVibration();
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
    public class RosterItemClickListener implements
            AdapterView.OnItemClickListener {
        // When a roster item is clicked, send request to acquire that engine.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            //use clicked position in list to retrieve roster item object from roster_list
            HashMap<String, String> hm = rosterList.get(position);
            String rosterNameString = hm.get("roster_name");
            String rosterAddressString = hm.get("roster_address");
            String rosterEntryType = hm.get("roster_entry_type");

            String rosterEntryIcon = hm.get("roster_icon");
            if (rosterEntryIcon != null) {
//                String imgFileName = "";
                ViewGroup vg = (ViewGroup) v;
                if (vg!=null) {
                    ImageView iv = (ImageView) vg.getChildAt(0);
                    if (iv != null) {
                        writeLocoImageToFile(rosterNameString, iv);
                    }
                }
            }
            String rosterEntryOwner = hm.get("roster_owner");

            // parse address and length from string, e.g. 2591(L)
            String[] ras = threaded_application.splitByString(rosterAddressString, "(");
            if (!ras[0].isEmpty()) {  //only process if address found
                locoAddressSize = (ras[1].charAt(0) == 'L')
                        ? address_type.LONG
                        : address_type.SHORT;   // convert S/L to 0/1
                try {
                    locoAddress = Integer.parseInt(ras[0]);   // convert address to int
                } catch (NumberFormatException e) {
                    threaded_application.safeToast("ERROR - could not parse address\n" + e.getMessage(), Toast.LENGTH_SHORT);
                    return; //get out, don't try to acquire
                }
                if ("loco".equals(rosterEntryType)) {
                    locoName = rosterNameString;
                    sWhichThrottle += rosterNameString;     //append rostername if type is loco (not consist) 
                }
                locoSource = source_type.ROSTER;

                boolean bRosterRecent = prefs.getBoolean("roster_recent_locos_preference",
                        getResources().getBoolean(R.bool.prefRosterRecentLocosDefaultValue));

                overrideThrottleName = rosterNameString;
                acquireLoco(bRosterRecent, -1);
                hideSoftKeyboard(v);
                mainapp.buttonVibration();
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    private void filterRoster() {
        prefRosterFilter = filterRosterText.getText().toString().trim();
        prefs.edit().putString("prefRosterFilter", prefRosterFilter).commit();
        refreshRosterList();
        //        onCreate(null);
    }

    // Handle pressing of the back button to simply return to caller
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        if (key == KeyEvent.KEYCODE_BACK) {
            overrideThrottleName = "";
            endThisActivity();
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("ClickableViewAccessibility")
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

        setContentView(layoutViewId);

        // put pointer to this activity's handler in main app's shared variable
        mainapp.select_loco_msg_handler = new SelectLocoHandler(Looper.getMainLooper());

//        getDefaultSortOrderRoster();
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
        rosterList = new ArrayList<>(); // local version for UI. may not be complete
        mainapp.rosterFullList = new ArrayList<>(); // global complete list
        rosterListAdapter = new RosterSimpleAdapter(this, rosterList,
                R.layout.roster_list_item, new String[]{"roster_name",
                "roster_address", "roster_icon", "roster_owner"}, new int[]{R.id.roster_name_label,
                R.id.roster_address_label, R.id.roster_icon_image, R.id.roster_owner_label});
        rosterOwnersList = new ArrayList<>();

        rosterListView = findViewById(R.id.roster_list);
        rosterListView.setAdapter(rosterListAdapter);
        rosterListView.setOnItemClickListener(new RosterItemClickListener());
        rosterListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongRosterListItemClick(pos);
            }
        });

        rosterList.clear();
        rosterOwnersList.clear();
        rosterOwnersList.add(this.getResources().getString(R.string.prefRosterOwnersFilterDefaultOwner));

        // Set the options for the owners
        Spinner rosterFilterOwners = findViewById(R.id.roster_filter_owner);
        spinner_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rosterOwnersList);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rosterFilterOwners.setAdapter(spinner_adapter);
        rosterFilterOwners.setOnItemSelectedListener(new OwnersFilterSpinnerListener());
        rosterOwnersFilter = prefs.getString("prefRosterOwnersFilterSelected", this.getResources().getString(R.string.prefRosterOwnersFilterDefaultOwner));
        rosterFilterOwners.setSelection(0);

        // Set up a list adapter to allow adding the list of recent engines to the UI.
        recentEngineList = new ArrayList<>();
        recentListAdapter = new RecentSimpleAdapter(this, recentEngineList, R.layout.engine_list_item,
                new String[]{"engine"},
                new int[]{R.id.engine_item_label, R.id.engine_icon_image});
        recentListView = findViewById(R.id.engine_list);
        recentListView.setAdapter(recentListAdapter);
        recentListView.setOnTouchListener(recentsSwipeDetector = new SwipeDetector());
        recentListView.setOnItemClickListener(new RecentLocosItemListener());
        recentListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongRecentListItemClick(pos);
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
        consists_list_view.setOnItemClickListener(new ConsistItemListener());
        consists_list_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongRecentConsistsListItemClick(pos);
            }
        });
        loadRecentConsistsList(false);

        // Set the button listeners.
        Button button = findViewById(R.id.acquire_button);
        button.setOnClickListener(new AcquireButtonListener());
        button = findViewById(R.id.clear_Loco_List_button);
        button.setOnClickListener(new ClearLocoListButtonListner());
        button = findViewById(R.id.clear_consists_list_button);
        button.setOnClickListener(new ClearConsistsListButtonListener());
        button = findViewById(R.id.idngo_button);
        button.setOnClickListener(new IdngoButtonListener());

        filterRosterText = findViewById(R.id.filter_roster_text);
        filterRosterText.setText(prefRosterFilter);
        filterRosterText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                filterRoster();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        filterRosterText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                    filterRoster();
                    return true;
                } else
                    return false;
            }
        });

        defaultAddressLength = prefs.getString("default_address_length", this
                .getResources().getString(
                        R.string.prefDefaultAddressLengthDefaultValue));
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sWhichThrottle = extras.getString("sWhichThrottle");
            whichThrottle = mainapp.throttleCharToInt(sWhichThrottle.charAt(0));
        }

        button = findViewById(R.id.Sl_release);
        button.setOnClickListener(new ReleaseButtonListener(whichThrottle));

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
                    hideSoftKeyboard(v);
                    return true;
                } else
                    return false;
            }
        });

        // consist edit button
        button = findViewById(R.id.Sl_edit_consist);
        button.setOnClickListener(new EditConsistButtonListener(whichThrottle, this));

        // consist lights edit button
        button = findViewById(R.id.Sl_edit_consist_lights);
        button.setOnClickListener(new EditConsistLightsButtonListener(whichThrottle, this));

        // loco sounds button
        button = findViewById(R.id.Sl_device_sounds);
        button.setOnClickListener(new DeviceSoundsButtonListener(this));
//        if ( (mainapp.prefDeviceSounds[0].equals("none")) && (mainapp.prefDeviceSounds[1].equals("none")) ) {
//            button.setVisibility(GONE);
//        }

        rbAddress = findViewById(R.id.select_loco_method_address_button);
        rbRoster = findViewById(R.id.select_loco_method_roster_button);
        rbRecentConsists = findViewById(R.id.select_consists_method_recent_button);
        rbIDnGo = findViewById(R.id.select_loco_method_idngo);

        prefSelectLocoMethod = prefs.getString("prefSelectLocoMethod", which_method.FIRST);
        // make sure the radio button will be pointing to something valid
        if (((recent_consists_list.isEmpty()) && (prefSelectLocoMethod.equals(which_method.RECENT_CONSISTS)))
                | ((importExportPreferences.recentLocoAddressList.isEmpty()) && (prefSelectLocoMethod.equals(which_method.RECENT_LOCOS)))
                | (!mainapp.supportsIDnGo() && prefSelectLocoMethod.equals(which_method.IDNGO))) {
            prefSelectLocoMethod = which_method.ADDRESS;
        }

        rlAddress = findViewById(R.id.enter_loco_group);
        rlAddressHelp = findViewById(R.id.enter_loco_group_help);
        rlRosterHeading = findViewById(R.id.roster_list_heading);
        rlRosterHeaderGroup = findViewById(R.id.roster_list_header_group);
        rlRosterEmpty = findViewById(R.id.roster_list_empty_group);
        llRoster = findViewById(R.id.roster_list_group);
        rlRecentHeader = findViewById(R.id.engine_list_header_group);
        llRecent = findViewById(R.id.engine_list_wrapper);
        rlRecentConsistsHeader = findViewById(R.id.consists_list_header_group);
        llRecentConsists = findViewById(R.id.consists_list_wrapper);
        rlIDnGo = findViewById(R.id.idngo_group);

        // setup the download button
        rosterDownloadButton = findViewById(R.id.roster_download);
        DownloadRosterButtonListener downloadRosterButtonListener = new DownloadRosterButtonListener();
        rosterDownloadButton.setOnClickListener(downloadRosterButtonListener);

        // setup the sort buttons
        Button b = findViewById(R.id.roster_sort);
        SortRosterButtonListener sortRosterButtonListener = new SortRosterButtonListener();
        b.setOnClickListener(sortRosterButtonListener);

        b = findViewById(R.id.recent_engines_sort);
        SortRecentLocosButtonListener sortRecentLocosButtonListener = new SortRecentLocosButtonListener();
        b.setOnClickListener(sortRecentLocosButtonListener);

        b = findViewById(R.id.recent_consists_sort);
        SortRecentConsistsButtonListener sortRecentConsistsButtonListener = new SortRecentConsistsButtonListener();
        b.setOnClickListener(sortRecentConsistsButtonListener);

        showMethod(prefSelectLocoMethod);

        RadioGroup rgLocoSelect = findViewById(R.id.select_loco_method_address_button_radio_group);
        rgLocoSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.select_loco_method_roster_button) {
                    showMethod(which_method.ROSTER);
                } else if (checkedId == R.id.select_loco_method_recent_button) {
                    showMethod(which_method.RECENT_LOCOS);
                } else if (checkedId == R.id.select_loco_method_address_button) {
                        showMethod(which_method.ADDRESS);
                } else if (checkedId == R.id.select_consists_method_recent_button) {
                        showMethod(which_method.RECENT_CONSISTS);
                } else if (checkedId == R.id.select_loco_method_idngo) {
                    showMethod(which_method.IDNGO);
                }
            }
        });

        if (mainapp.isDCCEX) maxAddr = 10239;  // DCC-EX supports the full range

        setLabels();
        overrideThrottleName = "";

        Handler handler = new Handler();
        handler.postDelayed(showMethodTask, 500);  // show or hide the soft keyboard after a short delay

        LinearLayout screenNameLine = findViewById(R.id.screen_name_line);
        Toolbar toolbar = findViewById(R.id.toolbar);
        LinearLayout statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_select_loco),
                    "" );
        }
    } //end OnCreate

    private final Runnable showMethodTask = new Runnable() {
        public void run() {
            showMethod(prefSelectLocoMethod);
        }
    };

    @SuppressLint("ApplySharedPref")
    private void showMethod(String whichMethod) {
        // hide everything by default then show only what is actually needed
        rlAddress.setVisibility(View.GONE);
        rlAddressHelp.setVisibility(View.GONE);
        rlRosterHeading.setVisibility(GONE);
        rlRosterHeaderGroup.setVisibility(GONE);
        llRoster.setVisibility(GONE);
        rlRosterEmpty.setVisibility(GONE);
        rlRecentHeader.setVisibility(GONE);
        llRecent.setVisibility(GONE);
        rlRecentConsistsHeader.setVisibility(GONE);
        llRecentConsists.setVisibility(GONE);
        rlIDnGo.setVisibility(GONE);

        rbAddress.setChecked(false);
        rbRoster.setChecked(false);
        rbRecent.setChecked(false);
        rbRecentConsists.setChecked(false);
        rbIDnGo.setChecked(false);

        switch (whichMethod) {
            default:
            case which_method.ADDRESS: {
                rlAddress.setVisibility(View.VISIBLE);
                rlAddressHelp.setVisibility(View.VISIBLE);
                rbAddress.setChecked(true);
                hideSoftKeyboard(rlAddress);
                break;
            }
            case which_method.ROSTER: {
                rlRosterHeading.setVisibility(View.VISIBLE);
                rlRosterHeaderGroup.setVisibility(View.VISIBLE);
                llRoster.setVisibility(View.VISIBLE);
                rlRosterEmpty.setVisibility(View.VISIBLE);
                rbRoster.setChecked(true);
                rosterDownloadButton.setEnabled((!mainapp.rosterFullList.isEmpty()) && (mainapp.roster_entries.size()==mainapp.rosterFullList.size()));

                if (!mainapp.shownToastRoster) { // only show it once
                    mainapp.safeToastInstructional(R.string.toastRosterHelp, Toast.LENGTH_LONG);
                    mainapp.shownToastRoster = true;
                }
                hideSoftKeyboard(rbAddress);
                break;
            }
            case which_method.RECENT_LOCOS: {
                rlRecentHeader.setVisibility(View.VISIBLE);
                llRecent.setVisibility(View.VISIBLE);
                rbRecent.setChecked(true);
                mainapp.shownToastRecentLocos = mainapp.safeToastInstructionalShowOnce(R.string.toastRecentsHelp, Toast.LENGTH_LONG, mainapp.shownToastRecentLocos);
                hideSoftKeyboard(rbAddress);
                break;
            }
            case which_method.RECENT_CONSISTS: {
                rlRecentConsistsHeader.setVisibility(View.VISIBLE);
                llRecentConsists.setVisibility(View.VISIBLE);
                rbRecentConsists.setChecked(true);
                mainapp.shownToastRecentConsists = mainapp.safeToastInstructionalShowOnce(R.string.toastRecentConsistsHelp, Toast.LENGTH_LONG, mainapp.shownToastRecentConsists);
                hideSoftKeyboard(rbAddress);
                break;
            }
            case which_method.IDNGO: {
                rlIDnGo.setVisibility(View.VISIBLE);
                rbIDnGo.setChecked(true);
                hideSoftKeyboard(rbAddress);
                break;
            }
        }
        prefSelectLocoMethod = whichMethod;
        prefs.edit().putString("prefSelectLocoMethod", whichMethod).commit();
    }  // enf showMethod()

    //Clears recent connection list of locos
    public void clearList() {
        File engineListFile = new File(context.getExternalFilesDir(null), "recent_engine_list.txt");

        if (engineListFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            engineListFile.delete();
            recentEngineList.clear();
        }
    } // end clearList()

    public void clearConsistsList() {
        File consists_list_file = new File(context.getExternalFilesDir(null), "recent_consist_list.txt");

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
    } // end clearConsistsList()

    @Override
    public void onResume() {
        Log.d("Engine_Driver", "select_loco: onResume():");
        super.onResume();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        // checking address length here covers (future) case where prefs changed while paused
        defaultAddressLength = prefs.getString("default_address_length", this
                .getResources().getString(R.string.prefDefaultAddressLengthDefaultValue));
        updateAddressEntry();   // enable/disable buttons
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (mainapp.supportsIDnGo()) {
            rbIDnGo.setVisibility(View.VISIBLE);
        } else {
            rbIDnGo.setVisibility(GONE);
        }
        if (mainapp.supportsRoster()) {
            rbRoster.setVisibility(View.VISIBLE);
        } else {
            rbRoster.setVisibility(GONE);
        }

        if (SMenu != null) {
            mainapp.displayFlashlightMenuButton(SMenu);
            mainapp.setFlashlightButton(SMenu);
            mainapp.displayPowerStateMenuButton(SMenu);
            mainapp.setPowerStateButton(SMenu);
        }
    }

    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "select_loco: onDestroy(): called");
        super.onDestroy();

        if (mainapp.select_loco_msg_handler != null) {
            mainapp.select_loco_msg_handler.removeCallbacksAndMessages(null);
            mainapp.select_loco_msg_handler = null;
        } else {
            Log.d("Engine_Driver", "select_loco: onDestroy(): mainapp.select_loco_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    } // end onDestroy()

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.select_loco_menu, menu);
        SMenu = menu;
        mainapp.displayEStop(menu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.EmerStop) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlight(this, SMenu);
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(SMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // end current activity
    void endThisActivity() {
        Log.d("Engine_Driver", "select_loco: endThisActivity(): ending select_loco normally");
        Intent resultIntent = new Intent();
        resultIntent.putExtra("whichThrottle", sWhichThrottle.charAt(0));  //pass whichThrottle as an extra
        resultIntent.putExtra("overrideThrottleName", overrideThrottleName);
        setResult(result, resultIntent);
        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void updateAddressEntry() {
        Button ba = findViewById(R.id.acquire_button);
        EditText la = findViewById(R.id.loco_address);
        if (ba == null || la == null) return; //bail if views not found
        String txt = la.getText().toString().trim();
        int txtLen = txt.length();
        int addr = -1;
        if (txtLen > 0) {
            try {
                addr = Integer.parseInt(txt);
            } catch (NumberFormatException e) {
                la.setText(""); //clear the bad entry
            }

            if (addr>maxAddr) {
                addr = -1;
                threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastAddressExceedsMax, txt, Integer.toString(maxAddr)), Toast.LENGTH_LONG);
                la.setText(""); //clear the bad entry
            }
        }

        // don't allow acquire button if nothing entered
        if (addr > -1) {
            ba.setEnabled(true);

            // set address length
            Spinner al = findViewById(R.id.address_length);
            if (defaultAddressLength.equals("Long") ||
                    (defaultAddressLength.equals("Auto") && (addr > 127))) {
                al.setSelection(1);
            } else {
                al.setSelection(0);
            }

        } else {
            ba.setEnabled(false);
        }

    }

    /** @noinspection SameReturnValue*/ // long click handler for the Roster List items.  Shows the details of the enter in a dialog.
    protected boolean onLongRosterListItemClick(int position) {
        RosterEntry re = null;
        HashMap<String, String> hm = rosterList.get(position);
        String rosterNameString = hm.get("roster_name");
        String rosterAddressString = hm.get("roster_address");
        if (mainapp.rosterJmriWeb != null) {
            re = mainapp.rosterJmriWeb.get(rosterNameString);
            if (re == null) {
                Log.w("Engine_Driver", "select_loco: onLongRosterListItemClick(): Roster entry " + rosterNameString + " not available.");
                return true;
            }
        }
        String iconURL = hm.get("roster_icon");
        showRosterDetailsDialog(re, rosterNameString, rosterAddressString, iconURL);
        return true;
    }

    protected void showRosterDetailsDialog(RosterEntry re, String rosterNameString, String rosterAddressString, String iconURL) {
        String res;
        Log.d("Engine_Driver", "select_loco: showRosterDetailsDialog(): Showing details for roster entry " + rosterNameString);
        final Dialog dialog = new Dialog(select_loco.this, mainapp.getSelectedTheme());
        dialog.setTitle(getApplicationContext().getResources().getString(R.string.rosterDetailsDialogTitle) + rosterNameString);
        dialog.setContentView(R.layout.roster_entry);
        if (re != null) {
            res = re.toString();
        } else {
//            res = "\n DCC Address: " + rosterAddressString +"\n Roster Entry: " + rosterNameString + "\n";
            res = "\n" + threaded_application.context.getResources().getString(R.string.rosterDecoderInfoDccAddress) + " " + rosterAddressString
                    +"\n" + threaded_application.context.getResources().getString(R.string.rosterDecoderInfoRosterEntry) + " " +rosterNameString + "\n";
        }
        TextView tv = dialog.findViewById(R.id.rosterEntryText);
        tv.setText(res);

        detailsRosterImageView = dialog.findViewById(R.id.rosterEntryImage);
        detailsRosterImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        loadRosterOrRecentImage(rosterNameString, detailsRosterImageView, iconURL);

        buttonClose = dialog.findViewById(R.id.rosterEntryButtonClose);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (newRosterImageSelected) {
                    writeLocoImageToFile(detailsRosterNameString, detailsRosterImageView);
                }
                if (newRosterImageSelected || LocalRosterImageRemoved ) {
                    rosterListView.invalidateViews();
                }
                dialog.dismiss();
                mainapp.buttonVibration();
            }
        });

        Button buttonSelectRosterImage = dialog.findViewById(R.id.selectRosterEntryImage);
        buttonSelectRosterImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                detailsRosterNameString = rosterNameString; // store the name for the return result
                // Create intent to Open Image applications like Gallery, Google Photos
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // Start the Intent
                startActivityForResult(galleryIntent, ACTIVITY_SELECT_ROSTER_ENTRY_IMAGE);

                mainapp.buttonVibration();
            }
        });

        buttonRemoveRosterImage = dialog.findViewById(R.id.removeRosterEntryImage);
        buttonRemoveRosterImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (deleteLocoImageFile(rosterNameString, detailsRosterImageView)) {
                    detailsRosterImageView.setVisibility(GONE);
                    buttonRemoveRosterImage.setVisibility(GONE);
                    LocalRosterImageRemoved = true;
                    hasLocalRosterImage = false;
                    newRosterImageSelected = false;
                }
                mainapp.buttonVibration();
            }
        });

        if ((iconURL != null) && (!iconURL.isEmpty())) {
            buttonSelectRosterImage.setVisibility(GONE);
            TextView rosterEntryImageHelpText = dialog.findViewById(R.id.rosterEntryImageHelpText);
            rosterEntryImageHelpText.setText(getString(R.string.rosterEntryImageServerImageHelpText));
            buttonRemoveRosterImage.setVisibility(GONE);
        }

        if (!hasLocalRosterImage) {
            buttonRemoveRosterImage.setVisibility(GONE);
        }

        dialog.setCancelable(true);
        dialog.show();
    }

    /** @noinspection SameReturnValue*/ // long click for the recent loco list items.
    protected boolean onLongRecentListItemClick(int position) {
        if (importExportPreferences.recentLocoSourceList.get(position) == source_type.ROSTER) {
            String rosterEntryName = importExportPreferences.recentLocoNameList.get(position);
            Integer rosterEntryAddress = importExportPreferences.recentLocoAddressList.get(position);
            RosterEntry re = null;
            if (mainapp.rosterJmriWeb != null) {
                re = mainapp.rosterJmriWeb.get(rosterEntryName);
            }
            if (re == null) {
                Log.w("Engine_Driver", "select_loco: onLongRecentListItemClick(): Roster entry " + rosterEntryName + " not available.");
                return true;
            }
            showRosterDetailsDialog(re, rosterEntryName, Integer.toString(rosterEntryAddress),"");
        } else {
            showEditRecentsNameDialog(position);
        }
        return true;
    }

    //  Clears the entry from the list
    protected void clearRecentListItem(final int position) {

        importExportPreferences.recentLocoAddressList.remove(position);
        importExportPreferences.recentLocoAddressSizeList.remove(position);
        importExportPreferences.recentLocoNameList.remove(position);
        importExportPreferences.recentLocoSourceList.remove(position);
        importExportPreferences.recentLocoFunctionsList.remove(position);

        removingLocoOrForceReload = true;

        Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        anim.setDuration(500);
        View itemView = recentListView.getChildAt(position - recentListView.getFirstVisiblePosition());
        itemView.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                recentEngineList.remove(position);
                saveRecentLocosList(true);
                recentListView.invalidateViews();
                mainapp.safeToastInstructional(R.string.toastRecentCleared, Toast.LENGTH_SHORT);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    /** @noinspection SameReturnValue*/ // long click for the recent consists list items.  Clears the entry from the list
    protected boolean onLongRecentConsistsListItemClick(int position) {
        showEditRecentConsistsNameDialog(position);
        return true;
    }

    // Clears the entry from the list
    protected void clearRecentConsistsListItem(final int position) {
        View itemView = consists_list_view.getChildAt(position - consists_list_view.getFirstVisiblePosition());

        importExportPreferences.consistEngineAddressList.remove(position);
        importExportPreferences.consistAddressSizeList.remove(position);
        importExportPreferences.consistDirectionList.remove(position);
        importExportPreferences.consistSourceList.remove(position);
        importExportPreferences.consistRosterNameList.remove(position);
        importExportPreferences.consistLightList.remove(position);
        importExportPreferences.consistNameList.remove(position);

        removingConsistOrForceRewrite = true;

        Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        anim.setDuration(500);
        itemView.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                recent_consists_list.remove(position);
                updateRecentConsists(true);
                consists_list_view.invalidateViews();
                mainapp.safeToastInstructional(R.string.toastRecentConsistCleared, Toast.LENGTH_SHORT);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

    }

    public class RosterSimpleAdapter extends SimpleAdapter {
        private final Context cont;

        /** @noinspection SameParameterValue*/
        RosterSimpleAdapter(Context context,
                            List<? extends Map<String, ?>> data, int resource,
                            String[] from, int[] to) {
            super(context, data, resource, from, to);
            cont = context;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < 0 || position >= rosterList.size())
                return convertView;

            HashMap<String, String> hm = rosterList.get(position);
            if (hm == null)
                return convertView;

            LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("InflateParams") RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.roster_list_item, null, false);

            String engineName = hm.get("roster_name");
            if (engineName != null) {
                TextView name = view.findViewById(R.id.roster_name_label);
                name.setText(engineName);
            }

            String engineNo = hm.get("roster_address");
            if (engineNo != null) {
                TextView secondLine = view.findViewById(R.id.roster_address_label);
                secondLine.setText(engineNo);
            }

            ImageView imageView = view.findViewById(R.id.roster_icon_image);
            String iconURL = hm.get("roster_icon");
            loadRosterOrRecentImage(engineName, imageView, iconURL);

            String owner = hm.get("roster_owner");
            if (owner != null) {
                TextView secondLine = view.findViewById(R.id.roster_owner_label);
                secondLine.setText(owner);
            }

            return view;
        }
    }

    public class RecentSimpleAdapter extends SimpleAdapter {
        private final Context cont;

        /** @noinspection SameParameterValue*/
        RecentSimpleAdapter(Context context,
                            List<? extends Map<String, ?>> data, int resource,
                            String[] from, int[] to) {
            super(context, data, resource, from, to);
            cont = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (position > recentEngineList.size())
                return convertView;

            HashMap<String, String> hm = recentEngineList.get(position);
            if (hm == null)
                return convertView;

            LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("InflateParams") RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.engine_list_item, null, false);

            String str = hm.get("engine_name");
            if (str != null) {
                TextView name = view.findViewById(R.id.engine_name_label);
                name.setText(Html.fromHtml(str));
            }

            String engineName = hm.get("engine");
            if (engineName != null) {
                TextView secondLine = view.findViewById(R.id.engine_item_label);
                secondLine.setText(engineName);
            }

            ImageView imageView = view.findViewById(R.id.engine_icon_image);
            String iconURL = hm.get("engine_icon");
            loadRosterOrRecentImage(engineName, imageView, iconURL);

            return view;
        }
    }

    private void loadRosterOrRecentImage(String engineName, ImageView imageView, String iconURL) {
        //see if there is a saved file and preload it, even if it gets written over later
        boolean foundSavedImage = false;
        String imgFileName = mainapp.fixFilename(engineName) + ".png";
        File image_file = new File(getApplicationContext().getExternalFilesDir(null), "/"+RECENT_LOCO_DIR+"/"+imgFileName);
        if (image_file.exists()) {
            try {
                imageView.setImageBitmap(BitmapFactory.decodeFile(image_file.getPath()));
                foundSavedImage = true;
            } catch (Exception e) {
                Log.d("Engine_Driver", "select_loco: loadRosterOrRecentImage(): recent consists - image file found but could not loaded");
            }
        }
        hasLocalRosterImage = foundSavedImage;

        if ((iconURL != null) && (!iconURL.isEmpty())) {
//            mainapp.imageDownloader.download(iconURL, imageView);
            mainapp.imageDownloader.requestImage(iconURL, imageView);
        } else {
            if (!foundSavedImage) {
                imageView.setVisibility(GONE);
            }
        }
    }

    public class RecentConsistsSimpleAdapter extends SimpleAdapter {
        private final Context cont;

        /** @noinspection SameParameterValue*/
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
            @SuppressLint("InflateParams") RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.consists_list_item, null, false);

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
                if (!rslt.isEmpty()) {
                    importExportPreferences.consistNameList.set(pos, rslt);
                    removingConsistOrForceRewrite = true;
                    updateRecentConsists(true);
                    loadRecentConsistsList(true);
                    consists_list_view.invalidateViews();
                }
                mainapp.buttonVibration();
            }
        });
        dialogBuilder.setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
                mainapp.buttonVibration();
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
        edt.setText(importExportPreferences.recentLocoNameList.get(pos));

        dialogBuilder.setTitle(getApplicationContext().getResources().getString(R.string.RecentsNameEditTitle));
        dialogBuilder.setMessage(getApplicationContext().getResources().getString(R.string.RecentsNameEditText));
        dialogBuilder.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String rslt = edt.getText().toString();
                if (!rslt.isEmpty()) {
                    importExportPreferences.recentLocoNameList.set(pos, rslt);
                    removingLocoOrForceReload = true;
                    saveRecentLocosList(true);
                    loadRecentLocosList(true);
                    recentListView.invalidateViews();
                    mainapp.buttonVibration();
                }
            }
        });
        dialogBuilder.setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
                mainapp.buttonVibration();
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        boolean rslt = mainapp.implDispatchGenericMotionEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    // listener for physical keyboard events
    // used to support the gamepad only   DPAD and key events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
//        InputDevice idev = getDevice(event.getDeviceId());
        boolean rslt = mainapp.implDispatchKeyEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    /** @noinspection SameParameterValue*/
    private static Bitmap viewToBitmap(View view, int maxHeight)
    {
        int originalWidth = view.getWidth();
        int originalHeight = view.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        if (maxHeight>0) {
            //scale the image based on th max allowed height
            int scaledHeight = (int) (((double) maxHeight) * context.getResources().getDisplayMetrics().density);  //52dp
            double ratio = ((float) scaledHeight) / originalHeight;
            int scaledWidth = (int) (((double) originalWidth) * ratio);
            return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        } else {
            return Bitmap.createScaledBitmap(bitmap, originalWidth, originalHeight, true);
        }
    }

    /** @noinspection UnusedReturnValue*/
    private boolean writeLocoImageToFile(String rosterNameString, ImageView imageView) {
        try {
            File dir = new File(context.getExternalFilesDir(null), RECENT_LOCO_DIR);
            if (!dir.exists()) dir.mkdir(); // in case the folder does not already exist
            String imgFileName = mainapp.fixFilename(rosterNameString) + ".png";
            File imageFile = new File(context.getExternalFilesDir(null) + "/" + RECENT_LOCO_DIR + "/" + imgFileName);
            if (dir.exists()) imageFile.delete(); // delete the old version if it exists
            FileOutputStream fileOutputStream =
                    new FileOutputStream(context.getExternalFilesDir(null) + "/" + RECENT_LOCO_DIR + "/" + imgFileName);
            Bitmap bitmap = viewToBitmap(imageView, 52);   //52dp
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e)  {
            Log.d("Engine_Driver", "select_loco: writeLocoImageToFile(): Unable to save roster loco image");
            return false;
        }
    }

    private boolean deleteLocoImageFile(String rosterNameString, ImageView imageView) {
        try {
            File dir = new File(context.getExternalFilesDir(null), RECENT_LOCO_DIR);
            if (!dir.exists()) dir.mkdir(); // in case the folder does not already exist
            String imgFileName = mainapp.fixFilename(rosterNameString + ".png");
            File imageFile = new File(context.getExternalFilesDir(null) + "/" + RECENT_LOCO_DIR + "/" + imgFileName);
            if (dir.exists()) imageFile.delete();
            return true;
        } catch (Exception e)  {
            Log.d("Engine_Driver", "select_loco: deleteLocoImageFile(): Unable to delete roster loco image");
            return false;
        }
    }

//    private void cropLocoImage() {
//        int crop = detailsRosterImageView.getHeight() / 10;
//        Bitmap bitmap = viewToBitmap(detailsRosterImageView, 0);
//        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, crop, detailsRosterImageView.getWidth(), detailsRosterImageView.getHeight()-(crop*2) );
//        detailsRosterImageView.setScaleY((float) 10/8);
//
//        detailsRosterImageView.setImageBitmap(croppedBitmap);
//        detailsRosterImageView.invalidate();
//        detailsRosterImageView.setVisibility(VISIBLE);
//        newRosterImageSelected = true;
//        hasLocalRosterImage = true;
//        LocalRosterImageRemoved = false;
//    }

    /** @noinspection CallToPrintStackTrace*/
    // from http://www.java2s.com/example/android/android.graphics/read-bitmap-from-file-and-rotate.html
    static private int getRotateDegreeFromExif(String filePath) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                degree = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                degree = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                degree = 270;
            }
        } catch (IOException e) {
            degree = -1;
            e.printStackTrace();
        }

        return degree;
    }

    public void hideSoftKeyboard(View view) {
        // Check if no view has focus:
        if (view != null) {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } catch (Exception e) {
                Log.e("Engine_Driver", "select_loco: hideSoftKeyboard(): unable to hide the soft keyboard");
            }
        }
    }

    public class OwnersFilterSpinnerListener implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.roster_filter_owner);
            rosterOwnersFilterIndex = spinner.getSelectedItemPosition();
            rosterOwnersFilter = spinner.getSelectedItem().toString();
            spinner.setSelection(rosterOwnersFilterIndex);
            refreshRosterList();
            prefs.edit().putString("prefRosterOwnersFilterSelected", rosterOwnersFilter).commit();
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
}
