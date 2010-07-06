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
import android.os.Bundle;
import android.os.Handler;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.os.Message;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.HashMap;

import android.widget.SimpleAdapter;
import android.widget.ListView;
import java.io.File;
import android.view.View;
import android.view.View.OnKeyListener;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.IOException;
import android.util.Log;
import java.io.FileReader;
import android.widget.EditText;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.AdapterView;
import java.io.PrintWriter;

public class select_loco extends Activity
{

  ArrayList<HashMap<String, String> > engine_list;
  private SimpleAdapter list_adapter;

  ArrayList<Integer> engine_address_list;
  ArrayList<Integer> address_size_list; //Look at address_type.java

  int engine_address;
  int address_size;
  private String whichThrottle; //"T" or "S" to distinguish which throttle we're asking for

  private threaded_application mainapp;  // hold pointer to mainapp

  //lookup and set values of various text labels 
  private void set_labels() {

	 //format and show currently selected locos, and hide or show Release buttons
	TextView v=(TextView)findViewById(R.id.sl_loco_T);
	Button b=(Button)findViewById(R.id.sl_release_T);
    v.setText(mainapp.loco_string_T);
    if (mainapp.loco_string_T.equals("Not Set")) {
    	b.setEnabled(false);
    } else {
    	b.setEnabled(true);
    }
	v=(TextView)findViewById(R.id.sl_loco_S);
	b=(Button)findViewById(R.id.sl_release_S);
    v.setText(mainapp.loco_string_S);
    if (mainapp.loco_string_S.equals("Not Set")) {
    	b.setEnabled(false);
    } else {
    	b.setEnabled(true);
    }
    
    //format and show footer info
	SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
    v=(TextView)findViewById(R.id.sl_footer);
  
    String s = "Throttle Name: " + prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
//    s += "\nt=" +  java.util.Arrays.toString(mainapp.function_states_T);
//    s += "\ns=" +  java.util.Arrays.toString(mainapp.function_states_S);
    s += "\nHost: " + mainapp.host_name_string +"  System Power: " + mainapp.power_state;
    s += "\nWiThrottle: v" + mainapp.withrottle_version_string;
    s += String.format("     Heartbeat: %d secs", mainapp.heartbeat_interval);
//    s += "\nRoster: " + mainapp.roster_list_string;
    v.setText(s);
    
  }
 
  //Handle messages from the communication thread back to this thread (responses from withrottle)
  class select_loco_handler extends Handler {

	public void handleMessage(Message msg) {
      switch(msg.what) {
      case message_type.RESPONSE: {
          set_labels();
        }
        break;
/*      case message_type.LOCO_SELECTED: {      	    //Start the throttle activity.
  		  Intent engine_driver=new Intent().setClass(getApplicationContext(), engine_driver.class);
  	      startActivityForResult(engine_driver, 0);
      	}
        break;
*/        
      case message_type.END_ACTIVITY: {      	    //Program shutdown has been requested
    	  end_this_activity();
    	}
        break;
    };
	}
  }

  //end current activity
  void end_this_activity() {
	  mainapp.select_loco_msg_handler = null; //remove pointer to this activity's handler
	  this.finish();
  }

  //handle return from engine_drive activity
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      //since we always do the same action no need to distinguish between requests
	  set_labels();
  }
  
  //request release of specified loco
  void release_loco(String whichThrottle)  {
    Message msg=Message.obtain();
    msg.what=message_type.RELEASE;
    msg.obj=new String(whichThrottle);  //pass T or S in message
    mainapp.comm_msg_handler.sendMessage(msg);
  }
  
  void acquire_engine()
  {
    Message acquire_msg=Message.obtain();
    acquire_msg.what=message_type.LOCO_ADDR;
    acquire_msg.arg1=engine_address;
    acquire_msg.arg2=address_size;
    acquire_msg.obj=new String(whichThrottle);  //pass T or S in message
    mainapp.comm_msg_handler.sendMessage(acquire_msg);

    //Save the engine list to the engine_list.txt file.
    File sdcard_path=Environment.getExternalStorageDirectory();
    File connections_list_file=new File(sdcard_path, "engine_driver/engine_list.txt");
    PrintWriter list_output;
    try
    {
      list_output=new PrintWriter(connections_list_file);
      //Add this connection to the head of connections list.
      list_output.format("%d:%d\n", engine_address, address_size);
      for(int i=0; i<engine_address_list.size(); i+=1)
      {
        if(engine_address==engine_address_list.get(i) && address_size==address_size_list.get(i)) { continue; }
        list_output.format("%d:%d\n", engine_address_list.get(i), address_size_list.get(i));
      }
      list_output.flush();
      list_output.close();
    }
    catch(IOException except)
    {
      Log.e("connection_activity", "Error creating a PrintWriter, IOException: "+except.getMessage());
    }
  };

  public class button_listener implements View.OnClickListener
  {
    public void onClick(View v)
    {
      EditText entry=(EditText)findViewById(R.id.loco_address);
      engine_address=Integer.decode(entry.getText().toString());
      Spinner spinner=(Spinner)findViewById(R.id.address_length);
      address_size=spinner.getSelectedItemPosition();
      acquire_engine();
      end_this_activity();
    };
  }

  public class release_button_listener_T implements View.OnClickListener
  {
    public void onClick(View v)    {
      release_loco("T");
      end_this_activity();
    };
  }
  public class release_button_listener_S implements View.OnClickListener
  {
    public void onClick(View v)    {
      release_loco("S");
      end_this_activity();
    };
  }

  public class engine_item implements AdapterView.OnItemClickListener
  {
    //When an item is clicked, acquire that engine.
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {
      engine_address=engine_address_list.get(position);
      address_size=address_size_list.get(position);
      acquire_engine();
      end_this_activity();
    };
  }

  //Handle pressing of the back button to simply return to caller 
  @Override
  public boolean onKeyDown(int key, KeyEvent event)
  {
    if(key==KeyEvent.KEYCODE_BACK)
    {
    	end_this_activity();
    }
    return(super.onKeyDown(key, event));
  };

  @Override
  public void onStart() {

	super.onStart();
    Bundle extras = getIntent().getExtras(); 
    if(extras !=null)    {
      whichThrottle = extras.getString("whichThrottle");
    }

 }
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.select_loco);

   //save pointer to main app
    mainapp=(threaded_application)getApplication();
    //put pointer to this activity's handler in main app's shared variable
    mainapp.select_loco_msg_handler=new select_loco_handler();
    
    //Set the options for the address length.
    Spinner address_spinner=(Spinner)findViewById(R.id.address_length);
    ArrayAdapter<?> spinner_adapter=ArrayAdapter.createFromResource(this, R.array.address_size,
                                                                 android.R.layout.simple_spinner_item);
    spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    address_spinner.setAdapter(spinner_adapter);
    //Set up a list adapter to allow adding the list of recent engines to the UI.
    engine_list=new ArrayList<HashMap<String, String> >();
    list_adapter=new SimpleAdapter(this, engine_list, R.layout.engine_list_item, new String[] {"engine"},
                                   new int[] {R.id.engine_item_label});
    ListView engine_list_view=(ListView)findViewById(R.id.engine_list);
    engine_list_view.setAdapter(list_adapter);
    engine_list_view.setOnItemClickListener(new engine_item());

    engine_address_list=new ArrayList<Integer>();
    address_size_list=new ArrayList<Integer>();
    //Populate the ListView with the recent engines saved in a file. This will be stored in
    // /sdcard/engine_driver/engine_list.txt
    try
    {
      File sdcard_path=Environment.getExternalStorageDirectory();
      if(sdcard_path.canWrite())
      {
        File engine_driver_dir=new File(sdcard_path, "engine_driver");
        //The engine_driver directory should already exist. The connection activity should have already created it.
        if(engine_driver_dir.exists() && engine_driver_dir.isDirectory())
        {
          //TODO: Fix things if the path is not a directory.
          File engine_list_file=new File(engine_driver_dir, "engine_list.txt");
          if(engine_list_file.exists())
          {
            BufferedReader list_reader=new BufferedReader(new FileReader(engine_list_file));
            while(list_reader.ready())
            {
              String line=list_reader.readLine();
              engine_address_list.add(Integer.decode(line.substring(0, line.indexOf(':'))));
              address_size_list.add(Integer.decode(line.substring(line.indexOf(':')+1, line.length())));
              HashMap<String, String> hm=new HashMap<String, String>();
              hm.put("engine", engine_address_list.get(engine_address_list.size()-1).toString());
              engine_list.add(hm);
            }
            list_adapter.notifyDataSetChanged();
          }
        }
      }
    }
    catch (IOException except) { Log.e("connection_activity", "Could not read file "+except.getMessage()); }

    //Set the button callbacks.
    Button button=(Button)findViewById(R.id.acquire);
    button_listener click_listener=new button_listener();
    button.setOnClickListener(click_listener);

    button=(Button)findViewById(R.id.sl_release_T);
    button.setOnClickListener(new release_button_listener_T());
    
    button=(Button)findViewById(R.id.sl_release_S);
    button.setOnClickListener(new release_button_listener_S());
    
    
    //set long/short based on length of text entered (but user can override if needed)
    EditText la = (EditText)findViewById(R.id.loco_address);
    la.setOnKeyListener(new OnKeyListener()  { 
        public boolean onKey(View v, int keyCode, KeyEvent event)  { 
            Button ba=(Button)findViewById(R.id.acquire);
            EditText la = (EditText)findViewById(R.id.loco_address);
    	    Spinner al=(Spinner)findViewById(R.id.address_length);

    	    //don't allow acquire button if nothing entered
    	    if (la.getText().toString().length() > 0) {
           	  ba.setEnabled(true);
          	} else {
          	  ba.setEnabled(false);
          	}
    	    
    	    //auto-set address length
        	if (la.getText().toString().length() > 2) {
          	  al.setSelection(1);
        	} else {
      	      al.setSelection(0);
        	}
            return false;
        };
    });
                
    set_labels();

  };


}
