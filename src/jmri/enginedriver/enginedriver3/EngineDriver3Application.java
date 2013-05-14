package jmri.enginedriver.enginedriver3;

import android.app.Application;
import android.util.Log;

public class EngineDriver3Application extends Application {

	public static final String DEBUG_TAG = "EngineDriver";  //TODO: remove duplicates of this
	public static final String WEB = "web";
	public static final String DIALOG = "dialog";
	public static final String LIST = "list";
	
	public FragmentEntry[] EDFrags = new FragmentEntry[5];  //TODO: remove hard-coded length

//	public EngineDriver3Application() {
		// TODO Auto-generated constructor stub
//	}
	@Override
	public void onCreate()  {
		super.onCreate();
	    Log.d(DEBUG_TAG,"in EngineDriver3Application.onCreate()");
		//TODO: move this initialization to application, and load from storage
		EDFrags[0] = new FragmentEntry("About", WEB, 2); 
		EDFrags[1] = new FragmentEntry("Prefs", LIST, 2); 
		EDFrags[2] = new FragmentEntry("Connect", LIST, 2); 
		EDFrags[3] = new FragmentEntry("Throttle", LIST, 1); 
		EDFrags[4] = new FragmentEntry("Web1", WEB, 3); 
		
	}
	

}
