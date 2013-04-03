/* Copyright (C) 2012 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.*;

import android.text.method.TextKeyListener;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

public class function_settings extends Activity{

	private threaded_application mainapp;
	private static boolean orientationChange = false;
	private static boolean settingsChange = false;

	//set up label, dcc function, toggle setting for each button
    private static ArrayList<String> aLbl = new ArrayList<String>();
    private static ArrayList<Integer> aFnc = new ArrayList<Integer>();

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp=(threaded_application)getApplication();  //save pointer to main app
       	setContentView(R.layout.function_settings);

       	if(!orientationChange) {			//if not an orientation change then settings array need initialization
       		initSettings();					//set settings array from the settings file
       		settingsChange = false;			//indicate settings array matches file 
       	}
       	else {								//else it is an orientation change and settings array is current
	       	orientationChange = false;
       	}
   		move_settings_to_view();			//copy settings array to view
       	
    	// suppress popup keyboard until EditText is touched
    	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Button b=(Button)findViewById(R.id.fb_copy_labels_from_roster);
    	if (mainapp.function_labels_T == null || mainapp.function_labels_T.size()==0) {
            b.setEnabled(false);  //disable button if no roster
        } 
    	else { 
        	//Set the button callback.
        	button_listener click_listener=new button_listener();
        	b.setOnClickListener(click_listener);
        	b.setEnabled(true);
        }
     }

    @Override
    public void onResume() {
  	  super.onResume();
  	  mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
    }

    @Override
    public void onDestroy() {
		Log.d("Engine_Driver","function_settings.onDestroy() called");
    	super.onDestroy();
    }
  
    @Override
    public void onSaveInstanceState(Bundle saveState) {		//orientation change
    	move_view_to_settings();		//update settings array so onCreate can use it to initialize
    	orientationChange = true;
    }

   
  	//build the arrays from the function_settings file
    //function_labels_default was loaded from settings file by TA
    //(and updated saveSettings() when required) so just copy it
    void initSettings() {
		aLbl.clear();
		aFnc.clear();
		//read settings into local arrays
		for (Integer f : mainapp.function_labels_default.keySet()) {
			aFnc.add(f);
			aLbl.add(mainapp.function_labels_default.get(f));
        }
    }

    //replace arrays using data from roster entry (called by button)
    void get_settings_from_roster() {
    	aLbl.clear();
    	aFnc.clear();
    	for (Integer f : mainapp.function_labels_T.keySet()) {
   			aLbl.add(mainapp.function_labels_T.get(f));
   			aFnc.add(f);
    	}
    }

    //take data from arrays and update the editing view
    void move_settings_to_view() {
	  ViewGroup t = (ViewGroup) findViewById(R.id.label_func_table); //table
	  //loop thru input rows, skipping first (headings)
	  int ndx = 0;
	  for(int i = 1; i < t.getChildCount(); i++) {
	    ViewGroup r = (ViewGroup)t.getChildAt(i);
	    //move to next non-blank array entry if it exists
	    while(ndx < aFnc.size() && aLbl.get(ndx).length() == 0)
	    	ndx++;
	    if(ndx < aFnc.size()) {
	    	((EditText)r.getChildAt(0)).setText(aLbl.get(ndx));
	    	((EditText)r.getChildAt(1)).setText(aFnc.get(ndx).toString());
	    	ndx++;
		}
		else {
//			
// work around for known EditText bug - see http://code.google.com/p/android/issues/detail?id=17508
//			((EditText)r.getChildAt(0)).setText("");
//			((EditText)r.getChildAt(1)).setText("");
			TextKeyListener.clear(((EditText)r.getChildAt(0)).getText());
			TextKeyListener.clear(((EditText)r.getChildAt(1)).getText());
		}
	  }
   }

    //Save the valid function labels in the settings array
    void move_view_to_settings() {
		ViewGroup t = (ViewGroup) findViewById(R.id.label_func_table); //table
		ViewGroup r;  //row
		//loop thru each row, Skipping the first one (the headings)  format is "label:function#"
		int ndx = 0;
		for(int i = 1; i < t.getChildCount(); i++) {
			r = (ViewGroup)t.getChildAt(i);
			//get the 2 inputs from each row
			String label = ((EditText)r.getChildAt(0)).getText().toString();
			label = label.replace("\n", " ");  //remove newlines
			label = label.replace(":", " ");   //   and colons, as they confuse the save format
			String sfunc = ((EditText)r.getChildAt(1)).getText().toString();
			if(label.length() > 0 && sfunc.length() > 0) {
				//verify function is valid number between 0 and 28
				int func;
				try {
					func = Integer.parseInt(sfunc);
					if(func >= 0 && func <= 28) {
						if(aFnc.size() <= ndx) {
							aLbl.add(label);
							aFnc.add(func);
							settingsChange = true;
						}
						else if(!label.equals(aLbl.get(ndx)) || func != aFnc.get(ndx)) {
							aLbl.set(ndx, label);
							aFnc.set(ndx, func);
							settingsChange = true;
						}
						ndx++;
					}
				} 
				catch (Exception e) {
				}
			}
		}
		
		while(aFnc.size() > ndx) {			//if array remains then trim it
			aFnc.remove(ndx);
			aLbl.remove(ndx);
			settingsChange = true;
		}
    }
    
   	public class button_listener implements View.OnClickListener
    {
      public void onClick(View v)
      {
    	  get_settings_from_roster();
    	  move_settings_to_view();
      };
    }

    //Handle pressing of the back button to save settings
    @Override
    public boolean onKeyDown(int key, KeyEvent event)
    {
      if(key==KeyEvent.KEYCODE_BACK) {
    	  move_view_to_settings();		//sync settings array to view
    	  if(settingsChange) {
    		saveSettings();				//save function labels to file
    	  }
    	  orientationChange = false;			//static - should already be false
    	  this.finish();  //end this activity
/*          connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
 */
      }
      return(super.onKeyDown(key, event));
    };

	//save function and labels to file
    void saveSettings() {
        //Save the valid function labels to the settings.txt file.
    	File sdcard_path=Environment.getExternalStorageDirectory();
    	File settings_file=new File(sdcard_path, "engine_driver/function_settings.txt");
    	PrintWriter settings_output;
    	String errMsg = "";
    	try {
    		settings_output=new PrintWriter(settings_file);
    		mainapp.function_labels_default.clear();
    		for(int i = 0; i < aFnc.size(); i++) {
    			String label = aLbl.get(i);
    			if(label.length() > 0) {
    				Integer fnc = aFnc.get(i);
    				settings_output.format("%s:%s\n", label, fnc);
    				mainapp.function_labels_default.put(fnc, label);
    			}
    		}
			settings_output.flush();
	        settings_output.close();
        }
        catch(IOException except) {
          errMsg = except.getMessage();
          Log.e("settings_activity", "Error creating a PrintWriter, IOException: "+errMsg);
        }
		if(errMsg.length() != 0)
			Toast.makeText(getApplicationContext(), "Save Settings Failed." +errMsg, Toast.LENGTH_LONG).show();
		else
			Toast.makeText(getApplicationContext(), "Settings Saved.", Toast.LENGTH_SHORT).show();
	}
    	
    // helper app to initialize statics (in case GC has not run since app last shutdown)
    // call before instantiating any instances of class
    public static void initStatics() {
    	orientationChange = false;
    	settingsChange = false;
        aLbl.clear();
        aFnc.clear();
    }
}
