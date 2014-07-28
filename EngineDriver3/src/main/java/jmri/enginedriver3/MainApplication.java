package jmri.enginedriver3;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Application;
import android.os.Message;
import android.os.Handler;
import android.util.Log;


public class MainApplication extends Application {

	protected HashMap<Integer, DynaFragEntry> dynaFrags;

    //store variables for use by the activity and all fragments

    public ArrayList<HashMap<String, String> > discoveredServersList;

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
	public void setWebPort(int webPort) { _webPort = webPort; }

    public String getJmriTime() {return _jmriTime;}
    public void setJmriTime(String jmriTime) {this._jmriTime = jmriTime;}
    private String _jmriTime = null;  //will be set by JmriWebSocket

    public String getPowerState() {return powerState;}
    public void setPowerState(String powerState) {this.powerState = powerState;}
    private String powerState = null;  //will be set by JmriWebSocket

    public String getJmriVersion() {return _jmriVersion;}
    public void setJmriVersion(String jmriVersion) {this._jmriVersion = jmriVersion;}
    private String _jmriVersion = null;  //will be set by JmriWebSocket


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

        tEntry = new DynaFragEntry("Turnouts", Consts.LIST, 1);
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
            }
            if(!sent)
                msg.recycle();
        }
        return sent;
    }




}
