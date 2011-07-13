/*Copyright (C) 2010 Jason M'Sadoques
  jlyonm@gmail.com

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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;

import android.util.Log;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnTouchListener;
import android.os.Message;
import android.widget.TextView;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.graphics.Typeface;

public class throttle extends Activity implements android.gesture.GestureOverlayView.OnGestureListener {

	private threaded_application mainapp;  // hold pointer to mainapp
	private static final int GONE = 8;
	private static final int VISIBLE = 0;
	private SharedPreferences prefs;
	private Timer heartbeatTimer;
	private static final int MAX_SPEED_DISPLAY = 99;	// value to display at maximum speed setting 
	private static final int MAX_SPEED_VAL = 126;		// maximum speed setting
	// speed scale factors
	private static final double SPEED_TO_DISPLAY = ((double)(MAX_SPEED_DISPLAY) / MAX_SPEED_VAL);
	private static final double DISPLAY_TO_SPEED = (1.0 / SPEED_TO_DISPLAY);
	
	boolean heartbeat = true; //turn on with each response, show error if not on

	private String whichVolume = "T";

	//these are used for gesture tracking
	private int gestureStartX = 0;
	private int gestureStartY = 0;
	private boolean gestureFailed = false;			// gesture didn't meet velocity or distance requirement
	private boolean gestureInProgress = false;		// gesture is in progress
	private long gestureLastCheckTime;				// time in milliseconds that velocity was last checked
	private static final long gestureCheckRate = 200;	// rate in milliseconds to check velocity
	private VelocityTracker mVelocityTracker;


  //Handle messages from the communication thread TO this thread (responses from withrottle)
  class throttle_handler extends Handler {

	public void handleMessage(Message msg) {
		
		switch(msg.what) {
	        
	        case message_type.RESPONSE: {  //handle messages from WiThrottle server
	    		
	        	heartbeat = true; //any response

	    		String response_str = msg.obj.toString();

	        	switch (response_str.charAt(0)) {

	        	  //various MultiThrottle responses
	        	  case 'M':
	        		if (response_str.substring(2,3).equals("+")) {  //if loco added, refresh the function labels
		      	  		set_default_function_labels();
		      	  		// loop through all function buttons and
		      	  		//   set label and dcc functions (based on settings) or hide if no label
		      	  		enable_disable_buttons(response_str.substring(1,2));  //pass whichthrottle
		      	  		if (response_str.charAt(1) == 'T') {
		      	  			ViewGroup tv = (ViewGroup) findViewById(R.id.function_buttons_table_T);
		      	  			set_function_labels_and_listeners_for_view("T");
		      	  			enable_disable_buttons_for_view(tv, true);
		      	  		} else if (response_str.charAt(1) == 'S') {
		      	  			ViewGroup tv = (ViewGroup) findViewById(R.id.function_buttons_table_S);
		      	  			set_function_labels_and_listeners_for_view("S");
		      	  			enable_disable_buttons_for_view(tv, true);
		      	  		}
	        		} else if (response_str.substring(2,3).equals("A")) {  //e.g. MTAL2608<;>R1
	        			String whichThrottle = response_str.substring(1,2);  //TODO: move this processing to ta?
    	  				String[] ls = threaded_application.splitByString(response_str,"<;>");
	    	  			if (ls[1].substring(0,1).equals("R")) {
	    	  				int dir;
	    	  				try {
	    	  					dir = new Integer(ls[1].substring(1,2));
	    	  				}
	    	  				catch(NumberFormatException e) {
	    	  					dir = 1;
	    	  				}
	    	  				set_direction_buttons(whichThrottle, dir); //set direction button 
	    	  			} 
	    	  			else if (ls[1].substring(0,1).equals("V")) {
	    	  				int speed = 0;
	    	  				try {
	    	  					speed = Integer.parseInt(ls[1].substring(1));
	    	  				}
	    	  				catch(NumberFormatException e) {
	    	  				}
        	  				set_speed_slider(whichThrottle, speed); //set slider to value
	    	  			}	    	  			

	        		}
		        	set_labels();
	        		break;
	      	  	  
	      	  	  case 'T':
	      	  	  case 'S':
		      	  		enable_disable_buttons(response_str.substring(0,1));  //pass whichthrottle
			        	set_labels();
			        	break;
	      	  	  
	      	  	  case 'R': //roster function labels
	      	  		  if (response_str.charAt(1) == 'F') {
	      	  			  ViewGroup tv = (ViewGroup) findViewById(R.id.function_buttons_table_T);
	      	  			  set_function_labels_and_listeners_for_view("T");
	      	  			  enable_disable_buttons_for_view(tv, true);
	      	  		  } else if (response_str.charAt(1) == 'S') {
	      	  			  ViewGroup tv = (ViewGroup) findViewById(R.id.function_buttons_table_S);
	      	  			  set_function_labels_and_listeners_for_view("S");
	      	  			  enable_disable_buttons_for_view(tv, true);
	      	  		  }
	      	  		  set_labels();
	      	  		  break;
	        	}  //end of switch
	        	
	        }
	        break;
	        case message_type.HEARTBEAT: {
	        	// refresh text labels
	        	set_labels();
	        	//only check for heartbeat if version 2.0+, heartbeat enabled, and loco selected
	        	if  (mainapp.withrottle_version >= 2.0 && mainapp.heartbeat_interval > 0
	        			&&	(!mainapp.loco_string_T.equals("Not Set") || !mainapp.loco_string_S.equals("Not Set"))) {
	        		if (!heartbeat) {
	        			Toast.makeText(getApplicationContext(), "WARNING: No response from WiThrottle server for " + mainapp.heartbeat_interval  + " seconds.  Verify connection.", Toast.LENGTH_LONG).show();
	        		}
	        		heartbeat = false;
	        	}
	        }
	        break;
	      }
	    };
	  }

  void set_speed_slider(String whichThrottle, int speed) {
	  TextView speed_label;
	  SeekBar throttle_slider;

	  if (whichThrottle.equals("T")) {
		  speed_label=(TextView)findViewById(R.id.speed_value_label_T);
		  throttle_slider=(SeekBar)findViewById(R.id.speed_T);
	  } else {
		  speed_label=(TextView)findViewById(R.id.speed_value_label_S);
		  throttle_slider=(SeekBar)findViewById(R.id.speed_S);
	  }
	  int displayedSpeed = 0;
	  if(speed < 0)
		  speed = 0;
	  else
		  displayedSpeed = (int)Math.round(speed * SPEED_TO_DISPLAY); //show speed as 0-99 instead of 0-126
	  speed_label.setText(Integer.toString(displayedSpeed));
	  throttle_slider.setProgress(speed);
  }

//"press" correct direction button based on direction returned from server
  //TODO: improve this code
  void set_direction_buttons(String whichThrottle, Integer direction) {
	  Button bFwd;
	  Button bRev;
	  if (whichThrottle.equals("T")) {
		  bFwd = (Button)findViewById(R.id.button_fwd_T);
		  bRev = (Button)findViewById(R.id.button_rev_T);
	  } else {
		  bFwd = (Button)findViewById(R.id.button_fwd_S);
		  bRev = (Button)findViewById(R.id.button_rev_S);
	  }
	  if (direction == 0) {
		  bFwd.setPressed(false);
		  bFwd.setTypeface(null, Typeface.NORMAL);
		  bRev.setPressed(true);
		  bRev.setTypeface(null, Typeface.ITALIC);
	  } else {
		  bFwd.setPressed(true);
		  bFwd.setTypeface(null, Typeface.ITALIC);
		  bRev.setPressed(false);
		  bRev.setTypeface(null, Typeface.NORMAL);
	  }
  }
  
  void set_stop_button(String whichThrottle, boolean pressed) {
	  Button bStop;
	  if (whichThrottle.equals("T")) {
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
void start_select_loco_activity(String whichThrottle)
  {
    Intent select_loco=new Intent().setClass(this, select_loco.class);
    select_loco.putExtra("whichThrottle", whichThrottle);  //pass whichThrottle as an extra to activity
//    select_loco.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    startActivityForResult(select_loco, 0);
    connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
  };

  void enable_disable_buttons(String whichThrottle)  {
      boolean newEnabledState;
	  if (whichThrottle.equals("T")) {
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

  //helper function to loop thru buttons, setting label text and appearance based on current function state (on or off)
  void set_function_states(String whichThrottle)  {
//	  Log.d("Engine_Driver","starting set_function_states");
	  ViewGroup vg; //table
	  ViewGroup r;  //row
	  Button b; //button
	  boolean[] fs;  //copy of this throttle's function state array
	  int k = 0; //counter for button array
	  LinkedHashMap<Integer, String> function_labels_temp = new  LinkedHashMap<Integer, String>();
	  
	  if (whichThrottle.equals("T")) {
  		 vg = (ViewGroup)findViewById(R.id.function_buttons_table_T);
  		 fs = mainapp.function_states_T;
  		 if (mainapp.function_labels_T != null && mainapp.function_labels_T.size()>0) {  //use roster functions or default functions
  			function_labels_temp = mainapp.function_labels_T;
  		 } else {
   			function_labels_temp = mainapp.function_labels_default;
  		 }
  	  } else {
		 vg = (ViewGroup)findViewById(R.id.function_buttons_table_S);
  		 fs = mainapp.function_states_S;
  		 if (mainapp.function_labels_S != null && mainapp.function_labels_S.size()>0) {  //use roster functions or default functions
   			function_labels_temp = mainapp.function_labels_S;
  		 } else {
   			function_labels_temp = mainapp.function_labels_default;
 		 }
  	  }

	  if (fs !=null) { //don't bother if no function states found
		  //put values in array for indexing in next step TODO: find direct way to do this
		  ArrayList<Integer> aList = new ArrayList<Integer>();
		  for (Integer f : function_labels_temp.keySet()) {
			  aList.add(f);
		  }

		  for(int i = 0; i < vg.getChildCount(); i++) {
			  r = (ViewGroup)vg.getChildAt(i);
			  for(int j = 0; j < r.getChildCount(); j++) {
				  if (k < aList.size()) {  //TODO: short-circuit this
					  b = (Button)r.getChildAt(j);
					  if (fs[aList.get(k)]) {  //get function number for kth button, and look up state in shared variable
						  b.setTypeface(null, Typeface.ITALIC);
						  b.setPressed(true);
					  } else {
						  b.setTypeface(null, Typeface.NORMAL);
						  b.setPressed(false);
					  }
				  }
				  k++;
			  }
		  }
	  }
} //end of set_function_labels_and_states 

  public class function_button_touch_listener implements View.OnTouchListener
  {
    int function;
    String whichThrottle;  //T for first throttle, S for second 

//    public function_button_touch_listener(int new_function, boolean new_toggle_type, String new_whichThrottle)
    public function_button_touch_listener(int new_function, String new_whichThrottle)
    {
      function=new_function;  //store these values for this button
      whichThrottle = new_whichThrottle;
    }

    public boolean onTouch(View v, MotionEvent event)
    {
      if(gestureInProgress == true)
      {
    	  //add movement and recheck gesture velocity
    	  //if gesture is still in progress, skip button processing
          Log.d("Engine_Driver", "onTouch func" + function + " action " + event.getAction());
    	  gestureVelocityCheck(event);
    	  if(gestureInProgress == true) 
    		  return(true);
      }
      if(gestureFailed == true)
      {
    	  // if gesture just failed, insert one DOWN event on this control
    	  handleAction(MotionEvent.ACTION_DOWN);
    	  gestureFailed = false;	// just do this once
      }
      handleAction(event.getAction());
      return(true);
    }

    private void handleAction(int action) 
    {
      Log.d("Engine_Driver", "handleAction func" + function + " action " + action);
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
            	set_direction_buttons(whichThrottle, 1);
            }
              break;

            case function_button.REVERSE : {
            	function_msg.what=message_type.DIRECTION;
                function_msg.arg1=1;
            	function_msg.arg2=0;  //reverse is 0
            	//show pressed image on current button, and turn off pressed image on "other" button 
            	set_direction_buttons(whichThrottle, 0);
            	//            	v.setPressed(true);
              }
              break;

            // setting the throttle slide to 0 sends a velocity change to the withrottle          	  
            case function_button.STOP : { 
            	set_stop_button(whichThrottle, true);
            	SeekBar sb;
            	if (whichThrottle.equals("T")) {
            	  sb=(SeekBar)findViewById(R.id.speed_T);
            	} else {
              	  sb=(SeekBar)findViewById(R.id.speed_S);
            	}
              sb.setProgress(0);
        	  break;
            }
            // specify which throttle the volume button controls          	  
            case function_button.SPEED_LABEL: { 
            	whichVolume = whichThrottle;  //use whichever was clicked
            	set_labels();
        	  break;
            }
            case function_button.SELECT_LOCO : {
            	start_select_loco_activity(new String(whichThrottle));  //pass throttle #
              }
              break;

            default : {  //handle the function buttons
              function_msg.what=message_type.FUNCTION;
              function_msg.arg1=function;
              function_msg.arg2=1;
            }
          }  //end of function switch
          
          if (function_msg.what != message_type.NONE) {  //don't send if no payload
            function_msg.obj=new String(whichThrottle);    // always load whichThrottle into message
            mainapp.comm_msg_handler.sendMessage(function_msg);            //send the message to comm thread
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
        		function_msg.obj=new String(whichThrottle);    // always load whichThrottle into message
        		mainapp.comm_msg_handler.sendMessage(function_msg);
        	}
        	break;
      }
    };
  }

  public class throttle_listener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener
  {
      String whichThrottle;
	  SeekBar speed_slider;
	  TextView speed_label;
//	  int progress;
      
    public throttle_listener(String new_whichThrottle)    {
	    whichThrottle = new_whichThrottle;   	//store values for this listener
  		if (whichThrottle.equals("T")) {
			speed_label=(TextView)findViewById(R.id.speed_value_label_T);
			speed_slider=(SeekBar)findViewById(R.id.speed_T);
		} 
		else {
			speed_label=(TextView)findViewById(R.id.speed_value_label_S);
			speed_slider=(SeekBar)findViewById(R.id.speed_S);
		}
//  		progress = speed_slider.getProgress();
    }
   
    public boolean onTouch(View v, MotionEvent event) 
    {
        Log.d("Engine_Driver", "onTouchThrot action " + event.getAction());
    	if(gestureInProgress == true)
    	{
    		//add movement and recheck gesture velocity
    		gestureVelocityCheck(event);
    	}
    	//consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged
    	return(gestureInProgress);
    }

    public void onProgressChanged(SeekBar throttle, int speed, boolean fromUser)
    {
        Log.d("Engine_Driver", "onProgChanged speed " + speed);
		Message msg=Message.obtain();
		msg.what=message_type.VELOCITY;
		msg.arg1=speed;
		msg.obj=new String(whichThrottle);    // always load whichThrottle into message
		mainapp.comm_msg_handler.sendMessage(msg);
		int displayedSpeed = (int)Math.round(speed * SPEED_TO_DISPLAY);
		speed_label.setText(Integer.toString(displayedSpeed));
//			progress = speed;
    }

	@Override
	public void onStartTrackingTouch(SeekBar sb) {
        Log.d("Engine_Driver", "onStartTracking");
	}

	@Override
	public void onStopTrackingTouch(SeekBar sb) {
        Log.d("Engine_Driver", "onStopTracking");
	}

  }

  //Handle pressing of the back button to release the selected loco and end this activity
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
	  if(key==KeyEvent.KEYCODE_BACK)  {
//		  mainapp.throttle_msg_handler = null; //clear out pointer to this activity
		  
		  //release first loco
		  Message msg=Message.obtain();
		  msg.what=message_type.RELEASE;
		  msg.obj=new String("T");
		  mainapp.comm_msg_handler.sendMessage(msg);

		  //release second loco
		  msg=Message.obtain();
		  msg.what=message_type.RELEASE;
		  msg.obj=new String("S");
		  mainapp.comm_msg_handler.sendMessage(msg);

		  //disconnect from throttle
		  msg=Message.obtain();
		  msg.what=message_type.DISCONNECT;
		  mainapp.comm_msg_handler.sendMessage(msg);  

		  //kill the heartbeat timer
		  if (heartbeatTimer != null) {
			  heartbeatTimer.cancel();
		  }
		  
		  //always go to Connection Activity
//	  	  Intent in=new Intent().setClass(this, connection_activity.class);
//	  	  in.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//		  startActivity(in);
		  this.finish();  //end this activity
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
//          return true;
	  } 
	  else if((key==KeyEvent.KEYCODE_VOLUME_UP) || (key==KeyEvent.KEYCODE_VOLUME_DOWN) ) { //use volume to change speed for specified loco
		  SeekBar sb = null;
		  if (whichVolume.equals("T") && !mainapp.loco_string_T.equals("Not Set")) {
			  sb=(SeekBar)findViewById(R.id.speed_T);
		  }
		  if (whichVolume.equals("S") && !mainapp.loco_string_S.equals("Not Set")) {
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
  };
  
  @Override
public void onResume() {
  super.onResume();

  //format the screen area
  enable_disable_buttons("T"); 
  enable_disable_buttons("S");  
  set_labels();
  gestureFailed = false;
  gestureInProgress = false;
}


@Override
public void onStart() {
  super.onStart();

  //put pointer to this activity's handler in main app's shared variable (If needed)
  if (mainapp.throttle_msg_handler == null){
	  mainapp.throttle_msg_handler=new throttle_handler();
  }
 
  //create heartbeat if requested and not already started
  if ((heartbeatTimer == null) && (mainapp.heartbeat_interval > 0)) {
      int interval = (mainapp.heartbeat_interval - 1) * 900;  //set heartbeat one second less than required (900 copied from withrottle side)

    heartbeatTimer = new Timer();
  	heartbeatTimer.schedule(new TimerTask() {
  		@Override
  		public void run() {
  			send_heartbeat();
  		}
  	}, 100, interval);
  }
  
  set_labels();

}


  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.throttle);


    mainapp=(threaded_application)getApplication();

    prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
    
//    myGesture = new GestureDetector(this);
    GestureOverlayView ov = (GestureOverlayView)findViewById(R.id.throttle_overlay);
    ov.addOnGestureListener(this);
    ov.setGestureVisible(false);
    
    Button b;
    function_button_touch_listener fbtl;
    
    //set listener for select loco buttons
    b = (Button)findViewById(R.id.button_select_loco_T);
    fbtl=new function_button_touch_listener(function_button.SELECT_LOCO, "T");
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_select_loco_S);
    fbtl=new function_button_touch_listener(function_button.SELECT_LOCO, "S");
    b.setOnTouchListener(fbtl);
    
    // set listeners for 3 direction buttons for each throttle
    b = (Button)findViewById(R.id.button_fwd_T);
    fbtl=new function_button_touch_listener(function_button.FORWARD, "T");
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_stop_T);
    fbtl=new function_button_touch_listener(function_button.STOP,  "T");
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_rev_T);
    fbtl=new function_button_touch_listener(function_button.REVERSE, "T");
    b.setOnTouchListener(fbtl);
    View v = findViewById(R.id.speed_cell_T);
    fbtl=new function_button_touch_listener(function_button.SPEED_LABEL, "T");
    v.setOnTouchListener(fbtl);

    b = (Button)findViewById(R.id.button_fwd_S);
    fbtl=new function_button_touch_listener(function_button.FORWARD, "S");
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_stop_S);
    fbtl=new function_button_touch_listener(function_button.STOP, "S");
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_rev_S);
    fbtl=new function_button_touch_listener(function_button.REVERSE, "S");
    b.setOnTouchListener(fbtl);
    v = findViewById(R.id.speed_cell_S);
    fbtl=new function_button_touch_listener(function_button.SPEED_LABEL, "S");
    v.setOnTouchListener(fbtl);

    // set up listeners for both throttles
    throttle_listener th1;
    SeekBar sb=(SeekBar)findViewById(R.id.speed_T);
    th1 = new throttle_listener("T");
    sb.setOnSeekBarChangeListener(th1);
    sb.setOnTouchListener(th1);
    
    sb=(SeekBar)findViewById(R.id.speed_S);
    th1 = new throttle_listener("S");
    sb.setOnSeekBarChangeListener(th1);
    sb.setOnTouchListener(th1);

    set_default_function_labels();
    // loop through all function buttons and
    //   set label and dcc functions (based on settings) or hide if no label
    set_function_labels_and_listeners_for_view("T");
    set_function_labels_and_listeners_for_view("S");

    ov.bringToFront();
    
  } //end of onCreate()

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
  void set_function_labels_and_listeners_for_view(String whichThrottle)  {
//	  Log.d("Engine_Driver","starting set_function_labels_and_listeners_for_view");

	  ViewGroup tv; //group
	  ViewGroup r;  //row
	  function_button_touch_listener fbtl;
	  Button b; //button
	  int k = 0; //button count
	  LinkedHashMap<Integer, String> function_labels_temp = new  LinkedHashMap<Integer, String>();

	  if (whichThrottle.equals("T")) {
		  tv = (ViewGroup) findViewById(R.id.function_buttons_table_T); //table
	  } else {
		  tv = (ViewGroup) findViewById(R.id.function_buttons_table_S); //table
	  }
	  
	  if (whichThrottle.equals("T") && mainapp.function_labels_T != null && mainapp.function_labels_T.size()>0) {
		  function_labels_temp = mainapp.function_labels_T;  //point temp to T
	  } else if (whichThrottle.equals("S") && mainapp.function_labels_S != null  && mainapp.function_labels_S.size()>0) {
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
} //end of set_function_buttons_for_view

  //lookup and set values of various informational text labels and size the screen elements 
  private void set_labels() {

//	  Log.d("Engine_Driver","starting set_labels");

    int throttle_count = 0;
   	int height_T;
	int height_S;
  
    // hide or display volume control indicator based on variable
    View viT = findViewById(R.id.volume_indicator_T);
    View viS = findViewById(R.id.volume_indicator_S);
    if (whichVolume.equals("T")) {
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
    int maxThrottle = MAX_SPEED_DISPLAY;
    try {
    	maxThrottle = Integer.parseInt(s);
    } catch (NumberFormatException e) {
	}

    maxThrottle = (int) Math.round(((double)(maxThrottle)/MAX_SPEED_DISPLAY) * MAX_SPEED_VAL);
    sbT.setMax(maxThrottle);
    sbS.setMax(maxThrottle);

 // increase height of throttle slider (if requested in preferences)
    boolean ish = prefs.getBoolean("increase_slider_height_preference", false);  //TODO fix getting from strings
    
 // Get the screen's density scale
    final float scale = getResources().getDisplayMetrics().density;
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
        whichVolume = "S";  //set the "other" one to use volume control 
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
        whichVolume = "T";  //set the "other" one to use volume control 
    } else {
      	b.setText(mainapp.loco_string_S);
    	throttle_count++;
    }
    b.setSelected(false);
    b.setPressed(false);

    int screenHeight = findViewById(R.id.throttle_screen).getHeight();  //get the height of usable area
  
    if (screenHeight > 0) {  //don't do this if screen not measurable
		//determine how to split the screen (evenly if both, 85/15 if only one)
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
	    ll.setLayoutParams(llLp);
	
	    //set height of S area
	    ll=(LinearLayout)findViewById(R.id.throttle_S);
	    llLp = new LinearLayout.LayoutParams(
	            ViewGroup.LayoutParams.FILL_PARENT,
	            height_S);
	    ll.setLayoutParams(llLp);
    }

    //update the state of each function button based on shared variable
    set_function_states("T");
    set_function_states("S");

  }

  //send heartbeat to withrottle to keep this throttle alive (if enabled in withrottle prefs)
  private void send_heartbeat() {
	  if (mainapp.heartbeat_interval > 0) {
	    Message msg=Message.obtain();
	    msg.what=message_type.HEARTBEAT;
	    mainapp.comm_msg_handler.sendMessage(msg);
	  }
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
      //   set label and dcc functions (based on settings) or hide if no label
      set_function_labels_and_listeners_for_view("T");
      set_function_labels_and_listeners_for_view("S");
      heartbeat = true;
//	  set_labels();
//	  enable_disable_buttons("T");
//	  enable_disable_buttons("S");  
  }
  
  // touch events outside the GestureOverlayView get caught here 
  @Override
  public boolean onTouchEvent(MotionEvent event){
	  if(event.getAction() == MotionEvent.ACTION_DOWN)
		  gestureStart(event);
	  else if(event.getAction() == MotionEvent.ACTION_UP)
		  gestureEnd(event);
	  return true;
  }

	@Override
	public void onGesture(GestureOverlayView arg0, MotionEvent event) {
        Log.d("Engine_Driver", "onGesture action " + event.getAction());
		gestureVelocityCheck(event);
	}
	
	public void gestureVelocityCheck(MotionEvent event)
	{
		if(gestureInProgress == true)
		{
			mVelocityTracker.addMovement(event);
			if((event.getEventTime() - gestureLastCheckTime) > gestureCheckRate) 
			{
				//monitor velocity and fail gesture if it is too low
				gestureLastCheckTime = event.getEventTime();
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000);
				int velocityX = (int) velocityTracker.getXVelocity();
		        Log.d("Engine_Driver", "gestureVelocity vel " + velocityX);
				if(Math.abs(velocityX) < threaded_application.min_fling_velocity)
				{
					gestureFailed = true;
					gestureInProgress = false;
				}
			}
		}
	}

	@Override
	public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        Log.d("Engine_Driver", "onGestureCancelled action " + event.getAction());
		gestureInProgress = false;
		gestureEnd(event);
	}
	
	//determine if the action was long enough to be a swipe
	@Override
	public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
        Log.d("Engine_Driver", "onGestureEnded action " + event.getAction());
		gestureEnd(event);
	}
	
	//save start position of potential gesture
	@Override
	public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
        Log.d("Engine_Driver", "onGestureStarted action" + event.getAction());
		gestureStart(event);
	}
	
	private void gestureStart( MotionEvent event ) {
        Log.d("Engine_Driver", "gestureStart action " + event.getAction());
		gestureStartX = (int) event.getX();
		gestureStartY = (int) event.getY();
		// track the new gesture. seekbar onProgressChanged clears this flag to stop tracking.
		gestureInProgress = true;
		gestureFailed = false;
		gestureLastCheckTime = event.getEventTime();
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.clear();
	}

	private void gestureEnd( MotionEvent event) {
		if(gestureInProgress == true)
		{
			
			if(Math.abs(event.getX() - gestureStartX) > threaded_application.min_fling_distance) 
			{
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
				gestureInProgress = false;
				gestureFailed = true;
			}
		}
//		mVelocityTracker.recycle();
	}
	void showEvent(MotionEvent event, String func)
	{
		String eventName;
		switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				eventName = "ACTION_DOWN";
				break;
			case MotionEvent.ACTION_UP:
				eventName = "ACTION_UP";
				break;
			case MotionEvent.ACTION_MOVE:
				eventName = "ACTION_MOVE";
				break;
			default:
				eventName = "ACTION_OTHER";
				break;
		}
		Log.d("Engine_Driver", func + " " + eventName + " gestureInProg " + gestureInProgress);
		
	}

}