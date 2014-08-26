package jmri.enginedriver3;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


/**
 * WebSocketRunnable - executed as a thread by the PermaFrag, ctor expects refs to PermaFrag, mainApp,
 *   and the server and port that are being requested.  If connect attempt fails, the thread ends.
 *   Also will end itself nicely when requested by message from PermaFrag, or when the connection is lost.
 *   While running, accepts various messages which it formats into json requests and sends to server
 *   Then processes the responses from the server, updating shared variables and sending messages.
 */
class WebSocketRunnable implements Runnable {
    private PermaFragment permaFragment; //passed in constructor
    private MainApplication mainApp; //passed in constructor
    private String requestedServer; //passed in constructor
    private int requestedWebPort; //passed in constructor

    private JmriWebSocketHandler jmriWebSocket = null; //set at beginning of run()

    //create, expecting refs to permaFrag and mainApp passed in
    public WebSocketRunnable(PermaFragment in_permaFragment,
                             MainApplication in_mainApp,
                             String in_requestedServer,
                             int in_requestedWebPort) {
//        Log.d(Consts.APP_NAME, "in WebSocketRunnable() " + in_requestedServer + ":" + in_requestedWebPort);
        permaFragment = in_permaFragment;
        mainApp = in_mainApp;
        requestedServer = in_requestedServer;
        requestedWebPort = in_requestedWebPort;
    }

    @Override
    public void run() {
//        Log.d(Consts.APP_NAME, "starting WebSocketRunnable.run()");
        Looper.prepare();
        permaFragment.webSocketRunnableHandler = new WebSocketRunnableHandler();  //update ref to thread's handler back in retained frag

        jmriWebSocket = new JmriWebSocketHandler();
//        clockWebSocket.refresh();
        jmriWebSocket.connect();

        Looper.loop();

        if (jmriWebSocket != null) {
            jmriWebSocket.disconnect();
            jmriWebSocket = null;
        }
        Log.d(Consts.APP_NAME, "ending WebSocketRunnable.run()");
    }

    private class WebSocketRunnableHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.APP_NAME, "in WebSocketRunnable.handleMessage()");
            switch (msg.what) {
                case MessageType.SHUTDOWN:
                    Log.d(Consts.APP_NAME, "in WebSocketRunnable.handleMessage() SHUTDOWN");
                    try {  //tell the server goodbye
                        jmriWebSocket.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_GOODBYE);
                    } catch (Exception e) {}  //if anything bad happens here, just ignore it
//                    getLooper().quit(); //stop the looper
                    break;
                case MessageType.HEARTBEAT:
//                    Log.d(Consts.APP_NAME, "in WebSocketRunnable.handleMessage() HEARTBEAT");
                    try {
                        jmriWebSocket.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_PING);
                    } catch (Exception e) {}  //if anything bad happens here, just ignore it
                    break;
                case MessageType.LOCO_REQUESTED:
                    //extract the parms from the json'ed hashmap and call method to process
                    Gson gson = new Gson();
                    HashMap<String, String> hm = gson.fromJson(msg.obj.toString(), new TypeToken<HashMap<String, String>>() {}.getType());
                    String fragmentName = hm.get("fragment_name");
                    String rosterName = hm.get("roster_name");
                    String rosterAddress = hm.get("roster_address");
                    int locoDirection = Integer.parseInt(hm.get("loco_direction"));
                    locoRequested(fragmentName, rosterName, rosterAddress, locoDirection);
                    break;
                case MessageType.RELEASE_LOCO_REQUESTED:
                    //extract the parms from the json'ed hashmap and call method to process
                    gson = new Gson();
                    hm = gson.fromJson(msg.obj.toString(), new TypeToken<HashMap<String, String>>() {}.getType());
                    fragmentName = hm.get("fragment_name");
                    rosterName = hm.get("roster_name");
                    releaseLocoRequested(fragmentName, rosterName);
                    break;
                case MessageType.TURNOUT_CHANGE_REQUESTED:
                    Turnout t = mainApp.getTurnout(msg.obj.toString()); //obj is turnout name
                    String jc = t.getChangeStateJson(msg.arg1);  //arg1 is requested state
//                    Log.d(Consts.APP_NAME, "in WebSocketRunnable.handleMessage() TURNOUT_CHANGE_REQUESTED " + jc);
                    try {
                        jmriWebSocket.webSocketConnection.sendTextMessage(jc);  //send the request to the server
                    } catch (Exception e) {}  //if anything bad happens here, just ignore it
                    break;
                case MessageType.SPEED_CHANGE_REQUESTED:
                    Throttle throttle = mainApp.getThrottle(msg.obj.toString()); //obj is throttleKey
                    float newSpeed = throttle.getSpeedForDisplayedSpeed(msg.arg1);  //arg2 is displayedSpeed
                    String js = throttle.getSpeedChangeJson(newSpeed);  //arg1 is direction, arg2 is speed
                    Log.d(Consts.APP_NAME, "in WebSocketRunnable.handleMessage() SPEED_CHANGE_REQUESTED " + js);
                    try {
                        jmriWebSocket.webSocketConnection.sendTextMessage(js);  //send the request to the server
                    } catch (Exception e) {
                        Log.w(Consts.APP_NAME, "problem sending SPEED_CHANGE message " + e);
                    }
                    break;
                case MessageType.DIRECTION_CHANGE_REQUESTED:
                    throttle = mainApp.getThrottle(msg.obj.toString()); //obj is throttleKey
                    String jd = throttle.getDirectionChangeJson(msg.arg1);  //arg1 is direction
                    Log.d(Consts.APP_NAME, "in WebSocketRunnable.handleMessage() DIRECTION_CHANGE_REQUESTED " + jd);
                    try {
                        jmriWebSocket.webSocketConnection.sendTextMessage(jd);  //send the request to the server
                    } catch (Exception e) {
                        Log.w(Consts.APP_NAME, "problem sending DIRECTION_CHANGE message " + e);
                    }
                    break;
                default:
                    Log.w(Consts.APP_NAME, "in WebSocketRunnable.handleMessage() received unknown message type " + msg.what);
                    break;
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

    private void locoRequested(String fragmentName, String rosterName, String rosterAddress, int locoDirection) {
        String throttleKey = fragmentName + ":" + rosterName;
        Throttle t = mainApp.getThrottle(throttleKey);
        if (t==null) {  //create new entry if none exists, to keep track of fragment name
            Log.d(Consts.APP_NAME, "locoRequested " + throttleKey + " created");
            t = new Throttle(throttleKey, fragmentName, rosterName);
            mainApp.storeThrottle(throttleKey, t);
        }
        String s = "{\"type\":\"throttle\",\"data\":{\"throttle\":\""
                + throttleKey + "\",\"address\":" + rosterAddress + "}}";
        Log.d(Consts.APP_NAME, "sending '" + s + "' to jmri");
        jmriWebSocket.webSocketConnection.sendTextMessage(s);

    }

    private void releaseLocoRequested(String fragmentName, String rosterName) {
        String throttleKey = fragmentName + ":" + rosterName;
        String s = "{\"type\":\"throttle\",\"data\":{\"throttle\":\""
                + throttleKey + "\",\"release\":null}}";
        Log.d(Consts.APP_NAME, "sending '"+s+"' to jmri");
        jmriWebSocket.webSocketConnection.sendTextMessage(s);
    }

    private class JmriWebSocketHandler extends WebSocketHandler {
        private WebSocketConnection webSocketConnection = new WebSocketConnection();

        @Override
        public void onOpen() {
//            displayClock = true;
            Log.d(Consts.APP_NAME,"JmriWebSocket opened");
            try {
                this.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_TIME);
                this.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_POWER_STATE);
                this.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_ROSTER_LIST);
                this.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_TURNOUTS_LIST);
                this.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_ROUTES_LIST);
                this.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_SYSTEMCONNECTIONS_LIST);
                this.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_CONSISTS_LIST);  //jmri-defined consists
                this.webSocketConnection.sendTextMessage(Consts.JSON_REQUEST_PANELS_LIST);
            } catch(Exception e) {
                Log.w(Consts.APP_NAME,"JmriWebSocket error in onOpen(): "+e.toString());
                mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.DISCONNECTED); //tell the app
            }
        }

        @Override
        public void onTextMessage(String msgString) {
//            Log.d(Consts.APP_NAME,"JmriWebSocket got a msg " + msgString);
            JSONObject msgJsonObject = null;
            JSONArray msgJsonArray = null;
            String type = null;
            JSONObject data = null;
            //it is either a single json object, or a json array
            try {
                msgJsonObject = new JSONObject(msgString);
                type = msgJsonObject.getString("type");
                if (!type.equals("pong") && !type.equals("goodbye")) {  //pong and goodbye have no data
                    data = msgJsonObject.getJSONObject("data");
                }
                if (type.equals("hello")) {  //process according to type of data received
                    receivedHello(data);
                } else if (type.equals("power")) {
                    mainApp.setPowerState(data.getString("state"));
                } else if (type.equals("throttle")) {
                    receivedThrottle(data);
                } else if (type.equals("turnout")) {
                    mainApp.setTurnoutState(data.getString("name"), data.getInt("state"));
                } else if (type.equals("route")) {
                    mainApp.setRouteState(data.getString("name"), data.getInt("state"));
                } else if (type.equals("pong")) {
//                    Log.d(Consts.APP_NAME, "JmriWebSocket, pong message received and ignored");
                } else if (type.equals("goodbye")) {
//                    Log.d(Consts.APP_NAME, "JmriWebSocket, goodbye message received and ignored");
                } else if (type.equals("memory")) {
                    receivedMemory(data);
                } else if (type.equals("error")) {  //log and show the error message
                    String s = data.getString("message");
                    Log.w(Consts.APP_NAME, "received error from JmriWebSocket: " + s);
                    mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.MESSAGE_SHORT, s);
                } else {
                    Log.w(Consts.APP_NAME, "JmriWebSocket, unexpected message received " + msgString);
                }
            } catch (JSONException e) {  //not a json object, treat as a json array
                try {
                    msgJsonArray = new JSONArray(msgString);
                    if (msgJsonArray.length() < 1) return;  //ignore empty lists
                    type = msgJsonArray.getJSONObject(0).getString("type");  //get type from first, assume all identical TODO:not true for panels
                    if (type.equals("turnout")) {  //process according to type of array elements received
                        receivedTurnoutList(msgJsonArray);
                    } else if (type.equals("route")) {
                        receivedRouteList(msgJsonArray);
                    } else if (type.equals("rosterEntry")) {
//                        Log.d(Consts.APP_NAME,"RosterEntry array=" + msgString);
                        receivedRosterEntryList(msgJsonArray);
                    } else if (type.equals("systemConnection")) {
                        Log.d(Consts.APP_NAME,"systemConnection array=" + msgString);
                    } else if (type.equals("consist")) {
                        Log.d(Consts.APP_NAME,"consist array=" + msgString);
                    } else if (type.equals("Layout")
                            || type.equals("Control Panel")
                            || type.equals("Panel")) {
//                        Log.d(Consts.APP_NAME,"panel array=" + msgString);
                        receivedPanelList(msgJsonArray);
                    } else {
                        Log.w(Consts.APP_NAME, "unsupported json array received="+msgString);
                    }
                } catch (JSONException e1) {
                    Log.d(Consts.APP_NAME, "error converting to json object or array.  ignoring " + msgString);
//                    return;
                }
            }
        }

        private void receivedTurnoutList(JSONArray msgJsonArray) throws JSONException {
            HashMap<String, Turnout> tl = new HashMap<String, Turnout>();  //make a temp list to populate
            for (int i = 0; i < msgJsonArray.length(); i++) {
                JSONObject data = msgJsonArray.getJSONObject(i).getJSONObject("data");
                if (!data.getString("userName").equals("null")) {  //skip any without a username TODO:verify we still want to handle this this way
                    Turnout t = new Turnout(data.getString("name"), data.getInt("state"),  //make a temp turnout for a single entry
                            (data.getString("userName").equals("null") ? null : data.getString("userName")),
                            data.getBoolean("inverted"),
                            (data.getString("comment").equals("null") ? null : data.getString("comment")));
                    tl.put(data.getString("name"), t);  //add this entry to the list, keyed by name
                    String s = t.getChangeStateJson(Consts.STATE_UNKNOWN);  //send no-change request to start listener
                    this.webSocketConnection.sendTextMessage(s);
                }
            }
            mainApp.setTurnoutList(tl);  //replace the shared var with the newly populated one
            Log.d(Consts.APP_NAME, "turnout list received containing " + tl.size() + " entries.");
        }

        private void receivedPanelList(JSONArray msgJsonArray) throws JSONException {
            SparseArray<Panel> pl = new SparseArray<Panel>();  //make a temp list to populate
            for (int i = 0; i < msgJsonArray.length(); i++) {
                String t = msgJsonArray.getJSONObject(i).getString("type");
                String n = msgJsonArray.getJSONObject(i).getString("name");
                String u = msgJsonArray.getJSONObject(i).getString("userName");
                String url = msgJsonArray.getJSONObject(i).getString("URL");
                Panel p = new Panel(t, n, u, url);
                pl.put(i, p);  //add this entry to the list
            }
            mainApp.setPanelList(pl);  //replace the shared var with the newly populated one
            Log.d(Consts.APP_NAME, "panel list received containing " + pl.size() + " entries.");
        }

        private void receivedRouteList(JSONArray msgJsonArray) throws JSONException {
            HashMap<String, Route> rl = new HashMap<String, Route>();  //make a temp list to populate
            for (int i = 0; i < msgJsonArray.length(); i++) {
                JSONObject data = msgJsonArray.getJSONObject(i).getJSONObject("data");
                if (!data.getString("userName").equals("null")) {  //skip any without a username TODO:verify we still want to handle this this way
                    Route r = new Route(data.getString("name"), data.getInt("state"),
                            (data.getString("userName").equals("null") ? null : data.getString("userName")),
                            (data.getString("comment").equals("null") ? null : data.getString("comment")));
                    rl.put(data.getString("name"), r);  //add this entry to the list, keyed by name
                    String s = r.getChangeStateJson(data.getInt("state"));  //send no-change request to start listener
                    this.webSocketConnection.sendTextMessage(s);
                }
            }
            mainApp.setRouteList(rl);  //replace the shared var with the newly populated one
            Log.d(Consts.APP_NAME, "route list received containing " + rl.size() + " entries.");
        }

        private void receivedRosterEntryList(JSONArray msgJsonArray) throws JSONException {
            HashMap<String, RosterEntry> rl = new HashMap<String, RosterEntry>();  //make a temp list to populate
            for (int i = 0; i < msgJsonArray.length(); i++) {
                JSONObject data = msgJsonArray.getJSONObject(i).getJSONObject("data");
                RosterEntry r = new RosterEntry(data.getString("name"), data.getString("address"),
                        data.getBoolean("isLongAddress"),
                        (data.getString("comment").equals("null") ? null : data.getString("comment")));
                //TODO: add other fields
                rl.put(data.getString("name"), r);  //add this entry to the list, keyed by name
            }
            mainApp.setRosterEntryList(rl);  //replace the shared var with the newly populated one
            Log.d(Consts.APP_NAME, "roster list received containing " + rl.size() + " entries.");
        }

        private void receivedMemory(JSONObject data) throws JSONException {
            int displayClockHrs = 0;
//                    Log.d(Consts.APP_NAME,"memory rcvd, name=" + data.getString("name") +
//                            ", value=" + data.getString("value"));
            //is it a clock update?, if so, format, set the shared var, and shout about it
            if (Consts.CLOCK_MEMORY_NAME.equals(data.getString("name"))) {
                String currentTime = data.getString("value");
                if (currentTime.length() > 0) {
                    final SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a");
                    final SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm");
                    try {  //TODO: add pref for format
                        if (!currentTime.contains("M")) {            // no AM or PM - in 24 hr format
                            if (displayClockHrs == 1) {                // display in 12 hr format
                                currentTime = sdf12.format(sdf24.parse(currentTime));
                            }
                        } else {                                    // in 12 hr format
                            if (displayClockHrs == 2) {                // display in 24 hr format
                                currentTime = sdf24.format(sdf12.parse(currentTime));
                            }
                        }
                    } catch (ParseException e) {
                    }
                    mainApp.setJmriTime(currentTime);
                }
            } else {
                Log.d(Consts.APP_NAME, "json memory item received, but not expected " + data.toString());
            }
        }

        private void receivedHello(JSONObject data) throws JSONException {
            Log.d(Consts.APP_NAME, "hello message received, data=" + data.toString());
            mainApp.setServer(requestedServer);  //update shared vars now that we know we're connected
            mainApp.setWebPort(requestedWebPort);
            mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.CONNECTED);
            //put other values into shared variables for later use
            mainApp.setJmriVersion(data.getString("JMRI"));
            mainApp.setRailroad(data.getString("railroad"));
            mainApp.setJmriHeartbeat(data.getInt("heartbeat"));
        }

        private void receivedThrottle(JSONObject data) throws JSONException {
            Log.d(Consts.APP_NAME, "throttle message received, data=" + data.toString());
            boolean changed = false;
            String throttleKey = data.getString("throttle");
            Throttle t = mainApp.getThrottle(throttleKey);
            t.setConfirmed(true);  //TODO: unset this somewhere
            if (data.has("speed")) {
                float speed = (float) data.getDouble("speed");
                t.setSpeed(speed);
                changed = true;
            }
            if (data.has("forward")) {
                boolean forward = data.getBoolean("forward");
                t.setForward(forward);
                changed = true;
            }
            if (changed) {  //let the interested throttle fragments know something changed
                mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.THROTTLE_CHANGED, throttleKey);
            }
            //put other values into shared variables for later use
        }

        @Override
        public void onClose(int code, String closeReason) {
            String s = closeReason + "\nJmriWebSocket onClose(), code=" + code;
            Log.d(Consts.APP_NAME,s);
            mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.MESSAGE_LONG, s); //tell the user TODO:don't tell if close was intentional
            mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.DISCONNECTED); //tell the app
        }

        private void connect() {
            Log.d(Consts.APP_NAME,"JmriWebSocket connection attempt to " + requestedServer+":"+requestedWebPort);
            try {
                this.webSocketConnection.connect(createUri(), this);
            } catch (WebSocketException e) {
                Log.w(Consts.APP_NAME,"JmriWebSocket connect failed: "+e.toString());
                mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.DISCONNECTED); //tell the app
            }
        }

        public void disconnect() { this.webSocketConnection.disconnect(); }
        public boolean isConnected() {
            return this.webSocketConnection.isConnected();
        }

    }
    // build a full uri
    public String createUri() {
        return "ws://" + requestedServer + ":" + requestedWebPort + "/json/";
    }
}
