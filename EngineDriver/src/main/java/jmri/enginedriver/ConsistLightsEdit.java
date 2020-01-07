/*Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import jmri.enginedriver.Consist.ConLoco;

public class ConsistLightsEdit extends Activity implements OnGestureListener {
    public static final int LIGHT_OFF = 0;
    public static final int LIGHT_FOLLOW = 1;
    public static final int LIGHT_UNKNOWN = 2;
    public static String LIGHT_TEXT_OFF = "Off";
    public static String LIGHT_TEXT_FOLLOW = "Follow Fn Btn";
    public static String LIGHT_TEXT_UNKNOWN = "Unknown";

    static public final int RESULT_CON_LIGHTS_EDIT = RESULT_FIRST_USER;

    private threaded_application mainapp;  // hold pointer to mainapp
    private Menu CLEMenu;
    private ArrayList<HashMap<String, String>> consistList;
    private SimpleAdapter consistListAdapter;
    private ArrayList<ConLoco> consistObjList;
//    private ArrayAdapter<ConLoco> consistObjListAdapter;
//    private Spinner consistSpinner;
    private Consist consist;
    private int result;                     // set to RESULT_FIRST_USER when something is edited

    private int whichThrottle;

    public ImportExportPreferences importExportPreferences = new ImportExportPreferences();

    private GestureDetector myGesture;

    private SharedPreferences prefs;

    public void refreshConsistLists() {
        //clear and rebuild
        consistObjList.clear();
        int pos = 0;
        Collection<ConLoco> cgl = consist.getLocos(); //copy from synchronized map to avoid holding it while iterating
        for (ConLoco l : cgl) {
            if (l.isConfirmed()) {
                consistObjList.add(l);
                pos++;
            }
        }

        consistList.clear();
        for (ConLoco l : cgl) {
            if (l.isConfirmed()) {
                //put values into temp hashmap
                HashMap<String, String> hm = new HashMap<>();
                hm.put("lead_label", consist.getLeadAddr().equals(l.getAddress()) ? "LEAD" : "");
                hm.put("loco_addr", l.getAddress());
                hm.put("loco_name", l.toString());
                if (consist.getLeadAddr().equals(l.getAddress())) { // lead loco is always 'follow'
                    hm.put("loco_light", LIGHT_TEXT_FOLLOW);
                } else {
                    if (l.isLightOn() == LIGHT_OFF) {
                        hm.put("loco_light", LIGHT_TEXT_OFF);
                        mainapp.forceFunction(mainapp.throttleIntToString(whichThrottle) + l.getAddress(), 0, false);
                    } else if (l.isLightOn() == LIGHT_FOLLOW) {
                        hm.put("loco_light", LIGHT_TEXT_FOLLOW);
                        // because we can't be sure if the function has been set elsewhere, force it to what we think it should be
                        //mainapp.forceFunction(whichThrottle+l.getAddress(), 0, true);
                    } else {
                        hm.put("loco_light", LIGHT_TEXT_UNKNOWN);
                    }
                }
                consistList.add(hm);
            }
        }

        consistListAdapter.notifyDataSetChanged();
        result = RESULT_CON_LIGHTS_EDIT;
    }


    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class ConsistLightsEditHandler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE:                       // see if loco added to or removed from any throttle
                    String response_str = msg.obj.toString();
                    if (response_str.length() >= 3) {
                        char com1 = response_str.charAt(0);
                        char com2 = response_str.charAt(2);
                        if (com1 == 'M' && (com2 == '+' || com2 == '-'))
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
            }
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return myGesture.onTouchEvent(event);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);

        mainapp.applyTheme(this);
        setTitle(getApplicationContext().getResources().getString(R.string.app_name_ConsistLightsEdit)); // needed in case the langauge was changed from the default

        setContentView(R.layout.consist_lights);
        //put pointer to this activity's handler in main app's shared variable
        mainapp.consist_lights_edit_msg_handler = new ConsistLightsEditHandler();
        myGesture = new GestureDetector(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            whichThrottle = mainapp.throttleCharToInt(extras.getChar("whichThrottle"));
        }

        LIGHT_TEXT_OFF = getApplicationContext().getResources().getString(R.string.lightsTextOff);
        LIGHT_TEXT_FOLLOW = getApplicationContext().getResources().getString(R.string.lightsTextFollow);
        LIGHT_TEXT_UNKNOWN = getApplicationContext().getResources().getString(R.string.lightsTextUnknown);

        consist = mainapp.consists[whichThrottle];

        //Set up a list adapter to allow adding the list of recent connections to the UI.
        consistList = new ArrayList<>();
        consistListAdapter = new SimpleAdapter(this, consistList, R.layout.consist_lights_item,
                new String[]{"loco_name", "loco_addr", "loco_light"},
                new int[]{R.id.con_loco_name, R.id.con_loco_addr_hidden, R.id.con_loco_light});
        ListView consistLV = findViewById(R.id.consist_lights_list);
        consistLV.setAdapter(consistListAdapter);
        consistLV.setOnItemClickListener(new OnItemClickListener() {
            //When an entry is clicked, toggle the lights state for that loco
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                ViewGroup vg = (ViewGroup) v; //convert to viewgroup for clicked row
                TextView addrv = (TextView) vg.getChildAt(1); // get address text from 2nd box
                String address = addrv.getText().toString();

                int light;
                if (consist.getLeadAddr().equals(address)) { // lead loco is always 'follow'
                    light = LIGHT_FOLLOW;
                } else {
                    if ((consist.isLight(address) == LIGHT_UNKNOWN) || (consist.isLight(address) == LIGHT_FOLLOW)) {
                        light = LIGHT_OFF;
                    } else {
                        light = LIGHT_FOLLOW;
                    }
                }
                    try {
                        consist.setLight(address, light);
                    } catch (Exception e) {    // setLight returns null if address is not in consist - should not happen since address was selected from consist list
                        Log.d("Engine_Driver", "ConsistLightsEdit selected engine " + address + " that is not in consist");
                    }

                refreshConsistLists();
            }
        });

        consistLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            //When an entry is long-clicked, toggle the lights state for that loco but don't send the command to the loco
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
                ViewGroup vg = (ViewGroup) v;
                TextView addrv = (TextView) vg.getChildAt(1); // get address text from 2nd box
                String address = addrv.getText().toString();

                int light;
                if ((consist.isLight(address) == LIGHT_UNKNOWN) | (consist.isLight(address) == LIGHT_FOLLOW)) {
                    light = LIGHT_OFF;
                } else {
                    light = LIGHT_FOLLOW;
                }
               try {
                    consist.setLight(address, light);
                } catch (Exception e) {    // setLight returns null if address is not in consist - should not happen since address was selected from consist list
                    Log.d("Engine_Driver", "ConsistLightsEdit selected engine " + address + " that is not in consist");
                }
                refreshConsistLists();
                return true;
            }
        });

        OnTouchListener gestureListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return myGesture.onTouchEvent(event);
            }
        };

        consistLV.setOnTouchListener(gestureListener);

        consistObjList = new ArrayList<>();


        //update consist list
        refreshConsistLists();
        result = RESULT_OK;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
        if (CLEMenu != null) {
            mainapp.displayEStop(CLEMenu);
        }
        // suppress popup keyboard until EditText is touched
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.isFinishing()) {       //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "ConsistLightsEdit.onDestroy() called");

//        loadRecentConsistsList();
        importExportPreferences.getRecentConsistsListFromFile();
        int whichEntryIsBeingUpdated = addCurrentConistToBeginningOfList();
//        updateRecentConsists(whichEntryIsBeingUpdated);
        importExportPreferences.writeRecentConsistsListToFile(prefs, whichEntryIsBeingUpdated);

        mainapp.consist_lights_edit_msg_handler = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.consist_lights_edit_menu, menu);
        CLEMenu = menu;
        mainapp.displayEStop(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        //noinspection SwitchStatementWithTooFewBranches
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
        if (key == KeyEvent.KEYCODE_BACK) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("whichThrottle", mainapp.throttleIntToChar(whichThrottle) );  //pass whichThrottle as an extra
            setResult(result, resultIntent);
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return (super.onKeyDown(key, event));
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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    int addCurrentConistToBeginningOfList() { // if necessary   return -1 if not currently in the list
        ArrayList<Integer> tempConsistEngineAddressList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistAddressSizeList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistDirectionList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistSourceList_inner = new ArrayList<>();
        ArrayList<String> tempConsistRosterNameList_inner = new ArrayList<>();
        ArrayList<Integer> tempConsistLightList_inner = new ArrayList<>();

        //if not updating list or no SD Card present then nothing else to do
        if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return -1;

        Collection<ConLoco> conLocos = consist.getLocos();


        for (ConLoco l : conLocos) {
            tempConsistEngineAddressList_inner.add(l.getIntAddress());
            tempConsistAddressSizeList_inner.add(l.getIntAddressLength());
            String addr = mainapp.locoAddressToString(l.getIntAddress(), l.getIntAddressLength(), true);
            tempConsistDirectionList_inner.add((consist.isBackward(addr) ? 1 : 0));
            String rosterName = "";
            if (l.getRosterName() != null) {
                rosterName = l.getRosterName();
            }
//            tempConsistSourceList_inner.add(rosterName.equals("") ? WHICH_SOURCE_ADDRESS : WHICH_SOURCE_ROSTER);
            tempConsistSourceList_inner.add(l.getWhichSource());
            tempConsistRosterNameList_inner.add(rosterName);
            tempConsistLightList_inner.add((consist.isLight(addr)));
        }

        int whichEntryIsBeingUpdated = -1;
        // find out which entry it is in the roster
        boolean isSame = false;
        for (int i = 0; i < importExportPreferences.consistEngineAddressList.size() && !isSame; i++) {
            if (importExportPreferences.consistEngineAddressList.get(i).size() == tempConsistEngineAddressList_inner.size()) {  // if the lists are different sizes don't bother
                for (int j = 0; j < importExportPreferences.consistEngineAddressList.get(i).size() && !isSame; j++) {
                    if ((importExportPreferences.consistEngineAddressList.get(i).get(j).equals(tempConsistEngineAddressList_inner.get(j)))
                            && (importExportPreferences.consistDirectionList.get(i).get(j).equals(tempConsistDirectionList_inner.get(j)))) {
                        isSame = true;
                    }
                }
                if (isSame) {
                    whichEntryIsBeingUpdated = i + 1; //remember this, so we can remove this line in the list.  Add 1 because we are gone to force a new line at the top
                }
            }
        }

        //add it to the beginning of the list
        importExportPreferences.consistEngineAddressList.add(0, tempConsistEngineAddressList_inner);
        importExportPreferences.consistAddressSizeList.add(0, tempConsistAddressSizeList_inner);
        importExportPreferences.consistDirectionList.add(0, tempConsistDirectionList_inner);
        importExportPreferences.consistSourceList.add(0, tempConsistSourceList_inner);
        importExportPreferences.consistRosterNameList.add(0, tempConsistRosterNameList_inner);
        importExportPreferences.consistLightList.add(0, tempConsistLightList_inner);

        String consistName = consist.toString();
        if (whichEntryIsBeingUpdated>0) { //this may already have a name
            consistName = importExportPreferences.consistNameList.get(whichEntryIsBeingUpdated-1);
        }
        importExportPreferences.consistNameList.add(0, consistName);

        return whichEntryIsBeingUpdated;
    }


}