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
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.LinkedHashMap;

// for changing the screen brightness

// used for supporting Keyboard and Gamepad input;

public class throttle_simple extends throttle {

    protected static final int MAX_SCREEN_THROTTLES = 6;

    private LinearLayout[] lThrottles;
    private LinearLayout[] lLowers;
    private LinearLayout[] Separators;
    private ScrollView[] svFnBtns;

    @Override
    protected void getDirectionButtonPrefs() {
        super.getDirectionButtonPrefs();
        super.DIRECTION_BUTTON_LEFT_TEXT = getApplicationContext().getResources().getString(R.string.prefLeftDirectionButtonsShortDefaultValue);
        super.DIRECTION_BUTTON_RIGHT_TEXT = getApplicationContext().getResources().getString(R.string.prefRightDirectionButtonsShortDefaultValue);

        super.prefLeftDirectionButtons = prefs.getString("prefLeftDirectionButtonsShort", getApplicationContext().getResources().getString(R.string.prefLeftDirectionButtonsShortDefaultValue)).trim();
        super.prefRightDirectionButtons = prefs.getString("prefRightDirectionButtonsShort", getApplicationContext().getResources().getString(R.string.prefRightDirectionButtonsShortDefaultValue)).trim();
    }

    @SuppressLint({"Recycle", "SetJavaScriptEnabled"})
    @Override
    public void onCreate(Bundle savedInstanceState) {

        mainapp = (threaded_application) this.getApplication();
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;

        mainapp.throttleLayoutViewId = R.layout.throttle_simple;
        super.onCreate(savedInstanceState);

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        Separators = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        svFnBtns = new ScrollView[mainapp.maxThrottlesCurrentScreen];
        lLowers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++)
            switch (throttleIndex) {
                default:
                case 0:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_0);
                    Separators[throttleIndex] = findViewById(R.id.separator0);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_0);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_0);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_0);
                    llSetSpds[throttleIndex] = (LinearLayout) findViewById(R.id.throttle_0_SetSpeed);
                    break;
                case 1:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_1);
                    Separators[throttleIndex] = findViewById(R.id.separator1);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_1);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_1);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_1);
                    llSetSpds[throttleIndex] = (LinearLayout) findViewById(R.id.throttle_1_SetSpeed);
                    break;
                case 2:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_2);
                    Separators[throttleIndex] = findViewById(R.id.separator2);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_2);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_2);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_2);
                    llSetSpds[throttleIndex] = (LinearLayout) findViewById(R.id.throttle_2_SetSpeed);
                    break;
                case 3:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_3);
                    Separators[throttleIndex] = findViewById(R.id.separator3);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_3);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_3);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_3);
                    llSetSpds[throttleIndex] = (LinearLayout) findViewById(R.id.throttle_3_SetSpeed);
                    break;
                case 4:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_4);
                    Separators[throttleIndex] = findViewById(R.id.separator4);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_4);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_4);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_4);
                    llSetSpds[throttleIndex] = (LinearLayout) findViewById(R.id.throttle_4_SetSpeed);
                    break;
                case 5:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_5);
                    Separators[throttleIndex] = findViewById(R.id.separator5);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_5);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_5);
                    llSetSpds[throttleIndex] = (LinearLayout) findViewById(R.id.throttle_5_SetSpeed);
                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_0);
                    break;
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
                case 2:
                    bLimitSpeed = findViewById(R.id.limit_speed_2);
                    bPauseSpeed = findViewById(R.id.pause_speed_2);
                    break;
                case 3:
                    bLimitSpeed = findViewById(R.id.limit_speed_3);
                    bPauseSpeed = findViewById(R.id.pause_speed_3);
                    break;
                case 4:
                    bLimitSpeed = findViewById(R.id.limit_speed_4);
                    bPauseSpeed = findViewById(R.id.pause_speed_4);
                    break;
                case 5:
                    bLimitSpeed = findViewById(R.id.limit_speed_5);
                    bPauseSpeed = findViewById(R.id.pause_speed_5);
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
            if( throttleIndex < mainapp.numThrottles) {
                lThrottles[throttleIndex].setVisibility(LinearLayout.VISIBLE);
                Separators[throttleIndex].setVisibility(LinearLayout.VISIBLE);
            } else {
                lThrottles[throttleIndex].setVisibility(LinearLayout.GONE);
                Separators[throttleIndex].setVisibility(LinearLayout.GONE);
            }
        }
        Separators[0].setVisibility(LinearLayout.GONE);

    } // end of onResume()


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
            if ((mainapp.consists != null) && (mainapp.consists[throttleIndex].isActive())) {
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
                    bLabel = mainapp.consists[throttleIndex].formatConsistAddr();
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
            int textSize = (int) (conNomTextSize * textScale);
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

        int prefSimpleThrottleLayoutShowFunctionButtonCount = preferences.getIntPrefValue(prefs, "prefSimpleThrottleLayoutShowFunctionButtonCount",
                getApplicationContext().getResources().getString(R.string.prefSimpleThrottleLayoutShowFunctionButtonCountDefaultValue));

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        // Get the screen's density scale
        final float denScale = dm.density;
        int sep = (int) (denScale * 12); // seperator

        int screenWidth = vThrotScrWrap.getWidth(); // get the width of usable area
        int throttleWidth = (screenWidth - (sep * (mainapp.numThrottles-1)))/ mainapp.numThrottles;
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex].getLayoutParams().height = LinearLayout.LayoutParams.FILL_PARENT;
            lThrottles[throttleIndex].getLayoutParams().width = throttleWidth;
            lThrottles[throttleIndex].requestLayout();
        }

        int screenHeight = vThrotScrWrap.getHeight(); // get the Height of usable area
        if (screenHeight == 0) {
            // throttle screen hasn't been drawn yet, so use display metrics for now
            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
            //Log.d("Engine_Driver","vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
        }

        int speedButtonHeight = (int) (50 * denScale);
        if (prefs.getBoolean("hide_slider_preference", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
            speedButtonHeight = (int) ((screenHeight - (200 * denScale)) / 2);
        }

        Button bStop = findViewById(R.id.button_stop_0);
        int fbsHeight = bStop.getHeight()+ (int) (3 * denScale);
        int lLowersHeight = lLowers[0].getHeight();

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

            if ( (prefSimpleThrottleLayoutShowFunctionButtonCount > 0) && (lLowersHeight > 0) )  {
                llSetSpds[throttleIndex].getLayoutParams().height
                        = lLowersHeight - (int) (prefSimpleThrottleLayoutShowFunctionButtonCount * fbsHeight + 20 * denScale);
                svFnBtns[throttleIndex].getLayoutParams().height = (int) (prefSimpleThrottleLayoutShowFunctionButtonCount * fbsHeight + 20 * denScale);
            }
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
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
            sliderBottomRightY[throttleIndex] = y + vsbSpeeds[throttleIndex].getHeight() -ovy;

//            Log.d("Engine_Driver","slider: " + throttleIndex + " Top: " + sliderTopLeftX[throttleIndex] + ", " + sliderTopLeftY[throttleIndex]
//                    + " Bottom: " + sliderBottomRightX[throttleIndex] + ", " + sliderBottomRightY[throttleIndex]);
        }



//        // update the state of each function button based on shared variable
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            set_all_function_states(throttleIndex);
        }


        // Log.d("Engine_Driver","ending set_labels");

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

}