package jmri.enginedriver.enginedriver3;

import android.app.Application;
import android.util.Log;

public class EngineDriver3Application extends Application {

	public static final String DEBUG_TAG = "EngineDriver";  //TODO: remove duplicates of this
	
	public FragmentEntry[] EDFrags = new FragmentEntry[5];  //TODO: remove hard-coded length

//	public EngineDriver3Application() {
		// TODO Auto-generated constructor stub
//	}
	@Override
	public void onCreate()  {
		super.onCreate();
	    Log.d(DEBUG_TAG,"in EngineDriver3Application.onCreate()");
		//TODO: move this initialization to application, and load from storage
		EDFrags[0] = new FragmentEntry(); 
		EDFrags[0].setName("About");
		EDFrags[0].setWidth(2);
		EDFrags[1] = new FragmentEntry(); 
		EDFrags[1].setName("Prefs");
		EDFrags[1].setWidth(2);
		EDFrags[2] = new FragmentEntry(); 
		EDFrags[2].setName("Connection");
		EDFrags[2].setWidth(2);
		EDFrags[3] = new FragmentEntry(); 
		EDFrags[3].setName("Throttle");
		EDFrags[3].setWidth(1);
		EDFrags[4] = new FragmentEntry(); 
		EDFrags[4].setName("Web1");
		EDFrags[4].setWidth(3);
		
	}
	

}
