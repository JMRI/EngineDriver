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

Original version of the simple throttle is by radsolutions.
 */

package jmri.enginedriver;

import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;

import android.annotation.SuppressLint;
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
import android.widget.Toast;

import java.util.LinkedHashMap;

import jmri.enginedriver.type.auto_increment_or_decrement_type;
import jmri.enginedriver.type.speed_button_type;
import jmri.enginedriver.type.tick_type;
import jmri.enginedriver.type.tts_msg_type;
import jmri.enginedriver.util.VerticalSeekBar;

public class throttle_semi_realistic extends throttle {

    protected static final int MAX_SCREEN_THROTTLES = 1;

    private LinearLayout[] lThrottles;
    private LinearLayout[] lUppers;
    private LinearLayout[] lLowers;
    private LinearLayout[] lSpeeds;
    private ScrollView[] svFnBtns;

    private static final int SLIDER_PURPOSE_THROTTLE = 0;
    private static final int SLIDER_PURPOSE_OTHER = 1;
    private static final int SLIDER_PURPOSE_SEMI_REALISTIC_THROTTLE = 2;

    private final int[] throttleSwitchingMax = {0, 0, 0, 0, 0, 0};

    private static final int GESTURE_NOT_STARTED = 0;
    private static final int GESTURE_STARTED     = 1;
    private static final int GESTURE_IN_PROGRESS = 2;
    private static final int GESTURE_FINISHED    = 3;
    int semiRealisticThrottleGestureState = 0; //0=not in progress, 1=in progress 2=finished

    int maxThrottlePcnt = 100;
    int maxThrottle = 126;

    int prefSemiRealisticThrottleSpeedStep = 1;
    int prefDisplaySemiRealisticThrottleNotches = 100;
    boolean useNotches = false;

    protected void removeLoco(int whichThrottle) {
        super.removeLoco(whichThrottle);
        set_function_labels_and_listeners_for_view(whichThrottle);
    }

    @Override
    protected void getCommonPrefs(boolean isCreate) {
        super.getCommonPrefs(isCreate);

        maxThrottlePcnt = threaded_application.getIntPrefValue(prefs, "maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (0.01 * maxThrottlePcnt)); // convert from percent
        prefSemiRealisticThrottleSpeedStep = threaded_application.getIntPrefValue(prefs, "prefSemiRealisticThrottleSpeedStep", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleSpeedStepDefaultValue));
        prefDisplaySemiRealisticThrottleNotches = threaded_application.getIntPrefValue(prefs, "prefDisplaySemiRealisticThrottleNotches", getApplicationContext().getResources().getString(R.string.prefSemiRealisticThrottleNotchesDefaultValue));
        useNotches = (prefDisplaySemiRealisticThrottleNotches!=100);
        prefSpeedButtonsSpeedStep = threaded_application.getIntPrefValue(prefs, "speed_arrows_throttle_speed_step", "4");
        REP_DELAY = threaded_application.getIntPrefValue(prefs,"speed_arrows_throttle_repeat_delay", "100");
    }

    @SuppressLint({"Recycle", "SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Engine_Driver", "srmt: onCreate(): called");

        mainapp = (threaded_application) this.getApplication();

        mainapp.currentScreenSupportsWebView = false;

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
        mainapp.throttleLayoutViewId = R.layout.throttle_semi_realistic_left;

        super.onCreate(savedInstanceState);

        if (mainapp.appIsFinishing) { return; }

        bTargetFwds = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetRevs = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetNeutrals = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetRSpds = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetLSpds = new Button[mainapp.maxThrottlesCurrentScreen];
        bTargetStops = new Button[mainapp.maxThrottlesCurrentScreen];

        TargetArrowSpeedButtonTouchListener targetArrowSpeedButtonTouchListener;

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            fbs[throttleIndex] = findViewById(R.id.function_buttons_table_0);
            tvDirectionIndicatorForwards[throttleIndex] = findViewById(R.id.direction_indicator_forward_0);
            tvDirectionIndicatorReverses[throttleIndex] = findViewById(R.id.direction_indicator_reverse_0);
            tvTargetSpdVals[throttleIndex] = findViewById(R.id.target_speed_value_label_0);
            tvTargetAccelerationVals[throttleIndex] = findViewById(R.id.target_acceleration_value_label_0);
            bPauses[throttleIndex] = findViewById(R.id.button_pause_0);

            bTargetFwds[throttleIndex] = findViewById(R.id.button_target_fwd_0);
            bTargetRevs[throttleIndex] = findViewById(R.id.button_target_rev_0);
            bTargetNeutrals[throttleIndex] = findViewById(R.id.button_target_neutral_0);

            bTargetRSpds[throttleIndex] = findViewById(R.id.right_target_speed_button_0);
            bTargetLSpds[throttleIndex] = findViewById(R.id.left_target_speed_button_0);
            bTargetStops[throttleIndex] = findViewById(R.id.button_target_stop_0);

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

            PauseSpeedButtonTouchListener psvtl = new PauseSpeedButtonTouchListener(throttleIndex);
            bPauses[throttleIndex].setOnTouchListener(psvtl);
        }

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lSpeeds = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        svFnBtns = new ScrollView[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbSemiRealisticThrottles = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbBrakes = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbLoads = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        lUppers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lLowers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex] = findViewById(R.id.throttle_0);
            lUppers[throttleIndex] = findViewById(R.id.loco_upper_0);
            lLowers[throttleIndex] = findViewById(R.id.loco_lower_0);
            lSpeeds[throttleIndex] = findViewById(R.id.throttle_0_SetSpeed);

            SemiRealisticDirectionButtonTouchListener directionButtonTouchListener = new SemiRealisticDirectionButtonTouchListener(DIRECTION_FORWARD, throttleIndex);
            bTargetFwds[throttleIndex].setOnTouchListener(directionButtonTouchListener);
            directionButtonTouchListener = new SemiRealisticDirectionButtonTouchListener(DIRECTION_REVERSE, throttleIndex);
            bTargetRevs[throttleIndex].setOnTouchListener(directionButtonTouchListener);
            directionButtonTouchListener = new SemiRealisticDirectionButtonTouchListener(DIRECTION_NEUTRAL, throttleIndex);
            bTargetNeutrals[throttleIndex].setOnTouchListener(directionButtonTouchListener);

            vsbSpeeds[throttleIndex] = findViewById(R.id.speed_0);

            vsbSemiRealisticThrottles[throttleIndex] = findViewById(R.id.semi_realistic_speed_0);

            vsbBrakes[throttleIndex] = findViewById(R.id.brake_0);
            vsbBrakes[throttleIndex].setMax(5);

            vsbLoads[throttleIndex] = findViewById(R.id.load_0);
            vsbLoads[throttleIndex].setMax(2);

            svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_0);

//            vsbSemiRealisticThrottles[throttleIndex].setMax(Math.round(maxThrottle));
            vsbSemiRealisticThrottles[throttleIndex].setMax(prefDisplaySemiRealisticThrottleNotches);
            vsbSemiRealisticThrottles[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_SEMI_REALISTIC_THROTTLE);
            if (!useNotches) {
                vsbSemiRealisticThrottles[throttleIndex].setTickType(tick_type.TICK_0_100);
            } else {
                vsbSemiRealisticThrottles[throttleIndex].setTickType(prefDisplaySemiRealisticThrottleNotches);
            }

            vsbBrakes[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_OTHER);
            vsbBrakes[throttleIndex].setTickType(tick_type.TICK_0_5);
            vsbBrakes[throttleIndex].setTitle(getResources().getString(R.string.brake));

            vsbLoads[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_OTHER);
            vsbLoads[throttleIndex].setTickType(tick_type.TICK_0_2);
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

//        // set listeners for the limit speed buttons for each throttle
//        LimitSpeedButtonSemiRealisticTouchListener lsstl;
//        Button bLimitSpeed = findViewById(R.id.limit_speed_0);
//
//        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
//            bLimitSpeed = findViewById(R.id.limit_speed_0);
//
//            bLimitSpeeds[throttleIndex] = bLimitSpeed;
//            limitSpeedSliderScalingFactors[throttleIndex] = 1;
//            lsstl = new LimitSpeedButtonSemiRealisticTouchListener(throttleIndex);
//            bLimitSpeeds[throttleIndex].setOnTouchListener(lsstl);
//            isLimitSpeeds[throttleIndex] = false;
//            if (!prefLimitSpeedButton) {
//                bLimitSpeed.setVisibility(View.GONE);
//            }
//        }

        // setup the handlers for the semi-realistic throttle updates
        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            semiRealisticTargetSpeedUpdateHandlers[i] = new Handler();
            semiRealisticSpeedButtonUpdateHandlers[i] = new Handler();
        }

        sliderType = SLIDER_TYPE_VERTICAL;
    } // end of onCreate()

    @Override
    public void onResume() {
        Log.d("Engine_Driver", "srmt: onResume(): called");
        super.onResume();

        if (mainapp.appIsFinishing) { return; }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            if (throttleIndex < mainapp.numThrottles) {
                lThrottles[throttleIndex].setVisibility(LinearLayout.VISIBLE);
            } else {
                lThrottles[throttleIndex].setVisibility(LinearLayout.GONE);
            }
        }
    } // end of onResume()

    @Override
    protected void getDirectionButtonPrefs() {
        super.getDirectionButtonPrefs();
        super.DIRECTION_BUTTON_LEFT_TEXT = getApplicationContext().getResources().getString(R.string.prefLeftDirectionButtonsShortDefaultValue);
        super.DIRECTION_BUTTON_RIGHT_TEXT = getApplicationContext().getResources().getString(R.string.prefRightDirectionButtonsShortDefaultValue);

        super.prefLeftDirectionButtons = prefs.getString("prefLeftDirectionButtonsShort", getApplicationContext().getResources().getString(R.string.prefLeftDirectionButtonsShortDefaultValue)).trim();
        super.prefRightDirectionButtons = prefs.getString("prefRightDirectionButtonsShort", getApplicationContext().getResources().getString(R.string.prefRightDirectionButtonsShortDefaultValue)).trim();
    }

    protected void set_labels() {
        Log.d("Engine_Driver", "srmt: set_labels(): called");
        super.set_labels();

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
                    if (!overrideThrottleNames[throttleIndex].equals("")) {
                        bLabel = overrideThrottleNames[throttleIndex];
                        bLabelPlainText = overrideThrottleNames[throttleIndex];
                    } else {
                        bLabel = mainapp.consists[throttleIndex].toHtml();
                        bLabelPlainText = mainapp.consists[throttleIndex].toString();
                    }
                } else {
                    if (overrideThrottleNames[throttleIndex].equals("")) {
                        bLabel = mainapp.consists[throttleIndex].formatConsistAddr();
                    } else {
                        bLabel = overrideThrottleNames[throttleIndex];
                    }
                    bLabelPlainText = bLabel;
                }
                bLabel = mainapp.locoAndConsistNamesCleanupHtml(bLabel);
                tvbSelsLabels[throttleIndex].setVisibility(View.GONE);

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

        if (webView != null) {
            setImmersiveModeOn(webView, false);
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

            //show speed buttons based on pref
            vsbSpeeds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default
            if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
                vsbSpeeds[throttleIndex].setVisibility(View.GONE);
            }

            vsbSpeeds[throttleIndex].setVisibility(View.GONE); //always hide the real slider

            vsbSemiRealisticThrottles[throttleIndex].setVisibility(View.VISIBLE); //always show as a default
            if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
                vsbSemiRealisticThrottles[throttleIndex].setVisibility(View.GONE);
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
            lSpeeds[throttleIndex].requestLayout();
        }

        int screenHeight = vThrotScrWrap.getHeight(); // get the Height of usable area
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

        if (webView != null) {
            setImmersiveModeOn(webView, false);
        }

        // save part the screen for webview
        if (!webViewLocation.equals(WEB_VIEW_LOCATION_NONE)) {
            webViewIsOn = true;
            if (!prefIncreaseWebViewSize) {
                screenHeight *= 0.5; // save half the screen
            } else {
                screenHeight *= 0.4; // save 60% of the screen for web view
            }
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

        if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
            speedButtonHeight = (int) ((screenHeight
                    - stopButtonParams.topMargin
                    - stopButtonParams.bottomMargin
                    - (160 * denScale)) / 2);
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            //show speed buttons based on pref
            if (prefs.getBoolean("display_speed_arrows_buttons", false)) {
                bTargetLSpds[throttleIndex].setVisibility(View.VISIBLE);
                bTargetRSpds[throttleIndex].setVisibility(View.VISIBLE);

                bTargetLSpds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                bTargetLSpds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bTargetLSpds[throttleIndex].requestLayout();
                bTargetRSpds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                bTargetRSpds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bTargetRSpds[throttleIndex].requestLayout();
            } else {
                bTargetLSpds[throttleIndex].setVisibility(View.GONE);
                bTargetRSpds[throttleIndex].setVisibility(View.GONE);
            }

            // always hide the real buttons
            bLSpds[throttleIndex].setVisibility(View.GONE);
            bRSpds[throttleIndex].setVisibility(View.GONE);
            bStops[throttleIndex].setVisibility(View.GONE);


//            bStops[throttleIndex].getLayoutParams().height = (int) (speedButtonHeight * 0.8);
            bTargetStops[throttleIndex].setLayoutParams(stopButtonParams);
        }

        int lowerButtonsHeight = findViewById(R.id.loco_buttons_group_0).getHeight();

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            svFnBtns[throttleIndex].requestLayout();
            lLowers[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            lLowers[throttleIndex].requestLayout();

            // update throttle slider top/bottom

            int[] location = new int[2];
            ov.getLocationOnScreen(location);
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
            set_all_function_states(throttleIndex);
        }
    }

    @Override
    void enable_disable_buttons(int whichThrottle, boolean forceDisable) {
        boolean newEnabledState = false;
        // avoid index and null crashes
        if (mainapp.consists == null || whichThrottle >= mainapp.consists.length
                || bFwds[whichThrottle] == null) {
            return;
        }
        if (!forceDisable) { // avoid index crash, but may simply push to next line
            newEnabledState = mainapp.consists[whichThrottle].isActive(); // set false if lead loco is not assigned
        }

        super.enable_disable_buttons(whichThrottle, forceDisable);

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
    } // end of enable_disable_buttons

    // helper function to enable/disable all children for a group
    @Override
    void enable_disable_buttons_for_view(ViewGroup vg, boolean newEnabledState) {

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
    } // enable_disable_buttons_for_view

    // update the appearance of all function buttons
    @Override
    void set_all_function_states(int whichThrottle) {
        // Log.d("Engine_Driver","srmt: set_function_states");

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
             Log.d("Engine_Driver","srmt: SemiRealisticThrottleSliderListener: onProgressChanged()");
            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (vsbSemiRealisticThrottles[whichThrottle].touchFromUser)) {
                if (semiRealisticThrottleGestureState == GESTURE_STARTED) {
                    semiRealisticThrottleGestureState = GESTURE_IN_PROGRESS;
                    lastSliderPosition = newSliderPosition;
                }
                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            } else {
                setSemiRealisticAutoIncrementDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
                setTargetSpeed(whichThrottle, true);
                lastSliderPosition = newSliderPosition;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbThrottle) {
            Log.d("Engine_Driver","srmt: SemiRealisticThrottleSliderListener: onStartTrackingTouch()");
            semiRealisticThrottleGestureState = GESTURE_STARTED;
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbThrottle) {
            Log.d("Engine_Driver","srmt: SemiRealisticThrottleSliderListener: onStopTrackingTouch()");
            finalSliderPosition = getSemiRealisticThrottleSlider(whichThrottle).getProgress();
            semiRealisticThrottleGestureState = GESTURE_FINISHED;
            autoIncrementDecrement();
        }

        public void autoIncrementDecrement() {
            Log.d("Engine_Driver","srmt: SemiRealisticThrottleSliderListener: autoIncrementDecrement()");
            if (lastSliderPosition == finalSliderPosition) {
                int targetSpeed = getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle);
                if (getSpeed(whichThrottle) < targetSpeed) {
                    setSemiRealisticAutoIncrementDecrement(whichThrottle, auto_increment_or_decrement_type.DECREMENT);
                } else if (getSpeed(whichThrottle) > targetSpeed) {
                    setSemiRealisticAutoIncrementDecrement(whichThrottle, auto_increment_or_decrement_type.INCREMENT);
                } else {
                    return;
                }
            } else {
                if (lastSliderPosition > finalSliderPosition) { // going down
                    setSemiRealisticAutoIncrementDecrement(whichThrottle, auto_increment_or_decrement_type.DECREMENT);
                } else { // going up
                    setSemiRealisticAutoIncrementDecrement(whichThrottle, auto_increment_or_decrement_type.INCREMENT);
                }
            }

            setTargetSpeed(whichThrottle,true);
            restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, getRepeatDelay());
        }
    }

    VerticalSeekBar getSemiRealisticThrottleSlider(int whichThrottle) {
        return vsbSemiRealisticThrottles[whichThrottle];
    }

    //Listeners for the brake slider
    protected class BrakeSliderListener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichThrottle;
        int lastBrakeSliderPosition;

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
            setTargetSpeed(whichThrottle, true);
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbBrake) {
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbBrake) {
            setTargetSpeed(whichThrottle, true);
        }
    }

    //Listeners for the brake slider
    protected class LoadSliderListener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichThrottle;
        int lastLoadSliderPosition;

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
            Log.d("Engine_Driver","srmt: TargetArrowSpeedButtonTouchListener: onLongClick()");
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (arrowDirection.equals(speed_button_type.RIGHT)) {
                mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.INCREMENT;
            } else if (arrowDirection.equals(speed_button_type.LEFT)) {
                mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.DECREMENT;
            } else { // stop
                mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.OFF;
                mSemiRealisticAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.DECREMENT;
                setTargetSpeed(whichThrottle, 0);
            }
            restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, getRepeatDelay());

            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
            mainapp.buttonVibration();
            return false;
        }

        @Override
        public void onClick(View v) {
            Log.d("Engine_Driver","srmt: TargetArrowSpeedButtonTouchListener: onClick()");
            mainapp.exitDoubleBackButtonInitiated = 0;
            if (arrowDirection.equals(speed_button_type.RIGHT)) {
                stopSemiRealsticThrottleSpeedButtonRepeater(whichThrottle);
                if (!semiRealisticSpeedButtonLongPressActive)
                    incrementSemiRealisticThrottlePosition(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
                setTargetSpeed(whichThrottle, true);
            } else if (arrowDirection.equals(speed_button_type.LEFT)) {
                stopSemiRealsticThrottleSpeedButtonRepeater(whichThrottle);
                if (!semiRealisticSpeedButtonLongPressActive)
                    decrementSemiRealisticThrottlePosition(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
                setTargetSpeed(whichThrottle, true);
            } else { // stop
//                semiRealisticSpeedButtonLongPressActive = false;
                semiRealisticThrottleSliderPositionUpdate(whichThrottle,0);
                stopSemiRealsticThrottleSpeedButtonRepeater(whichThrottle);
                mSemiRealisticAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.OFF;
            }
            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
            mainapp.buttonVibration();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d("Engine_Driver","srmt: TargetArrowSpeedButtonTouchListener: onTouch(): event: " + event.getAction());
            mainapp.exitDoubleBackButtonInitiated = 0;
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.INCREMENT) {
                stopSemiRealsticThrottleSpeedButtonRepeater(whichThrottle);
//                semiRealisticSpeedButtonLongPressActive = false;
            }
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.DECREMENT) {
                stopSemiRealsticThrottleSpeedButtonRepeater(whichThrottle);
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
        Log.d("Engine_Driver", "srmt: doVolumeButtonAction(): action: " + action);
        if (action == ACTION_UP) {
        } else {
            threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastVolumeButtonsNotSupported), Toast.LENGTH_SHORT);
        }
    }

    void semiRealisticThrottleSliderPositionUpdate(int whichThrottle, int newSpeed) {
        Log.d("Engine_Driver","srmt: semiRealisticThrottleSliderPositionUpdate(): newSpeed " + newSpeed);
        if (newSpeed < 0)
            newSpeed = 0;

//        int sliderPosition = getNewSemiRealisticThrottleSliderPositionFromSpeed(newSpeed, whichThrottle, false);
        int sliderPosition = getNewSemiRealisticThrottleSliderPositionFromSpeed(newSpeed, whichThrottle);
        setTargetSpeed(whichThrottle,newSpeed);
        vsbSemiRealisticThrottles[whichThrottle].setProgress(sliderPosition);
    }

    protected void setSemiRealisticAutoIncrementDecrement(int whichThrottle, int direction) {
        Log.d("Engine_Driver","srmt: setSemiRealisticAutoIncrementDecrement(): direction " + direction);
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

    public void decrementSemiRealisticThrottlePosition(int whichThrottle, int from) {
        Log.d("Engine_Driver","srmt: decrementSemiRealisticThrottlePosition(): from: " + from);
        decrementSemiRealisticThrottlePosition(whichThrottle, from, 1);
    }
    public void decrementSemiRealisticThrottlePosition(int whichThrottle, int from, int stepMultiplier) {
        switch (from) {
            case SPEED_COMMAND_FROM_BUTTONS:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, -prefSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case SPEED_COMMAND_FROM_VOLUME:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, -prefVolumeSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case SPEED_COMMAND_FROM_GAMEPAD:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, -prefGamePadSpeedButtonsSpeedStep * stepMultiplier);
                mainapp.whichThrottleLastTouch = whichThrottle;
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED,whichThrottle, false
                        ,getMaxSpeed(whichThrottle)
                        ,getSpeedFromCurrentSliderPosition(whichThrottle,false)
                        ,getSpeedFromCurrentSliderPosition(whichThrottle,true)
                        ,"");
                break;
        }
        mainapp.buttonVibration();
    }

    public void incrementSemiRealisticThrottlePosition(int whichThrottle, int from) {
        incrementSemiRealisticThrottlePosition(whichThrottle, from, 1);
    }
    public void incrementSemiRealisticThrottlePosition(int whichThrottle, int from, int stepMultiplier) {
        Log.d("Engine_Driver","srmt: incrementSemiRealisticThrottlePosition(): from: " + from);
        switch (from) {
            case SPEED_COMMAND_FROM_BUTTONS:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, prefSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case SPEED_COMMAND_FROM_VOLUME:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, prefVolumeSpeedButtonsSpeedStep);
                mainapp.whichThrottleLastTouch = whichThrottle;
                break;
            case SPEED_COMMAND_FROM_GAMEPAD:
                updateSemiRealisticThrottleSliderAndTargetSpeed(whichThrottle, prefGamePadSpeedButtonsSpeedStep * stepMultiplier);
                mainapp.whichThrottleLastTouch = whichThrottle;
                tts.speakWords(tts_msg_type.GAMEPAD_THROTTLE_SPEED,whichThrottle,false
                        ,getMaxSpeed(whichThrottle)
                        ,getSpeedFromCurrentSliderPosition(whichThrottle,false)
                        ,getSpeedFromCurrentSliderPosition(whichThrottle,true)
                        ,"");
                break;
        }
        mainapp.buttonVibration();
    }

    boolean changeTargetDirectionIfAllowed(int whichThrottle, int direction) {
        int speed = getSpeed(whichThrottle);
        int currentDirection = getDirection(whichThrottle);
        boolean result = false;

        if (direction == DIRECTION_NEUTRAL) {
            targetDirections[whichThrottle] = direction;
            result = true;
        } else if ( (speed == 0) || (currentDirection == direction) ) {
            targetDirections[whichThrottle] = direction;
            setEngineDirection(whichThrottle,direction,false);
            result = true;
        }
        return result;
    }

    // *******************************************************************************************************************************************
    // *******************************************************************************************************************************************
    // *******************************************************************************************************************************************

    void setTargetSpeed(int whichThrottle, int newSpeed) {
        targetSpeeds[whichThrottle] = newSpeed;
        setTargetSpeed(whichThrottle, false);
    }

    void setTargetSpeed(int whichThrottle, boolean fromSlider) {
        Log.d("Engine_Driver","srmt: setTargetSpeed(): fromSlider: " + fromSlider);
        int targetSpeed;
        if (fromSlider) {
//            targetSpeed = getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle, false);
            targetSpeed = getSpeedFromSemiRealisticThrottleCurrentSliderPosition(whichThrottle);
        } else {
            targetSpeed = targetSpeeds[whichThrottle];
        }
        int targetDirection = getTargetDirection(whichThrottle);
        double targetAccelleration = 1; //default
        double brakeSliderPosition = (double) getBrakeSliderPosition(whichThrottle);
        double loadSliderPosition = (double) getLoadSliderPosition(whichThrottle);
        int speed = getSpeed(whichThrottle);

        if (targetDirection == DIRECTION_NEUTRAL) {
            targetSpeed = 0;
            targetAccelleration = -1;
        }
        if (brakeSliderPosition > 0) {
            targetSpeed = 0;
            targetAccelleration = -1 * 1/(brakeSliderPosition+1)/0.52;
        }
        if  (loadSliderPosition > 0) {
            targetAccelleration = targetAccelleration * (loadSliderPosition+1) ;
        }
        if (targetSpeed < 0) targetSpeed = 0;
//        if (targetSpeed > MAX_SPEED_VAL_WIT) targetSpeed = MAX_SPEED_VAL_WIT;
        if (targetSpeed > maxThrottle) targetSpeed = maxThrottle;

        if ((targetSpeed < speed) && (targetAccelleration > 0)) targetAccelleration = targetAccelleration * -1;
        if ((targetSpeed > speed) && (targetAccelleration < 0)) targetAccelleration = targetAccelleration * -1;

        targetAccelerations[whichThrottle] = targetAccelleration;
        displayTargetAcceleration((whichThrottle));

        targetSpeeds[whichThrottle] = targetSpeed;

        // take brake, reverser and load into account here...

        displayTargetScaleSpeed(whichThrottle);

        // now change the speed if necessary
        if ( (targetSpeed != speed)
        && (targetSpeed != prevTargetSpeeds[whichThrottle])
        && (!semiRealisticSpeedButtonLongPressActive)) {
            semiRealisticTargetSpeedUpdateHandlers[whichThrottle].removeCallbacksAndMessages(null);
            restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, getRepeatDelay());
        }

        showTargetDirectionIndication(whichThrottle);
        prevTargetSpeeds[whichThrottle] = targetSpeeds[whichThrottle];
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
        Log.d("Engine_Driver","srmt: updateSemiRealisticThrottleSliderAndTargetSpeed(): delta: " + delta);
        int currentPosition = getSemiRealisticThrottleSlider(whichThrottle).getProgress();
        int newPosition = currentPosition+delta;
        getSemiRealisticThrottleSlider(whichThrottle).setProgress(newPosition);
        setTargetSpeed(whichThrottle, true);
    }

    protected void displayTargetScaleSpeed(int whichThrottle) {
        Log.d("Engine_Driver","srmt: displayTargetScaleSpeed():");
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
                case DIRECTION_FORWARD:
                    result = Integer.toString(scaleSpeed) + " ↑";
                    break;
                case DIRECTION_REVERSE:
                    result = Integer.toString(scaleSpeed) + " ↓";
                    break;
                case DIRECTION_NEUTRAL:
                default:
                    result = Integer.toString(scaleSpeed);
                    break;
            }
            target_speed_label.setText(result);
            mainapp.throttleVibration(scaleSpeed, prevScaleSpeed);
        } catch (NumberFormatException | ClassCastException e) {
            Log.e("Engine_Driver", "srmt: problem showing Target speed: " + e.getMessage());
        }
    }

    @SuppressLint("DefaultLocale")
    protected void displayTargetAcceleration(int whichThrottle) {
        TextView targetAccelerationView = tvTargetAccelerationVals[whichThrottle];
        targetAccelerationView.setText(String.format("%.2f", getTargetAccelleration(whichThrottle)));
    }

    int getSpeedFromSemiRealisticThrottleCurrentSliderPosition(int whichThrottle) {
        Log.d("Engine_Driver", "srmt: getSpeedFromSemiRealisticThrottleCurrentSliderPosition()");
        int semiRealisticThrottleSpeed = 0;
        if (vsbSemiRealisticThrottles[whichThrottle].isEnabled()) {
            semiRealisticThrottleSpeed = getSpeedFromSemiRealisticThrottleNewSliderPosition(vsbSemiRealisticThrottles[whichThrottle].getProgress(), whichThrottle);
        }
//        Log.d("Engine_Driver", "srmt: getSpeedFromSemiRealisticThrottleCurrentSliderPosition(): " + semiRealisticThrottleSpeed);
        return semiRealisticThrottleSpeed;
    }

    int getSpeedFromSemiRealisticThrottleNewSliderPosition(int sliderPosition, int whichThrottle) {
        Log.d("Engine_Driver", "srmt: getSpeedFromSemiRealisticThrottleNewSliderPosition()");
        double scale = 1;
        if (useNotches)  scale = 100 / ((double) prefDisplaySemiRealisticThrottleNotches);
        double max = ((double) maxThrottle) / 100;

//        int semiRealisticThrottleSpeed = (int) Math.round( ((double) sliderPosition) * scale * 1.26);
        int semiRealisticThrottleSpeed = (int) Math.round( ((double) sliderPosition) * scale * max);
        Log.d("Engine_Driver", "srmt: getSpeedFromSemiRealisticThrottleNewSliderPosition(): pref: " + prefDisplaySemiRealisticThrottleNotches + " position: " + sliderPosition + " scale: " + scale + " speed: " + semiRealisticThrottleSpeed);
        return semiRealisticThrottleSpeed;
    }

    int getNewSemiRealisticThrottleSliderPositionFromSpeed(int newSpeed, int whichThrottle) {
        Log.d("Engine_Driver", "srmt: getNewSemiRealisticThrottleSliderPositionFromSpeed()");
        double scale = 1;
        if (useNotches)  scale = 100 / ((double) prefDisplaySemiRealisticThrottleNotches);
        double max = ((double) maxThrottle) / 126;

//        int newSliderPosition = (int) Math.round(newSpeed / scale);
        int newSliderPosition = (int) Math.round(newSpeed / scale * max);
        Log.d("Engine_Driver", "srmt: getNewSemiRealisticThrottleSliderPositionFromSpeed(): pref: " + prefDisplaySemiRealisticThrottleNotches + " new speed: " + newSpeed + " scale: " + scale  + " new pos: " + newSliderPosition);
        return newSliderPosition;
    }

    int getBrakeSliderPosition(int whichThrottle) {
        return vsbBrakes[whichThrottle].getProgress();
    }

    int getLoadSliderPosition(int whichThrottle) {
        return vsbLoads[whichThrottle].getProgress();
    }

    // indicate direction using the button pressed state
    void showTargetDirectionIndication(int whichThrottle) {
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

            if (targetDirections[whichThrottle] == DIRECTION_FORWARD) {
                bFwd.setSelected(true);
                bFwd.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bFwd.setEnabled(true);

            } else if (targetDirections[whichThrottle] == DIRECTION_REVERSE) {
                bRev.setSelected(true);
                bRev.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bRev.setEnabled(true);
            } else if (targetDirections[whichThrottle] == DIRECTION_NEUTRAL) {
                bNeutral.setSelected(true);
                bNeutral.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bNeutral.setEnabled(true);


                int currentDirection = getDirection(whichThrottle);
                if ( (speed == 0) || (currentDirection == DIRECTION_FORWARD) ) {
                    bFwd.setEnabled(true);
                }
                if ( (speed == 0) || (currentDirection == DIRECTION_REVERSE) ) {
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

    int getTargetDirection(int whichThrottle) {
        return targetDirections[whichThrottle];
    }

    double getTargetAccelleration(int whichThrottle) {
        return targetAccelerations[whichThrottle];
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
            Log.d("Engine_Driver", "srmt: SemiRealisticDirectionButtonTouchListener: doButtonPress()");
            boolean result = false;
            mainapp.exitDoubleBackButtonInitiated = 0;

            switch (this.direction) {
                case DIRECTION_FORWARD:
                    result = changeTargetDirectionIfAllowed(whichThrottle, DIRECTION_FORWARD);
                    break;
                case DIRECTION_REVERSE:
                    result = changeTargetDirectionIfAllowed(whichThrottle, DIRECTION_REVERSE);
                    break;
                case DIRECTION_NEUTRAL: {
                    result = changeTargetDirectionIfAllowed(whichThrottle, DIRECTION_NEUTRAL);
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
            Log.d("Engine_Driver","srmt: SemiRealisticSpeedButtonRptUpdater: run()");

            if (mainapp.appIsFinishing) { return;}

            if (mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.INCREMENT) {
                incrementSemiRealisticThrottlePosition(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
                restartSemiRealisticThrottleSpeedButtonRepeater(whichThrottle, auto_increment_or_decrement_type.INCREMENT, buttonRepeatDelay);

            } else if (mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] == auto_increment_or_decrement_type.DECREMENT) {
                decrementSemiRealisticThrottlePosition(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
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
            int repeatDelay = getRepeatDelay(myRepeatDelay);
            double delay = getSemiRealisticTargetSpeedRptDelay(whichThrottle, repeatDelay);
            delayMillis = (int) Math.round(delay);
        }

        @Override
        public void run() {
            Log.d("Engine_Driver","srmt: SemiRealisticTargetSpeedRptUpdater: run()");
            if (mainapp.appIsFinishing) { return;}

            if (getSpeed(whichThrottle) == targetSpeeds[whichThrottle]) return;

            if (delayMillis > 0) { // increment
                if (getSpeed(whichThrottle) > targetSpeeds[whichThrottle]) {
                    setSpeed(whichThrottle, targetSpeeds[whichThrottle], SPEED_COMMAND_FROM_BUTTONS);
                    showTargetDirectionIndication(whichThrottle);
                    return;
                }
                incrementSpeed(whichThrottle, SPEED_COMMAND_FROM_BUTTONS, 1, prefSemiRealisticThrottleSpeedStep);
                restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, delayMillis);
            } else {
                if (getSpeed(whichThrottle) < targetSpeeds[whichThrottle]) {
                    setSpeed(whichThrottle, targetSpeeds[whichThrottle], SPEED_COMMAND_FROM_BUTTONS);
                    showTargetDirectionIndication(whichThrottle);
                    return;
                }
                delayMillis = delayMillis * -1;
                decrementSpeed(whichThrottle, SPEED_COMMAND_FROM_BUTTONS, 1, prefSemiRealisticThrottleSpeedStep);
                restartSemiRealisticThrottleTargetSpeedRepeater(whichThrottle, delayMillis);
                showTargetDirectionIndication(whichThrottle);
            }
        }
    }

    double getSemiRealisticTargetSpeedRptDelay(int whichThrottle, int repeatDelay) {
        return repeatDelay * getTargetAccelleration(whichThrottle);
    }

    int getRepeatDelay() {
        return getRepeatDelay(0);
    }
    int getRepeatDelay (int myRepeatDelay) {
        int repeatDelay = myRepeatDelay;

        if (repeatDelay==0) {
            try {
                repeatDelay = Integer.parseInt(prefs.getString("prefSemiRealisticThrottleRepeat", mainapp.getResources().getString(R.string.prefSemiRealisticThrottleRepeatDefaultValue)));
            } catch (NumberFormatException ex) {
                repeatDelay = Integer.parseInt(mainapp.getResources().getString(R.string.prefSemiRealisticThrottleRepeatDefaultValue));
            }
        }
        return repeatDelay;
    }

    void restartSemiRealisticThrottleTargetSpeedRepeater(int whichThrottle, int delayMillis) {
        Log.d("Engine_Driver","srmt: restartSemiRealisticThrottleTargetSpeedRepeater(): delayMillis: " + delayMillis);
        if (delayMillis == 0) {
            semiRealisticTargetSpeedUpdateHandlers[whichThrottle].post(new SemiRealisticTargetSpeedRptUpdater(whichThrottle, prefPauseSpeedRate));
        } else {
            semiRealisticTargetSpeedUpdateHandlers[whichThrottle].postDelayed(new SemiRealisticTargetSpeedRptUpdater(whichThrottle,getRepeatDelay()), delayMillis);
        }
    }

    void restartSemiRealisticThrottleSpeedButtonRepeater(int whichThrottle, int direction, int delayMillis) {
        Log.d("Engine_Driver","srmt: restartSemiRealisticThrottleSpeedButtonRepeater(): delayMillis: " + delayMillis);
        semiRealisticSpeedButtonLongPressActive = true;
        mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = direction;
        if (delayMillis == 0) {
            semiRealisticSpeedButtonUpdateHandlers[whichThrottle].post(new SemiRealisticSpeedButtonRptUpdater(whichThrottle, REP_DELAY));
        } else {
            semiRealisticSpeedButtonUpdateHandlers[whichThrottle].postDelayed(new SemiRealisticSpeedButtonRptUpdater(whichThrottle, REP_DELAY), delayMillis);
        }
    }

    void stopSemiRealsticThrottleSpeedButtonRepeater(int whichThrottle) {
        Log.d("Engine_Driver","srmt: stopSemiRealsticThrottleSpeedButtonRepeater()");
        semiRealisticSpeedButtonLongPressActive = false;
        prevTargetSpeeds[whichThrottle] = 999;
        mSemiRealisticSpeedButtonsAutoIncrementOrDecrement[whichThrottle] = auto_increment_or_decrement_type.OFF;
        semiRealisticSpeedButtonUpdateHandlers[whichThrottle].removeCallbacks(null);
    }
}

