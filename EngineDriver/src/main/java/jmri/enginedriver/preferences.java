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
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;

public class preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener,PermissionsHelper.PermissionsHelperGrantedCallback {
    static public final int RESULT_GAMEPAD = RESULT_FIRST_USER;
    static public final int RESULT_ESUMCII = RESULT_GAMEPAD + 1;

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu PRMenu;
    private int result;                     // set to RESULT_FIRST_USER when something is edited

    private String deviceId = "";

    private String exportedPreferencesFileName = "exported_preferences.ed";
    private boolean overwiteFile = false;

    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    private static final String DEMO_HOST = "jmri.mstevetodd.com";
    private String[] prefHostImportExportEntriesFound = {"None"};
    private String[] prefHostImportExportEntryValuesFound = {"None"};
    private static final String IMPORT_PREFIX = "Import- "; // these two have to be the same length
    private static final String EXPORT_PREFIX = "Export- ";

    private static final String IMPORT_EXPORT_OPTION_NONE = "None";
    private static final String IMPORT_EXPORT_OPTION_EXPORT = "Export";
    private static final String IMPORT_EXPORT_OPTION_IMPORT = "Import";
    private static final String IMPORT_EXPORT_OPTION_RESET = "Reset";
    private static final String IMPORT_EXPORT_OPTION_IMPORT_URL = "URL";

    private static String GAMEPAD_BUTTON_NOT_AVAILABLE_LABEL = "Button not available";
    private static String GAMEPAD_BUTTON_NOT_USABLE_LABEL = "Button not usable";

    private String prefThrottleScreenTypeOriginal = "Default";
    private String prefThemeOriginal = "Default";

    private String prefConsistFollowRuleStyle = "original";
    private static String CONSIST_FUNCTION_RULE_STYLE_ORIGINAL = "original";
    private static String CONSIST_FUNCTION_RULE_STYLE_COMPLEX = "complex";

    private ProgressDialog pDialog;
    public static final int PROGRESS_BAR_TYPE = 0;
    private static final String EXTERNAL_URL_PREFERENCES_IMPORT = "external_url_preferences_import.ed";
    private static final String ENGINE_DRIVER_DIR = "engine_driver";
    private static final String SERVER_ENGINE_DRIVER_DIR = "prefs/engine_driver";

    private static final int FORCED_RESTART_REASON_NONE = 0;
    private static final int FORCED_RESTART_REASON_RESET = 1;
    private static final int FORCED_RESTART_REASON_IMPORT = 2;
    private static final int FORCED_RESTART_REASON_IMPORT_SERVER_MANUAL = 3;
    private static final int FORCED_RESTART_REASON_THEME = 4;
    private static final int FORCED_RESTART_REASON_THROTTLE_PAGE = 5;
    private static final int FORCED_RESTART_REASON_LOCALE = 6;
//    private static final int FORCED_RESTART_REASON_IMPORT_SERVER_AUTO = 7;    // not used in preferences.  Used in throttle.java
//    private static final int FORCED_RESTART_REASON_AUTO_IMPORT = 8; // for local server files

    SharedPreferences sharedPreferences;
    /**
     * Called when the activity is first created.
     */

    private static String AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT = "Connect Disconnect";

    private static final String PREF_IMPORT_ALL_FULL = "Yes";
    private static final String PREF_IMPORT_ALL_PARTIAL = "No";
    private static final String PREF_IMPORT_ALL_RESET = "-";

    private String[] advancedPreferences;

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ListPreference preference;

        sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);

        mainapp = (threaded_application) getApplication();
        mainapp.applyTheme(this);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_preferences)); // needed in case the langauge was changed from the default

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        if (!mainapp.isPowerControlAllowed()) {
            enableDisablePreference("show_layout_power_button_preference", false);
        }
        if (mainapp.androidVersion < mainapp.minWebSocketVersion) {
            enableDisablePreference("ClockDisplayTypePreference", false);
        }
        if (mainapp.androidVersion < mainapp.minImmersiveModeVersion) {
            enableDisablePreference("prefThrottleViewImmersiveMode", false);
        }

        prefThemeOriginal = sharedPreferences.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
        if (mainapp.androidVersion < mainapp.minThemeVersion) {
            enableDisablePreference("prefTheme", false);
        }

        result = RESULT_OK;

        if (mainapp.connectedHostName.equals("")) { // option is only available when there is no current connection
            getConnectionsList();
            preference = (ListPreference) findPreference("prefHostImportExport");
            preference.setEntries(prefHostImportExportEntriesFound);
            preference.setEntryValues(prefHostImportExportEntryValuesFound);
        } else {
            enableDisablePreference("prefHostImportExport", false);
        }

        setGamePadPrefLabels(sharedPreferences);

        // Disable ESU MCII preferences if not an ESU MCII
        if (!MobileControl2.isMobileControl2()) {
            enableDisablePreference("prefEsuMc2", false);
        }

        enableDisablePreference("prefFlashlightButtonDisplay", mainapp.isFlashlightAvailable());

        sharedPreferences.edit().putBoolean("prefGamepadTestNow", false).commit();  //reset the preference

        if (mainapp.androidVersion < mainapp.minActivatedButtonsVersion) {
            enableDisablePreference("prefSelectedLocoIndicator", false);
        }

        deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        sharedPreferences.edit().putString("prefAndroidId", deviceId).commit();

        String currentValue = sharedPreferences.getString("prefTtsWhen", "");
        if (currentValue.equals("None")) {
            enableDisablePreference("prefTtsThrottleResponse", false);
            enableDisablePreference("prefTtsGamepadTest", false);
            enableDisablePreference("prefTtsGamepadTestComplete", false);
        }

        prefThrottleScreenTypeOriginal = sharedPreferences.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        showHideThrottleTypePreferences();

        sharedPreferences.edit().putBoolean("prefForcedRestart", false).commit();
        sharedPreferences.edit().putInt("prefForcedRestartReason", FORCED_RESTART_REASON_NONE).commit();
        sharedPreferences.edit().putString("prefPreferencesImportAll", PREF_IMPORT_ALL_RESET).commit();

        if (!sharedPreferences.getString("prefImportExport", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue)).equals("None")) {
            // preference is still confused after a reload or reset
            sharedPreferences.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        }

        prefConsistFollowRuleStyle = sharedPreferences.getString("prefConsistFollowRuleStyle", getApplicationContext().getResources().getString(R.string.prefConsistFollowRuleStyleDefaultValue));
        showHideConsistRuleStylePreferences();

        //put pointer to this activity's message handler in main app's shared variable (If needed)
        mainapp.preferences_msg_handler = new preferences_handler();

        if ((mainapp.connectedHostip == null) || (mainapp.web_server_port == 0)) {
            enableDisablePreference("prefImportServerManual", false);
        }

        advancedPreferences = getResources().getStringArray(R.array.advancedPreferences);
        hideAdvancedPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("Engine_Driver", "preferences.onResume() called");
        try {
            dismissDialog(PROGRESS_BAR_TYPE);
        } catch (Exception e) {
            Log.d("Engine_Driver", "preferences.onResume() no dialog to kill");
        }

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

    @Override
    protected void onPause() {
        //Log.d("Engine_Driver", "preferences.onPause() called");
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
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ApplySharedPref")
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        threaded_application mainapp = (threaded_application) this.getApplication();

        boolean prefForcedRestart = sharedPreferences.getBoolean("prefForcedRestart", false);

        if (!prefForcedRestart) {  // don't do anything if the preference have been loaded and we are about to reload the app.
            switch (key) {
                case "throttle_name_preference": {
                    String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
                    String currentValue = mainapp.fixThrottleName(sharedPreferences.getString(key, defaultName).trim());
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
                case "prefConnectTimeoutMs":
                    limitIntPrefValue(sharedPreferences, key, 100, 99999, getResources().getString(R.string.prefConnectTimeoutMsDefaultValue));
                    break;
                case "prefSocketTimeoutMs":
                    limitIntPrefValue(sharedPreferences, key, 100, 9999, getResources().getString(R.string.prefSocketTimeoutMsDefaultValue));
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
                        exportedPreferencesFileName = "exported_preferences.ed";
                        String currentValue = sharedPreferences.getString(key, "");
                        if (currentValue.equals(IMPORT_EXPORT_OPTION_EXPORT)) {
                            saveSharedPreferencesToFile(sharedPreferences, exportedPreferencesFileName, true);
                        } else if (currentValue.equals(IMPORT_EXPORT_OPTION_IMPORT)) {
                            loadSharedPreferencesFromFile(sharedPreferences, exportedPreferencesFileName, deviceId, FORCED_RESTART_REASON_IMPORT);
                        } else if (currentValue.equals(IMPORT_EXPORT_OPTION_RESET)) {
                            resetPreferences(sharedPreferences);
                        }
                    }
                    break;
                case "prefPreferencesImportAll":
                    String val = sharedPreferences.getString("prefImportServerManual", getApplicationContext().getResources().getString(R.string.prefImportServerManualDefaultValue));
                    val = val.trim();
                    if (!(sharedPreferences.getString("prefPreferencesImportAll", "").equals(PREF_IMPORT_ALL_RESET))) {
                        new importFromURL().execute(val);
                    }
                    break;
                case "prefHostImportExport":
                    if (!importExportPreferences.currentlyImporting) {
                        String currentValue = sharedPreferences.getString(key, "");
                        if (!currentValue.equals(IMPORT_EXPORT_OPTION_NONE)) {
                            String action = currentValue.substring(0, IMPORT_PREFIX.length());
                            exportedPreferencesFileName = currentValue.substring(IMPORT_PREFIX.length(), currentValue.length());
                            if (action.equals(EXPORT_PREFIX)) {
                                saveSharedPreferencesToFile(sharedPreferences, exportedPreferencesFileName, true);
                            } else if (action.equals(IMPORT_PREFIX)) {
                                loadSharedPreferencesFromFile(sharedPreferences, exportedPreferencesFileName, deviceId, FORCED_RESTART_REASON_IMPORT);
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
                case "prefTtsWhen":
                    String currentValue = sharedPreferences.getString("prefTtsWhen", "");
                    boolean enable = true;
                    if (currentValue.equals("None")) {
                        enable = false;
                    }
                    enableDisablePreference("prefTtsGamepadTest", enable);
                    enableDisablePreference("prefTtsGamepadTestComplete", enable);
                    break;
                case "prefNumberOfDefaultFunctionLabels":
                    // limit check new value
                    limitIntPrefValue(sharedPreferences, key, 0, 29, "29");
                    mainapp.set_default_function_labels(false);
                    break;
                case "prefLocale":
                    sharedPreferences.edit().putString("prefLeftDirectionButtons", "").commit();
                    sharedPreferences.edit().putString("prefRightDirectionButtons", "").commit();
                    sharedPreferences.edit().putString("prefLeftDirectionButtonsShort", "").commit();
                    sharedPreferences.edit().putString("prefRightDirectionButtonsShort", "").commit();
                    forceRestartApp(FORCED_RESTART_REASON_LOCALE);
                    break;
                case "prefDirectionButtonLongPressDelay":
                    // limit check new value
                    limitIntPrefValue(sharedPreferences, key, 500, 9999, "1000");
                    break;
                case "prefThrottleScreenType":
                case "NumThrottle":
                    limitNumThrottles(sharedPreferences);
                    break;
                case "prefTheme":
                    String prefTheme = sharedPreferences.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
                    if (!prefTheme.equals(prefThemeOriginal)) {
                        forceRestartApp(FORCED_RESTART_REASON_THEME);
                    }
                    break;
                case "prefConsistFollowRuleStyle":
                    prefConsistFollowRuleStyle = sharedPreferences.getString("prefConsistFollowRuleStyle", getApplicationContext().getResources().getString(R.string.prefConsistFollowRuleStyleDefaultValue));
                    showHideConsistRuleStylePreferences();
                    break;
                case "prefShowAdvancedPreferences":
                    reload();
                    break;
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    void forceRestartApp(int forcedRestartReason) {
        Log.d("Engine_Driver", "Preferences.forceRestartApp() ");

//        SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);

//        sharedPreferences.edit().putBoolean("prefForcedRestart", true).commit();
//        sharedPreferences.edit().putInt("prefForcedRestartReason", forcedRestartReason).commit();

        String prefAutoImportExport = sharedPreferences.getString("prefAutoImportExport", getApplicationContext().getResources().getString(R.string.prefAutoImportExportDefaultValue));

        if (prefAutoImportExport.equals(AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT)) {
            if (mainapp.connectedHostName != null) {
                String exportedPreferencesFileName = mainapp.connectedHostName.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                saveSharedPreferencesToFile(sharedPreferences, exportedPreferencesFileName, false);
            }
        }

        mainapp.forceRestartApp(forcedRestartReason);
    }

    @SuppressLint("ApplySharedPref")
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
                Log.d("Engine_Driver", "Preferences: " + ex.getMessage());
            }

        }
    }

    private void enableDisablePreference(String key, boolean enable) {
        Preference p = getPreferenceScreen().findPreference(key);
        if (p != null) {
            p.setSelectable(enable);
            p.setEnabled(enable);
        } else {
            Log.w("Engine_Driver", "Preference key '" + key + "' not found, not set to " + enable);
        }
    }

    private boolean loadSharedPreferencesFromFile(SharedPreferences sharedPreferences, String exportedPreferencesFileName, String deviceId, int forceRestartReason) {
        Log.d("Engine_Driver", "Preferences: Loading saved preferences from file: " + exportedPreferencesFileName);
        boolean res = importExportPreferences.loadSharedPreferencesFromFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName, deviceId);

        if (!res) {
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.prefImportExportErrorReadingFrom, exportedPreferencesFileName), Toast.LENGTH_LONG).show();
        }
//        fixAndReloadImportExportPreference(sharedPreferences);
        forceRestartApp(forceRestartReason);
        return res;
    }

    @SuppressLint("ApplySharedPref")
    private boolean saveSharedPreferencesToFile(SharedPreferences sharedPreferences, String exportedPreferencesFileName, boolean confirmDialog) {
        Log.d("Engine_Driver", "Preferences: Saving preferences to file");
        sharedPreferences.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        boolean res = false;
        if (!exportedPreferencesFileName.equals(".ed")) {
            File path = Environment.getExternalStorageDirectory();
            File engine_driver_dir = new File(path, ENGINE_DRIVER_DIR);
            engine_driver_dir.mkdir();            // create directory if it doesn't exist

            File dst = new File(path, ENGINE_DRIVER_DIR + "/" + exportedPreferencesFileName);

            if ((dst.exists()) && (confirmDialog)) {
                overwiteFileDialog(sharedPreferences, ENGINE_DRIVER_DIR + "/" + exportedPreferencesFileName);
            } else {
                res = importExportPreferences.saveSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.prefImportExportErrorNotConnected, Toast.LENGTH_LONG).show();
        }
        return res;
    }

    public void reload() {
        // restart the activity so all the preferences show correctly based on what was imported / hidden
        Log.d("Engine_Driver", "Preferences: Forcing activity to recreate");
            recreate();
    }

    @SuppressLint("ApplySharedPref")
    private void resetPreferences(SharedPreferences sharedPreferences) {
        Log.d("Engine_Driver", "Preferences: Resetting preferences");
        SharedPreferences.Editor prefEdit = sharedPreferences.edit();
        prefEdit.clear();
        prefEdit.commit();
        Log.d("Engine_Driver", "Preferences: Reset succeeded");
        delete_settings_file("function_settings.txt");
        delete_settings_file("connections_list.txt");
        delete_settings_file("recent_engine_list.txt");
        delete_auto_import_settings_files();

        reload();

        forceRestartApp(FORCED_RESTART_REASON_RESET);
    }

    private void delete_auto_import_settings_files() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdcard_path = Environment.getExternalStorageDirectory();
            File dir = new File(sdcard_path, ENGINE_DRIVER_DIR); // in case the folder does not already exist
            File[] edFiles = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File folder, String name) {
                    return name.toLowerCase().startsWith("auto_");
                }
            });
            if (edFiles.length > 0){
                for (int i=0; i<edFiles.length; i++) {
                    delete_settings_file(edFiles[i].getName());
                }
            }
        }
    }

    private void delete_settings_file(String file_name) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdcard_path = Environment.getExternalStorageDirectory();
            File settings_file = new File(sdcard_path, "engine_driver/" + file_name);
            if (settings_file.exists()) {
                if (settings_file.delete()) {
                    Log.d("Engine_Driver", file_name + " deleted");
                } else {
                    Log.e("Engine_Driver", file_name + " NOT deleted");
                }
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    private void fixAndReloadImportExportPreference(SharedPreferences sharedPreferences) {
        Log.d("Engine_Driver", "Preferences: Fix and Loading saved preferences.");
        sharedPreferences.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        sharedPreferences.edit().putString("prefHostImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
        reload();
    }

    public boolean overwiteFileDialog(final SharedPreferences sharedPreferences, String fileNameAndPath) {
        boolean res = false;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                boolean res = false;
                switch (which) {
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
        ab.setMessage(getApplicationContext().getResources().getString(R.string.prefImportExportOverwite).replace("%1$s",fileNameAndPath))
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener);
        ab.show();
        return overwiteFile;
    }

    @SuppressLint("ApplySharedPref")
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
            sharedPreferences.edit().putString(key, ((String) val)).commit();

    }

    @SuppressLint("ApplySharedPref")
    private boolean limitIntPrefValue(SharedPreferences sharedPreferences, String key, int minVal, int maxVal, String defaultVal) {
        boolean isValid = true;
        EditTextPreference prefText = (EditTextPreference) getPreferenceScreen().findPreference(key);
        try {
            int newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Integer.toString(maxVal)).commit();
                prefText.setText(Integer.toString(maxVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Integer.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Integer.toString(minVal)).commit();
                prefText.setText(Integer.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Integer.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric, Integer.toString(minVal), Integer.toString(maxVal), defaultVal), Toast.LENGTH_LONG).show();
        }
        return isValid;
    }

    @SuppressLint("ApplySharedPref")
    private boolean limitFloatPrefValue(SharedPreferences sharedPreferences, String key, Float minVal, Float maxVal, String defaultVal) {
        boolean isValid = true;
        EditTextPreference prefText = (EditTextPreference) getPreferenceScreen().findPreference(key);
        try {
            Float newVal = Float.parseFloat(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Float.toString(maxVal)).commit();
                prefText.setText(Float.toString(maxVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Float.toString(minVal), Float.toString(maxVal), Float.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Float.toString(minVal)).commit();
                prefText.setText(Float.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Float.toString(minVal), Float.toString(maxVal), Float.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric, Float.toString(minVal), Float.toString(maxVal), defaultVal), Toast.LENGTH_LONG).show();
        }
        return isValid;
    }

    @SuppressLint("ApplySharedPref")
    private void limitNumThrottles(SharedPreferences sharedPreferences) {
        int numThrottles = mainapp.Numeralise(sharedPreferences.getString("NumThrottle", getResources().getString(R.string.NumThrottleDefaulValue)));
        String prefThrottleScreenType = sharedPreferences.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));
        if (prefThrottleScreenType.equals("Default") && (numThrottles > 3)) {
            sharedPreferences.edit().putString("NumThrottle", "Three").commit();
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastNumThrottles, "Three"), Toast.LENGTH_SHORT).show();
            reload();
        }

        if (prefThrottleScreenType.equals("Vertical") && (numThrottles != 2)) {
            sharedPreferences.edit().putString("NumThrottle", "Two").commit();
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastNumThrottles, "Two"), Toast.LENGTH_SHORT).show();
            reload();
        }

        if ( ((prefThrottleScreenType.equals("Vertical Left")) || (prefThrottleScreenType.equals("Vertical Right"))
                || (prefThrottleScreenType.equals("Big Left")) || (prefThrottleScreenType.equals("Big Right")) )
                && (numThrottles != 1)) {
            sharedPreferences.edit().putString("NumThrottle", "One").commit();
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastNumThrottles, "One"), Toast.LENGTH_SHORT).show();
            reload();
        }

        if (!prefThrottleScreenType.equals(prefThrottleScreenTypeOriginal)) {
            SharedPreferences.Editor prefEdit = sharedPreferences.edit();
            prefEdit.commit();
            reload();
            forceRestartApp(FORCED_RESTART_REASON_THROTTLE_PAGE);
        }
    }

    private void showHideConsistRuleStylePreferences() {
        boolean enable = true;
        if (prefConsistFollowRuleStyle.equals(CONSIST_FUNCTION_RULE_STYLE_ORIGINAL)) {
            enable = false;
        }

        enableDisablePreference("SelectiveLeadSound", !enable);
        enableDisablePreference("SelectiveLeadSoundF1", !enable);
        enableDisablePreference("SelectiveLeadSoundF2", !enable);

        enableDisablePreference("prefConsistFollowDefaultAction", enable);
        enableDisablePreference("prefConsistFollowString1", enable);
        enableDisablePreference("prefConsistFollowAction1", enable);
        enableDisablePreference("prefConsistFollowString2", enable);
        enableDisablePreference("prefConsistFollowAction2", enable);
        enableDisablePreference("prefConsistFollowString3", enable);
        enableDisablePreference("prefConsistFollowAction3", enable);
        enableDisablePreference("prefConsistFollowString4", enable);
        enableDisablePreference("prefConsistFollowAction4", enable);
        enableDisablePreference("prefConsistFollowString5", enable);
        enableDisablePreference("prefConsistFollowAction5", enable);

    }

    private void showHideThrottleTypePreferences() {
        boolean enable = true;
        if ((prefThrottleScreenTypeOriginal.equals("Simple")) || (prefThrottleScreenTypeOriginal.equals("Vertical"))
                || (prefThrottleScreenTypeOriginal.equals("Vertical Left"))  || (prefThrottleScreenTypeOriginal.equals("Vertical Right"))) {
            enable = false;
        }
        enableDisablePreference("increase_slider_height_preference", enable);
        enableDisablePreference("left_slider_margin", enable);
        enableDisablePreference("prefHideSliderAndSpeedButtons", enable);

        enable = !prefThrottleScreenTypeOriginal.equals("Simple");
        enableDisablePreference("prefAlwaysUseDefaultFunctionLabels", enable);
        enableDisablePreference("prefNumberOfDefaultFunctionLabels", enable);
        enableDisablePreference("prefNumberOfDefaultFunctionLabelsForRoster", enable);

        enable = prefThrottleScreenTypeOriginal.equals("Default")
                || prefThrottleScreenTypeOriginal.equals("Vertical")
                || prefThrottleScreenTypeOriginal.equals("Vertical Left")
                || prefThrottleScreenTypeOriginal.equals("Vertical Right");
        enableDisablePreference("WebViewLocation", enable);
        enableDisablePreference("prefIncreaseWebViewSize", enable);
        enableDisablePreference("InitialThrotWebPage", enable);

        enable = !prefThrottleScreenTypeOriginal.equals("Default");
        enableDisablePreference("prefTickMarksOnSliders", enable);
        enableDisablePreference("prefLimitSpeedButton", enable);
        enableDisablePreference("prefLimitSpeedPercent", enable);
    }


    private void removePreference(Preference preference) {
        PreferenceGroup parent = getParent(getPreferenceScreen(), preference);
        if (parent != null)
            parent.removePreference(preference);
        else //Doesn't have a parent
            getPreferenceScreen().removePreference(preference);

    }

    private void hideAdvancedPreferences() {
        if (!sharedPreferences.getBoolean("prefShowAdvancedPreferences", getApplicationContext().getResources().getBoolean(R.bool.prefShowAdvancedPreferencesDefaultValue) ) ) {
            for (String advancedPreference1 : advancedPreferences) {
//                Log.d("Engine_Driver", "Preferences: Remove advanced preference: " + advancedPreference1);

                Preference advancedPreference = getPreferenceScreen().findPreference(advancedPreference1);
                removePreference(advancedPreference);
            }
        }
    }

    private PreferenceGroup getParent(PreferenceGroup groupToSearchIn, Preference preference) {
        for (int i = 0; i < groupToSearchIn.getPreferenceCount(); ++i) {
            Preference child = groupToSearchIn.getPreference(i);

            if (child == preference)
                return groupToSearchIn;

            if (child instanceof PreferenceGroup) {
                PreferenceGroup childGroup = (PreferenceGroup)child;
                PreferenceGroup result = getParent(childGroup, preference);
                if (result != null)
                    return result;
            }
        }

        return null;
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
                        if ((!host_name.equals("")) && (!isAlreadyInArray(prefHostImportExportEntriesFound, IMPORT_PREFIX + host_name_filename))) {
                            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, IMPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, EXPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, IMPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, EXPORT_PREFIX + host_name_filename);
                        }
                    }
                }
                list_reader.close();
            }
        } catch (IOException except) {
            errMsg = except.getMessage();
            Log.e("Engine_Driver", "preferences: Error reading recent connections list: " + errMsg);
            Toast.makeText(getApplicationContext(), R.string.prefImportExportErrorReadingList + " " + errMsg, Toast.LENGTH_SHORT).show();
        }

        if (!foundDemoHost) {
            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, IMPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, EXPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, IMPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, EXPORT_PREFIX + DEMO_HOST.replaceAll("[^A-Za-z0-9_]", "_") + ".ed");
        }

    }

    private static String[] add(String[] stringArray, String newValue) {
        String[] tempArray = new String[stringArray.length + 1];
        System.arraycopy(stringArray, 0, tempArray, 0, stringArray.length);
        tempArray[stringArray.length] = newValue;
        return tempArray;
    }

    public static boolean isAlreadyInArray(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue))
                return true;
        }
        return false;
    }

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
            case "FlydigiWee2":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadFlydigiWee2Labels);
                break;
            case "None":
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadNoneLabels);
                break;
            default:
                gamePadPrefLabels = this.getResources().getStringArray(R.array.prefGamePadMocuteLabels);
                break;
        }
        for (int i = 1; i < gamePadPrefLabels.length; i++) {  // skip the first one
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

        enableDisablePreference("prefGamePadFeedbackVolume", thisEnabled);
        enableDisablePreference("prefGamePadSpeedArrowsThrottleRepeatDelay", thisEnabled);
        enableDisablePreference("prefGamepadSwapForwardReverseWithScreenButtons", thisEnabled);
        enableDisablePreference("prefGamepadTestEnforceTesting", thisEnabled);
        enableDisablePreference("prefGamepadTestNow", thisEnabled);

    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@RequestCodes int requestCode) {
        if (!PermissionsHelper.getInstance().isPermissionGranted(preferences.this, requestCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(preferences.this, requestCode);
            }
        } else {
            // Go to the correct handler based on the request code.
            // Only need to consider relevant request codes initiated by this Activity
            switch (requestCode) {
                case PermissionsHelper.STORE_PREFERENCES:
                    Log.d("Engine_Driver", "Preferences: Got permission for STORE_PREFERENCES - navigate to saveSharedPreferencesToFileImpl()");
//                    saveSharedPreferencesToFileImpl();
                    break;
                case PermissionsHelper.READ_PREFERENCES:
                    Log.d("Engine_Driver", "Preferences: Got permission for READ_PREFERENCES - navigate to loadSharedPreferencesFromFileImpl()");
//                    loadSharedPreferencesFromFileImpl();
                    break;
                default:
                    // do nothing
                    Log.d("Engine_Driver", "Preferences: Unrecognised permissions request code: " + requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(preferences.this, requestCode, permissions, grantResults)) {
            Log.d("Engine_Driver", "Preferences: Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    //Handle messages from the communication thread back to the UI thread.
    // currently only for the download from a URL
    @SuppressLint("HandlerLeak")
    private class preferences_handler extends Handler {

        @SuppressLint("ApplySharedPref")
        public void handleMessage(Message msg) {
//            SharedPreferences sharedPreferences = getSharedPreferences("jmri.enginedriver_preferences", 0);
            switch (msg.what) {
                case message_type.IMPORT_SERVER_MANUAL_SUCCESS:
                    Log.d("Engine_Driver", "Preferences: Message: Import preferences from Server: File Found");
                    loadSharedPreferencesFromFile(sharedPreferences, EXTERNAL_URL_PREFERENCES_IMPORT, deviceId, FORCED_RESTART_REASON_IMPORT_SERVER_MANUAL);
                    break;
                case message_type.IMPORT_SERVER_MANUAL_FAIL:
                    Log.d("Engine_Driver", "Preferences: Message: Import preferences from Server: File not Found");
                    sharedPreferences.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
                    sharedPreferences.edit().putString("prefHostImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesImportServerManualFailed, sharedPreferences.getString("prefImportServerManual", getApplicationContext().getResources().getString(R.string.prefImportServerManualDefaultValue))), Toast.LENGTH_LONG).show();
                    reload();
                    break;

            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) { // dialog for the progress bar
        //noinspection SwitchStatementWithTooFewBranches
        switch (id) {
            case PROGRESS_BAR_TYPE: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    class importFromURL extends AsyncTask<String, String, String> {   // Background Async Task to download file

        //Before starting background thread Show Progress Bar Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(PROGRESS_BAR_TYPE);
        }

        // Importing file in background thread
        @SuppressLint("ApplySharedPref")
        @Override
        protected String doInBackground(String... f_url) {
            Log.d("Engine_Driver", "Preferences: Import preferences from Server: start");
            int count;
            String n_url = f_url[0].trim();

            if ((mainapp.connectedHostip != null)) {
                n_url = "http://" + mainapp.connectedHostip + ":" + mainapp.web_server_port + "/" + SERVER_ENGINE_DRIVER_DIR + "/" + f_url[0];
            } else {
                Log.d("Engine_Driver", "Preferences: Import preferences from Server: Not currently connected");
                return null;
            }

            try {
                URL url = new URL(n_url);
                URLConnection conection = url.openConnection();
                conection.connect();

                // to help show a 0-100% progress bar
                int lengthOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                File Directory = new File(ENGINE_DRIVER_DIR); // in case the folder does not already exist


                // Output stream
                FileOutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory().toString()
                        + "/" + ENGINE_DRIVER_DIR + "/" + EXTERNAL_URL_PREFERENCES_IMPORT);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

                dismissDialog(PROGRESS_BAR_TYPE);
                sharedPreferences.edit().putString("prefPreferencesImportFileName", n_url).commit();
                mainapp.sendMsgDelay(mainapp.preferences_msg_handler, 1000L, message_type.IMPORT_SERVER_MANUAL_SUCCESS);

            } catch (Exception e) {
                Log.e("Engine_Driver", "Import preferences from Server Failed: " + e.getMessage());
                dismissDialog(PROGRESS_BAR_TYPE);
                sharedPreferences.edit().putString("prefPreferencesImportAll", PREF_IMPORT_ALL_RESET).commit();
                mainapp.sendMsgDelay(mainapp.preferences_msg_handler, 1000L, message_type.IMPORT_SERVER_MANUAL_FAIL);
            }

            Log.d("Engine_Driver", "Preferences: Import preferences from Server: End");
            return null;
        }

        protected void onProgressUpdate(String... progress) {  //update progress bar
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(PROGRESS_BAR_TYPE);

        }

    }

}
