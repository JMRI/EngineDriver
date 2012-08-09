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
import android.widget.TextView;

public class function_settings extends Activity {

//	private static final int GONE = 8;
//	private static final int VISIBLE = 0;
	private threaded_application mainapp;

	//set up label, dcc function, toggle setting for each button
    ArrayList<String> aLbl = new ArrayList<String>();
    ArrayList<Integer> aFnc = new ArrayList<Integer>();

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.function_settings);

        mainapp=(threaded_application)getApplication();  //save pointer to main app
        
        //if settings found, replace default labels and functions with those from file
        if (get_settings_from_file()) {
        	move_settings_to_view();
        }
//        String s = mainapp.roster_function_string_T;
        TextView v=(TextView)findViewById(R.id.fb_copy_labels_from_roster);
        if (mainapp.function_labels_T == null || mainapp.function_labels_T.size()==0) {
            v.setEnabled(false);  //disable button if no roster
        } else { 

        	// suppress popup keyboard until EditText is touched
        	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        	//Set the button callback.
        	Button button=(Button)findViewById(R.id.fb_copy_labels_from_roster);
        	button_listener click_listener=new button_listener();
        	button.setOnClickListener(click_listener);
        	v.setEnabled(true);
        }
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
                  aLbl.add(temp[0]);
                  aFnc.add(Integer.parseInt(temp[1]));
                  settingsFound = true;
                }
              }
            }
          }
        }
        catch (IOException except) { Log.e("settings_activity", "Could not read file "+except.getMessage()); }
        
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
          ViewGroup r;  //row
          int k = 0; //input count
          //loop thru input rows, skipping first (headings)
          for(int i = 1; i < t.getChildCount(); i++) {
            r = (ViewGroup)t.getChildAt(i);

            if (k < aFnc.size()) { 
              //set label and function from saved settings
              ((EditText)r.getChildAt(1)).setText(aFnc.get(k).toString());
              ((EditText)r.getChildAt(0)).setText(aLbl.get(k));
            } else {
              //clear remaining label and function defaults
              ((EditText)r.getChildAt(1)).setText("");
              ((EditText)r.getChildAt(0)).setText("");
            }
            k++;
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

    		ViewGroup t = (ViewGroup) findViewById(R.id.label_func_table); //table
    		ViewGroup r;  //row
    		//loop thru each row, Skipping the first one (the headings)  format is "label:function#"
    		for(int i = 1; i < t.getChildCount(); i++){
    			r = (ViewGroup)t.getChildAt(i);
    			//get the 2 inputs from each row
    			String label = ((EditText)r.getChildAt(0)).getText().toString();
    			label = label.replace("\n", " ");  //remove newlines
    			label = label.replace(":", " ");   //   and colons, as they confuse the save format
    			String sfunc = ((EditText)r.getChildAt(1)).getText().toString();
    			//ignore blank labels and function  
    			if (label.length() > 0 && sfunc.length() > 0) {
    				//verify function is valid number between 0 and 28
    				int func;
    				try {
    					func = Integer.parseInt(sfunc);
    				} catch (Exception e) {
    					// if invalid, don't set func
    					func = -1;
    				}
    				// write out valid items to settings
    				if (func >= 0 && func <= 28 ) {
    					settings_output.format("%s:%s\n", label, func);
    				}
    			}

    		}
	
	          settings_output.flush();
	          settings_output.close();
	          Toast.makeText(getApplicationContext(), "Settings Saved.", Toast.LENGTH_SHORT).show();
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
    	  save_settings();
          this.finish();  //end this activity
          connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
      }
      return(super.onKeyDown(key, event));
    };

}
