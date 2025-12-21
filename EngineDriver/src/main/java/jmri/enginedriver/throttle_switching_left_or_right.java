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
import android.content.res.TypedArray;
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

import jmri.enginedriver.type.auto_increment_or_decrement_type;
import jmri.enginedriver.type.direction_type;
import jmri.enginedriver.type.kids_timer_action_type;
import jmri.enginedriver.type.max_throttles_current_screen_type;
import jmri.enginedriver.type.tick_type;
import jmri.enginedriver.util.VerticalSeekBar;
import jmri.enginedriver.type.slider_type;
import jmri.enginedriver.type.web_view_location_type;

public class throttle_switching_left_or_right extends throttle {
    static final String activityName = "throttle_switching_left_or_right";

    protected static final int MAX_SCREEN_THROTTLES = max_throttles_current_screen_type.SWITCHING;
    protected static final int MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT = max_throttles_current_screen_type.SWITCHING_LEFT_OR_RIGHT;
    protected static final int MAX_SCREEN_THROTTLES_TABLET_SWITCHING = max_throttles_current_screen_type.TABLET_SWITCHING;

    private LinearLayout[] lThrottles;
    private LinearLayout[] lUppers;
    private LinearLayout[] lLowers;
    private ScrollView[] svFnBtns;

    private final int[] throttleMidPointZero = {0,0,0,0,0,0};
    private final int[] throttleSwitchingMax = {0,0,0,0,0,0};

    private final int[] throttleMidPointDeadZoneUpper = {0,0,0,0,0,0};
    private final int[] throttleMidPointDeadZoneLower = {0,0,0,0,0,0};

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

    protected void setScreenDetails() {
        mainapp.currentScreenSupportsWebView = true;
        sliderType = slider_type.SWITCHING;

        switch (prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault))) {
            case "Switching":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
                mainapp.throttleLayoutViewId = R.layout.throttle_switching;
                break;
            case "Tablet Switching Left":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES_TABLET_SWITCHING;
                mainapp.throttleLayoutViewId = R.layout.throttle_switching_tablet_left;
                break;
            case "Switching Right":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT;
                mainapp.throttleLayoutViewId = R.layout.throttle_switching_right;
                break;
            case "Switching Left":
            default:
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT;
                mainapp.throttleLayoutViewId = R.layout.throttle_switching_left;
                break;
        }
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

        if (mainapp.appIsFinishing) { return;}

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            if( throttleIndex < mainapp.numThrottles) {
                lThrottles[throttleIndex].setVisibility(LinearLayout.VISIBLE);
            } else {
                lThrottles[throttleIndex].setVisibility(LinearLayout.GONE);
            }
        }

    } // end of onResume()

    @SuppressLint("ClickableViewAccessibility")
    void initialiseUiElements() {
        super.initialiseUiElements();

        TypedArray function_buttons_table_resource_ids = getResources().obtainTypedArray(R.array.function_buttons_table_resource_ids);
        TypedArray direction_indicator_forward_resource_ids = getResources().obtainTypedArray(R.array.direction_indicator_forward_resource_ids);
        TypedArray direction_indicator_reverse_resource_ids = getResources().obtainTypedArray(R.array.direction_indicator_reverse_resource_ids);
        TypedArray button_pause_resource_ids = getResources().obtainTypedArray(R.array.button_pause_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            functionButtonViewGroups[throttleIndex] = findViewById(function_buttons_table_resource_ids.getResourceId(throttleIndex,0));
            tvDirectionIndicatorForwards[throttleIndex] = findViewById(direction_indicator_forward_resource_ids.getResourceId(throttleIndex,0));
            tvDirectionIndicatorReverses[throttleIndex] = findViewById(direction_indicator_reverse_resource_ids.getResourceId(throttleIndex,0));
            bPauses[throttleIndex] = findViewById(button_pause_resource_ids.getResourceId(throttleIndex,0));

            PauseSpeedButtonTouchListener psvtl = new PauseSpeedButtonTouchListener(throttleIndex);
            bPauses[throttleIndex].setOnTouchListener(psvtl);
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            throttleMidPointZero[throttleIndex] =(maxThrottle + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[throttleIndex] = (maxThrottle + prefSwitchingThrottleSliderDeadZone) * 2;
            throttleMidPointDeadZoneUpper[throttleIndex] = throttleMidPointZero[throttleIndex] + prefSwitchingThrottleSliderDeadZone;
            throttleMidPointDeadZoneLower[throttleIndex] = throttleMidPointZero[throttleIndex] - prefSwitchingThrottleSliderDeadZone;
//        throttleReScale = ((throttleMidPointZero * 2)) / (double) throttleMidPointDeadZoneLower;
        }

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        llSetSpeeds = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        svFnBtns = new ScrollView[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbSwitchingSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        lUppers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lLowers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        TypedArray throttle_resource_ids = getResources().obtainTypedArray(R.array.throttle_resource_ids);
        TypedArray loco_upper_resource_ids = getResources().obtainTypedArray(R.array.loco_upper_resource_ids);
        TypedArray loco_lower_resource_ids = getResources().obtainTypedArray(R.array.loco_lower_resource_ids);
        TypedArray throttle_set_speed_resource_ids = getResources().obtainTypedArray(R.array.throttle_set_speed_resource_ids);
        TypedArray speed_resource_ids = getResources().obtainTypedArray(R.array.speed_resource_ids);
        TypedArray speed_switching_resource_ids = getResources().obtainTypedArray(R.array.speed_switching_resource_ids);
        TypedArray function_buttons_scroller_resource_ids = getResources().obtainTypedArray(R.array.function_buttons_scroller_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex] = findViewById(throttle_resource_ids.getResourceId(throttleIndex,0));
            lUppers[throttleIndex] = findViewById(loco_upper_resource_ids.getResourceId(throttleIndex,0));
            lLowers[throttleIndex] = findViewById(loco_lower_resource_ids.getResourceId(throttleIndex,0));
            llSetSpeeds[throttleIndex] = findViewById(throttle_set_speed_resource_ids.getResourceId(throttleIndex,0));
            vsbSpeeds[throttleIndex] = findViewById(speed_resource_ids.getResourceId(throttleIndex,0));
            vsbSwitchingSpeeds[throttleIndex] = findViewById(speed_switching_resource_ids.getResourceId(throttleIndex,0));
            svFnBtns[throttleIndex] = findViewById(function_buttons_scroller_resource_ids.getResourceId(throttleIndex,0));

            vsbSwitchingSpeeds[throttleIndex].setTickType(tick_type.TICK_0_100_0);
            vsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
            vsbSwitchingSpeeds[throttleIndex].setProgress(throttleMidPointZero[throttleIndex]);

            sbs[throttleIndex].setMax(Math.round(maxThrottle));
            vsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
        }

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        throttleSwitchingListener thsl;

        for (int i=0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            // set up listeners for all throttles
            thsl = new throttleSwitchingListener(i);
            vsbSwitchingSpeeds[i].setOnSeekBarChangeListener(thsl);
            vsbSwitchingSpeeds[i].setOnTouchListener(thsl);
        }

        // set listeners for the limit speed buttons for each throttle
        limit_speed_button_switching_touch_listener lsstl;
        Button bLimitSpeed = findViewById(R.id.limit_speed_0);

        TypedArray limit_speed_resource_ids = getResources().obtainTypedArray(R.array.limit_speed_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            bLimitSpeed = findViewById(limit_speed_resource_ids.getResourceId(throttleIndex,0));

            bLimitSpeeds[throttleIndex] = bLimitSpeed;
            limitSpeedSliderScalingFactors[throttleIndex] = 1;
            lsstl = new limit_speed_button_switching_touch_listener(throttleIndex);
            bLimitSpeeds[throttleIndex].setOnTouchListener(lsstl);
            isLimitSpeeds[throttleIndex] = false;
            if (!prefLimitSpeedButton) {
                bLimitSpeed.setVisibility(View.GONE);
            }
        }

        function_buttons_table_resource_ids.recycle();
        direction_indicator_forward_resource_ids.recycle();
        direction_indicator_reverse_resource_ids.recycle();
        button_pause_resource_ids.recycle();

        throttle_resource_ids.recycle();
        loco_upper_resource_ids.recycle();
        loco_lower_resource_ids.recycle();
        throttle_set_speed_resource_ids.recycle();
        speed_resource_ids.recycle();
        speed_switching_resource_ids.recycle();
        function_buttons_scroller_resource_ids.recycle();

        limit_speed_resource_ids.recycle();

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
//        Log.d(threaded_application.applicationName, activityName + ": set_Lbels() starting");
        super.setLabels();

        if (mainapp.appIsFinishing) { return;}

        // avoid NPE by not letting this run too early (reported to Play Store)
        if (tvVols[0] == null) return;

        final int conNomTextSize = 24;
        final double minTextScale = 0.5;
        String bLabel;
        String bLabelPlainText;
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            Button b = bSels[throttleIndex];
            if ( (mainapp.consists != null) && (mainapp.consists[throttleIndex] != null)
                    && (mainapp.consists[throttleIndex].isActive()) ) {
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

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

            //show speed buttons based on pref
            vsbSpeeds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default
            if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
                vsbSpeeds[throttleIndex].setVisibility(View.GONE);
            }

            vsbSpeeds[throttleIndex].setVisibility(View.GONE); //always hide the real slider
//            vsbSpeeds[throttleIndex].setVisibility(View.VISIBLE);

            vsbSwitchingSpeeds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default
            if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
                vsbSwitchingSpeeds[throttleIndex].setVisibility(View.GONE);
            }

        }

        // update the direction indicators
        showDirectionIndications();

        // update the switching sliders if necessary
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            speedUpdate(throttleIndex,
                    getSpeedFromCurrentSliderPosition(throttleIndex,false));
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
        if ((toolbar != null) && (!prefThrottleViewImmersiveModeHideToolbar))  {
            titleBar = mainapp.getToolbarHeight(toolbar, statusLine,  screenNameLine);
            if (screenHeight!=0) {
                screenHeight = screenHeight - titleBar;
            }
        }
//        int keepHeight = screenHeight;  // default height
        if (screenHeight == 0) {
            // throttle screen hasn't been drawn yet, so use display metrics for now
            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
            //Log.d(threaded_application.applicationName, activityName + ": setLabels(): vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
        }

        // save part the screen for webview
        if (!webViewLocation.equals(web_view_location_type.NONE)) {
            webViewIsOn = true;
            if (!prefIncreaseWebViewSize) {
                screenHeight *= 0.5; // save half the screen
            } else {
                screenHeight *= 0.4; // save 60% of the screen for web view
            }
            LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,fullScreenHeight - titleBar - screenHeight);
            webView.setLayoutParams(webViewParams);
        }

        ImageView myImage = findViewById(R.id.backgroundImgView);
        myImage.getLayoutParams().height = screenHeight;

        int speedButtonHeight = (int) (50 * denScale);

        LinearLayout.LayoutParams stopButtonParams;
        stopButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
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
                bLSpds[throttleIndex].setVisibility(View.VISIBLE);
                bRSpds[throttleIndex].setVisibility(View.VISIBLE);

                bLSpds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                bLSpds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bLSpds[throttleIndex].requestLayout();
                bRSpds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                bRSpds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bRSpds[throttleIndex].requestLayout();
            } else {
                bLSpds[throttleIndex].setVisibility(View.GONE);
                bRSpds[throttleIndex].setVisibility(View.GONE);
            }

            bStops[throttleIndex].setLayoutParams(stopButtonParams);
        }

        int lowerButtonsHeight = findViewById(R.id.loco_buttons_group_0).getHeight();

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            // set height of each function button area
            if (mainapp.maxThrottlesCurrentScreen==1) {
                svFnBtns[throttleIndex].getLayoutParams().height = screenHeight - lowerButtonsHeight - lUppers[throttleIndex].getHeight();
            } else {
                svFnBtns[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            }
            svFnBtns[throttleIndex].requestLayout();
            lLowers[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            lLowers[throttleIndex].requestLayout();

            // update throttle slider top/bottom

            int[] location = new int[2];
            throttleOverlay.getLocationOnScreen(location);
            int ovx = location[0];
            int ovy = location[1];

            location = new int[2];
            vsbSwitchingSpeeds[throttleIndex].getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];

            sliderTopLeftX[throttleIndex] = x - ovx;
            sliderTopLeftY[throttleIndex] = y - ovy;
            sliderBottomRightX[throttleIndex] = x + vsbSwitchingSpeeds[throttleIndex].getWidth() - ovx;
            sliderBottomRightY[throttleIndex] = y + vsbSwitchingSpeeds[throttleIndex].getHeight() -ovy;

//            Log.d(threaded_application.applicationName, activityName + ": setLabels(): slider: " + throttleIndex + " Top: " + sliderTopLeftX[throttleIndex] + ", " + sliderTopLeftY[throttleIndex]
//                    + " Bottom: " + sliderBottomRightX[throttleIndex] + ", " + sliderBottomRightY[throttleIndex]);
        }

//        // update the state of each function button based on shared variable
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            setAllFunctionStates(throttleIndex);
        }

        // Log.d(threaded_application.applicationName, activityName + ": setLabels() end");

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
        if (vsbSwitchingSpeeds == null) return;

        if (!newEnabledState) {
                vsbSwitchingSpeeds[whichThrottle].setProgress(0); // set slider to 0 if disabled
            }
        vsbSwitchingSpeeds[whichThrottle].setEnabled(newEnabledState);
    } // end of enableDisableButtons

    // helper function to enable/disable all children for a group
    @Override
    void enableDisableButtonsForView(ViewGroup vg, boolean newEnabledState) {
        // Log.d(threaded_application.applicationName, activityName + ": enableDisableButtonsForView " + newEnabledState);

        if (vg == null) { return;}
        if (mainapp.appIsFinishing) { return;}

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
        // Log.d(threaded_application.applicationName, activityName + ": set_function_states()");

        if (mainapp.appIsFinishing) { return;}

        LinkedHashMap<Integer, Button> fMap;
        fMap = functionMaps[whichThrottle];

        for (Integer f : fMap.keySet()) {
            set_function_state(whichThrottle, f);
        }
    }


    // update a function button appearance based on its state
    @Override
    void set_function_state(int whichThrottle, int function) {
        // Log.d(threaded_application.applicationName, activityName + ": set_function_request()");

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
    protected class throttleSwitchingListener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichThrottle;
        int lastSliderPosition;
        int lastDir;
//        boolean limitedJump;
        int jumpSpeed;
        int jumpDir;

        protected throttleSwitchingListener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle; // store values for this listener
            lastSliderPosition = throttleMidPointZero[whichThrottle];
            limitedJump[whichThrottle] = false;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Log.d(threaded_application.applicationName, activityName + ": onTouch(): action " + event.getAction());
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar throttle, int newSliderPosition, boolean fromUser) {
            int speed;
            int dir;

            speed = getSpeedFromSliderPosition(newSliderPosition, whichThrottle, false);
            dir = getDirectionFromSliderPosition(newSliderPosition,whichThrottle);
            lastDir = getDirectionFromSliderPosition(lastSliderPosition,whichThrottle);

//            Log.d(threaded_application.applicationName, activityName + ": onProgressChanged(): fromUser: " + fromUser + " touchFromUser: " + vsbSwitchingSpeeds[whichThrottle].touchFromUser + " limitedJump: " + limitedJump);
//            Log.d(threaded_application.applicationName, activityName + ": onProgressChanged(): isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);

            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (vsbSwitchingSpeeds[whichThrottle].touchFromUser) ) {
                if (!limitedJump[whichThrottle]) {         // touch generates multiple onProgressChanged events, skip processing after first limited jump

                    if (Math.abs(newSliderPosition - lastSliderPosition) > max_throttle_change) { // if jump is too large then limit it
                        jumpSpeed = getSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false);      // save ultimate target value
                        jumpDir = dir; // save ultimate target direction
                        limitedJump[whichThrottle] = true;
                        throttle.setProgress(lastSliderPosition);  // put the slider back to the original position
                        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

                        if (newSliderPosition < lastSliderPosition) { // going down
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.DECREMENT);
                        } else { // going up
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.INCREMENT);
                        }

                        if ((lastSliderPosition < throttleMidPointZero[whichThrottle]) && (newSliderPosition > throttleMidPointZero[whichThrottle])) { // passing from reverse to forward
                            mChangeDirectionAtZero= true;
                        } else if ((lastSliderPosition > throttleMidPointZero[whichThrottle]) && (newSliderPosition < throttleMidPointZero[whichThrottle])) { // passing from forward to reverse
                            mChangeDirectionAtZero= true;
                        }
                        repeatUpdateHandler.post(new RptUpdater(whichThrottle,prefPauseSpeedRate));

                        return;
                    }

//                    Log.d(threaded_application.applicationName, activityName + ": onProgressChanged() -- no throttling");

                    reverseDirectionIfNeeded(dir, whichThrottle);

                    speedUpdate(whichThrottle,
                            getSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false));
                    sendSpeedMsg(whichThrottle, speed);
                    setDisplayedSpeed(whichThrottle, speed);

                } else { // got a touch while processing limitJump
//                    Log.d(threaded_application.applicationName, activityName + ": onProgressChanged() -- touch while processing limited jump");
                    newSliderPosition = lastSliderPosition;    //   so suppress multiple touches
                    throttle.setProgress(lastSliderPosition);
                    if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

//                    Log.d(threaded_application.applicationName, activityName + ": onProgressChange(): fromUser: " + fromUser + " vsbSwitchingSpeeds[wt].touchFromUser: " +vsbSwitchingSpeeds[whichThrottle].touchFromUser + " isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);
                }

                // Now update ESU MCII Knob position
                if (IS_ESU_MCII) {
                    setEsuThrottleKnobPosition(whichThrottle, speed);
                }

                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference

            } else {
//                Log.d(threaded_application.applicationName, activityName + ": onProgressChanged() -- lj: " + limitedJump + " d: " + dir + " ld: " + lastDir + " ai: " + mAutoIncrement[whichThrottle] + " ad: " + mAutoDecrement + " cdaZ: " + mChangeDirectionAtZero + " s: " + speed + " js: " + jumpSpeed);
                if (limitedJump[whichThrottle]) {

                    int tempJumpSpeed = jumpSpeed;
                    if (mChangeDirectionAtZero) { tempJumpSpeed = 0; }  // we will need to change directions.  for now just get to zero

//                    Log.d(threaded_application.applicationName, activityName + ": onProgressChanged() -- lj: " + limitedJump[whichThrottle] + " d: " + dir + " ld: " + lastDir + " ai: " + mAutoIncrement[whichThrottle] + " ad: " + mAutoDecrement[whichThrottle] + " cdaZ: " + mChangeDirectionAtZero + " s: " + speed + " js: " + jumpSpeed + " tjs: " + tempJumpSpeed);

                    // check if we have hit the jumpSpeed or tempJumpSpeed (zero)
                    boolean hitJumpSpeed = false;
                    if (speed <= 0) {
                        if (jumpSpeed == 0) { hitJumpSpeed = true; }
                    } else // speed > 0
                        if (dir == direction_type.FORWARD) {
                            if (((mAutoIncrement[whichThrottle]) && (speed >= tempJumpSpeed))
                               || ((mAutoDecrement[whichThrottle]) && (speed <= tempJumpSpeed))) {
                                hitJumpSpeed = true;
                            }
                    } else if (dir == direction_type.REVERSE) {
                        if (((mAutoDecrement[whichThrottle]) && (speed >= tempJumpSpeed))
                                || ((mAutoIncrement[whichThrottle]) && (speed <= tempJumpSpeed))) {
                            hitJumpSpeed = true;
                        }
                    }

                    if ( hitJumpSpeed) {   // stop when we reach the target
                        if (mChangeDirectionAtZero) { // if change of direction is needed, then we must be at zero now.  need to continue to the final speed.
                            Log.d(threaded_application.applicationName, activityName + ": onProgressChanged() !!-- Direction change now needed");
                            mChangeDirectionAtZero = false;
                            reverseDirectionIfNeeded(jumpDir, whichThrottle);
                        } else {
                            Log.d(threaded_application.applicationName, activityName + ": onProgressChanged() !!-- LimitedJump hit jump speed.");
                            limitedJump[whichThrottle] = false;
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
                            throttle.setProgress(getNewSliderPositionFromSpeed(jumpSpeed, whichThrottle, false));
                            speedUpdate(whichThrottle, getSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false));
                        }
                    }
                }
            }
            lastSliderPosition = newSliderPosition;
        }

        @Override
        public void onStartTrackingTouch(SeekBar sb) {
//            Log.d(threaded_application.applicationName, activityName + ": onStartTrackingTouch() onProgressChanged");
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sb) {
//            Log.d(threaded_application.applicationName, activityName + ": onStopTrackingTouch() onProgressChanged");
            limitedJump[whichThrottle] = false;
            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
            kidsTimerActions(kids_timer_action_type.STARTED,0);
        }
    }

    VerticalSeekBar getSwitchingThrottleSlider(int whichThrottle) {
        return vsbSwitchingSpeeds[whichThrottle];
    }


    // change speed slider by scaled display unit value
    @Override
    int speedChange(int whichThrottle, int change) {

        VerticalSeekBar switchingThrottleSlider = getSwitchingThrottleSlider(whichThrottle);
//        double displayUnitScale = getDisplayUnitScale(whichThrottle);
        int lastSliderPosition = switchingThrottleSlider.getProgress();
        int lastScaleSpeed;
        int scaleSpeed;
        int speed;
//        int lastSpeed;

        if (getDirection(whichThrottle)==direction_type.REVERSE) {  // treat negative as positive
            change = change * -1;
        }
//        Log.d(threaded_application.applicationName, activityName + ": speedChange(): change: " + change + " lastSliderPosition: " + lastSliderPosition);

        lastScaleSpeed = getSpeedFromSliderPosition(lastSliderPosition, whichThrottle, true);
        scaleSpeed = lastScaleSpeed + change;
//        lastSpeed = getSpeedFromSliderPosition(lastSliderPosition, whichThrottle, false);

         if ((lastScaleSpeed>0) && (scaleSpeed<0)) {   // force a zero speed at least once when changing directions
             scaleSpeed = 0;
         }

//        Log.d(threaded_application.applicationName, activityName + ": speedChange(): lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) );
        if (scaleSpeed<0) {
            int dir = getDirection(whichThrottle) == direction_type.FORWARD ? direction_type.REVERSE : direction_type.FORWARD;
//            Log.d(threaded_application.applicationName, activityName + ": speedChange(): auto Reverse - dir:" + dir);
            dirs[whichThrottle] = dir;
            setEngineDirection(whichThrottle, dir, false);
            showDirectionIndication(whichThrottle, dir);
        }
//        Log.d(threaded_application.applicationName, activityName + ": speedChange(): lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) );
//        Log.d(threaded_application.applicationName, activityName + ": speedChange(): change: " + change);

        scaleSpeed = Math.abs(scaleSpeed);

        int newSliderPosition = getNewSliderPositionFromSpeed(scaleSpeed, whichThrottle, true);
//        Log.d(threaded_application.applicationName, activityName + ": speedChange(): newSliderPosition: " + newSliderPosition );

        if (lastScaleSpeed == scaleSpeed) {
            newSliderPosition += Math.signum(change);
        }

        if (newSliderPosition < 0)  //insure speed is inside bounds
            newSliderPosition = 0;

        if (newSliderPosition > throttleSwitchingMax[whichThrottle])
            newSliderPosition = throttleSwitchingMax[whichThrottle];

//        Log.d(threaded_application.applicationName, activityName + ": speedChange(): lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) + " newSliderPosition: " + newSliderPosition);
//        Log.d(threaded_application.applicationName, activityName + ": speedChange(): change: " + change);

        switchingThrottleSlider.setProgress(newSliderPosition);
        speed = Math.abs(getSpeedFromSliderPosition(newSliderPosition, whichThrottle, false));
        setDisplayedSpeed(whichThrottle, speed);
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

//        Log.d(threaded_application.applicationName, activityName + ": speedChange():  speed: " + speed + " change: " + change);

//        speedUpdateAndNotify(whichThrottle, speed);
        return speed;

    } // end speedChange


    @Override
    void speedUpdate(int whichThrottle, int speed) {
        super.speedUpdate(whichThrottle, speed);

        int sliderPosition = getNewSliderPositionFromSpeed(speed, whichThrottle, false);

        if (sliderPosition < 0)  //insure speed is inside bounds
            sliderPosition = 0;

        if (sliderPosition > throttleSwitchingMax[whichThrottle])
            sliderPosition = throttleSwitchingMax[whichThrottle];

        getSwitchingThrottleSlider(whichThrottle).setProgress(sliderPosition);
        setDisplayedSpeed(whichThrottle, speed);
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

//        Log.d(threaded_application.applicationName, activityName + ": speedUpdate():  sliderPosition: " + sliderPosition + " dir: " + getDirection(whichThrottle) + " Speed: " + speed);
    }

    // process WiT speed report
    // update speed slider if didn't just send a speed update to WiT
    @Override
    void speedUpdateWiT(int whichThrottle, int speedWiT) {
        super.speedUpdateWiT(whichThrottle,speedWiT);

        if (speedWiT < 0)
            speedWiT = 0;

        int sliderPosition;
        if (!changeTimers[whichThrottle].isDelayInProgress()) {
            sliderPosition = getNewSliderPositionFromSpeed(speedWiT, whichThrottle, false);
            vsbSwitchingSpeeds[whichThrottle].setProgress(sliderPosition);
        }
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
    }


    @Override
    boolean changeDirectionIfAllowed(int whichThrottle, int direction) {
        if (getDirection(whichThrottle) != direction) {

            showDirectionRequest(whichThrottle, direction);        // update requested direction indication
            setEngineDirection(whichThrottle, direction, false);   // update direction for each engine on this throttle

            int lastSliderPosition = vsbSwitchingSpeeds[whichThrottle].getProgress();
            int lastScaleSpeed;

            lastScaleSpeed = getSpeedFromSliderPosition(lastSliderPosition, whichThrottle, true);

            speedUpdate(whichThrottle,lastScaleSpeed);
        }
        return (getDirection(whichThrottle) == direction);
    }

    int getDisplaySpeedFromCurrentSliderPosition(int whichThrottle, boolean useScale) {
        return getSpeedFromCurrentSliderPosition(whichThrottle, useScale);
    }

    int getSpeedFromCurrentSliderPosition(int whichThrottle, boolean useScale) {
        int speed = 0;
        if (vsbSwitchingSpeeds[whichThrottle].isEnabled()) {
            speed = getSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(), whichThrottle, useScale);
        }
        return speed;
    }

    int getSpeedFromSliderPosition(int sliderPosition, int whichThrottle, boolean useScale) {
        int speed;

        double scale = 1;
        if (useScale) {
            scale = getDisplayUnitScale(whichThrottle);
        }

        if (sliderPosition >= (throttleMidPointDeadZoneUpper[whichThrottle])) { //forward
            speed = (int) Math.round((sliderPosition - throttleMidPointDeadZoneUpper[whichThrottle]) * scale);
        } else if (sliderPosition <= (throttleMidPointDeadZoneLower[whichThrottle])) { // reverse
            speed = (int) Math.round((throttleMidPointDeadZoneLower[whichThrottle] - sliderPosition) * scale);
        } else { // zero - deadzone
            speed = 0;
        }
//        Log.d(threaded_application.applicationName, activityName + ": getSpeedFromSliderPosition():  scale: " + scale + " sliderPosition: " + sliderPosition + " speed: " + speed );
        return speed;
    }

    int getDirectionFromSliderPosition(int sliderPosition, int whichThrottle) {
        int dir;

        if (sliderPosition >= (throttleMidPointDeadZoneUpper[whichThrottle])) { //forward
            dir = direction_type.FORWARD;
        } else if (sliderPosition <= (throttleMidPointDeadZoneLower[whichThrottle])) { // reverse
            dir = direction_type.REVERSE;
        } else { // zero - deadzone
            dir = direction_type.FORWARD;
        }
        return dir;
    }

    void reverseDirectionIfNeeded(int dir, int whichThrottle) {
        if (dirs[whichThrottle]!=dir) {
            dirs[whichThrottle] = dir;
            setEngineDirection(whichThrottle, dir, false);
            showDirectionIndication(whichThrottle, dir);
        }
    }

    int getNewSliderPositionFromSpeed(int speed, int whichThrottle, boolean useScale) {
        int newSliderPosition;
        double scale = getDisplayUnitScale(whichThrottle);
        if (!useScale) { scale = 1;}

        if (speed==0) {
            newSliderPosition = throttleMidPointZero[whichThrottle];
        } else {
            if (getDirection(whichThrottle) == direction_type.FORWARD) {
                newSliderPosition = throttleMidPointDeadZoneUpper[whichThrottle] + (int) Math.round( speed / scale );
            } else {
                newSliderPosition = throttleMidPointDeadZoneLower[whichThrottle] - (int) Math.round( speed / scale );
            }
        }
//        Log.d(threaded_application.applicationName, activityName + ": getNewSliderPositionFromSpeed():  scale: " + scale + " speed: " + speed + " newSliderPosition: " + newSliderPosition );

        return newSliderPosition;
    }

    // set the displayed numeric speed value
    @Override
    protected void setDisplayedSpeed(int whichThrottle, int speed) {
        setDisplayedSpeedWithDirection(whichThrottle, speed);
    }

    //listeners for the Limit Speed Button
    protected class limit_speed_button_switching_touch_listener implements View.OnTouchListener {
        int whichThrottle;

        protected limit_speed_button_switching_touch_listener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                vsbSwitchingSpeeds[whichThrottle].touchFromUser = false;
                limitSpeed(whichThrottle);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mainapp.buttonVibration();
            }
            return false;
        }
    }

    protected void limitSpeed(int whichThrottle) {
        int dir = getDirection(whichThrottle);
        int speed = getSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle, false);
//                Log.d(threaded_application.applicationName, activityName + ": limitSpeed():  speed: " + speed );

        isLimitSpeeds[whichThrottle] = !isLimitSpeeds[whichThrottle];
        if (isLimitSpeeds[whichThrottle]) {
            bLimitSpeeds[whichThrottle].setSelected(true);
            limitSpeedSliderScalingFactors[whichThrottle] = 100/ ((float) prefLimitSpeedPercent);
            sbs[whichThrottle].setMax( Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]));

            throttleMidPointZero[whichThrottle] = (Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]) + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[whichThrottle] = (Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]) + prefSwitchingThrottleSliderDeadZone) * 2;
            vsbSwitchingSpeeds[whichThrottle].setMax(throttleSwitchingMax[whichThrottle]);

        } else {
            bLimitSpeeds[whichThrottle].setSelected(false);
            sbs[whichThrottle].setMax(maxThrottle);

            throttleMidPointZero[whichThrottle] = (maxThrottle + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[whichThrottle] = (maxThrottle + prefSwitchingThrottleSliderDeadZone) * 2;
            vsbSwitchingSpeeds[whichThrottle].setMax(throttleSwitchingMax[whichThrottle]);
        }
        throttleMidPointDeadZoneUpper[whichThrottle] = throttleMidPointZero[whichThrottle] + prefSwitchingThrottleSliderDeadZone;
        throttleMidPointDeadZoneLower[whichThrottle] = throttleMidPointZero[whichThrottle] - prefSwitchingThrottleSliderDeadZone;

        speedUpdate(whichThrottle,  speed);
        setEngineDirection(whichThrottle, dir, false);

        Log.d(threaded_application.applicationName, activityName + ": limitSpeed():  speed: " + speed );
        speedChangeAndNotify(whichThrottle,0);
        setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
    }
}