/*Copyright (C) 2014 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.lang.reflect.Method;

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

	private threaded_application mainapp; // hold pointer to mainapp
	private SharedPreferences prefs;

	private static final int GONE = 8;
	private static final int VISIBLE = 0;
	private static final int throttleMargin = 8; // margin between the throttles in dp
	private static final int titleBar = 45; // estimate of lost screen height in dp

	// speed scale factors
	public static final int MAX_SPEED_VAL_WIT = 126;	// wit message maximum speed value, max speed slider value
	public static final int SPEED_STEP_CODE_14 = 8;		// wit speed step codes
	public static final int SPEED_STEP_CODE_27 = 4;
	public static final int SPEED_STEP_CODE_28 = 2;
	public static final int SPEED_STEP_CODE_128 = 1;
	private static int maxSpeedStepT = 100;					// throttle speed steps
	private static int maxSpeedStepG = 100;
	private static int maxSpeedStepS = 100;
	private static int max_throttle_change;			 // maximum allowable change of the sliders
	private static boolean displaySpeedSteps;
	private static double displayUnitScaleT;			// display units per slider count
	private static double displayUnitScaleG;
	private static double displayUnitScaleS;

	private static SeekBar sbT; // seekbars
	private static SeekBar sbS;
	private static SeekBar sbG;
	private static ViewGroup fbT; // function button tables
	private static ViewGroup fbS;
	private static ViewGroup fbG;
	private static Button bFwdT; // buttons
	private static Button bStopT;
	private static Button bRevT;
	private static Button bSelT;
	private static Button bRSpdT;
	private static Button bLSpdT;
	private static Button bFwdG;
	private static Button bStopG;
	private static Button bRevG;
	private static Button bSelG;
	private static Button bRSpdG;
	private static Button bLSpdG;
	private static Button bFwdS;
	private static Button bStopS;
	private static Button bRevS;
	private static Button bSelS;
	private static Button bRSpdS;
	private static Button bLSpdS;
	private static TextView tvSpdLabT; // labels
	private static TextView tvSpdValT;
	private static TextView tvSpdLabG;
	private static TextView tvSpdValG;
	private static TextView tvSpdLabS;
	private static TextView tvSpdValS;
	private static View vVolT; // volume indicators
	private static View vVolS;
	private static View vVolG;
	private static LinearLayout llT; // throttles
	private static LinearLayout llS;
	private static LinearLayout llG;
	private static LinearLayout llTSetSpd;
	private static View vThrotScr;
	private static View vThrotScrWrap;

	private boolean navigatingAway = false; // true if another activity was selected (false in onPause if going into background)
	private char whichVolume = 'T';

	// screen coordinates for throttle sliders, so we can ignore swipe on them
	private int T_top;
	private int T_bottom;
	private int S_top;
	private int S_bottom;
	private int G_top;
	private int G_bottom;

	// these are used for gesture tracking
	private float gestureStartX = 0;
	private float gestureStartY = 0;
	private boolean gestureFailed = false; // gesture didn't meet velocity or distance requirement
	private boolean gestureInProgress = false; // gesture is in progress
	private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
	private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
	private VelocityTracker mVelocityTracker;

	// function number-to-button maps
	private LinkedHashMap<Integer, Button> functionMapT;
	private LinkedHashMap<Integer, Button> functionMapS;
	private LinkedHashMap<Integer, Button> functionMapG;

	// current direction
	private int dirT = 1; // request direction for each throttle (single or multiple engines)
	private int dirS = 1;
	private int dirG = 1;

	private static final String noUrl = "file:///android_asset/blank_page.html";
	private static final float initialScale = 1.5f;
	private WebView webView = null;
	private String webViewLocation;
	private static float scale = initialScale; // used to restore throttle web zoom level (after rotation)
	private static boolean clearHistory = false; // flags webViewClient to clear history when page load finishes
	private static String currentUrl = null;
	private boolean currentUrlUpdate;
	private boolean orientationChange = false;
	private String currentTime = "";
	private Menu TMenu;
	static int REP_DELAY = 25;
	static int BUTTON_SPEED_STEP = 4;

	private Handler repeatUpdateHandler = new Handler();
	private boolean mAutoIncrement = false;
	private boolean mAutoDecrement = false;
	
	private static final int changeDelay = 1000;
	private ChangeDelay changeTimerT;
	private ChangeDelay changeTimerG;
	private ChangeDelay changeTimerS;

	private boolean selectLocoRendered = false; // this will be true once set_labels() runs following rendering of the loco select textViews

	// For speed slider speed buttons.
	class RptUpdater implements Runnable {
		char whichThrottle;

		public RptUpdater(char WhichThrottle) {
			whichThrottle = WhichThrottle;

			try {
				REP_DELAY = Integer.parseInt(prefs.getString("speed_arrows_throttle_repeat_delay", "100"));
			} catch (Exception ex) {
				REP_DELAY = 100;
			}
		}

		@Override
		public void run() {
			if (mAutoIncrement) {
				increment(whichThrottle);
				repeatUpdateHandler.postDelayed(new RptUpdater(whichThrottle), REP_DELAY);
			} else if (mAutoDecrement) {
				decrement(whichThrottle);
				repeatUpdateHandler.postDelayed(new RptUpdater(whichThrottle), REP_DELAY);
			}
		}
	}

	// Handle messages from the communication thread TO this thread (responses from withrottle)
	@SuppressLint("HandlerLeak")
	class throttle_handler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case message_type.RESPONSE: { // handle messages from WiThrottle server
				String response_str = msg.obj.toString();
				char com1 = response_str.charAt(0);
				char whichThrottle = response_str.charAt(1);

				switch (com1) {
				// various MultiThrottle messages
				case 'M':
					char com2 = response_str.charAt(2);
					String[] ls = threaded_application.splitByString(response_str, "<;>");
					String addr;
					try {
						addr = ls[0].substring(3);
					} catch (Exception e) {
						addr = "";
					}
					if (whichThrottle == 'T' || whichThrottle == 'S' || whichThrottle == 'G') {
						if (com2 == '+' || com2 == 'L') { // if loco added or function labels updated
							if (com2 == ('+')) {
								// set_default_function_labels();
								enable_disable_buttons(whichThrottle); // direction and slider: pass whichthrottle
							}
							// loop through all function buttons and set label and dcc functions (based on settings) or hide if no label
							set_function_labels_and_listeners_for_view(whichThrottle);
							if (whichThrottle == 'T') {
								enable_disable_buttons_for_view(fbT, true);
							} else if (whichThrottle == 'G') {
								enable_disable_buttons_for_view(fbG, true);
							} else {
								enable_disable_buttons_for_view(fbS, true);
							}
							set_labels();
						} else if (com2 == '-') { // if loco removed
							if (whichThrottle == 'T') {
								removeLoco('T');
							} else if (whichThrottle == 'G') {
								removeLoco('G');
							} else {
								removeLoco('S');
							}
						} else if (com2 == 'A') { // e.g. MTAL2608<;>R1
							char com3 = ls[1].charAt(0);
							if (com3 == 'R') {
								int dir;
								try {
									dir = Integer.valueOf(ls[1].substring(1, 2));
								} catch (Exception e) {
									dir = 1;
								}

								Consist con;
								int curDir;
								if (whichThrottle == 'T') {
									con = mainapp.consistT;
									curDir = dirT;
								} else if (whichThrottle == 'G') {
									con = mainapp.consistG;
									curDir = dirG;
								} else {
									con = mainapp.consistS;
									curDir = dirS;
								}

								if (addr.equals(con.getLeadAddr())) {
									if (dir != curDir) { // lead/consist direction changed from outside of ED
										showDirectionRequest(whichThrottle, dir); 		// update requested direction indication
										setEngineDirection(whichThrottle, dir, true);	// update rest of consist to match new direction
									}
								} else {
									int locoDir = curDir; 				// calc correct direction for this (non-lead) loco
									if (con != null) {
										try {
											if (con.isReverseOfLead(addr))
												locoDir ^= 1;
											if (locoDir != dir) {// if loco has wrong direction then correct it
												mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DIRECTION, addr, whichThrottle, locoDir);
											}
										} catch (Exception e) { 	// isReverseOfLead returns null if addr is not in con
																	// - should not happen unless WiT is reporting on engine user just dropped from ED consist?
											Log.d("Engine_Driver", "throttle " + whichThrottle + " loco " + addr + " direction reported by WiT but engine is not assigned");
										}
									}
								}
							} else if (com3 == 'V') {
								try {
									int speed = Integer.parseInt(ls[1].substring(1));
									speedUpdateWiT(whichThrottle, speed); // update speed slider and indicator
								} catch (Exception e) {
								}
							} else if (com3 == 'F') {
								try {
									int function = Integer.valueOf(ls[1].substring(2));
									set_function_state(whichThrottle, function);
								} catch (Exception e) {
								}
							} else if (com3 == 's') {
								try {
									int speedStepCode = Integer.valueOf(ls[1].substring(1));
									setSpeedSteps(whichThrottle, speedStepCode);
								} catch (Exception e) {
								}
							} else {
								// set_labels();
							}
						}
					}
					break;

				case 'T':
				case 'S':
				case 'G':
					enable_disable_buttons(com1); // pass whichthrottle
					set_labels();
					break;

				case 'R':
					if (whichThrottle == 'F' || whichThrottle == 'S' || whichThrottle == 'G') { // roster function labels - legacy
						if (whichThrottle == 'F') { // used to use 'F' instead of 'T'
							whichThrottle = 'T';
						}
						set_function_labels_and_listeners_for_view(whichThrottle);
						if (whichThrottle == 'T') {
							enable_disable_buttons_for_view(fbT, true);
						} else if (whichThrottle == 'G') {
							enable_disable_buttons_for_view(fbG, true);
						} else {
							enable_disable_buttons_for_view(fbS, true);
						}
						set_labels();

					} else {
						try {
							String scom2 = response_str.substring(1, 6);
							if (scom2.equals("PF}|{")) {
								whichThrottle = response_str.charAt(6);
								set_all_function_states(whichThrottle);
							}
						} catch (IndexOutOfBoundsException e) {
						}
					}
					break;
				case 'P': // panel info
					if (whichThrottle == 'W') // PW - web server port info
						reloadWeb();
					if (whichThrottle == 'P') // PP - power state change
						mainapp.setPowerStateButton(TMenu); // update the power state button
					break;
				} // end of switch
				
				if (!selectLocoRendered) // call set_labels if the select loco textViews had not rendered the last time it was called
					set_labels();
			}
			break;
			case message_type.ROSTER_UPDATE:
				set_labels();				// refresh function labels when any roster response is received
				break;
			case message_type.WIT_CON_RETRY:
				removeLoco('T');
				removeLoco('S');
				removeLoco('G');
				reloadWeb();
				break;
			case message_type.CURRENT_TIME:
				currentTime = msg.obj.toString();
				setTitle();
				break;
			case message_type.DISCONNECT:
			case message_type.SHUTDOWN:
				disconnect();
				break;
			case message_type.WEBVIEW_LOC: 		// webview location changed
				if ("none".equals(webViewLocation)) { 	// if not currently displayed
					currentUrl = null; 					// reload init url
				}
				// set new location
				webViewLocation = prefs
						.getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
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
		load_webview(); // reload
	}

	private void removeLoco(char whichThrottle) {
		enable_disable_buttons(whichThrottle); 		// direction and slider
		set_function_labels_and_listeners_for_view(whichThrottle);
		set_labels();
	}

	// process WiT speed report
	// update speed slider if didn't just send a speed update to WiT
	void speedUpdateWiT(char whichThrottle, int speed) {
		if (speed < 0)
			speed = 0;
		if (whichThrottle == 'T') {
			if(!changeTimerT.delayInProg) {
				sbT.setProgress(speed);
			}
		} else if (whichThrottle == 'G') {
			if(!changeTimerG.delayInProg) {
				sbG.setProgress(speed);
			}
		} else {
			if(!changeTimerS.delayInProg) {
				sbS.setProgress(speed);
			}
		}
	}

	// set speed slider to absolute value
	void speedUpdate(char whichThrottle, int speed) {
		if (speed < 0)
			speed = 0;
		if (whichThrottle == 'T') {
			sbT.setProgress(speed);
		} else if (whichThrottle == 'G') {
			sbG.setProgress(speed);
		} else {
			sbS.setProgress(speed);
		}
	}

	// change speed slider by scaled display unit value
	int speedChange(char whichThrottle, int change) {
		SeekBar throttle_slider;
		double displayUnitScale;
		if (whichThrottle == 'T') {
			throttle_slider = sbT;
			displayUnitScale = displayUnitScaleT;
		} else if (whichThrottle == 'G') {
			throttle_slider = sbG;
			displayUnitScale = displayUnitScaleG;
		} else {
			throttle_slider = sbS;
			displayUnitScale = displayUnitScaleS;
		}
		int lastSpeed = throttle_slider.getProgress();
		int lastScaleSpeed = (int)Math.round(lastSpeed * displayUnitScale);
		int speed = (int)Math.round(lastSpeed + (change / displayUnitScale));
		int scaleSpeed = (int)Math.round(speed * displayUnitScale);
		if(lastScaleSpeed == scaleSpeed) {
			speed += Math.signum(change);
		}
		if (speed < 0)
			speed = 0;
		throttle_slider.setProgress(speed);
		return speed;
	}

	// set speed slider and notify server
	void speedUpdateAndNotify(char whichThrottle, int speed) {
		speedUpdate(whichThrottle, speed);
		sendSpeedMsg(whichThrottle, speed);
	}

	// change speed slider by scaled value and notify server
	void speedChangeAndNotify(char whichThrottle, int change) {
		int speed = speedChange(whichThrottle, change);
		sendSpeedMsg(whichThrottle, speed);
	}

	// set the displayed numeric speed value
	private void setDisplayedSpeed(char whichThrottle, int speed) {
		TextView speed_label;
		Consist con;
		double speedScale;
		if (whichThrottle == 'T') {
			speedScale = displayUnitScaleT;
			speed_label = tvSpdValT;
			con = mainapp.consistT;
		} else if (whichThrottle == 'G') {
			speedScale = displayUnitScaleG;
			speed_label = tvSpdValG;
			con = mainapp.consistG;
		} else {
			speedScale = displayUnitScaleS;
			speed_label = tvSpdValS;
			con = mainapp.consistS;
		}
		if (speed < 0) {
			Toast.makeText(getApplicationContext(), "Alert: Engine " + con.toString() + " is set to ESTOP", Toast.LENGTH_LONG).show();
			speed = 0;
		}
		int scaleSpeed = (int) Math.round(speed * speedScale);
		speed_label.setText(Integer.toString(scaleSpeed));
	}

	private void setSpeedSteps(char whichThrottle, int speedStepCode) {
		int maxSpeedStep;
		switch(speedStepCode) {
		case SPEED_STEP_CODE_128:
			maxSpeedStep = 126;
			break;
		case SPEED_STEP_CODE_27:
			maxSpeedStep = 27;
			break;
		case SPEED_STEP_CODE_14:
			maxSpeedStep = 14;
			break;
		case SPEED_STEP_CODE_28:
			maxSpeedStep = 28;
			break;
		default:
			maxSpeedStep = 100;
			break;
		}
		if(whichThrottle == 'T') {
			maxSpeedStepT = maxSpeedStep;
		} else if(whichThrottle == 'G') {
			maxSpeedStepG = maxSpeedStep;
		} else {
			maxSpeedStepS = maxSpeedStep;
		}
		setDisplayUnitScale(whichThrottle);
	}
	
	private void setDisplayUnitScale(char whichThrottle) {
		if(whichThrottle == 'T') {
			displayUnitScaleT = calcDisplayUnitScale(maxSpeedStepT);
			tvSpdLabT.setText(calcDisplayUnitLabelId(maxSpeedStepT));
		} else if(whichThrottle == 'G') {
			displayUnitScaleG = calcDisplayUnitScale(maxSpeedStepG);
			tvSpdLabG.setText(calcDisplayUnitLabelId(maxSpeedStepG));
		} else {
			displayUnitScaleS = calcDisplayUnitScale(maxSpeedStepS);
			tvSpdLabS.setText(calcDisplayUnitLabelId(maxSpeedStepS));
		}
	}
	
	private double calcDisplayUnitScale(int maxSpeedStep) {
		int max = 100;
		if(displaySpeedSteps) {
			max = maxSpeedStep;
		}
		return max / (double)MAX_SPEED_VAL_WIT;
	}

	private int calcDisplayUnitLabelId(int maxSpeedStep) {
		int labId = R.string.label_percent;
		if(displaySpeedSteps) {
			switch(maxSpeedStep) {
			case 14:
				labId = R.string.label_14step;
				break;
			case 27:
				labId = R.string.label_27step;
				break;
			case 28:
				labId = R.string.label_28step;
				break;
			case 126:
				labId = R.string.label_128step;
				break;
			default:
				labId = R.string.label_percent;
				break;
			}
		}
		return labId;
	}
	
	
	// set the direction for all engines on the throttle
	// if skipLead is true, the direction is not set for the lead engine
	void setEngineDirection(char whichThrottle, int direction, boolean skipLead) {
		Consist con;
		if (whichThrottle == 'T')
			con = mainapp.consistT;
		else if (whichThrottle == 'G')
			con = mainapp.consistG;
		else
			con = mainapp.consistS;
		String leadAddr = con.getLeadAddr();
		for (String addr : con.getList()) { // loop through each engine in
											// consist
			if (!skipLead || !addr.equals(leadAddr)) {
				int locoDir = direction;
				try {
					if (con.isReverseOfLead(addr)) // if engine faces opposite of lead loco
						locoDir ^= 1; // then reverse the commanded direction
					mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DIRECTION, addr, whichThrottle, locoDir);
				} catch (Exception e) { // isReverseOfLead returns null if addr is not in con - should never happen since we are walking through consist list
					Log.d("Engine_Driver", "throttle " + whichThrottle + " direction change for unselected loco " + addr);
				}
			}
		}
	}

	// indicate direction using the button pressed state
	void showDirectionIndication(char whichThrottle, int direction) {
		Button bFwd;
		Button bRev;
		if (whichThrottle == 'T') {
			bFwd = bFwdT;
			bRev = bRevT;
		} else if (whichThrottle == 'G') {
			bFwd = bFwdG;
			bRev = bRevG;
		} else {
			bFwd = bFwdS;
			bRev = bRevS;
		}

		if (direction == 0) {
			bFwd.setPressed(false);
			bRev.setPressed(true);
		} else {
			bFwd.setPressed(true);
			bRev.setPressed(false);
		}
	}

	// indicate requested direction using the button typeface
	void showDirectionRequest(char whichThrottle, int direction) {
		Button bFwd;
		Button bRev;
		if (whichThrottle == 'T') {
			bFwd = bFwdT;
			bRev = bRevT;
			dirT = direction;
		} else if (whichThrottle == 'G') {
			bFwd = bFwdG;
			bRev = bRevG;
			dirG = direction;
		} else {
			bFwd = bFwdS;
			bRev = bRevS;
			dirS = direction;
		}

		if (direction == 0) {
			bFwd.setTypeface(null, Typeface.NORMAL);
			bRev.setTypeface(null, Typeface.ITALIC);
		} else {
			bFwd.setTypeface(null, Typeface.ITALIC);
			bRev.setTypeface(null, Typeface.NORMAL);
		}

		/*
		 * this code gathers direction feedback for the direction indication
		 * 
		 * if (mainapp.withrottle_version < 2.0) { // no feedback avail so just
		 * let indication follow request showDirectionIndication(whichThrottle,
		 * direction); } else { //get confirmation of direction changes
		 * mainapp.sendMsgDelay(mainapp.comm_msg_handler, 100,
		 * message_type.REQ_DIRECTION, "", (int) whichThrottle, 0); }
		 * 
		 * due to response lags, for now just track the setting: //******
		 */
		showDirectionIndication(whichThrottle, direction);
	}

	void set_stop_button(char whichThrottle, boolean pressed) {
		Button bStop;
		if (whichThrottle == 'T') {
			bStop = bStopT;
		} else if (whichThrottle == 'G') {
			bStop = bStopG;
		} else {
			bStop = bStopS;
		}
		if (pressed == true) {
			bStop.setPressed(true);
			bStop.setTypeface(null, Typeface.ITALIC);
		} else {
			bStop.setPressed(false);
			bStop.setTypeface(null, Typeface.NORMAL);
		}
	}

	void start_select_loco_activity(char whichThrottle) {
		// give feedback that select button was pressed
		if (whichThrottle == 'T') {
			bSelT.setPressed(true);
		} else if (whichThrottle == 'G') {
			bSelG.setPressed(true);
		} else {
			bSelS.setPressed(true);
		}
		try {
			Intent select_loco = new Intent().setClass(this, select_loco.class);
			select_loco.putExtra("sWhichThrottle", Character.toString(whichThrottle)); 	// pass whichThrottle as an extra to activity
			navigatingAway = true;
			startActivityForResult(select_loco, 1);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		} catch (Exception ex) {
			Log.d("debug", ex.getMessage());
		}
	};

	void enable_disable_buttons(char whichThrottle) {
		boolean newEnabledState;
		if (whichThrottle == 'T') {
			newEnabledState = mainapp.consistT.isActive(); 		// set false if lead loco is not assigned
			bFwdT.setEnabled(newEnabledState);
			bStopT.setEnabled(newEnabledState);
			bRevT.setEnabled(newEnabledState);
			tvSpdLabT.setEnabled(newEnabledState);
			tvSpdValT.setEnabled(newEnabledState);
			bLSpdT.setEnabled(newEnabledState);
			bRSpdT.setEnabled(newEnabledState);
			enable_disable_buttons_for_view(fbT, newEnabledState);
			if (!newEnabledState) {
				sbT.setProgress(0); // set slider to 0 if disabled
			}
			sbT.setEnabled(newEnabledState);
		} else if (whichThrottle == 'G') {
			newEnabledState = (mainapp.consistG.isActive()); 	// set false if lead loco is not assigned
			bFwdG.setEnabled(newEnabledState);
			bStopG.setEnabled(newEnabledState);
			bRevG.setEnabled(newEnabledState);
			tvSpdLabG.setEnabled(newEnabledState);
			tvSpdValG.setEnabled(newEnabledState);
			bLSpdG.setEnabled(newEnabledState);
			bRSpdG.setEnabled(newEnabledState);
			enable_disable_buttons_for_view(fbG, newEnabledState);
			if (!newEnabledState) {
				sbG.setProgress(0); // set slider to 0 if disabled
			}
			sbG.setEnabled(newEnabledState);
		} else {
			newEnabledState = (mainapp.consistS.isActive()); 	// set false if lead loco is not assigned
			bFwdS.setEnabled(newEnabledState);
			bStopS.setEnabled(newEnabledState);
			bRevS.setEnabled(newEnabledState);
			tvSpdLabS.setEnabled(newEnabledState);
			tvSpdValS.setEnabled(newEnabledState);
			bLSpdS.setEnabled(newEnabledState);
			bRSpdS.setEnabled(newEnabledState);
			enable_disable_buttons_for_view(fbS, newEnabledState);
			if (!newEnabledState) {
				sbS.setProgress(0); // set slider to 0 if disabled
			}
			sbS.setEnabled(newEnabledState);
		}
	}; // end of enable_disable_buttons

	// helper function to enable/disable all children for a group
	void enable_disable_buttons_for_view(ViewGroup vg, boolean newEnabledState) {
		// Log.d("Engine_Driver","starting enable_disable_buttons_for_view " +
		// newEnabledState);

		ViewGroup r; // row
		Button b; // button
		for (int i = 0; i < vg.getChildCount(); i++) {
			r = (ViewGroup) vg.getChildAt(i);
			for (int j = 0; j < r.getChildCount(); j++) {
				b = (Button) r.getChildAt(j);
				b.setEnabled(newEnabledState);
			}
		}
	} // enable_disable_buttons_for_view

	// update the appearance of all function buttons
	void set_all_function_states(char whichThrottle) {
		// Log.d("Engine_Driver","set_function_states");

		LinkedHashMap<Integer, Button> fMap;
		if (whichThrottle == 'T') {
			fMap = functionMapT;
		} else if (whichThrottle == 'G') {
			fMap = functionMapG;
		} else {
			fMap = functionMapS;
		}

		for (Integer f : fMap.keySet()) {
			set_function_state(whichThrottle, f);
		}
	}

	// update a function button appearance based on its state
	void set_function_state(char whichThrottle, int function) {
		// Log.d("Engine_Driver","starting set_function_request");
		Button b;
		boolean[] fs; 	// copy of this throttle's function state array
		if (whichThrottle == 'T') {
			b = functionMapT.get(function);
			fs = mainapp.function_states_T;
		} else if (whichThrottle == 'G') {
			b = functionMapG.get(function);
			fs = mainapp.function_states_G;
		} else {
			b = functionMapS.get(function);
			fs = mainapp.function_states_S;
		}
		if (b != null && fs != null) {
			if (fs[function] == true) {
				b.setTypeface(null, Typeface.ITALIC);
				b.setPressed(true);
			} else {
				b.setTypeface(null, Typeface.NORMAL);
				b.setPressed(false);
			}
		}
	}

	/*
	 * future use: displays the requested function state independent of (in addition to) feedback state 
	 * todo: need to handle momentary buttons somehow
	 */
	void set_function_request(char whichThrottle, int function, int reqState) {
		// Log.d("Engine_Driver","starting set_function_request");
		Button b;
		if (whichThrottle == 'T') {
			b = functionMapT.get(function);
		} else if (whichThrottle == 'G') {
			b = functionMapG.get(function);
		} else {
			b = functionMapS.get(function);
		}
		if (b != null) {
			if (reqState != 0) {
				b.setTypeface(null, Typeface.ITALIC);
			} else {
				b.setTypeface(null, Typeface.NORMAL);
			}
		}
	}

	public class select_function_button_touch_listener implements View.OnClickListener {
		char whichThrottle; 	// T for first throttle, S for second, G for third

		public select_function_button_touch_listener(char new_whichThrottle) {
			whichThrottle = new_whichThrottle;
		}

		@Override
		public void onClick(View v) {
			start_select_loco_activity(whichThrottle); // pass throttle #
		}
	}

	public class arrow_speed_button_touch_listener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
		char whichThrottle; 	// T for first throttle, S for second, G for third
		String arrowDirection;

		public arrow_speed_button_touch_listener(char new_whichThrottle, String new_arrowDirection) {
			whichThrottle = new_whichThrottle;
			arrowDirection = new_arrowDirection;
		}

		@Override
		public boolean onLongClick(View v) {
			if (arrowDirection.equals("right")) {
				mAutoIncrement = true;
			} else {
				mAutoDecrement = true;
			}
			repeatUpdateHandler.post(new RptUpdater(whichThrottle));
			return false;
		}

		@Override
		public void onClick(View v) {
			if (arrowDirection.equals("right")) {
				mAutoIncrement = false;
				increment(whichThrottle);
			} else {
				mAutoDecrement = false;
				decrement(whichThrottle);
			}
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoIncrement) {
				mAutoIncrement = false;
			}

			if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoDecrement) {
				mAutoDecrement = false;
			}
			return false;
		}
	}

	public class function_button_touch_listener implements View.OnTouchListener {
		int function;
		char whichThrottle;		// T for first throttle, S for second, G for third

		// public function_button_touch_listener(int new_function, boolean
		// new_toggle_type, String new_whichThrottle)
		public function_button_touch_listener(int new_function, char new_whichThrottle) {
			function = new_function; 	// store these values for this button
			whichThrottle = new_whichThrottle;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// Log.d("Engine_Driver", "onTouch func " + function + " action " +
			// event.getAction());

			// make the click sound once
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.playSoundEffect(SoundEffectConstants.CLICK);
			}

			// if gesture in progress, skip button processing
			if (gestureInProgress == true) {
				return (true);
			}

			// if gesture just failed, insert one DOWN event on this control
			if (gestureFailed == true) {
				handleAction(MotionEvent.ACTION_DOWN);
				gestureFailed = false; 	// just do this once
			}
			handleAction(event.getAction());
			return (true);
		}

		private void handleAction(int action) {
			String throt = Character.toString(whichThrottle);

			switch (action) {
			case MotionEvent.ACTION_DOWN: {
				switch (this.function) {
				case function_button.FORWARD:
				case function_button.REVERSE: {
					int dir = (function == function_button.FORWARD ? 1 : 0);
					showDirectionRequest(whichThrottle, dir);		 // update requested direction indication
					setEngineDirection(whichThrottle, dir, false);	 // update direction for each engine on this throttle
					break;
				}
				case function_button.STOP: {
					set_stop_button(whichThrottle, true);
					speedUpdateAndNotify(whichThrottle, 0);
				}
				case function_button.SPEED_LABEL: { // specify which throttle the volume button controls
					whichVolume = whichThrottle; 	// use whichever was clicked
					set_labels();
					break;
				}

				default: { // handle the function buttons
					Consist con;
					if (whichThrottle == 'T') {
						con = mainapp.consistT;
					} else if (whichThrottle == 'G') {
						con = mainapp.consistG;
					} else {
						con = mainapp.consistS;
					}

					String addr = con.getLeadAddr();
					mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, whichThrottle + addr, this.function, 1);
					// set_function_request(whichThrottle, function, 1);
				}
				}

			}
				break;
			// handle stopping of function on key-up
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:

				if (function == function_button.STOP) {
					set_stop_button(whichThrottle, false);
				}
				// only process UP for function buttons
				else if (function < function_button.FORWARD) {
					// Consist con = (whichThrottle == 'T') ? mainapp.consistT :
					// mainapp.consistS;
					Consist con;
					if (whichThrottle == 'T') {
						con = mainapp.consistT;
					} else if (whichThrottle == 'G') {
						con = mainapp.consistG;
					} else {
						con = mainapp.consistS;
					}
					String addr = con.getLeadAddr();
					mainapp.sendMsg(mainapp.comm_msg_handler, message_type.FUNCTION, throt + addr, function, 0);
					// set_function_request(whichThrottle, function, 0);
				}
				break;
			}
		}
	}

	public class throttle_listener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
		char whichThrottle;
		int lastSpeed;
		boolean limitedJump;

		public throttle_listener(char new_whichThrottle) {
			whichThrottle = new_whichThrottle; // store values for this listener
			lastSpeed = 0;
			limitedJump = false;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// Log.d("Engine_Driver", "onTouchThrot action " + event.getAction());
			// consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
			return (gestureInProgress);
		}

		@Override
		public void onProgressChanged(SeekBar throttle, int speed, boolean fromUser) {
			// limit speed change if change was initiated by a user slider touch (prevents "bouncing")
			if (fromUser) {
				if (!limitedJump) { 		// touch generates multiple onProgressChanged events, skip processing after first limited jump
					if ((speed - lastSpeed) > max_throttle_change) { 	// if jump is too large then limit it
						// Log.d("Engine_Driver", "onProgressChanged -- throttling change");
						mAutoIncrement = true;	// advance slowly
						repeatUpdateHandler.post(new RptUpdater(whichThrottle));
						speed = lastSpeed;
						limitedJump = true;
					}
					sendSpeedMsg(whichThrottle, speed);
					setDisplayedSpeed(whichThrottle, speed);
					lastSpeed = speed;
				}
				throttle.setProgress(lastSpeed);
			} else {
				setDisplayedSpeed(whichThrottle, speed);
				lastSpeed = speed;
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar sb) {
			gestureInProgress = false;
			limitedJump = false;
		}

		@Override
		public void onStopTrackingTouch(SeekBar sb) {
			mAutoIncrement = false;
		}
	}

	// send a throttle speed message to WiT
	public void sendSpeedMsg(char whichThrottle, int speed) {
		// start timer to briefly ignore WiT speed messages - avoids speed "jumping"
		if(whichThrottle == 'T') {
			changeTimerT.changeDelay();
		} else if(whichThrottle == 'G') {
			changeTimerG.changeDelay();
		} else {
			changeTimerS.changeDelay();
		}
		// send speed update to WiT
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.VELOCITY, "", whichThrottle, speed);
	}

	// implement delay for briefly ignoring WiT speed reports after sending a throttle speed update
	// this prevents use of speed reports sent by WiT just prior to WiT processing the speed update 
	class ChangeDelay {
		boolean delayInProg;
		Runnable changeTimer;
		char whichThrottle;

		public ChangeDelay(char wThrot) {
			delayInProg = false;
			changeTimer = new ChangeTimer();
			whichThrottle = wThrot;
		}

		public void changeDelay() {
			mainapp.throttle_msg_handler.removeCallbacks(changeTimer);			//remove any pending requests
			delayInProg = true;
			mainapp.throttle_msg_handler.postDelayed(changeTimer, changeDelay);
		}
		
		class ChangeTimer implements Runnable {
	
			public ChangeTimer() {
				delayInProg = false;
			}
			
			@Override
			public void run() {			// change delay is over - clear the delay flag
				delayInProg = false;
			}
		}
	};
	

	/*
	 * private void requestSpeedMsg(char whichThrottle) { // always load
	 * whichThrottle into message mainapp.sendMsg(mainapp.comm_msg_handler,
	 * message_type.REQ_VELOCITY, "", (int) whichThrottle); }
	 */

	// set the title, optionally adding the current time.
	public void setTitle() {
		if (mainapp.displayClock)
			setTitle(getApplicationContext().getResources().getString(R.string.app_name) + "  " + currentTime);
		else
			setTitle(getApplicationContext().getResources().getString(R.string.app_name_throttle));
	}

	public void decrement(char whichThrottle) {
		speedChangeAndNotify(whichThrottle, -BUTTON_SPEED_STEP);
	}

	public void increment(char whichThrottle) {
		speedChangeAndNotify(whichThrottle, BUTTON_SPEED_STEP);
	}

	@SuppressLint({ "Recycle", "SetJavaScriptEnabled" })
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainapp = (threaded_application) this.getApplication();
		prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
		orientationChange = false;

		if (mainapp.isForcingFinish()) { // expedite
			return;
		}
		setContentView(R.layout.throttle);

		webViewLocation = prefs.getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
		// myGesture = new GestureDetector(this);
		GestureOverlayView ov = (GestureOverlayView) findViewById(R.id.throttle_overlay);
		ov.addOnGestureListener(this);
		ov.setGestureVisible(false);
		
		function_button_touch_listener fbtl;
		select_function_button_touch_listener sfbt;
		arrow_speed_button_touch_listener asbl;

		// set listener for select loco buttons
		bSelT = (Button) findViewById(R.id.button_select_loco_T);
		bSelT.setClickable(true);
		sfbt = new select_function_button_touch_listener('T');
		bSelT.setOnClickListener(sfbt);

		bSelS = (Button) findViewById(R.id.button_select_loco_S);
		bSelS.setClickable(true);
		sfbt = new select_function_button_touch_listener('S');
		bSelS.setOnClickListener(sfbt);

		bSelG = (Button) findViewById(R.id.button_select_loco_G);
		bSelG.setClickable(true);
		sfbt = new select_function_button_touch_listener('G');
		bSelG.setOnClickListener(sfbt);

		// Arrow Keys
		try {
			// Throttle T speed buttons.
			bRSpdT = (Button) findViewById(R.id.Right_speed_button_T);
			bRSpdT.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('T', "right");
			bRSpdT.setOnLongClickListener(asbl);
			bRSpdT.setOnTouchListener(asbl);
			bRSpdT.setOnClickListener(asbl);

			bLSpdT = (Button) findViewById(R.id.Left_speed_button_T);
			bLSpdT.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('T', "left");
			bLSpdT.setOnLongClickListener(asbl);
			bLSpdT.setOnTouchListener(asbl);
			bLSpdT.setOnClickListener(asbl);

			// Throttle S speed buttons
			bRSpdS = (Button) findViewById(R.id.Right_speed_button_S);
			bRSpdS.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('S', "right");
			bRSpdS.setOnLongClickListener(asbl);
			bRSpdS.setOnTouchListener(asbl);
			bRSpdS.setOnClickListener(asbl);

			bLSpdS = (Button) findViewById(R.id.Left_speed_button_S);
			bLSpdS.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('S', "left");
			bLSpdS.setOnLongClickListener(asbl);
			bLSpdS.setOnTouchListener(asbl);
			bLSpdS.setOnClickListener(asbl);

			// Throttle G speed buttons.
			bRSpdG = (Button) findViewById(R.id.Right_speed_button_G);
			bRSpdG.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('G', "right");
			bRSpdG.setOnLongClickListener(asbl);
			bRSpdG.setOnTouchListener(asbl);
			bRSpdG.setOnClickListener(asbl);

			bLSpdG = (Button) findViewById(R.id.Left_speed_button_G);
			bLSpdG.setClickable(true);
			asbl = new arrow_speed_button_touch_listener('G', "left");
			bLSpdG.setOnLongClickListener(asbl);
			bLSpdG.setOnTouchListener(asbl);
			bLSpdG.setOnClickListener(asbl);
		} catch (Exception ex) {
			Log.d("debug", "onCreate: " + ex.getMessage());
		}

		// set listeners for 3 direction buttons for each throttle
		bFwdT = (Button) findViewById(R.id.button_fwd_T);
		fbtl = new function_button_touch_listener(function_button.FORWARD, 'T');
		bFwdT.setOnTouchListener(fbtl);
		bStopT = (Button) findViewById(R.id.button_stop_T);
		fbtl = new function_button_touch_listener(function_button.STOP, 'T');
		bStopT.setOnTouchListener(fbtl);
		bRevT = (Button) findViewById(R.id.button_rev_T);
		fbtl = new function_button_touch_listener(function_button.REVERSE, 'T');
		bRevT.setOnTouchListener(fbtl);
		View v = findViewById(R.id.speed_cell_T);
		fbtl = new function_button_touch_listener(function_button.SPEED_LABEL, 'T');
		v.setOnTouchListener(fbtl);

		bFwdS = (Button) findViewById(R.id.button_fwd_S);
		fbtl = new function_button_touch_listener(function_button.FORWARD, 'S');
		bFwdS.setOnTouchListener(fbtl);
		bStopS = (Button) findViewById(R.id.button_stop_S);
		fbtl = new function_button_touch_listener(function_button.STOP, 'S');
		bStopS.setOnTouchListener(fbtl);
		bRevS = (Button) findViewById(R.id.button_rev_S);
		fbtl = new function_button_touch_listener(function_button.REVERSE, 'S');
		bRevS.setOnTouchListener(fbtl);
		v = findViewById(R.id.speed_cell_S);
		fbtl = new function_button_touch_listener(function_button.SPEED_LABEL, 'S');
		v.setOnTouchListener(fbtl);

		bFwdG = (Button) findViewById(R.id.button_fwd_G);
		fbtl = new function_button_touch_listener(function_button.FORWARD, 'G');
		bFwdG.setOnTouchListener(fbtl);
		bStopG = (Button) findViewById(R.id.button_stop_G);
		fbtl = new function_button_touch_listener(function_button.STOP, 'G');
		bStopG.setOnTouchListener(fbtl);
		bRevG = (Button) findViewById(R.id.button_rev_G);
		fbtl = new function_button_touch_listener(function_button.REVERSE, 'G');
		bRevG.setOnTouchListener(fbtl);
		v = findViewById(R.id.speed_cell_G);
		fbtl = new function_button_touch_listener(function_button.SPEED_LABEL, 'G');
		v.setOnTouchListener(fbtl);

		// set up listeners for all throttles
		throttle_listener thl;
		sbT = (SeekBar) findViewById(R.id.speed_T);
		thl = new throttle_listener('T');
		sbT.setOnSeekBarChangeListener(thl);
		sbT.setOnTouchListener(thl);

		sbS = (SeekBar) findViewById(R.id.speed_S);
		thl = new throttle_listener('S');
		sbS.setOnSeekBarChangeListener(thl);
		sbS.setOnTouchListener(thl);

		sbG = (SeekBar) findViewById(R.id.speed_G);
		thl = new throttle_listener('G');
		sbG.setOnSeekBarChangeListener(thl);
		sbG.setOnTouchListener(thl);

		max_throttle_change = 1;
		displaySpeedSteps = false;

		// throttle layouts
		vThrotScr = findViewById(R.id.throttle_screen);
		vThrotScrWrap = findViewById(R.id.throttle_screen_wrapper);
		llT = (LinearLayout) findViewById(R.id.throttle_T);
		llG = (LinearLayout) findViewById(R.id.throttle_G);
		llS = (LinearLayout) findViewById(R.id.throttle_S);
		llTSetSpd = (LinearLayout) findViewById(R.id.Throttle_T_SetSpeed);

		// volume indicators
		vVolT = findViewById(R.id.volume_indicator_T);
		vVolS = findViewById(R.id.volume_indicator_S);
		vVolG = findViewById(R.id.volume_indicator_G);

		// set_default_function_labels();
		tvSpdLabT = (TextView) findViewById(R.id.speed_label_T);
		tvSpdValT = (TextView) findViewById(R.id.speed_value_label_T);
		tvSpdLabG = (TextView) findViewById(R.id.speed_label_G);
		tvSpdValG = (TextView) findViewById(R.id.speed_value_label_G);
		tvSpdLabS = (TextView) findViewById(R.id.speed_label_S);
		tvSpdValS = (TextView) findViewById(R.id.speed_value_label_S);

		fbT = (ViewGroup) findViewById(R.id.function_buttons_table_T);
		fbS = (ViewGroup) findViewById(R.id.function_buttons_table_S);
		fbG = (ViewGroup) findViewById(R.id.function_buttons_table_G);
		// loop through all function buttons and
		// set label and dcc functions (based on settings) or hide if no label
		set_function_labels_and_listeners_for_view('T');
		set_function_labels_and_listeners_for_view('S');
		set_function_labels_and_listeners_for_view('G');

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}

		webView = (WebView) findViewById(R.id.throttle_webview);
		String databasePath = webView.getContext().getDir("databases", Context.MODE_PRIVATE).getPath();
		webView.getSettings().setDatabasePath(databasePath);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setBuiltInZoomControls(true); // Enable Multitouch
															// if supported
		webView.getSettings().setUseWideViewPort(true); // Enable greater
														// zoom-out
		webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
		webView.setInitialScale((int) (100 * scale));
		// webView.getSettings().setLoadWithOverviewMode(true); // size image to
		// fill width

		// open all links inside the current view (don't start external web
		// browser)
		WebViewClient EDWebClient = new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return false;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				if (!noUrl.equals(url)) {
					if (clearHistory) {
						view.clearHistory();
						clearHistory = false;
					}
					if (currentUrlUpdate)
						currentUrl = url;
				} else
					clearHistory = true;
			}
		};

		webView.setWebViewClient(EDWebClient);
		currentUrlUpdate = true; // ok to update currentUrl
		if (currentUrl == null || savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
			load_webview(); // reload if no saved state or no page had loaded
							// when state was saved
		}
		// put pointer to this activity's handler in main app's shared variable
		mainapp.throttle_msg_handler = new throttle_handler();

		// set throttle change delay timers
		changeTimerT = new ChangeDelay('T');
		changeTimerG = new ChangeDelay('G');
		changeTimerS = new ChangeDelay('S');
		
	} // end of onCreate()

	@Override
	public void onResume() {
		super.onResume();
		if (mainapp.isForcingFinish()) { // expedite
			this.finish();
			return;
		}
		if (!mainapp.setActivityOrientation(this)) // set screen orientation based on prefs
		{
			Intent in = new Intent().setClass(this, web_activity.class); // if autoWeb and landscape, switch to Web activity
			navigatingAway = true;
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			return;
		}
		navigatingAway = false;
		mainapp.removeNotification();
		currentTime = "";
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CURRENT_TIME); // request time update

		// format the screen area
		enable_disable_buttons('T');
		enable_disable_buttons('S');
		enable_disable_buttons('G');
		gestureFailed = false;
		gestureInProgress = false;

		set_labels(); // handle labels and update view

		if (webView != null) {
			if (!callHiddenWebViewOnResume())
				webView.resumeTimers();
		}

		if (mainapp.EStopActivated) {
			speedUpdateAndNotify('T', 0);
			speedUpdateAndNotify('S', 0);
			speedUpdateAndNotify('G', 0);
			mainapp.EStopActivated = false;
		}

		if (TMenu != null) {
			TMenu.findItem(R.id.EditConsistT_menu).setVisible(mainapp.consistT.isMulti());
			TMenu.findItem(R.id.EditConsistS_menu).setVisible(mainapp.consistS.isMulti());
			TMenu.findItem(R.id.EditConsistG_menu).setVisible(mainapp.consistG.isMulti());
		}

		CookieSyncManager.getInstance().startSync();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		webView.saveState(outState); // save history (on rotation) if at least
										// one page has loaded
		orientationChange = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (webView != null) {
			if (!callHiddenWebViewOnPause())
				webView.pauseTimers();
		}
		CookieSyncManager.getInstance().stopSync();

		if (!this.isFinishing() && !navigatingAway) { // only invoke
														// setContentIntentNotification
														// when going into
														// background
			mainapp.addNotification(this.getIntent());
		}
	}

	/** Called when the activity is finished. */
	@SuppressWarnings("deprecation")
	@Override
	public void onDestroy() {
		Log.d("Engine_Driver", "throttle.onDestroy()");
		// prefs.edit().putInt("T", 0).commit();
		// prefs.edit().putInt("S", 0).commit();
		// prefs.edit().putInt("G", 0).commit();
		// prefs.edit().putBoolean("r", false).commit();
		if (webView != null) {
			scale = webView.getScale(); // save current scale for next onCreate
			if (!orientationChange) { // if screen is exiting
				webView.loadUrl(noUrl); // load a static url else any javascript
										// on current page would keep running
			}
		}
		repeatUpdateHandler = null;
		mainapp.throttle_msg_handler = null;

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

	// load the url
	private void load_webview() {
		String url = currentUrl;
		if (url == null)
			url = mainapp.createUrl(prefs.getString("InitialThrotWebPage",
					getApplicationContext().getResources().getString(R.string.prefInitialThrotWebPageDefaultValue)));

		if (url == null || webViewLocation.equals("none")) // if port is invalid
															// or not displaying
															// webview
			url = noUrl;
		// webView.clearCache(true);
		webView.loadUrl(url);
	}

	// helper function to set up function buttons for each throttle
	void set_function_labels_and_listeners_for_view(char whichThrottle) {
		// Log.d("Engine_Driver","starting set_function_labels_and_listeners_for_view");

		ViewGroup tv; // group
		ViewGroup r; // row
		function_button_touch_listener fbtl;
		Button b; // button
		int k = 0; // button count
		LinkedHashMap<Integer, String> function_labels_temp = new LinkedHashMap<Integer, String>();
		LinkedHashMap<Integer, Button> functionButtonMap = new LinkedHashMap<Integer, Button>();

		if (whichThrottle == 'T') {
			tv = fbT;
		} else if (whichThrottle == 'G') {
			tv = fbG;
		} else {
			tv = fbS;
		}

		// note: we make a copy of function_labels_x because TA might change it
		// while we are using it (causing issues during button update below)
		if (whichThrottle == 'T' && mainapp.function_labels_T != null && mainapp.function_labels_T.size() > 0) {
			function_labels_temp = new LinkedHashMap<Integer, String>(mainapp.function_labels_T);
		} else if (whichThrottle == 'G' && mainapp.function_labels_G != null && mainapp.function_labels_G.size() > 0) {
			function_labels_temp = new LinkedHashMap<Integer, String>(mainapp.function_labels_G);
		} else if (whichThrottle == 'S' && mainapp.function_labels_S != null && mainapp.function_labels_S.size() > 0) {
			function_labels_temp = new LinkedHashMap<Integer, String>(mainapp.function_labels_S);
		} else {
			function_labels_temp = mainapp.function_labels_default;
		}

		// put values in array for indexing in next step TODO: find direct way
		// to do this
		ArrayList<Integer> aList = new ArrayList<Integer>();
		for (Integer f : function_labels_temp.keySet()) {
			aList.add(f);
		}

		for (int i = 0; i < tv.getChildCount(); i++) {
			r = (ViewGroup) tv.getChildAt(i);
			for (int j = 0; j < r.getChildCount(); j++) {
				b = (Button) r.getChildAt(j);
				if (k < function_labels_temp.size()) {
					Integer func = aList.get(k);
					functionButtonMap.put(func, b); // save function to button
													// mapping
					fbtl = new function_button_touch_listener(func, whichThrottle);
					b.setOnTouchListener(fbtl);
					String bt = function_labels_temp.get(func) + "        "; 	// pad with spaces, and limit to 7 characters
					b.setText(bt.substring(0, 7));
					b.setVisibility(VISIBLE);
					b.setEnabled(false); // start out with everything disabled
				} else {
					b.setVisibility(GONE);
				}
				k++;
			}
		}

		// update the function-to-button map for the current throttle
		if (whichThrottle == 'T')
			functionMapT = functionButtonMap;
		else if (whichThrottle == 'G') {
			functionMapG = functionButtonMap;
		} else
			functionMapS = functionButtonMap;
	}

	// lookup and set values of various informational text labels and size the
	// screen elements
	@SuppressWarnings("deprecation")
	private void set_labels() {
		// Log.d("Engine_Driver","starting set_labels");

		int throttle_count = 0;
		int height_T = 0; // height of first throttle area
		int height_S = 0; // height of second throttle area
		int height_G = 0;// height of third throttle area

		// hide or display volume control indicator based on variable
		if (whichVolume == 'T') {
			vVolT.setVisibility(VISIBLE);
			vVolS.setVisibility(GONE);
			vVolG.setVisibility(GONE);
		} else if (whichVolume == 'G') {
			vVolT.setVisibility(GONE);
			vVolS.setVisibility(GONE);
			vVolG.setVisibility(VISIBLE);
		} else {
			vVolT.setVisibility(GONE);
			vVolS.setVisibility(VISIBLE);
			vVolG.setVisibility(GONE);
		}

		// set speed buttons speed step
		String s = prefs.getString("speed_arrows_throttle_speed_step", "4");
		try {
			BUTTON_SPEED_STEP = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			BUTTON_SPEED_STEP = 4;
		}

		// set up max speeds for throttles
		s = prefs.getString("maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
		int maxThrottle;
		try {
			maxThrottle = Integer.parseInt(s);
			if(maxThrottle > 100) {
				maxThrottle = 100;
			}
		} catch (NumberFormatException e) {
			maxThrottle = 100;
		}
		maxThrottle = (int)Math.round(MAX_SPEED_VAL_WIT * (maxThrottle * .01));	// convert from percent
		sbT.setMax(maxThrottle);
		sbS.setMax(maxThrottle);
		sbG.setMax(maxThrottle);

		// set max allowed change for throttles from prefs
		s = prefs.getString("maximum_throttle_change_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleChangeDefaultValue));
		int maxChange;
		try {
			maxChange = Integer.parseInt(s);		// units are integer percent
		} catch (NumberFormatException e) {
			maxChange = 25;
		}
		max_throttle_change = (int)Math.round(maxThrottle * (maxChange * .01));

		if(mainapp.consistT.isEmpty()) {
			maxSpeedStepT = 100;
		} 
		if(mainapp.consistG.isEmpty()) {
			maxSpeedStepG = 100;
		}
		if(mainapp.consistS.isEmpty()) {
			maxSpeedStepS = 100;
		}
		s = prefs.getString("DisplaySpeedUnits", getApplicationContext().getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));
		displaySpeedSteps = ("Speed Steps".equals(s));
		setDisplayUnitScale('T');
		setDisplayUnitScale('G');
		setDisplayUnitScale('S');
		
		setDisplayedSpeed('T', sbT.getProgress());	// update numeric speeds since units might have changed
		setDisplayedSpeed('G', sbG.getProgress());
		setDisplayedSpeed('S', sbS.getProgress());
		
		// increase height of throttle slider (if requested in preferences)
		boolean ish = prefs.getBoolean("increase_slider_height_preference", getResources().getBoolean(R.bool.prefIncreaseSliderHeightDefaultValue));

		final DisplayMetrics dm = getResources().getDisplayMetrics();
		// Get the screen's density scale

		final float denScale = dm.density;
		// Convert the dps to pixels, based on density scale
		int newHeight;
		if (ish) {
			newHeight = (int) (80 * denScale + 0.5f); // increased height
		} else {
			newHeight = (int) (50 * denScale + 0.5f); // normal height
		}

		final int conNomTextSize = 24;
		final double minTextScale = 0.5;
		String bLabel;
		Button b = bSelT;
		if (mainapp.consistT.isActive()) {
			bLabel = mainapp.consistT.toString();
			throttle_count++;
		} else {
			bLabel = "Press to select";
			// whichVolume = 'S'; //set the next throttle to use volume control
		}
		double textScale = 1.0;
		int bWidth = b.getWidth(); // scale text if required to fit the textView
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
		double textWidth = b.getPaint().measureText(bLabel);
		if (bWidth == 0)
			selectLocoRendered = false;
		else {
			selectLocoRendered = true;
			if (textWidth > 0 && textWidth > bWidth) {
				textScale = bWidth / textWidth;
				if (textScale < minTextScale)
					textScale = minTextScale;
			}
		}
		int textSize = (int) (conNomTextSize * textScale);
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		b.setText(bLabel);
		b.setSelected(false);
		b.setPressed(false);

		b = bSelS;
		if (mainapp.consistS.isActive()) {
			bLabel = mainapp.consistS.toString();
			throttle_count++;
		} else {
			bLabel = "Press to select";
		}
		textScale = 1.0;
		bWidth = b.getWidth(); // scale text if required to fit the textView
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
		textWidth = b.getPaint().measureText(bLabel);
		if (bWidth != 0 && textWidth > 0 && textWidth > bWidth) {
			textScale = bWidth / textWidth;
			if (textScale < minTextScale)
				textScale = minTextScale;
		}
		textSize = (int) (conNomTextSize * textScale);
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		b.setText(bLabel);
		b.setSelected(false);
		b.setPressed(false);

		b = bSelG;
		if (mainapp.consistG.isActive()) {
			bLabel = mainapp.consistG.toString();
			throttle_count++;
		} else {
			bLabel = "Press to select";
		}
		textScale = 1.0;
		bWidth = b.getWidth(); // scale text if required to fit the textView
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
		textWidth = b.getPaint().measureText(bLabel);
		if (bWidth != 0 && textWidth > 0 && textWidth > bWidth) {
			textScale = bWidth / textWidth;
			if (textScale < 0.5)
				textScale = 0.5;
		}
		textSize = (int) (conNomTextSize * textScale);
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
		b.setText(bLabel);
		b.setSelected(false);
		b.setPressed(false);

		int screenHeight = vThrotScrWrap.getHeight(); // get the height of usable area
		int sW = vThrotScrWrap.getWidth();
		if (screenHeight == 0) {
			// throttle screen hasn't been drawn yet, so use display metrics for
			// now
			screenHeight = dm.heightPixels - (int) (titleBar * (dm.densityDpi / 160.)); // allow for title bar, etc
		}
		if (sW == 0) {
			sW = dm.widthPixels;
		}

		// save 1/2 the screen for webview
		if (webViewLocation.equals("Top") || webViewLocation.equals("Bottom")) {
			screenHeight *= 0.5;
		}

		LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, newHeight);

		llTSetSpd.setLayoutParams(llLp);

		if (prefs.getBoolean("display_speed_arrows_buttons", false)) {
			int pL = (int) (sW * 0.10);
			int pS = (int) (sW * 0.80);

			llLp = new LinearLayout.LayoutParams(pL, newHeight);

			bLSpdT.setLayoutParams(llLp);
			bRSpdT.setLayoutParams(llLp);
			bLSpdS.setLayoutParams(llLp);
			bRSpdS.setLayoutParams(llLp);
			bLSpdG.setLayoutParams(llLp);
			bRSpdG.setLayoutParams(llLp);

			llLp = new LinearLayout.LayoutParams(pS, newHeight);

			sbT.setLayoutParams(llLp);
			sbS.setLayoutParams(llLp);
			sbG.setLayoutParams(llLp);
		} else {
			llLp = new LinearLayout.LayoutParams(0, 0);

			bLSpdT.setLayoutParams(llLp);
			bRSpdT.setLayoutParams(llLp);
			bLSpdS.setLayoutParams(llLp);
			bRSpdS.setLayoutParams(llLp);
			bLSpdG.setLayoutParams(llLp);
			bRSpdG.setLayoutParams(llLp);

			llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, newHeight);

			sbT.setLayoutParams(llLp);
			sbS.setLayoutParams(llLp);
			sbG.setLayoutParams(llLp);
		}

		// TODO: Fix graphic error when updating view, so blue dot and line
		// match the new width when sliding.
		if (mainapp.firstCreate) {

			String leftS = prefs.getString("left_slider_margin", getApplicationContext().getResources().getString(R.string.prefSliderLeftMarginDefaultValue));
			String rightS = prefs
					.getString("right_slider_margin", getApplicationContext().getResources().getString(R.string.prefSliderRightMarginDefaultValue));

			int left;
			int right;

			try {
				left = Integer.parseInt(leftS);
			} catch (Exception e) {
				left = 8;
			}
			try {
				right = Integer.parseInt(rightS);
			} catch (Exception e) {
				right = 8;
			}

			sbS.setPadding(left, 0, right, 0);
			sbG.setPadding(left, 0, right, 0);
			sbT.setPadding(left, 0, right, 0);

			sbT.setLayoutParams(llLp);
			sbS.setLayoutParams(llLp);
			sbG.setLayoutParams(llLp);

			mainapp.firstCreate = false;
		}

		if (screenHeight > throttleMargin) { // don't do this if height is invalid
			// determine how to split the screen (evenly if all three, 45/45/10 for two, 80/10/10 if only one)
			screenHeight -= throttleMargin;
			String numThrot = prefs.getString("NumThrottle", getResources().getString(R.string.prefNumOfThrottlesDefault));

			// don't allow third throttle if not supported in JMRI (prior to multithrottle change)
			if (mainapp.withrottle_version < 2.0 && numThrot.matches("Three")) {
				numThrot = "Two";
			}

			if (numThrot.matches("One")) {
				height_T = screenHeight;
				height_S = 0;
				height_G = 0;
			} else if (numThrot.matches("Two") && !mainapp.consistS.isActive()) {
				height_T = (int) (screenHeight * 0.9);
				height_S = (int) (screenHeight * 0.10);
				height_G = 0;
			} else if (numThrot.matches("Two") && !mainapp.consistT.isActive()) {
				height_T = (int) (screenHeight * 0.10);
				height_S = (int) (screenHeight * 0.9);
				height_G = 0;
			} else if (numThrot.matches("Two")) {
				height_T = (int) (screenHeight * 0.5);
				height_S = (int) (screenHeight * 0.5);
				height_G = 0;
			} else if (throttle_count == 0 || throttle_count == 3) {
				height_T = (int) (screenHeight * 0.33);
				height_S = (int) (screenHeight * 0.33);
				height_G = (int) (screenHeight * 0.33);
			} else if (!mainapp.consistT.isActive() && !mainapp.consistS.isActive()) {
				height_T = (int) (screenHeight * 0.10);
				height_S = (int) (screenHeight * 0.10);
				height_G = (int) (screenHeight * 0.80);
			} else if (!mainapp.consistT.isActive() && !mainapp.consistG.isActive()) {
				height_T = (int) (screenHeight * 0.10);
				height_S = (int) (screenHeight * 0.80);
				height_G = (int) (screenHeight * 0.10);
			} else if (!mainapp.consistS.isActive() && !mainapp.consistG.isActive()) {
				height_T = (int) (screenHeight * 0.80);
				height_S = (int) (screenHeight * 0.10);
				height_G = (int) (screenHeight * 0.10);
			} else if (!mainapp.consistT.isActive()) {
				height_T = (int) (screenHeight * 0.10);
				height_S = (int) (screenHeight * 0.45);
				height_G = (int) (screenHeight * 0.45);
			} else if (!mainapp.consistS.isActive()) {
				height_T = (int) (screenHeight * 0.45);
				height_S = (int) (screenHeight * 0.10);
				height_G = (int) (screenHeight * 0.45);
			} else {
				height_T = (int) (screenHeight * 0.45);
				height_S = (int) (screenHeight * 0.45);
				height_G = (int) (screenHeight * 0.10);
			}

			// set height of T area
			llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, height_T);
			llLp.bottomMargin = (int) (throttleMargin * (dm.densityDpi / 160.));
			llT.setLayoutParams(llLp);

			// update throttle top/bottom
			T_top = llT.getTop() + sbT.getTop();
			T_bottom = llT.getTop() + sbT.getBottom() + bSelT.getHeight() + bFwdT.getHeight();

			// set height of S area
			llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, height_S);
			llS.setLayoutParams(llLp);

			// update throttle top/bottom
			S_top = llS.getTop() + sbS.getTop();
			S_bottom = llS.getTop() + sbS.getBottom() + bSelS.getHeight() + bFwdS.getHeight();

			// set height of G area
			llLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, height_G);
			llG.setLayoutParams(llLp);

			// update throttle top/bottom
			G_top = llG.getTop() + sbS.getTop();
			G_bottom = llG.getTop() + sbS.getBottom() + bSelG.getHeight() + bFwdG.getHeight();
		}

		// update the direction indicators
		showDirectionIndication('T', dirT);
		showDirectionIndication('S', dirS);
		showDirectionIndication('G', dirG);

		// update the state of each function button based on shared variable
		set_all_function_states('T');
		set_all_function_states('S');
		set_all_function_states('G');

		if (TMenu != null) {
			mainapp.displayEStop(TMenu);
			mainapp.displayPowerStateMenuButton(TMenu);
		}
		vThrotScrWrap.invalidate();
		// Log.d("Engine_Driver","ending set_labels");
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		// Handle pressing of the back button to release the selected loco and end this activity
		if (key == KeyEvent.KEYCODE_BACK) {
			if (webView.canGoBack() && !clearHistory) {
				scale = webView.getScale(); // save scale
				webView.goBack();
				webView.setInitialScale((int) (100 * scale)); // restore scale
			} else
				mainapp.checkExit(this);
			return (true); // stop processing this key
		} else if ((key == KeyEvent.KEYCODE_VOLUME_UP) || (key == KeyEvent.KEYCODE_VOLUME_DOWN)) { 	// use volume to change speed for specified loco
			char wVol = 0;
			if (whichVolume == 'T' && mainapp.consistT.isActive()) {
				wVol = 'T';
			}
			if (whichVolume == 'S' && mainapp.consistS.isActive()) {
				wVol = 'S';
			}
			if (whichVolume == 'G' && mainapp.consistG.isActive()) {
				wVol = 'G';
			}
			if (wVol != 0) {
				if (key == KeyEvent.KEYCODE_VOLUME_UP) {
					speedChangeAndNotify(wVol, 1);
				} else {
					speedChangeAndNotify(wVol, -1);
				}
			}
			return (true); // stop processing this key
		}
		return (super.onKeyDown(key, event)); // continue with normal key
												// processing
	}

	private void disconnect() {
		// release the locos
		mainapp.consistT.release();
		mainapp.consistS.release();
		mainapp.consistG.release();
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", 'T'); 	// release first loco
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", 'S'); 	// release second loco
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", 'G'); 	// release third loco

		webView.stopLoading();
		this.finish(); // end this activity
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
			in = new Intent().setClass(this, turnouts.class);
			navigatingAway = true;
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			break;
		case R.id.routes_mnu:
			in = new Intent().setClass(this, routes.class);
			navigatingAway = true;
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.web_mnu:
			in = new Intent().setClass(this, web_activity.class);
			navigatingAway = true;
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.exit_mnu:
			navigatingAway = true;
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
		case R.id.settings_mnu:
			in = new Intent().setClass(this, function_settings.class);
			navigatingAway = true;
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.about_mnu:
			in = new Intent().setClass(this, about_page.class);
			navigatingAway = true;
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.logviewer_menu:
			Intent logviewer = new Intent().setClass(this, LogViewerActivity.class);
			navigatingAway = true;
			startActivity(logviewer);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EmerStop:
			speedUpdateAndNotify('T', 0);
			speedUpdateAndNotify('S', 0);
			speedUpdateAndNotify('G', 0);
			break;
		case R.id.power_layout_button:
			if (mainapp.power_state == null) {
				AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setIcon(android.R.drawable.ic_dialog_alert);
				b.setTitle("Will Not Work!");
				b.setMessage("JMRI has the wiThrottle power control setting to off.\n\nWill now remove Power Icon.\n\nWill display again when JMRI setting is on.");
				b.setCancelable(true);
				b.setNegativeButton("OK", null);
				AlertDialog alert = b.create();
				alert.show();
				mainapp.displayPowerStateMenuButton(TMenu);
			} else {
				mainapp.powerStateMenuButton();
			}
			break;
		case R.id.EditConsistT_menu:
			Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);
			consistEdit.putExtra("whichThrottle", 'T');
			navigatingAway = true;
			startActivityForResult(consistEdit, 1);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EditConsistS_menu:
			Intent consistEdit2 = new Intent().setClass(this, ConsistEdit.class);
			consistEdit2.putExtra("whichThrottle", 'S');
			navigatingAway = true;
			startActivityForResult(consistEdit2, 1);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EditConsistG_menu:
			Intent consistEdit3 = new Intent().setClass(this, ConsistEdit.class);
			consistEdit3.putExtra("whichThrottle", 'G');
			navigatingAway = true;
			startActivityForResult(consistEdit3, 1);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// handle return from menu items
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) { // edit loco or edit consist
			if (resultCode == RESULT_FIRST_USER) { // something about consist
													// was changed
				Bundle extras = data.getExtras();
				if (extras != null) {
					char whichThrottle = extras.getChar("whichThrottle");
					int dir;
					int speed;
					if (whichThrottle == 'T') {
						dir = dirT;
						speed = sbT.getProgress(); 
					} else if (whichThrottle == 'G') {
						dir = dirG;
						speed = sbG.getProgress(); 
					} else {
						dir = dirS;
						speed = sbS.getProgress(); 
					}
					setEngineDirection(whichThrottle, dir, false); 	// update direction for each loco in consist
					sendSpeedMsg(whichThrottle, speed);				// ensure all trailing units have the same speed as the lead engine
				}
				// update loco name
			}
		}
		// loop through all function buttons and
		// set label and dcc functions (based on settings) or hide if no label
		set_function_labels_and_listeners_for_view('T');
		set_function_labels_and_listeners_for_view('S');
		set_function_labels_and_listeners_for_view('G');
		set_labels();
	}

	// touch events outside the GestureOverlayView get caught here
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Log.d("Engine_Driver", "onTouch Title action " + event.getAction());
		switch (event.getAction()) {
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
		// Log.d("Engine_Driver", "gestureStart y=" + gestureStartY);

		// if gesture is attempting to start over an enabled slider, ignore it and return immediately.
		if ((((View)sbT).isEnabled() && gestureStartY >= T_top && gestureStartY <= T_bottom)
				|| (((View)sbS).isEnabled() && gestureStartY >= S_top && gestureStartY <= S_bottom)
				|| (((View)sbG).isEnabled() && gestureStartY >= G_top && gestureStartY <= G_bottom)) {
			// Log.d("Engine_Driver","exiting gestureStart");
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
		// Log.d("Engine_Driver", "gestureMove action " + event.getAction());
		if (gestureInProgress == true) {
			// stop the gesture timeout timer
			mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);

			mVelocityTracker.addMovement(event);
			if ((event.getEventTime() - gestureLastCheckTime) > gestureCheckRate) {
				// monitor velocity and fail gesture if it is too low
				gestureLastCheckTime = event.getEventTime();
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000);
				int velocityX = (int) velocityTracker.getXVelocity();
				// Log.d("Engine_Driver", "gestureVelocity vel " + velocityX);
				if (Math.abs(velocityX) < threaded_application.min_fling_velocity) {
					gestureFailed(event);
				}
			}
			if (gestureInProgress == true) {
				// restart the gesture timeout timer
				mainapp.throttle_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
			}
		}
	}

	private void gestureEnd(MotionEvent event) {
		// Log.d("Engine_Driver", "gestureEnd action " + event.getAction());
		mainapp.throttle_msg_handler.removeCallbacks(gestureStopped);
		if (gestureInProgress == true) {
			if (Math.abs(event.getX() - gestureStartX) > threaded_application.min_fling_distance) {
				// valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
				event.setAction(MotionEvent.ACTION_CANCEL);
				navigatingAway = true;
				// left to right swipe goes to turnouts
				if (event.getRawX() > gestureStartX) {
					Intent in = new Intent().setClass(this, turnouts.class);
					startActivity(in);
					connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
				}
				// right to left swipe goes to routes
				else {
					Intent in = new Intent().setClass(this, routes.class);
					startActivity(in);
					connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
				}
			} else {
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
		// end the gesture
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
			if (gestureInProgress == true) {
				// end the gesture
				gestureInProgress = false;
				gestureFailed = true;
				// create a MOVE event to trigger the underlying control
				if (vThrotScr != null) {
					// use uptimeMillis() rather than 0 for time in
					// MotionEvent.obtain() call in throttle gestureStopped:
					MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, gestureStartX,
							gestureStartY, 0);
					try {
						vThrotScr.dispatchTouchEvent(event);
					} catch (IllegalArgumentException e) {
						Log.d("Engine_Driver", "gestureStopped trigger IllegalArgumentException, OS " + android.os.Build.VERSION.SDK_INT);
					}
				}
			}
		}
	};

	// helper app to initialize statics (in case GC has not run since app last
	// shutdown)
	// call before instantiating any instances of class
	public static void initStatics() {
		scale = initialScale;
		clearHistory = false;
		currentUrl = null;
	}
}
