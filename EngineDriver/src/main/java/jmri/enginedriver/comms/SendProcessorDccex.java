package jmri.enginedriver.comms;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.source_type;

public class SendProcessorDccex {
    static final String activityName = "SendProcessorDccex";

    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;

    static String LATCHING_DEFAULT;
    static String LATCHING_DEFAULT_ENGLISH;

    public static void initialise(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread) {
        SendProcessorDccex.prefs = prefs;
        SendProcessorDccex.mainapp = mainapp;
        SendProcessorDccex.commThread = commThread;
    }

    /* ******************************************************************************************** */
    /* ******************************************************************************************** */

    public static void sendThrottleName(Boolean sendHWID) {
//    Log.d(threaded_application.applicationName, activityName + ": sendThrottleName DCC-EX: <s>");
        if (mainapp.DCCEXlistsRequested < 0) { // if we haven't received all the lists go ask for them
            comm_thread.wifiSend("<s>");
            sendDccexRequestRoster();
            if (!prefs.getBoolean("prefDccexSequenceItemRequests", false)) {
                sendDccexRequestTurnouts();
                sendDccexRequestTurnouts();
                sendDccexRequestRoutes();
                sendDccexRequestTracks();
            }
            sendDccexRequestEmergencyStopState();
            mainapp.DCCEXlistsRequested = 0;  // don't ask again
        } else {
            comm_thread.wifiSend("<#>");
        }
    }

    /* ******************************************************************************************** */

    // ask for specific loco to be added to a throttle
    public static void sendAcquireLoco(String addr, String rosterName, int whichThrottle) {

        if (!addr.equals("*")) {
            if (whichThrottle < mainapp.maxThrottles) {
                String msgTxt = String.format("<t %s>", addr.substring(1));  //add requested loco to this throttle

                Consist con = mainapp.consists[whichThrottle];
                con.setConfirmed(addr); // don't wait for confirmation
                con.setWhichSource(addr, con.getLoco(addr).getIsFromRoster() ? source_type.ROSTER : source_type.ADDRESS);
                mainapp.addLocoToRecents(con.getLoco(addr)); //DCC-EX
                comm_thread.wifiSend(msgTxt);

//                String lead = mainapp.consists[whichThrottle].getLeadAddr();
                sendDccexRequestRosterLocoDetails(addr); // get the CS to resend the Loco details so we can get the functions

                if (comm_thread.heart.getInboundInterval() > 0 && mainapp.withrottle_version > 0.0 && !comm_thread.heart.isHeartbeatSent()) {
                    mainapp.alertCommHandlerWithBundle(message_type.SEND_HEARTBEAT_START);
                }
                mainapp.alertActivitiesWithBundle(message_type.REFRESH_FUNCTIONS, 1000L);

                Bundle bundle = new Bundle();
                bundle.putInt(alert_bundle_tag_type.SPEED_STEPS, 1);
                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_SET_SPEED_STEP, bundle);

                Log.d(threaded_application.applicationName, activityName + ": sendAcquireLoco(): DCC-EX: " + msgTxt);

            } else {  // not a valid throttle. related to the roster download
                sendDccexRequestRosterLocoDetails(addr);
            }
        } else { // requesting the loco id on the programming track.  Using the DCC-EX driveway feature
            comm_thread.requestLocoIdForWhichThrottleDCCEX = whichThrottle;
            comm_thread.wifiSend("<R>");
        }
    }

    /* ******************************************************************************************** */

    public static void sendDccexRequestRosterLocoDetails(String addr) {
        comm_thread.wifiSend("<JR " + addr.substring(1) + ">");
    }

    /* ******************************************************************************************** */

    public static void sendDccexRequestEmergencyStopState() {
        comm_thread.wifiSend("<!Q>");
    }

    /* ******************************************************************************************** */

    public static void sendReleaseLoco(String addr, int whichThrottle) {
        // DCC-EX has no equivalent
    }

    /* ******************************************************************************************** */

    public static void sendEStop(int whichThrottle) {
//        wifiSend("<!>");
        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {
            comm_thread.sendSpeed(throttleIndex, -1); // -1 = EStop
        }
//        Log.d(threaded_application.applicationName, activityName + ": sendEStop(): DCC-EX: ");
    }

    /* ******************************************************************************************** */

    public static void sendDisconnect() {
        Log.d(threaded_application.applicationName, activityName + ": sendDisconnect(): ");

        //no equivalent to a WiThrottle "Q" so just drop all the locos to be tidy
        Consist con;
        if (mainapp.consists != null && mainapp.consists.length > 0) {
            for (int i = 0; i < mainapp.consists.length; i++) {
                con = mainapp.consists[i];
                for (Consist.ConLoco l : con.getLocos()) {
                    sendReleaseLoco(l.getAddress(), i);
                }
            }
        }
        if (mainapp.getServerType().equals("IoTT")) {
            comm_thread.wifiSend("<U DISCONNECT>");  // special command to disconnect IoTT clients
        }
        commThread.shutdown(true);
    }

    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    public static void sendFunction(int whichThrottle, String addr, int fn, int fState, boolean force) {

        String msgTxt;

        LATCHING_DEFAULT = mainapp.getString(R.string.prefFunctionConsistLatchingLightBellDefaultValue); // can change with language
        LATCHING_DEFAULT_ENGLISH = mainapp.getString(R.string.prefFunctionConsistLatchingLightBellDefaultValueEnglish); // can not change with language
        String isLatching = LATCHING_DEFAULT;

        int newfState = -1;

        if (mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle] != null) {  //  we have a roster specific latching for this
            if (fn < mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle].length) {
                isLatching = mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle][fn] ? LATCHING_DEFAULT : "none";
            }
        } else {   // no roster entry. go look at the DCC-EX/consist defaults
            isLatching = mainapp.function_consist_latching.get(fn);
        }

        if (isLatching == null) isLatching = LATCHING_DEFAULT;

        if (!force) {
            if ((isLatching.equals(LATCHING_DEFAULT)) || (isLatching.equals(LATCHING_DEFAULT_ENGLISH))) {
                if (mainapp.function_states[whichThrottle][fn]) { // currently pressed
                    if (fState == 1) newfState = 0;
                } else { // not currently pressed
                    if (fState == 0) newfState = 1;
                }

                if (fn < 10) { // special case for attached keyboards keys 0-9
//                        if (mainapp.numericKeyIsPressed[fn] == 0) {  // key down
//                        newfState = (mainapp.numericKeyFunctionStateAtTimePressed[fn]==0) ? 1 : 0;
                    // do nothing
//                        } else if (mainapp.numericKeyIsPressed[fn] == 1) { // key is up
                    if (mainapp.numericKeyIsPressed[fn] == 1) { // key is up
                        newfState = (mainapp.numericKeyFunctionStateAtTimePressed[fn] == 0) ? 1 : 0;
                        mainapp.numericKeyIsPressed[fn] = -1;
                        mainapp.numericKeyFunctionStateAtTimePressed[fn] = -1;
                    }
                }
            } else {
                newfState = fState;

                if (fn < 10) { // special case for attached keyboards keys 0-9
                    if (mainapp.numericKeyIsPressed[fn] == 0) {  // key is down
                        newfState = 1; // ON
                    } else if (mainapp.numericKeyIsPressed[fn] == 1) { // key is up
                        newfState = 0; // OFF
                        mainapp.numericKeyIsPressed[fn] = -1;  // these are not needed for momentary so just reset them
                        mainapp.numericKeyFunctionStateAtTimePressed[fn] = -1;
                    }
                }
            }
        } else {
            newfState = fState;
        }

        if (newfState >= 0) {
            if ((addr.isEmpty()) || (addr.equals("*"))) { // all on the throttle
                Consist con = mainapp.consists[whichThrottle];
                for (Consist.ConLoco l : con.getLocos()) {
                    msgTxt = String.format("<F %s %d %d>", l.getAddress().substring(1), fn, newfState);
                    comm_thread.wifiSend(msgTxt);
//                        Log.d(threaded_application.applicationName, activityName + ": sendSpeed(): DCC-EX: " + msgTxt);
                }
            } else { // just one address
                msgTxt = String.format("<F %s %d %d>", addr.substring(1), fn, newfState);
                comm_thread.wifiSend(msgTxt);
//                    Log.d(threaded_application.applicationName, activityName + ": sendFunction(): DCC-EX: " + msgTxt);
            }
        }
    }

    /* ******************************************************************************************** */

    public static void sendDccexRequestRoster() {
        if (!mainapp.dccexRosterRequested) { // only once, should not change
            mainapp.dccexRosterRequested = true;
            String msgTxt = "<JR>";
            comm_thread.wifiSend(msgTxt);
//            Log.d(threaded_application.applicationName, activityName + ": sendDccexRequestRoster(): DCC-EX: " + msgTxt);
        }
    }

    public static void sendDccexRequestTurnouts() {
        if (!mainapp.dccexTurnoutsRequested) { // only once, should not change
            mainapp.dccexTurnoutsRequested = true;
            mainapp.dccexTurnoutsBeingProcessed = false;
            String msgTxt = "<JT>";
            comm_thread.wifiSend(msgTxt);
//            Log.d(threaded_application.applicationName, activityName + ": sendDccexRequestTurnouts(): DCC-EX: " + msgTxt);
        }
    }

    public static void sendDccexRequestRoutes() {
        mainapp.dccexRoutesBeingProcessed = false;
        String msgTxt = "<JA>";
        comm_thread.wifiSend(msgTxt);
//        Log.d(threaded_application.applicationName, activityName + ": sendDccexRequestRoutes(): DCC-EX: " + msgTxt);
    }

    /* ******************************************************************************************** */

    public static void sendDccexRequestTracks() {
        float dccexVersionNumber = mainapp.getDccexVersionNumeric();

        if (dccexVersionNumber >= 04.002007) {  // need to remove the track manager option
            String msgTxt = "<=>";
            comm_thread.wifiSend(msgTxt);
//            Log.d(threaded_application.applicationName, activityName + ": sendDccexRequestTracks() DCC-EX: " + msgTxt);
        }
    }

    public static void sendDccexTrackPower(char track, int powerState) {
        String msgTxt = "<" + (powerState) + " " + track + ">";
        comm_thread.wifiSend(msgTxt);
    }

    public static void sendDccexTrack(char track, String type, int id) {
        String msgTxt = "";
        boolean needsId = false;
        for (int i = 0; i < comm_thread.TRACK_TYPES.length; i++) {
            if (type.equals(comm_thread.TRACK_TYPES[i])) {
                needsId = comm_thread.TRACK_TYPES_NEED_ID[i];
                break;
            }
        }
        if (!needsId) {
            msgTxt = msgTxt + "<= " + track + " " + type + ">";
            comm_thread.wifiSend(msgTxt);
        } else {
            msgTxt = msgTxt + "<= " + track + " " + type + " " + id + ">";
            comm_thread.wifiSend(msgTxt);
        }
//        Log.d(threaded_application.applicationName, activityName + ": sendTracks() DCC-EX: " + msgTxt);
    }

    public static void sendDccexJoinTracks() {
        sendDccexJoinTracks(true);
    }

    public static void sendDccexJoinTracks(boolean join) {
        if (join) {
            comm_thread.wifiSend("<1 JOIN>");
        } else {
            comm_thread.wifiSend("<0 PROG>");
            comm_thread.wifiSend("<1 PROG>");
        }
    }

    /* ******************************************************************************************** */

    public static void sendDccexEmergencyStopPauseResume() {
        sendDccexEmergencyStopPauseResume(true);
    }

    public static void sendDccexEmergencyStopPauseResume(boolean pause) {
        comm_thread.wifiSend("<!" + (pause ? "P" : "R") + ">");
    }

    /* ******************************************************************************************** */

    public static void sendDccexRequestInCommandStationConsistList() {
        if (mainapp.isWiThrottleProtocol()) return; // DCC-EX only

        comm_thread.wifiSend("<^>");
        if (mainapp.dccexInCommandStationConsists != null)
            mainapp.dccexInCommandStationConsists.clear();
    }

    /* ******************************************************************************************** */

    public static void sendTurnout(String systemName, char action) {
//        Log.d(threaded_application.applicationName, activityName + ": sendTurnout(): cmd=" + cmd);
        String cs = mainapp.getTurnoutState(systemName);
        if (cs == null) cs = ""; //avoid npe

        // check to see if the turnout is known and add it if it is not
        boolean found = false;
        for (int i = 0; i < mainapp.to_system_names.length; i++) {
            if (mainapp.to_system_names[i].equals(systemName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            String msgTxt = "<T " + systemName + " DCC " + systemName + ">";
            comm_thread.wifiSend(msgTxt);
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
        String msgTxt = "<T " + systemName + " " + translatedState + ">";              // format <T id 0|1|T|C>
        comm_thread.wifiSend(msgTxt);
//            Log.d(threaded_application.applicationName, activityName + ": sendTurnout(): DCC-EX: " + msgTxt);
    }

    /* ******************************************************************************************** */

    public static void sendRoute(String systemName, char action) {
        comm_thread.wifiSend("</START " + systemName + ">");
    }

    /* ******************************************************************************************** */

    // action is always ='2' but is currently unused
    @SuppressLint("DefaultLocale")
    public static void sendDccexAutomation(String systemName, char action, int automationLoco) {
        //Automation: </START addr id>
        String msgTxt = String.format("</START %d %s>", automationLoco, systemName);
        comm_thread.wifiSend(msgTxt);
        Log.d(threaded_application.applicationName, activityName + ": sendDccexAutomation(): DCC-EX: " + msgTxt);
    }

    /* ******************************************************************************************** */

    // not implemented
//    @SuppressLint("DefaultLocale")
//    public static void sendPowerStateRequest() {
//        if (mainapp.isDccexProtocol()) { //only works for DCC-EX
//            wifiSend("<s>");  // no specific command available, so just ask for the full CS status again
//        }
//    }

    @SuppressLint("DefaultLocale")
    public static void sendPower(int powerState) {
        String msgTxt = String.format("<%d>", powerState);
        comm_thread.wifiSend(msgTxt);
//         Log.d(threaded_application.applicationName, activityName + ": sendPower(): DCC-EX: " + msgTxt);
    }

    @SuppressLint("DefaultLocale")
    public static void sendPower(int pState, int track) {  // DCC-EX only
        if (mainapp.isWiThrottleProtocol()) return; // DCC-EX only

        char trackLetter = (char) ('A' + track);
        String msgTxt = String.format("<%d %s>", pState, trackLetter);
        comm_thread.wifiSend(msgTxt);
//            Log.d(threaded_application.applicationName, activityName + ": sendPower(): DCC-EX: " + msgTxt);
    }


    /* ******************************************************************************************** */

    public static void sendHeartbeatStart() {
        comm_thread.heart.setHeartbeatSent(true);
        comm_thread.wifiSend("<#>"); // DCC-EX doesn't have heartbeat, so sending a command with a simple response
//            Log.d(threaded_application.applicationName, activityName + ": sendHeartbeatStart(): DCC-EX: <#>)");
    }

    /* ******************************************************************************************** */

    // WiThrottle and DCC-EX
    @SuppressLint("DefaultLocale")
    public static void sendDirection(int whichThrottle, String addr, int dir) {
        String msgTxt;
        if ((addr.isEmpty()) || (addr.equals("*"))) { // all on the throttle
            Consist con = mainapp.consists[whichThrottle];
            for (Consist.ConLoco l : con.getLocos()) {
                int newDir = dir;
                if (l.isBackward()) newDir = (dir == 0) ? 1 : 0;
                String fmt = ( (mainapp.getDccexVersionNumeric() < 4.0) ? "<t 0 %s %d %d>" : "<t %s %d %d>" );
                msgTxt = String.format(fmt, l.getAddress().substring(1), mainapp.dccexLastKnownSpeed[whichThrottle], newDir);
                comm_thread.wifiSend(msgTxt);
                mainapp.dccexLastKnownDirection[whichThrottle] = newDir;
//                Log.d(threaded_application.applicationName, activityName + ": sendSpeed(): DCC-EX: " + msgTxt);
            }
        } else {
            String fmt = ( (mainapp.getDccexVersionNumeric() < 4.0) ? "<t 0 %s %d %d>" : "<t %s %d %d>" );
            msgTxt = String.format(fmt, addr.substring(1), mainapp.dccexLastKnownSpeed[whichThrottle], dir);
            comm_thread.wifiSend(msgTxt);
            if (mainapp.getConsist(whichThrottle).getLeadAddr().equals(addr)) {
                mainapp.dccexLastKnownDirection[whichThrottle] = dir;
            }
//            Log.d(threaded_application.applicationName, activityName + ": sendDirection(): DCC-EX: " + msgTxt);
        }
    }

    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    public static void sendSpeed(int whichThrottle, int speed) {
        Consist con = mainapp.consists[whichThrottle];
        String msgTxt;
        int dir = mainapp.dccexLastKnownDirection[whichThrottle];
        mainapp.dccexLastKnownSpeed[whichThrottle] = speed;
        for (Consist.ConLoco l : con.getLocos()) {
            int newDir = dir;
            if (l.isBackward()) newDir = (dir == 0) ? 1 : 0;
            String fmt = ((mainapp.getDccexVersionNumeric() < 4.0) ? "<t 0 %s %d %d>" : "<t %s %d %d>");
            if (speed >= 0) { // not Estop
                msgTxt = String.format(fmt, l.getAddress().substring(1), speed, newDir);
            } else { // Estop
                msgTxt = String.format(fmt, l.getAddress().substring(1), -1, newDir);
            }
            comm_thread.wifiSend(msgTxt);
            mainapp.dccexLastSpeedCommandSentTime[whichThrottle] = Calendar.getInstance().getTimeInMillis();

//                Log.d(threaded_application.applicationName, activityName + ": sendSpeed(): DCC-EX: " + msgTxt);
        }
    }

    @SuppressLint("DefaultLocale")
    public static void sendRequestSpeedAndDir(int whichThrottle) {
        Consist con = mainapp.consists[whichThrottle];
        String msgTxt;
        for (Consist.ConLoco l : con.getLocos()) {
            msgTxt = String.format("<t %s>", l.getAddress().substring(1));
            comm_thread.wifiSend(msgTxt);
//            Log.d(threaded_application.applicationName, activityName + ": sendRequestSpeedAndDir(): DCC-EX: " + msgTxt);
        }
    }

    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    public static void sendDccexWriteDecoderAddress(int addr) {
        String msgTxt = String.format("<W %s>", addr);
        comm_thread.wifiSend(msgTxt);
    }

    @SuppressLint("DefaultLocale")
    public static void sendDccexReadCv(int cv) {
        String msgTxt = String.format("<R %d>", cv);
        comm_thread.wifiSend(msgTxt);
    }

    @SuppressLint("DefaultLocale")
    public static void sendDccexWriteCv(int cv, int cvValue) {
        String msgTxt = String.format("<W %d %d>", cv, cvValue);
        comm_thread.wifiSend(msgTxt);
    }

    @SuppressLint("DefaultLocale")
    public static void sendDccexWritePomCv(int cv, int cvValue, int addr) {
        String msgTxt = String.format("<w %d %d %d>", addr, cv, cvValue);
        comm_thread.wifiSend(msgTxt);
    }

    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    public static void sendDccexCommand(String msgTxt) {
        comm_thread.wifiSend(msgTxt);
    }

    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    public static void sendDccexGetLocoAddress() {
        comm_thread.wifiSend("<R LOCOID>");
    }

    @SuppressLint("DefaultLocale")
    public static void sendDccexGetConsistAddress() {
        comm_thread.wifiSend("<R CONSIST>");
    }

    /* ******************************************************************************************** */

    @SuppressLint("DefaultLocale")
    public static void sendDccexCommandStationConsistAddLoco(String consistIdString, String locoIdString, int facing) {

        boolean foundConsist = false;

        String msgTxt;

        // see if there is already a consist
        if ( (mainapp.dccexInCommandStationConsists != null) && (!mainapp.dccexInCommandStationConsists.isEmpty()) ) {
            for (ArrayList<HashMap<String, String>> arrayList : mainapp.dccexInCommandStationConsists) {
                StringBuilder consistString = new StringBuilder();
                for (HashMap<String, String> map : arrayList) {
                    // Check if the HashMap contains the key and if its value matches
                    if (map.containsKey("loco_id")) {
                        String foundLocoIdString = map.get("loco_id");
                        if (foundLocoIdString == null) break;
                        if (foundLocoIdString.equals(consistIdString)) {
                            foundConsist = true;
                        }
                        if (!foundLocoIdString.equals(locoIdString)) {
                            String foundLocoFacing = map.get("loco_facing");
                            consistString.append(" ").append( (foundLocoFacing!=null) && (foundLocoFacing.equals("1")) ? "-" : "");
                            consistString.append(foundLocoIdString);
                        }
                    }
                }
                if (foundConsist) {
                    consistString.append(" ");
                    consistString.append( (facing==1) ? "-" : "");
                    consistString.append(locoIdString);
                    msgTxt = consistString.toString();
                    comm_thread.wifiSend(String.format("<^%s>", msgTxt));  /// will already have a leading space
                    break;
                }
            }
        }
        if (!foundConsist) { // new consist
//            msgTxt = String.format("<^ %s %s>", args[0], (args[2].equals("1") ? "-" : "") + args[1]);  //consist address + loco to remove
            msgTxt = String.format("<^ %s %s>", consistIdString, ((facing==1) ? "-" : "") + locoIdString);  //consist address + loco to remove
            comm_thread.wifiSend(msgTxt);
        }
    }

    @SuppressLint("DefaultLocale")
    public static void sendDccexCommandStationConsistRemoveLoco(String consistIdString, String locoIdString) {

        boolean foundConsist = false;
        boolean foundLocoInConsist = false;

        String msgTxt;

        // see if there is already a consist
        if ( (mainapp.dccexInCommandStationConsists != null) && (!mainapp.dccexInCommandStationConsists.isEmpty()) ) {
            for (ArrayList<HashMap<String, String>> arrayList : mainapp.dccexInCommandStationConsists) {
                StringBuilder consistString = new StringBuilder();
                for (HashMap<String, String> map : arrayList) {
                    // Check if the HashMap contains the key and if its value matches
                    if (map.containsKey("loco_id")) {
                        String foundLocoIdString = map.get("loco_id");
                        if (foundLocoIdString == null) break;
                        if (foundLocoIdString.equals(consistIdString)) {
                            foundConsist = true;
                        }
                        if (foundLocoIdString.equals(locoIdString)) {
                            foundLocoInConsist = true;
                        } else {
                            String foundLocoFacing = map.get("loco_facing");
                            consistString.append(" ").append( (foundLocoFacing!=null) && (foundLocoFacing.equals("1")) ? "-" : "");
                            consistString.append(foundLocoIdString);
                        }
                    }
                }
                if ( (foundConsist) && (foundLocoInConsist) ) {
                    msgTxt = consistString.toString();
                    comm_thread.wifiSend(String.format("<^%s>", msgTxt));  /// will already have a leading space
                    break;
                }
            }
        }
    }


}
