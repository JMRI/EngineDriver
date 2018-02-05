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
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import jmri.enginedriver.Consist.ConLoco;

import static android.view.InputDevice.getDevice;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_A;
import static android.view.KeyEvent.KEYCODE_D;
import static android.view.KeyEvent.KEYCODE_F;
import static android.view.KeyEvent.KEYCODE_N;
import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_T;
import static android.view.KeyEvent.KEYCODE_V;
import static android.view.KeyEvent.KEYCODE_W;
import static android.view.KeyEvent.KEYCODE_X;

public class gamepad_test extends Activity implements OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu CLEMenu;

    private GestureDetector myGesture;

    private SharedPreferences prefs;

    private String whichGamePadMode = "None";
    private static String PREF_GAMEPAD_BUTTON_OPTION_ALL_STOP = "All Stop";
    private static String PREF_GAMEPAD_BUTTON_OPTION_STOP = "Stop";
    private static String PREF_GAMEPAD_BUTTON_OPTION_NEXT_THROTTLE = "Next Throttle";
    private static String PREF_GAMEPAD_BUTTON_OPTION_FORWARD = "Forward";
    private static String PREF_GAMEPAD_BUTTON_OPTION_REVERSE = "Reverse";
    private static String PREF_GAMEPAD_BUTTON_OPTION_FORWARD_REVERSE_TOGGLE = "Forward/Reverse Toggle";
    private static String PREF_GAMEPAD_BUTTON_OPTION_INCREASE_SPEED = "Increase Speed";
    private static String PREF_GAMEPAD_BUTTON_OPTION_DECREASE_SPEED = "Decrease Speed";

    // Gamepad Button preferences
    private String[] prefGamePadButtons = {"Next Throttle","Stop", "Function 00/Light", "Function 01/Bell", "Function 02/Horn",
            "Increase Speed", "Reverse", "Decrease Speed", "Forward", "All Stop"};

    //                              none     NextThr  Speed+    Speed-      Fwd         Rev       All Stop    F2         F1          F0        Stop
    private int[] gamePadKeys =     {0,        0,   KEYCODE_W, KEYCODE_X,   KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F};
    private int[] gamePadKeys_Up =  {0,        0,   KEYCODE_W,  KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F};

    private ToneGenerator tg;

    private Button bDpadUp; // buttons
    private Button bDpadDown;
    private Button bDpadLeft;
    private Button bDpadRight;
    private Button bButtonX;
    private Button bButtonY;
    private Button bButtonA;
    private Button bButtonB;
    private TextView tvGamepadMode;
    private TextView tvGamepadKeyCode;
    private TextView tvGamepadKeyFunction;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return myGesture.onTouchEvent(event);
    }

    void GamepadFeedbackSound(boolean invalidAction) {
        if (invalidAction)
            tg.startTone(ToneGenerator.TONE_PROP_NACK);
        else
            tg.startTone(ToneGenerator.TONE_PROP_BEEP);
    }

    void GamepadFeedbackSoundStop() {
        tg.stopTone();
    }


    // setup the appropriate keycodes for the type of gamepad that has been selected in the preferences
    private void setGamepadKeys() {
        whichGamePadMode = prefs.getString("prefGamePadType", getApplicationContext().getResources().getString(R.string.prefGamePadTypeDefaultValue));

        // Gamepad button Preferences
        prefGamePadButtons[0] = prefs.getString("prefGamePadButtonStart", getApplicationContext().getResources().getString(R.string.prefGamePadButtonStartDefaultValue));
        prefGamePadButtons[9] = prefs.getString("prefGamePadButtonReturn", getApplicationContext().getResources().getString(R.string.prefGamePadButtonReturnDefaultValue));
        prefGamePadButtons[1] = prefs.getString("prefGamePadButton1", getApplicationContext().getResources().getString(R.string.prefGamePadButton1DefaultValue));
        prefGamePadButtons[2] = prefs.getString("prefGamePadButton2", getApplicationContext().getResources().getString(R.string.prefGamePadButton2DefaultValue));
        prefGamePadButtons[3] = prefs.getString("prefGamePadButton3", getApplicationContext().getResources().getString(R.string.prefGamePadButton3DefaultValue));
        prefGamePadButtons[4] = prefs.getString("prefGamePadButton4", getApplicationContext().getResources().getString(R.string.prefGamePadButton4DefaultValue));
        // Gamepad DPAD Preferences
        prefGamePadButtons[5] = prefs.getString("prefGamePadButtonUp", getApplicationContext().getResources().getString(R.string.prefGamePadButtonUpDefaultValue));
        prefGamePadButtons[6] = prefs.getString("prefGamePadButtonRight", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightDefaultValue));
        prefGamePadButtons[7] = prefs.getString("prefGamePadButtonDown", getApplicationContext().getResources().getString(R.string.prefGamePadButtonDownDefaultValue));
        prefGamePadButtons[8] = prefs.getString("prefGamePadButtonLeft", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftDefaultValue));

        if (!whichGamePadMode.equals("None")) {
            // make sure the Softkeyboard is hidden
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

        int[] bGamePadKeys;
        int[] bGamePadKeysUp;

        switch (whichGamePadMode) {
            case "iCade+DPAD":
            case "iCade+DPAD-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadiCadePlusDpad);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadiCadePlusDpad_UpCodes);
                break;
            case "MTK":
            case "MTK-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMTK);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Game":
            case "Game-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGame);
                bGamePadKeysUp = bGamePadKeys;
                break;
/*
            case "VRBoxA":
            case "VRBoxA-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadVRBoxA);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "VRBoxC":
            case "VRBoxC-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadVRBoxiC);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadVRBoxiC_UpCodes);
                break;
            case "VRBoxiC":
            case "VRBoxiC-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadVRBoxiC);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadVRBoxiC_UpCodes);
                break;
*/
            case "MagicseeR1B":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMagicseeR1B);
                bGamePadKeysUp = bGamePadKeys;
                break;
            default: // "iCade" or iCade-rotate
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadiCade);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadiCade_UpCodes);
                break;
        }
        // now grab the keycodes and put them into the arrays that will actually be used.
        for (int i = 0; i<=10; i++ ) {
            gamePadKeys[i] = bGamePadKeys[i];
            gamePadKeys_Up[i] = bGamePadKeysUp[i];
        }

        // if the preference name has "-rotate" at the end of it swap the dpad buttons around
        if (whichGamePadMode.contains("-rotate")) {
            gamePadKeys[2] = bGamePadKeys[4];
            gamePadKeys[3] = bGamePadKeys[5];
            gamePadKeys[4] = bGamePadKeys[3];
            gamePadKeys[5] = bGamePadKeys[2];

            gamePadKeys_Up[2] = bGamePadKeysUp[4];
            gamePadKeys_Up[3] = bGamePadKeysUp[5];
            gamePadKeys_Up[4] = bGamePadKeysUp[3];
            gamePadKeys_Up[5] = bGamePadKeysUp[2];
        }
    }

    private void setButtonOn(Button btn, String fn, int keyCode) {
        btn.setClickable(true);
        btn.setSelected(true);

        tvGamepadKeyCode.setText(String.valueOf(keyCode));
        tvGamepadKeyFunction.setText(fn);

    }

    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        //Log.d("Engine_Driver", "dgme " + event.getAction());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            if (!whichGamePadMode.equals("None")) { // respond to the gamepad and keyboard inputs only if the preference is set
                int action;

                float xAxis = 0;
                xAxis = event.getAxisValue(MotionEvent.AXIS_X);
                float yAxis = event.getAxisValue(MotionEvent.AXIS_Y);

                if ((xAxis!=0) || (yAxis!=0)) {
                    action = ACTION_DOWN;
                } else {
                    action = ACTION_UP;
                }

                if (action == ACTION_UP) {
                    GamepadFeedbackSoundStop();
                }

                if (yAxis == -1) { // DPAD Up Button
                    setButtonOn(bDpadUp, prefGamePadButtons[5],0);
                    return (true); // stop processing this key

                } else if (yAxis == 1) { // DPAD Down Button
                    setButtonOn(bDpadDown, prefGamePadButtons[7],0);
                    return (true); // stop processing this key

                } else if (xAxis == -1) { // DPAD Left Button
                    setButtonOn(bDpadLeft, prefGamePadButtons[8],0);
                    return (true); // stop processing this key

                } else if (xAxis == 1) { // DPAD Right Button
                    setButtonOn(bDpadRight, prefGamePadButtons[6],0);
                    return (true); // stop processing this key
                }
            }
        }
        return super.dispatchGenericMotionEvent(event);
    }

    // listener for physical keyboard events
    // used to support the gamepad only   DPAD and key events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        boolean isExternal = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            InputDevice idev = getDevice(event.getDeviceId());
            if( idev.toString().contains("Location: external")) isExternal = true;
        }

        if (isExternal) { // if has come from the phone itself, don't try to process it here
            if (!whichGamePadMode.equals("None")) { // respond to the gamepad and keyboard inputs only if the preference is set

                int action = event.getAction();
                int keyCode = event.getKeyCode();
                int repeatCnt = event.getRepeatCount();

                if (keyCode != 0) {
                    Log.d("Engine_Driver", "keycode " + keyCode + " action " + action + " repeat " + repeatCnt);
                }

                if (action == ACTION_UP) {
                    GamepadFeedbackSoundStop();
                }

                if (keyCode == gamePadKeys[2]) { // DPAD Up Button
                    setButtonOn(bDpadUp, prefGamePadButtons[5], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys[3]) { // DPAD Down Button
                    setButtonOn(bDpadDown, prefGamePadButtons[7], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys[4]) { // DPAD Left Button
                    setButtonOn(bDpadLeft, prefGamePadButtons[8], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys[5]) { // DPAD Right Button
                    setButtonOn(bDpadRight, prefGamePadButtons[6], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys[7]) { // ios button
                    setButtonOn(bButtonX, prefGamePadButtons[1], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys_Up[8]) { // X button
                    setButtonOn(bButtonY, prefGamePadButtons[3], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys_Up[9]) { // Triangle button
                    setButtonOn(bButtonA, prefGamePadButtons[2], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys_Up[10]) { // @ button
                    setButtonOn(bButtonB, prefGamePadButtons[4], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys[6]) { // start button
                    //setButtonOn(bButtonB, prefGamePadButtons[0], keyCode);
                    return (true); // stop processing this key

                } else if (keyCode == gamePadKeys[1]) { // Return button
                    //setButtonOn(bButtonB, prefGamePadButtons[9], keyCode);
                    return (true); // stop processing this key
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }


    /**
     * Called when the activity is first created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        mainapp.applyTheme(this);

        setContentView(R.layout.gamepad_test);
        //put pointer to this activity's handler in main app's shared variable
        myGesture = new GestureDetector(this);

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        // tone generator for feedback sounds
        tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                preferences.getIntPrefValue(prefs,"prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));

        setGamepadKeys();

        // set listener for select loco buttons
        bDpadUp = (Button) findViewById(R.id.gamepad_test_dpad_up);
        bDpadUp.setClickable(false);

        bDpadDown = (Button) findViewById(R.id.gamepad_test_dpad_down);
        bDpadDown.setClickable(false);

        bDpadLeft = (Button) findViewById(R.id.gamepad_test_dpad_left);
        bDpadLeft.setClickable(false);

        bDpadRight = (Button) findViewById(R.id.gamepad_test_dpad_right);
        bDpadRight.setClickable(false);

        bButtonX = (Button) findViewById(R.id.gamepad_test_button_x);
        bButtonX.setClickable(false);

        bButtonY = (Button) findViewById(R.id.gamepad_test_button_y);
        bButtonY.setClickable(false);

        bButtonA = (Button) findViewById(R.id.gamepad_test_button_a);
        bButtonA.setClickable(false);

        bButtonB = (Button) findViewById(R.id.gamepad_test_button_b);
        bButtonB.setClickable(false);

        tvGamepadMode =(TextView) findViewById(R.id.gamepad_test_mode);

        tvGamepadKeyCode =(TextView) findViewById(R.id.gamepad_test_keycode);
        tvGamepadKeyFunction =(TextView) findViewById(R.id.gamepad_test_keyfunction);

        tvGamepadMode.setText(whichGamePadMode);

//        tvGamepadMode.setText("");
        tvGamepadKeyCode.setText("");
        tvGamepadKeyFunction.setText("");


    }

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
       mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        if (CLEMenu != null) {
            mainapp.displayEStop(CLEMenu);
        }
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.isFinishing()) {       //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "ConsistLightsEdit.onDestroy()");

        //mainapp.consist_lights_edit_msg_handler = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gamepad_test, menu);
        CLEMenu = menu;
        //mainapp.displayEStop(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Always go to throttle if back button pressed
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            Intent resultIntent = new Intent();
//            resultIntent.putExtra("whichThrottle", whichThrottle);  //pass whichThrottle as an extra
//            setResult(result, resultIntent);
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    private void disconnect() {
        this.finish();
    }
}