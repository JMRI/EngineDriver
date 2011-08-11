/*Copyright (C) 2011 M. Steve Todd
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

public class web_activity extends Activity {

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
  };

  @Override
  public void onResume() {
	  super.onResume();
	
	    WebView webview = new WebView(this);
	     
	    String url = "file:///android_asset/feature_not_available.html";
	    threaded_application mainapp = (threaded_application) getApplication();
	    if (mainapp.web_server_port != null && mainapp.web_server_port > 0) {
	    	url = "http://" + mainapp.host_ip + ":" +  mainapp.web_server_port;
	    }
	    webview.loadUrl(url);
	    setContentView(webview);
	    
  }


  //Handle pressing of the back button to end this activity
  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
	  if(key==KeyEvent.KEYCODE_BACK)
	  {
	//      mainapp.power_control_msg_handler = null; //clear out pointer to this activity  
	      this.finish();  //end this activity
	      connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
	  }
	  return(super.onKeyDown(key, event));
  };

  @Override
  public boolean onCreateOptionsMenu(Menu menu){
	  MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.web_menu, menu);
	  return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
	  // Handle all of the possible menu actions.
	  Intent in;
	  switch (item.getItemId()) {
	  case R.id.about_menu:
		  in=new Intent().setClass(this, about_page.class);
		  startActivity(in);
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.routes_menu:
		  in=new Intent().setClass(this, routes.class);
		  startActivity(in);
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.power_control_menu:
		  in=new Intent().setClass(this, power_control.class);
		  startActivity(in);
		  connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		  break;
	  case R.id.throttle:
		  this.finish();
		  connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
		  break;
	  case R.id.turnouts:
		  in = new Intent().setClass(this, turnouts.class);
		  startActivity(in);
		  this.finish();
		  connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
		  break;
	  }
	  return super.onOptionsItemSelected(item);
  }
}

