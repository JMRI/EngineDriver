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

import static android.view.InputDevice.getDevice;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_0;
import static android.view.KeyEvent.KEYCODE_9;
import static android.view.KeyEvent.KEYCODE_A;
import static android.view.KeyEvent.KEYCODE_B;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_D;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_EQUALS;
import static android.view.KeyEvent.KEYCODE_F;
import static android.view.KeyEvent.KEYCODE_F1;
import static android.view.KeyEvent.KEYCODE_F10;
import static android.view.KeyEvent.KEYCODE_H;
import static android.view.KeyEvent.KEYCODE_L;
import static android.view.KeyEvent.KEYCODE_LEFT_BRACKET;
import static android.view.KeyEvent.KEYCODE_M;
import static android.view.KeyEvent.KEYCODE_MEDIA_NEXT;
import static android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;
import static android.view.KeyEvent.KEYCODE_MINUS;
import static android.view.KeyEvent.KEYCODE_MOVE_END;
import static android.view.KeyEvent.KEYCODE_MOVE_HOME;
import static android.view.KeyEvent.KEYCODE_N;
import static android.view.KeyEvent.KEYCODE_NUMPAD_ADD;
import static android.view.KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
import static android.view.KeyEvent.KEYCODE_P;
import static android.view.KeyEvent.KEYCODE_PLUS;
import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_RIGHT_BRACKET;
import static android.view.KeyEvent.KEYCODE_S;
import static android.view.KeyEvent.KEYCODE_T;
import static android.view.KeyEvent.KEYCODE_V;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_MUTE;
import static android.view.KeyEvent.KEYCODE_HEADSETHOOK;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.KeyEvent.KEYCODE_W;
import static android.view.KeyEvent.KEYCODE_X;
import static android.view.KeyEvent.KEYCODE_Z;
import static android.view.KeyEvent.KEYCODE_PAGE_UP;
import static android.view.KeyEvent.KEYCODE_PAGE_DOWN;
import static android.view.KeyEvent.KEYCODE_APOSTROPHE;
import static android.view.KeyEvent.KEYCODE_SEMICOLON;
import static android.view.KeyEvent.KEYCODE_BACKSLASH;
import static android.view.KeyEvent.KEYCODE_PERIOD;
import static android.view.KeyEvent.KEYCODE_COMMA;
import static android.view.View.VISIBLE;
import static jmri.enginedriver.threaded_application.MAX_FUNCTIONS;
import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import eu.esu.mobilecontrol2.sdk.StopButtonFragment;
import eu.esu.mobilecontrol2.sdk.ThrottleFragment;
import eu.esu.mobilecontrol2.sdk.ThrottleScale;

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.auto_increment_or_decrement_type;
import jmri.enginedriver.type.consist_function_rule_style_type;
import jmri.enginedriver.type.kids_timer_action_type;
import jmri.enginedriver.type.light_follow_type;
import jmri.enginedriver.type.pref_gamepad_button_option_type;
import jmri.enginedriver.type.restart_reason_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.direction_button;
import jmri.enginedriver.type.function_button;

import jmri.enginedriver.type.screen_swipe_index_type;
import jmri.enginedriver.type.sounds_type;
import jmri.enginedriver.type.speed_button_type;
import jmri.enginedriver.type.throttle_screen_type;
import jmri.enginedriver.type.tts_msg_type;
import jmri.enginedriver.util.BackgroundImageLoader;
import jmri.enginedriver.util.HorizontalSeekBar;
import jmri.enginedriver.util.Tts;
import jmri.enginedriver.util.VerticalSeekBar;
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
import jmri.enginedriver.type.sub_activity_type;
import jmri.enginedriver.type.speed_step_type;
import jmri.enginedriver.type.acceleratorometer_action_type;
import jmri.enginedriver.type.direction_type;

public class throttle extends AppCompatActivity implements android.gesture.GestureOverlayView.OnGestureListener, PermissionsHelper.PermissionsHelperGrantedCallback {

    protected threaded_application mainapp; // hold pointer to mainapp
    protected SharedPreferences prefs;

    protected static final int MAX_SCREEN_THROTTLES = 3;

    protected static final int throttleMargin = 8; // forced margin between the horizontal throttles in dp
    protected int titleBar = 45; // estimate of lost screen height in dp

    // speed scale factors
    public static final int MAX_SPEED_VAL_WIT = 126;    // wit message maximum speed value, max speed slider value
    protected static int[] maxSpeedSteps = {100, 100, 100, 100, 100, 100};             // decoder max speed steps
    protected static int max_throttle_change;          // maximum allowable change of the sliders
    protected static int speedStepPref = 100;
    private static double[] displayUnitScales;            // display units per slider count

    private static final String VOLUME_INDICATOR = "v";
    private static final int[] GAMEPAD_INDICATOR = {1, 2, 3, 4, 5, 6};

    private String prefSelectedLocoIndicator = selected_loco_indicator_type.NONE;

    static public final int RESULT_GAMEPAD = RESULT_FIRST_USER;
    static public final int RESULT_ESUMCII = RESULT_GAMEPAD + 1;

    protected String keyboardString = "";
    protected int keyboardThrottle = -1;
//    protected boolean keyboardShift = false;

    protected SeekBar[] sbs; // seekbars

    protected ViewGroup[] functionButtonViewGroups; // function button tables

    protected Button[] bFwds; // buttons
    protected Button[] bStops;
    protected Button[] bPauses;
    protected Button[] bRevs;
    protected Button[] bSels;
    protected TextView[] tvbSelsLabels;
    protected Button[] bRSpds;
    protected Button[] bLSpds;

    protected TextView[] tvSpdLabs; // labels
    protected TextView[] tvSpdVals;

    protected TextView[] tvDirectionIndicatorForwards;
    protected TextView[] tvDirectionIndicatorReverses;

    protected TextView[] tvVols; // volume indicators

    protected TextView[] tvLeftDirInds; // direction indicators
    protected TextView[] tvRightDirInds;

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
    protected boolean isSemiRealisticTrottle = false;

    protected Button[] bTargetFwds;
    protected Button[] bTargetRevs;
    protected Button[] bTargetNeutrals;

    protected Button[] bTargetRSpds;
    protected Button[] bTargetLSpds;
    protected Button[] bTargetStops;

    // semi-realistic throttle variables
    protected static int[] targetSpeeds = {0, 0, 0, 0, 0, 0};
    protected static int[] prevTargetSpeeds = {0, 0, 0, 0, 0, 0};
    protected static int[] prevLoads = {0, 0, 0, 0, 0, 0};
    protected static int[] targetDirections = {1, 1, 1, 1, 1, 1};
    protected static double[] targetAccelerations = {0, 0, 0, 0, 0, 0};  /// -4=full brake  +1=normal acceleration  0=at target speed
    protected static String[] targetAccelerationsForDisplay = {"", "", "", "", "", ""};  /// -4=full brake  +1=normal acceleration  0=at target speed

    protected Handler[] semiRealisticTargetSpeedUpdateHandlers = {null, null, null, null, null, null};
    protected Handler[] semiRealisticSpeedButtonUpdateHandlers = {null, null, null, null, null, null};
    protected int[] mSemiRealisticAutoIncrementOrDecrement = {0, 0, 0, 0, 0, 0};  //off
    protected int[] mSemiRealisticSpeedButtonsAutoIncrementOrDecrement = {0, 0, 0, 0, 0, 0};  //off
    protected boolean semiRealisticSpeedButtonLongPressActive = false;
    protected ImageView[] airIndicators = {null, null, null, null, null, null};
    protected ImageView[] airLineIndicators = {null, null, null, null, null, null};
    protected int[] airValues = {100, 100, 100, 100, 100, 100};
    protected int[] airLineValues = {100, 100, 100, 100, 100, 100};
    protected boolean isAirRechaging = false;
    protected boolean isAirLineRechaging = false;
    protected int[] previousBrakePosition = {0, 0, 0, 0, 0, 0};
    protected Handler[] semiRealisticAirUpdateHandlers = {null, null, null, null, null, null};
    protected Handler[] semiRealisticAirLineUpdateHandlers = {null, null, null, null, null, null};

    // SPDHT for Speed Id and Direction Button Heights
    protected LinearLayout[] llLocoIdAndSpeedViewGroups;
    protected LinearLayout[] llLocoDirectionButtonViewGroups;

    // SPDHT
    protected View vThrotScr;
    protected View vThrotScrWrap;

    private boolean stealPromptActive = false; //true while steal dialog is open
    protected int whichVolume = 0;
    //    private int whichLastVolume = -1;
    private int whichLastGamepad1 = -1;
    private int whichLastGamepadButtonPressed = -1;
    private String prefGamePadDoublePressStop = "";
    private long gamePadDoublePressStopTime;


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

    protected InPhoneLocoSoundsLoader iplsLoader;

    // function number-to-button maps
    protected LinkedHashMap<Integer, Button>[] functionMaps;

    // current direction
    protected int[] dirs = {1, 1, 1, 1, 1, 1};   // requested direction for each throttle (single or multiple engines)
    protected String[] overrideThrottleNames = {"", "", "", "", "", ""};

    private String noUrl = "file:///android_asset/blank_page.html";
    private static final float initialScale = 1.5f;
    protected WebView webView = null;
    protected String webViewLocation = web_view_location_type.NONE;

    private static float scale = initialScale;      // used to restore throttle web zoom level (after rotation)
    private static boolean clearHistory = false;    // flags webViewClient to clear history when page load finishes
    private static String firstUrl = null;          // desired first url when clearing history
    private static String currentUrl = null;
    private Menu TMenu;
    static int REP_DELAY = 25;
    protected int prefSpeedButtonsSpeedStep = 4;
    protected int prefVolumeSpeedButtonsSpeedStep = 1;
    protected int prefGamePadSpeedButtonsSpeedStep = 4;
    protected boolean prefSpeedButtonsSpeedStepDecrement = false;

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
    private static final int changeDelay = 1000;            // msec
    protected PacingDelay[] changeTimers;

    // implement delay for pacing speed change requests so we don't overload the WiT socket
    private static final int maxSpeedMessageRate = 200;      // msec
    protected SpeedPacingDelay[] speedMessagePacingTimers;

    protected boolean selectLocoRendered = false; // this will be true once set_labels() runs following rendering of the loco select textViews

    // used in the gesture for entering and exiting immersive mode
    private boolean immersiveModeIsOn;

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

    private float externalGamepadxAxis;
    private float externalGamepadyAxis;
    private float externalGamepadxAxis2;
    private float externalGamepadyAxis2;

//    protected MediaPlayer _mediaPlayer;

    // Gamepad Button preferences
    private final String[] prefGamePadButtons = {"Next Throttle", "Stop", "Function 00/Light", "Function 01/Bell", "Function 02/Horn",
            "Increase Speed", "Reverse", "Decrease Speed", "Forward", "All Stop", "Select", "Left Shoulder", "Right Shoulder", "Left Trigger", "Right Trigger", "Left Thumb", "Right Thumb", "", "", "", "", "", ""};

    //                               0         1    2           3          4          5          6          7          8          9          10        11 12 13 14 15 16 17 18 19 20
    //                              none     NextThr  Speed+    Speed-     Fwd        Rev        All Stop   F2         F1         F0         Stop
    private final int[] gamePadKeys = {0, 0, KEYCODE_W, KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final int[] gamePadKeys_Up = {0, 0, KEYCODE_W, KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
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
    protected boolean pref_increase_slider_height_preference = false;
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

    // not static. can be changed inthe preferences
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

    // The following are used for the shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private jmri.enginedriver.util.ShakeDetector shakeDetector;
    private String prefAccelerometerShake = acceleratorometer_action_type.NONE;
    private boolean accelerometerCurrent = false;

    //    protected static final String THEME_DEFAULT = "Default";
    private boolean isRestarting = false;

    private static final String PREF_KIDS_TIMER_NONE = "0";
    private static final String PREF_KIDS_TIMER_ENDED = "999";
    private String prefKidsTimer = PREF_KIDS_TIMER_NONE;
    private String prefKidsTimerButtonDefault = "1";
    private int prefKidsTime = 0;  // in milliseconds
    private int kidsTimerRunning = 0;
    private MyCountDownTimer kidsTimer;
    private String prefKidsTimerResetPassword = "9999";
    private String prefKidsTimerRestartPassword = "0000";
    private boolean prefKidsTimerEnableReverse = false;
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
    private static final String ENGINE_DRIVER_DIR = "Android/data/jmri.enginedriver/files";
    private static final String SERVER_ENGINE_DRIVER_DIR = "prefs/engine_driver";

    protected LinearLayout screenNameLine;
    protected Toolbar toolbar;
    protected LinearLayout statusLine;
    private int toolbarHeight;

    private enum EsuMc2Led {
        RED(MobileControl2.LED_RED),
        GREEN(MobileControl2.LED_GREEN);

        private final int value;

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
        NO_ACTION("(no function)"),
        ALL_STOP("All Stop"),
        STOP("Stop"),
        DIRECTION_FORWARD("Forward"),
        DIRECTION_REVERSE("Reverse"),
        DIRECTION_TOGGLE("Forward/Reverse Toggle"),
        SPEED_INCREASE("Increase Speed"),
        SPEED_DECREASE("Decrease Speed"),
        NEXT_THROTTLE("Next Throttle"),
        FN00("Function 00/Light", 0),
        FN01("Function 01/Bell", 1),
        FN02("Function 02/Horn", 2),
        FN03("Function 03", 3),
        FN04("Function 04", 4),
        FN05("Function 05", 5),
        FN06("Function 06", 6),
        FN07("Function 07", 7),
        FN08("Function 08", 8),
        FN09("Function 09", 9),
        FN10("Function 10", 10),
        FN11("Function 11", 11),
        FN12("Function 12", 12),
        FN13("Function 13", 13),
        FN14("Function 14", 14),
        FN15("Function 15", 15),
        FN16("Function 16", 16),
        FN17("Function 17", 17),
        FN18("Function 18", 18),
        FN19("Function 19", 19),
        FN20("Function 20", 20),
        FN21("Function 21", 21),
        FN22("Function 22", 22),
        FN23("Function 23", 23),
        FN24("Function 24", 24),
        FN25("Function 25", 25),
        FN26("Function 26", 26),
        FN27("Function 27", 27),
        FN28("Function 28", 28),
        FN29("Function 29", 29),
        FN30("Function 30", 30),
        FN31("Function 31", 31);

        private final String action;
        private final int function;

        private static final Map<String, EsuMc2ButtonAction> ENUM_MAP;

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
            Map<String, EsuMc2ButtonAction> map = new ConcurrentHashMap<>();
            for (EsuMc2ButtonAction action : EsuMc2ButtonAction.values()) {
                map.put(action.getAction(), action);
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
                case OFF:
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
                    REP_DELAY = Integer.parseInt(prefs.getString("speed_arrows_throttle_repeat_delay", "100"));
                } catch (NumberFormatException ex) {
                    REP_DELAY = 100;
                }
            }
        }

        @Override
        public void run() {
//            Log.d("Engine_Driver", "RptUpdater: onProgressChanged - mAutoIncrement: " + mAutoIncrement + " mAutoDecrement: " + mAutoDecrement);
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
        switch (action) {
            case kids_timer_action_type.DISABLED:
                if (arg == 0) { // not onResume
//                    speedUpdateAndNotify(0);
                    if (kidsTimer != null) kidsTimer.cancel();
                    kidsTimerRunning = kids_timer_action_type.DISABLED;
                    for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                        enableDisableButtons(throttleIndex, false);
                        bSels[throttleIndex].setEnabled(true);
                        enableDisableButtons(throttleIndex, false);
                    }
                    mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                            getApplicationContext().getResources().getString(R.string.app_name),
                            getApplicationContext().getResources().getString(R.string.app_name_throttle),
                            "");
                    if (TMenu != null) {
                        mainapp.setKidsMenuOptions(TMenu, true, 0);
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
                    bSels[throttleIndex].setEnabled(false);
                    if (!prefKidsTimerEnableReverse) bRevs[throttleIndex].setEnabled(false);
                }
                mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                        getApplicationContext().getResources().getString(R.string.app_name_throttle_kids_enabled),
                        getApplicationContext().getResources().getString(R.string.prefKidsTimerTitle),
                        "");
                if (TMenu != null) {
                    mainapp.setKidsMenuOptions(TMenu, false, 0);
                }
//                }
                mainapp.hideSoftKeyboard(this.getCurrentFocus());
                break;

            case kids_timer_action_type.STARTED:
                if ((prefKidsTime > 0) && (kidsTimerRunning != kids_timer_action_type.RUNNING)) {
                    kidsTimerRunning = kids_timer_action_type.RUNNING;
                    kidsTimer = new MyCountDownTimer(prefKidsTime, 1000);
                    kidsTimer.start();
                }
                break;
            case kids_timer_action_type.ENDED:
                speedUpdateAndNotify(0);
                kidsTimerRunning = kids_timer_action_type.ENDED;
                if (kidsTimer != null) kidsTimer.cancel();
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    enableDisableButtons(throttleIndex, true);
                    bSels[throttleIndex].setEnabled(false);
                    if (!prefKidsTimerEnableReverse) bRevs[throttleIndex].setEnabled(false);
                }
                prefs.edit().putString("prefKidsTimer", PREF_KIDS_TIMER_ENDED).commit();  //reset the preference
                mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                        getApplicationContext().getResources().getString(R.string.app_name_throttle_kids_ended),
                        getApplicationContext().getResources().getString(R.string.prefKidsTimerTitle),
                        "");
                break;
            case kids_timer_action_type.RUNNING:
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    bSels[throttleIndex].setEnabled(false);
                    if (!prefKidsTimerEnableReverse) bRevs[throttleIndex].setEnabled(false);
                }
                mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                        getApplicationContext().getResources().getString(R.string.app_name_throttle_kids_running).replace("%1$s", Integer.toString(arg)),
                        getApplicationContext().getResources().getString(R.string.prefKidsTimerTitle),
                        "");
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

//            Log.d("Engine_Driver", "throttle handleMessage " + response_str );

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
                            if ((whichThrottle >= 0) && (whichThrottle < mainapp.numThrottles)) {
                                if (com2 == '+' || com2 == 'L') { // if loco added or function labels updated
                                    if (com2 == ('+')) {
                                        enableDisableButtons(whichThrottle); // direction and slider: pass whichthrottle
                                        showHideConsistMenus();
                                    }
                                    // loop through all function buttons and set label and dcc functions (based on settings) or hide if no label
                                    set_function_labels_and_listeners_for_view(whichThrottle);
                                    enableDisableButtonsForView(functionButtonViewGroups[whichThrottle], true);
                                    soundsShowHideDeviceSoundsButton(whichThrottle);
                                    showHideSpeedLimitAndPauseButtons(whichThrottle);
                                    set_labels();
                                } else if (com2 == '-') { // if loco removed
                                    removeLoco(whichThrottle);
                                    swapToNextAvilableThrottleForGamePad(whichThrottle, true); // see if we can/need to move the gamepad to another throttle
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
                                                Log.d("Engine_Driver", "throttle " + whichThrottle + " loco " + addr + " direction reported by WiT but engine is not assigned");
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
                            enableDisableButtons(com0); // pass whichthrottle
                            set_labels();
                            break;
                        case 'P': // panel info
                            if (com1 == 'W') { // PW - web server port info
                                initWeb();
                                set_labels();
                            } else if (com1 == 'P') { // PP - power state change
                                set_labels();
                            }
                            break;
                    } // end of switch

                    if (!selectLocoRendered) // call set_labels if the select loco textViews had not rendered the last time it was called
                        set_labels();
                }
                break;
                case message_type.REQUEST_REFRESH_THROTTLE:
//                    refreshMenu();
                    set_labels();
                    Log.d("Engine_Driver", "throttle: ThrottleMessageHandler: REQUEST_REFRESH_THROTTLE");
                    break;
                case message_type.REFRESH_FUNCTIONS:
                    setAllFunctionLabelsAndListeners();
                    for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                        setAllFunctionStates(throttleIndex);
                        enableDisableButtons(throttleIndex); // direction and slider: pass whichthrottle
                        showHideConsistMenus();
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
                case message_type.TIME_CHANGED:
                    setActivityTitle();
                    break;
                case message_type.FORCE_THROTTLE_RELOAD:
                    try {
                        int whichThrottle = Integer.parseInt(response_str);
//                        llThrottleLayouts[whichThrottle].invalidate();
//                        llThrottleLayouts[whichThrottle].requestLayout();
//                        set_labels();
                        removeLoco(whichThrottle);
                        setAllFunctionLabelsAndListeners();
                        setAllFunctionStates(whichThrottle);
                        enableDisableButtons(whichThrottle); // direction and slider: pass whichthrottle
                        showHideConsistMenus();
                    } catch (Exception e) {
                        // do nothing
                    }
                    break;
                case message_type.RESTART_APP:
                    startNewThrottleActivity();
                    break;
                case message_type.RELAUNCH_APP:
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
                    Log.d("Engine_Driver", "throttle: ThrottleMessageHandler: AUTO_IMPORT_URL_AVAILABLE " + response_str);
                    autoImportUrlAskToImport();
                    break;
                case message_type.SOUNDS_FORCE_LOCO_SOUNDS_TO_START:
                    for (int throttleIndex = 0; throttleIndex < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES; throttleIndex++) {
                        doLocoSound(throttleIndex);
                    }
                    break;
                case message_type.GAMEPAD_ACTION:
                    Log.d("Engine_Driver", "throttle: ThrottleMessageHandler: GAMEPAD_ACTION " + response_str);
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
                case message_type.VOLUME_BUTTON_ACTION: // volumem button n another activity
                    Log.d("Engine_Driver", "throttle handleMessage VOLUMN_BUTTON_ACTION " + response_str);
                    if (!response_str.isEmpty()) {
                        String[] splitString = response_str.split(":");
                        doVolumeButtonAction(Integer.parseInt(splitString[0]), Integer.parseInt(splitString[1]), Integer.parseInt(splitString[2]));
                    }
                    break;

                case message_type.GAMEPAD_JOYSTICK_ACTION:
                    Log.d("Engine_Driver", "throttle handleMessage GAMEPAD_JOYSTICK_ACTION " + response_str);
                    if (!response_str.isEmpty()) {
                        String[] splitString = response_str.split(":");
                        externalGamepadAction = Integer.parseInt(splitString[0]);
                        externalGamepadWhichGamePadIsEventFrom = Integer.parseInt(splitString[1]);
                        externalGamepadxAxis = Float.parseFloat(splitString[2]);
                        externalGamepadyAxis = Float.parseFloat(splitString[3]);
                        externalGamepadxAxis2 = Float.parseFloat(splitString[4]);
                        externalGamepadyAxis2 = Float.parseFloat(splitString[5]);

                        dispatchGenericMotionEvent(null);
                    }
                    break;

                case message_type.ESTOP: // only needed for the Semi-Reasistic Throttle
                    if (isSemiRealisticTrottle) {
                        int whichThrottle = Integer.parseInt(response_str);
                        semiRealisticThrottleSliderPositionUpdate(whichThrottle,0);
                        setTargetSpeed(whichThrottle, 0);
                    }
                    break;
            }
        }
    }

    private void startNewThrottleActivity() {
        webView.stopLoading();
        // remove old handlers since the new Intent will have its own
        isRestarting = true;        // tell OnDestroy to skip removing handlers since it will run after the new Intent is created
        removeHandlers();

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

            if (mainapp.androidVersion >= mainapp.minScreenDimNewMethodVersion) {

                setScreenBrightnessMode(SCREEN_BRIGHTNESS_MODE_MANUAL);

                if (!PermissionsHelper.getInstance().isPermissionGranted(throttle.this, PermissionsHelper.WRITE_SETTINGS)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PermissionsHelper.getInstance().requestNecessaryPermissions(throttle.this, PermissionsHelper.WRITE_SETTINGS);
                    }
                }
                try {
                    Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessValue);
                    Log.d("Engine_Driver", "screen brightness successfully changed to " + brightnessValue);
                } catch (Exception e) {
                    Log.e("Engine_Driver", "screen brightness was NOT changed to " + brightnessValue);
                }
            } else {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = ((float) brightnessValue) / 255;
                getWindow().setAttributes(lp);
            }
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

        if (mainapp.androidVersion >= mainapp.minScreenDimNewMethodVersion) {
            if (brightnessModeValue >= 0 && brightnessModeValue <= 1) {
                if (!PermissionsHelper.getInstance().isPermissionGranted(throttle.this, PermissionsHelper.WRITE_SETTINGS)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PermissionsHelper.getInstance().requestNecessaryPermissions(throttle.this, PermissionsHelper.WRITE_SETTINGS);
                    }
                }
                try {
                    Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, brightnessModeValue);
                } catch (Exception e) {
//                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastUnableToSetBrightness), Toast.LENGTH_SHORT).show();
                    threaded_application.safeToast(R.string.toastUnableToSetBrightness, Toast.LENGTH_SHORT);
                }
            }
        }
    }

    protected int getScreenBrightnessMode() {
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

    protected void setImmersiveModeOn(View webView, boolean forceOn) {
        immersiveModeIsOn = false;

        if ((prefThrottleViewImmersiveMode) || (forceOn)) {   // if the preference is set use Immersive mode
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                immersiveModeIsOn = true;
                if (webView!=null)
                    webView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                View windowView = getWindow().getDecorView();
                windowView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
            if (prefThrottleViewImmersiveModeHideToolbar) {
                screenNameLine.setVisibility(View.GONE);
                toolbar.setVisibility(View.GONE);
                statusLine.setVisibility(View.GONE);
            }
            webView.invalidate();
        }
    }

    protected void setImmersiveModeOff(View webView, boolean forceOff) {
        immersiveModeIsOn = false;

        if ((prefThrottleViewImmersiveMode) || (forceOff)) {   // if the preference is set use Immersive mode
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (webView!=null)
                    webView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_VISIBLE);
                View windowView = getWindow().getDecorView();
                windowView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_VISIBLE);
            }
            screenNameLine.setVisibility(VISIBLE);
            toolbar.setVisibility(VISIBLE);
            statusLine.setVisibility(VISIBLE);
            webView.invalidate();
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
//            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            threaded_application.safeToast(toastMsg, Toast.LENGTH_SHORT);
            screenBrightnessOriginal = getScreenBrightness();
            setScreenBrightness(screenBrightnessDim);
        }
    }

    // set or restore the screen brightness and lock or unlock the sceen when used for the Swipe Up or Shake
    private void setRestoreScreenLockDim(String toastMsg) {
        if (isScreenLocked) {
            isScreenLocked = false;
//            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastThrottleScreenUnlocked), Toast.LENGTH_SHORT).show();
            threaded_application.safeToast(R.string.toastThrottleScreenUnlocked, Toast.LENGTH_SHORT);
            setScreenBrightness(screenBrightnessOriginal);
            setScreenBrightnessMode(screenBrightnessModeOriginal);
            if (!prefThrottleViewImmersiveMode)
                setImmersiveModeOff(webView, true);
        } else {
            isScreenLocked = true;
//            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            threaded_application.safeToast(toastMsg, Toast.LENGTH_SHORT);
            screenBrightnessOriginal = getScreenBrightness();
            setScreenBrightness(screenBrightnessDim);
            if (!prefThrottleViewImmersiveMode)
                setImmersiveModeOn(webView, true);
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
                                if ((webViewLocation.equals(web_view_location_type.NONE)) && (keepWebViewLocation.equals(web_view_location_type.NONE))) {
                                    GamepadFeedbackSound(true);
//                                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastShakeWebViewUnavailable), Toast.LENGTH_SHORT).show();
                                    threaded_application.safeToast(R.string.toastShakeWebViewUnavailable, Toast.LENGTH_SHORT);
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
                                    Log.d("Engine_Driver", "ESU_MCII: Move knob request for EStop");
                                    setEsuThrottleKnobPosition(whichVolume, 0);
                                }
                        }
                    }
                });
                accelerometerCurrent = true;
            } else {
//                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastAccelerometerNotFound), Toast.LENGTH_LONG).show();
                threaded_application.safeToast(R.string.toastAccelerometerNotFound, Toast.LENGTH_LONG);
            }
        }
    }

    protected void addConsistFollowRule(String consistFollowString, String consistFollowAction, Integer consistFollowHeadlight) {
        if (consistFollowString.trim().length() > 0) {
            String[] prefConsistFollowStringstemp = threaded_application.splitByString(consistFollowString, ",");
            for (int i = 0; i < prefConsistFollowStringstemp.length; i++) {
                prefConsistFollowStrings.add(prefConsistFollowStringstemp[i].trim());
                prefConsistFollowActions.add(consistFollowAction);
                prefConsistFollowHeadlights.add(consistFollowHeadlight);
            }
        }
    }

    protected void getKidsTimerPrefs() {
        prefKidsTimer = prefs.getString("prefKidsTimer", getResources().getString(R.string.prefKidsTimerDefaultValue));
        if ((!prefKidsTimer.equals(PREF_KIDS_TIMER_NONE)) && (!prefKidsTimer.equals(PREF_KIDS_TIMER_ENDED))) {
            prefKidsTime = Integer.parseInt(prefKidsTimer) * 60000;
        } else {
            prefKidsTime = 0;
        }
        prefKidsTimerResetPassword = prefs.getString("prefKidsTimerResetPassword", getResources().getString(R.string.prefKidsTimerResetPasswordDefaultValue));
        prefKidsTimerRestartPassword = prefs.getString("prefKidsTimerRestartPassword", getResources().getString(R.string.prefKidsTimerRestartPasswordDefaultValue));
        prefKidsTimerEnableReverse = prefs.getBoolean("prefKidsTimerEnableReverse", getResources().getBoolean(R.bool.prefKidsTimerEnableReverseDefaultValue));
        prefKidsTimerButtonDefault = prefs.getString("prefKidsTimerButtonDefault", getResources().getString(R.string.prefKidsTimerButtonDefaultDefaultValue));
    }

    // get all the preferences that should be read when the activity is created or resumes
    @SuppressLint("ApplySharedPref")
    protected void getCommonPrefs(boolean isCreate) {

        if (isCreate) {  //only do onCreate
            webViewLocation = prefs.getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
        }
        isSemiRealisticTrottle = false;

        prefDirectionButtonLongPressDelay = threaded_application.getIntPrefValue(prefs, "prefDirectionButtonLongPressDelay", getApplicationContext().getResources().getString(R.string.prefDirectionButtonLongPressDelayDefaultValue));

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

        String prefConsistFollowStringtemp = prefs.getString("prefConsistFollowString1", getApplicationContext().getResources().getString(R.string.prefConsistFollowString1DefaultValue));
        String prefConsistFollowActiontemp = prefs.getString("prefConsistFollowAction1", getApplicationContext().getResources().getString(R.string.prefConsistFollowString1DefaultValue));
        addConsistFollowRule(prefConsistFollowStringtemp, prefConsistFollowActiontemp, consist_function_is_type.HEADLIGHT);

        prefConsistFollowStringtemp = prefs.getString("prefConsistFollowString2", getApplicationContext().getResources().getString(R.string.prefConsistFollowString2DefaultValue));
        prefConsistFollowActiontemp = prefs.getString("prefConsistFollowAction2", getApplicationContext().getResources().getString(R.string.prefConsistFollowString2DefaultValue));
        addConsistFollowRule(prefConsistFollowStringtemp, prefConsistFollowActiontemp, consist_function_is_type.NOT_HEADLIGHT);

        prefConsistFollowStringtemp = prefs.getString("prefConsistFollowString3", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        prefConsistFollowActiontemp = prefs.getString("prefConsistFollowAction3", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        addConsistFollowRule(prefConsistFollowStringtemp, prefConsistFollowActiontemp, consist_function_is_type.NOT_HEADLIGHT);

        prefConsistFollowStringtemp = prefs.getString("prefConsistFollowString4", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        prefConsistFollowActiontemp = prefs.getString("prefConsistFollowAction4", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        addConsistFollowRule(prefConsistFollowStringtemp, prefConsistFollowActiontemp, consist_function_is_type.NOT_HEADLIGHT);

        prefConsistFollowStringtemp = prefs.getString("prefConsistFollowString5", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        prefConsistFollowActiontemp = prefs.getString("prefConsistFollowAction5", getApplicationContext().getResources().getString(R.string.prefConsistFollowStringOtherDefaultValue));
        addConsistFollowRule(prefConsistFollowStringtemp, prefConsistFollowActiontemp, consist_function_is_type.NOT_HEADLIGHT);

        // increase height of throttle slider (if requested in preferences)
        pref_increase_slider_height_preference = prefs.getBoolean("increase_slider_height_preference", getResources().getBoolean(R.bool.prefIncreaseSliderHeightDefaultValue));

        // decrease height of Loco Id (if requested in preferences)
        prefDecreaseLocoNumberHeight = prefs.getBoolean("prefDecreaseLocoNumberHeight", getResources().getBoolean(R.bool.prefDecreaseLocoNumberHeightDefaultValue));

        // increase the web view height if the preference is set
        prefIncreaseWebViewSize = prefs.getBoolean("prefIncreaseWebViewSize", getResources().getBoolean(R.bool.prefIncreaseWebViewSizeDefaultValue));
        mainapp.defaultWebViewURL = prefs.getString("InitialWebPage",
                getApplicationContext().getResources().getString(R.string.prefInitialWebPageDefaultValue));
        mainapp.defaultThrottleWebViewURL = prefs.getString("InitialThrotWebPage",
                getApplicationContext().getResources().getString(R.string.prefInitialThrotWebPageDefaultValue));

        prefThrottleViewImmersiveMode = prefs.getBoolean("prefThrottleViewImmersiveMode",
                getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeDefaultValue));
        prefThrottleViewImmersiveModeHideToolbar = prefs.getBoolean("prefThrottleViewImmersiveModeHideToolbar",
                getResources().getBoolean(R.bool.prefThrottleViewImmersiveModeHideToolbarDefaultValue));

        prefShowAddressInsteadOfName = prefs.getBoolean("prefShowAddressInsteadOfName", getResources().getBoolean(R.bool.prefShowAddressInsteadOfNameDefaultValue));

        prefSwipeDownOption = prefs.getString("SwipeDownOption", getApplicationContext().getResources().getString(R.string.prefSwipeUpOptionDefaultValue));
        prefSwipeUpOption = prefs.getString("SwipeUpOption", getApplicationContext().getResources().getString(R.string.prefSwipeUpOptionDefaultValue));
        isScreenLocked = false;

        dirChangeWhileMoving = prefs.getBoolean("DirChangeWhileMovingPreference", getResources().getBoolean(R.bool.prefDirChangeWhileMovingDefaultValue));
        stopOnDirectionChange = prefs.getBoolean("prefStopOnDirectionChange", getResources().getBoolean(R.bool.prefStopOnDirectionChangeDefaultValue));
        locoSelectWhileMoving = prefs.getBoolean("SelLocoWhileMovingPreference", getResources().getBoolean(R.bool.prefSelLocoWhileMovingDefaultValue));

        screenBrightnessDim = Integer.parseInt(prefs.getString("prefScreenBrightnessDim", getResources().getString(R.string.prefScreenBrightnessDimDefaultValue))) * 255 / 100;

        prefConsistLightsLongClick = prefs.getBoolean("ConsistLightsLongClickPreference", getResources().getBoolean(R.bool.prefConsistLightsLongClickDefaultValue));

        prefGamepadTestEnforceTesting = prefs.getBoolean("prefGamepadTestEnforceTesting", getResources().getBoolean(R.bool.prefGamepadTestEnforceTestingDefaultValue));

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
        prefSpeedButtonsSpeedStep = threaded_application.getIntPrefValue(prefs, "speed_arrows_throttle_speed_step", "4");
        prefVolumeSpeedButtonsSpeedStep = threaded_application.getIntPrefValue(prefs, "prefVolumeSpeedButtonsSpeedStep", getApplicationContext().getResources().getString(R.string.prefVolumeSpeedButtonsSpeedStepDefaultValue));
        prefGamePadSpeedButtonsSpeedStep = threaded_application.getIntPrefValue(prefs, "prefGamePadSpeedButtonsSpeedStep", getApplicationContext().getResources().getString(R.string.prefVolumeSpeedButtonsSpeedStepDefaultValue));
        prefSpeedButtonsSpeedStepDecrement = prefs.getBoolean("prefSpeedButtonsSpeedStepDecrement", getResources().getBoolean(R.bool.prefSpeedButtonsSpeedStepDecrementDefaultValue));

        mainapp.prefGamepadOnlyOneGamepad = prefs.getBoolean("prefGamepadOnlyOneGamepad", getResources().getBoolean(R.bool.prefGamepadOnlyOneGamepadDefaultValue));

        prefGamePadDoublePressStop = prefs.getString("prefGamePadDoublePressStop", getResources().getString(R.string.prefGamePadDoublePressStopDefaultValue));
        mainapp.prefGamePadIgnoreJoystick = prefs.getBoolean("prefGamePadIgnoreJoystick", getResources().getBoolean(R.bool.prefGamePadIgnoreJoystickDefaultValue));

        prefEsuMc2EndStopDirectionChange = prefs.getBoolean("prefEsuMc2EndStopDirectionChange", getResources().getBoolean(R.bool.prefEsuMc2EndStopDirectionChangeDefaultValue));
        prefEsuMc2StopButtonShortPress = prefs.getBoolean("prefEsuMc2StopButtonShortPress", getResources().getBoolean(R.bool.prefEsuMc2StopButtonShortPressDefaultValue));

        mainapp.numThrottles = mainapp.Numeralise(prefs.getString("NumThrottle", getResources().getString(R.string.NumThrottleDefaultValue)));
        prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        prefSelectiveLeadSound = prefs.getBoolean("SelectiveLeadSound", getResources().getBoolean(R.bool.prefSelectiveLeadSoundDefaultValue));
        prefSelectiveLeadSoundF1 = prefs.getBoolean("SelectiveLeadSoundF1", getResources().getBoolean(R.bool.prefSelectiveLeadSoundF1DefaultValue));
        prefSelectiveLeadSoundF2 = prefs.getBoolean("SelectiveLeadSoundF2", getResources().getBoolean(R.bool.prefSelectiveLeadSoundF2DefaultValue));

//        prefBackgroundImage = prefs.getBoolean("prefBackgroundImage", getResources().getBoolean(R.bool.prefBackgroundImageDefaultValue));
//        prefBackgroundImageFileName = prefs.getString("prefBackgroundImageFileName", getResources().getString(R.string.prefBackgroundImageFileNameDefaultValue));
//        prefBackgroundImagePosition = prefs.getString("prefBackgroundImagePosition", getResources().getString(R.string.prefBackgroundImagePositionDefaultValue));

        prefHideSlider = prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue));

        prefLimitSpeedButton = prefs.getBoolean("prefLimitSpeedButton", getResources().getBoolean(R.bool.prefLimitSpeedButtonDefaultValue));
        prefLimitSpeedPercent = Integer.parseInt(prefs.getString("prefLimitSpeedPercent", getResources().getString(R.string.prefLimitSpeedPercentDefaultValue)));
        speedStepPref = threaded_application.getIntPrefValue(prefs, "DisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
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

        mainapp.prefActionBarShowDccExButton = prefs.getBoolean("prefActionBarShowDccExButton",
                getResources().getBoolean(R.bool.prefActionBarShowDccExButtonDefaultValue));
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
            bFwds[throttleIndex].setText(FullLeftText[throttleIndex]);
            bRevs[throttleIndex].setText(FullRightText[throttleIndex]);
            tvLeftDirInds[throttleIndex].setText(dirLeftIndicationText[throttleIndex]);
            tvRightDirInds[throttleIndex].setText(dirRightIndicationText[throttleIndex]);
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
                webViewLocation = keepWebViewLocation;
                webView.setVisibility(VISIBLE);
            } else {
                webViewLocation = web_view_location_type.NONE;
                webView.setVisibility(View.GONE);
                if (!toastMsg.isEmpty())
//                    Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
                    threaded_application.safeToast(toastMsg, Toast.LENGTH_SHORT);
            }
            webViewIsOn = !webViewIsOn;

            set_labels();
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
        disable_buttons(whichThrottle);         // direction and slider
        set_function_labels_and_listeners_for_view(whichThrottle);
        set_labels();
        doLocoSound(whichThrottle);
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
                Log.d("Engine_Driver", "ESU_MCII: Move knob request for WiT speed report");
                setEsuThrottleKnobPosition(whichThrottle, speedWiT);
            }
        }
        doLocoSound(whichThrottle);
    }

    SeekBar getThrottleSlider(int whichThrottle) {
        return sbs[whichThrottle];
    }

    double getDisplayUnitScale(int whichThrottle) {
        return displayUnitScales[whichThrottle];
    }

    // set speed slider to absolute value on all throttles
    protected void speedUpdate(int speed) {
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            speedUpdate(throttleIndex, speed);
        }
    }

    // set speed slider to absolute value on one throttle
    void speedUpdate(int whichThrottle, int speed) {
        if (speed < 0)
            speed = 0;
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
//        Log.d("Engine_Driver","throttle: speedChange -  change: " + change + " lastSpeed: " + lastSpeed+ " lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed:" + scaleSpeed);
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

//        Log.d("Engine_Driver","throttle: speedChange -  change: " + change + " speed: " + speed+ " scaleSpeed: " + scaleSpeed);

        throttle_slider.setProgress(speed);
        doLocoSound(whichThrottle);
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
        doLocoSound(whichThrottle);
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
                        , isSemiRealisticTrottle
                        , "");
                break;
        }
        doLocoSound(whichThrottle);
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
                        , isSemiRealisticTrottle
                        , "");
                break;
        }
        if ((vsbSwitchingSpeeds != null) || (hsbSwitchingSpeeds != null)) { // get around a problem with reporting for the switching layouts
            sbs[whichThrottle].setProgress(getSpeedFromCurrentSliderPosition(whichThrottle, false));
        }

        kidsTimerActions(kids_timer_action_type.STARTED, 0);
        doLocoSound(whichThrottle);

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
        doLocoSound(whichThrottle);
    }

    // change speed slider by scaled value and notify server
    public void speedChangeAndNotify(int whichThrottle, int change) {
//        SeekBar throttle_slider = getThrottleSlider(whichThrottle);
//        int lastSpeed = throttle_slider.getProgress();

        int speed = speedChange(whichThrottle, change);
        sendSpeedMsg(whichThrottle, speed);
        // Now update ESU MCII Knob position
        if (IS_ESU_MCII) {
            Log.d("Engine_Driver", "ESU_MCII: Move knob request for speed change");
            setEsuThrottleKnobPosition(whichThrottle, speed);
        }
        doLocoSound(whichThrottle);
    }

    // set the displayed numeric speed value
    @SuppressLint("SetTextI18n")
    protected void setDisplayedSpeed(int whichThrottle, int speed) {
        TextView speed_label;
        double speedScale = getDisplayUnitScale(whichThrottle);
        speed_label = tvSpdVals[whichThrottle];
        if (speed < 0) {
//            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastThrottleAlertEstop, getConsistAddressString(whichThrottle)), Toast.LENGTH_LONG).show();
            threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastThrottleAlertEstop, getConsistAddressString(whichThrottle)), Toast.LENGTH_LONG);
            speed = 0;
        }
        int scaleSpeed = (int) Math.round(speed * speedScale);
        try {
            int prevScaleSpeed = Integer.parseInt((String) speed_label.getText());
            speed_label.setText(Integer.toString(scaleSpeed));
            mainapp.throttleVibration(scaleSpeed, prevScaleSpeed);
        } catch (NumberFormatException | ClassCastException e) {
            Log.e("Engine_Driver", "problem showing speed: " + e.getMessage());
        }
    }

    // set the displayed numeric speed value
    @SuppressLint("SetTextI18n")
    protected void setDisplayedSpeedWithDirection(int whichThrottle, int speed) {
        TextView speed_label;
        double speedScale = getDisplayUnitScale(whichThrottle);
        speed_label = tvSpdVals[whichThrottle];
        if (speed < 0) {
//            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastThrottleAlertEstop, getConsistAddressString(whichThrottle)), Toast.LENGTH_LONG).show();
            threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastThrottleAlertEstop, getConsistAddressString(whichThrottle)), Toast.LENGTH_LONG);
            speed = 0;
        }
        int scaleSpeed = (int) Math.round(speed * speedScale);

        int dir = getDirection(whichThrottle);
        if (tvDirectionIndicatorForwards[whichThrottle] != null) {   //not all layouts have the indicators
            int showForword = View.GONE;
            int showReverse = View.GONE;

            if (speed > 0) {
                if (((!directionButtonsAreCurrentlyReversed(whichThrottle)) && (dir == direction_type.FORWARD))
                        || ((directionButtonsAreCurrentlyReversed(whichThrottle)) && (dir == direction_type.REVERSE))) {
                    showForword = VISIBLE;
                } else {
                    showReverse = VISIBLE;
                }
            }
            tvDirectionIndicatorForwards[whichThrottle].setVisibility(showForword);
            tvDirectionIndicatorReverses[whichThrottle].setVisibility(showReverse);
        }

        String sPrevScaleSpeed = (String) speed_label.getText();
        int prevScaleSpeed = Integer.parseInt(sPrevScaleSpeed);
        speed_label.setText(Integer.toString(scaleSpeed));
        mainapp.throttleVibration(scaleSpeed, prevScaleSpeed);
    }

    //adjust maxspeedsteps from code passed from JMRI, but only if set to Auto, else do not change
    private void setSpeedStepsFromWiT(int whichThrottle, int speedStepCode) {
        int maxSpeedStep = 100;
        switch (speedStepCode) {
            case speed_step_type.STEPS_128:
                maxSpeedStep = 126;
                break;
            case speed_step_type.STEPS_27:
                maxSpeedStep = 27;
                break;
            case speed_step_type.STEPS_14:
                maxSpeedStep = 14;
                break;
            case speed_step_type.STEPS_28:
                maxSpeedStep = 28;
                break;
        }

        int zeroTrim = threaded_application.getIntPrefValue(prefs, "prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
        ThrottleScale esuThrottleScale = new ThrottleScale(zeroTrim, maxSpeedStep + 1);

        maxSpeedSteps[whichThrottle] = maxSpeedStep;
        esuThrottleScales[whichThrottle] = esuThrottleScale;
        setDisplayUnitScale(whichThrottle);

        limitSpeedMax[whichThrottle] = Math.round(maxSpeedStep * ((float) prefLimitSpeedPercent) / 100);
    }

    //get max speedstep based on Preferences 
    //unless pref is set to AUTO in which case just return the input value
    private int getSpeedSteps(int maxStep) {
        if (speedStepPref != speed_step_type.STEPS_AUTO) {
            maxStep = speedStepPref;
        }
        return maxStep;
    }

    protected void setDisplayUnitScale(int whichThrottle) {
        int maxStep;
        maxStep = getSpeedSteps(maxSpeedSteps[whichThrottle]);
        displayUnitScales[whichThrottle] = calcDisplayUnitScale(maxStep);
        tvSpdLabs[whichThrottle].setText(calcDisplayUnitLabel(maxStep));

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
                        Log.d("Engine_Driver", "throttle " + mainapp.throttleIntToString(whichThrottle) + " direction change for unselected loco " + addr);
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
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
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
    boolean changeDirectionIfAllowed(int whichThrottle, int direction) {
        if ((getDirection(whichThrottle) != direction) && isChangeDirectionAllowed(whichThrottle)) {
            // set speed to zero on direction change while moving if the preference is set
            if (stopOnDirectionChange && (getSpeed(whichThrottle) != 0)) {
                speedUpdateAndNotify(whichThrottle, 0);
            }

            showDirectionRequest(whichThrottle, direction);        // update requested direction indication
            setEngineDirection(whichThrottle, direction, false);   // update direction for each engine on this throttle
            doLocoSound(whichThrottle);
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

    void start_select_loco_activity(int whichThrottle) {
        // give feedback that select button was pressed
        bSels[whichThrottle].setPressed(true);
        try {
            Intent select_loco = new Intent().setClass(this, select_loco.class);
            select_loco.putExtra("sWhichThrottle", mainapp.throttleIntToString(whichThrottle));  // pass whichThrottle as an extra to activity
            startActivityForResult(select_loco, sub_activity_type.SELECT_LOCO);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        } catch (Exception ex) {
            Log.d("Engine_Driver", "Throttle: start_select_loco_activity() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
        }
    }

    void start_gamepad_test_activity(int gamepadNo) {

        if (prefGamepadTestEnforceTesting) {
            mainapp.gamePadDeviceIdsTested[gamepadNo] = 0;
            try {
                Intent in = new Intent().setClass(this, gamepad_test.class);
                in.putExtra("whichGamepadNo", Integer.toString(gamepadNo));
                tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST);
                startActivityForResult(in, sub_activity_type.GAMEPAD_TEST);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d("Engine_Driver", "Throttle: start_gamepad_test_activity() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
            }
        } else { // don't bother doing the test if the preference is set not to
            mainapp.gamePadDeviceIdsTested[gamepadNo] = GAMEPAD_GOOD;
        }
    }

    // Edit the Consist Lights
    void start_consist_lights_edit(int whichThrottle) {
        if (prefConsistLightsLongClick) {  // only allow the editing in the consist lights if the preference is set
            try {
                Intent consistLightsEdit = new Intent().setClass(this, ConsistLightsEdit.class);
                consistLightsEdit.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle));
                startActivityForResult(consistLightsEdit, sub_activity_type.CONSIST_LIGHTS);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d("Engine_Driver", "Throttle: start_consist_lights_edit() failed. " + ((ex.getMessage() != null) ? ex.getMessage() : "") );
            }
        }
    }

    void disable_buttons(int whichThrottle) {
        enableDisableButtons(whichThrottle, true);
    }

    void enableDisableButtons(int whichThrottle) {
        enableDisableButtons(whichThrottle, false);
    }

    void applySpeedRelatedOptions() {
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            applySpeedRelatedOptions(throttleIndex);
        }  // repeat for all three throttles
    }

    void applySpeedRelatedOptions(int whichThrottle) {

        Button bFwd = bFwds[whichThrottle];
        Button bRev = bRevs[whichThrottle];
        Button bSel = bSels[whichThrottle];
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
        } else {
            bFwd.setEnabled(false);
            bRev.setEnabled(false);
            bSel.setEnabled(true);
        }
    }

    void enableDisableButtons(int whichThrottle, boolean forceDisable) {
        boolean newEnabledState = false;
        // avoid index and null crashes
        if (mainapp.consists == null || whichThrottle >= mainapp.consists.length
                || bFwds[whichThrottle] == null) {
            return;
        }
        if (!forceDisable) { // avoid index crash, but may simply push to next line
            newEnabledState = mainapp.consists[whichThrottle].isActive(); // set false if lead loco is not assigned
        }
        bFwds[whichThrottle].setEnabled(newEnabledState);
        bStops[whichThrottle].setEnabled(newEnabledState);
        if ((bPauses != null) && (bPauses[whichThrottle] != null)) {
            bPauses[whichThrottle].setEnabled(newEnabledState);
        }
        if ((kidsTimerRunning == kids_timer_action_type.RUNNING)
                && (!prefKidsTimerEnableReverse)) {
            bRevs[whichThrottle].setEnabled(false);
        } else {
            bRevs[whichThrottle].setEnabled(newEnabledState);
        }
        tvSpdLabs[whichThrottle].setEnabled(newEnabledState);
        tvSpdVals[whichThrottle].setEnabled(newEnabledState);
        bLSpds[whichThrottle].setEnabled(newEnabledState);
        bRSpds[whichThrottle].setEnabled(newEnabledState);
        enableDisableButtonsForView(functionButtonViewGroups[whichThrottle], newEnabledState);
        soundsShowHideDeviceSoundsButton(whichThrottle);
        showHideSpeedLimitAndPauseButtons(whichThrottle);
        if (!newEnabledState) {
            sbs[whichThrottle].setProgress(0); // set slider to 0 if disabled
            doLocoSound(whichThrottle);
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
        // Log.d("Engine_Driver","starting enableDisableButtonsForView " +

        // implemented in derived class, but called from this class

    }

    // update the appearance of all function buttons
    void setAllFunctionStates(int whichThrottle) {
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

    boolean canChangeVolumeIndicatorOnTouch(boolean isSpeedButtonOrSlider) {
        // implemented in derived class, but called from this class
        return false;
    }

    void adjustThrottleHeights() {
        // implemented in derived class, but called from this class
    }

    private void clearVolumeAndGamepadAdditionalIndicators() {
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            if (throttleIndex < bSels.length) {
                if ((prefSelectedLocoIndicator.equals(selected_loco_indicator_type.NONE))
                        || (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.GAMEPAD))) {
                    bSels[throttleIndex].setActivated(false);
                }
                if ((prefSelectedLocoIndicator.equals(selected_loco_indicator_type.NONE))
                        || (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.VOLUME))) {
                    bSels[throttleIndex].setHovered(false);
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void setSelectedLocoAdditionalIndicator(int whichThrottle, boolean isVolume) {

        if (!prefSelectedLocoIndicator.equals(selected_loco_indicator_type.NONE)) {

            if (mainapp.androidVersion >= mainapp.minActivatedButtonsVersion) {

                if ((isVolume) && (((prefSelectedLocoIndicator.equals(selected_loco_indicator_type.BOTH)) || (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.VOLUME))))) { // note: 'Volume' option is no longer available
                    if (!prefDisableVolumeKeys) { // don't set the indicators if the volume keys are disabled the preferences
                        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                            bSels[throttleIndex].setActivated(whichThrottle == throttleIndex);
                        }
                    }
                }
                if ((!isVolume) && (((prefSelectedLocoIndicator.equals(selected_loco_indicator_type.BOTH)) || (prefSelectedLocoIndicator.equals(selected_loco_indicator_type.GAMEPAD))))) {
                    for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
                        bSels[throttleIndex].setHovered(whichThrottle == throttleIndex);
                    }
                }
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

    private void setNextActiveThrottle() {
        setNextActiveThrottle(false);
    }

    private void setNextActiveThrottle(boolean feedbackSound) {
        int i;
        int index = -1;

        // find current Volume throttle
        for (i = 0; i < mainapp.numThrottles; i++) {
            if (i == whichVolume) {
                index = i;
                break;
            }
        }
        // find next active throttle
        i = index + 1;
        while (i != index) {                        // check until we get back to current Volume throttle
            if (i >= mainapp.numThrottles) {                // wrap
                i = 0;
            } else {
                int whichT = i;
                if (getConsist(whichT).isActive()) { // found next active throttle
                    whichVolume = whichT;
                    setVolumeIndicator();
                    mainapp.whichThrottleLastTouch = whichT;
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
    protected void setActiveThrottle(int whichThrottle) {
        setActiveThrottle(whichThrottle, false);
    }
    protected void setActiveThrottle(int whichThrottle, boolean isSpeedButtonOrSlider) {
        if (tvVols[0] != null) { // if it is null assume that the page is not fully drawn yet
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

    // setup the appropriate keycodes for the type of gamepad that has been selected in the preferences
    private void setGamepadKeys() {
        mainapp.prefGamePadType = prefs.getString("prefGamePadType", getApplicationContext().getResources().getString(R.string.prefGamePadTypeDefaultValue));
        Log.d("Engine_Driver", "setGamepadKeys() : prefGamePadType" + mainapp.prefGamePadType);

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

        //extra buttons
        prefGamePadButtons[10] = prefs.getString("prefGamePadButtonLeftShoulder", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftTriggerDefaultValue));
        prefGamePadButtons[11] = prefs.getString("prefGamePadButtonRightShoulder", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightShoulderDefaultValue));
        prefGamePadButtons[12] = prefs.getString("prefGamePadButtonLeftTrigger", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftTriggerDefaultValue));
        prefGamePadButtons[13] = prefs.getString("prefGamePadButtonRightTrigger", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightTriggerDefaultValue));
        prefGamePadButtons[14] = prefs.getString("prefGamePadButtonLeftThumb", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftThumbDefaultValue));
        prefGamePadButtons[15] = prefs.getString("prefGamePadButtonRightThumb", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightThumbDefaultValue));

        if ((!mainapp.prefGamePadType.equals(threaded_application.WHICH_GAMEPAD_MODE_NONE))
                && (webViewLocation.equals(web_view_location_type.NONE))) {
            // make sure the Soft keyboard is hidden
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

        int[] bGamePadKeys;
        int[] bGamePadKeysUp;

        switch (mainapp.prefGamePadType) {
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
            case "Game-alternate-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGameAlternate);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "MagicseeR1B":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMagicseeR1B);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "MagicseeR1A":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMagicseeR1A);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "MagicseeR1C":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMagicseeR1C);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "FlydigiWee2":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadFlydigiWee2);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "UtopiaC":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadUtopiaC);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "AuvisioB":
            case "AuvisioB-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadAuvisioB);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Generic":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGeneric);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Generic3x4":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGeneric3x4);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Keyboard":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadNoneLabels);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Volume":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadVolume);
                bGamePadKeysUp = bGamePadKeys;
                break;

            default: // "iCade" or iCade-rotate
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadiCade);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadiCade_UpCodes);
                break;
        }
        // now grab the keycodes and put them into the arrays that will actually be used.
        for (int i = 0; i < GAMEPAD_KEYS_LENGTH; i++) {
            gamePadKeys[i] = bGamePadKeys[i];
            gamePadKeys_Up[i] = bGamePadKeysUp[i];
        }

        // if the preference name has "-rotate" at the end of it swap the dpad buttons around
        if (mainapp.prefGamePadType.contains("-rotate")) {
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

    /** @noinspection UnusedReturnValue*/ //
    private int swapToNextAvilableThrottleForGamePad(int fromThrottle, boolean quiet) {
        int whichThrottle = -1;
        int index;
        index = fromThrottle - 1;

        // need to count through all the throttle, but need to start from the current one, not zero
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            index++;
            if (index >= mainapp.numThrottles) {
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
                for (int k = 0; k < mainapp.numThrottles; k++) {
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
            for (i = 0; i < mainapp.numThrottles; i++) {
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

                    mainapp.setGamepadTestMenuOption(TMenu, mainapp.gamepadCount);

                    start_gamepad_test_activity(mainapp.gamepadCount - 1);

                }

                for (i = 0; i < mainapp.numThrottles; i++) {
                    if (mainapp.gamePadIdsAssignedToThrottles[i] == 0) {  // throttle is not assigned a gamepad
                        if (getConsist(i).isActive()) { // found next active throttle
                            mainapp.gamePadIdsAssignedToThrottles[i] = eventDeviceId;
                            if (reassigningGamepad == -1) { // not a reassignment
                                mainapp.gamePadThrottleAssignment[i] = GAMEPAD_INDICATOR[whichGamePadDeviceId];
                            } else { // reasigning
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
                    start_gamepad_test_activity(whichGamePadDeviceId);
                }
            }
        }
        if (mainapp.gamepadCount > 0) {
            mainapp.usingMultiplePads = true;
        }

        return whichGamePad;
    }

    // map the button pressed to the user selected action for that button on the gamepad or ESP32 DIY Gamepad (which thinks it is a Keyboard)
    private void performButtonAction(int buttonNo, int action, boolean isActive, int whichThrottle, int whichGamePadIsEventFrom, int repeatCnt) {
        Log.d("Engine_Driver", "throttle: performButtonAction() buttonNo: " + buttonNo + " action: " + ((action == ACTION_DOWN) ? "ACTION_DOWN" : "ACTION_UP"));

        if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.ALL_STOP)) {  // All Stop
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                GamepadFeedbackSound(false);
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    if (isPauseSpeeds[throttleIndex] == pause_speed_type.TO_RETURN) {
                        disablePauseSpeed(throttleIndex);
                    }
                }
                if (!isSemiRealisticTrottle) {
                    speedUpdateAndNotify(0);
                } else { // semi-realistic throttle variant
                    // assumes only one Throttle
                    semiRealisticThrottleSliderPositionUpdate(whichThrottle, 0);
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.STOP)) {  // Stop
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                GamepadFeedbackSound(false);
                if ((isPauseSpeeds[whichThrottle] == pause_speed_type.TO_RETURN)
                        || ((getSpeed(whichThrottle) == 0) && (isPauseSpeeds[whichThrottle] == pause_speed_type.ZERO))) {
                    disablePauseSpeed(whichThrottle);
                }
                if (!isSemiRealisticTrottle) {
                    speedUpdateAndNotify(whichThrottle, 0);
                } else { // semi-realistic throttle variant
// ok
                    semiRealisticThrottleSliderPositionUpdate(whichThrottle, 0);
                }
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                        , getMaxSpeed(whichThrottle)
                        , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                        , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                        , "");

                if ((whichLastGamepadButtonPressed == buttonNo)
                        && (System.currentTimeMillis() <= (gamePadDoublePressStopTime + 1000))) {  // double press - within 1 second
                    if (prefGamePadDoublePressStop.equals(pref_gamepad_button_option_type.ALL_STOP)) {
                        if (!isSemiRealisticTrottle) {
                            speedUpdateAndNotify(0);         // update all throttles
                        } else { // semi-realistic throttle variant
// ok
                            // assumes only one Throttle
                            semiRealisticThrottleSliderPositionUpdate(whichThrottle, 0);
                        }
                    } else if (prefGamePadDoublePressStop.equals(pref_gamepad_button_option_type.FORWARD_REVERSE_TOGGLE)) {
                        boolean dirChangeFailed;
                        if (!isSemiRealisticTrottle) {
                            if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                                dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                            } else {
                                dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                            }
                            GamepadFeedbackSound(dirChangeFailed);
                        } else { // semi-realistic throttle variant
// -----------
// need to figure out neutral!!!
                            if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                                dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                            } else {
                                dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                            }
                            GamepadFeedbackSound(dirChangeFailed);
                        }
                    } // else do nothing
                    whichLastGamepadButtonPressed = -1;  // reset the count
                    gamePadDoublePressStopTime = 0; // reset the time
                } else {
                    gamePadDoublePressStopTime = System.currentTimeMillis();
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.NEXT_THROTTLE)) {  // Next Throttle
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (mainapp.usingMultiplePads && whichGamePadIsEventFrom >= 0) {
                    swapToNextAvilableThrottleForGamePad(whichGamePadIsEventFrom, false);
                } else {
                    setNextActiveThrottle(true);
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.FORWARD)) {  // Forward
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed;
                if (!isSemiRealisticTrottle) {
                    if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    } else {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    }
                } else {
// ok
                    if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    } else {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    }
                    showTargetDirectionIndication(whichThrottle);
                }
                GamepadFeedbackSound(dirChangeFailed);
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.REVERSE)) {  // Reverse
            boolean dirChangeFailed;
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (!isSemiRealisticTrottle) {
                    if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    } else {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                    GamepadFeedbackSound(dirChangeFailed);
                } else { // semi-realistic throttle variant
// ok
                    if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    } else {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                    showTargetDirectionIndication(whichThrottle);
                    GamepadFeedbackSound(dirChangeFailed);
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.FORWARD_REVERSE_TOGGLE)) {  // Toggle Forward/Reverse
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed;
                if (!isSemiRealisticTrottle) {
                    if ((getDirection(whichThrottle) == direction_type.FORWARD)) {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    } else {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                } else { // semi-realistic throttle variant
// -----------
                    if (getTargetDirection(whichThrottle) == direction_type.FORWARD) {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    } else {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                    showTargetDirectionIndication(whichThrottle);
                }
                GamepadFeedbackSound(dirChangeFailed);
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.INCREASE_SPEED)) {  // Increase Speed
            if (isActive && (action == ACTION_DOWN)) {
                if (repeatCnt == 0) {
                    mGamepadAutoIncrement = true;
                    if (!isSemiRealisticTrottle) {
                        gamepadRepeatUpdateHandler.post(new GamepadRptUpdater(whichThrottle, 1));
                    } else { // semi-realistic throttle
// ok
                        gamepadRepeatUpdateHandler.post(new SemiRealisticGamepadRptUpdater(whichThrottle, 1));
                    }
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.DECREASE_SPEED)) {  // Decrease Speed
            if (isActive && (action == ACTION_DOWN)) {
                if (repeatCnt == 0) {
                    mGamepadAutoDecrement = true;
                    if (!isSemiRealisticTrottle) {
                        gamepadRepeatUpdateHandler.post(new GamepadRptUpdater(whichThrottle, 1));
                    } else { // semi-realistic throttle variant
// ok
                        gamepadRepeatUpdateHandler.post(new SemiRealisticGamepadRptUpdater(whichThrottle, 1));
                    }
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SOUNDS_MUTE)) {  // IPLS Sounds - Mute
            if (isActive && (action == ACTION_UP)) {
                soundsIsMuted[whichThrottle] = !soundsIsMuted[whichThrottle];
                setSoundButtonState(bMutes[whichThrottle], soundsIsMuted[whichThrottle]);
                soundsMuteUnmuteCurrentSounds(whichThrottle);
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SOUNDS_BELL)) {  // IPLS Sounds - Bell
            if (isActive && (action == ACTION_UP)) {
                boolean rslt = !mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BELL - 1];
                doDeviceButtonSound(whichThrottle, sounds_type.BELL);
                setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_BELL][whichThrottle], rslt);
                mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BELL - 1] = rslt;
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SOUNDS_HORN)) {  // IPLS Sounds - Horn
            if (isActive) {
                if (action == ACTION_UP) {
                    doDeviceButtonSound(whichThrottle, sounds_type.HORN);
                    setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle], false);
                    mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1] = false;
                } else {
                    if (!mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1]) {
                        doDeviceButtonSound(whichThrottle, sounds_type.HORN);
                        setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle], true);
                        mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1] = true;
                    }
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SOUNDS_HORN_SHORT)) {  // IPLS Sounds - Horn Short
            if (isActive) {
                if (action == ACTION_UP) {
                    doDeviceButtonSound(whichThrottle, sounds_type.HORN_SHORT);
                    setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle], false);
                    mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1] = false;
                } else {
                    if (!mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1]) {
                        doDeviceButtonSound(whichThrottle, sounds_type.HORN_SHORT);
                        setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle], true);
                        mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1] = true;
                    }
                }
            }
        } else if ((prefGamePadButtons[buttonNo].length() >= 11) && (prefGamePadButtons[buttonNo].startsWith(GAMEPAD_FUNCTION_PREFIX))) { // one of the Function Buttons
            int fKey = Integer.parseInt(prefGamePadButtons[buttonNo].substring(9, 11));
            doGamepadFunction(fKey, action, isActive, whichThrottle, repeatCnt);
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.LIMIT_SPEED)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (!isSemiRealisticTrottle) {
                    if (!isLimitSpeeds[whichThrottle]) {
                        gamepadBeep(1);
                    } else {
                        gamepadBeep(10);
                    }
                    limitSpeed(whichThrottle);
                } else { // not avaliable on sem-realistic throttle
                    GamepadFeedbackSound(true);
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.PAUSE)) {
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (!isSemiRealisticTrottle) {
                    gamepadBeep(6);
                    if (isPauseSpeeds[whichThrottle] == pause_speed_type.INACTIVE) {
                        gamepadBeep(2);
                    } else {
                        gamepadBeep(20);
                    }
                    pauseSpeed(whichThrottle);
                } else { // not avaliable on sem-realistic throttle
                    GamepadFeedbackSound(true);
                }
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.SPEAK_CURRENT_SPEED)) {
// -----------
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE, whichThrottle, true
                        , whichLastGamepad1
                        , getDisplaySpeedFromCurrentSliderPosition(whichThrottle, true)
                        , 0
                        , getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                        , isSemiRealisticTrottle
                        , getConsistAddressString(whichThrottle));
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.NEUTRAL)) {
// ok
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.NEUTRAL);
                showTargetDirectionIndication(whichThrottle);
                GamepadFeedbackSound(dirChangeFailed);
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.INCREASE_BRAKE)) {
// ok
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                incrementBrakeSliderPosition(whichThrottle);
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.DECREASE_BRAKE)) {
// ok
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                decrementBrakeSliderPosition(whichThrottle);
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.INCREASE_LOAD)) {
// ok
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                incrementLoadSliderPosition(whichThrottle);
            }
        } else if (prefGamePadButtons[buttonNo].equals(pref_gamepad_button_option_type.DECREASE_LOAD)) {
// ok
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                decrementLoadSliderPosition(whichThrottle);
            }
        }

        whichLastGamepadButtonPressed = buttonNo;
    }

    // map the button pressed to the user selected action for that button on a keyboard
    private void performKeyboardKeyAction(int keyCode, int action, boolean isShiftPressed, int repeatCnt, int originalWhichThrottle, int whichGamePadIsEventFrom) {
        Log.d("Engine_Driver", "throttle: performKeyboardKeyAction() action: " + action);
        int whichThrottle = originalWhichThrottle;
        boolean isActive = getConsist(originalWhichThrottle).isActive();
        if ((keyboardThrottle >= 0) && (keyboardThrottle != originalWhichThrottle)) {
            whichThrottle = keyboardThrottle;
            isActive = getConsist(whichThrottle).isActive();
        }

        if ((keyCode == KEYCODE_Z) || (keyCode == KEYCODE_MOVE_END)) {  // All Stop
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                GamepadFeedbackSound(false);
                if (!isSemiRealisticTrottle) {
                    speedUpdateAndNotify(0);         // update all three throttles
                } else {
// ok
                    // assumes only one Throttle
                    semiRealisticThrottleSliderPositionUpdate(whichThrottle, 0);
                }
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_X) || (keyCode == KEYCODE_MOVE_HOME)) {  // Stop
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                GamepadFeedbackSound(false);
                if (!isSemiRealisticTrottle) {
                    speedUpdateAndNotify(whichThrottle, 0);
                    tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                            , getMaxSpeed(whichThrottle)
                            , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                            , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                            , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                            , isSemiRealisticTrottle
                            , "");
                } else {
// ok
                    // assumes only one Throttle
                    semiRealisticThrottleSliderPositionUpdate(whichThrottle, 0);
                }
                resetKeyboardString();
            }
        } else if (keyCode == KEYCODE_N) {  // Next Throttle
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (mainapp.usingMultiplePads && whichGamePadIsEventFrom >= 0) {
                    swapToNextAvilableThrottleForGamePad(whichGamePadIsEventFrom, false);
                } else {
                    setNextActiveThrottle(true);
                }
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_DPAD_RIGHT) || (keyCode == KEYCODE_RIGHT_BRACKET)) {  // Forward
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed;
                if (!isSemiRealisticTrottle) {
                    if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    } else {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    }
                } else {
// ok
                    if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    } else {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    }
                    showTargetDirectionIndication(whichThrottle);
                }
                GamepadFeedbackSound(dirChangeFailed);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_DPAD_LEFT) || (keyCode == KEYCODE_LEFT_BRACKET)) {  // Reverse
            boolean dirChangeFailed;
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (!isSemiRealisticTrottle) {
                    if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    } else {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                } else {
// ok
                    if (!gamepadDirectionButtonsAreCurrentlyReversed(whichThrottle)) {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    } else {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                    showTargetDirectionIndication(whichThrottle);
                }
                GamepadFeedbackSound(dirChangeFailed);
                resetKeyboardString();
            }
        } else if (keyCode == KEYCODE_D) {  // Toggle Forward/Reverse
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                boolean dirChangeFailed;
                if (!isSemiRealisticTrottle) {
                    if ((getDirection(whichThrottle) == direction_type.FORWARD)) {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    } else {
                        dirChangeFailed = !changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                } else {
// ok
                    if (getTargetDirection(whichThrottle) == direction_type.FORWARD) {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    } else {
                        dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                    showTargetDirectionIndication(whichThrottle);
                }
                GamepadFeedbackSound(dirChangeFailed);
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_DPAD_UP) || (keyCode == KEYCODE_PLUS)
                || (keyCode == KEYCODE_EQUALS) || (keyCode == KEYCODE_NUMPAD_ADD)
                || (keyCode == KEYCODE_VOLUME_UP)) {  // Increase Speed
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                mGamepadAutoIncrement = true;
                if (!isSemiRealisticTrottle) {
                    gamepadRepeatUpdateHandler.post(new GamepadRptUpdater(whichThrottle, 1));
                } else { // semi-realistic throttle variant
// ok
                    gamepadRepeatUpdateHandler.post(new SemiRealisticGamepadRptUpdater(whichThrottle, 1));
                }
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_DPAD_DOWN) || (keyCode == KEYCODE_MINUS)
                || (keyCode == KEYCODE_NUMPAD_SUBTRACT)
                || (keyCode == KEYCODE_VOLUME_DOWN)) {  // Decrease Speed
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                mGamepadAutoDecrement = true;
                if (!isSemiRealisticTrottle) {
                    gamepadRepeatUpdateHandler.post(new GamepadRptUpdater(whichThrottle, 1));
                } else { // semi-realistic throttle variant
// ok
                    gamepadRepeatUpdateHandler.post(new SemiRealisticGamepadRptUpdater(whichThrottle, 1));
                }
                resetKeyboardString();
            }
        } else if ((isSemiRealisticTrottle) && (keyCode == KEYCODE_BACKSLASH)) { // semi-realistic throttle - neutral
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
// ok
                boolean dirChangeFailed = !changeTargetDirectionIfAllowed(whichThrottle, direction_type.NEUTRAL);
                showTargetDirectionIndication(whichThrottle);
                GamepadFeedbackSound(dirChangeFailed);
            }
        } else if ((isSemiRealisticTrottle) && ((keyCode == KEYCODE_PAGE_UP) || (keyCode == KEYCODE_APOSTROPHE)) ) { // semi-realistic throttle - increase brake
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
// ok
                incrementBrakeSliderPosition(whichThrottle);
            }
        } else if ((isSemiRealisticTrottle) && ((keyCode == KEYCODE_PAGE_DOWN) || (keyCode == KEYCODE_SEMICOLON)) ) { // semi-realistic throttle - decrease brake
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
// ok
                decrementBrakeSliderPosition(whichThrottle);
            }
        } else if ((isSemiRealisticTrottle) && (keyCode == KEYCODE_COMMA) ) { // semi-realistic throttle - decrease load
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
// ok
                                decrementLoadSliderPosition(whichThrottle);
            }
        } else if ((isSemiRealisticTrottle) && (keyCode == KEYCODE_PERIOD)) { // semi-realistic throttle - increase load
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
// ok
                incrementLoadSliderPosition(whichThrottle);
            }
        } else if ((keyCode == KEYCODE_MEDIA_NEXT)) {  // Increase Speed * 2
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                mGamepadAutoIncrement = true;
                if (!isSemiRealisticTrottle) {
                    gamepadRepeatUpdateHandler.post(new GamepadRptUpdater(whichThrottle, 2));
                } else {
                    gamepadRepeatUpdateHandler.post(new SemiRealisticGamepadRptUpdater(whichThrottle, 2));
                }
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_MEDIA_PREVIOUS)) {  // Decrease Speed * 2
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                mGamepadAutoDecrement = true;
                if (!isSemiRealisticTrottle) {
                    gamepadRepeatUpdateHandler.post(new GamepadRptUpdater(whichThrottle, 2));
                } else { // semi-realistic throttle variant
// ok
                    gamepadRepeatUpdateHandler.post(new SemiRealisticGamepadRptUpdater(whichThrottle, 2));
                }
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_VOLUME_MUTE) || (keyCode == KEYCODE_M)) {  // IPLS Sounds - Mute
            if (isActive && (action == ACTION_UP) && (repeatCnt == 0)) {
                soundsIsMuted[whichThrottle] = !soundsIsMuted[whichThrottle];
                setSoundButtonState(bMutes[whichThrottle], soundsIsMuted[whichThrottle]);
                soundsMuteUnmuteCurrentSounds(whichThrottle);
                resetKeyboardString();
            }
        } else if (keyCode == KEYCODE_B) {  // IPLS Sounds - Bell
            if (isActive && (action == ACTION_UP) && (repeatCnt == 0)) {
                boolean rslt = !mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BELL - 1];
                doDeviceButtonSound(whichThrottle, sounds_type.BELL);
                setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_BELL][whichThrottle], rslt);
                mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.BELL - 1] = rslt;
                resetKeyboardString();
            }
        } else if ((keyCode == KEYCODE_H) && (!isShiftPressed)) {  // IPLS Sounds - Horn
            if (isActive) {
                if (action == ACTION_UP) {
                    doDeviceButtonSound(whichThrottle, sounds_type.HORN);
                    setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle], false);
                    mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1] = false;
                    resetKeyboardString();
                } else {
                    if (repeatCnt == 0) {
                        if (!mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1]) {
                            doDeviceButtonSound(whichThrottle, sounds_type.HORN);
                            setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN][whichThrottle], true);
                            mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN - 1] = true;
                        }
                    }
                }
            }
        } else if ((keyCode == KEYCODE_H) && (isShiftPressed)) {  // IPLS Sounds - Horn Short
            if (isActive) {
                if (action == ACTION_UP) {
                    doDeviceButtonSound(whichThrottle, sounds_type.HORN_SHORT);
                    setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle], false);
                    mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1] = false;
                    resetKeyboardString();
                } else {
                    if (repeatCnt == 0) {
                        if (!mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1]) {
                            doDeviceButtonSound(whichThrottle, sounds_type.HORN_SHORT);
                            setSoundButtonState(bSoundsExtras[sounds_type.BUTTON_HORN_SHORT][whichThrottle], true);
                            mainapp.soundsDeviceButtonStates[whichThrottle][sounds_type.HORN_SHORT - 1] = true;
                        }
                    }
                }
            }
        } else if (keyCode == KEYCODE_L) { // Limit Speed toggle
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (!isSemiRealisticTrottle) {
                    if (!isLimitSpeeds[whichThrottle]) {
                        gamepadBeep(1);
                    } else {
                        gamepadBeep(10);
                    }
                    limitSpeed(whichThrottle);
                } else {
                    GamepadFeedbackSound(true);
                } // not avaliable on sem-realistic throttle
                resetKeyboardString();
            }
        } else if (keyCode == KEYCODE_P) { // pause speed toggle
            if (isActive && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                if (!isSemiRealisticTrottle) {
                    gamepadBeep(6);
                    if (isPauseSpeeds[whichThrottle] == pause_speed_type.INACTIVE) {
                        gamepadBeep(2);
                    } else {
                        gamepadBeep(20);
                    }
                    pauseSpeed(whichThrottle);
                } else { // not avaliable on sem-realistic throttle
                    GamepadFeedbackSound(true);
                }
                resetKeyboardString();
            }

        } else if (keyCode == KEYCODE_V) {  // Speak speed
            if (action == ACTION_DOWN) {
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE, whichThrottle, true
                        , whichLastGamepad1
                        , getDisplaySpeedFromCurrentSliderPosition(whichThrottle, true)
                        , 0
                        , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                        , isSemiRealisticTrottle
                        , getConsistAddressString(whichThrottle));
            }

        } else if (keyCode == KEYCODE_F) {  // Start of a Function command
            if (action == ACTION_DOWN) {
                keyboardString = "F";
            }
        } else if (keyCode == KEYCODE_S) {  // Start of a speed command
            if (action == ACTION_DOWN) {
                keyboardString = "S";
            }
        } else if (keyCode == KEYCODE_T) {  // Start of a throttle specification
            if (action == ACTION_DOWN) {
                keyboardString = "T";
                keyboardThrottle = -1;  //reset it
            }
        } else if (((keyCode >= KEYCODE_0) && (keyCode <= KEYCODE_9))
                || ((keyCode >= KEYCODE_F1) && (keyCode <= KEYCODE_F10))) {  // Start of a Function, Speed, or Throttle command
            String num;
            if ((keyCode >= KEYCODE_0) && (keyCode <= KEYCODE_9)) {
                num = Integer.toString(keyCode - KEYCODE_0);
            } else {
                if (keyCode != KEYCODE_F10) {
                    num = Integer.toString(keyCode - KEYCODE_F1 + 1);
                } else {
                    num = "0";
                }
            }

            if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'F')) {  // Function
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                }
                if (keyboardString.length() == 3) {  // have a two digit function number now
                    int fKey = Integer.parseInt(keyboardString.substring(1, 3));
                    if(fKey<MAX_FUNCTIONS) {
                        if (action == ACTION_DOWN) {
                            doGamepadFunction(fKey, action, isActive, whichThrottle, repeatCnt);
                        } else {
                            doGamepadFunction(fKey, action, isActive, whichThrottle, repeatCnt);
                            resetKeyboardString();
                        }
                    }
                }
            } else if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'S')) {  // speed
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                    if (keyboardString.length() == 4) {  // have a three digit speed amount now
                        int newSpeed = Math.min(Integer.parseInt(keyboardString.substring(1, 4)), 100);
                        float vSpeed = 126 * ((float) newSpeed) / 100;
                        newSpeed = (int) vSpeed;
                        if (!isSemiRealisticTrottle) {
                            speedUpdateAndNotify(whichThrottle, newSpeed);
                        } else { // semi-realistic throttle variant
// ok
                            semiRealisticThrottleSliderPositionUpdate(whichThrottle, newSpeed);
                        }
                    }
                }
                if (action == ACTION_UP) {
                    if (keyboardString.length() == 4) {  // have a three digit speed amount now
                        resetKeyboardString();
                    }
                }
            } else if ((!keyboardString.isEmpty()) && (keyboardString.charAt(0) == 'T')) {    // specify Throttle number
                if ((action == ACTION_DOWN) && (repeatCnt == 0)) {
                    keyboardString = keyboardString + num;
                }
                if (action == ACTION_DOWN) {
                    if (keyboardString.length() == 2) {  // have a complete Throttle number
                        keyboardThrottle = Integer.parseInt(keyboardString.substring(1, 2));
                        if (keyboardThrottle > MAX_SCREEN_THROTTLES) {
                            keyboardThrottle = -1;
                        }
                    }
                } else {
                    keyboardString = "";
                }
            } else if (keyboardString.isEmpty()) {  // direct function 0-9
                int fKey;
                if ((keyCode >= KEYCODE_0) && (keyCode <= KEYCODE_9)) {
                    fKey = keyCode - KEYCODE_0;
                } else {
                    if (keyCode != KEYCODE_F10) {
                        fKey = keyCode - KEYCODE_F1 + 1;
                    } else {
                        fKey = 0;
                    }
                }
                if (fKey < 10) { // special case for attached keyboards keys 0-9
                    mainapp.numericKeyIsPressed[fKey] = action;
                    if (action == ACTION_DOWN) {
                        mainapp.numericKeyFunctionStateAtTimePressed[fKey] = ((mainapp.function_states[whichThrottle][fKey]) ? 1 : 0);
                    }
                }
                doGamepadFunction(fKey, action, isActive, whichThrottle, repeatCnt);
                resetKeyboardString();
            }
        }
    }

    void resetKeyboardString() {
        Log.d("Engine_Driver", "throttle: resetKeyboardString()");
        keyboardString = "";
        keyboardThrottle = -1;  //reset it
    }

    void doGamepadFunction(int fKey, int action, boolean isActive, int whichThrottle, int repeatCnt) {
        Log.d("Engine_Driver", "throttle: doGamepadFunction() : fkey: " + fKey + " action: " + action + " isActive: " + isActive);
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
            if (action == ACTION_DOWN) {
                GamepadFeedbackSound(false);
//                sendFunctionToConsistLocos(whichThrottle, fKey, lab, button_press_message_type.DOWN, leadOnly, trailOnly, followLeadFunction, consist_function_latching_type.NA);
                sendFunctionToConsistLocos(whichThrottle, fKey, lab, button_press_message_type.DOWN, leadOnly, trailOnly, followLeadFunction, isLatching);
            } else {
//                sendFunctionToConsistLocos(whichThrottle, fKey, lab, button_press_message_type.UP, leadOnly, trailOnly, followLeadFunction, consist_function_latching_type.NA);
                sendFunctionToConsistLocos(whichThrottle, fKey, lab, button_press_message_type.UP, leadOnly, trailOnly, followLeadFunction, isLatching);
            }
        }

    }

    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        Log.d("Engine_Driver", "dispatchGenericMotionEvent() Joystick Event");
        if ( (!mainapp.prefGamePadType.equals(threaded_application.WHICH_GAMEPAD_MODE_NONE)) && (!mainapp.prefGamePadIgnoreJoystick) ) {

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
                    xAxis = event.getAxisValue(MotionEvent.AXIS_X);
                    yAxis = event.getAxisValue(MotionEvent.AXIS_Y);
                    xAxis2 = event.getAxisValue(MotionEvent.AXIS_Z);
                    yAxis2 = event.getAxisValue(MotionEvent.AXIS_RZ);

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
                    xAxis = externalGamepadxAxis;
                    yAxis = externalGamepadyAxis;
                    xAxis2 = externalGamepadxAxis2;
                    yAxis2 = externalGamepadyAxis2;
                }

                Log.d("Engine_Driver", "dispatchGenericMotionEvent() Joystick Event x: " + xAxis + "y: " + yAxis + "x2: " + xAxis2 + "y2: " + yAxis2 );

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
                    Log.d("Engine_Driver", "dispatchGenericMotionEvent: ACTION_UP"
                            + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                            + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
                    );
                }

                boolean rslt = false;
                if (yAxis == -1) { // DPAD Up Button
                    performButtonAction(5, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    rslt = true;

                } else if (yAxis == 1) { // DPAD Down Button
                    performButtonAction(7, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    rslt = true;

                } else if (xAxis == -1) { // DPAD Left Button
                    performButtonAction(8, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    rslt = true;

                } else if (xAxis == 1) { // DPAD Right Button
                    performButtonAction(6, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    rslt = true;
                }

                if (yAxis2 == -1) { // DPAD2 Up Button
                    performButtonAction(5, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    rslt = true;

                } else if (yAxis2 == 1) { // DPAD2 Down Button
                    performButtonAction(7, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    rslt = true;

                } else if (xAxis2 == -1) { // DPAD2 Left Button
                    performButtonAction(8, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    rslt = true;

                } else if (xAxis2 == 1) { // DPAD2 Right Button
                    performButtonAction(6, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                    rslt = true;
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

        boolean isExternal = false;
        if (event != null) {
            InputDevice idev = getDevice(event.getDeviceId());
            if (idev != null && idev.toString().contains("Location: external"))
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
                        whichThrottle = whichVolume;  // work out which throttle the volume keys are currently set to contol... and use that one
                        mainapp.whichThrottleLastTouch = whichThrottle;
                    }

                    boolean isActive = getConsist(whichThrottle).isActive();

                    if (keyCode != 0) {
                        Log.d("Engine_Driver", "throttle: dispatchKeyEvent(): keycode " + keyCode + " action " + action + " repeat " + repeatCnt);
                    }

                    if (!mainapp.prefGamePadType.equals("Keyboard")) {
                        if (action == ACTION_UP) {
                            mGamepadAutoIncrement = false;
                            mGamepadAutoDecrement = false;
                            GamepadFeedbackSoundStop();
                            Log.d("Engine_Driver", "throttle: dispatchKeyEvent (not Keyboard): ACTION_UP"
                                    + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                                    + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
                            );
                        } else {
                            Log.d("Engine_Driver", "throttle: dispatchKeyEvent (not Keyboard): ACTION_DOWN");
                        }

                        // if the preference name has "-rotate" at the end of it swap the direction keys around
//                        if (mainapp.prefGamePadType.contains("-rotate")) {
//                            if ((keyCode >= 19) && (keyCode <= 22)) {
//                                if (keyCode == 19) {
//                                    keyCode = 22;
//                                }  // was Up -> Right
//                                else if (keyCode == 20) {
//                                    keyCode = 21;
//                                } // was Down -> Left
//                                else if (keyCode == 21) {
//                                    keyCode = 20;
//                                } // was Left -> Down
////                                else if (keyCode==22) { keyCode=19; } // was Right -> Up
//                                else {
//                                    keyCode = 19;
//                                } // was Right -> Up
//                            }
//                        }

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
                            performButtonAction(actionNo, action, isActive, whichThrottle, whichGamePadIsEventFrom, repeatCnt);
                            return (true); // stop processing this key
                        } else if ((rslt >= 17) && (rslt <= 21)) {
                            performKeyboardKeyAction(keyCode, action, false, repeatCnt, whichThrottle, whichGamePadIsEventFrom);
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
                        Log.d("Engine_Driver", "throttle: dispatchKeyEvent: ACTION" + ((action == ACTION_UP) ? "UP" : "DOWN")
                                + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                                + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
                        );
                        performKeyboardKeyAction(keyCode, action, isShiftPressed, repeatCnt, whichThrottle, whichGamePadIsEventFrom);
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
        if (event != null) {
            return super.dispatchKeyEvent(event);
        } else {
            return true;
        }
    }

    void GamepadIncrementSpeed(int whichThrottle, int stepMultiplier) {
        Log.d("Engine_Driver", "GamepadIncrementSpeed()");
        incrementSpeed(whichThrottle, speed_commands_from_type.GAMEPAD, stepMultiplier);
        GamepadFeedbackSound(atMaxSpeed(whichThrottle) || atMinSpeed(whichThrottle));
        tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                , getMaxSpeed(whichThrottle)
                , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                , isSemiRealisticTrottle
                , "");
    }

    void GamepadDecrementSpeed(int whichThrottle, int stepMultiplier) {
        Log.d("Engine_Driver", "GamepadDecrementSpeed()");
        decrementSpeed(whichThrottle, speed_commands_from_type.GAMEPAD, stepMultiplier);
        GamepadFeedbackSound(atMinSpeed(whichThrottle) || atMaxSpeed(whichThrottle));
        tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED, whichThrottle, false
                , getMaxSpeed(whichThrottle)
                , getSpeedFromCurrentSliderPosition(whichThrottle, false)
                , getSpeedFromCurrentSliderPosition(whichThrottle, true)
                , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
                , isSemiRealisticTrottle
                , "");
    }

    void GamepadFeedbackSound(boolean invalidAction) {
        if (mainapp.appIsFinishing) {
            return;
        }

        if (tg != null) {
            if (invalidAction)
                tg.startTone(ToneGenerator.TONE_PROP_NACK);
            else
                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
    }

    void gamepadBeep(int whichBeep) {
        {
            if (mainapp._mediaPlayer != null) {
                mainapp._mediaPlayer.reset();     // reset stops and release on any state of the player
            }
            switch (whichBeep) {
                case 10:
                    mainapp._mediaPlayer = MediaPlayer.create(this, R.raw.beep_1a);
                    break;
                case 2:
                    mainapp._mediaPlayer = MediaPlayer.create(this, R.raw.beep_2);
                    break;
                case 20:
                    mainapp._mediaPlayer = MediaPlayer.create(this, R.raw.beep_2a);
                    break;
                case 1:
                default:
                    mainapp._mediaPlayer = MediaPlayer.create(this, R.raw.beep_1);
                    break;
            }
            mainapp._mediaPlayer.start();
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
        int stepMultiplier = 1;

        private GamepadRptUpdater(int WhichThrottle, int StepMultiplier) {
            whichThrottle = WhichThrottle;
            stepMultiplier = StepMultiplier;

            Log.d("Engine_Driver", "GamepadRptUpdater: WhichThrottle: " + whichThrottle
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
            Log.d("Engine_Driver", "GamepadRptUpdater: run(): WhichThrottle: " + whichThrottle
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
            Log.d("Engine_Driver", "VolumeKeysRptUpdater: WhichThrottle: " + whichThrottle
                    + " mGamepadAutoIncrement: " + (mGamepadAutoIncrement ? "True" : "False")
                    + " mGamepadAutoDecrement: " + (mGamepadAutoDecrement ? "True" : "False")
            );
            whichThrottle = WhichThrottle;

            try {
                REP_DELAY = Integer.parseInt(prefs.getString("speed_arrows_throttle_repeat_delay", "100"));
            } catch (NumberFormatException ex) {
                REP_DELAY = 100;
            }
        }

        @Override
        public void run() {
            Log.d("Engine_Driver", "VolumeKeysRptUpdater: run(): WhichThrottle: " + whichThrottle
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
            Log.d("Engine_Driver", "ESU_MCII: Knob button down for throttle " + whichVolume);
            if (!isScreenLocked) {
                if (!isEsuMc2KnobEnabled) {
                    Log.d("Engine_Driver", "ESU_MCII: Knob disabled - direction change ignored");
                } else if (prefEsuMc2EndStopDirectionChange) {
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
                if (!isEsuMc2KnobEnabled) {
                    Log.d("Engine_Driver", "ESU_MCII: Disabled knob position moved for throttle " + whichVolume);
                    Log.d("Engine_Driver", "ESU_MCII: Nothing updated");
                } else if (getConsist(whichVolume).isActive() && !isEsuMc2Stopped) {
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
    private final StopButtonFragment.OnStopButtonListener esuOnStopButtonListener = new StopButtonFragment.OnStopButtonListener() {
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
    protected void setEsuThrottleKnobPosition(int whichThrottle, int speed) {
        Log.d("Engine_Driver", "ESU_MCII: Request to update knob position for throttle " + whichThrottle);
        if (whichThrottle == whichVolume) {
            int knobPos;
            knobPos = esuThrottleScales[whichThrottle].stepToPosition(speed);
            Log.d("Engine_Driver", "ESU_MCII: Update knob position for throttle " + mainapp.throttleIntToString(whichThrottle));
            Log.d("Engine_Driver", "ESU_MCII: New knob position: " + knobPos + " ; speedstep: " + speed);
            try {
                esuThrottleFragment.moveThrottle(knobPos);
            } catch (IllegalArgumentException ex) {
                Log.e("Engine_Driver", "ESU_MCII: Problem moving throttle " + ex.getMessage());
            }
        } else {
            Log.d("Engine_Driver", "ESU_MCII: This throttle not selected for control by knob");
        }
    }

    private void updateEsuMc2ZeroTrim() {
        int zeroTrim = threaded_application.getIntPrefValue(prefs, "prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
        Log.d("Engine_Driver", "ESU_MCII: Update zero trim for throttle to: " + zeroTrim);

        // first the knob
        esuThrottleFragment.setZeroPosition(zeroTrim);

        // now throttle scales
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            esuThrottleScales[throttleIndex] = new ThrottleScale(zeroTrim, esuThrottleScales[throttleIndex].getStepCount());
        }
    }

    private void performEsuMc2ButtonAction(int buttonNo, int action, boolean isActive, int whichThrottle, int repeatCnt) {

        if (isEsuMc2Stopped) {
            Log.d("Engine_Driver", "ESU_MCII: Device button presses whilst stopped ignored");
//            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastEsuMc2NoButtonPresses), Toast.LENGTH_SHORT).show();
            threaded_application.safeToast(R.string.toastEsuMc2NoButtonPresses, Toast.LENGTH_SHORT);
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
                        changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    }
                    break;
                case DIRECTION_REVERSE:
                    if (!isScreenLocked && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                        changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    }
                    break;
                case DIRECTION_TOGGLE:
                    if (!isScreenLocked && (action == ACTION_DOWN) && (repeatCnt == 0)) {
                        if ((getDirection(whichThrottle) == direction_type.FORWARD)) {
                            changeDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                        } else {
                            changeDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
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
        bLSpds[whichThrottle].setEnabled(enabled);
        bRSpds[whichThrottle].setEnabled(enabled);
        sbs[whichThrottle].setEnabled(enabled);
    }

    /**
     * Display the ESU MCII knob action button if configured
     *
     * @param menu the menu upon which the action should be shown
     */
    private void displayEsuMc2KnobMenuButton(Menu menu) {
        MenuItem mi = menu.findItem(R.id.EsuMc2Knob_button);
        if (mi == null) return;

        if (prefs.getBoolean("prefEsuMc2KnobButtonDisplay", false)) {
            mainapp.actionBarIconCountThrottle++;
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }
        setEsuMc2KnobButton(menu);

    }

    /**
     * Set the state of the ESU MCII knob action button/menu entry
     *
     * @param menu the menu upon which the action is shown
     */
    public void setEsuMc2KnobButton(Menu menu) {
        if (menu != null) {
            if (isEsuMc2KnobEnabled) {
                menu.findItem(R.id.EsuMc2Knob_button).setIcon(R.drawable.esumc2knob_on);
            } else {
                menu.findItem(R.id.EsuMc2Knob_button).setIcon(R.drawable.esumc2knob_off);
            }
        }
    }

    /**
     * Toggle the state of the ESU MCII knob
     *
     * @param activity the requesting activity
     * @param menu     the menu upon which the entry/button should be updated
     */
    public void toggleEsuMc2Knob(Activity activity, Menu menu) {
        isEsuMc2KnobEnabled = !isEsuMc2KnobEnabled;
        setEsuMc2KnobButton(menu);
    }

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
//                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastEsuMc2NoLocoChange), Toast.LENGTH_SHORT).show();
                threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastEsuMc2NoLocoChange), Toast.LENGTH_SHORT);
            } else if (isSelectLocoAllowed(whichThrottle)) {
                // don't loco change while moving if the preference is set
                start_select_loco_activity(whichThrottle); // pass throttle #
                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            } else {
//                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastLocoChangeNotAllowed), Toast.LENGTH_SHORT).show();
                threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastLocoChangeNotAllowed), Toast.LENGTH_SHORT);
            }
            mainapp.buttonVibration();
        }

        @Override
        public boolean onLongClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            start_consist_lights_edit(whichThrottle);
            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
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
        int maxThrottle = threaded_application.getIntPrefValue(prefs, "maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
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
        setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
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

            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
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
//                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastDirectionButtonsSwapped), Toast.LENGTH_SHORT).show();
                            threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastDirectionButtonsSwapped), Toast.LENGTH_SHORT);
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

                // Log.d("Engine_Driver", "onTouch direction " + function + " action " +

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
        sendFunctionToConsistLocos(whichThrottle, function, lab, buttonPressMessageType, leadOnly, trailOnly, followLeadFunction, isLatching, false);
    }
    protected void sendFunctionToConsistLocos(int whichThrottle, int function, String lab, int buttonPressMessageType, boolean leadOnly, boolean trailOnly, boolean followLeadFunction, int isLatching, boolean force) {
        Consist con;
        con = mainapp.consists[whichThrottle];

        String tempPrefConsistFollowRuleStyle = mainapp.prefConsistFollowRuleStyle;
        // if there is only one loco and we are not forcing the use f the Defult Functions, then revert back to the ORIGINAL style
        if (((mainapp.prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.COMPLEX))
                || (mainapp.prefConsistFollowRuleStyle.contains(consist_function_rule_style_type.SPECIAL)))
                && (con.size() == 1)
                && (!mainapp.prefAlwaysUseDefaultFunctionLabels)) {

            tempPrefConsistFollowRuleStyle = consist_function_rule_style_type.ORIGINAL;
        }

        if (force) { // only used for the Sem-Realistic Throttle - ESU Decoder Brakes
            int newFnState = (buttonPressMessageType == button_press_message_type.UP) ? 0 : 1;
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + "*", function, newFnState);

        } else if (tempPrefConsistFollowRuleStyle.equals(consist_function_rule_style_type.ORIGINAL)) {

            String addr = "";
//            if ( (leadOnly) || (mainapp.prefOverrideWiThrottlesFunctionLatching) )
            if (leadOnly)
                addr = con.getLeadAddr();

            if (buttonPressMessageType == button_press_message_type.TOGGLE) {
                mainapp.toggleFunction(mainapp.throttleIntToString(whichThrottle) + addr, function);
            } else {
                if (!mainapp.prefOverrideWiThrottlesFunctionLatching) {
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, buttonPressMessageType);
                } else {
//                    boolean isLatch = mainapp.function_consist_latching.get(function).equals(FUNCTION_CONSIST_LATCHING); // ignore what we were sent
//                    if (isLatch) {
                    boolean fnState = mainapp.function_states[whichThrottle][function];
                    int oldFnState = fnState ? 1 : 0;
                    int newFnState = fnState ? 0 : 1;

                    if (isLatching == consist_function_latching_type.YES) {
                        if (buttonPressMessageType == button_press_message_type.UP) {
//                            int newState = (mainapp.function_states[whichThrottle][function]) ? button_press_message_type.DOWN : button_press_message_type.UP;
//                            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, newState);

                            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, oldFnState);
                            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, newFnState);
//                            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, newFnState);
                            mainapp.function_states[whichThrottle][function] = (newFnState==1);
                        }
                    } else {
//                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, buttonPressMessageType);
//                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, oldFnState);

                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FORCE_FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, newFnState);
//                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, mainapp.throttleIntToString(whichThrottle) + addr, function, newFnState);
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


    public class DoLocoSoundDelayed implements Runnable {
        int whichThrottle;

        public DoLocoSoundDelayed(int WhichThrottle) {
            whichThrottle = WhichThrottle;
        }

        @Override
        public void run() {
//            Log.d("Engine_Driver", "doLocoSoundDelayed.run: (locoSound) wt" + whichThrottle);
            doLocoSound(whichThrottle);
        }
    } // end DoLocoSoundDelayed

    void doLocoSound(int whichThrottle) {
        if (!mainapp.soundsSoundsAreBeingReloaded) {
            if (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES) { // only dealing with the first two throttle for now
                if (!mainapp.prefDeviceSounds[whichThrottle].equals("none")) {
                    int mSound = -1;
                    if ((mainapp.consists != null) && (mainapp.consists[whichThrottle].isActive())) {
                        mSound = getLocoSoundStep(whichThrottle);

                        Log.d("Engine_Driver", "doLocoSound               : (locoSound) wt: " + whichThrottle + " snd: " + mSound);
                        if ((mSound >= 0)) {
                            if (mainapp.soundsLocoCurrentlyPlaying[whichThrottle] == sounds_type.NOTHING_CURRENTLY_PLAYING) { // nothing currently playing
//                                Log.d("Engine_Driver", "doLocoSound 2              : (locoSound) wt: " + whichThrottle + " snd: " + mSound);
                                //see if there is a startup sound for this profile
                                if (mainapp.soundsLocoDuration[whichThrottle][sounds_type.STARTUP_INDEX] > 0) {
//                                    Log.d("Engine_Driver", "doLocoSound 3              : (locoSound) wt: " + whichThrottle + " snd: " + mSound);
                                    soundStart(sounds_type.LOCO, whichThrottle, sounds_type.STARTUP_INDEX, sounds_type.REPEAT_NONE);
                                    soundScheduleNextLocoSound(whichThrottle, mSound, mainapp.soundsLocoDuration[whichThrottle][sounds_type.STARTUP_INDEX]);
                                    soundQueueNextLocoSound(whichThrottle, mSound);
                                } else {
//                                    Log.d("Engine_Driver", "doLocoSound 4              : (locoSound) wt: " + whichThrottle + " snd: " + mSound);
                                    soundStart(sounds_type.LOCO, whichThrottle, mSound, sounds_type.REPEAT_INFINITE);
                                    mainapp.soundsLocoQueue[whichThrottle].setLastAddedValue(mSound);
                                }

                            } else {
                                soundQueueNextLocoSound(whichThrottle, mSound);
                            }
                        }
                    } else {
                        soundsStopAllSoundsForLoco(whichThrottle);
                    }
                }
                mainapp.soundsLocoLastDirection[whichThrottle] = dirs[whichThrottle];
            }
        }
    } // end doLocoSound()

    void soundQueueNextLocoSound(int whichThrottle, int mSound) {
        boolean wasDirectionChange = mainapp.soundsLocoLastDirection[whichThrottle] != dirs[whichThrottle];

        if ((mainapp.soundsLocoCurrentlyPlaying[whichThrottle] == mSound) && (mainapp.soundsLocoQueue[whichThrottle].queueCount() == 0) && (!wasDirectionChange)) {
            return; // sound is already playing and nothing is queued
        }
//        Log.d("Engine_Driver", "soundQueueNextLocoSound  : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " " + mainapp.soundsLocoQueue[whichThrottle].displayQueue());

        int queueCount = mainapp.soundsLocoQueue[whichThrottle].queueCount();

        if (mainapp.soundsLocoQueue[whichThrottle].enqueueWithIntermediateSteps(mSound, wasDirectionChange)) {
            if (queueCount == 0) {
                soundScheduleNextLocoSound(whichThrottle, mSound, -1);
            }
        }
    } // end soundQueueNextLocoSound()

    void soundScheduleNextLocoSound(int whichThrottle, int mSound, int forcedExpectedEndTime) {
//        Log.d("Engine_Driver", "soundScheduleNextLocoSound : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " " + mainapp.soundsLocoQueue[whichThrottle].displayQueue());

        int expectedEndTime;
        if (forcedExpectedEndTime > 0) {
            expectedEndTime = forcedExpectedEndTime;
        } else { // != -1
            expectedEndTime = soundStop(sounds_type.LOCO, whichThrottle, mainapp.soundsLocoCurrentlyPlaying[whichThrottle], false);
        }
        int nextSound = mainapp.soundsLocoQueue[whichThrottle].frontOfQueue();

        if (nextSound >= 0) {
            mainapp.throttle_msg_handler.postDelayed(
                    new SoundScheduleNextSoundToPlay(sounds_type.LOCO, whichThrottle, nextSound),
                    expectedEndTime - 100);
//            Log.d("Engine_Driver", "soundScheduleNextLocoSound : (locoSound) wt:" + whichThrottle + " snd: " + nextSound + " Start in: " + expectedEndTime + "msec");
        }
    } //end soundScheduleNextLocoSound()

    int getLocoSoundStep(int whichThrottle) {
        int rslt;
        int steps = mainapp.soundsLocoSteps[whichThrottle];

        float speed = (float) (getSpeedFromCurrentSliderPosition(whichThrottle, false));
        speed = (float) ((speed / 126 * steps) + 0.99);
        rslt = (int) speed;

//        Log.d("Engine_Driver", "getLocoSoundStep         : (locoSound) wt: " + whichThrottle + " step:" + rslt);
        return rslt;
    } // end getLocoSoundStep()

    void doDeviceButtonSound(int whichThrottle, int soundType) {
        Log.d("Engine_Driver", "doDeviceButtonSound (locoSounds): wt: " + whichThrottle + " soundType: " + soundType);
        int soundTypeArrayIndex = soundType - 1;

        if ((mainapp.consists != null)
                && (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES)  // only dealing with the first two throttles for now
                && (mainapp.consists[whichThrottle].isActive())) {

            if (!mainapp.prefDeviceSounds[whichThrottle].equals("none")) {
                if (!mainapp.soundsDeviceButtonStates[whichThrottle][soundTypeArrayIndex]) {
                    startBellHornSound(soundType, whichThrottle);
                } else {
                    stopBellHornSound(soundType, whichThrottle);
                }
            }
        }
    } // end doDeviceButtonSound()


    // this is not used for Loco sounds
    int soundIsPlaying(int soundType, int whichThrottle) {
        int isPlaying;
        int soundTypeArrayIndex = soundType - 1;

        switch (soundType) {
            case sounds_type.BELL: // bell
            case sounds_type.HORN: // horn
            case sounds_type.HORN_SHORT: // horn short
                isPlaying = mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle];
                break;
            default:
                isPlaying = -1;
                break;
        }
        return isPlaying;
    } // end soundIsPlaying()

    void startBellHornSound(int soundType, int whichThrottle) {
//        Log.d("Engine_Driver", "startBellHornSound        : soundType:" + soundType + " wt: " + whichThrottle);
        int soundTypeArrayIndex = soundType - 1;

        if (soundIsPlaying(soundType, whichThrottle) < 0) { // check if the loop sound is not currently playing
            if (soundType != sounds_type.HORN_SHORT) {
                soundStart(soundType, whichThrottle, sounds_type.BELL_HORN_START, 0);
                // queue up the loop sound to be played when the start sound is finished
                mainapp.throttle_msg_handler.postDelayed(
                        new SoundScheduleNextSoundToPlay(soundType, whichThrottle, sounds_type.BELL_HORN_START),
                        mainapp.soundsExtrasDuration[soundTypeArrayIndex][whichThrottle][sounds_type.BELL_HORN_START]);
            } else {
                soundStart(soundType, whichThrottle, 0, 0);   // mSound always 0 for the short horn (not really used)
            }
        }
    } // end startBellHornSound()

    void stopBellHornSound(int soundType, int whichThrottle) {
//        Log.d("Engine_Driver", "stopBellHornSound        : soundType:" + soundType + " wt: " + whichThrottle + " playing: " + soundIsPlaying(soundType,whichThrottle));
        int soundTypeArrayIndex = soundType - 1;

        if (soundType != sounds_type.HORN_SHORT) {
            if (soundIsPlaying(soundType, whichThrottle) == 0) {
                // if the start sound is currently playing need to do something special
                // as the loop is probably scheduled to run but has not started yet
                mainapp.throttle_msg_handler.postDelayed(
                        new SoundScheduleSoundToStop(soundType, whichThrottle, sounds_type.BELL_HORN_LOOP),
                        mainapp.soundsExtrasDuration[soundTypeArrayIndex][whichThrottle][sounds_type.BELL_HORN_START] + 100);

            } else if (soundIsPlaying(soundType, whichThrottle) == 1) { // check if the loop sound is currently playing
                int expectedEndTime = soundStop(soundType, whichThrottle, sounds_type.BELL_HORN_LOOP, false);
                // queue up the end sound
                mainapp.throttle_msg_handler.postDelayed(
                        new SoundScheduleNextSoundToPlay(soundType, whichThrottle, sounds_type.BELL_HORN_LOOP),
                        expectedEndTime);
            }
        } else { // for the short horn, stop immediately
            soundStop(soundType, whichThrottle, 0, true);
        }
    } // end stopBellHornSound(

    void soundStart(int soundType, int whichThrottle, int mSound, int loop) {
//        Log.d("Engine_Driver", "soundStart: SoundType:" + soundType + " wt: " + whichThrottle + " snd: " + mSound + " loop:" + loop);
        int soundTypeArrayIndex = soundType - 1;

        switch (soundType) {
            default:
            case sounds_type.LOCO: // loco
//                Log.d("Engine_Driver", "soundStart               : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " loop:" + loop);
                if (mSound >= 0) {
                    if (mainapp.soundsLocoCurrentlyPlaying[whichThrottle] != mSound) {
                        if (mSound < sounds_type.STARTUP_INDEX) {
                            mainapp.soundsLocoStreamId[whichThrottle][mSound]
                                    = mainapp.soundPool.play(mainapp.soundsLoco[whichThrottle][mSound],
                                    soundsVolume(sounds_type.LOCO, whichThrottle), soundsVolume(sounds_type.LOCO, whichThrottle),
                                    0, sounds_type.REPEAT_INFINITE, 1);
                        } else if (mSound == sounds_type.STARTUP_INDEX) {
                            mainapp.soundsLocoStreamId[whichThrottle][mSound]
                                    = mainapp.soundPool.play(mainapp.soundsLoco[whichThrottle][mSound],
                                    soundsVolume(sounds_type.LOCO, whichThrottle), soundsVolume(sounds_type.LOCO, whichThrottle),
                                    0, sounds_type.REPEAT_NONE, 1);
//                            Log.d("Engine_Driver", "soundStart SU            : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " loop:" + loop + " Sid: " + mainapp.soundsLocoStreamId[whichThrottle][mSound]);

//                        } else if (mSound == sounds_type.SHUTDOWN_INDEX) {
                        }
                        mainapp.soundsLocoCurrentlyPlaying[whichThrottle] = mSound;
                        mainapp.soundsLocoStartTime[whichThrottle][mSound] = System.currentTimeMillis();
                    }
                }
                break;

            case sounds_type.BELL: // bell
            case sounds_type.HORN: // horn
            case sounds_type.HORN_SHORT: // horn short
                if (mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] != mSound) { // nothing playing
                    mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound]
                            = mainapp.soundPool.play(mainapp.soundsExtras[soundTypeArrayIndex][whichThrottle][mSound],
                            soundsVolume(soundType, whichThrottle), soundsVolume(soundType, whichThrottle),
                            0, loop, 1);
                    mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] = (mSound < 2) ? mSound : -1; // if it is the end sound, treat it like is not playing
                    mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][mSound] = System.currentTimeMillis();
                }
                break;
        }
    }  // end startSound()

    int soundStop(int soundType, int whichThrottle, int mSound, boolean forceStop) {
//        Log.d("Engine_Driver", "soundStop: soundType" + soundType + " wt: " + whichThrottle + " snd: " + mSound);
        int timesPlayed = 0;
        double expectedEndTime = 0;
        int soundTypeArrayIndex = soundType - 1;

        switch (soundType) {
            case sounds_type.BELL: // bell
            case sounds_type.HORN: // horn
                if (mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] >= 0) {
                    if (mSound == sounds_type.BELL_HORN_LOOP) { // assume it is looping
                        mainapp.soundPool.pause(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound]);
                        mainapp.soundPool.setLoop(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound], sounds_type.REPEAT_NONE);  // don't really stop it, just let it finish
                        mainapp.soundPool.resume(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound]);
                        timesPlayed = (int) ((System.currentTimeMillis() - mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][mSound])
                                / mainapp.soundsExtrasDuration[soundTypeArrayIndex][whichThrottle][mSound]);
                        expectedEndTime = (timesPlayed) * mainapp.soundsExtrasDuration[soundTypeArrayIndex][whichThrottle][mSound];
                        expectedEndTime = mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][mSound] + expectedEndTime - System.currentTimeMillis();
                    } else {
                        mainapp.soundPool.stop(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][mSound]);
                    }
                    mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] = -1;
                    mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][mSound] = 0;
                }
                break;

            case sounds_type.HORN_SHORT: // horn short
                if (mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] >= 0) {
                    mainapp.soundPool.stop(mainapp.soundsExtrasStreamId[soundTypeArrayIndex][whichThrottle][0]);
                    mainapp.soundsExtrasCurrentlyPlaying[soundTypeArrayIndex][whichThrottle] = -1;
                    mainapp.soundsExtrasStartTime[soundTypeArrayIndex][whichThrottle][0] = 0;
                }
                break;

            case sounds_type.LOCO: // loco
            default:
//                Log.d("Engine_Driver", "soundStop                : (locoSound) wt: " + whichThrottle + " mSound: " + snd + " forceStop:" + forceStop);
                if (mSound >= 0) {
                    if (mSound < sounds_type.STARTUP_INDEX) {
                        if (!forceStop) {
                            double duration = mainapp.soundsLocoDuration[whichThrottle][mSound];

                            // number of complete completed loops
                            timesPlayed = (int) ((System.currentTimeMillis() - mainapp.soundsLocoStartTime[whichThrottle][mSound])
                                    / mainapp.soundsLocoDuration[whichThrottle][mSound]);
                            expectedEndTime = (timesPlayed + 1) * mainapp.soundsLocoDuration[whichThrottle][mSound];

                            int repeats = 1;
                            if (expectedEndTime < mainapp.prefDeviceSoundsMomentum) {
                                repeats = 2;
                            }
                            if ((duration * (repeats + 1)) < mainapp.prefDeviceSoundsMomentum) { // if the sound is less than the preference period 1 second will need to repeat it
                                repeats = (int) (mainapp.prefDeviceSoundsMomentum / duration) + 1;
                            }
//                            double x = expectedEndTime + repeats * duration;
//                        Log.d("Engine_Driver", "soundStop                : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " expected to end in: " +(x/1000)+"sec" );

                            mainapp.soundPool.pause(mainapp.soundsLocoStreamId[whichThrottle][mSound]); // unfortunately you seem to have to pause it to change the number of repeats
                            mainapp.soundPool.setLoop(mainapp.soundsLocoStreamId[whichThrottle][mSound], repeats);  // don't really stop it, just let it finish
                            mainapp.soundPool.resume(mainapp.soundsLocoStreamId[whichThrottle][mSound]);

                            if (mainapp.prefDeviceSoundsMomentumOverride) {
                                expectedEndTime = (timesPlayed + repeats) * mainapp.soundsLocoDuration[whichThrottle][mSound];
                                expectedEndTime = mainapp.soundsLocoStartTime[whichThrottle][mSound] + expectedEndTime - System.currentTimeMillis();
                            } else {
                                expectedEndTime = mainapp.prefDeviceSoundsMomentum;  // schedule the next sound for the preference amount regardless
                            }

//                        Log.d("Engine_Driver", "soundStop                : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " timesPlayed:" + timesPlayed + " will end in: " +(expectedEndTime/1000)+"sec" );
                        } else {
                            mainapp.soundPool.stop(mainapp.soundsLocoStreamId[whichThrottle][mSound]);
//                        Log.d("Engine_Driver", "soundStop                : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " timesPlayed:" + timesPlayed + " FORCED STOP");
                        }
                    } else if (mSound == sounds_type.STARTUP_INDEX) {
                        // this will only ever play once
                        if (!forceStop) {
                            expectedEndTime = mainapp.soundsLocoStartTime[whichThrottle][mSound] + mainapp.soundsLocoDuration[whichThrottle][mSound] - System.currentTimeMillis();
//                            Log.d("Engine_Driver", "soundStop                : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " timesPlayed:" + timesPlayed + " will end in: " +(expectedEndTime/1000)+"sec" );
                        } else {
                            mainapp.soundPool.stop(mainapp.soundsLocoStreamId[whichThrottle][mSound]);
//                            Log.d("Engine_Driver", "soundStop                : (locoSound) wt: " + whichThrottle + " snd: " + mSound + " timesPlayed:" + timesPlayed + " FORCED STOP");
                        }

                    }
                }
                break;

        }
        return (int) expectedEndTime;
    } // end stopSound()

    // used for the loco sounds only
    public class SoundScheduleNextSoundToPlay implements Runnable {
        int whichThrottle;
        int soundType;
        int mSound;

        public SoundScheduleNextSoundToPlay(int SoundType, int WhichThrottle, int MSound) {
            whichThrottle = WhichThrottle;
            soundType = SoundType;
            mSound = MSound;
        }

        @Override
        public void run() {
//            Log.d("Engine_Driver", "SoundScheduleNextSoundToPlay.run: Type" + soundType + " wt: " + whichThrottle + " snd: " + mSound);
            switch (soundType) {
                default:
                    break;
                case sounds_type.LOCO: // loco
                    // pull the next sound off the queue
//                    Log.d("Engine_Driver", "SoundScheduleNextSoundToPlay.run: (locoSound) wt: " + whichThrottle + " snd: " + mSound);
                    soundStop(soundType, whichThrottle, mainapp.soundsLocoCurrentlyPlaying[whichThrottle], true);
                    soundStart(soundType, whichThrottle, mSound, sounds_type.REPEAT_INFINITE);
                    mainapp.soundsLocoQueue[whichThrottle].dequeue();
                    if (mainapp.soundsLocoQueue[whichThrottle].queueCount() > 0) {  // if there are more on the queue, start the process to stop the one you just started
                        soundScheduleNextLocoSound(whichThrottle, mainapp.soundsLocoQueue[whichThrottle].frontOfQueue(), -1);
                    }
                    break;
                case sounds_type.BELL: // bell
                case sounds_type.HORN: // horn
                    int loop = (mSound == sounds_type.BELL_HORN_START) ? sounds_type.REPEAT_INFINITE : sounds_type.REPEAT_NONE; // of 0 now, then we will be playig the lop sound next.
                    soundStart(soundType, whichThrottle, mSound + 1, loop);
                    break;
//                case sounds_type.HORN_SHORT:
//                    break;
            }
        }
    } // end class SoundScheduleNextSoundToPlay

    public class SoundScheduleSoundToStop implements Runnable {
        int whichThrottle;
        int soundType;
        int mSound;

        public SoundScheduleSoundToStop(int SoundType, int WhichThrottle, int MSound) {
            whichThrottle = WhichThrottle;
            soundType = SoundType;
            mSound = MSound;
        }

        @Override
        public void run() {
//            Log.d("Engine_Driver", "SoundScheduleSoundToStop.run: Type" + soundType + " wt: " + whichThrottle + " snd: " + mSound);
            switch (soundType) {
                default:
                    break;
                case sounds_type.HORN: // horn
                case sounds_type.BELL: // bell
                    soundStop(soundType, whichThrottle, mSound, false);
                    break;
//                case sounds_type.HORN_SHORT:
//                    break;
            }
        }
    } // end class SoundScheduleSoundToStop

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
            // Log.d("Engine_Driver", "onTouch func " + function + " action " +

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
//            Log.d("Engine_Driver", "handleAction - action: " + action );
            int isLatching = consist_function_latching_type.NA;  // only used for the special consist function matching

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    switch (this.function) {
                        case function_button.STOP:
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
                            break;

                        case function_button.SPEED_LABEL:  // specify which throttle the volume button controls
                            if (getConsist(whichThrottle).isActive() && !(IS_ESU_MCII && isEsuMc2Stopped)) { // only assign if Active and, if an ESU MCII not in Stop mode
                                whichVolume = whichThrottle;
                                mainapp.whichThrottleLastTouch = whichThrottle;
                                set_labels();
                            }
                            if (IS_ESU_MCII && isEsuMc2Stopped) {
//                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getText(R.string.toastEsuMc2NoThrottleChange), Toast.LENGTH_SHORT).show();
                                threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastEsuMc2NoThrottleChange), Toast.LENGTH_SHORT);
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
                    if (function == function_button.STOP) {
                        set_stop_button(whichThrottle, false);
                    }
                    // only process UP event if this is a "function" button
                    else if (function < direction_button.LEFT) {
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
//             Log.d("Engine_Driver", "throttle: ThrottleSeekBarListener: onTouch: Throttle action " + event.getAction());
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
//                Log.d("Engine_Driver", "onProgressChanged -- lj: " + limitedJump[whichThrottle] + " ai: " + mAutoIncrement[whichThrottle] + " ad: " + mAutoDecrement + " s: " + speed + " js: " + jumpSpeed);

            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (touchFromUser)) {

                if ((!limitedJump[whichThrottle])         // touch generates multiple onProgressChanged events, skip processing after first limited jump
                        && (sliderType != slider_type.SWITCHING)) {

                    int dif = speed - lastSpeed;
                    if (prefSpeedButtonsSpeedStepDecrement) {  // don't limit the decrement speed if the preference is not set
                        dif = (Math.abs(speed - lastSpeed));
                    }

                    if (dif > max_throttle_change) { // if jump is too large then limit it

                        // Log.d("Engine_Driver", "onProgressChanged -- throttling change");

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
                    doLocoSound(whichThrottle);

                } else {                      // got a touch while processing limitJump
                    speed = lastSpeed;    //   so suppress multiple touches
                    throttle.setProgress(lastSpeed);
                    doLocoSound(whichThrottle);
                }
                // Now update ESU MCII Knob position
                if (IS_ESU_MCII) {
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
                        doLocoSound(whichThrottle);
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

    @SuppressLint({"Recycle", "SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Engine_Driver", "throttle: onCreate(): called");
        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        mainapp.applyTheme(this);

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
                Log.d("Engine_Driver", "Restore of saved instance state failed " + android.os.Build.VERSION.SDK_INT);
            }
        }


        if (mainapp.isForcingFinish()) { // expedite
            mainapp.appIsFinishing = true;
            return;
        }

        // put pointer to this activity's handler in main app's shared variable
//        mainapp.throttle_msg_handler = new throttle_handler();

        sliderType = slider_type.HORIZONTAL;

        setContentView(mainapp.throttleLayoutViewId);

        getCommonPrefs(true); // get all the common preferences
        mainapp.getDefaultSortOrderRoster();
        setThrottleNumLimits();

        getDirectionButtonPrefs();

        webViewIsOn = !webViewLocation.equals(web_view_location_type.NONE);
        keepWebViewLocation = webViewLocation;

        isScreenLocked = false;

        // get the screen brightness on create
        screenBrightnessOriginal = getScreenBrightness();
        screenBrightnessModeOriginal = getScreenBrightnessMode();

        // myGesture = new GestureDetector(this);
        throttleOverlay = findViewById(R.id.throttle_overlay);
        throttleOverlay.addOnGestureListener(this);
        throttleOverlay.setGestureVisible(false);

        DirectionButtonTouchListener directionButtonTouchListener;
        FunctionButtonTouchListener functionButtonTouchListener;
        SelectFunctionButtonTouchListener selectFunctionButtonTouchListener;
        ArrowSpeedButtonTouchListener arrowSpeedButtonTouchListener;

        initialiseArrays();

        // throttle layouts
        vThrotScr = findViewById(R.id.throttle_screen);
        vThrotScrWrap = findViewById(R.id.throttle_screen_wrapper);

        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            // set listener for select loco buttons
            Button bSel = findViewById(R.id.button_select_loco_0);
            TextView tvbSelsLabel = findViewById(R.id.loco_label_0);
            TextView tvLeft = findViewById(R.id.loco_left_direction_indication_0);
            TextView tvRight = findViewById(R.id.loco_right_direction_indication_0);
            switch (i) {
                case 1:
                    bSel = findViewById(R.id.button_select_loco_1);
                    tvbSelsLabel = findViewById(R.id.loco_label_1);
                    tvLeft = findViewById(R.id.loco_left_direction_indication_1);
                    tvRight = findViewById(R.id.loco_right_direction_indication_1);
                    break;
                case 2:
                    bSel = findViewById(R.id.button_select_loco_2);
                    tvbSelsLabel = findViewById(R.id.loco_label_2);
                    tvLeft = findViewById(R.id.loco_left_direction_indication_2);
                    tvRight = findViewById(R.id.loco_right_direction_indication_2);
                    break;
                case 3:
                    bSel = findViewById(R.id.button_select_loco_3);
                    tvbSelsLabel = findViewById(R.id.loco_label_3);
                    tvLeft = findViewById(R.id.loco_left_direction_indication_3);
                    tvRight = findViewById(R.id.loco_right_direction_indication_3);
                    break;
                case 4:
                    bSel = findViewById(R.id.button_select_loco_4);
                    tvbSelsLabel = findViewById(R.id.loco_label_4);
                    tvLeft = findViewById(R.id.loco_left_direction_indication_4);
                    tvRight = findViewById(R.id.loco_right_direction_indication_4);
                    break;
                case 5:
                    bSel = findViewById(R.id.button_select_loco_5);
                    tvbSelsLabel = findViewById(R.id.loco_label_5);
                    tvLeft = findViewById(R.id.loco_left_direction_indication_5);
                    tvRight = findViewById(R.id.loco_right_direction_indication_5);
                    break;
            }
            bSels[i] = bSel;
            tvbSelsLabels[i] = tvbSelsLabel;
            bSels[i].setClickable(true);
            selectFunctionButtonTouchListener = new SelectFunctionButtonTouchListener(i);
            bSels[i].setOnClickListener(selectFunctionButtonTouchListener);
//            bSels[i].setOnTouchListener(sfbt);
            bSels[i].setOnLongClickListener(selectFunctionButtonTouchListener);  // Consist Light Edit
            tvLeftDirInds[i] = tvLeft;
            tvRightDirInds[i] = tvRight;

            // Arrow Keys
            try {

                Button bRight = findViewById(R.id.right_speed_button_0);
                Button bLeft = findViewById(R.id.left_speed_button_0);
                switch (i) {
                    case 1:
                        bRight = findViewById(R.id.right_speed_button_1);
                        bLeft = findViewById(R.id.left_speed_button_1);
                        break;
                    case 2:
                        bRight = findViewById(R.id.right_speed_button_2);
                        bLeft = findViewById(R.id.left_speed_button_2);
                        break;
                    case 3:
                        bRight = findViewById(R.id.right_speed_button_3);
                        bLeft = findViewById(R.id.left_speed_button_3);
                        break;
                    case 4:
                        bRight = findViewById(R.id.right_speed_button_4);
                        bLeft = findViewById(R.id.left_speed_button_4);
                        break;
                    case 5:
                        bRight = findViewById(R.id.right_speed_button_5);
                        bLeft = findViewById(R.id.left_speed_button_5);
                        break;
                }
                bRSpds[i] = bRight;
                bRSpds[i].setClickable(true);
                arrowSpeedButtonTouchListener = new ArrowSpeedButtonTouchListener(i, speed_button_type.RIGHT);
                bRSpds[i].setOnLongClickListener(arrowSpeedButtonTouchListener);
                bRSpds[i].setOnTouchListener(arrowSpeedButtonTouchListener);
                bRSpds[i].setOnClickListener(arrowSpeedButtonTouchListener);

                bLSpds[i] = bLeft;
                bLSpds[i].setClickable(true);
                arrowSpeedButtonTouchListener = new ArrowSpeedButtonTouchListener(i, speed_button_type.LEFT);
                bLSpds[i].setOnLongClickListener(arrowSpeedButtonTouchListener);
                bLSpds[i].setOnTouchListener(arrowSpeedButtonTouchListener);
                bLSpds[i].setOnClickListener(arrowSpeedButtonTouchListener);

            } catch (Exception ex) {
                Log.d("debug", "onCreate: " + ex.getMessage());
            }

            // set listeners for 3 direction buttons for each throttle
            //----------------------------------------
            Button bFwd = findViewById(R.id.button_fwd_0);
            Button bStop = findViewById(R.id.button_stop_0);
            Button bPause = null;
            Button bRev = findViewById(R.id.button_rev_0);
            View v = findViewById(R.id.speed_cell_0);
            switch (i) {
                case 1:
                    bFwd = findViewById(R.id.button_fwd_1);
                    bStop = findViewById(R.id.button_stop_1);
                    bRev = findViewById(R.id.button_rev_1);
                    v = findViewById(R.id.speed_cell_1);
                    break;
                case 2:
                    bFwd = findViewById(R.id.button_fwd_2);
                    bStop = findViewById(R.id.button_stop_2);
                    bRev = findViewById(R.id.button_rev_2);
                    v = findViewById(R.id.speed_cell_2);
                    break;
                case 3:
                    bFwd = findViewById(R.id.button_fwd_3);
                    bStop = findViewById(R.id.button_stop_3);
                    bRev = findViewById(R.id.button_rev_3);
                    v = findViewById(R.id.speed_cell_3);
                    break;
                case 4:
                    bFwd = findViewById(R.id.button_fwd_4);
                    bStop = findViewById(R.id.button_stop_4);
                    bRev = findViewById(R.id.button_rev_4);
                    v = findViewById(R.id.speed_cell_4);
                    break;
                case 5:
                    bFwd = findViewById(R.id.button_fwd_5);
                    bStop = findViewById(R.id.button_stop_5);
                    bRev = findViewById(R.id.button_rev_5);
                    v = findViewById(R.id.speed_cell_5);
                    break;
            }

            bFwds[i] = bFwd;
            directionButtonTouchListener = new DirectionButtonTouchListener(direction_button.LEFT, i);
            bFwds[i].setOnTouchListener(directionButtonTouchListener);

            bStops[i] = bStop;
            functionButtonTouchListener = new FunctionButtonTouchListener(function_button.STOP, i);
            bStops[i].setOnTouchListener(functionButtonTouchListener);

            bRevs[i] = bRev;
            directionButtonTouchListener = new DirectionButtonTouchListener(direction_button.RIGHT, i);
            bRevs[i].setOnTouchListener(directionButtonTouchListener);

            functionButtonTouchListener = new FunctionButtonTouchListener(function_button.SPEED_LABEL, i);
            v.setOnTouchListener(functionButtonTouchListener);

            // set up listeners for all throttles
            SeekBar s = findViewById(R.id.speed_0);
            switch (i) {
                case 1:
                    s = findViewById(R.id.speed_1);
                    break;
                case 2:
                    s = findViewById(R.id.speed_2);
                    break;
                case 3:
                    s = findViewById(R.id.speed_3);
                    break;
                case 4:
                    s = findViewById(R.id.speed_4);
                    break;
                case 5:
                    s = findViewById(R.id.speed_5);
                    break;
            }
            ThrottleSeekBarListener thl;
            sbs[i] = s;
            thl = new ThrottleSeekBarListener(i);
            sbs[i].setOnSeekBarChangeListener(thl);
            sbs[i].setOnTouchListener(thl);

            max_throttle_change = 1;

            switch (i) {
                case 0:
                    llThrottleLayouts[i] = findViewById(R.id.throttle_0);
                    llLocoIdAndSpeedViewGroups[i] = findViewById(R.id.loco_buttons_group_0);
                    llLocoDirectionButtonViewGroups[i] = findViewById(R.id.dir_buttons_table_0);
                    tvVols[i] = findViewById(R.id.volume_indicator_0); // volume indicators
                    tvGamePads[i] = findViewById(R.id.gamepad_indicator_0); // gamepad indicators
                    tvSpdLabs[i] = findViewById(R.id.speed_label_0); // set_default_function_labels();
                    tvSpdVals[i] = findViewById(R.id.speed_value_label_0);
                    functionButtonViewGroups[i] = findViewById(R.id.function_buttons_table_0);
                    break;
                case 1:
                    llThrottleLayouts[i] = findViewById(R.id.throttle_1);
                    llLocoIdAndSpeedViewGroups[i] = findViewById(R.id.loco_buttons_group_1);
                    llLocoDirectionButtonViewGroups[i] = findViewById(R.id.dir_buttons_table_1);
                    tvVols[i] = findViewById(R.id.volume_indicator_1); // volume indicators
                    tvGamePads[i] = findViewById(R.id.gamepad_indicator_1); // gamepad indicators
                    tvSpdLabs[i] = findViewById(R.id.speed_label_1); // set_default_function_labels();
                    tvSpdVals[i] = findViewById(R.id.speed_value_label_1);
                    functionButtonViewGroups[i] = findViewById(R.id.function_buttons_table_1);
                    break;
                case 2:
                    llThrottleLayouts[i] = findViewById(R.id.throttle_2);
                    llLocoIdAndSpeedViewGroups[i] = findViewById(R.id.loco_buttons_group_2);
                    llLocoDirectionButtonViewGroups[i] = findViewById(R.id.dir_buttons_table_2);
                    tvVols[i] = findViewById(R.id.volume_indicator_2); // volume indicators
                    tvGamePads[i] = findViewById(R.id.gamepad_indicator_2); // gamepad indicators
                    tvSpdLabs[i] = findViewById(R.id.speed_label_2); // set_default_function_labels();
                    tvSpdVals[i] = findViewById(R.id.speed_value_label_2);
                    functionButtonViewGroups[i] = findViewById(R.id.function_buttons_table_2);
                    break;
                case 3:
                    llThrottleLayouts[i] = findViewById(R.id.throttle_3);
                    llLocoIdAndSpeedViewGroups[i] = findViewById(R.id.loco_buttons_group_3);
                    llLocoDirectionButtonViewGroups[i] = findViewById(R.id.dir_buttons_table_3);
                    tvVols[i] = findViewById(R.id.volume_indicator_3); // volume indicators
                    tvGamePads[i] = findViewById(R.id.gamepad_indicator_3); // gamepad indicators
                    tvSpdLabs[i] = findViewById(R.id.speed_label_3); // set_default_function_labels();
                    tvSpdVals[i] = findViewById(R.id.speed_value_label_3);
                    functionButtonViewGroups[i] = findViewById(R.id.function_buttons_table_3);
                    break;
                case 4:
                    llThrottleLayouts[i] = findViewById(R.id.throttle_4);
                    llLocoIdAndSpeedViewGroups[i] = findViewById(R.id.loco_buttons_group_4);
                    llLocoDirectionButtonViewGroups[i] = findViewById(R.id.dir_buttons_table_4);
                    tvVols[i] = findViewById(R.id.volume_indicator_4); // volume indicators
                    tvGamePads[i] = findViewById(R.id.gamepad_indicator_4); // gamepad indicators
                    tvSpdLabs[i] = findViewById(R.id.speed_label_4); // set_default_function_labels();
                    tvSpdVals[i] = findViewById(R.id.speed_value_label_4);
                    functionButtonViewGroups[i] = findViewById(R.id.function_buttons_table_4);
                    break;
                case 5:
                    llThrottleLayouts[i] = findViewById(R.id.throttle_5);
                    llLocoIdAndSpeedViewGroups[i] = findViewById(R.id.loco_buttons_group_5);
                    llLocoDirectionButtonViewGroups[i] = findViewById(R.id.dir_buttons_table_5);
                    tvVols[i] = findViewById(R.id.volume_indicator_5); // volume indicators
                    tvGamePads[i] = findViewById(R.id.gamepad_indicator_5); // gamepad indicators
                    tvSpdLabs[i] = findViewById(R.id.speed_label_5); // set_default_function_labels();
                    tvSpdVals[i] = findViewById(R.id.speed_value_label_5);
                    functionButtonViewGroups[i] = findViewById(R.id.function_buttons_table_5);
                    break;
            }

            // set throttle change delay timers
            changeTimers[i] = new PacingDelay(changeDelay);
            speedMessagePacingTimers[i] = new SpeedPacingDelay(maxSpeedMessageRate, i);
        }

        clearVolumeAndGamepadAdditionalIndicators();

        setDirectionButtonLabels(); // set all the direction button labels

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        if ((!prefThrottleScreenType.contains(throttle_screen_type.CONTAINS_SWITCHING)) || (prefThrottleScreenType.equals(throttle_screen_type.SWITCHING_HORIZONTAL))) {
            // set listeners for the limit speed buttons for each throttle
            LimitSpeedButtonTouchListener limitSpeedButtonTouchListener;
            Button bLimitSpeed = findViewById(R.id.limit_speed_0);

            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                switch (throttleIndex) {
                    case 0:
                        bLimitSpeed = findViewById(R.id.limit_speed_0);
                        break;
                    case 1:
                        bLimitSpeed = findViewById(R.id.limit_speed_1);
                        break;
                    case 2:
                        bLimitSpeed = findViewById(R.id.limit_speed_2);
                        break;
                    case 3:
                        bLimitSpeed = findViewById(R.id.limit_speed_3);
                        break;
                    case 4:
                        bLimitSpeed = findViewById(R.id.limit_speed_4);
                        break;
                    case 5:
                        bLimitSpeed = findViewById(R.id.limit_speed_5);
                        break;
                }
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
        }

        // set listeners for the pause buttons for each throttle
        PauseSpeedButtonTouchListener pauseSpeedButtonTouchListener;
        Button bPauseSpeed = findViewById(R.id.pause_speed_0);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            switch (throttleIndex) {
                case 0:
                    bPauseSpeed = findViewById(R.id.pause_speed_0);
                    break;
                case 1:
                    bPauseSpeed = findViewById(R.id.pause_speed_1);
                    break;
                case 2:
                    bPauseSpeed = findViewById(R.id.pause_speed_2);
                    break;
                case 3:
                    bPauseSpeed = findViewById(R.id.pause_speed_3);
                    break;
                case 4:
                    bPauseSpeed = findViewById(R.id.pause_speed_4);
                    break;
                case 5:
                    bPauseSpeed = findViewById(R.id.pause_speed_5);
                    break;
            }
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
        Button bMute = findViewById(R.id.device_sounds_mute_0);
        SoundDeviceExtrasButtonTouchListener soundsExtrasTl;
        Button bBell = findViewById(R.id.device_sounds_bell_0);
        Button bHorn = findViewById(R.id.device_sounds_horn_0);
        Button bHornShort = findViewById(R.id.device_sounds_horn_short_0);

        for (int throttleIndex = 0; ((throttleIndex < mainapp.maxThrottles) && (throttleIndex < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES)); throttleIndex++) {
            switch (throttleIndex) {
                case 0:
                    bMute = findViewById(R.id.device_sounds_mute_0);
                    bBell = findViewById(R.id.device_sounds_bell_0);
                    bHorn = findViewById(R.id.device_sounds_horn_0);
                    bHornShort = findViewById(R.id.device_sounds_horn_short_0);
                    break;
                case 1:
                    bMute = findViewById(R.id.device_sounds_mute_1);
                    bBell = findViewById(R.id.device_sounds_bell_1);
                    bHorn = findViewById(R.id.device_sounds_horn_1);
                    bHornShort = findViewById(R.id.device_sounds_horn_short_1);
                    break;
            }
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

        if (prefs.getString("WebViewLocation", web_view_location_type.NONE).equals(web_view_location_type.TOP)) {
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
        if (savedInstanceState != null) {
            if (savedInstanceState.getSerializable("scale") != null) {
                scale = (float) savedInstanceState.getSerializable(("scale"));
            }
        }
        webView.setInitialScale((int) (100 * scale));
        webView.clearCache(true);   // force fresh javascript download on first connection

        // enable remote debugging of all webviews
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
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

            // above form of shouldOverrideUrlloading is deprecated so support the new form if available
            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleLoadingErrorRetries();
            }

            // stop page from continually reloading when loading errors occur
            // (this can happen if the initial web page pref is set to a non-existant url)
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
        if (currentUrl == null || savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            load_webview(); // reload if no saved state or no page had loaded when state was saved
        } else {
            webView.setInitialScale((int) (100 * scale));   // apply scale to restored webView
        }

        //longpress webview to reload
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
            Log.e("Engine_Driver", "new ToneGenerator failed. Runtime Exception, OS " + android.os.Build.VERSION.SDK_INT + " Message: " + e);
        }
        // set GamePad Support
        setGamepadKeys();

        // initialise ESU MCII
        if (IS_ESU_MCII) {
            Log.d("Engine_Driver", "ESU_MCII: Initialise fragments...");
            int zeroTrim = threaded_application.getIntPrefValue(prefs, "prefEsuMc2ZeroTrim", getApplicationContext().getResources().getString(R.string.prefEsuMc2ZeroTrimDefaultValue));
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

        tts = new Tts(prefs, mainapp);

        if (prefs.getBoolean("prefImportServerAuto", getApplicationContext().getResources().getBoolean(R.bool.prefImportServerAutoDefaultValue))) {
            autoImportFromURL();
        }

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
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

    } // end of onCreate()

    @SuppressLint("ApplySharedPref")
    @Override
    public void onResume() {
        Log.d("Engine_Driver", "throttle: onResume(): called");
        super.onResume();
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

        setGamepadKeys();

        applySpeedRelatedOptions();  // update all throttles

        if (mainapp.soundsReloadSounds) loadSounds();

        set_labels(); // handle labels and update view
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);     //create this here so onPause/onResume for webViews can control it
        }
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
            Log.d("Engine_Driver", "connection: Forced Restart Reason: " + prefForcedRestartReason);
            if (mainapp.prefsForcedRestart(prefForcedRestartReason)) {
                Intent in = new Intent().setClass(this, SettingsActivity.class);
                startActivityForResult(in, 0);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            }
        }
    } // end onResume

    private void initialiseArrays() {
        bSels = new Button[mainapp.maxThrottlesCurrentScreen];
        tvbSelsLabels = new TextView[mainapp.maxThrottlesCurrentScreen];
        bRSpds = new Button[mainapp.maxThrottlesCurrentScreen];
        bLSpds = new Button[mainapp.maxThrottlesCurrentScreen];

        tvLeftDirInds = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvRightDirInds = new TextView[mainapp.maxThrottlesCurrentScreen];

        bFwds = new Button[mainapp.maxThrottlesCurrentScreen];
        bStops = new Button[mainapp.maxThrottlesCurrentScreen];
        bPauses = new Button[mainapp.maxThrottlesCurrentScreen];
        bRevs = new Button[mainapp.maxThrottlesCurrentScreen];
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
        tvSpdLabs = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvSpdVals = new TextView[mainapp.maxThrottlesCurrentScreen];
        tvVols = new TextView[mainapp.maxThrottlesCurrentScreen];
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

    private void showHideConsistMenus() {
        if ((mainapp.consists == null) && (!mainapp.isDCCEX)) {
            Log.d("Engine_Driver", "showHideConsistMenu consists[] is null and not DCC-EX");
            return;
        }

        if (TMenu != null) {
            boolean anyConsist = false;
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                Consist con = mainapp.consists[throttleIndex];
                if (con == null) {
                    Log.d("Engine_Driver", "showHideConsistMenu consists[" + throttleIndex + "] is null");
                    break;
                }
                boolean isMulti = con.isMulti();
                switch (throttleIndex) {
                    case 0:
                        anyConsist |= isMulti;
                        TMenu.findItem(R.id.EditConsist0_menu).setVisible(isMulti);
                        TMenu.findItem(R.id.EditLightsConsist0_menu).setVisible(isMulti);
                        break;
                    case 1:
                        anyConsist |= isMulti;
                        TMenu.findItem(R.id.EditLightsConsist1_menu).setVisible(isMulti);
                        TMenu.findItem(R.id.EditConsist1_menu).setVisible(isMulti);
                        break;
                    case 2:
                        anyConsist |= isMulti;
                        TMenu.findItem(R.id.EditLightsConsist2_menu).setVisible(isMulti);
                        TMenu.findItem(R.id.EditConsist2_menu).setVisible(isMulti);
                        break;
                    case 3:
                        anyConsist |= isMulti;
                        TMenu.findItem(R.id.EditLightsConsist3_menu).setVisible(isMulti);
                        TMenu.findItem(R.id.EditConsist3_menu).setVisible(isMulti);
                        break;
                    case 4:
                        anyConsist |= isMulti;
                        TMenu.findItem(R.id.EditLightsConsist4_menu).setVisible(isMulti);
                        TMenu.findItem(R.id.EditConsist4_menu).setVisible(isMulti);
                        break;
                    case 5:
                        anyConsist |= isMulti;
                        TMenu.findItem(R.id.EditLightsConsist5_menu).setVisible(isMulti);
                        TMenu.findItem(R.id.EditConsist5_menu).setVisible(isMulti);
                        break;
                }
            }
            TMenu.findItem(R.id.edit_consists_menu).setVisible(anyConsist);

            boolean isSpecial = (mainapp.prefAlwaysUseDefaultFunctionLabels)
                    && (mainapp.prefConsistFollowRuleStyle.contains(consist_function_rule_style_type.SPECIAL));
//            TMenu.findItem(R.id.function_consist_settings_mnu).setVisible(isSpecial || mainapp.isDCCEX);
            TMenu.findItem(R.id.function_consist_settings_mnu).setVisible(true);
            if (!isSpecial) {
                if (mainapp.isDCCEX) {
                    TMenu.findItem(R.id.function_consist_settings_mnu).setTitle(R.string.dccExFunctionSettings);
                } else {
                    TMenu.findItem(R.id.function_consist_settings_mnu).setVisible(mainapp.prefOverrideWiThrottlesFunctionLatching);
                    TMenu.findItem(R.id.function_consist_settings_mnu).setTitle(R.string.functionLatchingSettings);
                }
            } else {
                TMenu.findItem(R.id.function_consist_settings_mnu).setTitle(R.string.functionConsistSettings);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webViewIsOn) {
            pauseWebView();
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync();
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
            setImmersiveModeOn(webView, false);
            set_labels();       // need to redraw button Press states since ActionBar and Notification access clears them
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
    public void onStart() {
        Log.d("Engine_Driver", "throttle: onStart(): called");
        super.onStart();
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.throttle_msg_handler == null)
            mainapp.throttle_msg_handler = new ThrottleMessageHandler(Looper.getMainLooper());
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
        Log.d("Engine_Driver", "throttle: onDestroy(): called");
        super.onDestroy();
        Log.d("Engine_Driver", "throttle.onDestroy() called");
        if (!isRestarting) {
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
            Log.d("Engine_Driver", "onDestroy: mainapp.throttle_msg_handler is null. Unable to removeCallbacksAndMessages");
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
        webViewIsOn = !webViewLocation.equals(web_view_location_type.NONE);
        if (!webViewIsOn) {                         // if not displaying webview
            url = noUrl;
            currentUrl = null;
            firstUrl = null;
        } else {
            if (url == null || url.equals(noUrl)) {                // if initializing
                mainapp.defaultThrottleWebViewURL = prefs.getString("InitialThrotWebPage",
                        getApplicationContext().getResources().getString(R.string.prefInitialThrotWebPageDefaultValue));
                url = mainapp.createUrl(mainapp.defaultThrottleWebViewURL);
                if (url == null) {      //if port is invalid
                    url = noUrl;
                }
                firstUrl = null;
            }
            webView.loadUrl(url);
        }
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
        Log.d("Engine_Driver", "throttle: set_function_labels_and_listeners_for_view() called");

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
                        if (mainapp.function_labels[whichThrottle] != null
                                && mainapp.function_labels[whichThrottle].size() > 0) {
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

    protected void set_labels() {
//         Log.d("Engine_Driver","throttle: set_labels() starting");

        if (mainapp.appIsFinishing) {
            return;
        }

        // avoid NPE by not letting this run too early (reported to Play Store)
        if (tvVols[0] == null) return;

        // hide or display volume control indicator based on variable
        setVolumeIndicator();
        setGamepadIndicator();

        // set up max speeds for throttles
        int maxThrottle = threaded_application.getIntPrefValue(prefs, "maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (maxThrottle * .01)); // convert from percent
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            sbs[throttleIndex].setMax(maxThrottle);
        }

        // set max allowed change for throttles from prefs
        int maxChange = threaded_application.getIntPrefValue(prefs, "maximum_throttle_change_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleChangeDefaultValue));
        max_throttle_change = (int) Math.round(maxThrottle * (maxChange * .01));

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            sbs[throttleIndex].setMax(maxThrottle);
            if (mainapp.consists != null && mainapp.consists[throttleIndex] != null &&
                    mainapp.consists[throttleIndex].isEmpty()) {
                maxSpeedSteps[throttleIndex] = 100;
                limitSpeedMax[throttleIndex] = Math.round(100 * ((float) prefLimitSpeedPercent) / 100);
            }
            //get speed steps from prefs
            speedStepPref = threaded_application.getIntPrefValue(prefs, "DisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
            setDisplayUnitScale(throttleIndex);

            setDisplayedSpeed(throttleIndex, sbs[throttleIndex].getProgress());  // update numeric speeds since units might have changed
        }

        refreshMenu();

        vThrotScrWrap.invalidate();
        // Log.d("Engine_Driver","ending set_labels");
    }

    private void refreshMenu() {
        //adjust several items in the menu
        if (TMenu != null) {
            mainapp.actionBarIconCountThrottle = 0;
            mainapp.displayEStop(TMenu);
            mainapp.setPowerStateButton(TMenu);
            mainapp.displayPowerStateMenuButton(TMenu);
            mainapp.setPowerMenuOption(TMenu);
            mainapp.setDCCEXMenuOption(TMenu);
            mainapp.setWithrottleCvProgrammerMenuOption(TMenu);
            showHideConsistMenus();
            mainapp.setWebMenuOption(TMenu);
            mainapp.setRoutesMenuOption(TMenu);
            mainapp.setTurnoutsMenuOption(TMenu);
            mainapp.setGamepadTestMenuOption(TMenu, mainapp.gamepadCount);
            mainapp.setKidsMenuOptions(TMenu, prefKidsTimer.equals(PREF_KIDS_TIMER_NONE), mainapp.gamepadCount);
            mainapp.setFlashlightButton(TMenu);
            mainapp.displayFlashlightMenuButton(TMenu);
            mainapp.displayTimerMenuButton(TMenu, kidsTimerRunning);
            mainapp.displayThrottleSwitchMenuButton(TMenu);
            mainapp.displayWebViewMenuButton(TMenu);
            displayEsuMc2KnobMenuButton(TMenu);
            mainapp.displayDeviceSoundsThrottleButton(TMenu);
            mainapp.displayDccExButton(TMenu);
//            mainapp.displayMenuSeparator(TMenu, this, mainapp.actionBarIconCountThrottle);
        }
    }

    @Override
    public boolean onKeyUp(int key, KeyEvent event) {

        // Handle pressing of the back button
        if (key == KeyEvent.KEYCODE_BACK) {
            return true; // stop processing this key
        }

        if ((key == KEYCODE_VOLUME_UP) || (key == KEYCODE_VOLUME_DOWN)) {
//            mVolumeKeysAutoIncrement = false;
//            mVolumeKeysAutoDecrement = false;
            doVolumeButtonAction(event.getAction(), key, 0);
            return (true); // stop processing this key
        } else if ((key == KEYCODE_VOLUME_MUTE) || (key == KEYCODE_HEADSETHOOK)) {
            doMuteButtonAction(event.getAction(), key, 0);
            return (true); // stop processing this key
        }
        return (super.onKeyUp(key, event)); // continue with normal key
        // processing
    }


    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        int repeatCnt = event.getRepeatCount();

        // Handle pressing of the back button
        if (key == KEYCODE_BACK) {
            if (!isScreenLocked) {
                if (webViewIsOn && webView.canGoBack() && !clearHistory) {
                    webView.goBack();
                    webView.setInitialScale((int) (100 * scale)); // restore scale
                    return (true);
                } else {
                    if (webView != null) {
                        setImmersiveModeOn(webView, false);
                    }
                    if (mainapp.throttle_msg_handler != null) {
                        mainapp.checkExit(this);
                    } else { // something has gone wrong and the activity did not shut down properly so force it
                        disconnect();
                    }
                    return (true); // stop processing this key
                }
            } else {
//                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastShakeScreenLockedActionNotAllowed), Toast.LENGTH_SHORT).show();
                threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastShakeScreenLockedActionNotAllowed), Toast.LENGTH_SHORT);
            }
        } else if ((key == KEYCODE_VOLUME_UP) || (key == KEYCODE_VOLUME_DOWN)) {  // use volume to change speed for specified loco
//            if (!prefDisableVolumeKeys) {  // ignore the volume keys if the preference its set
//                for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
//                    if ( throttleIndex == whichVolume && (mainapp.consists != null) && (mainapp.consists[throttleIndex] != null)
//                          && (mainapp.consists[throttleIndex].isActive()) ) {
//                        if (key == KEYCODE_VOLUME_UP) {
//                            if (repeatCnt == 0) {
//                                mVolumeKeysAutoIncrement = true;
//                                volumeKeysRepeatUpdateHandler.post(new VolumeKeysRptUpdater(throttleIndex));
//                            }
//                        } else {
//                            if (repeatCnt == 0) {
//                                mVolumeKeysAutoDecrement = true;
//                                volumeKeysRepeatUpdateHandler.post(new VolumeKeysRptUpdater(throttleIndex));
//                            }
//                        }
//                    }
//                }
//            }
            doVolumeButtonAction(event.getAction(), key, repeatCnt);
            mainapp.exitDoubleBackButtonInitiated = 0;
            return (true); // stop processing this key
        } else if ((key == KEYCODE_VOLUME_MUTE) || (key == KEYCODE_HEADSETHOOK)) {
            doMuteButtonAction(event.getAction(), key, 0);
            mainapp.exitDoubleBackButtonInitiated = 0;
            return (true); // stop processing this key
        }
        mainapp.exitDoubleBackButtonInitiated = 0;
        return (super.onKeyDown(key, event)); // continue with normal key
        // processing
    }

    void doMuteButtonAction(int action, int key, int repeatCnt) {
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
                for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
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
        for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
            if (getConsist(throttleIndex).isActive()) {
                release_loco(throttleIndex);
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

    // request release of specified throttle
    void release_loco(int whichThrottle) {
        mainapp.storeThrottleLocosForReleaseDCCEX(whichThrottle);
        mainapp.consists[whichThrottle].release();
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", whichThrottle); // pass T, S or G in message
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.throttle_menu, menu);
        TMenu = menu;
        mainapp.actionBarIconCountThrottle = 0;
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.displayTimerMenuButton(menu, kidsTimerRunning);
        mainapp.setPowerMenuOption(menu);
        mainapp.setDCCEXMenuOption(menu);
        mainapp.setWithrottleCvProgrammerMenuOption(menu);
        showHideConsistMenus();
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);
        mainapp.setFlashlightButton(menu);
        mainapp.displayThrottleSwitchMenuButton(menu);
        mainapp.displayWebViewMenuButton(menu);
        if (IS_ESU_MCII) {
            displayEsuMc2KnobMenuButton(menu);
        }
        mainapp.displayDeviceSoundsThrottleButton(TMenu);
        mainapp.displayDccExButton(TMenu);

//        mainapp.adjustToolbarButtonSpacing(toolbar);

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint({"NonConstantResourceId", "ApplySharedPref"})
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (webView != null) {
            setImmersiveModeOn(webView, false);
        }

        // Handle all of the possible menu actions.
        Intent in;
        if (item.getItemId() == R.id.turnouts_mnu) {
            in = new Intent().setClass(this, turnouts.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if (item.getItemId() == R.id.routes_mnu) {
            in = new Intent().setClass(this, routes.class);
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
            in = new Intent().setClass(this, SettingsActivity.class);
            startActivityForResult(in, 0);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
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

        } else if (item.getItemId() == R.id.EmerStop) {
            mainapp.sendEStopMsg();
            speedUpdate(0);  // update all throttles
            applySpeedRelatedOptions();  // update all throttles
            if (IS_ESU_MCII) {
                Log.d("Engine_Driver", "ESU_MCII: Move knob request for EStop");
                setEsuThrottleKnobPosition(whichVolume, 0);
            }
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(TMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;

        } else if (item.getItemId() == R.id.EditConsist0_menu) {
            Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);
            consistEdit.putExtra("whichThrottle", '0');
            startActivityForResult(consistEdit, sub_activity_type.CONSIST);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.EditConsist1_menu) {
            Intent consistEdit2 = new Intent().setClass(this, ConsistEdit.class);
            consistEdit2.putExtra("whichThrottle", '1');
            startActivityForResult(consistEdit2, sub_activity_type.CONSIST);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.EditConsist2_menu) {
            Intent consistEdit3 = new Intent().setClass(this, ConsistEdit.class);
            consistEdit3.putExtra("whichThrottle", '2');
            startActivityForResult(consistEdit3, sub_activity_type.CONSIST);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.EditLightsConsist0_menu) {
            Intent consistLightsEdit = new Intent().setClass(this, ConsistLightsEdit.class);
            consistLightsEdit.putExtra("whichThrottle", '0');
            startActivityForResult(consistLightsEdit, sub_activity_type.CONSIST_LIGHTS);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.EditLightsConsist1_menu) {
            Intent consistLightsEdit2 = new Intent().setClass(this, ConsistLightsEdit.class);
            consistLightsEdit2.putExtra("whichThrottle", '1');
            startActivityForResult(consistLightsEdit2, sub_activity_type.CONSIST_LIGHTS);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.EditLightsConsist2_menu) {
            Intent consistLightsEdit3 = new Intent().setClass(this, ConsistLightsEdit.class);
            consistLightsEdit3.putExtra("whichThrottle", '2');
            startActivityForResult(consistLightsEdit3, sub_activity_type.CONSIST_LIGHTS);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        } else if (item.getItemId() == R.id.gamepad_test_reset) {
            mainapp.gamepadFullReset();
            mainapp.setGamepadTestMenuOption(TMenu, mainapp.gamepadCount);
            setGamepadIndicator();
            tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST_RESET);
            return true;
        } else if (item.getItemId() == R.id.gamepad_test_mnu1) {
            in = new Intent().setClass(this, gamepad_test.class);
            in.putExtra("whichGamepadNo", "0");
            startActivityForResult(in, sub_activity_type.GAMEPAD_TEST);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.gamepad_test_mnu2) {
            in = new Intent().setClass(this, gamepad_test.class);
            in.putExtra("whichGamepadNo", "1");
            startActivityForResult(in, sub_activity_type.GAMEPAD_TEST);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.gamepad_test_mnu3) {
            in = new Intent().setClass(this, gamepad_test.class);
            in.putExtra("whichGamepadNo", "2");
            startActivityForResult(in, sub_activity_type.GAMEPAD_TEST);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        } else if (item.getItemId() == R.id.timer_mnu) {
            showTimerPasswordDialog();
            return true;
        } else if (item.getItemId() == R.id.timer_button) {
            prefKidsTime = Integer.parseInt(prefKidsTimerButtonDefault) * 60000;
            prefKidsTimer = prefKidsTimerButtonDefault;
            prefs.edit().putString("prefKidsTimer", prefKidsTimerButtonDefault).commit();
            kidsTimerActions(kids_timer_action_type.ENABLED, 0);
            return true;

        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlight(this, TMenu);
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.throttle_switch_button) {
            switchThrottleScreenType();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.web_view_button) {
            showHideWebView("");
            return true;
        } else if (item.getItemId() == R.id.EsuMc2Knob_button) {
            toggleEsuMc2Knob(this, TMenu);
            return true;

        } else if ((item.getItemId() == R.id.device_sounds_button) || (item.getItemId() == R.id.device_sounds_menu)) {
            if (item.getItemId() == R.id.device_sounds_button) mainapp.buttonVibration();
            in = new Intent().setClass(this, device_sounds_settings.class);
            startActivityForResult(in, sub_activity_type.DEVICE_SOUNDS_SETTINGS);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // handle return from menu items
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case sub_activity_type.SELECT_LOCO:
                if (resultCode == select_loco.RESULT_LOCO_EDIT) {
                    activityConsistUpdate(resultCode, data.getExtras());
                }
                try {
                    overrideThrottleNames[mainapp.throttleCharToInt(data.getCharExtra("whichThrottle", ' '))] = data.getStringExtra("overrideThrottleName");
                } catch (RuntimeException e) {
                    Log.e("Engine_Driver", "Throttle: Call to OverrideThrottleName failed. Runtime Exception, OS " + android.os.Build.VERSION.SDK_INT + " Message: " + e);
                }
                if ((getConsist(whichVolume) != null) && (!getConsist(whichVolume).isActive())) {
                    setNextActiveThrottle(); // if consist on Volume throttle was released, move to next throttle
                } else {
                    if (IS_ESU_MCII) {
                        esuMc2Led.setState(EsuMc2Led.GREEN, EsuMc2LedState.STEADY_FLASH, true);
                    }
                    setActiveThrottle(whichVolume);
                }
                for (int i = 0; i < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES; i++) {
                    if (mainapp.consists != null && mainapp.consists[i] != null && !mainapp.consists[i].isActive()) {
                        soundsStopAllSoundsForLoco(i);
                    }
//                    showHideMuteButton(i);
                    soundsShowHideDeviceSoundsButton(i);
                }
                break;
            case sub_activity_type.CONSIST:         // edit loco or edit consist
                if (resultCode == ConsistEdit.RESULT_CON_EDIT)
                    activityConsistUpdate(resultCode, data.getExtras());
                break;
            case sub_activity_type.CONSIST_LIGHTS:         // edit consist lights
                break;   // nothing to do
            case sub_activity_type.PREFS: {    // edit prefs
                if (resultCode == RESULT_GAMEPAD) { // gamepad pref changed
                    // update tone generator volume
                    if (tg != null) {
                        tg.release();
                        try {
                            tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                                    threaded_application.getIntPrefValue(prefs, "prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));
                        } catch (RuntimeException e) {
                            Log.e("Engine_Driver", "new ToneGenerator failed. Runtime Exception, OS " + android.os.Build.VERSION.SDK_INT + " Message: " + e);
                        }
                    }
                    // update GamePad Support
                    setGamepadKeys();
                }
                if (resultCode == RESULT_ESUMCII) { // ESU MCII pref change
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
                soundsShowHideAllMuteButtons();
                break;
            }
            case sub_activity_type.GAMEPAD_TEST: {
                if (data != null) {
                    String whichGamepadNo = data.getExtras().getString("whichGamepadNo");
                    if (whichGamepadNo != null) {
                        int result = Integer.valueOf(whichGamepadNo.substring(1, 2));
                        int gamepadNo = Integer.valueOf(whichGamepadNo.substring(0, 1));
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
                                mainapp.setGamepadTestMenuOption(TMenu, mainapp.gamepadCount);
                                setGamepadIndicator();
                                tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST_RESET);
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
            case sub_activity_type.DEVICE_SOUNDS_SETTINGS: {
                loadSounds();
                soundsShowHideAllMuteButtons();
                break;
            }
        }

        // loop through all function buttons and
        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        set_labels();
        soundsShowHideAllMuteButtons();
    }

    private void activityConsistUpdate(int resultCode, Bundle extras) {
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
//        Log.d("Engine_Driver", "onTouchEvent action: " + event.getAction());
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
//        Log.d("Engine_Driver", "dispatchTouchEvent:");
        // if screen is locked
        if (isScreenLocked) {
            // check if we have a swipe up
            if (ev.getAction() == ACTION_DOWN) {
//                Log.d("Engine_Driver", "dispatchTouchEvent: ACTION_DOWN" );
                gestureStart(ev);
            }
            if (ev.getAction() == ACTION_UP) {
//                Log.d("Engine_Driver", "dispatchTouchEvent: ACTION_UP" );
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
//        Log.d("Engine_Driver", "gestureStart x=" + gestureStartX + " y=" + gestureStartY);

        toolbarHeight = mainapp.getToolbarHeight(toolbar, statusLine, screenNameLine);

        // check if the sliders are already hidden by preference
        if (!prefs.getBoolean("hide_slider_preference", false)) {
            // if gesture is attempting to start over an enabled slider, ignore it and return immediately.
            for (int throttleIndex = 0; throttleIndex < mainapp.numThrottles; throttleIndex++) {
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
//                    Log.d("Engine_Driver","exiting gestureStart on slider: " + gestureStartX + ", " + gestureStartY);
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
//            Log.d("Engine_Driver","gestureStart start gesture timer");
            mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
        } else {
            Log.d("Engine_Driver", "gestureStart Can't start gesture timer");
        }
    }

    public void gestureMove(MotionEvent event) {
//        Log.d("Engine_Driver", "gestureMove action " + event.getAction() + " eventTime: " + event.getEventTime() );
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
//                Log.d("Engine_Driver", "gestureMove gestureVelocity vel " + velocityX);
                if ((Math.abs(velocityX) < threaded_application.min_fling_velocity) && (Math.abs(velocityY) < threaded_application.min_fling_velocity)) {
                    gestureFailed(event);
                }
            }
//            else {
//                Log.d("Engine_Driver", "gestureMove event.getEventTime(): " +event.getEventTime()   + " gestureLastCheckTime: " + gestureLastCheckTime + " gestureCheckRate: " + gestureCheckRate);
//                Log.d("Engine_Driver", "gestureMove event.getEventTime() - gestureLastCheckTime: " + (event.getEventTime() - gestureLastCheckTime) + " gestureCheckRate: " + gestureCheckRate);
//            }
            if (gestureInProgress) {
                // restart the gesture timeout timer
//                Log.d("Engine_Driver","gestureSMove restart gesture timer");
                if (mainapp.throttle_msg_handler != null)
                    mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
//        Log.d("Engine_Driver", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
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
                        case swipe_up_down_option_type.IMMERSIVE:
                            if (immersiveModeIsOn) {
                                setImmersiveModeOff(webView, false);
//                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastImmersiveModeDisabled), Toast.LENGTH_SHORT).show();
                                threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastImmersiveModeDisabled), Toast.LENGTH_SHORT);
                            } else {
                                setImmersiveModeOn(webView, false);
                            }
                            break;
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
//        Log.d("Engine_Driver", "gestureEnd gestureCancel");
        if (mainapp.throttle_msg_handler != null)
            mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);
        gestureInProgress = false;
        gestureFailed = true;
    }

    void gestureFailed(MotionEvent event) {
//        Log.d("Engine_Driver", "gestureEnd gestureFailed");
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
//            Log.d("Engine_Driver", "gestureStopped");
            if (gestureInProgress) {
                // end the gesture
                gestureInProgress = false;
                gestureFailed = true;
                // create a MOVE event to trigger the underlying control
                if (vThrotScr != null) {
//                    Log.d("Engine_Driver", "gestureStopped vThrotScr != null");
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
        if (mainapp.numThrottles > mainapp.maxThrottlesCurrentScreen) {   // Maximum number of throttles this screen supports
            mainapp.numThrottles = mainapp.maxThrottlesCurrentScreen;
        }
    }


    private void showTimerPasswordDialog() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getApplicationContext().getResources().getString(R.string.timerDialogTitle));
        alert.setMessage(getApplicationContext().getResources().getString(R.string.timerDialogMessage));

// Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getApplicationContext().getResources().getString(R.string.timerDialogHint));
        alert.setView(input);

        alert.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @SuppressLint("ApplySharedPref")
            public void onClick(DialogInterface dialog, int whichButton) {
                mainapp.exitDoubleBackButtonInitiated = 0;
                passwordText = input.getText().toString();
                Log.d("", "Password Value : " + passwordText);

                if (passwordText.equals(prefKidsTimerResetPassword)) { //reset
                    kidsTimerActions(kids_timer_action_type.ENDED, 0);
                    kidsTimerActions(kids_timer_action_type.DISABLED, 0);
                    prefs.edit().putString("prefKidsTimer", PREF_KIDS_TIMER_NONE).commit();  //reset the preference
                    getKidsTimerPrefs();
                }

                if (passwordText.equals(prefKidsTimerRestartPassword)) { //reset
                    kidsTimerActions(kids_timer_action_type.ENABLED, 0);
                }

                mainapp.hideSoftKeyboardAfterDialog();
                mainapp.buttonVibration();
            }
        });

        alert.setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mainapp.exitDoubleBackButtonInitiated = 0;
                        mainapp.hideSoftKeyboardAfterDialog();
                        mainapp.buttonVibration();
//                        return;
                    }
                });
        alert.show();

    } // end showTimerPasswordDialog

    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@RequestCodes int requestCode) {
        Log.d("Engine_Driver", "throttle: navigateToHandler:" + requestCode);
        if (!PermissionsHelper.getInstance().isPermissionGranted(throttle.this, requestCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(throttle.this, requestCode);
            }
        } else {
            // Go to the correct handler based on the request code.
            // Only need to consider relevant request codes initiated by this Activity
            //noinspection SwitchStatementWithTooFewBranches
            switch (requestCode) {
//                case PermissionsHelper.READ_SERVER_AUTO_PREFERENCES:
//                    Log.d("Engine_Driver", "Got permission for READ_SERVER_AUTO_PREFERENCES");
//                    autoImportUrlAskToImportImpl();
//                    break;
//                case PermissionsHelper.STORE_SERVER_AUTO_PREFERENCES:
//                    Log.d("Engine_Driver", "Got permission for STORE_SERVER_AUTO_PREFERENCES");
//                    autoImportFromURLImpl();
//                    break;
                default:
                    // do nothing
                    Log.d("Engine_Driver", "Unrecognised permissions request code: " + requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(throttle.this, requestCode, permissions, grantResults)) {
            Log.d("Engine_Driver", "Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void autoImportFromURL() {

        new AutoImportFromURL();
    }

    public class AutoImportFromURL  implements Runnable{   // Background Async Task to download file

        public AutoImportFromURL() {
            new Thread(this).start();
        }

        // Importing file in background thread
        @SuppressLint("ApplySharedPref")
        public void run() {
            Log.d("Engine_Driver", "throttle: Import preferences from Server: start");
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
                urlPreferencesFilePath = context.getExternalFilesDir(null) + "/" + urlPreferencesFileName;
                Log.d("Engine_Driver", "throttle: Import preferences from Server: linkg for: " + urlPreferencesFilePath);
                url = new URL(n_url);

                connection = url.openConnection();
                connection.connect();

            } catch (Exception e) {
                Log.d("Engine_Driver", "throttle: Auto import preferences from Server Failed: " + e.getMessage());
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
                        Log.d("Engine_Driver", "throttle: Auto Import preferences from Server: Local file is up-to-date: " + localFile);
                        return;
//                    } else {
//                        Log.d("Engine_Driver", "throttle: Import preferences from Server: Local file is newer. Date " + localDate.toString());
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
                Log.e("Engine_Driver", "throttle: Auto import preferences from Server Failed: " + e.getMessage());
            }

            Log.d("Engine_Driver", "throttle: Auto Import preferences from Server: End");
            return;
        }

    }


    private void autoImportUrlAskToImport() {
//        navigateToHandler(PermissionsHelper.READ_SERVER_AUTO_PREFERENCES);
//        autoImportUrlAskToImportImpl();
//    }
//
//    private void autoImportUrlAskToImportImpl() {
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
        Log.d("Engine_Driver", "Preferences: Loading saved preferences from file: " + exportedPreferencesFileName);
        boolean result = importExportPreferences.loadSharedPreferencesFromFile(getApplicationContext(), sharedPreferences, exportedPreferencesFileName, deviceId, false);

        if (!result) {
//            Toast.makeText(getApplicationContext(),
//                    getApplicationContext().getResources().getString(R.string.prefImportExportErrorReadingFrom, exportedPreferencesFileName),
//                    Toast.LENGTH_LONG).show();
            threaded_application.safeToast(String.format(getApplicationContext().getResources().getString(R.string.prefImportExportErrorReadingFrom), exportedPreferencesFileName), Toast.LENGTH_LONG);
        }
        forceRestartApp(forceRestartReason);
    }

    @SuppressLint("ApplySharedPref")
    public void forceRestartApp(int forcedRestartReason) {
        Log.d("Engine_Driver", "throttle.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
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
        boolean prefThrottleSwitchButtonCycleAll = prefs.getBoolean("prefThrottleSwitchButtonCycleAll", getApplicationContext().getResources().getBoolean(R.bool.prefThrottleSwitchButtonCycleAllDefaultValue));
        String prefThrottleSwitchOption1 = prefs.getString("prefThrottleSwitchOption1", getApplicationContext().getResources().getString(R.string.prefThrottleSwitchOption1DefaultValue));
        String prefThrottleSwitchOption2 = prefs.getString("prefThrottleSwitchOption2", getApplicationContext().getResources().getString(R.string.prefThrottleSwitchOption2DefaultValue));

        if (!webViewLocation.equals(keepWebViewLocation)) {
            showHideWebView("");
        }

        if (!prefThrottleSwitchButtonCycleAll) {
            if (prefThrottleScreenType.equals(prefThrottleSwitchOption1)) {
                prefs.edit().putString("prefThrottleScreenType", prefThrottleSwitchOption2).commit();
            } else {
                prefs.edit().putString("prefThrottleScreenType", prefThrottleSwitchOption1).commit();
            }
        } else {
            prefs.edit().putString("prefThrottleScreenType", mainapp.getNextThrottleLayout()).commit();
        }
        prefs.edit().putString("WebViewLocation", webViewLocation).commit();
        fixNumThrottles();
        forceRestartApp(restart_reason_type.THROTTLE_SWITCH);
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
        int numThrottles = mainapp.Numeralise(prefs.getString("NumThrottle", getResources().getString(R.string.NumThrottleDefaultValue)));
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
//            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastNumThrottles, textNumbers[max[index] - 1]), Toast.LENGTH_LONG).show();
            threaded_application.safeToast(String.format(getApplicationContext().getResources().getString(R.string.toastNumThrottles), textNumbers[max[index] - 1]), Toast.LENGTH_LONG);
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

    void soundsStopAllSoundsForLoco(int whichThrottle) {
//        Log.d("Engine_Driver", "soundsStopAllSoundsForLoco : (locoSound) wt: " + whichThrottle);
        if (mainapp.soundPool != null) {
            for (int i = 0; i < 17; i++) {
                mainapp.soundPool.stop(mainapp.soundsLocoStreamId[whichThrottle][i]);
            }
            mainapp.soundsLocoQueue[whichThrottle].emptyQueue();
            for (int soundType = 0; soundType < 3; soundType++) {
                for (int i = 0; i < 3; i++) {
                    mainapp.soundPool.stop(mainapp.soundsExtrasStreamId[soundType][whichThrottle][i]);
                }
            }
            mainapp.soundsLocoCurrentlyPlaying[whichThrottle] = -1;
        }
    } // end soundsStopAllSoundsForLoco

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
            soundsMuteUnmuteCurrentSounds(whichThrottle);
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
        Log.d("Engine_Driver", "handleDeviceButtonAction: handleAction - action: " + action);

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

    float soundsVolume(int soundType, int whichThrottle) {
        float volume = 0;
        if ((whichThrottle < mainapp.maxThrottlesCurrentScreen) && (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES)) {
            if (!soundsIsMuted[whichThrottle]) {
                switch (soundType) {
                    case sounds_type.BELL: // bell
                        volume = mainapp.prefDeviceSoundsBellVolume;
                        break;
                    case sounds_type.HORN: // horn
                    case sounds_type.HORN_SHORT: // horn
                        volume = mainapp.prefDeviceSoundsHornVolume;
                        break;
                    case sounds_type.LOCO: // loco
                    default:
                        volume = mainapp.prefDeviceSoundsLocoVolume;
                        break;
                }
            }
        }
        return volume;
    }

    void soundsMuteUnmuteCurrentSounds(int whichThrottle) {
        if (mainapp.soundPool != null) {
            if (whichThrottle < threaded_application.SOUND_MAX_SUPPORTED_THROTTLES) {
                int mSound = mainapp.soundsLocoCurrentlyPlaying[whichThrottle];
                if (mSound >= 0) {
                    mainapp.soundPool.setVolume(mainapp.soundsLocoStreamId[whichThrottle][mSound],
                            soundsVolume(sounds_type.LOCO, whichThrottle),
                            soundsVolume(sounds_type.LOCO, whichThrottle));
                }
                for (int soundType = 0; soundType < 2; soundType++) {  // don't worry about the short horn
                    if (mainapp.soundsExtrasCurrentlyPlaying[soundType][whichThrottle] == sounds_type.BELL_HORN_LOOP) {
                        mainapp.soundPool.setVolume(mainapp.soundsExtrasStreamId[soundType][whichThrottle][sounds_type.BELL_HORN_LOOP],
                                soundsVolume(soundType + 1, whichThrottle),
                                soundsVolume(soundType + 1, whichThrottle));
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
        if (iplsLoader == null) iplsLoader = new InPhoneLocoSoundsLoader(mainapp, prefs, context);
        mainapp.soundsReloadSounds = true;
        iplsLoader.loadSounds();
        iplsLoader = null;
    }

    // common startActivity()
    // used for swipes for the main activities only - Throttle, Turnouts, Routes, Web
    protected void startACoreActivity(Activity activity, Intent in, boolean swipe, float deltaX) {
        if (activity != null && in != null) {
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ActivityOptions options;
            if (deltaX > 0) {
                options = ActivityOptions.makeCustomAnimation(context, R.anim.push_right_in, R.anim.push_right_out);
            } else {
                options = ActivityOptions.makeCustomAnimation(context, R.anim.push_left_in, R.anim.push_left_out);
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
    void incrementBrakeSliderPosition(int whichThrottle) {}
    void decrementBrakeSliderPosition(int whichThrottle) {}
    void incrementLoadSliderPosition(int whichThrottle) {}
    void decrementLoadSliderPosition(int whichThrottle) {}
    protected int getTargetDirection(int whichThrottle) { return 0; }
    protected int getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(int whichThrottle) { return 0; }

        private class SemiRealisticGamepadRptUpdater implements Runnable {
        int whichThrottle;
        int stepMultiplier;

        private SemiRealisticGamepadRptUpdater(int WhichThrottle, int StepMultiplier) {
            whichThrottle = WhichThrottle;
            stepMultiplier = StepMultiplier;

            Log.d("Engine_Driver", "GamepadRptUpdater: WhichThrottle: " + whichThrottle
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
}