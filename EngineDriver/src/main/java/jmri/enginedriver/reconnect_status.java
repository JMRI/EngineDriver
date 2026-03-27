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
import android.app.Activity;
import android.app.ActivityOptions;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;

import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.util.LocaleHelper;

public class reconnect_status extends AppCompatActivity {
    static final String activityName = "reconnect_status";


    private threaded_application mainapp;  // hold pointer to mainapp
    private String prog = "";
    private boolean backOk = true;
//    private boolean retryFirst = false;

    private LinearLayout screenNameLine;
    private Toolbar toolbar;
    private LinearLayout statusLine;

    private class BundleMessageHandler extends Handler {

        public BundleMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
//            threaded_application.extendedLogging(activityName + ": BundleMessageHandler.handleMessage() what: " + msg.what );
            Bundle bundle = msg.getData();

            switch (msg.what) {

                // - - - - - - - - - - - - - - - - - - - - - - - - //

                case message_type.WIT_CON_RETRY: {
                    if ( (bundle != null)
                            && (bundle.containsKey(alert_bundle_tag_type.COMMAND)) ) {

//                    retryFirst = true;
                        String command = (bundle.containsKey(alert_bundle_tag_type.COMMAND)) ? bundle.getString(alert_bundle_tag_type.COMMAND) : "";
                        refresh_reconnect_status(command);
                    }
                    break;
                }

                case message_type.WIT_CON_RECONNECT: {
                    if ( (bundle != null)
                            && (bundle.containsKey(alert_bundle_tag_type.COMMAND)) ) {

                        String command = (bundle.containsKey(alert_bundle_tag_type.COMMAND)) ? bundle.getString(alert_bundle_tag_type.COMMAND) : "";
                        refresh_reconnect_status(command);
                        reconnected();
                    }
                    break;
                }

                // - - - - - - - - - - - - - - - - - - - - - - - - //

                case message_type.REOPEN_THROTTLE:
                    // ignore
                    break;

                case message_type.DISCONNECT:
                    disconnect();
                    break;

                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.SHUTDOWN:
                    shutdown();
                    break;

            }
        }
    }

    @SuppressLint("SetTextI18n")
    public void refresh_reconnect_status(String status) {
        TextView tv = findViewById(R.id.reconnect_status);
        if (status != null) {
            prog = prog + ".";
            if (prog.length() > 5)
                prog = ".";
            tv.setText(status + prog);
        }
    }

    public void reconnected() {
        if (backOk) {                        // ensure we only run this once
            backOk = false;
            TextView tv = findViewById(R.id.reconnect_help);
            tv.setText(getString(R.string.reconnect_success));
            if (mainapp.activityBundleMessageHandlers[activity_id_type.RECONNECT_STATUS] != null) {
                mainapp.alertCommHandlerWithBundle(message_type.SEND_HEARTBEAT_START);
                mainapp.activityBundleMessageHandlers[activity_id_type.RECONNECT_STATUS].postDelayed(delayCloseScreen, 500L);
            } else {
                Log.d(threaded_application.applicationName, activityName + ": reconnected(): handler already null");
                endThisActivity();
            }
        }
    }

    @SuppressLint("Recycle")
    private final Runnable delayCloseScreen = new Runnable() {
        @Override
        public void run() {
            endThisActivity();
        }
    };

    private void endThisActivity() {
        Log.d(threaded_application.applicationName, activityName + ": endThisActivity()");
        this.finish();                  //end this activity

        threaded_application.activityInTransition(activityName);
        startACoreActivity(this, mainapp.getThrottleIntent(), false, 0);
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

        setContentView(R.layout.reconnect_page);

        //put pointer to this activity's handler in main app's shared variable (If needed)
        if (mainapp.activityBundleMessageHandlers[activity_id_type.RECONNECT_STATUS] == null)
            mainapp.activityBundleMessageHandlers[activity_id_type.RECONNECT_STATUS] = new BundleMessageHandler(Looper.getMainLooper());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String msg = extras.getString("status");
            if (msg != null) {
                refresh_reconnect_status(msg);
            }
        }
//        retryFirst = false;

        if (mainapp.prefFeedbackOnDisconnect) {
            MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            try {
                mediaPlayer.start();
            } catch (Exception e) {
                Log.d(threaded_application.applicationName, activityName + ": reconnected(): Unable to play notification sound");
            }

            mainapp.vibrate(new long[]{1000, 500, 1000, 500, 1000, 500});
        }

        // if this is called, assume that we will need to recreate the Throttle Screen
        mainapp.throttleSwitchWasRequestedOrReinitialiseRequired = true;

        Button exitButton = findViewById(R.id.reconnect_exit_button);
        ExitButtonListener exitButtonListener = new ExitButtonListener(this);
        exitButton.setOnClickListener(exitButtonListener);

        Button connectButton = findViewById(R.id.reconnect_new_connection_button);
        ConnectButtonListener connectButtonListener = new ConnectButtonListener(this);
        connectButton.setOnClickListener(connectButtonListener);

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_reconnect_status),
                    "");
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                mainapp.checkExit(reconnect_status.this);
                mainapp.exitDoubleBackButtonInitiated = 0;
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

    } //end onCreate()

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
        threaded_application.currentActivity = activity_id_type.RECONNECT_STATUS;
        if (mainapp.isForcingFinish()) { //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        this.backOk = true;
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        Log.d(threaded_application.applicationName, activityName + ": onDestroy()");
        super.onDestroy();

        mainapp.clearActivityBundleMessageHandler(activity_id_type.RECONNECT_STATUS);
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

    // common startActivity()
    // used for swipes for the main activities only - Throttle, Turnouts, Routes, Web
    protected void startACoreActivity(Activity activity, Intent in, boolean swipe, float deltaX) {
        if (activity != null && in != null) {
            threaded_application.activityInTransition(activityName);
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ActivityOptions options = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.fade_in, R.anim.fade_out);
            startActivity(in, options.toBundle());
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reconnect_status_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.exit_mnu) {
            mainapp.checkAskExit(this, true);
            return true;
        } else if (item.getItemId() == R.id.logviewer_menu) {
            Intent in = new Intent().setClass(this, LogViewerActivity.class);
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else {
                return super.onOptionsItemSelected(item);
        }
    }

    public class ExitButtonListener implements View.OnClickListener {
        Activity thisActivity;

        ExitButtonListener(Activity activity) {
            thisActivity = activity;
        }
        public void onClick(View v) {
            mainapp.buttonVibration();
            mainapp.checkAskExit(thisActivity, true);
        }
    }

    public class ConnectButtonListener implements View.OnClickListener {
        Activity thisActivity;

        ConnectButtonListener(Activity activity) {
            thisActivity = activity;
        }
        public void onClick(View v) {

            final AlertDialog.Builder b = new AlertDialog.Builder(thisActivity);
            b.setIcon(android.R.drawable.ic_dialog_alert);
            b.setTitle(R.string.newConnectionTitle);
            b.setMessage(R.string.newConnectionText);
            b.setCancelable(true);
            b.setPositiveButton(R.string.yes, (dialog, id) -> {
                threaded_application.activityInTransition(activityName);
                mainapp.alertCommHandlerWithBundle(message_type.DISCONNECT);
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    Intent in = new Intent().setClass(thisActivity, connection_activity.class);
                    startActivity(in);
                    connection_activity.overridePendingTransition(thisActivity, R.anim.fade_in, R.anim.fade_out);
                }, 2000);
            });
            b.setNegativeButton(R.string.no, null);
            AlertDialog alert = b.create();
            alert.show();

            // find positiveButton and negativeButton
            Button positiveButton = alert.findViewById(android.R.id.button1);
            Button negativeButton = alert.findViewById(android.R.id.button2);
            // then get their parent ViewGroup
            ViewGroup buttonPanelContainer = (ViewGroup) positiveButton.getParent();
            int positiveButtonIndex = buttonPanelContainer.indexOfChild(positiveButton);
            int negativeButtonIndex = buttonPanelContainer.indexOfChild(negativeButton);
            if (positiveButtonIndex < negativeButtonIndex) {  // force 'No' 'Yes' order
                // prepare exchange their index in ViewGroup
                buttonPanelContainer.removeView(positiveButton);
                buttonPanelContainer.removeView(negativeButton);
                buttonPanelContainer.addView(negativeButton, positiveButtonIndex);
                buttonPanelContainer.addView(positiveButton, negativeButtonIndex);
            }

        }
    }
}
