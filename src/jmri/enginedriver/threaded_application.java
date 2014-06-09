/*Copyright (C) 2013 M. Steve Todd
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

// Main java file.
/* TODO: see changelog-and-todo-list.txt for complete list of project to-do's */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import java.net.*;
import java.io.*;

import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import javax.jmdns.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketHandler;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;

import jmri.enginedriver.message_type;
import jmri.enginedriver.threaded_application.comm_thread.comm_handler;
import jmri.enginedriver.Consist;
import jmri.jmrit.roster.RosterEntry;
import jmri.jmrit.roster.RosterLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

//The application will start up a thread that will handle network communication in order to ensure that the UI is never blocked.
//This thread will only act upon messages sent to it. The network communication needs to persist across activities, so that is why
@SuppressLint("NewApi")
public class threaded_application extends Application {
	public comm_thread commThread;
	String host_ip = null; //The IP address of the WiThrottle server.
	volatile int port = 0; //The TCP port that the WiThrottle server is running on
	Double withrottle_version = 0.0; //version of withrottle server
	private int web_server_port = 0; //default port for jmri web server
	private volatile boolean doFinish = false;	// when true, tells any Activities that are being created/resumed to finish()
	//shared variables returned from the withrottle server, stored here for easy access by other activities
	volatile Consist consistT;
	volatile Consist consistS;
	volatile Consist consistG;
	LinkedHashMap<Integer, String> function_labels_T;  //function#s and labels from roster for throttle #1
	LinkedHashMap<Integer, String> function_labels_S;  //function#s and labels from roster for throttle #2
	LinkedHashMap<Integer, String> function_labels_G;  //function#s and labels from roster for throttle #3
	LinkedHashMap<Integer, String> function_labels_default;  //function#s and labels from local settings
	boolean[] function_states_T;  //current function states for first throttle
	boolean[] function_states_S;  //current function states for second throttle
	boolean[] function_states_G;  //current function states for second throttle
	String[] to_system_names;
	String[] to_user_names;
	String[] to_states;
	HashMap<String, String> to_state_names;
	String[] rt_system_names;
	String[] rt_user_names;
	String[] rt_states;
	HashMap<String, String> rt_state_names;
	HashMap<String, String> roster_entries;  //roster sent by WiThrottle
	LinkedHashMap<String, String> consist_entries;
	private static DownloadRosterTask dlRosterTask = null;
	private static DownloadMetaTask dlMetadataTask = null;
	HashMap<String, RosterEntry> roster;  //roster entries retrieved from roster.xml (null if not retrieved)
	public static HashMap<String, String> metadata;  //metadata values (such as JMRIVERSION) retrieved from web server (null if not retrieved)
	ImageDownloader imageDownloader = new ImageDownloader();
	String power_state;
	public boolean displayClock = false;
	public int androidVersion = 0;
	public final int minWebSocketVersion = 8;				//minimum Android version for Autobahn websocket library

	static final int MIN_OUTBOUND_HEARTBEAT_INTERVAL = 2;	//minimum interval for outbound heartbeat generator
	static final int HEARTBEAT_RESPONSE_ALLOWANCE = 4;		//worst case time delay for WiT to respond to a heartbeat message
	public int heartbeatInterval = 0;						//WiT heartbeat interval setting

	String client_address; //address string of the client address
	//For communication to the comm_thread.
	public comm_handler comm_msg_handler = null;
	//For communication to each of the activities (set and unset by the activity)
	public volatile Handler connection_msg_handler;
	public volatile Handler throttle_msg_handler;
	public volatile Handler web_msg_handler;
	public volatile Handler select_loco_msg_handler;
	public volatile Handler turnouts_msg_handler;
	public volatile Handler routes_msg_handler;
	public volatile Handler consist_edit_msg_handler;
	public volatile Handler power_control_msg_handler;

	//these constants are used for onFling
	public static final int SWIPE_MIN_DISTANCE = 120;
	public static final int SWIPE_MAX_OFF_PATH = 250;
	public static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private static final int ED_NOTIFICATION_ID = 0;
	public static int min_fling_distance;			// pixel width needed for fling
	public static int min_fling_velocity;			// velocity needed for fling

	private SharedPreferences prefs;

	public boolean EStopActivated = false;  // Used to determine if user pressed the EStop button.

	//Used to tell set_Labels in Throttle not to update padding for throttle sliders after onCreate.
	public boolean firstCreate = true;

	class comm_thread extends Thread  {
		JmDNS jmdns = null;
		volatile boolean endingJmdns = false;
		withrottle_listener listener;
		android.net.wifi.WifiManager.MulticastLock multicast_lock;
		socket_WiT socketWiT;
        ClockWebSocketHandler clockWebSocket = null;
		heartbeat heart = new heartbeat();
		volatile String currentTime = "";

		comm_thread() {
			super("comm_thread");
		}

		//Listen for a WiThrottle service advertisement on the LAN.
		public class withrottle_listener implements ServiceListener    {

			public void serviceAdded(ServiceEvent event)  	{
				//    		Log.d("Engine_Driver", String.format("serviceAdded fired"));
				//A service has been added. If no details, ask for them 
				Log.d("Engine_Driver", String.format("serviceAdded for '%s', Type='%s'", event.getName(), event.getType()));
				ServiceInfo si = jmdns.getServiceInfo(event.getType(), event.getName(), 0);
				if (si == null || si.getPort() == 0 ) { 
					Log.d("Engine_Driver", String.format("serviceAdded, requesting details: '%s', Type='%s'", event.getName(), event.getType()));
					jmdns.requestServiceInfo(event.getType(), event.getName(), true, (long)1000);
				}
			};

			public void serviceRemoved(ServiceEvent event)      {
				//Tell the UI thread to remove from the list of services available.
				sendMsg(connection_msg_handler, message_type.SERVICE_REMOVED, event.getName());	//send the service name to be removed
				Log.d("Engine_Driver", String.format("serviceRemoved: '%s'", event.getName()));
			};

			public void serviceResolved(ServiceEvent event)  {
				//    		Log.d("Engine_Driver", String.format("serviceResolved fired"));
				//A service's information has been resolved. Send the port and service name to connect to that service.
				int port=event.getInfo().getPort();
				String host_name = event.getInfo().getName(); //
				Inet4Address[] ip_addresses = event.getInfo().getInet4Addresses();  //only get ipV4 address
				String ip_address = ip_addresses[0].toString().substring(1);  //use first one, since WiThrottle is only putting one in (for now), and remove leading slash

				//Tell the UI thread to update the list of services available.
				HashMap<String, String> hm=new HashMap<String, String>();
				hm.put("ip_address", ip_address);
				hm.put("port", ((Integer)port).toString());
				hm.put("host_name",host_name);

				Message service_message=Message.obtain();
				service_message.what=message_type.SERVICE_RESOLVED;
				service_message.arg1=port;
				service_message.obj=hm;  //send the hashmap as the payload
				boolean sent = false;
				try {
					sent = connection_msg_handler.sendMessage(service_message);
				}
				catch(Exception e) {
				}
				if(!sent)
					service_message.recycle();

				Log.d("Engine_Driver", String.format("serviceResolved - %s(%s):%d -- %s", host_name, ip_address, port, event.toString().replace(System.getProperty("line.separator"), " ")));

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
					}

					byte[] byteaddr = new byte[] { (byte)(intaddr & 0xff), (byte)(intaddr >> 8 & 0xff), (byte)(intaddr >> 16 & 0xff),
							(byte)(intaddr >> 24 & 0xff) };
					Inet4Address addr = (Inet4Address) Inet4Address.getByAddress(byteaddr);
					client_address = addr.toString().substring(1);		//strip off leading /
					Log.d("Engine_Driver","start_jmdns: local IP addr " + client_address);

					jmdns=JmDNS.create(addr, client_address);  //pass ip as name to avoid hostname lookup attempt

					listener=new withrottle_listener();
					Log.d("Engine_Driver","start_jmdns: listener created");

				} else {
					process_comm_error("No local IP Address found.\nCheck your WiFi connection.");
				}  //end of if intaddr!=0
			}  catch(Exception except) { 
				Log.e("Engine_Driver", "start_jmdns - Error creating withrottle listener: "+except.getMessage()); 
				process_comm_error("Error creating withrottle zeroconf listener: IOException: \n"+except.getMessage()); 
			}
		}

		//end_jmdns() takes a long time, so put it in its own thread
		void end_jmdns() {
			Thread jmdnsThread = new Thread() {
				@Override
				public void run() {
					try {
						Log.d("Engine_Driver","removing jmdns listener");
						jmdns.removeServiceListener("_withrottle._tcp.local.", listener);
						multicast_lock.release();
					}
					catch(Exception e) {
						Log.d("Engine_Driver","exception in jmdns.removeServiceListener()");
					}
					try {
						Log.d("Engine_Driver","calling jmdns.close()");
						jmdns.close();
						Log.d("Engine_Driver","after jmdns.close()");
					} 
					catch (Exception e) {
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


		@SuppressLint("HandlerLeak")
		class comm_handler extends Handler    {
			//All of the work of the communications thread is initiated from this function.
			/***future PowerLock
  			private PowerManager.WakeLock wl = null;
			 */
			public void handleMessage(Message msg)      {
				switch(msg.what)        {
				//Start or Stop the WiThrottle listener and required jmdns stuff
				case message_type.SET_LISTENER:
					//arg1= 1 to turn on, arg1=0 to turn off
					if (msg.arg1 == 0) {
						end_jmdns();
					} 
					else {
						if (jmdns == null) {   //start jmdns if not started
							//							Log.d("Engine_Driver","comm_handler: jmdns not started, starting");
							start_jmdns();
							if (jmdns != null) {  //don't bother if jmdns didn't start
								try {
									multicast_lock.acquire();
								} catch (Exception e) {
									//log message, but keep going if this fails
									Log.d("Engine_Driver","comm_handler: multicast_lock.acquire() failed");
								}
								jmdns.addServiceListener("_withrottle._tcp.local.", listener);
								Log.d("Engine_Driver","comm_handler: jmdns listener added");
							} else {
								Log.d("Engine_Driver","comm_handler: jmdns not running, didn't start listener");
							}
						} else {
							Log.d("Engine_Driver","comm_handler: jmdns already running");
						}
					}
					break;

					//Connect to the WiThrottle server.
				case message_type.CONNECT:

					//avoid duplicate connects, seen when user clicks address multiple times quickly
					if (socketWiT != null && socketWiT.socketGood) {
						Log.d("Engine_Driver","Duplicate CONNECT message received.");
						return;
					}

					//clear app.thread shared variables so they can be reinitialized
					initShared();

					//The IP address is stored in the obj as a String, the port is stored in arg1.
					host_ip = new String((String)msg.obj);
					host_ip = host_ip.trim();
					port = msg.arg1;

					//attempt connection to WiThrottle server
					socketWiT = new socket_WiT();
					if(socketWiT.connect() == true) {
						sendThrottleName();
						sendMsg(connection_msg_handler, message_type.CONNECTED);
						/***future Notification
  				    	showNotification();
						 ***/
						/***future	PowerLock
 			    		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				    	wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Engine_Driver");
			    		wl.acquire();
						 ***/			    	
					}
					else {
						host_ip = null;  //clear vars if failed to connect
						port = 0;
					}
					currentTime = "";
					break;

					//Release one or all locos on the specified throttle.  addr is in msg (""==all), arg1 holds whichThrottle.
				case message_type.RELEASE: {
					String addr = msg.obj.toString();
					final char whichThrottle = (char) msg.arg1;
					final boolean releaseAll = (addr.length() == 0);
					if (whichThrottle == 'T') {
						if(releaseAll || consistT.isEmpty()) {
							addr = "";
							function_labels_T = new LinkedHashMap<Integer, String>();
							function_states_T = new boolean[32];
						}
					} 
					else if(whichThrottle == 'G') {
						if (releaseAll || consistG.isEmpty()) {
							addr = "";
							function_labels_G = new LinkedHashMap<Integer, String>();
							function_states_G = new boolean[32];
						}
					}
					else {
						if (releaseAll || consistS.isEmpty()) {
							addr = "";
							function_labels_S = new LinkedHashMap<Integer, String>();
							function_states_S = new boolean[32];
						}
					}
					if (prefs.getBoolean("stop_on_release_preference", 
							getResources().getBoolean(R.bool.prefStopOnReleaseDefaultValue))) {
						withrottle_send(whichThrottle+"V0"+"<;>"+addr);  //send stop command before releasing (if set in prefs)
					}
					withrottle_send(whichThrottle+"r<;>"+addr);  //send release command
					break;
				}
				//request speed. arg1 holds whichThrottle
				case message_type.REQ_VELOCITY: {
					final char whichThrottle = (char) msg.arg1;
					withrottle_send(whichThrottle+"qV");
					break;
				}

				//request direction.  arg1 hold whichThrottle
				case message_type.REQ_DIRECTION: {
					final char whichThrottle = (char) msg.arg1;
					//        		withrottle_send(whichThrottle+"qR");  //request updated direction
					withrottle_send(whichThrottle+"qR");  //request updated direction
					break;
				}

				//Disconnect from the WiThrottle server.
				case message_type.DISCONNECT: {
					Log.d("Engine_Driver","TA Disconnect");
					doFinish = true;
					heart.stopHeartbeat();
					withrottle_send("Q");
					if (heart.getInboundInterval() > 0) {
						withrottle_send("*-");     //request to turn off heartbeat (if enabled in server prefs)
					}
					end_jmdns();
					dlMetadataTask.stop();
					dlRosterTask.stop();
                    if (clockWebSocket != null) {
                   		clockWebSocket.disconnect();
                        clockWebSocket = null;
                    }
					/***future Notification
					hideNotification();
					 ***/					
					alert_activities(message_type.SHUTDOWN,"");	//tell all activities to finish()
					/***future PowerLock
					if(wl != null && wl.isHeld())
						wl.release();
					 ***/						
					//give msgs a chance to xmit before closing socket
					if(!sendMsgDelay(comm_msg_handler, 1000, message_type.SHUTDOWN)) {
						shutdown();
					}
					break;
				}

				//Set up an engine to control. The address of the engine is given in msg.obj and whichThrottle is in arg1
				//Optional rostername if present is separated from the address by the proper delimiter 
				case message_type.LOCO_ADDR: {
					final String addr = msg.obj.toString();  
					final char whichThrottle = (char) msg.arg1;
					if (withrottle_version >= 2.0) {  //don't pass rostername to older WiT
						if (prefs.getBoolean("drop_on_acquire_preference", 
								getResources().getBoolean(R.bool.prefDropOnAcquireDefaultValue))) {
							withrottle_send(whichThrottle+"r");  //send release command for any already acquired locos (if set in prefs)

						}
					}
					withrottle_send(whichThrottle + addr);

					if (withrottle_version >= 2.0) {
						//request current direction and speed (WiT 2.0+)
						withrottle_send("M" + whichThrottle+"A*<;>qV");
						withrottle_send("M" + whichThrottle+"A*<;>qR");
					}

					if (heart.getInboundInterval() > 0) {
						withrottle_send("*+");     //request to turn on heartbeat (if enabled in server prefs)
					}
					break;
				}
				//          case message_type.ERROR:
				//            break;

				//Adjust the locomotive's speed. whichThrottle is in arg 1 and arg2 holds the value of the speed to set. //TODO: Allow 14 and 28 speed steps (might need a change on the server side).
				case message_type.VELOCITY: {
					final char whichThrottle = (char) msg.arg1;
					withrottle_send(String.format(whichThrottle+"V%d", msg.arg2));
					break;
				}
				//Change direction. address in in msg, whichThrottle is in arg 1 and arg2 holds the direction to change to. 
				case message_type.DIRECTION: {
					final String addr = msg.obj.toString();
					final char whichThrottle = (char) msg.arg1;
					withrottle_send(String.format(whichThrottle+"R%d<;>"+addr, msg.arg2));
					break;
				}
				//Set or unset a function. whichThrottle+addr is in the msg, arg1 is the function number, arg2 is set or unset.
				case message_type.FUNCTION: {
					String addr = msg.obj.toString();
					final char whichThrottle = (char) addr.charAt(0);
					addr = addr.substring(1);
					withrottle_send(String.format(whichThrottle+"F%d%d<;>"+addr, msg.arg2, msg.arg1));
					break;
				}
				//send command to change turnout.  msg = (T)hrow, (C)lose or (2)(toggle) + systemName 
				case message_type.TURNOUT: {
					final String cmd = msg.obj.toString();
					withrottle_send("PTA" + cmd);
					break;
				}
				//send command to route turnout.  msg = 2(toggle) + systemName
				case message_type.ROUTE: {
					final String cmd = msg.obj.toString();
					withrottle_send("PRA" + cmd);
					break;
				}
				//send command to change power setting, new state is passed in arg1
				case message_type.POWER_CONTROL:
					withrottle_send(String.format("PPA%d", msg.arg1));
					break;

				//Current Time update request
				case message_type.CURRENT_TIME:
					alert_activities(message_type.CURRENT_TIME, currentTime);
                    break;
                    
				//Current Time clock display preference change
				case message_type.CLOCK_DISPLAY:
					if (clockWebSocket != null) {
						clockWebSocket.refresh();
						alert_activities(message_type.CURRENT_TIME, currentTime);
					}
                    break;
                        
				// SHUTDOWN - terminate socketWiT and it's done
				case message_type.SHUTDOWN:
					shutdown();
					break;

				// update of roster-related data completed in background
				case message_type.ROSTER_UPDATE:
					alert_activities(message_type.ROSTER_UPDATE,"");
					break;

				// WiT socket is down and reconnect attempt in prog 
				case message_type.WIT_CON_RETRY:
					/***future Notification
  					hideNotification();
					 ***/
					alert_activities(message_type.WIT_CON_RETRY,"");
					break;

					// WiT socket is back up
				case message_type.WIT_CON_RECONNECT:
					/***future Notification
					showNotification();
					 ***/
					alert_activities(message_type.WIT_CON_RECONNECT,"");
					break;
				}
			};
		}

		private void shutdown() {
			Log.d("Engine_Driver","TA Shutdown");
			end_jmdns();						//jmdns should already be down but no harm in making call
			if (socketWiT != null) {
				socketWiT.disconnect(true);		//stop reading from the socket
				socketWiT = null;
			}
			host_ip = null;
			port = 0;
			doFinish = false;					//ok for activities to run if restarted after this 
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

		//display error msg using Toast()
		private void process_comm_error(final String msg_txt) {
			Log.d("Engine_Driver", "TA comm error: " + msg_txt);
			//need to do Toast() on the main thread so create a handler
			Handler h =  new Handler(Looper.getMainLooper());
			h.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), msg_txt, Toast.LENGTH_SHORT).show();
				}
			});
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
			case 'M': {
				String sWhichThrottle = response_str.substring(1,2);
				char whichThrottle = sWhichThrottle.charAt(0);
				String[] ls = splitByString(response_str,"<;>");	//drop off separator
				String addr = ls[0].substring(3);
				char com2 = response_str.charAt(2);
				//loco was successfully added to a throttle
				if(com2 == '+') {  //"MT+L2591<;>"  loco was added
					Consist con;
					if(whichThrottle == 'T')				// indicate loco was Confirmed by WiT
						con = consistT;
					else if(whichThrottle == 'G')
						con = consistG;
					else
						con = consistS;
					if(con.getLoco(addr) != null)
						con.setConfirmed(addr);
					else
						Log.d("Engine_Driver", "loco " + addr + " not selected but assigned by WiT to " + whichThrottle);
				} 
				else if(com2 == '-') { //"MS-L6318<;>"  loco removed from throttle
					if(whichThrottle == 'T') {
						consistT.remove(addr);
					} else if(whichThrottle == 'G') {
						consistG.remove(addr);
					} else {
						consistS.remove(addr);
					}
					Log.d("Engine_Driver", "loco " + addr + " dropped from " + whichThrottle);

				} 
				else if(com2 == 'L') { //list of function buttons
					String lead;
					if ('T' == whichThrottle) 
						lead = consistT.getLeadAddr();
					else if('G' == whichThrottle)
						lead = consistG.getLeadAddr();
					else
						lead = consistS.getLeadAddr();
					if(lead.equals(addr))						//*** temp - only process if for lead engine in consist
						process_roster_function_string("RF29}|{1234(L)" + ls[1], sWhichThrottle);  //prepend some stuff to match old-style
				} 

				else if(com2 == 'A') { //process change in function value  MTAL4805<;>F028
					if ("F".equals(ls[1].substring(0,1))) {
						process_function_state_20(sWhichThrottle, Integer.valueOf(ls[1].substring(2)), "1".equals(ls[1].substring(1,2)) ? true : false);  
					}	    	  			
				}

				break;
			}
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
					if(response_str.charAt(2) == 'C' || response_str.charAt(2) == 'L') {  //RCC1 or RCL1 (treated the same in ED)
						clear_consist_list();
					} 
					else if(response_str.charAt(2) == 'D') {  //RCD}|{88(S)}|{88(S)]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
						process_consist_list(response_str);
					}
					break;

				case 'L': 
					//					roster_list_string = response_str.substring(2);  //set app variable
					process_roster_list(response_str);  //process roster list
					dlMetadataTask.get();		// run background metadata update if web server port is known
					dlRosterTask.get();			// run background roster update if web server port is known
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
                    dlMetadataTask.get();			// start background metadata update
					dlRosterTask.get();				// start background metadata update

					if(androidVersion >= minWebSocketVersion) {
						if (clockWebSocket == null)
							clockWebSocket = new ClockWebSocketHandler();
						clockWebSocket.refresh();
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
			String[] ta = splitByString(response_str,"]\\[");  //split into list of labels
			//clear the appropriate global variable
			if ("T".equals(whichThrottle)) {
				function_labels_T = new LinkedHashMap<Integer, String>();
			} 
			else if("S".equals(whichThrottle))
			{
				function_labels_S = new LinkedHashMap<Integer, String>();
			}
			else {
				function_labels_G = new LinkedHashMap<Integer, String>();
			}

			//initialize app arrays (skipping first)
			int i = 0;
			for (String ts : ta) {
				if (i > 0 && !"".equals(ts)) { //skip first chunk, which is length, and skip any blank entries
					if ("T".equals(whichThrottle)) {  //populate the appropriate hashmap
						function_labels_T.put(i-1,ts); //index is hashmap key, value is label string
					} 
					else if("S".equals(whichThrottle))
					{
						function_labels_S.put(i-1,ts); //index is hashmap key, value is label string
					}
					else {
						function_labels_G.put(i-1,ts); //index is hashmap key, value is label string
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
					}  
					else if("S".equals(whichThrottle))
					{
						function_states_S[fn] = fState;
					}
					else {
						function_states_G[fn] = fState;
					}
				}  //end if i==0
				i++;
			}  //end for
		}

		//parse function state string into appropriate app variable array (format for WiT >= 2.0)
		private void process_function_state_20(String whichThrottle, Integer fn, boolean fState) {

			if ("T".equals(whichThrottle)) {
				function_states_T[fn] = fState;
			}  
			else if ("S".equals(whichThrottle))
			{
				function_states_S[fn] = fState;
			}
			else {
				function_states_G[fn] = fState;
			}
		}

		//
		// withrottle_send(String msg)
		//
		//send msg to the socket using multithrottle format
		//
		//msg format is generally whichThrottle+cmd+<;>addr 
		//if <;>addr is omitted then command is sent to all locos on whichThrottle
		//
		//msg format for acquire loco is whichThrottle+addr+<;>rosterName
		//where <;>rosterName is optional
		//
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
					char whichThrottle = msg.charAt(0);
					String cmd = msg.substring(1);
					char com = cmd.charAt(0);
					String addr = "";
					if(cmd.length() > 0) {									//check for loco address after the command
						String[] as = splitByString(cmd,"<;>");
						if(as.length > 1) {
							addr = as[1];
							cmd = as[0];
						}
					}
					String prefix = "M" + whichThrottle;					// use a multithrottle command
					if ('T' == whichThrottle || 'S' == whichThrottle || 'G' == whichThrottle) { 	//acquire loco
						if ('L' == com || 'S' == com) { 					//if address length
							String rosterName = new String(addr);
							addr = cmd;
							if(rosterName.length() > 0) {
								rosterName = "E" + rosterName;  //use E to indicate rostername
							} 
							else {
								rosterName = addr;
							}
							newMsg = prefix + "+" + addr + "<;>" + rosterName;  //add requested loco to this throttle
						} 
						else if ('r' == com) {							//if release loco(s)
							if(addr.length() > 0)
								newMsg = prefix + "-" + addr + "<;> + addr";		//release one loco
							else
								newMsg = prefix + "-*<;>r";  						//release all locos from this throttle
						} 
						else if('V' == com) {							//if set speed
							if(addr.length() == 0)
								addr = "*";
							newMsg = prefix + "A" + addr + "<;>" + cmd;  
						}
						else if('R' == com) {							//if set direction
							if(addr.length() == 0)
								addr = "*";
							newMsg = prefix + "A" + addr + "<;>" + cmd;
						}
						else {												//if anything else
							newMsg = prefix + "A*<;>" + cmd;  				//pass all action commands along
						}
					}
				}
				catch(Exception e) {
					validMsg = false;
					if((socketWiT != null) && newMsg.equals("Q")) {
						Log.d("Sent Q command", "Sent " + newMsg + " command to jmri WiFi Throttle");
						socketWiT.Send(newMsg); //Sends quit command to JMRI.
					}
					else
					{
						Log.d("Engine_Driver", "--> invalid msg: " + newMsg);
					}
				}
			}

			if (validMsg) {
				//send response to debug log for review
				Log.d("Engine_Driver", "-->:" + newMsg + "  was(" + msg + ")");
				//perform the send
				if(socketWiT != null) {
					socketWiT.Send(newMsg);
				}
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
			private volatile boolean endRead = false;			//signals rcvr to terminate
			private volatile boolean socketGood = false;		//indicates socket condition

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
				return socketOk;
			}

			public void disconnect(boolean shutdown) {
				if (shutdown) {
					endRead = true;
					for(int i = 0; i < 5 && this.isAlive(); i++) {
						try { 
							Thread.sleep(300);    			//  give run() a chance to see endRead and exit
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

				if (!shutdown)	// going to retry the connection
				{
					// reinit shared variables then signal activities to refresh their views
					// so that (potentially) invalid information is not displayed
					initShared();
					sendMsg(comm_msg_handler, message_type.WIT_CON_RETRY);
				}
			}

			//read the input buffer
			public void run() {
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
				heart.stopHeartbeat();
			}

			public void Send(String msg) {
				//reconnect socket if needed
				if(!socketGood || !this.SocketCheck()) {
					this.disconnect(false);		//clean up socket but do not shut down the receiver
					this.connect();				//attempt to reestablish connection
					if(socketGood) {
						process_comm_error("Success: Restored connection to WiThrottle server " + host_ip + ".\n");
						sendMsg(comm_msg_handler, message_type.WIT_CON_RECONNECT);
					}
					else {
						process_comm_error("Warning: Lost connection to WiThrottle server " + host_ip + ".\nAttempting to reconnect.");
						comm_msg_handler.postDelayed(heart.outboundHeartbeatTimer, 6000L);	//try connection again in 6 seconds
					}
				}

				//send the message
				if(socketGood) {
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
						{
							haveConnectedWifi = true;
						}
					if ("MOBILE".equalsIgnoreCase(ni.getTypeName()))
						if (ni.isConnected())
						{
							haveConnectedMobile = true;
						}
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
							if (consistT.isActive()) {  
								withrottle_send("MTA*<;>qV"); //request speed
								withrottle_send("MTA*<;>qR"); //request direction
								anySent = true;
							}
							if (consistS.isActive()) {  
								withrottle_send("MSA*<;>qV"); //request speed
								withrottle_send("MSA*<;>qR"); //request direction
								anySent = true;
							}
							if (consistG.isActive()) {  
								withrottle_send("MGA*<;>qV"); //request speed
								withrottle_send("MGA*<;>qR"); //request direction
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
		
		
		class ClockWebSocketHandler extends WebSocketHandler {
		    private final String sGetClockMemory = "{\"type\":\"memory\",\"data\":{\"name\":\"IMCURRENTTIME\"}}";
		    private final String sClockMemoryName = "IMCURRENTTIME";
		    private WebSocketConnection mConnection = new WebSocketConnection();
		    private int displayClockHrs = 0;
		    private final SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a");
		    private final SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm");
		    
			@Override
		    public void onOpen() {
		        try {
    	            Log.d("Engine_Driver","ClockWebSocket open");
		            mConnection.sendTextMessage(sGetClockMemory);
		        } catch(Exception e) { 
    	            Log.d("Engine_Driver","ClockWebSocket open error: "+e.toString());
		        }
		    }
		    
    	    @Override
    	    public void onTextMessage(String msg) {
    	        try {
    	            JSONObject currentTimeMemory = new JSONObject(msg);
    	            JSONObject data = currentTimeMemory.getJSONObject("data");
    	            if(sClockMemoryName.equals(data.getString("name"))) {
    	            	currentTime = data.getString("value");
    	            	if(currentTime.length() > 0) {
    	            		String newTime; 
        	            	try {
	    	            		if(currentTime.indexOf("M") < 0) {			// no AM or PM - in 24 hr format
	    	            			if(displayClockHrs == 1) {				// display in 12 hr format
		    	            			newTime = sdf12.format(sdf24.parse(currentTime));
		    	            			currentTime = newTime;
	    	            			}
	    	            		} else {									// in 12 hr format 
	    	            			if(displayClockHrs == 2) {				// display in 24 hr format
	    	            				newTime = sdf24.format(sdf12.parse(currentTime));
	    	            				currentTime = newTime;
	    	            			}
    	            			}
        	            	} catch (ParseException e) { }
    	            		alert_activities(message_type.CURRENT_TIME, currentTime);	  //send the time update
    	            	}
    	            }
    	        } catch (JSONException e) {
    	        	// wasn't a clock memory message so just ignore it
    	        }
    	    }
		    
		    @Override
		    public void onClose(int code, String closeReason) {
		    	// attempt reconnection unless finishing
				if(!doFinish && displayClock)
					this.connect();
		    }
				    
		    public void connect() {
		        try {
    	            Log.d("Engine_Driver","ClockWebSocket attempt connect");
					mConnection.connect(createUri(), this);
		        } catch (Exception e) {
    	            Log.d("Engine_Driver","ClockWebSocket connect error: "+e.toString());
		        }
		    }
		    
		    public void disconnect() {
	            try {
	                mConnection.disconnect();
	            } catch (Exception e) {
    	            Log.d("Engine_Driver","ClockWebSocket disconnect error: "+e.toString());
	            }
	            displayClock = false;
		    }
		        
		    public void refresh() {
    			currentTime = "";
    			try {
    				displayClockHrs = Integer.parseInt(prefs.getString("ClockDisplayTypePreference", "0"));
    			} catch(NumberFormatException e) {
    				displayClockHrs = 0;
    			}
		    	if(displayClockHrs > 0) {
		    		if (mConnection.isConnected())
		    			this.disconnect();
	    			this.connect();
	    			displayClock = true;
		    	} else { 
		    			this.disconnect();
		    			displayClock = false;
		    	}
			}
		}
	}

	/**
	 * Display OnGoing Notification that indicates EngineDriver is Running.
	 * Should only be called when ED is going into the background.
	 * Currently call this from each activity onPause, passing the current intent 
	 * to return to when reopening.  */  
	void addNotification(Intent notificationIntent) {

	    NotificationCompat.Builder builder =
	    		new NotificationCompat.Builder(this)
	    .setSmallIcon(R.drawable.icon)
		.setContentTitle("Engine Driver")
		.setContentText("Tap to reopen EngineDriver.")
		.setOngoing(true);

	    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
	                     PendingIntent.FLAG_UPDATE_CURRENT);
	    builder.setContentIntent(contentIntent);

	    // Add as notification
	    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    manager.notify(ED_NOTIFICATION_ID, builder.build());
	}

	// Remove notification
	void removeNotification() {
	    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    manager.cancel(ED_NOTIFICATION_ID);
	}
	@Override
	public void onCreate()  {
		super.onCreate();
		Log.d("Engine_Driver","TA.onCreate()");

		//When starting ED after it has been killed in the bkg, the OS restarts any activities that were running.
		//Since we aren't connected at this point, we want all those activities to finish() so we do 2 things:
		// doFinish=true tells activities (except CA) that aren't running yet to finish() when they reach onResume()
		// DISCONNECT message tells any activities (except CA) that are already running to finish()
		doFinish = true;
		port = 0;				//indicate that no connection exists
		commThread=new comm_thread();
		commThread.start();
		alert_activities(message_type.DISCONNECT,"");

		/***future Recovery
	    //Normally CA is run via the manifest when ED is launched.
	    //However when starting ED after it has been killed in the bkg,
	    //CA may not be running (or may not be on top).
	    //We need to ensure CA is running at this point in the code,
	    //so start CA if it is not running else bring to top if already running.
		final Intent caIntent = new Intent(this, connection_activity.class);
		caIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(caIntent);
		 ***/

		
		androidVersion = android.os.Build.VERSION.SDK_INT;
		prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

		function_states_T = new boolean[32];
		function_states_S = new boolean[32];
		function_states_G = new boolean[32];

		dlMetadataTask = new DownloadMetaTask();
		dlRosterTask = new DownloadRosterTask();

		//use worker thread to initialize default function labels from file so UI can continue
		new Thread(new Runnable() {
			public void run() {
				set_default_function_labels();
			}
		}).start();	
		CookieSyncManager.createInstance(this);		//create this here so onPause/onResume for webViews can control it
	}

	public boolean isForcingFinish() {
		return doFinish;
	}


	//init default function labels from the settings files or set to default
	private void set_default_function_labels() {
		function_labels_default = new LinkedHashMap<Integer, String>();
		try {
			File sdcard_path=Environment.getExternalStorageDirectory();
			File settings_file=new File(sdcard_path + "/engine_driver/function_settings.txt");
			if(settings_file.exists()) {  //if file found, use it for settings arrays
				BufferedReader settings_reader=new BufferedReader(new FileReader(settings_file));
				//read settings into local arrays
				while(settings_reader.ready()) {
					String line=settings_reader.readLine();
					String temp[] = line.split(":");
					function_labels_default.put(Integer.parseInt(temp[1]), temp[0]); //put funcs and labels into global default
				}
				settings_reader.close();
			} 
			else {  		//hard-code some buttons and default the rest
				function_labels_default.put(0, "Light");
				function_labels_default.put(1, "Bell");
				function_labels_default.put(2, "Horn");
				for(int k = 3; k <= 28; k++) {
					function_labels_default.put(k, Integer.toString(k));		//String.format("%d",k));
				}
			}
		}
		catch (IOException except) { 
			Log.e("settings_activity", "Could not read file "+except.getMessage()); 
		}  
	}

	public class DownloadRosterTask extends DownloadDataTask {
		@SuppressWarnings("unchecked")
		@Override
		void runMethod(Download dl) throws IOException {
			String rosterUrl = createUrl("prefs/roster.xml");
			HashMap<String, RosterEntry> rosterTemp = null;
			if(rosterUrl == null || dl.cancel)
				return;
			Log.d("Engine_Driver","Background loading roster from " + rosterUrl);
			int rosterSize = 0;
			try {
				RosterLoader rl = new RosterLoader(rosterUrl);
				if(dl.cancel)
					return;
				rosterTemp = rl.parse();
				rosterSize = rosterTemp.size();		//throws exception if still null
				if(!dl.cancel)
					roster = (HashMap<String, RosterEntry>) rosterTemp.clone();
			}
			catch(Exception e) {
				throw new IOException();
			}
			Log.d("Engine_Driver","Loaded " + rosterSize +" entries from roster.xml.");
		}
	}

	public class DownloadMetaTask extends DownloadDataTask {
		@SuppressWarnings("unchecked")
		@Override
		void runMethod(Download dl) throws IOException   {
			String metaUrl = createUrl("xmlio/");
			if(metaUrl == null || dl.cancel)
				return;
			Log.d("Engine_Driver","Background loading metadata from " + metaUrl);
			try {
				URL url = new URL( metaUrl );
				URLConnection con = url.openConnection();

				// specify that we will send output and accept input
				con.setDoInput(true);
				con.setDoOutput(true);
				con.setConnectTimeout( 2000 );
				con.setReadTimeout( 2000 );
				con.setUseCaches (false);
				con.setDefaultUseCaches (false);

				// tell the web server to expect xml text
				con.setRequestProperty ( "Content-Type", "text/xml" );

				OutputStreamWriter writer = new OutputStreamWriter( con.getOutputStream() );
				writer.write( "<XMLIO><list type='metadata' /></XMLIO>" );  //ask for metadata info
				writer.flush();
				writer.close();

				//read response and treat as xml doc
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document dom = builder.parse( con.getInputStream() );
				Element root = dom.getDocumentElement();
				HashMap<String, String> metadataTemp = new HashMap<String, String>();
				//get list of metadata children and loop thru, putting each in global variable metadata
				NodeList items = root.getElementsByTagName("metadata");
				for (int i=0; i<items.getLength() & !dl.cancel; i++){
					String metadataName = items.item(i).getAttributes().getNamedItem("name").getNodeValue(); 
					String metadataValue = items.item(i).getAttributes().getNamedItem("value").getNodeValue(); 
					metadataTemp.put(metadataName, metadataValue);
				}
				if(!dl.cancel) {
					if(metadataTemp.size() == 0)		//throw exception if empty
						throw new IOException();
					metadata = (HashMap<String, String>) metadataTemp.clone();
				}
			}
			catch(Exception e) {
				throw new IOException();
			}
			Log.d("Engine_Driver", "Metadata retrieved: " + threaded_application.metadata.toString());
			Log.d("Engine_Driver","Loaded " + metadata.size() +" metadata entries from xmlio server.");
		}
	}

	abstract public class DownloadDataTask {
		private Download dl = null;
		abstract void runMethod(Download dl) throws IOException;

		public class Download extends Thread {
			public volatile boolean cancel = false;

			@Override
			public void run() {
				try {
					runMethod(this);
					if(!cancel)
						sendMsg(comm_msg_handler, message_type.ROSTER_UPDATE);		//send message to alert other activities
				}
				catch(Throwable t) {
					Log.d("Engine_Driver", "Data fetch failed: " + t.getMessage());
				}	  

				// background load of Data completed
				finally {
					if(cancel)
						Log.d("Engine_Driver", "Data fetch cancelled");
				}
			}
		}

		void get() {
			if(dl != null) {
				dl.cancel = true;	// try to stop any update that is in progress on old download thread
			}
			dl = new Download();	// create new download thread
			dl.start();				// start an update
		}

		void stop() {
			if(dl != null) {
				dl.cancel = true;
			}
		}
	}

	// get the roster name from address string 123(L).  Return input if not found in roster or in consist
	public String getRosterNameFromAddress(String response_str) {

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

	//initialize shared variables
	private void initShared() {
		withrottle_version = 0.0; 
		web_server_port = 0;
		power_state = null;
		to_states = null;
		to_system_names = null;
		to_user_names = null;
		to_state_names = null;
		rt_states = null;
		rt_system_names = null;
		rt_user_names = null;
		rt_state_names = null;
		consistT = new Consist();
		consistS = new Consist();
		consistG = new Consist();
		function_labels_S = new LinkedHashMap<Integer, String>();
		function_labels_T = new LinkedHashMap<Integer, String>();
		function_labels_G = new LinkedHashMap<Integer, String>();
		function_states_T = new boolean[32];		// also allocated in onCreate() ???
		function_states_S = new boolean[32];
		function_states_G = new boolean[32];
		consist_entries = new LinkedHashMap<String, String>();
		roster = null;
		roster_entries = null;
		metadata = null;
		doFinish = false;
	}

	//
	// utilities
	//

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
		while(temp.length() > 0) {
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
		while(temp.length() > 0) {
			int index = temp.indexOf(divider);
			if (index < 0) break;    // done with all but last
			result[size] = temp.substring(0,index);
			temp = temp.substring(index+divider.length());
			size++;
		}
		result[size] = temp;

		return result;
	}

	public void powerStateMenuButton()
	{
		int newState = 1;
		if (power_state.equals("1")) { //toggle to opposite value 0=off, 1=on
			newState = 0;
		}
		sendMsg(comm_msg_handler, message_type.POWER_CONTROL, "", newState);
	}

	//TODO: get power_state from JMRI WiThrottle before UI starts up to display Power Layout Icon. 
	//Then can remove displayPowerStateMenuButton2.
	// Also change in throttle, routes and turnouts.
	public void displayPowerStateMenuButton2(Menu menu)
	{
		if(prefs.getBoolean("show_layout_power_button_preference", false))
		{
			menu.findItem(R.id.power_layout_button).setVisible(true);
		}
		else
		{
			menu.findItem(R.id.power_layout_button).setVisible(false);
		}
	}

	public void displayPowerStateMenuButton(Menu menu)
	{
		if(prefs.getBoolean("show_layout_power_button_preference", false) && (power_state != null))
		{
			menu.findItem(R.id.power_layout_button).setVisible(true);
		}
		else
		{
			menu.findItem(R.id.power_layout_button).setVisible(false);
		}
	}

	public void setPowerStateButton(Menu menu)
	{
		if(menu != null)
		{
			if((power_state == null) || (power_state.equals("2")))
			{
				menu.findItem(R.id.power_layout_button).setIcon(R.drawable.power_yellow);
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
				{
					menu.findItem(R.id.power_layout_button).setTitle("Layout Power is UnKnown");
				}
			}
			else if(power_state.equals("1"))
			{
				menu.findItem(R.id.power_layout_button).setIcon(R.drawable.power_green);
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
				{
					menu.findItem(R.id.power_layout_button).setTitle("Layout Power is ON");
				}
			}
			else
			{
				menu.findItem(R.id.power_layout_button).setIcon(R.drawable.power_red);
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
				{
					menu.findItem(R.id.power_layout_button).setTitle("Layout Power is Off");
				}
			}
		}
	}

	public void displayEStop(Menu menu)
	{
		if(prefs.getBoolean("show_emergency_stop_button_preference", false))
		{
			menu.findItem(R.id.EmerStop).setVisible(true);
		}
		else
		{
			menu.findItem(R.id.EmerStop).setVisible(false);
		}

	}

	public void sendEStopMsg()
	{
		sendMsgDelay(comm_msg_handler,0, message_type.VELOCITY, "",(int) 'T', 0);
		sendMsgDelay(comm_msg_handler,0, message_type.VELOCITY, "",(int) 'S', 0);
		sendMsgDelay(comm_msg_handler,0, message_type.VELOCITY, "",(int) 'G', 0);
		EStopActivated = true;
	}

	// forward a message to all running activities 
	void alert_activities(int msgType, String msgBody) {
		sendMsg(connection_msg_handler, msgType, msgBody);
		sendMsg(turnouts_msg_handler, msgType, msgBody);
		sendMsg(routes_msg_handler, msgType, msgBody);
		sendMsg(consist_edit_msg_handler, msgType, msgBody);
		sendMsg(throttle_msg_handler, msgType, msgBody);
		sendMsg(web_msg_handler, msgType, msgBody);
		sendMsg(power_control_msg_handler, msgType, msgBody);
		sendMsg(select_loco_msg_handler, msgType, msgBody);
	}

	public boolean sendMsg(Handler h, int msgType) {
		return sendMsgDelay(h, 0, msgType, "", 0, 0);
	}

	public boolean sendMsg(Handler h, int msgType, String msgBody) {
		return sendMsgDelay(h, 0, msgType, msgBody, 0, 0);
	}

	public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1) {
		return sendMsgDelay(h, 0, msgType, msgBody, msgArg1, 0);
	}

	public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1, int msgArg2) {
		return sendMsgDelay(h, 0, msgType, msgBody, msgArg1, msgArg2);
	}

	public boolean sendMsgDelay(Handler h, long delayMs, int msgType) {
		return sendMsgDelay(h, delayMs, msgType, "", 0, 0);
	}

	public boolean sendMsgDelay(Handler h, long delayMs, int msgType, String msgBody, int msgArg1, int msgArg2) {
		boolean sent = false;
		if(h != null) {
			Message msg=Message.obtain();
			msg.what=msgType;
			msg.obj=new String(msgBody); 
			msg.arg1 = msgArg1;
			msg.arg2 = msgArg2;
			try {
				sent = h.sendMessageDelayed(msg, delayMs);
			}
			catch(Exception e) {
			}
			if(!sent)
				msg.recycle();
		}
		return sent;
	}

	//
	// methods for use by Activities
	//

	// build a full url
	// returns:	full url 	if web_server_port is valid
	//			null	 otherwise
	public String createUrl(String defaultUrl) {
		String url = "";
		int port = web_server_port;
		if (port > 0) {
			if (defaultUrl.startsWith("http"))  //if url starts with http, use it as is, else prepend servername and port
				url = defaultUrl;
			else
				url = "http://" + host_ip + ":" + port + "/" + defaultUrl;
		}
		return url;
	}

	// build a full uri
	// returns:	full uri    if webServerPort is valid
	//			null	    otherwise
	public String createUri() {
		String uri = "";
		int port = web_server_port;
		if (port > 0) {
            uri = "ws://" + host_ip + ":" + port + "/json/";
		}
		return uri;
	}

	/**
	 * Set activity screen orientation based on prefs, check to avoid sending change when already there.
	 * checks "auto Web on landscape" preference and returns false if orientation requires activity switch
	 *
     * @param activity	calling activity
     * @param webPref	if absent or false, uses Throttle Orientation pref.
     * 					if true, uses Web Orientation pref
	 * 
	 * @return 	true if the new orientation is ok for this activity.
	 * 			false if "Auto Web on Landscape" is enabled and new orientation requires activity switch
	 * */
	public boolean setActivityOrientation(Activity activity) {
		return setActivityOrientation(activity,false);
	}
	public boolean setActivityOrientation(Activity activity, Boolean webPref) {
		String to;
		to = prefs.getString("ThrottleOrientation", 
				activity.getApplicationContext().getResources().getString(R.string.prefThrottleOrientationDefaultValue));
		if(to.equals("Auto-Web")) {
			int orient = activity.getResources().getConfiguration().orientation;
			if((webPref && orient == Configuration.ORIENTATION_PORTRAIT)
					|| (!webPref && orient == Configuration.ORIENTATION_LANDSCAPE))
				return(false);
		}
		else if(webPref) {
			to = prefs.getString("WebOrientation", 
					activity.getApplicationContext().getResources().getString(R.string.prefWebOrientationDefaultValue));
		}
		
		int co = activity.getRequestedOrientation();
		if(to.equals("Landscape")   && (co != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE))  
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		else if(to.equals("Auto-Rotate") && (co != ActivityInfo.SCREEN_ORIENTATION_SENSOR))  
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		else if(to.equals("Portrait")    && (co != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		return true;
	}

	// prompt for Exit
	// must be called on the UI thread
	public void checkExit(final Activity activity) {
		final AlertDialog.Builder b = new AlertDialog.Builder(activity); 
		b.setIcon(android.R.drawable.ic_dialog_alert); 
		b.setTitle(R.string.exit_title); 
		b.setMessage(R.string.exit_text);
		b.setCancelable(true);
		b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				firstCreate = true;
				sendMsg(comm_msg_handler, message_type.DISCONNECT, "");  //trigger disconnect / shutdown sequence
			}
		} ); 
		b.setNegativeButton(R.string.no, null);
		AlertDialog alert = b.create();
		alert.show();
	}
}


