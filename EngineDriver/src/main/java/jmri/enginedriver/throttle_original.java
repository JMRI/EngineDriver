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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.LinkedHashMap;

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.max_throttles_current_screen_type;
import jmri.enginedriver.type.tick_type;
import jmri.enginedriver.util.HorizontalSeekBar;
import jmri.enginedriver.type.slider_type;
import jmri.enginedriver.type.web_view_location_type;

public class throttle_original extends throttle {
    static final String activityName = "throttle_original";

    protected static final int MAX_SCREEN_THROTTLES = max_throttles_current_screen_type.DEFAULT;


    protected void removeLoco(int whichThrottle) {
        super.removeLoco(whichThrottle);
        set_function_labels_and_listeners_for_view(whichThrottle);
    }

    // helper function to enable/disable all children for a group
    @Override
    void enableDisableButtonsForView(ViewGroup vg, boolean newEnabledState) {
        // Log.d(threaded_application.applicationName, activityName + ": enableDisableButtonsForView() " + newEnabledState);

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
        // Log.d(threaded_application.applicationName, activityName + ": set_function_states()");

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
        // Log.d(threaded_application.applicationName, activityName + ": set_function_request()");

        if (function > threaded_application.MAX_FUNCTION_NUMBER)
            return; //bail if this function number not supported

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

    protected void setScreenDetails() {
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
        mainapp.currentScreenSupportsWebView = true;

        mainapp.throttleLayoutViewId = R.layout.throttle;
    } // end setScreen()

    @SuppressLint({"Recycle", "SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(threaded_application.applicationName, activityName + ": onCreate(): called");

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        mainapp.throttleSwitchAllowed = false; // used to prevent throttle switches until the previous onStart() completes

        sliderType = slider_type.HORIZONTAL;

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
    public void onResume() {
        Log.d(threaded_application.applicationName, activityName + ": onResume(): called");
        super.onResume();
        threaded_application.activityResumed(activityName);

    } // end of onResume()

    void initialiseUiElements() {
        super.initialiseUiElements();

        sbSpeeds = new HorizontalSeekBar[mainapp.maxThrottlesCurrentScreen];

        TypedArray function_buttons_table_resource_ids = getResources().obtainTypedArray(R.array.function_buttons_table_resource_ids);
        TypedArray speed_resource_ids = getResources().obtainTypedArray(R.array.speed_resource_ids);
        TypedArray throttle_set_speed_resource_ids = getResources().obtainTypedArray(R.array.throttle_set_speed_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

            functionButtonViewGroups[throttleIndex] = findViewById(function_buttons_table_resource_ids.getResourceId(throttleIndex,0));
            sbSpeeds[throttleIndex] = findViewById(speed_resource_ids.getResourceId(throttleIndex,0));
            bPauses[throttleIndex] = null;
            llSetSpeeds[throttleIndex] = findViewById(throttle_set_speed_resource_ids.getResourceId(throttleIndex,0));

            sbSpeeds[throttleIndex].setTickType(tick_type.TICK_0_100);
        }

        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();

        function_buttons_table_resource_ids.recycle();
        speed_resource_ids.recycle();
        throttle_set_speed_resource_ids.recycle();

    } // end initialiseUiElements()

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            setLabels();       // need to redraw button Press states since ActionBar and Notification access clears them
        }
    }


    // lookup and set values of various informational text labels and size the
    // screen elements
    protected void setLabels() {
//        Log.d(threaded_application.applicationName, activityName + ": set_labels() starting");
        super.setLabels();

        if (mainapp.appIsFinishing) {
            return;
        }

        int throttle_count = 0;
        int[] heights = {0, 0, 0, 0, 0, 0};

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

        if (mainapp.consists == null) {
            Log.d(threaded_application.applicationName, activityName + ": set_Labels() consists is null");
            return;
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            Button b = bSels[throttleIndex];
            bLabel = getApplicationContext().getResources().getString(R.string.locoPressToSelect);
            bLabelPlainText = bLabel;

            Consist con = mainapp.consists[throttleIndex];
            if (con == null) {
                Log.d(threaded_application.applicationName, activityName + ": set_Labels() consists[" + throttleIndex + "] is null");
            } else {
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
                    throttle_count++;

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
            int textSize = (int) (conNomTextSize * textScale);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
//            b.setText(bLabel);
            b.setText(Html.fromHtml(bLabel));
            b.setSelected(false);
            b.setPressed(false);
        }

        setImmersiveMode(webView);
        int screenHeight = getAvailableScreenHeight();

        // set height of Loco Id and Direction Button areas
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

            LinearLayout.LayoutParams llLidp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newDlihHeight);
            llLocoIdAndSpeedViewGroups[throttleIndex].setLayoutParams(llLidp);
            llLocoDirectionButtonViewGroups[throttleIndex].setLayoutParams(llLidp);

            tvSpdVals[throttleIndex].setTextSize(TypedValue.COMPLEX_UNIT_SP, newDlihFontSize);

            //set height of slider areas
            LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newHeight);
            llSetSpeeds[throttleIndex].setLayoutParams(llLp);

            //set margins of slider areas
            int sliderMargin = threaded_application.getIntPrefValue(prefs, "prefLeftSliderMargin", getApplicationContext().getResources().getString(R.string.prefSliderLeftMarginDefaultValue));

            //show speed buttons based on pref
            llSetSpeeds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default

            sbs[throttleIndex].setVisibility(View.VISIBLE);  //always show slider if buttons not shown
            if (prefs.getBoolean("prefDisplaySpeedButtons", false)) {
                bLSpds[throttleIndex].setVisibility(View.VISIBLE);
                bRSpds[throttleIndex].setVisibility(View.VISIBLE);
                bLSpds[throttleIndex].setText(speedButtonLeftText);
                bRSpds[throttleIndex].setText(speedButtonRightText);
                //if buttons enabled, hide the slider if requested
                if (prefs.getBoolean("prefHideSlider", false)) {
                    sbs[throttleIndex].setVisibility(View.GONE);
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

            int additionalPadding = (sbs[throttleIndex].getWidth() > 400 ? 40 : 20);
            sbs[throttleIndex].setPadding(additionalPadding + sliderMargin, 0, additionalPadding + sliderMargin, 0);

            // update the state of each function button based on shared variable
            setAllFunctionStates(throttleIndex);
        }


        if ((screenHeight > throttleMargin) && (mainapp.consists != null)) { // don't do this if height is invalid
            // Log.d(threaded_application.applicationName, activityName + ": starting screen height adjustments, screenHeight=" + screenHeight);
            // determine how to split the screen (evenly if all three, 45/45/10 for two, 80/10/10 if only one)

            adjustThrottleHeights();

            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

                int[] location = new int[2];
                throttleOverlay.getLocationOnScreen(location);
                int ovx = location[0];
                int ovy = location[1];

                location = new int[2];
                sbSpeeds[throttleIndex].getLocationOnScreen(location);
                int x = location[0];
                int y = location[1];

                sliderTopLeftX[throttleIndex] = x - ovx;
                sliderTopLeftY[throttleIndex] = y - ovy;
                sliderBottomRightX[throttleIndex] = x + sbSpeeds[throttleIndex].getWidth() - ovx;
                sliderBottomRightY[throttleIndex] = y + sbSpeeds[throttleIndex].getHeight() - ovy;

//            Log.d(threaded_application.applicationName, activityName + ": setLabels(): slider: " + throttleIndex + " Top: " + sliderTopLeftX[throttleIndex] + ", " + sliderTopLeftY[throttleIndex]
//                    + " Bottom: " + sliderBottomRightX[throttleIndex] + ", " + sliderBottomRightY[throttleIndex]);
            }
        } else {
            Log.d(threaded_application.applicationName, activityName + ": screen height adjustments skipped, screenHeight=" + screenHeight);
        }

        // update the direction indicators
        showDirectionIndications();

//        // update the state of each function button based on shared variable
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            setAllFunctionStates(throttleIndex);
        }

        // Log.d(threaded_application.applicationName, activityName + ": setLabels() end");

    }

    @Override
    boolean canChangeVolumeIndicatorOnTouch(boolean isSpeedButtonOrSlider) {
        return prefHideFunctionButtonsOfNonSelectedThrottle;
    }

    @Override
    void adjustThrottleHeights() {
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int[] throttleHeights = {0, 0, 0, 0, 0, 0};

        double height = getAvailableScreenHeight();

        if ((height > throttleMargin) && (mainapp.consists != null)) { // don't do this if height is invalid

            if (mainapp.prefNumThrottles == 1) {        // just one throttle
                throttleHeights[0] = (int) height;
            } else {
                boolean[] throttlesInUse = {false, false, false, false, false, false};
                int throttlesInUseCount = 0;
                double newHeight = 0;
                for (int i = 0; i < mainapp.prefNumThrottles; i++) {
                    if ((mainapp.consists[i] != null) && (mainapp.consists[i].isActive())) {
                        throttlesInUse[i] = true;
                        throttlesInUseCount++;
                    }
                }

                double activeHeight = 0;
                double semiActiveHeight = 0;
                double inactiveHeight = llLocoIdAndSpeedViewGroups[0].getHeight();

                if (!prefHideFunctionButtonsOfNonSelectedThrottle) {

                    if (mainapp.prefNumThrottles == 2) {
                        if (throttlesInUseCount <= 1) {
                            activeHeight = height - llLocoIdAndSpeedViewGroups[0].getHeight();
                        } else {  // equals 2
                            activeHeight = (height - llLocoIdAndSpeedViewGroups[0].getHeight()) / 2;
                        }
                    } else { // equals 3
                        if (throttlesInUseCount <= 1) {
                            activeHeight = height - (llLocoIdAndSpeedViewGroups[0].getHeight() * 2);
                        } else if (throttlesInUseCount == 2) {
                            activeHeight = (height - llLocoIdAndSpeedViewGroups[0].getHeight()) / 2;
                        } else {  // equals 3
                            activeHeight = (height - llLocoIdAndSpeedViewGroups[0].getHeight()) / 3;
                        }
                    }
                    for (int i = 0; i < mainapp.prefNumThrottles; i++) {
                        if (throttlesInUse[i]) {
                            throttleHeights[i] = (int) activeHeight;
                        } else {
                            throttleHeights[i] = (int) inactiveHeight;
                        }
                    }

                } else { // hide function buttons of non-selected throttle
                    // one is always notionally 'active'
                    int semiActiveCount = (throttlesInUseCount == 0) ? 0 : (throttlesInUseCount - 1);
                    int inactiveCount = (throttlesInUseCount <= 1) ? (mainapp.prefNumThrottles - 1)  : (mainapp.prefNumThrottles - 1 - semiActiveCount);
                    semiActiveHeight = llLocoIdAndSpeedViewGroups[0].getHeight()
                            + llLocoDirectionButtonViewGroups[0].getHeight()
                            + llSetSpeeds[0].getHeight() + 6 * displayMetrics.density;  // 6 is for the padding
                    activeHeight = height - (semiActiveHeight * semiActiveCount) - (inactiveHeight * inactiveCount);

                    for (int i = 0; i < mainapp.prefNumThrottles; i++) {
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

            LinearLayout.LayoutParams layoutParams;
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                // set height of each area
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, throttleHeights[throttleIndex]);
//                layoutParams.bottomMargin = (int) (throttleMargin * (displayMetrics.densityDpi / 160.));
                llThrottleLayouts[throttleIndex].setLayoutParams(layoutParams);
            }
        }
    }

    int getAvailableScreenHeight() {   // excluding the webview if displayed
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        final float density = displayMetrics.density;

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
            // Log.d(threaded_application.applicationName, activityName + ": vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
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

    @Override
    protected void getCommonPrefs(boolean isCreate) {
        super.getCommonPrefs(isCreate);

        prefHideFunctionButtonsOfNonSelectedThrottle = prefs.getBoolean("prefHideFunctionButtonsOfNonSelectedThrottle", false);
    }
}