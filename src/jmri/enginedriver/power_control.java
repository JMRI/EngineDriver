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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class power_control extends Activity {

	private threaded_application mainapp;  // hold pointer to mainapp
		  
	  //Handle messages from the communication thread back to this thread (responses from withrottle)
	  class power_control_handler extends Handler {

		public void handleMessage(Message msg) {
	      switch(msg.what) {
	        case message_type.RESPONSE: {
	      	    String response_str = msg.obj.toString();
	        	if (response_str.substring(0,3).equals("PPA")) {  //refresh power state 
	        		refresh_power_control_view(); 
	        	}
	        }
	        break;
	    };
		}
	  }
	  public class button_listener implements View.OnClickListener	  {
		  
	    public void onClick(View v) {
	        Message msg=Message.obtain();  
        	msg.what=message_type.POWER_CONTROL;
        	int newState = 1;
        	if (mainapp.power_state.equals("1")) { //toggle to opposite value 0=off, 1=on
        		newState = 0;
        	}
        	msg.arg1=newState;
        	msg.arg2=0; // not used 
            mainapp.comm_msg_handler.sendMessage(msg);
	    };
	  }
	  
	    //Set the button text based on current power state  TODO: improve code 
		public void refresh_power_control_view() {
		    Button b=(Button)findViewById(R.id.power_control_button);
		    String currentState = "???";
		    if (mainapp.power_state == null) {
		    	currentState = "Not Allowed";  //must be turned on in Withrottle settings
			    b.setEnabled(false);
		    } else if (mainapp.power_state.equals("1")) {
		    	currentState = "On";
		    } else if (mainapp.power_state.equals("0")) {
		    	currentState = "Off";
		    }		    	
		    b.setText(currentState);
		}

  @Override
  public void onStart() {
    super.onStart();

    //put pointer to this activity's handler in main app's shared variable (If needed)
    if (mainapp.power_control_msg_handler == null){
  	  mainapp.power_control_msg_handler=new power_control_handler();
    }

    refresh_power_control_view();
  }

	  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.power_control);
    
    mainapp=(threaded_application)getApplication();
    
    //Set the button callbacks, storing the command to pass for each
    Button b=(Button)findViewById(R.id.power_control_button);
    button_listener click_listener=new button_listener();
    b.setOnClickListener(click_listener);

  };

  //Handle pressing of the back button to end this activity
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
  if(key==KeyEvent.KEYCODE_BACK)
  {
    mainapp.power_control_msg_handler = null; //clear out pointer to this activity  
    this.finish();  //end this activity
  }
  return(super.onKeyDown(key, event));
};

  
}
