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

import static android.view.KeyEvent.KEYCODE_BACK;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.light_follow_type;
import jmri.enginedriver.type.select_loco_method_type;
import jmri.enginedriver.type.sort_type;
import jmri.enginedriver.type.source_type;
import jmri.enginedriver.util.SwipeDetector;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.address_type;

import jmri.enginedriver.util.AdvancedConsistTool;
import jmri.jmrit.roster.RosterEntry;
import jmri.enginedriver.import_export.ImportExportPreferences;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.type.direction_type;
import jmri.enginedriver.type.sub_activity_type;

public class select_loco extends AppCompatActivity {
    static final String activityName = "select_loco";
    static public final int RESULT_LOCO_EDIT = RESULT_FIRST_USER;

    String prefSelectLocoMethod = select_loco_method_type.FIRST;

    private static final String RECENT_LOCO_DIR = "recent_engine_list";

    private ArrayList<HashMap<String, String>> recentLocosList;
    private ArrayList<HashMap<String, String>> rosterList;
    private ArrayList<String> rosterOwnersList;
    String rosterOwnersFilter = "Owner";
    int rosterOwnersFilterIndex = 0;
    private RosterSimpleAdapter rosterListAdapter;
    private RecentSimpleAdapter recentListAdapter;

    public static final int ACTIVITY_DEVICE_SOUNDS_SETTINGS = 5;
    public static final int ACTIVITY_SELECT_ROSTER_ENTRY_IMAGE = 6;

    // recent consists
    private ArrayList<HashMap<String, String>> recentConsistsList;
    private RecentConsistsSimpleAdapter recentConsistsListAdapter;

    public final ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    ListView recentConsistsListView;
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
    private Toolbar toolbar;

    protected final int layoutViewId = R.layout.select_loco;

    private String prefRosterFilter = "";
    EditText filterRosterText;

    RelativeLayout dccAddressLayout;
    TextView dccAddressHelpText;
    TextView rlRosterHeading;
    RelativeLayout rlRosterHeaderGroup;
    TextView rlRosterEmpty;
    LinearLayout rlRecentHeader;
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

    Integer selectLocoMethodIndex;
    String[] selectLocoMethodEntryValuesArray;
    String[] selectLocoMethodEntriesArray;
    Spinner selectLocoMethodSpinner;
    ImageButton[] selectMethodButton = new ImageButton[5];
    LinearLayout[] selectMethodButtonLayout = new LinearLayout[5];
    boolean prefSelectLocoByRadioButtons = false;

    LinearLayout selectLocoMethodButtonsLayout;
    RadioGroup selectLocoMethodRadioButtonsGroup;

    private int maxAddr = 9999;

    // populate the on-screen roster view from global hashmap
    @SuppressLint("DefaultLocale")
    public void refreshRosterList() {
        Log.d(threaded_application.applicationName, activityName + ": refreshRosterList()");
        // clear and rebuild
        rosterList.clear();  // local list. For UI
        mainapp.rosterFullList.clear(); // full global list

        if (((mainapp.roster_entries != null)  // add roster and consist entries if any defined
                && (!mainapp.roster_entries.isEmpty()))
                || ((mainapp.consist_entries != null)
                && (!mainapp.consist_entries.isEmpty()))) {

            //only show this warning once, it will be skipped for each entry below
            if (mainapp.rosterJmriWeb == null) {
                Log.w(threaded_application.applicationName, activityName + ": refreshRosterList(): xml roster not available");
            }

            int position = -1;
            boolean includeInList;

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

                    includeInList = false;
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
                                    Log.d(threaded_application.applicationName, activityName + ": refreshRosterList(): xml roster entry " + rostername + " found, but no icon specified.");
                                }
                            } else {
                                Log.w(threaded_application.applicationName, activityName + ": refreshRosterList(): WiThrottle roster entry " + rostername + " not found in xml roster.");
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
            rosterListView.invalidateViews();

            View v = findViewById(R.id.roster_list_heading);
            if (prefSelectLocoMethod.equals(select_loco_method_type.ROSTER)) v.setVisibility(View.VISIBLE);  // only show it if 'roster' is the currently selected method

            v = findViewById(R.id.filter_roster_text);
            v.setVisibility(View.VISIBLE);

            rosterListView.setVisibility(View.VISIBLE);

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
//        Log.d(threaded_application.applicationName, activityName + ": getLocoIconUrlFromRoster()");
        if (prefRosterRecentLocoNames) {
            if ((mainapp.roster_entries != null) && (!mainapp.roster_entries.isEmpty()) && (mainapp.rosterJmriWeb != null)) {
                for (String rostername : mainapp.roster_entries.keySet()) {  // loop thru roster entries,
                    if (engineName.isEmpty()) {
                        String rosterEntryRosterName = mainapp.roster_entries.get(rostername);
                        if (rosterEntryRosterName == null) return "";

                        if (rosterEntryRosterName.equals(engineAddress)) {
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

    @SuppressLint("ApplySharedPref")
    void checkValidMethod(String whichMethod) {
        Log.d(threaded_application.applicationName, activityName + ": checkValidMethod()");
        prefSelectLocoMethod = whichMethod;

        if (prefSelectLocoMethod.equals(select_loco_method_type.FIRST)) {
            if ((mainapp.rosterJmriWeb != null) && (!mainapp.rosterJmriWeb.isEmpty())) {
                prefSelectLocoMethod = select_loco_method_type.ROSTER;
            } else {
                prefSelectLocoMethod = select_loco_method_type.ADDRESS;
            }
        }

        if (((recentConsistsList.isEmpty()) && (prefSelectLocoMethod.equals(select_loco_method_type.RECENT_CONSISTS)))
                | ((importExportPreferences.recentLocoAddressList.isEmpty()) && (prefSelectLocoMethod.equals(select_loco_method_type.RECENT_LOCOS)))
                | (!mainapp.supportsIDnGo() && prefSelectLocoMethod.equals(select_loco_method_type.IDNGO))) {
            prefSelectLocoMethod = select_loco_method_type.ADDRESS;
        }

        prefs.edit().putString("prefSelectLocoMethod", prefSelectLocoMethod).commit();
    } // end checkValidMethod()

    // lookup and set values of various text labels
    protected void setLabels() {
        Log.d(threaded_application.applicationName, activityName + ": setLabels()");

        refreshRosterList();
        showMethod(prefSelectLocoMethod);

        boolean prefShowAddressInsteadOfName = prefs.getBoolean("prefShowAddressInsteadOfName",
                getResources().getBoolean(R.bool.prefShowAddressInsteadOfNameDefaultValue));

        // format and show currently selected locos, and hide or show Release buttons
        final int conNomTextSize = 16;
        final double minTextScale = 0.8;

        LinearLayout [] currentLocosLayout = new LinearLayout[2];
        currentLocosLayout[0] = findViewById(R.id.current_locos_layout0);
        currentLocosLayout[1] = findViewById(R.id.current_locos_layout1);

        Button releaseButton0 = findViewById(R.id.release_button0);
        releaseButton0.setVisibility(View.GONE);
        ImageButton releaseButton1 = findViewById(R.id.release_button1);
        releaseButton1.setVisibility(View.GONE);

        LinearLayout [] consistOptionsLayout = new LinearLayout[2];
        consistOptionsLayout[0] = findViewById(R.id.currentl_locos_options_layout0);
        consistOptionsLayout[1] = findViewById(R.id.currentl_locos_options_layout1);

        LinearLayout [] llEditConsist = new LinearLayout[2];
        llEditConsist[0] = findViewById(R.id.edit_consist_layout0);
        llEditConsist[1] = findViewById(R.id.edit_consist_layout1);

        TextView tvSelectLocoHeading = findViewById(R.id.select_loco_heading);
        tvSelectLocoHeading.setText(this.getResources().getString(R.string.select_loco_heading).replace("%1$s", Integer.toString(mainapp.throttleCharToInt(sWhichThrottle.charAt(0)) + 1)));
        tvSelectLocoHeading = findViewById(R.id.select_loco_current_locos_heading);
        tvSelectLocoHeading.setText(this.getResources().getString(R.string.select_loco_current_locos_heading).replace("%1$s", Integer.toString(mainapp.throttleCharToInt(sWhichThrottle.charAt(0)) + 1)));

        for (int i = 0; i < 2; i++) {
            currentLocosLayout[i].setVisibility(GONE);
            consistOptionsLayout[i].setVisibility(View.GONE);
            llEditConsist[i].setVisibility(GONE);
        }

        int showIndex = prefSelectLocoByRadioButtons ? 0 : 1;

        if (showIndex == 0 ) releaseButton0.setVisibility(View.VISIBLE);
        else releaseButton1.setVisibility(View.VISIBLE);

        consistOptionsLayout[showIndex].setVisibility(View.VISIBLE);

        if ((mainapp.consists != null) && (mainapp.consists[whichThrottle].isActive())) {
            currentLocosLayout[showIndex].setVisibility(VISIBLE);

            if (mainapp.consists[whichThrottle].size() > 1) {
                llEditConsist[showIndex].setVisibility(View.VISIBLE);
            }
            String vLabel = mainapp.consists[whichThrottle].toString();
            if (prefShowAddressInsteadOfName) { // show the DCC Address instead of the loco name if the preference is set
                vLabel = mainapp.consists[whichThrottle].formatConsistAddr();
            }

            // original release button
            releaseButton0.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);

            // scale text if required to fit the button
            double textScale = 1.0;
            int bWidth = releaseButton0.getWidth();
            double textWidth = releaseButton0.getPaint().measureText(vLabel);

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
            releaseButton0.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

            selectLocoRendered = true;
            releaseButton0.setText(this.getResources().getString(R.string.releaseThrottleLocos).replace("%1$s", vLabel));

            // new release button

            TextView textView = findViewById(R.id.current_locos_text);
            textView.setText(vLabel);

            // the widths are not known at this point in time, so this generally won't work.  see onWindowFocusChanged()
            adjustCurrentLocosListTextViewWidth();

            // both release buttons

            releaseButton0.setEnabled(true);
            releaseButton1.setEnabled(true);


        } else {
            releaseButton0.setEnabled(false);
            releaseButton0.setVisibility(GONE);
            releaseButton1.setEnabled(false);
            releaseButton1.setVisibility(GONE);

            for (int i = 0; i < 2; i++) {
                consistOptionsLayout[i].setVisibility(GONE);
            }
        }

        if (SMenu != null) {
            mainapp.displayEStop(SMenu);
        }

    }

    void adjustCurrentLocosListTextViewWidth() {
        TextView textView = findViewById(R.id.current_locos_text);
        LinearLayout consistButtonlayout = findViewById(R.id.consist_buttons_layout1);
        LinearLayout consistOptionsLayout = findViewById(R.id.currentl_locos_options_layout1);
        int bWidth = consistButtonlayout.getWidth() - consistOptionsLayout.getWidth();
        if (bWidth>0) {
            textView.getLayoutParams().width = bWidth;
            textView.requestLayout();
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
            Log.d(threaded_application.applicationName, activityName + ": handleMessage(): " + msg);

            switch (msg.what) {
                case message_type.RESPONSE:
                    String response_str = msg.obj.toString();
                    Log.d(threaded_application.applicationName, activityName + ": SelectLocoHandler(): RESPONSE - message <--:" + response_str);
                    if (response_str.length() >= 3) {
                        String comA = response_str.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(comA)) {
                            mainapp.setPowerStateActionViewButton(SMenu, findViewById(R.id.powerLayoutButton));
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
                            Log.d(threaded_application.applicationName, activityName + ": SelectLocoHandler(): RESPONSE - ignoring message: " + response_str);
                            break;
                        }
                    }
                    if (!selectLocoRendered)         // call setLabels() if the select loco textViews had not rendered the last time it was called
                        setLabels();
                    break;
                case message_type.WIT_CON_RETRY:
                    Log.d(threaded_application.applicationName, activityName + ": SelectLocoHandler(): WIT_CON_RETRY");
                    witRetry(msg.obj.toString());
                    break;
                case message_type.ROSTER_UPDATE:
                    Log.d(threaded_application.applicationName, activityName + ": SelectLocoHandler(): ROSTER_UPDATE");
                    setLabels();
                    showMethod(prefSelectLocoMethod);
                    break;
                case message_type.REOPEN_THROTTLE:
                    if (threaded_application.currentActivity == activity_id_type.SELECT_LOCO)
                        reopenThrottlePage();
                    break;
                case message_type.WIT_CON_RECONNECT:
                    Log.d(threaded_application.applicationName, activityName + ": SelectLocoHandler(): WIT_CON_RECONNECT");
                    rosterListAdapter.notifyDataSetChanged();
                    setLabels();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                    Log.d(threaded_application.applicationName, activityName + ": SelectLocoHandler(): DISCONNECT");
                    endThisActivity();
                    break;

                case message_type.TERMINATE_ALL_ACTIVITIES_BAR_CONNECTION:
                case message_type.LOW_MEMORY:
                    endThisActivity();
                    break;

                default:
                    break;

            }
        }
    }

    private void witRetry(String s) {
        Log.d(threaded_application.applicationName, activityName + ": witRetry()");
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    // request release of specified throttle
    void releaseLoco(int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": releaseLoco()");
        mainapp.storeThrottleLocosForReleaseDCCEX(whichThrottle);
        mainapp.consists[whichThrottle].release();
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", whichThrottle); // pass 0, 1 or 2 in message
        importExportPreferences.writeThrottlesEnginesListToFile(mainapp, mainapp.numThrottles);
    }

    boolean saveUpdateList;         // save value across ConsistEdit activity 
    boolean newEngine;              // save value across ConsistEdit activity

    boolean acquireLoco(boolean bUpdateList, int numberInConsist) { // if numberInConsist is greater than -1 it is not from the recent consists list
        Log.d(threaded_application.applicationName, activityName + ": acquireLoco()");
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
                Log.d(threaded_application.applicationName, activityName + ": acquireLoco(): consists is null");
            else if (mainapp.consists[whichThrottle] == null)
                Log.d(threaded_application.applicationName, activityName + ": acquireLoco(): consists[" + whichThrottle + "] is null");
            endThisActivity();
            return false;
        }

        Consist consist = mainapp.consists[whichThrottle];

        // if we already have it show message and request it anyway
        if (!consist.isEmpty()) {
            for (int i = 0; i <= consist.size(); i++) {
                if (consist.getLoco(sAddr) != null) {
                    overrideThrottleName = "";
                    threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastLocoAlreadySelected, sAddr), Toast.LENGTH_SHORT);
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, sAddr, whichThrottle);  // send the acquire message anyway
                    return false;
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
                return false;
            }
        }
        Log.d(threaded_application.applicationName, activityName + ": acquireLoco(): sAddr:'" + sAddr +"'");

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
        if ( (prefSelectLocoMethod.equals(select_loco_method_type.RECENT_LOCOS)) && (!mainapp.isDCCEX) ) {
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
        return true;
    }

    void processRosterFunctionString(String responseStr, int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": processRosterFunctionString(): processing function labels for " + mainapp.throttleIntToString(whichThrottle));
        LinkedHashMap<Integer, String> functionLabelsMap = threaded_application.parseFunctionLabels(responseStr);
        mainapp.function_labels[whichThrottle] = functionLabelsMap; //set the appropriate global variable from the temp
    }

    //handle return from ConsistEdit
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case sub_activity_type.CONSIST: // edit consist
                if (newEngine) {
                    saveRecentConsistsList(saveUpdateList);
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
                        Log.d(threaded_application.applicationName, activityName + ": onActivityResult(): Load image failed : " + imgpath);
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
                            if (roughBitmap == null) {
                                imageFileFoundButCannotBeLoaded();
                                return;
                            }
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
                            imageFileFoundButCannotBeLoaded();
                        }
                    }
                }
                break;
        }
    }

    private void imageFileFoundButCannotBeLoaded() {
        Log.d(threaded_application.applicationName, activityName + ": onActivityResult(): load image - image file found but could not loaded");
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
                Log.d(threaded_application.applicationName, activityName + ": saveRecentLocosList(): vLocoName='"+locoName+"', address="+locoAddress+", size="+locoAddressSize);
                Log.d(threaded_application.applicationName, activityName + ": saveRecentLocosList(): sLocoName='"+importExportPreferences.recentLocoNameList.get(i)
                        + "', address=" + importExportPreferences.recentLocoAddressList.get(i)
                        + ", size="+importExportPreferences.recentLocoAddressSizeList.get(i));
                if (locoAddress == importExportPreferences.recentLocoAddressList.get(i)
                        && locoAddressSize == importExportPreferences.recentLocoAddressSizeList.get(i)
                        && locoName.equals(importExportPreferences.recentLocoNameList.get(i))) {

                    keepFunctions = importExportPreferences.recentLocoFunctionsList.get(i);
                    if ( (i==0) && (!keepFunctions.isEmpty()) ) { return; } // if it already at the start of the list, don't do anything

                    importExportPreferences.removeRecentLocoFromList(i);
                    Log.d(threaded_application.applicationName, activityName + ": saveRecentLocosList(): Loco '"+ locoName + "' removed from Recents");
                    break;
                }
            }

            // now append it to the beginning of the list
            importExportPreferences.addRecentLocoToList(0, locoAddress, locoAddressSize, locoName, source_type.ROSTER, keepFunctions);

            Log.d(threaded_application.applicationName, activityName + ": saveRecentLocosList(): Loco '"+ locoName + "' added to Recents");
        }

        importExportPreferences.writeRecentLocosListToFile(prefs);
        importExportPreferences.writeThrottlesEnginesListToFile(mainapp, mainapp.numThrottles);
    }

    private void refreshRecentLocosList(boolean reload) {
        Log.d(threaded_application.applicationName, activityName + ": refreshRecentLocosList()");
        importExportPreferences.recentLocoAddressList = new ArrayList<>();
        importExportPreferences.recentLocoAddressSizeList = new ArrayList<>();
        importExportPreferences.recentLocoNameList = new ArrayList<>();
        importExportPreferences.recentLocoSourceList = new ArrayList<>();
        importExportPreferences.recentLocoFunctionsList = new ArrayList<>();
        if (reload) {
            if (recentLocosList == null) {
                recentLocosList = new ArrayList<>();
            } else {
                recentLocosList.clear();
            }
        }

        rbRecent = findViewById(R.id.select_loco_method_recent_button);

        //if no SD Card present then there is no recent locos list
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent locos list requires SD Card
            TextView v = findViewById(R.id.recent_locos_heading);
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
                hm.put("addressSize", Integer.toString(importExportPreferences.recentLocoAddressSizeList.get(i)));
                hm.put("locoSource", Integer.toString(importExportPreferences.recentLocoSourceList.get(i)));
                hm.put("last_used", Integer.toString(i));
                hm.put("functions", importExportPreferences.recentLocoFunctionsList.get(i));
                recentLocosList.add(hm);
            }

            if (importExportPreferences.recentLocoAddressList.isEmpty()) {  // if the list is empty, hide the radio button
                rbRecent.setVisibility(GONE);
            } else {

                Comparator<HashMap<String, String>> comparator = new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
                        int rslt;
                        String s0 = "";
                        String s1 = "";
                        switch (mainapp.recentLocosOrder) {
                            case sort_type.NAME: {
                                if (arg0.get("engine") != null)
                                    s0 = threaded_application.formatNumberInName(Objects.requireNonNull(arg0.get("engine")).replaceAll("_", " ").toLowerCase());
                                if (arg1.get("engine") != null)
                                    s1 = threaded_application.formatNumberInName(Objects.requireNonNull(arg1.get("engine")).replaceAll("_", " ").toLowerCase());
                                break;
                            }
                            case sort_type.ID: {
                                if (arg0.get("locoAddress") != null)
                                    s0 = threaded_application.formatNumberInName(arg0.get("locoAddress"));
                                if (arg1.get("locoAddress") != null)
                                    s1 = threaded_application.formatNumberInName(arg1.get("locoAddress"));
                                break;
                            }
                            case sort_type.LAST_USED:
                            default: {
                                if (arg0.get("last_used") != null)
                                    s0 = threaded_application.formatNumberInName(arg0.get("last_used"));
                                if (arg1.get("last_used") != null)
                                    s1 = threaded_application.formatNumberInName(arg1.get("last_used"));
                            }
                        }
                        rslt = s0.compareTo(s1);
                        return rslt;
                    }
                };
                Collections.sort(recentLocosList, comparator);

                rbRecent.setVisibility(View.VISIBLE);
            }
        }
        recentListAdapter.notifyDataSetChanged();
        recentListView.invalidateViews();
    }


    private void refreshRecentConsistsList(boolean reload) {
        Log.d(threaded_application.applicationName, activityName + ": refreshRecentConsistsList()");
        RadioButton myRadioButton = findViewById(R.id.select_consists_method_recent_button);

        if (reload) {
            if (recentConsistsList == null) {
                recentConsistsList = new ArrayList<>();
            } else {
                recentLocosList.clear();
            }
        }

        //if no SD Card present then there is no recent consists list
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //alert user that recent locos list requires SD Card
            TextView v = findViewById(R.id.recent_consists_heading);
            v.setText(getString(R.string.sl_recent_engine_notice));
            myRadioButton.setVisibility(GONE); // if the list is empty, hide the radio button
        } else {
            importExportPreferences.loadRecentConsistsListFromFile();
            for (int i = 0; i < importExportPreferences.recentConsistLocoAddressList.size(); i++) {
                HashMap<String, String> hm = new HashMap<>();
                hm.put("consist_name", mainapp.getRosterNameFromAddress(importExportPreferences.recentConsistNameHtmlList.get(i),
                        false));
                hm.put("consist", mainapp.locoAndConsistNamesCleanupHtml(importExportPreferences.recentConsistNameList.get(i)));
                hm.put("last_used", Integer.toString(i));
                recentConsistsList.add(hm);
            }
            if (importExportPreferences.recentConsistLocoAddressList.isEmpty()) {
                myRadioButton.setVisibility(GONE); // if the list is empty, hide the radio button
            } else {
                Comparator<HashMap<String, String>> comparator = new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
                        int rslt;
                        String s0 = "";
                        String s1 = "";
                        switch (mainapp.recentConsistsOrder) {
                            case sort_type.NAME: {
                                if (arg0.get("consist") != null)
                                    s0 = threaded_application.formatNumberInName(Objects.requireNonNull(arg0.get("consist")).replaceAll("_", " ").toLowerCase());
                                if (arg1.get("consist") != null)
                                    s1 = threaded_application.formatNumberInName(Objects.requireNonNull(arg1.get("consist")).replaceAll("_", " ").toLowerCase());
                                break;
                            }
                            case sort_type.ID: {
                                if (arg0.get("consist_name") != null)
                                    s0 = threaded_application.formatNumberInName(arg0.get("consist_name"));
                                if (arg1.get("consist_name") != null)
                                    s1 = threaded_application.formatNumberInName(arg1.get("consist_name"));
                                break;
                            }
                            case sort_type.LAST_USED:
                            default: {
                                if (arg0.get("last_used") != null)
                                    s0 = threaded_application.formatNumberInName(arg0.get("last_used"));
                                if (arg1.get("last_used") != null)
                                    s1 = threaded_application.formatNumberInName(arg1.get("last_used"));
                            }
                        }
                        rslt = s0.compareTo(s1);
                        return rslt;
                    }
                };
                Collections.sort(recentConsistsList, comparator);

                myRadioButton.setVisibility(View.VISIBLE);
            }
        }

        recentConsistsListAdapter.notifyDataSetChanged();
        recentConsistsListView.invalidateViews();
    }

    // save recent consists to file
    void saveRecentConsistsList(boolean bUpdateList) {
        ArrayList<Integer> tempRecentConsistLocoAddressList_inner = new ArrayList<>();
        ArrayList<Integer> tempRecentConsistAddressSizeList_inner = new ArrayList<>();
        ArrayList<Integer> tempRecentConsistDirectionList_inner = new ArrayList<>();
        ArrayList<Integer> tempRecentConsistSourceList_inner = new ArrayList<>();
        ArrayList<String> tempRecentConsistRosterNameList_inner = new ArrayList<>();
        ArrayList<Integer> tempRecentConsistLightList_inner = new ArrayList<>();

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
                tempRecentConsistLocoAddressList_inner.add(l.getIntAddress());
                tempRecentConsistAddressSizeList_inner.add(l.getIntAddressLength());
                String addr = importExportPreferences.locoAddressToString(l.getIntAddress(), l.getIntAddressLength(), true);
                tempRecentConsistDirectionList_inner.add((consist.isBackward(addr) ? direction_type.BACKWARD : direction_type.FORWARD));
                String rosterName = "";
                if (l.getRosterName() != null) {
                    rosterName = l.getRosterName();
                }
                tempRecentConsistSourceList_inner.add(l.getWhichSource());
                tempRecentConsistRosterNameList_inner.add(rosterName);
                tempRecentConsistLightList_inner.add(k == 0 ? light_follow_type.FOLLOW : consist.isLight(addr));   // always set the first loco as 'follow'

                int lastItem = tempRecentConsistLocoAddressList_inner.size() - 1;
                oneConsistHtml.append(importExportPreferences.addOneConsistAddressHtml(
                        tempRecentConsistLocoAddressList_inner.get(lastItem),
                        tempRecentConsistAddressSizeList_inner.get(lastItem)
                ));
            }

            // check if we already have it
            for (int i = 0; i < importExportPreferences.recentConsistLocoAddressList.size(); i++) {
                if (importExportPreferences.recentConsistLocoAddressList.get(i).size() == tempRecentConsistLocoAddressList_inner.size()) {  // if the lists are different sizes don't bother
                    boolean isSame = true;
                    for (int j = 0; j < importExportPreferences.recentConsistLocoAddressList.get(i).size() && isSame; j++) {
                        if ((!importExportPreferences.recentConsistLocoAddressList.get(i).get(j).equals(tempRecentConsistLocoAddressList_inner.get(j)))
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
            if ((!importExportPreferences.recentConsistLocoAddressList.isEmpty())
                    && (importExportPreferences.recentConsistLocoAddressList.get(0).size() == (tempRecentConsistLocoAddressList_inner.size() - 1))) {
                // check of the last added one is the same other then the last extra loco
                for (int j = 0; j < tempRecentConsistLocoAddressList_inner.size() - 1; j++) {
                    if ((!importExportPreferences.recentConsistLocoAddressList.get(0).get(j).equals(tempRecentConsistLocoAddressList_inner.get(j)))) {
                        isBuilding = false;
                    }
                }
                if (isBuilding) {  // remove the first entry
                    importExportPreferences.removeRecentConsistFromListAtPosition(0);
                    whichEntryIsBeingUpdated = -1;
                }
            }

            // now add it
            String consistName = consist.toString();
            if (whichEntryIsBeingUpdated > 0) { //this may already have a custom name
                consistName = importExportPreferences.recentConsistNameList.get(whichEntryIsBeingUpdated - 1);
            }
            importExportPreferences.addRecentConsistToList(0,consistName,
                    tempRecentConsistLocoAddressList_inner,
                    tempRecentConsistAddressSizeList_inner,
                    tempRecentConsistDirectionList_inner,
                    tempRecentConsistSourceList_inner,
                    tempRecentConsistRosterNameList_inner,
                    tempRecentConsistLightList_inner);

        }
        importExportPreferences.writeRecentConsistsListToFile(prefs, whichEntryIsBeingUpdated);
    }


    // listener for the Acquire button when entering a DCC Address
    public class AcquireButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            Log.d(threaded_application.applicationName, activityName + ": AcquireButtonListener(): onClick()");
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

            locoSource = source_type.ADDRESS;

            String keepsWhichThrottle = sWhichThrottle;
            sWhichThrottle += locoName;
            if (!acquireLoco(true, -1)) {
                sWhichThrottle = keepsWhichThrottle;
            }

            mainapp.hideSoftKeyboard(v, activityName);
            mainapp.buttonVibration();
        }
    }

    // listener for the ID 'n' Go button, when clicked, send the id request and return to Throttle
    public class IdngoButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            Log.d(threaded_application.applicationName, activityName + ": IdngoButtonListener(): onClick()");
            Consist consist = mainapp.consists[whichThrottle];
            consist.setWaitingOnID(true);
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_LOCO_ADDR, "*", whichThrottle);
            result = RESULT_OK;
            mainapp.hideSoftKeyboard(v, activityName);
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
            Log.d(threaded_application.applicationName, activityName + ": ReleaseButtonListener(): onClick()");
            mainapp.buttonVibration();

            releaseLoco(_throttle);
            overrideThrottleName = "";
            mainapp.hideSoftKeyboard(v, activityName);
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
            Log.d(threaded_application.applicationName, activityName + ": EditConsistButtonListener(): onClick()");
            Intent consistEdit = new Intent().setClass(_selectLocoActivity, ConsistEdit.class);
            consistEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));
            consistEdit.putExtra("saveConsistsFile", 'Y');

            mainapp.hideSoftKeyboard(v, activityName);
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
            Log.d(threaded_application.applicationName, activityName + ": EditConsistLightsButtonListener(): onClick()");
            Intent consistLightsEdit = new Intent().setClass(_selectLocoActivity, ConsistLightsEdit.class);
            consistLightsEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));

            mainapp.hideSoftKeyboard(v, activityName);
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
                    Log.d(threaded_application.applicationName, activityName + ": DownloadRosterButtonListener(): onClick()");
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
            refreshRecentLocosList(true);
            mainapp.toastSortType(mainapp.recentLocosOrder);
            mainapp.buttonVibration();
        }
    }

    public class SortRecentConsistsButtonListener implements View.OnClickListener {
        SortRecentConsistsButtonListener() {}

        public void onClick(View v) {
            Log.d(threaded_application.applicationName, activityName + ": SortRecentConsistsButtonListener(): onClick()");
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
            refreshRecentConsistsList(true);
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
            Log.d(threaded_application.applicationName, activityName + ": DeviceSoundsButtonListener(): onClick()");
            Intent deviceSounds = new Intent().setClass(_selectLocoActivity, device_sounds_settings.class);
            startActivityForResult(deviceSounds, ACTIVITY_DEVICE_SOUNDS_SETTINGS);
            connection_activity.overridePendingTransition(_selectLocoActivity, R.anim.fade_in, R.anim.fade_out);
            result = RESULT_OK;
            mainapp.hideSoftKeyboard(v, activityName);
            endThisActivity();
            mainapp.buttonVibration();
        }
    }

    // onClick for the Recent Locos list items
    public class RecentLocosItemListener implements AdapterView.OnItemClickListener {
        // When an item is clicked, acquire that engine.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            Log.d(threaded_application.applicationName, activityName + ": RecentLocosItemListener(): onItemClick()");
            if (position >= recentLocosList.size()) return;

            if (recentsSwipeDetector.swipeDetected()) {
                if (recentsSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearRecentLocosListItem(position);
                }
            } else {  //no swipe
                if (mainapp.consists==null) return; // attempt to catch NPEs

                HashMap<String, String> hm = recentLocosList.get(position);
                locoAddress = Integer.parseInt(Objects.requireNonNull(hm.get("locoAddress")));
                locoAddressSize = Integer.parseInt(Objects.requireNonNull(hm.get("addressSize")));
                locoSource = Integer.parseInt(Objects.requireNonNull(hm.get("locoSource")));
                locoName = Objects.requireNonNull(hm.get("engine"));
                String functions = Objects.requireNonNull(hm.get("functions"));
                if (locoSource == source_type.UNKNOWN) {
                    locoName = mainapp.getRosterNameFromAddress(importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, false), true);
                }

                String keepsWhichThrottle = sWhichThrottle;
                sWhichThrottle += locoName;
                if (acquireLoco(true, -1)) {
                    if (!functions.isEmpty()) {
                        String addrStr = ((locoAddressSize == 0) ? "S" : "L") + locoAddress;
                        String lead = mainapp.consists[whichThrottle].getLeadAddr();
                        if (lead.equals(addrStr)) {                        //*** temp - only process if for lead engine in consist
                            LinkedHashMap<Integer, String> functionLabelsMap = threaded_application.parseFunctionLabels("RF29}|{1234(L)]\\[" + functions);  //prepend some stuff to match old-style
                            mainapp.function_labels[whichThrottle] = functionLabelsMap;
//                            mainapp.consists[whichThrottle].getLoco(lead).setIsServerSuppliedFunctionlabels(true);
                        }
                    } else {
                        mainapp.function_labels[whichThrottle] = new LinkedHashMap<>(mainapp.function_labels_default);
                    }
                } else {
                    sWhichThrottle = keepsWhichThrottle;
                }

                mainapp.buttonVibration();
            }
        }
    }

    // onClick for the Recent Consists list items
    public class RecentConsistItemListener implements AdapterView.OnItemClickListener {
        // When an item is clicked, acquire that consist.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            Log.d(threaded_application.applicationName, activityName + ": RecentConsistItemListener(): onItemClick()");
            if (position >= recentConsistsList.size()) return;

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

                HashMap<String, String> hm = recentConsistsList.get(position);
                int actualPostion = Integer.parseInt(Objects.requireNonNull(hm.get("last_used")));

                overrideThrottleName = importExportPreferences.recentConsistNameList.get(actualPostion);

                for (int i = 0; i < importExportPreferences.recentConsistLocoAddressList.get(actualPostion).size(); i++) {

                    locoAddress = importExportPreferences.recentConsistLocoAddressList.get(actualPostion).get(i);
                    locoAddressSize = importExportPreferences.recentConsistAddressSizeList.get(actualPostion).get(i);
                    sAddr = importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, true);
                    locoSource = importExportPreferences.recentConsistSourceList.get(actualPostion).get(i);
                    locoName = mainapp.getRosterNameFromAddress(importExportPreferences.locoAddressToString(locoAddress, locoAddressSize, false), false);
                    if ((locoSource != source_type.ADDRESS) && (!importExportPreferences.recentConsistRosterNameList.get(actualPostion).get(i).isEmpty())) {
                        locoName = importExportPreferences.recentConsistRosterNameList.get(actualPostion).get(i);
                    }
                    sWhichThrottle = tempsWhichThrottle
                            + locoName;

                    acquireLoco(true, i);

                    Consist consist = mainapp.consists[whichThrottle];

                    dir = importExportPreferences.recentConsistDirectionList.get(actualPostion).get(i);
                    if (dir == direction_type.BACKWARD) {
                        consist.setBackward(sAddr, true);
                    }

                    if ((i == 0)) {  //lead loco. get the functions from the recent locos list
                        String functions = importExportPreferences.findRecentLocoFunctions(locoAddress, locoAddressSize, locoName);
                        if (!functions.isEmpty()) {
                            LinkedHashMap<Integer, String> functionLabelsMap = threaded_application.parseFunctionLabels("RF29}|{1234(L)]\\[" + functions);  //prepend some stuff to match old-style
                            mainapp.function_labels[whichThrottle] = functionLabelsMap;
                        }
                    } else {
                        mainapp.function_labels[whichThrottle] = new LinkedHashMap<>(mainapp.function_labels_default);
                    }

                    light = importExportPreferences.recentConsistLightList.get(actualPostion).get(i);
                    if (light != light_follow_type.UNKNOWN) {
                        consist.setLight(sAddr, light);
                    }

                }
                saveRecentConsistsList(saveUpdateList);

                result = RESULT_LOCO_EDIT;
                mainapp.buttonVibration();
                mainapp.hideSoftKeyboard(v, activityName);
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
                    Log.d(threaded_application.applicationName, activityName + ": ClearLocoListButtonListner(): onClick()");
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
                    Log.d(threaded_application.applicationName, activityName + ": ClearConsistsListButtonListener(): onClick()");
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
            Log.d(threaded_application.applicationName, activityName + ": RosterItemClickListener(): onItemClick()");
            //use clicked position in list to retrieve roster item object from roster_list
            HashMap<String, String> hm = rosterList.get(position);
            String rosterNameString = hm.get("roster_name");
            String rosterAddressString = hm.get("roster_address");
            String rosterEntryType = hm.get("roster_entry_type");

            String rosterEntryIcon = hm.get("roster_icon");
            if (rosterEntryIcon != null) {
//                String imgFileName = "";
//                String imgFileName = "";
                ViewGroup vg = (ViewGroup) v;
                if (vg!=null) {
                    ImageView iv = (ImageView) vg.getChildAt(0);
                    if (iv != null) {
                        writeLocoImageToFile(rosterNameString, iv);
                    }
                }
            }
//            String rosterEntryOwner = hm.get("roster_owner");

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
                String keepsWhichThrottle = sWhichThrottle;
                if ("loco".equals(rosterEntryType)) {
                    locoName = rosterNameString;
                    sWhichThrottle += rosterNameString;     //append rostername if type is loco (not consist)
                }
                locoSource = source_type.ROSTER;

                boolean bRosterRecent = prefs.getBoolean("roster_recent_locos_preference",
                        getResources().getBoolean(R.bool.prefRosterRecentLocosDefaultValue));

                overrideThrottleName = rosterNameString;
                if (!acquireLoco(bRosterRecent, -1)) {
                    sWhichThrottle = keepsWhichThrottle;
                }
                mainapp.hideSoftKeyboard(v, activityName);
                mainapp.buttonVibration();
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    private void filterRoster() {
        Log.d(threaded_application.applicationName, activityName + ": filterRoster()");
        prefRosterFilter = filterRosterText.getText().toString().trim();
        prefs.edit().putString("prefRosterFilter", prefRosterFilter).commit();
        refreshRosterList();
        //        onCreate(null);
    }

    // Handle pressing of the back button to simply return to caller
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        Log.d(threaded_application.applicationName, activityName + ": onKeyDown()");
        mainapp.exitDoubleBackButtonInitiated = 0;
        if (key == KEYCODE_BACK) {
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
        Log.d(threaded_application.applicationName, activityName + ": onCreate()");

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
        recentLocosList = new ArrayList<>();
        recentListAdapter = new RecentSimpleAdapter(this, recentLocosList, R.layout.engine_list_item,
                new String[]{"engine"},
                new int[]{R.id.engine_item_label, R.id.engine_icon_image});
        recentListView = findViewById(R.id.recentLocoListView);
        recentListView.setAdapter(recentListAdapter);
        recentListView.setOnTouchListener(recentsSwipeDetector = new SwipeDetector());
        recentListView.setOnItemClickListener(new RecentLocosItemListener());
        recentListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongRecentListItemClick(pos);
            }
        });
        refreshRecentLocosList(false);

        // Set up a list adapter to allow adding the list of recent consists to the UI.
        recentConsistsList = new ArrayList<>();
        recentConsistsListAdapter = new RecentConsistsSimpleAdapter(this, recentConsistsList,
                R.layout.consists_list_item, new String[]{"consist"},
                new int[]{R.id.consist_item_label});
        recentConsistsListView = findViewById(R.id.consists_list);
        recentConsistsListView.setAdapter(recentConsistsListAdapter);
        recentConsistsListView.setOnTouchListener(recentConsistsSwipeDetector = new SwipeDetector());
        recentConsistsListView.setOnItemClickListener(new RecentConsistItemListener());
        recentConsistsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongRecentConsistsListItemClick(pos);
            }
        });
        refreshRecentConsistsList(false);

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
            if (sWhichThrottle != null)
                whichThrottle = mainapp.throttleCharToInt(sWhichThrottle.charAt(0));
        }

        button = findViewById(R.id.release_button0);
        button.setOnClickListener(new ReleaseButtonListener(whichThrottle));
        ImageButton imageButton = findViewById(R.id.release_button1);
        imageButton.setOnClickListener(new ReleaseButtonListener(whichThrottle));

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
                    mainapp.hideSoftKeyboard(v, activityName);
                    return true;
                } else
                    return false;
            }
        });

        // consist edit button
        button = findViewById(R.id.edit_consist_button0);
        button.setOnClickListener(new EditConsistButtonListener(whichThrottle, this));
        imageButton = findViewById(R.id.edit_consist_button1);
        imageButton.setOnClickListener(new EditConsistButtonListener(whichThrottle, this));

        // consist lights edit button
        button = findViewById(R.id.edit_consist_lights_button0);
        button.setOnClickListener(new EditConsistLightsButtonListener(whichThrottle, this));
        imageButton = findViewById(R.id.edit_consist_lights_button1);
        imageButton.setOnClickListener(new EditConsistLightsButtonListener(whichThrottle, this));

        // loco sounds button
        button = findViewById(R.id.device_sounds_button0);
        button.setOnClickListener(new DeviceSoundsButtonListener(this));
        imageButton = findViewById(R.id.device_sounds_button1);
        imageButton.setOnClickListener(new DeviceSoundsButtonListener(this));

        rbAddress = findViewById(R.id.select_loco_method_address_button);
        rbRoster = findViewById(R.id.select_loco_method_roster_button);
        rbRecentConsists = findViewById(R.id.select_consists_method_recent_button);
        rbIDnGo = findViewById(R.id.select_loco_method_idngo);

//*********  Buttons and Spinner

        selectMethodButtonLayout[0] = findViewById(R.id.select_loco_glyph_dcc_address_layout);
        selectMethodButton[0] = findViewById(R.id.select_loco_glyph_dcc_address_button);
        SelectMethodButtonListener selectMethodButtonListener = new SelectMethodButtonListener(select_loco_method_type.ADDRESS);
        selectMethodButton[0].setOnClickListener(selectMethodButtonListener);

        selectMethodButtonLayout[1] = findViewById(R.id.select_loco_glyph_roster_layout);
        selectMethodButton[1] = findViewById(R.id.select_loco_glyph_roster_button);
        selectMethodButtonListener = new SelectMethodButtonListener(select_loco_method_type.ROSTER);
        selectMethodButton[1].setOnClickListener(selectMethodButtonListener);

        selectMethodButtonLayout[2] = findViewById(R.id.select_loco_glyph_recent_loco_layout);
        selectMethodButton[2] = findViewById(R.id.select_loco_glyph_recent_loco_button);
        selectMethodButtonListener = new SelectMethodButtonListener(select_loco_method_type.RECENT_LOCOS);
        selectMethodButton[2].setOnClickListener(selectMethodButtonListener);

        selectMethodButtonLayout[3] = findViewById(R.id.select_loco_glyph_recent_consist_layout);
        selectMethodButton[3] = findViewById(R.id.select_loco_glyph_recent_consist_button);
        selectMethodButtonListener = new SelectMethodButtonListener(select_loco_method_type.RECENT_CONSISTS);
        selectMethodButton[3].setOnClickListener(selectMethodButtonListener);

        selectMethodButtonLayout[4] = findViewById(R.id.select_loco_glyph_idngo_layout);
        selectMethodButton[4] = findViewById(R.id.select_loco_glyph_idngo_button);
        selectMethodButtonListener = new SelectMethodButtonListener(select_loco_method_type.IDNGO);
        selectMethodButton[4].setOnClickListener(selectMethodButtonListener);


        selectLocoMethodEntryValuesArray = this.getResources().getStringArray(R.array.selectLocoMethodEntryValues);
        selectLocoMethodEntriesArray = this.getResources().getStringArray(R.array.selectLocoMethodEntries); // display version

        selectLocoMethodIndex = 0;
        selectLocoMethodSpinner = findViewById(R.id.select_loco_method_spinner);
        spinner_adapter = ArrayAdapter.createFromResource(this, R.array.selectLocoMethodEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectLocoMethodSpinner.setAdapter(spinner_adapter);
        selectLocoMethodSpinner.setOnItemSelectedListener(new SelectLocoMethodIndexSpinnerListener());
        selectLocoMethodSpinner.setSelection(selectLocoMethodIndex);

        selectLocoMethodButtonsLayout = findViewById(R.id.select_loco_method_buttons_layout);
        selectLocoMethodRadioButtonsGroup = findViewById(R.id.select_loco_method_radio_button_group);

//*********

        dccAddressLayout = findViewById(R.id.dcc_address_layout);
        dccAddressHelpText = findViewById(R.id.dcc_address_help_text);

        rlRosterHeading = findViewById(R.id.roster_list_heading);
        rlRosterHeaderGroup = findViewById(R.id.roster_list_header_group);
//        rlRosterEmpty = findViewById(R.id.roster_list_empty_group);
        rlRosterEmpty = findViewById(R.id.roster_list_empty);
        rlRecentHeader = findViewById(R.id.recent_locos_list_header_group);
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

        selectLocoMethodRadioButtonsGroup = findViewById(R.id.select_loco_method_radio_button_group);
        selectLocoMethodRadioButtonsGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (prefSelectLocoByRadioButtons) { // otherwise ignore any cheges to these
                    if (checkedId == R.id.select_loco_method_roster_button) {
                        showMethod(select_loco_method_type.ROSTER);
                    } else if (checkedId == R.id.select_loco_method_recent_button) {
                        showMethod(select_loco_method_type.RECENT_LOCOS);
                    } else if (checkedId == R.id.select_loco_method_address_button) {
                        showMethod(select_loco_method_type.ADDRESS);
                    } else if (checkedId == R.id.select_consists_method_recent_button) {
                        showMethod(select_loco_method_type.RECENT_CONSISTS);
                    } else if (checkedId == R.id.select_loco_method_idngo) {
                        showMethod(select_loco_method_type.IDNGO);
                    }
                }
            }
        });

        prefSelectLocoByRadioButtons = prefs.getBoolean("prefSelectLocoByRadioButtons", getResources().getBoolean(R.bool.prefSelectLocoByRadioButtonsDefaultValue));
        prefSelectLocoMethod = prefs.getString("prefSelectLocoMethod", select_loco_method_type.FIRST);
        showMethod(prefSelectLocoMethod);

        if (mainapp.isDCCEX) maxAddr = 10239;  // DCC-EX supports the full range

        setLabels();
        overrideThrottleName = "";

        Handler handler = new Handler();
        handler.postDelayed(showMethodTask, 500);  // show or hide the soft keyboard after a short delay

        LinearLayout screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        LinearLayout statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_select_loco),
                    "" );
        }
        Log.d(threaded_application.applicationName, activityName + ": onCreate() end");
    } //end OnCreate

    private final Runnable showMethodTask = new Runnable() {
        public void run() {
            showMethod(prefSelectLocoMethod);
        }
    };

    @SuppressLint("ApplySharedPref")
    private void showMethod(String whichMethod) {
        Log.d(threaded_application.applicationName, activityName + ": showMethod()");
        checkValidMethod(whichMethod);

        selectLocoMethodRadioButtonsGroup.setVisibility(prefSelectLocoByRadioButtons ? VISIBLE : GONE);
        selectLocoMethodButtonsLayout.setVisibility(prefSelectLocoByRadioButtons ? GONE : VISIBLE);
//        selectLocoMethodSpinner.setVisibility(prefSelectLocoByRadioButtons ? GONE : VISIBLE);


        if (mainapp.supportsIDnGo()) {
            rbIDnGo.setVisibility(View.VISIBLE);
            selectMethodButtonLayout[Integer.parseInt(select_loco_method_type.IDNGO)-1].setVisibility(View.VISIBLE);
        } else {
            rbIDnGo.setVisibility(GONE);
            selectMethodButtonLayout[Integer.parseInt(select_loco_method_type.IDNGO)-1].setVisibility(View.GONE);
        }

        if (mainapp.supportsRoster()) {
            rbRoster.setVisibility(View.VISIBLE);
            selectMethodButtonLayout[Integer.parseInt(select_loco_method_type.ROSTER)-1].setVisibility(View.VISIBLE);
        } else {
            rbRoster.setVisibility(GONE);
            selectMethodButtonLayout[Integer.parseInt(select_loco_method_type.ROSTER)-1].setVisibility(View.GONE);
        }

        selectMethodButtonLayout[Integer.parseInt(select_loco_method_type.RECENT_LOCOS)-1].setVisibility(!importExportPreferences.recentLocoAddressList.isEmpty() ? View.VISIBLE : View.GONE);
        selectMethodButtonLayout[Integer.parseInt(select_loco_method_type.RECENT_CONSISTS)-1].setVisibility(!recentConsistsList.isEmpty() ? View.VISIBLE : View.GONE);

        // hide everything by default then show only what is actually needed
        dccAddressLayout.setVisibility(View.GONE);
        dccAddressHelpText.setVisibility(View.GONE);
        rlRosterHeading.setVisibility(GONE);
        rlRosterHeaderGroup.setVisibility(GONE);
        rosterListView.setVisibility(GONE);
        rlRosterEmpty.setVisibility(GONE);
        rlRecentHeader.setVisibility(GONE);
        recentListView.setVisibility(GONE);
        rlRecentConsistsHeader.setVisibility(GONE);
        llRecentConsists.setVisibility(GONE);
        rlIDnGo.setVisibility(GONE);

        rbAddress.setChecked(false);
        rbRoster.setChecked(false);
        rbRecent.setChecked(false);
        rbRecentConsists.setChecked(false);
        rbIDnGo.setChecked(false);

        for (int i=0; i<5; i++) {
            selectMethodButton[i].setSelected(false);
        }

        switch (prefSelectLocoMethod) {
            case select_loco_method_type.ROSTER: {
                rlRosterHeading.setVisibility(View.VISIBLE);
                rlRosterHeaderGroup.setVisibility(!mainapp.rosterFullList.isEmpty() ? VISIBLE : View.GONE);
                rosterListView.setVisibility(View.VISIBLE);
                rlRosterEmpty.setVisibility(!mainapp.rosterFullList.isEmpty() ? View.GONE : View.VISIBLE);
                rbRoster.setChecked(true);
                selectMethodButton[1].setSelected(true);
                rosterDownloadButton.setEnabled((!mainapp.rosterFullList.isEmpty()) && (mainapp.roster_entries.size()==mainapp.rosterFullList.size()));

                if (!mainapp.shownToastRoster) { // only show it once
                    mainapp.safeToastInstructional(R.string.toastRosterHelp, Toast.LENGTH_LONG);
                    mainapp.shownToastRoster = true;
                }
                mainapp.hideSoftKeyboard(rbAddress, activityName);
                break;
            }
            case select_loco_method_type.RECENT_LOCOS: {
                rlRecentHeader.setVisibility(View.VISIBLE);
                recentListView.setVisibility(View.VISIBLE);
                rbRecent.setChecked(true);
                selectMethodButton[2].setSelected(true);
                mainapp.shownToastRecentLocos = mainapp.safeToastInstructionalShowOnce(R.string.toastRecentsHelp, Toast.LENGTH_LONG, mainapp.shownToastRecentLocos);
                mainapp.hideSoftKeyboard(rbAddress, activityName);
                break;
            }
            case select_loco_method_type.RECENT_CONSISTS: {
                rlRecentConsistsHeader.setVisibility(View.VISIBLE);
                llRecentConsists.setVisibility(View.VISIBLE);
                rbRecentConsists.setChecked(true);
                selectMethodButton[3].setSelected(true);
                mainapp.shownToastRecentConsists = mainapp.safeToastInstructionalShowOnce(R.string.toastRecentConsistsHelp, Toast.LENGTH_LONG, mainapp.shownToastRecentConsists);
                mainapp.hideSoftKeyboard(rbAddress, activityName);
                break;
            }
            case select_loco_method_type.IDNGO: {
                rlIDnGo.setVisibility(View.VISIBLE);
                rbIDnGo.setChecked(true);
                selectMethodButton[4].setSelected(true);
                mainapp.hideSoftKeyboard(rbAddress, activityName);
                break;
            }
            case select_loco_method_type.ADDRESS:
            default: {
                dccAddressLayout.setVisibility(View.VISIBLE);
                dccAddressHelpText.setVisibility(View.VISIBLE);
                rbAddress.setChecked(true);
                selectMethodButton[0].setSelected(true);
                mainapp.hideSoftKeyboard(dccAddressLayout, activityName);
                break;
            }
        }

        if (selectLocoMethodSpinner != null) { // may not be found yet
            selectLocoMethodIndex = Integer.parseInt(prefSelectLocoMethod)-1;
            selectLocoMethodSpinner.setSelection(selectLocoMethodIndex);
        }

    }  // end showMethod()

    //Clears recent connection list of locos
    public void clearList() {
        Log.d(threaded_application.applicationName, activityName + ": clearList()");
        File engineListFile = new File(context.getExternalFilesDir(null), "recent_engine_list.txt");

        if (engineListFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            engineListFile.delete();
            recentLocosList.clear();
        }
    } // end clearList()

    public void clearConsistsList() {
        Log.d(threaded_application.applicationName, activityName + ": clearConsistsList()");
        File consists_list_file = new File(context.getExternalFilesDir(null), "recent_consist_list.txt");

        if (consists_list_file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            consists_list_file.delete();
            recentConsistsList.clear();
            importExportPreferences.recentConsistLocoAddressList.clear();
            importExportPreferences.recentConsistAddressSizeList.clear();
            importExportPreferences.recentConsistDirectionList.clear();
            importExportPreferences.recentConsistSourceList.clear();
            importExportPreferences.recentConsistRosterNameList.clear();
            importExportPreferences.recentConsistLightList.clear();
        }
    } // end clearConsistsList()

    @Override
    public void onPause() {
        Log.d(threaded_application.applicationName, activityName + ": onPause():");
        super.onPause();
        threaded_application.activityPaused(activityName);
    }

    @Override
    public void onResume() {
        Log.d(threaded_application.applicationName, activityName + ": onResume():");
        super.onResume();
        threaded_application.activityResumed(activityName);
        mainapp.removeNotification(this.getIntent());

        threaded_application.currentActivity = activity_id_type.SELECT_LOCO;
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

        prefSelectLocoByRadioButtons = prefs.getBoolean("prefSelectLocoByRadioButtons", getResources().getBoolean(R.bool.prefSelectLocoByRadioButtonsDefaultValue));

        if (SMenu != null) {
            mainapp.displayFlashlightMenuButton(SMenu);
            mainapp.setFlashlightActionViewButton(SMenu, findViewById(R.id.flashlight_button));
            mainapp.displayPowerStateMenuButton(SMenu);
            mainapp.setPowerStateActionViewButton(SMenu, findViewById(R.id.powerLayoutButton));
        }
        Log.d(threaded_application.applicationName, activityName + ": onResume(): end");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d(threaded_application.applicationName, activityName + ": onWindowFocusChanged():");
        super.onWindowFocusChanged(hasFocus);

        // adjust the current locos list if needed
        // the widths are generally not known until this point in time
        adjustCurrentLocosListTextViewWidth();
    }

    @Override
    public void onDestroy() {
        Log.d(threaded_application.applicationName, activityName + ": onDestroy(): called");
        super.onDestroy();

        if (mainapp.select_loco_msg_handler != null) {
            mainapp.select_loco_msg_handler.removeCallbacksAndMessages(null);
            mainapp.select_loco_msg_handler = null;
        } else {
            Log.d(threaded_application.applicationName, activityName + ": onDestroy(): mainapp.select_loco_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    } // end onDestroy()

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.d(threaded_application.applicationName, activityName + ": onCreateOptionsMenu()");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.select_loco_menu, menu);
        SMenu = menu;
        mainapp.displayEStop(menu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightActionViewButton(menu, findViewById(R.id.flashlight_button));
        mainapp.displayPowerStateMenuButton(menu);
//        mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        if (findViewById(R.id.powerLayoutButton) == null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
                }
            }, 100);
        } else {
            mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        }

        MenuItem mi = menu.findItem(R.id.advancedConsistButton);
        mi.setVisible(!mainapp.isDCCEX);

        adjustToolbarSize(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(threaded_application.applicationName, activityName + ": onOptionsItemSelected():");
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.EmerStop) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;

        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlightActionView(this, SMenu, findViewById(R.id.flashlight_button));
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

        } else if (item.getItemId() == R.id.advancedConsistButton) {
            AdvancedConsistTool advancedConsistToolDialogFragment = AdvancedConsistTool.newInstance();
            advancedConsistToolDialogFragment.show(getSupportFragmentManager(), "AdvancedConsistDialogFragment");
            mainapp.buttonVibration();
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // end current activity
    void endThisActivity() {
        threaded_application.activityInTransition(activityName);
        Log.d(threaded_application.applicationName, activityName + ": endThisActivity(): ending select_loco normally");
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
//        Log.d(threaded_application.applicationName, activityName + ": showRosterDetailsDialog(): Showing details for roster entry " + rosterNameString);
        String res;
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
                if (deleteLocoImageFile(rosterNameString)) {
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
                Log.w(threaded_application.applicationName, activityName + ": onLongRecentListItemClick(): Roster entry " + rosterEntryName + " not available.");
                return true;
            }
            showRosterDetailsDialog(re, rosterEntryName, Integer.toString(rosterEntryAddress),"");
        } else {
            showEditRecentsNameDialog(position);
        }
        return true;
    }

    //  Clears the entry from the list
    protected void clearRecentLocosListItem(final int onScreenPosition) {
//        Log.d(threaded_application.applicationName, activityName + ": clearRecentLocosListItem()");
        if (onScreenPosition >= recentLocosList.size()) return;

        HashMap<String, String> hm = recentLocosList.get(onScreenPosition);
        int postionInFullList = Integer.parseInt(Objects.requireNonNull(hm.get("last_used")));
        if (postionInFullList >= importExportPreferences.recentLocoAddressList.size()) return;

        importExportPreferences.removeRecentLocoFromList(postionInFullList);

        removingLocoOrForceReload = true;

        Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        anim.setDuration(500);
        View itemView = recentListView.getChildAt(onScreenPosition - recentListView.getFirstVisiblePosition());
        itemView.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                recentLocosList.remove(onScreenPosition);
                saveRecentLocosList(true);
                refreshRecentLocosList(true);
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
    protected void clearRecentConsistsListItem(final int onScreenPosition) {
//        Log.d(threaded_application.applicationName, activityName + ": clearRecentConsistsListItem()");
        if (onScreenPosition >= recentConsistsList.size()) return;

        HashMap<String, String> hm = recentConsistsList.get(onScreenPosition);
        int postionInFullList = Integer.parseInt(Objects.requireNonNull(hm.get("last_used")));
        if (postionInFullList >= importExportPreferences.recentConsistLocoAddressList.size()) return;

        importExportPreferences.removeRecentConsistFromListAtPosition(postionInFullList);
        removingConsistOrForceRewrite = true;

        Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        anim.setDuration(500);
        View itemView = recentConsistsListView.getChildAt(onScreenPosition - recentConsistsListView.getFirstVisiblePosition());
        itemView.startAnimation(anim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                recentConsistsList.remove(onScreenPosition);
                saveRecentConsistsList(true);
                refreshRecentConsistsList(true);
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

        @SuppressLint("InflateParams")
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(threaded_application.applicationName, activityName + ": RosterSimpleAdapter(): getView()");
            if (position < 0 || position >= rosterList.size())
                return convertView;

            HashMap<String, String> hm = rosterList.get(position);
            if (hm == null)
                return convertView;


            LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("ViewHolder") RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.roster_list_item, null, false);
//            RelativeLayout view = (RelativeLayout) convertView;
//            if (convertView == null) {
//                LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                view = (RelativeLayout) inflater.inflate(R.layout.roster_list_item, null, false);
//
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
//            }

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

        @SuppressLint("InflateParams")
        public View getView(int position, View convertView, ViewGroup parent) {
//            Log.d(threaded_application.applicationName, activityName + ": RecentSimpleAdapter(): getView()");
            if (position >= recentLocosList.size())
                return convertView;

            HashMap<String, String> hm = recentLocosList.get(position);
            if (hm == null)
                return convertView;

            LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("ViewHolder") RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.engine_list_item, null, false);
//            RelativeLayout view = (RelativeLayout) convertView;
//            if (convertView == null) {
//                LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                view = (RelativeLayout) inflater.inflate(R.layout.engine_list_item, null, false);

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
//            }

            return view;
        }
    }

    private void loadRosterOrRecentImage(String engineName, ImageView imageView, String iconURL) {
//        Log.d(threaded_application.applicationName, activityName + ": loadRosterOrRecentImage()");
        //see if there is a saved file and preload it, even if it gets written over later
        boolean foundSavedImage = false;
        String imgFileName = mainapp.fixFilename(engineName) + ".png";
        File image_file = new File(getApplicationContext().getExternalFilesDir(null), "/"+RECENT_LOCO_DIR+"/"+imgFileName);
        if (image_file.exists()) {
            try {
                imageView.setImageBitmap(BitmapFactory.decodeFile(image_file.getPath()));
                foundSavedImage = true;
            } catch (Exception e) {
                Log.d(threaded_application.applicationName, activityName + ": loadRosterOrRecentImage(): recent consists - image file found but could not loaded");
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

        @SuppressLint("InflateParams")
        public View getView(int position, View convertView, ViewGroup parent) {
//            Log.d(threaded_application.applicationName, activityName + ": RecentConsistsSimpleAdapter(): getView()");
            if (position >= recentConsistsList.size())
                return convertView;

            HashMap<String, String> hm = recentConsistsList.get(position);
            if (hm == null)
                return convertView;

            LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("ViewHolder") RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.consists_list_item, null, false);
//            RelativeLayout view = (RelativeLayout) convertView;
//            if (convertView == null) {
//                LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                view = (RelativeLayout) inflater.inflate(R.layout.consists_list_item, null, false);

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
//            }

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
        edt.setText(importExportPreferences.recentConsistNameList.get(pos));

        dialogBuilder.setTitle(getApplicationContext().getResources().getString(R.string.RecentConsistsNameEditTitle));
        dialogBuilder.setMessage(getApplicationContext().getResources().getString(R.string.RecentConsistsNameEditText));
        dialogBuilder.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String rslt = edt.getText().toString();
                if (!rslt.isEmpty()) {
                    importExportPreferences.recentConsistNameList.set(pos, rslt);
                    removingConsistOrForceRewrite = true;
                    saveRecentConsistsList(true);
                    refreshRecentConsistsList(true);
                    recentConsistsListView.invalidateViews();
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
                    refreshRecentLocosList(true);
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
        Log.d(threaded_application.applicationName, activityName + ": dispatchKeyEvent()");
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
            if (!dir.exists()) {
                if (!dir.mkdir()) { // in case the folder does not already exist
                    unableToSaveLocoImage();
                    return false;
                }
            }
            String imgFileName = mainapp.fixFilename(rosterNameString) + ".png";
            File imageFile = new File(context.getExternalFilesDir(null) + "/" + RECENT_LOCO_DIR + "/" + imgFileName);
            if (imageFile.exists()) {
                if (!imageFile.delete()) { // delete the old version if it exists
                    unableToSaveLocoImage();
                    return false;
                }
            }
            FileOutputStream fileOutputStream =
                    new FileOutputStream(context.getExternalFilesDir(null) + "/" + RECENT_LOCO_DIR + "/" + imgFileName);
            Bitmap bitmap = viewToBitmap(imageView, 52);   //52dp
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e)  {
            unableToSaveLocoImage();
            return false;
        }
    }

    private void unableToSaveLocoImage() {
        Log.d(threaded_application.applicationName, activityName + ": writeLocoImageToFile(): Unable to save roster loco image");
    }

    private boolean deleteLocoImageFile(String rosterNameString) {
//        Log.d(threaded_application.applicationName, activityName + ": deleteLocoImageFile()");
        try {
            File dir = new File(context.getExternalFilesDir(null), RECENT_LOCO_DIR);
            if (!dir.exists()) {
                if(!dir.mkdir()) { // in case the folder does not already exist
                    unableToDeleteLocoImage();
                    return false;
                }
            }
            String imgFileName = mainapp.fixFilename(rosterNameString + ".png");
            File imageFile = new File(context.getExternalFilesDir(null) + "/" + RECENT_LOCO_DIR + "/" + imgFileName);
            if (imageFile.exists()) {
                if (!imageFile.delete()) {
                    unableToDeleteLocoImage();
                    return false;
                }
            }
            return true;
        } catch (Exception e)  {
            unableToDeleteLocoImage();
            return false;
        }
    }

    private void unableToDeleteLocoImage() {
        Log.d(threaded_application.applicationName, activityName + ": deleteLocoImageFile(): Unable to delete roster loco image");
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

    public class OwnersFilterSpinnerListener implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.d(threaded_application.applicationName, activityName + ": OwnersFilterSpinnerListener(): onItemSelected()");
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

    public class SelectLocoMethodIndexSpinnerListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.d(threaded_application.applicationName, activityName + ": SelectLocoMethodIndexSpinnerListener(): onItemSelected()");
            selectLocoMethodIndex = selectLocoMethodSpinner.getSelectedItemPosition();
            showMethod(Integer.toString(selectLocoMethodIndex+1));
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class SelectMethodButtonListener implements View.OnClickListener {
        final String myMethod;

        SelectMethodButtonListener(String method) {
            myMethod = method;
        }

        public void onClick(View v) {
//            Log.d(threaded_application.applicationName, activityName + ": SelectMethodButtonListener(): onClick()");
            mainapp.buttonVibration();
            showMethod(myMethod);
            mainapp.hideSoftKeyboard(v, activityName);
        }
    }

    void reopenThrottlePage() {
        Log.d(threaded_application.applicationName, activityName + ": reopenThrottlePage()");
        endThisActivity();
    }

    void adjustToolbarSize(Menu menu) {
        ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
        int toolbarHeight = layoutParams.height;
        int newHeightAndWidth = toolbarHeight;

        if (!threaded_application.useSmallToolbarButtonSize) {
            newHeightAndWidth = toolbarHeight*2;
            layoutParams.height = newHeightAndWidth;
            toolbar.setLayoutParams(layoutParams);
        }
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            View itemChooser = item.getActionView();

            if (itemChooser != null) {
                itemChooser.getLayoutParams().height = newHeightAndWidth;
                itemChooser.getLayoutParams().width = (int) ( (float) newHeightAndWidth * 1.3 );

                itemChooser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onOptionsItemSelected(item);
                    }
                });
            }
        }
    }
}
