package jmri.enginedriver.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.WindowManager;

import java.util.Arrays;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

public class GamePadKeyLoader {
    private final threaded_application mainapp;
    private final SharedPreferences prefs;
    private final Activity activity;
    private final String[] prefGamePadButtons;
    private final int[] gamePadKeys;
    private final int[] gamePadKeys_Up;
    private final String[] gamePadButtonLabelsArray;
    private final String activityName = "GamePadKeyLoader";
    private static final int GAMEPAD_KEYS_LENGTH = 21;

    public GamePadKeyLoader(Activity activity, threaded_application mainapp,
                            SharedPreferences prefs, String[] prefGamePadButtons,
                            int[] gamePadKeys, int[] gamePadKeys_Up,
                            String[] gamePadButtonLabelsArray) {
        this.activity = activity;
        this.mainapp = mainapp;
        this.prefs = prefs;
        this.prefGamePadButtons = prefGamePadButtons;
        this.gamePadKeys = gamePadKeys;
        this.gamePadKeys_Up = gamePadKeys_Up;
        this.gamePadButtonLabelsArray = gamePadButtonLabelsArray;
    }

    // setup the appropriate keycodes for the type of gamepad that has been selected in the preferences
    public void loadGamepadKeys() {
        mainapp.prefGamePadType = prefs.getString("prefGamePadType", activity.getApplicationContext().getResources().getString(R.string.prefGamePadTypeDefaultValue));
        Log.d(threaded_application.applicationName, activityName + ": setGamepadKeys() : prefGamePadType: " + mainapp.prefGamePadType);

        // Gamepad button Preferences
        prefGamePadButtons[0] = prefs.getString("prefGamePadButtonStart", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonStartDefaultValue));
        prefGamePadButtons[9] = prefs.getString("prefGamePadButtonReturn", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonReturnDefaultValue));
        prefGamePadButtons[1] = prefs.getString("prefGamePadButton1", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButton1DefaultValue));
        prefGamePadButtons[2] = prefs.getString("prefGamePadButton2", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButton2DefaultValue));
        prefGamePadButtons[3] = prefs.getString("prefGamePadButton3", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButton3DefaultValue));
        prefGamePadButtons[4] = prefs.getString("prefGamePadButton4", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButton4DefaultValue));
        // Gamepad DPAD Preferences
        prefGamePadButtons[5] = prefs.getString("prefGamePadButtonUp", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonUpDefaultValue));
        prefGamePadButtons[6] = prefs.getString("prefGamePadButtonRight", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightDefaultValue));
        prefGamePadButtons[7] = prefs.getString("prefGamePadButtonDown", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonDownDefaultValue));
        prefGamePadButtons[8] = prefs.getString("prefGamePadButtonLeft", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftDefaultValue));

        //extra buttons
        prefGamePadButtons[10] = prefs.getString("prefGamePadButtonLeftShoulder", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftTriggerDefaultValue));
        prefGamePadButtons[11] = prefs.getString("prefGamePadButtonRightShoulder", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightShoulderDefaultValue));
        prefGamePadButtons[12] = prefs.getString("prefGamePadButtonLeftTrigger", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftTriggerDefaultValue));
        prefGamePadButtons[13] = prefs.getString("prefGamePadButtonRightTrigger", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightTriggerDefaultValue));
        prefGamePadButtons[14] = prefs.getString("prefGamePadButtonLeftThumb", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonLeftThumbDefaultValue));
        prefGamePadButtons[15] = prefs.getString("prefGamePadButtonRightThumb", activity.getApplicationContext().getResources().getString(R.string.prefGamePadButtonRightThumbDefaultValue));

        if (!mainapp.prefGamePadType.equals(threaded_application.WHICH_GAMEPAD_MODE_NONE)) {
            // make sure the Soft keyboard is hidden
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

        int[] bGamePadKeys;
        int[] bGamePadKeysUp;

        String[] gamePadButtonLabelsArray;

        String[] gamePadModeEntriesArray = activity.getResources().getStringArray(R.array.prefGamePadTypeEntryValues);
        int prefGamePadTypeIndex = Arrays.asList(gamePadModeEntriesArray).indexOf(mainapp.prefGamePadType);
        if (prefGamePadTypeIndex<0) prefGamePadTypeIndex=0;

        TypedArray prefGamePadTypeKeysIds = activity.getResources().obtainTypedArray(R.array.prefGamePadTypeKeysIds);
        TypedArray prefGamePadTypeKeysUpIds = activity.getResources().obtainTypedArray(R.array.prefGamePadTypeKeysUpIds);
        TypedArray gamepadTestButtonLabelsIds = activity.getResources().obtainTypedArray(R.array.gamepadTestButtonLabelsIds);

        bGamePadKeys = activity.getResources().getIntArray(prefGamePadTypeKeysIds.getResourceId(prefGamePadTypeIndex,0));
        bGamePadKeysUp = activity.getResources().getIntArray(prefGamePadTypeKeysUpIds.getResourceId(prefGamePadTypeIndex,0));

        gamePadButtonLabelsArray = activity.getResources().getStringArray(gamepadTestButtonLabelsIds.getResourceId(prefGamePadTypeIndex, 0));

        prefGamePadTypeKeysIds.recycle();
        prefGamePadTypeKeysUpIds.recycle();
        gamepadTestButtonLabelsIds.recycle();

        // now grab the keycodes and put them into the arrays that will actually be used.
        for (int i = 0; i < GAMEPAD_KEYS_LENGTH; i++) {
            gamePadKeys[i] = bGamePadKeys[i];
            gamePadKeys_Up[i] = bGamePadKeysUp[i];
        }

        // if the preference name has "-rotate" at the end of it swap the dpad buttons around
        if (mainapp.prefGamePadType.contains("-rotate")) {
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

    public int[] getGamePadKeys() {
        return gamePadKeys;
    }

    public int[] getGamePadKeys_Up() {
        return gamePadKeys_Up;
    }

    public String[] getGamePadButtonLabelsArray() {
        return gamePadButtonLabelsArray;
    }

    public String[] getGamePadModeEntryValuesArray() {
        return activity.getResources().getStringArray(R.array.prefGamePadTypeEntryValues);
    }

    public String[] getGamePadModeEntriesArray() {
        return activity.getResources().getStringArray(R.array.prefGamePadTypeEntries);
    }

}
