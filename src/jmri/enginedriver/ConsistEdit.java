/*Copyright (C) 2014 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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

import java.util.ArrayList;
import java.util.HashMap;
import jmri.enginedriver.Consist.ConLoco;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class ConsistEdit extends Activity  implements OnGestureListener {

	private threaded_application mainapp;  // hold pointer to mainapp
	private Menu CEMenu;
	private ArrayList<HashMap<String, String> > consistList;
	private SimpleAdapter consistListAdapter;
	private ArrayList<ConLoco> consistObjList;
	private ArrayAdapter<ConLoco> consistObjListAdapter;
	private Spinner consistSpinner;
	private Consist consist;
	private int result; 					// set to RESULT_FIRST_USER when something is edited

	private char whichThrottle;

	private GestureDetector myGesture ;

	public void refreshConsistLists() {
		//clear and rebuild
		consistObjList.clear();
		int pos = 0;
		for(ConLoco l : consist.getLocos()) {
			if(l.isConfirmed()) {
				consistObjList.add(l);
				if(l.getAddress().equals(consist.getLeadAddr()))
					consistSpinner.setSelection(pos);
				pos++;
			}
		}
		consistObjListAdapter.notifyDataSetChanged();

		consistList.clear();
		for (ConLoco l : consist.getLocos()) {
			if(l.isConfirmed()) {
				//put values into temp hashmap
				HashMap<String, String> hm=new HashMap<String, String>();
				hm.put("lead_label", consist.getLeadAddr().equals(l.getAddress()) ? "LEAD" : "");
				hm.put("loco_addr", l.getAddress());
				hm.put("loco_name", l.toString());
				hm.put("loco_facing", l.isBackward() ? "Rear" : "Front");
				consistList.add(hm);
			}
		}
		consistListAdapter.notifyDataSetChanged();
		result = RESULT_FIRST_USER;
	}


	//Handle messages from the communication thread back to this thread (responses from withrottle)
	@SuppressLint("HandlerLeak")
	class ConsistEditHandler extends Handler {

		public void handleMessage(Message msg) {
			switch(msg.what) {
			case message_type.RESPONSE: 					  // see if loco added to or removed from any throttle
				String response_str = msg.obj.toString();
				if(response_str.length() >= 3) {
					char com1 = response_str.charAt(0);
					char com2 = response_str.charAt(2);
					if(com1 == 'M' && (com2 == '+' || com2 == '-'))
						refreshConsistLists();
				}
				break;
			case message_type.WIT_CON_RETRY:
				witRetry(msg.obj.toString());
				break;
			case message_type.WIT_CON_RECONNECT:
				refreshConsistLists(); 
				break;
			case message_type.DISCONNECT:
			case message_type.SHUTDOWN:
				disconnect();
				break;
			};
		}
	}

	private void witRetry(String s) {
		Intent in = new Intent().setClass(this, reconnect_status.class);
		in.putExtra("status", s);
		startActivity(in);
		connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
	}
	

	@Override
	public boolean onTouchEvent(MotionEvent event){
		return myGesture.onTouchEvent(event);
	}

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState)  {
		super.onCreate(savedInstanceState);
		mainapp=(threaded_application)getApplication();
		if(mainapp.isForcingFinish()) {		// expedite
			return;
		}

		setContentView(R.layout.consist);
		//put pointer to this activity's handler in main app's shared variable
		mainapp.consist_edit_msg_handler=new ConsistEditHandler();
		myGesture = new GestureDetector(this);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			whichThrottle = extras.getChar("whichThrottle");
		}

		//consist = (whichThrottle == 'T') ? mainapp.consistT : mainapp.consistS;
		if(whichThrottle == 'T') {
			consist =  mainapp.consistT;
		} else if(whichThrottle == 'G') 	{
			consist =  mainapp.consistG;
		} else {
			consist =  mainapp.consistS;
		}

		//Set up a list adapter to allow adding the list of recent connections to the UI.
		consistList=new ArrayList<HashMap<String, String> >();
		consistListAdapter=new SimpleAdapter(this, consistList, R.layout.consist_item, 
				new String[] {"loco_name", "loco_addr", "lead_label", "loco_facing"},
				new int[] {R.id.con_loco_name, R.id.con_loco_addr_hidden, R.id.con_lead_label, R.id.con_loco_facing});
		ListView consistLV=(ListView)findViewById(R.id.consist_list);
		consistLV.setAdapter(consistListAdapter);
		consistLV.setOnItemClickListener(new OnItemClickListener() {
			//When an entry is clicked, toggle the facing state
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)	    {
				ViewGroup vg = (ViewGroup)v; //convert to viewgroup for clicked row
				TextView addrv = (TextView) vg.getChildAt(1); // get address text from 2nd box
				String address = addrv.getText().toString();

				try {
					consist.setBackward(address, !consist.isBackward(address));
				}
				catch(Exception e) {	// isBackward returns null if address is not in consist - should not happen since address was selected from consist list
					Log.d("Engine_Driver","ConsistEdit selected engine "+address+" that is not in consist");
				}
				refreshConsistLists();
			}
		});	  
		consistLV.setOnItemLongClickListener(new OnItemLongClickListener() {
			//When an entry is long-clicked, remove it from the consist
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
				ViewGroup vg = (ViewGroup) v;
				TextView addrv = (TextView) vg.getChildAt(1); // get address text from 2nd box
				String addr = addrv.getText().toString();

				if(!consist.getLeadAddr().equals(addr)) {
					consist.remove(addr);
					mainapp.sendMsg(mainapp.comm_msg_handler, message_type.RELEASE, addr, (int) whichThrottle);	  //release the loco
					refreshConsistLists();
				}
				return true;
			}
		});

		OnTouchListener gestureListener = new ListView.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (myGesture.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};

		consistLV.setOnTouchListener(gestureListener);

		consistObjList = new ArrayList<ConLoco>();
		consistSpinner = (Spinner) findViewById(R.id.consist_lead);
		consistObjListAdapter = new ArrayAdapter<ConLoco>(this, android.R.layout.simple_spinner_item, consistObjList);
		consistObjListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		consistSpinner.setAdapter(consistObjListAdapter);

		consistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				ConLoco l = (ConLoco)parent.getSelectedItem();
				String lAddr = l.getAddress();
				if (!(consist.getLeadAddr().equals(lAddr))) {
					consist.setLeadAddr(lAddr);
					refreshConsistLists();
				}
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		//update consist list
		refreshConsistLists();
		result = RESULT_OK;
	};

	@Override
	public void onResume() {
		super.onResume();
		mainapp.removeNotification();
		if(mainapp.isForcingFinish()) {		//expedite
			this.finish();
			return;
		}
		mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
		if(CEMenu != null)
		{
			mainapp.displayEStop(CEMenu);
		}
		// suppress popup keyboard until EditText is touched
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if(!this.isFinishing()) {		//only invoke setContentIntentNotification when going into background
			mainapp.addNotification(this.getIntent());
		}
	}

	/** Called when the activity is finished. */
	@Override
	public void onDestroy() {
		Log.d("Engine_Driver","ConsistedEdit.onDestroy()");

		mainapp.consist_edit_msg_handler = null;
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.consist_edit_menu, menu);
		CEMenu = menu;
		mainapp.displayEStop(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case R.id.EmerStop:
			mainapp.sendEStopMsg();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	//Always go to throttle if back button pressed
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		if(key==KeyEvent.KEYCODE_BACK) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra("whichThrottle", whichThrottle);  //pass whichThrottle as an extra
			setResult(result, resultIntent);
			this.finish();  //end this activity
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			return true;
		}
		return(super.onKeyDown(key, event));
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}
	@Override
	public void onShowPress(MotionEvent e) {
	}
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	private void disconnect() {
		this.finish();
	}
};