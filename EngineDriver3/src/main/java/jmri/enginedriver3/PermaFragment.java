package jmri.enginedriver3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by activity on first run, this fragment has no ui, and will be retained without 
 *   restart until the app is ended.  It is responsible for all of the background threads,
 *   starting, communicating with, and stopping them as needed.  It is also responsible for 
 *   maintaining many of the shared entities found in mainApp.
 */
public class PermaFragment extends Fragment {

  private static MainApplication mainApp; // hold pointer to mainApp
  private int started = 0;

  private Thread jmdnsRunnableThread = null;
  public Handler jmdnsRunnableHandler;  //this is set by the thread after startup
  private Thread heartbeatRunnableThread = null;
  public Handler heartbeatRunnableHandler;  //this is set by the thread after startup
  private Thread  webSocketRunnableThread = null;
  public Handler webSocketRunnableHandler;  //this is set by the thread after startup

//    protected MainActivity mainActivity = null;

  PermaFragment permaFragment; //set in constructor
  Handler permaFragHandler = new PermaFrag_Handler();

  public PermaFragment() {
    permaFragment = this;
  }
  @Override
  public void onAttach(Activity activity) {
    Log.d(Consts.APP_NAME, "in PermaFrag.onAttach()");
    mainApp =(MainApplication)getActivity().getApplication();  //set pointer to app
    mainApp.setMainActivity((MainActivity) activity);  //save ref to the new activity
    super.onAttach(activity);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(Consts.APP_NAME, "in PermaFrag.onCreate()");
    mainApp =(MainApplication)getActivity().getApplication();  //set pointer to app

    //need to clear all shared variables, since app object may be reused
    //TODO: load from storage, once save is implemented
//	    setServer("10.10.3.131");
//	    setServer("192.168.1.247");
    mainApp.setServer(null);  //this will be set once connected, by the CommThread
//        mainApp.setWiThrottlePort(0);
    mainApp.setWebPort(-1);
    mainApp.setDiscoveredServersList(new ArrayList<HashMap<String, String> >());

//        mainApp.setMainActivity(null);
    mainApp.setJmriTime(null);
    mainApp.setRailroad(null);
    mainApp.setJmriHeartbeat(Consts.INITIAL_HEARTBEAT);
    mainApp.setPowerState(null);
    mainApp.setJmriVersion(null);
    mainApp.setTurnoutList(new HashMap<String, Turnout>());
    mainApp.setRouteList(new HashMap<String, Route>());
    mainApp.setRosterEntryList(new HashMap<String, RosterEntry>());
    mainApp.setThrottleList(new HashMap<String, Throttle>());  //empty the list
    mainApp.setPanelList(new SparseArray<Panel>());

    super.onCreate(savedInstanceState);
  }

  @Override
  public void onDetach() {
    Log.d(Consts.APP_NAME, "in PermaFrag.onDetach()");
    mainApp.setMainActivity(null);  //remove ref to the old activity
    super.onDetach();
  }
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    Log.d(Consts.APP_NAME, "in PermaFrag.onActivityCreated()");
    super.onActivityCreated(savedInstanceState);
    setRetainInstance(true);  //this is what makes the fragment not go away on rotation
  }
  @Override
  public void onStart() {
    started++;
    Log.d(Consts.APP_NAME, "in PermaFrag.onStart() " + started);
    super.onStart();

    startThreads();
  }
  @Override
  public void onDestroy() {
    Log.d(Consts.APP_NAME, "in PermaFrag.onDestroy()");
    cancelThreads();
    mainApp.removeNotification();
    super.onDestroy();
  }
  @Override
  public void onStop() {
    Log.d(Consts.APP_NAME, "in PermaFrag.onStop()");
    super.onStop();
  }
  @Override
  public void onResume() {
    Log.d(Consts.APP_NAME, "in PermaFrag.onResume()");
    super.onResume();
  }
  @Override
  public void onPause() {
    Log.d(Consts.APP_NAME, "in PermaFrag.onPause()");
    super.onPause();
  }
  /** Start the "permanent" background tasks, which run with app. */
  public void startThreads() {
    if (jmdnsRunnableThread == null) {
      Log.d(Consts.APP_NAME, "starting the jmdnsRunnableThread");
      jmdnsRunnableThread = new Thread(new JmdnsRunnable(this, mainApp)); //create thread, pass ref back to this fragment
      jmdnsRunnableThread.start();
    }
    if (heartbeatRunnableThread == null) {
      Log.d(Consts.APP_NAME, "starting the heartbeatRunnableThread");
      heartbeatRunnableThread = new Thread(new HeartbeatRunnable(this, mainApp)); //create thread, pass ref back to this fragment
      heartbeatRunnableThread.start();
    }
  }

  /** Cancel the background tasks via message handlers*/
  public void cancelThreads() {
    if (jmdnsRunnableThread != null && jmdnsRunnableThread.isAlive()) {
      Log.d(Consts.APP_NAME, "ending the jmdnsRunnableThread");
      mainApp.sendMsg(jmdnsRunnableHandler, MessageType.SHUTDOWN);
      jmdnsRunnableThread = null;
    }
    if (heartbeatRunnableThread != null && heartbeatRunnableThread.isAlive()) {
      Log.d(Consts.APP_NAME, "ending the heartbeatRunnableThread");
      mainApp.sendMsg(heartbeatRunnableHandler, MessageType.SHUTDOWN);
      heartbeatRunnableThread = null;
    }
    cancelWebSocketThread();
  }

  private void cancelWebSocketThread() {
    if (webSocketRunnableThread != null && webSocketRunnableThread.isAlive()) {
      Log.d(Consts.APP_NAME, "ending the webSocketRunnableThread");
      mainApp.sendMsg(webSocketRunnableHandler, MessageType.SHUTDOWN);
      webSocketRunnableThread = null;
    }
  }


  private class PermaFrag_Handler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MessageType.CONNECT_REQUESTED:
          Log.d(Consts.APP_NAME, "in PermaFrag_Handler.handleMessage() CONNECT_REQUESTED");
          //start websocket thread and let its success send CONNECTED
          if (webSocketRunnableThread != null && webSocketRunnableThread.isAlive()) {  //one is started, shut it down, give it a chance to end, try again
            Log.d(Consts.APP_NAME, "webSocketRunnableThread already running, shutting down and retrying start");
            mainApp.sendMsg(webSocketRunnableHandler, MessageType.SHUTDOWN);  //shut down running thread
            mainApp.sendMsgDelayed(this, 1000, msg);  //delay to give shutdown time to run, then try again
          } else {  //start up a new webSocketRunnable
            Log.d(Consts.APP_NAME, "starting the webSocketRunnableThread");
            String rs = msg.obj.toString();
            int wsp = msg.arg1;
            webSocketRunnableThread = new Thread(new WebSocketRunnable(permaFragment, mainApp,
                rs, wsp)); //create thread, pass ref back to this fragment, also rqsted connection
            webSocketRunnableThread.start();
          }
          break;
        case MessageType.DISCONNECT_REQUESTED:
          Log.d(Consts.APP_NAME, "in PermaFrag_Handler.handleMessage() DISCONNECT_REQUESTED");
          cancelWebSocketThread();
          break;
        //server is lost, clear out variables
        case MessageType.DISCONNECTED:
          Log.d(Consts.APP_NAME, "in PermaFrag_Handler.handleMessage() DISCONNECTED");
          webSocketRunnableThread = null;
          webSocketRunnableHandler = null;
          mainApp.setServer(null);
          mainApp.setWebPort(-1);
          mainApp.setJmriVersion(null);
          mainApp.setPowerState(null);
          mainApp.setJmriTime(null);
          mainApp.setRailroad(null);
          mainApp.setTurnoutList(new HashMap<String, Turnout>());  //empty the list
          mainApp.setRouteList(new HashMap<String, Route>());  //empty the list
          mainApp.setRosterEntryList(new HashMap<String, RosterEntry>());  //empty the list
          mainApp.setThrottleList(new HashMap<String, Throttle>());  //empty the list
          mainApp.setPanelList(new SparseArray<Panel>()); //empty the list
          mainApp.setJmriHeartbeat(Consts.INITIAL_HEARTBEAT);
          if (mainApp.getMainActivity()!=null) {
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, msg);  //forward to activity
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.POWER_STATE_CHANGED);
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.JMRI_TIME_CHANGED);
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.TURNOUT_LIST_CHANGED);
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.ROUTE_LIST_CHANGED);
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.ROSTERENTRY_LIST_CHANGED);
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, MessageType.PANEL_LIST_CHANGED);
          }
          break;
        //simply forward these along to activity
        case MessageType.MESSAGE_LONG:
        case MessageType.MESSAGE_SHORT:
        case MessageType.DISCOVERED_SERVER_LIST_CHANGED:
        case MessageType.CONNECTED:
        case MessageType.POWER_STATE_CHANGED:
        case MessageType.THROTTLE_CHANGED:
        case MessageType.JMRI_TIME_CHANGED:
          if (mainApp.getMainActivity()!=null) {
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, msg);
          } else {
            Log.w(Consts.APP_NAME, "activity not active, message lost (" + msg.what + ")");
          }
          break;
        //simply forward these along to websocket thread
        case MessageType.SPEED_CHANGE_REQUESTED:
        case MessageType.DIRECTION_CHANGE_REQUESTED:
        case MessageType.TURNOUT_CHANGE_REQUESTED:
        case MessageType.LOCO_REQUESTED:
        case MessageType.RELEASE_LOCO_REQUESTED:
          if (webSocketRunnableHandler!=null) {
            mainApp.sendMsg(webSocketRunnableHandler, msg);
          }
          break;
        //forward heartbeat to websocketthread and to activity
        case MessageType.HEARTBEAT:
          if (mainApp.getMainActivity()!=null) {
            mainApp.sendMsg(mainApp.getMainActivity().mainActivityHandler, msg);
          }
          if (webSocketRunnableHandler!=null) {
            mainApp.sendMsg(webSocketRunnableHandler, msg);
          }
          break;

        default:  //log unknown messages, probably indicates missing coding
          Log.w(Consts.APP_NAME, "in PermaFrag_Handler.handleMessage() received unknown message type " + msg.what);
          break;
      }  //end of switch msg.what
      super.handleMessage(msg);
    }
  }

}