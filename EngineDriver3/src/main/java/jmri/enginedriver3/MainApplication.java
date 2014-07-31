package jmri.enginedriver3;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Application;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

public class MainApplication extends Application {

	protected HashMap<Integer, DynaFragEntry> dynaFrags;  //this is built in this onCreate()  TODO:convert to get/set

    //store variables for use by the activity and all fragments
    private MainActivity _mainActivity;
    public MainActivity getMainActivity() {return _mainActivity;}
    public void setMainActivity(MainActivity in_mainActivity) {this._mainActivity = in_mainActivity;}

    private ArrayList<HashMap<String, String> > _discoveredServersList;
    public ArrayList<HashMap<String, String>> getDiscoveredServersList() {return _discoveredServersList;}
    public void setDiscoveredServersList(ArrayList<HashMap<String, String>> in_discoveredServersList) {
        this._discoveredServersList = in_discoveredServersList;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.DISCOVERED_SERVER_LIST_CHANGED); //announce the change
    }

    private ArrayList<HashMap<String, String> > _turnoutsList;
    public ArrayList<HashMap<String, String>> getTurnoutsList() {return _turnoutsList;}
    public void setTurnoutsList(ArrayList<HashMap<String, String>> in_turnoutsList) {
        this._turnoutsList = in_turnoutsList;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.TURNOUTS_LIST_CHANGED); //announce the change
    }

    //jmri server name (used for both WiThrottle and Web connections)
	private String _server = null;
	public String getServer() { return _server; }
	public void setServer(String server) { _server = server; }

	//WiThrottle server port #	
//	private int _wiThrottlePort = -1;
//	public int getWiThrottlePort() { return _wiThrottlePort; }
//	public void setWiThrottlePort(int wiThrottlePort) { _wiThrottlePort = wiThrottlePort; }

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
        this._powerState = in_powerState;
        if (_mainActivity!=null) sendMsg(_mainActivity.mainActivityHandler, MessageType.POWER_STATE_CHANGED); //announce it
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
	    Log.d(Consts.DEBUG_TAG,"in ED3Application.onCreate()");

        //initialize the temporary fragment list, for now, key is 0-n, cannot leave "holes"
        //TODO: replace this with restore from database or somesuch, maybe in ED3Activity.onCreate()
        int fragKey = 0;
        dynaFrags = new HashMap<Integer, DynaFragEntry>();
        dynaFrags.put(fragKey++, new DynaFragEntry("Prefs", Consts.PREFS, 2));
        DynaFragEntry tEntry;
        tEntry = new DynaFragEntry("About", Consts.WEB, 2);
        tEntry.setData("file:///android_asset/about_page.html");
        dynaFrags.put(fragKey++, tEntry);
        dynaFrags.put(fragKey++, new DynaFragEntry("Connect", Consts.CONNECT, 2));
        tEntry = new DynaFragEntry("Throttle", Consts.WEB, 2);
        tEntry.setData("/web/webThrottle.html");
        dynaFrags.put(fragKey++, tEntry);

        tEntry = new DynaFragEntry("Panels", Consts.WEB, 3);
        tEntry.setData("/panel/");
        dynaFrags.put(fragKey++, tEntry);

        tEntry = new DynaFragEntry("Trains", Consts.WEB, 2);
        tEntry.setData("/operations/trains");
        dynaFrags.put(fragKey++, tEntry);

        tEntry = new DynaFragEntry("Turnouts", Consts.TURNOUT, 2);
        dynaFrags.put(fragKey++, tEntry);

        Log.d(Consts.DEBUG_TAG,"end of ED3Application.onCreate()");

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
                Log.d(Consts.DEBUG_TAG, "failed to send message of type " + msgType + " to " + h.getClass());
            }
            if(!sent)
                msg.recycle();
        }
        return sent;
    }




}
