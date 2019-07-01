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

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

public class about_page extends Activity {

    private threaded_application mainapp; // hold pointer to mainapp
    private Menu AMenu;

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("DefaultLocale")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) this.getApplication();

        mainapp.applyTheme(this);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_about)); // needed in case the langauge was changed from the default

        setContentView(R.layout.about_page);

        // format and show version info
        TextView v = findViewById(R.id.about_info);
        String s;
        // ED version info
        try {
            s = "Engine Driver: v"
                    + mainapp.getPackageManager().getPackageInfo(mainapp.getPackageName(), 0).versionName;
        } catch (Exception e) {
            s = "";
        }
        if (mainapp.host_ip != null) {
            // JMRI version info
            HashMap<String, String> metadata = threaded_application.metadata;
            if (metadata != null && metadata.size() > 0) {
                s += "\nJMRI v" + metadata.get("JMRIVERCANON") + "    build: " + metadata.get("JMRIVERSION");
                if (metadata.get("activeProfile") != null) {
                    s += "\nActive Profile: " + metadata.get("activeProfile");
                }
            }
            // WiT info
            if (mainapp.withrottle_version != 0.0) {
                s += "\nWiThrottle: v" + mainapp.withrottle_version;
                s += String.format("    Heartbeat: %d secs", mainapp.heartbeatInterval);
            }
            s += String.format("\nHost: %s", mainapp.host_ip);
        }
        s += String.format("\nSSID: %s Net: %s ", mainapp.client_ssid, mainapp.client_type);
        if (mainapp.client_address_inet4 != null) {
            s += String.format("IP: %s", mainapp.client_address_inet4.toString().replaceAll("/",""));
        }
        s += String.format("\nOS: %s, SDK: %s ", android.os.Build.VERSION.RELEASE, Build.VERSION.SDK_INT);

        // show info
        v.setText(s);

        // show ED webpage
        WebView webview = findViewById(R.id.about_webview);
        webview.loadUrl(getApplicationContext().getResources().getString(R.string.about_page_url));

    }

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) {        //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        if (AMenu != null) {
            mainapp.displayEStop(AMenu);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.isFinishing()) {        //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about_menu, menu);
        AMenu = menu;
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
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
        return (super.onKeyDown(key, event));
    }


}
