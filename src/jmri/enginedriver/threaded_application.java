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

/* TODO: see changelog-and-todo-list.txt for complete list of project to-do's */

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.net.*;
import java.io.*;

import android.util.Log;
import android.view.View;

import javax.jmdns.*;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
//import android.net.wifi.WifiManager.MulticastLock;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

import jmri.enginedriver.message_type;
import jmri.enginedriver.threaded_application.comm_thread.comm_handler;

import android.content.Context;
import android.content.SharedPreferences;

//The application will start up a thread that will handle network communication in order to ensure that the UI is never blocked.
//This thread will only act upon messages sent to it. The network communication needs to persist across activities, so that is why
public class threaded_application extends Application
{
    public comm_thread thread;
	String host_ip; //The IP address of the WiThrottle server.
	int port; //The TCP port that the WiThrottle server is running on
	int loco_address_T = -1; //The Address of the locomotive being controlled 
	int loco_address_S = -1; //The Address of the locomotive being controlled 
	//shared variables returned from the withrottle server, stored here for easy access by other activities
	String host_name_string; //retrieved host name of connection
	String loco_string_T = "Not Set"; //Loco Address string returned from the server for selected loco #1
	String loco_string_S = "Not Set"; //Loco Address string returned from the server for selected loco #1
	String withrottle_version_string; //version of withrottle server
	String roster_list_string; //roster list
//	String roster_function_string_T; //roster function list for selected loco #1
//	String roster_function_string_S; //roster function list for selected loco #2
	LinkedHashMap<Integer, String> function_labels_T;  //function#s and labels from roster for throttle #1
	LinkedHashMap<Integer, String> function_labels_S;  //function#s and labels from roster for throttle #2
	LinkedHashMap<Integer, String> function_labels_default;  //function#s and labels from local settings
	boolean[] function_states_T;  //current function states for first throttle
	boolean[] function_states_S;  //current function states for second throttle
	String[] to_system_names;
	String[] to_user_names;
	String[] to_states;
	boolean to_allowed = false;  //default turnout support to off
    HashMap<String, String> to_state_names;
	String[] rt_system_names;
	String[] rt_user_names;
	String[] rt_states;
	boolean rt_allowed = false;  //default route support to off
    HashMap<String, String> rt_state_names;
    HashMap<String, String> roster_entries;
	boolean consist_allowed = false;  //default consist support to off
    LinkedHashMap<String, String> consist_entries;

	String power_state;
	int heartbeat_interval; //heartbeat interval in seconds
	//Communications variables.
	Socket client_socket;
	InetAddress host_address;
	//For communication to the comm_thread.
	public comm_handler comm_msg_handler;
	//For communication to each of the activities (set and unset by the activity)
	public Handler ui_msg_handler;
	public Handler engine_driver_msg_handler;
	public Handler select_loco_msg_handler;
	public Handler turnouts_msg_handler;
	public Handler routes_msg_handler;
	public Handler power_control_msg_handler;
	
	PrintWriter output_pw;
	BufferedReader input_reader = null;
	private SharedPreferences prefs;
    private Timer readTimer;  

  class comm_thread extends Thread
  {
    JmDNS jmdns;
    withrottle_listener listener;
    android.net.wifi.WifiManager.MulticastLock multicast_lock;

    //Listen for a WiThrottle service advertisement on the LAN.
    public class withrottle_listener implements ServiceListener
    {
      public void serviceAdded(ServiceEvent event)
      {
        //A service has been added. Request the service's information.
//        JmDNS jmdns=event.getDNS();
        jmdns.requestServiceInfo(event.getType(), event.getName(), 0);
        Log.d("serviceAdded", String.format("InfoRqst Type='%s', Name='%s', %s", event.getType(), event.getName(), event.toString()));
      };

      public void serviceRemoved(ServiceEvent event)
      {
        Log.d("serviceRemoved", event.getName());
      };

      public void serviceResolved(ServiceEvent event)
      {
    	  //A service's information has been resolved. Capture the necessary part needed to connect to that service.
    	  int port=event.getInfo().getPort();
    	  String hostname=event.getInfo().getHostAddress();
    	  try { //attempt to find full name from ip
    		  InetAddress hostaddress=Inet4Address.getByName(hostname);
    		  hostname = hostaddress.getHostName();  //use this name instead of address
    	  }  
    	  catch(UnknownHostException except) {}  //ip will work fine, so don't worry about exception

        Log.d("serviceResolved", String.format("%s:%d", hostname, port));
        //Tell the UI thread so as to update the list of services available.
        Message service_message=Message.obtain();
        service_message.what=message_type.SERVICE_RESOLVED;
        service_message.arg1=port;
        service_message.obj=new String(hostname);
        ui_msg_handler.sendMessage(service_message);
      };
    }
/*    void end_this_thread() {
    	thread = null;
    	this.interrupt();
    }
   */
    
    class comm_handler extends Handler
    {
      //All of the work of the communications thread is initiated from this function.
      public void handleMessage(Message msg)
      {
        switch(msg.what)
        {
        //Connect to the WiThrottle server.
        case message_type.SET_LISTENER:
        	if (jmdns != null) { //don't bother if network stuff not running
        		//arg1= to turn on, arg1=0 to turn off
        		if (msg.arg1 == 0) {
        			jmdns.removeServiceListener("_withrottle._tcp.local.", listener);
        		} else {
        			jmdns.addServiceListener("_withrottle._tcp.local.", listener);
        		}
        	}
        	break;
        	//Connect to the WiThrottle server.
          case message_type.CONNECT:
            //The IP address is stored in the obj as a String, the port is stored in arg1.
            host_ip=new String((String)msg.obj);
            host_ip = host_ip.trim();
            port=msg.arg1;
            
            //clear app.thread shared variables so they can be reset
            host_name_string = null;
            withrottle_version_string = null; 
            heartbeat_interval = 0;
            roster_list_string = null;
            power_state = null;
            to_allowed = false;
            to_states = null;
            to_system_names = null;
            to_user_names = null;
            to_state_names = null;
            rt_allowed = false;
            rt_states = null;
            rt_system_names = null;
            rt_user_names = null;
            rt_state_names = null;
            roster_entries = null;
      	  	function_labels_S = new LinkedHashMap<Integer, String>();
      	  	function_labels_T = new LinkedHashMap<Integer, String>();
      	  	function_labels_default = new LinkedHashMap<Integer, String>();
            consist_allowed = false;
            consist_entries = new LinkedHashMap<String, String>();
            
            try { host_address=InetAddress.getByName(host_ip); }
            catch(UnknownHostException except) {
              process_comm_error("Could not connect to " + host_ip + "\n"+except.getMessage()+"\n"+except.getCause().getMessage());
              return;
            }
            if (host_address == null) {
                process_comm_error("Could not connect to " + host_ip);
                return;            	
            }
            
            host_name_string = host_address.getHostName();  //store host server name in app.thread shared variable

            try { client_socket=new Socket();               //look for someone to answer on specified socket, and set timeout
                  InetSocketAddress sa = new InetSocketAddress(host_ip, port);
                  client_socket.connect(sa, 1500);  //TODO: adjust these timeouts
    		      client_socket.setSoTimeout(300);
            }
            catch(IOException except) {
              process_comm_error("Cannot connect to host "+host_ip+" and port "+port+": "+except.getMessage()+"\nCheck WiThrottle and network settings.");
              return;
            }

            try { output_pw=new PrintWriter(new OutputStreamWriter(client_socket.getOutputStream()), true); }
            catch(IOException except) {
              process_comm_error("Error creating a PrintWriter, IOException: "+except.getMessage());
              return;
            }

			try {input_reader = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
			} catch (IOException except) {
				process_comm_error("Error creating input stream, IOException: "+except.getMessage());
			    return;
			} 

		    String s = prefs.getString("throttle_name_preference", getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue));
            withrottle_send("N" + s);  //send throttle name
            withrottle_send("HU" + s);  //also send throttle name as the UDID
            Message connection_message=Message.obtain();
            connection_message.what=message_type.CONNECTED;
            ui_msg_handler.sendMessage(connection_message);
            		
            start_read_timer();
            
            break;

          //Release the current loco
          case message_type.RELEASE:  //release specified loco
          	String whichThrottle = msg.obj.toString();

        	//        	Boolean f = getApplicationContext().getResources().getBoolean(R.string.prefStopOnReleaseDefaultValue); TODO: fix this
  		    if (prefs.getBoolean("stop_on_release_preference", true )) {
  		    	withrottle_send(whichThrottle+"V0");  //send stop command before releasing (if set in prefs)
  		    }
            if (whichThrottle.equals("T")) {
  		      loco_string_T = "Not Set"; 
//              roster_function_string_T = null;
              loco_address_T = -1;
              function_labels_T = new LinkedHashMap<Integer, String>();
            } else {
              loco_string_S = "Not Set"; 
//              roster_function_string_S = null;
              loco_address_S = -1;
              function_labels_S = new LinkedHashMap<Integer, String>();
            }
            withrottle_send(whichThrottle+"r");  //send release command
            break;

          //send heartbeat
          case message_type.HEARTBEAT:
        	withrottle_send("*");
        	//also send to engine_driver activity if active
 		    if (engine_driver_msg_handler != null) {
		       msg=Message.obtain(); 
		       msg.what=message_type.HEARTBEAT;
		       engine_driver_msg_handler.sendMessage(msg);
		    }
            break;

          //Disconnect from the WiThrottle server.
          case message_type.DISCONNECT:
        	withrottle_send("Q");
            withrottle_send("*-");     //turn off heartbeat
            readTimer.cancel();        //stop reading from socket
            try{ Thread.sleep(500); }   //  give server time to process this.
              catch (InterruptedException except){ process_comm_error("Error sleeping the thread, InterruptedException: "+except.getMessage()); }
            try { client_socket.close(); }
              catch(IOException except) { process_comm_error("Error closing the Socket, IOException: "+except.getMessage()); }
            break;

           //Set up an engine to control. The address of the engine is given in arg1, and the address type (long or short) is given in arg2.
          case message_type.LOCO_ADDR:
            //clear appropriate app-level shared variables so they can be reset
        	  whichThrottle = msg.obj.toString();
        	  if (whichThrottle.equals("T")) {
        		  loco_string_T = "Not Set"; 
//        		  roster_function_string_T = null;
        		  function_labels_T = new LinkedHashMap<Integer, String>();
        		  loco_address_T=msg.arg1;
        	  } else {
        		  loco_string_S = "Not Set"; 
//        		  roster_function_string_S = null;
        		  function_labels_S = new LinkedHashMap<Integer, String>();
        		  loco_address_S=msg.arg1;
        	  }
//            withrottle_send(String.format("T"+(msg.arg2==address_type.LONG ? "L" : "S")+"%d", loco_address_T));
            withrottle_send(String.format(whichThrottle+(msg.arg2==address_type.LONG ? "L" : "S")+"%d", msg.arg1));
                     //In order to get the engine to start, I must set a direction and some non-zero velocity and then set the velocity to zero. TODO: Fix this bug
            //in the WiThrottle server.
//            withrottle_send("TR1\nTV1\nTV0");
            withrottle_send(whichThrottle+"R1\n"+whichThrottle+"V1\n"+whichThrottle+"V0"); 
            withrottle_send("*+");     //always request to turn on heartbeat (must be enabled in server prefs)
            break;

//          case message_type.ERROR:
//            break;
            
          //Adjust the locomotive's speed. arg1 holds the value of the speed to set. //TODO: Allow 14 and 28 speed steps (might need a change on the server size).
          case message_type.VELOCITY:
//          	withrottle_send(String.format("TV%d", msg.arg1));
          	whichThrottle = msg.obj.toString();
        	withrottle_send(String.format(whichThrottle+"V%d", msg.arg1));
            break;

          //Change direction. arg2 holds the direction to change to. The reason direction is in arg2 is for compatibility
          //with the function buttons.
          case message_type.DIRECTION:
          	whichThrottle = msg.obj.toString();
        	withrottle_send(String.format(whichThrottle+"R%d", msg.arg2));
            break;
          
            //Set or unset a function. arg1 is the function number, arg2 is set or unset.
          case message_type.FUNCTION:
          	whichThrottle = msg.obj.toString();
        	withrottle_send(String.format(whichThrottle+"F%d%d", msg.arg2, msg.arg1));
            break;
          
            //send command to change turnout.  PTA2LT12  (throw, close or toggle is passed in arg1) TODO: fix the 8/9 by passing char in obj
          case message_type.TURNOUT:
        	  String whichCommand = "";
        	  String systemname = msg.obj.toString();
        	  switch (msg.arg1) {
	        	  case 2: whichCommand = "2";
	        	  break;
	        	  case 8: whichCommand = "C";
	        	  break;
	        	  case 9: whichCommand = "T";      	  
        	  }
        	  withrottle_send("PTA" + whichCommand + systemname);
              break;

              //send command to route turnout.  PRA2LT12  only 2=toggle supported
          case message_type.ROUTE:
        	  systemname = msg.obj.toString();
        	  withrottle_send("PRA2" + systemname);
              break;

              //send command to change power setting, new state is passed in arg1
          case message_type.POWER_CONTROL:
        	  withrottle_send(String.format("PPA%d", msg.arg1));
              break;

          //end the application and thread
/*          case message_type.SHUTDOWN:
        	//forward end message to all active activities
            Message fwd_msg=Message.obtain();
            fwd_msg.what=message_type.END_ACTIVITY;
//            if (engine_driver_msg_handler != null) { engine_driver_msg_handler.sendMessage(fwd_msg); }
//            if (select_loco_msg_handler != null) { select_loco_msg_handler.sendMessage(fwd_msg); }
//            if (ui_msg_handler != null) { ui_msg_handler.sendMessage(fwd_msg); }
            end_this_thread(); 
            break;
*/
        }
      };
    }
    private void process_comm_error(String msg_txt) {
        Log.e("comm_handler.handleMessage", msg_txt);
        Message ui_msg=Message.obtain();
        ui_msg.what=message_type.ERROR;
        ui_msg.obj = new String(msg_txt);  //put error message text in message
        if (ui_msg_handler != null) {
          ui_msg_handler.sendMessage(ui_msg); //send message to ui thread for display
        }
    }

    private void process_response(String response_str) {
    /* see java/arc/jmri/jmrit/withrottle/deviceserver.java for server code and some documentation
    	  VN<Version#>
    	  T<EngineAddress>(<LongOrShort>)  
    	  S<2ndEngineAddress>(<LongOrShort>)
    	  RL<RosterSize>]<RosterList>
    	  RF<RosterFunctionList>
    	  RS<2ndRosterFunctionList>
    	  *<HeartbeatIntervalInSeconds>      
    	  PTL[<SystemTurnoutName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
    	  PTA<NewTurnoutState><SystemName>
    	  PPA<NewPowerState> where state 0=off, 1=on, 2=unknown
    	  TODO: add remaining items, or move examples into code below
    	  */

    	//send response to debug log for review
        Log.d("Engine_Driver", "<--:" + response_str);

        switch (response_str.charAt(0)) {
	  	case 'T': 
	  		loco_string_T = get_loconame_from_address_string(response_str.substring(1));  //set app variable
	  		
 	  	    break;
	  	
	  	case 'S': 
	  		loco_string_S = get_loconame_from_address_string(response_str.substring(1));  //set app variable
	  	    break;
	  	
	  	case 'V': 
	  		withrottle_version_string = response_str.substring(2);  //set app variable
	  	    break;
	  	
	  	case '*': 
	  		heartbeat_interval = Integer.parseInt(response_str.substring(1));  //set app variable
	  	    break;
	  	
	  	case 'R': //Roster
	  		switch (response_str.charAt(1)) {

	  		case 'C': 
	  			if 	(response_str.charAt(2) == 'C') {  //RCC1
	  				consist_allowed = true;  //set app variable
	  			} else	if 	(response_str.charAt(2) == 'D') {  //RCD}|{88(S)}|{88(S)]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
	  				process_consist_list(response_str);
	  			}

	  			break;

	  		case 'L': 
	  			roster_list_string = response_str.substring(2);  //set app variable
	  			process_roster_list(response_str);  //process roster list
	  			break;

	  		case 'F':   //RF29}|{2591(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
	  			//    	  		roster_function_string_T = response_str.substring(2);  //set app variable for throttle 1  TODO: remove this
	  			process_roster_function_string(response_str.substring(2), "T");
	  			break;

	  		case 'S': //RS29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
	  			//    	  		roster_function_string_S = response_str.substring(2);  //set app variable for throttle 2  TODO: remove this
	  			process_roster_function_string(response_str.substring(2), "S");
	  			break;

	  		case 'P': //Properties
	  			if 	(response_str.charAt(2) == 'F') {  //function state 
	  				process_function_state(response_str);  //process function state message (passing the whole message)
	  			}
	  			break;
	  		}  //end switch inside R
	  		break;
	  	case 'P': //Panel 
	    	  switch (response_str.charAt(1)) {
	    	  	case 'T': //turnouts
	    	  		if (response_str.charAt(2) == 'T') {  //turnout control allowed
	    	  			to_allowed = true;
	    	  			process_turnout_titles(response_str);
	    	  		}
	    	  		if (response_str.charAt(2) == 'L') {  //list of turnouts
	    	  			process_turnout_list(response_str);  //process turnout list
	    	  		}
	    	  		if (response_str.charAt(2) == 'A') {  //action?  changes to turnouts
	    	  			process_turnout_change(response_str);  //process turnout changes
	    	  		}
	    	  	    break;
	    	  	
	    	  	case 'R':  //routes 
	    	  		if (response_str.charAt(2) == 'T') {  //route  control allowed
	    	  			rt_allowed = true;
	    	  			process_route_titles(response_str);
	    	  		}
	    	  		if (response_str.charAt(2) == 'L') {  //list of routes
	    	  			process_route_list(response_str);  //process route list
	    	  		}
	    	  		if (response_str.charAt(2) == 'A') {  //action?  changes to routes
	    	  			process_route_change(response_str);  //process route changes
	    	  		}
	    	  	    break;
	        	  	
	    	  	case 'P':  //power 
	    	  		if (response_str.charAt(2) == 'A') {  //change power state
	    	  			power_state = response_str.substring(3);
	    	  		}
	    	  	    break;
	    	  }  //end switch inside P
		  	 break;
  	  }  //end switch
  	  
  	  //forward whatever we got to other activities (if started)  dup code needed, not sure why msg is getting stepped on
      Message msg=Message.obtain(); 
      msg.what=message_type.RESPONSE;
      msg.obj=new String(response_str); 
      if (turnouts_msg_handler != null)   { turnouts_msg_handler.sendMessage(msg);   }
      msg=Message.obtain(); 
      msg.what=message_type.RESPONSE;
      msg.obj=new String(response_str); 
      if (routes_msg_handler != null)   { routes_msg_handler.sendMessage(msg);   }
      msg=Message.obtain(); 
      msg.what=message_type.RESPONSE;
      msg.obj=new String(response_str); 
      if (engine_driver_msg_handler != null) { engine_driver_msg_handler.sendMessage(msg); }
      msg=Message.obtain(); 
      msg.what=message_type.RESPONSE;
      msg.obj=new String(response_str); 
      if (power_control_msg_handler != null) { power_control_msg_handler.sendMessage(msg); }
      
    }  //end of process_response

    //parse roster functions list into appropriate app variable array
    //  //RF29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
    private void process_roster_function_string(String response_str, String whichThrottle) {
    	//clear the appropriate global variable
    	if (whichThrottle.equals("T")) {
    		function_labels_T = new LinkedHashMap<Integer, String>();
//    		function_labels_T.clear();
    	} else {
    		function_labels_S = new LinkedHashMap<Integer, String>();
//    		function_labels_S.clear();
    	}

    	String[] ta = splitByString(response_str,"]\\[");  //split into list of labels
    	//initialize app arrays (skipping first)
    	int i = 0;
    	for (String ts : ta) {
    		if (i > 0 && !ts.equals("")) { //skip first chunk, which is length, and skip any blank entries
    	    	if (whichThrottle.equals("T")) {  //populate the appropriate hashmap
    	    		function_labels_T.put(i-1,ts); //index is hashmap key, value is label string
    	    	} else {
    	    		function_labels_S.put(i-1,ts); //index is hashmap key, value is label string
    	    	}
    		}  //end if i>0
    		i++;
    	}  //end for
    }

    //parse roster list into appropriate app variable array
    //  RL2]\[NS2591}|{2591}|{L]\[NS4805}|{4805}|{L
    private void process_roster_list(String response_str) {
    	//clear the global variable
    	roster_entries = new HashMap<String, String>();

    	String[] ta = splitByString(response_str,"]\\[");  //initial separation 
    	//initialize app arrays (skipping first)
    	int i = 0;
    	for (String ts : ta) {
    		if (i > 0) { //skip first chunk
    			String[] tv = splitByString(ts,"}|{");  //split these into name, address and length
    			roster_entries.put(tv[0],tv[1]+"("+tv[2]+")"); //roster name is hashmap key, value is address(L or S), e.g.  2591(L)
    		}  //end if i>0
    		i++;
    	}  //end for
    }

    //parse consist list into appropriate mainapp hashmap
   //RCD}|{88(S)}|{88(S)]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
    private void process_consist_list(String response_str) {
    	String consist_addr = null;
    	String consist_desc = "";
    	String consist_name = "";
    	String[] ta = splitByString(response_str,"]\\[");  //initial separation
    	String plus = ""; //plus sign for a separator
    	//initialize app arrays (skipping first)
    	int i = 0;
    	for (String ts : ta) {
    		if (i == 0) { //first chunk is a "header"
    			String[] tv = splitByString(ts,"}|{");  //split header chunk into header, address and name
    			consist_addr = tv[1];
    			consist_name = tv[2];
    		}  else {  //list of locos in consist
    			String[] tv = splitByString(ts,"}|{");  //split these into loco address and direction
    			tv = splitByString(tv[0],"(");  //split again to strip off address size (L)
    			consist_desc +=  plus + tv[0];
    			plus = "+";
    		}  //end if i==0
    		i++;
    	}  //end for
    	if (!consist_name.equals(consist_addr)) {
    		consist_desc = consist_name; // use name if different from address
    	}
    	consist_entries.put(consist_addr, consist_desc); 
    }


    //parse turnout change to update mainapp array entry
	//  PTA<NewState><SystemName>
    //  PTA2LT12
    private void process_turnout_change(String response_str) {
    	String newState = response_str.substring(3,4);
    	String systemName = response_str.substring(4);
    	int pos = -1;
        for (String sn : to_system_names) { //TODO: rewrite for better lookup
        	pos++;
        	if (sn.equals(systemName)) {
        		break;
        	}
        }
        if (pos <= to_system_names.length) {  //if found, update to new value
        	to_states[pos] = newState;
        }
    }  //end of process_turnout_change

    //parse turnout list into appropriate app variable array
	//  PTL[<SystemName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
    //  PTL]\[LT12}|{my12}|{1
    private void process_turnout_list(String response_str) {
     
     String[] ta = splitByString(response_str,"]\\[");  //initial separation 
     //initialize app arrays (skipping first)
     to_system_names = new String[ta.length - 1];
     to_user_names = new String[ta.length - 1];
     to_states = new String[ta.length - 1];
     int i = 0;
     for (String ts : ta) {
    	 if (i > 0) { //skip first chunk, just message id
        	 String[] tv = splitByString(ts,"}|{");  //split these into 3 parts, key and value
        	 to_system_names[i-1] = tv[0];
        	 to_user_names[i-1]      = tv[1];
        	 to_states[i-1]                 = tv[2];
    	 }  //end if i>0
    	 i++;
     }  //end for
     
  }

    private void process_turnout_titles(String response_str) {
        //PTT]\[Turnouts}|{Turnout]\[Closed}|{2]\[Thrown}|{4
    	
    	//clear the global variable
        to_state_names = new HashMap<String, String>();
        
    	String[] ta = splitByString(response_str,"]\\[");  //initial separation 
        //initialize app arrays (skipping first)
        int i = 0;
        for (String ts : ta) {
       	 if (i > 1) { //skip first 2 chunks
       		 String[] tv = splitByString(ts,"}|{");  //split these into value and key
           	 to_state_names.put(tv[1],tv[0]);
       	 }  //end if i>0
       	 i++;
        }  //end for
        
     }

    //parse route list into appropriate app variable array
	//  PRA<NewState><SystemName>
    //  PRA2LT12
    private void process_route_change(String response_str) {
    	String newState = response_str.substring(3,4);
    	String systemName = response_str.substring(4);
    	int pos = -1;
        for (String sn : rt_system_names) {
        	pos++;
        	if (sn.equals(systemName)) {
        		break;
        	}
        }
        if (pos <= rt_system_names.length) {  //if found, update to new value
        	rt_states[pos] = newState;
        }
    }  //end of process_route_change

    //parse route list into appropriate app variable array
	//  PTL[<SystemName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
    //  PTL]\[LT12}|{my12}|{1
    private void process_route_list(String response_str) {
     
     String[] ta = splitByString(response_str,"]\\[");  //initial separation 
     //initialize app arrays (skipping first)
     rt_system_names = new String[ta.length - 1];
     rt_user_names = new String[ta.length - 1];
     rt_states = new String[ta.length - 1];
     int i = 0;
     for (String ts : ta) {
    	 if (i > 0) { //skip first chunk, just message id
        	 String[] tv = splitByString(ts,"}|{");  //split these into 3 parts, key and value
        	 rt_system_names[i-1] = tv[0];
        	 rt_user_names[i-1]      = tv[1];
        	 rt_states[i-1]                 = tv[2];
    	 }  //end if i>0
    	 i++;
     }  //end for
     
  }

    private void process_route_titles(String response_str) {
        //PRT
    	
    	//clear the global variable
        rt_state_names = new HashMap<String, String>();
        
    	String[] ta = splitByString(response_str,"]\\[");  //initial separation 
        //initialize app arrays (skipping first)
        int i = 0;
        for (String ts : ta) {
       	 if (i > 1) { //skip first 2 chunks
       		 String[] tv = splitByString(ts,"}|{");  //split these into value and key
           	 rt_state_names.put(tv[1],tv[0]);
       	 }  //end if i>0
       	 i++;
        }  //end for
     }

    //parse function state string into appropriate app variable array
    private void process_function_state(String response_str) {
    
     String whichThrottle = null;
     
     String[] sa = splitByString(response_str,"]\\[F");  //initial separation (note that I include the F just to strip it off and simplify later stuff
     int i = 0;
     for (String fs : sa) {
    	 String[] fa = splitByString(fs,"}|{");  //split these into 2 parts, key and value
    	 if (i == 0) { //first chunk is different, contains whichThrottle
    	    whichThrottle = fa[1];
    	 } else {  //all others have function#, then value
    		 int fn = Integer.parseInt(fa[0]);
    		 boolean fState = Boolean.parseBoolean(fa[1]);
    		 
    		 if (whichThrottle.equals("T")) {
    			 function_states_T[fn] = fState;
    		 }  else {
    			 function_states_S[fn] = fState;
    		 }
    	 }  //end if i==0
    	 i++;
     }  //end for
  }

	// get the roster name from address string 123(L).  Return input if not found in roster or in consist
    private String get_loconame_from_address_string(String response_str) {

    	if ((roster_entries != null) && (roster_entries.size() > 0))  { 
    		for (String rostername : roster_entries.keySet()) {  // loop thru roster entries, 
    			if (roster_entries.get(rostername).equals(response_str)) { //looking for value = input parm
    				return rostername;  //if found, return the roster name (key)
    			}
    		}
    	}
    	if ((consist_entries != null) && (consist_entries.size() > 0)) {
    		String consistname = consist_entries.get(response_str);  //consists are keyed by address "123(L)"
    		if (consistname != null)  { //looking for value = input parm
    			return consistname;  //if found, return the consist name (value)
    		}
    	}
    	return response_str; //return input if not found
    }
    
    //send the passed-in message to the socket
    private void withrottle_send(String msg) {
    	if (output_pw != null) {
      	  output_pw.println(msg);
      	  output_pw.flush();
      	} else {
          process_comm_error("No writer, tried to send: "+msg);
      	}
        //send response to debug log for review
        Log.d("Engine_Driver", "-->:" + msg);
    }  //end withrottle_send()


    //setup a loop to read from the socket 
    void start_read_timer() {
      readTimer = new Timer();
	  readTimer.schedule(new TimerTask() {
		@Override
		public void run() {
			withrottle_rcv();
		}
	}, 1000, 2000 );  //1 sec on first fire, 2 sec on subsequent ones
}

    
    //read anything coming back from server and call process_response for it.
    private void withrottle_rcv() {
      	  
     	  //read responses from withrottle and send non-blank ones to other activities
          readTimer.cancel();  //don't double-fire
      	  String str = null;
			try {
				while ((str = input_reader.readLine()) != null) {  //loop until no more data found
					if (str.length()>0) {
						process_response(str);
					}
				}
			} catch  (SocketTimeoutException e )   {
				start_read_timer();  //delay and read again
			} catch (IOException e) {
  	            readTimer.cancel();  //stop trying to read if an error occurred
//				e.printStackTrace();
  	            process_comm_error("withrottle_rcv err:" + e.getMessage());  
			} 
      	  
    }  //end withrottle_rcv()

    public void run()
    {
    	Looper.prepare();
    	comm_msg_handler=new comm_handler(); //set shared pointer to handler 

      	int intaddr = 0;

    	//Set up to find a WiThrottle service via ZeroConf, not supported for OS 1.5 (SDK 3)
    	if (android.os.Build.VERSION.SDK.equals("3")) {    	 
    		process_comm_error("WiFi discovery not supported.  Skipping.");
    	} else {
    		try   {
    			WifiManager wifi = (WifiManager)threaded_application.this.getSystemService(Context.WIFI_SERVICE);
    			//Acquire a multicast lock. This allows us to obtain multicast packets, but consumes a bit more battery life.
    			//Release it as soon as possible (after the user has connected to a WiThrottle service, or this application is
    			//not the currently active one.

       			multicast_lock=wifi.createMulticastLock("engine_driver");
      			multicast_lock.setReferenceCounted(true);
       			multicast_lock.acquire();
    			WifiInfo wifiinfo = wifi.getConnectionInfo();
    			intaddr = wifiinfo.getIpAddress();
    			if (intaddr != 0) {
    				byte[] byteaddr = new byte[] { (byte)(intaddr & 0xff), (byte)(intaddr >> 8 & 0xff), (byte)(intaddr >> 16 & 0xff),
    						(byte)(intaddr >> 24 & 0xff) };
    				InetAddress addr = InetAddress.getByAddress(byteaddr);
    				String s = String.format("found intaddr=%d, addr=%s", intaddr, addr.toString());
    				Log.d("comm_thread_run", s);

    				jmdns=JmDNS.create(addr);
    				listener=new withrottle_listener();
//    				jmdns.addServiceListener("_withrottle._tcp.local.", listener);  moved to handler so ca can control
    			} else {
    				process_comm_error("No IP Address found.\nCheck your WiFi connection.");
    			}  //end of if intaddr==0
    		}  catch(IOException except) { 
    			Log.e("comm_thread_run", "Error creating withrottle listener: IOException: "+except.getMessage()); 
    			process_comm_error("Error creating withrottle listener: IOException: \n"+except.getMessage()+"\n"+except.getCause().getMessage()); 
    		}
    	}     
//    	Looper.prepare();
//    	comm_msg_handler=new comm_handler();
    	Looper.loop();
    };
  }
  
  
  public void onCreate()
  {
	  prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
	  function_states_T = new boolean[32];
	  function_states_S = new boolean[32];
	
	  thread=new comm_thread();
	  thread.start();
  }

/** ------ copied from jmri util code -------------------
 * Split a string into an array of Strings, at a particular
 * divider.  This is similar to the new String.split method,
 * except that this does not provide regular expression
 * handling; the divider string is just a string.
 * @param input String to split
 * @param divider Where to divide the input; this does not appear in output
 */
  static public String[] splitByString(String input, String divider) {
    int size = 0;
    String temp = input;
    
    // count entries
    while (temp.length() > 0) {
        size++;
        int index = temp.indexOf(divider);
        if (index < 0) break;    // break not found
        temp = temp.substring(index+divider.length());
        if (temp.length() == 0) {  // found at end
            size++;
            break;
        }
    }
    
    String[] result = new String[size];
    
    // find entries
    temp = input;
    size = 0;
    while (temp.length() > 0) {
        int index = temp.indexOf(divider);
        if (index < 0) break;    // done with all but last
        result[size] = temp.substring(0,index);
        temp = temp.substring(index+divider.length());
        size++;
    }
    result[size] = temp;
    
    return result;
  }
}

