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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;

import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;
import android.view.MotionEvent;
import android.os.Message;
import android.widget.TextView;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

public class engine_driver extends Activity {

  private threaded_application mainapp;  // hold pointer to mainapp
  private static final int GONE = 8;
  private static final int VISIBLE = 0;
  private SharedPreferences prefs;
  private Timer heartbeatTimer;

  private Drawable button_pressed_drawable;  //hold background graphics for buttons
  private Drawable button_normal_drawable;

  ArrayList<String> aLbl;
  ArrayList<Integer> aFnc;

  //Handle messages from the communication thread TO this thread (responses from withrottle)
  class engine_driver_handler extends Handler {

	public void handleMessage(Message msg) {
	      switch(msg.what) {
	        
	        case message_type.RESPONSE: {  //handle messages from WiThrottle server
	        	String response_str = msg.obj.toString();

	        	//loco connected (or disconnected)
	        	switch (response_str.charAt(0)) {
	      	  	  case 'T':
	      	  	  case 'S':
		      	  		enable_disable_buttons(response_str.substring(0,1));  //pass whichthrottle
	      	  	  break;
	      	  	
	        	}  //end of switch
	        	
	        	// refresh text labels
	        	set_labels();
//	        	String response_str = msg.obj.toString();
//	        	Toast.makeText(getApplicationContext(), "responsed with:" + response_str, Toast.LENGTH_LONG).show();  //debugging use only
	        }
	        break;
	        case message_type.HEARTBEAT: {
	        	// refresh text labels
	        	set_labels();  //TODO: is this still needed?
	        }
	        break;
	        case message_type.END_ACTIVITY: {      	    //Program shutdown has been requested
	      	    end_this_activity();
	      	}
	        break;
	      }
	    };
	  }

  //end current activity
  void end_this_activity() {
	  mainapp.engine_driver_msg_handler = null;
	  this.finish();
  }

  void start_select_loco_activity(String whichThrottle)
  {
    Intent select_loco=new Intent().setClass(this, select_loco.class);
    select_loco.putExtra("whichThrottle", whichThrottle);  //pass whichThrottle as an extra to activity
    startActivityForResult(select_loco, 0);
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
	  ViewGroup r;  //row
	  Button b; //button
	  for(int i = 0; i < vg.getChildCount(); i++) {
	      r = (ViewGroup)vg.getChildAt(i);
	      for(int j = 0; j < r.getChildCount(); j++) {
	      	b = (Button)r.getChildAt(j);
       	    b.setEnabled(newEnabledState);
	      }
	  }
} //end of set_function_buttons_for_view

  //helper function to loop thru buttons, setting appearance based on current function state (on or off)
  void set_function_states(String whichThrottle)  {
	  ViewGroup vg; //table
	  ViewGroup r;  //row
	  Button b; //button
	  boolean[] fs;  //copy of this throttle's function state array
	  int k = 0; //counter for button array
  	  if (whichThrottle.equals("T")) {
  		 vg = (ViewGroup)findViewById(R.id.function_buttons_table_T);
  		 fs = mainapp.function_states_T;
  	  } else {
		 vg = (ViewGroup)findViewById(R.id.function_buttons_table_S);
  		 fs = mainapp.function_states_S;
  	  }
	  for(int i = 0; i < vg.getChildCount(); i++) {
	      r = (ViewGroup)vg.getChildAt(i);
	      for(int j = 0; j < r.getChildCount(); j++) {
	    	if (k < aFnc.size()) {  //TODO: short-circuit this
		      	b = (Button)r.getChildAt(j);
		      	if (fs[aFnc.get(k)]) {  //get function number for kth button, and look up state in shared variable
			      	  b.setTypeface(null, Typeface.ITALIC);
		      	} else {
			      	  b.setTypeface(null, Typeface.NORMAL);
		      	}
	    	}
  	        k++;
	      }
	  }
} //end of set_function_buttons_for_view

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
      switch(event.getAction())
      {
        case MotionEvent.ACTION_DOWN:
        {
          Message function_msg=Message.obtain();  //create a message to be used by many of the buttons below
          function_msg.what = message_type.NONE;

          switch (function) {
            case function_button.FORWARD : {
            	Button b;
            	function_msg.what=message_type.DIRECTION;
                function_msg.arg1=1;
            	function_msg.arg2=1;  //forward is 1
            	//show pressed image on current button, and turn off pressed image on "other" button 
                v.setBackgroundDrawable(button_pressed_drawable);
            	if (whichThrottle.equals("T")) {
                    b = (Button)findViewById(R.id.button_rev_T);
              	} else {
                    b = (Button)findViewById(R.id.button_rev_S);
              	}
            	b.setBackgroundDrawable(button_normal_drawable);
            }
              break;

            case function_button.REVERSE : {
            	Button b;
            	function_msg.what=message_type.DIRECTION;
                function_msg.arg1=1;
            	function_msg.arg2=0;  //reverse is 0
            	//show pressed image on current button, and turn off pressed image on "other" button 
            	v.setBackgroundDrawable(button_pressed_drawable);
            	if (whichThrottle.equals("T")) {
                    b = (Button)findViewById(R.id.button_fwd_T);
              	} else {
                    b = (Button)findViewById(R.id.button_fwd_S);
              	}
            	b.setBackgroundDrawable(button_normal_drawable);
              }
              break;

            // setting the throttle slide to 0 sends a velocity change to the withrottle          	  
            case function_button.STOP : { 
            	SeekBar sb;
            	if (whichThrottle.equals("T")) {
            	  sb=(SeekBar)findViewById(R.id.speed_T);
            	} else {
              	  sb=(SeekBar)findViewById(R.id.speed_S);
            	}
              sb.setProgress(0);
        	  break;
            }
            case function_button.SELECT_LOCO : {
            	start_select_loco_activity(new String(whichThrottle));  //pass throttle #
              }
              break;
            default : {
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
          // only process UP for function buttons
          if(function < function_button.FORWARD)   {
            Message function_msg=Message.obtain();
            function_msg.what=message_type.FUNCTION;
            function_msg.arg1=function;
            function_msg.arg2=0;
            function_msg.obj=new String(whichThrottle);    // always load whichThrottle into message
            mainapp.comm_msg_handler.sendMessage(function_msg);
          }
        break;
      }
      return(false);
    };
  }

  public class throttle_listener implements SeekBar.OnSeekBarChangeListener
  {
      String whichThrottle;
      
    public throttle_listener(String new_whichThrottle)    {
	      whichThrottle = new_whichThrottle;   //store this value for this listener
	    }

    public void onProgressChanged(SeekBar throttle, int speed, boolean fromUser)
    {
      TextView speed_label;
      Message msg=Message.obtain();
      msg.what=message_type.VELOCITY;
      msg.arg1=speed;
      msg.obj=new String(whichThrottle);    // always load whichThrottle into message
      mainapp.comm_msg_handler.sendMessage(msg);
  	   if (whichThrottle.equals("T")) {
  	      speed_label=(TextView)findViewById(R.id.speed_value_label_T);
  	   } else {
  	      speed_label=(TextView)findViewById(R.id.speed_value_label_S);
  	   }
  	  int displayedSpeed = (int)Math.round(speed * 99.0 / 126.0); //show speed as 0-99 instead of 0-126
      speed_label.setText(Integer.toString(displayedSpeed));
    }

	@Override
	public void onStartTrackingTouch(SeekBar sb) {}

	@Override
	public void onStopTrackingTouch(SeekBar sb) {}

  }

  //Handle pressing of the back button to release the selected loco and end this activity
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
  if(key==KeyEvent.KEYCODE_BACK)
  {
    mainapp.engine_driver_msg_handler = null; //clear out pointer to this activity  
    
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
    this.finish();  //end this activity
  }
  return(super.onKeyDown(key, event));
};

@Override
public void onResume() {
  super.onResume();

  //format the screen area
  set_labels();

}


@Override
public void onStart() {
  super.onStart();

  //put pointer to this activity's handler in main app's shared variable (If needed)
  if (mainapp.engine_driver_msg_handler == null){
	  mainapp.engine_driver_msg_handler=new engine_driver_handler();
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

}


  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.throttle);


    mainapp=(threaded_application)getApplication();

    button_pressed_drawable=getResources().getDrawable(R.drawable.btn_default_small_pressed);
    button_normal_drawable=getResources().getDrawable(R.drawable.btn_default_small_normal);

    prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

    set_function_buttons();

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

    b = (Button)findViewById(R.id.button_fwd_S);
    fbtl=new function_button_touch_listener(function_button.FORWARD, "S");
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_stop_S);
    fbtl=new function_button_touch_listener(function_button.STOP, "S");
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_rev_S);
    fbtl=new function_button_touch_listener(function_button.REVERSE, "S");
    b.setOnTouchListener(fbtl);

    // set up listeners for both throttles
    SeekBar sb=(SeekBar)findViewById(R.id.speed_T);
    sb.setOnSeekBarChangeListener(new throttle_listener("T"));
    
    sb=(SeekBar)findViewById(R.id.speed_S);
    sb.setOnSeekBarChangeListener(new throttle_listener("S"));

  } //end of onCreate()

  //set up text label and dcc function for each button from settings and setup listeners
  //TODO: unduplicate this code (in function_settings.java and engine_driver.java)
  private void set_function_buttons() {

	  aLbl = new ArrayList<String>();
	  aFnc = new ArrayList<Integer>();

	  try	  {
		  File sdcard_path=Environment.getExternalStorageDirectory();
		  //		  if(sdcard_path.canWrite()) {  //TODO: handle false better

		  //First, determine if the engine_driver directory exists. If not, create it.
		  //			  File engine_driver_dir=new File(sdcard_path, "engine_driver");
		  //			  if(!engine_driver_dir.exists()) { engine_driver_dir.mkdir(); }

		  //			  if(engine_driver_dir.exists() && engine_driver_dir.isDirectory())  {

		  File settings_file=new File(sdcard_path + "/engine_driver/function_settings.txt");
		  if(settings_file.exists()) {  //if file found, use it for settings arrays
			  BufferedReader settings_reader=new BufferedReader(new FileReader(settings_file));
			  //read settings into local arrays
			  while(settings_reader.ready()) {
				  String line=settings_reader.readLine();
				  String temp[] = line.split(":");
				  aLbl.add(temp[0]);
				  aFnc.add(Integer.parseInt(temp[1]));
			  }
		  } else {  //hard-code some buttons and default the rest
			  aLbl.add("Light");aFnc.add(0); //aTgl.add(true);
			  aLbl.add("Bell"); aFnc.add(1); //aTgl.add(true);
			  aLbl.add("Horn"); aFnc.add(2); //aTgl.add(false);
			  for(int k = 3; k <= 27; k++) {
				  aLbl.add(String.format("%d",k));
				  aFnc.add(k);
			  }
		  }
	  }
	  catch (IOException except) { Log.e("settings_activity", "Could not read file "+except.getMessage()); }

	  // loop through all function buttons and
	  //   set label and dcc functions (based on settings) or hide if no label

	  ViewGroup tv = (ViewGroup) findViewById(R.id.function_buttons_table_T); //table
	  set_function_buttons_for_view(tv, "T");
	  tv = (ViewGroup) findViewById(R.id.function_buttons_table_S); //table
	  set_function_buttons_for_view(tv, "S");

  }

  //helper function to set up function buttons for each throttle
  void set_function_buttons_for_view(ViewGroup t, String whichLoco)  {
	  ViewGroup r;  //row
	  function_button_touch_listener fbtl;
	  Button b; //button
	  int k = 0; //button count
	  for(int i = 0; i < t.getChildCount(); i++) {
	      r = (ViewGroup)t.getChildAt(i);
	      for(int j = 0; j < r.getChildCount(); j++) {
	      	b = (Button)r.getChildAt(j);
	    		if (k < aFnc.size()) {
		       		fbtl=new function_button_touch_listener(aFnc.get(k), whichLoco);
		       		b.setOnTouchListener(fbtl);
		       		String bt = aLbl.get(k) + "        ";  //pad with spaces, and limit to 7 characters
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

    int throttle_count = 0;
   	int height_T;
	int height_S;
  
    Button b=(Button)findViewById(R.id.button_select_loco_T);
    if (mainapp.loco_string_T.equals("Not Set")) {
        b.setText("Press to select");
    } else {
    	b.setText(mainapp.loco_string_T);
    	throttle_count++;
    }
 
    b=(Button)findViewById(R.id.button_select_loco_S);
    if (mainapp.loco_string_S.equals("Not Set")) {
        b.setText("Press to select");
    } else {
      	b.setText(mainapp.loco_string_S);
    	throttle_count++;
    }

    // set up max speeds for throttles
    String s = prefs.getString("maximum_throttle_preference", getApplicationContext().getResources().getString(R.string.prefMaximumThrottleDefaultValue));
    int maxThrottle = Integer.parseInt(s);
    maxThrottle =(int) ((double) (maxThrottle/99.0) * 126.0);
    SeekBar sb=(SeekBar)findViewById(R.id.speed_T);
    sb.setMax(maxThrottle);
    sb=(SeekBar)findViewById(R.id.speed_S);
    sb.setMax(maxThrottle);

    
    //update the state of each function button based on shared variable
    set_function_states("T");
    set_function_states("S");
     
//  int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
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
	    LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(
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
}

  //send heartbeat to withrottle to keep this throttle alive 
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
      switch (item.getItemId()) {
      case R.id.settings_menu:
    	  Intent settings=new Intent().setClass(this, function_settings.class);
    	  startActivityForResult(settings, 0);
    	  break;
      case R.id.about_menu:
    	  Intent about_page=new Intent().setClass(this, about_page.class);
    	  startActivity(about_page);
    	  break;
      case R.id.preferences:
    	  Intent preferences=new Intent().setClass(this, preferences.class);
    	    startActivityForResult(preferences, 0);
    	  break;
      case R.id.turnouts:
    	  Intent turnouts=new Intent().setClass(this, turnouts.class);
    	    startActivity(turnouts);
    	  break;
      case R.id.power_control_menu:
    	  Intent in = new Intent().setClass(this, power_control.class);
    	    startActivity(in);
    	  break;
      case R.id.routes:
    	  in = new Intent().setClass(this, routes.class);
    	    startActivity(in);
    	  break;
      }
      return super.onOptionsItemSelected(item);
  }
  //handle return from menu items
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      //since we always do the same action no need to distinguish between requests
      set_function_buttons();
//	  set_labels();
	  enable_disable_buttons("T");  //TODO: this is executed twice when loco is selected
	  enable_disable_buttons("S");  
  }

}