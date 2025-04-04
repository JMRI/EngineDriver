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

import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
//import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.screen_swipe_index_type;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.type.sort_type;

public class routes extends AppCompatActivity implements android.gesture.GestureOverlayView.OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp

    private SharedPreferences prefs;

    private ArrayList<HashMap<String, String>> routesFullList;
    private ArrayList<HashMap<String, String>> routes_list;
    private SimpleAdapter routes_list_adapter;
    private ArrayList<String> locationList;
    private ArrayAdapter<String> locationListAdapter;
    private static String location = null;
    private Spinner locationSpinner;

//    private GestureDetector myGesture;
    private Menu RMenu;

    private LinearLayout screenNameLine;
    private Toolbar toolbar;
    private LinearLayout statusLine;
    private int toolbarHeight;

    ListView routes_lv;
    protected View routesView;
    protected GestureOverlayView routesOverlayView;
    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    protected boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;

    public void refresh_route_view() {
//        Log.d("Engine_Driver", "routes: refresh_route_view()");

        boolean hidesystemroutes = prefs.getBoolean("hide_system_route_names_preference",
                getResources().getBoolean(R.bool.prefHideSystemRouteNamesDefaultValue));

        //specify logic for sort comparison (by username/in)
        Comparator<HashMap<String, String>> route_comparator = new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
//                return arg0.get("rt_user_name").compareTo(arg1.get("rt_user_name"));    //*** was compareToIgnoreCase
                int rslt;
                String a;
                String b;
                switch (mainapp.routesOrder) {
                    case sort_type.NAME: {
                    a = threaded_application.formatNumberInName(arg0.get("rt_user_name"));
                    b = threaded_application.formatNumberInName(arg1.get("rt_user_name"));
                        break;
                    }
                    case sort_type.ID: {
                    a = threaded_application.formatNumberInName(arg0.get("rt_system_name"));
                    b = threaded_application.formatNumberInName(arg1.get("rt_system_name"));
                        break;
                    }
                    case sort_type.POSITION:
                    default: {
                        a = threaded_application.formatNumberInName(arg0.get("rt_pos"));
                        b = threaded_application.formatNumberInName(arg1.get("rt_pos"));
                        break;
                    }
                }
                rslt = a.compareTo(b);
                return rslt;    //*** was compareToIgnoreCase()
            }
        };

        //clear and rebuild
        routesFullList.clear();
        locationList.clear();
        if (mainapp.routeStateNames != null) {  //not allowed
            if (mainapp.rt_user_names != null) { //none defined
                int pos = 0;
                String del = prefs.getString("DelimiterPreference", getApplicationContext().getResources().getString(R.string.prefDelimiterDefaultValue));
                boolean hideIfNoUserName = prefs.getBoolean("HideIfNoUserNamePreference", getResources().getBoolean(R.bool.prefHideIfNoUserNameDefaultValue));
                for (String username : mainapp.rt_user_names) {
                    boolean hasUserName = (username != null && !username.equals(""));
                    if (hasUserName || !hideIfNoUserName) {  //skip routes without usernames if pref is set
                        //get values from global array
                        String systemName = mainapp.routeSystemNames[pos];
                        String currentState = mainapp.routeStates[pos];
                        String currentDCCEXstate = String.valueOf(mainapp.routeDccexStates[pos]);
                        String currentDCCEXlabel = mainapp.routeDccexLabels[pos];

                        String currentstatedesc = mainapp.routeStateNames.get(currentState);
                        if (currentstatedesc == null) {
                            currentstatedesc = "   ???";
                        }

                        if (!currentDCCEXstate.equals("-1") ) { // is DCC-EX
                            currentstatedesc = currentDCCEXlabel;
                        }

                        if (!currentDCCEXstate.equals("2")) { // not hidden
                            //put values into temp hashmap
                            HashMap<String, String> hm = new HashMap<>();
                            if (hasUserName)
                                hm.put("rt_user_name", username);
                            else
                                hm.put("rt_user_name", systemName);
                            hm.put("rt_system_name_hidden", systemName);
                            if (!hidesystemroutes) {  //check prefs for show or not show this
                                hm.put("rt_system_name", systemName);
                            }
                            hm.put("rt_current_state_desc", currentstatedesc);
                            hm.put("rt_current_dccex_state", currentDCCEXstate);
                            hm.put("rt_pos", Integer.toString(pos));
                            routesFullList.add(hm);

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
                    }
                    pos++;
                }
            }
        }
        updateRouteEntry();

        //sort lists by username
        Collections.sort(routesFullList, route_comparator);
        Collections.sort(locationList);
        locationList.add(0, getString(R.string.location_all));   // this entry goes at the top of the list
        locationListAdapter.notifyDataSetChanged();
        if (!locationList.contains(location))
            location = getString(R.string.location_all);
        locationSpinner.setSelection(locationListAdapter.getPosition(location));

        filterRouteView();
    }

    private void setDCCEXbuttonStates() {
        ListView listView = findViewById(R.id.routes_list);

        for (int i=0; i<listView.getChildCount(); i++) {
            LinearLayout itemLayout = (LinearLayout) listView.getChildAt(i);
            TextView textView = (TextView) itemLayout.getChildAt(2);
            Button button = (Button) itemLayout.getChildAt(1);
            String state = (String) textView.getText();
            switch (state) {
                case "0":
                default:
                    button.setEnabled(true);
                    button.setSelected(false);
                    break;
                case "1":
                    button.setEnabled(true);
                    button.setSelected(true);
                    break;
                case "2":
                case "4":
                    button.setEnabled(false);
                    button.setSelected(false);
                    break;
            }
        }
    }

    private void filterRouteView() {
//        Log.d("Engine_Driver", "routes: filterRouteView()");

        final String loc = location + prefs.getString("DelimiterPreference", getApplicationContext().getResources().getString(R.string.prefDelimiterDefaultValue));
        final boolean useAllLocations = getString(R.string.location_all).equals(location);
        routes_list.clear();
        for (HashMap<String, String> hm : routesFullList) {
            String userName = hm.get("rt_user_name");
            if (useAllLocations || userName.startsWith(loc)) {
                @SuppressWarnings("unchecked")
                HashMap<String, String> hmFilt = (HashMap<String, String>) hm.clone();
                if (!useAllLocations)
                    hmFilt.put("rt_user_name", userName.substring(loc.length()));
                routes_list.add(hmFilt);
            }
        }
        routes_list_adapter.notifyDataSetChanged();  //update the list
    }

    /** @noinspection UnusedReturnValue*/
    private int updateRouteEntry() {
//        Log.d("Engine_Driver", "routes: updateRouteEntry()");

        Button routeButton = findViewById(R.id.route_toggle);
        EditText routeEntry = findViewById(R.id.route_entry);
        String route = routeEntry.getText().toString().trim();
        int txtLen = route.length();
        if (mainapp.routeStateNames != null) {
            routeEntry.setEnabled(true);
            routeButton.setText(getString(R.string.set));
            // don't allow Set button if nothing entered
            routeButton.setEnabled(txtLen > 0);
        } else {
            routeEntry.setEnabled(false);
            routeButton.setEnabled(false);
            if (!routeEntry.getText().toString().equals(getString(R.string.disabled)))
                routeEntry.setText(getString(R.string.disabled));
        }

        if (RMenu != null) {
            mainapp.displayEStop(RMenu);
        }

        return txtLen;
    }

    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class routes_handler extends Handler {

        public routes_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Log.d("Engine_Driver", "routes_handler: handleMessage("+msg.obj.toString()+")");

            switch (msg.what) {
                case message_type.WIT_CON_RECONNECT:
                case message_type.ROUTE_LIST_CHANGED:
                    refresh_route_view();
                    break;
                case message_type.RESPONSE: {
                    String response_str = msg.obj.toString();

                    if (response_str.length() >= 3) {
                        String com1 = response_str.substring(0, 3);
//                        //refresh routes if any have changed sta
//                        if ("PRL".equals(com1)) { //handle new route list
//                            refresh_route_view();
//                        } else if ("PRA".equals(com1)) { //handle change to individual route entry
//                            refresh_route_view();
//                        }
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateButton(RMenu);
                        }
                    }
                }
                break;
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
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
            }
        }
    }

    private void witRetry(String s) {
//        Log.d("Engine_Driver", "routes: witRetry");

        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    public class button_listener implements View.OnClickListener {
        char whichCommand; //always '2' 

        public button_listener(char new_command) {
            whichCommand = new_command;
        }

        public void onClick(View v) {
            EditText entryv = findViewById(R.id.route_entry);
            String entrytext = entryv.getText().toString().trim();
            if (entrytext.length() > 0) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.ROUTE, whichCommand + entrytext);
            }
            mainapp.buttonVibration();
        }
    }

    public class SortButtonListener implements View.OnClickListener {

        SortButtonListener() {}

        public void onClick(View v) {
            switch (mainapp.routesOrder) {
                case sort_type.NAME:
                    mainapp.routesOrder=sort_type.ID;
                    break;
                case sort_type.ID:
                    mainapp.routesOrder=sort_type.POSITION;
                    break;
                case sort_type.POSITION:
                default:
                    mainapp.routesOrder=sort_type.NAME;
            }
            refresh_route_view();
            mainapp.toastSortType(mainapp.routesOrder);
            mainapp.buttonVibration();
        }
    }

    //handle click for each route's state toggle button
    public class route_state_button_listener implements View.OnClickListener {

        public void onClick(View v) {
            ViewGroup vg = (ViewGroup) v.getParent();  //start with the list item the button belongs to
            ViewGroup rl = (ViewGroup) vg.getChildAt(0);  //get relativelayout that holds systemname and username
            TextView snv = (TextView) rl.getChildAt(1); // get systemname text from 2nd box
            String systemname = snv.getText().toString();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.ROUTE, '2' + systemname);  // 2=toggle
            mainapp.buttonVibration();
        }
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Log.d("Engine_Driver", "routes: onCreate");

        mainapp = (threaded_application) getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        mainapp.applyTheme(this);

        super.onCreate(savedInstanceState);

        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        setContentView(R.layout.routes);
        //put pointer to this activity's handler in main app's shared variable
        mainapp.routes_msg_handler = new routes_handler(Looper.getMainLooper());

        getDefaultSortOrderRoutes();

        routesFullList = new ArrayList<>();
        //Set up a list adapter to allow adding the list of recent connections to the UI.
        routes_list = new ArrayList<>();
        routes_list_adapter = new SimpleAdapter(this, routes_list, R.layout.routes_item,
                new String[]{"rt_user_name", "rt_system_name_hidden", "rt_system_name", "rt_current_state_desc", "rt_current_dccex_state"},
                new int[]{R.id.rt_user_name, R.id.rt_system_name_hidden, R.id.rt_system_name, R.id.rt_current_state_desc, R.id.rt_current_dccex_state}) {
            //set up listener for each state button
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                if (row != null) {
                    Button b = row.findViewById(R.id.rt_current_state_desc);
                    b.setOnClickListener(new route_state_button_listener());
                }
                return row;
            }
        };

        routes_lv = findViewById(R.id.routes_list);
        routes_lv.setAdapter(routes_list_adapter);

        if (mainapp.isDCCEX) {
            // need to setup a listerner so that we can update the buttons states AFTER the list has been fully updated
            ViewTreeObserver viewTreeObserver = routes_lv.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    setDCCEXbuttonStates();
                }
            });
        }

//        OnTouchListener gestureListener = new ListView.OnTouchListener() {
//            public boolean onTouch(View v, @NonNull MotionEvent event) {
////                if (event.getAction() == MotionEvent.ACTION_DOWN) {
////                    mainapp.buttonVibration();
////                }
//                return myGesture != null && myGesture.onTouchEvent(event);
//            }
//        };
//        routes_lv.setOnTouchListener(gestureListener);

        EditText rte = findViewById(R.id.route_entry);
        rte.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                updateRouteEntry();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        rte.setOnEditorActionListener(new OnEditorActionListener() {
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
        Button b = findViewById(R.id.route_toggle);
        button_listener click_listener = new button_listener('2');
        b.setOnClickListener(click_listener);

        locationList = new ArrayList<>();
        locationSpinner = findViewById(R.id.routes_location);
        locationListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationList);
        locationListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationListAdapter);

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                location = parent.getSelectedItem().toString();
                filterRouteView();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //update route list
        refresh_route_view();

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        routesView = findViewById(R.id.routes_screen);
        // enable swipe/fling detection if enabled in Prefs
        routesOverlayView = findViewById(R.id.routes_overlay);
        routesOverlayView.addOnGestureListener(this);
        routesOverlayView.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        // setup the sort button
        b = findViewById(R.id.routes_sort);
        SortButtonListener sortButtonListener = new SortButtonListener();
        b.setOnClickListener(sortButtonListener);

        mainapp.prefFullScreenSwipeArea = prefs.getBoolean("prefFullScreenSwipeArea",
                getResources().getBoolean(R.bool.prefFullScreenSwipeAreaDefaultValue));

    } // end onCreate


    @Override
    public void onResume() {
//        Log.d("Engine_Driver", "routes: onResume");

        mainapp.applyTheme(this);

        super.onResume();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        //restore view to last known scroll position
        ListView lv = findViewById(R.id.routes_list);
        lv.setSelectionFromTop(mainapp.routes_list_position, 0);

        if (RMenu != null) {
            mainapp.displayEStop(RMenu);
            mainapp.displayPowerStateMenuButton(RMenu);
            mainapp.displayThrottleMenuButton(RMenu, "swipe_through_routes_preference");
            mainapp.displayFlashlightMenuButton(RMenu);
            mainapp.setFlashlightButton(RMenu);
        }
        setActivityTitle();

        updateRouteEntry(); // enable/disable button
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!mainapp.setActivityOrientation(this)) { //set screen orientation based on prefs
            Intent in = new Intent().setClass(this, web_activity.class);      // if autoWeb and landscape, switch to Web activity
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
//        Log.d("Engine_Driver", "routes: onDestroy");

        super.onDestroy();

        if (mainapp.routes_msg_handler != null) {
            mainapp.routes_msg_handler.removeCallbacks(gestureStopped);
            mainapp.routes_msg_handler.removeCallbacksAndMessages(null);
            mainapp.routes_msg_handler = null;
        } else {
            Log.d("Engine_Driver", "onDestroy: mainapp.routes_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    @Override
    public void onPause() {
//        Log.d("Engine_Driver", "routes: onPause");

        super.onPause();
        //save scroll position for later restore
        ListView lv = findViewById(R.id.routes_list);
        mainapp.routes_list_position = (lv == null ? 0 : lv.getFirstVisiblePosition());

        //make sure the soft keyboard is closed
        EditText rte = findViewById(R.id.route_entry);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && rte != null) {
            imm.hideSoftInputFromWindow(rte.getWindowToken(), 0);
        }
    }

    //Always go to throttle activity if back button pressed
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            //Log.d("Engine_Driver","routes.onKeyDown() KEYCODE_BACK");
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
            return true;
        }
        mainapp.exitDoubleBackButtonInitiated = 0;
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.d("Engine_Driver", "routes: onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.routes_menu, menu);
        RMenu = menu;
        mainapp.actionBarIconCountRoutes = 0;
        mainapp.displayEStop(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.displayThrottleMenuButton(menu, "swipe_through_routes_preference");
        mainapp.setPowerMenuOption(menu);
        mainapp.setDCCEXMenuOption(menu);
        mainapp.displayDccExButton(menu);
        mainapp.setWithrottleCvProgrammerMenuOption(menu);
        mainapp.setPowerStateButton(menu);
        mainapp.setWebMenuOption(menu);
        mainapp.setTurnoutsMenuOption(menu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
//        mainapp.displayMenuSeparator(menu, this, mainapp.actionBarIconCountRoutes);

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.d("Engine_Driver", "routes: onOptionsItemSelected");

        // Handle all of the possible menu actions.
        Intent in;
        if ( (item.getItemId() == R.id.throttle_button_mnu )
        || (item.getItemId() == R.id.throttle_mnu) ) {
            in = mainapp.getThrottleIntent();
            startACoreActivity(this, in, false, 0);
            if (item.getItemId() == R.id.throttle_button_mnu ) { mainapp.buttonVibration(); }
            return true;
        } else if (item.getItemId() == R.id.turnouts_mnu) {
            in = new Intent().setClass(this, turnouts.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if (item.getItemId() == R.id.web_mnu) {
            in = new Intent().setClass(this, web_activity.class);
            startACoreActivity(this, in, false, 0);
            mainapp.webMenuSelected = true;
            return true;
        } else if (item.getItemId() == R.id.exit_mnu) {
            mainapp.checkAskExit(this);
            return true;
        } else if (item.getItemId() == R.id.power_control_mnu) {
            in = new Intent().setClass(this, power_control.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.settings_mnu) {
            in = new Intent().setClass(this, SettingsActivity.class);
            startActivityForResult(in, 0);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if ( (item.getItemId() == R.id.dcc_ex_button) || (item.getItemId() == R.id.dcc_ex_mnu) ) {
            in = new Intent().setClass(this, dcc_ex.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.logviewer_menu) {
            Intent logviewer = new Intent().setClass(this, LogViewerActivity.class);
            startActivity(logviewer);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.about_mnu) {
            in = new Intent().setClass(this, about_page.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.EmerStop) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(RMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlight(this, RMenu);
            mainapp.buttonVibration();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    //handle return from menu items
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d("Engine_Driver", "routes: onActivityResult");
        //since we always do the same action no need to distinguish between requests
        refresh_route_view();
    }

    private void disconnect() {
        this.finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    //	set the title, optionally adding the current time.
    private void setActivityTitle() {
        if (mainapp.getFastClockFormat() > 0)
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    "",
                    getApplicationContext().getResources().getString(R.string.app_name_routes_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_routes),
                    "");
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

        toolbarHeight = mainapp.getToolbarHeight(toolbar, statusLine,  screenNameLine);
        if (mainapp.prefFullScreenSwipeArea) {  // only allow swipe in the tool bar
            if (gestureStartY > toolbarHeight) {   // not in the toolbar area
                return;
            }
        }

        gestureInProgress = true;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.routes_msg_handler != null)
            mainapp.routes_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
//        Log.d("Engine_Driver", "routes: gestureMove action " + event.getAction());
        if (mainapp!=null && mainapp.routes_msg_handler!=null && gestureInProgress) {
            // stop the gesture timeout timer
            mainapp.routes_msg_handler.removeCallbacks(gestureStopped);

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
                mainapp.routes_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
//        Log.d("Engine_Driver", "routes: gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp!=null) && (mainapp.routes_msg_handler != null) && (gestureInProgress) ) {
            mainapp.routes_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(screen_swipe_index_type.ROUTES, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.routes_msg_handler != null)
            mainapp.routes_msg_handler.removeCallbacks(gestureStopped);
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
    private final Runnable gestureStopped = new Runnable() {
        @Override
        public void run() {
//            Log.d("Engine_Driver", "routes: Runnable");
            if (gestureInProgress) {
                // end the gesture
                gestureInProgress = false;
                // create a MOVE event to trigger the underlying control
                if (routesView != null) {
                    // use uptimeMillis() rather than 0 for time in
                    // MotionEvent.obtain() call in throttle gestureStopped:
                    MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, gestureStartX,
                            gestureStartY, 0);
                    try {
                        routesView.dispatchTouchEvent(event);
                    } catch (IllegalArgumentException e) {
                        Log.d("Engine_Driver", "gestureStopped trigger IllegalArgumentException, OS " + android.os.Build.VERSION.SDK_INT);
                    }
                }
            }
        }
    };

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

    // common startActivity()
    // used for swipes for the main activities only - Throttle, Turnouts, Routs, Web
    void startACoreActivity(Activity activity, Intent in, boolean swipe, float deltaX) {
        if (activity != null && in != null) {
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ActivityOptions options;
            if (deltaX>0) {
                options = ActivityOptions.makeCustomAnimation(context, R.anim.push_right_in, R.anim.push_right_out);
            } else {
                options = ActivityOptions.makeCustomAnimation(context, R.anim.push_left_in, R.anim.push_left_out);
            }
            startActivity(in, options.toBundle());
//            overridePendingTransition(mainapp.getFadeIn(swipe, deltaX), mainapp.getFadeOut(swipe, deltaX));
        }
    }

    void getDefaultSortOrderRoutes() {
        String prefSortOrderRoutes = prefs.getString("prefSortOrderTurnouts", this.getResources().getString(R.string.prefSortOrderRoutesDefaultValue));
        switch (prefSortOrderRoutes) {
            default:
            case "name":
                mainapp.routesOrder = sort_type.NAME;
                break;
            case "id":
                mainapp.routesOrder = sort_type.ID;
                break;
            case "position":
                mainapp.routesOrder = sort_type.POSITION;
                break;
        }
    }
}