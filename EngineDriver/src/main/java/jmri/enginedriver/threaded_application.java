/* Copyright (C) 2017-2026 M. Steve Todd mstevetodd@gmail.com

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
import static android.view.InputDevice.getDevice;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;

import android.annotation.SuppressLint;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
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
import android.content.res.TypedArray;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.appcompat.widget.Toolbar;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.Consist.ConLoco;
import jmri.enginedriver.type.auto_import_export_option_type;
import jmri.enginedriver.type.dccex_emergency_stop_state_type;
import jmri.enginedriver.type.gamepad_status_type;
import jmri.enginedriver.type.kids_timer_action_type;
import jmri.enginedriver.type.max_throttles_current_screen_type;
import jmri.enginedriver.type.notification_type;
import jmri.enginedriver.type.restart_reason_type;
import jmri.enginedriver.type.screen_swipe_index_type;
import jmri.enginedriver.type.sort_type;
import jmri.enginedriver.type.message_type;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import jmri.enginedriver.type.source_type;
import jmri.enginedriver.type.throttle_screen_type;
import jmri.enginedriver.type.toolbar_button_size_to_use_type;
import jmri.enginedriver.type.toolbar_button_size_type;
import jmri.enginedriver.util.ArrayQueue;
import jmri.enginedriver.util.Flashlight;
import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.ImageDownloader;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.comms.comm_handler;
import jmri.enginedriver.comms.comm_thread;
import jmri.enginedriver.import_export.ImportExportPreferences;
import jmri.enginedriver.import_export.ImportExportConnectionList;

import jmri.jmrit.roster.RosterEntry;
import jmri.jmrit.roster.RosterLoader;

//The application will start up a thread that will handle network communication in order to ensure that the UI is never blocked.
//This thread will only act upon messages sent to it. The network communication needs to persist across activities, so that is why
@SuppressLint("NewApi")
public class threaded_application extends Application {
    public static final String applicationName = "Engine_Driver";
    public static final String activityName = "t_a";

    // it does not matter what these value are as long as they have never been used before (so just add 1)
    public static String INTRO_VERSION = "10";  // set this to a different string to force the intro to run on next startup.
    private static final String LAST_PREFERENCE_NAME_RUN = "3";  // set this to a different string to force the check and rename of the old preferences on next startup.

    private final threaded_application mainapp = this;
    public comm_thread commThread;

    public String JMDNS_SERVICE_WITHROTTLE = "_withrottle._tcp.local.";
    public String JMDNS_SERVICE_JMRI_DCCPP_OVERTCP = "_dccppovertcpserver._tcp.local.";

    public volatile String host_ip = null; //The IP address of the WiThrottle server.
    public volatile String logged_host_ip = null;
    public volatile int port = 0; //The TCP port that the WiThrottle server is running on
    public Double withrottle_version = 0.0; //version of withrottle server
    public volatile int web_server_port = 0; //default port for jmri web server
    private String serverType = ""; //should be set by server in initial command strings
    private String serverDescription = ""; //may be set by server in initial command strings
    public String defaultWebViewURL;
    public String defaultThrottleWebViewURL;
    public volatile boolean doFinish = false;  // when true, tells any Activities that are being created/resumed to finish()
    //shared variables returned from the withrottle server, stored here for easy access by other activities
    public volatile Consist[] consists;
    public LinkedHashMap<Integer, String>[] function_labels;  //function#s and labels from roster for throttles
    public LinkedHashMap<Integer, String> function_labels_default;  //function#s and labels from local settings
    LinkedHashMap<Integer, String> function_labels_default_for_roster;  //function#s and labels from local settings for roster entries with no function labels
    public LinkedHashMap<Integer, String> function_consist_locos; // used for the 'special' consists function label string matching
    public LinkedHashMap<Integer, String> function_consist_latching; // used for the 'special' consists function label string matching

    public boolean[][] function_states = {null, null, null, null, null, null};  //current function states for throttles
    public String[] to_system_names;
    public String[] to_user_names;
//    public String[] to_states;
    private HashMap<String, String> turnout_states; //includes manual and system
    public HashMap<String, String> to_state_names;
    public int turnoutsOrder = sort_type.NAME;
    public String[] routeSystemNames;
    public String[] rt_user_names;
    public int routesOrder = sort_type.NAME;
    public String[] routeStates;
    public String[] routeDccexLabels; // only used by the DCC-EX protocol
    public int[] routeDccexStates; // only used by the DCC-EX protocol.   -1 if not DCC-EX
    public HashMap<String, String> routeStateNames; //if not set, routes are not allowed
    public Map<String, String> roster_entries;  //roster sent by WiThrottle
    public ArrayList<HashMap<String, String>> rosterFullList; // populated by select_loco. as different to roster which is populated using XML via the WebServer
    public int rosterOrder = sort_type.NAME;
    public int recentLocosOrder = sort_type.LAST_USED;
    public int recentConsistsOrder = sort_type.LAST_USED;
    public Map<String, String> consist_entries;
    public static DownloadRosterTask dlRosterTask = null;
//    private static DownloadMetaTask dlMetadataTask = null;
    HashMap<String, RosterEntry> rosterJmriWeb;  //roster entries retrieved from /roster/?format=xml (null if not retrieved)
//    public static HashMap<String, String> jmriMetadata = null;  //metadata values (such as JMRIVERSION) retrieved from web server (null if not retrieved)
//    ImageDownloader imageDownloader = new ImageDownloader();
    ImageDownloader imageDownloader = new ImageDownloader();
    public String power_state;

    public int getFastClockFormat() {
        return fastClockFormat;
    }

    public int fastClockFormat = 0; //0=no display, 1=12hr, 2=24hr
    public Long fastClockSeconds = 0L;
//    public int androidVersion = 0;
    public String appVersion = "";

    //all heartbeat values are in milliseconds
    public int heartbeatInterval = 0;                       //WiT heartbeat interval setting (milliseconds)
    public int prefHeartbeatResponseFactor = 90;   //adjustment for inbound and outbound timers as a percent

    public int turnouts_list_position = 0;                  //remember where user was in item lists
    public int routes_list_position = 0;

    public static int WiThrottle_Msg_Interval = 100;   //minimum desired interval (ms) between messages sent to
    //  WiThrottle server, can be changed for specific servers
    //   do not exceed 200, unless slider delay is also changed

    public static final int MAX_FUNCTIONS = 32;              // total number of supported functions
    public static final String MAX_FUNCTIONS_TEXT = "32";              // total number of supported functions
    public static final int MAX_FUNCTION_NUMBER = 31;        // maximum number of the function buttons supported.

    public String deviceId = "";

    public static final String DEMO_HOST = "jmri.mstevetodd.com";
    public static final String DEMO_PORT = "44444";
    public static final String DUMMY_HOST = "999";
    public static final String DUMMY_ADDRESS = "999";
    public static final int DUMMY_PORT = 999;
    public static final String DUMMY_SSID = "";

    public String client_address; //address string of the client address
    public Inet4Address client_address_inet4; //inet4 value of the client address
    public String client_ssid = "UNKNOWN";    //string of the connected SSID
    public String client_type = "UNKNOWN"; //network type, usually WIFI or MOBILE
    public boolean clientLocationServiceEnabled = false;
    public boolean connectionWarningsShown = false;

    public int whichThrottleLastTouch = 0; // needed in TA so that it can be used in the DCC-EX code

    public String prefAppIconAction = "throttle";

    public HashMap<String, String> knownDCCEXserverIps = new HashMap<>();
    public boolean isDCCEX = false;  // is a DCC-EX EX-CommandStation
    public String prefUseDccexProtocol = "Auto";
    public boolean prefAlwaysUseFunctionsFromServer = false;
    public String dccexVersionString = "";
    public int DCCEXlistsRequested = -1;  // -1=not requested  0=requested  1,2,3= no. of lists received

    public boolean dccexScreenIsOpen = false;
    public boolean witScreenIsOpen = false;

    public int dccexActionTypeIndex = 0;
    public int [] dccexTrackType = {1, 2, 0, 0, 0, 0, 0, 0};
    public int [] dccexTrackPower = {-1, -1, -1, -1, -1, -1, -1, -1};
    public boolean [] dccexTrackAvailable = {false, false, false, false, false, false, false, false};
    public String [] dccexTrackId = {"", "", "", "", "", "", "", ""};
    public final static int DCCEX_MAX_TRACKS = 8;
    public boolean dccexJoined = false;
    public int dccexEmergencyStopState = dccex_emergency_stop_state_type.RESUMED;

//    public boolean dccexRosterFullyReceived = false;
    public boolean dccexRosterRequested = false;
    public String dccexRosterString = ""; // used to process the roster list
    public int [] dccexRosterIDs;  // used to process the roster list
    public String [] dccexRosterLocoNames;  // used to process the roster list
    public String [] dccexRosterLocoFunctions;  // used to process the roster list
    public boolean [] dccexRosterDetailsReceived;  // used to process the roster list

    public int dccexPreviousCommandIndex = -1;
    public ArrayList<String> dccexPreviousCommandList = new ArrayList<>();

    public boolean prefDccexEmergencyStopPauseResume = false;

    public boolean [] [] throttleFunctionIsLatchingDCCEX = {null, null, null, null, null, null};
    public String [][] throttleLocoReleaseListDCCEX = {null, null, null, null, null, null};  // used to process the list of locos to release on a throttle

    public boolean dccexTurnoutsBeingProcessed = false;
    public boolean dccexTurnoutsRequested = false;
    public boolean dccexTurnoutsFullyReceived = false;
    public String dccexTurnoutString = ""; // used to process the turnout list
    public int [] dccexTurnoutIDs;  // used to process the turnout list
    public String [] dccexTurnoutNames;  // used to process the turnout list
    public String [] dccexTurnoutStates;  // used to process the turnout list
    public boolean [] dccexTurnoutDetailsReceived;  // used to process the turnout list

    public boolean dccexRoutesBeingProcessed = false;
    public String dccexRouteString = ""; // used to process the route list
    public int [] dccexRouteIDs;  // used to process the route list
    public String [] dccexRouteNames;  // used to process the route list
    public String [] dccexRouteTypes;  // used to process the route list
    public String [] dccexRouteStates;  // used to process the route list
    public String [] dccexRouteLabels;  // used to process the route list
    public boolean [] dccexRouteDetailsReceived;  // used to process the route list
    public boolean dccexRouteStatesReceived = false;
    public ArrayList<ArrayList<HashMap<String, String>>> dccexInCommandStationConsists;  // used to process the In Command Station consist list

    // only used to track the state of the numeric keys of an attached keyboard for DCCEX (0-9)
    public int[] numericKeyIsPressed = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
    public int[] numericKeyFunctionStateAtTimePressed = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};

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
    public volatile Handler dcc_ex_msg_handler;
    public volatile Handler advancedConsistToolMesssgeHandler;
    public volatile Handler withrottle_cv_programmer_msg_handler;
    public volatile Handler reconnect_status_msg_handler;
    public volatile Handler settings_msg_handler;
    public volatile Handler logviewer_msg_handler;
    public volatile Handler about_page_msg_handler;
    public volatile Handler function_settings_msg_handler;
    public volatile Handler function_consist_settings_msg_handler;
    public volatile Handler gamepad_test_msg_handler;
    public volatile Handler device_sounds_settings_msg_handler;

    // for handling control of camera flash
    public static Flashlight flashlight;
    public boolean flashState = false;

    static public int currentActivity = 0;

    //these constants are used for onFling
    public static final int SWIPE_MIN_DISTANCE = 120;
//    public static final int SWIPE_MAX_OFF_PATH = 250;
    public static final int SWIPE_THRESHOLD_VELOCITY = 200;
    public static int min_fling_distance;           // pixel width needed for fling
    public static int min_fling_velocity;           // velocity needed for fling

    public static DisplayMetrics displayMetrics;
    public static double displayDiagonalInches;
    public static String prefToolbarButtonSize = "auto";
//    public static boolean useSmallToolbarButtonSize = true;
    public static int toolbarButtonSizeToUse = toolbar_button_size_to_use_type.SMALL;
    public static final double MEDIUM_SCREEN_SIZE = 5.5; // inches
    public static final double LARGE_SCREEN_SIZE = 6.7; // inches
    public static int toolbarButtonCount = 0;
    private int initialToolbarHeight = -1;

    int notificationLevel = 0; // no notification
    NotificationManager notificationManager;
    NotificationChannel notificationChannel;
    NotificationCompat.Builder notificationCompatBuilder;
    private Notification.Builder notificationBuilder;
    private static final int ED_NOTIFICATION_ID = 416;  //no significance to 416, just shouldn't be 0

    private SharedPreferences prefs;

    public boolean EStopActivated = false;  // Has EStop been sent?

    public int prefNumThrottles = 1;
    public int maxThrottles = 6;   // maximum number of throttles the system supports
    public int maxThrottlesCurrentScreen = 6;   // maximum number of throttles the current screen supports
    public boolean currentScreenSupportsWebView = true;
    public boolean throttleSwitchAllowed = false; // used to prevent throttle switches until the previous onStart() completes
    public boolean throttleSwitchWasRequestedOrReinitialiseRequired = false;

    @NonNull
    public String connectedHostName = "";
    @NonNull
    public String connectedHostip = "";
//    public int getConnectedPort() {
//        return connectedPort;
//    }
    public int connectedPort = 0;
    public String connectedSsid = "";
    public String connectedServiceType = "";

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
    public boolean exitConfirmed = false;
    /** @noinspection FieldCanBeLocal*/
    private ApplicationLifecycleHandler lifecycleHandler;
//    public static Context context;

    public long exitDoubleBackButtonInitiated = 0;

    public int actionBarIconCountThrottle = 0;
    public int actionBarIconCountRoutes = 0;
    public int actionBarIconCountTurnouts = 0;

    public Resources.Theme theme;

    protected int throttleLayoutViewId;
    public boolean webServerNameHasBeenChecked = false;

    public boolean haveForcedWiFiConnection = false;
    public boolean prefAllowMobileData = false;

    public boolean prefAlwaysUseDefaultFunctionLabels = false;
    public String prefConsistFollowRuleStyle = "original";
    public boolean prefOverrideWiThrottlesFunctionLatching = false;
    public boolean prefOverrideRosterWithNoFunctionLabels = false;

    public boolean prefShowTimeOnLogEntry = false;
    public static boolean prefExtendedLogging = false;
    public static boolean prefLogOnNextStartup = false;
    public String logSaveFilename = "";
    public Process logcatProcess;

    public boolean prefFeedbackOnDisconnect = true;

    public String prefHapticFeedback = "None";
    //    public int prefHapticFeedbackSteps = 10;
    public int prefHapticFeedbackDuration = 250;
    public boolean prefHapticFeedbackButtons = false;

    public boolean prefHideInstructionalToasts = false;

    public boolean prefSwipeThoughTurnouts = true;
    public boolean prefSwipeThoughRoutes = true;
    public boolean prefSwipeThoughWeb = true;

    public boolean prefFullScreenSwipeArea = false;
    public boolean prefLeftRightSwipeChangesSpeed = false;
    public int prefSwipeSpeedChangeStep = 8;
    public boolean prefThrottleViewImmersiveModeHideToolbar = true;
    public boolean prefActionBarShowServerDescription = false;

//    public static final String HAPTIC_FEEDBACK_NONE = "None";
    public static final String HAPTIC_FEEDBACK_SLIDER = "Slider";
    public static final String HAPTIC_FEEDBACK_SLIDER_SCALED = "Scaled";

    private ToneGenerator tg;
    protected MediaPlayer _mediaPlayer;

    public SoundPool soundPool;

    public boolean prefDeviceSoundsButton = false;
    public boolean prefDeviceSoundsBellIsMomentary = false;
    public boolean prefDeviceSoundsMomentumOverride = false;
    public boolean prefDeviceSoundsF1F2ActivateBellHorn = false;
    public boolean prefDeviceSoundsHideMuteButton = false;
    public String[] prefDeviceSounds = {"none", "none"};  //currently only supporting two throttles
    public String[] prefDeviceSoundsCurrentlyLoaded = {"none", "none"};  //currently only supporting two throttles
    public static final int SOUND_MAX_SUPPORTED_THROTTLES = 2;
    public float prefDeviceSoundsMomentum = 1000;
    public float prefDeviceSoundsLocoVolume = 1;
    public float prefDeviceSoundsBellVolume = 1;
    public float prefDeviceSoundsHornVolume = 1;
    public boolean soundsReloadSounds = true;
    public boolean soundsSoundsAreBeingReloaded = false;

    public boolean[][] soundsDeviceButtonStates = {{false, false, false}, {false, false, false}};

    // [Type = Bell, Horn, HornShort] [whichThrottle] [Start, Loop, End]
    public int[][][] soundsExtras = {{{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}}};  // Start, Loop, End
    public int[][][] soundsExtrasStreamId = {{{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}}};
    public int[][][] soundsExtrasDuration = {{{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}}};
    public double[][][] soundsExtrasStartTime = {{{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}, {0, 0, 0}}};
    public int[][] soundsExtrasCurrentlyPlaying = {{-1, -1}, {-1, -1}, {-1, -1}};

    public int[][] soundsLoco = { // need one for each type of sound set available to select
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    public int[][] soundsLocoStreamId = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    public int[][] soundsLocoDuration = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    public double[][] soundsLocoStartTime = {  //  extra entries for the Startup and Shut down sounds  20 and 21
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
    public int[] soundsLocoCurrentlyPlaying = {-1, -1};
    public int[] soundsLocoLastDirection = {1, 1};

    public ArrayQueue[] soundsLocoQueue = new ArrayQueue[2];

    public int[] soundsLocoSteps = new int[2];

    public ArrayList<String> iplsNames;
    public ArrayList<String> iplsFileNames;

    // moved from throttle to ta to allow for gamepads to function in other activities
    public static final int MAX_GAMEPADS = 6;
    public static final String WHICH_GAMEPAD_MODE_NONE = "None";
    public String prefGamePadType = WHICH_GAMEPAD_MODE_NONE;
    public boolean prefGamepadOnlyOneGamepad = true;
    public String[] gamePadDescriptorsAssignedToThrottles = {"", "", "", "", "", "", ""}; // which device have we seen - Names
    public int[] gamePadThrottleAssignment = {-1, -1, -1, -1, -1, -1};
    public boolean usingMultiplePads = false;
    public String[] gamePadDeviceDescriptors = {"", "", "", "", "", "", ""}; // which device have we seen - Names
    public String[] gamePadDeviceNames = {"", "", "", "", "", "", ""}; // which device have we seen - Names
    public int[] gamePadDeviceIdsTested = {-1, -1, -1, -1, -1, -1, -1}; // which device ids have we tested  -1 = not tested 0 = test started 1 = test passed 2 = test failed
    public int gamepadCount = 0;
    public float[] gamePadLastxAxis = {0, 0, 0, 0, 0, 0, 0};
    public float[] gamePadLastyAxis = {0, 0, 0, 0, 0, 0, 0};
    public float[] gamePadLastxAxis2 = {0, 0, 0, 0, 0, 0, 0};
    public float[] gamePadLastyAxis2 = {0, 0, 0, 0, 0, 0, 0};

    public boolean prefGamePadIgnoreJoystick = false;
    public int prefGamePadFeedbackVolume = 100;
    public int[] gamepadBeepIds;

    public int[] dccexLastKnownSpeed = {0,0,0,0,0,0};
    public int[] dccexLastKnownDirection = {1,1,1,1,1,1};
    public long[] dccexLastSpeedCommandSentTime = {0,0,0,0,0,0};

    public boolean prefActionBarShowDccExButton = false;
    public boolean prefActionBarShowThrottleButton = false;
    public boolean prefActionBarShowTurnoutsButton = false;
    public boolean prefActionBarShowRoutesButton = false;
    public boolean prefActionBarShowWebButton = false;

    public String witCv = "";
    public String witCvValue = "";
    public String witAddress = "";

    static boolean activityVisible = false;
    static boolean activityInTransition = false;
    static String lastActivityName = "unknown";

    // ESU MC 2/Pro
    public String prefEsuMc2SliderType = "esu";
    public boolean useEsuMc2DecoderBrakes = false;
    public int [][] esuMc2BrakeFunctions = {{4,0,0,0,0}, {5,0,0,0,0}, {6,0,0,0,0} };
    public int [] esuMc2BrakeLevels = {30, 60, 100};
    public boolean [][] esuMc2BrakeActive = {{false, false, false}, {false, false, false}, {false, false, false}, {false, false, false}, {false, false, false}, {false, false, false}};
    public float esuMc2BrakePosition = 0;
    public boolean [] EsuMc2FirstServerUpdate = {false, false, false, false, false, false };

    public static boolean isActivityVisible() {
        extendedLogging(activityName + ": isActivityVisible(): " + ((activityVisible || activityInTransition) ? "True" : "False"));
        return activityVisible || activityInTransition;
    }

    public static void activityResumed(String activityName) {
        activityVisible = true;
        activityInTransition = false;
        lastActivityName = activityName;
        extendedLogging(activityName + ": activityResumed("+activityName+")");
    }

    public static void activityPaused() {
        activityPaused(lastActivityName);
    }
    public static void activityPaused(String activityName) {
        activityVisible = false;
        lastActivityName = activityName;
        threaded_application.extendedLogging(activityName + ": activityPaused("+activityName+")");
    }

    public static void activityNotInTransition() {
        activityNotInTransition(lastActivityName);
    }
    public static void activityNotInTransition(String activityName) {
        activityInTransition = false;
        lastActivityName = activityName;
        threaded_application.extendedLogging(activityName + ": activityNotInTransition("+activityName+")");
    }

    public static void activityInTransition(String activityName) {
        activityInTransition = true;
        lastActivityName = activityName;
        threaded_application.extendedLogging(activityName + ":  activityInTransition("+activityName+")");
    }

    /**
     * Display OnGoing Notification that indicates EngineDriver is Running.
     * Should only be called when ED is going into the background.
     * Currently call this from each activity onPause, passing the current intent
     * to return to when reopening.
     */

    // -----------------------------------------------------------------------------------------------------------------------------------
    // Notifications


    private void createNotificationChannelAndManager(Intent notificationIntent) {

        if (notificationManager == null) {
            if (notificationIntent == null) return;
            // create it
            if (Build.VERSION.SDK_INT >= 26) {
                String CHANNEL_ID = "ed_channel_HIGH";// The id of the channel.
                String CHANNEL_ID_QUIET = "ed_channel_HIGH_quiet";// The id of the channel without sound.
                String channelId;
                CharSequence name = this.getString(R.string.notificationInBackgroundTitle);// The user-visible name of the channel.

                boolean prefFeedbackWhenGoingToBackground = prefs.getBoolean("prefFeedbackWhenGoingToBackground", getResources().getBoolean(R.bool.prefFeedbackWhenGoingToBackgroundDefaultValue));
                if (prefFeedbackWhenGoingToBackground) {
                    channelId = CHANNEL_ID;
                    notificationChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
                } else {
                    channelId = CHANNEL_ID_QUIET;
                    notificationChannel = new NotificationChannel(CHANNEL_ID_QUIET, name, NotificationManager.IMPORTANCE_DEFAULT);
                    notificationChannel.setSound(null, null);
                }

                PendingIntent contentIntent = PendingIntent.getActivity(this, ED_NOTIFICATION_ID, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                notificationBuilder = new Notification.Builder(getApplicationContext(), notificationChannel.getId());

                notificationBuilder = notificationBuilder
                        .setSmallIcon(R.drawable.icon_notification)
                        .setContentTitle(this.getString(R.string.notificationInBackgroundTitle))
                        .setContentText("")
                        .setContentIntent(contentIntent)
                        .setOngoing(true)
                        .setOnlyAlertOnce(false)
                        .setChannelId(channelId);

                // Add as notification
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(notificationChannel);

            } else {
                //noinspection deprecation
                notificationCompatBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.icon_notification)
                                .setContentTitle(getResources().getString(R.string.notificationInBackgroundTitle))
                                .setContentText("")
                                .setOngoing(true)
                                .setOnlyAlertOnce(false)
                                .setPriority(NotificationManager.IMPORTANCE_HIGH);

                PendingIntent contentIntent = PendingIntent.getActivity(this, ED_NOTIFICATION_ID, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                notificationCompatBuilder.setContentIntent(contentIntent);

                // Add as notification
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }
        }

    }

    private void addNotification(Intent notificationIntent, int level) {
        if ( (isActivityVisible()) || (notificationLevel>0) ) return;
        threaded_application.extendedLogging(activityName + ": addNotification()");
        notificationLevel = level;
        String notificationText = getResources().getString(R.string.notificationInBackgroundText);
        if (level == 2) {
            notificationText = getResources().getString(R.string.notificationInBackgroundTextLowMemory);
        } else if (level == 3) {
            notificationText = getResources().getString(R.string.notificationInBackgroundTextKilled);
        }

        vibrate(new long[]{1000, 500, 1000, 500});
        createNotificationChannelAndManager(notificationIntent);

        if (Build.VERSION.SDK_INT >= 26) {
            notificationBuilder.setContentText(notificationText);
            PendingIntent contentIntent = PendingIntent.getActivity(this, ED_NOTIFICATION_ID, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            notificationBuilder.setContentIntent(contentIntent);
            notificationManager.notify(ED_NOTIFICATION_ID, notificationBuilder.build());

        } else {
            notificationCompatBuilder.setContentText(notificationText);

            PendingIntent contentIntent = PendingIntent.getActivity(this, ED_NOTIFICATION_ID, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            notificationCompatBuilder.setContentIntent(contentIntent);
            notificationManager.notify(ED_NOTIFICATION_ID, notificationCompatBuilder.build());
        }
    }

//    private void updateNotification(String updateText) {
//        if ( (notificationManager == null) || (isActivityVisible()) ) return;
//        Log.d(applicationName, "Engine Driver:updateNotification()");
//        notificationLevel = 2;
//
//        vibrate(new long[]{1000, 500, 1000, 500});
//        if (Build.VERSION.SDK_INT >= 26) {
//            notificationBuilder.setContentText(updateText);
//            notificationBuilder.setOnlyAlertOnce(false);
//            notificationManager.notify(ED_NOTIFICATION_ID, notificationBuilder.build());
//        } else {
//            notificationCompatBuilder.setContentText(updateText);
//            notificationCompatBuilder.setOnlyAlertOnce(false);
//            notificationManager.notify(ED_NOTIFICATION_ID, notificationCompatBuilder.build());
//        }
//    }

    // Remove notification
    public void removeNotification(Intent notificationIntent) {
        threaded_application.extendedLogging(activityName + ": removeNotification()");
        if (notificationIntent == null) return;
        notificationLevel = 0;
        createNotificationChannelAndManager(notificationIntent);
        notificationManager.cancel(ED_NOTIFICATION_ID);
    }

    // Notifications
    // -----------------------------------------------------------------------------------------------------------------------------------


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(applicationName, "t_a: onCreate()");
        try {
            appVersion = "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
//        androidVersion = android.os.Build.VERSION.SDK_INT;

        Log.i(applicationName, "Engine Driver:" + appVersion + ", SDK:" + android.os.Build.VERSION.SDK_INT);

//        context = getApplicationContext();

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        renamePreferencesToNewFormat();

        commThread = new comm_thread(mainapp, prefs);

        flashlight = Flashlight.newInstance(getApplicationContext());

        lifecycleHandler = new ApplicationLifecycleHandler();
        registerActivityLifecycleCallbacks(lifecycleHandler);
        registerComponentCallbacks(lifecycleHandler);

        prefNumThrottles = Numeralise(prefs.getString("prefNumThrottles", getResources().getString(R.string.prefNumThrottlesDefaultValue)));
        throttleLayoutViewId = R.layout.throttle;

        haveForcedWiFiConnection = false;

//        try {
//            Map<String, ?> ddd = prefs.getAll();
//            String dwr = prefs.getString("TypeThrottle", "false");
//        } catch (Exception ex) {
//            String dwr = ex.toString();
//        }

        for (int i = 0; i < maxThrottlesCurrentScreen; i++) {
            function_states[i] = new boolean[MAX_FUNCTION_NUMBER+1];
        }

//        dlMetadataTask = new DownloadMetaTask();
        dlRosterTask = new DownloadRosterTask();

        //use worker thread to initialize default function labels from file so UI can continue
        new Thread(new Runnable() {
            public void run() {
                set_default_function_labels(false);
            }
        }, "DefaultFunctionLabels").start();

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        prefShowTimeOnLogEntry = prefs.getBoolean("prefShowTimeOnLogEntry",
                getResources().getBoolean(R.bool.prefShowTimeOnLogEntryDefaultValue));
        prefExtendedLogging = prefs.getBoolean("prefExtendedLogging",
                getResources().getBoolean(R.bool.prefExtendedLoggingDefaultValue));
        prefFeedbackOnDisconnect = prefs.getBoolean("prefFeedbackOnDisconnect",
                getResources().getBoolean(R.bool.prefFeedbackOnDisconnectDefaultValue));

        prefHideInstructionalToasts = prefs.getBoolean("prefHideInstructionalToasts",
                getResources().getBoolean(R.bool.prefHideInstructionalToastsDefaultValue));

    } // end onCreate()

    public class ApplicationLifecycleHandler implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
        private boolean isInBackground = true;
        private Activity runningActivity = null;

        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            Log.d(applicationName, "t_a: ALO/ALH: onActivityResumed(): " + activity.getComponentName() + " : " + (isInBackground ? "Coming out of Background" : "foreground"));
            if (isInBackground) {                           // if coming out of background
                isInBackground = false;
                exitConfirmed = false;
                removeNotification((runningActivity != null) ? runningActivity.getIntent() : null);
            }
            runningActivity = activity;                 // save most recently resumed activity
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            Log.d(applicationName, "t_a: ALO/ALH: onActivityDestroyed(): " + activity.getComponentName());
            if (isInBackground && activity == runningActivity) {
                removeNotification(runningActivity.getIntent()); // destroyed in background so remove notification
            }
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration configuration) {
        }

        @Override
        public void onLowMemory() {
            Log.d(applicationName, "t_a: ALO/ALH: onLowMemory():");
        }

        @SuppressLint("ApplySharedPref")
        @Override
        public void onTrimMemory(int level) {
            Log.d(applicationName, "t_a: ALO/ALH: onTrimMemory(): " + level);
            if (exitConfirmed) return;

            Log.d(applicationName, "t_a: ALO/ALH: onTrimMemory(): " + level + " - Not Exiting");
            if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {   // if in background
                if (!isInBackground) {                              // if just went into bkg
                    isInBackground = true;
                    activityPaused();
                    activityNotInTransition();
                    if (runningActivity != null)
                        addNotification(runningActivity.getIntent(), notification_type.APP_PUSHED_TO_BACKGROUND);
                    prefs.edit().putBoolean("prefForcedRestart", true).commit();
                    prefs.edit().putInt("prefForcedRestartReason", restart_reason_type.APP_PUSHED_TO_BACKGROUND).commit();

                    if (prefs.getBoolean("prefStopOnBackground",
                            mainapp.getResources().getBoolean(R.bool.prefStopOnBackgroundDefaultValue))) {
                        Log.d(threaded_application.applicationName, activityName + ": onTrimMemory(): Stopping Trains");
                        if (mainapp.consists != null) {
                            for (int i = 0; i < mainapp.prefNumThrottles; i++) {
                                if ( (mainapp.consists[i] != null) && (mainapp.consists[i].isActive()) ) {
                                    sendMsg(comm_msg_handler, message_type.ESTOP_ONE_THROTTLE, "", i);
                                }
                            }
                        }
                    }
                }
            }
            if ( (level == ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            || (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE) ) {
                if (!isActivityVisible()) {   // double check it is in background
//                        updateNotification(getResources().getString(R.string.notificationInBackgroundTextLowMemory));
                    if (runningActivity != null) {
                        removeNotification(runningActivity.getIntent());
                        addNotification(runningActivity.getIntent(), notification_type.LOW_MEMORY);
                        // if this is called, assume that we will need to recreate the Throttle Screen
                        mainapp.throttleSwitchWasRequestedOrReinitialiseRequired = true;
                    }
                }
            } else if ( (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE)
                    || (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) ){
                if (!isActivityVisible()) {   // double check it is in background
                    // disconnect and shutdown
                    safeToast(getResources().getString(R.string.notificationInBackgroundTextKilled),Toast.LENGTH_LONG);
                    sendMsg(comm_msg_handler, message_type.SHUTDOWN, "", 1);
                    exitConfirmed = true;
                }
            }
        }

        @Override
        public void onActivityPreDestroyed(Activity activity) {
            Log.d(applicationName, "t_a: ALO/ALH: onActivityPreDestroyed(): " + activity.getComponentName());
        }

        @Override
        public void onActivityPreResumed(Activity activity) {
            Log.d(applicationName, "t_a: ALO/ALH: onActivityPreResumed(): " + activity.getComponentName());
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
        String latchingDefault = getResources().getString(R.string.prefFunctionConsistLatchingDefaultValue);
        String latchingLightBellDefault = getResources().getString(R.string.prefFunctionConsistLatchingLightBellDefaultValue);
//        String latchingDefaultEnglish = mainapp.getString(R.string.prefFunctionConsistLatchingLightBellDefaultValueEnglish); // can not change with language

        int numberOfDefaultFunctionLabels = MAX_FUNCTIONS;
        int numberOfDefaultFunctionLabelsForRoster = MAX_FUNCTIONS;
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
            File settings_file = new File(getApplicationContext().getExternalFilesDir(null), "function_settings.txt");
            if (settings_file.exists()) {  //if file found, use it for settings arrays
                BufferedReader settings_reader = new BufferedReader(new FileReader(settings_file));
                //read settings into local arrays
                int i = 0;
                while (settings_reader.ready()) {
                    String line = settings_reader.readLine();
                    String[] temp = line.split(":");
                    if (temp.length >= 2) {
                        if (i <= numberOfDefaultFunctionLabels) {
                            function_labels_default.put(Integer.parseInt(temp[1]), temp[0]); //put functions and labels into global default
                        }
                        if (i <= numberOfDefaultFunctionLabelsForRoster) {
                            function_labels_default_for_roster.put(Integer.parseInt(temp[1]), temp[0]); //put functions and labels into global default
                        }
                    }
                    if (temp.length == 4) {
                        if (i <= numberOfDefaultFunctionLabels) {
                            function_consist_locos.put(Integer.parseInt(temp[1]), temp[2]);
                            function_consist_latching.put(Integer.parseInt(temp[1]), temp[3]);
                        } else {
                            function_consist_locos.put(Integer.parseInt(temp[1]), locosDefault);
                            if (i != 2) { // make everything other than 'horn' latching by default
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
                    function_labels_default.put(0, getApplicationContext().getResources().getString(R.string.functionButton00DefaultValue));
                    function_consist_latching.put(0, latchingLightBellDefault);
                }
                if (numberOfDefaultFunctionLabels >= 1) {
                    function_labels_default.put(1, getApplicationContext().getResources().getString(R.string.functionButton01DefaultValue));
                    function_consist_latching.put(1, latchingLightBellDefault);
                }
                if (numberOfDefaultFunctionLabels >= 2) {
                    function_labels_default.put(2, getApplicationContext().getResources().getString(R.string.functionButton02DefaultValue));
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
            Log.e(applicationName, activityName + ": Could not read file, error: " + except.getMessage());
        } catch (NumberFormatException except) {
            Log.e(threaded_application.applicationName, activityName + ": set_default_function_labels(): NumberFormatException reading function_settings file: " + except.getMessage());
        }

    }

    public class DownloadRosterTask extends DownloadDataTask {
        @SuppressWarnings("unchecked")
        @Override
        void runMethod(Download dl) throws IOException {
            String rosterUrl = createUrl("roster/?format=xml");
            HashMap<String, RosterEntry> rosterTemp;
            if (rosterUrl == null || rosterUrl.isEmpty() || dl.cancel)
                return;
            Log.d(applicationName, "t_a: Background loading roster from " + rosterUrl);
            int rosterSize;
            try {
                RosterLoader rl = new RosterLoader(rosterUrl);
                if (dl.cancel)
                    return;
                rosterTemp = rl.parse();
                if (rosterTemp == null) {
                    Log.w(applicationName, "t_a: Roster parse failed.");
                    return;
                }
                rosterSize = rosterTemp.size();     //throws exception if still null
                if (!dl.cancel)
                    rosterJmriWeb = (HashMap<String, RosterEntry>) rosterTemp.clone();
            } catch (Exception e) {
                throw new IOException();
            }
            Log.d(applicationName, "t_a: Loaded " + rosterSize + " entries from /roster/?format=xml.");
        }
    }

//    public class DownloadMetaTask extends DownloadDataTask {
//        @SuppressWarnings("unchecked")
//        @Override
//        void runMethod(Download dl) throws IOException {
//            String metaUrl = createUrl("json/metadata");
//            if (metaUrl == null || metaUrl.equals("") || dl.cancel)
//                return;
//            Log.d(applicationName, "t_a: Background loading metadata from " + metaUrl);
//
//            HttpClient Client = new DefaultHttpClient();
//            HttpGet httpget = new HttpGet(metaUrl);
//            ResponseHandler<String> responseHandler = new BasicResponseHandler();
//            String jsonResponse;
//            jsonResponse = Client.execute(httpget, responseHandler);
//            Log.d(applicationName, "t_a: Raw metadata retrieved: " + jsonResponse);
//
//            HashMap<String, String> metadataTemp = new HashMap<>();
//            try {
//                JSONArray ja = new JSONArray(jsonResponse);
//                for (int i = 0; i < ja.length(); i++) {
//                    JSONObject j = ja.optJSONObject(i);
//                    String metadataName = j.getJSONObject("data").getString("name");
//                    String metadataValue = j.getJSONObject("data").getString("value");
//                    metadataTemp.put(metadataName, metadataValue);
//                }
//            } catch (JSONException e) {
//                Log.d(applicationName, "t_a: exception trying to retrieve json metadata.");
//            } catch (Exception e) {
//                throw new IOException();
//            }
//            if (metadataTemp.size() == 0) {
//                Log.d(applicationName, "t_a: did not retrieve any json metadata entries.");
//            } else {
//                jmriMetadata = (HashMap<String, String>) metadataTemp.clone();  // save the metadata in global variable
//                Log.d(applicationName, "t_a: Loaded " + jmriMetadata.size() + " metadata entries from json web server.");
//            }
//        }
//    }

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
                        Log.d(applicationName, "t_a: sendMsg - message - ROSTER_UPDATE");
                        sendMsg(comm_msg_handler, message_type.ROSTER_UPDATE);      //send message to alert other activities
                    }
                } catch (Throwable t) {
                    Log.d(applicationName, "t_a: Data fetch failed: " + t.getMessage());
                }

                // background load of Data completed
                finally {
                    if (cancel) {
                        Log.d(applicationName, "t_a: Data fetch cancelled");
                    }
                }
                Log.d(applicationName, "t_a: Data fetch ended");
            }

            Download() {
                super("DownloadData");
            }
        }

        public void get() {
            if (dl != null) {
                dl.cancel = true;   // try to stop any update that is in progress on old download thread
            }
            dl = new Download();    // create new download thread
            dl.start();             // start an update
        }

        public void stop() {
            if (dl != null) {
                dl.cancel = true;
            }
        }
    }

    // get the roster name from address string 123(L).  Return input string if not found in roster or in consist
    public String getRosterNameFromAddress(String addr_str, boolean returnBlankIfNotFound) {
        boolean prefRosterRecentLocoNames = prefs.getBoolean("prefRosterRecentLocoNames",
                getResources().getBoolean(R.bool.prefRosterRecentLocoNamesDefaultValue));

        if (prefRosterRecentLocoNames) {
            if ((roster_entries != null) && (!roster_entries.isEmpty())) {
                for (String rosterName : roster_entries.keySet()) {  // loop thru roster entries,
                    String rosterEntryName = roster_entries.get(rosterName);
                    if ( (rosterEntryName!=null) && (rosterEntryName.equals(addr_str)) ) { //looking for value = input parm
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
        if ((roster_entries != null) && (!roster_entries.isEmpty())) {
            String rslt = roster_entries.get(searchName);
            if (rslt != null) {
                if (!rslt.isEmpty()) {
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
        if ((consist_entries != null) && (!consist_entries.isEmpty())) {
            return consist_entries.get(addr_str);  //consists are keyed by address "123(L)"
        }
        return null;
    }

    public int getWhichThrottleFromAddress(String addr_str, int startAt) {
//        if (addr_str.charAt(0) == 'S' || addr_str.charAt(0) == 'L') { //convert from S123 to 123(S) formatting if needed
//            addr_str = cvtToAddrP(addr_str);
//        }
        // assumes "S123" "L333" type address, not "123(S)"
        Consist con;
        int whichThrottle = -1;
        boolean found = false;
        if (consists.length>=startAt) {
            for (int i=startAt; i<consists.length; i++) {
                con = mainapp.consists[i];
                for (Consist.ConLoco l : con.getLocos()) {
                    if (l.getAddress().equals(addr_str)) {
                        found = true;
                        whichThrottle = i;
                        break;
                    }
                }
                if (found) break;
            }
        }
        return whichThrottle;
    }


    //convert a string of form L123 to 123(L)
    public String cvtToAddrP(String addr_str) {
        return addr_str.substring(1) + "(" + addr_str.charAt(0) + ")";
    }

    //convert a string of form 123(L) to L123
    public String cvtToLAddr(String addr_str) {
        String[] sa = splitByString(addr_str, "(");  //split on the "("
        if (sa.length == 2) {
            return sa[1].charAt(0) + sa[0]; //move length to front and return format L123
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
        } else if ( (serverType.equals("DCC-EX")) && (isDCCEX) ) {
            WiThrottle_Msg_Interval = 100; //increase the interval for DCC-EX
        }
    }

    public String getServerDescription() {
        return this.serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }

    public String getAboutInfo() {
        return getAboutInfo(true);
    }
    @SuppressLint("DefaultLocale")
    public String getAboutInfo(boolean html) {
        String sHtml = "<span>";
        String s = "";
        // device info
        sHtml += "About: " + String.format("<small>OS: </small><b>%s</b> <small>SDK: </small><b>%s</b>", android.os.Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
        s += "About: " + String.format("OS: %s SDK: %s", android.os.Build.VERSION.RELEASE, Build.VERSION.SDK_INT);

        sHtml += String.format("<small>, DeviceID: </small><b>%s</b>", getFakeDeviceId());
        s += String.format(", DeviceID: %s", getFakeDeviceId());

        String ssid = client_ssid;
        PermissionsHelper phi = PermissionsHelper.getInstance();
        if ( (!mainapp.clientLocationServiceEnabled) || (!phi.isPermissionGranted(this, PermissionsHelper.ACCESS_FINE_LOCATION))) {
            ssid = "[DECLINED]";
        }

        if (client_address_inet4 != null) {
            sHtml += String.format("<small>, IP: </small><b>%s</b>", client_address_inet4.toString().replaceAll("/", ""));
            sHtml += String.format("<small>, SSID: </small><b>%s</b> <small>Net: </small><b>%s</b>", ssid, client_type);
            s += String.format(", IP: %s", client_address_inet4.toString().replaceAll("/", ""));
            s += String.format(", SSID: %s Net: %s", ssid, client_type);
        }

        // ED version info
        sHtml += "<small>, EngineDriver: </small><b>" + appVersion + "</b>";
        s += ", EngineDriver: " + appVersion;
        if (getHostIp() != null) {
            // WiT info
            if (getWithrottleVersion() != 0.0) {
                sHtml += "<small>, Protocol: </small>";
                s += ", Protocol: ";
                if (!isDCCEX) {
                    sHtml += "<b>WiThrottle</b> v</small><b>" + getWithrottleVersion() +"</b>";
                    sHtml += String.format("<small>, Heartbeat: </small><b>%dms</b>", heartbeatInterval);
                    s += "WiThrottle v" + getWithrottleVersion();
                    s += String.format(", Heartbeat: %dms", heartbeatInterval);
                } else {
                    sHtml += "<b>DCC-EX</b>";
                    s += "DCC-EX";
                }
            }
            sHtml += String.format("<small>, Host: </small><b>%s</b>", getHostIp() );
            sHtml += String.format("<small> Port: </small><b>%s</b>", connectedPort);
            s += String.format(", Host: %s", getHostIp() );
            s += String.format(" Port: %s", connectedPort);
            //show server type and description if set
            String sServer;
            if (getServerDescription().contains(getServerType())) {
                sServer = getServerDescription();
            } else {
                sServer = getServerType() + " " + getServerDescription();
            }
            if (!sServer.isEmpty()) {
                sHtml += "<small>, Server: </small><b>" + sServer + "</b>";
                s += ", Server: " + sServer;
            }
        } else {
            sHtml += "<small><br/>Not Connected</small>";
            s += " Not Connected";
        }
        sHtml += "</span>";
        return (html) ? sHtml : s;
    }

    //reinitialize statics in activities as required to be ready for next launch
    public static void reinitStatics() {
        throttle.initStatics();
        throttle_original.initStatics();
        throttle_simple.initStatics();
        web_activity.initStatics();
    }

    //initialize shared variables
    public void initShared() {
        withrottle_version = 0.0;
        web_server_port = 0;
        host_ip = null;
        setServerType("");
        setServerDescription("");
        power_state = null;
        to_system_names = null;
        to_user_names = null;
        to_state_names = null;
        turnout_states = new HashMap<String, String>();
        routeStates = null;
        routeSystemNames = null;
        rt_user_names = null;
        routeStateNames = null;
        routeDccexLabels = null;
        routeDccexStates = null;

        prefUseDccexProtocol = prefs.getString("prefUseDccexProtocol", mainapp.getResources().getString(R.string.prefUseDccexProtocolDefaultValue));
        prefAlwaysUseFunctionsFromServer = prefs.getBoolean("prefAlwaysUseFunctionsFromServer", mainapp.getResources().getBoolean(R.bool.prefAlwaysUseFunctionsFromServerDefaultValue));
        mainapp.isDCCEX = prefUseDccexProtocol.equals("Yes") || mainapp.isDCCEX;   // force it if the preference is set

        dccexVersionString = "";
        dccexEmergencyStopState = dccex_emergency_stop_state_type.RESUMED;

        DCCEXlistsRequested = -1;
        dccexRosterRequested = false;
//        dccexRosterFullyReceived = false;

        dccexTurnoutsBeingProcessed = false;
        dccexTurnoutsRequested = false;
        dccexTurnoutsFullyReceived = false;

        dccexRoutesBeingProcessed = false;
        dccexScreenIsOpen = false;
        witScreenIsOpen = false;

        dccexRosterString = "";
        dccexTurnoutString = "";
        dccexRouteString = "";

        try {
            consists = new Consist[maxThrottles];
            function_labels = (LinkedHashMap<Integer, String>[]) new LinkedHashMap<?, ?>[maxThrottles];
            function_states = new boolean[maxThrottles][MAX_FUNCTION_NUMBER+1];

            for (int i = 0; i < maxThrottles; i++) {
                consists[i] = new Consist();
                function_labels[i] = new LinkedHashMap<Integer, String>();
                function_states[i] = new boolean[MAX_FUNCTION_NUMBER+1]; // also allocated in onCreate() ???

                dccexLastKnownSpeed[i] = 0;
                dccexLastKnownDirection[i] = 1;
            }

            consist_entries = Collections.synchronizedMap(new LinkedHashMap<String, String>());
            roster_entries = Collections.synchronizedMap(new LinkedHashMap<String, String>());
        } catch (Exception e) {
            Log.d(applicationName, "t_a: initShared object create exception");
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
        if (input == null || input.isEmpty()) return new String[]{input};

        int size = 0;
        String temp = input;

        // count entries
        while (!temp.isEmpty()) {
            size++;
            int index = temp.indexOf(divider);
            if (index < 0) break;    // break not found
            temp = temp.substring(index + divider.length());
            if (temp.isEmpty()) {  // found at end
                size++;
                break;
            }
        }

        String[] result = new String[size];

        // find entries
        temp = input;
        size = 0;
        while (!temp.isEmpty()) {
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
        MenuItem menuItem = menu.findItem(R.id.powerLayoutButton);
        if (menuItem == null) return;

        if (prefs.getBoolean("prefShowLayoutPowerButton", false) && (power_state != null)) {
            actionBarIconCountThrottle++;
            actionBarIconCountRoutes++;
            actionBarIconCountTurnouts++;
            menuItem.setVisible(true);
        } else {
            menuItem.setVisible(false);
        }
    }

    public void powerControlNotAllowedDialog(Menu menu) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(getApplicationContext().getResources().getString(R.string.powerWillNotWorkTitle));
        b.setMessage(getApplicationContext().getResources().getString(R.string.powerWillNotWork));
        b.setCancelable(true);
        b.setNegativeButton("OK", null);
        AlertDialog alert = b.create();
        alert.show();
        displayPowerStateMenuButton(menu);
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
                    } else if ("DCCESP".equals(getServerType())) {
                        item.setTitle(R.string.dccesp_settings);
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
        String prefGamePadType = prefs.getString("prefGamePadType", getApplicationContext().getResources().getString(R.string.prefGamePadTypeDefaultValue));
        boolean result;

        if (menu != null) {
            boolean any = false;

            TypedArray gamepad_test_menu_resource_ids = getResources().obtainTypedArray(R.array.gamepad_test_menu_resource_ids);

            for (int i = 1; i <= 6; i++) {
                MenuItem item = menu.findItem(gamepad_test_menu_resource_ids.getResourceId(i-1,0));

                result = i <= gamepadCount;

                if (item != null) {
                    if ((!prefGamePadType.equals("None")) && (result)) {
                        any = true;
                        item.setVisible(true);
                    } else {
                        item.setVisible(false);
                    }
                }
            }
            gamepad_test_menu_resource_ids.recycle();

            if (any) {
                menu.findItem(R.id.gamepad_test_menu).setVisible(true);
                menu.findItem(R.id.gamepad_test_reset).setVisible(true);
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
                item.setVisible(isRouteControlAllowed());
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
                item.setVisible(isPowerControlAllowed());
            }
        }
    }

    public void setDCCEXMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.dcc_ex_mnu);
            if (item != null) {
                item.setVisible(isDCCEX);
            }
        }
    }

    public void setWithrottleCvProgrammerMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.withrottle_cv_programmer_mnu);
            if (item != null) {
                item.setVisible((!isDCCEX)
                        && (prefs.getBoolean("prefShowWitPom",  getResources().getBoolean(R.bool.prefShowWitPomDefaultValue)) ));
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
                item.setVisible(isTurnoutControlAllowed());
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
                item.setVisible(item.getItemId() == R.id.overflowMenu);

                if (item.hasSubMenu()) { // really only R.id.overflowMenu
                    SubMenu subMenu = item.getSubMenu();
                    if (subMenu != null) {
                        for (int j = 0; j < subMenu.size(); j++) {
                            MenuItem subMenuItem = subMenu.getItem(j);
                            subMenuItem.setVisible(subMenuItem.getItemId() == R.id.timer_mnu);
                        }
                    }
                }
            }
        } else {
            menu.findItem(R.id.overflowMenu).setVisible(true);
            menu.findItem(R.id.settings_mnu).setVisible(true);
            setPowerMenuOption(menu);
            mainapp.setDCCEXMenuOption(menu);
            mainapp.setWithrottleCvProgrammerMenuOption(menu);
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
        boolean rslt = false;
        if (web_server_port > 0) {
            rslt = true;
        } else if (defaultWebViewURL != null && defaultWebViewURL.toLowerCase().startsWith("http")) {
            rslt = true;
        }
        return rslt;
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
     * based on setting of routeStateNames for now
     * this hides/shows menu options and activities
     *
     * @return true or false
     */
    public boolean isRouteControlAllowed() {
        return (routeStateNames != null);
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

    // used for the menu items that appear on almost all activities
    public void refreshCommonOverflowMenu(Menu menu) {
        refreshCommonOverflowMenu(menu, true);
    }
    public void refreshCommonOverflowMenu(Menu menu, boolean includePower) {
        if (menu == null) return;

        MenuItem emergencyStopButtonMenuItem = menu.findItem(R.id.emergency_stop_button);
        MenuItem flashlightButtonMenuItem = menu.findItem(R.id.flashlight_button);
        MenuItem powerLayoutButtonMenuItem = menu.findItem(R.id.powerLayoutButton);

        boolean refreshRequired = false;

        if (emergencyStopButtonMenuItem != null) {
            displayEmergencyStopActionViewButton(menu);
            setEmergencyStopStateActionViewButton(menu, emergencyStopButtonMenuItem);
        }

        displayFlashlightMenuButton(menu);
        if (flashlightButtonMenuItem == null) {
            refreshRequired = true;
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                alert_activities(message_type.REFRESH_OVERFLOW_MENU, "");  //send response to running activities
            }, 100);
        } else {
            setFlashlightActionViewButton(menu, flashlightButtonMenuItem);
        }

        if (!includePower) return;

        displayPowerStateMenuButton(menu);
        if (powerLayoutButtonMenuItem == null) {
            if (!refreshRequired) { // don't bother if we already requested it for the flashlight
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    alert_activities(message_type.REFRESH_OVERFLOW_MENU, "");  //send response to running activities
                }, 100);
            }
        } else {
            setPowerStateActionViewButton(menu, powerLayoutButtonMenuItem);
        }
    }

    public void setPowerStateActionViewButton(Menu menu, MenuItem menuItem) {
        if (!prefs.getBoolean("prefShowLayoutPowerButton", false)) return;

        if ( (menu == null) ||  (menuItem  == null)) {
//            // the menu or button is not available yet. Force an update request to the get it to update ASAP
//            sendMsgDelay(comm_msg_handler, 100, message_type.POWER_STATE_REQUEST);
            return;
        }

        TypedValue outValue = new TypedValue();
        TypedArray power_button_state_attr_ids = getResources().obtainTypedArray(R.array.power_button_state_attr_ids);

        int powerState = (power_state != null) ? Integer.parseInt(power_state) : 2;  // default to unknown
        if ( (mainapp.isDCCEX) && (powerState == 2) ) powerState = 3; // mixed - part on, part off

        theme.resolveAttribute(power_button_state_attr_ids.getResourceId(powerState,0), outValue, true);
        power_button_state_attr_ids.recycle();

        setActionBarButtonImage(menuItem, outValue.resourceId);
    }

    public void displayEmergencyStopActionViewButton(Menu menu) {
        MenuItem mi = menu.findItem(R.id.emergency_stop_button);
        if (mi == null) return;

        if (prefs.getBoolean("prefShowEmergencyStopButton", false)) {
            actionBarIconCountThrottle++;
            actionBarIconCountRoutes++;
            actionBarIconCountTurnouts++;
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }
    }

    public void setEmergencyStopStateActionViewButton(Menu menu, MenuItem menuItem) {
        if (!prefs.getBoolean("prefShowEmergencyStopButton", false)) return;

        if ( (menu == null) ||  (menuItem  == null)) return; // the menu or button is not available yet.

        TypedValue outValue = new TypedValue();
        TypedArray estop_button_state_attr_ids = getResources().obtainTypedArray(R.array.estop_button_state_attr_ids);

        theme.resolveAttribute(estop_button_state_attr_ids.getResourceId(dccexEmergencyStopState,0), outValue, true);
        estop_button_state_attr_ids.recycle();

        setActionBarButtonImage(menuItem, outValue.resourceId);
    }

    public void sendEStopMsg() {
        if ( (mainapp.isDCCEX) && (prefDccexEmergencyStopPauseResume) ) {
            if (mainapp.dccexEmergencyStopState == dccex_emergency_stop_state_type.RESUMED) {
                sendMsg(comm_msg_handler, message_type.DCCEX_ESTOP_PAUSE, "");
            } else {
                sendMsg(comm_msg_handler, message_type.DCCEX_ESTOP_RESUME, "");
            }
        } else {
            for (int i = 0; i < maxThrottlesCurrentScreen; i++) {
                if (consists != null && consists[i] != null && consists[i].isActive()) {
                    sendMsg(comm_msg_handler, message_type.ESTOP, "", i);
                    EStopActivated = true;
                    threaded_application.extendedLogging(activityName + ":  sendEStopMsg(): EStop sent to server for throttle " + i);
                }
            }
        }
    }

    public void sendEStopOneThrottleMsg(int whichThrottle) {
        if (consists != null && consists[whichThrottle] != null && consists[whichThrottle].isActive()) {
            sendMsg(comm_msg_handler, message_type.ESTOP_ONE_THROTTLE, "", whichThrottle);
            EStopActivated = true;
            Log.d(applicationName, activityName + ": sendEStopOneThrottleMsg(): EStop sent to server for throttle " + whichThrottle);
        }
    }

    // forward a message to all running activities
    public void alert_activities(int msgType, String msgBody) {
        Log.d(threaded_application.applicationName, activityName + ": alert_activities(): " + msgType + " : " + msgBody);

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
            sendMsg(dcc_ex_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(advancedConsistToolMesssgeHandler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(withrottle_cv_programmer_msg_handler, msgType, msgBody);
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
        try {
            sendMsg(device_sounds_settings_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
    }

    /** @noinspection UnusedReturnValue*/
    public boolean sendMsg(Handler h, int msgType) {
        return sendMsgDelay(h, 0, msgType, "", 0, 0);
    }

    /** @noinspection UnusedReturnValue*/
    public boolean sendMsg(Handler h, int msgType, String msgBody) {
        return sendMsgDelay(h, 0, msgType, msgBody, 0, 0);
    }

    /** @noinspection UnusedReturnValue*/
    public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1) {
        return sendMsgDelay(h, 0, msgType, msgBody, msgArg1, 0);
    }

    /** @noinspection UnusedReturnValue*/
    public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1, int msgArg2) {
        return sendMsgDelay(h, 0, msgType, msgBody, msgArg1, msgArg2);
    }

    /** @noinspection UnusedReturnValue*/
    public boolean sendMsgDelay(Handler h, long delayMs, int msgType, int msgArg1) {
        return sendMsgDelay(h, delayMs, msgType, "", msgArg1, 0);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType) {
        return sendMsgDelay(h, delayMs, msgType, "", 0, 0);
    }

    /** @noinspection UnusedReturnValue*/
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
            Log.d(applicationName, "t_a: ignoring web url for MRC");
        }
        if ( (port > 0) || (defaultUrl.toLowerCase().startsWith("http")) ) {
            if (defaultUrl.toLowerCase().startsWith("http")) { //if url starts with http, use it as is
                url = defaultUrl;
            } else { //, else prepend server name and port and slash if needed
                url = "http://" + host_ip + ":" + port + (defaultUrl.startsWith("/") ? "" : "/") + defaultUrl;
            }
        }
        return url;
    }

//    // build a full uri
//    // returns: full uri    if webServerPort is valid
//    //          null        otherwise
//    public String createUri() {
//        String uri = "";
//        int port = web_server_port;
//        if (port > 0) {
//            uri = "ws://" + host_ip + ":" + port + "/json/";
//        }
//        return uri;
//    }

    public int getSelectedTheme() {
        return getSelectedTheme(false);
    }

    public int getSelectedTheme(boolean isPreferences) {
        String prefTheme = getCurrentTheme();
        if (!isPreferences) {  // not a preferences activity
            return switch (prefTheme) {
                case "Black" -> R.style.app_theme_black;
                case "Outline" -> R.style.app_theme_outline;
                case "Ultra" -> R.style.app_theme_ultra;
                case "Colorful" -> R.style.app_theme_colorful;
                case "Neon" -> R.style.app_theme_neon;
                default -> R.style.app_theme;
            };
        } else {
            return switch (prefTheme) {
                case "Black", "Outline", "Ultra", "Neon", "Colorful" ->
                        R.style.app_theme_black_preferences;
//                case "Colorful ->
//                    R.style.app_theme_colorful;
                default -> R.style.app_theme_preferences;
            };
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
        Log.d(applicationName, "t_a: applyTheme: " + selectedTheme);
        theme = activity.getTheme();

    }


    /**
     * Retrieve the currently configure theme from preferences
     *
     * @return a String representation of the selected theme
     */
    public String getCurrentTheme() {
        return prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
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
        String to = prefs.getString("prefThrottleOrientation",
                getApplicationContext().getResources().getString(R.string.prefThrottleOrientationDefaultValue));
        if ((to.equals("Auto-Web")) && (!webMenuSelected)) {
            int orient = activity.getResources().getConfiguration().orientation;
            if ((isWeb && orient == ORIENTATION_PORTRAIT)
                    || (!isWeb && orient == Configuration.ORIENTATION_LANDSCAPE))
                return (false);
        } else if (isWeb) {
            to = prefs.getString("prefWebOrientation",
                    getApplicationContext().getResources().getString(R.string.prefWebOrientationDefaultValue));
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
            Log.e(applicationName, "t_a: setActivityOrientation: Unable to change Orientation: " + e.getMessage());
        }

        webMenuSelected = false;  // reset after each check
        return true;
    }

    public void checkExit(final Activity activity) {
        Log.d(applicationName, "t_a: checkExit(1): ");
        boolean prefDoubleBackButtonToExit = prefs.getBoolean("prefDoubleBackButtonToExit", getResources().getBoolean(R.bool.prefDoubleBackButtonToExitDefaultValue));
        if (!prefDoubleBackButtonToExit) {
            checkAskExit(activity, false);
        } else {
            long time = System.currentTimeMillis();
            if ( (time==0) || ((time - exitDoubleBackButtonInitiated) > 3000)) {
                exitDoubleBackButtonInitiated = time;
                safeToast(R.string.toastDoubleBackButtonToExit, Toast.LENGTH_SHORT);
            } else {
                exitConfirmed = true;
                exitDoubleBackButtonInitiated = 0;
                sendMsg(comm_msg_handler, message_type.SHUTDOWN, "");  //trigger disconnect / shutdown sequence
                buttonVibration();
            }
        }
    }

//    public void checkExit(final Activity activity, boolean forceFastDisconnect) {
//        Log.d(applicationName, "t_a: checkExit(2): ");
//        boolean  prefDoubleBackButtonToExit = prefs.getBoolean("prefDoubleBackButtonToExit", getResources().getBoolean(R.bool.prefDoubleBackButtonToExitDefaultValue));
//        if (!prefDoubleBackButtonToExit) {
//            checkAskExit(activity, forceFastDisconnect);
//        } else {
//            long time = System.currentTimeMillis();
//            if ( (time==0) || ((time - exitDoubleBackButtonInitiated) > 3000)) {
//                exitDoubleBackButtonInitiated = time;
//                safeToast(R.string.toastDoubleBackButtonToExit, Toast.LENGTH_SHORT);
//            } else {
//                exitConfirmed = true;
//                exitDoubleBackButtonInitiated = 0;
//                sendMsg(comm_msg_handler, message_type.SHUTDOWN, "", 1);  //trigger fast disconnect / shutdown sequence
//                buttonVibration();
//            }
//        }
//    }

    public void checkAskExit(final Activity activity) {
        checkAskExit(activity, false);
    }
    // prompt for Exit
    // must be called on the UI thread
    public void checkAskExit(final Activity activity, boolean forceFastDisconnect) {
        exitDoubleBackButtonInitiated = 0;
        final AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(R.string.exit_title);
        b.setMessage(R.string.exit_text);
        b.setCancelable(true);
        b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(applicationName, "t_a: checkAskExit(): onClick() ");
                exitConfirmed = true;
                if (!forceFastDisconnect) {
                    sendMsg(comm_msg_handler, message_type.SHUTDOWN, "");  //trigger disconnect / shutdown sequence
                } else {
                    sendMsg(comm_msg_handler, message_type.SHUTDOWN, "", 1);  //trigger fast disconnect / shutdown sequence
                }
                buttonVibration();
            }
        });
        b.setNegativeButton(R.string.no, null);
        AlertDialog alert = b.create();
        alert.show();

        // find positiveButton and negativeButton
        Button positiveButton = alert.findViewById(android.R.id.button1);
        Button negativeButton = alert.findViewById(android.R.id.button2);
        // then get their parent ViewGroup
        if ( (positiveButton != null) && (negativeButton != null) ) {
            ViewGroup buttonPanelContainer;
            buttonPanelContainer = (ViewGroup) positiveButton.getParent();
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
    }

    @Override
    protected void attachBaseContext(Context base) {
        languageCountry = Locale.getDefault().getLanguage();
        if (!Locale.getDefault().getCountry().isEmpty()) {
            languageCountry = languageCountry + "_" + Locale.getDefault().getCountry();
        }
        super.attachBaseContext(LocaleHelper.onAttach(base, languageCountry));
    }

    public void setFlashlightActionViewButton(Menu menu, MenuItem menuItem) {
        if (menu == null) return;

        TypedValue outValue = new TypedValue();
        if (flashState) {
            theme.resolveAttribute(R.attr.ed_flashlight_on_button, outValue, true);
            menu.findItem(R.id.flashlight_button).setTitle(R.string.flashlightStateOn);
        } else {
            theme.resolveAttribute(R.attr.ed_flashlight_off_button, outValue, true);
            menu.findItem(R.id.flashlight_button).setTitle(R.string.flashlightStateOff);
        }
        setActionBarButtonImage(menuItem, outValue.resourceId);
    }

    public void setEsuMc2KnobButton(Menu menu, MenuItem menuItem, boolean isEsuMc2KnobEnabled) {
        if (menu == null) return;

        TypedValue outValue = new TypedValue();
        if (isEsuMc2KnobEnabled) {
            theme.resolveAttribute(R.attr.ed_esu_mc2_knob_on_button, outValue, true);
        } else {
            theme.resolveAttribute(R.attr.ed_esu_mc2_knob_off_button, outValue, true);
        }
        setActionBarButtonImage(menuItem, outValue.resourceId);
    }

    // only works on menu items with - app:actionLayout="@layout/..."
    public void setActionBarButtonImage(MenuItem menuItem, int resourceId) {
        View view = menuItem.getActionView();
        if (view == null) return;
        ViewGroup button = view.findViewById(menuItem.getItemId());
        if (button == null) return;
        ImageView image = (ImageView) button.getChildAt(0);
        image.setImageResource(resourceId);
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
                && !((kidsTimerRunning == kids_timer_action_type.RUNNING) || (kidsTimerRunning == kids_timer_action_type.ENABLED))) {
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
        String prefWebViewLocation = prefs.getString("prefWebViewLocation", defaultWebViewLocation);

        if ((prefs.getBoolean("prefWebViewButton", false))
                && (!prefWebViewLocation.equals(defaultWebViewLocation))
                && (currentScreenSupportsWebView)) {
            actionBarIconCountThrottle++;
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }
    }

    public void displayDeviceSoundsThrottleButton(Menu menu) {
        MenuItem mi;
        mi = menu.findItem(R.id.device_sounds_button);
        if (mi != null) {
            boolean rslt = prefDeviceSoundsButton;
            if (rslt) {
                actionBarIconCountThrottle++;
                mi.setVisible(true);
            } else {
                mi.setVisible(false);
            }
        }
    }

    public void displayDccExButton(Menu menu) {
        MenuItem mi;
        mi = menu.findItem(R.id.dcc_ex_button);
        if (mi != null) {
            boolean rslt = prefActionBarShowDccExButton;
            if ( (rslt) && (isDCCEX) ) {
                actionBarIconCountThrottle++;
                mi.setVisible(true);
            } else {
                mi.setVisible(false);
            }
        }
    }

    public void displayThrottleButton(Menu menu) {
        MenuItem mi;
        mi = menu.findItem(R.id.throttle_button_mnu);
        if (mi != null) {
            boolean rslt = prefActionBarShowThrottleButton;
            if (rslt) {
                actionBarIconCountThrottle++;
                mi.setVisible(true);
            } else {
                mi.setVisible(false);
            }
        }
    }

    public void displayTurnoutsButton(Menu menu) {
        MenuItem mi;
        mi = menu.findItem(R.id.turnouts_button);
        if (mi != null) {
            boolean rslt = prefActionBarShowTurnoutsButton && isTurnoutControlAllowed();
            if (rslt) {
                actionBarIconCountThrottle++;
                mi.setVisible(true);
            } else {
                mi.setVisible(false);
            }
        }
    }

    public void displayRoutesButton(Menu menu) {
        MenuItem mi;
        mi = menu.findItem(R.id.routes_button);
        if (mi != null) {
            boolean rslt = prefActionBarShowRoutesButton && isRouteControlAllowed();
            if (rslt) {
                actionBarIconCountThrottle++;
                mi.setVisible(true);
            } else {
                mi.setVisible(false);
            }
        }
    }

    public void displayWebButton(Menu menu) {
        MenuItem mi;
        mi = menu.findItem(R.id.web_button);
        if (mi != null) {
            boolean rslt = prefActionBarShowWebButton;
            if ( (isWebAllowed()) && (rslt) ) {
                actionBarIconCountThrottle++;
                mi.setVisible(true);
            } else {
                mi.setVisible(false);
            }
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
        if (menu == null) return;

        MenuItem mi = menu.findItem(R.id.throttle_switch_button);
        if (mi == null) return;

        if ( (prefs.getBoolean("prefThrottleSwitchButtonDisplay", false))
            && (mainapp.throttleSwitchAllowed) ) {
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
        return flashlight.isFlashlightAvailable(getApplicationContext());
    }

    public void toggleFlashlightActionView(Activity activity, Menu menu, MenuItem menuItem) {
        if (flashState) {
            flashlight.setFlashlightOff();
            flashState = false;
        } else {
            flashState = flashlight.setFlashlightOn(mainapp, activity);
        }
        setFlashlightActionViewButton(menu, menuItem);
    }

    public int Numeralise(String value) {
        return switch (value) {
            case "Two" -> 2;
            case "Three" -> 3;
            case "Four" -> 4;
            case "Five" -> 5;
            case "Six" -> 6;
//            case "One" -> 1;
            default -> 1;
        };
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
                Log.d(threaded_application.applicationName, "t_a: throttleCharToInt: no match for argument " + cWhichThrottle);
                break;
        }
        if (val > maxThrottlesCurrentScreen)
            Log.d(threaded_application.applicationName, "t_a: throttleCharToInt: argument exceeds max number of throttles for current screen " + cWhichThrottle);
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
        String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);

        String newValue = currentValue;
        //if name is blank or the default name, make it unique
        if (currentValue.isEmpty() || currentValue.equals(defaultName)) {
            deviceId = mainapp.getFakeDeviceId();
            if (MobileControl2.isMobileControl2()) {
                // Change default name for ESU MCII
                defaultName = getApplicationContext().getResources().getString(R.string.prefEsuMc2ThrottleNameDefaultValue);
            }
            newValue = defaultName + " " + deviceId;
        }
        prefs.edit().putString("prefThrottleName", newValue).commit();  //save new name to prefs
        return newValue;
    }

    public void playTone(int tone) {
        if (tg==null) {
            tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,100);
        }
        tg.startTone(tone);
    }

    public void vibrate(int duration) {
        //we need vibrate permissions, otherwise do nothing
        PermissionsHelper phi = PermissionsHelper.getInstance();
        if (phi.isPermissionGranted(getApplicationContext(), PermissionsHelper.VIBRATE)) {
            try {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= 26) {
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
                if (Build.VERSION.SDK_INT >= 26) {
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


    // used for Instructional Toasts.  They can be blocked by preference
    public void safeToastInstructional(final int resourceId, final int length) {
        safeToastInstructional(getApplicationContext().getResources().getString(resourceId), length);
    }
    public void safeToastInstructional(final String msg_txt, final int length) {
        if (!prefHideInstructionalToasts) {
            safeToast(msg_txt, length);
        }
    }
    /** @noinspection SameReturnValue*/
    public boolean safeToastInstructionalShowOnce(final int resourceId, final int length, boolean shownBefore) {
        return safeToastInstructionalShowOnce(getApplicationContext().getResources().getString(resourceId), length, shownBefore);
    }
    /** @noinspection SameReturnValue*/
    public boolean safeToastInstructionalShowOnce(final String msg_txt, final int length, boolean shownBefore) {
        if ((!prefHideInstructionalToasts) && (!shownBefore)) {
            safeToast(msg_txt, length);
        }
        return true;
    }

    //display msg using Toast() safely by ensuring Toast() is called from the UI Thread
    public void safeToast(final int resourceId, final int length) {
        safeToast(getApplicationContext().getResources().getString(resourceId), length);
    }
    public  void safeToast(final String msg_txt) {
        safeToast(msg_txt, Toast.LENGTH_SHORT);
    }
    public void safeToast(final String msg_txt, final int length) {
        Log.d(applicationName, "t_a: safeToast: " + msg_txt);
        //need to do Toast() on the main thread so create a handler
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg_txt, length).show();
            }
        });
    }

    public int getMaxThrottlesForScreen(String throttleScreenType) {
        return switch (throttleScreenType) {
            case throttle_screen_type.SIMPLE
                    -> max_throttles_current_screen_type.SIMPLE;
            case throttle_screen_type.VERTICAL
                    -> max_throttles_current_screen_type.VERTICAL;
            case throttle_screen_type.VERTICAL_LEFT, throttle_screen_type.VERTICAL_RIGHT
                    ->  max_throttles_current_screen_type.VERTICAL_LEFT_OR_RIGHT;
            case throttle_screen_type.TABLET_VERTICAL_LEFT
                    -> max_throttles_current_screen_type.VERTICAL_TABLET;
            case throttle_screen_type.SWITCHING
                    -> max_throttles_current_screen_type.SWITCHING;
            case throttle_screen_type.SWITCHING_LEFT, throttle_screen_type.SWITCHING_RIGHT
                    -> max_throttles_current_screen_type.SWITCHING_LEFT_OR_RIGHT;
            case throttle_screen_type.TABLET_SWITCHING_LEFT
                    -> max_throttles_current_screen_type.TABLET_SWITCHING;
            case throttle_screen_type.SWITCHING_HORIZONTAL
                    -> max_throttles_current_screen_type.SWITCHING_HORIZONTAL;
            case throttle_screen_type.BIG_LEFT, throttle_screen_type.BIG_RIGHT
                    -> max_throttles_current_screen_type.BIG_BUTTONS;
            case throttle_screen_type.SEMI_REALISTIC_LEFT
                    -> max_throttles_current_screen_type.SEMI_REALISTIC;
            default
                    -> max_throttles_current_screen_type.DEFAULT;
        };
    }

    public Intent getThrottleIntent() {
        Intent throttle;
        appIsFinishing = false;
        throttle = switch (prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault))) {
            case throttle_screen_type.SIMPLE
                    -> new Intent().setClass(this, throttle_simple.class);
            case throttle_screen_type.VERTICAL, throttle_screen_type.VERTICAL_LEFT,
                 throttle_screen_type.VERTICAL_RIGHT, throttle_screen_type.TABLET_VERTICAL_LEFT
                    -> new Intent().setClass(this, throttle_vertical_left_or_right.class);
            case throttle_screen_type.SWITCHING, throttle_screen_type.SWITCHING_LEFT,
                 throttle_screen_type.SWITCHING_RIGHT, throttle_screen_type.TABLET_SWITCHING_LEFT
                    -> new Intent().setClass(this, throttle_switching_left_or_right.class);
            case throttle_screen_type.SWITCHING_HORIZONTAL
                    -> new Intent().setClass(this, throttle_switching_horizontal.class);
            case throttle_screen_type.BIG_LEFT, throttle_screen_type.BIG_RIGHT
                    -> new Intent().setClass(this, throttle_big_buttons.class);
            case throttle_screen_type.SEMI_REALISTIC_LEFT
                    -> new Intent().setClass(this, throttle_semi_realistic.class);
            default
                    -> new Intent().setClass(this, throttle_original.class);
        };
        return throttle;
    }

    public String getNextThrottleLayout() {
//        Intent throttle;
        appIsFinishing = false;
        String prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        prefThrottleScreenType = switch (prefThrottleScreenType) {
            case throttle_screen_type.SIMPLE -> throttle_screen_type.VERTICAL;
            case throttle_screen_type.VERTICAL -> throttle_screen_type.VERTICAL_LEFT;
            case throttle_screen_type.VERTICAL_LEFT -> throttle_screen_type.VERTICAL_RIGHT;
            case throttle_screen_type.VERTICAL_RIGHT -> throttle_screen_type.TABLET_VERTICAL_LEFT;
            case throttle_screen_type.TABLET_VERTICAL_LEFT -> throttle_screen_type.SWITCHING;
            case throttle_screen_type.SWITCHING -> throttle_screen_type.SWITCHING_LEFT;
            case throttle_screen_type.SWITCHING_LEFT -> throttle_screen_type.SWITCHING_RIGHT;
            case throttle_screen_type.SWITCHING_RIGHT -> throttle_screen_type.TABLET_SWITCHING_LEFT;
            case throttle_screen_type.TABLET_SWITCHING_LEFT -> throttle_screen_type.SWITCHING_HORIZONTAL;
            case throttle_screen_type.SWITCHING_HORIZONTAL -> throttle_screen_type.BIG_LEFT;
            case throttle_screen_type.BIG_LEFT -> throttle_screen_type.BIG_RIGHT;
            case throttle_screen_type.BIG_RIGHT -> throttle_screen_type.SEMI_REALISTIC_LEFT;
            case throttle_screen_type.SEMI_REALISTIC_LEFT -> throttle_screen_type.DEFAULT;
            default -> throttle_screen_type.SIMPLE;
        };
        return prefThrottleScreenType;
    }


    public Intent getNextIntentInSwipeSequence(int currentScreen, float deltaX) {
        prefSwipeThoughTurnouts = prefs.getBoolean("prefSwipeThroughTurnouts",
                getResources().getBoolean(R.bool.prefSwipeThroughTurnoutsDefaultValue));
        prefSwipeThoughRoutes = prefs.getBoolean("prefSwipeThroughRoutes",
                getResources().getBoolean(R.bool.prefSwipeThroughRoutesDefaultValue));
        prefSwipeThoughWeb = prefs.getBoolean("prefSwipeThroughWeb",
                getResources().getBoolean(R.bool.prefSwipeThroughWebDefaultValue));

        boolean swipeRoutes = isRouteControlAllowed();  //also check the allowed flag
        boolean swipeTurnouts = isTurnoutControlAllowed();  //also check the allowed flag
        boolean swipeWeb = isWebAllowed();  //also check the allowed flag

        if ((!swipeRoutes) && (!swipeTurnouts) && (!swipeWeb)) {
            return null;
        }

        int nextScreen;
        if (deltaX <= 0.0) {
            nextScreen = currentScreen + 1;
            if ((nextScreen == screen_swipe_index_type.ROUTES)
                    && ((!isRouteControlAllowed()) || (!prefSwipeThoughRoutes))) {
                nextScreen++;
            }
            if ((nextScreen == screen_swipe_index_type.WEB)
                    && ((!isWebAllowed()) || (!prefSwipeThoughWeb))) {
                nextScreen++;
            }
            if ((nextScreen == screen_swipe_index_type.TURNOUTS)
                    && ((!isTurnoutControlAllowed()) || (!prefSwipeThoughTurnouts))) {
                nextScreen++;
            }
            if (nextScreen > screen_swipe_index_type.TURNOUTS) {
                nextScreen = screen_swipe_index_type.THROTTLE;
            }
        } else {
            nextScreen = currentScreen - 1;
            if (nextScreen < screen_swipe_index_type.THROTTLE) {
                nextScreen = screen_swipe_index_type.TURNOUTS;
            }
            if ((nextScreen == screen_swipe_index_type.TURNOUTS)
                    && ((!isTurnoutControlAllowed()) || (!prefSwipeThoughTurnouts))) {
                nextScreen--;
            }
            if ((nextScreen == screen_swipe_index_type.WEB)
                    && ((!isWebAllowed()) || (!prefSwipeThoughWeb))) {
                nextScreen--;
            }
            if ((nextScreen == screen_swipe_index_type.ROUTES)
                    && ((!isRouteControlAllowed()) || (!prefSwipeThoughRoutes))) {
                nextScreen--;
            }
        }

        return switch (nextScreen) {
            case screen_swipe_index_type.ROUTES -> new Intent().setClass(this, routes.class);
            case screen_swipe_index_type.TURNOUTS -> new Intent().setClass(this, turnouts.class);
            case screen_swipe_index_type.WEB -> new Intent().setClass(this, web_activity.class);
            default -> getThrottleIntent();
        };
    }

    /***
     * show appropriate messages on a restart that was forced by prefs
     *
     * @param prefForcedRestartReason the reason that the restart occurred
     * @return true if the activity should immediately launch Preferences
     */
    public boolean prefsForcedRestart(int prefForcedRestartReason) {
        switch (prefForcedRestartReason) {
            case restart_reason_type.AUTO_IMPORT:
            case restart_reason_type.IMPORT: {
                break;
            }
            case restart_reason_type.IMPORT_SERVER_MANUAL: {
                safeToast(getApplicationContext().getResources().getString(R.string.toastPreferencesImportServerManualSucceeded,
                                        prefs.getString("prefPreferencesImportFileName", "")), Toast.LENGTH_LONG);
                break;
            }
            case restart_reason_type.RESET: {
                safeToast(R.string.toastPreferencesResetSucceeded, Toast.LENGTH_LONG);
                break;
            }
            case restart_reason_type.THEME: {
                safeToast(R.string.toastPreferencesThemeChangeSucceeded, Toast.LENGTH_LONG);
                break;
            }
            case restart_reason_type.BACKGROUND: {
                safeToast(R.string.toastPreferencesBackgroundChangeSucceeded, Toast.LENGTH_LONG);
                break;
            }
            case restart_reason_type.THROTTLE_PAGE:
            case restart_reason_type.THROTTLE_SWITCH: {
                safeToast(R.string.toastPreferencesThrottleChangeSucceeded, Toast.LENGTH_LONG);
                break;
            }
            case restart_reason_type.LOCALE: {
                safeToast(R.string.toastPreferencesLocaleChangeSucceeded, Toast.LENGTH_LONG);
                break;
            }
            case restart_reason_type.IMPORT_SERVER_AUTO: {
                safeToast(getApplicationContext().getResources().getString(R.string.toastPreferencesImportServerAutoSucceeded,
                                    prefs.getString("prefPreferencesImportFileName", "")),
                        Toast.LENGTH_LONG);
                break;
            }
            case restart_reason_type.FORCE_WIFI: {
                safeToast(R.string.toastPreferencesChangedForceWiFi, Toast.LENGTH_LONG);
                break;
            }
            case restart_reason_type.APP_PUSHED_TO_BACKGROUND: {
                safeToast(R.string.notificationAppWasKilledInBackground, Toast.LENGTH_LONG);
                break;
            }
        }

        // include in this list if the Settings Activity should NOT be launched
        return ((prefForcedRestartReason != restart_reason_type.IMPORT_SERVER_AUTO)
                && (prefForcedRestartReason != restart_reason_type.BACKGROUND)
                && (prefForcedRestartReason != restart_reason_type.THROTTLE_SWITCH)
                && (prefForcedRestartReason != restart_reason_type.IMPORT_SERVER_MANUAL)
                && (prefForcedRestartReason != restart_reason_type.RESET)
                && (prefForcedRestartReason != restart_reason_type.AUTO_IMPORT)
                && (prefForcedRestartReason != restart_reason_type.FORCE_WIFI)
                && (prefForcedRestartReason != restart_reason_type.DEAD_ZONE)
                && (prefForcedRestartReason != restart_reason_type.APP_PUSHED_TO_BACKGROUND)
                && (prefForcedRestartReason != restart_reason_type.SHAKE_THRESHOLD));
    }

    public void writeSharedPreferencesToFileIfAllowed() {
        Log.d(applicationName, "t_a: writeSharedPreferencesToFileIfAllowed: start");
        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
        String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue));

        if (prefAutoImportExport.equals(auto_import_export_option_type.CONNECT_AND_DISCONNECT)) {
            if (!this.connectedHostName.isEmpty()) {
                String exportedPreferencesFileName = this.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";

                if (!exportedPreferencesFileName.equals(".ed")) {
                    ImportExportPreferences importExportPreferences = new ImportExportPreferences();
                    importExportPreferences.writeSharedPreferencesToFile(mainapp, getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
                }
                Log.d(applicationName, "t_a: writeSharedPreferencesToFileIfAllowed: done");
            } else {
                safeToast(R.string.toastConnectUnableToSavePref, Toast.LENGTH_LONG);
            }
        }
    }

    public void getServerNameFromWebServer() {
//        GetJsonFromUrl getJson = new GetJsonFromUrl(this);
//        getJson.execute("http://" + host_ip + ":" + web_server_port + "/json/railroad");
        webServerNameHasBeenChecked = true;
    }

    /* only DCC-EX supports the "Request Loco ID" feature at this time */
    public boolean supportsIDnGo() {
        return serverType.equals("DCC-EX");
    }

    public boolean supportsRoster() {
        //true if roster entries exist
        if ((roster_entries != null) && (!roster_entries.isEmpty())) return true;
        //always show roster panel for these entries
        return (serverType.equals("JMRI") || serverType.equals("MRC") || serverType.equals("DCC-EX"));
    }


    public void addLocoToRecents(ConLoco conLoco) {
        addLocoToRecents(conLoco, null);
    }

    /* add passed-in loco to Recent Locos list and store it */
    public void addLocoToRecents(ConLoco conLoco,  LinkedHashMap<Integer, String> functionLabelsMap) {
        // if we don't have external storage mounted, or permission to write it, just ignore, no prompt
//        if ((context.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
//                && (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
//                && (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))) {
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            return;
        }
        ImportExportPreferences importExportPreferences = new ImportExportPreferences();
        importExportPreferences.loadRecentLocosListFromFile(getApplicationContext());

        Integer locoAddress = conLoco.getIntAddress();
        Integer locoAddressSize = conLoco.getIntAddressLength();
        String locoName = conLoco.getFormatAddress();
        int locoSource = conLoco.getWhichSource();
        if (conLoco.getIsFromRoster()) {
            if ( (conLoco.getRosterName() != null) && (!conLoco.getRosterName().isEmpty())) {
                locoName = conLoco.getRosterName();
            } else {
                if (!conLoco.getDesc().isEmpty()) {
                    locoName = conLoco.getDesc();
                }
            }
            locoSource = source_type.ROSTER;
        }
        String keepFunctions = "";
        String functionLabels;
        if (locoSource == source_type.ROSTER) {
            functionLabels = threaded_application.packFunctionLabels(functionLabelsMap);
        } else {
            functionLabels = threaded_application.packFunctionLabels(mainapp.function_labels_default);
        }
        for (int i = 0; i < importExportPreferences.recentLocoAddressList.size(); i++) {
            if (locoAddress.equals(importExportPreferences.recentLocoAddressList.get(i))
                    && locoAddressSize.equals(importExportPreferences.recentLocoAddressSizeList.get(i))
                    && locoName.equals(importExportPreferences.recentLocoNameList.get(i))) {

                keepFunctions = importExportPreferences.recentLocoFunctionsList.get(i);
                if ( (i==0) && (!keepFunctions.isEmpty()) ) { return; } // if it already at the start of the list, don't do anything

                importExportPreferences.removeRecentLocoFromList(i);
                Log.d(applicationName, "t_a: addLocoToRecents: Loco '" + locoName + "' removed from Recents");
                break;
            }
        }

        if ( (!functionLabels.isEmpty()) && (!functionLabels.equals("]\\[")) ) {
            keepFunctions = functionLabels;  // restore functions from the previous value
        }
        // now append it to the beginning of the list
        importExportPreferences.addRecentLocoToList(0, locoAddress, locoAddressSize, locoName, source_type.ROSTER, keepFunctions);

        importExportPreferences.writeRecentLocosListToFile(getApplicationContext(), prefs);
        Log.d(applicationName, "t_a: Loco '" + locoName + "' added to Recents");
        importExportPreferences.writeThrottlesEnginesListToFile(mainapp, getApplicationContext(), mainapp.prefNumThrottles);

    }

    public int findLocoInRecents(Integer address, Integer size, String name) {
        int position = -1;
        ImportExportPreferences importExportPreferences = new ImportExportPreferences();
        importExportPreferences.loadRecentLocosListFromFile(getApplicationContext());

        for (int i = 0; i < importExportPreferences.recentLocoAddressList.size(); i++) {
            if (address.equals(importExportPreferences.recentLocoAddressList.get(i))
                    && size.equals(importExportPreferences.recentLocoAddressSizeList.get(i))
                    && name.equals(importExportPreferences.recentLocoNameList.get(i))) {
                position = i;
                Log.d(applicationName, "t_a: findLocoInRecents: Loco '" + name + "' found in Recents");
                break;
            }
        }
        return position;
    }

    @SuppressLint("ApplySharedPref")
    public void updateConnectionList(String retrievedServerName) {
        // if I don't have permissions, don't ask, just ignore

//  commented out as I had not ide why this was being done.. Did not seem to achieve anything
//        if ((context.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
//                && (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED) {

        ImportExportConnectionList importExportConnectionList = new ImportExportConnectionList(getApplicationContext(), prefs);
        importExportConnectionList.connections_list.clear();
        importExportConnectionList.getConnectionsList(getApplicationContext(), "", "");
        importExportConnectionList.saveConnectionsListExecute(
                this, connectedHostip, connectedHostName, connectedPort, retrievedServerName, connectedSsid, connectedServiceType);
        connectedHostName = retrievedServerName;

        String prefAutoImportExport = prefs.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue)).trim();

        String deviceId = mainapp.getFakeDeviceId();
        prefs.edit().putString("prefAndroidId", deviceId).commit();
        prefs.edit().putInt("prefForcedRestartReason", restart_reason_type.AUTO_IMPORT).commit();

        if ((prefAutoImportExport.equals(auto_import_export_option_type.CONNECT_AND_DISCONNECT))
                || (prefAutoImportExport.equals(auto_import_export_option_type.CONNECT_ONLY))) {  // automatically load the host specific preferences, if the preference is set
            if (!connectedHostName.isEmpty()) {
                String exportedPreferencesFileName = connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                ImportExportPreferences importExportPreferences = new ImportExportPreferences();
                importExportPreferences.loadSharedPreferencesFromFile(mainapp, getApplicationContext(), prefs, exportedPreferencesFileName, deviceId, true);

                Message msg = Message.obtain();
                msg.what = message_type.RESTART_APP;
                msg.arg1 = restart_reason_type.AUTO_IMPORT;
                Log.d(applicationName, "t_a: updateConnectionList: Reload of Server Preferences. Restart Requested: " + connectedHostName);
                comm_msg_handler.sendMessage(msg);
            } else {
                safeToast(getApplicationContext().getResources().getString(R.string.toastConnectUnableToLoadPref), Toast.LENGTH_LONG);
            }
        }
//        }
    }

    public void throttleVibration(int speed, int lastSpeed) {
        if ((prefHapticFeedback.equals(HAPTIC_FEEDBACK_SLIDER))
                || (prefHapticFeedback.equals(HAPTIC_FEEDBACK_SLIDER_SCALED))) {
            int prefDisplaySpeedUnits = getIntPrefValue(prefs, "prefDisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
            int xSpeed = speed;
            int xLastSpeed = lastSpeed;
            if (prefHapticFeedback.equals(HAPTIC_FEEDBACK_SLIDER_SCALED)) {
                if (prefDisplaySpeedUnits == 28) {
                    xSpeed = speed / 2;
                    xLastSpeed = lastSpeed / 2;
                } else if (prefDisplaySpeedUnits == 100) {
                    xSpeed = speed / 10;
                    xLastSpeed = lastSpeed / 10;
                } else if (prefDisplaySpeedUnits == 128) {
                    xSpeed = speed / 6;
                    xLastSpeed = lastSpeed / 6;
                }
            }

            if ((xSpeed - xLastSpeed >= 1) || (xLastSpeed - xSpeed >= 1)
                    || ((xSpeed == 0) && (xLastSpeed != 0))
                    || ((xSpeed == 126) && (xLastSpeed != 126))) {
//                    Log.d(applicationName, "t_a: haptic_test: " + "beep");
                vibrate(prefHapticFeedbackDuration);
            }
        }
    }

    public void buttonVibration() {
        if (prefHapticFeedbackButtons) {
            vibrate(prefHapticFeedbackDuration * 2);
        }
    }

    public String getHostIp() {
        return host_ip;
    }

    public Double getWithrottleVersion() {
        return withrottle_version;
    }

    public String getDccexVersionString() {
        return dccexVersionString;
    }
    public float getDccexVersionNumeric() {
        float versionNumber = 4;
        try {
            versionNumber = Float.parseFloat(dccexVersionString);
        } catch (Exception ignored) { } // invalid version
        return versionNumber;
    }

    public String getTurnoutState(String turnoutSystemName) {
        if (turnout_states==null) return ""; //avoid npe
        return turnout_states.get(turnoutSystemName);
    }

    public void putTurnoutState(String turnoutName, String newState) {
        turnout_states.put(turnoutName, newState); //store state by systemName e.g. "LT65"
        Pattern p = Pattern.compile(".T(\\d*)");  //  also store by digits only "65"
        Matcher m = p.matcher(turnoutName);
        if (m.matches()) {
            turnout_states.put(m.group(1), newState);
        }
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

    public int getToolbarHeight(Toolbar toolbar, LinearLayout statusLine,  LinearLayout screenNameLine) {
        int rslt = 0;
        if (toolbar!=null) rslt = toolbar.getHeight();
        if (statusLine!=null) rslt = rslt + statusLine.getHeight();
        if (screenNameLine!=null) rslt = rslt + screenNameLine.getHeight();
        return rslt;
    }

    public void setToolbarTitle(Toolbar toolbar, LinearLayout statusLine,  LinearLayout screenNameLine, String title, String iconTitle, String clockText) {
        if ((toolbar != null) && (statusLine != null) && (screenNameLine != null)) {
            toolbar.setTitle("");
            TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
            tvTitle.setText(title);

            TextView tvIconTitle = screenNameLine.findViewById(R.id.toolbar_icon_title);
            tvIconTitle.setText(iconTitle);

            TextView tvIconHelp = statusLine.findViewById(R.id.toolbar_icon_help);
            if (!prefFullScreenSwipeArea) {
                tvIconHelp.setText("");
                tvIconHelp.setVisibility(View.GONE);
            } else {
                tvIconHelp.setText("◄ ►");
                tvIconHelp.setVisibility(View.VISIBLE);
            }

            TextView tvToolbarServerDesc;
            int screenLayout = getApplicationContext().getResources().getConfiguration().screenLayout;
            screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;
            if (screenLayout >= Configuration.SCREENLAYOUT_SIZE_XLARGE) {
                tvToolbarServerDesc = toolbar.findViewById(R.id.toolbar_server_desc_x_large);
            } else {
                tvToolbarServerDesc = statusLine.findViewById(R.id.toolbar_server_desc);
            }
            if (prefActionBarShowServerDescription) {
                tvToolbarServerDesc.setText(getServerDescription());
                tvToolbarServerDesc.setVisibility(View.VISIBLE);
            } else {
                tvToolbarServerDesc.setVisibility(View.GONE);
            }

            TextView mClock = toolbar.findViewById(R.id.toolbar_clock);
            mClock.setText(clockText);

            prefAppIconAction = prefs.getString("prefAppIconAction", getResources().getString(R.string.prefAppIconActionDefaultValue));
            if (!prefAppIconAction.equals("None")) {
                LinearLayout llToolbarIconLayout = toolbar.findViewById(R.id.toolbar_icon_layout);
                app_icon_button_listener AppIconButtonListener = new app_icon_button_listener();
                llToolbarIconLayout.setOnClickListener(AppIconButtonListener);
            }

//            adjustToolbarButtonSpacing(toolbar);
        }
    }

    public class app_icon_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            // check the preference to see what to do
            if (prefAppIconAction.equals("estop")) {
                sendEStopMsg();
            } else if (prefAppIconAction.equals("throttle")) {
                mainapp.alert_activities(message_type.REOPEN_THROTTLE, "");
            }
            buttonVibration();
        }
    }

    public void hideSoftKeyboard(View view) {
        hideSoftKeyboard(view, "unknown", true);
    }
    public void hideSoftKeyboard(View view, String activityName) {
        hideSoftKeyboard(view, activityName, true);
    }
    public void hideSoftKeyboard(View view, String activityName, boolean activityIsInTransition) {
        Log.d(applicationName, "t_a: hideSoftKeyboard()");
        if (activityIsInTransition) activityInTransition(activityName);
        // Check if no view has focus:
        if (view != null) {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } catch (Exception e) {
                Log.e(applicationName, "select_loco: hideSoftKeyboard(): unable to hide the soft keyboard");
            }
        }
    }

    public void stopAllSounds() {
        Log.d(applicationName, "t_a: stopAllSounds (locoSounds)");
        if (soundPool != null) {
            for (int soundType = 0; soundType < 3; soundType++) {
                for (int throttleIndex = 0; throttleIndex < 2; throttleIndex++) {
                    for (int mSound = 0; mSound < 3; mSound++) {
                        soundPool.stop(soundsExtrasStreamId[soundType][throttleIndex][mSound]);
                    }
                }
            }

            for (int[] ints : soundsLocoStreamId) {
                for (int mSound = 0; mSound < soundsLocoStreamId.length; mSound++) {
                    soundPool.stop(ints[mSound]);
                }
            }

            for (int j = 0; j < 2; j++) {
                soundsLocoCurrentlyPlaying[j] = -1;
                for (int soundType = 0; soundType < 3; soundType++) {
                    soundsExtrasCurrentlyPlaying[soundType][j] = -1;
                }
            }
            soundPool.release();
        }
    } // end stopAllSounds

//    public static void log_dTrace(String label, StackTraceElement[] e) {
//        String method = "";
//        int doNext = 0;
//        for (StackTraceElement s : e) {
//            if (doNext == 1) {
//                method = s.getMethodName();
//            }
//            if (doNext == 2) {
//                Log.d(applicationName, s.getMethodName() + "->" + method + ": " + label);
//                return;
//            }
//            if ((s.getMethodName().equals("getStackTrace")) || (doNext>0)) { doNext++; }
//        }
//    }

    public int getGamePadIndexFromThrottleNo(int whichThrottle) {
        int whichGamepad = -1;
        for (int i = 0; i < prefNumThrottles; i++) {
            if (gamePadDescriptorsAssignedToThrottles[whichThrottle].equals(gamePadDeviceDescriptors[i])) {
                whichGamepad = i;
                break;
            }
        }
        return whichGamepad;
    }

    // work out a) if we need to look for multiple gamepads b) workout which gamepad we received the key event from
    public int findWhichGamePadEventIsFrom(String eventDeviceDescriptor, String eventDeviceName, int eventKeyCode) {
//    public int findWhichGamePadEventIsFrom(int eventDeviceId, String eventDeviceName, int eventKeyCode) {
        int whichGamePad = -2;  // default to the event not from a gamepad
        int whichGamePadDeviceId = -1;
        int j;

        if (!eventDeviceDescriptor.isEmpty()) { // event is from a gamepad (or at least not from a screen touch)
//        if (eventDeviceId >= 0) { // event is from a gamepad (or at least not from a screen touch)
            whichGamePad = -1;  // gamepad

            int reassigningGamepad = -1;
            int i;

            // set for only one but the descrptor has changed - probably turned off then on
            if ( (gamepadCount==1) && (prefGamepadOnlyOneGamepad) && (!gamePadDeviceDescriptors[0].equals(eventDeviceDescriptor)) ) {
                for (int k = 0; k < prefNumThrottles; k++) {
                    if (gamePadDescriptorsAssignedToThrottles[k].equals(gamePadDeviceDescriptors[0])) {
                        gamePadDescriptorsAssignedToThrottles[k] = eventDeviceDescriptor;
                        break;
                    }
                }
                gamePadDeviceDescriptors[0] = eventDeviceDescriptor;
                gamePadDeviceNames[0] = eventDeviceName;
//                gamePadDeviceIdsTested[0]= gamepad_status_type.BAD;
            }

            // find out if this gamepad is already assigned
            for (i = 0; i < prefNumThrottles; i++) {
                if (gamePadDescriptorsAssignedToThrottles[i].equals(eventDeviceDescriptor)) {
                    if (getConsist(i).isActive()) { //found the throttle and it is active
                        whichGamePad = i;
                    } else { // currently assigned to this throttle, but the throttle is not active
                        whichGamePad = i;
                        gamePadDescriptorsAssignedToThrottles[i] = "";
                        reassigningGamepad = gamePadThrottleAssignment[i];
                        gamePadThrottleAssignment[i] = -1;
//                        setGamepadIndicator(); // need to clear the indicator
                    }
                    break;
                }
            }

            if (whichGamePad == -1) { //didn't find it OR is known, but unassigned

                for (j = 0; j < gamepadCount; j++) {
                    if (gamePadDeviceDescriptors[j].equals(eventDeviceDescriptor)) { // known, but unassigned
                        whichGamePadDeviceId = j;
                        break;
                    }
                }
                if (whichGamePadDeviceId == -1) { // previously unseen gamepad
                    gamepadCount++;
                    gamePadDeviceDescriptors[gamepadCount - 1] = eventDeviceDescriptor;
                    gamePadDeviceNames[gamepadCount - 1] = eventDeviceName;
//                    whichGamePadDeviceId = gamepadCount - 1;
                }

                for (i = 0; i < prefNumThrottles; i++) {
                    if (gamePadDescriptorsAssignedToThrottles[i].isEmpty()) {  // throttle is not assigned a gamepad
                        if (getConsist(i).isActive()) { // found next active throttle
                            gamePadDescriptorsAssignedToThrottles[i] = eventDeviceDescriptor;
//                            if (reassigningGamepad == -1) { // not a reassignment
//                                gamePadThrottleAssignment[i] = GAMEPAD_INDICATOR[whichGamePadDeviceId];
//                            } else { // reassigning
                            if (reassigningGamepad != -1) { // not a reassignment
                                gamePadThrottleAssignment[i] = reassigningGamepad;
                            }
                            whichGamePad = i;
//                            setGamepadIndicator();
                            break;  // done
                        }
                    }
                }
//            } else {
//                for (j = 0; j < gamepadCount; j++) {
//                    if (gamePadDeviceIds[j] == eventDeviceId) { // known, but unassigned
//                        whichGamePadDeviceId = j;
//                        break;
//                    }
//                }
//                if (gamePadDeviceIdsTested[whichGamePad]==gamepad_status_type.BAD){  // gamepad is known but failed the test last time
//                    startGamepadTestActivity(whichGamePad);
//                }
            }
        }
        if (gamepadCount > 0) {
            usingMultiplePads = true;
        }

        return whichGamePad;
    }


    // get the consist for the specified throttle
    private static final Consist emptyConsist = new Consist();

    public Consist getConsist(int whichThrottle) {
        if (consists == null || whichThrottle >= consists.length || consists[whichThrottle] == null)
            return emptyConsist;
        return consists[whichThrottle];
    }

    // listener for the joystick events
    public boolean implDispatchGenericMotionEvent(android.view.MotionEvent event) {
        //Log.d(applicationName, activityName + "implDispatchGenericMotionEvent(): " + event.getAction());
        if ((!prefGamePadType.equals(threaded_application.WHICH_GAMEPAD_MODE_NONE)) && (!mainapp.prefGamePadIgnoreJoystick)) { // respond to the gamepad and keyboard inputs only if the preference is set

            int action;
            String deviceDescriptor = event.getDevice().getDescriptor();
            int whichGamePadIsEventFrom = findWhichGamePadEventIsFrom(deviceDescriptor, event.getDevice().getName(), 0); // dummy eventKeyCode

            float xAxis;
            xAxis = Math.round(event.getAxisValue(MotionEvent.AXIS_X) * 10.0f) / 10.0f;
            float yAxis = Math.round(event.getAxisValue(MotionEvent.AXIS_Y) * 10.0f) / 10.0f;
            float xAxis2 = Math.round(event.getAxisValue(MotionEvent.AXIS_Z) * 10.0f) / 10.0f;
            float yAxis2 = Math.round(event.getAxisValue(MotionEvent.AXIS_RZ) * 10.0f) / 10.0f;

            if ((xAxis != 0) || (yAxis != 0)) {
                action = ACTION_DOWN;
            } else {
                action = ACTION_UP;
            }

            sendMsg(comm_msg_handler, message_type.GAMEPAD_JOYSTICK_ACTION,
                    action + ":"
                            + whichGamePadIsEventFrom + ":"
                            + xAxis + ":"
                            + yAxis + ":"
                            + xAxis2 + ":"
                            + yAxis2);

            return (true); // stop processing this key
        }
        return (false);
    }


    // listener for physical keyboard events - called from the
    // used to support the gamepad only   DPAD and key events
    public boolean implDispatchKeyEvent(KeyEvent event) {
        InputDevice dev = event.getDevice();
        String deviceDescriptor = dev.getDescriptor();

        if (dev == null) { // unclear why, but some phones/tables don't seem to return a device for the internal keyboard
            return false;
        }
        String eventDeviceName = dev.getName();
        boolean isExternal = false;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
        InputDevice iDev = getDevice(event.getDeviceId());
        if (iDev != null && iDev.toString().contains("Location: external")) {
            isExternal = true;
        }
        if (!isExternal) {
            for (String gamePadDeviceDescriptor : gamePadDeviceDescriptors) {
                if (gamePadDeviceDescriptor.equals(deviceDescriptor)) {
                    isExternal = true;
                    break;
                }
            }
        }
//        }

        if (isExternal) { // is from a external device (otherwise if it has come from the phone itself, generally don't try to process it here)
            if (!prefGamePadType.equals(threaded_application.WHICH_GAMEPAD_MODE_NONE)) { // respond to the gamepad and keyboard inputs only if the preference is set
                boolean acceptEvent = true; // default to assuming that we will respond to the event

                int action = event.getAction();
                boolean isShiftPressed = event.isShiftPressed();
                int keyCode = event.getKeyCode();
                int repeatCnt = event.getRepeatCount();
//                int whichThrottle;

                int whichGamePadIsEventFrom = findWhichGamePadEventIsFrom(deviceDescriptor, event.getDevice().getName(), event.getKeyCode());
//                int whichGamePadIsEventFrom = findWhichGamePadEventIsFrom(event.getDeviceId(), event.getDevice().getName(), event.getKeyCode());
                if ((whichGamePadIsEventFrom > -1) && (whichGamePadIsEventFrom < gamePadDeviceIdsTested.length)) { // the event came from a gamepad
                    if (gamePadDeviceIdsTested[getGamePadIndexFromThrottleNo(whichGamePadIsEventFrom)] != gamepad_status_type.GOOD) { //if not, testing for this gamepad is not complete or has failed
                        acceptEvent = false;
                    }
                } else {
                    acceptEvent = false;
                }

                if (acceptEvent) {
                    sendMsg(comm_msg_handler, message_type.GAMEPAD_ACTION,
                            action + ":"
                                    + keyCode + ":"
                                    + ((isShiftPressed) ? "1" : "0") + ":"
                                    + repeatCnt + ":"
                                    + whichGamePadIsEventFrom);

                    return (true); // stop processing this key
                }
            }
        } else { // only process the volume keys
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            int repeatCnt = event.getRepeatCount();
            if ((keyCode == KEYCODE_VOLUME_UP) || (keyCode == KEYCODE_VOLUME_DOWN)) {
                sendMsg(comm_msg_handler, message_type.VOLUME_BUTTON_ACTION,
                        action + ":" + keyCode + ":" + repeatCnt);

                return (true); // stop processing this key
            }
        }
        return (false);
    }

    public String fixFilename(String fName) {
        String rslt = "";
        if (fName!=null) {
            rslt = fName.replaceAll("[\\\\/:\"*?<>|]", "_").trim();
        }
        return rslt;
    }

    public int getFadeIn(boolean swipe, float deltaX) {
        int fadeIn = R.anim.fade_in;
        if (swipe) {
            if (deltaX > 0.0) {
                fadeIn = R.anim.push_right_in;
            } else {
                fadeIn = R.anim.push_left_in;
            }
        }
        return fadeIn;
    }

    public int getFadeOut(boolean swipe, float deltaX) {
        int fadeOut = R.anim.fade_out;
        if (swipe) {
            if (deltaX > 0.0) {
                fadeOut = R.anim.push_right_out;
            } else {
                fadeOut = R.anim.push_left_out;
            }
        }
        return fadeOut;
    }

    public void gamepadFullReset() {
        usingMultiplePads = false;
        gamepadCount = 0;
        for (int i=0; i<MAX_GAMEPADS; i++) {
            gamePadDescriptorsAssignedToThrottles[i] = "";
            gamePadThrottleAssignment[i] = -1;
            gamePadDeviceDescriptors[i] = "";
            gamePadDeviceNames[i] = "";
            gamePadDeviceIdsTested[i] = gamepad_status_type.UNKNOWN;
            gamePadLastxAxis[i] = 0;
            gamePadLastyAxis[i] = 0;
            gamePadLastxAxis2[i] = 0;
            gamePadLastyAxis2[i] = 0;
        }
    }

    // Function to extract k bits from p position and returns the extracted value as integer
    // from: https://www.geeksforgeeks.org/extract-k-bits-given-position-number/
    public int bitExtracted(int number, int k, int p) {
        return (((1 << k) - 1) & (number >> (p - 1)));
    }
    public int bitExtracted(long number, int k, int p) {
        final long rightShifted = number >>> (p-1);
        final long mask = (1L << k) - 1L;
        return Math.toIntExact(rightShifted & mask);
    }

    public int toggleBit(int n, int k) {
        return (n ^ (1 << (k - 1)));
    }

    // for DCC-EX we need to temp store the list of locos so we can remove them individually
    public void storeThrottleLocosForReleaseDCCEX(int whichThrottle) {
        if (isDCCEX) {
            Consist con = mainapp.consists[whichThrottle];
            throttleLocoReleaseListDCCEX[whichThrottle] = new String [con.size()];
            int i=0;
            for (Consist.ConLoco l : con.getLocos()) {
                throttleLocoReleaseListDCCEX[whichThrottle][i] = l.getAddress();
                i++;
            }
        }
    }

    public String getFakeDeviceId() {
        return getFakeDeviceId(false);
    }

    @SuppressLint("ApplySharedPref")
    public String getFakeDeviceId(boolean forceNewId) {
//        return Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);  // no longer permitted
        mainapp.deviceId = prefs.getString("prefAndroidId", "");
        if ( (mainapp.deviceId.isEmpty()) || (forceNewId) ) {
            Random rand = new Random();
            mainapp.deviceId = String.valueOf(rand.nextInt(999999)); //use random string
            prefs.edit().putString("prefAndroidId", deviceId).commit();
        }
        return mainapp.deviceId;
    }

    @SuppressLint("DefaultLocale")
    static public String formatNumberInName (String name) {
        if (name==null) return ""; //avoid npe
        StringBuilder tempName = new StringBuilder(name);
        StringBuilder tempNo = new StringBuilder();
        //noinspection UnusedAssignment
        int tempVal = 0;
        char tempChar;
        boolean haveNo = false;
        boolean hasNumber = false;
//        hasNumber= tempName.matches("[\\D]?[\\d]+[\\D]?");
        for (int i=0; i<name.length();i++) {
            tempChar = name.charAt(i);
            if ((tempChar >= '0') && (tempChar <= '9')) {  // numeric
                hasNumber = true;
                break;
            }
        }
        if (hasNumber) {
            tempName = new StringBuilder();
            for (int i=0; i<name.length();i++) {
                tempChar = name.charAt(i);
                if ((tempChar>='0') && (tempChar<='9')) {  // numeric
                    haveNo = true;
                    tempNo.append(name.charAt(i));
                } else {
                    if (haveNo) {
                        tempVal = Integer.parseInt(tempNo.toString());
                        tempName.append(String.format("%6d", tempVal));
                        haveNo = false;
                        tempNo = new StringBuilder();
                    }
                    tempName.append(name.charAt(i));
                }
            }
            if (haveNo) {
                tempVal = Integer.parseInt(tempNo.toString());
                tempName.append(String.format("%6d", tempVal));
            }
        }
        return tempName.toString();
    }

    public void toastSortType(int sortType) {
        if (!mainapp.prefHideInstructionalToasts) {
            switch (sortType) {
                case sort_type.NAME:
                    safeToast(getApplicationContext().getResources().getString(R.string.toastSortedByName), Toast.LENGTH_SHORT);
                    break;
                case sort_type.ID:
                    safeToast(getApplicationContext().getResources().getString(R.string.toastSortedById), Toast.LENGTH_SHORT);
                    break;
                case sort_type.LAST_USED:
                    safeToast(getApplicationContext().getResources().getString(R.string.toastSortedByLastUsed), Toast.LENGTH_SHORT);
                    break;
                case sort_type.POSITION:
                    safeToast(getApplicationContext().getResources().getString(R.string.toastSortedByPosition), Toast.LENGTH_SHORT);
                    break;
            }
        }
    }

    //parse roster functions list into appropriate app variable array
    //  //RF29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
    public static LinkedHashMap<Integer, String> parseFunctionLabels(String responseStr) {
        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //split into list of labels

        //populate a temp label array from RF command string
        LinkedHashMap<Integer, String> functionLabelsMap = new LinkedHashMap<>();
        int i = 0;
        for (String ts : ta) {
            if (i > threaded_application.MAX_FUNCTION_NUMBER +1) break; //ignore unsupported functions
            if (i > 0 && !"".equals(ts)) { //skip first chunk, which is length, and skip any blank entries
                functionLabelsMap.put(i - 1, ts); //index is hashmap key, value is label string
            }
            i++;
        }
        return functionLabelsMap;
    }

    public static String packFunctionLabels(LinkedHashMap<Integer, String> functionLabelsMap) {
        StringBuilder functionLabels = new StringBuilder();
        if ( (functionLabelsMap!=null) && (!functionLabelsMap.isEmpty())) {
            for (int i = 0; i < MAX_FUNCTIONS; i++) {
                functionLabels.append(functionLabelsMap.get(i) != null ? functionLabelsMap.get(i) : "").append("]\\[");
            }
        }
        return functionLabels.toString();
    }

    public void getDefaultSortOrderRoster() {
        String prefSortOrderRoster = prefs.getString("prefSortOrderRoster", this.getResources().getString(R.string.prefSortOrderRosterDefaultValue));
        switch (prefSortOrderRoster) {
            case "id":
                mainapp.rosterOrder = sort_type.ID;
                break;
            case "position":
                mainapp.rosterOrder = sort_type.POSITION;
                break;
            case "name":
            default:
                mainapp.rosterOrder = sort_type.NAME;
                break;
        }
    }

    public static void extendedLogging(String logMessage) {
        if (prefExtendedLogging)
            Log.d(threaded_application.applicationName, logMessage);
    }

    public void getInitialToolbarSize(Toolbar toolbar) {
        if (initialToolbarHeight == -1) {
            ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
            initialToolbarHeight = layoutParams.height;
        }
    }

    public int adjustToolbarSize(Toolbar toolbar) {

        getToolbarButtonSizeToUse();

        getInitialToolbarSize(toolbar);
        int toolbarHeight = initialToolbarHeight;
        int newHeightAndWidth = toolbarHeight;

        ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
        if (threaded_application.toolbarButtonSizeToUse == toolbar_button_size_to_use_type.MEDIUM) {
            newHeightAndWidth = (int) ((float) toolbarHeight * 1.32);
            layoutParams.height = newHeightAndWidth;
            toolbar.setLayoutParams(layoutParams);
        } else if (threaded_application.toolbarButtonSizeToUse == toolbar_button_size_to_use_type.LARGE) {
            newHeightAndWidth = toolbarHeight*2;
            layoutParams.height = newHeightAndWidth;
            toolbar.setLayoutParams(layoutParams);
        }
        return newHeightAndWidth;
    }

    public void countActiveToolbarButtons() {
        threaded_application.toolbarButtonCount = 0;
        String[] prefActionBarButtonPreferencesNames = getResources().getStringArray(R.array.prefActionBarButtonPreferencesNames);
        for (String prefActionBarButtonPreferenceName : prefActionBarButtonPreferencesNames) {
            if (prefs.getBoolean(prefActionBarButtonPreferenceName, false))
                threaded_application.toolbarButtonCount++;
        }
    }

    public void getToolbarButtonSizeToUse() {
        if (MobileControl2.isMobileControl2()) {
            threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.SMALL;
            return;
        }
        countActiveToolbarButtons();
        threaded_application.prefToolbarButtonSize = prefs.getString("prefToolbarButtonSize", getApplicationContext().getResources().getString(R.string.prefToolbarButtonSizeDefaultValue));
        switch (threaded_application.prefToolbarButtonSize) {
            case toolbar_button_size_type.AUTO -> {
                if (threaded_application.displayDiagonalInches >= threaded_application.LARGE_SCREEN_SIZE) {
                    if (threaded_application.toolbarButtonCount <= 5) {
                        threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.LARGE;
                    } else if (threaded_application.toolbarButtonCount <= 9) {
                        threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.MEDIUM;
                    } else {
                        threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.SMALL;
                    }
                } else if (threaded_application.displayDiagonalInches >= threaded_application.MEDIUM_SCREEN_SIZE) {
                    if (threaded_application.toolbarButtonCount <= 5) {
                        threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.MEDIUM;
                    } else {
                        threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.SMALL;
                    }
                }
            }
            case toolbar_button_size_type.LARGE ->
                    threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.LARGE;
            case toolbar_button_size_type.SMALL ->
                    threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.SMALL;
            case toolbar_button_size_type.MEDIUM ->
                    threaded_application.toolbarButtonSizeToUse = toolbar_button_size_to_use_type.MEDIUM;
        }
    }

    public void logAllPreference() {
        // list all the existing preferences, if extended logging is enabled
        if (threaded_application.prefExtendedLogging) {
            Map<String, ?> allPreferences = prefs.getAll();
            for (Map.Entry<String, ?> entry : allPreferences.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Log the key and its value
                if (value != null) {
                    Log.d(threaded_application.applicationName, "PreferenceList: Key: " + key + ", Value: " + value + ", Type: " + value.getClass().getSimpleName());
                } else {
                    Log.d(threaded_application.applicationName, "PreferenceList: Key: " + key + ", Value: null");
                }
            }
        }
    }

    // run at startup
    // this method finds old preferences and renames them
    // always include the date the 'preference' was added so that it can eventually be removed.
    private void renamePreferencesToNewFormat() {
        threaded_application.prefExtendedLogging = prefs.getBoolean("prefExtendedLogging", false);
        logAllPreference();

        // check to see if the rename process has already run
        String prefLastPrefRename = prefs.getString("prefLastPrefRename", "X");
        if (prefLastPrefRename.equals(LAST_PREFERENCE_NAME_RUN)) return; // already checked

        prefs.edit().putString("prefLastPrefRename", LAST_PREFERENCE_NAME_RUN).apply();

        Log.d(threaded_application.applicationName, "PreferenceList: checking for old Preference Names");
        String[] preferencesToRename = getResources().getStringArray(R.array.preferencesToRename);
        for (String prefName : preferencesToRename) {
            String[] prefValues = prefName.split(":");
            if (prefValues.length ==2) {
                renameAPreferenceToNewFormat(prefValues[0], prefValues[1]);
            } else if (prefValues.length == 1) {
                renameAPreferenceToNewFormat(prefValues[0], "");
            }
        }
    }
    @SuppressWarnings("ChainOfInstanceofChecks")
    @SuppressLint("ApplySharedPref")
    private void renameAPreferenceToNewFormat(String oldName, String newName) {
        if (prefs.contains(oldName)) {
            if (!newName.isEmpty()) {
                Object value = prefs.getAll().get(oldName);
                if (value instanceof String) {
                    prefs.edit().putString(newName, (String) value).apply();
                } else if (value instanceof Integer) {
                    prefs.edit().putInt(newName, (Integer) value).apply();
                } else if (value instanceof Boolean) {
                    prefs.edit().putBoolean(newName, (Boolean) value).apply();
                } else if (value instanceof Float) {
                    prefs.edit().putFloat(newName, (Float) value).apply();
                } else if (value instanceof Long) {
                    prefs.edit().putLong(newName, (Long) value).apply();
                }
            }
            Log.d(threaded_application.applicationName, "PreferenceList: Renamed Key: " + oldName + " To: " + newName);

            prefs.edit().remove(oldName).apply();
        }
    }

    // Source - https://stackoverflow.com/a/40168175
    // Modified from post by mgcaguioa
    // Retrieved 2026-02-17, License - CC BY-SA 3.0
    public static void showCustomToast(final Activity activity, String title, String message, int length, int yOffsetSixthOfScreen) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        int durationMs = (length==Toast.LENGTH_SHORT) ? 3000 : 6000;

        // inflate your xml layout
        LayoutInflater inflater = activity.getLayoutInflater();
        final View layout = inflater.inflate(R.layout.custom_toast, null);

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        int yOffset = height * yOffsetSixthOfScreen / 6;


        // set the custom display
        if (!title.isEmpty()) {
            ((TextView) layout.findViewById(R.id.title)).setText(title);
        } else {
            ((TextView) layout.findViewById(R.id.title)).setVisibility(View.GONE);
        }
        ((TextView) layout.findViewById(R.id.message)).setText(message);

        // initialize your popupWindow and use your custom layout as the view
        final PopupWindow toastPopupWindow = new PopupWindow(layout,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        toastPopupWindow.setWidth(width-40);
        toastPopupWindow.setFocusable(true);

        // handle toastPopupWindow click event
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toastPopupWindow.dismiss();
            }
        });

        // Use post to ensure the Activity's window is ready and we have a window token
        final View decorView = activity.getWindow().getDecorView();
        if (decorView != null) {
            decorView.post(new Runnable() {
                @Override
                public void run() {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        try {
                            // Use decorView as the parent to provide the window token
                            toastPopupWindow.showAtLocation(decorView, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, yOffset);

                            // dismiss the popup window after specified period
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                public void run() {
                                    try {
                                        if (toastPopupWindow.isShowing()) {
                                            toastPopupWindow.dismiss();
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }, durationMs);
                        } catch (Exception e) {
                            Log.e(threaded_application.applicationName, activityName + ": showCustomToast(): Error showing custom toast: " + e.getMessage());
                        }
                    }
                }
            });
        }
    }

}
