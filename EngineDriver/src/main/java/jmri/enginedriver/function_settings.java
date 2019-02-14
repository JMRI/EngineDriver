/* Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;

import java.util.ArrayList;
import java.io.*;

import android.text.method.TextKeyListener;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;

@SuppressLint("ApplySharedPref")
public class function_settings extends Activity implements PermissionsHelper.PermissionsHelperGrantedCallback {

    private threaded_application mainapp;
    private boolean orientationChange = false;

    //set up label, dcc function, toggle setting for each button
    private static boolean settingsCurrent = false;
    private static ArrayList<String> aLbl = new ArrayList<>();
    private static ArrayList<Integer> aFnc = new ArrayList<>();
    private Menu FMenu;
    private EditText et;
    private EditText etForRoster;
    private String prefNumberOfDefaultFunctionLabels = "29";
    private String originalPrefNumberOfDefaultFunctionLabels = "29";
    private String prefNumberOfDefaultFunctionLabelsForRoster = "4";
    private String originalPrefNumberOfDefaultFunctionLabelsForRoster = "4";
    private boolean prefAlwaysUseDefaultFunctionLabels = false;
    private Spinner spinner;

    SharedPreferences prefs;

    public void setTitleToIncludeThrotName() {
        String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_functions) + "    |    Throttle Name: " +
                prefs.getString("throttle_name_preference", defaultName));
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();  //save pointer to main app

        //setTitleToIncludeThrotName();

        mainapp.applyTheme(this);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_functions)); // needed in case the langauge was changed from the default

        setContentView(R.layout.function_settings);
        orientationChange = false;

        if (savedInstanceState == null) {    //if not an orientation change then init settings array
            initSettings();
            settingsCurrent = true;
        }

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        prefNumberOfDefaultFunctionLabels = prefs.getString("prefNumberOfDefaultFunctionLabels", getApplicationContext().getResources().getString(R.string.prefNumberOfDefaultFunctionLabelsDefaultValue));
        prefNumberOfDefaultFunctionLabelsForRoster = prefs.getString("prefNumberOfDefaultFunctionLabelsForRoster", getApplicationContext().getResources().getString(R.string.prefNumberOfDefaultFunctionLabelsForRosterDefaultValue));
        originalPrefNumberOfDefaultFunctionLabels = prefNumberOfDefaultFunctionLabels;
        originalPrefNumberOfDefaultFunctionLabelsForRoster = prefNumberOfDefaultFunctionLabelsForRoster;
        prefAlwaysUseDefaultFunctionLabels = prefs.getBoolean("prefAlwaysUseDefaultFunctionLabels", getResources().getBoolean(R.bool.prefAlwaysUseDefaultFunctionLabelsDefaultValue));

        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Button b = findViewById(R.id.fb_copy_labels_from_roster);
        if (mainapp.function_labels[0] == null || mainapp.function_labels[0].size() == 0) {
            b.setEnabled(false);  //disable button if no roster
        } else {
            //Set the button callback.
            button_listener click_listener = new button_listener();
            b.setOnClickListener(click_listener);
            b.setEnabled(true);
        }

        Button bReset = findViewById(R.id.fb_reset_function_labels);
        reset_button_listener reset_click_listener = new reset_button_listener();
        bReset.setOnClickListener(reset_click_listener);
        bReset.setEnabled(true);

        et = findViewById(R.id.fb_number_of_default_function_labels);
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        etForRoster = findViewById(R.id.fb_number_of_default_function_labels_for_roster);
        etForRoster.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        spinner = findViewById(R.id.fb_always_use_default_function_labels);
        spinner.setOnItemSelectedListener(new spinner_listener());

        mainapp.set_default_function_labels(true);
        move_settings_to_view();            //copy settings array to view

        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //warn user that saving Default Function Settings requires SD Card
            TextView v = findViewById(R.id.fs_heading);
            v.setText(getString(R.string.fs_edit_notice));
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        if (FMenu != null) {
            mainapp.displayEStop(FMenu);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {     //orientation change
        move_view_to_settings();        //update settings array so onCreate can use it to initialize
        orientationChange = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.isFinishing()) {       //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    @Override
    public void onDestroy() {
        mainapp.set_default_function_labels(false); // reload the preference in cases the display number is less than the total number

        Log.d("Engine_Driver", "function_settings.onDestroy() called");
        if (!orientationChange) {
            aLbl.clear();
            aFnc.clear();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.function_settings_menu, menu);
        FMenu = menu;
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

    //build the arrays from the function_settings file
    //function_labels_default was loaded from settings file by TA
    //(and updated by saveSettings() when required) so just copy it
    void initSettings() {
        navigateToHandler(PermissionsHelper.READ_FUNCTION_SETTINGS);
    }

    private void initSettingsImpl() {
        mainapp.set_default_function_labels(true);

        aLbl.clear();
        aFnc.clear();
        //read settings into local arrays
        for (Integer f : mainapp.function_labels_default.keySet()) {
            aFnc.add(f);
            aLbl.add(mainapp.function_labels_default.get(f));
        }
    }

    //take data from arrays and update the editing view
    void move_settings_to_view() {
        ViewGroup t = findViewById(R.id.label_func_table); //table
        //loop thru input rows, skipping first (headings)
        int ndx = 0;
        for (int i = 1; i < t.getChildCount(); i++) {
            ViewGroup r = (ViewGroup) t.getChildAt(i);
            //move to next non-blank array entry if it exists
            while (ndx < aFnc.size() && aLbl.get(ndx).length() == 0)
                ndx++;
            if (ndx < aFnc.size()) {
                ((EditText) r.getChildAt(0)).setText(aLbl.get(ndx));
                ((EditText) r.getChildAt(1)).setText(aFnc.get(ndx).toString());
                ndx++;
            } else {
                //          
                // work around for known EditText bug - see http://code.google.com/p/android/issues/detail?id=17508
                //          ((EditText)r.getChildAt(0)).setText("");
                //          ((EditText)r.getChildAt(1)).setText("");
                TextKeyListener.clear(((EditText) r.getChildAt(0)).getText());
                TextKeyListener.clear(((EditText) r.getChildAt(1)).getText());
            }
        }

        if (prefAlwaysUseDefaultFunctionLabels) {
            spinner.setSelection(0);
        } else {
            spinner.setSelection(1);
        }

        et.setText(prefNumberOfDefaultFunctionLabels);
        etForRoster.setText(prefNumberOfDefaultFunctionLabelsForRoster);
    }

    //Save the valid function labels in the settings array
    void move_view_to_settings() {
        ViewGroup t = findViewById(R.id.label_func_table); //table
        ViewGroup r;  //row
        //loop thru each row, Skipping the first one (the headings)  format is "label:function#"
        int ndx = 0;
        for (int i = 1; i < t.getChildCount(); i++) {
            r = (ViewGroup) t.getChildAt(i);
            //get the 2 inputs from each row
            String label = ((EditText) r.getChildAt(0)).getText().toString();
            label = label.replace("\n", " ");  //remove newlines
            label = label.replace(":", " ");   //   and colons, as they confuse the save format
            String sfunc = ((EditText) r.getChildAt(1)).getText().toString();
            if (label.length() > 0 && sfunc.length() > 0) {
                //verify function is valid number between 0 and 28
                int func;
                try {
                    func = Integer.parseInt(sfunc);
                    if (func >= 0 && func <= 28) {
                        if (aFnc.size() <= ndx) {
                            aLbl.add(label);
                            aFnc.add(func);
                            settingsCurrent = false;
                        } else if (!label.equals(aLbl.get(ndx)) || func != aFnc.get(ndx)) {
                            aLbl.set(ndx, label);
                            aFnc.set(ndx, func);
                            settingsCurrent = false;
                        }
                        ndx++;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        while (aFnc.size() > ndx) {          //if array remains then trim it
            aFnc.remove(ndx);
            aLbl.remove(ndx);
            settingsCurrent = false;
        }
    }

    //replace arrays using data from roster entry (called by button)
    void move_roster_to_settings() {
        int ndx = 0;
        for (Integer func : mainapp.function_labels[0].keySet()) {
            String label = mainapp.function_labels[0].get(func);
            if (label.length() > 0 && func >= 0 && func <= 28) {
                if (aFnc.size() <= ndx) {
                    aLbl.add(label);
                    aFnc.add(func);
                    settingsCurrent = false;
                } else if (!label.equals(aLbl.get(ndx)) || !func.equals(aFnc.get(ndx))) {
                    aLbl.set(ndx, label);
                    aFnc.set(ndx, func);
                    settingsCurrent = false;
                }
                ndx++;
            }
        }

        while (aFnc.size() > ndx) {          //if array remains then trim it
            aFnc.remove(ndx);
            aLbl.remove(ndx);
            settingsCurrent = false;
        }
    }

    public class button_listener implements View.OnClickListener {
        public void onClick(View v) {
            move_roster_to_settings();
            move_settings_to_view();
        }
    }

    public class reset_button_listener implements View.OnClickListener {
        public void onClick(View v) {
//            SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
            String label = "";
            int func = 0;

            for (int i = 0; i <= 28; i++) {
                if (i==0) label = getResources().getString(R.string.functionButton00DefaultValue);
                if (i==1) label = getResources().getString(R.string.functionButton01DefaultValue);
                if (i==2) label = getResources().getString(R.string.functionButton02DefaultValue);
                if(i>=3) {
                    label = ""+i;
                }
                func = i;
                if (aFnc.size() <= i) {
                    aLbl.add(label);
                    aFnc.add(func);
                    settingsCurrent = false;
                } else {
                    aLbl.set(i, label);
                    aFnc.set(i, func);
                    settingsCurrent = false;
                }
            }

            prefAlwaysUseDefaultFunctionLabels = false;
            prefNumberOfDefaultFunctionLabels = "29";
            prefNumberOfDefaultFunctionLabelsForRoster = "4";
            move_settings_to_view();
        }
    }

    //Handle pressing of the back button to save settings
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        prefNumberOfDefaultFunctionLabels = limitIntEditValue("prefNumberOfDefaultFunctionLabels", et, 0, 29, "29");
        prefNumberOfDefaultFunctionLabelsForRoster = limitIntEditValue("prefNumberOfDefaultFunctionLabelsForRoster", etForRoster, 0, 29, "4");

        if (key == KeyEvent.KEYCODE_BACK) {
            move_view_to_settings();        //sync settings array to view
            if ( (!settingsCurrent)
                    || (!originalPrefNumberOfDefaultFunctionLabels.equals(prefNumberOfDefaultFunctionLabels))
                    || (!originalPrefNumberOfDefaultFunctionLabelsForRoster.equals(prefNumberOfDefaultFunctionLabelsForRoster)))  //if settings array is not current
                saveSettings();         //save function labels to file
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    void saveSettings() {
        saveSettingsImpl();
    }

    //save function and labels to file
    void saveSettingsImpl() {
        //SD Card required to save settings
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;
        //Save the valid function labels to the settings.txt file.
        File sdcard_path = Environment.getExternalStorageDirectory();
        File settings_file = new File(sdcard_path, "engine_driver/function_settings.txt");
        PrintWriter settings_output;
        String errMsg = "";
        try {
            settings_output = new PrintWriter(settings_file);
            mainapp.function_labels_default.clear();
            for (int i = 0; i < aFnc.size(); i++) {
                String label = aLbl.get(i);
                if (label.length() > 0) {
                    Integer fnc = aFnc.get(i);
                    settings_output.format("%s:%s\n", label, fnc);
                    mainapp.function_labels_default.put(fnc, label);
                }
            }
            settings_output.flush();
            settings_output.close();
        } catch (IOException except) {
            errMsg = except.getMessage();
            Log.e("settings_activity", "Error creating a PrintWriter, IOException: " + errMsg);
        }
        if (errMsg.length() != 0)
            Toast.makeText(getApplicationContext(), "Save Settings Failed." + errMsg, Toast.LENGTH_LONG).show();
        else
            Toast.makeText(getApplicationContext(), "Settings Saved.", Toast.LENGTH_SHORT).show();

        prefs.edit().putString("prefNumberOfDefaultFunctionLabels", prefNumberOfDefaultFunctionLabels).commit();  //reset the preference
        prefs.edit().putString("prefNumberOfDefaultFunctionLabelsForRoster", prefNumberOfDefaultFunctionLabelsForRoster).commit();  //reset the preference
        prefs.edit().putBoolean("prefAlwaysUseDefaultFunctionLabels", prefAlwaysUseDefaultFunctionLabels).commit();
    }

    @SuppressWarnings("deprecation")
    private String limitIntEditValue(String key, EditText et, int minVal, int maxVal, String defaultVal) {
        String sVal = defaultVal;
        boolean isValid = true;
        int newVal = maxVal;
        try {
            newVal = Integer.parseInt(et.getText().toString().trim());
            if (newVal > maxVal) {
                prefs.edit().putString(key, Integer.toString(maxVal)).commit();
                sVal = Integer.toString(maxVal);
                et.setText(sVal);
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Float.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                prefs.edit().putString(key, Integer.toString(minVal)).commit();
                sVal = Integer.toString(minVal);
                et.setText(sVal);
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Float.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            prefs.edit().putString(key, defaultVal).commit();
            sVal = defaultVal;
            et.setText(sVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric, Integer.toString(minVal), Integer.toString(maxVal), defaultVal), Toast.LENGTH_LONG).show();
        }
        if (isValid) sVal = Integer.toString(newVal);
        return sVal;
    }

    public class spinner_listener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            int alwaysUseDefaultFunctionLabelsIndex = spinner.getSelectedItemPosition();

            prefAlwaysUseDefaultFunctionLabels = alwaysUseDefaultFunctionLabelsIndex == 0;
            prefs.edit().putBoolean("prefAlwaysUseDefaultFunctionLabels", prefAlwaysUseDefaultFunctionLabels).commit();  //reset the preference

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

    // needed in case the langauge was changed from the default
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public void navigateToHandler(@RequestCodes int requestCode) {
        if (!PermissionsHelper.getInstance().isPermissionGranted(function_settings.this, requestCode)) {
            PermissionsHelper.getInstance().requestNecessaryPermissions(function_settings.this, requestCode);
        } else {
            switch (requestCode) {
                case PermissionsHelper.STORE_FUNCTION_SETTINGS:
                    Log.d("Engine_Driver", "Got permission for STORE_FUNCTION_SETTINGS - navigate to saveSettingsImpl()");
                    saveSettingsImpl();
                    break;
                case PermissionsHelper.READ_FUNCTION_SETTINGS:
                    Log.d("Engine_Driver", "Got permission for READ_FUNCTION_SETTINGS - navigate to initSettingsImpl()");
                    initSettingsImpl();
                    break;
                default:
                    // do nothing
                    Log.d("Engine_Driver", "Unrecognised permissions request code: " + requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(function_settings.this, requestCode, permissions, grantResults)) {
            Log.d("Engine_Driver", "Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
