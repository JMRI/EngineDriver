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

/* TODO: see changelog-and-todo-list.txt for complete list of project to-do's */

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import java.net.*;
import java.io.*;

import android.util.Log;
import javax.jmdns.*;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;

import jmri.enginedriver.message_type;
import jmri.enginedriver.threaded_application.comm_thread.comm_handler;
import jmri.jmrit.roster.RosterEntry;
import android.content.Context;
import android.content.SharedPreferences;

//The application will start up a thread that will handle network communication in order to ensure that the UI is never blocked.
//This thread will only act upon messages sent to it. The network communication needs to persist across activities, so that is why
public class threaded_application extends Application {
	public comm_thread thread;
	String host_ip; //The IP address of the WiThrottle server.
	int port; //The TCP port that the WiThrottle server is running on
	//shared variables returned from the withrottle server, stored here for easy access by other activities
	//	String host_name_string; //retrieved host name of connection
	String loco_string_T = "Not Set"; //Loco Address string to display on first throttle
	String loco_string_S = "Not Set"; //Loco Address string to display on second throttle
	Double withrottle_version = 0.0; //version of withrottle server
	Integer web_server_port = 0; //default port for jmri web server
	String roster_list_string; //roster list
	LinkedHashMap<Integer, String> function_labels_T;  //function#s and labels from roster for throttle #1
	LinkedHashMap<Integer, String> function_labels_S;  //function#s and labels from roster for throttle #2
	LinkedHashMap<String, String> locos_on_T;  //list of locos currently assigned to throttle #1
	LinkedHashMap<String, String> locos_on_S;  //list of locos currently assigned to throttle #2
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
	HashMap<String, String> roster_entries;  //roster sent by WiThrottle
	boolean consist_allowed = false;  //default consist support to off
	LinkedHashMap<String, String> consist_entries;
	HashMap<String, RosterEntry> roster;  //roster entries retrieved from roster.xml (null if not retrieved)
	public static HashMap<String, String> metadata;  //metadata values (such as JMRIVERSION) retrieved from web server (null if not retrieved)
	ImageDownloader imageDownloader = new ImageDownloader();
	String webViewLocation = "none"; //pref value where user would like to see webview (or none)

	String power_state;

	static final int MIN_OUTBOUND_HEARTBEAT_INTERVAL = 2;	//minimum interval for outbound heartbeat generator
	static final int HEARTBEAT_RESPONSE_ALLOWANCE = 4;		//worst case time delay for WiT to respond to a heartbeat message
	public int heartbeatInterval = 0;						//WiT heartbeat interval setting

	String client_address; //address string of the client address
	//For communication to the comm_thread.
	public comm_handler comm_msg_handler;
	//For communication to each of the activities (set and unset by the activity)
	public Handler connection_msg_handler;
	public Handler throttle_msg_handler;
	public Handler select_loco_msg_handler;
	public Handler turnouts_msg_handler;
	public Handler routes_msg_handler;
	public Handler power_control_msg_handler;

	//these constants are used for onFling
	public static final int SWIPE_MIN_DISTANCE = 120;
	public static final int SWIPE_MAX_OFF_PATH = 250;
	public static final int SWIPE_THRESHOLD_VELOCITY = 200;
	public static int min_fling_distance;			// pixel width needed for fling
	public static int min_fling_velocity;			// velocity needed for fling

	private SharedPreferences prefs;

	class comm_thread extends Thread  {
		JmDNS jmdns = null;
		boolean endingJmdns = false;
		withrottle_listener listener;
		android.net.wifi.WifiManager.MulticastLock multicast_lock;
		socket_WiT socketWiT;
		heartbeat heart  = new heartbeat();

		private String last_roster_entry_requested = ""; //"remember" last entry, for use in response

		comm_thread() {
			super("comm_thread");
		}

		//Listen for a WiThrottle service advertisement on the LAN.
		public class withrottle_listener implements ServiceListener    {

			public void serviceAdded(ServiceEvent event)  	{
				//    		Log.d("Engine_Driver", String.format("serviceAdded fired"));
				//A service has been added. If no details, ask for them 
				ServiceInfo si = jmdns.getServiceInfo(event.getType(), event.getName(), 0);
				if (si == null || si.getPort() == 0 ) { 
					jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
					Log.d("Engine_Driver", String.format("serviceAdded, requesting details: '%s', Type='%s'", event.getName(), event.getType()));
				}
			};

			public void serviceRemoved(ServiceEvent event)      {
				//Tell the UI thread so as to remove from the list of services available.
				Message service_message=Message.obtain();
				service_message.what=message_type.SERVICE_REMOVED;
				service_message.obj=event.getName();  //send the service name to be removed
				connection_msg_handler.sendMessage(service_message);
				Log.d("Engine_Driver", String.format("serviceRemoved: '%s'", event.getName()));
			};

			public void serviceResolved(ServiceEvent event)  {
				//    		Log.d("Engine_Driver", String.format("serviceResolved fired"));
				//A service's information has been resolved. Send the port and service name to connect to that service.
				int port=event.getInfo().getPort();
				String host_name = event.getInfo().getName(); //
				Inet4Address[] ip_addresses = event.getInfo().getInet4Addresses();  //only get ipV4 address
				String ip_address = ip_addresses[0].toString().substring(1);  //use first one, since WiThrottle is only putting one in (for now), and remove leading slash

				//Tell the UI thread so as to update the list of services available.
				HashMap<String, String> hm=new HashMap<String, String>();
				hm.put("ip_address", ip_address);
				hm.put("port", ((Integer)port).toString());
				hm.put("host_name",host_name);

				Message service_message=Message.obtain();
				service_message.what=message_type.SERVICE_RESOLVED;
				service_message.arg1=port;
				service_message.obj=hm;  //send the hashmap as the payload
				connection_msg_handler.sendMessage(service_message);

				Log.d("Engine_Driver", String.format("serviceResolved - %s(%s):%d -- %s", host_name, ip_address, port, event.toString().replace(System.getProperty("line.separator"), "")));

			};
		}

		void start_jmdns() {

			int intaddr = 0;

			//Set up to find a WiThrottle service via ZeroConf
			try   {
				WifiManager wifi = (WifiManager)threaded_application.this.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiinfo = wifi.getConnectionInfo();
				intaddr = wifiinfo.getIpAddress();
				if (intaddr != 0) {

					if (multicast_lock == null) {  //do this only as needed
						multicast_lock=wifi.createMulticastLock("engine_driver");
						multicast_lock.setReferenceCounted(true);
						multicast_lock.acquire();
					}

					byte[] byteaddr = new byte[] { (byte)(intaddr & 0xff), (byte)(intaddr >> 8 & 0xff), (byte)(intaddr >> 16 & 0xff),
							(byte)(intaddr >> 24 & 0xff) };
					Inet4Address addr = (Inet4Address) Inet4Address.getByAddress(byteaddr);
					client_address = addr.toString().substring(1);		//strip off leading /
					Log.d("Engine_Driver","start_jmdns: local IP addr " + client_address);

					jmdns=JmDNS.create(addr, client_address);  //pass ip as name to avoid hostname lookup attempt
					//    				jmdns=JmDNS.create(addr);

					listener=new withrottle_listener();

					jmdns.addServiceListener("_withrottle._tcp.local.", listener);

				} else {
					process_comm_error("No local IP Address found.\nCheck your WiFi connection.");
				}  //end of if intaddr==0
			}  catch(IOException except) { 
				Log.e("Engine_Driver", "start_jmdns - Error creating withrottle listener: "+except.getMessage()); 
				process_comm_error("Error creating withrottle listener: IOException: \n"+except.getMessage()); 
			}
		}


		//end_jmdns() takes a long time, so put it in its own thread
		void end_jmdns() {
			Thread jmdnsThread = new Thread() {
				@Override
				public void run() {
					try {
						Log.d("Engine_Driver","calling jmdns.close()");
						jmdns.close();
						Log.d("Engine_Driver","after jmdns.close()");
					} catch (Exception e) {
						Log.d("Engine_Driver","exception in jmdns.close()");
					}
					jmdns = null;
					endingJmdns = false;
				}
			};
			if (jmdnsIsActive()) {		//only need to run one instance of this thread to terminate jmdns 
				endingJmdns = true;
				jmdnsThread.start();
			}
		}

		boolean jmdnsIsActive() {
			boolean isActive = jmdns != null && !endingJmdns;
			return isActive;
		}


		class comm_handler extends Handler    {

			//All of the work of the communications thread is initiated from this function.
			public void handleMessage(Message msg)      {
				switch(msg.what)        {
				//Start or Stop the WiThrottle listener and required jmdns stuff
				case message_type.SET_LISTENER:
					//arg1= 1 to turn on, arg1=0 to turn off
					if (msg.arg1 == 0) {
						if (jmdnsIsActive()) { 
							try {
								jmdns.removeServiceListener("_withrottle._tcp.local.", listener);
								multicast_lock.release();
							}
							catch(Exception e) {	//just catch any exception and proceed to end_jmdns
							}
							end_jmdns();
						}
					} else {
						if (jmdns == null) {   //start jmdns if not started
							Log.d("Engine_Driver","comm_handler: jmdns not started, starting");
							start_jmdns();
						} else {
							Log.d("Engine_Driver","comm_handler: jmdns already running");
						}
						if (jmdns != null) {  //don't bother if jmdns didn't start
							try {
								multicast_lock.acquire();
							} catch (Exception e) {
								//keep going if this fails
							}
							jmdns.addServiceListener("_withrottle._tcp.local.", listener);
						} else {
							Log.d("Engine_Driver","comm_handler: jmdns not running, didn't start listener");
						}
					}
					break;

					//Connect to the WiThrottle server.
				case message_type.CONNECT:

					//avoid duplicate connects, seen when user clicks address multiple times quickly
					if (host_ip != null) {
						Log.d("Engine_Driver","Duplicate CONNECT message received.  Ignoring.");
						return;
					}

					//The IP address is stored in the obj as a String, the port is stored in arg1.
					host_ip=new String((String)msg.obj);
					host_ip = host_ip.trim();
					port=msg.arg1;

					//clear app.thread shared variables so they can be reset
					//            host_name_string = null;
					withrottle_version = 0.0; 
					web_server_port = 0;
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
					roster = null;
					function_labels_S = new LinkedHashMap<Integer, String>();
					function_labels_T = new LinkedHashMap<Integer, String>();
					locos_on_T = new LinkedHashMap<String, String>();
					locos_on_S = new LinkedHashMap<String, String>();
					function_labels_default = new LinkedHashMap<Integer, String>();
					function_states_T = new boolean[32];		// also allocated in onCreate() ???
					function_states_S = new boolean[32];
					consist_allowed = false;
					consist_entries = new LinkedHashMap<String, String>();

					//attempt connection to WiThrottle server
					socketWiT = new socket_WiT();
					if(socketWiT.connect() == true) {
						sendThrottleName();
						Message connection_message=Message.obtain();
						connection_message.what=message_type.CONNECTED;
						connection_msg_handler.sendMessage(connection_message);
					}
					else {
						host_ip = null;  //clear vars if failed to connect
						port = 0;
					}

					break;

					//Release the current loco
				case message_type.RELEASE:  //release specified loco
					String whichThrottle = msg.obj.toString();
					boolean doRelease = false;
					if ("T".equals(whichThrottle) && !"Not Set".equals(loco_string_T)) {
						doRelease = true;
						loco_string_T = "Not Set"; 
						//              loco_address_T = -1;
						function_labels_T = new LinkedHashMap<Integer, String>();
						function_states_T = new boolean[32];
						locos_on_T = new LinkedHashMap<String, String>();

					} else if (!"Not Set".equals(loco_string_S)) {
						doRelease = true;
						loco_string_S = "Not Set"; 
						//              loco_address_S = -1;
						function_labels_S = new LinkedHashMap<Integer, String>();
						function_states_S = new boolean[32];
						locos_on_S = new LinkedHashMap<String, String>();
					}
					if (doRelease) {
						//        	Boolean f = getApplicationContext().getResources().getBoolean(R.string.prefStopOnReleaseDefaultValue); TODO: fix this
						if (prefs.getBoolean("stop_on_release_preference", true )) {
							withrottle_send(whichThrottle+"V0");  //send stop command before releasing (if set in prefs)
						}
						withrottle_send(whichThrottle+"r");  //send release command
					}
					break;

					//request speed
				case message_type.REQ_VELOCITY:
					if (withrottle_version >= 2.0) {
						whichThrottle = msg.obj.toString();
						//               	withrottle_send(whichThrottle+"qV");
						withrottle_send("M"+whichThrottle+"A*<;>qV");
					}
					break;

					//request direction
				case message_type.REQ_DIRECTION:
					if (withrottle_version >= 2.0) {
						whichThrottle = msg.obj.toString();
						//        		withrottle_send(whichThrottle+"qR");  //request updated direction
						withrottle_send("M"+whichThrottle+"A*<;>qR");  //request updated direction
					}
					break;

					//Disconnect from the WiThrottle server.
				case message_type.DISCONNECT:
					heart.stopHeartbeat();
					withrottle_send("Q");
					alert_activities(message_type.DISCONNECT,"");
					if (heart.getInboundInterval() > 0) {
						withrottle_send("*-");     //request to turn off heartbeat (if enabled in server prefs)
					}
					try { 
						Thread.sleep(250L);			//give msgs a chance to xmit before closing socket
					}
					catch (Exception e) { 
					}
					//				heart.stopHeartbeat();
					if (socketWiT != null) socketWiT.disconnect(true);		//stop reading from the socket
					socketWiT = null;
					host_ip = null;
					break;

					//Set up an engine to control. The address of the engine is given in arg1, and the address type (long or short) is given in arg2.
					//  also add rostername at end 
				case message_type.LOCO_ADDR:
					whichThrottle = msg.obj.toString().substring(0, 1);  //first char is throttle T or S
					String pipeentry = "";  //TODO: clean up handling of pipe
					last_roster_entry_requested = "";
					if (withrottle_version >= 2.0) {  //don't pass rostername to older WiT
						pipeentry = msg.obj.toString().substring(1);  //roster name is rest of string with '|'
						if (!pipeentry.equals("")) {
							last_roster_entry_requested = pipeentry.substring(1);  //remember for use in response, stripping off bang 
						}
						if (prefs.getBoolean("drop_on_acquire_preference", false )) {
							withrottle_send(whichThrottle+"r");  //send release command for any already acquired locos (if set in prefs)
						}
}
					withrottle_send(String.format(whichThrottle+(msg.arg2==address_type.LONG ? "L" : "S")+"%d%s", msg.arg1, pipeentry));
					if (withrottle_version >= 2.0) {  //request current direction and speed (WiT 2.0+)
						withrottle_send("M" + whichThrottle+"A*<;>qV");
						withrottle_send("M" + whichThrottle+"A*<;>qR");
					}

					if (heart.getInboundInterval() > 0) {
						withrottle_send("*+");     //request to turn on heartbeat (if enabled in server prefs)
					}
					break;

					//          case message_type.ERROR:
					//            break;

					//Adjust the locomotive's speed. arg1 holds the value of the speed to set. //TODO: Allow 14 and 28 speed steps (might need a change on the server side).
				case message_type.VELOCITY:
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

					// terminate jmdns and send activities a SHUTDOWN msg
				case message_type.SHUTDOWN:
					end_jmdns();
					alert_activities(message_type.SHUTDOWN,"");	//only connection_activity should still be running
					break;
				}
			};
		}

		private void sendThrottleName() {
			sendThrottleName(true);
		}

		private void sendThrottleName(Boolean sendHWID) {
			String s = prefs.getString("throttle_name_preference", getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue));
			withrottle_send("N" + s);  //send throttle name
			if(sendHWID == true)
				withrottle_send("HU" + s);  //also send throttle name as the UDID
		}

		private void process_comm_error(String msg_txt) {
			Log.d("Engine_Driver", "comm_handler.handleMessage: " + msg_txt);
			if (connection_msg_handler!= null) {
				Message ui_msg=Message.obtain();
				ui_msg.what=message_type.ERROR;
				ui_msg.obj = new String(msg_txt);  //put error message text in message
				connection_msg_handler.sendMessage(ui_msg); //send message to ui thread for display
			}
		}

		private String setLocoString(LinkedHashMap<String,String> locos) {
			String s = "";
			String sep = "";
			for (String loco : locos.values()) {  // loop thru locos
				s += sep + loco;
				sep = " +";
			}
			if (s.equals("")) {
				s = "Not Set";
			}
			return s;
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

			//handle responses from MultiThrottle function
			case 'M': 
				//loco was successfully added to a throttle
				if 	(response_str.charAt(2) == '+') {  //"MT+L2591<;>"  loco was added
					char whichThrottle = response_str.charAt(1);
					String[] ls = splitByString(response_str.substring(3),"<;>");//drop off separator
					String rosterName = "rostername";
					String addr = ls[0].substring(1) + "(" + ls[0].substring(0,1) + ")";  //reformat from L2591 to 2591(L)  
					if (last_roster_entry_requested.equals("")) {  //use remembered name, or look up from address
						//look up name from address
						rosterName = get_loconame_from_address_string(addr);  //lookup name in roster
					} else {
						rosterName = last_roster_entry_requested;
					}

					if 	(whichThrottle == 'T') {
//						if ("Not Set".equals(loco_string_T)) {  
//							loco_string_T = ""; 
//						} else {
//							loco_string_T += " +"; 
//						}
//						loco_string_T += rosterName;  //append new loco to app variable
						locos_on_T.put(addr, rosterName);  //add new loco 
						loco_string_T = setLocoString(locos_on_T);  //reformat changed loco string
					} else {
						locos_on_S.put(addr, rosterName);
						loco_string_S = setLocoString(locos_on_S);
					}
				} else if (response_str.charAt(2) == '-'){ //"MS-L6318<;>"  loco removed from throttle
					char whichThrottle = response_str.charAt(1);
					String[] ls = splitByString(response_str.substring(3),"<;>");//drop off separator
					String addr = ls[0].substring(1) + "(" + ls[0].substring(0,1) + ")";  //reformat from L2591 to 2591(L)  
					if (whichThrottle == 'T') {
						locos_on_T.remove(addr);
						loco_string_T = setLocoString(locos_on_T);  //reformat changed loco string
					} else {
						locos_on_S.remove(addr);
						loco_string_S = setLocoString(locos_on_S);  //reformat changed loco string
					}
					Log.d("Engine_Driver", "loco " + addr + " dropped from " + whichThrottle);

				} else if (response_str.charAt(2) == 'L'){ //list of function buttons
					String[] ls = splitByString(response_str,"<;>");//drop off front portion
					process_roster_function_string("RF29}|{1234(L)" + ls[1], response_str.substring(1,2));  //prepend some stuff to match old-style

				} else if (response_str.charAt(2) == 'A'){ //process change in function value  MTAL4805<;>F028
					String whichThrottle = response_str.substring(1,2);
					String[] ls = threaded_application.splitByString(response_str,"<;>");
					if ("F".equals(ls[1].substring(0,1))) {
						process_function_state_20(whichThrottle, new Integer(ls[1].substring(2)), "1".equals(ls[1].substring(1,2)) ? true : false);  
					}	    	  			
				}

				break;

			case 'T': 
				loco_string_T = get_loconame_from_address_string(response_str.substring(1));  //set app variable
				Log.d("Engine_Driver", "comm_handler loco MT "+loco_string_T);
				break;

			case 'S': 
				loco_string_S = get_loconame_from_address_string(response_str.substring(1));  //set app variable
				break;

			case 'V': 
				try {
					withrottle_version= Double.parseDouble(response_str.substring(2));
				} 
				catch (Exception e) {
					Log.d("Engine_Driver", "process response: invalid WiT version string");
					withrottle_version = 0.0;
				}
				break;

			case '*': 
				try {
					heartbeatInterval = Integer.parseInt(response_str.substring(1));  //set app variable
				} catch (Exception e) {
					Log.d("Engine_Driver", "process response: invalid WiT hearbeat string");
					heartbeatInterval = 0;
				}
				heart.startHeartbeat(heartbeatInterval);
				break;

			case 'R': //Roster
				switch (response_str.charAt(1)) {

				case 'C': 
					if 	(response_str.charAt(2) == 'C' || response_str.charAt(2) == 'L') {  //RCC1 or RCL1 (treated the same in ED)
						consist_allowed = true;  //set app variable
						clear_consist_list();
					} else	if 	(response_str.charAt(2) == 'D') {  //RCD}|{88(S)}|{88(S)]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
						process_consist_list(response_str);
					}

					break;

				case 'L': 
					roster_list_string = response_str.substring(2);  //set app variable
					process_roster_list(response_str);  //process roster list
					break;

				case 'F':   //RF29}|{2591(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
					process_roster_function_string(response_str.substring(2), "T");
					break;

				case 'S': //RS29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
					process_roster_function_string(response_str.substring(2), "S");
					break;

				case 'P': //Properties   RPF}|{whichThrottle]\[function}|{state]\[function}|{state...
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

				case 'W':  //Web Server port 
					try {
						web_server_port = Integer.parseInt(response_str.substring(2));  //set app variable
					} 
					catch (Exception e) {
						Log.d("Engine_Driver", "process response: invalid web server port string");
						web_server_port = 0;
					}

					break;
				}  //end switch inside P
				break;
			}  //end switch

			alert_activities(message_type.RESPONSE, response_str);	//send response to running activities
		}  //end of process_response

		//parse roster functions list into appropriate app variable array
		//  //RF29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
		private void process_roster_function_string(String response_str, String whichThrottle) {

			Log.d("Engine_Driver", "processing function labels for " + whichThrottle);
			//clear the appropriate global variable
			if ("T".equals(whichThrottle)) {
				function_labels_T = new LinkedHashMap<Integer, String>();
			} else {
				function_labels_S = new LinkedHashMap<Integer, String>();
			}

			String[] ta = splitByString(response_str,"]\\[");  //split into list of labels
			//initialize app arrays (skipping first)
			int i = 0;
			for (String ts : ta) {
				if (i > 0 && !"".equals(ts)) { //skip first chunk, which is length, and skip any blank entries
					if ("T".equals(whichThrottle)) {  //populate the appropriate hashmap
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
					try {
						roster_entries.put(tv[0],tv[1]+"("+tv[2]+")"); //roster name is hashmap key, value is address(L or S), e.g.  2591(L)
					} catch (Exception e) {
						Log.d("Engine_Driver", "process_roster_list caught Exception");  //ignore any bad stuff in roster entries
					}
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
			if (!consist_name.equals(consist_addr) && (consist_name.length() > 4)) { 
				consist_desc = consist_name; // use entered name if significant
			}
			consist_entries.put(consist_addr, consist_desc); 
		}

		//clear out any stored consists
		private void clear_consist_list() {
			consist_entries.clear();
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
		//  PRL[<SystemName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
		//  PRL]\[LT12}|{my12}|{1
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

		//parse function state string into appropriate app variable array (format for WiT < 2.0)
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

					if ("T".equals(whichThrottle)) {
						function_states_T[fn] = fState;
					}  else {
						function_states_S[fn] = fState;
					}
				}  //end if i==0
				i++;
			}  //end for
		}

		//parse function state string into appropriate app variable array (format for WiT >= 2.0)
		private void process_function_state_20(String whichThrottle, Integer fn, boolean fState) {

			if ("T".equals(whichThrottle)) {
				function_states_T[fn] = fState;
			}  else {
				function_states_S[fn] = fState;
			}
		}

		// forward a message to all running activities 
		private void alert_activities(int msgType, String msgBody) {
			if (connection_msg_handler != null)   
			{ 
				Message msg=Message.obtain(); 
				msg.what=msgType;
				msg.obj=new String(msgBody);
				try {
					connection_msg_handler.sendMessage(msg);
				}
				catch(Exception e) {
					msg.recycle();
				}
			}
			if (turnouts_msg_handler != null)   
			{ 
				Message msg=Message.obtain(); 
				msg.what=msgType;
				msg.obj=new String(msgBody);
				try {
					turnouts_msg_handler.sendMessage(msg);
				}
				catch(Exception e) {
					msg.recycle();
				}
			}
			if (routes_msg_handler != null)   { 
				Message msg=Message.obtain(); 
				msg.what=msgType;
				msg.obj=new String(msgBody); 
				try {
					routes_msg_handler.sendMessage(msg);
				}
				catch(Exception e) {
					msg.recycle();
				}
			}
			if (throttle_msg_handler != null) {
				Message msg=Message.obtain(); 
				msg.what=msgType;
				msg.obj=new String(msgBody);
				try {
					throttle_msg_handler.sendMessage(msg);
				}
				catch(Exception e) {
					msg.recycle();
				}
			} else {
				Log.d("Engine_Driver", "Throttle activity not active, did not forward: " + msgBody);
			}
			if (power_control_msg_handler != null) { 
				Message msg=Message.obtain(); 
				msg.what=msgType;
				msg.obj=new String(msgBody); 
				try {
					power_control_msg_handler.sendMessage(msg);
				}
				catch(Exception e) {
					msg.recycle();
				}
			}

			if (select_loco_msg_handler != null) { 
				Message msg=Message.obtain(); 
				msg.what=msgType;
				msg.obj=new String(msgBody); 
				try {
					select_loco_msg_handler.sendMessage(msg);
				}
				catch(Exception e) {
					msg.recycle();
				}
			}
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
			//    	Log.d("Engine_Driver", "WiT send " + msg);    	
			String newMsg = msg;
			boolean validMsg = (newMsg != null);
			if(!validMsg) {
				Log.d("Engine_Driver", "--> null msg");
			}
			//convert msg to new MultiThrottle format if version >= 2.0
			else if (withrottle_version >= 2.0) {
				try {
					if ("T".equals(msg.substring(0,1)) || "S".equals(msg.substring(0,1))) {
						//acquire loco
						if ("L".equals(msg.substring(1,2)) || "S".equals(msg.substring(1,2))) { //address length
							String[] as = splitByString(msg.substring(1),"|");//split off rostername
							String address = as[0];
							String rostername = "";
							if (as.length == 1) {  //if rostername found, use it, else use address again
								rostername = as[0];
							} else {
								rostername = new String("E" + as[1]);  //use E to indicate rostername
							}
							newMsg = "M" + msg.substring(0,1) + "+" + address + "<;>" + rostername;  //add requested loco to this throttle
							//release loco(s)
						} else if ("r".equals(msg.substring(1,2))) {
							newMsg = "M" + msg.substring(0,1) + "-*<;>r";  //release all locos from this throttle
							//anything else (speed, direction, functions, etc.)
						} else {
							newMsg = "M" + msg.substring(0,1) + "A*<;>" + msg.substring(1);  //pass all action commands along
						}
					}
				}
				catch(Exception e) {
					Log.d("Engine_Driver", "--> invalid msg: " + newMsg);
					validMsg = false;
				}
			}

			if (validMsg) {
				//send response to debug log for review
				Log.d("Engine_Driver", "-->:" + newMsg + "  was(" + msg + ")");
				//perform the send
				if(socketWiT != null)
					socketWiT.Send(newMsg);
			}

			//        start_read_timer(busyReadDelay);
		}  //end withrottle_send()

		public void run()   {

			Looper.prepare();
			comm_msg_handler=new comm_handler();
			Looper.loop();
		};

		class socket_WiT extends Thread {
			protected InetAddress host_address;
			protected Socket clientSocket = null;
			protected BufferedReader inputBR = null;
			protected PrintWriter outputPW = null;
			private boolean endRead = false;			//signals rcvr to terminate
			private boolean socketGood = false;			//indicates socket condition

			socket_WiT() {
				super("socket_WiT");
			}

			public boolean connect() {

				//use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done
				boolean socketOk = HaveNetworkConnection();	

				//validate address
				if (socketOk) {
					try { 
						host_address=InetAddress.getByName(host_ip); 
					}
					catch(UnknownHostException except) {
						process_comm_error("Can't determine IP address of " + host_ip);
						socketOk = false;
					}
				}

				//            host_name_string = host_address.getHostName();  //store host server name in app.thread shared variable

				//socket
				if (socketOk) {
					try {
						clientSocket=new Socket();               //look for someone to answer on specified socket, and set timeout
						InetSocketAddress sa = new InetSocketAddress(host_ip, port);
						clientSocket.connect(sa, 3000);  //TODO: adjust these timeouts, or set in prefs
						clientSocket.setSoTimeout(500);
					}
					catch(Exception except)  {
						process_comm_error("Can't connect to host "+host_ip+" and port "+port+
								" from " + client_address +
								" - "+except.getMessage()+"\nCheck WiThrottle and network settings.");
						socketOk = false;
					}
				}

				//rcvr
				if (socketOk) {
					try {
						inputBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					}
					catch (IOException except) {
						process_comm_error("Error creating input stream, IOException: "+except.getMessage());
						socketOk = false;
					} 
				}

				//start the socket_WiT thread.
				if (socketOk) {
					if (!this.isAlive()) {
						endRead = false;
						try {
							this.start();
						} catch (IllegalThreadStateException except) {
							//ignore "already started" errors
							process_comm_error("Error starting socket_WiT thread:  "+except.getMessage());
						}
					}
				}

				//xmtr
				if (socketOk) {
					try { 
						outputPW = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true); 
					}
					catch(IOException e) {
						process_comm_error("Error creating output stream, IOException: "+e.getMessage());
						socketOk = false;
					}
				}
				socketGood = socketOk;
				return socketGood;
			}

			public void disconnect(boolean shutdown) {
				if (shutdown) {
					endRead = true;
					for(int i = 0; i < 3 && this.isAlive(); i++) {
						try { 
							Thread.sleep(300);    			//  give run() a chance to exit
						}
						catch (InterruptedException e) { 
							process_comm_error("Error sleeping the thread, InterruptedException: "+e.getMessage());
						}
					}
				}

				socketGood = false;

				//close socket
				if (clientSocket != null) {
					try { 
						clientSocket.close();
					}
					catch(Exception e) { 
						Log.d("Engine_Driver","Error closing the Socket: "+e.getMessage()); 
					}
				}

				//			if (!shutdown) {
				{
					// reinit shared variables then signal activities to refresh their views
					// so that (potentially) invalid information is not displayed
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
					loco_string_T = "Not Set";
					loco_string_S = "Not Set";
					locos_on_T = new LinkedHashMap<String, String>();
					locos_on_S = new LinkedHashMap<String, String>();
					function_labels_S = new LinkedHashMap<Integer, String>();
					function_labels_T = new LinkedHashMap<Integer, String>();
					function_labels_default = new LinkedHashMap<Integer, String>();
					function_states_T = new boolean[32];		// also allocated in onCreate() ???
					function_states_S = new boolean[32];
					consist_allowed = false;
					consist_entries = new LinkedHashMap<String, String>();
					roster_entries = null;

					if (turnouts_msg_handler != null)   
					{ 
						Message msg=Message.obtain(); 
						msg.what=message_type.RESPONSE;
						msg.obj=new String("PTL");		//tell turnout activity that there is a new turnout list
						try {
							turnouts_msg_handler.sendMessage(msg);
						}
						catch(Exception e) {
							msg.recycle();
						}
					}
					if (routes_msg_handler != null)   { 
						Message msg=Message.obtain(); 
						msg.what=message_type.RESPONSE;
						msg.obj=new String("PRL"); 		//tell route activity that there is a new route list
						try {
							routes_msg_handler.sendMessage(msg);
						}
						catch(Exception e) {
							msg.recycle();
						}
					}
					if (throttle_msg_handler != null) {
						Message msg=Message.obtain(); 
						msg.what=message_type.RESPONSE;
						msg.obj=new String("MT-");		//tell throttle activity to release throttle T
						try {
							throttle_msg_handler.sendMessage(msg);
						}
						catch(Exception e) {
							msg.recycle();
						}
						msg=Message.obtain(); 
						msg.what=message_type.RESPONSE;
						msg.obj=new String("MS-");		//tell throttle activity to release throttle S
						try {
							throttle_msg_handler.sendMessage(msg);
						}
						catch(Exception e) {
							msg.recycle();
						}
					}
					if (power_control_msg_handler != null) { 
						Message msg=Message.obtain(); 
						msg.what=message_type.RESPONSE;
						msg.obj=new String("PPA"); 		//tell power activity that the power state has changed
						try {
							power_control_msg_handler.sendMessage(msg);
						}
						catch(Exception e) {
							msg.recycle();
						}
					}
					if (select_loco_msg_handler != null) { 
						Message msg=Message.obtain(); 
						msg.what=message_type.RESPONSE;
						msg.obj=new String("R"); 		//tell select loco activity that the roster has changed
						try {
							select_loco_msg_handler.sendMessage(msg);
						}
						catch(Exception e) {
							msg.recycle();
						}
					}
				}
			}

			//read the input buffer
			public void run() {

				//       	Looper.prepare();

				String str = null;
				//continue reading until signalled to exit by endRead
				while(!endRead) {
					if(socketGood) {		//skip read when the socket is down
						try {
							if((str = inputBR.readLine()) != null) {
								if (str.length()>0) {
									heart.restartInboundInterval();
									process_response(str);
								}
							}
						} 
						catch (SocketTimeoutException e )   {
							socketGood = this.SocketCheck();
						} 
						catch (IOException e) {
							if(socketGood) {
								Log.d("Engine_Driver","WiT rcvr error.");
								socketGood = false;		//input buffer error so force reconnection on next send
							}
						}
					}
					if(!socketGood) {
						SystemClock.sleep(1000L);	//don't become compute bound here when the socket is down
					}
				}
				//        	Looper.loop();  
			}

			public void Send(String msg) {
				//reconnect socket if needed
				if(!socketGood || !this.SocketCheck()) {
					this.disconnect(false);		//clean up socket but do not shut down the receiver
					this.connect();				//attempt to reestablish connection
					if(socketGood) {
						process_comm_error("Success: Restored connection to WiThrottle server " + host_ip + ".\n");
					}
					else {
						process_comm_error("Warning: Lost connection to WiThrottle server " + host_ip + ".\nAttempting to reconnect.");
						comm_msg_handler.postDelayed(heart.outboundHeartbeatTimer, 5000L);	//try connection again in 5 seconds
					}
				}

				//send the message
				if (socketGood) {
					try {
						outputPW.println(msg);
						outputPW.flush();
					} 
					catch (Exception e) {
						Log.d("Engine_Driver","WiT xmtr error.");
						socketGood = false;		//output buffer error so force reconnection on next send
					}
				}
			}

			// attempt to determine if the socket connection is still good
			public boolean SocketCheck() {
				boolean status = clientSocket.isConnected() && !clientSocket.isInputShutdown() && !clientSocket.isOutputShutdown();
				if (status)
					status = HaveNetworkConnection();	// can't trust the socket flags so try something else...
				return status;
			}

			// temporary - SocketCheck should determine whether socket connection is good however socket flags sometimes do not get updated
			// so it doesn't work.  This is better than nothing though?
			private boolean HaveNetworkConnection()
			{
				boolean haveConnectedWifi = false;
				boolean haveConnectedMobile = false;

				ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo[] netInfo = cm.getAllNetworkInfo();
				for (NetworkInfo ni : netInfo)
				{
					if ("WIFI".equalsIgnoreCase(ni.getTypeName()))
						if (ni.isConnected())
							haveConnectedWifi = true;
					if ("MOBILE".equalsIgnoreCase(ni.getTypeName()))
						if (ni.isConnected())
							haveConnectedMobile = true;
				}
				return haveConnectedWifi || haveConnectedMobile;
			}
		}

		class heartbeat {
			//	outboundHeartbeat - send a periodic heartbeat to WiT to show that ED is alive.
			//
			//	inboundHeartbeat - WiT doesn't send a heartbeat to ED, so send a periodic message to WiT that requires a response.
			//
			//	If the WiT heartbeat interval = 0 then heartbeat checking is disabled.
			//
			//	Else 
			//		If the WiT heartbeat is >= (MIN_OUTBOUND_HEARTBEAT_INTERVAL + HEARTBEAT_RESPONSE_ALLOWANCE) 
			//		then the outbound heartbeat rate to (WiT heartbeat - HEARTBEAT_RESPONSE_ALLOWANCE)
			//
			//		Else the outbound heartbeat rate to	MIN_OUTBOUND_HEARTBEAT_INTERVAL.
			//
			//		The inbound heartbeat rate is set to (outbound heartbeat rate + HEARTBEAT_RESPONSE_ALLOWANCE)

			private int heartbeatIntervalSetpoint = 0;		//WiT heartbeat interval in seconds
			private int heartbeatOutboundInterval = 0;		//sends outbound heartbeat message at this rate
			private int heartbeatInboundInterval = 0;		//alerts user if there was no inbound traffic for this long


			public int getInboundInterval() {
				return heartbeatInboundInterval;
			}
			public int getOutboundInterval() {
				return heartbeatOutboundInterval;
			}

			//startHeartbeat(timeoutInterval in seconds)
			//calcs the inbound and outbound intervals and starts the beating
			public void startHeartbeat(int timeoutInterval) {
				//update interval timers only when the heartbeat timeout interval changed
				if(timeoutInterval != heartbeatIntervalSetpoint)
				{
					heartbeatIntervalSetpoint = timeoutInterval;
					if(heartbeatIntervalSetpoint == 0) {	//heartbeat is disabled
						heartbeatOutboundInterval = 0;
						heartbeatInboundInterval = 0;
					}
					else {
						heartbeatOutboundInterval = heartbeatIntervalSetpoint - HEARTBEAT_RESPONSE_ALLOWANCE;
						if(heartbeatOutboundInterval < MIN_OUTBOUND_HEARTBEAT_INTERVAL)
							heartbeatOutboundInterval = MIN_OUTBOUND_HEARTBEAT_INTERVAL;

						heartbeatInboundInterval = heartbeatOutboundInterval + HEARTBEAT_RESPONSE_ALLOWANCE;

						heartbeatOutboundInterval *= 1000;		//convert to milliseconds
						heartbeatInboundInterval *= 1000;		//convert to milliseconds
					}
					restartOutboundInterval();
					restartInboundInterval();
				}
			}

			//restartOutboundInterval()
			//restarts the outbound interval timing - call this after sending anything to WiT that requires a response
			public void restartOutboundInterval() {
				comm_msg_handler.removeCallbacks(outboundHeartbeatTimer);					//remove any pending requests
				if(heartbeatOutboundInterval > 0) {
					comm_msg_handler.postDelayed(outboundHeartbeatTimer, heartbeatOutboundInterval);	//restart interval
				}
			}

			//restartInboundInterval()
			//restarts the inbound interval timing - call this after receiving anything from WiT
			public void restartInboundInterval() {
				comm_msg_handler.removeCallbacks(inboundHeartbeatTimer);
				if(heartbeatInboundInterval > 0) {
					comm_msg_handler.postDelayed(inboundHeartbeatTimer, heartbeatInboundInterval);
				}
			}

			public void stopHeartbeat() {
				comm_msg_handler.removeCallbacks(outboundHeartbeatTimer);			//remove any pending requests
				comm_msg_handler.removeCallbacks(inboundHeartbeatTimer);
				heartbeatIntervalSetpoint = 0;
			}

			public void sendHeartbeat() {
				comm_msg_handler.post(outboundHeartbeatTimer);
			}

			//outboundHeartbeatTimer()
			//sends a periodic message to WiT
			private Runnable outboundHeartbeatTimer = new Runnable() {
				@Override
				public void run() {
					comm_msg_handler.removeCallbacks(this);				//remove pending requests
					if(heartbeatOutboundInterval != 0) {
						boolean anySent = false;
						if (withrottle_version >= 2.0) { 
							if (!loco_string_T.equals("Not Set")) {  
								withrottle_send("MTA*<;>qV"); //request speed
								withrottle_send("MTA*<;>qR"); //request direction
								anySent = true;
							}
							if (!loco_string_S.equals("Not Set")) {  
								withrottle_send("MSA*<;>qV"); //request speed
								withrottle_send("MSA*<;>qR"); //request direction
								anySent = true;
							}
						}
						if (!anySent) {
							sendThrottleName(false);	//send message that will get a response
						}
						comm_msg_handler.postDelayed(this,heartbeatOutboundInterval);	//set next beat
					}
				}
			};

			//inboundHeartbeatTimer()
			//display an alert message when there is no inbound traffic from WiT within required interval 
			private Runnable inboundHeartbeatTimer = new Runnable() {
				@Override
				public void run() {
					comm_msg_handler.removeCallbacks(this);	//remove pending requests
					if(heartbeatInboundInterval != 0) {
						if (socketWiT != null && socketWiT.socketGood)
							process_comm_error("WARNING: No response from WiThrottle server for " + (heartbeatInboundInterval/1000)  + " seconds.  Verify connection.");
						comm_msg_handler.postDelayed(this,heartbeatInboundInterval);	//set next inbound timeout
					}
				}
			};
		}
	}

	public void onCreate()  {
		super.onCreate();

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

