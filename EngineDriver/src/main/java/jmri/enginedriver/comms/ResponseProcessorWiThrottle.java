package jmri.enginedriver.comms;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.SharedPreferences;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.message_type;

public class ResponseProcessorWiThrottle {
    static final String activityName = "ResponseProcessorWiThrottle";

    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;

    public static void initialise(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread) {
        ResponseProcessorWiThrottle.prefs = prefs;
        ResponseProcessorWiThrottle.mainapp = mainapp;
        ResponseProcessorWiThrottle.commThread = commThread;
    }

    public static boolean processWifiResponse(String responseStr) {
        boolean skipDefaultAlertToAllActivities = false;

        switch (responseStr.charAt(0)) {

            // WiThrottle protocol only
            case 'M': { //handle responses from MultiThrottle function
                // --- M{throttleId}{command}{locoId}<;>{command}
                if (responseStr.length() < 5) { //must be at least Mtxs9
                    Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): invalid response string: '" + responseStr + "'");
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
                        mainapp.addLocoToRecents(con.getLoco(addr)); // WiT
                    } else if (con.isWaitingOnID()) { //we were waiting for this response to get address
                        Consist.ConLoco conLoco = new Consist.ConLoco(addr);
                        conLoco.setFunctionLabelDefaults(mainapp, whichThrottle);
                        //look for RosterEntry which matches address returned
                        String rn = mainapp.getRosterNameFromAddress(conLoco.getFormatAddress(), true);
                        if (!rn.isEmpty()) {
                            conLoco.setIsFromRoster(true);
                            conLoco.setRosterName(rn);
                        }
                        con.add(conLoco);
                        con.setWhichSource(addr, 1); //entered by address, not roster
                        con.setConfirmed(addr);
                        mainapp.addLocoToRecents(conLoco); // WiT
                        Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): loco '" + addr + "' ID'ed on programming track and added to " + whichThrottle);
                    } else {
                        Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): loco '" + addr + "' not selected but assigned by server to " + whichThrottle);
                    }

                    String consistName = mainapp.getConsistNameFromAddress(addr); //check for a JMRI consist for this address,
                    if (consistName != null) { //if found, request function keys for lead, format MTAS13<;>CL1234
                        String[] cna = threaded_application.splitByString(consistName, "+");
                        String cmd = String.format("M%sA%s<;>C%s", mainapp.throttleIntToString(whichThrottle), addr, mainapp.cvtToLAddr(cna[0]));
                        Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): rqsting fkeys for lead loco " + mainapp.cvtToLAddr(cna[0]));
                        comm_thread.wifiSend(cmd);
                    }

                    Bundle bundle = new Bundle();
                    bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                    mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_LOCO_ADDED, bundle);
                    skipDefaultAlertToAllActivities = true;

                } else if (com2 == '-') { //"MS-L6318<;>"  loco removed from throttle
                    mainapp.consists[whichThrottle].remove(addr);
                    Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): loco " + addr + " dropped from " + mainapp.throttleIntToString(whichThrottle));

                    Bundle bundle = new Bundle();
                    bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                    mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_LOCO_REMOVED, bundle);
                    skipDefaultAlertToAllActivities = true;

                } else if (com2 == 'L') { //list of function buttons
                    if ( (mainapp.consists[whichThrottle].isLeadFromRoster())  // if not from the roster ignore the function labels that WiT has sent back
                            || (mainapp.prefAlwaysUseFunctionsFromServer) ) { // unless overridden by the preference
                        String lead = mainapp.consists[whichThrottle].getLeadAddr();
                        if (lead.equals(addr)) {                        //*** temp - only process if for lead engine in consist
                            comm_thread.processRosterFunctionString("RF29}|{1234(L)" + ls[1], whichThrottle);  //prepend some stuff to match old-style
                            mainapp.consists[whichThrottle].getLoco(lead).setIsServerSuppliedFunctionLabels(true);
                        }
                    }
                    // save them in recents regardless
                    if (mainapp.consists[whichThrottle].getLoco(addr) != null) {
                        Consist.ConLoco loco = mainapp.consists[whichThrottle].getLoco(addr);
                        LinkedHashMap<Integer, String> functonMap =  threaded_application.parseFunctionLabels("RF29}|{1234(L)" + ls[1]);
                        mainapp.addLocoToRecents(loco, functonMap); // WiT
                        loco.setFunctionLabels("RF29}|{1234(L)" + ls[1]);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                    mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_FUNCTION_LABELS_UPDATE, bundle);
                    skipDefaultAlertToAllActivities = true;

                } else if (com2 == 'A') { //process change in function value  MTAL4805<;>F028
                    if (ls.length >= 2) { //make sure there's a value to parse
                        char com3 = ls[1].charAt(0);
                        Bundle bundle = new Bundle();

                        if (com3 == 'V') {
                            try {
                                int speedWiT = Integer.parseInt(ls[1].substring(1));
                                bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                                bundle.putInt(alert_bundle_tag_type.SPEED, speedWiT);
                                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_SET_SPEED, bundle, activity_id_type.THROTTLE);
                                skipDefaultAlertToAllActivities = true;
                            } catch (Exception ignored) {
                            }

                        } else if (com3 == 'R') { // set direction
                            int dir;
                            try {
                                dir = Integer.parseInt(ls[1].substring(1, 2));
                            } catch (Exception e) {
                                dir = 1;
                            }
                            bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                            bundle.putInt(alert_bundle_tag_type.DIRECTION, dir);
                            bundle.putString(alert_bundle_tag_type.LOCO, addr);
                            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_SET_DIRECTION, bundle, activity_id_type.THROTTLE);
                            skipDefaultAlertToAllActivities = true;

                        } else if (com3 == 'F') {
                            try {
                                comm_thread.processFunctionState(whichThrottle, Integer.valueOf(ls[1].substring(2)), "1".equals(ls[1].substring(1, 2)));
                            } catch (NumberFormatException |
                                     StringIndexOutOfBoundsException ignore) {
                                Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): bad incoming message data, unable to parse '" + responseStr + "'");
                            }
                            try {
                                int function = Integer.parseInt(ls[1].substring(2));
                                int action = Integer.parseInt(ls[1].substring(1, 2));
                                bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                                bundle.putString(alert_bundle_tag_type.LOCO, addr);
                                bundle.putInt(alert_bundle_tag_type.FUNCTION, function);
                                bundle.putInt(alert_bundle_tag_type.FUNCTION_ACTION, action);
                                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_SET_FUNCTION, bundle, activity_id_type.THROTTLE);
                                skipDefaultAlertToAllActivities = true;

                            } catch (Exception e) {
                                // ignore
                            }
                        } else if (com3 == 's') {
                            try {
                                int speedStepCode = Integer.parseInt(ls[1].substring(1));
                                bundle.putInt(alert_bundle_tag_type.SPEED_STEPS, speedStepCode);
                                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_SET_SPEED_STEP, bundle, activity_id_type.THROTTLE);
                                skipDefaultAlertToAllActivities = true;

                            } catch (Exception ignored) {
                            }
                        }
                    }

                } else if (com2 == 'S') { //"MTSL4425<;>L4425" loco is in use, prompt for Steal
                    Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): rcvd MTS, request prompt for " + addr + " on " + mainapp.throttleIntToString(whichThrottle));
                    Bundle bundle = new Bundle();
                    bundle.putString(alert_bundle_tag_type.LOCO, addr);
                    bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                    mainapp.alertActivitiesWithBundle(message_type.RECEIVED_REQ_STEAL, bundle, activity_id_type.THROTTLE);
                    skipDefaultAlertToAllActivities = true;
                }

                break;
            }

            // WiThrottle protocol only
            case 'V': // WiThrottle Protocol Version
                // --- VN{Version#}
                if (responseStr.startsWith("VN")) {
                    Double old_vn = mainapp.withrottle_version;
                    try {
                        mainapp.withrottle_version = Double.parseDouble(responseStr.substring(2));
                    } catch (Exception e) {
                        Log.e(threaded_application.applicationName, activityName + ": processWifiResponse(): invalid WiT version string");
                        mainapp.withrottle_version = 0.0;
                        break;
                    }
                    //only move on to Throttle screen if version received is 2.0+
                    if (mainapp.withrottle_version >= 2.0) {
                        if (!mainapp.withrottle_version.equals(old_vn)) { //only if changed
                            mainapp.alertActivitiesWithBundle(message_type.CONNECTED, activity_id_type.CONNECTION);
//                            } else {
//                                Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): version already set to " + mainapp.withrottle_version + ", ignoring");
                        }
                    } else {
                        mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastThreadedAppWiThrottleNotSupported, responseStr.substring(2)), LENGTH_SHORT);
                        comm_thread.socketWiT.disconnect(false);
                    }
                } else {
                    Log.e(threaded_application.applicationName, activityName + ": processWifiResponse(): invalid WiT version string");
                }
                break;

            // WiThrottle protocol only
            case 'H': // Alert and Info Message
                // --- HM«messageText»
                // --- Hm«messageText»
                // --- HT«serverType»
                // --- Ht«serverDescription»
                if (responseStr.charAt(1) == 'T') { //set hardware server type, HTMRC for example
                    mainapp.setServerType(responseStr.substring(2)); //store the type

                    if ("MRC".equals(mainapp.getServerType())) {
                        // If connected to the MRC WiFi adapter, treat as PW, which isn't coming
                        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_WEB_PORT);
                    }
                    skipDefaultAlertToAllActivities = true;

                } else if (responseStr.charAt(1) == 't') { //server description string "HtMy Server Details go here"
                    mainapp.setServerDescription(responseStr.substring(2)); //store the description
                    skipDefaultAlertToAllActivities = true;

                } else if (responseStr.charAt(1) == 'M') { //alert message sent from server to throttle

                    if (!comm_thread.acceptMessageOrAlert(responseStr.substring(2)))  { skipDefaultAlertToAllActivities = true; break;}

                    if (prefs.getBoolean("prefBeepOnAlertToasts", mainapp.getResources().getBoolean(R.bool.prefBeepOnAlertToastsDefaultValue))) {
                        mainapp.playTone(ToneGenerator.TONE_PROP_ACK);
                    }
                    mainapp.vibrate(new long[]{1000, 500, 1000, 500});
                    mainapp.safeToast(responseStr.substring(2), Toast.LENGTH_LONG); // copy to UI as toast message
                    //see if it is a turnout fail
                    if ((responseStr.contains("Turnout")) || (responseStr.contains("create not allowed"))) {
                        Pattern pattern = Pattern.compile(".*'(.*)'.*");
                        Matcher matcher = pattern.matcher(responseStr);
                        if (matcher.find()) {
                            Bundle bundle = new Bundle();
                            bundle.putString(alert_bundle_tag_type.COMMAND, matcher.group(1));
                            mainapp.alertActivitiesWithBundle(message_type.WIT_TURNOUT_NOT_DEFINED, bundle, activity_id_type.TURNOUTS);
                        }
                    }
                    skipDefaultAlertToAllActivities = true;

                } else if (responseStr.charAt(1) == 'm') { //info message sent from server to throttle
                    if ( (responseStr.length() > 8) && (responseStr.substring(2,7).equals("ESTOP")) ) {
                        // only a DCC-EX command station should send this message
                        ResponseProcessorDccex.processDccexEmergencyStopResponse(responseStr.substring(8));
                        skipDefaultAlertToAllActivities = true;
                        break;
                    }
                    if (!comm_thread.acceptMessageOrAlert(responseStr.substring(2)))  { skipDefaultAlertToAllActivities = true; break;}

                    mainapp.safeToast(responseStr.substring(2), Toast.LENGTH_LONG); // copy to UI as toast message
                    skipDefaultAlertToAllActivities = true;
                }
                break;

            // WiThrottle protocol only
            case '*': // heartbeat
                // --- *«HeartbeatIntervalInSeconds»
                try {
                    mainapp.heartbeatInterval = Integer.parseInt(responseStr.substring(1)) * 1000;  //convert to milliseconds
                } catch (Exception e) {
                    Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): invalid WiT heartbeat string");
                    mainapp.heartbeatInterval = 0;
                }
                comm_thread.heart.startHeartbeat(mainapp.heartbeatInterval);
                skipDefaultAlertToAllActivities = true;
                break;

            // WiThrottle protocol only
            case 'R': // Roster
                // --- RC«command»<;>«consistAddress»<;>«additionalInfo»
                // --- RL«numberOfEntries»]\[«entryList»
                // --- RF{RosterFunctionList}
                // --- RS{2ndRosterFunctionList}
                switch (responseStr.charAt(1)) {

                    case 'C': // Advanced Consist (Multiple Unit) Commands
                        if (responseStr.charAt(2) == 'C' || responseStr.charAt(2) == 'L') {  //RCC1 or RCL1 (treated the same in ED)
                            clearConsistList();
                        } else if (responseStr.charAt(2) == 'D') {  //RCD}|{88(S)}|{Consist Name]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
                            processConsistList(responseStr);
                        }
                        break;

                    case 'L': // Roster List
                        //                  roster_list_string = responseStr.substring(2);  //set app variable
                        processRosterList(responseStr);  //process roster list
                        break;

                    // TODO: I am not convinced this command exists. PRA
                    case 'F':   //RF29}|{2591(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
                        comm_thread.processRosterFunctionString(responseStr.substring(2), 0);
                        break;

                    // TODO: I am not convinced this command exists. PRA
                    case 'S': //RS29}|{4805(L)]\[Light]\[Bell]\[Horn]\[Air]\[Uncpl]\[BrkRls]\[]\[]\[]\[]\[]\[]\[Engine]\[]\[]\[]\[]\[]\[BellSel]\[HornSel]\[]\[]\[]\[]\[]\[]\[]\[]\[
                        comm_thread.processRosterFunctionString(responseStr.substring(2), 1);
                        break;

                }  //end switch inside R

                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_ROSTER_UPDATE, activity_id_type.SELECT_LOCO);
                skipDefaultAlertToAllActivities = true;
                break;

            // WiThrottle protocol only
            case 'P': //Turnouts/Routes/Power/WebPort/Fastclock
                // --- PTT]\[«turnoutsTitle»}\{«turnoutTitle»[\[«stateTitle»}\{«stateValue»...
                // --- PTL]\[{«SystemTurnoutName»}|{«UserName»}|{«State»}... where state 1=Unknown. 2=Closed, 4=Thrown
                // --- PRT]\[«stateTitle»}\{«stateValue»...
                // --- PRL]\[{«SystemRouteName»}|{«UserName»}|{«State»}...
                // --- PRA2«routeName»    action is always 2
                // --- PTA«turnoutAction»«turnoutName»
                // --- PPA«NewPowerState» where state 0=off, 1=on, 2=unknown
                // --- PFT«seconds»<;>«fastTimeRatio»
                // --- PW«webPort»

                char char2 = (responseStr.length() >= 3) ? responseStr.charAt(2) : ' ';

                switch (responseStr.charAt(1)) {

                    // WiThrottle protocol
                    case 'T': //turnouts
                        if (char2 == 'T') {  //turnout control allowed
                            processTurnoutTitles(responseStr);
                        } else if (char2 == 'L') {  //list of turnouts
                            mainapp.hasRosterTurnouts = true;
                            processTurnoutList(responseStr);  //process turnout list
                        } else if (char2 == 'A') {  //action?  changes to turnouts
                            processTurnoutChange(responseStr);  //process turnout changes
                        } else {
                            break;
                        }
                        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_TURNOUT_UPDATE);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // WiThrottle protocol
                    case 'R':  //routes
                        if (char2 == 'T') {  //route  control allowed
                            processRouteTitles(responseStr);
                        } else if (char2 == 'L') {  //list of routes
                            processRouteList(responseStr);  //process route list
                        } else if (char2 == 'A') {  //action?  changes to routes
                            processRouteChange(responseStr);  //process route changes
                        } else {
                            break;
                        }
                        mainapp.alertActivitiesWithBundle(message_type.ROUTE_LIST_CHANGED);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // WiThrottle protocol
                    case 'P':  //power
                        if (char2 == 'A') {  //change power state
                            String oldState = mainapp.power_state;
                            mainapp.power_state = responseStr.substring(3);
                            if (!mainapp.power_state.equals(oldState)) {
                                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_POWER_STATE_CHANGE);
                            }
                            skipDefaultAlertToAllActivities = true;
                        }
                        break;

                    // WiThrottle protocol
                    case 'F':  //FastClock message: PFT1581530521<;>2.0
                        //extracts and sets shared fastClockSeconds
                        if (responseStr.startsWith("PFT")) {
                            mainapp.fastClockSeconds = 0L;
                            String[] ta = threaded_application.splitByString(responseStr.substring(3), "<;>"); //get the number between "PFT" and the "<;>"
                            try {
                                mainapp.fastClockSeconds = Long.parseLong(ta[0]);
                                skipDefaultAlertToAllActivities = true;
                            } catch (NumberFormatException e) {
                                Log.w("Engine_Driver", "unable to extract fastClockSeconds from '" + responseStr + "'");
                            }
                            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_TIME_CHANGE);
                        }
                        break;

                    // WiThrottle protocol
                    case 'W':  //Web Server port
                        int oldPort = mainapp.web_server_port;
                        try {
                            mainapp.web_server_port = Integer.parseInt(responseStr.substring(2));  //set app variable
                        } catch (Exception e) {
                            Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): invalid web server port string");
                        }
                        if (oldPort != mainapp.web_server_port) {
//                                dlMetadataTask.get();           // start background metadata update
                            threaded_application.dlRosterTask.get();             // start background roster update

                            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_WEB_PORT);
                        }
                        mainapp.webServerNameHasBeenChecked = false;
                        skipDefaultAlertToAllActivities = true;

                        break;
                }  //end switch inside P
                break;
        }  //end switch

        return skipDefaultAlertToAllActivities;
    }


        /* ***********************************  *********************************** */


    // parse roster list into appropriate app variable array
    //  RL2]\[NS2591}|{2591}|{L]\[NS4805}|{4805}|{L
    public static void processRosterList(String responseStr) {
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
                    Log.d(threaded_application.applicationName, activityName + ": processRosterList(): caught Exception");  //ignore any bad stuff in roster entries
                }
            }
            i++;
        }
    } // end processRosterList()

    /* ***********************************  *********************************** */

    //parse consist list into appropriate mainapp hashmap
    //RCD}|{88(S)}|{Consist Name]\[2591(L)}|{true]\[3(S)}|{true]\[4805(L)}|{true
    public static void processConsistList(String responseStr) {
        String consist_addr = null;
        StringBuilder consist_desc = new StringBuilder();
        String consist_name = "";
        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        String plus = ""; //plus sign for a separator
        //initialize app arrays (skipping first)
        int i = 0;
        for (String ts : ta) {
            String[] tv = threaded_application.splitByString(ts, "}|{");  //split header chunk into header, address and name
            if (i == 0) { //first chunk is a "header"
                consist_addr = tv[1];
                consist_name = tv[2];
            } else {  //list of locos in consist
                consist_desc.append(plus).append(tv[0]);
                plus = "+";
            }  //end if i==0
            i++;
        }  //end for
        Log.d(threaded_application.applicationName, activityName + ": processConsistList(): consist header, addr='" + consist_addr
                + "', name='" + consist_name + "', desc='" + consist_desc + "'");
        //don't add empty consists to list
        if (mainapp.consist_entries != null && consist_desc.length() > 0) {
            mainapp.consist_entries.put(consist_addr, consist_desc.toString());
        } else {
            Log.d(threaded_application.applicationName, activityName + ": processConsistList(): skipping empty consist '" + consist_name + "'");
        }
    } // end processConsistList()

    //clear out any stored consists
    static void clearConsistList() {
        if (mainapp.consist_entries != null) mainapp.consist_entries.clear();
    }

    /* ***********************************  *********************************** */

//    public static int findTurnoutPos(String systemName) {
//        int pos = -1;
//        for (String sn : mainapp.to_system_names) {
//            pos++;
//            if (sn != null && sn.equals(systemName)) {
//                break;
//            }
//        }
//        return pos;
//    }

    /* ***********************************  *********************************** */

    //parse turnout change to update mainapp lists
    //  PTA<NewState><SystemName>
    //  PTA2LT12
    public static void processTurnoutChange(String responseStr) {
        String newState = responseStr.substring(3, 4);
        String systemName = responseStr.substring(4);
        //save new turnout state for later lookups
        mainapp.putTurnoutState(systemName, newState);
    }  //end of processTurnoutChange

    //parse turnout list into appropriate app variable array
    //  PTL[<SystemName><UserName><State>repeat] where state 1=Unknown. 2=Closed, 4=Thrown
    //  PTL]\[LT12}|{my12}|{1
    public static void processTurnoutList(String responseStr) {

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

    public static void processTurnoutTitles(String responseStr) {
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
            mainapp.to_state_names.put("T", "Thrown");
            mainapp.to_state_names.put("C", "Closed");
        }
    }

    /* ***********************************  *********************************** */

    //parse route list into appropriate app variable array
    //  PRA<NewState><SystemName>
    //  PRA2LT12
    public static void processRouteChange(String responseStr) {
        String newState = responseStr.substring(3, 4);
        String systemName = responseStr.substring(4);
        int pos = -1;
        for (String sn : mainapp.routeSystemNames) {
            pos++;
            if (sn != null && sn.equals(systemName)) {
                break;
            }
        }
        if (pos >= 0 && pos <= mainapp.routeSystemNames.length) {  //if found
            if (!newState.equals(mainapp.routeStates[pos])) { //route state is changed
//                Log.d(threaded_application.applicationName, activityName + ": processRouteChange(" + responseStr + ") CHANGED");
                mainapp.routeStates[pos] = newState;
                mainapp.alertActivitiesWithBundle(message_type.ROUTE_LIST_CHANGED);
//            } else {
//                Log.dthreaded_application.applicationName, activityName + ": processRouteChange(" + responseStr + ") NOT CHANGED");
            }
        }
    }  //end of processRouteChange

    //parse route list into appropriate app variable array
    //  PRL[<SystemName><UserName><State>]repeat where state 1=Unknown,2=Active,4=Inactive,8=Inconsistent
    //  PRL]\[LT12}|{my12}|{1
    public static void processRouteList(String responseStr) {

        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        //initialize app arrays (skipping first)
        mainapp.routeSystemNames = new String[ta.length - 1];
        mainapp.rt_user_names = new String[ta.length - 1];
        mainapp.routeStates = new String[ta.length - 1];
        mainapp.routeDccexLabels = new String[ta.length - 1];
        mainapp.routeDccexStates = new int[ta.length - 1];
        int i = 0;
        for (String ts : ta) {
            if (i > 0) { //skip first chunk, just message id
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split these into 3 parts, key and value
                mainapp.routeSystemNames[i - 1] = tv[0];
                mainapp.rt_user_names[i - 1] = tv[1];
                mainapp.routeStates[i - 1] = tv[2];
                if (mainapp.isWiThrottleProtocol()) {
                    mainapp.routeDccexLabels[i - 1] = "";
                    mainapp.routeDccexStates[i - 1] = -1;
                } else {
                    mainapp.routeDccexLabels[i - 1] = tv[2].equals("4")
                            ? mainapp.getResources().getString(R.string.dccexRouteHandoff)    // automation
                            : mainapp.getResources().getString(R.string.dccexRouteSet); // assume "2" = Route
                    mainapp.routeDccexStates[i - 1] = 0;
                }
            }  //end if i>0
            i++;
        }  //end for
        mainapp.alertActivitiesWithBundle(message_type.ROUTE_LIST_CHANGED);
    }

    public static void processRouteTitles(String responseStr) { //e.g  PRT]\[Routes}|{Route]\[Active}|{2]\[Inactive}|{4]\[Unknown}|{0]\[Inconsistent}|{8     only used for wiThrottle

        //clear the global variable
        mainapp.routeStateNames = new HashMap<>();

        String[] ta = threaded_application.splitByString(responseStr, "]\\[");  //initial separation
        //initialize app arrays (skipping first)
        int i = 0;
        for (String ts : ta) {
            if (i > 1) { //skip first 2 chunks
                String[] tv = threaded_application.splitByString(ts, "}|{");  //split these into value and key
                mainapp.routeStateNames.put(tv[1], tv[0]);
            }  //end if i>0
            i++;
        }  //end for
    }

    /* ***********************************  *********************************** */

}

