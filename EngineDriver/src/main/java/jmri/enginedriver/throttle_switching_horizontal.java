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
import android.widget.LinearLayout;
//import android.widget.ScrollView;
import android.widget.SeekBar;

import java.util.LinkedHashMap;

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.auto_increment_or_decrement_type;
import jmri.enginedriver.type.direction_type;
import jmri.enginedriver.type.kids_timer_action_type;
import jmri.enginedriver.type.max_throttles_current_screen_type;
import jmri.enginedriver.type.tick_type;
import jmri.enginedriver.util.HorizontalSeekBar;
import jmri.enginedriver.type.slider_type;
import jmri.enginedriver.type.web_view_location_type;

public class throttle_switching_horizontal extends throttle {
    static final String activityName = "throttle_switching_horizontal";

    protected static final int MAX_SCREEN_THROTTLES = max_throttles_current_screen_type.SWITCHING_HORIZONTAL;

    private LinearLayout[] lThrottles;

    private final int[] throttleMidPointZero = {0,0,0};
    private final int[] throttleSwitchingMax = {0,0,0};

    private final int[] throttleMidPointDeadZoneUpper = {0,0,0};
    private final int[] throttleMidPointDeadZoneLower = {0,0,0};

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

        maxThrottlePcnt = threaded_application.getIntPrefValue(prefs, "prefMaximumThrottle", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (0.01 * maxThrottlePcnt)); // convert from percent

        prefSwitchingThrottleSliderDeadZone = Integer.parseInt(prefs.getString("prefSwitchingThrottleSliderDeadZone", getResources().getString(R.string.prefSwitchingThrottleSliderDeadZoneDefaultValue)));

        prefHideFunctionButtonsOfNonSelectedThrottle = prefs.getBoolean("prefHideFunctionButtonsOfNonSelectedThrottle", false);
    }

    protected void setScreenDetails() {
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
        mainapp.currentScreenSupportsWebView = true;
        sliderType = slider_type.SWITCHING;

        mainapp.throttleLayoutViewId = R.layout.throttle_switching_horizontal;
    } // end setScreen()

    @SuppressLint({"Recycle", "SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(threaded_application.applicationName, activityName + ": onCreate(): called");

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        mainapp.throttleSwitchAllowed = false; // used to prevent throttle switches until the previous onStart() completes

        if(mainapp.throttleSwitchWasRequestedOrReinitialiseRequired) {
            setScreenDetails();
        }
        super.onCreate(savedInstanceState);

    } // end of onCreate()

    @Override
    public void onStart() {
        Log.d(threaded_application.applicationName, activityName + ": onStart(): called");
        if (mainapp.appIsFinishing) return;

        setScreenDetails();
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

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            functionButtonViewGroups[throttleIndex] = findViewById(function_buttons_table_resource_ids.getResourceId(throttleIndex,0));
            tvDirectionIndicatorForwards[throttleIndex] = findViewById(direction_indicator_forward_resource_ids.getResourceId(throttleIndex,0));
            tvDirectionIndicatorReverses[throttleIndex] = findViewById(direction_indicator_reverse_resource_ids.getResourceId(throttleIndex,0));
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
        sbSpeeds = new HorizontalSeekBar[mainapp.maxThrottlesCurrentScreen];
        hsbSwitchingSpeeds = new HorizontalSeekBar[mainapp.maxThrottlesCurrentScreen];

        TypedArray throttle_resource_ids = getResources().obtainTypedArray(R.array.throttle_resource_ids);
        TypedArray throttle_set_speed_resource_ids = getResources().obtainTypedArray(R.array.throttle_set_speed_resource_ids);
        TypedArray speed_resource_ids = getResources().obtainTypedArray(R.array.speed_resource_ids);
        TypedArray speed_switching_resource_ids = getResources().obtainTypedArray(R.array.speed_switching_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex] = findViewById(throttle_resource_ids.getResourceId(throttleIndex,0));
            llSetSpeeds[throttleIndex] = findViewById(throttle_set_speed_resource_ids.getResourceId(throttleIndex,0));
            sbSpeeds[throttleIndex] = findViewById(speed_resource_ids.getResourceId(throttleIndex,0));
            hsbSwitchingSpeeds[throttleIndex] = findViewById(speed_switching_resource_ids.getResourceId(throttleIndex,0));
            hsbSwitchingSpeeds[throttleIndex].setTickType(tick_type.TICK_0_100_0);
            hsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
            hsbSwitchingSpeeds[throttleIndex].setProgress(throttleMidPointZero[throttleIndex]);
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            sbs[throttleIndex].setMax(maxThrottle);
            hsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
        }

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        throttleSwitchingListener thsl;

        for (int i=0; i < mainapp.maxThrottlesCurrentScreen; i++) {
            // set up listeners for all throttles
            thsl = new throttleSwitchingListener(i);
            hsbSwitchingSpeeds[i].setOnSeekBarChangeListener(thsl);
            hsbSwitchingSpeeds[i].setOnTouchListener(thsl);
        }

        function_buttons_table_resource_ids.recycle();
        direction_indicator_forward_resource_ids.recycle();
        direction_indicator_reverse_resource_ids.recycle();
        throttle_resource_ids.recycle();
        throttle_set_speed_resource_ids.recycle();
        speed_resource_ids.recycle();
        speed_switching_resource_ids.recycle();

    } // endInitialiseUiElements()

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
//        Log.d(threaded_application.applicationName, activityName + ": setLabels() starting");
        super.setLabels();

        if (mainapp.appIsFinishing) { return;}

//        int throttle_count = 0;
//        int[] heights = {0, 0, 0, 0, 0, 0};

        // avoid NPE by not letting this run too early (reported to Play Store)
        if (tvVols[0] == null) return;

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        // Get the screen's density scale
        final float denScale = dm.density;

        // Convert the dps to pixels, based on density scale
        int newDlihHeight;
        int newDlihFontSize;
        if (prefDecreaseLocoNumberHeight) {
            newDlihHeight = (int) (40 * denScale + 0.5f); // decreased height
            newDlihFontSize = 32; // decreased height
        } else {
            newDlihHeight = (int) (50 * denScale + 0.5f); // normal height
            newDlihFontSize = 36; // normal height
        }
        // SPDHT

        // Convert the dps to pixels, based on density scale
        int newHeight;
        if (prefIncreaseSliderHeight) {
            newHeight = (int) (80 * denScale + 0.5f); // increased height
        } else {
            newHeight = (int) (50 * denScale + 0.5f); // normal height
        }

        final int conNomTextSize = 24;
        final double minTextScale = 0.5;
        String bLabel;
        String bLabelPlainText;

        if(mainapp.consists==null) {
            Log.d(threaded_application.applicationName, activityName + ": setLabels() consists is null");
            return;
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            Button b = bSels[throttleIndex];
            bLabel = getApplicationContext().getResources().getString(R.string.locoPressToSelect);
            bLabelPlainText = bLabel;

            Consist con = mainapp.consists[throttleIndex];
            if(con==null) {
                Log.d(threaded_application.applicationName, activityName + ": setLabels(): consists[" + throttleIndex + "] is null");
            }
            else {
                if (con.isActive()) {
                    if (!prefShowAddressInsteadOfName) {
                        if (!overrideThrottleNames[throttleIndex].isEmpty()) {
                            bLabel = overrideThrottleNames[throttleIndex];
                            bLabelPlainText = overrideThrottleNames[throttleIndex];
                        } else {
                            bLabel = con.toHtml();
                            bLabelPlainText = con.toString();
                        }

                    } else {
                        if (overrideThrottleNames[throttleIndex].isEmpty()) {
                            bLabel = con.formatConsistAddr();
                        } else {
                            bLabel = overrideThrottleNames[throttleIndex];
                        }
                        bLabelPlainText = bLabel;

                    }
                    bLabel = mainapp.locoAndConsistNamesCleanupHtml(bLabel);
//                    throttle_count++;

                    tvbSelsLabels[throttleIndex].setVisibility(View.GONE);
                } else {
                    tvbSelsLabels[throttleIndex].setVisibility(View.VISIBLE);
                }
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

        setImmersiveMode(webView);

        int screenHeight = getAvailableScreenHeight();

        // SPDHT set height of Loco Id and Direction Button areas
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

            LinearLayout.LayoutParams llLidp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newDlihHeight);
            llLocoIdAndSpeedViewGroups[throttleIndex].setLayoutParams(llLidp);
            llLocoDirectionButtonViewGroups[throttleIndex].setLayoutParams(llLidp);
            //
            tvSpdVals[throttleIndex].setTextSize(TypedValue.COMPLEX_UNIT_SP, newDlihFontSize);
            // SPDHT

            //set height of slider areas
            LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newHeight);
            llSetSpeeds[throttleIndex].setLayoutParams(llLp);

            //set margins of slider areas
            int sliderMargin = threaded_application.getIntPrefValue(prefs, "prefLeftSliderMargin", getApplicationContext().getResources().getString(R.string.prefSliderLeftMarginDefaultValue));

            //show speed buttons based on pref
            llSetSpeeds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default

            hsbSwitchingSpeeds[throttleIndex].setVisibility(View.VISIBLE);  //always show slider if buttons not shown
            if (prefs.getBoolean("prefDisplaySpeedButtons", false)) {
                bLSpds[throttleIndex].setVisibility(View.VISIBLE);
                bRSpds[throttleIndex].setVisibility(View.VISIBLE);
                bLSpds[throttleIndex].setText(speedButtonLeftText);
                bRSpds[throttleIndex].setText(speedButtonRightText);
                //if buttons enabled, hide the slider if requested
                if (prefs.getBoolean("prefHideSlider", false)) {
                    hsbSwitchingSpeeds[throttleIndex].setVisibility(View.GONE);
                    bLSpds[throttleIndex].setText(speedButtonDownText);
                    bRSpds[throttleIndex].setText(speedButtonUpText);
                }
            } else {  //hide speed buttons based on pref
                bLSpds[throttleIndex].setVisibility(View.GONE);
                bRSpds[throttleIndex].setVisibility(View.GONE);
//                sliderMargin += 30;  //a little extra margin previously in button
            }
            if (prefs.getBoolean("prefHideSliderAndSpeedButtons", getResources().getBoolean(R.bool.prefHideSliderAndSpeedButtonsDefaultValue))) {
                llSetSpeeds[throttleIndex].setVisibility(View.GONE);
            }

            int additionalPadding = (sbs[throttleIndex].getWidth()>400 ? 40 : 20);
            hsbSwitchingSpeeds[throttleIndex].setPadding(additionalPadding+sliderMargin, 0, additionalPadding+sliderMargin, 0);

            // update the state of each function button based on shared variable
            setAllFunctionStates(throttleIndex);
        }


        if ( (screenHeight > throttleMargin) && (mainapp.consists!=null)) { // don't do this if height is invalid
            //Log.d(threaded_application.applicationName, activityName + ": setLabels(): starting screen height adjustments, screenHeight=" + screenHeight);
            // determine how to split the screen (evenly if all three, 45/45/10 for two, 80/10/10 if only one)

            adjustThrottleHeights();

            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

                int[] location = new int[2];
                throttleOverlay.getLocationOnScreen(location);
                int ovx = location[0];
                int ovy = location[1];

                location = new int[2];
                hsbSwitchingSpeeds[throttleIndex].getLocationOnScreen(location);
                int x = location[0];
                int y = location[1];

                sliderTopLeftX[throttleIndex] = x - ovx;
                sliderTopLeftY[throttleIndex] = y - ovy;
                sliderBottomRightX[throttleIndex] = x + hsbSwitchingSpeeds[throttleIndex].getWidth() - ovx;
                sliderBottomRightY[throttleIndex] = y + hsbSwitchingSpeeds[throttleIndex].getHeight() -ovy;

//            Log.d(threaded_application.applicationName, activityName + ": setLabels(): slider: " + throttleIndex + " Top: " + sliderTopLeftX[throttleIndex] + ", " + sliderTopLeftY[throttleIndex]
//                    + " Bottom: " + sliderBottomRightX[throttleIndex] + ", " + sliderBottomRightY[throttleIndex]);
            }
        } else {
            Log.d(threaded_application.applicationName, activityName + ": setLabels(): screen height adjustments skipped, screenHeight=" + screenHeight);
        }

        // update the direction indicators
        showDirectionIndications();

        // update the switching sliders if necessary
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            speedUpdate(throttleIndex,
                    getSpeedFromCurrentSliderPosition(throttleIndex,false));
        }

//        // update the state of each function button based on shared variable
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            setAllFunctionStates(throttleIndex);
        }


        // Log.d(threaded_application.applicationName, activityName + ": setLabels(): end");

    } // end set_labels

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

        if (hsbSwitchingSpeeds != null) {
            if (!newEnabledState) {
                hsbSwitchingSpeeds[whichThrottle].setProgress(0); // set slider to 0 if disabled
            }
            hsbSwitchingSpeeds[whichThrottle].setEnabled(newEnabledState);
        }
    } // end of enableDisableButtons

    // helper function to enable/disable all children for a group
    @Override
    void enableDisableButtonsForView(ViewGroup vg, boolean newEnabledState) {
        // Log.d(threaded_application.applicationName, activityName + ": enableDisableButtonsForView() " + newEnabledState);

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
            // Log.d(threaded_application.applicationName, activityName + ": onTouchThrottle action " + event.getAction());
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

//            Log.d(threaded_application.applicationName, activityName + ": onProgressChanged(): fromUser: " + fromUser + " touchFromUser: " + hsbSwitchingSpeeds[whichThrottle].touchFromUser + " limitedJump: " + limitedJump);
//            Log.d(threaded_application.applicationName, activityName + ": onProgressChanged(): isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);

            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (hsbSwitchingSpeeds[whichThrottle].touchFromUser) ) {

                if (!limitedJump[whichThrottle]) {         // touch generates multiple onProgressChanged events, skip processing after first limited jump

                    if (Math.abs(newSliderPosition - lastSliderPosition) > max_throttle_change) { // if jump is too large then limit it

                        jumpSpeed = getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false);      // save ultimate target value
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
                            getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false));
                    sendSpeedMsg(whichThrottle, speed);
                    setDisplayedSpeed(whichThrottle, speed);

                } else { // got a touch while processing limitJump
//                    Log.d(threaded_application.applicationName, activityName + ": onProgressChanged() -- touch while processing limited jump");
                    newSliderPosition = lastSliderPosition;    //   so suppress multiple touches
                    throttle.setProgress(lastSliderPosition);
                    if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

//                    Log.d(threaded_application.applicationName, activityName + ": onProgressChange(): fromUser: " + fromUser + " hsbSwitchingSpeeds[wt].touchFromUser: " +hsbSwitchingSpeeds[whichThrottle].touchFromUser + " isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);
                }

                // Now update ESU MCII Knob position
                if (IS_ESU_MCII) {
                    setEsuThrottleKnobPosition(whichThrottle, speed);
                }

                setActiveThrottle(whichThrottle, true);

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
                            Log.d(threaded_application.applicationName, activityName + ": onProgressChanged(): !!-- Direction change now needed");
                            mChangeDirectionAtZero = false;
                            reverseDirectionIfNeeded(jumpDir, whichThrottle);
                        } else {
                            Log.d(threaded_application.applicationName, activityName + ": onProgressChanged(): !!-- LimitedJump hit jump speed.");
                            limitedJump[whichThrottle] = false;
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
                            throttle.setProgress(getNewSliderPositionFromSpeed(jumpSpeed, whichThrottle, false));
                            if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
                            speedUpdate(whichThrottle, getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false));
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

    HorizontalSeekBar getSwitchingThrottleSlider(int whichThrottle) {
        return hsbSwitchingSpeeds[whichThrottle];
    }


    // change speed slider by scaled display unit value
    @Override
    int speedChange(int whichThrottle, int change) {

        HorizontalSeekBar switchingThrottleSlider = getSwitchingThrottleSlider(whichThrottle);
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
//            Log.d(threaded_application.applicationName, activityName + ": speedChange(): - auto Reverse - dir:" + dir);
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
            newSliderPosition += Math.round(Math.signum(change));
        }

        if (newSliderPosition < 0)  //insure speed is inside bounds
            newSliderPosition = 0;

        if (newSliderPosition > throttleSwitchingMax[whichThrottle])
            newSliderPosition = throttleSwitchingMax[whichThrottle];

//        Log.d(threaded_application.applicationName, activityName + ": speedChange() - lastScaleSpeed: " + lastScaleSpeed + " scaleSpeed: " + scaleSpeed + " dir: " + getDirection(whichThrottle) + " newSliderPosition: " + newSliderPosition);
//        Log.d(threaded_application.applicationName, activityName + ": speedChange() - change: " + change);

        switchingThrottleSlider.setProgress(newSliderPosition);
        speed = Math.abs(getSpeedFromSliderPosition(newSliderPosition, whichThrottle, false));
        setDisplayedSpeed(whichThrottle, speed);
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

//        Log.d(threaded_application.applicationName, activityName + ": speedChange():  speed: " + speed + " change: " + change);

        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);

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

//        Log.d(threaded_application.applicationName, activityName + ": speedChange(): speedUpdate -  sliderPosition: " + sliderPosition + " dir: " + getDirection(whichThrottle) + " Speed: " + speed);
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
            hsbSwitchingSpeeds[whichThrottle].setProgress(sliderPosition);
        }
        if(ipls!=null) ipls.doLocoSound(whichThrottle, getSpeedFromCurrentSliderPosition(whichThrottle, false), dirs[whichThrottle], soundsIsMuted[whichThrottle]);
    }


    @Override
    boolean changeDirectionIfAllowed(int whichThrottle, int direction) {
        if (getDirection(whichThrottle) != direction) {

            showDirectionRequest(whichThrottle, direction);        // update requested direction indication
            setEngineDirection(whichThrottle, direction, false);   // update direction for each engine on this throttle

            int lastSliderPosition = hsbSwitchingSpeeds[whichThrottle].getProgress();
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
        if (hsbSwitchingSpeeds[whichThrottle].isEnabled()) {
            speed = getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(), whichThrottle, useScale);
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

    protected void limitSpeed(int whichThrottle) {
        int dir = getDirection(whichThrottle);
        int speed = getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle, false);
//                Log.d(threaded_application.applicationName, activityName + ": limitSpeed():  speed: " + speed );

        isLimitSpeeds[whichThrottle] = !isLimitSpeeds[whichThrottle];
        if (isLimitSpeeds[whichThrottle]) {
            bLimitSpeeds[whichThrottle].setSelected(true);
            limitSpeedSliderScalingFactors[whichThrottle] = 100/ ((float) prefLimitSpeedPercent);
            sbs[whichThrottle].setMax( Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]));

            throttleMidPointZero[whichThrottle] = (Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]) + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[whichThrottle] = (Math.round(MAX_SPEED_VAL_WIT / limitSpeedSliderScalingFactors[whichThrottle]) + prefSwitchingThrottleSliderDeadZone) * 2;
            hsbSwitchingSpeeds[whichThrottle].setMax(throttleSwitchingMax[whichThrottle]);

        } else {
            bLimitSpeeds[whichThrottle].setSelected(false);
            sbs[whichThrottle].setMax(maxThrottle);

            throttleMidPointZero[whichThrottle] = (maxThrottle + prefSwitchingThrottleSliderDeadZone);
            throttleSwitchingMax[whichThrottle] = (maxThrottle + prefSwitchingThrottleSliderDeadZone) * 2;
            hsbSwitchingSpeeds[whichThrottle].setMax(throttleSwitchingMax[whichThrottle]);
        }
        throttleMidPointDeadZoneUpper[whichThrottle] = throttleMidPointZero[whichThrottle] + prefSwitchingThrottleSliderDeadZone;
        throttleMidPointDeadZoneLower[whichThrottle] = throttleMidPointZero[whichThrottle] - prefSwitchingThrottleSliderDeadZone;

        speedUpdate(whichThrottle,  speed);
        setEngineDirection(whichThrottle, dir, false);

        Log.d(threaded_application.applicationName, activityName + ": limitSpeed():  speed: " + speed );
        speedChangeAndNotify(whichThrottle,0);
        setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
    }

    @Override
    boolean canChangeVolumeIndicatorOnTouch(boolean isSpeedButtonOrSlider) {
        if (!prefHideFunctionButtonsOfNonSelectedThrottle) return false;
        return !isSpeedButtonOrSlider;
    }

    @Override
    void adjustThrottleHeights() {
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int[] throttleHeights = {0, 0, 0, 0, 0, 0};

        int height = getAvailableScreenHeight();

        if ((height > throttleMargin) && (mainapp.consists != null)) { // don't do this if height is invalid

            if (mainapp.numThrottles == 1) {        // just one throttle
                throttleHeights[0] = height;
            } else {
                boolean[] throttlesInUse = {false, false, false, false, false, false};
                int throttlesInUseCount = 0;
//                double newHeight = 0;
                for (int i = 0; i < mainapp.numThrottles; i++) {
                    if ((mainapp.consists[i] != null) && (mainapp.consists[i].isActive())) {
                        throttlesInUse[i] = true;
                        throttlesInUseCount++;
                    }
                }

                double activeHeight;
                double semiActiveHeight;
                double inactiveHeight = llLocoIdAndSpeedViewGroups[0].getHeight();

                if (!prefHideFunctionButtonsOfNonSelectedThrottle) {

                    if (mainapp.numThrottles == 2) {
                        if (throttlesInUseCount <= 1) {
                            activeHeight = height - llLocoIdAndSpeedViewGroups[0].getHeight();
                        } else {  // equals 2
                            activeHeight = (height - llLocoIdAndSpeedViewGroups[0].getHeight()) / (float) 2;
                        }
                    } else { // equals 3
                        if (throttlesInUseCount <= 1) {
                            activeHeight = height - (llLocoIdAndSpeedViewGroups[0].getHeight() * 2);
                        } else if (throttlesInUseCount == 2) {
                            activeHeight = (height - llLocoIdAndSpeedViewGroups[0].getHeight()) / (float) 2;
                        } else {  // equals 3
                            activeHeight = (height - llLocoIdAndSpeedViewGroups[0].getHeight()) / (float) 3;
                        }
                    }
                    for (int i = 0; i < mainapp.numThrottles; i++) {
                        if (throttlesInUse[i]) {
                            throttleHeights[i] = (int) activeHeight;
                        } else {
                            throttleHeights[i] = (int) inactiveHeight;
                        }
                    }

                } else { // hide function buttons of non-selected throttle
                    int semiActiveCount = (throttlesInUseCount == 0) ? 0 : (throttlesInUseCount - 1);
                    int inactiveCount = (throttlesInUseCount == 0) ? mainapp.numThrottles : (mainapp.numThrottles - 1 - semiActiveCount);
                    semiActiveHeight = llLocoIdAndSpeedViewGroups[0].getHeight()
//                            + llLocoDirectionButtonViewGroups[0].getHeight()
                            + llSetSpeeds[0].getHeight() + 6 * displayMetrics.density;  // 6 is for the padding;
                    activeHeight = height - (semiActiveHeight * semiActiveCount) - (inactiveHeight * inactiveCount);

                    for (int i = 0; i < mainapp.numThrottles; i++) {
                        if (throttlesInUse[i]) {
                            if (i == whichVolume) {
                                throttleHeights[i] = (int) activeHeight;
                            } else {
                                throttleHeights[i] = (int) semiActiveHeight;
                            }
                        } else {
                            throttleHeights[i] = (int) inactiveHeight;
                        }
                    }
                }


                if (throttlesInUseCount == 0) throttleHeights[0] = (int) activeHeight;
            }

            LinearLayout.LayoutParams llLp;
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                // set height of each area
                llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, throttleHeights[throttleIndex]);
//                llLp.bottomMargin = (int) (throttleMargin * (displayMetrics.densityDpi / 160.));
                llThrottleLayouts[throttleIndex].setLayoutParams(llLp);
            }
        }
    }

    int getAvailableScreenHeight() {   // excluding the webview if displayed
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//        final float density = displayMetrics.density;

        int screenHeight = vThrotScrWrap.getHeight(); // get the height of usable area
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
            screenHeight = displayMetrics.heightPixels - (int) (titleBar * (displayMetrics.densityDpi / 160.)); // allow for title bar, etc
            //Log.d(threaded_application.applicationName, activityName + ": getAvailableSreenHeight(): vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
        }

        double height = screenHeight;
        LinearLayout.LayoutParams overlayParams;
        if(!prefWebViewLocation.equals(web_view_location_type.NONE))     {
            webViewIsOn = true;
            if (!prefIncreaseWebViewSize) {
                height *= 0.5; // save half the screen
            } else {
                height *= 0.4; // save 60% of the screen for web view
            }
            screenHeight = (int) height;
            LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, fullScreenHeight - titleBar - screenHeight);
            webView.setLayoutParams(webViewParams);
        }
        overlayParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, screenHeight  + titleBar);
        throttleOverlay.setLayoutParams(overlayParams);

        return (int) height;
    }

}