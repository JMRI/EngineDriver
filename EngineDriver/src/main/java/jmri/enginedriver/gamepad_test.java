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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.tts_msg_type;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.util.Tts;
import jmri.enginedriver.type.gamepad_test_type;

public class gamepad_test extends AppCompatActivity implements OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu GPTMenu;

//    private GestureDetector myGesture;

    private int result;

    private SharedPreferences prefs;

    private String whichGamepadNo = " "; //text version of the arr index of the gamepad we are testing.  Sent in and out

    private String prefGamePadType = "None";
    private int prefGamePadTypeIndex = 0;
    private int oldPrefGamePadTypeIndex = 0;
    String[] gamePadModeEntryValuesArray;
    String[] gamePadModeEntriesArray; // display version
    String[] gamePadButtonLabelsArray;

    private boolean prefGamepadTestEnforceTestingSimple = true;
    private boolean prefTtsGamepadTestKeys = false;

    private final boolean[] gamepadButtonsChecked = {false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false};

    // Gamepad Button preferences
    private final String[] prefGamePadButtons = {"Next Throttle","Stop", "Function 00/Light", "Function 01/Bell", "Function 02/Horn",
            "Increase Speed", "Reverse", "Decrease Speed", "Forward", "All Stop","Select", "Left Shoulder","Right Shoulder","Left Trigger","Right Trigger","Left Thumb","Right Thumb","","","","","",""};

    //                               0         1    2           3          4          5          6          7          8          9          10        11 12 13 14 15 16 17 18 19 20
    //                              none     NextThr  Speed+    Speed-     Fwd        Rev        All Stop   F2         F1         F0         Stop
    private final int[] gamePadKeys =     {0,        0,   KEYCODE_W,  KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F,0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final int[] gamePadKeys_Up =  {0,        0,   KEYCODE_W,  KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F,0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] BUTTON_ACTION_NUMBERS ={
                                     -1,       9,   5,          7,         8,         6,         0,         1,         3,         2,         4,        10,11,12,13,14,15,-1,-1,-1,-1};
    private static final int GAMEPAD_KEYS_LENGTH = 21;

    private ToneGenerator tg;

    private Button[] bButtons;

//    private TextView tvGamepadMode;
    private TextView tvGamepadKeyCode;
    private TextView tvGamepadKeyFunction;
    private TextView tvGamepadComplete;
    private TextView tvGamepadAllKeyCodes;

    private String allKeyCodes = "";

//    private static final String GAMEPAD_TEST_PASS = "1";
//    private static final String GAMEPAD_TEST_FAIL = "2";
//    private static final String GAMEPAD_TEST_SKIPPED = "3";
//    private static final String GAMEPAD_TEST_RESET = "9";

    private int decreaseButtonCount = 0;

    Tts tts;

    /** @noinspection FieldCanBeLocal*/
    private LinearLayout screenNameLine;
    /** @noinspection FieldCanBeLocal*/
    private Toolbar toolbar;
    /** @noinspection FieldCanBeLocal*/
    private LinearLayout statusLine;

    /** @noinspection SameParameterValue*/
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
        prefGamePadType = prefs.getString("prefGamePadType", getApplicationContext().getResources().getString(R.string.prefGamePadTypeDefaultValue));

        gamePadModeEntryValuesArray = this.getResources().getStringArray(R.array.prefGamePadTypeEntryValues);
        final List<String> gamePadEntryValuesList = new ArrayList<>(Arrays.asList(gamePadModeEntryValuesArray));

        // display version
        gamePadModeEntriesArray = this.getResources().getStringArray(R.array.prefGamePadTypeEntries);
        final List<String> gamePadModeEntriesList = new ArrayList<>(Arrays.asList(gamePadModeEntriesArray));

        prefGamePadTypeIndex = Arrays.asList(gamePadModeEntryValuesArray).indexOf(prefGamePadType);
        if (prefGamePadTypeIndex<0) prefGamePadTypeIndex=0;

        Spinner spinner = findViewById(R.id.gamepad_test_mode);
        spinner.setSelection(prefGamePadTypeIndex);

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

        //extra buttons
        prefGamePadButtons[10] = prefs.getString("prefGamePadButtonLeftShoulder", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftTriggerDefaultValue));
        prefGamePadButtons[11] = prefs.getString("prefGamePadButtonRightShoulder", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightShoulderDefaultValue));
        prefGamePadButtons[12] = prefs.getString("prefGamePadButtonLeftTrigger", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftTriggerDefaultValue));
        prefGamePadButtons[13] = prefs.getString("prefGamePadButtonRightTrigger", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightTriggerDefaultValue));
        prefGamePadButtons[14] = prefs.getString("prefGamePadButtonLeftThumb", getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftThumbDefaultValue));
        prefGamePadButtons[15] = prefs.getString("prefGamePadButtonRightThumb", getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightThumbDefaultValue));


        // make sure the Softkeyboard is hidden
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        int[] bGamePadKeys;
        int[] bGamePadKeysUp;

        gamePadButtonLabelsArray = this.getResources().getStringArray(R.array.gamepadTestButtonLabelsDefault);
        switch (prefGamePadType) {
            case "iCade+DPAD":
            case "iCade+DPAD-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadiCadePlusDpad);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadiCadePlusDpad_UpCodes);
                break;
            case "MTK":
            case "MTK-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMTK);
                bGamePadKeysUp = bGamePadKeys;
                gamePadButtonLabelsArray = this.getResources().getStringArray(R.array.gamepadTestButtonLabelsMTK);
                break;
            case "Game":
            case "Game-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGame);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Game-alternate-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGameAlternate);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "MagicseeR1B":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMagicseeR1B);
                bGamePadKeysUp = bGamePadKeys;
                gamePadButtonLabelsArray = this.getResources().getStringArray(R.array.gamepadTestButtonLabelsMagicR1);
                break;
            case "MagicseeR1A":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMagicseeR1A);
                bGamePadKeysUp = bGamePadKeys;
                gamePadButtonLabelsArray = this.getResources().getStringArray(R.array.gamepadTestButtonLabelsMagicR1);
                break;
            case "MagicseeR1C":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadMagicseeR1C);
                bGamePadKeysUp = bGamePadKeys;
                gamePadButtonLabelsArray = this.getResources().getStringArray(R.array.gamepadTestButtonLabelsMagicR1);
                break;
            case "FlydigiWee2":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadFlydigiWee2);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "UtopiaC":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadUtopiaC);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "AuvisioB":
            case "AuvisioB-rotate":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadAuvisioB);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Generic":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGeneric);
                bGamePadKeysUp = bGamePadKeys;
                gamePadButtonLabelsArray = this.getResources().getStringArray(R.array.gamepadTestButtonLabelsGeneric);
                break;
            case "Generic3x4":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadGeneric3x4);
                bGamePadKeysUp = bGamePadKeys;
                gamePadButtonLabelsArray = this.getResources().getStringArray(R.array.gamepadTestButtonLabelsGeneric3x4);
                break;
            case "Keyboard":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadNoneLabels);
                bGamePadKeysUp = bGamePadKeys;
                break;
            case "Volume":
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadVolume);
                bGamePadKeysUp = bGamePadKeys;
                gamePadButtonLabelsArray = this.getResources().getStringArray(R.array.gamepadTestButtonLabelsVolume);
                break;
            default: // "iCade" or iCade-rotate
                bGamePadKeys = this.getResources().getIntArray(R.array.prefGamePadiCade);
                bGamePadKeysUp = this.getResources().getIntArray(R.array.prefGamePadiCade_UpCodes);
                break;
        }

        for (int i=1;i<=16;i++) {
            bButtons[i].setText(gamePadButtonLabelsArray[i]);  // set the button labels
            bButtons[i].setSelected(false);
            setButtonOff(bButtons[i]);
        }


        // now grab the keycodes and put them into the arrays that will actually be used.
        for (int i = 0; i<GAMEPAD_KEYS_LENGTH; i++ ) {
            gamePadKeys[i] = bGamePadKeys[i];
            gamePadKeys_Up[i] = bGamePadKeysUp[i];
        }

        // if the preference name has "-rotate" at the end of it swap the dpad buttons around
        if (prefGamePadType.contains("-rotate")) {
            gamePadKeys[2] = bGamePadKeys[4];
            gamePadKeys[3] = bGamePadKeys[5];
            gamePadKeys[4] = bGamePadKeys[3];
            gamePadKeys[5] = bGamePadKeys[2];

            gamePadKeys_Up[2] = bGamePadKeysUp[4];
            gamePadKeys_Up[3] = bGamePadKeysUp[5];
            gamePadKeys_Up[4] = bGamePadKeysUp[3];
            gamePadKeys_Up[5] = bGamePadKeysUp[2];
        }

        LinearLayout dpad = findViewById(R.id.gamepad_dpad);
        TableLayout btns = findViewById(R.id.gamepad_buttons);
        RelativeLayout optn = findViewById(R.id.gamepad_test_optional_group);
        TableLayout extra = findViewById(R.id.gamepad_buttons_extra);
        RelativeLayout status = findViewById(R.id.gamepad_test_complete_group);
        View helpText = findViewById(R.id.gamepad_test_help);
        View helpTextKeyboard = findViewById(R.id.gamepad_test_keyboard_help);
        if (!prefGamePadType.equals("Keyboard")) {
            dpad.setVisibility(LinearLayout.VISIBLE);
            btns.setVisibility(TableLayout.VISIBLE);
            optn.setVisibility(RelativeLayout.VISIBLE);
            extra.setVisibility(TableLayout.VISIBLE);
            status.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.VISIBLE);
            helpTextKeyboard.setVisibility(View.GONE);
        } else {
            dpad.setVisibility(LinearLayout.GONE);
            btns.setVisibility(TableLayout.GONE);
            optn.setVisibility(RelativeLayout.GONE);
            extra.setVisibility(TableLayout.GONE);
            status.setVisibility(TableLayout.GONE);
            helpText.setVisibility(View.GONE);
            helpTextKeyboard.setVisibility(View.VISIBLE);
        }
    }

    /** @noinspection UnusedReturnValue*/
    private boolean isTestComplete(int keyIndex) {
        gamepadButtonsChecked[keyIndex] = true;

        boolean testComplete = true;
        for (int i = 1; i<=8; i++ ) {
            if (!gamepadButtonsChecked[i]) {
                testComplete = false;
                break;
            }
        }

        if (testComplete) {
            tvGamepadComplete.setText(R.string.gamepadTestComplete);
            if (!whichGamepadNo.equals(" ")) {
//                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.gamepadTestCompleteToast), Toast.LENGTH_SHORT).show();
                threaded_application.safeToast(R.string.gamepadTestCompleteToast, Toast.LENGTH_SHORT);
                if (result != RESULT_OK) {
                    result = RESULT_OK;
                    endThisActivity(gamepad_test_type.PASS);
                }
            }
        }
        return testComplete;
    }

    private void setButtonOff(Button btn) {
        btn.setClickable(false);
        btn.setSelected(false);
        btn.setTypeface(null, Typeface.NORMAL);
    }

    private void setAllButtonsOff(){
        for (int i=1;i<=16;i++) {
            setButtonOff(bButtons[i]);
        }
    }


    private void setButtonOn(Button btn, String fn, String keyCodeString, int action) {

        if (whichGamepadNo.equals(" ")) {
            setAllButtonsOff();
        }

        if (action==ACTION_DOWN)  {
            String text = keyCodeString;
            if ( (keyCodeString.length()<4) || (!keyCodeString.startsWith("DPad")) ) {
                text = "Button "+ btn.getText();
            }
            tts.speakWords(tts_msg_type.GAMEPAD_GAMEPAD_TEST_KEY_AND_PURPOSE, text+","+fn);
        }

        btn.setClickable(true);
        btn.setSelected(true);
        btn.setTypeface(null, Typeface.ITALIC);


        tvGamepadKeyCode.setText(String.valueOf(keyCodeString));
        tvGamepadKeyFunction.setText(fn);
        if (fn.equals("Decrease Speed")) { // button is set to decrease speed
            decreaseButtonCount++;
            if ((decreaseButtonCount>=3) || (prefGamepadTestEnforceTestingSimple)) {
                result = RESULT_OK;
                endThisActivity(gamepad_test_type.SKIPPED);
            }
        } else {
            decreaseButtonCount = 0;
        }
    }

    private void invalidKeyCode(int keyCode) {
        tvGamepadKeyCode.setText(String.valueOf(keyCode));
        tvGamepadKeyFunction.setText(R.string.gamepadTestInvalidKeycode);
    }

    private void setAllKeyCodes(String sKeyCode, int action) {

        if(action == ACTION_DOWN) {
            allKeyCodes = allKeyCodes + this.getResources().getString(R.string.gamepadTestKeyCodesDownAction);
        } else {
            allKeyCodes = allKeyCodes + this.getResources().getString(R.string.gamepadTestKeyCodesUpAction);
        }
        allKeyCodes = allKeyCodes +  sKeyCode + " ";
        if( allKeyCodes.length()>45) {
            allKeyCodes = allKeyCodes.substring( allKeyCodes.length()-45);
        }
        tvGamepadAllKeyCodes.setText(allKeyCodes);
    }

    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        //Log.d("Engine_Driver", "dgme " + event.getAction());
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            if ((!prefGamePadType.equals("None")) && (!mainapp.prefGamePadIgnoreJoystick)) { // respond to the gamepad and keyboard inputs only if the preference is set
                int action;

                float xAxis;
                xAxis = event.getAxisValue(MotionEvent.AXIS_X);
                float yAxis = event.getAxisValue(MotionEvent.AXIS_Y);
                float xAxis2 = event.getAxisValue(MotionEvent.AXIS_Z);
                float yAxis2 = event.getAxisValue(MotionEvent.AXIS_RZ);

                if ((xAxis!=0) || (yAxis!=0)) {
                    action = ACTION_DOWN;
                } else {
                    action = ACTION_UP;
                }

                if (action == ACTION_UP) {
                    GamepadFeedbackSoundStop();
                }

                if (yAxis == -1) { // DPAD Up Button
                    setButtonOn(bButtons[2], prefGamePadButtons[5],"DPad Up", action);
                    setAllKeyCodes( this.getResources().getString(R.string.gamepadTestKeyCodesUpCode), action);
                    GamepadFeedbackSound(false);
                    isTestComplete(5);
                    return (true); // stop processing this key

                } else if (yAxis == 1) { // DPAD Down Button
                    setButtonOn(bButtons[3], prefGamePadButtons[7],"DPad Down", action);
                    setAllKeyCodes( this.getResources().getString(R.string.gamepadTestKeyCodesDownCode), action);
                    GamepadFeedbackSound(false);
                    isTestComplete(7);
                    return (true); // stop processing this key

                } else if (xAxis == -1) { // DPAD Left Button
                    setButtonOn(bButtons[4], prefGamePadButtons[8],"DPad Left", action);
                    setAllKeyCodes( this.getResources().getString(R.string.gamepadTestKeyCodesLeftCode), action);
                    GamepadFeedbackSound(false);
                    isTestComplete(8);
                    return (true); // stop processing this key

                } else if (xAxis == 1) { // DPAD Right Button
                    setButtonOn(bButtons[5], prefGamePadButtons[6],"DPad Right", action);
                    setAllKeyCodes( this.getResources().getString(R.string.gamepadTestKeyCodesRightCode), action);
                    GamepadFeedbackSound(false);
                    isTestComplete(6);
                    return (true); // stop processing this key
                }

                if (yAxis2 == -1) { // DPAD2 Up Button
                    setButtonOn(bButtons[2], prefGamePadButtons[5],"DPad2 Up", action);
                    setAllKeyCodes( this.getResources().getString(R.string.gamepadTestKeyCodesUpCode), action);
                    GamepadFeedbackSound(false);
                    isTestComplete(5);
                    return (true); // stop processing this key

                } else if (yAxis2 == 1) { // DPAD2 Down Button
                    setButtonOn(bButtons[3], prefGamePadButtons[7],"DPad2 Down", action);
                    setAllKeyCodes( this.getResources().getString(R.string.gamepadTestKeyCodesDownCode), action);
                    GamepadFeedbackSound(false);
                    isTestComplete(7);
                    return (true); // stop processing this key

                } else if (xAxis2 == -1) { // DPAD2 Left Button
                    setButtonOn(bButtons[4], prefGamePadButtons[8],"DPad2 Left", action);
                    setAllKeyCodes( this.getResources().getString(R.string.gamepadTestKeyCodesLeftCode), action);
                    GamepadFeedbackSound(false);
                    isTestComplete(8);
                    return (true); // stop processing this key

                } else if (xAxis2 == 1) { // DPAD2 Right Button
                    setButtonOn(bButtons[5], prefGamePadButtons[6],"DPad2 Right", action);
                    setAllKeyCodes( this.getResources().getString(R.string.gamepadTestKeyCodesRightCode), action);
                    GamepadFeedbackSound(false);
                    isTestComplete(6);
                    return (true); // stop processing this key
                }
            }
//        }

        return super.dispatchGenericMotionEvent(event);
    }

    // listener for physical keyboard events
    // used to support the gamepad only   DPAD and key events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        boolean isExternal = false;
        InputDevice idev = getDevice(event.getDeviceId());
        if( idev.toString().contains("Location: external")) isExternal = true;

        if (isExternal) { // if has come from the phone itself, don't try to process it here
            if (!prefGamePadType.equals("None")) { // respond to the gamepad and keyboard inputs only if the preference is set

                int action = event.getAction();
                int keyCode = event.getKeyCode();
                int repeatCnt = event.getRepeatCount();

                if (keyCode != 0) {
                    Log.d("Engine_Driver", "keycode " + keyCode + " action " + action + " repeat " + repeatCnt);

                    setAllKeyCodes( String.valueOf(keyCode), action);
                }

                int rslt = -1;
                int actionNo = -1;
                for (int i=1; i<=16; i++){
                    if (keyCode == gamePadKeys[i]) {
                        rslt = i;
                        actionNo = BUTTON_ACTION_NUMBERS[i];
                        break;
                    }
                }
                if (rslt>=1) {
                    setButtonOn(bButtons[rslt], prefGamePadButtons[actionNo], String.valueOf(keyCode), action);
                    if (action == ACTION_DOWN) {
                        GamepadFeedbackSound(false);
                    } else {
                        isTestComplete(actionNo);
                        GamepadFeedbackSoundStop();
                    }
                }
                return (true); // ignore any other keycodes
            }
        }

        return super.dispatchKeyEvent(event);
    }

    public class spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            Spinner spinner = findViewById(R.id.gamepad_test_mode);
            prefGamePadTypeIndex = spinner.getSelectedItemPosition();

            prefs.edit().putString("prefGamePadType", gamePadModeEntryValuesArray[prefGamePadTypeIndex]).commit();  //reset the preference

            if (oldPrefGamePadTypeIndex != prefGamePadTypeIndex) {
                setGamepadKeys();
                oldPrefGamePadTypeIndex = prefGamePadTypeIndex;
            }
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class cancel_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            endThisActivity(gamepad_test_type.FAIL);
            mainapp.buttonVibration();
        }
    }

    private class reset_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            endThisActivity(gamepad_test_type.RESET);
            mainapp.buttonVibration();
        }
    }
    private class skip_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            endThisActivity(gamepad_test_type.SKIPPED);
            mainapp.buttonVibration();
        }
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("CutPasteId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        result = RESULT_CANCELED;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            whichGamepadNo = extras.getString("whichGamepadNo");
        }

        mainapp.applyTheme(this);

        setContentView(R.layout.gamepad_test);
        //put pointer to this activity's handler in main app's shared variable
//        myGesture = new GestureDetector(this);

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        prefGamepadTestEnforceTestingSimple = prefs.getBoolean("prefGamepadTestEnforceTestingSimple", getResources().getBoolean(R.bool.prefGamepadTestEnforceTestingSimpleDefaultValue));
        mainapp.prefGamePadIgnoreJoystick = prefs.getBoolean("prefGamePadIgnoreJoystick", getResources().getBoolean(R.bool.prefGamePadIgnoreJoystickDefaultValue));

        // tone generator for feedback sounds
        tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,
                threaded_application.getIntPrefValue(prefs,"prefGamePadFeedbackVolume", getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue)));

        bButtons = new Button[17];
        bButtons[1] = findViewById(R.id.gamepad_test_button_start);
        bButtons[2] = findViewById(R.id.gamepad_test_dpad_up);
        bButtons[3] = findViewById(R.id.gamepad_test_dpad_down);
        bButtons[4] = findViewById(R.id.gamepad_test_dpad_left);
        bButtons[5] = findViewById(R.id.gamepad_test_dpad_right);
        bButtons[6] = findViewById(R.id.gamepad_test_button_enter);
        bButtons[7] = findViewById(R.id.gamepad_test_button_x);
        bButtons[8] = findViewById(R.id.gamepad_test_button_y);
        bButtons[9] = findViewById(R.id.gamepad_test_button_a);
        bButtons[10] = findViewById(R.id.gamepad_test_button_b);
        bButtons[11] = findViewById(R.id.gamepad_test_button_left_shoulder);
        bButtons[12] = findViewById(R.id.gamepad_test_button_right_shoulder);
        bButtons[13] = findViewById(R.id.gamepad_test_button_left_trigger);
        bButtons[14] = findViewById(R.id.gamepad_test_button_right_trigger);
        bButtons[15] = findViewById(R.id.gamepad_test_button_left_thumb);
        bButtons[16] = findViewById(R.id.gamepad_test_button_right_thumb);
        for (int i=1;i<=16;i++) {
            bButtons[i].setClickable(false);
        }

        //tvGamepadMode =(TextView) findViewById(R.id.gamepad_test_mode);

        tvGamepadAllKeyCodes = findViewById(R.id.gamepad_test_all_keycodes);

        tvGamepadKeyCode = findViewById(R.id.gamepad_test_keycode);
        tvGamepadKeyFunction = findViewById(R.id.gamepad_test_keyfunction);
        tvGamepadComplete = findViewById(R.id.gamepad_test_complete);

        tvGamepadKeyCode.setText("");
        tvGamepadKeyFunction.setText("");
        tvGamepadComplete.setText(R.string.gamepadTestIncomplete);

        // Set the options for the mode.
        Spinner mode_spinner = findViewById(R.id.gamepad_test_mode);
        //ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.prefGamePadTypeOptions, android.R.layout.simple_spinner_item);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.prefGamePadTypeEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode_spinner.setAdapter(spinner_adapter);
        mode_spinner.setOnItemSelectedListener(new spinner_listener());

        setGamepadKeys();

        //Set the button
        Button cancelButton = findViewById(R.id.gamepad_test_button_cancel);
        gamepad_test.cancel_button_listener cancel_click_listener = new gamepad_test.cancel_button_listener();
        cancelButton.setOnClickListener(cancel_click_listener);

        Button resetButton = findViewById(R.id.gamepad_test_button_reset);
        gamepad_test.reset_button_listener reset_click_listener = new gamepad_test.reset_button_listener();
        resetButton.setOnClickListener(reset_click_listener);

        Button skipButton = findViewById(R.id.gamepad_test_button_skip);
        if (whichGamepadNo.equals(" ")) {
            resetButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            cancelButton.setText(R.string.gamepadTestCancelNonForced);
            TextView tvHelpText = findViewById(R.id.gamepad_test_help);
            tvHelpText.setText(R.string.gamepadTestHelpNonForced);
        } else {
            gamepad_test.skip_button_listener skip_click_listener = new gamepad_test.skip_button_listener();
            skipButton.setOnClickListener(skip_click_listener);
        }

        //put pointer to this activity's handler in main app's shared variable
        mainapp.gamepad_test_msg_handler = new gamepad_test_handler(Looper.getMainLooper());

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = (LinearLayout) findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tts = new Tts(prefs,mainapp);

    } // end onCreate

    @Override
    public void onResume() {
        super.onResume();
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        if (GPTMenu != null) {
            mainapp.displayEStop(GPTMenu);
            mainapp.displayFlashlightMenuButton(GPTMenu);
            mainapp.setFlashlightButton(GPTMenu);
            mainapp.displayPowerStateMenuButton(GPTMenu);
            mainapp.setPowerStateButton(GPTMenu);
        }
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        tts.loadPrefs(); // recheck the ttf prefs in case they have been changed
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "gamepad_test.onDestroy() called");

        if (tg != null) {
            tg.release();
        }

        //mainapp.consist_lights_edit_msg_handler = null;
        super.onDestroy();
    }

    // end current activity
    void endThisActivity(int passedTest) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("whichGamepadNo", whichGamepadNo + passedTest);  //pass whichGamepadNo as an extra - pass/fail/reset
        setResult(result, resultIntent);
        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gamepad_test, menu);
        GPTMenu = menu;
        mainapp.displayEStop(menu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);

        return  super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.EmerStop) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlight(this, GPTMenu);
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(GPTMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("HandlerLeak")
    class gamepad_test_handler extends Handler {

        public gamepad_test_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (msg.what) {
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateButton(GPTMenu);
                        }
                    }
                    break;
                }

            }
        }
    }

    //Always go to throttle if back button pressed
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        if (key == KeyEvent.KEYCODE_BACK) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("whichGamepadNo", whichGamepadNo+"2");  //pass whichGamepadNo as an extra - plus "2" for fail
            setResult(result, resultIntent);
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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}