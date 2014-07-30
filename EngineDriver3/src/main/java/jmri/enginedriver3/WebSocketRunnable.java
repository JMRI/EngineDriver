package jmri.enginedriver3;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


/**
 * Created by stevet on 7/16/2014.
 */
class WebSocketRunnable implements Runnable {
    private PermaFragment permaFragment; //set in constructor
    private MainApplication mainApp; //set in constructor
    private String requestedServer; //set in constructor or by CONNECT_REQUESTED message
    private int requestedWebPort; //set in constructor or by CONNECT_REQUESTED message

    private JmriWebSocketHandler jmriWebSocket = null; //set at beginning of run()

    //create, expecting refs to permaFrag and mainApp passed in
    public WebSocketRunnable(PermaFragment in_permaFragment,
                             MainApplication in_mainApp,
                             String in_requestedServer,
                             int in_requestedWebPort) {
//        Log.d(Consts.DEBUG_TAG, "in WebSocketRunnable() " + in_requestedServer + ":" + in_requestedWebPort);
        permaFragment = in_permaFragment;
        mainApp = in_mainApp;
        requestedServer = in_requestedServer;
        requestedWebPort = in_requestedWebPort;
    }

    @Override
    public void run() {
//        Log.d(Consts.DEBUG_TAG, "starting WebSocketRunnable.run()");
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
        Log.d(Consts.DEBUG_TAG, "ending WebSocketRunnable.run()");
    }

    private class WebSocketRunnableHandler extends Handler {
        final String pingJsonString = "{\"type\":\"ping\",\"data\":{}}";

        @Override
        public void handleMessage(Message msg) {
//            Log.d(Consts.DEBUG_TAG, "in WebSocketRunnable.handleMessage()");
            switch (msg.what) {
                case MessageType.SHUTDOWN:
                    Log.d(Consts.DEBUG_TAG, "in WebSocketRunnable.handleMessage() SHUTDOWN");
                    getLooper().quit(); //stop the looper
                    break;
                case MessageType.HEARTBEAT:
//                    Log.d(Consts.DEBUG_TAG, "in WebSocketRunnable.handleMessage() HEARTBEAT");
                    try {
                        jmriWebSocket.webSocketConnection.sendTextMessage(pingJsonString);
                    } catch (Exception e) {}  //if anything bad happens here, just ignore it
                    break;
                //this is received when already connected, and user asks for another server
//                case MessageType.CONNECT_REQUESTED:
//                    Log.d(Consts.DEBUG_TAG, "in WebSocketRunnable.handleMessage() CONNECT_REQUESTED");
//                    requestedServer = msg.obj.toString();  //set requested vars from msg
//                    requestedWebPort = msg.arg1;
//                    jmriWebSocket.connect();
//                    break;
                default:
                    Log.w(Consts.DEBUG_TAG, "in WebSocketRunnable.handleMessage() received unknown message type " + msg.what);
                    break;
            }  //end of switch msg.what
            super.handleMessage(msg);
        }
    }

    class JmriWebSocketHandler extends WebSocketHandler {
        private WebSocketConnection webSocketConnection = new WebSocketConnection();

        @Override
        public void onOpen() {
            final String initialWebSocketRequest1 = "{\"type\":\"memory\",\"data\":{\"name\":\"IMCURRENTTIME\"}}";
            final String initialWebSocketRequest2 = "{\"type\":\"power\",\"data\":{}}";
//            displayClock = true;
            Log.d(Consts.DEBUG_TAG,"JmriWebSocket opened");
            try {
                this.webSocketConnection.sendTextMessage(initialWebSocketRequest1);  //TODO: improve this
                this.webSocketConnection.sendTextMessage(initialWebSocketRequest2);
            } catch(Exception e) {
                Log.w(Consts.DEBUG_TAG,"JmriWebSocket open error: "+e.toString());
                mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.DISCONNECTED); //tell the app
            }
        }

        @Override
        public void onTextMessage(String msgString) {
//            Log.d(Consts.DEBUG_TAG,"JmriWebSocket got a msg " + msgString);
            final String sClockMemoryName = "IMCURRENTTIME";
            int displayClockHrs = 0;
            final SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a");
            final SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm");
            try {
                JSONObject msgJsonObject = new JSONObject(msgString);
                String type = msgJsonObject.getString("type");
                if (type.equals("hello")) {
                    Log.d(Consts.DEBUG_TAG,"hello message received, data=" + msgJsonObject.getString("data"));
                    JSONObject data = msgJsonObject.getJSONObject("data");
                    mainApp.setServer(requestedServer);  //update shared vars now that we know we're connected
                    mainApp.setWebPort(requestedWebPort);
                    mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.CONNECTED);
                    //put other values into shared variables for later use
                    mainApp.setJmriVersion(data.getString("JMRI"));
                    mainApp.setJmriHeartbeat(data.getInt("heartbeat"));
                } else if (type.equals("power")) {
                    JSONObject data = msgJsonObject.getJSONObject("data");
                    Log.d(Consts.DEBUG_TAG, "power=" + data.getString("state"));
                    if (mainApp.getPowerState()==null
                            || !mainApp.getPowerState().equals(data.getString("state"))) {
                        mainApp.setPowerState(data.getString("state"));
                        mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.POWER_STATE_CHANGED);
                    }
                } else if (type.equals("pong")) {
//                    Log.d(Consts.DEBUG_TAG, "pong");
                } else if (type.equals("memory")) {
                    JSONObject data = msgJsonObject.getJSONObject("data");
                    Log.d(Consts.DEBUG_TAG,"memory rcvd, name=" + data.getString("name") +
                            ", value=" + data.getString("value"));
                    //is it a clock update?, if so, format, set the shared var, and shout about it
                    if(sClockMemoryName.equals(data.getString("name"))) {
                        String currentTime = data.getString("value");
                        if(currentTime.length() > 0) {
                            try {  //TODO: add pref for format
                                if(currentTime.indexOf("M") < 0) {			// no AM or PM - in 24 hr format
                                    if(displayClockHrs == 1) {				// display in 12 hr format
                                        currentTime = sdf12.format(sdf24.parse(currentTime));
                                    }
                                } else {									// in 12 hr format
                                    if(displayClockHrs == 2) {				// display in 24 hr format
                                        currentTime = sdf24.format(sdf12.parse(currentTime));
                                    }
                                }
                            } catch (ParseException e) { }
                            mainApp.setJmriTime(currentTime);
                        }
                    }
                } else {
                    Log.w(Consts.DEBUG_TAG,"JmriWebSocket, unexpected message received " + msgString);
                }
            } catch (JSONException e) {
                Log.w(Consts.DEBUG_TAG,"JmriWebSocket, could not convert " + msgString);
            }
        }

        @Override
        public void onClose(int code, String closeReason) {
            String s = "JmriWebSocket onClose(), code=" + code + ", reason=" + closeReason;
            Log.d(Consts.DEBUG_TAG,s);
            mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.MESSAGE_LONG, s); //tell the user
            mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.DISCONNECTED); //tell the app
//            permaFragment.webSocketRunnableHandler.getLooper().quit(); //stop the looper
        }

        private void connect() {
            Log.d(Consts.DEBUG_TAG,"JmriWebSocket connection attempt to " + requestedServer+":"+requestedWebPort);
            try {
                this.webSocketConnection.connect(createUri(), this);
            } catch (WebSocketException e) {
                Log.w(Consts.DEBUG_TAG,"JmriWebSocket connect failed: "+e.toString());
                mainApp.sendMsg(permaFragment.permaFragHandler, MessageType.DISCONNECTED); //tell the app
            }
        }

        public void disconnect() {
//            try {
            this.webSocketConnection.disconnect();
//            } catch (Exception e) {
//                Log.d(Consts.DEBUG_TAG,"JmriWebSocket disconnect error: "+e.toString());
//            }
        }
        public boolean isConnected() {
            return this.webSocketConnection.isConnected();
        }

//        public void refresh() {
//            if (webSocketConnection.isConnected())
//                this.disconnect();
//            this.connect();
//        }
    }
    // build a full uri
    public String createUri() {
        return "ws://" + requestedServer + ":" + requestedWebPort + "/json/";
    }
}
