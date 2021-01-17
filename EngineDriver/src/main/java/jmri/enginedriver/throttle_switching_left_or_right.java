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

public class throttle_switching_left_or_right extends throttle {

    protected static final int MAX_SCREEN_THROTTLES = 2;
    protected static final int MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT = 1;

    private LinearLayout[] lThrottles;
    private LinearLayout[] lUppers;
    private LinearLayout[] lLowers;
    private LinearLayout[] lSpeeds;
    private ScrollView[] svFnBtns;

    private static final int TICK_TYPE_0_100 = 0;
    private static final int TICK_TYPE_0_100_0 = 1;

    private int[] throttleMidPointZero = {0,0};
    private int[] throttleSwitchingMax = {0,0};

    private int[] throttleMidPointDeadZoneUpper = {0,0};
    private int[] throttleMidPointDeadZoneLower = {0,0};

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

        maxThrottlePcnt = preferences.getIntPrefValue(prefs, "maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (0.01 * maxThrottlePcnt)); // convert from percent

        prefSwitchingThrottleSliderDeadZone = Integer.parseInt(prefs.getString("prefSwitchingThrottleSliderDeadZone", getResources().getString(R.string.prefSwitchingThrottleSliderDeadZoneDefaultValue)));

    }

    @SuppressLint({"Recycle", "SetJavaScriptEnabled"})
    @Override
    public void onCreate(Bundle savedInstanceState) {

        mainapp = (threaded_application) this.getApplication();

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        switch (prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault))) {
            case "Switching":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
                mainapp.throttleLayoutViewId = R.layout.throttle_switching;
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

        super.onCreate(savedInstanceState);

        if (mainapp.appIsFinishing) { return;}

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            switch (throttleIndex) {
                case 0:
                    fbs[throttleIndex] = findViewById(R.id.function_buttons_table_0);
                    break;
                case 1:
                    fbs[throttleIndex] = findViewById(R.id.function_buttons_table_1);
                    break;
            }

        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            throttleMidPointZero[throttleIndex] =(maxThrottle + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[throttleIndex] = (maxThrottle + prefSwitchingThrottleSliderDeadZone) * 2;
            throttleMidPointDeadZoneUpper[throttleIndex] = throttleMidPointZero[throttleIndex] + prefSwitchingThrottleSliderDeadZone;
            throttleMidPointDeadZoneLower[throttleIndex] = throttleMidPointZero[throttleIndex] - prefSwitchingThrottleSliderDeadZone;
//        throttleReScale = ((throttleMidPointZero * 2)) / (double) throttleMidPointDeadZoneLower;
        }

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lSpeeds = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        svFnBtns = new ScrollView[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        vsbSwitchingSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        lUppers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lLowers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            switch (throttleIndex) {
                default:
                case 0:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_0);
                    lUppers[throttleIndex] = findViewById(R.id.loco_upper_0);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_0);
                    lSpeeds[throttleIndex] = findViewById(R.id.throttle_0_SetSpeed);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_0);
                    vsbSwitchingSpeeds[throttleIndex] = findViewById(R.id.speed_switching_0);
                    vsbSwitchingSpeeds[throttleIndex].setTickType(TICK_TYPE_0_100_0);
//                    vsbSwitchingSpeeds[throttleIndex].setMax(MAX_SPEED_VAL_WIT);
                    vsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
                    vsbSwitchingSpeeds[throttleIndex].setProgress(throttleMidPointZero[throttleIndex]);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_0);
                    break;
                case 1:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_1);
                    lUppers[throttleIndex] = findViewById(R.id.loco_upper_1);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_1);
                    lSpeeds[throttleIndex] = findViewById(R.id.throttle_1_SetSpeed);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_1);
                    vsbSwitchingSpeeds[throttleIndex] = findViewById(R.id.speed_switching_1);
                    vsbSwitchingSpeeds[throttleIndex].setTickType(TICK_TYPE_0_100_0);
//                    vsbSwitchingSpeeds[throttleIndex].setMax(MAX_SPEED_VAL_WIT);
                    vsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
                    vsbSwitchingSpeeds[throttleIndex].setProgress(throttleMidPointZero[throttleIndex]);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_1);
                    break;
            }
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            sbs[throttleIndex].setMax(Math.round(maxThrottle));
            vsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
        }

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        limit_speed_button_switching_touch_listener lsstl;
        Button bLimitSpeed = findViewById(R.id.limit_speed_0);
        pause_speed_button_switching_touch_listener psstl;
        Button bPauseSpeed = findViewById(R.id.pause_speed_0);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            switch (throttleIndex) {
                case 0:
                    bLimitSpeed = findViewById(R.id.limit_speed_0);
                    bPauseSpeed = findViewById(R.id.pause_speed_0);
                    break;
                case 1:
                    bLimitSpeed = findViewById(R.id.limit_speed_1);
                    bPauseSpeed = findViewById(R.id.pause_speed_1);
                    break;

            }
            bLimitSpeeds[throttleIndex] = bLimitSpeed;
            limitSpeedSliderScalingFactors[throttleIndex] = 1;
            lsstl = new limit_speed_button_switching_touch_listener(throttleIndex);
            bLimitSpeeds[throttleIndex].setOnTouchListener(lsstl);
            isLimitSpeeds[throttleIndex] = false;
            if (!prefLimitSpeedButton) {
                bLimitSpeed.setVisibility(View.GONE);
            }

            bPauseSpeeds[throttleIndex] = bPauseSpeed;
            psstl = new pause_speed_button_switching_touch_listener(throttleIndex);
            bPauseSpeeds[throttleIndex].setOnTouchListener(psstl);
            isPauseSpeeds[throttleIndex] = PAUSE_SPEED_INACTIVE;
            if (!prefPauseSpeedButton) {
                bPauseSpeed.setVisibility(View.GONE);
            }
        }


        throttleSwitchingListener thsl;

        for (int i=0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            // set up listeners for all throttles
            thsl = new throttleSwitchingListener(i);
            vsbSwitchingSpeeds[i].setOnSeekBarChangeListener(thsl);
            vsbSwitchingSpeeds[i].setOnTouchListener(thsl);

        }
        sliderType = SLIDER_TYPE_SWITCHING;
    } // end of onCreate()

    @Override
    public void onResume() {
        super.onResume();

        if (mainapp.appIsFinishing) { return;}

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            if( throttleIndex < mainapp.numThrottles) {
                lThrottles[throttleIndex].setVisibility(LinearLayout.VISIBLE);
            } else {
                lThrottles[throttleIndex].setVisibility(LinearLayout.GONE);
            }

//             show or hide the limit speed buttons
            if (!prefLimitSpeedButton) {
                bLimitSpeeds[throttleIndex].setVisibility(View.GONE);
            } else {
                bLimitSpeeds[throttleIndex].setVisibility(View.VISIBLE);
            }

//             show or hide the pause speed buttons
            if (!prefPauseSpeedButton) {
//            if ((!prefPauseSpeedButton) || (prefHideSlider)) {
                bPauseSpeeds[throttleIndex].setVisibility(View.GONE);
            } else {
                bPauseSpeeds[throttleIndex].setVisibility(View.VISIBLE);
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
                    if (!overrideThrottleNames[throttleIndex].equals("")) {
                        bLabel = overrideThrottleNames[throttleIndex];
                        bLabelPlainText = overrideThrottleNames[throttleIndex];
                    } else {
                        bLabel = mainapp.consists[throttleIndex].toHtml();
                        bLabelPlainText = mainapp.consists[throttleIndex].toString();
                    }

//                    bLabel = mainapp.consists[throttleIndex].toString();
//                    bLabelPlainText = mainapp.consists[throttleIndex].toString();
//                    bLabel = mainapp.consists[throttleIndex].toHtml();
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
                // whichVolume = 'S'; //set the next throttle to use volume control
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
            setImmersiveModeOn(webView);
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


        final DisplayMetrics dm = getResources().getDisplayMetrics();
        // Get the screen's density scale
        final float denScale = dm.density;

//        int screenWidth = vThrotScrWrap.getWidth(); // get the width of usable area
//        int throttleWidth = (screenWidth - (int) (denScale * 6)) / mainapp.numThrottles;
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex].getLayoutParams().height = LinearLayout.LayoutParams.FILL_PARENT;
//            lThrottles[throttleIndex].getLayoutParams().width = throttleWidth;
            lThrottles[throttleIndex].requestLayout();

//            lSpeeds[throttleIndex].getLayoutParams().width = throttleWidth - svFnBtns[throttleIndex].getWidth();
            lSpeeds[throttleIndex].requestLayout();
        }

        int screenHeight = vThrotScrWrap.getHeight(); // get the Height of usable area
        int keepHeight = screenHeight;  // default height
        if (screenHeight == 0) {
            // throttle screen hasn't been drawn yet, so use display metrics for now
            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
            //Log.d("Engine_Driver","vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
        }

        if (webView!=null) {
            setImmersiveModeOn(webView);
        }

        // save part the screen for webview
        if (webViewLocation.equals(WEB_VIEW_LOCATION_TOP) || webViewLocation.equals(WEB_VIEW_LOCATION_BOTTOM)) {
            webViewIsOn = true;
            if (!prefIncreaseWebViewSize) {
                // save half the screen
                screenHeight *= 0.5;
            } else {
                // save 60% of the screen
                if (webViewLocation.equals(WEB_VIEW_LOCATION_BOTTOM)) {
                    screenHeight *= 0.40;
                } else {
                    screenHeight *= 0.60;
                }
            }
        }

        ImageView myImage = findViewById(R.id.backgroundImgView);
        myImage.getLayoutParams().height = screenHeight;

        int speedButtonHeight = (int) (50 * denScale);
        if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
            speedButtonHeight = (int) ((screenHeight - (200 * denScale)) / 2);
        }

        LinearLayout.LayoutParams stopButtonParams;
        stopButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
        stopButtonParams.topMargin = (int) (speedButtonHeight * 0.5);
        stopButtonParams.height = (int) (speedButtonHeight * 0.8);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            //show speed buttons based on pref
            if (prefs.getBoolean("display_speed_arrows_buttons", false)) {
                bLSpds[throttleIndex].setVisibility(View.VISIBLE);
                bRSpds[throttleIndex].setVisibility(View.VISIBLE);

                bLSpds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.FILL_PARENT;
                bLSpds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bLSpds[throttleIndex].requestLayout();
                bRSpds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.FILL_PARENT;
                bRSpds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bRSpds[throttleIndex].requestLayout();
            } else {
                bLSpds[throttleIndex].setVisibility(View.GONE);
                bRSpds[throttleIndex].setVisibility(View.GONE);
            }
            //bLSpds[throttleIndex].setText(speedButtonLeftText);
            //bRSpds[throttleIndex].setText(speedButtonRightText);

//            bStops[throttleIndex].getLayoutParams().height = (int) (speedButtonHeight * 0.8);
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
            ov.getLocationOnScreen(location);
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
        bLimitSpeeds[whichThrottle].setEnabled(newEnabledState);
        bPauseSpeeds[whichThrottle].setEnabled(newEnabledState);

        super.enable_disable_buttons(whichThrottle, forceDisable);

        if (!newEnabledState) {
                vsbSwitchingSpeeds[whichThrottle].setProgress(0); // set slider to 0 if disabled
            }
        vsbSwitchingSpeeds[whichThrottle].setEnabled(newEnabledState);
    } // end of enable_disable_buttons

    // helper function to enable/disable all children for a group
    @Override
    void enable_disable_buttons_for_view(ViewGroup vg, boolean newEnabledState) {
        // Log.d("Engine_Driver","starting enable_disable_buttons_for_view " +
        // newEnabledState);

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
    } // enable_disable_buttons_for_view

    // update the appearance of all function buttons
    @Override
    void set_all_function_states(int whichThrottle) {
        // Log.d("Engine_Driver","set_function_states");

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

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Log.d("Engine_Driver", "onTouchThrottle action " + event.getAction());
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

//            Log.d("Engine_Driver", "onProgressChanged: fromUser: " + fromUser + " touchFromUser: " + vsbSwitchingSpeeds[whichThrottle].touchFromUser + " limitedJump: " + limitedJump);
//            Log.d("Engine_Driver", "onProgressChanged: isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);

            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (vsbSwitchingSpeeds[whichThrottle].touchFromUser) ) {
//            if ((fromUser) || (vsbSwitchingSpeeds[whichThrottle].touchFromUser)
//                || (isPauseSpeeds[whichThrottle] == PAUSE_SPEED_START_TO_ZERO)
//                || (isPauseSpeeds[whichThrottle] == PAUSE_SPEED_START_RETURN) ) {

                if (!limitedJump[whichThrottle]) {         // touch generates multiple onProgressChanged events, skip processing after first limited jump

                    if (Math.abs(newSliderPosition - lastSliderPosition) > max_throttle_change) { // if jump is too large then limit it
//                    if ((Math.abs(newSliderPosition - lastSliderPosition) > max_throttle_change) // if jump is too large then limit it
//                    || (isPauseSpeeds[whichThrottle]!=PAUSE_SPEED_INACTIVE)){
//                        Log.d("Engine_Driver", "onProgressChanged -- throttling change");

                        jumpSpeed = getSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false);      // save ultimate target value
                        jumpDir = dir; // save ultimate target direction
                        limitedJump[whichThrottle] = true;
                        throttle.setProgress(lastSliderPosition);  // put the slider back to the original position

                        if (newSliderPosition < lastSliderPosition) { // going down
                            setAutoIncrementDecrement(whichThrottle, AUTO_INCREMENT_DECREMENT_DECREMENT);
                        } else { // going up
                            setAutoIncrementDecrement(whichThrottle, AUTO_INCREMENT_DECREMENT_INCREMENT);
                        }

                        if ((lastSliderPosition < throttleMidPointZero[whichThrottle]) && (newSliderPosition > throttleMidPointZero[whichThrottle])) { // passing from reverse to forward
                            mChangeDirectionAtZero= true;
                        } else if ((lastSliderPosition > throttleMidPointZero[whichThrottle]) && (newSliderPosition < throttleMidPointZero[whichThrottle])) { // passing from forward to reverse
                            mChangeDirectionAtZero= true;
                        }

//                        if (isPauseSpeeds[whichThrottle]==PAUSE_SPEED_INACTIVE) {
//                            repeatUpdateHandler.post(new RptUpdater(whichThrottle,0));
//                        } else {
//                            if (isPauseSpeeds[whichThrottle]==PAUSE_SPEED_START_TO_ZERO) {
//                                isPauseSpeeds[whichThrottle]=PAUSE_SPEED_TO_ZERO;
//                            } else {
//                                isPauseSpeeds[whichThrottle]=PAUSE_SPEED_TO_RETURN;
//                                jumpSpeed = pauseSpeed[whichThrottle];  // the recorded thumb position may be slightly out
//                            }
                            repeatUpdateHandler.post(new RptUpdater(whichThrottle,prefPauseSpeedRate));
//                        }
                        return;
                    }

//                    Log.d("Engine_Driver", "onProgressChanged -- no throttling");

                    reverseDirectionIfNeeded(dir, whichThrottle);

                    speedUpdate(whichThrottle,
                            getSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false));
                    sendSpeedMsg(whichThrottle, speed);
                    setDisplayedSpeed(whichThrottle, speed);

                } else { // got a touch while processing limitJump
//                    Log.d("Engine_Driver", "onProgressChanged -- touch while processing limited jump");
                    newSliderPosition = lastSliderPosition;    //   so suppress multiple touches
                    throttle.setProgress(lastSliderPosition);

//                    Log.d("Engine_Driver", "onProgressChange: fromUser: " + fromUser + " vsbSwitchingSpeeds[wt].touchFromUser: " +vsbSwitchingSpeeds[whichThrottle].touchFromUser + " isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);
                }

                // Now update ESU MCII Knob position
                if (IS_ESU_MCII) {
                    setEsuThrottleKnobPosition(whichThrottle, speed);
                }

                setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference

            } else {
//                Log.d("Engine_Driver", "onProgressChanged -- lj: " + limitedJump + " d: " + dir + " ld: " + lastDir + " ai: " + mAutoIncrement[whichThrottle] + " ad: " + mAutoDecrement + " cdaZ: " + mChangeDirectionAtZero + " s: " + speed + " js: " + jumpSpeed);
                if (limitedJump[whichThrottle]) {

                    int tempJumpSpeed = jumpSpeed;
                    if (mChangeDirectionAtZero) { tempJumpSpeed = 0; }  // we will need to change directions.  for now just get to zero

//                    Log.d("Engine_Driver", "onProgressChanged -- lj: " + limitedJump[whichThrottle] + " d: " + dir + " ld: " + lastDir + " ai: " + mAutoIncrement[whichThrottle] + " ad: " + mAutoDecrement[whichThrottle] + " cdaZ: " + mChangeDirectionAtZero + " s: " + speed + " js: " + jumpSpeed + " tjs: " + tempJumpSpeed);

                    // check if we have hit the jumpSpeed or tempJumpSpeed (zero)
                    boolean hitJumpSpeed = false;
                    if (speed <= 0) {
                        if (jumpSpeed == 0) { hitJumpSpeed = true; }
                    } else // speed > 0
                        if (dir==DIRECTION_FORWARD) {
                            if (((mAutoIncrement[whichThrottle]) && (speed >= tempJumpSpeed))
                               || ((mAutoDecrement[whichThrottle]) && (speed <= tempJumpSpeed))) {
                                hitJumpSpeed = true;
                            }
                    } else if (dir==DIRECTION_REVERSE) {
                        if (((mAutoDecrement[whichThrottle]) && (speed >= tempJumpSpeed))
                                || ((mAutoIncrement[whichThrottle]) && (speed <= tempJumpSpeed))) {
                            hitJumpSpeed = true;
                        }
                    }

                    if ( hitJumpSpeed) {   // stop when we reach the target
//                        switch (isPauseSpeeds[whichThrottle]) {
//                            case PAUSE_SPEED_TO_ZERO: isPauseSpeeds[whichThrottle] = PAUSE_SPEED_ZERO; break;
//                            case PAUSE_SPEED_TO_RETURN: isPauseSpeeds[whichThrottle] = PAUSE_SPEED_INACTIVE; break;
//                        }

                        if (mChangeDirectionAtZero) { // if change of direction is needed, then we must be at zero now.  need to continue to the final speed.
                            Log.d("Engine_Driver", "onProgressChanged !!-- Direction change now needed");
                            mChangeDirectionAtZero = false;
                            reverseDirectionIfNeeded(jumpDir, whichThrottle);
                        } else {
                            Log.d("Engine_Driver", "onProgressChanged !!-- LimitedJump hit jump speed.");
                            limitedJump[whichThrottle] = false;
                            setAutoIncrementDecrement(whichThrottle, AUTO_INCREMENT_DECREMENT_OFF);
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
//            Log.d("Engine_Driver", "onStartTrackingTouch() onProgressChanged");
            gestureInProgress = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sb) {
//            Log.d("Engine_Driver", "onStopTrackingTouch() onProgressChanged");
            limitedJump[whichThrottle] = false;
            setAutoIncrementDecrement(whichThrottle, AUTO_INCREMENT_DECREMENT_OFF);
            kidsTimerActions(KIDS_TIMER_STARTED,0);
        }
    }

    VerticalSeekBar getSwitchingThrottleSlider(int whichThrottle) {
        return vsbSwitchingSpeeds[whichThrottle];
    }


    // change speed slider by scaled display unit value
    @Override
    int speedChange(int whichThrottle, int change) {

        VerticalSeekBar switchingThrottleSlider = getSwitchingThrottleSlider(whichThrottle);
        double displayUnitScale = getDisplayUnitScale(whichThrottle);
        int lastSliderPosition = switchingThrottleSlider.getProgress();
        int lastScaleSpeed;
        int scaleSpeed;
        int speed;
        int lastSpeed;

        if (getDirection(whichThrottle)==DIRECTION_REVERSE) {  // treat negative as positive
            change = change * -1;
        }
//        Log.d("Engine_Driver", "throttle_switching_left_or_right - change: " + change + " lastSliderPosition: " + lastSliderPosition);

        lastScaleSpeed = getSpeedFromSliderPosition(lastSliderPosition, whichThrottle, true);
        scaleSpeed = lastScaleSpeed + change;
//        lastSpeed = getSpeedFromSliderPosition(lastSliderPosition, whichThrottle, false);

         if ((lastScaleSpeed>0) && (scaleSpeed<0)) {   // force a zero speed at least once when changing directions
             scaleSpeed = 0;
         }

//        Log.d("Engine_Driver", "throttle_switching_left_or_right - speedChange - lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) );
        if (scaleSpeed<0) {
            int dir = getDirection(whichThrottle) == DIRECTION_FORWARD ? DIRECTION_REVERSE : DIRECTION_FORWARD;
//            Log.d("Engine_Driver", "throttle_switching_left_or_right - speedChange - auto Reverse - dir:" + dir);
            dirs[whichThrottle] = dir;
            setEngineDirection(whichThrottle, dir, false);
            showDirectionIndication(whichThrottle, dir);
        }
//        Log.d("Engine_Driver", "throttle_switching_left_or_right - speedChange - lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) );
//        Log.d("Engine_Driver","throttle_switching_left_or_right - speedChange - change: " + change);

        scaleSpeed = Math.abs(scaleSpeed);

        int newSliderPosition = getNewSliderPositionFromSpeed(scaleSpeed, whichThrottle, true);
//        Log.d("Engine_Driver", "throttle_switching_left_or_right - newSliderPosition: " + newSliderPosition );

        if (lastScaleSpeed == scaleSpeed) {
            newSliderPosition += Math.signum(change);
        }

        if (newSliderPosition < 0)  //insure speed is inside bounds
            newSliderPosition = 0;

        if (newSliderPosition > throttleSwitchingMax[whichThrottle])
            newSliderPosition = throttleSwitchingMax[whichThrottle];

//        Log.d("Engine_Driver", "throttle_switching_left_or_right - speedChange - lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) + " newSliderPosition: " + newSliderPosition);
//        Log.d("Engine_Driver","throttle_switching_left_or_right - speedChange - change: " + change);

        switchingThrottleSlider.setProgress(newSliderPosition);
        speed = Math.abs(getSpeedFromSliderPosition(newSliderPosition, whichThrottle, false));
        setDisplayedSpeed(whichThrottle, speed);

//        Log.d("Engine_Driver","throttle_switching_left_or_right - speedChange -  speed: " + speed + " change: " + change);

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

//        Log.d("Engine_Driver","throttle_switching_left_or_right - speedUpdate -  sliderPosition: " + sliderPosition + " dir: " + getDirection(whichThrottle) + " Speed: " + speed);
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
//        Log.d("Engine_Driver","throttle_switching_left_or_right - getSpeedFromSliderPosition -  scale: " + scale + " sliderPosition: " + sliderPosition + " speed: " + speed );
        return speed;
    }

    int getDirectionFromSliderPosition(int sliderPosition, int whichThrottle) {
        int dir;

        if (sliderPosition >= (throttleMidPointDeadZoneUpper[whichThrottle])) { //forward
            dir = DIRECTION_FORWARD;
        } else if (sliderPosition <= (throttleMidPointDeadZoneLower[whichThrottle])) { // reverse
            dir = DIRECTION_REVERSE;
        } else { // zero - deadzone
            dir = DIRECTION_FORWARD;
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
            if (getDirection(whichThrottle) == DIRECTION_FORWARD) {
                newSliderPosition = throttleMidPointDeadZoneUpper[whichThrottle] + (int) Math.round( speed / scale );
            } else {
                newSliderPosition = throttleMidPointDeadZoneLower[whichThrottle] - (int) Math.round( speed / scale );
            }
        }
//        Log.d("Engine_Driver","throttle_switching_left_or_right - getNewSliderPositionFromSpeed -  scale: " + scale + " speed: " + speed + " newSliderPosition: " + newSliderPosition );

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

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                limitSpeed(whichThrottle);
            }
            return false;
        }
    }

    protected void limitSpeed(int whichThrottle) {
        int dir = getDirection(whichThrottle);
        int speed = getSpeedFromSliderPosition(vsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle, false);
//                Log.d("Engine_Driver","limit_speed_button_switching_touch_listener -  speed: " + speed );

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

//                    throttleMidPointZero = (MAX_SPEED_VAL_WIT + prefSwitchingThrottleSliderDeadZone);
//                    throttleSwitchingMax = (MAX_SPEED_VAL_WIT + prefSwitchingThrottleSliderDeadZone) * 2;
            throttleMidPointZero[whichThrottle] = (maxThrottle + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[whichThrottle] = (maxThrottle + prefSwitchingThrottleSliderDeadZone) * 2;
            vsbSwitchingSpeeds[whichThrottle].setMax(throttleSwitchingMax[whichThrottle]);
        }
        throttleMidPointDeadZoneUpper[whichThrottle] = throttleMidPointZero[whichThrottle] + prefSwitchingThrottleSliderDeadZone;
        throttleMidPointDeadZoneLower[whichThrottle] = throttleMidPointZero[whichThrottle] - prefSwitchingThrottleSliderDeadZone;

        speedUpdate(whichThrottle,  speed);
        setEngineDirection(whichThrottle, dir, false);

        Log.d("Engine_Driver","limitSpeed -  speed: " + speed );
        speedChangeAndNotify(whichThrottle,0);
        setActiveThrottle(whichThrottle); // set the throttle the volmue keys control depending on the preference
    }

    //listeners for the Pause Speed Button
    protected class pause_speed_button_switching_touch_listener implements View.OnTouchListener {
        int whichThrottle;

        protected pause_speed_button_switching_touch_listener(int new_whichThrottle) {
            whichThrottle = new_whichThrottle;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                pauseSpeed(whichThrottle);
            }
            return false;
        }
    }

    protected void pauseSpeed(int whichThrottle) {
        int speed = 0;

        switch (isPauseSpeeds[whichThrottle]) {
            case PAUSE_SPEED_ZERO: {
                isPauseSpeeds[whichThrottle] = PAUSE_SPEED_START_RETURN;
                bPauseSpeeds[whichThrottle].setSelected(false);
                speed = getSpeed(whichThrottle);
                break;
            }
            case PAUSE_SPEED_INACTIVE: {
                if (getSpeed(whichThrottle) != 0) {
                    isPauseSpeeds[whichThrottle] = PAUSE_SPEED_START_TO_ZERO;
                    bPauseSpeeds[whichThrottle].setSelected(true);
                    pauseSpeed[whichThrottle] = getSpeed(whichThrottle);
                    pauseDir[whichThrottle] = getDirection(whichThrottle);
                    speed = 0;
                } else {
                    return;
                }
                break;
            }
            case PAUSE_SPEED_TO_RETURN:
            case PAUSE_SPEED_TO_ZERO:
            default: {
                setAutoIncrementDecrement(whichThrottle,AUTO_INCREMENT_DECREMENT_OFF);
                bPauseSpeeds[whichThrottle].setSelected(false);
                isPauseSpeeds[whichThrottle] = PAUSE_SPEED_INACTIVE;
                limitedJump[whichThrottle] = false;
                break;
            }
        }
        if (isPauseSpeeds[whichThrottle] != PAUSE_SPEED_INACTIVE) {
            setSpeed(whichThrottle, speed, SPEED_COMMAND_FROM_BUTTONS);
        }
    }
}