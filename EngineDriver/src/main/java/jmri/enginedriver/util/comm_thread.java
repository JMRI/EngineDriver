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

package jmri.enginedriver.util;

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
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import jmri.enginedriver.Consist;
import jmri.enginedriver.R;
import jmri.enginedriver.message_type;
import jmri.enginedriver.threaded_application;

public class comm_thread extends Thread {
    JmDNS jmdns = null;
    volatile boolean endingJmdns = false;
    withrottle_listener listener;
    android.net.wifi.WifiManager.MulticastLock multicast_lock;
    static socketWifi socketWiT;
    PhoneListener phone;
    static heartbeat heart = new heartbeat();
    private static long lastSentMs = System.currentTimeMillis();
    private static long lastQueuedMs = System.currentTimeMillis();

    protected static threaded_application mainapp;  // hold pointer to mainapp
    protected static SharedPreferences prefs;

    protected String LATCHING_DEFAULT;

    private static int requestLocoIdForWhichThrottleDCCEX;

    static final int TRACK_TYPE_OFF_INDEX = 0;
    static final int TRACK_TYPE_DCC_MAIN_INDEX = 1;
    static final int TRACK_TYPE_DCC_PROG_INDEX = 2;
    static final int TRACK_TYPE_DC_INDEX = 3;
    static final int TRACK_TYPE_DCX_INDEX = 4;

    static final String [] TRACK_TYPES = { "OFF", "MAIN", "PROG", "DC", "DCX"};
    static final boolean [] TRACK_TYPES_NEED_ID = { false, false, false, true, true };

    public comm_thread(threaded_application myApp, SharedPreferences myPrefs) {
        super("comm_thread");

        mainapp = myApp;
        prefs = myPrefs;

        mainapp.prefDCCEX = prefs.getBoolean("prefDCCEX", mainapp.getResources().getBoolean(R.bool.prefDCCEXDefaultValue));
        LATCHING_DEFAULT = mainapp.getString(R.string.prefFunctionConsistLatchingLightBellDefaultValue); // can change with language

        this.start();
    }

    /* ******************************************************************************************** */

    //Listen for a WiThrottle service advertisement on the LAN.
    public class withrottle_listener implements ServiceListener {

        public void serviceAdded(ServiceEvent event) {
            //          Log.d("Engine_Driver", String.format("comm_thread.serviceAdded fired"));
            //A service has been added. If no details, ask for them
            Log.d("Engine_Driver", String.format("comm_thread.serviceAdded for '%s', Type='%s'", event.getName(), event.getType()));
            ServiceInfo si = jmdns.getServiceInfo(event.getType(), event.getName(), 0);
            if (si == null || si.getPort() == 0) {
                Log.d("Engine_Driver", String.format("comm_thread.serviceAdded, requesting details: '%s', Type='%s'", event.getName(), event.getType()));
                jmdns.requestServiceInfo(event.getType(), event.getName(), true, 1000);
            }
        }

        public void serviceRemoved(ServiceEvent event) {
            //Tell the UI thread to remove from the list of services available.
            mainapp.sendMsg(mainapp.connection_msg_handler, message_type.SERVICE_REMOVED, event.getName()); //send the service name to be removed
            Log.d("Engine_Driver", String.format("comm_thread.serviceRemoved: '%s'", event.getName()));
        }

        public void serviceResolved(ServiceEvent event) {
            //          Log.d("Engine_Driver", String.format("comm_thread.serviceResolved fired"));
            //A service's information has been resolved. Send the port and service name to connect to that service.
            int port = event.getInfo().getPort();

            String serverType = event.getInfo().getPropertyString("jmri") == null ? "" : "JMRI";

            String host_name = event.getInfo().getName(); //
            Inet4Address[] ip_addresses = event.getInfo().getInet4Addresses();  //only get ipV4 address
            String ip_address = ip_addresses[0].toString().substring(1);  //use first one, since WiThrottle is only putting one in (for now), and remove leading slash

            //Tell the UI thread to update the list of services available.
            HashMap<String, String> hm = new HashMap<>();
            hm.put("ip_address", ip_address);
            hm.put("port", ((Integer) port).toString());
            hm.put("host_name", host_name);

            mainapp.knownDCCEXserverIps.put(ip_address, serverType);

            Message service_message = Message.obtain();
            service_message.what = message_type.SERVICE_RESOLVED;
            service_message.arg1 = port;
            service_message.obj = hm;  //send the hashmap as the payload
            boolean sent = false;
            try {
                sent = mainapp.connection_msg_handler.sendMessage(service_message);
            } catch (Exception ignored) {
            }
            if (!sent)
                service_message.recycle();

            Log.d("Engine_Driver", String.format("comm_thread.serviceResolved - %s(%s):%d -- %s",
                    host_name, ip_address, port, event.toString().replace(System.getProperty("line.separator"), " ")));

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

                Log.d("Engine_Driver", "comm_thread.startJmdns: local IP addr " + mainapp.client_address);

                jmdns = JmDNS.create(mainapp.client_address_inet4, mainapp.client_address);  //pass ip as name to avoid hostname lookup attempt

                listener = new withrottle_listener();
                Log.d("Engine_Driver", "comm_thread.startJmdns: listener created");

            } else {
                threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppNoLocalIp), Toast.LENGTH_SHORT);
            }
        } catch (Exception except) {
            Log.e("Engine_Driver", "comm_thread.startJmdns - Error creating withrottle listener: " + except.getMessage());
            threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorCreatingWiThrottle, except.getMessage()), Toast.LENGTH_SHORT);
        }
    }

    //endJmdns() takes a long time, so put it in its own thread
    void endJmdns() {
        Thread jmdnsThread = new Thread("EndJmdns") {
            @Override
            public void run() {
                try {
                    Log.d("Engine_Driver", "comm_thread.endJmdns: removing jmdns listener");
                    jmdns.removeServiceListener("_withrottle._tcp.local.", listener);

                    multicast_lock.release();
                } catch (Exception e) {
                    Log.d("Engine_Driver", "comm_thread.endJmdns: exception in jmdns.removeServiceListener()");
                }
                try {
                    Log.d("Engine_Driver", "comm_thread.endJmdns: calling jmdns.close()");
                    jmdns.close();
                    Log.d("Engine_Driver", "comm_thread.endJmdns: after jmdns.close()");
                } catch (Exception e) {
                    Log.d("Engine_Driver", "comm_thread.endJmdns: exception in jmdns.close()");
                }
                jmdns = null;
                endingJmdns = false;
                Log.d("Engine_Driver", "comm_thread.endJmdns run exit");
            }
        };
        if (jmdnsIsActive()) {      //only need to run one instance of this thread to terminate jmdns
            endingJmdns = true;
            jmdnsThread.start();
            Log.d("Engine_Driver", "comm_thread.endJmdns active so ending it and starting thread to remove listener");
        } else {
            jmdnsThread = null;
            Log.d("Engine_Driver", "comm_thread.endJmdns not active");
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
        HashMap<String, String> hm = new HashMap<>();
        hm.put("ip_address", server_addr);
        hm.put("port", entryPort);
        hm.put("host_name", entryName);

        mainapp.knownDCCEXserverIps.put(server_addr, serverType);

        Message service_message = Message.obtain();
        service_message.what = message_type.SERVICE_RESOLVED;
        service_message.arg1 = mainapp.port;
        service_message.obj = hm;  //send the hashmap as the payload
        boolean sent = false;
        try {
            sent = mainapp.connection_msg_handler.sendMessage(service_message);
        } catch (Exception ignored) {
        }
        if (!sent)
            service_message.recycle();

        Log.d("Engine_Driver", String.format("comm_thread.addFakeDiscoveredServer: added '%s' at %s to Discovered List", entryName, server_addr));

    }

//        class comm_handler extends Handler {}

    protected void stoppingConnection() {
        heart.stopHeartbeat();
        if (phone != null) {
            phone.disable();
            phone = null;
        }
        endJmdns();
//            dlMetadataTask.stop();
        threaded_application.dlRosterTask.stop();
    }

    protected void shutdown(boolean fast) {
        Log.d("Engine_Driver", "comm_thread.Shutdown");
        if (socketWiT != null) {
            socketWiT.disconnect(true, fast);     //stop reading from the socket
        }
        Log.d("Engine_Driver", "comm_thread.Shutdown: socketWit down");
        mainapp.saveSharedPreferencesToFileIfAllowed();
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
        Log.d("Engine_Driver", "comm_thread.Shutdown finished");
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    protected void sendThrottleName() {
        sendThrottleName(true);
    }

    private static void sendThrottleName(Boolean sendHWID) {
//        String serverType = mainapp.knownDCCEXserverIps.get(mainapp.host_ip);
//        if ( (!mainapp.isDCCEX) && (serverType != null) && (!serverType.equals("JMRI")) && (mainapp.prefDCCEX) ) { //assume it is DCC-EX
//            mainapp.isDCCEX = true;
//            Log.d("Engine_Driver", "comm_thread.sendThrottleName DCC-EX: Believe this is DCC-EX system ");
//        }
        mainapp.isDCCEX = mainapp.prefDCCEX;

        if (!mainapp.isDCCEX) { // not DCC-EX
            String s = prefs.getString("throttle_name_preference", threaded_application.context.getResources().getString(R.string.prefThrottleNameDefaultValue));
            wifiSend("N" + s);  //send throttle name
            if (sendHWID) {
                if (mainapp.deviceId.equals("")) {
                    mainapp.deviceId = Settings.System.getString(mainapp.getContentResolver(), Settings.System.ANDROID_ID);
                }
                if (mainapp.deviceId.equals("")) {
                    wifiSend("HU" + s);  //also send throttle name as the UDID
                } else {
                    wifiSend("HU" + mainapp.deviceId);
                }
            }

        } else { //DCC-EX // name is not relevant, so send a Command Station Status Request
//            Log.d("Engine_Driver", "comm_thread.sendThrottleName DCC-EX: <s>");
            if (mainapp.DCCEXlistsRequested < 0) { // if we haven't received all the lists go ask for them
                wifiSend("<s>");
                sendRequestRoster();
                sendRequestTurnouts();
                sendRequestRoutes();
                sendRequestTracks();
                mainapp.DCCEXlistsRequested = 0;  // don't ask again
            } else {
                wifiSend("<#>");
            }

        }
    }

    /* ask for specific loco to be added to a throttle
         input addr is formatted "L37<;>CSX37" or "S96" (if no roster name)
         msgTxt will be formatted M0+L1012<;>EACL1012 or M1+S96<;>S96 */
    static void sendAcquireLoco(String addr, int whichThrottle, int interval) {
        String rosterName;
        String address;
        String[] as = threaded_application.splitByString(addr, "<;>");
        if (as.length > 1) {
            address = as[0];
            rosterName = "E" + as[1];
        } else { //if no rostername, just use address for both
            address = addr;
            rosterName = addr;
        }

        String msgTxt;
        if (!mainapp.isDCCEX) { // not DCC-EX
            //format multithrottle request for loco M1+L37<;>ECSX37
            msgTxt = String.format("M%s+%s<;>%s", mainapp.throttleIntToString(whichThrottle), address, rosterName);  //add requested loco to this throttle
//            Log.d("Engine_Driver", "comm_thread.sendAquireLoco: acquireLoco: addr:'" + addr + "' msgTxt: '" + msgTxt + "'");
//            sendMsgDelay(comm_msg_handler, interval, message_type.WIFI_SEND, msgTxt);
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIFI_SEND, msgTxt);

            if (heart.getInboundInterval() > 0 && mainapp.withrottle_version > 0.0 && !heart.isHeartbeatSent()) {
//                sendMsgDelay(comm_msg_handler, interval + WITHROTTLE_SPACING_INTERVAL, message_type.SEND_HEARTBEAT_START);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SEND_HEARTBEAT_START);
            }

        } else { //DCC-EX
            if (!address.equals("*")) {
                msgTxt = String.format("<t %s>", address.substring(1));  //add requested loco to this throttle

                Consist con = mainapp.consists[whichThrottle];
                con.setConfirmed(address); // don't wait for confirmation
                con.setWhichSource(address, 1); //entered by address, not roster
                mainapp.addLocoToRecents(con.getLoco(address));
                wifiSend(msgTxt);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIFI_SEND, msgTxt);

                String lead = mainapp.consists[whichThrottle].getLeadAddr();
                if (lead.equals(address)) {
                    sendRequestRosterLocoDetails(address); // get the CS to resend the Loco details so we can get the functions
                }

                if (heart.getInboundInterval() > 0 && mainapp.withrottle_version > 0.0 && !heart.isHeartbeatSent()) {
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SEND_HEARTBEAT_START);
                }
                mainapp.sendMsgDelay(mainapp.comm_msg_handler, 1000L, message_type.REFRESH_FUNCTIONS);

                mainapp.alert_activities(message_type.RESPONSE, "M" + whichThrottle + "A" + address +"<;>s1");

            } else { // requesting the loco id on the programming track.  Using the DCC-EX driveway feature
                requestLocoIdForWhichThrottleDCCEX = whichThrottle;
                wifiSend("<R>");
            }

//            Log.d("Engine_Driver", "comm_thread.sendAcquireLoco DCC-EX: " + msgTxt);
        }
    }

    protected static void sendRequestRosterLocoDetails(String addr) {
        if (mainapp.isDCCEX) { // only relevant to DCC-EX
            wifiSend("<JR " + addr.substring(1) + ">");
        }
    }


    /* "steal" will send the MTS command */
    protected void sendStealLoco(String addr, int whichThrottle) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            if (addr != null) {
                String msgtxt = String.format("M%sS%s<;>%s", mainapp.throttleIntToString(whichThrottle), addr, addr);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIFI_SEND, msgtxt);
            }
        } // not relevant to DCC-EX
    }

    //release all locos for throttle using '*', or a single loco using address
    protected void sendReleaseLoco(String addr, int whichThrottle, long interval) {
        String msgTxt = "";
        if (!mainapp.isDCCEX) { // not DCC-EX
            msgTxt = String.format("M%s-%s<;>r", mainapp.throttleIntToString(whichThrottle), (!addr.equals("") ? addr : "*"));
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, interval, message_type.WIFI_SEND, msgTxt);

        }  // else  // DCC-EX has no equivalent
    }

    protected void reacquireAllConsists() {
        for (int i = 0; i < mainapp.maxThrottlesCurrentScreen; i++)
            reacquireConsist(mainapp.consists[i], i);
    }

    private void reacquireConsist(Consist c, int whichThrottle) {
        int delays = 0;
        for (Consist.ConLoco l : c.getLocos()) { // reacquire each confirmed loco in the consist
            if (l.isConfirmed()) {
                String addr = l.getAddress();
                String roster_name = l.getRosterName();
                if (roster_name != null)  // add roster selection info if present
                    addr += "<;>" + roster_name;
//                    sendAcquireLoco(addr, whichThrottle, delays * WITHROTTLE_SPACING_INTERVAL); //ask for next loco, with 0 or more delays
                sendAcquireLoco(addr, whichThrottle, 0); //ask for next loco, with 0 or more delays
                delays++;
            }
        }
    }

    protected void sendEStop(int whichThrottle) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            wifiSend(String.format("M%sA*<;>X", mainapp.throttleIntToString(whichThrottle)));  //send eStop request

        } else { //DCC-EX
            wifiSend("<!>");
            for (int throttleIndex = 0; throttleIndex<mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
                sendSpeed(throttleIndex, 0);
            }
//            Log.d("Engine_Driver", "comm_thread.sendEStop DCC-EX: ");
        }
    }

    protected void sendDisconnect() {
        if (!mainapp.isDCCEX) { // not DCC-EX
            wifiSend("Q");
            shutdown(true);

        } else { //  DCC-EX   no equivalent to a "Q" so just drop all the locos to be tidy
            Consist con = null;
            if (mainapp.consists.length > 0) {
                for (int i = 0; i < mainapp.consists.length; i++) {
                    con = mainapp.consists[i];
                    for (Consist.ConLoco l : con.getLocos()) {
                        sendReleaseLoco(l.getAddress(), i, 0);
                    }
                }
            }
            wifiSend("<U DISCONNECT>");  // this is not a real command.  just a placeholder that will be ignored by the CS
            shutdown(true);
        }
    }

    protected void sendFunction(char cWhichThrottle, String addr, int fn, int fState) {
        sendFunction(mainapp.throttleCharToInt(cWhichThrottle), addr, fn, fState);
    }

    @SuppressLint("DefaultLocale")
    protected void sendFunction(int whichThrottle, String addr, int fn, int fState) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            if (addr.length() == 0) addr = "*";
            wifiSend(String.format("M%dA%s<;>F%d%d", whichThrottle, addr, fState, fn));

        } else { //DCC-EX
            String msgTxt = "";

            String isLatching = mainapp.function_consist_latching.get(fn);
            int newfState = -1;

            if (mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle] != null) {  //  we have a roster specific latching for this
                if (fn < mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle].length) {
                    isLatching = mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle][fn] ? LATCHING_DEFAULT : "none";
                }
            } else {   // no roster entry
                if (fn>2) { // assume it is latching
                    isLatching = LATCHING_DEFAULT;
                }
            }

            if ((isLatching != null) && (isLatching.equals(LATCHING_DEFAULT))) {
                if (mainapp.function_states[whichThrottle][fn]) { // currently pressed
                    if (fState == 1) newfState = 0;
                } else { // not currently pressed
                    if (fState == 1) newfState = 1;
                }
            } else {
                newfState = fState;
            }

            if (newfState >= 0) {
                if ((addr.length() == 0) || (addr.equals("*"))) { // all on the throttle
                    Consist con = mainapp.consists[whichThrottle];
                    for (Consist.ConLoco l : con.getLocos()) {
                        msgTxt = String.format("<F %s %d %d>", l.getAddress().substring(1, l.getAddress().length()), fn, newfState);
                        wifiSend(msgTxt);
//                        Log.d("Engine_Driver", "comm_thread.sendSpeed DCC-EX: " + msgTxt);
                    }
                } else { // just one address
                    msgTxt = String.format("<F %s %d %d>", addr.substring(1, addr.length()), fn, newfState);
                    wifiSend(msgTxt);
//                    Log.d("Engine_Driver", "comm_thread.sendFunction DCC-EX: " + msgTxt);
                }
            }
        }
    }

    protected void sendForceFunction(char cWhichThrottle, String addr, int fn, int fState) {
        sendForceFunction(mainapp.throttleCharToInt(cWhichThrottle), addr, fn, fState);
    }

    @SuppressLint("DefaultLocale")
    protected void sendForceFunction(int whichThrottle, String addr, int fn, int fState) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            if (addr.length() == 0) addr = "*";
            wifiSend(String.format("M%dA%s<;>F%d%d", whichThrottle, addr, fn, fState));

        } else { //DCC-EX
            sendFunction(whichThrottle, addr, fn, fState);
        }
    }

    protected static void sendRequestRoster() {
        if (mainapp.isDCCEX) { // DCC-EX only
            String msgTxt = "<JR>";
            wifiSend(msgTxt);
//            Log.d("Engine_Driver", "comm_thread.sendRequestRoster DCC-EX: " + msgTxt);
        }
    }

    protected static void sendRequestTurnouts() {
        if (mainapp.isDCCEX) { // DCC-EX only
            String msgTxt = "<JT>";
            wifiSend(msgTxt);
//            Log.d("Engine_Driver", "comm_thread.sendRequestTurnouts DCC-EX: " + msgTxt);
        }
    }

    protected static void sendRequestRoutes() {
        if (mainapp.isDCCEX) { // DCC-EX only
            String msgTxt = "<JA>";
            wifiSend(msgTxt);
//            Log.d("Engine_Driver", "comm_thread.sendRequestRoutes DCC-EX: " + msgTxt);
        }
    }

    protected static void sendRequestTracks() {
        if (mainapp.isDCCEX) { // DCC-EX only
            float vn = 4;
            try {
                vn = Float.valueOf(mainapp.DCCEXversion);
            } catch (Exception e) { } // invalid version

            if (vn >= 04.002007) {  /// need to remove the track manager option
                String msgTxt = "<=>";
                wifiSend(msgTxt);
//            Log.d("Engine_Driver", "comm_thread.sendRequestTracks DCC-EX: " + msgTxt);
            }
        }
    }

    protected static void sendTrack(String track, String type, int id) {
        if (mainapp.isDCCEX) { // DCC-EX only
            String msgTxt = "";
            boolean needsId = false;
            for (int i=0; i<TRACK_TYPES.length; i++) {
                if (type.equals(TRACK_TYPES[i])) {
                    needsId = TRACK_TYPES_NEED_ID[i];
                    break;
                }
            }
            if (!needsId) {
                msgTxt = msgTxt + "<= " + track + " "+ type + ">";
                wifiSend(msgTxt);
            } else {
                msgTxt = msgTxt + "<= " + track + " "+ type + " " + id + ">";
                wifiSend(msgTxt);
            }
//            Log.d("Engine_Driver", "comm_thread.sendTracks DCC-EX: " + msgTxt);
        }
    }

    protected void sendTurnout(String cmd) {
//        Log.d("Engine_Driver", "comm_thread.sendTurnout: cmd=" + cmd);
        char action = cmd.charAt(0); //first char is action (C=close,T=throw,2=toggle)
        String systemName = cmd.substring(1); //remainder is systemName
        String cs = mainapp.getTurnoutState(systemName);
        if (cs == null) cs = ""; //avoid npe
        if (!mainapp.isDCCEX) { // not DCC-EX
            //special "toggle" handling for LnWi
            if (mainapp.getServerType().equals("Digitrax") && action == '2') {
                action = cs.equals("C")?'T':'C'; //LnWi states are C/T (not 2/4)
            }
            wifiSend("PTA" + action + systemName);

        } else { //DCC-EX, includes special toggle handling

            String to_id = cmd.substring(1);
            // check to see if the turnout is known and add it if it is not
            boolean found = false;
            for (int i=0; i<mainapp.to_system_names.length; i++) {
                if (mainapp.to_system_names[i].equals(to_id)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String msgTxt = "<T " + to_id + " DCC " + to_id + ">";
                wifiSend(msgTxt);
            }

            String translatedState = "T";
            switch (action) {
                case 'C':
                    translatedState = "C";
                case '2': { // toggle
                     if (cs.equals("4")) {
                            translatedState = "C";
                     }
                }
            }
            String msgTxt = "<T " + to_id + " " + translatedState + ">";              // format <T id 0|1|T|C>
            wifiSend(msgTxt);
//            Log.d("Engine_Driver", "comm_thread.sendTurnout DCC-EX: " + msgTxt);
        }
    }

    protected void sendRoute(String cmd) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            wifiSend("PRA" + cmd);

        } else { //DCC-EX    Route: </START id>      Automation: </START id addr>
            String systemName = cmd.substring(1);
            String msgTxt = "</START";
            try {
                String whichLoco = "";
                int type = -1;
                whichLoco = mainapp.getConsist(mainapp.whichThrottleLastTouch).getLeadAddr();
                if (whichLoco.length()>0) {
                    String routeType = "";
                    int routeId = Integer.parseInt(systemName);
                    for (int i = 0; i < mainapp.routeIDsDCCEX.length; i++) {
                        if (mainapp.routeIDsDCCEX[i]==routeId) {
                            routeType = mainapp.routeTypesDCCEX[i];
                            break;
                        }
                    }
                    if (routeType.equals("A")) // automation
                       msgTxt = msgTxt + " " + whichLoco.substring(1);
                }
            } catch (Exception ignored) {
            }
            msgTxt = msgTxt + " " + systemName + ">";
            wifiSend(msgTxt);
//            Log.d("Engine_Driver", "comm_thread.sendRoute DCC-EX: " + msgTxt);
        }
    }

    @SuppressLint("DefaultLocale")
    protected void sendPower(int pState) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            wifiSend(String.format("PPA%d", pState));

        } else { //DCC-EX
            String msgTxt = String.format("<%d>", pState);
            wifiSend(msgTxt);
//            Log.d("Engine_Driver", "comm_thread.sendPower DCC-EX: " + msgTxt);
        }
    }

    @SuppressLint("DefaultLocale")
    protected static void sendJoinDCCEX() {
        if (mainapp.isDCCEX) { // DCC-EX
            wifiSend("<1 JOIN>");
        }
    }

    protected void sendQuit() {
        if (!mainapp.isDCCEX) { // not DCC-EX
            if (socketWiT != null && socketWiT.SocketGood())
                wifiSend("Q");
        } /// N/A for DCC-EX
    }

    protected void sendHeartbeatStart() {
        if (!mainapp.isDCCEX) { // not DCC-EX
            if (socketWiT != null && socketWiT.SocketGood())
                heart.setHeartbeatSent(true);
            wifiSend("*+");

        } else { //DCC-EX
            heart.setHeartbeatSent(true);
            wifiSend("<#>"); // DCC-EX doesn't have heartbeat, so sending a command with a simple response
//            Log.d("Engine_Driver", "comm_thread.sendHeartbeatStart DCC-EX: <#>)");
        }
    }

    @SuppressLint("DefaultLocale")
    protected void sendDirection(int whichThrottle, String addr, int dir) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            wifiSend(String.format("M%sA%s<;>R%d", mainapp.throttleIntToString(whichThrottle), addr, dir));

        } else { //DCC-EX
            String msgTxt = "";
            if ((addr.length() == 0) || (addr.equals("*"))) { // all on the throttle
                Consist con = mainapp.consists[whichThrottle];
                msgTxt = "";
                for (Consist.ConLoco l : con.getLocos()) {
                    int newDir = dir;
                    if (l.isBackward()) newDir = (dir == 0) ? 1 : 0;
                    String fmt = ( (Float.valueOf(mainapp.DCCEXversion) < 4.0) ? "<t 0 %s %d %d>" : "<t %s %d %d>" );
                    msgTxt = String.format(fmt, l.getAddress().substring(1), mainapp.lastKnownSpeedDCCEX[whichThrottle], newDir);
                    wifiSend(msgTxt);
                    mainapp.lastKnownDirDCCEX[whichThrottle] = newDir;
//                    Log.d("Engine_Driver", "comm_thread.sendSpeed DCC-EX: " + msgTxt);
                }
            } else {
                String fmt = ( (Float.valueOf(mainapp.DCCEXversion) < 4.0) ? "<t 0 %s %d %d>" : "<t %s %d %d>" );
                msgTxt = String.format(fmt, addr.substring(1), mainapp.lastKnownSpeedDCCEX[whichThrottle], dir);
                wifiSend(msgTxt);
                mainapp.lastKnownDirDCCEX[whichThrottle] = dir;
//                Log.d("Engine_Driver", "comm_thread.sendDirection DCC-EX: " + msgTxt);
            }
        }
    }

    protected static void sendSpeedZero(int whichThrottle) {
        sendSpeed(whichThrottle, 0);
    }

    @SuppressLint("DefaultLocale")
    protected static void sendSpeed(int whichThrottle, int speed) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            wifiSend(String.format("M%sA*<;>V%d", mainapp.throttleIntToString(whichThrottle), speed));

        } else { //DCC-EX
            Consist con = mainapp.consists[whichThrottle];
            String msgTxt = "";
            int dir = mainapp.lastKnownDirDCCEX[whichThrottle];
            mainapp.lastKnownSpeedDCCEX[whichThrottle] = speed;
            for (Consist.ConLoco l : con.getLocos()) {
                int newDir = dir;
                if (l.isBackward()) newDir = (dir == 0) ? 1 : 0;
                String fmt = ( (Float.valueOf(mainapp.DCCEXversion) < 4.0) ? "<t 0 %s %d %d>" : "<t %s %d %d>" );
                msgTxt = String.format(fmt, l.getAddress().substring(1), speed, newDir);
                wifiSend(msgTxt);
//                Log.d("Engine_Driver", "comm_thread.sendSpeed DCC-EX: " + msgTxt);
            }
            mainapp.lastSpeedCommandSentTimeDCCEX[whichThrottle] = Calendar.getInstance().getTimeInMillis();
        }
    }

//    @SuppressLint("DefaultLocale")
//    private void sendRequestSpeed(int whichThrottle) {
//        if (!mainapp.isDCCEX) { // not DCC-EX
//            wifiSend(String.format("M%sA*<;>qV",
//                mainapp.throttleIntToString(whichThrottle)));
//
//        } else { //DCC-EX
//            Consist con = mainapp.consists[whichThrottle];
//            String msgTxt = "";
//            for (Consist.ConLoco l : con.getLocos()) {
//                msgTxt = String.format("<t %s>", l.getAddress().substring(1,l.getAddress().length()));
//                wifiSend(msgTxt);
////                Log.d("Engine_Driver", "comm_thread.sendRequestSpeed DCC-EX: " + msgTxt);
//            }
//        }
//    }
//
//    @SuppressLint("DefaultLocale")
//    private void sendRequestDir(int whichThrottle) {
//        if (!mainapp.isDCCEX) { // not DCC-EX
//            wifiSend(String.format("M%sA*<;>qR",
//                    mainapp.throttleIntToString(whichThrottle)));
//
//        } else { //DCC-EX
//            Consist con = mainapp.consists[whichThrottle];
//            String msgTxt = "";
//            for (Consist.ConLoco l : con.getLocos()) {
//                msgTxt = String.format("<t %s>", l.getAddress().substring(1,l.getAddress().length()));
//                wifiSend(msgTxt);
////                Log.d("Engine_Driver", "comm_thread.sendRequestDir DCC-EX: " + msgTxt);
//            }
//        }
//    }

    @SuppressLint("DefaultLocale")
    protected static void sendRequestSpeedAndDir(int whichThrottle) {
        if (!mainapp.isDCCEX) { // not DCC-EX
            wifiSend(String.format("M%sA*<;>qV\nM%sA*<;>qR",
                    mainapp.throttleIntToString(whichThrottle), mainapp.throttleIntToString(whichThrottle)));

        } else { //DCC-EX
            Consist con = mainapp.consists[whichThrottle];
            String msgTxt = "";
            for (Consist.ConLoco l : con.getLocos()) {
                msgTxt = String.format("<t %s>", l.getAddress().substring(1));
                wifiSend(msgTxt);
//                Log.d("Engine_Driver", "comm_thread.sendRequestSpeedAndDir DCC-EX: " + msgTxt);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    public static void sendWriteDecoderAddress(int addr) {
        if (mainapp.isDCCEX) { // DCC-EX only
            String msgTxt = String.format("<W %s>", addr);
            wifiSend(msgTxt);
        }
    }

    @SuppressLint("DefaultLocale")
    public static void sendReadCv(int cv) {
        if (mainapp.isDCCEX) { // DCC-EX only
            String msgTxt = String.format("<R %d>", cv);
            wifiSend(msgTxt);
        }
    }

    @SuppressLint("DefaultLocale")
    public static void sendWriteCv(int cvValue, int cv) {
        if (mainapp.isDCCEX) { // DCC-EX only
            String msgTxt = String.format("<W %d %d>", cv, cvValue);
            wifiSend(msgTxt);
        }
    }

    @SuppressLint("DefaultLocale")
    public static void sendWritePomCv(int cv, int cvValue, int addr) {
        if (mainapp.isDCCEX) { // DCC-EX only
            String msgTxt = String.format("<w %d %d %d>", addr, cv, cvValue);
            wifiSend(msgTxt);
        }
    }

    @SuppressLint("DefaultLocale")
    public static void sendDCCEXcommand(String msgTxt) {
        if (mainapp.isDCCEX) { // DCC-EX only
            wifiSend(msgTxt);
        }
    }


    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    protected static void processWifiResponse(String responseStr) {
            /* see java/arc/jmri/jmrit/withrottle/deviceserver.java for server code and some documentation
          VN<Version#>
          RL<RosterSize>]<RosterList>
          RF<RosterFunctionList>
          RS<2ndRosterFunctionList>
          *<HeartbeatIntervalInSeconds>
          PTL[<SystemTurnoutName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
          PTA<NewTurnoutState><SystemName>
          PPA<NewPowerState> where state 0=off, 1=on, 2=unknown
          M<throttleid> multi-throttle command
          TODO: add remaining items, or move examples into code below
             */

        //send response to debug log for review
        Log.d("Engine_Driver", "comm_thread.processWifiResponse: " + (mainapp.isDCCEX ? "DCC-EX" : "") + "<--:" + responseStr);

        boolean skipAlert = false;          //set to true if the Activities do not need to be Alerted

        if (!mainapp.isDCCEX) { // not DCC-EX

            switch (responseStr.charAt(0)) {

                case 'M': { //handle responses from MultiThrottle function
                    if (responseStr.length() < 5) { //must be at least Mtxs9
                        Log.d("Engine_Driver", "comm_thread.processWifiResponse: invalid response string: '" + responseStr + "'");
                        break;
                    }
                    String sWhichThrottle = responseStr.substring(1, 2);
                    int whichThrottle = mainapp.throttleCharToInt(sWhichThrottle.charAt(0));
                    String[] ls = threaded_application.splitByString(responseStr, "<;>");    //drop off separator
                    String addr = ls[0].substring(3);
                    char com2 = responseStr.charAt(2);
                    //loco was successfully added to a throttle
                    if (com2 == '+') {  //"MT+L2591<;>"  loco was added
                        Consist con = mainapp.consists[whichThrottle];
                        if (con.getLoco(addr) != null) { //loco was added to consist in select_loco
                            con.setConfirmed(addr);
                            mainapp.addLocoToRecents(con.getLoco(addr));
                        } else if (con.isWaitingOnID()) { //we were waiting for this response to get address
                            Consist.ConLoco conLoco = new Consist.ConLoco(addr);
                            conLoco.setFunctionLabelDefaults(mainapp, whichThrottle);
                            //look for RosterEntry which matches address returned
                            String rn = mainapp.getRosterNameFromAddress(conLoco.getFormatAddress(), true);
                            if (!rn.equals("")) {
                                conLoco.setIsFromRoster(true);
                                conLoco.setRosterName(rn);
                            }
                            con.add(conLoco);
                            con.setWhichSource(addr, 1); //entered by address, not roster
                            con.setConfirmed(addr);
//                            addLocoToRecents(con.getLoco(addr));
                            mainapp.addLocoToRecents(conLoco);
                            Log.d("Engine_Driver", "comm_thread.processWifiResponse: loco '" + addr + "' ID'ed on programming track and added to " + whichThrottle);
                        } else {
                            Log.d("Engine_Driver", "comm_thread.processWifiResponse: loco '" + addr + "' not selected but assigned by server to " + whichThrottle);
                        }

                        String consistname = mainapp.getConsistNameFromAddress(addr); //check for a JMRI consist for this address,
                        if (consistname != null) { //if found, request function keys for lead, format MTAS13<;>CL1234
                            String[] cna = threaded_application.splitByString(consistname, "+");
                            String cmd = String.format("M%sA%s<;>C%s", mainapp.throttleIntToString(whichThrottle), addr, mainapp.cvtToLAddr(cna[0]));
                            Log.d("Engine_Driver", "comm_thread.processWifiResponse: rqsting fkeys for lead loco " + mainapp.cvtToLAddr(cna[0]));
                            wifiSend(cmd);
                        }

                    } else if (com2 == '-') { //"MS-L6318<;>"  loco removed from throttle
                        mainapp.consists[whichThrottle].remove(addr);
                        Log.d("Engine_Driver", "comm_thread.processWifiResponse: loco " + addr + " dropped from " + mainapp.throttleIntToString(whichThrottle));

                    } else if (com2 == 'L') { //list of function buttons
                        if (mainapp.consists[whichThrottle].isLeadFromRoster()) { // if not from the roster ignore the function labels that WiT has sent back
                            String lead;
                            lead = mainapp.consists[whichThrottle].getLeadAddr();
                            if (lead.equals(addr))                        //*** temp - only process if for lead engine in consist
                                processRosterFunctionString("RF29}|{1234(L)" + ls[1], whichThrottle);  //prepend some stuff to match old-style
                            mainapp.consists[whichThrottle].setFunctionLabels(addr, "RF29}|{1234(L)" + ls[1], mainapp);
                        }

                    } else if (com2 == 'A') { //process change in function value  MTAL4805<;>F028
                        if (ls.length == 2 && "F".equals(ls[1].substring(0, 1))) {
                            try {
                                processFunctionState(whichThrottle, Integer.valueOf(ls[1].substring(2)), "1".equals(ls[1].substring(1, 2)));
                            } catch (NumberFormatException | StringIndexOutOfBoundsException ignore) {
                                Log.d("Engine_Driver", "comm_thread.processWifiResponse: bad incoming message data, unable to parse '" + responseStr + "'");
                            }
                        }

                    } else if (com2 == 'S') { //"MTSL4425<;>L4425" loco is in use, prompt for Steal
                        Log.d("Engine_Driver", "comm_thread.processWifiResponse: rcvd MTS, request prompt for " + addr + " on " + mainapp.throttleIntToString(whichThrottle));
                        mainapp.sendMsg(mainapp.throttle_msg_handler, message_type.REQ_STEAL, addr, whichThrottle);
                    }

                    break;
                }
                case 'V': // WiThrottle Protocol Version
                    if (responseStr.startsWith("VN")) {
                        Double old_vn = mainapp.withrottle_version;
                        try {
                            mainapp.withrottle_version = Double.parseDouble(responseStr.substring(2));
                        } catch (Exception e) {
                            Log.e("Engine_Driver", "comm_thread.processWifiResponse: invalid WiT version string");
                            mainapp.withrottle_version = 0.0;
                            break;
                        }
                        //only move on to Throttle screen if version received is 2.0+
                        if (mainapp.withrottle_version >= 2.0) {
                            if (!mainapp.withrottle_version.equals(old_vn)) { //only if changed
                                mainapp.sendMsg(mainapp.connection_msg_handler, message_type.CONNECTED);
//                            } else {
//                                Log.d("Engine_Driver", "comm_thread.processWifiResponse: version already set to " + mainapp.withrottle_version + ", ignoring");
                            }
                        } else {
                            threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppWiThrottleNotSupported, responseStr.substring(2)), Toast.LENGTH_SHORT);
                            socketWiT.disconnect(false);
                        }
                    } else {
                        Log.e("Engine_Driver", "comm_thread.processWifiResponse: invalid WiT version string");
                    }
                    break;

                case 'H': // Alert and Info Message
                    if (responseStr.charAt(1) == 'T') { //set hardware server type, HTMRC for example
                        mainapp.setServerType(responseStr.substring(2)); //store the type
                    } else if (responseStr.charAt(1) == 't') { //server description string "HtMy Server Details go here"
                        mainapp.setServerDescription(responseStr.substring(2)); //store the description
                    } else if (responseStr.charAt(1) == 'M') { //alert message sent from server to throttle
                        threaded_application.safeToast(responseStr.substring(2), Toast.LENGTH_LONG); // copy to UI as toast message
                        //see if it is a turnout fail
                        if ((responseStr.contains("Turnout")) || (responseStr.contains("create not allowed"))) {
                            Pattern pattern = Pattern.compile(".*'(.*)'.*");
                            Matcher matcher = pattern.matcher(responseStr);
                            if (matcher.find()) {
                                mainapp.sendMsg(mainapp.turnouts_msg_handler, message_type.WIT_TURNOUT_NOT_DEFINED, matcher.group(1));
                            }
                        }

                    } else if (responseStr.charAt(1) == 'm') { //info message sent from server to throttle
                        threaded_application.safeToast(responseStr.substring(2), Toast.LENGTH_SHORT); // copy to UI as toast message
                    }
                    break;

                case '*': // heartbeat
                    try {
                        mainapp.heartbeatInterval = Integer.parseInt(responseStr.substring(1)) * 1000;  //convert to milliseconds
                    } catch (Exception e) {
                        Log.d("Engine_Driver", "comm_thread.processWifiResponse: invalid WiT hearbeat string");
                        mainapp.heartbeatInterval = 0;
                    }
                    heart.startHeartbeat(mainapp.heartbeatInterval);
                    break;

                case 'R': // Roster
                    switch (responseStr.charAt(1)) {

                        case 'C':
                            if (responseStr.charAt(2) == 'C' || responseStr.charAt(2) == 'L') {  //RCC1 or RCL1 (treated the same in ED)
                                clearConsistList();
                            } else if (responseStr.charAt(2) == 'D') {  //RCD}|{88(S)}|{Consist Name]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
                                processConsistList(responseStr);
                            }
                            break;

                        case 'L':
                            //                  roster_list_string = responseStr.substring(2);  //set app variable
                            processRosterList(responseStr);  //process roster list
                            break;

                        case 'F':   //RF29}|{2591(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
                            processRosterFunctionString(responseStr.substring(2), 0);
                            break;

                        case 'S': //RS29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
                            processRosterFunctionString(responseStr.substring(2), 1);
                            break;

                    }  //end switch inside R
                    break;
                case 'P': //Panel
                    switch (responseStr.charAt(1)) {
                        case 'T': //turnouts
                            if (responseStr.charAt(2) == 'T') {  //turnout control allowed
                                processTurnoutTitles(responseStr);
                            }
                            if (responseStr.charAt(2) == 'L') {  //list of turnouts
                                processTurnoutList(responseStr);  //process turnout list
                            }
                            if (responseStr.charAt(2) == 'A') {  //action?  changes to turnouts
                                processTurnoutChange(responseStr);  //process turnout changes
                            }
                            break;

                        case 'R':  //routes
                            if (responseStr.charAt(2) == 'T') {  //route  control allowed
                                processRouteTitles(responseStr);
                            }
                            if (responseStr.charAt(2) == 'L') {  //list of routes
                                processRouteList(responseStr);  //process route list
                            }
                            if (responseStr.charAt(2) == 'A') {  //action?  changes to routes
                                processRouteChange(responseStr);  //process route changes
                            }
                            break;

                        case 'P':  //power
                            if (responseStr.charAt(2) == 'A') {  //change power state
                                String oldState = mainapp.power_state;
                                mainapp.power_state = responseStr.substring(3);
                                if (mainapp.power_state.equals(oldState)) {
                                    skipAlert = true;
                                }
                            }
                            break;

                        case 'F':  //FastClock message: PFT1581530521<;>2.0
                            //extracts and sets shared fastClockSeconds
                            if (responseStr.startsWith("PFT")) {
                                mainapp.fastClockSeconds = 0L;
                                String[] ta = threaded_application.splitByString(responseStr.substring(3), "<;>"); //get the number between "PFT" and the "<;>"
                                try {
                                    mainapp.fastClockSeconds = Long.parseLong(ta[0]);
                                } catch (NumberFormatException e) {
                                    Log.w("Engine_Driver", "unable to extract fastClockSeconds from '" + responseStr + "'");
                                }
                                mainapp.alert_activities(message_type.TIME_CHANGED, "");     //tell activities the time has changed
                            }
                            break;

                        case 'W':  //Web Server port
                            int oldPort = mainapp.web_server_port;
                            try {
                                mainapp.web_server_port = Integer.parseInt(responseStr.substring(2));  //set app variable
                            } catch (Exception e) {
                                Log.d("Engine_Driver", "comm_thread.processWifiResponse: invalid web server port string");
                            }
                            if (oldPort == mainapp.web_server_port) {
                                skipAlert = true;
                            } else {
//                                dlMetadataTask.get();           // start background metadata update
                                threaded_application.dlRosterTask.get();             // start background roster update

                            }
                            mainapp.webServerNameHasBeenChecked = false;
                            break;
                    }  //end switch inside P
                    break;
            }  //end switch

            /* ***********************************  *********************************** */

        } else { // DCC-EX
            if (responseStr.length() >= 3) {
                if (!(responseStr.charAt(0) == '<')) {
                    if (responseStr.contains("<")) { // see if we can clean it up
                        responseStr = responseStr.substring(responseStr.indexOf("<"));
                    }
                }

                if (responseStr.charAt(0) == '<') {
                    if ((mainapp.DCCEXscreenIsOpen) && (responseStr.charAt(1)!='#') ) {
                        mainapp.alert_activities(message_type.DCCEX_RESPONSE, responseStr);
                    }


                    String[] args = responseStr.substring(1, responseStr.length() - 1).split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 999);

                    switch (responseStr.charAt(1)) {
                        case 'i': // Command Station Information
                            String old_vn = mainapp.DCCEXversion;
                            String [] vn1 = args[1].split("-");
                            String [] vn2 = vn1[1].split("\\.");
                            String vn = String.format("%02d.%03d",Integer.parseInt(vn2[0]),Integer.parseInt(vn2[1]));
                            if (vn.length()>=3) {
                                try { vn = vn +String.format("%03d",Integer.parseInt(vn2[2]));
                                } catch (Exception ignored) {
                                    // try to pull a partial number
                                    String pn = "0";
                                    for (int j=0; j<vn2[2].length(); j++ ) {
                                        if ( (vn2[2].charAt(j)>='0') && (vn2[2].charAt(j)<='9') ) {
                                            pn = pn + vn2[2].charAt(j);
                                        } else { break; }
                                    }
                                    vn = vn +String.format("%03d", Integer.parseInt(pn));
                                }
                            }
                            mainapp.DCCEXversion = vn;
                            if (!mainapp.DCCEXversion.equals(old_vn)) { //only if changed
                                mainapp.sendMsg(mainapp.connection_msg_handler, message_type.CONNECTED);
                            } else {
                                Log.d("Engine_Driver", "comm_thread.processWifiResponse: version already set to " + mainapp.DCCEXversion + ", ignoring");
                            }

                            mainapp.withrottle_version = 4.0;  // fudge it
                            mainapp.setServerType("DCC-EX");
                            mainapp.setServerDescription(responseStr.substring(2, responseStr.length() - 1)); //store the description

                            skipAlert = true;
                            mainapp.heartbeatInterval = 20000; // force a heartbeat period
                            heart.startHeartbeat(mainapp.heartbeatInterval);
                            mainapp.power_state = "2"; // unknown
                            break;

                        case 'l':
                            processDCCEXlocos(args);
                            skipAlert = true;
                            break;

                        case 'r':
                            if (args.length<=2) { // response from a request for a loco id (the Drive away feature, and also the Address read)
                                processDCCEXRequestLocoIdResponse(args);
                            } else { // response from a CV write
                                processDCCEXRequestCvResponse(args);
                            }
                            skipAlert = true;
                            break;

                        case 'p': // power response
                            String oldState = mainapp.power_state;
                            mainapp.power_state = responseStr.substring(2, 3);
                            if (mainapp.power_state.equals(oldState)) {
                                skipAlert = true;
                            } else {
                                responseStr = "PPA" + responseStr.charAt(2);
                            }
                            break;

                        case 'j': //roster, turnouts / routes lists
                            skipAlert = true;
                            switch (responseStr.charAt(2)) {
                                case 'T': // turnouts
                                    processDCCEXturnouts(args);
                                    break;
                                case 'A': // automations/routes
                                    processDCCEXroutes(args);
                                    break;
                                case 'R': // roster
                                    skipAlert = processDCCEXroster(args);
                                    break;
                                case 'C': // fastclock
                                    processDCCEXfastClock(args);
                                    return;
                            }
                            break;

                        case 'H': //Turnout change
                            responseStr = "PTA" + (((args[2].equals("T")) || (args[2].equals("1"))) ? 4 : 2) + args[1];
                            processTurnoutChange(responseStr);
                            break;

                        case 'v': // response from a request a CV value
                            processDCCEXRequestCvResponse(args);
                            skipAlert = true;
                            break;

                        case 'w': // response from an address write or other CV write
                            responseStr = args[1];
                            if (!(args[1].charAt(0) =='-')) {
                                mainapp.alert_activities(message_type.WRITE_DECODER_SUCCESS, responseStr);
                            } else {
                                mainapp.alert_activities(message_type.WRITE_DECODER_FAIL, responseStr);
                            }
                            break;

                        case '=': // Track Manager response
                            processDCCEXtrackManagerResponse(args);
                            skipAlert = true;
                            break;

                    }

                } else { // ignore responses that don't start with "<"
                    skipAlert = true;
                }
            } else {
                skipAlert = true;
            }
        }
        if (!skipAlert) {
            mainapp.alert_activities(message_type.RESPONSE, responseStr);  //send response to running activities
        }
    }  //end of processWifiResponse

    /* ***********************************  *********************************** */

    private static void processDCCEXRequestCvResponse (String [] args) {
        String cv = "";
        String cvValue = "-1";

        if (args.length==3) {
            cv = args[1];
            cvValue = args[2];
        }

        mainapp.alert_activities(message_type.RECEIVED_CV, cv + "|" + cvValue);  //send response to running activities
    }

    private static void processDCCEXtrackManagerResponse(String [] args) {
        int trackNo = -1;
        String type = args[2];
        if (type.charAt(type.length() - 1) == '+')
            type = type.substring(0, type.length() - 1);

        if (args.length>=2) {
            trackNo = args[1].charAt(0)-65;
            if ( (trackNo>=0) && (trackNo<=mainapp.DCCEX_MAX_TRACKS) ) {
                int trackTypeIndex = -1
;               boolean needsId = false;
                for (int i=0; i<TRACK_TYPES.length; i++) {
                    if (type.equals(TRACK_TYPES[i])) {
                        trackTypeIndex = i;
                        needsId = TRACK_TYPES_NEED_ID[i];
                        break;
                    }
                }

                if (trackTypeIndex>=0) {
                    mainapp.DCCEXtrackType[trackNo] = trackTypeIndex;
                    mainapp.DCCEXtrackId[trackNo] = "";
                }
                if ( (needsId) && (args.length>=3) ) {
                    mainapp.DCCEXtrackId[trackNo] = args[3];
                }
                mainapp.DCCEXtrackAvailable[trackNo] = true;
            }
            mainapp.alert_activities(message_type.RECEIVED_TRACKS, type);  //send response to running activities
        }
    }

    private static void processDCCEXRequestLocoIdResponse(String [] args) {
        String responseStr = "";

        if (requestLocoIdForWhichThrottleDCCEX!=-1) { // if -1, request came from the CV read/write screen
            if (!(args[1].charAt(0) =='-')) {
                String addrStr = args[1];
                if (Integer.parseInt(args[1]) <= 127) {
                    addrStr = "S" + addrStr;
                } else {
                    addrStr = "L" + addrStr;
                }
                Consist con = mainapp.consists[requestLocoIdForWhichThrottleDCCEX];
                if (con.isWaitingOnID()) { //we were waiting for this response to get address
                    Consist.ConLoco conLoco = new Consist.ConLoco(addrStr);
                    conLoco.setFunctionLabelDefaults(mainapp, requestLocoIdForWhichThrottleDCCEX);
                    //look for RosterEntry which matches address returned
                    String rn = mainapp.getRosterNameFromAddress(conLoco.getFormatAddress(), true);
                    if (!rn.equals("")) {
                        conLoco.setIsFromRoster(true);
                        conLoco.setRosterName(rn);
                    }
                    con.add(conLoco);
                    con.setWhichSource(addrStr, 1); //entered by address, not roster
                    con.setConfirmed(addrStr);
    //                            addLocoToRecents(con.getLoco(addr));
                    mainapp.addLocoToRecents(conLoco);

                    sendAcquireLoco(addrStr, requestLocoIdForWhichThrottleDCCEX, 0);
                    sendJoinDCCEX();
                    mainapp.alert_activities(message_type.REQUEST_REFRESH_THROTTLE, "");

                } else {
                    mainapp.alert_activities(message_type.RECEIVED_DECODER_ADDRESS, args[1]);  //send response to running activities
                }
            }  else {// else {} did not succeed
                threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.DCCEXrequestLocoIdFailed), Toast.LENGTH_SHORT);
            }

        } else {
            mainapp.alert_activities(message_type.RECEIVED_DECODER_ADDRESS, args[1]);  //send response to running activities
        }

    }

    private static void processDCCEXlocos(String [] args) {
        String responseStr="";
        
        int dir = 0;
        int speed = Integer.parseInt(args[3]);
        if (speed >= 128) {
            speed = speed - 128;
            dir = 1;
        }
        if (speed>1) {
           speed = speed - 1; // get round and idiotic design of the speed command
        } else {
            speed=0;
        }

        String addr_str = args[1];
        if (Integer.parseInt(args[1]) <= 127) {
            addr_str = "S" + addr_str;
        } else {
            addr_str = "L" + addr_str;
        }
        Long timeSinceLastCommand;

        for (int throttleIndex = 0; throttleIndex<mainapp.maxThrottlesCurrentScreen; throttleIndex++) {   //loco may be the lead on more that one throttle
            int whichThrottle = mainapp.getWhichThrottleFromAddress(addr_str, throttleIndex);
            if (whichThrottle >= 0) {
                timeSinceLastCommand = Calendar.getInstance().getTimeInMillis() - mainapp.lastSpeedCommandSentTimeDCCEX[whichThrottle];

                if (timeSinceLastCommand>1000) {  // don't process an incoming speed if we sent a command for this throttle in the last second
                    mainapp.lastKnownSpeedDCCEX[whichThrottle] = speed;
                    mainapp.lastKnownDirDCCEX[whichThrottle] = dir;
                    responseStr = "M" + mainapp.throttleIntToString(whichThrottle) + "A" + addr_str + "<;>V" + speed;
                    mainapp.alert_activities(message_type.RESPONSE, responseStr);  //send response to running activities
                    responseStr = "M" + mainapp.throttleIntToString(whichThrottle) + "A" + addr_str + "<;>R" + dir;
                    mainapp.alert_activities(message_type.RESPONSE, responseStr);  //send response to running activities

                    // Process the functions
                    int fnState;
                    for (int i = 0; i < 27; i++) {
                        fnState = mainapp.bitExtracted(Integer.parseInt(args[4]), 1, i + 1);
                        processFunctionState(whichThrottle, i, (fnState != 0));
                        responseStr = "M" + mainapp.throttleIntToString(whichThrottle) + "A" + addr_str + "<;>F" + fnState + "" + (i);
                        mainapp.alert_activities(message_type.RESPONSE, responseStr);  //send response to running activities
                    }
                }

                throttleIndex = whichThrottle; // skip ahead
            }
        }
    } // end processDCCEXlocos()

    private static boolean processDCCEXroster(String [] args) {
        boolean skipAlert = true;

        if ( (args!=null) && (args.length>1)) {
            if ( (args.length<3) || (args[2].charAt(0) != '"') ) {  // loco list
                if (mainapp.rosterStringDCCEX.equals("")) {
                    mainapp.rosterStringDCCEX = "";
                    mainapp.rosterIDsDCCEX = new int[args.length - 1];
                    mainapp.rosterLocoNamesDCCEX = new String[args.length - 1];
                    mainapp.rosterLocoFunctionsDCCEX = new String[args.length - 1];
                    mainapp.rosterDetailsReceivedDCCEX = new boolean[args.length - 1];
                    for (int i = 0; i < args.length - 1; i++) { // first will be blank
                        mainapp.rosterIDsDCCEX[i] = Integer.parseInt(args[i + 1]);
                        mainapp.rosterDetailsReceivedDCCEX[i] = false;
                        wifiSend("<JR " + args[i + 1] + ">");
                    }
                }
            } else {  // individual loco
                if (mainapp.DCCEXlistsRequested < 3) {
                    if (mainapp.rosterIDsDCCEX != null) {
                        for (int i = 0; i < mainapp.rosterIDsDCCEX.length; i++) {
                            if (mainapp.rosterIDsDCCEX[i] == Integer.parseInt(args[1])) {
                                mainapp.rosterLocoNamesDCCEX[i] = args[2].substring(1, args[2].length() - 1);
                                mainapp.rosterLocoFunctionsDCCEX[i] = args[3]; // ignore this
                                mainapp.rosterDetailsReceivedDCCEX[i] = true;
                                break;
                            }
                        }

                        // check if we have all of them
                        boolean ready = true;
                        for (int i = 0; i < mainapp.rosterIDsDCCEX.length; i++) {
                            if (!mainapp.rosterDetailsReceivedDCCEX[i]) {
                                ready = false;
                                break;
                            }
                        }
                        if (ready) {
                            mainapp.rosterStringDCCEX = "RL" + mainapp.rosterIDsDCCEX.length;
                            for (int i = 0; i < mainapp.rosterIDsDCCEX.length; i++) {
                                mainapp.rosterStringDCCEX = mainapp.rosterStringDCCEX
                                        + "]\\[" + mainapp.rosterLocoNamesDCCEX[i]
                                        + "}|{" + mainapp.rosterIDsDCCEX[i]
                                        + "}|{" + (mainapp.rosterIDsDCCEX[i] <= 127 ? "S" : "L");
                            }
                            processRosterList(mainapp.rosterStringDCCEX);
                            mainapp.rosterStringDCCEX = "";
                            mainapp.DCCEXlistsRequested++;
                            Log.d("Engine_Driver", "comm_thread.processDCCEXroster: Roster complete. Count: " + mainapp.DCCEXlistsRequested);
                        }
                    }

                } else { // this a request for details on a specific loco - not part of the main roster request

                    String addr_str = args[1];
                    addr_str = ((Integer.parseInt(args[1]) <= 127) ? "S" : "L") + addr_str;

                    for (int throttleIndex = 0; throttleIndex<mainapp.maxThrottlesCurrentScreen; throttleIndex++) {  //loco may be the lead on more that one throttle

                        int whichThrottle = mainapp.getWhichThrottleFromAddress(addr_str, throttleIndex);
                        if (whichThrottle >= 0) {
                            String lead = mainapp.consists[whichThrottle].getLeadAddr();
                            if (lead.equals(addr_str)) {                       // only process the functions for lead engine in consist
                                if (args[3].length() > 2) {                       // only process the functions for lead engine in consist
                                    String[] fnArgs = args[3].substring(1, args[3].length() - 1).split("/", 999);
                                    mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle] = new boolean[args[3].length()];
                                    StringBuilder responseStrBuilder = new StringBuilder("RF29}|{1234(L)]\\[");  //prepend some stuff to match old-style
                                    for (int i = 0; i < fnArgs.length; i++) {
                                        if (fnArgs[i].length() == 0) {
                                            responseStrBuilder.append("]\\[");
                                            mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle][i] = false;
                                        } else {
                                            if (fnArgs[i].charAt(0) == '*') { // is NOT latching
                                                responseStrBuilder.append(fnArgs[i].substring(1)).append("]\\[");
                                                mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle][i] = false;
                                            } else {
                                                responseStrBuilder.append(fnArgs[i]).append("]\\[");
                                                mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle][i] = true;
                                            }
                                        }
                                    }
                                    processRosterFunctionString(responseStrBuilder.toString(), whichThrottle);

                                    mainapp.consists[whichThrottle].setFunctionLabels(addr_str, responseStrBuilder.toString(), mainapp);
                                    skipAlert = false;
                                } else {
                                    mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle] = null;
                                }
                            }

                            Consist con = mainapp.consists[whichThrottle];
                            if (con.getLoco(addr_str) != null) { //loco was added to consist in select_loco
                                con.setConfirmed(addr_str);
                                con.setWhichSource(addr_str, 1); //entered by address, not roster
                                mainapp.addLocoToRecents(con.getLoco(addr_str));
                            }

                            throttleIndex = whichThrottle; // skip ahead
                        }
                    }
                }
            }
        }
        return skipAlert;
    } // end processDCCEXroster()

    private static void processDCCEXfastClock(String [] args) {
        if (args!=null)  {
            if (args.length == 3) { // <jC mmmm ss>
                mainapp.fastClockSeconds = 0L;
                try {
                    mainapp.fastClockSeconds = Long.parseLong(args[1]) * 60;
                    mainapp.alert_activities(message_type.TIME_CHANGED, "");     //tell activities the time has changed
                } catch (NumberFormatException e) {
                    Log.w("Engine_Driver", "unable to extract fastClockSeconds from '" + args + "'");
                }
            }
        }
    }

    private static void processDCCEXturnouts(String [] args) {

        if (args!=null)  {
            if ( (args.length == 1)  // no Turnouts <jT>
                    || ((args.length == 3) && ((args[2].charAt(0) == 'C') || (args[2].charAt(0) == 'T') || (args[2].charAt(0) == 'X')) ) // <jT id state>     or <jT id X>
                    || ((args.length == 4) && (args[3].charAt(0) == '"') ) ) { // individual turnout  <jT id state "[desc]">
                boolean ready = true;
                boolean noTurnouts = false;

                if ( args.length == 1) { // no turnouts
                    noTurnouts = true;
                } else {
                    for (int i = 0; i < mainapp.turnoutIDsDCCEX.length; i++) {
                        if (mainapp.turnoutIDsDCCEX[i] == Integer.parseInt(args[1])) {
                            mainapp.turnoutStatesDCCEX[i] = args[2];
                            if ((args.length > 3) && (args[3].length() > 2)) {
                                mainapp.turnoutNamesDCCEX[i] = args[3].substring(1, args[3].length() - 1);
                            } else {
                                mainapp.turnoutNamesDCCEX[i] = "";
                            }
                            mainapp.turnoutDetailsReceivedDCCEX[i] = true;
                            break;
                        }
                    }
                    // check if we have all of them
                    for (int i = 0; i < mainapp.turnoutIDsDCCEX.length; i++) {
                        if (!mainapp.turnoutDetailsReceivedDCCEX[i]) {
                            ready = false;
                            break;
                        }
                    }
                }
                if (ready) {
                    mainapp.turnoutStringDCCEX = "PTL";
                    if (!noTurnouts) {
                        for (int i = 0; i < mainapp.turnoutIDsDCCEX.length; i++) {
                            mainapp.turnoutStringDCCEX = mainapp.turnoutStringDCCEX
                                    + "]\\[" + mainapp.turnoutIDsDCCEX[i]
                                    + "}|{" + mainapp.turnoutNamesDCCEX[i]
                                    + "}|{" + (mainapp.turnoutStatesDCCEX[i].equals("T") ? 4 : 2);
                        }
                    }
                    processTurnoutTitles("PTT]\\[Turnouts}|{Turnout]\\["
                            + mainapp.getResources().getString(R.string.DCCEXturnoutClosed) + "}|{2]\\["
                            + mainapp.getResources().getString(R.string.DCCEXturnoutThrown) + "}|{4]\\["
                            + mainapp.getResources().getString(R.string.DCCEXturnoutUnknown) + "}|{1]\\["
                            + mainapp.getResources().getString(R.string.DCCEXturnoutInconsistent) + "}|{8");
                    processTurnoutList(mainapp.turnoutStringDCCEX);
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_REFRESH_THROTTLE, "");
                    mainapp.turnoutStringDCCEX = "";
                    mainapp.DCCEXlistsRequested++;

                    int count = (mainapp.turnoutIDsDCCEX==null) ? 0 : mainapp.turnoutIDsDCCEX.length;
                    Log.d("Engine_Driver", "comm_thread.processDCCEXturnouts: Turnouts complete. Count: " + count);
                    mainapp.turnoutsBeingProcessedDCCEX = false;
                }

            } else { // turnouts list  <jT id1 id2 id3 ...>

                Log.d("Engine_Driver", "comm_thread.processDCCEXturnouts: Turnouts list received.");
                if (!mainapp.turnoutsBeingProcessedDCCEX) {
                    mainapp.turnoutsBeingProcessedDCCEX = true;
                    if (mainapp.turnoutStringDCCEX.equals("")) {
                        mainapp.turnoutStringDCCEX = "";
                        mainapp.turnoutIDsDCCEX = new int[args.length - 1];
                        mainapp.turnoutNamesDCCEX = new String[args.length - 1];
                        mainapp.turnoutStatesDCCEX = new String[args.length - 1];
                        mainapp.turnoutDetailsReceivedDCCEX = new boolean[args.length - 1];
                        for (int i = 0; i < args.length - 1; i++) { // first will be blank
                            mainapp.turnoutIDsDCCEX[i] = Integer.parseInt(args[i + 1]);
                            mainapp.turnoutDetailsReceivedDCCEX[i] = false;
                            wifiSend("<JT " + args[i + 1] + ">");
                        }

                        int count = (mainapp.turnoutIDsDCCEX==null) ? 0 : mainapp.turnoutIDsDCCEX.length;
                        Log.d("Engine_Driver", "comm_thread.processDCCEXturnouts: Turnouts list received. Count: " + count);
                    }
                }
            }
        }
    } // end processDCCEXturnouts()

    private static void processDCCEXroutes(String [] args) {

        if (args != null)  {
            if ( (args.length == 1)  // no Turnouts <jT>
                    || ((args.length == 3) && ((args[2].charAt(0) == 'R') || (args[2].charAt(0) == 'A') || (args[2].charAt(0) == 'X')) )  // <jA id type>  or <jA id X>
                    || ((args.length == 4) && (args[3].charAt(0) == '"') ) ) { // individual routes  <jA id type "[desc]">

                boolean ready = true;
                boolean noRoutes = false;

                if ( args.length == 1) { // no turnouts
                    noRoutes = true;
                } else {
                    for (int i = 0; i < mainapp.routeIDsDCCEX.length; i++) {
                        if (mainapp.routeIDsDCCEX[i] == Integer.parseInt(args[1])) {
                            mainapp.routeTypesDCCEX[i] = args[2];
                            mainapp.routeNamesDCCEX[i] = args[3].substring(1, args[3].length() - 1);
                            mainapp.routeDetailsReceivedDCCEX[i] = true;
                            break;
                        }
                    }
                    // check if we have all of them

                    for (int i = 0; i < mainapp.routeIDsDCCEX.length; i++) {
                        if (!mainapp.routeDetailsReceivedDCCEX[i]) {
                            ready = false;
                            break;
                        }
                    }
                }
                if (ready) {
                    mainapp.routeStringDCCEX = "PRL";
                    if (!noRoutes) {
                        for (int i = 0; i < mainapp.routeIDsDCCEX.length; i++) {
                            mainapp.routeStringDCCEX = mainapp.routeStringDCCEX
                                    + "]\\[" + mainapp.routeIDsDCCEX[i]
                                    + "}|{" + mainapp.routeNamesDCCEX[i]
                                    + "}|{" + (mainapp.routeTypesDCCEX[i].equals("R") ? 2 : 4);  //2=Route 4=Automation
                        }
                    }
                    processRouteTitles("PRT]\\[Routes}|{Route]\\["
                            + mainapp.getResources().getString(R.string.DCCEXrouteSet)+"}|{2]\\["
                            + mainapp.getResources().getString(R.string.DCCEXrouteHandoff) + "}|{4");
                    processRouteList(mainapp.routeStringDCCEX);
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_REFRESH_THROTTLE, "");
                    mainapp.routeStringDCCEX = "";
                    mainapp.DCCEXlistsRequested++;

                    int count = (mainapp.routeIDsDCCEX==null) ? 0 : mainapp.routeIDsDCCEX.length;
                    Log.d("Engine_Driver", "comm_thread.processDCCEXroutes: Routes complete. Count: " + count);
                    mainapp.routesBeingProcessedDCCEX = false;
                }

            } else { // routes list   <jA id1 id2 id3 ...>   or <jA> for empty

                Log.d("Engine_Driver", "comm_thread.processDCCEXroutes: Routes list received.");
                if (!mainapp.routesBeingProcessedDCCEX) {
                    mainapp.routesBeingProcessedDCCEX = true;
                    if (mainapp.routeStringDCCEX.equals("")) {
                        mainapp.routeStringDCCEX = "";
                        mainapp.routeIDsDCCEX = new int[args.length - 1];
                        mainapp.routeNamesDCCEX = new String[args.length - 1];
                        mainapp.routeTypesDCCEX = new String[args.length - 1];
                        mainapp.routeStatesDCCEX = new String[args.length - 1];
                        mainapp.routeDetailsReceivedDCCEX = new boolean[args.length - 1];
                        for (int i = 0; i < args.length - 1; i++) { // first will be blank
                            mainapp.routeIDsDCCEX[i] = Integer.parseInt(args[i + 1]);
                            mainapp.routeDetailsReceivedDCCEX[i] = false;
                            wifiSend("<JA " + args[i + 1] + ">");
                        }

                        int count = (mainapp.routeIDsDCCEX==null) ? 0 : mainapp.routeIDsDCCEX.length;
                        Log.d("Engine_Driver", "comm_thread.processDCCEXroutes: Routes list received. Count: " + count);
                    }
                }
            }
        }
    } // end processDCCEXroutes()

    /* ***********************************  *********************************** */

    //parse roster functions list into appropriate app variable array
    //  //RF29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
    static void processRosterFunctionString(String responseStr, int whichThrottle) {

        Log.d("Engine_Driver", "comm_thread.processRosterFunctionString: processing function labels for " + mainapp.throttleIntToString(whichThrottle));
        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //split into list of labels

        //populate a temp label array from RF command string
        LinkedHashMap<Integer, String> function_labels_temp = new LinkedHashMap<>();
        int i = 0;
        for (String ts : ta) {
            if (i > 0 && !"".equals(ts)) { //skip first chunk, which is length, and skip any blank entries
                function_labels_temp.put(i - 1, ts); //index is hashmap key, value is label string
            }  //end if i>0
            i++;
        }  //end for

        //set the appropriate global variable from the temp
        mainapp.function_labels[whichThrottle] = function_labels_temp;

    }

    //parse roster list into appropriate app variable array
    //  RL2]\[NS2591}|{2591}|{L]\[NS4805}|{4805}|{L
    static void processRosterList(String responseStr) {
        //clear the global variable
        mainapp.roster_entries = Collections.synchronizedMap(new LinkedHashMap<String, String>());
        //todo   RDB why don't we just clear the existing map with roster_entries.clear() instead of disposing and creating a new instance?

        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        //initialize app arrays (skipping first)
        int i = 0;
        for (String ts : ta) {
            if (i > 0) { //skip first chunk
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split these into name, address and length
                try {
                    mainapp.roster_entries.put(tv[0], tv[1] + "(" + tv[2] + ")"); //roster name is hashmap key, value is address(L or S), e.g.  2591(L)
                } catch (Exception e) {
                    Log.d("Engine_Driver", "comm_thread.processRosterList caught Exception");  //ignore any bad stuff in roster entries
                }
            }  //end if i>0
            i++;
        }  //end for

    }

    //parse consist list into appropriate mainapp hashmap
    //RCD}|{88(S)}|{Consist Name]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
    static void processConsistList(String responseStr) {
        String consist_addr = null;
        StringBuilder consist_desc = new StringBuilder();
        String consist_name = "";
        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        String plus = ""; //plus sign for a separator
        //initialize app arrays (skipping first)
        int i = 0;
        for (String ts : ta) {
            if (i == 0) { //first chunk is a "header"
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split header chunk into header, address and name
                consist_addr = tv[1];
                consist_name = tv[2];
            } else {  //list of locos in consist
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split these into loco address and direction
                consist_desc.append(plus).append(tv[0]);
                plus = "+";
            }  //end if i==0
            i++;
        }  //end for
        Log.d("Engine_Driver", "comm_thread.processConsistList: consist header, addr='" + consist_addr
                + "', name='" + consist_name + "', desc='" + consist_desc + "'");
        //don't add empty consists to list
        if (mainapp.consist_entries != null && consist_desc.length() > 0) {
            mainapp.consist_entries.put(consist_addr, consist_desc.toString());
        } else {
            Log.d("Engine_Driver", "comm_thread.processConsistList: skipping empty consist '" + consist_name + "'");
        }
    }

    //clear out any stored consists
    static void clearConsistList() {
        if (mainapp.consist_entries!=null) mainapp.consist_entries.clear();
    }

    static int findTurnoutPos(String systemName) {
        int pos = -1;
        for (String sn : mainapp.to_system_names) { //TODO: rewrite for better lookup
            pos++;
            if (sn != null && sn.equals(systemName)) {
                break;
            }
        }
        return pos;
    }

    //parse turnout change to update mainapp lists
    //  PTA<NewState><SystemName>
    //  PTA2LT12
    static void processTurnoutChange(String responseStr) {
        String newState = responseStr.substring(3, 4);
        String systemName = responseStr.substring(4);
        //save new turnout state for later lookups
        mainapp.putTurnoutState(systemName, newState);
    }  //end of processTurnoutChange

    //parse turnout list into appropriate app variable array
    //  PTL[<SystemName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
    //  PTL]\[LT12}|{my12}|{1
    static void processTurnoutList(String responseStr) {

        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        //initialize app arrays (skipping first)
        mainapp.to_system_names = new String[ta.length - 1];
        mainapp.to_user_names = new String[ta.length - 1];
        int i = 0;
        for (String ts : ta) {
            if (i > 0) { //skip first chunk, just message id
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split these into 3 parts, key and value
                if (tv.length == 3) { //make sure split worked
                    mainapp.to_system_names[i - 1] = tv[0];
                    mainapp.to_user_names[i - 1] = tv[1];
                    mainapp.putTurnoutState(tv[0], tv[2]);
                }
            }  //end if i>0
            i++;
        }  //end for

    }

    static void processTurnoutTitles(String responseStr) {
        //PTT]\[Turnouts}|{Turnout]\[Closed}|{2]\[Thrown}|{4

        //clear the global variable
        mainapp.to_state_names = new HashMap<>();

        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        //initialize app arrays (skipping first)
        int i = 0;
        for (String ts : ta) {
            if (i > 1) { //skip first 2 chunks
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split these into value and key
                mainapp.to_state_names.put(tv[1], tv[0]);
            }  //end if i>0
            i++;
        }  //end for
        //workaround for bug in LnWi (they define 2/4, but send C/T)
        if (mainapp.getServerType().equals("Digitrax")) {
            mainapp.to_state_names.put("T","Thrown");
            mainapp.to_state_names.put("C","Closed");
        }
    }
    //parse route list into appropriate app variable array
    //  PRA<NewState><SystemName>
    //  PRA2LT12
    static void processRouteChange(String responseStr) {
        String newState = responseStr.substring(3, 4);
        String systemName = responseStr.substring(4);
        int pos = -1;
        for (String sn : mainapp.rt_system_names) {
            pos++;
            if (sn != null && sn.equals(systemName)) {
                break;
            }
        }
        if (pos >= 0 && pos <= mainapp.rt_system_names.length) {  //if found, update to new value
            mainapp.rt_states[pos] = newState;
        }
    }  //end of processRouteChange

    //parse route list into appropriate app variable array
    //  PRL[<SystemName><UserName><State>]repeat where state 1=Unknown,2=Active,4=Inactive,8=Inconsistent
    //  PRL]\[LT12}|{my12}|{1
    static void processRouteList(String responseStr) {

        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        //initialize app arrays (skipping first)
        mainapp.rt_system_names = new String[ta.length - 1];
        mainapp.rt_user_names = new String[ta.length - 1];
        mainapp.rt_states = new String[ta.length - 1];
        int i = 0;
        for (String ts : ta) {
            if (i > 0) { //skip first chunk, just message id
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split these into 3 parts, key and value
                mainapp.rt_system_names[i - 1] = tv[0];
                mainapp.rt_user_names[i - 1] = tv[1];
                mainapp.rt_states[i - 1] = tv[2];
            }  //end if i>0
            i++;
        }  //end for

    }

    static void processRouteTitles(String responseStr) {
        //PRT

        //clear the global variable
        mainapp.rt_state_names = new HashMap<>();

        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        //initialize app arrays (skipping first)
        int i = 0;
        for (String ts : ta) {
            if (i > 1) { //skip first 2 chunks
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split these into value and key
                mainapp.rt_state_names.put(tv[1], tv[0]);
            }  //end if i>0
            i++;
        }  //end for
    }

    //parse function state string into appropriate app variable array
    static void processFunctionState(int whichThrottle, Integer fn, boolean fState) {

        boolean skip = (fn > 2) && (mainapp.prefAlwaysUseDefaultFunctionLabels)
                && ((mainapp.prefConsistFollowRuleStyle.equals(threaded_application.CONSIST_FUNCTION_RULE_STYLE_SPECIAL_EXACT))
                || (mainapp.prefConsistFollowRuleStyle.equals(threaded_application.CONSIST_FUNCTION_RULE_STYLE_SPECIAL_PARTIAL)));

        if (!skip) {
            try {
                mainapp.function_states[whichThrottle][fn] = fState;
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
        }
    }

    //
    // wifiSend(String msg)
    //
    //send formatted msg to the socket using multithrottle format
    //  intermessage gap enforced by requeueing messages as needed
    protected static void wifiSend(String msg) {
//            Log.d("Engine_Driver", "comm_thread.wifiSend: WiT send '" + msg + "'");
        if (msg == null) { //exit if no message
            Log.d("Engine_Driver", "comm_thread.wifiSend: --> null msg");
            return;
        } else if (socketWiT == null) {
            Log.e("Engine_Driver", "comm_thread.wifiSend: socketWiT is null, message '" + msg + "' not sent!");
            return;
        }

        long now = System.currentTimeMillis();
        long lastGap = now - lastSentMs;

        //send if sufficient gap between messages or msg is timingSensitive, requeue if not
        if (lastGap >= threaded_application.WiThrottle_Msg_Interval || timingSensitive(msg)) {
            //perform the send
            Log.d("Engine_Driver", "comm_thread.wifiSend: " + (mainapp.isDCCEX ? "DCC-EX" : "") + "           -->:" + msg.replaceAll("\n", "\u21B5") + " (" + lastGap + ")"); //replace newline with cr arrow
            lastSentMs = now;
            socketWiT.Send(msg);

            if (mainapp.DCCEXscreenIsOpen) { // only relevant to some DCC-EX commands that we want to see in the DCC-EC Screen.
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_COMMAND_ECHO, msg);
            }
        } else {
            //requeue this message
            int nextGap = Math.max((int) (lastQueuedMs - now), 0) + (threaded_application.WiThrottle_Msg_Interval + 5); //extra 5 for processing
            Log.d("Engine_Driver", "comm_thread.wifiSend: requeue:" + msg.replaceAll("\n", "\u21B5") +
                    ", lastGap=" + lastGap + ", nextGap=" + nextGap); //replace newline with cr arrow
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, nextGap, message_type.WIFI_SEND, msg);
            lastQueuedMs = now + nextGap;
        }
    }  //end wifiSend()

    /* true indicates that message should NOT be requeued as the timing of this message
         is critical.
     */
    private static boolean timingSensitive(String msg) {
        boolean ret = false;
        if (!mainapp.isDCCEX) {
            if (msg.matches("^M[0-5]A.{1,5}<;>F[0-1][\\d]{1,2}$")) {
                ret = true;
            } //any function key message
        }
        if (ret) Log.d("Engine_Driver", "comm_thread.timingSensitive: timeSensitive msg, not requeuing:");
        return ret;
    }

    public void run() {
        Looper.prepare();
        mainapp.comm_msg_handler = new comm_handler(mainapp, prefs, this);
        Looper.loop();
        Log.d("Engine_Driver", "comm_thread.run() exit");
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    static class socketWifi extends Thread {
        InetAddress host_address;
        Socket clientSocket = null;
        BufferedReader inputBR = null;
        PrintWriter outputPW = null;
        private volatile boolean endRead = false;           //signals rcvr to terminate
        private volatile boolean socketGood = false;        //indicates socket condition
        private volatile boolean inboundTimeout = false;    //indicates inbound messages are not arriving from WiT
        private boolean firstConnect = false;               //indicates initial socket connection was achieved
        private int connectTimeoutMs = 3000; //connection timeout in milliseconds
        private int socketTimeoutMs = 500; //socket timeout in milliseconds

        private final int MAX_INBOUND_TIMEOUT_RETRIES = 2;
        private int inboundTimeoutRetryCount = 0;           // number of consecutive inbound timeouts
        private boolean inboundTimeoutRecovery = false;     // attempting to force WiT to respond


        socketWifi() {
            super("socketWifi");
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
//                        show_toast_message("Can't determine IP address of " + host_ip, Toast.LENGTH_LONG);
                    threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppCantDetermineIp, mainapp.host_ip), Toast.LENGTH_SHORT);
                    socketOk = false;
                }
            }

            //socket
            if (socketOk) {
                try {
                    //look for someone to answer on specified socket, and set timeout
                    Log.d("Engine_Driver", "comm_thread.socketWifi: Opening socket, connectTimeout=" + connectTimeoutMs + " and socketTimeout=" + socketTimeoutMs);
                    clientSocket = new Socket();
                    InetSocketAddress sa = new InetSocketAddress(mainapp.host_ip, mainapp.port);
                    clientSocket.connect(sa, connectTimeoutMs);
                    Log.d("Engine_Driver", "comm_thread.socketWifi: Opening socket: Connect successful.");
                    clientSocket.setSoTimeout(socketTimeoutMs);
                    Log.d("Engine_Driver", "comm_thread.socketWifi: Opening socket: set timeout successful.");
                } catch (Exception except) {
                    if (!firstConnect) {
                        threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppCantConnect,
                                mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address, except.getMessage()), Toast.LENGTH_LONG);
                    }
                    if ((!mainapp.client_type.equals("WIFI")) && (mainapp.prefAllowMobileData)) { //show additional message if using mobile data
                        Log.d("Engine_Driver", "comm_thread.socketWifi: Opening socket: Using mobile network, not WIFI. Check your WiFi settings and Preferences.");
                        threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppNotWIFI, mainapp.client_type), Toast.LENGTH_LONG);
                    }
                    socketOk = false;
                }
            }

            //rcvr
            if (socketOk) {
                try {
                    inputBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                } catch (IOException except) {
                    threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorInputStream, except.getMessage()), Toast.LENGTH_SHORT);
                    socketOk = false;
                }
            }

            //start the socketWifi thread.
            if (socketOk) {
                if (!this.isAlive()) {
                    endRead = false;
                    try {
                        this.start();
                    } catch (IllegalThreadStateException except) {
                        //ignore "already started" errors
                        threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorStartingSocket, except.getMessage()), Toast.LENGTH_SHORT);
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
                    threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorCreatingOutputStream, e.getMessage()), Toast.LENGTH_SHORT);
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
            if (shutdown) {
                endRead = true;
                if (!fastShutdown) {
                    for (int i = 0; i < 5 && this.isAlive(); i++) {
                        try {
                            Thread.sleep(connectTimeoutMs);     //  give run() a chance to see endRead and exit
                        } catch (InterruptedException e) {
                            threaded_application.safeToast(threaded_application.context.getResources().getString(R.string.toastThreadedAppErrorSleepingThread, e.getMessage()), Toast.LENGTH_SHORT);
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
                    Log.d("Engine_Driver", "comm_thread.socketWifi: Error closing the Socket: " + e.getMessage());
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
                            if (str.length() > 0) {
                                heart.restartInboundInterval();
                                clearInboundTimeout();
                                processWifiResponse(str);
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        socketGood = this.SocketCheck();
                    } catch (IOException e) {
                        if (socketGood) {
                            Log.d("Engine_Driver", "comm_thread.run(): WiT rcvr error.");
                            socketGood = false;     //input buffer error so force reconnection on next send
                        }
                    }
                }
                if (!socketGood) {
                    SystemClock.sleep(500L);        //don't become compute bound here when the socket is down
                }
            }
            heart.stopHeartbeat();
            Log.d("Engine_Driver", "comm_thread.run(): socketWifi exit.");
        }

        @SuppressLint("StringFormatMatches")
        void Send(String msg) {
            boolean reconInProg = false;
            //reconnect socket if needed
            if (!socketGood || inboundTimeout) {
                String status;
                if (mainapp.client_address == null) {
                    status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNotConnected);
                    Log.d("Engine_Driver", "comm_thread.send(): WiT send reconnection attempt.");
                } else if (inboundTimeout) {
                    status = threaded_application.context.getResources().getString(R.string.statusThreadedAppNoResponse, mainapp.host_ip, Integer.toString(mainapp.port), heart.getInboundInterval());
                    Log.d("Engine_Driver", "comm_thread.send(): WiT receive reconnection attempt.");
                } else {
                    status = threaded_application.context.getResources().getString(R.string.statusThreadedAppUnableToConnect, mainapp.host_ip, Integer.toString(mainapp.port), mainapp.client_address);
                    Log.d("Engine_Driver", "comm_thread.send(): WiT send reconnection attempt.");
                }
                socketGood = false;

                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIT_CON_RETRY, status);

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
                        String status = "Connected to WiThrottle Server at " + mainapp.host_ip + ":" + mainapp.port;
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WIT_CON_RECONNECT, status);
                        Log.d("Engine_Driver", "comm_thread.send(): WiT reconnection successful.");
                        clearInboundTimeout();
                        heart.restartInboundInterval();     //socket is good so restart inbound heartbeat timer
                    }
                } catch (Exception e) {
                    Log.d("Engine_Driver", "comm_thread.send(): WiT xmtr error.");
                    socketGood = false;             //output buffer error so force reconnection on next send
                }
            }

            if (!socketGood) {
                mainapp.comm_msg_handler.postDelayed(heart.outboundHeartbeatTimer, 500L);   //try connection again in 0.5 second
            }
        }

        // Attempt to determine if the socket connection is still good.
        // unfortunatley isConnected returns true if the Socket was disconnected other than by calling close()
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

            final ConnectivityManager cm = (ConnectivityManager) mainapp.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if ("WIFI".equalsIgnoreCase(ni.getTypeName()))

                    if (!mainapp.prefAllowMobileData) {
                        // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                && (!mainapp.haveForcedWiFiConnection)) {

                            Log.d("Engine_Driver", "comm_thread.HaveNetworkConnection: NetworkRequest.Builder");
                            NetworkRequest.Builder request = new NetworkRequest.Builder();
                            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

                            cm.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {
                                @Override
                                public void onAvailable(Network network) {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                        ConnectivityManager.setProcessDefaultNetwork(network);
                                    } else {
                                        cm.bindProcessToNetwork(network);  //API23+
                                    }
                                }
                            });
                            mainapp.haveForcedWiFiConnection = true;
                        }
                    }

                if (ni.isConnected()) {
                    haveConnectedWifi = true;
                } else {
                    // attempt to resolve the problem where some devices won't connect over wifi unless mobile data is turned off
                    if (mainapp.prefAllowMobileData) {
                        haveConnectedWifi = true;
                    }
                }
                if ("MOBILE".equalsIgnoreCase(ni.getTypeName()))
                    if ((ni.isConnected()) && (mainapp.prefAllowMobileData)) {
                        haveConnectedMobile = true;
                    }
            }
            return haveConnectedWifi || haveConnectedMobile;
        }

        boolean SocketGood() {
            return this.socketGood;
        }

        void InboundTimeout() {
            if (++inboundTimeoutRetryCount >= MAX_INBOUND_TIMEOUT_RETRIES) {
                Log.d("Engine_Driver", "comm_thread.InboundTimeout: WiT max inbound timeouts");
                inboundTimeout = true;
                inboundTimeoutRetryCount = 0;
                inboundTimeoutRecovery = false;
                // force a send to start the reconnection process
                mainapp.comm_msg_handler.postDelayed(heart.outboundHeartbeatTimer, 200L);
            } else {
                Log.d("Engine_Driver", "comm_thread.InboundTimeout: WiT inbound timeout " +
                        Integer.toString(inboundTimeoutRetryCount) + " of " + MAX_INBOUND_TIMEOUT_RETRIES);
                // heartbeat should trigger a WiT reply so force that now
                inboundTimeoutRecovery = true;
                mainapp.comm_msg_handler.post(heart.outboundHeartbeatTimer);
            }
        }

        void clearInboundTimeout() {
            inboundTimeout = false;
            inboundTimeoutRecovery = false;
            inboundTimeoutRetryCount = 0;
        }
    }

    /* ******************************************************************************************** */

    static class heartbeat {
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
         * calcs the inbound and outbound intervals and starts the beating
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
                    outInterval = threaded_application.DEFAULT_OUTBOUND_HEARTBEAT_INTERVAL;
                } else {
//                        outInterval = (int) (heartbeatIntervalSetpoint * HEARTBEAT_RESPONSE_FACTOR);
                    outInterval = (int) (heartbeatIntervalSetpoint * ( (double) mainapp.prefHeartbeatResponseFactor) / 100);
                    //keep values in a reasonable range
                    if (outInterval < threaded_application.MIN_OUTBOUND_HEARTBEAT_INTERVAL)
                        outInterval = threaded_application.MIN_OUTBOUND_HEARTBEAT_INTERVAL;
                    if (outInterval > threaded_application.MAX_OUTBOUND_HEARTBEAT_INTERVAL)
                        outInterval = threaded_application.MAX_OUTBOUND_HEARTBEAT_INTERVAL;
                }
                heartbeatOutboundInterval = outInterval;

                // inbound interval
                int inInterval = mainapp.heartbeatInterval;
                if (heartbeatIntervalSetpoint == 0) {    // wit heartbeat is disabled so disable inbound heartbeat
                    inInterval = 0;
                } else {
                    if (inInterval < threaded_application.MIN_INBOUND_HEARTBEAT_INTERVAL)
                        inInterval = threaded_application.MIN_INBOUND_HEARTBEAT_INTERVAL;
                    if (inInterval < outInterval)
//                            inInterval = (int) (outInterval / HEARTBEAT_RESPONSE_FACTOR);
                        inInterval = (int) (outInterval / ( ((double) mainapp.prefHeartbeatResponseFactor) / 100) );
                    if (inInterval > threaded_application.MAX_INBOUND_HEARTBEAT_INTERVAL)
                        inInterval = threaded_application.MAX_INBOUND_HEARTBEAT_INTERVAL;
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
            mainapp.comm_msg_handler.removeCallbacks(outboundHeartbeatTimer);                   //remove any pending requests
            if (heartbeatOutboundInterval > 0) {
                mainapp.comm_msg_handler.postDelayed(outboundHeartbeatTimer, heartbeatOutboundInterval);    //restart interval
            }
        }

        //restartInboundInterval()
        //restarts the inbound interval timing - call this after receiving anything from WiT
        void restartInboundInterval() {
            mainapp.comm_msg_handler.removeCallbacks(inboundHeartbeatTimer);
            if (heartbeatInboundInterval > 0) {
                mainapp.comm_msg_handler.postDelayed(inboundHeartbeatTimer, heartbeatInboundInterval);
            }
        }

        void stopHeartbeat() {
            mainapp.comm_msg_handler.removeCallbacks(outboundHeartbeatTimer);           //remove any pending requests
            mainapp.comm_msg_handler.removeCallbacks(inboundHeartbeatTimer);
            heartbeatIntervalSetpoint = 0;
            Log.d("Engine_Driver", "comm_thread.stopHeartbeat: heartbeat stopped.");
        }

        //outboundHeartbeatTimer()
        //sends a periodic message to WiT
        private final Runnable outboundHeartbeatTimer = new Runnable() {
            @Override
            public void run() {
                mainapp.comm_msg_handler.removeCallbacks(this);             //remove pending requests
                if (heartbeatIntervalSetpoint != 0) {
                    boolean anySent = false;
                    if (!mainapp.isDCCEX) {
                        for (int i = 0; i < mainapp.numThrottles; i++) {
                            if (mainapp.consists[i].isActive()) {
                                sendRequestSpeedAndDir(i);
                                anySent = true;
                            }
                        }
                    }
                    // prior to JMRI 4.20 there were cases where WiT might not respond to
                    // speed and direction request.  If inboundTimeout handling is in progress
                    // then we always send the Throttle Name to ensure a response
                    if (!anySent || (mainapp.getServerType().equals("") && socketWiT.inboundTimeoutRecovery)) {
                        sendThrottleName(false);    //send message that will get a response
                    }
                    mainapp.comm_msg_handler.postDelayed(this, heartbeatOutboundInterval);   //set next beat
                }
            }
        };

        //inboundHeartbeatTimer()
        //display an alert message when there is no inbound traffic from WiT within required interval
        private final Runnable inboundHeartbeatTimer = new Runnable() {
            @Override
            public void run() {
                mainapp.comm_msg_handler.removeCallbacks(this); //remove pending requests
                if (heartbeatIntervalSetpoint != 0) {
                    if (socketWiT != null && socketWiT.SocketGood()) {
                        socketWiT.InboundTimeout();
                    }
                    mainapp.comm_msg_handler.postDelayed(this, heartbeatInboundInterval);    //set next inbound timeout
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
                Log.e("Engine_Driver", "SecurityException encountered (and ignored) for telMgr");
            }
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                if (prefs.getBoolean("stop_on_phonecall_preference",
                        mainapp.getResources().getBoolean(R.bool.prefStopOnPhonecallDefaultValue))) {
                    Log.d("Engine_Driver", "comm_thread.onCallStateChanged: Phone is OffHook, Stopping Trains");
                    for (int i = 0; i < mainapp.numThrottles; i++) {
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

