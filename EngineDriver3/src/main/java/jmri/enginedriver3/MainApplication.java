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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;

public class MainApplication extends Application {

  private static final int NOTIFICATION_ID = 416;  //no significance to 416, just shouldn't be 0

  //definitions of the fragments (tabs) in use.  Note that name is expected to be unique
  private SparseArray<DynaFragEntry> _dynaFrags = new SparseArray<DynaFragEntry>();  //this is built in activity
  //store variables for use by the activity and all fragments
  private MainActivity _mainActivity;
  //list of discovered servers, maintained by jmdns runnable, read by ConnectFragment
  private ArrayList<HashMap<String, String> > _discoveredServersList;
  //list of roster entries, maintained by websocket thread
  private HashMap<String, RosterEntry> _rosterEntryList;
  //list of defined turnouts, empty when disconnected
  //key=system name of turnout, stores Turnout class
  private HashMap<String, Turnout> _turnoutList;
//    mainApp.setDynaFragHandler(getFragNum(), new Fragment_Handler());
  //list of known throttles, empty when disconnected
  //key=throttleKey as used by jmri json server
  private HashMap<String, Throttle> _throttleList;
  //list of defined routes, empty when disconnected
  //key=system name of turnout, stores Route class
  private HashMap<String, Route> _routeList;
  //jmri server name (used for both WiThrottle and Web connections)
  private String _server = null;
  //JMRI web server port #
  private int _webPort = -1;
  private String _jmriTime = null;  //will be set by JmriWebSocket
  private String _railroad = null;  //will be set by JmriWebSocket
  private String _activeProfile = null;  //will be set by JmriWebSocket
  private String _powerState = null;  //will be set by JmriWebSocket
  private String _jmriVersion = null;  //will be set by JmriWebSocket
  private int _jmriHeartbeat = Consts.INITIAL_HEARTBEAT;

  private float _emWidth;  //standard width of character "M", used for sizing screens and buttons, set by Activity

  private SparseArray<Panel> _panelList = new SparseArray<Panel>();  //definitions of the panels, populated in websocketthread

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

  public MainActivity getMainActivity() {return _mainActivity;}

  public void setMainActivity(MainActivity in_mainActivity) {this._mainActivity = in_mainActivity;}

  public ArrayList<HashMap<String, String>> getDiscoveredServersList() {return _discoveredServersList;}

  public void setDiscoveredServersList(ArrayList<HashMap<String, String>> in_discoveredServersList) {
    this._discoveredServersList = in_discoveredServersList;
    if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.DISCOVERED_SERVER_LIST_CHANGED); //announce the change
  }

  public HashMap<String, RosterEntry> getRosterEntryList() {return _rosterEntryList;}

  public void setRosterEntryList(HashMap<String, RosterEntry> in_rosterEntryList) {
    this._rosterEntryList = in_rosterEntryList;
    if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.ROSTERENTRY_LIST_CHANGED); //announce the change
  }

  public RosterEntry getRosterEntry(String in_rosterId) {  //helper function
    return this._rosterEntryList.get(in_rosterId);
  }

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

  public HashMap<String, Throttle> getThrottleList() {return _throttleList;}

  public void setThrottleList(HashMap<String, Throttle> in_throttleList) {
    this._throttleList = in_throttleList;
//        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.THROTTLE_LIST_CHANGED); //announce the change
  }

  public Throttle getThrottle(String in_throttle) {  //helper function
    return this._throttleList.get(in_throttle);
  }

  public void storeThrottle(String in_throttleKey, Throttle in_throttle) {
    this._throttleList.put(in_throttleKey, in_throttle);
  }
  public void removeThrottle(String in_throttleKey) {
    this._throttleList.remove(in_throttleKey);
  }

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

  public String getServer() { return _server; }

  public void setServer(String server) { _server = server; }

  public boolean isConnected() {  //convenience method for checking connectivity to jmri server
    return (_server!=null);
  }

  public int getWebPort() { return _webPort; }

  public void setWebPort(int in_webPort) { _webPort = in_webPort; }

  public String getJmriTime() {return _jmriTime;}

  public void setJmriTime(String in_jmriTime) {
    this._jmriTime = in_jmriTime;
    if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.JMRI_TIME_CHANGED); //announce it
  }

  public String getRailroad() {return _railroad;}

  public void setRailroad(String in_railroad) {
    this._railroad = in_railroad;
    if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.RAILROAD_CHANGED); //announce it
  }

  public String getActiveProfile() {return _activeProfile;}

  public void setActiveProfile(String in_activeProfile) { this._activeProfile = in_activeProfile; }

  public String getPowerState() {return _powerState;}

  public void setPowerState(String in_powerState) {
    if (_powerState == null || !_powerState.equals(in_powerState)) {
      Log.d(Consts.APP_NAME, "power state changed from " + _powerState + " to " + in_powerState);  //TODO: show descriptions
      this._powerState = in_powerState;  //if different, update it and announce it
      if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.POWER_STATE_CHANGED);
    }
  }

  public String getJmriVersion() {return _jmriVersion;}

  public void setJmriVersion(String in_jmriVersion) {this._jmriVersion = in_jmriVersion;}

  public int getJmriHeartbeat() {return _jmriHeartbeat;}

  public void setJmriHeartbeat(int in_jmriHeartbeat) {this._jmriHeartbeat = in_jmriHeartbeat;}

  public SparseArray<Panel> getPanelList() { return _panelList; }
  public void setPanelList(SparseArray<Panel> _panelList) {
    this._panelList = _panelList;
    if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.PANEL_LIST_CHANGED); //announce the change
  }
  public float getEmWidth() { return _emWidth; }
  public void setEmWidth(float emWidth) { this._emWidth = emWidth; }


//	public EngineDriver3Application() {
//	}

  //	@Override
//	public void onCreate()  {
//		super.onCreate();
//	    Log.d(Consts.APP_NAME,"in ED3Application.onCreate()");
//	}
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
  /**
   * Display OnGoing Notification that indicates EngineDriver is Running.
   * Should only be called when ED is going into the background.
   * Currently call this from each activity onPause, passing the current intent
   * to return to when reopening.  */
  void addNotification(Intent notificationIntent) {
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(getMainActivity())
            .setSmallIcon(R.drawable.ed3_launcher_icon)
            .setContentTitle(this.getString(R.string.notificationTitle))
            .setContentText(this.getString(R.string.notificationText))
            .setOngoing(true);

    PendingIntent contentIntent = PendingIntent.getActivity(getMainActivity(), NOTIFICATION_ID, notificationIntent,
        PendingIntent.FLAG_CANCEL_CURRENT);
    builder.setContentIntent(contentIntent);

    // Add as notification
    NotificationManager manager = (NotificationManager) getMainActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(NOTIFICATION_ID, builder.build());
  }

  // Remove notification
  void removeNotification() {
    NotificationManager manager = (NotificationManager) getMainActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancel(NOTIFICATION_ID);
  }


}
