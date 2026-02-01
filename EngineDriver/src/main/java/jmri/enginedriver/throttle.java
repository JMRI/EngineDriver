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

import static android.view.InputDevice.getDevice;
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
import static android.view.KeyEvent.KEYCODE_VOLUME_MUTE;
import static android.view.KeyEvent.KEYCODE_HEADSETHOOK;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.KeyEvent.KEYCODE_W;
import static android.view.KeyEvent.KEYCODE_X;
import static android.view.View.VISIBLE;
import static jmri.enginedriver.threaded_application.MAX_FUNCTIONS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.gesture.GestureOverlayView;
import androidx.core.graphics.Insets;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.text.InputType;
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
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import eu.esu.mobilecontrol2.sdk.StopButtonFragment;
import eu.esu.mobilecontrol2.sdk.ThrottleFragment;
import eu.esu.mobilecontrol2.sdk.ThrottleScale;

import jmri.enginedriver.esu_mcII.EsuMc2LedControl;
import jmri.enginedriver.type.Consist;
import jmri.enginedriver.esu_mcII.EsuMc2ButtonAction;
import jmri.enginedriver.esu_mcII.EsuMc2Led;
import jmri.enginedriver.esu_mcII.EsuMc2LedState;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.auto_increment_or_decrement_type;
import jmri.enginedriver.type.beep_type;
import jmri.enginedriver.type.consist_function_rule_style_type;
import jmri.enginedriver.type.gamepad_or_keyboard_event_type;
import jmri.enginedriver.type.kids_timer_action_type;
import jmri.enginedriver.type.light_follow_type;
import jmri.enginedriver.type.max_throttles_current_screen_type;
import jmri.enginedriver.type.restart_reason_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.direction_button;
import jmri.enginedriver.type.function_button;

import jmri.enginedriver.type.screen_swipe_index_type;
import jmri.enginedriver.type.activity_outcome_type;
import jmri.enginedriver.type.sounds_type;
import jmri.enginedriver.type.speed_button_type;
import jmri.enginedriver.type.throttle_screen_type;
import jmri.enginedriver.type.tts_msg_type;
import jmri.enginedriver.util.BackgroundImageLoader;
import jmri.enginedriver.util.GamePadKeyLoader;
import jmri.enginedriver.util.GamepadEventHandler;
import jmri.enginedriver.util.GamepadNotifierInterface;
import jmri.enginedriver.util.HorizontalSeekBar;
import jmri.enginedriver.util.KeyboardEventHandler;
import jmri.enginedriver.util.KeyboardNotifierInterface;
import jmri.enginedriver.util.Tts;
import jmri.enginedriver.util.VerticalSeekBar;
import jmri.enginedriver.util.InPhoneLocoSounds;
import jmri.enginedriver.util.InPhoneLocoSoundsLoader;
import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;
import jmri.enginedriver.util.ShakeDetector;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import jmri.enginedriver.import_export.ImportExportPreferences;
import jmri.enginedriver.type.pref_import_type;
import jmri.enginedriver.type.consist_function_is_type;
import jmri.enginedriver.type.consist_function_latching_type;
import jmri.enginedriver.type.function_is_type;
import jmri.enginedriver.type.button_press_message_type;
import jmri.enginedriver.type.pause_speed_type;
import jmri.enginedriver.type.slider_type;
import jmri.enginedriver.type.web_view_location_type;
import jmri.enginedriver.type.selected_loco_indicator_type;
import jmri.enginedriver.type.speed_commands_from_type;
import jmri.enginedriver.type.gamepad_test_type;
import jmri.enginedriver.type.swipe_up_down_option_type;
import jmri.enginedriver.type.speed_step_type;
import jmri.enginedriver.type.acceleratorometer_action_type;
import jmri.enginedriver.type.direction_type;

public class throttle extends AppCompatActivity implements
        android.gesture.GestureOverlayView.OnGestureListener,
        PermissionsHelper.PermissionsHelperGrantedCallback,
        KeyboardNotifierInterface,
        GamepadNotifierInterface {

    static final String activityName = "throttle";

    protected threaded_application mainapp; // hold pointer to mainapp
    protected SharedPreferences prefs;
    protected Bundle onCreateSavedInstanceState = null;

    private GamePadKeyLoader gamePadKeyLoader;
    private KeyboardEventHandler keyboardEventHandler;
    private GamepadEventHandler gamepadEventHandler;

    protected static final int MAX_SCREEN_THROTTLES = max_throttles_current_screen_type.DEFAULT;

    protected static final int throttleMargin = 8; // forced margin between the horizontal throttles in dp
    protected int titleBar = 45; // estimate of lost screen height in dp

    // speed scale factors
    public static final int MAX_SPEED_VAL_WIT = 126;    // wit message maximum speed value, max speed slider value
    protected static int[] maxSpeedSteps = {100, 100, 100, 100, 100, 100};             // decoder max speed steps
    protected static int max_throttle_change;          // maximum allowable change of the sliders
    protected static int prefDisplaySpeedUnits = 100;
    private static double[] displayUnitScales;            // display units per slider count

    private static final String VOLUME_INDICATOR = "v";
    private static final int[] GAMEPAD_INDICATOR = {1, 2, 3, 4, 5, 6};

    private String prefSelectedLocoIndicator = selected_loco_indicator_type.NONE;

    protected String keyboardString = "";
    protected int keyboardThrottle = -1;
//    protected boolean keyboardShift = false;

    protected SeekBar[] sbs; // seekbars

//    protected LinearLayout throttleScreenWrapper;

    protected ViewGroup[] functionButtonViewGroups; // function button tables

    protected Button[] bForwards; // buttons
    protected Button[] bStops;
    protected Button[] bPauses;
    protected Button[] bReverses;
    protected Button[] bSelects;
    protected TextView[] tvbSelectsLabels;
    protected Button[] bRightSpeeds;
    protected Button[] bLeftSpeeds;

    protected TextView[] tvSpeedLabels; // labels
    protected TextView[] tvSpeedValues;

    protected TextView[] tvDirectionIndicatorForwards;
    protected TextView[] tvDirectionIndicatorReverses;

    protected TextView[] tvVolumeIndicators; // volume indicators

    protected TextView[] tvLeftDirectionIndicators; // direction indicators
    protected TextView[] tvRightDirectionIndicators;

    protected TextView[] tvGamePads;

    protected LinearLayout[] llThrottleLayouts; // throttles
    protected LinearLayout[] llSetSpeeds;

    protected HorizontalSeekBar[] sbSpeeds = {};
    protected VerticalSeekBar[] vsbSpeeds;
    protected VerticalSeekBar[] vsbSwitchingSpeeds;
    protected HorizontalSeekBar[] hsbSwitchingSpeeds;

    // Semi-realistic Throttle
    protected VerticalSeekBar[] vsbSemiRealisticThrottles;
    protected VerticalSeekBar[] vsbBrakes;
    protected VerticalSeekBar[] vsbLoads;
    protected TextView[] tvTargetSpdVals;
    protected TextView[] tvTargetAccelerationVals;
    protected boolean isSemiRealisticThrottle = false;

    protected Button[] bTargetForwards;
    protected Button[] bTargetReverses;
    protected Button[] bTargetNeutrals;

    protected Button[] bTargetRightSpeeds;
    protected Button[] bTargetLeftSpeeds;
    protected Button[] bTargetStops;

    // semi-realistic throttle variables
    protected static int[] targetSpeeds = {0, 0, 0, 0, 0, 0};
    protected static int[] prevTargetSpeeds = {0, 0, 0, 0, 0, 0};
    protected static int[] prevLoads = {0, 0, 0, 0, 0, 0};
    protected static int[] targetDirections = {1, 1, 1, 1, 1, 1};
    protected static double[] targetAccelerations = {0, 0, 0, 0, 0, 0};  /// -4=full brake  +1=normal acceleration  0=at target speed
    protected static String[] targetAccelerationsForDisplay = {"", "", "", "", "", ""};  /// -4=full brake  +1=normal acceleration  0=at target speed

//    protected Handler[] semiRealisticTargetSpeedUpdateHandlers = {null, null, null, null, null, null};
//    protected Handler[] semiRealisticSpeedButtonUpdateHandlers = {null, null, null, null, null, null};
    protected int[] mSemiRealisticAutoIncrementOrDecrement = {0, 0, 0, 0, 0, 0};  //off
//    protected int[] mSemiRealisticSpeedButtonsAutoIncrementOrDecrement = {0, 0, 0, 0, 0, 0};  //off
//    protected boolean semiRealisticSpeedButtonLongPressActive = false;
//    protected ImageView[] airIndicators = {null, null, null, null, null, null};
//    protected ImageView[] airLineIndicators = {null, null, null, null, null, null};
//    protected int[] airValues = {100, 100, 100, 100, 100, 100};
//    protected int[] airLineValues = {100, 100, 100, 100, 100, 100};
//    protected boolean isAirRecharging = false;
//    protected boolean isAirLineRecharging = false;
//    protected int[] previousBrakePosition = {0, 0, 0, 0, 0, 0};
//    protected Handler[] semiRealisticAirUpdateHandlers = {null, null, null, null, null, null};
//    protected Handler[] semiRealisticAirLineUpdateHandlers = {null, null, null, null, null, null};

    // SPDHT for Speed Id and Direction Button Heights
    protected LinearLayout[] llLocoIdAndSpeedViewGroups;
    protected LinearLayout[] llLocoDirectionButtonViewGroups;

    // SPDHT
    protected View vThrottleScreen;
    protected View vThrottleScreenWrap;

    private boolean stealPromptActive = false; //true while steal dialog is open
    protected int whichVolume = 0;
    //    private int whichLastVolume = -1;
    private int whichLastGamepad1 = -1;
    private String prefGamePadDoublePressStop = "";

    private int lastGamepadFunction = -1;
    private boolean lastGamepadFunctionIsPressed = false;

    // screen coordinates for throttle sliders, so we can ignore swipe on them
    protected int[] sliderTopLeftX = {0, 0, 0, 0, 0, 0};
    protected int[] sliderTopLeftY = {0, 0, 0, 0, 0, 0};
    protected int[] sliderBottomRightX = {0, 0, 0, 0, 0, 0};
    protected int[] sliderBottomRightY = {0, 0, 0, 0, 0, 0};
    protected GestureOverlayView throttleOverlay;    // screen coordinates for brake sliders, so we can ignore swipe on them
    protected int[] brakeSliderTopLeftX = {0, 0, 0, 0, 0, 0};
    protected int[] brakeSliderTopLeftY = {0, 0, 0, 0, 0, 0};
    protected int[] brakeSliderBottomRightX = {0, 0, 0, 0, 0, 0};
    protected int[] brakeSliderBottomRightY = {0, 0, 0, 0, 0, 0};
    protected int[] loadSliderTopLeftX = {0, 0, 0, 0, 0, 0};
    protected int[] loadSliderTopLeftY = {0, 0, 0, 0, 0, 0};
    protected int[] loadSliderBottomRightX = {0, 0, 0, 0, 0, 0};
    protected int[] loadSliderBottomRightY = {0, 0, 0, 0, 0, 0};


    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    private boolean gestureFailed = false; // gesture didn't meet velocity or distance requirement
    protected boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;

    // not static. can be changed in the preferences
    private String FUNCTION_BUTTON_LOOK_FOR_WHISTLE = "WHISTLE";
    private String FUNCTION_BUTTON_LOOK_FOR_HORN = "HORN";
    private String FUNCTION_BUTTON_LOOK_FOR_BELL = "BELL";
    private String FUNCTION_BUTTON_LOOK_FOR_HEAD = "HEAD";
    private String FUNCTION_BUTTON_LOOK_FOR_LIGHT = "LIGHT";
    private String FUNCTION_BUTTON_LOOK_FOR_REAR = "REAR";

    private List<String> prefConsistFollowStrings;
    private List<String> prefConsistFollowActions;
    private List<Integer> prefConsistFollowHeadlights;
    private String prefConsistFollowDefaultAction = "none";

    private boolean prefSelectiveLeadSound = false;
    private boolean prefSelectiveLeadSoundF1 = false;
    private boolean prefSelectiveLeadSoundF2 = false;

    protected InPhoneLocoSounds ipls;
    protected InPhoneLocoSoundsLoader iplsLoader;

    // function number-to-button maps
    protected LinkedHashMap<Integer, Button>[] functionMaps;

    // current direction
    protected int[] dirs = {1, 1, 1, 1, 1, 1};   // requested direction for each throttle (single or multiple engines)
    protected String[] overrideThrottleNames = {"", "", "", "", "", ""};

    private String noUrl = "file:///android_asset/blank_page.html";
    private static final float initialScale = 1.5f;
    protected WebView webView = null;
    protected String prefWebViewLocation = web_view_location_type.NONE;

    private static float scale = initialScale;      // used to restore throttle web zoom level (after rotation)
    private static boolean clearHistory = false;    // flags webViewClient to clear history when page load finishes
    private static String firstUrl = null;          // desired first url when clearing history
    private static String currentUrl = null;
    private Menu overflowMenu;
    static int REP_DELAY = 25;
    protected int prefSpeedButtonsSpeedStep = 4;
    protected int prefVolumeSpeedButtonsSpeedStep = 1;
    protected int prefGamePadSpeedButtonsSpeedStep = 4;
    protected boolean prefSpeedButtonsSpeedStepDecrement = false;
    protected boolean prefStopButtonEStopOnLongPress = false;

    protected String speedButtonLeftText;
    protected String speedButtonRightText;
    protected String speedButtonUpText;
    protected String speedButtonDownText;

    protected boolean prefHideSlider = false;

    protected boolean prefLimitSpeedButton = false;
    protected boolean prefPauseSpeedButton = false;
    protected boolean prefPauseAlternateButton = false;
    protected int prefPauseSpeedRate = 200;
    protected int prefPauseSpeedStep = 2;
    protected int prefLimitSpeedPercent = 50;
    //    protected int prefLimitSpeedMax = 50;
    protected Button[] bLimitSpeeds;
    protected boolean[] isLimitSpeeds;
    protected Button[] bPauseSpeeds;
    protected int[] isPauseSpeeds = {100, 100, 100, 100, 100, 100};
    protected int[] pauseSpeed = {0, 0, 0, 0, 0, 0};
    protected int[] pauseDir = {1, 1, 1, 1, 1, 1};
    protected float[] limitSpeedSliderScalingFactors;
    protected int[] limitSpeedMax = {100, 100, 100, 100, 100, 100};
    protected boolean[] limitedJump = {false, false, false, false, false, false};

    protected Button[] bMutes;
    protected boolean[] soundsIsMuted = {false, false};
    protected Button[][] bSoundsExtras;

    protected int sliderType = slider_type.HORIZONTAL;

    protected Handler repeatUpdateHandler = new Handler();
    protected boolean[] mAutoIncrement = {false, false, false, false, false, false};
    protected boolean[] mAutoDecrement = {false, false, false, false, false, false};

    // implement delay for briefly ignoring WiT speed reports after sending a throttle speed update
    // this prevents use of speed reports sent by WiT just prior to WiT processing the speed update
    private static final int changeDelay = 1000;            // mSec
    protected PacingDelay[] changeTimers;

    // implement delay for pacing speed change requests so we don't overload the WiT socket
    private static final int maxSpeedMessageRate = 200;      // mSec
    protected SpeedPacingDelay[] speedMessagePacingTimers;

    protected boolean selectLocoRendered = false; // this will be true once setLabels() runs following rendering of the loco select textViews

    // used in the gesture for entering and exiting immersive mode
    private boolean immersiveModeIsOn = false;
    private boolean immersiveModeTempIsOn = false;

    //used in the gesture for temporarily showing the Web View
    protected boolean webViewIsOn = false;
    private String prefSwipeUpOption;
    private String prefSwipeDownOption;
    private String keepWebViewLocation = web_view_location_type.NONE;
    // use for locking the screen on swipe up
    private boolean isScreenLocked = false;
    private boolean screenDimmed = false;
    private int screenBrightnessDim;
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
//    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

    //private int screenBrightnessBright;
    private int screenBrightnessOriginal;
    private int screenBrightnessModeOriginal;

    // used to hold the direction change preferences
    boolean dirChangeWhileMoving;
    boolean stopOnDirectionChange;
    boolean locoSelectWhileMoving;

    // used for the GamePad Support
//    private static final String WHICH_GAMEPAD_MODE_NONE = "None";

    // default to the iOS iCade mappings
//    private String whichGamePadMode = WHICH_GAMEPAD_MODE_NONE;

    private int externalGamepadAction;
    //    private int externalGamepadWhichThrottle;
    private int externalGamepadKeyCode;
    private boolean externalGamepadIsShiftPressed;
    private int externalGamepadRepeatCnt;
    private int externalGamepadWhichGamePadIsEventFrom;

    private float externalGamepadXAxis;
    private float externalGamepadYAxis;
    private float externalGamepadXAxis2;
    private float externalGamepadYAxis2;

    int keyboardStopCount = 0;

//    protected MediaPlayer _mediaPlayer;

    // Gamepad Button preferences
    private final String[] prefGamePadButtons = {"Next Throttle", "Stop", "Function 00/Light", "Function 01/Bell", "Function 02/Horn",
            "Increase Speed", "Reverse", "Decrease Speed", "Forward", "All Stop", "Select", "Left Shoulder", "Right Shoulder", "Left Trigger", "Right Trigger", "Left Thumb", "Right Thumb", "", "", "", "", "", ""};

    //                               0         1    2           3          4          5          6          7          8          9          10        11 12 13 14 15 16 17 18 19 20
    //                              none     NextThr  Speed+    Speed-     Fwd        Rev        All Stop   F2         F1         F0         Stop
    private int[] gamePadKeys = {0, 0, KEYCODE_W, KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int[] gamePadKeys_Up = {0, 0, KEYCODE_W, KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int GAMEPAD_KEYS_LENGTH = 21;
    private static final int[] BUTTON_ACTION_NUMBERS = {
            -1, 9, 5, 7, 8, 6, 0, 1, 3, 2, 4, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1};

    Tts tts;

    private ToneGenerator tg;
    protected Handler gamepadRepeatUpdateHandler = new Handler();
    protected boolean mGamepadAutoIncrement = false;
    protected boolean mGamepadAutoDecrement = false;
    protected Handler volumeKeysRepeatUpdateHandler = new Handler();
    protected boolean mVolumeKeysAutoIncrement = false;
    protected boolean mVolumeKeysAutoDecrement = false;
    protected boolean prefDisableVolumeKeys = false;
    protected boolean prefVolumeKeysFollowLastTouchedThrottle = false;

    protected String prefThrottleScreenType;

    protected boolean prefThrottleViewImmersiveMode = false;
    protected boolean prefThrottleViewImmersiveModeHideToolbar = false;
    protected int prefNumberOfDefaultFunctionLabels = MAX_FUNCTIONS;
    protected boolean prefDecreaseLocoNumberHeight = false;
    protected boolean prefIncreaseSliderHeight = false;
    protected boolean prefShowAddressInsteadOfName = false;
    protected boolean prefIncreaseWebViewSize = false;
    protected boolean prefHideFunctionButtonsOfNonSelectedThrottle = false;

    // preference to change the consist's on long clicks
    boolean prefConsistLightsLongClick;

    protected boolean prefSwapForwardReverseButtons = false;
    protected boolean prefSwapForwardReverseButtonsLongPress = false;
    private final boolean[] currentSwapForwardReverseButtons = {false, false, false, false, false, false};

    protected boolean prefGamepadSwapForwardReverseWithScreenButtons = false;
    protected boolean prefGamepadTestEnforceTesting = true;

    private static final int GAMEPAD_GOOD = 1;
    private static final int GAMEPAD_BAD = 2;

    // not static. can be changed in the preferences
    protected String DIRECTION_BUTTON_LEFT_TEXT = "Forward";
    protected String DIRECTION_BUTTON_RIGHT_TEXT = "Reverse";

    protected String[] FullLeftText = {DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT, DIRECTION_BUTTON_LEFT_TEXT};
    protected String[] FullRightText = {DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT, DIRECTION_BUTTON_RIGHT_TEXT};
    protected String[] dirLeftIndicationText = {"", "", "", "", "", ""};
    protected String[] dirRightIndicationText = {"", "", "", "", "", ""};

    private static final String GAMEPAD_FUNCTION_PREFIX = "Function ";

    protected String prefLeftDirectionButtons = DIRECTION_BUTTON_LEFT_TEXT;
    protected String prefRightDirectionButtons = DIRECTION_BUTTON_RIGHT_TEXT;
    protected int prefDirectionButtonLongPressDelay = 1000;
    private boolean isDirectionButtonLongPress;
    Handler directionButtonLongPressHandler = new Handler();

    protected int prefStopButtonLongPressDelay = 1000;
    private boolean isStopButtonLongPress;
    Handler stopButtonLongPressHandler = new Handler();

    // The following are used for the shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private jmri.enginedriver.util.ShakeDetector shakeDetector;
    private String prefAccelerometerShake = acceleratorometer_action_type.NONE;
    private boolean accelerometerCurrent = false;

    //    protected static final String THEME_DEFAULT = "Default";
    private boolean isRestarting = false;

    private static final String PREF_KIDS_TIMER_NONE = "0";
    private static final String PREF_KIDS_TIMER_UNLIMITED = "777";
    private static final String PREF_KIDS_TIMER_ENDED = "999";
    private String prefKidsTimer = PREF_KIDS_TIMER_NONE;
    private String prefKidsTimerButtonDefault = "1";
    private int prefKidsTime = 0;  // in milliseconds
    private int kidsTimerRunning = kids_timer_action_type.DISABLED;
    private MyCountDownTimer kidsTimer;
    private String prefKidsTimerResetPassword = "9999";
    private String prefKidsTimerDemoModePassword = "8888";
    private String prefKidsTimerRestartPassword = "0000";
    private boolean prefKidsTimerEnableReverse = false;
    private boolean prefKidsTimerKioskMode = false;
    private String passwordText = "";

    // For ESU MobileControlII
    protected static final boolean IS_ESU_MCII = MobileControl2.isMobileControl2();
    private boolean isEsuMc2Stopped = false; // for tracking status of Stop button
    private boolean isEsuMc2AllStopped = false; // for tracking if all throttles stopped
    private ThrottleFragment esuThrottleFragment;
    private StopButtonFragment esuStopButtonFragment;
    private final EsuMc2LedControl esuMc2Led = new EsuMc2LedControl();
    private final Handler esuButtonRepeatUpdateHandler = new Handler();
    private boolean esuButtonAutoIncrement = false;
    private boolean esuButtonAutoDecrement = false;
    private boolean prefEsuMc2EndStopDirectionChange = true;
    private boolean prefEsuMc2StopButtonShortPress = true;
    private boolean isEsuMc2KnobEnabled = true;

    // Create default ESU MCII ThrottleScale for each throttle
    private final ThrottleScale[] esuThrottleScales
            = {new ThrottleScale(10, 127),
            new ThrottleScale(10, 127),
            new ThrottleScale(10, 127),
            new ThrottleScale(10, 127),
            new ThrottleScale(10, 127),
            new ThrottleScale(10, 127)};

    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();
    private static final String EXTERNAL_PREFERENCES_IMPORT_FILENAME = "auto_preferences.ed";
//    private static final String ENGINE_DRIVER_DIR = "Android/data/jmri.enginedriver/files";
    private static final String SERVER_ENGINE_DRIVER_DIR = "prefs/engine_driver";

    protected LinearLayout screenNameLine;
    protected Toolbar toolbar;
    protected LinearLayout statusLine;
    private int toolbarHeight;
    protected int systemStatusRowHeight = 0;
    protected int systemNavigationRowHeight = 0;
    protected int systemStatusRowHeightKeep = -1;
    protected int systemNavigationRowHeightKeep = -1;

    protected final ActivityResultLauncher<Intent> selectLocoActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(threaded_application.applicationName, activityName + ": selectLocoActivityLauncher callback received. ResultCode: " + result.getResultCode());

                int resultCode = result.getResultCode();
                if ( (resultCode == Activity.RESULT_OK) || (resultCode >= RESULT_FIRST_USER) )  {
                    Intent data = result.getData();
                    if (data != null) {
                        handleSelectLocoActivityResult(data, resultCode);
                    }
                }
            }
    );
    
    protected final ActivityResultLauncher<Intent> gamepadTestActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(threaded_application.applicationName, activityName + ": gamepadTestActivityLauncher callback received. ResultCode: " + result.getResultCode());

                int resultCode = result.getResultCode();
                if ( (resultCode == Activity.RESULT_OK) || (resultCode >= RESULT_FIRST_USER) )  {
                    Intent data = result.getData();
                    if (data != null) {
                        handleGamepadTestActivityResult(data, resultCode);
                    }
                }
            }
    );

    protected final ActivityResultLauncher<Intent> consistLightsEditActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(threaded_application.applicationName, activityName + ": consistLightsEditActivityLauncher callback received. ResultCode: " + result.getResultCode());

                int resultCode = result.getResultCode();
                if ( (resultCode == Activity.RESULT_OK) || (resultCode >= RESULT_FIRST_USER) )  {
                    Intent data = result.getData();
                    if (data != null) {
                        handleConsistLightsEditActivityResult();
                    }
                }
            }
    );

    protected final ActivityResultLauncher<Intent> consistEditActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(threaded_application.applicationName, activityName + ": consistEditActivityLauncher callback received. ResultCode: " + result.getResultCode());

                int resultCode = result.getResultCode();
                if ( (resultCode == Activity.RESULT_OK) || (resultCode >= RESULT_FIRST_USER) )  {
                    Intent data = result.getData();
                    if (data != null) {
                        handleConsistEditActivityResult(data, resultCode);
                    }
                }
            }
    );

    protected final ActivityResultLauncher<Intent> deviceSoundsSettingsActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(threaded_application.applicationName, activityName + ": deviceSoundsSettingsActivityLauncher callback received. ResultCode: " + result.getResultCode());

                int resultCode = result.getResultCode();
                if ( (resultCode == Activity.RESULT_OK) || (resultCode >= RESULT_FIRST_USER) )  {
                    handleDeviceSoundsSettingsActivityResult();
                }
            }
    );

    protected final ActivityResultLauncher<Intent> settingsActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(threaded_application.applicationName, activityName + ": settingsActivityLauncher callback received. ResultCode: " + result.getResultCode());

                int resultCode = result.getResultCode();
                if ( (resultCode == Activity.RESULT_OK) || (resultCode >= RESULT_FIRST_USER) )  {
                    handleSettingsActivityResult(resultCode);
                }
            }
    );

    // For speed slider speed buttons.
    protected class RptUpdater implements Runnable {
        int whichThrottle;
        int repeatDelay;

        protected RptUpdater(int WhichThrottle, int rptDelay) {
            whichThrottle = WhichThrottle;
            repeatDelay = rptDelay;

            if (repeatDelay != 0) {
                REP_DELAY = repeatDelay;
            } else {
                try {
                    REP_DELAY = Integer.parseInt(prefs.getString("prefSpeedButtonsRepeat", "100"));
                } catch (NumberFormatException ex) {
                    REP_DELAY = 100;
                }
            }
        }

        @Override
        public void run() {
//            Log.d(threaded_application.applicationName, activityName + ": RptUpdater: onProgressChanged - mAutoIncrement: " + mAutoIncrement + " mAutoDecrement: " + mAutoDecrement);
            if (mAutoIncrement[whichThrottle]) {
                incrementSpeed(whichThrottle, speed_commands_from_type.BUTTONS);
                repeatUpdateHandler.postDelayed(new RptUpdater(whichThrottle, REP_DELAY), REP_DELAY);
            } else if (mAutoDecrement[whichThrottle]) {
                decrementSpeed(whichThrottle, speed_commands_from_type.BUTTONS);
                repeatUpdateHandler.postDelayed(new RptUpdater(whichThrottle, REP_DELAY), REP_DELAY);
            }
        }
    }

    private class MyCountDownTimer extends CountDownTimer {

        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.KIDS_TIMER_START, "", 0, 0);
        }

        @Override
        public void onFinish() {  // When timer is finished
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.KIDS_TIMER_END, "", 0, 0);
        }

        @Override
        public void onTick(long millisUntilFinished) {   // millisUntilFinished    The amount of time until finished.
            int secondsLeft = (int) millisUntilFinished / 1000;
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.KIDS_TIMER_TICK, "", secondsLeft, 0);
        }
    }

    @SuppressLint("ApplySharedPref")
    protected void kidsTimerActions(int action, int arg) {
        if ((kidsTimerRunning == kids_timer_action_type.DISABLED)
                && (action != kids_timer_action_type.ENABLED) ) return;

        switch (action) {
            case kids_timer_action_type.DISABLED:
                if (arg == 0) { // not onResume
//                    speedUpdateAndNotify(0);
                    if (kidsTimer != null) kidsTimer.cancel();
                    kidsTimerRunning = kids_timer_action_type.DISABLED;
                    for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                        enableDisableButtons(throttleIndex, false);
                        bSelects[throttleIndex].setEnabled(true);
                        enableDisableButtons(throttleIndex, false);
                    }
                    mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                            getApplicationContext().getResources().getString(R.string.app_name),
                            getApplicationContext().getResources().getString(R.string.app_name_throttle),
                            "");
                    if (overflowMenu != null) {
                        mainapp.setKidsMenuOptions(overflowMenu, true, 0);
                    }
                    mainapp.hideSoftKeyboard(this.getCurrentFocus());
                }
                stopAppScreenPinning();
                break;

            case kids_timer_action_type.DEMO:
                if (arg == 0) { // not onResume
                    mainapp.safeToast(R.string.toastKidsTimerDemoMode, Toast.LENGTH_SHORT);
                    if (kidsTimer != null) kidsTimer.cancel();
                    kidsTimerRunning = kids_timer_action_type.DEMO;
                    for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                        enableDisableButtons(throttleIndex, false);
                        bSelects[throttleIndex].setEnabled(true);
                        enableDisableButtons(throttleIndex, false);
                    }
                    mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                            getApplicationContext().getResources().getString(R.string.app_name),
                            getApplicationContext().getResources().getString(R.string.app_name_throttle),
                            "");
                    if (overflowMenu != null) {
                        mainapp.setKidsMenuOptions(overflowMenu, true, 0);
                    }
                    mainapp.hideSoftKeyboard(this.getCurrentFocus());
                }
                break;

            case kids_timer_action_type.ENABLED:
                if (kidsTimerRunning == kids_timer_action_type.RUNNING) {  // reset the timer if it is already running
                    speedUpdateAndNotify(0);
                    if (kidsTimer != null) kidsTimer.cancel();
                }
//                if (((prefKidsTime>0) && (kidsTimerRunning!=KIDS_TIMER_RUNNING))) {
                kidsTimerRunning = kids_timer_action_type.ENABLED;
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    enableDisableButtons(throttleIndex, false);
                    bSelects[throttleIndex].setEnabled(false);
                    if (!prefKidsTimerEnableReverse) bReverses[throttleIndex].setEnabled(false);
                }
                mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                        getApplicationContext().getResources().getString(R.string.app_name_throttle_kids_enabled),
                        getApplicationContext().getResources().getString(R.string.prefKidsTimerTitle),
                        "");
                if (overflowMenu != null) {
                    mainapp.setKidsMenuOptions(overflowMenu, false, 0);
                }
//                }
                mainapp.hideSoftKeyboard(this.getCurrentFocus());
                break;

            case kids_timer_action_type.STARTED:
                if ((prefKidsTime > 0) && (kidsTimerRunning != kids_timer_action_type.RUNNING)) {
                    kidsTimerRunning = kids_timer_action_type.RUNNING;
                    kidsTimer = new MyCountDownTimer(prefKidsTime, 1000);
                    kidsTimer.start();
                    startAppScreenPinning();
                }
                break;

            case kids_timer_action_type.ENDED:
                speedUpdateAndNotify(0);
                kidsTimerRunning = kids_timer_action_type.ENDED;
                if (kidsTimer != null) kidsTimer.cancel();
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    enableDisableButtons(throttleIndex, true);
                    bSelects[throttleIndex].setEnabled(false);
                    if (!prefKidsTimerEnableReverse) bReverses[throttleIndex].setEnabled(false);
                }
                prefs.edit().putString("prefKidsTimer", PREF_KIDS_TIMER_ENDED).commit();  //reset the preference
                mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                        getApplicationContext().getResources().getString(R.string.app_name_throttle_kids_ended),
                        getApplicationContext().getResources().getString(R.string.prefKidsTimerTitle),
                        "");
                break;
            case kids_timer_action_type.RUNNING:
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    bSelects[throttleIndex].setEnabled(false);
                    if (!prefKidsTimerEnableReverse) bReverses[throttleIndex].setEnabled(false);
                }
                if (!prefKidsTimer.equals(PREF_KIDS_TIMER_UNLIMITED)) {
                    mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                            getApplicationContext().getResources().getString(R.string.app_name_throttle_kids_running).replace("%1$s", Integer.toString(arg)),
                            getApplicationContext().getResources().getString(R.string.prefKidsTimerTitle),
                            "");
                } else {
                    mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                            getApplicationContext().getResources().getString(R.string.app_name_throttle_kids_unlimited),
                            getApplicationContext().getResources().getString(R.string.prefKidsTimerTitle),
                            "");
                }
                break;
        }
    }

    // Handle messages from the communication thread TO this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    private class ThrottleMessageHandler extends Handler {

        public ThrottleMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String response_str = msg.obj.toString();

            threaded_application.extendedLogging(activityName + ": handleMessage() " + response_str );

            switch (msg.what) {
                case message_type.RESPONSE: { // handle messages from WiThrottle server
                    if (response_str.length() < 2)
                        break;  //bail if too short, to avoid crash
                    char com0 = response_str.charAt(0);
                    char com1 = response_str.charAt(1);
                    int whichThrottle;

                    switch (com0) {
                        // various MultiThrottle messages
                        case 'M':  // multi-throttle
                            if (response_str.length() < 3)
                                break;  //bail if too short, to avoid crash
                            whichThrottle = mainapp.throttleCharToInt(com1); // '0', '1'',2' etc.
                            char com2 = response_str.charAt(2);
                            String[] ls = threaded_application.splitByString(response_str, "<;>");
                            String addr;
                            try {
                                addr = ls[0].substring(3);
                            } catch (Exception e) {
                                addr = "";
                            }
                            if ((whichThrottle >= 0) && (whichThrottle < mainapp.prefNumThrottles)) {
                                if (com2 == '+' || com2 == 'L') { // if loco added or function labels updated
                                    if (com2 == ('+')) {
                                        enableDisableButtons(whichThrottle); // direction and slider: pass whichThrottle
                                        showHideConsistMenus();
                                    }
                                    // loop through all function buttons and set label and dcc functions (based on settings) or hide if no label
                                    set_function_labels_and_listeners_for_view(whichThrottle);
                                    enableDisableButtonsForView(functionButtonViewGroups[whichThrottle], true);
                                    soundsShowHideDeviceSoundsButton(whichThrottle);
                                    showHideSpeedLimitAndPauseButtons(whichThrottle);
                                    setLabels();
                                } else if (com2 == '-') { // if loco removed
                                    removeLoco(whichThrottle);
                                    swapToNextAvailableThrottleForGamePad(whichThrottle, true); // see if we can/need to move the gamepad to another throttle
                                    mainapp.gamePadIdsAssignedToThrottles[whichThrottle] = 0;
                                    mainapp.gamePadThrottleAssignment[whichThrottle] = -1;

                                } else if (com2 == 'A') { // Action e.g. MTAL2608<;>R1
                                    char com3 = ' ';
                                    if (ls.length >= 2) { //make sure there's a value to parse
                                        com3 = ls[1].charAt(0);
                                    }
                                    if (com3 == 'R') { // set direction
                                        int dir;
                                        try {
                                            dir = Integer.parseInt(ls[1].substring(1, 2));
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
                                                // needed for the switching throttle layouts
                                                speedUpdate(whichThrottle,
                                                        getSpeedFromCurrentSliderPosition(whichThrottle, false));
                                            }
                                        } else {
                                            int locoDir = curDir;               // calc correct direction for this (non-lead) loco
                                            try {
                                                if (con.isReverseOfLead(addr))
                                                    locoDir ^= 1;
                                                if (locoDir != dir) {// if loco has wrong direction then correct it
                                                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DIRECTION, addr, whichThrottle, locoDir);
                                                }
                                            } catch (
                                                    Exception e) {     // isReverseOfLead returns null if addr is not in con
                                                // - should not happen unless WiT is reporting on engine user just dropped from ED consist?
                                                threaded_application.extendedLogging(activityName + ": " + whichThrottle + " loco " + addr + " direction reported by WiT but engine is not assigned");
                                            }
                                        }
                                    } else if (com3 == 'V') { // set speed
                                        try {
                                            int speedWiT = Integer.parseInt(ls[1].substring(1));
                                            speedUpdateWiT(whichThrottle, speedWiT); // update speed slider and indicator
                                        } catch (Exception ignored) {
                                        }
                                    } else if (com3 == 'F') { // function key
                                        try {
                                            int function = Integer.parseInt(ls[1].substring(2));
                                            int action = Integer.parseInt(ls[1].substring(1, 2));
//                                            doFunctionSound(whichThrottle, function);

                                            String loco = ls[0].substring(3);
                                            Consist con = mainapp.consists[whichThrottle];
                                            if ((mainapp.prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.ORIGINAL))
                                                    || (loco.equals(con.getLeadAddr()))) { //if using the 'complex' follow function rules, only send it to the lead loco
                                                set_function_state(whichThrottle, function);
                                            }

                                            //ipls equivalents
                                            if (((function >= 1) && function <= 2)
                                                    && (!mainapp.prefDeviceSounds[whichThrottle].equals("none"))
                                                    && (mainapp.prefDeviceSoundsF1F2ActivateBellHorn)) {
                                                if (function == 1) {
                                                    if (action == 0) { // up
                                                        if (bSoundsExtras[sounds_type.BUTTON_BELL][whichThrottle].isSelected()) {
                                                            handleDeviceButtonAction(whichThrottle, sounds_type.BUTTON_BELL, sounds_type.BELL,
                                                                    MotionEvent.ACTION_UP);
                                                        } // else do nothing
                                                    } else { // down
                                                        if (!bSoundsExtras[sounds_type.BUTTON_BELL][whichThrottle].isSelected()) {
                                                            handleDeviceButtonAction(whichThrottle, sounds_type.BUTTON_BELL, sounds_type.BELL,
                                                                    MotionEvent.ACTION_DOWN);
                                                        } // else do nothing
                                                    }

                                                } else {
                                                    if (action == 0) {
                                                        if (bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle].isSelected()) {
                                                            handleDeviceButtonAction(whichThrottle, sounds_type.BUTTON_HORN, sounds_type.HORN,
                                                                    MotionEvent.ACTION_UP);
                                                        } // else do nothing
                                                    } else { // down
                                                        if (!bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle].isSelected()) {
                                                            handleDeviceButtonAction(whichThrottle, sounds_type.BUTTON_HORN, sounds_type.HORN,
                                                                    MotionEvent.ACTION_DOWN);
                                                        } // else do nothing
                                                    }
                                                }
                                            }
                                        } catch (Exception ignored) {
                                        }
                                    } else if (com3 == 's') { // set speed step
                                        try {
                                            int speedStepCode = Integer.parseInt(ls[1].substring(1));
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
                            enableDisableButtons(com0); // pass whichThrottle
                            setLabels();
                            break;
                        case 'P': // panel info
                            if (com1 == 'W') { // PW - web server port info
                                initWeb();
                                setLabels();
                            } else if (com1 == 'P') { // PP - power state change
                                setLabels();
                            }
                            break;
                    } // end of switch

                    if (!selectLocoRendered) // call set_labels if the select loco textViews had not rendered the last time it was called
                        setLabels();
                }
                break;

                case message_type.ESTOP_PAUSED:
                case message_type.ESTOP_RESUMED:
                    mainapp.setEmergencyStopStateActionViewButton(overflowMenu, findViewById(R.id.emergency_stop_button));
                    break;

                case message_type.REFRESH_OVERFLOW_MENU:
                    refreshOverflowMenu();
                    break;

                case message_type.REQUEST_REFRESH_THROTTLE:
                    setLabels();
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleMessageHandler(): REQUEST_REFRESH_THROTTLE");
                    break;

                case message_type.REFRESH_FUNCTIONS:
                    setAllFunctionLabelsAndListeners();
                    for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                        setAllFunctionStates(throttleIndex);
                        enableDisableButtons(throttleIndex); // direction and slider: pass whichThrottle
                        showHideConsistMenus();
                    }
                    break;
                case message_type.ROSTER_UPDATE:
                    setLabels();               // refresh function labels when any roster response is received
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(response_str);
                    break;
                case message_type.WIT_CON_RECONNECT:
                    break;
                case message_type.TIME_CHANGED:
                    setActivityTitle();
                    break;
                case message_type.FORCE_THROTTLE_RELOAD:
                    try {
                        int whichThrottle = Integer.parseInt(response_str);
                        removeLoco(whichThrottle);
                        setAllFunctionLabelsAndListeners();
                        setAllFunctionStates(whichThrottle);
                        enableDisableButtons(whichThrottle); // direction and slider: pass whichThrottle
                        showHideConsistMenus();
                    } catch (Exception e) {
                        // do nothing
                    }
                    break;
                case message_type.RESTART_APP:
                    startNewThrottleActivity();
                    break;
                case message_type.RELAUNCH_APP:
                case message_type.SHUTDOWN:
                    shutdown();
                    break;
                case message_type.DISCONNECT:
                    disconnect();
                    break;
                case message_type.WEBVIEW_LOC:      // webview location changed
                    // set new location
                    prefWebViewLocation = prefs
                            .getString("prefWebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
                    keepWebViewLocation = prefWebViewLocation;
                    webViewIsOn = false;
                    reloadWeb();
                    break;
                case message_type.INITIAL_THR_WEBPAGE:
                    initWeb();
                    break;
                case message_type.INITIAL_WEB_WEBPAGE:
                    // if web activity is not open then reinit web statics for next time it is opened
                    if (mainapp.web_msg_handler == null) {
                        web_activity.initStatics();
                    }
                    break;
                case message_type.REQ_STEAL:
                    promptForSteal(msg.obj.toString(), msg.arg1);
                    break;
                case message_type.KIDS_TIMER_ENABLE:
                    kidsTimerActions(kids_timer_action_type.ENABLED, 0);
                    break;
                case message_type.KIDS_TIMER_START:
                    kidsTimerActions(kids_timer_action_type.STARTED, 0);
                    break;
                case message_type.KIDS_TIMER_TICK:
                    kidsTimerActions(kids_timer_action_type.RUNNING, msg.arg1);
                    break;
                case message_type.KIDS_TIMER_END:
                    kidsTimerActions(kids_timer_action_type.ENDED, 0);
                    break;
                case message_type.IMPORT_SERVER_AUTO_AVAILABLE:
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleMessageHandler(): AUTO_IMPORT_URL_AVAILABLE " + response_str);
                    autoImportUrlAskToImport();
                    break;
                case message_type.SOUNDS_FORCE_LOCO_SOUNDS_TO_START:
                    for (int throttleIndex = 0; (throttleIndex < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES) && (throttleIndex < mainapp.maxThrottlesCurrentScreen); throttleIndex++) {
                        if(ipls!=null) ipls.doLocoSound(throttleIndex, getSpeedFromCurrentSliderPosition(throttleIndex, false), dirs[throttleIndex], soundsIsMuted[throttleIndex]);
                    }
                    break;
                case message_type.GAMEPAD_ACTION:
                    threaded_application.extendedLogging(activityName + ": ThrottleMessageHandler(): GAMEPAD_ACTION " + response_str);
                    if (!response_str.isEmpty()) {
                        String[] splitString = response_str.split(":");
                        externalGamepadAction = Integer.parseInt(splitString[0]);
//                        externalGamepadWhichThrottle = Integer.parseInt(splitString[1]);
                        externalGamepadKeyCode = Integer.parseInt(splitString[1]);
                        externalGamepadIsShiftPressed = (Integer.parseInt(splitString[2]) == 1);
                        externalGamepadRepeatCnt = Integer.parseInt(splitString[3]);
                        externalGamepadWhichGamePadIsEventFrom = Integer.parseInt(splitString[4]);

                        dispatchKeyEvent(null);
                    }
                    break;
                case message_type.VOLUME_BUTTON_ACTION: // volume button n another activity
                    threaded_application.extendedLogging(activityName + ": handleMessage(): VOLUME_BUTTON_ACTION " + response_str);
                    if (!response_str.isEmpty()) {
                        String[] splitString = response_str.split(":");
                        doVolumeButtonAction(Integer.parseInt(splitString[0]), Integer.parseInt(splitString[1]), Integer.parseInt(splitString[2]));
                    }
                    break;

                case message_type.GAMEPAD_JOYSTICK_ACTION:
                    threaded_application.extendedLogging(activityName + ": handleMessage(): GAMEPAD_JOYSTICK_ACTION " + response_str);
                    if (!response_str.isEmpty()) {
                        String[] splitString = response_str.split(":");
                        externalGamepadAction = Integer.parseInt(splitString[0]);
                        externalGamepadWhichGamePadIsEventFrom = Integer.parseInt(splitString[1]);
                        externalGamepadXAxis = Float.parseFloat(splitString[2]);
                        externalGamepadYAxis = Float.parseFloat(splitString[3]);
                        externalGamepadXAxis2 = Float.parseFloat(splitString[4]);
                        externalGamepadYAxis2 = Float.parseFloat(splitString[5]);

                        dispatchGenericMotionEvent(null);
                    }
                    break;

                case message_type.ESTOP: // only needed for the Semi-Realistic Throttle
                    if (isSemiRealisticThrottle) {
                        int whichThrottle = Integer.parseInt(response_str);
                        semiRealisticThrottleSliderPositionUpdate(whichThrottle,0);
                        setTargetSpeed(whichThrottle, 0);
                    }
                    break;

                case message_type.ESTOP_ONE_THROTTLE:
                    int whichThrottle = mainapp.throttleCharToInt(response_str.charAt(0));
                    setSpeed(whichThrottle, 0, speed_commands_from_type.BUTTONS);
                    break;

                case message_type.WEB_PORT_RECEIVED:
                    if (prefs.getBoolean("prefImportServerAuto", getApplicationContext().getResources().getBoolean(R.bool.prefImportServerAutoDefaultValue))) {
                        new AutoImportFromURL();
                    }
                    break;

                case message_type.REOPEN_THROTTLE:
                    // ignore
                    break;

                case message_type.TERMINATE_ALL_ACTIVITIES_BAR_CONNECTION:
                    endThisActivity();
                    break;

            }
        }
    }

    private void startNewThrottleActivity() {
        webView.stopLoading();
        // remove old handlers since the new Intent will have its own
        isRestarting = true;        // tell OnDestroy to skip removing handlers since it will run after the new Intent is created
        removeHandlers();

        threaded_application.activityInTransition(activityName);
        //end current throttle Intent then start the new Intent
        Intent newThrottle = mainapp.getThrottleIntent();
        this.finish();
        startActivity(newThrottle);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    // Change the screen brightness
    public void setScreenBrightness(int brightnessValue) {
        Context mContext;
        mContext = getApplicationContext();

        // Make sure brightness value between 0 to 255
        if (brightnessValue >= 0 && brightnessValue <= 255) {

//            if (mainapp.androidVersion >= mainapp.minScreenDimNewMethodVersion) {

                setScreenBrightnessMode(SCREEN_BRIGHTNESS_MODE_MANUAL);

                if (!PermissionsHelper.getInstance().isPermissionGranted(throttle.this, PermissionsHelper.WRITE_SETTINGS)) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        PermissionsHelper.getInstance().requestNecessaryPermissions(throttle.this, PermissionsHelper.WRITE_SETTINGS);
                    }
                }
                try {
                    Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessValue);
                    Log.d(threaded_application.applicationName, activityName + ": setScreenBrightness(): screen brightness successfully changed to " + brightnessValue);
                } catch (Exception e) {
                    Log.e(threaded_application.applicationName, activityName + ": setScreenBrightness(): screen brightness was NOT changed to " + brightnessValue);
                }
            } else {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = ((float) brightnessValue) / 255;
                getWindow().setAttributes(lp);
//            }
        }
    }

    // Get the screen current brightness
    protected int getScreenBrightness() {
        Context mContext;
        mContext = getApplicationContext();

        return Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                0);
    }

    public void setScreenBrightnessMode(int brightnessModeValue) {
        Context mContext;
        mContext = getApplicationContext();

//        if (mainapp.androidVersion >= mainapp.minScreenDimNewMethodVersion) {
            if (brightnessModeValue >= 0 && brightnessModeValue <= 1) {
                if (!PermissionsHelper.getInstance().isPermissionGranted(throttle.this, PermissionsHelper.WRITE_SETTINGS)) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        PermissionsHelper.getInstance().requestNecessaryPermissions(throttle.this, PermissionsHelper.WRITE_SETTINGS);
                    }
                }
                try {
                    Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessModeValue);
                } catch (Exception e) {
                    mainapp.safeToast(R.string.toastUnableToSetBrightness, Toast.LENGTH_SHORT);
                }
            }
//        }
    }

    protected int getScreenBrightnessMode() {
        Context mContext;
        mContext = getApplicationContext();
        return Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    0
            );
    }

    protected void setImmersiveMode(View webView) {
        if (prefThrottleViewImmersiveMode) {
            setImmersiveModeOn(webView, !immersiveModeTempIsOn);
        } else {
            if (immersiveModeTempIsOn) {
                setImmersiveModeOn(webView, true);
            } else {
                setImmersiveModeOff(webView, false);
            }
        }
    }

    protected void setImmersiveModeOn(View webView, boolean forceOn) {

        if ((prefThrottleViewImmersiveMode) || (forceOn)) {   // if the preference is set use Immersive mode

            if (webView!=null) {
                webView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                webView.postInvalidate();
            }

            View windowView = getWindow().getDecorView();
            windowView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

            if (prefThrottleViewImmersiveModeHideToolbar) {
                screenNameLine.setVisibility(View.GONE);
                toolbar.setVisibility(View.GONE);
                statusLine.setVisibility(View.GONE);
            }

            immersiveModeIsOn = true;

            vThrottleScreenWrap.setPadding(0,0,0,0);
            systemStatusRowHeight = 0;
            systemNavigationRowHeight = 0;

            windowView.postInvalidate();
        }
    }

    protected void setImmersiveModeOff(View webView, boolean forceOff) {

        if ((!prefThrottleViewImmersiveMode) || (forceOff)) {   // if the preference is set to not use Immersive mode

            if (webView!=null) {
                webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                webView.postInvalidate();
            }
            View windowView = getWindow().getDecorView();
            windowView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            screenNameLine.setVisibility(VISIBLE);
            toolbar.setVisibility(VISIBLE);
            statusLine.setVisibility(VISIBLE);

            getStatusBarAndNavigationBarHeights();

            vThrottleScreenWrap.setPadding(0,systemStatusRowHeightKeep,0,systemNavigationRowHeightKeep);
            systemStatusRowHeight = systemStatusRowHeightKeep;
            systemNavigationRowHeight = systemNavigationRowHeightKeep;

            immersiveModeIsOn = false;

            windowView.postInvalidate();
        }
    }

    protected boolean directionButtonsAreCurrentlyReversed(int throttleIndexNo) {
        return ((!prefSwapForwardReverseButtons) && (currentSwapForwardReverseButtons[throttleIndexNo]))
                || (((prefSwapForwardReverseButtons) && (!currentSwapForwardReverseButtons[throttleIndexNo])));
    }

    private boolean gamepadDirectionButtonsAreCurrentlyReversed(int throttleIndexNo) {
        return (prefGamepadSwapForwardReverseWithScreenButtons)
                && (((currentSwapForwardReverseButtons[throttleIndexNo]) && (!prefSwapForwardReverseButtons))
                || ((!currentSwapForwardReverseButtons[throttleIndexNo]) && (prefSwapForwardReverseButtons)));
    }

    // set or restore the screen brightness when used for the Swipe Up or Shake
    private void setRestoreScreenDim(String toastMsg) {
        if (screenDimmed) {
            screenDimmed = false;
            setScreenBrightness(screenBrightnessOriginal);
            setScreenBrightnessMode(screenBrightnessModeOriginal);
        } else {
            screenDimmed = true;
            mainapp.safeToast(toastMsg, Toast.LENGTH_SHORT);
            screenBrightnessOriginal = getScreenBrightness();
            setScreenBrightness(screenBrightnessDim);
        }
    }

    // set or restore the screen brightness and lock or unlock the screen when used for the Swipe Up or Shake
    private void setRestoreScreenLockDim(String toastMsg) {
        if (isScreenLocked) {
            isScreenLocked = false;
            mainapp.safeToast(R.string.toastThrottleScreenUnlocked, Toast.LENGTH_SHORT);
            setScreenBrightness(screenBrightnessOriginal);
            setScreenBrightnessMode(screenBrightnessModeOriginal);
            setImmersiveMode(webView);
        } else {
            isScreenLocked = true;
            mainapp.safeToast(toastMsg, Toast.LENGTH_SHORT);
            screenBrightnessOriginal = getScreenBrightness();
            setScreenBrightness(screenBrightnessDim);
            setImmersiveMode(webView);
        }
    }

    private void setupSensor() {
        if (!prefAccelerometerShake.equals(acceleratorometer_action_type.NONE)) {
            // ShakeDetector initialization
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                shakeDetector = new ShakeDetector(getApplicationContext());
                shakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

                    @Override
                    public void onShake(int count) {

                        switch (prefAccelerometerShake) {
                            case acceleratorometer_action_type.WEB_VIEW:
                                if ((prefWebViewLocation.equals(web_view_location_type.NONE)) && (keepWebViewLocation.equals(web_view_location_type.NONE))) {
                                    GamepadFeedbackSound(true);
                                    mainapp.safeToast(R.string.toastShakeWebViewUnavailable, Toast.LENGTH_SHORT);
                                } else {
                                    GamepadFeedbackSound(false);
                                    showHideWebView(getApplicationContext().getResources().getString(R.string.toastShakeWebViewHidden));
                                }
                                break;
                            case acceleratorometer_action_type.NEXT_V:
                                GamepadFeedbackSound(false);
                                setNextActiveThrottle();
                                tts.speakWords(tts_msg_type.VOLUME_THROTTLE, whichVolume, false
                                        , getDisplaySpeedFromCurrentSliderPosition(whichVolume, true)
                                        , 0
                                        , 0
                                        , getConsistAddressString(whichVolume));
                                break;
                            case acceleratorometer_action_type.LOCK_DIM_SCREEN:
                                GamepadFeedbackSound(false);
                                setRestoreScreenLockDim(getApplicationContext().getResources().getString(R.string.toastShakeScreenLocked));
                                break;
                            case acceleratorometer_action_type.DIM_SCREEN:
                                GamepadFeedbackSound(false);
                                setRestoreScreenDim(getApplicationContext().getResources().getString(R.string.toastShakeScreenDimmed));
                                break;
                            case acceleratorometer_action_type.ALL_STOP:
                                GamepadFeedbackSound(false);
                                speedUpdateAndNotify(0);         // update all throttles
                            case acceleratorometer_action_type.E_STOP:
                                GamepadFeedbackSound(false);
                                mainapp.sendEStopMsg();
                                speedUpdate(0);  // update all throttles
                                applySpeedRelatedOptions();  // update all throttles
                                if (IS_ESU_MCII) {
                                    Log.d(threaded_application.applicationName, activityName + ": setupSensor(): ESU_MCII: Move knob request for EStop");
                                    setEsuThrottleKnobPosition(whichVolume, 0);
                                }
                        }
                    }
                });
                accelerometerCurrent = true;
            } else {
                mainapp.safeToast(R.string.toastAccelerometerNotFound, Toast.LENGTH_LONG);
            }
        }
    }

    protected void addConsistFollowRule(String consistFollowString, String consistFollowAction, Integer consistFollowHeadlight) {
        if (!consistFollowString.trim().isEmpty()) {
            String[] prefConsistFollowStringsTemp = threaded_application.splitByString(consistFollowString, ",");
            for (int i = 0; i < prefConsistFollowStringsTemp.length; i++) {
                prefConsistFollowStrings.add(prefConsistFollowStringsTemp[i].trim());
                prefConsistFollowActions.add(consistFollowAction);
                prefConsistFollowHeadlights.add(consistFollowHeadlight);
            }
        }
    }

    protected void getKidsTimerPrefs() {
        prefKidsTimer = prefs.getString("prefKidsTimer", getResources().getString(R.string.prefKidsTimerDefaultValue));
        if ((!prefKidsTimer.equals(PREF_KIDS_TIMER_NONE)) && (!prefKidsTimer.equals(PREF_KIDS_TIMER_ENDED))) {
            if (!prefKidsTimer.equals(PREF_KIDS_TIMER_UNLIMITED)) {
                prefKidsTime = Integer.parseInt(prefKidsTimer) * 60000;
            } else {
                prefKidsTime = 14400 * 60000;
            }
        } else {
            prefKidsTime = 0;
        }
        prefKidsTimerResetPassword = prefs.getString("prefKidsTimerResetPassword", getResources().getString(R.string.prefKidsTimerResetPasswordDefaultValue));
        prefKidsTimerDemoModePassword = prefs.getString("prefKidsTimerDemoModePassword", getResources().getString(R.string.prefKidsTimerDemoModePasswordDefaultValue));
        prefKidsTimerRestartPassword = prefs.getString("prefKidsTimerRestartPassword", getResources().getString(R.string.prefKidsTimerRestartPasswordDefaultValue));
        prefKidsTimerEnableReverse = prefs.getBoolean("prefKidsTimerEnableReverse", getResources().getBoolean(R.bool.prefKidsTimerEnableReverseDefaultValue));
        prefKidsTimerKioskMode = prefs.getBoolean("prefKidsTimerKioskMode", getResources().getBoolean(R.bool.prefKidsTimerKioskModeDefaultValue));
        prefKidsTimerButtonDefault = prefs.getString("prefKidsTimerButtonDefault", getResources().getString(R.string.prefKidsTimerButtonDefaultDefaultValue));
    }

    // get all the preferences that should be read when the activity is created or resumes
    @SuppressLint("ApplySharedPref")
    protected void getCommonPrefs(boolean isCreate) {

        if (isCreate) {  //only do onCreate
            prefWebViewLocation = prefs.getString("prefWebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
        }
        isSemiRealisticThrottle = false;

        prefDirectionButtonLongPressDelay = threaded_application.getIntPrefValue(prefs, "prefDirectionButtonLongPressDelay", getApplicationContext().getResources().getString(R.string.prefDirectionButtonLongPressDelayDefaultValue));
        prefStopButtonLongPressDelay = threaded_application.getIntPrefValue(prefs, "prefStopButtonLongPressDelay", getApplicationContext().getResources().getString(R.string.prefStopButtonLongPressDelayDefaultValue));

        FUNCTION_BUTTON_LOOK_FOR_WHISTLE = getApplicationContext().getResources().getString(R.string.functionButtonLookForWhistle).trim();
        FUNCTION_BUTTON_LOOK_FOR_HORN = getApplicationContext().getResources().getString(R.string.functionButtonLookForHorn).trim();
        FUNCTION_BUTTON_LOOK_FOR_BELL = getApplicationContext().getResources().getString(R.string.functionButtonLookForBell).trim();
        FUNCTION_BUTTON_LOOK_FOR_HEAD = getApplicationContext().getResources().getString(R.string.functionButtonLookForHead).trim();
        FUNCTION_BUTTON_LOOK_FOR_LIGHT = getApplicationContext().getResources().getString(R.string.functionButtonLookForLight).trim();
        FUNCTION_BUTTON_LOOK_FOR_REAR = getApplicationContext().getResources().getString(R.string.functionButtonLookForRear).trim();

        prefConsistFollowDefaultAction = prefs.getString("prefConsistFollowDefaultAction", getApplicationContext().getResources().getString(R.string.prefConsistFollowDefaultActionDefaultValue));
        prefConsistFollowStrings = new ArrayList<>();
        prefConsistFollowActions = new ArrayList<>();
        prefConsistFollowHeadlights = new ArrayList<>();

        mainapp.prefConsistFollowRuleStyle = prefs.getString("prefConsistFollowRuleStyle", getApplicationContext().getResources().getString(R.string.prefConsistFollowRuleStyleDefaultValue));

        String prefConsistFollowStringTemp = prefs.getString("prefConsistFollowString1", getApplicationContext().getResources().getString(R.string.prefConsistFollowString1DefaultValue));
        String prefConsistFollowActionTemp = prefs.getString("prefConsistFollowAction1", getApplicationContext().getResources().getString(R.string.prefConsistFollowString1DefaultValue));
        addConsistFollowRule(prefConsistFollowStringTemp, prefConsistFollowActionTemp, consist_function_is_type.HEADLIGHT);

        prefConsistFollowStringTemp = prefs.getString("prefConsistFollowString2", getApplicationContext().getResources().getString(R.string.prefConsistFollowString2DefaultValue));
        prefConsistFollowActionTemp = prefs.getString("prefConsistFollowAction2", getApplicationContext().getResources().getString(R.string.prefConsistFollowString2DefaultValue));
        addConsistFollowRule(prefConsistFollowStringTemp, prefConsistFollowActionTemp, consist_function_is_type.NOT_HEADLIGHT);

        prefConsistFollowStringTemp = prefs.getString("prefConsistFollowString3", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        prefConsistFollowActionTemp = prefs.getString("prefConsistFollowAction3", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        addConsistFollowRule(prefConsistFollowStringTemp, prefConsistFollowActionTemp, consist_function_is_type.NOT_HEADLIGHT);

        prefConsistFollowStringTemp = prefs.getString("prefConsistFollowString4", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        prefConsistFollowActionTemp = prefs.getString("prefConsistFollowAction4", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        addConsistFollowRule(prefConsistFollowStringTemp, prefConsistFollowActionTemp, consist_function_is_type.NOT_HEADLIGHT);

        prefConsistFollowStringTemp = prefs.getString("prefConsistFollowString5", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        prefConsistFollowActionTemp = prefs.getString("prefConsistFollowAction5", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        addConsistFollowRule(prefConsistFollowStringTemp, prefConsistFollowActionTemp, consist_function_is_type.NOT_HEADLIGHT);

        // increase height of throttle slider (if requested in preferences)
        prefIncreaseSliderHeight = prefs.getBoolean("prefIncreaseSliderHeight", getResources().getBoolean(R.bool.prefIncreaseSliderHeightDefaultValue));

        // decrease height of Loco Id (if requested in preferences)
        prefDecreaseLocoNumberHeight = prefs.getBoolean("prefDecreaseLocoNumberHeight", getResources().getBoolean(R.bool.prefDecreaseLocoNumberHeightDefaultValue));

        // increase the web view height if the preference is set
        prefIncreaseWebViewSize = prefs.getBoolean("prefIncreaseWebViewSize", getResources().getBoolean(R.bool.prefIncreaseWebViewSizeDefaultValue));
        mainapp.defaultWebViewURL = prefs.getString("prefInitialWebPage",
                getApplicationContext().getResources().getString(R.string.prefInitialWebPageDefaultValue));
        mainapp.defaultThrottleWebViewURL = prefs.getString("prefInitialThrottleWebPage",
                getApplicationContext().getResources().getString(R.string.prefInitialThrottleWebPageDefaultValue));

        prefThrottleViewImmersiveMode = prefs.getBoolean("prefThrottleViewImmersiveMode",
                getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeDefaultValue));
        prefThrottleViewImmersiveModeHideToolbar = prefs.getBoolean("prefThrottleViewImmersiveModeHideToolbar",
                getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeHideToolbarDefaultValue));

        prefShowAddressInsteadOfName = prefs.getBoolean("prefShowAddressInsteadOfName", getResources().getBoolean(R.bool.prefShowAddressInsteadOfNameDefaultValue));

        prefSwipeDownOption = prefs.getString("prefSwipeDownOption", getApplicationContext().getResources().getString(R.string.prefSwipeUpOptionDefaultValue));
        prefSwipeUpOption = prefs.getString("prefSwipeUpOption", getApplicationContext().getResources().getString(R.string.prefSwipeUpOptionDefaultValue));
        isScreenLocked = false;

        dirChangeWhileMoving = prefs.getBoolean("prefDirChangeWhileMoving", getResources().getBoolean(R.bool.prefDirChangeWhileMovingDefaultValue));
        stopOnDirectionChange = prefs.getBoolean("prefStopOnDirectionChange", getResources().getBoolean(R.bool.prefStopOnDirectionChangeDefaultValue));
        locoSelectWhileMoving = prefs.getBoolean("prefSelectLocoWhileMoving", getResources().getBoolean(R.bool.prefSelectLocoWhileMovingDefaultValue));

        screenBrightnessDim = Integer.parseInt(prefs.getString("prefScreenBrightnessDim", getResources().getString(R.string.prefScreenBrightnessDimDefaultValue))) * 255 / 100;

        prefConsistLightsLongClick = prefs.getBoolean("prefConsistLightsLongClick", getResources().getBoolean(R.bool.prefConsistLightsLongClickDefaultValue));

        prefGamepadTestEnforceTesting = prefs.getBoolean("prefGamepadTestEnforceTesting", getResources().getBoolean(R.bool.prefGamepadTestEnforceTestingDefaultValue));
        mainapp.prefGamePadFeedbackVolume = threaded_application.getIntPrefValue(prefs, "prefGamePadFeedbackVolume",
                getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue));

        prefDisableVolumeKeys = prefs.getBoolean("prefDisableVolumeKeys", getResources().getBoolean(R.bool.prefDisableVolumeKeysDefaultValue));

        prefSelectedLocoIndicator = prefs.getString("prefSelectedLocoIndicator", getResources().getString(R.string.prefSelectedLocoIndicatorDefaultValue));

        prefVolumeKeysFollowLastTouchedThrottle = prefs.getBoolean("prefVolumeKeysFollowLastTouchedThrottle", getResources().getBoolean(R.bool.prefVolumeKeysFollowLastTouchedThrottleDefaultValue));

        // Ignore the labels for the loco in the Roster and use the defaults... if requested in preferences
        mainapp.prefAlwaysUseDefaultFunctionLabels = prefs.getBoolean("prefAlwaysUseDefaultFunctionLabels", getResources().getBoolean(R.bool.prefAlwaysUseDefaultFunctionLabelsDefaultValue));
        prefNumberOfDefaultFunctionLabels = Integer.parseInt(prefs.getString("prefNumberOfDefaultFunctionLabels", getResources().getString(R.string.prefNumberOfDefaultFunctionLabelsDefaultValue)));

        mainapp.prefOverrideWiThrottlesFunctionLatching = prefs.getBoolean("prefOverrideWiThrottlesFunctionLatching", getResources().getBoolean(R.bool.prefOverrideWiThrottlesFunctionLatchingDefaultValue));
        mainapp.prefOverrideRosterWithNoFunctionLabels = prefs.getBoolean("prefOverrideRosterWithNoFunctionLabels", getResources().getBoolean(R.bool.prefOverrideRosterWithNoFunctionLabelsDefaultValue));

        prefAccelerometerShake = prefs.getString("prefAccelerometerShake", getApplicationContext().getResources().getString(R.string.prefAccelerometerShakeDefaultValue));

        // set speed buttons speed step
        prefSpeedButtonsSpeedStep = threaded_application.getIntPrefValue(prefs, "prefSpeedButtonsSpeedStep", "4");
        prefVolumeSpeedButtonsSpeedStep = threaded_application.getIntPrefValue(prefs, "prefVolumeSpeedButtonsSpeedStep", getApplicationContext().getResources().getString(R.string.prefVolumeSpeedButtonsSpeedStepDefaultValue));
        prefGamePadSpeedButtonsSpeedStep = threaded_application.getIntPrefValue(prefs, "prefGamePadSpeedButtonsSpeedStep", getApplicationContext().getResources().getString(R.string.prefVolumeSpeedButtonsSpeedStepDefaultValue));
        prefSpeedButtonsSpeedStepDecrement = prefs.getBoolean("prefSpeedButtonsSpeedStepDecrement", getResources().getBoolean(R.bool.prefSpeedButtonsSpeedStepDecrementDefaultValue));
        prefStopButtonEStopOnLongPress = prefs.getBoolean("prefStopButtonEStopOnLongPress", getResources().getBoolean(R.bool.prefStopButtonEStopOnLongPressDefaultValue));

        mainapp.prefGamepadOnlyOneGamepad = prefs.getBoolean("prefGamepadOnlyOneGamepad", getResources().getBoolean(R.bool.prefGamepadOnlyOneGamepadDefaultValue));

        prefGamePadDoublePressStop = prefs.getString("prefGamePadDoublePressStop", getResources().getString(R.string.prefGamePadDoublePressStopDefaultValue));
        mainapp.prefGamePadIgnoreJoystick = prefs.getBoolean("prefGamePadIgnoreJoystick", getResources().getBoolean(R.bool.prefGamePadIgnoreJoystickDefaultValue));

        prefEsuMc2EndStopDirectionChange = prefs.getBoolean("prefEsuMc2EndStopDirectionChange", getResources().getBoolean(R.bool.prefEsuMc2EndStopDirectionChangeDefaultValue));
        prefEsuMc2StopButtonShortPress = prefs.getBoolean("prefEsuMc2StopButtonShortPress", getResources().getBoolean(R.bool.prefEsuMc2StopButtonShortPressDefaultValue));

        mainapp.prefNumThrottles = mainapp.Numeralise(prefs.getString("prefNumThrottles", getResources().getString(R.string.prefNumThrottlesDefaultValue)));
        prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        prefSelectiveLeadSound = prefs.getBoolean("prefSelectiveLeadSound", getResources().getBoolean(R.bool.prefSelectiveLeadSoundDefaultValue));
        prefSelectiveLeadSoundF1 = prefs.getBoolean("prefSelectiveLeadSoundF1", getResources().getBoolean(R.bool.prefSelectiveLeadSoundF1DefaultValue));
        prefSelectiveLeadSoundF2 = prefs.getBoolean("prefSelectiveLeadSoundF2", getResources().getBoolean(R.bool.prefSelectiveLeadSoundF2DefaultValue));

        prefHideSlider = prefs.getBoolean("prefHideSlider", getResources().getBoolean(R.bool.prefHideSliderDefaultValue));

        prefLimitSpeedButton = prefs.getBoolean("prefLimitSpeedButton", getResources().getBoolean(R.bool.prefLimitSpeedButtonDefaultValue));
        prefLimitSpeedPercent = Integer.parseInt(prefs.getString("prefLimitSpeedPercent", getResources().getString(R.string.prefLimitSpeedPercentDefaultValue)));
        prefDisplaySpeedUnits = threaded_application.getIntPrefValue(prefs, "prefDisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
        prefPauseSpeedButton = prefs.getBoolean("prefPauseSpeedButton", getResources().getBoolean(R.bool.prefPauseSpeedButtonDefaultValue));
        prefPauseAlternateButton = prefs.getBoolean("prefPauseAlternateButton", getResources().getBoolean(R.bool.prefPauseAlternateButtonDefaultValue));
        prefPauseSpeedRate = Integer.parseInt(prefs.getString("prefPauseSpeedRate", getResources().getString(R.string.prefPauseSpeedRateDefaultValue)));
        prefPauseSpeedStep = Integer.parseInt(prefs.getString("prefPauseSpeedStep", getResources().getString(R.string.prefPauseSpeedStepDefaultValue)));


        mainapp.prefHapticFeedback = prefs.getString("prefHapticFeedback", getResources().getString(R.string.prefHapticFeedbackDefaultValue));
//        mainapp.prefHapticFeedbackSteps = Integer.parseInt(prefs.getString("prefHapticFeedbackSteps", getResources().getString(R.string.prefHapticFeedbackStepsDefaultValue)));
        mainapp.prefHapticFeedbackDuration = Integer.parseInt(prefs.getString("prefHapticFeedbackDuration", getResources().getString(R.string.prefHapticFeedbackDurationDefaultValue)));
        mainapp.prefHapticFeedbackButtons = prefs.getBoolean("prefHapticFeedbackButtons", getResources().getBoolean(R.bool.prefHapticFeedbackButtonsDefaultValue));

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CLOCK_DISPLAY_CHANGED);

        if (isCreate) {
            prefs.edit().putString("prefKidsTimer", PREF_KIDS_TIMER_NONE).commit();  //reset the preference
        }
        getKidsTimerPrefs();

        mainapp.prefFullScreenSwipeArea = prefs.getBoolean("prefFullScreenSwipeArea",
                getResources().getBoolean(R.bool.prefFullScreenSwipeAreaDefaultValue));
        mainapp.prefLeftRightSwipeChangesSpeed = prefs.getBoolean("prefLeftRightSwipeChangesSpeed",
                getResources().getBoolean(R.bool.prefLeftRightSwipeChangesSpeedDefaultValue));

        mainapp.prefSwipeSpeedChangeStep = threaded_application.getIntPrefValue(prefs, "prefSwipeSpeedChangeStep",
                getApplicationContext().getResources().getString(R.string.prefSwipeSpeedChangeStepDefaultValue));

        mainapp.prefThrottleViewImmersiveModeHideToolbar = prefs.getBoolean("prefThrottleViewImmersiveModeHideToolbar",
                getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeHideToolbarDefaultValue));

        mainapp.prefActionBarShowServerDescription = prefs.getBoolean("prefActionBarShowServerDescription",
                getResources().getBoolean(R.bool.prefActionBarShowServerDescriptionDefaultValue));

        mainapp.prefDeviceSoundsButton = prefs.getBoolean("prefDeviceSoundsButton",
                getResources().getBoolean(R.bool.prefDeviceSoundsButtonDefaultValue));

        mainapp.prefDeviceSounds[0] = prefs.getString("prefDeviceSounds0", getResources().getString(R.string.prefDeviceSoundsDefaultValue));
        mainapp.prefDeviceSounds[1] = prefs.getString("prefDeviceSounds1", getResources().getString(R.string.prefDeviceSoundsDefaultValue));
        mainapp.prefDeviceSoundsMomentum = threaded_application.getIntPrefValue(prefs, "prefDeviceSoundsMomentum", getResources().getString(R.string.prefDeviceSoundsMomentumDefaultValue));
        mainapp.prefDeviceSoundsMomentumOverride = prefs.getBoolean("prefDeviceSoundsMomentumOverride",
                getResources().getBoolean(R.bool.prefDeviceSoundsMomentumOverrideDefaultValue));
        mainapp.prefDeviceSoundsLocoVolume = threaded_application.getIntPrefValue(prefs, "prefDeviceSoundsLocoVolume", "100");
        mainapp.prefDeviceSoundsBellVolume = threaded_application.getIntPrefValue(prefs, "prefDeviceSoundsBellVolume", "100");
        mainapp.prefDeviceSoundsHornVolume = threaded_application.getIntPrefValue(prefs, "prefDeviceSoundsHornVolume", "100");
        mainapp.prefDeviceSoundsLocoVolume = mainapp.prefDeviceSoundsLocoVolume / 100;
        mainapp.prefDeviceSoundsBellVolume = mainapp.prefDeviceSoundsBellVolume / 100;
        mainapp.prefDeviceSoundsHornVolume = mainapp.prefDeviceSoundsHornVolume / 100;

        mainapp.prefDeviceSoundsBellIsMomentary = prefs.getBoolean("prefDeviceSoundsBellIsMomentary",
                getResources().getBoolean(R.bool.prefDeviceSoundsBellIsMomentaryDefaultValue));
        mainapp.prefDeviceSoundsF1F2ActivateBellHorn = prefs.getBoolean("prefDeviceSoundsF1F2ActivateBellHorn",
                getResources().getBoolean(R.bool.prefDeviceSoundsF1F2ActivateBellHornDefaultValue));
        mainapp.prefDeviceSoundsHideMuteButton = prefs.getBoolean("prefDeviceSoundsHideMuteButton",
                getResources().getBoolean(R.bool.prefDeviceSoundsHideMuteButtonDefaultValue));

        if ((!mainapp.prefDeviceSounds[0].equals("none")) || (!mainapp.prefDeviceSounds[1].equals("none"))) {
            loadSounds();
        } else {
            mainapp.stopAllSounds();
        }

        mainapp.getDefaultSortOrderRoster();

        // ESU MC 2/Pro
        if (IS_ESU_MCII) {
            updateEsuMc2ZeroTrim();

            mainapp.prefEsuMc2SliderType = prefs.getString("prefEsuMc2SliderType", getApplicationContext().getResources().getString(R.string.prefEsuMc2SliderTypeDefaultValue));
            mainapp.useEsuMc2DecoderBrakes = (mainapp.prefEsuMc2SliderType.equals("esu"));

            if (mainapp.useEsuMc2DecoderBrakes) {
                String tmp = prefs.getString("prefEsuMc2SliderTypeDecoderBrakeTypeLowFunctionEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeLowFunctionEsuDefaultValue));
                String[] prefValues = threaded_application.splitByString(tmp, " ");
                for (int i = 0; i < 5; i++) {
                    if (i < prefValues.length)
                        mainapp.esuMc2BrakeFunctions[0][i] = Integer.parseInt(prefValues[i]);
                    else mainapp.esuMc2BrakeFunctions[0][i] = -1;
                }
                tmp = prefs.getString("prefEsuMc2SliderTypeDecoderBrakeTypeMidFunctionEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeMidFunctionEsuDefaultValue));
                prefValues = threaded_application.splitByString(tmp, " ");
                for (int i = 0; i < 5; i++) {
                    if (i < prefValues.length)
                        mainapp.esuMc2BrakeFunctions[1][i] = Integer.parseInt(prefValues[i]);
                    else mainapp.esuMc2BrakeFunctions[1][i] = -1;
                }
                tmp = prefs.getString("prefEsuMc2SliderTypeDecoderBrakeTypeHighFunctionEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeHighFunctionEsuDefaultValue));
                prefValues = threaded_application.splitByString(tmp, " ");
                for (int i = 0; i < 5; i++) {
                    if (i < prefValues.length)
                        mainapp.esuMc2BrakeFunctions[2][i] = Integer.parseInt(prefValues[i]);
                    else mainapp.esuMc2BrakeFunctions[2][i] = -1;
                }

                int tmpInt = threaded_application.getIntPrefValue(prefs, "prefEsuMc2SliderTypeDecoderBrakeTypeLowValueEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeLowValueEsuDefaultValue));
                mainapp.esuMc2BrakeLevels[0] = tmpInt>=0 ? tmpInt : 10000;
                tmpInt = threaded_application.getIntPrefValue(prefs, "prefEsuMc2SliderTypeDecoderBrakeTypeMidValueEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeMidValueEsuDefaultValue));
                mainapp.esuMc2BrakeLevels[1] = tmpInt>=0 ? tmpInt : 10000;
                tmpInt = threaded_application.getIntPrefValue(prefs, "prefEsuMc2SliderTypeDecoderBrakeTypeHighValueEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeHighValueEsuDefaultValue));
                mainapp.esuMc2BrakeLevels[2] = tmpInt>=0 ? tmpInt : 10000;
            }
        }

        mainapp.prefAppIconAction = prefs.getString("prefAppIconAction", getResources().getString(R.string.prefAppIconActionDefaultValue));
        mainapp.prefActionBarShowDccExButton = prefs.getBoolean("prefActionBarShowDccExButton",
                getResources().getBoolean(R.bool.prefActionBarShowDccExButtonDefaultValue));
        mainapp.prefActionBarShowThrottleButton = prefs.getBoolean("prefActionBarShowThrottleButton",
                getResources().getBoolean(R.bool.prefActionBarShowThrottleButtonDefaultValue));
        mainapp.prefActionBarShowTurnoutsButton = prefs.getBoolean("prefActionBarShowTurnoutsButton",
                getResources().getBoolean(R.bool.prefActionBarShowTurnoutsButtonDefaultValue));
        mainapp.prefActionBarShowRoutesButton = prefs.getBoolean("prefActionBarShowRoutesButton",
                getResources().getBoolean(R.bool.prefActionBarShowRoutesButtonDefaultValue));
        mainapp.prefActionBarShowWebButton = prefs.getBoolean("prefActionBarShowWebButton",
                getResources().getBoolean(R.bool.prefActionBarShowWebButtonDefaultValue));
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
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            FullLeftText[throttleIndex] = DIRECTION_BUTTON_LEFT_TEXT;
            FullRightText[throttleIndex] = DIRECTION_BUTTON_RIGHT_TEXT;
            dirLeftIndicationText[throttleIndex] = "";
            dirRightIndicationText[throttleIndex] = "";
        }

        if (((prefLeftDirectionButtons.equals(DIRECTION_BUTTON_LEFT_TEXT)) && (prefRightDirectionButtons.equals(DIRECTION_BUTTON_RIGHT_TEXT)))
                || ((prefLeftDirectionButtons.isEmpty()) && (prefRightDirectionButtons.isEmpty()))
                || ((prefLeftDirectionButtons.equals(DIRECTION_BUTTON_LEFT_TEXT)) && (prefRightDirectionButtons.isEmpty()))
                || ((prefRightDirectionButtons.equals(DIRECTION_BUTTON_RIGHT_TEXT))) && ((prefLeftDirectionButtons.isEmpty()))
        ) {
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                if (directionButtonsAreCurrentlyReversed(throttleIndex)) {
                    FullLeftText[throttleIndex] = DIRECTION_BUTTON_RIGHT_TEXT;
                    FullRightText[throttleIndex] = DIRECTION_BUTTON_LEFT_TEXT;
                }
            }
        } else {
            dirLeftText = prefLeftDirectionButtons;
            dirRightText = prefRightDirectionButtons;

            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
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

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            bForwards[throttleIndex].setText(FullLeftText[throttleIndex]);
            bReverses[throttleIndex].setText(FullRightText[throttleIndex]);
            tvLeftDirectionIndicators[throttleIndex].setText(dirLeftIndicationText[throttleIndex]);
            tvRightDirectionIndicators[throttleIndex].setText(dirRightIndicationText[throttleIndex]);
        }
    }

    private void reloadWeb() {
        if ( (webView == null) || (!mainapp.currentScreenSupportsWebView) ) return;
        webView.stopLoading();
        load_webview(); // reload
    }

    private void initWeb() {
        // reload from the initial webpage
        currentUrl = null;
        reloadWeb();
    }

    // used for the swipe up option and the shake, to show or hide the web view on the throttle page
    private void showHideWebView(String toastMsg) {
        if (!(keepWebViewLocation.equals(web_view_location_type.NONE))) { // show/hide the web view if the preference is set
            if (!webViewIsOn) {
                prefWebViewLocation = keepWebViewLocation;
                webView.setVisibility(VISIBLE);
            } else {
                prefWebViewLocation = web_view_location_type.NONE;
                webView.setVisibility(View.GONE);
                if (!toastMsg.isEmpty())
                    mainapp.safeToast(toastMsg, Toast.LENGTH_SHORT);
            }
            webViewIsOn = !webViewIsOn;

            setLabels();
            pauseResumeWebView();
        }
    }

    private void pauseResumeWebView() {
        if (webViewIsOn) {
            this.resumeWebView();
        } else {
            this.pauseWebView();
        }
    }

    private void witRetry(String s) {
        if (this.hasWindowFocus()) {
            webView.stopLoading();
            Intent in = new Intent().setClass(this, reconnect_status.class);
            in.putExtra("status", s);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    protected void removeLoco(int whichThrottle) {
        disableButtons(whichThrottle);         // direction and slider
        set_function_labels_and_listeners_for_view(whichThrottle);
        setLabels();
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
    }

    void queryAllSpeedsAndDirectionsWiT() {
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            if ((mainapp.consists != null) && (mainapp.consists[throttleIndex] != null)
                    && (mainapp.consists[throttleIndex].isActive())) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIT_QUERY_SPEED_AND_DIRECTION, "", throttleIndex);
            }
        }
    }

    // process WiT speed report
    // update speed slider if didn't just send a speed update to WiT
    void speedUpdateWiT(int whichThrottle, int speedWiT) {
        if (speedWiT < 0) speedWiT = 0;
        if (!changeTimers[whichThrottle].delayInProg) {
            sbs[whichThrottle].setProgress(speedWiT);
            // Now update ESU MCII Knob position
            if (IS_ESU_MCII) {
                if (!isSemiRealisticThrottle) {
                    threaded_application.extendedLogging(activityName + ": speedUpdateWiT(): ESU_MCII: Move knob request for WiT speed report");
                    setEsuThrottleKnobPosition(whichThrottle, speedWiT);
                } else {
                    // if it is the first update since acquiring the loco, force the knob abd slider to the retrieved speed
                    if (mainapp.EsuMc2FirstServerUpdate[whichThrottle]) {
                        mainapp.EsuMc2FirstServerUpdate[whichVolume] = false;
                        int vSpeed = (int) ( (float) speedWiT / 126 * (float) getNewSemiRealisticThrottleSliderNotches());
                        semiRealisticThrottleSliderPositionUpdate(whichVolume,vSpeed);
                        setTargetSpeed(whichVolume,true);
                        setEsuThrottleKnobPosition(whichVolume, speedWiT);
                        showTargetDirectionIndication(whichThrottle);
                    }
                }
            }
        }
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
    }

    SeekBar getThrottleSlider(int whichThrottle) {
        return sbs[whichThrottle];
    }

    double getDisplayUnitScale(int whichThrottle) {
        return displayUnitScales[whichThrottle];
    }

    // set speed slider to absolute value on all throttles
    protected void speedUpdate(int speed) {
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            speedUpdate(throttleIndex, speed);
        }
    }

    // set speed slider to absolute value on one throttle
    void speedUpdate(int whichThrottle, int speed) {
        if(whichThrottle >= mainapp.maxThrottlesCurrentScreen) return;

        if (speed < 0) speed = 0;
        getThrottleSlider(whichThrottle).setProgress(speed);

        mainapp.dccexLastKnownSpeed[whichThrottle] = speed;
        mainapp.dccexLastKnownDirection[whichThrottle] = getDirection(whichThrottle);
    }

    // get the current speed of the throttle from the slider
    int getSpeed(int whichThrottle) {
        return getThrottleSlider(whichThrottle).getProgress();
    }

    private int getScaleSpeed(int whichThrottle) {
//        Consist con = mainapp.consists[whichThrottle];
        int speed = getSpeed(whichThrottle);
        double speedScale = getDisplayUnitScale(whichThrottle);
        if (speed < 0) {
            speed = 0;
        }
        return (int) Math.round(speed * speedScale) - 1;
    }

    int getMaxSpeed(int whichThrottle) {
        return getThrottleSlider(whichThrottle).getMax();
    }

    /** @noinspection SameReturnValue, SameReturnValue */
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
        int scaleSpeed = lastScaleSpeed + change;
        int speed = (int) Math.round(scaleSpeed / displayUnitScale);
        threaded_application.extendedLogging(activityName + ": speedChange():  change: " + change + " lastSpeed: " + lastSpeed+ " lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed:" + scaleSpeed);
        if (lastScaleSpeed == scaleSpeed) {
            speed += (int) Math.signum(change);
        }
        if (speed < 0)  //insure speed is inside bounds
            speed = 0;
        if (speed > MAX_SPEED_VAL_WIT)
            speed = MAX_SPEED_VAL_WIT;

        if (prefLimitSpeedButton && isLimitSpeeds[whichThrottle] && (speed > limitSpeedMax[whichThrottle])) {
            speed = limitSpeedMax[whichThrottle];
        }

        threaded_application.extendedLogging(activityName + ": speedChange():  change: " + change + " speed: " + speed+ " scaleSpeed: " + scaleSpeed);

        throttle_slider.setProgress(speed);
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
        return speed;
    }

    public void setSpeed(int whichThrottle, int speed, int from) {
        switch (from) {
            case speed_commands_from_type.BUTTONS:
                if (isPauseSpeeds[whichThrottle] == pause_speed_type.INACTIVE) {
                    speedUpdateAndNotify(whichThrottle, speed);
                    mainapp.whichThrottleLastTouch = whichThrottle;
                } else {
                    if (isPauseSpeeds[whichThrottle] == pause_speed_type.INACTIVE) {
                        speedChangeAndNotify(whichThrottle, speed);
                        mainapp.whichThrottleLastTouch = whichThrottle;
                    } else {
                        if (isPauseSpeeds[whichThrottle] == pause_speed_type.START_TO_ZERO) {
                            isPauseSpeeds[whichThrottle] = pause_speed_type.TO_ZERO;
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.DECREMENT);
                            if (vsbSwitchingSpeeds != null) {
                                if ((pauseDir[whichThrottle] == direction_type.FORWARD) && (getDirection(whichThrottle) == direction_type.REVERSE)) {
                                    setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.INCREMENT);
                                }
                            }
                        } else {
                            isPauseSpeeds[whichThrottle] = pause_speed_type.TO_RETURN;
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.INCREMENT);
                            if (vsbSwitchingSpeeds != null) {
                                if (((pauseDir[whichThrottle] == direction_type.REVERSE) && (getDirection(whichThrottle) == direction_type.FORWARD))
                                        || ((pauseDir[whichThrottle] == direction_type.FORWARD) && (getDirection(whichThrottle) == direction_type.REVERSE))) {
                                    setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.DECREMENT);
                                }
                            }

                            if ((getSpeed(whichThrottle) > pauseSpeed[whichThrottle])
                                    && (pauseDir[whichThrottle] == getDirection(whichThrottle))) {
                                setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.INVERT);
                            }
                        }
                        repeatUpdateHandler.post(new RptUpdater(whichThrottle, prefPauseSpeedRate));
                    }
                }
                break;
            case speed_commands_from_type.VOLUME:
            case speed_commands_from_type.GAMEPAD:
                speedUpdateAndNotify(whichThrottle, speed);
                break;
        }
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
    }

    public void decrementSpeed(int whichThrottle, int from) {
        decrementSpeed(whichThrottle, from, 1);
    }

    public void decrementSpeed(int whichThrottle, int from, int stepMultiplier) {
        decrementSpeed(whichThrottle, from, stepMultiplier, prefSpeedButtonsSpeedStep);
    }

    public void decrementSpeed(int whichThrottle, int from, int stepMultiplier, int stepSize) {
        switch (from) {
            case speed_commands_from_type.BUTTONS:
                if ((isPauseSpeeds[whichThrottle] == pause_speed_type.INACTIVE)
                        || (isPauseSpeeds[whichThrottle] == pause_speed_type.ZERO)) {
                    speedChangeAndNotify(whichThrottle, - stepSize * stepMultiplier);
                    mainapp.whichThrottleLastTouch = whichThrottle;
                } else {
                    int targetSpeed = 0;
                    if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN) {
                        targetSpeed = pauseSpeed[whichThrottle];
                    }

                    int currentSpeed = getSpeed(whichThrottle);
                    int currentDir = getDirection(whichThrottle);
                    boolean atTarget = true;
                    if (((vsbSwitchingSpeeds == null) || (currentDir != direction_type.REVERSE))
                            && ((currentSpeed - prefPauseSpeedStep) > targetSpeed)) {
                        atTarget = false;
                    } else if ((vsbSwitchingSpeeds != null) && (currentDir == direction_type.REVERSE)
                            && (pauseDir[whichThrottle] == direction_type.REVERSE)
                            && ((currentSpeed - prefPauseSpeedStep) > targetSpeed)) {
                        atTarget = false;
                    } else if ((vsbSwitchingSpeeds != null)
                            && ((currentSpeed - prefPauseSpeedStep) <= 0)
                            && (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN)) {  // need a direction change
                        speedUpdateAndNotify(whichThrottle, 0);
                        setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.INVERT);
                        setEngineDirection(whichThrottle, (currentDir == direction_type.FORWARD ? direction_type.REVERSE : direction_type.FORWARD), false);
                        speedUpdateAndNotify(whichThrottle, prefPauseSpeedStep);
                        return;
                    } else if ((vsbSwitchingSpeeds != null)
                            && ((currentDir == direction_type.FORWARD) && (pauseDir[whichThrottle] == direction_type.REVERSE))
                            || ((currentDir == direction_type.REVERSE) && (pauseDir[whichThrottle] == direction_type.FORWARD))) {
                        atTarget = false;
                    }

                    if (!atTarget) {
                        speedUpdateAndNotify(whichThrottle, currentSpeed - prefPauseSpeedStep);
                    } else {
                        setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
                        if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_ZERO) {
                            isPauseSpeeds[whichThrottle] = pause_speed_type.ZERO;
                            speedUpdateAndNotify(whichThrottle, 0);
                        } else {
                            isPauseSpeeds[whichThrottle] = pause_speed_type.INACTIVE;
                            speedUpdateAndNotify(whichThrottle, targetSpeed);
                        }

                    }
                }
                break;
            case speed_commands_from_type.VOLUME:
                speedChangeAndNotify(whichThrottle, -prefVolumeSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case speed_commands_from_type.GAMEPAD:
                speedChangeAndNotify(whichThrottle, -prefGamePadSpeedButtonsSpeedStep * stepMultiplier);
                mainapp.whichThrottleLastTouch = whichThrottle;
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                        , getMaxSpeed(whichThrottle)
                        , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                        , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                        , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                        , isSemiRealisticThrottle
                        , "");
                break;
        }
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
    }

    public void incrementSpeed(int whichThrottle, int from) {
        incrementSpeed(whichThrottle, from, 1);
    }

    public void incrementSpeed(int whichThrottle, int from, int stepMultiplier) {
        incrementSpeed(whichThrottle, from, stepMultiplier, prefSpeedButtonsSpeedStep);
    }

    public void incrementSpeed(int whichThrottle, int from, int stepMultiplier, int stepSize) {
        switch (from) {
            case speed_commands_from_type.BUTTONS:
                if ((isPauseSpeeds[whichThrottle] == pause_speed_type.INACTIVE)
                        || (isPauseSpeeds[whichThrottle] == pause_speed_type.ZERO)) {
                    speedChangeAndNotify(whichThrottle, stepSize * stepMultiplier);
                    mainapp.whichThrottleLastTouch = whichThrottle;
                } else {
                    int targetSpeed = 0;
                    if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN) {
                        targetSpeed = pauseSpeed[whichThrottle];
                    }

                    int currentSpeed = getSpeed(whichThrottle);
                    int currentDir = getDirection(whichThrottle);
                    boolean atTarget = true;
                    if (((vsbSwitchingSpeeds == null) || (currentDir != direction_type.REVERSE))
                            && ((currentSpeed + prefPauseSpeedStep) < targetSpeed)) {
                        atTarget = false;
                    } else if ((vsbSwitchingSpeeds != null) && (currentDir == direction_type.REVERSE)
                            && (pauseDir[whichThrottle] == direction_type.REVERSE)
                            && ((currentSpeed + prefPauseSpeedStep) < targetSpeed)) {
                        atTarget = false;
                    } else if ((vsbSwitchingSpeeds != null) && (currentDir == direction_type.REVERSE)
                            && (pauseDir[whichThrottle] == direction_type.FORWARD)) {
                        atTarget = false;
                    }

                    if (!atTarget) {
                        speedUpdateAndNotify(whichThrottle, currentSpeed + prefPauseSpeedStep);
                    } else {
                        setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
                        if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_ZERO) {
                            isPauseSpeeds[whichThrottle] = pause_speed_type.ZERO;
                            speedUpdateAndNotify(whichThrottle, 0);
                        } else {
                            isPauseSpeeds[whichThrottle] = pause_speed_type.INACTIVE;
                            speedUpdateAndNotify(whichThrottle, targetSpeed);
                        }
                    }
                }
                break;
            case speed_commands_from_type.VOLUME:
                speedChangeAndNotify(whichThrottle, prefVolumeSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case speed_commands_from_type.GAMEPAD:
                speedChangeAndNotify(whichThrottle, prefGamePadSpeedButtonsSpeedStep * stepMultiplier);
                mainapp.whichThrottleLastTouch = whichThrottle;
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                        , getMaxSpeed(whichThrottle)
                        , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                        , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                        , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                        , isSemiRealisticThrottle
                        , "");
                break;
        }
        if ((vsbSwitchingSpeeds != null) || (hsbSwitchingSpeeds != null)) { // get around a problem with reporting for the switching layouts
            sbs[whichThrottle].setProgress(getSpeedFromCurrentSliderPosition(whichThrottle, false));
        }

        kidsTimerActions(kids_timer_action_type.STARTED, 0);
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

    } // end incrementSpeed

    protected void setAutoIncrementOrDecrement(int whichThrottle, int dir) {
        switch (dir) {
            case auto_increment_or_decrement_type.OFF:
                mAutoIncrement[whichThrottle] = false;
                mAutoDecrement[whichThrottle] = false;
                break;
            case auto_increment_or_decrement_type.INCREMENT:
                mAutoIncrement[whichThrottle] = true;
                mAutoDecrement[whichThrottle] = false;
                break;
            case auto_increment_or_decrement_type.DECREMENT:
                mAutoIncrement[whichThrottle] = false;
                mAutoDecrement[whichThrottle] = true;
                break;
            case auto_increment_or_decrement_type.INVERT:
                mAutoIncrement[whichThrottle] = !mAutoIncrement[whichThrottle];
                mAutoDecrement[whichThrottle] = !mAutoDecrement[whichThrottle];
                break;
        }
    }

    // combined - deal with the SRT specific requirements
    void speedUpdateAndNotifyCombined(int speed) {
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            speedUpdateAndNotifyCombined(throttleIndex, speed);
        }
    }
    private void speedUpdateAndNotifyCombined(int whichThrottle, int speed) {
        if (!isSemiRealisticThrottle) {
            speedUpdateAndNotify(whichThrottle, speed);
        } else {
            semiRealisticThrottleSliderPositionUpdate(whichThrottle, speed);
        }
    }

    // set speed slider and notify server for all throttles
    void speedUpdateAndNotify(int speed) {
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
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
        if (IS_ESU_MCII && moveMc2Knob && !isSemiRealisticThrottle) {
            threaded_application.extendedLogging(activityName + ": speedUpdateAndNotify(): ESU_MCII: Move knob request for speed update");
            setEsuThrottleKnobPosition(whichThrottle, speed);
        }
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
    }

    // change speed slider by scaled value and notify server
    public void speedChangeAndNotify(int whichThrottle, int change) {

        int speed = speedChange(whichThrottle, change);
        sendSpeedMsg(whichThrottle, speed);
        // Now update ESU MCII Knob position
        if (IS_ESU_MCII && !isSemiRealisticThrottle) {
            threaded_application.extendedLogging(activityName + ": speedChangeAndNotify(): ESU_MCII: Move knob request for speed change");
            setEsuThrottleKnobPosition(whichThrottle, speed);
        }
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
    }

    // set the displayed numeric speed value
    @SuppressLint("SetTextI18n")
    protected void setDisplayedSpeed(int whichThrottle, int speed) {
        TextView speed_label;
        double speedScale = getDisplayUnitScale(whichThrottle);
        speed_label = tvSpeedValues[whichThrottle];
        if (speed < 0) {
            mainapp.safeToast(getApplicationContext().getResources().getString(R.string.toastThrottleAlertEstop, getConsistAddressString(whichThrottle)), Toast.LENGTH_LONG);
            speed = 0;
        }
        int scaleSpeed = (int) Math.round(speed * speedScale);
        try {
            int prevScaleSpeed = Integer.parseInt((String) speed_label.getText());
            speed_label.setText(Integer.toString(scaleSpeed));
            mainapp.throttleVibration(scaleSpeed, prevScaleSpeed);
        } catch (NumberFormatException | ClassCastException e) {
            Log.e(threaded_application.applicationName, activityName + ": setDisplayedSpeed(): problem showing speed: " + e.getMessage());
        }
    }

    // set the displayed numeric speed value
    @SuppressLint("SetTextI18n")
    protected void setDisplayedSpeedWithDirection(int whichThrottle, int speed) {
        TextView speed_label;
        double speedScale = getDisplayUnitScale(whichThrottle);
        speed_label = tvSpeedValues[whichThrottle];
        if (speed < 0) {
            mainapp.safeToast(getApplicationContext().getResources().getString(R.string.toastThrottleAlertEstop, getConsistAddressString(whichThrottle)), Toast.LENGTH_LONG);
            speed = 0;
        }
        int scaleSpeed = (int) Math.round(speed * speedScale);

        int dir = getDirection(whichThrottle);
        if (tvDirectionIndicatorForwards[whichThrottle] != null) {   //not all layouts have the indicators
            int showForward = View.GONE;
            int showReverse = View.GONE;

            if (speed > 0) {
                if (((!directionButtonsAreCurrentlyReversed(whichThrottle)) && (dir == direction_type.FORWARD))
                        || ((directionButtonsAreCurrentlyReversed(whichThrottle)) && (dir == direction_type.REVERSE))) {
                    showForward = VISIBLE;
                } else {
                    showReverse = VISIBLE;
                }
            }
            tvDirectionIndicatorForwards[whichThrottle].setVisibility(showForward);
            tvDirectionIndicatorReverses[whichThrottle].setVisibility(showReverse);
        }

        String sPrevScaleSpeed = (String) speed_label.getText();
        int prevScaleSpeed = Integer.parseInt(sPrevScaleSpeed);
        speed_label.setText(Integer.toString(scaleSpeed));
        mainapp.throttleVibration(scaleSpeed, prevScaleSpeed);
    }

    //adjust maxSpeedSteps from code passed from JMRI, but only if set to Auto, else do not change
    private void setSpeedStepsFromWiT(int whichThrottle, int speedStepCode) {
        int maxSpeedStep = 100;
        switch (speedStepCode) {
            case speed_step_type.STEPS_128 -> maxSpeedStep = 126;
            case speed_step_type.STEPS_27 -> maxSpeedStep = 27;
            case speed_step_type.STEPS_14 -> maxSpeedStep = 14;
            case speed_step_type.STEPS_28 -> maxSpeedStep = 28;
        }

        int zeroTrim = threaded_application.getIntPrefValue(prefs, "prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
        ThrottleScale esuThrottleScale = new ThrottleScale(zeroTrim, maxSpeedStep + 1);

        maxSpeedSteps[whichThrottle] = maxSpeedStep;
        esuThrottleScales[whichThrottle] = esuThrottleScale;
        setDisplayUnitScale(whichThrottle);

        limitSpeedMax[whichThrottle] = Math.round(maxSpeedStep * ((float) prefLimitSpeedPercent) / 100);
    }

    //get max speedStep based on Preferences
    //unless pref is set to AUTO in which case just return the input value
    private int getSpeedSteps(int maxStep) {
        if (prefDisplaySpeedUnits != speed_step_type.STEPS_AUTO) {
            maxStep = prefDisplaySpeedUnits;
        }
        return maxStep;
    }

    protected void setDisplayUnitScale(int whichThrottle) {
        int maxStep;
        maxStep = getSpeedSteps(maxSpeedSteps[whichThrottle]);
        displayUnitScales[whichThrottle] = calcDisplayUnitScale(maxStep);
        tvSpeedLabels[whichThrottle].setText(calcDisplayUnitLabel(maxStep));

        limitSpeedMax[whichThrottle] = Math.round(maxSpeedSteps[whichThrottle] * ((float) prefLimitSpeedPercent) / 100);
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
        if ((mainapp.consists != null) && (dirs != null)) {
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
                    } catch (
                            Exception e) { // isReverseOfLead returns null if addr is not in con - should never happen since we are walking through consist list
                        Log.d(threaded_application.applicationName, activityName + ": " + mainapp.throttleIntToString(whichThrottle) + " direction change for unselected loco " + addr);
                    }
                }
            }
        }
    }

    protected String getConsistAddressString(int whichThrottle) {
        String result;
        if (!prefShowAddressInsteadOfName) {
            result = mainapp.consists[whichThrottle].toString();
        } else {
            result = mainapp.consists[whichThrottle].formatConsistAddr();
        }

        return result;
    }

    // get the consist for the specified throttle
    private static final Consist emptyConsist = new Consist();

    protected Consist getConsist(int whichThrottle) {
        if (mainapp.consists == null || whichThrottle >= mainapp.consists.length || mainapp.consists[whichThrottle] == null)
            return emptyConsist;
        return mainapp.consists[whichThrottle];
    }

    // get the indicated direction from the button pressed state
    protected int getDirection(int whichThrottle) {
        return dirs[whichThrottle];
    }

    // update the direction indicators
    void showDirectionIndications() {
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            showDirectionIndication(throttleIndex, dirs[throttleIndex]);
        }
    }

    // indicate direction using the button pressed state
    void showDirectionIndication(int whichThrottle, int direction) {

        boolean setLeftDirectionButtonEnabled;
        if (direction == 0) {  //0=reverse 1=forward
            setLeftDirectionButtonEnabled = directionButtonsAreCurrentlyReversed(whichThrottle);
        } else {
            setLeftDirectionButtonEnabled = !directionButtonsAreCurrentlyReversed(whichThrottle);
        }

        if (!getConsist(whichThrottle).isActive()) {
            bForwards[whichThrottle].setSelected(false);
            bReverses[whichThrottle].setSelected(false);
        } else {
            if (!setLeftDirectionButtonEnabled) {
                bForwards[whichThrottle].setSelected(false);
                bReverses[whichThrottle].setSelected(true);
                bForwards[whichThrottle].setTypeface(null, Typeface.NORMAL);
                bReverses[whichThrottle].setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                if ((getSpeed(whichThrottle) > 0) && (!dirChangeWhileMoving)) {
                    bForwards[whichThrottle].setEnabled(false);
                }
                bReverses[whichThrottle].setEnabled(true);
            } else {
                bForwards[whichThrottle].setSelected(true);
                bReverses[whichThrottle].setSelected(false);
                bForwards[whichThrottle].setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bReverses[whichThrottle].setTypeface(null, Typeface.NORMAL);
                bForwards[whichThrottle].setEnabled(true);
                if ((getSpeed(whichThrottle) > 0) && (!dirChangeWhileMoving)) {
                    bReverses[whichThrottle].setEnabled(false);
                }
            }
        }

    }

    // indicate requested direction using the button typeface
    void showDirectionRequest(int whichThrottle, int direction) {
        showDirectionIndication(whichThrottle, direction);
    }

    boolean changeActualOrTargetDirectionIfAllowed(int whichThrottle, int direction, boolean buttonsAreReversed) {
        boolean result;
        if (!isSemiRealisticThrottle) {
            if ((getDirection(whichThrottle) == direction_type.FORWARD)) {
                result = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
            } else {
                result = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
            }
        } else {
            if (getTargetDirection(whichThrottle) == direction_type.FORWARD) {
                result = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
            } else {
                result = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
            }
            showTargetDirectionIndication(whichThrottle);
        }
        return result;
    }

    //
    // processes a direction change if allowed by related preferences and current speed
    // returns true if throttle direction matches the requested direction
    boolean changeDirectionIfAllowed(int whichThrottle, int direction) {
        if ((getDirection(whichThrottle) != direction) && isChangeDirectionAllowed(whichThrottle)) {
            // set speed to zero on direction change while moving if the preference is set
            if (stopOnDirectionChange && (getSpeed(whichThrottle) != 0)) {
                speedUpdateAndNotify(whichThrottle, 0);
            }

            showDirectionRequest(whichThrottle, direction);        // update requested direction indication
            setEngineDirection(whichThrottle, direction, false);   // update direction for each engine on this throttle
            if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
        }
        return (getDirection(whichThrottle) == direction);
    }

    boolean isChangeDirectionAllowed(int whichThrottle) {
        // check whether direction change is permitted
        boolean isAllowed = false;
        if (getConsist(whichThrottle).isActive()) {
            if (stopOnDirectionChange || dirChangeWhileMoving || (getSpeed(whichThrottle) == 0))
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

    // only used for the 'Special' function label matching  AND you have custom Function Labels
    // also used for DCC-EX functions
    int setFunctionButtonState(int whichThrottle, int function, boolean downPress) {
        int isLatching = consist_function_latching_type.NA;
//        Consist con = mainapp.consists[whichThrottle];

        if (((mainapp.prefAlwaysUseDefaultFunctionLabels) && (mainapp.prefConsistFollowRuleStyle.contains(consist_function_rule_style_type.SPECIAL)))
                || (mainapp.prefOverrideWiThrottlesFunctionLatching)
                || (mainapp.isDCCEX)
        ) {
            int doPress = -1;
//            boolean doRelease = false;
            if ((mainapp.function_consist_latching.get(function) == null)
                    || (mainapp.function_consist_latching.get(function).equals(consist_function_latching_type.IS_NOT_LATCHING_TEXT))) {
                isLatching = consist_function_latching_type.NO;
                if (downPress) {
                    doPress = 1; //down
                } else {
                    doPress = 2; //up
                }
            } else { //latching
                isLatching = consist_function_latching_type.YES;
                if (!downPress) { //only process the release
                    doPress = 0;
                }
            }

            Button b;
            b = functionMaps[whichThrottle].get(function);

            if (b != null) {
                if (((!b.isPressed()) && (doPress == 0)) || (doPress == 1)) {
                    b.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                    b.setPressed(true);
                } else if (((b.isPressed()) && (doPress == 0)) || (doPress == 2)) {
                    b.setTypeface(null, Typeface.NORMAL);
                    b.setPressed(false);
                }
            }
        }
        return isLatching;
    }

    private boolean isSelectLocoAllowed(int whichThrottle) {
        // check whether loco change is permitted
        return !getConsist(whichThrottle).isActive() || locoSelectWhileMoving || (getSpeed(whichThrottle) == 0);
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    void startSelectLocoActivity(int whichThrottle) {
        bSelects[whichThrottle].setPressed(true);
        try {
            Intent intent = new Intent(this, select_loco.class);
            intent.putExtra("sWhichThrottle", mainapp.throttleIntToString(whichThrottle));
            selectLocoActivityLauncher.launch(intent);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        } catch (Exception ex) {
            Log.d(threaded_application.applicationName, activityName + ": startSelectLocoActivity() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
        }
    }

    private void handleSelectLocoActivityResult(@NonNull Intent data, int resultCode) {
        Log.d(threaded_application.applicationName, activityName + ": handleSelectLocoActivityResult() " );

        if (resultCode == activity_outcome_type.RESULT_LOCO_EDIT) {
            activityConsistUpdate(data.getExtras());
        }

        int newThrottle = 0;
        try {
            newThrottle = mainapp.throttleCharToInt(data.getCharExtra("whichThrottle", ' '));
            overrideThrottleNames[newThrottle] = data.getStringExtra("overrideThrottleName");
        } catch (RuntimeException e) {
            Log.e(threaded_application.applicationName, activityName + ": handleSelectLocoActivityResult failed: " + e.getMessage());
        }

        if ((getConsist(newThrottle) != null) && (!getConsist(newThrottle).isActive())) {
            setNextActiveThrottle();
        } else {
            if (IS_ESU_MCII) {
                esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.STEADY_FLASH, true);
            }
            whichVolume = newThrottle;
            setVolumeIndicator();
            setActiveThrottle(whichVolume);
            mainapp.EsuMc2FirstServerUpdate[whichVolume] = true;
        }

        for (int i = 0; i < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES; i++) {
            if (mainapp.consists != null && mainapp.consists[i] != null && !mainapp.consists[i].isActive()) {
                if(ipls != null) ipls.stopAllSoundsForLoco(i);
            }
            soundsShowHideDeviceSoundsButton(i);
        }
    }

    void startGamepadTestActivity(int gamepadNo) {
        if (prefGamepadTestEnforceTesting) {
            mainapp.gamePadDeviceIdsTested[gamepadNo] = 0;
            try {
                Intent intent = new Intent().setClass(this, gamepad_test.class);
                intent.putExtra("whichGamepadNo", Integer.toString(gamepadNo));
                tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST);
                gamepadTestActivityLauncher.launch(intent);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d(threaded_application.applicationName, activityName + ": startGamepadTestActivity() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
            }
        } else { // don't bother doing the test if the preference is set not to
            mainapp.gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_GOOD;
        }
    }

    private void handleGamepadTestActivityResult(@NonNull Intent data, int resultCode) {
        Log.d(threaded_application.applicationName, activityName + ": handleGamepadTestActivityResult() ");

        String whichGamepadNo = data.getStringExtra("whichGamepadNo");
        if (whichGamepadNo != null) {
            int result = Integer.parseInt(whichGamepadNo.substring(1, 2));
            int gamepadNo = Integer.parseInt(whichGamepadNo.substring(0, 1));
            switch (result) {
                case gamepad_test_type.PASS:
                    mainapp.gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_GOOD;
                    tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST_COMPLETE);
                    break;
                case gamepad_test_type.SKIPPED:
                    mainapp.gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_GOOD;
                    tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST_SKIPPED);
                    break;
                case gamepad_test_type.FAIL:
                    mainapp.gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_BAD;
                    tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST_FAIL);
                    break;
                case gamepad_test_type.RESET:
                    mainapp.gamepadCount = 0;
                    for (int i = 0; i < mainapp.gamePadDeviceIds.length; i++) {
                        mainapp.gamePadDeviceIds[i] = 0;
                        mainapp.gamePadDeviceIdsTested[i] = 0;
                    }
                    mainapp.gamepadFullReset();
                    mainapp.setGamepadTestMenuOption(overflowMenu, mainapp.gamepadCount);
                    setGamepadIndicator();
                    tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST_RESET);
                    break;
                default:
                    Log.e(threaded_application.applicationName, activityName + ": OnActivityResult(ACTIVITY_GAMEPAD_TEST) invalid result!");
            }
        }
    }

    // Edit the Consist Lights
    void startConsistLightsEditActivity(int whichThrottle) {
        if (prefConsistLightsLongClick) {  // only allow the editing in the consist lights if the preference is set
            try {
                Intent intent = new Intent().setClass(this, ConsistLightsEdit.class);
                intent.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));
                consistLightsEditActivityLauncher.launch(intent);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d(threaded_application.applicationName, activityName + ": startConsistLightsEditActivity() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
            }
        }
    }

    private void handleConsistLightsEditActivityResult() {
        Log.d(threaded_application.applicationName, activityName + ": handleConsistLightsEditActivityResult() ");
        // nothing to do
    }

    void startConsistEditActivity(int whichThrottle) {
        try {
            Intent intent = new Intent().setClass(this, ConsistEdit.class);
            intent.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));
            consistEditActivityLauncher.launch(intent);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        } catch (Exception ex) {
            Log.d(threaded_application.applicationName, activityName + ": startConsistEditActivity() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
        }
    }

    private void handleConsistEditActivityResult(@NonNull Intent data, int resultCode) {
        Log.d(threaded_application.applicationName, activityName + ": handleConsistEditActivityResult() ");

        if (resultCode == activity_outcome_type.RESULT_CON_EDIT)
            activityConsistUpdate(data.getExtras());

        setAllFunctionLabelsAndListeners();
        setLabels();
        soundsShowHideAllMuteButtons();
    }

    void startDeviceSoundsSettingsActivity() {
        try {
            Intent intent = new Intent().setClass(this, DeviceSoundsSettings.class);
            deviceSoundsSettingsActivityLauncher.launch(intent);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        } catch (Exception ex) {
            Log.d(threaded_application.applicationName, activityName + ": startConsistEditActivity() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
        }
    }

    private void handleDeviceSoundsSettingsActivityResult() {
        Log.d(threaded_application.applicationName, activityName + ": handleDeviceSoundsSettingsActivityResult() ");

        loadSounds();
    }

    void startSettingsActivity() {
        try {
            Intent intent = new Intent(this, SettingsActivity.class);
            settingsActivityLauncher.launch(intent);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        } catch (Exception ex) {
            Log.d(threaded_application.applicationName, activityName + ": startSettingsActivity() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
        }
    }

    private void handleSettingsActivityResult(int resultCode) {
        Log.d(threaded_application.applicationName, activityName + ": handleSettingsActivityResult() " );

        if (resultCode == activity_outcome_type.RESULT_GAMEPAD) { // gamepad pref changed
            // update tone generator volume
            if (tg != null) {
                tg.release();
                try {
                    tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                            threaded_application.getIntPrefValue(prefs, "prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));
                } catch (RuntimeException e) {
                    Log.e(threaded_application.applicationName, activityName + ": onActivityResult(): new ToneGenerator failed. Runtime Exception, OS " + Build.VERSION.SDK_INT + " Message: " + e);
                }
            }
            // update GamePad Support
            if (gamePadKeyLoader!=null) setGamepadKeys();
        }
        if (resultCode == activity_outcome_type.RESULT_ESUMCII) { // ESU MCII pref change
            // update zero trim values
            updateEsuMc2ZeroTrim();
        }
        // in case the preference has changed but the current screen does not support the number selected.
        setThrottleNumLimits();

        getKidsTimerPrefs();
        if (prefKidsTimer.equals(PREF_KIDS_TIMER_NONE)) {
            kidsTimerActions(kids_timer_action_type.DISABLED, 0);
        }

        redrawVerticalSliders();
        redrawSliders();
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    void disableButtons(int whichThrottle) {
        enableDisableButtons(whichThrottle, true);
    }

    void enableDisableButtons(int whichThrottle) {
        enableDisableButtons(whichThrottle, false);
    }

    void applySpeedRelatedOptions() {
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            applySpeedRelatedOptions(throttleIndex);
        }  // repeat for all throttles
    }

    void applySpeedRelatedOptions(int whichThrottle) {
        Button bForward = bForwards[whichThrottle];
        Button bReverse = bReverses[whichThrottle];
        Button bSelect = bSelects[whichThrottle];
        boolean tIsEnabled = llThrottleLayouts[whichThrottle].isEnabled();
        int dir = dirs[whichThrottle];

        if (getConsist(whichThrottle).isActive()) {
            boolean dirChangeAllowed = tIsEnabled && isChangeDirectionAllowed(whichThrottle);
            boolean locoChangeAllowed = tIsEnabled && isSelectLocoAllowed(whichThrottle);

            if (kidsTimerRunning == kids_timer_action_type.RUNNING) {
                locoChangeAllowed = false;
                if (!prefKidsTimerEnableReverse) {
                    dirChangeAllowed = false;
                }
            }

            if (dirChangeAllowed) {
                bForward.setEnabled(true);
                bReverse.setEnabled(true);
            } else {
                if (dir == 1) {
                    if (!directionButtonsAreCurrentlyReversed(whichThrottle)) {
                        bForward.setEnabled(true);
                        bReverse.setEnabled(false);
                    } else {
                        bForward.setEnabled(false);
                        bReverse.setEnabled(true);
                    }
                } else {
                    if (!directionButtonsAreCurrentlyReversed(whichThrottle)) {
                        bForward.setEnabled(false);
                        bReverse.setEnabled(true);
                    } else {
                        bForward.setEnabled(true);
                        bReverse.setEnabled(false);
                    }
                }
            }
            bSelect.setEnabled(locoChangeAllowed);
        } else {
            bForward.setEnabled(false);
            bReverse.setEnabled(false);
            bSelect.setEnabled(true);
        }
    }

    void enableDisableButtons(int whichThrottle, boolean forceDisable) {
        boolean newEnabledState = false;
        // avoid index and null crashes
        if (mainapp.consists == null || whichThrottle >= mainapp.consists.length
                || bForwards[whichThrottle] == null) {
            return;
        }
        if (!forceDisable) { // avoid index crash, but may simply push to next line
            newEnabledState = mainapp.consists[whichThrottle].isActive(); // set false if lead loco is not assigned
        }
        bForwards[whichThrottle].setEnabled(newEnabledState);
        bStops[whichThrottle].setEnabled(newEnabledState);
        if ((bPauses != null) && (bPauses[whichThrottle] != null)) {
            bPauses[whichThrottle].setEnabled(newEnabledState);
        }
        if ((kidsTimerRunning == kids_timer_action_type.RUNNING)
                && (!prefKidsTimerEnableReverse)) {
            bReverses[whichThrottle].setEnabled(false);
        } else {
            bReverses[whichThrottle].setEnabled(newEnabledState);
        }
        tvSpeedLabels[whichThrottle].setEnabled(newEnabledState);
        tvSpeedValues[whichThrottle].setEnabled(newEnabledState);
        bLeftSpeeds[whichThrottle].setEnabled(newEnabledState);
        bRightSpeeds[whichThrottle].setEnabled(newEnabledState);
        enableDisableButtonsForView(functionButtonViewGroups[whichThrottle], newEnabledState);
        soundsShowHideDeviceSoundsButton(whichThrottle);
        showHideSpeedLimitAndPauseButtons(whichThrottle);
        if (!newEnabledState) {
            sbs[whichThrottle].setProgress(0); // set slider to 0 if disabled
            if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
        }
        sbs[whichThrottle].setEnabled(newEnabledState);

        if (bLimitSpeeds[whichThrottle] != null)
            bLimitSpeeds[whichThrottle].setEnabled(newEnabledState);
        if (bPauseSpeeds[whichThrottle] != null)
            bPauseSpeeds[whichThrottle].setEnabled(newEnabledState);

        soundsEnableDisableDeviceSoundsButton(whichThrottle, newEnabledState);

    } // end of enableDisableButtons

    // helper function to enable/disable all children for a group
    void enableDisableButtonsForView(ViewGroup vg, boolean newEnabledState) {
        // implemented in derived class, but called from this class
    }

    // update the appearance of all function buttons
    void setAllFunctionStates(int whichThrottle) {
        // implemented in derived class, but called from this class
    }

    // update a function button appearance based on its state
    void set_function_state(int whichThrottle, int function) {
        // implemented in derived class, but called from this class
    }

    /*
     * future use: displays the requested function state independent of (in addition to) feedback state
     * todo: need to handle momentary buttons somehow
     */
    void set_function_request(int whichThrottle, int function, int reqState) {
        // Log.d(threaded_application.applicationName, activityName + ": set_function_request");
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

    boolean canChangeVolumeIndicatorOnTouch(boolean isSpeedButtonOrSlider) {
        // implemented in derived class, but called from this class
        return false;
    }

    void adjustThrottleHeights() {
        // implemented in derived class, but called from this class
    }

    private void clearVolumeAndGamepadAdditionalIndicators() {
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            if (throttleIndex < bSelects.length) {
                if ((prefSelectedLocoIndicator.equals(selected_loco_indicator_type.NONE))
                        || (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.GAMEPAD))) {
                    bSelects[throttleIndex].setActivated(false);
                }
                if ((prefSelectedLocoIndicator.equals(selected_loco_indicator_type.NONE))
                        || (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.VOLUME))) {
                    bSelects[throttleIndex].setHovered(false);
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void setSelectedLocoAdditionalIndicator(int whichThrottle, boolean isVolume) {

        if (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.NONE)) return;

        if ((isVolume) && (((prefSelectedLocoIndicator.equals(selected_loco_indicator_type.BOTH)) || (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.VOLUME))))) { // note: 'Volume' option is no longer available
            if (!prefDisableVolumeKeys) { // don't set the indicators if the volume keys are disabled the preferences
                for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
                    bSelects[throttleIndex].setActivated(whichThrottle == throttleIndex);
                }
            }
        }
        if ((!isVolume) && (((prefSelectedLocoIndicator.equals(selected_loco_indicator_type.BOTH)) || (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.GAMEPAD))))) {
            for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
                bSelects[throttleIndex].setHovered(whichThrottle == throttleIndex);
            }
        }
    }

    // Set the original volume indicator a small 'v' near the speed
    protected void setVolumeIndicator() {
        // hide or display volume control indicator based on variable
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            tvVolumeIndicators[throttleIndex].setText("");
            if (!prefDisableVolumeKeys) { // don't set the indicators if the volume keys are disabled
                tvVolumeIndicators[whichVolume].setText(VOLUME_INDICATOR);
                setSelectedLocoAdditionalIndicator(whichVolume, true);
            }
        }
        // Ensure ESU MCII tracks selected throttle
        if (IS_ESU_MCII && !isSemiRealisticThrottle) {
            threaded_application.extendedLogging(activityName + ": setVolumeIndicator(): ESU_MCII: Throttle changed to: " + whichVolume);
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
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            if (mainapp.gamePadThrottleAssignment[throttleIndex] != -1) {
                tvGamePads[throttleIndex].setText(String.valueOf(mainapp.gamePadThrottleAssignment[throttleIndex]));
            } else {
                tvGamePads[throttleIndex].setText("");
            }

            if (mainapp.gamePadThrottleAssignment[throttleIndex] == 1) {
                setSelectedLocoAdditionalIndicator(throttleIndex, false);
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE, throttleIndex, false
                        , whichLastGamepad1
                        , getDisplaySpeedFromCurrentSliderPosition(throttleIndex, true)
                        , 0
                        , getConsistAddressString(throttleIndex));
                isSet = true;
            }
        }
        if (!isSet) {
            setSelectedLocoAdditionalIndicator(-1, false);
        }
    }

    private int setNextActiveThrottle() {
        return setNextActiveThrottle(false);
    }

    private int setNextActiveThrottle(boolean feedbackSound) {
        int i;
        int index = -1;

        // find current Volume throttle
        for (i = 0; i < mainapp.prefNumThrottles; i++) {
            if (i == whichVolume) {
                index = i;
                break;
            }
        }
        // find next active throttle
        i = index + 1;
        while (i != index) {                        // check until we get back to current Volume throttle
            if (i >= mainapp.prefNumThrottles) {                // wrap
                i = 0;
            } else {
                int whichThrottle = i;
                if (getConsist(whichThrottle).isActive()) { // found next active throttle
                    whichVolume = whichThrottle;
                    setVolumeIndicator();
                    setSelectedLocoAdditionalIndicator(whichThrottle, true);
                    adjustThrottleHeights();
                    mainapp.whichThrottleLastTouch = whichThrottle;
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
        return whichVolume;
    }

    // if the preference is set, set the specific active throttle for the volume keys based on the throttle used
    protected void setActiveThrottle(int whichThrottle) {
        setActiveThrottle(whichThrottle, false);
    }
    protected void setActiveThrottle(int whichThrottle, boolean isSpeedButtonOrSlider) {
        if (tvVolumeIndicators[0] != null) { // if it is null assume that the page is not fully drawn yet
            if (prefVolumeKeysFollowLastTouchedThrottle) {
                if (canChangeVolumeIndicatorOnTouch(isSpeedButtonOrSlider)) {
                    if (whichVolume != whichThrottle) {
                        whichVolume = whichThrottle;
                        setVolumeIndicator();
                        setSelectedLocoAdditionalIndicator(whichThrottle, true);
                        adjustThrottleHeights();
                    }
                }
            }
            mainapp.whichThrottleLastTouch = whichThrottle;
        }
    }


    private void setGamepadKeys() {
        gamePadKeyLoader.loadGamepadKeys();
        gamePadKeys = gamePadKeyLoader.getGamePadKeys();
        gamePadKeys_Up = gamePadKeyLoader.getGamePadKeys_Up();
    }

    /** @noinspection UnusedReturnValue*/ //
    private int swapToNextAvailableThrottleForGamePad(int fromThrottle, boolean quiet) {
        int whichThrottle = -1;
        int index;
        index = fromThrottle - 1;

        // need to count through all the throttle, but need to start from the current one, not zero
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            index++;
            if (index >= mainapp.prefNumThrottles) {
                index = 0;
            }
            if (mainapp.gamePadIdsAssignedToThrottles[index] == 0) {  // unassigned
                if (getConsist(index).isActive()) { // found next active throttle
                    if (mainapp.gamePadIdsAssignedToThrottles[index] <= 0) { //not currently assigned
                        whichThrottle = index;
                        break;  // done
                    }
                }
            }
        }

        if (whichThrottle >= 0) {
            mainapp.gamePadIdsAssignedToThrottles[whichThrottle] = mainapp.gamePadIdsAssignedToThrottles[fromThrottle];
            mainapp.gamePadThrottleAssignment[whichThrottle] = mainapp.gamePadThrottleAssignment[fromThrottle];
            mainapp.gamePadIdsAssignedToThrottles[fromThrottle] = 0;
            mainapp.gamePadThrottleAssignment[fromThrottle] = -1;
            setGamepadIndicator();
        }

        if (whichThrottle == -1) {
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
    private int findWhichGamePadEventIsFrom(int eventDeviceId, String eventDeviceName, int eventKeyCode) {
        int whichGamePad = -2;  // default to the event not from a gamepad
        int whichGamePadDeviceId = -1;
        int j;

        if (eventDeviceId >= 0) { // event is from a gamepad (or at least not from a screen touch)
            whichGamePad = -1;  // gamepad

            int reassigningGamepad = -1;
            int i;

            // set for only one but the device id has changed - probably turned off then on
            if ((mainapp.gamepadCount == 1) && (mainapp.prefGamepadOnlyOneGamepad) && (mainapp.gamePadDeviceIds[0] != eventDeviceId)) {
                for (int k = 0; k < mainapp.prefNumThrottles; k++) {
                    if (mainapp.gamePadIdsAssignedToThrottles[k] == mainapp.gamePadDeviceIds[0]) {
                        mainapp.gamePadIdsAssignedToThrottles[k] = eventDeviceId;
                        break;
                    }
                }
                mainapp.gamePadDeviceIds[0] = eventDeviceId;
                mainapp.gamePadDeviceNames[0] = eventDeviceName;
                mainapp.gamePadDeviceIdsTested[0] = GAMEPAD_BAD;
            }

            // find out if this gamepad is already assigned
            for (i = 0; i < mainapp.prefNumThrottles; i++) {
                if (mainapp.gamePadIdsAssignedToThrottles[i] == eventDeviceId) {
                    if (getConsist(i).isActive()) { //found the throttle and it is active
                        whichGamePad = i;
                    } else { // currently assigned to this throttle, but the throttle is not active
                        whichGamePad = i;
                        mainapp.gamePadIdsAssignedToThrottles[i] = 0;
                        reassigningGamepad = mainapp.gamePadThrottleAssignment[i];
                        mainapp.gamePadThrottleAssignment[i] = -1;
                        setGamepadIndicator(); // need to clear the indicator
                    }
                    break;
                }
            }

            if (whichGamePad == -1) { //didn't find it OR is known, but unassigned

                for (j = 0; j < mainapp.gamepadCount; j++) {
                    if (mainapp.gamePadDeviceIds[j] == eventDeviceId) { // known, but unassigned
                        whichGamePadDeviceId = j;
                        break;
                    }
                }
                if (whichGamePadDeviceId == -1) { // previously unseen gamepad
                    mainapp.gamepadCount++;
                    mainapp.gamePadDeviceIds[mainapp.gamepadCount - 1] = eventDeviceId;
                    mainapp.gamePadDeviceNames[mainapp.gamepadCount - 1] = eventDeviceName;
                    whichGamePadDeviceId = mainapp.gamepadCount - 1;

                    mainapp.setGamepadTestMenuOption(overflowMenu, mainapp.gamepadCount);

                    startGamepadTestActivity(mainapp.gamepadCount - 1);

                }

                for (i = 0; i < mainapp.prefNumThrottles; i++) {
                    if (mainapp.gamePadIdsAssignedToThrottles[i] == 0) {  // throttle is not assigned a gamepad
                        if (getConsist(i).isActive()) { // found next active throttle
                            mainapp.gamePadIdsAssignedToThrottles[i] = eventDeviceId;
                            if (reassigningGamepad == -1) { // not a reassignment
                                mainapp.gamePadThrottleAssignment[i] = GAMEPAD_INDICATOR[whichGamePadDeviceId];
                            } else { // reassigning
                                mainapp.gamePadThrottleAssignment[i] = reassigningGamepad;
                            }
                            whichGamePad = i;
                            setGamepadIndicator();
                            break;  // done
                        }
                    }
                }
            } else {
                for (j = 0; j < mainapp.gamepadCount; j++) {
                    if (mainapp.gamePadDeviceIds[j] == eventDeviceId) { // known, but unassigned
                        whichGamePadDeviceId = j;
                        break;
                    }
                }
                if (mainapp.gamePadDeviceIdsTested[whichGamePadDeviceId] == GAMEPAD_BAD) {  // gamepad is known but failed the test last time
                    startGamepadTestActivity(whichGamePadDeviceId);
                }
            }
        }
        if (mainapp.gamepadCount > 0) {
            mainapp.usingMultiplePads = true;
        }

        return whichGamePad;
    }

    // load the gamepad support on first use
    private void loadGamepadAndKeyboardHandlers(boolean force) {
        threaded_application.extendedLogging(activityName + ": loadGamepadAndKeyboardHandlers() " + (force ? "Force" : "") );
        if ( (force) || (gamepadEventHandler == null) || (keyboardEventHandler == null) ) {
            gamePadKeyLoader = new GamePadKeyLoader(this, mainapp, prefs,
                    prefGamePadButtons, gamePadKeys, gamePadKeys_Up, null);

            gamepadEventHandler = new GamepadEventHandler(mainapp, this, mainapp.maxThrottlesCurrentScreen,
                    gamePadKeys, gamePadKeys_Up, prefGamePadButtons, prefGamePadDoublePressStop);
            keyboardEventHandler = new KeyboardEventHandler(mainapp, this, mainapp.maxThrottlesCurrentScreen);

            setGamepadKeys();
        }
    }

    @Override
    public void gamepadEventNotificationHandler(int event, int val, int repeatCnt,
                                                 int whichThrottle,
                                                 boolean isConsistActiveOnThrottle,
                                                 int whichGamePadIsEventFrom) {
        threaded_application.extendedLogging(activityName + ": gamepadEventNotificationHandler() event: " + event);

        keyboardEventNotificationHandler(event, val, repeatCnt,
                        whichThrottle,
                        isConsistActiveOnThrottle,
                        whichGamePadIsEventFrom);
    }

    @Override
    public void keyboardEventNotificationHandler(int event, int val, int repeatCnt,
                                                 int whichThrottle,
                                                 boolean isConsistActiveOnThrottle,
                                                 int whichGamePadIsEventFrom) {
        threaded_application.extendedLogging(activityName + ": keyboardEventNotificationHandler() event: " + event);
        if (!getConsist(whichThrottle).isActive()) {
            GamepadFeedbackSound(true);
            return;
        }

        int playFeedbackSound = 0;  // -1=don't play . 0=play success . 1=play fail

        switch (event) {
            case gamepad_or_keyboard_event_type.STOP: {
                if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN) {
                    disablePauseSpeed(whichThrottle);
                }
                speedUpdateAndNotifyCombined(whichThrottle, 0);
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                        , getMaxSpeed(whichThrottle)
                        , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                        , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                        , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                        , isSemiRealisticThrottle
                        , "");
                break;
            }
            case gamepad_or_keyboard_event_type.ESTOP: {
                speedUpdateAndNotifyCombined(0);
                mainapp.sendEStopMsg();
                break;
            }
            case gamepad_or_keyboard_event_type.STOP_ALL: {
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    if (isPauseSpeeds[throttleIndex] == pause_speed_type.TO_RETURN) {
                        disablePauseSpeed(throttleIndex);
                    }
                }
                speedUpdateAndNotifyCombined(0);
                break;
            }
            case gamepad_or_keyboard_event_type.NEXT_THROTTLE: {
                if (mainapp.usingMultiplePads && whichGamePadIsEventFrom >= 0) {
                    keyboardThrottle = swapToNextAvailableThrottleForGamePad(whichGamePadIsEventFrom, false);
                } else {
                    keyboardThrottle = setNextActiveThrottle(true);
                }
                break;
            }
            case gamepad_or_keyboard_event_type.FORWARD: {
                boolean dirChangeFailed = !changeActualOrTargetDirectionIfAllowed(whichThrottle,
                        direction_type.FORWARD,
                        gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle));
                playFeedbackSound = (dirChangeFailed ? 2 : 1);
                break;
            }
            case gamepad_or_keyboard_event_type.REVERSE: {
                boolean dirChangeFailed = !changeActualOrTargetDirectionIfAllowed(whichThrottle,
                        direction_type.REVERSE,
                        gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle));
                playFeedbackSound = (dirChangeFailed ? 2 : 1);
                break;
            }
            case gamepad_or_keyboard_event_type.TOGGLE_DIRECTION: {
                boolean  dirChangeFailed = !changeActualOrTargetDirectionIfAllowed(whichThrottle,
                        getDirection(whichThrottle) == direction_type.FORWARD ? direction_type.REVERSE : direction_type.FORWARD,
                        false);
                playFeedbackSound = (dirChangeFailed ? 2 : 1);
                break;
            }
            case gamepad_or_keyboard_event_type.INCREASE_SPEED_START: {
                mGamepadAutoIncrement = true;
                if (!isSemiRealisticThrottle) {
                    gamepadRepeatUpdateHandler.post(new throttle.GamepadRptUpdater(whichThrottle, 1));
                } else { // semi-realistic throttle variant
                    gamepadRepeatUpdateHandler.post(new throttle.SemiRealisticGamepadRptUpdater(whichThrottle, 1));
                }
                break;
            }
            case gamepad_or_keyboard_event_type.DECREASE_SPEED_START: {
                mGamepadAutoDecrement = true;
                if (!isSemiRealisticThrottle) {
                    gamepadRepeatUpdateHandler.post(new throttle.GamepadRptUpdater(whichThrottle, 1));
                } else { // semi-realistic throttle variant
                    gamepadRepeatUpdateHandler.post(new throttle.SemiRealisticGamepadRptUpdater(whichThrottle, 1));
                }
                break;
            }
            case gamepad_or_keyboard_event_type.SRT_NEUTRAL: {
                boolean dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.NEUTRAL);
                showTargetDirectionIndication(whichThrottle);
                playFeedbackSound = (dirChangeFailed ? 2 : 1);
                break;
            }
            case gamepad_or_keyboard_event_type.SRT_BRAKE_INCREASE: {
                incrementBrakeSliderPosition(whichThrottle);
                break;
            }
            case gamepad_or_keyboard_event_type.SRT_BRAKE_DECREASE: {
                decrementBrakeSliderPosition(whichThrottle);
                break;
            }
            case gamepad_or_keyboard_event_type.SRT_LOAD_INCREASE: {
                incrementLoadSliderPosition(whichThrottle);
                break;
            }
            case gamepad_or_keyboard_event_type.SRT_LOAD_DECREASE: {
                decrementLoadSliderPosition(whichThrottle);
                break;
            }
            case gamepad_or_keyboard_event_type.IPLS_MUTE: {
                soundsIsMuted[whichThrottle] = !soundsIsMuted[whichThrottle];
                setSoundButtonState(bMutes[whichThrottle], soundsIsMuted[whichThrottle]);
                if(ipls!=null) ipls.muteUnmuteCurrentSounds(whichThrottle, soundsIsMuted[whichThrottle]);
                break;
            }
            case gamepad_or_keyboard_event_type.IPLS_BELL_TOGGLE: {
                boolean rslt = !mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BELL - 1];
                doDeviceButtonSound(whichThrottle, sounds_type.BELL);
                setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_BELL][whichThrottle], rslt);
                mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BELL - 1] = rslt;
                playFeedbackSound = -1;
                break;
            }
            case gamepad_or_keyboard_event_type.IPLS_HORN_START: {
                if (!mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1]) {
                    doDeviceButtonSound(whichThrottle, sounds_type.HORN);
                    setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle], true);
                    mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1] = true;
                }
                playFeedbackSound = -1;
                break;
            }
            case gamepad_or_keyboard_event_type.IPLS_HORN_END: {
                doDeviceButtonSound(whichThrottle, sounds_type.HORN);
                setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle], false);
                mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1] = false;
                playFeedbackSound = -1;
                break;
            }
            case gamepad_or_keyboard_event_type.IPLS_HORN_SHORT_START: {
                if (!mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1]) {
                    doDeviceButtonSound(whichThrottle, sounds_type.HORN_SHORT);
                    setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle], true);
                    mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1] = true;
                }
                playFeedbackSound = -1;
                break;
            }
            case gamepad_or_keyboard_event_type.IPLS_HORN_SHORT_END: {
                doDeviceButtonSound(whichThrottle, sounds_type.HORN_SHORT);
                setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle], false);
                mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1] = false;
                playFeedbackSound = -1;
                break;
            }
            case gamepad_or_keyboard_event_type.LIMIT_SPEED_TOGGLE: {
                if (!isLimitSpeeds[whichThrottle]) {
                    gamepadBeep(beep_type.LIMIT_SPEED_START);
                } else {
                    gamepadBeep(beep_type.LIMIT_SPEED_END);
                }
                limitSpeed(whichThrottle);
                break;
            }
            case gamepad_or_keyboard_event_type.PAUSE_SPEED_TOGGLE: {
                gamepadBeep(beep_type.PAUSE_SPEED_START);
                if (isPauseSpeeds[whichThrottle] == pause_speed_type.INACTIVE) {
                    gamepadBeep(beep_type.PAUSE_SPEED_START);
                } else {
                    gamepadBeep(beep_type.PAUSE_SPEED_END);
                }
                pauseSpeed(whichThrottle);
                break;
            }
            case gamepad_or_keyboard_event_type.SPEAK_SPEED: {
                tts.speakWords(tts_msg_type.GAMEPAD_CURRENT_SPEED, whichThrottle, true
                        , whichThrottle
                        , getDisplaySpeedFromCurrentSliderPosition(whichThrottle, true)
                        , getDirection(whichThrottle)
                        , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                        , isSemiRealisticThrottle
                        , getConsistAddressString(whichThrottle));
                break;
            }
            case gamepad_or_keyboard_event_type.FUNCTION_START: {
                if (mainapp.isDCCEX) {
                    lastGamepadFunction = val;
                    lastGamepadFunctionIsPressed = mainapp.function_states[whichThrottle][val];
                }
                doGamepadFunction(val, ACTION_DOWN, isConsistActiveOnThrottle, whichThrottle, repeatCnt);
                break;
            }
            case gamepad_or_keyboard_event_type.FUNCTION_END: {
                if ( (mainapp.isDCCEX) && (lastGamepadFunction == val) ){
                    doGamepadFunction(val, (lastGamepadFunctionIsPressed ? ACTION_DOWN : ACTION_UP), isConsistActiveOnThrottle, whichThrottle, repeatCnt, true);
                } else {
                    doGamepadFunction(val, ACTION_UP, isConsistActiveOnThrottle, whichThrottle, repeatCnt);
                }
                playFeedbackSound = -1;
                break;
            }
            case gamepad_or_keyboard_event_type.FUNCTION_FORCED_LATCH_START: {
                doGamepadFunction(val, ACTION_DOWN, isConsistActiveOnThrottle, whichThrottle, repeatCnt, true);
                break;
            }
            case gamepad_or_keyboard_event_type.FUNCTION_FORCED_LATCH_END: {
                doGamepadFunction(val, ACTION_UP, isConsistActiveOnThrottle, whichThrottle, repeatCnt, true);
                break;
            }
            case gamepad_or_keyboard_event_type.SPEED: {
                speedUpdateAndNotifyCombined(whichThrottle, val);
                break;
            }

            case gamepad_or_keyboard_event_type.NONE:
            default: {
                playFeedbackSound = -1;
            }
        }

        if ( (playFeedbackSound>=0) && (repeatCnt==0) ) {
            GamepadFeedbackSound(playFeedbackSound == 2);
        }
    }

    void doGamepadFunction(int fKey, int action, boolean isActive, int whichThrottle, int repeatCnt) {
        doGamepadFunction(fKey, action, isActive, whichThrottle, repeatCnt, false);
    }
    void doGamepadFunction(int fKey, int action, boolean isActive, int whichThrottle, int repeatCnt, boolean forceIsLatching) {
        threaded_application.extendedLogging(activityName + ": doGamepadFunction() : fKey: " + fKey + " action: " + action + " isActive: " + isActive);
        if (isActive && (repeatCnt == 0)) {
            String lab = mainapp.function_labels[whichThrottle].get(fKey);
            if (lab != null) {
                lab = lab.toUpperCase().trim();
            } else {
                if ((fKey < prefNumberOfDefaultFunctionLabels)
                        && (mainapp.function_labels_default.get(fKey) != null)) {
                    lab = mainapp.function_labels_default.get(fKey).toUpperCase().trim();
                } else {
                    lab = "BLANK";
                }
            }

            boolean leadOnly = false;
            boolean trailOnly = false;
            boolean followLeadFunction = false;

            int result = isFunctionLeadTrailFollow(whichThrottle, fKey, lab);
            if ((result == function_is_type.LEAD_ONLY) || (result == function_is_type.LEAD_AND_FOLLOW) || (result == function_is_type.LEAD_AND_TRAIL)) {
                leadOnly = true;
            }
            if ((result == function_is_type.TRAIL_ONLY) || (result == function_is_type.LEAD_AND_TRAIL)) {
                trailOnly = true;
            }
            if ((result == function_is_type.FOLLOW) || (result == function_is_type.LEAD_AND_FOLLOW)) {
                followLeadFunction = true;
            }

            int isLatching = setFunctionButtonState(whichThrottle, fKey, true);  //special handling for when using the default function labels, and one of 'Special' function following options
            if (forceIsLatching) isLatching = consist_function_latching_type.YES; // override if commanded

            if (action == ACTION_DOWN) {
                sendFunctionToConsistLocos(whichThrottle, fKey, lab, button_press_message_type.DOWN, leadOnly, trailOnly, followLeadFunction, isLatching, false, forceIsLatching);
            } else {
                sendFunctionToConsistLocos(whichThrottle, fKey, lab, button_press_message_type.UP, leadOnly, trailOnly, followLeadFunction, isLatching, false,  forceIsLatching);
            }
        }
    }

    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        threaded_application.extendedLogging(activityName + ": dispatchGenericMotionEvent() Joystick Event");

        if ( (!mainapp.prefGamePadType.equals(threaded_application.WHICH_GAMEPAD_MODE_NONE)) && (!mainapp.prefGamePadIgnoreJoystick) ) {

            loadGamepadAndKeyboardHandlers(false);
            boolean acceptEvent = true; // default to assuming that we will respond to the event

            int action;
            int whichThrottle;
            int repeatCnt = 0;
            int whichGamePadIsEventFrom;

            if (event != null) {
                action = event.getAction();
                whichGamePadIsEventFrom = findWhichGamePadEventIsFrom(event.getDeviceId(), event.getDevice().getName(), 0); // dummy eventKeyCode
                if ((whichGamePadIsEventFrom > -1) && (whichGamePadIsEventFrom < mainapp.gamePadDeviceIdsTested.length)
                        && mainapp.getGamePadIndexFromThrottleNo(whichGamePadIsEventFrom) < mainapp.gamePadDeviceIdsTested.length) { // the event came from a valid gamepad
                    if (mainapp.gamePadDeviceIdsTested[mainapp.getGamePadIndexFromThrottleNo(whichGamePadIsEventFrom)] != GAMEPAD_GOOD) { //if not, testing for this gamepad is not complete or has failed
                        acceptEvent = false;
                    }
                } else {
                    acceptEvent = false;
                }
            } else {
                action = externalGamepadAction;
                whichGamePadIsEventFrom = externalGamepadWhichGamePadIsEventFrom;
            }

            if (acceptEvent) {
                float xAxis;
                float yAxis;
                float xAxis2;
                float yAxis2;

                if (event != null) {
                    xAxis = Math.round(event.getAxisValue(MotionEvent.AXIS_X) * 10.0f) / 10.0f;
                    yAxis = Math.round(event.getAxisValue(MotionEvent.AXIS_Y) * 10.0f) / 10.0f;
                    xAxis2 = Math.round(event.getAxisValue(MotionEvent.AXIS_Z) * 10.0f) / 10.0f;
                    yAxis2 = Math.round(event.getAxisValue(MotionEvent.AXIS_RZ) * 10.0f) / 10.0f;

                    if ((xAxis != 0) || (yAxis != 0)) {
                        action = ACTION_DOWN;
                    } else {
                        action = ACTION_UP;

                        // retrieve the previous DPAD direction press
                        xAxis = mainapp.gamePadLastxAxis[whichGamePadIsEventFrom];
                        yAxis = mainapp.gamePadLastyAxis[whichGamePadIsEventFrom];
                        xAxis2 = mainapp.gamePadLastxAxis2[whichGamePadIsEventFrom];
                        yAxis2 = mainapp.gamePadLastyAxis2[whichGamePadIsEventFrom];
                    }
                } else {
                    xAxis = externalGamepadXAxis;
                    yAxis = externalGamepadYAxis;
                    xAxis2 = externalGamepadXAxis2;
                    yAxis2 = externalGamepadYAxis2;
                }

                threaded_application.extendedLogging(activityName + ": dispatchGenericMotionEvent() Joystick Event x: " + xAxis + "y: " + yAxis + "x2: " + xAxis2 + "y2: " + yAxis2 );

                if ((mainapp.usingMultiplePads) && (whichGamePadIsEventFrom >= -1)) { // we have multiple gamepads AND the preference is set to make use of them AND the event came for a gamepad
                    if (whichGamePadIsEventFrom >= 0) {
                        whichThrottle = whichGamePadIsEventFrom;
                    } else {
                        GamepadFeedbackSound(true);
                        return (true);
                    }
                } else {
                    whichThrottle = whichVolume;  // work out which throttle the volume keys are currently set to control... and use that one
                    mainapp.whichThrottleLastTouch = whichThrottle;
                }

                boolean isActive = getConsist(whichThrottle).isActive();

                if (action == ACTION_UP) {// event received by this activity
                    mGamepadAutoIncrement = false;
                    mGamepadAutoDecrement = false;
                    GamepadFeedbackSoundStop();
                    threaded_application.extendedLogging(activityName + ": dispatchGenericMotionEvent(): ACTION_UP"
                            + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                            + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
                    );
                }

                boolean rslt = false;
                int buttonAction =-1;
                if (yAxis == -1) { // DPAD Up Button
//                    performButtonAction(5, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    buttonAction = 5;
                    rslt = true;

                } else if (yAxis == 1) { // DPAD Down Button
//                    performButtonAction(7, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    buttonAction = 7;
                    rslt = true;

                } else if (xAxis == -1) { // DPAD Left Button
//                    performButtonAction(8, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    buttonAction = 8;
                    rslt = true;

                } else if (xAxis == 1) { // DPAD Right Button
//                    performButtonAction(6, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    buttonAction = 6;
                    rslt = true;
                }

                if (yAxis2 == -1) { // DPAD2 Up Button
//                    performButtonAction(5, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    buttonAction = 5;
                    rslt = true;

                } else if (yAxis2 == 1) { // DPAD2 Down Button
//                    performButtonAction(7, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    buttonAction = 7;
                    rslt = true;

                } else if (xAxis2 == -1) { // DPAD2 Left Button
//                    performButtonAction(8, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    buttonAction = 8;
                    rslt = true;

                } else if (xAxis2 == 1) { // DPAD2 Right Button
//                    performButtonAction(6, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    buttonAction = 6;
                    rslt = true;
                }

                if (buttonAction>0) {
                    if (keyboardThrottle < 0) keyboardThrottle = whichThrottle;
                    gamepadEventHandler.handleGamepadEvent(buttonAction, action,
                            repeatCnt, whichThrottle,
                            getConsist(whichThrottle).isActive(),
                            keyboardThrottle,
                            getConsist(keyboardThrottle).isActive(),
                            isSemiRealisticThrottle,
                            whichGamePadIsEventFrom);
                }

                mainapp.gamePadLastxAxis[whichGamePadIsEventFrom] = xAxis;
                mainapp.gamePadLastyAxis[whichGamePadIsEventFrom] = yAxis;
                mainapp.gamePadLastxAxis2[whichGamePadIsEventFrom] = xAxis2;
                mainapp.gamePadLastyAxis2[whichGamePadIsEventFrom] = yAxis2;

                if (rslt) {
                    return (true); // stop processing this key
                }

            } else { // event is from a gamepad that has not finished testing. Ignore it
                return (true); // stop processing this key
            }
        }
        if (event != null) {
            return super.dispatchGenericMotionEvent(event);
        } else {
            return true;
        }
    }

    // listener for physical keyboard events
    // used to support the gamepad and physical keyboard events   DPAD and keystroke events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        threaded_application.extendedLogging(activityName + ": dispatchKeyEvent() gamepad or keyboard event");

        loadGamepadAndKeyboardHandlers(false);

        boolean isExternal = false;
        if (event != null) {
            InputDevice iDev = getDevice(event.getDeviceId());
            if (iDev != null && iDev.toString().contains("Location: external"))
                isExternal = true;
        } else { // received from another activity (Turnouts or Routes)
            isExternal = true;
        }

        if (isExternal) { // if has come from the phone itself, don't try to process it here
            if (!mainapp.prefGamePadType.equals(threaded_application.WHICH_GAMEPAD_MODE_NONE)) { // respond to the gamepad and keyboard inputs only if the preference is set

                boolean acceptEvent = true; // default to assuming that we will respond to the event

                int action;
                int keyCode;
                boolean isShiftPressed;
                int repeatCnt;
                int whichThrottle;
                int whichGamePadIsEventFrom;

                if (event != null) {
                    action = event.getAction();
                    keyCode = event.getKeyCode();
                    isShiftPressed = event.isShiftPressed();
                    repeatCnt = event.getRepeatCount();
                    whichGamePadIsEventFrom = findWhichGamePadEventIsFrom(event.getDeviceId(), event.getDevice().getName(), event.getKeyCode());
                } else {
                    action = externalGamepadAction;
                    keyCode = externalGamepadKeyCode;
                    isShiftPressed = externalGamepadIsShiftPressed;
                    repeatCnt = externalGamepadRepeatCnt;
                    whichGamePadIsEventFrom = externalGamepadWhichGamePadIsEventFrom;
                }

                if ((whichGamePadIsEventFrom > -1) && (whichGamePadIsEventFrom < mainapp.gamePadDeviceIdsTested.length)) { // the event came from a gamepad
                    if (mainapp.gamePadDeviceIdsTested[mainapp.getGamePadIndexFromThrottleNo(whichGamePadIsEventFrom)] != GAMEPAD_GOOD) { //if not, testing for this gamepad is not complete or has failed
                        acceptEvent = false;
                    }
                } else {
                    acceptEvent = false;
                }

                if (acceptEvent) {
                    if ((mainapp.usingMultiplePads) && (whichGamePadIsEventFrom >= -1)) { // we have multiple gamepads AND the preference is set to make use of them AND the event came for a gamepad
                        if (whichGamePadIsEventFrom >= 0) {
                            whichThrottle = whichGamePadIsEventFrom;
                        } else {
                            GamepadFeedbackSound(true);
                            return (true);
                        }
                    } else {
                        whichThrottle = whichVolume;  // work out which throttle the volume keys are currently set to control... and use that one
                        mainapp.whichThrottleLastTouch = whichThrottle;
                    }

                    boolean isActive = getConsist(whichThrottle).isActive();

                    if (keyCode != 0) {
                        threaded_application.extendedLogging(activityName + ": dispatchKeyEvent(): keycode " + keyCode + " action " + action + " repeat " + repeatCnt);
                    }

                    if (!mainapp.prefGamePadType.equals("Keyboard")) {
                        if (action == ACTION_UP) {
                            mGamepadAutoIncrement = false;
                            mGamepadAutoDecrement = false;
                            GamepadFeedbackSoundStop();
                            threaded_application.extendedLogging(activityName + ": dispatchKeyEvent (not Keyboard): ACTION_UP"
                                    + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                                    + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
                            );
                        } else {
                            threaded_application.extendedLogging(activityName + ": dispatchKeyEvent (not Keyboard): ACTION_DOWN");
                        }

                        int rslt = -1;
                        int actionNo = -1;
                        for (int i = 0; i <= 20; i++) {
                            if (keyCode == gamePadKeys[i]) {
                                rslt = i;
                                actionNo = BUTTON_ACTION_NUMBERS[i];
                                break;
                            }
                        }
                        if ((rslt >= 1) && (rslt <= 16)) {
//                            performButtonAction(actionNo, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                            if (keyboardThrottle < 0) keyboardThrottle = whichThrottle;
                            gamepadEventHandler.handleGamepadEvent(actionNo, action,
                                    repeatCnt, whichThrottle,
                                    getConsist(whichThrottle).isActive(),
                                    keyboardThrottle,
                                    getConsist(keyboardThrottle).isActive(),
                                    isSemiRealisticThrottle,
                                    whichGamePadIsEventFrom);

                            return (true); // stop processing this key

                        } else if ((rslt >= 17) && (rslt <= 21)) {
//                            performKeyboardKeyAction(keyCode, action, false, repeatCnt, whichThrottle, whichGamePadIsEventFrom);
                            if (keyboardThrottle < 0) keyboardThrottle = whichThrottle;
                            keyboardEventHandler.handleKeyboardEvent(keyCode, action,
                                    false, repeatCnt, whichThrottle,
                                    getConsist(whichThrottle).isActive(),
                                    keyboardThrottle,
                                    getConsist(keyboardThrottle).isActive(),
                                    isSemiRealisticThrottle,
                                    whichGamePadIsEventFrom);

                            return (true); // stop processing this key
                        }

                        if (whichGamePadIsEventFrom < mainapp.gamePadDeviceIdsTested.length) {// checked all the code and didn't find it
                            return (true); // ignore it
                        }
                    } else { // keyboard
                        if (action == ACTION_UP) {
                            mGamepadAutoIncrement = false;
                            mGamepadAutoDecrement = false;
                            GamepadFeedbackSoundStop();
                        }
                        threaded_application.extendedLogging(activityName + ": dispatchKeyEvent: ACTION" + ((action == ACTION_UP) ? "UP" : "DOWN")
                                + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                                + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
                        );
//                        performKeyboardKeyAction(keyCode, action, isShiftPressed, repeatCnt, whichThrottle, whichGamePadIsEventFrom);
                        if (keyboardThrottle < 0) keyboardThrottle = whichThrottle;
                        keyboardEventHandler.handleKeyboardEvent(keyCode, action,
                                isShiftPressed, repeatCnt, whichThrottle,
                                getConsist(whichThrottle).isActive(),
                                keyboardThrottle,
                                getConsist(keyboardThrottle).isActive(),
                                isSemiRealisticThrottle,
                                whichGamePadIsEventFrom);

                        return (true); // stop processing this key
                    }
                } else { // event is from a gamepad that has not finished testing. Ignore it
                    return (true); // stop processing this key
                }
            }
        } else if (IS_ESU_MCII) {
            // Process ESU MCII keys
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            int repeatCnt = event.getRepeatCount();
            boolean isActive = getConsist(whichVolume).isActive();

            if (keyCode != 0) {
                threaded_application.extendedLogging(activityName + ": dispatchKeyEvent(): ESU_MCII: keycode " + keyCode + " action " + action + " repeat " + repeatCnt);
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
                        Log.d(threaded_application.applicationName, activityName + ": dispatchKeyEvent(): ESU_MCII: Unrecognised keyCode: " + keyCode);

                }
            }
        }
        if (event != null) {
            return super.dispatchKeyEvent(event);
        } else {
            return true;
        }
    }

    void GamepadIncrementSpeed(int whichThrottle, int stepMultiplier) {
        threaded_application.extendedLogging(activityName + ": GamepadIncrementSpeed()");
        incrementSpeed(whichThrottle, speed_commands_from_type.GAMEPAD, stepMultiplier);
        GamepadFeedbackSound(atMaxSpeed(whichThrottle) || atMinSpeed(whichThrottle));
        tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                , getMaxSpeed(whichThrottle)
                , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                , isSemiRealisticThrottle
                , "");
    }

    void GamepadDecrementSpeed(int whichThrottle, int stepMultiplier) {
        threaded_application.extendedLogging(activityName + ": GamepadDecrementSpeed()");
        decrementSpeed(whichThrottle, speed_commands_from_type.GAMEPAD, stepMultiplier);
        GamepadFeedbackSound(atMinSpeed(whichThrottle) || atMaxSpeed(whichThrottle));
        tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                , getMaxSpeed(whichThrottle)
                , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                , isSemiRealisticThrottle
                , "");
    }

    void GamepadFeedbackSound(boolean invalidAction) {
        if (mainapp.appIsFinishing) return;
        // the tone generator is not compatible with the ipls
        if ((!mainapp.prefDeviceSounds[0].equals("none")) || (!mainapp.prefDeviceSounds[1].equals("none"))) {
            if (invalidAction) {
                gamepadBeep(beep_type.NAK);
            } else {
                gamepadBeep(beep_type.ACK);
            }
        } else {

            if (tg != null) {
                if (invalidAction)
                    tg.startTone(ToneGenerator.TONE_PROP_NACK);
                else
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
            }
        }
    }

    void gamepadBeep(int whichBeep) {
        if (mainapp.prefGamePadFeedbackVolume == 0) return;

        if (mainapp._mediaPlayer != null) {
            mainapp._mediaPlayer.reset();     // reset stops and release on any state of the player
        }

        mainapp._mediaPlayer = MediaPlayer.create(this, mainapp.gamepadBeepIds[whichBeep]);

        if (mainapp._mediaPlayer!=null) {
            float volumeFloat = ((float) mainapp.prefGamePadFeedbackVolume) / 100.0f;

            mainapp._mediaPlayer.setVolume(volumeFloat, volumeFloat);
            mainapp._mediaPlayer.start();
        }
    }

    void GamepadFeedbackSoundStop() {
        if ((!mainapp.prefDeviceSounds[0].equals("none")) || (!mainapp.prefDeviceSounds[1].equals("none")))
            return;

        if (tg != null) {
            tg.stopTone();
        }
    }

    // For gamepad speed buttons.
    private class GamepadRptUpdater implements Runnable {
        int whichThrottle;
        int stepMultiplier = 1;

        private GamepadRptUpdater(int WhichThrottle, int StepMultiplier) {
            whichThrottle = WhichThrottle;
            stepMultiplier = StepMultiplier;

            threaded_application.extendedLogging(activityName + ": GamepadRptUpdater(): WhichThrottle: " + whichThrottle
                    + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                    + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
            );

            try {
                REP_DELAY = Integer.parseInt(prefs.getString("prefGamePadSpeedArrowsThrottleRepeatDelay", getApplicationContext().getResources().getString(R.string.prefGamePadSpeedButtonsRepeatDefaultValue)));
            } catch (NumberFormatException ex) {
                REP_DELAY = 300;
            }
        }

        @Override
        public void run() {
            threaded_application.extendedLogging(activityName + ": GamepadRptUpdater(): run(): WhichThrottle: " + whichThrottle
                    + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                    + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
            );
            if (mainapp.appIsFinishing) {
                return;
            }

            if (mGamepadAutoIncrement) {
                GamepadIncrementSpeed(whichThrottle, stepMultiplier);
                gamepadRepeatUpdateHandler.postDelayed(new GamepadRptUpdater(whichThrottle, stepMultiplier), REP_DELAY);
            } else if (mGamepadAutoDecrement) {
                GamepadDecrementSpeed(whichThrottle, stepMultiplier);
                gamepadRepeatUpdateHandler.postDelayed(new GamepadRptUpdater(whichThrottle, stepMultiplier), REP_DELAY);
            }
        }
    }

    // For volume speed buttons.
    private class VolumeKeysRptUpdater implements Runnable {
        int whichThrottle;

        private VolumeKeysRptUpdater(int WhichThrottle) {
            threaded_application.extendedLogging(activityName + ": VolumeKeysRptUpdater(): WhichThrottle: " + whichThrottle
                    + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                    + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
            );
            whichThrottle = WhichThrottle;

            try {
                REP_DELAY = Integer.parseInt(prefs.getString("prefSpeedButtonsRepeat", "100"));
            } catch (NumberFormatException ex) {
                REP_DELAY = 100;
            }
        }

        @Override
        public void run() {
            threaded_application.extendedLogging(activityName + ": VolumeKeysRptUpdater(): run(): WhichThrottle: " + whichThrottle
                    + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                    + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
            );
            if (mVolumeKeysAutoIncrement) {
                incrementSpeed(whichThrottle, speed_commands_from_type.VOLUME);
                volumeKeysRepeatUpdateHandler.postDelayed(new VolumeKeysRptUpdater(whichThrottle), REP_DELAY);
            } else if (mVolumeKeysAutoDecrement) {
                decrementSpeed(whichThrottle, speed_commands_from_type.VOLUME);
                volumeKeysRepeatUpdateHandler.postDelayed(new VolumeKeysRptUpdater(whichThrottle), REP_DELAY);
            }
        }
    }

    /**
     * For ESU MCII buttons.
     * However, it seems that the ESU MCII buttons report UP immediately even if held down
     * so this, right now, is rather superfluous...
     */
    private class EsuMc2ButtonRptUpdater implements Runnable {
        int whichThrottle;

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
                incrementSpeed(whichThrottle, speed_commands_from_type.VOLUME);
                esuButtonRepeatUpdateHandler.postDelayed(new EsuMc2ButtonRptUpdater(whichThrottle), REP_DELAY);
            } else if (esuButtonAutoDecrement) {
                decrementSpeed(whichThrottle, speed_commands_from_type.VOLUME);
                gamepadRepeatUpdateHandler.postDelayed(new EsuMc2ButtonRptUpdater(whichThrottle), REP_DELAY);
            }
        }
    }

    // Callback for ESU MCII throttle knob events
    private final ThrottleFragment.OnThrottleListener esuOnThrottleListener = new ThrottleFragment.OnThrottleListener() {

        @Override
        public void onButtonDown() {
            threaded_application.extendedLogging(activityName + ": ThrottleListener(): onButtonDown(): ESU_MCII: Knob button down for throttle " + whichVolume);
            if (!isScreenLocked) {
                if (!isEsuMc2KnobEnabled) {
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onButtonDown(): ESU_MCII: Knob disabled - direction change ignored");
                } else if (prefEsuMc2EndStopDirectionChange) {
                    threaded_application.extendedLogging(activityName + ": ThrottleListener(): onButtonDown(): ESU_MCII: Attempting to switch direction");
                    changeActualOrTargetDirectionIfAllowed(whichVolume,
                            getDirection(whichVolume) == direction_type.FORWARD ? direction_type.REVERSE : direction_type.FORWARD,
                            false);
                    speedUpdateAndNotify(whichVolume, 0, false);
                } else {
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onButtonDown(): ESU_MCII: Direction change option disabled - do nothing");
                    speedUpdateAndNotify(whichVolume, 0, false);
                }
            } else {
                Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onButtonDown(): ESU_MCII: Screen locked - do nothing");
            }
        }

        @Override
        public void onButtonUp() {
            threaded_application.extendedLogging(activityName + ": ThrottleListener(): onButtonUp(): ESU_MCII: Knob button up for throttle " + whichVolume);
        }

        @Override
        public void onPositionChanged(int knobPos) {
            int speed;
            if (!isScreenLocked) {
                if (!isEsuMc2KnobEnabled) {
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onPositionChanged(): ESU_MCII: Disabled knob position moved for throttle " + whichVolume);
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onPositionChanged():ESU_MCII: Nothing updated");
                } else if (getConsist(whichVolume).isActive() && !isEsuMc2Stopped) {
                    speed = esuThrottleScales[whichVolume].positionToStep(knobPos);
                    threaded_application.extendedLogging(activityName + ": ThrottleListener(): onPositionChanged():ESU_MCII: Knob position changed for throttle " + whichVolume);
                    threaded_application.extendedLogging(activityName + ":  ThrottleListener(): onPositionChanged():ESU_MCII: New knob position: " + knobPos + " ; speedStep: " + speed);
                    if (!isSemiRealisticThrottle) {
                        speedUpdateAndNotify(whichVolume, speed, false); // No need to move knob
                    } else {
                        // set the target speed based on the new Knob position
                        int vSpeed = (int) ( (float) speed / 126 * (float) getNewSemiRealisticThrottleSliderNotches());
                        semiRealisticThrottleSliderPositionUpdate(whichVolume,vSpeed);
                        stopSemiRealisticThrottleSpeedButtonRepeater(whichVolume);
                        mSemiRealisticAutoIncrementOrDecrement[whichVolume] = auto_increment_or_decrement_type.OFF;
                        setTargetSpeed(whichVolume, vSpeed);
//                        Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onPositionChanged():ESU_MCII: SRT  speed: " + speed);
//                        Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onPositionChanged():ESU_MCII: SRT vSpeed: " + vSpeed);
                    }
                } else {
                    // Ignore knob movements for stopped or inactive throttles
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onPositionChanged():ESU_MCII: Knob position moved for " + (isEsuMc2Stopped ? "stopped" : "inactive") + " throttle " + whichVolume);
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onPositionChanged():ESU_MCII: Nothing updated");
                }
            } else {
                Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onPositionChanged():ESU_MCII: Screen locked - do nothing");
            }
        }

        // The new physical sliders position from 0 to 255.
        @Override
        public void onPhysicalSliderPositionChanged(int position) {
            float pcntPos = Math.round((((float) position) / 255 * 100));
            Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onPhysicalSliderPositionChanged(): position:" + position +  "Pcnt: " + pcntPos);
            if (mainapp.esuMc2BrakePosition != pcntPos) {
                mainapp.esuMc2BrakePosition = pcntPos;
                if (mainapp.prefEsuMc2SliderType.equals("dim")) {
                    setScreenBrightness(255 - position);
                } else {
                    if (IS_ESU_MCII) {
                        setEsuMc2DecoderBrake(mainapp.whichThrottleLastTouch);
                        if (mainapp.prefEsuMc2SliderType.equals("srt")) {
                            setBrakeSliderPositionPcnt(whichVolume, 100-pcntPos);
                        }
                    }
                }
            }
        }
    };

    void setEsuMc2DecoderBrake(int whichThrottle) {
        if (!mainapp.useEsuMc2DecoderBrakes) return;

        boolean[] functionIsOn;
        int[] functionShouldBeOnOff;

        functionIsOn = new boolean[threaded_application.MAX_FUNCTIONS];
        functionShouldBeOnOff = new int[threaded_application.MAX_FUNCTIONS];

        for (int k=0; k<threaded_application.MAX_FUNCTIONS; k++) {
            functionIsOn[k] = mainapp.function_states[whichThrottle][k];
            functionShouldBeOnOff[k] = -1; // -1 don't care, 1 = on, 0 = off
        }

        double brakePcnt = mainapp.esuMc2BrakePosition;
        int foundBrakeLevel = -1;
        for (int i = 0; i <= 2; i++) {  // cycle through the thresholds from low to high
            if (mainapp.esuMc2BrakeLevels[i] >= 0) {  // ignore it if it is set to -1
                for (int j = 0; j < 5; j++) { // set them to bee turned off
                    if (mainapp.esuMc2BrakeFunctions[i][j] >= 0) {
                        functionShouldBeOnOff[mainapp.esuMc2BrakeFunctions[i][j]] = 0;
                    }
                }
            }
        }
        for (int i = 2; i >= 0; i--) {  // cycle through the thresholds from high to low
            if (mainapp.esuMc2BrakeLevels[i] >= 0) {  // ignore it if it is set to -1
                if ((brakePcnt >= mainapp.esuMc2BrakeLevels[i]) && (foundBrakeLevel < 0)) {
                    for (int j = 0; j < 5; j++) { // set them to be turned on
                        if (mainapp.esuMc2BrakeFunctions[i][j] >= 0) {
                            functionShouldBeOnOff[mainapp.esuMc2BrakeFunctions[i][j]] = 1;
                        }
                    }
                    foundBrakeLevel = i;
                    mainapp.esuMc2BrakeActive[whichThrottle][i] = true;
                } else {
                    mainapp.esuMc2BrakeActive[whichThrottle][i] = false;
                }
            }
        }

        for (int k=0; k<threaded_application.MAX_FUNCTIONS; k++) {
            if (functionShouldBeOnOff[k] == 1) {  // should be on
                if (!functionIsOn[k]) { // is not on
                    sendFunctionToConsistLocos(whichThrottle, k, "", button_press_message_type.DOWN, false, false, true, consist_function_latching_type.NO, true);
                } // else already on so leave it alone
            } else if (functionShouldBeOnOff[k] == 0) { // should be off
                if (functionIsOn[k]) { // is on
                    sendFunctionToConsistLocos(whichThrottle, k, "", button_press_message_type.UP, false, false, true, consist_function_latching_type.NO, true);
                } // else already off so leave it alone
            } // else don't care
        }
    }

    // Callback for ESU MCII stop button
    private final StopButtonFragment.OnStopButtonListener esuOnStopButtonListener = new StopButtonFragment.OnStopButtonListener() {
        private int origSpeed;
        private long timePressed;
        private boolean wasLongPress = false;
        private int delay;
        private CountDownTimer buttonTimer;

        @Override
        public void onStopButtonDown() {
            if (!getConsist(whichVolume).isActive()) {
                Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onStopButton(): ESU_MCII: Stop button down for inactive throttle " + whichVolume);
                return;
            }
            Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onStopButton(): ESU_MCII: Stop button down for throttle " + whichVolume);
            if (!isEsuMc2Stopped) {
                origSpeed = getSpeed(whichVolume);
                timePressed = System.currentTimeMillis();
                Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): onStopButton(): ESU_MCII: Speed value was: " + origSpeed);
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
            delay = threaded_application.getIntPrefValue(prefs, "prefEsuMc2StopButtonDelay",
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
                Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): ESU_MCII: Stop button up for inactive throttle " + whichVolume);
                return;
            }
            if (fromTimer) {
                Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): ESU_MCII: Stop button timer finished for throttle " + whichVolume);
            } else {
                Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): ESU_MCII: Stop button up for throttle " + whichVolume);
            }
            if (isEsuMc2Stopped) {
                if (fromTimer || System.currentTimeMillis() - timePressed > delay) {
                    // It's a long initial press so record this
                    wasLongPress = true;
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): ESU_MCII: Stop button press was long - long flash Red LED");
                    esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.LONG_FLASH);
                    esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
                    // Set all throttles to zero
                    for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
                        set_stop_button(throttleIndex, true);
                        speedUpdateAndNotify(throttleIndex, 0);
                        setEnabledEsuMc2ThrottleScreenButtons(throttleIndex, false);
                    }
                    isEsuMc2AllStopped = true;
                } else {
                    wasLongPress = false;
                    if (prefEsuMc2StopButtonShortPress) {
                        Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): ESU_MCII: Stop button press was short - short flash Red LED");
                        esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.QUICK_FLASH);
                        esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
                        setEnabledEsuMc2ThrottleScreenButtons(whichVolume, false);
                    } else {
                        Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): ESU_MCII: Stop button press was short but action disabled");
                        isEsuMc2Stopped = !isEsuMc2Stopped;
                        esuMc2Led.revertLEDStates();
                    }
                }
            } else {
                if (!wasLongPress) {
                    if (prefEsuMc2StopButtonShortPress) {
                        Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): SU_MCII: Revert speed value to: " + origSpeed);
                        set_stop_button(whichVolume, false);
                        speedUpdateAndNotify(whichVolume, origSpeed);
                        setEnabledEsuMc2ThrottleScreenButtons(whichVolume, true);
                    } else {
                        Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): ESU_MCII: Stop button press was short but revert action disabled");
                    }
                } else {
                    Log.d(threaded_application.applicationName, activityName + ": ThrottleListener(): doStopButtonUp(): ESU_MCII: Resume control without speed revert");
                    origSpeed = 0;
                    for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
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
    protected void setEsuThrottleKnobPosition(int whichThrottle, int speed) {
        if (!IS_ESU_MCII) return;

        Log.d(threaded_application.applicationName, activityName + ": setEsuThrottleKnobPosition(): ESU_MCII: Request to update knob position for throttle " + whichThrottle);
        if (whichThrottle == whichVolume) {
            int knobPos;
            knobPos = esuThrottleScales[whichThrottle].stepToPosition(speed);
            Log.d(threaded_application.applicationName, activityName + ": setEsuThrottleKnobPosition(): ESU_MCII: Update knob position for throttle " + mainapp.throttleIntToString(whichThrottle));
            Log.d(threaded_application.applicationName, activityName + ": setEsuThrottleKnobPosition(): ESU_MCII: New knob position: " + knobPos + " ; speedStep: " + speed);
            try {
                esuThrottleFragment.moveThrottle(knobPos);
            } catch (IllegalArgumentException ex) {
                Log.e(threaded_application.applicationName, activityName + ": setEsuThrottleKnobPosition(): ESU_MCII: Problem moving throttle " + ex.getMessage());
            }
        } else {
            Log.d(threaded_application.applicationName, activityName + ": setEsuThrottleKnobPosition(): ESU_MCII: This throttle not selected for control by knob");
        }
    }

    private void updateEsuMc2ZeroTrim() {
        if (!IS_ESU_MCII) return;

        int zeroTrim = threaded_application.getIntPrefValue(prefs, "prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
        Log.d(threaded_application.applicationName, activityName + ": updateEsuMc2ZeroTrim(): ESU_MCII: Update zero trim for throttle to: " + zeroTrim);

        if (esuThrottleFragment == null) return; // not initialised yet

        // first the knob
        esuThrottleFragment.setZeroPosition(zeroTrim);

        // now throttle scales
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            esuThrottleScales[throttleIndex] = new ThrottleScale(zeroTrim, esuThrottleScales[throttleIndex].getStepCount());
        }
    }

    private void performEsuMc2ButtonAction(int buttonNo, int action, boolean isActive, int whichThrottle, int repeatCnt) {
        Log.d(threaded_application.applicationName, activityName + ": performEsuMc2ButtonAction(): ESU_MCII: Button no: " + buttonNo);

        if (isEsuMc2Stopped) {
            Log.d(threaded_application.applicationName, activityName + ": performEsuMc2ButtonAction(): ESU_MCII: Device button presses whilst stopped ignored");
            mainapp.safeToast(R.string.toastEsuMc2NoButtonPresses, Toast.LENGTH_SHORT);
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
                        changeActualOrTargetDirectionIfAllowed(whichThrottle,
                                direction_type.FORWARD,
                                false);
                    }
                    break;
                case DIRECTION_REVERSE:
                    if (!isScreenLocked && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                        changeActualOrTargetDirectionIfAllowed(whichThrottle,
                                direction_type.REVERSE,
                                false);
                    }
                    break;
                case DIRECTION_TOGGLE:
                    if (!isScreenLocked && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                        boolean dirChangeFailed = !changeActualOrTargetDirectionIfAllowed(whichThrottle,
                                getDirection(whichThrottle) == direction_type.FORWARD ? direction_type.REVERSE : direction_type.FORWARD,
                                gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle));
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
                case FN00:
                case FN01:
                case FN02:
                case FN03:
                case FN04:
                case FN05:
                case FN06:
                case FN07:
                case FN08:
                case FN09:
                case FN10:
                case FN11:
                case FN12:
                case FN13:
                case FN14:
                case FN15:
                case FN16:
                case FN17:
                case FN18:
                case FN19:
                case FN20:
                case FN21:
                case FN22:
                case FN23:
                case FN24:
                case FN25:
                case FN26:
                case FN27:
                case FN28:
                case FN29:
                case FN30:
                case FN31:
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
                case NO_ACTION:
                default:
                    // Do nothing
                    break;
            }
        }
    }

    private void setEnabledEsuMc2ThrottleScreenButtons(int whichThrottle, boolean enabled) {

        // Disables/enables seekbar and speed buttons
        bLeftSpeeds[whichThrottle].setEnabled(enabled);
        bRightSpeeds[whichThrottle].setEnabled(enabled);
        sbs[whichThrottle].setEnabled(enabled);
    }

    /**
     * Display the ESU MCII knob action button if configured
     *
     * @param menu the menu upon which the action should be shown
     */
    private void displayEsuMc2KnobMenuButton(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.esu_mc2_knob_button);
        if (menuItem == null) return;
        if (!IS_ESU_MCII) return;

        if (prefs.getBoolean("prefEsuMc2KnobButtonDisplay", false)) {
            mainapp.actionBarIconCountThrottle++;
            menuItem.setVisible(true);
        } else {
            menuItem.setVisible(false);
        }
        mainapp.setEsuMc2KnobButton(menu, findViewById(R.id.esu_mc2_knob_button), isEsuMc2KnobEnabled);

    }

//    /**
//     * Set the state of the ESU MCII knob action button/menu entry
//     *
//     * @param menu the menu upon which the action is shown
//     */
//    public void setEsuMc2KnobButton(Menu menu) {
//        if (menu != null) {
//            if (isEsuMc2KnobEnabled) {
//                menu.findItem(R.id.esu_mc2_knob_button).setIcon(R.drawable.original_toolbar_button_esu_mc2_knob_on);
//            } else {
//                menu.findItem(R.id.esu_mc2_knob_button).setIcon(R.drawable.original_toolbar_button_esu_mc2_knob_off);
//            }
//        }
//    }

    /**
     * Toggle the state of the ESU MCII knob
     *
     * @param activity the requesting activity
     * @param menu     the menu upon which the entry/button should be updated
     */
    public void toggleEsuMc2Knob(Activity activity, Menu menu) {
        isEsuMc2KnobEnabled = !isEsuMc2KnobEnabled;
        mainapp.setEsuMc2KnobButton(menu, findViewById(R.id.esu_mc2_knob_button), isEsuMc2KnobEnabled);
    }


    // Listeners for the Stop buttons
    private class StopButtonTouchListener implements View.OnTouchListener {
        int whichThrottle;

        private StopButtonTouchListener(int newWhichThrottle) {
            whichThrottle = newWhichThrottle;
        }

        Runnable run = new Runnable() {
            @Override
            public void run() {
                if ( (prefStopButtonEStopOnLongPress) && (isStopButtonLongPress) ) {
                    mainapp.sendEStopOneThrottleMsg(whichThrottle);
                }
            }
        };

        void doStop() {
            if (prefStopButtonEStopOnLongPress)
                setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);

            set_stop_button(whichThrottle, true);

            if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_ZERO) {
                isPauseSpeeds[whichThrottle] = pause_speed_type.ZERO;
            } else if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN) {
                isPauseSpeeds[whichThrottle] = pause_speed_type.INACTIVE;
            } else if ((getSpeed(whichThrottle) == 0)
                    && (isPauseSpeeds[whichThrottle] == pause_speed_type.ZERO)) {
                disablePauseSpeed(whichThrottle);
            }

            speedUpdateAndNotify(whichThrottle, 0);
            set_stop_button(whichThrottle, false);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isStopButtonLongPress = true;
                if (prefStopButtonEStopOnLongPress)
                    stopButtonLongPressHandler.postDelayed(run, prefStopButtonLongPressDelay);

                doStop();

                v.playSoundEffect(SoundEffectConstants.CLICK);  // make the click sound once
                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
                mainapp.buttonVibration();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                isStopButtonLongPress = false;
                if (prefStopButtonEStopOnLongPress)
                    stopButtonLongPressHandler.removeCallbacks(run);
            }
            return true;
        }
    }

//    protected class StopButtonClickListener implements View.OnClickListener, View.OnLongClickListener {
//        int whichThrottle;
//
//        protected StopButtonClickListener(int newWhichThrottle) {
//            whichThrottle = newWhichThrottle;
//        }
//
//        @Override
//        public void onClick(View view) {
//            // if gesture in progress, skip button processing
//            if (gestureInProgress) {
//                return;
//            }
//            view.playSoundEffect(SoundEffectConstants.CLICK);
//            mainapp.buttonVibration();
//
//            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
//
//            set_stop_button(whichThrottle, true);
//
//            if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_ZERO) {
//                isPauseSpeeds[whichThrottle] = pause_speed_type.ZERO;
//            } else if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN) {
//                isPauseSpeeds[whichThrottle] = pause_speed_type.INACTIVE;
//            } else if ((getSpeed(whichThrottle) == 0)
//                    && (isPauseSpeeds[whichThrottle] == pause_speed_type.ZERO)) {
//                disablePauseSpeed(whichThrottle);
//            }
//
//            speedUpdateAndNotify(whichThrottle, 0);
//            set_stop_button(whichThrottle, false);
//        }
//
//        @Override
//        public boolean onLongClick(View view) {
//            // if gesture in progress, skip button processing
//            if (gestureInProgress) {
//                return (true);
//            }
//
//            view.playSoundEffect(SoundEffectConstants.CLICK);
//            mainapp.buttonVibration();
//
//            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
//
//            set_stop_button(whichThrottle, true);
//
//            if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_ZERO) {
//                isPauseSpeeds[whichThrottle] = pause_speed_type.ZERO;
//            } else if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN) {
//                isPauseSpeeds[whichThrottle] = pause_speed_type.INACTIVE;
//            } else if ((getSpeed(whichThrottle) == 0)
//                    && (isPauseSpeeds[whichThrottle] == pause_speed_type.ZERO)) {
//                disablePauseSpeed(whichThrottle);
//            }
//
//            if (prefStopButtonEStopOnLongPress) {
//                mainapp.sendEStopOneThrottleMsg(whichThrottle);
//            } else {
//                speedUpdateAndNotify(whichThrottle, 0);
//            }
//
//            speedUpdateAndNotify(whichThrottle, 0);
//            set_stop_button(whichThrottle, false);
//
//            return false;
//        }
//    }

    // Listeners for the Select Loco buttons
//    protected class SelectFunctionButtonTouchListener implements View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {
    protected class SelectFunctionButtonTouchListener implements View.OnClickListener, View.OnLongClickListener {
        int whichThrottle;

        protected SelectFunctionButtonTouchListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @Override
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (IS_ESU_MCII && isEsuMc2Stopped) {
                mainapp.safeToast(getApplicationContext().getResources().getString(R.string.toastEsuMc2NoLocoChange), Toast.LENGTH_SHORT);
            } else if (isSelectLocoAllowed(whichThrottle)) {
                // don't loco change while moving if the preference is set
                startSelectLocoActivity(whichThrottle); // pass throttle #
                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            } else {
                mainapp.safeToast(getApplicationContext().getResources().getString(R.string.toastLocoChangeNotAllowed), Toast.LENGTH_SHORT);
            }
            mainapp.buttonVibration();
        }

        @Override
        public boolean onLongClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            startConsistLightsEditActivity(whichThrottle);
            setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            mainapp.buttonVibration();
            return true;
        }

    }

    //listeners for the Limit Speed Button
    protected class LimitSpeedButtonTouchListener implements View.OnTouchListener {
        int whichThrottle;

        protected LimitSpeedButtonTouchListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                limitSpeed(whichThrottle);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mainapp.buttonVibration();
            }
            return false;
        }
    }

    protected void limitSpeed(int whichThrottle) {
        int maxThrottle = threaded_application.getIntPrefValue(prefs, "prefMaximumThrottle", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (maxThrottle * .01)); // convert from percent

        isLimitSpeeds[whichThrottle] = !isLimitSpeeds[whichThrottle];
        if (isLimitSpeeds[whichThrottle]) {
            bLimitSpeeds[whichThrottle].setSelected(true);
            limitSpeedSliderScalingFactors[whichThrottle] = 100 / ((float) prefLimitSpeedPercent);
            sbs[whichThrottle].setMax(Math.round(maxThrottle / limitSpeedSliderScalingFactors[whichThrottle]));
        } else {
            bLimitSpeeds[whichThrottle].setSelected(false);
            sbs[whichThrottle].setMax(maxThrottle);
        }

        speedChangeAndNotify(whichThrottle, 0);
        setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
    }

    //listeners for the increase/decrease speed buttons (not the slider)
    protected class ArrowSpeedButtonTouchListener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
        int whichThrottle;
        String arrowDirection;

        protected ArrowSpeedButtonTouchListener(int new_whichThrottle, String new_arrowDirection) {
            whichThrottle = new_whichThrottle;
            arrowDirection = new_arrowDirection;
        }

        @Override
        public boolean onLongClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (arrowDirection.equals(speed_button_type.RIGHT)) {
                mAutoIncrement[whichThrottle] = true;
            } else {
                mAutoDecrement[whichThrottle] = true;
            }
            repeatUpdateHandler.post(new RptUpdater(whichThrottle, 0));

            setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            mainapp.buttonVibration();
            return false;
        }

        @Override
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (arrowDirection.equals(speed_button_type.RIGHT)) {
                mAutoIncrement[whichThrottle] = false;
                incrementSpeed(whichThrottle, speed_commands_from_type.BUTTONS);
            } else {
                mAutoDecrement[whichThrottle] = false;
                decrementSpeed(whichThrottle, speed_commands_from_type.BUTTONS);
            }
            setActiveThrottle(whichThrottle, true);
            mainapp.buttonVibration();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && mAutoIncrement[whichThrottle]) {
                mAutoIncrement[whichThrottle] = false;
            }

            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && mAutoDecrement[whichThrottle]) {
                mAutoDecrement[whichThrottle] = false;
            }
            setActiveThrottle(whichThrottle, true);
            return false;
        }
    }

    // Listeners for the direction buttons
    private class DirectionButtonTouchListener implements View.OnTouchListener {
        int function;
        int whichThrottle;

        private DirectionButtonTouchListener(int new_function, int new_whichThrottle) {
            function = new_function;    // store these values for this button
            whichThrottle = new_whichThrottle;
        }

        private void doButtonPress() {
            mainapp.exitDoubleBackButtonInitiated = 0;

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
            setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            mainapp.buttonVibration();
        }

        Runnable run = new Runnable() {
            @Override
            public void run() {
                if (isDirectionButtonLongPress) {

                    if (isChangeDirectionAllowed(whichThrottle)) { // only respond to the long click if it is ok to change directions
                        doButtonPress();  //in case the the direction button long clicked is not the current direction change the direction first

                        if (prefSwapForwardReverseButtonsLongPress) {
                            //v.playSoundEffect(SoundEffectConstants.CLICK);
                            currentSwapForwardReverseButtons[whichThrottle] = !currentSwapForwardReverseButtons[whichThrottle];
                            setDirectionButtonLabels();
                            mainapp.safeToast(getApplicationContext().getResources().getString(R.string.toastDirectionButtonsSwapped), Toast.LENGTH_SHORT);
                        }
                        doButtonPress();
                    }

                    setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
                }
            }
        };

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isDirectionButtonLongPress = true;
                directionButtonLongPressHandler.postDelayed(run, prefDirectionButtonLongPressDelay);

                // Log.d(threaded_application.applicationName, activityName + ": onTouch(): direction " + function + " action " +

                v.playSoundEffect(SoundEffectConstants.CLICK);  // make the click sound once

                doButtonPress();
                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
                mainapp.buttonVibration();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                isDirectionButtonLongPress = false;
                directionButtonLongPressHandler.removeCallbacks(run);
            }
            return true;
        }
    }

    private int isFunctionLeadTrailFollow(int whichThrottle, int fKey, String lab) {
        boolean lead = false;
        boolean trail = false;
        boolean followLeadFunction = false;

        if (mainapp.prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.ORIGINAL)) {
            if (!lab.isEmpty()) {
                lead = (prefSelectiveLeadSound &&
                        (lab.contains(FUNCTION_BUTTON_LOOK_FOR_WHISTLE) || lab.contains(FUNCTION_BUTTON_LOOK_FOR_HORN) || lab.contains(FUNCTION_BUTTON_LOOK_FOR_BELL))
                        || lab.contains(FUNCTION_BUTTON_LOOK_FOR_HEAD)
                        || (lab.contains(FUNCTION_BUTTON_LOOK_FOR_LIGHT) && !lab.contains(FUNCTION_BUTTON_LOOK_FOR_REAR)));
                followLeadFunction = (lab.contains(FUNCTION_BUTTON_LOOK_FOR_LIGHT));
                trail = lab.contains(FUNCTION_BUTTON_LOOK_FOR_REAR);
            }
            if ((prefSelectiveLeadSound) && (fKey == 1) && (prefSelectiveLeadSoundF1)) {
                lead = true;
            }
            if ((prefSelectiveLeadSound) && (fKey == 2) && (prefSelectiveLeadSoundF2)) {
                lead = true;
            }
        }

        if ((lead) && (followLeadFunction)) {
            return function_is_type.LEAD_AND_FOLLOW;
        }
        if ((lead) && (trail)) {
            return function_is_type.LEAD_AND_TRAIL;
        }
        if (lead) {
            return function_is_type.LEAD_ONLY;
        }
        if (trail) {
            return function_is_type.TRAIL_ONLY;
        }
        if (followLeadFunction) {
            return function_is_type.FOLLOW;
        }
        return 0;

    }

    protected void sendFunctionToConsistLocos(int whichThrottle, int function, String lab, int buttonPressMessageType, boolean leadOnly, boolean trailOnly, boolean followLeadFunction, int isLatching) {
        sendFunctionToConsistLocos(whichThrottle, function, lab, buttonPressMessageType, leadOnly, trailOnly, followLeadFunction, isLatching, false, false);
    }
    protected void sendFunctionToConsistLocos(int whichThrottle, int function, String lab, int buttonPressMessageType, boolean leadOnly, boolean trailOnly, boolean followLeadFunction, int isLatching, boolean forceSemiRealistic) {
        sendFunctionToConsistLocos(whichThrottle, function, lab, buttonPressMessageType, leadOnly, trailOnly, followLeadFunction, isLatching, forceSemiRealistic, false);
    }
    protected void sendFunctionToConsistLocos(int whichThrottle, int function, String lab, int buttonPressMessageType, boolean leadOnly, boolean trailOnly, boolean followLeadFunction, int isLatching, boolean forceSemiRealistic, boolean forceIsLatching) {
        Log.d(threaded_application.applicationName, activityName + ": sendFunctionToConsistLocos(): whichThrottle: " + whichThrottle + " function: " + function + " buttonPressMessageType: " + buttonPressMessageType);
        Consist con;
        con = mainapp.consists[whichThrottle];

        String tempPrefConsistFollowRuleStyle = mainapp.prefConsistFollowRuleStyle;
        // if there is only one loco and we are not forcing the use of the Default Functions, then revert back to the ORIGINAL style
        if (((mainapp.prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.COMPLEX))
                || (mainapp.prefConsistFollowRuleStyle.contains(consist_function_rule_style_type.SPECIAL)))
                && (con.size() == 1)
                && (!mainapp.prefAlwaysUseDefaultFunctionLabels)) {

            tempPrefConsistFollowRuleStyle = consist_function_rule_style_type.ORIGINAL;
        }

        if (forceSemiRealistic) { // only used for the Sem-Realistic Throttle - ESU Decoder Brakes
            int newFnState = (buttonPressMessageType == button_press_message_type.UP) ? 0 : 1;
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + "*", function, newFnState);

        } else if (tempPrefConsistFollowRuleStyle.equals(consist_function_rule_style_type.ORIGINAL)) {

            String addr = "";
            if (leadOnly)
                addr = con.getLeadAddr();

            if (buttonPressMessageType == button_press_message_type.TOGGLE) {
                mainapp.toggleFunction(mainapp.throttleIntToString(whichThrottle) + addr, function);
            } else {
                if ( (!mainapp.prefOverrideWiThrottlesFunctionLatching) && (!forceIsLatching) ) {
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, buttonPressMessageType);
                } else {
                    boolean fnState = mainapp.function_states[whichThrottle][function];
                    int oldFnState = fnState ? 1 : 0;
                    int newFnState = fnState ? 0 : 1;

                    if (isLatching == consist_function_latching_type.YES) {
                        if (buttonPressMessageType == button_press_message_type.UP) {
                            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, oldFnState);
                            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, newFnState);
                            mainapp.function_states[whichThrottle][function] = (newFnState==1);
                        }
                    } else {
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, newFnState);
                        mainapp.function_states[whichThrottle][function] = (newFnState==1);
                    }
                }
            }

            if (followLeadFunction) {
                for (Consist.ConLoco l : con.getLocos()) {
                    if (!l.getAddress().equals(con.getLeadAddr())) {  // ignore the lead as we have already set it
                        if (l.isLightOn() == light_follow_type.FOLLOW) {
                            if (buttonPressMessageType == button_press_message_type.TOGGLE) {
                                mainapp.toggleFunction(mainapp.throttleIntToString(whichThrottle) + l.getAddress(), function);
                            } else {
                                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + l.getAddress(), function, buttonPressMessageType);
                            }
                        }
                    }
                }
            }

        } else {  //Complex or SpecialExact or SpecialPartial

            boolean fnState = mainapp.function_states[whichThrottle][function];
            int newFnState = -1;
            if (tempPrefConsistFollowRuleStyle.equals(consist_function_rule_style_type.COMPLEX)) { // if Complex, always activate the lead loco
                if (buttonPressMessageType == button_press_message_type.TOGGLE) {
                    mainapp.toggleFunction(mainapp.throttleIntToString(whichThrottle) + con.getLeadAddr(), function);
                } else {
                    if (!mainapp.isDCCEX) {
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + con.getLeadAddr(), function, buttonPressMessageType);
                    } else {
                        newFnState = fnState ? 0 : 1;
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + con.getLeadAddr(), function, newFnState);
                    }
                }
            }

            function = getFunctionNumber(con, function, lab);

            List<Integer> functionList = new ArrayList<>();
            for (Consist.ConLoco l : con.getLocos()) {

                // for complex ignore the lead as we have already set it
                boolean processThisLoco = (!l.getAddress().equals(con.getLeadAddr()))
                        || (tempPrefConsistFollowRuleStyle.contains(consist_function_rule_style_type.SPECIAL))
                        || (mainapp.prefAlwaysUseDefaultFunctionLabels);

                if (processThisLoco) {
                    functionList = l.getMatchingFunctions(function, lab,
                            l.getAddress().equals(con.getLeadAddr()), l.getAddress().equals(con.getTrailAddr()),
                            tempPrefConsistFollowRuleStyle, prefConsistFollowDefaultAction,
                            prefConsistFollowStrings, prefConsistFollowActions, prefConsistFollowHeadlights,
                            mainapp);
                    if (!functionList.isEmpty()) {
                        for (int i = 0; i < functionList.size(); i++) {
                            if ((tempPrefConsistFollowRuleStyle.equals(consist_function_rule_style_type.COMPLEX))
                                    || (isLatching == consist_function_latching_type.NA)) {
                                if (buttonPressMessageType == button_press_message_type.TOGGLE) {
                                    mainapp.toggleFunction(mainapp.throttleIntToString(whichThrottle) + l.getAddress(), functionList.get(i));
                                } else {
                                    if (!mainapp.isDCCEX) {
                                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + l.getAddress(), functionList.get(i), buttonPressMessageType);
                                    } else {
                                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + l.getAddress(), functionList.get(i), newFnState);
                                    }
                                }
                            } else {
                                if ((isLatching == consist_function_latching_type.YES) && (buttonPressMessageType == button_press_message_type.UP)) {
                                    if (!mainapp.isDCCEX) {
                                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + l.getAddress(), functionList.get(i), button_press_message_type.DOWN);
                                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + l.getAddress(), functionList.get(i), button_press_message_type.UP);
                                    } else {
                                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + l.getAddress(), functionList.get(i), newFnState);
                                    }
                                } else if (isLatching == consist_function_latching_type.NO) {
                                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + l.getAddress(), functionList.get(i), buttonPressMessageType);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int getFunctionNumber(Consist con, int functionNumber, String lab) {
        int result = functionNumber;
        if (functionNumber == -1) { // find the function number for this if we don't already have it
            for (Consist.ConLoco l : con.getLocos()) {
                if (l.getAddress().equals(con.getLeadAddr())) {
                    result = l.getFunctionNumberFromLabel(lab);
                }
            }
        }
        return result;
    }

    void doDeviceButtonSound(int whichThrottle, int soundType) {
        Log.d(threaded_application.applicationName, activityName + ": doDeviceButtonSound (locoSounds): wt: " + whichThrottle + " soundType: " + soundType);
        int soundTypeArrayIndex = soundType - 1;

        if ((mainapp.consists != null)
                && (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES)  // only dealing with the first two throttles for now
                && (mainapp.consists[whichThrottle].isActive())) {

            if (!mainapp.prefDeviceSounds[whichThrottle].equals("none")) {
                if (!mainapp.soundsDeviceButtonStates[whichThrottle][soundTypeArrayIndex]) {
                    if(ipls!=null) ipls.startBellHornSound(soundType, whichThrottle, soundsIsMuted[whichThrottle]);
                } else {
                    if(ipls!=null) ipls.stopBellHornSound(soundType, whichThrottle, soundsIsMuted[whichThrottle]);
                }
            }
        }
    } // end doDeviceButtonSound()

    protected class FunctionButtonTouchListener implements View.OnTouchListener {
        int function;
        int whichThrottle;
        boolean leadOnly;       // function only applies to the lead loco
        boolean trailOnly;      // function only applies to the trail loco (future)
        boolean followLeadFunction;       // function only applies to the locos that have been set to follow the function
        String lab;
//        Integer functionNumber = -1;

        protected FunctionButtonTouchListener(int new_function, int new_whichThrottle) {
            this(new_function, new_whichThrottle, "");
        }

        protected FunctionButtonTouchListener(int new_function, int new_whichThrottle, String funcLabel) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            function = new_function;    // store these values for this button
            whichThrottle = new_whichThrottle;
            lab = funcLabel.toUpperCase().trim();
            leadOnly = false;
            trailOnly = false;
            followLeadFunction = false;

            int result = isFunctionLeadTrailFollow(whichThrottle, function, lab);
            if ((result == function_is_type.LEAD_ONLY) || (result == function_is_type.LEAD_AND_FOLLOW) || (result == function_is_type.LEAD_AND_TRAIL)) {
                leadOnly = true;
            }
            if (result == function_is_type.TRAIL_ONLY) {
                trailOnly = true;
            }
            if ((result == function_is_type.FOLLOW) || (result == function_is_type.LEAD_AND_FOLLOW)) {
                followLeadFunction = true;
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            // Log.d(threaded_application.applicationName, activityName + ": onTouch(): func " + function + " action " +

            // make the click sound once
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.playSoundEffect(SoundEffectConstants.CLICK);
                mainapp.buttonVibration();
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
//            Log.d(threaded_application.applicationName, activityName + ": handleAction(): action: " + action );
            int isLatching = consist_function_latching_type.NA;  // only used for the special consist function matching

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    //noinspection SwitchStatementWithTooFewBranches
                    switch (this.function) {
//                        case function_button.STOP:
//                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
//
//                            set_stop_button(whichThrottle, true);
//
//                            if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_ZERO) {
//                                isPauseSpeeds[whichThrottle] = pause_speed_type.ZERO;
//                            } else if (isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN) {
//                                isPauseSpeeds[whichThrottle] = pause_speed_type.INACTIVE;
//                            } else if ((getSpeed(whichThrottle) == 0)
//                                    && (isPauseSpeeds[whichThrottle] == pause_speed_type.ZERO)) {
//                                disablePauseSpeed(whichThrottle);
//                            }
//
//                            speedUpdateAndNotify(whichThrottle, 0);
//                            break;

                        case function_button.SPEED_LABEL:  // specify which throttle the volume button controls
                            if (getConsist(whichThrottle).isActive() && !(IS_ESU_MCII && isEsuMc2Stopped)) { // only assign if Active and, if an ESU MCII not in Stop mode
                                whichVolume = whichThrottle;
                                mainapp.whichThrottleLastTouch = whichThrottle;
                                setLabels();
                            }
                            if (IS_ESU_MCII && isEsuMc2Stopped) {
                                mainapp.safeToast(getApplicationContext().getResources().getString(R.string.toastEsuMc2NoThrottleChange), Toast.LENGTH_SHORT);
                            }
                            break;

                        default: { // handle the function buttons
                            isLatching = setFunctionButtonState(whichThrottle, function, true);  //special handling for when using the default function labels, and one of 'Special' function following options
                            sendFunctionToConsistLocos(whichThrottle, function, lab, button_press_message_type.DOWN, leadOnly, trailOnly, followLeadFunction, isLatching);
                            break;
                        }
                    }
                    break;
                }

                // handle stopping of function on key-up
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
//                    if (function == function_button.STOP) {
//                        set_stop_button(whichThrottle, false);
//                    } else
                    if (function < direction_button.LEFT) { // only process UP event if this is a "function" button
                        isLatching = setFunctionButtonState(whichThrottle, function, false);  //special handling for when using the default function labels, and one of 'Special' function following options
                        sendFunctionToConsistLocos(whichThrottle, function, lab, button_press_message_type.UP, leadOnly, trailOnly, followLeadFunction, isLatching);
                    }
                    break;
            }
            setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
        }
    }

    //Listeners for the throttle slider
    protected class ThrottleSeekBarListener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichThrottle;
        int lastSpeed;
        int jumpSpeed;

        protected ThrottleSeekBarListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle; // store values for this listener
            lastSpeed = 0;
            limitedJump[whichThrottle] = false;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mainapp.exitDoubleBackButtonInitiated = 0;
//             Log.d(threaded_application.applicationName, activityName + ": ThrottleSeekBarListener: onTouch: Throttle action " + event.getAction());
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar throttle, int speed, boolean fromUser) {

            //special check for the vertical throttles
            boolean touchFromUser = false;
            if (vsbSpeeds != null) {
                touchFromUser = vsbSpeeds[whichThrottle].touchFromUser;
            }
//                Log.d(threaded_application.applicationName, activityName + ": onProgressChanged(): lj: " + limitedJump[whichThrottle] + " ai: " + mAutoIncrement[whichThrottle] + " ad: " + mAutoDecrement + " s: " + speed + " js: " + jumpSpeed);

            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (touchFromUser)) {

                if ((!limitedJump[whichThrottle])         // touch generates multiple onProgressChanged events, skip processing after first limited jump
                        && (sliderType != slider_type.SWITCHING)) {

                    int dif = speed - lastSpeed;
                    if (prefSpeedButtonsSpeedStepDecrement) {  // don't limit the decrement speed if the preference is not set
                        dif = (Math.abs(speed - lastSpeed));
                    }

                    if (dif > max_throttle_change) { // if jump is too large then limit it

                        // Log.d(threaded_application.applicationName, activityName + ": onProgressChanged(): throttling change");

                        if (speed < lastSpeed) { // going down
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.DECREMENT);
                        } else { // going up
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.INCREMENT);
                        }
                        jumpSpeed = speed;      // save ultimate target value
                        limitedJump[whichThrottle] = true;
                        throttle.setProgress(lastSpeed);

                        repeatUpdateHandler.post(new RptUpdater(whichThrottle, 0));

                        return;
                    }
                    sendSpeedMsg(whichThrottle, speed);
                    setDisplayedSpeed(whichThrottle, speed);
                    if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

                } else {                      // got a touch while processing limitJump
                    speed = lastSpeed;    //   so suppress multiple touches
                    throttle.setProgress(lastSpeed);
                    if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
                }
                // Now update ESU MCII Knob position
                if (IS_ESU_MCII && !isSemiRealisticThrottle) {
                    setEsuThrottleKnobPosition(whichThrottle, speed);
                }

                setActiveThrottle(whichThrottle, true);
            } else {
                if ((limitedJump[whichThrottle])
                        && (sliderType != slider_type.SWITCHING)) {
                    if (((mAutoIncrement[whichThrottle]) && (speed >= jumpSpeed))
                            || ((mAutoDecrement[whichThrottle]) && (speed <= jumpSpeed))) {
                        setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
                        limitedJump[whichThrottle] = false;
                        throttle.setProgress(jumpSpeed);
                        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
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
        }

        @Override
        public void onStopTrackingTouch(SeekBar sb) {
            limitedJump[whichThrottle] = false;
            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
            kidsTimerActions(kids_timer_action_type.STARTED, 0);
        }
    }

    // send a throttle speed message to WiT with message pacing
    public void sendSpeedMsg(int whichThrottle, int speed) {
        if (!speedMessagePacingTimers[whichThrottle].isDelayInProgress()) {
            sendSpeedMsgBasic(whichThrottle, speed);
            speedMessagePacingTimers[whichThrottle].pacingDelay();
        } else {
            speedMessagePacingTimers[whichThrottle].setDelayedSpeed(speed);
        }
    }

    public void sendSpeedMsgBasic(int whichThrottle, int speed) {
        // start timer to briefly ignore WiT speed messages - avoids speed "jumping"
        changeTimers[whichThrottle].pacingDelay();
        // send speed update to WiT
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.VELOCITY, "", whichThrottle, speed);
    }

    // implement general purpose pacing delay class
    protected class PacingDelay {
        private boolean delayInProg;
        protected Runnable delayTimer;
        private final int delay;

        protected PacingDelay(int delay) {
            this.delay = delay;
            delayTimer = new DelayTimer();
        }

        protected boolean isDelayInProgress() {
            return delayInProg;
        }

        protected void pacingDelay() {
            if (mainapp.throttle_msg_handler == null) return;
            mainapp.throttle_msg_handler.removeCallbacks(delayTimer);          //remove any pending requests
            delayInProg = true;
            mainapp.throttle_msg_handler.postDelayed(delayTimer, delay);
        }

        protected class DelayTimer implements Runnable {

            private DelayTimer() {
                delayInProg = false;
            }

            @Override
            public void run() {         // change delay is over - clear the delay flag
                delayInProg = false;
            }
        }
    }

    // pacing delay class for speed messages
    // sends speed value once the pacing delay expires to ensure WiT gets final setpoint
    protected class SpeedPacingDelay extends PacingDelay {
        private int speed;
        private final int whichThrottle;
        private volatile boolean sendDelayedSpeed;

        protected SpeedPacingDelay(int delay, int whichThrottle) {
            super(delay);
            delayTimer = new SpeedDelayTimer();     // replace base timer with one that sets speed
            this.whichThrottle = whichThrottle;
            this.sendDelayedSpeed = false;
        }

        void setDelayedSpeed(int delayedSpeed) {
            this.speed = delayedSpeed;
            this.sendDelayedSpeed = true;
        }

        class SpeedDelayTimer extends PacingDelay.DelayTimer {
            @Override
            public void run() {
                super.run();
                if (sendDelayedSpeed) {             // if haven't sent the latest speed value
                    sendDelayedSpeed = false;       //  then send it now
                    sendSpeedMsgBasic(whichThrottle, speed);
                    pacingDelay();                  // start next delay
                }
            }
        }
    }

    // set the title, optionally adding the current time.
    private void setActivityTitle() {
        if (mainapp.getFastClockFormat() > 0)
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    "",
                    getApplicationContext().getResources().getString(R.string.app_name_throttle_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_throttle),
                    "");
    }

    @SuppressLint({"Recycle", "SetJavaScriptEnabled", "ClickableViewAccessibility", "ApplySharedPref"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(threaded_application.applicationName, activityName + ": onCreate(): called");
        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        mainapp.applyTheme(this);

        mainapp.throttleSwitchAllowed = false; // used to prevent throttle switches until the previous onStart completes
        mainapp.throttleSwitchWasRequestedOrReinitialiseRequired = false;

        sliderType = slider_type.HORIZONTAL; // default. Will likely be overridden by the child activities

        onCreateSavedInstanceState = savedInstanceState;
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            try {
                // restore the requested throttle direction so we can update the
                // direction indication while we wait for an update from WiT
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    if (savedInstanceState.getSerializable("dir" + mainapp.throttleIntToString(throttleIndex)) != null)
                        dirs[throttleIndex] = (int) savedInstanceState.getSerializable("dir" + throttleIndex);
                }
            } catch (Exception ignored) {              // log the error, but otherwise keep going.
                Log.d(threaded_application.applicationName, activityName + ": onCreate(): Restore of saved instance state failed " + android.os.Build.VERSION.SDK_INT);
            }
        }

        if (mainapp.isForcingFinish()) { // expedite
            mainapp.appIsFinishing = true;
            this.finish();
            return;
        }

        // was ED killed while it was in background?
        if (prefs.getBoolean("prefForcedRestart", false)) { // if forced restart from the preferences
            int prefForcedRestartReason = prefs.getInt("prefForcedRestartReason", restart_reason_type.NONE);
            if (prefForcedRestartReason == restart_reason_type.APP_PUSHED_TO_BACKGROUND) {
                mainapp.prefsForcedRestart(prefForcedRestartReason);
                prefs.edit().putBoolean("prefForcedRestart", false).commit();
                prefs.edit().putInt("prefForcedRestartReason", restart_reason_type.NONE).commit();
                Intent in = new Intent().setClass(this, connection_activity.class);
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            }
        }

        // get the screen brightness on create
        screenBrightnessOriginal = getScreenBrightness();
        screenBrightnessModeOriginal = getScreenBrightnessMode();

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.d(threaded_application.applicationName, activityName + ": handleOnBackPressed()");
                if (!isScreenLocked) {
                    if (webViewIsOn && webView.canGoBack() && !clearHistory) {
                        webView.goBack();
                        webView.setInitialScale((int) (100 * scale)); // restore scale
                    } else {
                        setImmersiveMode(webView);
                        if (mainapp.throttle_msg_handler != null) {
                            mainapp.checkExit(throttle.this);
                        } else { // something has gone wrong and the activity did not shut down properly so force it
                            shutdown();
                        }
                    }
                } else {
                    mainapp.safeToast(getApplicationContext().getResources().getString(R.string.toastShakeScreenLockedActionNotAllowed), Toast.LENGTH_SHORT);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // only do this in onCreate
        if (prefs.getBoolean("prefThrottlesLocos",getResources().getBoolean(R.bool.prefThrottlesLocosDefaultValue))) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mainapp.safeToastInstructional(R.string.prefThrottlesLocosToast, Toast.LENGTH_LONG);
                    importExportPreferences.loadThrottlesEnginesListFromFile(mainapp, getApplicationContext());
                    setLabels();
                }
            }, 2000);
        }

        setContentView(mainapp.throttleLayoutViewId); // default.  Will likely be overridden by the child activities

        getCommonPrefs(true); // get all the common preferences
        setThrottleNumLimits();

        getDirectionButtonPrefs();

        //load the gamepad beep sounds
        TypedArray gamepadBeepIds = getResources().obtainTypedArray(R.array.beep_ids);
        mainapp.gamepadBeepIds = new int[gamepadBeepIds.length()];
        for (int i=0; i<gamepadBeepIds.length(); i++) {
            mainapp.gamepadBeepIds[i] = gamepadBeepIds.getResourceId(i,0);
        }
        gamepadBeepIds.recycle();

        webViewIsOn = !prefWebViewLocation.equals(web_view_location_type.NONE);
        keepWebViewLocation = prefWebViewLocation;

        isScreenLocked = false;

        initialiseUiElements();

    } // end of onCreate()

    @SuppressLint("ApplySharedPref")
    @Override
    public void onStart() {
        Log.d(threaded_application.applicationName, activityName + ": onStart(): called");

        mainapp.throttleSwitchAllowed = false; // used to prevent throttle switches until the previous onStart() completes

        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.throttle_msg_handler == null)
            mainapp.throttle_msg_handler = new ThrottleMessageHandler(Looper.getMainLooper());

        // if it was killed in background, clear the save preferences
        if ( (prefs.getBoolean("prefForcedRestart", false))
                && (prefs.getInt("prefForcedRestartReason", restart_reason_type.NONE) == restart_reason_type.APP_PUSHED_TO_BACKGROUND) ) {
            prefs.edit().putBoolean("prefForcedRestart", false).commit();
            prefs.edit().putInt("prefForcedRestartReason", restart_reason_type.NONE).commit();
        }

        if(mainapp.throttleSwitchWasRequestedOrReinitialiseRequired) {
            setContentView(mainapp.throttleLayoutViewId); // default.  Will likely be overridden by the child activities
        }

        super.onStart();

        if(mainapp.throttleSwitchWasRequestedOrReinitialiseRequired) {
            mainapp.throttleSwitchWasRequestedOrReinitialiseRequired = false;

            getCommonPrefs(true); // get all the common preferences
            setThrottleNumLimits();

            getDirectionButtonPrefs();

            webViewIsOn = !prefWebViewLocation.equals(web_view_location_type.NONE);
            keepWebViewLocation = prefWebViewLocation;

            isScreenLocked = false;

            initialiseUiElements();
        }

    } // end onStart()

    @SuppressLint("ApplySharedPref")
    @Override
    public void onResume() {
        Log.d(threaded_application.applicationName, activityName + ": onResume(): called");
        super.onResume();
        threaded_application.activityResumed(activityName);
        mainapp.removeNotification(this.getIntent());

        mainapp.applyTheme(this);

        mainapp.throttleSwitchAllowed = false; // used to prevent throttle switches until the previous onStart() & onResume() completes
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainapp.throttleSwitchAllowed = true;
                mainapp.displayThrottleSwitchMenuButton(overflowMenu);
            }
        }, 6000);

        threaded_application.currentActivity = activity_id_type.THROTTLE;
        if (mainapp.isForcingFinish()) { // expedite
            mainapp.appIsFinishing = true;
            this.finish();
            overridePendingTransition(0, 0);
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        mainapp.exitDoubleBackButtonInitiated = 0;

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);

        // format the screen area
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            enableDisableButtons(throttleIndex);
            soundsShowHideDeviceSoundsButton(throttleIndex);
            showHideSpeedLimitAndPauseButtons(throttleIndex);
        }

        gestureFailed = false;
        gestureInProgress = false;

        getCommonPrefs(false);
        getDirectionButtonPrefs();

        setThrottleNumLimits();

        clearVolumeAndGamepadAdditionalIndicators();

        getDirectionButtonPrefs();
        setDirectionButtonLabels(); // set all the direction button labels

        if (gamePadKeyLoader!=null) setGamepadKeys();

        applySpeedRelatedOptions();  // update all throttles

        if (mainapp.soundsReloadSounds) loadSounds();

        setLabels(); // handle labels and update view
        pauseResumeWebView();

        if (mainapp.EStopActivated) {
            speedUpdateAndNotify(0);  // update all three throttles
            applySpeedRelatedOptions();  // update all three throttles

            mainapp.EStopActivated = false;
        }

        if (IS_ESU_MCII && isEsuMc2Stopped) {
            if (isEsuMc2AllStopped) {
                // disable buttons for all throttles
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    setEnabledEsuMc2ThrottleScreenButtons(throttleIndex, false);
                }
            } else {
                // disable buttons for current throttle
                setEnabledEsuMc2ThrottleScreenButtons(whichVolume, false);
            }
        }

        // update the direction indicators
        showDirectionIndications();

        showHideConsistMenus();

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        if (!prefAccelerometerShake.equals(acceleratorometer_action_type.NONE)) {
            if (!accelerometerCurrent) { // preference has only just been changed to turn it on
                setupSensor();
            } else {
                sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
        }

        if (((prefKidsTime > 0) && (kidsTimerRunning != kids_timer_action_type.RUNNING))) {
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.KIDS_TIMER_ENABLE, "", 0, 0);
        } else {
            if (kidsTimerRunning == kids_timer_action_type.ENDED) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.KIDS_TIMER_END, "", 0, 0);
            }

            if (prefKidsTimer.equals(PREF_KIDS_TIMER_NONE)) {
                kidsTimerActions(kids_timer_action_type.DISABLED, 1);
            }
        }

        tts.loadPrefs();

        setActivityTitle();

        if (prefs.getBoolean("prefForcedRestart", false)) { // if forced restart from the preferences
            prefs.edit().putBoolean("prefForcedRestart", false).commit();

            int prefForcedRestartReason = prefs.getInt("prefForcedRestartReason", restart_reason_type.NONE);
            Log.d(threaded_application.applicationName, activityName + ": onResume(): connection: Forced Restart Reason: " + prefForcedRestartReason);
            if (mainapp.prefsForcedRestart(prefForcedRestartReason)) {
                startSettingsActivity();
            }
        }
    } // end onResume()

    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    void initialiseUiElements() {
        Log.d(threaded_application.applicationName, activityName + ": initialiseUiElements(): start");

        throttleOverlay = findViewById(R.id.throttle_overlay);
        throttleOverlay.addOnGestureListener(this);
        throttleOverlay.setGestureVisible(false);

        DirectionButtonTouchListener directionButtonTouchListener;
        FunctionButtonTouchListener functionButtonTouchListener;
        SelectFunctionButtonTouchListener selectFunctionButtonTouchListener;
        ArrowSpeedButtonTouchListener arrowSpeedButtonTouchListener;

        initialiseArrays();

        // throttle layouts
        vThrottleScreen = findViewById(R.id.throttle_screen);
        vThrottleScreenWrap = findViewById(R.id.throttle_screen_wrapper);

        TypedArray right_speed_button_resource_ids = getResources().obtainTypedArray(R.array.right_speed_button_resource_ids);
        TypedArray left_speed_button_resource_ids = getResources().obtainTypedArray(R.array.left_speed_button_resource_ids);
        TypedArray button_select_loco_resource_ids = getResources().obtainTypedArray(R.array.button_select_loco_resource_ids);
        TypedArray loco_label_resource_ids = getResources().obtainTypedArray(R.array.loco_label_resource_ids);

        TypedArray loco_left_direction_indication_resource_ids = getResources().obtainTypedArray(R.array.loco_left_direction_indication_resource_ids);
        TypedArray loco_right_direction_indication_resource_ids = getResources().obtainTypedArray(R.array.loco_right_direction_indication_resource_ids);

        TypedArray button_fwd_resource_ids = getResources().obtainTypedArray(R.array.button_fwd_resource_ids);
        TypedArray button_stop_resource_ids = getResources().obtainTypedArray(R.array.button_stop_resource_ids);
        TypedArray button_rev_resource_ids = getResources().obtainTypedArray(R.array.button_rev_resource_ids);
        TypedArray speed_cell_resource_ids = getResources().obtainTypedArray(R.array.speed_cell_resource_ids);

        TypedArray speed_resource_ids = getResources().obtainTypedArray(R.array.speed_resource_ids);

        TypedArray throttle_resource_ids = getResources().obtainTypedArray(R.array.throttle_resource_ids);
        TypedArray loco_buttons_group_resource_ids = getResources().obtainTypedArray(R.array.loco_buttons_group_resource_ids);
        TypedArray dir_buttons_table_resource_ids = getResources().obtainTypedArray(R.array.dir_buttons_table_resource_ids);
        TypedArray volume_indicator_resource_ids = getResources().obtainTypedArray(R.array.volume_indicator_resource_ids);
        TypedArray gamepad_indicator_resource_ids = getResources().obtainTypedArray(R.array.gamepad_indicator_resource_ids);
        TypedArray speed_label_resource_ids = getResources().obtainTypedArray(R.array.speed_label_resource_ids);
        TypedArray speed_value_label_resource_ids = getResources().obtainTypedArray(R.array.speed_value_label_resource_ids);
        TypedArray function_buttons_table_resource_ids = getResources().obtainTypedArray(R.array.function_buttons_table_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            // set listener for select loco buttons
            bSelects[throttleIndex] = findViewById(button_select_loco_resource_ids.getResourceId(throttleIndex,0));
            tvbSelectsLabels[throttleIndex] = findViewById(loco_label_resource_ids.getResourceId(throttleIndex,0));
            bSelects[throttleIndex].setClickable(true);
            selectFunctionButtonTouchListener = new SelectFunctionButtonTouchListener(throttleIndex);
            bSelects[throttleIndex].setOnClickListener(selectFunctionButtonTouchListener);
//            bSelects[throttleIndex].setOnTouchListener(sfbt);
            bSelects[throttleIndex].setOnLongClickListener(selectFunctionButtonTouchListener);  // Consist Light Edit

            tvLeftDirectionIndicators[throttleIndex] = findViewById(loco_left_direction_indication_resource_ids.getResourceId(throttleIndex,0));
            tvRightDirectionIndicators[throttleIndex] = findViewById(loco_right_direction_indication_resource_ids.getResourceId(throttleIndex,0));

            // Arrow Keys
            try {
                bRightSpeeds[throttleIndex] = findViewById(right_speed_button_resource_ids.getResourceId(throttleIndex,0));
                bRightSpeeds[throttleIndex].setClickable(true);
                arrowSpeedButtonTouchListener = new ArrowSpeedButtonTouchListener(throttleIndex, speed_button_type.RIGHT);
                bRightSpeeds[throttleIndex].setOnLongClickListener(arrowSpeedButtonTouchListener);
                bRightSpeeds[throttleIndex].setOnTouchListener(arrowSpeedButtonTouchListener);
                bRightSpeeds[throttleIndex].setOnClickListener(arrowSpeedButtonTouchListener);

                bLeftSpeeds[throttleIndex] = findViewById(left_speed_button_resource_ids.getResourceId(throttleIndex,0));
                bLeftSpeeds[throttleIndex].setClickable(true);
                arrowSpeedButtonTouchListener = new ArrowSpeedButtonTouchListener(throttleIndex, speed_button_type.LEFT);
                bLeftSpeeds[throttleIndex].setOnLongClickListener(arrowSpeedButtonTouchListener);
                bLeftSpeeds[throttleIndex].setOnTouchListener(arrowSpeedButtonTouchListener);
                bLeftSpeeds[throttleIndex].setOnClickListener(arrowSpeedButtonTouchListener);

            } catch (Exception ex) {
                Log.d(threaded_application.applicationName, activityName + ": onCreate(): Exception: " + ex.getMessage());
            }

            // set listeners for 3 direction buttons for each throttle
            //----------------------------------------

            bForwards[throttleIndex] = findViewById(button_fwd_resource_ids.getResourceId(throttleIndex,0));
            directionButtonTouchListener = new DirectionButtonTouchListener(direction_button.LEFT, throttleIndex);
            bForwards[throttleIndex].setOnTouchListener(directionButtonTouchListener);

            bStops[throttleIndex] = findViewById(button_stop_resource_ids.getResourceId(throttleIndex,0));
            StopButtonTouchListener stopButtonTouchListener = new StopButtonTouchListener(throttleIndex);
            bStops[throttleIndex].setOnTouchListener(stopButtonTouchListener);

            bReverses[throttleIndex] = findViewById(button_rev_resource_ids.getResourceId(throttleIndex,0));
            directionButtonTouchListener = new DirectionButtonTouchListener(direction_button.RIGHT, throttleIndex);
            bReverses[throttleIndex].setOnTouchListener(directionButtonTouchListener);

            functionButtonTouchListener = new FunctionButtonTouchListener(function_button.SPEED_LABEL, throttleIndex);
            View v = findViewById(speed_cell_resource_ids.getResourceId(throttleIndex,0));
            v.setOnTouchListener(functionButtonTouchListener);

            // set up listeners for all throttles
            ThrottleSeekBarListener thl;
            sbs[throttleIndex] = findViewById(speed_resource_ids.getResourceId(throttleIndex,0));
            thl = new ThrottleSeekBarListener(throttleIndex);
            sbs[throttleIndex].setOnSeekBarChangeListener(thl);
            sbs[throttleIndex].setOnTouchListener(thl);

            max_throttle_change = 1;

            llThrottleLayouts[throttleIndex] = findViewById(throttle_resource_ids.getResourceId(throttleIndex,0));
            llLocoIdAndSpeedViewGroups[throttleIndex] = findViewById(loco_buttons_group_resource_ids.getResourceId(throttleIndex,0));
            llLocoDirectionButtonViewGroups[throttleIndex] = findViewById(dir_buttons_table_resource_ids.getResourceId(throttleIndex,0));
            tvVolumeIndicators[throttleIndex] = findViewById(volume_indicator_resource_ids.getResourceId(throttleIndex,0)); // volume indicators
            tvGamePads[throttleIndex] = findViewById(gamepad_indicator_resource_ids.getResourceId(throttleIndex,0)); // gamepad indicators
            tvSpeedLabels[throttleIndex] = findViewById(speed_label_resource_ids.getResourceId(throttleIndex,0)); // set_default_function_labels();
            tvSpeedValues[throttleIndex] = findViewById(speed_value_label_resource_ids.getResourceId(throttleIndex,0));
            functionButtonViewGroups[throttleIndex] = findViewById(function_buttons_table_resource_ids.getResourceId(throttleIndex,0));

            // set throttle change delay timers
            changeTimers[throttleIndex] = new PacingDelay(changeDelay);
            speedMessagePacingTimers[throttleIndex] = new SpeedPacingDelay(maxSpeedMessageRate, throttleIndex);
        }

        clearVolumeAndGamepadAdditionalIndicators();

        setDirectionButtonLabels(); // set all the direction button labels

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        if ((!prefThrottleScreenType.contains(throttle_screen_type.CONTAINS_SWITCHING)) || (prefThrottleScreenType.equals(throttle_screen_type.SWITCHING_HORIZONTAL))) {
            // set listeners for the limit speed buttons for each throttle
            LimitSpeedButtonTouchListener limitSpeedButtonTouchListener;
            Button bLimitSpeed;

            TypedArray limit_speed_resource_ids = getResources().obtainTypedArray(R.array.limit_speed_resource_ids);

            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                bLimitSpeed = findViewById(limit_speed_resource_ids.getResourceId(throttleIndex,0));
                if (bLimitSpeed != null) {
                    bLimitSpeeds[throttleIndex] = bLimitSpeed;
                    limitSpeedSliderScalingFactors[throttleIndex] = 1;
                    limitSpeedButtonTouchListener = new LimitSpeedButtonTouchListener(throttleIndex);
                    bLimitSpeeds[throttleIndex].setOnTouchListener(limitSpeedButtonTouchListener);
                    isLimitSpeeds[throttleIndex] = false;
                    if (!prefLimitSpeedButton) {
                        bLimitSpeed.setVisibility(View.GONE);
                    }
                }
            }

            limit_speed_resource_ids.recycle();
        }

        // set listeners for the pause buttons for each throttle
        PauseSpeedButtonTouchListener pauseSpeedButtonTouchListener;
        Button bPauseSpeed;

        TypedArray pause_speed_resource_ids = getResources().obtainTypedArray(R.array.pause_speed_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            bPauseSpeed = findViewById(pause_speed_resource_ids.getResourceId(throttleIndex,0));
            if (bPauseSpeed != null) {
                bPauseSpeeds[throttleIndex] = bPauseSpeed;
                pauseSpeedButtonTouchListener = new PauseSpeedButtonTouchListener(throttleIndex);
                bPauseSpeeds[throttleIndex].setOnTouchListener(pauseSpeedButtonTouchListener);
                isPauseSpeeds[throttleIndex] = pause_speed_type.INACTIVE;
                if (!prefPauseSpeedButton) {
                    bPauseSpeed.setVisibility(View.GONE);
                }
            }
        }

        //device sounds buttons
        SoundDeviceMuteButtonTouchListener muteTl;
        Button bMute;
        SoundDeviceExtrasButtonTouchListener soundsExtrasTl;
        Button bBell;
        Button bHorn;
        Button bHornShort;

        TypedArray device_sounds_mute_resource_ids = getResources().obtainTypedArray(R.array.device_sounds_mute_resource_ids);
        TypedArray device_sounds_bell_resource_ids = getResources().obtainTypedArray(R.array.device_sounds_bell_resource_ids);
        TypedArray device_sounds_horn_resource_ids = getResources().obtainTypedArray(R.array.device_sounds_horn_resource_ids);
        TypedArray device_sounds_horn_short_resource_ids = getResources().obtainTypedArray(R.array.device_sounds_horn_short_resource_ids);

        for (int throttleIndex = 0; ((throttleIndex < mainapp.maxThrottles) && (throttleIndex < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES)); throttleIndex++) {
            bMute = findViewById(device_sounds_mute_resource_ids.getResourceId(throttleIndex,0));
            bBell = findViewById(device_sounds_bell_resource_ids.getResourceId(throttleIndex,0));
            bHorn = findViewById(device_sounds_horn_resource_ids.getResourceId(throttleIndex,0));
            bHornShort = findViewById(device_sounds_horn_short_resource_ids.getResourceId(throttleIndex,0));

            if (bMute != null) { // some layouts only have one throttle or no mute buttons so this may be null
                bMutes[throttleIndex] = bMute;
                muteTl = new SoundDeviceMuteButtonTouchListener(throttleIndex);
                bMutes[throttleIndex].setOnTouchListener(muteTl);

                bSoundsExtras[sounds_type.BUTTON_BELL][throttleIndex] = bBell;
                soundsExtrasTl = new SoundDeviceExtrasButtonTouchListener(throttleIndex, sounds_type.BUTTON_BELL, sounds_type.BELL);
                bSoundsExtras[sounds_type.BUTTON_BELL][throttleIndex].setOnTouchListener(soundsExtrasTl);

                bSoundsExtras[sounds_type.BUTTON_HORN][throttleIndex] = bHorn;
                soundsExtrasTl = new SoundDeviceExtrasButtonTouchListener(throttleIndex, sounds_type.BUTTON_HORN, sounds_type.HORN);
                bSoundsExtras[sounds_type.BUTTON_HORN][throttleIndex].setOnTouchListener(soundsExtrasTl);

                bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][throttleIndex] = bHornShort;
                soundsExtrasTl = new SoundDeviceExtrasButtonTouchListener(throttleIndex, sounds_type.BUTTON_HORN_SHORT, sounds_type.HORN_SHORT);
                bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][throttleIndex].setOnTouchListener(soundsExtrasTl);

                soundsShowHideDeviceSoundsButton(throttleIndex);
            }
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        setActiveThrottle(0); // set the throttle the volume keys control depending on the preference to the default 0

        if (prefs.getString("prefWebViewLocation", web_view_location_type.NONE).equals(web_view_location_type.TOP)) {
            webView = findViewById(R.id.throttle_webview_top);
        } else {
            webView = findViewById(R.id.throttle_webview);
        }
        webView.setVisibility(VISIBLE);
        String databasePath = webView.getContext().getDir("databases", Context.MODE_PRIVATE).getPath();
        webView.getSettings().setDatabasePath(databasePath);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true); // Enable Multitouch
        // if supported
        webView.getSettings().setUseWideViewPort(true); // Enable greater
        // zoom-out
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        if (onCreateSavedInstanceState != null) {
            if (onCreateSavedInstanceState.getSerializable("scale") != null) {
                scale = (float) onCreateSavedInstanceState.getSerializable(("scale"));
            }
        }
        webView.setInitialScale((int) (100 * scale));
        webView.clearCache(true);   // force fresh javascript download on first connection

        // enable remote debugging of all webviews
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // open all links inside the current view (don't start external web
        // browser)
        noUrl = getApplicationContext().getResources().getString(R.string.blank_page_url);
        WebViewClient EDWebClient = new WebViewClient() {
            private int loadRetryCnt = 0;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!noUrl.equals(url)) {               // if url is legit
                    if (!url.equals(currentUrl)) {
                        currentUrl = url;
                        loadRetryCnt = 0;                   // reset count for next url load
                    }
                    if (firstUrl == null) {             // if this is the first legit url
                        firstUrl = url;
                        scale = initialScale;
                        webView.setInitialScale((int) (100 * scale));
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

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleLoadingErrorRetries();
            }

            // above form of shouldOverrideUrlLoading is deprecated so support the new form if available
            @androidx.annotation.RequiresApi(24)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleLoadingErrorRetries();
            }

            // stop page from continually reloading when loading errors occur
            // (this can happen if the initial web page pref is set to a non-existent url)
            private boolean handleLoadingErrorRetries() {
                if (++loadRetryCnt >= 3) {   // if same page is reloading (due to errors)
                    clearHistory = false;       // stop trying to clear history
                    loadRetryCnt = 0;        // reset count for next url load
                    return true;                // don't load the page
                }
                return false;                   // load in webView
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                scale = newScale;
            }
        };

        webView.setWebViewClient(EDWebClient);
        if (currentUrl == null || onCreateSavedInstanceState == null || webView.restoreState(onCreateSavedInstanceState) == null) {
            load_webview(); // reload if no saved state or no page had loaded when state was saved
        } else {
            webView.setInitialScale((int) (100 * scale));   // apply scale to restored webView
        }

        //longPress webview to reload
        webView.setOnLongClickListener(new WebView.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                reloadWeb();
                return true;
            }
        });

        // tone generator for feedback sounds
        try {
            tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                    threaded_application.getIntPrefValue(prefs, "prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));
        } catch (RuntimeException e) {
            Log.e(threaded_application.applicationName, activityName + ": onCreate(): new ToneGenerator failed. Runtime Exception, OS " + android.os.Build.VERSION.SDK_INT + " Message: " + e);
        }
        // set GamePad Support
        if (gamePadKeyLoader!=null) setGamepadKeys();

        initialiseEsuMc2();

        setupSensor(); // setup the support for shake actions.

        tts = new Tts(prefs, mainapp);

        queryAllSpeedsAndDirectionsWiT();

//        loadBackgroundImage();
        BackgroundImageLoader backgroundImageLoader = new BackgroundImageLoader(prefs, mainapp, findViewById(R.id.backgroundImgView));
        backgroundImageLoader.loadBackgroundImage();

        if (!mainapp.webServerNameHasBeenChecked) {
            mainapp.getServerNameFromWebServer();
        }

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
        }

        getStatusBarAndNavigationBarHeights();

        immersiveModeIsOn = false;
        immersiveModeTempIsOn = false;


        right_speed_button_resource_ids.recycle();
        left_speed_button_resource_ids .recycle();
        button_select_loco_resource_ids.recycle();
        loco_label_resource_ids.recycle();
        loco_left_direction_indication_resource_ids.recycle();
        loco_right_direction_indication_resource_ids.recycle();
        button_fwd_resource_ids.recycle();
        button_stop_resource_ids.recycle();
        button_rev_resource_ids.recycle();
        speed_cell_resource_ids.recycle();
        speed_resource_ids.recycle();
        throttle_resource_ids.recycle();
        loco_buttons_group_resource_ids.recycle();
        dir_buttons_table_resource_ids.recycle();
        volume_indicator_resource_ids.recycle();
        gamepad_indicator_resource_ids.recycle();
        speed_label_resource_ids.recycle();
        speed_value_label_resource_ids.recycle();
        function_buttons_table_resource_ids.recycle();

        pause_speed_resource_ids.recycle();

        device_sounds_mute_resource_ids.recycle();
        device_sounds_bell_resource_ids.recycle();
        device_sounds_horn_resource_ids.recycle();
        device_sounds_horn_short_resource_ids.recycle();

    } // end initialiseUiElements()

    private void initialiseArrays() {
        Log.d(threaded_application.applicationName, activityName + ": initialiseArrays(): called");

        bSelects = new Button[mainapp.maxThrottlesCurrentScreen];
        tvbSelectsLabels = new TextView[mainapp.maxThrottlesCurrentScreen];
        bRightSpeeds = new Button[mainapp.maxThrottlesCurrentScreen];
        bLeftSpeeds = new Button[mainapp.maxThrottlesCurrentScreen];

        tvLeftDirectionIndicators = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvRightDirectionIndicators = new TextView[mainapp.maxThrottlesCurrentScreen];

        bForwards = new Button[mainapp.maxThrottlesCurrentScreen];
        bStops = new Button[mainapp.maxThrottlesCurrentScreen];
        bPauses = new Button[mainapp.maxThrottlesCurrentScreen];
        bReverses = new Button[mainapp.maxThrottlesCurrentScreen];
        sbs = new SeekBar[mainapp.maxThrottlesCurrentScreen];

        bLimitSpeeds = new Button[mainapp.maxThrottlesCurrentScreen];
        isLimitSpeeds = new boolean[mainapp.maxThrottlesCurrentScreen];
        limitSpeedSliderScalingFactors = new float[mainapp.maxThrottlesCurrentScreen];
        bPauseSpeeds = new Button[mainapp.maxThrottlesCurrentScreen];
        isPauseSpeeds = new int[mainapp.maxThrottlesCurrentScreen];

        bMutes = new Button[mainapp.maxThrottlesCurrentScreen];
        soundsIsMuted = new boolean[mainapp.maxThrottlesCurrentScreen];
        bSoundsExtras = new Button[3][mainapp.maxThrottlesCurrentScreen];

        tvGamePads = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvSpeedLabels = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvSpeedValues = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvVolumeIndicators = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvDirectionIndicatorForwards = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvDirectionIndicatorReverses = new TextView[mainapp.maxThrottlesCurrentScreen];

        llThrottleLayouts = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        llSetSpeeds = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        llLocoIdAndSpeedViewGroups = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        llLocoDirectionButtonViewGroups = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        // Semi-Realistic throttle
        tvTargetSpdVals = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvTargetAccelerationVals = new TextView[mainapp.maxThrottlesCurrentScreen];

        functionButtonViewGroups = new ViewGroup[mainapp.maxThrottlesCurrentScreen];

        functionMaps = (LinkedHashMap<Integer, Button>[]) new LinkedHashMap<?, ?>[mainapp.maxThrottlesCurrentScreen];

        displayUnitScales = new double[mainapp.maxThrottlesCurrentScreen];

        changeTimers = new PacingDelay[mainapp.maxThrottlesCurrentScreen];
        speedMessagePacingTimers = new SpeedPacingDelay[mainapp.maxThrottlesCurrentScreen];

    }

    // initialise ESU MCII/PRO
    private void initialiseEsuMc2() {
        if (IS_ESU_MCII) {
            Log.d(threaded_application.applicationName, activityName + ": onCreate(): ESU_MCII: Initialise fragments...");
            int zeroTrim = threaded_application.getIntPrefValue(prefs, "prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
            esuThrottleFragment = ThrottleFragment.newInstance(zeroTrim);
            esuThrottleFragment.setOnThrottleListener(esuOnThrottleListener);
            esuStopButtonFragment = StopButtonFragment.newInstance();
            esuStopButtonFragment.setOnStopButtonListener(esuOnStopButtonListener);
            Log.d(threaded_application.applicationName, activityName + ": onCreate(): ESU_MCII: ...fragments initialised");

            getSupportFragmentManager().beginTransaction()
                    .add(esuThrottleFragment, "mc2:throttle")
                    .add(esuStopButtonFragment, "mc2:stopKey")
                    .commit();
            esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.OFF, true);
            esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.STEADY_FLASH, true);

            // Now apply knob zero trim
            updateEsuMc2ZeroTrim();
            Log.d(threaded_application.applicationName, activityName + ": onCreate(): ESU_MCII: Initialisation complete");
        }
    }

    private void showHideConsistMenus() {
        if ((mainapp.consists == null) && (!mainapp.isDCCEX)) {
            Log.d(threaded_application.applicationName, activityName + ": showHideConsistMenu(): consists[] is null and not DCC-EX");
            return;
        }

        if (overflowMenu != null) {
            boolean anyConsist = false;

            TypedArray edit_consist_menu_ids = getResources().obtainTypedArray(R.array.edit_consist_menu_ids);
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                Consist con = mainapp.consists[throttleIndex];
                if (con == null) {
                    Log.d(threaded_application.applicationName, activityName + ": showHideConsistMenu(): consists[" + throttleIndex + "] is null");
                    break;
                }
                boolean isMulti = con.isMulti();
                anyConsist |= isMulti;
                overflowMenu.findItem(edit_consist_menu_ids.getResourceId(throttleIndex,0)).setVisible(isMulti);
                overflowMenu.findItem(edit_consist_menu_ids.getResourceId(throttleIndex,0)).setVisible(isMulti);
            }
            edit_consist_menu_ids.recycle();

            overflowMenu.findItem(R.id.edit_consists_menu).setVisible(anyConsist);

            boolean isSpecial = (mainapp.prefAlwaysUseDefaultFunctionLabels)
                    && (mainapp.prefConsistFollowRuleStyle.contains(consist_function_rule_style_type.SPECIAL));
//            overflowMenu.findItem(R.id.function_consist_settings_mnu).setVisible(isSpecial || mainapp.isDCCEX);
            overflowMenu.findItem(R.id.function_consist_settings_mnu).setVisible(true);
            if (!isSpecial) {
                if (mainapp.isDCCEX) {
                    overflowMenu.findItem(R.id.function_consist_settings_mnu).setTitle(R.string.dccExFunctionSettings);
                } else {
                    overflowMenu.findItem(R.id.function_consist_settings_mnu).setVisible(mainapp.prefOverrideWiThrottlesFunctionLatching);
                    overflowMenu.findItem(R.id.function_consist_settings_mnu).setTitle(R.string.functionLatchingSettings);
                }
            } else {
                overflowMenu.findItem(R.id.function_consist_settings_mnu).setTitle(R.string.functionConsistSettings);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        threaded_application.activityPaused(activityName);

        if (webViewIsOn) {
            pauseWebView();
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
//            setImmersiveMode(webView);
            setLabels();       // need to redraw button Press states since ActionBar and Notification access clears them
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("scale", scale);   // save current scale for next onCreate
        if (webView != null) {
            webView.saveState(outState);                // save history on rotation or bkg mode
        }

        // save the requested throttle direction so we can update the
        // direction indication immediately in OnCreate following a rotate
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            outState.putSerializable("dir" + mainapp.throttleIntToChar(throttleIndex), dirs[throttleIndex]);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!mainapp.setActivityOrientation(this)) { // set screen orientation based on prefs
            Intent in = new Intent().setClass(this, web_activity.class); // if autoWeb and landscape, switch to Web activity
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(threaded_application.applicationName, activityName + ": onDestroy() called");
        super.onDestroy();

        kidsTimerActions(kids_timer_action_type.ENDED, 0);
        kidsTimerActions(kids_timer_action_type.DISABLED, 0);

        if (!isRestarting) {
            stopAppScreenPinning();
            removeHandlers();
        } else {
            isRestarting = false;
        }
    }

    private void removeHandlers() {
        if (webView != null) {
            final ViewGroup webGroup = (ViewGroup) webView.getParent();
            if (webGroup != null) {
                webGroup.removeView(webView);
            }
        }
        if (repeatUpdateHandler != null) {
            repeatUpdateHandler.removeCallbacksAndMessages(null);
            repeatUpdateHandler = null;
        }

        if (mainapp.throttle_msg_handler != null) {
            mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);
            mainapp.throttle_msg_handler.removeCallbacksAndMessages(null);
            mainapp.throttle_msg_handler = null;
        } else {
            Log.d(threaded_application.applicationName, activityName + ": onDestroy(): mainapp.throttle_msg_handler is null. Unable to removeCallbacksAndMessages");
        }

        if (volumeKeysRepeatUpdateHandler != null) {
            volumeKeysRepeatUpdateHandler.removeCallbacks(gestureStopped);
            volumeKeysRepeatUpdateHandler.removeCallbacksAndMessages(null);
            volumeKeysRepeatUpdateHandler = null;
        }
        if (gamepadRepeatUpdateHandler != null) {
            gamepadRepeatUpdateHandler.removeCallbacksAndMessages(null);
            gamepadRepeatUpdateHandler = null;
        }
        if (IS_ESU_MCII) {
            esuMc2Led.setState(EsuMc2Led.RED, EsuMc2LedState.OFF);
            esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.OFF);
        }
        if (tg != null) {
            tg.release();
            tg = null;
        }
    }

    private void pauseWebView() {
        if (webView != null) {
            try {
                Method method = WebView.class.getMethod("onPause");
                method.invoke(webView);
            } catch (Exception e) {
                webView.pauseTimers();
            }
        }
    }

    private void resumeWebView() {
        if (webView != null) {
            try {
                Method method = WebView.class.getMethod("onResume");
                method.invoke(webView);
            } catch (Exception e) {
                webView.resumeTimers();
            }
        }
    }

    // load the url
    private void load_webview() {
        String url = currentUrl;
        webViewIsOn = !prefWebViewLocation.equals(web_view_location_type.NONE);
        if (!webViewIsOn) {                         // if not displaying webview
//            url = noUrl;
            currentUrl = null;
            firstUrl = null;
        } else {
            if (url == null || url.equals(noUrl)) {                // if initializing
                mainapp.defaultThrottleWebViewURL = prefs.getString("InitialThrottleWebPage",
                        getApplicationContext().getResources().getString(R.string.prefInitialThrottleWebPageDefaultValue));
                url = mainapp.createUrl(mainapp.defaultThrottleWebViewURL);
                if (url == null) {      //if port is invalid
                    url = noUrl;
                }
                firstUrl = null;
            }
            webView.loadUrl(url);
        }
    }

    void getStatusBarAndNavigationBarHeights() {
//        if ( (systemStatusRowHeightKeep==-1) || (systemNavigationRowHeightKeep==-1) ) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.throttle_screen_wrapper), (v, windowInsets) -> {
                int insetTypes = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
                Insets insets = windowInsets.getInsets(insetTypes);
                systemStatusRowHeightKeep = insets.top;

                Insets navBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
                systemNavigationRowHeightKeep = navBarInsets.bottom;

                // Return CONSUMED to stop the inset from bubbling to other views
                return WindowInsetsCompat.CONSUMED;
            });
//        }
    }

    void setAllFunctionLabelsAndListeners() {
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            set_function_labels_and_listeners_for_view(throttleIndex);
        }
    }

    // helper function to set up function buttons for each throttle
    // loop through all function buttons and
    // set label and dcc functions (based on settings) or hide if no label
    @SuppressLint("ClickableViewAccessibility")
    void set_function_labels_and_listeners_for_view(int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": set_function_labels_and_listeners_for_view() called");

//        // implemented in derived class, but called from this class

        if (functionButtonViewGroups != null) { // if it is null it probably because the Throttle Screen Type does not have Functions Buttons
            if (functionButtonViewGroups[0] != null) {
                ViewGroup tv; // group
                ViewGroup r; // row
                FunctionButtonTouchListener functionButtonTouchListener;
                Button b; // button
                int k = 0; // button count
                LinkedHashMap<Integer, String> function_labels_temp;
                LinkedHashMap<Integer, Button> functionButtonMap = new LinkedHashMap<>();

                tv = functionButtonViewGroups[whichThrottle];

                // note: we make a copy of function_labels_x because TA might change it
                // while we are using it (causing issues during button update below)
                function_labels_temp = mainapp.function_labels_default;
                if (!mainapp.prefAlwaysUseDefaultFunctionLabels) { //avoid npe
                    if (mainapp.function_labels != null) {
                        if ( (mainapp.function_labels[whichThrottle] != null)
                                && (!mainapp.function_labels[whichThrottle].isEmpty()) ) {
                            function_labels_temp = new LinkedHashMap<>(mainapp.function_labels[whichThrottle]);
                        } else { // roster function list is deliberately empty
                            if (mainapp.consists[whichThrottle].isLeadFromRoster()) {
//                            if (mainapp.consists[whichThrottle].isLeadFromRoster()
//                                    && mainapp.consists[whichThrottle].isLeadServerSuppliedFunctionLabels()) {
                                if (!mainapp.prefOverrideRosterWithNoFunctionLabels)
                                    function_labels_temp = new LinkedHashMap<>(mainapp.function_labels[whichThrottle]);
                            }
                        }
                    } else {
                        if (mainapp.consists != null) {  //avoid npe maybe
                            if (mainapp.consists[whichThrottle] != null
                                    && !mainapp.consists[whichThrottle].isLeadFromRoster()) {
                                function_labels_temp = mainapp.function_labels_default;
                            } else {
                                function_labels_temp = mainapp.function_labels_default_for_roster;
                            }
                        }
                    }
                }

                // put values in array for indexing in next step
                // to do this
                ArrayList<Integer> aList = new ArrayList<>(function_labels_temp.keySet());

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

                                functionButtonTouchListener = new FunctionButtonTouchListener(func, whichThrottle, bt);
                                b.setOnTouchListener(functionButtonTouchListener);

                                // if there is a long first word or the total length is long, reduce the font size
                                String [] words = bt.split(" ");
                                int longestWord = 0;
                                for (int l=0; l<words.length; l++) {
                                    if (words[l].length()>longestWord) longestWord = words[l].length();
                                }
                                int textSize = 18;
                                int maxLines = 2;
                                if ( (longestWord > 14) || (bt.length() > 22) ) {
                                    textSize = 10;
                                    maxLines = 3;
                                } else if ( (longestWord > 12) || (bt.length() > 19) ) {
                                    textSize = 12;
                                    maxLines = 3;
                                } else if ( (longestWord > 10) || (bt.length() > 16) ) {
                                    textSize = 14;
                                    maxLines = 3;
                                } else if ( (longestWord > 8) || (bt.length() > 13) ) {
                                    textSize = 16;
                                }
                                //noinspection UnnecessaryUnicodeEscape
                                bt = bt.replaceAll("-","-\u0020"); // replace hyphen with hyphen+small-space
                                b.setText(bt);
                                b.setVisibility(VISIBLE);
                                b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                                b.setMaxLines(maxLines);

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

    protected void setLabels() {
        Log.d(threaded_application.applicationName, activityName + ": setLabels()");

        if (mainapp.appIsFinishing) {
            return;
        }

        // avoid NPE by not letting this run too early (reported to Play Store)
        if (tvVolumeIndicators[0] == null) return;

        // hide or display volume control indicator based on variable
        setVolumeIndicator();
        setGamepadIndicator();

        // set up max speeds for throttles
        int maxThrottle = threaded_application.getIntPrefValue(prefs, "prefMaximumThrottle", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (maxThrottle * .01)); // convert from percent
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            sbs[throttleIndex].setMax(maxThrottle);
        }

        // set max allowed change for throttles from prefs
        int maxChange = threaded_application.getIntPrefValue(prefs, "prefMaximumThrottleChange", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleChangeDefaultValue));
        max_throttle_change = (int) Math.round(maxThrottle * (maxChange * .01));

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            sbs[throttleIndex].setMax(maxThrottle);
            if (mainapp.consists != null && mainapp.consists[throttleIndex] != null &&
                    mainapp.consists[throttleIndex].isEmpty()) {
                maxSpeedSteps[throttleIndex] = 100;
                limitSpeedMax[throttleIndex] = Math.round(100 * ((float) prefLimitSpeedPercent) / 100);
            }
            //get speed steps from prefs
            prefDisplaySpeedUnits = threaded_application.getIntPrefValue(prefs, "prefDisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
            setDisplayUnitScale(throttleIndex);

            setDisplayedSpeed(throttleIndex, sbs[throttleIndex].getProgress());  // update numeric speeds since units might have changed
        }

        refreshOverflowMenu();

        vThrottleScreenWrap.invalidate();
        // Log.d(threaded_application.applicationName, activityName + ": setLabels(): end");
    }

    private void refreshOverflowMenu() {
        if (overflowMenu == null) return;

        mainapp.actionBarIconCountThrottle = 0;

        mainapp.refreshCommonOverflowMenu(overflowMenu, findViewById(R.id.emergency_stop_button), findViewById(R.id.flashlight_button), findViewById(R.id.powerLayoutButton));

        if (IS_ESU_MCII) {
            displayEsuMc2KnobMenuButton(overflowMenu);
        }

        mainapp.displayDccExButton(overflowMenu);
        mainapp.setDCCEXMenuOption(overflowMenu);
        mainapp.setWithrottleCvProgrammerMenuOption(overflowMenu);

        showHideConsistMenus();

        mainapp.setGamepadTestMenuOption(overflowMenu, mainapp.gamepadCount);
        mainapp.setKidsMenuOptions(overflowMenu, prefKidsTimer.equals(PREF_KIDS_TIMER_NONE), mainapp.gamepadCount);

        mainapp.displayTimerMenuButton(overflowMenu, kidsTimerRunning);
        mainapp.displayThrottleSwitchMenuButton(overflowMenu);
        displayEsuMc2KnobMenuButton(overflowMenu);

        mainapp.displayDeviceSoundsThrottleButton(overflowMenu);

        mainapp.setTurnoutsMenuOption(overflowMenu);
        mainapp.displayTurnoutsButton(overflowMenu);

        mainapp.setRoutesMenuOption(overflowMenu);
        mainapp.displayRoutesButton(overflowMenu);

        mainapp.setWebMenuOption(overflowMenu);
        mainapp.displayWebButton(overflowMenu);
        mainapp.displayWebViewMenuButton(overflowMenu);

        adjustToolbarSize(overflowMenu);
    }

    @Override
    public boolean onKeyUp(int key, KeyEvent event) {

        if ((key == KEYCODE_VOLUME_UP) || (key == KEYCODE_VOLUME_DOWN)) {
            doVolumeButtonAction(event.getAction(), key, 0);
            return (true); // stop processing this key
        } else if ((key == KEYCODE_VOLUME_MUTE) || (key == KEYCODE_HEADSETHOOK)) {
            doMuteButtonAction(event.getAction());
            return (true); // stop processing this key
        }
        return (super.onKeyUp(key, event)); // continue with normal key
        // processing
    }

    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        int repeatCnt = event.getRepeatCount();

        if ((key == KEYCODE_VOLUME_UP) || (key == KEYCODE_VOLUME_DOWN)) {  // use volume to change speed for specified loco
            doVolumeButtonAction(event.getAction(), key, repeatCnt);
            mainapp.exitDoubleBackButtonInitiated = 0;
            return (true); // stop processing this key
        } else if ((key == KEYCODE_VOLUME_MUTE) || (key == KEYCODE_HEADSETHOOK)) {
            doMuteButtonAction(event.getAction());
            mainapp.exitDoubleBackButtonInitiated = 0;
            return (true); // stop processing this key
        }
        if (key != KEYCODE_BACK)
            mainapp.exitDoubleBackButtonInitiated = 0;

        return (super.onKeyDown(key, event)); // continue with normal key
        // processing
    }

    void doMuteButtonAction(int action) {
        if (action == ACTION_UP) {
            mVolumeKeysAutoIncrement = false;
            mVolumeKeysAutoDecrement = false;
        } else {
            if (!prefDisableVolumeKeys) {  // ignore the volume keys if the preference its set
                if (getSpeed(whichVolume) > 0) {
                    speedUpdateAndNotify(whichVolume, 0);
                } else {
                    int dir = (getDirection(whichVolume) == direction_type.REVERSE) ? direction_type.FORWARD : direction_type.REVERSE;
                    changeDirectionIfAllowed(whichVolume, dir);
                }
            }
        }
    }

    void doVolumeButtonAction(int action, int key, int repeatCnt) {
        if (action == ACTION_UP) {
            mVolumeKeysAutoIncrement = false;
            mVolumeKeysAutoDecrement = false;
        } else {
            if (!prefDisableVolumeKeys) {  // ignore the volume keys if the preference its set
                for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
                    if (throttleIndex == whichVolume && (mainapp.consists != null) && (mainapp.consists[throttleIndex] != null)
                            && (mainapp.consists[throttleIndex].isActive())) {
                        if (key == KEYCODE_VOLUME_UP) {
                            if (repeatCnt == 0) {
                                mVolumeKeysAutoIncrement = true;
                                volumeKeysRepeatUpdateHandler.post(new VolumeKeysRptUpdater(throttleIndex));
                            }
                        } else {
                            if (repeatCnt == 0) {
                                mVolumeKeysAutoDecrement = true;
                                volumeKeysRepeatUpdateHandler.post(new VolumeKeysRptUpdater(throttleIndex));
                            }
                        }
                    }
                }
            }
        }
    }

    private void releaseThrottles() {
        //loop thru all throttles and send release to server for any that are active
        for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
            if (getConsist(throttleIndex).isActive()) {
                releaseLoco(throttleIndex);
            }
        }
    }

    private void disconnect() {
        releaseThrottles();
        webView.stopLoading();
        mainapp.appIsFinishing = true;
        this.finish(); // end this activity
        connection_activity.overridePendingTransition(this, 0, 0);
    }

    private void endThisActivity() {
        releaseThrottles();
        webView.stopLoading();
        this.finish(); // end this activity
        connection_activity.overridePendingTransition(this, 0, 0);
    }

    private void shutdown() {
        releaseThrottles();
        webView.stopLoading();
        mainapp.appIsFinishing = true;
        this.finish(); // end this activity
        connection_activity.overridePendingTransition(this, 0, 0);
    }

    // request release of specified throttle
    void releaseLoco(int whichThrottle) {
        mainapp.storeThrottleLocosForReleaseDCCEX(whichThrottle);
        mainapp.consists[whichThrottle].release();
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", whichThrottle); // pass T, S or G in message
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.throttle_menu, menu);
        overflowMenu = menu;

        refreshOverflowMenu();

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint({"NonConstantResourceId", "ApplySharedPref"})
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        setImmersiveMode(webView);

        // Handle all of the possible menu actions.
        Intent in;
        if ( (item.getItemId() == R.id.turnouts_mnu)
        || (item.getItemId() == R.id.turnouts_button) ) {
            in = new Intent().setClass(this, turnouts.class);
            startACoreActivity(this, in, false, 0);
            return true;

        } else if ( (item.getItemId() == R.id.routes_mnu)
        || (item.getItemId() == R.id.routes_button) ) {
            in = new Intent().setClass(this, routes.class);
            startACoreActivity(this, in, false, 0);
            return true;

        } else if ( (item.getItemId() == R.id.web_mnu)
        || (item.getItemId() == R.id.web_button) ) {
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

        } else if ((item.getItemId() == R.id.dcc_ex_button) || (item.getItemId() == R.id.dcc_ex_mnu)) {
            in = new Intent().setClass(this, dcc_ex.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        } else if (item.getItemId() == R.id.withrottle_cv_programmer_mnu) {
            in = new Intent().setClass(this, withrottle_cv_programmer.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        } else if (item.getItemId() == R.id.settings_mnu) {
            startSettingsActivity();
            return true;

        } else if (item.getItemId() == R.id.function_defaults_mnu) {
            in = new Intent().setClass(this, function_settings.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        } else if (item.getItemId() == R.id.function_consist_settings_mnu) {
            in = new Intent().setClass(this, function_consist_settings.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        } else if (item.getItemId() == R.id.connect_menu) {
            final AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setIcon(android.R.drawable.ic_dialog_alert);
            b.setTitle(R.string.newConnectionTitle);
            b.setMessage(R.string.newConnectionText);
            b.setCancelable(true);
            b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    threaded_application.activityInTransition(activityName);
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DISCONNECT, "");
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent in = new Intent().setClass(throttle.this, connection_activity.class);
                            startActivity(in);
                            connection_activity.overridePendingTransition(throttle.this, R.anim.fade_in, R.anim.fade_out);
                        }
                    }, 2000);
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
            return true;

        } else if (item.getItemId() == R.id.about_mnu) {
            in = new Intent().setClass(this, about_page.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        } else if (item.getItemId() == R.id.logviewer_menu) {
            Intent logviewer = new Intent().setClass(this, LogViewerActivity.class);
            startActivity(logviewer);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        } else if (item.getItemId() == R.id.emergency_stop_button) {
            mainapp.sendEStopMsg();
            speedUpdate(0);  // update all throttles
            applySpeedRelatedOptions();  // update all throttles
            if (IS_ESU_MCII && !isSemiRealisticThrottle) {
                Log.d(threaded_application.applicationName, activityName + ": onOptionsItemSelected(): ESU_MCII: Move knob request for EStop");
                setEsuThrottleKnobPosition(whichVolume, 0);
            }
            mainapp.buttonVibration();
            return true;

        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(overflowMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;

        } else if (item.getItemId() == R.id.EditConsist0_menu) {
            startConsistEditActivity(0);
            return true;

        } else if (item.getItemId() == R.id.EditConsist1_menu) {
            startConsistEditActivity(1);
            return true;

        } else if (item.getItemId() == R.id.EditConsist2_menu) {
            startConsistEditActivity(2);
            return true;

        } else if (item.getItemId() == R.id.EditLightsConsist0_menu) {
            startConsistLightsEditActivity(0);
            return true;

        } else if (item.getItemId() == R.id.EditLightsConsist1_menu) {
            startConsistLightsEditActivity(1);
            return true;

        } else if (item.getItemId() == R.id.EditLightsConsist2_menu) {
            startConsistLightsEditActivity(0);
            return true;

        } else if (item.getItemId() == R.id.gamepad_test_reset) {
            mainapp.gamepadFullReset();
            mainapp.setGamepadTestMenuOption(overflowMenu, mainapp.gamepadCount);
            setGamepadIndicator();
            tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST_RESET);
            return true;

        } else if (item.getItemId() == R.id.gamepad_test_mnu1) {
            startGamepadTestActivity(0);
            return true;

        } else if (item.getItemId() == R.id.gamepad_test_mnu2) {
            startGamepadTestActivity(1);
            return true;

        } else if (item.getItemId() == R.id.gamepad_test_mnu3) {
            startGamepadTestActivity(2);
            return true;

        } else if (item.getItemId() == R.id.timer_mnu) {
            showTimerPasswordDialog();
            return true;

        } else if (item.getItemId() == R.id.timer_button) {
            if (mainapp.consists[0].isActive()) {
                prefKidsTime = Integer.parseInt(prefKidsTimerButtonDefault) * 60000;
                prefKidsTimer = prefKidsTimerButtonDefault;
                prefs.edit().putString("prefKidsTimer", prefKidsTimerButtonDefault).commit();
                kidsTimerActions(kids_timer_action_type.ENABLED, 0);
            } else {
                mainapp.safeToast(R.string.toastKidsTimerNoLoco, Toast.LENGTH_SHORT);
            }
            return true;

        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlightActionView(this, overflowMenu, findViewById(R.id.flashlight_button));
            mainapp.buttonVibration();
            return true;

        } else if (item.getItemId() == R.id.throttle_switch_button) {
            switchThrottleScreenType();
            mainapp.buttonVibration();
            return true;

        } else if (item.getItemId() == R.id.web_view_button) {
            showHideWebView("");
            return true;

        } else if (item.getItemId() == R.id.esu_mc2_knob_button) {
            toggleEsuMc2Knob(this, overflowMenu);
            return true;

        } else if ((item.getItemId() == R.id.device_sounds_button) || (item.getItemId() == R.id.device_sounds_menu)) {
            startDeviceSoundsSettingsActivity();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // handle return from menu items
    // this is now largely superseded by the individual handlers
    // but this is still called after they are processed
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        setAllFunctionLabelsAndListeners();
        setLabels();
        soundsShowHideAllMuteButtons();
    }

    private void activityConsistUpdate(Bundle extras) {
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
//        Log.d(threaded_application.applicationName, activityName + ": onTouchEvent(): action: " + event.getAction());
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
//        Log.d(threaded_application.applicationName, activityName + ": dispatchTouchEvent():");
        // if screen is locked
        if (isScreenLocked) {
            // check if we have a swipe up
            if (ev.getAction() == ACTION_DOWN) {
//                Log.d(threaded_application.applicationName, activityName + ": dispatchTouchEvent(): ACTION_DOWN" );
                gestureStart(ev);
            }
            if (ev.getAction() == ACTION_UP) {
//                Log.d(threaded_application.applicationName, activityName + ": dispatchTouchEvent(): ACTION_UP" );
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
//        Log.d(threaded_application.applicationName, activityName + ": gestureStart(): x=" + gestureStartX + " y=" + gestureStartY);

        toolbarHeight = mainapp.getToolbarHeight(toolbar, statusLine, screenNameLine);

        // check if the sliders are already hidden by preference
        if (!prefs.getBoolean("prefHideSlider", false)) {
            // if gesture is attempting to start over an enabled slider, ignore it and return immediately.
            for (int throttleIndex = 0; throttleIndex < mainapp.prefNumThrottles; throttleIndex++) {
                if ((sbs[throttleIndex].isEnabled())
                        && (
                        ((gestureStartX >= sliderTopLeftX[throttleIndex])
                                && (gestureStartX <= sliderBottomRightX[throttleIndex])
                                && (gestureStartY >= sliderTopLeftY[throttleIndex])
                                && (gestureStartY <= sliderBottomRightY[throttleIndex])
                        ) || (
                                (gestureStartX >= brakeSliderTopLeftX[throttleIndex])
                                        && (gestureStartX <= brakeSliderBottomRightX[throttleIndex])
                                        && (gestureStartY >= brakeSliderTopLeftY[throttleIndex])
                                        && (gestureStartY <= brakeSliderBottomRightY[throttleIndex])
                        ) || (
                                (gestureStartX >= loadSliderTopLeftX[throttleIndex])
                                        && (gestureStartX <= loadSliderBottomRightX[throttleIndex])
                                        && (gestureStartY >= loadSliderTopLeftY[throttleIndex])
                                        && (gestureStartY <= loadSliderBottomRightY[throttleIndex])
                        )
                )
                ) {
//                    Log.d(threaded_application.applicationName, activityName + ": gestureStart() exit on slider: " + gestureStartX + ", " + gestureStartY);
                    return;
                }
            }
        }
        gestureInProgress = true;
        gestureFailed = false;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.throttle_msg_handler != null) {
//            Log.d(threaded_application.applicationName, activityName + ": gestureStart(): start gesture timer");
            mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
        } else {
            Log.d(threaded_application.applicationName, activityName + ": gestureStart(): Can't start gesture timer");
        }
    }

    public void gestureMove(MotionEvent event) {
//        Log.d(threaded_application.applicationName, activityName + ": gestureMove(): action " + event.getAction() + " eventTime: " + event.getEventTime() );
        if (gestureInProgress) {
            // stop the gesture timeout timer
            if (mainapp.throttle_msg_handler != null)
                mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);

            mVelocityTracker.addMovement(event);
            if ((event.getEventTime() - gestureLastCheckTime) > gestureCheckRate) {
                // monitor velocity and fail gesture if it is too low
                gestureLastCheckTime = event.getEventTime();
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                int velocityY = (int) velocityTracker.getYVelocity();
//                Log.d(threaded_application.applicationName, activityName + ": gestureMove(): gestureVelocity vel " + velocityX);
                if ((Math.abs(velocityX) < threaded_application.min_fling_velocity) && (Math.abs(velocityY) < threaded_application.min_fling_velocity)) {
                    gestureFailed(event);
                }
            }
//            else {
//                Log.d(threaded_application.applicationName, activityName + ": gestureMove(): event.getEventTime(): " +event.getEventTime()   + " gestureLastCheckTime: " + gestureLastCheckTime + " gestureCheckRate: " + gestureCheckRate);
//                Log.d(threaded_application.applicationName, activityName + ": gestureMove(): event.getEventTime() - gestureLastCheckTime: " + (event.getEventTime() - gestureLastCheckTime) + " gestureCheckRate: " + gestureCheckRate);
//            }
            if (gestureInProgress) {
                // restart the gesture timeout timer
//                Log.d(threaded_application.applicationName, activityName + ": gestureMove(): restart gesture timer");
                if (mainapp.throttle_msg_handler != null)
                    mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
//        Log.d(threaded_application.applicationName, activityName + ": gestureEnd(): action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ((mainapp != null) && (mainapp.throttle_msg_handler != null) && (gestureInProgress)) {
            mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float deltaY = (event.getY() - gestureStartY);
            float absDeltaX = Math.abs(deltaX);
            float absDeltaY = Math.abs(deltaY);
            if ((absDeltaX > threaded_application.min_fling_distance) || (absDeltaY > threaded_application.min_fling_distance)) {
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                if (absDeltaX >= absDeltaY) {

                    // check if only allow left-right swipe in the tool bar
                    if ((!mainapp.prefFullScreenSwipeArea) // full screen swipe allowed
                            || ((mainapp.prefFullScreenSwipeArea) && (gestureStartY <= toolbarHeight)
                            && ((!mainapp.prefThrottleViewImmersiveModeHideToolbar)
                            || ((mainapp.prefThrottleViewImmersiveModeHideToolbar) && (!immersiveModeIsOn))))
                    ) {   // not in the toolbar area  or the toolbar is hidden

                        // swipe left/right
                        if (!isScreenLocked) {
                            Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(screen_swipe_index_type.THROTTLE, deltaX);
                            startACoreActivity(this, nextScreenIntent, true, deltaX);
                        }
                    } else {
                        if (mainapp.prefLeftRightSwipeChangesSpeed) {
                            if (deltaX < 0.0) {  // swipe right
                                decrementSpeed(whichVolume, speed_commands_from_type.BUTTONS, 1, mainapp.prefSwipeSpeedChangeStep);
                            } else {
                                incrementSpeed(whichVolume, speed_commands_from_type.BUTTONS, 1, mainapp.prefSwipeSpeedChangeStep);
                            }
                        }
                    }
                } else {
                    // swipe up/down
                    String upDown;
                    if (deltaY > 0.0) {  // swipe down
                        upDown = prefSwipeDownOption;
                    } else { // Swipe up
                        upDown = prefSwipeUpOption;
                    }
                    switch (upDown) {
                        case swipe_up_down_option_type.WEB:
                            showHideWebView(getApplicationContext().getResources().getString(R.string.toastSwipeUpViewHidden));
                            break;
                        case swipe_up_down_option_type.LOCK:
                            setRestoreScreenLockDim(getApplicationContext().getResources().getString(R.string.toastSwipeUpDownScreenLocked));
                            break;
                        case swipe_up_down_option_type.DIM:
                            setRestoreScreenDim(getApplicationContext().getResources().getString(R.string.toastSwipeUpDownScreenDimmed));
                            break;

//                        case swipe_up_down_option_type.IMMERSIVE:
//                            if (immersiveModeIsOn) {
//                                immersiveModeTempIsOn = false;
//                                setImmersiveModeOff(webView, true);
//                                mainapp.safeToast(getApplicationContext().getResources().getString(R.string.toastImmersiveModeDisabled), Toast.LENGTH_SHORT);
//                            } else {
//                                immersiveModeTempIsOn = true;
//                                setImmersiveModeOn(webView, true);
//                            }
//                            setLabels();
////                            throttleScreenWrapper.invalidate();
//                            vThrottleScreenWrap.invalidate();
//                            break;

                        case swipe_up_down_option_type.SWITCH_LAYOUTS:
                            switchThrottleScreenType();
                            break;
                            
                        case swipe_up_down_option_type.CHANGE_SPEED:
                            if (deltaY > 0.0) {  // swipe down
                                decrementSpeed(whichVolume, speed_commands_from_type.BUTTONS, 1, mainapp.prefSwipeSpeedChangeStep);
                            } else {
                                incrementSpeed(whichVolume, speed_commands_from_type.BUTTONS, 1, mainapp.prefSwipeSpeedChangeStep);
                            }
                            break;

                        case swipe_up_down_option_type.NEXT_VOLUME:
                            setNextActiveThrottle(true);
                            adjustThrottleHeights();
                            break;
                    }
                }
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
//        Log.d(threaded_application.applicationName, activityName + ": gestureEnd(): gestureCancel");
        if (mainapp.throttle_msg_handler != null)
            mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);
        gestureInProgress = false;
        gestureFailed = true;
    }

    void gestureFailed(MotionEvent event) {
//        Log.d(threaded_application.applicationName, activityName + ": gestureEnd gestureFailed");
        // end the gesture
        gestureInProgress = false;
        gestureFailed = true;
    }

    //
    // GestureStopped runs when more than gestureCheckRate milliseconds
    // elapse between onGesture events (i.e. press without movement).
    //
    @SuppressLint("Recycle")
    private final Runnable gestureStopped = new Runnable() {
        @Override
        public void run() {
//            Log.d(threaded_application.applicationName, activityName + ": gestureStopped()");
            if (gestureInProgress) {
                // end the gesture
                gestureInProgress = false;
                gestureFailed = true;
                // create a MOVE event to trigger the underlying control
                if (vThrottleScreen != null) {
//                    Log.d(threaded_application.applicationName, activityName + ": gestureStopped vThrottleScreen != null");
                    // use uptimeMillis() rather than 0 for time in
                    // MotionEvent.obtain() call in throttle gestureStopped:
                    MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, gestureStartX,
                            gestureStartY, 0);
                    try {
                        vThrottleScreen.dispatchTouchEvent(event);
                    } catch (IllegalArgumentException e) {
                        Log.d(threaded_application.applicationName, activityName + ": gestureStopped trigger IllegalArgumentException, OS " + android.os.Build.VERSION.SDK_INT);
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
    public void promptForSteal(final String addr, final int whichThrottle) {
        if (stealPromptActive) return;
        stealPromptActive = true;
        final AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(R.string.steal_title);
        b.setMessage(getString(R.string.steal_text, addr));
        b.setCancelable(true);
        b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() { //if yes pressed, tell ta to proceed with steal
            public void onClick(DialogInterface dialog, int id) {
                mainapp.exitDoubleBackButtonInitiated = 0;
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.STEAL, addr, whichThrottle);
                stealPromptActive = false;
                mainapp.buttonVibration();
            }
        });
        b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() { //if no pressed do nothing
            public void onClick(DialogInterface dialog, int id) {
                mainapp.exitDoubleBackButtonInitiated = 0;
                stealPromptActive = false;
                mainapp.buttonVibration();
            }
        });
        AlertDialog alert = b.create();
        alert.show();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    protected void setThrottleNumLimits() {
        if (mainapp.prefNumThrottles > mainapp.maxThrottlesCurrentScreen) {   // Maximum number of throttles this screen supports
            mainapp.prefNumThrottles = mainapp.maxThrottlesCurrentScreen;
        }
    }


    private void showTimerPasswordDialog() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getApplicationContext().getResources().getString(R.string.timerDialogTitle));
        alert.setMessage(getApplicationContext().getResources().getString(R.string.timerDialogMessage));

// Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint(getApplicationContext().getResources().getString(R.string.timerDialogHint));
        alert.setView(input);
        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        input.requestFocus();

        alert.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            public void onClick(DialogInterface dialog, int whichButton) {
                mainapp.exitDoubleBackButtonInitiated = 0;
                passwordText = input.getText().toString();
                Log.d(threaded_application.applicationName, activityName + ": showTimerPasswordDialog(): onClick(): Password Value : " + passwordText);

                if (passwordText.equals(prefKidsTimerResetPassword)) { //reset
                    kidsTimerActions(kids_timer_action_type.ENDED, 0);
                    kidsTimerActions(kids_timer_action_type.DISABLED, 0);
                    prefs.edit().putString("prefKidsTimer", PREF_KIDS_TIMER_NONE).commit();  //reset the preference
                    getKidsTimerPrefs();
                }

                if (passwordText.equals(prefKidsTimerDemoModePassword)) { //Demo Mode
                    kidsTimerActions(kids_timer_action_type.DEMO, 0);
                    prefs.edit().putString("prefKidsTimer", PREF_KIDS_TIMER_NONE).commit();  //reset the preference
                    getKidsTimerPrefs();
                }

                if (passwordText.equals(prefKidsTimerRestartPassword)) { //restart
                    kidsTimerActions(kids_timer_action_type.ENABLED, 0);
                }

                mainapp.hideSoftKeyboard(input);
                mainapp.buttonVibration();
            }
        });

        alert.setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mainapp.exitDoubleBackButtonInitiated = 0;
                        mainapp.hideSoftKeyboard(input);
                        mainapp.buttonVibration();
                    }
                });

        AlertDialog dialog = alert.create();

        // Show the keyboard after the dialog is shown
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                input.requestFocus(); // Ensure focus is set
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        // Alternative method to show keyboard using WindowManager flags (might be more reliable in some cases)
        // dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();

    } // end showTimerPasswordDialog

    private void startAppScreenPinning() {
        if (!prefKidsTimerKioskMode) return;

        if (Build.VERSION.SDK_INT >= 23) {
            // Check if the activity is already in lock task mode.
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null && activityManager.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
                // App is not yet pinned.
                // You can start screen pinning. The user will typically see a confirmation dialog.
                try {
                    Log.d(threaded_application.applicationName, activityName + "Attempting to start screen pinning.");
                    startLockTask(); // This is the call to request pinning for the current task
                } catch (Exception e) {
                    Log.e(threaded_application.applicationName, activityName + "Error starting screen pinning: " + e.getMessage());
                    // Handle cases where pinning might fail (e.g., policy disallows it)
                    // You might want to inform the user or log this.
                }
            } else if (activityManager != null) {
                Log.d(threaded_application.applicationName, activityName + "Screen pinning is already active or in a non-NONE state: " + activityManager.getLockTaskModeState());
            } else {
                Log.e(threaded_application.applicationName, activityName + "ActivityManager is null, cannot check or start screen pinning.");
            }
        }
    }

    private void stopAppScreenPinning() {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null && activityManager.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE) {
                try {
                    Log.d(threaded_application.applicationName, activityName + "Attempting to stop screen pinning.");
                    stopLockTask(); // This is the call to stop pinning
                } catch (Exception e) {
                    Log.e(threaded_application.applicationName, activityName + "Error stopping screen pinning: " + e.getMessage());
                }
            } else if (activityManager != null) {
                Log.d(threaded_application.applicationName, activityName + "Screen pinning is not currently active.");
            } else {
                Log.e(threaded_application.applicationName, activityName + "ActivityManager is null, cannot check or stop screen pinning.");
            }
        }
    }


    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@RequestCodes int requestCode) {
        Log.d(threaded_application.applicationName, activityName + ": navigateToHandler:" + requestCode);
        if (!PermissionsHelper.getInstance().isPermissionGranted(throttle.this, requestCode)) {
            if (Build.VERSION.SDK_INT >= 23) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(throttle.this, requestCode);
            }
        } else {
            // Go to the correct handler based on the request code.
            // Only need to consider relevant request codes initiated by this Activity
            //            switch (requestCode) {
//                case PermissionsHelper.READ_SERVER_AUTO_PREFERENCES:
//                    Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Got permission for READ_SERVER_AUTO_PREFERENCES");
//                    autoImportUrlAskToImportImpl();
//                    break;
//                case PermissionsHelper.STORE_SERVER_AUTO_PREFERENCES:
//                    Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Got permission for STORE_SERVER_AUTO_PREFERENCES");
//                    autoImportFromURLImpl();
//                    break;
//                default:
                    // do nothing
                    Log.d(threaded_application.applicationName, activityName + ": navigateToHandler(): Unrecognised permissions request code: " + requestCode);
//            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(throttle.this, requestCode, permissions, grantResults)) {
            Log.d(threaded_application.applicationName, activityName + ": onRequestPermissionsResult(): Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public class AutoImportFromURL  implements Runnable{   // Background Async Task to download file

        public AutoImportFromURL() {
            new Thread(this).start();
        }

        // Importing file in background thread
        @SuppressLint("ApplySharedPref")
        public void run() {
            Log.d(threaded_application.applicationName, activityName + ": Import preferences from Server: start");
            int count;
            String n_url;

            if ((mainapp.connectedHostip != null)) {
                n_url = "http://" + mainapp.connectedHostip + ":" + mainapp.web_server_port
                        + "/" + SERVER_ENGINE_DRIVER_DIR + "/" + EXTERNAL_PREFERENCES_IMPORT_FILENAME;
            } else {
                return; // not currently connected
            }

            String urlPreferencesFileName;
            String urlPreferencesFilePath;
            URLConnection connection;
            URL url;

            try {
                urlPreferencesFileName = "auto_" + mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                urlPreferencesFilePath = getApplicationContext().getExternalFilesDir(null) + "/" + urlPreferencesFileName;
                Log.d(threaded_application.applicationName, activityName + ": Import preferences from Server: linkg for: " + urlPreferencesFilePath);
                url = new URL(n_url);

                connection = url.openConnection();
                connection.connect();

            } catch (Exception e) {
                Log.d(threaded_application.applicationName, activityName + ": Auto import preferences from Server Failed: " + e.getMessage());
                return;
            }

            try {
                Date urlDate = new Date(connection.getLastModified());

                //                need to compare the dates here
                File localFile = new File(urlPreferencesFilePath);
                Date localDate;
                if (localFile.exists()) {
                    long timestamp = localFile.lastModified();
                    localDate = new Date(timestamp);

                    if (localDate.compareTo(urlDate) >= 0) {
                        Log.d(threaded_application.applicationName, activityName + ": Auto Import preferences from Server: Local file is up-to-date: " + localFile);
                        return;
//                    } else {
//                        Log.d(threaded_application.applicationName, activityName + ": Import preferences from Server: Local file is newer. Date " + localDate.toString());
                    }
                }

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

//                File Directory = new File(ENGINE_DRIVER_DIR); // in case the folder does not already exist

                FileOutputStream output = new FileOutputStream(urlPreferencesFilePath);

                byte[] data = new byte[1024];

                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                prefs.edit().putString("prefPreferencesImportFileName", urlPreferencesFilePath).commit();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.IMPORT_SERVER_AUTO_AVAILABLE, "", 0);

            } catch (Exception e) {
                Log.e(threaded_application.applicationName, activityName + ": Auto import preferences from Server Failed: " + e.getMessage());
            }

            Log.d(threaded_application.applicationName, activityName + ": Auto Import preferences from Server: End");
        }
    }


    private void autoImportUrlAskToImport() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            //@Override
            @SuppressLint("ApplySharedPref")
            public void onClick(DialogInterface dialog, int which) {
                mainapp.exitDoubleBackButtonInitiated = 0;

//                String deviceId = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                String deviceId = mainapp.getFakeDeviceId();
                String urlPreferencesFileName = "auto_" + mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        prefs.edit().putString("prefPreferencesImportAll", pref_import_type.ALL_FULL).commit();
                        loadSharedPreferencesFromFileImpl(prefs, urlPreferencesFileName, deviceId, restart_reason_type.IMPORT_SERVER_AUTO);

                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        prefs.edit().putString("prefPreferencesImportAll", pref_import_type.ALL_PARTIAL).commit();
                        loadSharedPreferencesFromFileImpl(prefs, urlPreferencesFileName, deviceId, restart_reason_type.IMPORT_SERVER_AUTO);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        prefs.edit().putString("prefPreferencesImportAll", pref_import_type.ALL_RESET).commit();
                        break;
                }
                mainapp.buttonVibration();
            }
        };

        AlertDialog.Builder ab = new AlertDialog.Builder(throttle.this);
        ab.setMessage(getApplicationContext().getResources().getString(R.string.importServerAutoDialog, mainapp.connectedHostName))
                .setPositiveButton(R.string.importServerAutoDialogPositiveButton, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener)
                .setNeutralButton(R.string.importServerAutoDialogNeutralButton, dialogClickListener);
        ab.show();
    }

    // restart the app so that the new preferences can be applied
    // note: should verify that permissions have been granted before calling this method since it will try to read the preference file
    private void loadSharedPreferencesFromFileImpl(SharedPreferences sharedPreferences, String exportedPreferencesFileName, String deviceId, int forceRestartReason) {
        Log.d(threaded_application.applicationName, activityName + ": loadSharedPreferencesFromFileImpl(): Loading saved preferences from file: " + exportedPreferencesFileName);
        boolean result = importExportPreferences.loadSharedPreferencesFromFile(mainapp, getApplicationContext(), sharedPreferences, exportedPreferencesFileName, deviceId, false);

        if (!result) {
            mainapp.safeToast(String.format(getApplicationContext().getResources().getString(R.string.prefImportExportErrorReadingFrom), exportedPreferencesFileName), Toast.LENGTH_LONG);
        }
        forceRestartApp(forceRestartReason);
    }

    @SuppressLint("ApplySharedPref")
    public void forceRestartApp(int forcedRestartReason) {
        Log.d(threaded_application.applicationName, activityName + ": forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

    @SuppressLint("ApplySharedPref")
    public void forceReLaunchApp(int forcedRestartReason) {
        Log.d(threaded_application.applicationName, activityName + ": forceRelaunchApp() ");

        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        Message msg = Message.obtain();
        msg.what = message_type.RELAUNCH_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);

    }

    // implemented in derived class, but called from this class
    protected void redrawSliders() {
    }

    private void redrawVerticalSliders() {
        if (vsbSpeeds != null) {
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                vsbSpeeds[throttleIndex].tickMarksChecked = false;
                vsbSpeeds[throttleIndex].invalidate();
            }
        }

        if (vsbSwitchingSpeeds != null) {
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                vsbSwitchingSpeeds[throttleIndex].tickMarksChecked = false;
                vsbSwitchingSpeeds[throttleIndex].invalidate();
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    protected void switchThrottleScreenType() {
        Log.d(threaded_application.applicationName, activityName + ": switchThrottleScreenType() ");

        if(!mainapp.throttleSwitchAllowed) return;
        Log.d(threaded_application.applicationName, activityName + ": switchThrottleScreenType() Allowed");

        mainapp.throttleSwitchWasRequestedOrReinitialiseRequired = true;

        boolean prefThrottleSwitchButtonCycleAll = prefs.getBoolean("prefThrottleSwitchButtonCycleAll", getApplicationContext().getResources().getBoolean(R.bool.prefThrottleSwitchButtonCycleAllDefaultValue));
        String prefThrottleSwitchOption1 = prefs.getString("prefThrottleSwitchOption1", getApplicationContext().getResources().getString(R.string.prefThrottleSwitchOption1DefaultValue));
        String prefThrottleSwitchOption2 = prefs.getString("prefThrottleSwitchOption2", getApplicationContext().getResources().getString(R.string.prefThrottleSwitchOption2DefaultValue));

//        int maxThrottlesCurrentScreenTypeOriginal = mainapp.getMaxThrottlesForScreen(prefThrottleScreenType);
        String numThrottles;

        if (!prefWebViewLocation.equals(keepWebViewLocation)) {
            showHideWebView("");
        }

        String prefName;
        if (!prefThrottleSwitchButtonCycleAll) {
            if (prefThrottleScreenType.equals(prefThrottleSwitchOption1)) {
                prefThrottleScreenType = prefThrottleSwitchOption2;
                prefName = "prefThrottleSwitchOption2NumThrottles";
            } else {
                prefThrottleScreenType = prefThrottleSwitchOption1;
                prefName = "prefThrottleSwitchOption1NumThrottles";
            }
        } else {
            prefThrottleScreenType = mainapp.getNextThrottleLayout();
            prefName = "NumThrottle";
        }
        numThrottles = prefs.getString(prefName, getResources().getString(R.string.prefNumThrottlesDefaultValue));
        mainapp.prefNumThrottles = mainapp.Numeralise(numThrottles);

        prefs.edit().putString("prefThrottleScreenType", prefThrottleScreenType).commit();
        prefs.edit().putString("NumThrottle", numThrottles).commit();
        prefs.edit().putString("prefWebViewLocation", prefWebViewLocation).commit();
        fixNumThrottles();

//        if (maxThrottlesCurrentScreenTypeOriginal >= mainapp.getMaxThrottlesForScreen(prefThrottleScreenType)) {
            forceRestartApp(restart_reason_type.THROTTLE_SWITCH);
//        } else {
//            forceReLaunchApp(restart_reason_type.THROTTLE_SWITCH);
//        }
    }

    //listeners for the Pause Speed Button
    protected class PauseSpeedButtonTouchListener implements View.OnTouchListener {
        int whichThrottle;

        protected PauseSpeedButtonTouchListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                pauseSpeed(whichThrottle);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mainapp.buttonVibration();
            }
            return false;
        }
    }

    protected void pauseSpeed(int whichThrottle) {
        int speed = 0;

        switch (isPauseSpeeds[whichThrottle]) {
            case pause_speed_type.ZERO: {
                isPauseSpeeds[whichThrottle] = pause_speed_type.START_RETURN;
                bPauseSpeeds[whichThrottle].setSelected(false);
                if (bPauses[whichThrottle] != null) bPauses[whichThrottle].setSelected(false);
                speed = getSpeed(whichThrottle);
                break;
            }
            case pause_speed_type.INACTIVE: {
                if (getSpeed(whichThrottle) == 0) return;

                isPauseSpeeds[whichThrottle] = pause_speed_type.START_TO_ZERO;
                bPauseSpeeds[whichThrottle].setSelected(true);
                if (bPauses[whichThrottle] != null) bPauses[whichThrottle].setSelected(true);
                pauseSpeed[whichThrottle] = getSpeed(whichThrottle);
                pauseDir[whichThrottle] = getDirection(whichThrottle);
                break;
            }
            case pause_speed_type.TO_RETURN:
            case pause_speed_type.TO_ZERO:
            default: {
                disablePauseSpeed(whichThrottle);
                break;
            }
        }

        if (isPauseSpeeds[whichThrottle] != pause_speed_type.INACTIVE) {
            setSpeed(whichThrottle, speed, speed_commands_from_type.BUTTONS);
        }
    }

    void disablePauseSpeed(int whichThrottle) {
        setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
        bPauseSpeeds[whichThrottle].setSelected(false);
        if (bPauses[whichThrottle] != null) bPauses[whichThrottle].setSelected(false);
        isPauseSpeeds[whichThrottle] = pause_speed_type.INACTIVE;
        limitedJump[whichThrottle] = false;
    }

    @SuppressLint("ApplySharedPref")
    private void fixNumThrottles() {
        int numThrottles = mainapp.Numeralise(prefs.getString("NumThrottle", getResources().getString(R.string.prefNumThrottlesDefaultValue)));
        String prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        int index = -1;
        String[] textNumbers = this.getResources().getStringArray(R.array.NumOfThrottlesEntryValues);
        String[] arr = this.getResources().getStringArray(R.array.prefThrottleScreenTypeEntryValues);
        int[] fixed = this.getResources().getIntArray(R.array.prefThrottleScreenTypeFixedThrottleNumber);
        int[] max = this.getResources().getIntArray(R.array.prefThrottleScreenTypeMaxThrottleNumber);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(prefThrottleScreenType)) {
                index = i;
            }
        }
        if (index < 0) return; //bail if no matches

        if (((fixed[index] == 1) && (numThrottles != max[index]))
                || ((fixed[index] == 0) && (numThrottles > max[index]))) {
            prefs.edit().putString("NumThrottle", textNumbers[max[index] - 1]).commit();
            mainapp.safeToast(String.format(getApplicationContext().getResources().getString(R.string.toastNumThrottles), textNumbers[max[index] - 1]), Toast.LENGTH_LONG);
        }
    }

    int getDisplaySpeedFromCurrentSliderPosition(int whichThrottle, boolean useScale) {
        return getSpeedFromCurrentSliderPosition(whichThrottle, useScale) + 1;
    }

    int getSpeedFromCurrentSliderPosition(int whichThrottle, boolean useScale) {
        // separate versions of this exist for the switching throttle layouts
        int speed;
        if (!useScale) {
            speed = getSpeed(whichThrottle);
        } else {
            speed = getScaleSpeed(whichThrottle);
        }
        return speed;
    } // end getSpeedFromCurrentSliderPosition()

    protected void setSoundButtonState(Button btn, boolean state) {
        if (state) {
            btn.setSelected(true);
            btn.setPressed(true);
            btn.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
        } else {
            btn.setSelected(false);
            btn.setPressed(false);
            btn.setTypeface(null, Typeface.NORMAL);
        }

    }

    //listeners for the Mute Button
    protected class SoundDeviceMuteButtonTouchListener implements View.OnTouchListener {
        int whichThrottle;

        protected SoundDeviceMuteButtonTouchListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.playSoundEffect(SoundEffectConstants.CLICK);
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                soundsIsMuted[whichThrottle] = !soundsIsMuted[whichThrottle];
                setSoundButtonState(bMutes[whichThrottle], soundsIsMuted[whichThrottle]);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mainapp.buttonVibration();
            }
            if(ipls!=null) ipls.muteUnmuteCurrentSounds(whichThrottle, soundsIsMuted[whichThrottle]);
            return true;
        }
    }

    protected class SoundDeviceExtrasButtonTouchListener implements View.OnTouchListener {
        int whichThrottle;
        int buttonType;
        int soundType;

        protected SoundDeviceExtrasButtonTouchListener(int new_whichThrottle, int myButtonType, int mySoundType) {
            whichThrottle = new_whichThrottle;
            buttonType = myButtonType;
            soundType = mySoundType;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.playSoundEffect(SoundEffectConstants.CLICK);
                mainapp.buttonVibration();
            }

            // if gesture in progress, skip button processing
            if (gestureInProgress) {
                return (true);
            }

            if (((buttonType == sounds_type.BUTTON_BELL) && (mainapp.prefDeviceSoundsBellIsMomentary))
                    || (buttonType == sounds_type.BUTTON_HORN)
                    || (buttonType == sounds_type.BUTTON_HORN_SHORT)
            ) {
                // if gesture just failed, insert one DOWN event on this control
                if (gestureFailed) {
                    handleAction(MotionEvent.ACTION_DOWN);
                    gestureFailed = false;  // just do this once
                }
            }
            handleAction(event.getAction());
            return true;
        }

        private void handleAction(int action) {
            handleDeviceButtonAction(whichThrottle, buttonType, soundType, action);
        }
    }

    private void handleDeviceButtonAction(int whichThrottle, int buttonType, int soundType, int action) {
        Log.d(threaded_application.applicationName, activityName + ": handleDeviceButtonAction(): handleAction - action: " + action);

        if ((buttonType == sounds_type.BUTTON_BELL) && (!mainapp.prefDeviceSoundsBellIsMomentary)) {
            boolean rslt = !mainapp.soundsDeviceButtonStates[whichThrottle][buttonType];
            doDeviceButtonSound(whichThrottle, soundType);
            setSoundButtonState(bSoundsExtras[buttonType][whichThrottle], rslt);
            mainapp.soundsDeviceButtonStates[whichThrottle][buttonType] = rslt;
        } else {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    doDeviceButtonAction(whichThrottle, buttonType, soundType, true);
                    break;
                }
                // handle stopping of function on key-up
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    doDeviceButtonAction(whichThrottle, buttonType, soundType, false);
                    break;
            }
        }
    }

    void doDeviceButtonAction(int whichThrottle, int buttonType, int soundType, boolean state) {
        doDeviceButtonSound(whichThrottle, soundType);
        setSoundButtonState(bSoundsExtras[buttonType][whichThrottle], state);
        mainapp.soundsDeviceButtonStates[whichThrottle][buttonType] = state;
    }

    void soundsShowHideAllMuteButtons() {
        for (int i = 0; i < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES; i++) {
            soundsShowHideDeviceSoundsButton(i);
        }
    }

    void soundsShowHideDeviceSoundsButton(int whichThrottle) {
        if ((whichThrottle < mainapp.maxThrottlesCurrentScreen) && (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES)) {
            if (bMutes != null) {
                if (bMutes[whichThrottle] != null) {
                    int rslt = VISIBLE;
                    if (mainapp.prefDeviceSounds[whichThrottle].equals("none")) {
                        rslt = View.GONE;
                    }
                    bMutes[whichThrottle].setVisibility((mainapp.prefDeviceSoundsHideMuteButton) ? View.GONE : rslt);
                    bSoundsExtras[sounds_type.BUTTON_BELL][whichThrottle].setVisibility(rslt);
                    bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle].setVisibility(rslt);
                    bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle].setVisibility(rslt);
                    if (rslt == VISIBLE) {
                        setSoundButtonState(bMutes[whichThrottle], soundsIsMuted[whichThrottle]);
                        setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_BELL][whichThrottle], mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BUTTON_BELL]);
                        setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle], mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BUTTON_HORN]);
                        setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle], mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BUTTON_HORN_SHORT]);
                    }
                }
            }
        }
    }

    void soundsEnableDisableDeviceSoundsButton(int whichThrottle, boolean newEnabledState) {
        if ((whichThrottle < mainapp.maxThrottlesCurrentScreen) && (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES)) {
            if (bMutes != null) {
                if (bMutes[whichThrottle] != null) {
                    bMutes[whichThrottle].setEnabled(newEnabledState);
                    bSoundsExtras[sounds_type.BUTTON_BELL][whichThrottle].setEnabled(newEnabledState);
                    bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle].setEnabled(newEnabledState);
                    bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle].setEnabled(newEnabledState);
                    if (newEnabledState) {
                        setSoundButtonState(bMutes[whichThrottle], soundsIsMuted[whichThrottle]);
                    }
                }
            }
        }
    }

    void showHideSpeedLimitAndPauseButtons(int whichThrottle) {
        // show or hide the limit speed buttons
        if ((bLimitSpeeds != null) && (bLimitSpeeds[whichThrottle] != null)) {
            if (!prefLimitSpeedButton) {
                bLimitSpeeds[whichThrottle].setVisibility(View.GONE);
            } else {
                bLimitSpeeds[whichThrottle].setVisibility(VISIBLE);
            }
        }

        // show or hide the pause speed buttons
        if ((bPauseSpeeds != null) && (bPauseSpeeds[whichThrottle] != null)) {
            if (!prefPauseSpeedButton) {
                bPauseSpeeds[whichThrottle].setVisibility(View.GONE);
                if (bPauses[whichThrottle] != null) bPauses[whichThrottle].setVisibility(View.GONE);
            } else {
                if ((bPauses[whichThrottle] != null) && (prefPauseAlternateButton)) {
                    bPauses[whichThrottle].setVisibility(VISIBLE);
                    bPauseSpeeds[whichThrottle].setVisibility(View.GONE);
                } else {
                    bPauseSpeeds[whichThrottle].setVisibility(VISIBLE);
                }
            }
        }
    }

    protected void loadSounds() {
        Log.d(threaded_application.applicationName, activityName + ": loadSounds(): (ipls)" );
        if (ipls == null) ipls = new InPhoneLocoSounds(mainapp, prefs);
        if (iplsLoader == null) iplsLoader = new InPhoneLocoSoundsLoader(mainapp, prefs, getApplicationContext());
        mainapp.soundsReloadSounds = true;
        iplsLoader.loadSounds();
        iplsLoader = null;
    }

    // common startActivity()
    // used for swipes for the main activities only - Throttle, Turnouts, Routes, Web
    protected void startACoreActivity(Activity activity, Intent in, boolean swipe, float deltaX) {
        if (activity != null && in != null) {
            threaded_application.activityInTransition(activityName);
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ActivityOptions options;
            if (deltaX > 0) {
                options = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.push_right_in, R.anim.push_right_out);
            } else {
                options = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.push_left_in, R.anim.push_left_out);
            }
            startActivity(in, options.toBundle());
//            overridePendingTransition(mainapp.getFadeIn(swipe, deltaX), mainapp.getFadeOut(swipe, deltaX));
        }
    }

// *********************************************************************************************************************************
// *********************************************************************************************************************************

    // semi-realistic throttle functions
    // implemented in derived class, but called from this class
    void setTargetSpeed(int whichThrottle, int newSpeed) {}
    void setTargetSpeed(int whichThrottle, boolean fromSlider) {}
    boolean changeTargetDirectionIfAllowed(int whichThrottle, int direction) { return false; }
    public void incrementSemiRealisticThrottlePosition(int whichThrottle, int from) {}
    public void incrementSemiRealisticThrottlePosition(int whichThrottle, int from, int stepMultiplier) {}
    public void decrementSemiRealisticThrottlePosition(int whichThrottle, int from) {}
    public void decrementSemiRealisticThrottlePosition(int whichThrottle, int from, int stepMultiplier) {}
    protected void showTargetDirectionIndication(int whichThrottle) {}
    protected int getSpeedFromSemiRealisticThrottleCurrentSliderPosition(int whichThrottle) { return 0; }
    void semiRealisticThrottleSliderPositionUpdate(int whichThrottle, int newSpeed) {}
    void stopSemiRealisticThrottleSpeedButtonRepeater(int whichThrottle) {}
    void incrementBrakeSliderPosition(int whichThrottle) {}
    void decrementBrakeSliderPosition(int whichThrottle) {}
    void incrementLoadSliderPosition(int whichThrottle) {}
    void decrementLoadSliderPosition(int whichThrottle) {}
    protected int getTargetDirection(int whichThrottle) { return 0; }
    protected int getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(int whichThrottle) { return 0; }
    protected int getNewSemiRealisticThrottleSliderNotches() {return 100; }
    void setBrakeSliderPositionPcnt(int whichThrottle, float newPositionPcnt) {}

    private class SemiRealisticGamepadRptUpdater implements Runnable {
        int whichThrottle;
        int stepMultiplier;

        private SemiRealisticGamepadRptUpdater(int WhichThrottle, int StepMultiplier) {
            whichThrottle = WhichThrottle;
            stepMultiplier = StepMultiplier;

            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticGamepadRptUpdater(): WhichThrottle: " + whichThrottle
                    + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                    + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
            );

            try {
                REP_DELAY = Integer.parseInt(prefs.getString("prefGamePadSpeedArrowsThrottleRepeatDelay", getApplicationContext().getResources().getString(R.string.prefGamePadSpeedButtonsRepeatDefaultValue)));
            } catch (NumberFormatException ex) {
                REP_DELAY = 300;
            }
        }

        @Override
        public void run() {
            if (mainapp.appIsFinishing) {
                return;
            }

            if (mGamepadAutoIncrement) {
                incrementSemiRealisticThrottlePosition(whichThrottle, speed_commands_from_type.GAMEPAD, stepMultiplier);
                gamepadRepeatUpdateHandler.postDelayed(new SemiRealisticGamepadRptUpdater(whichThrottle, stepMultiplier), REP_DELAY);
            } else if (mGamepadAutoDecrement) {
                decrementSemiRealisticThrottlePosition(whichThrottle, speed_commands_from_type.GAMEPAD, stepMultiplier);
                gamepadRepeatUpdateHandler.postDelayed(new SemiRealisticGamepadRptUpdater(whichThrottle, stepMultiplier), REP_DELAY);
            }
        }
    }

    void adjustToolbarSize(Menu menu) {
        int newHeightAndWidth = mainapp.adjustToolbarSize(toolbar);

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