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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
    private Menu AMenu;
    private Toolbar toolbar;

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

        if (AMenu != null) {
            mainapp.displayEStop(AMenu);
            mainapp.displayFlashlightMenuButton(AMenu);
            mainapp.setFlashlightActionViewButton(AMenu, findViewById(R.id.flashlight_button));
            mainapp.displayPowerStateMenuButton(AMenu);
            mainapp.setPowerStateActionViewButton(AMenu, findViewById(R.id.powerLayoutButton));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about_menu, menu);
        AMenu = menu;
        mainapp.displayEStop(menu);
        mainapp.displayFlashlightMenuButton(AMenu);
        mainapp.setFlashlightActionViewButton(AMenu, findViewById(R.id.flashlight_button));
        mainapp.displayPowerStateMenuButton(menu);
//        mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        if (findViewById(R.id.powerLayoutButton) == null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
                }
            }, 100);
        } else {
            mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        }

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
            mainapp.toggleFlashlightActionView(this, AMenu, findViewById(R.id.flashlight_button));
            mainapp.buttonVibration();
            return true;
        } else if (item.getItemId() == R.id.powerLayoutButton) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(AMenu);
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
                            mainapp.setPowerStateActionViewButton(AMenu, findViewById(R.id.powerLayoutButton));
                        }
                    }
                    break;
                }

                case message_type.REOPEN_THROTTLE:
                    if (threaded_application.currentActivity == activity_id_type.ABOUT)
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
