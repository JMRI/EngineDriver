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
import java.io.*;

import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

public class function_settings extends Activity {

	private threaded_application mainapp;
	private static Boolean orientationChange = false;
	private static Boolean settingsChange = false;

	//set up label, dcc function, toggle setting for each button
    private static ArrayList<String> aLbl = new ArrayList<String>();
    private static ArrayList<Integer> aFnc = new ArrayList<Integer>();

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp=(threaded_application)getApplication();  //save pointer to main app
       	setContentView(R.layout.function_settings);

       	if(!orientationChange) { 			//if not an orientation change 
	       	if(get_settings_from_file()) {  //if settings file exists, load labels and functions
	       		move_settings_to_view();    //and copy setting array to view
	       		settingsChange = false;		//indicate settings file is current
	       	}
	       	else {
	       		move_view_to_settings();	//copy default values to settings array
	       		settingsChange = true;		//indicate settings file needs to be stored
	       	}
       	}
       	else {								//else it is an orientation change and settings array is current
       		move_settings_to_view();		//so copy settings array to view
	       	orientationChange = false;
       	}
       	
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
    	super.onDestroy();
    }
  
    @Override
    public void onSaveInstanceState(Bundle saveState) {		//orientation change
    	move_view_to_settings();		//update settings array so onCreate can use it to initialize
    	orientationChange = true;
    }

   
  	//build the arrays from the function_settings file
    boolean get_settings_from_file() {
    	boolean settingsFound = false; //set if any user settings found
        try
        {
          File sdcard_path=Environment.getExternalStorageDirectory();
          if(sdcard_path.canWrite())
          {
            //First, determine if the engine_driver directory exists. If not, create it.
            File engine_driver_dir=new File(sdcard_path, "engine_driver");
            if(!engine_driver_dir.exists()) { engine_driver_dir.mkdir(); }

            if(engine_driver_dir.exists() && engine_driver_dir.isDirectory())
            {
              //TODO: Fix things if the path is not a directory.
              File settings_file=new File(engine_driver_dir, "function_settings.txt");
              if(settings_file.exists())
              {
                BufferedReader settings_reader=new BufferedReader(new FileReader(settings_file));
                //read settings into local arrays
                while(settings_reader.ready())
                {
	              String line=settings_reader.readLine();
	              String temp[] = line.split(":");
	              String label = temp[0];
	              if(label.length() > 0) {
	                  int func;
	                  try {
	                	  func = Integer.parseInt(temp[1]);
	                	  if(func >= 0 || func <= 28) {
	                    	  if(!settingsFound) {		//if this is the first valid entry, clear array
	                    		  aLbl.clear();
	                    		  aFnc.clear();
	                        	  settingsFound = true;
	                    	  }
	                    	  aLbl.add(label);			// add data from file to array
	                    	  aFnc.add(func);
	                      }
	                  }
	                  catch(Exception e) {
	                  }
	              }
                }
              }
            }
          }
        }
        catch (IOException except) { 
        	Log.e("settings_activity", "Could not read file "+except.getMessage()); 
        }
        return settingsFound;
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
			((EditText)r.getChildAt(0)).setText("");
			((EditText)r.getChildAt(1)).setText("");
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

    void save_settings() {
        //Save the valid function labels to the settings.txt file.
    	File sdcard_path=Environment.getExternalStorageDirectory();
    	File settings_file=new File(sdcard_path, "engine_driver/function_settings.txt");
    	PrintWriter settings_output;
    	try
    	{
    		settings_output=new PrintWriter(settings_file);
    		for(int i = 0; i < aFnc.size(); i++) {
    			String label = aLbl.get(i);
    			if(label.length() > 0)
    				settings_output.format("%s:%s\n", label, aFnc.get(i));
    		}
			settings_output.flush();
	        settings_output.close();
        }
        catch(IOException except)
        {
          Log.e("settings_activity", "Error creating a PrintWriter, IOException: "+except.getMessage());
          Toast.makeText(getApplicationContext(), "Save Settings Failed." +except.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    //Handle pressing of the back button to save settings
    @Override
    public boolean onKeyDown(int key, KeyEvent event)
    {
      if(key==KeyEvent.KEYCODE_BACK)
      {
    	  move_view_to_settings();		//sync settings array to view
    	  if(settingsChange) {
    		  save_settings();
    		  Toast.makeText(getApplicationContext(), "Settings Saved.", Toast.LENGTH_SHORT).show();
    	  }
    	  orientationChange = false;			//static - should already be false
    	  aLbl.clear();							//statics - free array memory
    	  aFnc.clear();
          this.finish();  //end this activity
          connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
      }
      return(super.onKeyDown(key, event));
    };

    // helper app for TA to initial statics (in case GC has not run since app last shutdown)
    // call before instantiating any instances of class
    public static void initStatics() {
    	orientationChange = false;
    	settingsChange = false;
        aLbl.clear();
        aFnc.clear();
    }
}
