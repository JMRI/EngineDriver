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
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Random;

public class preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    static public final int RESULT_GAMEPAD = RESULT_FIRST_USER;

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu PRMenu;
    private int result;                     // set to RESULT_FIRST_USER when something is edited

    private boolean currentlyImporting = false;
    private static String exportedPreferencesFileName =  "exported_preferences.ed";
    private boolean overwiteFile = false;

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
        if (mainapp.androidVersion < mainapp.minGamepadVersion) {
            getPreferenceScreen().findPreference("gamepad_preferences").setSelectable(false);
            getPreferenceScreen().findPreference("gamepad_preferences").setEnabled(false);
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
                //limit new value to 0-100 (%)
                limitIntPrefValue(sharedPreferences, key, 0, 100, "100");
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
                if (!currentlyImporting) {
                    String currentValue = sharedPreferences.getString(key, "");
                    if (currentValue.equals("Export")) {
                        saveSharedPreferencesToFile(sharedPreferences);
                    } else if (currentValue.equals("Import")) {
                        loadSharedPreferencesFromFile(sharedPreferences);
                    } else if (currentValue.equals("Reset")) {
                        resetPreferences(sharedPreferences);
                    }
                    sharedPreferences.edit().putString(key, "None").commit();  //reset the preference
                }

                break;
        }
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
    }

    private boolean writeExportFile(SharedPreferences sharedPreferences, File dst){
        boolean res = false;
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            output.writeObject(sharedPreferences.getAll());

            Toast.makeText(getApplicationContext(), "Export to 'engine_driver/" + exportedPreferencesFileName + "' succeseded.", Toast.LENGTH_SHORT).show();
            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (!res) {
            Toast.makeText(getApplicationContext(), "Export failed!", Toast.LENGTH_LONG).show();
        }
        sharedPreferences.edit().putString("prefImportExport", "None").commit();  //reset the preference
        reload();
        return res;
    }

    public boolean overwiteFileDialog(SharedPreferences sharedPreferences, File dst) {
        boolean res = false;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        writeExportFile(sharedPreferences, dst);
                        overwiteFile = true;
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        overwiteFile = false;
                        break;
                }
            }
        };
        AlertDialog.Builder ab = new AlertDialog.Builder(preferences.this);
        ab.setMessage("File already exists. Overwite?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener);
        ab.show();
        return overwiteFile;
    }

    private boolean saveSharedPreferencesToFile(SharedPreferences sharedPreferences) {
        boolean res = false;

        File path = Environment.getExternalStorageDirectory();
        File engine_driver_dir = new File(path, "engine_driver");
        engine_driver_dir.mkdir();            // create directory if it doesn't exist

        File dst = new File(path, "engine_driver/"+exportedPreferencesFileName);

        if(dst.exists()) {
            overwiteFileDialog(sharedPreferences, dst);
        } else {
            res = writeExportFile(sharedPreferences, dst);
        }
        return res;
    }

    @SuppressWarnings({ "unchecked" })
    private boolean loadSharedPreferencesFromFile(SharedPreferences sharedPreferences) {
        currentlyImporting = true;
        boolean res = false;

        // save the current throttle name so that we can set it back after the restore is done
        String currentThrottleNameValue = sharedPreferences.getString("throttle_name_preference", getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue)).trim();

        File path = Environment.getExternalStorageDirectory();
        File engine_driver_dir = new File(path, "engine_driver");

        File src = new File(path, "engine_driver/"+exportedPreferencesFileName);

        if(src.exists()) {
                ObjectInputStream input = null;
            try {
                input = new ObjectInputStream(new FileInputStream(src));
                SharedPreferences.Editor prefEdit = sharedPreferences.edit();
                prefEdit.clear();

                Map<String, ?> entries = (Map<String, ?>) input.readObject();
                for (Map.Entry<String, ?> entry : entries.entrySet()) {
                    Object v = entry.getValue();
                    String key = entry.getKey();

                    if (v instanceof Boolean) prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                    else if (v instanceof Float) prefEdit.putFloat(key, ((Float) v).floatValue());
                    else if (v instanceof Integer) prefEdit.putInt(key, ((Integer) v).intValue());
                    else if (v instanceof Long) prefEdit.putLong(key, ((Long) v).longValue());
                    else if (v instanceof String) prefEdit.putString(key, ((String) v));
                }
                prefEdit.commit();
                res = true;

                // restore the remembered throttle name to avoid a duplicate throttle name
                sharedPreferences.edit().putString("throttle_name_preference", currentThrottleNameValue).commit();

                Toast.makeText(getApplicationContext(), "Import from 'engine_driver/"+exportedPreferencesFileName+"' succeseded.", Toast.LENGTH_SHORT).show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            currentlyImporting = false;
        }
        if (!res) {
            Toast.makeText(getApplicationContext(), "Import from 'engine_driver/"+exportedPreferencesFileName+"' failed!", Toast.LENGTH_LONG).show();
        } else {
            reload();
        }
        return res;
    }

    static void putObject(SharedPreferences sharedPreferences, final String key, final Object val) {
        if (val instanceof Boolean)
            sharedPreferences.edit().putBoolean(key, ((Boolean)val).booleanValue()).commit();
        else if (val instanceof Float)
            sharedPreferences.edit().putFloat(key, ((Float)val).floatValue()).commit();
        else if (val instanceof Integer)
            sharedPreferences.edit().putInt(key, ((Integer)val).intValue()).commit();
        else if (val instanceof Long)
            sharedPreferences.edit().putLong(key, ((Long)val).longValue()).commit();
        else if (val instanceof String)
            sharedPreferences.edit().putString(key, ((String)val)).commit();

    }

    private boolean limitIntPrefValue(SharedPreferences sharedPreferences, String key, int minVal, int maxVal, String defaultVal) {
        boolean isValid = true;
        try {
            int newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Integer.toString(maxVal)).commit();
                isValid = false;
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Integer.toString(minVal)).commit();
                isValid = false;
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            isValid = false;
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
