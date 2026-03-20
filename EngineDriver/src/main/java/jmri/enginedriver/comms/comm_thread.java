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

import static android.widget.Toast.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Objects;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.consist_function_rule_style_type;

import jmri.enginedriver.R;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.heartbeat_interval_type;

public class comm_thread extends Thread {
    static final String activityName = "comm_thread";

    JmDNS jmdns = null;
    volatile boolean endingJmdns = false;
    WithrottleListener listener;
    android.net.wifi.WifiManager.MulticastLock multicast_lock;
    static SocketWifi socketWiT;
    PhoneListener phone;
    static Heartbeat heart = new Heartbeat();
    private static long lastSentMs = System.currentTimeMillis();
    private static long lastQueuedMs = System.currentTimeMillis();

    protected static threaded_application mainapp;  // hold pointer to mainapp
    protected static SharedPreferences prefs;

    protected String LATCHING_DEFAULT;
    protected String LATCHING_DEFAULT_ENGLISH;

    protected static int requestLocoIdForWhichThrottleDCCEX;

    static final String[] TRACK_TYPES = {"NONE", "MAIN", "MAIN_INV", "PROG", "DC", "DCX", "AUTO", "EXT", "PROG"};
    static final boolean[] TRACK_TYPES_NEED_ID = {false, false, false, false, true, true, false, false, false};

    public comm_thread(threaded_application myApp, SharedPreferences myPrefs) {
        super("comm_thread");

        //noinspection AssignmentToStaticFieldFromInstanceMethod
        mainapp = myApp;
        prefs = myPrefs;

        mainapp.prefUseDccexProtocol = prefs.getString("prefUseDccexProtocol", mainapp.getResources().getString(R.string.prefUseDccexProtocolDefaultValue));
        mainapp.prefAlwaysUseFunctionsFromServer = prefs.getBoolean("prefAlwaysUseFunctionsFromServer", mainapp.getResources().getBoolean(R.bool.prefAlwaysUseFunctionsFromServerDefaultValue));
        LATCHING_DEFAULT = mainapp.getString(R.string.prefFunctionConsistLatchingLightBellDefaultValue); // can change with language
        LATCHING_DEFAULT_ENGLISH = mainapp.getString(R.string.prefFunctionConsistLatchingLightBellDefaultValueEnglish); // can not change with language

        ResponseProcessorDccex.initialise(mainapp, prefs, this);
        ResponseProcessorWiThrottle.initialise(mainapp, prefs, this);
        SendProcessorDccex.initialise(mainapp, prefs, this);
        SendProcessorWiThrottle.initialise(mainapp, prefs, this);

        this.start();
    }

    /* ******************************************************************************************** */

    //Listen for a WiThrottle service advertisement on the LAN.
    public class WithrottleListener implements ServiceListener {

        public void serviceAdded(ServiceEvent event) {
            //          Log.d(threaded_application.applicationName, activityName + ": serviceAdded()");
            //A service has been added. If no details, ask for them
            Log.d(threaded_application.applicationName, activityName + ": " + String.format("serviceAdded(): for '%s', Type='%s'", event.getName(), event.getType()));
            ServiceInfo si = jmdns.getServiceInfo(event.getType(), event.getName(), 0);
            if (si == null || si.getPort() == 0) {
                Log.d(threaded_application.applicationName, activityName + ": " + String.format("serviceAdded(): requesting details: '%s', Type='%s'", event.getName(), event.getType()));
                jmdns.requestServiceInfo(event.getType(), event.getName(), true, 1000);
            }
        }

        public void serviceRemoved(ServiceEvent event) {
            //Tell the UI thread to remove from the list of services available.
            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.SERVICE, event.getName());
            mainapp.alertActivitiesWithBundle(message_type.SERVICE_REMOVED, bundle);
            Log.d(threaded_application.applicationName, activityName + ": " + String.format("serviceRemoved(): '%s'", event.getName()));
        }

        public void serviceResolved(ServiceEvent event) {
            //          Log.d(threaded_application.applicationName, activityName + ": " + String.format("serviceResolved()"));
            //A service's information has been resolved. Send the port and service name to connect to that service.
            int port = event.getInfo().getPort();

            String serverType = event.getInfo().getPropertyString("jmri") == null ? "" : "JMRI";

            String host_name = event.getInfo().getName();
//            if (event.getInfo().getType().toString().equals(mainapp.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP)) {
            if (event.getInfo().getType().equals(mainapp.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP)) {
                host_name = host_name + " [DCC-EX]";
            }
            Inet4Address[] ip_addresses = event.getInfo().getInet4Addresses();  //only get ipV4 address
            String ip_address = ip_addresses[0].toString().substring(1);  //use first one, since WiThrottle is only putting one in (for now), and remove leading slash

            String key = ip_address+":"+port;
            mainapp.knownDCCEXserverIps.put(key, serverType);

            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.HOST_NAME, host_name);
            bundle.putString(alert_bundle_tag_type.IP_ADDRESS, ip_address);
            bundle.putString(alert_bundle_tag_type.PORT, ((Integer) port).toString() );
            bundle.putString(alert_bundle_tag_type.SSID, mainapp.client_ssid);
            bundle.putString(alert_bundle_tag_type.SERVICE_TYPE, event.getInfo().getType() );
            mainapp.alertActivitiesWithBundle(message_type.SERVICE_RESOLVED, bundle);

            Log.d(threaded_application.applicationName, activityName + ": " + String.format("serviceResolved(): %s(%s):%d -- %s",
                    host_name, ip_address, port,
                    event.toString().replace(Objects.requireNonNull(System.getProperty("line.separator")), " ")));

        }
    }

    void startJmdns() {
        //Set up to find a WiThrottle service via ZeroConf
        try {
            if (mainapp.client_address != null) {
                WifiManager wifi = (WifiManager) mainapp.getSystemService(Context.WIFI_SERVICE);

                if (multicast_lock == null) {  //do this only as needed
                    multicast_lock = wifi.createMulticastLock("engine_driver");
                    multicast_lock.setReferenceCounted(true);
                }

                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): local IP addr " + mainapp.client_address);

                jmdns = JmDNS.create(mainapp.client_address_inet4, mainapp.client_address);  //pass ip as name to avoid hostname lookup attempt

                listener = new WithrottleListener();
                Log.d(threaded_application.applicationName, activityName + ": startJmdns(): listener created");

            } else {
                mainapp.safeToast(R.string.toastThreadedAppNoLocalIp, Toast.LENGTH_LONG);
            }
        } catch (Exception except) {
            Log.e(threaded_application.applicationName, activityName + ": startJmdns(): Error creating withrottle listener: " + except.getMessage());
            mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorCreatingWiThrottle, except.getMessage()), LENGTH_SHORT);
        }
    }

    //endJmdns() takes a long time, so put it in its own thread
    void endJmdns() {
        Thread jmdnsThread = new Thread("EndJmdns") {
            @Override
            public void run() {
                try {
                    Log.d(threaded_application.applicationName, activityName + ": endJmdns(): removing jmdns listener");
                    jmdns.removeServiceListener(mainapp.JMDNS_SERVICE_WITHROTTLE, listener);
                    jmdns.removeServiceListener(mainapp.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP, listener);

                    multicast_lock.release();
                } catch (Exception e) {
                    Log.d(threaded_application.applicationName, activityName + ": endJmdns(): exception in jmdns.removeServiceListener()");
                }
                try {
                    Log.d(threaded_application.applicationName, activityName + ": endJmdns(): calling jmdns.close()");
                    jmdns.close();
                    Log.d(threaded_application.applicationName, activityName + ": endJmdns(): after jmdns.close()");
                } catch (Exception e) {
                    Log.d(threaded_application.applicationName, activityName + ": endJmdns(): exception in jmdns.close()");
                }
                jmdns = null;
                endingJmdns = false;
                Log.d(threaded_application.applicationName, activityName + ": endJmdns(): run exit");
            }
        };
        if (jmdnsIsActive()) {      //only need to run one instance of this thread to terminate jmdns
            endingJmdns = true;
            jmdnsThread.start();
            Log.d(threaded_application.applicationName, activityName + ": endJmdns(): active so ending it and starting thread to remove listener");
        } else {
            jmdnsThread = null;
            Log.d(threaded_application.applicationName, activityName + ": endJmdns(): not active");
        }
    }

    boolean jmdnsIsActive() {
        return jmdns != null && !endingJmdns;
    }

    /*
      add configuration of digitrax LnWi or DCCEX to discovered list, since they do not provide mDNS
     */
    void addFakeDiscoveredServer(String entryName, String clientAddr, String entryPort, String serverType) {

        if (clientAddr == null || clientAddr.lastIndexOf(".") < 0)
            return; //bail on unexpected value

        //assume that the server is at x.y.z.1
        String server_addr = clientAddr.substring(0, clientAddr.lastIndexOf("."));
        server_addr += ".1";

//        mainapp.knownDCCEXserverIps.put(server_addr, serverType);
        String key = server_addr+":"+entryPort;
        mainapp.knownDCCEXserverIps.put(key, serverType);

        Bundle bundle = new Bundle();
        bundle.putString(alert_bundle_tag_type.HOST_NAME, entryName);
        bundle.putString(alert_bundle_tag_type.IP_ADDRESS, server_addr);
        bundle.putString(alert_bundle_tag_type.PORT, entryPort);
        bundle.putString(alert_bundle_tag_type.SSID, mainapp.client_ssid);
        bundle.putString(alert_bundle_tag_type.SERVICE_TYPE, (serverType.equals("DCC-EX") ? mainapp.JMDNS_SERVICE_JMRI_DCCPP_OVERTCP : mainapp.JMDNS_SERVICE_WITHROTTLE) );
        mainapp.alertActivitiesWithBundle(message_type.SERVICE_RESOLVED, bundle);

        Log.d(threaded_application.applicationName, activityName + ": " + String.format("addFakeDiscoveredServer(): added '%s' at %s to Discovered List", entryName, server_addr));

    }

    protected void stoppingConnection() {
        Log.d(threaded_application.applicationName, activityName + ": stoppingConnection(): ");
        heart.stopHeartbeat();
        if (phone != null) {
            phone.disable();
            phone = null;
        }
        endJmdns();
//            dlMetadataTask.stop();
        threaded_application.dlRosterTask.stop();
    }

    protected void delayedAction(int action, long delay) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (action) {
                    case message_type.SHUTDOWN: {
                        shutdown(false);
                        break;
                    }
//                    case message_type.WIFI_QUIT: {
//                        sendQuit();
//                        break;
//                    }
                    case message_type.DISCONNECT: {
                        sendDisconnect();
                        break;
                    }
                }

            }
        }, delay);
    }

    protected void shutdown(boolean fast) {
        Log.d(threaded_application.applicationName, activityName + ": Shutdown()");
        if (socketWiT != null) {
            socketWiT.disconnect(true, fast);     //stop reading from the socket
        }
        Log.d(threaded_application.applicationName, activityName + ": Shutdown(): socketWit down");
        mainapp.writeSharedPreferencesToFileIfAllowed();
        mainapp.host_ip = null;
        mainapp.port = 0;
        threaded_application.reinitStatics();                    // ensure activities are ready for relaunch
        mainapp.doFinish = false;                   //ok for activities to run if restarted after this

        threaded_application.dlRosterTask.stop();
//            dlMetadataTask.stop();

        // make sure flashlight is switched off at shutdown
        if (threaded_application.flashlight != null) {
            threaded_application.flashlight.setFlashlightOff();
            threaded_application.flashlight.teardown();
        }
        mainapp.flashState = false;
        Log.d(threaded_application.applicationName, activityName + ": Shutdown(): end");
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    protected void sendThrottleName() {
        sendThrottleName(true);
    }
    private static void sendThrottleName(Boolean sendHWID) {
        if (mainapp.isWiThrottleProtocol()) { // Withrottle
            SendProcessorWiThrottle.sendThrottleName(sendHWID);
        } else { //DCC-EX
            SendProcessorDccex.sendThrottleName(sendHWID);
        }
    }

    static void sendAcquireLoco(String addr, String rosterName, int whichThrottle) {
        if (mainapp.isWiThrottleProtocol()) { // Withrottle
            SendProcessorWiThrottle.sendAcquireLoco(addr, rosterName, whichThrottle);
        } else { //DCC-EX
            SendProcessorDccex.sendAcquireLoco(addr, rosterName, whichThrottle);
        }
    }

    protected void sendReleaseLoco(String addr, int whichThrottle)  {
        if (mainapp.isWiThrottleProtocol()) { // Withrottle
            SendProcessorWiThrottle.sendReleaseLoco(addr, whichThrottle);
        }  // else  // DCC-EX has no equivalent
    }

    protected void sendReacquireAllConsists() {
        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++)
            sendReacquireConsist(mainapp.consists[i], i);
    }

    protected void sendReacquireConsist(Consist c, int whichThrottle) {
        for (Consist.ConLoco l : c.getLocos()) { // reacquire each confirmed loco in the consist
            if (l.isConfirmed()) {
                String addr = l.getAddress();
                String roster_name = l.getRosterName();
                sendAcquireLoco(addr, roster_name, whichThrottle); //ask for next loco, with 0 or more delays
            }
        }
    }

    protected void sendEStop(int whichThrottle) {
        if (mainapp.isWiThrottleProtocol()) { // Withrottle
            SendProcessorWiThrottle.sendEStop(whichThrottle);
        } else { //DCC-EX
            SendProcessorDccex.sendEStop(whichThrottle);
        }
    }

    protected void sendEStopOneThrottle(int whichThrottle) {
        sendSpeed(whichThrottle, -1); // -1 = EStop
    }

    protected void sendDisconnect() {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendDisconnect();
        } else { //  DCC-EX
            SendProcessorDccex.sendDisconnect();
        }
    }

// Keep these here in case we find a need for them later
//    protected void sendFunction(char cWhichThrottle, String addr, int fn, int fState) {
//        sendFunction(mainapp.throttleCharToInt(cWhichThrottle), addr, fn, fState, false);
//    }
//    protected void sendFunction(char cWhichThrottle, String addr, int fn, int fState, boolean force) {
//        sendFunction(mainapp.throttleCharToInt(cWhichThrottle), addr, fn, fState);
//    }
//    protected void sendFunction(int whichThrottle, String addr, int fn, int fState) {
//        sendFunction(whichThrottle, addr, fn, fState, false);
//    }
    protected void sendFunction(int whichThrottle, String addr, int fn, int fState, boolean force) {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendFunction( whichThrottle, addr, fn, fState, force);
        } else { //  DCC-EX
            SendProcessorDccex.sendFunction( whichThrottle, addr, fn, fState, force);
        }
    }

    protected void sendTurnout(String systemName, char action) {
        if (mainapp.isWiThrottleProtocol()) { // WiThrottle
            SendProcessorWiThrottle.sendTurnout(systemName, action);
        } else { //DCC-EX
            SendProcessorDccex.sendTurnout(systemName, action);
        }
    }

    protected void sendRoute(String systemName, char action) {
        if (mainapp.isWiThrottleProtocol()) { // WiThrottle
            SendProcessorWiThrottle.sendRoute(systemName, action);
        } else { //DCC-EX
            SendProcessorDccex.sendRoute(systemName, action);
        }
    }

    // WiThrottle and DCC-EX
    @SuppressLint("DefaultLocale")
    protected void sendPower(int powerState) {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendPower(powerState);
        } else { //DCC-EX
            SendProcessorDccex.sendPower(powerState);
        }
    }

    protected void sendQuit() {
        if (mainapp.isWiThrottleProtocol()) { // WiThrottle
            SendProcessorWiThrottle.sendQuit();
        } /// N/A for DCC-EX
    }

    protected void sendHeartbeatStart() {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendHeartbeatStart();
        } else { //DCC-EX
            SendProcessorDccex.sendHeartbeatStart();
        }
    }

    protected void sendDirection(int whichThrottle, String addr, int dir) {
    if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
        SendProcessorWiThrottle.sendDirection(whichThrottle, addr, dir);
    } else { //DCC-EX
        SendProcessorDccex.sendDirection(whichThrottle, addr, dir);
    }
}

    protected static void sendSpeedZero(int whichThrottle) {
        sendSpeed(whichThrottle, 0);
    }

    // WiThrottle and DCC-EX
    @SuppressLint("DefaultLocale")
    protected static void sendSpeed(int whichThrottle, int speed) {
        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
            SendProcessorWiThrottle.sendSpeed(whichThrottle, speed);
        } else { //DCC-EX
            SendProcessorDccex.sendSpeed(whichThrottle, speed);
        }
    }

//    @SuppressLint("DefaultLocale")
//    private void sendRequestDir(int whichThrottle) {
//        if (mainapp.isWiThrottleProtocol()) { // not DCC-EX
//            wifiSend(String.format("M%sA*<;>qR",
//                    mainapp.throttleIntToString(whichThrottle)));
//
//        } else { //DCC-EX
//            Consist con = mainapp.consists[whichThrottle];
//            String msgTxt = "";
//            for (Consist.ConLoco l : con.getLocos()) {
//                msgTxt = String.format("<t %s>", l.getAddress().substring(1,l.getAddress().length()));
//                wifiSend(msgTxt);
    ////                Log.d(threaded_application.applicationName, activityName + ": sendRequestDir(): DCC-EX: " + msgTxt);
//            }
//        }
//    }

    @SuppressLint("DefaultLocale")
    protected static void sendRequestSpeedAndDir(int whichThrottle) {
        if (mainapp.isWiThrottleProtocol()) { // WiThrottle
            SendProcessorWiThrottle.sendRequestSpeedAndDir(whichThrottle);
        } else { //DCC-EX
            SendProcessorDccex.sendRequestSpeedAndDir(whichThrottle);
        }
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    protected static void processWifiResponse(String responseStr) {
        /* Withrottle Protocol
            See java/arc/jmri/jmrit/withrottle/deviceserver.java for server code and some documentation
            Also see https://www.jmri.org/help/en/package/jmri/jmrit/withrottle/Protocol.shtml for documentation on the protocol
           DCC-EX Native Protocol
            See https://dcc-ex.com/reference/software/command-summary-consolidated.html#gsc.tab=0 for documentation on the protocol
        */

        //send response to debug log for review
        Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): " + (mainapp.isDccexProtocol() ? "DCC-EX" : "      ") + " :<>: <-- :" + responseStr);

        if (mainapp.activityBundleMessageHandlers[activity_id_type.RECONNECT_STATUS] != null) {
            // The reconnect screen must be active, so notify it so that it can be killed, then process the response as normal
            mainapp.alertActivitiesWithBundle(message_type.WIT_CON_RECONNECT, activity_id_type.RECONNECT_STATUS);
        }

        boolean skipDefaultAlertToAllActivities = false;          //set to true if the Activities do not need to be Alerted

        if (mainapp.isWiThrottleProtocol()) { // WiThrottle Protocol. not DCC-EX Native Protocol
            skipDefaultAlertToAllActivities = ResponseProcessorWiThrottle.processWifiResponse(responseStr);
        } else { // DCC-EX
            skipDefaultAlertToAllActivities = ResponseProcessorDccex.processWifiResponse(responseStr);
        }

        if (!skipDefaultAlertToAllActivities) { // if it has not been processed...
            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.COMMAND, responseStr);
            mainapp.alertActivitiesWithBundle(message_type.RESPONSE, bundle);  //send response to running activities

            Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): Unable to process command: " + responseStr);
        }
    }  //end of processWifiResponse

    /* ***********************************  *********************************** */
    /*  Common/Shared processing functions */
    /* ***********************************  *********************************** */

        static boolean acceptMessageOrAlert(String incomingMessage) {
        boolean acceptMessage = true;
        String[] messagesToIgnore;
        if (mainapp.isDccexProtocol()) {
            messagesToIgnore = mainapp.getResources().getStringArray(R.array.dccex_alert_messages_to_ignore);
        } else {
            messagesToIgnore = mainapp.getResources().getStringArray(R.array.withrottle_alert_messages_to_ignore);
        }
        for (String message : messagesToIgnore) {
            if (incomingMessage.equals(message)) {
                acceptMessage = false;
                break;
            }
        }
        return acceptMessage;
    }

    /* ***********************************  *********************************** */

    static void processRosterFunctionString(String responseStr, int whichThrottle) {
        Log.d(threaded_application.applicationName, activityName + ": processRosterFunctionString(): processing function labels for " + mainapp.throttleIntToString(whichThrottle));
        LinkedHashMap<Integer, String> functionLabelsMap = threaded_application.parseFunctionLabels(responseStr);
        mainapp.function_labels[whichThrottle] = functionLabelsMap; //set the appropriate global variable from the temp
    }

    /* ***********************************  *********************************** */

    //parse function state string into appropriate app variable array
    static void processFunctionState(int whichThrottle, Integer fn, boolean fState) {

        boolean skip = (fn > 2)
                && (mainapp.prefAlwaysUseDefaultFunctionLabels)
                && (!mainapp.prefConsistFollowRuleStyle.equals(consist_function_rule_style_type.ORIGINAL));

        if (!skip) {
            try {
                mainapp.function_states[whichThrottle][fn] = fState;
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
        }
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    //
    // wifiSend(String msg)
    //
    //send formatted msg to the socket using multithrottle format
    //  intermessage gap enforced by requeueing messages as needed
    protected static void wifiSend(String msg) {
        threaded_application.extendedLogging(activityName + ": wifiSend(): message: '" + msg + "'");
        if (msg == null) { //exit if no message
            Log.d(threaded_application.applicationName, activityName + ": wifiSend(): --> null msg");
            return;
        } else if (socketWiT == null) {
            Log.e(threaded_application.applicationName, activityName + ": wifiSend(): socketWiT is null, message: '" + msg + "' not sent!");
            return;
        }

        long now = System.currentTimeMillis();
        long lastGap = now - lastSentMs;

        //send if sufficient gap between messages or msg is timingSensitive, requeue if not
        if (lastGap >= threaded_application.WiThrottle_Msg_Interval || timingSensitive(msg)) {
            //perform the send
            //noinspection UnnecessaryUnicodeEscape
            Log.d(threaded_application.applicationName, activityName + ": wifiSend(): " + (mainapp.isDccexProtocol() ? "DCC-EX" : "      ") + "            :<>: -->: " + msg.replaceAll("\n", "\u21B5") + " (" + lastGap + ")"); //replace newline with cr arrow
            lastSentMs = now;
            socketWiT.Send(msg);

            if (threaded_application.dccexScreenIsOpen) { // only relevant to some DCC-EX commands that we want to see in the DCC-EC Screen.
                Bundle bundle = new Bundle();
                bundle.putString(alert_bundle_tag_type.COMMAND, msg);
                mainapp.alertActivitiesWithBundle(message_type.DCCEX_COMMAND_ECHO, bundle);
            }
        } else {
            //requeue this message
            int nextGap = Math.max((int) (lastQueuedMs - now), 0) + (threaded_application.WiThrottle_Msg_Interval + 5); //extra 5 for processing
            //noinspection UnnecessaryUnicodeEscape
            Log.d(threaded_application.applicationName, activityName + ": wifiSend(): requeue:" + msg.replaceAll("\n", "\u21B5") +
                    ", lastGap=" + lastGap + ", nextGap=" + nextGap); //replace newline with cr arrow

            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.MESSAGE, msg);
            mainapp.alertCommHandlerWithBundle(message_type.WIFI_SEND, nextGap, bundle);

            lastQueuedMs = now + nextGap;
        }
    }  //end wifiSend()

    /* true indicates that message should NOT be requeued as the timing of this message
         is critical.
     */
    private static boolean timingSensitive(String msg) {
        boolean ret = false;
        if (mainapp.isWiThrottleProtocol()) {
            if (msg.matches("^M[0-5]A.{1,5}<;>F[0-1][\\d]{1,2}$")) {
                ret = true;
            } //any function key message
        }
        if (ret) Log.d(threaded_application.applicationName, activityName + ": timingSensitive(): timeSensitive msg, not requeuing:");
        return ret;
    }

    public void run() {
        Looper.prepare();
        Looper threadLooper = Looper.myLooper();
        mainapp.commBundleMessageHandler = new comm_handler(threadLooper);
        mainapp.commBundleMessageHandler.initialise(mainapp, prefs, this);
        Looper.loop();
        Log.d(threaded_application.applicationName, activityName + ": run() exit");
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    static class SocketWifi extends Thread {
        InetAddress host_address;
        Socket clientSocket = null;
        BufferedReader inputBR = null;
        static final int BUFFER_SIZE = 8192;
        PrintWriter outputPW = null;
        private volatile boolean endRead = false;           //signals rcvr to terminate
        private volatile boolean socketGood = false;        //indicates socket condition
        private volatile boolean inboundTimeout = false;    //indicates inbound messages are not arriving from WiT
        private boolean firstConnect = false;               //indicates initial socket connection was achieved
        private int connectTimeoutMs = 3000; //connection timeout in milliseconds
        private int socketTimeoutMs = 500; //socket timeout in milliseconds

        /** @noinspection FieldCanBeLocal*/
        private final int MAX_INBOUND_TIMEOUT_RETRIES = 2;
        private int inboundTimeoutRetryCount = 0;           // number of consecutive inbound timeouts
        private boolean inboundTimeoutRecovery = false;     // attempting to force WiT to respond


        SocketWifi() {
            super("SocketWifi");
        }

        public boolean connect() {

            //use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done
            boolean socketOk = HaveNetworkConnection();

            connectTimeoutMs = Integer.parseInt(prefs.getString("prefConnectTimeoutMs", mainapp.getResources().getString(R.string.prefConnectTimeoutMsDefaultValue)));
            socketTimeoutMs = Integer.parseInt(prefs.getString("prefSocketTimeoutMs", mainapp.getResources().getString(R.string.prefSocketTimeoutMsDefaultValue)));

            //validate address
            if (socketOk) {
                try {
                    host_address = InetAddress.getByName(mainapp.host_ip);
                } catch (UnknownHostException except) {
                    mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppCantDetermineIp, mainapp.host_ip), LENGTH_SHORT);
                    socketOk = false;
                } catch (Exception except) {
                    Log.d(threaded_application.applicationName, activityName + ": connect(): Unknown error.");
                    socketOk = false;
                }
            }

            //socket
            if (socketOk) {
                try {
                    //look for someone to answer on specified socket, and set timeout
                    Log.d(threaded_application.applicationName, activityName + ": SocketWifi: Opening socket, connectTimeout=" + connectTimeoutMs + " and socketTimeout=" + socketTimeoutMs);
                    Log.d(threaded_application.applicationName, activityName + ": SocketWifi: Opening socket, ip=" + mainapp.host_ip + "port=" + mainapp.port);
                    clientSocket = new Socket();
                    InetSocketAddress sa = new InetSocketAddress(mainapp.host_ip, mainapp.port);
                    clientSocket.connect(sa, connectTimeoutMs);
                    Log.d(threaded_application.applicationName, activityName + ": SocketWifi: Opening socket: Connect successful.");
                    clientSocket.setSoTimeout(socketTimeoutMs);
                    Log.d(threaded_application.applicationName, activityName + ": SocketWifi: Opening socket: set timeout successful.");
                } catch (Exception except) {
                    if (!firstConnect) {
//                        mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppCantConnect,
//                                mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address, except.getMessage()), Toast.LENGTH_LONG);

                        Bundle bundle = new Bundle();
                        String message = mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppCantConnect,
                                mainapp.host_ip,
                                Integer.toString(mainapp.port),
                                mainapp.client_address,
                                except.getMessage());
                        bundle.putString(alert_bundle_tag_type.RESPONSE, message);
                        mainapp.alertActivitiesWithBundle(message_type.CONNECTION_FAILED, bundle, activity_id_type.CONNECTION);

                    }
                    if ((!mainapp.client_type.equals("WIFI")) && (mainapp.prefAllowMobileData)) { //show additional message if using mobile data
                        Log.d(threaded_application.applicationName, activityName + ": SocketWifi: Opening socket: Using mobile network, not WIFI. Check your WiFi settings and Preferences.");
                        mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppNotWIFI,
                                mainapp.client_type), Toast.LENGTH_LONG);
                    }
                    socketOk = false;
                }
            }

            //rcvr
            if (socketOk) {
                try {
                    inputBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), BUFFER_SIZE);
                } catch (IOException except) {
                    mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorInputStream, except.getMessage()), LENGTH_SHORT);
                    socketOk = false;
                }
            }

            //start the SocketWifi thread.
            if (socketOk) {
                if (!this.isAlive()) {
                    endRead = false;
                    try {
                        this.start();
                    } catch (IllegalThreadStateException except) {
                        //ignore "already started" errors
                        mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorStartingSocket, except.getMessage()), LENGTH_SHORT);
                    }
                }
            }

            //xmtr
            if (socketOk) {
                try {
                    outputPW = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                    if (outputPW.checkError()) {
                        socketOk = false;
                    }
                } catch (IOException e) {
                    mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorCreatingOutputStream, e.getMessage()), LENGTH_SHORT);
                    socketOk = false;
                }
            }
            socketGood = socketOk;
            if (socketOk)
                firstConnect = true;
            return socketOk;
        }

        public void disconnect(boolean shutdown) {
            disconnect(shutdown, false);
        }

        public void disconnect(boolean shutdown, boolean fastShutdown) {
            Log.d(threaded_application.applicationName, activityName + ": SocketWifi: disconnect()");
            if (shutdown) {
                endRead = true;
                if (!fastShutdown) {
                    for (int i = 0; i < 5 && this.isAlive(); i++) {
                        try {
                            Thread.sleep(connectTimeoutMs);     //  give run() a chance to see endRead and exit
                        } catch (InterruptedException e) {
                            mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppErrorSleepingThread, e.getMessage()), LENGTH_SHORT);
                        }
                    }
                }
            }

            socketGood = false;

            //close socket
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    Log.d(threaded_application.applicationName, activityName + ": SocketWifi(): Error closing the Socket: " + e.getMessage());
                }
            }
        }

        //read the input buffer
        public void run() {
            String str;
            //continue reading until signaled to exit by endRead
            while (!endRead) {
                if (socketGood) {        //skip read when the socket is down
                    try {
                        if ((str = inputBR.readLine()) != null) {
                            if (!str.isEmpty()) {
                                threaded_application.extendedLogging(activityName + ": <<-- " + str);
                                heart.restartInboundInterval();
                                clearInboundTimeout();
                                if (mainapp.isWiThrottleProtocol()) {
                                    processWifiResponse(str);
                                } else {
                                    String [] cmds = str.split("><");
                                    if (cmds.length == 1) { // multiple concatenated commands
                                        processWifiResponse(str);
                                    } else {
                                        for (int i=0; i< cmds.length; i++) {
                                            if ((cmds[i].charAt(0) == '<') && (cmds[i].charAt(cmds[i].length() - 1)) == '>') {
                                                processWifiResponse(cmds[i]);
                                            } else if ((cmds[i].charAt(0) == '<') && (cmds[i].charAt(cmds[i].length() - 1)) != '>') {
                                                processWifiResponse(cmds[i] + ">");
                                            } else if ((cmds[i].charAt(0) != '<') && (cmds[i].charAt(cmds[i].length() - 1)) == '>') {
                                                processWifiResponse("<" + cmds[i]);
                                            } else {
                                                processWifiResponse("<" + cmds[i] + ">");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        socketGood = this.SocketCheck();
                    } catch (IOException e) {
                        if (socketGood) {
                            Log.d(threaded_application.applicationName, activityName + ": run(): WiT rcvr error.");
                            socketGood = false;     //input buffer error so force reconnection on next send
                        }
                    }
                }
                if (!socketGood) {
                    SystemClock.sleep(500L);        //don't become compute bound here when the socket is down
                }
            }
            heart.stopHeartbeat();
            Log.d(threaded_application.applicationName, activityName + ": run(): SocketWifi exit.");
        }

        @SuppressLint("StringFormatMatches")
        void Send(String msg) {
            boolean reconInProg = false;
            //reconnect socket if needed
            if (!socketGood || inboundTimeout) {
                String status;
                if (mainapp.client_address == null) {
                    status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppNotConnected);
                    Log.d(threaded_application.applicationName, activityName + ": send(): Not Connected: WiT send reconnection attempt.");
                } else if (inboundTimeout) {
                    status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppNoResponse, mainapp.host_ip, Integer.toString(mainapp.port), heart.getInboundInterval());
                    Log.d(threaded_application.applicationName, activityName + ": send(): No Response: WiT receive reconnection attempt.");
                } else {
                    status = mainapp.getApplicationContext().getResources().getString(R.string.statusThreadedAppUnableToConnect, mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address);
                    Log.d(threaded_application.applicationName, activityName + ": send(): Unable to connect: WiT send reconnection attempt.");
                }
                socketGood = false;

                Bundle witBundle = new Bundle();
                witBundle.putString(alert_bundle_tag_type.MESSAGE, status);
                mainapp.alertActivitiesWithBundle(message_type.WIT_CON_RETRY, witBundle);

                //perform the reconnection sequence
                this.disconnect(false);             //clean up socket but do not shut down the receiver
                this.connect();                     //attempt to reestablish connection
                reconInProg = true;
            }

            //try to send the message
            if (socketGood) {
                try {
                    outputPW.println(msg);
                    outputPW.flush();
                    heart.restartOutboundInterval();

                    // if we get here without an exception then the socket is ok
                    if (reconInProg) {
//                        String status = "Connected to WiThrottle Server at " + mainapp.host_ip + ":" + mainapp.port;

                        mainapp.alertCommHandlerWithBundle(message_type.WIT_CON_RECONNECT);

                        Log.d(threaded_application.applicationName, activityName + ": send(): WiT reconnection successful.");
                        clearInboundTimeout();
                        heart.restartInboundInterval();     //socket is good so restart inbound heartbeat timer
                        mainapp.DCCEXlistsRequested = -1; //invalidate the lists
                    }
                } catch (Exception e) {
                    Log.d(threaded_application.applicationName, activityName + ": send(): WiT xmtr error.");
                    socketGood = false;             //output buffer error so force reconnection on next send
                }
            }

            if (!socketGood) {
                mainapp.commBundleMessageHandler.postDelayed(heart.outboundHeartbeatTimer, 500L);   //try connection again in 0.5 second
            }
        }

        // Attempt to determine if the socket connection is still good.
        // unfortunately isConnected returns true if the Socket was disconnected other than by calling close()
        // so on signal loss it still returns true.
        // Eventually we just try to send and handle the IOException if the socket was disconnected.
        boolean SocketCheck() {
            boolean status = clientSocket.isConnected() && !clientSocket.isInputShutdown() && !clientSocket.isOutputShutdown();
            if (status)
                status = HaveNetworkConnection();   // can't trust the socket flags so try something else...
            return status;
        }

        // temporary - SocketCheck should determine whether socket connection is good however socket flags sometimes do not get updated
        // so it doesn't work.  This is better than nothing though?
        private boolean HaveNetworkConnection() {
            boolean haveConnectedWifi = false;
            boolean haveConnectedMobile = false;
            mainapp.prefAllowMobileData = prefs.getBoolean("prefAllowMobileData", false);

            final ConnectivityManager connectivityManager = (ConnectivityManager) mainapp.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] netInfo = connectivityManager.getAllNetworkInfo();
            for (NetworkInfo networkInfo : netInfo) {
                if ("WIFI".equalsIgnoreCase(networkInfo.getTypeName()))

                    if (!mainapp.prefAllowMobileData) {
                        // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
//                        if ((Build.VERSION.SDK_INT >= 21)
//                                && (!mainapp.haveForcedWiFiConnection)) {
                        if (!mainapp.haveForcedWiFiConnection) {
                            Log.d(threaded_application.applicationName, activityName + ": HaveNetworkConnection(): NetworkRequest.Builder");
                            NetworkRequest.Builder request = new NetworkRequest.Builder();
                            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

                            connectivityManager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {
                                @Override
                                public void onAvailable(Network network) {
                                    if (Build.VERSION.SDK_INT < 23) {
                                        ConnectivityManager.setProcessDefaultNetwork(network);
                                    } else {
                                        connectivityManager.bindProcessToNetwork(network);  //API23+
                                    }
                                }
                            });
                            mainapp.haveForcedWiFiConnection = true;
                        }
                    }

                if (isNetworkAvailable()) {
                    haveConnectedWifi = true;
                } else {
                    // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                    if (mainapp.prefAllowMobileData) {
                        haveConnectedWifi = true;
                    }
                }
                if ("MOBILE".equalsIgnoreCase(networkInfo.getTypeName()))
                    if ((isNetworkAvailable()) && (mainapp.prefAllowMobileData)) {
                        haveConnectedMobile = true;
                    }
            }
            return haveConnectedWifi || haveConnectedMobile;
        }

        private Boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager = (ConnectivityManager) mainapp.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= 23) {
                Network nw = connectivityManager.getActiveNetwork();
                if (nw == null) return false;
                NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
                return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
            } else {
                NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
                return nwInfo != null && nwInfo.isConnected();
            }
        }

        boolean SocketGood() {
            return this.socketGood;
        }

        void InboundTimeout() {
            if (++inboundTimeoutRetryCount >= MAX_INBOUND_TIMEOUT_RETRIES) {
                Log.d(threaded_application.applicationName, activityName + ": InboundTimeout(): WiT max inbound timeouts");
                inboundTimeout = true;
                inboundTimeoutRetryCount = 0;
                inboundTimeoutRecovery = false;
                // force a send to start the reconnection process
                mainapp.commBundleMessageHandler.postDelayed(heart.outboundHeartbeatTimer, 200L);

            } else {
                Log.d(threaded_application.applicationName, activityName + ": InboundTimeout(): WiT inbound timeout " +
                        inboundTimeoutRetryCount + " of " + MAX_INBOUND_TIMEOUT_RETRIES);
                // heartbeat should trigger a WiT reply so force that now
                inboundTimeoutRecovery = true;

                mainapp.commBundleMessageHandler.post(heart.outboundHeartbeatTimer);
            }
        }

        void clearInboundTimeout() {
            inboundTimeout = false;
            inboundTimeoutRecovery = false;
            inboundTimeoutRetryCount = 0;
        }
    }

    /* ******************************************************************************************** */

    static class Heartbeat {
        //  outboundHeartbeat - send a periodic heartbeat to WiT to show that ED is alive.
        //  inboundHeartbeat - WiT doesn't send a heartbeat to ED, so send a periodic message to WiT that requires a response.
        //
        //  If the HeartbeatValueFromServer is 0 then set heartbeatOutboundInterval = DEFAULT_OUTBOUND_HEARTBEAT_INTERVAL,
        //    and set heartbeatInboundInterval = 0, to disable the inbound heartbeat checks
        //
        //  Otherwise, set heartbeatOutboundInterval to HeartbeatValueFromServer * HEARTBEAT_RESPONSE_ALLOWANCE,
        //    and set heartbeatInboundInterval to HeartbeatValueFromServer / HEARTBEAT_RESPONSE_ALLOWANCE
        //
        //  Insure both values are between MIN_OUTBOUND_HEARTBEAT_INTERVAL and MAX_OUTBOUND_HEARTBEAT_INTERVAL

        private int heartbeatIntervalSetpoint = 0;      //WiT heartbeat interval in msec
        private int heartbeatOutboundInterval = 0;      //sends outbound heartbeat message at this rate (msec)
        private int heartbeatInboundInterval = 0;       //alerts user if there was no inbound traffic for this long (msec)

        public boolean isHeartbeatSent() {
            return heartbeatSent;
        }

        public void setHeartbeatSent(boolean heartbeatSent) {
            this.heartbeatSent = heartbeatSent;
        }

        private boolean heartbeatSent = false;

        int getInboundInterval() {
            return heartbeatInboundInterval;
        }

        /***
         * startHeartbeat(timeoutInterval in milliseconds)
         * calculate the inbound and outbound intervals and starts the beating
         *
         * @param timeoutInterval the WiT timeoutInterval in milliseconds
         */
        void startHeartbeat(int timeoutInterval) {
            //update interval timers only when the heartbeat timeout interval changed
            mainapp.prefHeartbeatResponseFactor = threaded_application.getIntPrefValue(prefs, "prefHeartbeatResponseFactor", mainapp.getApplicationContext().getResources().getString(R.string.prefHeartbeatResponseFactorDefaultValue));

            if (timeoutInterval != heartbeatIntervalSetpoint) {
                heartbeatIntervalSetpoint = timeoutInterval;

                // outbound interval (in ms)
                int outInterval;
                if (heartbeatIntervalSetpoint == 0) {   //wit heartbeat is disabled so use default outbound heartbeat
                    outInterval = heartbeat_interval_type.DEFAULT_OUTBOUND;
                } else {
//                        outInterval = (int) (heartbeatIntervalSetpoint * HEARTBEAT_RESPONSE_FACTOR);
                    outInterval = (int) (heartbeatIntervalSetpoint * ( (double) mainapp.prefHeartbeatResponseFactor) / 100);
                    //keep values in a reasonable range
                    if (outInterval < heartbeat_interval_type.MIN_OUTBOUND)
                        outInterval = heartbeat_interval_type.MIN_OUTBOUND;
                    if (outInterval > heartbeat_interval_type.MAX_OUTBOUND)
                        outInterval = heartbeat_interval_type.MAX_OUTBOUND;
                }
                heartbeatOutboundInterval = outInterval;

                // inbound interval
                int inInterval = mainapp.heartbeatInterval;
                if (heartbeatIntervalSetpoint == 0) {    // wit heartbeat is disabled so disable inbound heartbeat
                    inInterval = 0;
                } else {
                    if (inInterval < heartbeat_interval_type.MIN_INBOUND)
                        inInterval = heartbeat_interval_type.MIN_INBOUND;
                    if (inInterval < outInterval)
//                            inInterval = (int) (outInterval / HEARTBEAT_RESPONSE_FACTOR);
                        inInterval = (int) (outInterval / ( ((double) mainapp.prefHeartbeatResponseFactor) / 100) );
                    if (inInterval > heartbeat_interval_type.MAX_INBOUND)
                        inInterval = heartbeat_interval_type.MAX_INBOUND;
                }
                heartbeatInboundInterval = inInterval;
                //sInboundInterval = Integer.toString(inInterval);    // seconds

                restartOutboundInterval();
                restartInboundInterval();
            }
        }

        //restartOutboundInterval()
        //restarts the outbound interval timing - call this after sending anything to WiT that requires a response
        void restartOutboundInterval() {
            mainapp.commBundleMessageHandler.removeCallbacks(outboundHeartbeatTimer);                   //remove any pending requests
            if (heartbeatOutboundInterval > 0) {
                mainapp.commBundleMessageHandler.postDelayed(outboundHeartbeatTimer, heartbeatOutboundInterval);    //restart interval
            }
        }

        //restartInboundInterval()
        //restarts the inbound interval timing - call this after receiving anything from WiT
        void restartInboundInterval() {
            mainapp.commBundleMessageHandler.removeCallbacks(inboundHeartbeatTimer);
            if (heartbeatInboundInterval > 0) {
                mainapp.commBundleMessageHandler.postDelayed(inboundHeartbeatTimer, heartbeatInboundInterval);
            }
        }

        void stopHeartbeat() {
            mainapp.commBundleMessageHandler.removeCallbacks(outboundHeartbeatTimer);           //remove any pending requests
            mainapp.commBundleMessageHandler.removeCallbacks(inboundHeartbeatTimer);
            heartbeatIntervalSetpoint = 0;
            Log.d(threaded_application.applicationName, activityName + ": stopHeartbeat(): heartbeat stopped.");
        }

        //outboundHeartbeatTimer()
        //sends a periodic message to WiT
        private final Runnable outboundHeartbeatTimer = new Runnable() {
            @Override
            public void run() {
                mainapp.commBundleMessageHandler.removeCallbacks(this);             //remove pending requests
                if (heartbeatIntervalSetpoint != 0) {
                    boolean anySent = false;
                    if (mainapp.isWiThrottleProtocol()) {
                        for (int i = 0; i < mainapp.prefNumThrottles; i++) {
                            if (mainapp.consists[i].isActive()) {
                                sendRequestSpeedAndDir(i);
                                anySent = true;
                            }
                        }
                    }
                    // prior to JMRI 4.20 there were cases where WiT might not respond to
                    // speed and direction request.  If inboundTimeout handling is in progress
                    // then we always send the Throttle Name to ensure a response
                    if (!anySent || (mainapp.getServerType().isEmpty() && socketWiT.inboundTimeoutRecovery)) {
                        sendThrottleName(false);    //send message that will get a response
                    }
                    mainapp.commBundleMessageHandler.postDelayed(this, heartbeatOutboundInterval);   //set next beat
                }
            }
        };

        //inboundHeartbeatTimer()
        //display an alert message when there is no inbound traffic from WiT within required interval
        private final Runnable inboundHeartbeatTimer = new Runnable() {
            @Override
            public void run() {
                mainapp.commBundleMessageHandler.removeCallbacks(this); //remove pending requests
                if (heartbeatIntervalSetpoint != 0) {
                    if (socketWiT != null && socketWiT.SocketGood()) {
                        socketWiT.InboundTimeout();
                    }
                    mainapp.commBundleMessageHandler.postDelayed(this, heartbeatInboundInterval);    //set next inbound timeout
                }
            }
        };
    }

    /* ******************************************************************************************** */

    static class PhoneListener extends PhoneStateListener {
        private final TelephonyManager telMgr;

        PhoneListener() {
            telMgr = (TelephonyManager) mainapp.getSystemService(Context.TELEPHONY_SERVICE);
            this.enable();
        }

        public void disable() {
            telMgr.listen(this, PhoneStateListener.LISTEN_NONE);
        }

        public void enable() {
            try {
                telMgr.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
            } catch (SecurityException e) {
                Log.e(threaded_application.applicationName, activityName + ": PhoneListener(): enable(): SecurityException encountered (and ignored) for telMgr");
            }
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                if (prefs.getBoolean("prefStopOnPhoneCall",
                        mainapp.getResources().getBoolean(R.bool.prefStopOnPhoneCallDefaultValue))) {
                    Log.d(threaded_application.applicationName, activityName + ": onCallStateChanged(): Phone is OffHook, Stopping Trains");
                    for (int i = 0; i < mainapp.prefNumThrottles; i++) {
                        if (mainapp.consists[i].isActive()) {
                            sendSpeedZero(i);
                        }
                    }
                }
            }
        }
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

}

