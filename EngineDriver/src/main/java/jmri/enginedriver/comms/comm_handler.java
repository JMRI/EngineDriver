/* Copyright (C) 2017-2026 M. Steve Todd mstevetodd@gmail.com

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.Collections;
import java.util.LinkedHashMap;

import jmri.enginedriver.R;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.dccex_protocol_option_type;

public class comm_handler extends Handler {
   static final String activityName = "comm_handler";

   //All of the work of the communications thread is initiated from this function.

   private boolean initialised = false;
   protected threaded_application mainapp;  // hold pointer to mainapp
   protected SharedPreferences prefs;
   protected comm_thread commThread;

//   public comm_handler(threaded_application myApp, SharedPreferences myPrefs, comm_thread myCommThread) {
//      mainapp = myApp;
//      prefs = myPrefs;
//      commThread = myCommThread;
//   }

   public comm_handler(Looper looper) {
      super(looper);
   }

   public void initialise(threaded_application myApp, SharedPreferences myPrefs, comm_thread myCommThread) {
      mainapp = myApp;
      prefs = myPrefs;
      commThread = myCommThread;
      initialised = true;
   }

   @SuppressLint({"DefaultLocale", "ApplySharedPref", "WebViewApiAvailability"})
   public void handleMessage(Message msg) {
//                Log.d(threaded_application.applicationName, activityName + ": handleMessage(): message: " +msg.what);
      if (!initialised) return;

      Bundle bundle = msg.getData();
      
      switch (msg.what) {
         // note: if the Throttle is sent in arg1, it is always expected to be a int
         // if it is sent in arg0, it will be a string

         //Start or Stop jmdns stuff, or add "fake" discovered servers
         case message_type.SET_LISTENER: {
            Log.d(threaded_application.applicationName, activityName + ": handleMessage(): SET_LISTENER");

            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.ON_OFF))) {

               int on_off = bundle.getInt(alert_bundle_tag_type.ON_OFF);

               if (mainapp.client_ssid != null &&
                       mainapp.client_ssid.matches("DCCEX_[0-9a-fA-F]{6}$")) {
                  Log.d(threaded_application.applicationName, activityName + ": handleMessage(): DCCEX SSID found");
                  //add "fake" discovered server entry for DCCEX: DCCEX_123abc
                  commThread.addFakeDiscoveredServer(mainapp.client_ssid, mainapp.client_address, "2560", "DCC-EX");
                  mainapp.setIsDccexProtocol( (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.YES))
                          || (mainapp.prefUseDccexProtocol.equals(dccex_protocol_option_type.AUTO)) );
               } else if (mainapp.client_ssid != null &&
                       mainapp.client_ssid.matches("^Dtx[0-9]{1,2}-.*_[0-9,A-F]{4}-[0-9]{1,3}$")) {
                  Log.d(threaded_application.applicationName, activityName + ": handleMessage(): LnWi SSID found");
                  //add "fake" discovered server entry for Digitrax LnWi: Dtx1-LnServer_0009-7
                  commThread.addFakeDiscoveredServer(mainapp.client_ssid, mainapp.client_address, "12090", "LnWi");
               } else {
                  if (mainapp.client_ssid == null)
                     Log.d(threaded_application.applicationName, activityName + ": handleMessage(): SSID is Null!");
                  else
                     Log.d(threaded_application.applicationName, activityName + ": handleMessage(): SSID: " + mainapp.client_ssid);

                  //arg1= 1 to turn on, arg1=0 to turn off
                  if (on_off == 0) {
                     commThread.endJmdns();
                  } else {
                     //show message if using mobile data
                     if ((!mainapp.client_type.equals("WIFI")) && (mainapp.prefAllowMobileData)) {
                        mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppNotWIFI, mainapp.client_type), Toast.LENGTH_LONG);
                     }
                     if (commThread.jmdns == null) {   //start jmdns if not started
                        commThread.startJmdns();
                        if (commThread.jmdns != null) {  //don't bother if jmdns didn't start
                           try {
                              commThread.multicast_lock.acquire();
                           } catch (Exception e) {
                              //log message, but keep going if this fails
                              Log.d(threaded_application.applicationName, activityName + ": handleMessage(): multicast_lock.acquire() failed");
                           }
                           commThread.jmdns.addServiceListener(mainapp.JMDNS_SERVICE_WITHROTTLE, commThread.listener);
                           commThread.jmdns.addServiceListener(mainapp.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP, commThread.listener);
                           Log.d(threaded_application.applicationName, activityName + ": handleMessage(): jmdns listener added");
                        } else {
                           Log.d(threaded_application.applicationName, activityName + ": handleMessage(): jmdns not running, didn't start listener");
                        }
                     } else {
                        Log.d(threaded_application.applicationName, activityName + ": handleMessage(): jmdns already running");
                     }
                  }
               }
            }
            break;
      }

         //Connect to the WiThrottle server.
         case message_type.CONNECT: {
            Log.d(threaded_application.applicationName, activityName + ": handleMessage(): CONNECT");

            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.IP_ADDRESS))
                    && (bundle.containsKey(alert_bundle_tag_type.PORT))) {

               String new_host_ip = bundle.getString(alert_bundle_tag_type.IP_ADDRESS).trim();
               int new_port = bundle.getInt(alert_bundle_tag_type.PORT);

               //avoid duplicate connects, seen when user clicks address multiple times quickly
               if (comm_thread.socketWiT != null && comm_thread.socketWiT.SocketGood()
                       && new_host_ip.equals(mainapp.host_ip) && new_port == mainapp.port) {
                  Log.d(threaded_application.applicationName, activityName + ": handleMessage(): Duplicate CONNECT message received.");
                  break;
               }

               //clear app.thread shared variables so they can be reinitialized
               mainapp.initShared();
               mainapp.fastClockSeconds = 0L;

               //store ip and port in global variables
               mainapp.host_ip = new_host_ip;
               mainapp.port = new_port;
               // skip url checking on Play Protect
               if (Build.VERSION.SDK_INT >= 27) {
                  WebView.setSafeBrowsingWhitelist(Collections.singletonList(mainapp.host_ip), null);
               }

               //attempt connection to WiThrottle server
               comm_thread.socketWiT = new comm_thread.SocketWifi();
               if (comm_thread.socketWiT.connect()) {
                  if (mainapp.isDccexProtocol()) {
                     if (!mainapp.prefHideInstructionalToasts) {
                        mainapp.safeToast(R.string.usingProtocolDCCEX, Toast.LENGTH_LONG);
                     }
                  }

                  commThread.sendThrottleName();
//                  mainapp.sendMsgDelay(mainapp.comm_msg_handler, 5000L, message_type.CONNECTION_COMPLETED_CHECK);
                  mainapp.alertCommHandlerWithBundle(message_type.CONNECTION_COMPLETED_CHECK, 5000L);

                  if (prefs.getBoolean("prefStopOnPhoneCall", mainapp.getResources().getBoolean(R.bool.prefStopOnPhoneCallDefaultValue))) {
                     commThread.phone = new comm_thread.PhoneListener();
                  }
               } else {
                  mainapp.host_ip = null;  //clear vars if failed to connect
                  mainapp.port = 0;
               }
               mainapp.soundsReloadSounds = true;
            }
            break;
         }

         //Release one or all locos on the specified throttle.  addr is in msg (""==all)
         case message_type.RELEASE: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE)) ) {

               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               final boolean releaseAll = (addr.isEmpty());

               if (releaseAll || mainapp.consists[whichThrottle].isEmpty()) {
                  addr = "";
                  mainapp.function_labels[whichThrottle] = new LinkedHashMap<>();
                  mainapp.function_states[whichThrottle] = new boolean[threaded_application.MAX_FUNCTION_NUMBER + 1];
               }
               if (prefs.getBoolean("prefStopOnRelease",    //send stop command before releasing (if set in prefs)
                       mainapp.getResources().getBoolean(R.bool.prefStopOnReleaseDefaultValue))) {
                  comm_thread.sendSpeedZero(whichThrottle);
               }

               commThread.sendReleaseLoco(addr, whichThrottle);
            }
            break;
         }

         // WiThrottle only
         // Dispatch one or all locos on the specified throttle.  addr is in msg (""==all).
         // generally acts like a release,but sends the WiThrottle 'd' command instead
         case message_type.DISPATCH: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE)) ) {

               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               final boolean releaseAll = (addr.isEmpty());

               if (releaseAll || mainapp.consists[whichThrottle].isEmpty()) {
                  addr = "";
                  mainapp.function_labels[whichThrottle] = new LinkedHashMap<>();
                  mainapp.function_states[whichThrottle] = new boolean[threaded_application.MAX_FUNCTION_NUMBER + 1];
               }
               if (prefs.getBoolean("prefStopOnRelease",                         //send stop command before releasing (if set in prefs)
                       mainapp.getResources().getBoolean(R.bool.prefStopOnReleaseDefaultValue))) {
                  comm_thread.sendSpeedZero(whichThrottle);
               }

               SendProcessorWiThrottle.sendDispatchLoco(addr, whichThrottle);
            }
            break;
         }

         case message_type.ESTOP: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE)) ) {

               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               commThread.sendEStop(whichThrottle);
               Bundle estopBundle = new Bundle();
               estopBundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
               mainapp.alertActivitiesWithBundle(message_type.ESTOP, estopBundle);
            }
            break;
         }

         case message_type.ESTOP_ONE_THROTTLE: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE)) ) {

               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               commThread.sendEStopOneThrottle(whichThrottle);
               Bundle estopBundle = new Bundle();
               estopBundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
               mainapp.alertActivitiesWithBundle(message_type.ESTOP, estopBundle);
            }
            break;
         }

         case message_type.RESTART_APP: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.RESTART_REASON)) ) {

               final int restartReason = bundle.getInt(alert_bundle_tag_type.RESTART_REASON);

               SharedPreferences sharedPreferences = mainapp.getSharedPreferences("jmri.enginedriver_preferences", 0);
               sharedPreferences.edit().putBoolean("prefForcedRestart", true).commit();
               sharedPreferences.edit().putInt("prefForcedRestartReason", restartReason).commit();
               mainapp.alertActivitiesWithBundle(message_type.RESTART_APP);
            }
            break;
         }

         case message_type.RELAUNCH_APP: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.RESTART_REASON)) ) {

               final int restartReason = bundle.getInt(alert_bundle_tag_type.RESTART_REASON);

               SharedPreferences sharedPreferences = mainapp.getSharedPreferences("jmri.enginedriver_preferences", 0);
               sharedPreferences.edit().putBoolean("prefForcedRestart", true).commit();
               sharedPreferences.edit().putInt("prefForcedRestartReason", restartReason).commit();
               mainapp.alertActivitiesWithBundle(message_type.RELAUNCH_APP);

               Intent intent = mainapp.getBaseContext().getPackageManager().getLaunchIntentForPackage(mainapp.getBaseContext().getPackageName());
               if (intent != null) {
                  intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  mainapp.startActivity(intent);
               }
               Runtime.getRuntime().exit(0); // really force the kill
            }
            break;
         }

         case message_type.DISCONNECT: {
            Log.d(threaded_application.applicationName, activityName + ": handleMessage(): DISCONNECT");

            commThread.sendQuit();

            Log.d(threaded_application.applicationName, activityName + ": handleMessage(): alert all activities to disconnect");
            mainapp.alertActivitiesWithBundle(message_type.DISCONNECT);
            commThread.stoppingConnection();

            commThread.sendDisconnect();

            if (mainapp.soundPool != null) mainapp.soundPool.autoPause();
            break;
         }

         case message_type.SHUTDOWN: {
            Log.d(threaded_application.applicationName, activityName + ": handleMessage(): SHUTDOWN");

            if (bundle != null) {

               int urgent = 0;
               if (bundle.containsKey(alert_bundle_tag_type.URGENT)) {
                  urgent = bundle.getInt(alert_bundle_tag_type.URGENT);
               }

               mainapp.doFinish = true;

               commThread.sendQuit();

               Log.d(threaded_application.applicationName, activityName + ": handleMessage(): alert all activities to shutdown");
               mainapp.alertActivitiesWithBundle(message_type.SHUTDOWN);     //tell all activities to finish()
               commThread.stoppingConnection();

               if (urgent == 1) { // arg1 = 1 means shutdown with no delays
                  commThread.sendDisconnect();
                  commThread.shutdown(false);
               } else {
                  commThread.delayedAction(message_type.SHUTDOWN, 1500);
               }

               if (mainapp.soundPool != null) mainapp.soundPool.autoPause();
            }
            break;
         }

         //Set up an engine to control.
         case message_type.REQUEST_LOCO_BY_ADDRESS: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE)) ) {

               final String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               String rosterName = "";
               if (bundle.containsKey(alert_bundle_tag_type.ROSTER_NAME)) {
                  rosterName = bundle.getString(alert_bundle_tag_type.ROSTER_NAME);
               }
               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);

               if (prefs.getBoolean("prefDropOnAcquire",
                       mainapp.getResources().getBoolean(R.bool.prefDropOnAcquireDefaultValue))) {
                  commThread.sendReleaseLoco("*", whichThrottle);

               }
               comm_thread.sendAcquireLoco(addr, rosterName, whichThrottle);
            }
            break;
         }

         case message_type.REQUEST_DECODER_ADDRESS: { // DCC-EX only
            comm_thread.sendAcquireLoco("*", "", -1);
            break;
         }

         case message_type.DCCEX_SEND_COMMAND: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.COMMAND)) ) {

               final String cmd = bundle.getString(alert_bundle_tag_type.COMMAND);
               SendProcessorDccex.sendDccexCommand(cmd);
            }
            break;
         }

         case message_type.REQUEST_TRACKS: { // DCC-EX only
            SendProcessorDccex.sendDccexRequestTracks();
            break;
         }

         case message_type.WRITE_TRACK: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.TRACK_CHAR))
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO))
                    && (bundle.containsKey(alert_bundle_tag_type.TRACK_TYPE_TEXT)) ) {

               final char track = bundle.getChar(alert_bundle_tag_type.TRACK_CHAR);
               final String type = bundle.getString(alert_bundle_tag_type.TRACK_TYPE_TEXT);
               final int id = bundle.getInt(alert_bundle_tag_type.LOCO);
               SendProcessorDccex.sendDccexTrack(track, type, id);
            }
            break;
         }

         case message_type.DCCEX_JOIN_TRACKS: { // DCC-EX only
            SendProcessorDccex.sendDccexJoinTracks();
            break;
         }

         case message_type.DCCEX_UNJOIN_TRACKS: { // DCC-EX only
            SendProcessorDccex.sendDccexJoinTracks(false);
            break;
         }

         case message_type.DCCEX_ESTOP_PAUSE: { // DCC-EX only
            SendProcessorDccex.sendDccexEmergencyStopPauseResume();
            break;
         }

         case message_type.DCCEX_ESTOP_RESUME: { // DCC-EX only
            SendProcessorDccex.sendDccexEmergencyStopPauseResume(false);
            break;
         }

         case message_type.WRITE_TRACK_POWER: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.TRACK_CHAR))
                    && (bundle.containsKey(alert_bundle_tag_type.POWER_STATE)) ) {

               char trackLetter = bundle.getChar(alert_bundle_tag_type.TRACK_CHAR);
               int powerState = bundle.getInt(alert_bundle_tag_type.POWER_STATE);
               SendProcessorDccex.sendDccexTrackPower(trackLetter, powerState);
            }
            break;
         }

         case message_type.REQUEST_CV: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.CV)) ) {

               final int cv = bundle.getInt(alert_bundle_tag_type.CV);
               SendProcessorDccex.sendDccexReadCv(cv);
            }
            break;
         }

         case message_type.WRITE_CV: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.CV))
                    && (bundle.containsKey(alert_bundle_tag_type.CV_VALUE)) ) {

               final int cv = bundle.getInt(alert_bundle_tag_type.CV);
               final int cvValue = bundle.getInt(alert_bundle_tag_type.CV_VALUE);
               SendProcessorDccex.sendDccexWriteCv(cv, cvValue);
            }
               break;
         }

         case message_type.WRITE_POM_CV: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.CV))
                    && (bundle.containsKey(alert_bundle_tag_type.CV_VALUE))
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO)) ) {

               final int cv = bundle.getInt(alert_bundle_tag_type.CV);
               final int cvValue = bundle.getInt(alert_bundle_tag_type.CV_VALUE);
               final int addr = bundle.getInt(alert_bundle_tag_type.LOCO);
               SendProcessorDccex.sendDccexWritePomCv(cv, cvValue, addr);
            }
            break;
         }

         case message_type.WRITE_DIRECT_DCC_COMMAND: { // WiThrottle only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.COMMAND)) ) {
               final String cmd = bundle.getString(alert_bundle_tag_type.COMMAND);
               String[] args = cmd.split(" ");
               SendProcessorWiThrottle.sendWriteDirectDccCommand(args);
            }
            break;
         }

         case message_type.WRITE_ADVANCED_CONSIST_ADD: { // WiThrottle only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.CONSIST_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.CONSIST_NAME))
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.FACING)) ) {

               final String consist = bundle.getString(alert_bundle_tag_type.CONSIST_TEXT);
               final String consistName = bundle.getString(alert_bundle_tag_type.CONSIST_NAME);
               final String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               final int facing = bundle.getInt(alert_bundle_tag_type.FACING);
               SendProcessorWiThrottle.sendAdvancedConsistAddLoco(consist, consistName, addr, facing);
            }
            break;
         }

         case message_type.WRITE_ADVANCED_CONSIST_REMOVE: { // WiThrottle only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.CONSIST_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT)) ) {

               final String consist = bundle.getString(alert_bundle_tag_type.CONSIST_TEXT);
               final String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               SendProcessorWiThrottle.sendAdvancedConsistRemoveLoco(consist, addr);
            }
            break;
         }

         case message_type.DCCEX_REQUEST_CONSIST_LIST: { // DCC-EX only
            SendProcessorDccex.sendDccexRequestInCommandStationConsistList();
            break;
         }

         case message_type.WRITE_DCCEX_COMMAND_STATION_CONSIST_ADD: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.CONSIST_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.FACING)) ) {

               final String consist = bundle.getString(alert_bundle_tag_type.CONSIST_TEXT);
               final String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               final int facing = bundle.getInt(alert_bundle_tag_type.FACING);
               SendProcessorDccex.sendDccexCommandStationConsistAddLoco(consist, addr, facing);
               SendProcessorDccex.sendDccexRequestInCommandStationConsistList();
            }
            break;
         }

         case message_type.WRITE_DCCEX_COMMAND_STATION_CONSIST_REMOVE: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.CONSIST_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT)) ) {

               final String consist = bundle.getString(alert_bundle_tag_type.CONSIST_TEXT);
               final String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               SendProcessorDccex.sendDccexCommandStationConsistRemoveLoco(consist, addr);
               SendProcessorDccex.sendDccexRequestInCommandStationConsistList();
            }
               break;
         }

         case message_type.READ_DCCEX_LOCO_ADDRESS: { // DCC-EX only
            SendProcessorDccex.sendDccexGetLocoAddress();
            break;
         }

         case message_type.READ_DCCEX_CONSIST_ADDRESS: { // DCC-EX only
            SendProcessorDccex.sendDccexGetConsistAddress();
            break;
         }

         case message_type.WRITE_DECODER_ADDRESS: { // DCC-EX only
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO)) ) {

               final int addr = bundle.getInt(alert_bundle_tag_type.LOCO);
               SendProcessorDccex.sendDccexWriteDecoderAddress(addr);
            }
            break;
         }

         //send commands to steal the last requested address
         case message_type.STEAL: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE)) ) {

               final String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               SendProcessorWiThrottle.sendStealLoco(addr, whichThrottle);
            }
            break;
         }

         // Adjust the throttle's speed.
         case message_type.VELOCITY: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE))
                    && (bundle.containsKey(alert_bundle_tag_type.SPEED)) ) {

               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               final int speed = bundle.getInt(alert_bundle_tag_type.SPEED);
               comm_thread.sendSpeed(whichThrottle, speed);
            }
            break;
         }

         // Change direction.
         case message_type.DIRECTION: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE))
                    && (bundle.containsKey(alert_bundle_tag_type.DIRECTION)) ) {

               final String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               final int dir = bundle.getInt(alert_bundle_tag_type.DIRECTION);
               commThread.sendDirection(whichThrottle, addr, dir);
            }
            break;
         }

         //Set or unset a function.
         case message_type.FUNCTION: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.THROTTLE))
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO_TEXT))
                    && (bundle.containsKey(alert_bundle_tag_type.FUNCTION))
                    && (bundle.containsKey(alert_bundle_tag_type.FUNCTION_ACTION)) ) {

               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               final String addr = bundle.getString(alert_bundle_tag_type.LOCO_TEXT);
               final int function = bundle.getInt(alert_bundle_tag_type.FUNCTION);
               final int function_action = bundle.getInt(alert_bundle_tag_type.FUNCTION_ACTION);
               boolean force = false;
               if (bundle.containsKey(alert_bundle_tag_type.FORCE)) {
                  force = bundle.getBoolean(alert_bundle_tag_type.FORCE);
               }

               commThread.sendFunction(whichThrottle, addr, function, function_action, force);
            }
            break;
         }
//         //Set or unset a function. whichThrottle+addr is in the msg, arg1 is the function number, arg2 is set or unset.
//         case message_type.FORCE_FUNCTION: {
//            String addr = msg.obj.toString();
//            final char cWhichThrottle = addr.charAt(0);
//            addr = addr.substring(1);
//            commThread.sendForceFunction(cWhichThrottle, addr, msg.arg1, msg.arg2);
//            break;
//         }

         //send command to change turnout.  action = (T)hrow, (C)lose or (2)(toggle)
         case message_type.TURNOUT: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.TURNOUT))
                    && (bundle.containsKey(alert_bundle_tag_type.TURNOUT_ACTION)) ) {

               final String turnout = bundle.getString(alert_bundle_tag_type.TURNOUT);
               final char turnoutAction = bundle.getChar(alert_bundle_tag_type.TURNOUT_ACTION);
               commThread.sendTurnout(turnout, turnoutAction);
            }
            break;
         }

         //send command to route turnout.  action always = 2(toggle)
         case message_type.ROUTE: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.ROUTE))
                    && (bundle.containsKey(alert_bundle_tag_type.ROUTE_ACTION)) ) {

               final String route = bundle.getString(alert_bundle_tag_type.ROUTE);
               final char routeAction = bundle.getChar(alert_bundle_tag_type.ROUTE_ACTION);
               commThread.sendRoute(route, routeAction);
            }
            break;
         }

         case message_type.START_AUTOMATION: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.ROUTE))
                    && (bundle.containsKey(alert_bundle_tag_type.ROUTE_ACTION))
                    && (bundle.containsKey(alert_bundle_tag_type.LOCO)) ) {

               final String route = bundle.getString(alert_bundle_tag_type.ROUTE);
               final char routeAction = bundle.getChar(alert_bundle_tag_type.ROUTE_ACTION);
               final int addr = bundle.getInt(alert_bundle_tag_type.LOCO);

               SendProcessorDccex.sendDccexAutomation(route, routeAction, addr);
            }
            break;
         }
         //send command to change power setting, new state is passed
         case message_type.POWER_CONTROL:
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.POWER_STATE)) ) {

               final int powerState = bundle.getInt(alert_bundle_tag_type.POWER_STATE);
               commThread.sendPower(powerState);
            }
            break;

         //send command to request the power state.  no arguments
//         case message_type.POWER_STATE_REQUEST:
//            commThread.sendPowerStateRequest();
//            break;

         // send whatever command string comes in obj to Withrottle Server
         // this will normally only be used if a delay is required, otherwise just use wifiSend() in comm_thread
         case message_type.WIFI_SEND: {
            if ( (bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.MESSAGE)) ) {

               final String message = bundle.getString(alert_bundle_tag_type.MESSAGE);
               comm_thread.wifiSend(message);
            }
            break;
         }

         // Request the throttle's speed and direction.
         case message_type.WIT_QUERY_SPEED_AND_DIRECTION: {
            if ((bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.POWER_STATE)) ) {

               final int whichThrottle = bundle.getInt(alert_bundle_tag_type.THROTTLE);
               comm_thread.sendRequestSpeedAndDir(whichThrottle);
            }
            break;
         }

//         //send Q to withrottle server
//         case message_type.WIFI_QUIT:
////            if (commThread.socketWiT != null && commThread.socketWiT.SocketGood())
////               commThread.wifiSend("Q");
//            commThread.sendQuit();
//            break;

         //send heartbeat start command to withrottle server
         case message_type.SEND_HEARTBEAT_START:
            commThread.sendHeartbeatStart();
            break;

//         //Current Time clock display preference change, sets mainapp.fastClockFormat
//         case message_type.CLOCK_DISPLAY_CHANGED:
//            if (!mainapp.doFinish) {
//               try {
//                  mainapp.fastClockFormat = Integer.parseInt(Objects.requireNonNull(prefs.getString("prefClockDisplayType", "0")));
//               } catch (NumberFormatException e) {
//                  mainapp.fastClockFormat = 0;
//               }
//               mainapp.alertActivitiesWithBundle(message_type.RECEIVED_TIME_CHANGE);
//            }
//            break;

         case message_type.CONNECTION_COMPLETED_CHECK:
            //if not successfully connected to a 2.0+ server by this time, kill connection
            if (mainapp.withrottle_version < 2.0) {
               if (mainapp.isWiThrottleProtocol()) {
                  mainapp.safeToast(
                          mainapp.getApplicationContext().getResources().getString(R.string.toastWaitingForConnection, mainapp.host_ip, Integer.toString(mainapp.port)),
                          Toast.LENGTH_SHORT);
               } else {
                  mainapp.safeToast(
                          mainapp.getApplicationContext().getResources().getString(R.string.toastWaitingForConnectionDccEx, mainapp.host_ip, Integer.toString(mainapp.port)),
                          Toast.LENGTH_SHORT);
               }

               if (comm_thread.socketWiT != null) {
                  comm_thread.socketWiT.disconnect(true, true);     //just close the socket
               }
            }
            break;

//         // WiT socket is down and reconnect attempt in prog
//         case message_type.WIT_CON_RETRY: {
//            if (!mainapp.doFinish) {
//               Bundle witBundle = new Bundle();
//               witBundle.putString(alert_bundle_tag_type.MESSAGE, msg.obj.toString());
//               mainapp.alertActivitiesWithBundle(message_type.WIT_CON_RETRY, witBundle);
//            }
//            break;
//         }

         // WiT socket is back up
         case message_type.WIT_CON_RECONNECT:
            if (!mainapp.doFinish) {
               commThread.sendThrottleName();
               commThread.sendReacquireAllConsists();
               mainapp.alertActivitiesWithBundle(message_type.WIT_CON_RECONNECT);
            }
            break;

         //send whatever message string comes in obj as a long toast message
         case message_type.TOAST_MESSAGE:
            if ( (bundle != null)
                    && (bundle.containsKey(alert_bundle_tag_type.MESSAGE)) ) {

               final String message = bundle.getString(alert_bundle_tag_type.MESSAGE);
               mainapp.safeToast(message, Toast.LENGTH_LONG);
            }
            break;
      }
   }
}

