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

import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.LinkedHashMap;

import jmri.enginedriver.type.auto_increment_or_decrement_type;
import jmri.enginedriver.type.direction_type;
import jmri.enginedriver.type.max_throttles_current_screen_type;
import jmri.enginedriver.type.speed_button_type;
import jmri.enginedriver.type.tick_type;
import jmri.enginedriver.util.VerticalSeekBar;
import jmri.enginedriver.type.button_press_message_type;
import jmri.enginedriver.type.consist_function_latching_type;
import jmri.enginedriver.type.slider_type;
import jmri.enginedriver.type.web_view_location_type;
import jmri.enginedriver.type.speed_commands_from_type;
import jmri.enginedriver.type.stop_button_type;

public class throttle_semi_realistic extends throttle {
    static final String activityName = "throttle_semi_realistic";

    protected static final int MAX_SCREEN_THROTTLES = max_throttles_current_screen_type.SEMI_REALISTIC;

    private LinearLayout[] lThrottles;
    private LinearLayout[] lUppers;
    private LinearLayout[] lLowers;
    private ScrollView[] svFnBtns;

//    private static final int SLIDER_PURPOSE_THROTTLE = 0;
    private static final int SLIDER_PURPOSE_BRAKE = 1;
    private static final int SLIDER_PURPOSE_LOAD = 2;
    private static final int SLIDER_PURPOSE_SEMI_REALISTIC_THROTTLE = 3;

//    private final int[] throttleSwitchingMax = {0, 0, 0, 0, 0, 0};

    private static final int GESTURE_NOT_STARTED = 0;
    private static final int GESTURE_STARTED     = 1;
    private static final int GESTURE_IN_PROGRESS = 2;
    private static final int GESTURE_FINISHED    = 3;
    int semiRealisticThrottleGestureState = 0; //0=not in progress, 1=in progress 2=finished

    int maxThrottlePcnt = 100;
    int maxThrottle = 126;

    int prefSemiRealisticThrottleSpeedStep = 1;
    int prefDisplaySemiRealisticThrottleNotches = 8;
    int prefSemiRealisticThrottleNumberOfBrakeSteps = 5;
    int prefSemiRealisticThrottleAirRefreshRate = 250;
    int prefSemiRealisticThrottleNumberOfLoadSteps = 5;
    int prefSemiRealisticThrottleMaxLoadPcnt = 1000;
    int prefSemiRealisticMaximumBrakePcnt = 70;
    boolean prefSemiRealisticThrottleDisableAir = true;
    boolean airEnabled = true;
    int prefSemiRealisticThrottleAcceleratonRepeat = 300;
    int prefSemiRealisticThrottleDecelerationRepeat = 600;
    boolean useNotches = false;
    Button [] bEStops;
    Button [] bAirs;
    double maxLoad = 1000;
    double maxBrake = 70;
    double maxBrakeUnderPower = 70;

    String prefDisplaySemiRealisticThrottleDecoderBrakeType = "none";
    String prefSemiRealisticThrottleStopButtonAction = "tz";
    int prefSemiRealisticThrottleDecoderBrakeTypeLowFunctionEsu = 4;
    int prefSemiRealisticThrottleDecoderBrakeTypeMidFunctionEsu = 5;
    int prefSemiRealisticThrottleDecoderBrakeTypeHighFunctionEsu = 6;
    int prefSemiRealisticThrottleDecoderBrakeTypeLowValueEsu = 30;
    int prefSemiRealisticThrottleDecoderBrakeTypeMidValueEsu = 60;
    int prefSemiRealisticThrottleDecoderBrakeTypeHighValueEsu = 100;

    boolean useEsuDecoderBrakes = false;
    int [][] esuBrakeFunctions = {{4,0,0,0,0}, {5,0,0,0,0}, {6,0,0,0,0} };
    int [] esuBrakeLevels = {30, 60, 100};
    boolean [] esuBrakeActive = {false, false, false};


    protected void removeLoco(int whichThrottle) {
        super.removeLoco(whichThrottle);
        set_function_labels_and_listeners_for_view(whichThrottle);
    }

    @Override
    protected void getCommonPrefs(boolean isCreate) {
        super.getCommonPrefs(isCreate);

        isSemiRealisticThrottle = true;

        maxThrottlePcnt = threaded_application.getIntPrefValue(prefs, "prefMaximumThrottle", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (0.01 * maxThrottlePcnt)); // convert from percent
        prefSemiRealisticThrottleSpeedStep = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleSpeedStep", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleSpeedStepDefaultValue));
        prefDisplaySemiRealisticThrottleNotches = threaded_application.getIntPrefValue(prefs, "prefDisplaySemiRealisticThrottleNotches", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleNotchesDefaultValue));
        useNotches = (prefDisplaySemiRealisticThrottleNotches!=100);
        prefSpeedButtonsSpeedStep = threaded_application.getIntPrefValue(prefs, "prefSpeedButtonsSpeedStep", "4");
        REP_DELAY = threaded_application.getIntPrefValue(prefs,"prefSpeedButtonsRepeat", "100");

        prefSemiRealisticThrottleNumberOfBrakeSteps = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleNumberOfBrakeSteps", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleNumberOfBrakeStepsDefaultValue));
        prefSemiRealisticMaximumBrakePcnt = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticMaximumBrakePcnt", getApplicationContext().getResources().getString(R.string.prefSemiRealisticMaximumBrakePcntDefaultValue));
        maxBrake = (double) prefSemiRealisticMaximumBrakePcnt / 100;
        maxBrakeUnderPower = ((double) prefSemiRealisticMaximumBrakePcnt / 100) - 0.2;

        prefSemiRealisticThrottleAirRefreshRate = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleAirRefreshRate", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleAirRefreshRateDefaultValue));
        prefSemiRealisticThrottleDisableAir = prefs.getBoolean("prefSemiRealisticThrottleDisableAir", getResources().getBoolean(R.bool.prefSemiRealisticThrottleDisableAirDefaultValue));


        prefSemiRealisticThrottleNumberOfLoadSteps = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleNumberOfLoadSteps", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleNumberOfLoadStepsDefaultValue));
        prefSemiRealisticThrottleMaxLoadPcnt = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleMaxLoadPcnt", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleMaxLoadPcntDefaultValue));
        maxLoad = prefSemiRealisticThrottleMaxLoadPcnt;

        prefSemiRealisticThrottleAcceleratonRepeat = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleAcceleratonRepeat", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleAccelerationRepeatDefaultValue));
        prefSemiRealisticThrottleDecelerationRepeat = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleDecelerationRepeat", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecelerationRepeatDefaultValue));

        prefDisplaySemiRealisticThrottleDecoderBrakeType = prefs.getString("prefDisplaySemiRealisticThrottleDecoderBrakeType", getApplicationContext().getResources().getString(R.string.prefDisplaySemiRealisticThrottleDecoderBrakeTypeDefaultValue));
        useEsuDecoderBrakes = (prefDisplaySemiRealisticThrottleDecoderBrakeType.equals("esu"));

        prefSemiRealisticThrottleStopButtonAction = prefs.getString("prefSemiRealisticThrottleStopButtonAction", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleStopButtonActionDefaultValue));

        String tmp = prefs.getString("prefSemiRealisticThrottleDecoderBrakeTypeLowFunctionEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeLowFunctionEsuDefaultValue));
        String[] prefValues = threaded_application.splitByString(tmp, " ");
        for (int i=0; i<5; i++) {
            if (i<prefValues.length) esuBrakeFunctions[0][i] = Integer.parseInt(prefValues[i]);
            else esuBrakeFunctions[0][i] = -1;
        }
        tmp = prefs.getString("prefSemiRealisticThrottleDecoderBrakeTypeMidFunctionEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeMidFunctionEsuDefaultValue));
        prefValues = threaded_application.splitByString(tmp, " ");
        for (int i=0; i<5; i++) {
            if (i<prefValues.length) esuBrakeFunctions[1][i] = Integer.parseInt(prefValues[i]);
            else esuBrakeFunctions[1][i] = -1;
        }
        tmp = prefs.getString("prefSemiRealisticThrottleDecoderBrakeTypeHighFunctionEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeHighFunctionEsuDefaultValue));
        prefValues = threaded_application.splitByString(tmp, " ");
        for (int i=0; i<5; i++) {
            if (i<prefValues.length) esuBrakeFunctions[2][i] = Integer.parseInt(prefValues[i]);
            else esuBrakeFunctions[2][i] = -1;
        }

        prefSemiRealisticThrottleDecoderBrakeTypeLowValueEsu = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleDecoderBrakeTypeLowValueEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeLowValueEsuDefaultValue));
        prefSemiRealisticThrottleDecoderBrakeTypeMidValueEsu = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleDecoderBrakeTypeMidValueEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeMidValueEsuDefaultValue));
        prefSemiRealisticThrottleDecoderBrakeTypeHighValueEsu = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleDecoderBrakeTypeHighValueEsu", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleDecoderBrakeTypeHighValueEsuDefaultValue));
        esuBrakeLevels[0] = prefSemiRealisticThrottleDecoderBrakeTypeLowFunctionEsu>=0 ? prefSemiRealisticThrottleDecoderBrakeTypeLowValueEsu : 10000;
        esuBrakeLevels[1] = prefSemiRealisticThrottleDecoderBrakeTypeMidFunctionEsu>=0 ? prefSemiRealisticThrottleDecoderBrakeTypeMidValueEsu : 10000;
        esuBrakeLevels[2] = prefSemiRealisticThrottleDecoderBrakeTypeHighFunctionEsu>=0 ? prefSemiRealisticThrottleDecoderBrakeTypeHighValueEsu : 10000;

    }

    protected void setScreenDetails() {
        mainapp.currentScreenSupportsWebView = false;
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;

        mainapp.throttleLayoutViewId = R.layout.throttle_semi_realistic_left;
    } // end setScreen()

    @SuppressLint({"Recycle", "SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(threaded_application.applicationName, activityName + ": onCreate(): called");

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        mainapp.throttleSwitchAllowed = false; // used to prevent throttle switches until the previous onStart() completes

        setScreenDetails();

        super.onCreate(savedInstanceState);

    } // end of onCreate()

    @Override
    public void onStart() {
        Log.d(threaded_application.applicationName, activityName + ": onStart(): called");
        if (mainapp.appIsFinishing) return;

        sliderType = slider_type.VERTICAL;

        if(mainapp.throttleSwitchWasRequestedOrReinitialiseRequired) {
            setScreenDetails();
        }
        super.onStart();

    } // end onStart()

    @Override
    public void onPause() {
        super.onPause();
        threaded_application.activityPaused(activityName);
    }

    @Override
    public void onResume() {
        Log.d(threaded_application.applicationName, activityName + ": onResume(): called");
        super.onResume();
        threaded_application.activityResumed(activityName);

        if (mainapp.appIsFinishing) { return; }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            if (throttleIndex < mainapp.numThrottles) {
                lThrottles[throttleIndex].setVisibility(LinearLayout.VISIBLE);
            } else {
                lThrottles[throttleIndex].setVisibility(GONE);
            }
        }
    } // end of onResume()

    @SuppressLint("ClickableViewAccessibility")
    void initialiseUiElements() {
        super.initialiseUiElements();

        bTargetFwds = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetRevs = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetNeutrals = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetRSpds = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetLSpds = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetStops = new Button[mainapp.maxThrottlesCurrentScreen];
        bEStops = new Button[mainapp.maxThrottlesCurrentScreen];
        bAirs = new Button[mainapp.maxThrottlesCurrentScreen];
        airIndicators = new ImageView[mainapp.maxThrottlesCurrentScreen];
        airLineIndicators = new ImageView[mainapp.maxThrottlesCurrentScreen];

        TargetArrowSpeedButtonTouchListener targetArrowSpeedButtonTouchListener;
        AirButtonTouchListener airButtonTouchListener;

        TypedArray function_buttons_table_resource_ids = getResources().obtainTypedArray(R.array.function_buttons_table_resource_ids);
        TypedArray direction_indicator_forward_resource_ids = getResources().obtainTypedArray(R.array.direction_indicator_forward_resource_ids);
        TypedArray direction_indicator_reverse_resource_ids = getResources().obtainTypedArray(R.array.direction_indicator_reverse_resource_ids);
        TypedArray target_speed_value_label_resource_ids = getResources().obtainTypedArray(R.array.target_speed_value_label_resource_ids);
        TypedArray target_acceleration_value_label_resource_ids = getResources().obtainTypedArray(R.array.target_acceleration_value_label_resource_ids);
        TypedArray button_target_fwd_resource_ids = getResources().obtainTypedArray(R.array.button_target_fwd_resource_ids);
        TypedArray button_target_rev_resource_ids = getResources().obtainTypedArray(R.array.button_target_rev_resource_ids);
        TypedArray button_target_neutral_resource_ids = getResources().obtainTypedArray(R.array.button_target_neutral_resource_ids);
        TypedArray right_target_speed_button_resource_ids = getResources().obtainTypedArray(R.array.right_target_speed_button_resource_ids);
        TypedArray left_target_speed_button_resource_ids = getResources().obtainTypedArray(R.array.left_target_speed_button_resource_ids);
        TypedArray button_target_stop_resource_ids = getResources().obtainTypedArray(R.array.button_target_stop_resource_ids);
        TypedArray button_e_stop_resource_ids = getResources().obtainTypedArray(R.array.button_e_stop_resource_ids);
        TypedArray button_air_resource_ids = getResources().obtainTypedArray(R.array.button_air_resource_ids);
        TypedArray air_indicator_resource_ids = getResources().obtainTypedArray(R.array.air_indicator_resource_ids);
        TypedArray air_line_indicator_resource_ids = getResources().obtainTypedArray(R.array.air_line_indicator_resource_ids);

        TypedArray throttle_resource_ids = getResources().obtainTypedArray(R.array.throttle_resource_ids);
        TypedArray throttle_set_speed_resource_ids = getResources().obtainTypedArray(R.array.throttle_set_speed_resource_ids);
        TypedArray loco_upper_resource_ids = getResources().obtainTypedArray(R.array.loco_upper_resource_ids);
        TypedArray loco_lower_resource_ids = getResources().obtainTypedArray(R.array.loco_lower_resource_ids);
        TypedArray speed_resource_ids = getResources().obtainTypedArray(R.array.speed_resource_ids);
        TypedArray brake_resource_ids = getResources().obtainTypedArray(R.array.brake_resource_ids);
        TypedArray load_resource_ids = getResources().obtainTypedArray(R.array.load_resource_ids);
        TypedArray function_buttons_scroller_resource_ids = getResources().obtainTypedArray(R.array.function_buttons_scroller_resource_ids);
        TypedArray semi_realistic_speed_resource_ids = getResources().obtainTypedArray(R.array.semi_realistic_speed_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            functionButtonViewGroups[throttleIndex] = findViewById(function_buttons_table_resource_ids.getResourceId(throttleIndex,0));
            tvDirectionIndicatorForwards[throttleIndex] = findViewById(direction_indicator_forward_resource_ids.getResourceId(throttleIndex,0));
            tvDirectionIndicatorReverses[throttleIndex] = findViewById(direction_indicator_reverse_resource_ids.getResourceId(throttleIndex,0));
            tvTargetSpdVals[throttleIndex] = findViewById(target_speed_value_label_resource_ids.getResourceId(throttleIndex,0));
            tvTargetAccelerationVals[throttleIndex] = findViewById(target_acceleration_value_label_resource_ids.getResourceId(throttleIndex,0));

            bTargetFwds[throttleIndex] = findViewById(button_target_fwd_resource_ids.getResourceId(throttleIndex,0));
            bTargetRevs[throttleIndex] = findViewById(button_target_rev_resource_ids.getResourceId(throttleIndex,0));
            bTargetNeutrals[throttleIndex] = findViewById(button_target_neutral_resource_ids.getResourceId(throttleIndex,0));

            bTargetRSpds[throttleIndex] = findViewById(right_target_speed_button_resource_ids.getResourceId(throttleIndex,0));
            bTargetLSpds[throttleIndex] = findViewById(left_target_speed_button_resource_ids.getResourceId(throttleIndex,0));
            bTargetStops[throttleIndex] = findViewById(button_target_stop_resource_ids.getResourceId(throttleIndex,0));

            bEStops[throttleIndex] = findViewById(button_e_stop_resource_ids.getResourceId(throttleIndex,0));
            bAirs[throttleIndex] = findViewById(button_air_resource_ids.getResourceId(throttleIndex,0));

            airIndicators[throttleIndex] = findViewById(air_indicator_resource_ids.getResourceId(throttleIndex,0));
            airLineIndicators[throttleIndex] = findViewById(air_line_indicator_resource_ids.getResourceId(throttleIndex,0));

            bTargetRSpds[throttleIndex].setClickable(true);
            targetArrowSpeedButtonTouchListener = new TargetArrowSpeedButtonTouchListener(throttleIndex, speed_button_type.RIGHT);
            bTargetRSpds[throttleIndex].setOnLongClickListener(targetArrowSpeedButtonTouchListener);
            bTargetRSpds[throttleIndex].setOnTouchListener(targetArrowSpeedButtonTouchListener);
            bTargetRSpds[throttleIndex].setOnClickListener(targetArrowSpeedButtonTouchListener);

            bTargetLSpds[throttleIndex].setClickable(true);
            targetArrowSpeedButtonTouchListener = new TargetArrowSpeedButtonTouchListener(throttleIndex, speed_button_type.LEFT);
            bTargetLSpds[throttleIndex].setOnLongClickListener(targetArrowSpeedButtonTouchListener);
            bTargetLSpds[throttleIndex].setOnTouchListener(targetArrowSpeedButtonTouchListener);
            bTargetLSpds[throttleIndex].setOnClickListener(targetArrowSpeedButtonTouchListener);

            bTargetStops[throttleIndex].setClickable(true);
            targetArrowSpeedButtonTouchListener = new TargetArrowSpeedButtonTouchListener(throttleIndex, "stop");
            bTargetStops[throttleIndex].setOnLongClickListener(targetArrowSpeedButtonTouchListener);
            bTargetStops[throttleIndex].setOnTouchListener(targetArrowSpeedButtonTouchListener);
            bTargetStops[throttleIndex].setOnClickListener(targetArrowSpeedButtonTouchListener);

            bEStops[throttleIndex].setClickable(true);
            targetArrowSpeedButtonTouchListener = new TargetArrowSpeedButtonTouchListener(throttleIndex, "estop");
            bEStops[throttleIndex].setOnLongClickListener(targetArrowSpeedButtonTouchListener);
            bEStops[throttleIndex].setOnTouchListener(targetArrowSpeedButtonTouchListener);
            bEStops[throttleIndex].setOnClickListener(targetArrowSpeedButtonTouchListener);

            bAirs[throttleIndex].setClickable(true);
            airButtonTouchListener = new AirButtonTouchListener(throttleIndex);
            bAirs[throttleIndex].setOnClickListener(airButtonTouchListener);
            airEnabled = !prefSemiRealisticThrottleDisableAir;

        }

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        llSetSpeeds = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        svFnBtns = new ScrollView[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbSemiRealisticThrottles = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbBrakes = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbLoads = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        lUppers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lLowers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex] = findViewById(throttle_resource_ids.getResourceId(throttleIndex,0));
            lUppers[throttleIndex] = findViewById(loco_upper_resource_ids.getResourceId(throttleIndex,0));
            lLowers[throttleIndex] = findViewById(loco_lower_resource_ids.getResourceId(throttleIndex,0));
            llSetSpeeds[throttleIndex] = findViewById(throttle_set_speed_resource_ids.getResourceId(throttleIndex,0));

            SemiRealisticDirectionButtonTouchListener directionButtonTouchListener = new SemiRealisticDirectionButtonTouchListener(direction_type.FORWARD, throttleIndex);
            bTargetFwds[throttleIndex].setOnTouchListener(directionButtonTouchListener);
            directionButtonTouchListener = new SemiRealisticDirectionButtonTouchListener(direction_type.REVERSE, throttleIndex);
            bTargetRevs[throttleIndex].setOnTouchListener(directionButtonTouchListener);
            directionButtonTouchListener = new SemiRealisticDirectionButtonTouchListener(direction_type.NEUTRAL, throttleIndex);
            bTargetNeutrals[throttleIndex].setOnTouchListener(directionButtonTouchListener);

            vsbSpeeds[throttleIndex] = findViewById(speed_resource_ids.getResourceId(throttleIndex,0));

            vsbSemiRealisticThrottles[throttleIndex] = findViewById(semi_realistic_speed_resource_ids.getResourceId(throttleIndex,0));
            vsbBrakes[throttleIndex] = findViewById(brake_resource_ids.getResourceId(throttleIndex,0));
            vsbLoads[throttleIndex] = findViewById(load_resource_ids.getResourceId(throttleIndex,0));

            svFnBtns[throttleIndex] = findViewById(function_buttons_scroller_resource_ids.getResourceId(throttleIndex,0));

//            vsbSemiRealisticThrottles[throttleIndex].setMax(Math.round(maxThrottle));
            vsbSemiRealisticThrottles[throttleIndex].setMax(prefDisplaySemiRealisticThrottleNotches);
            vsbSemiRealisticThrottles[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_SEMI_REALISTIC_THROTTLE);
            if (!useNotches) {
                vsbSemiRealisticThrottles[throttleIndex].setTickType(tick_type.TICK_0_100);
            } else {
                vsbSemiRealisticThrottles[throttleIndex].setTickType(prefDisplaySemiRealisticThrottleNotches);
            }

            vsbBrakes[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_BRAKE);
            vsbBrakes[throttleIndex].setMax(prefSemiRealisticThrottleNumberOfBrakeSteps);
            vsbBrakes[throttleIndex].setTickType(prefSemiRealisticThrottleNumberOfBrakeSteps);
            vsbBrakes[throttleIndex].setTitle(getResources().getString(R.string.brake));

            vsbLoads[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_LOAD);
            vsbLoads[throttleIndex].setMax(prefSemiRealisticThrottleNumberOfLoadSteps);
            vsbLoads[throttleIndex].setTickType(prefSemiRealisticThrottleNumberOfLoadSteps);
            vsbLoads[throttleIndex].setTitle(getResources().getString(R.string.load));
        }

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        SemiRealisticThrottleSliderListener srtsl;
        BrakeSliderListener bsl;
        LoadSliderListener lsl;

        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            // set up listeners for all throttles
            srtsl = new SemiRealisticThrottleSliderListener(i);
            vsbSemiRealisticThrottles[i].setOnSeekBarChangeListener(srtsl);
            vsbSemiRealisticThrottles[i].setOnTouchListener(srtsl);

            bsl = new BrakeSliderListener(i);
            vsbBrakes[i].setOnSeekBarChangeListener(bsl);
            vsbBrakes[i].setOnTouchListener(bsl);

            lsl = new LoadSliderListener(i);
            vsbLoads[i].setOnSeekBarChangeListener(lsl);
            vsbLoads[i].setOnTouchListener(lsl);
        }

        // setup the handlers for the semi-realistic throttle updates
        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            semiRealisticTargetSpeedUpdateHandlers[i] = new Handler();
            semiRealisticSpeedButtonUpdateHandlers[i] = new Handler();
        }

        // setup the handlers for the semi-realistic air updates
        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            semiRealisticAirUpdateHandlers[i] = new Handler();
            semiRealisticAirLineUpdateHandlers[i] = new Handler();
        }

        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            showAirButtonState(i);
        }



        function_buttons_table_resource_ids.recycle();
        direction_indicator_forward_resource_ids.recycle();
        direction_indicator_reverse_resource_ids.recycle();
        target_speed_value_label_resource_ids.recycle();
        target_acceleration_value_label_resource_ids.recycle();
        button_target_fwd_resource_ids.recycle();
        button_target_rev_resource_ids.recycle();
        button_target_neutral_resource_ids.recycle();
        right_target_speed_button_resource_ids.recycle();
        left_target_speed_button_resource_ids.recycle();
        button_target_stop_resource_ids.recycle();
        button_e_stop_resource_ids.recycle();
        button_air_resource_ids.recycle();
        air_indicator_resource_ids.recycle();
        air_line_indicator_resource_ids.recycle();
        throttle_resource_ids.recycle();
        throttle_set_speed_resource_ids.recycle();
        loco_upper_resource_ids.recycle();
        loco_lower_resource_ids.recycle();
        speed_resource_ids.recycle();
        brake_resource_ids.recycle();
        load_resource_ids.recycle();
        function_buttons_scroller_resource_ids.recycle();
        semi_realistic_speed_resource_ids.recycle();

    } // end initialiseUiElements()

    @Override
    protected void getDirectionButtonPrefs() {
        super.getDirectionButtonPrefs();
        super.DIRECTION_BUTTON_LEFT_TEXT = getApplicationContext().getResources().getString(R.string.prefLeftDirectionButtonsShortDefaultValue);
        super.DIRECTION_BUTTON_RIGHT_TEXT = getApplicationContext().getResources().getString(R.string.prefRightDirectionButtonsShortDefaultValue);

        super.prefLeftDirectionButtons = prefs.getString("prefLeftDirectionButtonsShort", getApplicationContext().getResources().getString(R.string.prefLeftDirectionButtonsShortDefaultValue)).trim();
        super.prefRightDirectionButtons = prefs.getString("prefRightDirectionButtonsShort", getApplicationContext().getResources().getString(R.string.prefRightDirectionButtonsShortDefaultValue)).trim();
    }

    // lookup and set values of various informational text labels and size the
    // screen elements
    protected void setLabels() {
//        Log.d(threaded_application.applicationName, activityName + ": setLabels(): starting");
        super.setLabels();

        if (mainapp.appIsFinishing) { return; }

        // avoid NPE by not letting this run too early
        if (tvVols[0] == null) return;

        final int conNomTextSize = 24;
        final double minTextScale = 0.5;
        String bLabel;
        String bLabelPlainText;
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            Button b = bSels[throttleIndex];
            if ((mainapp.consists != null) && (mainapp.consists[throttleIndex] != null)
                    && (mainapp.consists[throttleIndex].isActive())) {
                if (!prefShowAddressInsteadOfName) {
                    if (!overrideThrottleNames[throttleIndex].isEmpty()) {
                        bLabel = overrideThrottleNames[throttleIndex];
                        bLabelPlainText = overrideThrottleNames[throttleIndex];
                    } else {
                        bLabel = mainapp.consists[throttleIndex].toHtml();
                        bLabelPlainText = mainapp.consists[throttleIndex].toString();
                    }
                } else {
                    if (overrideThrottleNames[throttleIndex].isEmpty()) {
                        bLabel = mainapp.consists[throttleIndex].formatConsistAddr();
                    } else {
                        bLabel = overrideThrottleNames[throttleIndex];
                    }
                    bLabelPlainText = bLabel;
                }
                bLabel = mainapp.locoAndConsistNamesCleanupHtml(bLabel);
                tvbSelsLabels[throttleIndex].setVisibility(GONE);

            } else {
                bLabel = getApplicationContext().getResources().getString(R.string.locoPressToSelect);
                bLabelPlainText = bLabel;
                tvbSelsLabels[throttleIndex].setVisibility(View.VISIBLE);
            }

            double textScale = 1.0;
            int bWidth = b.getWidth(); // scale text if required to fit the textView
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
//            double textWidth = b.getPaint().measureText(bLabel);
            double textWidth = b.getPaint().measureText(bLabelPlainText);
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
            int textSize = (int) (conNomTextSize * textScale * 0.95);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            b.setText(Html.fromHtml(bLabel));
            b.setSelected(false);
            b.setPressed(false);

        }

        setImmersiveMode(webView);
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

            //show speed buttons based on pref
            vsbSpeeds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default
            if (prefs.getBoolean("prefHideSlider", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
                vsbSpeeds[throttleIndex].setVisibility(GONE);
            }

            vsbSpeeds[throttleIndex].setVisibility(GONE); //always hide the real slider

            vsbSemiRealisticThrottles[throttleIndex].setVisibility(View.VISIBLE); //always show as a default
            if (prefs.getBoolean("prefHideSlider", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
                vsbSemiRealisticThrottles[throttleIndex].setVisibility(GONE);
            }
        }

        // update the target sliders if necessary  // update the direction indicators
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            showTargetDirectionIndication(throttleIndex);
            speedUpdate(throttleIndex, getSpeedFromCurrentSliderPosition(throttleIndex, false));
        }

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        // Get the screen's density scale
        final float denScale = dm.density;

        int screenWidth = vThrotScrWrap.getWidth(); // get the width of usable area
        int throttleWidth = screenWidth / mainapp.numThrottles;
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex].getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
            lThrottles[throttleIndex].getLayoutParams().width = throttleWidth;
            lThrottles[throttleIndex].requestLayout();
            llSetSpeeds[throttleIndex].requestLayout();
        }

        setImmersiveMode(webView);
        int screenHeight = vThrotScrWrap.getHeight(); // get the Height of usable area
        screenHeight = screenHeight - systemStatusRowHeight - systemNavigationRowHeight; // cater for immersive mode
        int fullScreenHeight = screenHeight;
        if ((toolbar != null) && (!prefThrottleViewImmersiveModeHideToolbar)) {
            titleBar = mainapp.getToolbarHeight(toolbar, statusLine, screenNameLine);
            if (screenHeight != 0) {
                screenHeight = screenHeight - titleBar;
            }
        }
        if (screenHeight == 0) {
            // throttle screen hasn't been drawn yet, so use display metrics for now
            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
        }

        // save part the screen for webview
        if (!prefWebViewLocation.equals(web_view_location_type.NONE)) {
            webViewIsOn = true;
            double height = screenHeight;
            if (!prefIncreaseWebViewSize) {
                height *= 0.5; // save half the screen
            } else {
                height *= 0.4; // save 60% of the screen for web view
            }
            screenHeight = (int) height;
            LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fullScreenHeight - titleBar - screenHeight);
            webView.setLayoutParams(webViewParams);
        }

        ImageView myImage = findViewById(R.id.backgroundImgView);
        myImage.getLayoutParams().height = screenHeight;

        int speedButtonHeight = (int) (50 * denScale);

        LinearLayout.LayoutParams stopButtonParams;
        stopButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        int prefVerticalStopButtonMargin = threaded_application.getIntPrefValue(prefs, "prefVerticalStopButtonMargin", "0");
        stopButtonParams.topMargin = Math.max(prefVerticalStopButtonMargin, (int) (speedButtonHeight * 0.5));
        stopButtonParams.bottomMargin = prefVerticalStopButtonMargin;
        stopButtonParams.height = speedButtonHeight;

        if (prefs.getBoolean("prefHideSlider", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
            speedButtonHeight = (int) ((screenHeight
                    - stopButtonParams.topMargin
                    - stopButtonParams.bottomMargin
                    - (160 * denScale)) / 2);
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            //show speed buttons based on pref
            if (prefs.getBoolean("prefDisplaySpeedButtons", false)) {
                bTargetLSpds[throttleIndex].setVisibility(View.VISIBLE);
                bTargetRSpds[throttleIndex].setVisibility(View.VISIBLE);

                bTargetLSpds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                bTargetLSpds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bTargetLSpds[throttleIndex].requestLayout();
                bTargetRSpds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                bTargetRSpds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bTargetRSpds[throttleIndex].requestLayout();
            } else {
                bTargetLSpds[throttleIndex].setVisibility(GONE);
                bTargetRSpds[throttleIndex].setVisibility(GONE);
            }

            // always hide the real buttons
            bLSpds[throttleIndex].setVisibility(GONE);
            bRSpds[throttleIndex].setVisibility(GONE);
            bStops[throttleIndex].setVisibility(GONE);


//            bStops[throttleIndex].getLayoutParams().height = (int) (speedButtonHeight * 0.8);
            bTargetStops[throttleIndex].setLayoutParams(stopButtonParams);
        }

//        int lowerButtonsHeight = findViewById(R.id.loco_buttons_group_0).getHeight();

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            svFnBtns[throttleIndex].requestLayout();
            lLowers[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            lLowers[throttleIndex].requestLayout();

            // update throttle slider top/bottom

            int[] location = new int[2];
            throttleOverlay.getLocationOnScreen(location);
            int ovx = location[0];
            int ovy = location[1];

            location = new int[2];
            vsbSemiRealisticThrottles[throttleIndex].getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];

            sliderTopLeftX[throttleIndex] = x - ovx;
            sliderTopLeftY[throttleIndex] = y - ovy;
            sliderBottomRightX[throttleIndex] = x + vsbSemiRealisticThrottles[throttleIndex].getWidth() - ovx;
            sliderBottomRightY[throttleIndex] = y + vsbSemiRealisticThrottles[throttleIndex].getHeight() - ovy;

            vsbBrakes[throttleIndex].getLocationOnScreen(location);
            x = location[0];
            y = location[1];
            brakeSliderTopLeftX[throttleIndex] = x - ovx;
            brakeSliderTopLeftY[throttleIndex] = y - ovy;
            brakeSliderBottomRightX[throttleIndex] = x + vsbBrakes[throttleIndex].getWidth() - ovx;
            brakeSliderBottomRightY[throttleIndex] = y + vsbBrakes[throttleIndex].getHeight() -ovy;

            vsbLoads[throttleIndex].getLocationOnScreen(location);
            x = location[0];
            y = location[1];
            loadSliderTopLeftX[throttleIndex] = x - ovx;
            loadSliderTopLeftY[throttleIndex] = y - ovy;
            loadSliderBottomRightX[throttleIndex] = x + vsbLoads[throttleIndex].getWidth() - ovx;
            loadSliderBottomRightY[throttleIndex] = y + vsbLoads[throttleIndex].getHeight() -ovy;
        }

//        // update the state of each function button based on shared variable
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            setAllFunctionStates(throttleIndex);
        }
    }

    @Override
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

        super.enableDisableButtons(whichThrottle, forceDisable);

        //avoid npe
        if (vsbSemiRealisticThrottles == null) return;

        if (!newEnabledState) { // set sliders to 0 if disabled
            vsbSemiRealisticThrottles[whichThrottle].setProgress(0);
            vsbBrakes[whichThrottle].setProgress(0);
            vsbLoads[whichThrottle].setProgress(0);
        }

        bTargetLSpds[whichThrottle].setEnabled(newEnabledState);
        bTargetRSpds[whichThrottle].setEnabled(newEnabledState);
        bTargetStops[whichThrottle].setEnabled(newEnabledState);

        vsbSemiRealisticThrottles[whichThrottle].setEnabled(newEnabledState);
        vsbBrakes[whichThrottle].setEnabled(newEnabledState);
        vsbLoads[whichThrottle].setEnabled(newEnabledState);

        bEStops[whichThrottle].setEnabled(newEnabledState);
        bAirs[whichThrottle].setEnabled(newEnabledState);
        showAirIndicator(whichThrottle);
        showAirLineIndicator(whichThrottle);
        showAirButtonState(whichThrottle);
    } // end of enableDisableButtons

    // helper function to enable/disable all children for a group
    @Override
    void enableDisableButtonsForView(ViewGroup vg, boolean newEnabledState) {

        if (vg == null) {
            return;
        }
        if (mainapp.appIsFinishing) {
            return;
        }

        ViewGroup r; // row
        Button b; // button
        for (int i = 0; i < vg.getChildCount(); i++) {
            r = (ViewGroup) vg.getChildAt(i);
            for (int j = 0; j < r.getChildCount(); j++) {
                b = (Button) r.getChildAt(j);
                b.setEnabled(newEnabledState);
            }
        }
    } // enableDisableButtonsForView

    // update the appearance of all function buttons
    @Override
    void setAllFunctionStates(int whichThrottle) {
        // Log.d(threaded_application.applicationName, activityName + ": set_function_states");

        if (mainapp.appIsFinishing) {
            return;
        }

        LinkedHashMap<Integer, Button> fMap;
        fMap = functionMaps[whichThrottle];

        for (Integer f : fMap.keySet()) {
            set_function_state(whichThrottle, f);
        }
    }


    // update a function button appearance based on its state
    @Override
    void set_function_state(int whichThrottle, int function) {
        Button b;
        boolean[] fs;   // copy of this throttle's function state array
        b = functionMaps[whichThrottle].get(function);
        fs = mainapp.function_states[whichThrottle];

        if (b != null && fs != null) {
            if (fs[function]) {
                b.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                b.setPressed(true);
            } else {
                b.setTypeface(null, Typeface.NORMAL);
                b.setPressed(false);
            }
        }
    }

    //Listeners for the throttle slider
    protected class SemiRealisticThrottleSliderListener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichThrottle;
        public int lastSliderPosition;
        int finalSliderPosition;

        protected SemiRealisticThrottleSliderListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle; // store values for this listener
            lastSliderPosition = 0;
            semiRealisticThrottleGestureState = GESTURE_NOT_STARTED;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            semiRealisticThrottleGestureState = GESTURE_NOT_STARTED;
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar sbSemiRealisticThrottle, int newSliderPosition, boolean fromUser) {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticThrottleSliderListener: onProgressChanged()");
            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (vsbSemiRealisticThrottles[whichThrottle].touchFromUser)) {
                if (semiRealisticThrottleGestureState == GESTURE_STARTED) {
                    semiRealisticThrottleGestureState = GESTURE_IN_PROGRESS;
                    lastSliderPosition = newSliderPosition;
                }
                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            } else {
                setSemiRealisticAutoIncrementDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
                setTargetSpeed(whichThrottle, false);
                lastSliderPosition = newSliderPosition;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbThrottle) {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticThrottleSliderListener: onStartTrackingTouch()");
            semiRealisticThrottleGestureState = GESTURE_STARTED;
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbThrottle) {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticThrottleSliderListener: onStopTrackingTouch()");
            finalSliderPosition = getSemiRealisticThrottleSlider(whichThrottle).getProgress();
            semiRealisticThrottleGestureState = GESTURE_FINISHED;
            autoIncrementDecrement();
        }

        public void autoIncrementDecrement() {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticThrottleSliderListener: autoIncrementDecrement()");
            if (lastSliderPosition == finalSliderPosition) {
                int targetSpeed = getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle);
                if (getSpeed(whichThrottle) < targetSpeed) {
                    setSemiRealisticAutoDecrement(whichThrottle);
                } else if (getSpeed(whichThrottle) > targetSpeed) {
                    setSemiRealisticAutoIncrement(whichThrottle);
                } else {
                    return;
                }
            } else {
                if (lastSliderPosition > finalSliderPosition) { // going down
                    setSemiRealisticAutoDecrement(whichThrottle);
                } else { // going up
                    setSemiRealisticAutoIncrement(whichThrottle);
                }
            }

            setTargetSpeed(whichThrottle,true);
//            restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, getSemiRealisticTargetSpeedRptDelay(whichThrottle));
        }
    }

    VerticalSeekBar getSemiRealisticThrottleSlider(int whichThrottle) {
        return vsbSemiRealisticThrottles[whichThrottle];
    }

    //Listeners for the brake slider
    protected class BrakeSliderListener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichThrottle;
        int lastBrakeSliderPosition;
        boolean dragInProgress = false;

        protected BrakeSliderListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle; // store values for this listener
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar sbBrake, int newSliderPosition, boolean fromUser) {
            mainapp.buttonVibration();
            int currentBrakeSliderPosition = getBrakeSliderPosition(whichThrottle);
            if ( ((currentBrakeSliderPosition >= lastBrakeSliderPosition) || (currentBrakeSliderPosition==0))
            && (!dragInProgress) ) {
                setTargetSpeed(whichThrottle, true);
                setBothAirValues(whichThrottle);
                setDecoderBrake(whichThrottle);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbBrake) {
            lastBrakeSliderPosition = getBrakeSliderPosition(whichThrottle);
            dragInProgress = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbBrake) {
            int currentBrakeSliderPosition = getBrakeSliderPosition(whichThrottle);

            if ( (!prefSemiRealisticThrottleDisableAir) && (airEnabled) ) { // air preferences is enabled
                if ((currentBrakeSliderPosition >= lastBrakeSliderPosition) || (currentBrakeSliderPosition == 0)) {
                    setTargetSpeed(whichThrottle, true);
                    setBothAirValues(whichThrottle);
                    setDecoderBrake(whichThrottle);
                } else {
                    vsbBrakes[whichThrottle].setProgress(lastBrakeSliderPosition);
                }
            } else { // air preferences is disabled
                setTargetSpeed(whichThrottle, true);
                setBothAirValues(whichThrottle);
                setDecoderBrake(whichThrottle);
            }
            dragInProgress = false;
        }
    }

    void setDecoderBrake(int whichThrottle) {
        if (!useEsuDecoderBrakes) return;

        boolean[] functionIsOn;
        int[] functionShouldBeOnOff;

        functionIsOn = new boolean[threaded_application.MAX_FUNCTIONS];
        functionShouldBeOnOff = new int[threaded_application.MAX_FUNCTIONS];

        for (int k=0; k<threaded_application.MAX_FUNCTIONS; k++) {
            functionIsOn[k] = mainapp.function_states[whichThrottle][k];
            functionShouldBeOnOff[k] = -1; // -1 don't care, 1 = on, 0 = off
        }

        double brakePcnt = (1 - getBrakeDecimalPcnt(getBrakeSliderPosition(whichThrottle), prefSemiRealisticThrottleNumberOfBrakeSteps, maxBrake)) * 100;
        int foundBrakeLevel = -1;
        for (int i = 0; i <= 2; i++) {  // cycle through the thresholds from low to high
            if (esuBrakeLevels[i] >= 0) {  // ignore it if it is set to -1
                for (int j = 0; j < 5; j++) { // set them to bee turned off
                    if (esuBrakeFunctions[i][j] >= 0) {
                        functionShouldBeOnOff[esuBrakeFunctions[i][j]] = 0;
                    }
                }
            }
        }
        for (int i = 2; i >= 0; i--) {  // cycle through the thresholds from high to low
            if (esuBrakeLevels[i] >= 0) {  // ignore it if it is set to -1
                if ((brakePcnt >= esuBrakeLevels[i]) && (foundBrakeLevel < 0)) {
                    for (int j = 0; j < 5; j++) { // set them to be turned on
                        if (esuBrakeFunctions[i][j] >= 0) {
                            functionShouldBeOnOff[esuBrakeFunctions[i][j]] = 1;
                        }
                    }
                    foundBrakeLevel = i;
                    esuBrakeActive[i] = true;
                } else {
                    esuBrakeActive[i] = false;
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

    //Listeners for the load slider
    protected class LoadSliderListener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichThrottle;
//        int lastLoadSliderPosition;

        protected LoadSliderListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle; // store values for this listener
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar sbLoad, int newSliderPosition, boolean fromUser) {
            mainapp.buttonVibration();
            setTargetSpeed(whichThrottle, true);
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbLoad) {
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbLoad) {
            setTargetSpeed(whichThrottle, true);
        }
    }

    protected class AirButtonTouchListener implements View.OnClickListener {
        int whichThrottle;

        protected AirButtonTouchListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @Override
        public void onClick(View v) {
            Log.d(threaded_application.applicationName, activityName + ": AirButtonTouchListener: onClick()");
            airEnabled = !airEnabled;
            showAirButtonState(whichThrottle);
            mainapp.buttonVibration();
        }
    }

    void showAirButtonState(int whichThrottle) {
        if ( (!getConsist(whichThrottle).isActive()) || (prefSemiRealisticThrottleDisableAir) ) {
            bAirs[whichThrottle].setEnabled(false);
            return;
        }
        bAirs[whichThrottle].setEnabled(true);
        if (airEnabled) {
            bAirs[whichThrottle].setSelected(true);
            bAirs[whichThrottle].setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
        } else {
            bAirs[whichThrottle].setSelected(false);
            bAirs[whichThrottle].setTypeface(null, Typeface.NORMAL);
        }
    }

    //listeners for the increase/decrease speed buttons (not the slider)
    protected class TargetArrowSpeedButtonTouchListener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
        int whichThrottle;
        String arrowDirection;

        protected TargetArrowSpeedButtonTouchListener(int new_whichThrottle, String new_arrowDirection) {
            whichThrottle = new_whichThrottle;
            arrowDirection = new_arrowDirection;
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(threaded_application.applicationName, activityName + ": TargetArrowSpeedButtonTouchListener: onLongClick()");
            boolean repeatRequired = true;
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (arrowDirection.equals(speed_button_type.RIGHT)) {
                mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.INCREMENT;
            } else if (arrowDirection.equals(speed_button_type.LEFT)) {
                mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.DECREMENT;
            } else if (arrowDirection.equals(speed_button_type.STOP)) {
                mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.OFF;
                mSemiRealisticAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.DECREMENT;
                setTargetSpeed(whichThrottle, 0);

            } else { //estop
                setEStop(whichThrottle);
                repeatRequired = false;
            }
            if (repeatRequired)
                restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, getSemiRealisticTargetSpeedRptDelay(whichThrottle));

            setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            mainapp.buttonVibration();
            return false;
        }

        @Override
        public void onClick(View v) {
            Log.d(threaded_application.applicationName, activityName + ": TargetArrowSpeedButtonTouchListener: onClick()");
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (arrowDirection.equals(speed_button_type.RIGHT)) {
                stopSemiRealisticThrottleSpeedButtonRepeater(whichThrottle);
                if (!semiRealisticSpeedButtonLongPressActive)
                    incrementSemiRealisticThrottlePosition(whichThrottle, speed_commands_from_type.BUTTONS);
                setTargetSpeed(whichThrottle, true);

            } else if (arrowDirection.equals(speed_button_type.LEFT)) {
                stopSemiRealisticThrottleSpeedButtonRepeater(whichThrottle);
                if (!semiRealisticSpeedButtonLongPressActive)
                    decrementSemiRealisticThrottlePosition(whichThrottle, speed_commands_from_type.BUTTONS);
                setTargetSpeed(whichThrottle, true);

            } else if (arrowDirection.equals(speed_button_type.STOP)) {
                switch (prefSemiRealisticThrottleStopButtonAction) {
                    case (stop_button_type.THROTTLE_STOP_BRAKE_FULL):
                        setBrakeSliderPosition(whichThrottle, prefSemiRealisticThrottleNumberOfBrakeSteps);
                        break;
                    case (stop_button_type.SPEED_ZERO):
                        setSpeed(whichThrottle, 0, speed_commands_from_type.BUTTONS);
                        break;
                    case (stop_button_type.SPEED_ZERO_BRAKE_ZERO):
                        setBrakeSliderPosition(whichThrottle, 0);
                        setSpeed(whichThrottle, 0, speed_commands_from_type.BUTTONS);
                        break;
                    case stop_button_type.THROTTLE_STOP:
                    default:
                        break;
                }

//                semiRealisticSpeedButtonLongPressActive = false;
                semiRealisticThrottleSliderPositionUpdate(whichThrottle,0);
                stopSemiRealisticThrottleSpeedButtonRepeater(whichThrottle);
                mSemiRealisticAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.OFF;
                setTargetSpeed(whichThrottle, true);

            } else { // estop
                setEStop(whichThrottle);
            }
            setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            mainapp.buttonVibration();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d(threaded_application.applicationName, activityName + ": TargetArrowSpeedButtonTouchListener: onTouch(): event: " + event.getAction());
            mainapp.exitDoubleBackButtonInitiated = 0;
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.INCREMENT) {
                stopSemiRealisticThrottleSpeedButtonRepeater(whichThrottle);
//                semiRealisticSpeedButtonLongPressActive = false;
            }
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.DECREMENT) {
                stopSemiRealisticThrottleSpeedButtonRepeater(whichThrottle);
//                semiRealisticSpeedButtonLongPressActive = false;
            }

            if ( (event.getAction() == MotionEvent.ACTION_DOWN)
            && (mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] != auto_increment_or_decrement_type.INCREMENT)
            && (arrowDirection.equals(speed_button_type.RIGHT)) ) {
//                semiRealisticSpeedButtonLongPressActive = true;
                restartSemiRealisticThrottleSpeedButtonRepeater(whichThrottle, auto_increment_or_decrement_type.INCREMENT, REP_DELAY);
            }
            if ((event.getAction() == MotionEvent.ACTION_DOWN)
            && (mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] != auto_increment_or_decrement_type.DECREMENT)
            && (arrowDirection.equals(speed_button_type.LEFT)) ) {
//                semiRealisticSpeedButtonLongPressActive = true;
                restartSemiRealisticThrottleSpeedButtonRepeater(whichThrottle, auto_increment_or_decrement_type.DECREMENT, REP_DELAY);
            }

            setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            return false;
        }
    }

    @Override
    void doVolumeButtonAction(int action, int key, int repeatCnt) {
        Log.d(threaded_application.applicationName, activityName + ": doVolumeButtonAction(): action: " + action);
        if (action==ACTION_UP) {
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
                                volumeKeysRepeatUpdateHandler.post(new SemiRealisticThrottleVolumeKeysRptUpdater(throttleIndex));
                            }
                        } else {
                            if (repeatCnt == 0) {
                                mVolumeKeysAutoDecrement = true;
                                volumeKeysRepeatUpdateHandler.post(new SemiRealisticThrottleVolumeKeysRptUpdater(throttleIndex));
                            }
                        }
                    }
                }
            }
        }
    }

    void setEStop(int whichThrottle) {
        semiRealisticThrottleSliderPositionUpdate(whichThrottle,0);
        setSpeed(whichThrottle, 0, speed_commands_from_type.BUTTONS);
        mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.OFF;
        setTargetSpeed(whichThrottle, 0);
        mainapp.sendEStopMsg();
        if (IS_ESU_MCII) setEsuThrottleKnobPosition(whichThrottle, 0);
    }

    // For volume speed buttons.
    private class SemiRealisticThrottleVolumeKeysRptUpdater implements Runnable {
        int whichThrottle;

        private SemiRealisticThrottleVolumeKeysRptUpdater(int WhichThrottle) {
            whichThrottle = WhichThrottle;
            try {
                REP_DELAY = Integer.parseInt(prefs.getString("prefSpeedButtonsRepeat", "100"));
            } catch (NumberFormatException ex) {
                REP_DELAY = 100;
            }
        }

        @Override
        public void run() {
            if (mVolumeKeysAutoIncrement) {
                incrementSemiRealisticThrottlePosition(whichThrottle, speed_commands_from_type.VOLUME);
                volumeKeysRepeatUpdateHandler.postDelayed(new SemiRealisticThrottleVolumeKeysRptUpdater(whichThrottle), REP_DELAY);
            } else if (mVolumeKeysAutoDecrement) {
                decrementSemiRealisticThrottlePosition(whichThrottle, speed_commands_from_type.VOLUME);
                volumeKeysRepeatUpdateHandler.postDelayed(new SemiRealisticThrottleVolumeKeysRptUpdater(whichThrottle), REP_DELAY);
            }
        }
    }

    @Override
    void semiRealisticThrottleSliderPositionUpdate(int whichThrottle, int newSpeed) {
        Log.d(threaded_application.applicationName, activityName + ": semiRealisticThrottleSliderPositionUpdate(): newSpeed " + newSpeed);
        if (newSpeed < 0)
            newSpeed = 0;
        int sliderPosition = getNewSemiRealisticThrottleSliderPositionFromSpeed(newSpeed, whichThrottle);
        setTargetSpeed(whichThrottle,newSpeed);
        vsbSemiRealisticThrottles[whichThrottle].setProgress(sliderPosition);
    }

    protected void setSemiRealisticAutoIncrement(int whichThrottle) {
        setSemiRealisticAutoIncrementDecrement(whichThrottle, auto_increment_or_decrement_type.INCREMENT);
    }
    protected void setSemiRealisticAutoDecrement(int whichThrottle) {
        setSemiRealisticAutoIncrementDecrement(whichThrottle, auto_increment_or_decrement_type.DECREMENT);
    }
    protected void setSemiRealisticAutoIncrementDecrement(int whichThrottle, int direction) {
        Log.d(threaded_application.applicationName, activityName + ": setSemiRealisticAutoIncrementDecrement(): direction " + direction);
        switch (direction) {
            case auto_increment_or_decrement_type.OFF:
            case auto_increment_or_decrement_type.INCREMENT:
            case auto_increment_or_decrement_type.DECREMENT:
                mSemiRealisticAutoIncrementOrDecrement[whichThrottle] = direction;
                break;
            case auto_increment_or_decrement_type.INVERT:
                if (mSemiRealisticAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.INCREMENT) {
                    mSemiRealisticAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.DECREMENT;
                } else if (mSemiRealisticAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.DECREMENT) {
                    mSemiRealisticAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.INCREMENT;
                }
                break;
        }
    }

    @Override
    public void decrementSemiRealisticThrottlePosition(int whichThrottle, int from) {
        Log.d(threaded_application.applicationName, activityName + ": decrementSemiRealisticThrottlePosition(): from: " + from);
        decrementSemiRealisticThrottlePosition(whichThrottle, from, 1);
    }
    @Override
    public void decrementSemiRealisticThrottlePosition(int whichThrottle, int from, int stepMultiplier) {
        switch (from) {
            case speed_commands_from_type.BUTTONS:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, -prefSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case speed_commands_from_type.VOLUME:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, -prefVolumeSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case speed_commands_from_type.GAMEPAD:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, -prefGamePadSpeedButtonsSpeedStep * stepMultiplier);
                mainapp.whichThrottleLastTouch = whichThrottle;
//                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED,whichThrottle, false
//                        ,getMaxSpeed(whichThrottle)
//                        ,getSpeedFromCurrentSliderPosition(whichThrottle,false)
//                        ,getSpeedFromCurrentSliderPosition(whichThrottle,true)
//                        , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
//                        , isSemiRealisticThrottle
//                        ,"");
                break;
        }
        mainapp.buttonVibration();
    }

    @Override
    public void incrementSemiRealisticThrottlePosition(int whichThrottle, int from) {
        incrementSemiRealisticThrottlePosition(whichThrottle, from, 1);
    }
    @Override
    public void incrementSemiRealisticThrottlePosition(int whichThrottle, int from, int stepMultiplier) {
        Log.d(threaded_application.applicationName, activityName + ": incrementSemiRealisticThrottlePosition(): from: " + from);
        switch (from) {
            case speed_commands_from_type.BUTTONS:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, prefSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case speed_commands_from_type.VOLUME:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, prefVolumeSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case speed_commands_from_type.GAMEPAD:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, prefGamePadSpeedButtonsSpeedStep * stepMultiplier);
                mainapp.whichThrottleLastTouch = whichThrottle;
//                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED,whichThrottle,false
//                        ,getMaxSpeed(whichThrottle)
//                        ,getSpeedFromCurrentSliderPosition(whichThrottle,false)
//                        ,getSpeedFromCurrentSliderPosition(whichThrottle,true)
//                        , getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle)
//                        , isSemiRealisticThrottle
//                        ,"");
                break;
        }
        mainapp.buttonVibration();
    }

    @Override
    boolean changeTargetDirectionIfAllowed(int whichThrottle, int direction) {
        int speed = getSpeed(whichThrottle);
        int currentDirection = getDirection(whichThrottle);
        boolean result = false;

        if (direction == direction_type.NEUTRAL) {
            targetDirections[whichThrottle] = direction;
            result = true;
        } else if ( (speed == 0) || (currentDirection == direction) ) {
            targetDirections[whichThrottle] = direction;
            setEngineDirection(whichThrottle,direction,false);
            result = true;
        }
        return result;
    }

    @Override
    void incrementBrakeSliderPosition(int whichThrottle) {
        updateBrakeSliderAndTargetSpeed(whichThrottle, 1);
    }

    @Override
    void decrementBrakeSliderPosition(int whichThrottle) {
        updateBrakeSliderAndTargetSpeed(whichThrottle, -1);
    }

    void updateBrakeSliderAndTargetSpeed(int whichThrottle, int delta) {
        Log.d(threaded_application.applicationName, activityName + ": updateBrakeSliderAndTargetSpeed(): delta: " + delta);
        int brakeSliderPosition = getBrakeSliderPosition(whichThrottle);
        int newPosition = brakeSliderPosition+delta;
        getBrakeSlider(whichThrottle).setProgress(newPosition);
        setTargetSpeed(whichThrottle, true);
    }

    @Override
    void incrementLoadSliderPosition(int whichThrottle) {
        updateLoadSliderAndTargetSpeed(whichThrottle, 1);
    }

    @Override
    void decrementLoadSliderPosition(int whichThrottle) {
        updateLoadSliderAndTargetSpeed(whichThrottle, -1);
    }

    void updateLoadSliderAndTargetSpeed(int whichThrottle, int delta) {
        Log.d(threaded_application.applicationName, activityName + ": updateBrakeSliderAndTargetSpeed(): delta: " + delta);
        int loadSliderPosition = getLoadSliderPosition(whichThrottle);
        int newPosition = loadSliderPosition+delta;
        getLoadSlider(whichThrottle).setProgress(newPosition);
        setTargetSpeed(whichThrottle, true);
    }

    @Override
    protected void redrawSliders() {
        runOnUiThread(new Runnable() {
            public void run() {
                for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                    vsbSemiRealisticThrottles[throttleIndex].tickMarksChecked = false;
                    vsbSemiRealisticThrottles[throttleIndex].invalidate();
                    vsbBrakes[throttleIndex].tickMarksChecked = false;
                    vsbBrakes[throttleIndex].invalidate();
                    vsbLoads[throttleIndex].tickMarksChecked = false;
                    vsbLoads[throttleIndex].invalidate();
                }
            }
        });

//        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
////            lLowers[throttleIndex].invalidate();
//            vsbSemiRealisticThrottles[throttleIndex].setVisibility(View.GONE);
//            vsbSemiRealisticThrottles[throttleIndex].setVisibility(View.VISIBLE);
//            vsbBrakes[throttleIndex].setVisibility(View.GONE);
//            vsbBrakes[throttleIndex].setVisibility(View.VISIBLE);
//            vsbLoads[throttleIndex].setVisibility(View.GONE);
//            vsbLoads[throttleIndex].setVisibility(View.VISIBLE);
//        }
    }

    // *******************************************************************************************************************************************
    // *******************************************************************************************************************************************
    // *******************************************************************************************************************************************

    @Override
    void setTargetSpeed(int whichThrottle, int newSpeed) {
        targetSpeeds[whichThrottle] = newSpeed;
        setTargetSpeed(whichThrottle, false);
    }

    @SuppressLint("DefaultLocale")
    @Override
    void setTargetSpeed(int whichThrottle, boolean fromSlider) {
        Log.d(threaded_application.applicationName, activityName + ": setTargetSpeed(): fromSlider: " + fromSlider);
        int targetSpeed;
        int sliderSpeed;
        if (fromSlider) {
            targetSpeed = getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle);
        } else {
            targetSpeed = targetSpeeds[whichThrottle];
        }

        if(IS_ESU_MCII && fromSlider) {
            setEsuThrottleKnobPosition(whichThrottle, getNewSemiRealisticThrottleSliderPositionFromSpeed(targetSpeed, whichThrottle));
        }

        sliderSpeed = getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle);
//        Log.d(threaded_application.applicationName, activityName + ": X targetSpeed: %d sliderSpeed: %d", targetSpeed, sliderSpeed));
        int targetDirection = getTargetDirection(whichThrottle);
        double targetAcceleration = 1; //default
        double brakeSliderPosition = getBrakeSliderPosition(whichThrottle);
        double loadSliderPosition = getLoadSliderPosition(whichThrottle);
        int speed = getSpeed(whichThrottle);

        if (targetDirection == direction_type.NEUTRAL) {
            targetSpeed = 0;
            sliderSpeed = 0;
            targetAcceleration = -1;
        }

        // air & brake
        double airLine = (double) 1 - ((double) getAirLineValue(whichThrottle) / 100);
        double airLineAsBrakeStep = Math.round( airLine * (double) prefSemiRealisticThrottleNumberOfBrakeSteps);
        double airLineAsBrakePcnt = getBrakeDecimalPcnt(airLineAsBrakeStep, prefSemiRealisticThrottleNumberOfBrakeSteps, maxBrake);

        double brakePcnt = getBrakeDecimalPcnt(brakeSliderPosition, prefSemiRealisticThrottleNumberOfBrakeSteps, maxBrake);

        double effectiveBrake = brakePcnt;

        if ( (!prefSemiRealisticThrottleDisableAir) && (airEnabled) ) {  // air preferences is enabled (default)
            if (airLineAsBrakePcnt < brakePcnt) {
                effectiveBrake = airLineAsBrakePcnt;
            }
        }

//        Log.d(threaded_application.applicationName, activityName + ": X airLine: %.2f airLineAsBrakeStep: %.2f airLineAsBrakePcnt: %.2f brakePcnt: %.2f targetSpeed: %d effectiveBrake: %.2f",
//        airLine, airLineAsBrakeStep, airLineAsBrakePcnt, brakePcnt, targetSpeed, effectiveBrake));

        double intermediateBrake;
        // brake and/or air
        if (effectiveBrake == 1) { // no brake.  always use the slider speed as the target speed
            targetSpeed = sliderSpeed;

        } else if (effectiveBrake < 1) {
            if (targetSpeed == 0) { // throttle is at zero
                intermediateBrake = -1 * effectiveBrake;
                targetAcceleration = intermediateBrake;
//                Log.d(threaded_application.applicationName, activityName + ": X (Target Speed zero) intermediateBrake: %.2f", intermediateBrake));

            } else { // throttle is still active
                targetSpeed = (int) (Math.round((double) sliderSpeed) - (Math.round((double) sliderSpeed) * (1 - effectiveBrake)) );

                if (targetSpeed <= getSpeed(whichThrottle))  {
//                    intermediateBrake = -1 * (1 - (effectiveBrake * effectiveBrake * maxBrakeUnderPower));
                    intermediateBrake = -1 * ( 1 - effectiveBrake * maxBrakeUnderPower);
                    targetAcceleration = intermediateBrake;
//                    Log.d(threaded_application.applicationName, activityName + ": X (Lower Speed) intermediateBrake: %.2f targetSpeed: %d",intermediateBrake, targetSpeed));

                } else {
//                    intermediateBrake = 1 - (effectiveBrake * effectiveBrake * maxBrake);
                    intermediateBrake = 1 + (1 - effectiveBrake * maxBrake);
                    targetAcceleration = intermediateBrake;
//                    Log.d(threaded_application.applicationName, activityName + ": X (Higher Speed) intermediateBrake: %.2f targetSpeed: %d",intermediateBrake, targetSpeed));
                }
            }
        }
//        Log.d(threaded_application.applicationName, activityName + ": X intermediateBrake: %.2f targetAcceleration: %.2f",intermediateBrake, targetAcceleration));

        // load
//        double intermediateLoad = 1;
        if  (loadSliderPosition > 0) {
            targetAcceleration = targetAcceleration
                    * getLoadPcnt(loadSliderPosition, prefSemiRealisticThrottleNumberOfLoadSteps, maxLoad);
        }

//        Log.d(threaded_application.applicationName, activityName + ": X 1 targetSpeed: %d targetAcceleration: %.2f",targetSpeed, targetAcceleration));

        // check max and min
        if (targetSpeed < 0) targetSpeed = 0;
        if (targetSpeed > maxThrottle) targetSpeed = maxThrottle;

//        Log.d(threaded_application.applicationName, activityName + ": X 2 targetSpeed: %d targetAcceleration: %.2f",targetSpeed, targetAcceleration));

        // check up or down
        if ( ((targetSpeed < speed) && (targetAcceleration > 0))
        || ((targetSpeed > speed) && (targetAcceleration < 0)) ) {
            targetAcceleration = targetAcceleration * -1;
        }

        Log.d(threaded_application.applicationName, activityName + ":" + String.format(" X 3 targetSpeed: %d targetAcceleration: %.2f",targetSpeed, targetAcceleration));

        targetAccelerations[whichThrottle] = targetAcceleration;
//        targetAccelerationsForDisplay[whichThrottle] = String.format("a:%.2f b:%.2f l:%.2f A:%.2f", air, intermediateBrake, intermediateLoad, getTargetAcceleration(whichThrottle));
//        targetAccelerationsForDisplay[whichThrottle] = String.format("b:%.2f A:%.2f", intermediateBrake, getTargetAcceleration(whichThrottle));
        targetAccelerationsForDisplay[whichThrottle] = String.format("%.2f", getTargetAcceleration(whichThrottle));

        displayTargetAcceleration((whichThrottle));

        targetSpeeds[whichThrottle] = targetSpeed;

        // take brake, reverser and load into account here...

        displayTargetScaleSpeed(whichThrottle);

        // now change the speed if necessary
        if ( (targetSpeed != speed)
        && ((targetSpeed != prevTargetSpeeds[whichThrottle]) || (loadSliderPosition != prevLoads[whichThrottle]))
        && (!semiRealisticSpeedButtonLongPressActive)) {
            if (targetSpeed > speed) {
                setSemiRealisticAutoIncrement(whichThrottle);
            } else {
                setSemiRealisticAutoDecrement(whichThrottle);
            }
            semiRealisticTargetSpeedUpdateHandlers[whichThrottle].removeCallbacksAndMessages(null);
            restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, getSemiRealisticTargetSpeedRptDelay(whichThrottle));
        }

        showTargetDirectionIndication(whichThrottle);
        showAirIndicator(whichThrottle);
        showAirLineIndicator(whichThrottle);
        showAirButtonState(whichThrottle);
        prevTargetSpeeds[whichThrottle] = targetSpeeds[whichThrottle];
        prevLoads[whichThrottle] = (int) loadSliderPosition;
    }

    // WARNING: a related calculation is also in the vertical slider class
    static double getBrakeDecimalPcnt(double step, double steps, double maxBrakeDecimal ) {
//        double brake = (double) step / (double) steps;
//        double intermediateBrake = (1 - (brake * brake * maxBrake));
        double max = Math.sqrt(steps) * steps * maxBrakeDecimal;
        return 1 - (Math.sqrt(step) * step * maxBrakeDecimal / max * maxBrakeDecimal);
    }

    // WARNING: a related calculation is also in the vertical slider class
    static double getLoadPcnt(double step, double steps, double maxLoadPcnt ) {
        double load = step / steps;
        return (((load * load * (maxLoadPcnt - 100))) + 100) / 100;
    }

    // *******************************************************************************************************************************************
    // *******************************************************************************************************************************************
    // *******************************************************************************************************************************************

    @Override
    protected void setDisplayedSpeed(int whichThrottle, int speed) {
        super.setDisplayedSpeed(whichThrottle, speed);
        showTargetDirectionIndication(whichThrottle);
    }

    @Override
    protected void setDisplayedSpeedWithDirection(int whichThrottle, int speed) {
        super.setDisplayedSpeedWithDirection(whichThrottle, speed);
        showTargetDirectionIndication(whichThrottle);
    }

    void updateSemiRealisticThrottleSliderAndTargetSpeed(int whichThrottle, int delta) {
        Log.d(threaded_application.applicationName, activityName + ": updateSemiRealisticThrottleSliderAndTargetSpeed(): delta: " + delta);
        int currentPosition = getSemiRealisticThrottleSlider(whichThrottle).getProgress();
        int newPosition = currentPosition+delta;
        getSemiRealisticThrottleSlider(whichThrottle).setProgress(newPosition);
        setTargetSpeed(whichThrottle, true);
    }

    protected void displayTargetScaleSpeed(int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": displayTargetScaleSpeed():");
        int targetSpeed = targetSpeeds[whichThrottle];
        int targetDirection = targetDirections[whichThrottle];

        TextView target_speed_label;
        double speedScale = getDisplayUnitScale(whichThrottle);
        target_speed_label = tvTargetSpdVals[whichThrottle];
        String result;
        if (targetSpeed < 0) {
            targetSpeed = 0;
        }
        int prevScaleSpeed = (int) Math.round(prevTargetSpeeds[whichThrottle] * speedScale);
        int scaleSpeed = (int) Math.round(targetSpeed * speedScale);
        try {
            switch (targetDirection) {
                case direction_type.FORWARD:
                    result = scaleSpeed + " ↑";
                    break;
                case direction_type.REVERSE:
                    result = scaleSpeed + " ↓";
                    break;
                case direction_type.NEUTRAL:
                default:
                    result = Integer.toString(scaleSpeed);
                    break;
            }
            target_speed_label.setText(result);
            mainapp.throttleVibration(scaleSpeed, prevScaleSpeed);
        } catch (NumberFormatException | ClassCastException e) {
            Log.e(threaded_application.applicationName, activityName + ": problem showing Target speed: " + e.getMessage());
        }
    }

    @SuppressLint("DefaultLocale")
    protected void displayTargetAcceleration(int whichThrottle) {
        TextView targetAccelerationView = tvTargetAccelerationVals[whichThrottle];
//        targetAccelerationView.setText(String.format("%.2f", getTargetAcceleration(whichThrottle)));
        targetAccelerationView.setText(getTargetAccelerationForDisplay(whichThrottle));
    }

    @Override
    protected int getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(int whichThrottle) {
        return getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle, true);
    }
    protected int getScaleSpeedFromSemiRealisticThrottleCurrentSliderPosition(int whichThrottle, boolean from126) {
        int speed = getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle);
        double scale = 0.793650794;    //  100/126
        if (!from126) {
            if (useNotches) scale = 100 / ((double) prefDisplaySemiRealisticThrottleNotches);
        }
        speed = (int) Math.round(speed * scale);
        return speed;
    }

        @Override
    protected int getSpeedFromSemiRealisticThrottleCurrentSliderPosition(int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": getSpeedFromSemiRealisticThrottleCurrentSliderPosition()");
        int semiRealisticThrottleSpeed = 0;
        if (vsbSemiRealisticThrottles[whichThrottle].isEnabled()) {
            semiRealisticThrottleSpeed = getSpeedFromSemiRealisticThrottleNewSliderPosition(vsbSemiRealisticThrottles[whichThrottle].getProgress(), whichThrottle);
        }
//        Log.d(threaded_application.applicationName, activityName + ": getSpeedFromSemiRealisticThrottleCurrentSliderPosition(): " + semiRealisticThrottleSpeed);
        return semiRealisticThrottleSpeed;
    }

    int getSpeedFromSemiRealisticThrottleNewSliderPosition(int sliderPosition, int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": getSpeedFromSemiRealisticThrottleNewSliderPosition()");
        double scale = 1;
        if (useNotches)  scale = 100 / ((double) prefDisplaySemiRealisticThrottleNotches);
        double max = ((double) maxThrottle) / 100;

        int semiRealisticThrottleSpeed = (int) Math.round( ((double) sliderPosition) * scale * max);
        Log.d(threaded_application.applicationName, activityName + ": getSpeedFromSemiRealisticThrottleNewSliderPosition(): pref: " + prefDisplaySemiRealisticThrottleNotches + " position: " + sliderPosition + " scale: " + scale + " speed: " + semiRealisticThrottleSpeed);
        return semiRealisticThrottleSpeed;
    }

    int getNewSemiRealisticThrottleSliderPositionFromSpeed(int newSpeed, int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": getNewSemiRealisticThrottleSliderPositionFromSpeed()");
        double scale = 1;
        if (useNotches)  scale = 100 / ((double) prefDisplaySemiRealisticThrottleNotches);
        double max = ((double) maxThrottle) / 126;

        int newSliderPosition = (int) Math.round(newSpeed / scale * max);
        Log.d(threaded_application.applicationName, activityName + ": getNewSemiRealisticThrottleSliderPositionFromSpeed(): pref: " + prefDisplaySemiRealisticThrottleNotches + " new speed: " + newSpeed + " scale: " + scale  + " new pos: " + newSliderPosition);
        return newSliderPosition;
    }

    protected int getNewSemiRealisticThrottleSliderScale() {
        return prefDisplaySemiRealisticThrottleNotches;
    }

    void setBrakeSliderPosition(int whichThrottle, int newPosition) {
        vsbBrakes[whichThrottle].setProgress(newPosition);
    }

    // set the Brake Slider position from a percentage value
    void setBrakeSliderPositionPcnt(int whichThrottle, float newPositionPcnt) {
        int newPosition = Math.round(newPositionPcnt * (float) prefSemiRealisticThrottleNumberOfBrakeSteps / 100);
        vsbBrakes[whichThrottle].setProgress(newPosition);
    }

    VerticalSeekBar getBrakeSlider(int whichThrottle) {
        return vsbBrakes[whichThrottle];
    }

    int getBrakeSliderPosition(int whichThrottle) {
        return vsbBrakes[whichThrottle].getProgress();
    }

    void setAirValue(int whichThrottle, int value) {
        airValues[whichThrottle] = value;
        if (airValues[whichThrottle] > 100) airValues[whichThrottle] = 100;
        if (airValues[whichThrottle] < 0 ) airValues[whichThrottle] = 0;
    }
    int getAirValue(int whichThrottle) {
        return airValues[whichThrottle];
    }
    void setAirLineValue(int whichThrottle, int value) {
        airLineValues[whichThrottle] = value;
        if (airLineValues[whichThrottle] > 100) airLineValues[whichThrottle] = 100;
        if (airLineValues[whichThrottle] < 0 ) airLineValues[whichThrottle] = 0;
    }
    int getAirLineValue(int whichThrottle) {
        return airLineValues[whichThrottle];
    }

    void setBothAirValues(int whichThrottle) {
        if (getBrakeSliderPosition(whichThrottle) > previousBrakePosition[whichThrottle]) {
            double airLine = ((double) (prefSemiRealisticThrottleNumberOfBrakeSteps - getBrakeSliderPosition(whichThrottle)) / (double) prefSemiRealisticThrottleNumberOfBrakeSteps) * 100;
            if ( (!prefSemiRealisticThrottleDisableAir) && (airEnabled) ) {
                setAirLineValue(whichThrottle, (airLine <= 100) ? (int) Math.round(airLine) : 100);
            }
            showAirLineIndicator(whichThrottle);
        }
        if (getBrakeSliderPosition(whichThrottle) == 0) { //
//            int neededAir = 100 - airLineValues[whichThrottle];
//            if (airValues[whichThrottle] > neededAir ) {
//                airLineValues[whichThrottle] = 100;
//                airValues[whichThrottle] = airValues[whichThrottle] - neededAir;
//            } else {
//                airLineValues[whichThrottle] = airLineValues[whichThrottle] + airValues[whichThrottle];
//                airValues[whichThrottle] = 0;
//            }

            if (!isAirRechaging) startSemiRealisticThrottleAirRepeater(whichThrottle);
            if (!isAirLineRechaging) startSemiRealisticThrottleAirLineRepeater(whichThrottle);
            showAirIndicator(whichThrottle);
            showAirLineIndicator(whichThrottle);
        }
        previousBrakePosition[whichThrottle] = getBrakeSliderPosition(whichThrottle);
    }

    @SuppressLint("DefaultLocale")
    void showAirIndicator(int whichThrottle) {
        if ( (airIndicators[whichThrottle].getWidth()<=0) || (airIndicators[whichThrottle].getHeight()<=0) )  // not drawn yet
            return;

        if (prefSemiRealisticThrottleDisableAir) {
            airIndicators[whichThrottle].setVisibility(GONE);
            return;
        }
        airIndicators[whichThrottle].setVisibility(View.VISIBLE);

        TypedValue typedValue = new TypedValue();
        int foregroundColor;
        int backgroundColor;
        getTheme().resolveAttribute(R.attr.ed_primaryLineColor, typedValue, true);
        foregroundColor = typedValue.data;
        getTheme().resolveAttribute(R.attr.ed_secondaryLineColor, typedValue, true);
        backgroundColor = typedValue.data;

        float xMax = (float) airIndicators[whichThrottle].getWidth();
        float yMax = (float) airIndicators[whichThrottle].getHeight();
        float yMaxBar = yMax - 160;
        float yScale = yMaxBar / 100;
        Bitmap bitmap = Bitmap.createBitmap(airIndicators[whichThrottle].getWidth(), airIndicators[whichThrottle].getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        Paint paint = new Paint();

        paint.setColor(backgroundColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(4);
        paint.setAntiAlias(true);
        canvas.drawRect(0, 0, xMax, yMaxBar, paint);

        paint.setColor(foregroundColor);
        canvas.drawRect( 0, yMaxBar, xMax, yMaxBar - airValues[whichThrottle] * yScale, paint);
//        if (!prefs.getString("prefTheme", threaded_application.context.getResources().getString(R.string.prefThemeDefaultValue)).equals("outline")) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect( 0, yMaxBar, xMax, yMaxBar - airValues[whichThrottle] * yScale, paint);
//        }
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(0, 0, xMax, yMaxBar, paint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(getResources().getColor(R.color.seekBarTickColor));
        textPaint.setTextSize(airIndicators[whichThrottle].getWidth());
        canvas.rotate(-90, xMax-2, yMax);
        canvas.drawText(String.format(getString(R.string.airReservoirShort), getAirValue(whichThrottle)), xMax-2, yMax, textPaint);
        canvas.rotate(90, xMax -2, yMax);

        airIndicators[whichThrottle].setImageBitmap(bitmap);
    }

    @SuppressLint("DefaultLocale")
    void showAirLineIndicator(int whichThrottle) {
        if ( (airLineIndicators[whichThrottle].getWidth() <= 0) || (airLineIndicators[whichThrottle].getHeight() <= 0 ) )  // not drawn yet
            return;

        if (prefSemiRealisticThrottleDisableAir) {
            airLineIndicators[whichThrottle].setVisibility(GONE);
            return;
        }
        airLineIndicators[whichThrottle].setVisibility(View.VISIBLE);

        TypedValue typedValue = new TypedValue();
        int foregroundColor;
        int backgroundColor;
        getTheme().resolveAttribute(R.attr.ed_primaryLineColor, typedValue, true);
        foregroundColor = typedValue.data;
        getTheme().resolveAttribute(R.attr.ed_secondaryLineColor, typedValue, true);
        backgroundColor = typedValue.data;

        float xMax = (float) airLineIndicators[whichThrottle].getWidth();
        float yMax = (float) airLineIndicators[whichThrottle].getHeight();
        float yMaxBar = yMax - 160;
        float yScale = yMaxBar / 100;
        Bitmap bitmap = Bitmap.createBitmap(airLineIndicators[whichThrottle].getWidth(), airLineIndicators[whichThrottle].getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        Paint paint = new Paint();

        paint.setColor(backgroundColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(4);
        paint.setAntiAlias(true);
        canvas.drawRect(0, 0, xMax, yMaxBar, paint);

        paint.setColor(foregroundColor);
        canvas.drawRect( 0, yMaxBar, xMax, yMaxBar - airLineValues[whichThrottle] * yScale, paint);
//        if (!prefs.getString("prefTheme", threaded_application.context.getResources().getString(R.string.prefThemeDefaultValue)).equals("outline")) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect( 0, yMaxBar, xMax, yMaxBar - airLineValues[whichThrottle] * yScale, paint);
//        }
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(0, 0, xMax, yMaxBar, paint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(getResources().getColor(R.color.seekBarTickColor));
        textPaint.setTextSize(airLineIndicators[whichThrottle].getWidth());
        canvas.rotate(-90, xMax-2, yMax);
        canvas.drawText(String.format(getString(R.string.airLineShort), getAirLineValue(whichThrottle)), xMax-2, yMax, textPaint);
        canvas.rotate(90, xMax -2, yMax);

        airLineIndicators[whichThrottle].setImageBitmap(bitmap);
    }

    VerticalSeekBar getLoadSlider(int whichThrottle) {
        return vsbLoads[whichThrottle];
    }

    int getLoadSliderPosition(int whichThrottle) {
        return vsbLoads[whichThrottle].getProgress();
    }

    // indicate direction using the button pressed state
    @Override
    protected void showTargetDirectionIndication(int whichThrottle) {
        Button bFwd = bTargetFwds[whichThrottle];
        Button bRev = bTargetRevs[whichThrottle];
        Button bNeutral = bTargetNeutrals[whichThrottle];
        if (directionButtonsAreCurrentlyReversed(whichThrottle)) {
            bRev = bTargetFwds[whichThrottle];
            bFwd = bTargetRevs[whichThrottle];
        }

        if (!getConsist(whichThrottle).isActive()) {
            bTargetFwds[whichThrottle].setSelected(false);
            bTargetRevs[whichThrottle].setSelected(false);
            bTargetNeutrals[whichThrottle].setSelected(false);
        } else {
            int speed = getSpeed(whichThrottle);

            bRev.setSelected(false);
            bRev.setTypeface(null, Typeface.NORMAL);
            bRev.setEnabled(false);

            bFwd.setSelected(false);
            bFwd.setTypeface(null, Typeface.NORMAL);
            bFwd.setEnabled(false);

            bNeutral.setSelected(false);
            bNeutral.setTypeface(null, Typeface.NORMAL);

            if (targetDirections[whichThrottle] == direction_type.FORWARD) {
                bFwd.setSelected(true);
                bFwd.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bFwd.setEnabled(true);

            } else if (targetDirections[whichThrottle] == direction_type.REVERSE) {
                bRev.setSelected(true);
                bRev.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bRev.setEnabled(true);
            } else if (targetDirections[whichThrottle] == direction_type.NEUTRAL) {
                bNeutral.setSelected(true);
                bNeutral.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bNeutral.setEnabled(true);


                int currentDirection = getDirection(whichThrottle);
                if ( (speed == 0) || (currentDirection == direction_type.FORWARD) ) {
                    bFwd.setEnabled(true);
                }
                if ( (speed == 0) || (currentDirection == direction_type.REVERSE) ) {
                    bRev.setEnabled(true);
                }
            }
            if (speed == 0) {
                bFwd.setEnabled(true);
                bRev.setEnabled(true);
            }
            bTargetNeutrals[whichThrottle].setEnabled(true);
        }
    }

    @Override
    void showHideSpeedLimitAndPauseButtons(int whichThrottle) {
        // not currently implemented

        if ((bLimitSpeeds != null) && (bLimitSpeeds[whichThrottle] != null)) {
            bLimitSpeeds[whichThrottle].setVisibility(GONE);
        }
        if ((bPauseSpeeds != null) && (bPauseSpeeds[whichThrottle] != null)) {
            bPauseSpeeds[whichThrottle].setVisibility(GONE);
        }
    }

    @Override
    protected int getTargetDirection(int whichThrottle) {
        return targetDirections[whichThrottle];
    }

    double getTargetAcceleration(int whichThrottle) {
        return targetAccelerations[whichThrottle];
    }
    String getTargetAccelerationForDisplay(int whichThrottle) {
        return targetAccelerationsForDisplay[whichThrottle];
    }

    // Listeners for the direction buttons
    private class SemiRealisticDirectionButtonTouchListener implements View.OnTouchListener {
        int direction;
        int whichThrottle;

        private SemiRealisticDirectionButtonTouchListener(int myDirection, int myWhichThrottle) {
            direction = myDirection;    // store these values for this button
            whichThrottle = myWhichThrottle;
        }

        private void doButtonPress() {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticDirectionButtonTouchListener: doButtonPress()");
            boolean result = false;
            mainapp.exitDoubleBackButtonInitiated = 0;

            switch (this.direction) {
                case direction_type.FORWARD:
                    result = changeTargetDirectionIfAllowed(whichThrottle, direction_type.FORWARD);
                    break;
                case direction_type.REVERSE:
                    result = changeTargetDirectionIfAllowed(whichThrottle, direction_type.REVERSE);
                    break;
                case direction_type.NEUTRAL: {
                    result = changeTargetDirectionIfAllowed(whichThrottle, direction_type.NEUTRAL);
                        break;
                }
            }

            if (result) {
                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
                showTargetDirectionIndication(whichThrottle);
                setTargetSpeed(whichThrottle, true);
            }
            mainapp.buttonVibration();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                doButtonPress();
            }
            return true;
        }
    }

    // For Semi Realistic Speed Button updater
    protected class SemiRealisticSpeedButtonRptUpdater implements Runnable {
        int whichThrottle;
        int buttonRepeatDelay;

        protected SemiRealisticSpeedButtonRptUpdater(int myWhichThrottle, int myButtonRepeatDelay) {
            whichThrottle = myWhichThrottle;
            buttonRepeatDelay = myButtonRepeatDelay;
        }

        @Override
        public void run() {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticSpeedButtonRptUpdater: run()");

            if (mainapp.appIsFinishing) { return;}

            if (mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.INCREMENT) {
                incrementSemiRealisticThrottlePosition(whichThrottle, speed_commands_from_type.BUTTONS);
                restartSemiRealisticThrottleSpeedButtonRepeater(whichThrottle, auto_increment_or_decrement_type.INCREMENT, buttonRepeatDelay);

            } else if (mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.DECREMENT) {
                decrementSemiRealisticThrottlePosition(whichThrottle, speed_commands_from_type.BUTTONS);
                restartSemiRealisticThrottleSpeedButtonRepeater(whichThrottle, auto_increment_or_decrement_type.DECREMENT, buttonRepeatDelay);
            }
            mainapp.buttonVibration();
        }
    }

    // For Semi Realistic Target Speed updater
    protected class SemiRealisticTargetSpeedRptUpdater implements Runnable {
        int whichThrottle;
        int delayMillis;

        protected SemiRealisticTargetSpeedRptUpdater(int myWhichThrottle, int myRepeatDelay) {
            whichThrottle = myWhichThrottle;
//            delayMillis = getSemiRealisticTargetSpeedRptDelay(whichThrottle, getSemiRealisticTargetSpeedRptDelay(whichThrottle, myRepeatDelay));
            delayMillis = myRepeatDelay;
        }

        @Override
        public void run() {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticTargetSpeedRptUpdater: run()");
            if (mainapp.appIsFinishing) { return;}

            if (getSpeed(whichThrottle) == targetSpeeds[whichThrottle]) return;

            if (delayMillis > 0) { // increment
                if (getSpeed(whichThrottle) > targetSpeeds[whichThrottle]) {
                    setSpeed(whichThrottle, targetSpeeds[whichThrottle], speed_commands_from_type.BUTTONS);
                    showTargetDirectionIndication(whichThrottle);
                    return;
                }
                incrementSpeed(whichThrottle, speed_commands_from_type.BUTTONS, 1, prefSemiRealisticThrottleSpeedStep);
                restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, delayMillis);
            } else {
                if (getSpeed(whichThrottle) < targetSpeeds[whichThrottle]) {
                    setSpeed(whichThrottle, targetSpeeds[whichThrottle], speed_commands_from_type.BUTTONS);
                    showTargetDirectionIndication(whichThrottle);
                    return;
                }
                delayMillis = delayMillis * -1;
                decrementSpeed(whichThrottle, speed_commands_from_type.BUTTONS, 1, prefSemiRealisticThrottleSpeedStep);
                restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, delayMillis);
                showTargetDirectionIndication(whichThrottle);
            }
        }
    }

    int getSemiRealisticTargetSpeedRptDelay(int whichThrottle) {
        return getSemiRealisticTargetSpeedRptDelay(whichThrottle, getRepeatDelay(whichThrottle));
    }
    int getSemiRealisticTargetSpeedRptDelay(int whichThrottle, int repeatDelay) {
        Log.d(threaded_application.applicationName, activityName + ": getSemiRealisticTargetSpeedRptDelay():" + ((int) Math.round( ((double) repeatDelay) * getTargetAcceleration(whichThrottle))) );
        return (int) Math.round( ((double) repeatDelay) * getTargetAcceleration(whichThrottle) );
    }

    int getRepeatDelay(int whichThrottle) {
        return getRepeatDelay(whichThrottle, 0);
    }
    int getRepeatDelay(int whichThrottle, int myRepeatDelay) {
        Log.d(threaded_application.applicationName, activityName + ": getRepeatDelay():");
        int repeatDelay = myRepeatDelay;

        if (repeatDelay==0) {
            if (mSemiRealisticAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.INCREMENT) {
                repeatDelay = prefSemiRealisticThrottleAcceleratonRepeat;
            } else {
                repeatDelay = prefSemiRealisticThrottleDecelerationRepeat;
            }
        }
        return repeatDelay;
    }

    void restartSemiRealisticThrottleTargetSpeedRepeater(int whichThrottle, int delayMillis) {
        Log.d(threaded_application.applicationName, activityName + ": restartSemiRealisticThrottleTargetSpeedRepeater(): delayMillis: " + delayMillis);
        if (delayMillis == 0) {
            semiRealisticTargetSpeedUpdateHandlers[whichThrottle]
                    .post(new SemiRealisticTargetSpeedRptUpdater(whichThrottle, getRepeatDelay(whichThrottle)));
        } else {
            semiRealisticTargetSpeedUpdateHandlers[whichThrottle]
                    .postDelayed(new SemiRealisticTargetSpeedRptUpdater(whichThrottle,getSemiRealisticTargetSpeedRptDelay(whichThrottle)), delayMillis);
        }
    }

    void restartSemiRealisticThrottleSpeedButtonRepeater(int whichThrottle, int direction, int delayMillis) {
        Log.d(threaded_application.applicationName, activityName + ": restartSemiRealisticThrottleSpeedButtonRepeater(): delayMillis: " + delayMillis);
        semiRealisticSpeedButtonLongPressActive = true;
        mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = direction;
        if (delayMillis == 0) {
            semiRealisticSpeedButtonUpdateHandlers[whichThrottle].post(new SemiRealisticSpeedButtonRptUpdater(whichThrottle, REP_DELAY));
        } else {
            semiRealisticSpeedButtonUpdateHandlers[whichThrottle].postDelayed(new SemiRealisticSpeedButtonRptUpdater(whichThrottle, REP_DELAY), delayMillis);
        }
    }

    void stopSemiRealisticThrottleSpeedButtonRepeater(int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": stopSemiRealisticThrottleSpeedButtonRepeater()");
        semiRealisticSpeedButtonLongPressActive = false;
        prevTargetSpeeds[whichThrottle] = 999;
        prevLoads[whichThrottle] = 999;
        mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.OFF;
        semiRealisticSpeedButtonUpdateHandlers[whichThrottle].removeCallbacks(null);
    }

    // For Semi Realistic Air update repeater
    protected class SemiRealisticAirRptUpdater implements Runnable {
        int whichThrottle;
        int delayMillis;

        protected SemiRealisticAirRptUpdater(int myWhichThrottle, int myRepeatDelay) {
            whichThrottle = myWhichThrottle;
            delayMillis = myRepeatDelay;
        }

        @Override
        public void run() {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticAirRptUpdater: run()");
            if (mainapp.appIsFinishing) { return;}

            if (getAirValue(whichThrottle) >= 100) {
                isAirRechaging = false;
                return;
            }

//            airValues[whichThrottle] = getAirValue(whichThrottle) + ((isAirLineRechaging) ? 2 : 10);
            setAirValue(whichThrottle, getAirValue(whichThrottle) + 5);

            setTargetSpeed(whichThrottle,false);
            showAirIndicator(whichThrottle);
            showAirLineIndicator(whichThrottle);
//            restartSemiRealisticThrottleAirRepeater(whichThrottle);
            semiRealisticAirUpdateHandlers[whichThrottle]
                    .postDelayed(new SemiRealisticAirRptUpdater(whichThrottle,prefSemiRealisticThrottleAirRefreshRate), prefSemiRealisticThrottleAirRefreshRate);
        }
    }

    void startSemiRealisticThrottleAirRepeater(int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": restartSemiRealisticThrottleAirRepeater():");
        isAirRechaging = true;
        semiRealisticAirUpdateHandlers[whichThrottle].removeCallbacks(null);
        semiRealisticAirUpdateHandlers[whichThrottle]
                .postDelayed(new SemiRealisticAirRptUpdater(whichThrottle,prefSemiRealisticThrottleAirRefreshRate), prefSemiRealisticThrottleAirRefreshRate);
    }

    // For Semi Realistic Air Line update repeater
    protected class SemiRealisticAirLineRptUpdater implements Runnable {
        int whichThrottle;
        int delayMillis;

        protected SemiRealisticAirLineRptUpdater(int myWhichThrottle, int myRepeatDelay) {
            whichThrottle = myWhichThrottle;
            delayMillis = myRepeatDelay;
        }

        @Override
        public void run() {
            Log.d(threaded_application.applicationName, activityName + ": SemiRealisticAirLineRptUpdater: run()");
            if (mainapp.appIsFinishing) { return;}

            if ( (getAirLineValue(whichThrottle) >= 100) || (getBrakeSliderPosition(whichThrottle) > 0)) {  // can't recharge the line if the brake is active
                isAirLineRechaging = false;
                if ( (airValues[whichThrottle]<100) && (!isAirRechaging) ) startSemiRealisticThrottleAirRepeater(whichThrottle);
                return;
            }

            if (airValues[whichThrottle] >= 20 ) {
                setAirLineValue(whichThrottle, getAirLineValue(whichThrottle) + 20);
                setAirValue(whichThrottle, airValues[whichThrottle] - 20);
                if (!isAirRechaging) startSemiRealisticThrottleAirRepeater(whichThrottle);

            }  else if (airValues[whichThrottle] >= 0 ) {
                int availableAir = airValues[whichThrottle];  // In case it have been partly refilled in the mean time
                setAirLineValue(whichThrottle, getAirLineValue(whichThrottle) + availableAir);
                setAirValue(whichThrottle, airValues[whichThrottle] - availableAir);
                if (!isAirRechaging) startSemiRealisticThrottleAirRepeater(whichThrottle);
            } // else nothing to charge with

            setTargetSpeed(whichThrottle,false);
            showAirIndicator(whichThrottle);
            showAirLineIndicator(whichThrottle);
            semiRealisticAirLineUpdateHandlers[whichThrottle]
                    .postDelayed(new SemiRealisticAirLineRptUpdater(whichThrottle,prefSemiRealisticThrottleAirRefreshRate), prefSemiRealisticThrottleAirRefreshRate);
        }
    }

    void startSemiRealisticThrottleAirLineRepeater(int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": restartSemiRealisticThrottleAirLineRepeater():");
        isAirLineRechaging = true;
        semiRealisticAirLineUpdateHandlers[whichThrottle].removeCallbacks(null);
        semiRealisticAirLineUpdateHandlers[whichThrottle]
                .postDelayed(new SemiRealisticAirLineRptUpdater(whichThrottle,prefSemiRealisticThrottleAirRefreshRate), prefSemiRealisticThrottleAirRefreshRate);
    }

}

