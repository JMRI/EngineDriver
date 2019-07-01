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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

public class reconnect_status extends Activity {

    private threaded_application mainapp;  // hold pointer to mainapp
    private String prog = "";
    private boolean backOk = true;
    private boolean navigatingAway = false; // true if another activity was selected (false in onPause if going into background)
    private boolean retryFirst = false;

    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class reconnect_status_handler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE:
                    if (!retryFirst) {              // Got a message from WiThrottle server so the socket must already be ok.  This means the
                        reconnected();              // RETRY/RECONNECT sequence was over before this Screen came up, so just resume normal ops.
                    }
                    break;
                case message_type.WIT_CON_RETRY:
                    retryFirst = true;
                    refresh_reconnect_status(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refresh_reconnect_status(msg.obj.toString());
                    reconnected();
                    break;
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
            }
        }
    }

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
            if (mainapp.reconnect_status_msg_handler != null) {
                mainapp.reconnect_status_msg_handler.postDelayed(delayCloseScreen, 500L);
            } else {
                Log.d("Engine_Driver", "Reconnect: handler already null");
                closeScreen();
            }
        }
    }

    @SuppressLint("Recycle")
    private Runnable delayCloseScreen = new Runnable() {
        @Override
        public void run() {
            closeScreen();
        }
    };

    private void closeScreen() {
        navigatingAway = true;
        this.finish();                  //end this activity
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
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
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_reconnect_status)); // needed in case the langauge was changed from the default

        setContentView(R.layout.reconnect_page);

        //put pointer to this activity's handler in main app's shared variable (If needed)
        mainapp.reconnect_status_msg_handler = new reconnect_status_handler();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String msg = extras.getString("status");
            if (msg != null) {
                refresh_reconnect_status(msg);
            }
        }
        retryFirst = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) { //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        navigatingAway = false;

        this.backOk = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.isFinishing() && !navigatingAway) {        //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "reconnect_status.onDestroy() called");

        mainapp.reconnect_status_msg_handler = null;
        super.onDestroy();
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK && this.backOk) {
            mainapp.checkExit(this);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    private void disconnect() {
        this.finish();
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}
