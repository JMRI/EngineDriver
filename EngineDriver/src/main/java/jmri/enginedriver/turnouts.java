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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmri.enginedriver.logviewer.ui.LogViewerActivity;

import static android.text.TextUtils.substring;

//public class turnouts extends AppCompatActivity implements OnGestureListener {
public class turnouts extends AppCompatActivity implements android.gesture.GestureOverlayView.OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private ArrayList<HashMap<String, String>> turnoutsFullList;
    private ArrayList<HashMap<String, String>> turnouts_list;
    private SimpleAdapter turnouts_list_adapter;
    private ArrayList<String> locationList;
    private ArrayAdapter<String> locationListAdapter;
    private static String location = null;
    private Spinner locationSpinner;

    private GestureDetector myGesture;
    private Menu TuMenu;

    private static final int WHICH_SOURCE_ADDRESS = 1;
    private static final int WHICH_SOURCE_ROSTER = 2;

    private static final String WHICH_METHOD_FIRST = "0"; // first time the app has been used
    private static final String WHICH_METHOD_ADDRESS = "1";
    private static final String WHICH_METHOD_ROSTER = "2";

    String prefSelectTurnoutsMethod = "0";
    String currentSelectTurnoutMethod = "0";
    String lastTurnoutUsedName = "XYZZY";

    private static final String TURNOUT_TOGGLE = "2";
    private static final String TURNOUT_THROW = "T";
    private static final String TURNOUT_CLOSE = "C";

    private static final String TURNOUT_STATE_UNKNOWN_LABEL = "   ???";

    ListView turnouts_lv;

    TextView tvSelectionHeading;
    LinearLayout llSelectionGroup;
    LinearLayout llAddress;
    LinearLayout llRoster;
    LinearLayout llRecent;
    RadioButton rbAddress;
    RadioButton rbRoster;

    ArrayList<HashMap<String, String>> recentTurnoutsList;
    private RecentTurnoutsSimpleAdapter recentTurnoutsListAdapter;
    ListView recentTurnoutsListView;

    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();
    boolean removingTurnoutOrForceReload = false;

    String turnoutSystemName = "";
    String turnoutUserName = "";
    int turnoutSource = 0;

    private Toolbar toolbar;

    protected View turnoutsView;
    protected GestureOverlayView turnoutsOverlayView;
    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    protected boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;


    public void refresh_turnout_view() {
        //specify logic for sort comparison (by username)
        Comparator<HashMap<String, String>> turnout_comparator = new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
                return arg0.get("to_user_name").compareTo(arg1.get("to_user_name"));    //*** was compareToIgnoreCase()
            }
        };

        //clear and rebuild, or disable if not allowed
        turnoutsFullList.clear();
        locationList.clear();
        if (mainapp.isTurnoutControlAllowed()) {
            if (mainapp.to_user_names != null) { //none defined
                int pos = 0;
                String del = prefs.getString("DelimiterPreference", getApplicationContext().getResources().getString(R.string.prefDelimiterDefaultValue));
                boolean hideIfNoUserName = prefs.getBoolean("HideIfNoUserNamePreference", getResources().getBoolean(R.bool.prefHideIfNoUserNameDefaultValue));
                for (String username : mainapp.to_user_names) {
                    boolean hasUserName = (username != null && !username.equals(""));
                    if (hasUserName || !hideIfNoUserName) {  //skip turnouts without usernames if pref is set
                        //get values from global array
                        String systemName = mainapp.to_system_names[pos];
                        String currentState = mainapp.to_states[pos];
                        String currentStateDesc = getCurrentStateDesc(currentState);

                        //put values into temp hashmap
                        HashMap<String, String> hm = new HashMap<>();
                        if (hasUserName)
                            hm.put("to_user_name", username);
                        else
                            hm.put("to_user_name", systemName);
                        hm.put("to_system_name", systemName);
                        hm.put("to_current_state_desc", currentStateDesc);
                        turnoutsFullList.add(hm);

                        //if location is new, add to list
                        if (del.length() > 0 && hasUserName) {
                            int delim = username.indexOf(del);
                            if (delim >= 0) {
                                String loc = username.substring(0, delim);
                                if (!locationList.contains(loc))
                                    locationList.add(loc);
                            }
                        }
                    }
                    pos++;
                }
            }
        }

        updateTurnoutEntry();

        //sort by username
        Collections.sort(turnoutsFullList, turnout_comparator);
        Collections.sort(locationList);
        locationList.add(0, getString(R.string.location_all));   // this entry goes at the top of the list
        locationListAdapter.notifyDataSetChanged();
        if (!locationList.contains(location))
            location = getString(R.string.location_all);
        locationSpinner.setSelection(locationListAdapter.getPosition(location));

        filterTurnoutView();
    }


    public void refreshTurnoutViewStates() {
        if (mainapp.isTurnoutControlAllowed()) {
            if (mainapp.to_user_names != null) { //none defined
                int pos = 0;
                boolean hideIfNoUserName = prefs.getBoolean("HideIfNoUserNamePreference", getResources().getBoolean(R.bool.prefHideIfNoUserNameDefaultValue));
                for (String username : mainapp.to_user_names) {
                    boolean hasUserName = (username != null && !username.equals(""));
                    if (hasUserName || !hideIfNoUserName) {  //skip turnouts without usernames if pref is set
                        //get values from global array
                        String currentState = mainapp.to_states[pos];
                        String currentStateDesc = getCurrentStateDesc(currentState);

                        boolean found = false;
                        for (int i = 0; i < turnouts_lv.getCount() && !found; i++) {
                            ViewGroup vg = (ViewGroup) turnouts_lv.getChildAt(i);  //start with the list item the button belongs to
                            if (vg != null) {
                                ViewGroup rl = (ViewGroup) vg.getChildAt(0);  //get relativelayout that holds systemname and username
                                TextView unv = (TextView) rl.getChildAt(0); // get username text from 1st box
                                String rowUserName = unv.getText().toString();

                                if (username.equals(rowUserName)) {
                                    Button b = vg.findViewById(R.id.to_current_state_desc);
                                    Button bt = vg.findViewById(R.id.turnout_throw);
                                    Button bc = vg.findViewById(R.id.turnout_close);

                                    showHideTurnoutButtons(currentStateDesc, b, bt, bc);
                                    found = true;
                                }
                            }
                        }
                    }
                    pos++;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void filterTurnoutView() {
        final String loc = location + prefs.getString("DelimiterPreference", getApplicationContext().getResources().getString(R.string.prefDelimiterDefaultValue));
        final boolean useAllLocations = getString(R.string.location_all).equals(location);
        turnouts_list.clear();
        for (HashMap<String, String> hm : turnoutsFullList) {
            String userName = hm.get("to_user_name");
            if (useAllLocations || userName.startsWith(loc)) {
                HashMap<String, String> hmFilt = (HashMap<String, String>) hm.clone();
                if (!useAllLocations)
                    hmFilt.put("to_user_name", userName.substring(loc.length()));
                turnouts_list.add(hmFilt);
            }
        }
        turnouts_list_adapter.notifyDataSetChanged();  //update the list
    }

    //set the button states for the turnout address direct entry
    private int updateTurnoutEntry() {
        Button butTog = findViewById(R.id.turnout_toggle);
        Button butClose = findViewById(R.id.turnout_close);
        Button butThrow = findViewById(R.id.turnout_throw);
        EditText trn = findViewById(R.id.turnout_entry);
        TextView trnPrefix = findViewById(R.id.turnout_prefix);
        String turnout = trn.getText().toString().trim();
        int txtLen = turnout.length();
        if (mainapp.isTurnoutControlAllowed()) {
            trn.setEnabled(true);
            // don't allow buttons if nothing entered
            if (txtLen > 0) {
                butThrow.setEnabled(true);
                butClose.setEnabled(true);
                butTog.setEnabled(true);
                if (Character.isDigit(turnout.charAt(0))) //show hardware system prefix if numeric entry
                    trnPrefix.setEnabled(true);
                else
                    trnPrefix.setEnabled(false);
            } else {
                butThrow.setEnabled(false);
                butClose.setEnabled(false);
                butTog.setEnabled(false);
                trnPrefix.setEnabled(false);
            }
        } else {
            trn.setEnabled(false);
            butThrow.setEnabled(false);
            butClose.setEnabled(false);
            butTog.setEnabled(false);
            //set text to "Disabled", but only do this once to avoid getting stuck in this callback
            //NOTE: field is max 8 chars, and might be translated shorter or longer, so pad and truncate
            String disabledText = substring(getString(R.string.disabled) + "        ", 0, 8);
            if (!trn.getText().toString().equals(disabledText))
                trn.setText(disabledText);
            trnPrefix.setEnabled(false);
        }
        if (mainapp.getServerType().equals("Digitrax")) {  //Digitrax LnWi does not support toggle
            butTog.setEnabled(false);
        }

        if (TuMenu != null) {
            mainapp.displayEStop(TuMenu);
            mainapp.displayPowerStateMenuButton(TuMenu);
        }

        return txtLen;
    }

    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class turnouts_handler extends Handler {

        public void handleMessage(Message msg) {
            String response_str;
            switch (msg.what) {
                case message_type.RESPONSE:
                    response_str = msg.obj.toString();

                    if (response_str.length() >= 3) {
                        String com1 = response_str.substring(0, 3);
                        //refresh turnouts if any have changed or if turnout list has changed
                        if ("PTA".equals(com1) || "PTL".equals(com1)) {
                            refresh_turnout_view();
                            refreshTurnoutViewStates();
                            refreshRecentTurnoutView();
                            //show server list first time it arrives
                            if ("PTL".equals(com1) && !mainapp.shownRosterTurnouts) {
                                showMethod(WHICH_METHOD_ROSTER);
                            }
                        }
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateButton(TuMenu);
                        }
                    }
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refresh_turnout_view();
                    refreshTurnoutViewStates();
                    refreshRecentTurnoutView();
                    break;
                case message_type.TIME_CHANGED:
                    setActivityTitle();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
                case message_type.WIT_TURNOUT_NOT_DEFINED:
                    removeTurnoutFromRecentList(msg.obj.toString());
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

    public class button_listener implements View.OnClickListener {
        char whichCommand; //command to send for button instance 'C'lose, 'T'hrow or '2' for toggle

        public button_listener(char new_command) {
            whichCommand = new_command;
        }

        public void onClick(View v) {
            EditText entryv = findViewById(R.id.turnout_entry);
            String entrytext = entryv.getText().toString().trim();
            if (entrytext.length() > 0) {
                //if text starts with a digit, check number and prefix with hardware_system and "T"
                //otherwise send the text as is
                if (Character.isDigit(entrytext.charAt(0))) {
                    try {
                        Integer.valueOf(entrytext);  //edit check address by attempting conversion to int
                    } catch (Exception except) {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastTurnoutInvalidNumber) + " " + except.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TURNOUT, whichCommand + entrytext);

                turnoutSystemName = entrytext;
                turnoutUserName = entrytext;
                turnoutSource = WHICH_SOURCE_ADDRESS;

                boolean reloadRecents = false;
                if (importExportPreferences.recent_turnout_address_list.size() > 0) {
                    if (!turnoutSystemName.equals(importExportPreferences.recent_turnout_address_list.get(0))) {
                        reloadRecents = true;
                    }
                } else {
                    reloadRecents = true;
                }

                saveRecentTurnoutsList(true);

                if (reloadRecents) {
                    loadRecentTurnoutsList();
                    showHideRecentsList();
                }
            }
        }
    }

    //handle click for each turnout's state toggle button
    public class turnout_state_button_listener implements View.OnClickListener {
        String _buttonType;

        turnout_state_button_listener(String buttonType) {
            _buttonType = buttonType;
        }

        public void onClick(View v) {
            ViewGroup vg = (ViewGroup) v.getParent();  //start with the list item the button belongs to
            ViewGroup rl = (ViewGroup) vg.getChildAt(0);  //get relativelayout that holds systemname and username
            TextView snv = (TextView) rl.getChildAt(1); // get systemname text from 2nd box
            TextView unv = (TextView) rl.getChildAt(0); // get username text from 1st box
            turnoutSystemName = snv.getText().toString();
            turnoutUserName = unv.getText().toString();
            turnoutSource = WHICH_SOURCE_ROSTER;
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TURNOUT, _buttonType + turnoutSystemName);    // C=Close T=Throw 2=toggle

            saveRecentTurnoutsList(true);
            showHideRecentsList();
        }
    }

    //handle click for each recent turnout's state toggle button
    public class recentTurnoutStateButtonListener implements View.OnClickListener {
        String _buttonType;

        recentTurnoutStateButtonListener(String buttonType) {
            _buttonType = buttonType;
        }

        public void onClick(View v) {
            ViewGroup vg = (ViewGroup) v.getParent();  //start with the list item the button belongs to
            ViewGroup rl = (ViewGroup) vg.getChildAt(0);  //get relativelayout that holds systemname and username
            TextView source = (TextView) rl.getChildAt(2); // get source from 3nd (hidden) box
            TextView snv = (TextView) rl.getChildAt(1); // get systemname text from 2nd box
            TextView unv = (TextView) rl.getChildAt(0); // get username text from 1st box
            turnoutSystemName = snv.getText().toString();
            turnoutUserName = unv.getText().toString();
            turnoutSource = Integer.parseInt(source.getText().toString());
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TURNOUT, _buttonType + turnoutSystemName);    // C=Close T=Throw 2=toggle

            removingTurnoutOrForceReload = true;
            saveRecentTurnoutsList(true);
            showHideRecentsList();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return myGesture.onTouchEvent(event);
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        mainapp.applyTheme(this);

        setContentView(R.layout.turnouts);

        //put pointer to this activity's handler in main app's shared variable (If needed)
        mainapp.turnouts_msg_handler = new turnouts_handler();

//        myGesture = new GestureDetector(this);

        // -------------------------------------------------------------------

        turnoutsFullList = new ArrayList<>();
        //Set up a list adapter to allow adding the list of defined turnouts to the UI.
        turnouts_list = new ArrayList<>();
        turnouts_list_adapter = new SimpleAdapter(this, turnouts_list, R.layout.turnouts_item,
                new String[]{"to_user_name", "to_system_name", "to_current_state_desc"},
                new int[]{R.id.to_user_name, R.id.to_system_name, R.id.to_current_state_desc}) {

            //set up listener for each state button
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                if (row != null) {
                    Button bt = row.findViewById(R.id.turnout_throw);
                    bt.setOnClickListener(new turnout_state_button_listener(TURNOUT_THROW));

                    Button bc = row.findViewById(R.id.turnout_close);
                    bc.setOnClickListener(new turnout_state_button_listener(TURNOUT_CLOSE));

                    Button b = row.findViewById(R.id.to_current_state_desc);
                    b.setOnClickListener(new turnout_state_button_listener(TURNOUT_TOGGLE));

                    setTurnoutRowButtonState(row, b, bt, bc);
                }
                return row;
            }
        };
        turnouts_lv = findViewById(R.id.turnouts_list);
        turnouts_lv.setAdapter(turnouts_list_adapter);

        OnTouchListener gestureListener = new ListView.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return myGesture != null && myGesture.onTouchEvent(event);
            }
        };
        turnouts_lv.setOnTouchListener(gestureListener);

        turnouts_lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == 0) {  // scrolling has stopped
                    refreshTurnoutViewStates();
                }
            }
        });

        // -------------------------------------------------------------------

        EditText trn = findViewById(R.id.turnout_entry);
        trn.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                updateTurnoutEntry();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        trn.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    return true;
                } else
                    return false;
            }
        });

        //Set the button callbacks, storing the command to pass for each
        Button b = findViewById(R.id.turnout_toggle);
        button_listener click_listener = new button_listener('2');
        b.setOnClickListener(click_listener);

        b = findViewById(R.id.turnout_close);
        click_listener = new button_listener('C');
        b.setOnClickListener(click_listener);

        b = findViewById(R.id.turnout_throw);
        click_listener = new button_listener('T');
        b.setOnClickListener(click_listener);

        locationList = new ArrayList<>();
        locationSpinner = findViewById(R.id.turnouts_location);
        locationListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationList);
        locationListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationListAdapter);

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                location = parent.getSelectedItem().toString();
                filterTurnoutView();
                refreshTurnoutViewStates();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        llSelectionGroup = findViewById(R.id.select_turnout_method_radio_group);
        tvSelectionHeading = findViewById(R.id.turnouts_selection_type_heading);
        llAddress = findViewById(R.id.enter_turnout_address_group);
        llRoster = findViewById(R.id.turnouts_from_roster_group);
        llRecent = findViewById(R.id.turnouts_recent_group);
        mainapp.shownRosterTurnouts = false;

        // -------------------------------------------------------------------

        // Set up a list adapter to allow adding the list of recent turnouts to the UI.
        recentTurnoutsList = new ArrayList<>();
        recentTurnoutsListAdapter = new RecentTurnoutsSimpleAdapter(this, recentTurnoutsList, R.layout.turnouts_recent_item,
                new String[]{"to_recent_user_name", "to_recent_system_name"},
                new int[]{R.id.to_recent_user_name, R.id.to_recent_system_name}) {

            //set up listener for each Throw state button
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                if (row != null) {

                    Button b = row.findViewById(R.id.turnout_recent_throw);
                    b.setOnClickListener(new recentTurnoutStateButtonListener(TURNOUT_THROW));

                    b = row.findViewById(R.id.turnout_recent_close);
                    b.setOnClickListener(new recentTurnoutStateButtonListener(TURNOUT_CLOSE));

                    b = row.findViewById(R.id.turnout_recent_toggle);
                    b.setOnClickListener(new recentTurnoutStateButtonListener(TURNOUT_TOGGLE));
                }
                return row;
            }
        };
        recentTurnoutsListView = findViewById(R.id.turnouts_recent_list);
        recentTurnoutsListView.setAdapter(recentTurnoutsListAdapter);
        OnTouchListener recentTurnoutsGestureListener = new ListView.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return myGesture != null && myGesture.onTouchEvent(event);
            }
        };
        recentTurnoutsListView.setOnTouchListener(recentTurnoutsGestureListener);

        loadRecentTurnoutsList();


        b = findViewById(R.id.clear_turnouts_list_button);
        b.setOnClickListener(new clearRecentTurnoutsListButton());

        // -------------------------------------------------------------------

        // setup the method radio buttons
        rbAddress = findViewById(R.id.select_turnout_method_address_button);
        rbRoster = findViewById(R.id.select_turnout_method_roster_button);
        String serverType = mainapp.getServerType();
        if (serverType.isEmpty())
            serverType = getApplicationContext().getResources().getString(R.string.server);
        rbRoster.setText(getApplicationContext().getResources().getString(R.string.turnoutsSelectionTypeRoster, serverType));

        prefSelectTurnoutsMethod = prefs.getString("prefSelectTurnoutsMethod", WHICH_METHOD_FIRST);
        // if the recent lists are empty make sure the radio button will be pointing to something valid
        if (((importExportPreferences.recent_turnout_address_list.size() == 0) && (prefSelectTurnoutsMethod.equals(WHICH_METHOD_ADDRESS)))) {
            prefSelectTurnoutsMethod = WHICH_METHOD_ADDRESS;
        }

        showMethod(prefSelectTurnoutsMethod);

        RadioGroup rgTurnoutsSelect = findViewById(R.id.select_turnout_method_radio_group);
        rgTurnoutsSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.select_turnout_method_address_button:
                        showMethod(WHICH_METHOD_ADDRESS);
                        break;
                    case R.id.select_turnout_method_roster_button:
                        showMethod(WHICH_METHOD_ROSTER);
                        break;
                }
            }
        });

        //update turnout list
        refresh_turnout_view();
        refreshTurnoutViewStates();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        turnoutsView = findViewById(R.id.select_turnout_screen);
        // enable swipe/fling detection if enabled in Prefs
        turnoutsOverlayView = findViewById(R.id.turnouts_overlay);
        turnoutsOverlayView.addOnGestureListener(this);
        turnoutsOverlayView.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }


    } // end onCreate

    @Override
    public void onPause() {
        super.onPause();

        //save scroll position for later restore
        ListView lv = findViewById(R.id.turnouts_list);
        mainapp.turnouts_list_position = (lv == null ? 0 : lv.getFirstVisiblePosition());

        //make sure the soft keyboard is closed
        EditText trn = findViewById(R.id.turnout_entry);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && trn != null) {
            imm.hideSoftInputFromWindow(trn.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        if (!mainapp.setActivityOrientation(this))  //set screen orientation based on prefs
        {
            Intent in = new Intent().setClass(this, web_activity.class);      // if autoWeb and landscape, switch to Web activity
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(in);
            this.finish();
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return;
        }

        //restore view to last known scroll position
        ListView lv = findViewById(R.id.turnouts_list);
        lv.setSelectionFromTop(mainapp.turnouts_list_position, 0);

        // enable/disable buttons
        updateTurnoutEntry();
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setActivityTitle();
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        //Log.d("Engine_Driver","turnouts.onDestroy()");
        super.onDestroy();

        if (mainapp.turnouts_msg_handler != null) {
            mainapp.turnouts_msg_handler.removeCallbacksAndMessages(null);
            mainapp.turnouts_msg_handler = null;
        } else {
            Log.d("Engine_Driver", "onDestroy: mainapp.turnouts_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }


    //Always go to throttle activity if back button pressed
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            this.finish();
            connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

//    @Override
//    public boolean onDown(MotionEvent e) {
//        return false;
//    }
//
//    @Override
//    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        if (e1 == null || e2 == null)
//            return false;
//        float deltaX = e2.getX() - e1.getX();
//        float absDeltaX = Math.abs(deltaX);
//        if ((absDeltaX > threaded_application.min_fling_distance) &&
//                (Math.abs(velocityX) > threaded_application.min_fling_velocity) &&
//                (absDeltaX > Math.abs(e2.getY() - e1.getY()))) {
//            // left to right swipe goes to web, then routes if enabled in prefs
//            if (deltaX > 0.0) {
//                boolean swipeWeb = prefs.getBoolean("swipe_through_web_preference",
//                        getResources().getBoolean(R.bool.prefSwipeThroughWebDefaultValue));
//                swipeWeb = swipeWeb && mainapp.isWebAllowed();  //also check the allowed flag
//                if (swipeWeb) {
//                    Intent in = new Intent().setClass(this, web_activity.class);
//                    startActivity(in);
//                } else {
//                    boolean swipeRoutes = prefs.getBoolean("swipe_through_routes_preference",
//                            getResources().getBoolean(R.bool.prefSwipeThroughRoutesDefaultValue));
//                    swipeRoutes = swipeRoutes && mainapp.isRouteControlAllowed();  //also check the allowed flag
//                    if (swipeRoutes) {
//                        Intent in = new Intent().setClass(this, routes.class);
//                        startActivity(in);
//                    }
//                }
//                this.finish();  //don't keep on return stack
//                connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
//            }
//            // right to left swipe goes to throttle
//            else {
//                this.finish();  //don't keep on return stack
//                connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
//            }
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public void onLongPress(MotionEvent e) {
//    }
//
//    @Override
//    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
//                            float distanceY) {
//        return false;
//    }
//
//    @Override
//    public void onShowPress(MotionEvent e) {
//    }
//
//    @Override
//    public boolean onSingleTapUp(MotionEvent e) {
//        return false;
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.turnouts_menu, menu);
        TuMenu = menu;
        mainapp.actionBarIconCountTurnouts = 0;
        mainapp.displayEStop(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerMenuOption(menu);
        mainapp.setPowerStateButton(menu);
        mainapp.setWebMenuOption(menu);
        mainapp.setRoutesMenuOption(menu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
//        mainapp.displayMenuSeparator(menu, this, mainapp.actionBarIconCountTurnouts);

        return  super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        switch (item.getItemId()) {
            case R.id.throttle_button_mnu:
            case R.id.throttle_mnu:
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
                return true;
            case R.id.routes_mnu:
                in = new Intent().setClass(this, routes.class);
                startActivity(in);
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
                return true;
            case R.id.web_mnu:
                in = new Intent().setClass(this, web_activity.class);
                mainapp.webMenuSelected = true;
                startActivity(in);
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                return true;
            case R.id.exit_mnu:
                mainapp.checkExit(this);
                return true;
            case R.id.power_control_mnu:
                in = new Intent().setClass(this, power_control.class);
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                return true;
            case R.id.preferences_mnu:
                in = new Intent().setClass(this, SettingsActivity.class);
                startActivityForResult(in, 0);   // refresh view on return
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                return true;
            case R.id.settings_mnu:
                in = new Intent().setClass(this, SettingsActivity.class);
                startActivityForResult(in, 0);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                return true;
            case R.id.about_mnu:
                in = new Intent().setClass(this, about_page.class);
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                return true;
            case R.id.logviewer_menu:
                Intent logviewer = new Intent().setClass(this, LogViewerActivity.class);
                startActivity(logviewer);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                return true;
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                return true;
            case R.id.power_layout_button:
                mainapp.powerStateMenuButton();
                return true;
            case R.id.flashlight_button:
                mainapp.toggleFlashlight(this, TuMenu);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //handle return from menu items
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //since we always do the same action no need to distinguish between requests
        refresh_turnout_view();
        refreshTurnoutViewStates();
        refreshRecentTurnoutView();
    }

    private void disconnect() {
        this.finish();
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }


    private void loadRecentTurnoutsList() {

        importExportPreferences.recent_turnout_address_list = new ArrayList<>();
        importExportPreferences.recent_turnout_name_list = new ArrayList<>();
        importExportPreferences.recent_turnout_source_list = new ArrayList<>();
        importExportPreferences.recent_turnout_server_list = new ArrayList<>();
        ArrayList<HashMap<String, String>> tempRecentTurnoutsList = new ArrayList<>();

        //if no SD Card present then there is no recent consists list
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            importExportPreferences.getRecentTurnoutsListFromFile();
            for (int i = 0; i < importExportPreferences.recent_turnout_address_list.size(); i++) {
                // only load the turnout if it came from the current server
                if (importExportPreferences.recent_turnout_server_list.get(i).equals(mainapp.connectedHostip)) {
                    HashMap<String, String> hm = new HashMap<>();
                    String turnoutAddressString = importExportPreferences.recent_turnout_address_list.get(i);
                    String turnoutAddressSource = importExportPreferences.recent_turnout_source_list.get(i).toString();
                    String turnoutAddressServer = importExportPreferences.recent_turnout_server_list.get(i);

                    hm.put("turnout_name", importExportPreferences.recent_turnout_name_list.get(i)); // the larger name text
                    hm.put("turnout", turnoutAddressString);   // the small address field at the top of the row
                    hm.put("turnout_source", turnoutAddressSource);
                    hm.put("turnout_server", turnoutAddressServer);
                    tempRecentTurnoutsList.add(hm);
                }
            }

            showHideRecentsList();

            recentTurnoutsList.clear();
            recentTurnoutsList.addAll(tempRecentTurnoutsList);
            recentTurnoutsListAdapter.notifyDataSetChanged();
        }
    }


    @SuppressLint("ApplySharedPref")
    private void showMethod(String whichMethod) {
        if (mainapp.to_user_names == null) { // no defined Turnouts  Always force it to the address
            tvSelectionHeading.setVisibility(View.GONE);
            llSelectionGroup.setVisibility(View.GONE);
            whichMethod = WHICH_METHOD_ADDRESS;
        } else {
            tvSelectionHeading.setVisibility(View.VISIBLE);
            llSelectionGroup.setVisibility(View.VISIBLE);
            if (!mainapp.shownRosterTurnouts) { //this server has turnouts, and this is the first time in the turnouts view for this session
                mainapp.shownRosterTurnouts = true;
                whichMethod = WHICH_METHOD_ROSTER;
            }
        }
        currentSelectTurnoutMethod = whichMethod;
        switch (whichMethod) {
            default:
            case WHICH_METHOD_ADDRESS: {
                llAddress.setVisibility(View.VISIBLE);
                llRoster.setVisibility(View.GONE);
                llRecent.setVisibility(View.GONE);

                rbAddress.setChecked(true);
                rbRoster.setChecked(false);

                loadRecentTurnoutsList();
                break;
            }
            case WHICH_METHOD_ROSTER: {
                llAddress.setVisibility(View.GONE);
                llRoster.setVisibility(View.VISIBLE);
                llRecent.setVisibility(View.GONE);

                rbAddress.setChecked(false);
                rbRoster.setChecked(true);
                break;
            }
        }
        //save last selection method for next time
        prefs.edit().putString("prefSelectTurnoutsMethod", whichMethod).commit();
    }


    public class RecentTurnoutsSimpleAdapter extends SimpleAdapter {
        private Context cont;

        RecentTurnoutsSimpleAdapter(Context context,
                                    List<? extends Map<String, ?>> data, int resource,
                                    String[] from, int[] to) {
            super(context, data, resource, from, to);
            cont = context;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            if (position >= recentTurnoutsList.size())
                return convertView;

            HashMap<String, String> hm = recentTurnoutsList.get(position);
            if (hm == null)
                return convertView;

            LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout view = (LinearLayout) inflater.inflate(R.layout.turnouts_recent_item, null, false);

            String toName = hm.get("turnout_name");
            if (toName != null) {
                TextView name = view.findViewById(R.id.to_recent_user_name);
                name.setText(Html.fromHtml(toName));
            }

            String toAddress = hm.get("turnout");
            if (toAddress != null) {
                TextView secondLine = view.findViewById(R.id.to_recent_system_name);
                secondLine.setText(toAddress);
            }

            String str = hm.get("turnout_source");
            if (str != null) {
                TextView name = view.findViewById(R.id.to_recent_source);
                name.setText(Html.fromHtml(str));
                int i = Integer.parseInt(str);

                Button b = view.findViewById(R.id.turnout_recent_toggle);
                if (i != WHICH_SOURCE_ROSTER) {
                    b.setVisibility(LinearLayout.GONE);
                } else {
                    Button bt = view.findViewById(R.id.turnout_recent_throw);
                    Button bc = view.findViewById(R.id.turnout_recent_close);

                    String currentState;
                    String currentStateDesc = TURNOUT_STATE_UNKNOWN_LABEL;
                    if (mainapp.to_user_names != null) {
                        for (int pos = 0; pos < mainapp.to_user_names.length; pos++) {
                            String systemName = mainapp.to_system_names[pos];
                            if (systemName.equals(toAddress)) {
                                currentState = mainapp.to_states[pos];
                                currentStateDesc = getCurrentStateDesc(currentState);
                            }
                        }
                    }
                    b.setText(currentStateDesc);

                    showHideTurnoutButtons(currentStateDesc, b, bt, bc);
                }
            }
            return view;
        }
    }

    void removeTurnoutFromRecentList(String turnoutToRemoveSystemName) {
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;

        for (int i = importExportPreferences.recent_turnout_address_list.size() - 1; i >= 0; i--) {
            if (turnoutToRemoveSystemName.equals(importExportPreferences.recent_turnout_address_list.get(i))) {
                importExportPreferences.recent_turnout_address_list.remove(i);
                importExportPreferences.recent_turnout_name_list.remove(i);
                importExportPreferences.recent_turnout_source_list.remove(i);
                importExportPreferences.recent_turnout_server_list.remove(i);
            }
        }
        removingTurnoutOrForceReload = true;
        saveRecentTurnoutsList(true);
        loadRecentTurnoutsList();
        showHideRecentsList();
    }


    //write the recent turnouts to a file
    void saveRecentTurnoutsList(boolean bUpdateList) {

        //if not updating list or no SD Card present then nothing else to do
        if (!bUpdateList || !android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;


        if (!removingTurnoutOrForceReload) {
            if (lastTurnoutUsedName.equals(turnoutSystemName)) {
                return;
            }
            lastTurnoutUsedName = turnoutSystemName;

            int numberOfRecentTurnoutsToWrite;
            String smrl = prefs.getString("maximum_recent_locos_preference", "10"); //retrieve pref for max recent locos to show
            try {
                numberOfRecentTurnoutsToWrite = Integer.parseInt(smrl) * 3;
            } catch (Exception except) {
                Log.e("Engine_Driver",
                        "deleteRecentTurnoutsListFile: Turnouts: Error retrieving maximum_recent_locos_preference "
                                + except.getMessage());
                numberOfRecentTurnoutsToWrite = 20;
            }

            // check if the most recently used turnout is already in the list and remove it
            for (int i = 0; i < importExportPreferences.recent_turnout_address_list.size(); i++) {
                if (turnoutSystemName.equals(importExportPreferences.recent_turnout_address_list.get(i))) {
                    //noinspection SuspiciousListRemoveInLoop
                    importExportPreferences.recent_turnout_address_list.remove(i);
                    //noinspection SuspiciousListRemoveInLoop
                    importExportPreferences.recent_turnout_name_list.remove(i);
                    //noinspection SuspiciousListRemoveInLoop
                    importExportPreferences.recent_turnout_source_list.remove(i);
                    //noinspection SuspiciousListRemoveInLoop
                    importExportPreferences.recent_turnout_server_list.remove(i);
                }
            }

            // now prepend it to the beginning of the list
            importExportPreferences.recent_turnout_address_list.add(0, turnoutSystemName);
            importExportPreferences.recent_turnout_name_list.add(0, turnoutUserName);
            importExportPreferences.recent_turnout_source_list.add(0, turnoutSource);
            importExportPreferences.recent_turnout_server_list.add(0, mainapp.connectedHostip);

            // remove any extra ones
            if (importExportPreferences.recent_turnout_address_list.size() > numberOfRecentTurnoutsToWrite) {
                for (int i = importExportPreferences.recent_turnout_address_list.size() - 1; i >= numberOfRecentTurnoutsToWrite; i--) {
                    importExportPreferences.recent_turnout_address_list.remove(i);
                    importExportPreferences.recent_turnout_name_list.remove(i);
                    importExportPreferences.recent_turnout_source_list.remove(i);
                    importExportPreferences.recent_turnout_server_list.remove(i);
                }
            }
        }

        removingTurnoutOrForceReload = false;

        importExportPreferences.writeRecentTurnoutsListToFile(prefs);
    }


    public class clearRecentTurnoutsListButton implements AdapterView.OnClickListener {
        public void onClick(View v) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            clearRecentTurnoutsList();
                            onCreate(null);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder ab = new AlertDialog.Builder(turnouts.this);
            ab.setTitle(getApplicationContext().getResources().getString(R.string.dialogConfirmClearTitle))
                    .setMessage(getApplicationContext().getResources().getString(R.string.dialogRecentTurnoutsConfirmClearQuestions))
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.cancel, dialogClickListener);
            ab.show();
        }
    }


    public void clearRecentTurnoutsList() {

        for (int i = importExportPreferences.recent_turnout_address_list.size() - 1; i >= 0; i--) {
            // only load the turnout if it came from the current server
            if (importExportPreferences.recent_turnout_server_list.get(i).equals(mainapp.connectedHostip)) {
                importExportPreferences.recent_turnout_name_list.remove(i);
                importExportPreferences.recent_turnout_address_list.remove(i);
                importExportPreferences.recent_turnout_source_list.remove(i);
                importExportPreferences.recent_turnout_server_list.remove(i);
            }
        }

        if (importExportPreferences.recent_turnout_address_list.size() == 0) {
            importExportPreferences.deleteFile(ImportExportPreferences.RECENT_TURNOUTS_FILENAME);
        } else {
            importExportPreferences.writeRecentTurnoutsListToFile(prefs);
        }
        recentTurnoutsList.clear();
    }


    public void refreshRecentTurnoutView() {
        if (mainapp.isTurnoutControlAllowed()) {
            if (mainapp.to_user_names != null) { //none defined
                int pos = 0;
                boolean hideIfNoUserName = prefs.getBoolean("HideIfNoUserNamePreference", getResources().getBoolean(R.bool.prefHideIfNoUserNameDefaultValue));
                for (String username : mainapp.to_user_names) {
                    boolean hasUserName = (username != null && !username.equals(""));
                    if (hasUserName || !hideIfNoUserName) {  //skip turnouts without usernames if pref is set
                        //get values from global array
                        String systemName = mainapp.to_system_names[pos];
                        String currentState = mainapp.to_states[pos];
                        String currentStateDesc = getCurrentStateDesc(currentState);
                        for (int i = 0; i < importExportPreferences.recent_turnout_address_list.size(); i++) {
                            if (systemName.equals(importExportPreferences.recent_turnout_address_list.get(i))) {

                                View view = getViewByPosition(i, recentTurnoutsListView);
                                if (view != null) {
                                    Button b = view.findViewById(R.id.turnout_recent_toggle);
                                    Button bt = view.findViewById(R.id.turnout_recent_throw);
                                    Button bc = view.findViewById(R.id.turnout_recent_close);

                                    showHideTurnoutButtons(currentStateDesc, b, bt, bc);
                                }
                            }
                        }
                    }
                    pos++;
                }
            }
        }
        updateTurnoutEntry();
    }


    View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }


    void showHideRecentsList() {
        int visible = View.GONE;
        if (currentSelectTurnoutMethod.equals(WHICH_METHOD_ADDRESS)) { // not set to address/recent
            if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) { // has an SD Card
                if (importExportPreferences.recent_turnout_address_list.size() > 0) { // if the list is empty
                    visible = View.VISIBLE;  // hide the list
                }
            }
        }
        llRecent.setVisibility(visible);
    }


    //	set the title, optionally adding the current time.
    private void setActivityTitle() {
        if (mainapp.fastClockFormat > 0)
            setToolbarTitle(getApplicationContext().getResources().getString(R.string.app_name_turnouts_short)
                    + "\n" + mainapp.getFastClockTime());
        else
            setToolbarTitle(getApplicationContext().getResources().getString(R.string.app_name_turnouts)
                    + "\n" + getApplicationContext().getResources().getString(R.string.app_name));
    }

    private String getCurrentStateDesc(String currentState) {
        String currentStateDesc = mainapp.to_state_names.get(currentState);
        if (currentStateDesc == null) {
            currentStateDesc = TURNOUT_STATE_UNKNOWN_LABEL;
        }
        return currentStateDesc;
    }

    private void setTurnoutRowButtonState(View vRow, Button bToggle, Button bThrow, Button bClose) {
        TextView snv = vRow.findViewById(R.id.to_system_name);
        String systemName = snv.getText().toString();
        String currentStateDesc = TURNOUT_STATE_UNKNOWN_LABEL;
        boolean found = false;
        for (int i = 0; i < mainapp.to_system_names.length && !found; i++) {
            if (systemName.equals(mainapp.to_system_names[i])) {
                currentStateDesc = getCurrentStateDesc(mainapp.to_states[i]);
                found = true;
            }
        }
        showHideTurnoutButtons(currentStateDesc, bToggle, bThrow, bClose);
    }

    private void showHideTurnoutButtons(String currentStateDesc, Button bToggle, Button bThrow, Button bClose) {
        if ((currentStateDesc.equals(TURNOUT_STATE_UNKNOWN_LABEL)) ||
                (currentStateDesc.equals(getApplicationContext().getResources().getString(R.string.toggle_button))) ||
                (mainapp.getServerType().equals("Digitrax"))) {  //Digitrax LnWi does not support toggle
            bToggle.setVisibility(View.GONE);
            bThrow.setVisibility(View.VISIBLE);
            bClose.setVisibility(View.VISIBLE);
        } else {
            bToggle.setVisibility(View.VISIBLE);
            bThrow.setVisibility(View.GONE);
            bClose.setVisibility(View.GONE);
        }
        bToggle.setText(currentStateDesc);
    }

    private void setToolbarTitle(String title) {
        if (toolbar != null) {
            toolbar.setTitle("");
            TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
            mTitle.setText(title);
        }
    }


    @Override
    public void onGesture(GestureOverlayView arg0, MotionEvent event) {
        gestureMove(event);
    }

    @Override
    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        gestureCancel(event);
    }

    // determine if the action was long enough to be a swipe
    @Override
    public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
        gestureEnd(event);
    }

    @Override
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
        gestureStart(event);
    }

    private void gestureStart(MotionEvent event) {
        gestureStartX = event.getX();
        gestureStartY = event.getY();
//        Log.d("Engine_Driver", "gestureStart x=" + gestureStartX + " y=" + gestureStartY);

        gestureInProgress = true;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.web_msg_handler != null)
            mainapp.web_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d("Engine_Driver", "gestureMove action " + event.getAction());
        if (gestureInProgress) {
            // stop the gesture timeout timer
            mainapp.turnouts_msg_handler.removeCallbacks(gestureStopped);

            mVelocityTracker.addMovement(event);
            if ((event.getEventTime() - gestureLastCheckTime) > gestureCheckRate) {
                // monitor velocity and fail gesture if it is too low
                gestureLastCheckTime = event.getEventTime();
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                int velocityY = (int) velocityTracker.getYVelocity();
                // Log.d("Engine_Driver", "gestureVelocity vel " + velocityX);
                if ((Math.abs(velocityX) < threaded_application.min_fling_velocity) && (Math.abs(velocityY) < threaded_application.min_fling_velocity)) {
                    gestureFailed(event);
                }
            }
            if (gestureInProgress) {
                // restart the gesture timeout timer
                mainapp.turnouts_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d("Engine_Driver", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        mainapp.turnouts_msg_handler.removeCallbacks(gestureStopped);
        if (gestureInProgress) {
            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                if (deltaX > 0.0) { // left to right swipe goes to throttle

                    boolean swipeWeb = prefs.getBoolean("swipe_through_web_preference",
                            getResources().getBoolean(R.bool.prefSwipeThroughWebDefaultValue));
//                    swipeWeb = swipeWeb && mainapp.isWebAllowed();  //also check the allowed flag
                    if (swipeWeb) {
                        Intent in = new Intent().setClass(this, web_activity.class);
                        startActivity(in);
                    }

                    this.finish();  //don't keep on return stack
                    connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);

                } else { // right to left swipe

                    // else falls back  to throttle
                    this.finish();  //don't keep on return stack
                    connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
                }
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.turnouts_msg_handler != null)
            mainapp.turnouts_msg_handler.removeCallbacks(gestureStopped);
        gestureInProgress = false;
    }

    void gestureFailed(MotionEvent event) {
        // end the gesture
        gestureInProgress = false;
    }

    //
    // GestureStopped runs when more than gestureCheckRate milliseconds
    // elapse between onGesture events (i.e. press without movement).
    //
    @SuppressLint("Recycle")
    private Runnable gestureStopped = new Runnable() {
        @Override
        public void run() {
            if (gestureInProgress) {
                // end the gesture
                gestureInProgress = false;
                // create a MOVE event to trigger the underlying control
                if (turnoutsView != null) {
                    // use uptimeMillis() rather than 0 for time in
                    // MotionEvent.obtain() call in throttle gestureStopped:
                    MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, gestureStartX,
                            gestureStartY, 0);
                    try {
                        turnoutsView.dispatchTouchEvent(event);
                    } catch (IllegalArgumentException e) {
                        Log.d("Engine_Driver", "gestureStopped trigger IllegalArgumentException, OS " + android.os.Build.VERSION.SDK_INT);
                    }
                }
            }
        }
    };

}
