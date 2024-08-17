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

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
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

import java.util.LinkedHashMap;

import jmri.enginedriver.type.tick_type;
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

    private final int[] throttleMidPointZero = {0, 0, 0, 0, 0, 0};
    private final int[] throttleSwitchingMax = {0, 0, 0, 0, 0, 0};

    private final int[] throttleMidPointDeadZoneUpper = {0, 0, 0, 0, 0, 0};
    private final int[] throttleMidPointDeadZoneLower = {0, 0, 0, 0, 0, 0};

    private int prefSwitchingThrottleSliderDeadZone = 10;

    int maxThrottlePcnt = 100;
    int maxThrottle = 126;

    boolean mChangeDirectionAtZero = false; // needed for the mAutoincrement and mAutoDecrement

    protected void removeLoco(int whichThrottle) {
        super.removeLoco(whichThrottle);
        set_function_labels_and_listeners_for_view(whichThrottle);
    }

    @Override
    protected void getCommonPrefs(boolean isCreate) {
        super.getCommonPrefs(isCreate);

        maxThrottlePcnt = threaded_application.getIntPrefValue(prefs, "maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (0.01 * maxThrottlePcnt)); // convert from percent

        prefSwitchingThrottleSliderDeadZone = Integer.parseInt(prefs.getString("prefSwitchingThrottleSliderDeadZone", getResources().getString(R.string.prefSwitchingThrottleSliderDeadZoneDefaultValue)));

    }

    @SuppressLint({"Recycle", "SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {

        mainapp = (threaded_application) this.getApplication();

        mainapp.currentScreenSupportsWebView = true;

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
        mainapp.throttleLayoutViewId = R.layout.throttle_semi_realistic_left;

        super.onCreate(savedInstanceState);

        if (mainapp.appIsFinishing) {
            return;
        }

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
            bPauses[throttleIndex] = findViewById(R.id.button_pause_0);
            bTargetFwds[throttleIndex] = findViewById(R.id.button_target_fwd_0);
            bTargetRevs[throttleIndex] = findViewById(R.id.button_target_rev_0);
            bTargetNeutrals[throttleIndex] = findViewById(R.id.button_target_neutral_0);
            bTargetRSpds[throttleIndex] = findViewById(R.id.right_target_speed_button_0);
            bTargetLSpds[throttleIndex] = findViewById(R.id.left_target_speed_button_0);
            bTargetStops[throttleIndex] = findViewById(R.id.button_target_stop_0);


            bTargetRSpds[throttleIndex].setClickable(true);
            targetArrowSpeedButtonTouchListener = new TargetArrowSpeedButtonTouchListener(throttleIndex, "right");
            bTargetRSpds[throttleIndex].setOnLongClickListener(targetArrowSpeedButtonTouchListener);
            bTargetRSpds[throttleIndex].setOnTouchListener(targetArrowSpeedButtonTouchListener);
            bTargetRSpds[throttleIndex].setOnClickListener(targetArrowSpeedButtonTouchListener);

            bTargetLSpds[throttleIndex].setClickable(true);
            targetArrowSpeedButtonTouchListener = new TargetArrowSpeedButtonTouchListener(throttleIndex, "left");
            bTargetLSpds[throttleIndex].setOnLongClickListener(targetArrowSpeedButtonTouchListener);
            bTargetLSpds[throttleIndex].setOnTouchListener(targetArrowSpeedButtonTouchListener);
            bTargetLSpds[throttleIndex].setOnClickListener(targetArrowSpeedButtonTouchListener);

            PauseSpeedButtonTouchListener psvtl = new PauseSpeedButtonTouchListener(throttleIndex);
            bPauses[throttleIndex].setOnTouchListener(psvtl);
        }

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lSpeeds = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        svFnBtns = new ScrollView[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbSemiRealisticSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbBrakes = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbLoads = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        lUppers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lLowers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex] = findViewById(R.id.throttle_0);
            lUppers[throttleIndex] = findViewById(R.id.loco_upper_0);
            lLowers[throttleIndex] = findViewById(R.id.loco_lower_0);
            lSpeeds[throttleIndex] = findViewById(R.id.throttle_0_SetSpeed);

            vsbSpeeds[throttleIndex] = findViewById(R.id.speed_0);
            vsbSemiRealisticSpeeds[throttleIndex] = findViewById(R.id.semi_realistic_speed_0);
            vsbBrakes[throttleIndex] = findViewById(R.id.brake_0);
            vsbLoads[throttleIndex] = findViewById(R.id.load_0);
            svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_0);

            vsbSemiRealisticSpeeds[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_THROTTLE);
            vsbSemiRealisticSpeeds[throttleIndex].setTickType(tick_type.TICK_0_100);

            vsbBrakes[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_OTHER);
            vsbBrakes[throttleIndex].setTickType(tick_type.TICK_0_6);
            vsbBrakes[throttleIndex].setTitle(getResources().getString(R.string.brake));

            vsbLoads[throttleIndex].setSliderPurpose(SLIDER_PURPOSE_OTHER);
            vsbLoads[throttleIndex].setTickType(tick_type.TICK_0_3);
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
            vsbSemiRealisticSpeeds[i].setOnSeekBarChangeListener(srtsl);
            vsbSemiRealisticSpeeds[i].setOnTouchListener(srtsl);

            bsl = new BrakeSliderListener(i);
            vsbBrakes[i].setOnSeekBarChangeListener(bsl);
            vsbBrakes[i].setOnTouchListener(bsl);

            lsl = new LoadSliderListener(i);
            vsbLoads[i].setOnSeekBarChangeListener(lsl);
            vsbLoads[i].setOnTouchListener(lsl);
        }

        // set listeners for the limit speed buttons for each throttle
        LimitSpeedButtonSemiRealisticTouchListener lsstl;
        Button bLimitSpeed = findViewById(R.id.limit_speed_0);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            bLimitSpeed = findViewById(R.id.limit_speed_0);

            bLimitSpeeds[throttleIndex] = bLimitSpeed;
            limitSpeedSliderScalingFactors[throttleIndex] = 1;
            lsstl = new LimitSpeedButtonSemiRealisticTouchListener(throttleIndex);
            bLimitSpeeds[throttleIndex].setOnTouchListener(lsstl);
            isLimitSpeeds[throttleIndex] = false;
            if (!prefLimitSpeedButton) {
                bLimitSpeed.setVisibility(View.GONE);
            }
        }

        sliderType = SLIDER_TYPE_VERTICAL;
    } // end of onCreate()

    @Override
    public void onResume() {
        super.onResume();

        if (mainapp.appIsFinishing) {
            return;
        }

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
        super.set_labels();
        // Log.d("Engine_Driver","starting set_labels");

        if (mainapp.appIsFinishing) {
            return;
        }

        // avoid NPE by not letting this run too early (reported to Play Store)
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
//            b.setText(bLabel);
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
//            vsbSpeeds[throttleIndex].setVisibility(View.VISIBLE);

            vsbSemiRealisticSpeeds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default
            if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
                vsbSemiRealisticSpeeds[throttleIndex].setVisibility(View.GONE);
            }

        }

        // update the direction indicators
        showDirectionIndications();

        // update the target sliders if necessary
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            speedUpdate(throttleIndex,
                    getSpeedFromCurrentSliderPosition(throttleIndex, false));
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
//        int keepHeight = screenHeight;  // default height
        if (screenHeight == 0) {
            // throttle screen hasn't been drawn yet, so use display metrics for now
            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
            //Log.d("Engine_Driver","vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
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
//            speedButtonHeight = (int) ((screenHeight - (200 * denScale)) / 2);
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
            // set height of each function button area
//            if (mainapp.maxThrottlesCurrentScreen == 1) {
//                svFnBtns[throttleIndex].getLayoutParams().height = screenHeight - lowerButtonsHeight - lUppers[throttleIndex].getHeight();
//            } else {
//                svFnBtns[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
//            }
            svFnBtns[throttleIndex].requestLayout();
            lLowers[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            lLowers[throttleIndex].requestLayout();

            // update throttle slider top/bottom

            int[] location = new int[2];
            ov.getLocationOnScreen(location);
            int ovx = location[0];
            int ovy = location[1];

            location = new int[2];
            vsbSemiRealisticSpeeds[throttleIndex].getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];

            sliderTopLeftX[throttleIndex] = x - ovx;
            sliderTopLeftY[throttleIndex] = y - ovy;
            sliderBottomRightX[throttleIndex] = x + vsbSemiRealisticSpeeds[throttleIndex].getWidth() - ovx;
            sliderBottomRightY[throttleIndex] = y + vsbSemiRealisticSpeeds[throttleIndex].getHeight() - ovy;

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

//            Log.d("Engine_Driver","slider: " + throttleIndex + " Top: " + sliderTopLeftX[throttleIndex] + ", " + sliderTopLeftY[throttleIndex]
//                    + " Bottom: " + sliderBottomRightX[throttleIndex] + ", " + sliderBottomRightY[throttleIndex]);

        }


//        // update the state of each function button based on shared variable
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            set_all_function_states(throttleIndex);
        }

        // Log.d("Engine_Driver","ending set_labels");

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
        if (vsbSemiRealisticSpeeds == null) return;

        if (!newEnabledState) { // set sliders to 0 if disabled
            vsbSemiRealisticSpeeds[whichThrottle].setProgress(0);
            vsbBrakes[whichThrottle].setProgress(0);
            vsbLoads[whichThrottle].setProgress(0);
        }

        bTargetLSpds[whichThrottle].setEnabled(newEnabledState);
        bTargetRSpds[whichThrottle].setEnabled(newEnabledState);
        bTargetStops[whichThrottle].setEnabled(newEnabledState);

        vsbSemiRealisticSpeeds[whichThrottle].setEnabled(newEnabledState);
        vsbBrakes[whichThrottle].setEnabled(newEnabledState);
        vsbLoads[whichThrottle].setEnabled(newEnabledState);
    } // end of enable_disable_buttons

    // helper function to enable/disable all children for a group
    @Override
    void enable_disable_buttons_for_view(ViewGroup vg, boolean newEnabledState) {
        // Log.d("Engine_Driver","starting enable_disable_buttons_for_view " +
        // newEnabledState);

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
        // Log.d("Engine_Driver","set_function_states");

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
        // Log.d("Engine_Driver","starting set_function_request");

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
        int lastSliderPosition;
//        int lastDir;
//        boolean limitedJump;
        int jumpSpeed;

        protected SemiRealisticThrottleSliderListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle; // store values for this listener
            lastSliderPosition = throttleMidPointZero[whichThrottle];
            limitedJump[whichThrottle] = false;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Log.d("Engine_Driver", "onTouchThrottle action " + event.getAction());
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar sbThrottle, int newSliderPosition, boolean fromUser) {
            int semiRealisticSpeed;
            semiRealisticSpeed = getSemiRealisticSpeedFromSliderPosition(newSliderPosition, whichThrottle, false);

            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (vsbSemiRealisticSpeeds[whichThrottle].touchFromUser)) {
                if (!limitedJump[whichThrottle]) {         // touch generates multiple onProgressChanged events, skip processing after first limited jump

                    if (Math.abs(newSliderPosition - lastSliderPosition) > max_throttle_change) { // if jump is too large then limit it
                        jumpSpeed = getSemiRealisticSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(), whichThrottle, false);      // save ultimate target value
                        limitedJump[whichThrottle] = true;
                        sbThrottle.setProgress(lastSliderPosition);  // put the slider back to the original position
//                        doLocoSound(whichThrottle);

//                        if (newSliderPosition < lastSliderPosition) { // going down
//                            setAutoIncrementDecrement(whichThrottle, AUTO_INCREMENT_DECREMENT_DECREMENT);
//                        } else { // going up
//                            setAutoIncrementDecrement(whichThrottle, AUTO_INCREMENT_DECREMENT_INCREMENT);
//                        }

//                        repeatUpdateHandler.post(new RptUpdater(whichThrottle, prefPauseSpeedRate));

                        return;
                    }

//                    Log.d("Engine_Driver", "onProgressChanged -- no throttling");

//                    speedUpdate(whichThrottle,
//                            getSpeedFromSliderPosition(vsbSemiRealisticSpeeds[whichThrottle].getProgress(), whichThrottle, false));
//                    sendSpeedMsg(whichThrottle, speed);
//                    setDisplayedSpeed(whichThrottle, speed);

                } else { // got a touch while processing limitJump
//                    Log.d("Engine_Driver", "onProgressChanged -- touch while processing limited jump");
                    newSliderPosition = lastSliderPosition;    //   so suppress multiple touches
                    sbThrottle.setProgress(lastSliderPosition);
//                    doLocoSound(whichThrottle);

//                    Log.d("Engine_Driver", "onProgressChange: fromUser: " + fromUser + " vsbSwitchingSpeeds[wt].touchFromUser: " +vsbSwitchingSpeeds[whichThrottle].touchFromUser + " isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);
                }

                // Now update ESU MCII Knob position
//                if (IS_ESU_MCII) {
//                    setEsuThrottleKnobPosition(whichThrottle, targetSpeed);
//                }

                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference

            } else {
//                Log.d("Engine_Driver", "onProgressChanged -- lj: " + limitedJump + " d: " + dir + " ld: " + lastDir + " ai: " + mAutoIncrement[whichThrottle] + " ad: " + mAutoDecrement + " cdaZ: " + mChangeDirectionAtZero + " s: " + speed + " js: " + jumpSpeed);
                if (limitedJump[whichThrottle]) {

                    int tempJumpSpeed = jumpSpeed;
                    if (mChangeDirectionAtZero) {
                        tempJumpSpeed = 0;
                    }  // we will need to change directions.  for now just get to zero

//                    Log.d("Engine_Driver", "onProgressChanged -- lj: " + limitedJump[whichThrottle] + " d: " + dir + " ld: " + lastDir + " ai: " + mAutoIncrement[whichThrottle] + " ad: " + mAutoDecrement[whichThrottle] + " cdaZ: " + mChangeDirectionAtZero + " s: " + speed + " js: " + jumpSpeed + " tjs: " + tempJumpSpeed);

                    // check if we have hit the jumpSpeed or tempJumpSpeed (zero)
                    boolean hitJumpSpeed = false;
                    if (semiRealisticSpeed <= 0) {
                        if (jumpSpeed == 0) {
                            hitJumpSpeed = true;
                        }
                    } else // speed > 0
//                        if (dir == DIRECTION_FORWARD) {
                            if (((mAutoIncrement[whichThrottle]) && (semiRealisticSpeed >= tempJumpSpeed))
                                    || ((mAutoDecrement[whichThrottle]) && (semiRealisticSpeed <= tempJumpSpeed))) {
                                hitJumpSpeed = true;
                            }
//                        } else if (dir == DIRECTION_REVERSE) {
                            if (((mAutoDecrement[whichThrottle]) && (semiRealisticSpeed >= tempJumpSpeed))
                                    || ((mAutoIncrement[whichThrottle]) && (semiRealisticSpeed <= tempJumpSpeed))) {
                                hitJumpSpeed = true;
                            }
//                        }

                    if (hitJumpSpeed) {   // stop when we reach the target
                        Log.d("Engine_Driver", "onProgressChanged !!-- LimitedJump hit jump speed.");
                        limitedJump[whichThrottle] = false;
//                        setAutoIncrementDecrement(whichThrottle, AUTO_INCREMENT_DECREMENT_OFF);
                        sbThrottle.setProgress(getNewSliderPositionFromSemiRealisticSpeed(jumpSpeed, whichThrottle, false));
//                        speedUpdate(whichThrottle, getTargetSpeedFromSliderPosition(vsbSemiRealisticSpeeds[whichThrottle].getProgress(), whichThrottle, false));
                    }
                }
            }
            lastSliderPosition = newSliderPosition;
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbThrottle) {
//            Log.d("Engine_Driver", "onStartTrackingTouch() onProgressChanged");
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbThrottle) {
//            limitedJump[whichThrottle] = false;
//            setAutoIncrementDecrement(whichThrottle, AUTO_INCREMENT_DECREMENT_OFF);
        }
    }

    VerticalSeekBar getSemiRealisticThrottleSlider(int whichThrottle) {
        return vsbSemiRealisticSpeeds[whichThrottle];
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
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbBrake) {
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbBrake) {
            double brake = sbBrake.getProgress();
            brake = Math.round(brake/20) * (20); // 20 = 100%/5steps
            sbBrake.setProgress( (int) brake);
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
            // Log.d("Engine_Driver", "onTouchThrottle action " + event.getAction());
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar sbLoad, int newSliderPosition, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbLoad) {
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbLoad) {
            double brake = sbLoad.getProgress();
            brake = Math.round(brake/50) * (50); // 50 = 100%/2steps
            sbLoad.setProgress( (int) brake);
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
//            mainapp.exitDoubleBackButtonInitiated = 0;
//            if (arrowDirection.equals("right")) {
//                mAutoIncrement[whichThrottle] = true;
//            } else {
//                mAutoDecrement[whichThrottle] = true;
//            }
//            repeatUpdateHandler.post(new RptUpdater(whichThrottle,0));
//
//            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
//            mainapp.buttonVibration();
            return false;
        }

        @Override
        public void onClick(View v) {
//            mainapp.exitDoubleBackButtonInitiated = 0;
//            if (arrowDirection.equals("right")) {
//                mAutoIncrement[whichThrottle] = false;
//                incrementSpeed(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
//            } else {
//                mAutoDecrement[whichThrottle] = false;
//                decrementSpeed(whichThrottle, SPEED_COMMAND_FROM_BUTTONS);
//            }
//            setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
//            mainapp.buttonVibration();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
//            mainapp.exitDoubleBackButtonInitiated = 0;
//            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
//                    && mAutoIncrement[whichThrottle]) {
//                mAutoIncrement[whichThrottle] = false;
//            }
//
//            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
//                    && mAutoDecrement[whichThrottle]) {
//                mAutoDecrement[whichThrottle] = false;
//            }
//            setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
            return false;
        }
    }

    // change speed slider by scaled display unit value
    int semiRealisticSpeedChange(int whichThrottle, int change) {

        VerticalSeekBar targetThrottleSlider = getSemiRealisticThrottleSlider(whichThrottle);
        int lastSliderPosition = targetThrottleSlider.getProgress();
        int lastScaleSpeed;
        int scaleSpeed;
        int speed;
//        int lastSpeed;

        if (getDirection(whichThrottle) == DIRECTION_REVERSE) {  // treat negative as positive
            change = change * -1;
        }

        lastScaleSpeed = getSemiRealisticSpeedFromSliderPosition(lastSliderPosition, whichThrottle, true);
        scaleSpeed = lastScaleSpeed + change;
//        lastSpeed = getSpeedFromSliderPosition(lastSliderPosition, whichThrottle, false);

        if ((lastScaleSpeed > 0) && (scaleSpeed < 0)) {   // force a zero speed at least once when changing directions
            scaleSpeed = 0;
        }

//        Log.d("Engine_Driver", "throttle_semi_realistic - speedChange - lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) );
        if (scaleSpeed < 0) {
            int dir = getDirection(whichThrottle) == DIRECTION_FORWARD ? DIRECTION_REVERSE : DIRECTION_FORWARD;
            dirs[whichThrottle] = dir;
            setEngineDirection(whichThrottle, dir, false);
            showDirectionIndication(whichThrottle, dir);
        }
//        Log.d("Engine_Driver", "throttle_semi_realistic - speedChange - lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) );
//        Log.d("Engine_Driver","throttle_semi_realistic - speedChange - change: " + change);

        scaleSpeed = Math.abs(scaleSpeed);

        int newSliderPosition = getNewSliderPositionFromSemiRealisticSpeed(scaleSpeed, whichThrottle, true);

        if (lastScaleSpeed == scaleSpeed) {
            newSliderPosition += Math.signum(change);
        }

        if (newSliderPosition < 0)  //insure speed is inside bounds
            newSliderPosition = 0;

        if (newSliderPosition > throttleSwitchingMax[whichThrottle])
            newSliderPosition = throttleSwitchingMax[whichThrottle];

//        Log.d("Engine_Driver", "throttle_semi_realistic - speedChange - lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) + " newSliderPosition: " + newSliderPosition);
//        Log.d("Engine_Driver","throttle_semi_realistic - speedChange - change: " + change);

        targetThrottleSlider.setProgress(newSliderPosition);
        speed = Math.abs(getSemiRealisticSpeedFromSliderPosition(newSliderPosition, whichThrottle, false));
        setDisplayedSpeed(whichThrottle, speed);
        doLocoSound(whichThrottle);

//        speedUpdateAndNotify(whichThrottle, speed);
        return speed;

    } // end speedChange


    @Override
    void speedUpdate(int whichThrottle, int speed) {
        super.speedUpdate(whichThrottle, speed);

        int sliderPosition = getNewSliderPositionFromSemiRealisticSpeed(speed, whichThrottle, false);

        if (sliderPosition < 0)  //insure speed is inside bounds
            sliderPosition = 0;

        if (sliderPosition > throttleSwitchingMax[whichThrottle])
            sliderPosition = throttleSwitchingMax[whichThrottle];

        getSemiRealisticThrottleSlider(whichThrottle).setProgress(sliderPosition);
        setDisplayedSpeed(whichThrottle, speed);
        doLocoSound(whichThrottle);
    }

    // process WiT speed report
    // update speed slider if didn't just send a speed update to WiT
    @Override
    void speedUpdateWiT(int whichThrottle, int speedWiT) {
        super.speedUpdateWiT(whichThrottle, speedWiT);

        if (speedWiT < 0)
            speedWiT = 0;

        int sliderPosition;
        if (!changeTimers[whichThrottle].isDelayInProgress()) {
            sliderPosition = getNewSliderPositionFromSemiRealisticSpeed(speedWiT, whichThrottle, false);
            vsbSemiRealisticSpeeds[whichThrottle].setProgress(sliderPosition);
        }
        doLocoSound(whichThrottle);
    }


    @Override
    boolean changeDirectionIfAllowed(int whichThrottle, int direction) {
        if (getDirection(whichThrottle) != direction) {

            showDirectionRequest(whichThrottle, direction);        // update requested direction indication
            setEngineDirection(whichThrottle, direction, false);   // update direction for each engine on this throttle

            int lastSliderPosition = vsbSemiRealisticSpeeds[whichThrottle].getProgress();
            int lastScaleTargetSpeed;

            lastScaleTargetSpeed = getSemiRealisticSpeedFromSliderPosition(lastSliderPosition, whichThrottle, true);

            speedUpdate(whichThrottle, lastScaleTargetSpeed);
        }
        return (getDirection(whichThrottle) == direction);
    }

    int getDisplaySemiRealisticSpeedFromCurrentSliderPosition(int whichThrottle, boolean useScale) {
        return getSemiRealisticSpeedFromCurrentSliderPosition(whichThrottle, useScale);
    }

    int getSemiRealisticSpeedFromCurrentSliderPosition(int whichThrottle, boolean useScale) {
        int semiRealisticSpeed = 0;
        if (vsbSemiRealisticSpeeds[whichThrottle].isEnabled()) {
            semiRealisticSpeed = getSemiRealisticSpeedFromSliderPosition(vsbSemiRealisticSpeeds[whichThrottle].getProgress(), whichThrottle, useScale);
        }
        return semiRealisticSpeed;
    }

    int getSemiRealisticSpeedFromSliderPosition(int sliderPosition, int whichThrottle, boolean useScale) {
        int semiRealisticSpeed;

        double scale = 1;
        if (useScale) {
            scale = getDisplayUnitScale(whichThrottle);
        }
        semiRealisticSpeed = (int) Math.round(sliderPosition * scale);
//        Log.d("Engine_Driver","throttle_semi_realistic - getSemiRealisticSpeedFromSliderPosition -  scale: " + scale + " sliderPosition: " + sliderPosition + " speed: " + speed );
        return semiRealisticSpeed;
    }

    int getNewSliderPositionFromSemiRealisticSpeed(int semiRealisticSpeed, int whichThrottle, boolean useScale) {
        int newSliderPosition;
        double scale = getDisplayUnitScale(whichThrottle);
        if (!useScale) {
            scale = 1;
        }

        if (semiRealisticSpeed == 0) {
            newSliderPosition = throttleMidPointZero[whichThrottle];
        } else {
            if (getDirection(whichThrottle) == DIRECTION_FORWARD) {
                newSliderPosition = throttleMidPointDeadZoneUpper[whichThrottle] + (int) Math.round(semiRealisticSpeed / scale);
            } else {
                newSliderPosition = throttleMidPointDeadZoneLower[whichThrottle] - (int) Math.round(semiRealisticSpeed / scale);
            }
        }
//        Log.d("Engine_Driver","throttle_semi_realistic - getNewSliderPositionFromSpeed -  scale: " + scale + " speed: " + speed + " newSliderPosition: " + newSliderPosition );

        return newSliderPosition;
    }

    int getTargetSpeed(int whichThrottle) {
        // TODO:: need to take into account the load, brake and reverser
        return getSemiRealisticSpeedFromSliderPosition(vsbSemiRealisticSpeeds[whichThrottle].getProgress(), whichThrottle, false);
    }

    // set the displayed numeric speed value
    @Override
    protected void setDisplayedSpeed(int whichThrottle, int speed) {
        setDisplayedSpeedWithDirection(whichThrottle, speed);
    }

    //listeners for the Limit Speed Button
    protected class LimitSpeedButtonSemiRealisticTouchListener implements View.OnTouchListener {
        int whichThrottle;

        protected LimitSpeedButtonSemiRealisticTouchListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                vsbSemiRealisticSpeeds[whichThrottle].touchFromUser = false;
                limitSpeed(whichThrottle);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mainapp.buttonVibration();
            }
            return false;
        }
    }

    protected void limitSpeed(int whichThrottle) {
        int dir = getDirection(whichThrottle);
        int speed = getSemiRealisticSpeedFromSliderPosition(vsbSemiRealisticSpeeds[whichThrottle].getProgress(), whichThrottle, false);
//                Log.d("Engine_Driver","limit_speed_button_switching_touch_listener -  speed: " + speed );

        isLimitSpeeds[whichThrottle] = !isLimitSpeeds[whichThrottle];
        if (isLimitSpeeds[whichThrottle]) {
            bLimitSpeeds[whichThrottle].setSelected(true);
            limitSpeedSliderScalingFactors[whichThrottle] = 100 / ((float) prefLimitSpeedPercent);
            sbs[whichThrottle].setMax(Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]));

            throttleMidPointZero[whichThrottle] = (Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]) + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[whichThrottle] = (Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]) + prefSwitchingThrottleSliderDeadZone) * 2;
            vsbSemiRealisticSpeeds[whichThrottle].setMax(throttleSwitchingMax[whichThrottle]);

        } else {
            bLimitSpeeds[whichThrottle].setSelected(false);
            sbs[whichThrottle].setMax(maxThrottle);

//                    throttleMidPointZero = (MAX_SPEED_VAL_WIT + prefSwitchingThrottleSliderDeadZone);
//                    throttleSwitchingMax = (MAX_SPEED_VAL_WIT + prefSwitchingThrottleSliderDeadZone) * 2;
            throttleMidPointZero[whichThrottle] = (maxThrottle + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[whichThrottle] = (maxThrottle + prefSwitchingThrottleSliderDeadZone) * 2;
            vsbSemiRealisticSpeeds[whichThrottle].setMax(throttleSwitchingMax[whichThrottle]);
        }
        throttleMidPointDeadZoneUpper[whichThrottle] = throttleMidPointZero[whichThrottle] + prefSwitchingThrottleSliderDeadZone;
        throttleMidPointDeadZoneLower[whichThrottle] = throttleMidPointZero[whichThrottle] - prefSwitchingThrottleSliderDeadZone;

        speedUpdate(whichThrottle, speed);
        setEngineDirection(whichThrottle, dir, false);

        Log.d("Engine_Driver", "limitSpeed -  speed: " + speed);
        speedChangeAndNotify(whichThrottle, 0);
        setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
    }

    // indicate direction using the button pressed state
    void showDirectionIndication(int whichThrottle, int direction) {
        super.showDirectionIndication(whichThrottle, direction);
        boolean setLeftDirectionButtonEnabled;

        if (direction == 0) {  //0=reverse 1=forward
            setLeftDirectionButtonEnabled = directionButtonsAreCurrentlyReversed(whichThrottle);
        } else {
            setLeftDirectionButtonEnabled = !directionButtonsAreCurrentlyReversed(whichThrottle);
        }

        if (!getConsist(whichThrottle).isActive()) {
            bTargetFwds[whichThrottle].setSelected(false);
            bTargetRevs[whichThrottle].setSelected(false);
            bTargetNeutrals[whichThrottle].setSelected(false);
        } else {
            if (!setLeftDirectionButtonEnabled) {
                bTargetFwds[whichThrottle].setSelected(false);
                bTargetRevs[whichThrottle].setSelected(true);
                bTargetFwds[whichThrottle].setTypeface(null, Typeface.NORMAL);
                bTargetRevs[whichThrottle].setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                if ((getSpeed(whichThrottle) > 0) && (!dirChangeWhileMoving)) {
                    bTargetFwds[whichThrottle].setEnabled(false);
                }
                bTargetRevs[whichThrottle].setEnabled(true);

                bTargetNeutrals[whichThrottle].setEnabled(true);
            } else {
                bTargetFwds[whichThrottle].setSelected(true);
                bTargetRevs[whichThrottle].setSelected(false);
                bTargetFwds[whichThrottle].setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
                bTargetRevs[whichThrottle].setTypeface(null, Typeface.NORMAL);
                bTargetFwds[whichThrottle].setEnabled(true);
                if ((getSpeed(whichThrottle) > 0) && (!dirChangeWhileMoving)) {
                    bTargetRevs[whichThrottle].setEnabled(false);
                }
                bTargetNeutrals[whichThrottle].setEnabled(true);
            }
        }
    }
}
