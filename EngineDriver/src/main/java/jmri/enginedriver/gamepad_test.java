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
import static android.view.KeyEvent.KEYCODE_BACK;
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
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import android.view.ViewGroup;
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

import java.util.Arrays;
import java.util.Objects;

import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.beep_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.toolbar_button_size_to_use_type;
import jmri.enginedriver.type.tts_msg_type;
import jmri.enginedriver.util.GamePadKeyLoader;
import jmri.enginedriver.util.LocaleHelper;
import jmri.enginedriver.util.Tts;
import jmri.enginedriver.type.gamepad_test_type;

public class gamepad_test extends AppCompatActivity implements OnGestureListener {
    static final String activityName = "gamepad_test";

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu GPTMenu;

//    private GestureDetector myGesture;

    private int result;

    private GamePadKeyLoader gamePadKeyLoader;

    private SharedPreferences prefs;

    private String whichGamepadNo = " "; //text version of the arr index of the gamepad we are testing.  Sent in and out

    private String prefGamePadType = "None";
    private int prefGamePadTypeIndex = 0;
    private int oldPrefGamePadTypeIndex = 0;
    String[] gamePadModeEntryValuesArray;
    String[] gamePadModeEntriesArray; // display version
    String[] gamePadButtonLabelsArray;

    private boolean prefGamepadTestEnforceTestingSimple = true;
//    private boolean prefTtsGamepadTestKeys = false;

    private final boolean[] gamepadButtonsChecked = {false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false};

    // Gamepad Button preferences
    private final String[] prefGamePadButtons = {"Next Throttle","Stop", "Function 00/Light", "Function 01/Bell", "Function 02/Horn",
            "Increase Speed", "Reverse", "Decrease Speed", "Forward", "All Stop","Select", "Left Shoulder","Right Shoulder","Left Trigger","Right Trigger","Left Thumb","Right Thumb","","","","","",""};

    //                               0         1    2           3          4          5          6          7          8          9          10              11 12 13 14 15 16 17 18 19 20
    //                              none     NextThr  Speed+    Speed-     Fwd        Rev        All Stop   F2         F1         F0         Stop
    private int[] gamePadKeys =     {0,        0,   KEYCODE_W,  KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int[] gamePadKeys_Up =  {0,        0,   KEYCODE_W,  KEYCODE_X, KEYCODE_A, KEYCODE_D, KEYCODE_V, KEYCODE_T, KEYCODE_N, KEYCODE_R, KEYCODE_F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
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
        // the tone generator is not compatible with the ipls
        if ((!mainapp.prefDeviceSounds[0].equals("none")) || (!mainapp.prefDeviceSounds[1].equals("none"))) {
            if (invalidAction) {
                gamepadBeep(beep_type.NAK);
            } else {
                gamepadBeep(beep_type.ACK);
            }
        } else {

            if (tg != null) {
                if (invalidAction)
                    tg.startTone(ToneGenerator.TONE_PROP_NACK);
                else
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
            }
        }
    }

    void gamepadBeep(int whichBeep) {
        if (mainapp.prefGamePadFeedbackVolume == 0) return;

        if (mainapp._mediaPlayer != null) {
            mainapp._mediaPlayer.reset();     // reset stops and release on any state of the player
        }

        mainapp._mediaPlayer = MediaPlayer.create(this, mainapp.gamepadBeepIds[whichBeep]);

        if (mainapp._mediaPlayer!=null) {
            float volumeFloat = ((float) mainapp.prefGamePadFeedbackVolume) / 100.0f;

            mainapp._mediaPlayer.setVolume(volumeFloat, volumeFloat);
            mainapp._mediaPlayer.start();
        }
    }

    void GamepadFeedbackSoundStop() {
        // the tone generator is not compatible with the ipls
        if ((!mainapp.prefDeviceSounds[0].equals("none")) || (!mainapp.prefDeviceSounds[1].equals("none")))
            return;

        tg.stopTone();
    }

    private void setGamepadKeys() {
        gamePadKeyLoader.loadGamepadKeys();
        gamePadKeys = gamePadKeyLoader.getGamePadKeys();
        gamePadKeys_Up = gamePadKeyLoader.getGamePadKeys_Up();
        gamePadButtonLabelsArray = gamePadKeyLoader.getGamePadButtonLabelsArray();
        gamePadModeEntryValuesArray = gamePadKeyLoader.getGamePadModeEntryValuesArray();
        gamePadModeEntriesArray = gamePadKeyLoader.getGamePadModeEntriesArray();

    }

    private void setGamepadKeysUI() {
        prefGamePadType = prefs.getString("prefGamePadType", getApplicationContext().getResources().getString(R.string.prefGamePadTypeDefaultValue));
        Spinner spinner = findViewById(R.id.gamepad_test_mode);

        prefGamePadTypeIndex = Arrays.asList(gamePadModeEntryValuesArray).indexOf(prefGamePadType);
        if (prefGamePadTypeIndex < 0) prefGamePadTypeIndex = 0;

        spinner.setSelection(prefGamePadTypeIndex);

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
        threaded_application.extendedLogging(activityName + ": dispatchGenericMotionEvent(): " + event.getAction());
        if ((!prefGamePadType.equals("None")) && (!mainapp.prefGamePadIgnoreJoystick)) { // respond to the gamepad and keyboard inputs only if the preference is set
            int action;

            float xAxis;
            xAxis = Math.round(event.getAxisValue(MotionEvent.AXIS_X) * 10.0f) / 10.0f;
            float yAxis = Math.round(event.getAxisValue(MotionEvent.AXIS_Y) * 10.0f) / 10.0f;
            float xAxis2 = Math.round(event.getAxisValue(MotionEvent.AXIS_Z) * 10.0f) / 10.0f;
            float yAxis2 = Math.round(event.getAxisValue(MotionEvent.AXIS_RZ) *10.0f) / 10.0f;

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
                    threaded_application.extendedLogging(activityName + ": dispatchKeyEvent(): keycode " + keyCode + " action " + action + " repeat " + repeatCnt);

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
                setGamepadKeysUI();
                oldPrefGamePadTypeIndex = prefGamePadTypeIndex;
            }
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                mainapp.hideSoftKeyboard(view);
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
        TypedArray gamepad_test_button_resource_ids = getResources().obtainTypedArray(R.array.gamepad_test_button_resource_ids);
        for (int i=1;i<=16;i++) {
            bButtons[i] = findViewById(gamepad_test_button_resource_ids.getResourceId(i,0));
            bButtons[i].setClickable(false);
        }

        gamepad_test_button_resource_ids.recycle();

        gamePadKeyLoader = new GamePadKeyLoader(this, mainapp, prefs,
                prefGamePadButtons, gamePadKeys, gamePadKeys_Up, gamePadButtonLabelsArray);

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
        setGamepadKeysUI();

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
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_gamepad_test),
                    "");
        }

        tts = new Tts(prefs,mainapp);

    } // end onCreate

    @Override
    public void onPause() {
        super.onPause();
        threaded_application.activityPaused(activityName);
    }

    @Override
    public void onResume() {
        super.onResume();
        threaded_application.activityResumed(activityName);
        mainapp.removeNotification(this.getIntent());

        threaded_application.currentActivity = activity_id_type.GAMEPAD_TEST;
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        if (GPTMenu != null) {
            mainapp.displayEStop(GPTMenu);
            mainapp.displayFlashlightMenuButton(GPTMenu);
            mainapp.setFlashlightActionViewButton(GPTMenu, findViewById(R.id.flashlight_button));
            mainapp.displayPowerStateMenuButton(GPTMenu);
            mainapp.setPowerStateActionViewButton(GPTMenu, findViewById(R.id.powerLayoutButton));
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
        Log.d(threaded_application.applicationName, activityName + ": onDestroy()");

        if (tg != null) {
            tg.release();
        }

        //mainapp.consist_lights_edit_msg_handler = null;
        super.onDestroy();
    }

    // end current activity
    void endThisActivity(int passedTest) {
        Log.d(threaded_application.applicationName, activityName + ": endThisActivity()");
        threaded_application.activityInTransition(activityName);
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
        mainapp.setFlashlightActionViewButton(menu, findViewById(R.id.flashlight_button));
        mainapp.displayPowerStateMenuButton(menu);
//        mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        if (findViewById(R.id.powerLayoutButton) == null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
                }
            }, 100);
        } else {
            mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        }

        adjustToolbarSize(menu);

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
            mainapp.toggleFlashlightActionView(this, GPTMenu, findViewById(R.id.flashlight_button));
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
            switch (msg.what) {
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateActionViewButton(GPTMenu, findViewById(R.id.powerLayoutButton));
                        }
                    }
                    break;
                }

                case message_type.REOPEN_THROTTLE:
                    if (threaded_application.currentActivity == activity_id_type.GAMEPAD_TEST)
                        endThisActivity();
                    break;

                case message_type.TERMINATE_ALL_ACTIVITIES_BAR_CONNECTION:
                case message_type.LOW_MEMORY:
                    endThisActivity();
                    break;

                default:
                    break;

            }
        }
    }

    //Always go to throttle if back button pressed
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        if (key == KEYCODE_BACK) {
            endThisActivity();
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    void endThisActivity() {
        Log.d(threaded_application.applicationName, activityName + ": endThisActivity()");
        threaded_application.activityInTransition(activityName);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("whichGamepadNo", whichGamepadNo+"2");  //pass whichGamepadNo as an extra - plus "2" for fail
        setResult(result, resultIntent);
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

    private void shutdown() {
        this.finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    void adjustToolbarSize(Menu menu) {
        int newHeightAndWidth = mainapp.adjustToolbarSize(toolbar);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            View itemChooser = item.getActionView();

            if (itemChooser != null) {
                itemChooser.getLayoutParams().height = newHeightAndWidth;
                itemChooser.getLayoutParams().width = (int) ( (float) newHeightAndWidth * 1.3 );

                itemChooser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onOptionsItemSelected(item);
                    }
                });
            }
        }
    }
}