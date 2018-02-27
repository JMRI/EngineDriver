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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class routes extends Activity implements OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp

    private SharedPreferences prefs;

    private ArrayList<HashMap<String, String>> routesFullList;
    private ArrayList<HashMap<String, String>> routes_list;
    private SimpleAdapter routes_list_adapter;
    private ArrayList<String> locationList;
    private ArrayAdapter<String> locationListAdapter;
    private static String location = null;
    private Spinner locationSpinner;

    private GestureDetector myGesture;
    private Menu RMenu;
    private boolean navigatingAway = false;     // flag for onPause: set to true when another activity is selected, false if going into background 

    public void refresh_route_view() {

        boolean hidesystemroutes = prefs.getBoolean("hide_system_route_names_preference",
                getResources().getBoolean(R.bool.prefHideSystemRouteNamesDefaultValue));

        //specify logic for sort comparison (by username)
        Comparator<HashMap<String, String>> route_comparator = new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
                return arg0.get("rt_user_name").compareTo(arg1.get("rt_user_name"));    //*** was compareToIgnoreCase
            }
        };

        //clear and rebuild
        routesFullList.clear();
        locationList.clear();
        if (mainapp.rt_state_names != null) {  //not allowed
            if (mainapp.rt_user_names != null) { //none defined
                int pos = 0;
                String del = prefs.getString("DelimiterPreference", getApplicationContext().getResources().getString(R.string.prefDelimiterDefaultValue));
                boolean hideIfNoUserName = prefs.getBoolean("HideIfNoUserNamePreference", getResources().getBoolean(R.bool.prefHideIfNoUserNameDefaultValue));
                for (String username : mainapp.rt_user_names) {
                    boolean hasUserName = (username != null && !username.equals(""));
                    if (hasUserName || !hideIfNoUserName) {  //skip routes without usernames if pref is set
                        //get values from global array
                        String systemname = mainapp.rt_system_names[pos];
                        String currentstate = mainapp.rt_states[pos];
                        String currentstatedesc = mainapp.rt_state_names.get(currentstate);
                        if (currentstatedesc == null) {
                            currentstatedesc = "   ???";
                        }

                        //put values into temp hashmap
                        HashMap<String, String> hm = new HashMap<>();
                        if (hasUserName)
                            hm.put("rt_user_name", username);
                        else
                            hm.put("rt_user_name", systemname);
                        hm.put("rt_system_name_hidden", systemname);
                        if (!hidesystemroutes) {  //check prefs for show or not show this
                            hm.put("rt_system_name", systemname);
                        }
                        hm.put("rt_current_state_desc", currentstatedesc);
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
                        //
                    }
                    pos++;
                }
                //              routes_list_adapter.notifyDataSetChanged();
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

    private void filterRouteView() {
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

    private int updateRouteEntry() {
        Button butSet = (Button) findViewById(R.id.route_toggle);
        EditText rte = (EditText) findViewById(R.id.route_entry);
        String route = rte.getText().toString().trim();
        int txtLen = route.length();
        if (mainapp.rt_state_names != null) {
            rte.setEnabled(true);
            butSet.setText(getString(R.string.set));
            // don't allow Set button if nothing entered
            if (txtLen > 0) {
                butSet.setEnabled(true);
            } else {
                butSet.setEnabled(false);
            }
        } else {
            rte.setEnabled(false);
            butSet.setEnabled(false);
            if (!rte.getText().toString().equals(getString(R.string.disabled)))
                rte.setText(getString(R.string.disabled));
        }

        if (RMenu != null) {
            mainapp.displayEStop(RMenu);
        }

        return txtLen;
    }

    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class routes_handler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE: {
                    String response_str = msg.obj.toString();

                    if (response_str.length() >= 3) {
                        String com1 = response_str.substring(0, 3);
                        //refresh routes if any have changed state or if route list changed
                        if ("PRA".equals(com1) || "PRL".equals(com1)) {
                            refresh_route_view();
                        }
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
                case message_type.WIT_CON_RECONNECT:
                    refresh_route_view();
                    break;
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
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

    public class button_listener implements View.OnClickListener {
        char whichCommand; //always '2' 

        public button_listener(char new_command) {
            whichCommand = new_command;
        }

        public void onClick(View v) {
            EditText entryv = (EditText) findViewById(R.id.route_entry);
            String entrytext = entryv.getText().toString().trim();
            if (entrytext.length() > 0) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.ROUTE, whichCommand + entrytext);
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastRoutesCommandSent) + entrytext,
                        Toast.LENGTH_SHORT).show();

            }
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
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return myGesture.onTouchEvent(event);
    }


    public void setTitleToIncludeThrotName() {
        String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_routes) + "    |    Throttle Name: " +
                prefs.getString("throttle_name_preference", defaultName));
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.d("Engine_Driver","routes.onCreate()");
        super.onCreate(savedInstanceState);

        mainapp = (threaded_application) getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

//      setTitleToIncludeThrotName();

        mainapp.applyTheme(this);

        setContentView(R.layout.routes);
        //put pointer to this activity's handler in main app's shared variable
        mainapp.routes_msg_handler = new routes_handler();
        myGesture = new GestureDetector(this);

        routesFullList = new ArrayList<>();
        //Set up a list adapter to allow adding the list of recent connections to the UI.
        routes_list = new ArrayList<>();
        routes_list_adapter = new SimpleAdapter(this, routes_list, R.layout.routes_item,
                new String[]{"rt_user_name", "rt_system_name_hidden", "rt_system_name", "rt_current_state_desc"},
                new int[]{R.id.rt_user_name, R.id.rt_system_name_hidden, R.id.rt_system_name, R.id.rt_current_state_desc}) {
            //set up listener for each state button
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                if (row != null) {
                    Button b = (Button) row.findViewById(R.id.rt_current_state_desc);
                    b.setOnClickListener(new route_state_button_listener());
                }
                return row;
            }

        };
        ListView routes_lv = (ListView) findViewById(R.id.routes_list);
        routes_lv.setAdapter(routes_list_adapter);

        OnTouchListener gestureListener = new ListView.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return myGesture != null && myGesture.onTouchEvent(event);
            }
        };
        routes_lv.setOnTouchListener(gestureListener);

        EditText rte = (EditText) findViewById(R.id.route_entry);
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
        Button b = (Button) findViewById(R.id.route_toggle);
        button_listener click_listener = new button_listener('2');
        b.setOnClickListener(click_listener);

        //((EditText) findViewById(R.id.route_entry)).setRawInputType(InputType.TYPE_CLASS_NUMBER);

        locationList = new ArrayList<>();
        locationSpinner = (Spinner) findViewById(R.id.routes_location);
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
    }

    @Override
    public void onResume() {
        //Log.d("Engine_Driver","routes.onResume()");
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }

        if (!mainapp.setActivityOrientation(this)) { //set screen orientation based on prefs
            Intent in = new Intent().setClass(this, web_activity.class);      // if autoWeb and landscape, switch to Web activity
            navigatingAway = true;
            startActivity(in);
            this.finish();
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return;
        }

        navigatingAway = false;

        //restore view to last known scroll position
        ListView lv = (ListView) findViewById(R.id.routes_list);
        lv.setSelectionFromTop(mainapp.routes_list_position, 0);

//      setTitleToIncludeThrotName();
        if (RMenu != null) {
            mainapp.displayEStop(RMenu);
            mainapp.displayPowerStateMenuButton(RMenu);
        }
        updateRouteEntry(); // enable/disable button
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        //Log.d("Engine_Driver","routes.onDestroy()");
        mainapp.routes_msg_handler = null;
        super.onDestroy();
    }

    @Override
    public void onPause() {
        //Log.d("Engine_Driver","routes.onPause()");
        super.onPause();
        //save scroll position for later restore
        ListView lv = (ListView) findViewById(R.id.routes_list);
        mainapp.routes_list_position = (lv == null ? 0 : lv.getFirstVisiblePosition());

        //make sure the soft keyboard is closed
        EditText rte = (EditText) findViewById(R.id.route_entry);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && rte != null) {
            imm.hideSoftInputFromWindow(rte.getWindowToken(), 0);
        }

        if (!this.isFinishing() && !navigatingAway) {        //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
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
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null)
            return false;
        float deltaX = e2.getX() - e1.getX();
        float absDeltaX = Math.abs(deltaX);
        if ((absDeltaX > threaded_application.min_fling_distance) &&
                (Math.abs(velocityX) > threaded_application.min_fling_velocity) &&
                (absDeltaX > Math.abs(e2.getY() - e1.getY()))) {
            navigatingAway = true;
            // left to right swipe goes to throttle
            if (deltaX > 0.0) {
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
            }
            // right to left swipe goes to turnouts if enabled in prefs
            else {
                boolean swipeTurnouts = prefs.getBoolean("swipe_through_turnouts_preference", getResources().getBoolean(R.bool.prefSwipeThroughTurnoutsDefaultValue));
                swipeTurnouts = swipeTurnouts && mainapp.isTurnoutControlAllowed();  //also check the allowed flag
                if (swipeTurnouts) {
                    Intent in = new Intent().setClass(this, turnouts.class);
                    startActivity(in);
                }
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.routes_menu, menu);
        RMenu = menu;
        mainapp.displayEStop(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerMenuOption(menu);
        mainapp.setPowerStateButton(menu);
        mainapp.setWebMenuOption(menu);
        mainapp.setTurnoutsMenuOption(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        switch (item.getItemId()) {
            case R.id.throttle_mnu:
                navigatingAway = true;
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
                break;
            case R.id.turnouts_mnu:
                in = new Intent().setClass(this, turnouts.class);
                navigatingAway = true;
                startActivity(in);
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
                break;
            case R.id.web_mnu:
                in = new Intent().setClass(this, web_activity.class);
                navigatingAway = true;
                startActivity(in);
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.exit_mnu:
                mainapp.checkExit(this);
                break;
            case R.id.power_control_mnu:
                in = new Intent().setClass(this, power_control.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.preferences_mnu:
                in = new Intent().setClass(this, preferences.class);
                navigatingAway = true;
                startActivityForResult(in, 0);   // refresh view on return
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.about_mnu:
                in = new Intent().setClass(this, about_page.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                break;
            case R.id.power_layout_button:
                mainapp.powerStateMenuButton();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //handle return from menu items
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //since we always do the same action no need to distinguish between requests
        refresh_route_view();
    }

    private void disconnect() {
        this.finish();
    }

}