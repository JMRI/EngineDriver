/*Copyright (C) 2018 M. Steve Todd
  mstevetodd@gmail.com

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

package jmri.enginedriver.comms;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;

import jmri.enginedriver.R;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.threaded_application;

public class comm_handler extends Handler {
   //All of the work of the communications thread is initiated from this function.

   protected threaded_application mainapp;  // hold pointer to mainapp
   protected SharedPreferences prefs;
   protected comm_thread commThread;

   public comm_handler(threaded_application myApp, SharedPreferences myPrefs, comm_thread myCommThread) {
      mainapp = myApp;
      prefs = myPrefs;
      commThread = myCommThread;
   }

   @SuppressLint({"DefaultLocale", "ApplySharedPref", "WebViewApiAvailability"})
   public void handleMessage(Message msg) {
//                Log.d("Engine_Driver", "comm_handler.handleMessage: message: " +msg.what);
      switch (msg.what) {
         // note: if the Throttle is sent in arg1, it is always expected to be a int
         // if it is sent in arg0, it will be a string

         //Start or Stop jmdns stuff, or add "fake" discovered servers
         case message_type.SET_LISTENER:
            if (mainapp.client_ssid != null &&
                    mainapp.client_ssid.matches("DCCEX_[0-9a-fA-F]{6}$")) {
               //add "fake" discovered server entry for DCCEX: DCCEX_123abc
               commThread.addFakeDiscoveredServer(mainapp.client_ssid, mainapp.client_address, "2560", "DCC-EX");
               mainapp.isDCCEX = (mainapp.prefUseDccexProtocol.equals(threaded_application.DCCEX_PROTOCOL_OPTION_YES))
                       || (mainapp.prefUseDccexProtocol.equals(threaded_application.DCCEX_PROTOCOL_OPTION_AUTO));
            } else if (mainapp.client_ssid != null &&
                    mainapp.client_ssid.matches("^Dtx[0-9]{1,2}-.*_[0-9,A-F]{4}-[0-9]{1,3}$")) {
               //add "fake" discovered server entry for Digitrax LnWi: Dtx1-LnServer_0009-7
               commThread.addFakeDiscoveredServer(mainapp.client_ssid, mainapp.client_address, "12090", "LnWi");
            } else {
               //arg1= 1 to turn on, arg1=0 to turn off
               if (msg.arg1 == 0) {
                  commThread.endJmdns();
               } else {
                  //show message if using mobile data
                  if ((!mainapp.client_type.equals("WIFI")) && (mainapp.prefAllowMobileData)) {
                     threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppNotWIFI, mainapp.client_type), Toast.LENGTH_LONG);
                  }
                  if (commThread.jmdns == null) {   //start jmdns if not started
                     commThread.startJmdns();
                     if (commThread.jmdns != null) {  //don't bother if jmdns didn't start
                        try {
                           commThread.multicast_lock.acquire();
                        } catch (Exception e) {
                           //log message, but keep going if this fails
                           Log.d("Engine_Driver", "comm_handler.handleMessage: multicast_lock.acquire() failed");
                        }
                        commThread.jmdns.addServiceListener(mainapp.JMDNS_SERVICE_WITHROTTLE, commThread.listener);
                        commThread.jmdns.addServiceListener(mainapp.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP, commThread.listener);
                        Log.d("Engine_Driver", "comm_handler.handleMessage: jmdns listener added");
                     } else {
                        Log.d("Engine_Driver", "comm_handler.handleMessage: jmdns not running, didn't start listener");
                     }
                  } else {
                     Log.d("Engine_Driver", "comm_handler.handleMessage: jmdns already running");
                  }
               }
            }
            break;

         //Connect to the WiThrottle server.
         case message_type.CONNECT:

            //The IP address is stored in the obj as a String, the port is stored in arg1.
            String new_host_ip = msg.obj.toString();
            new_host_ip = new_host_ip.trim();
            int new_port = msg.arg1;

            //avoid duplicate connects, seen when user clicks address multiple times quickly
            if (comm_thread.socketWiT != null && comm_thread.socketWiT.SocketGood()
                    && new_host_ip.equals(mainapp.host_ip) && new_port == mainapp.port) {
               Log.d("Engine_Driver", "comm_handler.handleMessage: Duplicate CONNECT message received.");
               break;
            }

            //clear app.thread shared variables so they can be reinitialized
            mainapp.initShared();
            mainapp.fastClockSeconds = 0L;

            //store ip and port in global variables
            mainapp.host_ip = new_host_ip;
            mainapp.port = new_port;
            // skip url checking on Play Protect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
               WebView.setSafeBrowsingWhitelist(Collections.singletonList(mainapp.host_ip), null);
            }

            //attempt connection to WiThrottle server
            comm_thread.socketWiT = new comm_thread.socketWifi();
            if (comm_thread.socketWiT.connect()) {
               if (mainapp.isDCCEX) {
                  if (!mainapp.prefHideInstructionalToasts) {
                     threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.usingProtocolDCCEX), Toast.LENGTH_LONG);
                  }
               }

               commThread.sendThrottleName();
               mainapp.sendMsgDelay(mainapp.comm_msg_handler, 5000L, message_type.CONNECTION_COMPLETED_CHECK);
               commThread.phone = new comm_thread.PhoneListener();
            } else {
               mainapp.host_ip = null;  //clear vars if failed to connect
               mainapp.port = 0;
            }
            mainapp.soundsReloadSounds = true;
            break;

         //Release one or all locos on the specified throttle.  addr is in msg (""==all), arg1 holds whichThrottle.
         case message_type.RELEASE: {
//            int delays = 0;
            String addr = msg.obj.toString();
            final int whichThrottle = msg.arg1;
            final boolean releaseAll = (addr.length() == 0);

            if (releaseAll || mainapp.consists[whichThrottle].isEmpty()) {
               addr = "";
               mainapp.function_labels[whichThrottle] = new LinkedHashMap<>();
               mainapp.function_states[whichThrottle] = new boolean[threaded_application.MAX_FUNCTION_NUMBER +1];
            }
            if (prefs.getBoolean("stop_on_release_preference",                         //send stop command before releasing (if set in prefs)
                    mainapp.getResources().getBoolean(R.bool.prefStopOnReleaseDefaultValue))) {
               comm_thread.sendSpeedZero(whichThrottle);
//               delays++;
            }

//                        sendReleaseLoco(addr, whichThrottle, delays * WiThrottle_Msg_Interval);
            commThread.sendReleaseLoco(addr, whichThrottle, 0);
            break;
         }

            //estop requested.   arg1 holds whichThrottle
         //  M0A*<;>X  was(0X)
         case message_type.ESTOP: {
            final int whichThrottle = msg.arg1;
//            commThread.wifiSend(String.format("M%sA*<;>X", mainapp.throttleIntToString(whichThrottle)));  //send eStop request
            commThread.sendEStop(whichThrottle);
            break;
         }

         case message_type.FORCE_THROTTLE_RELOAD: {
            mainapp.alert_activities(message_type.FORCE_THROTTLE_RELOAD, mainapp.throttleIntToString(msg.arg1));
            break;
         }

         case message_type.RESTART_APP: {
            SharedPreferences sharedPreferences = mainapp.getSharedPreferences("jmri.enginedriver_preferences", 0);
            sharedPreferences.edit().putBoolean("prefForcedRestart", true).commit();
            sharedPreferences.edit().putInt("prefForcedRestartReason", msg.arg1).commit();
            mainapp.alert_activities(message_type.RESTART_APP, "");
            break;
         }

         case message_type.RELAUNCH_APP: {
            SharedPreferences sharedPreferences = mainapp.getSharedPreferences("jmri.enginedriver_preferences", 0);
            sharedPreferences.edit().putBoolean("prefForcedRestart", true).commit();
            sharedPreferences.edit().putInt("prefForcedRestartReason", msg.arg1).commit();
            mainapp.alert_activities(message_type.RELAUNCH_APP, "");

            Intent intent = mainapp.getBaseContext().getPackageManager().getLaunchIntentForPackage(mainapp.getBaseContext().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mainapp.startActivity(intent);
            Runtime.getRuntime().exit(0); // really force the kill
            break;
         }

         //Disconnect from the WiThrottle server and Shutdown
         case message_type.DISCONNECT: {
            Log.d("Engine_Driver", "comm_handler.handleMessage: TA Disconnect");
            mainapp.doFinish = true;
            Log.d("Engine_Driver", "comm_handler.handleMessage: TA alert all activities to shutdown");
            mainapp.alert_activities(message_type.SHUTDOWN, "");     //tell all activities to finish()
            commThread.stoppingConnection();

            // arg1 = 1 means shutdown with no delays
            if (msg.arg1 == 1) {
               commThread.sendDisconnect();
               commThread.shutdown(false);
            } else {
               // At this point TA needs to send the quit message to WiT and then shutdown.
               // However the DISCONNECT message also tells the Throttle activity to release all throttles
               // and that process can take some time:
               //  release request messages and possibly zero speed messages need to be sent to WiT
               //  for each active throttle and WiT will respond with release messages.
               // So we delay the Quit and shutdown to allow time for all the throttle messages to complete
               mainapp.sendMsgDelay(mainapp.comm_msg_handler, 1500L, message_type.WIFI_QUIT);
               if (!mainapp.sendMsgDelay(mainapp.comm_msg_handler, 1600L, message_type.SHUTDOWN)) {
                  commThread.shutdown(false);
               }
               if (mainapp.isDCCEX) {
                  comm_thread.wifiSend("<U DISCONNECT>");  // this is not a real command.  just a placeholder that will be ignored by the CS
               }
            }

            if (mainapp.soundPool != null) {
               mainapp.soundPool.autoPause();
            }
            break;
         }

         //Set up an engine to control. The address of the engine is given in msg.obj and whichThrottle is in arg1
         //Optional rostername if present is separated from the address by the proper delimiter
         case message_type.REQ_LOCO_ADDR: {
//                        int delays = 0;
            final String addr = msg.obj.toString();
            final int whichThrottle = msg.arg1;
            if (prefs.getBoolean("drop_on_acquire_preference",
                    mainapp.getResources().getBoolean(R.bool.prefDropOnAcquireDefaultValue))) {
               commThread.sendReleaseLoco("*", whichThrottle, 0);
//                            delays++;
            }
//                        sendAcquireLoco(addr, whichThrottle, delays * WiThrottle_Msg_Interval);
            comm_thread.sendAcquireLoco(addr, whichThrottle, 0);
            break;
         }

         case message_type.REQUEST_DECODER_ADDRESS: { // DCC-EX only
            comm_thread.sendAcquireLoco("*", -1, 0);
            break;
         }
         case message_type.RECEIVED_DECODER_ADDRESS: {
            if (!mainapp.doFinish) {
               mainapp.alert_activities(message_type.RECEIVED_DECODER_ADDRESS, msg.obj.toString());
            }
            break;
         }

         case message_type.DCCEX_SEND_COMMAND: { // DCC-EX only
            comm_thread.sendDccexCommand(msg.obj.toString());
            break;
         }

         case message_type.DCCEX_COMMAND_ECHO: { // DCC-EX only
            if (msg.obj.toString().charAt(1)!='#')
               mainapp.alert_activities(message_type.DCCEX_COMMAND_ECHO, msg.obj.toString());
            break;
         }

         case message_type.REQUEST_TRACKS: { // DCC-EX only
            comm_thread.sendRequestTracks();
            break;
         }

         case message_type.WRITE_TRACK: { // DCC-EX only
            String [] args = msg.obj.toString().split(" ");
            comm_thread.sendTrack(args[0], args[1], msg.arg1);
            break;
         }

         case message_type.WRITE_TRACK_POWER: { // DCC-EX only
            String [] args = msg.obj.toString().split(" ");
            comm_thread.sendTrackPower(args[0], msg.arg1);
            break;
         }

         case message_type.REQUEST_CV: { // DCC-EX only
            comm_thread.sendReadCv(msg.arg1);
            break;
         }

         case message_type.WRITE_CV: { // DCC-EX only
            int cvValue = Integer.decode(msg.obj.toString());
            comm_thread.sendWriteCv(cvValue, msg.arg1);
            break;
         }

         case message_type.WRITE_POM_CV: { // DCC-EX only
            int addr = msg.arg1;
            String [] args = msg.obj.toString().split(" ");  // [0]=cv [1]=cv value
            comm_thread.sendWritePomCv(Integer.decode(args[0]), Integer.decode(args [1]), addr);
            break;
         }

         case message_type.WRITE_DIRECT_DCC_COMMAND: { // WiThrottle only
//            int addr = msg.arg1;
            String [] args = msg.obj.toString().split(" ");
//            String rslt = "";
            comm_thread.sendWriteDirectDccCommand(args);
            break;
         }

         case message_type.RECEIVED_CV: {
            if (!mainapp.doFinish) {
               mainapp.alert_activities(message_type.RECEIVED_CV, msg.obj.toString());
            }
            break;
         }

         case message_type.WRITE_DECODER_ADDRESS: { // DCC-EX only
            comm_thread.sendWriteDecoderAddress(msg.arg1);
            break;
         }

         case message_type.REQUEST_REFRESH_THROTTLE:
         case message_type.DCCEX_RESPONSE:
         case message_type.WRITE_DECODER_SUCCESS:
         case message_type.WRITE_DECODER_FAIL: {
            if (!mainapp.doFinish) {
               mainapp.alert_activities(msg.what, msg.obj.toString());
            }
            break;
         }

         //send commands to steal the last requested address
         case message_type.STEAL: {
            String addr = msg.obj.toString();
            int whichThrottle = msg.arg1;
            commThread.sendStealLoco(addr, whichThrottle);
            break;
         }

         //Adjust the throttle's speed. whichThrottle is in arg 1 and arg2 holds the value of the speed to set.
         //  message sent is formatted M1A*<;>V13  was(1V13)
         case message_type.VELOCITY: {
            final int whichThrottle = msg.arg1;
            final int speed = msg.arg2;
            comm_thread.sendSpeed(whichThrottle, speed);
            break;
         }

         //Change direction. address is in msg, whichThrottle is in arg 1 and arg2 holds the direction to change to.
         //  message sent is formatted M1AS96<;>R0  was(1R0<;>S96)
         case message_type.DIRECTION: {
            final String addr = msg.obj.toString();
            final int whichThrottle = msg.arg1;
//            commThread.wifiSend(String.format("M%sA%s<;>R%d", mainapp.throttleIntToString(whichThrottle), addr, msg.arg2));
            commThread.sendDirection(whichThrottle, addr, msg.arg2);
            break;
         }
         //Set or unset a function. whichThrottle+addr is in the msg, arg1 is the function number, arg2 is set or unset.
         //  M1AS96<;>F01  was(1F01<;>S96)
         case message_type.FUNCTION: {
            String addr = msg.obj.toString();
            final char cWhichThrottle = addr.charAt(0);
            addr = addr.substring(1);
            commThread.sendFunction(cWhichThrottle, addr, msg.arg1, msg.arg2);
            break;
         }
         //Set or unset a function. whichThrottle+addr is in the msg, arg1 is the function number, arg2 is set or unset.
         case message_type.FORCE_FUNCTION: {
            String addr = msg.obj.toString();
            final char cWhichThrottle = addr.charAt(0);
            addr = addr.substring(1);
            commThread.sendForceFunction(cWhichThrottle, addr, msg.arg1, msg.arg2);
            break;
         }
         //send command to change turnout.  msg = (T)hrow, (C)lose or (2)(toggle) + systemName
         case message_type.TURNOUT: {
            final String cmd = msg.obj.toString();
//            commThread.wifiSend("PTA" + cmd);
            commThread.sendTurnout(cmd);
            break;
         }
         //send command to route turnout.  msg = 2(toggle) + systemName
         case message_type.ROUTE: {
            final String cmd = msg.obj.toString();
//            commThread.wifiSend("PRA" + cmd);
            commThread.sendRoute(cmd);
            break;
         }
         //send command to change power setting, new state is passed in arg1
         case message_type.POWER_CONTROL:
//            commThread.wifiSend(String.format("PPA%d", msg.arg1));
            commThread.sendPower(msg.arg1);
            break;
         //send whatever command string comes in obj to Withrottle Server
         case message_type.WIFI_SEND:
            comm_thread.wifiSend(msg.obj.toString());
            break;
         // Request the throttle's speed and direction. whichThrottle is in arg 1
         case message_type.WIT_QUERY_SPEED_AND_DIRECTION: {
            final int whichThrottle = msg.arg1;
            comm_thread.sendRequestSpeedAndDir(whichThrottle);
            break;
         }
         //send Q to withrottle server
         case message_type.WIFI_QUIT:
//            if (commThread.socketWiT != null && commThread.socketWiT.SocketGood())
//               commThread.wifiSend("Q");
            commThread.sendQuit();
            break;

         //send heartbeat start command to withrottle server
         case message_type.SEND_HEARTBEAT_START:
//            commThread.heart.setHeartbeatSent(true);
//            commThread.wifiSend("*+");
            commThread.sendHeartbeatStart();
            break;

         //Current Time clock display preference change, sets mainapp.fastClockFormat
         case message_type.CLOCK_DISPLAY_CHANGED:
            if (!mainapp.doFinish) {
               try {
                  mainapp.fastClockFormat = Integer.parseInt(Objects.requireNonNull(prefs.getString("ClockDisplayTypePreference", "0")));
               } catch (NumberFormatException e) {
                  mainapp.fastClockFormat = 0;
               }
               mainapp.alert_activities(message_type.TIME_CHANGED, "");
            }
            break;

         // SHUTDOWN - terminate socketWiT and it's done
         case message_type.SHUTDOWN:
            commThread.shutdown(false);
            break;

         case message_type.CONNECTION_COMPLETED_CHECK:
            //if not successfully connected to a 2.0+ server by this time, kill connection
            if (mainapp.withrottle_version < 2.0) {
               if (!mainapp.isDCCEX) {
                  threaded_application.safeToast(
                          threaded_application.context.getResources().getString(R.string.toastWaitingForConnection, mainapp.host_ip, Integer.toString(mainapp.port)),
                          Toast.LENGTH_SHORT);
               } else {
                  threaded_application.safeToast(
                          threaded_application.context.getResources().getString(R.string.toastWaitingForConnectionDccEx, mainapp.host_ip, Integer.toString(mainapp.port)),
                          Toast.LENGTH_SHORT);
               }

               if (comm_thread.socketWiT != null) {
                  comm_thread.socketWiT.disconnect(true, true);     //just close the socket
               }
            }
            break;

         case message_type.REFRESH_FUNCTIONS:
         case message_type.ROSTER_UPDATE: // update of roster-related data completed in background
            if (!mainapp.doFinish) {
               mainapp.alert_activities(msg.what, "");
            }
            break;

         // WiT socket is down and reconnect attempt in prog
         case message_type.WIT_CON_RETRY:
            if (!mainapp.doFinish) {
               mainapp.alert_activities(message_type.WIT_CON_RETRY, msg.obj.toString());
            }
            break;

         // WiT socket is back up
         case message_type.WIT_CON_RECONNECT:
            if (!mainapp.doFinish) {
               commThread.sendThrottleName();
               commThread.reacquireAllConsists();
               mainapp.alert_activities(message_type.WIT_CON_RECONNECT, msg.obj.toString());
            }
            break;

         //send whatever message string comes in obj as a long toast message
         case message_type.TOAST_MESSAGE:
            threaded_application.safeToast(msg.obj.toString(), Toast.LENGTH_LONG);
            break;

         case message_type.KIDS_TIMER_ENABLE:
            mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.KIDS_TIMER_ENABLE, "", 0);
            break;
         case message_type.KIDS_TIMER_START:
            mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.KIDS_TIMER_START, "", 0);
            break;
         case message_type.KIDS_TIMER_END:
            mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.KIDS_TIMER_END, "", 0);
            break;
         case message_type.KIDS_TIMER_TICK:
            mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.KIDS_TIMER_TICK, "", msg.arg1);
            break;
         case message_type.IMPORT_SERVER_AUTO_AVAILABLE:
            Log.d("Engine_Driver", "comm_handler.handleMessage: message: AUTO_IMPORT_URL_AVAILABLE " + msg.what);
            mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.IMPORT_SERVER_AUTO_AVAILABLE, "", 0);
            break;

         case message_type.HTTP_SERVER_NAME_RECEIVED:
            String retrievedServerName = msg.obj.toString();
            if (mainapp.connectedHostName != null &&
                    !retrievedServerName.equals(mainapp.connectedHostName) &&
                    !mainapp.connectedHostName.equals(threaded_application.demo_host)) {
               mainapp.updateConnectionList(retrievedServerName);
            }
            break;

         case message_type.GAMEPAD_ACTION:
            mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.GAMEPAD_ACTION, msg.obj.toString());
            break;
         case message_type.GAMEPAD_JOYSTICK_ACTION:
            mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.GAMEPAD_JOYSTICK_ACTION, msg.obj.toString());
            break;

         case message_type.VOLUME_BUTTON_ACTION:
            mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.VOLUME_BUTTON_ACTION, msg.obj.toString());
            break;
      }
   }
}

