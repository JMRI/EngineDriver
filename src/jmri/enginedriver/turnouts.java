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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class turnouts extends Activity implements OnGestureListener {

	private threaded_application mainapp;  // hold pointer to mainapp
	
	private SharedPreferences prefs;
	
	private static final int GONE = 8;
//	private static final int VISIBLE = 0;

	ArrayList<HashMap<String, String> > turnouts_list;
	private SimpleAdapter turnouts_list_adapter;

	private GestureDetector myGesture ;


	  public class turnout_item implements AdapterView.OnItemClickListener	  {

		  //When a turnout  is clicked, extract systemname and send command to toggle it
	    public void onItemClick(AdapterView<?> parent, View v, int position, long id)	    {
	    	ViewGroup vg = (ViewGroup)v; //convert to viewgroup for clicked row
	    	ViewGroup rl = (ViewGroup) vg.getChildAt(0);  //get relativelayout
	    	TextView snv = (TextView) rl.getChildAt(1); // get systemname text from 2nd box
	    	String systemname = (String) snv.getText();
	        Message msg=Message.obtain();  
        	msg.what=message_type.TURNOUT;
        	msg.arg1=2; // 2 = toggle        	
        	msg.arg2=0; // not used 
            msg.obj=new String(systemname);    // load system name for turnout into message
            mainapp.comm_msg_handler.sendMessage(msg);
	    };
	  }	  

	public void refresh_turnout_view() {
		
		//specify logic for sort comparison (by username)
	    Comparator<HashMap<String, String>> route_comparator = new Comparator<HashMap<String, String>>() {
			@Override
			public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
				return arg0.get("to_user_name").compareToIgnoreCase(arg1.get("to_user_name"));
			}
		};

		//show selected hardware system
	    String hs = prefs.getString("hardware_system", getApplicationContext().getResources().getString(R.string.prefHardwareSystemDefaultValue));
		TextView hstv =(TextView)findViewById(R.id.hardware_system);
		hstv.setText(hs);
	
		//clear and rebuild, or disable if not allowed
		turnouts_list.clear();
		if (mainapp.to_state_names != null) {  //not allowed

			if (mainapp.to_user_names != null) { //none defined
				int pos = 0;
				for (String username : mainapp.to_user_names) {
					if (!username.equals(""))  {  //skip turnouts without usernames
						//get values from global array
						String systemname = mainapp.to_system_names[pos];
						String currentstate = mainapp.to_states[pos];
						String currentstatedesc = mainapp.to_state_names.get(currentstate);
						if (currentstatedesc == null) {
							currentstatedesc = "   ???";
						}

						//put values into temp hashmap
						HashMap<String, String> hm=new HashMap<String, String>();
						hm.put("to_user_name", username);
						hm.put("to_system_name", systemname);
						hm.put("to_current_state_desc", currentstatedesc);

						//add temp hashmap to list which view is hooked to
						turnouts_list.add(hm);

					}  //if username blank
					pos++;
				}  //end for loop
			}  //if usernames is null
			EditText te =(EditText)findViewById(R.id.turnout_entry);  // enable the buttons
			te.setEnabled(true);
			Button b =(Button)findViewById(R.id.turnout_throw);
			b.setEnabled(true);
			b =(Button)findViewById(R.id.turnout_close);
			b.setEnabled(true);
			b =(Button)findViewById(R.id.turnout_toggle);
			b.setEnabled(true);
			b.setText(getString(R.string.toggle_button));
		}  else {
			EditText te =(EditText)findViewById(R.id.turnout_entry);
			te.setEnabled(false);
			Button b =(Button)findViewById(R.id.turnout_throw);
			b.setEnabled(false);
			b =(Button)findViewById(R.id.turnout_close);
			b.setEnabled(false);
			b =(Button)findViewById(R.id.turnout_toggle);
			b.setEnabled(false);
			b.setText(getString(R.string.not_allowed));

		}  //end statenames is null
		
		//sort by username
		Collections.sort(turnouts_list, route_comparator);

		turnouts_list_adapter.notifyDataSetChanged();  //update the list
	}
	  
	  //Handle messages from the communication thread back to this thread (responses from withrottle)
	  class turnouts_handler extends Handler {

		public void handleMessage(Message msg) {
			switch(msg.what) {
	      		case message_type.RESPONSE:
	      			String response_str = msg.obj.toString();
	      			if (response_str.length() >= 3) {
	      				String com1 = response_str.substring(0,3);
	      			    //refresh turnouts if any have changed or if turnout list has changed
	      				if ("PTA".equals(com1) || "PTL".equals(com1)) {
	      					refresh_turnout_view(); 
	      				}
	      			}
	      			break;
	  		  	case message_type.DISCONNECT:
	  		  		disconnect();
	  		  		break;
			};
		}
	  }

	  
	  public class button_listener implements View.OnClickListener  {
		  Integer whichCommand; //command to send for button instance 'C'lose, 'T'hrow or '2' for toggle
		  
		  public button_listener(Integer new_command) {
			  whichCommand = new_command;
		  }
		  
		    public void onClick(View v) {
		      EditText entryv=(EditText)findViewById(R.id.turnout_entry);
		      String entrytext = new String(entryv.getText().toString());
		      if (entrytext.trim().length() > 0 ) {
		        try {
		          new Integer(entrytext);  //edit check address by attempting conversion to int
		        } 
		        catch(Exception except) { 
		       	    Toast.makeText(getApplicationContext(), "Turnout # must be numeric, reenter.\n"+except.getMessage(), Toast.LENGTH_SHORT).show();
		         	return;
		        }
		        //use preference for system name in command string
		        String hs = prefs.getString("hardware_system", getApplicationContext().getResources().getString(R.string.prefHardwareSystemDefaultValue));
		        String systemname = hs + "T" + entrytext;
		        
		        Message msg=Message.obtain();  
	        	msg.what=message_type.TURNOUT;
	        	msg.arg1=whichCommand;
	        	msg.arg2=0; // not used 
	            msg.obj=new String(systemname);    // load system name for turnout into message
	            mainapp.comm_msg_handler.sendMessage(msg);
//	            entryv.setText(""); //clear the text after send
		      } 
		      else {
		    	    Toast.makeText(getApplicationContext(), "Enter a turnout # to control", Toast.LENGTH_SHORT).show();
		      }
		    };
	  }

  @Override
  public boolean onTouchEvent(MotionEvent event){
  	return myGesture.onTouchEvent(event);
  }

  @Override
  public void onResume() {
	  super.onResume();
    //update turnout list
    refresh_turnout_view();
  }

  @Override
  public void onStart() {
    super.onStart();

    //put pointer to this activity's handler in main app's shared variable (If needed)
//    if (mainapp.turnouts_msg_handler == null)
  	  mainapp.turnouts_msg_handler=new turnouts_handler();
  }
  
  /** Called when the activity is finished. */
  @Override
  public void onDestroy() {
	  super.onDestroy();
  	  mainapp.turnouts_msg_handler = null;
  }
  
	  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.turnouts);
    
    mainapp=(threaded_application)getApplication();
    
	prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

    myGesture = new GestureDetector(this);
    
    //Set up a list adapter to allow adding the list of recent connections to the UI.
    turnouts_list=new ArrayList<HashMap<String, String> >();
    turnouts_list_adapter=new SimpleAdapter(this, turnouts_list, R.layout.turnouts_item, 
    		new String[] {"to_user_name", "to_system_name", "to_current_state_desc"},
            new int[] {R.id.to_user_name, R.id.to_system_name, R.id.to_current_state_desc});
    ListView turnouts_lv=(ListView)findViewById(R.id.turnouts_list);
    turnouts_lv.setAdapter(turnouts_list_adapter);
    turnouts_lv.setOnItemClickListener(new turnout_item());
    
    OnTouchListener gestureListener = new ListView.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (myGesture.onTouchEvent(event)) {
                return true;
            }
            return false;
        }
    };
    turnouts_lv.setOnTouchListener(gestureListener);

    // suppress popup keyboard until EditText is touched
	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    
    //Set the button callbacks, storing the command to pass for each
    Button b=(Button)findViewById(R.id.turnout_toggle);
    button_listener click_listener=new button_listener(2);
    b.setOnClickListener(click_listener);

    //don't show throw and close buttons if withrottle version < 1.6
    if (mainapp.withrottle_version >= 1.6) {
    	b=(Button)findViewById(R.id.turnout_close);
    	click_listener=new button_listener(8);
    	b.setOnClickListener(click_listener);

    	b=(Button)findViewById(R.id.turnout_throw);
    	click_listener=new button_listener(9);
    	b.setOnClickListener(click_listener);

    } else {
    	//hide the buttons
    	b=(Button)findViewById(R.id.turnout_close);
   	    b.setVisibility(GONE);

    	b=(Button)findViewById(R.id.turnout_throw);
   	    b.setVisibility(GONE);
    	
    }
    
  };

    
  //Always go to throttle activity if back button pressed
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
	  if(key==KeyEvent.KEYCODE_BACK)  {
		  this.finish();
	   	  connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
		  return true;
	  }
	  return(super.onKeyDown(key, event));
	}

  @Override
	public boolean onDown(MotionEvent e) {
		return false;
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		  if((Math.abs(e2.getX() - e1.getX()) > threaded_application.min_fling_distance) && 
			 (Math.abs(velocityX) > threaded_application.min_fling_velocity)) {
			  // left to right swipe goes to routes
			  if(e2.getX() > e1.getX()) {
				  Intent in=new Intent().setClass(this, routes.class);
				  startActivity(in);
				  connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);				
			   	  this.finish();  //don't keep on return stack
			  }
			  // right to left swipe goes to throttle
			  else {
			   	  this.finish();  //don't keep on return stack
			   	  connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			  }
			  return true;
	    }
		return false;
	}
	
	@Override
	public void onLongPress(MotionEvent e) {
	}
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}
	@Override
	public void onShowPress(MotionEvent e) {
	}
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		  MenuInflater inflater = getMenuInflater();
		  inflater.inflate(R.menu.turnouts_menu, menu);
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
	    case R.id.web_menu:
	  	  in=new Intent().setClass(this, web_activity.class);
	   	  startActivity(in);
	   	  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
	  	  break;
	    case R.id.power_control_menu:
	  	  in=new Intent().setClass(this, power_control.class);
	   	  startActivity(in);
	   	  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
	  	  break;
	    case R.id.throttle:
	      this.finish();
	   	  connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
	  	  break;
	    case R.id.routes:
	  	  in = new Intent().setClass(this, routes.class);
	   	  startActivity(in);
	      this.finish();
	   	  connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
	   	  break;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	private void disconnect() {
		this.finish();
		connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
	}
	  
}
