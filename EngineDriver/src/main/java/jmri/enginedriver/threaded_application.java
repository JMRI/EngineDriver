/*Copyright (C) 2018 M. Steve Todd
  mstevetodd@gmail.com

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

// Main java file.
/* TODO: see changelog-and-todo-list.txt for complete list of project to-do's */

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import jmri.enginedriver.Consist.ConLoco;
import jmri.enginedriver.threaded_application.comm_thread.comm_handler;
import jmri.enginedriver.util.Flashlight;
import jmri.enginedriver.util.GetJsonFromUrl;
import jmri.enginedriver.util.PermissionsHelper;
import jmri.jmrit.roster.RosterEntry;
import jmri.jmrit.roster.RosterLoader;

//The application will start up a thread that will handle network communication in order to ensure that the UI is never blocked.
//This thread will only act upon messages sent to it. The network communication needs to persist across activities, so that is why
@SuppressLint("NewApi")
public class threaded_application extends Application {
    public static String INTRO_VERSION = "6";  // set this to a different string to force the intro to run on next startup.

    public comm_thread commThread;
    volatile String host_ip = null; //The IP address of the WiThrottle server.
    volatile String logged_host_ip = null;
    private volatile int port = 0; //The TCP port that the WiThrottle server is running on
    Double withrottle_version = 0.0; //version of withrottle server
    volatile int web_server_port = 0; //default port for jmri web server
    private String serverType = ""; //should be set by server in initial command strings
    private String serverDescription = ""; //may be set by server in initial command strings
    private volatile boolean doFinish = false;  // when true, tells any Activities that are being created/resumed to finish()
    //shared variables returned from the withrottle server, stored here for easy access by other activities
    volatile Consist[] consists;
    LinkedHashMap<Integer, String>[] function_labels;  //function#s and labels from roster for throttles
    LinkedHashMap<Integer, String> function_labels_default;  //function#s and labels from local settings
    LinkedHashMap<Integer, String> function_labels_default_for_roster;  //function#s and labels from local settings for roster entries with no function labels
    LinkedHashMap<Integer, String> function_consist_locos; // used for the 'special' consists function label string matching
    LinkedHashMap<Integer, String> function_consist_latching; // used for the 'special' consists function label string matching

    boolean[][] function_states = {null, null, null, null, null, null};  //current function states for throttles
    String[] to_system_names;
    String[] to_user_names;
    String[] to_states;
    HashMap<String, String> to_state_names;
    String[] rt_system_names;
    String[] rt_user_names;
    String[] rt_states;
    HashMap<String, String> rt_state_names; //if not set, routes are not allowed
    Map<String, String> roster_entries;  //roster sent by WiThrottle
    Map<String, String> consist_entries;
    private static DownloadRosterTask dlRosterTask = null;
    private static DownloadMetaTask dlMetadataTask = null;
    HashMap<String, RosterEntry> roster;  //roster entries retrieved from /roster/?format=xml (null if not retrieved)
    public static HashMap<String, String> jmriMetadata = null;  //metadata values (such as JMRIVERSION) retrieved from web server (null if not retrieved)
    ImageDownloader imageDownloader = new ImageDownloader();
    String power_state;
    public int fastClockFormat = 0; //0=no display, 1=12hr, 2=24hr
    private Long fastClockSeconds = 0L;
    public int androidVersion = 0;
    public String appVersion = "";
    //minimum Android version for some features
    public final int minImmersiveModeVersion = android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
    public final int minThemeVersion = android.os.Build.VERSION_CODES.HONEYCOMB;
    public final int minScreenDimNewMethodVersion = Build.VERSION_CODES.KITKAT;
    public final int minActivatedButtonsVersion = Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    //all heartbeat values are in milliseconds
    static final int DEFAULT_OUTBOUND_HEARTBEAT_INTERVAL = 10000; //interval for outbound heartbeat when WiT heartbeat is disabled
    static final int MIN_OUTBOUND_HEARTBEAT_INTERVAL = 1000;   //minimum allowed interval for outbound heartbeat generator
    static final int MAX_OUTBOUND_HEARTBEAT_INTERVAL = 30000;  //maximum allowed interval for outbound heartbeat generator
    static final double HEARTBEAT_RESPONSE_FACTOR = 0.9;   //adjustment for inbound and outbound timers
    static final int MIN_INBOUND_HEARTBEAT_INTERVAL = 1000;   //minimum allowed interval for (enabled) inbound heartbeat generator
    static final int MAX_INBOUND_HEARTBEAT_INTERVAL = 60000;  //maximum allowed interval for inbound heartbeat generator
    public int heartbeatInterval = 0;                       //WiT heartbeat interval setting (milliseconds)

    public int turnouts_list_position = 0;                  //remember where user was in item lists
    public int routes_list_position = 0;

    private static int WiThrottle_Msg_Interval = 100;   //minimum desired interval (ms) between messages sent to
    //  WiThrottle server, can be chgd for specific servers
    //   do not exceed 200, unless slider delay is also changed

    public static final int MAX_FUNCTION_NUMBER = 28;        // maximum number of the function buttons supported.

    public String deviceId = "";

    private static final String demo_host = "jmri.mstevetodd.com";
    private static final String demo_port = "44444";

    String client_address; //address string of the client address
    public Inet4Address client_address_inet4; //inet4 value of the client address
    public String client_ssid = "UNKNOWN";    //string of the connected SSID
    public String client_type = "UNKNOWN"; //network type, usually WIFI or MOBILE
    //For communication to the comm_thread.
    public comm_handler comm_msg_handler = null;
    //For communication to each of the activities (set and unset by the activity)
    public volatile Handler connection_msg_handler;
    public volatile Handler throttle_msg_handler;
    public volatile Handler web_msg_handler;
    public volatile Handler select_loco_msg_handler;
    public volatile Handler turnouts_msg_handler;
    public volatile Handler routes_msg_handler;
    public volatile Handler consist_edit_msg_handler;
    public volatile Handler consist_lights_edit_msg_handler;
    public volatile Handler power_control_msg_handler;
    public volatile Handler reconnect_status_msg_handler;
    public volatile Handler preferences_msg_handler;
    public volatile Handler settings_msg_handler;
    public volatile Handler logviewer_msg_handler;
    public volatile Handler about_page_msg_handler;
    public volatile Handler function_settings_msg_handler;
    public volatile Handler function_consist_settings_msg_handler;
    public volatile Handler gamepad_test_msg_handler;

    // for handling control of camera flash
    public static Flashlight flashlight;
    private boolean flashState = false;

    //these constants are used for onFling
    public static final int SWIPE_MIN_DISTANCE = 120;
    public static final int SWIPE_MAX_OFF_PATH = 250;
    public static final int SWIPE_THRESHOLD_VELOCITY = 200;
    public static int min_fling_distance;           // pixel width needed for fling
    public static int min_fling_velocity;           // velocity needed for fling

    private static final int ED_NOTIFICATION_ID = 416;  //no significance to 416, just shouldn't be 0

    private SharedPreferences prefs;

    public boolean EStopActivated = false;  // Has EStop been sent?

    public int numThrottles = 1;
    public int maxThrottles = 6;   // maximum number of throttles the system supports
    public int maxThrottlesCurrentScreen = 6;   // maximum number of throttles the current screen supports

    @NonNull
    public String connectedHostName = "";
    @NonNull
    public String connectedHostip = "";
    public int connectedPort = 0;

    public String languageCountry = "en";

    public boolean appIsFinishing = false;
    public boolean introIsRunning = false;

    public boolean webMenuSelected = false;  // used as an override for the auto-web code when the web menu is selected.

    public boolean shownToastRecentConsists = false;
    public boolean shownToastRecentLocos = false;
    public boolean shownToastRoster = false;
    public boolean shownToastConsistEdit = false;

    public boolean shownRosterTurnouts = false;
    public boolean firstWebActivity = false;
    private boolean exitConfirmed = false;
    private ApplicationLifecycleHandler lifecycleHandler;
    public static Context context;

    public static final int FORCED_RESTART_REASON_NONE = 0;
    public static final int FORCED_RESTART_REASON_RESET = 1;
    public static final int FORCED_RESTART_REASON_IMPORT = 2;
    public static final int FORCED_RESTART_REASON_IMPORT_SERVER_MANUAL = 3;
    public static final int FORCED_RESTART_REASON_THEME = 4;
    public static final int FORCED_RESTART_REASON_THROTTLE_PAGE = 5;
    public static final int FORCED_RESTART_REASON_LOCALE = 6;
    public static final int FORCED_RESTART_REASON_IMPORT_SERVER_AUTO = 7;
    public static final int FORCED_RESTART_REASON_AUTO_IMPORT = 8; // for local server files
    public static final int FORCED_RESTART_REASON_BACKGROUND = 9;
    public static final int FORCED_RESTART_REASON_THROTTLE_SWITCH = 10;
    public static final int FORCED_RESTART_REASON_FORCE_WIFI = 11;
    public static final int FORCED_RESTART_REASON_IMMERSIVE_MODE = 12;
    public static final int FORCED_RESTART_REASON_DEAD_ZONE = 13;
    public static final int FORCED_RESTART_REASON_SHAKE_THRESHOLD = 13;

    public int actionBarIconCountThrottle = 0;
    public int actionBarIconCountRoutes = 0;
    public int actionBarIconCountTurnouts = 0;

    public Resources.Theme theme;

    protected int throttleLayoutViewId;
    public boolean webServerNameHasBeenChecked = false;

    boolean haveForcedWiFiConnection = false;
    boolean prefAllowMobileData = false;

    public boolean prefAlwaysUseDefaultFunctionLabels = false;
    public String prefConsistFollowRuleStyle = "original";
    private static final String CONSIST_FUNCTION_RULE_STYLE_ORIGINAL = "original";
    private static final String CONSIST_FUNCTION_RULE_STYLE_COMPLEX = "complex";
    private static final String CONSIST_FUNCTION_RULE_STYLE_SPECIAL_EXACT = "specialExact";
    private static final String CONSIST_FUNCTION_RULE_STYLE_SPECIAL_PARTIAL = "specialPartial";

    public boolean prefShowTimeOnLogEntry = false;
    public boolean prefFeedbackOnDisconnect = true;

    public String prefHapticFeedback = "None";
    //    public int prefHapticFeedbackSteps = 10;
    public int prefHapticFeedbackDuration = 250;

    public boolean prefFullScreenSwipeArea = false;
    public boolean prefThrottleViewImmersiveModeHideToolbar = true;

    public static final String HAPTIC_FEEDBACK_NONE = "None";
    public static final String HAPTIC_FEEDBACK_SLIDER = "Slider";
    public static final String HAPTIC_FEEDBACK_SLIDER_SCALED = "Scaled";

    public static final int KIDS_TIMER_DISABLED = 0;
    public static final int KIDS_TIMER_STARTED = 1;
    public static final int KIDS_TIMER_ENABLED = 2;
    public static final int KIDS_TIMER_RUNNNING = 3;
    public static final int KIDS_TIMER_ENDED = 999;

    public SoundPool soundPool;
    public int[] sounds = {0,0,0,0,0,0,0,0,0,0,0};  // Bell, Horn_Start, Horn_Loop, Horn_End, Whistle_Start, Whistle_Loop, Whistle_End
    public int[] soundsStreamId = {0,0,0,0,0,0,0,0,0,0,0};
    public boolean[] soundsPlaying = {false,false,false,false,false,false,false,false,false,false,false};
    public static final int SOUND_BELL_START = 0;
    public static final int SOUND_BELL_LOOP = 1;
    public static final int SOUND_BELL_END = 2;
    public static final int SOUND_HORN_START = 3;
    public static final int SOUND_HORN_LOOP = 4;
    public static final int SOUND_HORN_END = 5;
    public static final int SOUND_WHISTLE_START = 6;
    public static final int SOUND_WHISTLE_LOOP = 7;
    public static final int SOUND_WHISTLE_END = 8;
    public String prefDeviceSounds = "none";

    public int [] soundsSteam = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    public int [] soundsSteamStreamId = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    public boolean[] soundsSteamPlaying = {false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false};

    public int [] soundsDiesel645turbo = {0,0,0,0,0};
    public int [] soundsDiesel645turboStreamId = {0,0,0,0,0};
    public boolean[] soundsDiesel645turboPlaying = {false,false,false,false,false};

    class comm_thread extends Thread {
        JmDNS jmdns = null;
        volatile boolean endingJmdns = false;
        withrottle_listener listener;
        android.net.wifi.WifiManager.MulticastLock multicast_lock;
        socket_WiT socketWiT;
        PhoneListener phone;
        heartbeat heart = new heartbeat();
        private long lastSentMs = System.currentTimeMillis();
        private long lastQueuedMs = System.currentTimeMillis();
        ;

        comm_thread() {
            super("comm_thread");
            this.start();
        }

        //Listen for a WiThrottle service advertisement on the LAN.
        public class withrottle_listener implements ServiceListener {

            public void serviceAdded(ServiceEvent event) {
                //          Log.d("Engine_Driver", String.format("serviceAdded fired"));
                //A service has been added. If no details, ask for them 
                Log.d("Engine_Driver", String.format("threaded_application.serviceAdded for '%s', Type='%s'", event.getName(), event.getType()));
                ServiceInfo si = jmdns.getServiceInfo(event.getType(), event.getName(), 0);
                if (si == null || si.getPort() == 0) {
                    Log.d("Engine_Driver", String.format("threaded_application.serviceAdded, requesting details: '%s', Type='%s'", event.getName(), event.getType()));
                    jmdns.requestServiceInfo(event.getType(), event.getName(), true, 1000);
                }
            }

            public void serviceRemoved(ServiceEvent event) {
                //Tell the UI thread to remove from the list of services available.
                sendMsg(connection_msg_handler, message_type.SERVICE_REMOVED, event.getName()); //send the service name to be removed
                Log.d("Engine_Driver", String.format("threaded_application.serviceRemoved: '%s'", event.getName()));
            }

            public void serviceResolved(ServiceEvent event) {
                //          Log.d("Engine_Driver", String.format("threaded_application.serviceResolved fired"));
                //A service's information has been resolved. Send the port and service name to connect to that service.
                int port = event.getInfo().getPort();
                String host_name = event.getInfo().getName(); //
                Inet4Address[] ip_addresses = event.getInfo().getInet4Addresses();  //only get ipV4 address
                String ip_address = ip_addresses[0].toString().substring(1);  //use first one, since WiThrottle is only putting one in (for now), and remove leading slash

                //Tell the UI thread to update the list of services available.
                HashMap<String, String> hm = new HashMap<>();
                hm.put("ip_address", ip_address);
                hm.put("port", ((Integer) port).toString());
                hm.put("host_name", host_name);

                Message service_message = Message.obtain();
                service_message.what = message_type.SERVICE_RESOLVED;
                service_message.arg1 = port;
                service_message.obj = hm;  //send the hashmap as the payload
                boolean sent = false;
                try {
                    sent = connection_msg_handler.sendMessage(service_message);
                } catch (Exception ignored) {
                }
                if (!sent)
                    service_message.recycle();

                Log.d("Engine_Driver", String.format("threaded_application.serviceResolved - %s(%s):%d -- %s", host_name, ip_address, port, event.toString().replace(System.getProperty("line.separator"), " ")));

            }
        }

        void start_jmdns() {
            //Set up to find a WiThrottle service via ZeroConf
            try {
                if (client_address != null) {
                    WifiManager wifi = (WifiManager) threaded_application.this.getSystemService(Context.WIFI_SERVICE);

                    if (multicast_lock == null) {  //do this only as needed
                        multicast_lock = wifi.createMulticastLock("engine_driver");
                        multicast_lock.setReferenceCounted(true);
                    }

                    Log.d("Engine_Driver", "threaded_application.start_jmdns: local IP addr " + client_address);

                    jmdns = JmDNS.create(client_address_inet4, client_address);  //pass ip as name to avoid hostname lookup attempt

                    listener = new withrottle_listener();
                    Log.d("Engine_Driver", "threaded_application.start_jmdns: listener created");

                } else {
                    safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppNoLocalIp), Toast.LENGTH_SHORT);
                }
            } catch (Exception except) {
                Log.e("Engine_Driver", "start_jmdns - Error creating withrottle listener: " + except.getMessage());
                safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorCreatingWiThrottle, except.getMessage()), Toast.LENGTH_SHORT);
            }
        }

        //end_jmdns() takes a long time, so put it in its own thread
        void end_jmdns() {
            Thread jmdnsThread = new Thread("EndJmdns") {
                @Override
                public void run() {
                    try {
                        Log.d("Engine_Driver", "threaded_application.end_jmdns: removing jmdns listener");
                        jmdns.removeServiceListener("_withrottle._tcp.local.", listener);
                        multicast_lock.release();
                    } catch (Exception e) {
                        Log.d("Engine_Driver", "threaded_application.end_jmdns: exception in jmdns.removeServiceListener()");
                    }
                    try {
                        Log.d("Engine_Driver", "threaded_application.end_jmdns: calling jmdns.close()");
                        jmdns.close();
                        Log.d("Engine_Driver", "threaded_application.end_jmdns: after jmdns.close()");
                    } catch (Exception e) {
                        Log.d("Engine_Driver", "threaded_application.end_jmdns: exception in jmdns.close()");
                    }
                    jmdns = null;
                    endingJmdns = false;
                    Log.d("Engine_Driver", "threaded_application.end_jmdns run exit");
                }
            };
            if (jmdnsIsActive()) {      //only need to run one instance of this thread to terminate jmdns 
                endingJmdns = true;
                jmdnsThread.start();
                Log.d("Engine_Driver", "threaded_application.end_jmdns active so ending it and starting thread to remove listener");
            } else {
                jmdnsThread = null;
                Log.d("Engine_Driver", "threaded_application.end_jmdns not active");
            }
        }

        boolean jmdnsIsActive() {
            return jmdns != null && !endingJmdns;
        }

        /*
          add configuration of digitrax LnWi or DCCEX to discovered list, since they do not provide mDNS
         */
        void addFakeDiscoveredServer(String entryName, String clientAddr, String entryPort) {

            if (clientAddr == null || clientAddr.lastIndexOf(".") < 0)
                return; //bail on unexpected value

            //assume that the server is at x.y.z.1
            String server_addr = clientAddr.substring(0, clientAddr.lastIndexOf("."));
            server_addr += ".1";
            HashMap<String, String> hm = new HashMap<>();
            hm.put("ip_address", server_addr);
            hm.put("port", entryPort);
            hm.put("host_name", entryName);

            Message service_message = Message.obtain();
            service_message.what = message_type.SERVICE_RESOLVED;
            service_message.arg1 = port;
            service_message.obj = hm;  //send the hashmap as the payload
            boolean sent = false;
            try {
                sent = connection_msg_handler.sendMessage(service_message);
            } catch (Exception ignored) {
            }
            if (!sent)
                service_message.recycle();

            Log.d("Engine_Driver", String.format("t_a: added '%s' at %s to Discovered List", entryName, server_addr));

        }

        @SuppressLint("HandlerLeak")
        class comm_handler extends Handler {
            //All of the work of the communications thread is initiated from this function.

            @SuppressLint({"DefaultLocale", "ApplySharedPref"})
            public void handleMessage(Message msg) {
//                Log.d("Engine_Driver", "threaded_application.comm_handler: message: " +msg.what);
                switch (msg.what) {
                    // note: if the Thottle is sent in arg1, it is always expected to be a int
                    // if it is sent in arg0, it will be a string

                    //Start or Stop jmdns stuff, or add "fake" discovered servers
                    case message_type.SET_LISTENER:
                        if (client_ssid != null &&
                                client_ssid.matches("DCCEX_[0-9a-f]{6}$")) {
                            //add "fake" discovered server entry for DCCEX: DCCEX_123abc
                            addFakeDiscoveredServer(client_ssid, client_address, "2560");
                        } else if (client_ssid != null &&
                                client_ssid.matches("^Dtx[0-9]{1,2}-.*_[0-9,A-F]{4}-[0-9]{1,3}$")) {
                            //add "fake" discovered server entry for Digitrax LnWi: Dtx1-LnServer_0009-7
                            addFakeDiscoveredServer(client_ssid, client_address, "12090");
                        } else {
                            //arg1= 1 to turn on, arg1=0 to turn off
                            if (msg.arg1 == 0) {
                                end_jmdns();
                            } else {
                                //show message if using mobile data
                                if ((!client_type.equals("WIFI")) && (prefAllowMobileData)) {
                                    safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppNotWIFI, client_type), Toast.LENGTH_LONG);
                                }
                                if (jmdns == null) {   //start jmdns if not started
                                    start_jmdns();
                                    if (jmdns != null) {  //don't bother if jmdns didn't start
                                        try {
                                            multicast_lock.acquire();
                                        } catch (Exception e) {
                                            //log message, but keep going if this fails
                                            Log.d("Engine_Driver", "threaded_application.comm_handler: multicast_lock.acquire() failed");
                                        }
                                        jmdns.addServiceListener("_withrottle._tcp.local.", listener);
                                        Log.d("Engine_Driver", "threaded_application.comm_handler: jmdns listener added");
                                    } else {
                                        Log.d("Engine_Driver", "threaded_application.comm_handler: jmdns not running, didn't start listener");
                                    }
                                } else {
                                    Log.d("Engine_Driver", "threaded_application.comm_handler: jmdns already running");
                                }
                            }
                        }
                        break;

                    //Connect to the WiThrottle server.
                    case message_type.CONNECT:

                        //The IP address is stored in the obj as a String, the port is stored in arg1.
                        String new_host_ip = msg.obj.toString();
                        new_host_ip = new_host_ip.trim();
                        int new_port = msg.arg1;

                        //avoid duplicate connects, seen when user clicks address multiple times quickly
                        if (socketWiT != null && socketWiT.SocketGood()
                                && new_host_ip.equals(host_ip) && new_port == port) {
                            Log.d("Engine_Driver", "t_a: Duplicate CONNECT message received.");
                            break;
                        }

                        //clear app.thread shared variables so they can be reinitialized
                        initShared();
                        fastClockSeconds = 0L;

                        //store ip and port in global variables
                        host_ip = new_host_ip;
                        port = new_port;
                        // skip url checking on Play Protect
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            WebView.setSafeBrowsingWhitelist(Collections.singletonList(host_ip), null);
                        }

                        //attempt connection to WiThrottle server
                        socketWiT = new socket_WiT();
                        if (socketWiT.connect()) {
                            sendThrottleName();
                            sendMsgDelay(comm_msg_handler, 5000L, message_type.CONNECTION_COMPLETED_CHECK);
                            phone = new PhoneListener();
                        } else {
                            host_ip = null;  //clear vars if failed to connect
                            port = 0;
                        }
                        break;

                    //Release one or all locos on the specified throttle.  addr is in msg (""==all), arg1 holds whichThrottle.
                    case message_type.RELEASE: {
                        int delays = 0;
                        String addr = msg.obj.toString();
                        final int whichThrottle = msg.arg1;
                        final boolean releaseAll = (addr.length() == 0);

                        if (releaseAll || consists[whichThrottle].isEmpty()) {
                            addr = "";
                            function_labels[whichThrottle] = new LinkedHashMap<>();
                            function_states[whichThrottle] = new boolean[32];
                        }
                        if (prefs.getBoolean("stop_on_release_preference",                         //send stop command before releasing (if set in prefs)
                                getResources().getBoolean(R.bool.prefStopOnReleaseDefaultValue))) {
                            witSetSpeedZero(whichThrottle);
                            delays++;
                        }

//                        releaseLoco(addr, whichThrottle, delays * WiThrottle_Msg_Interval);
                        releaseLoco(addr, whichThrottle, 0);
                        break;
                    }

                    //estop requested.   arg1 holds whichThrottle
                    //  M0A*<;>X  was(0X)
                    case message_type.ESTOP: {
                        final int whichThrottle = msg.arg1;
                        withrottle_send(String.format("M%sA*<;>X", throttleIntToString(whichThrottle)));  //send eStop request
                        break;
                    }

                    case message_type.RESTART_APP: {
                        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
                        sharedPreferences.edit().putBoolean("prefForcedRestart", true).commit();
                        sharedPreferences.edit().putInt("prefForcedRestartReason", msg.arg1).commit();
                        alert_activities(message_type.RESTART_APP, "");
                        break;
                    }

                    case message_type.RELAUNCH_APP: {
                        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
                        sharedPreferences.edit().putBoolean("prefForcedRestart", true).commit();
                        sharedPreferences.edit().putInt("prefForcedRestartReason", msg.arg1).commit();
                        alert_activities(message_type.RELAUNCH_APP, "");

                        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Runtime.getRuntime().exit(0); // really force the kill
                        break;
                    }

                    //Disconnect from the WiThrottle server and Shutdown
                    case message_type.DISCONNECT: {
                        Log.d("Engine_Driver", "t_a: TA Disconnect");
                        doFinish = true;
                        Log.d("Engine_Driver", "t_a: TA alert all activities to shutdown");
                        alert_activities(message_type.SHUTDOWN, "");     //tell all activities to finish()
                        stoppingConnection();

                        // arg1 = 1 means shutdown with no delays
                        if (msg.arg1 == 1) {
                            withrottle_send("Q");
                            shutdown(true);
                        } else {
                            // At this point TA needs to send the quit message to WiT and then shutdown.
                            // However the DISCONNECT message also tells the Throttle activity to release all throttles
                            // and that process can take some time:
                            //  release request messages and possibly zero speed messages need to be sent to WiT
                            //  for each active throttle and WiT will respond with release messages.
                            // So we delay the Quit and shutdown to allow time for all the throttle messages to complete
                            sendMsgDelay(comm_msg_handler, 1500L, message_type.WITHROTTLE_QUIT);
                            if (!sendMsgDelay(comm_msg_handler, 1600L, message_type.SHUTDOWN)) {
                                shutdown(false);
                            }
                        }

                        soundPool.autoPause();
                        break;
                    }

                    //Set up an engine to control. The address of the engine is given in msg.obj and whichThrottle is in arg1
                    //Optional rostername if present is separated from the address by the proper delimiter
                    case message_type.REQ_LOCO_ADDR: {
//                        int delays = 0;
                        final String addr = msg.obj.toString();
                        final int whichThrottle = msg.arg1;
                        if (prefs.getBoolean("drop_on_acquire_preference",
                                getResources().getBoolean(R.bool.prefDropOnAcquireDefaultValue))) {
                            releaseLoco("*", whichThrottle, 0);
//                            delays++;
                        }
//                        acquireLoco(addr, whichThrottle, delays * WiThrottle_Msg_Interval);
                        acquireLoco(addr, whichThrottle, 0);
                        break;
                    }

                    //send commands to steal the last requested address
                    case message_type.STEAL: {
                        String addr = msg.obj.toString();
                        int whichThrottle = msg.arg1;
                        stealLoco(addr, whichThrottle);
                        break;
                    }

                    //Adjust the throttle's speed. whichThrottle is in arg 1 and arg2 holds the value of the speed to set.
                    //  message sent is formatted M1A*<;>V13  was(1V13)
                    case message_type.VELOCITY: {
                        final int whichThrottle = msg.arg1;
                        final int speed = msg.arg2;
                        witSetSpeed(whichThrottle, speed);
                        break;
                    }
                    //Change direction. address is in msg, whichThrottle is in arg 1 and arg2 holds the direction to change to.
                    //  message sent is formatted M1AS96<;>R0  was(1R0<;>S96)
                    case message_type.DIRECTION: {
                        final String addr = msg.obj.toString();
                        final int whichThrottle = msg.arg1;
                        withrottle_send(String.format("M%sA%s<;>R%d", throttleIntToString(whichThrottle), addr, msg.arg2));
                        break;
                    }
                    //Set or unset a function. whichThrottle+addr is in the msg, arg1 is the function number, arg2 is set or unset.
                    //  M1AS96<;>F01  was(1F01<;>S96)
                    case message_type.FUNCTION: {
                        String addr = msg.obj.toString();
                        final char cWhichThrottle = addr.charAt(0);
                        addr = addr.substring(1);
                        if (addr.length() == 0)
                            addr = "*";
                        withrottle_send(String.format("M%sA%s<;>F%d%d", cWhichThrottle, addr, msg.arg2, msg.arg1));
                        break;
                    }
                    //Set or unset a function. whichThrottle+addr is in the msg, arg1 is the function number, arg2 is set or unset.
                    case message_type.FORCE_FUNCTION: {
                        String addr = msg.obj.toString();
                        final char cWhichThrottle = addr.charAt(0);
                        addr = addr.substring(1);
                        if (addr.length() == 0)
                            addr = "*";
                        withrottle_send(String.format("M%sA%s<;>f%d%d", cWhichThrottle, addr, msg.arg2, msg.arg1));
                        break;
                    }
                    //send command to change turnout.  msg = (T)hrow, (C)lose or (2)(toggle) + systemName
                    case message_type.TURNOUT: {
                        final String cmd = msg.obj.toString();
                        withrottle_send("PTA" + cmd);
                        break;
                    }
                    //send command to route turnout.  msg = 2(toggle) + systemName
                    case message_type.ROUTE: {
                        final String cmd = msg.obj.toString();
                        withrottle_send("PRA" + cmd);
                        break;
                    }
                    //send command to change power setting, new state is passed in arg1
                    case message_type.POWER_CONTROL:
                        withrottle_send(String.format("PPA%d", msg.arg1));
                        break;
                    //send whatever command string comes in obj to Withrottle Server
                    case message_type.WITHROTTLE_SEND:
                        withrottle_send(msg.obj.toString());
                        break;
                    // Request the throttle's speed and direction. whichThrottle is in arg 1
                    case message_type.WIT_QUERY_SPEED_AND_DIRECTION: {
                        final int whichThrottle = msg.arg1;
                        witRequestSpeedAndDir(whichThrottle);
                        break;
                    }
                    //send Q to withrottle server
                    case message_type.WITHROTTLE_QUIT:
                        if (socketWiT != null && socketWiT.SocketGood())
                            withrottle_send("Q");
                        break;

                    //send heartbeat start command to withrottle server
                    case message_type.SEND_HEARTBEAT_START:
                        heart.setHeartbeatSent(true);
                        withrottle_send("*+");
                        break;

                    //Current Time clock display preference change, sets mainapp.fastClockFormat
                    case message_type.CLOCK_DISPLAY_CHANGED:
                        if (!doFinish) {
                            try {
                                fastClockFormat = Integer.parseInt(prefs.getString("ClockDisplayTypePreference", "0"));
                            } catch (NumberFormatException e) {
                                fastClockFormat = 0;
                            }
                            alert_activities(message_type.TIME_CHANGED, "");
                        }
                        break;

                    // SHUTDOWN - terminate socketWiT and it's done
                    case message_type.SHUTDOWN:
                        shutdown(false);
                        break;

                    case message_type.CONNECTION_COMPLETED_CHECK:
                        //if not successfully connected to a 2.0+ server by this time, kill connection
                        if (withrottle_version < 2.0) {
                            sendMsg(comm_msg_handler, message_type.TOAST_MESSAGE,
                                    "timeout waiting for VN message for "
                                            + host_ip + ":" + port + ", disconnecting");
                            if (socketWiT != null) {
                                socketWiT.disconnect(true, true);     //just close the socket
                            }
                        }
                        break;

                    // update of roster-related data completed in background
                    case message_type.ROSTER_UPDATE:
                        if (!doFinish) {
                            alert_activities(message_type.ROSTER_UPDATE, "");
                        }
                        break;

                    // WiT socket is down and reconnect attempt in prog
                    case message_type.WIT_CON_RETRY:
                        if (!doFinish) {
                            alert_activities(message_type.WIT_CON_RETRY, msg.obj.toString());
                        }
                        break;

                    // WiT socket is back up
                    case message_type.WIT_CON_RECONNECT:
                        if (!doFinish) {
                            sendThrottleName();
                            reacquireAllConsists();
                            alert_activities(message_type.WIT_CON_RECONNECT, msg.obj.toString());
                        }
                        break;

                    //send whatever message string comes in obj as a long toast message
                    case message_type.TOAST_MESSAGE:
                        safeToast(msg.obj.toString(), Toast.LENGTH_LONG);
                        break;

                    case message_type.KIDS_TIMER_ENABLE:
                        sendMsg(throttle_msg_handler, message_type.KIDS_TIMER_ENABLE, "", 0);
                        break;
                    case message_type.KIDS_TIMER_START:
                        sendMsg(throttle_msg_handler, message_type.KIDS_TIMER_START, "", 0);
                        break;
                    case message_type.KIDS_TIMER_END:
                        sendMsg(throttle_msg_handler, message_type.KIDS_TIMER_END, "", 0);
                        break;
                    case message_type.KIDS_TIMER_TICK:
                        sendMsg(throttle_msg_handler, message_type.KIDS_TIMER_TICK, "", msg.arg1);
                        break;
                    case message_type.IMPORT_SERVER_AUTO_AVAILABLE:
                        Log.d("Engine_Driver", "threaded_application.comm_handler: message: AUTO_IMPORT_URL_AVAILABLE " + msg.what);
                        sendMsg(throttle_msg_handler, message_type.IMPORT_SERVER_AUTO_AVAILABLE, "", 0);
                        break;

                    case message_type.HTTP_SERVER_NAME_RECEIVED:
                        String retrievedServerName = msg.obj.toString();
                        if (connectedHostName != null &&
                                !retrievedServerName.equals(connectedHostName) &&
                                !connectedHostName.equals(demo_host)) {
                            updateConnectionList(retrievedServerName);
                        }
                        break;
                }
            }
        }

        private void stoppingConnection() {
            heart.stopHeartbeat();
            if (phone != null) {
                phone.disable();
                phone = null;
            }
            end_jmdns();
            dlMetadataTask.stop();
            dlRosterTask.stop();
        }

        private void shutdown(boolean fast) {
            Log.d("Engine_Driver", "threaded_application.Shutdown");
            if (socketWiT != null) {
                socketWiT.disconnect(true, fast);     //stop reading from the socket
            }
            Log.d("Engine_Driver", "threaded_application.Shutdown: socketWit down");
            saveSharedPreferencesToFileIfAllowed();
            host_ip = null;
            port = 0;
            reinitStatics();                    // ensure activities are ready for relaunch
            doFinish = false;                   //ok for activities to run if restarted after this

            dlRosterTask.stop();
            dlMetadataTask.stop();

            // make sure flashlight is switched off at shutdown
            if (flashlight != null) {
                flashlight.setFlashlightOff();
                flashlight.teardown();
            }
            flashState = false;
            Log.d("Engine_Driver", "threaded_application.Shutdown finished");
        }

        private void sendThrottleName() {
            sendThrottleName(true);
        }

        private void sendThrottleName(Boolean sendHWID) {
            String s = prefs.getString("throttle_name_preference", threaded_application.context.getResources().getString(R.string.prefThrottleNameDefaultValue));
            withrottle_send("N" + s);  //send throttle name
            if (sendHWID) {
                if (deviceId.equals("")) {
                    deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
                }
                if (deviceId.equals("")) {
                    withrottle_send("HU" + s);  //also send throttle name as the UDID
                } else {
                    withrottle_send("HU" + deviceId);
                }
            }
        }


        /* ask for specific loco to be added to a throttle
             input addr is formatted "L37<;>CSX37" or "S96" (if no roster name)
             msgTxt will be formatted M0+L1012<;>EACL1012 or M1+S96<;>S96 */
        private void acquireLoco(String addr, int whichThrottle, int interval) {
            String rosterName;
            String address;
            String[] as = splitByString(addr, "<;>");
            if (as.length > 1) {
                address = as[0];
                rosterName = "E" + as[1];
            } else { //if no rostername, just use address for both
                address = addr;
                rosterName = addr;
            }

            //format multithrottle request for loco M1+L37<;>ECSX37
            String msgTxt;
            msgTxt = String.format("M%s+%s<;>%s", throttleIntToString(whichThrottle), address, rosterName);  //add requested loco to this throttle
//            Log.d("Engine_Driver", "t_a: acquireLoco: addr:'" + addr + "' msgTxt: '" + msgTxt + "'");
//            sendMsgDelay(comm_msg_handler, interval, message_type.WITHROTTLE_SEND, msgTxt);
            sendMsg(comm_msg_handler, message_type.WITHROTTLE_SEND, msgTxt);

            if (heart.getInboundInterval() > 0 && withrottle_version > 0.0 && !heart.isHeartbeatSent()) {
//                sendMsgDelay(comm_msg_handler, interval + WITHROTTLE_SPACING_INTERVAL, message_type.SEND_HEARTBEAT_START);
                sendMsg(comm_msg_handler, message_type.SEND_HEARTBEAT_START);
            }
        }

        /* "steal" will send the MTS command */
        private void stealLoco(String addr, int whichThrottle) {
            if (addr != null) {
                String msgtxt = String.format("M%sS%s<;>%s", throttleIntToString(whichThrottle), addr, addr);
                sendMsg(comm_msg_handler, message_type.WITHROTTLE_SEND, msgtxt);
            }
        }

        //release all locos for throttle using '*', or a single loco using address
        private void releaseLoco(String addr, int whichThrottle, long interval) {
            String msgtxt = String.format("M%s-%s<;>r", throttleIntToString(whichThrottle), (!addr.equals("") ? addr : "*"));
            sendMsgDelay(comm_msg_handler, interval, message_type.WITHROTTLE_SEND, msgtxt);
        }

        private void reacquireAllConsists() {
            for (int i = 0; i < maxThrottlesCurrentScreen; i++)
                reacquireConsist(consists[i], i);
        }

        private void reacquireConsist(Consist c, int whichThrottle) {
            int delays = 0;
            for (ConLoco l : c.getLocos()) { // reacquire each confirmed loco in the consist
                if (l.isConfirmed()) {
                    String addr = l.getAddress();
                    String roster_name = l.getRosterName();
                    if (roster_name != null)  // add roster selection info if present
                        addr += "<;>" + roster_name;
//                    acquireLoco(addr, whichThrottle, delays * WITHROTTLE_SPACING_INTERVAL); //ask for next loco, with 0 or more delays
                    acquireLoco(addr, whichThrottle, 0); //ask for next loco, with 0 or more delays
                    delays++;
                }
            }
        }

        private void process_response(String response_str) {
            /* see java/arc/jmri/jmrit/withrottle/deviceserver.java for server code and some documentation
          VN<Version#>
          RL<RosterSize>]<RosterList>
          RF<RosterFunctionList>
          RS<2ndRosterFunctionList>
          *<HeartbeatIntervalInSeconds>
          PTL[<SystemTurnoutName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
          PTA<NewTurnoutState><SystemName>
          PPA<NewPowerState> where state 0=off, 1=on, 2=unknown
          M<throttleid> multi-throttle command
          TODO: add remaining items, or move examples into code below
             */

            //send response to debug log for review
            Log.d("Engine_Driver", "<--:" + response_str);

            boolean skipAlert = false;          //set to true if the Activities do not need to be Alerted

            switch (response_str.charAt(0)) {

                //handle responses from MultiThrottle function
                case 'M': {
                    if (response_str.length() < 5) { //must be at least Mtxs9
                        Log.d("Engine_Driver", "t_a: invalid response string: '" + response_str + "'");
                        break;
                    }
                    String sWhichThrottle = response_str.substring(1, 2);
                    int whichThrottle = throttleCharToInt(sWhichThrottle.charAt(0));
                    String[] ls = splitByString(response_str, "<;>");    //drop off separator
                    String addr = ls[0].substring(3);
                    char com2 = response_str.charAt(2);
                    //loco was successfully added to a throttle
                    if (com2 == '+') {  //"MT+L2591<;>"  loco was added
                        Consist con = consists[whichThrottle];
                        if (con.getLoco(addr) != null) { //loco was added to consist in select_loco
                            con.setConfirmed(addr);
                            addLocoToRecents(con.getLoco(addr));
                        } else if (con.isWaitingOnID()) { //we were waiting for this response to get address
                            ConLoco conLoco = new ConLoco(addr);
                            conLoco.setFunctionLabelDefaults(threaded_application.this, whichThrottle);
                            con.add(conLoco);
                            con.setWhichSource(addr, 1); //entered by address, not roster
                            con.setConfirmed(addr);
                            addLocoToRecents(con.getLoco(addr));
                            Log.d("Engine_Driver", "loco '" + addr + "' ID'ed on programming track and added to " + whichThrottle);
                        } else {
                            Log.d("Engine_Driver", "loco '" + addr + "' not selected but assigned by server to " + whichThrottle);
                        }

                        String consistname = getConsistNameFromAddress(addr); //check for a JMRI consist for this address,
                        if (consistname != null) { //if found, request function keys for lead, format MTAS13<;>CL1234
                            String[] cna = splitByString(consistname, "+");
                            String cmd = String.format("M%sA%s<;>C%s", throttleIntToString(whichThrottle), addr, cvtToLAddr(cna[0]));
                            Log.d("Engine_Driver", "t_a: rqsting fkeys for lead loco " + cvtToLAddr(cna[0]));
                            withrottle_send(cmd);
                        }

                    } else if (com2 == '-') { //"MS-L6318<;>"  loco removed from throttle
                        consists[whichThrottle].remove(addr);
                        Log.d("Engine_Driver", "t_a: loco " + addr + " dropped from " + throttleIntToString(whichThrottle));

                    } else if (com2 == 'L') { //list of function buttons
                        if (consists[whichThrottle].isLeadFromRoster()) { // if not from the roster ignore the function labels that WiT has sent back
                            String lead;
                            lead = consists[whichThrottle].getLeadAddr();
                            if (lead.equals(addr))                        //*** temp - only process if for lead engine in consist
                                process_roster_function_string("RF29}|{1234(L)" + ls[1], whichThrottle);  //prepend some stuff to match old-style
                            consists[whichThrottle].setFunctionLabels(addr, "RF29}|{1234(L)" + ls[1], threaded_application.this);
                        }

                    } else if (com2 == 'A') { //process change in function value  MTAL4805<;>F028
                        if (ls.length == 2 && "F".equals(ls[1].substring(0, 1))) {
                            try {
                                process_function_state(whichThrottle, Integer.valueOf(ls[1].substring(2)), "1".equals(ls[1].substring(1, 2)));
                            } catch (NumberFormatException | StringIndexOutOfBoundsException ignore) {
                                Log.d("Engine_Driver", "bad incoming message data, unable to parse '" + response_str + "'");
                            }
                        }

                    } else if (com2 == 'S') { //"MTSL4425<;>L4425" loco is in use, prompt for Steal
                        Log.d("Engine_Driver", "t_a: rcvd MTS, request prompt for " + addr + " on " + throttleIntToString(whichThrottle));
                        sendMsg(throttle_msg_handler, message_type.REQ_STEAL, addr, whichThrottle);
                    }

                    break;
                }
                case 'V':
                    Double old_vn = withrottle_version;
                    try {
                        withrottle_version = Double.parseDouble(response_str.substring(2));
                    } catch (Exception e) {
                        Log.e("Engine_Driver", "process response: invalid WiT version string");
                        withrottle_version = 0.0;
                    }
                    //only move on to Throttle screen if version received is 2.0+
                    if (withrottle_version >= 2.0) {
                        if (!withrottle_version.equals(old_vn)) { //only if changed
                            sendMsg(connection_msg_handler, message_type.CONNECTED);
                        } else {
                            Log.d("Engine_Driver", "version already set to " + withrottle_version + ", ignoring");
                        }
                    } else {
                        safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppWiThrottleNotSupported, response_str.substring(2)), Toast.LENGTH_SHORT);
                        socketWiT.disconnect(false);
                    }
                    break;

                case 'H':
                    if (response_str.charAt(1) == 'T') { //set hardware server type, HTMRC for example
                        setServerType(response_str.substring(2)); //store the type
                    } else if (response_str.charAt(1) == 't') { //server description string "HtMy Server Details go here"
                        setServerDescription(response_str.substring(2)); //store the description
                    } else if (response_str.charAt(1) == 'M') { //alert message sent from server to throttle
                        safeToast(response_str.substring(2), Toast.LENGTH_LONG); // copy to UI as toast message
                        //see if it is a turnout fail
                        if ((response_str.contains("Turnout")) || (response_str.contains("create not allowed"))) {
                            Pattern pattern = Pattern.compile(".*'(.*)'.*");
                            Matcher matcher = pattern.matcher(response_str);
                            if (matcher.find()) {
                                sendMsg(turnouts_msg_handler, message_type.WIT_TURNOUT_NOT_DEFINED, matcher.group(1));
                            }
                        }

                    } else if (response_str.charAt(1) == 'm') { //info message sent from server to throttle
                        safeToast(response_str.substring(2), Toast.LENGTH_SHORT); // copy to UI as toast message
                    }
                    break;

                case '*':
                    try {
                        heartbeatInterval = Integer.parseInt(response_str.substring(1)) * 1000;  //convert to milliseconds
                    } catch (Exception e) {
                        Log.d("Engine_Driver", "t_a: process response: invalid WiT hearbeat string");
                        heartbeatInterval = 0;
                    }
                    heart.startHeartbeat(heartbeatInterval);
                    break;

                case 'R': //Roster
                    switch (response_str.charAt(1)) {

                        case 'C':
                            if (response_str.charAt(2) == 'C' || response_str.charAt(2) == 'L') {  //RCC1 or RCL1 (treated the same in ED)
                                clear_consist_list();
                            } else if (response_str.charAt(2) == 'D') {  //RCD}|{88(S)}|{Consist Name]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
                                process_consist_list(response_str);
                            }
                            break;

                        case 'L':
                            //                  roster_list_string = response_str.substring(2);  //set app variable
                            process_roster_list(response_str);  //process roster list
                            break;

                        case 'F':   //RF29}|{2591(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
                            process_roster_function_string(response_str.substring(2), 0);
                            break;

                        case 'S': //RS29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
                            process_roster_function_string(response_str.substring(2), 1);
                            break;

                    }  //end switch inside R
                    break;
                case 'P': //Panel
                    switch (response_str.charAt(1)) {
                        case 'T': //turnouts
                            if (response_str.charAt(2) == 'T') {  //turnout control allowed
                                process_turnout_titles(response_str);
                            }
                            if (response_str.charAt(2) == 'L') {  //list of turnouts
                                process_turnout_list(response_str);  //process turnout list
                            }
                            if (response_str.charAt(2) == 'A') {  //action?  changes to turnouts
                                process_turnout_change(response_str);  //process turnout changes
                            }
                            break;

                        case 'R':  //routes
                            if (response_str.charAt(2) == 'T') {  //route  control allowed
                                process_route_titles(response_str);
                            }
                            if (response_str.charAt(2) == 'L') {  //list of routes
                                process_route_list(response_str);  //process route list
                            }
                            if (response_str.charAt(2) == 'A') {  //action?  changes to routes
                                process_route_change(response_str);  //process route changes
                            }
                            break;

                        case 'P':  //power
                            if (response_str.charAt(2) == 'A') {  //change power state
                                String oldState = power_state;
                                power_state = response_str.substring(3);
                                if (power_state.equals(oldState)) {
                                    skipAlert = true;
                                }
                            }
                            break;

                        case 'F':  //FastClock message: PFT1581530521<;>2.0
                            //extracts and sets shared fastClockSeconds
                            if (response_str.startsWith("PFT")) {
                                fastClockSeconds = 0L;
                                String[] ta = splitByString(response_str.substring(3), "<;>"); //get the number between "PFT" and the "<;>"
                                try {
                                    fastClockSeconds = Long.parseLong(ta[0]);
                                } catch (NumberFormatException e) {
                                    Log.w("Engine_Driver", "unable to extract fastClockSeconds from '" + response_str + "'");
                                }
                                alert_activities(message_type.TIME_CHANGED, "");     //tell activities the time has changed
                            }
                            break;

                        case 'W':  //Web Server port
                            int oldPort = web_server_port;
                            try {
                                web_server_port = Integer.parseInt(response_str.substring(2));  //set app variable
                            } catch (Exception e) {
                                Log.d("Engine_Driver", "t_a: process response: invalid web server port string");
                            }
                            if (oldPort == web_server_port) {
                                skipAlert = true;
                            } else {
                                dlMetadataTask.get();           // start background metadata update
                                dlRosterTask.get();             // start background roster update

                            }
                            webServerNameHasBeenChecked = false;
                            break;
                    }  //end switch inside P
                    break;
            }  //end switch

            if (!skipAlert) {
                alert_activities(message_type.RESPONSE, response_str);  //send response to running activities
            }
        }  //end of process_response

        //parse roster functions list into appropriate app variable array
        //  //RF29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
        private void process_roster_function_string(String response_str, int whichThrottle) {

            Log.d("Engine_Driver", "t_a: processing function labels for " + throttleIntToString(whichThrottle));
            String[] ta = splitByString(response_str, "]\\[");  //split into list of labels

            //populate a temp label array from RF command string
            LinkedHashMap<Integer, String> function_labels_temp = new LinkedHashMap<>();
            int i = 0;
            for (String ts : ta) {
                if (i > 0 && !"".equals(ts)) { //skip first chunk, which is length, and skip any blank entries
                    function_labels_temp.put(i - 1, ts); //index is hashmap key, value is label string
                }  //end if i>0
                i++;
            }  //end for

            //set the appropriate global variable from the temp
            function_labels[whichThrottle] = function_labels_temp;

        }

        //parse roster list into appropriate app variable array
        //  RL2]\[NS2591}|{2591}|{L]\[NS4805}|{4805}|{L
        private void process_roster_list(String response_str) {
            //clear the global variable
            roster_entries = Collections.synchronizedMap(new LinkedHashMap<String, String>());
            //todo   RDB why don't we just clear the existing map with roster_entries.clear() instead of disposing and creating a new instance?

            String[] ta = splitByString(response_str, "]\\[");  //initial separation
            //initialize app arrays (skipping first)
            int i = 0;
            for (String ts : ta) {
                if (i > 0) { //skip first chunk
                    String[] tv = splitByString(ts, "}|{");  //split these into name, address and length
                    try {
                        roster_entries.put(tv[0], tv[1] + "(" + tv[2] + ")"); //roster name is hashmap key, value is address(L or S), e.g.  2591(L)
                    } catch (Exception e) {
                        Log.d("Engine_Driver", "t_a: process_roster_list caught Exception");  //ignore any bad stuff in roster entries
                    }
                }  //end if i>0
                i++;
            }  //end for

        }

        //parse consist list into appropriate mainapp hashmap
        //RCD}|{88(S)}|{Consist Name]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
        private void process_consist_list(String response_str) {
            String consist_addr = null;
            StringBuilder consist_desc = new StringBuilder();
            String consist_name = "";
            String[] ta = splitByString(response_str, "]\\[");  //initial separation
            String plus = ""; //plus sign for a separator
            //initialize app arrays (skipping first)
            int i = 0;
            for (String ts : ta) {
                if (i == 0) { //first chunk is a "header"
                    String[] tv = splitByString(ts, "}|{");  //split header chunk into header, address and name
                    consist_addr = tv[1];
                    consist_name = tv[2];
                } else {  //list of locos in consist
                    String[] tv = splitByString(ts, "}|{");  //split these into loco address and direction
                    consist_desc.append(plus).append(tv[0]);
                    plus = "+";
                }  //end if i==0
                i++;
            }  //end for
            Log.d("Engine_Driver", "t_a: consist header, addr='" + consist_addr
                    + "', name='" + consist_name + "', desc='" + consist_desc + "'");
            //don't add empty consists to list
            if (consist_desc.length() > 0) {
                consist_entries.put(consist_addr, consist_desc.toString());
            } else {
                Log.d("Engine_Driver", "skipping empty consist '" + consist_name + "'");
            }
        }

        //clear out any stored consists
        private void clear_consist_list() {
            consist_entries.clear();
        }


        //parse turnout change to update mainapp array entry
        //  PTA<NewState><SystemName>
        //  PTA2LT12
        private void process_turnout_change(String response_str) {
            if (to_system_names == null) return;  //ignore if turnouts not defined
            String newState = response_str.substring(3, 4);
            String systemName = response_str.substring(4);
            int pos = -1;
            for (String sn : to_system_names) { //TODO: rewrite for better lookup
                pos++;
                if (sn != null && sn.equals(systemName)) {
                    break;
                }
            }
            if (pos >= 0 && pos <= to_system_names.length) {  //if found, update to new value
                to_states[pos] = newState;
            }
        }  //end of process_turnout_change

        //parse turnout list into appropriate app variable array
        //  PTL[<SystemName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
        //  PTL]\[LT12}|{my12}|{1
        private void process_turnout_list(String response_str) {

            String[] ta = splitByString(response_str, "]\\[");  //initial separation
            //initialize app arrays (skipping first)
            to_system_names = new String[ta.length - 1];
            to_user_names = new String[ta.length - 1];
            to_states = new String[ta.length - 1];
            int i = 0;
            for (String ts : ta) {
                if (i > 0) { //skip first chunk, just message id
                    String[] tv = splitByString(ts, "}|{");  //split these into 3 parts, key and value
                    if (tv.length == 3) { //make sure split worked
                        to_system_names[i - 1] = tv[0];
                        to_user_names[i - 1] = tv[1];
                        to_states[i - 1] = tv[2];
                    }
                }  //end if i>0
                i++;
            }  //end for

        }

        private void process_turnout_titles(String response_str) {
            //PTT]\[Turnouts}|{Turnout]\[Closed}|{2]\[Thrown}|{4

            //clear the global variable
            to_state_names = new HashMap<>();

            String[] ta = splitByString(response_str, "]\\[");  //initial separation
            //initialize app arrays (skipping first)
            int i = 0;
            for (String ts : ta) {
                if (i > 1) { //skip first 2 chunks
                    String[] tv = splitByString(ts, "}|{");  //split these into value and key
                    to_state_names.put(tv[1], tv[0]);
                }  //end if i>0
                i++;
            }  //end for

        }

        //parse route list into appropriate app variable array
        //  PRA<NewState><SystemName>
        //  PRA2LT12
        private void process_route_change(String response_str) {
            String newState = response_str.substring(3, 4);
            String systemName = response_str.substring(4);
            int pos = -1;
            for (String sn : rt_system_names) {
                pos++;
                if (sn != null && sn.equals(systemName)) {
                    break;
                }
            }
            if (pos >= 0 && pos <= rt_system_names.length) {  //if found, update to new value
                rt_states[pos] = newState;
            }
        }  //end of process_route_change

        //parse route list into appropriate app variable array
        //  PRL[<SystemName><UserName><State>]repeat where state 1=Unknown,2=Active,4=Inactive,8=Inconsistent
        //  PRL]\[LT12}|{my12}|{1
        private void process_route_list(String response_str) {

            String[] ta = splitByString(response_str, "]\\[");  //initial separation
            //initialize app arrays (skipping first)
            rt_system_names = new String[ta.length - 1];
            rt_user_names = new String[ta.length - 1];
            rt_states = new String[ta.length - 1];
            int i = 0;
            for (String ts : ta) {
                if (i > 0) { //skip first chunk, just message id
                    String[] tv = splitByString(ts, "}|{");  //split these into 3 parts, key and value
                    rt_system_names[i - 1] = tv[0];
                    rt_user_names[i - 1] = tv[1];
                    rt_states[i - 1] = tv[2];
                }  //end if i>0
                i++;
            }  //end for

        }

        private void process_route_titles(String response_str) {
            //PRT

            //clear the global variable
            rt_state_names = new HashMap<>();

            String[] ta = splitByString(response_str, "]\\[");  //initial separation
            //initialize app arrays (skipping first)
            int i = 0;
            for (String ts : ta) {
                if (i > 1) { //skip first 2 chunks
                    String[] tv = splitByString(ts, "}|{");  //split these into value and key
                    rt_state_names.put(tv[1], tv[0]);
                }  //end if i>0
                i++;
            }  //end for
        }

        //parse function state string into appropriate app variable array
        private void process_function_state(int whichThrottle, Integer fn, boolean fState) {

            boolean skip = (fn > 2) && (prefAlwaysUseDefaultFunctionLabels)
                    && ((prefConsistFollowRuleStyle.equals(CONSIST_FUNCTION_RULE_STYLE_SPECIAL_EXACT))
                    || (prefConsistFollowRuleStyle.equals(CONSIST_FUNCTION_RULE_STYLE_SPECIAL_PARTIAL)));

            if (!skip) {
                try {
                    function_states[whichThrottle][fn] = fState;
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
            }
        }

        //
        // withrottle_send(String msg)
        //
        //send formatted msg to the socket using multithrottle format
        //  intermessage gap enforced by requeueing messages as needed
        private void withrottle_send(String msg) {
//            Log.d("Engine_Driver", "t_a: WiT send '" + msg + "'");
            if (msg == null) { //exit if no message
                Log.d("Engine_Driver", "--> null msg");
                return;
            } else if (socketWiT == null) {
                Log.e("Engine_Driver", "socketWiT is null, message '" + msg + "' not sent!");
                return;
            }

            long now = System.currentTimeMillis();
            long lastGap = now - lastSentMs;

            //send if sufficient gap between messages or msg is timingSensitive, requeue if not
            if (lastGap >= WiThrottle_Msg_Interval || timingSensitive(msg)) {
                //perform the send
                Log.d("Engine_Driver", "-->:" + msg.replaceAll("\n", "\u21B5") + " (" + lastGap + ")"); //replace newline with cr arrow
                lastSentMs = now;
                socketWiT.Send(msg);
            } else {
                //requeue this message
                int nextGap = Math.max((int) (lastQueuedMs - now), 0) + (WiThrottle_Msg_Interval + 5); //extra 5 for processing
                Log.d("Engine_Driver", "requeue:" + msg.replaceAll("\n", "\u21B5") +
                        ", lastGap=" + lastGap + ", nextGap=" + nextGap); //replace newline with cr arrow
                sendMsgDelay(comm_msg_handler, nextGap, message_type.WITHROTTLE_SEND, msg);
                lastQueuedMs = now + nextGap;
            }
        }  //end withrottle_send()

        /* true indicates that message should NOT be requeued as the timing of this message
             is critical.
         */
        private boolean timingSensitive(String msg) {
            boolean ret = false;
            if (msg.matches("^M[0-5]A.{1,5}<;>F[0-1][\\d]{1,2}$")) {
                ret = true;
            } //any function key message
            if (ret) Log.d("Engine_Driver", "timeSensitive message, not requeued: '{}'" + msg);
            return ret;
        }

        private void witSetSpeedZero(int whichThrottle) {
            witSetSpeed(whichThrottle, 0);
        }

        private void witSetSpeed(int whichThrottle, int speed) {
            withrottle_send(String.format("M%sA*<;>V%d", throttleIntToString(whichThrottle), speed));
        }

        private void witRequestSpeed(int whichThrottle) {
            withrottle_send(String.format("M%sA*<;>qV", throttleIntToString(whichThrottle)));
        }

        private void witRequestDir(int whichThrottle) {
            withrottle_send(String.format("M%sA*<;>qR", throttleIntToString(whichThrottle)));
        }

        private void witRequestSpeedAndDir(int whichThrottle) {
            withrottle_send(String.format("M%sA*<;>qV\nM%sA*<;>qR", throttleIntToString(whichThrottle), throttleIntToString(whichThrottle)));
        }

        public void run() {
            Looper.prepare();
            comm_msg_handler = new comm_handler();
            Looper.loop();
            Log.d("Engine_Driver", "comm_thread run() exit");
        }

        class socket_WiT extends Thread {
            InetAddress host_address;
            Socket clientSocket = null;
            BufferedReader inputBR = null;
            PrintWriter outputPW = null;
            private volatile boolean endRead = false;           //signals rcvr to terminate
            private volatile boolean socketGood = false;        //indicates socket condition
            private volatile boolean inboundTimeout = false;    //indicates inbound messages are not arriving from WiT
            private boolean firstConnect = false;               //indicates initial socket connection was achieved
            private int connectTimeoutMs = 3000; //connection timeout in milliseconds
            private int socketTimeoutMs = 500; //socket timeout in milliseconds

            private final int MAX_INBOUND_TIMEOUT_RETRIES = 2;
            private int inboundTimeoutRetryCount = 0;           // number of consecutive inbound timeouts
            private boolean inboundTimeoutRecovery = false;     // attempting to force WiT to respond


            socket_WiT() {
                super("socket_WiT");
            }

            public boolean connect() {

                //use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done
                boolean socketOk = HaveNetworkConnection();

                connectTimeoutMs = Integer.parseInt(prefs.getString("prefConnectTimeoutMs", getResources().getString(R.string.prefConnectTimeoutMsDefaultValue)));
                socketTimeoutMs = Integer.parseInt(prefs.getString("prefSocketTimeoutMs", getResources().getString(R.string.prefSocketTimeoutMsDefaultValue)));

                //validate address
                if (socketOk) {
                    try {
                        host_address = InetAddress.getByName(host_ip);
                    } catch (UnknownHostException except) {
//                        show_toast_message("Can't determine IP address of " + host_ip, Toast.LENGTH_LONG);
                        safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppCantDetermineIp, host_ip), Toast.LENGTH_SHORT);
                        socketOk = false;
                    }
                }

                //socket
                if (socketOk) {
                    try {
                        //look for someone to answer on specified socket, and set timeout
                        Log.d("Engine_Driver", "t_a: Opening socket, connectTimeout=" + connectTimeoutMs + " and socketTimeout=" + socketTimeoutMs);
                        clientSocket = new Socket();
                        InetSocketAddress sa = new InetSocketAddress(host_ip, port);
                        clientSocket.connect(sa, connectTimeoutMs);
                        clientSocket.setSoTimeout(socketTimeoutMs);
                    } catch (Exception except) {
                        if (!firstConnect) {
                            safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppCantConnect,
                                    host_ip, Integer.toString(port), client_address, except.getMessage()), Toast.LENGTH_LONG);
                        }
                        if ((!client_type.equals("WIFI")) && (prefAllowMobileData)) { //show additional message if using mobile data
                            safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppNotWIFI, client_type), Toast.LENGTH_LONG);
                        }
                        socketOk = false;
                    }
                }

                //rcvr
                if (socketOk) {
                    try {
                        inputBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    } catch (IOException except) {
                        safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorInputStream, except.getMessage()), Toast.LENGTH_SHORT);
                        socketOk = false;
                    }
                }

                //start the socket_WiT thread.
                if (socketOk) {
                    if (!this.isAlive()) {
                        endRead = false;
                        try {
                            this.start();
                        } catch (IllegalThreadStateException except) {
                            //ignore "already started" errors
                            safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorStartingSocket, except.getMessage()), Toast.LENGTH_SHORT);
                        }
                    }
                }

                //xmtr
                if (socketOk) {
                    try {
                        outputPW = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                        if (outputPW.checkError()) {
                            socketOk = false;
                        }
                    } catch (IOException e) {
                        safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorCreatingOutputStream, e.getMessage()), Toast.LENGTH_SHORT);
                        socketOk = false;
                    }
                }
                socketGood = socketOk;
                if (socketOk)
                    firstConnect = true;
                return socketOk;
            }

            public void disconnect(boolean shutdown) {
                disconnect(shutdown, false);
            }

            public void disconnect(boolean shutdown, boolean fastShutdown) {
                if (shutdown) {
                    endRead = true;
                    if (!fastShutdown) {
                        for (int i = 0; i < 5 && this.isAlive(); i++) {
                            try {
                                Thread.sleep(connectTimeoutMs);     //  give run() a chance to see endRead and exit
                            } catch (InterruptedException e) {
                                safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorSleepingThread, e.getMessage()), Toast.LENGTH_SHORT);
                            }
                        }
                    }
                }

                socketGood = false;

                //close socket
                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        Log.d("Engine_Driver", "t_a: Error closing the Socket: " + e.getMessage());
                    }
                }
            }

            //read the input buffer
            public void run() {
                String str;
                //continue reading until signaled to exit by endRead
                while (!endRead) {
                    if (socketGood) {        //skip read when the socket is down
                        try {
                            if ((str = inputBR.readLine()) != null) {
                                if (str.length() > 0) {
                                    heart.restartInboundInterval();
                                    clearInboundTimeout();
                                    process_response(str);
                                }
                            }
                        } catch (SocketTimeoutException e) {
                            socketGood = this.SocketCheck();
                        } catch (IOException e) {
                            if (socketGood) {
                                Log.d("Engine_Driver", "t_a: WiT rcvr error.");
                                socketGood = false;     //input buffer error so force reconnection on next send
                            }
                        }
                    }
                    if (!socketGood) {
                        SystemClock.sleep(500L);        //don't become compute bound here when the socket is down
                    }
                }
                heart.stopHeartbeat();
                Log.d("Engine_Driver", "t_a: socket_WiT exit.");
            }

            void Send(String msg) {
                boolean reconInProg = false;
                //reconnect socket if needed
                if (!socketGood || inboundTimeout) {
                    String status;
                    if (client_address == null) {
                        status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNotConnected);
                        Log.d("Engine_Driver", "t_a: WiT send reconnection attempt.");
                    } else if (inboundTimeout) {
                        status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNoResponse, host_ip, Integer.toString(port), heart.getInboundInterval());
                        Log.d("Engine_Driver", "t_a: WiT receive reconnection attempt.");
                    } else {
                        status = threaded_application.context.getResources().getString(R.string.statusThreadedAppUnableToConnect, host_ip, Integer.toString(port), client_address);
                        Log.d("Engine_Driver", "t_a: WiT send reconnection attempt.");
                    }
                    socketGood = false;

                    sendMsg(comm_msg_handler, message_type.WIT_CON_RETRY, status);

                    //perform the reconnection sequence
                    this.disconnect(false);             //clean up socket but do not shut down the receiver
                    this.connect();                     //attempt to reestablish connection
                    reconInProg = true;
                }

                //try to send the message
                if (socketGood) {
                    try {
                        outputPW.println(msg);
                        outputPW.flush();
                        heart.restartOutboundInterval();

                        // if we get here without an exception then the socket is ok
                        if (reconInProg) {
                            String status = "Connected to WiThrottle Server at " + host_ip + ":" + port;
                            sendMsg(comm_msg_handler, message_type.WIT_CON_RECONNECT, status);
                            Log.d("Engine_Driver", "t_a: WiT reconnection successful.");
                            clearInboundTimeout();
                            heart.restartInboundInterval();     //socket is good so restart inbound heartbeat timer
                        }
                    } catch (Exception e) {
                        Log.d("Engine_Driver", "t_a: WiT xmtr error.");
                        socketGood = false;             //output buffer error so force reconnection on next send
                    }
                }

                if (!socketGood) {
                    comm_msg_handler.postDelayed(heart.outboundHeartbeatTimer, 500L);   //try connection again in 0.5 second
                }
            }

            // Attempt to determine if the socket connection is still good.
            // unfortunatley isConnected returns true if the Socket was disconnected other than by calling close() 
            // so on signal loss it still returns true.  
            // Eventually we just try to send and handle the IOException if the socket was disconnected.
            boolean SocketCheck() {
                boolean status = clientSocket.isConnected() && !clientSocket.isInputShutdown() && !clientSocket.isOutputShutdown();
                if (status)
                    status = HaveNetworkConnection();   // can't trust the socket flags so try something else...
                return status;
            }

            // temporary - SocketCheck should determine whether socket connection is good however socket flags sometimes do not get updated
            // so it doesn't work.  This is better than nothing though?
            private boolean HaveNetworkConnection() {
                boolean haveConnectedWifi = false;
                boolean haveConnectedMobile = false;
                prefAllowMobileData = prefs.getBoolean("prefAllowMobileData", false);

                final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo[] netInfo = cm.getAllNetworkInfo();
                for (NetworkInfo ni : netInfo) {
                    if ("WIFI".equalsIgnoreCase(ni.getTypeName()))

                        if (!prefAllowMobileData) {
                            // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                    && (!haveForcedWiFiConnection)) {

                                Log.d("Engine_Driver", "t_a: NetworkRequest.Builder");
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
                                haveForcedWiFiConnection = true;
                            }
                        }

                    if (ni.isConnected()) {
                        haveConnectedWifi = true;
                    } else {
                        // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                        if (prefAllowMobileData) {
                            haveConnectedWifi = true;
                        }
                    }
                    if ("MOBILE".equalsIgnoreCase(ni.getTypeName()))
                        if ((ni.isConnected()) && (prefAllowMobileData)) {
                            haveConnectedMobile = true;
                        }
                }
                return haveConnectedWifi || haveConnectedMobile;
            }

            boolean SocketGood() {
                return this.socketGood;
            }

            void InboundTimeout() {
                if (++inboundTimeoutRetryCount >= MAX_INBOUND_TIMEOUT_RETRIES) {
                    Log.d("Engine_Driver", "t_a: WiT max inbound timeouts");
                    inboundTimeout = true;
                    inboundTimeoutRetryCount = 0;
                    inboundTimeoutRecovery = false;
                    // force a send to start the reconnection process
                    comm_msg_handler.postDelayed(heart.outboundHeartbeatTimer, 200L);
                } else {
                    Log.d("Engine_Driver", "t_a: WiT inbound timeout " +
                            Integer.toString(inboundTimeoutRetryCount) + " of " + MAX_INBOUND_TIMEOUT_RETRIES);
                    // heartbeat should trigger a WiT reply so force that now
                    inboundTimeoutRecovery = true;
                    comm_msg_handler.post(heart.outboundHeartbeatTimer);
                }
            }

            void clearInboundTimeout() {
                inboundTimeout = false;
                inboundTimeoutRecovery = false;
                inboundTimeoutRetryCount = 0;
            }
        }

        class heartbeat {
            //  outboundHeartbeat - send a periodic heartbeat to WiT to show that ED is alive.
            //  inboundHeartbeat - WiT doesn't send a heartbeat to ED, so send a periodic message to WiT that requires a response.
            //
            //  If the HeartbeatValueFromServer is 0 then set heartbeatOutboundInterval = DEFAULT_OUTBOUND_HEARTBEAT_INTERVAL,
            //    and set heartbeatInboundInterval = 0, to disable the inbound heartbeat checks
            //
            //  Otherwise, set heartbeatOutboundInterval to HeartbeatValueFromServer * HEARTBEAT_RESPONSE_ALLOWANCE,
            //    and set heartbeatInboundInterval to HeartbeatValueFromServer / HEARTBEAT_RESPONSE_ALLOWANCE
            //
            //  Insure both values are between MIN_OUTBOUND_HEARTBEAT_INTERVAL and MAX_OUTBOUND_HEARTBEAT_INTERVAL

            private int heartbeatIntervalSetpoint = 0;      //WiT heartbeat interval in msec
            private int heartbeatOutboundInterval = 0;      //sends outbound heartbeat message at this rate (msec)
            private int heartbeatInboundInterval = 0;       //alerts user if there was no inbound traffic for this long (msec)

            public boolean isHeartbeatSent() {
                return heartbeatSent;
            }

            public void setHeartbeatSent(boolean heartbeatSent) {
                this.heartbeatSent = heartbeatSent;
            }

            private boolean heartbeatSent = false;

            private int getInboundInterval() {
                return heartbeatInboundInterval;
            }

            /***
             * startHeartbeat(timeoutInterval in milliseconds)
             * calcs the inbound and outbound intervals and starts the beating
             *
             * @param timeoutInterval the WiT timeoutInterval in milliseconds
             */
            void startHeartbeat(int timeoutInterval) {
                //update interval timers only when the heartbeat timeout interval changed
                if (timeoutInterval != heartbeatIntervalSetpoint) {
                    heartbeatIntervalSetpoint = timeoutInterval;

                    // outbound interval (in ms)
                    int outInterval;
                    if (heartbeatIntervalSetpoint == 0) {   //wit heartbeat is disabled so use default outbound heartbeat
                        outInterval = DEFAULT_OUTBOUND_HEARTBEAT_INTERVAL;
                    } else {
                        outInterval = (int) (heartbeatIntervalSetpoint * HEARTBEAT_RESPONSE_FACTOR);
                        //keep values in a reasonable range
                        if (outInterval < MIN_OUTBOUND_HEARTBEAT_INTERVAL)
                            outInterval = MIN_OUTBOUND_HEARTBEAT_INTERVAL;
                        if (outInterval > MAX_OUTBOUND_HEARTBEAT_INTERVAL)
                            outInterval = MAX_OUTBOUND_HEARTBEAT_INTERVAL;
                    }
                    heartbeatOutboundInterval = outInterval;

                    // inbound interval
                    int inInterval = heartbeatInterval;
                    if (heartbeatIntervalSetpoint == 0) {    // wit heartbeat is disabled so disable inbound heartbeat
                        inInterval = 0;
                    } else {
                        if (inInterval < MIN_INBOUND_HEARTBEAT_INTERVAL)
                            inInterval = MIN_INBOUND_HEARTBEAT_INTERVAL;
                        if (inInterval < outInterval)
                            inInterval = (int) (outInterval / HEARTBEAT_RESPONSE_FACTOR);
                        if (inInterval > MAX_INBOUND_HEARTBEAT_INTERVAL)
                            inInterval = MAX_INBOUND_HEARTBEAT_INTERVAL;
                    }
                    heartbeatInboundInterval = inInterval;
                    //sInboundInterval = Integer.toString(inInterval);    // seconds

                    restartOutboundInterval();
                    restartInboundInterval();
                }
            }

            //restartOutboundInterval()
            //restarts the outbound interval timing - call this after sending anything to WiT that requires a response
            void restartOutboundInterval() {
                comm_msg_handler.removeCallbacks(outboundHeartbeatTimer);                   //remove any pending requests
                if (heartbeatOutboundInterval > 0) {
                    comm_msg_handler.postDelayed(outboundHeartbeatTimer, heartbeatOutboundInterval);    //restart interval
                }
            }

            //restartInboundInterval()
            //restarts the inbound interval timing - call this after receiving anything from WiT
            void restartInboundInterval() {
                comm_msg_handler.removeCallbacks(inboundHeartbeatTimer);
                if (heartbeatInboundInterval > 0) {
                    comm_msg_handler.postDelayed(inboundHeartbeatTimer, heartbeatInboundInterval);
                }
            }

            void stopHeartbeat() {
                comm_msg_handler.removeCallbacks(outboundHeartbeatTimer);           //remove any pending requests
                comm_msg_handler.removeCallbacks(inboundHeartbeatTimer);
                heartbeatIntervalSetpoint = 0;
                Log.d("Engine_Driver", "t_a: heartbeat stopped.");
            }

            //outboundHeartbeatTimer()
            //sends a periodic message to WiT
            private Runnable outboundHeartbeatTimer = new Runnable() {
                @Override
                public void run() {
                    comm_msg_handler.removeCallbacks(this);             //remove pending requests
                    if (heartbeatIntervalSetpoint != 0) {
                        boolean anySent = false;
                        for (int i = 0; i < numThrottles; i++) {
                            if (consists[i].isActive()) {
                                witRequestSpeedAndDir(i);
                                anySent = true;
                            }
                        }
                        // prior to JMRI 4.20 there were cases where WiT might not respond to
                        // speed and direction request.  If inboundTimeout handling is in progress
                        // then we always send the Throttle Name to ensure a response
                        if (!anySent || (getServerType().equals("") && socketWiT.inboundTimeoutRecovery)) {
                            sendThrottleName(false);    //send message that will get a response
                        }
                        comm_msg_handler.postDelayed(this, heartbeatOutboundInterval);   //set next beat
                    }
                }
            };

            //inboundHeartbeatTimer()
            //display an alert message when there is no inbound traffic from WiT within required interval 
            private Runnable inboundHeartbeatTimer = new Runnable() {
                @Override
                public void run() {
                    comm_msg_handler.removeCallbacks(this); //remove pending requests
                    if (heartbeatIntervalSetpoint != 0) {
                        if (socketWiT != null && socketWiT.SocketGood()) {
                            socketWiT.InboundTimeout();
                        }
                        comm_msg_handler.postDelayed(this, heartbeatInboundInterval);    //set next inbound timeout
                    }
                }
            };
        }

        class PhoneListener extends PhoneStateListener {
            private TelephonyManager telMgr;

            PhoneListener() {
                telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                this.enable();
            }

            public void disable() {
                telMgr.listen(this, PhoneStateListener.LISTEN_NONE);
            }

            public void enable() {
                telMgr.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
            }

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    if (prefs.getBoolean("stop_on_phonecall_preference",
                            getResources().getBoolean(R.bool.prefStopOnPhonecallDefaultValue))) {
                        Log.d("Engine_Driver", "t_a: Phone is OffHook, Stopping Trains");
                        for (int i = 0; i < numThrottles; i++) {
                            if (consists[i].isActive()) {
                                witSetSpeedZero(i);
                            }
                        }
                    }
                }
            }
        }

    }


    /**
     * Display OnGoing Notification that indicates EngineDriver is Running.
     * Should only be called when ED is going into the background.
     * Currently call this from each activity onPause, passing the current intent
     * to return to when reopening.
     */
    private void addNotification(Intent notificationIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "ed_channel_HIGH";// The id of the channel.
            String CHANNEL_ID_QUIET = "ed_channel_HIGH_quiet";// The id of the channel without sound.
            String channelId;
            CharSequence name = this.getString(R.string.notification_title);// The user-visible name of the channel.
            NotificationChannel mChannel;

            boolean prefFeedbackWhenGoingToBackground = prefs.getBoolean("prefFeedbackWhenGoingToBackground", getResources().getBoolean(R.bool.prefFeedbackWhenGoingToBackgroundDefaultValue));
            if (prefFeedbackWhenGoingToBackground) {
                channelId = CHANNEL_ID;
                mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            } else {
                channelId = CHANNEL_ID_QUIET;
                mChannel = new NotificationChannel(CHANNEL_ID_QUIET, name, NotificationManager.IMPORTANCE_DEFAULT);
                mChannel.setSound(null, null);
            }


            PendingIntent contentIntent = PendingIntent.getActivity(this, ED_NOTIFICATION_ID, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            Notification notification =
                    new Notification.Builder(this)
                            .setSmallIcon(R.drawable.icon_notification)
                            .setContentTitle(this.getString(R.string.notification_title))
                            .setContentText(this.getString(R.string.notification_text))
                            .setContentIntent(contentIntent)
                            .setOngoing(true)
                            .setChannelId(channelId)
                            .build();

            // Add as notification
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(mChannel);
            manager.notify(ED_NOTIFICATION_ID, notification);
        } else {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.icon)
                            .setContentTitle(this.getString(R.string.notification_title))
                            .setContentText(this.getString(R.string.notification_text))
                            .setOngoing(true);

            PendingIntent contentIntent = PendingIntent.getActivity(this, ED_NOTIFICATION_ID, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            builder.setContentIntent(contentIntent);

            // Add as notification
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(ED_NOTIFICATION_ID, builder.build());
            safeToast(threaded_application.context.getResources().getString(R.string.notification_title), Toast.LENGTH_LONG);
        }
    }

    // Remove notification
    private void removeNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(ED_NOTIFICATION_ID);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Engine_Driver", "threaded_application.onCreate()");
        try {
            appVersion = "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        androidVersion = android.os.Build.VERSION.SDK_INT;

        Log.i("Engine_Driver", "Engine Driver:" + appVersion + ", SDK:" + androidVersion);

        context = getApplicationContext();

        commThread = new comm_thread();

        flashlight = Flashlight.newInstance(threaded_application.context);

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        lifecycleHandler = new ApplicationLifecycleHandler();
        registerActivityLifecycleCallbacks(lifecycleHandler);
        registerComponentCallbacks(lifecycleHandler);

        numThrottles = Numeralise(prefs.getString("NumThrottle", getResources().getString(R.string.NumThrottleDefaulValue)));
        //numThrottles = Numeralise(Objects.requireNonNull(prefs.getString("NumThrottle", getResources().getString(R.string.NumThrottleDefaulValue))));
        throttleLayoutViewId = R.layout.throttle;

        haveForcedWiFiConnection = false;

        try {
            Map<String, ?> ddd = prefs.getAll();
            String dwr = prefs.getString("TypeThrottle", "false");
        } catch (Exception ex) {
            String dwr = ex.toString();
        }

        for (int i = 0; i < maxThrottlesCurrentScreen; i++) {
            function_states[i] = new boolean[32];
        }

        dlMetadataTask = new DownloadMetaTask();
        dlRosterTask = new DownloadRosterTask();

        //use worker thread to initialize default function labels from file so UI can continue
        new Thread(new Runnable() {
            public void run() {
                set_default_function_labels(false);
            }
        }, "DefaultFunctionLabels").start();
        CookieSyncManager.createInstance(this);     //create this here so onPause/onResume for webViews can control it

        prefShowTimeOnLogEntry = prefs.getBoolean("prefShowTimeOnLogEntry",
                getResources().getBoolean(R.bool.prefShowTimeOnLogEntryDefaultValue));
        prefFeedbackOnDisconnect = prefs.getBoolean("prefFeedbackOnDisconnect",
                getResources().getBoolean(R.bool.prefFeedbackOnDisconnectDefaultValue));

        // setup the soundPool for the in device loco sounds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool
                    .Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC,0);
        }

        sounds[SOUND_BELL_START] = soundPool.load(this, R.raw.bell_start,1);
        sounds[SOUND_BELL_LOOP] = soundPool.load(this, R.raw.bell_loop,1);
        sounds[SOUND_BELL_END] = soundPool.load(this, R.raw.bell_end,1);
        sounds[SOUND_HORN_START] = soundPool.load(this, R.raw.horn_start,1);
        sounds[SOUND_HORN_LOOP] = soundPool.load(this, R.raw.horn_loop,1);
        sounds[SOUND_HORN_END] = soundPool.load(this, R.raw.horn_end,1);
        sounds[SOUND_WHISTLE_START] = soundPool.load(this, R.raw.whistle_start,1);
        sounds[SOUND_WHISTLE_LOOP] = soundPool.load(this, R.raw.whistle_loop,1);
        sounds[SOUND_WHISTLE_END] = soundPool.load(this, R.raw.whistle_end,1);

        soundsSteam[0] = soundPool.load(this, R.raw.steam_loco_stationary_med,1);
        soundsSteam[1] = soundPool.load(this, R.raw.steam_piston_stroke3,1);
        soundsSteam[2] = soundPool.load(this, R.raw.steam_loop_30rpm,1);
        soundsSteam[3] = soundPool.load(this, R.raw.steam_loop_35rpm,1);
        soundsSteam[4] = soundPool.load(this, R.raw.steam_loop_40rpm,1);
        soundsSteam[5] = soundPool.load(this, R.raw.steam_loop_50rpm,1);
        soundsSteam[6] = soundPool.load(this, R.raw.steam_loop_60rpm,1);
        soundsSteam[7] = soundPool.load(this, R.raw.steam_loop_75rpm,1);
        soundsSteam[8] = soundPool.load(this, R.raw.steam_loop_90rpm,1);
        soundsSteam[9] = soundPool.load(this, R.raw.steam_loop_100rpm,1);
        soundsSteam[10] = soundPool.load(this, R.raw.steam_loop_125rpm,1);
        soundsSteam[11] = soundPool.load(this, R.raw.steam_loop_150rpm,1);
        soundsSteam[12] = soundPool.load(this, R.raw.steam_loop_175rpm,1);
        soundsSteam[13] = soundPool.load(this, R.raw.steam_loop_200rpm,1);
        soundsSteam[14] = soundPool.load(this, R.raw.steam_loop_250rpm,1);
        soundsSteam[15] = soundPool.load(this, R.raw.steam_loop_300rpm,1);

        soundsDiesel645turbo[0] = soundPool.load(this, R.raw.diesel_645turbo_idle,1);
        soundsDiesel645turbo[1] = soundPool.load(this, R.raw.diesel_645turbo_d1_d2,1);
        soundsDiesel645turbo[2] = soundPool.load(this, R.raw.diesel_645turbo_d2_d3,1);
        soundsDiesel645turbo[3] = soundPool.load(this, R.raw.diesel_645turbo_d3_d4,1);
        soundsDiesel645turbo[4] = soundPool.load(this, R.raw.diesel_645turbo_d4,1);

    } // end onCreate

    public class ApplicationLifecycleHandler implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
        private boolean isInBackground = true;
        private Activity runningActivity = null;

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (isInBackground) {                           // if coming out of background
                isInBackground = false;
                exitConfirmed = false;
                removeNotification();
            }
            runningActivity = activity;                 // save most recently resumed activity
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (isInBackground && activity == runningActivity) {
                removeNotification();           // destroyed in background so remove notification
            }
        }

        @Override
        public void onConfigurationChanged(Configuration configuration) {
        }

        @Override
        public void onLowMemory() {
        }

        @Override
        public void onTrimMemory(int level) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {   // if in background
                if (!isInBackground) {                              // if just went into bkg
                    isInBackground = true;
                    if (!exitConfirmed) {                       // if user did not just confirm exit
                        addNotification(runningActivity.getIntent());
                    } else {                                    // user confirmed exit
                    }
                }
                if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) { // time to kill app
                    if (!exitConfirmed) {       // if TA is running in bkg
                        // disconnect and shutdown
                        sendMsg(comm_msg_handler, message_type.DISCONNECT, "", 1);
                        exitConfirmed = true;
                    }
                }
            }
        }
    }

    public boolean isForcingFinish() {
        return doFinish;
    }

    public void cancelForcingFinish() {
        doFinish = false;
    }


    //init default function labels from the settings files or set to default
    //also collects the loco and latching handling for the 'Special' consist function string matching
    public void set_default_function_labels(boolean getAll) {
        String locosDefault = getResources().getString(R.string.prefFunctionConsistLocosDefaultValue);
        ;
        String latchingDefault = getResources().getString(R.string.prefFunctionConsistLatchingDefaultValue);
        ;
        String latchingLightBellDefault = getResources().getString(R.string.prefFunctionConsistLatchingLightBellDefaultValue);
        ;

        int numberOfDefaultFunctionLabels = 29;
        int numberOfDefaultFunctionLabelsForRoster = 29;
        if (!getAll) {
            try {
                numberOfDefaultFunctionLabels = Integer.parseInt(prefs.getString("prefNumberOfDefaultFunctionLabels", getResources().getString(R.string.prefNumberOfDefaultFunctionLabelsDefaultValue))) - 1;
                numberOfDefaultFunctionLabelsForRoster = Integer.parseInt(prefs.getString("prefNumberOfDefaultFunctionLabelsForRoster", getResources().getString(R.string.prefNumberOfDefaultFunctionLabelsForRosterDefaultValue))) - 1;
            } catch (NumberFormatException ignored) {
            }
        }

        function_labels_default = new LinkedHashMap<>();
        function_labels_default_for_roster = new LinkedHashMap<>();
        function_consist_locos = new LinkedHashMap<>();
        function_consist_latching = new LinkedHashMap<>();
        try {
            File sdcard_path = Environment.getExternalStorageDirectory();
            File settings_file = new File(sdcard_path + "/engine_driver/function_settings.txt");
            if (settings_file.exists()) {  //if file found, use it for settings arrays
                BufferedReader settings_reader = new BufferedReader(new FileReader(settings_file));
                //read settings into local arrays
                int i = 0;
                while (settings_reader.ready()) {
                    String line = settings_reader.readLine();
                    String temp[] = line.split(":");
                    if (temp.length >= 2) {
                        if (i <= numberOfDefaultFunctionLabels) {
                            function_labels_default.put(Integer.parseInt(temp[1]), temp[0]); //put funcs and labels into global default
                        }
                        if (i <= numberOfDefaultFunctionLabelsForRoster) {
                            function_labels_default_for_roster.put(Integer.parseInt(temp[1]), temp[0]); //put funcs and labels into global default
                        }
                    }
                    if (temp.length == 4) {
                        if (i <= numberOfDefaultFunctionLabels) {
                            function_consist_locos.put(Integer.parseInt(temp[1]), temp[2]);
                            function_consist_latching.put(Integer.parseInt(temp[1]), temp[3]);
                        } else {
                            function_consist_locos.put(Integer.parseInt(temp[1]), locosDefault);
                            if (i < 2) {
                                function_consist_latching.put(Integer.parseInt(temp[1]), latchingLightBellDefault);
                            } else {
                                function_consist_latching.put(Integer.parseInt(temp[1]), latchingDefault);
                            }
                        }
                    }
                    i++;
                }
                settings_reader.close();
            } else {          //hard-code some buttons and default the rest
                if (numberOfDefaultFunctionLabels >= 0) {
                    function_labels_default.put(0, threaded_application.context.getResources().getString(R.string.functionButton00DefaultValue));
                    function_consist_latching.put(0, latchingLightBellDefault);
                }
                if (numberOfDefaultFunctionLabels >= 1) {
                    function_labels_default.put(1, threaded_application.context.getResources().getString(R.string.functionButton01DefaultValue));
                    function_consist_latching.put(1, latchingLightBellDefault);
                }
                if (numberOfDefaultFunctionLabels >= 2) {
                    function_labels_default.put(2, threaded_application.context.getResources().getString(R.string.functionButton02DefaultValue));
                    function_consist_latching.put(2, latchingDefault);
                }
                if (numberOfDefaultFunctionLabels >= 3) {
                    for (int k = 3; k <= numberOfDefaultFunctionLabels; k++) {
                        function_labels_default.put(k, Integer.toString(k));        //String.format("%d",k));
                        function_consist_latching.put(k, latchingDefault);
                    }
                }
            }
        } catch (IOException except) {
            Log.e("settings_activity", "Could not read file, error: " + except.getMessage());
        } catch (NumberFormatException except) {
            Log.e("settings_activity", "NumberFormatException reading function_settings file: " + except.getMessage());
        }

    }

    public class DownloadRosterTask extends DownloadDataTask {
        @SuppressWarnings("unchecked")
        @Override
        void runMethod(Download dl) throws IOException {
            String rosterUrl = createUrl("roster/?format=xml");
            HashMap<String, RosterEntry> rosterTemp;
            if (rosterUrl == null || rosterUrl.equals("") || dl.cancel)
                return;
            Log.d("Engine_Driver", "t_a: Background loading roster from " + rosterUrl);
            int rosterSize;
            try {
                RosterLoader rl = new RosterLoader(rosterUrl);
                if (dl.cancel)
                    return;
                rosterTemp = rl.parse();
                rosterSize = rosterTemp.size();     //throws exception if still null
                if (!dl.cancel)
                    roster = (HashMap<String, RosterEntry>) rosterTemp.clone();
            } catch (Exception e) {
                throw new IOException();
            }
            Log.d("Engine_Driver", "t_a: Loaded " + rosterSize + " entries from /roster/?format=xml.");
        }
    }

    public class DownloadMetaTask extends DownloadDataTask {
        @SuppressWarnings("unchecked")
        @Override
        void runMethod(Download dl) throws IOException {
            String metaUrl = createUrl("json/metadata");
            if (metaUrl == null || metaUrl.equals("") || dl.cancel)
                return;
            Log.d("Engine_Driver", "t_a: Background loading metadata from " + metaUrl);

            HttpClient Client = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(metaUrl);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String jsonResponse;
            jsonResponse = Client.execute(httpget, responseHandler);
            Log.d("Engine_Driver", "t_a: Raw metadata retrieved: " + jsonResponse);

            HashMap<String, String> metadataTemp = new HashMap<>();
            try {
                JSONArray ja = new JSONArray(jsonResponse);
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject j = ja.optJSONObject(i);
                    String metadataName = j.getJSONObject("data").getString("name");
                    String metadataValue = j.getJSONObject("data").getString("value");
                    metadataTemp.put(metadataName, metadataValue);
                }
            } catch (JSONException e) {
                Log.d("Engine_Driver", "t_a: exception trying to retrieve json metadata.");
            } catch (Exception e) {
                throw new IOException();
            }
            if (metadataTemp.size() == 0) {
                Log.d("Engine_Driver", "t_a: did not retrieve any json metadata entries.");
            } else {
                jmriMetadata = (HashMap<String, String>) metadataTemp.clone();  // save the metadata in global variable
                Log.d("Engine_Driver", "t_a: Loaded " + jmriMetadata.size() + " metadata entries from json web server.");
            }
        }
    }

    abstract class DownloadDataTask {
        private Download dl = null;

        abstract void runMethod(Download dl) throws IOException;

        public class Download extends Thread {
            public volatile boolean cancel = false;

            @Override
            public void run() {
                try {
                    runMethod(this);
                    if (!cancel) {
                        Log.d("Engine_Driver", "t_a: sendMsg - message - ROSTER_UPDATE");
                        sendMsg(comm_msg_handler, message_type.ROSTER_UPDATE);      //send message to alert other activities
                    }
                } catch (Throwable t) {
                    Log.d("Engine_Driver", "t_a: Data fetch failed: " + t.getMessage());
                }

                // background load of Data completed
                finally {
                    if (cancel) {
                        Log.d("Engine_Driver", "t_a: Data fetch cancelled");
                    }
                }
                Log.d("Engine_Driver", "t_a: Data fetch ended");
            }

            Download() {
                super("DownloadData");
            }
        }

        void get() {
            if (dl != null) {
                dl.cancel = true;   // try to stop any update that is in progress on old download thread
            }
            dl = new Download();    // create new download thread
            dl.start();             // start an update
        }

        void stop() {
            if (dl != null) {
                dl.cancel = true;
            }
        }
    }

    // get the roster name from address string 123(L).  Return input if not found in roster or in consist
    public String getRosterNameFromAddress(String addr_str, boolean returnBlankIfNotFound) {
        boolean prefRosterRecentLocoNames = prefs.getBoolean("prefRosterRecentLocoNames",
                getResources().getBoolean(R.bool.prefRosterRecentLocoNamesDefaultValue));

        if (prefRosterRecentLocoNames) {
            if ((roster_entries != null) && (roster_entries.size() > 0)) {
                for (String rosterName : roster_entries.keySet()) {  // loop thru roster entries,
                    if (roster_entries.get(rosterName).equals(addr_str)) { //looking for value = input parm
                        return rosterName;  //if found, return the roster name (key)
                    }
                }
            }
            if (getConsistNameFromAddress(addr_str) != null) { //check for a JMRI consist for this address
                return getConsistNameFromAddress(addr_str);
            }
        }
        if (returnBlankIfNotFound) return "";

        return addr_str; //return input if not found
    }

    public String findLocoNameInRoster(String searchName) {
        if ((roster_entries != null) && (roster_entries.size() > 0)) {
            String rslt = roster_entries.get(searchName);
            if (rslt != null) {
                if (rslt.length() > 0) {
                    return searchName;
                }
            }
        }
        return ""; //return blank if not found
    }

    public String getConsistNameFromAddress(String addr_str) {
        if (addr_str.charAt(0) == 'S' || addr_str.charAt(0) == 'L') { //convert from S123 to 123(S) formatting if needed
            addr_str = cvtToAddrP(addr_str);
        }
        if ((consist_entries != null) && (consist_entries.size() > 0)) {
            return consist_entries.get(addr_str);  //consists are keyed by address "123(L)"
        }
        return null;
    }

    //convert a string of form L123 to 123(L)
    public String cvtToAddrP(String addr_str) {
        return addr_str.substring(1) + "(" + addr_str.substring(0, 1) + ")";
    }

    //convert a string of form 123(L) to L123
    public String cvtToLAddr(String addr_str) {
        String[] sa = splitByString(addr_str, "(");  //split on the "("
        if (sa.length == 2) {
            return sa[1].substring(0, 1) + sa[0]; //move length to front and return format L123
        } else {
            return addr_str; //just return input if format not as expected
        }
    }

    public String getServerType() {
        return this.serverType;
    }

    /* handle server-specific settings here */
    public void setServerType(String serverType) {
        this.serverType = serverType;
        if (serverType.equals("MRC")) {
            web_server_port = 80; //hardcode web port for MRC
        } else if (serverType.equals("Digitrax")) {
            WiThrottle_Msg_Interval = 200; //increase the interval for LnWi
        }
    }

    public String getServerDescription() {
        return this.serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }

    //reinitialize statics in activities as required to be ready for next launch
    private static void reinitStatics() {
        throttle.initStatics();
        throttle_full.initStatics();
        throttle_simple.initStatics();
        web_activity.initStatics();
    }

    //initialize shared variables
    private void initShared() {
        withrottle_version = 0.0;
        web_server_port = 0;
        host_ip = null;
        setServerType("");
        setServerDescription("");
        jmriMetadata = null;
        power_state = null;
        to_states = null;
        to_system_names = null;
        to_user_names = null;
        to_state_names = null;
        rt_states = null;
        rt_system_names = null;
        rt_user_names = null;
        rt_state_names = null;

        try {
            consists = new Consist[maxThrottles];
            function_labels = (LinkedHashMap<Integer, String>[]) new LinkedHashMap<?, ?>[maxThrottles];
            function_states = new boolean[maxThrottles][32];

            for (int i = 0; i < maxThrottles; i++) {
                consists[i] = new Consist();
                function_labels[i] = new LinkedHashMap<Integer, String>();
                function_states[i] = new boolean[32];        // also allocated in onCreate() ???
            }

            consist_entries = Collections.synchronizedMap(new LinkedHashMap<String, String>());
            roster_entries = Collections.synchronizedMap(new LinkedHashMap<String, String>());
        } catch (Exception e) {
            Log.d("Engine_Driver", "initShared object create exception");
        }
        doFinish = false;
        turnouts_list_position = 0;
        routes_list_position = 0;
    }

    //
    // utilities
    //

    /**
     * ------ copied from jmri util code -------------------
     * Split a string into an array of Strings, at a particular
     * divider.  This is similar to the new String.split method,
     * except that this does not provide regular expression
     * handling; the divider string is just a string.
     *
     * @param input   String to split
     * @param divider Where to divide the input; this does not appear in output
     */
    static public String[] splitByString(String input, String divider) {

        //bail on empty input string, return input as single element
        if (input == null || input.length() == 0) return new String[]{input};

        int size = 0;
        String temp = input;

        // count entries
        while (temp.length() > 0) {
            size++;
            int index = temp.indexOf(divider);
            if (index < 0) break;    // break not found
            temp = temp.substring(index + divider.length());
            if (temp.length() == 0) {  // found at end
                size++;
                break;
            }
        }

        String[] result = new String[size];

        // find entries
        temp = input;
        size = 0;
        while (temp.length() > 0) {
            int index = temp.indexOf(divider);
            if (index < 0) break;    // done with all but last
            result[size] = temp.substring(0, index);
            temp = temp.substring(index + divider.length());
            size++;
        }
        result[size] = temp;

        return result;
    }

    public void powerStateMenuButton() {
        int newState = 1;
        if ("1".equals(power_state)) {          //toggle to opposite value 0=off, 1=on
            newState = 0;
        }
        sendMsg(comm_msg_handler, message_type.POWER_CONTROL, "", newState);
    }

    public void displayPowerStateMenuButton(Menu menu) {
        if (prefs.getBoolean("show_layout_power_button_preference", false) && (power_state != null)) {
            actionBarIconCountThrottle++;
            actionBarIconCountRoutes++;
            actionBarIconCountTurnouts++;
            menu.findItem(R.id.power_layout_button).setVisible(true);
        } else {
            menu.findItem(R.id.power_layout_button).setVisible(false);
        }
    }

    public void powerControlNotAllowedDialog(Menu pMenu) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(getApplicationContext().getResources().getString(R.string.powerWillNotWorkTitle));
        b.setMessage(getApplicationContext().getResources().getString(R.string.powerWillNotWork));
        b.setCancelable(true);
        b.setNegativeButton("OK", null);
        AlertDialog alert = b.create();
        alert.show();
        displayPowerStateMenuButton(pMenu);
    }

    public void displayThrottleMenuButton(Menu menu, String swipePreferenceToCheck) {
        if (prefs.getBoolean(swipePreferenceToCheck, false)) {
            menu.findItem(R.id.throttle_button_mnu).setVisible(false);
        } else {
            menu.findItem(R.id.throttle_button_mnu).setVisible(true);
        }
    }

    /**
     * for menu passed in, set the text or hide the menu option based on connected system
     *
     * @param menu - menu object that will be adjusted
     */
    public void setWebMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.web_mnu);
            if (item != null) {
                if (isWebAllowed()) {
                    item.setVisible(true);
                    if ("MRC".equals(getServerType())) {
                        //for MRC, the web view is only for settings, so also change the text
                        item.setTitle(R.string.mrc_settings);
                    }
                } else {
                    item.setVisible(false);
                }
            }
        }
    }

    /**
     * for menu passed in, hide or show the gamepad test menu
     *
     * @param menu - menu object that will be adjusted
     */
    public void setGamepadTestMenuOption(Menu menu, int gamepadCount) {
        String whichGamePadMode = prefs.getString("prefGamePadType", threaded_application.context.getResources().getString(R.string.prefGamePadTypeDefaultValue));
        boolean result;

        if (menu != null) {
            boolean any = false;
            for (int i = 1; i <= 3; i++) {
                MenuItem item = menu.findItem(R.id.gamepad_test_mnu1);
                switch (i) {
                    case 2:
                        item = menu.findItem(R.id.gamepad_test_mnu2);
                        break;
                    case 3:
                        item = menu.findItem(R.id.gamepad_test_mnu3);
                }

                result = i <= gamepadCount;

                if (item != null) {
                    if ((!whichGamePadMode.equals("None")) && (result)) {
                        any = true;
                        item.setVisible(true);
                    } else {
                        item.setVisible(false);
                    }
                }
            }
            if (any) {
                menu.findItem(R.id.gamepad_test_menu).setVisible(any);
            }

        }
    }

    /**
     * for menu passed in, hide or show the routes menu
     *
     * @param menu - menu object that will be adjusted
     */
    public void setRoutesMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.routes_mnu);
            if (item != null) {
                if (isRouteControlAllowed()) {
                    item.setVisible(true);
                } else {
                    item.setVisible(false);
                }
            }
        }
    }

    /**
     * for menu passed in, hide or show the power menu option
     *
     * @param menu - menu object that will be adjusted
     */
    public void setPowerMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.power_control_mnu);
            if (item != null) {
                if (isPowerControlAllowed()) {
                    item.setVisible(true);
                } else {
                    item.setVisible(false);
                }
            }
        }
    }

    /**
     * for menu passed in, hide or show the routes menu
     *
     * @param menu - menu object that will be adjusted
     */
    public void setTurnoutsMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.turnouts_mnu);
            if (item != null) {
                if (isTurnoutControlAllowed()) {
                    item.setVisible(true);
                } else {
                    item.setVisible(false);
                }
            }
        }
    }

    public void setMenuItemById(Menu menu, int id, boolean show) {
        if (menu != null) {
            MenuItem item = menu.findItem(id);
            if (item != null) {
                item.setVisible(show);
            }
        }
    }

    public void setKidsMenuOptions(Menu menu, boolean show, int gamepadCount) {
        if (!show) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                //if ((item.getItemId() == R.id.preferences_mnu) || (item.getItemId() == R.id.timer_mnu)) {
                if (item.getItemId() == R.id.timer_mnu) {
                    item.setVisible(true);
                } else {
                    item.setVisible(false);
                }
            }
        } else {
            menu.findItem(R.id.settings_mnu).setVisible(true);
            setPowerMenuOption(menu);
            setWebMenuOption(menu);
            setRoutesMenuOption(menu);
            setTurnoutsMenuOption(menu);
            setGamepadTestMenuOption(menu, gamepadCount);
            setMenuItemById(menu, R.id.logviewer_menu, true);
            setMenuItemById(menu, R.id.exit_mnu, true);
            setMenuItemById(menu, R.id.timer_mnu, false);
        }
    }

    public void forceFunction(String throttleAndAddr, int functionNumber, boolean state) {
        int onOff = 0;
        if (state) onOff = 1;
        if ((functionNumber >= 0) && (functionNumber <= MAX_FUNCTION_NUMBER)) {
            sendMsg(comm_msg_handler, message_type.FORCE_FUNCTION, throttleAndAddr, functionNumber, onOff);
        } // otherwise just ignore the request
    }

    public void toggleFunction(String throttleAndAddr, int functionNumber) {
        if ((functionNumber >= 0) && (functionNumber <= MAX_FUNCTION_NUMBER)) {
            sendMsg(comm_msg_handler, message_type.FUNCTION, throttleAndAddr, functionNumber, 1);
        } // otherwise just ignore the request
    }

    /**
     * Is Web View allowed for this connection?
     * this hides/shows menu options and activities
     *
     * @return true or false
     */
    public boolean isWebAllowed() {
        return (web_server_port > 0);
    }

    /**
     * Is Power Control allowed for this connection?
     * this hides/shows menu options and activities
     *
     * @return true or false
     */
    public boolean isPowerControlAllowed() {
        return (power_state != null);
    }

    /**
     * Is Route Control allowed for this connection?
     * based on setting of rt_state_names for now
     * this hides/shows menu options and activities
     *
     * @return true or false
     */
    public boolean isRouteControlAllowed() {
        return (rt_state_names != null);
    }

    /**
     * Are turnouts allowed for this connection?
     * based on setting of to_state_names for now
     * this hides/shows menu options and activities
     *
     * @return true or false
     */
    public boolean isTurnoutControlAllowed() {
        return (to_state_names != null);
    }

    public void setPowerStateButton(Menu menu) {
        if (menu != null) {
            TypedValue outValue = new TypedValue();
            if ((power_state == null) || (power_state.equals("2"))) {
                theme.resolveAttribute(R.attr.ed_power_yellow_button, outValue, true);
                menu.findItem(R.id.power_layout_button).setIcon(outValue.resourceId);
                menu.findItem(R.id.power_layout_button).setTitle("Layout Power is UnKnown");
            } else if (power_state.equals("1")) {
                theme.resolveAttribute(R.attr.ed_power_green_button, outValue, true);
                menu.findItem(R.id.power_layout_button).setIcon(outValue.resourceId);
                menu.findItem(R.id.power_layout_button).setTitle("Layout Power is ON");
            } else {
                theme.resolveAttribute(R.attr.ed_power_red_button, outValue, true);
                menu.findItem(R.id.power_layout_button).setIcon(outValue.resourceId);
                menu.findItem(R.id.power_layout_button).setTitle("Layout Power is Off");
            }
        }
    }

    public void displayEStop(Menu menu) {
        MenuItem mi = menu.findItem(R.id.EmerStop);
        if (mi == null) return;

        if (prefs.getBoolean("show_emergency_stop_menu_preference", false)) {
            TypedValue outValue = new TypedValue();
            theme.resolveAttribute(R.attr.ed_estop_button, outValue, true);
            mi.setIcon(outValue.resourceId);
            actionBarIconCountThrottle++;
            actionBarIconCountRoutes++;
            actionBarIconCountTurnouts++;
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }

    }

    public void sendEStopMsg() {
        for (int i = 0; i < maxThrottlesCurrentScreen; i++) {
            if (consists != null && consists[i] != null && consists[i].isActive()) {
                sendMsg(comm_msg_handler, message_type.ESTOP, "", i);
                EStopActivated = true;
                Log.d("Engine_Driver", "t_a: EStop sent to server for " + i);
            }
        }
    }

    // forward a message to all running activities 
    void alert_activities(int msgType, String msgBody) {
        try {
            sendMsg(connection_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(turnouts_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(routes_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(consist_edit_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(consist_lights_edit_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(throttle_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(web_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(power_control_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(reconnect_status_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(select_loco_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(settings_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(logviewer_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(about_page_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(function_settings_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(function_consist_settings_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(gamepad_test_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
    }

    public boolean sendMsg(Handler h, int msgType) {
        return sendMsgDelay(h, 0, msgType, "", 0, 0);
    }

    public boolean sendMsg(Handler h, int msgType, String msgBody) {
        return sendMsgDelay(h, 0, msgType, msgBody, 0, 0);
    }

    public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1) {
        return sendMsgDelay(h, 0, msgType, msgBody, msgArg1, 0);
    }

    public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1, int msgArg2) {
        return sendMsgDelay(h, 0, msgType, msgBody, msgArg1, msgArg2);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType, int msgArg1) {
        return sendMsgDelay(h, delayMs, msgType, "", msgArg1, 0);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType) {
        return sendMsgDelay(h, delayMs, msgType, "", 0, 0);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType, String msgBody) {
        return sendMsgDelay(h, delayMs, msgType, msgBody, 0, 0);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType, String msgBody, int msgArg1, int msgArg2) {
        boolean sent = false;
        if (h != null) {
            Message msg = Message.obtain();
            msg.what = msgType;
            msg.obj = msgBody;
            msg.arg1 = msgArg1;
            msg.arg2 = msgArg2;
            try {                           // handler access is not locked and might have been removed by activity
                sent = h.sendMessageDelayed(msg, delayMs);
            } catch (Exception e) {
                try {                       // exception could be that handler is gone so use another try/catch here
                    h.removeCallbacksAndMessages(null);
                } catch (Exception ignored) {
                }
            }
            if (!sent)
                msg.recycle();
        }
        return sent;
    }

    //
    // methods for use by Activities
    //

    // build a full url
    // returns: full url    if web_server_port is valid
    //          empty string   otherwise
    public String createUrl(String defaultUrl) {
        String url = "";
        int port = web_server_port;
        if (getServerType().equals("MRC")) {  //special case ignore any url passed-in if connected to MRC, as it does not forward
            defaultUrl = "";
            Log.d("Engine_Driver", "t_a: ignoring web url for MRC");
        }
        if (port > 0) {
            if (defaultUrl.startsWith("http")) { //if url starts with http, use it as is
                url = defaultUrl;
            } else { //, else prepend servername and port and slash if needed           
                url = "http://" + host_ip + ":" + port + (defaultUrl.startsWith("/") ? "" : "/") + defaultUrl;
            }
        }
        return url;
    }

    // build a full uri
    // returns: full uri    if webServerPort is valid
    //          null        otherwise
    public String createUri() {
        String uri = "";
        int port = web_server_port;
        if (port > 0) {
            uri = "ws://" + host_ip + ":" + port + "/json/";
        }
        return uri;
    }


    public int getSelectedTheme() {
        return getSelectedTheme(false);
    }
    public int getSelectedTheme(boolean isPreferences) {
        String prefTheme = getCurrentTheme();
        if (!isPreferences) {  // not a preferences activity
            switch (prefTheme) {
                case "Black":
                    return R.style.app_theme_black;
                case "Outline":
                    return R.style.app_theme_outline;
                case "Ultra":
                    return R.style.app_theme_ultra;
                case "Colorful":
                    return R.style.app_theme_colorful;
                default:
                    return R.style.app_theme;
            }
        } else {
            switch (prefTheme) {
                case "Colorful":
//                    return R.style.app_theme_colorful_preferences;
                case "Black":
                case "Outline":
                case "Ultra":
                    return R.style.app_theme_black_preferences;
                default:
                    return R.style.app_theme_preferences;
            }
        }
    }

    /**
     * Applies the chosen theme from preferences to the specified activity
     *
     * @param activity the activity to set the theme for
     */
    public void applyTheme(Activity activity) {
        applyTheme(activity, false);
    }
    public void applyTheme(Activity activity, boolean isPreferences) {
        int selectedTheme = getSelectedTheme(isPreferences);
        activity.setTheme(selectedTheme);
        Log.d("Engine_Driver", "applyTheme: " + selectedTheme);
        theme = activity.getTheme();

    }


    /**
     * Retrieve the currently configure theme from preferences
     *
     * @return a String representation of the selected theme
     */
    public String getCurrentTheme() {
        return prefs.getString("prefTheme", threaded_application.context.getResources().getString(R.string.prefThemeDefaultValue));
    }

    /**
     * Return fastClockSeconds as formatted time string
     *
     * @return a String representation of the time
     */
    public String getFastClockTime() {
        String f = "";
        if (fastClockFormat == 2) {
            f = "HH:mm"; // display in 24 hr format
        } else if (fastClockFormat == 1) {
            f = "h:mm a"; // display in 12 hr format
        }
        SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = new java.util.Date((fastClockSeconds * 1000L));
        return sdf.format(date);
    }

    /**
     * Set activity screen orientation based on prefs, check to avoid sending change when already there.
     * checks "auto Web on landscape" preference and returns false if orientation requires activity switch
     * Uses web orientation pref if called from web_activity, uses throttle orientation pref otherwise
     *
     * @param activity calling activity
     * @return true if the new orientation is ok for this activity.
     * false if "Auto Web on Landscape" is enabled and new orientation requires activity switch
     */
    @SuppressLint("SourceLockedOrientationActivity")
    public boolean setActivityOrientation(Activity activity) {
        boolean isWeb = (activity.getLocalClassName().equals("web_activity"));
        String to = prefs.getString("ThrottleOrientation",
                threaded_application.context.getResources().getString(R.string.prefThrottleOrientationDefaultValue));
        if ((to.equals("Auto-Web")) && (!webMenuSelected)) {
            int orient = activity.getResources().getConfiguration().orientation;
            if ((isWeb && orient == ORIENTATION_PORTRAIT)
                    || (!isWeb && orient == Configuration.ORIENTATION_LANDSCAPE))
                return (false);
        } else if (isWeb) {
            to = prefs.getString("WebOrientation",
                    threaded_application.context.getResources().getString(R.string.prefWebOrientationDefaultValue));
        }

        try {
            int co = activity.getRequestedOrientation();
            if (to.equals("Landscape") && (co != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE))
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else if ((to.equals("Auto-Rotate")) || (to.equals("Auto-Web")) && (co != ActivityInfo.SCREEN_ORIENTATION_SENSOR))
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            else if (to.equals("Portrait") && (co != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (Exception e) {
            Log.e("Engine_Driver", "setActivityOrientation: Unable to change Orientation: " + e.getMessage());
        }
        return true;
    }

    // prompt for Exit
    // must be called on the UI thread
    public void checkExit(final Activity activity) {
        final AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(R.string.exit_title);
        b.setMessage(R.string.exit_text);
        b.setCancelable(true);
        b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                exitConfirmed = true;
                sendMsg(comm_msg_handler, message_type.DISCONNECT, "");  //trigger disconnect / shutdown sequence
            }
        });
        b.setNegativeButton(R.string.no, null);
        AlertDialog alert = b.create();
        alert.show();

        // find positiveButton and negativeButton
        Button positiveButton = alert.findViewById(android.R.id.button1);
        Button negativeButton = alert.findViewById(android.R.id.button2);
        // then get their parent ViewGroup
        ViewGroup buttonPanelContainer = (ViewGroup) positiveButton.getParent();
        int positiveButtonIndex = buttonPanelContainer.indexOfChild(positiveButton);
        int negativeButtonIndex = buttonPanelContainer.indexOfChild(negativeButton);
        if (positiveButtonIndex < negativeButtonIndex) {  // force 'No' 'Yes' order
            // prepare exchange their index in ViewGroup
            buttonPanelContainer.removeView(positiveButton);
            buttonPanelContainer.removeView(negativeButton);
            buttonPanelContainer.addView(negativeButton, positiveButtonIndex);
            buttonPanelContainer.addView(positiveButton, negativeButtonIndex);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        languageCountry = Locale.getDefault().getLanguage();
        if (!Locale.getDefault().getCountry().equals("")) {
            languageCountry = languageCountry + "_" + Locale.getDefault().getCountry();
        }
        super.attachBaseContext(LocaleHelper.onAttach(base, languageCountry));
    }

    /**
     * Set the state of the flashlight action button/menu entry
     *
     * @param menu the menu upon which the action is shown
     */
    public void setFlashlightButton(Menu menu) {
        if (menu != null) {
            TypedValue outValue = new TypedValue();
            if (flashState) {
                theme.resolveAttribute(R.attr.ed_flashlight_on_button, outValue, true);
                menu.findItem(R.id.flashlight_button).setIcon(outValue.resourceId);
                menu.findItem(R.id.flashlight_button).setTitle(R.string.flashlightStateOn);
            } else {
                theme.resolveAttribute(R.attr.ed_flashlight_off_button, outValue, true);
                menu.findItem(R.id.flashlight_button).setIcon(outValue.resourceId);
                menu.findItem(R.id.flashlight_button).setTitle(R.string.flashlightStateOff);
            }
        }
    }

    /**
     * Display the flashlight action if configured
     *
     * @param menu the menu upon which the action should be shown
     */
    public void displayFlashlightMenuButton(Menu menu) {
        MenuItem mi = menu.findItem(R.id.flashlight_button);
        if (mi == null) return;

        if (prefs.getBoolean("prefFlashlightButtonDisplay", false)) {
            actionBarIconCountThrottle++;
            actionBarIconCountRoutes++;
            actionBarIconCountTurnouts++;
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }

    }

    public void displayTimerMenuButton(Menu menu, int kidsTimerRunning) {
        MenuItem mi = menu.findItem(R.id.timer_button);
        if (mi == null) return;

        if ((prefs.getBoolean("prefKidsTimerButton", false))
            && !((kidsTimerRunning == KIDS_TIMER_RUNNNING) || (kidsTimerRunning == KIDS_TIMER_ENABLED)) ) {
            actionBarIconCountThrottle++;
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }
    }

    public void displayWebViewMenuButton(Menu menu) {
        MenuItem mi = menu.findItem(R.id.web_view_button);
        if (mi == null) return;

        String defaultWebViewLocation = getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue);
        String webViewLocation = prefs.getString("WebViewLocation", defaultWebViewLocation);

        if ( (prefs.getBoolean("prefWebViewButton", false))
                && (!webViewLocation.equals(defaultWebViewLocation))){
            actionBarIconCountThrottle++;
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }

    }

/*    public void displayMenuSeparator(Menu menu, Activity activity, int actionBarIconCount) {
        MenuItem mi = menu.findItem(R.id.separator);
        if (mi == null) return;

        if ((activity.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT)
                && (actionBarIconCount > 2)) {
            mi.setVisible(true);
        } else if ((activity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
                && (actionBarIconCount > 3)) {
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }
    }*/

    public void displayThrottleSwitchMenuButton(Menu menu) {
        MenuItem mi = menu.findItem(R.id.throttle_switch_button);
        if (mi == null) return;

        if (prefs.getBoolean("prefThrottleSwitchButtonDisplay", false)) {
            actionBarIconCountThrottle++;
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }
    }

    /**
     * Checks to see if this device has a flashlight
     *
     * @return true if a flashlight is available; false if not
     */
    public boolean isFlashlightAvailable() {
        return flashlight.isFlashlightAvailable();
    }

    /**
     * Toggle the flashlight (where supported)
     *
     * @param activity the requesting activity
     * @param menu     the menu upon which the entry/button should be updated
     */
    public void toggleFlashlight(Activity activity, Menu menu) {
        if (flashState) {
            flashlight.setFlashlightOff();
            flashState = false;
        } else {
            flashState = flashlight.setFlashlightOn(activity);
        }
        setFlashlightButton(menu);
    }

    public int Numeralise(String value) {
        switch (value) {
            case "One":
                return 1;
            case "Two":
                return 2;
            case "Three":
                return 3;
            case "Four":
                return 4;
            case "Five":
                return 5;
            case "Six":
                return 6;
        }
        return 1; // default to 1 in case of problems
    }

    //
    // map '0'-'9' to 0-9
    // map WiT deprecated throttle name characters T,S,G to throttle values
    //
    public int throttleCharToInt(char cWhichThrottle) {
        int val;
        if (Character.isDigit(cWhichThrottle)) {  // throttle number
            val = Character.getNumericValue((cWhichThrottle));
            if (val < 0) val = 0;
            if (val >= maxThrottles) val = maxThrottles - 1;
        } else switch (cWhichThrottle) {          // WiT protocol deprecated throttle letter codes
            case 'T':
                val = 0;
                break;
            case 'S':
                val = 1;
                break;
            case 'G':
                val = 2;
                break;
            default:
                val = 0;
                Log.d("debug", "TA.throttleCharToInt: no match for argument " + cWhichThrottle);
                break;
        }
        if (val > maxThrottlesCurrentScreen)
            Log.d("debug", "TA.throttleCharToInt: argument exceeds max number of throttles for current screen " + cWhichThrottle);
        return val;
    }

    public char throttleIntToChar(int whichThrottle) {
        return (char) (whichThrottle + '0');
    }

    public String throttleIntToString(int whichThrottle) {
        return Integer.toString(whichThrottle);
    }

    @SuppressLint("ApplySharedPref")
    public String fixThrottleName(String currentValue) {
        String defaultName = threaded_application.context.getResources().getString(R.string.prefThrottleNameDefaultValue);

        String newValue = currentValue;
        //if name is blank or the default name, make it unique
        if (currentValue.equals("") || currentValue.equals(defaultName)) {
            String deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
            if (deviceId != null && deviceId.length() >= 4) {
                deviceId = deviceId.substring(deviceId.length() - 4);
            } else {
                Random rand = new Random();
                deviceId = String.valueOf(rand.nextInt(9999));  //use random string
                if (MobileControl2.isMobileControl2()) {
                    // Change default name for ESU MCII
                    defaultName = threaded_application.context.getResources().getString(R.string.prefEsuMc2ThrottleNameDefaultValue);
                }
            }
            newValue = defaultName + " " + deviceId;
        }
        prefs.edit().putString("throttle_name_preference", newValue).commit();  //save new name to prefs
        return newValue;
    }


    public void vibrate(int duration) {
        //we need vibrate permissions, otherwise do nothing
        PermissionsHelper phi = PermissionsHelper.getInstance();
        if (phi.isPermissionGranted(threaded_application.context, PermissionsHelper.VIBRATE)) {
            try {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(duration);
                }
            } catch (Exception ignored) {
            }
        }

    }


    public void vibrate(long[] pattern) {
        //we need vibrate permissions, otherwise do nothing
        PermissionsHelper phi = PermissionsHelper.getInstance();
        if (phi.isPermissionGranted(threaded_application.this, PermissionsHelper.VIBRATE)) {
            try {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    //deprecated in API 26
                    v.vibrate(pattern, -1);
                }
            } catch (Exception ignored) {
            }
        }

    }

    //post process loco or Consist names to reduce the size of the address length strings
    public String locoAndConsistNamesCleanupHtml(String name) {
        return name.replaceAll("[(]S[)]", "<small><small>(S)</small></small>")
                .replaceAll("[(]L[)]", "<small><small>(L)</small></small>")
                .replaceAll("[+]", "<small>+</small>");
    }

    //display msg using Toast() safely by ensuring Toast() is called from the UI Thread
    public static void safeToast(final String msg_txt) {
        safeToast(msg_txt, Toast.LENGTH_SHORT);
    }

    public static void safeToast(final String msg_txt, final int length) {
        Log.d("Engine_Driver", "threaded_application.show_toast_message: " + msg_txt);
        //need to do Toast() on the main thread so create a handler
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(threaded_application.context, msg_txt, length).show();
            }
        });
    }

    public Intent getThrottleIntent() {
        Intent throttle;
        appIsFinishing = false;
        switch (prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault))) {
            case "Simple":
                throttle = new Intent().setClass(this, throttle_simple.class);
                break;
            case "Vertical":
            case "Vertical Left":
            case "Vertical Right":
                throttle = new Intent().setClass(this, throttle_vertical_left_or_right.class);
                break;
            case "Switching":
            case "Switching Left":
            case "Switching Right":
                throttle = new Intent().setClass(this, throttle_switching_left_or_right.class);
                break;
            case "Switching Horizontal":
                throttle = new Intent().setClass(this, throttle_switching_horizontal.class);
                break;
            case "Big Left":
            case "Big Right":
                throttle = new Intent().setClass(this, throttle_big_buttons.class);
                break;
            case "Default":
            default:
                throttle = new Intent().setClass(this, throttle_full.class);
                break;
        }
        return throttle;
    }

    /***
     * show appropriate messages on a restart that was forced by prefs
     *
     * @param prefForcedRestartReason the reason that the restart occurred
     * @return true if the activity should immediately launch Preferences
     */
    public boolean prefsForcedRestart(int prefForcedRestartReason) {
        switch (prefForcedRestartReason) {
            case FORCED_RESTART_REASON_AUTO_IMPORT:
            case FORCED_RESTART_REASON_IMPORT: {
                break;
            }
            case FORCED_RESTART_REASON_IMPORT_SERVER_MANUAL: {
                Toast.makeText(context,
                        context.getResources().getString(R.string.toastPreferencesImportServerManualSucceeded, prefs.getString("prefPreferencesImportFileName", "")), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_RESET: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesResetSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_THEME: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesThemeChangeSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_BACKGROUND: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesBackgroundChangeSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_THROTTLE_PAGE:
            case FORCED_RESTART_REASON_THROTTLE_SWITCH: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesThrottleChangeSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_LOCALE: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesLocaleChangeSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_IMPORT_SERVER_AUTO: {
                Toast.makeText(context,
                        context.getResources().getString(R.string.toastPreferencesImportServerAutoSucceeded, prefs.getString("prefPreferencesImportFileName", "")),
                        Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_FORCE_WIFI: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesChangedForceWiFi),
                        Toast.LENGTH_LONG).show();
                break;
            }
        }

        // include in this list if the Settings Activity should NOT be launched
        return ((prefForcedRestartReason != FORCED_RESTART_REASON_IMPORT_SERVER_AUTO)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_BACKGROUND)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_THROTTLE_SWITCH)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_IMPORT_SERVER_MANUAL)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_RESET)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_AUTO_IMPORT)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_FORCE_WIFI)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_DEAD_ZONE)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_SHAKE_THRESHOLD));
    }

    // saveSharedPreferencesToFile if the necessary permissions have already been granted, otherwise do nothing.
    // use this method if exiting since we don't want to prompt for permissions at this point if they have not been granted
    private void saveSharedPreferencesToFileIfAllowed() {
        if (PermissionsHelper.getInstance().isPermissionGranted(threaded_application.context, PermissionsHelper.STORE_PREFERENCES)) {
            saveSharedPreferencesToFileImpl();
        }
    }

    private void saveSharedPreferencesToFileImpl() {
        Log.d("Engine_Driver", "TA: saveSharedPreferencesToFileImpl: start");
        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
        String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", threaded_application.context.getResources().getString(R.string.prefAutoImportExportDefaultValue));

        if (prefAutoImportExport.equals(ImportExportPreferences.AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT)) {
            if (this.connectedHostName != null) {
                String exportedPreferencesFileName = this.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";

                if (!exportedPreferencesFileName.equals(".ed")) {
                    File path = Environment.getExternalStorageDirectory();
                    File engine_driver_dir = new File(path, "engine_driver");
                    engine_driver_dir.mkdir();            // create directory if it doesn't exist

                    ImportExportPreferences importExportPreferences = new ImportExportPreferences();
                    importExportPreferences.saveSharedPreferencesToFile(threaded_application.context, sharedPreferences, exportedPreferencesFileName);
                }
                Log.d("Engine_Driver", "TA: saveSharedPreferencesToFileImpl: done");
            } else {
                this.safeToast(threaded_application.context.getResources().getString(R.string.toastConnectUnableToSavePref), Toast.LENGTH_LONG);
            }
        }
    }

    public void getServerNameFromWebServer() {
        GetJsonFromUrl getJson = new GetJsonFromUrl(this);
        getJson.execute("http://" + host_ip + ":" + web_server_port + "/json/railroad");
        webServerNameHasBeenChecked = true;
    }

    /* only DCC-EX supports the "Request Loco ID" feature at this time */
    public boolean supportsIDnGo() {
        return serverType.equals("DCC-EX");
    }

    /* only JMRI and MRC support Rosters at this time */
    public boolean supportsRoster() {
        return (serverType.equals("JMRI") || serverType.equals("") || serverType.equals("MRC"));
    }

    /* add passed-in loco to Recent Locos list and store it */
    private void addLocoToRecents(ConLoco conLoco) {
        // if we don't have external storage mounted, or permission to write it, just ignore, no prompt
        if ((context.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))) {
            return;
        }
        ImportExportPreferences importExportPreferences = new ImportExportPreferences();
        importExportPreferences.getRecentLocosListFromFile();

        Integer engine_address = conLoco.getIntAddress();
        Integer address_size = conLoco.getIntAddressLength();
        String loco_name = conLoco.getFormatAddress();
        if (conLoco.getIsFromRoster()) {
            loco_name = conLoco.getRosterName();
        }
        Integer locoSource = conLoco.getWhichSource();
        for (int i = 0; i < importExportPreferences.recent_loco_address_list.size(); i++) {
            if (engine_address.equals(importExportPreferences.recent_loco_address_list.get(i))
                    && address_size.equals(importExportPreferences.recent_loco_address_size_list.get(i))
                    && loco_name.equals(importExportPreferences.recent_loco_name_list.get(i))) {
                importExportPreferences.recent_loco_address_list.remove(i);
                importExportPreferences.recent_loco_address_size_list.remove(i);
                importExportPreferences.recent_loco_name_list.remove(i);
                importExportPreferences.recent_loco_source_list.remove(i);
                Log.d("Engine_Driver", "Loco '"+ loco_name + "' removed from Recents");
                break;
            }
        }

        // now append it to the beginning of the list
        importExportPreferences.recent_loco_address_list.add(0, engine_address);
        importExportPreferences.recent_loco_address_size_list.add(0, address_size);
        importExportPreferences.recent_loco_name_list.add(0, loco_name);
        importExportPreferences.recent_loco_source_list.add(0, locoSource);

        importExportPreferences.writeRecentLocosListToFile(prefs);
        Log.d("Engine_Driver", "Loco '"+ loco_name + "' added to Recents");

    }

    @SuppressLint("ApplySharedPref")
    private void updateConnectionList(String retrievedServerName) {
        // if I don't have permissions, don't ask, just ignore
        if ((context.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED) {

            ImportExportConnectionList importExportConnectionList = new ImportExportConnectionList(prefs);
            importExportConnectionList.connections_list.clear();
            importExportConnectionList.getConnectionsList("", "");
            importExportConnectionList.saveConnectionsListExecute(
                    this, connectedHostip, connectedHostName, connectedPort, retrievedServerName);
            connectedHostName = retrievedServerName;

            String prefAutoImportExport = prefs.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue)).trim();

            String deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
            prefs.edit().putString("prefAndroidId", deviceId).commit();
            prefs.edit().putInt("prefForcedRestartReason", threaded_application.FORCED_RESTART_REASON_AUTO_IMPORT).commit();

            if ((prefAutoImportExport.equals(ImportExportPreferences.AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT))
                    || (prefAutoImportExport.equals(ImportExportPreferences.AUTO_IMPORT_EXPORT_OPTION_CONNECT_ONLY))) {  // automatically load the host specific preferences, if the preference is set
                if (connectedHostName != null) {
                    String exportedPreferencesFileName = connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                    ImportExportPreferences importExportPreferences = new ImportExportPreferences();
                    importExportPreferences.loadSharedPreferencesFromFile(getApplicationContext(), prefs, exportedPreferencesFileName, deviceId, true);

                    Message msg = Message.obtain();
                    msg.what = message_type.RESTART_APP;
                    msg.arg1 = threaded_application.FORCED_RESTART_REASON_AUTO_IMPORT;
                    Log.d("Engine_Driver", "updateConnectionList: Reload of Server Preferences. Restart Requested: " + connectedHostName);
                    comm_msg_handler.sendMessage(msg);
                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastConnectUnableToLoadPref), Toast.LENGTH_LONG).show();
                }
            }

        }
    }

        public void throttleVibration(int speed, int lastSpeed) {
        if ( (prefHapticFeedback.equals(HAPTIC_FEEDBACK_SLIDER))
            || (prefHapticFeedback.equals(HAPTIC_FEEDBACK_SLIDER_SCALED)) ) {
                int speedStepPref = getIntPrefValue(prefs, "DisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
                int xSpeed = speed;
                int xLastSpeed = lastSpeed;
                if (prefHapticFeedback.equals(HAPTIC_FEEDBACK_SLIDER_SCALED)) {
                    if (speedStepPref == 28) {
                        xSpeed = speed / 2;
                        xLastSpeed = lastSpeed / 2;
                    } else if (speedStepPref == 100) {
                        xSpeed = speed / 10;
                        xLastSpeed = lastSpeed / 10;
                    } else if (speedStepPref == 128) {
                        xSpeed = speed / 6;
                        xLastSpeed = lastSpeed / 6;
                    }
                }

                if ((xSpeed - xLastSpeed >= 1) || (xLastSpeed - xSpeed >= 1)
                        || ((xSpeed == 0) && (xLastSpeed != 0))
                        || ((xSpeed == 126) && (xLastSpeed != 126))) {
//                    Log.d("Engine_Driver", "ta: haptic_test: " + "beep");
                    vibrate(prefHapticFeedbackDuration);
                }
            }
        }

    public String getHostIp() {
        return host_ip;
        }

    public Double getWithrottleVersion() {
        return withrottle_version;
        }

    static public int getIntPrefValue(SharedPreferences sharedPreferences, String key, String defaultVal) {
        int newVal;
        try {
            newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
        } catch (NumberFormatException e) {
            try {
                newVal = Integer.parseInt(defaultVal);
            } catch (NumberFormatException ex) {
                newVal = 0;
            }
        }
        return newVal;
    }

    public void setToolbarTitle(Toolbar toolbar, String title, String iconTitle,  String clockText) {
        if (toolbar != null) {
            toolbar.setTitle("");
            TextView tvTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
            tvTitle.setText(title);

            TextView tvIconTitle = (TextView) toolbar.findViewById(R.id.toolbar_icon_title);
            tvIconTitle.setText(iconTitle);

            TextView tvIconHelp = (TextView) toolbar.findViewById(R.id.toolbar_icon_help);
            if (!prefFullScreenSwipeArea) {
                tvIconHelp.setText("");
            } else {
                tvIconHelp.setText("  ◄ ►");
            }

            TextView mClock = (TextView) toolbar.findViewById(R.id.toolbar_clock);
            mClock.setText(clockText);

//            adjustToolbarButtonSpacing(toolbar);
        }
    }

    public void hideSoftKeyboard(View view) {
        // Check if no view has focus:
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void hideSoftKeyboardAfterDialog() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }


//    public void adjustToolbarButtonSpacing(Toolbar toolbar){
//        Log.d("Engine_Driver", "ta: adjustToolbarButtonSpacing");
//
//        // Get the ChildCount of your Toolbar
//        int childCount = toolbar.getChildCount();
//
//        // Loop through the child Items
//        for(int i = 0; i < childCount; i++){
//            // Get the item at the current index
//            View childView = toolbar.getChildAt(i);
//            // If its a ViewGroup
//            if(childView instanceof ViewGroup){
//                // Get the child count of this view group
//                int innerChildCount = ((ViewGroup) childView).getChildCount();
//                // Create layout params for the ActionMenuView
////                ActionMenuView.LayoutParams params = new ActionMenuView.LayoutParams(80, ActionMenuView.LayoutParams.WRAP_CONTENT);
//                ActionMenuView.LayoutParams params;
//                // Loop through the children
//                for(int j = 0; j < innerChildCount; j++){
//                    View grandChild = ((ViewGroup) childView).getChildAt(j);
//                    if(grandChild instanceof ActionMenuItemView){
//                        setToolbarItemSize(grandChild);
//                    } else {
//                        if(grandChild instanceof ViewGroup) {
//                            innerChildCount = ((ViewGroup) grandChild).getChildCount();
//                            // Loop through the children
//                            for (int k = 0; k < innerChildCount; k++) {
//                                View greatGrandChild = ((ViewGroup) grandChild).getChildAt(k);
//                                if (greatGrandChild instanceof ActionMenuItemView) {
//                                    setToolbarItemSize(greatGrandChild);
//                                }
//                            }
//                        }
//                    }
//                    grandChild.invalidate();
//                }
//                childView.invalidate();
//            }
//        }
//    }
//
//    private void setToolbarItemSize(View actionMenuItemView) {
//        ActionMenuView.LayoutParams params;
//
//        if(actionMenuItemView instanceof ActionMenuItemView) {
//            // set the layout parameters on each View
//            params = (ActionMenuView.LayoutParams) actionMenuItemView.getLayoutParams();
//            params.width = actionMenuItemView.getWidth() + 100;
////                              params.rightMargin = params.rightMargin + 180;
//            actionMenuItemView.setLayoutParams(params);
//            actionMenuItemView.invalidate();
//        }
//    }
}
