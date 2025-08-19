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

import static android.view.KeyEvent.KEYCODE_BACK;
import static jmri.enginedriver.threaded_application.context;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import java.lang.reflect.Method;
import java.util.Objects;

import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.screen_swipe_index_type;
import jmri.enginedriver.util.LocaleHelper;

public class web_activity extends AppCompatActivity implements android.gesture.GestureOverlayView.OnGestureListener {
    static final String activityName = "web_activity";

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private WebView webView;
    private String noUrl = "file:///android_asset/blank_page.html";
    private static boolean clearHistory = false;        // flags webViewClient to clear history when page load finishes
    private static String firstUrl = null;            // first url loaded that isn't noUrl
    private Menu WMenu;
    private static boolean savedWebMenuSelected;
    private int urlRestoreStep = 0;
    private static Bundle webBundle = new Bundle();

    protected GestureOverlayView ov;
    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    protected boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;

    private LinearLayout screenNameLine;
    private Toolbar toolbar;
    private LinearLayout statusLine;
    private int toolbarHeight;

//    Button closeButton;


    @Override
    public void onGesture(GestureOverlayView arg0, MotionEvent event) {
        gestureMove(event);
    }

    @Override
    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        gestureCancel(event);
    }

    // determine if the action was long enough to be a swipe
    @Override
    public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
        gestureEnd(event);
    }

    @Override
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
        gestureStart(event);
    }

    private void gestureStart(MotionEvent event) {
        gestureStartX = event.getX();
        gestureStartY = event.getY();
//        Log.d(threaded_application.applicationName, activityName + ": gestureStart(): x=" + gestureStartX + " y=" + gestureStartY);

        toolbarHeight = mainapp.getToolbarHeight(toolbar, statusLine,  screenNameLine);
        if (mainapp.prefFullScreenSwipeArea) {  // only allow swipe in the tool bar
            if (gestureStartY > toolbarHeight) {   // not in the toolbar area
                return;
            }
        }

        gestureInProgress = true;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.web_msg_handler != null)
            mainapp.web_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d(threaded_application.applicationName, activityName + ": gestureMove(): action " + event.getAction());
        if ( (mainapp != null) && (mainapp.web_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.web_msg_handler.removeCallbacks(gestureStopped);

            mVelocityTracker.addMovement(event);
            if ((event.getEventTime() - gestureLastCheckTime) > gestureCheckRate) {
                // monitor velocity and fail gesture if it is too low
                gestureLastCheckTime = event.getEventTime();
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                int velocityY = (int) velocityTracker.getYVelocity();
                // Log.d(threaded_application.applicationName, activityName + ": gestureMove(): gestureVelocity vel " + velocityX);
                if ((Math.abs(velocityX) < threaded_application.min_fling_velocity) && (Math.abs(velocityY) < threaded_application.min_fling_velocity)) {
                    gestureFailed(event);
                }
            }
            if (gestureInProgress) {
                // restart the gesture timeout timer
                mainapp.web_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d(threaded_application.applicationName, activityName + ": gestureEnd(): action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.web_msg_handler != null) && (gestureInProgress) ) {
            mainapp.web_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(screen_swipe_index_type.WEB, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.web_msg_handler != null)
            mainapp.web_msg_handler.removeCallbacks(gestureStopped);
        gestureInProgress = false;
    }

    void gestureFailed(MotionEvent event) {
        // end the gesture
        gestureInProgress = false;
    }

    //
    // GestureStopped runs when more than gestureCheckRate milliseconds
    // elapse between onGesture events (i.e. press without movement).
    //
    @SuppressLint("Recycle")
    private final Runnable gestureStopped = new Runnable() {
        @Override
        public void run() {
            if (gestureInProgress) {
                // end the gesture
                gestureInProgress = false;
                // create a MOVE event to trigger the underlying control
                if (webView != null) {
                    // use uptimeMillis() rather than 0 for time in
                    // MotionEvent.obtain() call in throttle gestureStopped:
                    MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, gestureStartX,
                            gestureStartY, 0);
                    try {
                        webView.dispatchTouchEvent(event);
                    } catch (IllegalArgumentException e) {
                        Log.d(threaded_application.applicationName, activityName + ": gestureStopped trigger IllegalArgumentException, OS " + android.os.Build.VERSION.SDK_INT);
                    }
                }
            }
        }
    };


    @SuppressLint("HandlerLeak")
    class web_handler extends Handler {

        public web_handler(Looper looper) {
            super(looper);
        }

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

                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateActionViewButton(WMenu, findViewById(R.id.powerLayoutButton));
                        }
                    }

                    break;
                }

                case message_type.REOPEN_THROTTLE:
                    if (threaded_application.currentActivity == activity_id_type.WEB)
                        reopenThrottlePage();
                    break;

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
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.SHUTDOWN:
                    shutdown();
                    break;
                case message_type.DISCONNECT:
                    disconnect();
                    break;
            }
        }
    }

    //	set the title, optionally adding the current time.
    private void setActivityTitle() {
        if (mainapp.getFastClockFormat() > 0)
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    "",
                    getApplicationContext().getResources().getString(R.string.app_name_web_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_web),
                    "");
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
        Log.d(threaded_application.applicationName, activityName + ": onCreate()");

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        mainapp.applyTheme(this);

        super.onCreate(savedInstanceState);

        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        setContentView(R.layout.web_activity);

        webView = findViewById(R.id.webActivityWebView);
        String databasePath = webView.getContext().getDir("databases", Context.MODE_PRIVATE).getPath();
        webView.getSettings().setDatabasePath(databasePath);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setBuiltInZoomControls(true); //Enable Multitouch if supported
        webView.getSettings().setUseWideViewPort(true);        // Enable greater zoom-out
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        if (!mainapp.firstWebActivity) {
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
            // (this can happen if the initial web page pref is set to a non-existent url)
            private boolean handleLoadingErrorRetries() {
                if (++loadRetryCnt >= 3) {   // if same page is reloading (due to errors)
                    clearHistory = false;       // stop trying to clear history
                    loadRetryCnt = 0;        // reset count for next url load
                    return true;                // don't load the page
                }
                return false;                   // load in webView
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

        //longpress webview to reload
        webView.setOnLongClickListener(new WebView.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                webView.reload();
                return true;
            }
        });

        //Set the buttons
//        closeButton = findViewById(R.id.webview_button_close);
//        web_activity.close_button_listener close_click_listener = new web_activity.close_button_listener();
//        closeButton.setOnClickListener(close_click_listener);

        //put pointer to this activity's handler in main app's shared variable
//        mainapp.web_msg_handler = new web_handler();

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
        }

        mainapp.prefFullScreenSwipeArea = prefs.getBoolean("prefFullScreenSwipeArea",
                getResources().getBoolean(R.bool.prefFullScreenSwipeAreaDefaultValue));

    } // end onCreate

    @Override
    public void onResume() {
        Log.d(threaded_application.applicationName, activityName + ": onResume()");
        mainapp.applyTheme(this);

        super.onResume();
        threaded_application.activityResumed(activityName);
        mainapp.removeNotification(this.getIntent());

        threaded_application.currentActivity = activity_id_type.WEB;

        setActivityTitle();

//        if (closeButton != null) {
//            if (mainapp.webMenuSelected) {
//                closeButton.setVisibility(View.VISIBLE);
//            } else {
//                closeButton.setVisibility(View.GONE);
//            }
//        }

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        if (WMenu != null) {
            mainapp.displayEStop(WMenu);
        }
        resumeWebView();
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);     //create this here so onPause/onResume for webViews can control it
        }
        cookieManager.setAcceptCookie(true);

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.web_overlay);
        boolean swipeWeb = prefs.getBoolean("swipe_through_web_preference",
                getResources().getBoolean(R.bool.prefSwipeThroughWebDefaultValue));
        if (swipeWeb) {
            ov.addOnGestureListener(this);
            ov.setEventsInterceptionEnabled(true);
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
        } else {
            ov.removeOnGestureListener(this);
            ov.setEventsInterceptionEnabled(false);
        }
    }

    @Override
    public void onPause() {
        Log.d(threaded_application.applicationName, activityName + ": onPause()");
        super.onPause();
        threaded_application.activityPaused(activityName);

        pauseWebView();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync();
        }
        if (webView != null) {
            webView.saveState(webBundle);           // save locally for use if finishing
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(threaded_application.applicationName, activityName + ": onStart()");
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.web_msg_handler == null)
            mainapp.web_msg_handler = new web_handler(Looper.getMainLooper());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!mainapp.setActivityOrientation(this)) {   //set screen orientation based on prefs
            Intent in = mainapp.getThrottleIntent();
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(threaded_application.applicationName, activityName + ": onDestroy()");

        if (webView != null) {
            final ViewGroup webGroup = (ViewGroup) webView.getParent();
            if (webGroup != null) {
                webGroup.removeView(webView);
            }
        }
        if (mainapp.web_msg_handler !=null) {
            mainapp.web_msg_handler.removeCallbacksAndMessages(null);
            mainapp.web_msg_handler = null;
        } else {
            Log.d(threaded_application.applicationName, activityName + ": onDestroy(): mainapp.web_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        if (webView == null) return;
        super.onSaveInstanceState(outState);
        Bundle bundle = new Bundle();
        webView.saveState(bundle);
        outState.putBundle("webViewState", bundle);
    }

    protected void onRestoreInstanceState(@NonNull Bundle state) {
        if (webView == null) return;
        super.onRestoreInstanceState(state);
        Bundle bundle = new Bundle();
        webView.saveState(bundle);
        state.putBundle("webViewState", bundle);
    }

    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            navigateAway();
            mainapp.buttonVibration();
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
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KEYCODE_BACK) {
            if (webView.canGoBack() && !clearHistory) {
                webView.goBack();
                return true;
            }
            navigateAway(true, null); // don't really finish the activity here
            return true;
        }
        mainapp.exitDoubleBackButtonInitiated = 0;
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.web_menu, menu);
        WMenu = menu;
        mainapp.displayEStop(menu);

        mainapp.displayPowerStateMenuButton(menu);
        mainapp.displayThrottleMenuButton(menu, "swipe_through_web_preference");
        mainapp.setPowerMenuOption(menu);
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

        mainapp.displayFlashlightMenuButton(menu);
        if (findViewById(R.id.flashlight_button) == null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mainapp.setFlashlightActionViewButton(menu, findViewById(R.id.flashlight_button));
                }
            }, 100);
        } else {
            mainapp.setFlashlightActionViewButton(menu, findViewById(R.id.flashlight_button));
        }

        mainapp.setRoutesMenuOption(menu);
        mainapp.setTurnoutsMenuOption(menu);
        mainapp.setPowerMenuOption(menu);

        adjustToolbarSize(menu);

        return  super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        switch (item.getItemId()) {
            case R.id.throttle_button_mnu:
            case R.id.throttle_mnu:
                navigateAway(true, null);
                return true;
            case R.id.turnouts_mnu:
                navigateAway(true, turnouts.class);
                return true;
            case R.id.routes_mnu:
                navigateAway(true, routes.class);
                return true;
            case R.id.exit_mnu:
                mainapp.checkAskExit(this);
                return true;
            case R.id.power_control_mnu:
                navigateAway(false, power_control.class);
                return true;
/*            case R.id.preferences_mnu:
                navigateAway(false, SettingsActivity.class);
                return true;*/
            case R.id.settings_mnu:
                in = new Intent().setClass(this, SettingsActivity.class);
                startActivityForResult(in, 0);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                return true;
            case R.id.EmerStop:
                mainapp.sendEStopMsg();
                mainapp.buttonVibration();
                return true;
            case R.id.logviewer_menu:
                navigateAway(false, LogViewerActivity.class);
                return true;
            case R.id.about_mnu:
                navigateAway(false, about_page.class);
                return true;
            case R.id.flashlight_button:
                mainapp.toggleFlashlightActionView(this, WMenu, findViewById(R.id.flashlight_button));
                mainapp.buttonVibration();
                return true;
            case R.id.powerLayoutButton:
                if (!mainapp.isPowerControlAllowed()) {
                    mainapp.powerControlNotAllowedDialog(WMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                mainapp.buttonVibration();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //handle return from menu items
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mainapp.webMenuSelected = savedWebMenuSelected;     // restore flag
    }

    // helper methods to handle navigating away from this activity
    private void navigateAway() {
        threaded_application.activityInTransition(activityName);
        mainapp.webMenuSelected = false;    // not returning so clear flag
        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void navigateAway(boolean returningToOtherActivity, Class activityClass) {
        Intent in;
        if (activityClass != null ) {
            in = new Intent().setClass(this, activityClass);
        } else {  // if null assume we want the throttle activity
            in = mainapp.getThrottleIntent();
        }
        if (returningToOtherActivity) {                 // if not returning
            startACoreActivity(this, in, false, 0);
        } else {
            threaded_application.activityInTransition(activityName);
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
        // try the pref setting
        if (urlRestoreStep == 2) {
            mainapp.defaultWebViewURL = prefs.getString("InitialWebPage",
                    getApplicationContext().getResources().getString(R.string.prefInitialWebPageDefaultValue));
            String url = mainapp.createUrl(mainapp.defaultWebViewURL);
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

    private void shutdown() {
        webView.stopLoading();
        this.finish();
    }

    // helper app to initialize statics (in case GC has not run since app last shutdown)
    // call before instantiating any instances of class
    public static void initStatics() {
        clearHistory = false;
        firstUrl = null;
        webBundle = new Bundle();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
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

    // common startActivity()
    // used for swipes for the main activities only - Throttle, Turnouts, Routs, Web
    void startACoreActivity(Activity activity, Intent in, boolean swipe, float deltaX) {
        if (activity != null && in != null) {
            threaded_application.activityInTransition(activityName);
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ActivityOptions options;
            if (deltaX>0) {
                options = ActivityOptions.makeCustomAnimation(context, R.anim.push_right_in, R.anim.push_right_out);
            } else {
                options = ActivityOptions.makeCustomAnimation(context, R.anim.push_left_in, R.anim.push_left_out);
            }
            startActivity(in, options.toBundle());
//            overridePendingTransition(mainapp.getFadeIn(swipe, deltaX), mainapp.getFadeOut(swipe, deltaX));
        }
    }

    void reopenThrottlePage() {
        Intent in = mainapp.getThrottleIntent();
        startACoreActivity(this, in, false, 0);
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
