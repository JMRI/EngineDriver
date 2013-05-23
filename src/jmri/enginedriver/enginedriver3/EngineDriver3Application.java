package jmri.enginedriver.enginedriver3;

import java.util.HashMap;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;


public class EngineDriver3Application extends Application {

	HashMap<Integer, FragmentEntry> EDFrags;

	//store variables for use by all activities and fragments
	
	//jmri server name (used for both WiThrottle and Web connections)	
	private String _server = "";
	public String getServer() { return _server; }
	public void setServer(String server) { _server = server; }

	//WiThrottle server port #	
	private int _wiThrottlePort = -1; 
	public int getWiThrottlePort() { return _wiThrottlePort; }
	public void setWiThrottlePort(int wiThrottlePort) { _wiThrottlePort = wiThrottlePort; }

	//JMRI web server port #	
	private int _webPort = -1; 
	public int getWebPort() { return _webPort; }
	public void setWebPort(int webPort) { _webPort = webPort; }

//	public EngineDriver3Application() {
		// TODO Auto-generated constructor stub
//	}
	@Override
	public void onCreate()  {
		super.onCreate();
	    Log.d(Consts.DEBUG_TAG,"in EngineDriver3Application.onCreate()");
		//TODO: load from storage, once save is implemented
	    //TODO: set base url from connection
//	    setServer("10.10.3.131");
//	    setServer("192.168.1.247");
	    setServer("jmri.mstevetodd.com");
	    setWiThrottlePort(44444);
	    setWebPort(1080);
	    
	    //initialize the fragment list, for now, key is 0-n, cannot leave "holes"
	    int fragKey = 0;
	    EDFrags = new HashMap<Integer, FragmentEntry>();
		EDFrags.put(fragKey++, new FragmentEntry("Prefs", Consts.PREFS, 2));
		
	    FragmentEntry tFrag;
	    tFrag = new FragmentEntry("About", Consts.WEB, 2);
		tFrag.setData("file:///android_asset/about_page.html");
	    EDFrags.put(fragKey++, tFrag);
	    
		EDFrags.put(fragKey++, new FragmentEntry("Connect", Consts.CONNECT, 2));
		
		tFrag = new FragmentEntry("Throttle", Consts.WEB, 2); 
		tFrag.setData("/web/inControl.html");
	    EDFrags.put(fragKey++, tFrag);
	    
		tFrag = new FragmentEntry("Panels", Consts.WEB, 3); 
		tFrag.setData("/web/showPanel.html");
	    EDFrags.put(fragKey++, tFrag);

		tFrag = new FragmentEntry("Trains", Consts.WEB, 2); 
		tFrag.setData("/web/operationsTrains.html");
	    EDFrags.put(fragKey++, tFrag);

	    tFrag = new FragmentEntry("Turnouts", Consts.WEB, 2); 
		tFrag.setData("/web/JMRIMobile.html#type-turnout");
	    EDFrags.put(fragKey++, tFrag);

	}


}
