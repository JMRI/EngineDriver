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

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.auto_increment_or_decrement_type;
import jmri.enginedriver.type.kids_timer_action_type;
import jmri.enginedriver.type.tick_type;
import jmri.enginedriver.util.HorizontalSeekBar;
import jmri.enginedriver.type.slider_type;
import jmri.enginedriver.type.web_view_location_type;

public class throttle_switching_horizontal extends throttle {

    protected static final int MAX_SCREEN_THROTTLES = 3;

    private LinearLayout[] lThrottles;
//    private LinearLayout[] lUppers;
//    private LinearLayout[] lLowers;
    private LinearLayout[] lSpeeds;
    private ScrollView[] svFnBtns;

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

        maxThrottlePcnt = threaded_application.getIntPrefValue(prefs, "maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
        maxThrottle = (int) Math.round(MAX_SPEED_VAL_WIT * (0.01 * maxThrottlePcnt)); // convert from percent

        prefSwitchingThrottleSliderDeadZone = Integer.parseInt(prefs.getString("prefSwitchingThrottleSliderDeadZone", getResources().getString(R.string.prefSwitchingThrottleSliderDeadZoneDefaultValue)));

        prefHideFunctionButtonsOfNonSelectedThrottle = prefs.getBoolean("prefHideFunctionButtonsOfNonSelectedThrottle", false);
    }

    @SuppressLint({"Recycle", "SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Engine_Driver", "throttle_switching_horizontal: onCreate(): called");

        mainapp = (threaded_application) this.getApplication();

        mainapp.currentScreenSupportsWebView = true;

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        //noinspection SwitchStatementWithTooFewBranches
        switch (prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault))) {
            case "Switching Horizontal":
                mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;
                mainapp.throttleLayoutViewId = R.layout.throttle_switching_horizontal;
                break;
        }

        super.onCreate(savedInstanceState);

        if (mainapp.appIsFinishing) { return;}

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            switch (throttleIndex) {
                case 0:
                    functionButtonViewGroups[throttleIndex] = findViewById(R.id.function_buttons_table_0);
                    tvDirectionIndicatorForwards[throttleIndex] = findViewById(R.id.direction_indicator_forward_0);
                    tvDirectionIndicatorReverses[throttleIndex] = findViewById(R.id.direction_indicator_reverse_0);
                    break;
                case 1:
                    functionButtonViewGroups[throttleIndex] = findViewById(R.id.function_buttons_table_1);
                    tvDirectionIndicatorForwards[throttleIndex] = findViewById(R.id.direction_indicator_forward_1);
                    tvDirectionIndicatorReverses[throttleIndex] = findViewById(R.id.direction_indicator_reverse_1);
                    break;
                case 2:
                    functionButtonViewGroups[throttleIndex] = findViewById(R.id.function_buttons_table_2);
                    tvDirectionIndicatorForwards[throttleIndex] = findViewById(R.id.direction_indicator_forward_2);
                    tvDirectionIndicatorReverses[throttleIndex] = findViewById(R.id.direction_indicator_reverse_2);
                    break;            }

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
        sbSpeeds = new HorizontalSeekBar[mainapp.maxThrottlesCurrentScreen];
        hsbSwitchingSpeeds = new HorizontalSeekBar[mainapp.maxThrottlesCurrentScreen];
//        lUppers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
//        lLowers = new LinearLayout[mainapp.maxThrottlesCurrentScreen];

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            switch (throttleIndex) {
                default:
                case 0:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_0);
//                    lUppers[throttleIndex] = findViewById(R.id.loco_upper_0);
//                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_0);
                    lSpeeds[throttleIndex] = findViewById(R.id.throttle_0_SetSpeed);
                    sbSpeeds[throttleIndex] = findViewById(R.id.speed_0);
                    hsbSwitchingSpeeds[throttleIndex] = findViewById(R.id.speed_switching_0);
                    hsbSwitchingSpeeds[throttleIndex].setTickType(tick_type.TICK_0_100_0);
//                    hsbSwitchingSpeeds[throttleIndex].setMax(MAX_SPEED_VAL_WIT);
                    hsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
                    hsbSwitchingSpeeds[throttleIndex].setProgress(throttleMidPointZero[throttleIndex]);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_0);

                    llSetSpeedLayouts[throttleIndex] = findViewById(R.id.throttle_0_SetSpeed);
                    break;
                case 1:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_1);
//                    lUppers[throttleIndex] = findViewById(R.id.loco_upper_1);
//                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_1);
                    lSpeeds[throttleIndex] = findViewById(R.id.throttle_1_SetSpeed);
                    sbSpeeds[throttleIndex] = findViewById(R.id.speed_1);
                    hsbSwitchingSpeeds[throttleIndex] = findViewById(R.id.speed_switching_1);
                    hsbSwitchingSpeeds[throttleIndex].setTickType(tick_type.TICK_0_100_0);
//                    hsbSwitchingSpeeds[throttleIndex].setMax(MAX_SPEED_VAL_WIT);
                    hsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
                    hsbSwitchingSpeeds[throttleIndex].setProgress(throttleMidPointZero[throttleIndex]);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_1);

                    llSetSpeedLayouts[throttleIndex] = findViewById(R.id.throttle_1_SetSpeed);
                    break;
                case 2:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_2);
//                    lUppers[throttleIndex] = findViewById(R.id.loco_upper_2);
//                    lLowers[throttleIndex] = findViewById(R.id.loco_lower_2);
                    lSpeeds[throttleIndex] = findViewById(R.id.throttle_2_SetSpeed);
                    sbSpeeds[throttleIndex] = findViewById(R.id.speed_2);
                    hsbSwitchingSpeeds[throttleIndex] = findViewById(R.id.speed_switching_2);
                    hsbSwitchingSpeeds[throttleIndex].setTickType(tick_type.TICK_0_100_0);
//                    hsbSwitchingSpeeds[throttleIndex].setMax(MAX_SPEED_VAL_WIT);
                    hsbSwitchingSpeeds[throttleIndex].setMax(throttleSwitchingMax[throttleIndex]);
                    hsbSwitchingSpeeds[throttleIndex].setProgress(throttleMidPointZero[throttleIndex]);
                    svFnBtns[throttleIndex] = findViewById(R.id.function_buttons_scroller_2);

                    llSetSpeedLayouts[throttleIndex] = findViewById(R.id.throttle_2_SetSpeed);
                    break;
            }
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            sbs[throttleIndex].setMax(Math.round(maxThrottle));
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
        sliderType = slider_type.SWITCHING;
    } // end of onCreate()

    @Override
    public void onResume() {
        Log.d("Engine_Driver", "throttle_switching_horizontal: onResume(): called");
        super.onResume();

        if (mainapp.appIsFinishing) { return;}

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            if( throttleIndex < mainapp.numThrottles) {
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

    //    // lookup and set values of various informational text labels and size the
//    // screen elements
//    @SuppressWarnings("deprecation")
//    @Override
    protected void set_labels() {
        Log.d("Engine_Driver","throttle_switching_horizontal: set_labels() starting");
        super.set_labels();

        if (mainapp.appIsFinishing) { return;}

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
        if (pref_increase_slider_height_preference) {
            newHeight = (int) (80 * denScale + 0.5f); // increased height
        } else {
            newHeight = (int) (50 * denScale + 0.5f); // normal height
        }

        final int conNomTextSize = 24;
        final double minTextScale = 0.5;
        String bLabel;
        String bLabelPlainText;

        if(mainapp.consists==null) {
            Log.d("Engine_Driver", "throttle_original.setLabels consists is null");
            return;
        }

        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            Button b = bSels[throttleIndex];
            bLabel = getApplicationContext().getResources().getString(R.string.locoPressToSelect);
            bLabelPlainText = bLabel;

            Consist con = mainapp.consists[throttleIndex];
            if(con==null) {
                Log.d("Engine_Driver", "throttle_original setLabels consists[" + throttleIndex + "] is null");
            }
            else {
                if (con.isActive()) {
                    if (!prefShowAddressInsteadOfName) {
                        if (!overrideThrottleNames[throttleIndex].equals("")) {
                            bLabel = overrideThrottleNames[throttleIndex];
                            bLabelPlainText = overrideThrottleNames[throttleIndex];
                        } else {
                            bLabel = con.toHtml();
                            bLabelPlainText = con.toString();
                        }

//                        bLabel = mainapp.consists[throttleIndex].toString();
//                        bLabelPlainText = mainapp.consists[throttleIndex].toString();
//                        bLabel = mainapp.consists[throttleIndex].toHtml();
                    } else {
                        if (overrideThrottleNames[throttleIndex].equals("")) {
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
            int textSize = (int) (conNomTextSize * textScale * 0.95);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
//            b.setText(bLabel);
            b.setText(Html.fromHtml(bLabel));
            b.setSelected(false);
            b.setPressed(false);
        }

        if (webView!=null) {
            setImmersiveModeOn(webView, false);
        }

        int screenHeight = (int) getAvailableSceenHeight();
//
//        int screenHeight = vThrotScrWrap.getHeight(); // get the height of usable area
//        int fullScreenHeight = screenHeight;
//        if ((toolbar != null) && (!prefThrottleViewImmersiveModeHideToolbar))  {
//            titleBar = mainapp.getToolbarHeight(toolbar, statusLine,  screenNameLine);
//            if (screenHeight!=0) {
//                screenHeight = screenHeight - titleBar;
//            }
//        }
//        //Log.d("Engine_Driver","vThrotScrWrap.getHeight(), screenHeight=" + screenHeight);
//        if (screenHeight == 0) {
//            // throttle screen hasn't been drawn yet, so use display metrics for now
//            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
//            //Log.d("Engine_Driver","vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
//        }
//
//        // save part the screen for webview
//        if (!webViewLocation.equals(web_view_location_type.NONE)) {
//            webViewIsOn = true;
//            if (!prefIncreaseWebViewSize) {
//                screenHeight *= 0.5; // save half the screen
//            } else {
//                screenHeight *= 0.4; // save 60% of the screen for web view
//            }
//            LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,fullScreenHeight - titleBar - screenHeight);
//            webView.setLayoutParams(webViewParams);
//        }

//        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
//            switch (throttleIndex) {
//                default:
//                case 0:
//                    llSetSpeedLayouts[throttleIndex] = findViewById(R.id.throttle_0_SetSpeed);
//                    break;
//                case 1:
//                    llSetSpeedLayouts[throttleIndex] = findViewById(R.id.throttle_1_SetSpeed);
//                    break;
//                case 2:
//                    llSetSpeedLayouts[throttleIndex] = findViewById(R.id.throttle_2_SetSpeed);
//                    break;
//            }
//        }

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
            llSetSpeedLayouts[throttleIndex].setLayoutParams(llLp);

            //set margins of slider areas
            int sliderMargin = threaded_application.getIntPrefValue(prefs, "left_slider_margin", getApplicationContext().getResources().getString(R.string.prefSliderLeftMarginDefaultValue));

            //show speed buttons based on pref
            llSetSpeedLayouts[throttleIndex].setVisibility(View.VISIBLE); //always show as a default

            hsbSwitchingSpeeds[throttleIndex].setVisibility(View.VISIBLE);  //always show slider if buttons not shown
            if (prefs.getBoolean("display_speed_arrows_buttons", false)) {
                bLSpds[throttleIndex].setVisibility(View.VISIBLE);
                bRSpds[throttleIndex].setVisibility(View.VISIBLE);
                bLSpds[throttleIndex].setText(speedButtonLeftText);
                bRSpds[throttleIndex].setText(speedButtonRightText);
                //if buttons enabled, hide the slider if requested
                if (prefs.getBoolean("hide_slider_preference", false)) {
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
                llSetSpeedLayouts[throttleIndex].setVisibility(View.GONE);
            }

            int additionalPadding = (sbs[throttleIndex].getWidth()>400 ? 40 : 20);
            hsbSwitchingSpeeds[throttleIndex].setPadding(additionalPadding+sliderMargin, 0, additionalPadding+sliderMargin, 0);

            // update the state of each function button based on shared variable
            setAllFunctionStates(throttleIndex);
        }


        if ( (screenHeight > throttleMargin) && (mainapp.consists!=null)) { // don't do this if height is invalid
            //Log.d("Engine_Driver","starting screen height adjustments, screenHeight=" + screenHeight);
            // determine how to split the screen (evenly if all three, 45/45/10 for two, 80/10/10 if only one)


            adjustThrottleHeights();

//            screenHeight -= throttleMargin;
//
//            if (mainapp.numThrottles == 1) {        // just one throttle
//                heights[0] = screenHeight;
//                heights[1] = 0;
//                heights[2] = 0;
//            } else {                                // 2 or more throttles - split screen among active throttles
//                boolean con0Active = false;
//                boolean con1Active = false;
//                boolean con2Active = false;
//                if (mainapp.consists[0] == null)
//                    Log.d("Engine_Driver", "throttle_original.set_labels() consists[0] is null");
//                else
//                    con0Active = mainapp.consists[0].isActive();
//                if( mainapp.consists[1] == null)
//                    Log.d("Engine_Driver", "throttle_original.set_labels() consists[1] is null");
//                else
//                    con1Active = mainapp.consists[1].isActive();
//                if (mainapp.consists[2] == null) {
//                    Log.d("Engine_Driver", "throttle_original.set_labels() consists[2] is null");
//                }
//                else
//                    con2Active = mainapp.consists[2].isActive();
//
//                if (mainapp.numThrottles == 2) {
//                    if (!con1Active) {
//                        heights[0] = (int) (screenHeight * 0.9);
//                        heights[1] = (int) (screenHeight * 0.10);
//                        heights[2] = 0;
//                    } else if (!con0Active) {
//                        heights[0] = (int) (screenHeight * 0.10);
//                        heights[1] = (int) (screenHeight * 0.9);
//                        heights[2] = 0;
//                    } else {
//                        heights[0] = (int) (screenHeight * 0.5);
//                        heights[1] = (int) (screenHeight * 0.5);
//                        heights[2] = 0;
//                    }
//                } else if (throttle_count == 0 || throttle_count == 3) {    // none active or all active
//                    heights[0] = (int) (screenHeight * 0.33);
//                    heights[1] = (int) (screenHeight * 0.33);
//                    heights[2] = (int) (screenHeight * 0.33);
//                } else if (!con0Active && !con1Active) {            // just throttle 2 active
//                    heights[0] = (int) (screenHeight * 0.10);
//                    heights[1] = (int) (screenHeight * 0.10);
//                    heights[2] = (int) (screenHeight * 0.80);
//                } else if (!con0Active && !con2Active) {            // just throttle 1 active
//                    heights[0] = (int) (screenHeight * 0.10);
//                    heights[1] = (int) (screenHeight * 0.80);
//                    heights[2] = (int) (screenHeight * 0.10);
//                } else if (!con1Active && !con2Active) {            // just throttle 0 active
//                    heights[0] = (int) (screenHeight * 0.80);
//                    heights[1] = (int) (screenHeight * 0.10);
//                    heights[2] = (int) (screenHeight * 0.10);
//                } else if (!con0Active) {                           // throttles 1 and 2 active
//                    heights[0] = (int) (screenHeight * 0.10);
//                    heights[1] = (int) (screenHeight * 0.45);
//                    heights[2] = (int) (screenHeight * 0.45);
//                } else if (!con1Active) {                           // throttles 0 and 2 active
//                    heights[0] = (int) (screenHeight * 0.45);
//                    heights[1] = (int) (screenHeight * 0.10);
//                    heights[2] = (int) (screenHeight * 0.45);
//                } else {                                            // throttles 0 and 1 active
//                    heights[0] = (int) (screenHeight * 0.45);
//                    heights[1] = (int) (screenHeight * 0.45);
//                    heights[2] = (int) (screenHeight * 0.10);
//                }
//            }
//
//            ImageView myImage = findViewById(R.id.backgroundImgView);
//            myImage.getLayoutParams().height = screenHeight;

            LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newHeight);
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

                // update throttle slider top/bottom
//                tops[throttleIndex] = llThrottleLayouts[throttleIndex].getTop() + sbs[throttleIndex].getTop() + bSels[throttleIndex].getHeight() + bFwds[throttleIndex].getHeight();
//                bottoms[throttleIndex] = llThrottleLayouts[throttleIndex].getTop() + sbs[throttleIndex].getBottom() + bSels[throttleIndex].getHeight() + bFwds[throttleIndex].getHeight();

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

//            Log.d("Engine_Driver","slider: " + throttleIndex + " Top: " + sliderTopLeftX[throttleIndex] + ", " + sliderTopLeftY[throttleIndex]
//                    + " Bottom: " + sliderBottomRightX[throttleIndex] + ", " + sliderBottomRightY[throttleIndex]);
            }
        } else {
            Log.d("Engine_Driver", "screen height adjustments skipped, screenHeight=" + screenHeight);
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


        // Log.d("Engine_Driver","ending set_labels");

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
        // Log.d("Engine_Driver","starting enableDisableButtonsForView " +
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
    } // enableDisableButtonsForView

    // update the appearance of all function buttons
    @Override
    void setAllFunctionStates(int whichThrottle) {
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

        @SuppressLint("ClickableViewAccessibility")
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

//            Log.d("Engine_Driver", "onProgressChanged: fromUser: " + fromUser + " touchFromUser: " + hsbSwitchingSpeeds[whichThrottle].touchFromUser + " limitedJump: " + limitedJump);
//            Log.d("Engine_Driver", "onProgressChanged: isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);

            // limit speed change if change was initiated by a user slider touch (prevents "bouncing")
            if ((fromUser) || (hsbSwitchingSpeeds[whichThrottle].touchFromUser) ) {

                if (!limitedJump[whichThrottle]) {         // touch generates multiple onProgressChanged events, skip processing after first limited jump

                    if (Math.abs(newSliderPosition - lastSliderPosition) > max_throttle_change) { // if jump is too large then limit it

                        jumpSpeed = getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false);      // save ultimate target value
                        jumpDir = dir; // save ultimate target direction
                        limitedJump[whichThrottle] = true;
                        throttle.setProgress(lastSliderPosition);  // put the slider back to the original position
                        doLocoSound(whichThrottle);

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

//                    Log.d("Engine_Driver", "onProgressChanged -- no throttling");

                    reverseDirectionIfNeeded(dir, whichThrottle);

                    speedUpdate(whichThrottle,
                            getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false));
                    sendSpeedMsg(whichThrottle, speed);
                    setDisplayedSpeed(whichThrottle, speed);

                } else { // got a touch while processing limitJump
//                    Log.d("Engine_Driver", "onProgressChanged -- touch while processing limited jump");
                    newSliderPosition = lastSliderPosition;    //   so suppress multiple touches
                    throttle.setProgress(lastSliderPosition);
                    doLocoSound(whichThrottle);

//                    Log.d("Engine_Driver", "onProgressChange: fromUser: " + fromUser + " hsbSwitchingSpeeds[wt].touchFromUser: " +hsbSwitchingSpeeds[whichThrottle].touchFromUser + " isPauseSpeeds[whichThrottle]: " + isPauseSpeeds[whichThrottle]);
                }

                // Now update ESU MCII Knob position
                if (IS_ESU_MCII) {
                    setEsuThrottleKnobPosition(whichThrottle, speed);
                }

                setActiveThrottle(whichThrottle, true);

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
                        if (mChangeDirectionAtZero) { // if change of direction is needed, then we must be at zero now.  need to continue to the final speed.
                            Log.d("Engine_Driver", "onProgressChanged !!-- Direction change now needed");
                            mChangeDirectionAtZero = false;
                            reverseDirectionIfNeeded(jumpDir, whichThrottle);
                        } else {
                            Log.d("Engine_Driver", "onProgressChanged !!-- LimitedJump hit jump speed.");
                            limitedJump[whichThrottle] = false;
                            setAutoIncrementOrDecrement(whichThrottle, auto_increment_or_decrement_type.OFF);
                            throttle.setProgress(getNewSliderPositionFromSpeed(jumpSpeed, whichThrottle, false));
                            doLocoSound(whichThrottle);
                            speedUpdate(whichThrottle, getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle,false));
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
        doLocoSound(whichThrottle);

//        Log.d("Engine_Driver","throttle_switching_left_or_right - speedChange -  speed: " + speed + " change: " + change);

//        speedUpdateAndNotify(whichThrottle, speed);

        doLocoSound(whichThrottle);

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
        doLocoSound(whichThrottle);

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
            hsbSwitchingSpeeds[whichThrottle].setProgress(sliderPosition);
        }
        doLocoSound(whichThrottle);
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

    protected void limitSpeed(int whichThrottle) {
        int dir = getDirection(whichThrottle);
        int speed = getSpeedFromSliderPosition(hsbSwitchingSpeeds[whichThrottle].getProgress(),whichThrottle, false);
//                Log.d("Engine_Driver","limit_speed_button_switching_touch_listener -  speed: " + speed );

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

        Log.d("Engine_Driver","limitSpeed -  speed: " + speed );
        speedChangeAndNotify(whichThrottle,0);
        setActiveThrottle(whichThrottle); // set the throttle the volume keys control depending on the preference
    }

    @Override
    boolean canChangeVolumeIndicatorOnTouch(boolean isSpeedButtonOrSlider) {
        if (!prefHideFunctionButtonsOfNonSelectedThrottle) return false;
        if (isSpeedButtonOrSlider) return false;
        return true;
    }

    @Override
    void adjustThrottleHeights() {
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int[] throttleHeights = {0, 0, 0, 0, 0, 0};
        boolean[] directionButtonsVisible = {false, false, false, false, false, false};
        boolean[] functionButtonsVisible = {false, false, false, false, false, false};

        int height = getAvailableSceenHeight();

        if ((height > throttleMargin) && (mainapp.consists != null)) { // don't do this if height is invalid

            if (mainapp.numThrottles == 1) {        // just one throttle
                throttleHeights[0] = height;
                directionButtonsVisible[0] = true;
                functionButtonsVisible[0] = true;
            } else {
                boolean[] throttlesInUse = {false, false, false, false, false, false};
                int throttlesInUseCount = 0;
                double newHeight = 0;
                for (int i = 0; i < mainapp.numThrottles; i++) {
                    if ((mainapp.consists[i] != null) && (mainapp.consists[i].isActive())) {
                        throttlesInUse[i] = true;
                        throttlesInUseCount++;
                    }
                }

                double activeHeight = 0;
                double semiActiveHeight = 0;
                double inactiveHeight = llLocoIdAndSpeedViewGroups[0].getHeight();

                if (!prefHideFunctionButtonsOfNonSelectedThrottle) {

                    if (mainapp.numThrottles == 2) {
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
                            + llSetSpeedLayouts[0].getHeight();
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

    int getAvailableSceenHeight() {   // excluding the webview if displayed
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        final float density = displayMetrics.density;

        int screenHeight = vThrotScrWrap.getHeight(); // get the height of usable area
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
            //Log.d("Engine_Driver","vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
        }

        double height = screenHeight;
        LinearLayout.LayoutParams overlayParams;
        if(!webViewLocation.equals(web_view_location_type.NONE))     {
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