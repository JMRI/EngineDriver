package jmri.enginedriver.enginedriver3;

import java.util.HashMap;

import android.app.Application;
import android.util.Log;


public class EngineDriver3Application extends Application {

//	public ArrayList<FragmentEntry> EDFrags = new ArrayList<FragmentEntry>();
	HashMap<Integer, FragmentEntry> EDFrags;
	
//	public FragmentEntry[] EDFrags = new FragmentEntry[8];  //TODO: replace with ArrayList

//	public EngineDriver3Application() {
		// TODO Auto-generated constructor stub
//	}
	@Override
	public void onCreate()  {
		super.onCreate();
	    Log.d(Consts.DEBUG_TAG,"in EngineDriver3Application.onCreate()");
		//TODO: load from storage, once save is implemented
	    //TODO: set base url from connection
//	    String baseURL = "http://10.10.3.136:1080";
	    String baseURL = "http://jmri.mstevetodd.com:1080";
	    
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
		tFrag.setData(baseURL + "/web/inControl.html");
	    EDFrags.put(fragKey++, tFrag);
	    
		tFrag = new FragmentEntry("Panels", Consts.WEB, 3); 
		tFrag.setData(baseURL + "/web/showPanel.html");
	    EDFrags.put(fragKey++, tFrag);

		tFrag = new FragmentEntry("Trains", Consts.WEB, 2); 
		tFrag.setData(baseURL + "/web/operationsTrains.html");
	    EDFrags.put(fragKey++, tFrag);

	    tFrag = new FragmentEntry("Turnouts", Consts.WEB, 2); 
		tFrag.setData(baseURL + "/web/JMRIMobile.html#type-turnout");
	    EDFrags.put(fragKey++, tFrag);

	}
	

}
