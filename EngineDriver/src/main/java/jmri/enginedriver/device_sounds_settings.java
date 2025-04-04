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

import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import jmri.enginedriver.util.InPhoneLocoSoundsLoader;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.util.LocaleHelper;

public class device_sounds_settings extends AppCompatActivity implements OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu DSSMenu;
//    private GestureDetector myGesture;

    String[] valuesList;

    private int dssThrottle0Index;
    private int dssThrottle1Index;
    private int result;
    String[] deviceSoundsEntryValuesArray;
    String[] deviceSoundsEntriesArray; // display version
    private EditText etDeviceSoundsMomentum;
    private int dssDeviceSoundsLocoVolumeIndex;
    private int dssDeviceSoundsBellVolumeIndex;
    private int dssDeviceSoundsHornVolumeIndex;
    private int dss_DeviceSoundsBellIsMomentary;
    private int dss_DeviceSoundsMomentumOverride;
    String prefDeviceSoundsMomentum;
    String prefDeviceSoundsLocoVolume;
    String prefDeviceSoundsBellVolume;
    String prefDeviceSoundsHornVolume;
    private SharedPreferences prefs;

    protected InPhoneLocoSoundsLoader iplsLoader;

    /** @noinspection FieldCanBeLocal*/
    private LinearLayout screenNameLine;
    /** @noinspection FieldCanBeLocal*/
    private Toolbar toolbar;
    /** @noinspection FieldCanBeLocal*/
    private LinearLayout statusLine;

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        return myGesture.onTouchEvent(event);
//    }

    /** @noinspection SameParameterValue, SameParameterValue , SameParameterValue */
    @SuppressLint("ApplySharedPref")
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

    public class spinner_listener_0 implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.dss_throttle0);
            dssThrottle0Index = spinner.getSelectedItemPosition();
            prefs.edit().putString("prefDeviceSounds0", deviceSoundsEntryValuesArray[dssThrottle0Index]).commit();  //reset the preference
            mainapp.soundsReloadSounds = true;
            hideKeyboard(view);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class spinner_listener_1 implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.dss_throttle1);
            dssThrottle1Index = spinner.getSelectedItemPosition();
            prefs.edit().putString("prefDeviceSounds1", deviceSoundsEntryValuesArray[dssThrottle1Index]).commit();  //reset the preference
            mainapp.soundsReloadSounds = true;
            hideKeyboard(view);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class spinner_listener_loco_volume implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.dss_DeviceSoundsLocoVolume);
            dssDeviceSoundsLocoVolumeIndex = spinner.getSelectedItemPosition();
            prefDeviceSoundsLocoVolume = valuesList[dssDeviceSoundsLocoVolumeIndex];
            prefs.edit().putString("prefDeviceSoundsLocoVolume", prefDeviceSoundsLocoVolume).commit();  //reset the preference

            hideKeyboard(view);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class spinner_listener_bell_volume implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.dss_DeviceSoundsBellVolume);
            dssDeviceSoundsBellVolumeIndex = spinner.getSelectedItemPosition();
            prefDeviceSoundsBellVolume = valuesList[dssDeviceSoundsBellVolumeIndex];
            prefs.edit().putString("prefDeviceSoundsBellVolume", prefDeviceSoundsBellVolume).commit();  //reset the preference

            hideKeyboard(view);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class spinner_listener_horn_volume implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.dss_DeviceSoundsHornVolume);
            dssDeviceSoundsHornVolumeIndex = spinner.getSelectedItemPosition();
            prefDeviceSoundsHornVolume = valuesList[dssDeviceSoundsHornVolumeIndex];
            prefs.edit().putString("prefDeviceSoundsHornVolume", prefDeviceSoundsHornVolume).commit();  //reset the preference

            hideKeyboard(view);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class bell_momentary_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            Spinner spinner = findViewById(R.id.dss_DeviceSoundsBellIsMomentary);
            dss_DeviceSoundsBellIsMomentary = spinner.getSelectedItemPosition();
            mainapp.prefDeviceSoundsBellIsMomentary = dss_DeviceSoundsBellIsMomentary == 0;
            prefs.edit().putBoolean("prefDeviceSoundsBellIsMomentary", mainapp.prefDeviceSoundsBellIsMomentary).commit();  //reset the preference

            hideKeyboard(view);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class momentum_override_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            Spinner spinner = findViewById(R.id.dss_DeviceSoundsMomentumOverride);
            dss_DeviceSoundsMomentumOverride = spinner.getSelectedItemPosition();
            mainapp.prefDeviceSoundsMomentumOverride = dss_DeviceSoundsMomentumOverride == 0;
            prefs.edit().putBoolean("prefDeviceSoundsMomentumOverride", mainapp.prefDeviceSoundsMomentumOverride).commit();  //reset the preference

            hideKeyboard(view);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        result = RESULT_CANCELED;

        Bundle extras = getIntent().getExtras();

        mainapp.applyTheme(this);

        setContentView(R.layout.device_sounds_settings);
        //put pointer to this activity's handler in main app's shared variable
//        myGesture = new GestureDetector(this);

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        iplsLoader = new InPhoneLocoSoundsLoader(mainapp, prefs, context);
        iplsLoader.getIplsList();        //see if there any custom ipls files
        int ipslCount = mainapp.iplsNames.size();
        int deviceSoundsCount = this.getResources().getStringArray(R.array.deviceSoundsEntries).length;
        deviceSoundsEntriesArray = new String[deviceSoundsCount + ipslCount];
        deviceSoundsEntryValuesArray = new String[deviceSoundsCount + ipslCount];
        for (int i=0; i<deviceSoundsCount; i++) {
            deviceSoundsEntriesArray[i] = this.getResources().getStringArray(R.array.deviceSoundsEntries)[i];
            deviceSoundsEntryValuesArray[i] = this.getResources().getStringArray(R.array.deviceSoundsEntryValues)[i];
        }
        if (!mainapp.iplsNames.isEmpty()) {
            for (int i=0; i<ipslCount; i++) {
                deviceSoundsEntriesArray[deviceSoundsCount+i] = mainapp.iplsNames.get(i);
                deviceSoundsEntryValuesArray[deviceSoundsCount+i] = mainapp.iplsFileNames.get(i);
            }
        }

        //Set the buttons
        Button closeButton = findViewById(R.id.device_sounds_settings_button_close);
        close_button_listener close_click_listener = new close_button_listener();
        closeButton.setOnClickListener(close_click_listener);

        // Set the options for the sounds  - throttle 0
        Spinner dss_throttle0 = findViewById(R.id.dss_throttle0);
//        ArrayAdapter<?> spinner_adapter0 = ArrayAdapter.createFromResource(this, R.array.deviceSoundsEntries, android.R.layout.simple_spinner_item);
        ArrayAdapter<String> spinner_adapter0 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceSoundsEntriesArray);
        spinner_adapter0.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dss_throttle0.setAdapter(spinner_adapter0);
        dss_throttle0.setOnItemSelectedListener(new spinner_listener_0());

        // Set the options for the sounds - throttle 1
        Spinner dss_throttle1 = findViewById(R.id.dss_throttle1);
//        ArrayAdapter<?> spinner_adapter1 = ArrayAdapter.createFromResource(this, R.array.deviceSoundsEntries, android.R.layout.simple_spinner_item);
        ArrayAdapter<String> spinner_adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceSoundsEntriesArray);
        spinner_adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dss_throttle1.setAdapter(spinner_adapter1);
        dss_throttle1.setOnItemSelectedListener(new spinner_listener_1());

        //put pointer to this activity's handler in main app's shared variable
        mainapp.device_sounds_settings_msg_handler = new device_sounds_settings_handler(Looper.getMainLooper());

//        deviceSoundsEntryValuesArray = this.getResources().getStringArray(R.array.deviceSoundsEntryValues);
//        deviceSoundsEntriesArray = this.getResources().getStringArray(R.array.deviceSoundsEntries);

        mainapp.prefDeviceSounds[0] = prefs.getString("prefDeviceSounds0", getResources().getString(R.string.prefDeviceSoundsDefaultValue));
        mainapp.prefDeviceSounds[1] = prefs.getString("prefDeviceSounds1", getResources().getString(R.string.prefDeviceSoundsDefaultValue));
        prefDeviceSoundsMomentum = prefs.getString("prefDeviceSoundsMomentum", getResources().getString(R.string.prefDeviceSoundsMomentumDefaultValue));
        prefDeviceSoundsLocoVolume = prefs.getString("prefDeviceSoundsLocoVolume", "100");
        prefDeviceSoundsBellVolume = prefs.getString("prefDeviceSoundsBellVolume", "100");
        prefDeviceSoundsHornVolume = prefs.getString("prefDeviceSoundsHornVolume", "100");

        // throttle 0
        dssThrottle0Index = Arrays.asList(deviceSoundsEntryValuesArray).indexOf(mainapp.prefDeviceSounds[0]);
        if (dssThrottle0Index<0) dssThrottle0Index=0;
//        Spinner spinner0 = findViewById(R.id.dss_throttle0);
        dss_throttle0.setSelection(dssThrottle0Index);
        // throttle 1
        dssThrottle1Index = Arrays.asList(deviceSoundsEntryValuesArray).indexOf(mainapp.prefDeviceSounds[1]);
        if (dssThrottle1Index<0) dssThrottle1Index=0;
//        Spinner spinner1 = findViewById(R.id.dss_throttle1);
        dss_throttle1.setSelection(dssThrottle1Index);

        etDeviceSoundsMomentum = findViewById(R.id.dss_DeviceSoundsMomentum);
        etDeviceSoundsMomentum.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        etDeviceSoundsMomentum.setText(prefDeviceSoundsMomentum);

        valuesList = new String[100];
        for(int i=1;i<=100;i++){
            valuesList[i-1]=Integer.toString(i);
        }
        Spinner spinner = findViewById(R.id.dss_DeviceSoundsLocoVolume);
        dssDeviceSoundsLocoVolumeIndex = Arrays.asList(valuesList).indexOf(prefDeviceSoundsLocoVolume);
        if (dssDeviceSoundsLocoVolumeIndex<1) dssDeviceSoundsLocoVolumeIndex=99;
        ArrayAdapter<String> sa_deviceSoundsLocoVolume = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, valuesList);
        sa_deviceSoundsLocoVolume.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(sa_deviceSoundsLocoVolume);
        spinner.setOnItemSelectedListener(new spinner_listener_loco_volume());
        spinner.setSelection(dssDeviceSoundsLocoVolumeIndex);

        spinner = findViewById(R.id.dss_DeviceSoundsBellVolume);
        dssDeviceSoundsBellVolumeIndex = Arrays.asList(valuesList).indexOf(prefDeviceSoundsBellVolume);
        if (dssDeviceSoundsBellVolumeIndex<1) dssDeviceSoundsBellVolumeIndex=99;
        ArrayAdapter<String> sa_deviceSoundsBellVolume = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, valuesList);
        sa_deviceSoundsBellVolume.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(sa_deviceSoundsBellVolume);
        spinner.setOnItemSelectedListener(new spinner_listener_bell_volume());
        spinner.setSelection(dssDeviceSoundsBellVolumeIndex);

        spinner = findViewById(R.id.dss_DeviceSoundsHornVolume);
        dssDeviceSoundsHornVolumeIndex = Arrays.asList(valuesList).indexOf(prefDeviceSoundsHornVolume);
        if (dssDeviceSoundsHornVolumeIndex<1) dssDeviceSoundsHornVolumeIndex=99;
        ArrayAdapter<String> sa_deviceSoundsHornVolume = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, valuesList);
        sa_deviceSoundsHornVolume.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(sa_deviceSoundsHornVolume);
        spinner.setOnItemSelectedListener(new spinner_listener_horn_volume());
        spinner.setSelection(dssDeviceSoundsHornVolumeIndex);

        spinner = findViewById(R.id.dss_DeviceSoundsBellIsMomentary);
        spinner.setOnItemSelectedListener(new bell_momentary_spinner_listener());
        if (mainapp.prefDeviceSoundsBellIsMomentary) {
            spinner.setSelection(0);
        } else {
            spinner.setSelection(1);
        }

        spinner = findViewById(R.id.dss_DeviceSoundsMomentumOverride);
        spinner.setOnItemSelectedListener(new momentum_override_spinner_listener());
        if (mainapp.prefDeviceSoundsMomentumOverride) {
            spinner.setSelection(0);
        } else {
            spinner.setSelection(1);
        }

        if (mainapp.maxThrottlesCurrentScreen<2) {
            dss_throttle1.setEnabled(false);
            dss_throttle1.setAlpha(0.5f);
            TextView dss_throttle1_label = findViewById(R.id.dss_throttle1_label);
            dss_throttle1_label.setAlpha(0.5f);
        }

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = (LinearLayout) findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

    } // end onCreate

    @Override
    public void onResume() {
        super.onResume();
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        if (DSSMenu != null) {
            mainapp.displayEStop(DSSMenu);
            mainapp.displayFlashlightMenuButton(DSSMenu);
            mainapp.setFlashlightButton(DSSMenu);
            mainapp.displayPowerStateMenuButton(DSSMenu);
            mainapp.setPowerStateButton(DSSMenu);
        }
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "device_sounds_settings.onDestroy() called");
        iplsLoader.loadSounds();
        super.onDestroy();
    }

    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            saveNumberEntries();
            hideKeyboard(v);
            mainapp.buttonVibration();
            finish();
        }
    }

    // end current activity
    void end_this_activity() {
        Intent resultIntent = new Intent();
        setResult(result, resultIntent);
        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.device_sounds_settings, menu);
        DSSMenu = menu;
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
            mainapp.toggleFlashlight(this, DSSMenu);
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(DSSMenu);
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
    class device_sounds_settings_handler extends Handler {

        public device_sounds_settings_handler(Looper looper) {
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
                            mainapp.setPowerStateButton(DSSMenu);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    void saveNumberEntries() {
        prefDeviceSoundsMomentum = limitIntEditValue("prefDeviceSoundsMomentum", etDeviceSoundsMomentum, 0, 2000, getResources().getString(R.string.prefDeviceSoundsMomentumDefaultValue));
        mainapp.prefDeviceSoundsMomentum = Integer.parseInt(prefDeviceSoundsMomentum);
        prefs.edit().putString("prefDeviceSoundsMomentum", prefDeviceSoundsMomentum).commit();  //reset the preference
        prefs.edit().putString("prefDeviceSoundsLocoVolume", prefDeviceSoundsLocoVolume).commit();  //reset the preference
        prefs.edit().putString("prefDeviceSoundsBellVolume", prefDeviceSoundsBellVolume).commit();  //reset the preference
        prefs.edit().putString("prefDeviceSoundsHornVolume", prefDeviceSoundsHornVolume).commit();  //reset the preference
    }

    //Always go to throttle if back button pressed
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        saveNumberEntries();
        mainapp.exitDoubleBackButtonInitiated = 0;

        if (key == KeyEvent.KEYCODE_BACK) {
            Intent resultIntent = new Intent();
            setResult(result, resultIntent);
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
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

    void hideKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if ((imm != null) && (view != null)) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
        }

    }
    // listener for the joystick events
    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        boolean rslt = mainapp.implDispatchGenericMotionEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }

    // listener for physical keyboard events
    // used to support the gamepad only   DPAD and key events
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
//        InputDevice idev = getDevice(event.getDeviceId());
        boolean rslt = mainapp.implDispatchKeyEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }
}