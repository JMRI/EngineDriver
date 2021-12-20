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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
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
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class device_sounds_settings extends AppCompatActivity implements OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu DSSMenu;
    private GestureDetector myGesture;

    private int dssThrottle0Index;
    private int dssThrottle1Index;
    private int result;
    String[] deviceSoundsEntryValuesArray;
    String[] deviceSoundsEntriesArray; // display version

    private SharedPreferences prefs;
    private Toolbar toolbar;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return myGesture.onTouchEvent(event);
    }

    public class spinner_listener_0 implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.dss_throttle0);
            dssThrottle0Index = spinner.getSelectedItemPosition();

            prefs.edit().putString("prefDeviceSounds0", deviceSoundsEntryValuesArray[dssThrottle0Index]).commit();  //reset the preference

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

    public class spinner_listener_1 implements AdapterView.OnItemSelectedListener {
        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.dss_throttle1);
            dssThrottle1Index = spinner.getSelectedItemPosition();

            prefs.edit().putString("prefDeviceSounds1", deviceSoundsEntryValuesArray[dssThrottle1Index]).commit();  //reset the preference

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
        myGesture = new GestureDetector(this);

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        //Set the buttons
        Button closeButton = findViewById(R.id.device_sounds_settings_button_close);
        close_button_listener close_click_listener = new close_button_listener();
        closeButton.setOnClickListener(close_click_listener);

        // Set the options for the sounds
        Spinner dss_throttle0 = findViewById(R.id.dss_throttle0);
        ArrayAdapter<?> spinner_adapter0 = ArrayAdapter.createFromResource(this, R.array.deviceSoundsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter0.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dss_throttle0.setAdapter(spinner_adapter0);
        dss_throttle0.setOnItemSelectedListener(new spinner_listener_0());

        // Set the options for the sounds
        Spinner dss_throttle1 = findViewById(R.id.dss_throttle1);
        ArrayAdapter<?> spinner_adapter1 = ArrayAdapter.createFromResource(this, R.array.deviceSoundsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dss_throttle1.setAdapter(spinner_adapter1);
        dss_throttle1.setOnItemSelectedListener(new spinner_listener_1());

        //put pointer to this activity's handler in main app's shared variable
        mainapp.device_sounds_settings_msg_handler = new device_sounds_settings_handler();

        deviceSoundsEntryValuesArray = this.getResources().getStringArray(R.array.deviceSoundsEntryValues);
        final List<String> deviceSoundsList = new ArrayList<>(Arrays.asList(deviceSoundsEntryValuesArray));
        deviceSoundsEntriesArray = this.getResources().getStringArray(R.array.deviceSoundsEntries);
        final List<String> deviceSoundsEntriesList = new ArrayList<>(Arrays.asList(deviceSoundsEntriesArray));

        mainapp.prefDeviceSounds[0] = prefs.getString("prefDeviceSounds0", getResources().getString(R.string.prefDeviceSoundsDefaultValue));
        mainapp.prefDeviceSounds[1] = prefs.getString("prefDeviceSounds1", getResources().getString(R.string.prefDeviceSoundsDefaultValue));

        // throttle 0
        dssThrottle0Index = Arrays.asList(deviceSoundsEntryValuesArray).indexOf(mainapp.prefDeviceSounds[0]);
        if (dssThrottle0Index<0) dssThrottle0Index=0;
        Spinner spinner0 = findViewById(R.id.dss_throttle0);
        spinner0.setSelection(dssThrottle0Index);
        // throttle 1
        dssThrottle1Index = Arrays.asList(deviceSoundsEntryValuesArray).indexOf(mainapp.prefDeviceSounds[1]);
        if (dssThrottle1Index<0) dssThrottle1Index=0;
        Spinner spinner1 = findViewById(R.id.dss_throttle1);
        spinner1.setSelection(dssThrottle1Index);



        toolbar = (Toolbar) findViewById(R.id.toolbar);
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

        super.onDestroy();
    }

    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
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
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                return true;
            case R.id.flashlight_button:
                mainapp.toggleFlashlight(this, DSSMenu);
                return true;
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    mainapp.powerControlNotAllowedDialog(DSSMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("HandlerLeak")
    class device_sounds_settings_handler extends Handler {

        public void handleMessage(Message msg) {
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
            }
        }
    }

    //Always go to throttle if back button pressed
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
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

}