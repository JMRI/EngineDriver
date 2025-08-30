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

import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.dccex_protocol_option_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.auto_import_export_option_type;
import jmri.enginedriver.type.restart_reason_type;

//import jmri.enginedriver.util.BackgroundImageLoader;
import jmri.enginedriver.type.toolbar_button_size_type;
import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;
import jmri.enginedriver.util.SwipeDetector;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.intro.intro_activity;
import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import jmri.enginedriver.import_export.ImportExportPreferences;
import jmri.enginedriver.import_export.ImportExportConnectionList;

public class connection_activity extends AppCompatActivity implements PermissionsHelper.PermissionsHelperGrantedCallback {
    static final String activityName = "connection_activity";

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
    private String connected_ssid;
    private String connected_serviceType;

    private static final String demo_host = "jmri.mstevetodd.com";
    private static final String demo_port = "44444";
    private static final String DUMMY_HOST = "999";
    private static final String DUMMY_ADDRESS = "999";
    private static final int DUMMY_PORT = 999;
    private static final String DUMMY_SSID = "";

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
    Button connect_button;

    LinearLayout rootView;
    int rootViewHeight = 0;

    boolean prefAllowMobileData = false;
    boolean prefDccexConnectionOption = false;
    //    boolean prefAlwaysUseFunctionsFromServer = false;
    TextView dccexConnectionOptionLabel;
    Spinner dccexConnectionOptionSpinner;
    String [] dccexConnectionOptionEntriesArray;
    String [] dccexConnectionOptionEntryValuesArray;
    TextView discoveredServersHeading;
    TextView discoveredServersWarning;

    private LinearLayout screenNameLine;
    private Toolbar toolbar;
    private LinearLayout statusLine;

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
//        navigateToHandler(PermissionsHelper.CONNECT_TO_SERVER);
//    }
//
//    //Request connection to the WiThrottle server.
//    private void connectImpl() {
        //	  sendMsgErr(0, message_type.CONNECT, connected_hostip, connected_port, "ERROR in ca.connect: comm thread not started.");
        Log.d(threaded_application.applicationName, activityName + ": connect()");
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CONNECT, connected_hostip, connected_port);
    }


    private void start_throttle_activity() {
        threaded_application.activityInTransition(activityName);
        Intent throttle = mainapp.getThrottleIntent();
        startActivity(throttle);
        this.finish();
        overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void startNewConnectionActivity() {
        // first remove old handler since the new Intent will have its own
        isRestarting = true;        // tell OnDestroy to skip this step since it will run after the new Intent is created
        if (mainapp.connection_msg_handler != null) {
            mainapp.connection_msg_handler.removeCallbacksAndMessages(null);
            mainapp.connection_msg_handler = null;
        }

        threaded_application.activityInTransition(activityName);
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
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (connectionsListSwipeDetector.swipeDetected()) { // check for swipe
                if (connectionsListSwipeDetector.getAction() == SwipeDetector.Action.LR) {
                    clearConnectionsListItem(v, position, id);
                    connectionsListSwipeDetector.swipeReset();
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
                        connected_port = Integer.parseInt(hpv.getText().toString());
                        connected_ssid = mainapp.client_ssid;
                        TextView hstv = (TextView) vg.getChildAt(3); // get from 4th box
                        connected_serviceType = hstv.getText().toString();
                        break;
                }
                checkIfDccexServerName(connected_hostname, connected_port);
                connect();
                mainapp.buttonVibration();
            }
        }
    }

    private class HostNumericListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            EditText entry = findViewById(R.id.host_ip);
            entry.setInputType(InputType.TYPE_CLASS_NUMBER);
            entry.setKeyListener(DigitsKeyListener.getInstance("01234567890."));
            entry.requestFocus();
            host_numeric.setVisibility(GONE);
            host_text.setVisibility(VISIBLE);
            mainapp.buttonVibration();
        }
    }

    private class HostTextListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            EditText entry = findViewById(R.id.host_ip);
            entry.setInputType(InputType.TYPE_CLASS_TEXT);
            entry.requestFocus();
            host_numeric.setVisibility(VISIBLE);
            host_text.setVisibility(GONE);
            mainapp.buttonVibration();
        }
    }

    private void showHideNumericOrTextButtons(boolean show) {
        View v = findViewById(R.id.host_numeric_or_text);
        if (show) {
            v.setVisibility(VISIBLE);
        } else {
            v.setVisibility(GONE);
        }
    }

    private class button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            EditText entry = findViewById(R.id.host_ip);
            connected_hostip = entry.getText().toString();
            if (!connected_hostip.trim().isEmpty()) {
                entry = findViewById(R.id.port);
                try {
                    connected_port = Integer.parseInt(entry.getText().toString());
                } catch (Exception except) {
                    threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastConnectInvalidPort) + "\n" + except.getMessage(), Toast.LENGTH_SHORT);
                    connected_port = 0;
                    return;
                }
                connected_hostname = connected_hostip; //copy ip to name
                connected_ssid = mainapp.client_ssid;
                checkIfDccexServerName(connected_hostname, connected_port);
                connect();
            } else {
//                if (!mainapp.prefHideInstructionalToasts) {
//                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectEnterAddress), Toast.LENGTH_SHORT).show();
//                }
//                mainapp.safeToastInstructional(getApplicationContext().getResources().getString(R.string.toastConnectEnterAddress), Toast.LENGTH_SHORT);
                mainapp.safeToastInstructional(R.string.toastConnectEnterAddress, Toast.LENGTH_SHORT);
            }
            mainapp.buttonVibration();
        }
    }

    private void clearConnectionsListItem(View v, int position, long id) {
        //When a connection swiped , remove it from the list
        ViewGroup vg = (ViewGroup) v; //convert to viewgroup for clicked row
        TextView hip = (TextView) vg.getChildAt(0); // get host ip from 1st box
        TextView hnv = (TextView) vg.getChildAt(1); // get host name from 2nd box
        TextView hpv = (TextView) vg.getChildAt(2); // get port from 3rd box
        TextView ssidView = (TextView) vg.getChildAt(3); // get port from 4th box
        if (!(hnv.getText().toString().equals(demo_host)) || !(hpv.getText().toString().equals(demo_port))) {
            getConnectionsListImpl(hip.getText().toString(), hpv.getText().toString());
            connected_hostip = DUMMY_ADDRESS;
            connected_hostname = DUMMY_HOST;
            connected_port = DUMMY_PORT;
            connected_ssid = DUMMY_SSID;

            Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
            anim.setDuration(500);
            v.startAnimation(anim);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mainapp.safeToastInstructional(R.string.toastConnectRemoved, Toast.LENGTH_SHORT);
                    importExportConnectionList.saveConnectionsListExecute(mainapp, connected_hostip, connected_hostname, connected_port, "", connected_ssid, connected_serviceType);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                /** @noinspection EmptyMethod*/
                public void run() {
                }
            });

        } else {
            threaded_application.safeToast(R.string.toastConnectRemoveDemoHostError, Toast.LENGTH_SHORT);
        }
    }


    //Method to connect to first discovered WiThrottle server.
    private void connectA() {
        try {
            if (discovery_list.get(0) != null) {
                HashMap<String, String> tm = discovery_list.get(0);

                connected_hostip = tm.get("ip_address");

                if (!connected_hostip.trim().isEmpty()) {
                    try {
                        connected_port = Integer.parseInt(tm.get("port"));
                    } catch (Exception except) {
                        threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastConnectInvalidPort) + "\n" + except.getMessage(), Toast.LENGTH_SHORT);
                        connected_port = 0;
                        return;
                    }
                    connected_hostname = tm.get("host_name"); //copy ip to name
                    connected_ssid = mainapp.client_ssid;
                    checkIfDccexServerName(connected_hostname, connected_port);
                    connect();
                    threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastConnectConnected, connected_hostname, Integer.toString(connected_port)), LENGTH_LONG);
                } else {
//                    if (!mainapp.prefHideInstructionalToasts) {
//                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectEnterAddress), Toast.LENGTH_SHORT).show();
//                    }
                    mainapp.safeToastInstructional(R.string.toastConnectEnterAddress, Toast.LENGTH_SHORT);
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

        public ui_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.SERVICE_RESOLVED:
                    HashMap<String, String> hm = (HashMap<String, String>) msg.obj;  //payload is already a hashmap
                    String found_host_name = hm.get("host_name");
                    String found_ip_address = hm.get("ip_address");
                    String found_port = hm.get("port");
                    String found_service_type = hm.get("service_type");
                    boolean entryExists = false;

                    //stop if new address is already in the list
                    HashMap<String, String> tm;
                    for (int index = 0; index < discovery_list.size(); index++) {
                        tm = discovery_list.get(index);
//                        if (tm.get("host_name").equals(found_host_name)) {
                        if ( (tm.get("ip_address").equals(found_ip_address))
                                && (tm.get("port").equals(found_port)) ) {
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
                    importExportConnectionList.saveConnectionsListExecute(mainapp, connected_hostip,
                            connected_hostname, connected_port, "", mainapp.client_ssid, connected_serviceType);
                    mainapp.connectedHostName = connected_hostname;
                    mainapp.connectedHostip = connected_hostip;
                    mainapp.connectedPort = connected_port;
                    mainapp.connectedSsid = mainapp.client_ssid;
                    mainapp.connectedServiceType = connected_serviceType;

                    loadSharedPreferencesFromFile();

                    start_throttle_activity();
                    break;

                case message_type.REOPEN_THROTTLE:
                    //ignore
                    break;

                case message_type.RESTART_APP:
                    startNewConnectionActivity();
                    break;

                case message_type.RELAUNCH_APP:
                case message_type.SHUTDOWN:
                    writeSharedPreferencesToFile();
                    mainapp.connectedHostName = "";
                    shutdown();
                    break;
                case message_type.DISCONNECT:
                    writeSharedPreferencesToFile();
                    mainapp.connectedHostName = "";
                    disconnect();
                    break;
            }
        }
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint({"ApplySharedPref", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //    timestamp = SystemClock.uptimeMillis();
        Log.d(threaded_application.applicationName, activityName + ": onCreate()");
        mainapp = (threaded_application) this.getApplication();
        mainapp.connection_msg_handler = new ui_handler(Looper.getMainLooper());

        mainapp.connectedHostName = "";
        mainapp.connectedHostip = "";
        mainapp.connectedPort = 0;
        mainapp.logged_host_ip = null;

        mainapp.roster_entries = null;
        mainapp.consist_entries = null;
        mainapp.rosterJmriWeb = null;

        connToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);    // save toast obj so it can be cancelled
        // setTitle(getApplicationContext().getResources().getString(R.string.app_name_connect));	//set title to long form of label

        //check for "default" throttle name and make it more unique
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        SharedPreferences prefsNoBackup = getSharedPreferences("jmri.enginedriver_preferences_no_backup", 0);
        if (!prefsNoBackup.getString("prefRunIntro", "0").equals(threaded_application.INTRO_VERSION)) {
            Intent intent = new Intent(this, intro_activity.class); // Call the AppIntro java class
            startActivity(intent);
            mainapp.introIsRunning = true;
        }

        mainapp.applyTheme(this);

//        checkForLegacyFiles();

        setContentView(R.layout.connection);

        boolean prefHideDemoServer = prefs.getBoolean("prefHideDemoServer", getResources().getBoolean(R.bool.prefHideDemoServerDefaultValue));
        mainapp.haveForcedWiFiConnection = false;

        //Set up a list adapter to allow adding discovered WiThrottle servers to the UI.
        discovery_list = new ArrayList<>();
        discovery_list_adapter = new SimpleAdapter(this, discovery_list, R.layout.connections_list_item,
                new String[]{"ip_address", "host_name", "port", "ssid", "serverType"},
                new int[]{R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label, R.id.ssid_item_label, R.id.serverType_item_label});
        ListView discover_list = findViewById(R.id.discovery_list);
        discover_list.setAdapter(discovery_list_adapter);
        discover_list.setOnItemClickListener(new connect_item(server_list_type.DISCOVERED_SERVER));

        importExportConnectionList = new ImportExportConnectionList(prefs);
        //Set up a list adapter to allow adding the list of recent connections to the UI.
//            connections_list = new ArrayList<>();
        connection_list_adapter = new SimpleAdapter(this, importExportConnectionList.connections_list, R.layout.connections_list_item,
                new String[]{"ip_address", "host_name", "port", "ssid", "serverType"},
                new int[]{R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label, R.id.ssid_item_label, R.id.serverType_item_label});
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
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
        port.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                checkIPandPort();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        host_numeric_or_text = findViewById(R.id.host_numeric_or_text);
        host_numeric = findViewById(R.id.host_numeric);
        HostNumericListener host_numeric_listener = new HostNumericListener();
        host_numeric.setOnClickListener(host_numeric_listener);
        host_numeric.setVisibility(GONE);
        host_text = findViewById(R.id.host_text);
        HostTextListener host_text_listener = new HostTextListener();
        host_text.setOnClickListener(host_text_listener);

        //Set the button callback.
        connect_button = findViewById(R.id.connect);
        button_listener click_listener = new button_listener();
        connect_button.setOnClickListener(click_listener);

        discoveredServersHeading = findViewById(R.id.discoveredServersHeading);
        discoveredServersWarning = findViewById(R.id.discoveredServersWarning);

        set_labels();
        calculateDisplayMetrics();

        if (prefs.getBoolean("prefForcedRestart", false)) { // if forced restart from the preferences
            prefs.edit().putBoolean("prefForcedRestart", false).commit();

            int prefForcedRestartReason = prefs.getInt("prefForcedRestartReason", restart_reason_type.NONE);
            Log.d(threaded_application.applicationName, activityName + ": onCreate(); Forced Restart Reason: " + prefForcedRestartReason);
            if (mainapp.prefsForcedRestart(prefForcedRestartReason)) {
                Intent in = new Intent().setClass(this, SettingsActivity.class);
                startActivityForResult(in, 0);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            }
        }

        Button button = findViewById(R.id.clear_recent_connections_button);
        button.setOnClickListener(new ClearRecentConnectionsButtonListener());

        rootView = findViewById(R.id.connection_view);
        rootView.requestFocus();
        rootViewHeight = rootView.getHeight();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (rootViewHeight != 0) {
                    int heightDiff = rootViewHeight - rootView.getHeight();
                    if (heightDiff < -400) {  //keyboard closed
                        rootView.requestFocus();
                        showHideNumericOrTextButtons(false);
                    }
                }
                rootViewHeight = rootView.getHeight();
            }
        });

        dccexConnectionOptionEntriesArray = this.getResources().getStringArray(R.array.prefYesNoAutoEntries);
        dccexConnectionOptionEntryValuesArray = this.getResources().getStringArray(R.array.prefYesNoAutoEntryValues);

        dccexConnectionOptionLabel = findViewById(R.id.cons_DccexConnectionOption_Label);
        dccexConnectionOptionSpinner = findViewById(R.id.cons_DccexConnectionOption);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.prefYesNoAutoEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccexConnectionOptionSpinner.setOnItemSelectedListener(new DccexConnectionOption_listener());
        setConnectionProtocolOption();

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_connect),
                    "");
        }

        mainapp.gamepadFullReset();

    } //end onCreate

    @Override
    public void onResume() {
        super.onResume();
        threaded_application.activityResumed(activityName);
        mainapp.removeNotification(this.getIntent());

        threaded_application.currentActivity = activity_id_type.CONNECTION;
        if (this.isFinishing()) {        //if finishing, expedite it
            return;
        }
        mainapp.exitDoubleBackButtonInitiated = 0;
//        if (this.runIntro) {        //if going to run the intro, expedite it
        if (mainapp.introIsRunning) {        //if going to run the intro, expedite it
            return;
        }

        calculateDisplayMetrics();
        for (int i=0; i<6; i++) { // reset the ESU 2/Pro first server update flags
            mainapp.EsuMc2FirstServerUpdate[i] = true;
        }

        getWifiInfo();
//        getPhoneInfo();

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
        setConnectionProtocolOption();

        if (CMenu != null) {
            mainapp.displayFlashlightMenuButton(CMenu);
            mainapp.setFlashlightActionViewButton(CMenu, findViewById(R.id.flashlight_button));
        }
    }  //end of onResume

    @Override
    public void onPause() {
        super.onPause();
        threaded_application.activityPaused(activityName);

//        runIntro = false;
        if (connToast != null) {
            connToast.cancel();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(threaded_application.applicationName, activityName + ": onStop()");
        //shutdown server discovery listener
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 0);
    }

    @Override
    public void onDestroy() {
        Log.d(threaded_application.applicationName, activityName + ": onDestroy()");
        super.onDestroy();

        if (!isRestarting) {
            if (mainapp.connection_msg_handler != null) {
                mainapp.connection_msg_handler.removeCallbacksAndMessages(null);
                mainapp.connection_msg_handler = null;
            } else {
                Log.d(threaded_application.applicationName, activityName + ": onDestroy(): mainapp.connection_msg_handler is null. Unable to removeCallbacksAndMessages");
            }
        } else {
            isRestarting = false;
        }
        CMenu = null;
        connectionsListSwipeDetector = null;
        prefs = null;
        discovery_list_adapter = null;
        connection_list_adapter = null;
        mainapp = null;
    }

    private void disconnect() {
        this.finish();
    }

    private void shutdown() {
        this.finish();
    }

    private void calculateDisplayMetrics() {
        Log.d(threaded_application.applicationName, activityName + ": calculateDisplayMetrics()");
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        threaded_application.displayMetrics = dm;
        float yInches= threaded_application.displayMetrics.heightPixels/threaded_application.displayMetrics.ydpi;
        float xInches= threaded_application.displayMetrics.widthPixels/threaded_application.displayMetrics.xdpi;
        threaded_application.displayDiagonalInches = Math.sqrt(xInches*xInches + yInches*yInches);
        threaded_application.prefToolbarButtonSize = prefs.getString("prefToolbarButtonSize", getApplicationContext().getResources().getString(R.string.prefToolbarButtonSizeDefaultValue));
        if (MobileControl2.isMobileControl2()) { // ESU 2/Pro / Pro mis-report their size
            threaded_application.useSmallToolbarButtonSize = true;
        } else if ( (threaded_application.displayDiagonalInches >= threaded_application.LARGE_SCREEN_SIZE)
                && (threaded_application.prefToolbarButtonSize.equals(toolbar_button_size_type.AUTO)) ) {
            threaded_application.useSmallToolbarButtonSize = false;
        } else if (threaded_application.prefToolbarButtonSize.equals(toolbar_button_size_type.LARGE)) {
            threaded_application.useSmallToolbarButtonSize = false;
        } else if (threaded_application.prefToolbarButtonSize.equals(toolbar_button_size_type.SMALL)) {
            threaded_application.useSmallToolbarButtonSize = true;
        }

        threaded_application.min_fling_distance = (int) (threaded_application.SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
        threaded_application.min_fling_velocity = (int) (threaded_application.SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);

    }

    private void set_labels() {
        SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        TextView v = findViewById(R.id.ca_footer);
        String throttleName = prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
        throttleName = mainapp.fixThrottleName(throttleName); //insure uniqueness
        v.setText(String.format(getString(R.string.throttle_name), throttleName));

        String ssid = mainapp.client_ssid;
        StringBuilder warningTextBuilder = new StringBuilder();
        if ( (ssid.equals("UNKNOWN")) || (ssid.equals("<unknown ssid>")) || (ssid.equals("Can't access SSID")) ) {
            if (mainapp.client_type.equals("MOBILE")) {
                ssid = getString(R.string.statusThreadedAppNotconnectedToWifi);
            } else {
                ssid = getString(R.string.statusThreadedAppNoLocationService);
                if (!mainapp.clientLocationServiceEnabled) {
                    warningTextBuilder.append(getString(R.string.statusThreadedAppServerDiscoveryNoLocationService));
                    warningTextBuilder.append("  ");
                }
                PermissionsHelper phi = PermissionsHelper.getInstance();
                if (!phi.isPermissionGranted(connection_activity.this, PermissionsHelper.ACCESS_FINE_LOCATION)) {
                    warningTextBuilder.append(getString(R.string.statusThreadedAppServerDiscoveryAccessFineLocationNotGranted));
                    warningTextBuilder.append("  ");
                }
                warningTextBuilder.append(getString(R.string.statusThreadedAppServerDiscoverySsidUnavailable));
                discoveredServersWarning.setText(warningTextBuilder.toString());
            }
            threaded_application.safeToast(warningTextBuilder.toString(), Toast.LENGTH_LONG);
//            discoveredServersWarning.setVisibility(VISIBLE);
        } else {
            discoveredServersWarning.setVisibility(GONE);
        }

        discoveredServersHeading.setText(String.format(getString(R.string.discovered_services), ssid));


        if (toolbar != null) {
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_connect),
                    "");
        }
    }

//    void getPhoneInfo() {
//        Log.d(threaded_application.applicationName, activityName + ":  getPhoneInfo()");
//
//        PermissionsHelper phi = PermissionsHelper.getInstance();
//        if (!phi.isPermissionGranted(connection_activity.this, PermissionsHelper.READ_PHONE_STATE)) {
//            if (prefs.getBoolean("stop_on_phonecall_preference", mainapp.getResources().getBoolean(R.bool.prefStopOnPhonecallDefaultValue))) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    // how many times have we asked for this before?
//                    int count = prefs.getInt("prefStopOnPhonecallCount", 0);
//                    if (count < 5) {  // this is effectively counted twice each startup
//                        phi.requestNecessaryPermissions(connection_activity.this, PermissionsHelper.READ_PHONE_STATE);
//                        count++;
//                    } else {
//                        prefs.edit().putBoolean("stop_on_phonecall_preference", false).apply();
//                        count = 0;
//                    }
//                    prefs.edit().putInt("prefStopOnPhonecallCount", count).apply();
//                }
//            }
//        }
//    }


    /**
     * retrieve some wifi details, stored in mainapp as client_ssid, client_address and client_address_inet4
     */
    void getWifiInfo() {
        Log.d(threaded_application.applicationName, activityName + ": getWifiInfo()");
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
//            if (!phi.isPermissionGranted(connection_activity.this, PermissionsHelper.ACCESS_FINE_LOCATION)) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    phi.requestNecessaryPermissions(connection_activity.this, PermissionsHelper.ACCESS_FINE_LOCATION);
//                }
//            }
//            if (!phi.isPermissionGranted(connection_activity.this, PermissionsHelper.ACCESS_COARSE_LOCATION)) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    phi.requestNecessaryPermissions(connection_activity.this, PermissionsHelper.ACCESS_COARSE_LOCATION);
//                }
//            }
            if (!phi.isPermissionGranted(connection_activity.this, PermissionsHelper.ACCESS_WIFI_STATE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    phi.requestNecessaryPermissions(connection_activity.this, PermissionsHelper.ACCESS_WIFI_STATE);
                }
            }
            if (!phi.isPermissionGranted(connection_activity.this, PermissionsHelper.INTERNET)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    phi.requestNecessaryPermissions(connection_activity.this, PermissionsHelper.INTERNET);
                }
            }

            prefAllowMobileData = prefs.getBoolean("prefAllowMobileData", false);

            mainapp.client_ssid = wifiinfo.getSSID();
            Log.d(threaded_application.applicationName, activityName + ": getWifiInfo(): SSID: " + mainapp.client_ssid);
            if (mainapp.client_ssid != null && mainapp.client_ssid.startsWith("\"") && mainapp.client_ssid.endsWith("\"")) {
                mainapp.client_ssid = mainapp.client_ssid.substring(1, mainapp.client_ssid.length() - 1);
            }

            // determine if the location service is enabled
            LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
            try {
                mainapp.clientLocationServiceEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception except) {
                Log.d(threaded_application.applicationName, activityName + ": getWifiInfo(): unable to determine if the location service is enabled");
            }
            if (MobileControl2.isMobileControl2()) {
                // ESU MC 2/Pro does not have a gps receiver so incorrectly reports that it that the service is disabled
                mainapp.clientLocationServiceEnabled = true;
            }

            //determine if currently using mobile connection or wifi
            final ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = cm.getActiveNetworkInfo();
            switch (nInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    mainapp.client_type = "WIFI";

                    if (!prefAllowMobileData) {
                        // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Log.d(threaded_application.applicationName, activityName + ": getWifiInfor(): Builder");
                            NetworkRequest.Builder request = new NetworkRequest.Builder();
                            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

                            cm.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {

                                @Override
                                public void onAvailable(Network network) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                        ConnectivityManager.setProcessDefaultNetwork(network);
                                    } else {
                                        cm.bindProcessToNetwork(network);  //API23+
                                    }
                                }
                            });
                        }
                    }
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    if ((!mainapp.client_type.equals("WIFI")) && (prefAllowMobileData)) {
                        mainapp.client_type = "MOBILE";
                    }
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
            Log.d(threaded_application.applicationName, activityName + ": getWifiInfo(): network type=" + nInfo.getType() + " " + mainapp.client_type);

        } catch (Exception except) {
            Log.e(threaded_application.applicationName, activityName + ": getWifiInfo(): error getting IP addr: " + except.getMessage());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.connection_menu, menu);
        CMenu = menu;
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightActionViewButton(menu, findViewById(R.id.flashlight_button));

        adjustToolbarSize(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mainapp.exitDoubleBackButtonInitiated = 0;

        // Handle all of the possible menu actions.
        Intent in;
        if (item.getItemId() == R.id.exit_mnu) {
            mainapp.checkAskExit(this);
            return true;
        } else if (item.getItemId() == R.id.settings_mnu) {
            in = new Intent().setClass(this, SettingsActivity.class);
            startActivityForResult(in, 0);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.about_mnu) {
            in = new Intent().setClass(this, about_page.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
//        } else if (item.getItemId() == R.id.ClearconnList) {
//            clearConnectionsList();
//            getConnectionsList();
//            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlightActionView(this, CMenu, findViewById(R.id.flashlight_button));
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.logviewer_menu) {
            in = new Intent().setClass(this, LogViewerActivity.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.intro_mnu) {
            in = new Intent().setClass(this, intro_activity.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    //handle return from menu items
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //only one activity with results here
        set_labels();
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Handle pressing of the back button to request exit
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KEYCODE_BACK) {
            mainapp.checkExit(this);
            return true;
        }
        mainapp.exitDoubleBackButtonInitiated = 0;
        return (super.onKeyDown(key, event));
    }


    private void checkIP() {
        String tempIP = host_ip.getText().toString().trim();
        if (tempIP.contains(":")) { //If a colon has been entered in the host field, remove and jump to the port field
            host_ip.setText(tempIP.replace(":", ""));
            port.requestFocus();
        }
        checkIPandPort();
    }

    private void checkIPandPort() {
        String tempIP = host_ip.getText().toString().trim();
        String tempPort = port.getText().toString().trim();
        connect_button.setEnabled(true);
        if ( (tempIP.isEmpty()) || (tempPort.isEmpty()) ) {
            connect_button.setEnabled(false);
        }
    }

    public class ClearRecentConnectionsButtonListener implements AdapterView.OnClickListener {
        public void onClick(View v) {
            clearConnectionsList();
        }
    }

    private void clearConnectionsList() {
        //Clears recent connection list.

        mainapp.exitDoubleBackButtonInitiated = 0;

        //if no SD Card present then nothing to do
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            threaded_application.safeToast(R.string.toastConnectErrorReadingRecentConnections, Toast.LENGTH_SHORT);
        } else {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                //@Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            File connections_list_file = new File(context.getExternalFilesDir(null), "connections_list.txt");

                            if (connections_list_file.exists()) {
                                //noinspection ResultOfMethodCallIgnored
                                connections_list_file.delete();
                                importExportConnectionList.connections_list.clear();
                                recreate();
                            }
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                    mainapp.buttonVibration();
                }
            };

            AlertDialog.Builder ab = new AlertDialog.Builder(connection_activity.this);
            ab.setTitle(getApplicationContext().getResources().getString(R.string.dialogConfirmClearTitle))
                    .setMessage(getApplicationContext().getResources().getString(R.string.dialogRecentConnectionsConfirmClearQuestions))
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.cancel, dialogClickListener);
            ab.show();

        }
    }

    private void getConnectionsList() {
//            navigateToHandler(PermissionsHelper.READ_CONNECTION_LIST);
        getConnectionsListImpl("", "");
    }

    private void getConnectionsListImpl(String addressToRemove, String portToRemove) {
        importExportConnectionList.connections_list.clear();

        SharedPreferences prefsNoBackup = getSharedPreferences("jmri.enginedriver_preferences_no_backup", 0);
        if (prefsNoBackup.getString("prefRunIntro", "0").equals(threaded_application.INTRO_VERSION)) { // if the intro hasn't run yet, the permissions may not have been granted yet, so don't red the SD card,

            if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                //alert user that recent connections list requires SD Card
                TextView v = findViewById(R.id.recent_connections_heading);
                v.setText(getString(R.string.ca_recent_conn_notice));
            } else {
                importExportConnectionList.getConnectionsList(addressToRemove, portToRemove);

                if (!importExportConnectionList.failureReason.isEmpty()) {
                    threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastConnectErrorReadingRecentConnections) + " " + importExportConnectionList.failureReason, Toast.LENGTH_SHORT);
                } else {
                    if (((importExportConnectionList.foundDemoHost)
                            && (importExportConnectionList.connections_list.size() > 1)) || (!importExportConnectionList.connections_list.isEmpty())) {
                        // use connToast so onPause can cancel toast if connection is made
                        if (!mainapp.prefHideInstructionalToasts) {
                            connToast.setText(threaded_application.context.getResources().getString(R.string.toastConnectionsListHelp));
                            connToast.setDuration(LENGTH_LONG);
                            connToast.show();
                        }
                    }
                }
            }
        }

        connection_list_adapter.notifyDataSetChanged();
    }

    //for debugging only
    /*	private void withrottle_list() {
		try {
			JmDNS jmdns = JmDNS.create();
			ServiceInfo[] infos = jmdns.list(mainapp.JMDNS_SERVICE_WITHROTTLE);
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

    private void writeSharedPreferencesToFile() {
        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
        String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue));

        if (prefAutoImportExport.equals(auto_import_export_option_type.CONNECT_AND_DISCONNECT)) {
            if (!mainapp.connectedHostName.isEmpty()) {
                String exportedPreferencesFileName = mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";

                if (!exportedPreferencesFileName.equals(".ed")) {
                    importExportPreferences.writeSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
                }
            } else {
                threaded_application.safeToast(R.string.toastConnectUnableToSavePref, LENGTH_LONG);
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    private void loadSharedPreferencesFromFile() {
//            navigateToHandler(PermissionsHelper.READ_PREFERENCES);
//        loadSharedPreferencesFromFileImpl();
//    }
//
//    @SuppressLint("ApplySharedPref")
//    private void loadSharedPreferencesFromFileImpl() {
        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
        String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue)).trim();

        String deviceId = mainapp.getFakeDeviceId();
        sharedPreferences.edit().putString("prefAndroidId", deviceId).commit();
        sharedPreferences.edit().putInt("prefForcedRestartReason", restart_reason_type.AUTO_IMPORT).commit();

        if ((prefAutoImportExport.equals(auto_import_export_option_type.CONNECT_AND_DISCONNECT))
                || (prefAutoImportExport.equals(auto_import_export_option_type.CONNECT_ONLY))) {  // automatically load the host specific preferences, if the preference is set
            if (!mainapp.connectedHostName.isEmpty()) {
                String exportedPreferencesFileName = mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                importExportPreferences.loadSharedPreferencesFromFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName, deviceId, true);
            } else {
                threaded_application.safeToast(R.string.toastConnectUnableToLoadPref, LENGTH_LONG);
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@RequestCodes int requestCode) {
        Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): " + requestCode);
        if (!PermissionsHelper.getInstance().isPermissionGranted(connection_activity.this, requestCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(connection_activity.this, requestCode);
            }
        } else {
            // Go to the correct handler based on the request code.
            // Only need to consider relevant request codes initiated by this Activity
//            switch (requestCode) {
//                    case PermissionsHelper.CLEAR_CONNECTION_LIST:
//                        Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Got permission for CLEAR_CONNECTION_LIST - navigate to clearConnectionsListImpl()");
//                        clearConnectionsListImpl();
//                        break;
//                    case PermissionsHelper.READ_CONNECTION_LIST:
//                        Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Got permission for READ_CONNECTION_LIST - navigate to getConnectionsListImpl()");
//                        getConnectionsListImpl("", "");
//                        break;
//                    case PermissionsHelper.STORE_PREFERENCES:
//                        Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Got permission for STORE_PREFERENCES - navigate to writeSharedPreferencesToFileImpl()");
//                        writeSharedPreferencesToFileImpl();
//                        break;
//                    case PermissionsHelper.READ_PREFERENCES:
//                        Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Got permission for READ_PREFERENCES - navigate to loadSharedPreferencesFromFileImpl()");
//                        loadSharedPreferencesFromFileImpl();
//                        break;
//                case PermissionsHelper.CONNECT_TO_SERVER:
//                    Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Got permission for READ_PHONE_STATE - navigate to connectImpl()");
////                    connectImpl();
//                    checkIfDccexServerName(connected_hostname, connected_port);
//                    connect();
//                    break;
//                default:
            // do nothing
            Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Unrecognised permissions request code: " + requestCode);
//            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(connection_activity.this, requestCode, permissions, grantResults, true)) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(connection_activity.this, requestCode, permissions, grantResults)) {
            Log.d(threaded_application.applicationName, activityName + ": onRequestPermissionsResult(): Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //source  https://www.journaldev.com/861/java-copy-file
    void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public class DccexConnectionOption_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            Spinner spinner = findViewById(R.id.cons_DccexConnectionOption);
            int spinnerPosition = spinner.getSelectedItemPosition();
//            mainapp.prefUseDccexProtocol = dccexConnectionOptionEntriesArray[spinnerPosition];
            mainapp.prefUseDccexProtocol = dccexConnectionOptionEntryValuesArray[spinnerPosition];
            prefs.edit().putString("prefUseDccexProtocol", mainapp.prefUseDccexProtocol).commit();  //set the preference

            if ( (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.YES))
                    || (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.NO)) ) {
                dccexConnectionOptionLabel.setText(R.string.useProtocolDCCEX);
            } else { // if (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.AUTO))
                dccexConnectionOptionLabel.setText(R.string.useProtocolDCCEXplusAutoHint);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private void setConnectionProtocolOption() {
        prefDccexConnectionOption = prefs.getBoolean("prefDCCEXconnectionOption", getResources().getBoolean(R.bool.prefDccexConnectionOptionDefaultValue));
        mainapp.prefUseDccexProtocol = prefs.getString("prefUseDccexProtocol", mainapp.getResources().getString(R.string.prefUseDccexProtocolDefaultValue));

        if (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.YES)) {
            dccexConnectionOptionSpinner.setSelection(0);
            dccexConnectionOptionLabel.setText(R.string.useProtocolDCCEX);
        } else if (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.NO)) {
            dccexConnectionOptionSpinner.setSelection(1);
            dccexConnectionOptionLabel.setText(R.string.useProtocolDCCEX);
        } else { // if (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.AUTO))
            dccexConnectionOptionSpinner.setSelection(2);
            dccexConnectionOptionLabel.setText(R.string.useProtocolDCCEXplusAutoHint);
        }

        TextView DCCEXheading =  findViewById(R.id.cons_DccexConnectionOption_heading);
        RelativeLayout DCCEXlayout =  findViewById(R.id.cons_DccexConnectionOption_layout);
        if (prefDccexConnectionOption) {
            DCCEXheading.setVisibility(VISIBLE);
            DCCEXlayout.setVisibility(VISIBLE);
        } else {
            DCCEXheading.setVisibility(GONE);
            DCCEXlayout.setVisibility(GONE);
        }
    }

    void checkIfDccexServerName(String serverName, int serverPort) {
        mainapp.prefUseDccexProtocol = prefs.getString("prefUseDccexProtocol", mainapp.getResources().getString(R.string.prefUseDccexProtocolDefaultValue));

        mainapp.isDCCEX = ( (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.AUTO))
                && ((serverName.matches("\\S*(DCCEX|dccex|DCC-EX|dcc-ex)\\S*")) || (serverPort==2560)) );
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