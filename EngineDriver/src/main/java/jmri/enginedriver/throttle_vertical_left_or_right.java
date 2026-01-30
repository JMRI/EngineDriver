/*Copyright (C) 2017-2026 M. Steve Todd mstevetodd@gmail.com

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.LinkedHashMap;

import jmri.enginedriver.type.max_throttles_current_screen_type;
import jmri.enginedriver.type.throttle_screen_type;
import jmri.enginedriver.type.tick_type;
import jmri.enginedriver.util.VerticalSeekBar;
import jmri.enginedriver.type.slider_type;
import jmri.enginedriver.type.web_view_location_type;

public class throttle_vertical_left_or_right extends throttle {
    static final String activityName = "throttle_vertical_left_or_right";

    protected static final int MAX_SCREEN_THROTTLES = max_throttles_current_screen_type.VERTICAL;
    protected static final int MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT = max_throttles_current_screen_type.VERTICAL_LEFT_OR_RIGHT;
    protected static final int  MAX_SCREEN_THROTTLES_VERTICAL_TABLET_LEFT = max_throttles_current_screen_type.VERTICAL_TABLET;

    private LinearLayout[] lThrottles;
    private LinearLayout[] lUppers;
    private LinearLayout[] lLowers;
    private ScrollView[] svFunctionButtons;

    protected void removeLoco(int whichThrottle) {
        super.removeLoco(whichThrottle);
        set_function_labels_and_listeners_for_view(whichThrottle);
    }


    protected void setScreenDetails() {
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
        mainapp.currentScreenSupportsWebView = true;
        sliderType = slider_type.VERTICAL;

        prefThrottleScreenType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        switch (prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault))) {
            case "Vertical":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
                mainapp.throttleLayoutViewId = R.layout.throttle_vertical;
                break;
            case "Vertical Right":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT;
                mainapp.throttleLayoutViewId = R.layout.throttle_vertical_right;
                break;
            case "Tablet Vertical Left":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES_VERTICAL_TABLET_LEFT;
                mainapp.throttleLayoutViewId = R.layout.throttle_vertical_tablet_left;
                break;
            case "Vertical Left":
            default:
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES_LEFT_OR_RIGHT;
                mainapp.throttleLayoutViewId = R.layout.throttle_vertical_left;
                break;
        }
    }

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
            if (throttleIndex < mainapp.prefNumThrottles) {
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

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            functionButtonViewGroups[throttleIndex] = findViewById(function_buttons_table_resource_ids.getResourceId(throttleIndex,0));
        }

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        llSetSpeeds = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        svFunctionButtons = new ScrollView[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        lUppers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        lLowers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        TypedArray throttle_resource_ids = getResources().obtainTypedArray(R.array.throttle_resource_ids);
        TypedArray loco_upper_resource_ids = getResources().obtainTypedArray(R.array.loco_upper_resource_ids);
        TypedArray loco_lower_resource_ids = getResources().obtainTypedArray(R.array.loco_lower_resource_ids);
        TypedArray throttle_set_speed_resource_ids = getResources().obtainTypedArray(R.array.throttle_set_speed_resource_ids);
        TypedArray speed_resource_ids = getResources().obtainTypedArray(R.array.speed_resource_ids);
        TypedArray function_buttons_scroller_resource_ids = getResources().obtainTypedArray(R.array.function_buttons_scroller_resource_ids);
        TypedArray button_pause_resource_ids = getResources().obtainTypedArray(R.array.button_pause_resource_ids);

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex] = findViewById(throttle_resource_ids.getResourceId(throttleIndex,0));
            lUppers[throttleIndex] = findViewById(loco_upper_resource_ids.getResourceId(throttleIndex,0));
            lLowers[throttleIndex] = findViewById(loco_lower_resource_ids.getResourceId(throttleIndex,0));
            llSetSpeeds[throttleIndex] = findViewById(throttle_set_speed_resource_ids.getResourceId(throttleIndex,0));
            vsbSpeeds[throttleIndex] = findViewById(speed_resource_ids.getResourceId(throttleIndex,0));
            svFunctionButtons[throttleIndex] = findViewById(function_buttons_scroller_resource_ids.getResourceId(throttleIndex,0));
            bPauses[throttleIndex] = findViewById(button_pause_resource_ids.getResourceId(throttleIndex,0));

            vsbSpeeds[throttleIndex].setTickType(tick_type.TICK_0_100);
            PauseSpeedButtonTouchListener pauseSpeedButtonTouchListener = new PauseSpeedButtonTouchListener(throttleIndex);
            bPauses[throttleIndex].setOnTouchListener(pauseSpeedButtonTouchListener);
        }

        function_buttons_table_resource_ids.recycle();
        throttle_resource_ids.recycle();
        loco_upper_resource_ids.recycle();
        loco_lower_resource_ids.recycle();
        throttle_set_speed_resource_ids.recycle();
        speed_resource_ids.recycle();
        function_buttons_scroller_resource_ids.recycle();
        button_pause_resource_ids.recycle();

    }

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

        if (mainapp.appIsFinishing) { return;}

        // avoid NPE by not letting this run too early (reported to Play Store)
        if (tvVolumeIndicators[0] == null) return;

        final int conNomTextSize = 24;
        final double minTextScale = 0.5;
        String bLabel;
        String bLabelPlainText;
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            Button b = bSelects[throttleIndex];
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
                tvbSelectsLabels[throttleIndex].setVisibility(View.GONE);

            } else {
                bLabel = getApplicationContext().getResources().getString(R.string.locoPressToSelect);
                bLabelPlainText = bLabel;
                tvbSelectsLabels[throttleIndex].setVisibility(View.VISIBLE);
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
            if (!prefThrottleScreenType.equals(throttle_screen_type.VERTICAL)) {
                textSize = (int) (conNomTextSize * textScale);
            }
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
//            b.setText(bLabel);
            b.setText(Html.fromHtml(bLabel));
            b.setSelected(false);
            b.setPressed(false);
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

            //show speed buttons based on pref
            vsbSpeeds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default
            if (prefs.getBoolean("prefHideSlider", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
                vsbSpeeds[throttleIndex].setVisibility(View.GONE);
            }
        }

        // update the direction indicators
        showDirectionIndications();

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        // Get the screen's density scale
        final float denScale = dm.density;


        int screenWidth = vThrottleScreenWrap.getWidth(); // get the width of usable area
        int throttleWidth = screenWidth / mainapp.prefNumThrottles;
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            lThrottles[throttleIndex].getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
            lThrottles[throttleIndex].getLayoutParams().width = throttleWidth;
            lThrottles[throttleIndex].requestLayout();
            llSetSpeeds[throttleIndex].requestLayout();
        }

        setImmersiveMode(webView);
        int screenHeight = vThrottleScreenWrap.getHeight(); // get the Height of usable area
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
            //Log.d(threaded_application.applicationName, activityName + ": setLabels(): vThrottleScreenWrap.getHeight()=0, new screenHeight=" + screenHeight);
        }

        // save part the screen for webview
        if (prefWebViewLocation.equals(web_view_location_type.TOP) || prefWebViewLocation.equals(web_view_location_type.BOTTOM)) {
            webViewIsOn = true;
            if (!prefIncreaseWebViewSize) {
                screenHeight = (int) Math.round(screenHeight * 0.5); // save half the screen
            } else {
                screenHeight = (int) Math.round(screenHeight * 0.6); // save 60% of the screen for web view
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

        if (prefs.getBoolean("prefHideSlider", getResources().getBoolean(R.bool.prefHideSliderDefaultValue))) {
            speedButtonHeight = (int) ((screenHeight
                    - stopButtonParams.topMargin
                    - stopButtonParams.bottomMargin
                    - (220 * denScale)) / 2);
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            //show speed buttons based on pref
            if (prefs.getBoolean("prefDisplaySpeedButtons", false)) {
                bLeftSpeeds[throttleIndex].setVisibility(View.VISIBLE);
                bRightSpeeds[throttleIndex].setVisibility(View.VISIBLE);

                bLeftSpeeds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                bLeftSpeeds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bLeftSpeeds[throttleIndex].requestLayout();
                bRightSpeeds[throttleIndex].getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                bRightSpeeds[throttleIndex].getLayoutParams().height = speedButtonHeight;
                bRightSpeeds[throttleIndex].requestLayout();
            } else {
                bLeftSpeeds[throttleIndex].setVisibility(View.GONE);
                bRightSpeeds[throttleIndex].setVisibility(View.GONE);
            }

//            bStops[throttleIndex].getLayoutParams().height = (int) (speedButtonHeight * 0.8);
            bStops[throttleIndex].setLayoutParams(stopButtonParams);
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            // set height of each function button area
            svFunctionButtons[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            svFunctionButtons[throttleIndex].requestLayout();
            lLowers[throttleIndex].getLayoutParams().height = screenHeight - lUppers[throttleIndex].getHeight();
            lLowers[throttleIndex].requestLayout();

            int[] location = new int[2];
            throttleOverlay.getLocationOnScreen(location);
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
                || bForwards[whichThrottle] == null) {
            return;
        }
        if (!forceDisable) { // avoid index crash, but may simply push to next line
            newEnabledState = mainapp.consists[whichThrottle].isActive(); // set false if lead loco is not assigned
        }

        super.enableDisableButtons(whichThrottle, forceDisable);

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
}