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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;

import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.message_type;

public class about_page extends AppCompatActivity {
    static final String activityName = "about_page";

    private threaded_application mainapp; // hold pointer to mainapp
    private Menu overflowMenu;
    private Toolbar toolbar;
    private int result = RESULT_OK;

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("DefaultLocale")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) this.getApplication();

        mainapp.applyTheme(this);

        setContentView(R.layout.about_page);

        // format and show version info
        TextView v = findViewById(R.id.about_info);
        v.setText(Html.fromHtml(mainapp.getAboutInfo()));

        // show ED webpage
        WebView webview = findViewById(R.id.about_webview);
        webview.loadUrl(getApplicationContext().getResources().getString(R.string.about_page_url));

        //Set the buttons
        Button closeButton = findViewById(R.id.about_button_close);
        CloseButtonListener closeButtonlistener = new CloseButtonListener();
        closeButton.setOnClickListener(closeButtonlistener);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.about_page_msg_handler = new about_page_handler(Looper.getMainLooper());

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
                    connection_activity.overridePendingTransition(about_page.this, R.anim.fade_in, R.anim.fade_out);
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
                    getApplicationContext().getResources().getString(R.string.app_name_about),
                    "");
        }

    } //end onCreate

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

        threaded_application.currentActivity = activity_id_type.ABOUT;
        if (mainapp.isForcingFinish()) {        //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        refreshOverflowMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about_menu, menu);
        overflowMenu = menu;

        refreshOverflowMenu();

        return super.onCreateOptionsMenu(menu);
    }

    private void refreshOverflowMenu() {
        if (overflowMenu == null) return;

        mainapp.refreshCommonOverflowMenu(overflowMenu, findViewById(R.id.emergency_stop_button), findViewById(R.id.flashlight_button), findViewById(R.id.powerLayoutButton));
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
            mainapp.toggleFlashlightActionView(this, overflowMenu, findViewById(R.id.flashlight_button));
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

    @SuppressLint("HandlerLeak")
    class about_page_handler extends Handler {

        public about_page_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateActionViewButton(overflowMenu, findViewById(R.id.powerLayoutButton));
                        }
                    }
                    break;
                }

                case message_type.ESTOP_PAUSED:
                case message_type.ESTOP_RESUMED:
                    mainapp.setEmergencyStopStateActionViewButton(overflowMenu, findViewById(R.id.emergency_stop_button));
                    break;

                case message_type.REFRESH_OVERFLOW_MENU:
                    refreshOverflowMenu();
                    break;

                case message_type.REOPEN_THROTTLE:
                    if (threaded_application.currentActivity == activity_id_type.ABOUT)
                        endThisActivity();
                    break;

                case message_type.LOW_MEMORY:
                    endThisActivity();
                    break;

                default:
                    break;

            }
        }
    }

    public class CloseButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            endThisActivity();
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
