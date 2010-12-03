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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class routes extends Activity {

	private threaded_application mainapp;  // hold pointer to mainapp

	private SharedPreferences prefs;
	
	ArrayList<HashMap<String, String> > routes_list;
	private SimpleAdapter routes_list_adapter;

	  public class route_item implements AdapterView.OnItemClickListener	  {

		  //When a route  is clicked, extract systemname and send command to toggle it
	    public void onItemClick(AdapterView<?> parent, View v, int position, long id)	    {
	    	ViewGroup vg = (ViewGroup)v; //convert to viewgroup for clicked row
	    	ViewGroup rl = (ViewGroup) vg.getChildAt(0);  //get relativelayout
	    	TextView snv = (TextView) rl.getChildAt(1); // get systemname text from 2nd box
	    	String systemname = (String) snv.getText();
	        Message msg=Message.obtain();  
        	msg.what=message_type.ROUTE;
        	msg.arg1=2; // 2 = toggle        	
        	msg.arg2=0; // not used 
            msg.obj=new String(systemname);    // load system name for route into message
            mainapp.comm_msg_handler.sendMessage(msg);
	    };
	  }	  

	public void refresh_route_view() {
		
	    boolean hidesystemroutes = prefs.getBoolean("hide_system_route_names_preference", false);  //TODO fix getting from strings

		//clear and rebuild
		routes_list.clear();
		if (mainapp.rt_state_names != null) {  //not allowed
			if (mainapp.rt_user_names != null) { //none defined
				int pos = 0;
				for (String username : mainapp.rt_user_names) {
					if (!username.equals(""))  {  //skip routes without usernames
						//get values from global array
						String systemname = mainapp.rt_system_names[pos];
						String currentstate = mainapp.rt_states[pos];
						String currentstatedesc = mainapp.rt_state_names.get(currentstate);
						if (currentstatedesc == null) {
							currentstatedesc = "   ???";
						}

						//put values into temp hashmap
						HashMap<String, String> hm=new HashMap<String, String>();
						hm.put("rt_user_name", username);
						hm.put("rt_system_name_hidden", systemname);
						if (!hidesystemroutes) {  //check prefs for show or not show this
							hm.put("rt_system_name", systemname);
						}
						hm.put("rt_current_state_desc", currentstatedesc);

						//add temp hashmap to list which view is hooked to
						routes_list.add(hm);
						//
					}
					pos++;
				}  //if username blank
				routes_list_adapter.notifyDataSetChanged();
			}  //if usernames is null
			EditText te =(EditText)findViewById(R.id.route_entry);  // enable the buttons
			te.setEnabled(true);
			Button b =(Button)findViewById(R.id.route_toggle);
			b.setEnabled(true);
			b.setText(getString(R.string.set));
		}  else {
			EditText te =(EditText)findViewById(R.id.route_entry);
			te.setEnabled(false);
			Button b =(Button)findViewById(R.id.route_toggle);
			b.setEnabled(false);
			b.setText(getString(R.string.not_allowed));
		}  //end statenames  is null

	}

	  //Handle messages from the communication thread back to this thread (responses from withrottle)
	  class routes_handler extends Handler {

		public void handleMessage(Message msg) {
	      switch(msg.what) {
	      case message_type.RESPONSE: {
	        	String response_str = msg.obj.toString();
	        	if (response_str.substring(0,3).equals("PRA")) {  //refresh routes if any have changed
	        		refresh_route_view(); 
	        	}
	        }
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
		      EditText entryv=(EditText)findViewById(R.id.route_entry);
		      String entrytext = new String(entryv.getText().toString());
		      if (entrytext.trim().length() > 0 ) {
		        try {
		          Integer entryint=new Integer(entrytext);  //edit check address by attempting conversion to int
		        } catch(NumberFormatException except) { 
		       	    Toast.makeText(getApplicationContext(), "route # must be numeric, reenter.\n"+except.getMessage(), Toast.LENGTH_SHORT).show();
		         	return;
		        }
		        String systemname ="R" + entrytext;
		        Message msg=Message.obtain();  
	        	msg.what=message_type.ROUTE;
	        	msg.arg1=whichCommand;
	        	msg.arg2=0; // not used 
	            msg.obj=new String(systemname);    // load system name for route into message
	            mainapp.comm_msg_handler.sendMessage(msg);
	            entryv.setText(""); //clear the text after send
		      } else {
		    	    Toast.makeText(getApplicationContext(), "Enter a route # to set", Toast.LENGTH_SHORT).show();
		      }
		    };
	  }

  @Override
  public void onStart() {
    super.onStart();

    //put pointer to this activity's handler in main app's shared variable (If needed)
    if (mainapp.routes_msg_handler == null){
  	  mainapp.routes_msg_handler=new routes_handler();
    }

    //update route list
    refresh_route_view();
  }

	  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.routes);
    
    mainapp=(threaded_application)getApplication();
    
	prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
    
	//Set up a list adapter to allow adding the list of recent connections to the UI.
    routes_list=new ArrayList<HashMap<String, String> >();
    routes_list_adapter=new SimpleAdapter(this, routes_list, R.layout.routes_item, 
    		new String[] {"rt_user_name", "rt_system_name_hidden", "rt_system_name", "rt_current_state_desc"},
            new int[] {R.id.rt_user_name, R.id.rt_system_name_hidden, R.id.rt_system_name, R.id.rt_current_state_desc});
    ListView routes_lv=(ListView)findViewById(R.id.routes_list);
    routes_lv.setAdapter(routes_list_adapter);
    routes_lv.setOnItemClickListener(new route_item());

    // suppress popup keyboard until EditText is touched
	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    //Set the button callbacks, storing the command to pass for each
    Button b=(Button)findViewById(R.id.route_toggle);
    button_listener click_listener=new button_listener(2);
    b.setOnClickListener(click_listener);

  };

  //Handle pressing of the back button to end this activity
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
  if(key==KeyEvent.KEYCODE_BACK)
  {
    mainapp.routes_msg_handler = null; //clear out pointer to this activity  
    this.finish();  //end this activity
  }
  return(super.onKeyDown(key, event));
};

  
}
