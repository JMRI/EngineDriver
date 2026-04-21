package jmri.enginedriver.comms;

import static android.widget.Toast.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.Consist;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.type.alert_bundle_tag_type;
import jmri.enginedriver.type.dccex_emergency_stop_state_type;
import jmri.enginedriver.type.message_type;
import jmri.enginedriver.type.source_type;

public class ResponseProcessorDccex {
    static final String activityName = "ResponseProcessorDccex";

    static SharedPreferences prefs;
    static threaded_application mainapp;
    static comm_thread commThread;

    static final String[] TRACK_TYPES = {"NONE", "MAIN", "MAIN_INV", "PROG", "DC", "DCX", "AUTO", "EXT", "PROG"};
    static final boolean[] TRACK_TYPES_NEED_ID = {false, false, false, false, true, true, false, false, false};

    public static void initialise(threaded_application mainapp, SharedPreferences prefs, comm_thread commThread) {
        ResponseProcessorDccex.prefs = prefs;
        ResponseProcessorDccex.mainapp = mainapp;
        ResponseProcessorDccex.commThread = commThread;
    }

    static boolean processWifiResponse(String responseStr) {
        boolean skipDefaultAlertToAllActivities = false;

        if (responseStr.length() >= 3) {
            if (!(responseStr.charAt(0) == '<')) {
                if (responseStr.contains("<")) { // see if we can clean it up
                    responseStr = responseStr.substring(responseStr.indexOf("<"));
                }
            }

            if (responseStr.charAt(0) == '<') {
                if ((threaded_application.dccexScreenIsOpen) && (responseStr.charAt(1)!='#') ) {
                    Bundle bundle = new Bundle();
                    bundle.putString(alert_bundle_tag_type.COMMAND, responseStr);
                    mainapp.alertActivitiesWithBundle(message_type.RECEIVED_DCCEX_RESPONSE, bundle, activity_id_type.DCC_EX);
                }

                // remove any escaped double quotes
                String s = responseStr.replace("\\\"", "'");
                //split on spaces, with stuff between double quotes treated as one item
                String[] args = s.substring(1, responseStr.length() - 1).split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 999);

                switch (responseStr.charAt(1)) {

                    // DCC_EX protocol only
                    case 'i': // Command Station Information
                        // --- <iDCCEX version / microprocessorType / MotorControllerType / buildNumber>

                        processDccexCommandStationInfoResponse(args, responseStr);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // DCC_EX protocol only
                    case '-': // forget loco - not normally used
                        // --- <- [cab]> - Remove one or all locos from reminders

                        forceDropDccexLoco(args);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // DCC_EX protocol only
                    case 'l': // loco speed/direction/function update
                        // --- <l cab reg speedByte functMap>

                        boolean [] verify = {false, true, true, true, true};
                        if (verifyParametersAreNumeric(args, verify)) processDccexLocos(args);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // DCC_EX protocol only
                    case 'r': // response from a decoder address read
                        // --- <r cab> or <r cab LOCOID> or <r cab CONSIST>

                        if (args.length<=2) { // response from a request for a loco id (the Drive away feature, and also the Address read)
                            processDccexRequestLocoIdResponse(args);
                        } else {
                            if ( (args[1].equals("LOCOID")) || (args[1].equals("CONSIST")) ) {
                                processDccexRequestLocoIdResponse(args); //second argument will be "LOCOID" or "CONSIST"
                            } else {
                                processDccexRequestCvResponse(args); // response from a CV read or write
                            }
                        }
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // DCC_EX protocol only
                    case 'p': // power response
                        // --- <pOnOFF [track]>

                        processDccexPowerResponse(args);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // DCC_EX protocol only
                    case 'j': //roster, turnouts / routes lists / automations
                        // --- <jR [id1 id2 id3 ...]>
                        // --- <jR id ""|"desc" ""|"funct1/funct2/funct3/...">
                        // --- <jT [id1 id2 id3 ...]>
                        // --- <jT id X|state |"[desc]">
                        // --- <jA id0 id1 id2 ..>
                        // --- <jA id X|type |"desc">
                        // --- <jB id state|"label">
                        // --- <jC minutes>

                        switch (responseStr.charAt(2)) {
                            case 'T': // turnouts
                                processDccexTurnouts(args);
                                skipDefaultAlertToAllActivities = true;
                                break;

                            case 'A': // automations/routes  <jA [id0 id1 id2 ...]> or <jA id X|type |"desc">
                                processDccexRoutes(args);
                                mainapp.alertActivitiesWithBundle(message_type.ROUTE_LIST_CHANGED);
                                skipDefaultAlertToAllActivities = true;
                                break;

                            case 'B': // automation/route update (Inactive, Active, Hidden, Caption)
                                processDccexRouteUpdate(args);
                                mainapp.alertActivitiesWithBundle(message_type.ROUTE_LIST_CHANGED);
                                skipDefaultAlertToAllActivities = true;
                                break;

                            case 'R': // roster
                                skipDefaultAlertToAllActivities = processDccexRoster(args);
                                break;

                            case 'C': // fastClock
                                processDccexFastClock(args);
                                skipDefaultAlertToAllActivities = true;
                        }
                        break;

                    // DCC_EX protocol only
                    case 'H': // Turnout/points change
                        // --- <H id state>

                        if (args.length==3) {
                            char action = args[2].charAt(0);
                            String turnoutId = args[1];
                            processDccexTurnoutChange(action, turnoutId);
                            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_TURNOUT_UPDATE);
                            skipDefaultAlertToAllActivities = true;
                        }
                        break;

                    // DCC_EX protocol only
                    case 'v': // response from a request a CV value
                        // --- <v cv value>

                        processDccexRequestCvResponse(args);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // DCC_EX protocol only
                    case 'w': { // response from an address write or other CV write
                        // --- <w address>

                        responseStr = args[1];
                        Bundle bundle = new Bundle();
                        bundle.putString(alert_bundle_tag_type.RESPONSE, responseStr);
                        if (!(args[1].charAt(0) == '-')) {
                            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_WRITE_DECODER_SUCCESS, bundle);
                        } else {
                            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_WRITE_DECODER_FAIL, bundle);
                        }
                        skipDefaultAlertToAllActivities = true;
                        break;
                    }

                    // DCC_EX protocol only
                    case '=': // Track Manager response
                        // --- <= trackLetter state cab>

                        processDccexTrackManagerResponse(args);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // DCC_EX protocol only
                    case 'm': // alert / info message sent from server to throttle

                        String message = args[1].substring(1,args[1].length()-1);
                        if (!comm_thread.acceptMessageOrAlert(message)) { skipDefaultAlertToAllActivities = true; break;}

                        mainapp.playTone(ToneGenerator.TONE_PROP_ACK);
                        mainapp.vibrate(new long[]{1000, 500, 1000, 500});
                        mainapp.safeToast(message, Toast.LENGTH_LONG); // copy to UI as toast message
                        break;

                    // DCC_EX protocol only
                    case '!': // estop
                        // --- <!>

                        processDccexEmergencyStopResponse(args);
                        skipDefaultAlertToAllActivities = true;
                        break;

                    // DCC_EX protocol only
                    case '^': // in-command-station consists
                        // --- <^>	Show all consists
                        // --- <^ loco1 loco2 ... >	Create Consist	loco: ID, negative for reversed loco
                        // --- <^ loco1> Deletes consist

                        processDccexInCommandStationConsistResponse(args);
                        skipDefaultAlertToAllActivities = true;
                        break;


                    // DCC_EX protocol only
                    case 'Q': // active sensor
                    case 'q': // inactive sensor
                    case 'X': { // inactive sensor
                        // --- `<Q id>
                        // --- `<q id>
                        // --- <X>

                        Bundle bundle = new Bundle();
                        bundle.putString(alert_bundle_tag_type.COMMAND, responseStr);
                        mainapp.alertActivitiesWithBundle(message_type.RESPONSE, bundle, activity_id_type.DCC_EX);
                        skipDefaultAlertToAllActivities = true;
                        break;
                    }

                    // DCC_EX protocol only
                    case '#': // number of supported cabs
                        // --- <# noCabs> this is just used as a heartbeat, so a heartbeat,

                        skipDefaultAlertToAllActivities = true;
                        break;

                    default: {
                        // if it was a valid DCC-EX command, and we have not picked it up above,
                        // then only send it to the dcc-ex activity
                        Bundle bundle = new Bundle();
                        bundle.putString(alert_bundle_tag_type.COMMAND, responseStr);
                        mainapp.alertActivitiesWithBundle(message_type.RESPONSE, bundle, activity_id_type.DCC_EX);
                        skipDefaultAlertToAllActivities = true;

                        Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): Unable to process valid DCC-EX command: " + responseStr);
                    }
                }

            } else { // ignore responses that don't start with "<"
                skipDefaultAlertToAllActivities = true;
            }
        } else { // ignore responses that are too short to be valid
            skipDefaultAlertToAllActivities = true;
        }
        return skipDefaultAlertToAllActivities;
    }

    @SuppressLint("DefaultLocale")
    public static void processDccexCommandStationInfoResponse(String [] args, String responseStr) { // <iDCCEX version / microprocessorType / MotorControllerType / buildNumber>
        String old_vn = threaded_application.getDccexVersionString();
        String [] vn1 = args[1].split("-");
        String [] vn2 = vn1[1].split("\\.");
        String vn = "4.";
        try {
            vn = String.format("%02d.", Integer.parseInt(vn2[0]));
        } catch (Exception e) {
            Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): Invalid Version " + threaded_application.getDccexVersionString() + ", ignoring");
        }
        if (vn2.length>=2) {
            try { vn = vn +String.format("%03d",Integer.parseInt(vn2[1]));
            } catch (Exception ignored) {
                // try to pull a partial number
                String pn = "0";
                for (int j=0; j<vn2[1].length(); j++ ) {
                    if ( (vn2[1].charAt(j)>='0') && (vn2[1].charAt(j)<='9') ) {
                        pn = pn + vn2[1].charAt(j);
                    } else { break; }
                }
                vn = vn +String.format("%03d", Integer.parseInt(pn));
            }
        }
        if (vn2.length>=3) {
            try { vn = vn + String.format("%03d",Integer.parseInt(vn2[2]));
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
        threaded_application.dccexVersionString = vn;
        if (!threaded_application.getDccexVersionString().equals(old_vn)) { //only if changed
            mainapp.alertActivitiesWithBundle(message_type.CONNECTED, activity_id_type.CONNECTION);
        } else {
            Log.d(threaded_application.applicationName, activityName + ": processWifiResponse(): version already set to " + threaded_application.getDccexVersionString() + ", ignoring");
        }

        mainapp.withrottle_version = 4.0;  // fudge it
        String serverDesc = responseStr.substring(2, responseStr.length() - 1);
        mainapp.setServerType("DCC-EX");
        mainapp.setServerDescription(serverDesc); //store the description
        Pattern p = Pattern.compile(".*IoTT WiThServer.*");
        if (p.matcher(serverDesc).matches()) { //identify IoTT Server
            mainapp.setServerType("IoTT");
        }

        mainapp.heartbeatInterval = 20000; // force a heartbeat period
        comm_thread.heart.startHeartbeat(mainapp.heartbeatInterval);
//                            mainapp.power_state = "2"; // unknown
    }


    public static void processDccexInCommandStationConsistResponse( String [] args) { // <^ [loco]|[-loco] [loco]|[-loco] ...>
        if (mainapp.dccexInCommandStationConsists == null) {
            mainapp.dccexInCommandStationConsists = new ArrayList<>();
        }
        try {
            ArrayList<HashMap<String, String>> consistArrayList = new ArrayList<>();

            for (int i = 1; i < args.length - 1; i++) {
                if (args[i].isEmpty()) break;

                int locoId = Integer.parseInt(args[i]);
                boolean locoReversed = false;
                if (locoId <= 0) {
                    locoId = locoId * -1;
                    locoReversed = true;
                }
                String locoIdString = Integer.toString(locoId);

                if ( (i==1) && (!mainapp.dccexInCommandStationConsists.isEmpty()) ) { //see if the consist is already in the list and remove it
                    for (ArrayList<HashMap<String, String>> arrayList : mainapp.dccexInCommandStationConsists) {
                        boolean found = false;
                        for (HashMap<String, String> map : arrayList) {
                            // Check if the HashMap contains the key and if its value matches
                            if (map.containsKey("loco_id")) {
                                String mapLocoId = map.get("loco_id");
                                if ( (mapLocoId!=null) && mapLocoId.equals(locoIdString)) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) {
                            mainapp.dccexInCommandStationConsists.remove(arrayList);
                            break;
                        }
                    }
                }

                HashMap<String, String> locoHashMap = new HashMap<>();
                locoHashMap.put("loco_id", locoIdString);
                locoHashMap.put("loco_facing", locoReversed ? "1" : "0");

                consistArrayList.add(locoHashMap);

                if (!mainapp.doFinish) {
                    mainapp.alertActivitiesWithBundle(message_type.DCCEX_RECEIVED_CONSIST_ENTRY);
                }
            }
            if (!consistArrayList.isEmpty())
                mainapp.dccexInCommandStationConsists.add(consistArrayList);
        } catch (Exception ignore) {
        }
    }

    /* ***********************************  *********************************** */

    public static void processDccexEmergencyStopResponse ( String [] args) { // <p0|1[PAUSED|RESUME]>
        String cmd = args[0].substring(1);
        processDccexEmergencyStopResponse(cmd);
    }
    public static void processDccexEmergencyStopResponse ( String cmd) { // <p0|1[PAUSED|RESUME]>
        if (cmd.equals("PAUSED")) {
            if (mainapp.dccexEmergencyStopState != dccex_emergency_stop_state_type.PAUSED) {
                mainapp.dccexEmergencyStopState = dccex_emergency_stop_state_type.PAUSED;
                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_DCCEX_ESTOP_PAUSED);
                mainapp.playTone(ToneGenerator.TONE_PROP_ACK);
            } // if no change don't bother doing anything
        }  else if (cmd.equals("RESUMED")) {
            if (mainapp.dccexEmergencyStopState != dccex_emergency_stop_state_type.RESUMED) {
                mainapp.dccexEmergencyStopState = dccex_emergency_stop_state_type.RESUMED;
                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_DCCEX_ESTOP_RESUMED);
                mainapp.playTone(ToneGenerator.TONE_PROP_ACK);
            } // if no change don't bother doing anything
        }
    }

    /* ***********************************  *********************************** */

    public static  void processDccexPowerResponse ( String [] args) { // <p![A|B|C|D|E|F|G|H|MAIN|PROG|DC|DCX]>
        String oldState = mainapp.power_state;
        Bundle bundle = new Bundle();
        Bundle bundleGlobal = new Bundle();

//        String responseStr;
        if ( (args.length==1)   // <p0|1>
                || ((args.length==2) && (args[0].length()==1) && (args[1].charAt(0)<='2')) ) {  // <p 0|1>
            char power;
            if ( (args[0].length() == 1) && (args[1].charAt(0) <= '2') ) {  // <p 0|1 A...
                power = args[1].charAt(0);
            } else { // <p0|1 A...
                power = args[0].charAt(1);
            }

            mainapp.power_state = "" + power;
            if (!mainapp.power_state.equals(oldState)) {
                if (power != '2') {
                    for (int i = 0; i < mainapp.dccexTrackType.length; i++) {
                        mainapp.dccexTrackPower[i] = power - '0';
                        bundle.putInt(alert_bundle_tag_type.TRACK, i);
                        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_POWER_STATE_CHANGE, bundle);
                    }
                }
            }

        } else { // <p0|1 A|B|C|D|E|F|G|H|MAIN|PROG|DC|DCX>  or  <p 0|1 A|B|C|D|E|F|G|H|MAIN|PROG|DC|DCX>
            int trackOffset = 0;
            char power;
            if ( (args[0].length() == 1) && (args[1].charAt(0) <= '2') ) {  // <p 0|1 A...
                trackOffset = 1;
                power = args[1].charAt(0);
            } else { // <p0|1 A...
                power = args[0].charAt(1);
            }

            if (args[1].length()==1) {  // <p0|1 A|B|C|D|E|F|G|H|>
                int trackNo = args[1+trackOffset].charAt(0) - 'A';
                mainapp.dccexTrackPower[trackNo] = power - '0';
                bundle.putInt(alert_bundle_tag_type.TRACK, trackNo);
                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_POWER_STATE_CHANGE, bundle);

            } else { // <p0|1 MAIN|PROG|DC|DCX|MAIN A|>
                int trackType = 0;
                for (int i=0; i<TRACK_TYPES.length; i++) {
                    String trackTypeStr = args[1+trackOffset];
                    if ( (args.length>(2+trackOffset)) && (args[1+trackOffset].equals("MAIN"))
                            && (args[2+trackOffset].charAt(0)>='A') && (args[2+trackOffset].charAt(0)<='I') ) {
                        if (args[2+trackOffset].charAt(0) == 'A')
                            trackTypeStr = "AUTO";
                        else if(args[2+trackOffset].charAt(0) == 'I')
                            trackTypeStr = "MAIN_INV";
                        else
                            trackTypeStr = "MAIN";
                    }
                    if (trackTypeStr.equals(TRACK_TYPES[i])) {
                        trackType = i;
                        break;
                    }
                }
                for (int i=0; i<mainapp.dccexTrackType.length; i++) {
                    if (mainapp.dccexTrackType[i] == trackType) {
                        mainapp.dccexTrackPower[i] = power - '0';
                        bundle.putInt(alert_bundle_tag_type.TRACK, i);
                        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_POWER_STATE_CHANGE, bundle);
                    }
                }
            }

            boolean globalPowerOn = true;
            boolean globalPowerOff = true;
            for (int i=0; i<mainapp.dccexTrackType.length; i++) {
                if ( (mainapp.dccexTrackAvailable[i]) && (mainapp.dccexTrackType[i] != 0) ) {  // not "NONE"
                    if (mainapp.dccexTrackPower[i] == 1) {
                        globalPowerOff = false;
                    }
                    if (mainapp.dccexTrackPower[i] == 0) {
                        globalPowerOn = false;
                    }
                }
            }

            if (!globalPowerOn && !globalPowerOff) {
                mainapp.power_state = "2";
            } else {
                if (globalPowerOn) {
                    mainapp.power_state = "1";
                } else {
                    mainapp.power_state = "0";
                }
            }
            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_POWER_STATE_CHANGE, bundleGlobal);
        }
    }

    /* ***********************************  *********************************** */

    public static void processDccexRequestCvResponse (String [] args) {
        String cv = "";
        String cvValue = "-1";

        if (args.length==3) {
            cv = args[1];
            cvValue = args[2];
        }

        Bundle bundle = new Bundle();
        bundle.putString(alert_bundle_tag_type.CV, cv);
        bundle.putString(alert_bundle_tag_type.CV_VALUE, cvValue);
        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_CV, bundle, activity_id_type.DCC_EX);  //send response to running activities
    }

    /* ***********************************  *********************************** */

    public static void processDccexTrackManagerResponse(String [] args) {
        int trackNo;
        String type = args[2];
        if (type.charAt(type.length() - 1) == '+') {
            type = type.substring(0, type.length() - 1);
        }
        if ( (args.length>3) && (args[2].equals("MAIN")) && (args[3].charAt(0)>='A') && (args[3].charAt(0)<='I') ) {
            if (args[3].charAt(0) == 'A')
                type = "AUTO";
            else if(args[3].charAt(0) == 'I')
                type = "MAIN_INV";
            else
                type = "MAIN";
        }

//        if (args.length>=2) {
        trackNo = args[1].charAt(0)-65;
        if ( (trackNo>=0) && (trackNo<= threaded_application.DCCEX_MAX_TRACKS) ) {
            int trackTypeIndex = -1;
            boolean needsId = false;
            for (int i=0; i<TRACK_TYPES.length; i++) {
                if (type.equals(TRACK_TYPES[i])) {
                    trackTypeIndex = i;
                    needsId = TRACK_TYPES_NEED_ID[i];
                    break;
                }
            }

            if (trackTypeIndex>=0) {
                mainapp.dccexTrackType[trackNo] = trackTypeIndex;
                mainapp.dccexTrackId[trackNo] = "";
            }
            if ( (needsId) && (args.length>=3) ) {
                mainapp.dccexTrackId[trackNo] = args[3];
            }
            mainapp.dccexTrackAvailable[trackNo] = true;
        }
        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_DCCEX_TRACKS);
//        }
    }

    /* ***********************************  *********************************** */

    public static void forceDropDccexLoco(String [] args) {
        String addressStr = args[1];
        if (Integer.parseInt(args[1]) <= 127) {
            addressStr = "S" + addressStr;
        } else {
            addressStr = "L" + addressStr;
        }

        for (int throttleIndex = 0; throttleIndex<mainapp.maxThrottlesCurrentScreen; throttleIndex++) {   //loco may be the lead on more than one throttle
            int whichThrottle = mainapp.getWhichThrottleFromAddress(addressStr, throttleIndex);
            if (whichThrottle >= 0) {
                // release everything on the throttle
                mainapp.storeThrottleLocosForReleaseDCCEX(whichThrottle);
                mainapp.consists[whichThrottle].release();
//                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "", whichThrottle);

                Bundle bundle = new Bundle();
                bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                bundle.putString(alert_bundle_tag_type.LOCO_TEXT,"");
                mainapp.alertCommHandlerWithBundle(message_type.RELEASE, bundle);

                bundle = new Bundle();
                bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                mainapp.alertActivitiesWithBundle(message_type.FORCE_THROTTLE_RELOAD, 1000L, bundle);
            }
        }

    }

    /* ***********************************  *********************************** */

    public static void processDccexLocos(String [] args) {
        String responseStr;

        int dir = 0;
        int speed = 0;
        try {
            speed = Integer.parseInt(args[3]);
        } catch (NumberFormatException ignore) {}

        if (speed >= 128) {
            speed = speed - 128;
            dir = 1;
        }
        if (speed>1) {
            speed = speed - 1; // get around the idiotic design of the speed command
        } else {
            speed=0;
        }

        String addressStr = args[1];
        try {
            if (Integer.parseInt(args[1]) <= 127) {
                addressStr = "S" + addressStr;
            } else {
                addressStr = "L" + addressStr;
            }
        } catch (Exception e) {
            return;
        }
        long timeSinceLastCommand;

        for (int throttleIndex = 0; throttleIndex<mainapp.maxThrottlesCurrentScreen; throttleIndex++) {   //loco may be the lead on more than one throttle
            int whichThrottle = mainapp.getWhichThrottleFromAddress(addressStr, throttleIndex);
            if (whichThrottle >= 0) {
                timeSinceLastCommand = Calendar.getInstance().getTimeInMillis() - mainapp.dccexLastSpeedCommandSentTime[whichThrottle];

                Consist con = mainapp.consists[whichThrottle];

                if (timeSinceLastCommand>1000) {  // don't process an incoming speed or direction changes if we sent a command for this throttle in the last second

//                    threaded_application.extendedLogging(activityName + ": speedUpdateWiT(): speed, direction or function update");
                    mainapp.dccexLastKnownSpeed[whichThrottle] = speed;
                    Bundle bundle = new Bundle();
                    bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                    bundle.putInt(alert_bundle_tag_type.SPEED, speed);
                    mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_SET_SPEED, bundle, activity_id_type.THROTTLE);

                    // only process the direction if it is the lead loco
                    if (con.getLeadAddr().equals(addressStr)) {
                        mainapp.dccexLastKnownDirection[whichThrottle] = dir;
                        bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                        bundle.putInt(alert_bundle_tag_type.DIRECTION, dir);
                        bundle.putString(alert_bundle_tag_type.LOCO, addressStr);
                        mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_SET_DIRECTION, bundle, activity_id_type.THROTTLE);
                    }

                } else {
                    threaded_application.extendedLogging(activityName + ": speedUpdateWiT(): :<>: extra DCC-EX pacing delay. speed update ignored");
                }

                    // only process the functions if it is the lead loco
                    if (con.getLeadAddr().equals(addressStr)) {
                        threaded_application.extendedLogging(activityName + ": processDccexLocos(): process functions" );
                        // Process the functions
                        int fnState;
                        for (int i = 0; i < threaded_application.MAX_FUNCTIONS; i++) {
                            try {
//                                fnState = mainapp.bitExtracted(Integer.parseInt(args[4]), 1, i + 1);
                                fnState = mainapp.bitExtracted(Long.parseLong(args[4]), 1, i + 1);
                                if (i==0) Log.d(threaded_application.applicationName, activityName + ": processDccexLocos(): function:" + i + " state: " + fnState);
                                comm_thread.processFunctionState(whichThrottle, i, (fnState != 0));
                                Bundle bundle = new Bundle();
                                bundle.putInt(alert_bundle_tag_type.THROTTLE, whichThrottle);
                                bundle.putString(alert_bundle_tag_type.LOCO, addressStr);
                                bundle.putInt(alert_bundle_tag_type.FUNCTION, i);
                                bundle.putInt(alert_bundle_tag_type.FUNCTION_ACTION, fnState);
                                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_THROTTLE_SET_FUNCTION, bundle, activity_id_type.THROTTLE);

                            } catch (NumberFormatException e) {
                                Log.w("Engine_Driver", "unable to parseInt: '" + e.getMessage() + "'");
                            }
                        }
                    }

                    //noinspection AssignmentToForLoopParameter
                throttleIndex = whichThrottle; // skip ahead
            }
        }
    } // end processDccexLocos()

    /* ***********************************  *********************************** */

    public static void processDccexRequestLocoIdResponse(String [] args) {
//        String responseStr = "";

        if (comm_thread.requestLocoIdForWhichThrottleDCCEX!=-1) { // if -1, request came from the CV read/write screen
            if (!(args[1].charAt(0) =='-')) {

                if (args.length<=2) {

                    String addrStr = args[1];
                    if (Integer.parseInt(args[1]) <= 127) {
                        addrStr = "S" + addrStr;
                    } else {
                        addrStr = "L" + addrStr;
                    }
                    Consist con = mainapp.consists[comm_thread.requestLocoIdForWhichThrottleDCCEX];
                    if (con.isWaitingOnID()) { //we were waiting for this response to get address
                        Consist.ConLoco conLoco = new Consist.ConLoco(addrStr);
                        conLoco.setFunctionLabelDefaults(mainapp, comm_thread.requestLocoIdForWhichThrottleDCCEX);
                        //look for RosterEntry which matches address returned
                        String rn = mainapp.getRosterNameFromAddress(conLoco.getFormatAddress(), true);
                        if (!rn.isEmpty()) {
                            conLoco.setIsFromRoster(true);
                            conLoco.setRosterName(rn);
                        }
                        con.add(conLoco);
                        con.setWhichSource(addrStr, 1); //entered by address, not roster
                        con.setConfirmed(addrStr);
                        mainapp.addLocoToRecents(conLoco); //DCC-EX

                        comm_thread.sendAcquireLoco(addrStr, rn, comm_thread.requestLocoIdForWhichThrottleDCCEX);
                        SendProcessorDccex.sendDccexJoinTracks(true);
                        mainapp.alertActivitiesWithBundle(message_type.REQUEST_REFRESH_THROTTLE, activity_id_type.THROTTLE);
                    }

                } else { // the second argument should be "LOCOID" or "CONSIST", which are a special type of loco id request only used on the CV writing page
                    if (!(args[2].charAt(0) =='-')) {
                        Bundle bundle = new Bundle();
                        if (args[1].equals("LOCOID")) {
                            bundle.putString(alert_bundle_tag_type.DECODER_ADDRESS, args[2]);
                            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_DECODER_ADDRESS, bundle, activity_id_type.DCC_EX);
                        } else if (args[1].equals("CONSIST")) {
                            bundle.putString(alert_bundle_tag_type.CONSIST_ADDRESS, args[2]);
                            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_CONSIST_ADDRESS, bundle, activity_id_type.DCC_EX);
                        }
                    }
                }

            }  else {// else {} did not succeed
                mainapp.safeToast(R.string.dccexRequestLocoIdFailed, LENGTH_SHORT);
            }

        } else {
            Bundle bundle = new Bundle();
            bundle.putString(alert_bundle_tag_type.DECODER_ADDRESS, args[1]);
            mainapp.alertActivitiesWithBundle(message_type.RECEIVED_DECODER_ADDRESS, bundle);
        }

    }

    /* ***********************************  *********************************** */

    public static void processDccexRosterList() {
        //clear the global variable
        mainapp.roster_entries = Collections.synchronizedMap(new LinkedHashMap<String, String>());
        //todo   RDB why don't we just clear the existing map with roster_entries.clear() instead of disposing and creating a new instance?

        int rosterCount = mainapp.dccexRosterIDs.length;
        //initialize app arrays (skipping first)
        for (int i = 0; i < rosterCount; i++) {
            try {
                mainapp.roster_entries.put(mainapp.dccexRosterLocoNames[i], mainapp.dccexRosterIDs[i] + "(" + (mainapp.dccexRosterIDs[i] <= 127 ? "S" : "L") + ")"); //roster name is hashmap key, value is address(L or S), e.g.  2591(L)
            } catch (Exception e) {
                Log.d(threaded_application.applicationName, activityName + ": processRosterList(): caught Exception");  //ignore any bad stuff in roster entries
            }
        }

        mainapp.dccexRosterProcessed = true;
    } // end processDccexRosterList()

    public static boolean processDccexRoster(String [] args) {
        boolean skipDefaultAlertToAllActivities = true;

        if (args != null) {
            if (args.length == 0) { // empty roster
                if (prefs.getBoolean("prefDccexSequenceItemRequests", false))
                    SendProcessorDccex.sendDccexRequestTurnouts();
            } else {
                if ((args.length < 3) || (args[2].charAt(0) != '"')) {  // loco list
//                    if (mainapp.dccexRosterString.isEmpty()) {
//                        mainapp.dccexRosterString = "";
                    if (!mainapp.dccexRosterProcessed) {
                        mainapp.dccexRosterIDs = new int[args.length - 1];
                        mainapp.dccexRosterLocoNames = new String[args.length - 1];
                        mainapp.dccexRosterLocoFunctions = new String[args.length - 1];
                        mainapp.dccexRosterDetailsReceived = new boolean[args.length - 1];
                        for (int i = 0; i < args.length - 1; i++) { // first will be blank
                            try {
                                mainapp.dccexRosterIDs[i] = Integer.parseInt(args[i + 1]);
                                mainapp.dccexRosterDetailsReceived[i] = false;
                                comm_thread.wifiSend("<JR " + args[i + 1] + ">");
                            } catch (Exception e) {
                                mainapp.safeToast(mainapp.getApplicationContext().getResources().getString(R.string.toastDccexInvalidRosterId, args[i + 1]), Toast.LENGTH_LONG);
                            }
                        }
                    }
                } else {  // individual loco
                    if (mainapp.DCCEXlistsRequested < 3) {
                        if (mainapp.dccexRosterIDs != null) {
                            for (int i = 0; i < mainapp.dccexRosterIDs.length; i++) {
                                if (mainapp.dccexRosterIDs[i] == Integer.parseInt(args[1])) {
                                    mainapp.dccexRosterLocoNames[i] = args[2].substring(1, args[2].length() - 1);  // stip the quotes
                                    mainapp.dccexRosterLocoFunctions[i] = args[3]; // ignore this
                                    mainapp.dccexRosterDetailsReceived[i] = true;
                                    break;
                                }
                            }

                            // check if we have all of them
                            boolean ready = true;
                            for (int i = 0; i < mainapp.dccexRosterIDs.length; i++) {
                                if (!mainapp.dccexRosterDetailsReceived[i]) {
                                    ready = false;
                                    break;
                                }
                            }
                            if (ready) {
//                                mainapp.dccexRosterString = getRosterString();
//                                processRosterList(mainapp.dccexRosterString);
                                processDccexRosterList();

//                                mainapp.dccexRosterString = "";
                                mainapp.DCCEXlistsRequested++;
                                mainapp.alertActivitiesWithBundle(message_type.RECEIVED_ROSTER_UPDATE);

                                Log.d(threaded_application.applicationName, activityName + ": processDccexRoster: Roster complete. Count: " + mainapp.dccexRosterIDs.length);

//                            mainapp.dccexRosterFullyReceived = true;
                                int count = (mainapp.dccexRosterIDs == null) ? 0 : mainapp.dccexRosterIDs.length;
                                if (count > 0 ) {
//                                    mainapp.safeToastInstructional(R.string.roster_available, LENGTH_SHORT);

                                    Bundle bundle = new Bundle();
                                    bundle.putString(alert_bundle_tag_type.MESSAGE, mainapp.getApplicationContext().getResources().getString(R.string.roster_available));
                                    bundle.putInt(alert_bundle_tag_type.DURATION, LENGTH_SHORT);
                                    bundle.putBoolean(alert_bundle_tag_type.INSTRUCTIONAL, true);
                                    mainapp.alertActivitiesWithBundle(message_type.CUSTOM_TOAST_MESSAGE, bundle, activity_id_type.THROTTLE);
                                }

                                if (prefs.getBoolean("prefDccexSequenceItemRequests", false))
                                    SendProcessorDccex.sendDccexRequestTurnouts();
                            }
                        }

                    } else { // this a request for details on a specific loco - not part of the main roster request
                        // <jR id ""|"desc" ""|"funct1/funct2/funct3/...">

                        String addr_str = args[1];
                        addr_str = ((Integer.parseInt(args[1]) <= 127) ? "S" : "L") + addr_str;

                        boolean found = false;
                        for (int throttleIndex = 0; throttleIndex < mainapp.maxThrottlesCurrentScreen; throttleIndex++) {  //loco may be the lead on more than one throttle
                            StringBuilder responseStrBuilder = new StringBuilder();

                            int whichThrottle = mainapp.getWhichThrottleFromAddress(addr_str, throttleIndex);
                            if (whichThrottle >= 0) {

                                found = true;
                                String lead = mainapp.consists[whichThrottle].getLeadAddr();
                                if (lead.equals(addr_str)) { // only process the functions for lead engine in consist
                                    if ((mainapp.consists[whichThrottle].isLeadFromRoster()) || (mainapp.prefAlwaysUseFunctionsFromServer)) { // only process the functions if the lead engine from the roster or the override preference is set

                                        if (args[3].length() > 2) {
                                            Log.d(threaded_application.applicationName, activityName + ": processDccexRoster: Processing Functions for lead loco");

                                            if ( (args.length != 4) || (!args[2].equals("\"\"")) ) {
                                                String[] fnArgs = args[3].substring(1, args[3].length() - 1).split("/", 999);
                                                mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle] = new boolean[args[3].length()];
                                                responseStrBuilder.append("RF29}|{1234(L)]\\[");  //prepend some stuff to match old-style
                                                for (int i = 0; i < fnArgs.length; i++) {
                                                    if (fnArgs[i].isEmpty()) {
                                                        mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle][i] = false;
                                                    } else {
                                                        if (fnArgs[i].charAt(0) == '*') { // is NOT latching
                                                            responseStrBuilder.append(fnArgs[i].substring(1));
                                                            mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle][i] = false;
                                                        } else {
                                                            responseStrBuilder.append(fnArgs[i]);
                                                            mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle][i] = true;
                                                        }
                                                    }
                                                    if (i < fnArgs.length - 1) {
                                                        responseStrBuilder.append("]\\[");
                                                    }
                                                }
                                            }

                                            comm_thread.processRosterFunctionString(responseStrBuilder.toString(), whichThrottle);
                                            mainapp.consists[whichThrottle].setFunctionLabels(addr_str, responseStrBuilder.toString());
                                        } else {
                                            Log.d(threaded_application.applicationName, activityName + ": processDccexRoster: Problem Processing Functions for lead loco");
                                            mainapp.throttleFunctionIsLatchingDCCEX[whichThrottle] = null;
                                            skipDefaultAlertToAllActivities = false;
                                        }
                                    } else {
                                        Log.d(threaded_application.applicationName, activityName + ": processDccexRoster: Processing Functions -  lead loco is not from the roster");
                                    }
                                } else {
                                    Log.d(threaded_application.applicationName, activityName + ": processDccexRoster: Processing Functions -  not the lead loco - ignoring");

                                }

                                Consist con = mainapp.consists[whichThrottle];
                                if (con.getLoco(addr_str) != null) { //loco was added to consist in select_loco
                                    con.setConfirmed(addr_str);
                                    con.setWhichSource(addr_str, source_type.ROSTER); //entered by address, not roster
//                                    con.setFunctionLabels(addr_str, threaded_application.parseFunctionLabels(responseStrBuilder.toString()));
                                    con.setFunctionLabels(addr_str, threaded_application.parseFunctionLabels(responseStrBuilder.toString()));

                                    mainapp.addLocoToRecents(con.getLoco(addr_str), threaded_application.parseFunctionLabels(responseStrBuilder.toString()));  //DCC-EX
                                }

                                //noinspection AssignmentToForLoopParameter
                                throttleIndex = whichThrottle; // skip ahead
                            }
                        }

                        if (!found) {  // we got the request, but it is not on a throttle
                            if (args[3].length() > 2) {
                                String[] fnArgs = args[3].substring(1, args[3].length() - 1).split("/", 999);
//                                String responseStr = getResponseStr(fnArgs);

                                // save them in recents regardless
                                Consist.ConLoco conLoco = new Consist.ConLoco(addr_str);
                                conLoco.setIsFromRoster(true);
                                conLoco.setRosterName(args[2].substring(1, args[2].length() - 1)); // strip the quotes
//                                mainapp.addLocoToRecents(conLoco, threaded_application.parseFunctionLabels(responseStr)); //DCC-EX
                                mainapp.addLocoToRecents(conLoco, threaded_application.parseFunctionLabels(fnArgs)); //DCC-EX
                            }
                        }
                    }
                }
            }
        }
        return skipDefaultAlertToAllActivities;
    } // end processDccexRoster()

    /* ***********************************  *********************************** */

    public static void processDccexFastClock(String [] args) {
        if (args!=null)  {
            if (args.length == 3) { // <jC mmmm ss>
                mainapp.fastClockSeconds = 0L;
                try {
                    mainapp.fastClockSeconds = Long.parseLong(args[1]) * 60;
                    mainapp.alertActivitiesWithBundle(message_type.RECEIVED_TIME_CHANGE);
                } catch (NumberFormatException e) {
                    Log.w("Engine_Driver", "unable to extract fastClockSeconds from '" + Arrays.toString(args) + "'");
                }
            }
        }
    }

    /* ***********************************  *********************************** */

    public static void processDccexTurnoutTitles() {
        //clear the global variable
        mainapp.to_state_names = new HashMap<>();

        String throwCode = "4";
        String closeCode = "2";
        if (prefs.getBoolean("prefDccexSwapThrowClose",
                mainapp.getResources().getBoolean(R.bool.prefDccexSwapThrowCloseDefaultValue))) {
            throwCode = "2";
            closeCode ="4";
        }
        String prefDccexThrownLabel = prefs.getString("prefDccexThrownLabel", "").trim();
        String prefDccexClosedLabel = prefs.getString("prefDccexClosedLabel", "").trim();

        if (prefDccexClosedLabel.isEmpty()) {
            mainapp.to_state_names.put(closeCode, mainapp.getResources().getString(R.string.dccexTurnoutClosed));
        } else {
            mainapp.to_state_names.put(closeCode, prefDccexClosedLabel);
        }
        if (prefDccexThrownLabel.isEmpty()) {
            mainapp.to_state_names.put(throwCode, mainapp.getResources().getString(R.string.dccexTurnoutThrown));
        } else {
            mainapp.to_state_names.put(throwCode, prefDccexThrownLabel);
        }
        mainapp.to_state_names.put("1", mainapp.getResources().getString(R.string.dccexTurnoutUnknown));
        mainapp.to_state_names.put("8", mainapp.getResources().getString(R.string.dccexTurnoutInconsistent));
    }

    public static void processDccexTurnoutList(boolean noTurnouts) {

        int turnoutCount = 0;
        if (mainapp.dccexTurnoutIDs != null)
            turnoutCount = mainapp.dccexTurnoutIDs.length;

        if ( (!noTurnouts) && (turnoutCount>0) ) {
            //initialize app arrays
            mainapp.to_system_names = new String[turnoutCount];
            mainapp.to_user_names = new String[turnoutCount];

            for (int i = 0; i < turnoutCount; i++) {
                mainapp.to_system_names[i] = "" + mainapp.dccexTurnoutIDs[i];
                mainapp.to_user_names[i] = mainapp.dccexTurnoutNames[i];
                mainapp.putTurnoutState(""+mainapp.dccexTurnoutIDs[i], (mainapp.dccexTurnoutStates[i].equals("T") ? "4" : "2"));
            }
        }
        mainapp.dccexTurnoutsProcessed = true;
    }

    public static void processDccexTurnoutChange(char action, String turnoutId) {
        String newState = ((action=='T') || (action == '1')) ? "4" : "2";
        mainapp.putTurnoutState(turnoutId, newState);
    }

    public static void processDccexTurnouts(String [] args) {

        if (args!=null)  {
            if ( (args.length == 1)  // no Turnouts <jT>
                    || ((args.length == 3) && ((args[2].charAt(0) == 'C') || (args[2].charAt(0) == 'T') || (args[2].charAt(0) == 'X')) ) // <jT id state>     or <jT id X>
                    || ((args.length == 4) && (args[3].charAt(0) == '"') ) ) { // individual turnout  <jT id state "[desc]">
                boolean ready = true;
                boolean noTurnouts = false;

                if ( args.length == 1) { // no turnouts
                    noTurnouts = true;
                    if (prefs.getBoolean("prefDccexSequenceItemRequests",false))
                        SendProcessorDccex.sendDccexRequestRoutes();
                } else {
                    for (int i = 0; i < mainapp.dccexTurnoutIDs.length; i++) {
                        if (mainapp.dccexTurnoutIDs[i] == Integer.parseInt(args[1])) {
                            mainapp.dccexTurnoutStates[i] = args[2];
                            if ((args.length > 3) && (args[3].length() > 2)) {
                                mainapp.dccexTurnoutNames[i] = args[3].substring(1, args[3].length() - 1);
                            } else {
                                mainapp.dccexTurnoutNames[i] = "";
                            }
                            mainapp.dccexTurnoutDetailsReceived[i] = true;
                            break;
                        }
                    }
                    // check if we have all of them
                    for (int i = 0; i < mainapp.dccexTurnoutIDs.length; i++) {
                        if (!mainapp.dccexTurnoutDetailsReceived[i]) {
                            ready = false;
                            break;
                        }
                    }
                }
                if ((ready) && (!mainapp.dccexTurnoutsFullyReceived) ) {

                    processDccexTurnoutTitles();
                    processDccexTurnoutList(noTurnouts);

                    mainapp.alertActivitiesWithBundle(message_type.REQUEST_REFRESH_THROTTLE, activity_id_type.THROTTLE);
//                    mainapp.dccexTurnoutString = "";
                    mainapp.DCCEXlistsRequested++;

                    int count = (mainapp.dccexTurnoutIDs == null) ? 0 : mainapp.dccexTurnoutIDs.length;
                    Log.d(threaded_application.applicationName, activityName + ": processDccexTurnouts(): Turnouts complete. Count: " + count);
                    mainapp.dccexTurnoutsBeingProcessed = false;

                    mainapp.dccexTurnoutsFullyReceived = true;
                    if (count > 0) {
//                        mainapp.safeToastInstructional(R.string.turnouts_available, LENGTH_SHORT);

                        Bundle bundle = new Bundle();
                        bundle.putString(alert_bundle_tag_type.MESSAGE, mainapp.getApplicationContext().getResources().getString(R.string.turnouts_available));
                        bundle.putInt(alert_bundle_tag_type.DURATION, LENGTH_SHORT);
                        bundle.putBoolean(alert_bundle_tag_type.INSTRUCTIONAL, true);
                        mainapp.alertActivitiesWithBundle(message_type.CUSTOM_TOAST_MESSAGE, bundle, activity_id_type.THROTTLE);
                    }
                    if (prefs.getBoolean("prefDccexSequenceItemRequests",false))
                        SendProcessorDccex.sendDccexRequestRoutes();
                }

            } else { // turnouts list  <jT id1 id2 id3 ...>

                Log.d(threaded_application.applicationName, activityName + ": processDccexTurnouts(): Turnouts list received.");
                if (!mainapp.dccexTurnoutsBeingProcessed) {
                    mainapp.dccexTurnoutsBeingProcessed = true;
//                    if (mainapp.dccexTurnoutString.isEmpty()) {
//                        mainapp.dccexTurnoutString = "";
                    if (!mainapp.dccexTurnoutsProcessed) {
                        mainapp.dccexTurnoutIDs = new int[args.length - 1];
                        mainapp.dccexTurnoutNames = new String[args.length - 1];
                        mainapp.dccexTurnoutStates = new String[args.length - 1];
                        mainapp.dccexTurnoutDetailsReceived = new boolean[args.length - 1];
                        for (int i = 0; i < args.length - 1; i++) { // first will be blank
                            mainapp.dccexTurnoutIDs[i] = Integer.parseInt(args[i + 1]);
                            mainapp.dccexTurnoutDetailsReceived[i] = false;
                            comm_thread.wifiSend("<JT " + args[i + 1] + ">");
                        }

                        int count = (mainapp.dccexTurnoutIDs == null) ? 0 : mainapp.dccexTurnoutIDs.length;
                        Log.d(threaded_application.applicationName, activityName + ": processDccexTurnouts(): Turnouts list received. Count: " + count);
                    }
                }
            }
        }
    } // end processDccexTurnouts()

    /* ***********************************  *********************************** */

    public static void processDccexRouteTitles() {
        //clear the global variable
        mainapp.routeStateNames = new HashMap<>();

        // these are fixed to the localised values
        mainapp.routeStateNames.put(mainapp.getResources().getString(R.string.dccexRouteSet), "2");
        mainapp.routeStateNames.put(mainapp.getResources().getString(R.string.dccexRouteHandoff), "4");
    }

    public static void processDccexRouteList() {

        int routeCount = 0;
        if (mainapp.dccexRouteIDs != null)
            routeCount = mainapp.dccexRouteIDs.length;

        if (routeCount>0) {
            //initialize app arrays (skipping first)
            mainapp.routeSystemNames = new String[routeCount];
            mainapp.rt_user_names = new String[routeCount];
            mainapp.routeStates = new String[routeCount];
            mainapp.routeDccexLabels = new String[routeCount];
            mainapp.routeDccexStates = new int[routeCount];

            for (int i = 0; i < routeCount; i++) {
                mainapp.routeSystemNames[i] = "" + mainapp.dccexRouteIDs[i];
                mainapp.rt_user_names[i] = mainapp.dccexRouteNames[i];
                mainapp.routeStates[i] = mainapp.dccexRouteTypes[i].equals("R") ? "2" : "4";
                mainapp.routeDccexLabels[i] = mainapp.dccexRouteTypes[i].equals("4")
                        ? mainapp.getResources().getString(R.string.dccexRouteHandoff)    // automation
                        : mainapp.getResources().getString(R.string.dccexRouteSet); // assume "2" = Route
                mainapp.routeDccexStates[i] = 0;
            }  //end for

            mainapp.alertActivitiesWithBundle(message_type.ROUTE_LIST_CHANGED);

        } else {
            mainapp.routeStateNames = null;
        }
        mainapp.dccexRoutesListReceived = true;
    }

    public static void processDccexRoutes(String [] args) {
        if (args != null)  {
            if ( (args.length == 1)  // no Routes <jA>
                    || ((args.length == 3) && ((args[2].charAt(0) == 'R') || (args[2].charAt(0) == 'A') || (args[2].charAt(0) == 'X')) )  // <jA id type>  or <jA id X>
                    || ((args.length == 4) && (args[3].charAt(0) == '"') ) ) { // individual routes  <jA id type "[desc]">

                boolean ready = true;
                boolean noRoutes = false;

                if ( args.length == 1) { // no Routes
                    noRoutes = true;
                    if (prefs.getBoolean("prefDccexSequenceItemRequests",false))
                        SendProcessorDccex.sendDccexRequestTracks();
                } else {
                    for (int i = 0; i < mainapp.dccexRouteIDs.length; i++) {
                        if (mainapp.dccexRouteIDs[i] == Integer.parseInt(args[1])) {
                            mainapp.dccexRouteTypes[i] = args[2];
                            mainapp.dccexRouteNames[i] = args[3].substring(1, args[3].length() - 1);
                            mainapp.dccexRouteDetailsReceived[i] = true;
                            break;
                        }
                    }
                    // check if we have all of them

                    for (int i = 0; i < mainapp.dccexRouteIDs.length; i++) {
                        if (!mainapp.dccexRouteDetailsReceived[i]) {
                            ready = false;
                            Log.d(threaded_application.applicationName, activityName + ": processDccexRoutes(): Routes incomplete. Missing: " + mainapp.dccexRouteIDs[i]);
                            break;
                        }
                    }
                }
                if (ready) {

                    processDccexRouteTitles();

//                    mainapp.dccexRouteString = getRoutesString(noRoutes);
//                    processRouteList(mainapp.dccexRouteString);
                    processDccexRouteList();

                    mainapp.alertActivitiesWithBundle(message_type.REQUEST_REFRESH_THROTTLE, activity_id_type.THROTTLE);
//                    mainapp.dccexRouteString = "";
                    mainapp.dccexRoutesListReceived = false;
                    mainapp.DCCEXlistsRequested++;

                    if (mainapp.dccexRouteStatesReceived) { // we received some DCC-EX route states before the list was complete
                        for (int i=0; i<mainapp.dccexRouteIDs.length;i++) {
                            if (mainapp.dccexRouteStates[i]!=null) {
                                mainapp.routeDccexStates[i] = Integer.parseInt(mainapp.dccexRouteStates[i]);
                            } else {
                                mainapp.routeDccexStates[i] = 0;
                            }
                            if (mainapp.dccexRouteLabels[i]!=null) {
                                mainapp.routeDccexLabels[i] = mainapp.dccexRouteLabels[i];
                            } else {
                                mainapp.routeDccexLabels[i] =  mainapp.dccexRouteTypes[i].equals("R")
                                        ? mainapp.getResources().getString(R.string.dccexRouteSet)
                                        : mainapp.getResources().getString(R.string.dccexRouteHandoff);
                            }
                        }
                    }

                    int count = (mainapp.dccexRouteIDs==null) ? 0 : mainapp.dccexRouteIDs.length;
                    Log.d(threaded_application.applicationName, activityName + ": processDccexRoutes(): Routes complete. Count: " + count);
                    mainapp.dccexRoutesBeingProcessed = false;

//                    mainapp.dccexRoutesFullyReceived = true;
                    if (count > 0) {
//                        mainapp.safeToastInstructional(R.string.routes_available, LENGTH_SHORT);

                        Bundle bundle = new Bundle();
                        bundle.putString(alert_bundle_tag_type.MESSAGE, mainapp.getApplicationContext().getResources().getString(R.string.routes_available));
                        bundle.putInt(alert_bundle_tag_type.DURATION, LENGTH_SHORT);
                        bundle.putBoolean(alert_bundle_tag_type.INSTRUCTIONAL, true);
                        mainapp.alertActivitiesWithBundle(message_type.CUSTOM_TOAST_MESSAGE, bundle, activity_id_type.THROTTLE);
                    }
                    if (prefs.getBoolean("prefDccexSequenceItemRequests",false))
                        SendProcessorDccex.sendDccexRequestTracks();
                }

            } else { // routes list   <jA id1 id2 id3 ...>   or <jA> for empty

                Log.d(threaded_application.applicationName, activityName + ": processDccexRoutes(): Routes list received.");
                if (!mainapp.dccexRoutesBeingProcessed) {
                    mainapp.dccexRoutesBeingProcessed = true;
//                    if (mainapp.dccexRouteString.isEmpty()) {
                    if (!mainapp.dccexRoutesListReceived) {
//                        mainapp.dccexRouteString = "";
                        mainapp.dccexRouteIDs = new int[args.length - 1];
                        mainapp.dccexRouteNames = new String[args.length - 1];
                        mainapp.dccexRouteTypes = new String[args.length - 1];
                        mainapp.dccexRouteStates = new String[args.length - 1];
                        mainapp.dccexRouteLabels = new String[args.length - 1];
                        mainapp.dccexRouteDetailsReceived = new boolean[args.length - 1];
                        for (int i = 0; i < args.length - 1; i++) { // first will be blank
                            mainapp.dccexRouteIDs[i] = Integer.parseInt(args[i + 1]);
                            mainapp.dccexRouteDetailsReceived[i] = false;
                            comm_thread.wifiSend("<JA " + args[i + 1] + ">");
                        }

                        int count = (mainapp.dccexRouteIDs==null) ? 0 : mainapp.dccexRouteIDs.length;
                        Log.d(threaded_application.applicationName, activityName + ": processDccexRoutes(): Routes list received. Count: " + count);
                    }
                }
            }
        }
    } // end processDccexRoutes()

    /* ***********************************  *********************************** */

    public static void processDccexRouteUpdate(String [] args) {
        if (args != null) {
            int pos = -1;
            boolean foundInMainList = false;
            if (mainapp.routeSystemNames != null) {
                for (String sn : mainapp.routeSystemNames) {
                    pos++;
                    if (sn != null && sn.equals(args[1])) {
                        foundInMainList = true;
                        if (pos >= 0 && pos <= mainapp.routeSystemNames.length) {  //if found, update to new value
                            if (args[2].charAt(0) != '"') { // <jB id "stuff">
                                mainapp.routeDccexStates[pos] = Integer.parseInt(args[2]);
                            } else { // <jB id <state>    state: 0=inactive 1=active 2=hidden
                                mainapp.routeDccexLabels[pos] = args[2].substring(1, args[2].length() - 1);
                            }
                        }
                        break;
                    }
                }
            }
            if (!foundInMainList) { // received a value before the main route list has been built. need to save them for later.
                int routeId = Integer.parseInt(args[1]);
                for (int i=0; i<mainapp.dccexRouteIDs.length;i++) {
                    if (mainapp.dccexRouteIDs[i]==routeId) {
                        if (args[2].charAt(0) != '"') { // <jB id "stuff">
                            mainapp.dccexRouteStates[i] = args[2];
                        } else { // <jB id <state>    state: 0=inactive 1=active 2=hidden
                            mainapp.dccexRouteLabels[i] = args[2].substring(1, args[2].length() - 1);
                        }
                        mainapp.dccexRouteStatesReceived = true;
                        break;
                    }
                }
            }
        }
    } // end processDccexRouteUpdate()

    /* ***********************************  *********************************** */
    /* ***********************************  *********************************** */
    /* ***********************************  *********************************** */

    static boolean verifyParametersAreNumeric(String [] args, boolean [] numericParameters) {
        for (int i=0; i<numericParameters.length; i++) {
            if (args.length<=i) {
                if (numericParameters[i]) {
                    try {
                        Integer.parseInt(args[i]);
                    } catch (Exception e) {
                        return false;
                    }
                }
            } // accept it may be short
        }
        return true;
    }
}
