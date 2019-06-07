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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.GestureOverlayView;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jmri.enginedriver.logviewer.ui.LogViewerActivity;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_A;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_D;
import static android.view.KeyEvent.KEYCODE_F;
import static android.view.KeyEvent.KEYCODE_N;
import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_T;
import static android.view.KeyEvent.KEYCODE_V;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static android.view.KeyEvent.KEYCODE_W;
import static android.view.KeyEvent.KEYCODE_X;

// for changing the screen brightness

// used for supporting Keyboard and Gamepad input;

public class throttle_simple extends throttle {

    protected static final int MAX_SCREEN_THROTTLES = 6;

    private LinearLayout[] lThrottles;
    private LinearLayout[] Separators;

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

        super.layoutViewId = R.layout.throttle_simple;
        super.onCreate(savedInstanceState);

        lThrottles = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        Separators = new LinearLayout[mainapp.maxThrottlesCurrentScreen];
        vsbSpeeds = new VerticalSeekBar[mainapp.maxThrottlesCurrentScreen];
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++)
            switch (throttleIndex) {
                default:
                case 0:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_0);
                    Separators[throttleIndex] = findViewById(R.id.separator0);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_0);
                    break;
                case 1:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_1);
                    Separators[throttleIndex] = findViewById(R.id.separator1);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_1);
                    break;
                case 2:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_2);
                    Separators[throttleIndex] = findViewById(R.id.separator2);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_2);
                    break;
                case 3:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_3);
                    Separators[throttleIndex] = findViewById(R.id.separator3);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_3);
                    break;
                case 4:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_4);
                    Separators[throttleIndex] = findViewById(R.id.separator4);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_4);
                    break;
                case 5:
                    lThrottles[throttleIndex] = findViewById(R.id.throttle_5);
                    Separators[throttleIndex] = findViewById(R.id.separator5);
                    vsbSpeeds[throttleIndex] = findViewById(R.id.speed_5);
                    break;
            }

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
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            Button b = bSels[throttleIndex];
            if (mainapp.consists[throttleIndex].isActive()) {
                if (!prefShowAddressInsteadOfName) {
                    bLabel = mainapp.consists[throttleIndex].toString();
                } else {
                    bLabel = mainapp.consists[throttleIndex].formatConsistAddr();
                }
            } else {
                bLabel = getApplicationContext().getResources().getString(R.string.locoPressToSelect);
                // whichVolume = 'S'; //set the next throttle to use volume control
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
        }

//        // update the state of each function button based on shared variable
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            set_all_function_states(throttleIndex);
        }

        // Log.d("Engine_Driver","ending set_labels");

    }

}