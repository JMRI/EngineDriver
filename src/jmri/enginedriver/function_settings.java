package jmri.enginedriver;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.*;
import java.io.*;

import jmri.enginedriver.engine_driver.function_button_touch_listener;
import jmri.enginedriver.select_loco.button_listener;

import android.util.Log;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.MotionEvent;
import android.os.Message;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.content.res.Resources;


public class function_settings extends Activity {

	private static final int GONE = 8;
	private static final int VISIBLE = 0;
	private threaded_application mainapp;

	//set up label, dcc function, toggle setting for each button
    ArrayList<String> aLbl = new ArrayList<String>();
    ArrayList<Integer> aFnc = new ArrayList<Integer>();
    ArrayList<Boolean> aTgl = new ArrayList<Boolean>();

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
        threaded_application app=(threaded_application)getApplication();
        String s = mainapp.roster_function_string_1;
        TextView v=(TextView)findViewById(R.id.fb_copy_labels_from_roster);
        if (s == null) {
          v.setVisibility(GONE);  //hide button if no roster
        } else { 
          //Set the button callback.
          Button button=(Button)findViewById(R.id.fb_copy_labels_from_roster);
          button_listener click_listener=new button_listener();
          button.setOnClickListener(click_listener);
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
                  aTgl.add(Boolean.parseBoolean(temp[2]));
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

        String s = mainapp.roster_function_string_1;
        s = s.replace("]\\[", "`");  //handle some odd stuff in format of roster string
        String ra[] = s.split("`");
        aLbl.clear();
        aFnc.clear();
        aTgl.clear();
    	//read settings into arrays, (skip first entry, which is length)
        for(int i = 1; i < ra.length; i++) {
                  aLbl.add(ra[i]);
                  aFnc.add(i - 1);
                  if (i == 3 ) {     
                    aTgl.add(false);   //default "horn" to momentary
                  } else {
                	aTgl.add(true);  //others to sticky, not being sent from withrottle
                  }
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
              ((CheckBox)r.getChildAt(2)).setChecked(aTgl.get(k));
            } else {
              //clear remaining label and function defaults
              ((EditText)r.getChildAt(1)).setText("");
              ((EditText)r.getChildAt(0)).setText("");
              ((CheckBox)r.getChildAt(2)).setChecked(false);
            }
            k++;
          }

   }

    public class button_listener implements View.OnClickListener
    {
      public void onClick(View v)
      {
//          String s = mainapp.roster_function_string_1;
//    	  Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
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
          //loop thru each row, Skipping the first one (the headings)  format is "label:function#:toggle"
          for(int i = 1; i < t.getChildCount(); i++){
              r = (ViewGroup)t.getChildAt(i);
              //get the 3 inputs from each row
              String label = ((EditText)r.getChildAt(0)).getText().toString();
              label = label.replace("\n", " ");  //remove newlines
              label = label.replace(":", " ");   //   and colons, as they confuse the save format
              String sfunc = ((EditText)r.getChildAt(1)).getText().toString();
              boolean toggle = ((CheckBox)r.getChildAt(2)).isChecked();
              //ignore blank labels and function  
              if (label.length() > 0 && sfunc.length() > 0) {
                //verify function is valid number between 0 and 99
            	int func = -1;
                try {
                    func = Integer.parseInt(sfunc);
                } catch (NumberFormatException e) {
                // if invalid, don't set func
                }
            	// write out valid items to settings
                if (func >= 0 && func <= 99 ) {
                  settings_output.format("%s:%s:%s\n", label, func, toggle);
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
          Toast.makeText(getApplicationContext(), "Save Settings Failed.", Toast.LENGTH_LONG).show();
        }
    }
    
    //Handle pressing of the back button to save settings
    @Override
    public boolean onKeyDown(int key, KeyEvent event)
    {
      if(key==KeyEvent.KEYCODE_BACK)
      {
    	  save_settings();
      }
      return(super.onKeyDown(key, event));
    };

}
