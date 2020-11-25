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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import jmri.enginedriver.util.PermissionsHelper;
import jmri.enginedriver.util.PermissionsHelper.RequestCodes;

@SuppressLint("ApplySharedPref")
public class function_consist_settings extends Activity implements PermissionsHelper.PermissionsHelperGrantedCallback {

    private threaded_application mainapp;
    private boolean orientationChange = false;

    //set up label, dcc function, toggle setting for each button
    private static boolean settingsCurrent = false;
    private static ArrayList<String> aLbl = new ArrayList<>();
    private static ArrayList<Integer> aFnc = new ArrayList<>();
    private static ArrayList<String> aLocos = new ArrayList<>();
    private static ArrayList<String> aLatching = new ArrayList<>();
    private Menu FMenu;
    private EditText et;

    private static String[] LOCOS = {"lead", "lead and trail", "all","trail"};
    private static String[] LATCHING = {"latching", "none"};

    SharedPreferences prefs;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();  //save pointer to main app

        mainapp.applyTheme(this);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_functions)); // needed in case the langauge was changed from the default

        setContentView(R.layout.function_consist_settings);
        orientationChange = false;

        if (savedInstanceState == null) {    //if not an orientation change then init settings array
            initSettings();
            settingsCurrent = true;
        }

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Button bReset = findViewById(R.id.functionConsistReset);
        reset_button_listener reset_click_listener = new reset_button_listener();
        bReset.setOnClickListener(reset_click_listener);
        bReset.setEnabled(true);

        mainapp.set_default_function_labels(true);

        move_settings_to_view();            //copy settings array to view

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //warn user that saving Default Function Settings requires SD Card
            TextView v = findViewById(R.id.fs_heading);
            v.setText(getString(R.string.fs_edit_notice));
        }

    }

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
        }
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {     //orientation change
        move_view_to_settings();        //update settings array so onCreate can use it to initialize
        orientationChange = true;
    }

    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "function_consist_settings.onDestroy() called");
        mainapp.set_default_function_labels(false); // reload the preference in cases the display number is less than the total number

        Log.d("Engine_Driver", "function_consist_settings.onDestroy() called");
        if (!orientationChange) {
            aLbl.clear();
            aFnc.clear();
            aLocos.clear();
            aLatching.clear();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.function_consist_settings_menu, menu);
        FMenu = menu;
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        aLocos.clear();
        aLatching.clear();
        //read settings into local arrays
        for (Integer f : mainapp.function_labels_default.keySet()) {
            aFnc.add(f);
            aLbl.add(mainapp.function_labels_default.get(f));
            aLocos.add(mainapp.function_consist_locos.get(f));
            aLatching.add(mainapp.function_consist_latching.get(f));
        }
        int x = 1;
    }

    //take data from arrays and update the editing view
    void move_settings_to_view() {

        ViewGroup t = findViewById(R.id.function_consist_table); //table
        //loop thru input rows, skipping first (headings)
        int ndx = 0;
        for (int i = 1; i < t.getChildCount(); i++) {
            ViewGroup r = (ViewGroup) t.getChildAt(i);
            if (ndx < aFnc.size()) {
                ((TextView) r.getChildAt(0)).setText(aLbl.get(ndx));
                Spinner sLocos = (Spinner) r.getChildAt(1);
//                sLocos.setSelection(getSpinnerIndex(sLocos, aLocos.get(ndx)));
                sLocos.setSelection(getArrayIndex(LOCOS, aLocos.get(ndx)));
                Spinner sLatching = (Spinner) r.getChildAt(2);
//                sLatching.setSelection(getSpinnerIndex(sLatching, aLatching.get(ndx)));
                sLatching.setSelection(getArrayIndex(LATCHING, aLatching.get(ndx)));
                ndx++;
            }
        }
    }

    private int getArrayIndex(String[] array, String myString){
        for (int i=0;i<array.length;i++){
            if (array[i].equalsIgnoreCase(myString)){
                return i;
            }
        }
        return 0;
    }
//
//    private int getSpinnerIndex(Spinner spinner, String myString){
//        for (int i=0;i<spinner.getCount();i++){
//            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
//                return i;
//            }
//        }
//        return 0;
//    }

    //Save the valid function labels in the settings array
    void move_view_to_settings() {
        ViewGroup t = findViewById(R.id.function_consist_table); //table
        ViewGroup r;  //row
        //loop thru each row, Skipping the first one (the headings)  format is "label:function#"
        int ndx = 0;
        for (int i = 1; i < t.getChildCount(); i++) {
            r = (ViewGroup) t.getChildAt(i);
            //get the 2 inputs from each row
            int locosIndex = ((Spinner) r.getChildAt(1)).getSelectedItemPosition();;
            String locoString = LOCOS[locosIndex];
            int latchingIndex = ((Spinner) r.getChildAt(2)).getSelectedItemPosition();;
            String latchingString = LATCHING[latchingIndex];

            if (aFnc.size() < i) {
                aLbl.add(mainapp.function_labels_default.get(ndx));
                aFnc.add(ndx);
                settingsCurrent = false;
            } else {
                aLbl.set(ndx, mainapp.function_labels_default.get(ndx));
                aFnc.set(ndx, ndx);
                settingsCurrent = false;
            }

            if (aLocos.size() < i) {
                aLocos.add(locoString);
                aLatching.add(latchingString);
                settingsCurrent = false;
            } else {
                aLocos.set(ndx, locoString);
                aLatching.set(ndx, latchingString);
                settingsCurrent = false;
            }
            ndx++;
        }

        while (aFnc.size() > ndx) {          //if array remains then trim it
            try {
                aFnc.remove(ndx);
                aLbl.remove(ndx);
                aLocos.remove(ndx);
                aLatching.remove(ndx);
                settingsCurrent = false;
            } catch (Exception ignored) {
            }
        }
    }


    public class reset_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            String locosDefault = getResources().getString(R.string.prefFunctionConsistLocosDefaultValue);;
            String latchingDefault = getResources().getString(R.string.prefFunctionConsistLatchingDefaultValue);;
            String latchingLightBellDefault = getResources().getString(R.string.prefFunctionConsistLatchingLightBellDefaultValue);;
            int func = 0;

            for (int i = 0; i <= 28; i++) {
                if (aFnc.size() <= i) {
                    aLbl.add(mainapp.function_labels_default.get(i));
                    aFnc.add(i);
                    aLocos.add(locosDefault);
                    aLatching.add(latchingDefault);
                    settingsCurrent = false;
                } else {
                    aLbl.set(i, mainapp.function_labels_default.get(i));
                    aFnc.set(i, i);
                    aLocos.set(i, locosDefault);
                    if (i<2) {
                        aLatching.set(i, latchingLightBellDefault);
                    } else {
                        aLatching.set(i, latchingDefault);
                    }
                    settingsCurrent = false;
                }
            }
            move_settings_to_view();
        }
    }

    //Handle pressing of the back button to save settings
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {

        if (key == KeyEvent.KEYCODE_BACK) {
            move_view_to_settings();        //sync settings array to view
            if (!settingsCurrent)
                saveSettings();         //save function settings to the file
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    void saveSettings() {
        navigateToHandler(PermissionsHelper.STORE_FUNCTION_SETTINGS);
    }

    //save function and labels to file
    void saveSettingsImpl() {
        //SD Card required to save settings
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
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
                Integer fnc = aFnc.get(i);
                String locos = aLocos.get(i);
                String latching = aLatching.get(i);
                settings_output.format("%s:%s:%s:%s\n", label, fnc, locos, latching);
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

    }


    // needed in case the language was changed from the default
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@RequestCodes int requestCode) {
        Log.d("Engine_Driver", "function_settings: navigateToHandler:" + requestCode);
        if (!PermissionsHelper.getInstance().isPermissionGranted(function_consist_settings.this, requestCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(function_consist_settings.this, requestCode);
            }
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
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(function_consist_settings.this, requestCode, permissions, grantResults)) {
            Log.d("Engine_Driver", "Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
