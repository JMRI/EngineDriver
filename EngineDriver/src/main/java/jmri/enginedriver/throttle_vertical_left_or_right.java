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
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.LinkedHashMap;

// for changing the screen brightness

// used for supporting Keyboard and Gamepad input;

public class throttle_vertical_left_or_right extends throttle {

    protected static final int MAX_SCREEN_THROTTLES = 2;
    protected static final int MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT = 1;

    private LinearLayout[] lThrottles;
    private LinearLayout[] lUppers;
    private LinearLayout[] lLowers;
    private LinearLayout[] lSpeeds;
    private ScrollView[] svFnBtns;

    protected void removeLoco(int whichThrottle) {
        super.removeLoco(whichThrottle);
        set_function_labels_and_listeners_for_view(whichThrottle);
    }

    @SuppressLint({"Recycle", "SetJavaScriptEnabled"})
    @Override
    public void onCreate(Bundle savedInstanceState) {

        mainapp = (threaded_application) this.getApplication();
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        switch (prefThrottleScreenType) {
            case "Vertical":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
                mainapp.throttleLayoutViewId = R.layout.throttle_vertical;
                break;
            case "Vertical Right":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT;
                mainapp.throttleLayoutViewId = R.layout.throttle_vertical_right;
                break;
            case "Vertical Left":
            default:
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT;
                mainapp.throttleLayoutViewId = R.layout.throttle_vertical_left;
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

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lSpeeds = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        svFnBtns = new ScrollView[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
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
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_0);
                    break;
                case 1:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_1);
                    lUppers[throttleIndex] = findViewById(R.id.loco_upper_1);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_1);
                    lSpeeds[throttleIndex] = findViewById(R.id.throttle_1_SetSpeed);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_1);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_1);
                    break;
            }
        }

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        // set listeners for the limit speed buttons for each throttle
        //----------------------------------------

        limit_speed_button_touch_listener lstl;
        Button bLimitSpeed = findViewById(R.id.limit_speed_0);
        pause_speed_button_vertical_touch_listener psvtl;
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
            lstl = new limit_speed_button_touch_listener(throttleIndex);
            bLimitSpeeds[throttleIndex].setOnTouchListener(lstl);
            isLimitSpeeds[throttleIndex] = false;
            if (!prefLimitSpeedButton) {
                bLimitSpeed.setVisibility(View.GONE);
            }

            bPauseSpeeds[throttleIndex] = bPauseSpeed;
            psvtl = new pause_speed_button_vertical_touch_listener(throttleIndex);
            bPauseSpeeds[throttleIndex].setOnTouchListener(psvtl);
            isPauseSpeeds[throttleIndex] = PAUSE_SPEED_INACTIVE;
            if (!prefPauseSpeedButton) {
                bPauseSpeed.setVisibility(View.GONE);
            }
        }
        sliderType = SLIDER_TYPE_VERTICAL;
    } // end of onCreate()

    @Override
    public void onResume() {
        super.onResume();

        if (mainapp.appIsFinishing) { return;}

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            if (throttleIndex < mainapp.numThrottles) {
                lThrottles[throttleIndex].setVisibility(LinearLayout.VISIBLE);
            } else {
                lThrottles[throttleIndex].setVisibility(LinearLayout.GONE);
            }

            // show or hide the limit speed buttons
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
            if (prefThrottleScreenType != "Vertical") {
                textSize = (int) (conNomTextSize * textScale);
            }
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
        if ((toolbar != null) && (!prefThrottleViewImmersiveModeHideToolbar))  {
            titleBar = toolbar.getHeight();
            if (screenHeight!=0) {
                screenHeight = screenHeight - titleBar;
            }
        }
        int keepHeight = screenHeight;  // default height
        if (screenHeight == 0) {
            // throttle screen hasn't been drawn yet, so use display metrics for now
            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
            //Log.d("Engine_Driver","vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
        }

        if (webView != null) {
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

        LinearLayout.LayoutParams stopButtonParams;
        stopButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
        int prefVerticalStopButtonMargin = mainapp.getIntPrefValue(prefs, "prefVerticalStopButtonMargin", "0");
        stopButtonParams.topMargin = Math.max(prefVerticalStopButtonMargin, (int) (speedButtonHeight * 0.5));
        stopButtonParams.bottomMargin = prefVerticalStopButtonMargin;
        stopButtonParams.height = speedButtonHeight;

        if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
            speedButtonHeight = (int) ((screenHeight
                    - stopButtonParams.topMargin
                    - stopButtonParams.bottomMargin
                    - (220 * denScale)) / 2);
        }

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

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            // set height of each function button area
            svFnBtns[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            svFnBtns[throttleIndex].requestLayout();
            lLowers[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            lLowers[throttleIndex].requestLayout();

            // update throttle slider top/bottom
//            tops[throttleIndex] = lls[throttleIndex].getTop() + sbs[throttleIndex].getTop() + bSels[throttleIndex].getHeight() + bFwds[throttleIndex].getHeight();
//            bottoms[throttleIndex] = lls[throttleIndex].getTop() + sbs[throttleIndex].getBottom() + bSels[throttleIndex].getHeight() + bFwds[throttleIndex].getHeight();

            int[] location = new int[2];
            ov.getLocationOnScreen(location);
            int ovx = location[0];
            int ovy = location[1];

            location = new int[2];
            vsbSpeeds[throttleIndex].getLocationOnScreen(location);
            int x = location[0];
            int y = location[1];

            sliderTopLeftX[throttleIndex] = x - ovx;
            sliderTopLeftY[throttleIndex] = y - ovy;
            sliderBottomRightX[throttleIndex] = x + vsbSpeeds[throttleIndex].getWidth() - ovx;
            sliderBottomRightY[throttleIndex] = y + vsbSpeeds[throttleIndex].getHeight() - ovy;

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
        if ((bLimitSpeeds!=null) && (bLimitSpeeds[whichThrottle]!=null)) {
            bLimitSpeeds[whichThrottle].setEnabled(newEnabledState);
            bPauseSpeeds[whichThrottle].setEnabled(newEnabledState);
        }

        super.enable_disable_buttons(whichThrottle, forceDisable);

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

    //listeners for the Pause Speed Button
    protected class pause_speed_button_vertical_touch_listener implements View.OnTouchListener {
        int whichThrottle;

        protected pause_speed_button_vertical_touch_listener(int new_whichThrottle) {
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

        if (isPauseSpeeds[whichThrottle]!=PAUSE_SPEED_INACTIVE) {
            setSpeed(whichThrottle, speed, SPEED_COMMAND_FROM_BUTTONS);
        }
    }

}