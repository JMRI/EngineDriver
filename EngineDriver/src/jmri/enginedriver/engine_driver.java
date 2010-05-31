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
/* version 0.3 by mstevetodd - added function labels, separate fwd-stop-rev buttons
 */
/*TODO: show loco number on throttle screen
 * 
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;

//import jmri.enginedriver.connection_activity.ui_handler;

import android.util.Log;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;
import android.view.MotionEvent;
import android.os.Message;
import android.widget.TextView;
import android.graphics.drawable.Drawable;

public class engine_driver extends Activity {

  private threaded_application mainapp;  // hold pointer to mainapp
  private static final int GONE = 8;
  private static final int VISIBLE = 0;
  private Timer heartbeatTimer;

  private Drawable button_pressed_drawable;  //hold background graphics for buttons
  private Drawable button_normal_drawable;
  private Drawable button_pressed_small_drawable;  //needed different buttons for wide vs. not-wide
  private Drawable button_normal_small_drawable;

  //Handle messages from the communication thread TO this thread (responses from withrottle)
  class engine_driver_handler extends Handler {

	public void handleMessage(Message msg) {
	      switch(msg.what) {
	        
	        case message_type.RESPONSE: {  //handle future messages, such as changes to functions
	        	// refresh text labels
	        	set_labels();
//	        	String response_str = msg.obj.toString();
//	        	Toast.makeText(getApplicationContext(), "responsed with:" + response_str, Toast.LENGTH_SHORT).show();
	        }
	        break;
	        case message_type.HEARTBEAT: {
	        	// refresh text labels
	        	set_labels();
//	        	Toast.makeText(getApplicationContext(), "refreshed", Toast.LENGTH_SHORT).show();
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
  
  public class function_button_touch_listener implements View.OnTouchListener
  {
    int function;
    boolean is_toggle_type; //True if the button is a toggle on/toggle off type (for example the head light).
    boolean toggled;

    public function_button_touch_listener(int new_function, boolean new_toggle_type)
    {
      function=new_function;
      is_toggle_type=new_toggle_type;
    }

    public boolean onTouch(View v, MotionEvent event)
    {
      switch(event.getAction())
      {
        case MotionEvent.ACTION_DOWN:
        {
          Message function_msg=Message.obtain();
          if(is_toggle_type) {
            toggled=!toggled; //The toggle/latch functionality is taken care of by the WiThrottle server. This might not be useful in certain
                              //cases, but for now I'll let it stand.
          }
          switch (function) {
            case function_button.FORWARD : {
            	function_msg.what=message_type.DIRECTION;
                function_msg.arg1=1;
            	function_msg.arg2=1;  //forward is 1
            	//show pressed image, and turn off pressed image on other button 
                v.setBackgroundDrawable(button_pressed_drawable);
                Button b = (Button)findViewById(R.id.button_rev);
            	b.setBackgroundDrawable(button_normal_drawable);
            }
              break;
            case function_button.REVERSE : {
            	function_msg.what=message_type.DIRECTION;
                function_msg.arg1=1;
            	function_msg.arg2=0;  //reverse is 0
            	//show pressed image, and turn off pressed image on other button 
            	v.setBackgroundDrawable(button_pressed_drawable);
                Button b = (Button)findViewById(R.id.button_fwd);
            	b.setBackgroundDrawable(button_normal_drawable);
            }
              break;
            // setting the throttle slide to 0 sends a velocity change to the withrottle          	  
            case function_button.STOP : {
              SeekBar sb=(SeekBar)findViewById(R.id.speed);
              sb.setProgress(0);
        	  break;
            }
            default : {
              function_msg.what=message_type.FUNCTION;
              function_msg.arg1=function;
              function_msg.arg2=1;
            }
          }
          
          mainapp.comm_msg_handler.sendMessage(function_msg);

          //Change the appearance of toggleable buttons to show the current function state.
          if(is_toggle_type) {
            if(toggled) {
              v.setBackgroundDrawable(button_pressed_small_drawable);
              v.setPadding(2, 2, 2, 2);  //not sure why, but padding gets lost and has to be reset
            } else {
              v.setBackgroundDrawable(button_normal_small_drawable);
              v.setPadding(2, 2, 2, 2);
            }
          }
        }
        break;
        //handle stopping of function on key-up 
        case MotionEvent.ACTION_UP:
          // only process UP for function buttons
          if(function < function_button.FORWARD)
          {
            Message function_msg=Message.obtain();
            function_msg.what=message_type.FUNCTION;
            function_msg.arg1=function;
            function_msg.arg2=0;
            mainapp.comm_msg_handler.sendMessage(function_msg);
          }
        break;
      }
      return(false);
    };
  }

  public class throttle_listener implements SeekBar.OnSeekBarChangeListener
  {
    public void onProgressChanged(SeekBar throttle, int speed, boolean fromUser)
    {
      Message velocity_msg=Message.obtain();
      velocity_msg.what=message_type.VELOCITY;
      velocity_msg.arg1=speed;
      mainapp.comm_msg_handler.sendMessage(velocity_msg);
      TextView speed_label=(TextView)findViewById(R.id.speed_value_label);
      speed_label.setText(Integer.toString(speed));
    }

    public void onStartTrackingTouch(SeekBar sb) { }

    public void onStopTrackingTouch(SeekBar sb) { }
  }

  //Handle pressing of the back button to release the selected loco and end this activity
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
  if(key==KeyEvent.KEYCODE_BACK)
  {
    mainapp.engine_driver_msg_handler = null; //clear out pointer to this activity  
    
    Message release_msg=Message.obtain();
    release_msg.what=message_type.RELEASE;
//    threaded_application app=(threaded_application)this.getApplication();
    mainapp.comm_msg_handler.sendMessage(release_msg);
    
    //kill the heartbeat timer
	if (heartbeatTimer != null) {
  	    heartbeatTimer.cancel();
  	}
  }
  return(super.onKeyDown(key, event));
};


  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.throttle);

    mainapp=(threaded_application)getApplication();
    //put pointer to this activity's handler in main app's shared variable
    mainapp.engine_driver_msg_handler=new engine_driver_handler();

    button_pressed_drawable=getResources().getDrawable(R.drawable.btn_default_small_pressed);
    button_normal_drawable=getResources().getDrawable(R.drawable.btn_default_small_normal);
    button_pressed_small_drawable=getResources().getDrawable(R.drawable.btn_default_small_pressed);
    button_normal_small_drawable=getResources().getDrawable(R.drawable.btn_default_small_normal);

    set_function_buttons();

    // set listeners for 3 direction buttons
    Button b = (Button)findViewById(R.id.button_fwd);
    function_button_touch_listener fbtl=new function_button_touch_listener(function_button.FORWARD, false);
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_stop);
    fbtl=new function_button_touch_listener(function_button.STOP, false);
    b.setOnTouchListener(fbtl);
    b = (Button)findViewById(R.id.button_rev);
    fbtl=new function_button_touch_listener(function_button.REVERSE, false);
    b.setOnTouchListener(fbtl);
    
    SeekBar sb=(SeekBar)findViewById(R.id.speed);
    sb.setMax(126);
    sb.setOnSeekBarChangeListener(new throttle_listener());

    set_labels();

    if (mainapp.heartbeat_interval > 0) {
        int interval = (mainapp.heartbeat_interval - 1) * 900;  //set heartbeat one second less than required (900 copied from withrottle side)

        heartbeatTimer = new Timer();
    	heartbeatTimer.schedule(new TimerTask() {
    		@Override
    		public void run() {
    			send_heartbeat();
    		}

    	}, 100, interval);
    }

   
  } //end of onCreate()

  //set up label, dcc function, toggle setting for each button from settings and setup listeners TODO: allow refresh (need to deal with old listeners?) 
  //TODO: unduplicate this code (in settings.java and engine_driver.java)
  private void set_function_buttons() {
	    ArrayList<String> aLbl = new ArrayList<String>();
	    ArrayList<Integer> aFnc = new ArrayList<Integer>();
	    ArrayList<Boolean> aTgl = new ArrayList<Boolean>();

	    try
	    {
	      File sdcard_path=Environment.getExternalStorageDirectory();
	      if(sdcard_path.canWrite())
	      {
	        //First, determine if the engine_driver directory exists. If not, create it.
	        File engine_driver_dir=new File(sdcard_path, "engine_driver");
	        if(!engine_driver_dir.exists()) { engine_driver_dir.mkdir(); }

	        if(engine_driver_dir.exists() && engine_driver_dir.isDirectory())
	        {
	          //TODO: Fix things if the path is not a directory.
	          File settings_file=new File(engine_driver_dir, "function_settings.txt");
	          if(settings_file.exists()) {
	            BufferedReader settings_reader=new BufferedReader(new FileReader(settings_file));
	            //read settings into local arrays
	            while(settings_reader.ready()) {
	              String line=settings_reader.readLine();
	              String temp[] = line.split(":");
	              aLbl.add(temp[0]);
	              aFnc.add(Integer.parseInt(temp[1]));
	              aTgl.add(Boolean.parseBoolean(temp[2]));
	            }
	          } else {  //hard-code some buttons and default the rest
	              aLbl.add("Light");aFnc.add(0); aTgl.add(true);
	              aLbl.add("Bell"); aFnc.add(1); aTgl.add(true);
	              aLbl.add("Horn"); aFnc.add(2); aTgl.add(false);
	              for(int k = 3; k <= 15; k++) {
	                aLbl.add(String.format("%d",k));
	                aFnc.add(k);
	                aTgl.add(true);
	              }
	          }
	        }
	      }
	    }
	    catch (IOException except) { Log.e("settings_activity", "Could not read file "+except.getMessage()); }

	    // loop through all function buttons and
	    //   set label and dcc functions (based on settings) or hide if no label
	    function_button_touch_listener fbtl;
	    Button b; //button
	    
	    ViewGroup t = (ViewGroup) findViewById(R.id.function_buttons_group); //table
	    ViewGroup r;  //row
	    int k = 0; //button count
	    for(int i = 0; i < t.getChildCount(); i++) {
	        r = (ViewGroup)t.getChildAt(i);
	        for(int j = 0; j < r.getChildCount(); j++) {
	        	b = (Button)r.getChildAt(j);
	      		if (k < aFnc.size()) {
		       		fbtl=new function_button_touch_listener(aFnc.get(k), aTgl.get(k));
		       		b.setOnTouchListener(fbtl);
		            if (aTgl.get(k)) {        //if button is sticky, set background to "off" state
		              b.setBackgroundDrawable(button_normal_small_drawable);
		              b.setPadding(2, 2, 2, 2);
		            }
		       		String bt = aLbl.get(k) + "        ";  //pad with spaces, and limit to 8 characters
		       		b.setText(bt.substring(0, 7));
		       	    b.setVisibility(VISIBLE);
		       	} else {
		       	    b.setVisibility(GONE);
		       	}
	        	k++;
	        }
	    }

  }

  //lookup and set values of various informational text labels 
  private void set_labels() {

	String s;

	TextView v=(TextView)findViewById(R.id.throttle_header);
    s = mainapp.loco_string_1;
    v.setText("Loco: " + s);

    //format and show footer info
    SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
    v=(TextView)findViewById(R.id.ed_footer);

    s = "Throttle Name: " + prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
    s += "\nHost: " + mainapp.host_name_string;
    s += "\nWiThrottle: v" + mainapp.withrottle_version_string;
    s += String.format("     Heartbeat: %d secs", mainapp.heartbeat_interval);
    v.setText(s);

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
	  inflater.inflate(R.menu.menu, menu);
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
      }
      return super.onOptionsItemSelected(item);
  }
  //handle return from menu items
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      //since we always do the same action no need to distinguish between requests
      set_function_buttons();
	  set_labels();
  }

}