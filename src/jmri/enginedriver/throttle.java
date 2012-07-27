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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jmri.jmrit.roster.RosterLoader;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
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
	private static final int GONE = 8;
	private static final int VISIBLE = 0;
	private static final int throttleMargin = 8;	// margin between the throttles in dp
	private static final int titleBar = 45;			// estimate of lost screen height in dp
	private static SharedPreferences prefs;
	private static final int MAX_SPEED_DISPLAY = 99;	// value to display at maximum speed setting 
	private static int MAX_SPEED_VAL_T = 126;			// maximum DCC speed setting in current mode
	private static int MAX_SPEED_VAL_S = 126;			// maximum DCC speed setting in current mode
	// speed scale factors
	private static double SPEED_TO_DISPLAY_T = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL_T);
	private static double SPEED_TO_DISPLAY_S = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL_S);
	
//	private static final long speedUpdateDelay = 500;	// idle time in milliseconds after speed change before requesting speed update 

	private char whichVolume = 'T';

	private boolean slider_moved_by_server = false;			// true if the slider was moved due to a processed response

	int max_throttle_change = 99;  //maximum allowable change of the sliders, set in preferences
	
	//screen coordinates for throttle sliders, so we can ignore swipe on them
	int T_top;  
	int T_bottom;  
	int S_top;  
	int S_bottom;  
	
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
	
	private DownloadRosterTask dlRosterTask;
	private DownloadMetadataTask dlMetadataTask;

	//current direction
	private int dirT = 0;
	private int dirS = 0;

  //Handle messages from the communication thread TO this thread (responses from withrottle)
  class throttle_handler extends Handler {

	  public void handleMessage(Message msg) {
		  switch(msg.what) {
		  case message_type.RESPONSE: {  //handle messages from WiThrottle server
			  String response_str = msg.obj.toString();
			  char com1 = response_str.charAt(0);
			  char thrSel = response_str.charAt(1);
			  switch (com1) {
			  //various MultiThrottle responses
			  case 'M':
				  if(thrSel == 'T' || thrSel == 'S') {

					  char com2 = response_str.charAt(2);
					  if (com2 == '+' || com2 == 'L') {  //if loco added or function labels updated
						  if(com2 == ('+')) {
							  set_default_function_labels();
							  enable_disable_buttons(thrSel);  //direction and slider: pass whichthrottle
						  }
						  // loop through all function buttons and
						  //   set label and dcc functions (based on settings) or hide if no label
						  ViewGroup tv;
						  if (thrSel == 'T') {
							  tv = (ViewGroup) findViewById(R.id.function_buttons_table_T);
						  } else {
							  tv = (ViewGroup) findViewById(R.id.function_buttons_table_S);
						  }
						  set_function_labels_and_listeners_for_view(thrSel);
						  enable_disable_buttons_for_view(tv, true);
						  set_labels();
					  } 
					  else if (com2 == '-') {  		//if loco removed
						  ViewGroup tv;
						  if (thrSel == 'T') {
							  tv = (ViewGroup) findViewById(R.id.function_buttons_table_T);
						  } else {
							  tv = (ViewGroup) findViewById(R.id.function_buttons_table_S);
						  }
						  enable_disable_buttons(thrSel);  //direction and slider
						  set_function_labels_and_listeners_for_view(thrSel);
						  enable_disable_buttons_for_view(tv, false);
						  set_labels();
					  } 
					  else if (com2 == 'A') {	  		//e.g. MTAL2608<;>R1
						  String[] ls = threaded_application.splitByString(response_str,"<;>");
						  char com3 = ls[1].charAt(0);
						  if (com3 == 'R') {
							  try {
								  int dir = new Integer(ls[1].substring(1,2));
								  set_direction_indication(thrSel, dir); //set direction button 
							  }
							  catch(Exception e) {
							  }
						  } 
						  else if (com3 == 'V') {
							  try {
								  int speed = Integer.parseInt(ls[1].substring(1));
								  slider_moved_by_server = true;
								  set_speed_slider(thrSel, speed);	//update speed slider and indicator
								  slider_moved_by_server = false;
							  }
							  catch(Exception e) {
							  }
						  }
						  else if (com3 == 'F') {
							  try {
								  int function = new Integer(ls[1].substring(2));
								  set_function_state(thrSel, function);
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
				  enable_disable_buttons(com1);  //pass whichthrottle
				  set_labels();
				  break;

			  case 'R': 
				  if(thrSel == 'F' || thrSel == 'S') { //roster function labels - legacy
					  ViewGroup tv;
					  if (thrSel == 'F') {			// used to use 'F' instead of 'T'
						  tv = (ViewGroup) findViewById(R.id.function_buttons_table_T);
						  thrSel = 'T';
					  } else {
						  tv = (ViewGroup) findViewById(R.id.function_buttons_table_S);
					  }
					  set_function_labels_and_listeners_for_view(thrSel);
					  enable_disable_buttons_for_view(tv, true);
					  set_labels();

				  } else if (thrSel == 'L') { //roster list received, request details from web server
					  //Log.d("Engine_Driver", "got updated roster list, requesting details");
					  dlRosterTask = new DownloadRosterTask();
					  dlRosterTask.getRoster();
					  
				  } else {
					  try {
						  String scom2 = response_str.substring(1,6);
						  if(scom2.equals("PF}|{"))
						  {
							  thrSel = response_str.charAt(6);
							  set_all_function_states(thrSel);
						  }
					  }
					  catch(IndexOutOfBoundsException e){
					  }
				  }
				  break;
			  case 'P': //panel info
				  if (thrSel == 'W') {		// PW - web server port info
					  // try web-dependent items again
					  dlMetadataTask.getMetadata();
					  dlRosterTask.getRoster();
				  }
			  }  //end of switch
		  }
		  break;
		  case message_type.DISCONNECT:
			  disconnect();
			  break;
		  }
	  };
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
	else {
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
  }
  
  void set_speed_slider(char whichThrottle, int speed) {
	  SeekBar throttle_slider;

	  if (whichThrottle == 'T') {
		  throttle_slider=(SeekBar)findViewById(R.id.speed_T);
	  } else {
		  throttle_slider=(SeekBar)findViewById(R.id.speed_S);
	  }
	  if(speed < 0)
		  speed = 0;
	  
	  throttle_slider.setProgress(speed);
  }

private void setDisplayedSpeed(char whichThrottle, int speed) {
	  	TextView speed_label;
	  	String loco;
	  	double speedScale;
	  	if (whichThrottle == 'T') {
			speedScale = SPEED_TO_DISPLAY_T;
			speed_label=(TextView)findViewById(R.id.speed_value_label_T);
	  		loco = mainapp.loco_string_T;
		} 
		else {
			speedScale = SPEED_TO_DISPLAY_S;
			speed_label=(TextView)findViewById(R.id.speed_value_label_S);
	  		loco = mainapp.loco_string_S;
	  	}
	  	if(speed < 0) {
	  		Toast.makeText(getApplicationContext(), "Alert: Engine " + loco + " is set to ESTOP", Toast.LENGTH_LONG).show();
	  		speed = 0;
	  	}
		int displayedSpeed = (int)Math.round(speed * speedScale);
		speed_label.setText(Integer.toString(displayedSpeed));
  }

 // indicate actual direction
  void set_direction_indication(char whichThrottle, Integer direction) {
	  Button bFwd;
	  Button bRev;
	  if (whichThrottle == 'T') {
		  bFwd = (Button)findViewById(R.id.button_fwd_T);
		  bRev = (Button)findViewById(R.id.button_rev_T);
		  dirT = direction;
	  } else {
		  bFwd = (Button)findViewById(R.id.button_fwd_S);
		  bRev = (Button)findViewById(R.id.button_rev_S);
		  dirS = direction;
	  }
	  if (direction == 0) {
		  bFwd.setPressed(false);
		  bRev.setPressed(true);
	  } else {
		  bFwd.setPressed(true);
		  bRev.setPressed(false);
	  }
  }

// indicate requested direction
  void set_direction_request(char whichThrottle, Integer direction) {
	  Button bFwd;
	  Button bRev;
	  if (whichThrottle == 'T') {
		  bFwd = (Button)findViewById(R.id.button_fwd_T);
		  bRev = (Button)findViewById(R.id.button_rev_T);
	  } else {
		  bFwd = (Button)findViewById(R.id.button_fwd_S);
		  bRev = (Button)findViewById(R.id.button_rev_S);
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
 *  
	  if (mainapp.withrottle_version < 2.0) {
		  // no feedback avail so just let indication follow request
		  set_direction_indication(whichThrottle, direction);
	  }
  	  else {
  			Message msg=Message.obtain();
  			msg.what=message_type.REQ_DIRECTION;
  			msg.obj=new String(Character.toString(whichThrottle));    // always load whichThrottle into message
  			mainapp.comm_msg_handler.sendMessage(msg);
  	}
*
*  for now just track the setting:
*/  	
		  set_direction_indication(whichThrottle, direction);
  }
  
  void set_stop_button(char whichThrottle, boolean pressed) {
	  Button bStop;
	  if (whichThrottle == 'T') {
		  bStop = (Button)findViewById(R.id.button_stop_T);
	  } 
	  else {
		  bStop = (Button)findViewById(R.id.button_stop_S);
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
	else {
		bSel = (Button)findViewById(R.id.button_select_loco_S);
	}
	bSel.setPressed(true);	//give feedback that select button was pressed

	Intent select_loco=new Intent().setClass(this, select_loco.class);
    select_loco.putExtra("whichThrottle", Character.toString(whichThrottle));  //pass whichThrottle as an extra to activity
//    select_loco.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    startActivityForResult(select_loco, 0);
    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
  };

  void enable_disable_buttons(char whichThrottle)  {
      boolean newEnabledState;
//      Log.d("Engine_Driver", "e/d buttons "+mainapp.loco_string_T);
	  if (whichThrottle == 'T') {
		  newEnabledState = !(mainapp.loco_string_T.equals("Not Set"));  //set false if loco is "Not Set"
          findViewById(R.id.button_fwd_T).setEnabled(newEnabledState);
          findViewById(R.id.button_stop_T).setEnabled(newEnabledState);
          findViewById(R.id.button_rev_T).setEnabled(newEnabledState);
          findViewById(R.id.speed_label_T).setEnabled(newEnabledState);
          findViewById(R.id.speed_value_label_T).setEnabled(newEnabledState);
          enable_disable_buttons_for_view((ViewGroup)findViewById(R.id.function_buttons_table_T),newEnabledState);
          SeekBar sb = (SeekBar)findViewById(R.id.speed_T);
          if (!newEnabledState) {
              sb.setProgress(0);  //set slider to 0 if disabled
          }
          sb.setEnabled(newEnabledState);
	  } else {
		  newEnabledState = !(mainapp.loco_string_S.equals("Not Set"));  //set false if loco is "Not Set"
          findViewById(R.id.button_fwd_S).setEnabled(newEnabledState);
          findViewById(R.id.button_stop_S).setEnabled(newEnabledState);
          findViewById(R.id.button_rev_S).setEnabled(newEnabledState);
          findViewById(R.id.speed_label_S).setEnabled(newEnabledState);
          findViewById(R.id.speed_value_label_S).setEnabled(newEnabledState);
          enable_disable_buttons_for_view((ViewGroup)findViewById(R.id.function_buttons_table_S),newEnabledState);
          SeekBar sb = (SeekBar)findViewById(R.id.speed_S);
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
	  else
		  fMap = functionMapS;

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
    	else {
        	b = functionMapS.get(function);
        	fs = mainapp.function_states_S;
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
		else {
	    	b = functionMapS.get(function);
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

  public class function_button_touch_listener implements View.OnTouchListener
  {
    int function;
    char whichThrottle;  //T for first throttle, S for second 

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
      switch(action)
      {
        case MotionEvent.ACTION_DOWN:
        {
          Message function_msg=Message.obtain();  //create a message to be used by many of the buttons below
          function_msg.what = message_type.NONE;

          switch (function) {
            case function_button.FORWARD : {
            	function_msg.what=message_type.DIRECTION;
                function_msg.arg1=1;
            	function_msg.arg2=1;  //forward is 1

            	//show pressed image on current button, and turn off pressed image on "other" button 
            	set_direction_request(whichThrottle, 1);
            }
              break;

            case function_button.REVERSE : {
            	function_msg.what=message_type.DIRECTION;
                function_msg.arg1=1;
            	function_msg.arg2=0;  //reverse is 0
            	//show pressed image on current button, and turn off pressed image on "other" button 
            	set_direction_request(whichThrottle, 0);
            	//            	v.setPressed(true);
              }
              break;

            case function_button.STOP : { 
            	set_stop_button(whichThrottle, true);
            	SeekBar sb;
            	if (whichThrottle == 'T') {
            	  sb=(SeekBar)findViewById(R.id.speed_T);
            	} else {
              	  sb=(SeekBar)findViewById(R.id.speed_S);
            	}
                // changing the throttle slider to 0 sends a velocity change to the withrottle
            	// otherwise explicitly send 0 speed message
            	if(sb.getProgress() != 0)
            		sb.setProgress(0);
            	else
            		sendSpeedMsg(whichThrottle, 0);
            	break;
            }
            // specify which throttle the volume button controls          	  
            case function_button.SPEED_LABEL: { 
            	whichVolume = whichThrottle;  //use whichever was clicked
            	set_labels();
        	  break;
            }
            case function_button.SELECT_LOCO : {
            	start_select_loco_activity(whichThrottle);  //pass throttle #
              }
              break;

            default : {  //handle the function buttons
              function_msg.what=message_type.FUNCTION;
              function_msg.arg1=function;
              function_msg.arg2=1;
//              set_function_request(whichThrottle, function, 1);
            }
          }  //end of function switch
          
          if (function_msg.what != message_type.NONE) {  //don't send if no payload
            function_msg.obj=new String(Character.toString(whichThrottle));    // always load whichThrottle into message
            mainapp.comm_msg_handler.sendMessage(function_msg);            //send the message to comm thread
          }
          else {
        	  function_msg.recycle();
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
        		Message function_msg=Message.obtain();
        		function_msg.what=message_type.FUNCTION;
        		function_msg.arg1=function;
        		function_msg.arg2=0;
        		function_msg.obj=new String(Character.toString(whichThrottle));    // always load whichThrottle into message
        		mainapp.comm_msg_handler.sendMessage(function_msg);
//                set_function_request(whichThrottle, function, 0);
        	}
        	break;
      }
    };
  }

  public class throttle_listener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener
  {
      char whichThrottle;
	  SeekBar speed_slider;
	  int lastSpeed;
      
    public throttle_listener(char new_whichThrottle)    {
	    whichThrottle = new_whichThrottle;   	//store values for this listener
  		if (whichThrottle == 'T') {
			speed_slider=(SeekBar)findViewById(R.id.speed_T);
		} 
		else {
			speed_slider=(SeekBar)findViewById(R.id.speed_S);
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
    	} else {
    		speedScale = SPEED_TO_DISPLAY_S;
    	}
    	if ((speed - lastSpeed) > (max_throttle_change / speedScale)) { 
        	//Log.d("Engine_Driver", "onProgressChanged -- throttling change");
    		speed = lastSpeed + 1;  //advance, but slowly, so user knows something is happening
    		throttle.setProgress(speed);
    	}

    	//send request for new speed to WiT server
		sendSpeedMsg(whichThrottle, speed);

		lastSpeed = speed;

		return;
    }

	@Override
	public void onStartTrackingTouch(SeekBar sb) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar sb) {
	}
    
/*
	//
	// speedUpdate runs when more than speedUpdateDelay milliseconds 
	// elapse between speed changes.
	//
	private Runnable speedUpdate = new Runnable() {
	   @Override
	   public void run() {
		   Log.d("Engine_Driver", "speedUpdate " + whichThrottle);
		  	  if (mainapp.withrottle_version < 2.0) 
		  	  {
				  // no speed feedback avail so just let speed indication follow request  
		  		  setDisplayedSpeed(whichThrottle, speed_slider.getProgress());
		  	  }
		  	  else 
		  	  {
		   
		  		  requestSpeedMsg(whichThrottle);
		  	  }
	   }
	};
*/
  }

/*  private void requestSpeedMsg(char whichThrottle) {
		Message msg=Message.obtain();
		msg.what=message_type.REQ_VELOCITY;
		msg.obj=new String(Character.toString(whichThrottle));    // always load whichThrottle into message
		mainapp.comm_msg_handler.sendMessage(msg);
}
*/

  private void sendSpeedMsg(char whichThrottle, int speed) {
		Message msg=Message.obtain();
		msg.what=message_type.VELOCITY;
		msg.arg1=speed;
		msg.obj=new String(Character.toString(whichThrottle));    // always load whichThrottle into message
		mainapp.comm_msg_handler.sendMessage(msg);
  }

  // set throttle screen orientation based on prefs, check to avoid sending change when already there
  private static void setActivityOrientation(Activity activity) {

	  String to = prefs.getString("ThrottleOrientation", 
			  activity.getApplicationContext().getResources().getString(R.string.prefThrottleOrientationDefaultValue));
	  int co = activity.getRequestedOrientation();
	  if      (to.equals("Landscape")   && (co != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE))  
		  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	  else if (to.equals("Auto-Rotate") && (co != ActivityInfo.SCREEN_ORIENTATION_SENSOR))  
		  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	  else if (to.equals("Portrait")    && (co != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
		  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  @Override
  public void onResume() {
	  super.onResume();

	  setActivityOrientation(this);  //set throttle screen orientation based on prefs

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
	  gestureFailed = false;
	  gestureInProgress = false;

	  setup_webview();

	  set_labels();

  }


@Override
public void onStart() {
  super.onStart();

  //put pointer to this activity's handler in main app's shared variable (If needed)
	  mainapp.throttle_msg_handler=new throttle_handler();
}

/** Called when the activity is finished. */
  @Override
  public void onDestroy() {
	  Log.d("Engine_Driver","throttle.onDestroy() called");

	  //load a bogus url to prevent javascript from continuing to run
	  WebView webView = (WebView) findViewById(R.id.throttle_webview);
	  webView.loadUrl("file:///android_asset/blank_page.html");

	  mainapp.throttle_msg_handler = null;

	  super.onDestroy();
  }


  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.throttle);

    mainapp=(threaded_application)this.getApplication();

    prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
    
//    myGesture = new GestureDetector(this);
    GestureOverlayView ov = (GestureOverlayView)findViewById(R.id.throttle_overlay);
    ov.addOnGestureListener(this);
    ov.setGestureVisible(false);
    
    Button b;
    function_button_touch_listener fbtl;
    
    //set listener for select loco buttons
    b = (Button)findViewById(R.id.button_select_loco_T);
    fbtl=new function_button_touch_listener(function_button.SELECT_LOCO, 'T');
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_select_loco_S);
    fbtl=new function_button_touch_listener(function_button.SELECT_LOCO, 'S');
    b.setOnTouchListener(fbtl);
    
    // set listeners for 3 direction buttons for each throttle
    b = (Button)findViewById(R.id.button_fwd_T);
    fbtl=new function_button_touch_listener(function_button.FORWARD, 'T');
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_stop_T);
    fbtl=new function_button_touch_listener(function_button.STOP,  'T');
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

    // set up listeners for both throttles
    throttle_listener th1;
    SeekBar sb=(SeekBar)findViewById(R.id.speed_T);
    th1 = new throttle_listener('T');
    sb.setOnSeekBarChangeListener(th1);
    sb.setOnTouchListener(th1);
    
    sb=(SeekBar)findViewById(R.id.speed_S);
    th1 = new throttle_listener('S');
    sb.setOnSeekBarChangeListener(th1);
    sb.setOnTouchListener(th1);

    set_default_function_labels();
    // loop through all function buttons and
    //   set label and dcc functions (based on settings) or hide if no label
    set_function_labels_and_listeners_for_view('T');
    set_function_labels_and_listeners_for_view('S');

	if (mVelocityTracker == null) {
		mVelocityTracker = VelocityTracker.obtain();
	}

	dlMetadataTask = new DownloadMetadataTask();
	dlMetadataTask.getMetadata();		// if web port is already known, start background roster dl here

	dlRosterTask = new DownloadRosterTask();
	dlRosterTask.getRoster();		// if web port is already known, start background roster dl here

  } //end of onCreate()

  //set up webview to requested initial page
  @SuppressLint("SetJavaScriptEnabled") private void setup_webview() {

	  //copy webviewlocation from prefs to app var
	  mainapp.webViewLocation = prefs.getString("WebViewLocation", getApplicationContext().getResources().getString(R.string.prefWebViewLocationDefaultValue));
	  WebView webView = (WebView) findViewById(R.id.throttle_webview);

	  if (!mainapp.webViewLocation.equals("none")) {
		  String url = prefs.getString("InitialWebPage", getApplicationContext().getResources().getString(R.string.prefInitialWebPageDefaultValue));
		  if (!url.startsWith("http")) {  //if url starts with http, use it as is, else prepend servername and port
			  url = "http://" + mainapp.host_ip + ":" +  mainapp.web_server_port + "/" + url;
		  }
		  if (webView.getUrl() == null) {
			  if (mainapp.web_server_port != null && mainapp.web_server_port > 0) {
				  webView.getSettings().setJavaScriptEnabled(true);
				  webView.getSettings().setBuiltInZoomControls(true);
				  webView.loadUrl(url);
				  Log.d("Engine_Driver","web view set to " + url);	  

				  // open all links inside the current view (don't start external web browser)
				  WebViewClient EDWebClient = new WebViewClient()	{
					  @Override
					  public boolean shouldOverrideUrlLoading(WebView  view, String  url) {
						  return false;
					  }
				  };
				  webView.setWebViewClient(EDWebClient);
			  }
		  } else {
			  Log.d("Engine_Driver","web view already set");
//			  webView.loadUrl(url);
		  }
	  } else {
		  Log.d("Engine_Driver","web view set to blank");
		  webView.loadUrl("file:///android_asset/blank_page.html");
	  }
  }  

  
  //set up text label and dcc function for each button from settings and setup listeners
  //TODO: move file reading to another function and only do when needed
  private void set_default_function_labels() {

	  mainapp.function_labels_default = new LinkedHashMap<Integer, String>();

	  try	  {
		  File sdcard_path=Environment.getExternalStorageDirectory();

		  File settings_file=new File(sdcard_path + "/engine_driver/function_settings.txt");
		  if(settings_file.exists()) {  //if file found, use it for settings arrays
			  BufferedReader settings_reader=new BufferedReader(new FileReader(settings_file));
			  //read settings into local arrays
			  while(settings_reader.ready()) {
				  String line=settings_reader.readLine();
				  String temp[] = line.split(":");
				  mainapp.function_labels_default.put(Integer.parseInt(temp[1]), temp[0]); //put funcs and labels into global default
			  }
			  settings_reader.close();
		  } else {  //hard-code some buttons and default the rest
			  mainapp.function_labels_default.put(0, "Light");
			  mainapp.function_labels_default.put(1, "Bell");
			  mainapp.function_labels_default.put(2, "Horn");
			  for(int k = 3; k <= 27; k++) {
				  mainapp.function_labels_default.put(k, String.format("%d",k));
			  }
		  }
	  }
	  catch (IOException except) { Log.e("settings_activity", "Could not read file "+except.getMessage()); }  

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
	  } else {
		  tv = (ViewGroup) findViewById(R.id.function_buttons_table_S); //table
	  }
	  
	  if (whichThrottle == 'T' && mainapp.function_labels_T != null && mainapp.function_labels_T.size()>0) {
		  function_labels_temp = mainapp.function_labels_T;  //point temp to T
	  } else if (whichThrottle == 'S' && mainapp.function_labels_S != null  && mainapp.function_labels_S.size()>0) {
		  function_labels_temp = mainapp.function_labels_S;  //point temp to S
	  } else {
		  function_labels_temp = mainapp.function_labels_default;  //point temp to default
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
	  else
		  functionMapS = functionButtonMap;
  }


//lookup and set values of various informational text labels and size the screen elements 
  private void set_labels() {

//	  Log.d("Engine_Driver","starting set_labels");

    int throttle_count = 0;
   	int height_T = 0; //height of first throttle area
	int height_S = 0; //height of second throttle area
  
    // hide or display volume control indicator based on variable
    View viT = findViewById(R.id.volume_indicator_T);
    View viS = findViewById(R.id.volume_indicator_S);
    if (whichVolume == 'T') {
    	viT.setVisibility(VISIBLE);
    	viS.setVisibility(GONE);
    } else {
    	viT.setVisibility(GONE);
    	viS.setVisibility(VISIBLE);
    }
    
    SeekBar sbT=(SeekBar)findViewById(R.id.speed_T);
    SeekBar sbS=(SeekBar)findViewById(R.id.speed_S);
    
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

 // increase height of throttle slider (if requested in preferences)
    boolean ish = prefs.getBoolean("increase_slider_height_preference", false);  //TODO fix getting from strings
    
    final DisplayMetrics dm = getResources().getDisplayMetrics();
 // Get the screen's density scale
    
    final float scale = dm.density;
    // Convert the dps to pixels, based on density scale
    int newHeight;
    if (ish) {
    	newHeight = (int) (80 * scale + 0.5f) ;  //increased height
    } else {
    	newHeight = (int) (50 * scale + 0.5f );  //normal height
    }    	
    LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(
    		ViewGroup.LayoutParams.FILL_PARENT,  newHeight); 
    sbS.setLayoutParams(llLp);
    sbT.setLayoutParams(llLp);

    Button b=(Button)findViewById(R.id.button_select_loco_T);
	if (mainapp.loco_string_T.length() > 18) {  //shrink text if long
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
	} else {
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
	}

    if (mainapp.loco_string_T.equals("Not Set")) {
        b.setText("Press to select");
        whichVolume = 'S';  //set the "other" one to use volume control 
    } else {
    	b.setText(mainapp.loco_string_T);
    	throttle_count++;
    }
    b.setSelected(false);
    b.setPressed(false);
 
    b=(Button)findViewById(R.id.button_select_loco_S);
	if (mainapp.loco_string_S.length() > 18) {  //shrink text if long
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
	} else {
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
	}
    if (mainapp.loco_string_S.equals("Not Set")) {
        b.setText("Press to select");
        whichVolume = 'T';  //set the "other" one to use volume control 
    } else {
      	b.setText(mainapp.loco_string_S);
    	throttle_count++;
    }
    b.setSelected(false);
    b.setPressed(false);

    View v = findViewById(R.id.throttle_screen_wrapper);
    int screenHeight = v.getHeight();  //get the height of usable area
    if(screenHeight == 0) {
    	//throttle screen hasn't been drawn yet, so use display metrics for now
        screenHeight = dm.heightPixels - (int)(titleBar * (dm.densityDpi/160.));	//allow for title bar, etc	
    }

    // save 1/2 the screen for webview
    if (mainapp.webViewLocation.equals("Top") || mainapp.webViewLocation.equals("Bottom")) {
    	screenHeight *= 0.5; 
    }
    
    if (screenHeight > throttleMargin) {	//don't do this if height is invalid
		//determine how to split the screen (evenly if both, 85/15 if only one)
    	screenHeight -= throttleMargin;
	    if (throttle_count == 0 || throttle_count == 2)  {
	    	height_T = (int) (screenHeight * 0.5);
	    	height_S = (int) (screenHeight * 0.5);
	    } else if (mainapp.loco_string_T.equals("Not Set")) {
	    	height_T = (int) (screenHeight * 0.15);
	    	height_S = (int) (screenHeight * 0.85);
	    } else {
	    	height_T = (int) (screenHeight * 0.85);
	    	height_S = (int) (screenHeight * 0.15);
	    }
	    
	  //set height of T area 
	    LinearLayout ll=(LinearLayout)findViewById(R.id.throttle_T);
	    llLp = new LinearLayout.LayoutParams(
	            ViewGroup.LayoutParams.FILL_PARENT,
	            height_T);
	    llLp.bottomMargin = (int)(throttleMargin * (dm.densityDpi/160.));
	    ll.setLayoutParams(llLp);
	
	    //set height of S area
	    ll=(LinearLayout)findViewById(R.id.throttle_S);
	    llLp = new LinearLayout.LayoutParams(
	            ViewGroup.LayoutParams.FILL_PARENT,
	            height_S);
	    ll.setLayoutParams(llLp);
    }

    //update the direction indicators
    set_direction_indication('T', dirT);
    set_direction_indication('S', dirS);

    //update the state of each function button based on shared variable
    set_all_function_states('T');
    set_all_function_states('S');
    v.invalidate();
//	  Log.d("Engine_Driver","ending set_labels");
  }

 
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
	  //Handle pressing of the back button to release the selected loco and end this activity
	  if(key==KeyEvent.KEYCODE_BACK)  {
		  final AlertDialog.Builder b = new AlertDialog.Builder(this); 
		  b.setIcon(android.R.drawable.ic_dialog_alert); 
		  b.setTitle(R.string.exit_title); 
		  b.setMessage(R.string.exit_text);
		  b.setCancelable(true);
		  b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int id) {
				  //disconnect from throttle
				  Message msg=Message.obtain();
				  msg.what=message_type.DISCONNECT;
				  mainapp.comm_msg_handler.sendMessage(msg);
			 }
		  } ); 
		  b.setNegativeButton(R.string.no, null);
		  AlertDialog alert = b.create();
		  alert.show();
          return (true);	//stop processing this key
	  } 
	  else if((key==KeyEvent.KEYCODE_VOLUME_UP) || (key==KeyEvent.KEYCODE_VOLUME_DOWN) ) { //use volume to change speed for specified loco
		  SeekBar sb = null;
		  if (whichVolume == 'T' && !mainapp.loco_string_T.equals("Not Set")) {
			  sb=(SeekBar)findViewById(R.id.speed_T);
		  }
		  if (whichVolume == 'S' && !mainapp.loco_string_S.equals("Not Set")) {
			  sb=(SeekBar)findViewById(R.id.speed_S);
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
	  //stop roster or metadata download if still in progress
	  try {
		  dlMetadataTask.stopMetadata();
		  dlRosterTask.stopRoster();
	  }
	  catch(Exception e){
	  }
	  
	  //release the locos
	  Message msg=Message.obtain();
	  try {
		  //release first loco
		  msg.what=message_type.RELEASE;
		  msg.obj=new String("T");
		  mainapp.comm_msg_handler.sendMessage(msg);
		  
		  //release second loco
		  msg=Message.obtain();
		  msg.what=message_type.RELEASE;
		  msg.obj=new String("S");
		  mainapp.comm_msg_handler.sendMessage(msg);
	  }
	  catch(Exception e) {
		  msg.recycle();
	  }

//always go to Connection Activity
//  	  Intent in=new Intent().setClass(this, connection_activity.class);
//	  startActivity(in);

	  this.finish();  //end this activity
//	  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);

	  //	  connection_activity.end_all_activity();
  }

	  
@Override
  public boolean onCreateOptionsMenu(Menu menu){
	  MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.throttle_menu, menu);
	  return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      // Handle all of the possible menu actions.
	  Intent in;
      switch (item.getItemId()) {
      case R.id.settings_menu:
    	  in=new Intent().setClass(this, function_settings.class);
     	  startActivityForResult(in, 0);
     	 connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
   	  break;
      case R.id.about_menu:
    	  in=new Intent().setClass(this, about_page.class);
     	  startActivity(in);
     	 connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
      case R.id.web_menu:
    	  in=new Intent().setClass(this, web_activity.class);
     	  startActivity(in);
     	 connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
      case R.id.preferences:
    	  in=new Intent().setClass(this, preferences.class);
     	  startActivityForResult(in, 0);
     	 connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
      case R.id.power_control_menu:
    	  in=new Intent().setClass(this, power_control.class);
     	  startActivity(in);
     	 connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
      case R.id.turnouts:
    	  in=new Intent().setClass(this, turnouts.class);
     	  startActivity(in);
       	  connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
    	  break;
      case R.id.routes:
    	  in = new Intent().setClass(this, routes.class);
     	  startActivity(in);
       	  connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
     	  break;
      }
      return super.onOptionsItemSelected(item);
  }
  //handle return from menu items
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      //since we always do the same action no need to distinguish between requests
      set_default_function_labels();
      // loop through all function buttons and
      //  set label and dcc functions (based on settings) or hide if no label
      set_function_labels_and_listeners_for_view('T');
      set_function_labels_and_listeners_for_view('S');
      
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
        
		//TODO: move the calc of slider coords elsewhere (for performance)
		//TODO: if slider is disabled, don't consider it in this logic
		//set global variables for slider tops and bottoms, so they can be ignored for swipe 
        View tv=findViewById(R.id.throttle_T);
        View sbT=findViewById(R.id.speed_T);
        T_top= tv.getTop()+sbT.getTop();
        T_bottom= tv.getTop()+sbT.getBottom();
        tv=findViewById(R.id.throttle_S);
        View sbS=findViewById(R.id.speed_S);
        S_top= tv.getTop()+sbS.getTop();
        S_bottom= tv.getTop()+sbS.getBottom();
        //Log.d("Engine_Driver","y="+gestureStartY+" T_top="+T_top+" T_bottom="+T_bottom+" S_top="+S_top+" S_bottom="+S_bottom);
        
        //if gesture is attempting to start over a slider, ignore it and return immediately.
        if ((gestureStartY >= T_top && gestureStartY <= T_bottom) ||
            (gestureStartY >= S_top && gestureStartY <= S_bottom)) {
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

	class DownloadRosterTask extends AsyncTask<String, Void, Integer> {
    	@Override    
    	protected Integer doInBackground(String... params) {
    		Log.d("Engine_Driver","Background loading roster from " + params[0].toString());
    		RosterLoader rl = new RosterLoader(params[0].toString());
    		//if (mainapp.roster != null) mainapp.roster.clear();
    		mainapp.roster = rl.parse();
    		Integer re = 0;
    		if (mainapp.roster != null) {
    			re = mainapp.roster.size();
    		} else {
    			Log.d("Engine_Driver","Failed to load roster.xml.");
    		}
    		return re;  //return the count of entries loaded
    	}
        /**
         * background load of roster completed
         */
        @Override
        protected void onPostExecute(Integer entries) {
			Log.d("Engine_Driver","Loaded " + entries +" entries from roster.xml.");
        }
        
        void getRoster() {
    		//attempt to get roster.xml if webserver port is known and not already retrieved 
    		if (mainapp.web_server_port == 0) {
    			Log.d("Engine_Driver","DownloadRosterTask: web_server_port is not known (yet?)");
    		}
    		if (mainapp.web_server_port > 0 && this.getStatus() == AsyncTask.Status.PENDING) {
   				this.execute("http://"+mainapp.host_ip+":"+mainapp.web_server_port+"/prefs/roster.xml");
    			//Log.d("Engine_Driver","executed roster download");
    		}
        }
        void stopRoster() {
        	if (this.getStatus() == AsyncTask.Status.RUNNING)
        		this.cancel(true);
        }
    }

	class DownloadMetadataTask extends AsyncTask<String, Void, Integer> {
    	@Override    
    	protected Integer doInBackground(String... params) {
    		Log.d("Engine_Driver","Background loading metadata from " + params[0].toString());
    		// retrieve JMRI metadata, and place in global hashmap 
    		threaded_application.metadata = new HashMap<String, String>();
    		String xmlioURL = params[0].toString();
    		Log.d("Engine_Driver", "Fetching JMRI metadata from: " + xmlioURL);
    		Integer re = 0;
    		try  {
    			URL url = new URL( xmlioURL );
    			URLConnection con = url.openConnection();

    			// specify that we will send output and accept input
    			con.setDoInput(true);
    			con.setDoOutput(true);
    			con.setConnectTimeout( 2000 );
    			con.setReadTimeout( 2000 );
    			con.setUseCaches (false);
    			con.setDefaultUseCaches (false);

    			// tell the web server to expect xml text
    			con.setRequestProperty ( "Content-Type", "text/xml" );

    			OutputStreamWriter writer = new OutputStreamWriter( con.getOutputStream() );
    			writer.write( "<XMLIO><list type='metadata' /></XMLIO>" );  //ask for metadata info
    			writer.flush();
    			writer.close();

    			//read response and treat as xml doc
    			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    			DocumentBuilder builder = factory.newDocumentBuilder();
    			Document dom = builder.parse( con.getInputStream() );
    			Element root = dom.getDocumentElement();
    			//get list of metadata children and loop thru, putting each in global variable metadata
    			NodeList items = root.getElementsByTagName("metadata");
    			for (int i=0;i<items.getLength();i++){
    				String metadataName = items.item(i).getAttributes().getNamedItem("name").getNodeValue(); 
    				String metadataValue = items.item(i).getAttributes().getNamedItem("value").getNodeValue(); 
    				threaded_application.metadata.put(metadataName, metadataValue);
    				re++;
    			}
    			Log.d("Engine_Driver", "Metadata retrieved: " + threaded_application.metadata.toString());

    		}
    		catch( Throwable t )	  {
    			Log.d("Engine_Driver", "Metadata fetch failed: " + t.getMessage());
    		}	  
    		return re;  //return the count of entries loaded
    	}
        // background load of Metadata completed
        @Override
        protected void onPostExecute(Integer entries) {
			Log.d("Engine_Driver","Loaded " + entries +" metadata entries from xmlio server.");
        }
        
        void getMetadata() {
    		//attempt to get roster.xml if webserver port is known and not already retrieved 
    		if (mainapp.web_server_port == 0) {
    			Log.d("Engine_Driver","DownloadMetadataTask: web_server_port is not known (yet?)");
    		}
    		if (mainapp.web_server_port > 0 && this.getStatus() == AsyncTask.Status.PENDING) {
   				this.execute("http://"+mainapp.host_ip+":"+mainapp.web_server_port + "/xmlio/");
    		}
        }
        void stopMetadata() {
        	if (this.getStatus() == AsyncTask.Status.RUNNING)
        		this.cancel(true);
        }
    }


}

