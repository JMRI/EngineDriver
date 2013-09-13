/*Copyright (C) 2013 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.lang.reflect.Method;

import jmri.enginedriver.Consist.ConLoco;
import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;
import android.view.MotionEvent;
import android.os.Message;
import android.widget.TextView;
import android.gesture.GestureOverlayView;
import android.graphics.Typeface;

public class throttle extends Activity implements android.gesture.GestureOverlayView.OnGestureListener {

	private threaded_application mainapp;  // hold pointer to mainapp
	private SharedPreferences prefs;

	private static final int GONE = 8;
	private static final int VISIBLE = 0;
	private static final int throttleMargin = 8;	// margin between the throttles in dp
	private static final int titleBar = 45;			// estimate of lost screen height in dp
	private static final int MAX_SPEED_DISPLAY = 99;	// value to display at maximum speed setting 
	private static int MAX_SPEED_VAL_T = 126;			// maximum DCC speed setting in current mode
	private static int MAX_SPEED_VAL_S = 126;			// maximum DCC speed setting in current mode
	private static int MAX_SPEED_VAL_G = 126;			// maximum DCC speed setting in current mode
	// speed scale factors
	private static double SPEED_TO_DISPLAY_T = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL_T);
	private static double SPEED_TO_DISPLAY_S = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL_S);
	private static double SPEED_TO_DISPLAY_G = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL_G);

	//	private static final long speedUpdateDelay = 500;	// idle time in milliseconds after speed change before requesting speed update 

	private char whichVolume = 'T';

	private boolean slider_moved_by_server = false;			// true if the slider was moved due to a processed response

	int max_throttle_change = 99;  //maximum allowable change of the sliders, set in preferences

	//screen coordinates for throttle sliders, so we can ignore swipe on them
	private int T_top;  
	private int T_bottom;  
	private int S_top;  
	private int S_bottom;  
	private int G_top;  
	private int G_bottom;  

	//these are used for gesture tracking
	private float gestureStartX = 0;
	private float gestureStartY = 0;
	private boolean gestureFailed = false;			// gesture didn't meet velocity or distance requirement
	private boolean gestureInProgress = false;		// gesture is in progress
	private long gestureLastCheckTime;				// time in milliseconds that velocity was last checked
	private static final long gestureCheckRate = 200;	// rate in milliseconds to check velocity
	private VelocityTracker mVelocityTracker;

	//function number-to-button maps
	private LinkedHashMap<Integer, Button> functionMapT;
	private LinkedHashMap<Integer, Button> functionMapS;
	private LinkedHashMap<Integer, Button> functionMapG;

	//current direction
	private int dirT = 1;									//request direction for each throttle (single or multiple engines)
	private int dirS = 1;
	private int dirG = 1;

	private static final String noUrl = "file:///android_asset/blank_page.html";
	private static final float initialScale = 1.5f;
	private WebView webView = null;
	private String webViewLocation;
	private static float scale = initialScale;			// used to restore throttle web zoom level (after rotation)
	private static boolean clearHistory = false;		// flags webViewClient to clear history when page load finishes
	private static String currentUrl = null;
	private boolean currentUrlUpdate;
	private boolean orientationChange = false;
	private Menu TMenu;
	int lastSpeed;
	int lastSpeedT;
	int lastSpeedS;
	int lastSpeedG;
	static int REP_DELAY = 25;

	private Handler repeatUpdateHandler = new Handler();
	private boolean mAutoIncrement = false;
	private boolean mAutoDecrement = false;

	//For speed slider speed buttons.
	class RptUpdater implements Runnable {
		char whichThrottle;

		public RptUpdater(char WhichThrottle)
		{
			whichThrottle = WhichThrottle;

			try
			{
				REP_DELAY = Integer.parseInt(prefs.getString("speed_arrows_throttle_change", "25"));
			}
			catch(Exception ex)
			{
				REP_DELAY = 100;
			}
		}
		public void run() {
			if( mAutoIncrement ){
				increment(whichThrottle);
				repeatUpdateHandler.postDelayed( new RptUpdater(whichThrottle), REP_DELAY );
			} else if( mAutoDecrement ){
				decrement(whichThrottle);
				repeatUpdateHandler.postDelayed( new RptUpdater(whichThrottle), REP_DELAY );
			}
		}
	}
	//Handle messages from the communication thread TO this thread (responses from withrottle)
	@SuppressLint("HandlerLeak")
	class throttle_handler extends Handler {

		public void handleMessage(Message msg) {
			switch(msg.what) {
			case message_type.RESPONSE: {  //handle messages from WiThrottle server
				String response_str = msg.obj.toString();
				char com1 = response_str.charAt(0);
				char whichThrottle = response_str.charAt(1);

				if (response_str.charAt(2) == 'A') {  //change power state icon
					mainapp.setPowerStateButton(TMenu);
				}

				switch (com1) {
				//various MultiThrottle messages
				case 'M':
					char com2 = response_str.charAt(2);
					String[] ls = threaded_application.splitByString(response_str,"<;>");
					String addr;
					try {
						addr = ls[0].substring(3);
					}
					catch(Exception e) {
						addr = "";
					}
					if(whichThrottle == 'T' || whichThrottle == 'S' || whichThrottle == 'G') {
						if (com2 == '+' || com2 == 'L') {  //if loco added or function labels updated
							if(com2 == ('+')) {
								//							  set_default_function_labels();
								enable_disable_buttons(whichThrottle);  //direction and slider: pass whichthrottle
							}
							// loop through all function buttons and
							//   set label and dcc functions (based on settings) or hide if no label
							ViewGroup tv;
							if (whichThrottle == 'T') {
								tv = (ViewGroup) findViewById(R.id.function_buttons_table_T);
							} 
							else if(whichThrottle == 'S')
							{
								tv = (ViewGroup) findViewById(R.id.function_buttons_table_S);
							}
							else {
								tv = (ViewGroup) findViewById(R.id.function_buttons_table_G);
							}
							set_function_labels_and_listeners_for_view(whichThrottle);
							enable_disable_buttons_for_view(tv, true);
							set_labels();
						} 
						else if (com2 == '-') {  		//if loco removed
							if (whichThrottle == 'T') {
								removeLoco('T');
							}
							else if(whichThrottle == 'S')
							{
								removeLoco('S');
							}
							else {
								removeLoco('G');
							}
						} 
						else if (com2 == 'A') {	  		//e.g. MTAL2608<;>R1
							char com3 = ls[1].charAt(0);
							if (com3 == 'R') {
								int dir;
								try {
									dir = Integer.valueOf(ls[1].substring(1,2));
								}
								catch(Exception e) {
									dir = 1;
								}

								Consist con;
								int curDir;
								if(whichThrottle == 'T') {
									con = mainapp.consistT;
									curDir = dirT;
								}
								else if(whichThrottle == 'S'){
									con = mainapp.consistS;
									curDir = dirS;
								}
								else
								{
									con = mainapp.consistG;
									curDir = dirG;
								}

								if(addr.equals(con.getLeadAddr())) {
									if(dir != curDir) {									//lead/consist direction changed from outside of ED
										showDirectionRequest(whichThrottle, dir);			//update requested direction indication
										setEngineDirection(whichThrottle, dir, true);		//update rest of consist to match new direction
									}
								}
								else {
									int locoDir = curDir;								//calc correct direction for this (non-lead) loco
									if(con != null)
									{
										if(con.isReverseOfLead(addr))
											locoDir ^= 1;
										if(locoDir != dir) {								//if loco has wrong direction then correct it
											mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DIRECTION, addr, (int) whichThrottle, locoDir);
										}
									}
								}
							}
							else if (com3 == 'V') {
								try {
									int speed = Integer.parseInt(ls[1].substring(1));
									slider_moved_by_server = true;
									set_speed_slider(whichThrottle, speed);			//update speed slider and indicator
									slider_moved_by_server = false;
								}
								catch(Exception e) {
								}
							}
							else if (com3 == 'F') {
								try {
									int function = Integer.valueOf(ls[1].substring(2));
									set_function_state(whichThrottle, function);
								}
								catch(Exception e) {
								}
							} else {
								//	    		        	set_labels();
							}
						}
					}
					break;

				case 'T':
				case 'S':
				case 'G':
					enable_disable_buttons(com1);  //pass whichthrottle

					set_labels();
					break;

				case 'R': 
					if(whichThrottle == 'F' || whichThrottle == 'S' || whichThrottle == 'G') { //roster function labels - legacy
						ViewGroup tv;
						if (whichThrottle == 'F') {			// used to use 'F' instead of 'T'
							tv = (ViewGroup) findViewById(R.id.function_buttons_table_T);
							whichThrottle = 'T';
						} 
						else if(whichThrottle == 'S')
						{
							tv = (ViewGroup) findViewById(R.id.function_buttons_table_S);
						}
						else {
							tv = (ViewGroup) findViewById(R.id.function_buttons_table_G);
						}
						set_function_labels_and_listeners_for_view(whichThrottle);
						enable_disable_buttons_for_view(tv, true);
						set_labels();

					} else {
						try {
							String scom2 = response_str.substring(1,6);
							if(scom2.equals("PF}|{"))
							{
								whichThrottle = response_str.charAt(6);
								set_all_function_states(whichThrottle);
							}
						}
						catch(IndexOutOfBoundsException e){
						}
					}
					break;
				case 'P': //panel info
					if (whichThrottle == 'W')		// PW - web server port info
						reloadWeb();
					break;
				}  //end of switch
			}
			break;
			case message_type.WIT_CON_RETRY:
				removeLoco('T');
				removeLoco('S');
				removeLoco('G');
				reloadWeb();
				break;
			case message_type.DISCONNECT:
			case message_type.SHUTDOWN:
				disconnect();
				break;
			case message_type.WEBVIEW_LOC:				// webview location changed
				if("none".equals(webViewLocation)) {		// if not currently displayed 
					currentUrl = null;					// reload init url
				}
				// set new location
				webViewLocation = prefs.getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));    
				load_webview();
				break;
			case message_type.INITIAL_WEBPAGE:
				reloadWeb();
				break;
			}
		};
	}

	private void reloadWeb() {
		// try web-dependent items again
		webView.stopLoading();
		clearHistory = true;
		currentUrl = null;
		load_webview();				//reload
	}

	private void removeLoco(char whichThrottle) {
		ViewGroup tv;
		if (whichThrottle == 'T') {
			tv = (ViewGroup) findViewById(R.id.function_buttons_table_T);
		} 
		else if(whichThrottle == 'S')
		{
			tv = (ViewGroup) findViewById(R.id.function_buttons_table_S);
		}
		else {
			tv = (ViewGroup) findViewById(R.id.function_buttons_table_G);
		}
		enable_disable_buttons(whichThrottle);  //direction and slider
		set_function_labels_and_listeners_for_view(whichThrottle);
		enable_disable_buttons_for_view(tv, false);
		set_labels();
	}
	void updateMaxSpeed(char whichThrottle, int maxSpeed) {

		if(whichThrottle == 'T') {
			if(maxSpeed != MAX_SPEED_VAL_T) {
				double rescale = ((double)maxSpeed) / MAX_SPEED_VAL_T; 
				SPEED_TO_DISPLAY_T = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL_T);
				MAX_SPEED_VAL_T = maxSpeed;
				SeekBar throttleSlider=(SeekBar)findViewById(R.id.speed_T);
				int newSetting = (int)(throttleSlider.getProgress() * rescale);
				throttleSlider.setMax(maxSpeed);
				throttleSlider.setProgress(newSetting);
				setDisplayedSpeed('T', newSetting);
			}
		}
		else if(whichThrottle == 'S')
		{
			if(maxSpeed != MAX_SPEED_VAL_S) {
				double rescale = ((double)maxSpeed) / MAX_SPEED_VAL_S; 
				SPEED_TO_DISPLAY_S = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL_S);
				MAX_SPEED_VAL_S = maxSpeed;
				SeekBar throttleSlider=(SeekBar)findViewById(R.id.speed_S);
				int newSetting = (int)(throttleSlider.getProgress() * rescale);
				throttleSlider.setMax(maxSpeed);
				throttleSlider.setProgress(newSetting);
				setDisplayedSpeed('S', newSetting);
			}
		}
		else {
			if(maxSpeed != MAX_SPEED_VAL_G) {
				double rescale = ((double)maxSpeed) / MAX_SPEED_VAL_G; 
				SPEED_TO_DISPLAY_G = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL_G);
				MAX_SPEED_VAL_G = maxSpeed;
				SeekBar throttleSlider=(SeekBar)findViewById(R.id.speed_G);
				int newSetting = (int)(throttleSlider.getProgress() * rescale);
				throttleSlider.setMax(maxSpeed);
				throttleSlider.setProgress(newSetting);
				setDisplayedSpeed('G', newSetting);
			}
		}
	}

	void set_speed_slider(char whichThrottle, int speed) {
		SeekBar throttle_slider;

		if (whichThrottle == 'T') {
			throttle_slider=(SeekBar)findViewById(R.id.speed_T);
			lastSpeedT = speed;
			prefs.edit().putInt("T", lastSpeedT).commit();
		} 
		else if(whichThrottle == 'S')
		{
			throttle_slider=(SeekBar)findViewById(R.id.speed_S);
			lastSpeedS = speed;
		}
		else {
			throttle_slider=(SeekBar)findViewById(R.id.speed_G);
			lastSpeedG = speed;
		}
		if(speed < 0)
		{
			//Check to confirm.
			if(whichThrottle == 'T')
			{
				lastSpeedT = 0;
			}
			else if(whichThrottle == 'S')
			{
				lastSpeedS = 0;
			}
			else
			{
				lastSpeedG = 0;
			}

			speed = 0;
		}
		throttle_slider.setProgress(speed);
	}

	private void setDisplayedSpeed(char whichThrottle, int speed) {
		TextView speed_label;
		Consist con;
		double speedScale;
		if (whichThrottle == 'T') {
			speedScale = SPEED_TO_DISPLAY_T;
			speed_label=(TextView)findViewById(R.id.speed_value_label_T);
			con = mainapp.consistT;
		} 

		else if(whichThrottle == 'S')
		{
			speedScale = SPEED_TO_DISPLAY_S;
			speed_label=(TextView)findViewById(R.id.speed_value_label_S);
			con = mainapp.consistS;
		}
		else {
			speedScale = SPEED_TO_DISPLAY_G;
			speed_label=(TextView)findViewById(R.id.speed_value_label_G);
			con = mainapp.consistS;
		}
		if(speed < 0) {
			Toast.makeText(getApplicationContext(), "Alert: Engine " + con.toString() + " is set to ESTOP", Toast.LENGTH_LONG).show();
			speed = 0;
		}
		int displayedSpeed = (int)Math.round(speed * speedScale);
		speed_label.setText(Integer.toString(displayedSpeed));
	}

	// set the direction for all engines on the throttle
	// if skipLead is true, the direction is not set for the lead engine
	void setEngineDirection(char whichThrottle, int direction, boolean skipLead) {
		Consist con;
		if(whichThrottle == 'T')
			con = mainapp.consistT;
		else if(whichThrottle == 'S')
			con = mainapp.consistS;
		else
			con = mainapp.consistG;
		String leadAddr = con.getLeadAddr();
		for(String addr : con.getList()) {			// loop through each engine in consist
			if(!skipLead || !addr.equals(leadAddr)) {
				int locoDir = direction;
				if(con.isReverseOfLead(addr))			//if engine faces opposite of lead loco
					locoDir ^= 1;						//then reverse the commanded direction
				mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DIRECTION, addr, (int) whichThrottle, locoDir);
			}
		}
	}

	// indicate direction using the button pressed state
	void showDirectionIndication(char whichThrottle, int direction) {
		Button bFwd;
		Button bRev;
		if (whichThrottle == 'T') {
			bFwd = (Button)findViewById(R.id.button_fwd_T);
			bRev = (Button)findViewById(R.id.button_rev_T);
		}
		else if(whichThrottle == 'S')
		{
			bFwd = (Button)findViewById(R.id.button_fwd_S);
			bRev = (Button)findViewById(R.id.button_rev_S);
		}
		else {
			bFwd = (Button)findViewById(R.id.button_fwd_G);
			bRev = (Button)findViewById(R.id.button_rev_G);
		}

		if (direction == 0) {
			bFwd.setPressed(false);
			bRev.setPressed(true);
		} 
		else {
			bFwd.setPressed(true);
			bRev.setPressed(false);
		}
	}

	// indicate requested direction using the button typeface
	void showDirectionRequest(char whichThrottle, int direction) {
		Button bFwd;
		Button bRev;
		if (whichThrottle == 'T') {
			bFwd = (Button)findViewById(R.id.button_fwd_T);
			bRev = (Button)findViewById(R.id.button_rev_T);
			dirT = direction;
		} 
		else if(whichThrottle == 'S')
		{
			bFwd = (Button)findViewById(R.id.button_fwd_S);
			bRev = (Button)findViewById(R.id.button_rev_S);
			dirS = direction;
		}
		else {
			bFwd = (Button)findViewById(R.id.button_fwd_G);
			bRev = (Button)findViewById(R.id.button_rev_G);
			dirG = direction;
		}

		if (direction == 0) {
			bFwd.setTypeface(null, Typeface.NORMAL);
			bRev.setTypeface(null, Typeface.ITALIC);
		} 
		else {
			bFwd.setTypeface(null, Typeface.ITALIC);
			bRev.setTypeface(null, Typeface.NORMAL);
		}

		/*
		 * this code gathers direction feedback for the direction indication
		 *
		 *  
	  if (mainapp.withrottle_version < 2.0) {
		  // no feedback avail so just let indication follow request
		  showDirectionIndication(whichThrottle, direction);
	  }
      else {				//get confirmation of direction changes
          mainapp.sendMsgDelay(mainapp.comm_msg_handler, 100, message_type.REQ_DIRECTION, "", (int) whichThrottle, 0);
      }
		 *
		 *  due to response lags, for now just track the setting:	//******
		 */  	
		showDirectionIndication(whichThrottle, direction);
	}

	void set_stop_button(char whichThrottle, boolean pressed) {
		Button bStop;
		if (whichThrottle == 'T') {
			bStop = (Button)findViewById(R.id.button_stop_T);
		} 
		else if(whichThrottle == 'S')
		{
			bStop = (Button)findViewById(R.id.button_stop_S);
		}
		else {
			bStop = (Button)findViewById(R.id.button_stop_G);
		}
		if (pressed == true) {
			bStop.setPressed(true);
			bStop.setTypeface(null, Typeface.ITALIC);
		} else {
			bStop.setPressed(false);
			bStop.setTypeface(null, Typeface.NORMAL);
		}
	}

	void start_select_loco_activity(char whichThrottle)
	{
		Button bSel;
		if (whichThrottle == 'T') {
			bSel = (Button)findViewById(R.id.button_select_loco_T);
		} 
		else if(whichThrottle == 'S')
		{
			bSel = (Button)findViewById(R.id.button_select_loco_S);
		}
		else {
			bSel = (Button)findViewById(R.id.button_select_loco_G);
		}
		bSel.setPressed(true);	//give feedback that select button was pressed
		try
		{
			Intent select_loco = new Intent().setClass(this, select_loco.class);
			select_loco.putExtra("sWhichThrottle", Character.toString(whichThrottle));  //pass whichThrottle as an extra to activity
			startActivityForResult(select_loco, 1);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		}
		catch(Exception ex)
		{
			Log.d("debug", ex.getMessage());
		}
	};

	void enable_disable_buttons(char whichThrottle)  {
		boolean newEnabledState;
		if (whichThrottle == 'T') {
			newEnabledState = !mainapp.consistT.isEmpty();  				//set false if no locos assigned
			findViewById(R.id.button_fwd_T).setEnabled(newEnabledState);
			findViewById(R.id.button_stop_T).setEnabled(newEnabledState);
			findViewById(R.id.button_rev_T).setEnabled(newEnabledState);
			findViewById(R.id.speed_label_T).setEnabled(newEnabledState);
			findViewById(R.id.speed_value_label_T).setEnabled(newEnabledState);
			findViewById(R.id.Left_speed_button_T).setEnabled(newEnabledState);
			findViewById(R.id.Right_speed_button_T).setEnabled(newEnabledState);
			enable_disable_buttons_for_view((ViewGroup)findViewById(R.id.function_buttons_table_T),newEnabledState);
			SeekBar sb = (SeekBar)findViewById(R.id.speed_T);
			if (!newEnabledState) {
				sb.setProgress(0);  //set slider to 0 if disabled
			}
			sb.setEnabled(newEnabledState);
		} 
		else if(whichThrottle == 'S')
		{
			newEnabledState = !(mainapp.consistS.isEmpty());  			//set false if no locos assigned
			findViewById(R.id.button_fwd_S).setEnabled(newEnabledState);
			findViewById(R.id.button_stop_S).setEnabled(newEnabledState);
			findViewById(R.id.button_rev_S).setEnabled(newEnabledState);
			findViewById(R.id.speed_label_S).setEnabled(newEnabledState);
			findViewById(R.id.speed_value_label_S).setEnabled(newEnabledState);
			findViewById(R.id.Left_speed_button_S).setEnabled(newEnabledState);
			findViewById(R.id.Right_speed_button_S).setEnabled(newEnabledState);
			enable_disable_buttons_for_view((ViewGroup)findViewById(R.id.function_buttons_table_S),newEnabledState);
			SeekBar sb = (SeekBar)findViewById(R.id.speed_S);
			if (!newEnabledState) {
				sb.setProgress(0);  //set slider to 0 if disabled
			}
			sb.setEnabled(newEnabledState);
		}
		else {

			newEnabledState = !(mainapp.consistG.isEmpty());  			//set false if no locos assigned
			findViewById(R.id.button_fwd_G).setEnabled(newEnabledState);
			findViewById(R.id.button_stop_G).setEnabled(newEnabledState);
			findViewById(R.id.button_rev_G).setEnabled(newEnabledState);
			findViewById(R.id.speed_label_G).setEnabled(newEnabledState);
			findViewById(R.id.speed_value_label_G).setEnabled(newEnabledState);
			findViewById(R.id.Left_speed_button_G).setEnabled(newEnabledState);
			findViewById(R.id.Right_speed_button_G).setEnabled(newEnabledState);
			enable_disable_buttons_for_view((ViewGroup)findViewById(R.id.function_buttons_table_G),newEnabledState);
			SeekBar sb = (SeekBar)findViewById(R.id.speed_G);
			if (!newEnabledState) {
				sb.setProgress(0);  //set slider to 0 if disabled
			}
			sb.setEnabled(newEnabledState);
		}
	};  //end of enable_disable_buttons

	//helper function to enable/disable all children for a group
	void enable_disable_buttons_for_view(ViewGroup vg, boolean newEnabledState)  {
		// Log.d("Engine_Driver","starting enable_disable_buttons_for_view " + newEnabledState);

		ViewGroup r;  //row
		Button b; //button
		for(int i = 0; i < vg.getChildCount(); i++) {
			r = (ViewGroup)vg.getChildAt(i);
			for(int j = 0; j < r.getChildCount(); j++) {
				b = (Button)r.getChildAt(j);
				b.setEnabled(newEnabledState);
			}
		}
	} //enable_disable_buttons_for_view

	//update the appearance of all function buttons 
	void set_all_function_states(char whichThrottle)  {
		//	  Log.d("Engine_Driver","set_function_states");

		LinkedHashMap<Integer, Button> fMap;
		if(whichThrottle == 'T')
			fMap = functionMapT;
		else if(whichThrottle == 'S')
		{
			fMap = functionMapS;
		}
		else
			fMap = functionMapG;

		for(Integer f : fMap.keySet()) {
			set_function_state(whichThrottle, f);
		}
	}

	//update a function button appearance based on its state 
	void set_function_state(char whichThrottle, int function)  {
		//  	  Log.d("Engine_Driver","starting set_function_request");
		Button b;
		boolean[] fs;  //copy of this throttle's function state array
		if(whichThrottle == 'T') {
			b = functionMapT.get(function);
			fs = mainapp.function_states_T;
		}
		else if(whichThrottle == 'S')
		{
			b = functionMapS.get(function);
			fs = mainapp.function_states_S;
		}
		else {
			b = functionMapG.get(function);
			fs = mainapp.function_states_G;
		}
		if(b != null && fs != null) {
			if(fs[function] == true) {
				b.setTypeface(null, Typeface.ITALIC);
				b.setPressed(true);
			} 
			else {
				b.setTypeface(null, Typeface.NORMAL);
				b.setPressed(false);
			}
		}
	}

	/*
	 * future use: displays the requested function state independent of (in addition to) feedback state
	 * todo: need to handle momentary buttons somehow
	 */
	void set_function_request(char whichThrottle, int function, int reqState)  {
		//	  Log.d("Engine_Driver","starting set_function_request");
		Button b;
		if(whichThrottle == 'T') {
			b = functionMapT.get(function);
		}
		else if(whichThrottle == 'S')
		{
			b = functionMapS.get(function);
		}
		else {
			b = functionMapG.get(function);
		}
		if(b != null) {
			if(reqState != 0) {
				b.setTypeface(null, Typeface.ITALIC);
			} 
			else {
				b.setTypeface(null, Typeface.NORMAL);
			}
		}
	} 

	public class select_function_button_touch_listener implements View.OnClickListener
	{
		char whichThrottle;  //T for first throttle, S for second, G for third

		public select_function_button_touch_listener(char new_whichThrottle)
		{
			whichThrottle = new_whichThrottle;
		}

		@Override
		public void onClick(View v) {
			start_select_loco_activity(whichThrottle);  //pass throttle #	
		}
	}

	public class arrow_speed_button_touch_listener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener
	{
		char whichThrottle;  //T for first throttle, S for second, G for third
		String arrowDirection;

		public arrow_speed_button_touch_listener(char new_whichThrottle, String new_arrowDirection)
		{
			whichThrottle = new_whichThrottle;
			arrowDirection = new_arrowDirection;
		}

		@Override
		public boolean onLongClick(View v) {
			if(arrowDirection.equals("right"))
			{
				mAutoIncrement = true;
			}
			else
			{
				mAutoDecrement = true;
			}
			repeatUpdateHandler.post( new RptUpdater(whichThrottle));
			return false;
		}

		@Override
		public void onClick(View v) {
			if(arrowDirection.equals("right"))
			{
				mAutoIncrement = false;
				increment(whichThrottle);
			}
			else
			{
				mAutoDecrement = false;
				decrement(whichThrottle);
			}
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if( (event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL) 
					&& mAutoIncrement ){
				mAutoIncrement = false;
			}

			if( (event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL) 
					&& mAutoDecrement ){
				mAutoDecrement = false;
			}
			return false;
		}
	}

	public class function_button_touch_listener implements View.OnTouchListener
	{
		int function;
		char whichThrottle;  //T for first throttle, S for second, G for third

		//    public function_button_touch_listener(int new_function, boolean new_toggle_type, String new_whichThrottle)
		public function_button_touch_listener(int new_function, char new_whichThrottle)
		{
			function=new_function;  //store these values for this button
			whichThrottle = new_whichThrottle;

		}

		public boolean onTouch(View v, MotionEvent event)
		{
			//      Log.d("Engine_Driver", "onTouch func" + function + " action " + event.getAction());
			//if gesture in progress, skip button processing
			if(gestureInProgress == true)
			{
				return(true);
			}

			// if gesture just failed, insert one DOWN event on this control
			if(gestureFailed == true)
			{
				handleAction(MotionEvent.ACTION_DOWN);
				gestureFailed = false;	// just do this once
			}
			handleAction(event.getAction());
			return(true);
		}

		private void handleAction(int action) 
		{
			//      Log.d("Engine_Driver", "handleAction func" + function + " action " + action);
			String throt = Character.toString(whichThrottle);

			switch(action)
			{
			case MotionEvent.ACTION_DOWN:
			{
				switch (this.function) {
				case function_button.FORWARD :
				case function_button.REVERSE : {
					int dir = (function == function_button.FORWARD ? 1 : 0);
					showDirectionRequest(whichThrottle, dir);		//update requested direction indication
					setEngineDirection(whichThrottle, dir, false);	//update direction for each engine on this throttle
					break;
				}	
				case function_button.STOP : { 
					set_stop_button(whichThrottle, true);
					SeekBar sb;
					if (whichThrottle == 'T') {
						sb=(SeekBar)findViewById(R.id.speed_T);
					} 
					else if(whichThrottle == 'S')
					{
						sb=(SeekBar)findViewById(R.id.speed_S);
					}
					else {
						sb=(SeekBar)findViewById(R.id.speed_G);
					}
					// changing the throttle slider to 0 sends a velocity change to the withrottle
					// otherwise explicitly send 0 speed message
					if(sb.getProgress() != 0)
						sb.setProgress(0);
					else
						sendSpeedMsg(whichThrottle, 0);
					break;
				}
				case function_button.SPEED_LABEL: {			            // specify which throttle the volume button controls          	  
					whichVolume = whichThrottle;  				//use whichever was clicked
					set_labels();
					break;
				}
				//Commented because this section kept starting select loco activity twice.
				//				case function_button.SELECT_LOCO : {
				//					start_select_loco_activity(whichThrottle);  //pass throttle #
				//					Log.d("debug", "Select Loco");
				//
				//					break;
				//				}

				default : {  //handle the function buttons
					//Consist con = (whichThrottle == 'T') ? mainapp.consistT : mainapp.consistS;
					Consist con;
					if(whichThrottle == 'T')
					{
						con = mainapp.consistT;
					}
					else if(whichThrottle == 'S')
					{
						con = mainapp.consistS;
					}
					else
					{
						con = mainapp.consistG;
					}

					String addr = con.getLeadAddr();
					mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, whichThrottle+addr, this.function, 1);
					//              set_function_request(whichThrottle, function, 1);
				}
				}

			}
			break;
			//handle stopping of function on key-up 
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:

				if(function == function_button.STOP) { 
					set_stop_button(whichThrottle, false);
				}
				// only process UP for function buttons
				else if(function < function_button.FORWARD)   {
					//Consist con = (whichThrottle == 'T') ? mainapp.consistT : mainapp.consistS;
					Consist con;
					if(whichThrottle == 'T')
					{
						con = mainapp.consistT;
					}
					else if(whichThrottle == 'S')
					{
						con = mainapp.consistS;
					}
					else
					{
						con = mainapp.consistG;
					}
					String addr = con.getLeadAddr();
					mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, throt+addr, function, 0);
					//                set_function_request(whichThrottle, function, 0);
				}
				break;
			}
		}
	}

	public class throttle_listener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener
	{
		char whichThrottle;
		SeekBar speed_slider;
		//int lastSpeed;

		public throttle_listener(char new_whichThrottle)    {
			whichThrottle = new_whichThrottle;   	//store values for this listener
			if (whichThrottle == 'T') {
				speed_slider=(SeekBar)findViewById(R.id.speed_T);
			} 
			else if(whichThrottle == 'S')
			{
				speed_slider=(SeekBar)findViewById(R.id.speed_S);
			}
			else {
				speed_slider=(SeekBar)findViewById(R.id.speed_G);
			}
		}

		public boolean onTouch(View v, MotionEvent event) 
		{
			//        Log.d("Engine_Driver", "onTouchThrot action " + event.getAction());

			//consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
			return(gestureInProgress);
		}

		public void onProgressChanged(SeekBar throttle, int speed, boolean fromUser) {

			//Log.d("Engine_Driver", "onProgChanged " + speed + "-" + lastSpeed + "=" + (speed-lastSpeed));

			throttlechangespeed(throttle, speed, fromUser, whichThrottle);
			return;
		}

		@Override
		public void onStartTrackingTouch(SeekBar sb) {
			gestureInProgress = false;
			return;
		}

		@Override
		public void onStopTrackingTouch(SeekBar sb) {
		}
	}

	public void throttlechangespeed(SeekBar throttle, int speed, boolean fromUser, char whichThrottle)
	{
		setDisplayedSpeed(whichThrottle, speed);

		//only continue (limit, send to JMRI) if change was initiated by a user touch (prevents "bouncing")
		if (slider_moved_by_server) {
			lastSpeed = speed;
			slider_moved_by_server = false;  //reset before return
			return;
		}

		//limit the percent change requested (from prefs)
		double speedScale;
		if (whichThrottle == 'T') {
			speedScale = SPEED_TO_DISPLAY_T;
		} 
		else if(whichThrottle == 'S')
		{
			speedScale = SPEED_TO_DISPLAY_S;
		}
		else {
			speedScale = SPEED_TO_DISPLAY_G;
		}
		if ((speed - lastSpeed) > (max_throttle_change / speedScale)) { 
			//Log.d("Engine_Driver", "onProgressChanged -- throttling change");
			speed = lastSpeed + 1;  //advance, but slowly, so user knows something is happening
			throttle.setProgress(speed);
		}

		//send request for new speed to WiT server
		sendSpeedMsg(whichThrottle, speed);

		lastSpeed = speed;

		//Check to confirm.
		if(whichThrottle == 'T')
		{
			lastSpeedT = speed;
		}
		else if(whichThrottle == 'S')
		{
			lastSpeedS = speed;
		}
		else
		{
			lastSpeedG = speed;
		}
	}

	// send a throttle speed message to WiT
	public void sendSpeedMsg(char whichThrottle, int speed) {
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.VELOCITY, "",(int) whichThrottle, speed);
	}

	/*  private void requestSpeedMsg(char whichThrottle) {
		// always load whichThrottle into message
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQ_VELOCITY, "", (int) whichThrottle);
}
	 */

//	public String setTitleToIncludeThrotName()
//	{
//		//sets the tile to include throttle name.
//		String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
//		//setTitle(getApplicationContext().getResources().getString(R.string.app_name_throttle) + "    |    Throttle Name: " + prefs.getString("throttle_name_preference", defaultName));
//		return  prefs.getString("throttle_name_preference", defaultName);
//	}

	public void decrement(char whichThrottle){

		SeekBar speed_slider;

		if(whichThrottle == 'T')
		{
			speed_slider=(SeekBar)findViewById(R.id.speed_T);
			lastSpeedT--;
			speed_slider.setProgress(lastSpeedT);

		}
		else if(whichThrottle == 'S')
		{
			speed_slider=(SeekBar)findViewById(R.id.speed_S);
			lastSpeedS--;
			speed_slider.setProgress(lastSpeedS);
		}
		else
		{
			speed_slider=(SeekBar)findViewById(R.id.speed_G);
			lastSpeedG--;
			speed_slider.setProgress(lastSpeedG);
		}
	}

	public void increment(char whichThrottle)
	{
		SeekBar speed_slider;

		if(whichThrottle == 'T')
		{
			speed_slider=(SeekBar)findViewById(R.id.speed_T);
			lastSpeedT++;
			speed_slider.setProgress(lastSpeedT);
		}
		else if(whichThrottle == 'S')
		{
			speed_slider=(SeekBar)findViewById(R.id.speed_S);
			lastSpeedS++;
			speed_slider.setProgress(lastSpeedS);
		}
		else
		{
			speed_slider=(SeekBar)findViewById(R.id.speed_G);
			lastSpeedG++;
			speed_slider.setProgress(lastSpeedG);
		}
	}

	@SuppressLint({ "Recycle", "SetJavaScriptEnabled" })
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mainapp=(threaded_application)this.getApplication();
		prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
		orientationChange = false;

		if(mainapp.isForcingFinish()) {		// expedite
			return;
		}
		setContentView(R.layout.throttle);

		//setTitleToIncludeThrotName();

		webViewLocation = prefs.getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));    
		//    myGesture = new GestureDetector(this);
		GestureOverlayView ov = (GestureOverlayView)findViewById(R.id.throttle_overlay);
		ov.addOnGestureListener(this);
		ov.setGestureVisible(false);

		Button b;
		function_button_touch_listener fbtl;
		select_function_button_touch_listener sfbt;
		arrow_speed_button_touch_listener asbl;

		//set listener for select loco buttons
		b = (Button)findViewById(R.id.button_select_loco_T);
		b.setClickable(true);
		sfbt = new select_function_button_touch_listener('T');
		b.setOnClickListener(sfbt);

		b = (Button)findViewById(R.id.button_select_loco_S);
		b.setClickable(true);
		sfbt = new select_function_button_touch_listener('S');
		b.setOnClickListener(sfbt);

		b = (Button)findViewById(R.id.button_select_loco_G);
		b.setClickable(true);
		sfbt = new select_function_button_touch_listener('G');
		b.setOnClickListener(sfbt);


		//Arrow Keys
		try
		{
			// Throttle T speed buttons.
			b = (Button) findViewById(R.id.Right_speed_button_T);
			b.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('T', "right");
			b.setOnLongClickListener(asbl);
			b.setOnTouchListener(asbl);
			b.setOnClickListener(asbl);

			b = (Button) findViewById(R.id.Left_speed_button_T);
			b.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('T', "left");
			b.setOnLongClickListener(asbl);
			b.setOnTouchListener(asbl);
			b.setOnClickListener(asbl);


			// Throttle S speed buttons
			b = (Button) findViewById(R.id.Right_speed_button_S);
			b.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('S', "right");
			b.setOnLongClickListener(asbl);
			b.setOnTouchListener(asbl);
			b.setOnClickListener(asbl);

			b = (Button) findViewById(R.id.Left_speed_button_S);
			b.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('S', "left");
			b.setOnLongClickListener(asbl);
			b.setOnTouchListener(asbl);
			b.setOnClickListener(asbl);


			
			//Throttle G speed buttons.
			b = (Button) findViewById(R.id.Right_speed_button_G);
			b.setClickable(true);asbl = new arrow_speed_button_touch_listener('G', "right");
			b.setOnLongClickListener(asbl);
			b.setOnTouchListener(asbl);
			b.setOnClickListener(asbl);

			b = (Button) findViewById(R.id.Left_speed_button_G);
			b.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('G', "left");
			b.setOnLongClickListener(asbl);
			b.setOnTouchListener(asbl);
			b.setOnClickListener(asbl);
		}
		catch(Exception ex)
		{
			Log.d("debug", "onCreate: " + ex.getMessage());
		}

		// set listeners for 3 direction buttons for each throttle
		b = (Button)findViewById(R.id.button_fwd_T);
		fbtl=new function_button_touch_listener(function_button.FORWARD, 'T');
		b.setOnTouchListener(fbtl);
		b = (Button)findViewById(R.id.button_stop_T);
		fbtl=new function_button_touch_listener(function_button.STOP, 'T');
		b.setOnTouchListener(fbtl);
		b = (Button)findViewById(R.id.button_rev_T);
		fbtl=new function_button_touch_listener(function_button.REVERSE, 'T');
		b.setOnTouchListener(fbtl);
		View v = findViewById(R.id.speed_cell_T);
		fbtl=new function_button_touch_listener(function_button.SPEED_LABEL, 'T');
		v.setOnTouchListener(fbtl);

		b = (Button)findViewById(R.id.button_fwd_S);
		fbtl=new function_button_touch_listener(function_button.FORWARD, 'S');
		b.setOnTouchListener(fbtl);
		b = (Button)findViewById(R.id.button_stop_S);
		fbtl=new function_button_touch_listener(function_button.STOP, 'S');
		b.setOnTouchListener(fbtl);
		b = (Button)findViewById(R.id.button_rev_S);
		fbtl=new function_button_touch_listener(function_button.REVERSE, 'S');
		b.setOnTouchListener(fbtl);
		v = findViewById(R.id.speed_cell_S);
		fbtl=new function_button_touch_listener(function_button.SPEED_LABEL, 'S');
		v.setOnTouchListener(fbtl);

		b = (Button)findViewById(R.id.button_fwd_G);
		fbtl=new function_button_touch_listener(function_button.FORWARD, 'G');
		b.setOnTouchListener(fbtl);
		b = (Button)findViewById(R.id.button_stop_G);
		fbtl=new function_button_touch_listener(function_button.STOP,  'G');
		b.setOnTouchListener(fbtl);
		b = (Button)findViewById(R.id.button_rev_G);
		fbtl=new function_button_touch_listener(function_button.REVERSE, 'G');
		b.setOnTouchListener(fbtl);
		v = findViewById(R.id.speed_cell_G);
		fbtl=new function_button_touch_listener(function_button.SPEED_LABEL, 'G');
		v.setOnTouchListener(fbtl);


		// set up listeners for all throttles
		throttle_listener th1;
		SeekBar sb=(SeekBar)findViewById(R.id.speed_T);
		th1 = new throttle_listener('T');
		sb.setOnSeekBarChangeListener(th1);
		sb.setOnTouchListener(th1);

		sb=(SeekBar)findViewById(R.id.speed_S);
		th1 = new throttle_listener('S');
		sb.setOnSeekBarChangeListener(th1);
		sb.setOnTouchListener(th1);

		sb=(SeekBar)findViewById(R.id.speed_G);
		th1 = new throttle_listener('G');
		sb.setOnSeekBarChangeListener(th1);
		sb.setOnTouchListener(th1);

		//    set_default_function_labels();
		// loop through all function buttons and
		//   set label and dcc functions (based on settings) or hide if no label
		set_function_labels_and_listeners_for_view('T');
		set_function_labels_and_listeners_for_view('S');
		set_function_labels_and_listeners_for_view('G');

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}

		webView = (WebView) findViewById(R.id.throttle_webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setBuiltInZoomControls(true); //Enable Multitouch if supported
		webView.getSettings().setUseWideViewPort(true);		// Enable greater zoom-out
		webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
		webView.setInitialScale((int)(100 * scale));
		//	webView.getSettings().setLoadWithOverviewMode(true);	// size image to fill width

		// open all links inside the current view (don't start external web browser)
		WebViewClient EDWebClient = new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView  view, String  url) {
				return false;
			}
			@Override
			public void onPageFinished(WebView view, String url) {
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
		mainapp.throttle_msg_handler=new throttle_handler();
	} //end of onCreate()

	@Override
	public void onResume() {
		super.onResume();
		if(mainapp.isForcingFinish()) {		//expedite
			this.finish();
			return;
		}
		mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
		// set max allowed change for throttles from prefs
		String s = prefs.getString("maximum_throttle_change_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleChangeDefaultValue));
		try {
			max_throttle_change = Integer.parseInt(s);
		} 
		catch (Exception e) {
			max_throttle_change = 25;
		}

		//format the screen area
		enable_disable_buttons('T'); 
		enable_disable_buttons('S');
		enable_disable_buttons('G');
		gestureFailed = false;
		gestureInProgress = false;

		set_labels();			// handle labels and update view

		if(webView != null) {
			if(!callHiddenWebViewOnResume())
				webView.resumeTimers();
		}


		if(mainapp.EStopActivated)
		{
			SeekBar sbT=(SeekBar)findViewById(R.id.speed_T);
			SeekBar sbS=(SeekBar)findViewById(R.id.speed_S);
			SeekBar sbG=(SeekBar)findViewById(R.id.speed_G);

			sbT.setProgress(0);

			sbS.setProgress(0);

			sbG.setProgress(0);

			mainapp.EStopActivated = false;
		}

		if(TMenu != null)
		{
			if(mainapp.consistT.size() >= 2)
			{
				TMenu.findItem(R.id.EditConsistT_menu).setVisible(true);
			}
			else
			{
				TMenu.findItem(R.id.EditConsistT_menu).setVisible(false);
			}


			if(mainapp.consistS.size() >= 2)
			{
				TMenu.findItem(R.id.EditConsistS_menu).setVisible(true);
			}
			else
			{
				TMenu.findItem(R.id.EditConsistS_menu).setVisible(false);
			}

			if(mainapp.consistG.size() >= 2)
			{
				TMenu.findItem(R.id.EditConsistG_menu).setVisible(true);
			}
			else
			{
				TMenu.findItem(R.id.EditConsistG_menu).setVisible(false);
			}
		}

		CookieSyncManager.getInstance().startSync();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		webView.saveState(outState);		// save history (on rotation) if at least one page has loaded
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

		if(this.isFinishing()) {		//if finishing, expedite it and don't invoke setContentIntentNotification
			return;
		}

		mainapp.setContentIntentNotification(this.getIntent());
	}

	/** Called when the activity is finished. */
	@SuppressWarnings("deprecation")
	@Override
	public void onDestroy() {
		Log.d("Engine_Driver","throttle.onDestroy()");
		prefs.edit().putInt("T", 0).commit();
		prefs.edit().putInt("S", 0).commit();
		prefs.edit().putInt("G", 0).commit();
		prefs.edit().putBoolean("r", false).commit();
		if(webView != null) {
			scale = webView.getScale();		// save current scale for next onCreate
			if(!orientationChange) {			// if screen is exiting
				webView.loadUrl(noUrl);		//load a static url else any javascript on current page would keep running
			}
		}
		mainapp.throttle_msg_handler = null;

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

	// load the url
	private void load_webview()
	{
		String url = currentUrl;
		if(url == null)
			url = mainapp.createUrl(prefs.getString("InitialThrotWebPage", getApplicationContext().getResources().getString(R.string.prefInitialThrotWebPageDefaultValue)));

		if (url == null || webViewLocation.equals("none"))		// if port is invalid or not displaying webview
			url = noUrl;
		//	  webView.clearCache(true);
		webView.loadUrl(url);
	}

	//helper function to set up function buttons for each throttle
	void set_function_labels_and_listeners_for_view(char whichThrottle)  {
		//	  Log.d("Engine_Driver","starting set_function_labels_and_listeners_for_view");

		ViewGroup tv; //group
		ViewGroup r;  //row
		function_button_touch_listener fbtl;
		Button b; //button
		int k = 0; //button count
		LinkedHashMap<Integer, String> function_labels_temp = new  LinkedHashMap<Integer, String>();
		LinkedHashMap<Integer, Button> functionButtonMap = new  LinkedHashMap<Integer, Button>();

		if (whichThrottle == 'T') {
			tv = (ViewGroup) findViewById(R.id.function_buttons_table_T); //table
		} 
		else if(whichThrottle == 'S')
		{
			tv = (ViewGroup) findViewById(R.id.function_buttons_table_S); //table
		}
		else {
			tv = (ViewGroup) findViewById(R.id.function_buttons_table_G); //table
		}

		//note: we make a copy of function_labels_x because TA might change it while we are using it (causing issues during button update below)
		if (whichThrottle == 'T' && mainapp.function_labels_T != null && mainapp.function_labels_T.size()>0) {
			function_labels_temp = new LinkedHashMap<Integer, String>(mainapp.function_labels_T);
		} else if (whichThrottle == 'S' && mainapp.function_labels_S != null  && mainapp.function_labels_S.size()>0) {
			function_labels_temp = new LinkedHashMap<Integer, String>(mainapp.function_labels_S);
		} 
		else if(whichThrottle == 'G' && mainapp.function_labels_G != null  && mainapp.function_labels_G.size()>0)
		{
			function_labels_temp = new LinkedHashMap<Integer, String>(mainapp.function_labels_G);
		}
		else {
			function_labels_temp = mainapp.function_labels_default;
		}

		//put values in array for indexing in next step TODO: find direct way to do this
		ArrayList<Integer> aList = new ArrayList<Integer>();
		for (Integer f : function_labels_temp.keySet()) {
			aList.add(f);
		}

		for(int i = 0; i < tv.getChildCount(); i++) {
			r = (ViewGroup)tv.getChildAt(i);
			for(int j = 0; j < r.getChildCount(); j++) {
				b = (Button)r.getChildAt(j);
				if (k <  function_labels_temp.size()) {
					Integer func = aList.get(k);
					functionButtonMap.put(func, b);	//save function to button mapping
					fbtl=new function_button_touch_listener(func, whichThrottle);
					b.setOnTouchListener(fbtl);
					String bt = function_labels_temp.get(func) + "        ";  //pad with spaces, and limit to 7 characters
					b.setText(bt.substring(0, 7));
					b.setVisibility(VISIBLE);
					b.setEnabled(false);  //start out with everything disabled
				} else {
					b.setVisibility(GONE);
				}
				k++;
			}
		}

		//update the function-to-button map for the current throttle
		if(whichThrottle == 'T')
			functionMapT = functionButtonMap;
		else if(whichThrottle == 'S')
		{
			functionMapS = functionButtonMap;
		}
		else
			functionMapG = functionButtonMap;
	}

	//lookup and set values of various informational text labels and size the screen elements 
	@SuppressWarnings("deprecation")
	private void set_labels() {

		//	  Log.d("Engine_Driver","starting set_labels");

		int throttle_count = 0;
		int height_T = 0; //height of first throttle area
		int height_S = 0; //height of second throttle area
		int height_G = 0;//height of third throttle area

		// hide or display volume control indicator based on variable
		View viT = findViewById(R.id.volume_indicator_T);
		View viS = findViewById(R.id.volume_indicator_S);
		View viG = findViewById(R.id.volume_indicator_G);
		if (whichVolume == 'T') {
			viT.setVisibility(VISIBLE);
			viS.setVisibility(GONE);
			viG.setVisibility(GONE);
		} 
		else if(whichVolume == 'S')
		{
			viT.setVisibility(GONE);
			viS.setVisibility(VISIBLE);
			viG.setVisibility(GONE);
		}
		else {
			viT.setVisibility(GONE);
			viS.setVisibility(GONE);
			viG.setVisibility(VISIBLE);
		}

		SeekBar sbT=(SeekBar)findViewById(R.id.speed_T);
		SeekBar sbS=(SeekBar)findViewById(R.id.speed_S);
		SeekBar sbG=(SeekBar)findViewById(R.id.speed_G);

		// set up max speeds for throttles
		String s = prefs.getString("maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
		int maxThrottle;
		try {
			maxThrottle = Integer.parseInt(s);
		} 
		catch (Exception e) {
			maxThrottle = MAX_SPEED_DISPLAY;
		}

		sbT.setMax((int) Math.round(((double)(maxThrottle)/MAX_SPEED_DISPLAY) * MAX_SPEED_VAL_T));
		sbS.setMax((int) Math.round(((double)(maxThrottle)/MAX_SPEED_DISPLAY) * MAX_SPEED_VAL_S));
		sbG.setMax((int) Math.round(((double)(maxThrottle)/MAX_SPEED_DISPLAY) * MAX_SPEED_VAL_G));

		// increase height of throttle slider (if requested in preferences)
		boolean ish = prefs.getBoolean("increase_slider_height_preference", 
				getResources().getBoolean(R.bool.prefIncreaseSliderHeightDefaultValue));

		final DisplayMetrics dm = getResources().getDisplayMetrics();
		// Get the screen's density scale

		final float denScale = dm.density;
		// Convert the dps to pixels, based on density scale
		int newHeight;
		if (ish) {
			newHeight = (int) (80 * denScale + 0.5f) ;  //increased height
		} else {
			newHeight = (int) (50 * denScale + 0.5f );  //normal height
		}

		Button b=(Button)findViewById(R.id.button_select_loco_T);
		if (mainapp.consistT.toString().length() > 18) {  //shrink text if long
			b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		} else {
			b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		}

		if (mainapp.consistT.isEmpty()) {
			b.setText("Press to select");
			whichVolume = 'S';  //set the "other" one to use volume control 
		} else {
			b.setText(mainapp.consistT.toString());
			throttle_count++;
		}

		b.setSelected(false);
		b.setPressed(false);

		b=(Button)findViewById(R.id.button_select_loco_S);
		if (mainapp.consistS.toString().length() > 18) {  //shrink text if long
			b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		} else {
			b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		}
		if (mainapp.consistS.isEmpty()) {
			b.setText("Press to select");
			whichVolume = 'G';  //set the "other" one to use volume control 
		} else {
			b.setText(mainapp.consistS.toString());
			throttle_count++;
		}

		b.setSelected(false);
		b.setPressed(false);

		b=(Button)findViewById(R.id.button_select_loco_G);
		if (mainapp.consistG.toString().length() > 18) {  //shrink text if long
			b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		} else {
			b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		}
		if (mainapp.consistG.isEmpty()) {
			b.setText("Press to select");
			whichVolume = 'T';  //set the "other" one to use volume control 
		} else {
			b.setText(mainapp.consistG.toString());
			throttle_count++;
		}

		b.setSelected(false);
		b.setPressed(false);

		View v = findViewById(R.id.throttle_screen_wrapper);
		int screenHeight = v.getHeight();  //get the height of usable area
		int sW = v.getWidth();
		if(screenHeight == 0) {
			//throttle screen hasn't been drawn yet, so use display metrics for now
			screenHeight = dm.heightPixels - (int)(titleBar * (dm.densityDpi/160.));	//allow for title bar, etc	
		}
		if(sW == 0)
		{
			sW = dm.widthPixels;
		}

		// save 1/2 the screen for webview
		if (webViewLocation.equals("Top") || webViewLocation.equals("Bottom")) {
			screenHeight *= 0.5; 
		}

		//Sets up speed arrow buttons width;
		Button bLT=(Button)findViewById(R.id.Left_speed_button_T);
		Button bRT=(Button)findViewById(R.id.Right_speed_button_T);
		Button bLS=(Button)findViewById(R.id.Left_speed_button_S);
		Button bRS=(Button)findViewById(R.id.Right_speed_button_S);
		Button bLG=(Button)findViewById(R.id.Left_speed_button_G);
		Button bRG=(Button)findViewById(R.id.Right_speed_button_G);

		LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,  newHeight); 

		LinearLayout l=(LinearLayout)findViewById(R.id.Throttle_T_SetSpeed);

		l.setLayoutParams(llLp);

		if(prefs.getBoolean("display_speed_arrows_buttons", false))
		{
			int pL = (int) (sW * 0.10);
			int pS = (int) (sW * 0.80);

			llLp = new LinearLayout.LayoutParams(
					pL,  newHeight);

			bLT.setLayoutParams(llLp);
			bRT.setLayoutParams(llLp);
			bLS.setLayoutParams(llLp);
			bRS.setLayoutParams(llLp);
			bLG.setLayoutParams(llLp);
			bRG.setLayoutParams(llLp);
			
			llLp = new LinearLayout.LayoutParams(
					pS,  newHeight); 

			sbT.setLayoutParams(llLp);
			sbS.setLayoutParams(llLp);
			sbG.setLayoutParams(llLp);
		}
		else
		{
			llLp = new LinearLayout.LayoutParams(
					0,  0);

			bLT.setLayoutParams(llLp);
			bRT.setLayoutParams(llLp);
			bLS.setLayoutParams(llLp);
			bRS.setLayoutParams(llLp);
			bLG.setLayoutParams(llLp);
			bRG.setLayoutParams(llLp);

			llLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT,  newHeight);

			sbT.setLayoutParams(llLp);
			sbS.setLayoutParams(llLp);
			sbG.setLayoutParams(llLp);
		}

		//TODO: Fix graphic error when updating view, so blue dot and line match the new width when sliding.
		if(mainapp.firstCreate)
		{

			String leftS = prefs.getString("left_slider_margin", getApplicationContext().getResources().getString(R.string.prefSliderLeftMarginDefaultValue));
			String rightS = prefs.getString("right_slider_margin", getApplicationContext().getResources().getString(R.string.prefSliderRightMarginDefaultValue));

			int left;
			int right;

			try {
				left = Integer.parseInt(leftS);
			} 
			catch (Exception e) {
				left = 8;
			}
			try {
				right = Integer.parseInt(rightS);
			} 
			catch (Exception e) {
				right = 8;
			}

			sbS.setPadding(left, 0, right, 0);
			sbG.setPadding(left, 0, right, 0);
			sbT.setPadding(left, 0, right, 0);

			mainapp.firstCreate = false;
		}

		if (screenHeight > throttleMargin) {	//don't do this if height is invalid
			//determine how to split the screen (evenly if all three, 45/45/10 for two, 80/10/10 if only one)
			screenHeight -= throttleMargin;
			/*boolean useOneThrot = prefs.getBoolean("use_one_throttle_preference",
        		getResources().getBoolean(R.bool.prefUseOneThrottleDefaultValue));*/
			String numThrot = prefs.getString("NumThrottle", getResources().getString(R.string.prefNumOfThrottlesDefault));

			if(numThrot.matches("One"))
			{
				height_T = screenHeight;
				height_S = 0;
				height_G = 0;
			}
			else if(numThrot.matches("Two") && mainapp.consistS.isEmpty())
			{
				height_T = (int) (screenHeight * 0.9);
				height_S = (int) (screenHeight * 0.10);
				height_G = 0;
			}
			else if(numThrot.matches("Two") && mainapp.consistT.isEmpty())
			{
				height_T = (int) (screenHeight * 0.10);
				height_S = (int) (screenHeight * 0.9);
				height_G = 0;
			}
			else if(numThrot.matches("Two"))
			{
				height_T = (int) (screenHeight * 0.5);
				height_S = (int) (screenHeight * 0.5);
				height_G = 0;
			}
			else if (throttle_count == 0 || throttle_count == 3)  {
				height_T = (int) (screenHeight * 0.33);
				height_S = (int) (screenHeight * 0.33);
				height_G = (int) (screenHeight * 0.33);

			} else if (mainapp.consistT.isEmpty() && mainapp.consistS.isEmpty()) {
				height_T = (int) (screenHeight * 0.10);
				height_S = (int) (screenHeight * 0.10);
				height_G = (int) (screenHeight * 0.80);
			} 

			else if(mainapp.consistT.isEmpty() && mainapp.consistG.isEmpty())
			{
				height_T = (int) (screenHeight * 0.10);
				height_S = (int) (screenHeight * 0.80);
				height_G = (int) (screenHeight * 0.10);
			}

			else if(mainapp.consistS.isEmpty() && mainapp.consistG.isEmpty())
			{
				height_T = (int) (screenHeight * 0.80);
				height_S = (int) (screenHeight * 0.10);
				height_G = (int) (screenHeight * 0.10);
			}

			else if(mainapp.consistT.isEmpty())
			{
				height_T = (int) (screenHeight * 0.10);
				height_S = (int) (screenHeight * 0.45);
				height_G = (int) (screenHeight * 0.45);
			}

			else if(mainapp.consistS.isEmpty())
			{
				height_T = (int) (screenHeight * 0.45);
				height_S = (int) (screenHeight * 0.10);
				height_G = (int) (screenHeight * 0.45);
			}

			else {
				height_T = (int) (screenHeight * 0.45);
				height_S = (int) (screenHeight * 0.45);
				height_G = (int) (screenHeight * 0.10);
			}

			//set height of T area 
			LinearLayout ll=(LinearLayout)findViewById(R.id.throttle_T);
			llLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT,
					height_T);
			llLp.bottomMargin = (int)(throttleMargin * (dm.densityDpi/160.));
			ll.setLayoutParams(llLp);
			
			//Used to get speed slider top and bottom.
			b=(Button)findViewById(R.id.button_select_loco_T);
			Button b2=(Button)findViewById(R.id.button_fwd_T);
			
			//update throttle top/bottom
			T_top= ll.getTop()+ sbT.getTop();
			T_bottom= ll.getTop()+ sbT.getBottom() + b.getHeight() + b2.getHeight();

			//set height of S area
			ll=(LinearLayout)findViewById(R.id.throttle_S);
			llLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT,
					height_S);
			ll.setLayoutParams(llLp);
			
			//Used to get speed slider top and bottom.
			b=(Button)findViewById(R.id.button_select_loco_S);
			b2=(Button)findViewById(R.id.button_fwd_S);
			
			//update throttle top/bottom
			S_top= ll.getTop()+sbS.getTop();
			S_bottom= ll.getTop()+sbS.getBottom() + b.getHeight() + b2.getHeight();

			//set height of G area
			ll=(LinearLayout)findViewById(R.id.throttle_G);
			llLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT,
					height_G);
			ll.setLayoutParams(llLp);
			
			//Used to get speed slider top and bottom.
			b=(Button)findViewById(R.id.button_select_loco_G);
			b2=(Button)findViewById(R.id.button_fwd_G);
			
			//update throttle top/bottom
			G_top= ll.getTop()+sbS.getTop();
			G_bottom= ll.getTop()+sbS.getBottom() + b.getHeight() + b2.getHeight();
		}

		//update the direction indicators
		showDirectionIndication('T', dirT);
		showDirectionIndication('S', dirS);
		showDirectionIndication('G', dirG);

		//update the state of each function button based on shared variable
		set_all_function_states('T');
		set_all_function_states('S');
		set_all_function_states('G');

		if(TMenu != null)
		{
			mainapp.displayEStop(TMenu);
			mainapp.displayPowerStateMenuButton(TMenu);
		}
		v.invalidate();
		//	  Log.d("Engine_Driver","ending set_labels");
	}


	@SuppressWarnings("deprecation")
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		//Handle pressing of the back button to release the selected loco and end this activity
		if(key==KeyEvent.KEYCODE_BACK)  {
			if(webView.canGoBack() && !clearHistory) {
				scale = webView.getScale();	// save scale
				webView.goBack();
				webView.setInitialScale((int)(100 * scale));	// restore scale
			}
			else
				mainapp.checkExit(this);
			return (true);	//stop processing this key
		} 
		else if((key==KeyEvent.KEYCODE_VOLUME_UP) || (key==KeyEvent.KEYCODE_VOLUME_DOWN) ) { //use volume to change speed for specified loco
			SeekBar sb = null;
			if (whichVolume == 'T' && !mainapp.consistT.isEmpty()) {
				sb=(SeekBar)findViewById(R.id.speed_T);
			}
			if (whichVolume == 'S' && !mainapp.consistS.isEmpty()) {
				sb=(SeekBar)findViewById(R.id.speed_S);
			}
			if (whichVolume == 'G' && !mainapp.consistG.isEmpty()) {
				sb=(SeekBar)findViewById(R.id.speed_G);
			}
			if (sb != null) {
				if (key==KeyEvent.KEYCODE_VOLUME_UP) {
					sb.setProgress(sb.getProgress() + 1 ); //increase 
				} else {
					sb.setProgress(sb.getProgress() - 1 );  //decrease (else is VOLUME_DOWN)
				}
			}
			return(true);  //stop processing this key
		}
		return(super.onKeyDown(key, event)); //continue with normal key processing
	}

	private void disconnect() {
		//release the locos
		mainapp.consistT.release();
		mainapp.consistS.release();
		mainapp.consistG.release();
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", (int) 'T');	  //release first loco
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", (int) 'S');	  //release second loco
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", (int) 'G');	  //release third loco

		webView.stopLoading();
		this.finish();  //end this activity
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.throttle_menu, menu);
		TMenu = menu;
		mainapp.displayEStop(menu);
		mainapp.displayPowerStateMenuButton2(menu);
		mainapp.setPowerStateButton(menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		Intent in;
		switch (item.getItemId()) {
		case R.id.turnouts_mnu:
			in=new Intent().setClass(this, turnouts.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			break;
		case R.id.routes_mnu:
			in = new Intent().setClass(this, routes.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.web_mnu:
			in=new Intent().setClass(this, web_activity.class);
			startActivity(in);
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
		case R.id.settings_mnu:
			in=new Intent().setClass(this, function_settings.class);
			startActivityForResult(in, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.about_mnu:
			in=new Intent().setClass(this, about_page.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.logviewer_menu:
			Intent logviewer=new Intent().setClass(this, LogViewerActivity.class);
			startActivity(logviewer);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EmerStop:
			SeekBar sb;

			sb=(SeekBar)findViewById(R.id.speed_T);
			sb.setProgress(0);

			sb=(SeekBar)findViewById(R.id.speed_S);
			sb.setProgress(0);

			sb=(SeekBar)findViewById(R.id.speed_G);
			sb.setProgress(0);

			EStop();
			break;
		case R.id.power_layout_button:
			if(mainapp.power_state == null)
			{
				AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setIcon(android.R.drawable.ic_dialog_alert);
				b.setTitle("Will Not Work!"); 
				b.setMessage("JMRI has the wiThrottle power control setting to off.\n\nWill now remove Power Icon.\n\nWill display agian when JMRI setting is on.");
				b.setCancelable(true);
				b.setNegativeButton("OK", null);
				AlertDialog alert = b.create();
				alert.show();
				mainapp.displayPowerStateMenuButton(TMenu);
			}
			else
			{
				mainapp.powerStateMenuButton();
			}
			break;
		case R.id.EditConsistT_menu:
			Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);

			String addr = "";

			ArrayList <ConLoco>swe = new ArrayList<ConLoco>();

			swe.addAll(mainapp.consistT.getLocos());

			addr = swe.get(swe.size() - 1).getAddress();

			consistEdit.putExtra("address", addr);
			consistEdit.putExtra("whichThrottle", 'T');
			startActivityForResult(consistEdit, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EditConsistS_menu:
			Intent consistEdit2 = new Intent().setClass(this, ConsistEdit.class);

			String addr2 = "";

			ArrayList <ConLoco>swe2 = new ArrayList<ConLoco>();

			swe2.addAll(mainapp.consistS.getLocos());

			addr = swe2.get(swe2.size() - 1).getAddress();

			consistEdit2.putExtra("address", addr2);
			consistEdit2.putExtra("whichThrottle", 'S');
			startActivityForResult(consistEdit2, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EditConsistG_menu:
			Intent consistEdit3 = new Intent().setClass(this, ConsistEdit.class);

			String addr3 = "";

			ArrayList <ConLoco>swe3 = new ArrayList<ConLoco>();

			swe3.addAll(mainapp.consistG.getLocos());

			addr3 = swe3.get(swe3.size() - 1).getAddress();

			consistEdit3.putExtra("address", addr3);
			consistEdit3.putExtra("whichThrottle", 'G');
			startActivityForResult(consistEdit3, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void EStop()
	{
		sendSpeedMsg('T', 0);
		sendSpeedMsg('S', 0);
		sendSpeedMsg('G', 0);
	}


	//handle return from menu items
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 1) {					// select loco
			if(resultCode == RESULT_FIRST_USER) {									//something about consist was changed
				Bundle extras = data.getExtras();
				if (extras != null) {
					char whichThrottle = extras.getString("whichThrottle").charAt(0);
					//int dir = (whichThrottle == 'T') ? dirT : dirS;
					int dir;
					if(whichThrottle == 'T')
					{
						dir = dirT;
					}
					else if(whichThrottle == 'S')
					{
						dir = dirS;
					}
					else
					{
						dir = dirG;
					}

					setEngineDirection(whichThrottle, dir, false);				//update direction for each loco in consist
				}
				//update loco name
			}
		}
		// loop through all function buttons and
		//  set label and dcc functions (based on settings) or hide if no label
		set_function_labels_and_listeners_for_view('T');
		set_function_labels_and_listeners_for_view('S');
		set_function_labels_and_listeners_for_view('G');
		set_labels();
	}

	// touch events outside the GestureOverlayView get caught here 
	@Override
	public boolean onTouchEvent(MotionEvent event){
		//	  Log.d("Engine_Driver", "onTouch Title action " + event.getAction());
		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			gestureStart(event);
			break;
		case MotionEvent.ACTION_UP:
			gestureEnd(event);
			break;
		case MotionEvent.ACTION_MOVE:
			gestureMove(event);
			break;
		case MotionEvent.ACTION_CANCEL:
			gestureCancel(event);
		}
		return true;
	}

	@Override
	public void onGesture(GestureOverlayView arg0, MotionEvent event) {
		gestureMove(event);
	}

	@Override
	public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
		gestureCancel(event);
	}

	//determine if the action was long enough to be a swipe
	@Override
	public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
		gestureEnd(event);
	}

	@Override
	public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
		gestureStart(event);
	}

	private void gestureStart(MotionEvent event ) {
		gestureStartX = event.getX();
		gestureStartY = event.getY();
		//Log.d("Engine_Driver", "gestureStart y=" + gestureStartY);


		//if gesture is attempting to start over an enabled slider, ignore it and return immediately.
		View sbT=findViewById(R.id.speed_T);
		View sbS=findViewById(R.id.speed_S);
		View sbG=findViewById(R.id.speed_G);
		
		if ((sbT.isEnabled() && gestureStartY >= T_top && gestureStartY <= T_bottom)
				|| (sbS.isEnabled() && gestureStartY >= S_top && gestureStartY <= S_bottom)
				|| (sbG.isEnabled() && gestureStartY >= G_top && gestureStartY <= G_bottom))
		{
			//Log.d("Engine_Driver","exiting gestureStart");
			return;
		}

		gestureInProgress = true;
		gestureFailed = false;
		gestureLastCheckTime = event.getEventTime();
		mVelocityTracker.clear();
		
		// start the gesture timeout timer
		if (mainapp.throttle_msg_handler != null)
			mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
	}

	public void gestureMove(MotionEvent event) {
		//        Log.d("Engine_Driver", "gestureMove action " + event.getAction());
		if(gestureInProgress == true)
		{
			//stop the gesture timeout timer
			mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);

			mVelocityTracker.addMovement(event);
			if((event.getEventTime() - gestureLastCheckTime) > gestureCheckRate) 
			{
				//monitor velocity and fail gesture if it is too low
				gestureLastCheckTime = event.getEventTime();
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000);
				int velocityX = (int) velocityTracker.getXVelocity();
				//		        Log.d("Engine_Driver", "gestureVelocity vel " + velocityX);
				if(Math.abs(velocityX) < threaded_application.min_fling_velocity)
				{
					gestureFailed(event);
				}
			}
			if(gestureInProgress == true)
			{
				// restart the gesture timeout timer
				mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
			}
		}
	}


	private void gestureEnd(MotionEvent event) {
		//        Log.d("Engine_Driver", "gestureEnd action " + event.getAction());
		mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);
		if(gestureInProgress == true)
		{
			if(Math.abs(event.getX() - gestureStartX) > threaded_application.min_fling_distance) 
			{
				// valid gesture.  Change the event action to CANCEL so that it isn't processed by
				// any control below the gesture overlay 
				event.setAction(MotionEvent.ACTION_CANCEL);
				// left to right swipe goes to turnouts
				if(event.getRawX() > gestureStartX) {
					Intent in=new Intent().setClass(this, turnouts.class);
					startActivity(in);
					connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
				}
				// right to left swipe goes to routes
				else {
					Intent in=new Intent().setClass(this, routes.class);
					startActivity(in);
					connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
				}
			}
			else
			{
				// gesture was not long enough
				gestureFailed(event);
			}
		}
	}

	private void gestureCancel(MotionEvent event) {
		if (mainapp.throttle_msg_handler != null)
			mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);
		gestureInProgress = false;
		gestureFailed = true;
	}

	void gestureFailed(MotionEvent event) {
		//end the gesture
		gestureInProgress = false;
		gestureFailed = true;
	}

	//
	// GestureStopped runs when more than gestureCheckRate milliseconds 
	// elapse between onGesture events (i.e. press without movement).
	//
	@SuppressLint("Recycle")
	private Runnable gestureStopped = new Runnable() {
		@Override
		public void run() {
			if(gestureInProgress == true)
			{
				//end the gesture
				gestureInProgress = false;
				gestureFailed = true;
				//create a MOVE event to trigger the underlying control
				View v = findViewById(R.id.throttle_screen);
				if(v != null)  {
					//use uptimeMillis() rather than 0 for time in MotionEvent.obtain() call in throttle gestureStopped:
					MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),MotionEvent.ACTION_MOVE,gestureStartX, gestureStartY,0);
					try {
						v.dispatchTouchEvent(event);
					}
					catch(IllegalArgumentException e) 
					{
						Log.d("Engine_Driver", "gestureStopped trigger IllegalArgumentException, OS " + android.os.Build.VERSION.SDK_INT);
					}
				}
			}
		}
	};

	// helper app to initialize statics (in case GC has not run since app last shutdown)
	// call before instantiating any instances of class
	public static void initStatics() {
		scale = initialScale;
		clearHistory = false;
		currentUrl = null;
	}
}

