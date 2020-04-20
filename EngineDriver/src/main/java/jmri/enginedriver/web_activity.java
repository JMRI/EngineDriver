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
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.lang.reflect.Method;

import jmri.enginedriver.logviewer.ui.LogViewerActivity;

public class web_activity extends Activity {

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private WebView webView;
    private String noUrl = "file:///android_asset/blank_page.html";
    private static final float initialScale = 1.5f;
    private static float scale = initialScale;        // used to restore web zoom level
    private static boolean clearHistory = false;        // flags webViewClient to clear history when page load finishes
    private static String firstUrl = null;            // first url loaded that isn't noUrl
    private Menu WMenu;
    private static boolean savedWebMenuSelected;
    private int urlRestoreStep = 0;
    private static Bundle webBundle = new Bundle();

    Button closeButton;

    @SuppressLint("HandlerLeak")
    class web_handler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    String response_str = s.substring(0, Math.min(s.length(), 2));
                    if ("PW".equals(response_str)       // PW - web server port info
                        || ("HTMRC".equals(s))) {        // If connected to the MRC Wifi adapter, treat as PW, which isn't coming
                        if (urlRestoreStep == 3) {
                            urlRestore(true);
                        }
                    }
                    break;
                }
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    break;
                case message_type.INITIAL_WEB_WEBPAGE:
                    initStatics();
                    urlRestore(true);
                    break;
                case message_type.TIME_CHANGED:
                    setActivityTitle();
                    break;
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
            }
        }
    }

    //	set the title, optionally adding the current time.
    private void setActivityTitle() {
        if (mainapp.fastClockFormat > 0)
            setTitle(getApplicationContext().getResources().getString(R.string.app_name_web_short) + "  " + mainapp.getFastClockTime());
        else
            setTitle(getApplicationContext().getResources().getString(R.string.app_name_web));
    }

    private void witRetry(String s) {
        webView.stopLoading();
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Engine_Driver", "web_activity.onCreate()");
        super.onCreate(savedInstanceState);

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }
        mainapp.applyTheme(this);

        setContentView(R.layout.web_activity);

        webView = findViewById(R.id.webview);
        String databasePath = webView.getContext().getDir("databases", Context.MODE_PRIVATE).getPath();
        webView.getSettings().setDatabasePath(databasePath);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setBuiltInZoomControls(true); //Enable Multitouch if supported
        webView.getSettings().setUseWideViewPort(true);        // Enable greater zoom-out
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        if (savedInstanceState != null) {
            if (savedInstanceState.getSerializable("scale") != null) {
                scale = (float) savedInstanceState.getSerializable(("scale"));
            }
        }
        if (mainapp.firstWebActivity == false) {
            webView.clearCache(true);   // force fresh javascript download on first connection
            mainapp.firstWebActivity = true;
        }

        // enable remote debugging of all webviews
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        // open all links inside the current view (don't start external web browser)
        WebViewClient EDWebClient = new WebViewClient() {
            private int loadRetryCnt = 0;
            private String currentUrl = null;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!noUrl.equals(url) || urlRestoreStep >= 3) {    // if url is legit or out of options
                    if (!url.equals(currentUrl)) {          // if first try loading page
                        loadRetryCnt = 0;                // reset count for next url load
                        currentUrl = url;
                    }
                    if (firstUrl == null) {                // if this is the first legit url
                        firstUrl = url;
                        scale = initialScale;
                        webView.setInitialScale((int) (100 * scale));
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
                // if webview didn't get restored but options remain, try again
                else {
                    urlRestore();
                }
            }
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleLoadingErrorRetries();
            }

            // above form of shouldOverrideUrlloading is deprecated so support the new form if available
            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleLoadingErrorRetries();
            }

            // stop page from continually reloading when loading errors occur
            // (this can happen if the initial web page pref is set to a non-existant url)
            private boolean handleLoadingErrorRetries() {
                if (++loadRetryCnt >= 3) {   // if same page is reloading (due to errors)
                    clearHistory = false;       // stop trying to clear history
                    loadRetryCnt = 0;        // reset count for next url load
                    return true;                // don't load the page
                }
                return false;                   // load in webView
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                scale = newScale;
            }
        };

        noUrl = getApplicationContext().getResources().getString(R.string.blank_page_url);
        webView.setWebViewClient(EDWebClient);
        // restore the url if possible
        // first try loading from the savedInstanceState if it exists
        urlRestoreStep = 0;
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            // try remaining methods
            urlRestore(true);
        }
        else {
            webView.setInitialScale((int) (100 * scale));   // apply scale to restored webView
        }

        //longpress webview to reload
        webView.setOnLongClickListener(new WebView.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                webView.reload();
                return true;
            }
        });

        //Set the buttons
        closeButton = findViewById(R.id.webview_button_close);
        web_activity.close_button_listener close_click_listener = new web_activity.close_button_listener();
        closeButton.setOnClickListener(close_click_listener);


        //put pointer to this activity's handler in main app's shared variable
        mainapp.web_msg_handler = new web_handler();
    }

    @Override
    public void onResume() {
        Log.d("Engine_Driver", "web_activity.onResume() called");
        super.onResume();

        setActivityTitle();

        if (closeButton != null) {
            if (mainapp.webMenuSelected) {
                closeButton.setVisibility(View.VISIBLE);
            } else {
                closeButton.setVisibility(View.GONE);
            }
        }

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        if (!mainapp.setActivityOrientation(this)) {   //set screen orientation based on prefs
            this.finish();
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
        else {
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
            if (WMenu != null) {
                mainapp.displayEStop(WMenu);
            }
            resumeWebView();
            CookieSyncManager.getInstance().startSync();
        }
    }

    @Override
    public void onPause() {
        Log.d("Engine_Driver", "web_activity.onPause() called");
        super.onPause();
        pauseWebView();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("scale", scale);   // save in bundle for use if just rotationg
        if (webView != null) {
            webView.saveState(webBundle);           // save locally for use if finishing
            webView.saveState(outState);            // save in bundle for use if rotating
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Engine_Driver", "web_activity.onDestroy() called");

        if (webView != null) {
            final ViewGroup webGroup = (ViewGroup) webView.getParent();
            if (webGroup != null) {
                webGroup.removeView(webView);
            }
//            webView.removeAllViews();
        }
        if (mainapp.web_msg_handler !=null) {
            mainapp.web_msg_handler.removeCallbacksAndMessages(null);
            mainapp.web_msg_handler = null;
        } else {
            Log.d("Engine_Driver", "onDestroy: mainapp.web_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }


    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            navigateAway();
        }
    }

    private void pauseWebView() {
        if (webView != null) {
            try {
                Method method = WebView.class.getMethod("onPause");
                method.invoke(webView);
            }
            catch (Exception e) {
                webView.pauseTimers();
            }
        }
    }

    private void resumeWebView() {
        if (webView != null) {
            try {
                Method method = WebView.class.getMethod("onResume");
                method.invoke(webView);
            }
            catch (Exception e) {
                webView.resumeTimers();
            }
        }
    }

    //Handle pressing of the back button to end this activity
    @SuppressWarnings("deprecation")
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack() && !clearHistory) {
                webView.goBack();
                webView.setInitialScale((int) (100 * scale));    // restore scale
                return true;
            }
            navigateAway();
            return true;
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
                navigateAway();
                return true;
            case R.id.turnouts_mnu:
                navigateAway(true, turnouts.class);
                return true;
            case R.id.routes_mnu:
                navigateAway(true, routes.class);
                return true;
            case R.id.exit_mnu:
                mainapp.checkExit(this);
                return true;
            case R.id.power_control_mnu:
                navigateAway(false, power_control.class);
                return true;
            case R.id.preferences_mnu:
                navigateAway(false, preferences.class);
                return true;
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                return true;
            case R.id.logviewer_menu:
                navigateAway(false, LogViewerActivity.class);
                return true;
            case R.id.about_mnu:
                navigateAway(false, about_page.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //handle return from menu items
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mainapp.webMenuSelected = savedWebMenuSelected;     // restore flag
    }

    // helper methods to handle navigating away from this activity
    private void navigateAway() {
        mainapp.webMenuSelected = false;    // not returning so clear flag
        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void navigateAway(boolean doFinish, Class activityClass) {
        Intent in = new Intent().setClass(this, activityClass);
        if (doFinish) {                 // if not returning
            startActivity(in);
            navigateAway();
        } else {
            savedWebMenuSelected = mainapp.webMenuSelected; // returning so preserve flag
            mainapp.webMenuSelected = true;     // ensure we return regardless of auto-web setting and orientation changes
            startActivityForResult(in, 0);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    // attempt to reload url first from local store, then try from prefs, and if that fails then use noUrl
    private void urlRestore() {
        urlRestore(false);
    }
    private void urlRestore(boolean restart) {
        if(restart) {
            urlRestoreStep = 1;
        }
        // try the local store
        if (urlRestoreStep == 1 && webBundle != null && webView.restoreState(webBundle) == null) {
            urlRestoreStep = 2;
        }
        else {
            webView.setInitialScale((int) (100 * scale));   // apply scale to restored webView
        }
        // try the pref setting
        if (urlRestoreStep == 2) {
            String url = mainapp.createUrl(prefs.getString("InitialWebPage", getApplicationContext().getResources().getString(R.string.prefInitialWebPageDefaultValue)));
            if (url != null) {      // if port is valid
                webView.loadUrl(url);
            }
            else {
                urlRestoreStep = 3;
            }
        }
        // use noUrl
        if (urlRestoreStep == 3) {
            webView.loadUrl(noUrl);
        }
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
        firstUrl = null;
        webBundle = new Bundle();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}
