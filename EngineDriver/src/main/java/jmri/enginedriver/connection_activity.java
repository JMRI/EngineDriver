/* Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;
import jmri.enginedriver.util.SwipeDetector;

public class connection_activity extends Activity implements PermissionsHelper.PermissionsHelperGrantedCallback {
//    private ArrayList<HashMap<String, String>> connections_list;
    private ArrayList<HashMap<String, String>> discovery_list;
    private SimpleAdapter connection_list_adapter;
    private SimpleAdapter discovery_list_adapter;
    private SharedPreferences prefs;

    //pointer back to application
    private threaded_application mainapp;
    private Menu CMenu;
    //The IP address and port that are used to connect.
    private String connected_hostip;
    private String connected_hostname;
    private int connected_port;

    private static final String demo_host = "jmri.mstevetodd.com";
    private static final String demo_port = "44444";
    private static final String DUMMY_HOST = "999";
    private static final String DUMMY_ADDRESS = "999";
    private static final int DUMMY_PORT = 999;

    private boolean prefHideDemoServer = false;

    private static Method overridePendingTransition;

    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();
    public ImportExportConnectionList importExportConnectionList;

    private Toast connToast = null;
    private boolean isRestarting = false;

    SwipeDetector connectionsListSwipeDetector;

    EditText host_ip;
    EditText port;

    View host_numeric_or_text;
    Button host_numeric;
    Button host_text;

    LinearLayout rootView;
    int rootViewHeight = 0;

    static {
        try {
            overridePendingTransition = Activity.class.getMethod("overridePendingTransition", Integer.TYPE, Integer.TYPE); //$NON-NLS-1$
        } catch (NoSuchMethodException e) {
            overridePendingTransition = null;
        }
    }

    /**
     * Calls Activity.overridePendingTransition if the method is available (>=Android 2.0)
     *
     * @param activity  the activity that launches another activity
     * @param animEnter the entering animation
     * @param animExit  the exiting animation
     */
    public static void overridePendingTransition(Activity activity, int animEnter, int animExit) {
        if (overridePendingTransition != null) {
            try {
                overridePendingTransition.invoke(activity, animEnter, animExit);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    private void connect() {
        navigateToHandler(PermissionsHelper.CONNECT_TO_SERVER);
    }

    //Request connection to the WiThrottle server.
    private void connectImpl() {
        //	  sendMsgErr(0, message_type.CONNECT, connected_hostip, connected_port, "ERROR in ca.connect: comm thread not started.");
        Log.d("Engine_Driver", "in connection_activity.connect()");
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CONNECT, connected_hostip, connected_port);
    }


    private void start_throttle_activity() {
//        Intent throttle = new Intent().setClass(this, throttle.class);
        Intent throttle = mainapp.getThrottleIntent();
        startActivity(throttle);
        this.finish();
        overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void startNewConnectionActivity() {
        // first remove old handler since the new Intent will have its own
        isRestarting = true;        // tell OnDestroy to skip this step since it will run after the new Intent is created
        if (mainapp.connection_msg_handler !=null) {
            mainapp.connection_msg_handler.removeCallbacksAndMessages(null);
            mainapp.connection_msg_handler = null;
        }

        //end current CA Intent then start the new Intent
        Intent newConnection = new Intent().setClass(this, connection_activity.class);
        this.finish();
        startActivity(newConnection);
        overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private enum server_list_type {DISCOVERED_SERVER, RECENT_CONNECTION}

    private class connect_item implements AdapterView.OnItemClickListener {
        final server_list_type server_type;

        connect_item(server_list_type new_type) {
            server_type = new_type;
        }

        //When an item is clicked, connect to the given IP address and port.
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            if(connectionsListSwipeDetector.swipeDetected()) { // check for swipe
                if(connectionsListSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearConnectionsListItem(v, position, id);
//                } else {
                }
            } else {  //no swipe
                switch (server_type) {
                    case DISCOVERED_SERVER:
                    case RECENT_CONNECTION:
                        ViewGroup vg = (ViewGroup) v; //convert to viewgroup for clicked row
                        TextView hip = (TextView) vg.getChildAt(0); // get host ip from 1st box
                        connected_hostip = hip.getText().toString();
                        TextView hnv = (TextView) vg.getChildAt(1); // get host name from 2nd box
                        connected_hostname = hnv.getText().toString();
                        TextView hpv = (TextView) vg.getChildAt(2); // get port from 3rd box
                        connected_port = Integer.valueOf(hpv.getText().toString());
                        break;
                }
                connect();
            }
        }
    }

    private class HostNumericListener implements View.OnClickListener {
        public void onClick(View v) {
            EditText entry = findViewById(R.id.host_ip);
            entry.setInputType(InputType.TYPE_CLASS_PHONE);
            entry.requestFocus();
            host_numeric.setVisibility(View.GONE);
            host_text.setVisibility(View.VISIBLE);
        }
    }

    private class HostTextListener implements View.OnClickListener {
        public void onClick(View v) {
            EditText entry = findViewById(R.id.host_ip);
            entry.setInputType(InputType.TYPE_CLASS_TEXT);
            entry.requestFocus();
            host_numeric.setVisibility(View.VISIBLE);
            host_text.setVisibility(View.GONE);
        }
    }

    private void showHideNumericOrTextButtons(boolean show) {
        View v = findViewById(R.id.host_numeric_or_text);
        if (show) { v.setVisibility(View.VISIBLE); }
        else {v.setVisibility(View.GONE); }
    }

        private class button_listener implements View.OnClickListener {
            public void onClick(View v) {
                EditText entry = findViewById(R.id.host_ip);
                connected_hostip = entry.getText().toString();
                if (connected_hostip.trim().length() > 0) {
                    entry = findViewById(R.id.port);
                    try {
                        connected_port = Integer.valueOf(entry.getText().toString());
                    } catch (Exception except) {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectInvalidPort) + "\n" + except.getMessage(), Toast.LENGTH_SHORT).show();
                        connected_port = 0;
                        return;
                    }
                    connected_hostname = connected_hostip; //copy ip to name
                    connect();
                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectEnterAddress), Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void clearConnectionsListItem(View v, int position, long id) {
            //When a connection swiped , remove it from the list
            ViewGroup vg = (ViewGroup) v; //convert to viewgroup for clicked row
            TextView hip = (TextView) vg.getChildAt(0); // get host ip from 1st box
            //connected_hostip = hip.getText().toString();
            TextView hnv = (TextView) vg.getChildAt(1); // get host name from 2nd box
            //connected_hostname = hnv.getText().toString();
            TextView hpv = (TextView) vg.getChildAt(2); // get port from 3rd box
            //connected_port = Integer.valueOf(hpv.getText().toString());
            //Log.d("Engine_Driver", "connection.longClick " + connected_hostip );
            if (!(hnv.getText().toString().equals(demo_host)) || !(hpv.getText().toString().equals(demo_port))) {
                getConnectionsListImpl(hip.getText().toString(), hpv.getText().toString());
//            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectRemoved), Toast.LENGTH_SHORT).show();
                connected_hostip = DUMMY_ADDRESS;
                connected_hostname = DUMMY_HOST;
                connected_port = DUMMY_PORT;
//            new saveConnectionsList().execute();

                Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
                anim.setDuration(500);
                v.startAnimation(anim);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectRemoved), Toast.LENGTH_SHORT).show();
//                        new saveConnectionsList().execute();
                        importExportConnectionList.saveConnectionsListExecute(mainapp, connected_hostip, connected_hostname, connected_port,"");
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}

                    public void run() {}
                });

            } else {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectRemoveDemoHostError), Toast.LENGTH_SHORT).show();
            }
        }


        //Method to connect to first discovered WiThrottle server.
        private void connectA() {
            try {
                if (discovery_list.get(0) != null) {
                    HashMap<String, String> tm = discovery_list.get(0);

                    connected_hostip = tm.get("ip_address");

                    if (connected_hostip.trim().length() > 0) {
                        try {
                            connected_port = Integer.valueOf(tm.get("port"));
                        } catch (Exception except) {
                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectInvalidPort) + "\n" + except.getMessage(), Toast.LENGTH_SHORT).show();
                            connected_port = 0;
                            return;
                        }
                        connected_hostname = tm.get("host_name"); //copy ip to name
                        connect();
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectConnected, connected_hostname, Integer.toString(connected_port)), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectEnterAddress), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception ex) {
                //Catches exception, if no discovered server exists.
                //Does Nothing On purpose.
            }
        }

        //Handle messages from the communication thread back to the UI thread.
        @SuppressLint("HandlerLeak")
        private class ui_handler extends Handler {
            @SuppressWarnings("unchecked")

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case message_type.SERVICE_RESOLVED:
                        HashMap<String, String> hm = (HashMap<String, String>) msg.obj;  //payload is already a hashmap
                        String found_host_name = hm.get("host_name");
                        boolean entryExists = false;

                        //stop if new address is already in the list
                        HashMap<String, String> tm;
                        for (int index = 0; index < discovery_list.size(); index++) {
                            tm = discovery_list.get(index);
                            if (tm.get("host_name").equals(found_host_name)) {
                                entryExists = true;
                                break;
                            }
                        }
                        if (!entryExists) {                // if new host, add to discovered list on screen
                            discovery_list.add(hm);
                            discovery_list_adapter.notifyDataSetChanged();

                            if (prefs.getBoolean("connect_to_first_server_preference", false)) {
                                connectA();
                            }
                        }
                        break;

                    case message_type.SERVICE_REMOVED:
                        //look for name in list
                        String removed_host_name = msg.obj.toString();
                        for (int index = 0; index < discovery_list.size(); index++) {
                            tm = discovery_list.get(index);
                            if (tm.get("host_name").equals(removed_host_name)) {
                                discovery_list.remove(index);
                                discovery_list_adapter.notifyDataSetChanged();
                                break;
                            }
                        }
                        break;

                    case message_type.CONNECTED:
                        //use asynctask to save the updated connections list to the connections_list.txt file
//                        new saveConnectionsList().execute();
                        importExportConnectionList.saveConnectionsListExecute(mainapp, connected_hostip, connected_hostname, connected_port,"");
                        mainapp.connectedHostName = connected_hostname;
                        mainapp.connectedHostip = connected_hostip;
                        mainapp.connectedPort = connected_port;

                        loadSharedPreferencesFromFile();

                        start_throttle_activity();
                        break;

                    case message_type.RESTART_APP:
                        startNewConnectionActivity();
                        break;

                    case message_type.DISCONNECT:
                    case message_type.SHUTDOWN:
                        saveSharedPreferencesToFile();
                        mainapp.connectedHostName = "";
                        shutdown();
                        break;
                }
            }
        }

        /**
         * Called when the activity is first created.
         */
        @SuppressLint("ApplySharedPref")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //    timestamp = SystemClock.uptimeMillis();
            Log.d("Engine_Driver", "connection.onCreate ");
            mainapp = (threaded_application) this.getApplication();
            mainapp.connection_msg_handler = new ui_handler();

            connToast = Toast.makeText(this, "", Toast.LENGTH_LONG);    // save toast obj so it can be cancelled
            // setTitle(getApplicationContext().getResources().getString(R.string.app_name_connect));	//set title to long form of label

            //check for "default" throttle name and make it more unique
            prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

            if (!prefs.getString("prefRunIntro", "0").equals(threaded_application.INTRO_VERSION)) {
                Intent intent = new Intent(this, intro_activity.class); // Call the AppIntro java class
                startActivity(intent);
                mainapp.introIsRunning = true;
            }

            mainapp.applyTheme(this);
            setTitle(getApplicationContext().getResources().getString(R.string.app_name_connect)); // needed in case the langauge was changed from the default

            setContentView(R.layout.connection);

            prefHideDemoServer = prefs.getBoolean("prefHideDemoServer", getResources().getBoolean(R.bool.prefHideDemoServerDefaultValue));

            //Set up a list adapter to allow adding discovered WiThrottle servers to the UI.
            discovery_list = new ArrayList<>();
            discovery_list_adapter = new SimpleAdapter(this, discovery_list, R.layout.connections_list_item,
                    new String[]{"ip_address", "host_name", "port"},
                    new int[]{R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
            ListView discover_list = findViewById(R.id.discovery_list);
            discover_list.setAdapter(discovery_list_adapter);
            discover_list.setOnItemClickListener(new connect_item(server_list_type.DISCOVERED_SERVER));

            importExportConnectionList = new ImportExportConnectionList(prefs);
            //Set up a list adapter to allow adding the list of recent connections to the UI.
//            connections_list = new ArrayList<>();
            connection_list_adapter = new SimpleAdapter(this, importExportConnectionList.connections_list, R.layout.connections_list_item,
                    new String[]{"ip_address", "host_name", "port"},
                    new int[]{R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
            ListView conn_list = findViewById(R.id.connections_list);
            conn_list.setAdapter(connection_list_adapter);
            conn_list.setOnTouchListener(connectionsListSwipeDetector = new SwipeDetector());
            conn_list.setOnItemClickListener(new connect_item(server_list_type.RECENT_CONNECTION));

            // suppress popup keyboard until EditText is touched
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            host_ip = findViewById(R.id.host_ip);
            port = findViewById(R.id.port);
            host_ip.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    checkIP();
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
            });
            host_ip.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        showHideNumericOrTextButtons(true);
                    }
                }
            });
            port.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        showHideNumericOrTextButtons(false);
                    }
                }
            });

            host_numeric_or_text = findViewById(R.id.host_numeric_or_text);
            host_numeric = findViewById(R.id.host_numeric);
            HostNumericListener host_numeric_listener = new HostNumericListener();
            host_numeric.setOnClickListener(host_numeric_listener);
            host_numeric.setVisibility(View.GONE);
            host_text = findViewById(R.id.host_text);
            HostTextListener host_text_listener = new HostTextListener();
            host_text.setOnClickListener(host_text_listener);

            //Set the button callback.
            Button button = findViewById(R.id.connect);
            button_listener click_listener = new button_listener();
            button.setOnClickListener(click_listener);

            set_labels();
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            threaded_application.min_fling_distance = (int) (threaded_application.SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
            threaded_application.min_fling_velocity = (int) (threaded_application.SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);

            if (prefs.getBoolean("prefForcedRestart", false)) { // if forced restart from the preferences
                prefs.edit().putBoolean("prefForcedRestart", false).commit();

                int prefForcedRestartReason = prefs.getInt("prefForcedRestartReason", threaded_application.FORCED_RESTART_REASON_NONE);
                Log.d("Engine_Driver", "connection: Forced Restart Reason: " + prefForcedRestartReason);
                if (mainapp.prefsForcedRestart(prefForcedRestartReason)) {
                    Intent in = new Intent().setClass(this, preferences.class);
                    startActivityForResult(in, 0);
                    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                }
            }

            rootView = findViewById(R.id.connection_view);
            rootView.requestFocus();
            rootViewHeight = rootView.getHeight();
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (rootViewHeight!=0) {
                        int heightDiff = rootViewHeight - rootView.getHeight();
                        if (heightDiff < -400) {  //keyboard closed
                            rootView.requestFocus();
                            showHideNumericOrTextButtons(false);
                        }
                    }
                    rootViewHeight = rootView.getHeight();
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            if (this.isFinishing()) {        //if finishing, expedite it
                return;
            }
//        if (this.runIntro) {        //if going to run the intro, expedite it
            if (mainapp.introIsRunning) {        //if going to run the intro, expedite it
                return;
            }

            getWifiInfo();

            mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
            //start up server discovery listener
            //	    sendMsgErr(0, message_type.SET_LISTENER, "", 1, "ERROR in ca.onResume: comm thread not started.") ;
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 1);
            //populate the ListView with the recent connections
            getConnectionsList();
            set_labels();
            mainapp.cancelForcingFinish();            // if fresh start or restart after being killed in the bkg, indicate app is running again
            //start up server discovery listener again (after a 1 second delay)
            //TODO: this is a rig, figure out why this is needed for ubuntu servers
            //	    sendMsgErr(1000, message_type.SET_LISTENER, "", 1, "ERROR in ca.onResume: comm thread not started.") ;
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 1);

            if (prefs.getBoolean("connect_to_first_server_preference", false)) {
                connectA();
            }
            if (CMenu != null) {
                mainapp.displayFlashlightMenuButton(CMenu);
                mainapp.setFlashlightButton(CMenu);
            }
        }  //end of onResume

        @Override
        public void onPause() {
            super.onPause();
//        runIntro = false;
            if (connToast != null) {
                connToast.cancel();
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.d("Engine_Driver", "connection.onStop()");
            //shutdown server discovery listener
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 0);
        }

        @Override
        public void onDestroy() {
            Log.d("Engine_Driver", "connection.onDestroy() called");
            super.onDestroy();

            if (!isRestarting) {
                if (mainapp.connection_msg_handler != null) {
                    mainapp.connection_msg_handler.removeCallbacksAndMessages(null);
                    mainapp.connection_msg_handler = null;
                } else {
                    Log.d("Engine_Driver", "onDestroy: mainapp.connection_msg_handler is null. Unable to removeCallbacksAndMessages");
                }
            }
            else {
                isRestarting = false;
            }
            CMenu = null;
            connectionsListSwipeDetector = null;
            prefs = null;
            discovery_list_adapter = null;
            connection_list_adapter = null;
            mainapp = null;
        }

        private void shutdown() {
            this.finish();
        }

        private void set_labels() {
            SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
            TextView v = findViewById(R.id.ca_footer);
            String s = prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
            v.setText(getString(R.string.throttle_name, s));

            //sets the tile to include throttle name.
            //String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
            setTitle(getApplicationContext().getResources().getString(R.string.app_name_connect));// + "    |    Throttle Name: " + prefs.getString("throttle_name_preference", defaultName));
        }

        /**
         * retrieve some wifi details, stored in mainapp as client_ssid, client_address and client_address_inet4
         */
        void getWifiInfo() {
            int intaddr;
            mainapp.client_address_inet4 = null;
            mainapp.client_address = null;
            //set several wifi-related variables, requesting permission if required
            try {
                WifiManager wifi = (WifiManager) connection_activity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiinfo = wifi.getConnectionInfo();
                intaddr = wifiinfo.getIpAddress();
                if (intaddr != 0) {
                    byte[] byteaddr = new byte[]{(byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff),
                            (byte) (intaddr >> 24 & 0xff)};
                    mainapp.client_address_inet4 = (Inet4Address) Inet4Address.getByAddress(byteaddr);
                    mainapp.client_address = mainapp.client_address_inet4.toString().substring(1);      //strip off leading /
                }
                //we must have location permissions to get SSID.
                PermissionsHelper phi = PermissionsHelper.getInstance();
                if (!phi.isPermissionGranted(connection_activity.this, PermissionsHelper.ACCESS_COARSE_LOCATION)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        phi.requestNecessaryPermissions(connection_activity.this, PermissionsHelper.ACCESS_COARSE_LOCATION);
                    }
                }
                mainapp.client_ssid = wifiinfo.getSSID();
                if (mainapp.client_ssid != null && mainapp.client_ssid.startsWith("\"") && mainapp.client_ssid.endsWith("\"")) {
                    mainapp.client_ssid = mainapp.client_ssid.substring(1, mainapp.client_ssid.length() - 1);
                }
                //determine if currently using mobile connection or wifi
                ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo nInfo = cm.getActiveNetworkInfo();
                switch (nInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        mainapp.client_type = "WIFI";
                        break;
                    case ConnectivityManager.TYPE_MOBILE:
                        mainapp.client_type = "MOBILE";
                        break;
                    case ConnectivityManager.TYPE_DUMMY:
                        mainapp.client_type = "DUMMY";
                        break;
                    case ConnectivityManager.TYPE_VPN:
                        mainapp.client_type = "VPN";
                        break;
                    default:
                        mainapp.client_type = "other";
                        break;
                }
                Log.d("Engine_Driver", "network type=" + nInfo.getType() + " " + mainapp.client_type);

            } catch (Exception except) {
                Log.e("Engine_Driver", "getWifiInfo - error getting IP addr: " + except.getMessage());
            }

        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.connection_menu, menu);
            CMenu = menu;
            mainapp.displayFlashlightMenuButton(menu);
            mainapp.setFlashlightButton(menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle all of the possible menu actions.
            Intent in;
            switch (item.getItemId()) {
                case R.id.exit_mnu:
                    mainapp.checkExit(this);
                    return true;
                case R.id.preferences_mnu:
                    in = new Intent().setClass(this, preferences.class);
                    startActivityForResult(in, 0);
                    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                    return true;
                case R.id.about_mnu:
                    in = new Intent().setClass(this, about_page.class);
                    startActivity(in);
                    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                    return true;
                case R.id.ClearconnList:
                    clearConnectionsList();
                    getConnectionsList();
                    return true;
                case R.id.flashlight_button:
                    mainapp.toggleFlashlight(this, CMenu);
                    return true;
                case R.id.logviewer_menu:
                    Intent logviewer = new Intent().setClass(this, LogViewerActivity.class);
                    startActivity(logviewer);
                    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                    return true;
                case R.id.intro_mnu:
                    in = new Intent().setClass(this, intro_activity.class);
                    startActivity(in);
                    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        //handle return from menu items
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            //only one activity with results here
            set_labels();
        }

        // Handle pressing of the back button to request exit
        @Override
        public boolean onKeyDown(int key, KeyEvent event) {
            if (key == KeyEvent.KEYCODE_BACK) {
                mainapp.checkExit(this);
                return true;
            }
            return (super.onKeyDown(key, event));
        }


        private void checkIP() {
            String tempIP = host_ip.getText().toString().trim();
            if (tempIP.contains(":")) { //If a colon has been entered in the host field, remove and jump to the port field
                host_ip.setText(tempIP.replace(":", ""));
                port.requestFocus();
            }
        }

        /***
         private void sendMsgErr(long delayMs, int msgType, String msgBody, int msgArg1, String errMsg) {
         if(!mainapp.sendMsgDelay(mainapp.comm_msg_handler, delayMs, msgType, msgBody, msgArg1, 0)) {
         Log.e("Engine_Driver",errMsg) ;
         Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
         }
         }
         ***/

        //save connections list in background using asynctask
//        private class saveConnectionsList extends AsyncTask<Void, Void, String> {
//            @Override
//            protected String doInBackground(Void... params) {
//
//                String errMsg = "";
//                //exit if values not set, avoid NPE reported to Play Store
//                if (connected_hostip == null || connected_port == 0) return errMsg;
//
//                //if no SD Card present then nothing to do
//                if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
//                    return errMsg;
//                try {
//                    File path = Environment.getExternalStorageDirectory();
//                    File engine_driver_dir = new File(path, "engine_driver");
//                    //noinspection ResultOfMethodCallIgnored
//                    engine_driver_dir.mkdir();            // create directory if it doesn't exist
//
//                    File connections_list_file = new File(path, "engine_driver/connections_list.txt");
//                    PrintWriter list_output = new PrintWriter(connections_list_file);
//
//                    if (!(connected_hostip.equals(DUMMY_ADDRESS)) || (connected_port != DUMMY_PORT)) {  // will have been called from the remove connection longClick so ignore the current connection values
//                        //Write selected connection to file, then write all others (skipping selected if found)
//                        list_output.format("%s:%s:%d\n", connected_hostname, connected_hostip, connected_port);
//                    }
//
//                    SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
//                    String smrc = prefs.getString("maximum_recent_connections_preference", ""); //retrieve pref for max recents to show
//                    if (smrc.equals("")) { //if no value or entry removed, set to default
//                        smrc = getApplicationContext().getResources().getString(R.string.prefMaximumRecentConnectionsDefaultValue);
//                    }
//                    int mrc = 10;
//                    try {
//                        mrc = Integer.parseInt(smrc);
//                    } catch (NumberFormatException ignored) {
//                    }
//                    int clEntries = Math.min(connections_list.size(), mrc);  //don't keep more entries than specified in preference
//                    for (int i = 0; i < clEntries; i++) {  //loop thru entries from connections list, up to max in prefs
//                        HashMap<String, String> t = connections_list.get(i);
//                        String li = t.get("ip_address");
//                        String lh = t.get("host_name");
//                        Integer lp = Integer.valueOf(t.get("port"));
//                        if (!connected_hostip.equals(li) || (connected_port != lp)) {  //write it out if not same as selected
//                            list_output.format("%s:%s:%d\n", lh, li, lp);
//                        }
//                    }
//                    list_output.flush();
//                    list_output.close();
//                } catch (IOException except) {
//                    errMsg = except.getMessage();
//                }
//                return errMsg;
//            }
//
//            @Override
//            protected void onPostExecute(String errMsg) {
//                if (errMsg.length() > 0)
//                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectErrorSavingRecentConnection) + " " + errMsg, Toast.LENGTH_SHORT).show();
//            }
//        }

        private void clearConnectionsList() {
            navigateToHandler(PermissionsHelper.CLEAR_CONNECTION_LIST);
        }

        //Jeffrey M added 7/3/2013
        //Clears recent connection list.
        private void clearConnectionsListImpl() {
            //if no SD Card present then nothing to do
            if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                Toast.makeText(getApplicationContext(), "Error no recent connections exist", Toast.LENGTH_SHORT).show();
            } else {
                File sdcard_path = Environment.getExternalStorageDirectory();
                File connections_list_file = new File(sdcard_path, "engine_driver/connections_list.txt");

                if (connections_list_file.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    connections_list_file.delete();
                    importExportConnectionList.connections_list.clear();
                }
            }
        }

        private void getConnectionsList() {
            navigateToHandler(PermissionsHelper.READ_CONNECTION_LIST);
        }

        private void getConnectionsListImpl(String addressToRemove, String portToRemove) {
            boolean foundDemoHost = false;
            importExportConnectionList.connections_list.clear();
            String errMsg;

            if (prefs.getString("prefRunIntro", "0").equals(threaded_application.INTRO_VERSION)) { // if the intro hasn't run yet, the permissions may not have been granted yet, so don't red the SD card,

                if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                    //alert user that recent connections list requires SD Card
                    TextView v = findViewById(R.id.recent_connections_heading);
                    v.setText(getString(R.string.ca_recent_conn_notice));
                } else {
                    importExportConnectionList.getConnectionsList(addressToRemove, portToRemove);
//                    try {
//                        File sdcard_path = Environment.getExternalStorageDirectory();
//                        File connections_list_file = new File(sdcard_path, "engine_driver/connections_list.txt");
//
//                        if (connections_list_file.exists()) {
//                            BufferedReader list_reader = new BufferedReader(new FileReader(connections_list_file));
//                            while (list_reader.ready()) {
//                                String line = list_reader.readLine();
//                                String ip_address;
//                                String host_name;
//                                String port_str;
//                                Integer port = 0;
//                                List<String> parts = Arrays.asList(line.split(":", 3)); //split record from file, max of 3 parts
//                                if (parts.size() > 1) {  //skip if not split
//                                    if (parts.size() == 2) {  //old style, use part 1 for ip and host
//                                        host_name = parts.get(0);
//                                        ip_address = parts.get(0);
//                                        port_str = parts.get(1);
//                                    } else {                          //new style, get all 3 parts
//                                        host_name = parts.get(0);
//                                        ip_address = parts.get(1);
//                                        port_str = parts.get(2);
//                                    }
//                                    try {  //attempt to convert port to integer
//                                        port = Integer.decode(port_str);
//                                    } catch (Exception ignored) {
//                                    }
//                                    if (!(ip_address.equals(addressToRemove)) || !(port.toString().equals(portToRemove))) {
//                                        if (port > 0) {  //skip if port not converted to integer
//
//                                            if (!prefHideDemoServer || !(host_name.equals(demo_host) && port.toString().equals(demo_port))) {
//                                                HashMap<String, String> hm = new HashMap<>();
//                                                hm.put("ip_address", ip_address);
//                                                hm.put("host_name", host_name);
//                                                hm.put("port", port.toString());
//                                                if (!connections_list.contains(hm)) {    // suppress dups
//                                                    connections_list.add(hm);
//                                                }
//                                            }
//                                            if (host_name.equals(demo_host) && port.toString().equals(demo_port)) {
//                                                foundDemoHost = true;
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            list_reader.close();
//
//                            if (((foundDemoHost) && (connections_list.size()>1)) || (connections_list.size()>0)) {
//                                // use connToast so onPause can cancel toast if connection is made
//                                connToast.setText(threaded_application.context.getResources().getString(R.string.toastConnectionsListHelp));
//                                connToast.show();
//                            }
//
//                        }
//                    } catch (IOException except) {
//                        errMsg = except.getMessage();
//                        Log.e("connection_activity", "Error reading recent connections list: " + errMsg);
//                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectErrorReadingRecentConnections) + " " + errMsg, Toast.LENGTH_SHORT).show();
//                    }

                    if (((importExportConnectionList.foundDemoHost)
                            && (importExportConnectionList.connections_list.size()>1)) || (importExportConnectionList.connections_list.size()>0)) {
                        // use connToast so onPause can cancel toast if connection is made
                        connToast.setText(threaded_application.context.getResources().getString(R.string.toastConnectionsListHelp));
                        connToast.show();
                    }
                }
            }

//            //if demo host not already in list, add it at end
//            if ((!prefHideDemoServer) && (!foundDemoHost)) {
//                HashMap<String, String> hm = new HashMap<>();
//                hm.put("ip_address", demo_host);
//                hm.put("host_name", demo_host);
//                hm.put("port", demo_port);
//                importExportConnectionList.connections_list.add(hm);
//            }
            connection_list_adapter.notifyDataSetChanged();
        }

        //for debugging only
    /*	private void withrottle_list() {
		try {
			JmDNS jmdns = JmDNS.create();
			ServiceInfo[] infos = jmdns.list("_withrottle._tcp.local.");
			String fh = "";
			for (ServiceInfo info : infos) {
				fh +=  info.getURL() + "\n";
			}
			jmdns.close();
			if (fh != "") {
				Toast.makeText(getApplicationContext(), "Found withrottle servers:\n" + fh, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(), "No withrottle servers found.", Toast.LENGTH_LONG).show();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	 */

        private void saveSharedPreferencesToFile() {
            navigateToHandler(PermissionsHelper.STORE_PREFERENCES);
        }

        private void saveSharedPreferencesToFileImpl() {
            SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
            String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue));

            if (prefAutoImportExport.equals(ImportExportPreferences.AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT)) {
                if (mainapp.connectedHostName != null) {
                    String exportedPreferencesFileName = mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";

                    if (!exportedPreferencesFileName.equals(".ed")) {
                        File path = Environment.getExternalStorageDirectory();
                        File engine_driver_dir = new File(path, "engine_driver");
                        engine_driver_dir.mkdir();            // create directory if it doesn't exist

                        importExportPreferences.saveSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectUnableToSavePref), Toast.LENGTH_LONG).show();
                }
            }
        }

        private void loadSharedPreferencesFromFile() {
            navigateToHandler(PermissionsHelper.READ_PREFERENCES);
        }

        @SuppressLint("ApplySharedPref")
        private void loadSharedPreferencesFromFileImpl() {
            SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
            String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue)).trim();

            String deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
            sharedPreferences.edit().putString("prefAndroidId", deviceId).commit();
            sharedPreferences.edit().putInt("prefForcedRestartReason", threaded_application.FORCED_RESTART_REASON_AUTO_IMPORT).commit();

            if ((prefAutoImportExport.equals(ImportExportPreferences.AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT))
                    || (prefAutoImportExport.equals(ImportExportPreferences.AUTO_IMPORT_EXPORT_OPTION_CONNECT_ONLY))) {  // automatically load the host specific preferences, if the preference is set
                if (mainapp.connectedHostName != null) {
                    String exportedPreferencesFileName = mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                    importExportPreferences.loadSharedPreferencesFromFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName, deviceId, true);
                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectUnableToLoadPref), Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void attachBaseContext(Context base) {
            super.attachBaseContext(LocaleHelper.onAttach(base));
        }

        @SuppressLint("SwitchIntDef")
        public void navigateToHandler(@RequestCodes int requestCode) {
            Log.d("Engine_Driver", "connection_activity: navigateToHandler:" + requestCode);
            if (!PermissionsHelper.getInstance().isPermissionGranted(connection_activity.this, requestCode)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PermissionsHelper.getInstance().requestNecessaryPermissions(connection_activity.this, requestCode);
                }
            } else {
                // Go to the correct handler based on the request code.
                // Only need to consider relevant request codes initiated by this Activity
                switch (requestCode) {
                    case PermissionsHelper.CLEAR_CONNECTION_LIST:
                        Log.d("Engine_Driver", "Got permission for CLEAR_CONNECTION_LIST - navigate to clearConnectionsListImpl()");
                        clearConnectionsListImpl();
                        break;
                    case PermissionsHelper.READ_CONNECTION_LIST:
                        Log.d("Engine_Driver", "Got permission for READ_CONNECTION_LIST - navigate to getConnectionsListImpl()");
                        getConnectionsListImpl("", "");
                        break;
                    case PermissionsHelper.STORE_PREFERENCES:
                        Log.d("Engine_Driver", "Got permission for STORE_PREFERENCES - navigate to saveSharedPreferencesToFileImpl()");
                        saveSharedPreferencesToFileImpl();
                        break;
                    case PermissionsHelper.READ_PREFERENCES:
                        Log.d("Engine_Driver", "Got permission for READ_PREFERENCES - navigate to loadSharedPreferencesFromFileImpl()");
                        loadSharedPreferencesFromFileImpl();
                        break;
                    case PermissionsHelper.CONNECT_TO_SERVER:
                        Log.d("Engine_Driver", "Got permission for READ_PHONE_STATE - navigate to connectImpl()");
                        connectImpl();
                        break;
                    default:
                        // do nothing
                        Log.d("Engine_Driver", "Unrecognised permissions request code: " + requestCode);
                }
            }
        }

        @Override
        public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (!PermissionsHelper.getInstance().processRequestPermissionsResult(connection_activity.this, requestCode, permissions, grantResults)) {
                Log.d("Engine_Driver", "Unrecognised request - send up to super class");
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

