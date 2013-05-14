/* Copyright (C) 2013 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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
import android.os.AsyncTask;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.AdapterView;

public class connection_activity extends Activity {
	ArrayList<HashMap<String, String> > connections_list;
	ArrayList<HashMap<String, String> > discovery_list;
	private SimpleAdapter connection_list_adapter;
	private SimpleAdapter discovery_list_adapter;

	//pointer back to application
	private threaded_application mainapp;
	//The IP address and port that are used to connect.
	private String connected_hostip;
	private String connected_hostname;
	private int connected_port;

    private static final String example_host = "jmri.mstevetodd.com";
    private static final String example_port = "44444";

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
//	  sendMsgErr(0, message_type.CONNECT, connected_hostip, connected_port, "ERROR in ca.connect: comm thread not started.");
	  mainapp.sendMsg(mainapp.comm_msg_handler, message_type.CONNECT, connected_hostip, connected_port);
  };


  void start_throttle_activity()
  {
	Intent throttle=new Intent().setClass(this, throttle.class);
/***future Notification
	//set flags to ensure there is just one throttle activity
	throttle.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
***/	
	startActivity(throttle);
 	this.finish();
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
    	case RECENT_CONNECTION:
    		ViewGroup vg = (ViewGroup)v; //convert to viewgroup for clicked row
    		TextView hip = (TextView) vg.getChildAt(0); // get host ip from 1st box
    		connected_hostip = (String) hip.getText();
    		TextView hnv = (TextView) vg.getChildAt(1); // get host name from 2nd box
    		connected_hostname = (String) hnv.getText();
    		TextView hpv = (TextView) vg.getChildAt(2); // get port from 3rd box
    		connected_port = Integer.valueOf((String) hpv.getText());
    		break;
    	}
    	connect();
    };
  }

  public class button_listener implements View.OnClickListener
  {
    public void onClick(View v) {
    	EditText entry=(EditText)findViewById(R.id.host_ip);
		connected_hostip=new String(entry.getText().toString());
		if (connected_hostip.trim().length() > 0 ) {
	        entry =(EditText)findViewById(R.id.port);
			try {
				connected_port = Integer.valueOf(entry.getText().toString());
			} catch(Exception except) { 
				Toast.makeText(getApplicationContext(), "Invalid port#, retry.\n"+except.getMessage(), Toast.LENGTH_SHORT).show();
				connected_port = 0;
				return;
			}
			connected_hostname = connected_hostip; //copy ip to name
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
    @SuppressWarnings("unchecked")
	public void handleMessage(Message msg)
    {
      switch(msg.what)
      {
        case message_type.SERVICE_RESOLVED:
            HashMap<String, String> hm=new HashMap<String, String>();
            hm = (HashMap<String, String>) msg.obj;  //payload is already a hashmap
            String found_host_name = hm.get("host_name");
            boolean entryExists = false;

            //stop if new address is already in the list
            HashMap<String, String> tm=new HashMap<String, String>();
            for(int index=0; index < discovery_list.size(); index++) {
            	tm = discovery_list.get(index);
            	if (tm.get("host_name").equals(found_host_name)) { 
            		entryExists = true;
            		break;
            	}
            }
            if(!entryExists) {            	// if new host, add to discovered list on screen          
            	discovery_list.add(hm);
            	discovery_list_adapter.notifyDataSetChanged();
            }
            break;

        case message_type.SERVICE_REMOVED:        
            //look for name in list
            String removed_host_name = (String) msg.obj;
            tm=new HashMap<String, String>();
            for(int index=0; index < discovery_list.size(); index++) {
            	tm = discovery_list.get(index);
            	if (tm.get("host_name").equals(removed_host_name)) {
                    discovery_list.remove(index);
                    discovery_list_adapter.notifyDataSetChanged();
                    break;
            	}
            }
        	break;

        case message_type.CONNECTED:
        	//use asynctask to save the updated connections list to the connections_list.txt file
        	new saveConnectionsList().execute();
        	start_throttle_activity();
        	break;

        case message_type.SHUTDOWN:
        	shutdown();
        	break;
      }
    };
  }

 /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
//    timestamp = SystemClock.uptimeMillis();
    Log.d("Engine_Driver","connection.onCreate ");
    mainapp=(threaded_application)this.getApplication();
    mainapp.connection_msg_handler=new ui_handler();

    //ensure statics in all activities are reinitialize since Android might not have killed app since it was last Exited.
    //do this here instead of TA.onCreate() because that method won't be invoked if app is still running. 
	throttle.initStatics();
	web_activity.initStatics();
    
    
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
    		new String[] {"ip_address", "host_name", "port"},
    		new int[] {R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
    ListView discover_list=(ListView)findViewById(R.id.discovery_list);
    discover_list.setAdapter(discovery_list_adapter);
    discover_list.setOnItemClickListener(new connect_item(server_list_type.DISCOVERED_SERVER));
    
    //Set up a list adapter to allow adding the list of recent connections to the UI.
    connections_list=new ArrayList<HashMap<String, String> >();
    connection_list_adapter=new SimpleAdapter(this, connections_list, R.layout.connections_list_item, 
    		new String[] {"ip_address", "host_name", "port"},
    		new int[] {R.id.ip_item_label, R.id.host_item_label, R.id.port_item_label});
    ListView conn_list=(ListView)findViewById(R.id.connections_list);
    conn_list.setAdapter(connection_list_adapter);
    conn_list.setOnItemClickListener(new connect_item(server_list_type.RECENT_CONNECTION));

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

  }
  
    @Override
	public void onResume() {
		super.onResume();
		if(this.isFinishing()) {		//if finishing, expedite it
			return;
		}

		mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
	    //start up server discovery listener
//	    sendMsgErr(0, message_type.SET_LISTENER, "", 1, "ERROR in ca.onResume: comm thread not started.") ;
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 1);
	    //populate the ListView with the recent connections
	    getConnectionsList();
	    set_labels();
	    //start up server discovery listener again (after a 1 second delay)
	    //TODO: this is a rig, figure out why this is needed for ubuntu servers
//	    sendMsgErr(1000, message_type.SET_LISTENER, "", 1, "ERROR in ca.onResume: comm thread not started.") ;
		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 1);
	}  //end of onResume

	@Override
	public void onPause() {
	    super.onPause();
	}

	@Override
	public void onStop() {
		Log.d("Engine_Driver","connection.onStop()");
		//shutdown server discovery listener
	    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_LISTENER, "", 0);
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		Log.d("Engine_Driver","connection.onDestroy()");
//		mainapp.connection_msg_handler = null;
		super.onDestroy();
	}
	
    private void shutdown() {
    	this.finish();
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
	  Intent in;
      switch (item.getItemId()) {
      case R.id.exit_mnu:
    	  mainapp.checkExit(this);
     	  break;
      case R.id.preferences_mnu:
    	  in=new Intent().setClass(this, preferences.class);
     	  startActivityForResult(in, 0);
     	  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    	  break;
	  case R.id.about_mnu:
		  in=new Intent().setClass(this, about_page.class);
	 	  startActivity(in);
	 	  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
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
			mainapp.checkExit(this);
		}
		return (super.onKeyDown(key, event));
	};

/***	
	private void sendMsgErr(long delayMs, int msgType, String msgBody, int msgArg1, String errMsg) {
		if(!mainapp.sendMsgDelay(mainapp.comm_msg_handler, delayMs, msgType, msgBody, msgArg1, 0)) {
			Log.e("Engine_Driver",errMsg) ;
			Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
		}    	
	}
***/
	
	//save connections list in background using asynctask
	 private class saveConnectionsList extends AsyncTask<Void, Void, String> {
		 @Override
		 protected String doInBackground(Void... params) {
			String errMsg = "";
			
			//if no SD Card present then nothing to do
	    	if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
	    		return errMsg;
	    	try  {
		    	File path=Environment.getExternalStorageDirectory();
	    		File engine_driver_dir=new File(path, "engine_driver");
    			engine_driver_dir.mkdir();			// create directory if it doesn't exist 

	    		File connections_list_file=new File(path, "engine_driver/connections_list.txt");
	    		PrintWriter list_output=new PrintWriter(connections_list_file);

	    		//Write selected connection to file, then write all others (skipping selected if found)
	    		list_output.format("%s:%s:%d\n", connected_hostname, connected_hostip, connected_port);

	    		SharedPreferences prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
	    		String smrc = prefs.getString("maximum_recent_connections_preference", ""); //retrieve pref for max recents to show  
	    		if (smrc.equals("")) { //if no value or entry removed, set to default
	    			smrc = getApplicationContext().getResources().getString(R.string.prefMaximumRecentConnectionsDefaultValue);
	    		}
	    		int mrc = Integer.parseInt(smrc);  
	    		int clEntries =Math.min(connections_list.size(), mrc);  //don't keep more entries than specified in preference
	    		for(int i = 0; i < clEntries; i++)  {  //loop thru entries from connections list, up to max in prefs 
	    			HashMap <String, String> t = connections_list.get(i);
	    			String li = (String) t.get("ip_address");
	    			String lh = (String) t.get("host_name");
	    			Integer lp = Integer.valueOf((String) t.get("port"));
//***        			if(connected_hostip != null && connected_port != 0)
	    			if(!connected_hostip.equals(li) || connected_port!=lp) {  //write it out if not same as selected 
	    				list_output.format("%s:%s:%d\n", lh, li, lp);
	    			}
	    		}
	    		list_output.flush();
	    		list_output.close();
	    	}
	    	catch(IOException except) 	{
	    		errMsg = except.getMessage();
	    	}
			return errMsg;
		 }
		 @Override
		 protected void onPostExecute(String errMsg) {
			 if(errMsg.length() > 0)
	    		Toast.makeText(getApplicationContext(), "Error saving recent connection: "+errMsg, Toast.LENGTH_SHORT).show();
		 }
	 }
	  
	private void getConnectionsList() {
		boolean foundExampleHost = false;
		connections_list.clear();
		String errMsg = "";
    	if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
        	//alert user that recent connections list requires SD Card
       	    TextView v=(TextView)findViewById(R.id.recent_connections_heading);
       	    v.setText(getString(R.string.ca_recent_conn_notice));
    	}
    	else {
    		try {
		    	File sdcard_path=Environment.getExternalStorageDirectory();
		    	File connections_list_file=new File(sdcard_path, "engine_driver/connections_list.txt");
	
		    	if(connections_list_file.exists())    {
		    		BufferedReader list_reader=new BufferedReader(new FileReader(connections_list_file));
		    		while(list_reader.ready())    {
		    			String line=list_reader.readLine();
		    			String ip_address;
		    			String host_name;
		    			String port_str = "";
		    			Integer port = 0;
		    			List<String> parts = new ArrayList<String>();
		    			parts = Arrays.asList(line.split(":", 3)); //split record from file, max of 3 parts
		    			if (parts.size() > 1) {  //skip if not split
		    				if (parts.size() == 2) {  //old style, use part 1 for ip and host
		    					host_name = parts.get(0);
		    					ip_address = parts.get(0);
		    					port_str = parts.get(1).toString();
		    				} else { 						  //new style, get all 3 parts
		    					host_name = parts.get(0);
		    					ip_address = parts.get(1);
		    					port_str = parts.get(2).toString();
		    				}
		    				try {  //attempt to convert port to integer
		    					port = Integer.decode(port_str);
		    				} 
		    				catch (Exception e) {
		    				}
		    				if (port > 0) {  //skip if port not converted to integer
			    					HashMap<String, String> hm=new HashMap<String, String>();
			    					hm.put("ip_address", ip_address);
			    					hm.put("host_name", host_name);
			    					hm.put("port", port.toString());
			    					if(!connections_list.contains(hm)) {	// suppress dups
			    						connections_list.add(hm);
			    					}
		    					if (host_name.equals(example_host) && port.toString().equals(example_port)) {
		    						foundExampleHost = true; 
		    					}
		    				}
		    			} 
		    		}
		    		list_reader.close();
		    	}
		    }
		    catch (IOException except) {
				errMsg = except.getMessage();
				Log.e("connection_activity", "Error reading recent connections list: "+errMsg);
				Toast.makeText(getApplicationContext(), "Error reading recent connections list: "+errMsg, Toast.LENGTH_SHORT).show();
		    }
    	}
    	
	    //if example host not already in list, add it at end
		if (!foundExampleHost) {
				HashMap<String, String> hm=new HashMap<String, String>();
				hm.put("ip_address", example_host);
				hm.put("host_name", example_host);
				hm.put("port", example_port);
				connections_list.add(hm);
		}
		connection_list_adapter.notifyDataSetChanged();
	}
	
	//for debugging only
/*	private void withrottle_list() {		
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
*/

}
