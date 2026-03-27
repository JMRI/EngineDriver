package jmri.enginedriver.comms;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.turnout_action_type;

public class SendProcessorWiThrottle {
    static final String activityName = "SendProcessorWiThrottle";

    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;

    public static void initialise(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread) {
        SendProcessorWiThrottle.prefs = prefs;
        SendProcessorWiThrottle.mainapp = mainapp;
        SendProcessorWiThrottle.commThread = commThread;
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    public static void sendThrottleName(Boolean sendHWID) {
        String s = prefs.getString("prefThrottleName", mainapp.getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue));
        comm_thread.wifiSend("N" + s);  //send throttle name
        if (sendHWID) {
            comm_thread.wifiSend("HU" + mainapp.getFakeDeviceId());
        }
    }

    /* ******************************************************************************************** */

    // ask for specific loco to be added to a throttle
    public static void sendAcquireLoco(String addr, String rosterName, int whichThrottle) {
        String rosterNamePrefix ="E";
        if ( (rosterName==null) || (rosterName.trim().isEmpty()) ) { //if blank rosterName, just use address for both
            rosterName = addr;
            rosterNamePrefix = "";
        }
        // msgTxt will be formatted M0+L1012<;>EACL1012 or M1+S96<;>S96
        String msgTxt = String.format("M%s+%s<;>%s%s", mainapp.throttleIntToString(whichThrottle), addr, rosterNamePrefix, rosterName);  //add requested loco to this throttle
        Log.d(threaded_application.applicationName, activityName + ": sendAquireLoco(): sendAcquireLoco: addr:'" + addr + " rosterName: '" + rosterName + "' msgTxt: '" + msgTxt + "'");
        comm_thread.wifiSend(msgTxt);

        if (comm_thread.heart.getInboundInterval() > 0 && mainapp.withrottle_version > 0.0 && !comm_thread.heart.isHeartbeatSent()) {
            mainapp.alertCommHandlerWithBundle(message_type.SEND_HEARTBEAT_START);
        }
    }


    /* "steal" will send the MTS command */
    public static void sendStealLoco(String addr, int whichThrottle) {
        if (addr != null) {
            String msgTxt = String.format("M%sS%s<;>%s", mainapp.throttleIntToString(whichThrottle), addr, addr);
            comm_thread.wifiSend(msgTxt);
        }
    }

    //release all locos for throttle using '*', or a single loco using address
    public static void sendReleaseLoco(String addr, int whichThrottle) {
        String msgTxt;
        String cmd = prefs.getBoolean("prefUseDispatchCommand", false) ? "d" : "r";  // by default, use 'r'elease
        msgTxt = String.format("M%s-%s<;>%s", mainapp.throttleIntToString(whichThrottle), (!addr.isEmpty() ? addr : "*"), cmd);
        comm_thread.wifiSend(msgTxt);
    }

    //dispatch all locos for throttle using '*', or a single loco using address
    public static void sendDispatchLoco(String addr, int whichThrottle) {
        String msgTxt;
        msgTxt = String.format("M%s-%s<;>%s", mainapp.throttleIntToString(whichThrottle), (!addr.isEmpty() ? addr : "*"), "d");
        comm_thread.wifiSend(msgTxt);
    }

    public static void sendEStop(int whichThrottle) {
        comm_thread.wifiSend(String.format("M%sA*<;>X", mainapp.throttleIntToString(whichThrottle)));  //send eStop request
    }

    public static void sendDisconnect() {
        Log.d(threaded_application.applicationName, activityName + ": sendDisconnect(): ");
        comm_thread.wifiSend("Q");
        commThread.shutdown(true);
    }

    @SuppressLint("DefaultLocale")
    public static void sendFunction(int whichThrottle, String addr, int fn, int fState, boolean force) {
        if (addr.isEmpty()) addr = "*";
        comm_thread.wifiSend(String.format("M%1$dA%2$s<;>F%3$d%4$d", whichThrottle, addr, fState, fn));
    }

    public static void sendTurnout(String systemName, char action) {
//        Log.d(threaded_application.applicationName, activityName + ": sendTurnout(): cmd=" + cmd);
        String cs = mainapp.getTurnoutState(systemName);
        if (cs == null) cs = ""; //avoid npe
        //special "toggle" handling for LnWi
        if (mainapp.getServerType().equals("Digitrax") && (action == turnout_action_type.TOGGLE) ) {
            action = cs.equals("C")?'T':'C'; //LnWi states are C/T (not 2/4)
        }
        comm_thread.wifiSend("PTA" + action + systemName);
    }

    public static void sendRoute(String systemName, char action) {
        comm_thread.wifiSend("PRA" + action + systemName);
    }

    @SuppressLint("DefaultLocale")
    public static void sendPower(int powerState) {
        comm_thread.wifiSend(String.format("PPA%d", powerState));
    }

    public static void sendQuit() {
        Log.d(threaded_application.applicationName, activityName + ": sendQuit(): ");
        if (comm_thread.socketWiT != null && comm_thread.socketWiT.SocketGood())
            comm_thread.wifiSend("Q");
    }

    public static void sendHeartbeatStart() {
        if (comm_thread.socketWiT != null && comm_thread.socketWiT.SocketGood())
            comm_thread.heart.setHeartbeatSent(true);
        comm_thread.wifiSend("*+");
    }

    // WiThrottle and DCC-EX
    @SuppressLint("DefaultLocale")
    public static void sendDirection(int whichThrottle, String addr, int dir) {
        comm_thread.wifiSend(String.format("M%sA%s<;>R%d", mainapp.throttleIntToString(whichThrottle), addr, dir));
    }

    @SuppressLint("DefaultLocale")
    public static void sendSpeed(int whichThrottle, int speed) {
        comm_thread.wifiSend(String.format("M%sA*<;>V%d", mainapp.throttleIntToString(whichThrottle), speed));
    }

    @SuppressLint("DefaultLocale")
    public static void sendRequestSpeedAndDir(int whichThrottle) {
        comm_thread.wifiSend(String.format("M%sA*<;>qV", mainapp.throttleIntToString(whichThrottle)));
        comm_thread.wifiSend(String.format("M%sA*<;>qR", mainapp.throttleIntToString(whichThrottle)));
    }

    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    public static void sendWriteDirectDccCommand(String [] args) {
        String msgTxt = "";
        if (args.length==5) {
            msgTxt = String.format("D2%s %s %s %s %s", args[0], args[1], args[2], args[3], args[4] );
        } else if (args.length==6) {
            msgTxt = String.format("D2%s %s %s %s %s %s", args[0], args[1], args[2], args[3], args[4], args[5] );
        }
        comm_thread.wifiSend(msgTxt);

        Bundle bundle = new Bundle();
        bundle.putString(alert_bundle_tag_type.COMMAND,msgTxt);
        mainapp.alertActivitiesWithBundle(message_type.WRITE_DIRECT_DCC_COMMAND_ECHO, bundle, activity_id_type.WITHROTTLE_CV_PROGRAMMER);
    }

    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    public static void sendAdvancedConsistAddLoco(String consistIdString, String consistName, String locoIdString, int facing) {
        String facingString = (facing==0) ? "true" : "false";
        String msgTxt = String.format("RC+<;>%s<;>%s<:>%s<;>%s", consistIdString, consistName, locoIdString, facingString);  //consist address + loco to add + direction (true or false)
        comm_thread.wifiSend(msgTxt);
    }

    @SuppressLint("DefaultLocale")
    public static void sendAdvancedConsistRemoveLoco(String consistIdString, String locoIdString) {
        String msgTxt = String.format("RC-<;>%s<:>%s", consistIdString, locoIdString);  //consist address + loco to remove
        comm_thread.wifiSend(msgTxt);
    }

//    @SuppressLint("DefaultLocale")
//    public static void sendAdvancedConsistRemoveConsist(String [] args) {
//        if (args.length==1) {
//            String msgTxt = String.format("RCR<;>%s", args[0]);  //consist address to remove
//            comm_thread.wifiSend(msgTxt);
//        }
//    }
}
