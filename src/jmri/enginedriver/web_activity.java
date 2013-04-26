/*Copyright (C) 2012 M. Steve Todd
  mstevetodd@enginedriver.rrclubs.org
  
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
  private static float scale = initialScale;		// used to restore web zoom level
  private static boolean clearHistory = false;		// flags webViewClient to clear history when page load finishes
  private static String currentUrl = null;
  private boolean currentUrlUpdate = false;
  private boolean orientationChange = false;
 
  class web_handler extends Handler {

  public void handleMessage(Message msg) {
	  switch(msg.what) {
		  case message_type.RESPONSE: {  	//handle messages from WiThrottle server
			  String s = msg.obj.toString();
			  String response_str = s.substring(0, Math.min(s.length(), 2));
			  if("PW".equals(response_str))		// PW - web server port info
				  reloadWebpage();
			  break;
		  }
		  case message_type.WIT_CON_RETRY:
	  	  case message_type.INITIAL_WEBPAGE:
			  reloadWebpage(); 
			  break;
		  case message_type.DISCONNECT:
		  case message_type.SHUTDOWN:
			  disconnect();
			  break;
		  }
	  };
  }

  private void reloadWebpage() {
	  webView.stopLoading();
	  clearHistory = true;
	  currentUrl = null;
	  load_webview();		// reload the page
  }
  
  /** Called when the activity is first created. */
  @SuppressLint("SetJavaScriptEnabled") @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mainapp=(threaded_application)this.getApplication();
    prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
    orientationChange = false;
    if(mainapp.doFinish) {		// expedite
    	return;
    }
    setContentView(R.layout.web_activity);
  
	webView = (WebView) findViewById(R.id.webview);
	webView.getSettings().setJavaScriptEnabled(true);
	webView.getSettings().setBuiltInZoomControls(true); //Enable Multitouch if supported
	webView.getSettings().setUseWideViewPort(true);		// Enable greater zoom-out
	webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
	webView.setInitialScale((int)(100 * scale));
//	webView.getSettings().setLoadWithOverviewMode(true);	// size image to fill width

	// open all links inside the current view (don't start external web browser)
	WebViewClient EDWebClient = new WebViewClient()	{
		@Override
		public boolean shouldOverrideUrlLoading(WebView  view, String  url) {
			return false;
		}
		
		@Override
		public void onPageFinished(WebView view, String url)
		{
			if(!noUrl.equals(url)) {
				if(clearHistory)
				{
					view.clearHistory();
					clearHistory = false;
				}
				if(currentUrlUpdate)
					currentUrl = url;
			}
			else
				clearHistory = true;
		}
	};
	webView.setWebViewClient(EDWebClient);
	currentUrlUpdate = true;		// ok to update currentUrl
	if(currentUrl == null || savedInstanceState == null || webView.restoreState(savedInstanceState) == null)
		load_webview();			// reload if no saved state or no page had loaded when state was saved
	
    //put pointer to this activity's handler in main app's shared variable
	mainapp.web_msg_handler=new web_handler();
};

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  public void onResume() {
	  Log.d("Engine_Driver","web_activity.onResume() called");
 	  super.onResume();
	  if(mainapp.doFinish) {	//expedite
		  this.finish();
		  return;
	  }
 	  mainapp.setActivityOrientation(this, true);  	//set screen orientation based on prefs

// don't load here - onCreate already handled it.  Load might not be finished yet
// in which case call load_webview here just creates extra work since url will still be null
// causing load_webview to load the page again 
//	  load_webview();
 	  
	  if(!callHiddenWebViewOnResume())
		  webView.resumeTimers();
	  CookieSyncManager.getInstance().startSync();
 };

 @Override
  public void onSaveInstanceState(Bundle outState) {
	  super.onSaveInstanceState(outState);
	  if(webView != null)
		  webView.saveState(outState);		// save history
	  orientationChange = true;
	  
  }
  
  @Override
  public void onPause() {
	  super.onPause();
	  if(webView != null) {
		  if(!callHiddenWebViewOnPause())
			  webView.pauseTimers();
	  }
	  CookieSyncManager.getInstance().stopSync();
}
   
  /** Called when the activity is finished. */
  @Override
  public void onDestroy() {
	  Log.d("Engine_Driver","web_activity.onDestroy() called");

	  if(webView != null) {
		  scale = webView.getScale();	// save scale for next onCreate
		  if(!orientationChange) {		// screen is exiting
			  webView.loadUrl(noUrl);	//load a static url else any javascript on current page would keep running
		  }
	  }
	  mainapp.web_msg_handler = null;
	  super.onDestroy();
  }

  private boolean callHiddenWebViewOnPause(){
      try {
          Method method = WebView.class.getMethod("onPause");
          method.invoke(webView);
      } 
      catch (Exception e) {
      	return false;
      }
	    return true;
  }
  private boolean callHiddenWebViewOnResume(){
      try {
          Method method = WebView.class.getMethod("onResume");
          method.invoke(webView);
      } 
      catch (Exception e) {
  	    return false;
	    }
      return true;
  }
  
  //Handle pressing of the back button to end this activity
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
	  if(key==KeyEvent.KEYCODE_BACK)
	  {
			if(webView.canGoBack() && !clearHistory) {
				scale = webView.getScale();	// save scale
				webView.goBack();
				webView.setInitialScale((int)(100 * scale));	// restore scale
				return true;
			}
	  
	      mainapp.web_msg_handler = null; //clear out pointer to this activity  
	      this.finish();  //end this activity
	      connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
	  }
	  return(super.onKeyDown(key, event));
  };

  @Override
  public boolean onCreateOptionsMenu(Menu menu){
	  MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.web_menu, menu);
	  return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
	  // Handle all of the possible menu actions.
	  Intent in;
	  switch (item.getItemId()) {
	  case R.id.throttle_mnu:
		  this.finish();
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.turnouts_mnu:
		  in = new Intent().setClass(this, turnouts.class);
		  startActivity(in);
		  this.finish();
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.routes_mnu:
		  in=new Intent().setClass(this, routes.class);
		  startActivity(in);
		  this.finish();
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.exit_mnu:
		  mainapp.checkExit(this);
		  break;
	  case R.id.power_control_mnu:
		  in=new Intent().setClass(this, power_control.class);
		  startActivity(in);
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
      case R.id.preferences_mnu:
    	  in=new Intent().setClass(this, preferences.class);
     	  startActivityForResult(in, 0);
     	  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
	  case R.id.about_mnu:
		  in=new Intent().setClass(this, about_page.class);
		  startActivity(in);
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  }
	  return super.onOptionsItemSelected(item);
  }
  
  // load the url
  private void load_webview() {
//	  webView.loadUrl(mainapp.getWebUrl());
	  String url = currentUrl;
	  if(url == null)
		  url = mainapp.createUrl(prefs.getString("InitialWebPage", getApplicationContext().getResources().getString(R.string.prefInitialWebPageDefaultValue)));

	  if (url == null)					// if port is invalid 
		  url = noUrl;
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
 }

}
