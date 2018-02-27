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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import eu.esu.mobilecontrol2.sdk.MobileControl2;

public class preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    static public final int RESULT_GAMEPAD = RESULT_FIRST_USER;
    static public final int RESULT_ESUMCII = RESULT_GAMEPAD + 1;

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu PRMenu;
    private int result;                     // set to RESULT_FIRST_USER when something is edited

    private String deviceId = "";

    private boolean currentlyImporting = false;
    private String exportedPreferencesFileName =  "exported_preferences.ed";
    private boolean overwiteFile = false;

    private ArrayList<Integer> engine_address_list;
    private ArrayList<Integer> address_size_list; // Look at address_type.java
    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    private static final String DEMO_HOST = "jmri.mstevetodd.com";
    private String[] prefHostImportExportOptionsFound = {"None"};
    private static final String IMPORT_PREFIX = "Import- "; // these two have to be the same length
    private static final String EXPORT_PREFIX = "Export- ";

    private static final String IMPORT_EXPORT_OPTION_NONE = "None";
    private static final String IMPORT_EXPORT_OPTION_EXPORT = "Export";
    private static final String IMPORT_EXPORT_OPTION_IMPORT ="Import";
    private static final String IMPORT_EXPORT_OPTION_RESET = "Reset";

    private static String GAMEPAD_BUTTON_NOT_AVAILABLE_LABEL = "Button not available";
    private static String GAMEPAD_BUTTON_NOT_USABLE_LABEL = "Button not usable";

    /**
     * Called when the activity is first created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ListPreference preference;

        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);

        mainapp = (threaded_application) getApplication();
        mainapp.applyTheme(this);

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        if (!mainapp.isPowerControlAllowed()) {
            getPreferenceScreen().findPreference("show_layout_power_button_preference").setSelectable(false);
            getPreferenceScreen().findPreference("show_layout_power_button_preference").setEnabled(false);
        }
        if (mainapp.androidVersion < mainapp.minWebSocketVersion) {
            getPreferenceScreen().findPreference("ClockDisplayTypePreference").setSelectable(false);
            getPreferenceScreen().findPreference("ClockDisplayTypePreference").setEnabled(false);
        }
        if (mainapp.androidVersion < mainapp.minImmersiveModeVersion) {
            getPreferenceScreen().findPreference("prefThrottleViewImmersiveMode").setSelectable(false);
            getPreferenceScreen().findPreference("prefThrottleViewImmersiveMode").setEnabled(false);
        }
        if (mainapp.androidVersion < mainapp.minThemeVersion) {
            getPreferenceScreen().findPreference("prefTheme").setSelectable(false);
            getPreferenceScreen().findPreference("prefTheme").setEnabled(false);
        }
        result = RESULT_OK;

        if (mainapp.connectedHostName.equals("")) { // option is only available when there is no curent connection
            getConnectionsList();
            preference = (ListPreference) findPreference("prefHostImportExport");
            preference.setEntries(prefHostImportExportOptionsFound);
            preference.setEntryValues(prefHostImportExportOptionsFound);
        } else {
            getPreferenceScreen().findPreference("prefHostImportExport").setSelectable(false);
            getPreferenceScreen().findPreference("prefHostImportExport").setEnabled(false);
        }

//        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
        setGamePadPrefLabels(sharedPreferences);

        // Disable ESU MCII preferences if not an ESU MCII
        if (!MobileControl2.isMobileControl2()) {
            getPreferenceScreen().findPreference("prefEsuMc2").setSelectable(false);
            getPreferenceScreen().findPreference("prefEsuMc2").setEnabled(false);
        }

        sharedPreferences.edit().putBoolean("prefGamepadTestNow", false).commit();  //reset the preference

        if (mainapp.androidVersion < mainapp.minActivatedButtonsVersion) {
            getPreferenceScreen().findPreference("prefSelectedLocoIndicator").setSelectable(false);
            getPreferenceScreen().findPreference("prefSelectedLocoIndicator").setEnabled(false);
        }

        deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        sharedPreferences.edit().putString("prefAndroidId", deviceId).commit();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        // Set up a listener whenever a key changes            
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        if (PRMenu != null) {
            mainapp.displayEStop(PRMenu);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        Log.d("Engine_Driver", "preferences.onPause() called");
        super.onPause();

        // Unregister the listener            
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        if (!this.isFinishing()) {
            mainapp.addNotification(this.getIntent());
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("Engine_Driver", "preferences.onDestroy() called");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preferences_menu, menu);
        PRMenu = menu;
        mainapp.displayEStop(menu);
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

    @SuppressWarnings("deprecation")
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        threaded_application mainapp = (threaded_application) this.getApplication();
        switch (key) {
            case "throttle_name_preference": {
                String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
                String currentValue = sharedPreferences.getString(key, defaultName).trim();
                //if new name is blank or the default name, make it unique
                if (currentValue.equals("") || currentValue.equals(defaultName)) {
                    String deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
                    if (deviceId != null && deviceId.length() >= 4) {
                        deviceId = deviceId.substring(deviceId.length() - 4);
                    } else {
                        Random rand = new Random();
                        deviceId = String.valueOf(rand.nextInt(9999));  //use random string
                    }
                    if (MobileControl2.isMobileControl2()) {
                        // Change default name for ESU MCII
                        defaultName = getApplicationContext().getResources().getString(R.string.prefEsuMc2ThrottleNameDefaultValue);
                    }
                    String uniqueDefaultName = defaultName + " " + deviceId;
                    sharedPreferences.edit().putString(key, uniqueDefaultName).commit();  //save new name to prefs
                }
                break;
            }
            case "maximum_throttle_preference":
                //limit new value to 1-100 (%)
                limitIntPrefValue(sharedPreferences, key, 1, 100, "100");
                break;
            case "maximum_throttle_change_preference":
                limitIntPrefValue(sharedPreferences, key, 1, 100, "25");
                break;
            case "speed_arrows_throttle_speed_step":
                limitIntPrefValue(sharedPreferences, key, 1, 99, "4");
                break;
            case "prefScreenBrightnessDim":
                limitIntPrefValue(sharedPreferences, key, 1, 100, "5");
                break;
            case "WebViewLocation":
                mainapp.alert_activities(message_type.WEBVIEW_LOC, "");
                break;
            case "ThrottleOrientation":
                //if mode was fixed (Port or Land) won't get callback so need explicit call here
                mainapp.setActivityOrientation(this);
                break;
            case "InitialWebPage":
                mainapp.alert_activities(message_type.INITIAL_WEBPAGE, "");
                break;
            case "InitialThrotWebPage":
                mainapp.alert_activities(message_type.INITIAL_WEBPAGE, "");
                break;
            case "ClockDisplayTypePreference":
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CLOCK_DISPLAY);
                break;
            case "prefGamePadFeedbackVolume":
                //limit check new value
                limitIntPrefValue(sharedPreferences, key, ToneGenerator.MIN_VOLUME, ToneGenerator.MAX_VOLUME,
                       getApplicationContext().getResources().getString(R.string.prefGamePadFeedbackVolumeDefaultValue));
                result = RESULT_GAMEPAD;
                break;
            case "prefGamePadType":
                setGamePadPrefLabels(sharedPreferences);
            case "prefGamePadStartButton":
                result = RESULT_GAMEPAD;
                break;
            case "prefEsuMc2ZeroTrim":
                // limit check new value
                limitIntPrefValue(sharedPreferences, key, 0, 255, "10");
                result = RESULT_ESUMCII;
                break;
            case "prefEsuMc2ButtonsRepeatDelay":
                // limit check new value
                limitIntPrefValue(sharedPreferences, key, 0, 9999, "500");
                break;
            case "prefEsuMc2StopButtonDelay":
                // limit check new value
                limitIntPrefValue(sharedPreferences, key, 0, 9999, "500");
                break;
            case "prefImportExport":
                if (!importExportPreferences.currentlyImporting) {
                    exportedPreferencesFileName =  "exported_preferences.ed";
                    String currentValue = sharedPreferences.getString(key, "");
                    if (currentValue.equals(IMPORT_EXPORT_OPTION_EXPORT)) {
                        saveSharedPreferencesToFile(sharedPreferences,exportedPreferencesFileName);
                    } else if (currentValue.equals(IMPORT_EXPORT_OPTION_IMPORT)) {
                        loadSharedPreferencesFromFile(sharedPreferences,exportedPreferencesFileName, deviceId);
                    } else if (currentValue.equals(IMPORT_EXPORT_OPTION_RESET)) {
                        resetPreferences(sharedPreferences);
                    }
                }
                break;
            case "prefHostImportExport":
                if (!importExportPreferences.currentlyImporting) {
                    String currentValue = sharedPreferences.getString(key, "");
                    if (!currentValue.equals(IMPORT_EXPORT_OPTION_NONE)) {
                        String action = currentValue.substring(0,IMPORT_PREFIX.length());
                        exportedPreferencesFileName = currentValue.substring(IMPORT_PREFIX.length(),currentValue.length());
                        if (action.equals(EXPORT_PREFIX)) {
                            saveSharedPreferencesToFile(sharedPreferences,exportedPreferencesFileName);
                        } else if (action.equals(IMPORT_PREFIX)) {
                            loadSharedPreferencesFromFile(sharedPreferences, exportedPreferencesFileName, deviceId);
                        }
                    }
                }
                break;
            case "prefGamepadTestNow":
                start_gamepad_test_activity();
                break;
            case "prefAccelerometerShakeThreshold":
                limitFloatPrefValue(sharedPreferences, key, 1.2F, 3.0F, "2.0"); // limit check new value
                break;
        }
    }

    void start_gamepad_test_activity() {
        SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        boolean result = prefs.getBoolean("prefGamepadTestNow", getResources().getBoolean(R.bool.prefGamepadTestNowDefaultValue));

        if (result) {
            prefs.edit().putBoolean("prefGamepadTestNow", false).commit();  //reset the preference
            reload();

            try {
                Intent in = new Intent().setClass(this, gamepad_test.class);
                //navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            } catch (Exception ex) {
                Log.d("Engine_Driver", ex.getMessage());
            }

        }
    }

    @SuppressWarnings({ "unchecked" })
    private boolean loadSharedPreferencesFromFile(SharedPreferences sharedPreferences, String exportedPreferencesFileName, String deviceId) {
        boolean res = importExportPreferences.loadSharedPreferencesFromFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName, deviceId);

        if (!res) {
            Toast.makeText(getApplicationContext(), "Import from 'engine_driver/" + exportedPreferencesFileName + "' failed! You may not have saved the preferences for this host yet.", Toast.LENGTH_LONG).show();
        }
        fixAndReloadImportExportPreference(sharedPreferences);
        return res;
    }

    private boolean saveSharedPreferencesToFile(SharedPreferences sharedPreferences, String exportedPreferencesFileName) {
        boolean res = false;
        if (!exportedPreferencesFileName.equals(".ed")) {
            File path = Environment.getExternalStorageDirectory();
            File engine_driver_dir = new File(path, "engine_driver");
            engine_driver_dir.mkdir();            // create directory if it doesn't exist

            File dst = new File(path, "engine_driver/"+exportedPreferencesFileName);

            if(dst.exists()) {
                overwiteFileDialog(sharedPreferences, dst);
            } else {
                res = importExportPreferences.saveSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Not connected to a host.", Toast.LENGTH_LONG).show();
        }
        return res;
    }

    public void reload() {
        // restart the activity so all the preferences show correctly based on what was imported
        if (Build.VERSION.SDK_INT >= 11) {
            recreate();
        } else {
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);

            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    private void resetPreferences(SharedPreferences sharedPreferences){
        SharedPreferences.Editor prefEdit = sharedPreferences.edit();
        prefEdit.clear();
        prefEdit.commit();
        reload();
        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesResetSucceeded), Toast.LENGTH_LONG).show();
    }

    private void fixAndReloadImportExportPreference(SharedPreferences sharedPreferences){
        sharedPreferences.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        sharedPreferences.edit().putString("prefHostImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        reload();
    }

    public boolean overwiteFileDialog(SharedPreferences sharedPreferences, File dst) {
        boolean res = false;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                boolean res = false;
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        res = importExportPreferences.saveSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
                        overwiteFile = true;
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        overwiteFile = false;
                        break;
                }
                fixAndReloadImportExportPreference(sharedPreferences);
            }
        };
        AlertDialog.Builder ab = new AlertDialog.Builder(preferences.this);
        ab.setMessage("File already exists. Overwite?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener);
        ab.show();
        return overwiteFile;
    }

    static void putObject(SharedPreferences sharedPreferences, final String key, final Object val) {
        if (val instanceof Boolean)
            sharedPreferences.edit().putBoolean(key, (Boolean) val).commit();
        else if (val instanceof Float)
            sharedPreferences.edit().putFloat(key, (Float) val).commit();
        else if (val instanceof Integer)
            sharedPreferences.edit().putInt(key, (Integer) val).commit();
        else if (val instanceof Long)
            sharedPreferences.edit().putLong(key, (Long) val).commit();
        else if (val instanceof String)
            sharedPreferences.edit().putString(key, ((String)val)).commit();

    }

    @SuppressWarnings("deprecation")
    private boolean limitIntPrefValue(SharedPreferences sharedPreferences, String key, int minVal, int maxVal, String defaultVal) {
        boolean isValid = true;
        EditTextPreference prefText = (EditTextPreference) getPreferenceScreen().findPreference(key);
        try {
            int newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Integer.toString(maxVal)).commit();
                prefText.setText(Integer.toString(maxVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits).replace("%%1%%",Integer.toString(minVal)).replace("%%2%%",Integer.toString(minVal)).replace("%%3%%",Float.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Integer.toString(minVal)).commit();
                prefText.setText(Integer.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits).replace("%%1%%",Integer.toString(minVal)).replace("%%2%%",Integer.toString(minVal)).replace("%%3%%",Float.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric).replace("%%1%%",Integer.toString(minVal)).replace("%%2%%",Integer.toString(maxVal)).replace("%%3%%",defaultVal), Toast.LENGTH_LONG).show();
        }
        return isValid;
    }

    @SuppressWarnings("deprecation")
    private boolean limitFloatPrefValue(SharedPreferences sharedPreferences, String key, Float minVal, Float maxVal, String defaultVal) {
        boolean isValid = true;
        EditTextPreference prefText = (EditTextPreference) getPreferenceScreen().findPreference(key);
        try {
            Float newVal = Float.parseFloat(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Float.toString(maxVal)).commit();
                prefText.setText(Float.toString(maxVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits).replace("%%1%%",Float.toString(minVal)).replace("%%2%%",Float.toString(maxVal)).replace("%%3%%",Float.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Float.toString(minVal)).commit();
                prefText.setText(Float.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits).replace("%%1%%",Float.toString(minVal)).replace("%%2%%",Float.toString(maxVal)).replace("%%3%%",Float.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric).replace("%%1%%",Float.toString(minVal)).replace("%%2%%",Float.toString(maxVal)).replace("%%3%%",defaultVal), Toast.LENGTH_LONG).show();
        }
        return isValid;
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            setResult(result);
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    static public int getIntPrefValue(SharedPreferences sharedPreferences, String key, String defaultVal) {
        int newVal;
        try {
            newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
        } catch (NumberFormatException e) {
            try {
                newVal = Integer.parseInt(defaultVal);
            } catch (NumberFormatException ex) {
                newVal = 0;
            }
        }
        return newVal;
    }

    private void getConnectionsList() {
        boolean foundDemoHost = false;
        String host_name;
        String host_name_filename;
        String errMsg;

        try {
            File sdcard_path = Environment.getExternalStorageDirectory();
            File connections_list_file = new File(sdcard_path, "engine_driver/connections_list.txt");

            if (connections_list_file.exists()) {
                BufferedReader list_reader = new BufferedReader(new FileReader(connections_list_file));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    List<String> parts = Arrays.asList(line.split(":", 3)); //split record from file, max of 3 parts
                    if (parts.size() > 1) {  //skip if not split
                        host_name = parts.get(0);
                        host_name_filename = host_name.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                        if (host_name.equals(DEMO_HOST)) {
                            foundDemoHost = true;
                        }
                        if ((!host_name.equals("")) && (!isAlreadyInArray(prefHostImportExportOptionsFound, IMPORT_PREFIX + host_name_filename))) {
                            prefHostImportExportOptionsFound = add(prefHostImportExportOptionsFound, IMPORT_PREFIX + host_name_filename);
                            prefHostImportExportOptionsFound = add(prefHostImportExportOptionsFound, EXPORT_PREFIX + host_name_filename);
                         }
                    }
                }
                list_reader.close();
            }
        } catch (IOException except) {
            errMsg = except.getMessage();
            Log.e("connection_activity", "Error reading recent connections list: " + errMsg);
            Toast.makeText(getApplicationContext(), "Error reading recent connections list: " + errMsg, Toast.LENGTH_SHORT).show();
        }

        if (!foundDemoHost) {
            prefHostImportExportOptionsFound = add(prefHostImportExportOptionsFound, IMPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
            prefHostImportExportOptionsFound = add(prefHostImportExportOptionsFound, EXPORT_PREFIX+ DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
        }

    }

    private static String[] add(String[] stringArray, String newValue) {
        String[] tempArray = new String[ stringArray.length + 1 ];
        for (int i=0; i<stringArray.length; i++) {
            tempArray[i] = stringArray[i];
        }
        tempArray[stringArray.length] = newValue;
        return tempArray;
    }

    public static boolean isAlreadyInArray(String[] arr, String targetValue) {
        for(String s: arr){
            if(s.equals(targetValue))
                return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void setGamePadPrefLabels(SharedPreferences sharedPreferences) {
        String whichGamePadMode = sharedPreferences.getString("prefGamePadType", "None").trim();
        String[] gamePadPrefLabels;
        String[] gamePadPrefButtonReferences = this.getResources().getStringArray(R.array.prefGamePadPrefButtonReferences);

        switch (whichGamePadMode) {
            case "iCade+DPAD":
            case "iCade+DPAD-rotate":
            case "MTK":
            case "MTK-rotate":
            case "Game":
            case "Game-rotate":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadMocuteLabels);
                break;
/*
            case "VRBoxA":
            case "VRBoxA-rotate":
            case "VRBoxC":
            case "VRBoxC-rotate":
            case "VRBoxiC":
            case "VRBoxiC-rotate":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadVRBoxLabels);
                break;
*/
            case "MagicseeR1B":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadMagicseeR1Labels);
                break;
            case "None":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadNoneLabels);
                break;
            default:
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadMocuteLabels);
                break;
        }
        for (int i=1; i<gamePadPrefLabels.length; i++) {  // skip the first one
            boolean thisEnabled = true;
            Preference thisPref = getPreferenceScreen().findPreference(gamePadPrefButtonReferences[i]);
            thisPref.setTitle(gamePadPrefLabels[i]);
            if ((gamePadPrefLabels[i].equals(GAMEPAD_BUTTON_NOT_AVAILABLE_LABEL)) || (gamePadPrefLabels[i].equals(GAMEPAD_BUTTON_NOT_USABLE_LABEL))) {
                thisEnabled = false;
            }
            thisPref.setSelectable(thisEnabled);
            thisPref.setEnabled(thisEnabled);
        }

        boolean thisEnabled = true;
        if (whichGamePadMode.equals("None")) {
            thisEnabled = false;
        }
        Preference thisPref;

        thisPref = getPreferenceScreen().findPreference("prefGamePadFeedbackVolume");
        thisPref.setSelectable(thisEnabled);
        thisPref.setEnabled(thisEnabled);

        thisPref = getPreferenceScreen().findPreference("prefGamePadMultipleDevices");
        thisPref.setSelectable(thisEnabled);
        thisPref.setEnabled(thisEnabled);

        thisPref = getPreferenceScreen().findPreference("prefGamePadSpeedArrowsThrottleRepeatDelay");
        thisPref.setSelectable(thisEnabled);
        thisPref.setEnabled(thisEnabled);

        thisPref = getPreferenceScreen().findPreference("prefGamepadSwapForwardReverseWithScreenButtons");
        thisPref.setSelectable(thisEnabled);
        thisPref.setEnabled(thisEnabled);

        thisPref = getPreferenceScreen().findPreference("prefGamepadTestEnforceTesting");
        thisPref.setSelectable(thisEnabled);
        thisPref.setEnabled(thisEnabled);

        thisPref = getPreferenceScreen().findPreference("prefGamepadTestNow");
        thisPref.setSelectable(thisEnabled);
        thisPref.setEnabled(thisEnabled);
    }

}
