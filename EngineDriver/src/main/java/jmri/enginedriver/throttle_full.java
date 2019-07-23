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
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class throttle_full extends throttle {

    protected static final int MAX_SCREEN_THROTTLES = 3;

    protected void removeLoco(int whichThrottle) {
        super.removeLoco(whichThrottle);
        set_function_labels_and_listeners_for_view(whichThrottle);
    }


//    @Override
//    void enable_disable_buttons(int whichThrottle, boolean forceDisable) {
//        super.enable_disable_buttons(whichThrottle, forceDisable);
//
//        boolean newEnabledState = false;
//        if (!forceDisable) {
//            newEnabledState = mainapp.consists[whichThrottle].isActive();      // set false if lead loco is not assigned
//        }
//
//    } // end of enable_disable_buttons

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


    @SuppressLint({"Recycle", "SetJavaScriptEnabled"})
    @Override
    public void onCreate(Bundle savedInstanceState) {

        mainapp = (threaded_application) this.getApplication();
        mainapp.maxThrottlesCurrentScreen = MAX_SCREEN_THROTTLES;

        super.layoutViewId = R.layout.throttle;
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
                case 2:
                    fbs[throttleIndex] = findViewById(R.id.function_buttons_table_2);
                    break;
            }

        }



        // set label and dcc functions (based on settings) or hide if no label
        setAllFunctionLabelsAndListeners();
//

    } // end of onCreate()


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            set_labels();       // need to redraw button Press states since ActionBar and Notification access clears them
        }
    }


//    void setAllFunctionLabelsAndListeners() {
//        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++) {
//            set_function_labels_and_listeners_for_view(i);
//        }
//    }

    // helper function to set up function buttons for each throttle
    // loop through all function buttons and
    // set label and dcc functions (based on settings) or hide if no label
//    @Override
//    void set_function_labels_and_listeners_for_view(int whichThrottle) {
//        // Log.d("Engine_Driver","starting set_function_labels_and_listeners_for_view");
//
//        ViewGroup tv; // group
//        ViewGroup r; // row
//        function_button_touch_listener fbtl;
//        Button b; // button
//        int k = 0; // button count
//        LinkedHashMap<Integer, String> function_labels_temp;
//        LinkedHashMap<Integer, Button> functionButtonMap = new LinkedHashMap<>();
//
//        tv = fbs[whichThrottle];
//
//        // note: we make a copy of function_labels_x because TA might change it
//        // while we are using it (causing issues during button update below)
//        function_labels_temp = mainapp.function_labels_default;
//        if (!prefAlwaysUseDefaultFunctionLabels) {
//            if (mainapp.function_labels[whichThrottle] != null && mainapp.function_labels[whichThrottle].size() > 0) {
//                function_labels_temp = new LinkedHashMap<>(mainapp.function_labels[whichThrottle]);
//            } else {
//                function_labels_temp = mainapp.function_labels_default;
//            }
//        }
//
//        // put values in array for indexing in next step
//        // to do this
//        ArrayList<Integer> aList = new ArrayList<>();
//        aList.addAll(function_labels_temp.keySet());
//
//        if (tv != null) {
//            for (int i = 0; i < tv.getChildCount(); i++) {
//                r = (ViewGroup) tv.getChildAt(i);
//                for (int j = 0; j < r.getChildCount(); j++) {
//                    b = (Button) r.getChildAt(j);
//                    if (k < function_labels_temp.size()) {
//                        Integer func = aList.get(k);
//                        functionButtonMap.put(func, b); // save function to button
//                        // mapping
//                        String bt = function_labels_temp.get(func);
//                        fbtl = new function_button_touch_listener(func, whichThrottle, bt);
//                        b.setOnTouchListener(fbtl);
//                        if ((mainapp.getCurrentTheme().equals(THEME_DEFAULT))) {
//                            bt = bt + "        ";  // pad with spaces, and limit to 7 characters
//                            b.setText(bt.substring(0, 7));
//                        } else {
//                            bt = bt + "                      ";  // pad with spaces, and limit to 20 characters
//                            b.setText(bt.trim());
//                        }
//                        b.setVisibility(View.VISIBLE);
//                        b.setEnabled(false); // start out with everything disabled
//                    } else {
//                        b.setVisibility(View.GONE);
//                    }
//                    k++;
//                }
//            }
//        }
//
//        // update the function-to-button map for the current throttle
//        functionMaps[whichThrottle] = functionButtonMap;
//    }

    // helper function to get a numbered function button from its throttle and function number
//    Button getFunctionButton(int whichThrottle, int func) {
//        Button b; // button
//        LinkedHashMap<Integer, Button> functionButtonMap;
//
//        functionButtonMap = functionMaps[whichThrottle];
//        b = functionButtonMap.get(func);
//        return b;
//    }

//    // lookup and set values of various informational text labels and size the
//    // screen elements
//    @SuppressWarnings("deprecation")
//    @Override
    protected void set_labels() {
        super.set_labels();
        // Log.d("Engine_Driver","starting set_labels");

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
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            Button b = bSels[throttleIndex];
            bLabel = getApplicationContext().getResources().getString(R.string.locoPressToSelect);
            if (mainapp.consists[throttleIndex] != null) {
                if (mainapp.consists[throttleIndex].isActive()) {
                    if (!prefShowAddressInsteadOfName) {
                        bLabel = mainapp.consists[throttleIndex].toString();
                    } else {
                        bLabel = mainapp.consists[throttleIndex].formatConsistAddr();
                    }
                    throttle_count++;
                }
            }
            double textScale = 1.0;
            int bWidth = b.getWidth(); // scale text if required to fit the textView
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
            double textWidth = b.getPaint().measureText(bLabel);
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
            b.setText(bLabel);
            b.setSelected(false);
            b.setPressed(false);
        }

        if (webView!=null) {
            setImmersiveModeOn(webView);
        }

        int screenHeight = vThrotScrWrap.getHeight(); // get the height of usable area
        //Log.d("Engine_Driver","vThrotScrWrap.getHeight(), screenHeight=" + screenHeight);
        if (screenHeight == 0) {
            // throttle screen hasn't been drawn yet, so use display metrics for now
            screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
            //Log.d("Engine_Driver","vThrotScrWrap.getHeight()=0, new screenHeight=" + screenHeight);
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


        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            switch (throttleIndex) {
                default:
                case 0:
                    llSetSpds[throttleIndex] = findViewById(R.id.throttle_0_setspeed);
                    break;
                case 1:
                    llSetSpds[throttleIndex] = findViewById(R.id.throttle_1_setspeed);
                    break;
                case 2:
                    llSetSpds[throttleIndex] = findViewById(R.id.throttle_2_setspeed);
                    break;
            }
        }

        // SPDHT set height of Loco Id and Direction Button areas
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {

            LinearLayout.LayoutParams llLidp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newDlihHeight);
            llLocoIds[throttleIndex].setLayoutParams(llLidp);
            llLocoDirs[throttleIndex].setLayoutParams(llLidp);
            //
            tvSpdVals[throttleIndex].setTextSize(TypedValue.COMPLEX_UNIT_SP, newDlihFontSize);
            // SPDHT

            //set height of slider areas
            LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newHeight);
            llSetSpds[throttleIndex].setLayoutParams(llLp);

            //set margins of slider areas
            int sliderMargin = preferences.getIntPrefValue(prefs, "left_slider_margin", getApplicationContext().getResources().getString(R.string.prefSliderLeftMarginDefaultValue));

            //show speed buttons based on pref
            llSetSpds[throttleIndex].setVisibility(View.VISIBLE); //always show as a default

            sbs[throttleIndex].setVisibility(View.VISIBLE);  //always show slider if buttons not shown
            if (prefs.getBoolean("display_speed_arrows_buttons", false)) {
                bLSpds[throttleIndex].setVisibility(View.VISIBLE);
                bRSpds[throttleIndex].setVisibility(View.VISIBLE);
                bLSpds[throttleIndex].setText(speedButtonLeftText);
                bRSpds[throttleIndex].setText(speedButtonRightText);
                //if buttons enabled, hide the slider if requested
                if (prefs.getBoolean("hide_slider_preference", false)) {
                    sbs[throttleIndex].setVisibility(View.GONE);
                    bLSpds[throttleIndex].setText(speedButtonDownText);
                    bRSpds[throttleIndex].setText(speedButtonUpText);
                }
            } else {  //hide speed buttons based on pref
                bLSpds[throttleIndex].setVisibility(View.GONE);
                bRSpds[throttleIndex].setVisibility(View.GONE);
                sliderMargin += 30;  //a little extra margin previously in button
            }
            if (prefs.getBoolean("prefHideSliderAndSpeedButtons", getResources().getBoolean(R.bool.prefHideSliderAndSpeedButtonsDefaultValue))) {
                llSetSpds[throttleIndex].setVisibility(View.GONE);
            }

            sbs[throttleIndex].setPadding(sliderMargin, 0, sliderMargin, 0);

            // update the state of each function button based on shared variable
            set_all_function_states(throttleIndex);
        }
        if (screenHeight > throttleMargin) { // don't do this if height is invalid
            //Log.d("Engine_Driver","starting screen height adjustments, screenHeight=" + screenHeight);
            // determine how to split the screen (evenly if all three, 45/45/10 for two, 80/10/10 if only one)
            screenHeight -= throttleMargin;
            String numThrot = prefs.getString("NumThrottle", getResources().getString(R.string.prefNumOfThrottlesDefault));

            if (numThrot.matches("One")) {
                heights[0] = screenHeight;
                heights[1] = 0;
                heights[2] = 0;
            } else if (numThrot.matches("Two") && !mainapp.consists[1].isActive()) {
                heights[0] = (int) (screenHeight * 0.9);
                heights[1] = (int) (screenHeight * 0.10);
                heights[2] = 0;
            } else if (numThrot.matches("Two") && !mainapp.consists[0].isActive()) {
                heights[0] = (int) (screenHeight * 0.10);
                heights[1] = (int) (screenHeight * 0.9);
                heights[2] = 0;
            } else if (numThrot.matches("Two")) {
                heights[0] = (int) (screenHeight * 0.5);
                heights[1] = (int) (screenHeight * 0.5);
                heights[2] = 0;
            } else if (throttle_count == 0 || throttle_count == 3) {
                heights[0] = (int) (screenHeight * 0.33);
                heights[1] = (int) (screenHeight * 0.33);
                heights[2] = (int) (screenHeight * 0.33);
            } else if (!mainapp.consists[0].isActive() && !mainapp.consists[1].isActive()) {
                heights[0] = (int) (screenHeight * 0.10);
                heights[1] = (int) (screenHeight * 0.10);
                heights[2] = (int) (screenHeight * 0.80);
            } else if (!mainapp.consists[0].isActive() && !mainapp.consists[2].isActive()) {
                heights[0] = (int) (screenHeight * 0.10);
                heights[1] = (int) (screenHeight * 0.80);
                heights[2] = (int) (screenHeight * 0.10);
            } else if (!mainapp.consists[1].isActive() && !mainapp.consists[2].isActive()) {
                heights[0] = (int) (screenHeight * 0.80);
                heights[1] = (int) (screenHeight * 0.10);
                heights[2] = (int) (screenHeight * 0.10);
            } else if (!mainapp.consists[0].isActive()) {
                heights[0] = (int) (screenHeight * 0.10);
                heights[1] = (int) (screenHeight * 0.45);
                heights[2] = (int) (screenHeight * 0.45);
            } else if (!mainapp.consists[1].isActive()) {
                heights[0] = (int) (screenHeight * 0.45);
                heights[1] = (int) (screenHeight * 0.10);
                heights[2] = (int) (screenHeight * 0.45);
            } else {
                heights[0] = (int) (screenHeight * 0.45);
                heights[1] = (int) (screenHeight * 0.45);
                heights[2] = (int) (screenHeight * 0.10);
            }

            LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, newHeight);
            for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                // set height of each area
                llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, heights[throttleIndex]);
                llLp.bottomMargin = (int) (throttleMargin * (dm.densityDpi / 160.));
                lls[throttleIndex].setLayoutParams(llLp);

                // update throttle slider top/bottom
                tops[throttleIndex] = lls[throttleIndex].getTop() + sbs[throttleIndex].getTop() + bSels[throttleIndex].getHeight() + bFwds[throttleIndex].getHeight();
                bottoms[throttleIndex] = lls[throttleIndex].getTop() + sbs[throttleIndex].getBottom() + bSels[throttleIndex].getHeight() + bFwds[throttleIndex].getHeight();
            }
        } else {
            Log.d("Engine_Driver", "screen height adjustments skipped, screenHeight=" + screenHeight);
        }

        // update the direction indicators
        showDirectionIndications();

//        // update the state of each function button based on shared variable
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            set_all_function_states(throttleIndex);
        }


        // Log.d("Engine_Driver","ending set_labels");

    }
}