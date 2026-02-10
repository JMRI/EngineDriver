/* Copyright (C) 2017-2026 M. Steve Todd mstevetodd@gmail.com

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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;

import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.util.LocaleHelper;

public class power_control extends AppCompatActivity {
    static final String activityName = "power_control";

    private threaded_application mainapp;  // hold pointer to mainapp
    private Drawable powerOnDrawable;  //hold background graphics for power button
    private Drawable powerOnAndOffDrawable;
    private Drawable powerOffDrawable;
    private Drawable powerUnknownDrawable;
    private Menu overflowMenu;
    private Toolbar toolbar;
    private int result = RESULT_OK;

    static final String[] TRACK_TYPES = {"NONE", "MAIN", "PROG", "DC", "DCX", "AUTO", "EXT", "PROG"};
    private final Button[] dccexTrackPowerButton = {null, null, null, null, null, null, null, null};
    private final LinearLayout[] dccexTrackTypeLayout = {null, null, null, null, null, null, null, null};
    private final TextView[] dccexTrackType = {null, null, null, null, null, null, null, null};
    private final TextView[] dccexTrackTypeId = {null, null, null, null, null, null, null, null};

    float vn = 4; // DCC-EC Version number

    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class PowerControlMessageHandler extends Handler {

        public PowerControlMessageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE:
                    String response_str = msg.obj.toString();
                    if (response_str.length() >= 3 && response_str.startsWith("PPA")) {  //refresh power state
                        refresh_power_control_view();
                    }
                    if ( (response_str.length() == 5) && ("PXX".equals(response_str.substring(0, 3))) ) {  // individual track power response
                        refreshDccexTracksView();
                    }
                    break;

                case message_type.ESTOP_PAUSED:
                case message_type.ESTOP_RESUMED:
                    mainapp.setEmergencyStopStateActionViewButton(overflowMenu, overflowMenu.findItem(R.id.emergency_stop_button));
                    break;

                case message_type.REFRESH_OVERFLOW_MENU:
                    refreshOverflowMenu();
                    break;

                case message_type.REOPEN_THROTTLE:
                    if (threaded_application.currentActivity == activity_id_type.POWER_CONTROL)
                        endThisActivity();
                    break;

                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refresh_power_control_view();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.SHUTDOWN:
                    shutdown();
                    break;
                case message_type.DISCONNECT:
                    disconnect();
                    break;
                case message_type.RECEIVED_TRACKS:
                    refreshDccexTracksView();
                    break;

                case message_type.TERMINATE_ALL_ACTIVITIES_BAR_CONNECTION:
                case message_type.LOW_MEMORY:
                    endThisActivity();
                    break;

                default:
                    break;

            }
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    public class button_listener implements View.OnClickListener {

        public void onClick(View v) {
            int newState = 1;
            if (mainapp.power_state.equals("1")) { //toggle to opposite value 0=off, 1=on
                newState = 0;
            }
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.POWER_CONTROL, "", newState);
            mainapp.buttonVibration();
        }
    }

    public class SetTrackPowerButtonListener implements View.OnClickListener {
        int myTrack;
        char myTrackLetter;

        public SetTrackPowerButtonListener(int track) {
            myTrack = track;
            myTrackLetter = (char) ('A' + track);
        }

        public void onClick(View v) {
            if (mainapp.dccexTrackPower[myTrack] == 0 ) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK_POWER, ""+myTrackLetter, 1);
            } else {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK_POWER, ""+myTrackLetter, 0);
            }
        }
    }

    void setPowerButton(Button btn, int powerState) {
        TypedValue outValue = new TypedValue();
        if (powerState == 1) {
            mainapp.theme.resolveAttribute(R.attr.ed_power_green_button, outValue, true);
        } else if (powerState == 0) {
            mainapp.theme.resolveAttribute(R.attr.ed_power_red_button, outValue, true);
        } else {
            if (!mainapp.isDCCEX) {
                mainapp.theme.resolveAttribute(R.attr.ed_power_yellow_button, outValue, true);
            } else {
                mainapp.theme.resolveAttribute(R.attr.ed_power_green_red_button, outValue, true);
            }
        }
        Drawable img = AppCompatResources.getDrawable(this, outValue.resourceId);
        btn.setBackground(img);
    }

    public void refreshDccexTracksView() {
        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            if (vn >= 05.002005) {  /// need to remove the track power options
                dccexTrackTypeLayout[i].setVisibility(mainapp.dccexTrackAvailable[i] ? View.VISIBLE : View.GONE);
                dccexTrackType[i].setText(TRACK_TYPES[mainapp.dccexTrackType[i]]);
                dccexTrackTypeId[i].setText(mainapp.dccexTrackId[i]);
                setPowerButton(dccexTrackPowerButton[i],mainapp.dccexTrackPower[i]);
            } else {
                dccexTrackTypeLayout[i].setVisibility(View.GONE);
            }
        }
    }

    //Set the button text based on current power state
    public void refresh_power_control_view() {
        Button b = findViewById(R.id.power_control_button);
        Drawable currentImage = powerUnknownDrawable;
        if (!mainapp.isPowerControlAllowed()) {
            b.setEnabled(false);
            TextView tv = findViewById(R.id.power_control_text);
            tv.setText(getString(R.string.power_control_not_allowed));
        } else {
            b.setEnabled(true);
            switch (mainapp.power_state) {
                case "1":
                    currentImage = powerOnDrawable;
                    break;
                case "2":
                    if (!mainapp.isDCCEX) {
                        currentImage = powerUnknownDrawable;
                    } else {
                        currentImage = powerOnAndOffDrawable;
                    }
                    break;
                default:
                    currentImage = powerOffDrawable;
                    break;
            }
        }

        refreshOverflowMenu();

//        b.setBackgroundDrawable(currentImage);
        b.setBackground(currentImage);
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

        mainapp.applyTheme(this);

        setContentView(R.layout.power_control);

        //put pointer to this activity's handler in main app's shared variable (If needed)
        mainapp.power_control_msg_handler = new PowerControlMessageHandler(Looper.getMainLooper());

        // request this as early as possible
        if (mainapp.isDCCEX) mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

        vn = mainapp.getDccexVersionNumeric();

        TypedValue outValue = new TypedValue();
        mainapp.theme.resolveAttribute(R.attr.ed_power_green_button, outValue, true);
        powerOnDrawable = AppCompatResources.getDrawable(this, outValue.resourceId);
        mainapp.theme.resolveAttribute(R.attr.ed_power_green_red_button, outValue, true);
        powerOnAndOffDrawable = AppCompatResources.getDrawable(this, outValue.resourceId);
        mainapp.theme.resolveAttribute(R.attr.ed_power_red_button, outValue, true);
        powerOffDrawable = AppCompatResources.getDrawable(this, outValue.resourceId);
        mainapp.theme.resolveAttribute(R.attr.ed_power_yellow_button, outValue, true);
        powerUnknownDrawable = AppCompatResources.getDrawable(this, outValue.resourceId);

        //Set the button callbacks, storing the command to pass for each
        Button b = findViewById(R.id.power_control_button);
        button_listener click_listener = new button_listener();
        b.setOnClickListener(click_listener);

        Button closeButton = findViewById(R.id.power_button_close);
        CloseButtonClickListener close_click_listener = new CloseButtonClickListener();
        closeButton.setOnClickListener(close_click_listener);

        TypedArray dccex_power_control_track_type_layout_ids = getResources().obtainTypedArray(R.array.dccex_power_control_track_type_layout_ids);
        TypedArray dccex_power_control_track_power_button_ids = getResources().obtainTypedArray(R.array.dccex_power_control_track_power_button_ids);
        TypedArray dccex_power_control_track_type_ids = getResources().obtainTypedArray(R.array.dccex_power_control_track_type_ids);
        TypedArray dccex_power_control_track_type_id_ids = getResources().obtainTypedArray(R.array.dccex_power_control_track_type_id_ids);

        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            dccexTrackTypeLayout[i] = findViewById(dccex_power_control_track_type_layout_ids.getResourceId(i,0));
            dccexTrackPowerButton[i] = findViewById(dccex_power_control_track_power_button_ids.getResourceId(i,0));
            dccexTrackType[i] = findViewById(dccex_power_control_track_type_ids.getResourceId(i,0));
            dccexTrackTypeId[i] = findViewById(dccex_power_control_track_type_id_ids.getResourceId(i,0));

            SetTrackPowerButtonListener buttonListener = new SetTrackPowerButtonListener(i);
            dccexTrackPowerButton[i].setOnClickListener(buttonListener);
        }

        dccex_power_control_track_type_layout_ids.recycle();
        dccex_power_control_track_power_button_ids.recycle();
        dccex_power_control_track_type_ids.recycle();
        dccex_power_control_track_type_id_ids.recycle();

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.d(threaded_application.applicationName, activityName + ": handleOnBackPressed()");
                mainapp.exitDoubleBackButtonInitiated = 0;
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    threaded_application.activityInTransition(activityName);
                    setResult(result);
                    finish();
                    connection_activity.overridePendingTransition(power_control.this, R.anim.fade_in, R.anim.fade_out);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        LinearLayout screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        LinearLayout statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_power_control),
                    "");
        }

    } // end onCreate

    @Override
    public void onPause() {
        super.onPause();
        threaded_application.activityPaused(activityName);
    }

    @Override
    public void onResume() {
        super.onResume();
        threaded_application.activityResumed(activityName);
        mainapp.removeNotification(this.getIntent());

        //noinspection AssignmentToStaticFieldFromInstanceMethod
        threaded_application.currentActivity = activity_id_type.POWER_CONTROL;
        if (mainapp.isForcingFinish()) { //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        refreshOverflowMenu();
        refresh_power_control_view();
        refreshDccexTracksView();
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mainapp.power_control_msg_handler !=null) {
            mainapp.power_control_msg_handler.removeCallbacksAndMessages(null);
            mainapp.power_control_msg_handler = null;
        } else {
            Log.d(threaded_application.applicationName, activityName + ": onDestroy(): mainapp.power_control_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.power_menu, menu);
        overflowMenu = menu;
        refreshOverflowMenu();
        return super.onCreateOptionsMenu(menu);
    }


    private void refreshOverflowMenu() {
        if (overflowMenu == null) return;

        mainapp.refreshCommonOverflowMenu(overflowMenu, false);
        adjustToolbarSize(overflowMenu);
    }

        @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.emergency_stop_button) {
            mainapp.sendEStopMsg();
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.flashlight_button) {
            mainapp.toggleFlashlightActionView(this, overflowMenu, overflowMenu.findItem(R.id.flashlight_button));
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(overflowMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    void endThisActivity() {
        Log.d(threaded_application.applicationName, activityName + ": endThisActivity()");
        threaded_application.activityInTransition(activityName);
        this.finish();  //end this activity
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void disconnect() {
        this.finish();
    }

    private void shutdown() {
        this.finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public class CloseButtonClickListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            finish();
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
        boolean rslt = mainapp.implDispatchKeyEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    void adjustToolbarSize(Menu menu) {
        int newHeightAndWidth = mainapp.adjustToolbarSize(toolbar);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            View itemChooser = item.getActionView();

            if (itemChooser != null) {
                itemChooser.getLayoutParams().height = newHeightAndWidth;
                itemChooser.getLayoutParams().width = (int) ( (float) newHeightAndWidth * 1.3 );

                itemChooser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onOptionsItemSelected(item);
                    }
                });
            }
        }
    }
}
