/*Copyright (C) 2018 M. Steve Todd mstevetodd@gmail.com

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
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.GestureOverlayView;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.InputDevice;
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
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import eu.esu.mobilecontrol2.sdk.StopButtonFragment;
import eu.esu.mobilecontrol2.sdk.ThrottleFragment;
import eu.esu.mobilecontrol2.sdk.ThrottleScale;
import jmri.enginedriver.logviewer.ui.LogViewerActivity;

import static android.view.InputDevice.getDevice;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_0;
import static android.view.KeyEvent.KEYCODE_5;
import static android.view.KeyEvent.KEYCODE_A;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_BUTTON_A;
import static android.view.KeyEvent.KEYCODE_BUTTON_B;
import static android.view.KeyEvent.KEYCODE_BUTTON_L1;
import static android.view.KeyEvent.KEYCODE_BUTTON_L2;
import static android.view.KeyEvent.KEYCODE_BUTTON_R1;
import static android.view.KeyEvent.KEYCODE_BUTTON_R2;
import static android.view.KeyEvent.KEYCODE_BUTTON_X;
import static android.view.KeyEvent.KEYCODE_BUTTON_Y;
import static android.view.KeyEvent.KEYCODE_D;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_F;
import static android.view.KeyEvent.KEYCODE_N;
import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_SPACE;
import static android.view.KeyEvent.KEYCODE_T;
import static android.view.KeyEvent.KEYCODE_V;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.KeyEvent.KEYCODE_W;
import static android.view.KeyEvent.KEYCODE_X;
import static android.view.KeyEvent.KEYCODE_Z;

// for changing the screen brightness

// used for supporting Keyboard and Gamepad input;

public class throttle extends FragmentActivity implements android.gesture.GestureOverlayView.OnGestureListener {
////public class throttle extends Activity implements android.gesture.GestureOverlayView.OnGestureListener {

    protected threaded_application mainapp; // hold pointer to mainapp
    protected SharedPreferences prefs;

    protected static final int MAX_SCREEN_THROTTLES = 3;
    // activity codes
    public static final int ACTIVITY_PREFS = 0;
    public static final int ACTIVITY_SELECT_LOCO = 1;
    public static final int ACTIVITY_CONSIST = 2;
    public static final int ACTIVITY_CONSIST_LIGHTS = 3;
    public static final int ACTIVITY_GAMEPAD_TEST = 4;

    private static final int GONE = 8;
    private static final int VISIBLE = 0;
    protected static final int throttleMargin = 8; // margin between the throttles in dp
    protected static final int titleBar = 45; // estimate of lost screen height in dp

    // speed scale factors
    public static final int MAX_SPEED_VAL_WIT = 126;    // wit message maximum speed value, max speed slider value
    public static final int SPEED_STEP_CODE_14 = 8;     // wit speed step codes
    public static final int SPEED_STEP_CODE_27 = 4;
    public static final int SPEED_STEP_CODE_28 = 2;
    public static final int SPEED_STEP_CODE_128 = 1;
    public static final int SPEED_STEP_AUTO_MODE = -1;  // speed step pref value when set to AUTO mode
    protected static int[] maxSpeedSteps = {100,100,100,100,100,100 };             // decoder max speed steps
    protected static int max_throttle_change;          // maximum allowable change of the sliders
    protected static int speedStepPref = 100;
    private static double[] displayUnitScales;            // display units per slider count

    private static String VOLUME_INDICATOR = "v";
    private static int[] GAMEPAD_INDICATOR = {1, 2, 3};

    private static final String SELECTED_LOCO_INDICATOR_NONE = "None";
    private static final String SELECTED_LOCO_INDICATOR_GAMEPAD = "Gamepad";
    private static final String SELECTED_LOCO_INDICATOR_VOLUME = "Volume";
    private static final String SELECTED_LOCO_INDICATOR_BOTH = "Both";
    private String prefSelectedLocoIndicator = SELECTED_LOCO_INDICATOR_NONE;

    protected SeekBar[] sbs; // seekbars

    protected ViewGroup[] fbs; // function button tables

    protected Button[] bFwds; // buttons
    protected Button[] bStops;
    protected Button[] bRevs;
    protected Button[] bSels;
    protected Button[] bRSpds;
    protected Button[] bLSpds;

    protected TextView[] tvSpdLabs; // labels
    protected TextView[] tvSpdVals;

    protected TextView[] tvVols; // volume indicators

    protected TextView[] tvLeftDirInds; // direction indicators
    protected TextView[] tvRightDirInds;

    protected TextView[] tvGamePads;

    protected LinearLayout[] lls; // throttles
    protected LinearLayout[] llSetSpds;
    protected VerticalSeekBar[] vsbSpeeds;

    // SPDHT for Speed Id and Direction Button Heights
    protected LinearLayout[] llLocoIds;
    protected LinearLayout[] llLocoDirs;

    // SPDHT
    protected View vThrotScr;
    protected View vThrotScrWrap;

    private boolean stealPromptActive = false; //true while steal dialog is open
    protected boolean navigatingAway = false; // true if another activity was selected (false in onPause if going into background)
    private int whichVolume = 0;
    private int whichLastVolume = -1;
    private int whichLastGamepad1 = -1;


    // screen coordinates for throttle sliders, so we can ignore swipe on them
    protected int[] tops;
    protected int[] bottoms;

    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    private boolean gestureFailed = false; // gesture didn't meet velocity or distance requirement
    private boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;

    private String FUNCTION_BUTTON_LOOK_FOR_WHISTLE = "WHISTLE";
    private String FUNCTION_BUTTON_LOOK_FOR_HORN = "HORN";
    private String FUNCTION_BUTTON_LOOK_FOR_BELL = "BELL";
    private String FUNCTION_BUTTON_LOOK_FOR_HEAD = "HEAD";
    private String FUNCTION_BUTTON_LOOK_FOR_LIGHT = "LIGHT";
    private String FUNCTION_BUTTON_LOOK_FOR_REAR = "REAR";

    // function number-to-button maps
    protected LinkedHashMap<Integer, Button>[] functionMaps;

    // current direction
    private int[] dirs = {1,1,1,1,1,1};   // requested direction for each throttle (single or multiple engines)

    protected static final String WEB_VIEW_LOCATION_NONE = "none";
    protected static final String WEB_VIEW_LOCATION_BOTTOM = "Bottom";
    protected static final String WEB_VIEW_LOCATION_TOP = "Top";
    private static final String noUrl = "file:///android_asset/blank_page.html";
    private static final float initialScale = 1.5f;
    protected WebView webView = null;
    protected String webViewLocation = WEB_VIEW_LOCATION_NONE;

    private static float scale = initialScale;      // used to restore throttle web zoom level (after rotation)
    private static boolean clearHistory = false;    // flags webViewClient to clear history when page load finishes
    private static String firstUrl = null;          // desired first url when clearing history
    private static String currentUrl = null;
    private String currentTime = "";
    private Menu TMenu;
    static int REP_DELAY = 25;
    private int prefSpeedButtonsSpeedStep = 4;
    private int prefVolumeSpeedButtonsSpeedStep = 1;
    private int prefGamePadSpeedButtonsSpeedStep = 4;

    private static final int SPEED_COMMAND_FROM_BUTTONS = 0;
    private static final int SPEED_COMMAND_FROM_VOLUME = 1;
    private static final int SPEED_COMMAND_FROM_GAMEPAD = 2;

    protected String speedButtonLeftText;
    protected String speedButtonRightText;
    protected String speedButtonUpText;
    protected String speedButtonDownText;

    private Handler repeatUpdateHandler = new Handler();
    private boolean mAutoIncrement = false;
    private boolean mAutoDecrement = false;

    private static final int changeDelay = 1000;
    protected ChangeDelay[] changeTimers;

    protected boolean selectLocoRendered = false; // this will be true once set_labels() runs following rendering of the loco select textViews

    // used in the gesture for entering and exiting immersive mode
    private boolean immersiveModeIsOn;

    //used in the gesture for temporarily showing the Web View
    protected boolean webViewIsOn = false;
    private String prefSwipeUpOption;
    private String keepWebViewLocation = WEB_VIEW_LOCATION_NONE;
    // use for locking the screen on swipe up
    private boolean isScreenLocked = false;
    private boolean screenDimmed = false;
    private int screenBrightnessDim;
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

    private static final String SWIPE_UP_OPTION_WEB = "Hide Web View";
    private static final String SWIPE_UP_OPTION_LOCK = "Lock and Dim Screen";
    private static final String SWIPE_UP_OPTION_DIM = "Dim Screen";
    private static final String SWIPE_UP_OPTION_IMMERSIVE = "Immersive Mode";

    //private int screenBrightnessBright;
    private int screenBrightnessOriginal;
    private int screenBrightnessModeOriginal;

    // used to hold the direction change preferences
    boolean dirChangeWhileMoving;
    boolean stopOnDirectionChange;
    boolean locoSelectWhileMoving;

    // used for the GamePad Support
    private static final String WHICH_GAMEPAD_MODE_NONE = "None";
    private static final int DIRECTION_FORWARD = 1;
    private static final int DIRECTION_REVERSE = 0;
    // default to the iOS iCade mappings
    private String whichGamePadMode = WHICH_GAMEPAD_MODE_NONE;
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
                                            "Increase Speed", "Reverse", "Decrease Speed", "Forward", "All Stop"};

    //                              none     NextThr  Speed+    Speed-      Fwd         Rev       All Stop    F2         F1          F0        Stop
    private int[] gamePadKeys =     {0,        0,   KEYCODE_W, KEYCODE_X,   KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F};
    private int[] gamePadKeys_Up =  {0,        0,   KEYCODE_W,  KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F};

    // For TTS
    private int MY_TTS_DATA_CHECK_CODE = 1234;
    private static final String PREF_TTS_THROTTLE_RESPONSE_NONE = "None";
    private static final String PREF_TTS_THROTTLE_RESPONSE_THROTTLE = "Throttle";
    private static final String PREF_TTS_THROTTLE_RESPONSE_LOCO = "Loco";
    private static final String PREF_TTS_THROTTLE_RESPONSE_SPEED = "Speed";
    private static final String PREF_TTS_THROTTLE_RESPONSE_LOCO_SPEED = "LocoSpeed";

    private TextToSpeech myTts;
    private String lastTts = "none";
    private String prefTtsWhen = "None";
    private String prefTtsThrottleResponse = "Throttle";
    private boolean prefTtsGamepadTest = true;
    private boolean prefTtsGamepadTestComplete = true;
    private Time lastTtsTime;
    private static final String PREF_TT_WHEN_NONE = "None";
    private static final String PREF_TT_WHEN_KEY = "Key";
    private static final int TTS_MSG_VOLUME_THROTTLE = 1;
    private static final int TTS_MSG_GAMEPAD_THROTTLE = 2;
    private static final int TTS_MSG_GAMEPAD_GAMEPAD_TEST = 3;
    private static final int TTS_MSG_GAMEPAD_GAMEPAD_TEST_COMPLETE = 4;
    private static final int TTS_MSG_GAMEPAD_GAMEPAD_TEST_SKIPPED = 5;
    private static final int TTS_MSG_GAMEPAD_GAMEPAD_TEST_RESET = 6;
    private static final int TTS_MSG_GAMEPAD_GAMEPAD_TEST_FAIL = 7;

    private ToneGenerator tg;
    private Handler gamepadRepeatUpdateHandler = new Handler();
    private boolean mGamepadAutoIncrement = false;
    private boolean mGamepadAutoDecrement = false;
    private Handler volumeKeysRepeatUpdateHandler = new Handler();
    private boolean mVolumeKeysAutoIncrement = false;
    private boolean mVolumeKeysAutoDecrement = false;
    protected boolean prefDisableVolumeKeys = false;
    protected boolean prefVolumeKeysFollowLastTouchedThrottle = false;

    protected int layoutViewId;
    protected String prefThrottleScreenType;

    protected boolean prefThrottleViewImmersiveMode = false;
    protected boolean prefAlwaysUseDefaultFunctionLabels = false;
    protected int prefNumberOfDefaultFunctionLabels = 28;
    protected boolean prefDecreaseLocoNumberHeight = false;
    protected boolean pref_increase_slider_height_preference = false;
    protected boolean prefShowAddressInsteadOfName = false;
    protected boolean prefIncreaseWebViewSize = false;

    private int[] gamePadIds = {0,0,0,0,0,0}; // which device id if assigned to each of the three throttles
    private int[] gamePadThrottleAssignment = {-1,-1,-1,-1,-1,-1};
    private boolean usingMultiplePads = false;
    private int[] gamePadDeviceIds = {0,0,0,0,0,0,0}; // which device ids have we seen
    private int[] gamePadDeviceIdsTested = {-1,-1,-1,-1,-1,-1,-1}; // which device ids have we tested  -1 = not tested 0 = test started 1 = test passed 2 = test failed
    private int gamepadCount = 0;
    // preference to chnage the consist's on long clicks
    boolean prefConsistLightsLongClick;
    public static final int LIGHT_FOLLOW = 1;

    protected boolean prefSwapForwardReverseButtons = false;
    protected boolean prefSwapForwardReverseButtonsLongPress = false;
    private boolean[] currentSwapForwardReverseButtons = {false,false,false,false,false,false};

    protected boolean prefGamepadSwapForwardReverseWithScreenButtons = false;
    protected boolean prefGamepadTestEnforceTesting = true;

    private static final int GAMEPAD_TEST_PASS = 1;
    private static final int GAMEPAD_TEST_FAIL = 2;
    private static final int GAMEPAD_TEST_SKIPPED = 3;
    private static final int GAMEPAD_TEST_RESET = 9;
    private static final int GAMEPAD_GOOD = 1;
    private static final int GAMEPAD_BAD = 2;

    protected String DIRECTION_BUTTON_LEFT_TEXT = "Forward";
    protected String DIRECTION_BUTTON_RIGHT_TEXT = "Reverse";

    protected String[] FullLeftText = {DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT};
    protected String[] FullRightText = {DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT};
    protected String[] dirLeftIndicationText = {"", "", "", "", "", ""};
    protected String[] dirRightIndicationText = {"", "", "", "", "", ""};

    private static String GAMEPAD_FUNCTION_PREFIX = "Function ";

    protected String prefLeftDirectionButtons = DIRECTION_BUTTON_LEFT_TEXT;
    protected String prefRightDirectionButtons = DIRECTION_BUTTON_RIGHT_TEXT;
    protected int prefDirectionButtonLongPressDelay = 1000;
    private boolean isDirectionButtonLongPress;
    Handler directionButtonLongPressHandler = new Handler();

    // The following are used for the shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private shakeDetector shakeDetector;
    private static final String ACCELERATOROMETER_SHAKE_NONE = "None";
    private static final String ACCELERATOROMETER_SHAKE_NEXT_V = "NextV";
    private static final String ACCELERATOROMETER_SHAKE_DIM_SCREEN= "Dim";
    private static final String ACCELERATOROMETER_SHAKE_LOCK_DIM_SCREEN= "LockDim";
    private static final String ACCELERATOROMETER_SHAKE_WEB_VIEW= "Web";
    private static final String ACCELERATOROMETER_SHAKE_ALL_STOP= "AllStop";
    private String prefAccelerometerShake = ACCELERATOROMETER_SHAKE_NONE;
    private boolean accelerometerCurrent = false;

    protected static final String THEME_DEFAULT = "Default";

    // For ESU MobileControlII
    private static final boolean IS_ESU_MCII = MobileControl2.isMobileControl2();
    private boolean isEsuMc2Stopped = false; // for tracking status of Stop button
    private boolean isEsuMc2AllStopped = false; // for tracking if all throttles stopped
    private ThrottleFragment esuThrottleFragment;
    private StopButtonFragment esuStopButtonFragment;
    private EsuMc2LedControl esuMc2Led = new EsuMc2LedControl();
    private Handler esuButtonRepeatUpdateHandler = new Handler();
    private boolean esuButtonAutoIncrement = false;
    private boolean esuButtonAutoDecrement = false;
    private boolean prefEsuMc2EndStopDirectionChange = true;
    private boolean prefEsuMc2StopButtonShortPress = true;

    // Create default ESU MCII ThrottleScale for each throttle
    private ThrottleScale[] esuThrottleScales
            = {new ThrottleScale(10, 127),
            new ThrottleScale(10, 127),
            new ThrottleScale(10, 127),
            new ThrottleScale(10, 127),
            new ThrottleScale(10, 127),
            new ThrottleScale(10, 127)};

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
        int whichThrottle;

        private RptUpdater(int WhichThrottle) {
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
                incrementSpeed(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
                repeatUpdateHandler.postDelayed(new RptUpdater(whichThrottle), REP_DELAY);
            } else if (mAutoDecrement) {
                decrementSpeed(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
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

//            Log.d("Engine_Driver", "throttle handleMessage " + response_str );

            switch (msg.what) {
                case message_type.RESPONSE: { // handle messages from WiThrottle server
                    if (response_str.length() < 2)
                        return;  //bail if too short, to avoid crash
                    char com1 = response_str.charAt(0);
                    int whichThrottle = mainapp.throttleCharToInt(response_str.charAt(1)); // '0', '1'',2' etc.

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
                            if ((whichThrottle>=0) && (whichThrottle<mainapp.numThrottles)) {
                                if (com2 == '+' || com2 == 'L') { // if loco added or function labels updated
                                    if (com2 == ('+')) {
                                        // set_default_function_labels();
                                        enable_disable_buttons(whichThrottle); // direction and slider: pass whichthrottle
                                    }
                                    // loop through all function buttons and set label and dcc functions (based on settings) or hide if no label
                                    set_function_labels_and_listeners_for_view(whichThrottle);
                                    enable_disable_buttons_for_view(fbs[whichThrottle], true);
                                    set_labels();
                                } else if (com2 == '-') { // if loco removed
                                    removeLoco(whichThrottle);
                                    swapToNextAvilableThrottleForGamePad(whichThrottle, true); // see if we can/need to move the gamepad to another throttle
                                    gamePadIds[whichThrottle] = 0;
                                    gamePadThrottleAssignment[whichThrottle] = -1;

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
                                        int curDir = dirs[whichThrottle];
                                        con = mainapp.consists[whichThrottle];

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

                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                            enable_disable_buttons(com1); // pass whichthrottle
                            set_labels();
                            break;

                        case 'R':
                            if (whichThrottle >= 0 && whichThrottle <= mainapp.numThrottles) {
                                set_function_labels_and_listeners_for_view(whichThrottle);
                                enable_disable_buttons_for_view(fbs[whichThrottle], true);
                                set_labels();

                            } else {
                                try {
                                    String scom2 = response_str.substring(1, 6);
                                    if (scom2.equals("PF}|{")) {
                                        whichThrottle = mainapp.throttleCharToInt(response_str.charAt(6));
                                        set_all_function_states(whichThrottle);
                                    }
                                } catch (IndexOutOfBoundsException ignored) {
                                }
                            }
                            break;
                        case 'P': // panel info
//                            if (whichThrottle == 'W') { // PW - web server port info
                            if (whichThrottle == 39) { // PW - web server port info
                                initWeb();
                                set_labels();
                            }
//                            if (whichThrottle == 'P') { // PP - power state change
                            if (whichThrottle == 32) { // PP - power state change
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
                    promptForSteal( msg.obj.toString(), msg.arg1);
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

            if (mainapp.androidVersion >= mainapp.minScreenDimNewMethodVersion) {

                setScreenBrightnessMode(SCREEN_BRIGHTNESS_MODE_MANUAL);

                if (Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessValue)) {
                    Log.d("Engine_Driver", "screen brightness successfully changed to " + brightnessValue);
                } else {
                    Log.e("Engine_Driver", "screen brightness was NOT changed to " + brightnessValue);
                }
            } else {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = ((float) brightnessValue)/255;
                getWindow().setAttributes(lp);
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

        if (mainapp.androidVersion >= mainapp.minScreenDimNewMethodVersion) {
            if(brightnessModeValue >= 0 && brightnessModeValue <= 1){
                if (!Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessModeValue)) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastUnableToSetBrightness), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    protected int getScreenBrightnessMode(){
        Context mContext;
        mContext = getApplicationContext();
        int BrightnessModeValue = 0;

        if (mainapp.androidVersion >= mainapp.minScreenDimNewMethodVersion) {
            BrightnessModeValue = Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    0
            );
        }
        return BrightnessModeValue;
    }

    protected void setImmersiveModeOn(View webView) {
        immersiveModeIsOn = false;

        if (prefThrottleViewImmersiveMode) {   // if the preference is set use Immersive mode
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

    protected void setImmersiveModeOff(View webView) {
         immersiveModeIsOn = false;

        if (prefThrottleViewImmersiveMode) {   // if the preference is set use Immersive mode
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                webView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_VISIBLE);
            }
            webView.invalidate();
        }
    }

    private boolean directionButtonsAreCurrentlyReversed(int throttleIndexNo) {
        boolean isOk = false;
        if ( ((!prefSwapForwardReverseButtons) && (currentSwapForwardReverseButtons[throttleIndexNo]))
                || (((prefSwapForwardReverseButtons) && (!currentSwapForwardReverseButtons[throttleIndexNo]))) ) {
            isOk= true;
        }
        return isOk;
    }

    private boolean gamepadDirectionButtonsAreCurrentlyReversed(int throttleIndexNo) {
        boolean isOk = false;
        if ((prefGamepadSwapForwardReverseWithScreenButtons)
                && (((currentSwapForwardReverseButtons[throttleIndexNo]) && (!prefSwapForwardReverseButtons))
                    ||((!currentSwapForwardReverseButtons[throttleIndexNo]) && (prefSwapForwardReverseButtons)))) {
            isOk= true;
        }
        return isOk;
    }

    // set or restore the screen brightness when used for the Swipe Up or Shake
    private void setRestoreScreenDim(String toastMsg){
        if (screenDimmed) {
            screenDimmed = false;
            setScreenBrightness(screenBrightnessOriginal);
            setScreenBrightnessMode(screenBrightnessModeOriginal);
        } else {
            screenDimmed = true;
            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            screenBrightnessOriginal = getScreenBrightness();
            setScreenBrightness(screenBrightnessDim);
        }

    }

    // set or restore the screen brightness and lock or unlock the sceen when used for the Swipe Up or Shake
    private void setRestoreScreenLockDim( String toastMsg) {
        if (isScreenLocked) {
            isScreenLocked = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastThrottleScreenUnlocked), Toast.LENGTH_SHORT).show();
            setScreenBrightness(screenBrightnessOriginal);
            setScreenBrightnessMode(screenBrightnessModeOriginal);
        } else {
            isScreenLocked = true;
            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            screenBrightnessOriginal = getScreenBrightness();
            setScreenBrightness(screenBrightnessDim);
        }

    }

    private void setupSensor () {
        if (!prefAccelerometerShake.equals(ACCELERATOROMETER_SHAKE_NONE)) {
            // ShakeDetector initialization
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                shakeDetector = new shakeDetector(getApplicationContext());
                shakeDetector.setOnShakeListener(new shakeDetector.OnShakeListener() {

                    @Override
                    public void onShake(int count) {

                        switch (prefAccelerometerShake) {
                            case ACCELERATOROMETER_SHAKE_WEB_VIEW:
                                if ((webViewLocation.equals(WEB_VIEW_LOCATION_NONE)) && (keepWebViewLocation.equals(WEB_VIEW_LOCATION_NONE))) {
                                    GamepadFeedbackSound(true);
                                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastShakeWebViewUnavailable), Toast.LENGTH_SHORT).show();
                                } else {
                                    GamepadFeedbackSound(false);
                                    showHideWebView(getApplicationContext().getResources().getString(R.string.toastShakeWebViewHidden));
                                }
                                break;
                            case ACCELERATOROMETER_SHAKE_NEXT_V:
                                GamepadFeedbackSound(false);
                                setNextActiveThrottle();
                                speakWords(TTS_MSG_VOLUME_THROTTLE, whichVolume);
                                break;
                            case ACCELERATOROMETER_SHAKE_LOCK_DIM_SCREEN:
                                GamepadFeedbackSound(false);
                                setRestoreScreenLockDim(getApplicationContext().getResources().getString(R.string.toastShakeScreenLocked));
                                break;
                            case ACCELERATOROMETER_SHAKE_DIM_SCREEN:
                                GamepadFeedbackSound(false);
                                setRestoreScreenDim(getApplicationContext().getResources().getString(R.string.toastShakeScreenDimmed));
                                break;
                            case ACCELERATOROMETER_SHAKE_ALL_STOP:
                                GamepadFeedbackSound(false);
                                speedUpdateAndNotify(0);         // update all three throttles
                            }
                    }
                });
                accelerometerCurrent = true;
            } else {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastAccelerometerNotFound), Toast.LENGTH_LONG).show();
            }

        }
    }

    // get all the preferences that should be read when the activity is created or resumes
    protected void getCommonPrefs(boolean isCreate) {

        if (isCreate) {  //only do onCreate
            webViewLocation = prefs.getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
        }

//        DIRECTION_BUTTON_LEFT_TEXT = getApplicationContext().getResources().getString(R.string.prefLeftDirectionButtonsDefaultValue).trim();
//        DIRECTION_BUTTON_RIGHT_TEXT = getApplicationContext().getResources().getString(R.string.prefRightDirectionButtonsDefaultValue).trim();

        prefDirectionButtonLongPressDelay = preferences.getIntPrefValue(prefs, "prefDirectionButtonLongPressDelay", getApplicationContext().getResources().getString(R.string.prefDirectionButtonLongPressDelayDefaultValue));

        FUNCTION_BUTTON_LOOK_FOR_WHISTLE = getApplicationContext().getResources().getString(R.string.functionButtonLookForWhistle).trim();
        FUNCTION_BUTTON_LOOK_FOR_HORN = getApplicationContext().getResources().getString(R.string.functionButtonLookForHorn).trim();
        FUNCTION_BUTTON_LOOK_FOR_BELL = getApplicationContext().getResources().getString(R.string.functionButtonLookForBell).trim();
        FUNCTION_BUTTON_LOOK_FOR_HEAD = getApplicationContext().getResources().getString(R.string.functionButtonLookForHead).trim();
        FUNCTION_BUTTON_LOOK_FOR_LIGHT = getApplicationContext().getResources().getString(R.string.functionButtonLookForLight).trim();
        FUNCTION_BUTTON_LOOK_FOR_REAR = getApplicationContext().getResources().getString(R.string.functionButtonLookForRear).trim();


        // increase height of throttle slider (if requested in preferences)
        pref_increase_slider_height_preference = prefs.getBoolean("increase_slider_height_preference", getResources().getBoolean(R.bool.prefIncreaseSliderHeightDefaultValue));

        // decrease height of Loco Id (if requested in preferences)
        prefDecreaseLocoNumberHeight = prefs.getBoolean("prefDecreaseLocoNumberHeight", getResources().getBoolean(R.bool.prefDecreaseLocoNumberHeightDefaultValue));

        // increase the web view height if the preference is set
        prefIncreaseWebViewSize = prefs.getBoolean("prefIncreaseWebViewSize", getResources().getBoolean(R.bool.prefIncreaseWebViewSizeDefaultValue));

        prefThrottleViewImmersiveMode = prefs.getBoolean("prefThrottleViewImmersiveMode", getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeDefaultValue));

        prefShowAddressInsteadOfName = prefs.getBoolean("prefShowAddressInsteadOfName", getResources().getBoolean(R.bool.prefShowAddressInsteadOfNameDefaultValue));

        prefSwipeUpOption = prefs.getString("SwipeUpOption", getApplicationContext().getResources().getString(R.string.prefSwipeUpOptionDefaultValue));
        isScreenLocked = false;

        dirChangeWhileMoving = prefs.getBoolean("DirChangeWhileMovingPreference", getResources().getBoolean(R.bool.prefDirChangeWhileMovingDefaultValue));
        stopOnDirectionChange = prefs.getBoolean("prefStopOnDirectionChange", getResources().getBoolean(R.bool.prefStopOnDirectionChangeDefaultValue));
        locoSelectWhileMoving = prefs.getBoolean("SelLocoWhileMovingPreference", getResources().getBoolean(R.bool.prefSelLocoWhileMovingDefaultValue));

        screenBrightnessDim = Integer.parseInt(prefs.getString("prefScreenBrightnessDim", getResources().getString(R.string.prefScreenBrightnessDimDefaultValue))) * 255 /100;
        //screenBrightnessBright = Integer.parseInt(prefs.getString("prefScreenBrightnessBright", getResources().getString(R.string.prefScreenBrightnessBrightDefaultValue))) * 255 /100;

        prefConsistLightsLongClick = prefs.getBoolean("ConsistLightsLongClickPreference", getResources().getBoolean(R.bool.prefConsistLightsLongClickDefaultValue));

        prefGamepadTestEnforceTesting = prefs.getBoolean("prefGamepadTestEnforceTesting", getResources().getBoolean(R.bool.prefGamepadTestEnforceTestingDefaultValue));

        prefDisableVolumeKeys = prefs.getBoolean("prefDisableVolumeKeys", getResources().getBoolean(R.bool.prefDisableVolumeKeysDefaultValue));

        prefSelectedLocoIndicator = prefs.getString("prefSelectedLocoIndicator", getResources().getString(R.string.prefSelectedLocoIndicatorDefaultValue));

        prefVolumeKeysFollowLastTouchedThrottle = prefs.getBoolean("prefVolumeKeysFollowLastTouchedThrottle", getResources().getBoolean(R.bool.prefVolumeKeysFollowLastTouchedThrottleDefaultValue));

        // Ignore the labels for the loco in the Roster and use the defaults... if requested in preferences
        prefAlwaysUseDefaultFunctionLabels = prefs.getBoolean("prefAlwaysUseDefaultFunctionLabels", getResources().getBoolean(R.bool.prefAlwaysUseDefaultFunctionLabelsDefaultValue));
        prefNumberOfDefaultFunctionLabels = Integer.parseInt(prefs.getString("prefNumberOfDefaultFunctionLabels", getResources().getString(R.string.prefNumberOfDefaultFunctionLabelsDefaultValue)));

        prefAccelerometerShake = prefs.getString("prefAccelerometerShake", getApplicationContext().getResources().getString(R.string.prefAccelerometerShakeDefaultValue));

        // set speed buttons speed step
        prefSpeedButtonsSpeedStep = preferences.getIntPrefValue(prefs, "speed_arrows_throttle_speed_step", "4");
        prefVolumeSpeedButtonsSpeedStep= preferences.getIntPrefValue(prefs, "prefVolumeSpeedButtonsSpeedStep", getApplicationContext().getResources().getString(R.string.prefVolumeSpeedButtonsSpeedStepDefaultValue));
        prefGamePadSpeedButtonsSpeedStep = preferences.getIntPrefValue(prefs, "prefGamePadSpeedButtonsSpeedStep", getApplicationContext().getResources().getString(R.string.prefVolumeSpeedButtonsSpeedStepDefaultValue));

        prefTtsWhen = prefs.getString("prefTtsWhen", getResources().getString(R.string.prefTtsWhenDefaultValue));
        prefTtsThrottleResponse = prefs.getString("prefTtsThrottleResponse", getResources().getString(R.string.prefTtsThrottleResponseDefaultValue));
        prefTtsGamepadTest = prefs.getBoolean("prefTtsGamepadTest", getResources().getBoolean(R.bool.prefTtsGamepadTestDefaultValue));
        prefTtsGamepadTestComplete = prefs.getBoolean("prefTtsGamepadTestComplete", getResources().getBoolean(R.bool.prefTtsGamepadTestCompleteDefaultValue));

        prefEsuMc2EndStopDirectionChange = prefs.getBoolean("prefEsuMc2EndStopDirectionChange", getResources().getBoolean(R.bool.prefEsuMc2EndStopDirectionChangeDefaultValue));
        prefEsuMc2StopButtonShortPress = prefs.getBoolean("prefEsuMc2StopButtonShortPress", getResources().getBoolean(R.bool.prefEsuMc2StopButtonShortPressDefaultValue));

        mainapp.numThrottles = mainapp.Numeralise(prefs.getString("NumThrottle", getResources().getString(R.string.NumThrottleDefaulValue)));
        prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
    }

    protected void getDirectionButtonPrefs() {
        DIRECTION_BUTTON_LEFT_TEXT = getApplicationContext().getResources().getString(R.string.forward);
        DIRECTION_BUTTON_RIGHT_TEXT = getApplicationContext().getResources().getString(R.string.reverse);

        speedButtonLeftText = getApplicationContext().getResources().getString(R.string.LeftButton);
        speedButtonRightText = getApplicationContext().getResources().getString(R.string.RightButton);
        speedButtonUpText = getApplicationContext().getResources().getString(R.string.UpButton);
        speedButtonDownText = getApplicationContext().getResources().getString(R.string.DownButton);

        prefSwapForwardReverseButtons = prefs.getBoolean("prefSwapForwardReverseButtons", getResources().getBoolean(R.bool.prefSwapForwardReverseButtonsDefaultValue));
        prefSwapForwardReverseButtonsLongPress = prefs.getBoolean("prefSwapForwardReverseButtonsLongPress", getResources().getBoolean(R.bool.prefSwapForwardReverseButtonsLongPressDefaultValue));

        prefLeftDirectionButtons = prefs.getString("prefLeftDirectionButtons", getApplicationContext().getResources().getString(R.string.prefLeftDirectionButtonsDefaultValue)).trim();
        prefRightDirectionButtons = prefs.getString("prefRightDirectionButtons", getApplicationContext().getResources().getString(R.string.prefRightDirectionButtonsDefaultValue)).trim();

        prefGamepadSwapForwardReverseWithScreenButtons = prefs.getBoolean("prefGamepadSwapForwardReverseWithScreenButtons", getResources().getBoolean(R.bool.prefGamepadSwapForwardReverseWithScreenButtonsDefaultValue));
    }

    private void setDirectionButtonLabels() {
        String dirLeftText;
        String dirRightText;
        for (int throttleIndex = 0; throttleIndex<mainapp.maxThrottles; throttleIndex++) {
            FullLeftText[throttleIndex] = DIRECTION_BUTTON_LEFT_TEXT;
            FullRightText[throttleIndex] = DIRECTION_BUTTON_RIGHT_TEXT;
            dirLeftIndicationText[throttleIndex] = "";
            dirRightIndicationText[throttleIndex] = "";
        }

        if ( ((prefLeftDirectionButtons.equals(DIRECTION_BUTTON_LEFT_TEXT)) && (prefRightDirectionButtons.equals(DIRECTION_BUTTON_RIGHT_TEXT)))
                || ((prefLeftDirectionButtons.equals("")) && (prefRightDirectionButtons.equals(""))) ){
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
                if (directionButtonsAreCurrentlyReversed(throttleIndex)) {
                    FullLeftText[throttleIndex] = DIRECTION_BUTTON_RIGHT_TEXT;
                    FullRightText[throttleIndex] = DIRECTION_BUTTON_LEFT_TEXT;
                }
            }
        } else {
            dirLeftText = prefLeftDirectionButtons;
            dirRightText = prefRightDirectionButtons;

            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
                if (!directionButtonsAreCurrentlyReversed(throttleIndex)) {
                    FullLeftText[throttleIndex] = dirLeftText;
                    dirLeftIndicationText[throttleIndex] = getApplicationContext().getResources().getString(R.string.loco_direction_left_extra);
                    FullRightText[throttleIndex] = dirRightText;
                    dirRightIndicationText[throttleIndex] = getApplicationContext().getResources().getString(R.string.loco_direction_right_extra);
                } else {
                    FullLeftText[throttleIndex] = dirLeftText;
                    dirLeftIndicationText[throttleIndex] = getApplicationContext().getResources().getString(R.string.loco_direction_right_extra);
                    FullRightText[throttleIndex] = dirRightText;
                    dirRightIndicationText[throttleIndex] = getApplicationContext().getResources().getString(R.string.loco_direction_left_extra);
                }
            }
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
            bFwds[throttleIndex].setText(FullLeftText[throttleIndex]);
            bRevs[throttleIndex].setText(FullRightText[throttleIndex]);
            tvLeftDirInds[throttleIndex].setText(dirLeftIndicationText[throttleIndex]);
            tvRightDirInds[throttleIndex].setText(dirRightIndicationText[throttleIndex]);
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

    // used for the swipe up option and the shake, to show or hide the web view at the bottom of the throttle page
    private void showHideWebView(String toastMsg) {
        if (!(keepWebViewLocation.equals(WEB_VIEW_LOCATION_NONE))) { // show/hide the web view if the preference is set
            if (!webViewIsOn) {
                webViewLocation = keepWebViewLocation;
            } else {
                webViewLocation = WEB_VIEW_LOCATION_NONE;
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            }
            webViewIsOn = !webViewIsOn;

            this.onResume();
        }
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

    protected void removeLoco(int whichThrottle) {
        disable_buttons(whichThrottle);         // direction and slider
        set_function_labels_and_listeners_for_view(whichThrottle);
        set_labels();
    }

    // process WiT speed report
    // update speed slider if didn't just send a speed update to WiT
    void speedUpdateWiT(int whichThrottle, int speed) {
        if (speed < 0)
            speed = 0;
            if (!changeTimers[whichThrottle].delayInProg) {
                sbs[whichThrottle].setProgress(speed);
                // Now update ESU MCII Knob position
                if (IS_ESU_MCII) {
                    Log.d("Engine_Driver", "ESU_MCII: Move knob request for WiT speed report");
                    setEsuThrottleKnobPosition(whichThrottle, speed);
                }
            }
    }

    SeekBar getThrottleSlider(int whichThrottle) {
        return sbs[whichThrottle];
    }

    double getDisplayUnitScale(int whichThrottle) {
        return displayUnitScales[whichThrottle];
    }

    // set speed slider to absolute value on all throttles
    void speedUpdate(int speed) {
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            speedUpdate(throttleIndex, speed);
        }
    }

    // set speed slider to absolute value on one throttle
    void speedUpdate(int whichThrottle, int speed) {
        if (speed < 0)
            speed = 0;
        getThrottleSlider(whichThrottle).setProgress(speed);
    }

    // get the current speed of the throttle
    int getSpeed(int whichThrottle) {
        return getThrottleSlider(whichThrottle).getProgress();
    }

    private int getScaleSpeed(int whichThrottle) {
        Consist con = mainapp.consists[whichThrottle];
        int speed = getSpeed(whichThrottle);
        double speedScale = getDisplayUnitScale(whichThrottle);
        if (speed < 0) {
            speed = 0;
        }
        int scaleSpeed = (int) Math.round(speed * speedScale) -1;

        return scaleSpeed;
    }


    int getMaxSpeed(int whichThrottle) {
        return getThrottleSlider(whichThrottle).getMax();
    }

    int getMinSpeed(int whichThrottle) {
        return 0;
    }

    // returns true if throttle is set for the maximum speed
    boolean atMaxSpeed(int whichThrottle) {
        return (getSpeed(whichThrottle) >= getMaxSpeed(whichThrottle));
    }

    // returns true if throttle is set for the minimum speed
    boolean atMinSpeed(int whichThrottle) {
        return (getSpeed(whichThrottle) <= getMinSpeed(whichThrottle));
    }

    // change speed slider by scaled display unit value
    int speedChange(int whichThrottle, int change) {
        SeekBar throttle_slider = getThrottleSlider(whichThrottle);
        double displayUnitScale = getDisplayUnitScale(whichThrottle);
        int lastSpeed = throttle_slider.getProgress();
        int lastScaleSpeed = (int) Math.round(lastSpeed * displayUnitScale);
        int scaleSpeed = (int) lastScaleSpeed + change;
        int speed = (int) Math.round(scaleSpeed / displayUnitScale);
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

    public void decrementSpeed(int whichThrottle, int from) {
        switch (from) {
            case SPEED_COMMAND_FROM_BUTTONS:
                speedChangeAndNotify(whichThrottle, -prefSpeedButtonsSpeedStep);
                break;
            case SPEED_COMMAND_FROM_VOLUME:
                speedChangeAndNotify(whichThrottle, -prefVolumeSpeedButtonsSpeedStep);
                break;
            case SPEED_COMMAND_FROM_GAMEPAD:
                speedChangeAndNotify(whichThrottle, -prefGamePadSpeedButtonsSpeedStep);
                break;
        }
    }

    public void incrementSpeed(int whichThrottle, int from) {
        switch (from) {
            case SPEED_COMMAND_FROM_BUTTONS:
                speedChangeAndNotify(whichThrottle, prefSpeedButtonsSpeedStep);
                break;
            case SPEED_COMMAND_FROM_VOLUME:
                speedChangeAndNotify(whichThrottle, prefVolumeSpeedButtonsSpeedStep);
                break;
            case SPEED_COMMAND_FROM_GAMEPAD:
                speedChangeAndNotify(whichThrottle, prefGamePadSpeedButtonsSpeedStep);
                break;
        }
    }

    // set speed slider and notify server for all throttles
    void speedUpdateAndNotify(int speed) {
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            speedUpdateAndNotify(throttleIndex, speed);
        }
    }

    // set speed slider and notify server for one throttle
    void speedUpdateAndNotify(int whichThrottle, int speed) {
        speedUpdateAndNotify(whichThrottle, speed, true);
    }

    // set speed slider and notify server for one throttle and optionally move ESU MCII knob
    void speedUpdateAndNotify(int whichThrottle, int speed, boolean moveMc2Knob) {
        speedUpdate(whichThrottle, speed);
        sendSpeedMsg(whichThrottle, speed);
        // Now update ESU MCII Knob position
        if (IS_ESU_MCII && moveMc2Knob) {
            Log.d("Engine_Driver", "ESU_MCII: Move knob request for speed update");
            setEsuThrottleKnobPosition(whichThrottle, speed);
        }
    }

    // change speed slider by scaled value and notify server
    public void speedChangeAndNotify(int whichThrottle, int change) {
        int speed = speedChange(whichThrottle, change);
        sendSpeedMsg(whichThrottle, speed);
        // Now update ESU MCII Knob position
        if (IS_ESU_MCII) {
            Log.d("Engine_Driver", "ESU_MCII: Move knob request for speed change");
            setEsuThrottleKnobPosition(whichThrottle, speed);
        }
    }

    // set the displayed numeric speed value
    protected void setDisplayedSpeed(int whichThrottle, int speed) {
        TextView speed_label;
        double speedScale = getDisplayUnitScale(whichThrottle);
        speed_label = tvSpdVals[whichThrottle];
        if (speed < 0) {
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastThrottleAlertEstop, getConsistAddressString(whichThrottle)), Toast.LENGTH_LONG).show();
            speed = 0;
        }
        int scaleSpeed = (int) Math.round(speed * speedScale);
        speed_label.setText(Integer.toString(scaleSpeed));
    }

    //adjust maxspeedsteps from code passed from JMRI, but only if set to Auto, else do not change
    private void setSpeedStepsFromWiT(int whichThrottle, int speedStepCode) {
        int maxSpeedStep = 100;
        switch (speedStepCode) {
            case SPEED_STEP_CODE_128:
                maxSpeedStep = 126;
                break;
            case SPEED_STEP_CODE_27:
                maxSpeedStep = 27;
                break;
            case SPEED_STEP_CODE_14:
                maxSpeedStep = 14;
                break;
            case SPEED_STEP_CODE_28:
                maxSpeedStep = 28;
                break;
        }

        int zeroTrim = preferences.getIntPrefValue(prefs,"prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
        ThrottleScale esuThrottleScale = new ThrottleScale(zeroTrim, maxSpeedStep + 1);

        maxSpeedSteps[whichThrottle] = maxSpeedStep;
        esuThrottleScales[whichThrottle] = esuThrottleScale;
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

    protected void setDisplayUnitScale(int whichThrottle) {
        int maxStep;
        maxStep = getSpeedSteps(maxSpeedSteps[whichThrottle]);
        displayUnitScales[whichThrottle] = calcDisplayUnitScale(maxStep);
        tvSpdLabs[whichThrottle].setText(calcDisplayUnitLabel(maxStep));
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
    void setEngineDirection(int whichThrottle, int direction, boolean skipLead) {
        Consist con;
        con = mainapp.consists[whichThrottle];
        dirs[whichThrottle] = direction;

        String leadAddr = con.getLeadAddr();
        for (String addr : con.getList()) { // loop through each engine in consist
            if (!skipLead || (addr != null && !addr.equals(leadAddr))) {
                int locoDir = direction;
                try {
                    if (con.isReverseOfLead(addr)) // if engine faces opposite of lead loco
                        locoDir ^= 1; // then reverse the commanded direction
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DIRECTION, addr, whichThrottle, locoDir);
                } catch (Exception e) { // isReverseOfLead returns null if addr is not in con - should never happen since we are walking through consist list
                    Log.d("Engine_Driver", "throttle " + mainapp.throttleIntToString(whichThrottle) + " direction change for unselected loco " + addr);
                }
            }
        }
    }

    private String getConsistAddressString(int whichThrottle) {
        String result = "";
        if (!prefShowAddressInsteadOfName) {
            result = mainapp.consists[whichThrottle].toString();
        } else {
            result = mainapp.consists[whichThrottle].formatConsistAddr();
        }

        return result;
    }


    // get the consist for the specified throttle
    private Consist getConsist(int whichThrottle) {
        return mainapp.consists[whichThrottle];
    }

    // get the indicated direction from the button pressed state
    private int getDirection(int whichThrottle) {
        return dirs[whichThrottle];
    }

    // update the direction indicators
    void showDirectionIndications() {
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            showDirectionIndication(throttleIndex, dirs[throttleIndex]);
        }
    }

    // indicate direction using the button pressed state
    void showDirectionIndication(int whichThrottle, int direction) {

        boolean setLeftDirectionButtonEnabled = true;
        if (direction == 0) {  //0=reverse 1=forward
            if (!directionButtonsAreCurrentlyReversed(whichThrottle))
                setLeftDirectionButtonEnabled = false;
            else
                setLeftDirectionButtonEnabled = true;
        } else {
            if (!directionButtonsAreCurrentlyReversed(whichThrottle))
                setLeftDirectionButtonEnabled = true;
            else
                setLeftDirectionButtonEnabled = false;
        }

        if (!getConsist(whichThrottle).isActive()) {
            bFwds[whichThrottle].setSelected(false);
            bRevs[whichThrottle].setSelected(false);
        } else {
            if (!setLeftDirectionButtonEnabled) {
                bFwds[whichThrottle].setSelected(false);
                bRevs[whichThrottle].setSelected(true);
                bFwds[whichThrottle].setTypeface(null, Typeface.NORMAL);
                bRevs[whichThrottle].setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                if ((getSpeed(whichThrottle) > 0) && (!dirChangeWhileMoving)) {
                    bFwds[whichThrottle].setEnabled(false);
                }
                bRevs[whichThrottle].setEnabled(true);
            } else {
                bFwds[whichThrottle].setSelected(true);
                bRevs[whichThrottle].setSelected(false);
                bFwds[whichThrottle].setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bRevs[whichThrottle].setTypeface(null, Typeface.NORMAL);
                bFwds[whichThrottle].setEnabled(true);
                if ((getSpeed(whichThrottle) > 0) && (!dirChangeWhileMoving)) {
                    bRevs[whichThrottle].setEnabled(false);
                }
            }
        }

    }

    // indicate requested direction using the button typeface
    void showDirectionRequest(int whichThrottle, int direction) {
        showDirectionIndication(whichThrottle, direction);
    }

    //
    // processes a direction change if allowed by related preferences and current speed
    // returns true if throttle direction matches the requested direction
    private boolean changeDirectionIfAllowed(int whichThrottle, int direction) {
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

    private boolean isChangeDirectionAllowed(int whichThrottle) {
        // check whether direction change is permitted
        boolean isAllowed = false;
        if (getConsist(whichThrottle).isActive()) {
            if(stopOnDirectionChange || dirChangeWhileMoving || (getSpeed(whichThrottle) == 0))
                isAllowed = true;
        }
        return isAllowed;
    }

    void set_stop_button(int whichThrottle, boolean pressed) {
        if (pressed) {
            bStops[whichThrottle].setPressed(true);
            bStops[whichThrottle].setTypeface(null, Typeface.ITALIC);
        } else {
            bStops[whichThrottle].setPressed(false);
            bStops[whichThrottle].setTypeface(null, Typeface.NORMAL);
        }
    }

    private boolean isSelectLocoAllowed(int whichThrottle) {
        // check whether loco change is permitted
        boolean isAllowed = false;
        if (!getConsist(whichThrottle).isActive() || locoSelectWhileMoving || (getSpeed(whichThrottle) == 0)) {
            isAllowed = true;
        }
        return isAllowed;
    }

    void start_select_loco_activity(int whichThrottle) {
        // give feedback that select button was pressed
        bSels[whichThrottle].setPressed(true);
        try {
            Intent select_loco = new Intent().setClass(this, select_loco.class);
            select_loco.putExtra("sWhichThrottle", mainapp.throttleIntToString(whichThrottle));  // pass whichThrottle as an extra to activity
            navigatingAway = true;
            startActivityForResult(select_loco, ACTIVITY_SELECT_LOCO);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        } catch (Exception ex) {
            Log.d("Engine_Driver", ex.getMessage());
        }
    }

    void start_gamepad_test_activity(int gamepadNo) {

        if (prefGamepadTestEnforceTesting) {
            gamePadDeviceIdsTested[gamepadNo] = 0;
            try {
                Intent in = new Intent().setClass(this, gamepad_test.class);
                in.putExtra("whichGamepadNo", Integer.toString(gamepadNo));
                navigatingAway = true;
                speakWords(TTS_MSG_GAMEPAD_GAMEPAD_TEST,' ');
                startActivityForResult(in, ACTIVITY_GAMEPAD_TEST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d("Engine_Driver", ex.getMessage());
            }
        } else { // don't bother doing the test if the preference is set not to
            gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_GOOD;
        }
    }

    // Edit the Consist Lights
//    void start_consist_lights_edit(char whichThrottle) {
    void start_consist_lights_edit(int whichThrottle) {
        if (prefConsistLightsLongClick) {  // only allow the editing in the consist lights if the preference is set
            try {
                Intent consistLightsEdit = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle) ) ;
                navigatingAway = true;
                startActivityForResult(consistLightsEdit, throttle.ACTIVITY_CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d("Engine_Driver", ex.getMessage());
            }
        }
    }

    void disable_buttons(int whichThrottle) {
        enable_disable_buttons(whichThrottle, true);
    }

    void enable_disable_buttons(int whichThrottle) {
        enable_disable_buttons(whichThrottle, false);
    }

    void applySpeedRelatedOptions() {
          for (int throttleIndex = 0;throttleIndex < mainapp.numThrottles; throttleIndex++) {
              applySpeedRelatedOptions(throttleIndex);}  // repeat for all three throttles
    }

    void applySpeedRelatedOptions(int whichThrottle) {

        Button bFwd = bFwds[whichThrottle];
        Button bRev = bRevs[whichThrottle];
        Button bSel = bSels[whichThrottle];
        boolean tIsEnabled = lls[whichThrottle].isEnabled();
        int dir = dirs[whichThrottle];

        if (getConsist(whichThrottle).isActive()) {
            boolean dirChangeAllowed = tIsEnabled && isChangeDirectionAllowed(whichThrottle);
            boolean locoChangeAllowed = tIsEnabled && isSelectLocoAllowed(whichThrottle);
            if (dirChangeAllowed) {
                bFwd.setEnabled(true);
                bRev.setEnabled(true);
            } else {
                if (dir == 1) {
                    if (!directionButtonsAreCurrentlyReversed(whichThrottle)) {
                        bFwd.setEnabled(true);
                        bRev.setEnabled(false);
                    } else {
                        bFwd.setEnabled(false);
                        bRev.setEnabled(true);
                    }
                } else {
                    if (!directionButtonsAreCurrentlyReversed(whichThrottle)) {
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

    void enable_disable_buttons(int whichThrottle, boolean forceDisable) {
        boolean newEnabledState = false;
        if (whichThrottle >= mainapp.consists.length) { // avoid index crash
            return;
        }
        if (!forceDisable) { // avoid index crash, but may simply push to next line
            newEnabledState = mainapp.consists[whichThrottle].isActive(); // set false if lead loco is not assigned
        }
        bFwds[whichThrottle].setEnabled(newEnabledState);
        bRevs[whichThrottle].setEnabled(newEnabledState);
        bStops[whichThrottle].setEnabled(newEnabledState);
        tvSpdLabs[whichThrottle].setEnabled(newEnabledState);
        tvSpdVals[whichThrottle].setEnabled(newEnabledState);
        bLSpds[whichThrottle].setEnabled(newEnabledState);
        bRSpds[whichThrottle].setEnabled(newEnabledState);
        enable_disable_buttons_for_view(fbs[whichThrottle], newEnabledState);
        if (!newEnabledState) {
            sbs[whichThrottle].setProgress(0); // set slider to 0 if disabled
        }
        sbs[whichThrottle].setEnabled(newEnabledState);
    } // end of enable_disable_buttons

    // helper function to enable/disable all children for a group
    void enable_disable_buttons_for_view(ViewGroup vg, boolean newEnabledState) {
        // Log.d("Engine_Driver","starting enable_disable_buttons_for_view " +

        // implemented in derived class, but called from this class

    }

    // update the appearance of all function buttons
    void set_all_function_states(int whichThrottle) {
        // Log.d("Engine_Driver","set_function_states");

        // implemented in derived class, but called from this class

    }

    // update a function button appearance based on its state
    void set_function_state(int whichThrottle, int function) {
        // Log.d("Engine_Driver","starting set_function_request");

        // implemented in derived class, but called from this class

    }

    /*
     * future use: displays the requested function state independent of (in addition to) feedback state 
     * todo: need to handle momentary buttons somehow
     */
    void set_function_request(int whichThrottle, int function, int reqState) {
        // Log.d("Engine_Driver","starting set_function_request");
        Button b;
        b = functionMaps[whichThrottle].get(function);
        if (b != null) {
            if (reqState != 0) {
                b.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
            } else {
                b.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void clearVolumeAndGamepadAdditionalIndicators() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                if ((prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_NONE)) || (prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_GAMEPAD))) {
                    bSels[throttleIndex].setActivated(false);
                }
                if ((prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_NONE)) || (prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_VOLUME))) {
                    bSels[throttleIndex].setHovered(false);
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void setSelectedLocoAdditionalIndicator(int whichThrottle, boolean isVolume) {

        if (!prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_NONE)) {

            if (mainapp.androidVersion >= mainapp.minActivatedButtonsVersion) {

                if ((isVolume) && (((prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_BOTH)) || (prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_VOLUME))))) { // note: 'Volume' option is no longer available
                    if (!prefDisableVolumeKeys) { // don't set the indicators if the volume keys are disabled the preferences
                        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                            if (whichThrottle == throttleIndex) {
                                bSels[throttleIndex].setActivated(true);
                            } else {
                                bSels[throttleIndex].setActivated(false);
                            }
                        }
                    }
                }
                if ((!isVolume) && (((prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_BOTH)) || (prefSelectedLocoIndicator.equals(SELECTED_LOCO_INDICATOR_GAMEPAD))))) {
                    for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                        if (whichThrottle == throttleIndex) {
                            bSels[throttleIndex].setHovered(true);
                        } else {
                            bSels[throttleIndex].setHovered(false);
                        }
                    }
                }
            }
        }
    }

    // For TTS
    private void speakWords(int msgNo, int whichThrottle) {
        boolean result = false;
        String speech = "";
        if (!prefTtsWhen.equals(PREF_TT_WHEN_NONE)) {
            if (myTts != null) {
                switch (msgNo) {
                    case TTS_MSG_VOLUME_THROTTLE:
                        if (!prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_NONE)) {
                            if (whichLastVolume != whichThrottle) {
                                result = true;
                                whichLastVolume = whichThrottle;
                                speech = getApplicationContext().getResources().getString(R.string.TtsVolumeThrottle) + " " + (whichThrottle + 1);
                            }
                            if ((prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_LOCO)) || (prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + getApplicationContext().getResources().getString(R.string.TtsLoco) + " " + (getConsistAddressString(whichThrottle));
                            }
                            if ((prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_SPEED)) || (prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + getApplicationContext().getResources().getString(R.string.TtsSpeed) + " " + (getScaleSpeed(whichThrottle) + 1);
                            }
                        }
                        break;
                    case TTS_MSG_GAMEPAD_THROTTLE:
                        if (!prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_NONE)) {
                            if (whichLastGamepad1 != whichThrottle) {
                                result = true;
                                whichLastGamepad1 = whichThrottle;
                                speech = getApplicationContext().getResources().getString(R.string.TtsGamepadThrottle) + " " + (whichThrottle + 1);
                            }
                            if ((prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_LOCO)) || (prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + getApplicationContext().getResources().getString(R.string.TtsLoco) + " " + (getConsistAddressString(whichThrottle));
                            }
                            if ((prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_SPEED)) || (prefTtsThrottleResponse.equals(PREF_TTS_THROTTLE_RESPONSE_LOCO_SPEED))) {
                                speech = speech  + ", " + getApplicationContext().getResources().getString(R.string.TtsSpeed) + " " + (getScaleSpeed(whichThrottle) + 1);
                            }
                        }
                        break;
                    case TTS_MSG_GAMEPAD_GAMEPAD_TEST:
                        if ((prefTtsGamepadTest)) {
                            result = true;
                            speech = getApplicationContext().getResources().getString(R.string.TtsGamepadTest);
                        }
                        break;
                    case TTS_MSG_GAMEPAD_GAMEPAD_TEST_COMPLETE:
                        if ((prefTtsGamepadTestComplete)) {
                            result = true;
                            speech = getApplicationContext().getResources().getString(R.string.TtsGamepadTestComplete);
                        }
                        break;
                    case TTS_MSG_GAMEPAD_GAMEPAD_TEST_SKIPPED:
                        if ((prefTtsGamepadTestComplete)) {
                            result = true;
                            speech = getApplicationContext().getResources().getString(R.string.TtsGamepadTestSkipped);
                        }
                        break;
                    case TTS_MSG_GAMEPAD_GAMEPAD_TEST_FAIL:
                        if ((prefTtsGamepadTestComplete)) {
                            result = true;
                            speech = getApplicationContext().getResources().getString(R.string.TtsGamepadTestFail);
                        }
                        break;
                    case TTS_MSG_GAMEPAD_GAMEPAD_TEST_RESET:
                        if ((prefTtsGamepadTestComplete)) {
                            result = true;
                            speech = getApplicationContext().getResources().getString(R.string.TtsGamepadTestReset);
                        }
                        break;
                }

                if (result) {
                    Time currentTime = new Time();
                    currentTime.setToNow();
                    // //don't repeat what was last spoken withing 6 seconds
                    if (((currentTime.toMillis(true) >= (lastTtsTime.toMillis(true) + 6000)) || (!speech.equals(lastTts)))) {
                        //myTts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
                        myTts.speak(speech, TextToSpeech.QUEUE_ADD, null);
                        lastTtsTime = currentTime;
                    }
                    lastTts = speech;
                }
            }
        }
    }

    // For TTS
    private void setupTts() {
        if (!prefTtsWhen.equals(getApplicationContext().getResources().getString(R.string.prefTtsWhenDefaultValue))) {
            if (myTts == null) {
                Intent checkTTSIntent = new Intent();
//                checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
//                startActivityForResult(checkTTSIntent, MY_TTS_DATA_CHECK_CODE);
                lastTtsTime = new Time();
                lastTtsTime.setToNow();

                myTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            myTts.setLanguage(Locale.getDefault());
                        } else {
                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastTtsFailed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    // Set the original volume indicator a small 'v' near the speed
    protected void setVolumeIndicator() {
        // hide or display volume control indicator based on variable
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            tvVols[throttleIndex].setText("");
            if (!prefDisableVolumeKeys) { // don't set the indicators if the volume keys are disabled
                tvVols[whichVolume].setText(VOLUME_INDICATOR);
                setSelectedLocoAdditionalIndicator(whichVolume, true);
            }
        }
        // Ensure ESU MCII tracks selected throttle
        if (IS_ESU_MCII) {
            Log.d("Engine_Driver", "ESU_MCII: Throttle changed to: " + whichVolume);
            setEsuThrottleKnobPosition(whichVolume, getSpeed(whichVolume));
            if (!isEsuMc2Stopped) {
                // Set green LED on if controlling a throttle; flash if nothing selected
                // Only do this if ESU MCII not in Stop mode
                if (getConsist(whichVolume).isActive()) {
                    esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.ON, true);
                } else {
                    esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.STEADY_FLASH, true);
                }
            }
        }
    }

    protected void setGamepadIndicator() {
        // hide or display gamepad number indicator based on variable
        boolean isSet = false;
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            if (gamePadThrottleAssignment[throttleIndex]!=-1) {
                tvGamePads[throttleIndex].setText(String.valueOf(gamePadThrottleAssignment[throttleIndex]));
            } else {
                tvGamePads[throttleIndex].setText("");
            }

            if (gamePadThrottleAssignment[throttleIndex] == 1) {
                setSelectedLocoAdditionalIndicator(throttleIndex, false);
                speakWords(TTS_MSG_GAMEPAD_THROTTLE, throttleIndex);
                isSet = true;
            }
        }
        if (!isSet) {
            setSelectedLocoAdditionalIndicator(-1,false);
        }
    }

    private void setNextActiveThrottle() {
        setNextActiveThrottle(false);
    }

    private void setNextActiveThrottle(boolean feedbackSound) {
        int i;
        int index = -1;

        // find current Volume throttle
        for (i = 0; i < mainapp.numThrottles ; i++) {
            if (i == whichVolume) {
                index = i;
                break;
            }
        }
        // find next active throttle
        i = index+1;
        while (i != index) {                        // check until we get back to current Volume throttle
            if (i >= mainapp.numThrottles) {                // wrap
                i = 0;
            } else {
                int whichT = i;
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

    // if the preference is set, set the specific active throttle for the volume keys based on the throttle used
    private void setActiveThrottle(int whichThrottle) {
        if (tvVols[0]!=null) { // if it is null assume that the page is not fully drawn yet
            if (prefVolumeKeysFollowLastTouchedThrottle) {
                if (whichVolume != whichThrottle) {
                    whichVolume = whichThrottle;
                    setVolumeIndicator();
                    setSelectedLocoAdditionalIndicator(whichThrottle, true);
                }
            }
        }
    }

        // setup the appropriate keycodes for the type of gamepad that has been selected in the preferences
    private void setGamepadKeys() {
        whichGamePadMode = prefs.getString("prefGamePadType", getApplicationContext().getResources().getString(R.string.prefGamePadTypeDefaultValue));

        // Gamepad button Preferences
        prefGamePadButtons[0] = prefs.getString("prefGamePadButtonStart", getApplicationContext().getResources().getString(R.string.prefGamePadButtonStartDefaultValue));
        prefGamePadButtons[9] = prefs.getString("prefGamePadButtonReturn", getApplicationContext().getResources().getString(R.string.prefGamePadButtonReturnDefaultValue));
        prefGamePadButtons[1] = prefs.getString("prefGamePadButton1", getApplicationContext().getResources().getString(R.string.prefGamePadButton1DefaultValue));
        prefGamePadButtons[2] = prefs.getString("prefGamePadButton2", getApplicationContext().getResources().getString(R.string.prefGamePadButton2DefaultValue));
        prefGamePadButtons[3] = prefs.getString("prefGamePadButton3", getApplicationContext().getResources().getString(R.string.prefGamePadButton3DefaultValue));
        prefGamePadButtons[4] = prefs.getString("prefGamePadButton4", getApplicationContext().getResources().getString(R.string.prefGamePadButton4DefaultValue));
        // Gamepad DPAD Preferences
        prefGamePadButtons[5] = prefs.getString("prefGamePadButtonUp", getApplicationContext().getResources().getString(R.string.prefGamePadButtonUpDefaultValue));
        prefGamePadButtons[6] = prefs.getString("prefGamePadButtonRight", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightDefaultValue));
        prefGamePadButtons[7] = prefs.getString("prefGamePadButtonDown", getApplicationContext().getResources().getString(R.string.prefGamePadButtonDownDefaultValue));
        prefGamePadButtons[8] = prefs.getString("prefGamePadButtonLeft", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftDefaultValue));

        if (!whichGamePadMode.equals(WHICH_GAMEPAD_MODE_NONE)) {
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
/*
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
*/
            case "MagicseeR1B":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMagicseeR1B);
                bGamePadKeysUp = bGamePadKeys;
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
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            index++;
            if (index >= mainapp.numThrottles) {
                index = 0;
            }
            if (gamePadIds[index] == 0) {  // unassigned
                if (getConsist(index).isActive()) { // found next active throttle
                    if (gamePadIds[index] <= 0) { //not currently assigned
                        whichThrottle = index;
                        break;  // done
                    }
                }
            }
        }

        if (whichThrottle>=0) {
            gamePadIds[whichThrottle] = gamePadIds[fromThrottle];
//            gamePadDeviceIdsTested[whichThrottle] = gamePadDeviceIdsTested[fromThrottle];
            gamePadThrottleAssignment[whichThrottle] = gamePadThrottleAssignment[fromThrottle];
            gamePadIds[fromThrottle] = 0;
//            gamePadDeviceIdsTested[fromThrottle] = -1;
            gamePadThrottleAssignment[fromThrottle] = -1;
            setGamepadIndicator();
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
    private int findWhichGamePadEventIsFrom(int eventDeviceId, int eventKeyCode) {
        int whichGamePad = -2;  // default to the event not from a gamepad
        int whichGamePadDeviceId = -1;
        int j;

//        if ((eventKeyCode!=KEYCODE_VOLUME_UP)&&(eventKeyCode!=KEYCODE_VOLUME_DOWN)&&(eventKeyCode!=KEYCODE_BACK)) { // if it is a volume key or the back assume it did not come form a gamepad
        if (eventDeviceId >= 0) { // event is from a gamepad (or at least not from a screen touch)
            whichGamePad = -1;  // gamepad

            int reassigningGamepad = -1;
            int i;
            // find out if this gamepad is already assigned
            for (i = 0; i < mainapp.numThrottles; i++) {
                if (gamePadIds[i] == eventDeviceId) {
                    if (getConsist(i).isActive()) { //found the throttle and it is active
                        whichGamePad = i;
                    } else { // currently assigned to this throttle, but the throttle is not active
                        whichGamePad = i;
                        gamePadIds[i] = 0;
                        reassigningGamepad = gamePadThrottleAssignment[i];
                        gamePadThrottleAssignment[i] = -1;
                        setGamepadIndicator(); // need to clear the indicator
                    }
                    break;
                }
            }

            if (whichGamePad == -1) { //didn't find it OR is known, but unassigned

                for (j = 0; j < gamepadCount; j++) {
                    if (gamePadDeviceIds[j] == eventDeviceId) { // known, but unassigned
                        whichGamePadDeviceId = j;
                        break;
                    }
                }
                if (whichGamePadDeviceId == -1) { // previously unseen gamepad
                    gamepadCount++;
                    gamePadDeviceIds[gamepadCount - 1] = eventDeviceId;
                    whichGamePadDeviceId = gamepadCount - 1;

                    mainapp.setGamepadTestMenuOption(TMenu,gamepadCount);

                    start_gamepad_test_activity(gamepadCount - 1);

                }

                for (i = 0; i < mainapp.numThrottles; i++) {
                    if (gamePadIds[i] == 0) {  // throttle is not assigned a gamepad
                        if (getConsist(i).isActive()) { // found next active throttle
                            gamePadIds[i] = eventDeviceId;
                            if (reassigningGamepad==-1) { // not a reassignment
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
            } else {
                if (gamePadDeviceIdsTested[i]==GAMEPAD_BAD){  // gamepad is known but failed the test last time
                    start_gamepad_test_activity(i);
                }
            }
        }
        if (gamepadCount > 0) {
            usingMultiplePads = true;
        }

//        }
        return whichGamePad;
    }

    // map the button pressed to the user selected action for that button on the gamepad
    private void performButtonAction(int buttonNo, int action, boolean isActive, int whichThrottle, int whichGamePadIsEventFrom, int repeatCnt) {


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
                    swapToNextAvilableThrottleForGamePad(whichGamePadIsEventFrom, false);
                } else {
                    setNextActiveThrottle(true);
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_FORWARD)) {  // Forward
            if (isActive && (action==ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed;
                if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                    dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                } else {
                    dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                }
                GamepadFeedbackSound(dirChangeFailed);
            }
        } else if (prefGamePadButtons[buttonNo].equals(PREF_GAMEPAD_BUTTON_OPTION_REVERSE)) {  // Reverse
            boolean dirChangeFailed;
            if (isActive && (action==ACTION_DOWN) && (repeatCnt == 0)) {
                if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                    dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                } else {
                    dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                }
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
        } else if ((prefGamePadButtons[buttonNo].length()>=11) && (prefGamePadButtons[buttonNo].substring(0,9).equals(GAMEPAD_FUNCTION_PREFIX))) { // one of the Function Buttons
            int fKey = Integer.parseInt(prefGamePadButtons[buttonNo].substring(9,11));
            if (isActive && (repeatCnt == 0)) {
                if (action==ACTION_DOWN) {
                    GamepadFeedbackSound(false);
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle), fKey, 1);
                } else {
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle), fKey, 0);
                }
            }

        }
    }

    private int getGamePadIndexFromThrottleNo (int whichThrottle) {
        int whichGamepad = -1;
        for (int i = 0; i < mainapp.numThrottles; i++) {
            if (gamePadIds[whichThrottle] == gamePadDeviceIds[i]) {
                whichGamepad = i;
                break;
            }
        }
        return whichGamepad;
    }

    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        //Log.d("Engine_Driver", "dgme " + event.getAction());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            if (!whichGamePadMode.equals(WHICH_GAMEPAD_MODE_NONE)) { // respond to the gamepad and keyboard inputs only if the preference is set

                boolean acceptEvent = true; // default to assuming that we will respond to the event

                int action;
                int whichThrottle;
                int repeatCnt = 0;
                int whichGamePadIsEventFrom = findWhichGamePadEventIsFrom(event.getDeviceId(), 0); // dummy eventKeyCode

                if (whichGamePadIsEventFrom > -1) { // the event came for a gamepad
                    if (gamePadDeviceIdsTested[getGamePadIndexFromThrottleNo(whichGamePadIsEventFrom)]!=GAMEPAD_GOOD) { //if not, testing for this gamepad is not complete or has failed
                        acceptEvent = false;
                    }
                } else {
                    acceptEvent = false;
                }

                if (acceptEvent) {
                    float xAxis;
                    xAxis = event.getAxisValue(MotionEvent.AXIS_X);
                    float yAxis = event.getAxisValue(MotionEvent.AXIS_Y);

                    if ((xAxis != 0) || (yAxis != 0)) {
                        action = ACTION_DOWN;
                    } else {
                        action = ACTION_UP;
                    }
                    if ((usingMultiplePads) && (whichGamePadIsEventFrom >= -1)) { // we have multiple gamepads AND the preference is set to make use of them AND the event came for a gamepad
                        if (whichGamePadIsEventFrom >= 0) {
                            whichThrottle = whichGamePadIsEventFrom;
                        } else {
                            GamepadFeedbackSound(true);
                            return (true);
                        }
                    } else {
                        whichThrottle = whichVolume;  // work out which throttle the volume keys are currently set to contol... and use that one
                    }

                    boolean isActive = getConsist(whichThrottle).isActive();

                    if (action == ACTION_UP) {
                        mGamepadAutoIncrement = false;
                        mGamepadAutoDecrement = false;
                        GamepadFeedbackSoundStop();
                    }

                    if (yAxis == -1) { // DPAD Up Button
                        performButtonAction(5, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                        return (true); // stop processing this key

                    } else if (yAxis == 1) { // DPAD Down Button
                        performButtonAction(7, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                        return (true); // stop processing this key

                    } else if (xAxis == -1) { // DPAD Left Button
                        performButtonAction(8, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                        return (true); // stop processing this key

                    } else if (xAxis == 1) { // DPAD Right Button
                        performButtonAction(6, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                        return (true); // stop processing this key
                    }
                } else { // event is from a gamepad that has not finished testing. Ignore it
                    return (true); // stop processing this key
                }
            }
        }
        return super.dispatchGenericMotionEvent(event);
    }

    // listener for physical keyboard events
    // used to support the gamepad only   DPAD and key events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        boolean isExternal = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            InputDevice idev = getDevice(event.getDeviceId());
            if( idev != null && idev.toString() != null
                    && idev.toString().contains("Location: external")) isExternal = true;
        }

        if (isExternal) { // if has come from the phone itself, don't try to process it here
            if (!whichGamePadMode.equals(WHICH_GAMEPAD_MODE_NONE)) { // respond to the gamepad and keyboard inputs only if the preference is set

                boolean acceptEvent = true; // default to assuming that we will respond to the event

                int action = event.getAction();
                int keyCode = event.getKeyCode();
                int repeatCnt = event.getRepeatCount();
                int whichThrottle;
                int whichGamePadIsEventFrom = findWhichGamePadEventIsFrom(event.getDeviceId(), event.getKeyCode());

                if (whichGamePadIsEventFrom > -1) { // the event came for a gamepad
                    if (gamePadDeviceIdsTested[getGamePadIndexFromThrottleNo(whichGamePadIsEventFrom)]!=GAMEPAD_GOOD) { //if not, testing for this gamepad is not complete or has failed
                        acceptEvent = false;
                    }
                } else {
                    acceptEvent = false;
                }

                if (acceptEvent) {
                    if ((usingMultiplePads) && (whichGamePadIsEventFrom >= -1)) { // we have multiple gamepads AND the preference is set to make use of them AND the event came for a gamepad
                        if (whichGamePadIsEventFrom >= 0) {
                            whichThrottle = whichGamePadIsEventFrom;
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

                    } else if (keyCode == gamePadKeys[1]) { // Return button
                        // NextThrottle
                    /* if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                        if (usingMultiplePads && whichGamePadIsEventFrom >= 0) {
                            whichGamePadIsEventFrom = swapToNextAvilableThrottleForGamePad(whichGamePadIsEventFrom, false);
                        } else {
                            setNextActiveThrottle(true);
                        }
                    } */
                        performButtonAction(9, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                        return (true); // stop processing this key
                    }
                } else { // event is from a gamepad that has not finished testing. Ignore it
                    return (true); // stop processing this key
                }
//  for now pass all keystrokes not in gamePadKeys[] to super
//  if problems occur, we can uncomment the next 2 lines
//            else if (!((keyCode == KEYCODE_BACK) || (keyCode == KEYCODE_VOLUME_DOWN) || (keyCode == KEYCODE_VOLUME_UP) || (keyCode == KEYCODE_MENU)))
//            return (true); // swallow all other keystrokes except back, menu and the volume keys
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
        }
        return super.dispatchKeyEvent(event);
    }

    void GamepadIncrementSpeed(int whichThrottle) {
        incrementSpeed(whichThrottle, SPEED_COMMAND_FROM_GAMEPAD);
        GamepadFeedbackSound(atMaxSpeed(whichThrottle));
    }

    void GamepadDecrementSpeed(int whichThrottle) {
        decrementSpeed(whichThrottle, SPEED_COMMAND_FROM_GAMEPAD);
        GamepadFeedbackSound(atMinSpeed(whichThrottle));
    }

    void GamepadFeedbackSound(boolean invalidAction) {
        if (mainapp.appIsFinishing) { return;}

        if (tg != null) {
            if (invalidAction)
                tg.startTone(ToneGenerator.TONE_PROP_NACK);
            else
                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
    }

    void GamepadFeedbackSoundStop() {
        if (tg != null) {
            tg.stopTone();
        }
    }

    // For gamepad speed buttons.
    private class GamepadRptUpdater implements Runnable {
        int whichThrottle;

        private GamepadRptUpdater(int WhichThrottle) {
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
            if (mainapp.appIsFinishing) { return;}

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
        int whichThrottle;

        private volumeKeysRptUpdater(int WhichThrottle) {
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
                incrementSpeed(whichThrottle, SPEED_COMMAND_FROM_VOLUME);
                volumeKeysRepeatUpdateHandler.postDelayed(new volumeKeysRptUpdater(whichThrottle), REP_DELAY);
            } else if (mVolumeKeysAutoDecrement) {
                decrementSpeed(whichThrottle, SPEED_COMMAND_FROM_VOLUME);
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
//        char whichThrottle;
        int whichThrottle;

//        private EsuMc2ButtonRptUpdater(char whichThrottle) {
        private EsuMc2ButtonRptUpdater(int whichThrottle) {
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
                incrementSpeed(whichThrottle, SPEED_COMMAND_FROM_VOLUME);
                esuButtonRepeatUpdateHandler.postDelayed(new EsuMc2ButtonRptUpdater(whichThrottle), REP_DELAY);
            } else if (esuButtonAutoDecrement) {
                decrementSpeed(whichThrottle, SPEED_COMMAND_FROM_VOLUME);
                gamepadRepeatUpdateHandler.postDelayed(new EsuMc2ButtonRptUpdater(whichThrottle), REP_DELAY);
            }
        }
    }

    // Callback for ESU MCII throttle knob events
    private ThrottleFragment.OnThrottleListener esuOnThrottleListener = new ThrottleFragment.OnThrottleListener() {

        @Override
        public void onButtonDown() {
            Log.d("Engine_Driver", "ESU_MCII: Knob button down for throttle " + whichVolume);
            if (!isScreenLocked) {
                if (prefEsuMc2EndStopDirectionChange) {
                    Log.d("Engine_Driver", "ESU_MCII: Attempting to switch direction");
                    changeDirectionIfAllowed(whichVolume, (getDirection(whichVolume) == 1 ? 0 : 1));
                    speedUpdateAndNotify(whichVolume, 0, false);
                } else {
                    Log.d("Engine_Driver", "ESU_MCII: Direction change option disabled - do nothing");
                    speedUpdateAndNotify(whichVolume, 0, false);
                }
            } else {
                Log.d("Engine_Driver", "ESU_MCII: Screen locked - do nothing");
            }
        }

        @Override
        public void onButtonUp() {
            Log.d("Engine_Driver", "ESU_MCII: Knob button up for throttle " + whichVolume);
        }

        @Override
        public void onPositionChanged(int knobPos) {
            int speed;
            if (!isScreenLocked) {
                if (getConsist(whichVolume).isActive() && !isEsuMc2Stopped) {
                    speed = esuThrottleScales[whichVolume].positionToStep(knobPos);
                    Log.d("Engine_Driver", "ESU_MCII: Knob position changed for throttle " + whichVolume);
                    Log.d("Engine_Driver", "ESU_MCII: New knob position: " + knobPos + " ; speedstep: " + speed);
                    speedUpdateAndNotify(whichVolume, speed, false); // No need to move knob
                } else {
                    // Ignore knob movements for stopped or inactive throttles
                    Log.d("Engine_Driver", "ESU_MCII: Knob position moved for " + (isEsuMc2Stopped ? "stopped" : "inactive") + " throttle " + whichVolume);
                    Log.d("Engine_Driver", "ESU_MCII: Nothing updated");
                }
            } else {
                Log.d("Engine_Driver", "ESU_MCII: Screen locked - do nothing");
            }
        }
    };

    // Callback for ESU MCII stop button
    private StopButtonFragment.OnStopButtonListener esuOnStopButtonListener = new StopButtonFragment.OnStopButtonListener() {
        private int origSpeed;
        private long timePressed;
        private boolean wasLongPress = false;
        private int delay;
        private CountDownTimer buttonTimer;

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
            if (prefEsuMc2StopButtonShortPress) {
                set_stop_button(whichVolume, true);
                speedUpdateAndNotify(whichVolume, 0);
            }
            esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.ON);
            esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
            // Read current stop button delay pref value
            delay = preferences.getIntPrefValue(prefs,"prefEsuMc2StopButtonDelay",
                    getApplicationContext().getResources().getString(R.string.prefEsuMc2StopButtonDelayDefaultValue));
            buttonTimer = new CountDownTimer(delay, delay) {
                @Override
                public void onTick(long l) {
                    // do nothing...
                }

                @Override
                public void onFinish() {
                    doStopButtonUp(true);
                }
            };
            buttonTimer.start();
        }

        @Override
        public void onStopButtonUp() {
            if (buttonTimer != null) {
                buttonTimer.cancel();
            }
            doStopButtonUp(false);
        }

        private void doStopButtonUp(boolean fromTimer) {
            if (buttonTimer != null) {
                buttonTimer.cancel();
            }
            if (!getConsist(whichVolume).isActive()) {
                Log.d("Engine_Driver", "ESU_MCII: Stop button up for inactive throttle " + whichVolume);
                return;
            }
            if (fromTimer) {
                Log.d("Engine_Driver", "ESU_MCII: Stop button timer finished for throttle " + whichVolume);
            } else {
                Log.d("Engine_Driver", "ESU_MCII: Stop button up for throttle " + whichVolume);
            }
            if (isEsuMc2Stopped) {
                if (fromTimer || System.currentTimeMillis() - timePressed > delay) {
                    // It's a long initial press so record this
                    wasLongPress = true;
                    Log.d("Engine_Driver", "ESU_MCII: Stop button press was long - long flash Red LED");
                    esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.LONG_FLASH);
                    esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
                    // Set all throttles to zero
                    for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                        set_stop_button(throttleIndex, true);
                        speedUpdateAndNotify(throttleIndex, 0);
                        setEnabledEsuMc2ThrottleScreenButtons(throttleIndex, false);
                    }
                    isEsuMc2AllStopped = true;
                } else {
                    wasLongPress = false;
                    if (prefEsuMc2StopButtonShortPress) {
                        Log.d("Engine_Driver", "ESU_MCII: Stop button press was short - short flash Red LED");
                        esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.QUICK_FLASH);
                        esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
                        setEnabledEsuMc2ThrottleScreenButtons(whichVolume, false);
                    } else {
                        Log.d("Engine_Driver", "ESU_MCII: Stop button press was short but action disabled");
                        isEsuMc2Stopped = !isEsuMc2Stopped;
                        esuMc2Led.revertLEDStates();
                    }
                }
            } else {
                if (!wasLongPress) {
                    if (prefEsuMc2StopButtonShortPress) {
                        Log.d("Engine_Driver", "ESU_MCII: Revert speed value to: " + origSpeed);
                        set_stop_button(whichVolume, false);
                        speedUpdateAndNotify(whichVolume, origSpeed);
                        setEnabledEsuMc2ThrottleScreenButtons(whichVolume, true);
                    } else {
                        Log.d("Engine_Driver", "ESU_MCII: Stop button press was short but revert action disabled");
                    }
                } else {
                    Log.d("Engine_Driver", "ESU_MCII: Resume control without speed revert");
                    origSpeed = 0;
                    for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                        set_stop_button(throttleIndex, false);
                        setEnabledEsuMc2ThrottleScreenButtons(throttleIndex, true);
                    }
                    isEsuMc2AllStopped = false;
                }
                // Revert LED states
                esuMc2Led.revertLEDStates();
            }
        }
    };

    // Function to move ESU MCII control knob
//    private void setEsuThrottleKnobPosition(char whichThrottle, int speed) {
    private void setEsuThrottleKnobPosition(int whichThrottle, int speed) {
        Log.d("Engine_Driver", "ESU_MCII: Request to update knob position for throttle " + whichThrottle);
        if (whichThrottle == whichVolume) {
            int knobPos;
            knobPos = esuThrottleScales[whichThrottle].stepToPosition(speed);
            Log.d("Engine_Driver", "ESU_MCII: Update knob position for throttle " + mainapp.throttleIntToString(whichThrottle));
            Log.d("Engine_Driver", "ESU_MCII: New knob position: " + knobPos + " ; speedstep: " + speed);
            esuThrottleFragment.moveThrottle(knobPos);
        } else {
            Log.d("Engine_Driver", "ESU_MCII: This throttle not selected for control by knob");
        }
    }

    private void updateEsuMc2ZeroTrim() {
        int zeroTrim = preferences.getIntPrefValue(prefs,"prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
        Log.d("Engine_Driver", "ESU_MCII: Update zero trim for throttle to: " + zeroTrim);

        // first the knob
        esuThrottleFragment.setZeroPosition(zeroTrim);

        // now throttle scales
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            esuThrottleScales[throttleIndex] = new ThrottleScale(zeroTrim, esuThrottleScales[throttleIndex].getStepCount());
        }
    }

//    private void performEsuMc2ButtonAction(int buttonNo, int action, boolean isActive, char whichThrottle, int repeatCnt) {
    private void performEsuMc2ButtonAction(int buttonNo, int action, boolean isActive, int whichThrottle, int repeatCnt) {

        if (isEsuMc2Stopped) {
            Log.d("Engine_Driver", "ESU_MCII: Device button presses whilst stopped ignored");
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastEsuMc2NoButtonPresses), Toast.LENGTH_SHORT).show();
            return;
        }

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
                        speedUpdateAndNotify(0);    // update all three throttles
                    }
                    break;
                case STOP:
                    if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                        speedUpdateAndNotify(whichThrottle, 0);
                    }
                    break;
                case DIRECTION_FORWARD:
                    if (!isScreenLocked && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                        changeDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                    }
                    break;
                case DIRECTION_REVERSE:
                    if (!isScreenLocked && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                        changeDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                    }
                    break;
                case DIRECTION_TOGGLE:
                    if (!isScreenLocked && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                        if ((getDirection(whichThrottle) == DIRECTION_FORWARD)) {
                            changeDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                        } else {
                            changeDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                        }
                    }
                    break;
                case SPEED_INCREASE:
                    if (!isScreenLocked && action == ACTION_DOWN) {
                        if (repeatCnt == 0) {
                            esuButtonAutoIncrement = true;
                            esuButtonRepeatUpdateHandler.post(new EsuMc2ButtonRptUpdater(whichThrottle));
                        }
                    }
                        break;
                case SPEED_DECREASE:
                    if (!isScreenLocked && action == ACTION_DOWN) {
                        if (repeatCnt == 0) {
                            esuButtonAutoDecrement = true;
                            esuButtonRepeatUpdateHandler.post(new EsuMc2ButtonRptUpdater(whichThrottle));
                        }
                    }
                    break;
                case NEXT_THROTTLE:
                    if (!isScreenLocked && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                        setNextActiveThrottle();
                    }
                    break;
                case FN00: case FN01: case FN02: case FN03: case FN04: case FN05:
                case FN06: case FN07: case FN08: case FN09: case FN10: case FN11:
                case FN12: case FN13: case FN14: case FN15: case FN16: case FN17:
                case FN18: case FN19: case FN20: case FN21: case FN22: case FN23:
                case FN24: case FN25: case FN26: case FN27: case FN28:
                    if (!isScreenLocked && repeatCnt == 0) {
                        if (action == ACTION_DOWN) {
                            mainapp.sendMsg(mainapp.comm_msg_handler,
                                    message_type.FUNCTION,
                                    mainapp.throttleIntToString(whichThrottle),
                                    buttonAction.getFunction(),
                                    1);
                        } else {
                            mainapp.sendMsg(mainapp.comm_msg_handler,
                                    message_type.FUNCTION,
                                    mainapp.throttleIntToString(whichThrottle),
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

//    private void setEnabledEsuMc2ThrottleScreenButtons(char whichThrottle, boolean enabled) {
    private void setEnabledEsuMc2ThrottleScreenButtons(int whichThrottle, boolean enabled) {

        // Disables/enables seekbar and speed buttons
        bLSpds[whichThrottle].setEnabled(enabled);
        bRSpds[whichThrottle].setEnabled(enabled);
        sbs[whichThrottle].setEnabled(enabled);
    }

// Listeners for the Select Loco buttons
    protected class select_function_button_touch_listener implements View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {
    int whichThrottle;

    protected select_function_button_touch_listener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @Override
        public void onClick(View v) {
            if (IS_ESU_MCII && isEsuMc2Stopped) {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastEsuMc2NoLocoChange), Toast.LENGTH_SHORT).show();
            } else if (isSelectLocoAllowed(whichThrottle)) {
                // don't loco change while moving if the preference is set
                start_select_loco_activity(whichThrottle); // pass throttle #
                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            } else {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastLocoChangeNotAllowed), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean onLongClick(View v) {
            start_consist_lights_edit(whichThrottle);
            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference

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
    protected class arrow_speed_button_touch_listener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
        int whichThrottle;
        String arrowDirection;

        protected arrow_speed_button_touch_listener(int new_whichThrottle, String new_arrowDirection) {
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

            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
            return false;
        }

        @Override
        public void onClick(View v) {
            if (arrowDirection.equals("right")) {
                mAutoIncrement = false;
                incrementSpeed(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
            } else {
                mAutoDecrement = false;
                decrementSpeed(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
            }
            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoIncrement) {
                mAutoIncrement = false;
            }

            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoDecrement) {
                mAutoDecrement = false;
            }
            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
            return false;
        }
    }

    // Listeners for the direction buttons
    private class direction_button_touch_listener implements View.OnTouchListener {
        int function;
        int whichThrottle;

        private direction_button_touch_listener(int new_function, int new_whichThrottle) {
            function = new_function;    // store these values for this button
            whichThrottle = new_whichThrottle;
        }

        private void doButtonPress() {

            switch (this.function) {
                case direction_button.LEFT:
                    if (!directionButtonsAreCurrentlyReversed(whichThrottle)) {
                        changeDirectionIfAllowed(whichThrottle, 1);
                    } else {
                        changeDirectionIfAllowed(whichThrottle, 0);
                    }
                    break;
                case direction_button.RIGHT: {
                    if (!directionButtonsAreCurrentlyReversed(whichThrottle)) {
                        changeDirectionIfAllowed(whichThrottle, 0);
                    } else {
                        changeDirectionIfAllowed(whichThrottle, 1);
                    }
                    break;
                }
            }
            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
        }

        Runnable run = new Runnable() {
            @Override
            public void run() {
                if (isDirectionButtonLongPress) {


                    if (isChangeDirectionAllowed(whichThrottle)) { // only respond to the long click if it is ok to change directions
                        doButtonPress();  //in case the the direction button long clicked is not the current direction change the direction first

                        if (prefSwapForwardReverseButtonsLongPress) {
                            //v.playSoundEffect(SoundEffectConstants.CLICK);
                            if (currentSwapForwardReverseButtons[whichThrottle]) {
                                currentSwapForwardReverseButtons[whichThrottle] = false;
                            } else {
                                currentSwapForwardReverseButtons[whichThrottle] = true;
                            }
                            setDirectionButtonLabels();
                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastDirectionButtonsSwapped), Toast.LENGTH_SHORT).show();
                        }
                        doButtonPress();
                    }

                    setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
                }
            }
        };

        @Override
        public boolean onTouch(final View v, MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isDirectionButtonLongPress = true;
                directionButtonLongPressHandler.postDelayed(run, prefDirectionButtonLongPressDelay);

                // Log.d("Engine_Driver", "onTouch direction " + function + " action " +
                // event.getAction());

                v.playSoundEffect(SoundEffectConstants.CLICK);  // make the click sound once

                doButtonPress();
                setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference

            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                isDirectionButtonLongPress = false;
                directionButtonLongPressHandler.removeCallbacks(run);
            }
            return true;
        }

    }

    protected class function_button_touch_listener implements View.OnTouchListener {
    int function;
    int whichThrottle;
    boolean leadOnly;       // function only applies to the lead loco
    boolean trailOnly;      // function only applies to the trail loco (future)
    boolean followLeadFunction;       // function only applies to the locos that have been set to follow the function

        protected function_button_touch_listener(int new_function, int new_whichThrottle) {
            this(new_function, new_whichThrottle, "");
        }

        protected function_button_touch_listener(int new_function, int new_whichThrottle, String funcLabel) {
            function = new_function;    // store these values for this button
            whichThrottle = new_whichThrottle;
            String lab = funcLabel.toUpperCase().trim();
            leadOnly = false;
            trailOnly = false;
            followLeadFunction = false;

            boolean selectiveLeadSound = prefs.getBoolean("SelectiveLeadSound", getResources().getBoolean(R.bool.prefSelectiveLeadSoundDefaultValue));
            if (!lab.equals("")) {
                leadOnly = (selectiveLeadSound &&
                        (lab.contains(FUNCTION_BUTTON_LOOK_FOR_WHISTLE) || lab.contains(FUNCTION_BUTTON_LOOK_FOR_HORN) || lab.contains(FUNCTION_BUTTON_LOOK_FOR_BELL))
                        || lab.contains(FUNCTION_BUTTON_LOOK_FOR_HEAD)
                        || (lab.contains(FUNCTION_BUTTON_LOOK_FOR_LIGHT) && !lab.contains(FUNCTION_BUTTON_LOOK_FOR_REAR)));
                followLeadFunction = (lab.contains(FUNCTION_BUTTON_LOOK_FOR_LIGHT));
                trailOnly = lab.contains(FUNCTION_BUTTON_LOOK_FOR_REAR);
            }
            boolean selectiveLeadSoundF1 = prefs.getBoolean("SelectiveLeadSoundF1", getResources().getBoolean(R.bool.prefSelectiveLeadSoundF1DefaultValue));
            boolean selectiveLeadSoundF2 = prefs.getBoolean("SelectiveLeadSoundF2", getResources().getBoolean(R.bool.prefSelectiveLeadSoundF2DefaultValue));
            if ((selectiveLeadSound) && (new_function == 1) && (selectiveLeadSoundF1)) {leadOnly = true;}
            if ((selectiveLeadSound) && (new_function == 2) && (selectiveLeadSoundF2)) {leadOnly = true;}
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
                        case function_button.STOP:
                            set_stop_button(whichThrottle, true);
                            speedUpdateAndNotify(whichThrottle, 0);
                            break;
                        case function_button.SPEED_LABEL:  // specify which throttle the volume button controls
                            if (getConsist(whichThrottle).isActive() && !(IS_ESU_MCII && isEsuMc2Stopped)) { // only assign if Active and, if an ESU MCII not in Stop mode
                                whichVolume = whichThrottle;
                                set_labels();
                            }
                            if (IS_ESU_MCII && isEsuMc2Stopped) {
                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getText(R.string.toastEsuMc2NoThrottleChange), Toast.LENGTH_SHORT).show();
                            }
                            break;

                        default: { // handle the function buttons
                            Consist con;
                            con = mainapp.consists[whichThrottle];

                            String addr = "";
                            if (leadOnly)
                                addr = con.getLeadAddr();
// ***future                else if (trailOnly)
//                              addr = con.getTrailAddr();

                            //mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, whichThrottle + addr, this.function, 1);
                            mainapp.toggleFunction(mainapp.throttleIntToString(whichThrottle) + addr, this.function);
                            // set_function_request(whichThrottle, function, 1);

                            if(followLeadFunction) {
                                for (Consist.ConLoco l : con.getLocos()) {
                                    if (!l.getAddress().equals(con.getLeadAddr())) {  // ignore the lead as we have already set it
                                        if (l.isLightOn() == LIGHT_FOLLOW) {
                                            mainapp.toggleFunction(mainapp.throttleIntToString(whichThrottle) + l.getAddress(), this.function);
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
                    else if (function < direction_button.LEFT) {
                        Consist con = mainapp.consists[whichThrottle];

                        String addr = "";
                        if (leadOnly)
                            addr = con.getLeadAddr();
// ***future            else if (trailOnly)
//                          addr = con.getTrailAddr();
                       mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, 0);
                        // set_function_request(whichThrottle, function, 0);
                    }
                    break;
            }
            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
        }
    }

    //Listeners for the throttle slider
    protected class throttle_listener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichThrottle;
        int lastSpeed;
        boolean limitedJump;
        int jumpSpeed;

        protected throttle_listener(int new_whichThrottle) {
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

                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
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
    public void sendSpeedMsg(int whichThrottle, int speed) {
        // start timer to briefly ignore WiT speed messages - avoids speed "jumping"
        changeTimers[whichThrottle].changeDelay();
        // send speed update to WiT
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.VELOCITY, "", whichThrottle, speed);
    }

    // implement delay for briefly ignoring WiT speed reports after sending a throttle speed update
    // this prevents use of speed reports sent by WiT just prior to WiT processing the speed update
    protected class ChangeDelay {
        boolean delayInProg;
        Runnable changeTimer;
        int whichThrottle;

        protected ChangeDelay(int wThrot) {
            delayInProg = false;
            changeTimer = new ChangeTimer();
            whichThrottle = wThrot;
        }

        private void changeDelay() {
            if (mainapp.throttle_msg_handler == null) return;
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

    // set the title, optionally adding the current time.
    public void setTitle() {
        if (mainapp.displayClock)
            setTitle(getApplicationContext().getResources().getString(R.string.app_name_throttle_short) + "  " + currentTime);
        else
            setTitle(getApplicationContext().getResources().getString(R.string.app_name_throttle));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @SuppressLint({"Recycle", "SetJavaScriptEnabled"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            try {
                // restore the requested throttle direction so we can update the
                // direction indication while we wait for an update from WiT
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    if (savedInstanceState.getSerializable("dir" + mainapp.throttleIntToString(throttleIndex) ) != null)
                        dirs[throttleIndex] = (int) savedInstanceState.getSerializable("dir" + throttleIndex);
                }
            } catch (Exception ignored) {              // log the error, but otherwise keep going.
                Log.d("Engine_Driver", "Restore of saved instance state failed " + android.os.Build.VERSION.SDK_INT);
            }
        }

        mainapp = (threaded_application) this.getApplication();

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        if (mainapp.isForcingFinish()) { // expedite
            mainapp.appIsFinishing = true;
            return;
        }

        mainapp.applyTheme(this);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_throttle)); // needed in case the langauge was changed from the default

//        setContentView(R.layout.throttle);
        setContentView(layoutViewId);

        getCommonPrefs(true); // get all the common preferences
        setThottleNumLimits();

        getDirectionButtonPrefs();

        webViewIsOn = !webViewLocation.equals(WEB_VIEW_LOCATION_NONE);
        keepWebViewLocation = webViewLocation;

        isScreenLocked = false;

        // get the screen brightness on create
        screenBrightnessOriginal = getScreenBrightness();
        screenBrightnessModeOriginal = getScreenBrightnessMode();

        // myGesture = new GestureDetector(this);
        GestureOverlayView ov = (GestureOverlayView) findViewById(R.id.throttle_overlay);
        ov.addOnGestureListener(this);
        ov.setGestureVisible(false);

        direction_button_touch_listener dbtl;
        function_button_touch_listener fbtl;
        select_function_button_touch_listener sfbt;
        arrow_speed_button_touch_listener asbl;

        bSels = new Button[mainapp.maxThrottles];
        bRSpds = new Button[mainapp.maxThrottles];
        bLSpds = new Button[mainapp.maxThrottles];

        tvLeftDirInds = new TextView[mainapp.maxThrottles];
        tvRightDirInds = new TextView[mainapp.maxThrottles];

        bFwds = new Button[mainapp.maxThrottles];
        bStops = new Button[mainapp.maxThrottles];
        bRevs = new Button[mainapp.maxThrottles];
        sbs = new SeekBar[mainapp.maxThrottles];

        tvGamePads = new TextView[mainapp.maxThrottles];
        tvSpdLabs = new TextView[mainapp.maxThrottles];
        tvSpdVals = new TextView[mainapp.maxThrottles];
        tvVols = new TextView[mainapp.maxThrottles];

        lls = new LinearLayout[mainapp.maxThrottles];
        llSetSpds = new LinearLayout[mainapp.maxThrottles];
        llLocoIds = new LinearLayout[mainapp.maxThrottles];
        llLocoDirs = new LinearLayout[mainapp.maxThrottles];

        fbs = new ViewGroup[mainapp.maxThrottles];

        tops = new int[mainapp.maxThrottles];
        bottoms= new int[mainapp.maxThrottles];

        functionMaps = ( LinkedHashMap<Integer, Button>[]) new LinkedHashMap<?,?>[mainapp.maxThrottles];

        displayUnitScales = new double[mainapp.maxThrottles];

        changeTimers = new ChangeDelay[mainapp.maxThrottles];

        // throttle layouts
        vThrotScr = findViewById(R.id.throttle_screen);
        vThrotScrWrap = findViewById(R.id.throttle_screen_wrapper);

        for (int i=0; i < mainapp.maxThrottles; i++) {
            // set listener for select loco buttons
            Button bSel = (Button) findViewById(R.id.button_select_loco_0);
            TextView tvLeft = (TextView) findViewById(R.id.loco_left_direction_indicaton_0);
            TextView tvRight = (TextView) findViewById(R.id.loco_right_direction_indicaton_0);
            switch (i) {
                case 1:
                    bSel = (Button) findViewById(R.id.button_select_loco_1);
                    tvLeft = (TextView) findViewById(R.id.loco_left_direction_indicaton_1);
                    tvRight = (TextView) findViewById(R.id.loco_right_direction_indicaton_1);
                    break;
                case 2:
                    bSel = (Button) findViewById(R.id.button_select_loco_2);
                    tvLeft = (TextView) findViewById(R.id.loco_left_direction_indicaton_2);
                    tvRight = (TextView) findViewById(R.id.loco_right_direction_indicaton_2);
                    break;
                case 3:
                    bSel = (Button) findViewById(R.id.button_select_loco_3);
                    tvLeft = (TextView) findViewById(R.id.loco_left_direction_indicaton_3);
                    tvRight = (TextView) findViewById(R.id.loco_right_direction_indicaton_3);
                    break;
                case 4:
                    bSel = (Button) findViewById(R.id.button_select_loco_4);
                    tvLeft = (TextView) findViewById(R.id.loco_left_direction_indicaton_4);
                    tvRight = (TextView) findViewById(R.id.loco_right_direction_indicaton_4);
                    break;
                case 5:
                    bSel = (Button) findViewById(R.id.button_select_loco_5);
                    tvLeft = (TextView) findViewById(R.id.loco_left_direction_indicaton_5);
                    tvRight = (TextView) findViewById(R.id.loco_right_direction_indicaton_5);
                    break;
            }
            bSels[i] = bSel;
            bSels[i].setClickable(true);
            sfbt = new select_function_button_touch_listener(i);
            bSels[i].setOnClickListener(sfbt);
            bSels[i].setOnTouchListener(sfbt);
            bSels[i].setOnLongClickListener(sfbt);  // Consist Light Edit
            tvLeftDirInds[i] = tvLeft;
            tvRightDirInds[i] = tvRight;

            // Arrow Keys
            try {

                Button bRight = (Button) findViewById(R.id.right_speed_button_0);
                Button bLeft = (Button) findViewById(R.id.left_speed_button_0);
                switch (i) {
                    case 1:
                        bRight = (Button) findViewById(R.id.right_speed_button_1);
                        bLeft = (Button) findViewById(R.id.left_speed_button_1);
                        break;
                    case 2:
                        bRight = (Button) findViewById(R.id.right_speed_button_2);
                        bLeft = (Button) findViewById(R.id.left_speed_button_2);
                        break;
                    case 3:
                        bRight = (Button) findViewById(R.id.right_speed_button_3);
                        bLeft = (Button) findViewById(R.id.left_speed_button_3);
                        break;
                    case 4:
                        bRight = (Button) findViewById(R.id.right_speed_button_4);
                        bLeft = (Button) findViewById(R.id.left_speed_button_4);
                        break;
                    case 5:
                        bRight = (Button) findViewById(R.id.right_speed_button_5);
                        bLeft = (Button) findViewById(R.id.left_speed_button_5);
                        break;
                }
                bRSpds[i] = bRight;
                bRSpds[i].setClickable(true);
                asbl = new arrow_speed_button_touch_listener(i, "right");
                bRSpds[i].setOnLongClickListener(asbl);
                bRSpds[i].setOnTouchListener(asbl);
                bRSpds[i].setOnClickListener(asbl);

                bLSpds[i] = bLeft;
                bLSpds[i].setClickable(true);
                asbl = new arrow_speed_button_touch_listener(i, "left");
                bLSpds[i].setOnLongClickListener(asbl);
                bLSpds[i].setOnTouchListener(asbl);
                bLSpds[i].setOnClickListener(asbl);

            } catch (Exception ex) {
                Log.d("debug", "onCreate: " + ex.getMessage());
            }

            // set listeners for 3 direction buttons for each throttle
            //----------------------------------------
            Button bFwd = (Button) findViewById(R.id.button_fwd_0);
            Button bStop = (Button) findViewById(R.id.button_stop_0);
            Button bRev = (Button) findViewById(R.id.button_rev_0);
            View v = findViewById(R.id.speed_cell_0);
            switch (i) {
                case 1:
                    bFwd = (Button) findViewById(R.id.button_fwd_1);
                    bStop = (Button) findViewById(R.id.button_stop_1);
                    bRev = (Button) findViewById(R.id.button_rev_1);
                    v = findViewById(R.id.speed_cell_1);
                    break;
                case 2:
                    bFwd = (Button) findViewById(R.id.button_fwd_2);
                    bStop = (Button) findViewById(R.id.button_stop_2);
                    bRev = (Button) findViewById(R.id.button_rev_2);
                    v = findViewById(R.id.speed_cell_2);
                    break;
                case 3:
                    bFwd = (Button) findViewById(R.id.button_fwd_3);
                    bStop = (Button) findViewById(R.id.button_stop_3);
                    bRev = (Button) findViewById(R.id.button_rev_3);
                    v = findViewById(R.id.speed_cell_3);
                    break;
                case 4:
                    bFwd = (Button) findViewById(R.id.button_fwd_4);
                    bStop = (Button) findViewById(R.id.button_stop_4);
                    bRev = (Button) findViewById(R.id.button_rev_4);
                    v = findViewById(R.id.speed_cell_4);
                    break;
                case 5:
                    bFwd = (Button) findViewById(R.id.button_fwd_5);
                    bStop = (Button) findViewById(R.id.button_stop_5);
                    bRev = (Button) findViewById(R.id.button_rev_5);
                    v = findViewById(R.id.speed_cell_5);
                    break;
            }

            bFwds[i] = bFwd;
            dbtl = new direction_button_touch_listener(direction_button.LEFT, i);
            bFwds[i].setOnTouchListener(dbtl);

            bStops[i] = bStop;
            fbtl = new function_button_touch_listener(function_button.STOP, i);
            bStops[i].setOnTouchListener(fbtl);

            bRevs[i] = bRev;
            dbtl = new direction_button_touch_listener(direction_button.RIGHT, i);
            bRevs[i].setOnTouchListener(dbtl);

            fbtl = new function_button_touch_listener(function_button.SPEED_LABEL, i);
            v.setOnTouchListener(fbtl);

            // set up listeners for all throttles
            SeekBar s = (SeekBar) findViewById(R.id.speed_0);
            switch (i) {
                case 1:
                    s = (SeekBar) findViewById(R.id.speed_1);
                    break;
                case 2:
                    s = (SeekBar) findViewById(R.id.speed_2);
                    break;
                case 3:
                    s = (SeekBar) findViewById(R.id.speed_3);
                    break;
                case 4:
                    s = (SeekBar) findViewById(R.id.speed_4);
                    break;
                case 5:
                    s = (SeekBar) findViewById(R.id.speed_5);
                    break;
            }
            throttle_listener thl;
            sbs[i] = s;
            thl = new throttle_listener(i);
            sbs[i].setOnSeekBarChangeListener(thl);
            sbs[i].setOnTouchListener(thl);

            max_throttle_change = 1;
//          displaySpeedSteps = false;

            switch (i) {
                case 0:
                    lls[i] = (LinearLayout) findViewById(R.id.throttle_0);
//                    llSetSpds[i] = (LinearLayout) findViewById(R.id.throttle_0_setspeed);
                    llLocoIds[i] = (LinearLayout) findViewById(R.id.loco_buttons_group_0);
                    llLocoDirs[i] = (LinearLayout) findViewById(R.id.dir_buttons_table_0);
                    tvVols[i] = (TextView) findViewById(R.id.volume_indicator_0); // volume indicators
                    tvGamePads[i] = (TextView) findViewById(R.id.gamepad_indicator_0); // gamepad indicators
                    tvSpdLabs[i] = (TextView) findViewById(R.id.speed_label_0); // set_default_function_labels();
                    tvSpdVals[i] = (TextView) findViewById(R.id.speed_value_label_0);
//**//                    fbs[i] = (ViewGroup) findViewById(R.id.function_buttons_table_0);
                    break;
                case 1:
                    lls[i] = (LinearLayout) findViewById(R.id.throttle_1);
//                    llSetSpds[i] = (LinearLayout) findViewById(R.id.throttle_1_setspeed);
                    llLocoIds[i] = (LinearLayout) findViewById(R.id.loco_buttons_group_1);
                    llLocoDirs[i] = (LinearLayout) findViewById(R.id.dir_buttons_table_1);
                    tvVols[i] = (TextView) findViewById(R.id.volume_indicator_1); // volume indicators
                    tvGamePads[i] = (TextView) findViewById(R.id.gamepad_indicator_1); // gamepad indicators
                    tvSpdLabs[i] = (TextView) findViewById(R.id.speed_label_1); // set_default_function_labels();
                    tvSpdVals[i] = (TextView) findViewById(R.id.speed_value_label_1);
//**//                    fbs[i] = (ViewGroup) findViewById(R.id.function_buttons_table_1);
                    break;
                case 2:
                    lls[i] = (LinearLayout) findViewById(R.id.throttle_2);
//                    llSetSpds[i] = (LinearLayout) findViewById(R.id.throttle_2_setspeed);
                    llLocoIds[i] = (LinearLayout) findViewById(R.id.loco_buttons_group_2);
                    llLocoDirs[i] = (LinearLayout) findViewById(R.id.dir_buttons_table_2);
                    tvVols[i] = (TextView) findViewById(R.id.volume_indicator_2); // volume indicators
                    tvGamePads[i] = (TextView) findViewById(R.id.gamepad_indicator_2); // gamepad indicators
                    tvSpdLabs[i] = (TextView) findViewById(R.id.speed_label_2); // set_default_function_labels();
                    tvSpdVals[i] = (TextView) findViewById(R.id.speed_value_label_2);
//**//                    fbs[i] = (ViewGroup) findViewById(R.id.function_buttons_table_2);
                    break;
                case 3:
                    lls[i] = (LinearLayout) findViewById(R.id.throttle_3);
//                    llSetSpds[i] = (LinearLayout) findViewById(R.id.throttle_3_setspeed);
                    llLocoIds[i] = (LinearLayout) findViewById(R.id.loco_buttons_group_3);
                    llLocoDirs[i] = (LinearLayout) findViewById(R.id.dir_buttons_table_3);
                    tvVols[i] = (TextView) findViewById(R.id.volume_indicator_3); // volume indicators
                    tvGamePads[i] = (TextView) findViewById(R.id.gamepad_indicator_3); // gamepad indicators
                    tvSpdLabs[i] = (TextView) findViewById(R.id.speed_label_3); // set_default_function_labels();
                    tvSpdVals[i] = (TextView) findViewById(R.id.speed_value_label_3);
//**//                    fbs[i] = (ViewGroup) findViewById(R.id.function_buttons_table_3);
                    break;
                case 4:
                    lls[i] = (LinearLayout) findViewById(R.id.throttle_4);
//                    llSetSpds[i] = (LinearLayout) findViewById(R.id.throttle_4_setspeed);
                    llLocoIds[i] = (LinearLayout) findViewById(R.id.loco_buttons_group_4);
                    llLocoDirs[i] = (LinearLayout) findViewById(R.id.dir_buttons_table_4);
                    tvVols[i] = (TextView) findViewById(R.id.volume_indicator_4); // volume indicators
                    tvGamePads[i] = (TextView) findViewById(R.id.gamepad_indicator_4); // gamepad indicators
                    tvSpdLabs[i] = (TextView) findViewById(R.id.speed_label_4); // set_default_function_labels();
                    tvSpdVals[i] = (TextView) findViewById(R.id.speed_value_label_4);
//**//                    fbs[i] = (ViewGroup) findViewById(R.id.function_buttons_table_4);
                    break;
                case 5:
                    lls[i] = (LinearLayout) findViewById(R.id.throttle_5);
//                    llSetSpds[i] = (LinearLayout) findViewById(R.id.throttle_5_setspeed);
                    llLocoIds[i] = (LinearLayout) findViewById(R.id.loco_buttons_group_5);
                    llLocoDirs[i] = (LinearLayout) findViewById(R.id.dir_buttons_table_5);
                    tvVols[i] = (TextView) findViewById(R.id.volume_indicator_5); // volume indicators
                    tvGamePads[i] = (TextView) findViewById(R.id.gamepad_indicator_5); // gamepad indicators
                    tvSpdLabs[i] = (TextView) findViewById(R.id.speed_label_5); // set_default_function_labels();
                    tvSpdVals[i] = (TextView) findViewById(R.id.speed_value_label_5);
//**//                    fbs[i] = (ViewGroup) findViewById(R.id.function_buttons_table_5);
                    break;
            }

            // set throttle change delay timers
            changeTimers[i] = new ChangeDelay(i);

        }

        clearVolumeAndGamepadAdditionalIndicators();

        setDirectionButtonLabels(); // set all the direction button labels

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        setActiveThrottle(0); // set the throttle the volume keys control depending on the preference to the default 0

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
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
            changeTimers[throttleIndex] = new ChangeDelay(throttleIndex);
        }
        // tone generator for feedback sounds
        try {
            tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                preferences.getIntPrefValue(prefs,"prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));
        } catch (RuntimeException e) {
            Log.e("Engine_Driver", "new ToneGenerator failed. Runtime Exception, OS " + android.os.Build.VERSION.SDK_INT + " Message: " + e);
        }
        // set GamePad Support
        setGamepadKeys();

        // initialise ESU MCII
        if (IS_ESU_MCII) {
            Log.d("Engine_Driver", "ESU_MCII: Initialise fragments...");
            int zeroTrim = preferences.getIntPrefValue(prefs,"prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
            esuThrottleFragment = ThrottleFragment.newInstance(zeroTrim);
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

            // Now apply knob zero trim
            updateEsuMc2ZeroTrim();
            Log.d("Engine_Driver", "ESU_MCII: Initialisation complete");
        }

        setupSensor(); // setup the support for shake actions.

        setupTts();

    } // end of onCreate()

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) { // expedite
            mainapp.appIsFinishing = true;
            this.finish();
            overridePendingTransition(0, 0);
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
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
            enable_disable_buttons(throttleIndex);
        }

        gestureFailed = false;
        gestureInProgress = false;

        getCommonPrefs(false);
        getDirectionButtonPrefs();
        setThottleNumLimits();

        clearVolumeAndGamepadAdditionalIndicators();

        getDirectionButtonPrefs();
        setDirectionButtonLabels(); // set all the direction button labels

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

        if (IS_ESU_MCII && isEsuMc2Stopped) {
            if (isEsuMc2AllStopped) {
                // disable buttons for all throttles
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
                    setEnabledEsuMc2ThrottleScreenButtons(throttleIndex, false);
                }
            } else {
                // disable buttons for current throttle
                setEnabledEsuMc2ThrottleScreenButtons(whichVolume, false);
            }
        }

        // update the direction indicators
        showDirectionIndications();

        if (TMenu != null) {
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {

                switch (throttleIndex) {
                    case 0:
                        TMenu.findItem(R.id.EditConsist0_menu).setVisible(mainapp.consists[0].isMulti());
                        TMenu.findItem(R.id.EditLightsConsist0_menu).setVisible(mainapp.consists[0].isMulti());
                        break;
                    case 1:
                        TMenu.findItem(R.id.EditLightsConsist1_menu).setVisible(mainapp.consists[1].isMulti());
                        TMenu.findItem(R.id.EditConsist1_menu).setVisible(mainapp.consists[1].isMulti());
                        break;
                    case 2:
                        TMenu.findItem(R.id.EditLightsConsist2_menu).setVisible(mainapp.consists[2].isMulti());
                        TMenu.findItem(R.id.EditConsist2_menu).setVisible(mainapp.consists[2].isMulti());
                        break;
                    case 3:
                        TMenu.findItem(R.id.EditLightsConsist3_menu).setVisible(mainapp.consists[3].isMulti());
                        TMenu.findItem(R.id.EditConsist3_menu).setVisible(mainapp.consists[3].isMulti());
                        break;
                    case 4:
                        TMenu.findItem(R.id.EditLightsConsist4_menu).setVisible(mainapp.consists[4].isMulti());
                        TMenu.findItem(R.id.EditConsist4_menu).setVisible(mainapp.consists[4].isMulti());
                        break;
                    case 5:
                        TMenu.findItem(R.id.EditLightsConsist5_menu).setVisible(mainapp.consists[5].isMulti());
                        TMenu.findItem(R.id.EditConsist5_menu).setVisible(mainapp.consists[5].isMulti());
                        break;
                }
            }
        }

        CookieSyncManager.getInstance().startSync();

        if (!prefAccelerometerShake.equals(ACCELERATOROMETER_SHAKE_NONE)) {
            if (!accelerometerCurrent) { // perference has only just been changed to turn it on
                setupSensor();
            } else {
                sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
        }

        setupTts();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState); // save history (on rotation) if at least one page has loaded

        // save the requested throttle direction so we can update the
        // direction indication immediately in OnCreate following a rotate

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
            outState.putSerializable("dir" + mainapp.throttleIntToChar(throttleIndex), dirs[throttleIndex]);
        }
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

            if (accelerometerCurrent) {
                sensorManager.unregisterListener(shakeDetector);
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
        if (tg != null) {
            tg.release();
        }
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
        if (webViewLocation.equals(WEB_VIEW_LOCATION_NONE)) {       // if not displaying webview
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
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
            set_function_labels_and_listeners_for_view(throttleIndex);
        }
    }

    // helper function to set up function buttons for each throttle
    // loop through all function buttons and
    // set label and dcc functions (based on settings) or hide if no label
    void set_function_labels_and_listeners_for_view(int whichThrottle) {
        // Log.d("Engine_Driver","starting set_function_labels_and_listeners_for_view");

//        // implemented in derived class, but called from this class

        if (fbs != null) { // if it is null it probably because the Throttle Screen Type does not have Functions Buttons
            if (fbs[0] != null) {
                ViewGroup tv; // group
                ViewGroup r; // row
                function_button_touch_listener fbtl;
                Button b; // button
                int k = 0; // button count
                LinkedHashMap<Integer, String> function_labels_temp;
                LinkedHashMap<Integer, Button> functionButtonMap = new LinkedHashMap<>();

                tv = fbs[whichThrottle];

                // note: we make a copy of function_labels_x because TA might change it
                // while we are using it (causing issues during button update below)
                function_labels_temp = mainapp.function_labels_default;
                if (!prefAlwaysUseDefaultFunctionLabels) {
                    if (mainapp.function_labels != null
                            && mainapp.function_labels[whichThrottle] != null
                            && mainapp.function_labels[whichThrottle].size() > 0) {
                        function_labels_temp = new LinkedHashMap<>(mainapp.function_labels[whichThrottle]);
                    } else {
                        if (!mainapp.consists[whichThrottle].isLeadFromRoster()) {
                            function_labels_temp = mainapp.function_labels_default;
                        } else {
                            function_labels_temp = mainapp.function_labels_default_for_roster;
                        }
                    }
                }

                // put values in array for indexing in next step
                // to do this
                ArrayList<Integer> aList = new ArrayList<>();
                aList.addAll(function_labels_temp.keySet());

                if (tv != null) {
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
                                if ((mainapp.getCurrentTheme().equals(THEME_DEFAULT))) {
                                    bt = bt + "        ";  // pad with spaces, and limit to 7 characters
                                    b.setText(bt.substring(0, 7));
                                } else {
                                    bt = bt + "                      ";  // pad with spaces, and limit to 20 characters
                                    b.setText(bt.trim());
                                }
                                b.setVisibility(View.VISIBLE);
                                b.setEnabled(false); // start out with everything disabled
                            } else {
                                b.setVisibility(View.GONE);
                            }
                            k++;
                        }
                    }
                }

                // update the function-to-button map for the current throttle
                functionMaps[whichThrottle] = functionButtonMap;
            }
        }
    }

    // helper function to get a numbered function button from its throttle and function number
    Button getFunctionButton(char whichThrottle, int func) {
        Button b; // button
        LinkedHashMap<Integer, Button> functionButtonMap;

        functionButtonMap = functionMaps[whichThrottle];
        b = functionButtonMap.get(func);
        return b;
   }

    // lookup and set values of various informational text labels and size the
    // screen elements
    @SuppressWarnings("deprecation")
    protected void set_labels() {
        // Log.d("Engine_Driver","starting set_labels");

        if (mainapp.appIsFinishing) { return;}

        // avoid NPE by not letting this run too early (reported to Play Store)
        if (tvVols[0] == null) return;

        // hide or display volume control indicator based on variable
        setVolumeIndicator();
        setGamepadIndicator();

        // set up max speeds for throttles
        int maxThrottle = preferences.getIntPrefValue(prefs, "maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (maxThrottle * .01)); // convert from percent
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
            sbs[throttleIndex].setMax(maxThrottle);
        }

        // set max allowed change for throttles from prefs
        int maxChange = preferences.getIntPrefValue(prefs, "maximum_throttle_change_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleChangeDefaultValue));
        max_throttle_change = (int) Math.round(maxThrottle * (maxChange * .01));

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottles; throttleIndex++) {
            sbs[throttleIndex].setMax(maxThrottle);
            if (mainapp.consists[throttleIndex].isEmpty()) {
                maxSpeedSteps[throttleIndex] = 100;
            }
            //get speed steps from prefs
            speedStepPref = preferences.getIntPrefValue(prefs, "DisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
            setDisplayUnitScale(throttleIndex);

            setDisplayedSpeed(throttleIndex, sbs[throttleIndex].getProgress());  // update numeric speeds since units might have changed
        }

        //adjust several items in the menu
        if (TMenu != null) {
            mainapp.displayEStop(TMenu);
            mainapp.setPowerStateButton(TMenu);
            mainapp.displayPowerStateMenuButton(TMenu);
            mainapp.setPowerMenuOption(TMenu);
            mainapp.setWebMenuOption(TMenu);
            mainapp.setRoutesMenuOption(TMenu);
            mainapp.setTurnoutsMenuOption(TMenu);
            mainapp.setGamepadTestMenuOption(TMenu,gamepadCount);
            mainapp.setFlashlightButton(TMenu);
            mainapp.displayFlashlightMenuButton(TMenu);
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
        if (key == KeyEvent.KEYCODE_BACK) {
            return true; // stop processing this key
        }

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
                if (webView != null) {
                    setImmersiveModeOn(webView);
                }
                mainapp.checkExit(this);
            return (true); // stop processing this key
        } else if ((key == KEYCODE_VOLUME_UP) || (key == KEYCODE_VOLUME_DOWN)) {  // use volume to change speed for specified loco

            if (!prefDisableVolumeKeys) {  // ignore the volume keys if the preference its set
                for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                    if (throttleIndex == whichVolume && mainapp.consists[throttleIndex].isActive()) {
                        if (key == KEYCODE_VOLUME_UP) {
                            if (repeatCnt == 0) {
                                mVolumeKeysAutoIncrement = true;
                                volumeKeysRepeatUpdateHandler.post(new volumeKeysRptUpdater(throttleIndex));
                            }
                        } else {
                            if (repeatCnt == 0) {
                                mVolumeKeysAutoDecrement = true;
                                volumeKeysRepeatUpdateHandler.post(new volumeKeysRptUpdater(throttleIndex));
                            }
                        }
                    }
                }
            }
            return (true); // stop processing this key
        }
        return (super.onKeyDown(key, event)); // continue with normal key
        // processing
    }

    private void disconnect() {
        //loop thru all throttles and send release to server for any that are active
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            if (getConsist(throttleIndex).isActive()) {
                release_loco(throttleIndex);
            }
        }

        webView.stopLoading();
        mainapp.appIsFinishing = true;
        this.finish(); // end this activity
        overridePendingTransition(0, 0);
    }

    // request release of specified throttle
    void release_loco(int whichThrottle) {
        mainapp.consists[whichThrottle].release();

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", whichThrottle); // pass T, S or G in message
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.throttle_menu, menu);
        TMenu = menu;
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (webView != null) {
            setImmersiveModeOn(webView);
        }

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
                if (IS_ESU_MCII) {
                    Log.d("Engine_Driver", "ESU_MCII: Move knob request for EStop");
                    setEsuThrottleKnobPosition(whichVolume, 0);
                }

                break;
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    AlertDialog.Builder b = new AlertDialog.Builder(this);
                    b.setIcon(android.R.drawable.ic_dialog_alert);
                    b.setTitle(getApplicationContext().getResources().getString(R.string.powerWillNotWorkTitle));
                    b.setMessage(getApplicationContext().getResources().getString(R.string.powerWillNotWork));
                    b.setCancelable(true);
                    b.setNegativeButton("OK", null);
                    AlertDialog alert = b.create();
                    alert.show();
                    mainapp.displayPowerStateMenuButton(TMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                break;

            case R.id.EditConsist0_menu:
                Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);

                consistEdit.putExtra("whichThrottle", '0');
                navigatingAway = true;
                startActivityForResult(consistEdit, ACTIVITY_CONSIST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EditConsist1_menu:
                Intent consistEdit2 = new Intent().setClass(this, ConsistEdit.class);
                consistEdit2.putExtra("whichThrottle", '1');
                navigatingAway = true;
                startActivityForResult(consistEdit2, ACTIVITY_CONSIST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
                                                                                                                                                                                                                                                                                                                                            
            case R.id.EditConsist2_menu:
                Intent consistEdit3 = new Intent().setClass(this, ConsistEdit.class);
                consistEdit3.putExtra("whichThrottle", '2');
                navigatingAway = true;
                startActivityForResult(consistEdit3, ACTIVITY_CONSIST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.EditLightsConsist0_menu:
                Intent consistLightsEdit = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit.putExtra("whichThrottle", '0');
                navigatingAway = true;
                startActivityForResult(consistLightsEdit, ACTIVITY_CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.EditLightsConsist1_menu:
                Intent consistLightsEdit2 = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit2.putExtra("whichThrottle", '1');
                navigatingAway = true;
                startActivityForResult(consistLightsEdit2, ACTIVITY_CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EditLightsConsist2_menu:
                Intent consistLightsEdit3 = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit3.putExtra("whichThrottle", '2');
                navigatingAway = true;
                startActivityForResult(consistLightsEdit3, ACTIVITY_CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.gamepad_test_mnu1:
                in = new Intent().setClass(this, gamepad_test.class);
                in.putExtra("whichGamepadNo", "0");
                navigatingAway = true;
                startActivityForResult(in, ACTIVITY_GAMEPAD_TEST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.gamepad_test_mnu2:
                in = new Intent().setClass(this, gamepad_test.class);
                in.putExtra("whichGamepadNo", "1");
                navigatingAway = true;
                startActivityForResult(in, ACTIVITY_GAMEPAD_TEST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.gamepad_test_mnu3:
                in = new Intent().setClass(this, gamepad_test.class);
                in.putExtra("whichGamepadNo", "2");
                navigatingAway = true;
                startActivityForResult(in, ACTIVITY_GAMEPAD_TEST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.flashlight_button:
                mainapp.toggleFlashlight(this, TMenu);
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
                    setActiveThrottle(whichVolume);
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
                    if (tg != null) {
                        tg.release();
                        try {
                            tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                                preferences.getIntPrefValue(prefs, "prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));
                        } catch (RuntimeException e) {
                            Log.e("Engine_Driver", "new ToneGenerator failed. Runtime Exception, OS " + android.os.Build.VERSION.SDK_INT + " Message: " + e);
                        }
                    }
                    // update GamePad Support
                    setGamepadKeys();
                }
                if (resultCode == preferences.RESULT_ESUMCII) { // ESU MCII pref change
                    // update zero trim values
                    updateEsuMc2ZeroTrim();
                }
                // in case the preference has changed but the current screen does not support the number selected.
                setThottleNumLimits();
                break;
            }
            case ACTIVITY_GAMEPAD_TEST: {
                if (data != null) {
                    String whichGamepadNo = data.getExtras().getString("whichGamepadNo");
                    if (whichGamepadNo != null) {
                        int result = Integer.valueOf(whichGamepadNo.substring(1, 2));
                        int gamepadNo = Integer.valueOf(whichGamepadNo.substring(0, 1));
                        switch (result) {
                            case GAMEPAD_TEST_PASS:
                                gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_GOOD;
                                speakWords(TTS_MSG_GAMEPAD_GAMEPAD_TEST_COMPLETE,' ');
                                break;
                            case GAMEPAD_TEST_SKIPPED:
                                gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_GOOD;
                                speakWords(TTS_MSG_GAMEPAD_GAMEPAD_TEST_SKIPPED, ' ');
                                break;
                            case GAMEPAD_TEST_FAIL:
                                gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_BAD;
                                speakWords(TTS_MSG_GAMEPAD_GAMEPAD_TEST_FAIL, ' ');
                                break;
                            case GAMEPAD_TEST_RESET:
                                gamepadCount = 0;
                                for (int i=0;i<gamePadDeviceIds.length;i++) {
                                    gamePadDeviceIds[i]=0;
                                    gamePadDeviceIdsTested[i]=0;
                                }
                                for (int i = 0; i < 3; i++) {
                                    gamePadIds[i] = 0;
                                    gamePadThrottleAssignment[0] = -1;
                                }
                                mainapp.setGamepadTestMenuOption(TMenu,gamepadCount);
                                setGamepadIndicator();
                                speakWords(TTS_MSG_GAMEPAD_GAMEPAD_TEST_RESET,' ');
                                break;
                            default:
                                Log.e("Engine_Driver", "OnActivityResult(ACTIVITY_GAMEPAD_TEST) invalid result!");
                        }
                    }
                } else {
                    Log.e("Engine_Driver", "OnActivityResult(ACTIVITY_GAMEPAD_TEST) called with null data!");
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
            int whichThrottle = mainapp.throttleCharToInt(extras.getChar("whichThrottle"));
            int dir;
            int speed;
            dir = dirs[whichThrottle];
            speed = (sbs[whichThrottle] == null ? 0 : sbs[whichThrottle].getProgress());
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
            for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                if ((sbs[throttleIndex].isEnabled() && gestureStartY >= tops[throttleIndex] && gestureStartY <= bottoms[throttleIndex])) {
                    // Log.d("Engine_Driver","exiting gestureStart");
                    return;
                }
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
                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastImmersiveModeDisabled), Toast.LENGTH_SHORT).show();
                            } else {
                                setImmersiveModeOn(webView);
                            }
                        }
                    } else {
                        // swipe up
                        switch(prefSwipeUpOption) {
                            case SWIPE_UP_OPTION_WEB:
                                showHideWebView(getApplicationContext().getResources().getString(R.string.toastSwipeUpViewHidden));
                                break;
                            case SWIPE_UP_OPTION_LOCK:
                                setRestoreScreenLockDim(getApplicationContext().getResources().getString(R.string.toastSwipeUpScreenLocked));
                                break;
                            case SWIPE_UP_OPTION_DIM:
                                setRestoreScreenDim(getApplicationContext().getResources().getString(R.string.toastSwipeUpScreenDimmed));
                                break;
                            case SWIPE_UP_OPTION_IMMERSIVE:
                                if (immersiveModeIsOn) {
                                    setImmersiveModeOff(webView);
                                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastImmersiveModeDisabled), Toast.LENGTH_SHORT).show();
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
//    public void promptForSteal(String addr, char whichThrottle) {
    public void promptForSteal(String addr, int whichThrottle) {
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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    protected void setThottleNumLimits() {
        if (mainapp.numThrottles > mainapp.maxThrottlesCurrentScreen) {   // Maximum number of throttles this screen supports
            mainapp.numThrottles = mainapp.maxThrottlesCurrentScreen;
        }
        if (mainapp.maxThrottles > mainapp.maxThrottlesCurrentScreen) {   // Maximum number of throttles this screen supports
            mainapp.maxThrottles = mainapp.maxThrottlesCurrentScreen;
        }
    }
}