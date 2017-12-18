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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    static public final int RESULT_GAMEPAD = RESULT_FIRST_USER;

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu PRMenu;
    private int result;                     // set to RESULT_FIRST_USER when something is edited

    private boolean currentlyImporting = false;
    private String exportedPreferencesFileName =  "exported_preferences.ed";
    private boolean overwiteFile = false;

    private ArrayList<Integer> engine_address_list;
    private ArrayList<Integer> address_size_list; // Look at address_type.java
    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    /**
     * Called when the activity is first created.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
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
        result = RESULT_OK;
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
                limitIntPrefValue(sharedPreferences, key, 0, 100, "5");
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
            case "prefGamePadStartButton":
                result = RESULT_GAMEPAD;
                break;
            case "prefImportExport":
                //if (!currentlyImporting) {
                if (!importExportPreferences.currentlyImporting) {
                    String currentValue = sharedPreferences.getString(key, "");
                    if (currentValue.equals("Export")) {
                        saveSharedPreferencesToFile(sharedPreferences,exportedPreferencesFileName);
                    } else if (currentValue.equals("Import")) {
                        loadSharedPreferencesFromFile(sharedPreferences,exportedPreferencesFileName);
                    } else if (currentValue.equals("Reset")) {
                        resetPreferences(sharedPreferences);
                    }
                }
                break;
        }
    }

    @SuppressWarnings({ "unchecked" })
    private boolean loadSharedPreferencesFromFile(SharedPreferences sharedPreferences, String exportedPreferencesFileName) {
        boolean res = importExportPreferences.loadSharedPreferencesFromFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);

        if (!res) {
            Toast.makeText(getApplicationContext(), "Import from 'engine_driver/" + exportedPreferencesFileName + "' failed! You may not have saved the preferences for this host yet.", Toast.LENGTH_LONG).show();
        }
        resetAndReloadImportExportPreference(sharedPreferences);
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
        Toast.makeText(getApplicationContext(), "Preferences Reset to defaults!", Toast.LENGTH_LONG).show();
    }

    private void resetAndReloadImportExportPreference(SharedPreferences sharedPreferences){
        sharedPreferences.edit().putString("prefImportExport", "None").commit();  //reset the preference
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
                        //writeExportFile(sharedPreferences, dst);
                        res = importExportPreferences.saveSharedPreferencesToFile(mainapp.getApplicationContext(), sharedPreferences, exportedPreferencesFileName);
                        overwiteFile = true;
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        overwiteFile = false;
                        break;
                }
                resetAndReloadImportExportPreference(sharedPreferences);
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
                Toast.makeText(getApplicationContext(), "Value entered is outside the limits ("+Integer.toString(minVal)+"-"+Integer.toString(maxVal)+"). Reset to "+Integer.toString(maxVal)+".", Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Integer.toString(minVal)).commit();
                prefText.setText(Integer.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), "Value entered is outside the limits ("+Integer.toString(minVal)+"-"+Integer.toString(maxVal)+"). Reset to "+Integer.toString(minVal)+".", Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), "Value entered not numeric ("+Integer.toString(minVal)+"-"+Integer.toString(maxVal)+")! Reset to default.", Toast.LENGTH_LONG).show();
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

}
