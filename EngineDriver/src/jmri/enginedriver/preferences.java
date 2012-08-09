/*Copyright (C) 2012 M. Steve Todd
  mstevetodd@enginedriver.rrclubs.org

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package jmri.enginedriver;

import java.util.Random;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.view.KeyEvent;

public class preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	  /** Called when the activity is first created. */
	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);
	  }
	  
	  @Override
	  protected void onResume() {
	      super.onResume();

	      // Set up a listener whenever a key changes            
	      getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	  }

	  @Override
	  protected void onPause() {
	      super.onPause();

	      // Unregister the listener            
	      getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
	  }

	  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		  if (key.equals("throttle_name_preference"))  {
			  String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
			  String currentValue = sharedPreferences.getString(key, defaultName).trim();
			  //if new name is blank or the default name, make it unique
			  if (currentValue.equals("") || currentValue.equals(defaultName)) {
				  String deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
				  if (deviceId != null && deviceId.length() >=4) {
					  deviceId = deviceId.substring(deviceId.length() - 4);
				  } else {
			        	Random rand = new Random();
			        	deviceId = String.valueOf(rand.nextInt(9999));  //use random string
				  }
				  String uniqueDefaultName = defaultName + " " + deviceId;
				  sharedPreferences.edit().putString(key, uniqueDefaultName).commit();  //save new name to prefs
			  }


		  }

	  }
	  //Handle pressing of the back button to end this activity
	  @Override
	  public boolean onKeyDown(int key, KeyEvent event) {
	  if(key==KeyEvent.KEYCODE_BACK)
	  {
	      this.finish();  //end this activity
	      connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
	  }
	  return(super.onKeyDown(key, event));
	};
}
