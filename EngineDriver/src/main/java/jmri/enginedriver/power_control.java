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

import static android.view.KeyEvent.KEYCODE_BACK;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
    private Menu menu;
    private Toolbar toolbar;

    static final String[] TRACK_TYPES = {"NONE", "MAIN", "PROG", "DC", "DCX", "AUTO", "EXT", "PROG"};
    private final Button[] dccExTrackPowerButton = {null, null, null, null, null, null, null, null};
    private final LinearLayout[] dccExTrackTypeLayout = {null, null, null, null, null, null, null, null};
    private final TextView[] dccExTrackType = {null, null, null, null, null, null, null, null};
    private final TextView[] dccExTrackTypeId = {null, null, null, null, null, null, null, null};

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
        Drawable img = getResources().getDrawable(outValue.resourceId);
        btn.setBackground(img);
    }

    public void refreshDccexTracksView() {
        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            if (vn >= 05.002005) {  /// need to remove the track power options
                dccExTrackTypeLayout[i].setVisibility(mainapp.dccexTrackAvailable[i] ? View.VISIBLE : View.GONE);
                dccExTrackType[i].setText(TRACK_TYPES[mainapp.dccexTrackType[i]]);
                dccExTrackTypeId[i].setText(mainapp.dccexTrackId[i]);
                setPowerButton(dccExTrackPowerButton[i],mainapp.dccexTrackPower[i]);
            } else {
                dccExTrackTypeLayout[i].setVisibility(View.GONE);
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

        if (menu != null) {
            mainapp.displayEStop(menu);
        }

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

        try {
            vn = Float.parseFloat(mainapp.DccexVersion);
        } catch (Exception ignored) { } // invalid version

        TypedValue outValue = new TypedValue();
        mainapp.theme.resolveAttribute(R.attr.ed_power_green_button, outValue, true);
        powerOnDrawable = getResources().getDrawable(outValue.resourceId);
        mainapp.theme.resolveAttribute(R.attr.ed_power_green_red_button, outValue, true);
        powerOnAndOffDrawable = getResources().getDrawable(outValue.resourceId);
        mainapp.theme.resolveAttribute(R.attr.ed_power_red_button, outValue, true);
        powerOffDrawable = getResources().getDrawable(outValue.resourceId);
        mainapp.theme.resolveAttribute(R.attr.ed_power_yellow_button, outValue, true);
        powerUnknownDrawable = getResources().getDrawable(outValue.resourceId);

        //Set the button callbacks, storing the command to pass for each
        Button b = findViewById(R.id.power_control_button);
        button_listener click_listener = new button_listener();
        b.setOnClickListener(click_listener);

        Button closeButton = findViewById(R.id.power_button_close);
        CloseButtonClickListener close_click_listener = new CloseButtonClickListener();
        closeButton.setOnClickListener(close_click_listener);

        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DccexTrackPower0layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton0);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType0);
                    dccExTrackTypeId[i] = findViewById(R.id.DCCEXpowerControlTrackTypeId0);
                    break;
                case 1:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DccexTrackPower1layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton1);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType1);
                    dccExTrackTypeId[i] = findViewById(R.id.DCCEXpowerControlTrackTypeId1);
                    break;
                case 2:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DccexTrackPower2layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton2);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType2);
                    dccExTrackTypeId[i] = findViewById(R.id.DCCEXpowerControlTrackTypeId2);
                    break;
                case 3:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DccexTrackPower3layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton3);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType3);
                    dccExTrackTypeId[i] = findViewById(R.id.DCCEXpowerControlTrackTypeId3);
                    break;
                case 4:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DccexTrackPower4layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton4);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType4);
                    dccExTrackTypeId[i] = findViewById(R.id.DCCEXpowerControlTrackTypeId4);
                    break;
                case 5:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DccexTrackPower5layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton5);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType5);
                    dccExTrackTypeId[i] = findViewById(R.id.DCCEXpowerControlTrackTypeId5);
                    break;
                case 6:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DccexTrackPower6layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton6);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType6);
                    dccExTrackTypeId[i] = findViewById(R.id.DCCEXpowerControlTrackTypeId6);
                    break;
                case 7:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DccexTrackPower7layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton7);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType7);
                    dccExTrackTypeId[i] = findViewById(R.id.DCCEXpowerControlTrackTypeId7);
                    break;
            }
            SetTrackPowerButtonListener buttonListener = new SetTrackPowerButtonListener(i);
            dccExTrackPowerButton[i].setOnClickListener(buttonListener);
        }

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

        threaded_application.currentActivity = activity_id_type.POWER_CONTROL;
        if (mainapp.isForcingFinish()) { //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        if (menu != null) {
            mainapp.displayEStop(menu);
            mainapp.displayFlashlightMenuButton(menu);
            mainapp.setFlashlightActionViewButton(menu, findViewById(R.id.flashlight_button));
        }
        //update power state
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
    public boolean onCreateOptionsMenu(Menu myMenu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.power_menu, myMenu);
        menu = myMenu;
        mainapp.displayEStop(menu);
        mainapp.displayFlashlightMenuButton(menu);
        mainapp.setFlashlightActionViewButton(menu, findViewById(R.id.flashlight_button));

        adjustToolbarSize(menu);

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
            mainapp.toggleFlashlightActionView(this, menu, findViewById(R.id.flashlight_button));
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(menu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        mainapp.exitDoubleBackButtonInitiated = 0;
        if (key == KEYCODE_BACK) {
            endThisActivity();
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    void endThisActivity() {
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
//        InputDevice idev = getDevice(event.getDeviceId());
        boolean rslt = mainapp.implDispatchKeyEvent(event);
        if (rslt) {
            return (true);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    void adjustToolbarSize(Menu menu) {
        ViewGroup.LayoutParams layoutParams = toolbar.getLayoutParams();
        int toolbarHeight = layoutParams.height;
        int newHeightAndWidth = toolbarHeight;

        if (!threaded_application.useSmallToolbarButtonSize) {
            newHeightAndWidth = toolbarHeight*2;
            layoutParams.height = newHeightAndWidth;
            toolbar.setLayoutParams(layoutParams);
        }
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
