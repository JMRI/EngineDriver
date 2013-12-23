/*Copyright (C) 2013 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.widget.SimpleAdapter;
import android.widget.ListView;

import java.io.File;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.IOException;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;

import java.io.FileReader;

import android.widget.EditText;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.TextView.OnEditorActionListener;

import java.io.PrintWriter;

import jmri.enginedriver.Consist;
import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import jmri.jmrit.roster.RosterEntry;

public class select_loco extends Activity {
	private static final int GONE = 8;
	private static final int VISIBLE = 0;

	ArrayList<HashMap<String, String>> recent_engine_list;
	private SimpleAdapter recent_list_adapter;
	ArrayList<HashMap<String, String>> roster_list;
	private RosterSimpleAdapter roster_list_adapter;

	private ArrayList<Integer> engine_address_list;
	private ArrayList<Integer> address_size_list; // Look at address_type.java

	private int engine_address;
	private int address_size;
	private String sWhichThrottle = "T"; // "T" or "S" + roster name
	private int result;

	private threaded_application mainapp; // hold pointer to mainapp

	private SharedPreferences prefs;
	private String default_address_length;
	private Menu SMenu;

	// populate the on-screen roster view from global hashmap
	public void refresh_roster_list() {
		// clear and rebuild
		roster_list.clear();
		if (((mainapp.roster_entries != null)  // add roster and consist entries if any defined
				&& (mainapp.roster_entries.size() > 0))
				|| ((mainapp.consist_entries != null)
						&& (mainapp.consist_entries.size() > 0))) {

			//put roster entries into screen list
			if (mainapp.roster_entries != null) {
				for (String rostername : mainapp.roster_entries.keySet()) {
					// put key and values into temp hashmap
					HashMap<String, String> hm = new HashMap<String, String>();
					hm.put("roster_name", rostername);
					hm.put("roster_address", mainapp.roster_entries.get(rostername));
					hm.put("roster_entry_type", "loco");
					//add icon if url set
					if ((mainapp.roster != null) && (mainapp.roster.get(rostername)!=null) && (mainapp.roster.get(rostername).getIconPath()!=null)) {
						hm.put("roster_icon", mainapp.roster.get(rostername).getIconPath() + "?maxHeight=52");  //include sizing instructions	
					}
					// add temp hashmap to list which view is hooked to
					roster_list.add(hm);
				}
			}

			//add consist entries to screen list
			if (mainapp.consist_entries != null) {
				Set<String> ces = mainapp.consist_entries.keySet();
				for (String consistname : ces) {
					// put key and values into temp hashmap
					HashMap<String, String> hm = new HashMap<String, String>();
					hm.put("roster_name", mainapp.consist_entries.get(consistname));
					hm.put("roster_address", consistname);
					hm.put("roster_entry_type", "consist");

					// add temp hashmap to list which view is hooked to
					roster_list.add(hm);

				} // for consistname
			} //if consist_entries not null

			Comparator<HashMap<String, String>> comparator = new Comparator<HashMap<String, String>>() {
				@Override
				public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
					return arg0.get("roster_name").compareToIgnoreCase(arg1.get("roster_name"));
				}
			};
			Collections.sort(roster_list, comparator);

			roster_list_adapter.notifyDataSetChanged();
			View v = findViewById(R.id.roster_list_heading);
			v.setVisibility(VISIBLE);
			v = findViewById(R.id.roster_list);
			v.setVisibility(VISIBLE);

		} 
		else { // hide roster section if nothing to show
			View v = findViewById(R.id.roster_list_heading);
			v.setVisibility(GONE);
			v = findViewById(R.id.roster_list);
			v.setVisibility(GONE);
		} // if roster_entries not null
	}

	// lookup and set values of various text labels
	private void set_labels() {

		TextView v = (TextView) findViewById(R.id.throttle_name_header);
		// show throttle name
		String s = "Throttle Name: "
			+ prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
		v.setText(s);

		// format and show currently selected locos, and hide or show Release
		// buttons
		v = (TextView) findViewById(R.id.sl_loco_T);
		if (mainapp.consistT.toString().length() > 7) {  //shrink text if long
			v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
		} else {
			v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		}
		v.setText(mainapp.consistT.toString());
		Button b = (Button) findViewById(R.id.sl_release_T);
		if (mainapp.consistT.isEmpty()) {
			b.setEnabled(false);
		} else {
			b.setEnabled(true);
		}

		v = (TextView) findViewById(R.id.sl_loco_S);
		if (mainapp.consistS.toString().length() > 7) {  //shrink text if long
			v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
		} else {
			v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		}
		v.setText(mainapp.consistS.toString());
		b = (Button) findViewById(R.id.sl_release_S);
		if (mainapp.consistS.isEmpty()) {
			b.setEnabled(false);
		} else {
			b.setEnabled(true);
		}

		v = (TextView) findViewById(R.id.sl_loco_G);
		if (mainapp.consistG.toString().length() > 7) {  //shrink text if long
			v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
		} else {
			v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		}
		v.setText(mainapp.consistG.toString());
		b = (Button) findViewById(R.id.sl_release_G);
		if (mainapp.consistG.isEmpty()) {
			b.setEnabled(false);
		} else {
			b.setEnabled(true);
		}

		// only show loco text and release buttons for allowed # of locos
		String numThrot = prefs.getString("NumThrottle", getResources().getString(R.string.prefNumOfThrottlesDefault));
		if(numThrot.matches("One") || numThrot.matches("Two")) {
			View rlv = (View) findViewById(R.id.sl_loco_G);
			rlv.setVisibility(GONE);
			rlv = (View) findViewById(R.id.sl_release_G);
			rlv.setVisibility(GONE);
		}
		if(numThrot.matches("One")) {
			View rlv = (View) findViewById(R.id.sl_loco_S);
			rlv.setVisibility(GONE);
			rlv = (View) findViewById(R.id.sl_release_S);
			rlv.setVisibility(GONE);
		}
		
		// hide the recent locos list if selected in prefs
		boolean hrl = prefs.getBoolean("hide_recent_locos_preference", 
				getResources().getBoolean(R.bool.prefHideRecentLocosDefaultValue)); 
		if (hrl) {
			View rlv = (View) findViewById(R.id.recent_engines_heading);
			rlv.setVisibility(GONE);
			rlv = (View) findViewById(R.id.engine_list_wrapper);
			rlv.setVisibility(GONE);
			rlv = (View) findViewById(R.id.clear_Loco_List_button);
			rlv.setVisibility(GONE);
		}
		if(SMenu != null)
		{
			mainapp.displayEStop(SMenu);
		}

		//		String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
		//		setTitle(getApplicationContext().getResources().getString(R.string.app_name_select_loco) + "    |    Throttle Name: " + 
		//				prefs.getString("throttle_name_preference", defaultName));

		refresh_roster_list();
	}

	// Handle messages from the communication thread back to this thread
	// (responses from withrottle)
	@SuppressLint("HandlerLeak")
	class select_loco_handler extends Handler {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case message_type.ROSTER_UPDATE:
				//refresh labels when any roster response is received
				roster_list_adapter.notifyDataSetChanged();
				set_labels();
				break;
			case message_type.RESPONSE:
				String response_str = msg.obj.toString();
				if (response_str.length() >= 1) {
					char com1 = response_str.charAt(0);

					//refresh labels when any roster response is received
					if (com1 == 'R') {
						roster_list_adapter.notifyDataSetChanged();
						set_labels();
					}
				}
				break;
			case message_type.WIT_CON_RETRY:
				roster_list_adapter.notifyDataSetChanged();
				set_labels();
				break;
			case message_type.DISCONNECT:
				end_this_activity();
				break;
			};
		}
	}

	// request release of specified throttle
	void release_loco(char whichThrottle) {
		if(whichThrottle == 'T')
		{
			mainapp.consistT.release();
		}
		else if(whichThrottle == 'S')
		{
			mainapp.consistS.release();
		}
		else
		{
			mainapp.consistG.release();
		}

		mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, "",(int) whichThrottle); // pass T, S or G in message 
	}

	boolean saveUpdateList;			// save value across ConsistEdit activity 
	boolean newEngine;				// save value across ConsistEdit activity

	void acquire_engine(boolean bUpdateList) {
		char whichThrottle = sWhichThrottle.charAt(0);
		String addr = (address_size== address_type.LONG ? "L" : "S") + engine_address;
		Loco l = new Loco(addr);
		l.setDesc(mainapp.getRosterNameFromAddress(l.toString()));	//use rosterName if present
		Consist consist;

		if(whichThrottle == 'T')
		{
			consist = mainapp.consistT;
		}
		else if(whichThrottle == 'S')
		{
			consist = mainapp.consistS;
		}
		else
		{
			consist = mainapp.consistG;
		}

		if(sWhichThrottle.length() > 1)				// add roster selection info if present
			addr += "<;>" + sWhichThrottle.substring(1);
		if(consist.size() == 0) {					// just this loco so tell WiT and exit

			consist.add(l);

			mainapp.sendMsg(mainapp.comm_msg_handler, message_type.LOCO_ADDR, addr, (int) whichThrottle);
			updateRecentEngines(bUpdateList);

			result = RESULT_OK;
			end_this_activity();
		}
		else {										// consist exists, bring up editor
			if(prefs.getBoolean("drop_on_acquire_preference", true) == false)
			{
				newEngine = (consist.getLoco(addr) == null);


				if(newEngine) {
					consist.add(l);
					mainapp.sendMsg(mainapp.comm_msg_handler, message_type.LOCO_ADDR, addr, (int) whichThrottle);


					saveUpdateList = bUpdateList;
					Intent consistEdit = new Intent().setClass(this, ConsistEdit.class);
					consistEdit.putExtra("address", addr);
					consistEdit.putExtra("whichThrottle", whichThrottle);
					startActivityForResult(consistEdit, 0);
					connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
				}
			}
			else
			{

				release_loco(whichThrottle);

				if(whichThrottle == 'T')
				{
					consist = mainapp.consistT;
				}
				else if(whichThrottle == 'S')
				{
					consist = mainapp.consistS;
				}
				else
				{
					consist = mainapp.consistG;
				}

				consist.add(l);
				consist.setLeadAddr(addr);
				mainapp.sendMsg(mainapp.comm_msg_handler, message_type.LOCO_ADDR, addr, (int) whichThrottle);
				updateRecentEngines(bUpdateList);

				result = RESULT_OK;
				end_this_activity();
			}
		}
	}

	//handle return from ConsistEdit
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK) {						// something in consist was changed
			if(newEngine) {
				updateRecentEngines(saveUpdateList);
			}
			result = RESULT_FIRST_USER;						//tell Throttle to update loco directions
		}
		end_this_activity();
	}

	void updateRecentEngines(boolean bUpdateList) {
		//if not updating list or no SD Card present then nothing else to do
		if(!bUpdateList || !android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
			return;
		// Save the engine list to the recent_engine_list.txt file
		File sdcard_path = Environment.getExternalStorageDirectory();
		File connections_list_file = new File(sdcard_path,
		"engine_driver/recent_engine_list.txt");
		PrintWriter list_output;
		String smrl = prefs.getString("maximum_recent_locos_preference", ""); //retrieve pref for max recent locos to show  
		if (smrl.equals("")) { //if no value or entry removed, set to default
			smrl = getApplicationContext().getResources().getString(R.string.prefMaximumRecentLocosDefaultValue);
		}
		try {
			int mrl = Integer.parseInt(smrl);
			list_output = new PrintWriter(connections_list_file);
			if(mrl > 0) {
				// Add this engine to the head of recent engines list.
				mrl--;
				list_output.format("%d:%d\n", engine_address, address_size);
				for (int i = 0; i < engine_address_list.size() && mrl > 0; i++) {
					if (engine_address != engine_address_list.get(i) || address_size != address_size_list.get(i)) {
						list_output.format("%d:%d\n", engine_address_list.get(i), address_size_list.get(i));
						mrl--;
					}
				}
			}
			list_output.flush();
			list_output.close();
		} 
		catch (IOException except) {
			Log.e("Engine_Driver",
					"select_loco - Error creating a PrintWriter, IOException: "
					+ except.getMessage());
		}
	};

	/***
	void getConsistLocoDirection() {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setIcon(android.R.drawable.ic_dialog_alert); 
		b.setTitle("Direction relative to Lead engine");
		b.setMessage(R.string.exit_text);
		forwardDirection = false;
		b.setCancelable(true);
		b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				forwardDirection = true;
				Intent intent = new Intent();
				setResult(1,intent);
			}
		});
		b.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				forwardDirection = true;
				Intent intent = new Intent();
				setResult(0,intent);
			}
		});

		AlertDialog alert = b.create();
		alert.show();
//		b.create().show();
	}
/****/

	public class button_listener implements View.OnClickListener {
		public void onClick(View v) {
			EditText entry = (EditText) findViewById(R.id.loco_address);
			try {
				engine_address = Integer.valueOf(entry.getText().toString());
			} catch (NumberFormatException e) {
				Toast.makeText(getApplicationContext(), "ERROR - Please enter a valid DCC address.\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
				return;
			}
			Spinner spinner = (Spinner) findViewById(R.id.address_length);
			address_size = spinner.getSelectedItemPosition();
			acquire_engine(true);
		};
	}

	public class release_button_listener_T implements View.OnClickListener {
		public void onClick(View v) {
			release_loco('T');
			end_this_activity();
		};
	}

	public class release_button_listener_S implements View.OnClickListener {
		public void onClick(View v) {
			release_loco('S');
			end_this_activity();
		};
	}

	public class release_button_listener_G implements View.OnClickListener {
		public void onClick(View v) {
			release_loco('G');
			end_this_activity();
		};
	}

	public class engine_item implements AdapterView.OnItemClickListener {
		// When an item is clicked, acquire that engine.
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			engine_address = engine_address_list.get(position);
			address_size = address_size_list.get(position);
			acquire_engine(true);
		};
	}

	//Jeffrey M added 7/3/2013
	//Clears recent connection list of locos when button is touched or clicked
	public class clear_Loco_List_button implements AdapterView.OnClickListener {
		public void onClick(View v) {
			clearList();
			onCreate(null);
		};
	}

	public class roster_item_ClickListener implements
	AdapterView.OnItemClickListener {
		// When a roster item is clicked, send request to acquire that engine.
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

			//use clicked position in list to retrieve roster item object from roster_list
			HashMap<String, String> hm 	= roster_list.get(position);
			String rosterNameString 	= hm.get("roster_name");
			String rosterAddressString 	= hm.get("roster_address");
			String rosterEntryType 		= hm.get("roster_entry_type");
			// parse address and length from string, e.g. 2591(L)
			String ras[] = threaded_application.splitByString(rosterAddressString, "(");
			if (ras[0].length() > 0) {  //only process if address found
				address_size = (ras[1].charAt(0) == 'L') 
				? address_type.LONG
						: address_type.SHORT; 	// convert S/L to 0/1
				engine_address = Integer.valueOf(ras[0]); 	// convert address to int
				if("loco".equals(rosterEntryType)) {
					sWhichThrottle += rosterNameString;  	//append rostername if type is loco (not consist) 
				}
				boolean bRosterRecent = prefs.getBoolean("roster_recent_locos_preference", 
						getResources().getBoolean(R.bool.prefRosterRecentLocosDefaultValue)); 
				acquire_engine(bRosterRecent);
			}
		};
	}

	// Handle pressing of the back button to simply return to caller
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		if (key == KeyEvent.KEYCODE_BACK) {
			end_this_activity();
		}
		return (super.onKeyDown(key, event));
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainapp = (threaded_application) getApplication();
		prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
		result = RESULT_CANCELED;
		if(mainapp.isForcingFinish()) {		// expedite
			return;
		}
		setContentView(R.layout.select_loco);

		// put pointer to this activity's handler in main app's shared variable
		mainapp.select_loco_msg_handler = new select_loco_handler();

		// Set the options for the address length.
		Spinner address_spinner = (Spinner) findViewById(R.id.address_length);
		ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this,
				R.array.address_size, android.R.layout.simple_spinner_item);
		spinner_adapter
		.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		address_spinner.setAdapter(spinner_adapter);

		// Set up a list adapter to contain the current roster list.
		roster_list = new ArrayList<HashMap<String, String>>();
		roster_list_adapter = new RosterSimpleAdapter(this, roster_list,
				R.layout.roster_list_item, new String[] { "roster_name",
				"roster_address", "roster_icon" }, new int[] { R.id.roster_name_label,
				R.id.roster_address_label, R.id.roster_icon_image });

		ListView roster_list_view = (ListView) findViewById(R.id.roster_list);
		roster_list_view.setAdapter(roster_list_adapter);
		roster_list_view.setOnItemClickListener(new roster_item_ClickListener());
		roster_list_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
				return onLongListItemClick(v,pos,id);
			}
		});


		//		refresh_roster_list();

		// Set up a list adapter to allow adding the list of recent engines to
		// the UI.
		recent_engine_list = new ArrayList<HashMap<String, String>>();
		recent_list_adapter = new SimpleAdapter(this, recent_engine_list,
				R.layout.engine_list_item, new String[] { "engine" },
				new int[] { R.id.engine_item_label });
		ListView engine_list_view = (ListView) findViewById(R.id.engine_list);
		engine_list_view.setAdapter(recent_list_adapter);
		engine_list_view.setOnItemClickListener(new engine_item());

		engine_address_list = new ArrayList<Integer>();
		address_size_list = new ArrayList<Integer>();
		//if no SD Card present then there is no recent locos list
		if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			//alert user that recent locos list requires SD Card
			TextView v=(TextView)findViewById(R.id.recent_engines_heading);
			v.setText(getString(R.string.sl_recent_engine_notice));
		}
		else {
			try {
				// Populate the ListView with the recent engines saved in a file. This
				// will be stored in /sdcard/engine_driver/recent_engine_list.txt
				// entries not matching the assumptions will be ignored
				File sdcard_path = Environment.getExternalStorageDirectory();
				File engine_list_file = new File(sdcard_path + "/engine_driver/recent_engine_list.txt");
				if (engine_list_file.exists()) {
					BufferedReader list_reader = new BufferedReader(
							new FileReader(engine_list_file));
					while (list_reader.ready()) {
						String line = list_reader.readLine();
						Integer splitPos = line.indexOf(':');
						if (splitPos > 0) {
							Integer ea, as;
							try {	
								ea = Integer.decode(line.substring(0, splitPos));
								as = Integer.decode(line.substring(splitPos + 1, line.length()));
							} 
							catch (Exception e) {
								ea = -1;
								as = -1;
							}

							if ((ea >= 0) && (as >= 0)) {
								engine_address_list.add(ea);
								address_size_list.add(as);
								HashMap<String, String> hm = new HashMap<String, String>();
								String addressLengthString = ((as == 0) ? "S" : "L");  //show L or S based on length from file
								String engineAddressString = String.format("%s(%s)",engine_address_list.get(
										engine_address_list.size() - 1).toString(), addressLengthString);
								hm.put("engine", engineAddressString);
								recent_engine_list.add(hm);
							} //if ea>=0&&as>=0
						} //if splitPos>0
					}
					list_reader.close();
					recent_list_adapter.notifyDataSetChanged();
				}

			} 
			catch (IOException except) {
				Log.e("Engine_Driver", "select_loco - Error reading recent loco file. "
						+ except.getMessage());
			}
		}

		// Set the button callbacks.
		Button button = (Button) findViewById(R.id.acquire);
		button_listener click_listener = new button_listener();
		button.setOnClickListener(click_listener);

		button = (Button) findViewById(R.id.sl_release_T);
		button.setOnClickListener(new release_button_listener_T());

		//Jeffrey added 7/3/2013
		button = (Button) findViewById(R.id.clear_Loco_List_button);
		button.setOnClickListener(new clear_Loco_List_button());

		button = (Button) findViewById(R.id.sl_release_S);
		button.setOnClickListener(new release_button_listener_S());

		button = (Button) findViewById(R.id.sl_release_G);
		button.setOnClickListener(new release_button_listener_G());

		default_address_length = prefs.getString("default_address_length", this
				.getResources().getString(
						R.string.prefDefaultAddressLengthDefaultValue));
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			sWhichThrottle = extras.getString("sWhichThrottle");
		}

		EditText la = (EditText) findViewById(R.id.loco_address);
		la.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				updateAddressEntry();
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
		la.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
					InputMethodManager imm = 
						(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
				}
				else
					return false;
			}
		});
		set_labels();
	};

	//Jeffrey added 7/3/2013
	//Clears recent connection list of locos
	public void clearList()
	{
		File sdcard_path = Environment.getExternalStorageDirectory();
		File engine_list_file = new File(sdcard_path + "/engine_driver/recent_engine_list.txt");

		if (engine_list_file.exists())
		{
			engine_list_file.delete();
			recent_engine_list.clear();
		}
		else
		{
			engine_list_file = new File(sdcard_path + "/engine_driver/recent_engine_list.txt");
		}

	} 

	@Override
	public void onResume() {
		super.onResume();
		if(mainapp.isForcingFinish()) {		//expedite
			this.finish();
			return;
		}
		mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
		mainapp.cancelRunningNotify();

		// checking address length here covers (future) case where prefs changed while paused
		default_address_length = prefs.getString("default_address_length", this
				.getResources().getString(R.string.prefDefaultAddressLengthDefaultValue));
		updateAddressEntry();	// enable/disable buttons
		// suppress popup keyboard until EditText is touched
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if(this.isFinishing()) {		//if finishing, expedite it and don't invoke setContentIntentNotification
			return;
		}
		mainapp.setContentIntentNotification(this.getIntent());
	}

	@Override
	public void onDestroy() {
		Log.d("Engine_Driver","select_loco.onDestroy() called");
		mainapp.select_loco_msg_handler = null; 
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.select_loco_menu, menu);
		SMenu = menu;
		mainapp.displayEStop(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		Intent in;
		switch (item.getItemId()) {
		case R.id.throttle_mnu:
			this.finish();
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.turnouts_mnu:
			this.finish();
			in=new Intent().setClass(this, turnouts.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			break;
		case R.id.routes_mnu:
			in = new Intent().setClass(this, routes.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.web_mnu:
			this.finish();
			in=new Intent().setClass(this, web_activity.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.preferences_mnu:
			this.finish();
			in=new Intent().setClass(this, preferences.class);
			startActivityForResult(in, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.power_control_mnu:
			this.finish();
			in=new Intent().setClass(this, power_control.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.about_mnu:
			this.finish();
			in=new Intent().setClass(this, about_page.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.logviewer_menu:
			this.finish();
			Intent logviewer=new Intent().setClass(this, LogViewerActivity.class);
			startActivity(logviewer);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EmerStop:
			mainapp.sendEStopMsg();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// end current activity
	void end_this_activity() {
		Intent resultIntent = new Intent();
		resultIntent.putExtra("whichThrottle", sWhichThrottle);  //pass whichThrottle as an extra
		setResult(result, resultIntent);
		this.finish();
		connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
	}

	private int updateAddressEntry() {
		Button ba = (Button) findViewById(R.id.acquire);
		EditText la = (EditText) findViewById(R.id.loco_address);
		int txtLen = la.getText().toString().trim().length();

		// don't allow acquire button if nothing entered
		if (txtLen > 0) {
			ba.setEnabled(true);
		} 
		else {
			ba.setEnabled(false);
		}

		// set address length
		Spinner al = (Spinner) findViewById(R.id.address_length);
		if (default_address_length.equals("Long") 
				|| (default_address_length.equals("Auto") && txtLen > 2)) {
			al.setSelection(1);
		} 
		else {
			al.setSelection(0);
		}
		return txtLen;
	}

	protected boolean onLongListItemClick(View v, int position, long id) {
		if (mainapp.roster == null) {
			Log.w("Engine_Driver", "No roster details found.");
			return true;
		} 
		HashMap<String, String> hm 	= roster_list.get(position);
		String rosternamestring 	= hm.get("roster_name");
		RosterEntry re = mainapp.roster.get(rosternamestring);
		if (re == null) {
			Log.w("Engine_Driver", "Roster entry " + rosternamestring + " not available.");
			return true;
		} 
		Log.d("Engine_Driver", "Showing details for roster entry " + rosternamestring);
		Dialog dialog = new Dialog(select_loco.this);
		dialog.setTitle("Roster details for " + rosternamestring);
		dialog.setContentView(R.layout.roster_entry);
		String res = re.toString();
		TextView tv = (TextView) dialog.findViewById(R.id.rosterEntryText);
		tv.setText(res);
		dialog.setCancelable(true);
		dialog.show();
		return true;
	}

	public class RosterSimpleAdapter extends SimpleAdapter {
		private Context cont;

		public RosterSimpleAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
						String[] from, int[] to) {
			super(context, data, resource, from, to);
			cont = context;
		}


		public View getView(int position, View convertView, ViewGroup parent) {
			if (position>roster_list.size())
				return convertView;

			HashMap<String, String> hm = roster_list.get(position);
			if (hm == null)
				return convertView;

			LayoutInflater inflater = (LayoutInflater) cont.getSystemService(Context.LAYOUT_INFLATER_SERVICE);        	
			RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.roster_list_item, null, false);

			String str = hm.get("roster_name");
			if (str != null) {
				TextView name = (TextView) view.findViewById(R.id.roster_name_label);
				name.setText(str);
			}

			str = hm.get("roster_address");
			if (str != null) {        	
				TextView secondLine = (TextView) view.findViewById(R.id.roster_address_label);
				secondLine.setText(hm.get("roster_address"));
			}

			String iconURL = hm.get("roster_icon");
			if ((iconURL != null) && (iconURL.length()>0)){
				ImageView imageView = (ImageView) view.findViewById(R.id.roster_icon_image);
				mainapp.imageDownloader.download(iconURL, imageView);
			} else {
				View v = view.findViewById(R.id.roster_icon_image);
				v.setVisibility(GONE);
			}

			return view;
		}
	}
}
