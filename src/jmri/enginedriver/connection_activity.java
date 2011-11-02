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
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.widget.SimpleAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.File;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.IOException;

import android.util.DisplayMetrics;
import android.util.Log;
import java.io.FileReader;
import java.lang.reflect.Method;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.os.Message;
import android.widget.EditText;
import android.widget.Button;
import android.os.Handler;
import android.provider.Settings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.AdapterView;


public class connection_activity extends Activity {
	
  //set use_testhost to default to preset host and port values
  //this avoids manual entry if debugging and discovery isn't available 
  //
  // 0	none
  // 1	mstevetodd.com 44444
  // 2	192.168.1.2 2029
  //
  private static final int use_test_host = 0;
  
  String mst_host = "dev.mstevetodd.com";
  String mst_port = "44444";
  String rdb_host = "192.168.1.2";
  String rdb_port = "2029";
  
  ArrayList<HashMap<String, String> > connections_list;
  ArrayList<HashMap<String, String> > discovery_list;
  private SimpleAdapter connection_list_adapter;
  private SimpleAdapter discovery_list_adapter;

  ArrayList<String> discovered_ip_list;
  ArrayList<Integer> discovered_port_list;

  //pointer back to application
  static threaded_application mainapp;
  
  //The IP address and port that are used to connect.
  private String connected_host;
  private int connected_port;
  
  private long timestamp = 0;
  //flag to indicate the app is shutting down, used to speed up the transition through the lifecycle 
  private boolean isShuttingDown = false;
  
  private static Method overridePendingTransition;
  static {
	  try {
		  overridePendingTransition = Activity.class.getMethod("overridePendingTransition", new Class[] {Integer.TYPE, Integer.TYPE}); //$NON-NLS-1$
	  }
	  catch (NoSuchMethodException e) {
		  overridePendingTransition = null;
	  }
  }

  /**
  * Calls Activity.overridePendingTransition if the method is available (>=Android 2.0)
  * @param activity the activity that launches another activity
  * @param animEnter the entering animation
  * @param animExit the exiting animation
  */
  public static void overridePendingTransition(Activity activity, int animEnter, int animExit) {
	  if (overridePendingTransition!=null) {
		  try {
			  overridePendingTransition.invoke(activity, animEnter, animExit);
		  } catch (Exception e) {
			  // do nothing
		  }
	  }
  }


  //Request connection to the WiThrottle server.
  void connect()  {
	  Message connect_msg=Message.obtain();
	  connect_msg.what=message_type.CONNECT;
	  connect_msg.arg1=connected_port;
	  connect_msg.obj=new String(connected_host);
	  if (mainapp.comm_msg_handler != null) {
		  mainapp.comm_msg_handler.sendMessage(connect_msg);
	  } else {
		  connect_msg.recycle();
		  Toast.makeText(getApplicationContext(), "ERROR: comm thread not started.", Toast.LENGTH_SHORT).show();
	  }    	
  };


/*  void start_select_loco_activity()
  {
//    multicast_lock.release();
	Intent select_loco=new Intent().setClass(this, select_loco.class);
	select_loco.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
	startActivity(select_loco);
	};
  */

  void start_throttle_activity()
  {
	Intent throttle=new Intent().setClass(this, throttle.class);
	startActivity(throttle);
//    this.finish();
    overridePendingTransition(this,R.anim.fade_in, R.anim.fade_out);
 };
  
  public enum server_list_type { DISCOVERED_SERVER, RECENT_CONNECTION }
  public class connect_item implements AdapterView.OnItemClickListener
  {
    server_list_type server_type;

    connect_item(server_list_type new_type) { server_type=new_type; }

    //When an item is clicked, connect to the given IP address and port.
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)    {
    	switch(server_type)      {
    	case DISCOVERED_SERVER:
    		connected_host=new String(discovered_ip_list.get(position));
    		connected_port=discovered_port_list.get(position);
    		break;
    	case RECENT_CONNECTION:
    		ViewGroup vg = (ViewGroup)v; //convert to viewgroup for clicked row
    		TextView hnv = (TextView) vg.getChildAt(0); // get host name from 1st box
    		connected_host = (String) hnv.getText();
    		TextView hpv = (TextView) vg.getChildAt(1); // get port from 2nd box
    		connected_port =new Integer( (String) hpv.getText());

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
	        entry =(EditText)findViewById(R.id.port);
			try {
				connected_port=new Integer(entry.getText().toString());
			} catch(Exception except) { 
				Toast.makeText(getApplicationContext(), "Invalid port#, retry.\n"+except.getMessage(), Toast.LENGTH_SHORT).show();
				connected_port = 0;
				return;
			}
			connect();
		} 
		else {
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
        	start_throttle_activity();

        	//Save the updated connection list to the connections_list.txt file
        	try  {
        		File sdcard_path=Environment.getExternalStorageDirectory();

        		//First, determine if the engine_driver directory exists. If not, create it.
        		File engine_driver_dir=new File(sdcard_path, "engine_driver");
        		if(!engine_driver_dir.exists()) { engine_driver_dir.mkdir(); }

        		File connections_list_file=new File(sdcard_path, "engine_driver/connections_list.txt");
        		PrintWriter list_output;
        		list_output=new PrintWriter(connections_list_file);

        		//Write selected connection to file, then write all others (skipping selected if found)
        		list_output.format("%s:%d\n", connected_host, connected_port);

        		SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        		String smrc = prefs.getString("maximum_recent_connections_preference", ""); //retrieve pref for max recents to show  
        		if (smrc.equals("")) { //if no value or entry removed, set to default
        			smrc = getApplicationContext().getResources().getString(R.string.prefMaximumRecentConnectionsDefaultValue);
        		}
        		int mrc = Integer.parseInt(smrc);  

        		int clEntries =Math.min(connections_list.size(), mrc);  //don't keep more entries than specified in preference
        		for(int i = 0; i < clEntries; i++)  {  //loop thru entries from connections list, up to max in prefs 
        			HashMap <String, String> t = connections_list.get(i);
        			String lh = (String) t.get("ip_address");
        			Integer lp = new Integer((String) t.get("port"));
//***        			if(connected_host != null && connected_port != 0)
        			if(!connected_host.equals(lh) || connected_port!=lp) {  //write it out if not same as selected 
        				list_output.format("%s:%d\n", lh, lp);
        			}
        		}
        		list_output.flush();
        		list_output.close();
        	}
        	catch(IOException except) 	{
        		Log.e("connection_activity", "Error saving recent connection: "+except.getMessage());
        		Toast.makeText(getApplicationContext(), "Error saving recent connection: "+except.getMessage(), Toast.LENGTH_SHORT).show();
        	}
        	break;

        case message_type.DISCONNECT:
        	startShutdown();
        	break;

        case message_type.SHUTDOWN:
        	completeShutdown();
        	break;

        case message_type.ERROR:
          //display error message from msg.obj
          String msg_txt = new String((String)msg.obj);
      	  Toast.makeText(getApplicationContext(), msg_txt, Toast.LENGTH_LONG).show();
      	  break;
      }
    };
  }

	@Override
	public void onPause() {
        super.onPause();
//	      Log.d("Engine_Driver","CA onPause " + timestamp);
		//shutdown server discovery listener
	    Message msg=Message.obtain();
	    msg.what=message_type.SET_LISTENER;
	    msg.arg1 = 0; //zero turns it off
	    if (mainapp.comm_msg_handler != null) {
	    	mainapp.comm_msg_handler.sendMessage(msg);
	    }

	    // clear the discovered list  TODO: handle this better
	    discovered_ip_list.clear();
	    discovered_port_list.clear();
	    discovery_list.clear();
        discovery_list_adapter.notifyDataSetChanged();

}
	
	@Override
	public void onResume() {
		super.onResume();
//	      Log.d("Engine_Driver","CA onResume " + timestamp);
	    if (isShuttingDown)
	    	return;

		connections_list.clear();
	    String example_host = "jmri.mstevetodd.com";
	    String example_port = "44444";
	    
	    //Populate the ListView with the recent connections saved in a file. This will be stored in
	    // /sdcard/engine_driver/connections_list.txt
	    try    {
	    	File sdcard_path=Environment.getExternalStorageDirectory();
	    	File connections_list_file=new File(sdcard_path, "engine_driver/connections_list.txt");

	    	if(connections_list_file.exists())    {
	    		BufferedReader list_reader=new BufferedReader(new FileReader(connections_list_file));
	    		while(list_reader.ready())    {
	    			String line=list_reader.readLine();
	    			Integer splitPos = line.indexOf(':');
	    			if (splitPos > 0) {
	    				String il = line.substring(0, splitPos);
	    				Integer pl;
	    				try {
	    					pl = Integer.decode(line.substring(splitPos+1, line.length()));
	    				} 
	    				catch (Exception e) {
	    					pl = 0;
	    				}
	    				if (pl > 0) {
	    					HashMap<String, String> hm=new HashMap<String, String>();
	    					hm.put("ip_address", il);
	    					String ps = pl.toString();
	    					hm.put("port", ps);
	    					connections_list.add(hm);
	    					if (il.equals(example_host) && ps.equals(example_port)) {
	    						example_host = "";  //clear if found
	    					}
	    				} //if pl>0
	    			} //if splitPos>0 
	    		} //while list_reader
	    	} //if file exists

	    	//if example host not already in list, add it
    		if (!example_host.equals("")) {
    			HashMap<String, String> hm=new HashMap<String, String>();
    			hm.put("ip_address", example_host);
    			hm.put("port", example_port);
    			connections_list.add(hm);
    		}

    		connection_list_adapter.notifyDataSetChanged();

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

	@Override
	public void onRestart() {
		super.onRestart();
//	    Log.d("Engine_Driver","CA onRestart " + timestamp);
	    if (this.isShuttingDown && !this.isFinishing())
	    	this.finish();
	}
	
 /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
//    timestamp = SystemClock.uptimeMillis();
//    Log.d("Engine_Driver","CA onCreate " + timestamp);
    mainapp=(threaded_application)this.getApplication();
//    if(mainapp.connection_msg_handler == null)
    	mainapp.connection_msg_handler=new ui_handler();
    isShuttingDown = false;
    
    //check for "default" throttle name and make it more unique
    //TODO: move this and similar code in preferences.java into single routine
    SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
    String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
    String s = prefs.getString("throttle_name_preference", defaultName);
    if (s.trim().equals("") || s.equals(defaultName)) {
        String deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        if (deviceId != null && deviceId.length() >=4) {
        	deviceId = deviceId.substring(deviceId.length() - 4);
        } else {
        	Random rand = new Random();
        	deviceId = String.valueOf(rand.nextInt(9999));  //use random string
        }
        String uniqueDefaultName = defaultName + " " + deviceId;
    	s = uniqueDefaultName;
    	prefs.edit().putString("throttle_name_preference", s).commit();  //save new name to prefs

    }

    
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

    // suppress popup keyboard until EditText is touched
	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    //Set the button callback.
    Button button=(Button)findViewById(R.id.connect);
    button_listener click_listener=new button_listener();
    button.setOnClickListener(click_listener);
    
//    set_labels();
    DisplayMetrics dm = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(dm);
    threaded_application.min_fling_distance = (int)(threaded_application.SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
    threaded_application.min_fling_velocity = (int)(threaded_application.SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f); 

    if(use_test_host > 0)
    {
    	if(use_test_host == 1)
    	{
    		((EditText)findViewById(R.id.host_ip)).setText(mst_host);
    		((EditText)findViewById(R.id.port)).setText(mst_port);
    	}
    	else if(use_test_host == 2)
    	{
    		((EditText)findViewById(R.id.host_ip)).setText(rdb_host);
    		((EditText)findViewById(R.id.port)).setText(rdb_port);
    	}
	}
  }
  
  private void set_labels() {
	    SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
	    TextView v=(TextView)findViewById(R.id.ca_footer);
	    String s = prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
	    v.setText("Throttle Name: " + s);  
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
//	  	  settings.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    	  startActivityForResult(settings, 0);
     	  overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
      case R.id.about_menu:
    	  Intent about_page=new Intent().setClass(this, about_page.class);
//	  	  about_page.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    	  startActivity(about_page);
     	  overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
        case R.id.preferences:
    	  Intent preferences=new Intent().setClass(this, preferences.class);
//	  	  preferences.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    	  startActivityForResult(preferences, 0);
     	  overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
      }
      return super.onOptionsItemSelected(item);
  }

  //handle return from menu items
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      //since we always do the same action no need to distinguish between requests
	  set_labels();
  }

  // Handle pressing of the back button to request exit
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		if (key == KeyEvent.KEYCODE_BACK) {
			startShutdown();	// close activity and app
		}
		return (super.onKeyDown(key, event));
	};

    //end all activity
    private void startShutdown() {
      isShuttingDown = true;
  	  if (mainapp.comm_msg_handler != null) {
  	  	  Message msg=Message.obtain();
  	  	  msg.what=message_type.SHUTDOWN;
  		  mainapp.comm_msg_handler.sendMessage(msg);
  	  }
    }
    
    private void completeShutdown() {
      this.finish();
//	  mainapp.connection_msg_handler = null;
//	  System.exit(0);
    }


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
