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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class web_activity extends Activity {

  private threaded_application mainapp;  // hold pointer to mainapp
  private static SharedPreferences prefs;
  private Boolean clearHistory = false;

  
  class web_handler extends Handler {

	  public void handleMessage(Message msg) {
		  switch(msg.what) {
			  case message_type.RESPONSE: {  //handle messages from WiThrottle server
				  String response_str = msg.obj.toString();
				  char com1 = response_str.charAt(0);
				  char thrSel = response_str.charAt(1);
				  switch (com1) {
					  case 'P': //panel info
						  if (thrSel == 'W') {		// PW - web server port info
							  clearHistory = true;
							  load_webview();		// reload the page
						  }
						  break;
				  }
				  break;
			  }
			  case message_type.DISCONNECT:
				  disconnect();
				  break;
		  }
	  };
  }

  /** Called when the activity is first created. */
  @SuppressLint("SetJavaScriptEnabled") @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mainapp=(threaded_application)this.getApplication();
    prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

    setContentView(R.layout.web_activity);
  
	WebView webView = (WebView) findViewById(R.id.webview);

	webView.getSettings().setJavaScriptEnabled(true);
	webView.getSettings().setBuiltInZoomControls(true); //Enable Multitouch if supported

	webView.getSettings().setUseWideViewPort(true);		// Enable greater zoom-out
	webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);

	webView.getSettings().setLoadWithOverviewMode(true);	// zoomed-out when page is loaded
	
	// open all links inside the current view (don't start external web browser)
	WebViewClient EDWebClient = new WebViewClient()	{
		@Override
		public boolean shouldOverrideUrlLoading(WebView  view, String  url) {
			return false;
		}
		@Override
		public void onPageFinished(WebView view, String url)
		{
			if(clearHistory)
			{
				view.clearHistory();
				clearHistory = false;
			}
		}
	};
	webView.setWebViewClient(EDWebClient);

  };

  // load the url
  private void load_webview()
  {
	  WebView webView = (WebView) findViewById(R.id.webview);
	  if(!(mainapp.getWebUrl().equals(webView.getUrl())))		// suppress load if url hasn't changed
		  webView.loadUrl(mainapp.getWebUrl());
  }
  
  @Override
  public void onStart() {
    super.onStart();

    //put pointer to this activity's handler in main app's shared variable (If needed)
  	  mainapp.web_msg_handler=new web_handler();
  }

  @Override
  public void onResume() {
	  Log.d("Engine_Driver","web_activity.onResume() called");
 	  super.onResume();

 	  setActivityOrientation(this);  //set screen orientation based on prefs
 	  load_webview();								// load the url

  };

  @Override
  public void onPause() {
	  WebView webView = (WebView) findViewById(R.id.webview);
      threaded_application mainapp = (threaded_application) getApplication();
	  mainapp.setWebUrl(webView.getUrl());
	  super.onPause();
  }
  
  
  /** Called when the activity is finished. */
  @Override
  public void onDestroy() {
	  Log.d("Engine_Driver","web_activity.onDestroy() called");

	  //load a bogus url to prevent javascript from continuing to run
	  WebView webView = (WebView) findViewById(R.id.webview);
	  webView.loadUrl("file:///android_asset/blank_page.html");
  	  mainapp.power_control_msg_handler = null;

	  super.onDestroy();
  }

  
  //Handle pressing of the back button to end this activity
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
	  if(key==KeyEvent.KEYCODE_BACK)
	  {
			WebView webView = (WebView) findViewById(R.id.webview);
			if(webView.canGoBack()) {
				webView.goBack();
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
	  case R.id.about_menu:
		  in=new Intent().setClass(this, about_page.class);
		  startActivity(in);
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.routes_menu:
		  in=new Intent().setClass(this, routes.class);
		  startActivity(in);
		  this.finish();
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.power_control_menu:
		  in=new Intent().setClass(this, power_control.class);
		  startActivity(in);
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.throttle:
		  this.finish();
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.turnouts:
		  in = new Intent().setClass(this, turnouts.class);
		  startActivity(in);
		  this.finish();
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  }
	  return super.onOptionsItemSelected(item);
  }
//set throttle screen orientation based on prefs, check to avoid sending change when already there
 private static void setActivityOrientation(Activity activity) {

	  String to = prefs.getString("WebOrientation", 
			  activity.getApplicationContext().getResources().getString(R.string.prefWebOrientationDefaultValue));
	  int co = activity.getRequestedOrientation();
	  if      (to.equals("Landscape")   && (co != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE))  
		  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	  else if (to.equals("Auto-Rotate") && (co != ActivityInfo.SCREEN_ORIENTATION_SENSOR))  
		  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	  else if (to.equals("Portrait")    && (co != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
		  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
 }

 private void disconnect() {
	  this.finish();
	  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
 }

}


