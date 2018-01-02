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
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import eu.esu.mobilecontrol2.sdk.StopButtonFragment;
import eu.esu.mobilecontrol2.sdk.ThrottleFragment;
import eu.esu.mobilecontrol2.sdk.ThrottleScale;
import jmri.enginedriver.logviewer.ui.LogViewerActivity;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_A;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_D;
import static android.view.KeyEvent.KEYCODE_F;
import static android.view.KeyEvent.KEYCODE_N;
import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_T;
import static android.view.KeyEvent.KEYCODE_V;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.KeyEvent.KEYCODE_W;
import static android.view.KeyEvent.KEYCODE_X;

// for changing the screen brightness

// used for supporting Keyboard and Gamepad input;

public class throttle extends FragmentActivity implements android.gesture.GestureOverlayView.OnGestureListener {

    private threaded_application mainapp; // hold pointer to mainapp
    private SharedPreferences prefs;

    // activity codes
    public static final int ACTIVITY_PREFS = 0;
    public static final int ACTIVITY_SELECT_LOCO = 1;
    public static final int ACTIVITY_CONSIST = 2;
    public static final int ACTIVITY_CONSIST_LIGHTS = 3;

    private static final int GONE = 8;
    private static final int VISIBLE = 0;
    private static final int throttleMargin = 8; // margin between the throttles in dp
    private static final int titleBar = 45; // estimate of lost screen height in dp

    // speed scale factors
    public static final int MAX_SPEED_VAL_WIT = 126;    // wit message maximum speed value, max speed slider value
    public static final int SPEED_STEP_CODE_14 = 8;     // wit speed step codes
    public static final int SPEED_STEP_CODE_27 = 4;
    public static final int SPEED_STEP_CODE_28 = 2;
    public static final int SPEED_STEP_CODE_128 = 1;
    public static final int SPEED_STEP_AUTO_MODE = -1;  // speed step pref value when set to AUTO mode
    private static int maxSpeedStepT = 100;             // decoder max speed steps
    private static int maxSpeedStepG = 100;
    private static int maxSpeedStepS = 100;
    private static int max_throttle_change;          // maximum allowable change of the sliders
    private static int speedStepPref = 100;
    private static double displayUnitScaleT;            // display units per slider count
    private static double displayUnitScaleG;
    private static double displayUnitScaleS;

    private static String VOLUME_INDICATOR = "v";
    private static String[] GAMEPAD_INDICATOR = {"1", "2", "3"};

    private SeekBar sbT; // seekbars
    private SeekBar sbS;
    private SeekBar sbG;
    private ViewGroup fbT; // function button tables
    private ViewGroup fbS;
    private ViewGroup fbG;
    private Button bFwdT; // buttons
    private Button bStopT;
    private Button bRevT;
    private Button bSelT;
    private Button bRSpdT;
    private Button bLSpdT;
    private Button bFwdG;
    private Button bStopG;
    private Button bRevG;
    private Button bSelG;
    private Button bRSpdG;
    private Button bLSpdG;
    private Button bFwdS;
    private Button bStopS;
    private Button bRevS;
    private Button bSelS;
    private Button bRSpdS;
    private Button bLSpdS;
    private TextView tvSpdLabT; // labels
    private TextView tvSpdValT;
    private TextView tvSpdLabG;
    private TextView tvSpdValG;
    private TextView tvSpdLabS;
    private TextView tvSpdValS;
    private TextView tvVolT; // volume indicators
    private TextView tvVolS;
    private TextView tvVolG;

    private TextView tvGamePadT; // volume indicators
    private TextView tvGamePadS;
    private TextView tvGamePadG;

    private LinearLayout llT; // throttles
    private LinearLayout llS;
    private LinearLayout llG;
    private LinearLayout llTSetSpd;
    private LinearLayout llSSetSpd;
    private LinearLayout llGSetSpd;
    // SPDHT for Speed Id and Direction Button Heights
    private LinearLayout llTLocoId;
    private LinearLayout llSLocoId;
    private LinearLayout llGLocoId;
    private LinearLayout llTLocoDir;
    private LinearLayout llSLocoDir;
    private LinearLayout llGLocoDir;
    // SPDHT
    private View vThrotScr;
    private View vThrotScrWrap;

    private boolean stealPromptActive = false; //true while steal dialog is open
    private boolean navigatingAway = false; // true if another activity was selected (false in onPause if going into background)
    private char whichVolume = 'T';

    // screen coordinates for throttle sliders, so we can ignore swipe on them
    private int T_top;
    private int T_bottom;
    private int S_top;
    private int S_bottom;
    private int G_top;
    private int G_bottom;

    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    private boolean gestureFailed = false; // gesture didn't meet velocity or distance requirement
    private boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;

    // function number-to-button maps
    private LinkedHashMap<Integer, Button> functionMapT;
    private LinkedHashMap<Integer, Button> functionMapS;
    private LinkedHashMap<Integer, Button> functionMapG;

    // current direction
    private int dirT = 1;   // requested direction for each throttle (single or multiple engines)
    private int dirS = 1;
    private int dirG = 1;

    private static final String noUrl = "file:///android_asset/blank_page.html";
    private static final float initialScale = 1.5f;
    private WebView webView = null;
    private String webViewLocation;
    private static float scale = initialScale;      // used to restore throttle web zoom level (after rotation)
    private static boolean clearHistory = false;    // flags webViewClient to clear history when page load finishes
    private static String firstUrl = null;          // desired first url when clearing history
    private static String currentUrl = null;
    private String currentTime = "";
    private Menu TMenu;
    static int REP_DELAY = 25;
    static int BUTTON_SPEED_STEP = 4;

    private String speedButtonLeftText;
    private String speedButtonRightText;
    private String speedButtonUpText;
    private String speedButtonDownText;

    private Handler repeatUpdateHandler = new Handler();
    private boolean mAutoIncrement = false;
    private boolean mAutoDecrement = false;

    private static final int changeDelay = 1000;
    private ChangeDelay changeTimerT;
    private ChangeDelay changeTimerG;
    private ChangeDelay changeTimerS;

    private boolean selectLocoRendered = false; // this will be true once set_labels() runs following rendering of the loco select textViews

    // used in the gesture for entering and exiting immersive mode
    private boolean immersiveModeIsOn;

    //used in the gesture for temporarily showing the Web View
    private boolean webViewIsOn = false;
    private String prefSwipeUpOption;
    private String keepWebViewLocation = "none";
    // use for locking the screen on swipe up
    private boolean isScreenLocked = false;
    private boolean screenDimmed = false;
    private int screenBrightnessDim;
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

    //private int screenBrightnessBright;
    private int screenBrightnessOriginal;
    private int screenBrightnessModeOriginal;

    // used to hold the direction change preferences
    boolean dirChangeWhileMoving;
    boolean stopOnDirectionChange;
    boolean locoSelectWhileMoving;

    // used for the GamePad Support
    private static final int DIRECTION_FORWARD = 1;
    private static final int DIRECTION_REVERSE = 0;
    // default to the iOS iCade mappings
    private String whichGamePadMode = "None";
    private static String PREF_GAMEPAD_BUTTON_OPTION_ALL_STOP = "All Stop";
    private static String PREF_GAMEPAD_BUTTON_OPTION_STOP = "Stop";
    private static String PREF_GAMEPAD_BUTTON_OPTION_NEXT_THROTTLE = "Next Throttle";
    private static String PREF_GAMEPAD_BUTTON_OPTION_FORWARD = "Forward";
    private static String PREF_GAMEPAD_BUTTON_OPTION_REVERSE = "Reverse";
    private static String PREF_GAMEPAD_BUTTON_OPTION_FORWARD_REVERSE_TOGGLE = "Forward/Reverse Toggle";
    private static String PREF_GAMEPAD_BUTTON_OPTION_INCREASE_SPEED = "Increase Speed";
    private static String PREF_GAMEPAD_BUTTON_OPTION_DECREASE_SPEED = "Decrease Speed";

    // Gamepad Button preferences
    private String[] prefGamePadButtons = {"Next Throttle","Stop", "Function 00/Light", "Function 01/Bell", "Function 02/Horn",
                                            "Increase Speed", "Reverse", "Decrease Speed", "Forward"};

    //                              none     NextThr  Speed+    Speed-      Fwd         Rev         EStop       F2      F1          F0          Stop
    private int[] gamePadKeys =     {0,        0,   KEYCODE_W, KEYCODE_X,   KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F};
    private int[] gamePadKeys_Up =  {0,        0,   KEYCODE_W,  KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F};

    private ToneGenerator tg;
    private Handler gamepadRepeatUpdateHandler = new Handler();
    private boolean mGamepadAutoIncrement = false;
    private boolean mGamepadAutoDecrement = false;
    private Handler volumeKeysRepeatUpdateHandler = new Handler();
    private boolean mVolumeKeysAutoIncrement = false;
    private boolean mVolumeKeysAutoDecrement = false;

    private int[] gamePadIds = {0,0,0}; // which device id if assigned to each of the three throttles
    private String[] gamePadThrottleAssignment = {"","",""};
    private boolean prefGamePadMultipleDevices = false;
    private boolean usingMultiplePads = false;
    private int[] gamePadDeviceIds = {0,0,0,0,0,0,0}; // which device ids have we seen
    private int gamepadCount = 0;
    // preference to chnage the consist's on long clicks
    boolean prefConsistLightsLongClick;
    public static final int LIGHT_FOLLOW = 1;

    private boolean prefSwapForwardReverseButtons = false;
    private String DirectionButtonLeftText = "Forward";
    private String DirectionButtonRightText = "Reverse";

    //Throttle Array
    private final char[] allThrottleLetters = {'T', 'S', 'G'};

    // For ESU MobileControlII
    private static final boolean IS_ESU_MCII = MobileControl2.isMobileControl2();
    private boolean isEsuMc2Stopped = false; // for tracking status of Stop button
    private ThrottleFragment esuThrottleFragment;
    private StopButtonFragment esuStopButtonFragment;
    private EsuMc2LedControl esuMc2Led = new EsuMc2LedControl();
    private Handler esuButtonRepeatUpdateHandler = new Handler();
    private boolean esuButtonAutoIncrement = false;
    private boolean esuButtonAutoDecrement = false;

    // Create default ESU MCII ThrottleScale for each throttle
    private ThrottleScale esuThrottleScaleT = new ThrottleScale(10, 127);
    private ThrottleScale esuThrottleScaleS = new ThrottleScale(10, 127);
    private ThrottleScale esuThrottleScaleG = new ThrottleScale(10, 127);

    private enum EsuMc2Led {
        RED (MobileControl2.LED_RED),
        GREEN (MobileControl2.LED_GREEN);

        private int value;

        EsuMc2Led(int value) {
            this.value = value;
        }

        private int getValue() {
            return value;
        }
    }

    private enum EsuMc2LedState {
        OFF,
        ON,
        QUICK_FLASH,
        STEADY_FLASH,
        LONG_FLASH,
        SHORT_FLASH
    }

    private enum EsuMc2ButtonAction {
        NO_ACTION ("(no function)"),
        ALL_STOP ("All Stop"),
        STOP ("Stop"),
        DIRECTION_FORWARD ("Forward"),
        DIRECTION_REVERSE ("Reverse"),
        DIRECTION_TOGGLE ("Forward/Reverse Toggle"),
        SPEED_INCREASE ("Increase Speed"),
        SPEED_DECREASE ("Decrease Speed"),
        NEXT_THROTTLE ("Next Throttle"),
        FN00 ("Function 00/Light", 0),
        FN01 ("Function 01/Bell", 1),
        FN02 ("Function 02/Horn", 2),
        FN03 ("Function 03", 3),
        FN04 ("Function 04", 4),
        FN05 ("Function 05", 5),
        FN06 ("Function 06", 6),
        FN07 ("Function 07", 7),
        FN08 ("Function 08", 8),
        FN09 ("Function 09", 9),
        FN10 ("Function 10", 10),
        FN11 ("Function 11", 11),
        FN12 ("Function 12", 12),
        FN13 ("Function 13", 13),
        FN14 ("Function 14", 14),
        FN15 ("Function 15", 15),
        FN16 ("Function 16", 16),
        FN17 ("Function 17", 17),
        FN18 ("Function 18", 18),
        FN19 ("Function 19", 19),
        FN20 ("Function 20", 20),
        FN21 ("Function 21", 21),
        FN22 ("Function 22", 22),
        FN23 ("Function 23", 23),
        FN24 ("Function 24", 24),
        FN25 ("Function 25", 25),
        FN26 ("Function 26", 26),
        FN27 ("Function 27", 27),
        FN28 ("Function 28", 28);

        private String action;
        private int function;

        private static final Map<String,EsuMc2ButtonAction> ENUM_MAP;

        EsuMc2ButtonAction(String action) {
            this(action, -1);
        }

        EsuMc2ButtonAction(String action, int function) {
            this.action = action;
            this.function = function;
        }

        private String getAction() {
            return this.action;
        }

        private int getFunction() {
            return this.function;
        }

        // Build immutable map of String name to enum pairs

        static {
            Map<String,EsuMc2ButtonAction> map = new ConcurrentHashMap<>();
            for (EsuMc2ButtonAction action: EsuMc2ButtonAction.values()) {
                map.put(action.getAction(),action);
            }
            ENUM_MAP = Collections.unmodifiableMap(map);
        }

        private static EsuMc2ButtonAction getAction(String action) {
            return ENUM_MAP.get(action);
        }
    }

    private static class EsuMc2LedControl {
        EsuMc2LedState stateRed;
        EsuMc2LedState stateGreen;

        private void setState(EsuMc2Led which, EsuMc2LedState state) {
            this.setState(which, state, false);
        }

        private void setState(EsuMc2Led which, EsuMc2LedState state, boolean storeState) {
            switch (state) {
                case OFF:
                    MobileControl2.setLedState(which.getValue(), false);
                    break;
                case ON:
                    MobileControl2.setLedState(which.getValue(), true);
                    break;
                case QUICK_FLASH:
                    MobileControl2.setLedState(which.getValue(), 125, 125);
                    break;
                case STEADY_FLASH:
                    MobileControl2.setLedState(which.getValue(), 250, 250);
                    break;
                case LONG_FLASH:
                    MobileControl2.setLedState(which.getValue(), 375, 125);
                    break;
                case SHORT_FLASH:
                    MobileControl2.setLedState(which.getValue(), 125, 375);
                    break;
                default:
                    // Default off
                    MobileControl2.setLedState(which.getValue(), false);
            }
            if (storeState) {
                switch (which) {
                    case RED:
                        stateRed = state;
                        break;
                    case GREEN:
                        stateGreen = state;
                        break;
                }
            }
        }

        private EsuMc2LedState getState(EsuMc2Led which) {
            if (which == EsuMc2Led.RED) {
                return this.stateRed;
            } else {
                return this.stateGreen;
            }
        }

        private void revertLEDStates() {
            revertLEDState(EsuMc2Led.RED);
            revertLEDState(EsuMc2Led.GREEN);
        }

        private void revertLEDState(EsuMc2Led which) {
            setState(which, getState(which), false);
        }
    }

    // For speed slider speed buttons.
    private class RptUpdater implements Runnable {
        char whichThrottle;

        private RptUpdater(char WhichThrottle) {
            whichThrottle = WhichThrottle;

            try {
                REP_DELAY = Integer.parseInt(prefs.getString("speed_arrows_throttle_repeat_delay", "100"));
            } catch (NumberFormatException ex) {
                REP_DELAY = 100;
            }
        }

        @Override
        public void run() {
            if (mAutoIncrement) {
                incrementSpeed(whichThrottle);
                repeatUpdateHandler.postDelayed(new RptUpdater(whichThrottle), REP_DELAY);
            } else if (mAutoDecrement) {
                decrementSpeed(whichThrottle);
                repeatUpdateHandler.postDelayed(new RptUpdater(whichThrottle), REP_DELAY);
            }
        }
    }

    // Handle messages from the communication thread TO this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    private class throttle_handler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            String response_str = msg.obj.toString();
            switch (msg.what) {
                case message_type.RESPONSE: { // handle messages from WiThrottle server
                    if (response_str.length() < 2)
                        return;  //bail if too short, to avoid crash
                    char com1 = response_str.charAt(0);
                    char whichThrottle = response_str.charAt(1);

                    switch (com1) {
                        // various MultiThrottle messages
                        case 'M':
                            char com2 = response_str.charAt(2);
                            String[] ls = threaded_application.splitByString(response_str, "<;>");
                            String addr;
                            try {
                                addr = ls[0].substring(3);
                            } catch (Exception e) {
                                addr = "";
                            }
                            if (whichThrottle == 'T' || whichThrottle == 'S' || whichThrottle == 'G') {
                                if (com2 == '+' || com2 == 'L') { // if loco added or function labels updated
                                    if (com2 == ('+')) {
                                        // set_default_function_labels();
                                        enable_disable_buttons(whichThrottle); // direction and slider: pass whichthrottle
                                    }
                                    // loop through all function buttons and set label and dcc functions (based on settings) or hide if no label
                                    set_function_labels_and_listeners_for_view(whichThrottle);
                                    if (whichThrottle == 'T') {
                                        enable_disable_buttons_for_view(fbT, true);
                                    } else if (whichThrottle == 'G') {
                                        enable_disable_buttons_for_view(fbG, true);
                                    } else {
                                        enable_disable_buttons_for_view(fbS, true);
                                    }
                                    set_labels();
                                } else if (com2 == '-') { // if loco removed
                                    if (whichThrottle == 'T') {
                                        removeLoco('T');
                                        swapToNextAvilableThrottleForGamePad(0, true); // see if we can/need to move the gamepad to another throttle
                                        gamePadIds[0] = 0;
                                        gamePadThrottleAssignment[0] = " ";
                                    } else if (whichThrottle == 'G') {
                                        removeLoco('G');
                                        swapToNextAvilableThrottleForGamePad(2, true); // see if we can/need to move the gamepad to another throttle
                                        gamePadIds[2] = 0;
                                        gamePadThrottleAssignment[2] = " ";
                                    } else {
                                        removeLoco('S');
                                        swapToNextAvilableThrottleForGamePad(1, true); // see if we can/need to move the gamepad to another throttle
                                        gamePadIds[1] = 0;
                                        gamePadThrottleAssignment[1] = " ";
                                    }

                                } else if (com2 == 'A') { // e.g. MTAL2608<;>R1
                                    char com3 = ' ';
                                    if (ls.length >= 2) { //make sure there's a value to parse
                                        com3 = ls[1].charAt(0);
                                    }
                                    if (com3 == 'R') { //MTAL5511<;>R0
                                        int dir;
                                        try {
                                            dir = Integer.valueOf(ls[1].substring(1, 2));
                                        } catch (Exception e) {
                                            dir = 1;
                                        }

                                        Consist con;
                                        int curDir;
                                        if (whichThrottle == 'T') {
                                            con = mainapp.consistT;
                                            curDir = dirT;
                                        } else if (whichThrottle == 'G') {
                                            con = mainapp.consistG;
                                            curDir = dirG;
                                        } else {
                                            con = mainapp.consistS;
                                            curDir = dirS;
                                        }

                                        if (addr.equals(con.getLeadAddr())) {
                                            if (dir != curDir) { // lead/consist direction changed from outside of ED
                                                showDirectionRequest(whichThrottle, dir);       // update requested direction indication
                                                setEngineDirection(whichThrottle, dir, true);   // update rest of consist to match new direction
                                            }
                                        } else {
                                            int locoDir = curDir;               // calc correct direction for this (non-lead) loco
                                            try {
                                                if (con.isReverseOfLead(addr))
                                                    locoDir ^= 1;
                                                if (locoDir != dir) {// if loco has wrong direction then correct it
                                                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DIRECTION, addr, whichThrottle, locoDir);
                                                }
                                            } catch (Exception e) {     // isReverseOfLead returns null if addr is not in con
                                                // - should not happen unless WiT is reporting on engine user just dropped from ED consist?
                                                Log.d("Engine_Driver", "throttle " + whichThrottle + " loco " + addr + " direction reported by WiT but engine is not assigned");
                                            }
                                        }
                                    } else if (com3 == 'V') {
                                        try {
                                            int speed = Integer.parseInt(ls[1].substring(1));
                                            speedUpdateWiT(whichThrottle, speed); // update speed slider and indicator
                                        } catch (Exception ignored) {
                                        }
                                    } else if (com3 == 'F') {
                                        try {
                                            int function = Integer.valueOf(ls[1].substring(2));
                                            set_function_state(whichThrottle, function);
                                        } catch (Exception ignored) {
                                        }
                                    } else if (com3 == 's') {
                                        try {
                                            int speedStepCode = Integer.valueOf(ls[1].substring(1));
                                            setSpeedStepsFromWiT(whichThrottle, speedStepCode);
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }
                            }
                            break;

                        case 'T':
                        case 'S':
                        case 'G':
                            enable_disable_buttons(com1); // pass whichthrottle
                            set_labels();
                            break;

                        case 'R':
                            if (whichThrottle == 'F' || whichThrottle == 'S' || whichThrottle == 'G') { // roster function labels - legacy
                                if (whichThrottle == 'F') { // used to use 'F' instead of 'T'
                                    whichThrottle = 'T';
                                }
                                set_function_labels_and_listeners_for_view(whichThrottle);
                                if (whichThrottle == 'T') {
                                    enable_disable_buttons_for_view(fbT, true);
                                } else if (whichThrottle == 'G') {
                                    enable_disable_buttons_for_view(fbG, true);
                                } else {
                                    enable_disable_buttons_for_view(fbS, true);
                                }
                                set_labels();

                            } else {
                                try {
                                    String scom2 = response_str.substring(1, 6);
                                    if (scom2.equals("PF}|{")) {
                                        whichThrottle = response_str.charAt(6);
                                        set_all_function_states(whichThrottle);
                                    }
                                } catch (IndexOutOfBoundsException ignored) {
                                }
                            }
                            break;
                        case 'P': // panel info
                            if (whichThrottle == 'W') { // PW - web server port info
                                initWeb();
                                set_labels();
                            }
                            if (whichThrottle == 'P') { // PP - power state change
                                set_labels();
                            }
                            break;
                    } // end of switch

                    if (!selectLocoRendered) // call set_labels if the select loco textViews had not rendered the last time it was called
                        set_labels();
                }
                break;
                case message_type.ROSTER_UPDATE:
                    set_labels();               // refresh function labels when any roster response is received
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(response_str);
                    break;
                case message_type.WIT_CON_RECONNECT:
                    break;
                case message_type.CURRENT_TIME:
                    currentTime = response_str;
                    setTitle();
                    break;
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
                case message_type.WEBVIEW_LOC:      // webview location changed
                    // set new location
                    webViewLocation = prefs
                            .getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
                    keepWebViewLocation = webViewLocation;
                    webViewIsOn = false;
                    reloadWeb();
                    break;
                case message_type.INITIAL_WEBPAGE:
                    initWeb();
                    break;
                case message_type.REQ_STEAL:
                    String addr = msg.obj.toString();
                    char whichThrottle = (char) msg.arg1;
                    promptForSteal(addr, whichThrottle);
                    break;
            }
        }
    }

    // Change the screen brightness
    public void setScreenBrightness(int brightnessValue){
        Context mContext;
        mContext = getApplicationContext();

        /*
            public abstract ContentResolver getContentResolver ()
                Return a ContentResolver instance for your application's package.
        */
        /*
            Settings
                The Settings provider contains global system-level device preferences.

            Settings.System
                System settings, containing miscellaneous system preferences. This table holds
                simple name/value pairs. There are convenience functions for accessing
                individual settings entries.
        */
        /*
            public static final String SCREEN_BRIGHTNESS
                The screen backlight brightness between 0 and 255.
                Constant Value: "screen_brightness"
        */
        /*
            public static boolean putInt (ContentResolver cr, String name, int value)
                Convenience function for updating a single settings value as an integer. This will
                either create a new entry in the table if the given name does not exist, or modify
                the value of the existing row with that name. Note that internally setting values
                are always stored as strings, so this function converts the given value to a
                string before storing it.

            Parameters
                cr : The ContentResolver to access.
                name : The name of the setting to modify.
                value : The new value for the setting.
            Returns
                true : if the value was set, false on database errors
        */

        // Make sure brightness value between 0 to 255
        if(brightnessValue >= 0 && brightnessValue <= 255){
            setScreenBrightnessMode(SCREEN_BRIGHTNESS_MODE_MANUAL);

            if (Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessValue)) {
                Log.d("Engine_Driver", "screen brightness successfully changed to " + brightnessValue);
            } else {
                Log.e("Engine_Driver", "screen brightness was NOT changed to " + brightnessValue);
            }
        }
    }

    // Get the screen current brightness
    protected int getScreenBrightness(){
        Context mContext;
        mContext = getApplicationContext();

        /*
            public static int getInt (ContentResolver cr, String name, int def)
                Convenience function for retrieving a single system settings value as an integer.
                Note that internally setting values are always stored as strings; this function
                converts the string to an integer for you. The default value will be returned
                if the setting is not defined or not an integer.

            Parameters
                cr : The ContentResolver to access.
                name : The name of the setting to retrieve.
                def : Value to return if the setting is not defined.
            Returns
                The setting's current value, or 'def' if it is not defined or not a valid integer.
        */
        int brightnessValue = Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                0
        );
        return brightnessValue;
    }

    public void setScreenBrightnessMode(int brightnessModeValue){
        Context mContext;
        mContext = getApplicationContext();

        if(brightnessModeValue >= 0 && brightnessModeValue <= 1){
            if (!Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessModeValue)) {
                Toast.makeText(getApplicationContext(), "Unable to set the Auto Brightness/ Adaptive Brightness.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected int getScreenBrightnessMode(){
        Context mContext;
        mContext = getApplicationContext();
        int BrightnessModeValue = 0;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BrightnessModeValue = Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    0
            );
        }
        return BrightnessModeValue;
    }

    private void setImmersiveModeOn(View webView) {
        boolean tvim = prefs.getBoolean("prefThrottleViewImmersiveMode", getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeDefaultValue));
        immersiveModeIsOn = false;

        if (tvim) {   // if the preference is set use Immersive mode
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                immersiveModeIsOn = true;
                webView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
        }
    }

    private void setImmersiveModeOff(View webView) {
        boolean tvim = prefs.getBoolean("prefThrottleViewImmersiveMode", getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeDefaultValue));
        immersiveModeIsOn = false;

        if (tvim) {   // if the preference is set use Immersive mode
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                webView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_VISIBLE);
            }
            webView.invalidate();
        }
    }

    private void reloadWeb() {
        webView.stopLoading();
        load_webview(); // reload
    }

    private void initWeb() {
        // reload from the initial webpage
        currentUrl = null;
        reloadWeb();
    }

    private void witRetry(String s) {
        if (this.hasWindowFocus()) {
            webView.stopLoading();
            Intent in = new Intent().setClass(this, reconnect_status.class);
            in.putExtra("status", s);
            navigatingAway = true;
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    private void removeLoco(char whichThrottle) {
        disable_buttons(whichThrottle);         // direction and slider
        set_function_labels_and_listeners_for_view(whichThrottle);
        set_labels();
    }

    // process WiT speed report
    // update speed slider if didn't just send a speed update to WiT
    void speedUpdateWiT(char whichThrottle, int speed) {
        if (speed < 0)
            speed = 0;
        if (whichThrottle == 'T') {
            if (!changeTimerT.delayInProg) {
                sbT.setProgress(speed);
            }
        } else if (whichThrottle == 'G') {
            if (!changeTimerG.delayInProg) {
                sbG.setProgress(speed);
            }
        } else {
            if (!changeTimerS.delayInProg) {
                sbS.setProgress(speed);
            }
        }
    }

    SeekBar getThrottleSlider(char whichThrottle) {
        SeekBar throttle_slider;
        if (whichThrottle == 'T') {
            throttle_slider = sbT;
        } else if (whichThrottle == 'G') {
            throttle_slider = sbG;
        } else {
            throttle_slider = sbS;
        }
        return throttle_slider;
    }

    double getDisplayUnitScale(char whichThrottle) {
        double displayUnitScale;
        if (whichThrottle == 'T') {
            displayUnitScale = displayUnitScaleT;
        } else if (whichThrottle == 'G') {
            displayUnitScale = displayUnitScaleG;
        } else {
            displayUnitScale = displayUnitScaleS;
        }
        return displayUnitScale;
    }

    // set speed slider to absolute value on all throttles
    void speedUpdate(int speed) {
        for (char throttleLetter : allThrottleLetters) {
            speedUpdate(throttleLetter, speed);
        }
    }

    // set speed slider to absolute value on one throttle
    void speedUpdate(char whichThrottle, int speed) {
        if (speed < 0)
            speed = 0;
        getThrottleSlider(whichThrottle).setProgress(speed);
    }

    // get the current speed of the throttle
    int getSpeed(char whichThrottle) {
        return getThrottleSlider(whichThrottle).getProgress();
    }

    int getMaxSpeed(char whichThrottle) {
        return getThrottleSlider(whichThrottle).getMax();
    }

    int getMinSpeed(char whichThrottle) {
        return 0;
    }

    // returns true if throttle is set for the maximum speed
    boolean atMaxSpeed(char whichThrottle) {
        return (getSpeed(whichThrottle) >= getMaxSpeed(whichThrottle));
    }

    // returns true if throttle is set for the minimum speed
    boolean atMinSpeed(char whichThrottle) {
        return (getSpeed(whichThrottle) <= getMinSpeed(whichThrottle));
    }

    // change speed slider by scaled display unit value
    int speedChange(char whichThrottle, int change) {
        SeekBar throttle_slider = getThrottleSlider(whichThrottle);
        double displayUnitScale = getDisplayUnitScale(whichThrottle);
        int lastSpeed = throttle_slider.getProgress();
        int lastScaleSpeed = (int) Math.round(lastSpeed * displayUnitScale);
        int speed = (int) Math.round(lastSpeed + (change / displayUnitScale));
        int scaleSpeed = (int) Math.round(speed * displayUnitScale);
        if (lastScaleSpeed == scaleSpeed) {
            speed += Math.signum(change);
        }
        if (speed < 0)  //insure speed is inside bounds
            speed = 0;
        if (speed > MAX_SPEED_VAL_WIT)
            speed = MAX_SPEED_VAL_WIT;
        throttle_slider.setProgress(speed);
        return speed;
    }

    public void decrementSpeed(char whichThrottle) {
        speedChangeAndNotify(whichThrottle, -BUTTON_SPEED_STEP);
    }

    public void incrementSpeed(char whichThrottle) {
        speedChangeAndNotify(whichThrottle, BUTTON_SPEED_STEP);
    }

    // set speed slider and notify server for all throttles
    void speedUpdateAndNotify(int speed) {
        for (char throttleLetter : allThrottleLetters) {
            speedUpdateAndNotify(throttleLetter, speed);
        }
    }

    // set speed slider and notify server for one throttle
    void speedUpdateAndNotify(char whichThrottle, int speed) {
        speedUpdate(whichThrottle, speed);
        sendSpeedMsg(whichThrottle, speed);
    }

    // change speed slider by scaled value and notify server
    public void speedChangeAndNotify(char whichThrottle, int change) {
        int speed = speedChange(whichThrottle, change);
        sendSpeedMsg(whichThrottle, speed);
        // Now update ESU MCII Knob position
        if (IS_ESU_MCII) {
            setEsuThrottleKnobPosition(whichThrottle, speed);
        }
    }

    // set the displayed numeric speed value
    private void setDisplayedSpeed(char whichThrottle, int speed) {
        TextView speed_label;
        Consist con;
        double speedScale = getDisplayUnitScale(whichThrottle);
        if (whichThrottle == 'T') {
            speed_label = tvSpdValT;
            con = mainapp.consistT;
        } else if (whichThrottle == 'G') {
            speed_label = tvSpdValG;
            con = mainapp.consistG;
        } else {
            speed_label = tvSpdValS;
            con = mainapp.consistS;
        }
        if (speed < 0) {
            Toast.makeText(getApplicationContext(), "Alert: Engine " + con.toString() + " is set to ESTOP", Toast.LENGTH_LONG).show();
            speed = 0;
        }
        int scaleSpeed = (int) Math.round(speed * speedScale);
        speed_label.setText(Integer.toString(scaleSpeed));
    }

    //adjust maxspeedsteps from code passed from JMRI, but only if set to Auto, else do not change
    private void setSpeedStepsFromWiT(char whichThrottle, int speedStepCode) {
        int maxSpeedStep = 100;
        ThrottleScale esuThrottleScale = new ThrottleScale(10, 127);
        switch (speedStepCode) {
            case SPEED_STEP_CODE_128:
                maxSpeedStep = 126;
                esuThrottleScale = new ThrottleScale(10, 127);
                break;
            case SPEED_STEP_CODE_27:
                maxSpeedStep = 27;
                esuThrottleScale = new ThrottleScale(10, 28);
                break;
            case SPEED_STEP_CODE_14:
                maxSpeedStep = 14;
                esuThrottleScale = new ThrottleScale(10, 15);
                break;
            case SPEED_STEP_CODE_28:
                maxSpeedStep = 28;
                esuThrottleScale = new ThrottleScale(10, 29);
                break;
        }
        if (whichThrottle == 'T') {
            maxSpeedStepT = maxSpeedStep;
            esuThrottleScaleT = esuThrottleScale;
        } else if (whichThrottle == 'G') {
            maxSpeedStepG = maxSpeedStep;
            esuThrottleScaleG = esuThrottleScale;
        } else {
            maxSpeedStepS = maxSpeedStep;
            esuThrottleScaleS = esuThrottleScale;
        }
        setDisplayUnitScale(whichThrottle);
    }

    //get max speedstep based on Preferences 
    //unless pref is set to AUTO in which case just return the input value
    private int getSpeedSteps(int maxStep) {
        if (speedStepPref != SPEED_STEP_AUTO_MODE) {
            maxStep = speedStepPref;
        }
        return maxStep;
    }

    private void setDisplayUnitScale(char whichThrottle) {
        int maxStep;
        if (whichThrottle == 'T') {
            maxStep = getSpeedSteps(maxSpeedStepT);
            displayUnitScaleT = calcDisplayUnitScale(maxStep);
            tvSpdLabT.setText(calcDisplayUnitLabel(maxStep));
        } else if (whichThrottle == 'G') {
            maxStep = getSpeedSteps(maxSpeedStepG);
            displayUnitScaleG = calcDisplayUnitScale(maxStep);
            tvSpdLabG.setText(calcDisplayUnitLabel(maxStep));
        } else {
            maxStep = getSpeedSteps(maxSpeedStepS);
            displayUnitScaleS = calcDisplayUnitScale(maxStep);
            tvSpdLabS.setText(calcDisplayUnitLabel(maxStep));
        }
    }

    private double calcDisplayUnitScale(int maxSpeedStep) {
        return maxSpeedStep / (double) MAX_SPEED_VAL_WIT;
    }

    private String calcDisplayUnitLabel(int maxSpeedStep) {
        if (maxSpeedStep == 100) {
            return getApplicationContext().getResources().getString(R.string.label_percent);
        } else {
            return String.valueOf(maxSpeedStep);
        }
    }


    // set the direction for all engines on the throttle
    // if skipLead is true, the direction is not set for the lead engine
    void setEngineDirection(char whichThrottle, int direction, boolean skipLead) {
        Consist con;
        if (whichThrottle == 'T') {
            con = mainapp.consistT;
            dirT = direction;
        }
        else if (whichThrottle == 'G') {
            con = mainapp.consistG;
            dirG = direction;
        }
        else {
            con = mainapp.consistS;
            dirS = direction;
        }
        String leadAddr = con.getLeadAddr();
        for (String addr : con.getList()) { // loop through each engine in consist
            if (!skipLead || (addr != null && !addr.equals(leadAddr))) {
                int locoDir = direction;
                try {
                    if (con.isReverseOfLead(addr)) // if engine faces opposite of lead loco
                        locoDir ^= 1; // then reverse the commanded direction
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DIRECTION, addr, whichThrottle, locoDir);
                } catch (Exception e) { // isReverseOfLead returns null if addr is not in con - should never happen since we are walking through consist list
                    Log.d("Engine_Driver", "throttle " + whichThrottle + " direction change for unselected loco " + addr);
                }
            }
        }
    }

    // get the consist for the specified throttle
    private Consist getConsist(char whichThrottle) {
        Consist con;
        if (whichThrottle == 'T')
            con = mainapp.consistT;
        else if (whichThrottle == 'G')
            con = mainapp.consistG;
        else
            con = mainapp.consistS;
        return con;
    }

    // get the indicated direction from the button pressed state
    private int getDirection(char whichThrottle) {
        if (whichThrottle == 'T') {
            return dirT;
        } else if (whichThrottle == 'G') {
            return dirG;
        } else {
            return dirS;
        }
    }

    // update the direction indicators
    void showDirectionIndications() {
        showDirectionIndication('T', dirT);
        showDirectionIndication('S', dirS);
        showDirectionIndication('G', dirG);
    }

    // indicate direction using the button pressed state
    void showDirectionIndication(char whichThrottle, int direction) {
        Button bFwd;
        Button bRev;
        if (!prefSwapForwardReverseButtons) {
            if (whichThrottle == 'T') {
                bFwd = bFwdT;
                bRev = bRevT;
            } else if (whichThrottle == 'G') {
                bFwd = bFwdG;
                bRev = bRevG;
            } else {
                bFwd = bFwdS;
                bRev = bRevS;
            }
        } else {
            if (whichThrottle == 'T') {
                bRev = bFwdT;
                bFwd = bRevT;
            } else if (whichThrottle == 'G') {
                bRev = bFwdG;
                bFwd = bRevG;
            } else {
                bRev = bFwdS;
                bFwd = bRevS;
            }
        }
        if (direction == 0) {
            bFwd.setPressed(false);
            bRev.setPressed(true);
        } else {
            bFwd.setPressed(true);
            bRev.setPressed(false);
        }
//        bFwd.invalidate(); //button wasn't changing at times
//        bRev.invalidate();
    }

    // indicate requested direction using the button typeface
    void showDirectionRequest(char whichThrottle, int direction) {
        Button bFwd;
        Button bRev;
        if (!prefSwapForwardReverseButtons) {
            if (whichThrottle == 'T') {
                bFwd = bFwdT;
                bRev = bRevT;
                dirT = direction;
            } else if (whichThrottle == 'G') {
                bFwd = bFwdG;
                bRev = bRevG;
                dirG = direction;
            } else {
                bFwd = bFwdS;
                bRev = bRevS;
                dirS = direction;
            }
        } else {
            if (whichThrottle == 'T') {
                bRev = bFwdT;
                bFwd = bRevT;
                dirT = direction;
            } else if (whichThrottle == 'G') {
                bRev = bFwdG;
                bFwd = bRevG;
                dirG = direction;
            } else {
                bRev = bFwdS;
                bFwd = bRevS;
                dirS = direction;
            }

        }
        if (direction == 0) {
            bFwd.setTypeface(null, Typeface.NORMAL);
            bRev.setTypeface(null, Typeface.ITALIC);
        } else {
            bFwd.setTypeface(null, Typeface.ITALIC);
            bRev.setTypeface(null, Typeface.NORMAL);
        }

        /*
         * this code gathers direction feedback for the direction indication
         * 
         * if (mainapp.withrottle_version < 2.0) { // no feedback avail so just
         * let indication follow request showDirectionIndication(whichThrottle,
         * direction); } else { //get confirmation of direction changes
         * mainapp.sendMsgDelay(mainapp.comm_msg_handler, 100,
         * message_type.REQ_DIRECTION, "", (int) whichThrottle, 0); }
         * 
         * due to response lags, for now just track the setting: //******
         */
        showDirectionIndication(whichThrottle, direction);
    }

    //
    // processes a direction change if allowed by related preferences and current speed
    // returns true if throttle direction matches the requested direction
    private boolean changeDirectionIfAllowed(char whichThrottle, int direction) {
        if ((getDirection(whichThrottle) != direction) && isChangeDirectionAllowed(whichThrottle)) {
            // set speed to zero on direction change while moving if the preference is set
            if (stopOnDirectionChange && (getSpeed(whichThrottle) != 0)) {
                speedUpdateAndNotify(whichThrottle, 0);
            }

            showDirectionRequest(whichThrottle, direction);        // update requested direction indication
            setEngineDirection(whichThrottle, direction, false);   // update direction for each engine on this throttle
        }
        return (getDirection(whichThrottle) == direction);
    }

    private boolean isChangeDirectionAllowed(char whichThrottle) {
        // check whether direction change is permitted
        boolean isAllowed = false;
        if (getConsist(whichThrottle).isActive()) {
            if(stopOnDirectionChange || dirChangeWhileMoving || (getSpeed(whichThrottle) == 0))
                isAllowed = true;
        }
        return isAllowed;
    }

    void set_stop_button(char whichThrottle, boolean pressed) {
        Button bStop;
        if (whichThrottle == 'T') {
            bStop = bStopT;
        } else if (whichThrottle == 'G') {
            bStop = bStopG;
        } else {
            bStop = bStopS;
        }
        if (pressed) {
            bStop.setPressed(true);
            bStop.setTypeface(null, Typeface.ITALIC);
        } else {
            bStop.setPressed(false);
            bStop.setTypeface(null, Typeface.NORMAL);
        }
    }

    private boolean isSelectLocoAllowed(char whichThrottle) {
        // check whether loco change is permitted
        boolean isAllowed = false;
        if (!getConsist(whichThrottle).isActive() || locoSelectWhileMoving || (getSpeed(whichThrottle) == 0)) {
            isAllowed = true;
        }
        return isAllowed;
    }

    void start_select_loco_activity(char whichThrottle) {
        // give feedback that select button was pressed
        if (whichThrottle == 'T') {
            bSelT.setPressed(true);
        } else if (whichThrottle == 'G') {
            bSelG.setPressed(true);
        } else {
            bSelS.setPressed(true);
        }
        try {
            Intent select_loco = new Intent().setClass(this, select_loco.class);
            select_loco.putExtra("sWhichThrottle", Character.toString(whichThrottle));  // pass whichThrottle as an extra to activity
            navigatingAway = true;
            startActivityForResult(select_loco, ACTIVITY_SELECT_LOCO);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        } catch (Exception ex) {
            Log.d("Engine_Driver", ex.getMessage());
        }
    }

    // Edit the Consist Lights
    void start_consist_lights_edit(char whichThrottle) {
        if (prefConsistLightsLongClick) {  // only allow the editing in the consist lights if the preference is set
            try {
                Intent consistLightsEdit = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit.putExtra("whichThrottle", whichThrottle);
                navigatingAway = true;
                startActivityForResult(consistLightsEdit, throttle.ACTIVITY_CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d("Engine_Driver", ex.getMessage());
            }
        }
    }

    void disable_buttons(char whichThrottle) {
        enable_disable_buttons(whichThrottle, true);
    }

    void enable_disable_buttons(char whichThrottle) {
        enable_disable_buttons(whichThrottle, false);
    }

    void applySpeedRelatedOptions() {
        for (char throttleLetter : allThrottleLetters) {
            applySpeedRelatedOptions(throttleLetter);}  // repeat for all three throttles
    }

    void applySpeedRelatedOptions(char whichThrottle) {
        // default to throttle 'T'
        Button bFwd = bFwdT;
        Button bRev = bRevT;
        Button bSel = bSelT;
        boolean tIsEnabled = llT.isEnabled();
        int dir = dirT;
        switch (whichThrottle) {
            case 'S':
                bFwd = bFwdS;
                bRev = bRevS;
                bSel = bSelS;
                tIsEnabled = llS.isEnabled();
                dir = dirS;
                break;
            case 'G':
                bFwd = bFwdG;
                bRev = bRevG;
                bSel = bSelG;
                tIsEnabled = llG.isEnabled();
                dir = dirG;
        }

        if (getConsist(whichThrottle).isActive()) {
            boolean dirChangeAllowed = tIsEnabled && isChangeDirectionAllowed(whichThrottle);
            boolean locoChangeAllowed = tIsEnabled && isSelectLocoAllowed(whichThrottle);
            if (dirChangeAllowed) {
                    bFwd.setEnabled(true);
                    bRev.setEnabled(true);
            } else {
                if (dir == 1) {
                    if (!prefSwapForwardReverseButtons) {
                        bFwd.setEnabled(true);
                        bRev.setEnabled(false);
                    } else {
                        bFwd.setEnabled(false);
                        bRev.setEnabled(true);
                    }
                } else {
                    if (!prefSwapForwardReverseButtons) {
                        bFwd.setEnabled(false);
                        bRev.setEnabled(true);
                    } else {
                        bFwd.setEnabled(true);
                        bRev.setEnabled(false);
                    }
                }
            }
            bSel.setEnabled(locoChangeAllowed);
        }
        else {
            bFwd.setEnabled(false);
            bRev.setEnabled(false);
            bSel.setEnabled(true);
        }
    }

    void enable_disable_buttons(char whichThrottle, boolean forceDisable) {
        boolean newEnabledState = false;
        if (whichThrottle == 'T') {
            if (!forceDisable) {
                newEnabledState = mainapp.consistT.isActive();      // set false if lead loco is not assigned
            }
            bFwdT.setEnabled(newEnabledState);
            bRevT.setEnabled(newEnabledState);
            bStopT.setEnabled(newEnabledState);
            tvSpdValT.setEnabled(newEnabledState);
            bLSpdT.setEnabled(newEnabledState);
            bRSpdT.setEnabled(newEnabledState);
            enable_disable_buttons_for_view(fbT, newEnabledState);
            if (!newEnabledState) {
                sbT.setProgress(0); // set slider to 0 if disabled
            }
            sbT.setEnabled(newEnabledState);
        } else if (whichThrottle == 'G') {
            if (!forceDisable) {
                newEnabledState = (mainapp.consistG.isActive());    // set false if lead loco is not assigned
            }
            bFwdG.setEnabled(newEnabledState);
            bStopG.setEnabled(newEnabledState);
            bRevG.setEnabled(newEnabledState);
            tvSpdLabG.setEnabled(newEnabledState);
            tvSpdValG.setEnabled(newEnabledState);
            bLSpdG.setEnabled(newEnabledState);
            bRSpdG.setEnabled(newEnabledState);
            enable_disable_buttons_for_view(fbG, newEnabledState);
            if (!newEnabledState) {
                sbG.setProgress(0); // set slider to 0 if disabled
            }
            sbG.setEnabled(newEnabledState);
        } else {
            if (!forceDisable) {
                newEnabledState = (mainapp.consistS.isActive());    // set false if lead loco is not assigned
            }
            bFwdS.setEnabled(newEnabledState);
            bStopS.setEnabled(newEnabledState);
            bRevS.setEnabled(newEnabledState);
            tvSpdLabS.setEnabled(newEnabledState);
            tvSpdValS.setEnabled(newEnabledState);
            bLSpdS.setEnabled(newEnabledState);
            bRSpdS.setEnabled(newEnabledState);
            enable_disable_buttons_for_view(fbS, newEnabledState);
            if (!newEnabledState) {
                sbS.setProgress(0); // set slider to 0 if disabled
            }
            sbS.setEnabled(newEnabledState);
        }
    } // end of enable_disable_buttons

    // helper function to enable/disable all children for a group
    void enable_disable_buttons_for_view(ViewGroup vg, boolean newEnabledState) {
        // Log.d("Engine_Driver","starting enable_disable_buttons_for_view " +
        // newEnabledState);

        ViewGroup r; // row
        Button b; // button
        for (int i = 0; i < vg.getChildCount(); i++) {
            r = (ViewGroup) vg.getChildAt(i);
            for (int j = 0; j < r.getChildCount(); j++) {
                b = (Button) r.getChildAt(j);
                b.setEnabled(newEnabledState);
            }
        }
    } // enable_disable_buttons_for_view

    // update the appearance of all function buttons
    void set_all_function_states(char whichThrottle) {
        // Log.d("Engine_Driver","set_function_states");

        LinkedHashMap<Integer, Button> fMap;
        if (whichThrottle == 'T') {
            fMap = functionMapT;
        } else if (whichThrottle == 'G') {
            fMap = functionMapG;
        } else {
            fMap = functionMapS;
        }

        for (Integer f : fMap.keySet()) {
            set_function_state(whichThrottle, f);
        }
    }

    // update a function button appearance based on its state
    void set_function_state(char whichThrottle, int function) {
        // Log.d("Engine_Driver","starting set_function_request");
        Button b;
        boolean[] fs;   // copy of this throttle's function state array
        if (whichThrottle == 'T') {
            b = functionMapT.get(function);
            fs = mainapp.function_states_T;
        } else if (whichThrottle == 'G') {
            b = functionMapG.get(function);
            fs = mainapp.function_states_G;
        } else {
            b = functionMapS.get(function);
            fs = mainapp.function_states_S;
        }
        if (b != null && fs != null) {
            if (fs[function]) {
                b.setTypeface(null, Typeface.ITALIC);
                b.setPressed(true);
            } else {
                b.setTypeface(null, Typeface.NORMAL);
                b.setPressed(false);
            }
        }
    }

    /*
     * future use: displays the requested function state independent of (in addition to) feedback state 
     * todo: need to handle momentary buttons somehow
     */
    void set_function_request(char whichThrottle, int function, int reqState) {
        // Log.d("Engine_Driver","starting set_function_request");
        Button b;
        if (whichThrottle == 'T') {
            b = functionMapT.get(function);
        } else if (whichThrottle == 'G') {
            b = functionMapG.get(function);
        } else {
            b = functionMapS.get(function);
        }
        if (b != null) {
            if (reqState != 0) {
                b.setTypeface(null, Typeface.ITALIC);
            } else {
                b.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void setVolumeIndicator() {
        // hide or display volume control indicator based on variable
        tvVolT.setText("");
        tvVolS.setText("");
        tvVolG.setText("");
        if (whichVolume == 'T') {
            tvVolT.setText(VOLUME_INDICATOR);
        } else if (whichVolume == 'S') {
            tvVolS.setText(VOLUME_INDICATOR);
        } else {
            tvVolG.setText(VOLUME_INDICATOR);
        }
        // Ensure ESU MCII tracks selected throttle
        if (IS_ESU_MCII) {
            Log.d("Engine_Driver", "ESU_MCII: Throttle changed to: " + whichVolume);
            setEsuThrottleKnobPosition(whichVolume, getSpeed(whichVolume));
            if (getConsist(whichVolume).isActive()) {
                esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.ON, true);
            } else {
                esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.STEADY_FLASH, true);
            }
        }
    }

    private void setGamepadIndicator() {
        // hide or display gamepad number indicator based on variable
        tvGamePadT.setText(gamePadThrottleAssignment[0]);
        tvGamePadS.setText(gamePadThrottleAssignment[1]);
        tvGamePadG.setText(gamePadThrottleAssignment[2]);
    }

    private void setNextActiveThrottle() {
        setNextActiveThrottle(false);
    }

    private void setNextActiveThrottle(boolean feedbackSound) {
        int i;
        int index = -1;
        int numThrottles = allThrottleLetters.length;

        // find current Volume throttle
        for (i = 0; i < numThrottles ; i++) {
            if (allThrottleLetters[i] == whichVolume) {
                index = i;
                break;
            }
        }
        // find next active throttle
        i = index+1;
        while (i != index) {                        // check until we get back to current Volume throttle
            if (i >= numThrottles) {                // wrap
                i = 0;
            } else {
                char whichT = allThrottleLetters[i];
                if (getConsist(whichT).isActive()) { // found next active throttle
                    whichVolume = whichT;
                    setVolumeIndicator();
                    break;  // done
                } else {                            // move to next throttle
                    i++;
                }
            }
        }
        // play appropriate feedbackSound
        if (feedbackSound) {
            GamepadFeedbackSound(i == index);
        }
    }


    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
//        Log.d("Engine_Driver", "dgme " + event.getAction());
        return super.dispatchGenericMotionEvent(event);
    }

    // setup the appropriate keycodes for the type of gamepad that has been selected in the preferences
    private void setGamepadKeys() {
        whichGamePadMode = prefs.getString("prefGamePadType", getApplicationContext().getResources().getString(R.string.prefGamePadTypeDefaultValue));
        prefGamePadMultipleDevices = prefs.getBoolean("prefGamePadMultipleDevices", getResources().getBoolean(R.bool.prefGamePadMultipleDevicesDefaultValue));

        // Gamepad button Preferences
        prefGamePadButtons[0] = prefs.getString("prefGamePadButtonStart", getApplicationContext().getResources().getString(R.string.prefGamePadButtonStartDefaultValue));
        prefGamePadButtons[1] = prefs.getString("prefGamePadButton1", getApplicationContext().getResources().getString(R.string.prefGamePadButton1DefaultValue));
        prefGamePadButtons[2] = prefs.getString("prefGamePadButton2", getApplicationContext().getResources().getString(R.string.prefGamePadButton2DefaultValue));
        prefGamePadButtons[3] = prefs.getString("prefGamePadButton3", getApplicationContext().getResources().getString(R.string.prefGamePadButton3DefaultValue));
        prefGamePadButtons[4] = prefs.getString("prefGamePadButton4", getApplicationContext().getResources().getString(R.string.prefGamePadButton4DefaultValue));
        // Gamepad DPAD Preferences
        prefGamePadButtons[5] = prefs.getString("prefGamePadButtonUp", getApplicationContext().getResources().getString(R.string.prefGamePadButtonUpDefaultValue));
        prefGamePadButtons[6] = prefs.getString("prefGamePadButtonRight", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightDefaultValue));
        prefGamePadButtons[7] = prefs.getString("prefGamePadButtonDown", getApplicationContext().getResources().getString(R.string.prefGamePadButtonDownDefaultValue));
        prefGamePadButtons[8] = prefs.getString("prefGamePadButtonLeft", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftDefaultValue));

        if (!whichGamePadMode.equals("None")) {
            // make sure the Softkeyboard is hidden
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

        int[] bGamePadKeys;
        int[] bGamePadKeysUp;

        switch (whichGamePadMode) {
            case "iCade+DPAD":
            case "iCade+DPAD-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadiCadePlusDpad);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadiCadePlusDpad_UpCodes);
                break;
            case "MTK":
            case "MTK-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMTK);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Game":
            case "Game-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGame);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "VRBoxA":
            case "VRBoxA-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadVRBoxA);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "VRBoxC":
            case "VRBoxC-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadVRBoxiC);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadVRBoxiC_UpCodes);
                break;
            case "VRBoxiC":
            case "VRBoxiC-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadVRBoxiC);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadVRBoxiC_UpCodes);
                break;
            default: // "iCade" or iCade-rotate
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadiCade);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadiCade_UpCodes);
                break;
        }
        // now grab the keycodes and put them into the arrays that will actually be used.
        for (int i = 0; i<=10; i++ ) {
            gamePadKeys[i] = bGamePadKeys[i];
            gamePadKeys_Up[i] = bGamePadKeysUp[i];
        }

        // if the preference name has "-rotate" at the end of it swap the dpad buttons around
        if (whichGamePadMode.contains("-rotate")) {
            gamePadKeys[2] = bGamePadKeys[4];
            gamePadKeys[3] = bGamePadKeys[5];
            gamePadKeys[4] = bGamePadKeys[3];
            gamePadKeys[5] = bGamePadKeys[2];

            gamePadKeys_Up[2] = bGamePadKeysUp[4];
            gamePadKeys_Up[3] = bGamePadKeysUp[5];
            gamePadKeys_Up[4] = bGamePadKeysUp[3];
            gamePadKeys_Up[5] = bGamePadKeysUp[2];
        }
    }

    //
    private int swapToNextAvilableThrottleForGamePad(int fromThrottle, boolean quiet) {
        int whichThrottle = -1;
        int index;
        index = fromThrottle -1;

        // need to count through all the throttle, but need to start from the current one, not zero
        if (prefGamePadMultipleDevices) {  // deal with multiple devices if the preference is set
            for (int i = 0; i < allThrottleLetters.length; i++) {
                index++;
                if (index>=allThrottleLetters.length) {
                    index=0;
                }
                if (gamePadIds[index] == 0) {  // unassigned
                    if (getConsist(allThrottleLetters[index]).isActive()) { // found next active throttle
                        if (gamePadIds[index] <= 0) { //not currently assigned
                            whichThrottle = index;
                            break;  // done
                        }
                    }
                }
            }
            if (whichThrottle>=0) {
                gamePadIds[whichThrottle] = gamePadIds[fromThrottle];
                gamePadThrottleAssignment[whichThrottle] = gamePadThrottleAssignment[fromThrottle];
                gamePadIds[fromThrottle] = 0;
                gamePadThrottleAssignment[fromThrottle] = "";
                setGamepadIndicator();
            }
        }
        if (whichThrottle==-1) {
            if (!quiet) {
                GamepadFeedbackSound(true);
            }
            return fromThrottle;  // didn't work. leave it alone
        } else {
            if (!quiet) {
                GamepadFeedbackSound(false);
            }
            return whichThrottle;
        }
    }

    // work out a) if we need to look for multiple gamepads b) workout which gamepad we received the key event from
    private int findWhichGamePadEventIsFrom(KeyEvent event) {
        int whichGamePad = -2;  // default to the event not from a gamepad
        int whichGamePadDeviceId = -1;
        int j;

        if ((event.getKeyCode()!=KEYCODE_VOLUME_UP)&&(event.getKeyCode()!=KEYCODE_VOLUME_DOWN)&&(event.getKeyCode()!=KEYCODE_BACK)) { // if it is a volume key or the back assume it did not come form a gamepad
            if (prefGamePadMultipleDevices) {  // deal with multiple devices if the preference is set
                int gamePadDeviceId = event.getDeviceId();

                if (gamePadDeviceId >= 0) { // event is from a gamepad (or at least not from a screen touch)
                    whichGamePad = -1;  // gamepad

                    String reassigningGamepad = "X";
                    int i;
                    int numThrottles = allThrottleLetters.length;
                    // find out if this gamepad is alread assigned
                    for (i = 0; i < numThrottles; i++) {
                        if (gamePadIds[i] == gamePadDeviceId) {
                            if (getConsist(allThrottleLetters[i]).isActive()) { //found the throttle and it is active
                                whichGamePad = i;
                            } else { // currently assigned to this throttle, but the throttle is not active
                                whichGamePad = i;
                                gamePadIds[i] = 0;
                                reassigningGamepad = gamePadThrottleAssignment[i];
                                gamePadThrottleAssignment[i] = " ";
                                setGamepadIndicator(); // need to clear the indicator
                            }
                            break;
                        }
                    }

                    if (whichGamePad == -1) { //didn't find it OR is known, but unassigned

                        for (j = 0; j < gamepadCount; j++) {
                            if (gamePadDeviceIds[j] == gamePadDeviceId) { // known, but unassigned
                                whichGamePadDeviceId = j;
                                break;
                            }
                        }
                        if (whichGamePadDeviceId == -1) { // previously unseen gamepad
                            gamepadCount++;
                            gamePadDeviceIds[gamepadCount - 1] = gamePadDeviceId;
                            whichGamePadDeviceId = gamepadCount - 1;
                        }

                        for (i = 0; i < numThrottles; i++) {
                            if (gamePadIds[i] == 0) {  // throttle is not assigned a gamepad
                                if (getConsist(allThrottleLetters[i]).isActive()) { // found next active throttle
                                    gamePadIds[i] = gamePadDeviceId;
                                    if (reassigningGamepad.equals("X")) { // not a reassignment
                                        gamePadThrottleAssignment[i] = GAMEPAD_INDICATOR[whichGamePadDeviceId];
                                    } else { // reasigning
                                        gamePadThrottleAssignment[i] = reassigningGamepad;
                                    }
                                    whichGamePad = i;
                                    setGamepadIndicator();
                                    break;  // done
                                }
                            }
                        }
                    }
                }
                if (gamepadCount > 0) {
                    usingMultiplePads = true;
                }
            }
        }
        return whichGamePad;
    }

    // map the button pressed to the user selected action for that button on the gamepad
    private void performButtonAction(int buttonNo, int action, boolean isActive, char whichThrottle, int whichGamePadIsEventFrom, int repeatCnt) {
        String x =prefGamePadButtons[buttonNo];

        if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_ALL_STOP)) {  // All Stop
            if (isActive && (action==ACTION_DOWN) && (repeatCnt == 0)) {
                GamepadFeedbackSound(false);
                speedUpdateAndNotify(0);         // update all three throttles
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_STOP)) {  // Stop
            if (isActive && (action==ACTION_DOWN) && (repeatCnt == 0)) {
                GamepadFeedbackSound(false);
                speedUpdateAndNotify(whichThrottle, 0);
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_NEXT_THROTTLE)) {  // Next Throttle
            if (isActive && (action==ACTION_DOWN) && (repeatCnt == 0)) {
                if ( usingMultiplePads && whichGamePadIsEventFrom >= 0) {
                    whichGamePadIsEventFrom = swapToNextAvilableThrottleForGamePad(whichGamePadIsEventFrom, false);
                } else {
                    setNextActiveThrottle(true);
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_FORWARD)) {  // Forward
            if (isActive && (action==ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                GamepadFeedbackSound(dirChangeFailed);
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_REVERSE)) {  // Reverse
            if (isActive && (action==ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                GamepadFeedbackSound(dirChangeFailed);
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_FORWARD_REVERSE_TOGGLE)) {  // Toggle Forward/Reverse
            if (isActive && (action==ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed;
                if ((getDirection(whichThrottle)==DIRECTION_FORWARD)) {
                    dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                } else {
                    dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                }
                GamepadFeedbackSound(dirChangeFailed);
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_INCREASE_SPEED)) {  // Increase Speed
            if (isActive && (action == ACTION_DOWN)) {
                if (repeatCnt == 0) {
/*                    GamepadIncrementSpeed(whichThrottle);
                }
                // if longpress, start repeater
                else if (repeatCnt == 1) {
*/
                    mGamepadAutoIncrement = true;
                    gamepadRepeatUpdateHandler.post(new GamepadRptUpdater(whichThrottle));
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_DECREASE_SPEED)) {  // Decrease Speed
            if (isActive && (action == ACTION_DOWN)) {
                if (repeatCnt == 0) {
/*                    GamepadDecrementSpeed(whichThrottle);
                }
                // if longpress, start repeater
                else if (repeatCnt == 1) {
*/
                    mGamepadAutoDecrement = true;
                    gamepadRepeatUpdateHandler.post(new GamepadRptUpdater(whichThrottle));
                }
            }
        } else if ((prefGamePadButtons[buttonNo].length()>=11) && (prefGamePadButtons[buttonNo].substring(0,9).equals("Function "))) { // one of the Function Buttons
            int fKey = Integer.parseInt(prefGamePadButtons[buttonNo].substring(10,11));
            if (isActive && (repeatCnt == 0)) {
                if (action==ACTION_DOWN) {
                    GamepadFeedbackSound(false);
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, whichThrottle + "", fKey, 1);
                } else {
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, whichThrottle + "", fKey, 0);
                }
            }

        }
    }

        // listener for physical keyboard events
    // used to support the gamepad only   DPAD and key events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!whichGamePadMode.equals("None")) { // respond to the gamepad and keyboard inputs only if the preference is set
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            int repeatCnt = event.getRepeatCount();
            char whichThrottle;
            int whichGamePadIsEventFrom = findWhichGamePadEventIsFrom(event);

            if ((usingMultiplePads) && (whichGamePadIsEventFrom >= -1)) { // we have multiple gamepads AND the preference is set to make use of them AND the event came for a gamepad
                if (whichGamePadIsEventFrom >= 0) {
                    whichThrottle = allThrottleLetters[whichGamePadIsEventFrom];
                } else {
                    GamepadFeedbackSound(true);
                    return (true);
                }
            } else {
                whichThrottle = whichVolume;  // work out which throttle the volume keys are currently set to contol... and use that one
            }

            boolean isActive = getConsist(whichThrottle).isActive();

            if (keyCode != 0) {
                Log.d("Engine_Driver", "keycode " + keyCode + " action " + action + " repeat " + repeatCnt);
            }

            if (action == ACTION_UP) {
                mGamepadAutoIncrement = false;
                mGamepadAutoDecrement = false;
                GamepadFeedbackSoundStop();
            }

            if (keyCode == gamePadKeys[2]) { // DPAD Up Button
                performButtonAction(5, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys[3]) { // DPAD Down Button
                performButtonAction(7, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys[4]) { // DPAD Left Button
                performButtonAction(8, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys[5]) { // DPAD Right Button
                performButtonAction(6, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys[7]) { // ios button
                performButtonAction(1, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys_Up[8]) { // X button
                performButtonAction(3, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys_Up[9]) { // Triangle button
                performButtonAction(2, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys_Up[10]) { // @ button
                performButtonAction(4, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys[6]) { // start button
                performButtonAction(0, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                return (true); // stop processing this key

            } else if (keyCode == gamePadKeys[1]) {
                // NextThrottle
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    if (usingMultiplePads && whichGamePadIsEventFrom >= 0) {
                        whichGamePadIsEventFrom = swapToNextAvilableThrottleForGamePad(whichGamePadIsEventFrom, false);
                    } else {
                        setNextActiveThrottle(true);
                    }
                }
                return (true); // stop processing this key
            }
        } else if(IS_ESU_MCII) {
            // Process ESU MCII keys
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            int repeatCnt = event.getRepeatCount();
            boolean isActive = getConsist(whichVolume).isActive();

            if (keyCode != 0) {
                Log.d("Engine_Driver", "ESU_MCII: keycode " + keyCode + " action " + action + " repeat " + repeatCnt);
                if (action == ACTION_UP) {
                    esuButtonAutoIncrement = false;
                    esuButtonAutoDecrement = false;
                }

                switch (keyCode) {
                    case MobileControl2.KEYCODE_TOP_LEFT:
                    case MobileControl2.KEYCODE_BOTTOM_LEFT:
                    case MobileControl2.KEYCODE_TOP_RIGHT:
                    case MobileControl2.KEYCODE_BOTTOM_RIGHT:
                        performEsuMc2ButtonAction(keyCode, action, isActive, whichVolume, repeatCnt);
                        return true; // stop processing this key
                    default:
                        // Unrecognised key - do nothing
                        Log.d("Engine_Driver", "ESU_MCII: Unrecognised keyCode: " + keyCode);

                }
            }
//  for now pass all keystrokes not in gamePadKeys[] to super
//  if problems occur, we can uncomment the next 2 lines
//            else if (!((keyCode == KEYCODE_BACK) || (keyCode == KEYCODE_VOLUME_DOWN) || (keyCode == KEYCODE_VOLUME_UP) || (keyCode == KEYCODE_MENU)))
//            return (true); // swallow all other keystrokes except back, menu and the volume keys
        }
        return super.dispatchKeyEvent(event);
    }

    void GamepadIncrementSpeed(char whichThrottle) {
        incrementSpeed(whichThrottle);
        GamepadFeedbackSound(atMaxSpeed(whichThrottle));
    }

    void GamepadDecrementSpeed(char whichThrottle) {
        decrementSpeed(whichThrottle);
        GamepadFeedbackSound(atMinSpeed(whichThrottle));
    }

    void GamepadFeedbackSound(boolean invalidAction) {
        if (invalidAction)
            tg.startTone(ToneGenerator.TONE_PROP_NACK);
        else
            tg.startTone(ToneGenerator.TONE_PROP_BEEP);
    }

    void GamepadFeedbackSoundStop() {
        tg.stopTone();
    }

    // For gamepad speed buttons.
    private class GamepadRptUpdater implements Runnable {
        char whichThrottle;

        private GamepadRptUpdater(char WhichThrottle) {
            whichThrottle = WhichThrottle;

            try {
                //REP_DELAY = Integer.parseInt(prefs.getString("speed_arrows_throttle_repeat_delay", "100"));
                REP_DELAY = Integer.parseInt(prefs.getString("prefGamePadSpeedArrowsThrottleRepeatDelay",  getApplicationContext().getResources().getString(R.string.prefGamePadSpeedButtonsRepeatDefaultValue)));
            } catch (NumberFormatException ex) {
                REP_DELAY = 300;
            }
        }

        @Override
        public void run() {
            if (mGamepadAutoIncrement) {
                GamepadIncrementSpeed(whichThrottle);
                gamepadRepeatUpdateHandler.postDelayed(new GamepadRptUpdater(whichThrottle), REP_DELAY);
            } else if (mGamepadAutoDecrement) {
                GamepadDecrementSpeed(whichThrottle);
                gamepadRepeatUpdateHandler.postDelayed(new GamepadRptUpdater(whichThrottle), REP_DELAY);
            }
        }
    }

    // For volume speed buttons.
    private class volumeKeysRptUpdater implements Runnable {
        char whichThrottle;

        private volumeKeysRptUpdater(char WhichThrottle) {
            whichThrottle = WhichThrottle;

            try {
                REP_DELAY = Integer.parseInt(prefs.getString("speed_arrows_throttle_repeat_delay", "100"));
            } catch (NumberFormatException ex) {
                REP_DELAY = 100;
            }
        }

        @Override
        public void run() {
            if (mVolumeKeysAutoIncrement) {
                incrementSpeed(whichThrottle);
                volumeKeysRepeatUpdateHandler.postDelayed(new volumeKeysRptUpdater(whichThrottle), REP_DELAY);
            } else if (mVolumeKeysAutoDecrement) {
                decrementSpeed(whichThrottle);
                volumeKeysRepeatUpdateHandler.postDelayed(new volumeKeysRptUpdater(whichThrottle), REP_DELAY);
            }
        }
    }

    /**
     * For ESU MCII buttons.
     *
     * However, it seems that the ESU MCII buttons report UP immediately even if held down
     * so this, right now, is rather superfluous...
     */
    private class EsuMc2ButtonRptUpdater implements Runnable {
        char whichThrottle;

        private EsuMc2ButtonRptUpdater(char whichThrottle) {
            this.whichThrottle = whichThrottle;

            try {
                REP_DELAY = Integer.parseInt(prefs.getString("prefEsuMc2ButtonsRepeatDelay",
                        getApplicationContext().getResources().getString(R.string.prefEsuMc2ButtonsRepeatDefaultValue)));
            } catch (NumberFormatException ex) {
                REP_DELAY = 300;
            }
        }

        @Override
        public void run() {
            if (esuButtonAutoIncrement) {
                incrementSpeed(whichThrottle);
                esuButtonRepeatUpdateHandler.postDelayed(new EsuMc2ButtonRptUpdater(whichThrottle), REP_DELAY);
            } else if (esuButtonAutoDecrement) {
                decrementSpeed(whichThrottle);
                gamepadRepeatUpdateHandler.postDelayed(new EsuMc2ButtonRptUpdater(whichThrottle), REP_DELAY);
            }
        }
    }

    // Callback for ESU MCII throttle knob events
    private ThrottleFragment.OnThrottleListener esuOnThrottleListener = new ThrottleFragment.OnThrottleListener() {

        @Override
        public void onButtonDown() {
            Log.d("Engine_Driver", "ESU_MCII: Knob button down for throttle " + whichVolume);
            Log.d("Engine_Driver", "ESU_MCII: Attempting to switch direction");
            changeDirectionIfAllowed(whichVolume, (getDirection(whichVolume) == 1 ? 0 : 1 ));
        }

        @Override
        public void onButtonUp() {
            Log.d("Engine_Driver", "ESU_MCII: Knob button up for throttle " + whichVolume);
        }

        @Override
        public void onPositionChanged(int knobPos) {
            int speed;
            if (getConsist(whichVolume).isActive() && !isEsuMc2Stopped) {
                if (whichVolume == 'T') {
                    speed = esuThrottleScaleT.positionToStep(knobPos);
                } else if (whichVolume == 'G') {
                    speed = esuThrottleScaleG.positionToStep(knobPos);
                } else {
                    speed = esuThrottleScaleS.positionToStep(knobPos);
                }
                Log.d("Engine_Driver", "ESU_MCII: Knob position changed for throttle " + whichVolume);
                Log.d("Engine_Driver", "ESU_MCII: New knob position: " + knobPos + " ; speedstep: " + speed);
                speedUpdateAndNotify(whichVolume, speed);
            } else {
                // Ignore knob movements for stopped or inactive throttles
                Log.d("Engine_Driver", "ESU_MCII: Knob position moved for " + (isEsuMc2Stopped ? "stopped": "inactive") + " throttle " + whichVolume);
                Log.d("Engine_Driver", "ESU_MCII: Nothing updated");
            }
        }
    };

    // Callback for ESU MCII stop button
    private StopButtonFragment.OnStopButtonListener esuOnStopButtonListener = new StopButtonFragment.OnStopButtonListener() {
        private int origSpeed;
        private long timePressed;
        private boolean wasLongPress = false;
        private int delay;

        @Override
        public void onStopButtonDown() {
            if (!getConsist(whichVolume).isActive()) {
                Log.d("Engine_Driver", "ESU_MCII: Stop button down for inactive throttle " + whichVolume);
                return;
            }
            Log.d("Engine_Driver", "ESU_MCII: Stop button down for throttle " + whichVolume);
            if (!isEsuMc2Stopped) {
                origSpeed = getSpeed(whichVolume);
                timePressed = System.currentTimeMillis();
                Log.d("Engine_Driver", "ESU_MCII: Speed value was: " + origSpeed);
            }
            // Toggle press status
            isEsuMc2Stopped = !isEsuMc2Stopped;
            setEsuThrottleKnobPosition(whichVolume, 0);
            set_stop_button(whichVolume, true);
            speedUpdateAndNotify(whichVolume, 0);
            esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.ON);
            esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
            // Read current stop button delay pref value
            delay = preferences.getIntPrefValue(prefs,"prefEsuMc2StopButtonDelay",
                    getApplicationContext().getResources().getString(R.string.prefEsuMc2StopButtonDelayDefaultValue));
        }

        @Override
        public void onStopButtonUp() {
            if (!getConsist(whichVolume).isActive()) {
                Log.d("Engine_Driver", "ESU_MCII: Stop button up for inactive throttle " + whichVolume);
                return;
            }
            Log.d("Engine_Driver", "ESU_MCII: Stop button up for throttle " + whichVolume);
            if (isEsuMc2Stopped) {
                if (System.currentTimeMillis() - timePressed > delay) {
                    // It's a long initial press so record this
                    wasLongPress = true;
                    Log.d("Engine_Driver", "ESU_MCII: Stop button press was long - long flash Red LED");
                    esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.LONG_FLASH);
                    esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
                    // Set all throttles to zero
                    for (char t : allThrottleLetters) {
                        set_stop_button(t, true);
                        speedUpdateAndNotify(t, 0);
                    }
                } else {
                    wasLongPress = false;
                    Log.d("Engine_Driver", "ESU_MCII: Stop button press was short - short flash Red LED");
                    esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.QUICK_FLASH);
                    esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
                }
            } else {
                if (!wasLongPress) {
                    Log.d("Engine_Driver", "ESU_MCII: Revert speed value to: " + origSpeed);
                    setEsuThrottleKnobPosition(whichVolume, origSpeed);
                    set_stop_button(whichVolume, false);
                    speedUpdateAndNotify(whichVolume, origSpeed);
                } else {
                    Log.d("Engine_Driver", "ESU_MCII: Resume control without speed revert");
                    origSpeed = 0;
                    for (char t : allThrottleLetters) {
                        set_stop_button(t, false);
                    }
                }
                // Revert LED states
                esuMc2Led.revertLEDStates();
            }
        }
    };

    // Function to move ESU MCII control knob
    private void setEsuThrottleKnobPosition(char whichThrottle, int speed) {
        Log.d("Engine_Driver", "ESU_MCII: Request to update knob position for throttle " + whichThrottle);
        if (whichThrottle == whichVolume) {
            int knobPos;
            if (whichThrottle == 'T') {
                knobPos = esuThrottleScaleT.stepToPosition(speed);
            } else if (whichThrottle == 'G') {
                knobPos = esuThrottleScaleG.stepToPosition(speed);
            } else {
                knobPos = esuThrottleScaleS.stepToPosition(speed);
            }
            Log.d("Engine_Driver", "ESU_MCII: Update knob position for throttle " + whichThrottle);
            Log.d("Engine_Driver", "ESU_MCII: New knob position: " + knobPos + " ; speedstep: " + speed);
            esuThrottleFragment.moveThrottle(knobPos);
        } else {
            Log.d("Engine_Driver", "ESU_MCII: This throttle not selected for control by knob");
        }
    }

    private void performEsuMc2ButtonAction(int buttonNo, int action, boolean isActive, char whichThrottle, int repeatCnt) {

        EsuMc2ButtonAction buttonAction;

        switch (buttonNo) {
            case MobileControl2.KEYCODE_TOP_LEFT:
                buttonAction = EsuMc2ButtonAction.getAction(prefs.getString("prefEsuMc2ButtonTL",
                        getApplicationContext().getResources().getString(R.string.prefEsuMc2ButtonTLDefaultValue)));
                break;
            case MobileControl2.KEYCODE_BOTTOM_LEFT:
                buttonAction = EsuMc2ButtonAction.getAction(prefs.getString("prefEsuMc2ButtonBL",
                        getApplicationContext().getResources().getString(R.string.prefEsuMc2ButtonBLDefaultValue)));
                break;
            case MobileControl2.KEYCODE_TOP_RIGHT:
                buttonAction = EsuMc2ButtonAction.getAction(prefs.getString("prefEsuMc2ButtonTR",
                        getApplicationContext().getResources().getString(R.string.prefEsuMc2ButtonTRDefaultValue)));
                break;
            case MobileControl2.KEYCODE_BOTTOM_RIGHT:
                buttonAction = EsuMc2ButtonAction.getAction(prefs.getString("prefEsuMc2ButtonBR",
                        getApplicationContext().getResources().getString(R.string.prefEsuMc2ButtonBRDefaultValue)));
                break;
            default:
                buttonAction = EsuMc2ButtonAction.NO_ACTION;
        }

        if (isActive) {
            switch (buttonAction) {
                case NO_ACTION:
                    // Do nothing
                    break;
                case ALL_STOP:
                    if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                        setEsuThrottleKnobPosition(whichThrottle, 0);
                        speedUpdateAndNotify(0);    // update all three throttles
                    }
                    break;
                case STOP:
                    if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                        setEsuThrottleKnobPosition(whichThrottle, 0);
                        speedUpdateAndNotify(whichThrottle, 0);
                    }
                    break;
                case DIRECTION_FORWARD:
                    if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                        changeDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                    }
                    break;
                case DIRECTION_REVERSE:
                    if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                        changeDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                    }
                    break;
                case DIRECTION_TOGGLE:
                    if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                        if ((getDirection(whichThrottle) == DIRECTION_FORWARD)) {
                            changeDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                        } else {
                            changeDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                        }
                    }
                    break;
                case SPEED_INCREASE:
                    if (action == ACTION_DOWN) {
                        if (repeatCnt == 0) {
                            esuButtonAutoIncrement = true;
                            esuButtonRepeatUpdateHandler.post(new EsuMc2ButtonRptUpdater(whichThrottle));
                        }
                    }
                        break;
                case SPEED_DECREASE:
                    if (action == ACTION_DOWN) {
                        if (repeatCnt == 0) {
                            esuButtonAutoDecrement = true;
                            esuButtonRepeatUpdateHandler.post(new EsuMc2ButtonRptUpdater(whichThrottle));
                        }
                    }
                    break;
                case NEXT_THROTTLE:
                    if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                        setNextActiveThrottle();
                    }
                    break;
                case FN00: case FN01: case FN02: case FN03: case FN04: case FN05:
                case FN06: case FN07: case FN08: case FN09: case FN10: case FN11:
                case FN12: case FN13: case FN14: case FN15: case FN16: case FN17:
                case FN18: case FN19: case FN20: case FN21: case FN22: case FN23:
                case FN24: case FN25: case FN26: case FN27: case FN28:
                    if (repeatCnt == 0) {
                        if (action == ACTION_DOWN) {
                            mainapp.sendMsg(mainapp.comm_msg_handler,
                                    message_type.FUNCTION,
                                    whichThrottle + "",
                                    buttonAction.getFunction(),
                                    1);
                        } else {
                            mainapp.sendMsg(mainapp.comm_msg_handler,
                                    message_type.FUNCTION,
                                    whichThrottle + "",
                                    buttonAction.getFunction(),
                                    0);
                        }
                    }
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
    }

// Listeners for the Select Loco buttons
    private class select_function_button_touch_listener implements View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {
        char whichThrottle;     // T for first throttle, S for second, G for third

        private select_function_button_touch_listener(char new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @Override
        public void onClick(View v) {
            // don't loco change while moving if the preference is set
            if (isSelectLocoAllowed(whichThrottle)) {
                start_select_loco_activity(whichThrottle); // pass throttle #
            } else {
                Toast.makeText(getApplicationContext(), "Loco change not allowed: 'Direction change while moving' is disabled in the preferences", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean onLongClick(View v) {
            start_consist_lights_edit(whichThrottle);

            return true;
        }


    //TODO: This onTouch may be redundant now that the gesture overlay is working better

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int pos[] = new int[2];
            v.getLocationOnScreen(pos);

            // if gesture in progress, we may need to skip button processing
            if (gestureInProgress) {
                // Log.d("Engine_Driver", "onTouch " + "Gesture- currentY: " + (pos[1]+event.getY()) + " startY: " + gestureStartY);
                //check to see if we have a substantial vertical movement
                if (Math.abs(pos[1] + event.getY() - gestureStartY) > (threaded_application.min_fling_distance)) {
                    // Log.d("Engine_Driver", "onTouch " + "Gesture - long - cY: " + (pos[1]+event.getY()) + " sY: " + gestureStartY);
                    return true;
                }
            }
            return false;
        }

    }

    //listeners for the increase/decrease speed buttons (not the slider)
    private class arrow_speed_button_touch_listener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
        char whichThrottle;     // T for first throttle, S for second, G for third
        String arrowDirection;

        private arrow_speed_button_touch_listener(char new_whichThrottle, String new_arrowDirection) {
            whichThrottle = new_whichThrottle;
            arrowDirection = new_arrowDirection;
        }

        @Override
        public boolean onLongClick(View v) {
            if (arrowDirection.equals("right")) {
                mAutoIncrement = true;
            } else {
                mAutoDecrement = true;
            }
            repeatUpdateHandler.post(new RptUpdater(whichThrottle));
            return false;
        }

        @Override
        public void onClick(View v) {
            if (arrowDirection.equals("right")) {
                mAutoIncrement = false;
                incrementSpeed(whichThrottle);
            } else {
                mAutoDecrement = false;
                decrementSpeed(whichThrottle);
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoIncrement) {
                mAutoIncrement = false;
            }

            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoDecrement) {
                mAutoDecrement = false;
            }
            return false;
        }
    }

    private class function_button_touch_listener implements View.OnTouchListener {
        int function;
        char whichThrottle;     // T for first throttle, S for second, G for third
        boolean leadOnly;       // function only applies to the lead loco
        boolean trailOnly;      // function only applies to the trail loco (future)
        boolean followLeadFunction;       // function only applies to the locos that have been set to follow the function

        private function_button_touch_listener(int new_function, char new_whichThrottle) {
            this(new_function, new_whichThrottle, "");
        }

        private function_button_touch_listener(int new_function, char new_whichThrottle, String funcLabel) {
            function = new_function;    // store these values for this button
            whichThrottle = new_whichThrottle;
            String lab = funcLabel.toUpperCase().trim();
            leadOnly = false;
            trailOnly = false;
            followLeadFunction = false;

            if (!lab.equals("")) {
                boolean selectiveLeadSound = prefs.getBoolean("SelectiveLeadSound", getResources().getBoolean(R.bool.prefSelectiveLeadSoundDefaultValue));
                leadOnly = (selectiveLeadSound &&
                        (lab.contains("WHISTLE") || lab.contains("HORN") || lab.contains("BELL"))
                        || lab.contains("HEAD")
                        || (lab.contains("LIGHT") && !lab.contains("REAR")));
                followLeadFunction = (lab.contains("LIGHT"));
                trailOnly = lab.contains("REAR");
            }
        }


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Log.d("Engine_Driver", "onTouch func " + function + " action " +
            // event.getAction());

            // make the click sound once
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.playSoundEffect(SoundEffectConstants.CLICK);
            }

            // if gesture in progress, skip button processing
            if (gestureInProgress) {
                return (true);
            }

            // if gesture just failed, insert one DOWN event on this control
            if (gestureFailed) {
                handleAction(MotionEvent.ACTION_DOWN);
                gestureFailed = false;  // just do this once
            }
            handleAction(event.getAction());
            return (true);
        }

        private void handleAction(int action) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    switch (this.function) {
                        case function_button.FORWARD:
                            changeDirectionIfAllowed(whichThrottle,1);
                            break;
                        case function_button.REVERSE: {
                             changeDirectionIfAllowed(whichThrottle,0);
                            break;
                        }
                        case function_button.STOP:
                            set_stop_button(whichThrottle, true);
                            speedUpdateAndNotify(whichThrottle, 0);
                            if (IS_ESU_MCII) {
                                setEsuThrottleKnobPosition(whichThrottle, 0);
                            }
                            break;
                        case function_button.SPEED_LABEL:  // specify which throttle the volume button controls
                            if (getConsist(whichThrottle).isActive()) { // only assign if Active
                                whichVolume = whichThrottle;
                                set_labels();
                            }
                            break;

                        default: { // handle the function buttons
                            Consist con;
                            if (whichThrottle == 'T') {
                                con = mainapp.consistT;
                            } else if (whichThrottle == 'G') {
                                con = mainapp.consistG;
                            } else {
                                con = mainapp.consistS;
                            }

                            String addr = "";
                            if (leadOnly)
                                addr = con.getLeadAddr();
// ***future                else if (trailOnly)
//                              addr = con.getTrailAddr();

                            //mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, whichThrottle + addr, this.function, 1);
                            mainapp.toggleFunction(whichThrottle + addr, this.function);
                            // set_function_request(whichThrottle, function, 1);

                            if(followLeadFunction) {
                                for (Consist.ConLoco l : con.getLocos()) {
                                    if (!l.getAddress().equals(con.getLeadAddr())) {  // ignore the lead as we have already set it
                                        if (l.isLightOn() == LIGHT_FOLLOW) {
                                            mainapp.toggleFunction(whichThrottle + l.getAddress(), this.function);
                                        }
                                    }
                                }
                            }

                            break;
                        }
                    }
                    break;
                }

                // handle stopping of function on key-up
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    if (function == function_button.STOP) {
                        set_stop_button(whichThrottle, false);
                    }
                    // only process UP event if this is a "function" button
                    else if (function < function_button.FORWARD) {
                        Consist con;
                        if (whichThrottle == 'T') {
                            con = mainapp.consistT;
                        } else if (whichThrottle == 'G') {
                            con = mainapp.consistG;
                        } else {
                            con = mainapp.consistS;
                        }
                        String addr = "";
                        if (leadOnly)
                            addr = con.getLeadAddr();
// ***future            else if (trailOnly)
//                          addr = con.getTrailAddr();
                       mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, whichThrottle + addr, function, 0);
                        // set_function_request(whichThrottle, function, 0);
                    }
                    break;
            }
        }
    }

    //Listeners for the throttle slider
    private class throttle_listener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        char whichThrottle;
        int lastSpeed;
        boolean limitedJump;
        int jumpSpeed;

        private throttle_listener(char new_whichThrottle) {
            whichThrottle = new_whichThrottle; // store values for this listener
            lastSpeed = 0;
            limitedJump = false;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Log.d("Engine_Driver", "onTouchThrot action " + event.getAction());
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar throttle, int speed, boolean fromUser) {
            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if (fromUser) {
                if (!limitedJump) {         // touch generates multiple onProgressChanged events, skip processing after first limited jump
                    if ((speed - lastSpeed) > max_throttle_change) {    // if jump is too large then limit it
                        // Log.d("Engine_Driver", "onProgressChanged -- throttling change");
                        mAutoIncrement = true;  // advance slowly
                        jumpSpeed = speed;      // save ultimate target value
                        limitedJump = true;
                        throttle.setProgress(lastSpeed);
                        repeatUpdateHandler.post(new RptUpdater(whichThrottle));
                        return;
                    }
                    sendSpeedMsg(whichThrottle, speed);
                    setDisplayedSpeed(whichThrottle, speed);
                }
                else {                      // got a touch while processing limitJump
                    speed = lastSpeed;    //   so suppress multiple touches
                    throttle.setProgress(lastSpeed);
                }
                // Now update ESU MCII Knob position
                if (IS_ESU_MCII) {
                    setEsuThrottleKnobPosition(whichThrottle, speed);
                }
            } else {
                if (limitedJump) {
                    if (speed >= jumpSpeed) {   // stop when we reach the target
                        mAutoIncrement = false;
                        limitedJump = false;
                        throttle.setProgress(jumpSpeed);
                    }
                }
                setDisplayedSpeed(whichThrottle, speed);
            }
            if (speed == 0 || lastSpeed == 0) {     // check rules when going to/from 0 speed
                applySpeedRelatedOptions(whichThrottle);
            }
            lastSpeed = speed;
        }

        @Override
        public void onStartTrackingTouch(SeekBar sb) {
            gestureInProgress = false;
            limitedJump = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sb) {
            mAutoIncrement = false;
        }
    }

    // send a throttle speed message to WiT
    public void sendSpeedMsg(char whichThrottle, int speed) {
        // start timer to briefly ignore WiT speed messages - avoids speed "jumping"
        if (whichThrottle == 'T') {
            changeTimerT.changeDelay();
        } else if (whichThrottle == 'G') {
            changeTimerG.changeDelay();
        } else {
            changeTimerS.changeDelay();
        }
        // send speed update to WiT
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.VELOCITY, "", whichThrottle, speed);
    }

    // implement delay for briefly ignoring WiT speed reports after sending a throttle speed update
    // this prevents use of speed reports sent by WiT just prior to WiT processing the speed update 
    private class ChangeDelay {
        boolean delayInProg;
        Runnable changeTimer;
        char whichThrottle;

        private ChangeDelay(char wThrot) {
            delayInProg = false;
            changeTimer = new ChangeTimer();
            whichThrottle = wThrot;
        }

        private void changeDelay() {
            mainapp.throttle_msg_handler.removeCallbacks(changeTimer);          //remove any pending requests
            delayInProg = true;
            mainapp.throttle_msg_handler.postDelayed(changeTimer, changeDelay);
        }

        class ChangeTimer implements Runnable {

            private ChangeTimer() {
                delayInProg = false;
            }

            @Override
            public void run() {         // change delay is over - clear the delay flag
                delayInProg = false;
            }
        }
    }
    

    /*
     * private void requestSpeedMsg(char whichThrottle) { // always load
     * whichThrottle into message mainapp.sendMsg(mainapp.comm_msg_handler,
     * message_type.REQ_VELOCITY, "", (int) whichThrottle); }
     */

    // set the title, optionally adding the current time.
    public void setTitle() {
        if (mainapp.displayClock)
            setTitle(getApplicationContext().getResources().getString(R.string.app_name) + "  " + currentTime);
        else
            setTitle(getApplicationContext().getResources().getString(R.string.app_name_throttle));
    }

    @SuppressLint({"Recycle", "SetJavaScriptEnabled"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // restore the requested throttle direction so we can update the
            // direction indication while we wait for an update from WiT
            if (savedInstanceState.getSerializable("dirT") != null)
                dirT = (int) savedInstanceState.getSerializable("dirT");
            if (savedInstanceState.getSerializable("dirS") != null)
                dirS = (int) savedInstanceState.getSerializable("dirS");
            if (savedInstanceState.getSerializable("dirT") != null)
                dirG = (int) savedInstanceState.getSerializable("dirG");
        }

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        if (mainapp.isForcingFinish()) { // expedite
            return;
        }

        setContentView(R.layout.throttle);

        speedButtonLeftText = getApplicationContext().getResources().getString(R.string.LeftButton);
        speedButtonRightText = getApplicationContext().getResources().getString(R.string.RightButton);
        speedButtonUpText = getApplicationContext().getResources().getString(R.string.UpButton);
        speedButtonDownText = getApplicationContext().getResources().getString(R.string.DownButton);

        prefSwapForwardReverseButtons = prefs.getBoolean("prefSwapForwardReverseButtons", getResources().getBoolean(R.bool.prefSwapForwardReverseButtonsDefaultValue));

        if (prefSwapForwardReverseButtons) {
            DirectionButtonLeftText = getApplicationContext().getResources().getString(R.string.reverse);
            DirectionButtonRightText = getApplicationContext().getResources().getString(R.string.forward);
        } else {
            DirectionButtonLeftText = getApplicationContext().getResources().getString(R.string.forward);
            DirectionButtonRightText = getApplicationContext().getResources().getString(R.string.reverse);
        }

        webViewLocation = prefs.getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
        webViewIsOn = !webViewLocation.equals("none");
        keepWebViewLocation = webViewLocation;

        prefSwipeUpOption = prefs.getString("SwipeUpOption", getApplicationContext().getResources().getString(R.string.prefSwipeUpOptionDefaultValue));
        isScreenLocked = false;

        // get the screen brightness on create
        screenBrightnessOriginal = getScreenBrightness();
        screenBrightnessModeOriginal = getScreenBrightnessMode();

        // myGesture = new GestureDetector(this);
        GestureOverlayView ov = (GestureOverlayView) findViewById(R.id.throttle_overlay);
        ov.addOnGestureListener(this);
        ov.setGestureVisible(false);

        function_button_touch_listener fbtl;
        select_function_button_touch_listener sfbt;
        arrow_speed_button_touch_listener asbl;

        // set listener for select loco buttons
        bSelT = (Button) findViewById(R.id.button_select_loco_T);
        bSelT.setClickable(true);
        sfbt = new select_function_button_touch_listener('T');
        bSelT.setOnClickListener(sfbt);
        bSelT.setOnTouchListener(sfbt);
        bSelT.setOnLongClickListener(sfbt);  // Consist Light Edit

        bSelS = (Button) findViewById(R.id.button_select_loco_S);
        bSelS.setClickable(true);
        sfbt = new select_function_button_touch_listener('S');
        bSelS.setOnClickListener(sfbt);
        bSelS.setOnTouchListener(sfbt);
        bSelS.setOnLongClickListener(sfbt);  // Consist Light Edit

        bSelG = (Button) findViewById(R.id.button_select_loco_G);
        bSelG.setClickable(true);
        sfbt = new select_function_button_touch_listener('G');
        bSelG.setOnClickListener(sfbt);
        bSelG.setOnTouchListener(sfbt);
        bSelG.setOnLongClickListener(sfbt);  // Consist Light Edit

        // Arrow Keys
        try {
            // Throttle T speed buttons.
            bRSpdT = (Button) findViewById(R.id.Right_speed_button_T);
            bRSpdT.setClickable(true);
            asbl = new arrow_speed_button_touch_listener('T', "right");
            bRSpdT.setOnLongClickListener(asbl);
            bRSpdT.setOnTouchListener(asbl);
            bRSpdT.setOnClickListener(asbl);

            bLSpdT = (Button) findViewById(R.id.Left_speed_button_T);
            bLSpdT.setClickable(true);
            asbl = new arrow_speed_button_touch_listener('T', "left");
            bLSpdT.setOnLongClickListener(asbl);
            bLSpdT.setOnTouchListener(asbl);
            bLSpdT.setOnClickListener(asbl);

            // Throttle S speed buttons
            bRSpdS = (Button) findViewById(R.id.Right_speed_button_S);
            bRSpdS.setClickable(true);
            asbl = new arrow_speed_button_touch_listener('S', "right");
            bRSpdS.setOnLongClickListener(asbl);
            bRSpdS.setOnTouchListener(asbl);
            bRSpdS.setOnClickListener(asbl);

            bLSpdS = (Button) findViewById(R.id.Left_speed_button_S);
            bLSpdS.setClickable(true);
            asbl = new arrow_speed_button_touch_listener('S', "left");
            bLSpdS.setOnLongClickListener(asbl);
            bLSpdS.setOnTouchListener(asbl);
            bLSpdS.setOnClickListener(asbl);

            // Throttle G speed buttons.
            bRSpdG = (Button) findViewById(R.id.Right_speed_button_G);
            bRSpdG.setClickable(true);
            asbl = new arrow_speed_button_touch_listener('G', "right");
            bRSpdG.setOnLongClickListener(asbl);
            bRSpdG.setOnTouchListener(asbl);
            bRSpdG.setOnClickListener(asbl);

            bLSpdG = (Button) findViewById(R.id.Left_speed_button_G);
            bLSpdG.setClickable(true);
            asbl = new arrow_speed_button_touch_listener('G', "left");
            bLSpdG.setOnLongClickListener(asbl);
            bLSpdG.setOnTouchListener(asbl);
            bLSpdG.setOnClickListener(asbl);
        } catch (Exception ex) {
            Log.d("debug", "onCreate: " + ex.getMessage());
        }

        // set listeners for 3 direction buttons for each throttle
        bFwdT = (Button) findViewById(R.id.button_fwd_T);
        if (prefSwapForwardReverseButtons) {
            fbtl = new function_button_touch_listener(function_button.REVERSE, 'T');
        } else {
            fbtl = new function_button_touch_listener(function_button.FORWARD, 'T');
        }
        bFwdT.setOnTouchListener(fbtl);
        bFwdT.setText(DirectionButtonLeftText);
        bStopT = (Button) findViewById(R.id.button_stop_T);
        fbtl = new function_button_touch_listener(function_button.STOP, 'T');
        bStopT.setOnTouchListener(fbtl);
        bRevT = (Button) findViewById(R.id.button_rev_T);
        if (prefSwapForwardReverseButtons) {
            fbtl = new function_button_touch_listener(function_button.FORWARD, 'T');
        } else {
            fbtl = new function_button_touch_listener(function_button.REVERSE, 'T');
        }
        bRevT.setOnTouchListener(fbtl);
        bRevT.setText(DirectionButtonRightText);
        View v = findViewById(R.id.speed_cell_T);
        fbtl = new function_button_touch_listener(function_button.SPEED_LABEL, 'T');
        v.setOnTouchListener(fbtl);

        bFwdS = (Button) findViewById(R.id.button_fwd_S);
        if (prefSwapForwardReverseButtons) {
            fbtl = new function_button_touch_listener(function_button.REVERSE, 'S');
        } else {
            fbtl = new function_button_touch_listener(function_button.FORWARD, 'S');
        }
        bFwdS.setOnTouchListener(fbtl);
        bFwdS.setText(DirectionButtonLeftText);
        bStopS = (Button) findViewById(R.id.button_stop_S);
        fbtl = new function_button_touch_listener(function_button.STOP, 'S');
        bStopS.setOnTouchListener(fbtl);
        bRevS = (Button) findViewById(R.id.button_rev_S);
        if (prefSwapForwardReverseButtons) {
            fbtl = new function_button_touch_listener(function_button.FORWARD, 'S');
        } else {
            fbtl = new function_button_touch_listener(function_button.REVERSE, 'S');
        }
        bRevS.setOnTouchListener(fbtl);
        bRevS.setText(DirectionButtonRightText);
        v = findViewById(R.id.speed_cell_S);
        fbtl = new function_button_touch_listener(function_button.SPEED_LABEL, 'S');
        v.setOnTouchListener(fbtl);

        bFwdG = (Button) findViewById(R.id.button_fwd_G);
        if (prefSwapForwardReverseButtons) {
            fbtl = new function_button_touch_listener(function_button.REVERSE, 'G');
        } else {
            fbtl = new function_button_touch_listener(function_button.FORWARD, 'G');
        }
        bFwdG.setOnTouchListener(fbtl);
        bFwdG.setText(DirectionButtonLeftText);
        bStopG = (Button) findViewById(R.id.button_stop_G);
        fbtl = new function_button_touch_listener(function_button.STOP, 'G');
        bStopG.setOnTouchListener(fbtl);
        bRevG = (Button) findViewById(R.id.button_rev_G);
        if (prefSwapForwardReverseButtons) {
            fbtl = new function_button_touch_listener(function_button.FORWARD, 'G');
        } else {
            fbtl = new function_button_touch_listener(function_button.REVERSE, 'G');
        }
        bRevG.setOnTouchListener(fbtl);
        bRevG.setText(DirectionButtonRightText);
        v = findViewById(R.id.speed_cell_G);
        fbtl = new function_button_touch_listener(function_button.SPEED_LABEL, 'G');
        v.setOnTouchListener(fbtl);

        // set up listeners for all throttles
        throttle_listener thl;
        sbT = (SeekBar) findViewById(R.id.speed_T);
        thl = new throttle_listener('T');
        sbT.setOnSeekBarChangeListener(thl);
        sbT.setOnTouchListener(thl);

        sbS = (SeekBar) findViewById(R.id.speed_S);
        thl = new throttle_listener('S');
        sbS.setOnSeekBarChangeListener(thl);
        sbS.setOnTouchListener(thl);

        sbG = (SeekBar) findViewById(R.id.speed_G);
        thl = new throttle_listener('G');
        sbG.setOnSeekBarChangeListener(thl);
        sbG.setOnTouchListener(thl);

        max_throttle_change = 1;
//      displaySpeedSteps = false;

        // throttle layouts
        vThrotScr = findViewById(R.id.throttle_screen);
        vThrotScrWrap = findViewById(R.id.throttle_screen_wrapper);
        llT = (LinearLayout) findViewById(R.id.throttle_T);
        llG = (LinearLayout) findViewById(R.id.throttle_G);
        llS = (LinearLayout) findViewById(R.id.throttle_S);
        llTSetSpd = (LinearLayout) findViewById(R.id.Throttle_T_SetSpeed);
        llSSetSpd = (LinearLayout) findViewById(R.id.Throttle_S_SetSpeed);
        llGSetSpd = (LinearLayout) findViewById(R.id.Throttle_G_SetSpeed);
        // SPDHT
        llTLocoId = (LinearLayout) findViewById(R.id.loco_buttons_group_T);
        llSLocoId = (LinearLayout) findViewById(R.id.loco_buttons_group_S);
        llGLocoId = (LinearLayout) findViewById(R.id.loco_buttons_group_G);
        //
        llTLocoDir = (LinearLayout) findViewById(R.id.dir_buttons_table_T);
        llSLocoDir = (LinearLayout) findViewById(R.id.dir_buttons_table_S);
        llGLocoDir = (LinearLayout) findViewById(R.id.dir_buttons_table_G);
        // SPDHT

        // volume indicators
        tvVolT =(TextView) findViewById(R.id.volume_indicator_T);
        tvVolS =(TextView) findViewById(R.id.volume_indicator_S);
        tvVolG =(TextView) findViewById(R.id.volume_indicator_G);

        // gamepad indicators
        tvGamePadT =(TextView) findViewById(R.id.gamepad_indicator_T);
        tvGamePadS =(TextView) findViewById(R.id.gamepad_indicator_S);
        tvGamePadG =(TextView) findViewById(R.id.gamepad_indicator_G);

        // set_default_function_labels();
        tvSpdLabT = (TextView) findViewById(R.id.speed_label_T);
        tvSpdValT = (TextView) findViewById(R.id.speed_value_label_T);
        tvSpdLabG = (TextView) findViewById(R.id.speed_label_G);
        tvSpdValG = (TextView) findViewById(R.id.speed_value_label_G);
        tvSpdLabS = (TextView) findViewById(R.id.speed_label_S);
        tvSpdValS = (TextView) findViewById(R.id.speed_value_label_S);

        fbT = (ViewGroup) findViewById(R.id.function_buttons_table_T);
        fbS = (ViewGroup) findViewById(R.id.function_buttons_table_S);
        fbG = (ViewGroup) findViewById(R.id.function_buttons_table_G);
        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

       webView = (WebView) findViewById(R.id.throttle_webview);
        String databasePath = webView.getContext().getDir("databases", Context.MODE_PRIVATE).getPath();
        webView.getSettings().setDatabasePath(databasePath);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true); // Enable Multitouch
        // if supported
        webView.getSettings().setUseWideViewPort(true); // Enable greater
        // zoom-out
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        webView.setInitialScale((int) (100 * scale));
        // webView.getSettings().setLoadWithOverviewMode(true); // size image to
        // fill width

        // open all links inside the current view (don't start external web
        // browser)
        WebViewClient EDWebClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!noUrl.equals(url)) {               // if url is legit
                    currentUrl = url;
                    if (firstUrl == null) {             // if this is the first legit url 
                        firstUrl = url;
                        clearHistory = true;
                    }
                    if (clearHistory) {                 // keep clearing history until off this page
                        if (url.equals(firstUrl)) {      // (works around Android bug)
                            webView.clearHistory();
                        } else {
                            clearHistory = false;
                        }
                    }
                }
            }
        };

        webView.setWebViewClient(EDWebClient);
        if (currentUrl == null || savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            load_webview(); // reload if no saved state or no page had loaded when state was saved
        }
        // put pointer to this activity's handler in main app's shared variable
        mainapp.throttle_msg_handler = new throttle_handler();

        // set throttle change delay timers
        changeTimerT = new ChangeDelay('T');
        changeTimerG = new ChangeDelay('G');
        changeTimerS = new ChangeDelay('S');

        // tone generator for feedback sounds
        tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                preferences.getIntPrefValue(prefs,"prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));

        // set GamePad Support
        setGamepadKeys();

        // initialise ESU MCII
        if (IS_ESU_MCII) {
            Log.d("Engine_Driver", "ESU_MCII: Initialise fragments...");
            esuThrottleFragment = ThrottleFragment.newInstance(1);
            esuThrottleFragment.setOnThrottleListener(esuOnThrottleListener);
            esuStopButtonFragment = StopButtonFragment.newInstance();
            esuStopButtonFragment.setOnStopButtonListener(esuOnStopButtonListener);
            Log.d("Engine_Driver", "ESU_MCII: ...fragments initialised");

            getSupportFragmentManager().beginTransaction()
                    .add(esuThrottleFragment, "mc2:throttle")
                    .add(esuStopButtonFragment, "mc2:stopKey")
                    .commit();
            esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.OFF, true);
            esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.STEADY_FLASH, true);
            Log.d("Engine_Driver", "ESU_MCII: Initialisation complete");
        }

    } // end of onCreate()

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) { // expedite
            this.finish();
            return;
        }
        if (!mainapp.setActivityOrientation(this)) // set screen orientation based on prefs
        {
            Intent in = new Intent().setClass(this, web_activity.class); // if autoWeb and landscape, switch to Web activity
            navigatingAway = true;
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return;
        }
        navigatingAway = false;
        currentTime = "";
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CURRENT_TIME); // request time update

        // format the screen area
        enable_disable_buttons('T');
        enable_disable_buttons('S');
        enable_disable_buttons('G');
        gestureFailed = false;
        gestureInProgress = false;

        prefSwipeUpOption = prefs.getString("SwipeUpOption", getApplicationContext().getResources().getString(R.string.prefSwipeUpOptionDefaultValue));
        isScreenLocked = false;

        dirChangeWhileMoving = prefs.getBoolean("DirChangeWhileMovingPreference", getResources().getBoolean(R.bool.prefDirChangeWhileMovingDefaultValue));
        stopOnDirectionChange = prefs.getBoolean("prefStopOnDirectionChange", getResources().getBoolean(R.bool.prefStopOnDirectionChangeDefaultValue));
        locoSelectWhileMoving = prefs.getBoolean("SelLocoWhileMovingPreference", getResources().getBoolean(R.bool.prefSelLocoWhileMovingDefaultValue));

        screenBrightnessDim = Integer.parseInt(prefs.getString("prefScreenBrightnessDim", getResources().getString(R.string.prefScreenBrightnessDimDefaultValue))) * 255 /100;
        //screenBrightnessBright = Integer.parseInt(prefs.getString("prefScreenBrightnessBright", getResources().getString(R.string.prefScreenBrightnessBrightDefaultValue))) * 255 /100;

        prefConsistLightsLongClick = prefs.getBoolean("ConsistLightsLongClickPreference", getResources().getBoolean(R.bool.prefConsistLightsLongClickDefaultValue));

        prefGamePadMultipleDevices = prefs.getBoolean("prefGamePadMultipleDevices", getResources().getBoolean(R.bool.prefGamePadMultipleDevicesDefaultValue));

        setGamepadKeys();

        applySpeedRelatedOptions();  // update all throttles

        set_labels(); // handle labels and update view

        if (webView != null) {
            if (!callHiddenWebViewOnResume()) {
                webView.resumeTimers();
            }
            if (noUrl.equals(webView.getUrl()) && webView.canGoBack()) {    //unload static url loaded by onPause
                webView.goBack();
            }
        }

        if (mainapp.EStopActivated) {
            speedUpdateAndNotify(0);  // update all three throttles
            applySpeedRelatedOptions();  // update all three throttles

            mainapp.EStopActivated = false;
        }

        // update the direction indicators
        showDirectionIndications();

        if (TMenu != null) {
            TMenu.findItem(R.id.EditConsistT_menu).setVisible(mainapp.consistT.isMulti());
            TMenu.findItem(R.id.EditConsistS_menu).setVisible(mainapp.consistS.isMulti());
            TMenu.findItem(R.id.EditConsistG_menu).setVisible(mainapp.consistG.isMulti());

            TMenu.findItem(R.id.EditLightsConsistT_menu).setVisible(mainapp.consistT.isMulti());
            TMenu.findItem(R.id.EditLightsConsistS_menu).setVisible(mainapp.consistS.isMulti());
            TMenu.findItem(R.id.EditLightsConsistG_menu).setVisible(mainapp.consistG.isMulti());
        }

        CookieSyncManager.getInstance().startSync();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState); // save history (on rotation) if at least one page has loaded

        // save the requested throttle direction so we can update the
        // direction indication immediately in OnCreate following a rotate
        outState.putSerializable("dirT", dirT);
        outState.putSerializable("dirS", dirS);
        outState.putSerializable("dirG", dirG);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            if (!callHiddenWebViewOnPause()) {
                webView.pauseTimers();
            }

            String url = webView.getUrl();
            if (url != null && !noUrl.equals(url)) {    // if any url has been loaded 
                webView.loadUrl(noUrl);                 // load a static url to stop any javascript
            }
        }
        CookieSyncManager.getInstance().stopSync();

        if (!this.isFinishing() && !navigatingAway) { // only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }

        if ((isScreenLocked) || (screenDimmed)) {
            isScreenLocked = false;
            screenDimmed = false;
            setScreenBrightness(screenBrightnessOriginal);
            setScreenBrightnessMode(screenBrightnessModeOriginal);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            setImmersiveModeOn(webView);
            set_labels();       // need to redraw button Press states since ActionBar and Notification access clears them
        }
    }

    /**
     * Called when the activity is finished.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "throttle.onDestroy()");
        if (webView != null) {
            scale = webView.getScale(); // save current scale for next onCreate
        }
        repeatUpdateHandler = null;
        volumeKeysRepeatUpdateHandler = null;
        gamepadRepeatUpdateHandler = null;
        if (IS_ESU_MCII) {
            esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.OFF);
            esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
        }

        tg.release();
        mainapp.throttle_msg_handler = null;

        super.onDestroy();
    }

    private boolean callHiddenWebViewOnPause() {
        try {
            Method method = WebView.class.getMethod("onPause");
            method.invoke(webView);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean callHiddenWebViewOnResume() {
        try {
            Method method = WebView.class.getMethod("onResume");
            method.invoke(webView);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // load the url
    private void load_webview() {
        String url = currentUrl;
        if (webViewLocation.equals("none")) {       // if not displaying webview
            webViewIsOn = false;
            url = noUrl;                            // load static url to stop javascript
            currentUrl = null;
            firstUrl = null;
        } else if (url == null)                       // else if initializing
        {
            webViewIsOn = true;
            url = mainapp.createUrl(prefs.getString("InitialThrotWebPage",
                    getApplicationContext().getResources().getString(R.string.prefInitialThrotWebPageDefaultValue)));
            if (url == null) {      //if port is invalid
                url = noUrl;
            }

            if (firstUrl == null) {
                scale = initialScale;
                webView.setInitialScale((int) (100 * scale));
            }
            firstUrl = null;
        }
        webView.loadUrl(url);
    }

    void setAllFunctionLabelsAndListeners() {
        set_function_labels_and_listeners_for_view('T');
        set_function_labels_and_listeners_for_view('S');
        set_function_labels_and_listeners_for_view('G');
    }

    // helper function to set up function buttons for each throttle
    // loop through all function buttons and
    // set label and dcc functions (based on settings) or hide if no label
    void set_function_labels_and_listeners_for_view(char whichThrottle) {
        // Log.d("Engine_Driver","starting set_function_labels_and_listeners_for_view");

        ViewGroup tv; // group
        ViewGroup r; // row
        function_button_touch_listener fbtl;
        Button b; // button
        int k = 0; // button count
        LinkedHashMap<Integer, String> function_labels_temp;
        LinkedHashMap<Integer, Button> functionButtonMap = new LinkedHashMap<>();

        if (whichThrottle == 'T') {
            tv = fbT;
        } else if (whichThrottle == 'G') {
            tv = fbG;
        } else {
            tv = fbS;
        }

        // Ignore the labels for the loco in the Roster and use the defaults... if requested in preferences
        boolean audfl = prefs.getBoolean("prefAlwaysUseDefaultFunctionLabels", getResources().getBoolean(R.bool.prefAlwaysUseDefaultFunctionLabelsDefaultValue));

        // note: we make a copy of function_labels_x because TA might change it
        // while we are using it (causing issues during button update below)
        if (!audfl) {
            if (whichThrottle == 'T' && mainapp.function_labels_T != null && mainapp.function_labels_T.size() > 0) {
                function_labels_temp = new LinkedHashMap<>(mainapp.function_labels_T);
            } else if (whichThrottle == 'G' && mainapp.function_labels_G != null && mainapp.function_labels_G.size() > 0) {
                function_labels_temp = new LinkedHashMap<>(mainapp.function_labels_G);
            } else if (whichThrottle == 'S' && mainapp.function_labels_S != null && mainapp.function_labels_S.size() > 0) {
                function_labels_temp = new LinkedHashMap<>(mainapp.function_labels_S);
            } else {
                function_labels_temp = mainapp.function_labels_default;
            }
        } else { // Force using the Default Function Labels
            function_labels_temp = mainapp.function_labels_default;
        }

        // put values in array for indexing in next step TODO: find direct way
        // to do this
        ArrayList<Integer> aList = new ArrayList<>();
        aList.addAll(function_labels_temp.keySet());

        for (int i = 0; i < tv.getChildCount(); i++) {
            r = (ViewGroup) tv.getChildAt(i);
            for (int j = 0; j < r.getChildCount(); j++) {
                b = (Button) r.getChildAt(j);
                if (k < function_labels_temp.size()) {
                    Integer func = aList.get(k);
                    functionButtonMap.put(func, b); // save function to button
                    // mapping
                    String bt = function_labels_temp.get(func);
                    fbtl = new function_button_touch_listener(func, whichThrottle, bt);
                    b.setOnTouchListener(fbtl);
                    bt = bt + "        ";  // pad with spaces, and limit to 7 characters
                    b.setText(bt.substring(0, 7));
                    b.setVisibility(View.VISIBLE);
                    b.setEnabled(false); // start out with everything disabled
                } else {
                    b.setVisibility(View.GONE);
                }
                k++;
            }
        }

        // update the function-to-button map for the current throttle
        if (whichThrottle == 'T')
            functionMapT = functionButtonMap;
        else if (whichThrottle == 'G') {
            functionMapG = functionButtonMap;
        } else
            functionMapS = functionButtonMap;
    }

    // helper function to get a numbered function button from its throttle and function number
    Button getFunctionButton(char whichThrottle, int func) {
        Button b; // button
        LinkedHashMap<Integer, Button> functionButtonMap;

        if (whichThrottle == 'T')
            functionButtonMap = functionMapT;
        else if (whichThrottle == 'G') {
            functionButtonMap = functionMapG;
        } else {
            functionButtonMap = functionMapS;
        }
        b = functionButtonMap.get(func);
        return b;
   }

    // lookup and set values of various informational text labels and size the
    // screen elements
    @SuppressWarnings("deprecation")
    private void set_labels() {
        // Log.d("Engine_Driver","starting set_labels");

        int throttle_count = 0;
        int height_T; // height of first throttle area
        int height_S; // height of second throttle area
        int height_G;// height of third throttle area

        // avoid NPE by not letting this run too early (reported to Play Store)
        if (tvVolT == null) return;

        // hide or display volume control indicator based on variable
        setVolumeIndicator();
        setGamepadIndicator();

        // set speed buttons speed step
        BUTTON_SPEED_STEP = preferences.getIntPrefValue(prefs, "speed_arrows_throttle_speed_step", "4");

        // set up max speeds for throttles
        int maxThrottle = preferences.getIntPrefValue(prefs, "maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (maxThrottle * .01)); // convert from percent
        sbT.setMax(maxThrottle);
        sbS.setMax(maxThrottle);
        sbG.setMax(maxThrottle);

        // set max allowed change for throttles from prefs
        int maxChange = preferences.getIntPrefValue(prefs, "maximum_throttle_change_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleChangeDefaultValue));
        max_throttle_change = (int) Math.round(maxThrottle * (maxChange * .01));

        if (mainapp.consistT.isEmpty()) {
            maxSpeedStepT = 100;
        }
        if (mainapp.consistG.isEmpty()) {
            maxSpeedStepG = 100;
        }
        if (mainapp.consistS.isEmpty()) {
            maxSpeedStepS = 100;
        }
        //get speed steps from prefs
        speedStepPref = preferences.getIntPrefValue(prefs, "DisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
        setDisplayUnitScale('T');
        setDisplayUnitScale('G');
        setDisplayUnitScale('S');

        setDisplayedSpeed('T', sbT.getProgress());  // update numeric speeds since units might have changed
        setDisplayedSpeed('G', sbG.getProgress());
        setDisplayedSpeed('S', sbS.getProgress());

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        // Get the screen's density scale
        final float denScale = dm.density;

        // SPDHT decrease height of Loco Id (if requested in preferences)
        boolean dlih = prefs.getBoolean("prefDecreaseLocoNumberHeight", getResources().getBoolean(R.bool.prefDecreaseLocoNumberHeightDefaultValue));
        // Convert the dps to pixels, based on density scale
        int newDlihHeight;
        int newDlihFontSize;
        if (dlih) {
            newDlihHeight = (int) (40 * denScale + 0.5f); // decreased height
            newDlihFontSize = 32; // decreased height
        } else {
            newDlihHeight = (int) (50 * denScale + 0.5f); // normal height
            newDlihFontSize = 36; // normal height
        }
        // SPDHT

        // increase height of throttle slider (if requested in preferences)
        boolean ish = prefs.getBoolean("increase_slider_height_preference", getResources().getBoolean(R.bool.prefIncreaseSliderHeightDefaultValue));

        // Convert the dps to pixels, based on density scale
        int newHeight;
        if (ish) {
            newHeight = (int) (80 * denScale + 0.5f); // increased height
        } else {
            newHeight = (int) (50 * denScale + 0.5f); // normal height
        }

        //
        boolean sadion = prefs.getBoolean("prefShowAddressInsteadOfName", getResources().getBoolean(R.bool.prefShowAddressInsteadOfNameDefaultValue));
        //

        final int conNomTextSize = 24;
        final double minTextScale = 0.5;
        String bLabel;
        Button b = bSelT;
        if (mainapp.consistT.isActive()) {
            if (!sadion) {
                bLabel = mainapp.consistT.toString();
            } else {
                bLabel = mainapp.consistT.formatConsistAddr();
            }
            throttle_count++;
        } else {
            bLabel = "Press to select";
            // whichVolume = 'S'; //set the next throttle to use volume control
        }
        double textScale = 1.0;
        int bWidth = b.getWidth(); // scale text if required to fit the textView
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
        double textWidth = b.getPaint().measureText(bLabel);
        if (bWidth == 0)
            selectLocoRendered = false;
        else {
            selectLocoRendered = true;
            if (textWidth > 0 && textWidth > bWidth) {
                textScale = bWidth / textWidth;
                if (textScale < minTextScale)
                    textScale = minTextScale;
            }
        }
        int textSize = (int) (conNomTextSize * textScale);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        b.setText(bLabel);
        b.setSelected(false);
        b.setPressed(false);

        b = bSelS;
        if (mainapp.consistS.isActive()) {
            if (!sadion) {
                bLabel = mainapp.consistS.toString();
            } else {
                bLabel = mainapp.consistS.formatConsistAddr();
            }
            throttle_count++;
        } else {
            bLabel = "Press to select";
        }
        textScale = 1.0;
        bWidth = b.getWidth(); // scale text if required to fit the textView
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
        textWidth = b.getPaint().measureText(bLabel);
        if (bWidth != 0 && textWidth > 0 && textWidth > bWidth) {
            textScale = bWidth / textWidth;
            if (textScale < minTextScale)
                textScale = minTextScale;
        }
        textSize = (int) (conNomTextSize * textScale);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        b.setText(bLabel);
        b.setSelected(false);
        b.setPressed(false);

        b = bSelG;
        if (mainapp.consistG.isActive()) {
            if (!sadion) {
                bLabel = mainapp.consistG.toString();
            } else {
                bLabel = mainapp.consistG.formatConsistAddr();
            }
            throttle_count++;
        } else {
            bLabel = "Press to select";
        }
        textScale = 1.0;
        bWidth = b.getWidth(); // scale text if required to fit the textView
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
        textWidth = b.getPaint().measureText(bLabel);
        if (bWidth != 0 && textWidth > 0 && textWidth > bWidth) {
            textScale = bWidth / textWidth;
            if (textScale < 0.5)
                textScale = 0.5;
        }
        textSize = (int) (conNomTextSize * textScale);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        b.setText(bLabel);
        b.setSelected(false);
        b.setPressed(false);

        int screenHeight = vThrotScrWrap.getHeight(); // get the height of usable area
        //Log.d("Engine_Driver","vThrotScrWrap.getHeight(), screenHeight=" + screenHeight);
        if (screenHeight == 0) {
            // throttle screen hasn't been drawn yet, so use display metrics for now
            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
            //Log.d("Engine_Driver","vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
        }

        // increase the web view height if the preference is set
        boolean iwvs = prefs.getBoolean("prefIncreaseWebViewSize", getResources().getBoolean(R.bool.prefIncreaseWebViewSizeDefaultValue));

        // save part the screen for webview
        if (webViewLocation.equals("Top") || webViewLocation.equals("Bottom")) {
            webViewIsOn = true;
            if (!iwvs) {
                // save half the screen
                screenHeight *= 0.5;
            } else {
                // save 60% of the screen
                if (webViewLocation.equals("Bottom")) {
                    screenHeight *= 0.40;
                } else {
                    screenHeight *= 0.60;
                }
            }
        }

        // SPDHT set height of Loco Id and Direction Button areas
        LinearLayout.LayoutParams llLidp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newDlihHeight);
        llTLocoId.setLayoutParams(llLidp);
        llSLocoId.setLayoutParams(llLidp);
        llGLocoId.setLayoutParams(llLidp);
        llTLocoDir.setLayoutParams(llLidp);
        llSLocoDir.setLayoutParams(llLidp);
        llGLocoDir.setLayoutParams(llLidp);
        //
        tvSpdValT.setTextSize(TypedValue.COMPLEX_UNIT_SP, newDlihFontSize);
        tvSpdValS.setTextSize(TypedValue.COMPLEX_UNIT_SP, newDlihFontSize);
        tvSpdValG.setTextSize(TypedValue.COMPLEX_UNIT_SP, newDlihFontSize);
        // SPDHT


        //set height of slider areas
        LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newHeight);
        llTSetSpd.setLayoutParams(llLp);
        llSSetSpd.setLayoutParams(llLp);
        llGSetSpd.setLayoutParams(llLp);

        //set margins of slider areas
        int sliderMargin = preferences.getIntPrefValue(prefs, "left_slider_margin", getApplicationContext().getResources().getString(R.string.prefSliderLeftMarginDefaultValue));

        //show speed buttons based on pref
        sbS.setVisibility(View.VISIBLE);  //always show slider if buttons not shown
        sbG.setVisibility(View.VISIBLE);
        sbT.setVisibility(View.VISIBLE);
        if (prefs.getBoolean("display_speed_arrows_buttons", false)) {
            bLSpdT.setVisibility(View.VISIBLE);
            bLSpdS.setVisibility(View.VISIBLE);
            bLSpdG.setVisibility(View.VISIBLE);
            bRSpdT.setVisibility(View.VISIBLE);
            bRSpdS.setVisibility(View.VISIBLE);
            bRSpdG.setVisibility(View.VISIBLE);
            bLSpdT.setText(speedButtonLeftText);
            bLSpdG.setText(speedButtonLeftText);
            bLSpdS.setText(speedButtonLeftText);
            bRSpdT.setText(speedButtonRightText);
            bRSpdG.setText(speedButtonRightText);
            bRSpdS.setText(speedButtonRightText);
            //if buttons enabled, hide the slider if requested
            if (prefs.getBoolean("hide_slider_preference", false)) {
                sbS.setVisibility(View.GONE);
                sbG.setVisibility(View.GONE);
                sbT.setVisibility(View.GONE);
                bLSpdT.setText(speedButtonDownText);
                bLSpdG.setText(speedButtonDownText);
                bLSpdS.setText(speedButtonDownText);
                bRSpdT.setText(speedButtonUpText);
                bRSpdG.setText(speedButtonUpText);
                bRSpdS.setText(speedButtonUpText);
            }
        } else {  //hide speed buttons based on pref
            bLSpdT.setVisibility(View.GONE);
            bLSpdS.setVisibility(View.GONE);
            bLSpdG.setVisibility(View.GONE);
            bRSpdT.setVisibility(View.GONE);
            bRSpdS.setVisibility(View.GONE);
            bRSpdG.setVisibility(View.GONE);
            sliderMargin += 30;  //a little extra margin previously in button
        }

        sbS.setPadding(sliderMargin, 0, sliderMargin, 0);
        sbG.setPadding(sliderMargin, 0, sliderMargin, 0);
        sbT.setPadding(sliderMargin, 0, sliderMargin, 0);

        if (screenHeight > throttleMargin) { // don't do this if height is invalid
            //Log.d("Engine_Driver","starting screen height adjustments, screenHeight=" + screenHeight);
            // determine how to split the screen (evenly if all three, 45/45/10 for two, 80/10/10 if only one)
            screenHeight -= throttleMargin;
            String numThrot = prefs.getString("NumThrottle", getResources().getString(R.string.prefNumOfThrottlesDefault));

            // don't allow third throttle if not supported in JMRI (prior to multithrottle change)
            if (mainapp.withrottle_version < 2.0 && numThrot.matches("Three")) {
                numThrot = "Two";
            }

            if (numThrot.matches("One")) {
                height_T = screenHeight;
                height_S = 0;
                height_G = 0;
            } else if (numThrot.matches("Two") && !mainapp.consistS.isActive()) {
                height_T = (int) (screenHeight * 0.9);
                height_S = (int) (screenHeight * 0.10);
                height_G = 0;
            } else if (numThrot.matches("Two") && !mainapp.consistT.isActive()) {
                height_T = (int) (screenHeight * 0.10);
                height_S = (int) (screenHeight * 0.9);
                height_G = 0;
            } else if (numThrot.matches("Two")) {
                height_T = (int) (screenHeight * 0.5);
                height_S = (int) (screenHeight * 0.5);
                height_G = 0;
            } else if (throttle_count == 0 || throttle_count == 3) {
                height_T = (int) (screenHeight * 0.33);
                height_S = (int) (screenHeight * 0.33);
                height_G = (int) (screenHeight * 0.33);
            } else if (!mainapp.consistT.isActive() && !mainapp.consistS.isActive()) {
                height_T = (int) (screenHeight * 0.10);
                height_S = (int) (screenHeight * 0.10);
                height_G = (int) (screenHeight * 0.80);
            } else if (!mainapp.consistT.isActive() && !mainapp.consistG.isActive()) {
                height_T = (int) (screenHeight * 0.10);
                height_S = (int) (screenHeight * 0.80);
                height_G = (int) (screenHeight * 0.10);
            } else if (!mainapp.consistS.isActive() && !mainapp.consistG.isActive()) {
                height_T = (int) (screenHeight * 0.80);
                height_S = (int) (screenHeight * 0.10);
                height_G = (int) (screenHeight * 0.10);
            } else if (!mainapp.consistT.isActive()) {
                height_T = (int) (screenHeight * 0.10);
                height_S = (int) (screenHeight * 0.45);
                height_G = (int) (screenHeight * 0.45);
            } else if (!mainapp.consistS.isActive()) {
                height_T = (int) (screenHeight * 0.45);
                height_S = (int) (screenHeight * 0.10);
                height_G = (int) (screenHeight * 0.45);
            } else {
                height_T = (int) (screenHeight * 0.45);
                height_S = (int) (screenHeight * 0.45);
                height_G = (int) (screenHeight * 0.10);
            }

            // set height of T area
            llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, height_T);
            llLp.bottomMargin = (int) (throttleMargin * (dm.densityDpi / 160.));
            llT.setLayoutParams(llLp);

            // update throttle slider top/bottom
            T_top = llT.getTop() + sbT.getTop() + bSelT.getHeight() + bFwdT.getHeight();
            T_bottom = llT.getTop() + sbT.getBottom() + bSelT.getHeight() + bFwdT.getHeight();

            // set height of S area
            llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, height_S);
            llLp.bottomMargin = (int) (throttleMargin * (dm.densityDpi / 160.));
            llS.setLayoutParams(llLp);

            // update throttle slider top/bottom
            S_top = llS.getTop() + sbS.getTop() + bSelS.getHeight() + bFwdS.getHeight();
            S_bottom = llS.getTop() + sbS.getBottom() + bSelS.getHeight() + bFwdS.getHeight();

            // set height of G area
            llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, height_G);
            llLp.bottomMargin = (int) (throttleMargin * (dm.densityDpi / 160.));
            llG.setLayoutParams(llLp);

            // update throttle slider top/bottom
            G_top = llG.getTop() + sbG.getTop() + bSelG.getHeight() + bFwdG.getHeight();
            G_bottom = llG.getTop() + sbG.getBottom() + bSelG.getHeight() + bFwdG.getHeight();
        } else {
            Log.d("Engine_Driver","screen height adjustments skipped, screenHeight=" + screenHeight);
        }

        // update the direction indicators
        showDirectionIndications();

        // update the state of each function button based on shared variable
        set_all_function_states('T');
        set_all_function_states('S');
        set_all_function_states('G');

        //adjust several items in the menu
        if (TMenu != null) {
            mainapp.displayEStop(TMenu);
            mainapp.setPowerStateButton(TMenu);
            mainapp.displayPowerStateMenuButton(TMenu);
            mainapp.setPowerMenuOption(TMenu);
            mainapp.setWebMenuOption(TMenu);
            mainapp.setRoutesMenuOption(TMenu);
            mainapp.setTurnoutsMenuOption(TMenu);
        }
        vThrotScrWrap.invalidate();
        // Log.d("Engine_Driver","ending set_labels");
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onKeyUp(int key, KeyEvent event) {
        int repeatCnt = event.getRepeatCount();
        int action = event.getAction();

        // Handle pressing of the back button
        if ((key == KEYCODE_VOLUME_UP) || (key == KEYCODE_VOLUME_DOWN)) {
            mVolumeKeysAutoIncrement = false;
            mVolumeKeysAutoDecrement = false;

            return (true); // stop processing this key
        }
        return (super.onKeyUp(key, event)); // continue with normal key
        // processing
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        int repeatCnt = event.getRepeatCount();
        int action = event.getAction();

        // Handle pressing of the back button
        if (key == KEYCODE_BACK) {
            if (webView.canGoBack() && !clearHistory) {
                scale = webView.getScale(); // save scale
                webView.goBack();
                webView.setInitialScale((int) (100 * scale)); // restore scale
            } else
                mainapp.checkExit(this);
            return (true); // stop processing this key
        } else if ((key == KEYCODE_VOLUME_UP) || (key == KEYCODE_VOLUME_DOWN)) {  // use volume to change speed for specified loco
            char wVol = 0;
            if (whichVolume == 'T' && mainapp.consistT.isActive()) {
                wVol = 'T';
            }
            if (whichVolume == 'S' && mainapp.consistS.isActive()) {
                wVol = 'S';
            }
            if (whichVolume == 'G' && mainapp.consistG.isActive()) {
                wVol = 'G';
            }
            if (wVol != 0) {
                if (key == KEYCODE_VOLUME_UP) {
                    if (repeatCnt == 0) {
                        mVolumeKeysAutoIncrement = true;
                        volumeKeysRepeatUpdateHandler.post(new volumeKeysRptUpdater(wVol));
                    }
                } else {
                    if (repeatCnt == 0) {
                        mVolumeKeysAutoDecrement = true;
                        volumeKeysRepeatUpdateHandler.post(new volumeKeysRptUpdater(wVol));
                    }
                }
            }
            return (true); // stop processing this key
        }
        return (super.onKeyDown(key, event)); // continue with normal key
        // processing
    }

    private void disconnect() {
        // release the locos
        mainapp.consistT.release();
        mainapp.consistS.release();
        mainapp.consistG.release();
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", 'T');   // release first loco
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", 'S');   // release second loco
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", 'G');   // release third loco

        webView.stopLoading();
        this.finish(); // end this activity
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.throttle_menu, menu);
        TMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        switch (item.getItemId()) {
            case R.id.turnouts_mnu:
                in = new Intent().setClass(this, turnouts.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
                break;
            case R.id.routes_mnu:
                in = new Intent().setClass(this, routes.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
                break;
            case R.id.web_mnu:
                in = new Intent().setClass(this, web_activity.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.exit_mnu:
                navigatingAway = true;
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
                startActivityForResult(in, ACTIVITY_PREFS);   // reinitialize function buttons and labels on return
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.settings_mnu:
                in = new Intent().setClass(this, function_settings.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.about_mnu:
                in = new Intent().setClass(this, about_page.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.logviewer_menu:
                Intent logviewer = new Intent().setClass(this, LogViewerActivity.class);
                navigatingAway = true;
                startActivity(logviewer);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                speedUpdate(0);  // update all three throttles
                applySpeedRelatedOptions();  // update all three throttles

                break;
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    AlertDialog.Builder b = new AlertDialog.Builder(this);
                    b.setIcon(android.R.drawable.ic_dialog_alert);
                    b.setTitle("Will Not Work!");
                    b.setMessage("JMRI has the wiThrottle power control setting to off.\n\nWill now remove Power Icon.\n\nWill display again when JMRI setting is on.");
                    b.setCancelable(true);
                    b.setNegativeButton("OK", null);
                    AlertDialog alert = b.create();
                    alert.show();
                    mainapp.displayPowerStateMenuButton(TMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                break;
            case R.id.EditConsistT_menu:
                Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);
                consistEdit.putExtra("whichThrottle", 'T');
                navigatingAway = true;
                startActivityForResult(consistEdit, ACTIVITY_CONSIST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EditConsistS_menu:
                Intent consistEdit2 = new Intent().setClass(this, ConsistEdit.class);
                consistEdit2.putExtra("whichThrottle", 'S');
                navigatingAway = true;
                startActivityForResult(consistEdit2, ACTIVITY_CONSIST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EditConsistG_menu:
                Intent consistEdit3 = new Intent().setClass(this, ConsistEdit.class);
                consistEdit3.putExtra("whichThrottle", 'G');
                navigatingAway = true;
                startActivityForResult(consistEdit3, ACTIVITY_CONSIST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EditLightsConsistT_menu:
                Intent consistLightsEdit = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit.putExtra("whichThrottle", 'T');
                navigatingAway = true;
                startActivityForResult(consistLightsEdit, ACTIVITY_CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EditLightsConsistS_menu:
                Intent consistLightsEdit2 = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit2.putExtra("whichThrottle", 'S');
                navigatingAway = true;
                startActivityForResult(consistLightsEdit2, ACTIVITY_CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EditLightsConsistG_menu:
                Intent consistLightsEdit3 = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit3.putExtra("whichThrottle", 'G');
                navigatingAway = true;
                startActivityForResult(consistLightsEdit3, ACTIVITY_CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // handle return from menu items
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_SELECT_LOCO:
                if (resultCode == select_loco.RESULT_LOCO_EDIT)
                    ActivityConsistUpdate(resultCode, data.getExtras());
                if ((getConsist(whichVolume) != null) && (!getConsist(whichVolume).isActive())) {
                    setNextActiveThrottle(); // if consist on Volume throttle was released, move to next throttle
                } else {
                    if (IS_ESU_MCII) {
                        esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.STEADY_FLASH, true);
                    }
                }
                break;
            case ACTIVITY_CONSIST:         // edit loco or edit consist
                if (resultCode == ConsistEdit.RESULT_CON_EDIT)
                    ActivityConsistUpdate(resultCode, data.getExtras());
                break;
            case ACTIVITY_CONSIST_LIGHTS:         // edit consist lights
                break;   // nothing to do
            case ACTIVITY_PREFS: {    // edit prefs
                if (resultCode == preferences.RESULT_GAMEPAD) { // gamepad pref changed
                    // update tone generator volume
                    tg.release();
                    tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                            preferences.getIntPrefValue(prefs, "prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));
                    // update GamePad Support
                    setGamepadKeys();
                }
                break;
            }
        }

        // loop through all function buttons and
        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();
        set_labels();

    }

    private void ActivityConsistUpdate(int resultCode, Bundle extras) {
        if (extras != null) {
            char whichThrottle = extras.getChar("whichThrottle");
            int dir;
            int speed;
            if (whichThrottle == 'T') {
                dir = dirT;
                speed = (sbT == null ? 0 : sbT.getProgress());
            } else if (whichThrottle == 'G') {
                dir = dirG;
                speed = (sbG == null ? 0 : sbG.getProgress());
            } else {
                dir = dirS;
                speed = (sbS == null ? 0 : sbS.getProgress());
            }
            setEngineDirection(whichThrottle, dir, false);  // update direction for each loco in consist
            sendSpeedMsg(whichThrottle, speed);             // ensure all trailing units have the same speed as the lead engine
        }
    }

    // touch events outside the GestureOverlayView get caught here
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Log.d("Engine_Driver", "onTouch Title action " + event.getAction());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                gestureStart(event);
                break;
            case MotionEvent.ACTION_UP:
                gestureEnd(event);
                break;
            case MotionEvent.ACTION_MOVE:
                gestureMove(event);
                break;
            case MotionEvent.ACTION_CANCEL:
                gestureCancel(event);
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // if screen is locked
        if (isScreenLocked) {
            // check if we have a swipe up
            if (ev.getAction() == ACTION_DOWN) {
                gestureStart(ev);
            }
            if (ev.getAction() == ACTION_UP) {
                gestureEnd(ev);
            }
            // otherwise ignore the event
            return true;
        }
        // not locked ... proceed with normal processing
        return super.dispatchTouchEvent(ev);
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
        // Log.d("Engine_Driver", "gestureStart y=" + gestureStartY);

        // check if the sliders are already hidden by preference
        if (!prefs.getBoolean("hide_slider_preference", false)) {
            // if gesture is attempting to start over an enabled slider, ignore it and return immediately.
            if ((sbT.isEnabled() && gestureStartY >= T_top && gestureStartY <= T_bottom)
                    || (sbS.isEnabled() && gestureStartY >= S_top && gestureStartY <= S_bottom)
                    || (sbG.isEnabled() && gestureStartY >= G_top && gestureStartY <= G_bottom)) {
                // Log.d("Engine_Driver","exiting gestureStart");
                return;
            }
        }
        gestureInProgress = true;
        gestureFailed = false;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.throttle_msg_handler != null)
            mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d("Engine_Driver", "gestureMove action " + event.getAction());
        if (gestureInProgress) {
            // stop the gesture timeout timer
            mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);

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
                mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d("Engine_Driver", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);
        if (gestureInProgress) {
            float deltaX = (event.getX() - gestureStartX);
            float deltaY = (event.getY() - gestureStartY);
            float absDeltaX =  Math.abs(deltaX);
            float absDeltaY = Math.abs(deltaY);
            if ((absDeltaX > threaded_application.min_fling_distance) || (absDeltaY > threaded_application.min_fling_distance)) {
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                if (absDeltaX >= absDeltaY) {
                    // swipe left/right
                    if (!isScreenLocked) {
                        boolean swipeTurnouts = prefs.getBoolean("swipe_through_turnouts_preference",
                                getResources().getBoolean(R.bool.prefSwipeThroughTurnoutsDefaultValue));
                        swipeTurnouts = swipeTurnouts && mainapp.isTurnoutControlAllowed();  //also check the allowed flag
                        boolean swipeRoutes = prefs.getBoolean("swipe_through_routes_preference",
                                getResources().getBoolean(R.bool.prefSwipeThroughRoutesDefaultValue));
                        swipeRoutes = swipeRoutes && mainapp.isRouteControlAllowed();  //also check the allowed flag
                        // if swiping (to Turnouts or Routes screen) is enabled, process the swipe
                        if (swipeTurnouts || swipeRoutes) {
                            navigatingAway = true;
                            // left to right swipe goes to turnouts if enabled in prefs
                            if (deltaX > 0.0) {
                                // swipe left
                                Intent in;
                                if (swipeTurnouts) {
                                    in = new Intent().setClass(this, turnouts.class);
                                } else {
                                    in = new Intent().setClass(this, routes.class);
                                }
                                startActivity(in);
                                connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
                            }
                            // right to left swipe goes to routes if enabled in prefs
                            else {
                                // swipe right
                                Intent in;
                                if (swipeRoutes) {
                                    in = new Intent().setClass(this, routes.class);
                                } else {
                                    in = new Intent().setClass(this, turnouts.class);
                                }
                                startActivity(in);
                                connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
                            }
                        }
                    }
                }
                else {
                    // swipe up/down
                    if (deltaY > 0.0) {
                        // swipe down
                        if (!isScreenLocked) {
                            // enter or exit immersive mode only if the preference is set
                            if (immersiveModeIsOn) {
                                setImmersiveModeOff(webView);
                                Toast.makeText(getApplicationContext(), "Immersive mode temporarily disabled. To disable permanently change in preferences", Toast.LENGTH_SHORT).show();
                            } else {
                                setImmersiveModeOn(webView);
                            }
                        }
                    } else {
                        // swipe up
                        switch(prefSwipeUpOption) {
                            case "Hide Web View\n(requires 'Throttle Web View' preference)":
                                if (!(keepWebViewLocation.equals("none"))) { // show/hide the web view if the preference is set
                                    if (!webViewIsOn) {
                                        webViewLocation = keepWebViewLocation;
                                    } else {
                                        webViewLocation = "none";
                                        Toast.makeText(getApplicationContext(), "Web View temporarily hidden. To hide permanently change in preferences" + webViewLocation, Toast.LENGTH_SHORT).show();
                                    }
                                    webViewIsOn = !webViewIsOn;
                                    //Toast.makeText(getApplicationContext(), "Swipe Up - " + webViewLocation, Toast.LENGTH_SHORT).show();

                                    this.onResume();
                                }
                                break;
                            case "Lock and Dim Screen":
                                if (isScreenLocked) {
                                    isScreenLocked = false;
                                    Toast.makeText(getApplicationContext(), "Throttle Screen Unlocked", Toast.LENGTH_SHORT).show();
                                    setScreenBrightness(screenBrightnessOriginal);
                                    setScreenBrightnessMode(screenBrightnessModeOriginal);
                                } else {
                                    isScreenLocked = true;
                                    Toast.makeText(getApplicationContext(), "Throttle Screen Locked - Swipe up again to unlock", Toast.LENGTH_SHORT).show();
                                    screenBrightnessOriginal = getScreenBrightness();
                                    setScreenBrightness(screenBrightnessDim);
                                }
                                break;
                            case "Dim Screen":
                                if (screenDimmed) {
                                    screenDimmed = false;
                                    setScreenBrightness(screenBrightnessOriginal);
                                    setScreenBrightnessMode(screenBrightnessModeOriginal);
                                } else {
                                    screenDimmed = true;
                                    Toast.makeText(getApplicationContext(), "Throttle Screen Dimmed - Swipe up to restore", Toast.LENGTH_SHORT).show();
                                    screenBrightnessOriginal = getScreenBrightness();
                                    setScreenBrightness(screenBrightnessDim);
                                }
                                break;
                            case "Immersive Mode temporarily enable-disable":
                                if (immersiveModeIsOn) {
                                    setImmersiveModeOff(webView);
                                    Toast.makeText(getApplicationContext(), "Immersive mode temporarily disabled. To disable permanently change in preferences", Toast.LENGTH_SHORT).show();
                                } else {
                                    setImmersiveModeOn(webView);
                                }
                                break;

                        }
                    }
                }
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.throttle_msg_handler != null)
            mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);
        gestureInProgress = false;
        gestureFailed = true;
    }

    void gestureFailed(MotionEvent event) {
        // end the gesture
        gestureInProgress = false;
        gestureFailed = true;
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
                gestureFailed = true;
                // create a MOVE event to trigger the underlying control
                if (vThrotScr != null) {
                    // use uptimeMillis() rather than 0 for time in
                    // MotionEvent.obtain() call in throttle gestureStopped:
                    MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, gestureStartX,
                            gestureStartY, 0);
                    try {
                        vThrotScr.dispatchTouchEvent(event);
                    } catch (IllegalArgumentException e) {
                        Log.d("Engine_Driver", "gestureStopped trigger IllegalArgumentException, OS " + android.os.Build.VERSION.SDK_INT);
                    }
                }
            }
        }
    };

    // helper app to initialize statics (in case GC has not run since app last
    // shutdown)
    // call before instantiating any instances of class
    public static void initStatics() {
        scale = initialScale;
        clearHistory = false;
        currentUrl = null;
        firstUrl = null;
    }
    // prompt for Steal? Address, if yes, send message to execute the steal
    public void promptForSteal(String addr, char whichThrottle) {
        if (stealPromptActive) return;
        stealPromptActive = true;
        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(R.string.steal_title);
        b.setMessage(getString(R.string.steal_text, addr));
        b.setCancelable(true);
        b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() { //if yes pressed, tell ta to proceed with steal
            public void onClick(DialogInterface dialog, int id) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.STEAL, addr, whichThrottle);
                stealPromptActive = false;
            }
        });
        b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() { //if no pressed do nothing
            public void onClick(DialogInterface dialog, int id) {
                stealPromptActive = false;
            }
        });
        AlertDialog alert = b.create();
        alert.show();
    }

}
