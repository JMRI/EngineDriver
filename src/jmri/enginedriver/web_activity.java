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

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class web_activity extends Activity {

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private WebView webView;
    private static final String noUrl = "file:///android_asset/blank_page.html";
    private static final float initialScale = 1.5f;
    private static float scale = initialScale;        // used to restore web zoom level
    private static boolean clearHistory = false;        // flags webViewClient to clear history when page load finishes
    private static String firstUrl = null;            // first url loaded that isn't noUrl
    private static String currentUrl = null;
    private String currentTime = "";
    private Menu WMenu;
    private boolean navigatingAway = false;        // flag for onPause: set to true when another activity is selected, false if going into background
    private boolean webInited = false;

    @SuppressLint("HandlerLeak")
    class web_handler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    String response_str = s.substring(0, Math.min(s.length(), 2));
                    if ("PW".equals(response_str))        // PW - web server port info
                        if (!webInited) {
                            initWeb();
                            webInited = true;
                        }
                    if ("HTMRC".equals(s))        // If connected to the MRC Wifi adapter, treat as PW, which isn't coming
                        if (!webInited) {
                            initWeb();
                            webInited = true;
                        }
                    break;
                }
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    break;
                case message_type.INITIAL_WEBPAGE:
                    initWeb();
                    break;
                case message_type.CURRENT_TIME:
                    currentTime = msg.obj.toString();
                    setTitle();
                    break;
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
            }
        }
    }

    //	set the title, optionally adding the current time.
    public void setTitle() {
        if (mainapp.displayClock)
            setTitle(getApplicationContext().getResources().getString(R.string.app_name) + "  " + currentTime);
        else
            setTitle(getApplicationContext().getResources().getString(R.string.app_name_web));
    }

    private void reloadWeb() {
        webView.stopLoading();
        load_webview(); // reload
    }

    private void initWeb() {
        // reload from the initial webpage
        currentUrl = null;
        reloadWeb();
    }

    private void witRetry(String s) {
        webView.stopLoading();
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        navigatingAway = true;
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Engine_Driver","web_activity.onCreate()");
        super.onCreate(savedInstanceState);

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }
        setContentView(R.layout.web_activity);

        mainapp.applyTheme(this);

        webView = (WebView) findViewById(R.id.webview);
        String databasePath = webView.getContext().getDir("databases", Context.MODE_PRIVATE).getPath();
        webView.getSettings().setDatabasePath(databasePath);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true); //Enable Multitouch if supported
        webView.getSettings().setUseWideViewPort(true);        // Enable greater zoom-out
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        webView.setInitialScale((int) (100 * scale));

        // open all links inside the current view (don't start external web browser)
        WebViewClient EDWebClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!noUrl.equals(url)) {                // if url is legit
                    currentUrl = url;
                    if (firstUrl == null) {                // if this is the first legit url
                        firstUrl = url;
                        clearHistory = true;
                    }
                    if (clearHistory) {                    // keep clearing history until off this page
                        if (url.equals(firstUrl)) {        // (works around Android bug)
                            webView.clearHistory();
                        } else {
                            clearHistory = false;
                        }
                    }
                }
            }
        };

        webView.setWebViewClient(EDWebClient);
        if (currentUrl == null || savedInstanceState == null || webView.restoreState(savedInstanceState) == null)
            load_webview();            // reload if no saved state or no page had loaded when state was saved

        //put pointer to this activity's handler in main app's shared variable
        mainapp.web_msg_handler = new web_handler();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d("Engine_Driver", "web_activity.onResume() called");
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }
        if (!mainapp.setActivityOrientation(this, true))    //set screen orientation based on prefs
        {
            navigatingAway = true;
            this.finish();                                // if autoweb and portrait, switch to throttle screen
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return;
        }
        navigatingAway = false;
        currentTime = "";
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CURRENT_TIME);    // request time update
        if (WMenu != null) {
            mainapp.displayEStop(WMenu);
        }

// don't load here - onCreate already handled it.  Load might not be finished yet
// in which case call load_webview here just creates extra work since url will still be null
// causing load_webview to load the page again 
//	  load_webview();

        if (webView != null) {
            if (!callHiddenWebViewOnResume())
                webView.resumeTimers();
            if (noUrl.equals(webView.getUrl()) && webView.canGoBack()) {    //unload static url loaded by onPause
                webView.goBack();
            }
        }
        CookieSyncManager.getInstance().startSync();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null)
            webView.saveState(outState);        // save history

    }

    @Override
    public void onPause() {
        Log.d("Engine_Driver", "web_activity.onPause() called");
        super.onPause();
        if (webView != null) {
            if (!callHiddenWebViewOnPause())
                webView.pauseTimers();
            String url = webView.getUrl();
            if (url != null && !noUrl.equals(url)) {    // if any url has been loaded
                webView.loadUrl(noUrl);                // load a static url to stop any javascript
            }
        }
        CookieSyncManager.getInstance().stopSync();

        if (!this.isFinishing() && !navigatingAway) {        //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    /**
     * Called when the activity is finished.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "web_activity.onDestroy() called");

        if (webView != null) {
            scale = webView.getScale();    // save scale for next onCreate
        }
        mainapp.web_msg_handler = null;
        super.onDestroy();
    }

    private boolean callHiddenWebViewOnPause() {
        try {
            Method method = WebView.class.getMethod("onPause");
            method.invoke(webView);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean callHiddenWebViewOnResume() {
        try {
            Method method = WebView.class.getMethod("onResume");
            method.invoke(webView);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //Handle pressing of the back button to end this activity
    @SuppressWarnings("deprecation")
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack() && !clearHistory) {
                scale = webView.getScale();    // save scale
                webView.goBack();
                webView.setInitialScale((int) (100 * scale));    // restore scale
                return true;
            }

            mainapp.web_msg_handler = null; //clear out pointer to this activity
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.web_menu, menu);
        WMenu = menu;
        mainapp.displayEStop(menu);
        mainapp.setRoutesMenuOption(menu);
        mainapp.setTurnoutsMenuOption(menu);
        mainapp.setPowerMenuOption(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        switch (item.getItemId()) {
            case R.id.throttle_mnu:
                navigatingAway = true;
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.turnouts_mnu:
                in = new Intent().setClass(this, turnouts.class);
                navigatingAway = true;
                startActivity(in);
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.routes_mnu:
                in = new Intent().setClass(this, routes.class);
                navigatingAway = true;
                startActivity(in);
                this.finish();
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.exit_mnu:
                mainapp.checkExit(this);
                break;
            case R.id.power_control_mnu:
                in = new Intent().setClass(this, power_control.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.preferences_mnu:
                in = new Intent().setClass(this, preferences.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                break;
            case R.id.about_mnu:
                in = new Intent().setClass(this, about_page.class);
                navigatingAway = true;
                startActivity(in);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // load the url
    private void load_webview() {
        String url = currentUrl;
        if (url == null) {
            url = mainapp.createUrl(prefs.getString("InitialWebPage", getApplicationContext().getResources().getString(R.string.prefInitialWebPageDefaultValue)));
            Log.d("Engine_Driver", "initial web url set to '" + url + "'");
            if (url == null) {        //if port is invalid
                url = noUrl;
            }

            if (firstUrl == null) {
                scale = initialScale;
                webView.setInitialScale((int) (100 * scale));
            }
            firstUrl = null;
        }
        webView.loadUrl(url);
    }

    private void disconnect() {
        webView.stopLoading();
        this.finish();
    }

    // helper app to initialize statics (in case GC has not run since app last shutdown)
    // call before instantiating any instances of class
    public static void initStatics() {
        scale = initialScale;
        clearHistory = false;
        currentUrl = null;
        firstUrl = null;
    }

}
