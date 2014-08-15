package jmri.enginedriver3;
/*  MainApplication - this overrides Android's Application object, which is created once when an app starts.
      Note: It may (or may NOT) be destroyed when all activities have ended.
      ED3 will use this for shared data accessed by various threads, fragments and the activity.
      All variables should be private, accessed only by get(), .set() and helper functions.  The .set() functions are
      also responsible for sending xx_UPDATED messages when variables change, to make sure interested objects are
      informed of the changes.
      This object will also house some utility functions, such as message handling.  (May move these to a separate class?)
* */
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Application;
import android.os.Message;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

public class MainApplication extends Application {

    //definitions of the fragments (tabs) in use.  Note that name is expected to be unique
    private SparseArray<DynaFragEntry> _dynaFrags = new SparseArray<DynaFragEntry>();  //this is built in activity
    public SparseArray<DynaFragEntry> getDynaFrags() { return _dynaFrags; }
    public void setDynaFrags(SparseArray<DynaFragEntry> dynaFrags) { this._dynaFrags = dynaFrags; }
    //convenience method to set the handler for the specified fragment number, called from each dynafrag to set and to clear
    public void setDynaFragHandler(Integer in_fragmentNum, Handler in_handler) {
        this._dynaFrags.get(in_fragmentNum).setHandler(in_handler);
    }
    //convenience method to determine if a tab name is already in use
    public boolean dynaFragExists(String in_tabName) {
        for (int i = 0; i < _dynaFrags.size(); i++) {
            if (_dynaFrags.get(i).getName().equals(in_tabName)) {
                return true;
            }
        }
        return false;
    }
//    mainApp.setDynaFragHandler(getFragNum(), new Fragment_Handler());


    //store variables for use by the activity and all fragments
    private MainActivity _mainActivity;
    public MainActivity getMainActivity() {return _mainActivity;}
    public void setMainActivity(MainActivity in_mainActivity) {this._mainActivity = in_mainActivity;}

    //list of discovered servers, maintained by jmdns runnable, read by ConnectFragment
    private ArrayList<HashMap<String, String> > _discoveredServersList;
    public ArrayList<HashMap<String, String>> getDiscoveredServersList() {return _discoveredServersList;}
    public void setDiscoveredServersList(ArrayList<HashMap<String, String>> in_discoveredServersList) {
        this._discoveredServersList = in_discoveredServersList;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.DISCOVERED_SERVER_LIST_CHANGED); //announce the change
    }

    //list of roster entries, maintained by websocket thread
    private HashMap<String, RosterEntry> _rosterEntryList;
    public HashMap<String, RosterEntry> getRosterEntryList() {return _rosterEntryList;}
    public void setRosterEntryList(HashMap<String, RosterEntry> in_rosterEntryList) {
        this._rosterEntryList = in_rosterEntryList;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.ROSTERENTRY_LIST_CHANGED); //announce the change
    }

    //list of defined turnouts, empty when disconnected
    //key=system name of turnout, stores Turnout class
    private HashMap<String, Turnout> _turnoutList;
    public HashMap<String, Turnout> getTurnoutList() {return _turnoutList;}
    public void setTurnoutList(HashMap<String, Turnout> in_turnoutList) {
        this._turnoutList = in_turnoutList;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.TURNOUT_LIST_CHANGED); //announce the change
    }
    public Turnout getTurnout(String in_turnout) {  //helper function
        return this._turnoutList.get(in_turnout);
    }
    public int getTurnoutState(String in_turnout) {  //helper function
        return this._turnoutList.get(in_turnout).getState();
    }
    public void setTurnoutState(String in_turnout, int in_state) {  //helper function, also sends alert message
        if (getTurnoutState(in_turnout) != in_state) { //don't do anything if already this state
//            Log.d(Consts.APP_NAME, "setTurnoutState(" + in_turnout + ", " + in_state +")");
            this._turnoutList.get(in_turnout).setState(in_state);  //update just the state of this entry
            if (_mainActivity != null)
                sendMsg(_mainActivity.mainActivityHandler, MessageType.TURNOUT_LIST_CHANGED); //announce the change
        }
    }

    //list of defined routes, empty when disconnected
    //key=system name of turnout, stores Route class
    private HashMap<String, Route> _routeList;
    public HashMap<String, Route> getRouteList() {return _routeList;}
    public void setRouteList(HashMap<String, Route> in_routeList) {
        this._routeList = in_routeList;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.ROUTE_LIST_CHANGED); //announce the change
    }
    public Route getRoute(String in_route) {  //helper function
        return this._routeList.get(in_route);
    }
    public int getRouteState(String in_route) {  //helper function
        return this._routeList.get(in_route).getState();
    }
    public void setRouteState(String in_route, int in_state) {  //helper function, also sends alert message
        if (getRouteState(in_route) != in_state) { //don't do anything if already this state
//            Log.d(Consts.APP_NAME, "setTurnoutState(" + in_turnout + ", " + in_state +")");
            this._routeList.get(in_route).setState(in_state);  //update just the state of this entry
            if (_mainActivity != null)
                sendMsg(_mainActivity.mainActivityHandler, MessageType.ROUTE_LIST_CHANGED); //announce the change
        }
    }


    //jmri server name (used for both WiThrottle and Web connections)
	private String _server = null;
	public String getServer() { return _server; }
	public void setServer(String server) { _server = server; }

    public boolean isConnected() {  //convenience method for checking connectivity to jmri server
        return (_server!=null);
    }

	//JMRI web server port #
	private int _webPort = -1;
	public int getWebPort() { return _webPort; }
	public void setWebPort(int in_webPort) { _webPort = in_webPort; }

    private String _jmriTime = null;  //will be set by JmriWebSocket
    public String getJmriTime() {return _jmriTime;}
    public void setJmriTime(String in_jmriTime) {
        this._jmriTime = in_jmriTime;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.JMRI_TIME_CHANGED); //announce it
    }

    private String _railroad = null;  //will be set by JmriWebSocket
    public String getRailroad() {return _railroad;}
    public void setRailroad(String in_railroad) {
        this._railroad = in_railroad;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.RAILROAD_CHANGED); //announce it
    }

    private String _powerState = null;  //will be set by JmriWebSocket
    public String getPowerState() {return _powerState;}
    public void setPowerState(String in_powerState) {
        if (_powerState == null || !_powerState.equals(in_powerState)) {
            Log.d(Consts.APP_NAME, "power state changed from " + _powerState + " to " + in_powerState);  //TODO: show descriptions
            this._powerState = in_powerState;  //if different, update it and announce it
            if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.POWER_STATE_CHANGED);
        }
    }

    private String _jmriVersion = null;  //will be set by JmriWebSocket
    public String getJmriVersion() {return _jmriVersion;}
    public void setJmriVersion(String in_jmriVersion) {this._jmriVersion = in_jmriVersion;}

    private int _jmriHeartbeat = Consts.INITIAL_HEARTBEAT;
    public int getJmriHeartbeat() {return _jmriHeartbeat;}
    public void setJmriHeartbeat(int in_jmriHeartbeat) {this._jmriHeartbeat = in_jmriHeartbeat;}

//	public EngineDriver3Application() {
//	}

	@Override
	public void onCreate()  {
		super.onCreate();
	    Log.d(Consts.APP_NAME,"in ED3Application.onCreate()");

        //initialize the temporary fragment list, for now, key is 0-n, cannot leave "holes"
        // replace this with restore from database or somesuch, maybe in ED3Activity.onCreate()
/*
        int fragKey = 0;
        _dynaFrags = new SparseArray<DynaFragEntry>();  //make sure its ready for use.  Populated by activity
//        _dynaFrags.put(fragKey++, new DynaFragEntry("Prefs", Consts.PREFS, 2));
        DynaFragEntry tEntry;
        tEntry = new DynaFragEntry("About", Consts.WEB, 2);
        tEntry.setData("file:///android_asset/about_page.html");
        _dynaFrags.put(fragKey++, tEntry);
        _dynaFrags.put(fragKey++, new DynaFragEntry("Connect", Consts.CONNECT, 2));
        tEntry = new DynaFragEntry("Throttle", Consts.WEB, 2);
        tEntry.setData("/web/webThrottle.html");
        _dynaFrags.put(fragKey++, tEntry);

        tEntry = new DynaFragEntry("Turnouts", Consts.TURNOUT, 2);
        _dynaFrags.put(fragKey++, tEntry);

        tEntry = new DynaFragEntry("Panels", Consts.WEB, 3);
        tEntry.setData("/panel/");
        _dynaFrags.put(fragKey++, tEntry);

        tEntry = new DynaFragEntry("Trains", Consts.WEB, 2);
        tEntry.setData("/operations/trains");
        _dynaFrags.put(fragKey++, tEntry);

        Log.d(Consts.APP_NAME,"end of ED3Application.onCreate()");

*/
	}
    public boolean sendMsg(Handler h, int msgType) {
        return sendMsgDelayed(h, 0, msgType, "", 0, 0);
    }
    public boolean sendMsg(Handler h, int msgType, String msgBody) {
        return sendMsgDelayed(h, 0, msgType, msgBody, 0, 0);
    }
    public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1) {
        return sendMsgDelayed(h, 0, msgType, msgBody, msgArg1, 0);
    }
    public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1, int msgArg2) {
        return sendMsgDelayed(h, 0, msgType, msgBody, msgArg1, msgArg2);
    }
    public boolean sendMsgDelayed(Handler h, long delayMs, int msgType) {
        return sendMsgDelayed(h, delayMs, msgType, "", 0, 0);
    }
    //this one forwards a copy of the input message
    public boolean sendMsg(Handler h, Message msg) {
        return sendMsgDelayed(h, 0, msg.what, (msg.obj == null) ? null : msg.obj.toString(), msg.arg1, msg.arg2);
    }
    //this one forwards a copy of the input message
    public boolean sendMsgDelayed(Handler h, long delayMs, Message msg) {
        return sendMsgDelayed(h, delayMs, msg.what, (msg.obj == null) ? null : msg.obj.toString(), msg.arg1, msg.arg2);
    }
    public boolean sendMsgDelayed(Handler h, long delayMs, int msgType, String msgBody, int msgArg1, int msgArg2) {
        boolean sent = false;
        if(h != null) {
            Message msg= Message.obtain();
            msg.what=   msgType;
            msg.obj=    msgBody;
            msg.arg1=   msgArg1;
            msg.arg2=   msgArg2;
            try {
                sent = h.sendMessageDelayed(msg, delayMs);
            }
            catch(Exception e) {
                Log.d(Consts.APP_NAME, "failed to send message of type " + msgType + " to " + h.getClass());
            }
            if(!sent)
                msg.recycle();
        }
        return sent;
    }




}
