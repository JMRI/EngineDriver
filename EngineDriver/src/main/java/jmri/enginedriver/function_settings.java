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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static jmri.enginedriver.threaded_application.MAX_FUNCTIONS;
import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.util.LocaleHelper;

@SuppressLint("ApplySharedPref")
public class function_settings extends AppCompatActivity implements PermissionsHelper.PermissionsHelperGrantedCallback {

    private threaded_application mainapp;
    private boolean orientationChange = false;

    //set up label, dcc function, toggle setting for each button
    private static boolean settingsCurrent = false;
    private static final ArrayList<String> aLbl = new ArrayList<>();
    private static final ArrayList<Integer> aFnc = new ArrayList<>();
    private Menu FMenu;
    private EditText et;
    private EditText etForRoster;
    private String prefNumberOfDefaultFunctionLabels = threaded_application.MAX_FUNCTIONS_TEXT;
    private String originalPrefNumberOfDefaultFunctionLabels = threaded_application.MAX_FUNCTIONS_TEXT;
    private String prefNumberOfDefaultFunctionLabelsForRoster = "4";
    private String originalPrefNumberOfDefaultFunctionLabelsForRoster = "4";
    private Spinner alwaysUseDefaultFunctionLabelsSspinner;
    private Spinner overrideWiThrottlesFunctionLatchingSpinner;
    private TextView overrideWiThrottlesFunctionLatchingLabel;
    private Spinner overrideRosterWithNoFunctionLabelsSpinner;

    SharedPreferences prefs;

    /** @noinspection FieldCanBeLocal*/
    private LinearLayout screenNameLine;
    /** @noinspection FieldCanBeLocal*/
    private Toolbar toolbar;
    /** @noinspection FieldCanBeLocal*/
    private LinearLayout statusLine;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();  //save pointer to main app

        //setTitleToIncludeThrotName();

        mainapp.applyTheme(this);

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
        mainapp.prefAlwaysUseDefaultFunctionLabels = prefs.getBoolean("prefAlwaysUseDefaultFunctionLabels", getResources().getBoolean(R.bool.prefAlwaysUseDefaultFunctionLabelsDefaultValue));
        mainapp.prefOverrideWiThrottlesFunctionLatching = prefs.getBoolean("prefOverrideWiThrottlesFunctionLatching", getResources().getBoolean(R.bool.prefOverrideWiThrottlesFunctionLatchingDefaultValue));
        mainapp.prefOverrideRosterWithNoFunctionLabels = prefs.getBoolean("prefOverrideRosterWithNoFunctionLabels", getResources().getBoolean(R.bool.prefOverrideRosterWithNoFunctionLabelsDefaultValue));

        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Button b = findViewById(R.id.fb_copy_labels_from_roster);
        if (mainapp.function_labels == null || mainapp.function_labels[0] == null || mainapp.function_labels[0].size() == 0) {
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

        Button closeButton = findViewById(R.id.fb_button_close);
        close_button_listener closeClickListener = new close_button_listener();
        closeButton.setOnClickListener(closeClickListener);

        et = findViewById(R.id.fb_number_of_default_function_labels);
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                settingsCurrent = false;
            }
        });

        etForRoster = findViewById(R.id.fb_number_of_default_function_labels_for_roster);
        etForRoster.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                settingsCurrent = false;
            }
        });

        alwaysUseDefaultFunctionLabelsSspinner = findViewById(R.id.fb_always_use_default_function_labels);
        alwaysUseDefaultFunctionLabelsSspinner.setOnItemSelectedListener(new AlwaysUseDefaultFunctionLabelsSpinnerListener());

        overrideWiThrottlesFunctionLatchingSpinner = findViewById(R.id.fb_override_withrottles_function_latching);
        overrideWiThrottlesFunctionLatchingSpinner.setOnItemSelectedListener(new OverrideWithrottlesFunctionLatchingSpinnerListener());
        overrideRosterWithNoFunctionLabelsSpinner = findViewById(R.id.fb_override_roster_with_no_function_labels);
        overrideRosterWithNoFunctionLabelsSpinner.setOnItemSelectedListener(new OverrideRosterWithNoFunctionLabelsSpinnerListener());

        findViewById(R.id.fb_override_withrottles_function_latching_label).setEnabled(!mainapp.isDCCEX);
//        overrideWiThrottlesFunctionLatchingSpinner.setEnabled(!mainapp.isDCCEX);
        overrideWiThrottlesFunctionLatchingSpinner.setVisibility(mainapp.isDCCEX ? GONE : VISIBLE);

        overrideWiThrottlesFunctionLatchingLabel = findViewById(R.id.fb_override_withrottles_function_latching_label);
        overrideWiThrottlesFunctionLatchingLabel.setVisibility(mainapp.isDCCEX ? GONE : VISIBLE);

        mainapp.set_default_function_labels(true);
        move_settings_to_view();            //copy settings array to view

        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //warn user that saving Default Function Settings requires SD Card
            TextView v = findViewById(R.id.fs_heading);
            v.setText(getString(R.string.fs_edit_notice));
        }

        //put pointer to this activity's handler in main app's shared variable
        mainapp.function_settings_msg_handler = new function_settings_handler(Looper.getMainLooper());

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = (LinearLayout) findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_functions),
                    "");
        }

    } // end onCreate

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        if (FMenu != null) {
            mainapp.displayEStop(FMenu);
            mainapp.displayFlashlightMenuButton(FMenu);
            mainapp.setFlashlightButton(FMenu);
            mainapp.displayPowerStateMenuButton(FMenu);
            mainapp.setPowerStateButton(FMenu);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onSaveInstanceState(Bundle saveState) {     //orientation change
        move_view_to_settings();        //update settings array so onCreate can use it to initialize
        orientationChange = true;
    }

    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "function_Settings.onDestroy() called");
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
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightButton(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.EmerStop) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlight(this, FMenu);
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(FMenu);
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
    class function_settings_handler extends Handler {

        public function_settings_handler(Looper looper) {
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
                            mainapp.setPowerStateButton(FMenu);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    //build the arrays from the function_settings file
    //function_labels_default was loaded from settings file by TA
    //(and updated by saveSettings() when required) so just copy it
    void initSettings() {
//        navigateToHandler(PermissionsHelper.READ_FUNCTION_SETTINGS);
//        initSettingsImpl();
//    }
//
//    private void initSettingsImpl() {
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
    @SuppressLint("SetTextI18n")
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

        if (mainapp.prefAlwaysUseDefaultFunctionLabels) {
            alwaysUseDefaultFunctionLabelsSspinner.setSelection(0);
        } else {
            alwaysUseDefaultFunctionLabelsSspinner.setSelection(1);
        }

        if (mainapp.prefOverrideWiThrottlesFunctionLatching) {
            overrideWiThrottlesFunctionLatchingSpinner.setSelection(0);
        } else {
            overrideWiThrottlesFunctionLatchingSpinner.setSelection(1);
        }

        if (mainapp.prefOverrideRosterWithNoFunctionLabels) {
            overrideRosterWithNoFunctionLabelsSpinner.setSelection(0);
        } else {
            overrideRosterWithNoFunctionLabelsSpinner.setSelection(1);
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
                //verify function is valid number between 0 and 31
                int func;
                try {
                    func = Integer.parseInt(sfunc);
                    if (func >= 0 && func < MAX_FUNCTIONS) {
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
            if (label.length() > 0 && func >= 0 && func < MAX_FUNCTIONS) {
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
            mainapp.buttonVibration();
        }
    }

    public class reset_button_listener implements View.OnClickListener {
        public void onClick(View v) {
//            SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
            String label = "";
            int func = 0;

            for (int i = 0; i < MAX_FUNCTIONS; i++) {
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

            mainapp.prefAlwaysUseDefaultFunctionLabels = false;
            mainapp.prefOverrideWiThrottlesFunctionLatching = false;
            mainapp.prefOverrideRosterWithNoFunctionLabels = false;
            prefNumberOfDefaultFunctionLabels = threaded_application.MAX_FUNCTIONS_TEXT;
            prefNumberOfDefaultFunctionLabelsForRoster = "4";
            move_settings_to_view();
            mainapp.buttonVibration();
        }
    }

    //Handle pressing of the back button to save settings
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        prefNumberOfDefaultFunctionLabels = limitIntEditValue("prefNumberOfDefaultFunctionLabels", et, 0, MAX_FUNCTIONS, threaded_application.MAX_FUNCTIONS_TEXT);
        prefNumberOfDefaultFunctionLabelsForRoster = limitIntEditValue("prefNumberOfDefaultFunctionLabelsForRoster", etForRoster, 0, MAX_FUNCTIONS, "4");

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
//        navigateToHandler(PermissionsHelper.STORE_FUNCTION_SETTINGS);
//        saveSettingsImpl();
//    }
//
//    //save function and labels to file
//    void saveSettingsImpl() {
        //SD Card required to save settings
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;
        //Save the valid function labels to the settings.txt file.
//        File sdcard_path = Environment.getExternalStorageDirectory();
//        File settings_file = new File(sdcard_path, "engine_driver/function_settings.txt");
        File settings_file = new File(context.getExternalFilesDir(null), "function_settings.txt");
        PrintWriter settings_output;
        String errMsg = "";
        try {
            settings_output = new PrintWriter(settings_file);
            mainapp.function_labels_default.clear();
            for (int i = 0; i < aFnc.size(); i++) {
                String label = aLbl.get(i);
                if (label.length() > 0) {
                    Integer fnc = aFnc.get(i);
                    String locos = mainapp.function_consist_locos.get(i);
                    String latching = mainapp.function_consist_latching.get(i);
                    settings_output.format("%s:%s:%s:%s\n", label, fnc, locos, latching);
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
//            Toast.makeText(getApplicationContext(), "Save Settings Failed." + errMsg, Toast.LENGTH_LONG).show();
            threaded_application.safeToast("Save Settings Failed." + errMsg, Toast.LENGTH_LONG);
        else
//            Toast.makeText(getApplicationContext(), "Settings Saved.", Toast.LENGTH_SHORT).show();
            threaded_application.safeToast("Settings Saved.", Toast.LENGTH_SHORT);

        prefs.edit().putString("prefNumberOfDefaultFunctionLabels", prefNumberOfDefaultFunctionLabels).commit();  //reset the preference
        prefs.edit().putString("prefNumberOfDefaultFunctionLabelsForRoster", prefNumberOfDefaultFunctionLabelsForRoster).commit();  //reset the preference
        prefs.edit().putBoolean("prefAlwaysUseDefaultFunctionLabels", mainapp.prefAlwaysUseDefaultFunctionLabels).commit();
        prefs.edit().putBoolean("prefOverrideWiThrottlesFunctionLatching", mainapp.prefOverrideWiThrottlesFunctionLatching).commit();
        prefs.edit().putBoolean("prefOverrideRosterWithNoFunctionLabels", mainapp.prefOverrideRosterWithNoFunctionLabels).commit();
    }

    /** @noinspection SameParameterValue, SameParameterValue */
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
//                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Float.toString(maxVal)), Toast.LENGTH_LONG).show();
                threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits,
                                                Integer.toString(minVal), Integer.toString(maxVal), Float.toString(maxVal)), Toast.LENGTH_LONG);
            } else if (newVal < minVal) {
                prefs.edit().putString(key, Integer.toString(minVal)).commit();
                sVal = Integer.toString(minVal);
                et.setText(sVal);
                isValid = false;
//                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Float.toString(minVal)), Toast.LENGTH_LONG).show();
                threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits,
                                        Integer.toString(minVal), Integer.toString(maxVal), Float.toString(minVal)), Toast.LENGTH_LONG);
            }
        } catch (NumberFormatException e) {
            prefs.edit().putString(key, defaultVal).commit();
            sVal = defaultVal;
            et.setText(sVal);
            isValid = false;
//            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric, Integer.toString(minVal), Integer.toString(maxVal), defaultVal), Toast.LENGTH_LONG).show();
            threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric,
                                        Integer.toString(minVal), Integer.toString(maxVal), defaultVal), Toast.LENGTH_LONG);
        }
        if (isValid) sVal = Integer.toString(newVal);
        return sVal;
    }

    public class AlwaysUseDefaultFunctionLabelsSpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            int alwaysUseDefaultFunctionLabelsIndex = alwaysUseDefaultFunctionLabelsSspinner.getSelectedItemPosition();

            mainapp.prefAlwaysUseDefaultFunctionLabels = alwaysUseDefaultFunctionLabelsIndex == 0;
            prefs.edit().putBoolean("prefAlwaysUseDefaultFunctionLabels", mainapp.prefAlwaysUseDefaultFunctionLabels).commit();  //reset the preference

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
    public class OverrideWithrottlesFunctionLatchingSpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            int overrideWithrottlesFunctionLatchingSpinnerListenerIndex =  overrideWiThrottlesFunctionLatchingSpinner.getSelectedItemPosition();

            mainapp.prefOverrideWiThrottlesFunctionLatching = overrideWithrottlesFunctionLatchingSpinnerListenerIndex == 0;
            prefs.edit().putBoolean("prefOverrideWiThrottlesFunctionLatching", mainapp.prefOverrideWiThrottlesFunctionLatching).commit();  //reset the preference

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

    public class OverrideRosterWithNoFunctionLabelsSpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            int overrideRosterWithNoFunctionLabelsSpinnerListenerIndex =  overrideRosterWithNoFunctionLabelsSpinner.getSelectedItemPosition();

            mainapp.prefOverrideRosterWithNoFunctionLabels = overrideRosterWithNoFunctionLabelsSpinnerListenerIndex == 0;
            prefs.edit().putBoolean("prefOverrideRosterWithNoFunctionLabels", mainapp.prefOverrideRosterWithNoFunctionLabels).commit();  //reset the preference

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

    // needed in case the language was changed from the default
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@RequestCodes int requestCode) {
        Log.d("Engine_Driver", "function_settings: navigateToHandler:" + requestCode);
        if (!PermissionsHelper.getInstance().isPermissionGranted(function_settings.this, requestCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(function_settings.this, requestCode);
            }
        } else {
//            switch (requestCode) {
//                case PermissionsHelper.STORE_FUNCTION_SETTINGS:
//                    Log.d("Engine_Driver", "Got permission for STORE_FUNCTION_SETTINGS - navigate to saveSettingsImpl()");
//                    saveSettingsImpl();
//                    break;
//                case PermissionsHelper.READ_FUNCTION_SETTINGS:
//                    Log.d("Engine_Driver", "Got permission for READ_FUNCTION_SETTINGS - navigate to initSettingsImpl()");
//                    initSettingsImpl();
//                    break;
//                default:
                    // do nothing
                    Log.d("Engine_Driver", "Unrecognised permissions request code: " + requestCode);
//            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(function_settings.this, requestCode, permissions, grantResults)) {
            Log.d("Engine_Driver", "Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();

            prefNumberOfDefaultFunctionLabels = limitIntEditValue("prefNumberOfDefaultFunctionLabels", et, 0, MAX_FUNCTIONS, threaded_application.MAX_FUNCTIONS_TEXT);
            prefNumberOfDefaultFunctionLabelsForRoster = limitIntEditValue("prefNumberOfDefaultFunctionLabelsForRoster", etForRoster, 0, MAX_FUNCTIONS, "4");

            move_view_to_settings();        //sync settings array to view
            if ( (!settingsCurrent)
                    || (!originalPrefNumberOfDefaultFunctionLabels.equals(prefNumberOfDefaultFunctionLabels))
                    || (!originalPrefNumberOfDefaultFunctionLabelsForRoster.equals(prefNumberOfDefaultFunctionLabelsForRoster)))  //if settings array is not current
                saveSettings();         //save function labels to file
            finish();
        }
    }
}
