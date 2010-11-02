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

import java.util.ArrayList;
import java.util.HashMap;

import android.widget.SimpleAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.File;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.IOException;
import android.util.Log;
import java.io.FileReader;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.os.Message;
import android.widget.EditText;
import android.widget.Button;
import android.os.Handler;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.AdapterView;

public class connection_activity extends Activity {
	
  ArrayList<HashMap<String, String> > connections_list;
  ArrayList<HashMap<String, String> > discovery_list;
  private SimpleAdapter connection_list_adapter;
  private SimpleAdapter discovery_list_adapter;

  ArrayList<String> ip_list;
  ArrayList<Integer> port_list;

  ArrayList<String> discovered_ip_list;
  ArrayList<Integer> discovered_port_list;

  //pointer back to application
  threaded_application mainapp;
  
  //The IP address and port that are used to connect.
  String connected_host;
  int connected_port;

  //Request connection to the WiThrottle server.
  void connect()
  {
    Message connect_msg=Message.obtain();
    connect_msg.what=message_type.CONNECT;
    connect_msg.arg1=connected_port;
    connect_msg.obj=new String(connected_host);
    if (mainapp.comm_msg_handler != null) {
    	mainapp.comm_msg_handler.sendMessage(connect_msg);
    } else {
   	    Toast.makeText(getApplicationContext(), "ERROR: comm thread not started.", Toast.LENGTH_SHORT).show();
    }    	
  };


  void start_select_loco_activity()
  {
//    multicast_lock.release();
    Intent select_loco=new Intent().setClass(this, select_loco.class);
    startActivity(select_loco);
  };

  void start_engine_driver_activity()
  {
	    // clear the discovered list
//	    discovered_ip_list.clear();
//	    discovered_port_list.clear();
//	    discovery_list.clear();

	    /*
	    //shutdown server discovery listener
	    Message msg=Message.obtain();
	    msg.what=message_type.SET_LISTENER;
	    msg.arg1 = 0; //zero turns it off
	    if (mainapp.comm_msg_handler != null) {
	    	mainapp.comm_msg_handler.sendMessage(msg);
	    } else {
	   	    Toast.makeText(getApplicationContext(), "ERROR: comm thread not started.", Toast.LENGTH_SHORT).show();
	    }    	
	    */

	    Intent engine_driver=new Intent().setClass(this, engine_driver.class);
	    startActivity(engine_driver);

  };
  
  public enum server_list_type { DISCOVERED_SERVER, RECENT_CONNECTION }
  public class connect_item implements AdapterView.OnItemClickListener
  {
    server_list_type server_type;

    connect_item(server_list_type new_type) { server_type=new_type; }

    //When an item is clicked, connect to the given IP address and port.
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {
      switch(server_type)
      {
        case DISCOVERED_SERVER:
          connected_host=new String(discovered_ip_list.get(position));
          connected_port=discovered_port_list.get(position);
        break;
        case RECENT_CONNECTION:
          connected_host=new String(ip_list.get(position));
          connected_port=port_list.get(position);
        break;
      }
      connect();
    };
  }

  public class button_listener implements View.OnClickListener
  {
    public void onClick(View v) {
      EditText entry=(EditText)findViewById(R.id.host_ip);
      connected_host=new String(entry.getText().toString());
      if (connected_host.trim().length() > 0 ) {
        entry=(EditText)findViewById(R.id.port);
        try {
          connected_port=new Integer(entry.getText().toString());
        } catch(NumberFormatException except) { 
       	    Toast.makeText(getApplicationContext(), "Invalid port#, retry.\n"+except.getMessage(), Toast.LENGTH_SHORT).show();
         	return;
        }
        connect();
      } else {
    	    Toast.makeText(getApplicationContext(), "Enter or select an address and port", Toast.LENGTH_SHORT).show();

      }
    };
  }

  //Handle messages from the communication thread back to the UI thread.
  class ui_handler extends Handler
  {
    public void handleMessage(Message msg)
    {
      switch(msg.what)
      {
        case message_type.SERVICE_RESOLVED:
          //stop if new address is already in the list   TODO: improve this lookup
        	String newaddr = new String((String)msg.obj);
        	for (String oldaddr : discovered_ip_list) {
        		if (oldaddr.equals(newaddr)) {
        			return;
        		}
        	}
            //Add this discovered service to the list.
        	discovered_ip_list.add(newaddr);
        	discovered_port_list.add(msg.arg1);

          HashMap<String, String> hm=new HashMap<String, String>();
          hm.put("ip_address", discovered_ip_list.get(discovered_ip_list.size()-1));
          hm.put("port", discovered_port_list.get(discovered_port_list.size()-1).toString());
          discovery_list.add(hm);
          discovery_list_adapter.notifyDataSetChanged();
        break;

        case message_type.SERVICE_REMOVED:        
        	//TODO: add this after removing arraylists
        break;

        case message_type.CONNECTED:
        	//Save the new connection list to the connections_list.txt file.
        	try  {
        		File sdcard_path=Environment.getExternalStorageDirectory();

        		//First, determine if the engine_driver directory exists. If not, create it.
        		File engine_driver_dir=new File(sdcard_path, "engine_driver");
        		if(!engine_driver_dir.exists()) { engine_driver_dir.mkdir(); }
        	
        		File connections_list_file=new File(sdcard_path, "engine_driver/connections_list.txt");
        		PrintWriter list_output;
        		list_output=new PrintWriter(connections_list_file);
        		//Add this connection to the head of connections list.
        		list_output.format("%s:%d\n", connected_host, connected_port);
        		for(int i=0; i<ip_list.size(); i+=1)
        		{
        			if(connected_host.equals(ip_list.get(i)) && port_list.get(i)==connected_port) { continue; }
        			list_output.format("%s:%d\n", ip_list.get(i), port_list.get(i));
        		}
        		list_output.flush();
        		list_output.close();
        	}
        	catch(IOException except)
        	{
        		Log.e("connection_activity", "Error saving recent connection: "+except.getMessage());
        		Toast.makeText(getApplicationContext(), "Error saving recent connection: "+except.getMessage(), Toast.LENGTH_SHORT).show();
        	}

          start_engine_driver_activity();
        break;

        case message_type.ERROR: {
          //display error message from msg.obj
          String msg_txt = new String((String)msg.obj);
      	  Toast.makeText(getApplicationContext(), msg_txt, Toast.LENGTH_SHORT).show();
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
	  mainapp.ui_msg_handler = null;
	  this.finish();
  }

	@Override
	public void onPause() {

		//shutdown server discovery listener
	    Message msg=Message.obtain();
	    msg.what=message_type.SET_LISTENER;
	    msg.arg1 = 0; //zero turns it off
	    if (mainapp.comm_msg_handler != null) {
	    	mainapp.comm_msg_handler.sendMessage(msg);
	    } else {
	   	    Toast.makeText(getApplicationContext(), "ERROR: comm thread not started.", Toast.LENGTH_SHORT).show();
	    }    	

	    // clear the discovered list  TODO: handle this better
	    discovered_ip_list.clear();
	    discovered_port_list.clear();
	    discovery_list.clear();
        discovery_list_adapter.notifyDataSetChanged();

        super.onPause();
}
	
	@Override
	public void onResume() {

		super.onResume();

		ip_list=new ArrayList<String>();
	    port_list=new ArrayList<Integer>();

	    connections_list.clear();
	    
	    //Populate the ListView with the recent connections saved in a file. This will be stored in
	    // /sdcard/engine_driver/connections_list.txt
	    try    {
	    	File sdcard_path=Environment.getExternalStorageDirectory();
	    	File connections_list_file=new File(sdcard_path, "engine_driver/connections_list.txt");

	    	if(connections_list_file.exists())    {
	    		BufferedReader list_reader=new BufferedReader(new FileReader(connections_list_file));
	    		while(list_reader.ready())    {
	    			String line=list_reader.readLine();
	    			String il = line.substring(0, line.indexOf(':'));
	    			Integer pl = Integer.decode(line.substring(line.indexOf(':')+1, line.length()));
	    			ip_list.add(il);
	    			port_list.add(pl);
	    			HashMap<String, String> hm=new HashMap<String, String>();
	    			hm.put("ip_address", il);
	    			hm.put("port", pl.toString());
	    			connections_list.add(hm);
	    		}
	    		connection_list_adapter.notifyDataSetChanged();
	    	}
	    }
	    catch (IOException except) { 
	    	Log.e("connection_activity", "Error reading recent connections list: "+except.getMessage());
			Toast.makeText(getApplicationContext(), "Error reading recent connections list: "+except.getMessage(), Toast.LENGTH_SHORT).show();
	    }
	    // clear the discovered list  TODO: handle this better
	    discovered_ip_list.clear();
	    discovered_port_list.clear();
	    discovery_list.clear();
        discovery_list_adapter.notifyDataSetChanged();
	    
	    //start up server discovery listener
	    Message msg=Message.obtain();
	    msg.what=message_type.SET_LISTENER;
	    msg.arg1 = 1; //one turns it on
	    if (mainapp.comm_msg_handler != null) {
	    	mainapp.comm_msg_handler.sendMessage(msg);
	    } else {
	   	    Toast.makeText(getApplicationContext(), "ERROR: comm thread not started.", Toast.LENGTH_SHORT).show();
	    }    	
	    
	    set_labels();

	   // withrottle_list();  //debugging
	    
	}  //end of onResume
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    
    mainapp=(threaded_application)this.getApplication();
    mainapp.ui_msg_handler=new ui_handler();

    setContentView(R.layout.connection);
    
    //Set up a list adapter to allow adding discovered WiThrottle servers to the UI.
    discovery_list=new ArrayList<HashMap<String, String> >();
    discovery_list_adapter=new SimpleAdapter(this, discovery_list, R.layout.connections_list_item,
                                             new String[] {"ip_address", "port"},
                                             new int[] {R.id.ip_item_label, R.id.port_item_label});
    ListView discover_list=(ListView)findViewById(R.id.discovery_list);
    discover_list.setAdapter(discovery_list_adapter);
    discover_list.setOnItemClickListener(new connect_item(server_list_type.DISCOVERED_SERVER));
    //Set up a list adapter to allow adding the list of recent connections to the UI.
    connections_list=new ArrayList<HashMap<String, String> >();
    connection_list_adapter=new SimpleAdapter(this, connections_list, R.layout.connections_list_item, new String[] {"ip_address", "port"},
                                   new int[] {R.id.ip_item_label, R.id.port_item_label});
    ListView conn_list=(ListView)findViewById(R.id.connections_list);
    conn_list.setAdapter(connection_list_adapter);
    conn_list.setOnItemClickListener(new connect_item(server_list_type.RECENT_CONNECTION));

    discovered_ip_list=new ArrayList<String>();
    discovered_port_list=new ArrayList<Integer>();

    //Set the button callback.
    Button button=(Button)findViewById(R.id.connect);
    button_listener click_listener=new button_listener();
    button.setOnClickListener(click_listener);
    
    set_labels();

  }
  
  private void set_labels() {
	    SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
	    TextView v=(TextView)findViewById(R.id.ca_footer);
	    String s = prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
	    v.setText("Throttle Name: " + s + "\n" + "Client Address: " + mainapp.client_address);  
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu){
	  MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.connection_menu, menu);
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
	  set_labels();
  }

  // Handle pressing of the back button to request shutdown of all threads
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		if (key == KeyEvent.KEYCODE_BACK) {
		    Message connect_msg=Message.obtain();
		    connect_msg.what=message_type.SHUTDOWN;
		    if (mainapp.comm_msg_handler != null) {
		    	mainapp.comm_msg_handler.sendMessage(connect_msg);
		    }    	
			end_this_activity();
		}
		return (super.onKeyDown(key, event));
	};

	//for debugging only
	private void withrottle_list() {		
		try {
			JmDNS jmdns = JmDNS.create();
			ServiceInfo[] infos = jmdns.list("_withrottle._tcp.local.");
			String fh = "";
			for (ServiceInfo info : infos) {
				fh +=  info.getURL() + "\n";
			}
			jmdns.close();
			if (fh != "") {
				Toast.makeText(getApplicationContext(), "Found withrottle servers:\n" + fh, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(), "No withrottle servers found.", Toast.LENGTH_LONG).show();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
